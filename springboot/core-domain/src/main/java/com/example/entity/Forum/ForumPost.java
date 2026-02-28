package com.example.entity.Forum;

import com.example.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.awt.*;
import java.util.Date;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ForumPost {
    private Long postId;
    private Integer sectionId;
    private String title;
    private String content;
    private Long userId;
    private Byte isTop;
    private Byte isEssence;
    private Byte status;
    private Integer collectCount;
    private Integer commentCount;
    private Date createdTime;
    private Date updatedTime;
    private Date lastCommentTime;
    private String sectionName;
    private String sectionDesc;
    private Integer sortOrder;
    private Long createdBy;
    private Long commentId;
    private Long parentId;
    private Integer likeCount;
    private Long attachmentId;
    private List<Image> images;
    private List<ForumAttachment> attachments;
    // 在ForumPost类中添加
    private List<ForumComment> comments;
    private String nickname; // 发帖人昵称
    private String avatarUrl;
    private User author;
    private String authorType; // 作者身份标签：业主/物业员工/管理员
    private String username; // 作者用户名（用于判断身份来源，如 emp_ / admin_ 前缀）

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
    public String getAuthorType() { return authorType; }
    public void setAuthorType(String authorType) { this.authorType = authorType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    // 添加getter/setter
    public List<ForumComment> getComments() {
        return comments;
    }

    public void setComments(List<ForumComment> comments) {
        this.comments = comments;
    }
    public List<ForumAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ForumAttachment> attachments) {
        this.attachments = attachments;
    }

    public static class Image {
        private String url; // 字段名必须与前端 JSON 的 "url" 完全一致

        // 无参构造函数（Jackson 反序列化必须！）
        public Image() {}

        // getter 和 setter（必须！否则 Jackson 无法赋值）
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }

        // 重写 toString()
        @Override
        public String toString() {
            return "Image{url='" + url + "'}";
        }
    }

        @Override
    public String toString() {
        return "ForumPost{" +
                "postId=" + postId +
                ", sectionId=" + sectionId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", userId=" + userId +
                ", isTop=" + isTop +
                ", isEssence=" + isEssence +
                ", status=" + status +
                ", collectCount=" + collectCount +
                ", commentCount=" + commentCount +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                ", lastCommentTime=" + lastCommentTime +
                ", sectionName='" + sectionName + '\'' +
                ", sectionDesc='" + sectionDesc + '\'' +
                ", sortOrder=" + sortOrder +
                ", createdBy=" + createdBy +
                ", commentId=" + commentId +
                ", parentId=" + parentId +
                ", likeCount=" + likeCount +
                ", attachmentId=" + attachmentId +
                ", images=" + images +
                '}';
    }


    public List<Image> getImages() {
        if (images == null && attachments != null) {
            images = new java.util.ArrayList<>();
            for (ForumAttachment attachment : attachments) {
                Image img = new Image();
                img.setUrl(attachment.getUrl() != null ? attachment.getUrl() : attachment.getFilePath());
                images.add(img);
            }
        }
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionDesc() {
        return sectionDesc;
    }

    public void setSectionDesc(String sectionDesc) {
        this.sectionDesc = sectionDesc;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
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

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Byte getIsTop() {
        return isTop;
    }

    public void setIsTop(Byte isTop) {
        this.isTop = isTop;
    }

    public Byte getIsEssence() {
        return isEssence;
    }

    public void setIsEssence(Byte isEssence) {
        this.isEssence = isEssence;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }


    public Integer getCollectCount() {
        return collectCount;
    }

    public void setCollectCount(Integer collectCount) {
        this.collectCount = collectCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Date getLastCommentTime() {
        return lastCommentTime;
    }

    public void setLastCommentTime(Date lastCommentTime) {
        this.lastCommentTime = lastCommentTime;
    }

    // Getters and Setters
}
