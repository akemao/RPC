package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.constants.LthRpcConstants;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.message.LthRequest;
import com.mszlu.rpc.message.LthResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LthNettyServerHandler extends ChannelInboundHandlerAdapter {

    private LthRequestHandler lthRequestHandler;

    public LthNettyServerHandler(){
        lthRequestHandler = SingletonFactory.getInstance(LthRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收客户端发来的数据，数据肯定包括了 要调用的服务提供者的接口，方法
        //解析消息，去找到对应的服务提供者，然后调用，得到调用结果，发消息给客户端即可
        try{
            if (msg instanceof LthMessage){
                //拿到请求数据，调用对应服务提供方方法，获取结果给客户端
                LthMessage message = (LthMessage) msg;
                byte messageType = message.getMessageType();
                if (MessageTypeEnum.HEARTBEAT_PING.getCode() == messageType){
                    message.setMessageType(MessageTypeEnum.HEARTBEAT_PONG.getCode());
                    message.setData(LthRpcConstants.PONG);
                }
                if (MessageTypeEnum.REQUEST.getCode() == messageType){
                    LthRequest lthRequest = (LthRequest) message.getData();
                    //处理业务，使用反射找到方法，发起调用 获取结果
                    Object result = lthRequestHandler.handler(lthRequest);
                    message.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    if (ctx.channel().isActive() && ctx.channel().isWritable()){
                        LthResponse lthResponse = LthResponse.success(result,lthRequest.getRequestId());
                        message.setData(lthResponse);
                    }else {
                        message.setData(LthResponse.fail("net fail"));
                    }
                }
                //ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);在写操作完成后关闭连接(不符合预期)
                //写完数据 不能关闭通道
                ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } catch (Exception e){
            log.error("读取消息出错:",e);
        }finally {
            //释放 以防止内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 如果10s没有读请求，不进行 处理，以免连接过多，每个都回复 会造成网络压力
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("客户端10s 未发送读请求，判定失效，进行关闭");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        //出现异常 关闭连接
        ctx.close();
    }
}
