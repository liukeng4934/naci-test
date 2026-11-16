package com.naci.guestbook.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 Bad Request：客户端提交的请求本身有问题，原样重试不会成功
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "请求参数不合法"),            // 参数缺失、越界或取值非法
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "请求参数校验失败"),     // @Valid 校验未通过
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "请求格式不正确"),        // JSON 无法解析、字段类型不匹配

    // 401 Unauthorized：身份未确认，需要重新登录
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "请先登录"),                // 未携带 token，或 token 无效/过期
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "用户名或密码错误"), // 登录时用户名或密码不匹配

    // 403 Forbidden：身份已确认，但没有操作该资源的权限
    FORBIDDEN(HttpStatus.FORBIDDEN, "没有权限执行此操作"),            // 如删除他人的留言或回复

    // 404 Not Found：目标资源不存在
    NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),                    // 通用兜底
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "留言不存在"),            // 按 id 查不到留言
    REPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "回复不存在"),              // 按 id 查不到回复

    // 405 Method Not Allowed：路径存在，但 HTTP 方法不被支持
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),

    // 409 Conflict：与服务端当前数据状态冲突
    CONFLICT(HttpStatus.CONFLICT, "数据冲突"),                        // 通用兜底
    USERNAME_TAKEN(HttpStatus.CONFLICT, "用户名已被使用"),            // uk_users_username 唯一索引冲突
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "数据已被修改，请稍后重试"), // 并发下记录已被删除或变更

    // 500 Internal Server Error：服务端非预期异常，具体原因只记录日志不返回给前端
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus status;

    private final String defaultMessage;

    /**
     * 定义错误码对应的 HTTP 状态和默认提示。
     *
     * @param status 错误码对应的 HTTP 状态
     * @param defaultMessage 默认错误提示
     */
    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
