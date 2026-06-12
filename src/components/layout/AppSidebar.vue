<script setup>
/**
 * @file AppSidebar.vue - 侧边导航菜单组件
 * @description
 * 应用左侧导航菜单，提供以下功能：
 *   1. 品牌标识区域（logo 缩写 "A" + 品牌名称 "AI Learning / Growth Studio"）
 *   2. 根据路由配置动态生成导航菜单项（调用 getMenuItems()）
 *   3. 路由激活状态高亮（isActive 判断当前路径是否匹配菜单项）
 *   4. 侧栏折叠/展开动画切换（通过 useThemeStore.sidebarCollapsed 控制）
 *   5. Element Plus 图标动态映射（iconMap 将字符串名称映射为图标组件）
 *
 * 依赖的 Store：
 *   - useThemeStore：管理侧栏折叠状态（sidebarCollapsed）
 *
 * 导航菜单数据来源：
 *   - getMenuItems()（@/router/routes）：从路由配置中提取 meta 信息生成菜单项
 *   - 每个菜单项包含：path（路由路径）、icon（图标字符串名）、title（显示名称）
 *
 * @computed menuItems - 将路由菜单项映射为带 Vue 组件引用的菜单数据
 *
 * 注：本组件无 props/emits —— 导航通过 <router-link> 直接驱动路由跳转。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Odometer,
  Calendar,
  AlarmClock,
  Reading,
  ChatDotRound,
  DataAnalysis,
  User,
  Document,
  Warning,
  FolderOpened,
  Trophy,
} from '@element-plus/icons-vue'
import { getMenuItems } from '@/router/routes'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const themeStore = useThemeStore()

/**
 * 图标字符串名 → Vue 图标组件的映射表
 * getMenuItems() 返回的 item.icon 是字符串（如 "Calendar"），
 * 通过此 map 转换为 Element Plus 图标组件供 <component :is> 渲染。
 * 未知图标名兜底显示 Odometer。
 */
const iconMap = {
  Odometer,
  Calendar,
  AlarmClock,
  Reading,
  ChatDotRound,
  DataAnalysis,
  User,
  Document,
  Warning,
  FolderOpened,
  Trophy,
}

/**
 * 计算属性：从路由配置中获取菜单项，并将字符串图标名映射为 Vue 组件
 * @returns {Array<{path: string, icon: Component, title: string}>}
 */
const menuItems = computed(() =>
  getMenuItems().map((item) => ({
    ...item,
    icon: iconMap[item.icon] || Odometer,
  })),
)

/**
 * 判断当前路由是否匹配某个菜单项路径
 * 匹配规则：完全匹配或以 "path/" 开头（子路由也视为激活）
 * @param {string} path - 菜单项的路由路径
 * @returns {boolean}
 */
function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <!-- 侧边导航栏：毛玻璃卡片，支持折叠/展开切换 -->
  <aside
    class="app-sidebar glass-card"
    :class="{ 'app-sidebar--collapsed': themeStore.sidebarCollapsed }"
  >
    <!-- ========== 品牌标识区域 ========== -->
    <div class="app-sidebar__brand">
      <!-- Logo 缩写 "A" -->
      <span class="app-sidebar__logo-mark">A</span>
      <!-- 品牌名称：折叠状态下隐藏（v-show 保留 DOM，避免回流开销） -->
      <div v-show="!themeStore.sidebarCollapsed" class="app-sidebar__brand-copy">
        <span class="gradient-text app-sidebar__logo">AI Learning</span>
        <span class="app-sidebar__title">Growth Studio</span>
      </div>
    </div>

    <!-- ========== 导航菜单区域 ========== -->
    <nav class="app-sidebar__nav">
      <!--
        遍历 menuItems 生成导航链接
          - router-link：Vue Router 的声明式导航组件
          - is-active class：当前激活的菜单项高亮（左侧蓝色竖条 + 半透明背景）
          - title 属性：折叠状态下 hover 显示提示文字
      -->
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="app-sidebar__item"
        :class="{ 'is-active': isActive(item.path) }"
        :title="item.title"
      >
        <!-- 图标：使用 <component :is> 动态渲染 Element Plus 图标 -->
        <el-icon><component :is="item.icon" /></el-icon>
        <!-- 文字标签：折叠状态下隐藏 -->
        <span v-show="!themeStore.sidebarCollapsed">{{ item.title }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped lang="scss">
.app-sidebar {
  width: var(--sidebar-width, #{$sidebar-width});
  margin: 16px 0 16px 16px;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  border-radius: $radius-lg;
  transition: width $transition-normal;
  flex-shrink: 0;
}

.app-sidebar--collapsed {
  width: var(--sidebar-collapsed-width, #{$sidebar-collapsed-width});

  .app-sidebar__item {
    justify-content: center;
    padding: 12px;
  }
}

.app-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 24px;
  font-weight: 700;
}

.app-sidebar__logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #06b6d4);
  color: #fff;
  font-size: 18px;
  box-shadow: 0 12px 30px rgba(99, 102, 241, 0.28);
}

.app-sidebar__brand-copy {
  display: flex;
  flex-direction: column;
}

.app-sidebar__logo {
  font-size: 18px;
}

.app-sidebar__title {
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.app-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.app-sidebar__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: $radius-sm;
  color: var(--color-text-secondary);
  transition: all $transition-fast;

  &:hover {
    color: $color-text-primary;
    background: rgba(59, 130, 246, 0.12);
  }

  &.is-active {
    color: $color-text-primary;
    background: rgba(59, 130, 246, 0.2);
    box-shadow: inset 3px 0 0 $color-accent;
  }
}
</style>
