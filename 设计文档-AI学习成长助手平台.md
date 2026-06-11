# AI 学习成长助手平台 —— 系统设计说明书

> **项目名称**：AI 学习成长助手平台（AI Learning Growth Studio）
> **所属课程**：信息系统综合实训
> **技术路线**：Spring Boot 2.7 + Vue 3 + Spring Data JPA + MySQL 8.0 + Element Plus + DeepSeek AI
> **文档版本**：v2.0
> **编写日期**：2026-06-12

---

## 一、引言

### 1.1 设计目标

本文档对 AI 学习成长助手平台进行系统设计，涵盖系统架构、前后端模块设计、数据库设计、安全设计及部署方案。

1. **完整性**：覆盖从浏览器到数据库的全部技术层次
2. **可追溯性**：每个设计决策可追溯至需求规格说明书的对应条目
3. **可实施性**：设计粒度足够支撑编码实现
4. **规范性**：遵循 MVC 分层架构、RESTful API 规范

### 1.2 设计范围

| 维度 | 范围 |
|------|------|
| 系统架构 | B/S 架构（浏览器 → Vite Dev Server Proxy → Spring Boot → MySQL） |
| 前端设计 | SPA 应用（Vue 3 + Vite 5）的组件树、路由、状态管理、API 层、主题系统 |
| 后端设计 | Spring Boot 2.7.18 分层架构、JWT 认证、角色权限、RESTful API |
| 数据库 | MySQL 8.0 的 ER 模型、9 张核心表 |
| 安全 | JWT + 角色拦截 + CORS |
| 部署 | 前后端分离部署 |
| AI | DeepSeek API + LangChain4j RAG 知识增强 |

### 1.3 设计原则

1. **前后端分离**：前端 SPA 与后端 RESTful API 独立开发，通过 HTTP/JSON 通信
2. **分层架构**：后端严格遵循 Controller → Service → Repository 三层
3. **安全优先**：认证（JWT）、授权（角色拦截）
4. **组件复用**：前端通用组件跨页面复用
5. **渐进增强**：核心功能优先，高级特性逐步叠加

---

## 二、系统架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────┐
│                     客户端层 (Client)                      │
│  ┌────────────────────────────────────────────────┐      │
│  │        Vue 3 SPA (Vite 5, Element Plus)         │      │
│  │   Login │ Dashboard │ StudyPlan │ Course │ ...   │      │
│  └────────────────────┬───────────────────────────┘      │
└───────────────────────┼──────────────────────────────────┘
                        │ HTTP/REST (JSON)
                        ▼
┌──────────────────────────────────────────────────────────┐
│                  Vite Dev Server Proxy                     │
│  /api/*  →  http://127.0.0.1:8080/api/*                   │
└───────────────────────┼──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│                    应用服务层 (Application)                 │
│  ┌────────────────────────────────────────────────┐      │
│  │           Spring Boot 2.7.18 (Tomcat)            │      │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │      │
│  │  │Controller│→│ Service  │→│  Repository   │  │      │
│  │  │ (REST)   │  │ (Logic)  │  │ (Spring Data) │  │      │
│  │  └──────────┘  └──────────┘  └──────┬───────┘  │      │
│  │  ┌─────────────────────────────┐   │           │      │
│  │  │  AuthInterceptor (JWT)      │   │           │      │
│  │  │  + AuthContext.requireAdmin │   │           │      │
│  │  └─────────────────────────────┘   │           │      │
│  └────────────────────────────────────┼───────────┘      │
└───────────────────────────────────────┼──────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│                   数据持久层 (Data)                         │
│  ┌──────────────────────────────────────────────┐        │
│  │              MySQL 8.0 (业务数据)               │        │
│  │  users / course / study_plan / notes / ...    │        │
│  └──────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼ (AI 模块)
┌──────────────────────────────────────────────────────────┐
│              外部 AI 服务 (DeepSeek API)                    │
│  deepseek-v4-pro + RAG (LangChain4j)                      │
└──────────────────────────────────────────────────────────┘
```

### 2.2 技术栈总览

#### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 核心框架（Composition API） |
| Vite | 5.4 | 构建工具与开发服务器 |
| Vue Router | 5.0 | SPA 路由管理 |
| Pinia | 3.0 | 状态管理 |
| Element Plus | 2.14 | UI 组件库 |
| ECharts | 6.1 | 数据可视化 |
| Axios | 1.16 | HTTP 请求封装 |
| Sass | 1.100 | CSS 预处理器 |
| lodash-es | 4.18 | 工具函数（debounce） |

#### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Spring Data JPA | - | ORM（Hibernate 5.6） |
| MySQL | 8.0 | 关系型数据库 |
| JJWT | 0.12.6 | JWT Token 签发与验证 |
| LangChain4j | 0.36.2 | AI 集成（DeepSeek + RAG） |
| WebFlux | - | AI API 异步调用 |
| Maven | 3.9 | 项目构建与依赖管理 |
| Lombok | 1.18 | 简化 POJO 代码 |

### 2.3 后端分层架构

```
backend/src/main/java/com/ailearning/backend/
├── controller/          # 控制器层（10 个 Controller）
│   ├── UserController.java         # 用户登录/注册/资料
│   ├── CourseController.java       # 课程 CRUD（管理员权限）
│   ├── StudyPlanController.java    # 学习计划
│   ├── NoteController.java         # 笔记管理
│   ├── ResourceController.java     # 学习资源
│   ├── AchievementController.java  # 成就系统
│   ├── WrongQuestionController.java # 错题本
│   ├── AiController.java           # AI 对话
│   ├── AnalyticsController.java    # 数据分析
│   └── DashboardController.java    # 仪表盘
├── service/             # 业务逻辑层（含 AI/RAG 服务）
│   ├── UserService.java
│   ├── AuthService.java            # JWT 签发/验证/角色提取
│   ├── CourseService.java          # 课程 CRUD
│   ├── AiService.java             # DeepSeek API 调用
│   ├── RagService.java            # RAG 知识增强
│   ├── AnalyticsService.java
│   └── ...（其余 Service 与 Controller 一一对应）
├── repository/          # 数据访问层（Spring Data JPA）
├── entity/              # JPA 实体类
├── dto/                 # 请求/响应 DTO
├── config/              # 配置类
│   ├── WebConfig.java           # CORS + 拦截器注册
│   ├── AuthInterceptor.java     # JWT 认证 + 角色注入
│   ├── RagConfig.java           # LangChain4j 配置
│   └── DataInitializer.java     # 启动初始化
├── common/              # 公共模块
│   ├── ApiResponse.java         # 统一响应封装
│   └── AuthContext.java         # ThreadLocal 用户上下文（含角色）
└── exception/           # 全局异常处理
    ├── ApiException.java
    └── GlobalExceptionHandler.java
```

### 2.4 前端架构

#### 组件树

```
App.vue ─ <router-view> (过渡动画)
├── LoginView.vue (/login)
│   └── ParticleBackground.vue
└── AppLayout.vue (/)
    ├── ParticleBackground.vue (粒子背景)
    ├── AppSidebar.vue (12 项侧边栏导航)
    ├── AppHeader.vue (顶栏 + 主题/粒子切换)
    └── <router-view> (keep-alive 缓存)
        ├── DashboardView.vue (/dashboard)
        ├── StudyPlanView.vue (/study-plan)
        ├── PomodoroView.vue (/pomodoro)
        ├── CourseView.vue (/course) ★ 管理员可增删改
        ├── NoteView.vue (/note)
        ├── WrongQuestionView.vue (/wrong-question)
        ├── ResourceView.vue (/resource)
        ├── AchievementView.vue (/achievement)
        ├── AiAssistantView.vue (/ai-assistant)
        ├── AnalyticsView.vue (/analytics)
        └── ProfileView.vue (/profile)
```

---

## 三、前端详细设计

### 3.1 路由设计

系统共 **12 条路由**（1 条公开 + 11 条受保护）：

| 路径 | 名称 | 标题 | 需登录 |
|------|------|------|--------|
| `/login` | Login | 登录 | 否 |
| `/dashboard` | Dashboard | 首页 | 是 |
| `/study-plan` | StudyPlan | 学习计划 | 是 |
| `/pomodoro` | Pomodoro | 番茄专注 | 是 |
| `/course` | Course | 课程管理 | 是 |
| `/note` | Note | 笔记管理 | 是 |
| `/wrong-question` | WrongQuestion | 错题本 | 是 |
| `/resource` | Resource | 学习资源 | 是 |
| `/achievement` | Achievement | 成就系统 | 是 |
| `/ai-assistant` | AiAssistant | AI 助手 | 是 |
| `/analytics` | Analytics | 数据分析 | 是 |
| `/profile` | Profile | 个人中心 | 是 |

**导航守卫**（`router/index.js` beforeEach）：
- 未认证 + 需要认证 → 重定向 `/login?redirect=<原始路径>`
- 已认证 + 访问 `/login` → 重定向 `/dashboard`
- 路由懒加载（动态 import + chunk 命名）
- 预留 `asyncRoutes` 供动态路由扩展

### 3.2 状态管理设计

使用 Pinia 管理 **8 个 Store**，均支持 localStorage 持久化：

- **useUserStore**：token、userInfo（含 role 字段）、isLoggedIn、isAdmin
- **useThemeStore**：isDark、showParticles、sidebarCollapsed
- **useStudyPlanStore**：plans、total、loading、query
- **useNoteStore**：notes、categories、CRUD 操作
- **useResourceStore**：resources、上传/下载
- **useAchievementStore**：achievements、同步
- **useWrongQuestionStore**：wrongQuestions、筛选
- **usePomodoroStore**：计时器状态、阶段管理

### 3.3 API 层设计

**Axios 实例**（`src/api/request.js`）：
- `baseURL: '/api'`（通过 Vite proxy 转发）
- `timeout: 15000`（AI 请求 60000）
- 请求拦截器：自动注入 JWT Token、全局 Loading
- 响应拦截器：401 自动登出、业务错误提示、支持静默模式

### 3.4 主题系统

深色/浅色双主题，通过 CSS 变量实现：`data-theme` 属性切换 + Element Plus dark class + ECharts 联动

### 3.5 性能优化

路由懒加载、ECharts 按需引入、图片懒加载（v-lazy 指令 + IntersectionObserver）、keep-alive 页面缓存、搜索防抖（400ms lodash-es debounce）、gzip/brotli 预压缩

---

## 四、后端详细设计

### 4.1 角色权限模型

采用简化的角色模型（User 实体中 `role` 字段）：

| 角色 | 权限 |
|------|------|
| `admin` | 全部功能 + 课程增删改 |
| `user` | 个人学习管理、查看课程 |

**权限实现**：
- JWT Token 包含 `role` 声明
- `AuthInterceptor` 从 JWT 提取角色 → 注入 `AuthContext`
- Controller 方法调用 `AuthContext.requireAdmin()` 进行鉴权
- 非管理员访问返回 `403 Forbidden`

### 4.2 JWT 认证流程

#### 登录流程

1. 客户端 POST `/api/user/login`（username, password）
2. 后端查询用户 → 明文比对密码（演示项目）
3. 生成 JWT（HS256，24h 过期，Claims：userId, username, nickname, role）
4. 返回 `{ token, userInfo }`

#### 后续请求认证

1. 客户端请求带 `Authorization: Bearer <token>`
2. `AuthInterceptor` 提取 Token → 验证签名/有效期
3. 解析 userId + role → 存入 `AuthContext` ThreadLocal
4. 请求结束后 `afterCompletion` 清除 ThreadLocal

### 4.3 核心 API

系统共 **47 个 API 端点**（10 个 Controller），关键端点如下：

#### 课程模块（含管理员 CRUD）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/api/course/list` | 所有用户 |
| GET | `/api/course/{id}` | 所有用户 |
| POST | `/api/course` | **仅管理员** |
| PUT | `/api/course/{id}` | **仅管理员** |
| DELETE | `/api/course/{id}` | **仅管理员** |

#### AI 助手模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | DeepSeek 对话（含 RAG 增强） |
| GET | `/api/ai/quick-questions` | 快捷问题列表 |

#### 其他模块

用户认证（login/register/profile）、Dashboard、学习计划 CRUD、笔记 CRUD、资源管理、成就系统、错题本、数据分析 — 完整端点清单见各 Controller。

### 4.4 全局异常处理

`@RestControllerAdvice` 统一处理异常，响应格式 `{ code, message, data }`：

| 异常类型 | HTTP 状态码 | code |
|---------|------------|------|
| ApiException | 自定义 | 自定义 |
| MethodArgumentNotValidException | 400 | 400 |
| Exception | 500 | 500 |

---

## 五、数据库设计

### 5.1 表结构

系统共 **9 张核心表**：

| 表名 | 说明 | 主键类型 |
|------|------|---------|
| `users` | 用户表（含 role 字段） | BIGINT AUTO_INCREMENT |
| `course` | 课程表 | VARCHAR(40) |
| `course_chapter` | 课程章节表 | VARCHAR(50) |
| `course_knowledge_point` | 课程知识点表 | BIGINT AUTO_INCREMENT |
| `study_plan` | 学习计划表 | BIGINT AUTO_INCREMENT |
| `resources` | 学习资源表 | BIGINT AUTO_INCREMENT |
| `notes` | 笔记表 | BIGINT AUTO_INCREMENT |
| `achievements` | 成就表 | BIGINT AUTO_INCREMENT |
| `wrong_questions` | 错题本表 | BIGINT AUTO_INCREMENT |

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

其余表 DDL 见 `backend/src/main/resources/schema.sql`。

### 5.3 JPA 映射要点

- 使用 Spring Data JPA + Hibernate，`ddl-auto: update` 自动建表/更新表结构
- `Course.knowledgePoints`：`@ElementCollection` + `@OrderColumn`
- `Course.chapters`：`@OneToMany` 级联 ALL
- `User`：唯一约束 `@Column(unique = true)` on username
- 实体使用 Lombok `@Data` 简化代码

---

## 六、安全设计

### 6.1 认证安全

| 措施 | 实现 |
|------|------|
| Token 生成 | JWT，HS256 签名，24 小时过期 |
| Token 验证 | AuthInterceptor 拦截 /api/** 请求 |
| Token 黑名单 | `ConcurrentHashMap` 内存黑名单（退出登录后失效） |
| 密码存储 | 明文（演示项目，生产环境应替换为 BCrypt） |

### 6.2 授权安全

| 措施 | 实现 |
|------|------|
| 角色权限 | User.role 字段 + JWT role 声明 |
| 接口鉴权 | `AuthContext.requireAdmin()` |
| 数据隔离 | 通过 JWT userId 过滤个人数据 |

### 6.3 防护措施

- CORS：`allowedOriginPatterns(*)`（开发环境），生产应限制具体域名
- XSS：Vue 默认转义输出
- SQL 注入：Spring Data JPA 参数化查询

---

## 七、部署设计

### 7.1 开发环境

| 组件 | 端口 | 说明 |
|------|------|------|
| Vite Dev Server | 5173 | 前端开发服务器，代理 /api 到 8080 |
| Spring Boot | 8080 | 后端 API 服务 |
| MySQL 8.0 | 3307 | 数据库 |

```
Vite :5173  ──(proxy /api)──►  Spring Boot :8080  ──►  MySQL :3307
```

### 7.2 构建与启动

```bash
# 前端
npm install
npm run dev        # 开发
npm run build      # 生产构建

# 后端
cd backend
mvn spring-boot:run         # 开发
mvn package -DskipTests     # 打包
java -jar target/backend-1.0.0.jar
```

---

## 八、AI 集成设计

### 8.1 DeepSeek API

- 模型：`deepseek-v4-pro`
- 调用方式：WebClient（WebFlux），60s 超时
- 请求参数：model、messages（system + user）、thinking、reasoning_effort
- 响应解析：`choices[0].message.content`

### 8.2 RAG 知识增强

- 框架：LangChain4j 0.36.2
- 知识源：用户笔记 + 错题内容
- 流程：用户提问 → RagService 检索用户知识库 → 构建增强 Prompt → 发送至 DeepSeek

---

> **文档编制**：本设计说明书基于实际代码（前端 `src/` + 后端 `backend/src/main/java/`）编写，与项目代码完全一致。完整数据库脚本见 `backend/src/main/resources/schema.sql`，组件接口说明见 `docs/COMPONENTS.md`。
