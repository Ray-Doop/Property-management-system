package com.example.modules.system.stats.controller;

import com.example.common.Result;
import com.example.modules.system.stats.service.StatsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
public class StatsController {
    @Resource
    private StatsService statsService;

    @GetMapping("/overview")
    public Result overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/trend")
    public Result trend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(statsService.trend(days));
    }
}
