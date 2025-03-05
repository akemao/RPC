package com.mszlu.rpc.factory;

import com.mszlu.rpc.server.LthServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 获取单例对象的工厂类
 */
public class SingletonFactory {
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

   /* public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        String key = c.toString();
        if (OBJECT_MAP.containsKey(key)) {
            //value默认Object,需强转为T类型
            return c.cast(OBJECT_MAP.get(key));
        } else {
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }*/

    public static <T> T getInstance(Class<T> c) {
        if (c == null) throw new IllegalArgumentException();
        String key = c.getName(); // 关键改动：使用类全限定名作为键
        return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("创建单例失败", e);
            }
        }));
    }

    public static void main(String[] args) {
        //测试并发下 生成的单例是否唯一
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0 ; i< 1000; i++) {
            executorService.execute(new Runnable() {//1868987089  147089688
                @Override
                public void run() {
                    LthServiceProvider instance = SingletonFactory.getInstance(LthServiceProvider.class);
                    System.out.println(instance);
                }
            });
        }
        while (true){}
    }
}
