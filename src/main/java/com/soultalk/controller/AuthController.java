package com.soultalk.controller;

import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.UserPO;
import com.soultalk.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("name") String name, @RequestParam("password") String password) {
        return authService.login(name, password);
    }

    //注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam("name") String name, @RequestParam("password") String password) {
        return authService.register(name, password);
    }

    @PostMapping("/hello")
    public ResponseEntity<?> hello() {
        return ResponseEntity.ok("hello");
    }

    //更新个人信息
    @PostMapping("/update")
    public R update(@RequestParam("name") String name,
                    @RequestParam("password") String password,
                    @RequestParam("introduce") String introduce,
                    @RequestParam("photo") MultipartFile photo) {

        UserPO user = userMapper.selectByName(name);
        if (authService.check(password, user.getPassword())) {

            if (user.getIntroduce() != null) {
                user.setIntroduce(introduce);
            }

            return authService.update(user, photo);
        } else {
            return R.Success("密码错误");
        }

    }

    @GetMapping("/info")
    public R info(@RequestParam("name") String name, @RequestParam("password") String password) {
        UserPO user = userMapper.selectByName(name);
        user.setPassword(null);
        return R.Success(user);
    }
}
