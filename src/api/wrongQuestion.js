/**
 * 错题本 API 接口
 *
 * 封装错题 CRUD、掌握标记和分类查询相关的 HTTP 请求。
 *
 * @module api/wrongQuestion
 */

import request from './request'

/**
 * 获取错题列表（支持分页和筛选）
 * @param {object} [params] - 查询参数（page、pageSize、category、difficulty、status、keyword 等）
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>} 分页错题列表
 */
export function getWrongQuestionListApi(params) {
  return request.get('/wrongQuestion/list', { params })
}

/**
 * 获取错题详情
 * @param {string|number} id - 错题 ID
 * @returns {Promise<{code: number, data: object}>} 错题详情数据
 */
export function getWrongQuestionDetailApi(id) {
  return request.get(`/wrongQuestion/detail/${id}`)
}

/**
 * 创建错题记录
 * @param {object} data - 错题数据（question、answer、category、difficulty、reason 等）
 * @returns {Promise<object>} 新创建的错题数据
 */
export function createWrongQuestionApi(data) {
  return request.post('/wrongQuestion/create', data)
}

/**
 * 更新错题信息
 * @param {string|number} id - 错题 ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise<object>} 更新后的错题数据
 */
export function updateWrongQuestionApi(id, data) {
  return request.put(`/wrongQuestion/update/${id}`, data)
}

/**
 * 删除错题
 * @param {string|number} id - 错题 ID
 * @returns {Promise<object>}
 */
export function deleteWrongQuestionApi(id) {
  return request.delete(`/wrongQuestion/delete/${id}`)
}

/**
 * 将错题标记为"已掌握"
 * @param {string|number} id - 错题 ID
 * @returns {Promise<object>} 更新后的错题数据
 */
export function markAsMasteredApi(id) {
  return request.post(`/wrongQuestion/master/${id}`)
}

/**
 * 获取错题分类列表
 * @returns {Promise<{code: number, data: Array}>} 分类列表
 */
export function getWrongQuestionCategoriesApi() {
  return request.get('/wrongQuestion/categories')
}
