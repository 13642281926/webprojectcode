/**
 * 防抖函数 — Lodash 再导出
 *
 * 从 lodash-es 的 _.debounce 重新导出，以支持 tree-shaking（仅打包用到的函数）。
 * lodash-es 提供 ES Module 格式，配合打包工具的 tree-shaking 可显著减小包体积。
 *
 * 使用示例：
 *   import { debounce } from '@/utils'
 *   const searchHandler = debounce(() => { ... }, 300)
 *
 * @module utils/debounce
 *
 * @see https://lodash.com/docs#debounce
 */
export { debounce } from 'lodash-es'
