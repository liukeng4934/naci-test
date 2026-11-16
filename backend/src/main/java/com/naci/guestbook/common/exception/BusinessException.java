package com.naci.guestbook.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    private final HttpStatus status;

    /**
     * 使用错误码的默认 HTTP 状态和默认提示信息创建业务异常。
     *
     * @param errorCode 预定义错误码
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.name(), errorCode.getStatus(), errorCode.getDefaultMessage());
    }

    /**
     * 使用错误码和自定义提示信息创建业务异常；提示为空时回退到错误码默认提示。
     *
     * @param errorCode 预定义错误码
     * @param message 自定义错误提示
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(
                errorCode.name(),
                errorCode.getStatus(),
                message == null || message.isBlank() ? errorCode.getDefaultMessage() : message
        );
    }

    /**
     * 使用整数 HTTP 状态码创建业务异常。
     *
     * @param status HTTP 状态码
     * @param code 错误码名称
     * @param message 错误提示
     */
    public BusinessException(int status, String code, String message) {
        this(code, resolveStatus(status), message);
    }

    /**
     * 使用完整错误信息创建业务异常。
     *
     * @param code 错误码名称
     * @param status HTTP 状态
     * @param message 错误提示
     */
    public BusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * 创建 HTTP 400 业务异常。
     *
     * @param message 错误提示
     * @return 新的业务异常实例
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    /**
     * 创建 HTTP 401 业务异常。
     *
     * @param message 错误提示
     * @return 新的业务异常实例
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    /**
     * 创建 HTTP 403 业务异常。
     *
     * @param message 错误提示
     * @return 新的业务异常实例
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }

    /**
     * 创建 HTTP 404 业务异常。
     *
     * @param message 错误提示
     * @return 新的业务异常实例
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 创建 HTTP 409 业务异常。
     *
     * @param message 错误提示
     * @return 新的业务异常实例
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }

    /**
     * 将整数状态码解析为 Spring 的 HTTP 状态对象。
     *
     * @param status HTTP 状态码
     * @return 对应的 HTTP 状态；无法识别时返回 500
     */
    private static HttpStatus resolveStatus(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
    }
}
