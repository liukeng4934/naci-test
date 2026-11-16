package com.naci.guestbook.common.exception;

import com.naci.guestbook.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将业务异常转换为统一的错误响应。
     *
     * @param exception 业务异常
     * @return 带有业务状态码和提示的错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    /**
     * 处理请求参数绑定失败，返回首个字段校验错误。
     *
     * @param exception 参数绑定异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
        return validationError(message);
    }

    /**
     * 处理方法参数校验失败，返回首个校验错误提示。
     *
     * @param exception 方法参数校验异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidationException(
            HandlerMethodValidationException exception) {
        Stream<MessageSourceResolvable> parameterErrors = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream());
        Stream<MessageSourceResolvable> crossParameterErrors =
                exception.getCrossParameterValidationResults().stream();
        String message = Stream.concat(parameterErrors, crossParameterErrors)
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
        return validationError(message);
    }

    /**
     * 处理约束校验失败，返回首个违规提示。
     *
     * @param exception 约束校验异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
        return validationError(message);
    }

    /**
     * 处理无法解析的请求体。
     *
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage() {
        return error(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST.name(),
                "请求体格式不正确");
    }

    /**
     * 处理请求参数缺失或绑定失败。
     *
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiResponse<Void>> handleRequestBindingException() {
        return error(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST.name(),
                "请求参数缺失或格式不正确");
    }

    /**
     * 处理方法参数类型不匹配。
     *
     * @param exception 参数类型不匹配异常
     * @return HTTP 400 错误响应，提示具体参数名称
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_REQUEST.name(),
                "参数 " + exception.getName() + " 类型不正确");
    }

    /**
     * 处理认证失败。
     *
     * @return HTTP 401 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException() {
        return error(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.name(),
                "请先登录或登录已过期");
    }

    /**
     * 处理访问权限不足。
     *
     * @return HTTP 403 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException() {
        return error(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN.name(),
                "没有权限执行此操作");
    }

    /**
     * 处理数据库约束冲突。
     *
     * @param exception 数据完整性异常
     * @return HTTP 409 错误响应
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        log.warn("Database constraint violation", exception);
        return error(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT.name(),
                "数据已存在或违反约束");
    }

    /**
     * 处理访问不存在的静态资源或接口。
     *
     * @return HTTP 404 错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound() {
        return error(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.name(), "接口不存在");
    }

    /**
     * 处理不支持的 HTTP 请求方法。
     *
     * @return HTTP 405 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported() {
        return error(
                HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCode.METHOD_NOT_ALLOWED.name(),
                ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage());
    }

    /**
     * 兜底处理未被其他处理器匹配的异常。
     *
     * @param exception 未处理异常
     * @return HTTP 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.name(),
                "服务器内部错误");
    }

    /**
     * 构造参数校验错误响应。
     *
     * @param message 校验错误提示
     * @return HTTP 400 错误响应
     */
    private ResponseEntity<ApiResponse<Void>> validationError(String message) {
        return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), message);
    }

    /**
     * 构造统一错误响应。
     *
     * @param status HTTP 状态
     * @param code 错误码
     * @param message 错误提示
     * @return 带状态码和错误体的响应
     */
    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
