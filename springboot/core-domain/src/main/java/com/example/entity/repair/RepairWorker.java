package com.example.entity.repair;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class RepairWorker {
    private Long workerId;     // 维修人员ID
    private String name;       // 姓名
    private String phone;      // 联系电话
    private String specialty;  // 擅长领域（水电/空调/电梯等）
    private String avatar;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;  // 入职时间
}
