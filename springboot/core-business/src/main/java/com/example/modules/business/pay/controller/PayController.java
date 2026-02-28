// controller/PayController.java
package com.example.modules.business.pay.controller;

import com.example.dto.CreatePayOrderReq;
import com.example.entity.Alipay.PayOrder;
import com.example.modules.business.pay.mapper.PayOrderMapper;
import com.example.modules.business.pay.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** 支付相关接口：创建订单、同步回跳、异步通知 */
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {
    private final PayService payService;

    @Autowired
    private PayOrderMapper payOrderMapper;

    /** 创建支付订单：返回可直接渲染提交的 HTML 表单 */
    @PostMapping(value = "/create/alipay", produces = MediaType.TEXT_HTML_VALUE)
    public String create(@RequestParam String residenceId, // 改为 String 类型
                         @Validated @RequestBody CreatePayOrderReq req,
                         HttpServletRequest httpReq) throws Exception {
        String clientIp = httpReq.getRemoteAddr();
        return payService.createAlipayOrder(residenceId, req, clientIp);
    }

    /** GET方式获取支付页面（用于小程序 web-view） */
    @GetMapping(value = "/page/alipay", produces = MediaType.TEXT_HTML_VALUE)
    public String alipayPage(@RequestParam String residenceId,
                             @RequestParam Long billId,
                             HttpServletRequest httpReq) throws Exception {
        CreatePayOrderReq req = new CreatePayOrderReq();
        req.setBillId(billId);
        String clientIp = httpReq.getRemoteAddr();
        return payService.createAlipayOrder(residenceId, req, clientIp);
    }

    /** 同步回跳（GET）：前端 result 页面会调用本接口进行验签与展示 */
    @GetMapping("/return/alipay")
    public Map<String, Object> returnPay(@RequestParam("out_trade_no") String outTradeNo) {
        Map<String, Object> result = new HashMap<>();
        try {
            PayOrder order = payOrderMapper.findByOrderNo(outTradeNo);
            if (order == null) {
                result.put("status", 1); // 未支付（订单不存在）
            } else if (order.getStatus() == 2) {
                result.put("status", 2); // 支付成功
            } else if (order.getStatus() == 0) {
                result.put("status", 0); // 支付中
            } else {
                result.put("status", 1); // 默认当成未支付
            }
        } catch (Exception e) {
            result.put("status", 1); // 异常时也当成未支付
        }
        return result;
    }

    /** 异步通知（POST）：支付宝服务器回调 */
    @PostMapping(value = "/notify/alipay", produces = "text/plain;charset=UTF-8")
    public String notifyUrl(HttpServletRequest request) {
        try {
            Map<String, String> params = PayService.toParamMap(request);
            boolean ok = payService.handleNotify(params);
            return ok ? "success" : "failure"; // 必须返回 success 才表示已接收
        } catch (Exception e) {
            return "failure";
        }
    }
}
