package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求DTO。
 * <p>
 * 封装用户注册时提交的账号、密码、确认密码和昵称信息。
 * 通过 {@code @Size} 约束限制用户名和密码的长度范围，
 * confirmPassword 字段用于前端/后端双重校验两次密码是否一致。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
public class RegisterRequest {

    /** 登录账号，长度限制 3-20 位，不可为空 */
    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 20, message = "账号长度 3-20 位")
    private String username;

    /** 登录密码，长度限制 6-32 位，不可为空 */
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    /** 确认密码，需与 password 一致，由后端校验 */
    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    /** 用户昵称（可选），长度不超过50字符 */
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;
}
