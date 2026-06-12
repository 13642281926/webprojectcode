/**
 * API 接口层 — 统一导出入口
 *
 * 集中导出所有 API 模块，外部可通过 `import { xxxApi } from '@/api'` 统一引用。
 * 各 API 模块按业务领域拆分（user、course、note、studyPlan 等）。
 *
 * @module api
 */

export { default as request } from './request'
export * from './user'
export * from './dashboard'
export * from './studyPlan'
export * from './course'
export * from './ai'
export * from './analytics'
