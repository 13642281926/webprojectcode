/**
 * 仪表盘 API 接口
 *
 * 封装首页仪表盘相关的 HTTP 请求。
 * 禁用全局 Loading（showLoading: false）以避免首页加载时出现遮罩闪烁。
 *
 * @module api/dashboard
 */

import request from './request'

/**
 * 获取仪表盘首页统计数据
 * 禁用 Loading 以保证首页加载体验流畅
 * @returns {Promise<{code: number, data: object}>} 仪表盘统计数据（学习时长、任务数、番茄数等）
 */
export function getDashboardStatsApi() {
  return request.get('/dashboard/stats', { showLoading: false })
}
