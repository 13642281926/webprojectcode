# AI 学习成长助手平台 —— 系统设计说明书

> **项目名称**：AI 学习成长助手平台（AI Learning Assistant Platform）  
> **所属课程**：j3225706《信息系统综合实训》2025-2026-2  
> **技术路线**：Spring Boot + Vue 3 + MyBatis-Plus + MySQL + Redis + Element Plus  
> **文档版本**：v1.0  
> **编写日期**：2025-06-04  

---

## 一、引言

### 1.1 设计目标

本文档对 AI 学习成长助手平台进行全面的系统设计，涵盖系统架构、前后端模块详细设计、数据库设计、安全设计、部署方案及测试策略。设计遵循以下目标：

1. **完整性**：覆盖从浏览器到数据库的全部技术层次
2. **可追溯性**：每个设计决策可追溯至需求规格说明书的对应条目
3. **可实施性**：设计粒度足够支撑 2 周开发周期内的编码实现
4. **规范性**：遵循 MVC 分层架构、RESTful API 规范、阿里巴巴 Java 开发手册

### 1.2 设计范围

| 维度 | 范围 |
|------|------|
| 系统架构 | 三层 B/S 架构（浏览器 → Nginx → Spring Boot → MySQL/Redis） |
| 前端设计 | SPA 应用（Vue 3 + Vite 8）的组件树、路由、状态管理、API 层、主题系统、性能优化 |
| 后端设计 | Spring Boot 3.x 的分层架构、RBAC 权限、JWT 认证、RESTful API |
| 数据库 | MySQL 8.0 的 ER 模型、12 张核心表 DDL、索引策略 |
| 安全 | JWT + BCrypt + RBAC + XSS/CSRF/SQL 注入防护 |
| 部署 | Nginx 反向代理 + 前端静态资源 + 后端 JAR 包 |
| 测试 | JUnit 5 + MockMvc（后端）+ Vitest + Vue Test Utils（前端） |

### 1.3 设计原则

1. **前后端分离**：前端 SPA 与后端 RESTful API 独立开发、独立部署，通过 HTTP/JSON 通信
2. **分层架构**：后端严格遵循 Controller → Service → Mapper 三层，层间单向依赖
3. **安全优先**：认证（JWT）、授权（RBAC）、传输加密（HTTPS）、存储加密（BCrypt）、输入防注入
4. **组件复用**：前端通用组件（StatCard、ChartCard、AIChatBox 等）跨页面复用，保持一致的 UI 风格
5. **渐进增强**：核心功能优先实现，高级特性（如 Redis 缓存、AOP 日志）在基础功能稳定后叠加

### 1.4 参考文档

- 《信息系统综合实训》论文（设计）任务书
- 附件 B：《信息系统综合实训》选题示例
- 附件 C：项目开发规范 — 阿里巴巴 Java 开发手册
- 附件 A：广东海洋大学本科生毕业论文（设计）撰写规范
- Vue 3 官方文档：https://cn.vuejs.org/
- Spring Boot 官方文档：https://spring.io/projects/spring-boot
- MyBatis-Plus 官方文档：https://baomidou.com/
- Element Plus 官方文档：https://element-plus.org/

---

## 二、系统架构设计

### 2.1 整体架构

系统采用 **B/S 三层架构**：

```
┌────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                        │
│  ┌──────────────────────────────────────────────────────┐      │
│  │           Vue 3 SPA (Vite 构建, Element Plus UI)      │      │
│  │    Login │ Dashboard │ StudyPlan │ Course │ AI │ ...  │      │
│  └──────────────────────┬───────────────────────────────┘      │
└─────────────────────────┼──────────────────────────────────────┘
                          │ HTTP/REST (JSON)
                          ▼
┌────────────────────────────────────────────────────────────────┐
│                      中间层 / 网关 (Gateway)                     │
│  ┌──────────────────────────────────────────────────────┐      │
│  │           Nginx (反向代理 + 静态资源 + Gzip)            │      │
│  │   /           → dist/index.html                       │      │
│  │   /api/*      → http://127.0.0.1:8080/api/*           │      │
│  └──────────────────────────────────────────────────────┘      │
└─────────────────────────┼──────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────┐
│                       应用服务层 (Application)                   │
│  ┌──────────────────────────────────────────────────────┐      │
│  │                Spring Boot 3.x (Tomcat)                │      │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐    │      │
│  │  │Controller│→│ Service  │→│ Mapper (MyBatis+) │    │      │
│  │  │ (REST)   │  │ (Logic)  │  │ (Data Access)     │    │      │
│  │  └──────────┘  └──────────┘  └────────┬─────────┘    │      │
│  │  ┌──────────────────────────────────┐ │              │      │
│  │  │    Security (Spring Security)    │ │              │      │
│  │  │    JWT Filter + RBAC + BCrypt    │ │              │      │
│  │  └──────────────────────────────────┘ │              │      │
│  └───────────────────────────────────────┼──────────────┘      │
└─────────────────────────────────────────┼──────────────────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
┌──────────────────────┐  ┌──────────────────────┐
│   数据持久层 (Data)    │  │   缓存层 (Cache)      │
│  ┌────────────────┐  │  │  ┌────────────────┐  │
│  │   MySQL 8.0    │  │  │  │   Redis 7.0    │  │
│  │   (业务数据)    │  │  │  │ (权限缓存+黑名单)│  │
│  └────────────────┘  │  │  └────────────────┘  │
└──────────────────────┘  └──────────────────────┘
```

### 2.2 技术栈总览

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 核心框架（Composition API + `<script setup>`） |
| Vite | 8.0+ | 构建工具与开发服务器 |
| Vue Router | 5.0+ | SPA 路由管理，含懒加载与导航守卫 |
| Pinia | 3.0+ | 状态管理（用户/主题/学习计划） |
| Element Plus | 2.14+ | UI 组件库（按需引入 + 自动导入） |
| ECharts | 6.1+ | 数据可视化（按需引入 4 种图表 + 热力图） |
| Axios | 1.16+ | HTTP 请求封装（拦截器 + Loading 管理） |
| Sass | 1.100+ | CSS 预处理器（变量 + 混入 + 双主题） |
| MockJS | 1.1+ | 开发环境 Mock 数据（vite-plugin-mock） |
| Lodash | 4.18+ | 工具函数（debounce 等） |

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.x | 核心框架（自动配置 + 嵌入式 Tomcat） |
| Spring Security | 6.x | 安全框架（认证 + 授权） |
| MyBatis-Plus | 3.5+ | ORM 框架（BaseMapper + LambdaQueryWrapper） |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存（权限缓存 + Token 黑名单） |
| jjwt | 0.12+ | JWT Token 签发与验证 |
| SpringDoc / Knife4j | 4.x | API 文档自动生成（OpenAPI 3.0） |
| Maven | 3.9+ | 项目构建与依赖管理 |
| Lombok | 1.18+ | 简化 POJO 代码 |

### 2.3 后端分层架构

```
src/main/java/com/aiplatform/
├── controller/          # 控制器层：接收 HTTP 请求，参数校验，调用 Service
│   ├── AuthController.java
│   ├── UserController.java
│   ├── DashboardController.java
│   ├── StudyPlanController.java
│   ├── CourseController.java
│   ├── AiController.java
│   ├── AnalyticsController.java
│   └── system/
│       ├── SysUserController.java
│       ├── SysRoleController.java
│       └── SysMenuController.java
├── service/             # 业务逻辑层：核心业务处理，事务管理
│   ├── AuthService.java
│   ├── UserService.java
│   ├── StudyPlanService.java
│   ├── CourseService.java
│   ├── AiService.java
│   ├── AnalyticsService.java
│   └── system/
│       ├── SysUserService.java
│       ├── SysRoleService.java
│       └── SysMenuService.java
├── mapper/              # 数据访问层：MyBatis-Plus BaseMapper
├── entity/              # 数据库实体类（POJO）
├── dto/                 # 数据传输对象（请求/响应 DTO）
├── config/              # 配置类
│   ├── SecurityConfig.java       # Spring Security 配置
│   ├── MyBatisPlusConfig.java    # MyBatis-Plus 分页插件
│   ├── RedisConfig.java          # Redis 配置
│   ├── CorsConfig.java           # CORS 跨域配置
│   └── SwaggerConfig.java        # Knife4j 配置
├── security/            # 安全组件
│   ├── JwtTokenProvider.java     # JWT 生成/验证
│   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   └── UserDetailsServiceImpl.java   # 用户详情加载
├── common/              # 公共模块
│   ├── Result.java              # 统一响应封装
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   ├── BusinessException.java   # 业务异常
│   └── constants/               # 常量定义
└── annotation/          # 自定义注解
    └── Log.java                 # AOP 操作日志注解
```

### 2.4 前端架构

#### 组件树结构

```
App.vue
└── <router-view> (过渡动画)
    ├── LoginView.vue (/login)
    │   └── ParticleBackground.vue
    └── AppLayout.vue (/)
        ├── AppSidebar.vue (侧边栏导航)
        ├── AppHeader.vue (顶部栏 + 面包屑)
        │   └── [主题切换, 粒子开关, 用户信息]
        └── <router-view> (keep-alive 缓存)
            ├── DashboardView.vue (/dashboard)
            │   ├── StatCard.vue ×3
            │   ├── ChartCard.vue ×2 (折线图, 柱状图)
            │   ├── QuickEntry.vue
            │   └── el-timeline
            ├── StudyPlanView.vue (/study-plan)
            │   ├── el-table / el-card-grid
            │   ├── el-dialog (新增/编辑表单)
            │   └── el-pagination
            ├── CourseView.vue (/course)
            │   ├── CourseCard.vue (含 LazyImage)
            │   │   └── LazyImage.vue
            │   └── el-drawer (含 el-timeline)
            ├── AiAssistantView.vue (/ai-assistant)
            │   └── AIChatBox.vue
            ├── AnalyticsView.vue (/analytics)
            │   ├── StatCard.vue ×3
            │   └── ChartCard.vue ×5
            └── ProfileView.vue (/profile)
                └── el-form (资料编辑)
```

---

## 三、前端详细设计

### 3.1 路由设计

系统共设计 **7 条路由**（1 条公开路由 + 6 条受保护路由 + 1 条通配 404）：

| 路径 | 路由名称 | 页面标题 | 图标 | 懒加载 Chunk | 认证要求 |
|------|---------|---------|------|-------------|---------|
| `/login` | Login | 登录 | - | `login` | 否 |
| `/dashboard` | Dashboard | 首页 | Odometer | `dashboard` | 是 |
| `/study-plan` | StudyPlan | 学习计划 | Calendar | `study-plan` | 是 |
| `/course` | Course | 课程管理 | Reading | `course` | 是 |
| `/ai-assistant` | AiAssistant | AI 助手 | ChatDotRound | `ai-assistant` | 是 |
| `/analytics` | Analytics | 数据分析 | DataAnalysis | `analytics` | 是 |
| `/profile` | Profile | 个人中心 | User | `profile` | 是 |
| `/:pathMatch(.*)*` | - | 404 | - | - | - |

**导航守卫逻辑**（`router/index.js` beforeEach）：

```
用户访问任意路由
  ├─ 未认证 + 需要认证 → 重定向 /login?redirect=<原始路径>
  ├─ 已认证 + 访问 /login → 重定向 /dashboard
  └─ 其他情况 → 放行
```

路由懒加载使用 Vite 的动态 `import()`，按页面拆分 chunk，配合 `keep-alive` 缓存已访问页面组件，避免重复挂载和数据请求。

### 3.2 状态管理设计

系统使用 Pinia 管理三种全局状态：

#### useUserStore（用户状态）

```javascript
state: {
  token: ref(null),           // JWT Token 字符串
  userInfo: ref({             // 用户资料对象
    id, username, nickname,
    avatar, signature,
    studyDays, totalHours
  }),
  profileLoading: ref(false)
}
getters: {
  isLoggedIn: computed(() => Boolean(token.value))
}
actions: {
  setLogin(payload),          // 登录成功：存储 token + userInfo
  logout(),                   // 退出登录：清除所有状态
  updateProfile(partial),     // 局部更新用户资料
  fetchProfile()              // 从后端拉取最新资料
}
persist: localStorage (key: 'ai-learning-user')
```

#### useThemeStore（主题状态）

```javascript
state: {
  isDark: ref(true),          // 深色/浅色模式
  showParticles: ref(true),   // 粒子背景开关
  sidebarCollapsed: ref(false) // 侧边栏折叠
}
actions: {
  toggleTheme(),              // 切换主题 → 更新 <html data-theme>
  toggleParticles(),
  toggleSidebar()
}
persist: localStorage (key: 'ai-learning-theme')
```

#### useStudyPlanStore（学习计划状态）

```javascript
state: {
  plans: ref([]),             // 当前页计划列表
  total: ref(0),              // 符合条件的总条数
  loading: ref(false),
  query: ref({ page:1, pageSize:10, priority:'', status:'', keyword:'' })
}
getters: {
  pendingCount: computed(...),
  doneCount: computed(...)
}
actions: {
  fetchPlans(params),         // 分页查询
  addPlan(data),              // 创建计划
  editPlan(id, data),         // 更新计划
  removePlan(id),             // 删除计划
  toggleStatus(id)            // 状态循环切换
}
cache: localStorage (key: 'ai-learning-plans-cache')
```

### 3.3 API 层设计

**Axios 实例配置**（`src/api/request.js`）：

```javascript
const request = axios.create({
  baseURL: '/api',            // 通过 Vite proxy 转发至后端
  timeout: 15000,             // 15 秒超时
})
```

**请求拦截器**：
1. 自动注入 `Authorization: Bearer <token>` 头部
2. 自动显示全局 Loading（可配置禁用：`showLoading: false`）
3. 可配置 Loading 文字：`loadingText: '自定义文字'`

**响应拦截器**：
1. 关闭全局 Loading
2. 处理 `code === 401`：清除登录态 → 显示提示 → 跳转登录页
3. 处理 `code !== 200`：显示错误消息（支持静默模式：`silent: true`）
4. HTTP 状态码 401/403/500 的统一错误处理

**API 模块文件**（与后端一一对应）：

| 文件 | 接口数量 | 对应后端 Controller |
|------|---------|-------------------|
| `api/user.js` | 3 | AuthController + UserController |
| `api/dashboard.js` | 1 | DashboardController |
| `api/studyPlan.js` | 4 | StudyPlanController |
| `api/course.js` | 2 | CourseController |
| `api/ai.js` | 2 | AiController |
| `api/analytics.js` | 1 | AnalyticsController |

> ⚠ **前后端对接关键约束**：后端响应的 JSON 格式必须为 `{ code: 200, message: "ok", data: {...} }`。前端 `request.js` 响应拦截器通过 `res.code !== 200` 判断业务错误，若格式不一致将导致所有页面报错。

### 3.4 通用组件接口

#### StatCard.vue

| Props | 类型 | 必填 | 说明 |
|-------|------|------|------|
| label | String | 是 | 指标名称 |
| value | Number/String | 是 | 指标数值 |
| unit | String | 否 | 单位后缀 |
| icon | Component | 否 | Element Plus 图标组件 |
| color | String | 否 | 强调色（图标背景 + 数值渐变） |
| clickable | Boolean | 否 | 是否可点击（hover 抬升效果） |

| 插槽 | 说明 |
|------|------|
| default | 自定义数值区域 |
| footer | 底部扩展区域 |

#### ChartCard.vue

| Props | 类型 | 必填 | 说明 |
|-------|------|------|------|
| title | String | 是 | 图表标题 |
| option | Object | 是 | ECharts 配置项 |
| height | String | 否 | 图表高度（默认 '300px'） |
| loading | Boolean | 否 | 加载状态 |

> 组件内部通过 `inject('themeConfig')` 获取主题配置，深色/浅色切换时自动销毁并重绘图表以更新配色。

#### AIChatBox.vue

| Props | 类型 | 必填 | 说明 |
|-------|------|------|------|
| messages | Array | 是 | 消息列表 `[{ id, role, content, time }]` |
| quickQuestions | Array | 否 | 快捷问题列表 |
| loading | Boolean | 否 | 等待 AI 回复状态 |

| Emits | 参数 | 说明 |
|-------|------|------|
| send | (message: string) | 用户发送消息 |
| quick | (question: string) | 点击快捷问题 |

#### CourseCard.vue / LazyImage.vue / QuickEntry.vue / ParticleBackground.vue

详细接口说明参见 `docs/COMPONENTS.md`。

### 3.5 主题系统设计

系统支持深色（默认）和浅色两种主题，通过 CSS 变量体系实现：

```
:root {                                    /* 深色主题（默认） */
  --bg-primary: #0a0e1a;                   /* 主背景 */
  --bg-secondary: #111827;                 /* 次级背景 */
  --bg-card: rgba(17, 24, 39, 0.8);       /* 卡片背景（玻璃效果） */
  --brand-primary: #6366f1;                /* 品牌色（靛蓝） */
  --brand-secondary: #8b5cf6;              /* 辅助色（紫） */
  --text-primary: #f1f5f9;                 /* 主文字 */
  --text-secondary: #94a3b8;               /* 次级文字 */
  ...
}

[data-theme="light"] {                     /* 浅色主题 */
  --bg-primary: #f8fafc;
  --bg-secondary: #ffffff;
  --bg-card: rgba(255, 255, 255, 0.9);
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  ...
}
```

主题切换流程：`toggleTheme()` → 更新 `isDark` → 更新 `<html data-theme>` → Element Plus `.dark` 类切换 → ECharts 图表实例销毁重绘

### 3.6 性能优化策略

| 优化项 | 实现方式 | 效果 |
|--------|---------|------|
| 路由懒加载 | Vite 动态 `import()` + webpackChunkName | 首屏仅加载当前路由 chunk |
| ECharts 按需引入 | 仅注册 LineBarPieHeatmap + CanvasRenderer | 包体积减少 ~40% |
| 图片懒加载 | `v-lazy` 指令（IntersectionObserver） + LazyImage 组件 | 首屏图片延迟加载 |
| 页面缓存 | `<keep-alive>` 包裹 `<router-view>` | 切换页面不重新挂载和数据请求 |
| 搜索防抖 | lodash-es `debounce(fn, 400ms)` | 减少不必要的请求 |
| Gzip/Brotli 压缩 | vite-plugin-compression 构建时预压缩 | 传输体积减少 60-70% |
| Element Plus 按需引入 | unplugin-vue-components 自动导入 | 仅打包使用到的组件 |

---

## 四、后端详细设计

### 4.1 RBAC 权限模型设计

采用经典 **RBAC（Role-Based Access Control）五表模型**：

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   sys_user   │       │   sys_role   │       │   sys_menu   │
│──────────────│       │──────────────│       │──────────────│
│ id (PK)      │  N:M  │ id (PK)      │  N:M  │ id (PK)      │
│ username     │◄─────►│ role_name    │◄─────►│ parent_id    │
│ password     │       │ role_code    │       │ menu_name    │
│ nickname     │       │ description  │       │ path         │
│ status       │       │ status       │       │ component    │
│ ...          │       │ ...          │       │ permission   │
└──────────────┘       └──────────────┘       │ type (M/C/F) │
       │                       │              │ icon         │
       │                       │              │ sort         │
       ▼                       ▼              └──────────────┘
┌──────────────┐       ┌──────────────┐
│sys_user_role │       │sys_role_menu │
│──────────────│       │──────────────│
│ user_id (FK) │       │ role_id (FK) │
│ role_id (FK) │       │ menu_id (FK) │
└──────────────┘       └──────────────┘
```

**菜单类型定义**：

| 类型编码 | 类型名称 | 说明 | 示例 |
|---------|---------|------|------|
| M | 目录 | 一级菜单分组 | 系统管理 |
| C | 菜单 | 具体页面入口 | 用户管理 |
| F | 按钮 | 页面内操作权限 | 新增用户、删除用户 |

**权限标识命名规范**：`模块:功能:操作`，例如：
- `study:plan:list` — 学习计划列表查询
- `study:plan:add` — 新增学习计划
- `study:plan:edit` — 编辑学习计划
- `study:plan:delete` — 删除学习计划
- `system:user:list` — 用户列表查询
- `system:user:add` — 新增用户

**Spring Security 集成**：

```
HTTP 请求
    │
    ▼
JwtAuthenticationFilter
    │─ 从 Authorization Header 提取 Token
    │─ 验证 Token 有效性
    │─ 从 Redis 校验 Token 是否在黑名单
    │─ 解析 userId, roles 等 Claims
    │─ 加载 GrantedAuthorities（权限列表）
    │─ 设置 SecurityContextHolder
    │
    ▼
Controller @PreAuthorize("hasAuthority('study:plan:add')")
    │─ 权限校验通过 → 执行方法
    │─ 权限校验失败 → 返回 403 Forbidden
```

### 4.2 JWT 认证流程

#### 登录流程

```
客户端                          服务端                         数据库/Redis
  │                               │                               │
  │  POST /api/user/login         │                               │
  │  { username, password }       │                               │
  │──────────────────────────────►│                               │
  │                               │  查询用户（username）          │
  │                               │──────────────────────────────►│
  │                               │◄────── 返回 sys_user 记录 ────│
  │                               │                               │
  │                               │  BCrypt.matches(password, hash)
  │                               │  验证密码                      │
  │                               │                               │
  │                               │  查询用户角色 + 权限           │
  │                               │──────────────────────────────►│
  │                               │◄────── roles + permissions ───│
  │                               │                               │
  │                               │  生成 JWT:                     │
  │                               │  { userId, username, roles }   │
  │                               │  签名算法: HS256              │
  │                               │  过期时间: 24h               │
  │                               │                               │
  │                               │  缓存权限到 Redis:             │
  │                               │  KEY: "perm:{userId}"          │
  │                               │  TTL: 24h                     │
  │                               │──────────────────────────────►│
  │                               │                               │
  │  { code:200, data: {          │                               │
  │    token: "eyJhbG...",        │                               │
  │    userInfo: { id, nickname   │                               │
  │      avatar, signature,       │                               │
  │      studyDays, totalHours }  │                               │
  │  }}                           │                               │
  │◄──────────────────────────────│                               │
```

#### 后续请求认证

```
客户端                          服务端                         数据库/Redis
  │                               │                               │
  │  GET /api/study-plan/list     │                               │
  │  Authorization: Bearer <token>│                               │
  │──────────────────────────────►│                               │
  │                               │  JwtAuthenticationFilter:      │
  │                               │  1. 提取 Token                 │
  │                               │  2. 验证签名 + 有效期          │
  │                               │  3. 检查 Redis 黑名单          │
  │                               │  4. 从 Redis 获取权限缓存      │
  │                               │  5. 设置 SecurityContext       │
  │                               │                               │
  │                               │  @PreAuthorize 权限校验        │
  │                               │  通过 → 执行业务逻辑           │
  │                               │                               │
  │  { code:200, data: {...} }    │                               │
  │◄──────────────────────────────│                               │
```

### 4.3 核心 API 详细设计

#### POST /api/user/login — 用户登录

```
Request:
{
  "username": "admin",       // 账号，3-20 字符
  "password": "123456"       // 密码，6-32 字符
}

Response (200):
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "AI 学习者",
      "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=admin",
      "signature": "专注学习，持续成长",
      "studyDays": 128,
      "totalHours": 486
    }
  }
}

Response (401):
{
  "code": 401,
  "message": "账号或密码错误",
  "data": null
}
```

#### GET /api/study-plan/list — 学习计划列表

```
Request (Query):
  page=1 & pageSize=10 & priority=high & status=pending & keyword=Vue

Response (200):
{
  "code": 200,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": "1",
        "title": "Vue3 组件化实战",
        "content": "完成 Composition API 与 Pinia 练习",
        "deadline": "2026-06-01",
        "priority": "high",
        "status": "pending",
        "createdAt": "2026-05-20"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

#### GET /api/analytics/overview — 数据分析总览

```
Response (200):
{
  "code": 200,
  "message": "ok",
  "data": {
    "totalHours": 486,            // 累计学习小时
    "completedTasks": 45,         // 完成任务数
    "courseCompletion": 68,       // 课程完成率(%)
    "weeklyHours": {              // 本周每日时长
      "labels": ["周一","周二","周三","周四","周五","周六","周日"],
      "values": [2, 4, 6, 7, 5, 3, 1]
    },
    "taskRate": {                 // 任务分布
      "done": 45,
      "doing": 12,
      "pending": 8
    },
    "courseProgress": {           // 各课程进度
      "list": [
        { "name": "Vue3 前端开发", "value": 72 },
        ...
      ]
    },
    "monthTrend": {               // 月度趋势
      "labels": ["1月","2月","3月","4月","5月","6月"],
      "values": [120, 180, 220, 260, 310, 340]
    },
    "heatmap": {                  // 学习热力图
      "data": [["2026-01-15", 45], ["2026-01-16", 90], ...]
    }
  }
}
```

> 其余 API 端点（共 20+ 个）的完整请求/响应定义参见需求文档「接口需求」章节和 Mock 实现文件（`src/mock/*.js`）。

### 4.4 全局异常处理

使用 `@RestControllerAdvice` 统一处理异常，确保所有响应遵循 `{ code, message, data }` 格式：

| 异常类型 | HTTP 状态码 | code | 说明 |
|---------|------------|------|------|
| MethodArgumentNotValidException | 400 | 400 | 参数校验失败 |
| BindException | 400 | 400 | 参数绑定失败 |
| AuthenticationException | 401 | 401 | 认证失败 |
| AccessDeniedException | 403 | 403 | 权限不足 |
| BusinessException | 400 | 自定义 | 业务逻辑异常 |
| RuntimeException | 500 | 500 | 未知服务器错误 |

### 4.5 AOP 操作日志

使用自定义 `@Log` 注解配合 AOP 切面，记录关键操作日志：

```java
@Log(module = "学习计划", operation = "新增计划")
@PostMapping
public Result createPlan(@Valid @RequestBody PlanDTO dto) { ... }
```

日志记录字段：操作用户、模块名称、操作类型、请求参数、IP 地址、操作时间、执行耗时。日志数据可存入 `sys_operation_log` 表（可选扩展）。

---

## 五、数据库设计

### 5.1 实体关系（ER）图

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   sys_user   │        │   sys_role   │        │   sys_menu   │
│──────────────│        │──────────────│        │──────────────│
│ id       PK  │──┐     │ id       PK  │──┐     │ id       PK  │──┐
│ username     │  │     │ role_name    │  │     │ parent_id    │  │ (自引用)
│ password     │  │     │ role_code UK │  │     │ menu_name    │  │
│ nickname     │  │     │ description  │  │     │ path         │  │
│ avatar       │  │     │ status       │  │     │ component    │  │
│ signature    │  │     │ create_time  │  │     │ permission   │  │
│ email        │  │     │ update_time  │  │     │ type         │  │
│ phone        │  │     └──────────────┘  │     │ icon         │  │
│ status       │  │           │           │     │ sort         │  │
│ create_time  │  │           │           │     │ status       │  │
│ update_time  │  │           │           │     │ create_time  │  │
└──────────────┘  │           │           │     │ update_time  │  │
       │          │           │           │     └──────────────┘  │
       │ 1:N      │    ┌──────┘           │            │          │
       ▼          │    │    ┌─────────────┘            │          │
┌──────────────┐  │    │    │  ┌───────────────────────┘          │
│ study_plan   │  │    │    │  │                                  │
│──────────────│  │    ▼    ▼  ▼                                  │
│ id       PK  │  │  ┌──────────────┐      ┌──────────────┐       │
│ user_id  FK  │──┘  │sys_user_role │      │sys_role_menu │       │
│ title        │     │──────────────│      │──────────────│       │
│ content      │     │ user_id  FK  │      │ role_id  FK  │       │
│ deadline     │     │ role_id  FK  │      │ menu_id  FK  │       │
│ priority     │     └──────────────┘      └──────────────┘       │
│ status       │◄─────────────────────────────────────────────────┘
│ create_time  │
│ update_time  │

┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   course    │        │study_record  │        │ai_conversation│
│──────────────│        │──────────────│        │──────────────│
│ id       PK  │        │ id       PK  │        │ id       PK  │
│ title        │        │ user_id  FK  │──┐     │ user_id  FK  │──┐
│ category     │        │ plan_id  FK  │  │     │ question     │  │
│ cover        │        │ duration     │  │     │ answer       │  │
│ description  │        │ description  │  │     │ create_time  │  │
│ teacher      │        │ create_time  │  │     └──────────────┘  │
│ lessons      │        └──────────────┘  │                       │
│ progress     │               │          │                       │
│ create_time  │               │ 1:N      │ 1:N                   │
│ update_time  │               ▼          ▼                       │
└──────┬───────┘        ┌──────────────┐  ┌──────────────┐        │
       │ 1:N            │  sys_user    │  │  sys_user    │        │
       ▼                └──────────────┘  └──────────────┘        │
┌──────────────┐                                                   │
│course_chapter│        ┌──────────────────┐                      │
│──────────────│        │user_learning_stats│                     │
│ id       PK  │        │──────────────────│                      │
│ course_id FK │        │ id           PK  │                      │
│ title        │        │ user_id      FK  │──────────────────────┘
│ duration     │        │ study_days       │
│ done         │        │ total_hours      │
│ sort         │        │ completed_tasks  │
│ create_time  │        │ create_time      │
└──────────────┘        │ update_time      │
                        └──────────────────┘
┌─────────────────┐
│course_knowledge_ │
│     point        │
│─────────────────│
│ id          PK  │
│ course_id   FK  │
│ name            │
│ create_time     │
└─────────────────┘
```

### 5.2 建表 DDL

#### sys_user（系统用户表）

```sql
CREATE TABLE sys_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username     VARCHAR(50)  NOT NULL COMMENT '用户名',
  password     VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
  nickname     VARCHAR(50)  DEFAULT '学习者' COMMENT '昵称',
  avatar       VARCHAR(500) COMMENT '头像 URL',
  signature    VARCHAR(200) COMMENT '个性签名',
  email        VARCHAR(100) COMMENT '电子邮箱',
  phone        VARCHAR(20)  COMMENT '手机号',
  status       TINYINT      DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

#### sys_role（角色表）

```sql
CREATE TABLE sys_role (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name    VARCHAR(50) NOT NULL COMMENT '角色名称',
  role_code    VARCHAR(50) NOT NULL COMMENT '角色编码',
  description  VARCHAR(200) COMMENT '角色描述',
  status       TINYINT     DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

#### sys_menu（菜单权限表）

```sql
CREATE TABLE sys_menu (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id    BIGINT       DEFAULT 0 COMMENT '父菜单 ID',
  menu_name    VARCHAR(50)  NOT NULL COMMENT '菜单名称',
  path         VARCHAR(200) COMMENT '路由路径',
  component    VARCHAR(200) COMMENT '前端组件路径',
  permission   VARCHAR(100) COMMENT '权限标识（如 study:plan:add）',
  type         CHAR(1)      NOT NULL COMMENT '类型（M=目录, C=菜单, F=按钮）',
  icon         VARCHAR(50)  COMMENT '图标名称',
  sort         INT          DEFAULT 0 COMMENT '排序号',
  status       TINYINT      DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';
```

#### sys_user_role（用户角色关联表）

```sql
CREATE TABLE sys_user_role (
  user_id  BIGINT NOT NULL COMMENT '用户 ID',
  role_id  BIGINT NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (user_id, role_id),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

#### sys_role_menu（角色菜单关联表）

```sql
CREATE TABLE sys_role_menu (
  role_id  BIGINT NOT NULL COMMENT '角色 ID',
  menu_id  BIGINT NOT NULL COMMENT '菜单 ID',
  PRIMARY KEY (role_id, menu_id),
  KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
```

#### study_plan（学习计划表）

```sql
CREATE TABLE study_plan (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT       NOT NULL COMMENT '所属用户 ID',
  title        VARCHAR(100) NOT NULL COMMENT '计划标题',
  content      TEXT         COMMENT '学习内容',
  deadline     DATE         COMMENT '截止日期',
  priority     VARCHAR(10)  DEFAULT 'medium' COMMENT '优先级（high/medium/low）',
  status       VARCHAR(10)  DEFAULT 'pending' COMMENT '状态（pending/doing/done）',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';
```

#### course（课程表）

```sql
CREATE TABLE course (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(100) NOT NULL COMMENT '课程标题',
  category     VARCHAR(20)  COMMENT '分类（frontend/cs/language）',
  cover        VARCHAR(500) COMMENT '封面图 URL',
  description  TEXT         COMMENT '课程描述',
  teacher      VARCHAR(50)  COMMENT '讲师',
  lessons      INT          DEFAULT 0 COMMENT '总课时数',
  progress     INT          DEFAULT 0 COMMENT '学习进度（0-100）',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
```

#### course_chapter（课程章节表）

```sql
CREATE TABLE course_chapter (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  course_id    BIGINT       NOT NULL COMMENT '所属课程 ID',
  title        VARCHAR(100) NOT NULL COMMENT '章节标题',
  duration     VARCHAR(20)  COMMENT '时长',
  done         TINYINT      DEFAULT 0 COMMENT '是否已完成（0=否, 1=是）',
  sort         INT          DEFAULT 0 COMMENT '排序号',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';
```

#### course_knowledge_point（课程知识点表）

```sql
CREATE TABLE course_knowledge_point (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  course_id    BIGINT       NOT NULL COMMENT '所属课程 ID',
  name         VARCHAR(50)  NOT NULL COMMENT '知识点名称',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程知识点表';
```

#### study_record（学习记录表）

```sql
CREATE TABLE study_record (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT       NOT NULL COMMENT '用户 ID',
  plan_id      BIGINT       COMMENT '关联计划 ID（可为空）',
  duration     INT          NOT NULL COMMENT '学习时长（分钟）',
  description  VARCHAR(500) COMMENT '学习内容描述',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_plan_id (plan_id),
  KEY idx_user_date (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';
```

#### ai_conversation（AI 对话记录表）

```sql
CREATE TABLE ai_conversation (
  id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT   NOT NULL COMMENT '用户 ID',
  question     TEXT     COMMENT '用户问题',
  answer       TEXT     COMMENT 'AI 回复',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话记录表';
```

#### user_learning_stats（用户学习统计表）

```sql
CREATE TABLE user_learning_stats (
  id               BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id          BIGINT NOT NULL COMMENT '用户 ID',
  study_days       INT    DEFAULT 0 COMMENT '累计学习天数',
  total_hours      INT    DEFAULT 0 COMMENT '累计学习小时数',
  completed_tasks  INT    DEFAULT 0 COMMENT '完成任务数',
  create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学习统计表';
```

### 5.3 索引策略

| 表名 | 索引名 | 索引字段 | 索引类型 | 用途 |
|------|--------|---------|---------|------|
| sys_user | uk_username | username | UNIQUE | 登录名唯一性约束 + 按用户名查询 |
| sys_role | uk_role_code | role_code | UNIQUE | 角色编码唯一性约束 |
| sys_menu | idx_parent_id | parent_id | NORMAL | 菜单树查询 |
| sys_user_role | idx_role_id | role_id | NORMAL | 按角色查用户 |
| sys_role_menu | idx_menu_id | menu_id | NORMAL | 按菜单查角色 |
| study_plan | idx_user_id | user_id | NORMAL | 按用户查计划（最常用查询） |
| study_plan | idx_status | status | NORMAL | 按状态筛选 |
| study_plan | idx_priority | priority | NORMAL | 按优先级筛选 |
| course_chapter | idx_course_id | course_id | NORMAL | 按课程查章节 |
| study_record | idx_user_id | user_id | NORMAL | 按用户查记录 |
| study_record | idx_user_date | (user_id, create_time) | COMPOSITE | 按用户+日期聚合统计（Dashboard/Analytics） |
| ai_conversation | idx_user_id | user_id | NORMAL | 按用户查对话历史 |
| user_learning_stats | uk_user_id | user_id | UNIQUE | 每用户一条统计记录 |

### 5.4 MyBatis-Plus 实现要点

1. **实体类映射**：使用 `@TableName` 指定表名，`@TableId(type = IdType.AUTO)` 配置自增主键，`@TableField` 配置字段映射
2. **自动填充**：`create_time` 和 `update_time` 通过 `MetaObjectHandler` 自动填充，无需手动设置
3. **分页查询**：配置 `MybatisPlusInterceptor` 添加 `PaginationInnerInterceptor`，使用 `Page<T>` 和 `IPage<T>` 实现分页
4. **条件构造**：使用 `LambdaQueryWrapper` 构建动态查询条件（如学习计划的 priority/status/keyword 组合筛选）
5. **逻辑删除**：`@TableLogic` 注解实现软删除（如角色、菜单的删除操作）

---

## 六、安全设计

### 6.1 认证安全

| 安全措施 | 实现方式 |
|---------|---------|
| 密码存储 | BCrypt 加密（强度因子 = 10），不可逆 |
| Token 生成 | JWT，HS256 签名算法，密钥配置于 application.yml（生产环境使用环境变量） |
| Token 过期 | 24 小时，过期后需重新登录 |
| Token 刷新 | 过期前 1 小时内可调用刷新接口，返回新 Token，旧 Token 加入黑名单 |
| Token 注销 | 退出登录时将 Token 加入 Redis 黑名单，TTL = Token 剩余有效期 |
| 防暴力破解 | 连续 5 次登录失败后锁定账号 30 分钟（通过 Redis 记录失败次数） |

### 6.2 授权安全

| 安全措施 | 实现方式 |
|---------|---------|
| 角色权限 | RBAC 五表模型，用户 → 角色 → 菜单/权限 |
| 接口鉴权 | Spring Security + @PreAuthorize("hasAuthority('permission')") |
| 数据隔离 | 非管理员用户只能访问自己的数据（通过 JWT 中的 userId 过滤） |
| 权限缓存 | 登录时将用户权限列表存入 Redis，减少数据库查询 |

### 6.3 常见攻击防护

| 攻击类型 | 防护措施 |
|---------|---------|
| SQL 注入 | MyBatis-Plus 默认参数化查询；代码审查禁止字符串拼接 SQL |
| XSS 跨站脚本 | Vue 默认转义输出；后端输入过滤；Content-Type 正确设置 |
| CSRF 跨站请求伪造 | JWT Token 机制（非 Cookie）；SameSite Cookie 属性 |
| 敏感信息泄露 | 密码永不返回前端；Token 不记录日志；错误消息不暴露内部细节 |
| CORS 跨域攻击 | 后端配置允许的源（开发：`localhost:5173`，生产：具体域名） |

### 6.4 CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")     // 开发环境
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## 七、部署设计

### 7.1 开发环境

```
┌─────────────┐     ┌─────────────────┐     ┌──────────┐
│ Vite Dev    │────►│ Vite Proxy      │────►│ Spring   │
│ Server      │     │ /api → :8080    │     │ Boot     │
│ :5173       │     │                 │     │ :8080    │
└─────────────┘     └─────────────────┘     └────┬─────┘
                                                 │
                                    ┌────────────┼────────────┐
                                    ▼            ▼            │
                              ┌──────────┐ ┌──────────┐       │
                              │ MySQL    │ │ Redis    │       │
                              │ :3306    │ │ :6379    │       │
                              └──────────┘ └──────────┘       │
```

**Vite 代理配置**（`vite.config.js`）：

```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

**环境要求**：

| 组件 | 版本 | 端口 |
|------|------|------|
| Node.js | ≥ 18 | - |
| JDK | ≥ 17 | - |
| MySQL | 8.0+ | 3306 |
| Redis | 7.0+ | 6379 |

### 7.2 生产环境

```
┌────────────────────────────────────────┐
│              Nginx (:80/443)            │
│  /           → /usr/share/nginx/html/  │
│  /api/*      → http://127.0.0.1:8080   │
│  gzip_static on (预压缩 .gz/.br)        │
└────────────┬───────────────────────────┘
             │
    ┌────────┴────────┐
    ▼                 ▼
┌──────────┐   ┌──────────────┐
│ Spring   │   │ 静态资源目录   │
│ Boot JAR │   │ dist/ (Vite   │
│ :8080    │   │  build 产物)   │
└────┬─────┘   └──────────────┘
     │
┌────┴────┐
▼         ▼
┌──────┐ ┌──────┐
│MySQL │ │Redis │
└──────┘ └──────┘
```

**Nginx 核心配置**：

```nginx
server {
    listen 80;
    server_name example.com;

    # 前端静态资源
    root /usr/share/nginx/html;
    index index.html;
    
    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 预压缩静态文件
    gzip_static on;
    
    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 7.3 构建流程

**前端构建**：

```bash
npm run build          # 输出至 dist/（含 .gz/.br 预压缩文件）
```

**后端构建**：

```bash
mvn clean package -DskipTests    # 输出 target/app.jar
```

**启动后端**：

```bash
java -jar app.jar --spring.profiles.active=prod
```

---

## 八、测试设计

### 8.1 测试层次

```
         ┌──────────────────────────┐
         │  E2E 测试（Postman / REST）│  ← API 端到端验证
         ├──────────────────────────┤
         │  集成测试（MockMvc）       │  ← Controller + Service 联动
         ├──────────────────────────┤
         │  单元测试（JUnit 5）       │  ← Service / Mapper 方法级
         └──────────────────────────┘
```

### 8.2 后端测试策略

| 测试层次 | 工具 | 覆盖目标 | 关键测试场景 |
|---------|------|---------|------------|
| 单元测试 | JUnit 5 + Mockito | Service 层 ≥ 70% | 业务逻辑正确性、边界条件、异常处理 |
| 集成测试 | MockMvc + Spring Test | Controller 层 ≥ 60% | API 请求/响应、JWT 认证、RBAC 权限、参数校验 |
| 数据层测试 | MyBatis-Plus Test | Mapper 层 | SQL 正确性、分页、条件构造 |

**关键测试用例**：

| 测试项 | 测试方法 | 预期结果 |
|--------|---------|---------|
| 登录成功 | POST /api/user/login，正确凭据 | 返回 200 + Token |
| 登录失败 | POST /api/user/login，错误密码 | 返回 401 |
| 无 Token 访问 | GET /api/study-plan/list，无 Header | 返回 401 |
| 权限不足 | 普通用户访问 /api/system/user/list | 返回 403 |
| 创建计划 | POST /api/study-plan，有效数据 | 返回 200 + 新计划 |
| 参数校验 | POST /api/study-plan，空 title | 返回 400 + 校验错误 |

### 8.3 前端测试策略

| 测试层次 | 工具 | 关键测试场景 |
|---------|------|------------|
| 单元测试 | Vitest | Store 方法（setLogin/logout）、工具函数（debounce/storage） |
| 组件测试 | Vue Test Utils | 登录表单校验、StatCard 渲染、路由守卫行为 |

### 8.4 API 文档与调试

- 使用 **Knife4j**（基于 SpringDoc OpenAPI 3.0）自动生成 API 文档
- 开发环境访问：`http://localhost:8080/doc.html`
- 支持在线调试：直接在 Swagger UI 中发送请求并查看响应
- 导出格式：OpenAPI JSON、Markdown、PDF

---

> **文档编制说明**：本设计说明书中的前端设计章节（第三章）基于已有代码进行规范化整理，与 `docs/COMPONENTS.md` 中的组件树、`src/api/` 中的 API 定义、`src/mock/` 中的数据结构完全对应。后端设计章节（第四章）和数据库设计章节（第五章）为全新设计，待后续开发阶段实施。
