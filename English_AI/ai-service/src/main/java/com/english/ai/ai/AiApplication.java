package com.english.ai.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.english.ai")
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
        System.out.println("========================================");
        System.out.println("  AI Service 启动成功!");
        System.out.println("  端口: 8084");
        System.out.println("========================================");
    }
}
