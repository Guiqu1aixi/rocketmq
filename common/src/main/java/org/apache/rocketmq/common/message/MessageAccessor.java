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

package org.apache.rocketmq.common.message;

import java.util.Map;

public class MessageAccessor {

    public static void clearProperty(Message msg, String name) {
        msg.clearProperty(name);
    }

    public static void setProperties(Message msg, Map<String, String> properties) {
        msg.setProperties(properties);
    }

    public static void setTransferFlag(Message msg, String unit) {
        putProperty(msg, MessageConst.PROPERTY_TRANSFER_FLAG, unit);
    }

    public static void putProperty(Message msg, String name, String value) {
        msg.putProperty(name, value);
    }

    public static String getTransferFlag(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_TRANSFER_FLAG);
    }

    public static void setCorrectionFlag(Message msg, String unit) {
        putProperty(msg, MessageConst.PROPERTY_CORRECTION_FLAG, unit);
    }

    public static String getCorrectionFlag(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_CORRECTION_FLAG);
    }

    public static void setOriginMessageId(Message msg, String originMessageId) {
        putProperty(msg, MessageConst.PROPERTY_ORIGIN_MESSAGE_ID, originMessageId);
    }

    public static String getOriginMessageId(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID);
    }

    public static void setMQ2Flag(Message msg, String flag) {
        putProperty(msg, MessageConst.PROPERTY_MQ2_FLAG, flag);
    }

    public static String getMQ2Flag(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_MQ2_FLAG);
    }

    public static void setReconsumeTime(Message msg, String reconsumeTimes) {
        putProperty(msg, MessageConst.PROPERTY_RECONSUME_TIME, reconsumeTimes);
    }

    public static String getReconsumeTime(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_RECONSUME_TIME);
    }

    public static void setMaxReconsumeTimes(Message msg, String maxReconsumeTimes) {
        putProperty(msg, MessageConst.PROPERTY_MAX_RECONSUME_TIMES, maxReconsumeTimes);
    }

    public static String getMaxReconsumeTimes(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_MAX_RECONSUME_TIMES);
    }

    public static void setConsumeStartTimeStamp(Message msg, String propertyConsumeStartTimeStamp) {
        putProperty(msg, MessageConst.PROPERTY_CONSUME_START_TIMESTAMP, propertyConsumeStartTimeStamp);
    }

    public static String getConsumeStartTimeStamp(Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_CONSUME_START_TIMESTAMP);
    }

    public static Message cloneMessage(Message msg) {
        Message newMsg = new Message(msg.getTopic(), msg.getBody());
        newMsg.setFlag(msg.getFlag());
        newMsg.setProperties(msg.getProperties());
        return newMsg;
    }

}
