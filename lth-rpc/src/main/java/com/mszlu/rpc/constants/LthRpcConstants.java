package com.mszlu.rpc.constants;

public class LthRpcConstants {

    public static final int TOTAL_LENGTH = 16;

    public static final byte[] MAGIC_NUMBER = {(byte)'m',(byte)'s',(byte)'n',(byte)'b'};

    public static final int VERSION = 1;

    public static final int HEAD_LENGTH = 16;

    //ping
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;
    //pong
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;

    public static final String PING = "ping";
    public static final String PONG = "pong";
}
