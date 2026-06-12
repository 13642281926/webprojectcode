package com.ailearning.backend.service;

import com.ailearning.backend.entity.Course;
import com.ailearning.backend.entity.StudyPlan;
import com.ailearning.backend.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 学习分析服务 —— 聚合用户的学习数据，生成多维度的学习分析报告。
 *
 * <p>分析维度：
 * <ol>
 *   <li><b>概览指标</b>：学习天数、总时长、完成任务数、课程完成度、任务完成率</li>
 *   <li><b>月度趋势</b>：过去 6 个月的学习时长趋势（基于用户总时长模拟分配）</li>
 *   <li><b>周度分布</b>：一周 7 天的学习时长分布（固定模式）</li>
 *   <li><b>任务状态</b>：已完成/进行中/待开始的比例</li>
 *   <li><b>课程进度</b>：每门课程的学习进度百分比</li>
 *   <li><b>学习热力图</b>：过去 12 周每天的学习分钟数分布</li>
 * </ol>
 *
 * <p>注意：月度趋势和热力图数据目前使用随机算法模拟生成（演示用途），
 * 未来应替换为真实的学习记录数据源。
 */
@Service
public class AnalyticsService {
    private final UserService userService;
    private final StudyPlanService studyPlanService;
    private final CourseService courseService;

    /**
     * 构造学习分析服务。
     *
     * @param userService       用户服务
     * @param studyPlanService  学习计划服务
     * @param courseService     课程服务
     */
    public AnalyticsService(UserService userService, StudyPlanService studyPlanService, CourseService courseService) {
        this.userService = userService;
        this.studyPlanService = studyPlanService;
        this.courseService = courseService;
    }

    /**
     * 生成用户学习分析总览数据。
     *
     * <p>聚合用户、学习计划、课程三大模块的数据，生成多维分析报告。
     * 其中月度趋势和热力图使用模拟数据（演示用途）。
     *
     * @param userId 用户 ID
     * @return 包含 studyDays, totalHours, completedTasks, courseCompletion,
     *         taskCompletionRate, monthTrend, weeklyHours, taskRate, courseProgress, heatmap 的 Map
     */
    public Map<String, Object> overview(Long userId) {
        // 获取基础数据
        User user = userService.getUser(userId);
        List<StudyPlan> plans = studyPlanService.findAllByUserId(userId);
        List<Course> courses = courseService.allCourses();

        // 统计各状态任务数量
        long done = plans.stream().filter(plan -> "done".equals(plan.getStatus())).count();
        long doing = plans.stream().filter(plan -> "doing".equals(plan.getStatus())).count();
        long pending = plans.stream().filter(plan -> "pending".equals(plan.getStatus())).count();

        // 平均课程进度
        int averageCourseProgress = (int) courses.stream().mapToInt(Course::getProgress).average().orElse(0);

        // 过去6个月学习时长趋势（模拟数据，基于总时长分配）
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth currentMonth = YearMonth.now();
        List<String> monthLabels = new ArrayList<>();
        List<Integer> monthValues = new ArrayList<>();
        int baseHours = Math.max(20, user.getTotalHours());
        Random rnd = new Random(userId == null ? 1L : userId);  // 用 userId 作为随机种子保持一致性
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            monthLabels.add(ym.format(ymFmt));
            int hours = baseHours / 6 + rnd.nextInt(20);  // 模拟每月波动
            monthValues.add(hours);
        }
        Map<String, Object> monthTrend = new HashMap<>();
        monthTrend.put("labels", monthLabels);
        monthTrend.put("values", monthValues);

        // 周度学习时长分布（固定模式：工作日高、周末低）
        Map<String, Object> weeklyHours = new HashMap<>();
        weeklyHours.put("labels", List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        weeklyHours.put("values", List.of(2, 4, 6, 7, 5, 3, 1));

        // 任务完成率分布
        Map<String, Object> taskRate = new HashMap<>();
        taskRate.put("done", done);
        taskRate.put("doing", doing);
        taskRate.put("pending", pending);

        // 各课程进度列表
        List<Map<String, Object>> progressList = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", course.getTitle());
            item.put("value", course.getProgress());
            progressList.add(item);
        }
        Map<String, Object> courseProgress = new HashMap<>();
        courseProgress.put("list", progressList);

        // 学习热力图：过去12周（约84天）的每日学习分钟数（模拟数据）
        List<Object[]> heatmap = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 12 * 7 - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int minutes = rnd.nextInt(120);
            if (minutes < 5) continue;  // 跳过极少学习的日期，减少数据量
            heatmap.add(new Object[] { d.toString(), minutes });
        }
        // 补充离散日期，防止前端热力图过于稀疏
        for (int i = 0; i < 5; i++) {
            LocalDate d = today.minusDays(rnd.nextInt(12 * 7));
            heatmap.add(new Object[] { d.toString(), 30 + rnd.nextInt(60) });
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studyDays", user.getStudyDays());
        data.put("totalHours", user.getTotalHours());
        data.put("completedTasks", (int) done);
        data.put("courseCompletion", averageCourseProgress);
        data.put("taskCompletionRate", plans.isEmpty() ? 0 : (int) (done * 100 / plans.size()));
        data.put("monthTrend", monthTrend);
        data.put("weeklyHours", weeklyHours);
        data.put("taskRate", taskRate);
        data.put("courseProgress", courseProgress);
        data.put("heatmap", heatmap);

        return data;
    }
}
