package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习笔记实体类，对应数据库中的 notes 表。
 * <p>
 * 用于存储用户在学习过程中创建的笔记，包含笔记标题、正文内容、分类等信息。
 * 每条笔记关联一个用户（通过 userId），记录创建时间和最后修改时间，
 * 支持按分类筛选和按时间排序展示。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "notes")
public class Note {

    /** 笔记唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户的ID */
    @Column(nullable = false)
    private Long userId;

    /** 笔记标题 */
    @Column(nullable = false, length = 150)
    private String title;

    /** 笔记正文内容，使用TEXT类型存储长文本 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 笔记分类，如"课堂笔记"、"读书笔记"等 */
    @Column(length = 50)
    private String category;

    /** 笔记创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 笔记最后更新时间 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
