package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.service.AnalyticsService;
import com.ailearning.backend.service.DashboardService;
import com.ailearning.backend.service.ResourceService;
import com.ailearning.backend.service.StudyPlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final StudyPlanService studyPlanService;
    private final ResourceService resourceService;

    public AnalyticsController(AnalyticsService analyticsService,
                               DashboardService dashboardService,
                               StudyPlanService studyPlanService,
                               ResourceService resourceService) {
        this.analyticsService = analyticsService;
        this.dashboardService = dashboardService;
        this.studyPlanService = studyPlanService;
        this.resourceService = resourceService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(analyticsService.overview(AuthContext.getCurrentUserId()));
    }

    /** 首页 / 数据分析 共用的核心面板数据（与DashboardService 一致） */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam(required = false) String range) {
        return ApiResponse.success(dashboardService.stats(AuthContext.getCurrentUserId()));
    }

    /** 任务完成分布（前端AnalyticsView 用） */
    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> tasks(@RequestParam(required = false) String range) {
        Long userId = AuthContext.getCurrentUserId();
        var stats = studyPlanService.stats(userId);
        return ApiResponse.success(stats);
    }

    /** 资源统计 */
    @GetMapping("/resources")
    public ApiResponse<Map<String, Object>> resources(@RequestParam(required = false) String range) {
        Long userId = AuthContext.getCurrentUserId();
        return ApiResponse.success(resourceService.stats(userId));
    }
}
