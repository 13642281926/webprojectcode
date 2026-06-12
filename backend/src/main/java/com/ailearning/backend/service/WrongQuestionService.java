package com.ailearning.backend.service;

import com.ailearning.backend.entity.WrongQuestion;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.WrongQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 错题服务 —— 负责用户错题本的管理与统计。
 *
 * <p>业务功能：
 * <ol>
 *   <li><b>错题列表</b>：分页查询，支持按分类（数学/英语/专业课）、难度（简单/中等/困难）、
 *       掌握状态（新错题/复习中/已掌握）和关键词筛选。</li>
 *   <li><b>错题详情</b>：按 ID 查询，含用户归属校验。</li>
 *   <li><b>创建/更新/删除</b>：支持设置标题、内容、答案、解析、分类、难度、标签。</li>
 *   <li><b>标记已掌握</b>：一键将错题状态切换为 mastered。</li>
 *   <li><b>统计分类</b>：返回掌握/复习中/新错题的数量统计。</li>
 * </ol>
 *
 * <p>掌握状态根据错误次数判断：
 * <ul>
 *   <li>new（新错题）：错题次数 <= 1 且未掌握</li>
 *   <li>reviewing（复习中）：错题次数 > 1 且未掌握</li>
 *   <li>mastered（已掌握）：已手动标记掌握</li>
 * </ul>
 */
@Service
public class WrongQuestionService {
    private final WrongQuestionRepository wrongQuestionRepository;

    /**
     * 构造错题服务，注入错题数据仓库。
     *
     * @param wrongQuestionRepository 错题数据访问接口
     */
    public WrongQuestionService(WrongQuestionRepository wrongQuestionRepository) {
        this.wrongQuestionRepository = wrongQuestionRepository;
    }

    /**
     * 分页查询用户错题列表，支持多条件筛选。
     *
     * <p>同时返回掌握状态分布统计：masteredCount、reviewingCount、newCount。
     *
     * @param userId     用户 ID
     * @param category   错题分类（"all"/"math"/"english"/"cs"），可为空
     * @param keyword    搜索关键词（匹配标题和内容），可为空
     * @param difficulty 难度筛选（"简单"/"中等"/"困难"），可为空
     * @param status     掌握状态筛选（"new"/"reviewing"/"mastered"），可为空
     * @param page       页码（从 1 开始）
     * @param pageSize   每页条数（默认 10）
     * @return 包含 list、total、masteredCount、reviewingCount、newCount 的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId, String category, String keyword, String difficulty, String status, Integer page, Integer pageSize) {
        List<WrongQuestion> list = wrongQuestionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .filter(q -> !StringUtils.hasText(category) || "all".equals(category) || category.equals(q.getCategory()))
                .filter(q -> !StringUtils.hasText(difficulty) || difficulty.equals(q.getDifficulty()))
                .filter(q -> !StringUtils.hasText(status) || matchesStatus(q, status))
                .filter(q -> matchesKeyword(q, keyword))
                .collect(Collectors.toList());

        long masteredCount = list.stream().filter(WrongQuestion::isMastered).count();
        long reviewingCount = list.stream().filter(q -> !q.isMastered() && q.getWrongCount() > 1).count();
        long newCount = list.stream().filter(q -> !q.isMastered() && q.getWrongCount() <= 1).count();

        int total = list.size();
        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 10;
        int start = (pageNum - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> resultList = list.subList(start, end).stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultList);
        data.put("total", total);
        data.put("masteredCount", masteredCount);
        data.put("reviewingCount", reviewingCount);
        data.put("newCount", newCount);
        return data;
    }

    /**
     * 将错题实体转换为前端可用的 Map 视图，并动态计算 status 字段。
     */
    private Map<String, Object> convertToMap(WrongQuestion q) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", q.getId());
        m.put("title", q.getTitle());
        m.put("content", q.getContent());
        m.put("answer", q.getAnswer());
        m.put("analysis", q.getAnalysis());
        m.put("category", q.getCategory());
        m.put("difficulty", q.getDifficulty());
        m.put("wrongCount", q.getWrongCount());
        m.put("tags", q.getTags());
        m.put("mastered", q.isMastered());
        m.put("status", q.isMastered() ? "mastered" : (q.getWrongCount() > 1 ? "reviewing" : "new"));
        m.put("createTime", q.getCreatedAt().toString());
        m.put("updatedAt", q.getUpdatedAt().toString());
        return m;
    }

    /**
     * 判断错题是否匹配指定的掌握状态筛选条件。
     *
     * @param q      错题实体
     * @param status 筛选状态："mastered"/"reviewing"/"new"
     * @return 是否匹配
     */
    private boolean matchesStatus(WrongQuestion q, String status) {
        if ("mastered".equals(status)) return q.isMastered();
        if ("reviewing".equals(status)) return !q.isMastered() && q.getWrongCount() > 1;
        if ("new".equals(status)) return !q.isMastered() && q.getWrongCount() <= 1;
        return true;
    }

    /**
     * 查询错题详情，附带用户归属校验。
     *
     * @param userId 当前用户 ID
     * @param id     错题 ID
     * @return 错题实体
     * @throws ApiException 404 如果错题不存在或不属于当前用户
     */
    @Transactional(readOnly = true)
    public WrongQuestion detail(Long userId, Long id) {
        return wrongQuestionRepository.findById(id)
                .filter(q -> q.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "题目不存在"));
    }

    /**
     * 创建错题记录。
     *
     * <p>初始错题次数为 1，默认未掌握，标签以逗号分隔存储。
     *
     * @param userId 错题所属用户 ID
     * @param body   错题信息 Map，含 title, content, answer, analysis, category, difficulty, tags
     * @return 保存后的错题实体
     */
    @Transactional
    public WrongQuestion create(Long userId, Map<String, Object> body) {
        WrongQuestion q = new WrongQuestion();
        q.setUserId(userId);
        q.setTitle(String.valueOf(body.get("title")));
        q.setContent(String.valueOf(body.get("content")));
        q.setAnswer(String.valueOf(body.getOrDefault("answer", "")));
        q.setAnalysis(String.valueOf(body.getOrDefault("analysis", "")));
        q.setCategory(String.valueOf(body.getOrDefault("category", "")));
        q.setDifficulty(String.valueOf(body.getOrDefault("difficulty", "中等")));
        q.setTags(body.get("tags") != null ? String.join(",", (List<String>) body.get("tags")) : "");
        q.setWrongCount(1);
        q.setMastered(false);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        return wrongQuestionRepository.save(q);
    }

    /**
     * 更新错题（仅更新 body 中提供的字段）。
     *
     * @param userId 当前用户 ID
     * @param id     错题 ID
     * @param body   要更新的字段 Map
     * @return 更新后的错题实体
     * @throws ApiException 404 如果错题不存在或不属于当前用户
     */
    @Transactional
    public WrongQuestion update(Long userId, Long id, Map<String, Object> body) {
        WrongQuestion q = detail(userId, id);
        if (body.containsKey("title")) q.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("content")) q.setContent(String.valueOf(body.get("content")));
        if (body.containsKey("answer")) q.setAnswer(String.valueOf(body.get("answer")));
        if (body.containsKey("analysis")) q.setAnalysis(String.valueOf(body.get("analysis")));
        if (body.containsKey("category")) q.setCategory(String.valueOf(body.get("category")));
        if (body.containsKey("difficulty")) q.setDifficulty(String.valueOf(body.get("difficulty")));
        if (body.containsKey("mastered")) q.setMastered((Boolean) body.get("mastered"));
        if (body.containsKey("tags")) q.setTags(String.join(",", (List<String>) body.get("tags")));
        q.setUpdatedAt(LocalDateTime.now());
        return wrongQuestionRepository.save(q);
    }

    /**
     * 删除错题。
     *
     * @param userId 当前用户 ID
     * @param id     错题 ID
     * @throws ApiException 404 如果错题不存在或不属于当前用户
     */
    @Transactional
    public void delete(Long userId, Long id) {
        WrongQuestion q = detail(userId, id);
        wrongQuestionRepository.delete(q);
    }

    /**
     * 标记错题为"已掌握"。
     *
     * @param userId 当前用户 ID
     * @param id     错题 ID
     * @throws ApiException 404 如果错题不存在或不属于当前用户
     */
    @Transactional
    public void markAsMastered(Long userId, Long id) {
        WrongQuestion q = detail(userId, id);
        q.setMastered(true);
        q.setUpdatedAt(LocalDateTime.now());
        wrongQuestionRepository.save(q);
    }

    /**
     * 获取错题分类选项列表。
     *
     * @return 固定四种分类：全部、数学、英语、专业课
     */
    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "math", "name", "数学"),
                Map.of("id", "english", "name", "英语"),
                Map.of("id", "cs", "name", "专业课")
        );
    }

    /**
     * 判断错题是否匹配搜索关键词（大小写不敏感）。
     *
     * <p>搜索范围：错题标题、错题内容。
     *
     * @param q       错题实体
     * @param keyword 搜索关键词
     * @return 如果 keyword 为空或匹配关键词，返回 true
     */
    private boolean matchesKeyword(WrongQuestion q, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return q.getTitle().toLowerCase().contains(lowerKeyword) ||
                (q.getContent() != null && q.getContent().toLowerCase().contains(lowerKeyword));
    }
}
