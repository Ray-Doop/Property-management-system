// entity/fee/FeeBill.java
package com.example.entity.Alipay;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/** 费用账单实体（与表 fee_bill 对应） */
@Data
public class FeeBill {
    private Long id;
    private String billNo;
    private String residenceId;
    private String title;
    private BigDecimal unitPrice;
    private BigDecimal areaSnapshot;
    private Date periodStart;
    private Date periodEnd;
    private BigDecimal amount;
    private Integer status; // 0-已取消，1-待支付，2-已支付，3-已关闭
    private Long payOrderId;
    private Date dueDate;
    private String remark;
    private Date createdAt;
    private Date updatedAt;
}
