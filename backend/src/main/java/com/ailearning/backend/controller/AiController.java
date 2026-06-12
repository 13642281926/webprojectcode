package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 智能助手控制器，提供 AI 聊天对话和快捷问题推荐接口。
 * <p>
 * 该控制器是平台 AI 功能的入口，集成了大语言模型（LLM）能力，
 * 为用户提供智能问答、学习辅导、知识答疑等服务。
 * 聊天接口接收用户输入的自然语言问题，调用 AI 服务生成回答并返回。
 * 快捷问题接口提供预设的高频问题列表，降低用户输入门槛，提升交互效率。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    /**
     * 构造函数，通过 Spring IoC 注入 AI 服务。
     *
     * @param aiService AI 对话与问答业务服务
     */
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI 聊天对话接口。
     * 接收用户输入的文本问题，调用后端的 AI 大模型服务进行推理生成回答，
     * 返回 AI 生成的回复内容。适用于开放式的学习咨询、知识点解答等场景。
     * 如果请求体为空或 question 字段缺失，将传入 null 给服务层处理。
     *
     * @param body 请求体，包含 question 字段（用户输入的自然语言问题）
     * @return AI 生成的回答结果，包含回复文本和可能的上下文信息
     */
    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        // 安全提取 question 字段，body 为 null 时返回 null
        String question = body == null ? null : String.valueOf(body.get("question"));
        return ApiResponse.success(aiService.chat(question));
    }

    /**
     * 获取预设的 AI 快捷问题列表。
     * 返回一组高频推荐问题，展示在 AI 对话界面的快捷入口区域，
     * 用户点击即可快速发起对话，无需手动输入，降低使用门槛。
     *
     * @return 预设问题文本的列表
     */
    @GetMapping("/quick-questions")
    public ApiResponse<List<String>> quickQuestions() {
        return ApiResponse.success(aiService.quickQuestions());
    }
}
