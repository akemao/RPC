package com.mszlu.rpc.balance;

import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinLoadBalance implements LoadBalance{

    private static final RoundRobinLoadBalance INSTANCE = new RoundRobinLoadBalance();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public RoundRobinLoadBalance() {
    }

    public static RoundRobinLoadBalance getInstance() {
        return INSTANCE;
    }

    @Override
    public String name() {
        return "roundRobin";
    }

    @Override
    public InetSocketAddress loadBalance(Map<String, List<Instance>> serviceProviders, String serviceName) {
        if (serviceProviders.containsKey(serviceName)) {
            List<Instance> instances = serviceProviders.get(serviceName);
            if (!instances.isEmpty()) {
                int size = instances.size();
                log.info("Available instances: {}", instances); // 打印实例列表
                // 轮询算法核心
                int index = (currentIndex.getAndIncrement() & Integer.MAX_VALUE) % size;
                Instance instance = instances.get(index);
                log.info("应用了轮询算法负载均衡器... Selected instance: {}", instance);
                return new InetSocketAddress(instance.getIp(), instance.getPort());
            }
        }
        return null;
    }
}
