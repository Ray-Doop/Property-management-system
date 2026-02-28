package com.example.modules.auth.controller;

import com.example.entity.User;
import com.example.exception.GlobalExceptionHandler;
import com.example.modules.auth.service.LoginRegisterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserAdminControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "wechat.appid=test_appid",
        "wechat.secret=test_secret"
})
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginRegisterService loginRegisterService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void wechatLogin_requiresCode() throws Exception {
        mockMvc.perform(post("/LoginRegister/wechatLogin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void wechatLogin_returnsUserWhenOpenidExists() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("user001");
        when(loginRegisterService.loginByOpenid(anyString())).thenReturn(user);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"openid\":\"OPENID_TEST\"}");

        mockMvc.perform(post("/LoginRegister/wechatLogin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"valid_wechat_code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.username").value("user001"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
    @ComponentScan(basePackageClasses = UserAdminController.class)
    static class TestConfig {
    }
}
