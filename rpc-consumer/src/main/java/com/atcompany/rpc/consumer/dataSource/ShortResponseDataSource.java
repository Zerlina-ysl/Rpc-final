package com.atcompany.rpc.consumer.dataSource;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 */
public class ShortResponseDataSource extends LoadDataSource{

    /**
     * 自定义结构体判定最小
     */
    private final Comparator<Connection> compareTo = new ShortResponseComparator();


    private final static Vector<ShortResponseDataSource> VECTOR  = new Vector<>();

    /**
     * 静态线程池
     */
    private final static ScheduledThreadPoolExecutor SCHEDULED_THREAD_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(2);


    static{
        //设置执行周期 下次执行时间是上次任务执行完成的系统时间+period
        //执行时间不固定，但是周期固定 参与相对固定的延迟执行任务
        System.out.println("task start time:"+new Date().toString());
        SCHEDULED_THREAD_POOL_EXECUTOR.scheduleWithFixedDelay(()->{

            try {

                for(ShortResponseDataSource shortResponseDataSource:VECTOR){
                    //获取当前所有连接
                    List<Connection> connections = shortResponseDataSource.connections();
                    //获取当前所有监听器
                    Collection<DataSourceListener> listeners = shortResponseDataSource.listeners();
                    if(listeners==null||listeners.size()==0){
                        return ;
                    }
                    for(Connection connection:connections){
                        Connection.ConnectionStatus status = connection.status();
                        if(status!=null){
                            for(DataSourceListener listener:listeners){
                                listener.connectionUpdated(connection);
                            }
                        }
                    }
                }
                System.out.println("task finish time:"+new Date().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

//任务初始延迟1s 任务执行间隔5s
//每5s上报一次
        },0,5, TimeUnit.SECONDS);




    }


    public ShortResponseDataSource(String serviceName) {
        super(serviceName);
        VECTOR.add(this);
    }

    /**
     * 定义负载均衡
     * @return
     */
    @Override
    public Connection pickOne() {
//不适合堆排序，因为节点的更新会导致二叉树的不断重排序而使效率变慢
        List<Connection> connections = connections();
        Connection connection = connections.get(0);
        for(int i=1;i<connections.size();i++){
            //少量数据下的o(n)
            if(compareTo.compare(connection,connections.get(i))>0){
                connection=connections.get(i);
            }
        }
        return connection;
    }
    private static class ShortResponseComparator implements Comparator<Connection>{
        /**
         * 定义负载均衡规则：
         *  两者运行时间相同，随意
         *  选取运行时间长的
         * @param o1
         * @param o2
         * @return
         */
        @Override
        public int compare(Connection o1, Connection o2) {
            Connection.ConnectionStatus status1 = o1.status();
            Connection.ConnectionStatus status2 = o2.status();
            if(status1!=null&&(status1.getRecordTime()+ 50000 )<System.currentTimeMillis()){
                status1=null;
            }
            if(status2!=null&&(status2.getRecordTime()+ 50000 )<System.currentTimeMillis()){
                status2=null;
            }
            //为空说明还不知道该链接的响应时长 优先测试
            if(status1==null&&status2==null){
                return 0;
            }
            if(status1==null||status2==null){
                return -1;
            }
            if(status1.getResponseMillisSeconds()==status2.getResponseMillisSeconds()){
                return ThreadLocalRandom.current().nextInt(-1,2);
            }

            return status1.getResponseMillisSeconds()-status2.getResponseMillisSeconds();
        }
    }
}
