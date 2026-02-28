package com.example.entity;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class User {

    private Long userId;        // 用户唯一ID（主键）
    private String username;    // 用户名（登录用，唯一）
    private String password;    // 密码（BCrypt哈希值）
    private String nickname;    // 昵称（展示用）
    private String avatarUrl;   // 头像存储路径（如OSS地址）
    private String phone;       // 手机号（登录/通知用）
    private String idCard;      // 身份证号（实名认证用）
    private String vehicleInfo; // 绑定车辆信息（如"沪A·12345"）
    private Integer status;     // 账号状态：0-未激活，1-正常，2-禁言，3-封禁
    @Column(name = "register_time")
    private String registerTime;// 注册时间（格式：yyyy-MM-dd HH:mm:ss）
    private String lastLoginTime;// 最后登录时间（格式：yyyy-MM-dd HH:mm:ss）
    private Integer points;     // 积分（社区商城用）
    private String remark;      // 备注（管理员添加）
    private String token;
    private Integer buildingNo;
    private Integer unitNo;
    private Integer roomNo;
    private String avatar;
    @Column(name = "area", nullable = false, columnDefinition = "INT COMMENT '用户房产面积'")
    private Integer area;//面积
    private String residenceId;
    @jakarta.persistence.Transient
    private String codeId; // 展示用ID：001+数字
    
    // 验证码相关字段（不持久化到数据库）
    @jakarta.persistence.Transient
    private String captchaId;      // 验证码ID
    @jakarta.persistence.Transient
    private String captchaCode;    // 用户输入的验证码
    
    // 无参构造函数

}
