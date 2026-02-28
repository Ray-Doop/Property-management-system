package com.example.entity;

import lombok.Data;

@Data
public class AccessLog {
    private Long id;
    private Long userId;
    private Long adminId;
    private Long employeeId;
    private String username;
    private String role;
    private String method;
    private String uri;
    private String query;
    private String ip;
    private String userAgent;
    private Integer statusCode;
    private Long durationMs;
    private String error;
    private String createTime;
}
