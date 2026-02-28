package com.example.entity.notice;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "notice_attachment")
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attachmentId;      // 附件唯一ID

    private String fileUrl;         // 文件URL地址（OSS或本地路径）

    private String fileType;        // 文件类型（image/video/file）

    private String uploadTime;      // 上传时间（yyyy-MM-dd HH:mm:ss）

    // 多对一：多个附件对应一个公告
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    @ToString.Exclude  // 避免打印时进入附件列表
    private Notice notice;
    @Transient
    private Long noticeId;

    // 只读辅助方法，直接从 notice 对象获取 ID
    public Long getNoticeId() {
        return notice != null ? notice.getNoticeId() : null;
    }
}
