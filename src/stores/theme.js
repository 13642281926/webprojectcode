/**
 * 主题与界面配置 Store
 *
 * 管理深色/浅色模式切换、粒子背景开关、侧边栏折叠状态。
 * 主题切换通过操作 DOM（document.documentElement.classList）即时生效。
 * 所有设置通过 localStorage 持久化，刷新后恢复用户偏好。
 *
 * 关键行为：
 * - 默认启用以太粒子背景（showParticles: true）
 * - 主题切换通过 watch 自动持久化，避免手动调用 persist
 * - applyTheme 直接操作 <html> 元素的 class 和 data-theme 属性
 *
 * @module stores/theme
 */

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { getStorage, setStorage } from '@/utils/storage'

/** localStorage 存储键名 */
const THEME_STORAGE_KEY = 'ai-learning-theme'

/**
 * 返回默认主题配置
 * @returns {{isDark: boolean, showParticles: boolean, sidebarCollapsed: boolean}}
 */
const defaultTheme = () => ({
  isDark: true,          // 默认深色模式
  showParticles: true,   // 默认显示粒子背景
  sidebarCollapsed: false, // 侧边栏默认展开
})

/**
 * 系统主题状态 Store
 */
export const useThemeStore = defineStore('theme', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<boolean>} 是否处于深色模式 */
  const isDark = ref(true)
  /** @type {import('vue').Ref<boolean>} 是否显示流动粒子背景 */
  const showParticles = ref(true)
  /** @type {import('vue').Ref<boolean>} 侧边栏是否折叠 */
  const sidebarCollapsed = ref(false)

  // ==================== 内部辅助方法 ====================

  /**
   * 从 localStorage 加载主题配置
   * 在 Store 实例化时自动调用
   */
  function loadFromStorage() {
    const cached = getStorage(THEME_STORAGE_KEY)
    if (cached) {
      isDark.value = cached.isDark ?? true
      showParticles.value = cached.showParticles ?? true
      sidebarCollapsed.value = cached.sidebarCollapsed ?? false
    }
    // 加载后立即应用主题到 DOM
    applyTheme()
  }

  /**
   * 将当前主题配置持久化到 localStorage
   * 由 watch 自动触发，无需手动调用
   */
  function persist() {
    setStorage(THEME_STORAGE_KEY, {
      isDark: isDark.value,
      showParticles: showParticles.value,
      sidebarCollapsed: sidebarCollapsed.value,
    })
  }

  /**
   * 应用主题到 DOM
   * 通过操作 <html> 元素的 class 和 data-theme 属性实现主题切换
   * - .dark 类控制 Tailwind CSS 的暗色模式变体
   * - data-theme 属性供其他组件/样式读取当前主题
   */
  function applyTheme() {
    document.documentElement.classList.toggle('dark', isDark.value)
    document.documentElement.dataset.theme = isDark.value ? 'dark' : 'light'
  }

  // ==================== 操作方法（Actions） ====================

  /**
   * 切换深色/浅色主题
   * 切换后立即应用到 DOM，持久化由 watch 统一处理（避免重复写入 localStorage）
   */
  function toggleTheme() {
    isDark.value = !isDark.value
    applyTheme()
    // persist 由 watch 统一触发，避免重复写入
  }

  /** 切换流动粒子背景的显示/隐藏 */
  function toggleParticles() {
    showParticles.value = !showParticles.value
  }

  /** 切换侧边栏的展开/折叠状态 */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  // 监听状态变化，自动持久化到 localStorage
  watch([isDark, showParticles, sidebarCollapsed], persist)

  // Store 实例化时自动恢复配置
  loadFromStorage()

  return {
    // 状态
    isDark,
    showParticles,
    sidebarCollapsed,
    // 方法
    toggleTheme,
    toggleParticles,
    toggleSidebar,
    loadFromStorage,
  }
})
