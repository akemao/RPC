package com.mszlu.rpc.netty.handler;

import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.message.LthRequest;
import com.mszlu.rpc.message.LthResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
                ctx.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e){
            log.error("读取消息出错:",e);
        }finally {
            //释放 以防止内存泄漏
            ReferenceCountUtil.release(msg);
        }

    }
}
