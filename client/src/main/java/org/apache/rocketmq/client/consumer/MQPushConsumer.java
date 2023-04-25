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
package org.apache.rocketmq.client.consumer;

import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;

/**
 * Push consumer
 */
public interface MQPushConsumer extends MQConsumer {

    /**
     * Start the consumer
     */
    void start() throws MQClientException;

    /**
     * Shutdown the consumer
     */
    void shutdown();

    /**
     * Register the message listener
     */
    @Deprecated
    void registerMessageListener(MessageListener messageListener);

    /**
     * 注册并发消息时间监听器
     */
    void registerMessageListener(MessageListenerConcurrently messageListener);

    /**
     * 注册顺序消息时间监听器
     */
    void registerMessageListener(MessageListenerOrderly messageListener);

    /**
     * 基于 Topic 订阅消息
     * Subscribe some topic
     *
     * @param subExpression subscription expression.it only support or operation such as "tag1 || tag2 || tag3" <br> if
     * null or * expression,meaning subscribe
     * all
     */
    void subscribe(String topic, String subExpression) throws MQClientException;

    /**
     * This method will be removed in the version 5.0.0,because filterServer was removed,and method <code>subscribe(final String topic, final MessageSelector messageSelector)</code>
     * is recommended.
     *
     * Subscribe some topic
     *
     * @param fullClassName full class name,must extend org.apache.rocketmq.common.filter. MessageFilter
     * @param filterClassSource class source code,used UTF-8 file encoding,must be responsible for your code safety
     */
    @Deprecated
    void subscribe(String topic, String fullClassName, String filterClassSource) throws MQClientException;

    /**
     * Subscribe some topic with selector.
     * <p>
     * This interface also has the ability of {@link #subscribe(String, String)},
     * and, support other message selection, such as {@link org.apache.rocketmq.common.filter.ExpressionType#SQL92}.
     * </p>
     * <p/>
     * <p>
     * Choose Tag: {@link MessageSelector#byTag(java.lang.String)}
     * </p>
     * <p/>
     * <p>
     * Choose SQL92: {@link MessageSelector#bySql(java.lang.String)}
     * </p>
     *
     * @param selector message selector({@link MessageSelector}), can be null.
     */
    void subscribe(String topic, MessageSelector selector) throws MQClientException;

    /**
     * 取消订阅
     * Unsubscribe consumption some topic
     *
     * @param topic message topic
     */
    void unsubscribe(String topic);

    /**
     * Update the consumer thread pool size Dynamically
     */
    void updateCorePoolSize(int corePoolSize);

    /**
     * Suspend the consumption
     */
    void suspend();

    /**
     * Resume the consumption
     */
    void resume();

}
