package com.soultalk.mapper;

import com.github.pagehelper.Page;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    UserPO selectByName(String name);

    UserPO selectById(Long id);

    Long getTimeById(Long id);

    Integer countByName(String name);

    void insert(UserPO user);

    void update(UserPO user);


}
