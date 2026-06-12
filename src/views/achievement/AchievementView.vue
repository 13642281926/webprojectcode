<script setup>
/**
 * AchievementView - 成就殿堂 / 荣誉系统
 *
 * 核心功能：
 * - 汇集多维度学习数据（笔记数、错题数、计划数、资源数、番茄分钟……）
 * - 通过 achievementStore.evaluate() 实时评估成就解锁状态
 * - 成就按分类（成长/学习/任务/创作）和状态（已解锁/未解锁）筛选
 * - 稀有度系统：普通/稀有/珍贵/史诗/传说，不同样式渲染
 * - 仪表盘：荣誉积分、解锁进度百分比环、连续学习天数、最高连击
 * - 最近解锁列表 + 成长建议
 * - 监听各 store 数据变化自动重算成就状态
 * - 成就一旦解锁永久点亮，无法重置
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAchievementStore } from '@/stores/achievement'
import { usePomodoroStore } from '@/stores/pomodoro'
import { useNoteStore } from '@/stores/notes'
import { useWrongQuestionStore } from '@/stores/wrongQuestion'
import { useStudyPlanStore } from '@/stores/studyPlan'
import { getStorage } from '@/utils/storage'
import {
  Timer,
  CircleCheck,
  Document,
  List,
  Trophy,
  Warning,
  Folder,
  Medal,
  StarFilled,
} from '@element-plus/icons-vue'

const achievementStore = useAchievementStore()
const pomodoroStore = usePomodoroStore()
const noteStore = useNoteStore()
const wrongStore = useWrongQuestionStore()
const planStore = useStudyPlanStore()

/** 筛选条件：成就分类 / 解锁状态 */
const filterCategory = ref('')
const filterStatus = ref('')

/**
 * 从各个 store 收集当前学习指标数据
 * 番茄：专注分钟、完成数、连续打卡天数
 * 笔记：笔记总数
 * 错题：总数、已掌握数
 * 计划：总数、已完成数（优先 store，兜底 localStorage）
 * 资源：从 localStorage 读取（无独立 store）
 * @returns {Object} 汇总指标对象，供 evaluate() 评估成就
 */
function collectMetrics() {
  // 累计专注分钟（仅已完成轮次）
  const focusMinutes = pomodoroStore.logs
    .filter((l) => l.completed)
    .reduce((sum, l) => sum + Math.round(l.durationSec / 60), 0)
  const completedPomodoros = pomodoroStore.logs.filter((l) => l.completed).length
  const streakDays = pomodoroStore.streakDays

  // 计划：优先使用 store，数据为空时降级到 localStorage
  const planList = (planStore.plans && planStore.plans.length)
    ? planStore.plans
    : (getStorage('ai-learning-plans', []) || [])
  const completedPlans = planList.filter((p) => p.status === 'completed' || p.completed).length
  const plans = planList.length

  // 资源：从 localStorage 读取（项目无独立 resource store）
  const resources = (getStorage('ai-learning-resources', []) || []).length

  return {
    notes: noteStore.notes?.length || 0,
    wrongQuestions: wrongStore.wrongQuestions?.length || 0,
    masteredWrongs: wrongStore.masteredCount || 0,
    plans,
    completedPlans,
    resources,
    completedPomodoros,
    focusMinutes,
    streakDays,
  }
}

/**
 * 刷新成就评估
 * 1. 收集当前各项学习指标
 * 2. 调用 achievementStore.evaluate() 计算每个成就的当前进度
 * 3. 将 streakDays 透传给 stats 面板
 */
function refresh() {
  const metrics = collectMetrics()
  const list = achievementStore.evaluate(metrics)
  // 把 streakDays 透传到 stats 计算里
  list.streakDays = metrics.streakDays
  achievementStore.setAchievements(list)
}

/** 从 store 映射的成就与统计 */
const achievements = computed(() => achievementStore.achievements)
const stats = computed(() => achievementStore.stats)

/**
 * 筛选后的成就列表
 * 排序规则：已解锁优先 > 未解锁，同状态按进度降序
 */
const filteredAchievements = computed(() => {
  let list = [...achievements.value]
  if (filterCategory.value) list = list.filter((a) => a.category === filterCategory.value)
  if (filterStatus.value === 'unlocked') list = list.filter((a) => a.unlocked)
  else if (filterStatus.value === 'locked') list = list.filter((a) => !a.unlocked)
  return list.sort((a, b) => {
    if (a.unlocked !== b.unlocked) return Number(b.unlocked) - Number(a.unlocked)
    return (b.progressPercent || 0) - (a.progressPercent || 0)
  })
})

/** 成就解锁百分比（已解锁数 / 总数） */
const unlockRate = computed(() => {
  const total = achievements.value.length
  if (!total) return 0
  return Math.round((achievements.value.filter((a) => a.unlocked).length / total) * 100)
})

/** 仪表盘摘要卡片数据 */
const summaryCards = computed(() => [
  { label: '荣誉积分', value: stats.value?.totalPoints || 0, unit: 'pts', icon: StarFilled },
  { label: '解锁进度', value: `${stats.value?.unlockedCount || 0}/${stats.value?.total || 0}`, unit: '', icon: Trophy },
  { label: '连续学习', value: stats.value?.streak || 0, unit: '天', icon: Timer },
  { label: '最高连击', value: stats.value?.longestStreak || 0, unit: '天', icon: Medal },
])

/** 最近解锁的成就列表（含格式化时间） */
const recentUnlocked = computed(() =>
  (stats.value?.recentUnlocked || []).map((item) => ({
    ...item,
    formattedTime: formatDateTime(item.unlockedAt),
  })),
)

/** 环形进度条样式：conic-gradient 实现圆弧显示解锁百分比 */
const ringStyle = computed(() => ({
  background: `conic-gradient(#22c55e 0deg, #6366f1 ${unlockRate.value * 3.6}deg, rgba(255, 255, 255, 0.08) 0deg)`,
}))

/** 下一个即将解锁的成就（未解锁 + 有目标值） */
const nextUnlockAchievement = computed(() =>
  filteredAchievements.value.find((a) => !a.unlocked && a.target > 0),
)

/** 页面挂载：并行拉取各 store 数据，完成后触发初次成就评估 */
onMounted(async () => {
  await Promise.all([
    noteStore.fetchNotes().catch(() => null),
    wrongStore.fetchWrongQuestions().catch(() => null),
    planStore.fetchPlans?.().catch(() => null),
  ])
  refresh()
})

/**
 * 监听数据源变化自动重算
 * 当番茄日志数、笔记数、错题数或已掌握数变化时触发
 */
watch(
  () => [
    pomodoroStore.logs.length,
    noteStore.notes?.length,
    wrongStore.wrongQuestions?.length,
    wrongStore.masteredCount,
  ],
  () => refresh(),
)

/** 点击成就卡片：已解锁提示成功，未解锁提示进度差 */
const handleAchievementClick = (achievement) => {
  if (achievement.unlocked) {
    ElMessage.success(`成就「${achievement.title}」已解锁`)
  } else {
    ElMessage.info(`还差 ${achievement.target - achievement.progress} 即可解锁「${achievement.title}」`)
  }
}

// ==================== 工具函数 ====================

/** 稀有度 -> Element Plus tag type 映射 */
const getRarityType = (rarity) => {
  const map = { common: 'info', uncommon: 'success', rare: 'primary', epic: 'warning', legendary: 'danger' }
  return map[rarity] || 'info'
}
/** 稀有度 -> 中文文本映射 */
const getRarityText = (rarity) => {
  const map = { common: '普通', uncommon: '稀有', rare: '珍贵', epic: '史诗', legendary: '传说' }
  return map[rarity] || '普通'
}
/** 图标名称 -> Element Plus 图标组件映射 */
const getIconComponent = (iconName) => {
  const iconMap = { Timer, Document, CircleCheck, List, Trophy, Warning, Folder }
  return iconMap[iconName] || Trophy
}
/** 获取成就进度百分比，避免 undefined */
const getProgressPercentage = (achievement) => achievement.progressPercent || 0
/** 格式化日期时间为 "MM/DD HH:MM" 中文格式 */
const formatDateTime = (value) => {
  if (!value) return '待解锁'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="page-container achievement-page">
    <!-- 顶部 Hero 区域：荣誉殿堂介绍 + 进度环 + 下一枚徽章预览 -->
    <section class="achievement-hero glass-card">
      <div class="achievement-hero__content">
        <div class="achievement-hero__badge">
          <el-icon><Trophy /></el-icon>
          荣誉殿堂
        </div>
        <h2 class="achievement-hero__title">让学习成果被看见，让坚持拥有奖励</h2>
        <p class="achievement-hero__desc">
          成就会根据学习天数、笔记、错题、番茄和资源沉淀自动解锁，达成即永久点亮，无法重置。
        </p>
        <div class="achievement-hero__actions">
          <!-- 解锁百分比 -->
          <div class="achievement-hero__progress">
            <span>荣誉完成度</span>
            <strong>{{ unlockRate }}%</strong>
          </div>
        </div>
      </div>
      <!-- 环形进度指示器 + 下一枚徽章 -->
      <div class="achievement-hero__panel">
        <div class="achievement-hero__ring" :style="ringStyle">
          <span>{{ unlockRate }}%</span>
        </div>
        <div class="achievement-hero__hint">
          <div class="achievement-hero__hint-label">下一枚徽章</div>
          <div class="achievement-hero__hint-title">{{ nextUnlockAchievement?.title || '全部点亮' }}</div>
          <div class="achievement-hero__hint-text">
            {{ nextUnlockAchievement ? `${nextUnlockAchievement.progress}/${nextUnlockAchievement.target}` : '当前已达成全部成就' }}
          </div>
        </div>
      </div>
    </section>

    <!-- 摘要卡片：荣誉积分 / 解锁进度 / 连续学习 / 最高连击 -->
    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="summary-card glass-card hover-lift">
        <div class="summary-card__icon">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div>
          <div class="summary-card__label">{{ card.label }}</div>
          <div class="summary-card__value">
            {{ card.value }}
            <span v-if="card.unit">{{ card.unit }}</span>
          </div>
        </div>
      </article>
    </section>

    <!-- 主区域：左侧成就面板 + 右侧最近解锁 / 成长建议 -->
    <section class="achievement-main">
      <!-- 全部成就面板 -->
      <div class="achievement-board glass-card">
        <div class="section-head">
          <div>
            <div class="section-head__eyebrow">Achievement Board</div>
            <h3 class="section-title">全部成就</h3>
          </div>
          <!-- 筛选按钮：分类 + 状态 -->
          <div class="filter-tabs">
            <el-radio-group v-model="filterCategory" size="small">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="成长">成长</el-radio-button>
              <el-radio-button label="学习">学习</el-radio-button>
              <el-radio-button label="任务">任务</el-radio-button>
              <el-radio-button label="创作">创作</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="filterStatus" size="small">
              <el-radio-button label="">全部状态</el-radio-button>
              <el-radio-button label="unlocked">已解锁</el-radio-button>
              <el-radio-button label="locked">未解锁</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 成就卡片网格 -->
        <div class="achievements-grid">
          <div
            v-for="achievement in filteredAchievements"
            :key="achievement.id"
            class="achievement-card"
            :class="{ locked: !achievement.unlocked, [`rarity-${achievement.rarity}`]: achievement.rarity }"
            @click="handleAchievementClick(achievement)"
          >
            <!-- 光照装饰 -->
            <div class="achievement-card__glow" />
            <!-- 成就图标 -->
            <div class="achievement-icon">
              <el-icon :size="34"><component :is="getIconComponent(achievement.icon)" /></el-icon>
            </div>
            <div class="achievement-info">
              <div class="achievement-top">
                <div class="achievement-name">{{ achievement.title }}</div>
                <!-- 稀有度标签 -->
                <el-tag :type="getRarityType(achievement.rarity)" size="small" effect="dark">
                  {{ getRarityText(achievement.rarity) }}
                </el-tag>
              </div>
              <div class="achievement-desc">{{ achievement.description }}</div>
              <div class="achievement-meta">
                <span class="achievement-points">+{{ achievement.points }} 积分</span>
                <span class="achievement-category">{{ achievement.category }}</span>
              </div>
              <!-- 进度条 + 当前进度/目标 -->
              <div class="achievement-progress">
                <el-progress
                  :percentage="getProgressPercentage(achievement)"
                  :stroke-width="10"
                  :show-text="false"
                  :color="achievement.unlocked ? '#22c55e' : '#6366f1'"
                />
                <span class="progress-text">{{ achievement.progress }}/{{ achievement.target }}</span>
              </div>
              <div class="achievement-footer">
                <span :class="['achievement-status', achievement.unlocked ? 'is-unlocked' : 'is-locked']">
                  {{ achievement.unlocked ? '已点亮' : '待达成' }}
                </span>
                <span class="achievement-unlock-time">{{ formatDateTime(achievement.unlockedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边栏：最近解锁 + 成长建议 -->
      <aside class="achievement-side">
        <!-- 最近解锁列表 -->
        <div class="achievement-side__panel glass-card">
          <div class="section-head">
            <div>
              <div class="section-head__eyebrow">Recent Unlocks</div>
              <h3 class="section-title">最近解锁</h3>
            </div>
          </div>
          <div v-if="recentUnlocked.length" class="recent-list">
            <div v-for="item in recentUnlocked" :key="item.id" class="recent-item">
              <div class="recent-icon">
                <el-icon><component :is="getIconComponent(item.icon)" /></el-icon>
              </div>
              <div class="recent-info">
                <div class="recent-name">{{ item.name }}</div>
                <div class="recent-time">{{ item.formattedTime }}</div>
              </div>
              <div class="recent-points">+{{ item.points }}</div>
            </div>
          </div>
          <el-empty v-else description="继续完成任务，这里会展示你的新勋章" />
        </div>

        <!-- 成长建议 -->
        <div class="achievement-side__panel glass-card">
          <div class="section-head">
            <div>
              <div class="section-head__eyebrow">Growth Tips</div>
              <h3 class="section-title">成长建议</h3>
            </div>
          </div>
          <div class="tips-list">
            <div class="tip-item">
              <el-icon><Document /></el-icon>
              <span>持续整理学习笔记，最快推进"笔记达人"。</span>
            </div>
            <div class="tip-item">
              <el-icon><Warning /></el-icon>
              <span>把错题沉淀进系统，成就和复习质量会一起提升。</span>
            </div>
            <div class="tip-item">
              <el-icon><Folder /></el-icon>
              <span>多上传高质量资料，资源类成就会自动同步进度。</span>
            </div>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped lang="scss">
.achievement-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.achievement-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.25), transparent 28%),
    radial-gradient(circle at bottom left, rgba(14, 165, 233, 0.16), transparent 30%),
    var(--color-bg-card);
  overflow: hidden;
}

.achievement-hero__content {
  max-width: 720px;
}

.achievement-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.14);
  color: #c7d2fe;
  margin-bottom: 18px;
}

.achievement-hero__title {
  font-size: 32px;
  line-height: 1.2;
  margin-bottom: 12px;
  color: var(--color-text-primary);
}

.achievement-hero__desc {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  max-width: 640px;
}

.achievement-hero__actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.achievement-hero__progress {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.45);
  color: var(--color-text-secondary);

  strong {
    color: var(--color-text-primary);
    font-size: 22px;
  }
}

.achievement-hero__panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 18px;
  min-width: 280px;
}

.achievement-hero__ring {
  width: 168px;
  height: 168px;
  border-radius: 50%;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;

  span {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(10, 14, 26, 0.92);
    color: #fff;
    font-size: 30px;
    font-weight: 700;
  }
}

.achievement-hero__hint {
  width: 100%;
  padding: 18px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.48);
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}

.achievement-hero__hint-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.achievement-hero__hint-title {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.achievement-hero__hint-text {
  margin-top: 6px;
  color: var(--color-text-secondary);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 20px;
  border-radius: 20px;
}

.summary-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9), rgba(14, 165, 233, 0.75));
  color: #fff;
  font-size: 22px;
}

.summary-card__label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.summary-card__value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);

  span {
    margin-left: 6px;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-secondary);
  }
}

.achievement-main {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(300px, 0.8fr);
  gap: 20px;
  align-items: start;
}

.achievement-board,
.achievement-side__panel {
  padding: 24px;
  border-radius: 24px;
}

.achievement-side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-head__eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.section-title {
  margin-top: 6px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.filter-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.achievement-card {
  position: relative;
  overflow: hidden;
  background: rgba(12, 18, 34, 0.86);
  border-radius: 22px;
  padding: 24px;
  display: flex;
  gap: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: rgba(99, 102, 241, 0.5);
    box-shadow: 0 18px 44px rgba(15, 23, 42, 0.34);
  }

  &.locked {
    opacity: 0.74;

    .achievement-icon {
      filter: grayscale(100%);
    }
  }
}

.achievement-card__glow {
  position: absolute;
  inset: auto -20% -40% auto;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.24), transparent 70%);
  pointer-events: none;
}

.achievement-card.rarity-legendary {
  border-color: rgba(245, 158, 11, 0.55);
}

.achievement-card.rarity-epic {
  border-color: rgba(139, 92, 246, 0.55);
}

.achievement-card.rarity-rare {
  border-color: rgba(59, 130, 246, 0.42);
}

.achievement-icon {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.28);
}

.achievement-info {
  flex: 1;
  position: relative;
  z-index: 1;
}

.achievement-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.achievement-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.achievement-desc {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-top: 10px;
  margin-bottom: 12px;
}

.achievement-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.achievement-points {
  font-size: 13px;
  color: #fbbf24;
}

.achievement-category {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-text-secondary);
  font-size: 12px;
}

.achievement-progress {
  display: flex;
  align-items: center;
  gap: 12px;

  .el-progress {
    flex: 1;
  }

  .progress-text {
    font-size: 12px;
    color: var(--color-text-muted);
    min-width: 44px;
    text-align: right;
  }
}

.achievement-footer {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.achievement-status {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
}

.achievement-status.is-unlocked {
  background: rgba(34, 197, 94, 0.14);
  color: #4ade80;
}

.achievement-status.is-locked {
  background: rgba(148, 163, 184, 0.14);
  color: #cbd5e1;
}

.achievement-unlock-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.recent-list,
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.recent-item,
.tip-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.recent-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8b5cf6, #06b6d4);
  color: #fff;
}

.recent-info {
  flex: 1;
}

.recent-name {
  color: var(--color-text-primary);
  font-weight: 600;
}

.recent-time {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.recent-points {
  color: #fbbf24;
  font-weight: 700;
}

.tip-item {
  align-items: flex-start;
  color: var(--color-text-secondary);
  line-height: 1.7;

  .el-icon {
    margin-top: 2px;
    color: #818cf8;
    font-size: 18px;
  }
}

@media (max-width: 1200px) {
  .achievement-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .achievement-hero {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }

  .achievement-hero__title {
    font-size: 26px;
  }

  .achievement-hero__panel {
    min-width: 100%;
  }
}

:deep(.el-radio-group) {
  gap: 8px;
}

:deep(.el-radio-button__inner) {
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: var(--color-text-secondary);
  box-shadow: none;
}

:deep(.el-radio-button:first-child .el-radio-button__inner),
:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 999px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: transparent;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.26);
}
</style>
