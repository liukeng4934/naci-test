package com.naci.guestbook.common.security;

public record AuthenticatedUser(Long id, String username, String displayName) {

    /**
     * 创建并校验认证用户信息；显示名称为空时使用用户名回退。
     *
     * @param id 用户 ID
     * @param username 登录用户名
     * @param displayName 展示名称
     * @throws IllegalArgumentException 用户 ID 为空或用户名为空时抛出
     */
    public AuthenticatedUser {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = username;
        }
    }

    /**
     * 使用用户 ID 和用户名创建认证用户，展示名称默认与用户名相同。
     *
     * @param id 用户 ID
     * @param username 登录用户名
     */
    public AuthenticatedUser(Long id, String username) {
        this(id, username, username);
    }
}
