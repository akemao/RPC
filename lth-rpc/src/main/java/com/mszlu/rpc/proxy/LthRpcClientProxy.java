package com.mszlu.rpc.proxy;

import com.mszlu.rpc.annontation.LthReference;
import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.message.LthRequest;
import com.mszlu.rpc.message.LthResponse;
import com.mszlu.rpc.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
@Slf4j
public class LthRpcClientProxy implements InvocationHandler {

    public LthRpcClientProxy(){}

    private NettyClient nettyClient;
    private LthReference lthReference;

    public LthRpcClientProxy(LthReference lthReference, NettyClient nettyClient) {
        this.lthReference = lthReference;
        this.nettyClient = nettyClient;
    }

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1. 构建请求数据MsRequest
        String requestId = UUID.randomUUID().toString();
        LthRequest request = LthRequest.builder()
                .group("lth-rpc")
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(requestId)
                .version(lthReference.version())
                .build();

        //2. 通过客户端向服务端发送请求
      /*  String host = lthReference.host();
        int port = lthReference.port();*/
        CompletableFuture<LthResponse<Object>> future = (CompletableFuture<LthResponse<Object>>) nettyClient.sendRequest(request);
        //4. 接收数据
        LthResponse<Object> lthResponse = future.get();
        if (lthResponse == null){
            throw new LthRpcException("服务调用失败");
        }

        if (!requestId.equals(lthResponse.getRequestId())){
            throw new LthRpcException("响应结果和请求不一致");
        }
        return lthResponse.getData();
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
}
