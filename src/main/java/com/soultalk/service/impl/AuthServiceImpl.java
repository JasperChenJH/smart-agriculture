package com.soultalk.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.soultalk.aigc.MainAgent;
import com.soultalk.config.Configs;
import com.soultalk.controller.request.JwtResponse;
import com.soultalk.mapper.UserEmotionRecordMapper;
import com.soultalk.mapper.UserInfoMapper;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.UserEmotionRecordPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import com.soultalk.po.WeChatLoginPO;
import com.soultalk.properties.WeChatProperties;
import com.soultalk.service.AuthService;
import com.soultalk.utils.HttpClientUtil;
import com.soultalk.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private MainAgent mainAgent;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserEmotionRecordMapper userEmotionRecordMapper;
    //  微信登录
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public ResponseEntity<?> login(String name, String password) {
        UserPO user = userMapper.selectByName(name);
        if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
            String token = JwtUtils.generateToken(user.getId());
            return ResponseEntity.ok(new JwtResponse(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或者密码错误");
    }

    @Transactional
    @Override
    public ResponseEntity<?> register(String name, String password) {
        // 检查用户名重复
        if (userMapper.countByName(name) > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已存在");
        }

        // 验证密码规则
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";
        if (!password.matches(passwordPattern)) {
            return ResponseEntity.badRequest().body("密码至少6位 并 包含字母和数字");
        }

        // 加密密码并保存
        UserPO user = new UserPO();
        user.setName(name);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setTime(System.currentTimeMillis());
        userMapper.insert(user);

        //创建memory_id
        try {
            String memoryId = mainAgent.createMemoryId(Configs.ALI_WORKSPACE_ID, "用户:" + user.getId());
            userMapper.setMemoryIdToId(user.getId(), memoryId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        //注册之后添加用户详细详细表
        UserInfoPO userInfo = new UserInfoPO();
        userInfo.setUserId(user.getId());
        userInfoMapper.insertDetailInfo(userInfo);

        // 添加用户情感表
        UserEmotionRecordPO userEmotionRecord = new UserEmotionRecordPO();
        userEmotionRecord.setUserId(user.getId());
        userEmotionRecord.setStatus(0);
        userEmotionRecord.setEmotion("无效情绪");
        userEmotionRecord.setScore(BigDecimal.valueOf(0.0f));
        userEmotionRecordMapper.insert(userEmotionRecord);
        return ResponseEntity.ok("注册成功");
    }

    @Override
    public boolean check(String name, String password) {
        try {
            UserPO user = userMapper.selectByName(name);
            if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    @Override
    public boolean check(Long userId, String password) {
        try {
            UserPO user = userMapper.selectById(userId);
            if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        UserPO user = userMapper.selectById(userId);

        //如果新密码为空，则重置为a12345
        if (newPassword.isEmpty()) {
            newPassword = "a12345";
        }
        //重设时间
        user.setTime(System.currentTimeMillis());
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));

        userMapper.update(user);
    }

    @Override
    public WeChatLoginPO wechatLogin(String code) {
        // 获得当前用户的openId 通过 HttpClient向微信发送请求
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",WeChatProperties.APP_ID);
        map.put("secret",WeChatProperties.SECRET);
        map.put("js_code",code);
        //"authorization_code"固定值
        map.put("grant_type","authorization_code");
        // 返回值是json格式数据
        /*
            {
            "openid":"xxxxxx",
            "session_key":"xxxxx",
            "unionid":"xxxxx",
            "errcode":0,
            "errmsg":"xxxxx"
            }
         */
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        String sessionKey = jsonObject.getString("session_key");
        // 判断当前openid 是否为空如果为空,抛出异常
        if(openid == null){
            throw new RuntimeException("微信登录异常未取到openid");
        }
        WeChatLoginPO weChatLoginPO = new WeChatLoginPO();
        weChatLoginPO.setOpenId(openid);
        weChatLoginPO.setSessionKey(sessionKey);
        //判断是否为新用户
        UserPO user = userMapper.getByOpenId(openid);
        UserPO newUser = new UserPO();
        if (user==null){
            newUser.setOpenId(openid);
            newUser.setTime(System.currentTimeMillis());
            userMapper.insert(newUser);
            String token = JwtUtils.generateToken(newUser.getId());
            weChatLoginPO.setToken(token);
            return weChatLoginPO;
        }
        String token = JwtUtils.generateToken(user.getId());
        weChatLoginPO.setToken(token);
        return weChatLoginPO;
    }
}


