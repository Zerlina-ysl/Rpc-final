package com.center.rpc.common;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 封装响应对象
 */
@Getter
@Setter
public class RpcResponse {


    /**
     * 返回信息要携带请求信息的id
     */
    private String requestId;


    /**
     * 错误信息
     */
    private String error;


    private Object result;





}
