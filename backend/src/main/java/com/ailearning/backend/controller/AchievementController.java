package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.entity.Achievement;
import com.ailearning.backend.service.AchievementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievement")
public class AchievementController {
    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Achievement>> list() {
        return ApiResponse.success(achievementService.list(AuthContext.getCurrentUserId()));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(achievementService.stats(AuthContext.getCurrentUserId()));
    }

    @PostMapping("/unlock/{id}")
    public ApiResponse<Void> unlock(@PathVariable Long id) {
        achievementService.unlock(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("解锁成功", null);
    }

    @PostMapping("/init")
    public ApiResponse<Void> init(@RequestParam(defaultValue = "false") boolean reset) {
        achievementService.initAchievements(AuthContext.getCurrentUserId(), reset);
        return ApiResponse.success("初始化成功", null);
    }
}
