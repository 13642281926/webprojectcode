/**
 * 成就系统 API 接口
 *
 * 封装成就系统相关的 HTTP 请求。
 * 注意：成就进度计算和自动解锁逻辑已前移到前端 Store（stores/achievement.js），
 * 后端接口仅作为可选扩展（如跨设备同步、管理员手动操作等）。
 *
 * @module api/achievement
 */

import request from './request'

/**
 * 获取成就列表（含所有成就及其当前进度）
 * @returns {Promise<{code: number, data: Array}>} 成就列表
 */
export function getAchievementListApi() {
  return request.get('/achievement/list')
}

/**
 * 获取成就汇总统计数据
 * @returns {Promise<{code: number, data: object}>} 统计数据（总数、已解锁数、积分等）
 */
export function getAchievementStatsApi() {
  return request.get('/achievement/stats')
}

/**
 * 手动解锁指定成就（管理员功能）
 * @param {string} id - 成就 ID
 * @returns {Promise<object>}
 */
export function unlockAchievementApi(id) {
  return request.post(`/achievement/unlock/${id}`)
}

/**
 * 初始化/重置成就系统
 * @param {boolean} [reset=false] - 是否重置所有成就进度
 * @returns {Promise<object>}
 */
export function initAchievementsApi(reset = false) {
  return request.post('/achievement/init', null, {
    params: { reset },
  })
}
