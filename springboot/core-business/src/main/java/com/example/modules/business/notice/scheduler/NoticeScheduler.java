package com.example.modules.business.notice.scheduler;

import com.example.modules.business.notice.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeScheduler {
    @Resource
    private NoticeService noticeService;

    @Scheduled(fixedDelay = 60000)
    public void publishDueDrafts() {
        noticeService.publishDueDrafts();
    }
}
