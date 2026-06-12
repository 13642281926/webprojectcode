package com.ailearning.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话服务 —— 负责与 DeepSeek 大语言模型的 API 交互。
 *
 * <p>调用流程：
 * <ol>
 *   <li><b>用户上下文获取</b>：从 {@link com.ailearning.backend.common.AuthContext} 获取当前登录用户 ID。</li>
 *   <li><b>RAG 增强</b>：如果用户已登录，调用 {@link RagService#buildRagPrompt} 将用户上传的资料内容
 *       作为参考上下文注入提示词，实现"基于个人知识库的 AI 问答"。</li>
 *   <li><b>API 请求构建</b>：组装多轮对话消息（system + user），启用 DeepSeek 的 thinking/reasoning 模式，
 *       通过 WebClient 以非流式（stream=false）方式调用 /chat/completions 端点。</li>
 *   <li><b>响应解析</b>：从 choices[0].message.content 提取 AI 回复文本，封装为统一的消息格式返回。</li>
 *   <li><b>异常容错</b>：API 调用失败或超时时返回友好的错误提示消息，而非抛出 5xx。</li>
 * </ol>
 *
 * <p>技术栈：Spring WebClient (Reactor Netty) + DeepSeek API (兼容 OpenAI 格式)。
 * 超时配置：读写超时各 60 秒。
 */
@Service
public class AiService {
    /** WebClient 实例，用于调用 DeepSeek API */
    private final WebClient webClient;
    /** RAG 服务，用于构建增强提示词 */
    private final RagService ragService;

    /** DeepSeek API 密钥 */
    @Value("${app.deepseek.api-key}")
    private String apiKey;

    /** 模型名称（如 deepseek-chat / deepseek-reasoner） */
    @Value("${app.deepseek.model}")
    private String model;

    /** 系统提示词，定义 AI 助手的角色和行为 */
    @Value("${app.deepseek.system-prompt}")
    private String systemPrompt;

    /**
     * 构造 AI 服务，初始化 WebClient 与超时配置。
     *
     * @param baseUrl   DeepSeek API 基础地址
     * @param ragService RAG 知识库服务
     */
    public AiService(@Value("${app.deepseek.base-url}") String baseUrl, RagService ragService) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
                .doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(60))
                        .addHandlerLast(new WriteTimeoutHandler(60)));
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.ragService = ragService;
    }

    /**
     * 与 AI 助手进行单轮对话。
     *
     * <p>如果用户已登录，会自动调用 RAG 服务将用户知识库中的相关内容作为上下文注入提示词，
     * 提升回答的个性化和准确性。
     *
     * <p>请求配置：
     * <ul>
     *   <li>启用 thinking 推理模式（thinking.type=enabled）</li>
     *   <li>推理力度设为 high（reasoning_effort=high）</li>
     *   <li>非流式响应（stream=false）</li>
     * </ul>
     *
     * @param question 用户问题文本
     * @return 包含 id（消息ID）、role（assistant）、content（AI回复）、createdAt（时间）的 Map
     */
    public Map<String, Object> chat(String question) {
        try {
            // 获取当前登录用户上下文
            Long userId = com.ailearning.backend.common.AuthContext.getCurrentUserId();

            // 如果用户已登录，通过 RAG 增强问题上下文
            String enhancedPrompt = question;
            if (userId != null) {
                enhancedPrompt = ragService.buildRagPrompt(userId, question);
            }

            // 构建 DeepSeek API 请求体（兼容 OpenAI 格式）
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt + "\n\n如果用户问题有相关参考资料，请优先基于参考资料回答。"),
                            Map.of("role", "user", "content", enhancedPrompt)
                    ),
                    "thinking", Map.of("type", "enabled"),
                    "reasoning_effort", "high",
                    "stream", false
            );

            // 发送 POST 请求到 DeepSeek API
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();  // 同步阻塞等待响应

            // 解析 OpenAI 兼容的响应格式：choices[0].message.content
            String content;
            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    content = (String) message.get("content");
                } else {
                    content = "抱歉，我暂时无法回答你的问题，请稍后重试。";
                }
            } else {
                content = "抱歉，我暂时无法回答你的问题，请稍后重试。";
            }

            // 封装为统一的消息格式
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", "msg_" + System.currentTimeMillis());
            data.put("role", "assistant");
            data.put("content", content);
            data.put("createdAt", LocalDateTime.now());
            return data;

        } catch (WebClientResponseException e) {
            // API 返回了错误状态码（如 401 密钥无效、429 限流）
            System.err.println("DeepSeek API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", "msg_" + System.currentTimeMillis());
            data.put("role", "assistant");
            data.put("content", "抱歉，调用AI服务失败，请检查API密钥配置或稍后重试。");
            data.put("createdAt", LocalDateTime.now());
            return data;
        } catch (Exception e) {
            // 其他未预期的异常（网络超时、连接失败等）
            System.err.println("Error calling DeepSeek: " + e.getMessage());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", "msg_" + System.currentTimeMillis());
            data.put("role", "assistant");
            data.put("content", "抱歉，系统出现错误，请稍后重试。");
            data.put("createdAt", LocalDateTime.now());
            return data;
        }
    }

    /**
     * 获取推荐快捷问题列表，供前端快捷提问按钮使用。
     *
     * @return 5 个预设的快捷问题
     */
    public List<String> quickQuestions() {
        return List.of(
                "如何准备考研，",
                "怎样提高英语学习效率，",
                "Vue3 项目学习路线是什么？",
                "如何做时间管理？",
                "算法刷题有什么建议？"
        );
    }
}
