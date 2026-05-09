package com.soultalk.service;

import com.soultalk.controller.request.JwtResponse;
import com.soultalk.po.AdminPO;
import com.soultalk.po.UserPO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminService {

    ResponseEntity<?> login(String name, String password);

    AdminPO getById(Integer adminId);

    AdminPO getByNickname(String nickname);

    ResponseEntity<?> register(String name, String password);

    void update(AdminPO admin);

    void deleteById(Integer adminId);

    void resetPassword(Long userId);

    List<UserPO> getAllUserStatus();

}