package com.example.modules.system.accesslog.mapper;

import com.example.entity.AccessLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessLogMapper {
    @Insert("INSERT INTO access_log (user_id, admin_id, employee_id, username, role, method, uri, query, ip, user_agent, status_code, duration_ms, error, create_time) VALUES (#{userId}, #{adminId}, #{employeeId}, #{username}, #{role}, #{method}, #{uri}, #{query}, #{ip}, #{userAgent}, #{statusCode}, #{durationMs}, #{error}, NOW())")
    int insert(AccessLog log);
}
