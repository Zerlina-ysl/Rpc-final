package com.center.rpc.handler;

import com.center.rpc.pojo.ServiceInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
public interface ClientHandler {

   /**
    * 查询提供该服务的服务列表
    * @param serverClass
    * @return
    */
   List<ServiceInfo> lookupServices(Class serverClass);

   /**
    * 更新某个服务提供者节点的结点信息 存储数据 如响应时间
    * 负载均衡是从客户端的信息（响应时间等）来综合判断
    * @param serverClass
    * @param serviceInfo
    * @param data
    */
   void updateServiceData(Class serverClass,ServiceInfo serviceInfo,String data);

   /**
    * 监听服务 是否修改 更新后会回调ServerListener
    * @param serverClass
    * @param serverListener
    */
   void addListener(Class serverClass,ServerListener serverListener);

   boolean removeListener(Class serverClass,ServerListener serverListener);



}
