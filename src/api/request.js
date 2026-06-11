import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/** Axios 统一实例 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let loadingCount = 0
let loadingInstance = null

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

function hideLoading() {
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

/** 处理未授权：清除登录态并跳转 */
async function handleUnauthorized(message) {
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.warning(message || '登录已过期，请重新登录')
  const { default: router } = await import('@/router')
  const current = router.currentRoute.value.fullPath
  if (current !== '/login') {
    router.push({ name: 'Login', query: { redirect: current } })
  }
}

/** 请求拦截器 */
request.interceptors.request.use(
  (config) => {
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`, config.data || config.params || '')
    if (config.showLoading !== false) {
      showLoading(config.loadingText)
    }
    const userStore = useUserStore()
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

/** 响应拦截器 */
request.interceptors.response.use(
  (response) => {
    console.log(`[API Response] ${response.config.url}`, response.data)
    hideLoading()
    const res = response.data

    if (res && res.code === 401) {
      handleUnauthorized(res.message)
      return Promise.reject(new Error(res.message || '未授权'))
    }

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

function configSilent(config) {
  return config?.silent === true
}

export default request
