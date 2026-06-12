package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.dto.LoginRequest;
import com.ailearning.backend.dto.ProfileUpdateRequest;
import com.ailearning.backend.dto.RegisterRequest;
import com.ailearning.backend.service.AuthService;
import com.ailearning.backend.service.UserService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器，负责处理用户注册、登录、退出及个人信息管理相关的 HTTP 请求。
 * <p>
 * 该控制器是平台的用户入口，所有与用户身份认证和账号信息相关的操作均在此处理。
 * 登录成功后返回 JWT 令牌，后续请求通过 Authorization 请求头携带令牌完成身份校验。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    /**
     * 构造函数，通过 Spring IoC 注入用户服务与认证服务。
     *
     * @param userService 用户业务服务
     * @param authService 认证与令牌管理服务
     */
    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    /**
     * 用户登录接口。
     * 验证用户提交的用户名和密码，验证通过后生成并返回 JWT 访问令牌，
     * 同时返回用户基本信息（如昵称、头像、角色等）。
     *
     * @param request 登录请求体，包含用户名和密码
     * @return 包含 JWT 令牌和用户基本信息的响应结果
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    /**
     * 用户注册接口。
     * 校验注册信息的合法性（用户名唯一性、密码强度等），创建新用户账号，
     * 注册成功后自动完成登录并返回 JWT 令牌。
     *
     * @param request 注册请求体，包含用户名、密码、确认密码等信息
     * @return 包含 JWT 令牌和新用户基本信息的响应结果
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    /**
     * 用户退出登录接口。
     * 将当前请求携带的 JWT 令牌加入黑名单或失效列表，使其无法再用于认证，
     * 确保退出后令牌不可复用，提升系统安全性。
     *
     * @param authorization 请求头中的 Authorization 字段，格式为 "Bearer {token}"
     * @return 退出成功的空响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.invalidateToken(authorization);
        return ApiResponse.success("退出成功", null);
    }

    /**
     * 获取当前登录用户的个人信息。
     * 从认证上下文中获取当前用户 ID，查询并返回用户的详细资料，
     * 包括昵称、头像、邮箱、角色、学习统计等。
     *
     * @return 当前用户的个人资料信息
     */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        // 从线程绑定的认证上下文中获取当前登录用户ID
        return ApiResponse.success(userService.getProfile(AuthContext.getCurrentUserId()));
    }

    /**
     * 更新当前登录用户的个人信息。
     * 支持修改昵称、头像、密码等字段，部分敏感字段（如角色）不允许自行修改。
     *
     * @param request 个人信息更新请求体，包含待修改的字段
     * @return 更新后的用户资料信息
     */
    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("保存成功", userService.updateProfile(AuthContext.getCurrentUserId(), request));
    }
}
