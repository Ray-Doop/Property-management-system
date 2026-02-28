// controller/FeeController.java
package com.example.modules.business.fee.controller;

//import com.example.entity.fee.FeeBill;

import com.example.common.Result;
import com.example.entity.Alipay.FeeBill;
import com.example.entity.Forum.CommentWithPostDTO;
import com.example.entity.Forum.ForumPost;
import com.example.modules.business.fee.service.FeeService;
import com.github.pagehelper.PageInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 账单相关接口：前端列出并展示
 */
@RestController
@RequestMapping("/api/fee")
@RequiredArgsConstructor
public class FeeController {
    private final FeeService feeService;

    //根据当前住宅ID获取账单列表
    @GetMapping("/billsById")
    public Result allBills(@RequestParam String residenceId, @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<FeeBill> pageInfo = feeService.listByResidenceId(residenceId,pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @GetMapping("/billDetail")
    public Result billDetail(@RequestParam String billNo) {
        FeeBill bill = feeService.getByBillNo(billNo);
        if (bill == null) {
            return Result.error("404", "账单不存在");
        }
        return Result.success(bill);
    }

    //所有列表
    @GetMapping("/allBills")
    public Result allBills(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<FeeBill> pageInfo = feeService.allBills(pageNum, pageSize);
        return Result.success(pageInfo);
    }
    //筛选月份
    @GetMapping("/FilterMonth")
    public Result FilterMonth(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam String periodStart) {

        // 把 yyyyMMdd 转换成 Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.parse(periodStart, formatter);

        // 如果你数据库存的是 Date，需要转成 java.util.Date
        Date date = java.sql.Date.valueOf(localDate);

        PageInfo<FeeBill> pageInfo = feeService.FilterMonth(date, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/publishMonthly")
    public Result publishMonthly(@RequestBody java.util.Map<String, Object> params) {
        String monthStr = (String) params.get("month"); // yyyy-MM
        Object unitPriceObj = params.get("unitPrice");
        String unitPriceStr = unitPriceObj != null ? String.valueOf(unitPriceObj) : null;
        String title = (String) params.get("title");
        
        if (monthStr == null || unitPriceStr == null || title == null) {
            return Result.error("400", "参数不完整");
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
            Date month = sdf.parse(monthStr);
            java.math.BigDecimal unitPrice = new java.math.BigDecimal(unitPriceStr);
            
            int count = feeService.publishMonthlyFee(month, unitPrice, title);
            if (count == 0) {
                return Result.error("400", "该月账单已发布，请勿重复发布");
            }
            return Result.success("发布成功，新增 " + count + " 条账单");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500", "发布失败: " + e.getMessage());
        }
    }

}
