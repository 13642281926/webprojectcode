/**
 * 笔记管理 Store
 *
 * 管理学习笔记的 CRUD 操作、分类列表和分页查询状态。
 * 每次增删改操作后会自动刷新列表，确保 UI 与后端数据同步。
 *
 * @module stores/notes
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNoteListApi,
  getNoteDetailApi,
  createNoteApi,
  updateNoteApi,
  deleteNoteApi,
  getNoteCategoriesApi,
} from '@/api/note'

/**
 * 笔记管理 Store
 */
export const useNoteStore = defineStore('note', () => {
  // ==================== 状态字段 ====================

  /** @type {import('vue').Ref<Array>} 笔记列表（当前页） */
  const notes = ref([])
  /** @type {import('vue').Ref<Array>} 笔记分类列表 */
  const categories = ref([])
  /** @type {import('vue').Ref<number>} 笔记总数（用于分页） */
  const total = ref(0)
  /** @type {import('vue').Ref<boolean>} 列表加载状态 */
  const loading = ref(false)
  /** @type {import('vue').Ref<object|null>} 当前选中的笔记详情 */
  const currentNote = ref(null)
  /** @type {import('vue').Ref<object>} 列表查询条件（分页、分类、关键词） */
  const query = ref({ page: 1, pageSize: 10, category: '', keyword: '' })

  // ==================== 操作方法 ====================

  /**
   * 获取笔记列表（支持分页和筛选）
   * @param {object} [params={}] - 查询参数，会与当前 query 合并
   * @param {number} [params.page] - 页码
   * @param {number} [params.pageSize] - 每页条数
   * @param {string} [params.category] - 分类筛选
   * @param {string} [params.keyword] - 关键词搜索
   * @returns {Promise<void>}
   */
  async function fetchNotes(params = {}) {
    loading.value = true
    query.value = { ...query.value, ...params }
    try {
      const res = await getNoteListApi(query.value)
      notes.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取单篇笔记详情
   * @param {string|number} id - 笔记 ID
   * @returns {Promise<object>} 笔记详情数据
   */
  async function fetchNoteDetail(id) {
    const res = await getNoteDetailApi(id)
    currentNote.value = res.data
    return res.data
  }

  /**
   * 获取笔记分类列表
   * @returns {Promise<void>}
   */
  async function fetchCategories() {
    const res = await getNoteCategoriesApi()
    categories.value = res.data
  }

  /**
   * 创建新笔记，成功后自动刷新列表
   * @param {object} data - 笔记数据（标题、内容、分类等）
   * @returns {Promise<object>} 新创建的笔记数据
   */
  async function addNote(data) {
    const res = await createNoteApi(data)
    await fetchNotes()
    return res.data
  }

  /**
   * 编辑笔记，成功后自动刷新列表
   * @param {string|number} id - 笔记 ID
   * @param {object} data - 需要更新的字段
   * @returns {Promise<object>} 更新后的笔记数据
   */
  async function editNote(id, data) {
    const res = await updateNoteApi(id, data)
    await fetchNotes()
    return res.data
  }

  /**
   * 删除笔记，成功后自动刷新列表
   * @param {string|number} id - 笔记 ID
   * @returns {Promise<void>}
   */
  async function removeNote(id) {
    await deleteNoteApi(id)
    await fetchNotes()
  }

  return {
    // 状态
    notes,
    categories,
    total,
    loading,
    currentNote,
    query,
    // 方法
    fetchNotes,
    fetchNoteDetail,
    fetchCategories,
    addNote,
    editNote,
    removeNote,
  }
})
