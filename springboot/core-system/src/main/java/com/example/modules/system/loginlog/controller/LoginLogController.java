package com.example.modules.system.loginlog.controller;

import com.example.common.Result;
import com.example.modules.system.loginlog.mapper.LoginLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/logs")
public class LoginLogController {
    @Resource
    private LoginLogMapper loginLogMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String role) {
        PageHelper.startPage(pageNum, pageSize);
        List<Map<String, Object>> list = loginLogMapper.selectPage(username, role);
        return Result.success(PageInfo.of(list));
    }
}
