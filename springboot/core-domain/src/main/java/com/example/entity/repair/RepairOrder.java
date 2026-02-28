package com.example.entity.repair;

import jakarta.persistence.Column;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class RepairOrder {
    private Long orderId;          // 报修单ID
    private Long userId;           // 报修人ID（住户）
    private Long categoryId;       // 报修类别ID
    private String description;    // 报修描述
    private String status;         // 状态（待处理/已指派/维修中/已完成/已取消）
    private String priority;       // 优先级（普通/紧急）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;      // 报修时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;      // 最后更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appointmentTime;
    private Long assignedWorker;   // 维修人员ID（如果已指派）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;     // 完成时间
    private Integer buildingNo;
    private Integer unitNo;
    private Integer roomNo;
    private String phone;
    private String name; // 业主姓名
    private List<RepairFile> files;
    
    @jakarta.persistence.Transient
    private List<String> fileUrls; // 用于接收前端上传的图片URL
    
    private String evaluation; // 评价内容
    private Integer rating;    // 评分 (1-5)
    
    private RepairCategory category;
    private String categoryName;

    private String workerName; // 维修人员姓名
    private String replyContent; // 评价回复内容





}
