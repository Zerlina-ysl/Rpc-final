package com.center.rpc.common;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 封装请求对象
 */
@Getter
@Setter
public class RpcRequest {

    /**
     * 请求对象id
     */
    private String requestId;

    /**
     * 请求类名
     */
    private String className;

    /**
     * 请求方法名
     */
    private String methodName;

    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;

    /**
     * 传入参数
     */
    private Object [] parameters;

}

