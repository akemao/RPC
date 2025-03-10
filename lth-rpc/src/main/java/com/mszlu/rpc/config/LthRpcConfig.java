package com.mszlu.rpc.config;

import lombok.Data;

@Data
public class LthRpcConfig {

    private String nacosHost = "localhost";

    private int nacosPort = 8848;

    private int providerPort = 13567;
    /**
     * 同一个组内 互通，并组成集群
     */
    private String nacosGroup = "lth-rpc-group";

    private String loadbalance = "random";
}
