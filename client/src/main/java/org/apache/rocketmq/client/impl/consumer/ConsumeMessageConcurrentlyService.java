/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.impl.consumer;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeReturnType;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.client.stat.ConsumerStatsManager;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.CMResult;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.*;
import java.util.concurrent.*;

/**
 * 这一遍仔细看了消息消费设计，起点与我设想的不太一样，我本来以ProcessQueue已经有本地消息全集
 * 消费线程关注ProcessQueue#msgTreeMap即可知道是否具备消费条件
 * 今天发现并非如此，而是在PullMessage request之后，Broker回复拉取response
 * 这个时候会回调DefaultMQPushConsumerImpl#newPullCallback()对象，
 * 然后会对返回的消息进行tag过滤，如果发现还有msgFoundList不为空，则会给本服务线程提交一个任务
 *
 * ConsumeMessageConcurrentlyService主体设计是一个服务消费模型，感知有新的task
 * 会进行消息的后续消费
 *
 *    DefaultMQPushConsumerImpl#newPullCallback()
 * -> ConsumeMessageService#submitConsumeRequest()
 * -> ConsumeRequest :: new
 * -> this.consumeExecutor.submit 往线程池中提交任务
 * -> ConsumeRequest#run()
 *
 * 设计为提供者消费者模型为了减少cpu空转做无用的消息存量检测，只需要被阻塞队列唤醒即可
 */
public class ConsumeMessageConcurrentlyService implements ConsumeMessageService {

    private static final InternalLogger log = ClientLogger.getLog();

    private final DefaultMQPushConsumerImpl defaultMQPushConsumerImpl;
    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final MessageListenerConcurrently messageListener;
    private final BlockingQueue<Runnable> consumeRequestQueue;
    /* 消息并行消费线程池 */
    private final ThreadPoolExecutor consumeExecutor;
    private final String consumerGroup;

    /**
     * 添加消费任务到 consumeExecutor 的延时调度器
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * 定时删除过期消息清理器
     */
    private final ScheduledExecutorService cleanExpireMsgExecutors;

    public ConsumeMessageConcurrentlyService(DefaultMQPushConsumerImpl defaultMQPushConsumerImpl,
        MessageListenerConcurrently messageListener) {
        this.defaultMQPushConsumerImpl = defaultMQPushConsumerImpl;
        this.messageListener = messageListener;

        this.defaultMQPushConsumer = this.defaultMQPushConsumerImpl.getDefaultMQPushConsumer();
        this.consumerGroup = this.defaultMQPushConsumer.getConsumerGroup();
        this.consumeRequestQueue = new LinkedBlockingQueue<>();

        this.consumeExecutor = new ThreadPoolExecutor(
            this.defaultMQPushConsumer.getConsumeThreadMin(),
            this.defaultMQPushConsumer.getConsumeThreadMax(),
            1000 * 60,
            TimeUnit.MILLISECONDS,
            this.consumeRequestQueue,
            new ThreadFactoryImpl("ConsumeMessageThread_")
        );

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryImpl("ConsumeMessageScheduledThread_")
        );
        this.cleanExpireMsgExecutors = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryImpl("CleanExpireMsgScheduledThread_")
        );
    }

    public void start() {
        this.cleanExpireMsgExecutors.scheduleAtFixedRate(
            this::cleanExpireMsg,
            this.defaultMQPushConsumer.getConsumeTimeout(),
            this.defaultMQPushConsumer.getConsumeTimeout(),
            TimeUnit.MINUTES
        );
    }

    public void shutdown(long awaitTerminateMillis) {
        this.scheduledExecutorService.shutdown();

        ThreadUtils.shutdownGracefully(
            this.consumeExecutor,
            awaitTerminateMillis,
            TimeUnit.MILLISECONDS
        );

        this.cleanExpireMsgExecutors.shutdown();
    }

    @Override
    public void updateCorePoolSize(int corePoolSize) {
        if (corePoolSize > 0
            && corePoolSize <= Short.MAX_VALUE
            && corePoolSize < this.defaultMQPushConsumer.getConsumeThreadMax()
        ) {
            this.consumeExecutor.setCorePoolSize(corePoolSize);
        }
    }

    /**
     * ⚠️: incCorePoolSize() 增加核心线程数
     *     decCorePoolSize() 减少核心线程数
     *
     * 都不会生效，源码都被注释了，只保留了一个空方法体
     */
    @Override
    public void incCorePoolSize() {
        // long corePoolSize = this.consumeExecutor.getCorePoolSize();
        // if (corePoolSize < this.defaultMQPushConsumer.getConsumeThreadMax())
        // {
        // this.consumeExecutor.setCorePoolSize(this.consumeExecutor.getCorePoolSize()
        // + 1);
        // }
        // log.info("incCorePoolSize Concurrently from {} to {}, ConsumerGroup:
        // {}",
        // corePoolSize,
        // this.consumeExecutor.getCorePoolSize(),
        // this.consumerGroup);
    }

    @Override
    public void decCorePoolSize() {
        // long corePoolSize = this.consumeExecutor.getCorePoolSize();
        // if (corePoolSize > this.defaultMQPushConsumer.getConsumeThreadMin())
        // {
        // this.consumeExecutor.setCorePoolSize(this.consumeExecutor.getCorePoolSize()
        // - 1);
        // }
        // log.info("decCorePoolSize Concurrently from {} to {}, ConsumerGroup:
        // {}",
        // corePoolSize,
        // this.consumeExecutor.getCorePoolSize(),
        // this.consumerGroup);
    }

    @Override
    public int getCorePoolSize() {
        return this.consumeExecutor.getCorePoolSize();
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt msg, String brokerName) {
        ConsumeMessageDirectlyResult result = new ConsumeMessageDirectlyResult();
        result.setOrder(false);
        result.setAutoCommit(true);

        List<MessageExt> msgs = new ArrayList<>();
        msgs.add(msg);
        MessageQueue mq = new MessageQueue();
        mq.setBrokerName(brokerName);
        mq.setTopic(msg.getTopic());
        mq.setQueueId(msg.getQueueId());

        ConsumeConcurrentlyContext context = new ConsumeConcurrentlyContext(mq);

        this.defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, this.consumerGroup);

        final long beginTime = System.currentTimeMillis();
        log.info("consumeMessageDirectly receive new message: {}", msg);

        try {
            ConsumeConcurrentlyStatus status = this.messageListener.consumeMessage(msgs, context);
            if (status != null) {
                switch (status) {
                    case CONSUME_SUCCESS:
                        result.setConsumeResult(CMResult.CR_SUCCESS);
                        break;
                    case RECONSUME_LATER:
                        result.setConsumeResult(CMResult.CR_LATER);
                        break;
                    default:
                        break;
                }
            } else {
                result.setConsumeResult(CMResult.CR_RETURN_NULL);
            }
        } catch (Throwable e) {
            result.setConsumeResult(CMResult.CR_THROW_EXCEPTION);
            result.setRemark(RemotingHelper.exceptionSimpleDesc(e));

            log.warn(String.format("consumeMessageDirectly exception: %s Group: %s Msgs: %s MQ: %s",
                RemotingHelper.exceptionSimpleDesc(e),
                ConsumeMessageConcurrentlyService.this.consumerGroup, msgs, mq),
                e
            );
        }

        result.setSpentTimeMills(System.currentTimeMillis() - beginTime);
        log.info("consumeMessageDirectly Result: {}", result);

        return result;
    }

    /**
     * 这里需要留心，虽然消息MessageListenerConcurrently#consumeMessage入参是一个List
     * 但是消息并发消费时consumeBatchSize默认为1，其实List中只有一条数据
     * 此处有分页逻辑，且并发消费模式下必分页
     */
    @Override
    public void submitConsumeRequest(
        List<MessageExt> msgs, ProcessQueue processQueue,
        MessageQueue messageQueue, boolean dispatchToConsume
    ) {
        /* consumeBatchSize默认为1 */
        int consumeBatchSize = this.defaultMQPushConsumer.getConsumeMessageBatchMaxSize();
        if (msgs.size() <= consumeBatchSize) {
            ConsumeRequest consumeRequest = new ConsumeRequest(msgs, processQueue, messageQueue);
            try {
                this.consumeExecutor.submit(consumeRequest);
            } catch (RejectedExecutionException e) {
                this.submitConsumeRequestLater(consumeRequest);
            }
        }
        else {
            /* 分页处理 */
            for (int total = 0; total < msgs.size(); ) {
                List<MessageExt> msgThis = new ArrayList<>(consumeBatchSize);
                for (int i = 0; i < consumeBatchSize; i++, total++) {
                    if (total < msgs.size()) {
                        msgThis.add(msgs.get(total));
                    } else {
                        break;
                    }
                }

                ConsumeRequest consumeRequest = new ConsumeRequest(msgThis, processQueue, messageQueue);
                try {
                    this.consumeExecutor.submit(consumeRequest);
                } catch (RejectedExecutionException e) {
                    for (; total < msgs.size(); total++) {
                        msgThis.add(msgs.get(total));
                    }

                    this.submitConsumeRequestLater(consumeRequest);
                }
            }
        }
    }


    private void cleanExpireMsg() {
        Iterator<Map.Entry<MessageQueue, ProcessQueue>> it = defaultMQPushConsumerImpl
            .getRebalanceImpl()
            .getProcessQueueTable()
            .entrySet()
            .iterator();

        while (it.hasNext()) {
            Map.Entry<MessageQueue, ProcessQueue> next = it.next();
            ProcessQueue pq = next.getValue();
            pq.cleanExpiredMsg(defaultMQPushConsumer);
        }
    }

    /**
     * 消息消费之后的后续处理
     * 消费进度也是在此处修改的
     * 此时的消费进度仅仅是保存在Client内存中，并没有同步到Broker
     */
    public void processConsumeResult(
        ConsumeConcurrentlyStatus status, ConsumeConcurrentlyContext context, ConsumeRequest consumeRequest
    ) {
        if (consumeRequest.getMsgs().isEmpty())
            return;

        int ackIndex = context.getAckIndex();
        switch (status) {
            case CONSUME_SUCCESS:
                if (ackIndex >= consumeRequest.getMsgs().size()) {
                    ackIndex = consumeRequest.getMsgs().size() - 1;
                }
                int ok = ackIndex + 1;
                int failed = consumeRequest.getMsgs().size() - ok;
                this.getConsumerStatsManager().incConsumeOKTPS(
                    consumerGroup, consumeRequest.getMessageQueue().getTopic(), ok
                );
                this.getConsumerStatsManager().incConsumeFailedTPS(
                    consumerGroup, consumeRequest.getMessageQueue().getTopic(), failed
                );
                break;
            case RECONSUME_LATER:
                ackIndex = -1;
                this.getConsumerStatsManager().incConsumeFailedTPS(
                    consumerGroup,
                    consumeRequest.getMessageQueue().getTopic(),
                    consumeRequest.getMsgs().size()
                );
                break;
            default:
                break;
        }

        switch (defaultMQPushConsumer.getMessageModel()) {
            case BROADCASTING:
                for (int i = ackIndex + 1; i < consumeRequest.getMsgs().size(); i++) {
                    MessageExt msg = consumeRequest.getMsgs().get(i);
                    log.warn("BROADCASTING, the message consume failed, drop it, {}", msg.toString());
                }
                break;
            case CLUSTERING:
                List<MessageExt> msgBackFailed = new ArrayList<>(consumeRequest.getMsgs().size());
                /* ⚠️:如果不修改默认设置，s = 1，这个for根本不会进入 */
                for (int i = ackIndex + 1, s = consumeRequest.getMsgs().size(); i < s; i++) {
                    MessageExt msg = consumeRequest.getMsgs().get(i);
                    boolean result = this.sendMessageBack(msg, context);
                    if (!result) {
                        msg.setReconsumeTimes(msg.getReconsumeTimes() + 1);
                        msgBackFailed.add(msg);
                    }
                }
                /* 与broker通信失败，上一个for要是没走，那这里也不会走 */
                if (!msgBackFailed.isEmpty()) {
                    consumeRequest.getMsgs().removeAll(msgBackFailed);
                    this.submitConsumeRequestLater(
                        msgBackFailed, consumeRequest.getProcessQueue(), consumeRequest.getMessageQueue()
                    );
                }
                break;
            default:
                break;
        }
        /* 获取当前 ProcessQueue#msgTreeMap 中最小的消息偏移量,并上报此偏移量作为消费进度 */
        long offset = consumeRequest.getProcessQueue().removeMessage(consumeRequest.getMsgs());
        if (offset >= 0 && !consumeRequest.getProcessQueue().isDropped()) {
            defaultMQPushConsumerImpl.getOffsetStore().updateOffset(consumeRequest.getMessageQueue(), offset, true);
        }
    }

    public ConsumerStatsManager getConsumerStatsManager() {
        return defaultMQPushConsumerImpl.getConsumerStatsManager();
    }

    public boolean sendMessageBack(MessageExt msg, ConsumeConcurrentlyContext context) {
        int delayLevel = context.getDelayLevelWhenNextConsume();

        /* Wrap topic with namespace before sending back message */
        msg.setTopic(defaultMQPushConsumer.withNamespace(msg.getTopic()));
        try {
            defaultMQPushConsumerImpl.sendMessageBack(msg, delayLevel, context.getMessageQueue().getBrokerName());
            return true;
        } catch (Exception e) {
            log.error("sendMessageBack exception, group: " + consumerGroup + " msg: " + msg.toString(), e);
        }

        return false;
    }

    private void submitConsumeRequestLater(
        List<MessageExt> msgs, ProcessQueue processQueue, MessageQueue messageQueue
    ) {
        this.scheduledExecutorService.schedule(
            () -> submitConsumeRequest(msgs, processQueue, messageQueue, true),
            5000,
            TimeUnit.MILLISECONDS
        );
    }

    private void submitConsumeRequestLater(ConsumeRequest consumeRequest) {
        this.scheduledExecutorService.schedule(
            () -> consumeExecutor.submit(consumeRequest),
            5000,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * ConsumeMessageOrderlyService.ConsumeRequest
     * 名字一样，内部实现区别较大
     */
    class ConsumeRequest implements Runnable {

        private final List<MessageExt> msgs;
        private final ProcessQueue processQueue;
        private final MessageQueue messageQueue;

        public ConsumeRequest(List<MessageExt> msgs, ProcessQueue processQueue, MessageQueue messageQueue) {
            this.msgs = msgs;
            this.processQueue = processQueue;
            this.messageQueue = messageQueue;
        }

        @Override
        public void run() {
            if (this.processQueue.isDropped()) {
                log.info("the message queue not be able to consume, because it's dropped. group={} {}", ConsumeMessageConcurrentlyService.this.consumerGroup, this.messageQueue);
                return;
            }

            MessageListenerConcurrently listener = messageListener;
            ConsumeConcurrentlyContext context = new ConsumeConcurrentlyContext(messageQueue);
            ConsumeConcurrentlyStatus status = null;
            defaultMQPushConsumerImpl.resetRetryAndNamespace(msgs, defaultMQPushConsumer.getConsumerGroup());

            ConsumeMessageContext consumeMessageContext = null;
            /* 检查消息消费时候是否注册了钩子函数，如果有的话需要先执行钩子函数 */
            if (defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext = new ConsumeMessageContext();
                consumeMessageContext.setNamespace(defaultMQPushConsumer.getNamespace());
                consumeMessageContext.setConsumerGroup(defaultMQPushConsumer.getConsumerGroup());
                consumeMessageContext.setProps(new HashMap<>());
                consumeMessageContext.setMq(messageQueue);
                consumeMessageContext.setMsgList(msgs);
                consumeMessageContext.setSuccess(false);
                defaultMQPushConsumerImpl.executeHookBefore(consumeMessageContext);
            }

            long beginTimestamp = System.currentTimeMillis();
            boolean hasException = false;
            ConsumeReturnType returnType = ConsumeReturnType.SUCCESS;
            try {
                if (msgs != null && !msgs.isEmpty()) {
                    for (MessageExt msg : msgs) {
                        MessageAccessor.setConsumeStartTimeStamp(msg, String.valueOf(System.currentTimeMillis()));
                    }
                }
                /* ⚠️: 消息集合被包装成不可变集合 */
                status = listener.consumeMessage(Collections.unmodifiableList(msgs), context);
            } catch (Throwable e) {
                log.warn("consumeMessage exception: {} Group: {} Msgs: {} MQ: {}",
                    RemotingHelper.exceptionSimpleDesc(e), consumerGroup, msgs, messageQueue
                );
                hasException = true;
            }
            long consumeRT = System.currentTimeMillis() - beginTimestamp;
            if (null == status) {
                if (hasException) {
                    returnType = ConsumeReturnType.EXCEPTION;
                } else {
                    returnType = ConsumeReturnType.RETURNNULL;
                }
            } else if (consumeRT >= defaultMQPushConsumer.getConsumeTimeout() * 60 * 1000) {
                returnType = ConsumeReturnType.TIME_OUT;
            } else if (ConsumeConcurrentlyStatus.RECONSUME_LATER == status) {
                returnType = ConsumeReturnType.FAILED;
            }

            if (defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext.getProps().put(MixAll.CONSUME_CONTEXT_TYPE, returnType.name());
            }

            if (null == status) {
                log.warn("consumeMessage return null, Group: {} Msgs: {} MQ: {}",
                    consumerGroup, msgs, messageQueue
                );
                status = ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }

            if (defaultMQPushConsumerImpl.hasHook()) {
                consumeMessageContext.setStatus(status.toString());
                consumeMessageContext.setSuccess(ConsumeConcurrentlyStatus.CONSUME_SUCCESS == status);
                defaultMQPushConsumerImpl.executeHookAfter(consumeMessageContext);
            }

            getConsumerStatsManager().incConsumeRT(consumerGroup, messageQueue.getTopic(), consumeRT);

            if (!processQueue.isDropped()) {
                processConsumeResult(status, context, this);
            } else {
                log.warn("processQueue is dropped without process consume result. messageQueue={}, msgs={}", messageQueue, msgs);
            }
        }

        public List<MessageExt> getMsgs() {
            return msgs;
        }

        public ProcessQueue getProcessQueue() {
            return processQueue;
        }

        public MessageQueue getMessageQueue() {
            return messageQueue;
        }

    }

}
