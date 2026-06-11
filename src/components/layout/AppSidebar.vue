<script setup>
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

const menuItems = computed(() =>
  getMenuItems().map((item) => ({
    ...item,
    icon: iconMap[item.icon] || Odometer,
  })),
)

function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <aside
    class="app-sidebar glass-card"
    :class="{ 'app-sidebar--collapsed': themeStore.sidebarCollapsed }"
  >
    <div class="app-sidebar__brand">
      <span class="app-sidebar__logo-mark">A</span>
      <div v-show="!themeStore.sidebarCollapsed" class="app-sidebar__brand-copy">
        <span class="gradient-text app-sidebar__logo">AI Learning</span>
        <span class="app-sidebar__title">Growth Studio</span>
      </div>
    </div>
    <nav class="app-sidebar__nav">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="app-sidebar__item"
        :class="{ 'is-active': isActive(item.path) }"
        :title="item.title"
      >
        <el-icon><component :is="item.icon" /></el-icon>
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
