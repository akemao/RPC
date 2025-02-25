package com.mszlu.rpc.proxy;

import com.mszlu.rpc.annontation.MsMapping;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
// 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
// 当我们通过动态代理对象调用一个方法时候，
// 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
public class LthHttpClientProxy implements InvocationHandler {

    /**
     * proxy:代理类代理的真实代理对象com.sun.proxy.$Proxy0
     * method:我们所要调用某个对象真实的方法的Method对象
     * args:指代代理对象方法传递的参数
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //在这里实现调用
        MsMapping msMapping = method.getAnnotation(MsMapping.class);
        if (msMapping != null){
            String url = msMapping.url();
            //provide/goods/{id} ,需要将{id}替换为真实id
            String api = msMapping.api();
            Pattern compile = Pattern.compile("(\\{\\w+})");
            Matcher matcher = compile.matcher(api);
            if (matcher.find()){
                int count = matcher.groupCount();
                for(int i=0; i<count; i++){
                    String group = matcher.group(i);
                    api = api.replace(group,args[i].toString());
                }
            }
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(url+api,method.getReturnType());
        }
        return null;
    }

    /**
     * get the proxy object
     */
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
}
