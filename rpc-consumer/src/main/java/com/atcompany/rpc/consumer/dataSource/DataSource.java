package com.atcompany.rpc.consumer.dataSource;

import java.io.Closeable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 定义与连接有关的操作
 */
public interface DataSource extends Closeable {

    String getServiceName();


    Connection getConnection();

    void remove(String connectionId);

    List<Connection> listById(String connectionId);

    void add(Connection connection);

    void addListener(DataSourceListener dataSourceListener);

    void removeListener(DataSourceListener dataSourceListener);

    /**
     * 由datasource通知节点数据的更新
     */
    public interface  DataSourceListener{
        void connectionUpdated(Connection connection);
    }




}
