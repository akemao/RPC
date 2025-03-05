package com.mszlu.rpc.netty.client.handler;

import com.mszlu.rpc.exception.LthRpcException;
import com.mszlu.rpc.message.LthResponse;

import javax.xml.ws.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UnprocessedRequests {

    private static final Map<String, CompletableFuture<LthResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<LthResponse<Object>> resultFuture){
        UNPROCESSED_RESPONSE_FUTURES.put(requestId,resultFuture);
    }

    public void complete(LthResponse<Object> rpcResponse){
        CompletableFuture<LthResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (future!=null){
            future.complete(rpcResponse);
        }else {
            throw new LthRpcException("获取结果数据出现问题");
        }
    }
}
