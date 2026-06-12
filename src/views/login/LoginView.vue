<script setup>
/**
 * LoginView - 登录/注册页
 *
 * 核心功能：
 * - 双标签切换（登录/注册），表单校验与提交
 * - 粒子背景动画（可开关），科技感品牌展示区
 * - 登录/注册成功后自动写入 token 与 userInfo 到 userStore，跳转到目标页
 * - 演示账号 admin/123456，方便快速体验
 */
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Trophy, DataAnalysis, MagicStick } from '@element-plus/icons-vue'
import ParticleBackground from '@/components/common/ParticleBackground.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { loginApi, registerApi } from '@/api/user'
import heroImage from '@/assets/hero.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

/** 按钮 loading 状态，防止重复提交 */
const loading = ref(false)
/** 当前激活的标签页：'login' 或 'register' */
const activeTab = ref('login')

/** 登录表单数据，预填演示账号方便体验 */
const loginForm = reactive({
  username: 'admin',
  password: '123456',
})

/** 注册表单数据，昵称为选填项，默认使用账号 */
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

/** 登录表单校验规则：账号 3-20 位，密码 6-32 位 */
const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
}

/** 注册表单校验规则：在登录规则基础上增加二次密码确认和昵称长度校验 */
const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      /** 自定义校验器：确保两次输入密码一致 */
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  nickname: [
    { max: 50, message: '昵称长度不能超过 50', trigger: 'blur' },
  ],
}

/** 表单组件引用，用于调用 validate 等 Element Plus 表单方法 */
const loginFormRef = ref()
const registerFormRef = ref()
/** 根据当前 tab 自动返回对应的表单引用 */
const currentFormRef = computed(() => (activeTab.value === 'login' ? loginFormRef : registerFormRef))

/** 品牌展示区域亮点数据：成就系统、数据洞察、AI 陪练 */
const highlights = [
  { title: '成就激励', desc: '自动同步学习成果，形成可视化成长路径', icon: Trophy },
  { title: '数据洞察', desc: '趋势分析与热力图帮助你看清学习节奏', icon: DataAnalysis },
  { title: 'AI 陪练', desc: '智能问答与学习建议提高复盘效率', icon: MagicStick },
]

/**
 * 处理登录提交
 * 1. 先触发表单校验
 * 2. 调用登录 API
 * 3. 将返回的 token + userInfo 写入 store
 * 4. 跳转到 redirect 地址或默认仪表盘
 */
async function handleLogin() {
  await loginFormRef.value?.validate()
  loading.value = true
  try {
    const res = await loginApi(loginForm)
    // 将认证信息持久化到 Pinia store（pinia-plugin-persistedstate 自动同步 localStorage）
    userStore.setLogin({
      token: res.data.token,
      userInfo: res.data.userInfo,
    })
    ElMessage.success('登录成功')
    // 支持登录后重定向回原目标页
    const redirect = route.query.redirect || '/dashboard'
    router.push(redirect)
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}

/**
 * 处理注册提交
 * 1. 先校验表单
 * 2. 调用注册 API，后端返回 token 即表示自动登录
 * 3. 写入 store 并跳转仪表盘
 */
async function handleRegister() {
  await registerFormRef.value?.validate()
  loading.value = true
  try {
    const res = await registerApi(registerForm)
    // 注册成功后自动登录：后端直接返回 token
    userStore.setLogin({
      token: res.data.token,
      userInfo: res.data.userInfo,
    })
    ElMessage.success('注册成功，已为你自动登录')
    router.push('/dashboard')
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}

/** 根据当前 tab 分发到登录或注册处理函数 */
function handleSubmit() {
  return activeTab.value === 'login' ? handleLogin() : handleRegister()
}
</script>

<template>
  <div class="login-page tech-grid-bg">
    <!-- 粒子背景动画，根据主题开关控制 -->
    <ParticleBackground v-if="themeStore.showParticles" color="59, 130, 246" :count="80" />
    <!-- 顶部光晕装饰 -->
    <div class="login-page__glow" />
    <div class="login-shell">
      <!-- 左侧品牌展示区 -->
      <section class="login-showcase glass-card" aria-label="产品介绍">
        <div class="login-showcase__badge">AI Learning Growth Studio</div>
        <h1 class="gradient-text login-showcase__title">更有沉浸感的学习空间，更清晰可见的成长轨迹</h1>
        <p class="login-showcase__desc">
          聚合学习计划、课程、笔记、错题、资源与 AI 助手，把碎片努力沉淀成连续进步。
        </p>
        <img class="login-showcase__hero" :src="heroImage" alt="AI 学习成长助手视觉插图" />
        <!-- 产品亮点三列 -->
        <div class="login-showcase__highlights">
          <div v-for="item in highlights" :key="item.title" class="login-showcase__item">
            <div class="login-showcase__icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div>
              <div class="login-showcase__item-title">{{ item.title }}</div>
              <div class="login-showcase__item-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧登录/注册卡片 -->
      <el-card class="login-card gradient-border-card" shadow="never" role="main">
        <!-- 品牌标识与标题 -->
        <div class="login-card__brand">
          <span class="login-card__brand-mark">AI</span>
          <div>
            <h2 class="gradient-text login-card__title">
              {{ activeTab === 'login' ? '欢迎登录' : '创建账号' }}
            </h2>
            <p class="login-card__subtitle">
              {{ activeTab === 'login' ? '进入你的智能学习控制台' : '开启你的智能学习之旅' }}
            </p>
          </div>
        </div>
        <!-- Tab 切换栏 -->
        <div class="login-card__tabs">
          <button
            type="button"
            class="login-card__tab"
            :class="{ 'is-active': activeTab === 'login' }"
            @click="activeTab = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            class="login-card__tab"
            :class="{ 'is-active': activeTab === 'register' }"
            @click="activeTab = 'register'"
          >
            注册
          </button>
        </div>

        <!-- 登录表单 -->
        <el-form
          v-if="activeTab === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="账号" prop="username">
            <el-input v-model="loginForm.username" placeholder="admin" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="123456" size="large" show-password />
          </el-form-item>
          <el-button type="primary" size="large" class="login-card__btn" :loading="loading" native-type="submit">
            登 录
          </el-button>
        </el-form>

        <!-- 注册表单 -->
        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          @submit.prevent="handleRegister"
        >
          <el-form-item label="账号" prop="username">
            <el-input v-model="registerForm.username" placeholder="3-20 位，支持中英文与数字" size="large" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="选填，默认使用账号" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="6-32 位" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
          </el-form-item>
          <el-button type="primary" size="large" class="login-card__btn" :loading="loading" native-type="submit">
            注 册
          </el-button>
        </el-form>

        <!-- 底部提示：演示账号 / 用户协议 / 功能说明 -->
        <div class="login-card__footer">
          <p v-if="activeTab === 'login'" class="login-card__hint">
            演示账号：admin / 123456
          </p>
          <p v-else class="login-card__hint">
            注册即表示同意用户协议与隐私政策
          </p>
          <p class="login-card__meta">支持深浅主题、成就系统、数据看板与 AI 学习辅助</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-page__glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.2), transparent 70%);
  top: 20%;
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 0;
}

.login-shell {
  position: relative;
  z-index: 2;
  width: min(1120px, calc(100% - 48px));
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 420px;
  gap: 24px;
  align-items: stretch;
}

.login-showcase {
  padding: 34px;
  border-radius: 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    radial-gradient(circle at top left, rgba(99, 102, 241, 0.22), transparent 28%),
    radial-gradient(circle at bottom right, rgba(14, 165, 233, 0.18), transparent 30%);
}

.login-showcase__badge {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.14);
  color: #c7d2fe;
  margin-bottom: 18px;
}

.login-showcase__title {
  font-size: 38px;
  line-height: 1.2;
}

.login-showcase__desc {
  margin-top: 14px;
  max-width: 620px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}

.login-showcase__hero {
  width: 320px;
  max-width: 100%;
  margin: 18px auto 12px 0;
  filter: drop-shadow(0 20px 48px rgba(99, 102, 241, 0.25));
}

.login-showcase__highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.login-showcase__item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.login-showcase__icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  flex-shrink: 0;
}

.login-showcase__item-title {
  color: var(--color-text-primary);
  font-weight: 600;
}

.login-showcase__item-desc {
  margin-top: 6px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: 13px;
}

.login-card {
  position: relative;
  width: 420px;
  max-width: 100%;
  padding: 18px 10px 10px;
  border-radius: 28px;
}

.login-card__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.login-card__brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #06b6d4);
  color: #fff;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.login-card__title {
  font-size: 28px;
  margin-bottom: 6px;
}

.login-card__subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-card__btn {
  width: 100%;
  margin-top: 12px;
  height: 44px;
  border-radius: 14px;
}

.login-card__tabs {
  display: flex;
  gap: 8px;
  padding: 6px;
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}

.login-card__tab {
  flex: 1;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-card__tab:hover {
  color: var(--color-text-primary);
}

.login-card__tab.is-active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.login-card__footer {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.login-card__hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.login-card__meta {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-showcase__highlights {
    grid-template-columns: 1fr;
  }

  .login-card {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: 24px 0;
  }

  .login-showcase {
    padding: 24px;
  }

  .login-showcase__title {
    font-size: 30px;
  }
}
</style>
