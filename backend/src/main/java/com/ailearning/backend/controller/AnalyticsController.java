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

/**
 * 数据分析控制器，提供学习数据概览、仪表盘面板、任务统计和资源统计等分析接口。
 * <p>
 * 该控制器聚合了多个业务服务的统计数据，为前端的数据分析视图和首页仪表盘提供多维度的
 * 学习数据洞察。通过调用 AnalyticsService 获取整体学习概览（学习时长、笔记数、课程进度等），
 * 调用 DashboardService 获取仪表盘核心面板数据，调用 StudyPlanService 获取任务完成分布，
 * 调用 ResourceService 获取资源使用统计。所有数据均按当前登录用户隔离。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final StudyPlanService studyPlanService;
    private final ResourceService resourceService;

    /**
     * 构造函数，通过 Spring IoC 注入多个业务服务。
     *
     * @param analyticsService 学习分析服务
     * @param dashboardService 仪表盘数据服务
     * @param studyPlanService 学习计划服务
     * @param resourceService  资源管理服务
     */
    public AnalyticsController(AnalyticsService analyticsService,
                               DashboardService dashboardService,
                               StudyPlanService studyPlanService,
                               ResourceService resourceService) {
        this.analyticsService = analyticsService;
        this.dashboardService = dashboardService;
        this.studyPlanService = studyPlanService;
        this.resourceService = resourceService;
    }

    /**
     * 获取当前用户的学习数据综合概览。
     * 返回学习总时长、笔记数量、课程进度、学习天数、成就进度等核心指标的汇总数据，
     * 用于在前端"数据分析"页面的概览卡片中展示。
     *
     * @return 包含各项学习指标汇总数据的键值对结构
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(analyticsService.overview(AuthContext.getCurrentUserId()));
    }

    /**
     * 获取首页与数据分析共用的核心仪表盘面板数据。
     * 该接口底层与 DashboardService 一致，返回用户的学习统计数据，
     * 包括笔记数、错题数、计划数、资源数、学习天数等聚合指标。
     *
     * @param range 时间范围筛选（可选），保留参数用于未来扩展（如"本周"/"本月"）
     * @return 包含仪表盘各模块统计数据的键值对结构
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@RequestParam(required = false) String range) {
        return ApiResponse.success(dashboardService.stats(AuthContext.getCurrentUserId()));
    }

    /**
     * 获取学习任务的完成状态分布统计。
     * 返回用户学习计划中已完成、未完成、按优先级分布等统计数据，
     * 供前端数据分析视图中的任务图表（如饼图、柱状图）使用。
     *
     * @param range 时间范围筛选（可选），保留参数用于未来按时间过滤
     * @return 包含任务完成分布统计数据的键值对结构
     */
    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> tasks(@RequestParam(required = false) String range) {
        Long userId = AuthContext.getCurrentUserId();
        var stats = studyPlanService.stats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 获取学习资源的分类和使用统计。
     * 返回用户上传资源的类型分布、总文件大小、各类别数量等聚合数据，
     * 供前端数据分析视图中的资源统计图表使用。
     *
     * @param range 时间范围筛选（可选），保留参数用于未来按时间过滤
     * @return 包含资源统计数据的键值对结构
     */
    @GetMapping("/resources")
    public ApiResponse<Map<String, Object>> resources(@RequestParam(required = false) String range) {
        Long userId = AuthContext.getCurrentUserId();
        return ApiResponse.success(resourceService.stats(userId));
    }
}
