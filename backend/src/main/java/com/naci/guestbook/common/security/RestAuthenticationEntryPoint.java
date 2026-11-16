package com.naci.guestbook.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naci.guestbook.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 创建 REST 未认证响应入口。
     *
     * @param objectMapper JSON 序列化器
     */
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 向客户端写出 HTTP 401 的统一 JSON 错误响应。
     *
     * @param request 当前请求，可携带 JWT 解析失败原因
     * @param response 当前响应
     * @param authException 认证异常
     * @throws IOException 写出响应失败时抛出
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object authError = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        String message = authError == null ? "请先登录" : authError.toString();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error("UNAUTHORIZED", message));
    }
}
