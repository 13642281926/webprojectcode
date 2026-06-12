/**
 * 路由实例与导航守卫
 *
 * 本模块负责：
 * 1. 创建 Vue Router 实例（History 模式）
 * 2. 定义顶层静态路由（登录页、布局容器、404 通配）
 * 3. 注册全局前置导航守卫，实现登录鉴权与页面重定向
 * 4. 自动设置页面标题（document.title）
 *
 * @module router/index
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { layoutRoutes, asyncRoutes } from './routes'

/** 布局组件 — 按需懒加载，打包为独立 chunk */
const AppLayout = () => import(/* webpackChunkName: "layout" */ '@/components/layout/AppLayout.vue')
/** 登录页 — 按需懒加载，打包为独立 chunk */
const LoginView = () => import(/* webpackChunkName: "login" */ '@/views/login/LoginView.vue')

/**
 * 顶层静态路由表
 * - /login：登录页，无需鉴权
 * - /：主布局容器，默认重定向到 /dashboard，所有子路由受 requiresAuth 保护
 * - /:pathMatch(.*)*：404 通配路由，提示后自动跳转首页
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '登录', requiresAuth: false }, // requiresAuth: false 表示无需登录即可访问
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard', // 根路径默认重定向到首页仪表盘
    meta: { requiresAuth: true }, // 布局容器及其所有子路由均需登录
    children: layoutRoutes,
  },
  {
    path: '/:pathMatch(.*)*',
    /**
     * 404 通配路由 — 异步加载 ElMessage 提示用户后重定向到首页
     * 使用 beforeEnter 而非组件渲染，避免创建不必要的视图实例
     */
    beforeEnter: async () => {
      const { ElMessage } = await import('element-plus')
      ElMessage.warning('页面不存在，已跳转到首页')
      return '/dashboard'
    },
  },
]

/**
 * Vue Router 实例
 * - 使用 HTML5 History 模式
 * - 路由切换后自动滚动到顶部
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/**
 * 全局前置导航守卫
 *
 * 逻辑说明：
 * 1. 对于 requiresAuth 路由，未登录则重定向到 /login，并携带 redirect 参数供登录后回跳
 * 2. 已登录用户访问 /login 时，直接跳转到仪表盘首页
 * 3. 每次路由切换时自动更新 document.title
 *
 * @param {import('vue-router').RouteLocationNormalized} to - 目标路由对象
 * @returns {boolean|import('vue-router').RouteLocationRaw} 返回 true 放行，或返回重定向目标
 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  // 检查目标路由及其所有父级路由中是否有任何一条 requiresAuth 不为 false
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth !== false)

  const title = to.meta.title || 'AI 学习成长助手'
  document.title = `${title} - AI 学习成长助手`

  // 需要登录但未登录 → 跳转登录页，记录来源路径供登录后回跳
  if (requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // 已登录访问登录页 → 直接进入仪表盘
  if (to.name === 'Login' && userStore.isLoggedIn) {
    return { name: 'Dashboard' }
  }

  return true
})

export default router
export { asyncRoutes }
