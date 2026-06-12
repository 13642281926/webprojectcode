package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.entity.Note;
import com.ailearning.backend.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习笔记控制器，提供笔记的增删改查及分类管理接口。
 * <p>
 * 笔记是用户在学习过程中的个人记录与心得总结，与用户账号强关联——
 * 每个用户只能查看、编辑和删除自己创建的笔记，实现数据隔离。
 * 笔记支持按分类（如"课程笔记"、"读书笔记"）归档，以及按关键词全文搜索。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/note")
public class NoteController {
    private final NoteService noteService;

    /**
     * 构造函数，通过 Spring IoC 注入笔记服务。
     *
     * @param noteService 笔记业务服务
     */
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 获取当前用户的笔记列表。
     * 自动从认证上下文获取当前用户 ID，仅返回该用户创建的笔记。
     * 支持按分类过滤和按标题/内容关键词模糊搜索。
     *
     * @param category 笔记分类（可选），如"课程笔记"、"读书笔记"
     * @param keyword  搜索关键词（可选），匹配笔记标题和内容
     * @return 包含笔记列表和分页信息的响应结果
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        // 从认证上下文获取当前用户ID，确保数据隔离
        return ApiResponse.success(noteService.list(AuthContext.getCurrentUserId(), category, keyword));
    }

    /**
     * 查看单篇笔记的详细内容。
     * 仅允许查看本人创建的笔记，传入其他用户笔记ID将返回权限错误。
     *
     * @param id 笔记ID
     * @return 笔记的完整实体信息（标题、正文、分类、创建时间等）
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Note> detail(@PathVariable Long id) {
        return ApiResponse.success(noteService.detail(AuthContext.getCurrentUserId(), id));
    }

    /**
     * 创建一篇新的学习笔记。
     * 笔记自动关联到当前登录用户，请求体中包含标题、正文内容和分类信息。
     *
     * @param body 请求体，包含 title/content/category 等字段
     * @return 创建成功的笔记实体
     */
    @PostMapping("/create")
    public ApiResponse<Note> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success("创建成功", noteService.create(AuthContext.getCurrentUserId(), body));
    }

    /**
     * 更新已有笔记的内容。
     * 仅笔记创建者本人可执行更新操作，支持修改标题、正文和分类。
     *
     * @param id   待更新的笔记ID
     * @param body 包含待修改字段的请求体
     * @return 更新后的笔记实体
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Note> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success("更新成功", noteService.update(AuthContext.getCurrentUserId(), id, body));
    }

    /**
     * 删除指定笔记。
     * 仅笔记创建者本人可删除自己的笔记，删除后不可恢复。
     *
     * @param id 待删除的笔记ID
     * @return 删除成功的空响应
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.delete(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 获取系统预置的笔记分类列表。
     * 返回所有可用的分类键值对（如"课程笔记"、"读书笔记"、"学习心得"），
     * 用于前端下拉选择器和分类筛选。
     *
     * @return 包含所有笔记分类的列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, String>>> categories() {
        return ApiResponse.success(noteService.categories());
    }
}
