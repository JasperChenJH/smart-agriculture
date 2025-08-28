package com.soultalk.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.soultalk.context.BaseContext;
import com.soultalk.mapper.*;
import com.soultalk.po.PageResult;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.BaseService;
import com.soultalk.service.MainAgentService;
import com.soultalk.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    MainDiaMapper mainDiaMapper;
    @Resource
    private MainAgentService mainAgentService;
    @Autowired
    private BaseService baseService;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private UserEmotionRecordMapper emotionRecordMapper;
    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Resource
    private DiaMapper diaMapper;
    @Autowired
    private UserEmotionRecordMapper userEmotionRecordMapper;

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
    public UserInfoPO getDetailInfo(Long userId) {
        UserInfoPO userInfo = userInfoMapper.getDetailInfoByUserId(userId);
        return userInfo;
    }

    @Override
    public void updateDetailInfo(UserInfoPO userInfo) {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        userInfo.setUserId(userId);

        //清除session
        userMapper.setSessionIdToId(userId, null);

        // 先跟新信息再上传
        userInfoMapper.updateDetailInfo(userInfo);
        mainAgentService.uploadInfoToMemory(userId);
    }

    @Override
    public PageResult getEmotionPageList(Integer page, Integer size) {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        PageHelper.startPage(page, size);
        Page<UserEmotionRecordPO> list = emotionRecordMapper.getEmotionPageList(userId);
        List<UserEmotionRecordPO> records = list.getResult();
        return new PageResult(list.getTotal(), records);
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
        List<UserEmotionRecordPO> list = emotionRecordMapper.getEmotionChatList(userId, items, startTime, endTime);
        return list;
    }

    @Override
    public void insertEmotionRecord(UserEmotionRecordPO record) {
        userEmotionRecordMapper.insert(record);
    }

    @Transactional
    @Override
    public boolean updatePassword(String oldPassword, String newPassword) {
        try {
            Long userId = Long.valueOf(BaseContext.getCurrentId());
            UserPO userPo = userMapper.selectById(userId);
            if (bCryptPasswordEncoder.matches(oldPassword, userPo.getPassword())) {
                // 更新密码时间
                userPo.setTime(System.currentTimeMillis());
                userPo.setPassword(bCryptPasswordEncoder.encode(newPassword));
                userMapper.update(userPo);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void updateBaseInfo(String introduce, MultipartFile photo) {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        UserPO user = new UserPO();
        user.setId(userId);
        if (introduce != null && !introduce.isEmpty()) {
            user.setIntroduce(introduce);
        }
        if (photo != null && !photo.isEmpty()) {
            String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);
            if (url != null) {
                user.setPhoto(url);
            }
        }
        userMapper.update(user);
        mainAgentService.uploadInfoToMemory(userId);
    }

    @Transactional
    @Override
    public void dropUser() {
        Long userId = Long.valueOf(BaseContext.getCurrentId());
        emotionRecordMapper.deleteAllByUserId(userId);
        userInfoMapper.deleteByUserId(userId);
        diaMapper.deleteByUserId(userId);
        mainDiaMapper.removeAllByUserId(userId);
        userMapper.deleteById(userId);
    }

}
