package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页仪表盘控制器，提供平台首页的核心统计面板数据。
 * <p>
 * 仪表盘是用户进入平台后首先看到的数据概览页面，展示用户个人学习状态的各项关键指标。
 * 该控制器从 DashboardService 获取聚合统计数据，包括但不限于：笔记数量、错题数量、
 * 学习计划完成情况、上传资源数量、累计学习天数、成就解锁进度等。
 * 所有数据与当前登录用户绑定，确保数据的私密性和准确性。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * 构造函数，通过 Spring IoC 注入仪表盘数据服务。
     *
     * @param dashboardService 仪表盘业务服务
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取当前用户的首页仪表盘统计数据。
     * 返回用户学习状况的全景概览数据，包括各模块的计数指标和完成百分比，
     * 用于在平台首页渲染数据卡片和统计图表，帮助用户直观了解自身学习进度。
     *
     * @return 包含笔记数、错题数、计划完成率、资源数、学习天数等指标的统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(dashboardService.stats(AuthContext.getCurrentUserId()));
    }
}
