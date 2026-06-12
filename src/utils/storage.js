/**
 * localStorage 安全读写封装
 *
 * 对原生 localStorage API 的封装，提供以下增强：
 * 1. 自动 JSON 序列化/反序列化，支持对象、数组等复杂类型
 * 2. try-catch 容错，避免隐私模式或存储满时抛出异常导致页面崩溃
 * 3. 读操作支持默认值，key 不存在时返回 defaultValue
 * 4. 写/删操作失败时 console.warn 告警但不中断程序执行
 *
 * @module utils/storage
 */

/**
 * 从 localStorage 读取并解析 JSON 数据
 * @param {string} key - 存储键名
 * @param {*} [defaultValue=null] - 键不存在或解析失败时的默认返回值
 * @returns {*} 解析后的数据，或 defaultValue
 */
export function getStorage(key, defaultValue = null) {
  try {
    const raw = localStorage.getItem(key)
    if (raw === null) return defaultValue
    return JSON.parse(raw)
  } catch {
    // JSON 解析失败（数据损坏等）时返回默认值
    return defaultValue
  }
}

/**
 * 将数据 JSON 序列化后写入 localStorage
 * @param {string} key - 存储键名
 * @param {*} value - 要存储的数据（自动 JSON 序列化）
 */
export function setStorage(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    // 存储失败（如隐私模式、配额超限）时仅告警，不中断程序
    console.warn('[storage] 写入失败:', error)
  }
}

/**
 * 从 localStorage 删除指定键
 * @param {string} key - 要删除的键名
 */
export function removeStorage(key) {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.warn('[storage] 删除失败:', error)
  }
}
