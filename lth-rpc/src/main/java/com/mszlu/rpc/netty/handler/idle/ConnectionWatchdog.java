package com.mszlu.rpc.netty.handler.idle;

import com.alibaba.nacos.api.naming.pojo.Instance;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
/**
 *
 * 重连检测狗，当发现当前的链路不稳定关闭之后，进行12次重连
 */
@Sharable//用来说明ChannelHandler是否可以在多个channel直接共享使用
@Slf4j
/**
 * 继承了ChannelInboundHandlerAdapter，说明它也是Handler，也对，作为一个检测对象，肯定会放在链路中，否则怎么检测
 *
 */
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask ,ChannelHandlerHolder,CacheClearHandler{

 
    private final Bootstrap bootstrap;
    private final Timer timer;
    private final InetSocketAddress inetSocketAddress;
    private final String serviceName;


    private volatile boolean reconnect = true;
    private int attempts;

    private final CompletableFuture<Channel> completableFuture;
 
 
    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, InetSocketAddress inetSocketAddress, String serviceName, CompletableFuture<Channel> completableFuture, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.inetSocketAddress = inetSocketAddress;
        this.serviceName = serviceName;
        this.reconnect = reconnect;
        this.completableFuture = completableFuture;
    }
 
    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("当前链路已经激活了，重连尝试次数重新置为0");
        attempts = 0;
        ctx.fireChannelActive();
    }
 
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("链接关闭");
        if(reconnect){
            log.info("链接关闭，将进行重连");
            if (attempts < 3) {
                attempts++;
                log.info("重连次数:{}",attempts);
            }else{
                //不在重连了
                log.info("超过重连次数~");
                reconnect = false;
                //连接失败 从 缓存中 去除
                clear(inetSocketAddress);
            }
            //重连的间隔时间会越来越长
            int timeout = 2 << attempts;
            timer.newTimeout(this, timeout, TimeUnit.SECONDS);
        }
        ctx.fireChannelInactive();
    }
 
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future;
        //bootstrap已经初始化好了，只需要将handler填入就可以了
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>(){
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(inetSocketAddress);
        }
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()){
                    //代表连接成功，将channel放入任务中
                    completableFuture.complete(f.channel());
                }else {
                    completableFuture.completeExceptionally(future.cause());
                    //尝试重连
                    f.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }
 
}