package com.example.entity.notice;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 公告展示对象（VO：View Object）
 * 用于前端展示公告时携带更多信息（例如已读数、附件等）
 */
@Data
public class NoticeVO {

    private Long noticeId;         // 公告ID
    private String title;          // 公告标题
    private String content;        // 公告内容（HTML）
    private String scopeType;      // 发布范围 ALL/BUILDING/UNIT/ROOM
    private Integer buildingNo;    // 楼栋号
    private Integer unitNo;        // 单元号
    private Integer roomNo;        // 房间号

    private Long adminId;          // 发布管理员ID
    private String adminName;      // 发布管理员用户名
    private Date createTime;       // 发布时间

    // 统计字段
    private Integer readCount;     // 已读人数
    private Integer unreadCount;   // 未读人数

    // 附件
    private List<String> fileUrls; // 附件URL列表

    // getter / setter
    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Integer getBuildingNo() {
        return buildingNo;
    }

    public void setBuildingNo(Integer buildingNo) {
        this.buildingNo = buildingNo;
    }

    public Integer getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(Integer unitNo) {
        this.unitNo = unitNo;
    }

    public Integer getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(Integer roomNo) {
        this.roomNo = roomNo;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }
}
