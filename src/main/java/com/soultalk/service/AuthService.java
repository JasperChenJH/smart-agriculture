package com.soultalk.service;

import com.soultalk.controller.request.R;
import com.soultalk.po.UserPO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    ResponseEntity<?> login(String name, String password);

    ResponseEntity<?> register(String name, String password);

    //传入密码明文，检查
    boolean check(String name, String password);

    R update(UserPO user, MultipartFile photo);
}
