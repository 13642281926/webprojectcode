<script setup>
/**
 * CourseView - 课程中心
 *
 * 核心功能：
 * - 课程卡片网格展示，支持按分类筛选和关键词搜索（400ms 防抖）
 * - 点击卡片弹出详情抽屉，展示封面、描述、讲师、课时、知识标签、章节时间线
 * - 管理员（isAdmin）可进行课程 CRUD：创建、编辑、删除
 * - 课程类别由后端返回，支持动态分类
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CourseCard from '@/components/common/CourseCard.vue'
import LazyImage from '@/components/common/LazyImage.vue'
import { getCourseListApi, getCourseDetailApi, createCourseApi, updateCourseApi, deleteCourseApi } from '@/api/course'
import { useUserStore } from '@/stores/user'
import { debounce } from '@/utils/debounce'
import { Search, Collection, Medal, Plus, Edit, Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()
/** 是否为管理员，控制增删改按钮的显示 */
const isAdmin = computed(() => userStore.isAdmin)

/** 课程列表加载状态 */
const loading = ref(false)
/** 课程列表数据 */
const courses = ref([])
/** 课程分类列表（由后端返回） */
const categories = ref([])
/** 当前选中的分类筛选值 */
const category = ref('all')
/** 搜索关键词 */
const keyword = ref('')

/** 课程详情抽屉可见性 */
const drawerVisible = ref(false)
/** 详情加载状态 */
const detailLoading = ref(false)
/** 当前查看的课程详情数据 */
const courseDetail = ref(null)

/** 管理员：添加/编辑课程对话框 */
const dialogVisible = ref(false)
/** 对话框标题（添加课程 / 编辑课程） */
const dialogTitle = ref('添加课程')
/** 是否处于编辑模式 */
const isEditing = ref(false)
/** 正在编辑的课程 ID */
const editingId = ref('')
/** 保存按钮 loading */
const saving = ref(false)
/** 课程表单数据，使用 reactive 支持双向绑定 */
const formData = reactive({
  id: '',
  title: '',
  category: 'frontend',
  cover: '',
  description: '',
  teacher: '',
  lessons: 0,
})

/** 课程类别选项（管理员创建/编辑时使用） */
const categoryOptions = [
  { value: 'frontend', label: '前端开发' },
  { value: 'cs', label: '计算机基础' },
  { value: 'language', label: '语言学习' },
]

/** 重置表单数据为初始值 */
function resetForm() {
  formData.id = ''
  formData.title = ''
  formData.category = 'frontend'
  formData.cover = ''
  formData.description = ''
  formData.teacher = ''
  formData.lessons = 0
}

/** 打开创建课程对话框 */
function openCreateDialog() {
  resetForm()
  isEditing.value = false
  dialogTitle.value = '添加课程'
  dialogVisible.value = true
}

/** 打开编辑课程对话框，预填已有数据 */
function openEditDialog(course) {
  isEditing.value = true
  editingId.value = course.id
  dialogTitle.value = '编辑课程'
  formData.id = course.id
  formData.title = course.title
  formData.category = course.category
  formData.cover = course.cover || ''
  formData.description = course.description || ''
  formData.teacher = course.teacher || ''
  formData.lessons = course.lessons || 0
  dialogVisible.value = true
}

/** 保存课程（创建或更新），根据 isEditing 分发 API */
async function handleSave() {
  saving.value = true
  try {
    const data = { ...formData }
    if (isEditing.value) {
      await updateCourseApi(editingId.value, data)
      ElMessage.success('课程更新成功')
    } else {
      await createCourseApi(data)
      ElMessage.success('课程创建成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

/**
 * 删除课程
 * 弹出确认框，用户确认后调用 API 并刷新列表
 */
async function handleDelete(course) {
  try {
    await ElMessageBox.confirm(
      `确定要删除课程「${course.title}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteCourseApi(course.id)
    ElMessage.success('课程已删除')
    await fetchList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }
}

/** 从后端拉取课程列表，传入当前分类和关键词筛选 */
async function fetchList() {
  loading.value = true
  try {
    const res = await getCourseListApi({
      category: category.value === 'all' ? '' : category.value,
      keyword: keyword.value,
    })
    courses.value = res.data.list
    categories.value = res.data.categories || []
  } finally {
    loading.value = false
  }
}

/** 关键词搜索防抖处理（400ms），减少 API 请求频率 */
const debouncedSearch = debounce(() => {
  fetchList()
}, 400)

/** 监听关键词变化，触发防抖搜索 */
watch(keyword, () => {
  debouncedSearch()
})

/** 监听分类变化，立即刷新列表 */
watch(category, () => {
  fetchList()
})

onMounted(fetchList)

/**
 * 打开课程详情抽屉
 * 先显示 loading 占位，异步加载详情数据
 */
async function openDetail(course) {
  drawerVisible.value = true
  detailLoading.value = true
  courseDetail.value = null
  try {
    const res = await getCourseDetailApi(course.id)
    courseDetail.value = res.data
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <div class="page-container course-page">
    <!-- 页面标题区域：课程中心概览 + 管理员操作按钮 -->
    <div class="page-hero">
      <div class="page-hero__content">
        <div class="page-badge">
          <el-icon :size="16"><Collection /></el-icon>
          <span>课程中心</span>
        </div>
        <h1 class="page-title gradient-text">探索优质课程</h1>
        <p class="page-subtitle">发现适合你的学习资源，开启新的学习旅程</p>
        <div class="page-actions">
          <el-tag size="large" type="primary">
            <el-icon><Medal /></el-icon>
            {{ courses.length }} 门课程
          </el-tag>
          <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="openCreateDialog">
            添加课程
          </el-button>
        </div>
      </div>
    </div>

    <!-- 筛选区域：分类单选 + 关键词搜索 -->
    <div class="filter-panel">
      <div class="filter-panel__grow">
        <el-radio-group v-model="category" size="default" class="category-radio-group">
          <el-radio-button
            v-for="cat in categories"
            :key="cat.value || cat.id"
            :value="cat.value || cat.id"
          >
            {{ cat.label || cat.name }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索课程名称/讲师"
        style="width: 260px"
        :prefix-icon="Search"
      />
    </div>

    <!-- 课程卡片网格 + 管理员浮层按钮 -->
    <div v-loading="loading" class="content-grid">
      <div v-for="course in courses" :key="course.id" class="course-card-wrapper">
        <CourseCard
          :course="course"
          class="course-page__card"
          @click="openDetail(course)"
        />
        <!-- 管理员悬停时显示的编辑/删除按钮 -->
        <div v-if="isAdmin" class="course-card-admin">
          <el-button size="small" type="primary" :icon="Edit" circle @click.stop="openEditDialog(course)" />
          <el-button size="small" type="danger" :icon="Delete" circle @click.stop="handleDelete(course)" />
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !courses.length" description="暂无课程" class="empty-state" />

    <!-- 课程详情抽屉 -->
    <el-drawer v-model="drawerVisible" size="460px" destroy-on-close class="course-drawer">
      <template #header>
        <div class="drawer-header">
          <h2 class="drawer-title">课程详情</h2>
        </div>
      </template>
      <div v-loading="detailLoading" class="course-detail">
        <template v-if="courseDetail">
          <!-- 课程封面图 -->
          <LazyImage :src="courseDetail.cover" :alt="courseDetail.title" class="course-detail__cover" />
          <h3 class="course-detail__title">{{ courseDetail.title }}</h3>
          <p class="course-detail__desc">{{ courseDetail.description }}</p>
          <p class="course-detail__meta">
            讲师：{{ courseDetail.teacher }} · {{ courseDetail.lessons }} 课时
          </p>
          <!-- 知识标签 -->
          <div v-if="courseDetail.knowledgePoints?.filter(Boolean).length" class="course-detail__tags">
            <el-tag v-for="kp in courseDetail.knowledgePoints.filter(Boolean)" :key="kp" size="default" effect="plain" type="info">
              {{ kp }}
            </el-tag>
          </div>
          <!-- 学习进度条 -->
          <div class="course-detail__progress">
            <span class="progress-label">学习进度</span>
            <span class="progress-value">{{ courseDetail.progress }}%</span>
          </div>
          <el-progress
            :percentage="courseDetail.progress"
            :stroke-width="12"
            :color="[
              { color: '#3b82f6', percentage: 0 },
              { color: '#22c55e', percentage: 100 }
            ]"
          />
          <el-divider content-position="left">
            <span class="divider-title">章节列表</span>
          </el-divider>
          <!-- 已完成/待学习图例 -->
          <div class="course-detail__legend">
            <span class="legend-item">
              <span class="course-detail__dot course-detail__dot--done"></span>
              已完成
            </span>
            <span class="legend-item">
              <span class="course-detail__dot course-detail__dot--todo"></span>
              待学习
            </span>
          </div>
          <!-- 章节时间线 -->
          <div class="chapters-container">
            <el-timeline>
              <el-timeline-item
                v-for="ch in courseDetail.chapters.filter(Boolean)"
                :key="ch.id"
                :type="ch.done ? 'success' : 'primary'"
                :size="'large'"
              >
                <div class="chapter-item">
                  <span class="chapter-title">{{ ch.title }}</span>
                  <span class="chapter-duration">{{ ch.duration }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </template>
      </div>
    </el-drawer>

    <!-- 管理员：添加/编辑课程对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="formData" label-position="top">
        <el-form-item label="课程ID" required>
          <el-input v-model="formData.id" placeholder="如 course-001" :disabled="isEditing" />
        </el-form-item>
        <el-form-item label="课程标题" required>
          <el-input v-model="formData.title" placeholder="请输入课程标题" />
        </el-form-item>
        <el-form-item label="课程类别" required>
          <el-select v-model="formData.category" style="width: 100%">
            <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="formData.cover" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="课程描述" required>
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入课程描述" />
        </el-form-item>
        <el-form-item label="讲师" required>
          <el-input v-model="formData.teacher" placeholder="请输入讲师姓名" />
        </el-form-item>
        <el-form-item label="课时数" required>
          <el-input-number v-model="formData.lessons" :min="1" :max="200" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isEditing ? '保存修改' : '创建课程' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.course-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-hero {
  margin: 0;
}

.category-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.course-page__card {
  width: 100%;
}

.course-card-wrapper {
  position: relative;

  .course-card-admin {
    position: absolute;
    top: 8px;
    left: 8px;
    display: flex;
    gap: 6px;
    z-index: 2;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .course-card-admin {
    opacity: 1;
  }
}

.empty-state {
  padding: 60px 0;
}

.course-drawer {
  :deep(.el-drawer__header) {
    padding: 24px 24px 0;
    margin-bottom: 16px;
  }
  
  :deep(.el-drawer__body) {
    padding: 0 24px 24px;
  }
}

.drawer-header {
  display: flex;
  align-items: center;
}

.drawer-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.course-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.course-detail__cover {
  width: 100%;
  height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

  :deep(.lazy-image) {
    height: 200px;
    border-radius: 16px;
  }
}

.course-detail__title {
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0 4px;
  color: var(--color-text-primary);
}

.course-detail__desc {
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.8;
  font-size: 14px;
}

.course-detail__meta {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 4px 0 16px;
}

.course-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.course-detail__progress {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  margin-bottom: 12px;
}

.progress-label {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.progress-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.divider-title {
  font-weight: 600;
  font-size: 15px;
}

.course-detail__legend {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-detail__dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;

  &--done {
    background: linear-gradient(135deg, #22c55e, #16a34a);
    box-shadow: 0 0 8px rgba(34, 197, 94, 0.3);
  }
  &--todo {
    background: linear-gradient(135deg, #6366f1, #4f46e5);
    box-shadow: 0 0 8px rgba(99, 102, 241, 0.3);
  }
}

.chapters-container {
  background: rgba(15, 23, 42, 0.3);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.chapter-title {
  font-weight: 500;
}

.chapter-duration {
  font-size: 12px;
  color: var(--color-text-muted);
  background: rgba(99, 102, 241, 0.1);
  padding: 4px 10px;
  border-radius: 10px;
}

:root[data-theme='light'],
:root:not(.dark) {
  .chapters-container {
    background: rgba(255, 255, 255, 0.7);
    border-color: rgba(0, 0, 0, 0.08);
  }
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
