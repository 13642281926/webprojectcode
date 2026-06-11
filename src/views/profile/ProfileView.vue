<script setup>
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
const saving = ref(false)
const formRef = ref()

const form = reactive({
  nickname: '',
  signature: '',
  avatar: '',
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

onMounted(async () => {
  await userStore.fetchProfile()
  Object.assign(form, {
    nickname: userStore.userInfo.nickname,
    signature: userStore.userInfo.signature,
    avatar: userStore.userInfo.avatar,
  })
})

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

    <el-card class="glass-card profile__card" shadow="never">
      <template #header>个人资料</template>
      <div class="profile__main">
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
