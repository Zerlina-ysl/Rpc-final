package com.atcompany.rpc.consumer.dataSource;

import com.alibaba.fastjson.JSON;
import com.center.rpc.handler.ClientHandler;
import com.center.rpc.handler.ServerListener;
import com.center.rpc.pojo.ServiceInfo;
import com.atcompany.rpc.consumer.msg.InBoundMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 管理服务的连接池 线程安全类
 */
@Service
public class DataSourceManager implements Closeable {

    @Autowired
    private ClientHandler clientHandler;

    @Autowired
    private InBoundMsg<String> inBoundMsg;

    private ConcurrentHashMap<String,DataSource> connectionsMap = new ConcurrentHashMap<String, DataSource>();


    public Connection getConnection(String serviceName){
        //对于其内部定义了获取connection的选择策略
        return getDataSource(serviceName).getConnection();
    }

    private DataSource getDataSource(String serviceName) {
        DataSource dataSource = connectionsMap.computeIfAbsent(serviceName, key -> {

            DataSource source = defaultSource(serviceName);

            try {
                Class<?> aClass = Class.forName(serviceName);
                //寻找该服务中提供信息的节点
                List<ServiceInfo> serviceInfos = clientHandler.lookupServices(aClass);
                for (ServiceInfo serviceInfo : serviceInfos) {
                    //根据节点建立RPC连接
                    source.add(buildConnection(serviceInfo));
                    System.out.println(serviceName + ",服务初始化上线，服务提供者：" + serviceInfo);
                }
                //添加监听
                addListener(serviceName);

                source.addListener(connection -> {
                    clientHandler.updateServiceData(aClass, connection.connectInfo(), JSON.toJSONString(connection.status()));
                });


            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(serviceName + "服务不存在");
            }
            return source;
        });
        return dataSource;
    }




    /**初始化一个空的无连接dataSource
     * 返回不同负载均衡的方法获取到的DataSource
     * @param serviceName
     * @return
     */
    private DataSource defaultSource(String serviceName){
//        return new ShortResponseDataSource(serviceName);
        return new RoundLoadDataSource(serviceName);
    }

    /**
     * 监听节点行为（新增、下线、更新） 并做出响应
     * @param serviceName
     * @throws ClassNotFoundException
     */
    public void addListener(String serviceName) throws ClassNotFoundException {
        Class<?> aClass = Class.forName(serviceName);
        clientHandler.addListener(aClass,new ServerListener(){

            /**
             * 新增节点服务
             * @param service
             */
            @Override
            public void serverAdded(ServiceInfo service){

                try {
                    DataSource dataSource = connectionsMap.computeIfAbsent(serviceName, key -> {
                        return defaultSource(key);
                    });
                    if(dataSource.listById(buildSign(service)).size()>0){
                        System.out.println(serviceName+",服务已经上线该服务提供者："+service);
                        return ;
                    }
                    Connection connection = buildConnection(service);
                    dataSource.add(connection);
                    System.out.println(serviceName+",服务上线提供服务者："+service);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * 节点下线
             * @param service
             */
            @Override
            public void serverRemoved(ServiceInfo service) {
                DataSource dataSource = connectionsMap.get(serviceName);
                if(dataSource==null){
                    return ;
                }

                dataSource.remove(buildSign(service));
                System.out.println(serviceName+",服务下线服务提供者："+service);
            }

            @Override
            public void serverUpdated(ServiceInfo service, String data) {
                DataSource dataSource = connectionsMap.get(serviceName);
                if(dataSource==null){
                    return ;
                }
                Connection.ConnectionStatus connectionStatus = null;
                if(data!=null&&data.length()>0){
                    connectionStatus = JSON.parseObject(data,Connection.ConnectionStatus.class);

                }
                String sign = buildSign(service);
                List<Connection> connections = dataSource.listById(sign);
                for(Connection connection:connections){
                    connection.recordStatus(connectionStatus);
                }


            }
        });
    }

    private Connection buildConnection(ServiceInfo serviceInfo) throws IOException {

       Connection connectionInfo  = new NettyConnection(serviceInfo, buildSign(serviceInfo), inBoundMsg);

       return connectionInfo;

    }

    private String buildSign(ServiceInfo serviceInfo) {
        return serviceInfo.getIp()+":"+serviceInfo.getPort();
    }


    @Override
    public void close() throws IOException {

        for(DataSource dataSource:connectionsMap.values()){
            if(dataSource!=null){
                dataSource.close();
            }
        }
    }
}
