package com.soultalk.service.impl;

import com.soultalk.config.Configs;
import com.soultalk.controller.request.JwtResponse;
import com.soultalk.mapper.AdminMapper;
import com.soultalk.po.AdminPO;
import com.soultalk.po.UserInfoPO;
import com.soultalk.po.UserPO;
import com.soultalk.service.AdminService;
import com.soultalk.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.io.SAXReader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {
    @Resource
    private AdminMapper adminMapper;
    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Resource
    private com.soultalk.mapper.UserMapper userMapper;

    @Override
    public ResponseEntity<?> login(String nickname, String password) {
        AdminPO user = adminMapper.selectByNickname(nickname);
        if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
            String token = JwtUtils.generateToken(Long.valueOf(user.getAdminId()));
            return ResponseEntity.ok(new JwtResponse(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或者密码错误");
    }

    @Override
    public AdminPO getById(Integer adminId) {
        AdminPO admin = adminMapper.selectById(Long.valueOf(adminId));
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }

    @Override
    public AdminPO getByNickname(String nickname) {
        AdminPO admin = adminMapper.selectByNickname(nickname);
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }

    @Transactional
    @Override
    public ResponseEntity<?> register(String name, String password) {
        // 检查用户名重复
        if (adminMapper.selectByNickname(name)!= null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("用户名已存在");
        }
        // 加密密码并保存
        AdminPO user = new AdminPO();
        user.setNickname(name);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        adminMapper.insert(user);
        return ResponseEntity.ok("注册成功");
    }

    @Override
    public void update(AdminPO admin) {
        String psw = adminMapper.selectById(Long.valueOf(admin.getAdminId())).getPassword();
        admin.setPassword(psw);
        adminMapper.update(admin);
    }

    @Override
    public void deleteById(Integer adminId) {
        adminMapper.deleteById(adminId);
    }

    @Override
    public void resetPassword(Long userId) {
        AdminPO user = adminMapper.selectById(userId);
        String newPassword = "a12345";
        //重设时间
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        adminMapper.update(user);
    }

    @Override
    public java.util.List<UserPO> getAllUserStatus() {
        return userMapper.selectAll();
    }

}