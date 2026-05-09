package com.soultalk.mapper;

import com.soultalk.po.AdminPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper {
    AdminPO selectByNickname(String nickname);

    AdminPO selectById(Long adminId);

    void insert(AdminPO admin);

    void update(AdminPO admin);

    void deleteById(Integer adminId);
}