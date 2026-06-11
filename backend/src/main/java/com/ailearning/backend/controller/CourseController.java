package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.entity.Course;
import com.ailearning.backend.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(courseService.list(category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<Course> detail(@PathVariable String id) {
        return ApiResponse.success(courseService.detail(id));
    }

    @PostMapping
    public ApiResponse<Course> create(@RequestBody Map<String, Object> body) {
        AuthContext.requireAdmin();
        return ApiResponse.success("课程创建成功", courseService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<Course> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        AuthContext.requireAdmin();
        return ApiResponse.success("课程更新成功", courseService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        AuthContext.requireAdmin();
        courseService.delete(id);
        return ApiResponse.success(null);
    }
}
