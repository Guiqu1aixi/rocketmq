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
package org.apache.rocketmq.tools.admin;

import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.AclConfig;
import org.apache.rocketmq.common.PlainAccessConfig;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.*;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.remoting.exception.*;
import org.apache.rocketmq.tools.admin.api.MessageTrack;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public interface MQAdminExt extends MQAdmin {
    void start() throws MQClientException;

    void shutdown();

    void updateBrokerConfig(String brokerAddr, Properties properties) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException;

    Properties getBrokerConfig(String brokerAddr) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException;

    void createAndUpdateTopicConfig(String addr,
        TopicConfig config) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void createAndUpdatePlainAccessConfig(String addr, PlainAccessConfig plainAccessConfig) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void deletePlainAccessConfig(String addr, String accessKey) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void updateGlobalWhiteAddrConfig(String addr, String globalWhiteAddrs)throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    ClusterAclVersionInfo examineBrokerClusterAclVersionInfo(String addr) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    AclConfig examineBrokerClusterAclConfig(String addr) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void createAndUpdateSubscriptionGroupConfig(String addr,
        SubscriptionGroupConfig config) throws RemotingException,
        MQBrokerException, InterruptedException, MQClientException;

    SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group);

    TopicConfig examineTopicConfig(String addr, String topic);

    TopicStatsTable examineTopicStats(
        String topic) throws RemotingException, MQClientException, InterruptedException,
        MQBrokerException;

    TopicList fetchAllTopicList() throws RemotingException, MQClientException, InterruptedException;

    TopicList fetchTopicsByCLuster(
        String clusterName) throws RemotingException, MQClientException, InterruptedException;

    KVTable fetchBrokerRuntimeStats(
        String brokerAddr) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, MQBrokerException;

    ConsumeStats examineConsumeStats(
        String consumerGroup) throws RemotingException, MQClientException, InterruptedException,
        MQBrokerException;

    ConsumeStats examineConsumeStats(String consumerGroup,
        String topic) throws RemotingException, MQClientException,
        InterruptedException, MQBrokerException;

    ClusterInfo examineBrokerClusterInfo() throws InterruptedException, MQBrokerException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException;

    TopicRouteData examineTopicRouteInfo(
        String topic) throws RemotingException, MQClientException, InterruptedException;

    ConsumerConnection examineConsumerConnectionInfo(String consumerGroup) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException, RemotingException,
        MQClientException;

    ProducerConnection examineProducerConnectionInfo(String producerGroup,
        String topic) throws RemotingException,
        MQClientException, InterruptedException, MQBrokerException;

    List<String> getNameServerAddressList();

    int wipeWritePermOfBroker(String namesrvAddr, String brokerName) throws RemotingCommandException,
        RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQClientException;

    void putKVConfig(String namespace, String key, String value);

    String getKVConfig(String namespace,
        String key) throws RemotingException, MQClientException, InterruptedException;

    KVTable getKVListByNamespace(
        String namespace) throws RemotingException, MQClientException, InterruptedException;

    void deleteTopicInBroker(Set<String> addrs, String topic) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void deleteTopicInNameServer(Set<String> addrs,
        String topic) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void deleteSubscriptionGroup(String addr, String groupName) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void createAndUpdateKvConfig(String namespace, String key,
        String value) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    void deleteKvConfig(String namespace, String key) throws RemotingException, MQBrokerException, InterruptedException,
        MQClientException;

    List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp, boolean force)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException;

    Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp, boolean isForce)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException;

    void resetOffsetNew(String consumerGroup, String topic, long timestamp) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group,
        String clientAddr) throws RemotingException,
        MQBrokerException, InterruptedException, MQClientException;

    void createOrUpdateOrderConf(String key, String value,
        boolean isCluster) throws RemotingException, MQBrokerException,
        InterruptedException, MQClientException;

    GroupList queryTopicConsumeByWho(String topic) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, MQBrokerException, RemotingException, MQClientException;

    List<QueueTimeSpan> queryConsumeTimeSpan(String topic,
        String group) throws InterruptedException, MQBrokerException,
        RemotingException, MQClientException;

    boolean cleanExpiredConsumerQueue(String cluster) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    boolean cleanExpiredConsumerQueueByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack)
        throws RemotingException, MQClientException, InterruptedException;

    ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup,
        String clientId,
        String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException;

    ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup,
        String clientId,
        String topic,
        String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException;

    List<MessageTrack> messageTrackDetail(
        MessageExt msg) throws RemotingException, MQClientException, InterruptedException,
        MQBrokerException;

    void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline) throws RemotingException,
        MQClientException, InterruptedException, MQBrokerException;

    BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException,
        InterruptedException;

    Set<String> getClusterList(String topic) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder,
        long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, MQClientException, InterruptedException;

    Set<String> getTopicClusterList(
        String topic) throws InterruptedException, MQBrokerException, MQClientException, RemotingException;

    SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr,
        long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
        RemotingConnectException, MQBrokerException;

    TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr,
        long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
        RemotingConnectException, MQBrokerException;

    void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq,
        long offset) throws RemotingException, InterruptedException, MQBrokerException;

    /**
     * Update name server config.
     * <br>
     * Command Code : RequestCode.UPDATE_NAMESRV_CONFIG
     *
     * <br> If param(nameServers) is null or empty, will use name servers from ns!
     */
    void updateNameServerConfig(Properties properties,
        List<String> nameServers) throws InterruptedException, RemotingConnectException,
        UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException,
        MQClientException, MQBrokerException;

    /**
     * Get name server config.
     * <br>
     * Command Code : RequestCode.GET_NAMESRV_CONFIG
     * <br> If param(nameServers) is null or empty, will use name servers from ns!
     *
     * @return The fetched name server config
     */
    Map<String, Properties> getNameServerConfig(List<String> nameServers) throws InterruptedException,
        RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException,
        MQClientException, UnsupportedEncodingException;

    /**
     * query consume queue data
     *
     * @param brokerAddr broker ip address
     * @param topic topic
     * @param queueId id of queue
     * @param index start offset
     * @param count how many
     * @param consumerGroup group
     */
    QueryConsumeQueueResponseBody queryConsumeQueue(String brokerAddr,
        String topic, int queueId,
        long index, int count, String consumerGroup)
        throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException;

    boolean resumeCheckHalfMessage(String msgId)
            throws RemotingException, MQClientException, InterruptedException, MQBrokerException;

    boolean resumeCheckHalfMessage(String topic, String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException;
}
