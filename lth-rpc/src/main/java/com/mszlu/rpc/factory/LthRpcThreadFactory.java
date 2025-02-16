package com.mszlu.rpc.factory;

import com.mszlu.rpc.server.LthServiceProvider;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LthRpcThreadFactory implements ThreadFactory {
    private LthServiceProvider lthServiceProvider;

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    private final String namePrefix;

    private final ThreadGroup threadGroup;


    public LthRpcThreadFactory(LthServiceProvider lthServiceProvider){
        this.lthServiceProvider = lthServiceProvider;
        SecurityManager securityManager = System.getSecurityManager();
        threadGroup = securityManager != null ? securityManager.getThreadGroup() :Thread.currentThread().getThreadGroup();
        namePrefix = "lth-rpc-" + poolNumber.getAndIncrement()+"-thread-";
    }

    //创建的线程以“N-thread-M”命名，N是该工厂的序号，M是线程号
    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(threadGroup, runnable,
                namePrefix + threadNumber.getAndIncrement(), 0);
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
