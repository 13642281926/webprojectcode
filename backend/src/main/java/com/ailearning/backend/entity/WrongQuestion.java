package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 错题本实体类，对应数据库中的 wrong_questions 表。
 * <p>
 * 用于记录用户在学习过程中做错的题目，支持收录题目、正确答案、解析等信息。
 * 提供掌握状态标记（mastered）和错误次数统计（wrongCount），
 * 帮助用户进行针对性复习。支持按分类、难度和标签进行筛选检索。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "wrong_questions")
public class WrongQuestion {

    /** 错题唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户的ID */
    @Column(nullable = false)
    private Long userId;

    /** 错题标题，简要描述题目内容 */
    @Column(nullable = false, length = 500)
    private String title;

    /** 题目正文内容，使用TEXT类型存储长文本 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 正确答案 */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** 题目解析/解题思路，帮助用户理解正确答案 */
    @Column(columnDefinition = "TEXT")
    private String analysis;

    /** 题目分类，如"数学"、"编程"、"英语"等 */
    @Column(length = 50)
    private String category;

    /** 是否已掌握，true 表示用户已完全理解该题 */
    @Column(nullable = false)
    private boolean mastered;

    /** 错题首次收录时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 错题信息最后更新时间 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 题目难度等级，如 "easy"（简单）、"medium"（中等）、"hard"（困难） */
    @Column(length = 20)
    private String difficulty;

    /** 累计错误次数，用于统计薄弱知识点 */
    @Column(nullable = false)
    private int wrongCount;

    /** 标签列表，以逗号分隔，如 "数组,排序,动态规划" */
    @Column(length = 255)
    private String tags;
}
