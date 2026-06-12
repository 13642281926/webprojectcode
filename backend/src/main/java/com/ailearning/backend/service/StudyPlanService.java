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

/**
 * 学习计划服务 —— 负责用户学习计划的完整 CRUD 及统计数据分析。
 *
 * <p>业务功能：
 * <ol>
 *   <li><b>计划列表</b>：分页查询，支持按优先级（high/medium/low）、状态（pending/doing/done/completed/overdue）和关键词筛选。</li>
 *   <li><b>创建/更新/删除</b>：默认优先级为 medium，默认状态为 pending。</li>
 *   <li><b>统计分析</b>：计算任务完成率、状态分布（已完成/进行中/待开始/已逾期），供分析页面使用。</li>
 * </ol>
 *
 * <p>优先级：high（高）、medium（中）、low（低）。
 * 状态：pending（待开始）、doing（进行中）、done/completed（已完成）、overdue（已逾期）。
 */
@Service
public class StudyPlanService {
    private final StudyPlanRepository studyPlanRepository;

    /**
     * 构造学习计划服务，注入计划数据仓库。
     *
     * @param studyPlanRepository 学习计划数据访问接口
     */
    public StudyPlanService(StudyPlanRepository studyPlanRepository) {
        this.studyPlanRepository = studyPlanRepository;
    }

    /**
     * 分页查询用户学习计划列表。
     *
     * <p>先按条件过滤全量数据，再在内存中做分页截取。
     * page/pageSize 做了安全兜底处理（最小为 1/10）。
     *
     * @param userId   用户 ID
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     * @param priority 优先级筛选（high/medium/low），可为空
     * @param status   状态筛选（pending/doing/done/completed/overdue），可为空
     * @param keyword  搜索关键词（匹配标题和内容），可为空
     * @return 包含 list、total、page、pageSize 的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId, Integer page, Integer pageSize, String priority, String status, String keyword) {
        // 安全的页码/条数兜底
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;

        List<Map<String, Object>> filtered = studyPlanRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(plan -> !StringUtils.hasText(priority) || priority.equals(plan.getPriority()))
                .filter(plan -> !StringUtils.hasText(status) || status.equals(plan.getStatus()))
                .filter(plan -> matchesKeyword(plan, keyword))
                .map(this::toMap)
                .collect(Collectors.toList());

        // 内存分页：计算起止索引，防止越界
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", filtered.subList(fromIndex, toIndex));
        data.put("total", filtered.size());
        data.put("page", safePage);
        data.put("pageSize", safePageSize);
        return data;
    }

    /**
     * 创建学习计划。
     *
     * <p>优先级默认为 medium（中等），状态默认为 pending（待开始）。
     *
     * @param userId  用户 ID
     * @param request 计划创建请求（title, content, deadline, priority, status）
     * @return 创建后的计划 Map
     */
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

    /**
     * 更新学习计划（仅更新 request 中非 null 的字段）。
     *
     * @param userId  用户 ID（需与计划所属用户一致）
     * @param id      计划 ID
     * @param request 计划更新请求
     * @return 更新后的计划 Map
     * @throws ApiException 404 如果计划不存在或不属于当前用户
     */
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

    /**
     * 删除学习计划（需用户归属校验）。
     *
     * @param userId 用户 ID
     * @param id     计划 ID
     * @throws ApiException 404 如果计划不存在或不属于当前用户
     */
    @Transactional
    public void delete(Long userId, Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "计划不存在"));
        studyPlanRepository.delete(plan);
    }

    /**
     * 获取用户所有学习计划（供其他服务调用）。
     *
     * @param userId 用户 ID
     * @return 用户全部计划列表
     */
    @Transactional(readOnly = true)
    public List<StudyPlan> findAllByUserId(Long userId) {
        return studyPlanRepository.findByUserIdOrderByIdDesc(userId);
    }

    /**
     * 学习计划统计分析（供分析页面使用）。
     *
     * <p>统计指标：
     * <ul>
     *   <li>totalTasks - 任务总数</li>
     *   <li>completedTasks / doingTasks / pendingTasks - 各状态数量</li>
     *   <li>completedRate - 完成率（百分比，四舍五入取整）</li>
     *   <li>taskDistribution - 任务状态分布（已完成/进行中/待开始/已逾期）</li>
     * </ul>
     *
     * @param userId 用户 ID
     * @return 统计指标 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long userId) {
        var plans = studyPlanRepository.findByUserIdOrderByIdDesc(userId);
        // 统计各状态数量（done 和 completed 均视为已完成）
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

    /**
     * 判断计划是否匹配搜索关键词（大小写不敏感）。
     *
     * <p>搜索范围：计划标题、计划内容。
     *
     * @param plan    学习计划实体
     * @param keyword 搜索关键词
     * @return 如果 keyword 为空或计划匹配，返回 true
     */
    private boolean matchesKeyword(StudyPlan plan, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return plan.getTitle().toLowerCase().contains(lowerKeyword) ||
                (plan.getContent() != null && plan.getContent().toLowerCase().contains(lowerKeyword));
    }

    /**
     * 将学习计划实体转换为 Map 视图，供前端使用。
     *
     * @param plan 学习计划实体
     * @return 包含 id, title, content, deadline, priority, status, createdAt 的 Map
     */
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
