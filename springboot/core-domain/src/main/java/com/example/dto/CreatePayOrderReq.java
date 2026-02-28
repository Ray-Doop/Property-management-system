// dto/CreatePayOrderReq.java
package com.example.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
//import javax.validation.constraints.NotNull;

/** 创建支付订单请求（前端仅传 billId，金额以后台账单为准） */
@Data
public class CreatePayOrderReq {
    @NotNull(message = "billId不能为空")
    private Long billId;
}
