package com.example.modules.system.admin.controller;

import com.example.common.Result;
import com.example.entity.Admin;
import com.example.modules.system.admin.mapper.AdminMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Resource
    private AdminMapper adminMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       Admin admin) {
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<Admin> pageInfo = PageInfo.of(adminMapper.selectAll(admin));
        return Result.success(pageInfo);
    }

    @PostMapping("/updateStatus")
    public Result updateStatus(@RequestParam Long adminId, @RequestParam Integer status) {
        adminMapper.updateStatus(adminId, status);
        return Result.success();
    }

    @PostMapping("/updateRole")
    public Result updateRole(@RequestParam Long adminId, @RequestParam String role) {
        adminMapper.updateRole(adminId, role);
        return Result.success();
    }

    @PostMapping("/add")
    public Result add(@RequestBody Admin admin) {
        if (admin.getPassword() != null && !admin.getPassword().startsWith("$2")) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        adminMapper.insert(admin);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(@RequestParam Long adminId) {
        adminMapper.deleteById(adminId);
        return Result.success();
    }
}
