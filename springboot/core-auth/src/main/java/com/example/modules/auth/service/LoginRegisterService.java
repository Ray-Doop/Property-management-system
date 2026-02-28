package com.example.modules.auth.service;

import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.modules.auth.mapper.LoginRegisterMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.common.RedisConstants;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.common.RedisConstants.TOKEN_EXPIRE_SECONDS;

@Service
public class LoginRegisterService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LoginRegisterMapper loginRegisterMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("dataProcessExecutor")
    private Executor dataProcessExecutor;

//    private static final long TOKEN_EXPIRE_SECONDS = 60 * 60 * 8; // 8小时

    // 普通用户登录
    public User login(User loginUser) {
        String username = loginUser.getUsername();
        String password = loginUser.getPassword();

        User dbUser = loginRegisterMapper.selectByUsername(username);
        if (dbUser == null) {
            throw new CustomException("500", "账号不存在");
        }
        validateUserLoginStatus(dbUser);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException e) {
            throw new CustomException("500", "密码错误");
        } catch (Exception e) {
            throw new CustomException("500", "密码错误");
        }

        String token = TokenUtils.createToken(dbUser.getUsername(), String.valueOf(dbUser.getUserId()),"ROLE_USER");
        dbUser.setToken(token);
        dbUser.setCodeId(formatCode("001", dbUser.getUserId()));

        String redisKey = "login:user:" + dbUser.getUserId();
        redisTemplate.opsForValue().set(redisKey, dbUser, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return dbUser;
    }
    public User getUserById(Long userId) {
        return (User) redisTemplate.opsForValue().get("login:user:" + userId);
    }

    // 根据 ID 获取管理员
    public Admin getAdminById(Long adminId) {
        // 先查缓存
        String cacheKey = "login:admin:" + adminId;
        Admin admin = (Admin) redisTemplate.opsForValue().get(cacheKey);
        if (admin != null) {
            return admin;
        }
        // 查数据库
        admin = loginRegisterMapper.selectAdminById(adminId);
        if (admin != null) {
            redisTemplate.opsForValue().set(cacheKey, admin, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        return admin;
    }

    // 登出
    public void logout(String role, Long id) {
        String key = role.equals("ROLE_ADMIN") ? "login:admin:" + id : "login:user:" + id;
        redisTemplate.delete(key);
    }

    // 微信一键登录
    public User loginByOpenid(String openid) {
        // 使用 username 字段存储/查询 openid
        User dbUser = loginRegisterMapper.selectByUsername(openid);
        if (dbUser == null) {
            return null; // 用户不存在，需要注册
        }
        
        validateUserLoginStatus(dbUser);
        
        // 生成 Token 并返回
        String token = TokenUtils.createToken(dbUser.getUsername(), String.valueOf(dbUser.getUserId()),"ROLE_USER");
        dbUser.setToken(token);
        
        // 存入 Redis
        String redisKey = "login:user:" + dbUser.getUserId();
        redisTemplate.opsForValue().set(redisKey, dbUser, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        return dbUser;
    }

    private void validateUserLoginStatus(User user) {
        if (user == null || user.getStatus() == null) {
            return;
        }
        Integer status = user.getStatus();
        if (status == 0) {
            throw new CustomException("403", "账号未激活，请等待管理员审核");
        }
        if (status == 3) {
            throw new CustomException("403", "账号已被封禁，请联系管理员");
        }
        if (status == 4) {
            throw new CustomException("403", "账号审核未通过，请联系管理员");
        }
    }

    public void register(User user) {
        String username = user.getUsername();
        User dbUser=loginRegisterMapper.selectByUsername(username);
        if (dbUser != null) {
            throw new CustomException("500", "账号已存在");
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            User phoneUser = loginRegisterMapper.selectByPhone(user.getPhone());
            if (phoneUser != null) {
                throw new CustomException("500", "手机号已注册");
            }
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        int rows;
        try {
            rows = loginRegisterMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new CustomException("500", "手机号已注册");
        }
        if (rows == 0) {
            throw new CustomException("500", "注册失败（数据库保存异常）");
        }
        // 注册成功后，清除相关缓存（如果之前有空对象缓存）
        if (username != null) {
            redisTemplate.delete("user:username:" + username);
            redisTemplate.delete("userdetails:user:" + username);
        }
    }


    public User getEmployeeById(String userName) {
        return getUserByUsername(userName);
    }

    /**
     * 按用户名获取用户信息，供其他模块安全复用，避免直接访问 Mapper
     */
    public User getUserByUsername(String username) {
        String cacheKey = "user:username:" + username;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof User && ((User) cache).getUsername() != null) {
            return (User) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                User user = loginRegisterMapper.selectByUsername(username);
                if (user != null) {
                    redisTemplate.opsForValue().set(cacheKey, user, 15, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new User(), 2, TimeUnit.MINUTES);
                }
                return user;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof User && ((User) cache).getUsername() != null) {
                    return (User) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return loginRegisterMapper.selectByUsername(username);
        }
    }

    /**
     * 提供给业务模块的用户列表入口，统一由认证模块负责用户数据访问
     */
    public List<User> selectAllValidUsers() {
        return loginRegisterMapper.selectAllValidUsers();
    }

    /**
     * 获取或创建员工镜像用户，用于跨模块作者身份映射
     */
    public User getOrCreateShadowUserForEmployee(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            return null;
        }
        String username = "emp_" + employee.getEmployeeId();
        User existed = getUserByUsername(username);
        if (existed != null && existed.getUserId() != null) {
            return existed;
        }
        User shadow = new User();
        shadow.setUsername(username);
        shadow.setPassword("");
        shadow.setNickname(employee.getName() != null ? employee.getName() : username);
        shadow.setAvatarUrl(employee.getAvatarUrl());
        shadow.setPhone("");
        shadow.setRemark("员工镜像用户");
        loginRegisterMapper.insert(shadow);
        redisTemplate.delete("user:username:" + username);
        return getUserByUsername(username);
    }

    /**
     * 获取或创建管理员镜像用户，用于跨模块作者身份映射
     */
    public User getOrCreateShadowUserForAdmin(Admin admin) {
        if (admin == null || admin.getAdminId() == null) {
            return null;
        }
        String username = "admin_" + admin.getAdminId();
        User existed = getUserByUsername(username);
        if (existed != null && existed.getUserId() != null) {
            return existed;
        }
        User shadow = new User();
        shadow.setUsername(username);
        shadow.setPassword("");
        shadow.setNickname(admin.getUsername());
        shadow.setAvatarUrl(null);
        shadow.setPhone(admin.getPhone() != null ? admin.getPhone() : "");
        shadow.setRemark("管理员镜像用户");
        loginRegisterMapper.insert(shadow);
        redisTemplate.delete("user:username:" + username);
        return getUserByUsername(username);
    }

    public User findByUsername(String username) {
        String cacheKey = "user:userid:" + username;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof User && ((User) cache).getUsername() != null) {
            return (User) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                User db = loginRegisterMapper.selectByUserid(username);
                if (db != null) {
                    redisTemplate.opsForValue().set(cacheKey, db, 15, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new User(), 2, TimeUnit.MINUTES);
                }
                return db;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof User && ((User) cache).getUsername() != null) {
                    return (User) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return loginRegisterMapper.selectByUserid(username);
        }
    }

    public User selectUserData(Long userId) {
        String cacheKey = "user:data:" + userId;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof User && ((User) cache).getUserId() != null) {
            return (User) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                User user = loginRegisterMapper.selectUserData(userId);
                if (user != null) {
                    redisTemplate.opsForValue().set(cacheKey, user, 20, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new User(), 2, TimeUnit.MINUTES);
                }
                return user;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof User && ((User) cache).getUserId() != null) {
                    return (User) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return loginRegisterMapper.selectUserData(userId);
        }
    }

    public User getUserData(Long userId) {
        // 复用selectUserData的缓存逻辑
        return selectUserData(userId);
    }

    public void uplastlogintime(Long userId) {
        loginRegisterMapper.uplastlogintime(userId);
        if (userId != null) {
            redisTemplate.delete("user:data:" + userId);
            refreshLoginUserCache(userId, null);
        }
    }

    public void UpdatePassword(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println("===========+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(user);
        System.out.println(encodedPassword);
        user.setPassword(encodedPassword);
        System.out.println(user.getPassword());
        System.out.println("===========+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        loginRegisterMapper.UpdatePassword(user);
        // 清除相关缓存
        if (user.getUserId() != null) {
            redisTemplate.delete("user:data:" + user.getUserId());
            refreshLoginUserCache(user.getUserId(), null);
        }
        if (user.getUsername() != null) {
            redisTemplate.delete("user:username:" + user.getUsername());
            redisTemplate.delete("userdetails:user:" + user.getUsername());
        }
    }
    
    // 管理员修改密码
    public void updateAdminPassword(Admin admin) {
        String encodedPassword = passwordEncoder.encode(admin.getPassword());
        admin.setPassword(encodedPassword);
        loginRegisterMapper.updateAdminPassword(admin);
        // 清除相关缓存
        if (admin.getAdminId() != null) {
            redisTemplate.delete("login:admin:" + admin.getAdminId());
        }
        if (admin.getUsername() != null) {
            redisTemplate.delete("admin:username:" + admin.getUsername());
            redisTemplate.delete("userdetails:admin:" + admin.getUsername());
        }
    }

    public void updateAvatar(Long userId, String avatarUrl) {
        loginRegisterMapper.updateAvatar(userId,avatarUrl);
        redisTemplate.delete("user:data:" + userId);
        User updatedUser = loginRegisterMapper.selectUserData(userId);
        if (updatedUser != null && updatedUser.getUsername() != null) {
            redisTemplate.delete("user:username:" + updatedUser.getUsername());
            redisTemplate.delete("userdetails:user:" + updatedUser.getUsername());
        }
        refreshLoginUserCache(userId, updatedUser);
    }
    
    // 管理员修改头像
    public void updateAdminAvatar(Long adminId, String avatarUrl) {
        loginRegisterMapper.updateAdminAvatar(adminId, avatarUrl);
        // 清除管理员缓存
        redisTemplate.delete("login:admin:" + adminId);
    }

    public void UpdateUserData(User user) {
        loginRegisterMapper.UpdateUserData(user);
        User updatedUser = null;
        if (user.getUserId() != null) {
            redisTemplate.delete("user:data:" + user.getUserId());
            updatedUser = loginRegisterMapper.selectUserData(user.getUserId());
            refreshLoginUserCache(user.getUserId(), updatedUser);
        }
        if (updatedUser != null && updatedUser.getUsername() != null) {
            redisTemplate.delete("user:username:" + updatedUser.getUsername());
            redisTemplate.delete("userdetails:user:" + updatedUser.getUsername());
        }
    }
    
    // 管理员修改资料
    public void updateAdminData(Admin admin) {
        loginRegisterMapper.updateAdminData(admin);
        // 清除相关缓存
        if (admin.getAdminId() != null) {
            redisTemplate.delete("login:admin:" + admin.getAdminId());
        }
        if (admin.getUsername() != null) {
            redisTemplate.delete("admin:username:" + admin.getUsername());
        }
    }

    private User refreshLoginUserCache(Long userId, User updatedUser) {
        if (userId == null) {
            return null;
        }
        String cacheKey = "login:user:" + userId;
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (!(cache instanceof User) || ((User) cache).getUserId() == null) {
            return null;
        }
        User cachedUser = (User) cache;
        User user = updatedUser != null ? updatedUser : loginRegisterMapper.selectUserData(userId);
        if (user == null) {
            return null;
        }
        user.setToken(cachedUser.getToken());
        user.setCodeId(cachedUser.getCodeId());
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        long expire = (ttl == null || ttl <= 0) ? TOKEN_EXPIRE_SECONDS : ttl;
        redisTemplate.opsForValue().set(cacheKey, user, expire, TimeUnit.SECONDS);
        return user;
    }

    public Admin adminLogin(Admin loginAdmin) {
        String username = loginAdmin.getUsername();
        String password = loginAdmin.getPassword();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            Admin dbAdmin = loginRegisterMapper.selectAdminByUsername(username);
            if (dbAdmin == null) {
                throw new CustomException("500", "管理员不存在");
            }

            String token = TokenUtils.createToken(dbAdmin.getUsername(), String.valueOf(dbAdmin.getAdminId()),"ROLE_ADMIN");

            dbAdmin.setToken(token);
            dbAdmin.setCodeId(formatCode("002", dbAdmin.getAdminId()));

            // Redis 存储 Key: login:admin:{adminId}
            String redisKey = "login:admin:" + dbAdmin.getAdminId();
            redisTemplate.opsForValue().set(redisKey, dbAdmin, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

            return dbAdmin;

        } catch (Exception e) {
            throw new CustomException("500", "密码错误");
        }
    }




    public Employee employeeLogin(Employee loginEmployee) {
        String username = loginEmployee.getUsername();
        String password = loginEmployee.getPassword();

        try {
            Employee dbEmployee = loginRegisterMapper.selectEmployeeByUsername(username);
            if (dbEmployee == null) {
                throw new CustomException("500", "员工不存在");
            }
            if (dbEmployee.getPassword() == null) {
                throw new CustomException("500", "密码未设置");
            }
            String stored = dbEmployee.getPassword();
            boolean matched;
            if (stored.startsWith("$2")) {
                matched = passwordEncoder.matches(password, stored);
            } else {
                matched = stored.equals(password) || passwordEncoder.matches(password, stored);
            }
            if (!matched) {
                throw new CustomException("500", "密码错误");
            }

            // 生成 Token (角色可设为 ROLE_EMPLOYEE)
            String token = TokenUtils.createToken(dbEmployee.getUsername(), String.valueOf(dbEmployee.getEmployeeId()), "ROLE_EMPLOYEE");
            dbEmployee.setToken(token);
            // 展示用编码
            dbEmployee.setCodeId(formatCode("003", dbEmployee.getEmployeeId()));

            // Redis 存储
            String redisKey = "login:employee:" + dbEmployee.getEmployeeId();
            redisTemplate.opsForValue().set(redisKey, dbEmployee, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

            return dbEmployee;

        } catch (Exception e) {
            if (e instanceof CustomException) throw e;
            throw new CustomException("500", "密码错误或认证失败");
        }
    }

    public Employee selectEmployeeByUsername(String username) {
        String cacheKey = "employee:username:" + username;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof Employee && ((Employee) cache).getUsername() != null) {
            return (Employee) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                Employee employee = loginRegisterMapper.selectEmployeeByUsername(username);
                if (employee != null) {
                    redisTemplate.opsForValue().set(cacheKey, employee, 30, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new Employee(), 2, TimeUnit.MINUTES);
                }
                return employee;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof Employee && ((Employee) cache).getUsername() != null) {
                    return (Employee) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return loginRegisterMapper.selectEmployeeByUsername(username);
        }
    }
    
    public Employee getEmployeeById(Long id) {
        return loginRegisterMapper.selectEmployeeById(id);
    }

    public PageInfo<User> selectAllUserPage(User user, Integer pageNum, Integer pageSize) {
        // 直接查询数据库，不使用缓存（避免缓存一致性问题）
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = loginRegisterMapper.selectAllUser(user);
        return PageInfo.of(list);
    }

    public Admin getAdminByUsername(String username) {
        String cacheKey = "admin:username:" + username;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof Admin && ((Admin) cache).getUsername() != null) {
            return (Admin) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                Admin admin = loginRegisterMapper.getAdminByUsername(username);
                if (admin != null) {
                    redisTemplate.opsForValue().set(cacheKey, admin, 30, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new Admin(), 2, TimeUnit.MINUTES);
                }
                return admin;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof Admin && ((Admin) cache).getUsername() != null) {
                    return (Admin) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return loginRegisterMapper.getAdminByUsername(username);
        }
    }

    public PageInfo<User> selectUserApproval(User user, Integer pageNum, Integer pageSize) {
        // 直接查询数据库，不使用缓存（避免缓存一致性问题）
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = loginRegisterMapper.selectUserApproval(user);
        return PageInfo.of(list);
    }

    public void passUser(String username) {
        loginRegisterMapper.passUser(username);
        // 异步并行清除用户详情缓存
        clearUserCacheAsync(username, null);
    }

    public void refuseUser(Long userId,String remark) {
        loginRegisterMapper.refuseUser(userId,remark);
        // 异步清除用户详情缓存
        clearUserCacheAsync(null, userId);
    }

    public void muteUser(Long userId, String remark) {
        loginRegisterMapper.muteUser(userId,remark);
        // 异步清除用户详情缓存
        clearUserCacheAsync(null, userId);
    }

    public void banUser(Long userId, String remark) {
        loginRegisterMapper.banUser(userId,remark);
        // 异步清除用户详情缓存
        clearUserCacheAsync(null, userId);
    }

    /**
     * 批量审批用户（多线程并行处理）
     */
    public void batchApproveUsers(List<String> usernames) {
        // 并行处理多个用户审批
        List<CompletableFuture<Void>> futures = usernames.stream()
                .map(username -> CompletableFuture.runAsync(() -> {
                    loginRegisterMapper.passUser(username);
                    clearUserCacheAsync(username, null);
                }, dataProcessExecutor))
                .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * 异步清除用户缓存
     */
    private void clearUserCacheAsync(String username, Long userId) {
        CompletableFuture.runAsync(() -> {
            if (username != null) {
                redisTemplate.delete("user:username:" + username);
                redisTemplate.delete("userdetails:user:" + username);
            }
            if (userId != null) {
                redisTemplate.delete("user:data:" + userId);
            }
        }, dataProcessExecutor);
    }

    private String formatCode(String prefix, Long id) {
        if (id == null) {
            return prefix + "000000";
        }
        String num = String.valueOf(id);
        if (num.length() < 6) {
            num = String.format("%06d", id);
        }
        return prefix + num;
    }
    
    public void deleteUser(Long userId) {
        loginRegisterMapper.deleteUser(userId);
        redisTemplate.delete("user:data:" + userId);
    }
    
    public void unmuteUser(Long userId) {
        loginRegisterMapper.unmuteUser(userId);
        clearUserCacheAsync(null, userId);
    }
}
