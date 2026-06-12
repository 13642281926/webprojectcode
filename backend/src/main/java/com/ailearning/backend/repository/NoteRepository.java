package com.ailearning.backend.repository;

import com.ailearning.backend.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 学习笔记数据访问层接口。
 * <p>
 * 继承 Spring Data JPA 的 {@link JpaRepository}，提供对 notes 表的基础CRUD操作。
 * 支持按用户ID查询并排序，用于我的笔记列表展示。
 * </p>
 *
 * @author AI学习成长助手平台
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 根据用户ID查询该用户的所有笔记，按更新时间降序排列。
     * 最近修改的笔记排在最前面，方便用户快速找到最新内容。
     *
     * @param userId 用户ID
     * @return 按更新时间倒序排列的笔记列表
     */
    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
