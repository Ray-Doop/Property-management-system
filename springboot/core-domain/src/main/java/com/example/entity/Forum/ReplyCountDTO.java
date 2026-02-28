package com.example.entity.Forum;

import lombok.Data;

// 新建 ReplyCountDTO.java
@Data
public class ReplyCountDTO {
    private Long parentId;    // 对应数据库的 parent_id
    private Integer replyCount;
    private Integer maxDepth;
}