package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 20, message = "账号长度 3-20 位")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;
}
