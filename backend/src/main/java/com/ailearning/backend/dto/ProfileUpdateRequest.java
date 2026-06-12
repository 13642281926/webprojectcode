package com.ailearning.backend.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人资料更新请求DTO。
 * <p>
 * 封装用户修改个人资料时提交的昵称、签名和头像URL。
 * 昵称为必填项（{@code @NotBlank}），签名和头像为可选字段，
 * 不传表示不修改对应字段。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
public class ProfileUpdateRequest {

    /** 用户昵称，必填，用于界面展示 */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 个性签名（可选） */
    private String signature;

    /** 头像URL（可选），指向上传后的图片地址 */
    private String avatar;
}
