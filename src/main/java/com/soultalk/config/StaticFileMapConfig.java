package com.soultalk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态文件映射
 */
@Configuration
@Slf4j
public class StaticFileMapConfig implements WebMvcConfigurer {
    //静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //把本地静态资源映射到项目
        try {
            registry.addResourceHandler("/resources/**")
                    .addResourceLocations("classpath:/");
        } catch (Exception e) {
            log.error(e.getMessage());
            System.out.print("错误映射:" + e);
        }
    }
}
