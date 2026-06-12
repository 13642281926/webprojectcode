/**
 * @file main.js - Vue 3 应用入口文件
 * @description
 * AI学习成长助手平台（AI Learning Growth Assistant Platform）的主入口。
 * 负责完成以下引导流程：
 *   1. 创建 Vue 3 应用实例（createApp）
 *   2. 注册 Pinia 全局状态管理（createPinia）
 *   3. 注册 Vue Router 路由系统
 *   4. 注册全局自定义指令 v-lazy（考核点：自定义指令 + IntersectionObserver）
 *   5. 加载主题配置（深色模式 + localStorage 持久化）
 *   6. 挂载应用到 #app 根节点
 *
 * 样式引入顺序说明：
 *   1. Element Plus 基础样式 → 建立组件默认外观
 *   2. Element Plus 深色变量 → 覆盖为深色主题 CSS 变量
 *   3. 项目全局样式 global.scss → 定义 CSS 变量、通用工具类
 *   4. element-override.scss → 精细覆盖 Element Plus 组件样式
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useThemeStore } from '@/stores/theme'
import vLazy from '@/directives/lazy'

// ============================================================
// 第 1 步：引入全局样式
// ============================================================

// Element Plus 基础样式（按需引入组件，样式仍需全局注册）
import 'element-plus/dist/index.css'
// Element Plus 深色主题 CSS 变量覆盖
import 'element-plus/theme-chalk/dark/css-vars.css'

// 项目全局样式：定义 CSS 变量、mixin、通用工具类等
import '@/styles/global.scss'
// Element Plus 组件样式精细覆盖（如圆角、阴影、毛玻璃效果等）
import '@/styles/element-override.scss'

// ============================================================
// 第 2 步：创建应用实例并注册插件
// ============================================================

const app = createApp(App)
const pinia = createPinia()

// 注册 Pinia 状态管理 —— 所有组件可通过 useXxxStore() 访问全局状态
app.use(pinia)
// 注册 Vue Router —— 启用 SPA 页面路由
app.use(router)

// 注册全局自定义指令 v-lazy（考核点：自定义指令）
// 用于 <img> 标签，图片进入视口后才开始加载，加载完成后渐显
app.directive('lazy', vLazy)

// ============================================================
// 第 3 步：初始化主题（深色模式 + localStorage 持久化）
// ============================================================
// 在挂载前从 localStorage 读取用户偏好，设置 isDark / showParticles 等状态
// 确保首次渲染即为用户选择的主题，避免闪烁
const themeStore = useThemeStore()
themeStore.loadFromStorage()

// ============================================================
// 第 4 步：挂载应用到 DOM
// ============================================================
app.mount('#app')
