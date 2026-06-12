/**
 * 用户状态管理 Store
 *
 * 管理用户认证信息（token）、用户资料、登录/退出逻辑。
 * 登录态通过 localStorage 持久化，页面刷新后可恢复会话。
 * 支持管理员角色判断（isAdmin），用于前端权限控制。
 *
 * @module stores/user
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'
import { getUserProfileApi } from '@/api/user'

/** localStorage 存储键名 */
const USER_STORAGE_KEY = 'ai-learning-user'

/**
 * 返回默认用户信息对象
 * 每次调用返回新对象，避免引用共享导致的状态污染
 * @returns {{id: string, username: string, nickname: string, avatar: string, signature: string, studyDays: number, totalHours: number, role: string}}
 */
const defaultUserInfo = () => ({
  id: '',
  username: '',
  nickname: '学习者',
  avatar: '',
  signature: '每天进步一点点',
  studyDays: 0,
  totalHours: 0,
  role: 'user', // 'user' | 'admin'
})

/**
 * 用户与登录状态 Store
 * 使用 Composition API 风格的 Pinia Store（Setup Store）
 */
export const useUserStore = defineStore('user', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<string>} JWT 认证令牌 */
  const token = ref('')
  /** @type {import('vue').Ref<object>} 用户基本信息（昵称、头像、签名等） */
  const userInfo = ref(defaultUserInfo())
  /** @type {import('vue').Ref<boolean>} 是否正在加载用户资料 */
  const profileLoading = ref(false)

  // ==================== 计算属性（Getters） ====================

  /** 是否已登录 — 依据 token 是否存在 */
  const isLoggedIn = computed(() => Boolean(token.value))
  /** 是否为管理员 — 依据 role 字段判断，用于前端权限控制（如显示管理菜单） */
  const isAdmin = computed(() => userInfo.value.role === 'admin')

  // ==================== 内部辅助方法 ====================

  /**
   * 从 localStorage 恢复登录态
   * 在 Store 实例化时自动调用，实现页面刷新后会话保持
   */
  function loadFromStorage() {
    const cached = getStorage(USER_STORAGE_KEY)
    if (cached) {
      token.value = cached.token || ''
      userInfo.value = { ...defaultUserInfo(), ...cached.userInfo }
    }
  }

  /**
   * 将当前状态持久化到 localStorage
   * 每次登录/更新资料后调用
   */
  function persist() {
    setStorage(USER_STORAGE_KEY, {
      token: token.value,
      userInfo: userInfo.value,
    })
  }

  // ==================== 操作方法（Actions） ====================

  /**
   * 设置登录状态
   * 登录成功后调用，保存 token 和用户信息并持久化
   * @param {{token: string, userInfo: object}} payload - 登录响应数据
   */
  function setLogin(payload) {
    token.value = payload.token
    userInfo.value = { ...defaultUserInfo(), ...payload.userInfo }
    persist()
  }

  /**
   * 退出登录
   * 清除 token 和用户信息，移除 localStorage 中的持久化数据
   */
  function logout() {
    token.value = ''
    userInfo.value = defaultUserInfo()
    removeStorage(USER_STORAGE_KEY)
  }

  /**
   * 更新用户资料（部分字段）
   * 合并更新后自动持久化到 localStorage
   * @param {object} partial - 需要更新的字段（支持部分更新）
   */
  function updateProfile(partial) {
    userInfo.value = { ...userInfo.value, ...partial }
    persist()
  }

  /**
   * 从后端拉取用户资料（当前为 Mock 数据）
   * 仅在已登录状态下调用，失败时保留缓存数据不受影响
   * @returns {Promise<void>}
   */
  async function fetchProfile() {
    if (!isLoggedIn.value) return
    profileLoading.value = true
    try {
      const res = await getUserProfileApi()
      userInfo.value = { ...userInfo.value, ...res.data }
      persist()
    } catch (err) {
      console.warn('[user store] fetchProfile 失败，继续使用缓存数据:', err.message || err)
    } finally {
      profileLoading.value = false
    }
  }

  // Store 实例化时自动从 localStorage 恢复登录态
  loadFromStorage()

  return {
    // 状态
    token,
    userInfo,
    profileLoading,
    // 计算属性
    isLoggedIn,
    isAdmin,
    // 方法
    setLogin,
    logout,
    updateProfile,
    fetchProfile,
    loadFromStorage,
  }
})
