package com.example.modules.business.travelpass.service;

import com.example.entity.TravelPassRecord;

public interface TravelPassService {
    TravelPassRecord createRecord(TravelPassRecord record);

    TravelPassRecord findById(Long id);

    void markEntered(Long id, Long employeeId);

    void markExited(Long id, double fee, Long employeeId);
    
    com.github.pagehelper.PageInfo<TravelPassRecord> selectPage(Integer pageNum, Integer pageSize, String status, String username);
    
    com.github.pagehelper.PageInfo<TravelPassRecord> selectMyRecords(Integer pageNum, Integer pageSize, Long userId);
    
    void markExpired(Long id);
}
