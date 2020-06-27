package com.commonmodule.loadbalance.impl;


import com.commonmodule.loadbalance.IpPort;
import com.commonmodule.loadbalance.LoadBalance;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;


public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * @param ipPorts
     * @param key
     * @return
     */
    protected abstract IpPort doSelect(List<IpPort> ipPorts, String key);

    @Override
    public IpPort select(final List<IpPort> ipPorts, final String key) {
        if (CollectionUtils.isEmpty(ipPorts)) {
            return null;
        }
        if (ipPorts.size() == 1) {
            return ipPorts.get(0);
        }
        return doSelect(ipPorts, key);
    }

}
