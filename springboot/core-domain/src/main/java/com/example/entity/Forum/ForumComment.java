package com.example.entity.Forum;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"handler"})
public class ForumComment {
    private Long commentId;
    private Long postId;
    private String content;
    private Long userId;
    private Long parentId;
    private Byte status;
    private Integer likeCount;
    private Date createdTime;
    private Date updatedTime;

    // 新增：评论用户的昵称
    private String nickname;

    // 新增：评论用户的头像地址
    private String avatarUrl;
    private String authorType; // 作者身份标签：业主/物业员工/管理员
    private String username; // 评论用户的用户名（emp_/admin_ 前缀判断）

    // 新增：子评论列表（楼中楼）
    private List<ForumComment> replies;
    // 新增：评论图片附件
    private java.util.List<ForumAttachment> attachments;
    // 新增：前端提交的图片对象（{url}）
    private java.util.List<com.example.entity.Forum.ForumPost.Image> images;

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
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
    public String getAuthorType() { return authorType; }
    public void setAuthorType(String authorType) { this.authorType = authorType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<ForumComment> getReplies() {
        return replies;
    }

    public void setReplies(List<ForumComment> replies) {
        this.replies = replies;
    }
    public java.util.List<ForumAttachment> getAttachments() { return attachments; }
    public void setAttachments(java.util.List<ForumAttachment> attachments) { this.attachments = attachments; }
    public java.util.List<com.example.entity.Forum.ForumPost.Image> getImages() { return images; }
    public void setImages(java.util.List<com.example.entity.Forum.ForumPost.Image> images) { this.images = images; }
}
