package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

/**
 * 学习计划创建/更新请求DTO。
 * <p>
 * 封装用户创建或编辑学习计划时提交的数据。
 * 标题为必填项；内容、截止日期、优先级和状态为可选字段，
 * 不传时使用默认值（如优先级默认为"medium"，状态默认为"pending"）。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
public class StudyPlanRequest {

    /** 计划标题，必填 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 计划详细内容/描述（可选） */
    private String content;

    /** 计划截止日期（可选），不设置表示无截止时间 */
    private LocalDate deadline;

    /** 优先级（可选）："high"（高）、"medium"（中）、"low"（低） */
    private String priority;

    /** 状态（可选）："pending"（待办）、"in_progress"（进行中）、"completed"（已完成） */
    private String status;
}
