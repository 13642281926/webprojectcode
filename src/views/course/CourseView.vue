<script setup>
import { onMounted, ref, watch } from 'vue'
import CourseCard from '@/components/common/CourseCard.vue'
import LazyImage from '@/components/common/LazyImage.vue'
import { getCourseListApi, getCourseDetailApi } from '@/api/course'
import { debounce } from '@/utils/debounce'
import { Search, Collection, Medal } from '@element-plus/icons-vue'

const loading = ref(false)
const courses = ref([])
const categories = ref([])
const category = ref('all')
const keyword = ref('')

const drawerVisible = ref(false)
const detailLoading = ref(false)
const courseDetail = ref(null)

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

const debouncedSearch = debounce(() => {
  fetchList()
}, 400)

watch(keyword, () => {
  debouncedSearch()
})

watch(category, () => {
  fetchList()
})

onMounted(fetchList)

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
    <!-- 页面标题区域 -->
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
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
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

    <!-- 课程卡片网格 -->
    <div v-loading="loading" class="content-grid">
      <CourseCard 
        v-for="course in courses" 
        :key="course.id" 
        :course="course" 
        class="course-page__card" 
        @click="openDetail(course)" 
      />
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
          <LazyImage :src="courseDetail.cover" :alt="courseDetail.title" class="course-detail__cover" />
          <h3 class="course-detail__title">{{ courseDetail.title }}</h3>
          <p class="course-detail__desc">{{ courseDetail.description }}</p>
          <p class="course-detail__meta">
            讲师：{{ courseDetail.teacher }} · {{ courseDetail.lessons }} 课时
          </p>
          <div v-if="courseDetail.knowledgePoints?.filter(Boolean).length" class="course-detail__tags">
            <el-tag v-for="kp in courseDetail.knowledgePoints.filter(Boolean)" :key="kp" size="default" effect="plain" type="info">
              {{ kp }}
            </el-tag>
          </div>
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
