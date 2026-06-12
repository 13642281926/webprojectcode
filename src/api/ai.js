/**
 * AI 助手 API 接口
 *
 * 封装 AI 对话和快速提问相关的 HTTP 请求。
 *
 * 特殊配置说明：
 * - 禁用全局 Loading（showLoading: false）：AI 对话为流式交互，避免遮罩干扰用户体验
 * - 超时时间延长至 60 秒（timeout: 60000）：AI 推理响应较慢，需要更长的等待时间
 *
 * @module api/ai
 */

import request from './request'

/**
 * 发送 AI 对话消息
 * 禁用 Loading 以免干扰对话流式体验，超时设为 60 秒以适应 AI 推理耗时
 * @param {{message: string, history?: Array, context?: object}} data - 对话数据（消息内容、历史记录、上下文）
 * @returns {Promise<{code: number, data: {reply: string, suggestions?: Array}}>} AI 回复及建议
 */
export function sendAiChatApi(data) {
  return request.post('/ai/chat', data, { showLoading: false, timeout: 60000 })
}

/**
 * 获取快速提问列表（预设问题）
 * 禁用 Loading 以减少不必要的 UI 闪烁
 * @returns {Promise<{code: number, data: Array<{id: string, question: string}>}>} 快速提问列表
 */
export function getQuickQuestionsApi() {
  return request.get('/ai/quick-questions', { showLoading: false })
}
