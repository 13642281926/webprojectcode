package com.ailearning.backend.repository;

import com.ailearning.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 用户数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 users 表的基础CRUD操作。
 * 通过方法名约定自动生成查询，无需编写SQL。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户。
     * Spring Data JPA 自动根据方法名生成 {@code WHERE username = ?} 查询。
     *
     * @param username 登录用户名（唯一）
     * @return 包含 User 的 Optional，不存在时为空
     */
    Optional<User> findByUsername(String username);
}
