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
package org.apache.rocketmq.remoting.protocol;

import com.alibaba.fastjson.JSON;

import java.nio.charset.StandardCharsets;

public abstract class RemotingSerializable {
    
    public static byte[] encode(Object obj) {
        String json = toJson(obj, false);
        if (json != null) {
            return json.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    public static String toJson(Object obj, boolean prettyFormat) {
        return JSON.toJSONString(obj, prettyFormat);
    }

    public static <T> T decode(byte[] data, Class<T> classOfT) {
        String json = new String(data, StandardCharsets.UTF_8);
        return fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return JSON.parseObject(json, classOfT);
    }

    public byte[] encode() {
        String json = this.toJson();
        if (json != null) {
            return json.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    public String toJson() {
        return toJson(false);
    }

    public String toJson(boolean prettyFormat) {
        return toJson(this, prettyFormat);
    }
    
}
