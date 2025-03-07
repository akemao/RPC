package com.mszlu.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.annontation.LthService;
import com.mszlu.rpc.config.LthRpcConfig;
import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.NettyServer;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.sun.org.apache.xpath.internal.operations.Lt;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LthServiceProvider {

    public void setLthRpcConfig(LthRpcConfig lthRpcConfig) {
        this.lthRpcConfig = lthRpcConfig;
    }

    public LthRpcConfig getLthRpcConfig() {
        return lthRpcConfig;
    }

    private LthRpcConfig lthRpcConfig;

    private final Map<String, Object> serviceMap;
    private NacosTemplate nacosTemplate;

    public LthServiceProvider(){
        //发布的服务 都在这里
        serviceMap = new ConcurrentHashMap<>();
        log.info("LthServiceProvider 实例创建, HashCode: {}", this.hashCode());
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
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
        String serviceName = service.getClass().getInterfaces()[0].getCanonicalName() + lthService.version();
        serviceMap.put(serviceName,service);
        //将服务注册到nacos上
        if (lthRpcConfig == null){
            throw new LthRpcException("必须开启EnableRPC");
        }
        try {
            Instance instance = new Instance();
            //instance.setPort(NettyServer.PORT);
            instance.setPort(lthRpcConfig.getProviderPort());
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            instance.setClusterName("lth-rpc-service-provider");
            instance.setServiceName(serviceName);
            //nacosTemplate.registerServer("lth-rpc",instance);
            nacosTemplate.registerServer(lthRpcConfig.getNacosGroup(),instance);
        }catch (Exception e){
            log.error("nacos 注册服务失败:",e);
        }
        log.info("发现服务并注册key:{},value:{}",serviceName,service);
    }

    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }
}
