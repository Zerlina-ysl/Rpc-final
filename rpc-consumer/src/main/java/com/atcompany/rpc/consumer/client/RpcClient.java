package com.atcompany.rpc.consumer.client;

import com.alibaba.fastjson.JSON;
import com.center.rpc.common.RpcRequest;
import com.center.rpc.common.RpcResponse;
import com.atcompany.rpc.consumer.dataSource.DataSourceManager;
import com.atcompany.rpc.consumer.msg.InBoundMsg;
import com.atcompany.rpc.consumer.msg.OutBoundMsg;
import com.atcompany.rpc.consumer.dataSource.Connection;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 *
 * 客户端发送请求
 * 1. 连接netty服务器
 * 2. 提供给调用者主动关闭资源的方法
 * 3. 提供消息发送的方法
 *
 */
@Service
public class RpcClient implements DisposableBean, OutBoundMsg<RpcRequest, RpcResponse>, InBoundMsg<String> {


    private ConcurrentHashMap<String,RecordFuture> map = new ConcurrentHashMap<>();

    /**
     * 连接池
     */
    @Autowired
    DataSourceManager datasourceManager;



    /**
     * 服务器receive客户端的方法
     * @param msg
     */
    @Override
    public void receive(String msg) {

        try {
            System.out.println("----------"+Thread.currentThread()+"----------");
            RpcResponse rpcResponse = JSON.parseObject(msg,RpcResponse.class);
            String requestId = rpcResponse.getRequestId();
            RecordFuture remove = map.remove(requestId);
            if(remove==null){
                throw new RuntimeException("历史消息："+msg);
            }
            //唤醒future的等待线程
            remove.complete(rpcResponse);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Future<RpcResponse> sendMsg(RpcRequest msg) throws IOException {
        //从DataSourceManager获取一个连接通道发送信息
             Connection connection = datasourceManager.getConnection(msg.getClassName());
        connection.getOutBoundMsg().sendMsg(JSON.toJSONString(msg));
        RecordFuture recordFuture = new RecordFuture(connection);
        map.put(msg.getRequestId(),recordFuture);
        //将future返回给上层业务
        return recordFuture;
    }



    @Override
    public void destroy() throws Exception {
        if(datasourceManager!=null){
            datasourceManager.close();
        }





    }
    private static class RecordFuture extends CompletableFuture<RpcResponse> {

        private long startTime;

        private long finishedTime;

        private Connection connection;

        public RecordFuture(Connection connection) {
            this.connection = connection;
            //初始化时记录起始时间
            this.startTime = System.currentTimeMillis();

        }

        @Override
        public boolean complete(RpcResponse value) {
            boolean complete = super.complete(value);
            finished();
            return complete;
        }

        private void finished() {


             finishedTime = System.currentTimeMillis();

            Connection.ConnectionStatus connectionStatus = new Connection.ConnectionStatus();

            connectionStatus.setRecordTime(finishedTime);

            //统计服务发出到服务响应的时长
            connectionStatus.setResponseMillisSeconds((int)(finishedTime-startTime));

            //将响应时长记录到connection中
            connection.recordStatus(connectionStatus);



        }
    }
}

