package com.center.rpc.pojo;

import lombok.*;


/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 连接zookeeper配置中心
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class ServiceInfo {

    private String ip;

    private int port;




}
