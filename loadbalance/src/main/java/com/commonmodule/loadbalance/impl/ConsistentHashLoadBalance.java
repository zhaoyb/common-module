/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.commonmodule.loadbalance.impl;


import com.commonmodule.loadbalance.IpPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private static final int VIRTUAL_NODE_NUM = 40;

    private ConcurrentMap<Integer, TreeMap<Long, IpPort>> virtualNodes = new ConcurrentHashMap<>(16);

    @Override
    protected IpPort doSelect(final List<IpPort> ipPorts, final String key) {
        int hashcode = ipPorts.hashCode();
        TreeMap<Long, IpPort> treeMap = virtualNodes.get(hashcode);
        if (treeMap == null) {
            for (IpPort ipPort : ipPorts) {
                for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                    byte[] digest = md5(ipPort.toIdentityString() + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        treeMap.put(m, ipPort);
                    }
                }
            }
            virtualNodes.put(hashcode, treeMap);
        }

        byte[] digest = md5(key);
        long hash = hash(digest, 0);
        SortedMap<Long, IpPort> lastRing = treeMap.tailMap(hash);
        if (!lastRing.isEmpty()) {
            return lastRing.get(lastRing.firstKey());
        }
        return treeMap.firstEntry().getValue();
    }

    private long hash(byte[] digest, int number) {
        return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                | (digest[number * 4] & 0xFF))
                & 0xFFFFFFFFL;
    }

    private byte[] md5(String value) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        md5.reset();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        md5.update(bytes);
        return md5.digest();
    }

}
