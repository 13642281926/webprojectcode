package com.ailearning.backend.repository;

import com.ailearning.backend.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 成就数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 achievements 表的基础CRUD操作。
 * 支持按用户ID查询成就列表，用于用户成就展示页面。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    /**
     * 根据用户ID查询该用户的所有成就记录。
     * 包括已解锁和未解锁的成就，前端根据 unlocked 字段区分展示。
     *
     * @param userId 用户ID
     * @return 该用户的所有成就列表
     */
    List<Achievement> findByUserId(Long userId);
}
