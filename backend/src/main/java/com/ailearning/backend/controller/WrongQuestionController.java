package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.service.WrongQuestionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * 错题本控制器，提供错题的完整生命周期管理接口。
 * <p>
 * 错题本是平台重要的学习辅助工具，用于记录和追踪用户答错的题目。
 * 用户可以手动录入错题、查看错题详情（含正确答案和解析）、对错题按难度/分类/掌握状态进行筛选、
 * 将已掌握的错题标记为"已掌握"等。系统还支持统计错题分类和难度分布，帮助用户精准定位薄弱环节。
 * 所有操作均关联当前登录用户，实现用户级别数据隔离。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/wrongQuestion")
public class WrongQuestionController {
    private final WrongQuestionService wrongQuestionService;

    /**
     * 构造函数，通过 Spring IoC 注入错题服务。
     *
     * @param wrongQuestionService 错题业务服务
     */
    public WrongQuestionController(WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 获取当前用户的错题列表，支持多维度筛选和分页。
     * 支持按分类、关键词、难度、掌握状态组合过滤，并支持分页返回，
     * 方便用户按条件查找特定错题进行针对性复习。
     *
     * @param category   题目分类（可选），如"数学"、"英语"
     * @param keyword    搜索关键词（可选），匹配题目标题和内容
     * @param difficulty 难度筛选（可选），如"easy"/"medium"/"hard"
     * @param status     掌握状态筛选（可选），"mastered" 仅查已掌握，"unmastered" 仅查未掌握
     * @param page       页码（可选），用于分页
     * @param pageSize   每页条数（可选），用于分页
     * @return 包含错题列表和分页信息的结果
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(wrongQuestionService.list(AuthContext.getCurrentUserId(), category, keyword, difficulty, status, page, pageSize));
    }

    /**
     * 查看单道错题的完整详情。
     * 返回错题的标题、内容、正确答案、详细解析、分类、难度、错误次数、
     * 标签、是否已掌握等信息，用于用户深入理解错误原因并巩固知识点。
     *
     * @param id 错题ID
     * @return 包含完整错题信息的键值对结构
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        var q = wrongQuestionService.detail(AuthContext.getCurrentUserId(), id);
        // 将错题实体转换为前端友好的键值对结构，便于直接展示
        Map<String, Object> m = Map.ofEntries(
                entry("id", q.getId()),
                entry("title", q.getTitle()),
                entry("content", q.getContent()),
                entry("answer", q.getAnswer()),
                entry("analysis", q.getAnalysis()),
                entry("category", q.getCategory()),
                entry("difficulty", q.getDifficulty()),
                entry("wrongCount", q.getWrongCount()),
                entry("tags", q.getTags()),
                entry("mastered", q.isMastered()),
                entry("createTime", q.getCreatedAt().toString())
        );
        return ApiResponse.success(m);
    }

    /**
     * 手动添加一道错题到错题本。
     * 用户在练习或考试中遇到错误时，可通过此接口录入错题信息，
     * 包括题目内容、正确答案、解析、分类、难度和标签等。
     *
     * @param body 请求体，包含 title/content/answer/analysis/category/difficulty/tags 等字段
     * @return 创建成功，返回新错题的ID
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        var q = wrongQuestionService.create(AuthContext.getCurrentUserId(), body);
        return ApiResponse.success("添加成功", Map.of("id", q.getId()));
    }

    /**
     * 更新已有错题信息。
     * 支持修改错题的标题、内容、答案、解析、分类、难度和标签等字段。
     *
     * @param id   待更新的错题ID
     * @param body 包含待修改字段的请求体
     * @return 更新成功，返回错题ID
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var q = wrongQuestionService.update(AuthContext.getCurrentUserId(), id, body);
        return ApiResponse.success("更新成功", Map.of("id", q.getId()));
    }

    /**
     * 删除指定错题记录。
     * 删除后错题将从错题本中移除，不再参与统计和复习。
     *
     * @param id 待删除的错题ID
     * @return 删除成功的空响应
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        wrongQuestionService.delete(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 将错题标记为"已掌握"。
     * 用户经过复习后确认已理解该题的解题方法，调用此接口标记掌握状态。
     * 已掌握的错题不会被移除，但可通过列表筛选将其隐藏，专注攻克未掌握的错题。
     *
     * @param id 待标记的错题ID
     * @return 标记成功的空响应
     */
    @PostMapping("/master/{id}")
    public ApiResponse<Void> markAsMastered(@PathVariable Long id) {
        wrongQuestionService.markAsMastered(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("掌握成功", null);
    }

    /**
     * 获取错题分类列表。
     * 返回系统中所有错题的分类（如"数学"、"英语"、"编程"等），
     * 用于前端分类筛选下拉框和统计图表。
     *
     * @return 包含所有错题分类键值对的列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, String>>> categories() {
        return ApiResponse.success(wrongQuestionService.categories());
    }
}
