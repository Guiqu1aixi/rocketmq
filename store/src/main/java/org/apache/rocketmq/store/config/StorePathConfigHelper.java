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
package org.apache.rocketmq.store.config;

import org.apache.rocketmq.store.DefaultMessageStore;

import java.io.File;

/**
 * 文件路径获取
 */
public class StorePathConfigHelper {

    public static String getStorePathConsumeQueue(String rootDir) {
        return rootDir + File.separator + "consumequeue";
    }

    public static String getStorePathConsumeQueueExt(String rootDir) {
        return rootDir + File.separator + "consumequeue_ext";
    }

    public static String getStorePathIndex(String rootDir) {
        return rootDir + File.separator + "index";
    }

    /**
     * 获取文件监测点文件路径
     *
     * 存储内容：
     * CommitLog 文件最后一次刷盘时间戳、ConsumerQueue 最后一次刷盘时间戳、 Index 索引最后一次刷盘时间戳
     *
     * @see DefaultMessageStore#load
     * @see DefaultMessageStore#destroy
     *
     * @param rootDir ${ROCKET_HOME}/store
     */
    public static String getStoreCheckpoint(String rootDir) {
        return rootDir + File.separator + "checkpoint";
    }

    /**
     * 获取 "abort" 文件的物理路径
     * 如果 Broker 处于关闭状态存在这个文件则说明 Broker 非正常关闭
     * 该文件会在启动时默认创建，正常退出时自动删除
     *
     * @see DefaultMessageStore#destroy
     * @see DefaultMessageStore#shutdown
     *
     * @param rootDir ${ROCKET_HOME}/store
     */
    public static String getAbortFile(String rootDir) {
        return rootDir + File.separator + "abort";
    }

    public static String getLockFile(String rootDir) {
        return rootDir + File.separator + "lock";
    }

    public static String getDelayOffsetStorePath(String rootDir) {
        return rootDir + File.separator + "config" + File.separator + "delayOffset.json";
    }

    public static String getTranStateTableStorePath(String rootDir) {
        return rootDir + File.separator + "transaction" + File.separator + "statetable";
    }

    public static String getTranRedoLogStorePath(String rootDir) {
        return rootDir + File.separator + "transaction" + File.separator + "redolog";
    }

}
