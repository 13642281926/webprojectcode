package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.service.ResourceService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 学习资源控制器，提供资源文件的上传、下载、列表查询和分类管理接口。
 * <p>
 * 资源模块是平台的资料共享中心，用户可上传学习资料（如 PDF、视频、笔记文件等），
 * 按类型（文档/视频/音频/其他）和分类归档，支持关键词搜索和分页浏览。
 * 下载功能会记录下载行为并累加下载计数，文件名自动进行 UTF-8 编码以兼容各种浏览器。
 * 所有资源与用户关联，用户可管理自己上传的资源。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {
    private final ResourceService resourceService;

    /**
     * 构造函数，通过 Spring IoC 注入资源服务。
     *
     * @param resourceService 资源业务服务
     */
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 获取当前用户的资源列表，支持多维度筛选和分页。
     * 按资源类型（文档/视频/音频）、分类和关键词进行过滤，方便用户快速找到所需资料。
     *
     * @param category 资源分类（可选），如"考试资料"、"课件"
     * @param keyword  搜索关键词（可选），匹配资源标题
     * @param type     资源类型（可选），如"document"/"video"/"audio"/"other"
     * @param page     页码（可选），用于分页
     * @param pageSize 每页条数（可选），用于分页
     * @return 包含资源列表和分页信息的响应结果
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(resourceService.list(AuthContext.getCurrentUserId(), category, keyword, type, page, pageSize));
    }

    /**
     * 查看单条资源的详细元数据信息。
     * 返回资源的标题、类型、分类、文件大小、下载次数、创建时间等元信息，
     * 不包含文件实际内容，文件获取需通过下载接口。
     *
     * @param id 资源ID
     * @return 包含资源元信息的键值对结构
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        var res = resourceService.detail(AuthContext.getCurrentUserId(), id);
        // 将资源实体转换为前端友好的 Map 结构
        return ApiResponse.success(Map.of(
                "id", res.getId(),
                "title", res.getTitle(),
                "type", res.getType(),
                "category", res.getCategory(),
                "size", res.getSize(),
                "url", res.getUrl(),
                "description", res.getDescription(),
                "downloadCount", res.getDownloadCount(),
                "createTime", res.getCreatedAt().toString()
        ));
    }

    /**
     * 通过文件上传方式创建新资源。
     * 接收前端表单提交的文件流（MultipartFile），将文件存储到服务器后创建资源记录。
     * 适用于需要将本地文件上传到平台的场景。
     *
     * @param file        上传的文件对象
     * @param title       资源标题
     * @param category    资源分类（可选）
     * @param description 资源描述（可选）
     * @return 上传成功，返回新资源的ID
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description
    ) {
        var res = resourceService.upload(AuthContext.getCurrentUserId(), file, title, category, description);
        return ApiResponse.success("上传成功", Map.of("id", res.getId()));
    }

    /**
     * 通过 JSON 数据创建新资源（不含文件上传）。
     * 适用于资源为外部链接或云存储地址的场景，仅记录元数据不涉及文件存储。
     *
     * @param body 请求体，包含 title/type/category/url/description/size 等字段
     * @return 创建成功，返回新资源的ID
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        var res = resourceService.create(AuthContext.getCurrentUserId(), body);
        return ApiResponse.success("上传成功", Map.of("id", res.getId()));
    }

    /**
     * 删除指定资源及其关联的文件。
     * 删除资源记录的同时从磁盘中移除对应的物理文件，释放存储空间。
     *
     * @param id 待删除的资源ID
     * @return 删除成功的空响应
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        resourceService.delete(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 下载资源文件。
     * 首先递增下载计数以统计热门资源，然后读取实际文件并通过 HTTP 响应流返回。
     * 文件名使用 UTF-8 编码并设置 Content-Disposition 头，触发浏览器下载保存对话框。
     * 如果文件不存在或不可读，返回 404；如果读取失败，返回 500。
     *
     * @param id 待下载的资源ID
     * @return 包含文件二进制流的 ResponseEntity，或错误状态码
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        // 先递增下载计数，记录此次下载行为
        var res = resourceService.incrementDownload(AuthContext.getCurrentUserId(), id);
        try {
            Path filePath = Paths.get(res.getUrl());
            Resource resource = new UrlResource(filePath.toUri());

            // 校验文件是否实际存在于磁盘且可读
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            // 对文件名进行 UTF-8 编码，确保中英文文件名在各浏览器中正确显示
            String encodedFileName = URLEncoder.encode(res.getTitle(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取资源分类列表。
     * 返回系统中所有可用的资源分类，用于前端分类筛选和统计展示。
     *
     * @return 包含所有资源分类键值对的列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, String>>> categories() {
        return ApiResponse.success(resourceService.categories());
    }
}
