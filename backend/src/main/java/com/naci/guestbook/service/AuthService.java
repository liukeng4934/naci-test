package com.naci.guestbook.service;

import com.naci.guestbook.model.dto.LoginRequest;
import com.naci.guestbook.model.dto.RegisterRequest;
import com.naci.guestbook.model.vo.AuthResultVO;
import com.naci.guestbook.model.vo.CurrentUserVO;

public interface AuthService {

    /**
     * 注册新用户，并在成功后签发 JWT。
     *
     * @param request 注册请求，包含用户名和密码
     * @return 认证令牌及用户信息
     */
    AuthResultVO register(RegisterRequest request);

    /**
     * 校验登录凭据，并在成功后签发 JWT。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 认证令牌及用户信息
     */
    AuthResultVO login(LoginRequest request);

    /**
     * 查询当前登录用户信息。
     *
     * @param userId 当前用户 ID
     * @return 当前用户信息
     */
    CurrentUserVO getCurrentUser(Long userId);
}
