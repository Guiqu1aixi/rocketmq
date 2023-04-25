package org.apache.rocketmq.store;

import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.rocketmq.store.stats.BrokerStatsManager;

public class CommitLogTest {

    public static void main(String[] args) throws Exception {
        CommitLog log = new CommitLog(
            new DefaultMessageStore(
                    new MessageStoreConfig(),
                    new BrokerStatsManager(""),
                    (MessageArrivingListener) (topic, queueId, logicOffset, tagsCode, msgStoreTime, filterBitMap, properties) -> {},
                    new BrokerConfig()
            )
        );

        CommitLog.GroupCommitService service = log.new GroupCommitService();
        Runnable runnable = () -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }

                service.putRequest(new CommitLog.GroupCommitRequest(100));
            }
        };
        new Thread(runnable).start();

        service.start();
    }

}
