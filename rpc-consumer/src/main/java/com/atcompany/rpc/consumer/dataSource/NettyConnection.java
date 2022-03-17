package com.atcompany.rpc.consumer.dataSource;

import com.atcompany.rpc.consumer.handler.RpcClientHandler;
import com.atcompany.rpc.consumer.msg.InBoundMsg;
import com.atcompany.rpc.consumer.msg.OutBoundMsg;
import com.center.rpc.pojo.ServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 封装与nettyConnection相关的属性以及get/set方法和isActive方法
 * 通过netty建立一个长链接
 */
public class NettyConnection implements Connection{

    private ServiceInfo serviceInfo;

    private  String id;

    private InBoundMsg<String> inBoundMsg;

    private volatile EventLoopGroup group;

    private volatile Channel channel;

    private  volatile ChannelPipeline pipeline;

    private volatile ConnectionStatus connectionStatus;


    private OutBoundMsg<String,Void> outBoundMsg = new OutBoundMsg<String, Void>() {
        @Override
        public Future<Void> sendMsg(String msg) throws IOException {
            return pipeline.writeAndFlush(msg+"$");
        }
    };

    public NettyConnection(ServiceInfo serviceInfo, String id, InBoundMsg<String> inBoundMsg) throws IOException {
        this.serviceInfo = serviceInfo;
        this.id = id;
        this.inBoundMsg = inBoundMsg;
        //初始化连接的同时为其他三个成员变量赋值
        this.init();
    }

    /**
     * 初始化方法-连接netty服务器
     * @return
     */
    private Connection init() throws IOException {

        try {
            // 1. 创建线程组
            NioEventLoopGroup group = new NioEventLoopGroup();

            //2. 创建启动助手
            Bootstrap bootstrap = new Bootstrap();

            //3. 设置参数
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE,Boolean.TRUE)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            ByteBuf byteBuf = Unpooled.copiedBuffer("$".getBytes(StandardCharsets.UTF_8));
                            pipeline.addLast(new DelimiterBasedFrameDecoder(2048,byteBuf));

                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());

                            //添加客户端处理类
                            pipeline.addLast(new RpcClientHandler(inBoundMsg));
                        }
                    });
            //4.连接netty服务器
            Channel channel = bootstrap.connect(this.serviceInfo.getIp(), this.serviceInfo.getPort()).sync().channel();
            ChannelPipeline pipeline=channel.pipeline();
            this.pipeline = pipeline;
            this.channel=channel;
            this.group=group;
        } catch (InterruptedException e) {
            e.printStackTrace();
            close();
        }
        return this;

    }

    @Override
    public boolean isActive() {
      if(channel==null){
          return false;
      }
      return this.channel.isActive();
    }

    /**
     * 返回id
     * @return
     */
    @Override
    public String id() {
        return id;
    }

    @Override
    public ServiceInfo connectInfo() {
        return serviceInfo;
    }

    @Override
    public OutBoundMsg<String, Void> getOutBoundMsg() {
        return outBoundMsg;
    }

    @Override
    public void recordStatus(ConnectionStatus t) {
    this.connectionStatus=connectionStatus;
    }

    @Override
    public ConnectionStatus status() {
        return connectionStatus;
    }

    /**
     * 关闭连接
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        try {
            if(this.channel!=null){
                channel.close();
            }
            if(this.group!=null){
                group.shutdownGracefully();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
