package com.example.modules.system.admin.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {
    List<Admin> selectAll(Admin admin);
    void updateStatus(@Param("adminId") Long adminId, @Param("status") Integer status);
    void updateRole(@Param("adminId") Long adminId, @Param("role") String role);
    void insert(Admin admin);
    void deleteById(@Param("adminId") Long adminId);
}
