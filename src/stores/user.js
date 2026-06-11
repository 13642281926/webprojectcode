import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'
import { getUserProfileApi } from '@/api/user'

const USER_STORAGE_KEY = 'ai-learning-user'

const defaultUserInfo = () => ({
  id: '',
  username: '',
  nickname: '学习者',
  avatar: '',
  signature: '每天进步一点点',
  studyDays: 0,
  totalHours: 0,
  role: 'user',
})

/** 用户与登录状态 */
export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(defaultUserInfo())
  const profileLoading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => userInfo.value.role === 'admin')

  function loadFromStorage() {
    const cached = getStorage(USER_STORAGE_KEY)
    if (cached) {
      token.value = cached.token || ''
      userInfo.value = { ...defaultUserInfo(), ...cached.userInfo }
    }
  }

  function persist() {
    setStorage(USER_STORAGE_KEY, {
      token: token.value,
      userInfo: userInfo.value,
    })
  }

  function setLogin(payload) {
    token.value = payload.token
    userInfo.value = { ...defaultUserInfo(), ...payload.userInfo }
    persist()
  }

  function logout() {
    token.value = ''
    userInfo.value = defaultUserInfo()
    removeStorage(USER_STORAGE_KEY)
  }

  function updateProfile(partial) {
    userInfo.value = { ...userInfo.value, ...partial }
    persist()
  }

  /** 拉取用户资料（Mock） */
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

  loadFromStorage()

  return {
    token,
    userInfo,
    profileLoading,
    isLoggedIn,
    isAdmin,
    setLogin,
    logout,
    updateProfile,
    fetchProfile,
    loadFromStorage,
  }
})
