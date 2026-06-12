package com.ailearning.backend.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG（检索增强生成）配置类。
 * <p>
 * 配置 LangChain4j 框架所需的嵌入模型（EmbeddingModel）和向量存储（EmbeddingStore）。
 * 使用 DeepSeek 提供的 text-embedding-v3 模型将文本转换为向量，
 * 向量存储在内存中（{@link InMemoryEmbeddingStore}），适用于开发和小规模应用场景。
 * </p>
 * <p>
 * Bean装配说明：
 * <ul>
 *   <li>{@code embeddingStore}：向量存储，存放文本段（TextSegment）及其向量表示，
 *       用于后续的语义检索。</li>
 *   <li>{@code embeddingModel}：嵌入模型，调用 DeepSeek API 将文本转为向量。
 *       使用 OpenAI 兼容的接口格式（baseUrl 指向 DeepSeek 网关）。</li>
 * </ul>
 * </p>
 *
 * @author AI学习成长助手平台
 */
@Configuration
public class RagConfig {

    /**
     * DeepSeek API密钥，从配置文件 app.deepseek.api-key 中注入。
     */
    @Value("${app.deepseek.api-key}")
    private String apiKey;

    /**
     * 创建基于内存的向量存储Bean。
     * <p>
     * 使用 {@link InMemoryEmbeddingStore}，所有向量数据存储在JVM堆内存中。
     * 优点是无外部依赖、启动快速；缺点是服务重启后数据丢失，
     * 适合开发环境或数据量较小的场景。生产环境可替换为持久化存储如 Milvus、Pinecone 等。
     * </p>
     *
     * @return 内存向量存储实例
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 创建嵌入模型Bean。
     * <p>
     * 通过 DeepSeek 的 OpenAI 兼容接口调用 text-embedding-v3 模型，
     * 将文本内容转换为高维向量表示，用于后续的语义相似度检索。
     * </p>
     *
     * @return 嵌入模型实例
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .build();
    }
}
