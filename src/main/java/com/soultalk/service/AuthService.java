package com.soultalk.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> login(String name, String password);

    ResponseEntity<?> register(String name, String password);

    //核对账密
    boolean check(String name, String password);
}
