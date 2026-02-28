package com.example.entity.notice;

import java.util.Date;

/**
 * 公告附件实体类
 * 对应数据库表 notice_file
 */
public class NoticeFile {

    private Long fileId;       // 附件ID（主键）
    private Long noticeId;     // 公告ID（外键，关联 notice.notice_id）
    private String fileName;   // 原始文件名
    private String fileUrl;    // 文件存储路径或访问URL
    private String fileType;   // 文件类型（例如 pdf、jpg、docx）
    private Long fileSize;     // 文件大小（字节数）
    private Date uploadTime;   // 上传时间
    private Long uploaderId;   // 上传人ID（通常是管理员）

    // getter/setter
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }
}
