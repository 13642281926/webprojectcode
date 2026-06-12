package com.ailearning.backend.repository;

import com.ailearning.backend.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 学习计划数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 study_plan 表的基础CRUD操作。
 * 支持按用户ID查询计划列表，用于用户个人学习计划管理。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    /**
     * 根据用户ID查询该用户的所有学习计划，按计划ID降序排列。
     * 最新创建的计划排在最前面。
     *
     * @param userId 用户ID
     * @return 按ID倒序排列的学习计划列表
     */
    List<StudyPlan> findByUserIdOrderByIdDesc(Long userId);
}
