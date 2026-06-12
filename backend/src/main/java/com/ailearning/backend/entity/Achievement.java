package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 成就实体类，对应数据库中的 achievements 表。
 * <p>
 * 用于存储平台中用户的学习成就信息，实现游戏化激励机制。
 * 每个成就有进度（progress）和目标（target）的概念，当进度达到目标值时成就解锁。
 * 支持设置稀有度（rarity）和分类（category）以区分不同级别的成就，
 * 解锁后用户可获得对应的积分奖励（points）。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "achievements")
public class Achievement {

    /** 成就唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户的ID */
    @Column(nullable = false)
    private Long userId;

    /** 成就名称/标题 */
    @Column(nullable = false, length = 100)
    private String title;

    /** 成就的详细描述 */
    @Column(length = 255)
    private String description;

    /** 成就图标的URL或CSS类名 */
    @Column(length = 100)
    private String icon;

    /** 是否已解锁 */
    @Column(nullable = false)
    private boolean unlocked;

    /** 成就解锁时间，未解锁时为 null */
    private LocalDateTime unlockedAt;

    /** 当前进度值，如已学习天数、完成课程数等 */
    @Column(nullable = false)
    private int progress;

    /** 目标值，达到该值时成就自动解锁 */
    @Column(nullable = false)
    private int target;

    /** 成就分类，如"学习类"、"社交类"等 */
    @Column(length = 50)
    private String category;

    /** 稀有度等级，如 "common"（普通）、"rare"（稀有）、"epic"（史诗） */
    @Column(length = 50)
    private String rarity;

    /** 解锁奖励积分数 */
    @Column(nullable = false)
    private int points;
}
