/**
 * 笔记管理 API 接口
 *
 * 封装笔记 CRUD 和分类查询相关的 HTTP 请求。
 *
 * @module api/note
 */

import request from './request'

/**
 * 获取笔记列表（支持分页和筛选）
 * @param {object} [params] - 查询参数（page、pageSize、category、keyword 等）
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>} 分页笔记列表
 */
export function getNoteListApi(params) {
  return request.get('/note/list', { params })
}

/**
 * 获取笔记详情
 * @param {string|number} id - 笔记 ID
 * @returns {Promise<{code: number, data: object}>} 笔记详情（包含 Markdown 内容）
 */
export function getNoteDetailApi(id) {
  return request.get(`/note/detail/${id}`)
}

/**
 * 创建笔记
 * @param {object} data - 笔记数据（title、content、category 等）
 * @returns {Promise<object>} 新创建的笔记数据
 */
export function createNoteApi(data) {
  return request.post('/note/create', data)
}

/**
 * 更新笔记
 * @param {string|number} id - 笔记 ID
 * @param {object} data - 需要更新的字段
 * @returns {Promise<object>} 更新后的笔记数据
 */
export function updateNoteApi(id, data) {
  return request.put(`/note/update/${id}`, data)
}

/**
 * 删除笔记
 * @param {string|number} id - 笔记 ID
 * @returns {Promise<object>}
 */
export function deleteNoteApi(id) {
  return request.delete(`/note/delete/${id}`)
}

/**
 * 获取笔记分类列表
 * @returns {Promise<{code: number, data: Array}>} 分类列表
 */
export function getNoteCategoriesApi() {
  return request.get('/note/categories')
}
