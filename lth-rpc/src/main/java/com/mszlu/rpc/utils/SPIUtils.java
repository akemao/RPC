package com.mszlu.rpc.utils;

import com.mszlu.rpc.balance.LoadBalance;
import com.mszlu.rpc.balance.RoundRobinLoadBalance;
import com.mszlu.rpc.exception.LthRpcException;

import java.util.ServiceLoader;

public class SPIUtils {

    public static LoadBalance loadBalance(String name) {
        ServiceLoader<LoadBalance> load = ServiceLoader.load(LoadBalance.class);
        for (LoadBalance loadBalance : load) {
            if (loadBalance.name().equals(name)) {
                // 如果是轮询负载均衡器，返回单例实例
                if (loadBalance instanceof RoundRobinLoadBalance) {
                    return RoundRobinLoadBalance.getInstance();
                }
                return loadBalance;
            }
        }
        throw new LthRpcException("无对应的负载均衡器");
    }
}
