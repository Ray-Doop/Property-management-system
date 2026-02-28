package com.example.modules.business.travelpass.controller;

import com.example.entity.TravelPassRecord;
import com.example.entity.User;
import com.example.exception.GlobalExceptionHandler;
import com.example.modules.auth.service.LoginRegisterService;
import com.example.modules.business.travelpass.service.TravelPassService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TravelPassControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TravelPassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginRegisterService loginRegisterService;

    @MockBean
    private TravelPassService travelPassService;

    @Test
    void issuePass_returnsQrCode() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("user001");
        when(loginRegisterService.findByUsername(anyString())).thenReturn(user);
        doAnswer(invocation -> {
            TravelPassRecord record = invocation.getArgument(0);
            record.setId(99L);
            return record;
        }).when(travelPassService).createRecord(any(TravelPassRecord.class));

        mockMvc.perform(get("/travel-pass/issue")
                .param("username", "user001")
                .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.recordId").value(99L));
    }

    @Test
    void verifyPass_requiresRecordIdOrFile() throws Exception {
        mockMvc.perform(post("/travel-pass/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
    @Import(TravelPassController.class)
    static class TestConfig {
    }
}
