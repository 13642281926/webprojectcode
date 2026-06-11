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

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.invalidateToken(authorization);
        return ApiResponse.success("退出成功", null);
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        return ApiResponse.success(userService.getProfile(AuthContext.getCurrentUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("保存成功", userService.updateProfile(AuthContext.getCurrentUserId(), request));
    }
}
