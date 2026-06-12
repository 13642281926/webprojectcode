/**
 * 学习计划 API 接口
 *
 * 封装学习计划 CRUD 相关的 HTTP 请求。
 *
 * @module api/studyPlan
 */

import request from './request'

/**
 * 获取学习计划列表（支持分页和筛选）
 * @param {object} [params] - 查询参数（page、pageSize、priority、status、keyword 等）
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>} 分页计划列表
 */
export function getStudyPlanListApi(params) {
  return request.get('/study-plan/list', { params })
}

/**
 * 创建学习计划
 * @param {object} data - 计划数据（title、priority、deadline、description 等）
 * @returns {Promise<object>} 新创建的计划数据
 */
export function createStudyPlanApi(data) {
  return request.post('/study-plan', data)
}

/**
 * 更新学习计划
 * @param {string|number} id - 计划 ID
 * @param {object} data - 需要更新的字段（status、priority、title 等）
 * @returns {Promise<object>} 更新后的计划数据
 */
export function updateStudyPlanApi(id, data) {
  return request.put(`/study-plan/${id}`, data)
}

/**
 * 删除学习计划
 * @param {string|number} id - 计划 ID
 * @returns {Promise<object>}
 */
export function deleteStudyPlanApi(id) {
  return request.delete(`/study-plan/${id}`)
}
