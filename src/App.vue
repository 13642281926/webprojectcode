<script setup>
/**
 * @file App.vue - Vue 3 根组件
 * @description
 * 整个应用的顶层路由出口，负责：
 *   1. 提供根级 DOM 容器（app-root），设置全屏最小高度和背景色
 *   2. 通过 <router-view> 渲染当前路由匹配的页面组件
 *   3. 使用 <transition> 包裹路由切换，实现页面淡入淡出动画（page-fade）
 *
 * 路由层级说明：
 *   - 登录页、注册页等独立页面由 router-view 直接渲染
 *   - 系统内部页面由 AppLayout 组件统一管理（侧栏 + 顶栏 + 内容区）
 *   - mode="out-in" 确保旧页面先离开，新页面再进入，避免布局抖动
 *
 * 本组件自身不接收 props/emits/slots —— 所有逻辑由路由系统驱动。
 */
</script>

<template>
  <div class="app-root">
    <!--
      路由视图插槽模式：
        - 解构出当前路由匹配的 Component
        - 用 <transition> 包裹，提供跨路由的过渡动画
        - mode="out-in"：先出后入，防止两个页面同时存在导致布局冲突
    -->
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.app-root {
  min-height: 100vh;
  background: var(--color-bg-primary, #0a0e1a);
}
</style>
