/**
 * 成就系统 Store
 *
 * 数据驱动的成就系统，核心设计：
 * 1. 成就定义与进度计算完全由前端完成，不依赖后端接口
 * 2. 进度根据学习数据（笔记/错题/番茄/计划/资源）实时计算
 * 3. 一旦成就达成，解锁状态写入 localStorage 永久保存，不随学习数据清空而重置
 * 4. 不提供"手动解锁"或"重置"入口，保证成就系统的真实性和激励性
 * 5. 后端 /achievement 接口仅作为可选扩展（如跨设备同步）
 *
 * 成就类型说明：
 * - streak：连续学习天数
 * - focusMinutes：累计专注分钟数
 * - completedPomodoros：完成的番茄钟数量
 * - notes：创建的笔记数量
 * - wrongQuestions：记录的错题数量
 * - masteredWrongs：掌握的错题数量
 * - plans：创建的学习计划数量
 * - completedPlans：完成的学习计划数量
 * - resources：上传的学习资源数量
 *
 * @module stores/achievement
 */

import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { getStorage, setStorage } from '@/utils/storage'

/** localStorage 存储键名 — 永久保存已解锁的成就 ID 及其解锁时间 */
const UNLOCKED_STORAGE_KEY = 'ai-learning-achievement-unlocked'

/**
 * 内置成就定义
 * 覆盖学习、任务、创作、成长四大类别
 * 每个成就包含：id、标题、描述、图标、分类、稀有度、积分、检测类型、目标值
 * @type {Array<{id: string, title: string, description: string, icon: string, category: string, rarity: string, points: number, type: string, target: number}>}
 */
export const ACHIEVEMENT_DEFS = [
  // ========== 成长类成就 ==========
  { id: 'streak-3', title: '初出茅庐', description: '连续学习 3 天', icon: 'Timer', category: '成长', rarity: 'common', points: 10, type: 'streak', target: 3 },
  { id: 'streak-7', title: '持之以恒', description: '连续学习 7 天', icon: 'Timer', category: '成长', rarity: 'uncommon', points: 25, type: 'streak', target: 7 },
  { id: 'streak-30', title: '学习狂人', description: '连续学习 30 天', icon: 'Timer', category: '成长', rarity: 'epic', points: 100, type: 'streak', target: 30 },
  { id: 'study-hours-10', title: '入门学子', description: '累计学习满 10 小时', icon: 'Timer', category: '成长', rarity: 'common', points: 15, type: 'focusMinutes', target: 10 * 60 },
  { id: 'study-hours-50', title: '学海无涯', description: '累计学习满 50 小时', icon: 'Timer', category: '成长', rarity: 'rare', points: 60, type: 'focusMinutes', target: 50 * 60 },
  { id: 'study-hours-100', title: '百炼成钢', description: '累计学习满 100 小时', icon: 'Timer', category: '成长', rarity: 'epic', points: 150, type: 'focusMinutes', target: 100 * 60 },

  // ========== 番茄类成就 ==========
  { id: 'pomodoro-1', title: '第一个番茄', description: '完成 1 个番茄钟', icon: 'Trophy', category: '学习', rarity: 'common', points: 5, type: 'completedPomodoros', target: 1 },
  { id: 'pomodoro-10', title: '番茄达人', description: '完成 10 个番茄钟', icon: 'Trophy', category: '学习', rarity: 'uncommon', points: 25, type: 'completedPomodoros', target: 10 },
  { id: 'pomodoro-50', title: '番茄大师', description: '完成 50 个番茄钟', icon: 'Trophy', category: '学习', rarity: 'rare', points: 80, type: 'completedPomodoros', target: 50 },
  { id: 'pomodoro-200', title: '番茄之神', description: '完成 200 个番茄钟', icon: 'Trophy', category: '学习', rarity: 'legendary', points: 200, type: 'completedPomodoros', target: 200 },

  // ========== 笔记类成就 ==========
  { id: 'note-1', title: '好记性不如烂笔头', description: '创建 1 篇笔记', icon: 'Document', category: '创作', rarity: 'common', points: 5, type: 'notes', target: 1 },
  { id: 'note-10', title: '笔记达人', description: '创建 10 篇笔记', icon: 'Document', category: '创作', rarity: 'uncommon', points: 25, type: 'notes', target: 10 },
  { id: 'note-30', title: '笔耕不辍', description: '创建 30 篇笔记', icon: 'Document', category: '创作', rarity: 'rare', points: 80, type: 'notes', target: 30 },

  // ========== 错题类成就 ==========
  { id: 'wrong-1', title: '吃一堑长一智', description: '记录 1 道错题', icon: 'Warning', category: '学习', rarity: 'common', points: 5, type: 'wrongQuestions', target: 1 },
  { id: 'wrong-20', title: '错题猎人', description: '记录 20 道错题', icon: 'Warning', category: '学习', rarity: 'uncommon', points: 30, type: 'wrongQuestions', target: 20 },
  { id: 'wrong-mastered-10', title: '举一反三', description: '掌握 10 道错题', icon: 'CircleCheck', category: '学习', rarity: 'rare', points: 60, type: 'masteredWrongs', target: 10 },

  // ========== 任务/计划类成就 ==========
  { id: 'plan-1', title: '凡事预则立', description: '创建 1 个学习计划', icon: 'List', category: '任务', rarity: 'common', points: 5, type: 'plans', target: 1 },
  { id: 'plan-10', title: '计划专家', description: '创建 10 个学习计划', icon: 'List', category: '任务', rarity: 'uncommon', points: 25, type: 'plans', target: 10 },
  { id: 'plan-complete-5', title: '高效执行', description: '完成 5 个学习计划', icon: 'CircleCheck', category: '任务', rarity: 'rare', points: 60, type: 'completedPlans', target: 5 },

  // ========== 资源类成就 ==========
  { id: 'resource-1', title: '资源收集者', description: '上传 1 个学习资源', icon: 'Folder', category: '创作', rarity: 'common', points: 5, type: 'resources', target: 1 },
  { id: 'resource-20', title: '知识宝库', description: '上传 20 个学习资源', icon: 'Folder', category: '创作', rarity: 'uncommon', points: 30, type: 'resources', target: 20 },
]

/**
 * 成就系统 Store
 */
export const useAchievementStore = defineStore('achievement', () => {
  // ==================== 状态字段 ====================

  /**
   * 永久解锁记录映射
   * 结构：{ [achievementId]: { unlockedAt: timestamp } }
   * 写入 localStorage 后永久保存，不随学习数据清空而重置
   */
  const unlockedMap = ref(getStorage(UNLOCKED_STORAGE_KEY, {}))
  // 监听 unlockedMap 变化，自动持久化到 localStorage
  watch(unlockedMap, (val) => setStorage(UNLOCKED_STORAGE_KEY, val), { deep: true })

  // ==================== 核心方法 ====================

  /**
   * 计算所有成就的当前状态
   * 根据外部传入的学习指标，评估每个成就的达成进度，并自动永久解锁已达成的成就
   *
   * @param {object} metrics - 来自外部的学习指标数据
   * @param {number} [metrics.notes] - 总笔记数
   * @param {number} [metrics.wrongQuestions] - 总错题数
   * @param {number} [metrics.masteredWrongs] - 已掌握错题数
   * @param {number} [metrics.plans] - 总计划数
   * @param {number} [metrics.completedPlans] - 已完成计划数
   * @param {number} [metrics.resources] - 总资源数
   * @param {number} [metrics.completedPomodoros] - 已完成番茄钟数
   * @param {number} [metrics.focusMinutes] - 累计专注分钟数
   * @param {number} [metrics.streakDays] - 连续学习天数
   * @returns {Array<object>} 包含所有成就当前状态（进度、是否解锁、解锁时间等）的数组
   */
  function evaluate(metrics = {}) {
    return ACHIEVEMENT_DEFS.map((def) => {
      const value = Number(metrics[def.type] || 0)
      const reached = value >= def.target
      // 已达到目标或已被永久记录，即视为已解锁
      const unlocked = reached || Boolean(unlockedMap.value[def.id])
      const target = def.target
      const progress = Math.min(value, target)
      const prev = unlockedMap.value[def.id]
      // 自动永久解锁：刚达到目标且之前未被记录
      if (reached && !prev) {
        unlockedMap.value = { ...unlockedMap.value, [def.id]: { unlockedAt: Date.now() } }
      }
      return {
        ...def,
        progress,
        target,
        unlocked,
        unlockedAt: prev?.unlockedAt || null,
        progressPercent: Math.min(100, Math.round((progress / target) * 100)), // 进度百分比（0-100）
      }
    })
  }

  /** @type {import('vue').Ref<Array>} 当前成就列表（外部调用 evaluate 后通过 setAchievements 写入） */
  const achievements = ref([])

  /**
   * 设置当前成就列表
   * @param {Array<object>} list - evaluate() 返回的成就状态数组
   */
  function setAchievements(list) {
    achievements.value = list
  }

  // ==================== 汇总统计（计算属性） ====================

  /**
   * 成就汇总统计
   * 基于当前成就列表计算总数、已解锁数、总积分、最近解锁、连续天数等
   */
  const stats = computed(() => {
    const list = achievements.value
    const total = list.length
    const unlockedList = list.filter((a) => a.unlocked)
    const totalPoints = unlockedList.reduce((s, a) => s + (a.points || 0), 0)
    // 最近解锁的 5 个成就
    const recentUnlocked = unlockedList
      .filter((a) => a.unlockedAt)
      .sort((a, b) => b.unlockedAt - a.unlockedAt)
      .slice(0, 5)
      .map((a) => ({ id: a.id, name: a.title, icon: a.icon, points: a.points, unlockedAt: a.unlockedAt }))
    // 已解锁的最高连续天数成就的目标值
    const longestStreak = list
      .filter((a) => a.type === 'streak' && a.unlocked)
      .reduce((m, a) => Math.max(m, a.target), 0)
    return {
      total,
      unlockedCount: unlockedList.length,
      totalPoints,
      recentUnlocked,
      streak: achievements.value.streakDays || 0, // 由调用方注入当前连续天数
      longestStreak,
    }
  })

  return {
    achievements,
    unlockedMap,
    stats,
    setAchievements,
    evaluate,
  }
})
