package com.ailearning.backend.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG（检索增强生成）服务 —— 实现基于用户个人知识库的 AI 增强问答。
 *
 * <p>RAG 流水线（Pipeline）分为三个阶段：
 *
 * <h3>一、文档摄取（Ingestion）</h3>
 * <ol>
 *   <li>根据文件扩展名选择文档解析器（PDF 用 PdfBox、TXT/MD 用 Text、其他用 Tika）</li>
 *   <li>解析文档内容并附加元数据（userId、resourceId、title）</li>
 *   <li>使用递归文本分割器（Recursive Document Splitter，块大小 500 字符，重叠 100 字符）切分文档</li>
 *   <li>通过嵌入模型（EmbeddingModel）将每个文本块转为向量</li>
 *   <li>将向量和文本段存入向量存储（EmbeddingStore）</li>
 *   <li>维护用户-资源映射关系，便于后续管理</li>
 * </ol>
 *
 * <h3>二、语义检索（Retrieval）</h3>
 * <ol>
 *   <li>将用户查询文本通过嵌入模型转为查询向量</li>
 *   <li>在向量存储中执行语义相似度搜索（最小相似度阈值 0.7）</li>
 *   <li>过滤结果：仅返回属于当前用户的文档片段（数据隔离）</li>
 *   <li>格式化返回内容，附加文档来源标签</li>
 * </ol>
 *
 * <h3>三、提示词增强（Prompt Augmentation）</h3>
 * <ol>
 *   <li>将检索到的相关文档片段拼接为"参考资料"</li>
 *   <li>附加原始用户问题，构建增强版提示词</li>
 *   <li>如果没有相关参考资料，直接返回原始问题</li>
 * </ol>
 *
 * <p>技术栈：LangChain4j（文档解析/分割/嵌入/向量存储一体化框架）。
 */
@Service
public class RagService {

    /** 向量存储，用于保存和检索文档嵌入向量 */
    private final EmbeddingStore<TextSegment> embeddingStore;
    /** 嵌入模型，用于将文本转换为向量 */
    private final EmbeddingModel embeddingModel;

    /** 文件上传目录 */
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    /** 用户-资源映射：Key=用户ID，Value=该用户拥有的资源ID列表 */
    private final Map<Long, List<String>> userResourceMapping = new ConcurrentHashMap<>();

    /**
     * 构造 RAG 服务。
     *
     * @param embeddingStore 向量存储（Spring 自动注入）
     * @param embeddingModel 嵌入模型（Spring 自动注入）
     */
    public RagService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 【RAG Phase 1】将用户上传的资源文件添加到知识库。
     *
     * <p>完整摄取流水线：
     * <ol>
     *   <li>校验文件是否存在</li>
     *   <li>根据扩展名选择合适的文档解析器</li>
     *   <li>解析文档并附加元数据（userId、resourceId、title、description）</li>
     *   <li>递归分割文档（块大小 500 字符，重叠 100 字符）</li>
     *   <li>嵌入并存入向量存储</li>
     *   <li>更新用户-资源映射关系</li>
     * </ol>
     *
     * @param userId      所属用户 ID
     * @param resourceId  资源记录 ID
     * @param filePath    文件在磁盘上的绝对路径
     * @param title       资源标题（存入元数据）
     * @param description 资源描述（存入元数据）
     */
    public void addResourceToKnowledgeBase(Long userId, Long resourceId, String filePath, String title, String description) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("File not found: " + filePath);
                return;
            }

            // Step 1: 选择文档解析器
            DocumentParser parser = getDocumentParser(filePath);

            try (InputStream inputStream = new FileInputStream(filePath)) {
                // Step 2: 解析文档内容
                Document document = parser.parse(inputStream);

                // Step 3: 附加元数据
                document.metadata().put("userId", userId.toString());
                document.metadata().put("resourceId", resourceId.toString());
                document.metadata().put("title", title);
                if (description != null && !description.isEmpty()) {
                    document.metadata().put("description", description);
                }

                // Step 4-6: 分割、嵌入、存储（LangChain4j Ingestor 一站式处理）
                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                        .documentSplitter(DocumentSplitters.recursive(500, 100))  // 递归分割：块500字符，重叠100字符
                        .embeddingModel(embeddingModel)                            // 文本 -> 向量
                        .embeddingStore(embeddingStore)                            // 向量持久化存储
                        .build();

                ingestor.ingest(document);

                // Step 7: 维护用户-资源映射
                userResourceMapping.computeIfAbsent(userId, k -> new ArrayList<>()).add(resourceId.toString());
                
                System.out.println("Resource added to knowledge base: " + title);
            }
        } catch (Exception e) {
            System.err.println("Error adding resource to knowledge base: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 【RAG Phase 2】语义检索 —— 根据用户查询从知识库中搜索相关内容。
     *
     * <p>检索流程：
     * <ol>
     *   <li>将查询文本嵌入为向量</li>
     *   <li>在向量存储中执行相似度搜索（最小相似度 0.7）</li>
     *   <li>过滤结果：只返回属于当前用户的文档片段（多租户数据隔离）</li>
     *   <li>格式化输出：附加文档来源标签</li>
     * </ol>
     *
     * @param userId     用户 ID（用于数据隔离）
     * @param query      语义查询文本
     * @param maxResults 最大返回结果数
     * @return 相关文档片段列表（已格式化），异常时返回空列表
     */
    public List<String> searchRelevantContent(Long userId, String query, int maxResults) {
        try {
            // 查询向量嵌入 -> 语义相似度搜索（最小相似度阈值 0.7）
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    embeddingModel.embed(query).content(),
                    maxResults,
                    0.7       // 最低相似度：低于此值的结果被过滤
            );

            List<String> relevantContents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                // 数据隔离：只返回当前用户的文档片段
                String segmentUserId = segment.metadata().getString("userId");

                if (segmentUserId != null && segmentUserId.equals(userId.toString())) {
                    String title = segment.metadata().getString("title");
                    String content = segment.text();
                    // 格式化输出：附带来源标签
                    relevantContents.add(String.format("[来源: %s]\n%s", title, content));
                }
            }

            return relevantContents;
        } catch (Exception e) {
            System.err.println("Error searching knowledge base: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从知识库映射中移除资源记录（不删除向量存储中的数据）。
     *
     * @param userId     用户 ID
     * @param resourceId 资源 ID
     */
    public void removeResourceFromKnowledgeBase(Long userId, Long resourceId) {
        userResourceMapping.computeIfPresent(userId, (k, v) -> {
            v.remove(resourceId.toString());
            return v.isEmpty() ? null : v;
        });
    }

    /**
     * 【RAG Phase 3】构建增强提示词 —— 将检索到的参考资料注入用户问题。
     *
     * <p>如果没有检索到相关资料，直接返回原始问题（退化为普通 AI 对话）。
     *
     * @param userId    用户 ID
     * @param userQuery 原始用户问题
     * @return 增强后的提示词（包含参考资料 + 用户问题）
     */
    public String buildRagPrompt(Long userId, String userQuery) {
        // 检索最多 3 条相关文档片段
        List<String> relevantContents = searchRelevantContent(userId, userQuery, 3);

        if (relevantContents.isEmpty()) {
            return userQuery;  // 无参考资料，直接返回原始问题
        }

        // 拼接参考资料
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请基于以下参考资料回答用户的问题：\n\n");

        for (int i = 0; i < relevantContents.size(); i++) {
            promptBuilder.append(String.format("参考资料%d:\n%s\n\n", i + 1, relevantContents.get(i)));
        }

        promptBuilder.append("用户问题: ").append(userQuery);

        return promptBuilder.toString();
    }

    /**
     * 根据文件扩展名选择合适的文档解析器。
     *
     * <p>解析器选择策略：
     * <ul>
     *   <li>.pdf -> Apache PDFBox 解析器</li>
     *   <li>.txt / .md -> 纯文本解析器</li>
     *   <li>其他 -> Apache Tika 通用解析器（支持 doc/docx/xlsx/pptx 等）</li>
     * </ul>
     *
     * @param filePath 文件路径
     * @return 对应的文档解析器实例
     */
    private DocumentParser getDocumentParser(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".pdf")) {
            return new ApachePdfBoxDocumentParser();
        } else if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".md")) {
            return new TextDocumentParser();
        } else {
            return new ApacheTikaDocumentParser();
        }
    }
}
