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

@Service
public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

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

    @Transactional(readOnly = true)
    public Note detail(Long userId, Long id) {
        return noteRepository.findById(id)
                .filter(note -> note.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "笔记不存在"));
    }

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

    @Transactional
    public Note update(Long userId, Long id, Map<String, Object> body) {
        Note note = detail(userId, id);
        if (body.containsKey("title")) note.setTitle(String.valueOf(body.get("title")));
        if (body.containsKey("content")) note.setContent(String.valueOf(body.get("content")));
        if (body.containsKey("category")) note.setCategory(String.valueOf(body.get("category")));
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepository.save(note);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Note note = detail(userId, id);
        noteRepository.delete(note);
    }

    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "study", "name", "学习笔记"),
                Map.of("id", "thought", "name", "心得体会"),
                Map.of("id", "plan", "name", "学习计划")
        );
    }

    private boolean matchesKeyword(Note note, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return note.getTitle().toLowerCase().contains(lowerKeyword) ||
                (note.getContent() != null && note.getContent().toLowerCase().contains(lowerKeyword));
    }
}
