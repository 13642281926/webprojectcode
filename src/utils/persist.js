/**
 * Pinia 状态持久化辅助函数
 *
 * 提供通用的 Pinia Store 状态 <-> localStorage 双向同步能力。
 *
 * 核心机制：
 * 1. load()：初始化时从 localStorage 恢复状态（支持全量恢复和按 paths 字段选择恢复）
 * 2. save()：在状态变化时自动将状态写入 localStorage
 * 3. watch()：通过 Vue 的 watch 监听状态变化，自动触发 save（immediate 模式）
 *
 * 支持两种状态格式：
 * - Ref 包装的状态（state.value）：适用于 Composition API Setup Store
 * - 普通响应式对象：适用于 Options API Store
 *
 * 关键参数：
 * - paths：指定需要持久化的字段名数组，为 null 时持久化整个 state
 * - immediate：是否在初始化时立即建立 watch 监听
 *
 * @module utils/persist
 */

import { watch } from 'vue'
import { getStorage, setStorage } from './storage'

/**
 * 创建状态持久化实例
 *
 * @param {import('vue').Ref|object} state - 响应式对象或 Ref（Pinia Store 的 state）
 * @param {string} key - localStorage 存储键名
 * @param {object} [options] - 配置选项
 * @param {string[]|null} [options.paths=null] - 指定需持久化的字段名数组；null 表示持久化整个 state
 * @param {boolean} [options.immediate=true] - 是否在初始化时立即建立 watch 自动持久化
 * @returns {{load: Function, save: Function}} 包含 load 和 save 方法的对象
 */
export function usePersist(state, key, options = {}) {
  const { paths = null, immediate = true } = options

  /**
   * 从 localStorage 加载并恢复状态
   * 支持三种恢复模式：
   * 1. paths 指定 + 对象缓存：按字段名逐个恢复
   * 2. Ref 状态：直接赋值 state.value
   * 3. 普通对象状态：Object.assign 合并
   */
  function load() {
    const cached = getStorage(key)
    if (!cached) return
    if (paths && typeof cached === 'object') {
      // 按 paths 列表选择性恢复指定字段
      paths.forEach((p) => {
        if (cached[p] !== undefined && state[p] !== undefined) {
          state[p] = cached[p]
        }
      })
    } else if (state.value !== undefined) {
      // Ref 包装的状态（Composition API）
      state.value = cached
    } else {
      // 普通响应式对象（Options API）
      Object.assign(state, cached)
    }
  }

  /**
   * 将当前状态保存到 localStorage
   * 与 load() 对称，支持相同的三种状态格式
   */
  function save() {
    let payload
    if (paths) {
      // 按 fields 列表提取指定字段，同时处理 Ref 和普通属性
      payload = {}
      paths.forEach((p) => {
        payload[p] = state[p]?.value ?? state[p]
      })
    } else if (state.value !== undefined) {
      // Ref：取 .value
      payload = state.value
    } else {
      // 普通对象：浅拷贝
      payload = { ...state }
    }
    setStorage(key, payload)
  }

  // 初始化时立即从 localStorage 恢复状态
  load()

  // 建立 watch 监听，状态变化时自动持久化
  if (immediate) {
    watch(
      // 监听源：按 paths 提取字段或整体 state
      () => (paths ? paths.map((p) => state[p]?.value ?? state[p]) : state.value ?? state),
      () => save(),
      { deep: true }, // deep 以检测嵌套对象内部变化
    )
  }

  return { load, save }
}
