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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final int recyclePeriod = 60000;

    private ConcurrentMap<Integer, ConcurrentMap<String, WeightedRoundRobin>> ipPortWeightMap = new ConcurrentHashMap<>(16);
    private AtomicBoolean updateLock = new AtomicBoolean();

    @Override
    protected IpPort doSelect(final List<IpPort> ipPorts, final String key) {
        int hashCode = ipPorts.hashCode();
        ConcurrentMap<String, WeightedRoundRobin> map = ipPortWeightMap.get(hashCode);
        if (map == null) {
            ipPortWeightMap.putIfAbsent(hashCode, new ConcurrentHashMap<>(16));
            map = ipPortWeightMap.get(key);
        }
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        IpPort selectedIpPort = null;
        WeightedRoundRobin selectedWRR = null;
        for (IpPort ipPort : ipPorts) {
            String rKey = ipPort.toIdentityString();
            WeightedRoundRobin weightedRoundRobin = map.get(rKey);
            int weight = ipPort.getWeight();
            if (weightedRoundRobin == null) {
                weightedRoundRobin = new WeightedRoundRobin();
                weightedRoundRobin.setWeight(weight);
                map.putIfAbsent(rKey, weightedRoundRobin);
            }
            if (weight != weightedRoundRobin.getWeight()) {
                //weight changed
                weightedRoundRobin.setWeight(weight);
            }
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedIpPort = ipPort;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;
        }
        if (!updateLock.get() && ipPorts.size() != map.size()) {
            if (updateLock.compareAndSet(false, true)) {
                try {
                    // copy -> modify -> update reference
                    ConcurrentMap<String, WeightedRoundRobin> newMap = new ConcurrentHashMap<>(map);
                    newMap.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > recyclePeriod);
                    ipPortWeightMap.put(hashCode, newMap);
                } finally {
                    updateLock.set(false);
                }
            }
        }
        if (selectedIpPort != null) {
            selectedWRR.sel(totalWeight);
            return selectedIpPort;
        }
        // should not happen here
        return ipPorts.get(0);
    }

    /**
     * The type Weighted round robin.
     */
    protected static class WeightedRoundRobin {

        private int weight;

        private AtomicLong current = new AtomicLong(0);

        private long lastUpdate;

        /**
         * Gets weight.
         *
         * @return the weight
         */
        int getWeight() {
            return weight;
        }

        /**
         * Sets weight.
         *
         * @param weight the weight
         */
        void setWeight(final int weight) {
            this.weight = weight;
            current.set(0);
        }

        /**
         * Increase current long.
         *
         * @return the long
         */
        long increaseCurrent() {
            return current.addAndGet(weight);
        }

        /**
         * Sel.
         *
         * @param total the total
         */
        void sel(final int total) {
            current.addAndGet(-1 * total);
        }

        /**
         * Gets last update.
         *
         * @return the last update
         */
        long getLastUpdate() {
            return lastUpdate;
        }

        /**
         * Sets last update.
         *
         * @param lastUpdate the last update
         */
        void setLastUpdate(final long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }

}
