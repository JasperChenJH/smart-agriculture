package com.soultalk.mapper;

import com.soultalk.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    UserPO selectByName(String name);

    Integer countByName(String name);

    void insert(UserPO user);

    void update(UserPO user);

    UserPO selectByNameAndPassword(String name, String password);
}
