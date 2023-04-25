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
package org.apache.rocketmq.client.consumer.store;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Map;
import java.util.Set;

/**
 * 消费进度持久化接口
 * 集群消费，广播消费两种持久化策略
 *
 * Offset store interface
 */
public interface OffsetStore {

    /**
     * 从消息进度存储文件加载消息进度到内存
     * Load
     */
    void load() throws MQClientException;

    /**
     * 更新内存中的消息进度
     * Update the offset,store it in memory
     */
    void updateOffset(MessageQueue mq, long offset, boolean increaseOnly);

    /**
     * 读取消息消费进度
     * Get offset from local storage
     * @return The fetched offset
     */
    long readOffset(MessageQueue mq, ReadOffsetType type);

    /**
     * 持久化指定消息队列进度到磁盘
     * Persist all offsets,may be in local storage or remote name server
     */
    void persistAll(Set<MessageQueue> mqs);

    /**
     * Persist the offset,may be in local storage or remote name server
     */
    void persist(MessageQueue mq);

    /**
     * Remove offset
     */
    void removeOffset(MessageQueue mq);

    /**
     * @return The cloned offset table of given topic
     */
    Map<MessageQueue, Long> cloneOffsetTable(String topic);

    /**
     * 更新存储在 Broker 端的消息消费进度，使用集群模式
     */
    void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway)
        throws RemotingException, MQBrokerException,
               InterruptedException, MQClientException;

}
