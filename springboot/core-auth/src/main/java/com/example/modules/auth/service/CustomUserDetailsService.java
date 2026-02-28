package com.example.modules.auth.service;

import com.example.entity.Admin;
import com.example.entity.User;
import com.example.modules.auth.mapper.LoginRegisterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final LoginRegisterMapper loginRegisterMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public CustomUserDetailsService(LoginRegisterMapper loginRegisterMapper) {
        this.loginRegisterMapper = loginRegisterMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ====== 1. 构造缓存Key ======
        String adminCacheKey = "userdetails:admin:" + username;
        String userCacheKey = "userdetails:user:" + username;
        String adminLockKey = "lock:" + adminCacheKey;
        String userLockKey = "lock:" + userCacheKey;

        // ====== 2. 先查管理员缓存 ======
        Object adminCache = redisTemplate.opsForValue().get(adminCacheKey);
        if (adminCache != null) {
            // 空对象表示不存在
            if (adminCache instanceof Admin && ((Admin) adminCache).getUsername() != null) {
                Admin admin = (Admin) adminCache;
                return new org.springframework.security.core.userdetails.User(
                        admin.getUsername(),
                        admin.getPassword(),
                        true, true, true, true,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            }
            // 如果是空对象标记，继续查用户
        } else {
            // ====== 3. 未命中缓存，尝试加锁查询管理员 ======
            Boolean adminLockAcquired = redisTemplate.opsForValue().setIfAbsent(adminLockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(adminLockAcquired)) {
                try {
                    Admin admin = loginRegisterMapper.selectByAdminname(username);
                    if (admin != null) {
                        // 缓存管理员信息，30分钟过期
                        redisTemplate.opsForValue().set(adminCacheKey, admin, 30, TimeUnit.MINUTES);
                        return new org.springframework.security.core.userdetails.User(
                                admin.getUsername(),
                                admin.getPassword(),
                                true, true, true, true,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                    } else {
                        // 缓存空对象，5分钟过期
                        redisTemplate.opsForValue().set(adminCacheKey, new Admin(), 5, TimeUnit.MINUTES);
                    }
                } finally {
                    redisTemplate.delete(adminLockKey);
                }
            } else {
                // 未拿到锁，等待后重试
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                    adminCache = redisTemplate.opsForValue().get(adminCacheKey);
                    if (adminCache != null && adminCache instanceof Admin && ((Admin) adminCache).getUsername() != null) {
                        Admin admin = (Admin) adminCache;
                        return new org.springframework.security.core.userdetails.User(
                                admin.getUsername(),
                                admin.getPassword(),
                                true, true, true, true,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }

        // ====== 4. 查普通用户缓存 ======
        Object userCache = redisTemplate.opsForValue().get(userCacheKey);
        if (userCache != null) {
            if (userCache instanceof User && ((User) userCache).getUsername() != null) {
                User user = (User) userCache;
                return new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        true, true, true, true,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }
            // 空对象，用户不存在
            throw new UsernameNotFoundException("用户不存在");
        }

        // ====== 5. 未命中缓存，尝试加锁查询用户 ======
        Boolean userLockAcquired = redisTemplate.opsForValue().setIfAbsent(userLockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(userLockAcquired)) {
            try {
                User user = loginRegisterMapper.selectByUsername(username);
                if (user != null) {
                    // 缓存用户信息，30分钟过期
                    redisTemplate.opsForValue().set(userCacheKey, user, 30, TimeUnit.MINUTES);
                    return new org.springframework.security.core.userdetails.User(
                            user.getUsername(),
                            user.getPassword(),
                            true, true, true, true,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                } else {
                    // 缓存空对象，5分钟过期
                    redisTemplate.opsForValue().set(userCacheKey, new User(), 5, TimeUnit.MINUTES);
                    throw new UsernameNotFoundException("用户不存在");
                }
            } finally {
                redisTemplate.delete(userLockKey);
            }
        } else {
            // 未拿到锁，等待后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                userCache = redisTemplate.opsForValue().get(userCacheKey);
                if (userCache != null) {
                    if (userCache instanceof User && ((User) userCache).getUsername() != null) {
                        User user = (User) userCache;
                        return new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getPassword(),
                                true, true, true, true,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    } else {
                        throw new UsernameNotFoundException("用户不存在");
                    }
                }
            } catch (InterruptedException ignored) {
            }
            // 兜底查询
            User user = loginRegisterMapper.selectByUsername(username);
            if (user != null) {
                return new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        true, true, true, true,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            }
            throw new UsernameNotFoundException("用户不存在");
        }
    }
}
