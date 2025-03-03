package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthRequest;
import com.mszlu.rpc.server.LthServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class LthRequestHandler {

    private LthServiceProvider lthServiceProvider;

    public LthRequestHandler(){
        lthServiceProvider = SingletonFactory.getInstance(LthServiceProvider.class);
    }

    public Object handler(LthRequest lthRequest) {
        String interfaceName = lthRequest.getInterfaceName();
        String version = lthRequest.getVersion();
        Object service = lthServiceProvider.getService(interfaceName + version);
        if (service == null){
            throw new LthRpcException("没有找到可用的服务提供方");
        }
        try {
            Method method = service.getClass().getMethod(lthRequest.getMethodName(), lthRequest.getParamTypes());
            return method.invoke(service,lthRequest.getParameters());
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.info("服务提供方 方法调用出现问题:",e);
        }
        return null;
    }
}
