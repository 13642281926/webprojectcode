package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * 学习计划实体类，对应数据库中的 study_plan 表。
 * <p>
 * 用于存储用户制定的学习计划，支持设置优先级、截止日期和状态跟踪。
 * 用户可以创建多个学习计划，每个计划有独立的完成状态（待办/进行中/已完成），
 * 用于帮助用户规划和管理AI学习进度。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "study_plan")
public class StudyPlan {

    /** 计划唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户的ID */
    @Column(nullable = false)
    private Long userId;

    /** 计划标题 */
    @Column(nullable = false, length = 120)
    private String title;

    /** 计划详细内容 / 描述 */
    @Column(length = 500)
    private String content;

    /** 计划截止日期，可为空表示无截止时间 */
    private LocalDate deadline;

    /**
     * 优先级，可选值如 "high"（高）、"medium"（中）、"low"（低）
     */
    @Column(nullable = false, length = 20)
    private String priority;

    /**
     * 计划状态，可选值如 "pending"（待办）、"in_progress"（进行中）、"completed"（已完成）
     */
    @Column(nullable = false, length = 20)
    private String status;

    /** 计划创建日期 */
    @Column(nullable = false)
    private LocalDate createdAt;
}
