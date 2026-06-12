/**
 * 课程管理 API 接口
 *
 * 封装课程 CRUD 相关的 HTTP 请求。
 * 获取课程详情时禁用全局 Loading（showLoading: false），避免频繁切换视图时闪烁。
 *
 * @module api/course
 */

import request from './request'

/**
 * 获取课程列表（支持分页和筛选）
 * @param {object} [params] - 查询参数（page、pageSize、keyword、category 等）
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>} 分页课程列表
 */
export function getCourseListApi(params) {
  return request.get('/course/list', { params })
}

/**
 * 获取课程详情
 * 禁用全局 Loading 以避免从列表切换到详情时的遮罩闪烁
 * @param {string|number} id - 课程 ID
 * @returns {Promise<{code: number, data: object}>} 课程详情数据
 */
export function getCourseDetailApi(id) {
  return request.get(`/course/${id}`, { showLoading: false })
}

/**
 * 创建课程
 * @param {object} data - 课程数据（name、description、cover、category 等）
 * @returns {Promise<object>} 新创建的课程数据
 */
export function createCourseApi(data) {
  return request.post('/course', data)
}

/**
 * 更新课程信息
 * @param {string|number} id - 课程 ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise<object>} 更新后的课程数据
 */
export function updateCourseApi(id, data) {
  return request.put(`/course/${id}`, data)
}

/**
 * 删除课程
 * @param {string|number} id - 课程 ID
 * @returns {Promise<object>}
 */
export function deleteCourseApi(id) {
  return request.delete(`/course/${id}`)
}
