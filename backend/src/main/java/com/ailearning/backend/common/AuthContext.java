package com.ailearning.backend.common;

import com.ailearning.backend.exception.ApiException;

/**
 * 认证上下文工具类（基于 ThreadLocal 的线程级用户信息持有者）。
 * <p>
 * 设计为不可实例化的工具类（private 构造函数），通过静态方法提供线程绑定的用户身份信息。
 * 在 {@link com.ailearning.backend.config.AuthInterceptor AuthInterceptor} 的
 * {@code preHandle} 中设置用户ID和角色，在 {@code afterCompletion} 中清除，
 * 保证一个请求线程的全生命周期内都可以随时获取当前登录用户的信息。
 * </p>
 *
 * <h3>ThreadLocal 设计考量</h3>
 * <ul>
 *   <li><b>线程隔离</b>：每个HTTP请求由独立的线程处理，使用 ThreadLocal 确保
 *       并发请求之间的用户信息互不干扰。</li>
 *   <li><b>必须清理</b>：Servlet 容器（如 Tomcat）使用线程池，线程会被复用。
 *       如果不清除 ThreadLocal，复用时可能残留上一个请求的用户数据，造成严重的数据串扰问题。
 *       因此 {@code clear()} 方法必须在拦截器的 afterCompletion 中被调用。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在Service层获取当前登录用户ID
 * Long currentUserId = AuthContext.getCurrentUserId();
 *
 * // 在需要管理员权限的方法开头做权限校验
 * AuthContext.requireAdmin();
 * }</pre>
 *
 * @author AI学习成长助手平台
 */
public final class AuthContext {

    /** 存放当前线程的用户ID */
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    /** 存放当前线程的用户角色（"user" 或 "admin"） */
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    /**
     * 私有构造函数，防止外部实例化。
     */
    private AuthContext() {
    }

    /**
     * 设置当前线程的用户ID。
     * 由 {@code AuthInterceptor} 在请求进入时调用。
     *
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前线程的用户ID。
     * 在请求处理期间的任何位置都可以调用此方法获取已认证的用户ID。
     *
     * @return 当前用户ID，未认证时返回 null
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 设置当前线程的用户角色。
     * 由 {@code AuthInterceptor} 在请求进入时调用。
     *
     * @param role 用户角色："user" 或 "admin"
     */
    public static void setCurrentUserRole(String role) {
        CURRENT_USER_ROLE.set(role);
    }

    /**
     * 获取当前线程的用户角色。
     *
     * @return 当前用户角色："user" 或 "admin"，未认证时返回 null
     */
    public static String getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    /**
     * 判断当前用户是否为管理员。
     *
     * @return true 表示当前用户是 admin 角色，false 表示不是或未认证
     */
    public static boolean isAdmin() {
        return "admin".equals(CURRENT_USER_ROLE.get());
    }

    /**
     * 要求当前用户必须是管理员，否则抛出 403 异常。
     * <p>
     * 用于需要管理员权限的Service/Controller方法开头，
     * 替代在每个方法中重复编写权限判断逻辑。
     * </p>
     *
     * @throws ApiException 当前用户不是管理员时抛出，code=403
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ApiException(403, "仅管理员可执行此操作");
        }
    }

    /**
     * 清除当前线程的所有认证信息。
     * <p>
     * 由 {@code AuthInterceptor.afterCompletion} 调用，
     * 确保 ThreadLocal 在请求结束后被清理，防止内存泄漏和线程池数据串扰。
     * </p>
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
