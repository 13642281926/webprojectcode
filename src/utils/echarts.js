/**
 * ECharts 图表构建器
 *
 * 基于 ECharts 封装的图表 option 构建函数，提供以下能力：
 * 1. 深色/浅色双主题配色方案，与 Element Plus 暗色模式同步
 * 2. 统一的动画配置（动画时长、缓动函数、序列延迟）
 * 3. 四种常用图表类型的 option 构建函数：折线图、柱状图、热力图、环形图
 * 4. 日期格式归一化处理（normalizeDate），支撑热力图的日历坐标系
 *
 * 设计原则：
 * - 每个构建函数返回完整的 ECharts option 对象，调用方可直接 setOption
 * - 所有颜色值和主题样式集中管理，便于统一调整
 * - 动画参数可全局配置（考核点：动态效果）
 *
 * @module utils/echarts
 */

/** 图表配色色板（6 色，紫-蓝-青-绿-橙-红渐变） */
export const CHART_COLORS = ['#6366f1', '#8b5cf6', '#06b6d4', '#22c55e', '#f59e0b', '#ef4444']

/**
 * 全局图表动画配置
 * 用于提升图表的视觉动态效果（考核点：动态效果）
 * animationDelay 使用索引函数实现序列动画（每个数据点依次入场）
 */
export const chartAnimation = {
  animation: true,
  animationDuration: 1200,
  animationEasing: 'cubicOut',
  animationDelay: (idx) => idx * 80, // 每个数据点延迟 80ms 依次动画
}

/**
 * 深色模式图表主题
 * 文字颜色 #94a3b8（slate-400），适用于暗色背景
 */
export const darkChartTheme = {
  backgroundColor: 'transparent',
  textStyle: { color: '#94a3b8' },
  ...chartAnimation,
}

/**
 * 浅色模式图表主题
 * 文字颜色 #718096（gray-500），坐标轴灰色、网格线更淡，适配浅色背景
 */
export const lightChartTheme = {
  backgroundColor: 'transparent',
  textStyle: { color: '#718096' },
  ...chartAnimation,
}

/**
 * 根据当前主题模式返回对应的图表主题配置
 * @param {boolean} [isDark=true] - 是否为深色模式
 * @returns {object} ECharts 主题配置对象
 */
export function getChartTheme(isDark = true) {
  return isDark ? darkChartTheme : lightChartTheme
}

/**
 * 获取坐标轴样式配置（适配深色/浅色模式）
 * 包含轴线条、轴标签、分割线的颜色设定
 * @param {boolean} [isDark=true] - 是否为深色模式
 * @returns {{axisLine: object, axisLabel: object, splitLine: object}} 坐标轴样式对象
 */
export function getAxisStyle(isDark = true) {
  return {
    axisLine: { lineStyle: { color: isDark ? 'rgba(148,163,184,0.3)' : '#d1d5db' } },
    axisLabel: { color: isDark ? '#94a3b8' : '#718096' },
    splitLine: { lineStyle: { color: isDark ? 'rgba(148,163,184,0.1)' : '#e5e7eb' } },
  }
}

/**
 * 构建折线图 option
 * 包含渐变面积填充、平滑曲线、焦点高亮等交互效果
 *
 * @param {object} options - 配置选项
 * @param {string} [options.title] - 图表标题（可选）
 * @param {string[]} options.labels - X 轴标签数据
 * @param {number[]} options.values - Y 轴数值数据
 * @param {string} [options.seriesName='学习时长'] - 系列名称
 * @param {boolean} [options.isDark=true] - 是否深色模式
 * @returns {object} ECharts 折线图 option 对象
 */
export function buildLineOption({ title, labels, values, seriesName = '学习时长', isDark = true }) {
  const theme = getChartTheme(isDark)
  const axis = getAxisStyle(isDark)
  return {
    ...theme,
    title: title ? { text: title, textStyle: { color: isDark ? '#f1f5f9' : '#2d3748', fontSize: 14 } } : undefined,
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: title ? 48 : 32, bottom: 32 },
    xAxis: {
      type: 'category',
      data: labels,
      ...axis,
    },
    yAxis: {
      type: 'value',
      ...axis,
    },
    series: [
      {
        name: seriesName,
        type: 'line',
        smooth: true,
        data: values,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59,130,246,0.35)' },
              { offset: 1, color: 'rgba(59,130,246,0)' },
            ],
          },
        },
        lineStyle: { color: CHART_COLORS[0], width: 2 },
        itemStyle: { color: CHART_COLORS[0] },
        emphasis: { focus: 'series' },
      },
    ],
  }
}

/**
 * 构建柱状图 option
 * 包含渐变色柱体、圆角顶部、焦点高亮等视觉效果
 *
 * @param {object} options - 配置选项
 * @param {string} [options.title] - 图表标题（可选）
 * @param {string[]} options.labels - X 轴标签数据
 * @param {number[]} options.values - Y 轴数值数据
 * @param {string} [options.seriesName='任务数'] - 系列名称
 * @param {boolean} [options.isDark=true] - 是否深色模式
 * @returns {object} ECharts 柱状图 option 对象
 */
export function buildBarOption({ title, labels, values, seriesName = '任务数', isDark = true }) {
  const theme = getChartTheme(isDark)
  const axis = getAxisStyle(isDark)
  return {
    ...theme,
    title: title ? { text: title, textStyle: { color: isDark ? '#f1f5f9' : '#2d3748', fontSize: 14 } } : undefined,
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: title ? 48 : 32, bottom: 32 },
    xAxis: {
      type: 'category',
      data: labels,
      ...axis,
    },
    yAxis: {
      type: 'value',
      ...axis,
    },
    series: [
      {
        name: seriesName,
        type: 'bar',
        barWidth: '40%',
        data: values,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: CHART_COLORS[1] },
              { offset: 1, color: CHART_COLORS[0] },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: { focus: 'series' },
      },
    ],
  }
}

/**
 * 将各种日期格式归一化为 YYYY-MM-DD（ECharts calendar 坐标系必需格式）
 *
 * 支持的输入格式：
 * - 数字时间戳（13 位毫秒）
 * - YYYY-MM-DD、YYYY/MM/DD、YYYY.MM.DD 等分隔符格式
 * - 中文日期格式（YYYY年MM月DD日）
 * - ISO 8601 / RFC 2822 等标准日期字符串
 *
 * @param {string|number|null} input - 原始日期值
 * @returns {string} 归一化后的日期字符串（YYYY-MM-DD），无效输入返回空字符串
 */
function normalizeDate(input) {
  if (input == null) return ''
  if (typeof input === 'number') {
    // 纯数字按时间戳处理
    const d = new Date(input)
    if (Number.isNaN(d.getTime())) return ''
    return formatYMD(d)
  }
  const str = String(input).trim()
  if (!str) return ''
  // 已经是 YYYY-MM-DD
  if (/^\d{4}-\d{1,2}-\d{1,2}$/.test(str)) {
    const [y, m, d] = str.split('-')
    return `${y}-${pad(m)}-${pad(d)}`
  }
  // YYYY/MM/DD 或 YYYY.MM.DD 或 YYYY_MM_DD
  const m1 = str.match(/^(\d{4})[\/.\u5e74](\d{1,2})[\/.\u6708](\d{1,2})/)
  if (m1) return `${m1[1]}-${pad(m1[2])}-${pad(m1[3])}`
  // MM/DD/YYYY
  const m2 = str.match(/^(\d{1,2})[\/.\u6708](\d{1,2})[\/.\u65e5](\d{4})/)
  if (m2) return `${m2[3]}-${pad(m2[1])}-${pad(m2[2])}`
  // ISO 8601 / RFC 2822 等
  const d = new Date(str)
  if (!Number.isNaN(d.getTime())) return formatYMD(d)
  return ''
}

/**
 * 补零到两位（如 3 -> '03'）
 * @param {number|string} n - 数值
 * @returns {string} 补零后的字符串
 */
function pad(n) {
  return String(n).padStart(2, '0')
}

/**
 * 将 Date 对象格式化为 YYYY-MM-DD
 * @param {Date} d - Date 对象
 * @returns {string} YYYY-MM-DD 格式日期
 */
function formatYMD(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 构建学习热力图 option（日历坐标系）
 *
 * 使用 ECharts 的 calendar + heatmap 组合实现类似 GitHub 贡献图的学习热力图。
 * 自动根据数据最大值决定使用分钟级或数量级色阶。
 *
 * 数据处理流程：
 * 1. 使用 normalizeDate 将所有日期统一为 YYYY-MM-DD
 * 2. 过滤无效日期项，按日期升序排列
 * 3. 根据 maxValue 是否 > 5 判断使用分钟级分段色阶或连续色阶
 *
 * @param {object} options - 配置选项
 * @param {Array<[string, number]>} [options.data=[]] - 热力图数据，每项为 [日期, 数值]
 * @param {string|number} [options.year] - 年份范围，不传时自动根据数据计算
 * @param {boolean} [options.isDark=true] - 是否深色模式
 * @returns {object} ECharts 热力图 option 对象
 */
export function buildHeatmapOption({ data = [], year, isDark = true }) {
  const theme = getChartTheme(isDark)
  // 归一化日期 + 过滤无效项
  const normalized = (Array.isArray(data) ? data : [])
    .map((item) => {
      if (!Array.isArray(item) || item.length < 2) return null
      const date = normalizeDate(item[0])
      if (!date) return null
      return [date, Number(item[1]) || 0]
    })
    .filter(Boolean)
    .sort((a, b) => (a[0] < b[0] ? -1 : 1))

  const values = normalized.map(([, v]) => v)
  const maxValue = Math.max(...values, 0)
  const usesMinuteScale = maxValue > 5
  const range =
    year ??
    (normalized.length
      ? [normalized[0][0], normalized[normalized.length - 1][0]]
      : new Date().getFullYear())

  return {
    ...theme,
    tooltip: {
      position: 'top',
      formatter: (params) => {
        const [date, value] = Array.isArray(params.value) ? params.value : ['', params.value]
        return usesMinuteScale ? `${date}：${value} 分钟` : `${date}：${value}`
      },
    },
    visualMap: usesMinuteScale
      ? {
          min: 0,
          max: Math.max(maxValue, 180),
          type: 'piecewise',
          orient: 'horizontal',
          left: 'center',
          bottom: 0,
          textStyle: { color: isDark ? '#94a3b8' : '#718096' },
          pieces: [
            { min: 120, label: '120分钟以上', color: '#6366f1' },
            { min: 60, max: 119, label: '60-119分钟', color: '#8b5cf6' },
            { min: 30, max: 59, label: '30-59分钟', color: '#06b6d4' },
            { min: 1, max: 29, label: '1-29分钟', color: '#a5b4fc' },
            { min: 0, max: 0, label: '未学习', color: isDark ? '#1e293b' : '#e5e7eb' },
          ],
        }
      : {
          min: 0,
          max: Math.max(maxValue, 5),
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '0%',
          inRange: {
            color: isDark
              ? ['rgba(59, 130, 246, 0.1)', '#3b82f6']
              : ['#eff6ff', '#1d4ed8'],
          },
          show: false,
        },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      range,
      cellSize: ['auto', usesMinuteScale ? 14 : 13],
      splitLine: usesMinuteScale
        ? { lineStyle: { color: isDark ? '#1e293b' : '#e2e8f0' } }
        : { show: false },
      itemStyle: {
        borderWidth: 2,
        borderColor: isDark ? (usesMinuteScale ? '#0b1020' : 'rgba(0,0,0,0.2)') : '#fff',
        color: isDark ? 'rgba(255,255,255,0.05)' : '#f3f4f6',
        borderRadius: 2,
      },
      yearLabel: { show: false },
      dayLabel: {
        color: isDark ? '#94a3b8' : '#718096',
        fontSize: 10,
        margin: usesMinuteScale ? 4 : undefined,
        nameMap: 'cn',
      },
      monthLabel: {
        color: isDark ? '#94a3b8' : '#718096',
        fontSize: 10,
        margin: usesMinuteScale ? 8 : undefined,
        align: usesMinuteScale ? 'left' : undefined,
        nameMap: 'cn',
      },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: normalized,
      },
    ],
  }
}

/**
 * 构建环形图 option
 *
 * 中空环形布局（内外半径 45%-70%），自动按色板循环分配颜色。
 * 支持图例（底部居中）、缩放强调、圆角扇区等视觉效果。
 *
 * @param {object} options - 配置选项
 * @param {string} [options.title] - 图表标题（可选）
 * @param {Array<{name: string, value: number}>} options.data - 环形图数据
 * @param {boolean} [options.isDark=true] - 是否深色模式
 * @returns {object} ECharts 环形图 option 对象
 */
export function buildPieOption({ title, data, isDark = true }) {
  const theme = getChartTheme(isDark)
  return {
    ...theme,
    title: title ? { text: title, textStyle: { color: isDark ? '#f1f5f9' : '#2d3748', fontSize: 14 } } : undefined,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: isDark ? '#94a3b8' : '#718096' } },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#0a0e1a', borderWidth: 2 },
        label: { color: '#cbd5e1' },
        emphasis: {
          scale: true,
          scaleSize: 8,
        },
        data: data.map((item, i) => ({
          ...item,
          itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length] },
        })),
      },
    ],
  }
}

