<template>
  <div class="note-page">
    <!-- 页面标题与新建按钮 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><Document /></el-icon>
        笔记管理
      </h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建笔记
      </el-button>
    </div>

    <!-- 筛选栏：分类下拉 + 搜索输入 -->
    <div class="filter-bar">
      <el-select v-model="filterCategory" placeholder="分类" clearable @change="handleFilter">
        <el-option
          v-for="cat in categories"
          :key="cat"
          :label="cat"
          :value="cat"
        />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记..."
        clearable
        :prefix-icon="Search"
        style="width: 300px"
        @input="handleSearch"
      />
    </div>

    <!-- 笔记卡片网格 -->
    <div class="notes-grid" v-loading="loading">
      <div
        v-for="note in notes"
        :key="note.id"
        class="note-card"
        :class="{ pinned: note.isPinned }"
        @click="handleOpen(note)"
      >
        <div class="note-header">
          <!-- 标题 + 置顶图标 -->
          <div class="note-title" :title="note.title">
            <el-icon v-if="note.isPinned" class="pin-icon"><TopRight /></el-icon>
            {{ note.title }}
          </div>
          <!-- 操作按钮：编辑、删除，阻止冒泡防止触发详情 -->
          <div class="note-actions" @click.stop>
            <el-button link type="primary" size="small" @click="handleEdit(note)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(note.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <!-- 笔记内容预览（最多 3 行，超出省略） -->
        <div class="note-preview">{{ note.content }}</div>
        <div class="note-meta">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" style="margin-right: 4px">
            {{ tag }}
          </el-tag>
          <span class="note-category">{{ note.category }}</span>
          <span class="note-time">{{ note.updateTime }}</span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
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

    <!-- 抽屉：编辑模式 / 详情模式 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEditing ? (currentNote?.id ? '编辑笔记' : '新建笔记') : '笔记详情'"
      size="60%"
      :destroy-on-close="true"
    >
      <!-- 编辑表单 -->
      <div v-if="isEditing" class="note-edit-form">
        <el-form :model="formData" label-width="80px">
          <el-form-item label="标题">
            <el-input v-model="formData.title" placeholder="请输入笔记标题" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="formData.category" placeholder="选择分类">
              <el-option
                v-for="cat in categories.filter(c => c !== '全部')"
                :key="cat"
                :label="cat"
                :value="cat"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <!-- allow-create 支持自定义输入新标签 -->
            <el-select
              v-model="formData.tags"
              multiple
              filterable
              allow-create
              placeholder="选择或输入标签"
            >
              <el-option label="Vue" value="Vue" />
              <el-option label="React" value="React" />
              <el-option label="Java" value="Java" />
              <el-option label="Python" value="Python" />
              <el-option label="数学" value="数学" />
              <el-option label="英语" value="英语" />
            </el-select>
          </el-form-item>
          <el-form-item label="内容">
            <el-input
              v-model="formData.content"
              type="textarea"
              :rows="15"
              placeholder="请输入笔记内容..."
            />
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </div>
      <!-- 详情只读视图 -->
      <div v-else class="note-detail" v-if="currentNote">
        <div class="detail-header">
          <h3>{{ currentNote.title }}</h3>
          <div class="detail-meta">
            <el-tag v-for="tag in currentNote.tags" :key="tag" size="small">
              {{ tag }}
            </el-tag>
            <span>{{ currentNote.category }}</span>
            <span>{{ currentNote.updateTime }}</span>
            <span>{{ currentNote.wordCount }} 字</span>
          </div>
        </div>
        <div class="detail-content">{{ currentNote.content }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * NoteView - 笔记管理
 *
 * 核心功能：
 * - 笔记网格展示，支持按分类筛选和关键词搜索（400ms 防抖）
 * - 抽屉式编辑器：新建/编辑/查看三合一，根据 isEditing 切换编辑/详情模式
 * - 标签支持多选、搜索、自定义输入（allow-create）
 * - 笔记支持置顶（pinned）、分页浏览
 * - 数据由 Pinia store（useNoteStore）统一管理
 */
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Search, Edit, Delete, TopRight } from '@element-plus/icons-vue'
import { useNoteStore } from '@/stores/notes'
import { debounce } from 'lodash-es'

const noteStore = useNoteStore()

/** 从 store 映射的响应式状态 */
const loading = computed(() => noteStore.loading)
const notes = computed(() => noteStore.notes)
const total = computed(() => noteStore.total)
const categories = computed(() => noteStore.categories)
/** 双向绑定 store 分页查询参数 */
const query = computed({
  get: () => noteStore.query,
  set: (val) => { noteStore.query = val }
})

/** 本地筛选状态 */
const filterCategory = ref('')
const searchKeyword = ref('')
/** 抽屉可见性 */
const drawerVisible = ref(false)
/** 是否为编辑模式（否则为详情只读模式） */
const isEditing = ref(false)
/** 当前选中的笔记（用于详情展示或编辑回填） */
const currentNote = ref(null)
/** 编辑表单数据 */
const formData = ref({
  title: '',
  content: '',
  category: '',
  tags: [],
})

/** 页面挂载时拉取笔记列表和分类 */
onMounted(() => {
  noteStore.fetchNotes()
  noteStore.fetchCategories()
})

/** 重新拉取列表，传入当前筛选条件 */
const handleFilter = () => {
  noteStore.fetchNotes({
    category: filterCategory.value,
    keyword: searchKeyword.value,
    page: query.value.page,
    pageSize: query.value.pageSize,
  })
}

/** 搜索防抖 400ms */
const handleSearch = debounce(() => {
  handleFilter()
}, 400)

/** 新建笔记：清空表单，进入编辑模式 */
const handleCreate = () => {
  isEditing.value = true
  currentNote.value = null
  formData.value = { title: '', content: '', category: '', tags: [] }
  drawerVisible.value = true
}

/** 打开笔记详情（只读模式） */
const handleOpen = (note) => {
  isEditing.value = false
  currentNote.value = note
  drawerVisible.value = true
}

/** 编辑已有笔记：回填数据，进入编辑模式 */
const handleEdit = (note) => {
  isEditing.value = true
  currentNote.value = note
  formData.value = {
    title: note.title,
    content: note.content,
    category: note.category,
    tags: note.tags,
  }
  drawerVisible.value = true
}

/**
 * 保存笔记（新建或更新）
 * 根据 currentNote.id 判断是新增还是编辑，分发 store 对应方法
 */
const handleSave = async () => {
  if (!formData.value.title) {
    ElMessage.warning('请输入标题')
    return
  }

  try {
    if (currentNote.value?.id) {
      await noteStore.editNote(currentNote.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await noteStore.addNote(formData.value)
      ElMessage.success('创建成功')
    }
    drawerVisible.value = false
    handleFilter()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

/** 删除笔记：弹出确认框后调用 store */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条笔记吗？', '提示', {
      type: 'warning',
    })
    await noteStore.removeNote(id)
    ElMessage.success('删除成功')
    handleFilter()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped lang="scss">
.note-page {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .page-title {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 24px;
    margin: 0;
    color: var(--text-primary);
  }
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.note-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }
  
  &.pinned {
    border: 1px solid var(--primary-color);
    background: linear-gradient(135deg, var(--card-bg), color-mix(in srgb, var(--primary-color) 10%, var(--card-bg)));
  }
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.note-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  
  .pin-icon {
    color: var(--primary-color);
  }
}

.note-preview {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 16px;
}

.note-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  
  .note-category {
    background: var(--tag-bg);
    padding: 2px 8px;
    border-radius: 4px;
  }
  
  .note-time {
    margin-left: auto;
  }
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

.note-edit-form {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .drawer-footer {
    margin-top: auto;
    padding-top: 24px;
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.note-detail {
  .detail-header {
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--border-color);
    
    h3 {
      margin: 0 0 16px 0;
      font-size: 24px;
      color: var(--text-primary);
    }
    
    .detail-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
      color: var(--text-secondary);
      font-size: 14px;
    }
  }
  
  .detail-content {
    font-size: 15px;
    line-height: 1.8;
    color: var(--text-primary);
    white-space: pre-wrap;
  }
}
</style>
