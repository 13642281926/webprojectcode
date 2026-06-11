package com.ailearning.backend.service;

import com.ailearning.backend.dto.StudyPlanRequest;
import com.ailearning.backend.entity.StudyPlan;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.StudyPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyPlanService {
    private final StudyPlanRepository studyPlanRepository;

    public StudyPlanService(StudyPlanRepository studyPlanRepository) {
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId, Integer page, Integer pageSize, String priority, String status, String keyword) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<Map<String, Object>> filtered = studyPlanRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(plan -> !StringUtils.hasText(priority) || priority.equals(plan.getPriority()))
                .filter(plan -> !StringUtils.hasText(status) || status.equals(plan.getStatus()))
                .filter(plan -> matchesKeyword(plan, keyword))
                .map(this::toMap)
                .collect(Collectors.toList());

        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", filtered.subList(fromIndex, toIndex));
        data.put("total", filtered.size());
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        return data;
    }

    @Transactional
    public Map<String, Object> create(Long userId, StudyPlanRequest request) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle(request.getTitle());
        plan.setContent(request.getContent());
        plan.setDeadline(request.getDeadline());
        plan.setPriority(request.getPriority() == null ? "medium" : request.getPriority());
        plan.setStatus(request.getStatus() == null ? "pending" : request.getStatus());
        plan.setCreatedAt(LocalDate.now());

        studyPlanRepository.save(plan);
        return toMap(plan);
    }

    @Transactional
    public Map<String, Object> update(Long userId, Long id, StudyPlanRequest request) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "计划不存在"));

        if (request.getTitle() != null) plan.setTitle(request.getTitle());
        if (request.getContent() != null) plan.setContent(request.getContent());
        if (request.getDeadline() != null) plan.setDeadline(request.getDeadline());
        if (request.getPriority() != null) plan.setPriority(request.getPriority());
        if (request.getStatus() != null) plan.setStatus(request.getStatus());

        studyPlanRepository.save(plan);
        return toMap(plan);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "计划不存在"));
        studyPlanRepository.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<StudyPlan> findAllByUserId(Long userId) {
        return studyPlanRepository.findByUserIdOrderByIdDesc(userId);
    }

    /** 分析页：任务完成分布 + 任务总数 + 完成率*/
    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long userId) {
        var plans = studyPlanRepository.findByUserIdOrderByIdDesc(userId);
        long done = plans.stream().filter(p -> "done".equals(p.getStatus()) || "completed".equals(p.getStatus())).count();
        long doing = plans.stream().filter(p -> "doing".equals(p.getStatus())).count();
        long pending = plans.stream().filter(p -> "pending".equals(p.getStatus())).count();
        int total = plans.size();

        List<Map<String, Object>> taskDistribution = List.of(
                Map.of("name", "已完成", "value", done),
                Map.of("name", "进行中", "value", doing),
                Map.of("name", "待开始", "value", pending),
                Map.of("name", "已逾期", "value", 0)
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTasks", total);
        data.put("completedTasks", done);
        data.put("doingTasks", doing);
        data.put("pendingTasks", pending);
        data.put("completedRate", total == 0 ? 0 : Math.round(done * 100.0 / total));
        data.put("taskDistribution", taskDistribution);
        return data;
    }

    private boolean matchesKeyword(StudyPlan plan, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return plan.getTitle().toLowerCase().contains(lowerKeyword) ||
                (plan.getContent() != null && plan.getContent().toLowerCase().contains(lowerKeyword));
    }

    private Map<String, Object> toMap(StudyPlan plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", plan.getId());
        map.put("title", plan.getTitle());
        map.put("content", plan.getContent());
        map.put("deadline", plan.getDeadline());
        map.put("priority", plan.getPriority());
        map.put("status", plan.getStatus());
        map.put("createdAt", plan.getCreatedAt());
        return map;
    }
}
