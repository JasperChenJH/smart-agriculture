package com.soultalk.controller;

import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


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

}
