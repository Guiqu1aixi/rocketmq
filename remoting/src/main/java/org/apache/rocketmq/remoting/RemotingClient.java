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
package org.apache.rocketmq.remoting;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

public interface RemotingClient extends RemotingService {

    void updateNameServerAddressList(List<String> addrs);

    List<String> getNameServerAddressList();

    RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis)
            throws InterruptedException,
            RemotingConnectException,
            RemotingSendRequestException,
            RemotingTimeoutException;

    void invokeAsync(String addr, RemotingCommand request, long timeoutMillis,
                     InvokeCallback invokeCallback)
            throws InterruptedException,
            RemotingConnectException,
            RemotingTooMuchRequestException,
            RemotingTimeoutException,
            RemotingSendRequestException;

    void invokeOneway(String addr, RemotingCommand request, long timeoutMillis)
            throws InterruptedException,
            RemotingConnectException,
            RemotingTooMuchRequestException,
            RemotingTimeoutException,
            RemotingSendRequestException;

    void registerProcessor(int requestCode, NettyRequestProcessor processor,
                           ExecutorService executor);

    void setCallbackExecutor(ExecutorService callbackExecutor);

    ExecutorService getCallbackExecutor();

    boolean isChannelWritable(String addr);

}
