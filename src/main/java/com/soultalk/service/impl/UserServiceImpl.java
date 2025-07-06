package com.soultalk.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.soultalk.context.BaseContext;
import com.soultalk.mapper.UserEmotionRecordMapper;
import com.soultalk.mapper.UserInfoMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.PageResult;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.BaseService;
import com.soultalk.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private BaseService baseService;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private UserEmotionRecordMapper emotionRecordMapper;

    @Override
    public UserPO info(Long id) {
        UserPO user = userMapper.selectById(id);
        user.setPassword(null);
        return user;
    }

    @Override
    public String update(UserPO user, String introduce, MultipartFile photo) {
        if (!introduce.isEmpty()) {
            user.setIntroduce(introduce);
        }

        if (!photo.isEmpty()) {
            String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);
            if (url != null) {
                user.setPhoto(url);
            }
        }

        userMapper.update(user);
        return user.getName() + " 修改成功";
    }

    @Override
    public UserInfoPO getDetailInfo() {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        UserInfoPO userInfo = userInfoMapper.getDetailInfoByUserId(userId);
        return userInfo;
    }

    @Override
    public void updateDetailInfo(UserInfoPO userInfo) {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        userInfo.setUserId(userId);
        userInfoMapper.updateDetailInfo(userInfo);
    }

    @Override
    public PageResult getEmotionPageList(Integer page, Integer size) {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        PageHelper.startPage(page, size);
        Page<UserEmotionRecordPO> list =  emotionRecordMapper.getEmotionPageList(userId);
        List<UserEmotionRecordPO> records = list.getResult();
        return new PageResult(list.getTotal(),records);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        emotionRecordMapper.deleteBatch(ids);
    }

    @Override
    public List<UserEmotionRecordPO> getEmotionChart(Integer items, Integer days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        List<UserEmotionRecordPO> list = emotionRecordMapper.getEmotionChatList(userId, items, startTime,endTime);
        return list;
    }


}
