// mapper/FeeBillMapper.java
package com.example.modules.business.fee.mapper;

import com.example.entity.Alipay.FeeBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface FeeBillMapper {
    List<FeeBill> listByResidence(@Param("residenceId") String residenceId);
    FeeBill findByIdFromfee(@Param("id") Long id);
    FeeBill findByBillNo(@Param("billNo") String billNo);
    int insertTofee(FeeBill bill);
    int updateStatusAndOrder(@Param("id") Long id,
                             @Param("status") Integer status,
                             @Param("payOrderId") Long payOrderId);
    int markPaid(@Param("id") Long id, @Param("status") Integer status);

    List<FeeBill> allBills();

    List<FeeBill> FilterMonth(Date periodStart);

    List<FeeBill> findByMonth(@Param("monthStr") String monthStr);
}
