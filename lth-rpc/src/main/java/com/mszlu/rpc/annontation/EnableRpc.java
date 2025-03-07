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

    //nacos主机名
    String nacosHost() default "localhost";
    //nacos端口号
    int nacosPort() default 8848;

    //nacos组，同一个组内 互通，并且组成集群
    String nacosGroup() default "lth-rpc-group";

    //server服务端口
    int serverPort() default 13567;
}
