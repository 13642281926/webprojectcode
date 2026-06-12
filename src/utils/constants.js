/**
 * 业务常量映射
 *
 * 集中定义项目中使用的业务枚举值和常量配置，包括：
 * - 学习计划优先级映射（PLAN_PRIORITY）：高/中/低 -> 标签文字 + Element Plus type
 * - 学习计划状态映射（PLAN_STATUS）：待开始/进行中/已完成 -> 标签文字 + Element Plus type
 * - 快捷入口配置（QUICK_ENTRIES）：首页快捷导航按钮的定义
 *
 * 使用这些常量而非硬编码字符串可保证：
 * 1. 显示文字和 UI 样式统一
 * 2. 修改时只需改一处
 * 3. TypeScript 友好（如后续迁移到 TS）
 *
 * @module utils/constants
 */

/**
 * 学习计划优先级映射
 * type 对应 Element Plus el-tag 的 type 属性
 * @type {{high: {label: string, type: string}, medium: {label: string, type: string}, low: {label: string, type: string}}}
 */
export const PLAN_PRIORITY = {
  high: { label: '高', type: 'danger' },
  medium: { label: '中', type: 'warning' },
  low: { label: '低', type: 'info' },
}

/**
 * 学习计划状态映射
 * type 对应 Element Plus el-tag 的 type 属性
 * @type {{pending: {label: string, type: string}, doing: {label: string, type: string}, done: {label: string, type: string}}}
 */
export const PLAN_STATUS = {
  pending: { label: '待开始', type: 'info' },
  doing: { label: '进行中', type: 'primary' },
  done: { label: '已完成', type: 'success' },
}

/**
 * 首页快捷入口配置
 * 定义仪表盘首页显示的快捷功能导航按钮
 * @type {Array<{title: string, path: string, icon: string, color: string}>}
 */
export const QUICK_ENTRIES = [
  { title: '学习计划', path: '/study-plan', icon: 'Calendar', color: '#3b82f6' },
  { title: '课程管理', path: '/course', icon: 'Reading', color: '#8b5cf6' },
  { title: 'AI 助手', path: '/ai-assistant', icon: 'ChatDotRound', color: '#22c55e' },
  { title: '数据分析', path: '/analytics', icon: 'DataAnalysis', color: '#f59e0b' },
]
