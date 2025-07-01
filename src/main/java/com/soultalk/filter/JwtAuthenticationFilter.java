package com.soultalk.filter;


import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.soultalk.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 路径跳过 JWT 验证
    private static final List<String> JWT_EXCLUDED = Arrays.asList(
            "/auth/login",
            "/auth/register"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 处理预检请求 OPTIONS
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String requestURI = request.getRequestURI();

        // WebSocket 请求直接放行
        if (requestURI.startsWith("/ws/") || "/ws".equals(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 白名单路径直接放行
        if (JWT_EXCLUDED.contains(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头获取 JWT
        String token = getTokenFromRequest(request);

        try {
            if (token != null && !JwtUtils.isTokenExpired(token)) {
                // 验证并解析 JWT
                DecodedJWT decodedJWT = JwtUtils.verifyToken(token);
                String username = decodedJWT.getSubject();

                // 创建认证对象
                UserDetails userDetails = new User(username, "", new ArrayList<>());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 添加用户名到请求属性
                request.setAttribute("username", username);

                // 放行请求
                filterChain.doFilter(request, response);
                return;
            }
        } catch (JWTVerificationException e) {
            log.info("JWT 验证失败");
            log.error(e.getMessage());
        }

        // 认证失败处理401
        response.setCharacterEncoding("utf-8");//中文
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("未知或过期JWT令牌");
    }

    // 从 Authorization 头提取令牌
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null)return bearerToken;
        else return null;
    }
}