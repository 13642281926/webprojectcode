# AI 学习成长助手平台 —— 系统设计说明书

> **项目名称**：AI 学习成长助手平台（AI Learning Growth Studio）
> **所属课程**：信息系统综合实训
> **技术路线**：Spring Boot 2.7.18 + Vue 3.5 + Spring Data JPA + MySQL 8.0 + Element Plus 2.14 + DeepSeek AI
> **文档版本**：v2.1
> **编写日期**：2026-06-12

---

## 一、引言

### 1.1 设计目标

本文档对 AI 学习成长助手平台进行系统设计，涵盖系统架构、前后端模块设计、数据库设计、安全设计及部署方案。设计遵循以下目标：

1. **完整性**：覆盖从浏览器到数据库的全部技术层次，包含 45 个 API 端点、12 个前端视图、8 个 JPA 实体的完整设计
2. **可追溯性**：每个设计决策可追溯至需求规格说明书的对应条目（REQ-xxx-xxx）
3. **可实施性**：设计粒度足够支撑编码实现，所有模块均已完成代码实现
4. **规范性**：遵循 MVC 分层架构、RESTful API 规范、前端组件化设计

### 1.2 设计范围

| 维度 | 范围 |
|------|------|
| 系统架构 | B/S 架构（浏览器 → Vite Dev Server Proxy → Spring Boot → MySQL），含 AI 外部服务集成 |
| 前端设计 | 12 视图 SPA 应用（Vue 3 Composition API + Vite 5）的组件树、路由、状态管理、API 层、主题系统 |
| 后端设计 | Spring Boot 2.7.18 分层架构（10 Controller / 12 Service / 7 Repository）、JWT 认证、角色权限、RESTful API |
| 数据库 | MySQL 8.0 的 ER 模型，9 张表（8 实体 + 1 ElementCollection 映射表） |
| 安全 | JWT + 角色字段 + 数据隔离 + CORS + XSS/SQL 注入防护 |
| 部署 | 前后端分离开发环境（Vite :5173 + Spring Boot :8080 + MySQL :3307） |
| AI | DeepSeek API（deepseek-v4-pro）+ LangChain4j 0.36.2 RAG 检索增强生成 |

### 1.3 设计原则

1. **前后端分离**：前端 SPA 与后端 RESTful API 独立开发部署，通过 HTTP/JSON 通信，Vite Proxy 解决开发跨域
2. **分层架构**：后端严格遵循 Controller → Service → Repository 三层，Service 承载业务逻辑，Controller 仅做参数校验和路由
3. **安全优先**：认证（JWT Token 24h）+ 授权（AuthContext.requireAdmin）+ 数据隔离（userId 过滤）
4. **组件复用**：前端 11 个通用/布局组件（StatCard、ChartCard、AIChatBox、CourseCard、LazyImage、ParticleBackground、PomodoroTimer、QuickEntry、AppLayout、AppSidebar、AppHeader）跨页面复用
5. **渐进增强**：核心功能（登录、仪表盘、课程）优先，高级特性（AI、RAG、数据分析）逐步叠加
6. **统一规范**：ApiResponse 统一响应格式、全局异常处理、axios 统一拦截器

---

## 二、系统架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                          │
│  ┌────────────────────────────────────────────────────────┐      │
│  │          Vue 3 SPA (Vite 5.4, Element Plus 2.14)        │      │
│  │   Login │ Dashboard │ StudyPlan │ Course │ AI │ ...     │      │
│  │   12 Views + 11 Components + 8 Pinia Stores             │      │
│  └────────────────────┬───────────────────────────────────┘      │
└───────────────────────┼──────────────────────────────────────────┘
                        │ HTTP/REST (JSON)
                        │ Authorization: Bearer <jwt>
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│                  Vite Dev Server (Port 5173)                       │
│  Proxy: /api/*  →  http://127.0.0.1:8080/api/*                    │
└───────────────────────┼──────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────────────┐
│                    应用服务层 (Port 8080)                          │
│  ┌────────────────────────────────────────────────────────┐      │
│  │            Spring Boot 2.7.18 (Embedded Tomcat)          │      │
│  │                                                          │      │
│  │  ┌──────────────────────────────────────────────────┐  │      │
│  │  │        AuthInterceptor (HandlerInterceptor)       │  │      │
│  │  │   preHandle: JWT 验证 → userId + role → AuthContext │  │      │
│  │  │   afterCompletion: ThreadLocal clear()             │  │      │
│  │  └────────────────────┬─────────────────────────────┘  │      │
│  │                       ▼                                 │      │
│  │  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐  │      │
│  │  │Controller│→│   Service    │→│   Repository     │  │      │
│  │  │  (REST)  │  │  (Business)  │  │  (Spring Data)   │  │      │
│  │  │ 10 files │  │   12 files   │  │    7 files       │  │      │
│  │  └──────────┘  └──────────────┘  └──────┬───────────┘  │      │
│  │                                         │               │      │
│  │  ┌──────────────────────────────────────┼───────────┐  │      │
│  │  │  GlobalExceptionHandler             │           │  │      │
│  │  │  (@RestControllerAdvice)            │           │  │      │
│  │  └──────────────────────────────────────┼───────────┘  │      │
│  └─────────────────────────────────────────┼──────────────┘      │
└────────────────────────────────────────────┼──────────────────────┘
                        │                        │
                        ▼                        ▼
┌──────────────────────────────────┐  ┌──────────────────────────┐
│       数据持久层 (Port 3307)      │  │   外部 AI 服务             │
│  ┌────────────────────────────┐  │  │  DeepSeek API             │
│  │       MySQL 8.0            │  │  │  deepseek-v4-pro (Chat)   │
│  │  9 张表 (ddl-auto: update) │  │  │  text-embedding-v3 (RAG)  │
│  │  JPA + Hibernate 5.6       │  │  │  LangChain4j 0.36.2       │
│  │  users / course / notes /  │  │  │  WebClient (WebFlux)      │
│  │  study_plan / resources /  │  │  │  60s timeout              │
│  │  achievements / wrong_qs   │  │  └──────────────────────────┘
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

### 2.2 技术栈总览

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.34 | 核心框架（Composition API + `<script setup>`） |
| Vite | 5.4.11 | 构建工具与开发服务器（HMR 热更新） |
| Vue Router | 5.0.7 | SPA 路由管理（HTML5 History 模式） |
| Pinia | 3.0.4 | 状态管理（8 个 Store，localStorage 持久化） |
| Element Plus | 2.14.0 | UI 组件库（深色/浅色主题） |
| @element-plus/icons-vue | 2.3.2 | Element Plus 图标集 |
| ECharts | 6.1.0 | 数据可视化（按需引入 line/bar/pie/heatmap） |
| Axios | 1.16.1 | HTTP 请求封装（拦截器 + Loading 管理） |
| Sass | 1.100.0 | CSS 预处理器（SCSS 变量 + 混入） |
| lodash-es | 4.18.1 | 工具函数（debounce 防抖） |

#### 开发辅助（Vite 插件）

| 插件 | 版本 | 用途 |
|------|------|------|
| @vitejs/plugin-vue | 5.1.4 | Vue SFC 编译 |
| unplugin-auto-import | 0.18.6 | 自动导入 Vue/Router/Pinia API |
| unplugin-vue-components | 0.27.5 | Element Plus 组件按需自动导入 |
| vite-plugin-compression | 0.5.1 | Gzip + Brotli 预压缩（阈值 1KB） |

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架（spring-boot-starter-web + webflux + validation + data-jpa） |
| Spring Data JPA | (managed) | ORM 框架（Hibernate 5.6 实现） |
| MySQL Connector/J | (managed) | JDBC 驱动（mysql-connector-j） |
| JJWT | 0.12.6 | JWT Token 签发与验证（jjwt-api + jjwt-impl + jjwt-jackson） |
| LangChain4j | 0.36.2 | AI 集成框架（langchain4j + langchain4j-open-ai） |
| LangChain4j Parsers | 0.36.2 | 文档解析（PDF + Apache Tika） |
| PDFBox | 2.0.30 | PDF 文件解析 |
| Lombok | (managed) | 简化 POJO 代码（@Data, @Slf4j 等） |
| Maven | 3.9 | 项目构建与依赖管理 |
| JDK | 17 | Java 运行环境 |

### 2.3 后端分层架构

```
backend/src/main/java/com/ailearning/backend/
├── AiLearningBackendApplication.java    # Spring Boot 入口
│
├── controller/                           # 控制器层（10 个 Controller）
│   ├── UserController.java              # /api/user — 登录/注册/资料/退出 (5 endpoints)
│   ├── CourseController.java            # /api/course — 课程 CRUD ★ (5 endpoints)
│   ├── StudyPlanController.java         # /api/study-plan — 学习计划 CRUD (4 endpoints)
│   ├── NoteController.java              # /api/note — 笔记 CRUD + 分类 (6 endpoints)
│   ├── WrongQuestionController.java     # /api/wrongQuestion — 错题 CRUD + 掌握 (7 endpoints)
│   ├── ResourceController.java          # /api/resource — 资源上传/下载/CRUD (7 endpoints)
│   ├── AchievementController.java       # /api/achievement — 成就列表/统计/初始化 (4 endpoints)
│   ├── AiController.java                # /api/ai — AI 对话 + 快捷问题 (2 endpoints)
│   ├── AnalyticsController.java         # /api/analytics — 数据分析面板 (4 endpoints)
│   └── DashboardController.java         # /api/dashboard — 首页指标 (1 endpoint)
│
├── service/                              # 业务逻辑层（12 个 Service）
│   ├── UserService.java                 # 用户登录/注册/资料管理（明文密码比对）
│   ├── AuthService.java                 # JWT 生命周期：签发/解析/验证/黑名单
│   ├── CourseService.java               # 课程 CRUD（含管理员鉴权、分类/关键词过滤）
│   ├── StudyPlanService.java            # 学习计划 CRUD + 分页/筛选 + 统计
│   ├── NoteService.java                 # 笔记 CRUD + 分类筛选 + 所有权校验
│   ├── WrongQuestionService.java        # 错题 CRUD + 掌握标记 + 状态管理
│   ├── ResourceService.java             # 资源上传/下载 + 计数 + RAG 注入
│   ├── AchievementService.java          # 成就引擎：同步/解锁/初始化（8 项预设成就）
│   ├── AiService.java                   # DeepSeek LLM 调用（WebClient 异步）
│   ├── RagService.java                  # RAG 管道：解析→分块→嵌入→存储→检索→增强
│   ├── AnalyticsService.java            # 数据分析：趋势/分布/热力图（含模拟数据）
│   └── DashboardService.java            # 首页指标聚合
│
├── repository/                           # 数据访问层（7 个 Repository）
│   ├── UserRepository.java              # findByUsername(username)
│   ├── CourseRepository.java            # findAllByOrderByIdAsc()
│   ├── NoteRepository.java              # findByUserIdOrderByUpdatedAtDesc(userId)
│   ├── ResourceRepository.java          # findByUserIdOrderByCreatedAtDesc(userId)
│   ├── StudyPlanRepository.java         # findByUserIdOrderByIdDesc(userId)
│   ├── AchievementRepository.java       # findByUserId(userId)
│   └── WrongQuestionRepository.java     # findByUserIdOrderByUpdatedAtDesc(userId)
│
├── entity/                               # JPA 实体类（8 个 Entity）
│   ├── User.java                        # users — 用户账户 + 学习统计
│   ├── Course.java                      # course — 课程（含 @ElementCollection 知识点 + @OneToMany 章节）
│   ├── CourseChapter.java               # course_chapter — 课程章节（@ManyToOne → Course）
│   ├── Note.java                        # notes — 笔记
│   ├── Resource.java                    # resources — 学习资源
│   ├── StudyPlan.java                   # study_plan — 学习计划
│   ├── Achievement.java                 # achievements — 成就记录
│   └── WrongQuestion.java              # wrong_questions — 错题
│
├── dto/                                  # 数据传输对象（5 个 DTO）
│   ├── LoginRequest.java                # @NotBlank username + password
│   ├── RegisterRequest.java             # @NotBlank + @Size 校验 + confirmPassword
│   ├── ProfileUpdateRequest.java        # nickname + signature + avatar
│   ├── CourseRequest.java               # id + title + category + cover + description + lessons + teacher
│   └── StudyPlanRequest.java            # title + content + deadline + priority + status
│
├── config/                               # 配置类（4 个）
│   ├── WebConfig.java                   # CORS（allowedOriginPatterns *）+ 拦截器注册（排除 login/register）
│   ├── AuthInterceptor.java             # JWT 认证拦截器：preHandle 验证 → afterCompletion 清理
│   ├── RagConfig.java                   # LangChain4j Bean：EmbeddingStore + EmbeddingModel
│   └── DataInitializer.java             # 启动初始化：创建演示用户 + 课程 + 笔记 + 错题 + 资源 + 成就
│
├── common/                               # 公共模块（2 个）
│   ├── ApiResponse.java                 # 统一响应封装 { code, message, data } + 静态工厂方法
│   └── AuthContext.java                 # ThreadLocal 用户上下文（userId + role + requireAdmin）
│
└── exception/                            # 异常处理（2 个）
    ├── ApiException.java                # 业务异常（code + message，默认 400/401/403/404/409/500）
    └── GlobalExceptionHandler.java      # @RestControllerAdvice：处理 ApiException / @Valid 校验 / 未知异常
```

### 2.4 前端架构

#### 目录结构

```
src/
├── main.js                              # 入口：创建 App → 注册 Pinia/Router/全局指令 → 初始化主题
├── App.vue                              # 根组件：<router-view> + page-fade 过渡动画
│
├── router/
│   ├── index.js                         # 路由实例 + 全局前置守卫 (beforeEach)
│   └── routes.js                        # 11 条布局子路由 + getMenuItems() 配置
│
├── stores/
│   ├── index.js                         # 统一导出（user/studyPlan/theme/pomodoro）
│   ├── user.js                          # 认证状态（token/userInfo/isAdmin/isLoggedIn）
│   ├── theme.js                         # 主题配置（isDark/showParticles/sidebarCollapsed）
│   ├── studyPlan.js                     # 学习计划 CRUD + 筛选
│   ├── pomodoro.js                      # 番茄钟核心（计时/日志/统计计算属性）
│   ├── notes.js                         # 笔记 CRUD + 分页筛选
│   ├── resource.js                      # 资源上传/下载管理
│   ├── achievement.js                   # 成就系统（18 项前端定义 + 进度评估 + localStorage）
│   └── wrongQuestion.js                 # 错题 CRUD + 掌握标记 + 状态计算
│
├── api/
│   ├── request.js                       # Axios 实例（baseURL /api, timeout 15s, 拦截器）
│   ├── user.js                          # 认证相关 API
│   ├── dashboard.js                     # 仪表盘 API
│   ├── studyPlan.js                     # 学习计划 API
│   ├── course.js                        # 课程 API
│   ├── ai.js                            # AI 对话 API（timeout 60s）
│   ├── analytics.js                     # 数据分析 API
│   ├── note.js                          # 笔记 API
│   ├── resource.js                      # 资源 API
│   ├── achievement.js                   # 成就 API
│   └── wrongQuestion.js                 # 错题 API
│
├── views/
│   ├── login/LoginView.vue              # 登录/注册（品牌展示 + 粒子背景 + 表单切换）
│   ├── dashboard/DashboardView.vue      # 仪表盘（欢迎 + 名言 + 推荐 + 快捷入口 + 数据面板）
│   ├── study-plan/StudyPlanView.vue     # 学习计划（表格/卡片视图 + 多条件筛选）
│   ├── pomodoro/PomodoroView.vue        # 番茄专注（计时器 + 任务绑定 + 统计图表）
│   ├── course/CourseView.vue            # 课程管理（卡片网格 + 详情抽屉 + 管理员 CRUD 对话框）
│   ├── ai-assistant/AiAssistantView.vue # AI 助手（消息气泡 + 快捷问题 + 保存笔记）
│   ├── note/NoteView.vue                # 笔记管理（卡片网格 + 分类筛选 + 抽屉编辑器）
│   ├── wrongQuestion/WrongQuestionView.vue # 错题本（列表 + 彩色状态边框 + 多条件筛选）
│   ├── resource/ResourceView.vue        # 学习资源（网格 + 上传 + 下载 + 筛选）
│   ├── achievement/AchievementView.vue  # 成就系统（进度环 + 稀有度网格 + 成长建议）
│   ├── analytics/AnalyticsView.vue      # 数据分析（4 图表 + 洞察面板 + 时间范围切换）
│   └── profile/ProfileView.vue          # 个人中心（统计卡片 + 资料编辑 + 笔记列表）
│
├── components/
│   ├── common/
│   │   ├── AIChatBox.vue                # AI 聊天 UI（消息/typing/快捷问题/保存笔记）
│   │   ├── ChartCard.vue                # ECharts 包装器（异步加载/ResizeObserver/主题联动）
│   │   ├── CourseCard.vue               # 课程卡片（封面懒加载/进度条/悬停编辑按钮）
│   │   ├── LazyImage.vue                # 图片懒加载（IntersectionObserver + shimmer 占位）
│   │   ├── ParticleBackground.vue       # Canvas 粒子动画（60 粒子 + 连线 + 响应式）
│   │   ├── PomodoroTimer.vue            # 迷你番茄钟（自管理计时器/通知/环形进度）
│   │   ├── QuickEntry.vue               # 快捷入口网格（配置驱动 + 图标映射）
│   │   └── StatCard.vue                 # 统计卡片（渐变数值 + 图标 + 插槽扩展）
│   └── layout/
│       ├── AppLayout.vue                # 主布局（粒子 + 侧栏 + 顶栏 + keep-alive + provide themeConfig）
│       ├── AppSidebar.vue               # 侧栏导航（路由驱动菜单 + component :is 动态图标）
│       └── AppHeader.vue                # 顶栏（折叠/面包屑/主题/粒子/头像）
│
├── utils/
│   ├── storage.js                       # localStorage 封装（带 try-catch + JSON 序列化）
│   ├── debounce.js                      # lodash-es debounce 再导出
│   ├── persist.js                       # Pinia localStorage 持久化助手（usePersist）
│   ├── constants.js                     # 业务常量（优先级/状态/快捷入口）
│   ├── echarts.js                       # ECharts 图表构建函数（4 种类型 + 双主题）
│   ├── echarts-init.js                  # ECharts 按需引入入口
│   └── index.js                         # 统一导出
│
├── directives/
│   └── lazy.js                          # v-lazy 全局指令（IntersectionObserver 单例）
│
└── styles/
    ├── variables.scss                   # SCSS 变量（颜色/圆角/阴影/布局尺寸/浅色模式）
    ├── mixins.scss                      # SCSS 混入（glass-card/gradient-border/text-gradient）
    ├── global.scss                      # 全局样式（CSS 自定义属性/重置/工具类/过渡动画/响应式/浅色覆盖）
    └── element-override.scss            # Element Plus 样式覆盖（深色/浅色主题 + 自定义组件样式）
```

#### 组件树（完整）

```
App.vue ─ <router-view> (page-fade transition)
│
├── LoginView.vue (/login)
│   ├── ParticleBackground.vue          ★ props: count/color/linkLines/opacity
│   ├── 品牌展示区（3 个亮点卡片）
│   └── el-form（登录/注册双标签切换）
│
└── AppLayout.vue (/)                    ★ provide('themeConfig')
    ├── ParticleBackground.vue
    ├── AppSidebar.vue                   ★ component :is 动态图标
    │   └── router-link × 11（getMenuItems() 配置驱动）
    ├── AppHeader.vue
    │   ├── el-switch（粒子开关）
    │   ├── 主题切换按钮
    │   └── 用户头像 + 昵称
    │
    └── <router-view> (fade-slide transition)
        └── <keep-alive>
            ├── DashboardView.vue (/dashboard)
            │   ├── StatCard × 3         ★ props: label/value/unit/icon/color | slot: default/footer
            │   ├── ChartCard × 2        ★ props: title/height/option/loading | slot: extra/default
            │   ├── QuickEntry.vue       ★ component :is 动态图标
            │   └── 最近笔记卡片
            │
            ├── CourseView.vue (/course)
            │   ├── CourseCard × N       ★ props: course | emit: click
            │   │   └── LazyImage.vue    ★ props: src/alt/fetchpriority
            │   ├── el-drawer（课程详情 + el-timeline）
            │   └── el-dialog（管理员添加/编辑表单）
            │
            ├── AiAssistantView.vue (/ai-assistant)
            │   └── AIChatBox.vue        ★ props: messages/loading | emit: send/quick/saveAsNote
            │
            ├── StudyPlanView.vue (/study-plan)
            ├── PomodoroView.vue (/pomodoro)
            │   ├── PomodoroTimer.vue
            │   └── ChartCard × 3
            │
            ├── NoteView.vue (/note)
            ├── WrongQuestionView.vue (/wrong-question)
            ├── ResourceView.vue (/resource)
            ├── AchievementView.vue (/achievement)
            ├── AnalyticsView.vue (/analytics)
            │   ├── StatCard × 4
            │   └── ChartCard × 4
            │
            └── ProfileView.vue (/profile)
                ├── StatCard × 3
                └── el-form（资料编辑）
```

---

## 三、前端详细设计

### 3.1 路由设计

系统共 **13 条路由**（1 条公开 + 11 条受保护 + 1 条 404 通配）：

#### 路由表

| 路径 | 名称 | 标题 | 懒加载 Chunk | 需登录 | 图标 |
|------|------|------|-------------|--------|------|
| `/login` | Login | 登录 | login | 否 | - |
| `/dashboard` | Dashboard | 首页 | dashboard | 是 | Odometer |
| `/study-plan` | StudyPlan | 学习计划 | study-plan | 是 | Calendar |
| `/pomodoro` | Pomodoro | 番茄专注 | pomodoro | 是 | AlarmClock |
| `/course` | Course | 课程管理 | course | 是 | Reading |
| `/note` | Note | 笔记管理 | note | 是 | Document |
| `/wrong-question` | WrongQuestion | 错题本 | wrong-question | 是 | Warning |
| `/resource` | Resource | 学习资源 | resource | 是 | FolderOpened |
| `/achievement` | Achievement | 成就系统 | achievement | 是 | Trophy |
| `/ai-assistant` | AiAssistant | AI 助手 | ai-assistant | 是 | ChatDotRound |
| `/analytics` | Analytics | 数据分析 | analytics | 是 | DataAnalysis |
| `/profile` | Profile | 个人中心 | profile | 是 | User |
| `/:pathMatch(.*)*` | - | 404 | - | - | - |

#### 导航守卫（`router/index.js` beforeEach）

```
┌─ 是否匹配到路由？
│  └─ 否 → ElMessage 警告 + 重定向 /dashboard
│
├─ requiresAuth === true 且未登录？
│  └─ 是 → 重定向 /login?redirect=<原始路径>
│
├─ 已登录 + 访问 /login？
│  └─ 是 → 重定向 /dashboard
│
└─ 设置 document.title
```

#### 路由懒加载

所有视图组件均使用动态 `import()` 语法，配合 webpackChunkName 注释实现按 chunk 分组：

```javascript
component: () => import(/* webpackChunkName: "dashboard" */ '@/views/dashboard/DashboardView.vue')
```

### 3.2 状态管理设计（Pinia）

使用 Pinia 管理 **8 个 Store**，均通过 `usePersist` 或手动 watch 实现 localStorage 持久化：

#### useUserStore（`stores/user.js`）

| State | 类型 | 说明 |
|-------|------|------|
| token | string | JWT Token |
| userInfo | object | { id, username, nickname, avatar, signature, studyDays, totalHours, role } |

| Getter | 说明 |
|--------|------|
| isLoggedIn | !!token |
| isAdmin | userInfo.role === 'admin' |

| Action | 说明 |
|--------|------|
| setLogin(token, userInfo) | 保存登录信息到 state + localStorage |
| logout() | 清除 state + localStorage |
| fetchProfile() | GET /api/user/profile |
| updateProfile(data) | PUT /api/user/profile |

**持久化**：token 和 userInfo 均写入 localStorage，支持页面刷新恢复。

#### useThemeStore（`stores/theme.js`）

| State | 类型 | 默认值 | 说明 |
|-------|------|--------|------|
| isDark | boolean | true | 深色/浅色主题 |
| showParticles | boolean | true | 粒子动画开关 |
| sidebarCollapsed | boolean | false | 侧栏折叠状态 |

| Action | 说明 |
|--------|------|
| toggleDark() | 切换 isDark → 调用 applyTheme() |
| applyTheme() | 设置 `document.documentElement.classList` + `data-theme` 属性 |

**持久化**：watch 监听所有 state 变化 → 写入 localStorage。初始化时从 localStorage 恢复。

#### usePomodoroStore（`stores/pomodoro.js`）

番茄钟核心状态机，管理计时器生命周期：

| 核心 State | 说明 |
|-----------|------|
| phase | 当前阶段：focus / shortBreak / longBreak |
| timeLeft | 剩余秒数（基于 Date.now() 精确计算） |
| isRunning | 是否正在计时 |
| logs[] | 番茄钟完成记录数组 |

| 计算属性 | 说明 |
|---------|------|
| todayCount | 今日完成番茄数 |
| todayMinutes | 今日学习分钟 |
| streakDays | 连续学习天数 |
| weeklyTrend | 近 7 天数据（供图表） |
| courseDistribution | 课程时间分布（供图表） |
| focusPeak | 最佳专注时段（供图表） |

**持久化**：设置项和日志数组写入 localStorage。无历史数据时自动生成演示数据。

#### 其他 Store

| Store | 文件 | 管理内容 |
|-------|------|---------|
| useStudyPlanStore | `stores/studyPlan.js` | 学习计划 CRUD、pendingCount/doneCount 计算属性、toggleStatus 状态循环、分页筛选查询 |
| useNoteStore | `stores/notes.js` | 笔记 CRUD、categories 预设分类、分页查询（category/keyword 参数） |
| useResourceStore | `stores/resource.js` | 资源 CRUD、文件上传/下载、分类管理 |
| useAchievementStore | `stores/achievement.js` | 18 项前端成就定义、5 类别（成长/学习/任务/创作/习惯）、基于指标实时评估进度、稀有度系统、localStorage 永久解锁记录 |
| useWrongQuestionStore | `stores/wrongQuestion.js` | 错题 CRUD、预设分类（数学/英语/计算机）、markAsMastered、状态计算（newCount/reviewingCount/masteredCount） |

### 3.3 API 层设计

#### Axios 实例（`src/api/request.js`）

```javascript
const service = axios.create({
  baseURL: '/api',          // 通过 Vite proxy 转发至 http://127.0.0.1:8080
  timeout: 15000,           // 默认 15 秒
})
```

**请求拦截器**：
- 自动注入 `Authorization: Bearer <token>`（从 useUserStore 读取）
- 全局 Loading 引用计数管理（`showLoading: true` 时计数 +1）
- 支持 `config.loadingText` 自定义 Loading 文案

**响应拦截器**：
- HTTP 401 → 自动登出 + 重定向登录页（错误计数防抖，避免并发请求重复弹窗）
- 业务错误（code !== 200）→ `ElMessage.error(message)`
- 支持 `config.silent: true` 静默模式（不弹错误提示，用于后台请求）
- 处理完成后 Loading 计数 -1

#### API 模块清单（10 个文件，45 个端点）

| 模块文件 | 导出函数数 | 说明 |
|---------|-----------|------|
| `api/user.js` | 5 | loginApi, registerApi, logoutApi, getUserProfileApi, updateUserProfileApi |
| `api/dashboard.js` | 1 | getDashboardStatsApi（showLoading: false） |
| `api/studyPlan.js` | 4 | getStudyPlanListApi, createStudyPlanApi, updateStudyPlanApi, deleteStudyPlanApi |
| `api/course.js` | 5 | getCourseListApi, getCourseDetailApi, createCourseApi, updateCourseApi, deleteCourseApi |
| `api/ai.js` | 2 | sendAiChatApi（timeout: 60000, showLoading: false）, getQuickQuestionsApi |
| `api/analytics.js` | 4 | getAnalyticsOverviewApi, getDashboardStatsApi, getTaskStatsApi, getResourceStatsApi |
| `api/note.js` | 6 | getNoteListApi, getNoteDetailApi, createNoteApi, updateNoteApi, deleteNoteApi, getNoteCategoriesApi |
| `api/resource.js` | 7 | getResourceListApi, getResourceDetailApi, createResourceApi, uploadResourceApi（multipart）, deleteResourceApi, downloadResourceApi（blob）, getResourceCategoriesApi |
| `api/achievement.js` | 4 | getAchievementListApi, getAchievementStatsApi, unlockAchievementApi, initAchievementsApi |
| `api/wrongQuestion.js` | 7 | getWrongQuestionListApi, getWrongQuestionDetailApi, createWrongQuestionApi, updateWrongQuestionApi, deleteWrongQuestionApi, markAsMasteredApi, getWrongQuestionCategoriesApi |

### 3.4 主题系统设计

采用 **三层主题架构**：SCSS 变量 + CSS 自定义属性 + Element Plus CSS 变量。

#### 第一层：SCSS 变量（`variables.scss`）

```scss
// 深色背景层级
$color-bg-primary: #0b1020;      // 最深底
$color-bg-secondary: #111827;    // 卡片背景
$color-bg-tertiary: #1a2236;     // 悬浮态

// 品牌色（靛蓝 → 紫 → 青渐变体系）
$color-accent: #6366f1;          // 靛蓝主色
$color-accent-secondary: #8b5cf6; // 紫色辅色
$color-accent-tertiary: #06b6d4;  // 青色点缀

// 文字色（WCAG AA 对比度）
$color-text-primary: #f1f5f9;
$color-text-secondary: #cbd5e1;
$color-text-muted: #94a3b8;

// 功能色
$color-success: #22c55e;
$color-warning: #f59e0b;
$color-danger: #ef4444;
```

#### 第二层：CSS 自定义属性（`:root` / `:root[data-theme='light']`）

运行时通过 `data-theme` 属性切换 CSS 变量值，影响全局样式（背景、文字、边框、阴影）。

#### 第三层：Element Plus CSS 变量覆盖（`element-override.scss`）

覆盖 Element Plus 组件的 CSS 变量（--el-bg-color、--el-text-color 等），配合 `html.dark` class 实现组件级深色适配。

#### 主题切换机制

```
用户点击主题切换按钮
  → useThemeStore.toggleDark()
    → isDark = !isDark
      → applyTheme():
        1. document.documentElement.classList.toggle('dark')
        2. document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
        3. ECharts 图表通过 provide/inject 的 themeConfig 响应 → 销毁并重建实例
```

#### 多媒介适配

- **ECharts 图表**：`ChartCard.vue` 通过 `inject('themeConfig')` 监听主题变化，调用 `chart.dispose()` + 重新 `init()` + 应用 `getChartTheme(isDark)`
- **粒子背景**：`AppLayout.vue` 通过 `provide('themeConfig')` 向子树注入主题状态
- **SCSS 混入**：`glass-card`、`gradient-border`、`text-gradient` 等混入适配深色/浅色双模式

### 3.5 性能优化设计

| 优化策略 | 实现方式 | 效果 |
|---------|---------|------|
| 路由懒加载 | 动态 import() + webpackChunkName | 首屏仅加载当前路由 chunk |
| ECharts 按需引入 | `echarts-init.js` 仅注册 line/bar/pie/heatmap + Grid/Tooltip/Legend/Title/Calendar | 约 350KB vs 全量 1MB |
| 代码分割 | Vite build.rollupOptions 手动分割：echarts / element-plus / vue-vendor | 并行加载，利用浏览器缓存 |
| Gzip/Brotli 预压缩 | `vite-plugin-compression`（阈值 1KB，deleteOriginalAssets: false） | 传输体积减少 60-70% |
| 图片懒加载 | `v-lazy` 指令 + IntersectionObserver（rootMargin: 100px, threshold: 0.01） | 视口外图片延迟加载 |
| KeepAlive 缓存 | `<keep-alive>` 包裹 `<component :is>` | 页面切换保留状态，避免重复渲染 |
| 搜索防抖 | `lodash-es/debounce(fn, 400)` | 减少筛选请求频率 |
| 全局 Loading 管理 | Axios 拦截器引用计数 | 防止多个请求重复显示/隐藏 Loading |
| 骨架屏 | ChartCard loading prop → el-skeleton | 图表加载前显示占位骨架 |
| Vite 自动导入 | unplugin-auto-import + unplugin-vue-components | Tree-shaking，仅打包使用到的 API 和组件 |

---

## 四、后端详细设计

### 4.1 角色权限模型

采用简化的双角色模型，角色存储在 `users.role` 字段（VARCHAR(20)），并在 JWT Token Claims 中携带：

| 角色 | role 值 | 权限范围 |
|------|--------|---------|
| 管理员 | `admin` | 全部功能，含课程 CRUD（POST/PUT/DELETE `/api/course`） |
| 普通用户 | `user` | 个人学习管理、查看课程、AI 对话、数据分析 |

#### 权限实现全链路

```
1. JWT 签发（AuthService.issueToken）
   └─ Claims: { userId, username, nickname, role }

2. 请求拦截（AuthInterceptor.preHandle）
   └─ 从 Authorization header 提取 Token
   └─ authService.requireUserId(token) → userId
   └─ authService.extractRole(token) → role
   └─ AuthContext.setCurrentUserId(userId)
   └─ AuthContext.setCurrentUserRole(role)

3. 接口鉴权（Controller 方法内）
   └─ AuthContext.requireAdmin() → 非 admin 抛出 ApiException(403)

4. 数据隔离（Service 层）
   └─ Long userId = AuthContext.getCurrentUserId()
   └─ repository.findByUserId(userId) → 仅返回当前用户数据

5. 请求结束清理（AuthInterceptor.afterCompletion）
   └─ AuthContext.clear() → ThreadLocal.remove()
```

### 4.2 JWT 认证流程

#### 登录流程

```
客户端 POST /api/user/login { username, password }
  → UserController.login()
    → UserService.login()
      1. userRepository.findByUsername(username)
      2. 明文密码比对（演示项目）
      3. authService.issueToken(user)
         └─ Jwts.builder()
              .subject(user.getId())
              .claims({ userId, username, nickname, role })
              .issuedAt(now)
              .expiration(now + 1440min)
              .signWith(HMAC-SHA256, secretKey)
              .compact()
      4. 返回 { token, userInfo }
```

#### 后续请求认证

```
客户端请求 → 携带 Authorization: Bearer <token>
  → AuthInterceptor.preHandle()
    1. 提取 Token
    2. authService.requireUserId(token)
       └─ Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token)
       └─ 验证签名 + 过期时间
       └─ 检查黑名单（ConcurrentHashMap）
    3. authService.extractRole(token) → role
    4. AuthContext 存入 ThreadLocal
  → Controller / Service 处理
  → AuthInterceptor.afterCompletion()
    └─ AuthContext.clear()
```

#### Token 黑名单

```java
// AuthService
private final ConcurrentHashMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

public void invalidateToken(String token) {
    tokenBlacklist.put(token, System.currentTimeMillis());
    // 过期 Token 自动清理（惰性删除 + 定时清理）
}
```

### 4.3 核心 API 详细设计

#### 课程模块（管理员 CRUD）

| 方法 | 路径 | 权限 | 请求参数 | 响应 |
|------|------|------|---------|------|
| GET | `/api/course/list` | 所有用户 | `?category=&keyword=` | `ApiResponse<List<Course>>` |
| GET | `/api/course/{id}` | 所有用户 | 路径参数 id (String) | `ApiResponse<Course>`（含 chapters 和 knowledgePoints） |
| POST | `/api/course` | **仅 admin** | JSON Body（id/title/category/cover/description/lessons/teacher） | `ApiResponse<Course>` |
| PUT | `/api/course/{id}` | **仅 admin** | JSON Body（部分字段更新） | `ApiResponse<Course>` |
| DELETE | `/api/course/{id}` | **仅 admin** | 路径参数 id | `ApiResponse<null>` |

**Controller 鉴权示例**：

```java
@PostMapping
public ApiResponse<Course> create(@RequestBody Map<String, Object> body) {
    AuthContext.requireAdmin();  // 非管理员抛出 403
    return ApiResponse.success(courseService.create(body));
}
```

#### AI 对话模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | DeepSeek 对话（WebClient 异步调用，60s 超时） |
| GET | `/api/ai/quick-questions` | 预设快捷问题列表（考研/英语/Vue/时间管理/算法） |

**AI 对话流程**：

```
客户端 POST /api/ai/chat { message: "用户问题" }
  → AiController.chat()
    → AiService.chat(message, userId)
      1. RagService.searchRelevant(userId, message)
         └─ 向量相似度检索（阈值 0.7）
         └─ 构建 RAG 上下文文本
      2. 构建 System Prompt
         └─ "You are a professional AI learning assistant..."
      3. 构建请求体
         { model: "deepseek-v4-pro",
           messages: [
             { role: "system", content: systemPrompt },
             { role: "user", content: message + ragContext }
           ],
           thinking: true,
           reasoning_effort: "medium" }
      4. WebClient.post() → https://api.deepseek.com/chat/completions
      5. 解析响应 choices[0].message.content
      6. 返回 AI 回复文本
    → 异常处理：try-catch → 返回友好错误提示
```

### 4.4 全局异常处理

`@RestControllerAdvice` 统一拦截所有异常，返回标准 `ApiResponse` 格式：

| 异常类型 | HTTP 状态码 | code | 处理逻辑 |
|---------|------------|------|---------|
| `ApiException` | 200 | 异常自带 code | 直接返回 code + message |
| `MethodArgumentNotValidException` | 200 | 400 | 收集所有字段校验错误 → 逗号拼接 message |
| `ConstraintViolationException` | 200 | 400 | 返回校验失败提示 |
| `Exception`（兜底） | 200 | 500 | 返回 "服务器内部错误"，控制台打印堆栈 |

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ApiResponse<?> handleApiException(ApiException e) {
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ApiResponse.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleUnknown(Exception e) {
        log.error("Unknown error", e);
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
```

### 4.5 数据初始化设计（DataInitializer）

`@Bean CommandLineRunner` 在应用启动时执行，采用"先清空再重建"策略确保幂等性：

| 数据类型 | 数量 | 详情 |
|---------|------|------|
| 用户 | 2 | admin (role=admin, pwd=admin123)、zhangsan (role=user, pwd=123456) |
| 课程 | 5 | Vue3 Advanced (12 章)、Python ML (10 章)、Spring Boot Microservices (10 章)、Data Structures & Algorithms (12 章)、Technical English (8 章)，共 52 章节 |
| 笔记 | 13 | admin 5 篇、zhangsan 8 篇（含学习笔记/读书笔记/项目笔记等） |
| 学习计划 | 10 | admin 4 项、zhangsan 6 项 |
| 错题 | 9 | admin 3 题、zhangsan 6 题 |
| 资源 | 10 | admin 4 个、zhangsan 6 个 |
| 成就 | 13 | admin 5 项已解锁、zhangsan 8 项已解锁 |

---

## 五、数据库设计

### 5.1 表结构总览

系统共 **9 张数据库表**（8 个实体表 + 1 个 ElementCollection 映射表）：

| 表名 | 对应实体 | 主键类型 | 外键 | 索引 |
|------|---------|---------|------|------|
| `users` | User | BIGINT AUTO_INCREMENT | - | UNIQUE(username) |
| `course` | Course | VARCHAR(40) | - | - |
| `course_chapter` | CourseChapter | VARCHAR(50) | course_id → course.id | course_id |
| `course_knowledge_point` | (ElementCollection) | BIGINT AUTO_INCREMENT | course_id → course.id | course_id |
| `study_plan` | StudyPlan | BIGINT AUTO_INCREMENT | user_id → users.id | user_id |
| `notes` | Note | BIGINT AUTO_INCREMENT | user_id → users.id | user_id |
| `resources` | Resource | BIGINT AUTO_INCREMENT | user_id → users.id | user_id |
| `achievements` | Achievement | BIGINT AUTO_INCREMENT | user_id → users.id | user_id |
| `wrong_questions` | WrongQuestion | BIGINT AUTO_INCREMENT | user_id → users.id | user_id |

### 5.2 核心表 DDL

#### users

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    signature VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    study_days INT NOT NULL DEFAULT 0,
    total_hours INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### course

```sql
CREATE TABLE course (
    id VARCHAR(40) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    cover VARCHAR(255),
    description VARCHAR(500) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    teacher VARCHAR(50) NOT NULL,
    lessons INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### course_chapter

```sql
CREATE TABLE course_chapter (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(40) NOT NULL,
    title VARCHAR(150) NOT NULL,
    duration VARCHAR(30) NOT NULL,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    INDEX idx_chapter_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### course_knowledge_point

```sql
CREATE TABLE course_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id VARCHAR(40) NOT NULL,
    knowledge_point VARCHAR(255) NOT NULL,
    sort_order INT,
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    INDEX idx_kp_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### study_plan

```sql
CREATE TABLE study_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500),
    deadline DATE,
    priority VARCHAR(20) NOT NULL DEFAULT '中',
    status VARCHAR(20) NOT NULL DEFAULT '待开始',
    created_at DATE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_plan_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notes

```sql
CREATE TABLE notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_note_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### resources

```sql
CREATE TABLE resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    type VARCHAR(50),
    category VARCHAR(50),
    size VARCHAR(50) NOT NULL,
    url VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_resource_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### achievements

```sql
CREATE TABLE achievements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(100),
    unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP,
    progress INT NOT NULL DEFAULT 0,
    target INT NOT NULL DEFAULT 1,
    category VARCHAR(50),
    rarity VARCHAR(50),
    points INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_achieve_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### wrong_questions

```sql
CREATE TABLE wrong_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    answer TEXT,
    analysis TEXT,
    category VARCHAR(50),
    difficulty VARCHAR(20),
    mastered BOOLEAN NOT NULL DEFAULT FALSE,
    wrong_count INT NOT NULL DEFAULT 0,
    tags VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_wq_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.3 JPA 映射要点

| 映射特性 | 实现 | 涉及实体 |
|---------|------|---------|
| 主键策略 | `@GeneratedValue(strategy = GenerationType.IDENTITY)` | User, StudyPlan, Note, Resource, Achievement, WrongQuestion |
| 手动主键 | `@Id` 无 GeneratedValue（业务主键） | Course(String), CourseChapter(String) |
| 一对多 | `@OneToMany(mappedBy="course", cascade=ALL, orphanRemoval=true)` | Course → CourseChapter |
| 多对一 | `@ManyToOne(fetch=LAZY)` + `@JoinColumn` + `@JsonIgnore` | CourseChapter → Course |
| 元素集合 | `@ElementCollection(fetch=EAGER)` + `@CollectionTable` + `@OrderColumn` | Course → knowledgePoints (List\<String\>) |
| 唯一约束 | `@Column(unique = true)` | User.username |
| 枚举字段 | `@Column` + 代码中字符串常量（非 Java enum） | StudyPlan.priority/status, Achievement.rarity, WrongQuestion.difficulty |
| 时间戳 | `@Column` + Java LocalDateTime/LocalDate | Note.createdAt/updatedAt, StudyPlan.deadline/createdAt |
| 表管理 | `spring.jpa.hibernate.ddl-auto: update` | Hibernate 自动建表/更新字段 |
| SQL 初始化 | `spring.sql.init.mode: never` | schema.sql 和 data.sql 存在但不自动执行 |

---

## 六、安全设计

### 6.1 认证安全

| 措施 | 实现细节 |
|------|---------|
| Token 生成 | JWT（JJWT 0.12.6），HMAC-SHA256 签名，密钥 `app.auth.jwt-secret` |
| Token 有效期 | 1440 分钟（24 小时），`app.auth.jwt-expire-minutes` |
| Token 载体 | Claims: userId, username, nickname, role |
| Token 验证 | `AuthInterceptor.preHandle()` 拦截 `/api/**`（排除 login/register） |
| Token 传输 | HTTP Header `Authorization: Bearer <token>` |
| Token 吊销 | `ConcurrentHashMap<String, Long>` 内存黑名单，退出登录时加入 |
| 密码存储 | 明文（演示项目，生产应使用 BCryptPasswordEncoder） |

### 6.2 授权安全

| 措施 | 实现细节 |
|------|---------|
| 角色模型 | `users.role` 字段（admin / user），JWT role Claims 携带 |
| 接口鉴权 | Controller 方法内调用 `AuthContext.requireAdmin()`，非 admin 抛 ApiException(403) |
| 数据隔离 | Service 层通过 `AuthContext.getCurrentUserId()` 过滤查询 |
| 所有权校验 | Note/StudyPlan/WrongQuestion/Resource 的 update/delete 操作前验证 `entity.userId == currentUserId` |
| ThreadLocal 清理 | `afterCompletion()` 中 `AuthContext.clear()`，防止内存泄漏和请求间数据串扰 |

### 6.3 防护措施

| 威胁类型 | 防护方式 | 实现位置 |
|---------|---------|---------|
| XSS | Vue 模板默认转义输出 | 前端框架层 |
| SQL 注入 | Spring Data JPA 参数化查询（`?` 占位符） | Repository 层 |
| CORS | `WebConfig.addCorsMappings()` 配置 `allowedOriginPatterns("*")` | 后端配置层 |
| CSRF | SPA 无 Cookie 会话，JWT 通过 Header 传递，天然免疫 | 架构层 |
| 暴力破解 | 无（演示项目未实现登录限流） | - |
| 路径遍历 | 文件上传使用 UUID 重命名，不保留原始文件名 | ResourceService |

---

## 七、部署设计

### 7.1 开发环境拓扑

```
┌─────────────────────────────────────────────────────────┐
│                    开发环境 (localhost)                     │
│                                                           │
│  ┌──────────────────┐     ┌──────────────────┐           │
│  │  Vite Dev Server  │     │   Spring Boot    │           │
│  │  Port: 5173       │────▶│   Port: 8080     │           │
│  │  HMR 热更新        │proxy│   Tomcat 内嵌     │           │
│  │  /api/* → 8080    │     │   DevTools 热部署  │           │
│  └──────────────────┘     └────────┬─────────┘           │
│                                     │                      │
│                                     ▼                      │
│                          ┌──────────────────┐           │
│                          │    MySQL 8.0     │           │
│                          │    Port: 3307    │           │
│                          │  ai_learning DB  │           │
│                          └──────────────────┘           │
│                                                           │
│                          ┌──────────────────┐           │
│                          │  DeepSeek API    │           │
│                          │  互联网外部服务     │           │
│                          └──────────────────┘           │
└─────────────────────────────────────────────────────────┘
```

### 7.2 环境配置

| 组件 | 端口 | 配置项 |
|------|------|--------|
| Vite Dev Server | 5173 | `vite.config.js` → `server.port` |
| Spring Boot | 8080 | `application.yml` → `server.port` |
| MySQL | 3307 | `application.yml` → `spring.datasource.url` |
| DeepSeek API | HTTPS/443 | `application.yml` → `app.deepseek.base-url` |

### 7.3 构建与启动命令

```bash
# ===== 前端 =====
cd frontend/
npm install                        # 安装依赖
npm run dev                        # 开发模式（HMR, Port 5173）
npm run build                      # 生产构建（dist/）

# ===== 后端 =====
cd backend/
mvn spring-boot:run                # 开发模式（Port 8080, DevTools）
mvn package -DskipTests            # 打包 JAR
java -jar target/backend-1.0.0.jar # 生产运行

# ===== 数据库 =====
# MySQL 8.0 需预先启动，端口 3307
# 数据库 ai_learning 由 JDBC URL createDatabaseIfNotExist=true 自动创建
# 表结构由 JPA ddl-auto: update 自动管理
# 初始数据由 DataInitializer CommandLineRunner 自动写入
```

### 7.4 构建产物

| 产物 | 路径 | 说明 |
|------|------|------|
| 前端构建 | `frontend/dist/` | 静态文件（HTML/CSS/JS），Gzip+Brotli 预压缩 |
| 后端 JAR | `backend/target/backend-1.0.0.jar` | Fat JAR（含所有依赖，内嵌 Tomcat） |
| 上传文件 | `./uploads/` | 用户上传的学习资源文件（UUID 重命名） |
| 访问日志 | `backend/logs/access_log.*.log` | Tomcat Access Log（common 格式） |

---

## 八、AI 集成设计

### 8.1 DeepSeek API 集成

#### 配置（`application.yml`）

```yaml
app:
  deepseek:
    api-key: sk-xxx                    # DeepSeek API Key
    base-url: https://api.deepseek.com # API 基础 URL
    model: deepseek-v4-pro             # 对话模型
    system-prompt: >-                  # 系统提示词
      You are a professional AI learning assistant.
      You help students with their studies, answer questions,
      provide learning suggestions, and assist with homework.
      Be concise, accurate, and encouraging.
```

#### 技术实现（AiService）

```java
@Service
public class AiService {

    private final WebClient webClient;  // WebFlux 异步 HTTP 客户端

    public String chat(String userMessage, Long userId) {
        // 1. RAG 知识增强
        String ragContext = ragService.searchRelevant(userId, userMessage);

        // 2. 构建增强 Prompt
        String enhancedMessage = ragContext.isEmpty()
            ? userMessage
            : "参考以下知识：\n" + ragContext + "\n\n问题：" + userMessage;

        // 3. 构建请求体
        Map<String, Object> requestBody = Map.of(
            "model", deepseekModel,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", enhancedMessage)
            ),
            "thinking", true,
            "reasoning_effort", "medium"
        );

        // 4. 发送请求
        String response = webClient.post()
            .uri(deepseekBaseUrl + "/chat/completions")
            .header("Authorization", "Bearer " + deepseekApiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(60))
            .block();

        // 5. 解析响应
        return extractContent(response); // choices[0].message.content
    }
}
```

### 8.2 RAG 检索增强生成

#### 架构

```
┌─────────────────────────────────────────────────────────┐
│                    RAG Pipeline                          │
│                                                           │
│  ┌──────────┐   ┌──────────┐   ┌──────────────────┐    │
│  │ 文档摄入  │──▶│ 文档分块  │──▶│ 向量嵌入          │    │
│  │ (Ingest) │   │ (Split)  │   │ (Embed)           │    │
│  │ PDF/Text │   │ 500字符   │   │ DeepSeek          │    │
│  │ Tika解析  │   │ 100重叠   │   │ text-embedding-v3 │    │
│  └──────────┘   └──────────┘   └────────┬─────────┘    │
│                                         │                │
│                                         ▼                │
│                              ┌──────────────────┐       │
│                              │ 向量存储          │       │
│                              │ InMemory         │       │
│                              │ EmbeddingStore   │       │
│                              └────────┬─────────┘       │
│                                       │                  │
│  ┌──────────┐   ┌──────────┐         │                  │
│  │ Prompt   │◀──│ 语义检索  │◀────────┘                  │
│  │ 增强     │   │ (Search)  │                            │
│  │ 上下文注入│   │ 相似度≥0.7│                            │
│  └──────────┘   └──────────┘                            │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

#### 技术实现（RagService + RagConfig）

**RagConfig** 配置两个核心 Bean：

```java
@Configuration
public class RagConfig {

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();  // 内存存储
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .baseUrl("https://api.deepseek.com/v1")
            .apiKey(deepseekApiKey)
            .modelName("text-embedding-v3")
            .build();
    }
}
```

**RagService** 实现 RAG 管道：

| 方法 | 功能 | 实现 |
|------|------|------|
| `ingestDocument(File, Long userId)` | 文档摄入 | 解析（PDF/Text/Tika）→ 500 字符分块（100 字符重叠）→ 嵌入向量 → 存入 InMemoryEmbeddingStore（带 userId 元数据） |
| `searchRelevant(Long userId, String query)` | 语义检索 | 查询文本嵌入 → 向量相似度检索（阈值 0.7）→ 过滤 userId 匹配结果 → 拼接上下文文本 |
| `buildRagPrompt(String query, String context)` | Prompt 增强 | 将检索到的上下文注入用户问题 |

**触发时机**：用户通过 `POST /api/resource/upload` 上传文件时，`ResourceService` 自动调用 `ragService.ingestDocument()` 将文件内容注入 RAG 知识库。

**注意**：使用 `InMemoryEmbeddingStore`，服务重启后知识库数据丢失，生产环境应替换为持久化向量数据库（如 Chroma、Pinecone、Milvus）。

### 8.3 快捷问题

预设 5 类快捷问题，由 `GET /api/ai/quick-questions` 返回：

| 类别 | 示例问题 |
|------|---------|
| 考研 | "如何高效备考研究生入学考试？" |
| 英语 | "提高英语听力最有效的方法是什么？" |
| Vue | "Vue 3 Composition API 和 Options API 的区别？" |
| 时间管理 | "番茄工作法如何帮助提高学习效率？" |
| 算法 | "动态规划和贪心算法的区别？" |

---

## 九、统一响应规范

### ApiResponse 设计

```java
public class ApiResponse<T> {
    private int code;       // 状态码
    private String message; // 提示信息
    private T data;         // 响应数据

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "ok", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

### 错误码规范

| code | HTTP 语义 | 触发场景 |
|------|----------|---------|
| 200 | OK | 请求成功 |
| 400 | Bad Request | 参数校验失败（@Valid）、用户名重复 |
| 401 | Unauthorized | 未登录、Token 无效/过期/已吊销 |
| 403 | Forbidden | 非管理员访问管理接口 |
| 404 | Not Found | 资源不存在（课程/笔记/计划等） |
| 409 | Conflict | 数据冲突 |
| 500 | Internal Server Error | 未知服务器异常 |

---

> **文档编制说明**：本设计说明书基于实际项目代码编写（前端 `src/` 12 视图 + 11 组件 + 8 Store + 10 API 模块，后端 10 Controller + 12 Service + 7 Repository + 8 Entity + 5 DTO + 4 Config），所有架构图、API 清单、数据库 DDL 均与代码实现一致。完整数据库脚本见 `backend/src/main/resources/schema.sql`，组件接口说明见 `docs/COMPONENTS.md`，前端构建配置见 `vite.config.js`。
