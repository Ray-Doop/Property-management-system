package com.example.modules.system.accesslog;

import com.example.entity.AccessLog;
import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import com.example.modules.system.accesslog.mapper.AccessLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Configuration
public class AccessLogWebConfig implements WebMvcConfigurer {

    @Resource
    private AccessLogMapper accessLogMapper;

    @Resource
    @org.springframework.beans.factory.annotation.Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessLogInterceptor(accessLogMapper, asyncTaskExecutor));
    }

    private static class AccessLogInterceptor implements HandlerInterceptor {
        private final AccessLogMapper accessLogMapper;
        private final Executor executor;

        AccessLogInterceptor(AccessLogMapper accessLogMapper, Executor executor) {
            this.accessLogMapper = accessLogMapper;
            this.executor = executor;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute("accessLogStartTime", System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            Long start = (Long) request.getAttribute("accessLogStartTime");
            long duration = start == null ? 0L : System.currentTimeMillis() - start;
            AccessLog log = new AccessLog();
            fillUserInfo(log);
            log.setMethod(request.getMethod());
            log.setUri(request.getRequestURI());
            log.setQuery(buildQueryString(request));
            log.setIp(resolveIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            int status = response.getStatus();
            if (ex != null && status < 500) {
                status = 500;
            }
            log.setStatusCode(status);
            log.setDurationMs(duration);
            if (ex != null) {
                String msg = ex.getMessage();
                if (msg != null && msg.length() > 500) {
                    msg = msg.substring(0, 500);
                }
                log.setError(msg);
            }
            CompletableFuture.runAsync(() -> {
                try {
                    accessLogMapper.insert(log);
                } catch (Exception ignored) {
                }
            }, executor);
        }

        private void fillUserInfo(AccessLog log) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return;
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof Admin admin) {
                log.setAdminId(admin.getAdminId());
                log.setUsername(admin.getUsername());
            } else if (principal instanceof Employee employee) {
                log.setEmployeeId(employee.getEmployeeId());
                log.setUsername(employee.getUsername());
            } else if (principal instanceof User user) {
                log.setUserId(user.getUserId());
                log.setUsername(user.getUsername());
            }
            List<String> roles = new ArrayList<>();
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority != null && authority.getAuthority() != null) {
                    roles.add(authority.getAuthority());
                }
            }
            if (!roles.isEmpty()) {
                log.setRole(String.join(",", roles));
            }
        }

        private String buildQueryString(HttpServletRequest request) {
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isBlank()) {
                return trimValue(queryString, 2000);
            }
            Map<String, String[]> params = request.getParameterMap();
            if (params == null || params.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                if (values == null || values.length == 0) {
                    appendParam(sb, key, "");
                } else {
                    for (String value : values) {
                        appendParam(sb, key, value);
                    }
                }
            }
            return trimValue(sb.toString(), 2000);
        }

        private void appendParam(StringBuilder sb, String key, String value) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value == null ? "" : value);
        }

        private String resolveIp(HttpServletRequest request) {
            String ip = headerValue(request, "X-Forwarded-For");
            if (ip == null) ip = headerValue(request, "X-Real-IP");
            if (ip == null) ip = headerValue(request, "Proxy-Client-IP");
            if (ip == null) ip = headerValue(request, "WL-Proxy-Client-IP");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        private String headerValue(HttpServletRequest request, String header) {
            String value = request.getHeader(header);
            if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
                return null;
            }
            return value;
        }

        private String trimValue(String value, int max) {
            if (value == null) {
                return null;
            }
            if (value.length() <= max) {
                return value;
            }
            return value.substring(0, max);
        }
    }
}
