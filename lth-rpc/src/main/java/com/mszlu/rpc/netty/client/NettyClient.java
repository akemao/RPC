package com.mszlu.rpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mszlu.rpc.config.LthRpcConfig;
import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.message.LthRequest;
import com.mszlu.rpc.message.LthResponse;
import com.mszlu.rpc.netty.client.handler.LthNettyClientHandler;
import com.mszlu.rpc.netty.client.handler.UnprocessedRequests;
import com.mszlu.rpc.netty.codec.LthRpcDecoder;
import com.mszlu.rpc.netty.codec.LthRpcEncoder;
import com.mszlu.rpc.netty.handler.idle.ConnectionWatchdog;
import com.mszlu.rpc.netty.timer.UpdateNacosServiceTask;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyClient implements LthClient{

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final UnprocessedRequests unprocessedRequests;
    private final NacosTemplate nacosTemplate;
    private LthRpcConfig lthRpcConfig;
    protected HashedWheelTimer serviceTimer;

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    // 使用 ConcurrentHashMap 来缓存服务提供者地址
    private static final Map<String, List<Instance>> SERVICE_PROVIDERS = new ConcurrentHashMap<>();


    public NettyClient(){
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
//                .handler(new ChannelInitializer<SocketChannel>() {
//                    @Override
//                    protected void initChannel(SocketChannel ch) throws Exception {
//                        //3s 没收到写请求，进行心跳检测
//                        ch.pipeline().addLast(new IdleStateHandler(0,3,0, TimeUnit.SECONDS));
//                        ch.pipeline ().addLast ( "decoder",new LthRpcDecoder() );
//                        ch.pipeline ().addLast ( "encoder",new LthRpcEncoder());
//                        ch.pipeline ().addLast ( "handler",new LthNettyClientHandler() );
//
//                    }
//                });
    }


    //public Object sendRequest(LthRequest lthRequest, String host, int port) {
    @Override
    public Object sendRequest(LthRequest lthRequest) {
        if (lthRpcConfig == null){
            throw new LthRpcException("必须开启EnableRPC");
        }
        //结果获取的任务
        CompletableFuture<LthResponse<Object>> resultFuture = new CompletableFuture<>();

        //获取服务名
        String serviceName = lthRequest.getInterfaceName() + lthRequest.getVersion();

        InetSocketAddress inetSocketAddress = null;

        // 从缓存中获取服务提供者地址
        if (SERVICE_PROVIDERS.containsKey(serviceName)) {
            List<Instance> instances = SERVICE_PROVIDERS.get(serviceName);
            if (!instances.isEmpty()) {
                // 随机选择一个实例
                Random random = new Random();
                int index = random.nextInt(instances.size());
                Instance instance = instances.get(index);
                inetSocketAddress = new InetSocketAddress(instance.getIp(), instance.getPort());
                log.info("走了缓存的服务提供者地址，省去了连接 Nacos 的过程...");
            }
        }

        // 如果缓存中没有地址，从 Nacos 获取
        if (inetSocketAddress == null){
            //需要从nacos中获取服务提供方的ip和端口
            Instance oneHealthyInstance = null;
            try {
                oneHealthyInstance = nacosTemplate.getOneHealthyInstance(serviceName, lthRpcConfig.getNacosGroup());
            } catch (Exception e) {
                log.error("获取nacos实例出错:",e);
                resultFuture.completeExceptionally(e);
                return resultFuture;
            }
            //更新缓存
            if (oneHealthyInstance != null){
                SERVICE_PROVIDERS.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(oneHealthyInstance);
                //触发定时任务，保证新开启的服务能进入缓存
                if (serviceTimer == null){
                    serviceTimer = new HashedWheelTimer();
                    serviceTimer.newTimeout(new UpdateNacosServiceTask(serviceName,lthRpcConfig,SERVICE_PROVIDERS),10,TimeUnit.SECONDS);
                }
            }
            inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(), oneHealthyInstance.getPort());
        }

        //连接
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();

        final ConnectionWatchdog watchdog = new ConnectionWatchdog
                (bootstrap, timer, inetSocketAddress, serviceName, channelCompletableFuture, true) {

            @Override
            public void clear(InetSocketAddress inetSocketAddress) {
                // 直接根据 serviceName 清除缓存
                List<Instance> instances = SERVICE_PROVIDERS.get(serviceName);
                if (instances != null) {
                    //String ip = inetSocketAddress.getHostName();
                    String ip = inetSocketAddress.getAddress().getHostAddress();
                    int port = inetSocketAddress.getPort();
                    instances.removeIf(instance ->
                            instance.getIp().equals(ip) && instance.getPort() == port
                    );
                    log.info("instances:{}",instances.size());
                    // 若列表为空，删除键
                    if (instances.isEmpty()) {
                        SERVICE_PROVIDERS.remove(serviceName);
                        log.info("移除空实例列表服务: serviceName={}", serviceName);
                    }
                }
            }
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateHandler(0, 3, 0, TimeUnit.SECONDS),
                        new LthRpcDecoder(),
                        new LthRpcEncoder(),
                        new LthNettyClientHandler()
                };
            }
        };
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(watchdog.handlers());
            }
        });
        //String finalIpAndPort = ipAndPort;

        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //链接是否完成
                if (future.isSuccess()){
                    channelCompletableFuture.complete(future.channel());
                }else {
                    //连接失败，删除缓存
                    log.info("连接netty服务失败");
                    channelCompletableFuture.completeExceptionally(future.cause());
                }
            }
        });

        try {
            Channel channel = channelCompletableFuture.get();

            if (!channel.isActive()){
                throw new LthRpcException("连接异常");
            }
                //将任务 存起来，和请求id对应，便于后续读取到数据后，可以根据请求id，将任务标识完成
                unprocessedRequests.put(lthRequest.getRequestId(),resultFuture);
                LthMessage lthMessage = LthMessage.builder()
                        .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .messageType(MessageTypeEnum.REQUEST.getCode())
                        .data(lthRequest)
                        .build();
                channel.writeAndFlush(lthMessage).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()){
                            //任务完成
                            log.info("发送数据成功:{}",lthMessage);
                        }else {
                            //发送数据失败
                            future.channel().close();
                            //任务标识为完成 有异常
                            resultFuture.completeExceptionally(future.cause());
                            log.info("发送数据失败:",future.cause());
                        }
                    }
                });
        }catch (Exception e) {
            throw new LthRpcException("获取Channel失败",e);
        }

        return resultFuture;
    }

    public void setLthRpcConfig(LthRpcConfig lthRpcConfig) {
        this.lthRpcConfig = lthRpcConfig;
    }

    public LthRpcConfig getLthRpcConfig() {
        return lthRpcConfig;
    }

}
