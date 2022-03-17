package com.atcompany.rpc.consumer.msg;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 对应OutBoundMsg
 * 信息流入
 */
public interface InBoundMsg<T> {
    public void receive(T msg);


}
