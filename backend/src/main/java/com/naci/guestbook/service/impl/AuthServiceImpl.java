package com.naci.guestbook.service.impl;

import com.naci.guestbook.common.exception.BusinessException;
import com.naci.guestbook.common.exception.ErrorCode;
import com.naci.guestbook.common.security.JwtUtil;
import com.naci.guestbook.mapper.UserMapper;
import com.naci.guestbook.model.dto.LoginRequest;
import com.naci.guestbook.model.dto.RegisterRequest;
import com.naci.guestbook.model.entity.User;
import com.naci.guestbook.model.vo.AuthResultVO;
import com.naci.guestbook.model.vo.CurrentUserVO;
import com.naci.guestbook.model.vo.UserVO;
import com.naci.guestbook.service.AuthService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final int USERNAME_MIN_LENGTH = 2;

    private static final int USERNAME_MAX_LENGTH = 20;

    private static final int PASSWORD_MIN_LENGTH = 6;

    private static final int PASSWORD_MAX_LENGTH = 64;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    /**
     * 创建认证业务服务。
     *
     * @param userMapper 用户数据访问对象
     * @param passwordEncoder 密码编码器
     * @param jwtUtil JWT 工具
     */
    public AuthServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 校验并规范化注册信息，保存 BCrypt 密码哈希后签发 JWT。
     *
     * @param request 注册请求，包含用户名和密码
     * @return 认证令牌及用户信息
     * @throws BusinessException 用户名校验失败、用户名已存在或数据库唯一约束冲突时抛出
     */
    @Override
    @Transactional
    public AuthResultVO register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        validatePassword(request.getPassword());

        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(username);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN);
        }

        if (user.getId() == null) {
            throw new IllegalStateException("Failed to obtain generated user id");
        }
        return createAuthResult(user);
    }

    /**
     * 校验用户名和密码，认证成功后签发 JWT。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 认证令牌及用户信息
     * @throws BusinessException 参数格式不合法或用户名、密码不匹配时抛出
     */
    @Override
    public AuthResultVO login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        validatePassword(request.getPassword());

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return createAuthResult(user);
    }

    /**
     * 按用户 ID 查询当前登录用户。
     *
     * @param userId 当前用户 ID
     * @return 当前用户信息
     * @throws BusinessException 用户不存在或登录状态失效时抛出
     */
    @Override
    public CurrentUserVO getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        return new CurrentUserVO(toUserVO(user));
    }

    /**
     * 为用户生成 JWT，并组装认证结果。
     *
     * @param user 已认证用户实体
     * @return 令牌及用户信息
     */
    private AuthResultVO createAuthResult(User user) {
        return new AuthResultVO(jwtUtil.generateToken(user), toUserVO(user));
    }

    /**
     * 将用户实体转换为对外用户视图。
     *
     * @param user 用户实体
     * @return 仅包含用户 ID 和用户名的视图对象
     */
    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), user.getUsername());
    }

    /**
     * 去除用户名首尾空白并校验长度。
     *
     * @param rawUsername 原始用户名
     * @return 规范化后的用户名
     * @throws BusinessException 用户名长度不在 2-20 个字符范围内时抛出
     */
    private String normalizeUsername(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户名长度需为 2-20 个字符");
        }
        return username;
    }

    /**
     * 校验密码长度。
     *
     * @param password 原始密码
     * @throws BusinessException 密码为空或长度不在 6-64 个字符范围内时抛出
     */
    private void validatePassword(String password) {
        if (password == null
                || password.length() < PASSWORD_MIN_LENGTH
                || password.length() > PASSWORD_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "密码长度需为 6-64 个字符");
        }
    }
}
