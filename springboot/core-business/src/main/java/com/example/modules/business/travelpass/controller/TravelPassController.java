package com.example.modules.business.travelpass.controller;

import cn.hutool.json.JSONUtil;
import com.example.entity.TravelPassRecord;
import com.example.entity.User;
import com.example.modules.auth.service.LoginRegisterService;
import com.example.modules.business.travelpass.service.TravelPassService;
import com.example.utils.QRCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
    @RequestMapping("/travel-pass")
    public class TravelPassController {

    @Autowired
    private LoginRegisterService loginRegisterService;

    @Autowired
    private TravelPassService travelPassService;

    /**
     * 住户端：生成出行码二维码
     */
    @GetMapping("/issue")
    public ResponseEntity<?> issuePass(
            @RequestParam String username,
            @RequestParam(defaultValue = "120") Integer duration,
            @RequestParam(defaultValue = "false") Boolean hasVehicle,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(defaultValue = "false") Boolean paid
    ) {
        User user = loginRegisterService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        long now = System.currentTimeMillis();
        long expireAt = now + duration * 60 * 1000L;

        // 新建数据库记录
        TravelPassRecord record = new TravelPassRecord();
        record.setUserId(user.getUserId());
        record.setUsername(user.getUsername());
        record.setHasVehicle(hasVehicle);
        record.setPlateNumber(hasVehicle ? plateNumber : null);
        record.setPaid(paid);
        record.setIssueTime(new Date(now));
        record.setExpireTime(new Date(expireAt));
        record.setStatus("ISSUED");
        travelPassService.createRecord(record);

        // 二维码内容包含 recordId 和时间戳（使二维码动态变化）
        Map<String, Object> qrMap = new HashMap<>();
        qrMap.put("recordId", record.getId());
        qrMap.put("expireAt", expireAt);
        qrMap.put("valid", true);
        qrMap.put("timestamp", now); // 添加时间戳，使二维码动态变化

        String qrContent = JSONUtil.toJsonStr(qrMap);

        String base64;
        try {
            base64 = QRCodeUtil.generateQRCodeBase64(qrContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("生成二维码失败");
        }

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", base64);
        result.put("recordId", record.getId());
        result.put("expireAt", expireAt);
        result.put("duration", duration);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 管理端：分页查询出行记录
     */
    @GetMapping("/selectPage")
    public ResponseEntity<?> selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String username) {
        var list = travelPassService.selectPage(pageNum, pageSize, status, username);
        return ResponseEntity.ok(list);
    }

    /**
     * 住户端：查询我的出行记录
     */
    @GetMapping("/myRecords")
    public ResponseEntity<?> myRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam String username) { // 暂时传username，理想情况是从token获取userId
        User user = loginRegisterService.findByUsername(username);
        if (user == null) return ResponseEntity.badRequest().body("用户不存在");
        
        var list = travelPassService.selectMyRecords(pageNum, pageSize, user.getUserId());
        return ResponseEntity.ok(list);
    }

    /**
     * 门岗端：核销二维码（支持文件上传和直接ID两种方式）
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPass(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "recordId", required = false) Long recordId,
            @RequestBody(required = false) String requestBody
    ) {
        try {
            // 处理JSON请求体中的recordId
            if (requestBody != null && !requestBody.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> requestMap = JSONUtil.toBean(requestBody, Map.class);
                    if (requestMap.containsKey("recordId")) {
                        recordId = ((Number) requestMap.get("recordId")).longValue();
                    }
                } catch (Exception e) {
                    // 如果JSON解析失败，尝试直接解析为recordId
                    try {
                        recordId = Long.parseLong(requestBody.trim());
                    } catch (NumberFormatException nfe) {
                        // 忽略解析错误，继续使用其他参数
                    }
                }
            }

            // 参数验证：必须提供file或recordId之一
            if (file == null && recordId == null) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "请提供二维码文件或记录ID"));
            }

            // 方式1：通过文件上传识别二维码
            if (file != null) {
                String content = QRCodeUtil.decodeQRCode(file.getInputStream());
                if (content == null || content.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "二维码内容为空"));
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> qrData = JSONUtil.toBean(content, Map.class);
                recordId = ((Number) qrData.get("recordId")).longValue();
            }

            // 方式2：直接使用提供的recordId
            TravelPassRecord record = travelPassService.findById(recordId);
            if (record == null) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "记录不存在"));
            }

            // 校验逻辑
            boolean valid = true;
            String message = "";
            
            // 检查二维码是否过期
            Date now = new Date();
            if (record.getExpireTime().before(now)) {
                // 如果二维码已过期，更新数据库状态为EXPIRED
                if ("ISSUED".equals(record.getStatus())) {
                    travelPassService.markExpired(recordId);
                    record.setStatus("EXPIRED");
                }
                valid = false;
                message = "二维码已过期";
                return ResponseEntity.ok(Map.of("valid", valid, "data", record, "message", message));
            }
            
            // 取核销员工ID（门卫）
            Long employeeId = null;
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.example.entity.Employee emp) {
                employeeId = emp.getEmployeeId();
            }
            
            if ("ISSUED".equals(record.getStatus())) {
                // 入场
                travelPassService.markEntered(recordId, employeeId);
                record.setStatus("ENTERED");
                record.setEntryTime(now);
                message = "✅ 入场成功";
            } else if ("ENTERED".equals(record.getStatus())) {
                // 出场：计算停车费
                long diffMillis = now.getTime() - record.getEntryTime().getTime();
                long hours = (long) Math.ceil(diffMillis / (1000.0 * 60 * 60));

                double fee = 0;
                if (record.getHasVehicle()) {
                    if (diffMillis > 30 * 60 * 1000L) {
                        fee = hours * 2.0; // 停车费：超过30分钟后，每小时2元
                    }
                }
                travelPassService.markExited(recordId, fee, employeeId);
                record.setExitTime(now);
                record.setFee(fee);
                record.setStatus("EXITED");
                message = "✅ 出场成功，停车费：" + fee + " 元";
            } else if ("EXPIRED".equals(record.getStatus())) {
                valid = false;
                message = "二维码已过期";
            } else {
                valid = false;
                message = "该二维码已使用完毕";
            }

            return ResponseEntity.ok(Map.of("valid", valid, "data", record, "message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "二维码识别或解析失败"));
        }
    }

    /**
     * 刷新已生成的出行码二维码（动态更新）
     */
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshPass(@RequestParam Long recordId) {
        TravelPassRecord record = travelPassService.findById(recordId);
        if (record == null) {
            return ResponseEntity.badRequest().body("记录不存在");
        }

        // 检查是否已过期
        long now = System.currentTimeMillis();
        if (record.getExpireTime() != null && record.getExpireTime().getTime() < now) {
            return ResponseEntity.badRequest().body("出行码已过期");
        }

        // 检查状态
        if (!"ISSUED".equals(record.getStatus()) && !"ENTERED".equals(record.getStatus()) && !"EXPIRED".equals(record.getStatus())) {
            return ResponseEntity.badRequest().body("出行码已使用完毕，无法刷新");
        }

        // 生成新的动态二维码（包含新的时间戳）
        Map<String, Object> qrMap = new HashMap<>();
        qrMap.put("recordId", record.getId());
        qrMap.put("expireAt", record.getExpireTime().getTime());
        qrMap.put("valid", true);
        qrMap.put("timestamp", now); // 更新时间戳，使二维码动态变化

        String qrContent = JSONUtil.toJsonStr(qrMap);

        String base64;
        try {
            base64 = QRCodeUtil.generateQRCodeBase64(qrContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("生成二维码失败");
        }

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", base64);
        result.put("recordId", record.getId());
        result.put("expireAt", record.getExpireTime().getTime());
        result.put("timestamp", now);
        return ResponseEntity.ok(result);
    }
}
