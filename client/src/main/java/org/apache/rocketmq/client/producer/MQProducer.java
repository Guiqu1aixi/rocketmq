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
package org.apache.rocketmq.client.producer;

import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.exception.RequestTimeoutException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Collection;
import java.util.List;

public interface MQProducer extends MQAdmin {
    
    void start() throws MQClientException;

    void shutdown();

    List<MessageQueue> fetchPublishMessageQueues(String topic) throws MQClientException;

    SendResult send(Message msg) throws MQClientException, RemotingException, MQBrokerException,
        InterruptedException;

    SendResult send(Message msg, long timeout) throws MQClientException,
        RemotingException, MQBrokerException, InterruptedException;

    void send(Message msg, SendCallback sendCallback) throws MQClientException,
        RemotingException, InterruptedException;

    void send(Message msg, SendCallback sendCallback, long timeout)
        throws MQClientException, RemotingException, InterruptedException;

    void sendOneway(Message msg) throws MQClientException, RemotingException,
        InterruptedException;

    SendResult send(Message msg, MessageQueue mq) throws MQClientException,
        RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Message msg, MessageQueue mq, long timeout)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void send(Message msg, MessageQueue mq, SendCallback sendCallback)
        throws MQClientException, RemotingException, InterruptedException;

    void send(Message msg, MessageQueue mq, SendCallback sendCallback, long timeout)
        throws MQClientException, RemotingException, InterruptedException;

    void sendOneway(Message msg, MessageQueue mq) throws MQClientException,
        RemotingException, InterruptedException;

    SendResult send(Message msg, MessageQueueSelector selector, Object arg)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Message msg, MessageQueueSelector selector, Object arg,
        long timeout) throws MQClientException, RemotingException, MQBrokerException,
        InterruptedException;

    void send(Message msg, MessageQueueSelector selector, Object arg,
        SendCallback sendCallback) throws MQClientException, RemotingException,
        InterruptedException;

    void send(Message msg, MessageQueueSelector selector, Object arg,
        SendCallback sendCallback, long timeout) throws MQClientException, RemotingException,
        InterruptedException;

    void sendOneway(Message msg, MessageQueueSelector selector, Object arg)
        throws MQClientException, RemotingException, InterruptedException;

    TransactionSendResult sendMessageInTransaction(Message msg,
        LocalTransactionExecuter tranExecuter, Object arg) throws MQClientException;

    TransactionSendResult sendMessageInTransaction(Message msg,
        Object arg) throws MQClientException;

    //for batch
    SendResult send(Collection<Message> msgs) throws MQClientException, RemotingException, MQBrokerException,
        InterruptedException;

    SendResult send(Collection<Message> msgs, long timeout) throws MQClientException,
        RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Collection<Message> msgs, MessageQueue mq) throws MQClientException,
        RemotingException, MQBrokerException, InterruptedException;

    SendResult send(Collection<Message> msgs, MessageQueue mq, long timeout)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;
    
    void send(Collection<Message> msgs, SendCallback sendCallback) throws MQClientException, RemotingException, MQBrokerException,
        InterruptedException;
    
    void send(Collection<Message> msgs, SendCallback sendCallback, long timeout) throws MQClientException, RemotingException,
        MQBrokerException, InterruptedException;
    
    void send(Collection<Message> msgs, MessageQueue mq, SendCallback sendCallback) throws MQClientException, RemotingException,
        MQBrokerException, InterruptedException;
    
    void send(Collection<Message> msgs, MessageQueue mq, SendCallback sendCallback, long timeout) throws MQClientException,
        RemotingException, MQBrokerException, InterruptedException;
    
    //for rpc
    Message request(Message msg, long timeout) throws RequestTimeoutException, MQClientException,
        RemotingException, MQBrokerException, InterruptedException;

    void request(Message msg, RequestCallback requestCallback, long timeout)
        throws MQClientException, RemotingException, InterruptedException, MQBrokerException;

    Message request(Message msg, MessageQueueSelector selector, Object arg,
        long timeout) throws RequestTimeoutException, MQClientException, RemotingException, MQBrokerException,
        InterruptedException;

    void request(Message msg, MessageQueueSelector selector, Object arg,
        RequestCallback requestCallback,
        long timeout) throws MQClientException, RemotingException,
        InterruptedException, MQBrokerException;

    Message request(Message msg, MessageQueue mq, long timeout)
        throws RequestTimeoutException, MQClientException, RemotingException, MQBrokerException, InterruptedException;

    void request(Message msg, MessageQueue mq, RequestCallback requestCallback, long timeout)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;
    
}
