package com.center.rpc.handler;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 服务启动后把信息注册到zk上
 */
public interface ServerHandler {

    boolean registerServer(Class serverClass,String ip,int port);




}
