package com.ailearning.backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应封装类。
 * <p>
 * 泛型类 {@code <T>}，为整个平台提供标准化的HTTP响应格式。
 * 所有Controller的返回值都封装为 ApiResponse 对象，前端据此统一处理。
 * </p>
 *
 * <h3>响应结构</h3>
 * <pre>{@code
 * {
 *   "code": 200,        // 业务状态码：200成功，其他为错误码
 *   "message": "ok",    // 提示信息
 *   "data": { ... }     // 泛型响应数据，失败时为 null
 * }
 * }</pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 成功返回数据
 * return ApiResponse.success(userInfo);
 *
 * // 成功返回自定义消息
 * return ApiResponse.success("登录成功", tokenInfo);
 *
 * // 失败返回错误信息
 * return ApiResponse.fail(401, "用户名或密码错误");
 * }</pre>
 *
 * @param <T> 响应数据的类型
 * @author AI学习成长助手平台
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 业务状态码：200 表示成功，其他值（如 400/401/403/500）表示各类错误 */
    private int code;

    /** 提示信息，成功时默认为 "ok"，失败时携带具体错误原因 */
    private String message;

    /** 响应数据，泛型，成功时携带业务数据，失败时为 null */
    private T data;

    /**
     * 创建成功响应（默认消息 "ok"）。
     *
     * @param <T>  数据类型
     * @param data 业务数据
     * @return 包含 data 的成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    /**
     * 创建成功响应（自定义消息）。
     *
     * @param <T>     数据类型
     * @param message 自定义成功提示
     * @param data    业务数据
     * @return 包含自定义消息和 data 的成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 创建失败响应。
     *
     * @param <T>     数据类型
     * @param code    业务错误码（如 400/401/403/500）
     * @param message 错误描述信息
     * @return 包含错误码和消息的失败响应（data = null）
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
