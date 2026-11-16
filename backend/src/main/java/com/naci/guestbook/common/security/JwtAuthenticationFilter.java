package com.naci.guestbook.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "jwtAuthenticationError";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    /**
     * 创建 JWT 认证过滤器。
     *
     * @param jwtUtil JWT 解析工具
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 从请求头解析 JWT，并在令牌有效时写入当前请求的安全上下文。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器处理失败时抛出
     * @throws IOException 读写请求或响应失败时抛出
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                authenticate(request, jwtUtil.parseToken(token));
            } catch (JwtException | IllegalArgumentException exception) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "登录状态已失效，请重新登录");
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 将认证用户写入 Spring Security 上下文，并补充请求详情。
     *
     * @param request 当前 HTTP 请求
     * @param principal 已通过 JWT 验证的用户
     */
    private void authenticate(HttpServletRequest request, AuthenticatedUser principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 从 Authorization 请求头中提取 Bearer 令牌。
     *
     * @param request 当前 HTTP 请求
     * @return JWT 字符串；请求头缺失、格式不符或令牌为空时返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
