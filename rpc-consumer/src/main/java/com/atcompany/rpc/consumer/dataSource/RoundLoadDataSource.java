package com.atcompany.rpc.consumer.dataSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 轮询负载策略
 */
public class RoundLoadDataSource extends LoadDataSource{
    /**
     * 全局计数
     */
    private AtomicInteger atomicInteger = new AtomicInteger(0);

    public RoundLoadDataSource(String serviceName) {
      super(serviceName);
    }

    @Override
    public Connection pickOne() {
        int andIncrement = atomicInteger.incrementAndGet();
        if(andIncrement<=0){
            synchronized (atomicInteger){
                //获取当前值
                if(atomicInteger.get()<=0){
                    atomicInteger.set(0);
                }
            }
            andIncrement=atomicInteger.incrementAndGet();
        }
        try {
            int index = andIncrement % connections().size();
            Connection connection = connections().get(index);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return pickOne();
        }


    }
}
