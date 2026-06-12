/**
 * 番茄钟计时器 Store
 *
 * 基于番茄工作法的专注计时器，核心功能包括：
 * 1. 专注/短休/长休三阶段循环计时
 * 2. 基于 Date.now() 的精确倒计时（避免 setInterval 漂移）
 * 3. 后台标签页恢复校准（visibilitychange 事件）
 * 4. 番茄记录日志（完成/中断、任务关联、复盘反思）
 * 5. 多维度数据统计（今日统计、连续天数、周趋势、课程分布、专注高峰时段）
 *
 * 计时原理：
 * - 不使用递减计数器，而是记录结束时刻 endAt
 * - 每 1 秒 tick 计算 remaining = endAt - Date.now()
 * - 页面切回时通过 onVisible 校准剩余时间
 *
 * 阶段流转：
 * - 专注完成 -> 根据 completedRounds % roundsForLongBreak 决定进入短休或长休
 * - 休息结束 -> 自动回到专注阶段
 * - 中断 -> 记录中断日志，直接回到专注阶段
 *
 * @module stores/pomodoro
 */

import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { getStorage, setStorage } from '@/utils/storage'

/** localStorage 存储键名（仅持久化 settings 和 logs，不持久化计时状态） */
const POMODORO_STORAGE_KEY = 'ai-learning-pomodoro'

/**
 * 番茄钟阶段类型常量
 * @enum {string}
 */
export const PHASE = {
  FOCUS: 'focus',           // 专注阶段
  SHORT_BREAK: 'shortBreak', // 短休阶段
  LONG_BREAK: 'longBreak',   // 长休阶段
}

/** 阶段中文标签映射 */
const PHASE_LABEL = {
  [PHASE.FOCUS]: '专注时光',
  [PHASE.SHORT_BREAK]: '短休',
  [PHASE.LONG_BREAK]: '长休',
}

/** 阶段对应颜色（用于 UI 环形进度条等） */
const PHASE_COLOR = {
  [PHASE.FOCUS]: '#3b82f6',
  [PHASE.SHORT_BREAK]: '#22c55e',
  [PHASE.LONG_BREAK]: '#8b5cf6',
}

/**
 * 返回默认番茄钟设置
 * @returns {{focusDuration: number, shortBreakDuration: number, longBreakDuration: number, roundsForLongBreak: number, autoStartNext: boolean}}
 */
const defaultSettings = () => ({
  focusDuration: 25,       // 专注时长（分钟）
  shortBreakDuration: 5,   // 短休时长（分钟）
  longBreakDuration: 15,   // 长休时长（分钟）
  roundsForLongBreak: 4,   // 每完成多少轮专注进入一次长休
  autoStartNext: false,    // 是否自动开始下一阶段
})

/**
 * 生成今天的日期键（格式：YYYY-MM-DD）
 * @returns {string} 日期字符串
 */
const todayKey = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * 判断两个时间戳是否在同一天
 * @param {number} a - 时间戳 A
 * @param {number} b - 时间戳 B
 * @returns {boolean}
 */
const isSameDay = (a, b) => {
  const da = new Date(a)
  const db = new Date(b)
  return da.getFullYear() === db.getFullYear() && da.getMonth() === db.getMonth() && da.getDate() === db.getDate()
}

/**
 * 番茄钟计时器 Store
 */
export const usePomodoroStore = defineStore('pomodoro', () => {
  // ==================== 计时核心状态 ====================

  /** @type {import('vue').Ref<string>} 当前阶段（focus / shortBreak / longBreak） */
  const phase = ref(PHASE.FOCUS)
  /** @type {import('vue').Ref<number>} 剩余秒数 */
  const timeLeft = ref(25 * 60) // 秒
  /** @type {import('vue').Ref<boolean>} 是否正在计时 */
  const isRunning = ref(false)
  /** @type {import('vue').Ref<number>} 计时结束的绝对时间戳（基于 Date.now()），用于精确倒计时 */
  const endAt = ref(0)
  /** setInterval 句柄，用于清理定时器 */
  let tickHandle = null

  // ==================== 本轮上下文 ====================

  /** @type {import('vue').Ref<{type: string, id: string, title: string}>} 当前关联的学习任务 */
  const currentTask = ref({ type: 'free', id: '', title: '' })
  /** @type {import('vue').Ref<number>} 已完成的专注轮数（用于判断是否进入长休） */
  const completedRounds = ref(0)

  // ==================== 设置与历史 ====================

  /** @type {import('vue').Ref<object>} 番茄钟设置参数 */
  const settings = ref(defaultSettings())
  /** @type {import('vue').Ref<Array>} 全部番茄记录日志 */
  const logs = ref([])

  // ==================== 持久化 ====================

  /** 将设置和日志持久化到 localStorage */
  function persist() {
    setStorage(POMODORO_STORAGE_KEY, {
      settings: settings.value,
      logs: logs.value,
    })
  }

  /**
   * 从 localStorage 恢复设置和日志
   * 在 Store 实例化时自动调用
   */
  function hydrate() {
    const cached = getStorage(POMODORO_STORAGE_KEY)
    if (cached) {
      if (cached.settings) settings.value = { ...defaultSettings(), ...cached.settings }
      if (Array.isArray(cached.logs)) logs.value = cached.logs
    }
    // 初始化当前阶段的时间
    timeLeft.value = getPhaseDuration(phase.value)
  }

  // 监听设置和日志变化，自动持久化（deep: true 保证嵌套对象变化也能触发）
  watch(settings, persist, { deep: true })
  watch(logs, persist, { deep: true })

  // ==================== 派生数据（计算属性） ====================

  /** 当前阶段的中文标签 */
  const phaseLabel = computed(() => PHASE_LABEL[phase.value])
  /** 当前阶段的主题色 */
  const phaseColor = computed(() => PHASE_COLOR[phase.value])
  /** 格式化的显示时间（MM:SS） */
  const displayTime = computed(() => {
    const m = Math.floor(timeLeft.value / 60)
    const s = timeLeft.value % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  })
  /** 当前阶段的进度百分比（0-100） */
  const progress = computed(() => {
    const total = getPhaseDuration(phase.value)
    return ((total - timeLeft.value) / total) * 100
  })
  /** 是否处于专注阶段 */
  const isFocusing = computed(() => phase.value === PHASE.FOCUS)

  // ---- 今日统计 ----

  /** 今日的番茄日志 */
  const todayLogs = computed(() => logs.value.filter((l) => isSameDay(l.startAt, Date.now())))
  /** 今日完成的番茄数 */
  const todayCount = computed(() => todayLogs.value.filter((l) => l.completed).length)
  /** 今日累计专注分钟数 */
  const todayMinutes = computed(() =>
    todayLogs.value
      .filter((l) => l.completed)
      .reduce((sum, l) => sum + Math.round(l.durationSec / 60), 0),
  )
  /**
   * 连续学习天数
   * 从今天往回推算，统计连续有完成番茄的天数
   */
  const streakDays = computed(() => {
    const days = new Set(
      logs.value
        .filter((l) => l.completed)
        .map((l) => {
          const d = new Date(l.startAt)
          return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
        }),
    )
    let count = 0
    const cursor = new Date()
    while (true) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`
      if (days.has(key)) {
        count += 1
        cursor.setDate(cursor.getDate() - 1)
      } else {
        break
      }
    }
    return count
  })

  // ---- 今日完成率 / 中断率 / 平均评分 ----

  /** 今日番茄完成率（百分比） */
  const todayCompletionRate = computed(() => {
    const todays = todayLogs.value
    if (!todays.length) return 0
    return Math.round((todays.filter((l) => l.completed).length / todays.length) * 100)
  })
  /** 今日番茄中断率（百分比） */
  const todayInterruptRate = computed(() => {
    const todays = todayLogs.value
    if (!todays.length) return 0
    return Math.round((todays.filter((l) => l.interruptReason).length / todays.length) * 100)
  })
  /** 今日平均心情评分（1-5 分，保留 1 位小数） */
  const todayAvgRating = computed(() => {
    const rated = todayLogs.value.filter((l) => l.reflection?.mood)
    if (!rated.length) return 0
    return (rated.reduce((s, l) => s + l.reflection.mood, 0) / rated.length).toFixed(1)
  })

  // ---- 7 天趋势 ----

  /**
   * 最近 7 天的番茄统计数据
   * @returns {Array<{date: string, label: string, count: number, minutes: number}>}
   */
  const weeklyTrend = computed(() => {
    const days = []
    for (let i = 6; i >= 0; i -= 1) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const key = `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
      const items = logs.value.filter((l) => {
        const ld = new Date(l.startAt)
        return ld.getFullYear() === d.getFullYear() && ld.getMonth() === d.getMonth() && ld.getDate() === d.getDate()
      })
      const completed = items.filter((l) => l.completed).length
      const minutes = items.filter((l) => l.completed).reduce((s, l) => s + Math.round(l.durationSec / 60), 0)
      days.push({ date: key, label: `${d.getMonth() + 1}/${d.getDate()}`, count: completed, minutes })
    }
    return days
  })

  // ---- 课程投入分布 ----

  /**
   * 各课程的学习时长分布（仅统计 taskType === 'course' 的完成记录）
   * @returns {Array<{name: string, minutes: number}>}
   */
  const courseDistribution = computed(() => {
    const map = new Map()
    logs.value
      .filter((l) => l.completed && l.taskType === 'course')
      .forEach((l) => {
        const key = l.taskTitle || '未命名课程'
        map.set(key, (map.get(key) || 0) + Math.round(l.durationSec / 60))
      })
    return Array.from(map.entries()).map(([name, minutes]) => ({ name, minutes }))
  })

  // ---- 专注高峰时段 ----

  /**
   * 按小时统计的专注完成次数分布（0-23 时）
   * @returns {Array<{hour: number, count: number}>}
   */
  const focusPeak = computed(() => {
    const arr = Array.from({ length: 24 }, (_, h) => ({ hour: h, count: 0 }))
    logs.value
      .filter((l) => l.completed)
      .forEach((l) => {
        const h = new Date(l.startAt).getHours()
        arr[h].count += 1
      })
    return arr
  })

  // ==================== 操作方法（Actions） ====================

  /**
   * 获取指定阶段的持续时长（秒）
   * @param {string} p - 阶段类型（PHASE.FOCUS / PHASE.SHORT_BREAK / PHASE.LONG_BREAK）
   * @returns {number} 时长（秒）
   */
  function getPhaseDuration(p) {
    if (p === PHASE.FOCUS) return settings.value.focusDuration * 60
    if (p === PHASE.SHORT_BREAK) return settings.value.shortBreakDuration * 60
    if (p === PHASE.LONG_BREAK) return settings.value.longBreakDuration * 60
    return 25 * 60 // 默认 25 分钟
  }

  /**
   * 设置当前关联的学习任务
   * @param {{type?: string, id?: string, title?: string}} task - 任务信息
   */
  function setTask(task) {
    currentTask.value = { type: task?.type || 'free', id: task?.id || '', title: task?.title || '' }
  }

  // ---- 计时核心逻辑 ----

  /**
   * 计时滴答 — 每秒计算剩余时间
   * 当 remaining <= 0 时触发 completePhase 完成当前阶段
   */
  function tick() {
    const remaining = Math.round((endAt.value - Date.now()) / 1000)
    if (remaining <= 0) {
      timeLeft.value = 0
      completePhase()
    } else {
      timeLeft.value = remaining
    }
  }

  /**
   * 开始计时
   * 记录结束时间戳 endAt，启动 1 秒间隔的定时器
   * 同时注册 visibilitychange 事件以处理后台标签页恢复
   */
  function start() {
    if (isRunning.value) return
    endAt.value = Date.now() + timeLeft.value * 1000
    isRunning.value = true
    if (tickHandle) clearInterval(tickHandle)
    tickHandle = setInterval(tick, 1000)
    // 监听页面可见性变化，用于后台标签页切回时校准时间
    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', onVisible)
    }
  }

  /**
   * 暂停计时
   * 清除定时器并移除 visibilitychange 监听
   */
  function pause() {
    if (!isRunning.value) return
    isRunning.value = false
    if (tickHandle) {
      clearInterval(tickHandle)
      tickHandle = null
    }
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', onVisible)
    }
  }

  /**
   * 重置当前阶段
   * 暂停计时并将剩余时间恢复到该阶段的完整时长
   */
  function reset() {
    pause()
    timeLeft.value = getPhaseDuration(phase.value)
  }

  /**
   * 跳过当前阶段
   * 暂停计时后直接切换到下一阶段
   */
  function skip() {
    pause()
    switchPhase(true)
  }

  /**
   * 页面可见性变化回调
   * 当用户从后台标签页切回时，重新计算剩余时间以校准计时器
   */
  function onVisible() {
    if (document.visibilityState === 'visible' && isRunning.value) {
      tick()
    }
  }

  /**
   * 发送浏览器通知
   * 仅在 Notification API 可用且已授权时发送
   * @param {string} title - 通知标题
   * @param {string} body - 通知正文
   */
  function notify(title, body) {
    if (typeof window === 'undefined') return
    if (window.Notification && Notification.permission === 'granted') {
      try {
        new Notification(title, { body })
      } catch {
        /* noop — 通知发送失败不影响主流程 */
      }
    }
  }

  /**
   * 完成当前阶段
   * - 专注阶段：记录完成日志，completedRounds +1，发送完成通知
   * - 休息阶段：发送休息结束通知
   * 完成后自动切换到下一阶段
   */
  function completePhase() {
    pause()
    const now = Date.now()
    const phaseDuration = getPhaseDuration(phase.value)
    // 仅专注阶段记入统计日志
    if (phase.value === PHASE.FOCUS) {
      const log = {
        id: `${now}-${Math.random().toString(36).slice(2, 8)}`,
        startAt: now - phaseDuration * 1000,
        endAt: now,
        durationSec: phaseDuration,
        taskType: currentTask.value.type,
        taskId: currentTask.value.id,
        taskTitle: currentTask.value.title,
        completed: true,
        interruptReason: '',
        reflection: null, // 复盘反思（后续通过 saveReflection 填写）
        date: todayKey(),
      }
      logs.value.unshift(log)
      completedRounds.value += 1
      notify('专注完成', '休息一下吧，继续保持节奏！')
    } else {
      notify('休息结束', '准备好开始下一个专注周期了吗？')
    }
    switchPhase(true)
  }

  /**
   * 切换到下一阶段
   * 阶段切换规则：
   * - 专注完成后：completedRounds 为 roundsForLongBreak 的倍数时进入长休，否则短休
   * - 休息结束后：回到专注阶段
   *
   * @param {boolean} [autoStart=false] - 是否在切换后自动开始计时（取决于 settings.autoStartNext）
   */
  function switchPhase(autoStart = false) {
    if (phase.value === PHASE.FOCUS) {
      // 专注完成后判断是长休还是短休
      const useLong =
        completedRounds.value > 0 && completedRounds.value % settings.value.roundsForLongBreak === 0
      phase.value = useLong ? PHASE.LONG_BREAK : PHASE.SHORT_BREAK
    } else {
      // 休息结束后回到专注
      phase.value = PHASE.FOCUS
    }
    timeLeft.value = getPhaseDuration(phase.value)
    if (autoStart && settings.value.autoStartNext) {
      start()
    }
  }

  /**
   * 中断当前番茄钟
   * 记录中断日志（含中断原因），然后恢复到专注阶段
   * @param {string} reason - 中断原因描述
   */
  function interrupt(reason) {
    if (phase.value !== PHASE.FOCUS || isRunning.value) {
      pause()
    }
    const now = Date.now()
    const elapsed = getPhaseDuration(phase.value) - timeLeft.value
    if (elapsed <= 0) return // 还未开始的不记录
    const log = {
      id: `${now}-${Math.random().toString(36).slice(2, 8)}`,
      startAt: now - elapsed * 1000,
      endAt: now,
      durationSec: elapsed,
      taskType: currentTask.value.type,
      taskId: currentTask.value.id,
      taskTitle: currentTask.value.title,
      completed: false,
      interruptReason: reason || '未说明',
      reflection: null,
      date: todayKey(),
    }
    logs.value.unshift(log)
    // 中断后直接回到专注阶段
    phase.value = PHASE.FOCUS
    timeLeft.value = getPhaseDuration(PHASE.FOCUS)
  }

  /**
   * 保存番茄记录的复盘反思
   * @param {string} logId - 番茄日志 ID
   * @param {{mood?: number, note?: string}} reflection - 复盘数据（心情评分、文字反思）
   */
  function saveReflection(logId, reflection) {
    const log = logs.value.find((l) => l.id === logId)
    if (log) {
      log.reflection = reflection
    }
  }

  /**
   * 清空所有番茄历史记录
   */
  function clearHistory() {
    logs.value = []
  }

  // ---- 通知权限 ----

  /**
   * 请求浏览器通知权限
   * 在 Store 实例化时自动调用，仅处理 'default' 状态（用户尚未做出选择）
   */
  function requestNotification() {
    if (typeof window === 'undefined') return
    if (window.Notification && Notification.permission === 'default') {
      try {
        Notification.requestPermission()
      } catch {
        /* noop */
      }
    }
  }

  /**
   * 生成演示番茄日志（无历史数据时预填充图表）
   * 过去7天，每天2-5个完成记录 + 少量中断，散布在不同时段
   */
  function generateDemoLogs() {
    const demoLogs = []
    const now = Date.now()
    const taskPool = [
      { taskType: 'course', taskTitle: 'Vue3 高级开发实战' },
      { taskType: 'course', taskTitle: 'Python 机器学习入门' },
      { taskType: 'course', taskTitle: '数据结构与算法精讲' },
      { taskType: 'plan', taskTitle: '完成 Vue3 课程剩余章节' },
      { taskType: 'plan', taskTitle: 'LeetCode 刷题' },
      { taskType: 'free', taskTitle: '自由学习' },
    ]
    for (let day = 6; day >= 0; day -= 1) {
      const count = 2 + Math.floor(Math.random() * 4)
      for (let i = 0; i < count; i += 1) {
        const hour = 8 + Math.floor(Math.random() * 14)
        const start = new Date(now - day * 86400000)
        start.setHours(hour, Math.floor(Math.random() * 60), 0, 0)
        const durMin = 15 + Math.floor(Math.random() * 35)
        const task = taskPool[Math.floor(Math.random() * taskPool.length)]
        const ok = Math.random() > 0.2
        demoLogs.push({
          id: `demo-${day}-${i}-${Math.random().toString(36).slice(2, 6)}`,
          startAt: start.getTime(),
          endAt: start.getTime() + durMin * 60000,
          durationSec: durMin * 60,
          taskType: task.taskType,
          taskId: '',
          taskTitle: task.taskTitle,
          completed: ok,
          interruptReason: ok ? '' : '临时有事打断',
          reflection: ok && Math.random() > 0.5
            ? { mood: 1 + Math.floor(Math.random() * 5), what: '完成计划内容', blocker: '', next: '继续下一章' }
            : null,
          date: `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-${String(start.getDate()).padStart(2, '0')}`,
        })
      }
    }
    demoLogs.sort((a, b) => b.startAt - a.startAt)
    logs.value = demoLogs
  }

  hydrate()
  if (!logs.value || logs.value.length === 0) {
    generateDemoLogs()
  }
  requestNotification()

  return {
    // state
    phase,
    timeLeft,
    isRunning,
    currentTask,
    completedRounds,
    settings,
    logs,
    // computed
    phaseLabel,
    phaseColor,
    displayTime,
    progress,
    isFocusing,
    todayLogs,
    todayCount,
    todayMinutes,
    streakDays,
    todayCompletionRate,
    todayInterruptRate,
    todayAvgRating,
    weeklyTrend,
    courseDistribution,
    focusPeak,
    // actions
    setTask,
    start,
    pause,
    reset,
    skip,
    interrupt,
    switchPhase,
    saveReflection,
    clearHistory,
    getPhaseDuration,
    hydrate,
  }
})
