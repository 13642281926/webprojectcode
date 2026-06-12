<script setup>
/**
 * @file PomodoroTimer.vue - 番茄钟计时器组件
 * @description
 * 迷你番茄钟（Pomodoro Technique）计时器组件，用于辅助专注学习。功能包括：
 *   1. 25 分钟专注 + 5 分钟休息的经典番茄工作法周期
 *   2. 倒计时显示（MM:SS 格式）与环形进度条
 *   3. 播放/暂停切换（实际使用 setInterval 每秒递减）
 *   4. 计时结束时自动切换模式（专注→休息 / 休息→专注）
 *   5. 浏览器桌面通知（Notification API）或 ElMessage 兜底提示
 *   6. 折叠/展开切换（节省屏幕空间）
 *   7. 重置按钮（恢复到 25:00 专注初始状态）
 *
 * 计时器核心逻辑说明：
 *   - 使用 setInterval 每秒递减 timeLeft（单位：秒）
 *   - 当 timeLeft 递减到 0 时触发 handleTimerEnd()：
 *       1. 清除 interval 停止计时
 *       2. 发送浏览器通知或 ElMessage 提示
 *       3. 自动切换模式（isResting 取反）
 *       4. 重置 timeLeft 为对应模式的预设时长
 *   - toggleTimer() 切换运行/暂停：运行 → 创建 interval；暂停 → 清除 interval
 *   - 组件卸载时（onUnmounted）自动清除 interval，防止内存泄漏
 *
 * 桌面通知说明：
 *   - 组件初始化时请求 Notification 权限（仅 default 状态才请求，避免重复弹窗）
 *   - 权限 granted 时使用浏览器原生通知，denied 时降级为 ElMessage
 *
 * 使用场景：
 *   - 作为页面浮动工具或侧栏小部件独立使用
 *   - 不依赖任何 props —— 纯自管理计时器组件
 *
 * 注：本组件无 props/emits/slots —— 所有状态内部管理，为独立功能组件。
 */
import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { AlarmClock, VideoPlay, VideoPause, Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

// ============================================================
// 常量定义
// ============================================================

/** 专注时长（秒）：25 分钟 */
const TOTAL_WORK_TIME = 25 * 60
/** 休息时长（秒）：5 分钟 */
const TOTAL_REST_TIME = 5 * 60

// ============================================================
// 响应式状态
// ============================================================

/** 剩余时间（秒）—— 初始为 25 分钟专注时间 */
const timeLeft = ref(TOTAL_WORK_TIME)
/** 是否正在运行（计时中） */
const isRunning = ref(false)
/** 是否为休息模式（true = 休息中，false = 专注中） */
const isResting = ref(false)
/** 是否折叠（收起模式，只显示时间和切换按钮） */
const isCollapsed = ref(false)
/** setInterval 返回的定时器 ID */
let timer = null

// ============================================================
// 计算属性
// ============================================================

/**
 * 格式化剩余时间为 MM:SS 显示
 * 使用 padStart 确保两位数补零（如 "05:03"）
 */
const displayTime = computed(() => {
  const minutes = Math.floor(timeLeft.value / 60)
  const seconds = timeLeft.value % 60
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
})

/**
 * 计算环形进度条百分比（已用时间 / 总时间 * 100）
 * 总时间根据当前模式选择：专注模式用 TOTAL_WORK_TIME，休息模式用 TOTAL_REST_TIME
 */
const progress = computed(() => {
  const total = isResting.value ? TOTAL_REST_TIME : TOTAL_WORK_TIME
  return ((total - timeLeft.value) / total) * 100
})

// ============================================================
// 核心计时逻辑
// ============================================================

/**
 * 切换定时器运行/暂停状态
 *
 * 运行状态（isRunning = false → true）：
 *   创建 setInterval 每秒执行：
 *     1. timeLeft > 0 → timeLeft 减 1
 *     2. timeLeft = 0 → 调用 handleTimerEnd() 处理计时结束
 *
 * 暂停状态（isRunning = true → false）：
 *   清除 setInterval，停止倒计时
 */
function toggleTimer() {
  if (isRunning.value) {
    // 暂停：清除定时器
    clearInterval(timer)
  } else {
    // 启动：创建每秒递减的定时器
    timer = setInterval(() => {
      if (timeLeft.value > 0) {
        // 倒计时减 1 秒
        timeLeft.value--
      } else {
        // 倒计时归零 → 处理计时结束
        handleTimerEnd()
      }
    }, 1000)
  }
  // 翻转运行状态
  isRunning.value = !isRunning.value
}

/**
 * 处理计时结束事件
 *
 * 执行流程：
 *   1. 清除 interval 停止计时
 *   2. 发送通知提醒用户
 *   3. 自动切换模式（专注→休息 / 休息→专注）
 *   4. 重置 timeLeft 为新模式的预设时长
 */
function handleTimerEnd() {
  // 停止计时
  clearInterval(timer)
  isRunning.value = false

  // 准备通知内容
  const title = isResting.value ? '休息结束' : '专注结束'
  const message = isResting.value
    ? '准备好开始下一个专注周期了吗？'
    : '太棒了！休息 5 分钟吧。'

  // 发送通知：优先使用浏览器原生通知 API，降级为 ElMessage
  if (window.Notification && Notification.permission === 'granted') {
    new Notification(title, { body: message })
  } else {
    ElMessage.success(title + ': ' + message)
  }

  // 切换模式：专注 → 休息 / 休息 → 专注
  isResting.value = !isResting.value
  // 重置倒计时为对应模式的时长
  timeLeft.value = isResting.value ? TOTAL_REST_TIME : TOTAL_WORK_TIME
}

// ============================================================
// 辅助方法
// ============================================================

/**
 * 重置定时器
 * 清除定时器，恢复到初始状态：专注模式、25:00、未运行
 */
function resetTimer() {
  clearInterval(timer)
  isRunning.value = false
  isResting.value = false
  timeLeft.value = TOTAL_WORK_TIME
}

/**
 * 切换折叠/展开状态
 * 折叠模式下只显示时间和展开按钮，节省屏幕空间
 */
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

// ============================================================
// 生命周期钩子
// ============================================================

/**
 * 组件卸载时清除定时器
 * 防止页面切换后定时器仍在后台运行导致内存泄漏
 */
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// ============================================================
// 初始化：请求桌面通知权限
// ============================================================
// 仅在权限状态为 "default"（用户尚未做出选择）时主动请求
// 如果用户已拒绝或已授权，不再弹出权限请求对话框
if (window.Notification && Notification.permission === 'default') {
  Notification.requestPermission()
}
</script>

<template>
  <!--
    番茄钟容器：毛玻璃卡片
    collapsed class 切换宽度和内容可见性
  -->
  <div class="pomodoro-timer glass-card" :class="{ collapsed: isCollapsed }">
    <!-- ========== 标题栏 ========== -->
    <div class="pomodoro-timer__header">
      <el-icon><AlarmClock /></el-icon>
      <!-- 展开状态：显示 "专注时光" 或 "休息时间" -->
      <span v-if="!isCollapsed">{{ isResting ? '休息时间' : '专注时光' }}</span>
      <!-- 折叠状态：直接显示倒计时时间 -->
      <span v-else>{{ displayTime }}</span>
      <!-- 折叠/展开切换按钮 -->
      <el-button class="collapse-btn" link @click="toggleCollapse">
        <el-icon>
          <!-- 折叠状态显示向上箭头（点击展开），展开状态显示向下箭头（点击折叠） -->
          <ArrowUp v-if="isCollapsed" />
          <ArrowDown v-else />
        </el-icon>
      </el-button>
    </div>

    <!-- ========== 倒计时显示区域（展开状态下可见） ========== -->
    <div v-if="!isCollapsed" class="pomodoro-timer__display">
      <!--
        环形进度条（Element Plus el-progress type="circle"）
          - percentage：计算属性 progress，表示已用时间百分比
          - color：专注模式蓝色(#3b82f6)，休息模式绿色(#22c55e)
          - 中央插槽显示格式化时间（MM:SS）
      -->
      <el-progress
        type="circle"
        :percentage="progress"
        :stroke-width="8"
        :color="isResting ? '#22c55e' : '#3b82f6'"
        :width="120"
      >
        <div class="time-text">{{ displayTime }}</div>
      </el-progress>
    </div>

    <!-- ========== 操作按钮区域（展开状态下可见） ========== -->
    <div v-if="!isCollapsed" class="pomodoro-timer__actions">
      <!--
        播放/暂停按钮
          - 运行中：warning 类型（橙色警告色，提示可暂停），显示暂停图标
          - 未运行：primary 类型（蓝色），显示播放图标
      -->
      <el-button
        circle
        size="large"
        :type="isRunning ? 'warning' : 'primary'"
        @click="toggleTimer"
      >
        <el-icon :size="20">
          <VideoPause v-if="isRunning" />
          <VideoPlay v-else />
        </el-icon>
      </el-button>

      <!-- 重置按钮：恢复到 25:00 专注初始状态 -->
      <el-button circle size="large" @click="resetTimer">
        <el-icon :size="20"><Refresh /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<template>
  <div class="pomodoro-timer glass-card" :class="{ collapsed: isCollapsed }">
    <div class="pomodoro-timer__header">
      <el-icon><AlarmClock /></el-icon>
      <span v-if="!isCollapsed">{{ isResting ? '休息时间' : '专注时光' }}</span>
      <span v-else>{{ displayTime }}</span>
      <el-button class="collapse-btn" link @click="toggleCollapse">
        <el-icon><ArrowUp v-if="isCollapsed" /><ArrowDown v-else /></el-icon>
      </el-button>
    </div>
    
    <div v-if="!isCollapsed" class="pomodoro-timer__display">
      <el-progress 
        type="circle" 
        :percentage="progress" 
        :stroke-width="8"
        :color="isResting ? '#22c55e' : '#3b82f6'"
        :width="120"
      >
        <div class="time-text">{{ displayTime }}</div>
      </el-progress>
    </div>

    <div v-if="!isCollapsed" class="pomodoro-timer__actions">
      <el-button 
        circle 
        size="large" 
        :type="isRunning ? 'warning' : 'primary'"
        @click="toggleTimer"
      >
        <el-icon :size="20">
          <VideoPause v-if="isRunning" />
          <VideoPlay v-else />
        </el-icon>
      </el-button>
      
      <el-button circle size="large" @click="resetTimer">
        <el-icon :size="20"><Refresh /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.pomodoro-timer {
  padding: 16px;
  width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    transform: translateY(-4px);
  }
  
  &.collapsed {
    width: auto;
    padding: 8px 12px;
  }
}

.pomodoro-timer__header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  width: 100%;
  justify-content: space-between;
}

.collapse-btn {
  padding: 0;
  margin-left: auto;
  
  &:hover {
    background: transparent;
  }
}

.pomodoro-timer__display {
  margin: 8px 0;
}

.time-text {
  font-size: 20px;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
}

.pomodoro-timer__actions {
  display: flex;
  gap: 12px;
}
</style>
