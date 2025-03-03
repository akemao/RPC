package com.mszlu.rpc.netty.codec;

import com.mszlu.rpc.compress.Compress;
import com.mszlu.rpc.constants.CompressTypeEnum;
import com.mszlu.rpc.constants.LthRpcConstants;
import com.mszlu.rpc.constants.SerializationTypeEnum;
import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.message.LthMessage;
import com.mszlu.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

public class LthRpcEncoder extends MessageToByteEncoder<LthMessage> {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    // 4B  magic number（魔法数）
    // 1B version（版本）
    // 4B full length（消息长度）
    // 1B messageType（消息类型）
    // 1B codec（序列化类型）
    // 1B compress（压缩类型）
    // 4B  requestId（请求的Id）
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LthMessage msg, ByteBuf out) throws Exception {
        out.writeBytes(LthRpcConstants.MAGIC_NUMBER);
        out.writeByte(LthRpcConstants.VERSION);
        // 预留数据长度位置
        out.writerIndex(out.writerIndex() + 4);
        byte messageType = msg.getMessageType();
        out.writeByte(messageType);
        out.writeByte(msg.getCodec());
        out.writeByte(msg.getCompress());
        //请求id 原子操作 线程安全 相对加锁 效率高
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());
        // build full length
        byte[] bodyBytes = null;
        //header 长度为 16
        int fullLength = LthRpcConstants.HEAD_LENGTH;
        // fullLength = head length + body length
        // 序列化数据
        Serializer serializer = loadSerializer(msg.getCodec());
        bodyBytes = serializer.serialize(msg.getData());
        // 压缩数据
        Compress compress = loadCompress(msg.getCompress());
        bodyBytes = compress.compress(bodyBytes);
        fullLength += bodyBytes.length;
        out.writeBytes(bodyBytes);
        int writeIndex = out.writerIndex();
        //将fullLength写入之前的预留的位置
        out.writerIndex(writeIndex - fullLength + LthRpcConstants.MAGIC_NUMBER.length + 1);
        out.writeInt(fullLength);
        //恢复写入索引到原来的位置
        out.writerIndex(writeIndex);
    }

    private Serializer loadSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name().equals(serializerName)) {
                return serializer;
            }
        }
        throw new LthRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if (compress.name().equals(compressName)) {
                return compress;
            }
        }
        throw new LthRpcException("无对应的压缩类型");
    }
}
