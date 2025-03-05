package com.mszlu.rpc.server;

import com.mszlu.rpc.annontation.LthService;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LthServiceProvider {

    private final Map<String, Object> serviceMap;

    public LthServiceProvider(){
        //发布的服务 都在这里
        serviceMap = new ConcurrentHashMap<>();
        log.info("LthServiceProvider 实例创建, HashCode: {}", this.hashCode());
    }

    public void publishService(LthService lthService, Object service) {
        registerService(lthService,service);
        //检测到有服务发布的注解，启动NettyServer
        NettyServer nettyServer = SingletonFactory.getInstance(NettyServer.class);
        nettyServer.setLthServiceProvider(this);
        if (!nettyServer.isRunning()){
            nettyServer.run();
        }
    }

    private void registerService(LthService lthService, Object service) {
        //service要进行注册, 先创建一个map进行存储
        //getInterfaces()[0] 获取实现类第一个实现的接口
        String interfaceName = service.getClass().getInterfaces()[0].getCanonicalName();
        String version =  lthService.version();
        String key = interfaceName + version;
        serviceMap.put(key,service);
        log.info("发现服务并注册key:{},value:{}",key,service);
    }

    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }
}
