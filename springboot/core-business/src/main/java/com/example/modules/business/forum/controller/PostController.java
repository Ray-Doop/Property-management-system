package com.example.modules.business.forum.controller;

import com.example.common.Result;
import com.example.entity.Forum.CommentWithPostDTO;
import com.example.entity.Forum.ForumComment;
import com.example.entity.Forum.ForumPost;
import com.example.entity.Forum.ForumSection;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.modules.auth.service.LoginRegisterService;
import com.example.modules.business.forum.service.PostService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/Forum")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private LoginRegisterService loginRegisterService;

    //Post分页
    @GetMapping("/SelectPage")
    public Result selectPage(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("2222");
        postService.updatePostWithSectionName();
        postService.synchronous();
        PageInfo<ForumPost> pageInfo = postService.selectPage(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //新政帖子
    @PostMapping("/Post")
    public Result post(@RequestBody ForumPost forumPost) {
        System.out.println(forumPost);
        assertUserCanPost("发帖");
        // 注入作者身份标签
        String authorType = "业主";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.example.entity.Employee) {
                authorType = "物业员工";
                forumPost.setUserId(resolveUserIdForPrincipal(principal));
            } else if (principal instanceof com.example.entity.Admin) {
                authorType = "管理员";
                forumPost.setUserId(resolveUserIdForPrincipal(principal));
            }
        }
        forumPost.setAuthorType(authorType);
        Long postId = postService.addPost(forumPost);
        if (forumPost.getImages() != null) {
            postService.addAttachment(forumPost); // 使用已赋值的 postId
        }
        System.out.println(postId);
        return Result.success(postId);
    }

    //查询帖子详情
    @GetMapping("/PostDetail/{postId}") // 确保占位符名称与参数名一致
    public Result PostDetail(@PathVariable Integer postId) { // 参数名改为 postId
        ForumPost db = postService.postDetail(postId); // 使用前端传递的 postId
        return Result.success(db);
    }

    //评论
    @PostMapping("/addPostComment")
    public Result addPostComment(@RequestBody ForumComment forumComment) {
        assertUserCanPost("评论");
        // 注入作者身份标签
        String authorType = "业主";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.example.entity.Employee) {
                authorType = "物业员工";
                forumComment.setUserId(resolveUserIdForPrincipal(principal));
            } else if (principal instanceof com.example.entity.Admin) {
                authorType = "管理员";
                forumComment.setUserId(resolveUserIdForPrincipal(principal));
            }
        }
        forumComment.setAuthorType(authorType);
        postService.addPostComment(forumComment);
        return Result.success();
    }
    
    @DeleteMapping("/delComment/{commentId}")
    public Result delComment(@PathVariable Integer commentId) {
        postService.delMyPostComment(commentId);
        return Result.success();
    }
    
    /**
     * 根据鉴权主体解析作者用户ID，镜像用户由认证模块统一维护
     */
    private Long resolveUserIdForPrincipal(Object principal) {
        if (principal instanceof com.example.entity.User u) {
            return u.getUserId();
        }
        if (principal instanceof com.example.entity.Employee emp) {
            User shadow = loginRegisterService.getOrCreateShadowUserForEmployee(emp);
            return shadow != null ? shadow.getUserId() : null;
        }
        if (principal instanceof com.example.entity.Admin adm) {
            User shadow = loginRegisterService.getOrCreateShadowUserForAdmin(adm);
            return shadow != null ? shadow.getUserId() : null;
        }
        return null;
    }

    private void assertUserCanPost(String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof com.example.entity.User)) {
            return;
        }
        User current = resolveCurrentUser((com.example.entity.User) principal);
        if (current == null || current.getStatus() == null) {
            return;
        }
        int status = current.getStatus();
        if (status == 2) {
            throw new CustomException("403", "账号已被禁言，无法" + action);
        }
        if (status == 0) {
            throw new CustomException("403", "账号未激活，无法" + action);
        }
        if (status == 3) {
            throw new CustomException("403", "账号已被封禁，无法" + action);
        }
        if (status == 4) {
            throw new CustomException("403", "账号审核未通过，无法" + action);
        }
    }

    private User resolveCurrentUser(com.example.entity.User principal) {
        if (principal == null || principal.getUserId() == null) {
            return principal;
        }
        User dbUser = loginRegisterService.selectUserData(principal.getUserId());
        if (dbUser != null && dbUser.getUserId() != null) {
            return dbUser;
        }
        return principal;
    }
    
    @PostMapping("/likeComment")
    public Result likeComment(@RequestParam Long commentId, @RequestParam Long userId) {
        java.util.Map<String, Object> res = postService.toggleLikeComment(commentId, userId);
        return Result.success(res);
    }

    //我的收藏
    @GetMapping("/SelectPageCollect")
    public Result selectPageCollect(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        System.out.println("222222222222222222222222222222222222222222");
        System.out.println(forumPost.getUserId());
        PageInfo<ForumPost> pageInfo = postService.selectPageCollect(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //最热帖子
    @GetMapping("/SelectPageHot")
    public Result selectPageHot(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("11111111111111111111111111111111111111111111111111111111111");
        PageInfo<ForumPost> pageInfo = postService.selectPageHot(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //最多收藏
    @GetMapping("/selectPageHotByCollect")
    public Result selectPageHotByCollect(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("11111111111111111111111111111111111111111111111111111111111");
        postService.updatePostCollectCount();
        PageInfo<ForumPost> pageInfo = postService.selectPageHotByCollect(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //搜索
    @GetMapping("/SelectByTitle")
    public Result SelectByTitle(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("11111111111111111111111111111111111111111111111111111111111");
        PageInfo<ForumPost> pageInfo = postService.SelectByTitle(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @GetMapping("/Sections")
    public Result Sections() {
        List<ForumSection> list = postService.selectSections();
        return Result.success(list);
    }

    //分类
    @GetMapping("/SwitchSection")
    public Result SwitchSection(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("11111111111111111111111111111111111111111111111111111111111");
        PageInfo<ForumPost> pageInfo = postService.SwitchSection(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //我的帖子
    @GetMapping("/showMyPost")
    public Result showMyPost(
            ForumPost forumPost,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        System.out.println("11111111111111111111111111111111111111111111111111111111111");
        PageInfo<ForumPost> pageInfo = postService.showMyPost(forumPost, pageNum, pageSize);
        return Result.success(pageInfo);
    }
    //删除收藏

    @DeleteMapping("/delCancelCollect")
    public Result delCancelCollect(@RequestBody ForumPost forumPost) {
        postService.delCancelCollect(forumPost);
        return Result.success();
    }

    //删除帖子
    @DeleteMapping("/delMyPost")
    public Result delMyPost(@RequestParam Integer postId) {
        postService.delMyPost(postId);
        return Result.success();
    }

    //收藏
    @PostMapping("/addToCollection")
    public Result addToCollection(@RequestBody ForumPost forumPost) {
        postService.addToCollection(forumPost);
        return Result.success();
    }

    //查看是否收藏
    @GetMapping("/isCollected")
    public Result isCollected(@RequestParam Long postId, @RequestParam Long userId) {
        System.out.println(postId);
        System.out.println(userId);
        boolean collected = postService.isCollected(postId, userId);
        return Result.success(collected);
    }

    //查找作者数据
    @GetMapping("/selectUserData")
    public Result selectUserData(@RequestParam Long userId) {
        System.out.println(userId);
        System.out.println("====================================================================================================");
        User user = loginRegisterService.selectUserData(userId);
        System.out.println("==============================1111111111111111111111111111111111111111111=====================================");
        System.out.println(user);
        return Result.success(user);
    }

    //查找我的评论
    @GetMapping("/SelectMyPostComment")
    public Result SelectMyPostComment(ForumPost forumPost,
                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = forumPost.getUserId();
        PageInfo<CommentWithPostDTO> pageInfo = postService.SelectMyPostCommentt(userId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    //删除评论
    @DeleteMapping("/deleteComment/{commentId}")
    public Result delMyPostComment(@PathVariable("commentId") Integer commentId) {
        System.out.println("-------------------");
        System.out.println(commentId);
        postService.delMyPostComment(commentId);
        return Result.success();
    }

    //所有评论
    @GetMapping("/SelectAllComment")
    public Result SelectAllComment(
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {


        PageInfo<CommentWithPostDTO> pageInfo = postService.SelectAllComment( pageNum, pageSize);
        return Result.success(pageInfo);
    }
//置顶
    @PostMapping("/top")
    public Result top(@RequestParam Integer postId) {
        postService.top(postId);
        return Result.success();
    }
    //加精
    @PostMapping("/exquisite")
    public Result exquisite(@RequestParam Integer postId) {
        postService.exquisite(postId);
        return Result.success();
    }
    //取消加精
    @PostMapping("/cancelExquisite")
    public Result cancelExquisite(@RequestParam Integer postId) {
        postService.cancelExquisite(postId);
        return Result.success();
    }
    //取消置顶
    @PostMapping("/cancelTop")
    public Result cancelTop(@RequestParam Integer postId) {
        postService.cancelTop(postId);
        return Result.success();
    }

}
