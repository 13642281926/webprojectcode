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

@Service
public class WrongQuestionService {
    private final WrongQuestionRepository wrongQuestionRepository;

    public WrongQuestionService(WrongQuestionRepository wrongQuestionRepository) {
        this.wrongQuestionRepository = wrongQuestionRepository;
    }

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

    private boolean matchesStatus(WrongQuestion q, String status) {
        if ("mastered".equals(status)) return q.isMastered();
        if ("reviewing".equals(status)) return !q.isMastered() && q.getWrongCount() > 1;
        if ("new".equals(status)) return !q.isMastered() && q.getWrongCount() <= 1;
        return true;
    }

    @Transactional(readOnly = true)
    public WrongQuestion detail(Long userId, Long id) {
        return wrongQuestionRepository.findById(id)
                .filter(q -> q.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "题目不存在"));
    }

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

    @Transactional
    public void delete(Long userId, Long id) {
        WrongQuestion q = detail(userId, id);
        wrongQuestionRepository.delete(q);
    }

    @Transactional
    public void markAsMastered(Long userId, Long id) {
        WrongQuestion q = detail(userId, id);
        q.setMastered(true);
        q.setUpdatedAt(LocalDateTime.now());
        wrongQuestionRepository.save(q);
    }

    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "math", "name", "数学"),
                Map.of("id", "english", "name", "英语"),
                Map.of("id", "cs", "name", "专业课")
        );
    }

    private boolean matchesKeyword(WrongQuestion q, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return q.getTitle().toLowerCase().contains(lowerKeyword) ||
                (q.getContent() != null && q.getContent().toLowerCase().contains(lowerKeyword));
    }
}
