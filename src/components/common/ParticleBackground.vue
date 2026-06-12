<script setup>
/**
 * @file ParticleBackground.vue - Canvas 粒子动画背景组件
 * @description
 * 基于 Canvas 2D 的动态粒子背景，提供科技感视觉效果。核心功能：
 *   1. 随机初始化 N 个粒子，每个粒子拥有位置 (x, y)、速度 (vx, vy)、半径 (r)
 *   2. 每帧通过 requestAnimationFrame 驱动粒子运动 + 边界碰撞反弹
 *   3. 可选连线效果：距离 < 120px 的粒子对之间绘制半透明连线，
 *      透明度随距离增大而递减（近亮远暗），形成网状脉络效果
 *   4. 跟随容器尺寸自动响应 resize，重新初始化粒子分布
 *   5. 粒子数量可配置，count 变化时自动重新初始化粒子数组
 *
 * 动画循环说明：
 *   - 使用 requestAnimationFrame 递归调用 draw()，与浏览器刷新率同步
 *   - 每帧执行：清屏 → 更新粒子位置 → 绘制粒子 → 绘制连线 → 请求下一帧
 *   - 组件卸载时通过 cancelAnimationFrame 停止循环，防止内存泄漏
 *
 * @props {number}  count     - 粒子数量，默认 60。越大 GPU 开销越高
 * @props {string}  color     - 粒子颜色（RGB 值，不含 "rgb()" 包裹），默认 "59, 130, 246"（蓝色）
 * @props {boolean} linkLines - 是否启用粒子间连线，默认 true
 * @props {number}  opacity   - 粒子透明度 0~1，默认 0.18（降低默认值减少视觉噪点）
 *
 * 注：Canvas 本身设置 pointer-events: none，不阻挡下层交互。
 * 注：aria-hidden="true" 告知屏幕阅读器忽略此纯装饰性元素。
 */
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps({
  /** 粒子数量 */
  count: { type: Number, default: 60 },
  /** 粒子颜色（RGB 值，例如 "59, 130, 246"，用于 rgba() 拼接） */
  color: { type: String, default: '59, 130, 246' },
  /** 是否启用连线 */
  linkLines: { type: Boolean, default: true },
  /** 透明度 0-1（降低默认值减少视觉噪点） */
  opacity: { type: Number, default: 0.18 },
})

/** Canvas DOM 元素引用 */
const canvasRef = ref(null)
/** requestAnimationFrame 返回的动画帧 ID，用于取消循环 */
let animationId = null
/** 粒子数组 */
let particles = []
/** Canvas 当前宽度 */
let width = 0
/** Canvas 当前高度 */
let height = 0

/**
 * 初始化粒子数组
 * 每个粒子对象包含：
 *   - x, y：随机位置（在 Canvas 区域内均匀分布）
 *   - vx, vy：随机速度（范围 [-0.3, 0.3]，低速漂浮效果）
 *   - r：随机半径（范围 [1, 3]）
 *
 * 粒子数量由 props.count 决定，count 变化时通过 watch 自动重新调用。
 */
function initParticles() {
  particles = Array.from({ length: props.count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.6,
    vy: (Math.random() - 0.5) * 0.6,
    r: Math.random() * 2 + 1,
  }))
}

/**
 * 核心动画绘制函数（递归调用 requestAnimationFrame）
 *
 * 每帧执行 4 个步骤：
 *   1. 清屏：clearRect 清除上一帧画面
 *   2. 更新粒子位置：位移 + 边界碰撞检测（越界则速度反向）
 *   3. 绘制粒子：每个粒子绘制为圆形填充，颜色由 props.color + props.opacity 决定
 *   4. 绘制连线（可选）：O(n²) 遍历所有粒子对，距离 < 120px 的绘制连线，
 *      透明度 = 0.15 * (1 - dist/120)，即越近越亮
 *
 * @param {CanvasRenderingContext2D} ctx - Canvas 2D 上下文
 */
function draw(ctx) {
  // 步骤 1：清屏
  ctx.clearRect(0, 0, width, height)

  // 步骤 2 & 3：更新并绘制每个粒子
  for (const p of particles) {
    // 位移更新
    p.x += p.vx
    p.y += p.vy
    // 边界碰撞检测：越界时速度反向，产生反弹效果
    if (p.x < 0 || p.x > width) p.vx *= -1
    if (p.y < 0 || p.y > height) p.vy *= -1

    // 绘制圆形粒子
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${props.color}, ${props.opacity})`
    ctx.fill()
  }

  // 步骤 4：绘制粒子间连线（如果启用）
  if (props.linkLines) {
    // 双循环 O(n²) 遍历所有粒子对（无重复，j 从 i+1 开始）
    for (let i = 0; i < particles.length; i += 1) {
      for (let j = i + 1; j < particles.length; j += 1) {
        const a = particles[i]
        const b = particles[j]
        // Math.hypot：计算欧几里得距离 sqrt((x1-x2)² + (y1-y2)²)
        const dist = Math.hypot(a.x - b.x, a.y - b.y)
        // 仅当距离 < 120px 时绘制连线（减少不必要的渲染开销）
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          // 连线透明度与距离成反比：越近越亮，越远越暗
          ctx.strokeStyle = `rgba(${props.color}, ${0.15 * (1 - dist / 120)})`
          ctx.stroke()
        }
      }
    }
  }

  // 请求下一帧动画（递归调用，形成持续循环）
  animationId = requestAnimationFrame(() => draw(ctx))
}

/**
 * 响应容器尺寸变化，重新设置 Canvas 尺寸并重新初始化粒子
 * 从父元素获取实际渲染尺寸，确保 Canvas 像素比与 CSS 显示尺寸一致
 */
function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  const parent = canvas.parentElement
  // 优先取父元素尺寸，兜底取视口尺寸
  width = parent?.clientWidth || window.innerWidth
  height = parent?.clientHeight || window.innerHeight
  // Canvas 绘制尺寸必须同时设置，否则会出现模糊或比例失调
  canvas.width = width
  canvas.height = height
  // 尺寸变化后需要重新分布粒子位置
  initParticles()
}

/**
 * 启动粒子动画
 * 先 resize 适配容器，再获取 2D 上下文并启动 draw 递归循环。
 * 如已有正在运行的动画帧，先取消旧帧再启动新帧（防止多个循环叠加）。
 */
function start() {
  const canvas = canvasRef.value
  if (!canvas) return
  resize()
  const ctx = canvas.getContext('2d')
  if (animationId) cancelAnimationFrame(animationId)
  draw(ctx)
}

/**
 * 停止粒子动画
 * 取消 requestAnimationFrame 循环，释放 GPU 渲染资源。
 * 通常在组件卸载时调用。
 */
function stop() {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

// ============================================================
// 生命周期钩子
// ============================================================

onMounted(() => {
  // 组件挂载后启动动画并监听窗口 resize
  start()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  // 组件卸载时停止动画并移除 resize 监听，防止内存泄漏
  stop()
  window.removeEventListener('resize', resize)
})

// ============================================================
// 监听器
// ============================================================

/**
 * 当粒子数量变化时，重新初始化粒子数组
 * 不需要重启动画循环（draw 仍在运行，会自动读取新的 particles 数组）
 */
watch(
  () => props.count,
  () => {
    initParticles()
  },
)
</script>

<template>
  <canvas ref="canvasRef" class="particle-bg" aria-hidden="true" />
</template>

<style scoped>
.particle-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
