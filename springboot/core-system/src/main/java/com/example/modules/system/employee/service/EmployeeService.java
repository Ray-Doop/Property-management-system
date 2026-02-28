package com.example.modules.system.employee.service;

import cn.hutool.core.util.StrUtil;
import com.example.entity.Employee;
import com.example.exception.CustomException;
import com.example.modules.system.employee.mapper.EmployeeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Employee> selectAll(Employee employee) {
        List<Employee> list = employeeMapper.selectAll(employee);
        return list;

    }

    public Employee selectById(Integer id) {
        Employee employee = employeeMapper.selectById(id);
        return employee;
    }

    public PageInfo<Employee> selectPage(Employee employee, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Employee> list = employeeMapper.selectPage(employee);
        return PageInfo.of(list);
    }

    public void add(Employee employee) {
        if (employee == null || StrUtil.isBlank(employee.getUsername())) {
            throw new CustomException("400", "用户名不能为空");
        }
        if (StrUtil.isBlank(employee.getPassword())) {
            throw new CustomException("400", "密码不能为空");
        }
        if (StrUtil.isBlank(employee.getPermission())) {
            throw new CustomException("400", "职称不能为空");
        }
        if ("维修人员".equals(employee.getPermission()) && StrUtil.isBlank(employee.getSpecialty())) {
            throw new CustomException("400", "请选择维修人员工作内容");
        }
        if (employee.getStatus() == null) {
            employee.setStatus(1);
        }
        if (StrUtil.isBlank(employee.getRole())) {
            employee.setRole("EMPLOYEE");
        }
        String rawPassword = employee.getPassword();
        if (!rawPassword.startsWith("$2")) {
            employee.setPassword(passwordEncoder.encode(rawPassword));
        }
        employeeMapper.insert(employee);
    }

    public void updata(Employee employee) {
        employeeMapper.update(employee);
    }

    public void del(Integer id) {
        employeeMapper.del(id);
    }

    public void delBatch(List<Integer> ids) {
        for (Integer id : ids) {
            this.del(id);
        }
    }

    public void updateAvatar(Long employeeId, String avatarUrl) {
        employeeMapper.updateAvatar(employeeId, avatarUrl);
    }

    public String getAvatarUrl(Long employeeId) {
        return employeeMapper.getAvatarUrl(employeeId);
    }

    public boolean checkOldPassword(Long employeeId, String oldPassword) {
        Employee employee = employeeMapper.selectById(employeeId.intValue());
        if (employee == null || employee.getPassword() == null) {
            return false;
        }
        String stored = employee.getPassword();
        if (stored.startsWith("$2")) {
            return passwordEncoder.matches(oldPassword, stored);
        }
        if (passwordEncoder.matches(oldPassword, stored)) {
            return true;
        }
        return stored.equals(oldPassword);
    }

    public void updatePassword(Long employeeId, String newPassword) {
        String hashed = passwordEncoder.encode(newPassword);
        employeeMapper.updatePassword(employeeId, hashed);
    }
}
