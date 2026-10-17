package com.english.ai.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置 — 静态资源映射
 * 将 /user/avatar/** 请求映射到磁盘头像文件目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 头像文件存储目录，与 UserController 中一致 */
    @Value("${avatar.upload-dir:./uploads/avatars}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /user/avatar/xxx.jpg 映射到 avatarUploadDir 目录下的文件
        String absolutePath = Paths.get(avatarUploadDir).toAbsolutePath().normalize().toString();
        // 必须以 file: 协议前缀 + 目录结尾的 / 才能正确映射
        registry.addResourceHandler("/user/avatar/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
