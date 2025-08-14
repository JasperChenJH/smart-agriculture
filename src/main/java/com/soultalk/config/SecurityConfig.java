package com.soultalk.config;

import com.soultalk.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    //哪些路径完全绕过 Spring Security 认证体系
    private final String[] EXCLUDE_URLS = new String[]{
            "/auth/login",
            "/auth/register",
            "/auth/resetPassword",
            "/dia/streamQuestion",
            "/main/ask"
    };

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    //注入加密方式
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // HTTP 安全过滤器链
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. 禁用 CSRF 防护
        http.csrf(AbstractHttpConfigurer::disable)
                // 2. 启用 CORS 跨域支持
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 配置会话管理策略
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 4. 设置访问控制规则
                .authorizeHttpRequests(auth -> auth
                        // 允许OPTIONS预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // 0.开放websocket
                        .requestMatchers("/ws/**")
                        .permitAll()

                        // 1.开放静态资源路径
                        .requestMatchers("/resources/**")
                        .permitAll()

                        // 2.开放白名单路径
                        .requestMatchers(EXCLUDE_URLS)
                        .permitAll()

                        // 保护其他所有路径
                        .anyRequest().authenticated()
                )
                // 5. 注入 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ============= CORS跨域配置 =============
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 关键修改：使用 allowedOriginPatterns 代替 allowedOrigins
        config.setAllowedOriginPatterns(List.of("*"));

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));// 允许所有域名
        config.setAllowCredentials(true); // 允许凭据（如需要）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
