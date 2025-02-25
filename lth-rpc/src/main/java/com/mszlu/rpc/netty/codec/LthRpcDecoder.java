package com.mszlu.rpc.netty.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MsRpcDecoder extends LengthFieldBasedFrameDecoder {
    public MsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}
