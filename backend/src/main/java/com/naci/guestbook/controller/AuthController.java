package com.naci.guestbook.controller;

import com.naci.guestbook.common.response.ApiResponse;
import com.naci.guestbook.common.security.AuthenticatedUser;
import com.naci.guestbook.model.dto.LoginRequest;
import com.naci.guestbook.model.dto.RegisterRequest;
import com.naci.guestbook.model.vo.AuthResultVO;
import com.naci.guestbook.model.vo.CurrentUserVO;
import com.naci.guestbook.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证业务服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户并返回登录令牌和用户信息。
     *
     * @param request 注册请求，包含用户名和密码
     * @return 注册成功后的认证结果
     */
    @PostMapping("/register")
    public ApiResponse<AuthResultVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 校验用户名和密码并签发登录令牌。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录成功后的认证结果
     */
    @PostMapping("/login")
    public ApiResponse<AuthResultVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 查询当前 JWT 对应的用户信息。
     *
     * @param currentUser 由 JWT 认证过滤器注入的当前用户
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(authService.getCurrentUser(currentUser.id()));
    }

    /**
     * 退出登录；服务端不保存 JWT 会话，客户端需自行清除本地令牌。
     *
     * @return 统一成功响应，无业务数据
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
