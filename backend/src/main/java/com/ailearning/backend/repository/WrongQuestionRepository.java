package com.ailearning.backend.repository;

import com.ailearning.backend.entity.WrongQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 错题本数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 wrong_questions 表的基础CRUD操作。
 * 支持按用户ID查询并按更新时间排序，用于用户的错题复习页面。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, Long> {

    /**
     * 根据用户ID查询该用户的所有错题，按更新时间降序排列。
     * 最近修改/收录的错题排在最前面。
     *
     * @param userId 用户ID
     * @return 按更新时间倒序排列的错题列表
     */
    List<WrongQuestion> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
