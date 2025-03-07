package com.mszlu.rpc.spring;

import com.mszlu.rpc.annontation.EnableRpc;
import com.mszlu.rpc.annontation.LthReference;
import com.mszlu.rpc.annontation.LthService;
import com.mszlu.rpc.config.LthRpcConfig;
import com.mszlu.rpc.factory.SingletonFactory;
import com.mszlu.rpc.netty.client.NettyClient;
import com.mszlu.rpc.proxy.LthRpcClientProxy;
import com.mszlu.rpc.register.nacos.NacosTemplate;
import com.mszlu.rpc.server.LthServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 在spring的bean初始化前后进行调用，一般写到初始化之后
 */
@Component
@Slf4j
public class LthRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {

    private LthServiceProvider lthServiceProvider;
    private NettyClient nettyClient;
    private LthRpcConfig lthRpcConfig;
    private NacosTemplate nacosTemplate;

    public LthRpcSpringBeanPostProcessor(){
        lthServiceProvider = SingletonFactory.getInstance(LthServiceProvider.class);
        //创建netty客户端
        nettyClient = SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }


    //bean初始化方法前被调用
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if (enableRpc != null){
            if (lthRpcConfig == null){
                log.info("EnableRpc会先于所有的bean初始化之前执行，在这里我们进行配置的加载");
                lthRpcConfig = new LthRpcConfig();
                lthRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                lthRpcConfig.setNacosHost(enableRpc.nacosHost());
                lthRpcConfig.setNacosPort(enableRpc.nacosPort());
                lthRpcConfig.setProviderPort(enableRpc.serverPort());
                lthServiceProvider.setLthRpcConfig(lthRpcConfig);
                //nacos 根据配置进行初始化
                nacosTemplate.init(lthRpcConfig.getNacosHost(),lthRpcConfig.getNacosPort());
                nettyClient.setLthRpcConfig(lthRpcConfig);
            }
        }
        return bean;
    }

    //bean初始化方法后被调用
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //判断bean上有没有加@LthService注解;如果有,则将其发布
        if(bean.getClass().isAnnotationPresent(LthService.class)){
            LthService lthService = bean.getClass().getAnnotation(LthService.class);
            //加了LthService的bean就被找到了,就把其中的方法都发布为服务
            lthServiceProvider.publishService(lthService,bean);
        }

        //判断bean里字段有没有加@Lthreference注解
        //如果有 识别并生成代理实现类,发起网络请求
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            LthReference annotation = declaredField.getAnnotation(LthReference.class);
            if(annotation != null){
                LthRpcClientProxy lthRpcClientProxy = new LthRpcClientProxy(annotation,nettyClient);
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

    /*
        在 Spring 容器加载所有 Bean 定义之后(实例化-构造 之前)、但在任何 Bean 被初始化之前调用。

     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        LthRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );
                // add filter
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        LthRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( EnableRpc.class );
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", LthRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );
                // scan packages
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );
                scan.invoke ( scanner, new Object[]{"com.mszlu.rpc.annontation"} );
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }
}
