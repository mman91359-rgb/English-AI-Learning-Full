package com.english.ai.exercise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.english.ai")
@MapperScan("com.english.ai.exercise.mapper")
@EnableScheduling
public class ExerciseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExerciseApplication.class, args);
        System.out.println("========================================");
        System.out.println("  Exercise Service 启动成功!");
        System.out.println("  端口: 8082");
        System.out.println("========================================");
    }
}
