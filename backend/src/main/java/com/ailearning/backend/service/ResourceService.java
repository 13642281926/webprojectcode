package com.ailearning.backend.service;

import com.ailearning.backend.entity.Resource;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final RagService ragService;

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    public ResourceService(ResourceRepository resourceRepository, RagService ragService) {
        this.resourceRepository = resourceRepository;
        this.ragService = ragService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(Long userId, String category, String keyword, String type, Integer page, Integer pageSize) {
        List<Resource> resources = resourceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(res -> !StringUtils.hasText(category) || "all".equals(category) || category.equals(res.getCategory()))
                .filter(res -> !StringUtils.hasText(type) || type.equals(res.getType()))
                .filter(res -> matchesKeyword(res, keyword))
                .collect(Collectors.toList());

        int total = resources.size();
        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 12;
        int start = (pageNum - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> resultList = resources.subList(start, end).stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultList);
        data.put("total", total);
        return data;
    }

    private Map<String, Object> convertToMap(Resource res) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", res.getId());
        m.put("title", res.getTitle());
        m.put("type", res.getType());
        m.put("category", res.getCategory());
        m.put("size", res.getSize());
        m.put("url", res.getUrl());
        m.put("description", res.getDescription());
        m.put("downloadCount", res.getDownloadCount());
        m.put("createTime", res.getCreatedAt().toString());
        return m;
    }

    @Transactional(readOnly = true)
    public Resource detail(Long userId, Long id) {
        return resourceRepository.findById(id)
                .filter(res -> res.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "资源不存在"));
    }

    @Transactional
    public Resource create(Long userId, Map<String, Object> body) {
        Resource res = new Resource();
        res.setUserId(userId);
        res.setTitle(String.valueOf(body.get("title")));
        res.setType(String.valueOf(body.getOrDefault("type", "pdf")));
        res.setCategory(String.valueOf(body.getOrDefault("category", "")));
        res.setSize(String.valueOf(body.getOrDefault("size", "1MB")));
        res.setUrl(String.valueOf(body.getOrDefault("url", "")));
        res.setDescription(String.valueOf(body.getOrDefault("description", "")));
        res.setDownloadCount(0);
        res.setCreatedAt(LocalDateTime.now());
        return resourceRepository.save(res);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Resource res = detail(userId, id);
        resourceRepository.delete(res);
    }

    @Transactional
    public Resource incrementDownload(Long userId, Long id) {
        Resource res = detail(userId, id);
        res.setDownloadCount(res.getDownloadCount() + 1);
        return resourceRepository.save(res);
    }

    @Transactional
    public Resource upload(Long userId, MultipartFile file, String title, String category, String description) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String newFileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFileName);

            Files.copy(file.getInputStream(), filePath);

            String fileType = getFileType(originalFilename);
            String fileSize = formatFileSize(file.getSize());

            Resource res = new Resource();
            res.setUserId(userId);
            res.setTitle(title);
            res.setType(fileType);
            res.setCategory(category != null ? category : "");
            res.setSize(fileSize);
            res.setUrl(filePath.toAbsolutePath().toString());
            res.setDescription(description != null ? description : "");
            res.setDownloadCount(0);
            res.setCreatedAt(LocalDateTime.now());
            Resource savedRes = resourceRepository.save(res);

            ragService.addResourceToKnowledgeBase(
                userId,
                savedRes.getId(),
                filePath.toAbsolutePath().toString(),
                title,
                description
            );

            return savedRes;

        } catch (IOException e) {
            throw new ApiException(500, "文件上传失败: " + e.getMessage());
        }
    }

    private String getFileType(String filename) {
        if (filename == null) return "unknown";
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "docx";
            case "md" -> "md";
            case "xls", "xlsx" -> "xlsx";
            case "mp4", "avi", "mov" -> "mp4";
            case "zip", "rar", "7z" -> "zip";
            case "png", "jpg", "jpeg", "gif" -> "png";
            default -> "zip";
        };
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        else if (size < 1024 * 1024) return String.format("%.2fKB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.2fMB", size / (1024.0 * 1024.0));
        else return String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
    }

    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "book", "name", "电子书"),
                Map.of("id", "code", "name", "代码示例"),
                Map.of("id", "paper", "name", "试卷真题")
        );
    }

    /** 分析页：资源总数 / 分类分布 / 总下载量 */
    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long userId) {
        var resources = resourceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int total = resources.size();
        int totalDownloads = resources.stream().mapToInt(Resource::getDownloadCount).sum();
        Map<String, Long> byCategory = resources.stream()
                .collect(Collectors.groupingBy(Resource::getCategory, Collectors.counting()));
        List<Map<String, Object>> categoryDistribution = byCategory.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey() == null || e.getKey().isEmpty() ? "未分类" : e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("totalResources", total);
        data.put("totalDownloads", totalDownloads);
        data.put("categoryDistribution", categoryDistribution);
        return data;
    }

    private boolean matchesKeyword(Resource res, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return res.getTitle().toLowerCase().contains(lowerKeyword);
    }
}
