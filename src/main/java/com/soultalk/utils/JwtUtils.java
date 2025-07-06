package com.soultalk.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

import static com.soultalk.config.Configs.JWT_SECRET_KEY;

public class JwtUtils {
    // 生成JWT
    public static String generateToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId)) // 设置JWT的主题
                .withIssuedAt(new Date()) // 设置签发时间
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 设置过期时间，7天后过期
                .sign(Algorithm.HMAC512(JWT_SECRET_KEY)); // 使用HMAC算法进行签名
    }

    // 解析JWT并验证
    public static DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC512(JWT_SECRET_KEY))
                .build(); // 创建验证器
        return verifier.verify(token); // 验证并返回解码后的JWT对象
    }

    // 获取用户ID
    public static Long getUserIdFromToken(String token) {
        String s = verifyToken(token).getSubject();
        return Long.parseLong(s);
    }

    // 验证JWT是否有效（未过期）
    public static boolean isTokenExpired(String token) {
        return verifyToken(token).getExpiresAt().before(new Date());
    }
}