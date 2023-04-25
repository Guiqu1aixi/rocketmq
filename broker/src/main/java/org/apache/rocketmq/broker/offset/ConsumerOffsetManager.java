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
package org.apache.rocketmq.broker.offset;

import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.broker.BrokerPathConfigHelper;
import org.apache.rocketmq.common.ConfigManager;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConsumerOffsetManager extends ConfigManager {

    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.BROKER_LOGGER_NAME);

    private static final String TOPIC_GROUP_SEPARATOR = "@";

    /**
     * ConcurrentMap<topic@group, ConcurrentMap<QueueId, offset>>
     */
    private ConcurrentMap<String, ConcurrentMap<Integer, Long>> offsetTable =
        new ConcurrentHashMap<>(512);
    private transient BrokerController brokerController;

    public ConsumerOffsetManager() {
    }

    public ConsumerOffsetManager(BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    public void scanUnsubscribedTopic() {
        Iterator<Entry<String, ConcurrentMap<Integer, Long>>> it = this.offsetTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ConcurrentMap<Integer, Long>> next = it.next();
            String topicAtGroup = next.getKey();
            String[] arrays = topicAtGroup.split(TOPIC_GROUP_SEPARATOR);
            if (arrays.length == 2) {
                String topic = arrays[0];
                String group = arrays[1];

                if (null == brokerController.getConsumerManager().findSubscriptionData(group, topic)
                    && this.offsetBehindMuchThanData(topic, next.getValue())) {
                    it.remove();
                    log.warn("remove topic offset, {}", topicAtGroup);
                }
            }
        }
    }

    private boolean offsetBehindMuchThanData(String topic, ConcurrentMap<Integer, Long> table) {
        Iterator<Entry<Integer, Long>> it = table.entrySet().iterator();
        boolean result = !table.isEmpty();

        while (it.hasNext() && result) {
            Entry<Integer, Long> next = it.next();
            long minOffsetInStore = this.brokerController.getMessageStore().getMinOffsetInQueue(topic, next.getKey());
            long offsetInPersist = next.getValue();
            result = offsetInPersist <= minOffsetInStore;
        }

        return result;
    }

    public Set<String> whichTopicByConsumer(String group) {
        Set<String> topics = new HashSet<>();

        Iterator<Entry<String, ConcurrentMap<Integer, Long>>> it = this.offsetTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ConcurrentMap<Integer, Long>> next = it.next();
            String topicAtGroup = next.getKey();
            String[] arrays = topicAtGroup.split(TOPIC_GROUP_SEPARATOR);
            if (arrays.length == 2) {
                if (group.equals(arrays[1])) {
                    topics.add(arrays[0]);
                }
            }
        }

        return topics;
    }

    public Set<String> whichGroupByTopic(String topic) {
        Set<String> groups = new HashSet<>();

        Iterator<Entry<String, ConcurrentMap<Integer, Long>>> it = this.offsetTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, ConcurrentMap<Integer, Long>> next = it.next();
            String topicAtGroup = next.getKey();
            String[] arrays = topicAtGroup.split(TOPIC_GROUP_SEPARATOR);
            if (arrays.length == 2) {
                if (topic.equals(arrays[0])) {
                    groups.add(arrays[1]);
                }
            }
        }

        return groups;
    }

    public void commitOffset(String clientHost, String group, String topic, int queueId,
        long offset) {
        /* topic@group */
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        this.commitOffset(clientHost, key, queueId, offset);
    }

    private void commitOffset(String clientHost, String key, int queueId, long offset) {
        ConcurrentMap<Integer, Long> map = this.offsetTable.get(key);
        if (null == map) {
            map = new ConcurrentHashMap<>(32);
            map.put(queueId, offset);
            this.offsetTable.put(key, map);
        } else {
            Long storeOffset = map.put(queueId, offset);
            if (storeOffset != null && offset < storeOffset) {
                log.warn("[NOTIFYME]update consumer offset less than store. clientHost={}, key={}, queueId={}, requestOffset={}, storeOffset={}",
                    clientHost, key, queueId, offset, storeOffset
                );
            }
        }
    }

    public long queryOffset(String group, String topic, int queueId) {
        /* topic@group */
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        ConcurrentMap<Integer, Long> map = this.offsetTable.get(key);
        if (null != map) {
            Long offset = map.get(queueId);
            if (offset != null)
                return offset;
        }

        return -1;
    }

    public String encode() {
        return this.encode(false);
    }

    @Override
    public String configFilePath() {
        return BrokerPathConfigHelper.getConsumerOffsetPath(
            this.brokerController.getMessageStoreConfig().getStorePathRootDir()
        );
    }

    @Override
    public void decode(String jsonString) {
        if (jsonString != null) {
            ConsumerOffsetManager obj = RemotingSerializable.fromJson(jsonString, ConsumerOffsetManager.class);
            if (obj != null) {
                this.offsetTable = obj.offsetTable;
            }
        }
    }

    public String encode(boolean prettyFormat) {
        return RemotingSerializable.toJson(this, prettyFormat);
    }

    public ConcurrentMap<String, ConcurrentMap<Integer, Long>> getOffsetTable() {
        return offsetTable;
    }

    public void setOffsetTable(ConcurrentHashMap<String, ConcurrentMap<Integer, Long>> offsetTable) {
        this.offsetTable = offsetTable;
    }

    public Map<Integer, Long> queryMinOffsetInAllGroup(String topic, String filterGroups) {
        Map<Integer, Long> queueMinOffset = new HashMap<>();
        Set<String> topicGroups = this.offsetTable.keySet();
        if (!UtilAll.isBlank(filterGroups)) {
            for (String group : filterGroups.split(",")) {
                topicGroups.removeIf(s -> group.equals(s.split(TOPIC_GROUP_SEPARATOR)[1]));
            }
        }

        for (Map.Entry<String, ConcurrentMap<Integer, Long>> offSetEntry : this.offsetTable.entrySet()) {
            String topicGroup = offSetEntry.getKey();
            String[] topicGroupArr = topicGroup.split(TOPIC_GROUP_SEPARATOR);
            if (topic.equals(topicGroupArr[0])) {
                for (Entry<Integer, Long> entry : offSetEntry.getValue().entrySet()) {
                    long minOffset = this.brokerController.getMessageStore().getMinOffsetInQueue(topic, entry.getKey());
                    if (entry.getValue() >= minOffset) {
                        Long offset = queueMinOffset.get(entry.getKey());
                        if (offset == null) {
                            queueMinOffset.put(entry.getKey(), Math.min(Long.MAX_VALUE, entry.getValue()));
                        } else {
                            queueMinOffset.put(entry.getKey(), Math.min(entry.getValue(), offset));
                        }
                    }
                }
            }

        }
        return queueMinOffset;
    }

    public Map<Integer, Long> queryOffset(String group, String topic) {
        /* topic@group */
        String key = topic + TOPIC_GROUP_SEPARATOR + group;
        return this.offsetTable.get(key);
    }

    public void cloneOffset(String srcGroup, String destGroup, String topic) {
        ConcurrentMap<Integer, Long> offsets = this.offsetTable.get(topic + TOPIC_GROUP_SEPARATOR + srcGroup);
        if (offsets != null) {
            this.offsetTable.put(
                topic + TOPIC_GROUP_SEPARATOR + destGroup,
                new ConcurrentHashMap<>(offsets)
            );
        }
    }

}
