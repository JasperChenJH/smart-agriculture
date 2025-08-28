package com.soultalk.controller;

import com.soultalk.controller.request.R;
import com.soultalk.po.WeChatLoginPO;
import com.soultalk.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 登录注册与重置
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    /**
     * 登录
     *
     * @param name
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("name") String name, @RequestParam("password") String password) {
        return authService.login(name, password);

    }

    /**
     * 微信登录
     *
     * @param code 微信登录返回的code
     * @return
     */
    @GetMapping("/wechat/login")
    public WeChatLoginPO wechatLogin(@RequestParam("code") String code) {
        WeChatLoginPO weChatLoginPO = authService.wechatLogin(code);
        return weChatLoginPO;
    }

    /**
     * 注册
     *
     * @param name
     * @param password
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam("name") String name, @RequestParam("password") String password) {
        return authService.register(name, password);
    }


    /**
     * 重置密码
     *
     * @param id
     * @param oldPassword
     * @param newPassword
     * @return
     */
    @PostMapping("/resetPassword")
    public R resetPassword(@RequestParam("id") String id, @RequestParam("oldpwd") String oldPassword, @RequestParam("newpwd") String newPassword) {
        try {
            Long userId = Long.parseLong(id);
            //检查密码
            if (authService.check(userId, oldPassword)) {
                authService.resetPassword(userId, newPassword);//修改密码

                if (newPassword.isEmpty()) {
                    return R.Success("密码已重置为 a12345");
                } else {
                    return R.Success("密码修改成功");
                }
            }
            return R.Success("修改失败，密码错误");
        } catch (Exception e) {
            log.error(e.getMessage());
            return R.Failed(500, "修改失败");
        }
    }
}
