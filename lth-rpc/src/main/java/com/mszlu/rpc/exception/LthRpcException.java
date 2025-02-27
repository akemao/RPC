package com.mszlu.rpc.exception;

public class LthRpcException extends RuntimeException{

    public LthRpcException(){
        super();
    }

    public LthRpcException(String msg){
        super(msg);
    }

    public LthRpcException(String msg,Exception e){
        super(msg,e);
    }
}
