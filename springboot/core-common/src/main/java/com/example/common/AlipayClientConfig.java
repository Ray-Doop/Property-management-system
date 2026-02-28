// config/AlipayClientConfig.java
package com.example.common;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 创建 AlipayClient Bean（单例复用，线程安全） */
@Configuration
public class AlipayClientConfig {
    @Bean
    public AlipayClient alipayClient(AlipayProperties p) {
        // 电脑网站支付：使用默认 SDK 客户端
        return new DefaultAlipayClient(
                p.getServerUrl(),
                p.getAppId(),
                p.getPrivateKey(),
                "json",
                p.getCharset(),
                p.getAlipayPublicKey(),
                p.getSignType()
        );
    }
}
