package com.mszlu.rpc.netty.client;

import com.mszlu.rpc.message.LthRequest;

public interface LthClient {

    /**
     * 发送请求，并接收数据
     * @param lthRequest
     * @return
     */
    //Object sendRequest(LthRequest lthRequest, String host, int port);
    Object sendRequest(LthRequest lthRequest);
}
