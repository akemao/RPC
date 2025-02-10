package com.mszlu.rpc.server;

import com.mszlu.rpc.annontation.LthService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LthServiceProvider {

    private final Map<String, Object> serviceMap;

    public LthServiceProvider(){
        //发布的服务 都在这里
        serviceMap = new ConcurrentHashMap<>();
    }

    public void publishService(LthService lthService, Object service) {
        registerService(lthService,service);
        //检测到有服务发布的注解，启动NettyServer

    }

    private void registerService(LthService lthService, Object service) {
        //service要进行注册, 先创建一个map进行存储
        //getInterfaces()[0] 获取实现类第一个实现的接口
        String serviceName = service.getClass().getInterfaces()[0].getCanonicalName()+lthService.version();
        serviceMap.put(serviceName,service);
        log.info("发现服务{}并注册",serviceName);
    }

    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }
}
