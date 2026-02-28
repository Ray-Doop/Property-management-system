package com.example.modules.business.repair.service;

import com.example.entity.Employee;
import com.example.entity.Forum.ForumAttachment;
import com.example.entity.Forum.ForumPost;
import com.example.entity.repair.*;
import com.example.modules.business.repair.mapper.RepairMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class RepairService {
    @Autowired
    private RepairMapper repairMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    @Qualifier("cacheCleanExecutor")
    private Executor cacheCleanExecutor;

    public List<RepairCategory> selectCategories() {
        return repairMapper.selectCategories();
    }


    @Transactional
    public Long addRepairOrder(RepairOrder repairOrder) {
        // 1. 设置创建和更新时间
        repairOrder.setCreatedTime(LocalDateTime.now());
        repairOrder.setUpdatedTime(LocalDateTime.now());

        // 2. 保存报修单
        repairMapper.addRepairOrder(repairOrder);

        // 3. 此时 repairOrder.orderId 已经被 MyBatis 回填
        Long orderId = repairOrder.getOrderId();

        // 4. 保存附件
        if (repairOrder.getFiles() != null && !repairOrder.getFiles().isEmpty()) {
            for (RepairFile file : repairOrder.getFiles()) {
                RepairFile repairFile = new RepairFile();
                repairFile.setOrderId(orderId);
                repairFile.setUserID(repairOrder.getUserId());
                repairFile.setFileUrl(file.getFileUrl());
                repairFile.setFileType(file.getFileType() != null ? file.getFileType() : "image");
                repairFile.setUploadedAt(LocalDateTime.now());
                repairMapper.addRepairFiles(repairFile);
            }
        }
        
        // 清除相关缓存
        if (repairOrder.getUserId() != null) {
            // 清除用户相关的维修单列表缓存
            clearUserRepairListCache(Math.toIntExact(repairOrder.getUserId()));
        }
        // 清除所有维修单列表缓存
        clearRepairListCache();
        
        return orderId;
    }

    //我的维修
    public PageInfo<RepairOrder> selectMyRepair(Integer userID, Integer page, Integer size) {


        String cacheKey = "Repair:MyRepair:" + userID + ":" + page + ":" + size;
        String lockKey = "lock:" + cacheKey;

        Object db = redisTemplate.opsForValue().get(cacheKey);

        if (db != null) {
            return (PageInfo<RepairOrder>) db;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, 1, 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(page, size);
                List<RepairOrder> list = repairMapper.selectMyRepair(userID);
                PageInfo<RepairOrder> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, "", 20, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, 1, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // ====== 7. 未拿到锁，短暂等待再查缓存（防止同时查询数据库） ======
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }
            // 再查一次缓存（此时可能被其他线程写入了）
            Object retryCache = redisTemplate.opsForValue().get(cacheKey);
            if (retryCache != null) {
                return (PageInfo<RepairOrder>) db;
            }

            // 如果仍然没有缓存（极端情况），再从数据库查询（不建议频繁发生）
            PageHelper.startPage(page, size);
            List<RepairOrder> list = repairMapper.selectMyRepair(userID);
            PageInfo<RepairOrder> pageInfo = PageInfo.of(list);
            return pageInfo;
        }
    }

    
    public RepairOrder getRepairDetail(Integer orderId) {
        String cacheKey = "repair:detail:" + orderId;
        String lockKey = "lock:" + cacheKey;

        // 先查缓存
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof RepairOrder) {
            RepairOrder cachedOrder = (RepairOrder) cache;
            // 检查是否有orderId，空对象则返回null
            if (cachedOrder.getOrderId() != null) {
                return cachedOrder;
            }
            return null;
        }

        // 未命中缓存，加锁查询
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // 查询报修单信息（包含 categoryName）
                RepairOrder repairOrder = repairMapper.getRepairDetailByOrderId(orderId);
                if (repairOrder == null) {
                    // 缓存空对象，5分钟过期
                    redisTemplate.opsForValue().set(cacheKey, new RepairOrder(), 5, TimeUnit.MINUTES);
                    return null;
                }

                // 查询附件列表
                List<RepairFile> files = repairMapper.getRepairFilesByOrderId(orderId);
                repairOrder.setFiles(files);

                // 缓存维修详情，15分钟过期
                redisTemplate.opsForValue().set(cacheKey, repairOrder, 15, TimeUnit.MINUTES);
                return repairOrder;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未拿到锁，等待后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof RepairOrder) {
                    RepairOrder cachedOrder = (RepairOrder) cache;
                    if (cachedOrder.getOrderId() != null) {
                        return cachedOrder;
                    }
                    return null;
                }
            } catch (InterruptedException ignored) {
            }
            // 兜底查询
            RepairOrder repairOrder = repairMapper.getRepairDetailByOrderId(orderId);
            if (repairOrder != null) {
                List<RepairFile> files = repairMapper.getRepairFilesByOrderId(orderId);
                repairOrder.setFiles(files);
            }
            return repairOrder;
        }
    }

    public void cancelRepair(Integer orderId) {
        repairMapper.cancelRepair(orderId);
        // 清除相关缓存
        redisTemplate.delete("repair:detail:" + orderId);
        // 清除维修单列表缓存（清除常见的前几页）
        clearRepairListCache();
    }
    
    /**
     * 多线程并行清除维修单列表缓存
     */
    private void clearRepairListCache() {
        // 使用并行流多线程清除缓存
        IntStream.rangeClosed(1, 10).parallel().forEach(page -> {
            IntStream.range(10, 51).filter(size -> size % 10 == 0).parallel().forEach(size -> {
                redisTemplate.delete("repair:all:" + page + ":" + size);
                // 注意：由于status字段值不同，无法精确清除所有状态缓存，缓存会自然过期
            });
        });
    }
    
    private void clearUserRepairListCache(Integer userId) {
        // 清除用户相关的维修单列表缓存
        try {
            java.util.Set<String> keys = redisTemplate.keys("Repair:MyRepair:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PageInfo<RepairOrder> allRepair(Integer page, Integer size) {
        String cacheKey = "repair:all:" + page + ":" + size;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<RepairOrder>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(page, size);
                List<RepairOrder> list = repairMapper.allRepair();
                PageInfo<RepairOrder> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 维修单列表缓存10分钟
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
                    return (PageInfo<RepairOrder>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(page, size);
            List<RepairOrder> list = repairMapper.allRepair();
            return PageInfo.of(list);
        }
    }

    public PageInfo<RepairOrder> status(String status, Integer page, Integer size) {
        String cacheKey = "repair:status:" + status + ":" + page + ":" + size;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<RepairOrder>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(page, size);
                List<RepairOrder> list = repairMapper.status(status);
                PageInfo<RepairOrder> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 按状态查询的维修单列表缓存5分钟
                    long expireMinutes = 3 + (long) (Math.random() * 4);
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
                    return (PageInfo<RepairOrder>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(page, size);
            List<RepairOrder> list = repairMapper.status(status);
            return PageInfo.of(list);
        }
    }

    public Long countByStatus(String status) {
        return repairMapper.countByStatus(status);
    }



    public String dispatchOrder(RepairAssignment repairAssignment) {
        if (repairAssignment == null || repairAssignment.getOrderId() == null || repairAssignment.getWorkerId() == null) {
            return "参数错误";
        }
        String currentStatus = repairMapper.selectStatusByOrderId(repairAssignment.getOrderId());
        if (currentStatus == null) {
            return "工单不存在";
        }
        if (!List.of("待处理", "已指派", "维修中").contains(currentStatus)) {
            return "当前状态不可更换人员";
        }
        String nextStatus = "待处理".equals(currentStatus) ? "已指派" : currentStatus;
        repairAssignment.setStatus("已指派");
        repairMapper.dispatchOrder(repairAssignment);
        repairMapper.updateAssignmentOnDispatch(repairAssignment.getOrderId(), repairAssignment.getWorkerId(), nextStatus);
        if (repairAssignment.getOrderId() != null) {
            redisTemplate.delete("repair:detail:" + repairAssignment.getOrderId());
            clearRepairListCache();
            clearAllStatusCaches();
        }
        return null;
    }

    public void modificationTime(RepairAssignment repairAssignment) {
        repairMapper.modificationTime(repairAssignment);
        // 清除相关缓存
        if (repairAssignment.getOrderId() != null) {
            redisTemplate.delete("repair:detail:" + repairAssignment.getOrderId());
            // 清除列表缓存（时间变化）
            clearRepairListCache();
        }
    }

    public void addRepairFiles(Long orderId, List<String> fileUrls, Long userId) {
        if (fileUrls == null || fileUrls.isEmpty()) return;
        for (String url : fileUrls) {
            String fileType = "image";
            if (url != null) {
                String lower = url.toLowerCase();
                int dotIndex = lower.lastIndexOf('.');
                String ext = dotIndex > -1 ? lower.substring(dotIndex + 1) : "";
                if (ext.equals("mp4") || ext.equals("mov") || ext.equals("avi") || ext.equals("webm") || ext.equals("m4v") || ext.equals("3gp") || ext.equals("mkv")) {
                    fileType = "video";
                } else if (!ext.isEmpty()) {
                    fileType = ext;
                }
            }
            RepairFile repairFile = new RepairFile();
            repairFile.setOrderId(orderId);
            repairFile.setUserID(userId);
            repairFile.setFileUrl(url);
            repairFile.setFileType(fileType);
            repairFile.setUploadedAt(LocalDateTime.now());
            repairMapper.addRepairFiles(repairFile);
        }
    }
    
    public void evaluateRepair(Long orderId, String evaluation, Integer rating) {
        repairMapper.evaluateRepair(orderId, evaluation, rating);
        // Clean cache
        redisTemplate.delete("repair:detail:" + orderId);
        clearRepairListCache();
    }
    
    public RepairEvaluation getEvaluationByOrderId(Long orderId) {
        return repairMapper.getEvaluationByOrderId(orderId);
    }
    
    public void replyEvaluation(Long orderId, String replyContent) {
        repairMapper.replyEvaluation(orderId, replyContent);
        // 清缓存以刷新详情显示回复
        redisTemplate.delete("repair:detail:" + orderId);
    }

    public List<Employee> findWorkers(Long categoryId) {
        String cacheKey = "repair:workers:" + categoryId;
        String lockKey = "lock:" + cacheKey;

        // 先查缓存，处理可能的序列化异常
        Object cache = null;
        try {
            cache = redisTemplate.opsForValue().get(cacheKey);
            if (cache != null) {
                return (List<Employee>) cache;
            }
        } catch (Exception e) {
            // 序列化异常，清除缓存
            redisTemplate.delete(cacheKey);
            cache = null;
        }

        // 未命中缓存，加锁查询
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                List<Employee> list = repairMapper.findWorkers(categoryId);
                // 维修工人列表缓存30分钟
                if (list != null && !list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, list, 30, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, List.of(), 5, TimeUnit.MINUTES);
                }
                return list;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                // 再次尝试读取缓存，处理可能的序列化异常
                try {
                    cache = redisTemplate.opsForValue().get(cacheKey);
                    if (cache != null) {
                        return (List<Employee>) cache;
                    }
                } catch (Exception e) {
                    // 序列化异常，清除缓存
                    redisTemplate.delete(cacheKey);
                    cache = null;
                }
            } catch (InterruptedException ignored) {
            }
            return repairMapper.findWorkers(categoryId);
        }
    }

    public void updateStatus(Long orderId, String status) {
        String finishedTime = null;
        if ("已完成".equals(status)) {
            finishedTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        }
        repairMapper.updateStatus(orderId, status, finishedTime);
        // 清除相关缓存，确保列表与详情实时更新
        if (orderId != null) {
            redisTemplate.delete("repair:detail:" + orderId);
            clearRepairListCache();
            clearAllStatusCaches();
        }
    }

    // 查询分配给指定员工的工单
    public PageInfo<RepairOrder> selectByWorkerId(Integer pageNum, Integer pageSize, Long workerId, String status) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(repairMapper.selectByWorker(workerId, status));
    }
    
    public void acceptByWorker(Long orderId, Long workerId) {
        // 更新订单的维修人员
        repairMapper.acceptByWorker(orderId, workerId);
        // 同步或生成指派记录
        RepairAssignment current = repairMapper.selectAssignmentByOrderId(orderId);
        if (current == null) {
            repairMapper.insertAssignmentOnAccept(orderId, workerId);
        } else {
            repairMapper.updateAssignmentOnAccept(orderId, workerId);
        }
        // 清缓存
        redisTemplate.delete("repair:detail:" + orderId);
        clearRepairListCache();
        clearAllStatusCaches();
    }
    
    private void clearAllStatusCaches() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("repair:status:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
