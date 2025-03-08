package com.mszlu.rpc.netty.timer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.LthRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class UpdateNacosServiceTask implements TimerTask {
    private String serviceName;
    private NacosTemplate nacosTemplate;
    private LthRpcConfig lthRpcConfig;
    private Map<String, List<Instance>> servicesProvider;
    public UpdateNacosServiceTask(String serviceName, LthRpcConfig lthRpcConfig, Map<String, List<Instance>> servicesProvider){
        this.serviceName = serviceName;
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        this.lthRpcConfig = lthRpcConfig;
        this.servicesProvider=servicesProvider;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        Instance oneHealthyInstance = nacosTemplate.getOneHealthyInstance(serviceName, lthRpcConfig.getNacosGroup());
        servicesProvider.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(oneHealthyInstance);
        //执行完，继续任务
        timeout.timer().newTimeout(timeout.task(),10, TimeUnit.SECONDS);
    }
}