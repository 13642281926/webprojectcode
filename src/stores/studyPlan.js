/**
 * 学习计划管理 Store
 *
 * 管理学习计划的 CRUD 操作、分页查询和状态流转（pending -> doing -> done）。
 * 支持按优先级、状态、关键词筛选，并提供待完成/已完成计数等派生数据。
 *
 * 状态流转规则：
 * - pending（待开始） -> doing（进行中）
 * - doing（进行中） -> done（已完成）
 * - done（已完成） -> pending（重新开始）
 *
 * @module stores/studyPlan
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getStudyPlanListApi,
  createStudyPlanApi,
  updateStudyPlanApi,
  deleteStudyPlanApi,
} from '@/api/studyPlan'

/**
 * 学习计划状态 Store
 */
export const useStudyPlanStore = defineStore('studyPlan', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<Array>} 学习计划列表（当前页） */
  const plans = ref([])
  /** @type {import('vue').Ref<number>} 计划总数（用于分页） */
  const total = ref(0)
  /** @type {import('vue').Ref<boolean>} 列表加载状态 */
  const loading = ref(false)
  /** @type {import('vue').Ref<object>} 列表查询条件（分页、优先级、状态、关键词） */
  const query = ref({ page: 1, pageSize: 10, priority: '', status: '', keyword: '' })

  // ==================== 计算属性（Getters） ====================

  /** 待开始计划数量 */
  const pendingCount = computed(() => plans.value.filter((p) => p.status === 'pending').length)
  /** 已完成计划数量 */
  const doneCount = computed(() => plans.value.filter((p) => p.status === 'done').length)

  // ==================== 操作方法 ====================

  /**
   * 获取学习计划列表（支持分页和筛选）
   * @param {object} [params={}] - 查询参数，会与当前 query 合并
   * @param {number} [params.page] - 页码
   * @param {number} [params.pageSize] - 每页条数
   * @param {string} [params.priority] - 优先级筛选（high / medium / low）
   * @param {string} [params.status] - 状态筛选（pending / doing / done）
   * @param {string} [params.keyword] - 关键词搜索
   * @returns {Promise<void>}
   */
  async function fetchPlans(params = {}) {
    loading.value = true
    query.value = { ...query.value, ...params }
    try {
      const res = await getStudyPlanListApi(query.value)
      plans.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建学习计划，成功后自动刷新列表
   * @param {object} data - 计划数据（标题、优先级、截止日期等）
   * @returns {Promise<object>} 新创建的计划数据
   */
  async function addPlan(data) {
    const res = await createStudyPlanApi(data)
    await fetchPlans()
    return res.data
  }

  /**
   * 编辑学习计划，成功后自动刷新列表
   * @param {string|number} id - 计划 ID
   * @param {object} data - 需要更新的字段
   * @returns {Promise<object>} 更新后的计划数据
   */
  async function editPlan(id, data) {
    const res = await updateStudyPlanApi(id, data)
    await fetchPlans()
    return res.data
  }

  /**
   * 删除学习计划，成功后自动刷新列表
   * @param {string|number} id - 计划 ID
   * @returns {Promise<void>}
   */
  async function removePlan(id) {
    await deleteStudyPlanApi(id)
    await fetchPlans()
  }

  /**
   * 切换计划状态（待开始 -> 进行中 -> 已完成 -> 待开始 循环）
   * @param {string|number} id - 计划 ID
   * @returns {Promise<object|undefined>} 更新后的计划数据；计划不存在时返回 undefined
   */
  async function toggleStatus(id) {
    const plan = plans.value.find((p) => p.id === id)
    if (!plan) return
    // 状态循环映射：pending -> doing -> done -> pending
    const statusMap = { pending: 'doing', doing: 'done', done: 'pending' }
    return editPlan(id, { status: statusMap[plan.status] || 'pending' })
  }

  return {
    // 状态
    plans,
    total,
    loading,
    query,
    // 计算属性
    pendingCount,
    doneCount,
    // 方法
    fetchPlans,
    addPlan,
    editPlan,
    removePlan,
    toggleStatus,
  }
})
