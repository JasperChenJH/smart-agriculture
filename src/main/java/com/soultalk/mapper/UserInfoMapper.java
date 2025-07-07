package com.soultalk.mapper;

import com.soultalk.po.UserInfoPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper {

    UserInfoPO getDetailInfoByUserId(Long userId);

    void updateDetailInfo(UserInfoPO userInfo);

    void insertDetailInfo(UserInfoPO userInfo);

}
