package com.atcompany.rpc.provider.Handler;

import com.alibaba.fastjson.JSON;
import com.center.rpc.common.RpcRequest;
import com.center.rpc.common.RpcResponse;
import com.atcompany.rpc.provider.anno.RpcService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.BeansException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 *  服务端业务处理类,存在请求时会进入该类进行处理
 *  1. 缓存标有@RpcService的bean
 *  2. 接收客户端请求
 *  3. 根据传递过来的beanName从缓存中查找对应的bean
 *  4. 解析请求中的方法名称，参数类型，参数信息
 *  5. 反射调用bean的方法
 *  6. 响应客户端的请求
 *
 */

@Component
@ChannelHandler.Sharable
public class RpcServerHandler extends SimpleChannelInboundHandler<String> implements ApplicationContextAware {



    private static final Map SERVICE_INSTANCE_MAP = new ConcurrentHashMap();



    /**
     * 使用线程安全的map缓存标有@RpcService的bean和对应接口的名称
     * @param applicationContext 上下文变量
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String,Object> serviceMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(serviceMap!=null&&serviceMap.size()>0){
            Set<Map.Entry<String,Object>> entries = serviceMap.entrySet();
            for(Map.Entry<String,Object> items:entries){
                Object serviceBean = items.getValue();
                System.out.println("RpcServerHandler获取到的serviceBean:"+serviceBean);
                if(serviceBean.getClass().getInterfaces().length==0){
                    throw new RuntimeException("服务必须实现接口");
                }
                //默认取第一个接口作为缓存bean的名称
                String name = serviceBean.getClass().getInterfaces()[0].getName();
                System.out.println("加载服务类..."+name);
                //接口名称 类名
                SERVICE_INSTANCE_MAP.put(name,serviceBean);


            }
        }
        System.out.println("已加载全部服务接口："+serviceMap);

    }




    /**
     * 服务器读取通道已经就绪的信息 不用关心释放资源
     * extends SimpleChannelInboundHandler implements ChannelInboundHandler
     * @param channelHandlerContext
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {

        //1.接收客户端信息，并转化为RpcRequest对象
        //return JSON.parseObject(result.toString(),method.getReturnType()); consumer-RpcClientProxy
        RpcRequest rpcRequest = JSON.parseObject(s,RpcRequest.class);
        RpcResponse rpcResponse = new RpcResponse();

        rpcResponse.setRequestId(rpcRequest.getRequestId());
        //业务处理
        try {
            //通过io线程调用业务逻辑，不太合理
            rpcResponse.setResult(handler(rpcRequest));
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            rpcResponse.setError(e.getMessage());
        }
        //6. 响应客户端请求
        channelHandlerContext.writeAndFlush(JSON.toJSONString(rpcResponse)+"$");


    }

    /**
     * 处理客户端发送到服务器的请求 handler(RpcRequest)
     * @param rpcRequest
     * @return
     */
    private Object handler(RpcRequest rpcRequest) throws InvocationTargetException {
        //3. 根据传递过来的RpcRequest的beanName从缓存中查找对应的bean
        //那么rpc.getClassName()应该是接口名称
        Object serviceBean = SERVICE_INSTANCE_MAP.get(rpcRequest.getClassName());
        if(serviceBean==null){
            throw new RuntimeException("根据当前beanName找不到服务，beanName:"+rpcRequest.getClassName());
        }
        //4. 解析请求中的方法名称，参数类型，参数信息
        Class<?> serviceBeanClass = serviceBean.getClass();
        System.out.println("customer.RpcServerHandler.handler请求的方法类名："+serviceBeanClass);
        String methodName = rpcRequest.getMethodName();
        System.out.println("请求的方法名："+methodName);
        Class<?>[] parametersTypes = rpcRequest.getParameterTypes();
        System.out.println("请求的参数类型长度："+parametersTypes.length);

        Object[] parameters = rpcRequest.getParameters();
        System.out.println("请求的参数长度："+parameters.length);
        System.out.println("请求的参数："+parameters[0]);
        //5. CGLIB反射调用bean的方法
        FastClass fastClass = FastClass.create(serviceBeanClass);
        FastMethod method = fastClass.getMethod(methodName,parametersTypes);
        return method.invoke(serviceBean,parameters);


    }

    /**
     * 将所有使用注解的类名封装到hashset中
     * @return
     */
    public Set<String> listServices() {

        return  new HashSet<>(SERVICE_INSTANCE_MAP.keySet());

    }
}
