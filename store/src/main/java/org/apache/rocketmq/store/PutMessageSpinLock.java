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
package org.apache.rocketmq.store;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spin lock Implementation to put message, suggest using this with low race conditions
 */
public class PutMessageSpinLock implements PutMessageLock {

    /* true: Can lock, false : in lock */
    private final AtomicBoolean putMessageSpinLock = new AtomicBoolean(true);

    /**
     * 自旋锁
     * 被阻塞的线程一直在尝试
     */
    @Override
    public void lock() {
        boolean flag;

        do {
            flag = putMessageSpinLock.compareAndSet(true, false);
        }
        while (!flag);
    }

    @Override
    public void unlock() {
        putMessageSpinLock.compareAndSet(false, true);
    }

}
