package com.atcompany.rpc.consumer.proxy;

import com.alibaba.fastjson.JSON;
import com.center.rpc.common.RpcRequest;
import com.center.rpc.common.RpcResponse;
import com.atcompany.rpc.consumer.msg.OutBoundMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 客户端代理类创建代理对象
 * 1 - 封装request请求
 * 2 - 创建RpcClient对象
 * 3 - 发送消息
 * 4 - 返回结束
 */
@Service
public class RpcClientProxy {

    @Autowired
    private OutBoundMsg<RpcRequest, RpcResponse> outBoundMsg;




    public Object createProxy(Class serviceClass){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{serviceClass}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        //---1.封装request对象
                        RpcRequest rpcRequest = new RpcRequest();
                        //请求id赋值
                        rpcRequest.setRequestId(UUID.randomUUID().toString());

                        //代理对象类名赋值
                        rpcRequest.setClassName(method.getDeclaringClass().getName());

                        //请求方法名赋值
                        rpcRequest.setMethodName(method.getName());

                        rpcRequest.setParameters(args);
                        rpcRequest.setParameterTypes(method.getParameterTypes());

                        System.out.println(rpcRequest.toString());
                        // OutBoundMsg<RpcRequest,RpcResponse>
                        // Future<R> sendMsg(P msg) throws IOException;
                        try {
                            // 2 - 发送消息
                            Future<RpcResponse> rpcResponseFuture = outBoundMsg.sendMsg(rpcRequest);

                            //Waits if necessary for the computation to complete, and then retrieves its result.
                            //业务线程在等待远程响应 同步化处理
                            RpcResponse rpcResponse = rpcResponseFuture.get();

                            if(rpcResponse.getError()!=null){
                                throw new RuntimeException(rpcResponse.getError());
                            }

                            // 3 - 返回结果
                            Object result = rpcResponse.getResult();
                            //public static <T> T parseObject(String text, Class<T> clazz)
                            return JSON.parseObject(result.toString(),method.getReturnType());
                        } catch (Exception e) {
                            throw e;
                        }

                    }
                }
        );
    }


}
