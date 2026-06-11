package com.ailearning.backend.common;

import com.ailearning.backend.exception.ApiException;

public final class AuthContext {
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ROLE = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setCurrentUserRole(String role) {
        CURRENT_USER_ROLE.set(role);
    }

    public static String getCurrentUserRole() {
        return CURRENT_USER_ROLE.get();
    }

    public static boolean isAdmin() {
        return "admin".equals(CURRENT_USER_ROLE.get());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ApiException(403, "仅管理员可执行此操作");
        }
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USER_ROLE.remove();
    }
}
