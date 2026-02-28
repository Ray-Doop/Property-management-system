// entity/pay/PayOrder.java
package com.example.entity.Alipay;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/** 支付订单实体（与表 pay_order 对应） */
@Data
public class PayOrder {
    private Long id;
    private String orderNo;
    private String residenceId;
    private Long billId;
    private String subject;
    private BigDecimal totalAmount;
    private String payChannel; // ALIPAY
    private Integer status;    // 0-已关闭，1-待支付，2-支付成功，3-支付失败，4-已退款
    private String tradeNo;    // 支付宝交易号
    private Date payTime;
    private Date notifyTime;
    private String clientIp;
    private String extra;      // JSON 字符串
    private Date createdAt;
    private Date updatedAt;
}
