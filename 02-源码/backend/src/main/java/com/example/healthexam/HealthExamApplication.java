package com.example.healthexam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 体检预约与报告领取管理系统 启动类
 * 技术栈：Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8 + Spring Security + JWT / JDK 17
 */
@SpringBootApplication
@MapperScan("com.example.healthexam.mapper")
public class HealthExamApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthExamApplication.class, args);
    }
}
