package com.soultalk.service.impl;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.JwtResponse;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.UserPO;
import com.soultalk.service.AuthService;
import com.soultalk.service.BaseService;
import com.soultalk.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private BaseService baseService;

    @Override
    public ResponseEntity<?> login(String name, String password) {
        UserPO user = userMapper.selectByNameAndPassword(name, password);
        if (user!=null) {
            String token = JwtUtils.generateToken(name);
            BaseContext.setCurrentId(user.getId());
            return ResponseEntity.ok(new JwtResponse(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或者密码错误");
    }

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
        userMapper.insert(user);

        return ResponseEntity.ok("注册成功");
    }

    @Override
    public boolean check(String name, String password) {
        UserPO user = userMapper.selectByNameAndPassword(name, password);
        return user != null;
    }

    @Override
    public R update(UserPO user, MultipartFile photo) {
        String url = baseService.saveFileToOSS(photo.getOriginalFilename(), photo);

        if (url != null) {
            user.setPhoto(url);
        }

        userMapper.update(user);
        return R.Success(user.getName() + " 修改成功");
    }
}


