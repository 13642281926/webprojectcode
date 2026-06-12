package com.ailearning.backend.service;

import com.ailearning.backend.entity.Course;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务 —— 负责课程目录的查询、创建、更新、删除等完整 CRUD 操作。
 *
 * <p>业务功能：
 * <ol>
 *   <li><b>课程列表</b>：支持按分类（前端开发/计算机基础/语言学习）和关键词筛选，列表返回时去除章节详情以减少响应体积。</li>
 *   <li><b>课程详情</b>：按 ID 查询完整课程信息（含章节内容）。</li>
 *   <li><b>课程管理</b>：管理员可创建、更新、删除课程，支持设置标题、分类、封面、讲师等属性。</li>
 * </ol>
 *
 * <p>预设三个课程分类：前端开发、计算机基础、语言学习。
 */
@Service
public class CourseService {
    private final CourseRepository courseRepository;

    /**
     * 构造课程服务，注入课程数据仓库。
     *
     * @param courseRepository 课程数据访问接口
     */
    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * 查询课程列表，支持按分类与关键词筛选。
     *
     * <p>筛选规则：
     * <ul>
     *   <li>category 为空或 "all" 时不过滤分类</li>
     *   <li>keyword 为空时不过滤关键词</li>
     *   <li>列表返回时去除 chapters 字段，减少网络传输量</li>
     * </ul>
     *
     * @param category 课程分类（"all"/"frontend"/"cs"/"language"），可为空
     * @param keyword  搜索关键词（匹配标题、描述、讲师），可为空
     * @return 包含 list（课程列表）和 categories（分类选项）的 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(String category, String keyword) {
        // 流式过滤：先按分类过滤，再按关键词过滤，最后去除章节数据
        List<Course> filtered = courseRepository.findAllByOrderByIdAsc().stream()
                .filter(course -> !StringUtils.hasText(category) || "all".equals(category) || category.equals(course.getCategory()))
                .filter(course -> matchesKeyword(course, keyword))
                .map(this::copyCourseWithoutChapters)  // 去除 chapters 以轻量化列表响应
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", filtered);
        data.put("categories", List.of(
                categoryItem("all", "全部"),
                categoryItem("frontend", "前端开发"),
                categoryItem("cs", "计算机基础"),
                categoryItem("language", "语言学习")
        ));
        return data;
    }

    /**
     * 查询课程详情（含完整章节信息）。
     *
     * @param id 课程 ID
     * @return 课程实体（包含 chapters）
     * @throws ApiException 404 如果课程不存在
     */
    @Transactional(readOnly = true)
    public Course detail(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "课程不存在"));
    }

    /**
     * 获取所有课程（不含章节信息），供其他服务调用。
     *
     * @return 所有课程的列表（已去除 chapters）
     */
    @Transactional(readOnly = true)
    public List<Course> allCourses() {
        return courseRepository.findAllByOrderByIdAsc().stream()
                .map(this::copyCourseWithoutChapters)
                .collect(Collectors.toList());
    }

    /**
     * 创建新课程（管理员）。
     *
     * <p>从 Map 中提取课程属性，lessons 字段兼容 Integer 和 String 类型。
     *
     * @param body 课程信息 Map，含 id, title, category, cover, description, teacher, lessons
     * @return 保存后的课程实体
     */
    @Transactional
    public Course create(Map<String, Object> body) {
        Course course = new Course();
        course.setId((String) body.get("id"));
        course.setTitle((String) body.get("title"));
        course.setCategory((String) body.get("category"));
        course.setCover((String) body.get("cover"));
        course.setDescription((String) body.get("description"));
        course.setTeacher((String) body.get("teacher"));
        course.setLessons(body.get("lessons") instanceof Integer
                ? (Integer) body.get("lessons")
                : Integer.parseInt(String.valueOf(body.get("lessons"))));
        course.setProgress(0);
        return courseRepository.save(course);
    }

    /**
     * 更新课程信息（管理员）。
     *
     * <p>只更新 body 中非空的字段，不会覆盖未提供的属性。
     *
     * @param id   课程 ID
     * @param body 要更新的字段 Map
     * @return 更新后的课程实体
     * @throws ApiException 404 如果课程不存在
     */
    @Transactional
    public Course update(String id, Map<String, Object> body) {
        Course course = detail(id);
        if (body.containsKey("title")) course.setTitle((String) body.get("title"));
        if (body.containsKey("category")) course.setCategory((String) body.get("category"));
        if (body.containsKey("cover")) course.setCover((String) body.get("cover"));
        if (body.containsKey("description")) course.setDescription((String) body.get("description"));
        if (body.containsKey("teacher")) course.setTeacher((String) body.get("teacher"));
        if (body.containsKey("lessons")) {
            course.setLessons(body.get("lessons") instanceof Integer
                    ? (Integer) body.get("lessons")
                    : Integer.parseInt(String.valueOf(body.get("lessons"))));
        }
        return courseRepository.save(course);
    }

    /**
     * 删除课程（管理员）。
     *
     * @param id 课程 ID
     * @throws ApiException 404 如果课程不存在
     */
    @Transactional
    public void delete(String id) {
        Course course = detail(id);
        courseRepository.delete(course);
    }

    /**
     * 判断课程是否匹配搜索关键词（大小写不敏感）。
     *
     * <p>搜索范围：课程标题、描述、讲师。
     *
     * @param course  课程实体
     * @param keyword 搜索关键词
     * @return 如果 keyword 为空或课程匹配关键词，返回 true
     */
    private boolean matchesKeyword(Course course, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return course.getTitle().toLowerCase().contains(lowerKeyword) ||
                course.getDescription().toLowerCase().contains(lowerKeyword) ||
                course.getTeacher().toLowerCase().contains(lowerKeyword);
    }

    /**
     * 构造分类选项 Map（value=ID，label=显示名称）。
     *
     * @param id   分类 ID
     * @param name 分类显示名称
     * @return 包含 value 和 label 的 Map
     */
    private Map<String, String> categoryItem(String id, String name) {
        Map<String, String> item = new HashMap<>();
        item.put("value", id);
        item.put("label", name);
        return item;
    }

    /**
     * 复制课程对象，但去除 chapters 字段。
     *
     * <p>列表查询时避免传输大量章节 JSON 数据，减少响应体积，提升前端渲染速度。
     *
     * @param original 原始课程实体
     * @return 不包含 chapters 的课程副本
     */
    private Course copyCourseWithoutChapters(Course original) {
        Course copy = new Course();
        copy.setId(original.getId());
        copy.setTitle(original.getTitle());
        copy.setCategory(original.getCategory());
        copy.setCover(original.getCover());
        copy.setDescription(original.getDescription());
        copy.setProgress(original.getProgress());
        copy.setTeacher(original.getTeacher());
        copy.setLessons(original.getLessons());
        copy.setKnowledgePoints(original.getKnowledgePoints());
        // 不复制 chapters，避免列表接口响应过大
        return copy;
    }
}
