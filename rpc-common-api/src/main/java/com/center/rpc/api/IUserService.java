package com.center.rpc.api;

import com.center.rpc.pojo.User;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 * 用户服务 对外提供的api接口
 */
public interface IUserService {


     User getUserById(int id);
}
