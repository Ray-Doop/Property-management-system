//package com.example.common;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 匹配所有接口
//                .allowedOrigins("*") // 允许所有前端源（生产环境建议指定具体域名，如 "http://localhost:8080"）
//                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
//                .allowedHeaders("*") // 允许所有请求头
//                .exposedHeaders("*") // 允许前端获取的响应头
//                .allowCredentials(true); // 允许携带 Cookie（如需要）
//    }
//}