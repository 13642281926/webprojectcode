package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO。
 * <p>
 * 封装用户登录时提交的账号和密码，由前端表单提交后经 Spring Validation 校验。
 * 两个字段均标注 {@code @NotBlank}，确保不会收到空字符串。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
public class LoginRequest {

    /** 登录账号，不可为空 */
    @NotBlank(message = "请输入账号")
    private String username;

    /** 登录密码，不可为空 */
    @NotBlank(message = "请输入密码")
    private String password;
}
