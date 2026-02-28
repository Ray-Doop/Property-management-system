package com.example.modules.business.repair.controller;

import com.example.common.Result;
import com.example.entity.repair.RepairEvaluation;
import com.example.modules.business.repair.mapper.RepairEvaluationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/repair/evaluation")
public class RepairEvaluationController {
    @Resource
    private RepairEvaluationMapper repairEvaluationMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) Integer score,
                       @RequestParam(required = false) Long assignmentId) {
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<RepairEvaluation> pageInfo = PageInfo.of(repairEvaluationMapper.selectPage(score, assignmentId));
        return Result.success(pageInfo);
    }

    @PostMapping("/reply")
    public Result reply(@RequestParam Long evalId, @RequestParam String replyContent) {
        repairEvaluationMapper.updateReply(evalId, replyContent);
        return Result.success();
    }
}
