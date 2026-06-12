/**
 * 学习资源 API 接口
 *
 * 封装资源 CRUD、文件上传/下载和分类查询相关的 HTTP 请求。
 * 文件上传使用 multipart/form-data 格式，下载使用 blob 响应类型。
 *
 * @module api/resource
 */

import request from './request'

/**
 * 获取资源列表（支持分页和筛选）
 * @param {object} [params] - 查询参数（page、pageSize、category、keyword、type 等）
 * @returns {Promise<{code: number, data: {list: Array, total: number}}>} 分页资源列表
 */
export function getResourceListApi(params) {
  return request.get('/resource/list', { params })
}

/**
 * 获取资源详情
 * @param {string|number} id - 资源 ID
 * @returns {Promise<{code: number, data: object}>} 资源详情数据
 */
export function getResourceDetailApi(id) {
  return request.get(`/resource/detail/${id}`)
}

/**
 * 创建资源记录（JSON 方式，不含文件）
 * @param {object} data - 资源数据（name、description、category 等）
 * @returns {Promise<object>} 新创建的资源数据
 */
export function createResourceApi(data) {
  return request.post('/resource/create', data)
}

/**
 * 上传资源文件（multipart/form-data 格式）
 * Content-Type 设为 multipart/form-data 以支持文件上传
 * @param {FormData} formData - 包含文件的 FormData 对象
 * @returns {Promise<object>} 上传后的资源数据
 */
export function uploadResourceApi(formData) {
  return request.post('/resource/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除资源
 * @param {string|number} id - 资源 ID
 * @returns {Promise<object>}
 */
export function deleteResourceApi(id) {
  return request.delete(`/resource/delete/${id}`)
}

/**
 * 下载资源文件
 * 使用 blob 响应类型以支持文件流下载
 * @param {string|number} id - 资源 ID
 * @returns {Promise<Blob>} 文件 Blob 数据
 */
export function downloadResourceApi(id) {
  return request.get(`/resource/download/${id}`, {
    responseType: 'blob'
  })
}

/**
 * 获取资源分类列表
 * @returns {Promise<{code: number, data: Array}>} 分类列表
 */
export function getResourceCategoriesApi() {
  return request.get('/resource/categories')
}
