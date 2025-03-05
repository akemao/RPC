package com.mszlu.rpc.netty.client.handler;

import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.message.LthResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class LthNettyClientHandler extends ChannelInboundHandlerAdapter {
    private UnprocessedRequests unprocessedRequests;

    public LthNettyClientHandler(){
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //一旦客户端发出去消息，在这就得等待接收
        try {
            if (msg instanceof LthMessage){
                LthMessage msMessage = (LthMessage) msg;
                byte messageType = msMessage.getMessageType();
                //读取数据 如果是response的消息类型，拿到数据，标识为完成
                if (messageType == MessageTypeEnum.RESPONSE.getCode()){
                    LthResponse<Object> data = (LthResponse<Object>) msMessage.getData();
                    unprocessedRequests.complete(data);
                }
            }
        }finally {
            //释放ByteBuf 避免内存泄露
            ReferenceCountUtil.release(msg);
        }

    }
}
