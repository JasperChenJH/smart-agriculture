package com.soultalk.controller;

import com.soultalk.context.BaseContext;
import com.soultalk.controller.request.R;
import com.soultalk.mapper.UserMapper;
import com.soultalk.po.UserPO;
import com.soultalk.service.AuthService;
import com.soultalk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户基本信息表
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;

    @PostMapping("/hello")
    public ResponseEntity<?> hello() {
        System.out.println(BaseContext.getCurrentId());
        return ResponseEntity.ok("hello");
    }

    @GetMapping("/info")
    public R info(@RequestParam("name") String name, @RequestParam("password") String password) {
        return userService.info(name);
    }

    @PostMapping("/update")
    public R update(@RequestParam("name") String name,
                    @RequestParam("password") String password,
                    @RequestParam("introduce") String introduce,
                    @RequestParam("photo") MultipartFile photo) {

        if (authService.check(password, password)) {
            UserPO user = userMapper.selectByName(name);
            return userService.update(user, introduce, photo);
        } else {
            return R.Success("密码错误");
        }
    }
}
