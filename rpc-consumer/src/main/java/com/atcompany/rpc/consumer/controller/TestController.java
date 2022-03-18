package com.atcompany.rpc.consumer.controller;

import com.atcompany.rpc.consumer.proxy.RpcClientProxy;
import com.center.rpc.api.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.center.rpc.pojo.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 入口
 */
@Controller
public class TestController {



    @Autowired
    private RpcClientProxy rpcClientProxy;


    @ResponseBody
    @RequestMapping(value="/getUser")
        public Object getUsersByName(int id){

        //使用代理对象
        IUserService userService = (IUserService) rpcClientProxy.createProxy(IUserService.class);

        return userService.getUserById(id);



    }



}
