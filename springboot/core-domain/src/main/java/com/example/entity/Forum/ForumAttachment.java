package com.example.entity.Forum; // 或 com.example.entity（建议放在实体类包）

public class ForumAttachment {
    private Long attachmentId;          // 自增主键（可选，如果数据库有自增 ID）
    private Long postId;      // 关联的帖子 ID
    private Long commentId;   // 关联的评论 ID
    private String url;       // 图片 URL
    private String type;      // 文件类型（如 "png", "jpg"）
    private Long userId;
    private String filePath;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // 必须提供无参构造方法（MyBatis 需要）
    public ForumAttachment() {}

    // 可选：提供带参构造方法（方便测试）
    public ForumAttachment(Long postId, String url, String type) {
        this.postId = postId;
        this.url = url;
        this.type = type;
    }

    // Getter 和 Setter 方法（必须提供，MyBatis 依赖它们进行映射）

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
