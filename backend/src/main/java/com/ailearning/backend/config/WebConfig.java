package com.ailearning.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web层配置类。
 * <p>
 * 实现 {@link WebMvcConfigurer} 接口，集中进行以下配置：
 * <ol>
 *   <li><b>CORS跨域</b>：允许来自任意来源的前端请求（开发环境），
 *       支持常用HTTP方法和自定义请求头，同时允许携带Cookie/Authorization等凭证信息。</li>
 *   <li><b>拦截器注册</b>：将 {@link AuthInterceptor} 注册到所有 "/api/**" 路径上，
 *       同时排除登录（/api/user/login）和注册（/api/user/register）接口，
 *       确保未认证用户仍可访问这两个公开接口。</li>
 * </ol>
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 认证拦截器，由Spring自动注入 */
    private final AuthInterceptor authInterceptor;

    /**
     * 构造函数注入拦截器实例。
     *
     * @param authInterceptor 认证拦截器
     */
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 配置CORS跨域映射。
     * <p>
     * 使用 {@code allowedOriginPatterns(" *")} 而非 {@code allowedOrigins(" *")}，
     * 以兼容同时携带 {@code allowCredentials(true)} 的场景（
     * 浏览器在携带凭证时不允许使用通配符origin，originPattern 提供更灵活的模式匹配）。
     * </p>
     *
     * @param registry CORS注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 开发环境放宽跨域限制
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * 注册拦截器。
     * <p>
     * 拦截所有 "/api/**" 请求进行认证，但放行登录和注册接口，
     * 保证新用户可以正常注册和登录后获取令牌。
     * </p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register");
    }
}
