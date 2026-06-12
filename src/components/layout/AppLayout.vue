<script setup>
/**
 * @file AppLayout.vue - 主应用布局组件（Shell Layout）
 * @description
 * 整个系统的页面外壳，管理全局布局结构。功能包括：
 *   1. 粒子背景层（ParticleBackground，可开关）
 *   2. 科技感网格底层叠加（tech-grid-bg）
 *   3. 左侧：AppSidebar 侧边导航菜单
 *   4. 顶部：AppHeader 顶部导航头栏
 *   5. 主内容区：<router-view> + KeepAlive 页面缓存
 *   6. 页面切换过渡动画（fade-slide）
 *
 * 考核点：provide / inject 跨级通信
 *   - 通过 provide('themeConfig') 向深层子组件注入响应式主题配置
 *   - 子组件（如 ChartCard.vue）通过 inject('themeConfig') 获取，无需层层传递 props
 *
 * 考核点：KeepAlive 页面缓存
 *   - 使用 <keep-alive> 包裹 <router-view>，避免重复挂载与数据请求
 *   - key="route.path" 确保同路由不同参数页面独立缓存
 *
 * @provide {ComputedRef<{isDark: boolean}>} themeConfig - 响应式主题配置对象，
 *   向下注入给 ChartCard 等深层组件，用于 ECharts 主题切换联动
 *
 * 注：本组件无 props/emits/slots —— 所有交互通过 Pinia Store 和路由系统驱动。
 */
import { computed, provide } from 'vue'
import { useThemeStore } from '@/stores/theme'
import ParticleBackground from '@/components/common/ParticleBackground.vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

const themeStore = useThemeStore()

/**
 * 通过 provide 向深层组件注入响应式主题配置（考核点：provide/inject 跨级通信）
 * 使用 computed() 包裹确保响应式传递 —— 当 themeStore.isDark 变化时，
 * 所有 inject('themeConfig') 的子孙组件会自动感知并响应。
 *
 * 消费方示例：ChartCard.vue 中通过 `inject('themeConfig')` 获取，
 * 并在主题切换时销毁旧图表实例，以新主题重新初始化 ECharts。
 */
provide(
  'themeConfig',
  computed(() => ({
    isDark: themeStore.isDark,
  })),
)

/**
 * 动态计算布局容器的 class
 * sidebarCollapsed 为 true 时添加 app-layout--collapsed，
 * 通过 CSS 变量 --sidebar-width 切换侧栏宽度。
 */
const layoutClass = computed(() => ({
  'app-layout': true,
  'app-layout--collapsed': themeStore.sidebarCollapsed,
}))
</script>

<template>
  <div :class="layoutClass">
    <!--
      第 1 层：Canvas 粒子背景（可开关）
      v-if 确保关闭时销毁 Canvas 节点，释放 GPU 资源
    -->
    <ParticleBackground v-if="themeStore.showParticles" class="app-layout__particles" />

    <!--
      第 2 层：科技感网格底层叠加（纯 CSS，pointer-events: none 不阻挡交互）
    -->
    <div class="app-layout__overlay tech-grid-bg" />

    <!--
      第 3 层：主体内容区（z-index: 1，位于背景之上）
      采用 flex 布局：侧栏 + 主内容区（头栏 + 页面）
    -->
    <div class="app-layout__body">
      <!-- 左侧导航菜单 -->
      <AppSidebar />

      <!-- 右侧主体：头栏 + 主内容区 -->
      <div class="app-layout__main-wrap">
        <!-- 顶部导航头栏 -->
        <AppHeader />

        <!-- 主内容区：路由视图 + 页面缓存 + 过渡动画 -->
        <main class="app-layout__main">
          <!--
            router-view 插槽模式：
              - 解构出 Component 和 route
              - <transition> 提供 fade-slide 过渡动画
              - <keep-alive> 缓存已访问页面，避免重复挂载与数据请求（考核点：KeepAlive）
              - :key="route.path" 确保不同路径的页面独立缓存
          -->
          <router-view v-slot="{ Component, route }">
            <transition name="fade-slide" mode="out-in">
              <keep-alive>
                <component :is="Component" :key="route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-layout {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.app-layout__particles {
  z-index: 0;
}

.app-layout__overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.6;
}

.app-layout__body {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100vh;
}

.app-layout__main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-layout__main {
  flex: 1;
  overflow: auto;
  @include scrollbar-dark;
}

.app-layout--collapsed {
  --sidebar-width: var(--sidebar-collapsed-width);
}

.app-layout__floating-tool {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 100;
  pointer-events: auto;
}
</style>
