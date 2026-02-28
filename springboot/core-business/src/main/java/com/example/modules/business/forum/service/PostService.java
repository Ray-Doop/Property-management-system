package com.example.modules.business.forum.service;

import com.example.entity.Forum.*;
import com.example.modules.business.forum.mapper.PostMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    @Qualifier("cacheCleanExecutor")
    private Executor cacheCleanExecutor;
    
    @Autowired
    @Qualifier("dataProcessExecutor")
    private Executor dataProcessExecutor;

    public PageInfo<ForumPost> selectPage(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        // ====== 1. 生成缓存Key ======
        // Key 尽量唯一，可包含查询条件、分页参数
        String conditionKey = generateStableKey(forumPost);
        String cacheKey = "forum:posts:" + pageNum + ":" + pageSize + ":" + conditionKey;
        String lockKey = "lock:" + cacheKey; // 分布式锁的key

        // ====== 2. 先查缓存 ======
        Object cacheData = redisTemplate.opsForValue().get(cacheKey);
        if (cacheData != null) {
            // 命中缓存（可能是空对象），直接返回
            return (PageInfo<ForumPost>) cacheData;
        }

        // ====== 3. 未命中缓存，尝试加锁防止击穿 ======
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // ====== 4. 查询数据库 ======
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.selectPostAll(forumPost);
                if (list != null) {
                    for (ForumPost fp : list) {
                        String un = fp.getUsername();
                        if (un != null) {
                            if (un.startsWith("emp_")) fp.setAuthorType("物业员工");
                            else if (un.startsWith("admin_")) fp.setAuthorType("管理员");
                            else fp.setAuthorType("业主");
                        } else {
                            fp.setAuthorType("业主");
                        }
                    }
                }
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                // ====== 5. 缓存结果（含空对象） ======
                if (list == null || list.isEmpty()) {
                    // 缓存空对象防止穿透
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 正常数据缓存，过期时间随机化防止雪崩
                    long expire = 5 + (long) (Math.random() * 5); // 5~10分钟
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expire, TimeUnit.MINUTES);
                }

                return pageInfo;

            } finally {
                // ====== 6. 释放锁 ======
                redisTemplate.delete(lockKey);
            }
        } else {
            // ====== 7. 未拿到锁，短暂等待再查缓存（防止同时查询数据库） ======
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }
            // 再查一次缓存（此时可能被其他线程写入了）
            Object retryCache = redisTemplate.opsForValue().get(cacheKey);
            if (retryCache != null) {
                return (PageInfo<ForumPost>) retryCache;
            }

            // 如果仍然没有缓存（极端情况），再从数据库查询（不建议频繁发生）
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.selectPostAll(forumPost);
            return PageInfo.of(list);
        }
    }

    public void add(ForumPost forumPost) {
    }

    public Long addPost(ForumPost forumPost) {
        postMapper.addPost(forumPost);
        Long PostId = forumPost.getPostId();
        System.out.println("生成的 postId: " + forumPost.getPostId());
        // 清除相关缓存
        if (forumPost.getUserId() != null) {
            // Redis不支持通配符删除，需要清除所有可能的列表缓存键
            // 这里清除常见的分页缓存（实际生产环境建议使用Redis Set记录缓存键）
            clearPostListCache(forumPost.getUserId());
        }
        // 清除帖子列表缓存（所有列表）
        clearAllPostListCache();
        return PostId;
    }
    
    /**
     * 多线程并行清除用户帖子列表缓存
     */
    private void clearPostListCache(Long userId) {
        try {
            java.util.Set<String> keys = redisTemplate.keys("forum:posts:my:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 多线程并行清除所有帖子列表缓存
     */
    @Async("cacheCleanExecutor")
    public void clearAllPostListCache() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("forum:posts:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            java.util.Set<String> collectKeys = redisTemplate.keys("forum:post:collect:*");
            if (collectKeys != null && !collectKeys.isEmpty()) {
                redisTemplate.delete(collectKeys);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 多线程并行清除用户评论列表缓存
     */
    private void clearUserCommentListCache(Long userId) {
        // 使用并行流多线程清除缓存
        IntStream.rangeClosed(1, 10).parallel().forEach(page -> {
            IntStream.range(10, 51).filter(size -> size % 10 == 0).parallel().forEach(size -> {
                redisTemplate.delete("forum:comments:my:" + userId + ":" + page + ":" + size);
            });
        });
    }

    public void addAttachment(ForumPost forumPost) {
        if (forumPost.getImages() != null && !forumPost.getImages().isEmpty()) {
            List<ForumAttachment> attachments = new ArrayList<>();
            for (ForumPost.Image image : forumPost.getImages()) {
                ForumAttachment attachment = new ForumAttachment();
                attachment.setPostId(forumPost.getPostId());
                attachment.setUserId(forumPost.getUserId());
                attachment.setUrl(image.getUrl());
                attachments.add(attachment);
            }
            postMapper.batchInsertAttachments(attachments); // 批量插入
        }
    }

    public void synchronous() {
        postMapper.synchronous();
    }

    public List<ForumSection> selectSections() {
        return postMapper.selectSections();
    }

    public ForumPost postDetail(Integer postId) {
        // ==============================================================
        // 1️⃣ 构造缓存 Key 与 分布式锁 Key
        // ==============================================================
        // cacheKey 用于缓存帖子详情
        String cacheKey = "forum:post:detail:" + postId;
        // lockKey 用于防止并发时多个线程同时查询数据库
        String lockKey = "lock:" + cacheKey;

        // ==============================================================
        // 2️⃣ 查询缓存（Cache Aside 模式第一步）
        // ==============================================================
        Object cacheValue = null;
        try {
            cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                return (ForumPost) cacheValue;
            }
        } catch (Exception e) {
            redisTemplate.delete(cacheKey);
            cacheValue = null;
        }

        // ==============================================================
        // 3️⃣ 缓存未命中 -> 尝试加分布式锁（防止缓存击穿）
        // ==============================================================
        // Redis setIfAbsent() = SETNX + EXPIRE（自动带过期时间）
        // 表示：若锁不存在则加锁成功，并设置 10 秒过期时间防止死锁。
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(lockAcquired)) {
            // ==============================================================
            // 4️⃣ 拿到锁 -> 查询数据库（只有一个线程能进来）
            // ==============================================================
            try {
                ForumPost dbPost = postMapper.postDetail(postId);
                if (dbPost != null) {
                    String un = dbPost.getUsername();
                    if (un != null) {
                        if (un.startsWith("emp_")) dbPost.setAuthorType("物业员工");
                        else if (un.startsWith("admin_")) dbPost.setAuthorType("管理员");
                        else dbPost.setAuthorType("业主");
                    } else {
                        dbPost.setAuthorType("业主");
                    }
                    if (dbPost.getComments() != null) {
                        for (ForumComment c : dbPost.getComments()) {
                            String cun = c.getUsername();
                            if (cun != null) {
                                if (cun.startsWith("emp_")) c.setAuthorType("物业员工");
                                else if (cun.startsWith("admin_")) c.setAuthorType("管理员");
                                else c.setAuthorType("业主");
                            } else {
                                c.setAuthorType("业主");
                            }
                        }
                    }
                }

                // ==============================================================
                // 5️⃣ 将查询结果写入缓存
                // ==============================================================

                if (dbPost == null) {
                    // 🧱 情况①：数据库没有该数据
                    // → 缓存一个“空对象”来防止缓存穿透（恶意请求不存在的 ID）
                    // → 有效期短一些，比如 2 分钟
                    redisTemplate.opsForValue().set(cacheKey, new ForumPost(), 2, TimeUnit.MINUTES);
                } else {
                    // 🧱 情况②：数据库查到了数据
                    // → 缓存真实数据
                    // → 为了防止缓存雪崩，设置“随机过期时间”让大量 Key 不同时失效
                    long expireMinutes = 5 + (long) (Math.random() * 5); // 5~10分钟
                    redisTemplate.opsForValue().set(cacheKey, dbPost, expireMinutes, TimeUnit.MINUTES);
                }

                // ✅ 返回查询结果（此时缓存已写好）
                return dbPost;

            } finally {
                // ==============================================================
                // 6️⃣ 释放锁（防止死锁）
                // ==============================================================
                // 无论是否异常，最终都删除锁 Key。
                // ⚠️ 注意：这里不适用于 Redisson 公平锁场景，只适合简单缓存防护。
                redisTemplate.delete(lockKey);
            }

        } else {
            // ==============================================================
            // 7️⃣ 没拿到锁（说明别的线程正在构建缓存）
            // ==============================================================
            try {
                // 休眠一小段时间，让锁持有线程先查完数据库并回填缓存
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }

            // 再次尝试读取缓存（很可能此时缓存已被其他线程写入）
            Object retryCache = null;
            try {
                retryCache = redisTemplate.opsForValue().get(cacheKey);
                if (retryCache != null) {
                    return (ForumPost) retryCache;
                }
            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
                retryCache = null;
            }

            // 🚨 如果依旧没命中缓存（极端情况，比如锁线程异常退出）
            // 最后兜底再查一次数据库（但不再写缓存，避免重复加锁）
            ForumPost dbPost = postMapper.postDetail(postId);
            return dbPost;
        }
    }

    public ForumComment addPostComment(ForumComment forumComment) {
        postMapper.addPostComment(forumComment);
        // 评论图片附件保存
        if (forumComment.getImages() != null && !forumComment.getImages().isEmpty() && forumComment.getCommentId() != null) {
            java.util.List<ForumAttachment> attachments = new java.util.ArrayList<>();
            for (ForumPost.Image img : forumComment.getImages()) {
                ForumAttachment a = new ForumAttachment();
                a.setCommentId(forumComment.getCommentId());
                a.setUserId(forumComment.getUserId());
                a.setUrl(img.getUrl());
                attachments.add(a);
            }
            if (!attachments.isEmpty()) {
                postMapper.batchInsertCommentAttachments(attachments);
            }
        }
        // 如果是追评（回复某评论），更新追评计数缓存
        if (forumComment.getParentId() != null) {
            postMapper.upsertReplyCountCache(forumComment.getParentId());
        }
        // 清除帖子详情缓存（评论数变化）
        if (forumComment.getPostId() != null) {
            redisTemplate.delete("forum:post:detail:" + forumComment.getPostId());
        }
        // 清除评论列表缓存
        if (forumComment.getUserId() != null) {
            // 清除用户相关的评论列表缓存
            clearUserCommentListCache(forumComment.getUserId());
        }
        clearAllCommentListCache();
        return forumComment;
    }
    
    public java.util.Map<String, Object> toggleLikeComment(Long commentId, Long userId) {
        String setKey = "forum:comment:like:" + commentId;
        Boolean liked = redisTemplate.opsForSet().isMember(setKey, userId);
        int delta = Boolean.TRUE.equals(liked) ? -1 : 1;
        if (delta > 0) {
            redisTemplate.opsForSet().add(setKey, userId);
        } else {
            redisTemplate.opsForSet().remove(setKey, userId);
        }
        postMapper.updateCommentLikeCount(commentId, delta);
        ForumComment c = postMapper.selectCommentById(commentId);
        if (c != null && c.getPostId() != null) {
            // 清除帖子详情缓存以刷新点赞数
            redisTemplate.delete("forum:post:detail:" + c.getPostId());
        }
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("liked", delta > 0);
        res.put("likeCount", c != null ? c.getLikeCount() + delta : delta);
        return res;
    }

    public PageInfo<ForumPost> selectPageCollect(ForumPost forumPost, Integer pageNum, Integer pageSize) {

        String cacheKey = "forum:post:collect:" + forumPost.getUserId();
        String lockKey = "lock:" + cacheKey;

        Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            return (PageInfo<ForumPost>) cacheValue;
        }
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.selectPageCollect(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, pageInfo.of(List.of()), 1, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 5 + (long) (Math.random() * 5);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {
            }
            // 再查一次缓存（此时可能被其他线程写入了）
            Object retryCache = redisTemplate.opsForValue().get(cacheKey);
            if (retryCache != null) {
                return (PageInfo<ForumPost>) retryCache;
            }

            // 如果仍然没有缓存（极端情况），再从数据库查询（不建议频繁发生）
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.selectPostAll(forumPost);
            return PageInfo.of(list);
        }

    }

    public PageInfo<ForumPost> selectPageHot(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        String cacheKey = "forum:posts:hot:" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<ForumPost>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.selectPageHot(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 热门帖子缓存5分钟
                    long expireMinutes = 3 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<ForumPost>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.selectPageHot(forumPost);
            return PageInfo.of(list);
        }
    }

    public PageInfo<ForumPost> MostBookmarked(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        return null;
    }

    public void updatePostCollectCount() {
        postMapper.updatePostCollectCount();
        // 清除所有帖子列表缓存（收藏数变化影响排序）
        clearAllPostListCache();
    }

    public PageInfo<ForumPost> selectPageHotByCollect(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        String cacheKey = "forum:posts:hot:collect:" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<ForumPost>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.selectPageHotByCollect(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 3 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<ForumPost>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.selectPageHotByCollect(forumPost);
            return PageInfo.of(list);
        }
    }

    public PageInfo<ForumPost> SelectByTitle(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        String titleKey = forumPost != null && forumPost.getTitle() != null ? forumPost.getTitle() : "";
        String cacheKey = "forum:posts:title:" + titleKey.hashCode() + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<ForumPost>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.SelectByTitle(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 3 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<ForumPost>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.SelectByTitle(forumPost);
            return PageInfo.of(list);
        }
    }

    public PageInfo<ForumPost> showMyPost(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        Long userId = forumPost != null ? forumPost.getUserId() : null;
        String cacheKey = "forum:posts:my:" + userId + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<ForumPost>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.showMyPost(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    // 我的帖子缓存3分钟
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, 3, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<ForumPost>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.showMyPost(forumPost);
            return PageInfo.of(list);
        }
    }

    public void delCancelCollect(ForumPost forumPost) {
        postMapper.delCancelCollect(forumPost);
        // 清除收藏相关缓存
        if (forumPost.getPostId() != null && forumPost.getUserId() != null) {
            redisTemplate.delete("forum:collect:" + forumPost.getPostId() + ":" + forumPost.getUserId());
            redisTemplate.delete("forum:post:collect:" + forumPost.getUserId());
        }
        // 清除帖子详情缓存（收藏数变化）
        if (forumPost.getPostId() != null) {
            redisTemplate.delete("forum:post:detail:" + forumPost.getPostId());
        }
        clearAllPostListCache();
    }

    public void delMyPost(Integer postId) {
        postMapper.delMyPost(postId);
        // 清除帖子详情和列表缓存
        redisTemplate.delete("forum:post:detail:" + postId);
        // 清除所有帖子列表缓存
        clearAllPostListCache();
    }

    public void updatePostWithSectionName() {
        postMapper.updatePostWithSectionName();
        // 清除所有帖子列表缓存（板块名称变化）
        clearAllPostListCache();
    }

    public PageInfo<ForumPost> SwitchSection(ForumPost forumPost, Integer pageNum, Integer pageSize) {
        String sectionKey = "";
        if (forumPost != null) {
            if (forumPost.getSectionId() != null) {
                sectionKey = String.valueOf(forumPost.getSectionId());
            } else if (forumPost.getSectionName() != null) {
                sectionKey = forumPost.getSectionName();
            }
        }
        String cacheKey = "forum:posts:section:" + sectionKey.hashCode() + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<ForumPost>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<ForumPost> list = postMapper.SwitchSectione(forumPost);
                PageInfo<ForumPost> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 5 + (long) (Math.random() * 5);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<ForumPost>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<ForumPost> list = postMapper.SwitchSectione(forumPost);
            return PageInfo.of(list);
        }
    }

    public void addToCollection(ForumPost forumPost) {
        postMapper.addToCollection(forumPost);
        // 清除收藏相关缓存
        if (forumPost.getPostId() != null && forumPost.getUserId() != null) {
            redisTemplate.delete("forum:collect:" + forumPost.getPostId() + ":" + forumPost.getUserId());
            redisTemplate.delete("forum:post:collect:" + forumPost.getUserId());
        }
        // 清除帖子详情缓存（收藏数变化）
        if (forumPost.getPostId() != null) {
            redisTemplate.delete("forum:post:detail:" + forumPost.getPostId());
        }
        clearAllPostListCache();
    }

    public boolean isCollected(Long postId, Long userId) {
        String cacheKey = "forum:collect:" + postId + ":" + userId;
        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (Boolean) cache;
        }

        Integer count = postMapper.existsByPostIdAndUserId(postId, userId);
        boolean collected = count != null && count > 0;
        // 缓存收藏状态，5分钟过期
        redisTemplate.opsForValue().set(cacheKey, collected, 5, TimeUnit.MINUTES);
        return collected;
    }

    public PageInfo<CommentWithPostDTO> SelectMyPostCommentt(Long userId, Integer pageNum, Integer pageSize) {
        String cacheKey = "forum:comments:my:" + userId + ":" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<CommentWithPostDTO>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<CommentWithPostDTO> list = postMapper.SelectMyPostCommentt(userId);
                PageInfo<CommentWithPostDTO> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, 3, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<CommentWithPostDTO>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<CommentWithPostDTO> list = postMapper.SelectMyPostCommentt(userId);
            return PageInfo.of(list);
        }
    }

    public void delMyPostComment(Integer commentId) {
        ForumComment comment = postMapper.selectCommentById(commentId.longValue());
        Long postId = comment != null ? comment.getPostId() : null;
        Long parentId = comment != null ? comment.getParentId() : null;
        postMapper.delMyPostComment(commentId);
        if (parentId != null) {
            postMapper.upsertReplyCountCache(parentId);
        }
        if (postId != null) {
            redisTemplate.delete("forum:post:detail:" + postId);
        }
        // 清除评论相关缓存
        // 注意：由于无法通过commentId直接获取postId和userId，这里清除所有评论列表缓存
        clearAllCommentListCache();
    }
    
    /**
     * 多线程并行清除所有评论列表缓存
     */
    private void clearAllCommentListCache() {
        // 使用并行流多线程清除缓存
        IntStream.rangeClosed(1, 10).parallel().forEach(page -> {
            IntStream.range(10, 51).filter(size -> size % 10 == 0).parallel().forEach(size -> {
                redisTemplate.delete("forum:comments:all:" + page + ":" + size);
            });
        });
    }

    public PageInfo<CommentWithPostDTO> SelectAllComment(Integer pageNum, Integer pageSize) {
        String cacheKey = "forum:comments:all:" + pageNum + ":" + pageSize;
        String lockKey = "lock:" + cacheKey;

        Object cache = redisTemplate.opsForValue().get(cacheKey);
        if (cache != null) {
            return (PageInfo<CommentWithPostDTO>) cache;
        }

        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PageHelper.startPage(pageNum, pageSize);
                List<CommentWithPostDTO> list = postMapper.SelectAllComment();
                PageInfo<CommentWithPostDTO> pageInfo = PageInfo.of(list);

                if (list == null || list.isEmpty()) {
                    redisTemplate.opsForValue().set(cacheKey, PageInfo.of(List.of()), 2, TimeUnit.MINUTES);
                } else {
                    long expireMinutes = 3 + (long) (Math.random() * 4);
                    redisTemplate.opsForValue().set(cacheKey, pageInfo, expireMinutes, TimeUnit.MINUTES);
                }
                return pageInfo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                cache = redisTemplate.opsForValue().get(cacheKey);
                if (cache != null) {
                    return (PageInfo<CommentWithPostDTO>) cache;
                }
            } catch (InterruptedException ignored) {
            }
            PageHelper.startPage(pageNum, pageSize);
            List<CommentWithPostDTO> list = postMapper.SelectAllComment();
            return PageInfo.of(list);
        }
    }

    public void top(Integer postId) {
        postMapper.top(postId);
        // 清除帖子详情和列表缓存
        redisTemplate.delete("forum:post:detail:" + postId);
        clearAllPostListCache();
    }

    public void exquisite(Integer postId) {
        postMapper.exquisite(postId);
        // 清除帖子详情和列表缓存
        redisTemplate.delete("forum:post:detail:" + postId);
        clearAllPostListCache();
    }

    public void cancelExquisite(Integer postId) {
        postMapper.cancelExquisite(postId);
        // 清除帖子详情和列表缓存
        redisTemplate.delete("forum:post:detail:" + postId);
        clearAllPostListCache();
    }

    public void cancelTop(Integer postId) {
        postMapper.cancelTop(postId);
        // 清除帖子详情和列表缓存
        redisTemplate.delete("forum:post:detail:" + postId);
        clearAllPostListCache();
    }

    //更新追评
    private String generateStableKey(ForumPost forumPost) {
        String rawKey = null;
        try {
            // 你可以选择关键查询字段（如标题、作者、分类等）
            rawKey = String.format("title:%s|author:%s|category:%s",
                    forumPost.getTitle(),
                    forumPost.getPostId(),
                    forumPost.getCommentId());

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));

            // 转换成十六进制字符串
            Formatter formatter = new Formatter();
            for (byte b : digest) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (Exception e) {
            return String.valueOf(rawKey.hashCode());
        }
    }
}
