package com.example.entity;

import lombok.Data;
import java.util.Date;

/**
 * 出行码申请与使用记录实体
 */
@Data
public class TravelPassRecord {
    private Long id;
    private Long userId;         // 用户ID
    private String username;     // 用户名
    private String nickname;     // 用户昵称
    private String avatar;       // 用户头像
    private Long employeeId;     // 核销人员工ID
    private Boolean hasVehicle;  // 是否有车辆
    private String plateNumber;  // 车牌号
    private Boolean paid;        // 是否支付通行费
    private Date issueTime;      // 生成时间
    private Date expireTime;     // 过期时间
    private Date entryTime;      // 入场时间
    private Date exitTime;       // 出场时间
    private Double fee;          // 停车费
    private String status;       // 状态：ISSUED, ENTERED, EXITED
}
