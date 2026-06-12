/**
 * @file lazy.js - Vue 3 自定义指令 v-lazy
 * @description
 * 图片视口懒加载指令（考核点：自定义指令 + IntersectionObserver）
 *
 * 用法：
 *   <img v-lazy src="真实图片地址" alt="..." />
 *   或通过 binding value：
 *   <img v-lazy="'真实图片地址'" alt="..." />
 *
 * 工作流程：
 *   1. mounted 阶段：
 *      a. 读取真实 src（binding.value 优先级高于 el.getAttribute('src')）
 *      b. 将真实地址暂存到 data-src 属性
 *      c. 清空 src 属性（避免浏览器立即发起网络请求）
 *      d. 设置 opacity: 0 + transition（为加载完成后的渐显效果做准备）
 *      e. 通过 IntersectionObserver 监听元素是否进入视口
 *      f. 注册 load / error 事件监听器
 *
 *   2. 当图片进入视口（rootMargin: 100px，提前 100px 触发）：
 *      a. 从 data-src 读取真实地址并设置 src 属性 → 触发浏览器加载
 *      b. 移除 data-src 属性
 *      c. 取消对该元素的观察（unobserve）
 *
 *   3. 图片加载完成（load 事件）：
 *      设置 opacity: 1 → CSS transition 产生 0.4s 渐显动画
 *
 *   4. 图片加载失败（error 事件）：
 *      同样设置 opacity: 1 → 不显示空白，交给 alt 属性兜底
 *
 *   5. unmounted 阶段：取消对该元素的观察，防止内存泄漏
 *
 * IntersectionObserver 设计要点：
 *   - 单例模式：全局共享一个 observer 实例，避免重复创建
 *   - rootMargin: '100px'：提前 100px 开始加载，用户滚动到附近时图片已就绪
 *   - 回调中检查 isIntersecting：确保仅在元素进入视口时触发加载
 */

// ============================================================
// IntersectionObserver 单例
// ============================================================
/** 全局共享的 IntersectionObserver 实例（懒初始化，仅创建一次） */
let observer = null

/**
 * 获取或创建 IntersectionObserver 单例
 *
 * 单例模式的价值：
 *   - 避免每个使用 v-lazy 的元素都创建一个 observer
 *   - 所有元素共享一个 observer 实例，统一管理
 *
 * IntersectionObserver 配置：
 *   - 回调：遍历 entries，仅处理 isIntersecting 的 entry
 *     （即元素进入视口或 rootMargin 范围内的 entry）
 *   - rootMargin: '100px'：视口向外扩展 100px，提前触发加载
 *
 * @returns {IntersectionObserver}
 */
function getObserver() {
  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          // 仅处理进入视口的元素
          if (!entry.isIntersecting) return
          const img = entry.target
          // 将 data-src 赋值给 src 触发真实加载
          const realSrc = img.getAttribute('data-src')
          if (realSrc) {
            img.src = realSrc
            img.removeAttribute('data-src')
          }
          // 加载触发后立即取消观察，避免重复处理
          observer.unobserve(img)
        })
      },
      // rootMargin: 100px → 提前 100px 触发加载，提升用户体验
      { rootMargin: '100px' },
    )
  }
  return observer
}

// ============================================================
// v-lazy 指令定义对象
// ============================================================
const vLazy = {
  /**
   * mounted 生命周期钩子
   * 元素挂载到 DOM 时执行，完成以下初始化操作：
   *
   * 步骤 1：提取真实图片地址
   *   binding.value（v-lazy="url" 传值方式）优先级高于 el.getAttribute('src')
   *
   * 步骤 2：暂存真实地址
   *   将真实地址保存到 data-src 自定义属性，并清空 src 属性
   *   清空 src 是关键：浏览器看到无 src 的 img 不会发起网络请求
   *
   * 步骤 3：设置渐显样式
   *   opacity: 0（初始隐藏）+ transition: opacity 0.4s ease（渐显过渡）
   *
   * 步骤 4：开始 IntersectionObserver 监听
   *   注册到全局 observer 实例，等待元素进入视口
   *
   * 步骤 5：注册 load / error 事件
   *   load 成功：opacity → 1（渐显）
   *   error 失败：opacity → 1（不隐藏，显示 alt 文本或浏览器默认占位）
   *
   * @param {HTMLImageElement} el - 被绑定的 img 元素
   * @param {Object} binding - 指令绑定对象，binding.value 为传递的值
   */
  mounted(el, binding) {
    // 步骤 1 & 2：提取真实 src 并暂存到 data-src，清空 src 避免立即加载
    const src = binding.value || el.getAttribute('src')
    if (src) {
      el.setAttribute('data-src', src)
      el.removeAttribute('src')
    }
    // 步骤 3：添加渐显样式（初始隐藏，加载完成后淡入）
    el.style.opacity = '0'
    el.style.transition = 'opacity 0.4s ease'
    // 步骤 4：开始观察
    getObserver().observe(el)
    // 步骤 5：加载完成回调 → 渐显
    el.addEventListener('load', () => {
      el.style.opacity = '1'
    })
    // 加载失败回调 → 同样显示（不隐藏破损图，让 alt 或浏览器默认占位可见）
    el.addEventListener('error', () => {
      el.style.opacity = '1'
    })
  },

  /**
   * unmounted 生命周期钩子
   * 组件卸载或元素从 DOM 中移除时执行
   * 取消对该元素的 IntersectionObserver 监听，防止内存泄漏
   *
   * @param {HTMLImageElement} el - 被绑定的 img 元素
   */
  unmounted(el) {
    observer?.unobserve(el)
  },
}

export default vLazy
