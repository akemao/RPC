package com.mszlu.rpc.spring;

import com.mszlu.rpc.annontation.LthReference;
import com.mszlu.rpc.annontation.LthService;
import com.mszlu.rpc.proxy.LthRpcClientProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 在spring的bean初始化前后进行调用，一般写到初始化之后
 */
@Component
public class LthRpcSpringBeanPostProcessor implements BeanPostProcessor {
    //bean初始化方法前被调用
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    //bean初始化方法后被调用
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //判断bean上有没有加@LthService注解;如果有,则将其发布
        if(bean.getClass().isAnnotationPresent(LthService.class)){
            LthService lthService = bean.getClass().getAnnotation(LthService.class);

        }

        //判断bean里字段有没有加@Lthreference注解
        //如果有 识别并生成代理实现类,发起网络请求
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            LthReference annotation = declaredField.getAnnotation(LthReference.class);
            if(annotation != null){
                LthRpcClientProxy lthRpcClientProxy = new LthRpcClientProxy(annotation);
                Object proxy = lthRpcClientProxy.getProxy(declaredField.getType());
                //当isAccessible()的结果是false时不允许通过反射访问该字段
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
