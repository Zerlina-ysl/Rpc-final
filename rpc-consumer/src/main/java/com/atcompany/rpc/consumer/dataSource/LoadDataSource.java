package com.atcompany.rpc.consumer.dataSource;

import com.atcompany.rpc.consumer.client.LoadBalance;
import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 抽象类可以不用实现接口的全部方法
 */
@Getter
abstract class LoadDataSource implements DataSource, LoadBalance<Connection> {

    private final String serviceName;

    /**
     * 不同的负载均衡策略需要不同的数据结构
     *
     */
    private List<Connection> list;

    private Collection<DataSourceListener> listeners = new ConcurrentLinkedQueue();

    public LoadDataSource(String serviceName) {
        this.serviceName = serviceName;
        //初始化存放connection的list
        this.list = newList();
    }

    private List<Connection> newList() {
        //ArrayList的线程安全实现
        //大量查询 读多写少
        return new CopyOnWriteArrayList<>();
    }




    @Override
    public Connection getConnection() {
        if(connections().size()==0){
            System.out.println("没有可用的连接...");
        }
        return popConnection(pickOne());


    }

    @Override
    public void remove(String connectionId) {
        list.removeIf(new Predicate<Connection>() {
            @Override
            public boolean test(Connection connection) {
                if(connectionId.equals(connection.id())){
                    try {
                        connection.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

    }

    /**
     * 与connectionId相等的connection存储到result中
     * @param connectionId
     * @return
     */
    @Override
    public List<Connection> listById(String connectionId) {
        List<Connection> result = new ArrayList<>();
        for(Connection connection:list){
            if(connectionId.equals(connection.id())){
                result.add(popConnection(connection));
            }
        }

        return result;
    }

    /**
     * 将connection添加到list 若已存在则不添加
     * @param connection
     */
    @Override
    public void add(Connection connection) {
        for(Connection old:list){
            if(old.id().equals(connection.id())){
                return;
            }

        }
        list.add(connection);

    }

    @Override
    public void addListener(DataSourceListener dataSourceListener) {
        listeners.add(dataSourceListener);
    }

    @Override
    public void removeListener(DataSourceListener dataSourceListener) {
        listeners.remove(dataSourceListener);
    }

    /**
     * 通过负载均衡策略选取节点后的pickone操作重新封装
     *      * 外界使用connection无法感知 即使进行close操作 因此可能会返回无用连接
     * 使用PooledConnection进行再次封装
     * @param connection
     * @return
     */
    private Connection popConnection(Connection connection) {
        return new PooledConnection(this,connection).getProxyConnection();
    }

    protected Collection<DataSourceListener> listeners(){
        return listeners;
    }
    protected List<Connection> connections() {

        return list;

    }

    /**
     * 关闭list中的所有连接
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

        for(Connection connection:list){
            connection.close();
        }
    }



    /**
     * connection close 从datasource中删除
     */
    @Getter
    class PooledConnection implements InvocationHandler {
      private final String CLOSE = "close";

      private final Class<?>[] IFACES = new Class<?>[]{Connection.class};

      private final LoadDataSource loadDataSource;
      private final Connection realConnection;
      private final Connection proxyConnection;

        public PooledConnection(LoadDataSource loadDataSource, Connection realConnection) {
            this.loadDataSource = loadDataSource;
            this.realConnection = realConnection;
            this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),IFACES,this);
        }

        /**
         * 该类的封装主要是通过代理实现，防止close操作
         * 存在close操作则删除该链接
         * @param proxy
         * @param method
         * @param args
         * @return
         * @throws Throwable
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if(CLOSE.hashCode()==methodName.hashCode()&&CLOSE.equals(methodName)){
                loadDataSource.connections().remove(realConnection);
            }
            return method.invoke(realConnection,args);
        }
    }


}
