<script setup>
/**
 * PomodoroView - 番茄专注
 *
 * 核心功能：
 * - 计时核心逻辑由 stores/pomodoro 驱动，本页面负责 UI 交互
 * - 任务绑定：可选关联课程/学习计划或自由学习
 * - 计时控制：开始/暂停/重置/跳过/中断，支持专注/短休/长休三阶段
 * - 阶段手动切换（需先暂停）或自动流转（store 内部逻辑）
 * - 设置面板：自定义各阶段时长、长休轮数、自动开始
 * - 今日记录时间线：查看完成/中断日志，一键打开复盘弹窗
 * - 复盘功能：记录本轮完成、难点、下轮计划、专注感受（1-5星）
 *   - 支持沉淀到笔记（自动创建 Markdown 格式笔记）
 *   - 支持难点加入错题本（自动创建错题记录）
 * - 统计分析：今日完成数/分钟/连续打卡/完成率统计卡片
 *   - ECharts 图表：近7天趋势、专注高峰时段、课程投入分布
 *   - 质量分析面板：完成率/中断率/平均感受/累计分钟
 * - 计时器进度环使用环形 el-progress，颜色随阶段变化
 */
import { computed, inject, onMounted, ref } from 'vue'
import {
  AlarmClock,
  VideoPlay,
  VideoPause,
  Refresh,
  ArrowRight,
  Document,
  Warning,
  MagicStick,
  Star,
  StarFilled,
  DataLine,
  Trophy,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/common/StatCard.vue'
import ChartCard from '@/components/common/ChartCard.vue'
import { usePomodoroStore, PHASE } from '@/stores/pomodoro'
import { useNoteStore } from '@/stores/notes'
import { useWrongQuestionStore } from '@/stores/wrongQuestion'
import { getCourseListApi } from '@/api/course'
import { getStudyPlanListApi } from '@/api/studyPlan'
import { CHART_COLORS, getAxisStyle, getChartTheme } from '@/utils/echarts'

const pomodoroStore = usePomodoroStore()
const noteStore = useNoteStore()
const wrongStore = useWrongQuestionStore()
/** 主题配置注入，用于图表暗色模式适配 */
const themeConfig = inject('themeConfig', { isDark: true })
const isDark = computed(() => themeConfig.value?.isDark ?? themeConfig.isDark)

/** 可关联的课程列表（从后端拉取） */
const courseOptions = ref([])
/** 可关联的学习计划列表（从后端拉取） */
const planOptions = ref([])

/** 页面挂载时并行拉取课程和计划列表，失败不影响主功能 */
onMounted(async () => {
  try {
    const [courseRes, planRes] = await Promise.all([
      getCourseListApi({ page: 1, pageSize: 100 }),
      getStudyPlanListApi({ page: 1, pageSize: 100 }),
    ])
    courseOptions.value = courseRes.data?.list || courseRes.data || []
    planOptions.value = planRes.data?.list || planRes.data || []
  } catch {
    // 后端未就绪时不影响主功能
  }
})

// ==================== 任务绑定 ====================

/** 任务类型：free（自由）/ course（课程）/ plan（计划） */
const taskType = ref('free')
/** 选中的课程 ID */
const selectedCourseId = ref('')
/** 选中的计划 ID */
const selectedPlanId = ref('')
/** 自由学习时的自定义标题 */
const freeTitle = ref('')

/**
 * 应用任务绑定
 * 根据 taskType 查找对应的课程/计划名称，调用 store.setTask 写入当前任务
 */
function applyTask() {
  if (taskType.value === 'course' && selectedCourseId.value) {
    const c = courseOptions.value.find((x) => x.id === selectedCourseId.value)
    pomodoroStore.setTask({ type: 'course', id: c?.id, title: c?.name || c?.title })
  } else if (taskType.value === 'plan' && selectedPlanId.value) {
    const p = planOptions.value.find((x) => x.id === selectedPlanId.value)
    pomodoroStore.setTask({ type: 'plan', id: p?.id, title: p?.title || p?.name })
  } else {
    pomodoroStore.setTask({ type: 'free', id: '', title: freeTitle.value || '自由学习' })
  }
}

/** 当前任务标题，用于界面显示 */
const currentTaskTitle = computed(() => pomodoroStore.currentTask.title || '自由学习')

// ==================== 控制按钮 ====================

/** 开始计时：先应用任务绑定，再调用 store.start() */
function handleStart() {
  applyTask()
  pomodoroStore.start()
}
/** 暂停计时 */
function handlePause() {
  pomodoroStore.pause()
}

// ==================== 阶段切换 ====================

/** 阶段 computed（只读 get，禁止 set — 阶段切换必须通过 manualSwitch 手动触发） */
const phaseTab = computed({
  get() {
    return pomodoroStore.phase
  },
  set() {
    // 阶段切换通过 pause + switchPhase 触发，禁止直接 set
  },
})

/**
 * 手动切换阶段
 * 1. 检查是否正在运行（需先暂停）
 * 2. 如果切回专注阶段则重置完成轮数
 * 3. 更新阶段并设置对应时长
 */
function manualSwitch(target) {
  if (pomodoroStore.isRunning) {
    ElMessage.warning('请先暂停计时再切换阶段')
    return
  }
  if (pomodoroStore.phase === target) return
  // 切回专注阶段时重置轮数计数器
  if (target === PHASE.FOCUS) {
    pomodoroStore.completedRounds = 0
  }
  pomodoroStore.phase = target
  pomodoroStore.timeLeft = pomodoroStore.getPhaseDuration(target)
}

/**
 * 中断当前专注轮
 * 仅在专注阶段可用，弹出对话框记录中断原因
 * 中断记录会保存到 store.logs 中，供复盘分析
 */
async function handleInterrupt() {
  if (pomodoroStore.phase !== PHASE.FOCUS) {
    ElMessage.info('仅在专注阶段可以标记中断')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      '简单说明中断原因，便于后续优化专注节奏',
      '结束本轮专注',
      {
        confirmButtonText: '保存并结束',
        cancelButtonText: '继续专注',
        inputPlaceholder: '例如：被打扰 / 任务太难 / 临时有事',
        inputValue: '',
      },
    )
    pomodoroStore.interrupt(value || '未说明')
    ElMessage.success('已记录为中断')
  } catch {
    /* 用户取消 */
  }
}

// ==================== 设置面板 ====================

const settingsOpen = ref(false)
/** 设置草稿，通过浅拷贝隔离，取消时不污染 store */
const settingsDraft = ref({ ...pomodoroStore.settings })

/** 打开设置面板，从 store 拷贝当前设置到草稿 */
function openSettings() {
  settingsDraft.value = { ...pomodoroStore.settings }
  settingsOpen.value = true
}

/**
 * 保存设置
 * 1. 对各值做数值校验（Math.max 确保最小值）
 * 2. 写入 store.settings
 * 3. 如果未在计时中，刷新当前倒计时
 */
function saveSettings() {
  pomodoroStore.settings = {
    focusDuration: Math.max(1, Number(settingsDraft.value.focusDuration) || 25),
    shortBreakDuration: Math.max(1, Number(settingsDraft.value.shortBreakDuration) || 5),
    longBreakDuration: Math.max(1, Number(settingsDraft.value.longBreakDuration) || 15),
    roundsForLongBreak: Math.max(1, Number(settingsDraft.value.roundsForLongBreak) || 4),
    autoStartNext: !!settingsDraft.value.autoStartNext,
  }
  if (!pomodoroStore.isRunning) {
    pomodoroStore.timeLeft = pomodoroStore.getPhaseDuration(pomodoroStore.phase)
  }
  settingsOpen.value = false
  ElMessage.success('设置已保存')
}

// ==================== 复盘弹窗 ====================

/** 当前复盘的日志 ID */
const reflectionLogId = ref('')
const reflectionOpen = ref(false)
/** 复盘表单数据：本轮完成、遇到难点、下轮计划、专注感受(1-5)、沉淀选项 */
const reflection = ref({ what: '', blocker: '', next: '', mood: 3, saveAsNote: true, addAsWrong: false })

/**
 * 打开复盘弹窗
 * 回填已有复盘数据（如果之前写过），saveAsNote 根据是否已有 noteId 判断
 */
function openReflection(log) {
  reflectionLogId.value = log.id
  reflection.value = {
    what: log.reflection?.what || '',
    blocker: log.reflection?.blocker || '',
    next: log.reflection?.next || '',
    mood: log.reflection?.mood || 3,
    saveAsNote: !log.reflection?.noteId,
    addAsWrong: false,
  }
  reflectionOpen.value = true
}

/**
 * 保存复盘并沉淀
 * 1. 将复盘数据写入 store 对应日志记录
 * 2. 如果勾选"保存为笔记"，自动创建 Markdown 格式的复盘笔记
 *    - 包含本轮完成、遇到难点、下轮计划、专注感受（星级）
 * 3. 如果勾选"难点加入错题本"，自动创建错题记录
 *    - 标题格式：番茄复盘难点 · {任务名}
 */
async function saveReflection() {
  const log = pomodoroStore.logs.find((l) => l.id === reflectionLogId.value)
  if (!log) return
  const reflectionData = { ...reflection.value, createdAt: Date.now() }
  pomodoroStore.saveReflection(reflectionLogId.value, reflectionData)

  // 步骤 1：沉淀到笔记系统
  if (reflection.value.saveAsNote && !log.reflection?.noteId) {
    try {
      const created = await noteStore.addNote({
        title: `番茄复盘 · ${log.taskTitle || '自由学习'} · ${new Date(log.startAt).toLocaleDateString('zh-CN')}`,
        content: [
          `## 本轮完成`,
          reflection.value.what || '（未填写）',
          '',
          `## 遇到难点`,
          reflection.value.blocker || '（未填写）',
          '',
          `## 下轮计划`,
          reflection.value.next || '（未填写）',
          '',
          `## 专注感受`,
          '⭐'.repeat(reflection.value.mood) + '☆'.repeat(5 - reflection.value.mood),
        ].join('\n'),
        category: 'study',
        tags: ['番茄专注', log.taskTitle].filter(Boolean),
      })
      if (created?.id) {
        log.reflection = { ...log.reflection, noteId: created.id }
      }
    } catch (err) {
      console.warn('[pomodoro] 同步笔记失败:', err)
    }
  }

  // 步骤 2：沉淀到错题本
  if (reflection.value.addAsWrong && reflection.value.blocker && !log.reflection?.wrongId) {
    try {
      const created = await wrongStore.addWrongQuestion({
        title: `番茄复盘难点 · ${log.taskTitle || '自由学习'}`,
        content: reflection.value.blocker,
        category: log.taskTitle || '番茄专注',
        difficulty: 'medium',
        tags: ['番茄专注'],
      })
      if (created?.id) {
        log.reflection = { ...log.reflection, wrongId: created.id }
      }
    } catch (err) {
      console.warn('[pomodoro] 同步错题失败:', err)
    }
  }
  reflectionOpen.value = false
  ElMessage.success('复盘已保存')
}

// ==================== 阶段颜色与标签映射 ====================

/** 阶段中文标签 */
const phaseLabelMap = {
  [PHASE.FOCUS]: '专注时光',
  [PHASE.SHORT_BREAK]: '短休',
  [PHASE.LONG_BREAK]: '长休',
}
/** 阶段对应颜色，用于进度环 */
const phaseColorMap = {
  [PHASE.FOCUS]: '#3b82f6',
  [PHASE.SHORT_BREAK]: '#22c55e',
  [PHASE.LONG_BREAK]: '#8b5cf6',
}

// ==================== ECharts 图表配置 ====================

/**
 * 图表：近 7 天趋势
 * 柱状图展示每日完成番茄数（bar），折线图展示每日专注分钟（line）
 * 双 Y 轴：左侧为番茄数，右侧为分钟
 */
const weeklyOption = computed(() => {
  const trend = pomodoroStore.weeklyTrend
  return {
    ...getChartTheme(isDark.value),
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 32, bottom: 32 },
    legend: { data: ['完成数', '专注分钟'], textStyle: { color: isDark.value ? '#94a3b8' : '#718096' } },
    xAxis: {
      type: 'category',
      data: trend.map((d) => d.label),
      ...getAxisStyle(isDark.value),
    },
    yAxis: [
      { type: 'value', name: '番茄数', ...getAxisStyle(isDark.value) },
      { type: 'value', name: '分钟', ...getAxisStyle(isDark.value) },
    ],
    series: [
      {
        name: '完成数',
        type: 'bar',
        data: trend.map((d) => d.count),
        itemStyle: { color: CHART_COLORS[0], borderRadius: [4, 4, 0, 0] },
        barWidth: '40%',
      },
      {
        name: '专注分钟',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: trend.map((d) => d.minutes),
        lineStyle: { color: CHART_COLORS[1], width: 2 },
        itemStyle: { color: CHART_COLORS[1] },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(139,92,246,0.35)' },
              { offset: 1, color: 'rgba(139,92,246,0)' },
            ],
          },
        },
      },
    ],
  }
})

/**
 * 图表：课程投入分布（环形饼图）
 * 展示各课程/任务的总专注分钟占比
 */
const courseOption = computed(() => {
  const data = pomodoroStore.courseDistribution
  if (!data.length) return {}
  return {
    ...getChartTheme(isDark.value),
    tooltip: { trigger: 'item', formatter: '{b}: {c} 分钟 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: isDark.value ? '#94a3b8' : '#718096' } },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        data: data.map((d, i) => ({ name: d.name, value: d.minutes, itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length] } })),
        label: { color: '#cbd5e1' },
        itemStyle: { borderRadius: 6, borderColor: '#0a0e1a', borderWidth: 2 },
      },
    ],
  }
})

/**
 * 图表：专注高峰时段
 * 按小时统计完成的番茄数，帮助用户找到最佳专注时段
 */
const peakOption = computed(() => {
  const data = pomodoroStore.focusPeak
  return {
    ...getChartTheme(isDark.value),
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 32, bottom: 32 },
    xAxis: {
      type: 'category',
      data: data.map((d) => `${d.hour}时`),
      ...getAxisStyle(isDark.value),
    },
    yAxis: { type: 'value', ...getAxisStyle(isDark.value) },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.count),
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: CHART_COLORS[2] },
              { offset: 1, color: CHART_COLORS[0] },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
        barWidth: '50%',
      },
    ],
  }
})

// ==================== 工具函数 ====================

/** 将时间戳格式化为 HH:MM */
function formatTime(ts) {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/** 将秒数格式化为可读时长（X分X秒） */
function formatDuration(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return m > 0 ? `${m}分${s ? s + '秒' : ''}` : `${s}秒`
}
</script>

<template>
  <div class="pomodoro-page">
    <!-- 顶部信息：标题 + 设置入口 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">
          <el-icon><AlarmClock /></el-icon>
          番茄专注
        </h2>
        <p class="page-subtitle">专注当下，复盘沉淀，让每一次投入都被看见</p>
      </div>
      <el-button type="primary" plain @click="openSettings">
        <el-icon><MagicStick /></el-icon>
        计时设置
      </el-button>
    </div>

    <!-- 统计卡片行：今日完成 / 今日专注 / 连续打卡 / 完成率 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="6">
        <StatCard label="今日完成" :value="pomodoroStore.todayCount" unit="个番茄" :icon="AlarmClock" color="#3b82f6" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <StatCard label="今日专注" :value="pomodoroStore.todayMinutes" unit="分钟" :icon="DataLine" color="#22c55e" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <StatCard label="连续打卡" :value="pomodoroStore.streakDays" unit="天" :icon="Trophy" color="#f59e0b" />
      </el-col>
      <el-col :xs="12" :sm="6">
        <StatCard label="完成率" :value="pomodoroStore.todayCompletionRate" unit="%" :icon="StarFilled" color="#8b5cf6" />
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧：计时器区域 + 任务绑定面板 -->
      <el-col :xs="24" :lg="14">
        <el-card class="glass-card timer-card" shadow="never">
          <!-- 阶段标签 + 当前任务 -->
          <div class="timer-card__head">
            <el-tag :type="pomodoroStore.isFocusing ? 'primary' : 'success'" effect="dark" size="large">
              <el-icon><AlarmClock /></el-icon>
              {{ pomodoroStore.phaseLabel }}
            </el-tag>
            <span class="timer-card__task">当前任务：<b>{{ currentTaskTitle }}</b></span>
          </div>

          <!-- 环形进度计时器 -->
          <div class="timer-card__display" :style="{ '--ring': phaseColorMap[pomodoroStore.phase] }">
            <el-progress
              type="circle"
              :percentage="pomodoroStore.progress"
              :stroke-width="10"
              :width="260"
              :color="phaseColorMap[pomodoroStore.phase]"
            >
              <div class="timer-card__time">{{ pomodoroStore.displayTime }}</div>
              <div class="timer-card__phase">{{ phaseLabelMap[pomodoroStore.phase] }}</div>
            </el-progress>
          </div>

          <!-- 操作按钮区 -->
          <div class="timer-card__actions">
            <el-button
              v-if="!pomodoroStore.isRunning"
              type="primary"
              size="large"
              :icon="VideoPlay"
              @click="handleStart"
            >
              开始专注
            </el-button>
            <el-button v-else type="warning" size="large" :icon="VideoPause" @click="handlePause">
              暂停
            </el-button>
            <el-button size="large" :icon="Refresh" @click="pomodoroStore.reset()">重置</el-button>
            <el-button size="large" :icon="ArrowRight" plain @click="pomodoroStore.skip()">跳过</el-button>
            <!-- 仅在专注阶段可中断 -->
            <el-button size="large" type="danger" plain @click="handleInterrupt">结束本轮</el-button>
          </div>

          <el-divider />

          <!-- 任务绑定面板：选择自由学习 / 关联课程 / 关联计划 -->
          <div class="timer-card__task-panel">
            <h4>本轮学习什么</h4>
            <!-- 计时中禁止切换任务类型 -->
            <el-radio-group v-model="taskType" :disabled="pomodoroStore.isRunning">
              <el-radio-button value="free">自由学习</el-radio-button>
              <el-radio-button value="course">关联课程</el-radio-button>
              <el-radio-button value="plan">关联计划</el-radio-button>
            </el-radio-group>
            <div class="timer-card__task-form">
              <el-select
                v-if="taskType === 'course'"
                v-model="selectedCourseId"
                placeholder="选择课程"
                filterable
                :disabled="pomodoroStore.isRunning"
                style="width: 100%"
              >
                <el-option
                  v-for="c in courseOptions"
                  :key="c.id"
                  :label="c.name || c.title"
                  :value="c.id"
                />
              </el-select>
              <el-select
                v-else-if="taskType === 'plan'"
                v-model="selectedPlanId"
                placeholder="选择学习计划"
                filterable
                :disabled="pomodoroStore.isRunning"
                style="width: 100%"
              >
                <el-option
                  v-for="p in planOptions"
                  :key="p.id"
                  :label="p.title || p.name"
                  :value="p.id"
                />
              </el-select>
              <el-input
                v-else
                v-model="freeTitle"
                placeholder="本轮准备做什么（选填）"
                :disabled="pomodoroStore.isRunning"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：阶段切换 + 今日记录时间线 -->
      <el-col :xs="24" :lg="10">
        <!-- 阶段切换卡片 -->
        <el-card class="glass-card" shadow="never">
          <template #header>
            <div class="section-header">
              <h3>阶段切换</h3>
            </div>
          </template>
          <!-- segmented 分段控件：专注 / 短休 / 长休 -->
          <el-segmented
            :model-value="pomodoroStore.phase"
            :options="[
              { label: '专注', value: 'focus' },
              { label: '短休', value: 'shortBreak' },
              { label: '长休', value: 'longBreak' },
            ]"
            :disabled="pomodoroStore.isRunning"
            @change="manualSwitch"
            block
          />
          <p class="phase-hint">每完成 {{ pomodoroStore.settings.roundsForLongBreak }} 个专注，自动进入长休</p>
        </el-card>

        <!-- 今日记录时间线 -->
        <el-card class="glass-card log-card" shadow="never">
          <template #header>
            <div class="section-header">
              <h3>今日记录</h3>
              <el-tag size="small">{{ pomodoroStore.todayLogs.length }} 条</el-tag>
            </div>
          </template>
          <el-empty v-if="!pomodoroStore.todayLogs.length" description="今天还没有番茄记录，开始第一个吧" :image-size="80" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="log in pomodoroStore.todayLogs"
              :key="log.id"
              :type="log.completed ? (log.taskType === 'course' ? 'primary' : 'success') : 'warning'"
              :timestamp="formatTime(log.startAt)"
              placement="top"
            >
              <div class="log-item">
                <div class="log-item__head">
                  <span class="log-item__title">
                    {{ log.taskTitle || '自由学习' }}
                    <!-- 中断/完成状态标签 -->
                    <el-tag v-if="!log.completed" size="small" type="danger">中断</el-tag>
                    <el-tag v-else size="small" type="success">完成</el-tag>
                  </span>
                  <span class="log-item__duration">{{ formatDuration(log.durationSec) }}</span>
                </div>
                <!-- 关联标记：专注感受 / 已沉淀笔记 / 已加入错题 -->
                <div class="log-item__meta">
                  <span v-if="log.reflection?.mood" class="log-item__mood">
                    <el-icon><StarFilled /></el-icon>
                    {{ log.reflection.mood }}/5
                  </span>
                  <span v-if="log.reflection?.noteId" class="log-item__tag">
                    <el-icon><Document /></el-icon>
                    已沉淀笔记
                  </span>
                  <span v-if="log.reflection?.wrongId" class="log-item__tag">
                    <el-icon><Warning /></el-icon>
                    已加入错题
                  </span>
                </div>
                <!-- 复盘入口：已复盘显示"查看复盘"，未复盘显示"写复盘" -->
                <el-button link type="primary" size="small" @click="openReflection(log)">
                  {{ log.reflection ? '查看复盘' : '写复盘' }}
                </el-button>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计分析区域 -->
    <el-card class="glass-card analysis-card" shadow="never">
      <template #header>
        <div class="section-header">
          <h3>专注分析</h3>
          <span class="muted">基于本地累计数据</span>
        </div>
      </template>
      <el-row :gutter="20">
        <!-- 近7天趋势图 -->
        <el-col :xs="24" :md="12">
          <ChartCard title="近 7 天趋势" :option="weeklyOption" height="280px" />
        </el-col>
        <!-- 专注高峰时段图 -->
        <el-col :xs="24" :md="12">
          <ChartCard title="专注高峰时段" :option="peakOption" height="280px" />
        </el-col>
        <!-- 课程投入分布饼图 -->
        <el-col :xs="24" :md="12">
          <ChartCard title="课程投入分布（分钟）" :option="courseOption" height="280px" />
        </el-col>
        <!-- 质量分析面板 -->
        <el-col :xs="24" :md="12">
          <el-card class="glass-card quality-card" shadow="never">
            <h4>质量分析</h4>
            <ul>
              <li>
                <span>今日完成率</span>
                <b>{{ pomodoroStore.todayCompletionRate }}%</b>
              </li>
              <li>
                <span>今日中断率</span>
                <b>{{ pomodoroStore.todayInterruptRate }}%</b>
              </li>
              <li>
                <span>今日平均感受</span>
                <b>{{ pomodoroStore.todayAvgRating || '--' }} / 5</b>
              </li>
              <li>
                <!-- 累计专注分钟：汇总所有已完成日志 -->
                <span>累计专注分钟</span>
                <b>{{ pomodoroStore.logs.filter((l) => l.completed).reduce((s, l) => s + Math.round(l.durationSec / 60), 0) }}</b>
              </li>
            </ul>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 设置弹窗 -->
    <el-dialog v-model="settingsOpen" title="计时设置" width="480px">
      <el-form label-width="120px">
        <el-form-item label="专注时长">
          <el-input-number v-model="settingsDraft.focusDuration" :min="1" :max="120" /> 分钟
        </el-form-item>
        <el-form-item label="短休时长">
          <el-input-number v-model="settingsDraft.shortBreakDuration" :min="1" :max="30" /> 分钟
        </el-form-item>
        <el-form-item label="长休时长">
          <el-input-number v-model="settingsDraft.longBreakDuration" :min="1" :max="60" /> 分钟
        </el-form-item>
        <el-form-item label="每几轮进入长休">
          <el-input-number v-model="settingsDraft.roundsForLongBreak" :min="1" :max="10" /> 轮
        </el-form-item>
        <el-form-item label="自动开始下一轮">
          <el-switch v-model="settingsDraft.autoStartNext" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsOpen = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>

    <!-- 复盘弹窗 -->
    <el-dialog v-model="reflectionOpen" title="本轮复盘" width="560px">
      <el-form label-width="84px">
        <el-form-item label="本轮完成">
          <el-input v-model="reflection.what" type="textarea" :rows="3" placeholder="记录本轮的实际成果" />
        </el-form-item>
        <el-form-item label="遇到难点">
          <el-input v-model="reflection.blocker" type="textarea" :rows="2" placeholder="可一键加入错题本" />
        </el-form-item>
        <el-form-item label="下轮计划">
          <el-input v-model="reflection.next" type="textarea" :rows="2" placeholder="下个番茄做什么" />
        </el-form-item>
        <!-- 专注感受 1-5 星评分 -->
        <el-form-item label="专注感受">
          <el-rate v-model="reflection.mood" :max="5" />
        </el-form-item>
        <!-- 沉淀到笔记/错题 -->
        <el-form-item label="沉淀动作">
          <el-checkbox v-model="reflection.saveAsNote">保存为笔记</el-checkbox>
          <el-checkbox v-model="reflection.addAsWrong" :disabled="!reflection.blocker">
            难点加入错题本
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reflectionOpen = false">取消</el-button>
        <el-button type="primary" @click="saveReflection">保存复盘</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.pomodoro-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  margin: 0 0 4px;
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.stat-row {
  margin: 0 !important;
}

.timer-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timer-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.timer-card__task {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.timer-card__display {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.timer-card__time {
  font-size: 56px;
  font-weight: 700;
  font-family: 'Courier New', Courier, monospace;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  line-height: 1;
}

.timer-card__phase {
  margin-top: 6px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.timer-card__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.timer-card__task-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;

  h4 {
    margin: 0;
    font-size: 15px;
  }
}

.timer-card__task-form {
  margin-top: 4px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  h3 {
    margin: 0;
    font-size: 16px;
  }
}

.phase-hint {
  margin: 12px 0 0;
  color: var(--color-text-secondary);
  font-size: 12px;
  text-align: center;
}

.log-card {
  margin-top: 20px;
}

.log-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.log-item__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.log-item__duration {
  color: var(--color-text-secondary);
  font-size: 12px;
}

.log-item__meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.log-item__tag,
.log-item__mood {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.analysis-card {
  .muted {
    color: var(--color-text-secondary);
    font-size: 12px;
  }
}

.quality-card {
  height: 100%;
  display: flex;
  flex-direction: column;

  h4 {
    margin: 0 0 12px;
    font-size: 15px;
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  li {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: rgba(99, 102, 241, 0.08);
    border-radius: 8px;

    span {
      color: var(--color-text-secondary);
      font-size: 13px;
    }

    b {
      font-size: 15px;
    }
  }
}
</style>
