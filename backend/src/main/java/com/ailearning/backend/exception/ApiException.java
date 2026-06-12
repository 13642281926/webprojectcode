package com.ailearning.backend.exception;

/**
 * 自定义业务异常类。
 * <p>
 * 继承自 {@link RuntimeException}（非受检异常），携带业务错误码（code）和错误消息（message）。
 * 在Service/Controller层中，当遇到业务逻辑错误时抛出此异常，由
 * {@link GlobalExceptionHandler} 统一捕获并转换为标准的 {@link com.ailearning.backend.common.ApiResponse} 响应。
 * </p>
 *
 * <h3>常用错误码</h3>
 * <ul>
 *   <li>400 - 请求参数错误（业务校验失败）</li>
 *   <li>401 - 未认证（令牌无效或过期）</li>
 *   <li>403 - 权限不足（非管理员操作管理接口）</li>
 *   <li>404 - 资源不存在</li>
 *   <li>409 - 资源冲突（如用户名已存在）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * if (user == null) {
 *     throw new ApiException(401, "用户名或密码错误");
 * }
 * }</pre>
 *
 * @author AI学习成长助手平台
 */
public class ApiException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 构造业务异常。
     *
     * @param code    业务错误码（如 401、403、404 等）
     * @param message 错误描述信息（将直接返回给前端）
     */
    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
