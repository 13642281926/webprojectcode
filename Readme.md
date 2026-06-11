# AI 学习成长助手平台（AI Learning Growth Studio）

基于 **Vue 3 + Spring Boot + MySQL 8.0** 的全栈学习助手平台，集成 DeepSeek AI 对话、RAG 知识增强、数据分析、课程管理、角色权限等功能。

---

## 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| Node.js | >= 20.19 |
| Java | 17 (JDK) |
| Maven | >= 3.6 |
| MySQL | 8.0 |

### 启动前端

```bash
npm install
npm run dev        # http://localhost:5173
```

### 启动后端

```bash
# 1. 确保 MySQL 8.0 已启动
# 2. 修改 backend/src/main/resources/application.yml 中的数据库密码
# 3. 启动后端
cd backend
mvn spring-boot:run   # http://localhost:8080
```

首次启动会自动建库建表，并执行 `schema.sql` 导入演示数据。

### 数据库配置

| 配置项 | 默认值 |
|--------|--------|
| 地址 | `localhost:3307` |
| 数据库 | `ai_learning` |
| 用户 | `root` |
| 密码 | `123456` |

可在 `backend/src/main/resources/application.yml` 中修改。

### 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | **管理员**（可管理课程） |

> 新注册用户默认为普通用户，无法进行课程增删改操作。

---

## 常见问题

| 问题 | 处理方式 |
|------|----------|
| 端口 5173 被占用 | 修改 `vite.config.js` 中 `server.port` |
| 端口 8080 被占用 | 修改 `application.yml` 中 `server.port` 或关闭旧进程 |
| 数据库连接失败 | 确认 MySQL 已启动，检查 `application.yml` 中密码和端口 |
| 登录后刷新退出 | 检查浏览器是否禁用 localStorage |
| AI 问答超时 | DeepSeek 推理较慢，已设置 60s 超时 |

---

## 项目结构

```
zprojectcode/
├── src/                           # Vue 3 前端
│   ├── api/                       # Axios 接口封装
│   ├── components/
│   │   ├── common/                # 通用组件
│   │   └── layout/               # 布局组件
│   ├── directives/                # 自定义指令 (v-lazy)
│   ├── router/                    # 路由 + 导航守卫
│   ├── stores/                    # Pinia 状态管理
│   ├── styles/                    # SCSS 主题
│   ├── utils/                     # 工具函数
│   └── views/                     # 业务页面
├── backend/                       # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml    # 数据库 / DeepSeek 配置
│       │   └── schema.sql         # 数据库初始化脚本
│       └── java/com/ailearning/backend/
│           ├── controller/        # REST 控制器
│           ├── service/           # 业务逻辑（含 AI / RAG）
│           ├── entity/            # JPA 实体
│           ├── repository/        # 数据访问层
│           ├── dto/               # 请求 / 响应 DTO
│           ├── config/            # CORS / JWT 拦截器 / RAG
│           ├── common/            # 通用工具（AuthContext 等）
│           └── exception/         # 全局异常处理
├── docs/                          # 组件文档
├── vite.config.js
└── package.json
```

---

## 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | Composition API | 核心框架 |
| Vite | 5.x | 构建与开发服务器 |
| Vue Router | 5.x | 路由、懒加载、导航守卫 |
| Pinia | 3.x | 状态管理 + localStorage 持久化 |
| Axios | 1.x | HTTP 请求封装 |
| Element Plus | 2.x | UI 组件库 |
| ECharts | 6.x | 图表（按需引入） |
| Sass | - | 全局样式与变量 |
| lodash-es | 4.x | 防抖 |

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | Web 框架 |
| Spring Data JPA | - | ORM |
| MySQL | 8.0 | 数据库 |
| JJWT | 0.12.6 | JWT 认证 |
| LangChain4j | 0.36.2 | AI 集成（DeepSeek + RAG） |
| WebFlux | - | AI API 异步调用 |

---

## 功能模块

### 课程管理（管理员 + CRUD）

- 分类筛选 + 关键字搜索（400ms 防抖）
- 课程详情抽屉（封面、知识点、进度条、章节时间轴）
- **管理员**可添加 / 编辑 / 删除课程，普通用户仅可查看
- 管理员权限基于 JWT `role` 声明 + `AuthInterceptor` 校验

### AI 学习助手

- 接入 DeepSeek API（`deepseek-v4-pro` 模型）
- RAG 知识增强：基于用户笔记/错题构建增强 Prompt
- 快捷问题推荐 + 回复保存为笔记
- 后端 WebClient 60s 超时

### 数据分析

- 学习时长月度趋势折线图
- 每周学习分布柱状图
- 任务完成率环形图
- 课程学习进度列表
- 学习热力图（日历视图）

### 角色权限

| 角色 | 权限 |
|------|------|
| `admin` | 全部功能 + 课程增删改 |
| `user` | 查看课程、个人学习管理 |

JWT Token 包含 `role` 字段，后端通过 `AuthContext.requireAdmin()` 进行鉴权。

---

## 配置说明

### 前端

- **Vite 代理**：`/api` → `http://127.0.0.1:8080`
- **baseURL**：`/api`（Axios）
- **路径别名**：`@` → `src/`
- **Element Plus**：按需自动注册
- **主题**：深色/浅色双主题，localStorage 持久化

### 后端

- **端口**：8080
- **数据库**：MySQL 8.0，`ddl-auto: update`
- **JWT**：24 小时过期
- **文件上传**：`./uploads/`
- **DeepSeek**：API Key 配置在 `application.yml`

### 状态持久化（Pinia + localStorage）

| Store | Key | 内容 |
|-------|-----|------|
| user | `ai-learning-user` | token、用户信息、角色 |
| theme | `ai-learning-theme` | 深色模式、粒子开关、侧栏折叠 |

---

## 路由一览

| 路径 | 页面 | 需登录 |
|------|------|--------|
| `/login` | 登录 | 否 |
| `/dashboard` | 首页 | 是 |
| `/study-plan` | 学习计划 | 是 |
| `/pomodoro` | 番茄专注 | 是 |
| `/course` | 课程管理 | 是 |
| `/note` | 笔记管理 | 是 |
| `/wrong-question` | 错题本 | 是 |
| `/resource` | 学习资源 | 是 |
| `/achievement` | 成就系统 | 是 |
| `/ai-assistant` | AI 助手 | 是 |
| `/analytics` | 数据分析 | 是 |
| `/profile` | 个人中心 | 是 |

- 路由懒加载 + chunk 命名
- 导航守卫：未登录 → `/login`
- 预留 `asyncRoutes` 动态路由扩展

---

## 技术考核覆盖

| 考核项 | 实现 |
|--------|------|
| Vue3 Composition API | 全部页面 `<script setup>` |
| Vue Router | 懒加载、守卫、`meta.requiresAuth` |
| Pinia | user / studyPlan / theme / notes 等 8 个 Store |
| Axios | 统一封装、拦截器、JWT 注入 |
| Element Plus | 按需引入 + 深色主题 |
| ECharts | Dashboard、数据分析页（含热力图） |
| 组件化 | StatCard / ChartCard / CourseCard / AIChatBox 等 |
| 自定义指令 v-lazy | IntersectionObserver 图片懒加载 |
| KeepAlive | AppLayout 缓存页面 |
| provide/inject | AppLayout → ChartCard 主题跨级通信 |
| 动态组件 | `<component :is>` 图标动态渲染 |
| 深色/浅色双主题 | CSS 变量 + `data-theme` + ECharts 联动 |
| Lodash | debounce 搜索防抖 |
| gzip/brotli 压缩 | vite-plugin-compression |
| Spring Boot | REST API + JPA + JWT |
| 角色权限 | admin/user + AuthInterceptor |
| RAG 检索增强 | LangChain4j + DeepSeek |

---

## 项目阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| 阶段一 | Vite 工程初始化 | ✅ |
| 阶段二 | 布局/主题/Pinia/Axios | ✅ |
| 阶段三 | 业务页面 | ✅ |
| 阶段四 | Spring Boot + MySQL + JWT | ✅ |
| 阶段五 | 打磨与考核 | ✅ |
| 阶段六 | Lighthouse 优化 | ✅ |
| 阶段七 | UI/UX 重构 | ✅ |
| 阶段八 | DeepSeek AI + RAG + 角色系统 + 课程 CRUD | ✅ |

---

## 需求说明

### 项目背景

基于 Vue3 的 Web 前端课程项目，实现科技感 AI 学习成长助手平台，覆盖 Vue Router、Pinia、Axios、组件化、动态路由、状态管理等核心技术。

### 技术必选

Vue3、Vite、Vue Router、Pinia、Axios、Element Plus、ECharts、Sass

### 风格要求

现代科技风、深色系、渐变高光、毛玻璃、动态粒子背景、平滑动画

### 页面清单

登录、Dashboard、学习计划、番茄专注、课程管理、笔记管理、错题本、学习资源、成就系统、AI 助手、数据分析、个人中心
