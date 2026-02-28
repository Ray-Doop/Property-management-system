package com.example.modules.system.loginlog.mapper;

import com.github.pagehelper.PageInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogMapper {
    List<Map<String, Object>> selectPage(@Param("username") String username,
                                         @Param("role") String role);
}
