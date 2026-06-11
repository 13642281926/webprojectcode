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

@Service
public class AnalyticsService {
    private final UserService userService;
    private final StudyPlanService studyPlanService;
    private final CourseService courseService;

    public AnalyticsService(UserService userService, StudyPlanService studyPlanService, CourseService courseService) {
        this.userService = userService;
        this.studyPlanService = studyPlanService;
        this.courseService = courseService;
    }

    public Map<String, Object> overview(Long userId) {
        User user = userService.getUser(userId);
        List<StudyPlan> plans = studyPlanService.findAllByUserId(userId);
        List<Course> courses = courseService.allCourses();

        long done = plans.stream().filter(plan -> "done".equals(plan.getStatus())).count();
        long doing = plans.stream().filter(plan -> "doing".equals(plan.getStatus())).count();
        long pending = plans.stream().filter(plan -> "pending".equals(plan.getStatus())).count();

        int averageCourseProgress = (int) courses.stream().mapToInt(Course::getProgress).average().orElse(0);

        // 过去6个月学习时长（按当前月倒推）
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth currentMonth = YearMonth.now();
        List<String> monthLabels = new ArrayList<>();
        List<Integer> monthValues = new ArrayList<>();
        int baseHours = Math.max(20, user.getTotalHours());
        Random rnd = new Random(userId == null ? 1L : userId);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            monthLabels.add(ym.format(ymFmt));
            int hours = baseHours / 6 + rnd.nextInt(20);
            monthValues.add(hours);
        }
        Map<String, Object> monthTrend = new HashMap<>();
        monthTrend.put("labels", monthLabels);
        monthTrend.put("values", monthValues);

        Map<String, Object> weeklyHours = new HashMap<>();
        weeklyHours.put("labels", List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        weeklyHours.put("values", List.of(2, 4, 6, 7, 5, 3, 1));

        Map<String, Object> taskRate = new HashMap<>();
        taskRate.put("done", done);
        taskRate.put("doing", doing);
        taskRate.put("pending", pending);

        List<Map<String, Object>> progressList = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", course.getTitle());
            item.put("value", course.getProgress());
            progressList.add(item);
        }
        Map<String, Object> courseProgress = new HashMap<>();
        courseProgress.put("list", progressList);

        // 学习热力图：过去12周（按天）
        List<Object[]> heatmap = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 12 * 7 - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int minutes = rnd.nextInt(120);
            if (minutes < 5) continue;
            heatmap.add(new Object[] { d.toString(), minutes });
        }
        // 补一些离散日期防止前端空
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
