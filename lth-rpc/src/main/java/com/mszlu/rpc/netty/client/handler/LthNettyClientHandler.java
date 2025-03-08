package com.mszlu.rpc.netty.client.handler;

import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.LthRpcConstants;
import com.mszlu.rpc.constants.MessageTypeEnum;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.message.LthResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        } catch (Exception e){
            log.error("客户端读取消息出错:",e);
        }finally {
            //释放ByteBuf 避免内存泄露
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.info("3s未收到写请求，发起心跳,地址：{}", ctx.channel().remoteAddress());
                LthMessage rpcMessage = new LthMessage();
                rpcMessage.setCodec(SerializationTypeEnum.PROTO_STUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(LthRpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(LthRpcConstants.PING);
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //代表通道已连接
        //表示channel活着
        //super.channelActive(ctx);  其父类就是执行的 ctx.fireChannelActive();
        log.info("客户端连接上了.....连接正常");
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //代表连接关闭了
        log.info("服务端连接关闭:{}",ctx.channel().remoteAddress());
        //需要将缓存清除掉

        //标识channel不活着
        ctx.fireChannelInactive();
    }
}
