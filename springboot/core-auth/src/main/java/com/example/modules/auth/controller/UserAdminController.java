package com.example.modules.auth.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.common.Result;
import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import com.example.modules.auth.service.LoginRegisterService;
import com.example.utils.CaptchaUtil;
import com.example.utils.TokenUtils;
import com.example.exception.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/LoginRegister")
public class UserAdminController {
    @Resource
    private LoginRegisterService loginRegisterService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${wechat.appid:}")
    private String wechatAppid;

    @Value("${wechat.secret:}")
    private String wechatSecret;

    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_SECONDS = 300; // 验证码5分钟过期

    /**
     * 生成验证码
     */
    @GetMapping("/captcha")
    public Result generateCaptcha() {
        CaptchaUtil.CaptchaResult captchaResult = CaptchaUtil.generateCaptcha();
        
        // 将验证码存储到Redis，5分钟过期
        String captchaKey = CAPTCHA_PREFIX + captchaResult.getCaptchaId();
        redisTemplate.opsForValue().set(captchaKey, captchaResult.getCode(), CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        // 返回验证码ID和图片（不返回验证码字符串）
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaResult.getCaptchaId());
        result.put("imageBase64", captchaResult.getImageBase64());
        
        return Result.success(result);
    }
    
    /**
     * 验证验证码
     */
    private boolean validateCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        
        String captchaKey = CAPTCHA_PREFIX + captchaId;
        String storedCode = (String) redisTemplate.opsForValue().get(captchaKey);
        
        if (storedCode == null) {
            return false; // 验证码已过期或不存在
        }
        
        // 验证后删除验证码（一次性使用）
        redisTemplate.delete(captchaKey);
        
        // 不区分大小写比较
        return storedCode.equalsIgnoreCase(userInput);
    }
    
    //登录
    @PostMapping("/userLogin")
    public Result login(@RequestBody User loginUser) {
        // 验证验证码
        if (loginUser.getCaptchaId() == null || loginUser.getCaptchaCode() == null) {
            throw new CustomException("400", "请输入验证码");
        }
        
        if (!validateCaptcha(loginUser.getCaptchaId(), loginUser.getCaptchaCode())) {
            throw new CustomException("400", "验证码错误或已过期");
        }
        
        User db = loginRegisterService.login(loginUser);
        return Result.success(db);
    }

    // 微信一键登录（模拟）
    @PostMapping("/wechatLogin")
    public Result wechatLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String openid = body.get("openid");
        if ((code == null || code.isEmpty()) && (openid == null || openid.isEmpty())) {
            throw new CustomException("400", "Code不能为空");
        }

        if (openid == null || openid.isEmpty()) {
            openid = getOpenidByCode(code);
        }
        
        // 尝试通过 openid 登录
        User user = loginRegisterService.loginByOpenid(openid);
        
        if (user != null) {
            // 登录成功
            return Result.success(user);
        } else {
            // 用户不存在，返回特定状态码（如 201）和 openid，前端需跳转注册
            Map<String, Object> data = new HashMap<>();
            data.put("needRegister", true);
            data.put("openid", openid);
            return Result.success(data);
        }
    }

    private String getOpenidByCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new CustomException("400", "Code不能为空");
        }
        if (wechatAppid == null || wechatAppid.isEmpty() || wechatSecret == null || wechatSecret.isEmpty()) {
            throw new CustomException("500", "微信配置缺失");
        }
        String url = UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", wechatAppid)
                .queryParam("secret", wechatSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        try {
            RestTemplate client = restTemplate != null ? restTemplate : new RestTemplate();
            String response = client.getForObject(url, String.class);
            Map<String, Object> data = new ObjectMapper().readValue(
                    response == null ? "{}" : response,
                    new TypeReference<Map<String, Object>>() {}
            );
            Object errcode = data.get("errcode");
            if (errcode != null && !"0".equals(String.valueOf(errcode))) {
                String errMsg = String.valueOf(data.getOrDefault("errmsg", "微信登录失败"));
                throw new CustomException("500", errMsg);
            }
            Object openid = data.get("openid");
            if (openid == null || openid.toString().isEmpty()) {
                String errMsg = String.valueOf(data.getOrDefault("errmsg", "微信登录失败"));
                throw new CustomException("500", errMsg);
            }
            return openid.toString();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("500", "微信登录失败");
        }
    }


    //管理员登录
    @PostMapping("/adminlogin")
    public Result Adminlogin(@RequestBody Admin admin) {
        // 验证验证码
        if (admin.getCaptchaId() == null || admin.getCaptchaCode() == null) {
            throw new CustomException("400", "请输入验证码");
        }
        
        if (!validateCaptcha(admin.getCaptchaId(), admin.getCaptchaCode())) {
            throw new CustomException("400", "验证码错误或已过期");
        }
        
        // 调用 Service 层登录逻辑（Spring Security 自动验证密码）
        System.out.println("111");
        Admin db = loginRegisterService.adminLogin(admin);
        return Result.success(db);
    }

    @PostMapping("/employeeLogin")
    public Result employeeLogin(@RequestBody Employee employee) {
        Employee dbEmployee = loginRegisterService.employeeLogin(employee);
        return Result.success(dbEmployee);
    }
    // 登出
    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 解析 token 获取 id + role
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(TokenUtils.getSecret())).build().verify(token);
        Long id = decodedJWT.getClaim("id").asLong();
        String role = decodedJWT.getClaim("role").asString();
        loginRegisterService.logout(role, id);
        return Result.success("退出成功");
    }
    //注册
    @PostMapping("/userRegister")
    public Result register(@RequestBody User user) {
        System.out.println("你的数据" + user);
        loginRegisterService.register(user);
        return Result.success();
    }

    //获取用户数据
    @GetMapping("/GetUserData")
    public Result getUserData(@RequestParam Long userId) {
        System.out.println("1111111111111111111111111111111111111111111111111111111");
        User user = loginRegisterService.getUserData(userId);
        return Result.success(user);
    }
    
    //获取管理员数据
    @GetMapping("/GetAdminData")
    public Result getAdminData(@RequestParam Long adminId) {
        Admin admin = loginRegisterService.getAdminById(adminId);
        return Result.success(admin);
    }

    //更改密码
    @PostMapping("/CheckOldPassword")
    public Result checkOldPassword(@RequestBody User user) {
        loginRegisterService.login(user);
        return Result.success();
    }
    
    //管理员验证旧密码
    @PostMapping("/CheckOldAdminPassword")
    public Result checkOldAdminPassword(@RequestBody Admin admin) {
        loginRegisterService.adminLogin(admin);
        return Result.success();
    }

    @PostMapping("/UpdatePassword")
    public Result UpdatePassword(@RequestBody User user) {
        loginRegisterService.UpdatePassword(user);
        return Result.success();
    }
    
    //管理员修改密码
    @PostMapping("/UpdateAdminPassword")
    public Result updateAdminPassword(@RequestBody Admin admin) {
        loginRegisterService.updateAdminPassword(admin);
        return Result.success();
    }

    //更改头像
    @PostMapping("/UpdateAvatar")
    public Result updateAvatar(@RequestBody Map<String, Object> request) {
        // 检查是否包含adminId，如果有则更新管理员头像，否则更新用户头像
        if (request.containsKey("adminId")) {
            Long adminId = Long.parseLong(request.get("adminId").toString());
            String avatarUrl = request.get("avatarUrl").toString();
            loginRegisterService.updateAdminAvatar(adminId, avatarUrl);
        } else {
            Long userId = Long.parseLong(request.get("userId").toString());
            String avatarUrl = request.get("avatarUrl").toString();
            loginRegisterService.updateAvatar(userId, avatarUrl);
        }
        return Result.success();
    }

    //改数据
    @PostMapping("/UpdateUserData")
    public Result UpdateUserData(@RequestBody User user) {
        loginRegisterService.UpdateUserData(user);
        return Result.success();
    }
    
    //管理员修改资料
    @PostMapping("/UpdateAdminData")
    public Result updateAdminData(@RequestBody Admin admin) {
        loginRegisterService.updateAdminData(admin);
        return Result.success();
    }



    //查找所有用户
    @GetMapping("/selectAllUser")
    public Result selectAllUser(User user,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("2222");
        PageInfo<User> pageInfo = loginRegisterService.selectAllUserPage(user, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //审核列表
    @GetMapping("/approval")
    public Result approval(User user,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("2222");
        PageInfo<User> pageInfo = loginRegisterService.selectUserApproval(user, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //用户名查找
    @GetMapping("/selectUserByUsername")
    public Result selectUserByUsername(@RequestParam String username) {
        User user = loginRegisterService.findByUsername(username);
        System.out.println(user);
        return Result.success(user);
    }

    //通过
    @PostMapping("/pass")
    public Result passUser(@RequestParam String username) {
        loginRegisterService.passUser(username);
        return Result.success();
    }

    //拒绝
    @PostMapping("/refuse")
    public Result refuseUser(@RequestParam Long userId, @RequestParam String remark) {
        loginRegisterService.refuseUser(userId, remark);
        return Result.success();
    }

    //禁言
    @PostMapping("/mute")
    public Result mute(@RequestParam Long userId, @RequestParam String remark) {
        loginRegisterService.muteUser(userId, remark);
        return Result.success();
    }

    //封禁
    @PostMapping("/ban")
    public Result ban(@RequestParam Long userId, @RequestParam String remark) {
        loginRegisterService.banUser(userId, remark);
        return Result.success();
    }
    
    @DeleteMapping("/deleteUser")
    public Result deleteUser(@RequestParam Long userId) {
        loginRegisterService.deleteUser(userId);
        return Result.success();
    }
    
    @PostMapping("/unmute")
    public Result unmute(@RequestParam Long userId) {
        loginRegisterService.unmuteUser(userId);
        return Result.success();
    }
}
