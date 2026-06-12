<script setup>
/**
 * @file AppHeader.vue - 顶部导航头栏组件
 * @description
 * 应用的主导航头栏，位于 AppLayout 内容区顶部，提供以下功能：
 *   1. 侧栏折叠/展开切换按钮
 *   2. 页面标题与面包屑（根据当前路由 meta.title 动态展示）
 *   3. 粒子背景开关（el-switch 控制 showParticles 状态）
 *   4. 深色/亮色主题切换按钮
 *   5. 用户头像与昵称展示，点击跳转个人中心
 *
 * 依赖的 Store：
 *   - useThemeStore：管理主题模式（isDark）、侧栏折叠（sidebarCollapsed）、粒子开关（showParticles）
 *   - useUserStore：管理当前登录用户信息（userInfo）
 *
 * @computed pageTitle - 从 route.meta.title 获取当前页面标题，兜底为 "AI 学习成长助手"
 * @computed themeIcon - 根据 isDark 返回 Sunny（白天图标）或 Moon（夜间图标）
 * @computed collapseIcon - 根据 sidebarCollapsed 返回 Expand（展开图标）或 Fold（折叠图标）
 *
 * 注：本组件无 props/emits —— 所有交互通过 Pinia Store 驱动。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sunny, Moon, Fold, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

/**
 * 根据当前路由 meta.title 动态计算页面标题
 * 兜底值："AI 学习成长助手"
 */
const pageTitle = computed(() => route.meta.title || 'AI 学习成长助手')

/**
 * 根据深色/亮色模式切换图标
 *   - 深色模式 → 显示 Sunny（提示可切换亮色）
 *   - 亮色模式 → 显示 Moon（提示可切换深色）
 */
const themeIcon = computed(() => (themeStore.isDark ? Sunny : Moon))

/**
 * 根据侧栏折叠状态切换按钮图标
 *   - 折叠状态 → 显示 Expand（提示可展开）
 *   - 展开状态 → 显示 Fold（提示可折叠）
 */
const collapseIcon = computed(() => (themeStore.sidebarCollapsed ? Expand : Fold))

/**
 * 跳转到个人中心页面（路由名称为 'Profile'）
 */
function goProfile() {
  router.push({ name: 'Profile' })
}
</script>

<template>
  <!-- 顶部头栏：毛玻璃卡片 + 底部光晕效果 -->
  <header class="app-header glass-card glass-card--glow">
    <!-- ========== 左侧区域：折叠按钮 + 品牌标识 + 页面标题 ========== -->
    <div class="app-header__left">
      <!-- 侧栏折叠/展开切换 -->
      <el-button
        :icon="collapseIcon"
        circle
        text
        :aria-label="themeStore.sidebarCollapsed ? '展开侧栏' : '折叠侧栏'"
        @click="themeStore.toggleSidebar"
      />
      <!-- 品牌标识缩写 "AI" -->
      <div class="app-header__brand-mark">AI</div>
      <!-- 标题与面包屑 -->
      <div>
        <h1 class="app-header__title gradient-text">{{ pageTitle }}</h1>
        <p class="app-header__breadcrumb">AI Learning Growth Studio / {{ pageTitle }}</p>
      </div>
    </div>

    <!-- ========== 右侧区域：粒子开关 + 主题切换 + 用户头像 ========== -->
    <div class="app-header__actions">
      <!-- 粒子背景开关：inline-prompt 模式显示文字 -->
      <el-tooltip :content="themeStore.showParticles ? '关闭粒子' : '开启粒子'" placement="bottom">
        <el-switch
          v-model="themeStore.showParticles"
          inline-prompt
          active-text="粒子"
          inactive-text="粒子"
        />
      </el-tooltip>

      <!-- 深色/亮色主题切换按钮 -->
      <el-button
        :icon="themeIcon"
        circle
        text
        :aria-label="themeStore.isDark ? '切换亮色模式' : '切换深色模式'"
        @click="themeStore.toggleTheme"
      />

      <!-- 用户头像与昵称：点击跳转个人中心 -->
      <div class="app-header__user" @click="goProfile">
        <el-avatar
          :size="36"
          :src="userStore.userInfo.avatar"
          :alt="userStore.userInfo.nickname + '的头像'"
        >
          <!-- 头像加载失败或无头像时显示昵称首字，兜底为 "学" -->
          {{ userStore.userInfo.nickname?.[0] || '学' }}
        </el-avatar>
        <span class="app-header__name">{{ userStore.userInfo.nickname }}</span>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $header-height;
  margin: 16px 16px 0;
  padding: 0 20px;
  border-radius: $radius-md;
}

.app-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #06b6d4);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.app-header__title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}

.app-header__breadcrumb {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-header__user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: $radius-sm;
  transition: background $transition-fast;

  &:hover {
    background: rgba(59, 130, 246, 0.12);
  }
}

.app-header__name {
  color: var(--color-text-secondary);
  font-size: 14px;
}
</style>
