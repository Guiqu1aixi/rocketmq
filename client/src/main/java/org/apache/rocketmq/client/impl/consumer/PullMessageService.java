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

import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.logging.InternalLogger;

import java.util.concurrent.*;

/**
 * 消息拉取服务线程
 */
public class PullMessageService extends ServiceThread {

    private final InternalLogger log = ClientLogger.getLog();

    private final LinkedBlockingQueue<PullRequest> pullRequestQueue = new LinkedBlockingQueue<>();
    private final MQClientInstance mQClientFactory;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "PullMessageServiceScheduledThread")
    );

    public PullMessageService(MQClientInstance mQClientFactory) {
        this.mQClientFactory = mQClientFactory;
    }

    public void executePullRequestLater(PullRequest pullRequest, long timeDelay) {
        if (!isStopped()) {
            this.scheduledExecutorService.schedule(
                () -> executePullRequestImmediately(pullRequest),
                timeDelay,
                TimeUnit.MILLISECONDS
            );
        } else {
            log.warn("PullMessageServiceScheduledThread has shutdown");
        }
    }

    public void executePullRequestImmediately(PullRequest pullRequest) {
        try {
            this.pullRequestQueue.put(pullRequest);
        } catch (InterruptedException e) {
            log.error("executePullRequestImmediately pullRequestQueue.put", e);
        }
    }

    public void executeTaskLater(Runnable r, long timeDelay) {
        if (!isStopped()) {
            this.scheduledExecutorService.schedule(r, timeDelay, TimeUnit.MILLISECONDS);
        } else {
            log.warn("PullMessageServiceScheduledThread has shutdown");
        }
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    private void pullMessage(PullRequest pullRequest) {
        MQConsumerInner consumer = this.mQClientFactory.selectConsumer(pullRequest.getConsumerGroup());
        if (consumer != null) {
            DefaultMQPushConsumerImpl impl = (DefaultMQPushConsumerImpl) consumer;
            impl.pullMessage(pullRequest);
        } else {
            log.warn("No matched consumer for the PullRequest {}, drop it", pullRequest);
        }
    }

    /**
     * 进行消息拉取实际上就是从 pullRequestQueue 中获取任务
     * 能向 pullRequestQueue 队列中提交 PullRequest，PullMessageService 一共暴露了两个方法
     * @see #executePullRequestImmediately(PullRequest) 立即向队列中添加任务
     * @see #executePullRequestLater(PullRequest, long) 延迟一段时间后添加任务
     */
    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");
        /* 每次拉取消息都要检测自身状态，意味着别的线程可以通过改变 stopped 的状态来影响拉取消息的线程 */
        while (!this.isStopped()) {
            try {
                /* pullRequestQueue 队列为空会阻塞 */
                PullRequest pullRequest = this.pullRequestQueue.take();
                /* 进行消息拉取 */
                this.pullMessage(pullRequest);
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                log.error("Pull Message Service Run Method exception", e);
            }
        }

        log.info(this.getServiceName() + " service end");
    }

    @Override
    public void shutdown(boolean interrupt) {
        super.shutdown(interrupt);
        ThreadUtils.shutdownGracefully(
                this.scheduledExecutorService,
                1000,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public String getServiceName() {
        return PullMessageService.class.getSimpleName();
    }

}
