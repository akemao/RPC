package com.mszlu.rpc.balance;

import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class RandomLoadBalance implements LoadBalance{
    @Override
    public String name() {
        return "random";
    }

    @Override
    public InetSocketAddress loadBalance(Map<String, List<Instance>> serviceProviders, String serviceName) {
        if (serviceProviders.containsKey(serviceName)){
            List<Instance> instances = serviceProviders.get(serviceName);
            if (!instances.isEmpty()){
                // 随机选择一个实例
                Random random = new Random();
                int index = random.nextInt(instances.size());
                Instance instance = instances.get(index);
                log.info("应用了随机算法负载均衡器...");
                return new InetSocketAddress(instance.getIp(), instance.getPort());
            }
        }
        return null;
    }
}
