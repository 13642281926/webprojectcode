<script setup>
/**
 * @file CourseCard.vue - 课程展示卡片组件
 * @description
 * 用于课程列表或网格中展示单个课程信息的卡片组件。功能包括：
 *   1. 课程封面图片懒加载（LazyImage 组件，带渐变蒙层确保标签可见）
 *   2. 课程分类标签（el-tag，通过 categoryLabels 映射中文展示名）
 *   3. 课程标题、描述、讲师、课时数等基础信息
 *   4. 学习进度条（el-progress，百分比展示）
 *   5. 点击卡片触发 emit('click', course)，用于跳转课程详情
 *
 * 课程对象格式（由父组件传入 course prop）：
 *   { id, title, cover, description, progress, teacher, lessons, category }
 *
 * @props {Object} course - 课程数据对象（required）
 *
 * @emit {Object} click - 点击卡片时触发，参数为完整的 course 对象
 *
 * 注：categoryLabels 目前仅映射 frontend/cs/language 三种分类，
 *      未匹配的分类将显示原始 category 值作为兜底。
 */
import LazyImage from './LazyImage.vue'

defineProps({
  /** 课程数据对象，包含 id、title、cover、description、progress、teacher、lessons、category */
  course: { type: Object, required: true },
})

const emit = defineEmits([
  /** 点击卡片，参数为 course 对象 */
  'click',
])

/**
 * 课程分类中文标签映射
 * 将后端返回的英文分类 key 转换为前端显示的中文名称
 * 未匹配的分类将原样显示
 */
const categoryLabels = {
  frontend: '前端',
  cs: '计算机',
  language: '语言',
}
</script>

<template>
  <!-- 课程卡片容器：毛玻璃卡片 + hover 上浮效果 -->
  <el-card
    class="course-card glass-card glass-card--glow hover-lift"
    shadow="never"
    @click="emit('click', course)"
  >
    <!-- ========== 封面区域 ========== -->
    <div class="course-card__cover">
      <!-- 课程封面图片（懒加载） -->
      <LazyImage :src="course.cover" :alt="course.title" fetchpriority="low" />
      <!-- 渐变蒙层：从底部黑色渐变到透明，确保标签在任何背景图下清晰可见 -->
      <div class="course-card__overlay" />
      <!-- 课程分类标签：右上角悬浮 -->
      <el-tag size="small" class="course-card__tag">
        {{ categoryLabels[course.category] || course.category }}
      </el-tag>
    </div>

    <!-- ========== 课程信息区域 ========== -->
    <div class="course-card__body">
      <!-- 课程标题（单行溢出省略号） -->
      <h3 class="course-card__title">{{ course.title }}</h3>
      <!-- 课程描述（最多两行，超出省略号） -->
      <p class="course-card__desc">{{ course.description }}</p>
      <!-- 元信息：讲师 + 课时数 -->
      <div class="course-card__meta">
        <span>{{ course.teacher }}</span>
        <span>{{ course.lessons }} 课时</span>
      </div>
      <!-- 学习进度标签 -->
      <div class="course-card__progress">
        <span>学习进度</span>
        <span>{{ course.progress }}%</span>
      </div>
      <!-- 进度条 -->
      <el-progress :percentage="course.progress" :stroke-width="6" :show-text="false" />
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.course-card {
  cursor: pointer;
  height: 100%;
}

.course-card__cover {
  position: relative;
  height: 140px;
  overflow: hidden;
  border-radius: $radius-sm $radius-sm 0 0;
  margin: -20px -20px 12px;

  :deep(.lazy-image) {
    height: 140px;
  }

  &:hover :deep(img) {
    transform: scale(1.05);
  }
}

.course-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.55) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.course-card__tag {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1;
}

.course-card__title {
  font-size: 16px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-card__desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
  margin-bottom: 12px;
}

.course-card__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.course-card__progress {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}
</style>
