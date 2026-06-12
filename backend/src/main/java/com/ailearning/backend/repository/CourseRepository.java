package com.ailearning.backend.repository;

import com.ailearning.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 课程数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，主键类型为 String（手动分配的课程ID）。
 * 对 course 表提供基础的增删改查操作。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface CourseRepository extends JpaRepository<Course, String> {

    /**
     * 查询所有课程，按课程ID升序排列。
     * 用于课程列表页面的展示，保证输出顺序的一致性。
     *
     * @return 按ID升序排列的课程列表
     */
    List<Course> findAllByOrderByIdAsc();
}
