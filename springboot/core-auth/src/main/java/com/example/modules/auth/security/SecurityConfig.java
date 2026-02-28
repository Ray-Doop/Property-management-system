package com.example.modules.auth.security;

import com.example.modules.auth.security.JWTAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 放行 OPTIONS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行登录和公共接口
                        .requestMatchers(
                                "/LoginRegister/userLogin",
                                "/LoginRegister/adminlogin",
                                "/LoginRegister/employeeLogin",
                                "/LoginRegister/userRegister",
                                "/LoginRegister/captcha",
                                "/LoginRegister/wechatLogin",
                                "/api/pay/page/alipay",
                                "/api/pay/return/alipay",
                                "/api/pay/notify/alipay",
                                "/files/download/**",
                                "/files/**"

                        ).permitAll()
                        // 管理员专用接口
                        .requestMatchers(
                                "/employee/add",
                                "/employee/del/**",
                                "/employee/delBatch",
                                "/employee/selectAll",
                                "/employee/selectPage"
                        ).hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 员工个人资料接口
                        .requestMatchers(
                                "/employee/updateAvatar",
                                "/employee/updata",
                                "/employee/checkOldPassword",
                                "/employee/updatePassword",
                                "/employee/selectById/**"
                        ).hasAnyRole("EMPLOYEE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/travel-pass/selectPage").hasRole("ADMIN")
                        .requestMatchers("/Forum/SelectAllComment", "/Forum/deleteComment/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/LoginRegister/selectAllUser",
                                "/LoginRegister/approval",
                                "/LoginRegister/pass",
                                "/LoginRegister/refuse",
                                "/LoginRegister/mute",
                                "/LoginRegister/ban"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/fee/allBills").hasRole("ADMIN")
                        // 维修权限 - 普通用户可访问的维修接口
                        .requestMatchers(
                                "/repair/myRepair",
                                "/repair/categories",
                                "/repair/submit",
                                "/repair/detail/**",
                                "/repair/cancel/**",
                                "/repair/evaluate"
                        ).hasAnyRole("USER", "ADMIN", "REPAIRER")
                        // 维修权限 - 管理员和维修人员可访问的接口
                        .requestMatchers(
                                "/repair/allRepair",
                                "/repair/status",
                                "/repair/findWorkers",
                                "/repair/dispatchOrder",
                                "/repair/complete",
                                "/repair/accept",
                                "/repair/byWorker",
                                "/repair/evaluation/**"
                        ).hasAnyRole("REPAIRER", "ADMIN", "EMPLOYEE")
                        // 门卫权限（核销）
                        .requestMatchers("/travel-pass/verify").hasAnyRole("GUARD", "ADMIN", "EMPLOYEE")
                        // 普通用户接口
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        // 需要登录即可访问（管理员也可访问）
                        .requestMatchers("/travel-pass/**").authenticated()
                        .requestMatchers("/Forum/**").authenticated()
                        .requestMatchers("/api/fee/**").authenticated()
                        // 其他接口必须登录
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
