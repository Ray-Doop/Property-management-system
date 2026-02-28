package com.example.modules.business.forum.mapper;

import com.example.entity.Forum.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    List<ForumPost> selectPostAll(ForumPost forumPost);

    void addPost(ForumPost forumPost);

    void batchInsertAttachments(@Param("list") List<ForumAttachment> attachments);


    void synchronous();

    ForumPost postDetail(Integer postId);

    void addPostComment(ForumComment forumComment);
    void batchInsertCommentAttachments(@org.apache.ibatis.annotations.Param("list") java.util.List<ForumAttachment> attachments);
    com.example.entity.Forum.ForumComment selectCommentById(@org.apache.ibatis.annotations.Param("commentId") Long commentId);
    void updateCommentLikeCount(@org.apache.ibatis.annotations.Param("commentId") Long commentId,
                                @org.apache.ibatis.annotations.Param("delta") Integer delta);

    List<ForumPost> selectPageCollect(ForumPost forumPost);

    List<ForumPost> selectPageHot(ForumPost forumPost);

    void updatePostCollectCount();

    List<ForumPost> selectPageHotByCollect(ForumPost forumPost);

    List<ForumPost> SelectByTitle(ForumPost forumPost);

    List<ForumPost> showMyPost(ForumPost forumPost);

    void delCancelCollect(ForumPost forumPost);

    void delMyPost(Integer postId);

    void updatePostWithSectionName();

    List<ForumPost> SwitchSectione(ForumPost forumPost);

    void addToCollection(ForumPost forumPost);

    // 返回整数，而不是 boolean
    Integer existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    List<ForumSection> selectSections();

    List<CommentWithPostDTO> SelectMyPostCommentt(@Param("userId") Long userId);




    void updateChildrenParentId(Integer commentId);
    
    void delMyPostComment(Integer commentId);

    List<CommentWithPostDTO> SelectAllComment();

    void top(Integer postId);
    void exquisite(Integer postId);
    void cancelTop(Integer postId);
    void cancelExquisite(Integer postId);
    
    void upsertReplyCountCache(@Param("commentId") Long commentId);


}
