package com.ailearning.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程章节实体类，对应数据库中的 course_chapter 表。
 * <p>
 * 每个章节隶属于一个课程（Course），通过 {@code @ManyToOne} 建立多对一关联关系。
 * 使用 {@code @JsonIgnore} 注解防止JSON序列化时产生循环引用（课程中包含章节列表，
 * 章节又反向引用课程），同时采用懒加载策略优化查询性能。
 * 提供全参构造器和无参构造器以方便测试和框架使用。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_chapter")
public class CourseChapter {

    /** 章节唯一标识，手动分配（非自增），长度上限50字符 */
    @Id
    @Column(nullable = false, length = 50)
    private String id;

    /** 章节标题 */
    @Column(nullable = false, length = 150)
    private String title;

    /** 章节视频/内容的时长，如 "12:30" 格式 */
    @Column(nullable = false, length = 30)
    private String duration;

    /** 标记该章节是否已学习完成 */
    @Column(nullable = false)
    private boolean done;

    /**
     * 所属课程。
     * 使用 {@code @JsonIgnore} 避免JSON序列化时的循环嵌套问题,
     * 并通过 {@code FetchType.LAZY} 实现按需加载以提升性能。
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
