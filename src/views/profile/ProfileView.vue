<script setup>
/**
 * ProfileView - 个人资料页
 *
 * 核心功能：
 * - 统计卡片展示：学习天数、累计学时、账号名称
 * - 个人资料编辑：昵称（必填）、个性签名、头像 URL
 * - 头像展示：优先使用图片 URL，无图时显示昵称首字
 * - 保存调用 updateUserProfileApi 并同步到 userStore
 * - 退出登录：调用 logoutApi 清除服务端会话 + 前端清除 token 并跳转到登录页
 * - 展示笔记列表（来自 noteStore），支持删除
 */
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/common/StatCard.vue'
import { useUserStore } from '@/stores/user'
import { useNoteStore } from '@/stores/notes'
import { updateUserProfileApi, logoutApi } from '@/api/user'
import { Timer, Calendar, Edit, Delete, Collection } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const noteStore = useNoteStore()
/** 保存按钮 loading 状态 */
const saving = ref(false)
/** 表单组件引用 */
const formRef = ref()

/** 个人资料表单 */
const form = reactive({
  nickname: '',
  signature: '',
  avatar: '',
})

/** 表单校验规则：昵称为必填 */
const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

/** 页面挂载：拉取用户信息并回填表单 */
onMounted(async () => {
  await userStore.fetchProfile()
  Object.assign(form, {
    nickname: userStore.userInfo.nickname,
    signature: userStore.userInfo.signature,
    avatar: userStore.userInfo.avatar,
  })
})

/**
 * 保存个人资料
 * 1. 触发表单校验
 * 2. 合并原有 userInfo 与表单数据，调用后端 API
 * 3. 将响应数据更新到 userStore
 */
async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const res = await updateUserProfileApi({
      ...userStore.userInfo,
      ...form,
    })
    userStore.updateProfile(res.data)
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

/**
 * 退出登录
 * 1. 调用后端 logoutApi（失败也继续前端退出）
 * 2. 清除 frontend 登录状态（userStore.logout）
 * 3. 跳转到登录页
 */
async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // 无论后端是否成功响应，都确保前端退出本地登录态
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="page-container profile">
    <!-- 统计卡片行：学习天数 / 累计学时 / 账号 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <StatCard label="学习天数" :value="userStore.userInfo.studyDays ?? 0" unit="天" :icon="Calendar" color="#3b82f6" />
      </el-col>
      <el-col :xs="24" :sm="8">
        <StatCard label="累计学时" :value="userStore.userInfo.totalHours ?? 0" unit="小时" :icon="Timer" color="#8b5cf6" />
      </el-col>
      <el-col :xs="24" :sm="8">
        <StatCard label="账号" :value="userStore.userInfo.username || 'admin'" :icon="Edit" color="#22c55e" />
      </el-col>
    </el-row>

    <!-- 个人资料编辑卡片 -->
    <el-card class="glass-card profile__card" shadow="never">
      <template #header>个人资料</template>
      <div class="profile__main">
        <!-- 头像：图片 URL 优先，否则显示昵称首字 -->
        <el-avatar :size="96" :src="form.avatar || userStore.userInfo.avatar" :alt="(form.nickname || userStore.userInfo.nickname) + '的头像'">
          {{ form.nickname?.[0] || '学' }}
        </el-avatar>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="profile__form">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input v-model="form.signature" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="头像 URL">
            <el-input v-model="form.avatar" placeholder="可选，输入头像图片地址" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
            <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- AI 学习笔记卡片 -->
    <el-card class="glass-card profile__notes" shadow="never">
      <template #header>
        <div class="notes-header">
          <span><el-icon><Collection /></el-icon> AI 学习笔记</span>
          <el-tag size="small" effect="plain">{{ noteStore.notes.length }} 条</el-tag>
        </div>
      </template>

      <el-empty v-if="!noteStore.notes.length" description="还没有笔记，快去 AI 助手聊聊吧" />

      <div v-else class="notes-list">
        <div v-for="note in noteStore.notes" :key="note.id" class="note-item glass-card">
          <!-- 笔记内容预览（最多 4 行） -->
          <div class="note-item__content">{{ note.content }}</div>
          <div class="note-item__footer">
            <span class="note-item__time">{{ note.time }}</span>
            <el-button type="danger" link :icon="Delete" @click="noteStore.removeNote(note.id)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.stat-row {
  margin-bottom: 20px;
}

.profile__main {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.profile__form {
  flex: 1;
  min-width: 280px;
}

.profile__notes {
  margin-top: 20px;
}

.notes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  span {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.notes-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.note-item {
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  
  &__content {
    font-size: 14px;
    line-height: 1.6;
    color: var(--color-text-primary);
    display: -webkit-box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  
  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid var(--color-border);
    padding-top: 8px;
  }
  
  &__time {
    font-size: 12px;
    color: var(--color-text-muted);
  }
}
</style>
