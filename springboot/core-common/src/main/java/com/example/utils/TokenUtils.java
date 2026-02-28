package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;

public class TokenUtils {

    private static final String SECRET_ENV = "JWT_SECRET";
    private static final String SECRET_PROP = "jwt.secret";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24; // 24 小时

    /**
     * 生成 Token（旧版，只存用户名和角色）
     */
    public static String createToken(String username, String role) {
        return JWT.create()
                .withAudience(username) // 存用户名
                .withClaim("role", role)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(resolveSecret()));
    }

    /**
     * ✅ 生成 Token（新版，存 ID、用户名、角色）
     * @param username 用户名
     * @param id 用户或管理员 ID（存 Redis Key）
     * @param role 用户角色
     */
    public static String createToken(String username, String id, String role) {
        return JWT.create()
                .withAudience(id)         // 把用户/管理员 ID 存进去
                .withClaim("username", username)
                .withClaim("role", role)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(resolveSecret()));
    }

    public static String getSecret() {
        return resolveSecret();
    }

    // 通过环境变量或 JVM 参数配置密钥，避免在代码中硬编码
    private static String resolveSecret() {
        String env = System.getenv(SECRET_ENV);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty(SECRET_ENV);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        prop = System.getProperty(SECRET_PROP);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        throw new IllegalStateException("JWT_SECRET is not configured");
    }
}
