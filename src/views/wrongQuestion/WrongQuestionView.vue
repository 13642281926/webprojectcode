<template>
  <div class="page-container wrong-question-page">
    <!-- 页面标题区域 -->
    <div class="page-hero">
      <div class="page-hero__content">
        <div class="page-badge">
          <el-icon :size="16"><Warning /></el-icon>
          <span>错题本</span>
        </div>
        <h1 class="page-title gradient-text">错题管理</h1>
        <p class="page-subtitle">记录错题，总结经验，不断进步</p>
        <div class="page-actions">
          <el-tag size="large" type="primary">
            <el-icon><DocumentRemove /></el-icon>
            {{ wrongQuestions.length }} 道错题
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 统计卡片：新错题 / 复习中 / 已掌握 -->
    <div class="metric-grid">
      <div class="metric-card glass-card hover-lift">
        <div class="metric-card__icon" style="background: linear-gradient(135deg, #ef4444, #dc2626)">
          <el-icon :size="24"><DocumentRemove /></el-icon>
        </div>
        <div class="metric-card__content">
          <p class="metric-card__label">新错题</p>
          <p class="metric-card__value">{{ newCount }}</p>
        </div>
      </div>
      <div class="metric-card glass-card hover-lift">
        <div class="metric-card__icon" style="background: linear-gradient(135deg, #f59e0b, #d97706)">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="metric-card__content">
          <p class="metric-card__label">复习中</p>
          <p class="metric-card__value">{{ reviewingCount }}</p>
        </div>
      </div>
      <div class="metric-card glass-card hover-lift">
        <div class="metric-card__icon" style="background: linear-gradient(135deg, #22c55e, #16a34a)">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="metric-card__content">
          <p class="metric-card__label">已掌握</p>
          <p class="metric-card__value">{{ masteredCount }}</p>
        </div>
      </div>
    </div>

    <!-- 工具栏：分类/难度/状态筛选 + 搜索 + 添加按钮 -->
    <div class="filter-panel">
      <div class="filter-panel__grow">
        <el-select v-model="filterCategory" placeholder="分类" clearable @change="handleFilter" style="width: 150px">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
        <el-select v-model="filterDifficulty" placeholder="难度" clearable @change="handleFilter" style="width: 120px">
          <el-option label="简单" value="简单" />
          <el-option label="中等" value="中等" />
          <el-option label="困难" value="困难" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleFilter" style="width: 120px">
          <el-option label="新错题" value="new" />
          <el-option label="复习中" value="reviewing" />
          <el-option label="已掌握" value="mastered" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目..."
          clearable
          :prefix-icon="Search"
          style="width: 260px"
          @input="handleSearch"
        />
      </div>
      <div class="filter-panel__actions">
        <el-button type="primary" :icon="Plus" @click="handleCreate" size="default">
          添加错题
        </el-button>
      </div>
    </div>

    <!-- 错题列表：按状态显示左侧彩色边框（红=新、黄=复习中、绿=已掌握） -->
    <div class="content-stack" v-loading="loading">
      <div
        v-for="question in wrongQuestions"
        :key="question.id"
        class="content-card question-card"
        :class="[`status-${question.status}`]"
        @click="handleOpen(question)"
      >
        <div class="question-header">
          <!-- 标题 + 状态标签 -->
          <div class="question-title" :title="question.title">
            <el-tag :type="getStatusType(question.status)" size="default" :effect="question.status === 'mastered' ? 'dark' : 'plain'">
              {{ getStatusText(question.status) }}
            </el-tag>
            {{ question.title }}
          </div>
          <!-- 操作按钮：掌握（非已掌握时显示）/ 编辑 / 删除 -->
          <div class="question-actions" @click.stop>
            <el-button
              v-if="question.status !== 'mastered'"
              type="success"
              size="small"
              link
              @click="handleMarkMastered(question.id)"
            >
              <el-icon><CircleCheck /></el-icon>
              掌握
            </el-button>
            <el-button type="primary" size="small" link @click="handleEdit(question)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(question.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
        <!-- 题目内容预览 -->
        <div class="question-content">{{ question.content }}</div>
        <!-- 元信息：难度 / 分类 / 错误次数 / 创建时间 -->
        <div class="question-meta">
          <el-tag :type="getDifficultyType(question.difficulty)" size="default" effect="plain">
            {{ question.difficulty }}
          </el-tag>
          <span class="question-category">
            <el-icon><Folder /></el-icon>
            {{ question.category }}
          </span>
          <span class="question-wrong-count">
            <el-icon><Warning /></el-icon>
            错 {{ question.wrongCount }} 次
          </span>
          <span class="question-time">
            <el-icon><Calendar /></el-icon>
            {{ question.createTime?.split(' ')[0] }}
          </span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="modern-pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleFilter"
        @current-change="handleFilter"
      />
    </div>

    <!-- 对话框：编辑模式 / 详情模式 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? (currentQuestion?.id ? '编辑错题' : '添加错题') : '错题详情'"
      width="640px"
      :destroy-on-close="true"
      class="modern-dialog"
    >
      <!-- 编辑表单 -->
      <div v-if="isEditing" class="edit-form">
        <el-form :model="formData" label-width="80px" class="modern-form">
          <el-form-item label="标题">
            <el-input v-model="formData.title" placeholder="请输入题目标题" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="formData.category" placeholder="选择分类" style="width: 100%">
              <el-option
                v-for="cat in categories.filter(c => c.id !== 'all')"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="formData.difficulty" size="default">
              <el-radio label="简单" border>简单</el-radio>
              <el-radio label="中等" border>中等</el-radio>
              <el-radio label="困难" border>困难</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="题目">
            <el-input
              v-model="formData.content"
              type="textarea"
              :rows="4"
              placeholder="请输入题目内容"
            />
          </el-form-item>
          <el-form-item label="答案">
            <el-input
              v-model="formData.answer"
              type="textarea"
              :rows="3"
              placeholder="请输入正确答案"
            />
          </el-form-item>
          <el-form-item label="解析">
            <el-input
              v-model="formData.analysis"
              type="textarea"
              :rows="4"
              placeholder="请输入题目解析"
            />
          </el-form-item>
          <el-form-item label="标签">
            <!-- allow-create 支持自定义标签 -->
            <el-select
              v-model="formData.tags"
              multiple
              filterable
              allow-create
              placeholder="选择或输入标签"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <!-- 详情只读视图 -->
      <div v-else class="detail-view" v-if="currentQuestion">
        <div class="detail-section">
          <h4>
            <el-icon><Document /></el-icon>
            题目
          </h4>
          <p>{{ currentQuestion.content }}</p>
        </div>
        <div class="detail-section">
          <h4>
            <el-icon><CircleCheck /></el-icon>
            答案
          </h4>
          <p>{{ currentQuestion.answer }}</p>
        </div>
        <div class="detail-section">
          <h4>
            <el-icon><ChatDotRound /></el-icon>
            解析
          </h4>
          <p>{{ currentQuestion.analysis }}</p>
        </div>
        <div class="detail-info">
          <span>
            <el-icon><TrendCharts /></el-icon>
            难度: {{ currentQuestion.difficulty }}
          </span>
          <span>
            <el-icon><Folder /></el-icon>
            分类: {{ currentQuestion.category }}
          </span>
          <span>
            <el-icon><Warning /></el-icon>
            错误次数: {{ currentQuestion.wrongCount }}
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false" size="default">关闭</el-button>
        <el-button v-if="isEditing" type="primary" @click="handleSave" size="default">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * WrongQuestionView - 错题本
 *
 * 核心功能：
 * - 错题列表展示，按分类、难度、状态（新错题/复习中/已掌握）筛选
 * - 关键词搜索（400ms 防抖）
 * - 统计卡片：新错题数、复习中数、已掌握数
 * - 对话框：新建/编辑/详情三合一，根据 isEditing 切换编辑/只读模式
 * - 支持标记"已掌握"操作
 * - 编辑表单含标题、分类、难度、题目内容、答案、解析、标签
 * - 数据由 Pinia store（useWrongQuestionStore）统一管理
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Warning,
  Plus,
  Search,
  Edit,
  Delete,
  DocumentRemove,
  Clock,
  CircleCheck,
  Calendar,
  Folder,
  Document,
  ChatDotRound,
  TrendCharts,
} from '@element-plus/icons-vue'
import { useWrongQuestionStore } from '@/stores/wrongQuestion'
import { debounce } from 'lodash-es'

const wrongQuestionStore = useWrongQuestionStore()

/** 从 store 映射的响应式状态 */
const loading = computed(() => wrongQuestionStore.loading)
const wrongQuestions = computed(() => wrongQuestionStore.wrongQuestions)
const total = computed(() => wrongQuestionStore.total)
const categories = computed(() => wrongQuestionStore.categories)
/** 双向绑定 store 分页查询参数 */
const query = computed({
  get: () => wrongQuestionStore.query,
  set: (val) => { wrongQuestionStore.query = val }
})
/** 统计数量：已掌握 / 复习中 / 新错题 */
const masteredCount = computed(() => wrongQuestionStore.masteredCount)
const reviewingCount = computed(() => wrongQuestionStore.reviewingCount)
const newCount = computed(() => wrongQuestionStore.newCount)

/** 本地筛选状态 */
const filterCategory = ref('')
const filterDifficulty = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')
/** 对话框可见性 */
const dialogVisible = ref(false)
/** 是否为编辑模式（否则为详情只读模式） */
const isEditing = ref(false)
/** 当前选中的错题（用于详情展示或编辑回填） */
const currentQuestion = ref(null)
/** 编辑表单数据 */
const formData = ref({
  title: '',
  content: '',
  answer: '',
  analysis: '',
  category: '',
  difficulty: '中等',
  tags: [],
})

/** 页面挂载时拉取错题列表和分类 */
onMounted(() => {
  wrongQuestionStore.fetchWrongQuestions()
  wrongQuestionStore.fetchCategories()
})

/** 重新拉取列表，传入当前筛选条件 */
const handleFilter = () => {
  wrongQuestionStore.fetchWrongQuestions({
    category: filterCategory.value,
    difficulty: filterDifficulty.value,
    status: filterStatus.value,
    keyword: searchKeyword.value,
    page: query.value.page,
    pageSize: query.value.pageSize,
  })
}

/** 搜索防抖 400ms */
const handleSearch = debounce(() => {
  handleFilter()
}, 400)

/** 新建错题：清空表单，进入编辑模式 */
const handleCreate = () => {
  isEditing.value = true
  currentQuestion.value = null
  formData.value = { title: '', content: '', answer: '', analysis: '', category: '', difficulty: '中等', tags: [] }
  dialogVisible.value = true
}

/** 打开错题详情（只读模式） */
const handleOpen = (question) => {
  isEditing.value = false
  currentQuestion.value = question
  dialogVisible.value = true
}

/** 编辑已有错题：回填数据（tags 从逗号分割的字符串转为数组），进入编辑模式 */
const handleEdit = (question) => {
  isEditing.value = true
  currentQuestion.value = question
  formData.value = {
    title: question.title,
    content: question.content,
    answer: question.answer,
    analysis: question.analysis,
    category: question.category,
    difficulty: question.difficulty,
    tags: question.tags ? question.tags.split(',') : [],
  }
  dialogVisible.value = true
}

/**
 * 保存错题（新建或更新）
 * 根据 currentQuestion.id 判断是新增还是编辑
 */
const handleSave = async () => {
  if (!formData.value.title || !formData.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    if (currentQuestion.value?.id) {
      await wrongQuestionStore.editWrongQuestion(currentQuestion.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await wrongQuestionStore.addWrongQuestion(formData.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    handleFilter()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

/** 删除错题：弹出确认框后调用 store */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这道错题吗？', '提示', {
      type: 'warning',
    })
    await wrongQuestionStore.removeWrongQuestion(id)
    ElMessage.success('删除成功')
    handleFilter()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 将错题标记为"已掌握" */
const handleMarkMastered = async (id) => {
  try {
    await wrongQuestionStore.markAsMastered(id)
    ElMessage.success('已标记为掌握')
    handleFilter()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

/**
 * 根据错题状态返回对应的 Element Plus tag 类型
 * new -> danger, reviewing -> warning, mastered -> success
 */
const getStatusType = (status) => {
  const map = {
    new: 'danger',
    reviewing: 'warning',
    mastered: 'success',
  }
  return map[status] || 'info'
}

/** 根据错题状态返回中文显示文本 */
const getStatusText = (status) => {
  const map = {
    new: '新错题',
    reviewing: '复习中',
    mastered: '已掌握',
  }
  return map[status] || status
}

/**
 * 根据难度返回对应的 Element Plus tag 类型
 * 简单 -> success, 中等 -> warning, 困难 -> danger
 */
const getDifficultyType = (difficulty) => {
  const map = {
    简单: 'success',
    中等: 'warning',
    困难: 'danger',
  }
  return map[difficulty] || 'info'
}
</script>

<style scoped lang="scss">
.wrong-question-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  
  .metric-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 24px;
  }
  
  .metric-card__content {
    flex: 1;
  }
}

.question-card {
  cursor: pointer;
  border-left: 4px solid rgba(255, 255, 255, 0.08);
  
  &.status-new {
    border-left-color: #ef4444;
  }
  
  &.status-reviewing {
    border-left-color: #f59e0b;
  }
  
  &.status-mastered {
    border-left-color: #22c55e;
    opacity: 0.85;
  }
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.question-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.question-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin-bottom: 18px;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.question-category,
.question-wrong-count,
.question-time {
  display: flex;
  align-items: center;
  gap: 6px;
}

.edit-form,
.detail-view {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  
  h4 {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  p {
    font-size: 15px;
    color: var(--color-text-primary);
    line-height: 1.8;
    white-space: pre-wrap;
    background: rgba(15, 23, 42, 0.3);
    padding: 16px;
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.detail-info {
  display: flex;
  gap: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--color-text-muted);
  font-size: 14px;
  flex-wrap: wrap;
  
  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.modern-dialog {
  :deep(.el-dialog__header) {
    padding: 24px 24px 0;
    margin-bottom: 16px;
  }
  
  :deep(.el-dialog__body) {
    padding: 0 24px;
  }
  
  :deep(.el-dialog__footer) {
    padding: 20px 24px 24px;
  }
}

.modern-form {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
  }
}

:root[data-theme='light'],
:root:not(.dark) {
  .detail-section p {
    background: rgba(0, 0, 0, 0.02);
    border-color: rgba(0, 0, 0, 0.08);
  }
  
  .detail-info {
    border-top-color: rgba(0, 0, 0, 0.08);
  }
}

@media (max-width: 992px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-panel {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-panel__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
