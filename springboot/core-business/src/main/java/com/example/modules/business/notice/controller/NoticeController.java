package com.example.modules.business.notice.controller;

import com.example.common.Result;
import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import com.example.entity.notice.Notice;
import com.example.modules.business.notice.service.NoticeService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 公告控制层
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/List")
    public Result allNotice(
            Notice  notice,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 尝试从 SecurityContext 获取当前用户ID
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                 // 这里通常 UserDetails 的 username 是用户名，我们需要根据用户名查 ID
                 // 或者，如果 JWT 中存了 ID，可以从 auth.getPrincipal() 或 details 中获取
                 // 假设前端传了 currentUserId，或者后端通过 TokenUtils 解析
            }
        } catch (Exception e) {
            // ignore
        }
        
        PageInfo<Notice> repairOrderPageInfo = noticeService.selectAllNotice(notice,page, size);
        return Result.success(repairOrderPageInfo);
    }

    @PostMapping("/publish")
    public Result publish(@RequestBody Notice  notice) {
        fillPublisher(notice);
        if (notice.getPublisherId() == null) {
            return Result.error("发布人不能为空");
        }
        Long noticeId = noticeService.publish(notice);
        if (notice.getAttachments() != null && !notice.getAttachments().isEmpty()) {
            notice.setNoticeId(noticeId);
            noticeService.addAttachment(notice);
        }
        System.out.println("=======================================================================================================================");
        System.out.println(notice);
        System.out.println("=======================================================================================================================");
        System.out.println();
        return Result.success(noticeId);
    }

    @PostMapping("/saveDraft")
    public Result saveDraft(@RequestBody Notice notice) {
        fillPublisher(notice);
        if (notice.getPublisherId() == null) {
            return Result.error("发布人不能为空");
        }
        Long noticeId = noticeService.saveDraft(notice);
        if (notice.getAttachments() != null && !notice.getAttachments().isEmpty()) {
            notice.setNoticeId(noticeId);
            noticeService.addAttachment(notice);
        }
        return Result.success(noticeId);
    }

    @GetMapping("/{id}")
    public Result getNoticeById(@PathVariable Long id) {
        Notice notice = noticeService.selectById(id);
        if (notice == null) {
            return Result.error("公告不存在");
        }
        return Result.success(notice);
    }
    
    @GetMapping("/readStatus")
    public Result readStatus(@RequestParam Long noticeId) {
        return Result.success(noticeService.readStatus(noticeId));
    }
    
    @GetMapping("/summaryPage")
    public Result summaryPage(@RequestParam(required = false) String title,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(noticeService.summaryPage(title, page, size));
    }
    
    @GetMapping("/readTable")
    public Result readTable(@RequestParam Long noticeId,
                            @RequestParam(required = false) Integer readStatus,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(noticeService.readTable(noticeId, readStatus, keyword, page, size));
    }

    @GetMapping("/stats")
    public Result getStats() {
        return Result.success(noticeService.getStats());
    }

    @PostMapping("/markRead")
    public Result markRead(@RequestParam Long noticeId,
                           @RequestParam String residenceId,
                           @RequestParam Integer readStatus) {
        noticeService.updateReadStatus(noticeId, residenceId, readStatus);
        return Result.success();
    }

    @PostMapping("/read")
    public Result markAsRead(@RequestBody java.util.Map<String, Long> params) {
        Long noticeId = params.get("noticeId");
        Long userId = params.get("userId");
        if (noticeId == null || userId == null) {
            return Result.error("参数错误");
        }
        noticeService.markAsRead(noticeId, userId);
        return Result.success();
    }
    
    @DeleteMapping("/{noticeId}")
    public Result deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return Result.success();
    }
    
    @PostMapping("/audit")
    public Result audit(@RequestBody java.util.Map<String, Object> params) {
        Long noticeId = Long.valueOf(params.get("noticeId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        noticeService.audit(noticeId, status);
        return Result.success();
    }

    private void fillPublisher(Notice notice) {
        if (notice == null || notice.getPublisherId() != null) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Admin admin) {
            notice.setPublisherId(admin.getAdminId());
            if (notice.getPublisherName() == null || notice.getPublisherName().isEmpty()) {
                String name = admin.getNickname();
                notice.setPublisherName(name != null && !name.isEmpty() ? name : admin.getUsername());
            }
            if (notice.getPublisherType() == null || notice.getPublisherType().isEmpty()) {
                notice.setPublisherType("ADMIN");
            }
        } else if (principal instanceof Employee employee) {
            notice.setPublisherId(employee.getEmployeeId());
            if (notice.getPublisherName() == null || notice.getPublisherName().isEmpty()) {
                String name = employee.getNickname();
                notice.setPublisherName(name != null && !name.isEmpty() ? name : employee.getUsername());
            }
            if (notice.getPublisherType() == null || notice.getPublisherType().isEmpty()) {
                notice.setPublisherType("EMPLOYEE");
            }
        } else if (principal instanceof User user) {
            notice.setPublisherId(user.getUserId());
            if (notice.getPublisherName() == null || notice.getPublisherName().isEmpty()) {
                String name = user.getNickname();
                notice.setPublisherName(name != null && !name.isEmpty() ? name : user.getUsername());
            }
            if (notice.getPublisherType() == null || notice.getPublisherType().isEmpty()) {
                notice.setPublisherType("USER");
            }
        }
    }
}
