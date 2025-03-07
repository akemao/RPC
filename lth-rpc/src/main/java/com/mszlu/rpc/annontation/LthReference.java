package com.mszlu.rpc.annontation;

import java.lang.annotation.*;

//可用于构造方法和字段上
@Target({ElementType.CONSTRUCTOR,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LthReference {
  /*  //netty的服务主机名
    String host();
    //netty服务的端口号
    int port();*/
    //调用的服务提供方的版本号
    String version() default "1.0";
}
