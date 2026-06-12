package com.ailearning.backend.service;

import com.ailearning.backend.entity.StudyPlan;
import com.ailearning.backend.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘服务 —— 提供首页仪表盘所需的关键指标数据。
 *
 * <p>展示指标：
 * <ol>
 *   <li><b>今日学习时长</b>：根据用户学习天数模拟计算（上限 240 分钟）</li>
 *   <li><b>任务数量</b>：当前用户的学习计划总数</li>
 *   <li><b>任务完成率</b>：已完成任务占总任务的百分比</li>
 *   <li><b>周学习趋势</b>：一周 7 天的学习时长分布（固定模式）</li>
 *   <li><b>学习热力图</b>：过去 6 个月每日活跃度数据（模拟）</li>
 * </ol>
 *
 * <p>与 AnalyticsService 的区别：DashboardService 面向首页概览，数据更精简；
 * AnalyticsService 面向分析页面，数据更全面。
 */
@Service
public class DashboardService {
    private final UserService userService;
    private final StudyPlanService studyPlanService;

    /**
     * 构造仪表盘服务。
     *
     * @param userService       用户服务
     * @param studyPlanService  学习计划服务
     */
    public DashboardService(UserService userService, StudyPlanService studyPlanService) {
        this.userService = userService;
        this.studyPlanService = studyPlanService;
    }

    /**
     * 获取仪表盘核心指标数据。
     *
     * @param userId 用户 ID
     * @return 包含 todayMinutes, taskCount, completedRate, weekTrend, heatmap 的 Map
     */
    public Map<String, Object> stats(Long userId) {
        User user = userService.getUser(userId);
        List<StudyPlan> plans = studyPlanService.findAllByUserId(userId);

        // 计算任务完成率（兜底：至少为 1 防止除零）
        long doneCount = plans.stream().filter(plan -> "done".equals(plan.getStatus())).count();
        int totalTasks = Math.max(plans.size(), 1);
        int completedRate = (int) Math.round(doneCount * 100.0 / totalTasks);

        // 周学习趋势（固定展示数据）
        Map<String, Object> weekTrend = new HashMap<>();
        weekTrend.put("labels", List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        weekTrend.put("values", List.of(45, 80, 120, 155, 130, 95, 50));

        Map<String, Object> data = new HashMap<>();
        // 今日学习时长：基于学习天数的模拟值，上限 240 分钟
        data.put("todayMinutes", Math.min(240, 60 + user.getStudyDays() % 80));
        data.put("taskCount", plans.size());
        data.put("completedRate", completedRate);
        data.put("weekTrend", weekTrend);
        data.put("heatmap", buildHeatmap());

        return data;
    }

    /**
     * 构建学习热力图数据（过去 6 个月每日活跃度，模拟数据）。
     *
     * @return 每日 [日期字符串, 活跃度等级(0-5)] 的列表
     */
    private List<List<Object>> buildHeatmap() {
        List<List<Object>> data = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(6);
        int index = 0;
        while (!start.isAfter(end)) {
            data.add(List.of(start.toString(), (index * 3 + start.getDayOfMonth()) % 6));
            start = start.plusDays(1);
            index++;
        }
        return data;
    }
}
