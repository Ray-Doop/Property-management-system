package com.example.entity.notice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Entity
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;           // 公告唯一ID

    private String title;            // 公告标题

    @Column(columnDefinition = "TEXT")
    private String content;          // 公告内容

    private String publishTime;      // 公告发布时间（yyyy-MM-dd HH:mm:ss）

    private Long publisherId;        // 发布人ID
    
    private String publisherName;    // 发布者名称（管理员用户名或昵称）

    private String publisherType;    // 发布者类型：ADMIN/EMPLOYEE


    private String targetResidenceId; // 发布目标住户或小区（为空则全体）

    private Integer status;          // 公告状态：0-草稿，1-已发布，2-已撤回，3-已删除


    private String remark;           // 公告备注

    // 一对多：公告对应多个附件
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude  // 避免打印时进入附件列表
    private List<NoticeAttachment> attachments;

    // 一对多：公告对应多个阅读记录
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NoticeRead> readRecords;

    @Transient
    private Integer isRead; // 0-未读，1-已读（用于返回给前端）

    @Transient
    private Long currentUserId; // 用于查询时的当前用户ID

    @Transient
    private Integer queryReadStatus; // 用于查询时的筛选状态：0-未读，1-已读
}
