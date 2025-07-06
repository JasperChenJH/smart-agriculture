package com.soultalk.mapper;

import com.soultalk.po.UserInfoPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserInfoMapper {

    UserInfoPO getDetailInfoByUserId(Long userId);

    void updateDetailInfo(UserInfoPO userInfo);

    void insertDetailInfo(UserInfoPO userInfo);

}
