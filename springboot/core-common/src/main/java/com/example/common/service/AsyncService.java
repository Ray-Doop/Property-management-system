package com.example.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 异步服务类
 * 提供各种异步操作的封装
 */
@Service
public class AsyncService {

    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    @Autowired
    @Qualifier("cacheCleanExecutor")
    private Executor cacheCleanExecutor;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 异步批量清除缓存
     */
    @Async("cacheCleanExecutor")
    public CompletableFuture<Void> batchClearCache(List<String> cacheKeys) {
        // 并行清除缓存
        cacheKeys.parallelStream().forEach(key -> redisTemplate.delete(key));
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步批量清除缓存（带回调）
     */
    @Async("cacheCleanExecutor")
    public CompletableFuture<Integer> batchClearCacheWithCallback(List<String> cacheKeys, Runnable onComplete) {
        int deletedCount = 0;
        for (String key : cacheKeys) {
            if (redisTemplate.delete(key)) {
                deletedCount++;
            }
        }
        if (onComplete != null) {
            onComplete.run();
        }
        return CompletableFuture.completedFuture(deletedCount);
    }

    /**
     * 并行执行多个任务
     */
    public <T> CompletableFuture<List<T>> executeParallel(List<CompletableFuture<T>> tasks) {
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenApply(v -> tasks.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * 异步执行任务（通用方法）
     */
    @Async("asyncTaskExecutor")
    public <T> CompletableFuture<T> executeAsync(java.util.function.Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, asyncTaskExecutor);
    }
}

