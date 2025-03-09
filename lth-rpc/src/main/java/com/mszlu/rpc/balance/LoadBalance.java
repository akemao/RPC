package com.mszlu.rpc.balance;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

//负载均衡器接口
public interface LoadBalance {

    String name();

    InetSocketAddress loadBalance(Map<String, List<Instance>> serviceProviders, String serviceName);
}
