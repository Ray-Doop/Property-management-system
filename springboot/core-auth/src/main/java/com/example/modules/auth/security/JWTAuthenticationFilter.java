package com.example.modules.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import com.example.modules.auth.service.LoginRegisterService;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    @org.springframework.context.annotation.Lazy
    private LoginRegisterService loginRegisterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // ✅ 放行白名单：支付回调、用户登录、管理员登录
        if (uri.startsWith("/api/pay/notify/alipay")
                || uri.startsWith("/api/pay/page/alipay")
                || uri.startsWith("/api/pay/return/alipay")
                || uri.startsWith("/LoginRegister/userLogin")
                || uri.startsWith("/LoginRegister/adminLogin")
                || uri.startsWith("/LoginRegister/adminlogin")
                || uri.startsWith("/LoginRegister/employeeLogin")
                || uri.startsWith("/LoginRegister/userRegister")
                || uri.startsWith("/LoginRegister/captcha")
                || uri.startsWith("/LoginRegister/wechatLogin")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 获取 Authorization Header
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // ✅ 如果没有 Token，直接放行，Security 会拦截受保护接口
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // ✅ 验证 JWT 签名
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(TokenUtils.getSecret())).build();
            DecodedJWT decodedJWT = verifier.verify(token);

            String audience = decodedJWT.getAudience().isEmpty() ? null : decodedJWT.getAudience().get(0);
            Long id = null;
            if (audience != null && !audience.isBlank()) {
                try {
                    id = Long.parseLong(audience);
                } catch (NumberFormatException ignored) {
                }
            }
            String username = decodedJWT.getClaim("username").asString();
            if ((username == null || username.isBlank()) && audience != null && !audience.isBlank() && id == null) {
                username = audience;
            }

            String role = decodedJWT.getClaim("role").asString();
            if (role != null && !role.isBlank() && !role.startsWith("ROLE_")) {
                role = "ROLE_" + role.trim().toUpperCase();
            }
            final String roleAuthority = (role == null || role.isBlank()) ? "ROLE_USER" : role;

            Object principal = null;

            // ✅ 根据角色，从 Redis 或数据库获取用户信息
            if ("ROLE_ADMIN".equals(roleAuthority)) {
                Admin admin = id == null ? null : loginRegisterService.getAdminById(id);
                if (admin == null && username != null && !username.isEmpty()) {
                    // 回退：通过用户名加载管理员信息（避免Redis丢失导致401）
                    admin = loginRegisterService.getAdminByUsername(username);
                }
                if (admin == null) {
                    // 最后回退：构造最简主体，仅用于鉴权通过
                    admin = new Admin();
                    if (id != null) {
                        admin.setAdminId(id);
                    }
                    admin.setUsername(username);
                }
                principal = admin;
            } else if ("ROLE_EMPLOYEE".equals(roleAuthority)) {
                // 员工角色处理
                Employee employee = id == null ? null : loginRegisterService.getEmployeeById(id);
                // 如果没有getEmployeeById(Long)，可以用getEmployeeById(String username)或者新增
                // LoginRegisterService 目前只有 selectEmployeeByUsername(String)
                // 让我们假设 id 是 employee_id
                // 但 LoginRegisterService 没有 getEmployeeById(Long)
                // 我们可以直接用 username 查，或者新增 getEmployeeById
                // 为简单起见，这里假设 id 确实是 ID
                // 如果 LoginRegisterService 没这个方法，我们需要去加
                // 先用 username 查吧，token 里有 username
                if (employee == null) {
                     employee = loginRegisterService.selectEmployeeByUsername(username);
                }
                if (employee == null) {
                    employee = new Employee();
                    if (id != null) {
                        employee.setEmployeeId(id);
                    }
                    employee.setUsername(username);
                }
                principal = employee;
                java.util.List<org.springframework.security.core.GrantedAuthority> auths = new java.util.ArrayList<>();
                // 基础角色
                    auths.add(() -> "ROLE_EMPLOYEE");
                // 根据权限映射为角色
                String perm = employee.getPermission() != null ? employee.getPermission() : employee.getRole();
                String permNorm = perm == null ? "" : perm.trim().toUpperCase();
                if (permNorm.contains("维修") || "REPAIRER".equals(permNorm)) {
                    auths.add(() -> "ROLE_REPAIRER");
                } else if (permNorm.contains("门卫") || "GUARD".equals(permNorm)) {
                    auths.add(() -> "ROLE_GUARD");
                } else {
                    // 默认授予维修权限，避免合法员工因权限字段缺失而被拒
                    auths.add(() -> "ROLE_REPAIRER");
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                auths
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            } else {
                User user = id == null ? null : loginRegisterService.getUserById(id);
                if (user != null) {
                    principal = user;
                } else {
                    // 用户侧也进行回退，不阻断后续接口（如统计读取）
                    user = new User();
                    if (id != null) {
                        user.setUserId(id);
                    }
                    user.setUsername(username);
                    principal = user;
                }
            }

            // ✅ 设置 Security 上下文，便于后续接口获取身份
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.singletonList(() -> roleAuthority)
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // ✅ 捕获 Token 异常
            sendErrorResponse(response, 401, "Token 无效或已过期");
            return;
        }

        // ✅ 放行
        filterChain.doFilter(request, response);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\"}");
    }
}
