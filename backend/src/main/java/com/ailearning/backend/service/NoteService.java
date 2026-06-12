package com.ailearning.backend.service;

import com.ailearning.backend.entity.Note;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 笔记服务 —— 负责个人学习笔记的增删改查与分类管理。
 *
 * <p>业务功能：
 * <ol>
 *   <li><b>笔记列表</b>：按用户 ID 查询，支持按分类（学习笔记/心得体会/学习计划）和关键词筛选，按更新时间倒序排列。</li>
 *   <li><b>笔记详情</b>：按 ID 查询，附带用户归属校验（只能查看自己的笔记）。</li>
 *   <li><b>创建/更新/删除</b>：自动维护 createdAt 和 updatedAt 时间戳。</li>
 *   <li><b>分类字典</b>：提供固定的分类选项列表供前端下拉使用。</li>
 * </ol>
 */
@Service
public class NoteService {
    private final NoteRepository noteRepository;

    /**
     * 构造笔记服务，注入笔记数据仓库。
     *
     * @param noteRepository 笔记数据访问接口
     */
    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    /**
     * 查询当前用户的笔记列表，支持分类和关键词筛选。
     *
     * <p>按更新时间倒序排列，分类为 "all" 或空时不筛选分类。
     *
     * @param userId   用户 ID
     * @param category 笔记分类（"all"/"study"/"thought"/"plan"），可为空
     * @param keyword  搜索关键词（匹配标题和内容），可为空
     * @return 包含 list（笔记列表）的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId, String category, String keyword) {
        List<Note> notes = noteRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .filter(note -> !StringUtils.hasText(category) || "all".equals(category) || category.equals(note.getCategory()))
                .filter(note -> matchesKeyword(note, keyword))
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", notes);
        return data;
    }

    /**
     * 查询笔记详情，附带用户归属校验。
     *
     * @param userId 当前用户 ID（必须与笔记所属用户一致）
     * @param id     笔记 ID
     * @return 笔记实体
     * @throws ApiException 404 如果笔记不存在或不属于当前用户
     */
    @Transactional(readOnly = true)
    public Note detail(Long userId, Long id) {
        return noteRepository.findById(id)
                .filter(note -> note.getUserId().equals(userId))  // 用户归属校验
                .orElseThrow(() -> new ApiException(404, "笔记不存在"));
    }

    /**
     * 创建新笔记。
     *
     * @param userId 笔记所属用户 ID
     * @param body   笔记信息 Map，含 title, content, category
     * @return 保存后的笔记实体
     */
    @Transactional
    public Note create(Long userId, Map<String, Object> body) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(String.valueOf(body.get("title")));
        note.setContent(String.valueOf(body.get("content")));
        note.setCategory(String.valueOf(body.get("category")));
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    /**
     * 更新笔记（仅更新 body 中提供的字段）。
     *
     * @param userId 当前用户 ID
     * @param id     笔记 ID
     * @param body   要更新的字段 Map
     * @return 更新后的笔记实体
     * @throws ApiException 404 如果笔记不存在或不属于当前用户
     */
    @Transactional
    public Note update(Long userId, Long id, Map<String, Object> body) {
        Note note = detail(userId, id);
        if (body.containsKey("title")) note.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("content")) note.setContent(String.valueOf(body.get("content")));
        if (body.containsKey("category")) note.setCategory(String.valueOf(body.get("category")));
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    /**
     * 删除笔记。
     *
     * @param userId 当前用户 ID
     * @param id     笔记 ID
     * @throws ApiException 404 如果笔记不存在或不属于当前用户
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Note note = detail(userId, id);
        noteRepository.delete(note);
    }

    /**
     * 获取笔记分类选项列表。
     *
     * @return 固定四种分类：全部、学习笔记、心得体会、学习计划
     */
    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "study", "name", "学习笔记"),
                Map.of("id", "thought", "name", "心得体会"),
                Map.of("id", "plan", "name", "学习计划")
        );
    }

    /**
     * 判断笔记是否匹配搜索关键词（大小写不敏感）。
     *
     * <p>搜索范围：笔记标题、笔记内容。
     *
     * @param note    笔记实体
     * @param keyword 搜索关键词
     * @return 如果 keyword 为空或笔记匹配关键词，返回 true
     */
    private boolean matchesKeyword(Note note, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return note.getTitle().toLowerCase().contains(lowerKeyword) ||
                (note.getContent() != null && note.getContent().toLowerCase().contains(lowerKeyword));
    }
}
