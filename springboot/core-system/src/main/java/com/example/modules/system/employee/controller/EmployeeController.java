package com.example.modules.system.employee.controller;

import com.example.common.Result;
import com.example.entity.Employee;
import com.example.modules.system.employee.service.EmployeeService;
import com.example.modules.auth.service.LoginRegisterService;
import com.github.pagehelper.PageInfo;
import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;
    
    @Resource
    private LoginRegisterService loginRegisterService;

    @DeleteMapping("/del/{id}")
    public Result del(@PathVariable Integer id) {
        employeeService.del(id);
        return Result.success();
    }

    @PostMapping("/add")
    public Result add(@RequestBody Employee employee) {
        employeeService.add(employee);
        return Result.success();
    }

    @PostMapping("/delBatch")
    public Result delBatch(@RequestBody List<Integer> ids) {
        employeeService.delBatch(ids);
        return Result.success();
    }

    @PutMapping("/updata")
    public Result update(@RequestBody Employee employee) {
        employeeService.updata(employee);
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(Employee employee) {
        List<Employee> list = employeeService.selectAll(employee);
        return Result.success(list);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Employee employee = employeeService.selectById(id);
        return Result.success(employee);
    }

    @GetMapping("/selectPage")
    public Result selectPage(
            Employee employee,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Employee> pageInfo = employeeService.selectPage(employee,pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @PostMapping("/checkOldPassword")
    public Result checkOldPassword(@RequestBody Map<String, Object> body) {
        Object employeeIdObj = body.get("employeeId");
        Object oldPasswordObj = body.get("oldPassword");
        if (employeeIdObj == null || oldPasswordObj == null) {
            return Result.error("400", "参数不完整");
        }
        Long employeeId = Long.parseLong(employeeIdObj.toString());
        String oldPassword = oldPasswordObj.toString();
        boolean ok = employeeService.checkOldPassword(employeeId, oldPassword);
        if (!ok) {
            return Result.error("400", "旧密码错误");
        }
        return Result.success();
    }

    @PostMapping("/updatePassword")
    public Result updatePassword(@RequestBody Map<String, Object> body) {
        Object employeeIdObj = body.get("employeeId");
        Object newPasswordObj = body.get("newPassword");
        if (employeeIdObj == null || newPasswordObj == null) {
            return Result.error("400", "参数不完整");
        }
        Long employeeId = Long.parseLong(employeeIdObj.toString());
        String newPassword = newPasswordObj.toString();
        employeeService.updatePassword(employeeId, newPassword);
        return Result.success();
    }
    
    // 更新头像
    @PostMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam("file") MultipartFile file, @RequestParam("employeeId") Long employeeId) {
        if (file == null || file.isEmpty()) {
            return Result.error("400", "未接收到文件");
        }
        
        try {
            // 使用与FileController一致的路径
            String filePath = System.getProperty("user.dir") + "/files/avatar/";
            if (!new java.io.File(filePath).exists()) {
                new java.io.File(filePath).mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            String realPath = filePath + fileName;
            
            // 保存文件
            FileUtil.writeBytes(file.getBytes(), realPath);
            
            // 生成访问URL
            String avatarUrl = "http://localhost:8080/files/avatar/" + fileName;
            
            // 更新数据库
            employeeService.updateAvatar(employeeId, avatarUrl);
            
            // 更新Redis缓存
            Employee employee = loginRegisterService.getEmployeeById(employeeId);
            if (employee != null) {
                employee.setAvatarUrl(avatarUrl);
            }
            
            // 使用正确的Result.success方法
            java.util.Map<String, String> resultMap = new java.util.HashMap<>();
            resultMap.put("avatarUrl", avatarUrl);
            return Result.success(resultMap);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("500", "头像上传失败");
        }
    }
}
