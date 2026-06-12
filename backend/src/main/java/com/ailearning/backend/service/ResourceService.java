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

/**
 * 资源服务 —— 负责学习资源的完整生命周期管理，包括文件上传、列表查询、下载统计等。
 *
 * <p>业务功能：
 * <ol>
 *   <li><b>资源列表</b>：分页查询，支持按分类（电子书/代码示例/试卷真题）、文件类型、关键词筛选。</li>
 *   <li><b>文件上传</b>：接收 MultipartFile，存储到本地文件系统，自动识别文件类型与大小格式化，
 *       同时将文件内容注入 RAG 知识库供 AI 引用。</li>
 *   <li><b>下载计数</b>：每次下载调用 incrementDownload 递增下载次数。</li>
 *   <li><b>统计分析</b>：统计资源总数、总下载量、按分类分布情况。</li>
 * </ol>
 *
 * <p>支持的文件类型：PDF、Word、Markdown、Excel、视频、压缩包、图片等，
 * 上传后通过 RAG 服务解析文本内容建立向量索引。
 */
@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final RagService ragService;

    /** 文件上传目录，默认 ./uploads */
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 构造资源服务。
     *
     * @param resourceRepository 资源数据访问接口
     * @param ragService         RAG 知识库服务（上传时自动注入知识库）
     */
    public ResourceService(ResourceRepository resourceRepository, RagService ragService) {
        this.resourceRepository = resourceRepository;
        this.ragService = ragService;
    }

    /**
     * 分页查询用户资源列表，支持多条件筛选。
     *
     * @param userId   用户 ID
     * @param category 资源分类（"all"/"book"/"code"/"paper"），可为空
     * @param keyword  搜索关键词（匹配标题）
     * @param type     文件类型（pdf/docx/md 等），可为空
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数（默认 12）
     * @return 包含 list 和 total 的 Map
     */
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

    /**
     * 将资源实体转换为前端可用的 Map 视图。
     */
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

    /**
     * 查询资源详情（附带用户归属校验）。
     *
     * @param userId 当前用户 ID
     * @param id     资源 ID
     * @return 资源实体
     * @throws ApiException 404 如果资源不存在或不属于当前用户
     */
    @Transactional(readOnly = true)
    public Resource detail(Long userId, Long id) {
        return resourceRepository.findById(id)
                .filter(res -> res.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "资源不存在"));
    }

    /**
     * 创建资源记录（不包含文件上传，用于手动录入）。
     *
     * @param userId 资源所属用户 ID
     * @param body   资源信息 Map，含 title, type, category, size, url, description
     * @return 保存后的资源实体
     */
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

    /**
     * 删除资源记录（不删除物理文件）。
     *
     * @param userId 当前用户 ID
     * @param id     资源 ID
     * @throws ApiException 404 如果资源不存在或不属于当前用户
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Resource res = detail(userId, id);
        resourceRepository.delete(res);
    }

    /**
     * 资源下载计数 +1。
     *
     * @param userId 当前用户 ID
     * @param id     资源 ID
     * @return 更新后的资源实体
     * @throws ApiException 404 如果资源不存在或不属于当前用户
     */
    @Transactional
    public Resource incrementDownload(Long userId, Long id) {
        Resource res = detail(userId, id);
        res.setDownloadCount(res.getDownloadCount() + 1);
        return resourceRepository.save(res);
    }

    /**
     * 文件上传 —— 存储到本地文件系统并注入 RAG 知识库。
     *
     * <p>处理流程：
     * <ol>
     *   <li>确保上传目录存在（不存在则自动创建）</li>
     *   <li>生成 UUID 文件名保留原始扩展名，避免冲突</li>
     *   <li>将文件流写入磁盘</li>
     *   <li>根据扩展名识别文件类型，格式化文件大小</li>
     *   <li>保存资源实体到数据库</li>
     *   <li>将文件路径传给 RAG 服务解析并建立向量索引</li>
     * </ol>
     *
     * @param userId      上传用户 ID
     * @param file        上传的 MultipartFile 对象
     * @param title       资源标题
     * @param category    资源分类
     * @param description 资源描述
     * @return 保存后的资源实体
     * @throws ApiException 500 如果文件 I/O 操作失败
     */
    @Transactional
    public Resource upload(Long userId, MultipartFile file, String title, String category, String description) {
        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成 UUID 文件名，保留原始扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String newFileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFileName);

            // 将文件写入磁盘
            Files.copy(file.getInputStream(), filePath);

            // 识别文件类型并格式化大小
            String fileType = getFileType(originalFilename);
            String fileSize = formatFileSize(file.getSize());

            // 构建并保存资源实体
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

            // 将文件注入 RAG 知识库，建立向量索引
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

    /**
     * 根据文件扩展名识别文件类型。
     *
     * @param filename 原始文件名
     * @return 文件类型标识（pdf/docx/md/xlsx/mp4/zip/png/unknown）
     */
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

    /**
     * 格式化文件大小为可读字符串。
     *
     * @param size 文件字节数
     * @return 格式化后的字符串（如 "1.50MB"、"256B"）
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        else if (size < 1024 * 1024) return String.format("%.2fKB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.2fMB", size / (1024.0 * 1024.0));
        else return String.format("%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * 获取资源分类选项列表。
     *
     * @return 固定四种分类：全部、电子书、代码示例、试卷真题
     */
    public List<Map<String, String>> categories() {
        return List.of(
                Map.of("id", "all", "name", "全部"),
                Map.of("id", "book", "name", "电子书"),
                Map.of("id", "code", "name", "代码示例"),
                Map.of("id", "paper", "name", "试卷真题")
        );
    }

    /**
     * 资源统计分析（供分析页面使用）。
     *
     * <p>统计指标：资源总数、总下载量、按分类的数量分布。
     *
     * @param userId 用户 ID
     * @return 包含 totalResources、totalDownloads、categoryDistribution 的 Map
     */
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

    /**
     * 判断资源是否匹配搜索关键词（大小写不敏感，只匹配标题）。
     *
     * @param res     资源实体
     * @param keyword 搜索关键词
     * @return 如果 keyword 为空或标题匹配关键词，返回 true
     */
    private boolean matchesKeyword(Resource res, String keyword) {
        if (!StringUtils.hasText(keyword)) return true;
        String lowerKeyword = keyword.toLowerCase();
        return res.getTitle().toLowerCase().contains(lowerKeyword);
    }
}
