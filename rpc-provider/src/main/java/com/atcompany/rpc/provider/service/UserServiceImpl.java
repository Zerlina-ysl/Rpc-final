package com.atcompany.rpc.provider.service;

import com.atcompany.rpc.provider.mapper.UserMapper;
import com.center.rpc.api.IUserService;
import com.atcompany.rpc.provider.anno.RpcService;
import com.center.rpc.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 */
@Service
@RpcService
public class UserServiceImpl implements IUserService {





    @Autowired
    UserMapper umapper;

    @Override
    public User getUserById(int id) {
        return umapper.getUser(id);
    }
}
