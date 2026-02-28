// mapper/PayOrderMapper.java
package com.example.modules.business.pay.mapper;


import com.example.entity.Alipay.PayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface PayOrderMapper {
    int insertToPayorder(PayOrder order);
    PayOrder findByOrderNo(@Param("orderNo") String orderNo);
    int markSuccess(@Param("orderNo") String orderNo,
                    @Param("tradeNo") String tradeNo);
    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("status") Integer status);

    Integer selectPayStatusByOrderNo(@Param("params") Map<String, String> params);

    PayOrder selectByOutTradeNo(@Param("orderNo") String orderNo);
}
