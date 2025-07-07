package com.soultalk.service;

import com.soultalk.po.PageResult;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    //获取该name的用户信息
    UserPO info(Long id);

    //更新个人主页信息
    String update(UserPO user, String introduce, MultipartFile photo);

    // 获取个人详细信息
    UserInfoPO getDetailInfo();

    // 更新个人详细信息
    void updateDetailInfo(UserInfoPO userInfo);

    // 分页查询用户的情绪得分记录
    PageResult getEmotionPageList(Integer page, Integer size);

    // 批量删除用户情感得分记录
    void deleteBatch(List<Long> ids);

    // 获取用户情感得分图表数据
    List<UserEmotionRecordPO> getEmotionChart(Integer items, Integer days);

    // 修改密码
    boolean updatePassword(String oldPassword, String newPassword);

    // 更新用户基本信息
    void updateBaseInfo(String introduce, MultipartFile photo);
}
