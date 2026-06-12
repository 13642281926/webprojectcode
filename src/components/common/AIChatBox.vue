<script setup>
/**
 * @file AIChatBox.vue - AI 聊天对话组件
 * @description
 * 通用的 AI 对话 UI 组件，用于展示与 AI 助手的聊天交互。功能包括：
 *   1. 消息列表渲染：根据 role（user/assistant）区分气泡样式与方向
 *   2. 自动滚动：新消息到达时自动滚动到底部（通过 watch messages.length 触发）
 *   3. 快捷问题：预设问题标签，点击一键发送
 *   4. 文本输入：支持 Enter 发送、Shift+Enter 换行
 *   5. 加载状态：发送中显示 typing 动画，禁用输入与按钮
 *   6. 笔记保存：AI 回复可一键保存到"我的笔记"（通过 noteStore）
 *
 * 消息对象格式（由父组件传入）：
 *   { id: string|number, role: 'user'|'assistant', content: string, time: string }
 *
 * 消息流转说明：
 *   1. 用户在底部输入框输入文本，按 Enter（或点击发送按钮）
 *   2. 触发 emit('send', text) → 父组件将用户消息加入 messages 数组
 *   3. 父组件设置 loading=true，开始请求 AI 后端
 *   4. 请求完成后，父组件将 AI 回复加入 messages 数组，设置 loading=false
 *   5. 本组件 watch messages.length 检测到变化 → 自动 scrollToBottom()
 *
 * @props {Array}   messages      - 消息列表，每项 { id, role, content, time }
 * @props {Array}   quickQuestions - 预设快捷问题数组，每项为字符串
 * @props {boolean} loading       - 是否正在等待 AI 回复
 *
 * @emit {string} send  - 用户发送消息时触发，参数为输入文本
 * @emit {string} quick - 用户点击快捷问题时触发，参数为问题文本
 *
 * 注：本组件为纯展示与交互组件，不负责 AI 请求逻辑 —— 消息数据由父组件驱动。
 * 注：角色标识 "user" / "assistant" 区分消息来源，气泡方向和样式随之变化。
 */
import { ref, nextTick, watch } from 'vue'
import { MagicStick, DocumentCopy } from '@element-plus/icons-vue'
import { useNoteStore } from '@/stores/notes'
import { ElMessage } from 'element-plus'

// ============================================================
// Props 定义
// ============================================================

const props = defineProps({
  /** 消息列表数组，每项包含 id、role、content、time */
  messages: { type: Array, default: () => [] },
  /** 预设快捷问题列表 */
  quickQuestions: { type: Array, default: () => [] },
  /** 是否正在等待 AI 回复（控制 typing 动画和输入禁用） */
  loading: { type: Boolean, default: false },
})

// ============================================================
// Emits 定义
// ============================================================

const emit = defineEmits([
  /** 用户发送消息 */
  'send',
  /** 用户点击快捷问题 */
  'quick',
])

// ============================================================
// 响应式状态
// ============================================================

/** 输入框文本绑定 */
const inputText = ref('')
/** 消息列表容器 DOM 引用，用于自动滚动 */
const listRef = ref(null)
/** 笔记 Store 实例 */
const noteStore = useNoteStore()

// ============================================================
// 自动滚动逻辑
// ============================================================

/**
 * 将消息列表滚动到最底部
 * 使用 nextTick 确保 DOM 已更新（新消息已渲染到列表中）
 * 然后设置 scrollTop = scrollHeight 滚动到底部
 */
async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

/**
 * 监听消息列表长度变化 → 自动滚动到底部
 * 当父组件向 messages 数组新增消息时触发
 */
watch(
  () => props.messages.length,
  () => scrollToBottom(),
)

// ============================================================
// 用户交互处理
// ============================================================

/**
 * 发送消息
 * 1. 去除首尾空白
 * 2. 空文本或加载中状态不发送
 * 3. emit('send', text) 通知父组件处理
 * 4. 清空输入框
 */
function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
}

/**
 * 点击快捷问题标签
 * loading 状态下禁用交互
 * @param {string} q - 快捷问题文本
 */
function handleQuick(q) {
  if (props.loading) return
  emit('quick', q)
}

/**
 * 键盘事件处理
 * Enter 键（不按 Shift）→ 发送消息
 * Shift+Enter → 换行（默认行为，不拦截）
 * @param {KeyboardEvent} e
 */
function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// ============================================================
// 笔记保存
// ============================================================

/**
 * 将 AI 回复内容保存到"我的笔记"
 * 取内容前 50 个字符作为笔记标题，分类标记为 "AI问答"
 * @param {string} content - AI 回复的完整文本内容
 */
function saveAsNote(content) {
  noteStore.addNote({
    title: content.slice(0, 50),  // 前 50 字作为笔记标题
    content: content,              // 完整内容
    category: 'AI问答',           // 标记来源分类
  })
  ElMessage.success('已保存到我的笔记')
}
</script>

<template>
  <div class="ai-chat glass-card">
    <!--
      ==========================================
      消息列表区域
      role="log" + aria-live="polite" 实现无障碍通知：
        新消息到达时屏幕阅读器会朗读内容
      ==========================================
    -->
    <div
      ref="listRef"
      class="ai-chat__messages"
      role="log"
      aria-live="polite"
      aria-label="聊天消息列表"
    >
      <!--
        遍历消息列表渲染每条消息气泡
          - ai-chat__bubble--user：用户消息，右对齐，紫色背景
          - ai-chat__bubble--assistant：AI 消息，左对齐，紫色半透明背景
      -->
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="ai-chat__bubble"
        :class="`ai-chat__bubble--${msg.role}`"
      >
        <!-- 头像：用户显示"我"，AI 显示"AI" -->
        <div class="ai-chat__avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>

        <!-- 消息内容气泡 -->
        <div class="ai-chat__content">
          <p>{{ msg.content }}</p>
          <!-- 消息底部：时间戳 + 笔记保存按钮（仅 AI 回复可保存） -->
          <div class="ai-chat__footer">
            <span class="ai-chat__time">{{ msg.time }}</span>
            <el-button
              v-if="msg.role === 'assistant'"
              type="primary"
              link
              :icon="DocumentCopy"
              size="small"
              @click="saveAsNote(msg.content)"
            >
              存为笔记
            </el-button>
          </div>
        </div>
      </div>

      <!--
        loading 状态：显示 typing 动画（三个小圆点弹跳）
        role="status" 告知屏幕阅读器当前状态
      -->
      <div
        v-if="loading"
        class="ai-chat__typing"
        role="status"
        aria-label="AI 正在输入..."
      >
        <span /><span /><span />
      </div>
    </div>

    <!--
      ==========================================
      快捷问题区域（仅在 quickQuestions 非空时显示）
      ==========================================
    -->
    <div v-if="quickQuestions.length" class="ai-chat__quick">
      <!--
        每个快捷问题渲染为一个 el-tag
        loading 状态下添加 --disabled 样式，禁用点击
      -->
      <el-tag
        v-for="q in quickQuestions"
        :key="q"
        class="ai-chat__quick-tag"
        :class="{ 'ai-chat__quick-tag--disabled': loading }"
        effect="plain"
        round
        @click="handleQuick(q)"
      >
        {{ q }}
      </el-tag>
    </div>

    <!--
      ==========================================
      输入区域
      ==========================================
    -->
    <div class="ai-chat__input">
      <!--
        多行文本输入框
          - Enter 发送（由 onKeydown 拦截）
          - Shift+Enter 换行（默认行为，不拦截）
          - loading 时禁用输入
          - resize="none" 固定高度
      -->
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入学习问题，Enter 发送..."
        :disabled="loading"
        resize="none"
        @keydown="onKeydown"
      />
      <!--
        发送按钮
          - loading 状态显示加载动画 + 禁用双击
          - MagicStick 图标表示 AI 魔法
      -->
      <el-button
        type="primary"
        :icon="MagicStick"
        :loading="loading"
        :disabled="loading"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 480px;
  padding: 16px;
}

.ai-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  @include scrollbar-dark;
}

.ai-chat__bubble {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &--user {
    flex-direction: row-reverse;

    .ai-chat__content {
      background: rgba(99, 102, 241, 0.2);
      border-color: rgba(99, 102, 241, 0.3);
      border-radius: 16px 4px 16px 16px;
    }

    .ai-chat__time {
      text-align: right;
    }
  }

  &--assistant .ai-chat__content {
    background: rgba(139, 92, 246, 0.12);
    border-color: rgba(139, 92, 246, 0.2);
    border-radius: 4px 16px 16px 16px;
  }

  // 浅色模式气泡适配
  :root[data-theme='light'] &--user .ai-chat__content,
  :root:not(.dark) &--user .ai-chat__content {
    background: #eef2ff;
    border-color: #c7d2fe;
    color: #2d3748;
  }
  :root[data-theme='light'] &--assistant .ai-chat__content,
  :root:not(.dark) &--assistant .ai-chat__content {
    background: #f8fafc;
    border-color: #e2e8f0;
    color: #2d3748;
  }
}

.ai-chat__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $color-accent-gradient;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.ai-chat__content {
  max-width: 75%;
  padding: 12px 16px;
  border: 1px solid $color-border;
  border-radius: $radius-md;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  line-height: 1.6;
  font-size: 14px;
}

.ai-chat__time {
  display: block;
  font-size: 11px;
  color: var(--color-text-muted);
}

.ai-chat__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.ai-chat__typing {
  display: flex;
  gap: 4px;
  padding: 12px 16px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $color-accent;
    animation: bounce 1.2s infinite ease-in-out;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.ai-chat__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid $color-border;
}

.ai-chat__quick-tag {
  cursor: pointer;

  &--disabled {
    cursor: not-allowed;
    opacity: 0.5;
    pointer-events: none;
  }
}

.ai-chat__input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid $color-border;

  .el-textarea {
    flex: 1;
  }
}
</style>
