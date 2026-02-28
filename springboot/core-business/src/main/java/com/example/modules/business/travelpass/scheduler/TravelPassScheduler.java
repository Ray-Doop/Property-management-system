package com.example.modules.business.travelpass.scheduler;

import com.example.modules.business.travelpass.mapper.TravelPassRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TravelPassScheduler {
    @Resource
    private TravelPassRecordMapper travelPassRecordMapper;

    /**
     * 每60秒检查一次过期的出行码，并更新状态为EXPIRED
     */
    @Scheduled(fixedDelay = 60000)
    public void updateExpiredPasses() {
        travelPassRecordMapper.updateExpiredPasses();
    }
}
