<script setup>
/**
 * AnalyticsView - 数据分析 / 学习数据洞察
 *
 * 核心功能：
 * - 时间范围切换：本周 / 本月 / 季度，切换时重新拉取数据
 * - 核心统计卡片：总学习时长、总任务数、完成率、连续学习天数
 * - 亮点洞察：最佳学习时段 + 学习预测
 * - ECharts 图表：
 *   - 学习时长趋势折线图（buildLineOption）
 *   - 任务完成分布饼图（buildPieOption）
 *   - 学习活跃热力图（buildHeatmapOption，含 30 天占位数据）
 *   - 类别学习趋势图
 * - 学习类别分布进度条列表 + 成就解锁预测
 * - 后端接口失败时使用硬编码占位数据确保页面可渲染
 */
import { computed, onMounted, ref } from 'vue'
import { Timer, List, CircleCheck, Trophy, Calendar, DataAnalysis, TrendCharts } from '@element-plus/icons-vue'
import StatCard from '@/components/common/StatCard.vue'
import ChartCard from '@/components/common/ChartCard.vue'
import { getDashboardStatsApi, getTaskStatsApi, getResourceStatsApi } from '@/api/analytics'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { buildPieOption, buildLineOption, buildBarOption, buildHeatmapOption } from '@/utils/echarts'

const userStore = useUserStore()
const themeStore = useThemeStore()

/** 各类统计数据，null 表示尚未加载 */
const stats = ref(null)
const taskStats = ref(null)
const resourceStats = ref(null)
/** 全页面加载状态 */
const loading = ref(true)
/** 时间范围筛选值：week / month / quarter */
const timeRange = ref('week')

/**
 * 任务完成分布饼图配置
 * 优先使用后端数据，无数据时使用硬编码兜底
 */
const pieOption = computed(() => {
  const data = taskStats.value?.taskDistribution || [
    { name: '已完成', value: 35 },
    { name: '进行中', value: 25 },
    { name: '待开始', value: 20 },
    { name: '已逾期', value: 10 },
  ]
  return buildPieOption({
    title: '任务完成分布',
    data,
    isDark: themeStore.isDark,
  })
})

/**
 * 学习时长趋势折线图配置
 * X 轴为星期标签，Y 轴为学习分钟数
 */
const lineOption = computed(() => {
  const labels = stats.value?.weekTrend?.labels || ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const values = stats.value?.weekTrend?.values || [45, 60, 55, 80, 70, 95, 88]
  return buildLineOption({
    labels,
    values,
    seriesName: '学习时长（分钟）',
    isDark: themeStore.isDark,
  })
})

/**
 * 任务完成量柱状图配置
 * 将分钟值除以 10 作为任务完成数量估算
 */
const barOption = computed(() => {
  const labels = stats.value?.weekTrend?.labels || ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const values = (stats.value?.weekTrend?.values || [45, 60, 55, 80, 70, 95, 88]).map((x) => Math.round(x / 10))
  return buildBarOption({
    labels,
    values,
    seriesName: '任务完成量',
    isDark: themeStore.isDark,
  })
})

/**
 * 学习活跃热力图配置
 * 后端未提供数据时自动生成近 30 天的随机占位数据
 */
const heatmapOption = computed(() => {
  const data =
    stats.value?.heatmap ||
    Array.from({ length: 30 }, (_, i) => {
      const d = new Date()
      d.setDate(d.getDate() - (29 - i))
      const value = Math.floor(Math.random() * 120)
      return [`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`, value]
    })
  return buildHeatmapOption({
    data,
    isDark: themeStore.isDark,
  })
})

/** 类别学习趋势图（当前使用硬编码占位数据） */
const categoryTrendOption = computed(() => {
  return buildLineOption({
    labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    values: [45, 52, 38, 65, 48, 72, 55],
    seriesName: '学习类别趋势',
    isDark: themeStore.isDark
  })
})

/** 最佳学习时段分析（占位数据，后续可接后端） */
const bestTimeSlot = computed(() => {
  return {
    hour: 20,
    efficiency: 85
  }
})

/** 学习预测（占位数据，后续可接入 AI 预测算法） */
const studyPrediction = computed(() => {
  return {
    tomorrow: 120,
    thisWeek: 680,
    trend: 'up'
  }
})

/** 各类别学习时长与颜色映射 */
const categoryStats = ref([
  { name: '前端开发', minutes: 450, color: '#3b82f6' },
  { name: '后端开发', minutes: 320, color: '#8b5cf6' },
  { name: '算法', minutes: 280, color: '#22c55e' },
  { name: '语言学习', minutes: 180, color: '#f59e0b' }
])

/** 时间范围切换时触发重新拉取 */
const onTimeRangeChange = (value) => {
  timeRange.value = value
  fetchAnalytics()
}

/**
 * 并行拉取三类分析数据
 * 失败时保持空对象，让各 computed 使用默认值渲染
 */
async function fetchAnalytics() {
  loading.value = true
  try {
    const [statsRes, taskRes, resourceRes] = await Promise.all([
      getDashboardStatsApi({ range: timeRange.value }),
      getTaskStatsApi({ range: timeRange.value }),
      getResourceStatsApi({ range: timeRange.value }),
    ])
    stats.value = statsRes.data
    taskStats.value = taskRes.data
    resourceStats.value = resourceRes.data
  } catch (err) {
    console.warn('[analytics] 数据加载失败，使用本地占位数据:', err.message || err)
    stats.value = stats.value || {}
    taskStats.value = taskStats.value || {}
    resourceStats.value = resourceStats.value || {}
  } finally {
    loading.value = false
  }
}

onMounted(fetchAnalytics)
</script>

<template>
  <div v-loading="loading" class="page-container analytics">
    <!-- 页面标题 + 时间范围切换 -->
    <div class="page-header">
      <div class="page-header__left">
        <div class="page-badge">
          <el-icon :size="16"><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </div>
        <h1 class="page-title">学习数据洞察</h1>
        <p class="page-subtitle">可视化你的学习历程，发现进步空间</p>
      </div>
      <div class="page-header__right">
        <!-- 时间范围切换：本周 / 本月 / 季度 -->
        <el-radio-group v-model="timeRange" size="default" @change="onTimeRangeChange">
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="quarter">季度</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 核心数据统计卡片 -->
    <div class="metric-grid">
      <StatCard label="总学习时长" :value="stats?.totalMinutes || '3250'" unit="分钟" :icon="Timer" color="#3b82f6" :clickable="true" />
      <StatCard label="总任务数" :value="taskStats?.totalTasks || '128'" unit="个" :icon="List" color="#8b5cf6" :clickable="true" />
      <StatCard label="完成率" :value="stats?.completedRate || '78'" unit="%" :icon="CircleCheck" color="#22c55e" :clickable="true" />
      <StatCard label="连续学习" :value="userStore.userInfo.studyDays || 0" unit="天" :icon="Trophy" color="#f59e0b" :clickable="true" />
    </div>

    <!-- 亮点洞察区：最佳时段 + 学习预测 -->
    <el-row :gutter="20" class="insights-row">
      <el-col :xs="24" :lg="12">
        <el-card class="glass-card hover-lift insight-card" shadow="never">
          <div class="insight-card__header">
            <div class="insight-card__icon" style="background: linear-gradient(135deg, #22c55e, #10b981);">
              <el-icon :size="24"><TrendCharts /></el-icon>
            </div>
            <div class="insight-card__content">
              <h3>最佳学习时段</h3>
              <p>你的黄金学习时间是 <strong>{{ bestTimeSlot.hour }}:00</strong>，效率高达 <strong>{{ bestTimeSlot.efficiency }}%</strong></p>
            </div>
          </div>
          <div class="insight-card__tip">
            建议在这个时段安排高难度学习任务
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="glass-card hover-lift insight-card" shadow="never">
          <div class="insight-card__header">
            <div class="insight-card__icon" style="background: linear-gradient(135deg, #f59e0b, #d97706);">
              <el-icon :size="24"><Calendar /></el-icon>
            </div>
            <div class="insight-card__content">
              <h3>学习预测</h3>
              <p>预计明天学习 <strong>{{ studyPrediction.tomorrow }} 分钟</strong>，本周累计 <strong>{{ studyPrediction.thisWeek }} 分钟</strong></p>
            </div>
          </div>
          <div class="insight-card__tip" style="color: #22c55e;">
            学习趋势良好，继续保持！
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主图表区：时长趋势折线图 + 任务分布饼图 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <ChartCard title="学习时长趋势" :option="lineOption" :loading="loading" height="380px" />
      </el-col>
      <el-col :xs="24" :lg="8">
        <ChartCard title="任务完成分布" :option="pieOption" :loading="loading" height="380px" />
      </el-col>
    </el-row>

    <!-- 学习活跃热力图 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <ChartCard title="学习活跃热力图" :option="heatmapOption" :loading="loading" height="220px" />
      </el-col>
    </el-row>

    <!-- 详细分析区：类别分布 + 类别趋势图 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card class="glass-card hover-lift" shadow="never">
          <template #header>
            <div class="section-header">
              <h3>学习类别分布</h3>
            </div>
          </template>
          <div class="category-list">
            <!-- 各学习类别进度条（分钟数 / 总分钟数 * 100） -->
            <div v-for="(cat, idx) in categoryStats" :key="idx" class="category-item">
              <div class="category-info">
                <span class="category-name">{{ cat.name }}</span>
                <span class="category-minutes">{{ cat.minutes }} 分钟</span>
              </div>
              <el-progress :percentage="Math.round((cat.minutes / 1230) * 100)" :stroke-width="12" :show-text="false" :color="cat.color" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="类别学习趋势" :option="categoryTrendOption" :loading="loading" height="300px" />
      </el-col>
    </el-row>

    <!-- 成就解锁预测 -->
    <el-card class="glass-card hover-lift" shadow="never">
      <template #header>
        <div class="section-header">
          <h3>成就解锁预测</h3>
          <el-button type="primary" link>查看全部成就</el-button>
        </div>
      </template>
      <div class="achievement-prediction">
        <div class="prediction-item">
          <div class="prediction-info">
            <div class="prediction-icon" style="background: rgba(139, 92, 246, 0.2); color: #8b5cf6;">
              <Trophy />
            </div>
            <div class="prediction-content">
              <h4>知识探索者</h4>
              <p class="prediction-status">还差 <strong>25</strong> 分钟学习时长解锁</p>
            </div>
          </div>
          <el-tag type="success">预计 1 天内</el-tag>
        </div>
        <div class="prediction-item">
          <div class="prediction-info">
            <div class="prediction-icon" style="background: rgba(34, 197, 94, 0.2); color: #22c55e;">
              <Trophy />
            </div>
            <div class="prediction-content">
              <h4>笔记达人</h4>
              <p class="prediction-status">还差 <strong>3</strong> 条笔记解锁</p>
            </div>
          </div>
          <el-tag type="warning">预计 2 天内</el-tag>
        </div>
        <div class="prediction-item">
          <div class="prediction-info">
            <div class="prediction-icon" style="background: rgba(245, 158, 11, 0.2); color: #f59e0b;">
              <Trophy />
            </div>
            <div class="prediction-content">
              <h4>学习狂人</h4>
              <p class="prediction-status">还差 <strong>15</strong> 次学习解锁</p>
            </div>
          </div>
          <el-tag type="info">预计 5 天内</el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.analytics {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
}

.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  font-size: 32px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.insights-row {
  margin-top: 4px;
}

.insight-card {
  height: 100%;
}

.insight-card__header {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.insight-card__icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.insight-card__content {
  flex: 1;
}

.insight-card__content h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
}

.insight-card__content p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.insight-card__tip {
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 10px 12px;
  background: rgba(139, 92, 246, 0.1);
  border-radius: 8px;
}

.charts-row {
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
    font-weight: 600;
  }
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
}

.category-minutes {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.achievement-prediction {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.prediction-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.05);
}

.prediction-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.prediction-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.prediction-content h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
}

.prediction-status {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>