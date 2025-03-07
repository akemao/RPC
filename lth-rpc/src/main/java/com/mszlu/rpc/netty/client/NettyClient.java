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
import com.mszlu.rpc.register.nacos.NacosTemplate;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NettyClient implements LthClient{

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final UnprocessedRequests unprocessedRequests;
    private final NacosTemplate nacosTemplate;

    private LthRpcConfig lthRpcConfig;
    public void setLthRpcConfig(LthRpcConfig lthRpcConfig) {
        this.lthRpcConfig = lthRpcConfig;
    }



    public NettyClient(){
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline ().addLast ( "decoder",new LthRpcDecoder() );
                        ch.pipeline ().addLast ( "encoder",new LthRpcEncoder());
                        ch.pipeline ().addLast ( "handler",new LthNettyClientHandler() );

                    }
                });
    }


    //public Object sendRequest(LthRequest lthRequest, String host, int port) {
    @Override
    public Object sendRequest(LthRequest lthRequest) {
        if (lthRpcConfig == null){
            throw new LthRpcException("必须开启EnableRPC");
        }
        //结果获取的任务
        CompletableFuture<LthResponse<Object>> resultFuture = new CompletableFuture<>();

        //需要从nacos中获取服务提供方的ip和端口
        Instance oneHealthyInstance = null;
        try {
            oneHealthyInstance = nacosTemplate.getOneHealthyInstance(lthRequest.getInterfaceName() + lthRequest.getVersion(), lthRpcConfig.getNacosGroup());
        } catch (Exception e) {
            log.error("获取nacos实例出错:",e);
            resultFuture.completeExceptionally(e);
            return resultFuture;
        }

        //1.先连接netty服务 拿到channel
        InetSocketAddress inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(),oneHealthyInstance.getPort());
        //连接
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                //链接是否完成
                if (future.isSuccess()){
                    channelCompletableFuture.complete(future.channel());
                }else {
                    log.info("连接netty服务失败");
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
}
