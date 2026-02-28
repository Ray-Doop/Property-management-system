package com.example.modules.business.notice.service;

import com.example.common.Result;
import com.example.entity.Employee;
import com.example.entity.Forum.ForumPost;
import com.example.entity.User;
import com.example.entity.notice.*;
import com.example.modules.business.notice.mapper.NoticeMapper;
import com.example.modules.auth.service.LoginRegisterService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 公告业务逻辑层
 */
@Service
public class NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;
    
    @Autowired
    private LoginRegisterService loginRegisterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final AtomicLong idGenerator = new AtomicLong(1);
    public PageInfo<Notice> selectAllNotice(Notice notice, Integer page, Integer size) {
        StringBuilder conditionBuilder = new StringBuilder();
        if (notice != null) {
            if (notice.getTitle() != null) conditionBuilder.append(notice.getTitle());
            if (notice.getStatus() != null) conditionBuilder.append(":st").append(notice.getStatus());
            if (notice.getCurrentUserId() != null) conditionBuilder.append(":uid").append(notice.getCurrentUserId());
            if (notice.getQueryReadStatus() != null) conditionBuilder.append(":rs").append(notice.getQueryReadStatus());
            if (notice.getPublisherId() != null) conditionBuilder.append(":pid").append(notice.getPublisherId());
            if (notice.getPublisherType() != null) conditionBuilder.append(":pt").append(notice.getPublisherType());
        }
        String conditionKey = conditionBuilder.toString();
        
        String cacheKey = "notice:all:" + conditionKey.hashCode() + ":" + page + ":" + size;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<Notice>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(page, size);
                List<Notice> list = noticeMapper.selectAllNotice(notice);
                PageInfo<Notice> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(Collections.emptyList()), 2, TimeUnit.MINUTES);
                } else {
                    // 公告列表缓存10分钟
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
                    return (PageInfo<Notice>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(page, size);
            List<Notice> list = noticeMapper.selectAllNotice(notice);
            return PageInfo.of(list);
        }
    }

    public Long publish(Notice notice) {
        boolean isEmployee = "EMPLOYEE".equalsIgnoreCase(notice.getPublisherType());
        boolean isScheduled = isFuturePublishTime(notice.getPublishTime());
        if (isEmployee) {
            notice.setStatus(2);
        } else if (isScheduled) {
            notice.setStatus(0);
            notice.setPublisherType("ADMIN");
        } else {
            notice.setStatus(1);
            notice.setPublisherType("ADMIN");
        }
        noticeMapper.publish(notice);
        Long NoticeId = notice.getNoticeId();
        System.out.println("生成的 noticeId: " + notice.getNoticeId());
        
        if (notice.getStatus() != null && notice.getStatus() == 1) {
            noticeMapper.initReadRecords(NoticeId);
        }

        // 清除公告列表缓存（清除常见的前几页）
        clearNoticeListCache();
        return NoticeId;
    }

    public Long saveDraft(Notice notice) {
        noticeMapper.saveDraft(notice);
        Long noticeId = notice.getNoticeId();
        System.out.println("保存草稿 noticeId: " + noticeId);
        // 清除公告列表缓存
        clearNoticeListCache();
        return noticeId;
    }

    public Notice selectById(Long noticeId) {
        return noticeMapper.selectById(noticeId);
    }
    
    public java.util.Map<String, Object> readStatus(Long noticeId) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("read", noticeMapper.selectReadList(noticeId));
        map.put("unread", noticeMapper.selectUnreadList(noticeId));
        return map;
    }

    public com.github.pagehelper.PageInfo<java.util.Map<String, Object>> summaryPage(String title, Integer page, Integer size) {
        com.github.pagehelper.PageHelper.startPage(page, size);
        java.util.List<java.util.Map<String, Object>> rows = noticeMapper.selectSummary(title);
        return com.github.pagehelper.PageInfo.of(rows);
    }
    
    public com.github.pagehelper.PageInfo<java.util.Map<String, Object>> readTable(Long noticeId, Integer readStatus, String keyword, Integer page, Integer size) {
        com.github.pagehelper.PageHelper.startPage(page, size);
        java.util.List<java.util.Map<String, Object>> rows = noticeMapper.selectReadTable(noticeId, readStatus, keyword);
        return com.github.pagehelper.PageInfo.of(rows);
    }
    
    public void updateReadStatus(Long noticeId, String residenceId, Integer readStatus) {
        noticeMapper.updateReadStatus(noticeId, residenceId, readStatus);
        clearNoticeListCache();
    }

    public void markAsRead(Long noticeId, Long userId) {
        User user = loginRegisterService.selectUserData(userId);
        if (user != null && user.getResidenceId() != null) {
            noticeMapper.insertReadRecord(noticeId, user.getResidenceId());
            // Clear cache to reflect read status change
            clearNoticeListCache();
        }
    }
    
    private void clearNoticeListCache() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("notice:all:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAttachment(Notice notice) {

        for (NoticeAttachment att : notice.getAttachments()) {
            att.setNotice(notice);
            att.setNoticeId(notice.getNoticeId()); // 新增字段
            Long noticeId = att.getNoticeId(); // 直接获取
            String url = att.getFileUrl();
            String ext = "";
            int index = url.lastIndexOf('.');
            if (index != -1 && index < url.length() - 1) {
                ext = url.substring(index + 1); // png / jpg / ...
            }
            att.setFileType(ext);
        }
        noticeMapper.insertAttachments(notice.getAttachments());
        // 清除公告列表缓存（附件变化）
        clearNoticeListCache();
    }
    
    public void deleteNotice(Long noticeId) {
        // 依次删除关联数据，确保每个SQL语句单独执行
        noticeMapper.deleteNoticeAttachments(noticeId); // 删除公告附件
        noticeMapper.deleteNoticeReadRecords(noticeId); // 删除公告阅读记录
        noticeMapper.deleteNotice(noticeId); // 删除公告本身
        // 清除公告列表缓存
        clearNoticeListCache();
    }
    
    public void audit(Long noticeId, Integer status) {
        noticeMapper.updateStatus(noticeId, status);
        if (status == 1) {
            noticeMapper.initReadRecords(noticeId);
        }
        clearNoticeListCache();
    }

    public void publishDueDrafts() {
        List<Long> dueIds = noticeMapper.selectDueDraftNoticeIds();
        if (dueIds == null || dueIds.isEmpty()) {
            return;
        }
        for (Long noticeId : dueIds) {
            noticeMapper.updateStatus(noticeId, 1);
            noticeMapper.initReadRecords(noticeId);
        }
        clearNoticeListCache();
    }

    private boolean isFuturePublishTime(String publishTime) {
        if (publishTime == null || publishTime.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime time = LocalDateTime.parse(publishTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return time.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("total", noticeMapper.countTotal());
        map.put("todayPublished", noticeMapper.countTodayPublished());
        map.put("pendingAudit", noticeMapper.countPendingAudit());
        return map;
    }
}
