package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.entity.Course;
import com.ailearning.backend.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 课程管理控制器，提供课程的增删改查（CRUD）接口。
 * <p>
 * 课程是平台的核心教学单元，包含课程名称、分类、描述、封面图片等信息。
 * 普通用户可以浏览和查看课程详情；新增、修改、删除课程操作仅限管理员角色。
 * 列表查询支持按分类和关键词模糊搜索筛选。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/course")
public class CourseController {
    private final CourseService courseService;

    /**
     * 构造函数，通过 Spring IoC 注入课程服务。
     *
     * @param courseService 课程业务服务
     */
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * 获取课程分页列表。
     * 支持按课程分类过滤和按课程名称关键词模糊搜索，
     * 两个参数均为可选，不传时返回全部课程。
     *
     * @param category 课程分类（可选），如"编程"、"数学"等
     * @param keyword  搜索关键词（可选），模糊匹配课程名称
     * @return 包含课程列表和分页信息的结果
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(courseService.list(category, keyword));
    }

    /**
     * 根据课程ID获取课程详细信息。
     *
     * @param id 课程唯一标识
     * @return 课程的完整详情信息
     */
    @GetMapping("/{id}")
    public ApiResponse<Course> detail(@PathVariable String id) {
        return ApiResponse.success(courseService.detail(id));
    }

    /**
     * 创建新课程（管理员权限）。
     * 仅管理员可调用此接口，用于向平台添加新的教学课程。
     * 请求体中包含课程名称、分类、描述、封面图等信息。
     *
     * @param body 课程创建数据，包含 title/category/description/coverUrl 等字段
     * @return 创建成功的课程实体
     */
    @PostMapping
    public ApiResponse<Course> create(@RequestBody Map<String, Object> body) {
        // 校验当前用户是否为管理员角色，非管理员返回 403
        AuthContext.requireAdmin();
        return ApiResponse.success("课程创建成功", courseService.create(body));
    }

    /**
     * 更新已有课程信息（管理员权限）。
     * 仅管理员可调用，支持修改课程的名称、分类、描述和封面等信息。
     *
     * @param id   待更新的课程ID
     * @param body 包含待修改字段的请求体
     * @return 更新后的课程实体
     */
    @PutMapping("/{id}")
    public ApiResponse<Course> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        AuthContext.requireAdmin();
        return ApiResponse.success("课程更新成功", courseService.update(id, body));
    }

    /**
     * 删除指定课程（管理员权限）。
     * 仅管理员可执行删除操作，课程删除后相关数据将被清理。
     *
     * @param id 待删除的课程ID
     * @return 删除成功的空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        AuthContext.requireAdmin();
        courseService.delete(id);
        return ApiResponse.success(null);
    }
}
