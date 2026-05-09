package com.soultalk.mapper;

import com.soultalk.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    UserPO selectByName(String name);

    UserPO selectById(Long id);

    Long getTimeById(Long id);

    Integer countByName(String name);

    void setMemoryIdToId(Long id, String memoryId);

    void setMemoryInfoIdToId(Long id, String memoryInfoId);

    void setSessionIdToId(Long id, String sessionId);

    void insert(UserPO user);

    void update(UserPO user);

    void deleteById(Long id);

    UserPO getByOpenId(String openid);

    List<UserPO> selectAll();
}
