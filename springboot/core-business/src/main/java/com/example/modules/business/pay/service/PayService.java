package com.example.modules.business.pay.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.common.AlipayProperties;
import com.example.dto.CreatePayOrderReq;
import com.example.entity.Alipay.FeeBill;
import com.example.entity.Alipay.PayOrder;
import com.example.modules.business.pay.mapper.PayOrderMapper;
import com.example.modules.business.fee.service.FeeService;
import com.example.utils.Enums;
import com.example.utils.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProps;
    private final PayOrderMapper payOrderMapper;
    private final FeeService feeService;

    /** 创建支付宝支付订单并返回前端可直接提交的HTML表单 */
    @Transactional
    public String createAlipayOrder(String residenceId, CreatePayOrderReq req, String clientIp) throws AlipayApiException {
        // 账单校验
        FeeBill bill = feeService.getById(req.getBillId());
        if (bill == null || !bill.getResidenceId().equals(residenceId)) {
            throw new RuntimeException("账单不存在或无权支付");
        }
        if (bill.getStatus() != Enums.BillStatus.PENDING) {
            throw new RuntimeException("账单当前状态不可支付");
        }

        // 生成商户订单号
        String orderNo = IdUtil.next("PO");
        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setResidenceId(residenceId); // String 类型
        order.setBillId(bill.getId());
        order.setSubject(bill.getTitle());
        order.setTotalAmount(bill.getAmount());
        order.setPayChannel("ALIPAY");
        order.setStatus(Enums.PayStatus.PENDING);
        order.setClientIp(clientIp);
        payOrderMapper.insertToPayorder(order);

        // 关联账单
        feeService.linkOrderAndMarkPending(bill.getId(), order.getId());

        // 构造支付宝请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProps.getNotifyUrl());
        request.setReturnUrl(alipayProps.getReturnUrl());

        Map<String, Object> biz = new HashMap<>();
        biz.put("out_trade_no", orderNo);
        biz.put("product_code", "FAST_INSTANT_TRADE_PAY");
        biz.put("total_amount", order.getTotalAmount().toPlainString());
        biz.put("subject", order.getSubject());
        request.setBizContent(com.alibaba.fastjson.JSON.toJSONString(biz));

        return alipayClient.pageExecute(request).getBody();
    }


    /** 支付宝同步跳转（GET） */
    public boolean handleReturn(Map<String, String> params) {
        try {
            boolean signOk = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProps.getAlipayPublicKey(),
                    alipayProps.getCharset(),
                    alipayProps.getSignType());
            if (!signOk) {
                return true;
            }
            String outTradeNo = params.get("out_trade_no");
            String totalAmountStr = params.get("total_amount");
            PayOrder order = payOrderMapper.findByOrderNo(outTradeNo);
            if (order == null) return false;
            if (totalAmountStr != null &&
                    new BigDecimal(totalAmountStr).compareTo(order.getTotalAmount()) != 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 支付宝异步通知（POST） */
    @Transactional
    public boolean handleNotify(Map<String, String> params) throws Exception {
        boolean signOk = AlipaySignature.rsaCheckV1(
                params,
                alipayProps.getAlipayPublicKey(),
                alipayProps.getCharset(),
                alipayProps.getSignType());
        if (!signOk) return false;

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmountStr = params.get("total_amount");

        PayOrder order = payOrderMapper.findByOrderNo(outTradeNo);
        if (order == null) return false;

        BigDecimal notifyAmount = new BigDecimal(totalAmountStr);
        if (notifyAmount.compareTo(order.getTotalAmount()) != 0) return false;
        if (order.getStatus() == Enums.PayStatus.SUCCESS) return true;

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            payOrderMapper.markSuccess(outTradeNo, tradeNo);
            feeService.markPaid(order.getBillId());
            return true;
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            payOrderMapper.updateStatus(outTradeNo, Enums.PayStatus.CLOSED);
            return true;
        }
        return false;
    }

    /** 将 HttpServletRequest 的参数转为 Map<String,String> */
    public static Map<String, String> toParamMap(HttpServletRequest request) {
        Map<String, String[]> requestParams = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        requestParams.forEach((k, v) -> params.put(k, String.join(",", v)));
        return params;
    }

    public PayOrder getByOutTradeNo(String outTradeNo) {
        return payOrderMapper.selectByOutTradeNo(outTradeNo);
    }
}
