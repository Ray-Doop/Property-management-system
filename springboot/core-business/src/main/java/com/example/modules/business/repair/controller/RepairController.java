package com.example.modules.business.repair.controller;

import com.example.common.Result;
import com.example.entity.Forum.ForumPost;
import com.example.entity.repair.RepairAssignment;
import com.example.entity.repair.RepairCategory;
import com.example.entity.repair.RepairOrder;
import com.example.entity.Employee;
import com.example.modules.business.repair.service.RepairService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/repair")
public class RepairController {
    @Autowired
    private RepairService repairService;

    //    查询维修类别
    @GetMapping("/categories")
    public Result categories() {
        List<RepairCategory> categories = repairService.selectCategories();
        return Result.success(categories);
    }

    // 评价
    @PostMapping("/evaluate")
    public Result evaluate(@RequestBody RepairOrder repairOrder) {
        if (repairOrder.getOrderId() == null || repairOrder.getRating() == null) {
            return Result.error("参数错误");
        }
        repairService.evaluateRepair(repairOrder.getOrderId(), repairOrder.getEvaluation(), repairOrder.getRating());
        return Result.success();
    }

    @PostMapping("/updateStatus")
    public Result updateStatus(@RequestBody RepairOrder repairOrder) {
        if (repairOrder.getOrderId() == null || repairOrder.getStatus() == null) {
            return Result.error("参数错误");
        }
        repairService.updateStatus(repairOrder.getOrderId(), repairOrder.getStatus());
        return Result.success();
    }

    //    提交维修内容
    @PostMapping("/submit")
    public Result addRepairOrder(@RequestBody RepairOrder repairOrder) {
        System.out.println(repairOrder);
        Long orderId = repairService.addRepairOrder(repairOrder);
        
        // 保存图片
        if (repairOrder.getFileUrls() != null && !repairOrder.getFileUrls().isEmpty()) {
            repairService.addRepairFiles(orderId, repairOrder.getFileUrls(), repairOrder.getUserId());
        }
        
        return Result.success(orderId);
    }

    //查看我的维修
    @GetMapping("/myRepair")
    public Result myRepair(@RequestParam Integer userId,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size) {
        PageInfo<RepairOrder> repairOrderPageInfo = repairService.selectMyRepair(userId, page, size);
        return Result.success(repairOrderPageInfo);
    }

    //维修详情
    @GetMapping("/detail/{orderId}")
    public ResponseEntity<?> getRepairDetail(@PathVariable Integer orderId) {
        RepairOrder repairOrder = repairService.getRepairDetail(orderId);
        if (repairOrder == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", "404", "msg", "报修单不存在"));
        }
        System.out.println("---------------------------------------------------------");
        System.out.println(repairOrder);
        return ResponseEntity.ok(Map.of("code", "200", "msg", "成功", "data", repairOrder));
    }

    /**
     * 统计各状态的维修单数量
     */
    @GetMapping("/stats")
    public Result getStats() {
        Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("pending", repairService.countByStatus("待处理"));
        stats.put("processing", repairService.countByStatus("维修中"));
        stats.put("completed", repairService.countByStatus("已完成"));
        return Result.success(stats);
    }

    //取消
    @PostMapping("/cancel/{orderId}")
    public Result cancelRepair(@PathVariable Integer orderId) {
        System.out.println("2222222222222222222222222222222222222222222222222222222222222222222222222");
        System.out.println(orderId);
        repairService.cancelRepair(orderId);
        return Result.success();
    }

    //所有工单
    @GetMapping("/allRepair")
    public Result allRepair(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageInfo<RepairOrder> repairOrderPageInfo = repairService.allRepair(page, size);
        return Result.success(repairOrderPageInfo);
    }

    //状态分类
    @GetMapping("/status")
    public Result status(@RequestParam String status,
                         @RequestParam(defaultValue = "1") Integer page,
                         @RequestParam(defaultValue = "10") Integer size) {
        PageInfo<RepairOrder> repairOrderPageInfo = repairService.status(status, page, size);
        return Result.success(repairOrderPageInfo);
    }

    //查询匹配人员
    @GetMapping("/findWorkers")
    public Result findWorkers(@RequestParam Long categoryId) {
        List<Employee> list = repairService.findWorkers(categoryId);
        return Result.success(list);
    }

    //分配
    @PostMapping("dispatchOrder")
    public Result dispatchOrder(@RequestBody RepairAssignment repairAssignment) {
        String error = repairService.dispatchOrder(repairAssignment);
        if (error != null) {
            return Result.error(error);
        }
        return Result.success();
    }

    // 完成维修（上传图片并标记完成）
    @PostMapping("/complete")
    public Result complete(@RequestBody Map<String, Object> body) {
        Object orderIdObj = body.get("orderId");
        if (orderIdObj == null) {
            return Result.error("缺少参数 orderId");
        }
        Long orderId = ((Number) orderIdObj).longValue();
        @SuppressWarnings("unchecked")
        List<String> fileUrls = (List<String>) body.get("fileUrls");
        Long uploaderId = null;
        Object uploaderObj = body.get("uploaderId");
        if (uploaderObj instanceof Number) {
            uploaderId = ((Number) uploaderObj).longValue();
        }
        // 附件可选
        if (fileUrls != null && !fileUrls.isEmpty()) {
            repairService.addRepairFiles(orderId, fileUrls, uploaderId);
        }
        // 更新状态为已完成
        repairService.updateStatus(orderId, "已完成");
        return Result.success();
    }
    
    // 员工接单：指派自己并置为维修中
    @PostMapping("/accept")
    public Result accept(@RequestBody Map<String, Object> body) {
        Object orderIdObj = body.get("orderId");
        Object workerIdObj = body.get("workerId");
        if (orderIdObj == null || workerIdObj == null) {
            return Result.error("缺少参数 orderId 或 workerId");
        }
        Long orderId = ((Number) orderIdObj).longValue();
        Long workerId = ((Number) workerIdObj).longValue();
        repairService.acceptByWorker(orderId, workerId);
        return Result.success();
    }
    
    // 按员工过滤工单（如已完成仅看本人）
    @GetMapping("/byWorker")
    public Result byWorker(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam Long workerId,
                           @RequestParam(required = false) String status) {
        PageInfo<RepairOrder> pageInfo = repairService.selectByWorkerId(page, size, workerId, status);
        return Result.success(pageInfo);
    }
    
    // 获取订单评价（已完成订单可查看）
    @GetMapping("/evaluation/{orderId}")
    public Result evaluation(@PathVariable Long orderId) {
        var eval = repairService.getEvaluationByOrderId(orderId);
        return Result.success(eval);
    }
    


}
