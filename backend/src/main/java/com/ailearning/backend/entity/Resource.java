package com.ailearning.backend.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习资源实体类，对应数据库中的 resources 表。
 * <p>
 * 用于存储平台中的学习资源信息，包括资料标题、类型、分类、文件大小及下载地址等。
 * 用户可上传或分享学习资料，系统记录下载次数以反映资源的热度/受欢迎程度。
 * 每条资源关联一个上传用户（通过 userId）。
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Data
@Entity
@Table(name = "resources")
public class Resource {

    /** 资源唯一标识，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 上传/所属用户的ID */
    @Column(nullable = false)
    private Long userId;

    /** 资源标题 */
    @Column(nullable = false, length = 150)
    private String title;

    /** 资源类型，如 "PDF"、"视频"、"PPT"、"文章" 等 */
    @Column(length = 50)
    private String type;

    /** 资源分类，如"编程资料"、"数学资料"等 */
    @Column(length = 50)
    private String category;

    /** 文件大小（字符串格式），如 "2.5MB" */
    @Column(nullable = false)
    private String size;

    /** 文件下载地址或在线访问URL */
    @Column(nullable = false)
    private String url;

    /** 资源创建/上传时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 资源描述信息 */
    @Column(length = 500)
    private String description;

    /** 下载次数，用于排序和热门推荐 */
    @Column(nullable = false)
    private int downloadCount;
}
