/**
 * 学习资源管理 Store
 *
 * 管理学习资源的列表查询、详情查看、添加（含文件上传）、删除操作和分类列表。
 * 支持按分类、类型、关键词筛选资源，每次增删操作后自动刷新列表。
 *
 * @module stores/resource
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getResourceListApi,
  getResourceDetailApi,
  createResourceApi,
  uploadResourceApi,
  deleteResourceApi,
  getResourceCategoriesApi,
} from '@/api/resource'

/**
 * 学习资源管理 Store
 */
export const useResourceStore = defineStore('resource', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<Array>} 资源列表（当前页） */
  const resources = ref([])
  /** @type {import('vue').Ref<Array>} 资源分类列表 */
  const categories = ref([])
  /** @type {import('vue').Ref<number>} 资源总数（用于分页） */
  const total = ref(0)
  /** @type {import('vue').Ref<boolean>} 列表加载状态 */
  const loading = ref(false)
  /** @type {import('vue').Ref<object>} 列表查询条件（分页、分类、关键词、类型） */
  const query = ref({ page: 1, pageSize: 12, category: '', keyword: '', type: '' })

  // ==================== 操作方法 ====================

  /**
   * 获取资源列表（支持分页和筛选）
   * @param {object} [params={}] - 查询参数，会与当前 query 合并
   * @param {number} [params.page] - 页码
   * @param {number} [params.pageSize] - 每页条数（默认 12）
   * @param {string} [params.category] - 分类筛选
   * @param {string} [params.keyword] - 关键词搜索
   * @param {string} [params.type] - 资源类型筛选
   * @returns {Promise<void>}
   */
  async function fetchResources(params = {}) {
    loading.value = true
    query.value = { ...query.value, ...params }
    try {
      const res = await getResourceListApi(query.value)
      resources.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取单个资源详情
   * @param {string|number} id - 资源 ID
   * @returns {Promise<object>} 资源详情数据（包含文件信息等）
   */
  async function fetchResourceDetail(id) {
    return await getResourceDetailApi(id)
  }

  /**
   * 获取资源分类列表
   * @returns {Promise<void>}
   */
  async function fetchCategories() {
    const res = await getResourceCategoriesApi()
    categories.value = res.data
  }

  /**
   * 添加资源（JSON 数据方式），成功后自动刷新列表
   * @param {object} data - 资源数据（名称、分类、描述等）
   * @returns {Promise<object>} 新创建的资源数据
   */
  async function addResource(data) {
    const res = await createResourceApi(data)
    await fetchResources()
    return res.data
  }

  /**
   * 上传资源文件（multipart/form-data），成功后自动刷新列表
   * @param {FormData} formData - 包含文件的表单数据
   * @returns {Promise<object>} 上传后的资源数据
   */
  async function uploadResource(formData) {
    const res = await uploadResourceApi(formData)
    await fetchResources()
    return res.data
  }

  /**
   * 删除资源，成功后自动刷新列表
   * @param {string|number} id - 资源 ID
   * @returns {Promise<void>}
   */
  async function removeResource(id) {
    await deleteResourceApi(id)
    await fetchResources()
  }

  return {
    // 状态
    resources,
    categories,
    total,
    loading,
    query,
    // 方法
    fetchResources,
    fetchResourceDetail,
    fetchCategories,
    addResource,
    uploadResource,
    removeResource,
  }
})
