package com.ailearning.backend.config;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器。
 * <p>
 * 实现 Spring MVC 的 {@link HandlerInterceptor} 接口，在请求到达 Controller 之前
 * 进行统一的身份认证。工作流程：
 * <ol>
 *   <li>{@code preHandle}：从请求头中提取 Authorization 令牌，通过
 *       {@link AuthService} 解析出用户ID和角色，存入 {@link AuthContext} 的 ThreadLocal 中；
 *       认证失败则返回401 JSON 响应，并阻止请求继续。</li>
 *   <li>{@code afterCompletion}：请求处理完毕后清除 ThreadLocal 中的认证信息，
 *       防止内存泄漏和线程池复用导致的数据串扰。</li>
 * </ol>
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 认证服务，负责令牌解析和用户ID/角色提取 */
    private final AuthService authService;

    /** JSON序列化工具，用于在拦截器中直接写出错误响应 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，通过Spring依赖注入 AuthService 和 ObjectMapper。
     *
     * @param authService 认证服务
     * @param objectMapper JSON序列化工具
     */
    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * 请求前置处理：验证Authorization令牌，将用户信息写入AuthContext。
     * <p>
     * 认证成功时返回 true，请求继续进入Controller；
     * 认证失败时捕获 {@link ApiException}，直接向响应流写入JSON错误信息并返回 false。
     * </p>
     *
     * @param request  当前HTTP请求
     * @param response 当前HTTP响应
     * @param handler  处理器对象
     * @return true 表示认证通过，false 表示拦截
     * @throws Exception IO异常等
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String header = request.getHeader("Authorization");
            Long userId = authService.requireUserId(header);
            String role = authService.extractRole(header);
            AuthContext.setCurrentUserId(userId);
            AuthContext.setCurrentUserRole(role);
            return true;
        } catch (ApiException exception) {
            // 认证失败，返回JSON格式的错误响应
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(exception.getCode(), exception.getMessage()));
            return false;
        }
    }

    /**
     * 请求完成后的回调：清除 AuthContext 中的 ThreadLocal 数据。
     * <p>
     * 无论请求成功还是失败，此方法都会执行，确保 ThreadLocal 不会造成内存泄漏。
     * 在Servlet容器使用线程池的场景下，这一点尤为重要。
     * </p>
     *
     * @param request  当前HTTP请求
     * @param response 当前HTTP响应
     * @param handler  处理器对象
     * @param ex       请求处理过程中抛出的异常（如有），无异常时为 null
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
