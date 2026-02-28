package com.example.modules.business.travelpass.service;

import com.example.entity.TravelPassRecord;
import com.example.modules.business.travelpass.mapper.TravelPassRecordMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

//import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TravelPassServiceImpl implements TravelPassService {

    @Resource
    private TravelPassRecordMapper recordMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public TravelPassRecord createRecord(TravelPassRecord record) {
        recordMapper.insert(record);
        // 清除相关缓存（如果记录有ID）
        if (record.getId() != null) {
            redisTemplate.delete("travel:pass:" + record.getId());
        }
        return record;
    }

    @Override
    public TravelPassRecord findById(Long id) {
        String cacheKey = "travel:pass:" + id;
        String lockKey = "lock:" + cacheKey;
        
        // 先查缓存
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null && cache instanceof TravelPassRecord) {
            TravelPassRecord cachedRecord = (TravelPassRecord) cache;
            if (cachedRecord.getId() != null) {
                return cachedRecord;
            }
            return null;
        }
        
        // 未命中缓存，加锁查询
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                TravelPassRecord record = recordMapper.findById(id);
                if (record != null) {
                    // 通行记录缓存15分钟
                    redisTemplate.opsForValue().set(cacheKey, record, 15, TimeUnit.MINUTES);
                } else {
                    // 缓存空对象，5分钟过期
                    redisTemplate.opsForValue().set(cacheKey, new TravelPassRecord(), 5, TimeUnit.MINUTES);
                }
                return record;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 未拿到锁，等待后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null && cache instanceof TravelPassRecord) {
                    TravelPassRecord cachedRecord = (TravelPassRecord) cache;
                    if (cachedRecord.getId() != null) {
                        return cachedRecord;
                    }
                    return null;
                }
            } catch (InterruptedException ignored) {
            }
            return recordMapper.findById(id);
        }
    }

    @Override
    public void markEntered(Long id, Long employeeId) {
        recordMapper.updateStatusAndEntryTime(id, new Date(), employeeId);
        // 清除相关缓存
        redisTemplate.delete("travel:pass:" + id);
    }

    @Override
    public void markExited(Long id, double fee, Long employeeId) {
        recordMapper.updateExit(id, new Date(), fee, "EXITED", employeeId);
        // 清除相关缓存
        redisTemplate.delete("travel:pass:" + id);
    }
    
    @Override
    public PageInfo<TravelPassRecord> selectPage(Integer pageNum, Integer pageSize, String status, String username) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(recordMapper.selectPage(status, username));
    }
    
    @Override
    public PageInfo<TravelPassRecord> selectMyRecords(Integer pageNum, Integer pageSize, Long userId) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(recordMapper.selectByUserId(userId));
    }
    
    @Override
    public void markExpired(Long id) {
        recordMapper.updateExpired(id);
        // 清除相关缓存
        redisTemplate.delete("travel:pass:" + id);
    }
}
