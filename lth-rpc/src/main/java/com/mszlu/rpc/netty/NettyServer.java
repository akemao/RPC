package com.mszlu.rpc.netty;

import com.mszlu.rpc.factory.LthRpcThreadFactory;
import com.mszlu.rpc.netty.handler.NettyServerInitiator;
import com.mszlu.rpc.server.LthServiceProvider;
import com.mszlu.rpc.utils.RuntimeUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServer implements LthServer{
    public static final int PORT = 13567;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private LthServiceProvider lthServiceProvider;
    private DefaultEventExecutorGroup eventExecutors;

    private boolean isRunning;


    @Override
    public void run() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            eventExecutors = new DefaultEventExecutorGroup(RuntimeUtil.cpus() * 2,new LthRpcThreadFactory(lthServiceProvider));
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG,1024)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new NettyServerInitiator(eventExecutors));
            bootstrap.bind(lthServiceProvider.getLthRpcConfig().getProviderPort()).sync().channel();
            isRunning = true;
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    stopServer();
                }
            });

        }catch (InterruptedException e){
            log.error("occur exception when start server:",e);
        }
    }

    @Override
    public void stopServer() {
        stopNettyServer();
        isRunning = false;
    }

    private void stopNettyServer() {
        if (eventExecutors != null){
            eventExecutors.shutdownGracefully();
        }
        if (bossGroup != null){
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null){
            workerGroup.shutdownGracefully();
        }
    }

    public void setLthServiceProvider(LthServiceProvider lthServiceProvider) {
        this.lthServiceProvider = lthServiceProvider;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
