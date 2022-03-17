package com.atcompany.rpc.consumer.msg;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * 信息流出
 * Date: 2022/3/15
 */
public interface OutBoundMsg<P,R> {

    Future<R> sendMsg(P msg) throws IOException;




}
