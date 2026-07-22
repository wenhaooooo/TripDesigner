package com.tripdesigner.auth.api;

import com.tripdesigner.auth.api.dto.TokenResponse;
import com.tripdesigner.auth.application.AuthAppService;
import com.tripdesigner.common.exception.BizException;
import com.tripdesigner.common.exception.GlobalExceptionHandler;
import com.tripdesigner.common.response.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {AuthController.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean AuthAppService service;

    @Test
    void register_returns_token() throws Exception {
        when(service.register("a@b.com", "password123"))
                .thenReturn(TokenResponse.of("acc", "ref", 900));
        mvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void register_validates_short_password() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.com\",\"password\":\"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_maps_biz_exception_to_success_envelope_with_error_code() throws Exception {
        when(service.login(any(), any()))
                .thenThrow(new BizException(ResultCode.AUTH_INVALID_CREDENTIALS));
        mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }
}