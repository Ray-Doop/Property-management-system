package com.example.modules.business.notice.mapper;

import com.example.entity.notice.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NoticeMapper {

    List<Notice> selectAllNotice(Notice notice);

    void publish(Notice notice);

    void saveDraft(Notice notice);

    Notice selectById(Long noticeId);

    void insertAttachments(List<NoticeAttachment> attachments);

    void publishDueDrafts();

    List<Long> selectDueDraftNoticeIds();

    List<NoticeRead> selectReadList(Long noticeId);

    List<NoticeRead> selectUnreadList(Long noticeId);

    void insertReadRecord(Long noticeId, String residenceId);
    
    List<java.util.Map<String, Object>> selectSummary(String title);
    
    java.util.List<java.util.Map<String, Object>> selectReadTable(Long noticeId, Integer readStatus, String keyword);
    
    void updateStatus(@org.apache.ibatis.annotations.Param("noticeId") Long noticeId, @org.apache.ibatis.annotations.Param("status") Integer status);
    
    void updateReadStatus(Long noticeId, String residenceId, Integer readStatus);

    void initReadRecords(Long noticeId);
    
    void deleteNoticeAttachments(Long noticeId);
    
    void deleteNoticeReadRecords(Long noticeId);
    
    void deleteNotice(Long noticeId);

    long countTotal();

    long countTodayPublished();

    long countPendingAudit();
}
