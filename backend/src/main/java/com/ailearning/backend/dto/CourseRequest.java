package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 课程创建/更新请求DTO。
 * <p>
 * 封装管理员创建或编辑课程时提交的课程信息。
 * 课程ID由管理员手动指定（非自增），所有核心字段（ID、标题、分类、描述、课时数、讲师）
 * 均标注了对应的校验注解，确保入库数据的完整性和合规性。
 * 封面URL为可选字段。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
public class CourseRequest {

    /** 课程唯一标识，由管理员手动指定，长度上限40字符 */
    @NotBlank(message = "课程ID不能为空")
    @Size(max = 40, message = "课程ID长度不能超过40")
    private String id;

    /** 课程标题，长度上限120字符 */
    @NotBlank(message = "课程标题不能为空")
    @Size(max = 120, message = "课程标题长度不能超过120")
    private String title;

    /** 课程所属分类，如"编程"、"数学"，长度上限40字符 */
    @NotBlank(message = "课程类别不能为空")
    @Size(max = 40, message = "课程类别长度不能超过40")
    private String category;

    /** 课程封面图片URL（可选），长度上限255字符 */
    @Size(max = 255, message = "封面URL长度不能超过255")
    private String cover;

    /** 课程简介/描述，长度上限500字符 */
    @NotBlank(message = "课程描述不能为空")
    @Size(max = 500, message = "课程描述长度不能超过500")
    private String description;

    /** 课程总课时数，不能为 null */
    @NotNull(message = "课时数不能为空")
    private Integer lessons;

    /** 课程讲师名称，长度上限50字符 */
    @NotBlank(message = "讲师不能为空")
    @Size(max = 50, message = "讲师长度不能超过50")
    private String teacher;
}
