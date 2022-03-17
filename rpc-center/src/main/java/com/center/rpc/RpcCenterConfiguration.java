package com.center.rpc;

import com.center.rpc.handler.ClientHandler;
import com.center.rpc.handler.ServerHandler;
import com.center.rpc.handler.ZookeeperServerHandler;
import com.center.rpc.pojo.ZKConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 注册中心
 * 消费者获取节点信息 提供者注册节点信息
 */
@Configuration
@ConditionalOnProperty(value="rpc.center.active",havingValue="true",matchIfMissing=false)
public class RpcCenterConfiguration {

    /**
     * 读取zk的连接信息
     * @return
     */
    @Bean()
    @ConfigurationProperties(prefix="rpc.center")
    public ZKConfig newDataSource(){
        return new ZKConfig();
    }

    /**
     * 连接zk客户端
     * @param zkConfig
     * @return
     */
    @Bean
    public CuratorFramework newCuratorFramework(ZKConfig zkConfig){
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(zkConfig.getConnectStr())
                .sessionTimeoutMs(zkConfig.getSessionTimeoutMs())
                .connectionTimeoutMs(zkConfig.getConnectionTimeoutMs())
                .retryPolicy(retryPolicy)
                .namespace(zkConfig.getNamespace())
                .build();
        client.start();
        return client;

    }


    /**
     * 供服务提供者使用
     * @param client
     * @return
     */
    @Bean
    public ServerHandler newServerHandler(CuratorFramework client)
    {
        return new ZookeeperServerHandler(client);
    }

    /**
     * 供服务消费者使用
     * @param client
     * @return
     */
    @Bean
    public ClientHandler newClientHandler(CuratorFramework client){
        return new ZookeeperServerHandler(client);
    }


}
