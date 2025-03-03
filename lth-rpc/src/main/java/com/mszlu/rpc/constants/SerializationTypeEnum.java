package com.mszlu.rpc.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    //读取协议这的序列化类型，来此枚举进行匹配
    PROTO_STUFF((byte) 0x01, "protostuff");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        //每个枚举实例都是一个对象，存储在枚举类的静态数组 values() 中
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
