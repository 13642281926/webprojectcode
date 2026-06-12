/**
 * 路由与侧边栏菜单统一配置
 *
 * 本模块负责：
 * 1. 定义所有受登录保护的布局子路由（layoutRoutes）
 * 2. 提供侧边栏菜单数据派生函数（getMenuItems）
 * 3. 预留动态路由扩展入口（asyncRoutes），供后续按角色/权限动态注入路由
 *
 * 技术要点：
 * - 使用动态 import() 实现路由懒加载（按页面拆分为独立 chunk，减少首屏体积）
 * - meta.icon 对应 @element-plus/icons-vue 组件名，由侧边栏组件动态渲染
 * - 所有子路由默认继承父路由的 requiresAuth 鉴权规则
 *
 * @module router/routes
 */

/**
 * 需登录的布局子路由
 * 每个路由会被注入到 AppLayout 的 <router-view> 中
 * meta.title — 页面标题（同时用于侧边栏菜单文字）
 * meta.icon — Element Plus Icons 组件名（用于侧边栏图标）
 */
export const layoutRoutes = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    /** 仪表盘首页 — 概览学习数据概览 */
    component: () => import(/* webpackChunkName: "dashboard" */ '@/views/dashboard/DashboardView.vue'),
    meta: { title: '首页', icon: 'Odometer', requiresAuth: true },
  },
  {
    path: 'study-plan',
    name: 'StudyPlan',
    /** 学习计划管理 — 创建、编辑、跟踪学习计划进度 */
    component: () => import(/* webpackChunkName: "study-plan" */ '@/views/study-plan/StudyPlanView.vue'),
    meta: { title: '学习计划', icon: 'Calendar', requiresAuth: true },
  },
  {
    path: 'pomodoro',
    name: 'Pomodoro',
    /** 番茄专注 — 基于番茄工作法的专注计时器 */
    component: () => import(/* webpackChunkName: "pomodoro" */ '@/views/pomodoro/PomodoroView.vue'),
    meta: { title: '番茄专注', icon: 'AlarmClock', requiresAuth: true },
  },
  {
    path: 'course',
    name: 'Course',
    /** 课程管理 — 课程 CRUD、分类筛选 */
    component: () => import(/* webpackChunkName: "course" */ '@/views/course/CourseView.vue'),
    meta: { title: '课程管理', icon: 'Reading', requiresAuth: true },
  },
  {
    path: 'note',
    name: 'Note',
    /** 笔记管理 — Markdown 笔记撰写、分类、搜索 */
    component: () => import(/* webpackChunkName: "note" */ '@/views/note/NoteView.vue'),
    meta: { title: '笔记管理', icon: 'Document', requiresAuth: true },
  },
  {
    path: 'wrong-question',
    name: 'WrongQuestion',
    /** 错题本 — 错题记录、复习追踪、掌握标记 */
    component: () => import(/* webpackChunkName: "wrong-question" */ '@/views/wrongQuestion/WrongQuestionView.vue'),
    meta: { title: '错题本', icon: 'Warning', requiresAuth: true },
  },
  {
    path: 'resource',
    name: 'Resource',
    /** 学习资源 — 文件上传、下载、分类管理 */
    component: () => import(/* webpackChunkName: "resource" */ '@/views/resource/ResourceView.vue'),
    meta: { title: '学习资源', icon: 'FolderOpened', requiresAuth: true },
  },
  {
    path: 'achievement',
    name: 'Achievement',
    /** 成就系统 — 成就展示、进度追踪、解锁记录 */
    component: () => import(/* webpackChunkName: "achievement" */ '@/views/achievement/AchievementView.vue'),
    meta: { title: '成就系统', icon: 'Trophy', requiresAuth: true },
  },
  {
    path: 'ai-assistant',
    name: 'AiAssistant',
    /** AI 助手 — 智能问答、学习建议、快速提问 */
    component: () => import(/* webpackChunkName: "ai-assistant" */ '@/views/ai-assistant/AiAssistantView.vue'),
    meta: { title: 'AI 助手', icon: 'ChatDotRound', requiresAuth: true },
  },
  {
    path: 'analytics',
    name: 'Analytics',
    /** 数据分析 — 学习数据可视化（ECharts 图表） */
    component: () => import(/* webpackChunkName: "analytics" */ '@/views/analytics/AnalyticsView.vue'),
    meta: { title: '数据分析', icon: 'DataAnalysis', requiresAuth: true },
  },
  {
    path: 'profile',
    name: 'Profile',
    /** 个人中心 — 用户资料查看与编辑 */
    component: () => import(/* webpackChunkName: "profile" */ '@/views/profile/ProfileView.vue'),
    meta: { title: '个人中心', icon: 'User', requiresAuth: true },
  },
]

/**
 * 从 layoutRoutes 派生侧边栏菜单数据
 * 将路由配置映射为菜单组件所需的 { path, name, title, icon } 格式
 * @returns {Array<{path: string, name: string, title: string, icon: string}>} 菜单项数组
 */
export function getMenuItems() {
  return layoutRoutes.map((route) => ({
    path: `/${route.path}`,
    name: route.name,
    title: route.meta.title,
    icon: route.meta.icon,
  }))
}

/**
 * 动态路由扩展预留
 * 如需按角色/权限动态注入路由，可在导航守卫中通过 router.addRoute 将此数组中的路由配置添加到路由实例
 * @type {Array<import('vue-router').RouteRecordRaw>}
 */
export const asyncRoutes = []
