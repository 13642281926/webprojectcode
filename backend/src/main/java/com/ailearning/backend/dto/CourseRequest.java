package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {
    @NotBlank(message = "课程ID不能为空")
    @Size(max = 40, message = "课程ID长度不能超过40")
    private String id;

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 120, message = "课程标题长度不能超过120")
    private String title;

    @NotBlank(message = "课程类别不能为空")
    @Size(max = 40, message = "课程类别长度不能超过40")
    private String category;

    @Size(max = 255, message = "封面URL长度不能超过255")
    private String cover;

    @NotBlank(message = "课程描述不能为空")
    @Size(max = 500, message = "课程描述长度不能超过500")
    private String description;

    @NotNull(message = "课时数不能为空")
    private Integer lessons;

    @NotBlank(message = "讲师不能为空")
    @Size(max = 50, message = "讲师长度不能超过50")
    private String teacher;
}
