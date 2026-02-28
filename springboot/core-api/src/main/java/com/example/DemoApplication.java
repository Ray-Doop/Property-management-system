package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) // ❗ 禁用SpringBoot自动配置数据源
@MapperScan({
        "com.example.modules.auth.mapper",
        "com.example.modules.business.*.mapper",
        "com.example.modules.business.*.*.mapper",
        "com.example.modules.system.*.mapper",
        "com.example.modules.notice.mapper",
        "com.example.modules.pay.mapper",
        "com.example.modules.fee.mapper",
        "com.example.modules.travelpass.mapper",
        "com.example.modules.forum.mapper",
        "com.example.modules.repair.mapper"
}) // MyBatis Mapper 扫描
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
