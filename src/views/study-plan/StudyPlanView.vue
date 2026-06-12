<script setup>
/**
 * StudyPlanView - 学习计划
 *
 * 核心功能：
 * - 表格/卡片双视图模式切换
 * - 创建、编辑、删除计划，支持状态切换（待开始/进行中/已完成）
 * - 多条件筛选：优先级（高/中/低）、状态、关键词搜索（400ms 防抖）
 * - 完成状态使用删除线标识，已完成的卡片降低透明度
 * - 数据由 Pinia store（useStudyPlanStore）统一管理
 */
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, List, Grid, Calendar, Trophy, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { useStudyPlanStore } from '@/stores/studyPlan'
import { PLAN_PRIORITY, PLAN_STATUS } from '@/utils/constants'
import { debounce } from '@/utils/debounce'

const planStore = useStudyPlanStore()
/** 视图模式：table（表格）/ card（卡片） */
const viewMode = ref('table')

/** 新增/编辑对话框 */
const dialogVisible = ref(false)
/** 对话框标题 */
const dialogTitle = ref('新增计划')
/** 正在编辑的计划 ID，null 表示新增 */
const editingId = ref(null)
/** 表单组件引用，用于触发 validate */
const formRef = ref()

/** 计划表单数据 */
const form = reactive({
  title: '',
  content: '',
  deadline: '',
  priority: 'medium',
  status: 'pending',
})

/** 表单校验规则：标题和截止时间为必填 */
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
}

/** 关键词搜索防抖（400ms），减少 API 请求频率 */
const debouncedKeywordSearch = debounce(() => {
  planStore.fetchPlans({ page: 1 })
}, 400)

/** 监听搜索关键词变化，触发防抖搜索 */
watch(
  () => planStore.query.keyword,
  (val, oldVal) => {
    if (val !== oldVal) debouncedKeywordSearch()
  },
)

/** 页面挂载时拉取计划列表 */
onMounted(() => {
  planStore.fetchPlans()
})

/** 打开新增计划对话框，清空表单 */
function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增计划'
  Object.assign(form, { title: '', content: '', deadline: '', priority: 'medium', status: 'pending' })
  dialogVisible.value = true
}

/** 打开编辑计划对话框，回填已有数据 */
function openEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑计划'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

/** 提交表单：先校验，再根据编辑/新增分发 store 对应方法 */
async function handleSubmit() {
  await formRef.value?.validate()
  if (editingId.value) {
    await planStore.editPlan(editingId.value, { ...form })
    ElMessage.success('更新成功')
  } else {
    await planStore.addPlan({ ...form })
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
}

/** 删除计划：弹出确认框后调用 store */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？`, '提示', { type: 'warning' })
  await planStore.removePlan(row.id)
  ElMessage.success('已删除')
}

/** 切换计划完成状态（待开始 <-> 进行中 <-> 已完成） */
async function handleToggle(row) {
  await planStore.toggleStatus(row.id)
  ElMessage.success('状态已更新')
}

/** 筛选条件变更时重置到第一页并重新拉取 */
function handleFilter() {
  planStore.fetchPlans({ page: 1 })
}

/** 分页切换时重新拉取指定页 */
function handlePageChange(page) {
  planStore.fetchPlans({ page })
}
</script>

<template>
  <div class="page-container study-plan">
    <!-- 页面标题区域：计划概览 + 完成进度卡片 -->
    <div class="page-hero">
      <div class="page-hero__content">
        <div class="page-badge">
          <el-icon :size="16"><Calendar /></el-icon>
          <span>学习计划</span>
        </div>
        <h1 class="page-title gradient-text">规划你的学习</h1>
        <p class="page-subtitle">制定学习计划，保持自律，实现你的学习目标</p>
        <div class="page-actions">
          <el-tag size="large" type="primary">
            <el-icon><Trophy /></el-icon>
            {{ planStore.plans.length }} 个计划
          </el-tag>
        </div>
      </div>
      <div class="page-hero__aside">
        <div class="page-hero__card">
          <div class="page-hero__eyebrow">当前进度</div>
          <!-- 已完成数 / 总数 -->
          <div class="page-hero__metric">{{ planStore.plans.filter(p => p.status === 'done').length }}<span class="page-hero__metric-unit">/{{ planStore.plans.length }}</span></div>
          <div class="page-hero__hint">已完成计划</div>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="page-shell">
      <!-- 工具栏：优先级/状态筛选 + 关键词搜索 + 视图切换 + 新增按钮 -->
      <div class="filter-panel">
        <div class="filter-panel__grow">
          <el-form :inline="true" class="study-plan__filter">
            <el-form-item label="优先级">
              <el-select v-model="planStore.query.priority" clearable placeholder="全部" @change="handleFilter">
                <el-option label="高" value="high" />
                <el-option label="中" value="medium" />
                <el-option label="低" value="low" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="planStore.query.status" clearable placeholder="全部" @change="handleFilter">
                <el-option label="待开始" value="pending" />
                <el-option label="进行中" value="doing" />
                <el-option label="已完成" value="done" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input
                v-model="planStore.query.keyword"
                clearable
                placeholder="搜索标题/内容"
                @keyup.enter="handleFilter"
                @clear="handleFilter"
              />
            </el-form-item>
          </el-form>
        </div>
        <div class="filter-panel__actions">
          <!-- 视图切换按钮组 -->
          <el-button-group size="default">
            <el-button :type="viewMode === 'table' ? 'primary' : ''" @click="viewMode = 'table'">
              <el-icon><List /></el-icon>
              表格
            </el-button>
            <el-button :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">
              <el-icon><Grid /></el-icon>
              卡片
            </el-button>
          </el-button-group>
          <el-button type="primary" :icon="Plus" @click="openCreate" size="default">
            新增计划
          </el-button>
        </div>
      </div>

      <!-- 表格视图 -->
      <div v-if="viewMode === 'table'" class="panel-card glass-card">
        <el-table v-loading="planStore.loading" :data="planStore.plans" class="modern-table">
          <el-table-column prop="title" label="标题" min-width="160">
            <template #default="{ row }">
              <div class="table-title">
                <!-- 已完成计划附加删除线样式 -->
                <span :class="{ 'text-strikethrough': row.status === 'done' }">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="学习内容" min-width="220" show-overflow-tooltip="true">
            <template #default="{ row }">
              <span class="table-content" :class="{ 'text-strikethrough': row.status === 'done' }">{{ row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deadline" label="截止时间" width="130">
            <template #default="{ row }">
              <div class="deadline-cell">
                <el-icon><Calendar /></el-icon>
                <span>{{ row.deadline }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="PLAN_PRIORITY[row.priority]?.type" size="default" :effect="row.priority === 'high' ? 'dark' : 'plain'">
                {{ PLAN_PRIORITY[row.priority]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="PLAN_STATUS[row.status]?.type" size="default" :effect="row.status === 'done' ? 'dark' : 'plain'">
                {{ PLAN_STATUS[row.status]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button :icon="Refresh" link type="primary" @click="handleToggle(row)" size="small">
                  切换状态
                </el-button>
                <el-button :icon="Edit" link type="primary" @click="openEdit(row)" size="small">
                  编辑
                </el-button>
                <el-button :icon="Delete" link type="danger" @click="handleDelete(row)" size="small">
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" v-loading="planStore.loading" class="content-grid">
        <div v-for="plan in planStore.plans" :key="plan.id" class="content-card" :class="{ 'card-done': plan.status === 'done' }">
          <div class="study-plan__card-header">
            <h4 :class="{ 'text-strikethrough': plan.status === 'done' }">{{ plan.title }}</h4>
            <el-tag :type="PLAN_STATUS[plan.status]?.type" size="default" :effect="plan.status === 'done' ? 'dark' : 'plain'">
              {{ PLAN_STATUS[plan.status]?.label }}
            </el-tag>
          </div>
          <!-- 内容预览最多 2 行 -->
          <p class="study-plan__card-content" :class="{ 'text-strikethrough': plan.status === 'done' }">
            {{ plan.content }}
          </p>
          <div class="study-plan__card-meta">
            <el-tag :type="PLAN_PRIORITY[plan.priority]?.type" size="small" effect="plain">
              {{ PLAN_PRIORITY[plan.priority]?.label }}优先级
            </el-tag>
            <span class="deadline-text">
              <el-icon><Calendar /></el-icon>
              {{ plan.deadline }}
            </span>
          </div>
          <div class="study-plan__card-actions">
            <el-button size="default" @click="handleToggle(plan)" :icon="Refresh">
              切换状态
            </el-button>
            <el-button size="default" type="primary" @click="openEdit(plan)" :icon="Edit">
              编辑
            </el-button>
            <el-button size="default" type="danger" @click="handleDelete(plan)" :icon="Delete">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="modern-pagination">
        <el-pagination
          v-model:current-page="planStore.query.page"
          :page-size="planStore.query.pageSize"
          :total="planStore.total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close class="modern-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="modern-form">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入计划标题" />
        </el-form-item>
        <el-form-item label="学习内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入学习内容" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择截止日期" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority" size="default">
            <el-radio value="high" border>高</el-radio>
            <el-radio value="medium" border>中</el-radio>
            <el-radio value="low" border>低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status" size="default">
            <el-radio value="pending" border>待开始</el-radio>
            <el-radio value="doing" border>进行中</el-radio>
            <el-radio value="done" border>已完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="default">取消</el-button>
        <el-button type="primary" @click="handleSubmit" size="default">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.study-plan {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-hero {
  margin: 0;
}

.study-plan__filter {
  margin: 0;
}

.modern-table {
  border-radius: 16px;
  overflow: hidden;
}

.table-title {
  font-weight: 600;
  color: var(--color-text-primary);
}

.table-content {
  color: var(--color-text-secondary);
}

.text-strikethrough {
  text-decoration: line-through;
  opacity: 0.6;
}

.deadline-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.panel-card {
  padding: 0;
  overflow: hidden;
}

.study-plan__card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;

  h4 {
    font-size: 16px;
    margin: 0;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

.study-plan__card-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.7;
}

.study-plan__card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 18px;
  gap: 12px;
}

.deadline-text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.study-plan__card-actions {
  display: flex;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-wrap: wrap;
}

.card-done {
  opacity: 0.8;
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
  .study-plan__card-actions {
    border-top-color: rgba(0, 0, 0, 0.08);
  }
}

@media (max-width: 768px) {
  .filter-panel {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-panel__actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
