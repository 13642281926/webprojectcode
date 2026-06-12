/**
 * 错题本 Store
 *
 * 管理错题的 CRUD 操作、分类列表、掌握标记和分页查询。
 * 支持按分类、难度、状态（new/reviewing/mastered）、关键词筛选。
 * 提供各状态的错题计数作为派生数据。
 *
 * 错题状态说明：
 * - new：新记录，尚未开始复习
 * - reviewing：正在复习中
 * - mastered：已掌握
 *
 * @module stores/wrongQuestion
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getWrongQuestionListApi,
  getWrongQuestionDetailApi,
  createWrongQuestionApi,
  updateWrongQuestionApi,
  deleteWrongQuestionApi,
  markAsMasteredApi,
  getWrongQuestionCategoriesApi,
} from '@/api/wrongQuestion'

/**
 * 错题本 Store
 */
export const useWrongQuestionStore = defineStore('wrongQuestion', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<Array>} 错题列表（当前页） */
  const wrongQuestions = ref([])
  /** @type {import('vue').Ref<Array>} 错题分类列表 */
  const categories = ref([])
  /** @type {import('vue').Ref<number>} 错题总数（用于分页） */
  const total = ref(0)
  /** @type {import('vue').Ref<boolean>} 列表加载状态 */
  const loading = ref(false)
  /** @type {import('vue').Ref<object>} 列表查询条件（分页、分类、难度、状态、关键词） */
  const query = ref({ page: 1, pageSize: 10, category: '', difficulty: '', status: '', keyword: '' })

  // ==================== 操作方法 ====================

  /**
   * 获取错题列表（支持分页和筛选）
   * @param {object} [params={}] - 查询参数，会与当前 query 合并
   * @param {number} [params.page] - 页码
   * @param {number} [params.pageSize] - 每页条数
   * @param {string} [params.category] - 分类筛选
   * @param {string} [params.difficulty] - 难度筛选
   * @param {string} [params.status] - 状态筛选（new / reviewing / mastered）
   * @param {string} [params.keyword] - 关键词搜索
   * @returns {Promise<void>}
   */
  async function fetchWrongQuestions(params = {}) {
    loading.value = true
    query.value = { ...query.value, ...params }
    try {
      const res = await getWrongQuestionListApi(query.value)
      wrongQuestions.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取单道错题详情
   * @param {string|number} id - 错题 ID
   * @returns {Promise<object>} 错题详情数据
   */
  async function fetchWrongQuestionDetail(id) {
    return await getWrongQuestionDetailApi(id)
  }

  /**
   * 获取错题分类列表
   * @returns {Promise<void>}
   */
  async function fetchCategories() {
    const res = await getWrongQuestionCategoriesApi()
    categories.value = res.data
  }

  /**
   * 添加错题，成功后自动刷新列表
   * @param {object} data - 错题数据（题目、答案、分类、难度等）
   * @returns {Promise<object>} 新创建的错题数据
   */
  async function addWrongQuestion(data) {
    const res = await createWrongQuestionApi(data)
    await fetchWrongQuestions()
    return res.data
  }

  /**
   * 编辑错题，成功后自动刷新列表
   * @param {string|number} id - 错题 ID
   * @param {object} data - 需要更新的字段
   * @returns {Promise<object>} 更新后的错题数据
   */
  async function editWrongQuestion(id, data) {
    const res = await updateWrongQuestionApi(id, data)
    await fetchWrongQuestions()
    return res.data
  }

  /**
   * 删除错题，成功后自动刷新列表
   * @param {string|number} id - 错题 ID
   * @returns {Promise<void>}
   */
  async function removeWrongQuestion(id) {
    await deleteWrongQuestionApi(id)
    await fetchWrongQuestions()
  }

  /**
   * 将错题标记为"已掌握"
   * @param {string|number} id - 错题 ID
   * @returns {Promise<object>} 更新后的错题数据
   */
  async function markAsMastered(id) {
    const res = await markAsMasteredApi(id)
    await fetchWrongQuestions()
    return res.data
  }

  // ==================== 计算属性（Getters） ====================

  /** 已掌握的错题数 */
  const masteredCount = computed(() => wrongQuestions.value.filter(q => q.status === 'mastered').length)
  /** 复习中的错题数 */
  const reviewingCount = computed(() => wrongQuestions.value.filter(q => q.status === 'reviewing').length)
  /** 新记录的错题数 */
  const newCount = computed(() => wrongQuestions.value.filter(q => q.status === 'new').length)

  return {
    // 状态
    wrongQuestions,
    categories,
    total,
    loading,
    query,
    // 计算属性
    masteredCount,
    reviewingCount,
    newCount,
    // 方法
    fetchWrongQuestions,
    fetchWrongQuestionDetail,
    fetchCategories,
    addWrongQuestion,
    editWrongQuestion,
    removeWrongQuestion,
    markAsMastered,
  }
})
