package com.center.rpc.handler;

import com.center.rpc.pojo.ServiceInfo;
import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;

import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.*;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
public class ZookeeperServerHandler implements ServerHandler,ClientHandler{
    /**
     * 分割符
     */
    private static final String ZK_PATH_SPLIT = "/";
    private static final String RPC_ROOT_PATH= "/lsyRpc";
    private static final String charset = "utf-8";
    private CuratorFramework client;
    private Collection<ServerListenerFilter> listeners = new ConcurrentLinkedQueue<>();
    private AtomicBoolean watcherAdded = new AtomicBoolean(false);

    public ZookeeperServerHandler(CuratorFramework client) {
        this.client = client;
    }

    @Override
    public List<ServiceInfo> lookupServices(Class serverClass) {
        try {
            List<String> children = listChildren(buildServerPath(serverClass));
            //流式操作
            return children.stream().map(s->trans(s)).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }




    @Override
    public void updateServiceData(Class serverClass, ServiceInfo serviceInfo, String data) {

        try {
            updateData(buildServerNodePath(serverClass,serviceInfo.getIp(),serviceInfo.getPort()),data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void addListener(Class serverClass, ServerListener serverListener) {
        ServerListenerFilter serverListenerFilter = new ServerListenerFilter(serverListener, serverClass);
        listeners.add(serverListenerFilter);
        if(!watcherAdded.get()&&watcherAdded.compareAndSet(false,true)){
            registerWatcher(buildServerPath(serverClass));
        }

    }



    /**
     * ServerHandler 创建一个临时节点
     * @param serverClass
     * @param ip
     * @param port
     * @return
     */
    @Override
    public boolean registerServer(Class serverClass, String ip, int port) {

        try {
            //初始时data为null 信息是从客户端传递
            createNode(buildServerNodePath(serverClass,ip,port),null, CreateMode.EPHEMERAL);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }

    /**
     * zk中创建节点 curator封装了对zookeeper的结点操作
     * @param buildServerNodePath
     * @param data
     * @param createNode
     */
    private void createNode(String buildServerNodePath, String data, CreateMode createNode) throws Exception {
        if(data==null){
            //存在父节点删除 防止报错node for
            client.create().creatingParentContainersIfNeeded()
                    .withMode(createNode).forPath(buildServerNodePath);
        }else{
            client.create().creatingParentContainersIfNeeded()
                    .withMode(createNode).forPath(buildServerNodePath,data.getBytes(charset));
        }

    }


    private static String buildServerNodePath(Class serverClass, String ip, int port) {

        String path = buildServerPath(serverClass)+ZK_PATH_SPLIT+ip+":"+port;
        System.out.println("在zk中即将创建的节点路径为："+path);
        //                       /lsyRpc/serverClass.getName()/lsyRpc/ip:port
        return  path;

    }

    /**
     * 服务前缀
     * @param serverClass
     * @return
     */
    private static String buildServerPath(Class serverClass) {
        String path = RPC_ROOT_PATH+ZK_PATH_SPLIT+serverClass.getName();
        //  /lsyRpc/serverClass.getName()
        System.out.println("在zk中即将创建的节点路径前缀为："+path);
        return path;


    }
    private List<String> listChildren(String buildServerPath) throws Exception {
        List<String> children = client.getChildren().forPath(buildServerPath);
        return children;
    }

    /**
     * 获得ip port并传递给serviceInfo
     * @param server
     * @return
     */
    private static ServiceInfo trans(String server) {

        ServiceInfo serviceInfo = new ServiceInfo();
        String[] split = server.split(":");
        serviceInfo.setIp(split[0]);
        serviceInfo.setPort(Integer.parseInt(split[1]));
        return serviceInfo;


    }


    private void registerWatcher(String buildServerPath) {
        CuratorCacheListener curatorCacheListener = CuratorCacheListener
                .builder()
                .forPathChildrenCache(buildServerPath,client,(c,e)->{
                   //数据节点子节点（包括子节点数据）的变化
                    Iterator<ServerListenerFilter> iterator = listeners.iterator();
                    PathChildrenCacheEvent.Type type = e.getType();

                    while(iterator.hasNext()){
                        //针对监听事件适配
                        //监听zk事件并转换为自定义事件
                        ServerListenerFilter next = iterator.next();
                        switch (type){
                            case CHILD_ADDED:
                                next.serverAdded(e.getData().getPath());
                                break;
                            case CHILD_REMOVED:
                                next.serverRemoved(e.getData().getPath());
                                break;
                            case CHILD_UPDATED:
                                byte[] dataByte = e.getData().getData();
                                String data = dataByte == null ? null : new String(dataByte, charset);

                                next.serverUpdated(e.getData().getPath(),data);
                                break;
                            default:
                                break;
                         }
                    }
                })
//                .forNodeCache(()->{
//        //根据数据节点本身的变化，监听节点是否存在
//                    System.out.println("nodeCache");
//                })
                .build();

        CuratorCache cache = CuratorCache.build(client, buildServerPath);
        cache.listenable()
                .addListener(curatorCacheListener, Executors.newSingleThreadExecutor());
        cache.start();


    }

    @Override
    public boolean removeListener(Class serverClass, ServerListener serverListener) {
        return listeners.removeIf(oldServerListenerFilter->{
            if(oldServerListenerFilter.getServerListener().equals(serverListener)
            && serverClass==oldServerListenerFilter.getServerClass()){
                return true;
            }else{
                return false;
            }
        });
    }

    private void updateData(String buildServerNodePath, String data) throws Exception {

        Stat stat = client.setData().forPath(buildServerNodePath,data.getBytes(charset));
    }



    /**
     * 存放在zk节点的路径转换为定义的ServiceInfo对象
     */
    @Getter
    private static class ServerListenerFilter implements ServerListener{

        private String prefixMatchPath;

        private ServerListener serverListener;

        private Class serverClass;

        public ServerListenerFilter( ServerListener serverListener, Class serverClass) {
       this.prefixMatchPath=buildServerPath(serverClass)+ZK_PATH_SPLIT;
            this.serverListener = serverListener;
            this.serverClass = serverClass;
        }

        @Override
        public void serverAdded(ServiceInfo service) {
            serverListener.serverAdded(service);

        }

        @Override
        public void serverRemoved(ServiceInfo service) {

            serverListener.serverRemoved(service);
        }

        @Override
        public void serverUpdated(ServiceInfo service, String data) {

            serverListener.serverUpdated(service,data);
        }

        /**方法重载
         * 传递的是path路径 需要把path路径转换为serviceinfo
         * @param server
         */
        public void serverAdded(String server){
            String serverInfo = server.replaceFirst(prefixMatchPath, "");
            if(serverInfo.length()!=server.length()){
                serverAdded(trans(serverInfo));
            }
        }
        public void serverUpdated(String server,String data){
            String serverInfo = server.replaceFirst(prefixMatchPath, "");
            if(serverInfo.length()!=server.length()){
                serverUpdated(trans(serverInfo),data);
            }
        }
        public void serverRemoved(String server){
            String serverInfo = server.replaceFirst(prefixMatchPath, "");
            if(serverInfo.length()!=server.length()){
                serverRemoved(trans(serverInfo));
            }
        }



    }
}
