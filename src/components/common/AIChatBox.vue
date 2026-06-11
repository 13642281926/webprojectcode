<script setup>
import { ref, nextTick, watch } from 'vue'
import { MagicStick, DocumentCopy } from '@element-plus/icons-vue'
import { useNoteStore } from '@/stores/notes'
import { ElMessage } from 'element-plus'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  quickQuestions: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['send', 'quick'])

const inputText = ref('')
const listRef = ref(null)
const noteStore = useNoteStore()

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

watch(
  () => props.messages.length,
  () => scrollToBottom(),
)

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.loading) return
  emit('send', text)
  inputText.value = ''
}

function handleQuick(q) {
  if (props.loading) return
  emit('quick', q)
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function saveAsNote(content) {
  noteStore.addNote({
    title: content.slice(0, 50),
    content: content,
    category: 'AI问答',
  })
  ElMessage.success('已保存到我的笔记')
}
</script>

<template>
  <div class="ai-chat glass-card">
    <div ref="listRef" class="ai-chat__messages" role="log" aria-live="polite" aria-label="聊天消息列表">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="ai-chat__bubble"
        :class="`ai-chat__bubble--${msg.role}`"
      >
        <div class="ai-chat__avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="ai-chat__content">
          <p>{{ msg.content }}</p>
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
      <div v-if="loading" class="ai-chat__typing" role="status" aria-label="AI 正在输入...">
        <span /><span /><span />
      </div>
    </div>

    <div v-if="quickQuestions.length" class="ai-chat__quick">
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

    <div class="ai-chat__input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入学习问题，Enter 发送..."
        :disabled="loading"
        resize="none"
        @keydown="onKeydown"
      />
      <el-button type="primary" :icon="MagicStick" :loading="loading" :disabled="loading" @click="handleSend">发送</el-button>
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
