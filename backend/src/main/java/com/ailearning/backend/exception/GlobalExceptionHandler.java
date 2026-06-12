package com.ailearning.backend.exception;

import com.ailearning.backend.common.ApiResponse;
import javax.validation.ConstraintViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 使用 {@code @RestControllerAdvice} 注解，拦截所有 Controller 抛出的异常，
 * 将异常信息统一转换为标准的 {@link ApiResponse} JSON 响应格式返回给前端。
 * 这样前端无需分别处理不同的异常类型，只需根据统一格式中的 code 和 message 做展示。
 * </p>
 *
 * <h3>异常处理层次（优先级从高到低）</h3>
 * <ol>
 *   <li>{@link ApiException} — 自定义业务异常，直接使用其 code 和 message</li>
 *   <li>{@link MethodArgumentNotValidException} — {@code @Valid} 校验失败的请求体DTO，
 *       收集所有字段错误信息合并返回</li>
 *   <li>{@link ConstraintViolationException} — 方法参数级别校验失败</li>
 *   <li>{@link Exception} — 兜底处理器，捕获所有未处理的异常，
 *       返回 500 加服务器异常提示</li>
 * </ol>
 *
 * @author AI学习成长助手平台
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常 {@link ApiException}。
     * 直接将异常的 code 和 message 封装为 ApiResponse 返回。
     *
     * @param exception 业务异常
     * @return 包含错误码和消息的 ApiResponse
     */
    @ExceptionHandler(ApiException.class)
    public ApiResponse<Void> handleApiException(ApiException exception) {
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理 {@code @Valid} 注解触发的请求体校验失败异常。
     * <p>
     * 收集所有字段的错误消息，用逗号拼接后统一返回，
     * 方便前端一次性展示所有校验错误（如"账号长度 3-20 位, 密码长度 6-32 位"）。
     * </p>
     *
     * @param exception 校验异常
     * @return code=400 的 ApiResponse，message 为所有字段错误拼接
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.fail(400, message);
    }

    /**
     * 处理方法参数校验失败异常（通常在Controller方法参数上使用 {@code @Validated} 时触发）。
     *
     * @param exception 约束违反异常
     * @return code=400 的 ApiResponse
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    /**
     * 兜底异常处理器，捕获所有未在上述处理器中匹配的异常。
     * <p>
     * 返回 500 状态码，消息默认为"服务器异常"，
     * 实际开发中应配合日志框架记录完整堆栈以便排查问题。
     * </p>
     *
     * @param exception 未知异常
     * @return code=500 的 ApiResponse
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.fail(500, exception.getMessage() == null ? "服务器异常" : exception.getMessage());
    }
}
