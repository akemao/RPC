package com.mszlu.rpc.bean;

import org.springframework.beans.factory.FactoryBean;

public class LthHttpClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;

    @Override
    public T getObject() throws Exception {
        return null;
    }

    //Bean的类型
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    //true是单例，false是非单例  在Spring5.0中此方法利用了JDK1.8的新特性变成了default方法，返回true
    @Override
    public boolean isSingleton() {
        return true;
    }
}
