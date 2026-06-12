/**
 * 用户相关 API 接口
 *
 * 封装用户认证与资料管理相关的 HTTP 请求，包括：
 * - 登录/注册/退出
 * - 获取/更新个人资料
 *
 * @module api/user
 */

import request from './request'

/**
 * 用户登录
 * @param {{username: string, password: string}} data - 登录凭证
 * @returns {Promise<{code: number, data: {token: string, userInfo: object}, message: string}>} 返回 token 和用户信息
 */
export function loginApi(data) {
  return request.post('/user/login', data)
}

/**
 * 用户注册
 * @param {{username: string, password: string, nickname?: string}} data - 注册信息
 * @returns {Promise<object>} 注册结果
 */
export function registerApi(data) {
  return request.post('/user/register', data)
}

/**
 * 用户退出登录
 * @returns {Promise<object>}
 */
export function logoutApi() {
  return request.post('/user/logout', {})
}

/**
 * 获取当前用户个人资料
 * @returns {Promise<{code: number, data: object}>} 返回用户详细信息
 */
export function getUserProfileApi() {
  return request.get('/user/profile')
}

/**
 * 更新用户个人资料（部分字段）
 * @param {object} data - 需要更新的字段（nickname、avatar、signature 等）
 * @returns {Promise<object>} 更新后的用户信息
 */
export function updateUserProfileApi(data) {
  return request.put('/user/profile', data)
}
