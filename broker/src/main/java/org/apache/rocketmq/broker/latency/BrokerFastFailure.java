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
package org.apache.rocketmq.broker.latency;

import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.remoting.netty.RequestTask;
import org.apache.rocketmq.remoting.protocol.RemotingSysResponseCode;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Broker端快速失败机制实现
 * https://mp.weixin.qq.com/s?__biz=MzIzNzgyMjYxOQ==&mid=2247484718&idx=1&sn=de898f6efec78890e699eb02d8d1ee74&scene=19#wechat_redirect
 *
 * cleanExpiredRequestInQueue() 会产生 ResponseCode.SYSTEM_BUSY
 * https://mp.weixin.qq.com/s?__biz=MzIzNzgyMjYxOQ==&mid=2247484473&idx=1&sn=0ad69109dbd819fe834ad66b49730674&scene=19#wechat_redirect
 */
public class BrokerFastFailure {

    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.BROKER_LOGGER_NAME);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryImpl("BrokerFastFailureScheduledThread")
    );
    private final BrokerController brokerController;

    public BrokerFastFailure(BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    @SuppressWarnings("all")
    public static RequestTask castRunnable(Runnable runnable) {
        try {
            if (runnable instanceof FutureTaskExt) {
                FutureTaskExt object = (FutureTaskExt) runnable;
                return (RequestTask) object.getRunnable();
            }
        } catch (Throwable e) {
            log.error(String.format("castRunnable exception, %s", runnable.getClass().getName()), e);
        }

        return null;
    }

    /**
     * 每隔 10ms 执行一次过时任务清理
     * 先判断是否打开快速失败机制
     */
    public void start() {
        this.scheduledExecutorService.scheduleAtFixedRate(
                () -> {
                    if (brokerController.getBrokerConfig().isBrokerFastFailureEnable()) {
                        cleanExpiredRequest();
                    }
                },
                1000,
                10,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 清理已到期的请求
     * 返回 RemotingSysResponseCode.SYSTEM_BUSY 状态，如果是同步发送也没机会重试
     */
    private void cleanExpiredRequest() {
        /* Page Cache Busy */
        while (this.brokerController.getMessageStore().isOSPageCacheBusy()) {
            try {
                if (!this.brokerController.getSendThreadPoolQueue().isEmpty()) {
                    final Runnable runnable = this.brokerController.getSendThreadPoolQueue().poll(0, TimeUnit.SECONDS);
                    if (null == runnable) {
                        break;
                    }

                    final RequestTask rt = castRunnable(runnable);
                    if (Objects.nonNull(rt)) {
                        rt.returnResponse(
                            RemotingSysResponseCode.SYSTEM_BUSY,
                            String.format("[PCBUSY_CLEAN_QUEUE]broker busy, start flow control for a while, period in queue: %sms, size of queue: %d",
                                    System.currentTimeMillis() - rt.getCreateTimestamp(), this.brokerController.getSendThreadPoolQueue().size()
                            )
                        );
                    }
                } else {
                    break;
                }
            } catch (Throwable ignored) {
            }
        }

        cleanExpiredRequestInQueue(
            this.brokerController.getSendThreadPoolQueue(),
            this.brokerController.getBrokerConfig().getWaitTimeMillsInSendQueue()
        );
        cleanExpiredRequestInQueue(
            this.brokerController.getPullThreadPoolQueue(),
            this.brokerController.getBrokerConfig().getWaitTimeMillsInPullQueue()
        );
        cleanExpiredRequestInQueue(
            this.brokerController.getHeartbeatThreadPoolQueue(),
            this.brokerController.getBrokerConfig().getWaitTimeMillsInHeartbeatQueue()
        );
        cleanExpiredRequestInQueue(
            this.brokerController.getEndTransactionThreadPoolQueue(),
            this.brokerController.getBrokerConfig().getWaitTimeMillsInTransactionQueue()
        );
    }

    /**
     * 清理过期任务
     *
     * ⚠️：这里的清理是指逻辑上的清理，并非丢弃任务不管而是将其状态码置为RemotingSysResponseCode.SYSTEM_BUSY，这个状态至关重要
     *
     * 因为 Client 发送的消息我们都会给一个回复，快速失败的话 code = RemotingSysResponseCode.SYSTEM_BUSY
     * @see org.apache.rocketmq.client.impl.MQClientAPIImpl#processSendResponse 这里会抛出 MQBrokerException 异常
     * 然后异常会被同一处理{@link org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl#sendKernelImpl}
     *
     * 认定为 MQBrokerException 异常后，执行完钩子函数，继续向上抛出该异常
     * 然后来到{@link org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl#sendDefaultImpl}
     * sendDefaultImpl(),判定为 MQBrokerException 异常之后，根据状态码判断是否重试,
     * sendDefaultImpl()内部有一个 for 循环，其实重试能力就是 for 赋予的
     *
     * switch (e.getResponseCode()) {
     *        case ResponseCode.TOPIC_NOT_EXIST:
     *        case ResponseCode.SERVICE_NOT_AVAILABLE:
     *        case ResponseCode.SYSTEM_ERROR:
     *        case ResponseCode.NO_PERMISSION:
     *        case ResponseCode.NO_BUYER_ID:
     *        case ResponseCode.NOT_IN_CURRENT_UNIT:
     *             continue;
     *        default:
     *             if (sendResult != null) {
     *                  return sendResult;
     *             }
     *             throw e;
     * }
     *
     * 他这个判断 ResponseCode.SYSTEM_BUSY 恰恰不执行 continue，也就是不重试
     *
     * @param blockingQueue           任务队列
     * @param maxWaitTimeMillsInQueue 过期时间
     */
    void cleanExpiredRequestInQueue(BlockingQueue<Runnable> blockingQueue, long maxWaitTimeMillsInQueue) {
        while (true) {
            try {
                if (!blockingQueue.isEmpty()) {
                    final Runnable runnable = blockingQueue.peek();
                    if (null == runnable) {
                        break;
                    }
                    RequestTask rt = castRunnable(runnable);
                    if (rt == null || rt.isStopRun()) {
                        break;
                    }

                    /**
                     * 这里执行判断逻辑，发现排队时间大于该队列容忍的最大等待时长
                     * 移除该任务，并给该 RequestTask 填充状态码code = RemotingSysResponseCode.SYSTEM_BUSY
                     *
                     * processSendResponse() 方法会根据 code 定制化行为
                     */
                    long behind = System.currentTimeMillis() - rt.getCreateTimestamp();
                    if (behind >= maxWaitTimeMillsInQueue) {
                        if (blockingQueue.remove(runnable)) {
                            rt.setStopRun(true);
                            rt.returnResponse(RemotingSysResponseCode.SYSTEM_BUSY,
                                    String.format("[TIMEOUT_CLEAN_QUEUE]broker busy, start flow control for a while, period in queue: %sms, size of queue: %d", behind, blockingQueue.size()));
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

}
