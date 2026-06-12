<script setup>
/**
 * @file QuickEntry.vue - Dashboard 快捷入口组件
 * @description
 * 展示在 Dashboard 首页的快捷导航入口卡片，功能包括：
 *   1. 从常量配置 QUICK_ENTRIES 读取快捷入口数据（标题、路径、图标、颜色）
 *   2. 以网格布局渲染入口项，每项含图标 + 标题文字
 *   3. 点击入口项触发路由跳转并 emit('navigate') 通知父组件
 *   4. 图标通过 iconMap 映射字符串名到 Element Plus 图标组件
 *
 * 数据来源：
 *   - QUICK_ENTRIES（@/utils/constants）：定义快捷入口的 path、icon、title、color
 *   - 目前 iconMap 包含 Calendar、Reading、ChatDotRound、DataAnalysis
 *
 * @emit {string} navigate - 点击入口项时触发，参数为路由路径字符串
 */
import { useRouter } from 'vue-router'
import {
  Calendar,
  Reading,
  ChatDotRound,
  DataAnalysis,
} from '@element-plus/icons-vue'
import { QUICK_ENTRIES } from '@/utils/constants'

const emit = defineEmits([
  /** 点击入口，参数为路由路径 */
  'navigate',
])

const router = useRouter()

/**
 * 图标字符串名 → Vue 图标组件的映射表
 * QUICK_ENTRIES 中 item.icon 是字符串，通过此 map 转换为组件
 */
const iconMap = { Calendar, Reading, ChatDotRound, DataAnalysis }

/**
 * 导航到指定路径
 * 1. emit('navigate') 通知父组件
 * 2. router.push() 执行实际路由跳转
 * @param {string} path - 目标路由路径
 */
function navigate(path) {
  emit('navigate', path)
  router.push(path)
}
</script>

<template>
  <!-- 快捷入口卡片：毛玻璃效果 + hover 上浮 -->
  <el-card class="quick-entry glass-card hover-lift" shadow="never">
    <template #header>快捷入口</template>

    <!-- 入口项网格布局：响应式 auto-fill，最小列宽 120px -->
    <div class="quick-entry__grid">
      <!--
        遍历 QUICK_ENTRIES 渲染每个入口项
          - 图标使用对应 item.color + 28% 透明度作为背景色
          - hover 时图标放大 + 文字变色 + 整体上移
      -->
      <div
        v-for="item in QUICK_ENTRIES"
        :key="item.path"
        class="quick-entry__item"
        @click="navigate(item.path)"
      >
        <!-- 图标容器：背景色为 item.color 的 22% 透明度 -->
        <div
          class="quick-entry__icon"
          :style="{ background: `${item.color}22`, color: item.color }"
        >
          <el-icon :size="22"><component :is="iconMap[item.icon]" /></el-icon>
        </div>
        <!-- 入口标题 -->
        <span>{{ item.title }}</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.quick-entry__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.quick-entry__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 8px;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-normal;
  color: var(--color-text-secondary);

  &:hover {
    background: rgba(59, 130, 246, 0.12);
    color: $color-text-primary;
    transform: translateY(-4px);
  }
}

.quick-entry__icon {
  width: 48px;
  height: 48px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform $transition-fast;
}

.quick-entry__item:hover .quick-entry__icon {
  transform: scale(1.1);
}
</style>
