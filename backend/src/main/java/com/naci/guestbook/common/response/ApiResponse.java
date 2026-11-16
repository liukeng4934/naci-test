package com.naci.guestbook.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.naci.guestbook.common.exception.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "OK";

    private final String code;

    private final String message;

    private final T data;

    /**
     * 创建通用 API 响应对象。
     *
     * @param code 响应码
     * @param message 响应提示
     * @param data 响应数据
     */
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建携带数据的成功响应。
     *
     * @param <T> 数据类型
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 创建不携带数据的成功响应。
     *
     * @return 成功响应
     */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /**
     * 使用自定义错误码和提示创建失败响应。
     *
     * @param <T> 数据类型
     * @param code 错误码
     * @param message 错误提示
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 使用预定义错误码和自定义提示创建失败响应。
     *
     * @param <T> 数据类型
     * @param errorCode 预定义错误码
     * @param message 错误提示
     * @return 失败响应
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return error(errorCode.name(), message);
    }
}
