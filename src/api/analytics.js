/**
 * 数据分析 API 接口
 *
 * 封装学习数据统计与分析相关的 HTTP 请求，包括：
 * - 学习概览数据（总时长、番茄数、笔记数等）
 * - 仪表盘统计数据
 * - 任务/计划统计分析
 * - 资源使用统计分析
 *
 * @module api/analytics
 */

import request from './request'

/**
 * 获取学习概览数据（总览页使用）
 * @returns {Promise<{code: number, data: object}>} 概览统计数据
 */
export function getAnalyticsOverviewApi() {
  return request.get('/analytics/overview')
}

/**
 * 获取仪表盘统计数据（首页仪表盘使用）
 * @returns {Promise<{code: number, data: object}>} 仪表盘数据
 */
export function getDashboardStatsApi() {
  return request.get('/analytics/dashboard')
}

/**
 * 获取任务/计划统计分析数据
 * @returns {Promise<{code: number, data: object}>} 任务统计（完成率、趋势等）
 */
export function getTaskStatsApi() {
  return request.get('/analytics/tasks')
}

/**
 * 获取资源使用统计分析数据
 * @returns {Promise<{code: number, data: object}>} 资源统计（使用频率、类型分布等）
 */
export function getResourceStatsApi() {
  return request.get('/analytics/resources')
}
