package com.atcompany.rpc.provider.mapper;

import com.center.rpc.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: luna
 * Date: 2022/3/15
 */
@Mapper
public interface UserMapper {


    @Select("select name,age,id,gender from tb_user where  id=#{id}")
    public User getUser(int id);



}
