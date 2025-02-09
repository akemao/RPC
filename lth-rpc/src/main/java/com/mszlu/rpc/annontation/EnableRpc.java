package com.mszlu.rpc.annontation;

import com.mszlu.rpc.bean.LthBeanDefinitionRegistry;
import com.mszlu.rpc.spring.LthRpcSpringBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
//通过 @Import导入,允许在 Spring 容器启动时动态注册 Bean 定义。
@Import({LthRpcSpringBeanPostProcessor.class})
public @interface EnableRpc {
}
