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

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String category, String keyword) {
        List<Course> filtered = courseRepository.findAllByOrderByIdAsc().stream()
                .filter(course -> !StringUtils.hasText(category) || "all".equals(category) || category.equals(course.getCategory()))
                .filter(course -> matchesKeyword(course, keyword))
                .map(this::copyCourseWithoutChapters)
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

    @Transactional(readOnly = true)
    public Course detail(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "课程不存在"));
    }

    @Transactional(readOnly = true)
    public List<Course> allCourses() {
        return courseRepository.findAllByOrderByIdAsc().stream()
                .map(this::copyCourseWithoutChapters)
                .collect(Collectors.toList());
    }

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

    @Transactional
    public void delete(String id) {
        Course course = detail(id);
        courseRepository.delete(course);
    }

    private boolean matchesKeyword(Course course, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return course.getTitle().toLowerCase().contains(lowerKeyword) ||
                course.getDescription().toLowerCase().contains(lowerKeyword) ||
                course.getTeacher().toLowerCase().contains(lowerKeyword);
    }

    private Map<String, String> categoryItem(String id, String name) {
        Map<String, String> item = new HashMap<>();
        item.put("value", id);
        item.put("label", name);
        return item;
    }

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
        // Do not copy chapters to avoid large response in list
        return copy;
    }
}
