package com.atcompany.rpc.provider.Server;

import com.atcompany.rpc.provider.Handler.RpcServerHandler;
import com.center.rpc.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 启动类
 */
@Service
public class RpcServer implements DisposableBean {


    NioEventLoopGroup bossGroup;


    NioEventLoopGroup workerGroup;


    @Autowired
    RpcServerHandler rpcServerHandler;


    @Autowired
    ServerHandler serverHandler;

    @Override
    public void destroy() throws Exception {


        if(bossGroup!=null){
            bossGroup.shutdownGracefully();
        }
        if(workerGroup!=null){
            workerGroup.shutdownGracefully();
        }



    }


    public void startServer(String ip,int port)  {

        try {
            //1. 创建线程池
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            //2. 创建服务端启动助手
            ServerBootstrap serverBootstrap = new ServerBootstrap();


            //3. 设置参数
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            //网络字节通过$分割解决黏包问题`
                            ByteBuf byteBuf = Unpooled.copiedBuffer("$".getBytes(StandardCharsets.UTF_8));
                            pipeline.addLast(new DelimiterBasedFrameDecoder(2048,byteBuf));
                            //添加String的编码解码器
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            //业务处理类
                            pipeline.addLast(rpcServerHandler);
                        }
                    });
            //绑定端口
            ChannelFuture sync = serverBootstrap.bind(ip, port).sync();
            System.out.println("服务提供方开始提供服务...");
            sync.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if(future.isSuccess()){
                        //端口启动成功后的回调方法
                        //注册服务到zk
                        Set<String> services = rpcServerHandler.listServices();
                        for(String service:services){
                            serverHandler.registerServer(Class.forName(service),ip,port);
                        }
                    }
                }
            });
            System.out.println("-------服务端启动成功，port:"+port+"------------");
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if(bossGroup!=null){
                bossGroup.shutdownGracefully();
            }
            if(workerGroup!=null){
                workerGroup.shutdownGracefully();
            }
        }
    }




}
