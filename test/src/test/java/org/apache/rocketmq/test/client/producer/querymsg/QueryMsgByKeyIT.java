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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.test.client.producer.querymsg;

import org.apache.log4j.Logger;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.test.base.BaseConf;
import org.apache.rocketmq.test.client.rmq.RMQNormalProducer;
import org.apache.rocketmq.test.factory.MQMessageFactory;
import org.apache.rocketmq.test.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class QueryMsgByKeyIT extends BaseConf {
    private static Logger logger = Logger.getLogger(QueryMsgByKeyIT.class);
    private RMQNormalProducer producer = null;
    private String topic = null;

    @Before
    public void setUp() {
        topic = initTopic();
        logger.info(String.format("use topic: %s;", topic));
        producer = getProducer(nsAddr, topic);
    }

    @After
    public void tearDown() {
        shutdown();
    }

    @Test
    public void testQueryMsg() {
        int msgSize = 20;
        String key = "jueyin";
        long begin = System.currentTimeMillis();
        List<Object> msgs = MQMessageFactory.getKeyMsg(topic, key, msgSize);
        producer.send(msgs);
        Assert.assertEquals("Not all are sent", msgSize, producer.getAllUndupMsgBody().size());

        List<MessageExt> queryMsgs = null;
        try {
            TestUtils.waitForMoment(500 * 3);
            queryMsgs = producer.getProducer().queryMessage(topic, key, msgSize, begin - 5000,
                System.currentTimeMillis() + 5000).getMessageList();
        } catch (Exception e) {
        }

        assertThat(queryMsgs).isNotNull();
        assertThat(queryMsgs.size()).isEqualTo(msgSize);
    }

    @Test
    public void testQueryMax() {
        int msgSize = 500;
        int max = 64 * brokerNum;
        String key = "jueyin";
        long begin = System.currentTimeMillis();
        List<Object> msgs = MQMessageFactory.getKeyMsg(topic, key, msgSize);
        producer.send(msgs);
        Assert.assertEquals("Not all are sent", msgSize, producer.getAllUndupMsgBody().size());

        List<MessageExt> queryMsgs = null;
        try {
            queryMsgs = producer.getProducer().queryMessage(topic, key, msgSize, begin - 15000,
                System.currentTimeMillis() + 15000).getMessageList();

            int i = 3;
            while (queryMsgs == null || queryMsgs.size() != brokerNum) {
                i--;
                queryMsgs = producer.getProducer().queryMessage(topic, key, msgSize, begin - 15000,
                    System.currentTimeMillis() + 15000).getMessageList();
                TestUtils.waitForMoment(1000);

                if (i == 0 || (queryMsgs != null && queryMsgs.size() == max)) {
                    break;
                }
            }
        } catch (Exception e) {
        }

        assertThat(queryMsgs).isNotNull();
        assertThat(queryMsgs.size()).isEqualTo(max);
    }
}
