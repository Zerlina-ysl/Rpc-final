package com.atcompany.rpc.consumer.handler;

import com.atcompany.rpc.consumer.msg.InBoundMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/16
 * 客户端处理类
 *  1 - 发送消息
 *  2 - 接收消息
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<String> {

    InBoundMsg<String> inBoundMsg;

    public RpcClientHandler(InBoundMsg<String> inBoundMsg) {

        this.inBoundMsg=inBoundMsg;
    }

    /**
     * 通道读取就绪事件 调用io线程
     * @param channelHandlerContext
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        inBoundMsg.receive(s);
    }
}
