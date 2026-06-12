<script setup>
/**
 * AiAssistantView - AI 学习助手
 *
 * 核心功能：
 * - 聊天界面，通过 AIChatBox 组件渲染对话
 * - 欢迎消息 + 快捷问题入口
 * - 发送消息调用后端 Spring Boot AI API，流式返回回答
 * - 最小 loading 延迟 300ms，避免动画一闪而过
 * - 快捷问题从后端拉取，失败时回退到硬编码默认值
 */
import { onMounted, ref } from 'vue'
import AIChatBox from '@/components/common/AIChatBox.vue'
import { sendAiChatApi, getQuickQuestionsApi } from '@/api/ai'

/** 聊天消息列表，第一条为欢迎语 */
const messages = ref([
  {
    id: 'welcome',
    role: 'assistant',
    content: '你好！我是 AI 学习助手，可以为你提供考研、英语、Vue 项目、时间管理等学习建议。试试下面的快捷问题吧～',
    time: formatTime(new Date()),
  },
])
/** 快捷问题列表 */
const quickQuestions = ref([])
/** AI 回答加载状态 */
const loading = ref(false)
/** 是否开发环境，用于显示 API 标签 */
const isDev = import.meta.env.DEV

/** 格式化时间为 HH:MM */
function formatTime(date) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

/**
 * 添加一条消息到列表
 * @param {string} role - 'user' | 'assistant'
 * @param {string} content - 消息文本
 */
function pushMessage(role, content) {
  messages.value.push({
    id: `${Date.now()}_${Math.random()}`,
    role,
    content,
    time: formatTime(new Date()),
  })
}

/**
 * 发送消息
 * 1. 防重复发送检查
 * 2. 先 push 用户消息
 * 3. 调用后端 API，设置最小 300ms 延迟避免 loading 闪烁
 * 4. push AI 回复，失败时显示错误提示
 */
async function handleSend(text) {
  if (loading.value) return
  pushMessage('user', text)
  loading.value = true

  // 保证 loading 动画至少展示 300ms，避免一闪而过
  const minDelay = new Promise((resolve) => setTimeout(resolve, 300))

  try {
    const res = await sendAiChatApi({ question: text })
    await minDelay
    pushMessage('assistant', res.data.content)
  } catch (err) {
    console.error('[AI] 请求失败:', err.message || err)
    await minDelay
    pushMessage('assistant', '请求失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

/** 点击快捷问题，直接发送 */
function handleQuick(q) {
  handleSend(q)
}

/** 页面挂载时拉取快捷问题列表，失败时使用硬编码兜底 */
onMounted(async () => {
  try {
    const res = await getQuickQuestionsApi()
    quickQuestions.value = res.data
  } catch {
    quickQuestions.value = ['如何准备考研？', '怎样提高英语学习效率？']
  }
})
</script>

<template>
  <div class="page-container ai-page">
    <!-- AI 聊天卡片 -->
    <el-card class="glass-card ai-page__card" shadow="never">
      <template #header>
        <div class="ai-page__header">
          <span class="gradient-text">AI 学习助手</span>
          <!-- 开发环境显示后端 API 标签 -->
          <el-tag v-if="isDev" effect="plain" type="success" size="small">Spring Boot API</el-tag>
        </div>
      </template>
      <!-- 聊天组件：消息列表、快捷问题、loading 状态、发送/快捷事件 -->
      <AIChatBox
        :messages="messages"
        :quick-questions="quickQuestions"
        :loading="loading"
        @send="handleSend"
        @quick="handleQuick"
      />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.ai-page__card {
  :deep(.el-card__body) {
    padding: 0;
  }
}

.ai-page__header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
