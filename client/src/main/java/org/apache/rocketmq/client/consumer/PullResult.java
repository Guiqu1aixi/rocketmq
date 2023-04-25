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

import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class PullResult {

    /**
     * 标志拉取结果
     */
    private final PullStatus pullStatus;

    /**
     * 下次拉取的起始偏移量
     */
    private final long nextBeginOffset;

    /**
     * 本次拉取消息的最小偏移量
     */
    private final long minOffset;

    /**
     * 本次拉取消息的最大偏移量
     */
    private final long maxOffset;

    /**
     * 拉取到的消息集合
     */
    private List<MessageExt> msgFoundList;

    public PullResult(PullStatus pullStatus, long nextBeginOffset, long minOffset, long maxOffset,
        List<MessageExt> msgFoundList) {
        super();
        this.pullStatus = pullStatus;
        this.nextBeginOffset = nextBeginOffset;
        this.minOffset = minOffset;
        this.maxOffset = maxOffset;
        this.msgFoundList = msgFoundList;
    }

    public PullStatus getPullStatus() {
        return pullStatus;
    }

    public long getNextBeginOffset() {
        return nextBeginOffset;
    }

    public long getMinOffset() {
        return minOffset;
    }

    public long getMaxOffset() {
        return maxOffset;
    }

    public List<MessageExt> getMsgFoundList() {
        return msgFoundList;
    }

    public void setMsgFoundList(List<MessageExt> msgFoundList) {
        this.msgFoundList = msgFoundList;
    }

    @Override
    public String toString() {
        return "PullResult [pullStatus=" + pullStatus + ", nextBeginOffset=" + nextBeginOffset
            + ", minOffset=" + minOffset + ", maxOffset=" + maxOffset + ", msgFoundList="
            + (msgFoundList == null ? 0 : msgFoundList.size()) + "]";
    }

}
