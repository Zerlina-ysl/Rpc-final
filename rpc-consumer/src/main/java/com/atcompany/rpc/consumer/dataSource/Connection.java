package com.atcompany.rpc.consumer.dataSource;

import com.atcompany.rpc.consumer.msg.OutBoundMsg;
import com.center.rpc.pojo.ServiceInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
public interface Connection extends Closeable {

    boolean isActive();


    /**
     * 连接的唯一标识
     * @return
     */
    String id();


    ServiceInfo connectInfo();


    /**
     * public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");
     * 不可实例化的占位符
     * @return
     */
    OutBoundMsg<String,Void> getOutBoundMsg();



    void recordStatus(ConnectionStatus t);

    ConnectionStatus status();


    /**
     * 内部类，主要记录响应时间和运行时长
     */
    @Getter
    @Setter
    public static class ConnectionStatus{

        private long recordTime;

        private int responseMillisSeconds;


    }
}
