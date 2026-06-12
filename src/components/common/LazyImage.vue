<script setup>
/**
 * @file LazyImage.vue - 图片懒加载组件
 * @description
 * 封装了原生 <img> 标签，提供以下增强功能：
 *   1. 加载前占位动画（shimmer 骨架屏渐变效果）
 *   2. 加载完成后渐显过渡（opacity 0→1，0.4s ease）
 *   3. 加载失败时显示暗色背景（无占位动画）
 *   4. 原生 lazy loading（loading="lazy"）+ async decode（decoding="async"）
 *   5. 支持 fetchpriority 设置资源加载优先级
 *
 * 与 v-lazy 指令的区别：
 *   - v-lazy 是全局自定义指令，拦截 src 属性实现 IntersectionObserver 懒加载
 *   - LazyImage 是组件级封装，使用原生 loading="lazy" + 加载状态管理
 *   - 两者可配合使用，也可独立使用
 *
 * @props {string} src          - 图片地址（required）
 * @props {string} alt          - 替代文本，默认 "课程封面"
 * @props {string} fit          - object-fit 属性值（CSS），默认 "cover"
 * @props {string} fetchpriority - 资源加载优先级提示（"high"/"low"/"auto"），默认 "auto"
 *
 * @emit load  - 图片加载完成时触发
 * @emit error - 图片加载失败时触发
 */
import { ref } from 'vue'

defineProps({
  /** 图片地址 */
  src: { type: String, required: true },
  /** 替代文本（无障碍 + 加载失败回退显示） */
  alt: { type: String, default: '课程封面' },
  /** object-fit CSS 属性，控制图片填充方式 */
  fit: { type: String, default: 'cover' },
  /** 资源加载优先级提示 */
  fetchpriority: { type: String, default: 'auto' },
})

const emit = defineEmits([
  /** 图片加载成功 */
  'load',
  /** 图片加载失败 */
  'error',
])

/** 图片是否已加载完成 */
const loaded = ref(false)
/** 图片是否加载失败 */
const errored = ref(false)

/**
 * 图片加载成功回调
 * 设置 loaded 状态 → 触发 opacity 渐显过渡
 * emit('load') 通知父组件
 */
function onLoad() {
  loaded.value = true
  emit('load')
}

/**
 * 图片加载失败回调
 * 设置 errored 状态 → 隐藏占位动画，显示暗色背景
 * emit('error') 通知父组件
 */
function onError() {
  errored.value = true
  emit('error')
}
</script>

<template>
  <!--
    图片容器：
      - is-loaded：图片加载完成，图片渐显
      - is-error：图片加载失败，显示暗色背景
  -->
  <div class="lazy-image" :class="{ 'is-loaded': loaded, 'is-error': errored }">
    <!--
      原生 img 标签
        - loading="lazy"：浏览器原生懒加载（进入视口前不加载）
        - decoding="async"：异步解码，不阻塞主线程
        - @load / @error：监听加载状态
    -->
    <img
      :src="src"
      :alt="alt"
      :fetchpriority="fetchpriority"
      loading="lazy"
      decoding="async"
      :style="{ objectFit: fit }"
      @load="onLoad"
      @error="onError"
    />
    <!--
      加载前占位动画（shimmer 效果）
      仅当图片未加载完成且未出错时显示
      加载完成或出错后自动隐藏
    -->
    <div v-if="!loaded && !errored" class="lazy-image__placeholder" />
  </div>
</template>

<style scoped lang="scss">
.lazy-image {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: rgba(30, 41, 59, 0.5);

  img {
    width: 100%;
    height: 100%;
    opacity: 0;
    transition: opacity 0.4s ease, transform 0.4s ease;
  }

  &.is-loaded img {
    opacity: 1;
  }

  &.is-error {
    background: rgba(30, 41, 59, 0.8);
  }
}

.lazy-image__placeholder {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(30, 41, 59, 0.4) 0%,
    rgba(59, 130, 246, 0.15) 50%,
    rgba(30, 41, 59, 0.4) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
