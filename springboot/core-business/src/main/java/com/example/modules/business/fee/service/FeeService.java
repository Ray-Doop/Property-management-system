// service/FeeService.java
package com.example.modules.business.fee.service;

//import com.example.entity.fee.FeeBill;
import com.example.entity.Alipay.FeeBill;
import com.example.modules.business.fee.mapper.FeeBillMapper;
import com.example.modules.auth.service.LoginRegisterService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 账单查询与状态维护 */
@Service
public class FeeService {
    @Autowired
    private FeeBillMapper billMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    @Qualifier("dataProcessExecutor")
    private Executor dataProcessExecutor;

    public FeeBill getById(Long id) {
        String cacheKey = "fee:bill:" + id;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof FeeBill && ((FeeBill) cache).getId() != null) {
            return (FeeBill) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                FeeBill bill = billMapper.findByIdFromfee(id);
                if (bill != null) {
                    // 账单信息缓存20分钟
                    redisTemplate.opsForValue().set(cacheKey, bill, 20, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, new FeeBill(), 5, TimeUnit.MINUTES);
                }
                return bill;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof FeeBill && ((FeeBill) cache).getId() != null) {
                    return (FeeBill) cache;
                }
            } catch (InterruptedException ignored) {
            }
            return billMapper.findByIdFromfee(id);
        }
    }

    public FeeBill getByBillNo(String billNo) {
        // 简单实现，暂时不加复杂缓存逻辑，或者直接查库
        return billMapper.findByBillNo(billNo);
    }

    /**
     * 批量获取账单（多线程并行查询）
     */
    public List<FeeBill> batchGetByIds(List<Long> billIds) {
        // 并行查询多个账单
        List<CompletableFuture<FeeBill>> futures = billIds.stream()
                .map(billId -> CompletableFuture.supplyAsync(() -> getById(billId), dataProcessExecutor))
                .collect(Collectors.toList());
        
        // 等待所有查询完成并返回结果
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(bill -> bill != null)
                .collect(Collectors.toList());
    }

    public void linkOrderAndMarkPending(Long billId, Long payOrderId) {
        billMapper.updateStatusAndOrder(billId, 1, payOrderId); // 仍然待支付
        redisTemplate.delete("fee:bill:" + billId);
        clearFeeListCache();
    }

    /**
     * 批量标记账单为已支付（多线程并行处理）
     */
    public void batchMarkPaid(List<Long> billIds) {
        // 并行处理多个账单
        List<CompletableFuture<Void>> futures = billIds.stream()
                .map(billId -> CompletableFuture.runAsync(() -> {
                    billMapper.markPaid(billId, 2);
                    redisTemplate.delete("fee:bill:" + billId);
                }, dataProcessExecutor))
                .collect(Collectors.toList());
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        clearFeeListCache();
    }

    public void markPaid(Long billId) {
        billMapper.markPaid(billId, 2); // 2-已支付
        redisTemplate.delete("fee:bill:" + billId);
        clearFeeListCache();
    }


    public PageInfo<FeeBill> allBills(Integer pageNum, Integer pageSize) {
        String cacheKey = "fee:bills:all:" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<FeeBill>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<FeeBill> list = billMapper.allBills();
                PageInfo<FeeBill> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 账单列表缓存10分钟
                    long expireMinutes = 8 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<FeeBill>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<FeeBill> list = billMapper.allBills();
            return PageInfo.of(list);
        }
    }

    public PageInfo<FeeBill> listByResidenceId(String residenceId, Integer pageNum, Integer pageSize) {
        String cacheKey = "fee:bills:residence:" + residenceId + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<FeeBill>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<FeeBill> list = billMapper.listByResidence(residenceId);
                PageInfo<FeeBill> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 8 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<FeeBill>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<FeeBill> list = billMapper.listByResidence(residenceId);
            return PageInfo.of(list);
        }
    }

    public PageInfo<FeeBill> FilterMonth(Date periodStart, Integer pageNum, Integer pageSize) {
        String periodKey = periodStart != null ? String.valueOf(periodStart.getTime()) : "";
        String cacheKey = "fee:bills:month:" + periodKey + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<FeeBill>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<FeeBill> list = billMapper.FilterMonth(periodStart);
                PageInfo<FeeBill> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 8 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<FeeBill>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<FeeBill> list = billMapper.FilterMonth(periodStart);
            return PageInfo.of(list);
        }
    }

    /**
     * 通过认证模块服务获取用户数据，避免业务模块直接访问认证模块 Mapper
     */
    @Autowired
    private LoginRegisterService loginRegisterService;

    public int publishMonthlyFee(Date month, java.math.BigDecimal unitPrice, String title) {
        // 1. 获取所有状态正常(status=1)的用户
        List<com.example.entity.User> users = loginRegisterService.selectAllValidUsers();
        
        // 2. 遍历用户生成账单
        Date now = new Date();
        // 计算账期（默认为传入月份的1号到月底）
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(month);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();
        cal.add(java.util.Calendar.MONTH, 1);
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        Date end = cal.getTime();

        // 截止日期默认设置为下个月15号
        cal.add(java.util.Calendar.DAY_OF_MONTH, 15); 
        Date dueDate = cal.getTime();

        // 检查当月是否已发布账单
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
        String monthStr = sdf.format(start);
        List<FeeBill> existingBills = billMapper.findByMonth(monthStr);
        java.util.Set<String> existingResidences = existingBills.stream()
                .map(FeeBill::getResidenceId)
                .map(String::trim)
                .collect(Collectors.toSet());

        List<FeeBill> bills = new java.util.ArrayList<>();
        for (com.example.entity.User user : users) {
            // 跳过没有房产信息的用户
            if (user.getBuildingNo() == null || user.getUnitNo() == null || user.getRoomNo() == null || user.getArea() == null) {
                continue;
            }

            // 构造 residenceId: 楼栋-单元-房号
            String residenceId = (user.getBuildingNo() + "-" + user.getUnitNo() + "-" + user.getRoomNo()).trim();

            // 如果该住户当月已存在账单，则跳过
            if (existingResidences.contains(residenceId)) {
                continue;
            }

            FeeBill bill = new FeeBill();
            bill.setBillNo("FB" + System.currentTimeMillis() + (int)(Math.random()*1000));
            
            bill.setResidenceId(residenceId);
            
            bill.setTitle(title);
            bill.setUnitPrice(unitPrice);
            java.math.BigDecimal area = java.math.BigDecimal.valueOf(user.getArea());
            bill.setAreaSnapshot(area);
            
            // 计算总金额 = 单价 * 面积
            bill.setAmount(unitPrice.multiply(area));
            
            bill.setPeriodStart(start);
            bill.setPeriodEnd(end);
            bill.setStatus(1); // 待支付
            bill.setDueDate(dueDate);
            bill.setRemark("系统批量发布");
            
            bills.add(bill);
        }
        
        // 3. 批量插入（这里简单循环插入，量大建议优化为批量Insert）
        for (FeeBill bill : bills) {
            billMapper.insertTofee(bill);
        }
        clearFeeListCache();
        return bills.size();
    }

    private void clearFeeListCache() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("fee:bills:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
