package com.atcompany.rpc.provider.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 对外暴露服务接口
 * 使用元注解，注解其他注解
 */
//注解的适用范围：用于接口或类上
@Target(ElementType.TYPE)
//定义该Annotation被保留的时间长短 在运行时可以获取
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
}
