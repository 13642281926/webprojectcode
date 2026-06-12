<template>
  <div class="resource-page">
    <!-- 页面标题与上传按钮 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><FolderOpened /></el-icon>
        学习资源库
      </h2>
      <el-button type="primary" @click="handleUpload">
        <el-icon><Upload /></el-icon>
        上传资源
      </el-button>
    </div>

    <!-- 筛选栏：分类 + 文件类型 + 搜索 -->
    <div class="filter-bar">
      <el-select v-model="filterCategory" placeholder="分类" clearable @change="handleFilter">
        <el-option
          v-for="cat in categories"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
      </el-select>
      <el-select v-model="filterType" placeholder="文件类型" clearable @change="handleFilter">
        <el-option v-for="t in ['pdf', 'docx', 'mp4', 'zip', 'md', 'xlsx', 'png']" :key="t" :label="t.toUpperCase()" :value="t" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索资源..."
        clearable
        :prefix-icon="Search"
        style="width: 300px"
        @input="handleSearch"
      />
    </div>

    <!-- 资源网格 -->
    <div class="resources-grid" v-loading="loading">
      <div
        v-for="resource in resources"
        :key="resource.id"
        class="resource-card"
        @click="handleOpen(resource)"
      >
        <!-- 文件类型图标 -->
        <div class="resource-icon">
          <el-icon :size="48"><component :is="getFileIcon(resource.type)" /></el-icon>
        </div>
        <div class="resource-info">
          <div class="resource-title" :title="resource.title">{{ resource.title }}</div>
          <div class="resource-desc">{{ resource.description }}</div>
          <!-- 文件大小 / 下载次数 / 创建时间 -->
          <div class="resource-meta">
            <span class="resource-size">{{ resource.size }}</span>
            <span class="resource-downloads">
              <el-icon><Download /></el-icon>
              {{ resource.downloadCount }}
            </span>
            <span class="resource-time">{{ resource.createTime?.split(' ')[0] }}</span>
          </div>
        </div>
        <!-- 下载/删除操作按钮 -->
        <div class="resource-actions" @click.stop>
          <el-button type="primary" size="small" @click="handleDownload(resource)">
            <el-icon><Download /></el-icon>
            下载
          </el-button>
          <el-button type="danger" size="small" link @click="handleDelete(resource.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleFilter"
        @current-change="handleFilter"
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传资源"
      width="500px"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" placeholder="请输入资源标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="uploadForm.category" placeholder="选择分类">
            <el-option
              v-for="cat in categories.filter(c => c.id !== 'all')"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入资源描述"
          />
        </el-form-item>
        <!-- 拖拽上传区域，auto-upload=false 手动控制上传时机 -->
        <el-form-item label="文件">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUpload" :loading="uploadLoading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * ResourceView - 学习资源库
 *
 * 核心功能：
 * - 资源列表展示（网格布局），按分类和文件类型筛选，关键词搜索（400ms 防抖）
 * - 上传资源：标题、分类、描述 + 拖拽上传文件（FormData 多部分上传）
 * - 下载资源：通过 API 获取 Blob，创建临时下载链接自动触发浏览器下载
 *   - 自动根据文件类型补全扩展名（pdf/docx/md/xlsx/mp4/zip/png）
 * - 删除资源（含确认弹窗）
 * - 根据文件类型显示对应图标（Document/VideoCamera/Box/Picture）
 * - 数据由 Pinia store（useResourceStore）统一管理
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderOpened,
  Upload,
  Search,
  Download,
  Delete,
  UploadFilled,
  Document,
  VideoCamera,
  Files,
  Picture,
  Box,
} from '@element-plus/icons-vue'
import { useResourceStore } from '@/stores/resource'
import { debounce } from 'lodash-es'
import { downloadResourceApi } from '@/api/resource'

const resourceStore = useResourceStore()

/** 从 store 映射的响应式状态 */
const loading = computed(() => resourceStore.loading)
const resources = computed(() => resourceStore.resources)
const total = computed(() => resourceStore.total)
const categories = computed(() => resourceStore.categories)
/** 双向绑定 store 分页查询参数 */
const query = computed({
  get: () => resourceStore.query,
  set: (val) => { resourceStore.query = val }
})

/** 本地筛选状态 */
const filterCategory = ref('')
const filterType = ref('')
const searchKeyword = ref('')
/** 上传对话框 */
const uploadDialogVisible = ref(false)
/** 上传表单数据 */
const uploadForm = ref({
  title: '',
  category: '',
  description: '',
  file: null,
})
/** 上传按钮 loading */
const uploadLoading = ref(false)

/** 页面挂载时拉取资源列表和分类 */
onMounted(() => {
  resourceStore.fetchResources()
  resourceStore.fetchCategories()
})

/** 重新拉取列表，传入当前筛选条件 */
const handleFilter = () => {
  resourceStore.fetchResources({
    category: filterCategory.value,
    type: filterType.value,
    keyword: searchKeyword.value,
    page: query.value.page,
    pageSize: query.value.pageSize,
  })
}

/** 搜索防抖 400ms */
const handleSearch = debounce(() => {
  handleFilter()
}, 400)

/** 打开上传对话框，重置表单 */
const handleUpload = () => {
  uploadForm.value = { title: '', category: '', description: '', file: null }
  uploadDialogVisible.value = true
}

/** 文件选择变更时，保存原始 File 对象 */
const handleFileChange = (file) => {
  uploadForm.value.file = file.raw
}

/**
 * 保存上传
 * 构建 FormData 多部分表单，包含 file + title + category + description
 * 调用 store.uploadResource 完成上传
 */
const handleSaveUpload = async () => {
  if (!uploadForm.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择文件')
    return
  }

  try {
    uploadLoading.value = true
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    formData.append('title', uploadForm.value.title)
    if (uploadForm.value.category) {
      formData.append('category', uploadForm.value.category)
    }
    if (uploadForm.value.description) {
      formData.append('description', uploadForm.value.description)
    }
    await resourceStore.uploadResource(formData)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    handleFilter()
  } catch (e) {
    console.error('上传失败:', e)
    ElMessage.error('上传失败')
  } finally {
    uploadLoading.value = false
  }
}

/** 打开资源详情（当前为占位） */
const handleOpen = (resource) => {
  ElMessage.info('资源详情')
}

/**
 * 下载资源
 * 1. 调用 downloadResourceApi 获取 Blob 响应
 * 2. 创建 ObjectURL，构建隐藏 a 标签触发浏览器下载
 * 3. 自动根据资源类型补全文件扩展名
 * 4. 下载完成后清理 URL 对象和 DOM 节点
 */
const handleDownload = async (resource) => {
  try {
    const res = await downloadResourceApi(resource.id)

    // 将 Blob 转为临时 URL
    const url = window.URL.createObjectURL(res)
    const link = document.createElement('a')
    link.href = url

    // 自动补全文件扩展名
    let filename = resource.title
    if (!filename.includes('.')) {
      const ext = {
        pdf: '.pdf',
        docx: '.docx',
        md: '.md',
        xlsx: '.xlsx',
        mp4: '.mp4',
        zip: '.zip',
        png: '.png'
      }[resource.type] || ''
      filename += ext
    }

    // 触发浏览器下载
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success(`开始下载: ${resource.title}`)
    handleFilter()
  } catch (e) {
    console.warn('[resource] download failed:', e?.message || e)
    ElMessage.error('下载失败，请稍后重试')
  }
}

/** 删除资源：弹出确认框后调用 store */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个资源吗？', '提示', {
      type: 'warning',
    })
    await resourceStore.removeResource(id)
    ElMessage.success('删除成功')
    handleFilter()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 根据文件类型返回对应的 Element Plus 图标组件
 * Document: pdf/docx/md/xlsx
 * VideoCamera: mp4
 * Box: zip
 * Picture: png
 * 默认: Files
 */
const getFileIcon = (type) => {
  const map = {
    pdf: Document,
    docx: Document,
    md: Document,
    xlsx: Document,
    mp4: VideoCamera,
    zip: Box,
    png: Picture,
  }
  return map[type] || Files
}
</script>

<style scoped lang="scss">
.resource-page {
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
  flex-wrap: wrap;
}

.resources-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.resource-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }
}

.resource-icon {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary-color), #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.resource-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.resource-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.resource-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
  
  .resource-downloads {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.resource-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}
</style>
