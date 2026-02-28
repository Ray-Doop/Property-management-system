package com.example.modules.business.travelpass.mapper;

import com.example.entity.TravelPassRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TravelPassRecordMapper {
    void insert(TravelPassRecord record);

    TravelPassRecord findById(@Param("id") Long id);

    void updateStatusAndEntryTime(@Param("id") Long id, @Param("entryTime") java.util.Date entryTime, @Param("employeeId") Long employeeId);

    void updateExit(@Param("id") Long id,
                    @Param("exitTime") java.util.Date exitTime,
                    @Param("fee") Double fee,
                    @Param("status") String status,
                    @Param("employeeId") Long employeeId);
    
    java.util.List<com.example.entity.TravelPassRecord> selectPage(@Param("status") String status,
                                                                   @Param("username") String username);
                                                                    
    java.util.List<com.example.entity.TravelPassRecord> selectByUserId(@Param("userId") Long userId);
    
    void updateExpired(@Param("id") Long id);

    /**
     * 更新过期的出行码状态
     */
    void updateExpiredPasses();
}
