package com.atcompany.rpc.consumer.client;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 负载均衡
 */
public interface LoadBalance<T> {
    /**
     *不同选择策略可以通过继承本类重写pickOne()
     * @return
     */
    public T pickOne();



}
