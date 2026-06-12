package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.dto.StudyPlanRequest;
import com.ailearning.backend.service.StudyPlanService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学习计划控制器，提供学习计划的创建、查询、更新与删除接口。
 * <p>
 * 学习计划帮助用户合理规划学习任务，每个计划包含任务名称、截止时间、优先级（高/中/低）、
 * 完成状态等属性。列表查询支持多维度筛选——按优先级、完成状态和关键词进行过滤，
 * 并支持分页展示。所有操作自动关联当前登录用户，确保用户只能管理自己的计划。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/study-plan")
public class StudyPlanController {
    private final StudyPlanService studyPlanService;

    /**
     * 构造函数，通过 Spring IoC 注入学习计划服务。
     *
     * @param studyPlanService 学习计划业务服务
     */
    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    /**
     * 获取当前用户的学习计划列表，支持多维度筛选和分页。
     * 按优先级、完成状态和关键词过滤任务，方便用户快速定位特定计划。
     *
     * @param page     页码（可选），用于分页
     * @param pageSize 每页条数（可选），用于分页
     * @param priority 优先级筛选（可选），如"high"/"medium"/"low"
     * @param status   完成状态筛选（可选），如"pending"/"completed"
     * @param keyword  搜索关键词（可选），模糊匹配计划标题
     * @return 包含学习计划列表和分页信息的响应结果
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(studyPlanService.list(AuthContext.getCurrentUserId(), page, pageSize, priority, status, keyword));
    }

    /**
     * 创建一个新的学习计划任务。
     * 请求体包含计划标题、详细描述、截止日期、优先级等信息，
     * 计划自动关联到当前登录用户。
     *
     * @param request 学习计划创建请求，包含 title/description/deadline/priority 等字段
     * @return 创建成功的学习计划信息
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody StudyPlanRequest request) {
        return ApiResponse.success("创建成功", studyPlanService.create(AuthContext.getCurrentUserId(), request));
    }

    /**
     * 更新已有学习计划。
     * 支持修改标题、描述、截止时间、优先级和完成状态等字段，
     * 仅计划创建者本人可执行更新操作。
     *
     * @param id      待更新的计划ID
     * @param request 包含待修改字段的请求体（全部字段可选）
     * @return 更新后的学习计划信息
     */
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody StudyPlanRequest request) {
        return ApiResponse.success("更新成功", studyPlanService.update(AuthContext.getCurrentUserId(), id, request));
    }

    /**
     * 删除指定学习计划。
     * 仅计划创建者本人可删除，删除后数据不可恢复。
     *
     * @param id 待删除的计划ID
     * @return 删除成功的空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studyPlanService.delete(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }
}
