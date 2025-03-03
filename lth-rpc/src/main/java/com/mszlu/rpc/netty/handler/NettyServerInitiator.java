package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.netty.codec.LthRpcDecoder;
import com.mszlu.rpc.netty.codec.LthRpcEncoder;
import com.mszlu.rpc.netty.handler.LthNettyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;


public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {
    private EventExecutorGroup eventExecutors;

    public NettyServerInitiator(EventExecutorGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        //解码器
        channel.pipeline ().addLast ( "decoder",new LthRpcDecoder() );
        //编码器
        channel.pipeline ().addLast ( "encoder",new LthRpcEncoder());
        //消息处理器，线程池处理
        channel.pipeline ().addLast ( eventExecutors,"handler",new LthNettyServerHandler() );
    }

}
