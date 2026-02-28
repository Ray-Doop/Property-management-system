package com.example.entity.repair;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class RepairAssignment {
    private Long assignmentId;  // 指派记录ID
    private Long orderId;       // 报修单ID
    private Long workerId;      // 维修人员ID
    private Long assignedBy;    // 指派人ID（物业管理员）从本地内存获取adminId
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignedTime;  // 指派时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime visitingTime;  //上门时间
    private String status;      // 指派状态（已指派/已接受/已拒绝/已完成）


}
