/**
 * Axios 请求封装 — 统一 HTTP 客户端
 *
 * 本模块基于 Axios 创建统一的 HTTP 请求实例，提供以下核心能力：
 * 1. 统一 baseURL 和超时配置
 * 2. 请求拦截器：自动注入 JWT Token、显示全局 Loading
 * 3. 响应拦截器：统一错误处理、401 自动登出跳转、静默模式支持
 * 4. Loading 计数器：支持并发请求共享单个 Loading 实例
 *
 * 关键行为：
 * - 请求自动携带 Authorization: Bearer <token>
 * - 响应 code === 401 时自动清除登录态并跳转登录页
 * - config.showLoading = false 可禁用 Loading 动画
 * - config.silent = true 可静默处理错误（不弹窗提示）
 *
 * @module api/request
 */

import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * Axios 统一实例
 * - baseURL: /api（通过 Vite 代理转发到后端）
 * - timeout: 15 秒
 * - Content-Type: application/json
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

/**
 * Loading 计数器
 * 多个并发请求共享同一个 ElLoading 实例，loadingCount 记录当前活跃请求数
 * 当 loadingCount 从 0 变为 1 时显示 Loading，从 1 变为 0 时隐藏
 */
let loadingCount = 0
/** ElLoading 实例引用 */
let loadingInstance = null

/**
 * 显示全局 Loading 遮罩
 * 使用引用计数，多个请求共享同一个 Loading 实例
 * @param {string} [text='加载中...'] - Loading 提示文字
 */
function showLoading(text = '加载中...') {
  loadingCount += 1
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: true,
      text,
      background: 'rgba(10, 14, 26, 0.72)',
    })
  }
}

/**
 * 隐藏全局 Loading 遮罩
 * 引用计数归零时才真正关闭 Loading
 */
function hideLoading() {
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

/**
 * 处理 401 未授权：清除登录态并跳转到登录页
 * 自动记录当前页面路径作为 redirect 参数，供登录后回跳
 * @param {string} [message] - 提示信息
 */
async function handleUnauthorized(message) {
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.warning(message || '登录已过期，请重新登录')
  // 动态导入 router 避免循环依赖
  const { default: router } = await import('@/router')
  const current = router.currentRoute.value.fullPath
  if (current !== '/login') {
    router.push({ name: 'Login', query: { redirect: current } })
  }
}

/**
 * 请求拦截器
 *
 * 功能：
 * - 打印请求日志（开发调试用）
 * - 显示全局 Loading（可通过 config.showLoading = false 禁用）
 * - 自动注入 JWT Token 到 Authorization 请求头
 */
request.interceptors.request.use(
  (config) => {
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`, config.data || config.params || '')
    // 默认显示 Loading，可通过 showLoading: false 关闭
    if (config.showLoading !== false) {
      showLoading(config.loadingText)
    }
    const userStore = useUserStore()
    // 已登录状态下自动携带 Token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error(`[API Error] ${error.config?.url}`, error)
    hideLoading()
    return Promise.reject(error)
  },
)

/**
 * 响应拦截器
 *
 * 功能：
 * - 打印响应日志
 * - 隐藏 Loading
 * - 处理业务错误码（code !== 200）：非静默模式下弹窗提示
 * - 处理 401 未授权：自动登出并跳转登录页
 * - 支持 config.silent = true 静默处理错误（不弹 ElMessage）
 */
request.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.config.url}`, response.data)
    hideLoading()
    const res = response.data

    // 业务层 401 未授权
    if (res && res.code === 401) {
      handleUnauthorized(res.message)
      return Promise.reject(new Error(res.message || '未授权'))
    }

    // 业务错误码（非 200）
    if (res && typeof res.code !== 'undefined' && res.code !== 200) {
      if (configSilent(response.config)) {
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  (error) => {
    hideLoading()
    const status = error.response?.status
    const resData = error.response?.data

    // HTTP 状态码 401 或业务 code === 401
    if (status === 401 || resData?.code === 401) {
      handleUnauthorized(resData?.message)
      return Promise.reject(error)
    }

    const message = resData?.message || error.message || '网络异常，请稍后重试'
    if (!configSilent(error.config)) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

/**
 * 判断请求是否配置了静默模式
 * 静默模式下不弹出 ElMessage 错误提示，由调用方自行处理
 * @param {object} [config] - Axios 请求配置
 * @returns {boolean}
 */
function configSilent(config) {
  return config?.silent === true
}

export default request
