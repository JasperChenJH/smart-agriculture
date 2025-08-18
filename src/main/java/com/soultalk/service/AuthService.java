package com.soultalk.service;

import com.soultalk.po.UserPO;
import com.soultalk.po.WeChatLoginPO;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> login(String name, String password);

    ResponseEntity<?> register(String name, String password);

    //核对账密
    boolean check(String name, String password);

    boolean check(Long userId, String password);

    //重设密码，传空则重置为 a12345
    void resetPassword(Long userId, String newPassword);

    WeChatLoginPO wechatLogin(String code);
}
