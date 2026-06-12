package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;

/**
 * 用户实体类，对应数据库中的 users 表。
 * <p>
 * 用于存储平台用户的账号信息、个人资料以及学习统计数据。
 * 角色字段（role）用于区分普通用户（user）和管理员（admin），
 * 支撑平台的角色权限控制。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "users")
public class User {

    /** 用户唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录账号，不可为空且全局唯一 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 加密后的登录密码 */
    @Column(nullable = false, length = 100)
    private String password;

    /** 用户昵称，用于界面展示 */
    @Column(nullable = false, length = 50)
    private String nickname;

    /** 用户头像的URL地址 */
    @Column(length = 255)
    private String avatar;

    /** 用户个性签名 */
    @Column(length = 255)
    private String signature;

    /** 用户角色：默认为 "user"（普通用户），管理员为 "admin" */
    @Column(nullable = false, length = 20)
    private String role = "user";

    /** 累计学习天数，用于学习统计 */
    @Column(nullable = false)
    private Integer studyDays;

    /** 累计学习总时长（小时），用于学习统计 */
    @Column(nullable = false)
    private Integer totalHours;
}
