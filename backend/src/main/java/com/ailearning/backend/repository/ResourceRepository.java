package com.ailearning.backend.repository;

import com.ailearning.backend.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 学习资源数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 resources 表的基础CRUD操作。
 * 支持按用户ID查询并按创建时间排序，用于资源列表展示。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /**
     * 根据用户ID查询该用户上传的所有资源，按创建时间降序排列。
     * 最新上传的资源排在最前面。
     *
     * @param userId 用户ID
     * @return 按创建时间倒序排列的资源列表
     */
    List<Resource> findByUserIdOrderByCreatedAtDesc(Long userId);
}
