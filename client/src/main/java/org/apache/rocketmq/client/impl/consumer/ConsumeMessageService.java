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

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;

import java.util.List;

/**
 * 消息消费过程
 * PullMessageService 负责消息拉取，从远程服务器上拉取到消息后，将消息存入 ProcessQueue 消息处理队列中
 * 然后调用 {@link #submitConsumeRequest} 方法进行消息消费
 * 使用线程池消费，确保了消息拉取与消息消费的解耦
 */
public interface ConsumeMessageService {

    void start();

    void shutdown(long awaitTerminateMillis);

    void updateCorePoolSize(int corePoolSize);

    void incCorePoolSize();

    void decCorePoolSize();

    int getCorePoolSize();

    /**
     * 直接消费消息，主要用于通过管理命令接收消息
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(MessageExt msg, String brokerName);

    /**
     *
     * @param msgs             消息体集合，默认拉取32条
     * @param processQueue     消息处理队列
     * @param messageQueue     消息所属消费队列
     * @param dispathToConsume 是否转发到消费线程池，并发消费时忽略
     */
    void submitConsumeRequest(
        List<MessageExt> msgs,
        ProcessQueue processQueue,
        MessageQueue messageQueue,
        boolean dispathToConsume);

}
