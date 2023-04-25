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
package org.apache.rocketmq.store.util;

import com.sun.jna.*;

/**
 * Java JNA 机制
 * 相比 JNI 而言，性能会有下降，但是更加易用
 * 接口不需要实现，但是依然可以调用操作系统提供的调用
 */
public interface LibC extends Library {

    LibC INSTANCE = (LibC) Native.loadLibrary(Platform.isWindows() ? "msvcrt" : "c", LibC.class);

    /**
     * 表示应用程序希望很快访问此地址范围
     */
    int MADV_WILLNEED = 3;

    /**
     * 表示应用程序不希望很快访问此地址范围
     */
    int MADV_DONTNEED = 4;


    int MCL_CURRENT = 1;
    int MCL_FUTURE = 2;
    int MCL_ONFAULT = 4;

    /* sync memory asynchronously */
    int MS_ASYNC = 0x0001;
    /* invalidate mappings & caches */
    int MS_INVALIDATE = 0x0002;
    /* synchronous memory sync */
    int MS_SYNC = 0x0004;

    /**
     * 锁定内存
     * 防止操作系统将物理内存交换到硬盘
     */
    int mlock(Pointer var1, NativeLong var2);

    /**
     * 解锁内存
     */
    int munlock(Pointer var1, NativeLong var2);

    int madvise(Pointer var1, NativeLong var2, int var3);

    Pointer memset(Pointer p, int v, long len);

    int mlockall(int flags);

    int msync(Pointer p, NativeLong length, int flags);

}
