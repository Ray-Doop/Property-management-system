package com.example.entity.notice;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "notice_read",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"notice_id", "residence_id"})}
)
public class NoticeRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;               // 主键

    @Column(name = "residence_id", nullable = false)
    private String residenceId;    // 住户唯一ID（每户）

    private Integer readStatus;    // 阅读状态：0-未读，1-已读

    private String readTime;       // 阅读时间（yyyy-MM-dd HH:mm:ss）

    // 多对一：多个阅读记录对应一个公告
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;
}
