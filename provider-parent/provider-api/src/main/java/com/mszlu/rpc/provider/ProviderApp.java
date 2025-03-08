package com.mszlu.rpc.provider;

import com.mszlu.rpc.annontation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRpc(nacosGroup = "lth-rpc-group", serverPort = 13567)
public class ProviderApp {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApp.class,args);
    }
}
