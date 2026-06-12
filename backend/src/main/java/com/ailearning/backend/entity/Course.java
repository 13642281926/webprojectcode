package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程实体类，对应数据库中的 course 表。
 * <p>
 * 用于存储平台的AI学习课程信息，包括课程基本信息、知识点列表以及章节列表。
 * 课程与章节（CourseChapter）之间为一对多关系，级联操作保证添加/删除课程时同步处理章节。
 * 知识点列表通过 {@code @ElementCollection} 映射到独立的 course_knowledge_point 表，
 * 支持排序以保持知识点的逻辑顺序。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "course")
public class Course {

    /** 课程唯一标识，手动分配（非自增），长度上限40字符 */
    @Id
    @Column(nullable = false, length = 40)
    private String id;

    /** 课程标题 */
    @Column(nullable = false, length = 120)
    private String title;

    /** 课程所属分类，如"编程"、"数学"等 */
    @Column(nullable = false, length = 40)
    private String category;

    /** 课程封面图片的URL地址 */
    @Column(length = 255)
    private String cover;

    /** 课程简介/描述 */
    @Column(nullable = false, length = 500)
    private String description;

    /** 课程学习进度，以百分比或课时数表示 */
    @Column(nullable = false)
    private int progress;

    /** 课程讲师名称 */
    @Column(nullable = false, length = 50)
    private String teacher;

    /** 课程总课时数 */
    @Column(nullable = false)
    private int lessons;

    /**
     * 课程知识点列表。
     * 使用 {@code @ElementCollection} 映射到 course_knowledge_point 表，
     * 通过 sort_order 字段保持知识点在界面展示时的排序。
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_knowledge_point", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "knowledge_point", nullable = false, length = 100)
    @OrderColumn(name = "sort_order")
    private List<String> knowledgePoints = new ArrayList<>();

    /**
     * 课程下属章节列表，与 CourseChapter 实体为一对多关系。
     * 设置 {@code orphanRemoval = true} 以确保从课程中移除章节时自动删除数据库记录。
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "sort_order")
    private List<CourseChapter> chapters = new ArrayList<>();
}
