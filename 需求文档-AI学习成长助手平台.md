# AI 学习成长助手平台 —— 需求规格说明书

> **项目名称**：AI 学习成长助手平台（AI Learning Growth Studio）
> **所属课程**：信息系统综合实训
> **技术路线**：Spring Boot 2.7.18 + Vue 3.5 + Spring Data JPA + MySQL 8.0 + Element Plus + DeepSeek AI
> **文档版本**：v2.1
> **编写日期**：2026-06-12

---

## 一、引言

### 1.1 项目背景

随着人工智能技术在教育领域的深入应用，个性化学习辅助工具已成为提升学习效率的重要手段。当前高校学生面临课程管理分散、学习计划缺乏系统跟踪、学习数据分析不足等痛点。

本项目顺应教育信息化趋势，基于 Web 全栈技术构建面向高校学生的 AI 学习成长助手平台，帮助学习者科学规划学习任务、系统管理课程资源、获取 AI 智能学习建议、并通过数据可视化直观了解自身学习状态。

### 1.2 项目目标

1. **构建全栈学习管理平台**：Spring Boot + Vue 3 前后端分离架构，实现学习计划、课程管理、AI 智能问答、数据分析等 12 个功能模块
2. **实现角色权限管理**：基于 JWT 认证 + User 实体 role 字段的权限控制（admin / user），管理员可进行课程 CRUD
3. **接入真实 AI 大模型**：集成 DeepSeek API（deepseek-v4-pro）+ LangChain4j RAG 知识增强，提供智能学习问答
4. **提供数据驱动的学习洞察**：集成 ECharts 6.1，支持折线图、柱状图、环形图、热力图等多维度数据可视化
5. **符合工程化开发规范**：RESTful API 设计、MVC 分层架构、统一 ApiResponse 响应格式、全局异常处理

### 1.3 项目范围

**范围内**：

| 模块分类 | 具体内容 |
|---------|---------|
| 用户认证 | 登录、注册、退出登录、JWT 会话管理、路由导航守卫 |
| 首页仪表盘 | 统计卡片、学习趋势图表、快捷入口、AI 推荐、每日名言 |
| 学习计划 | 计划 CRUD、优先级/状态筛选、表格/卡片双视图切换 |
| 番茄专注 | 25 分钟倒计时、任务绑定、阶段切换（专注/短休/长休）、统计复盘 |
| 课程管理 | 课程浏览（卡片网格 + 分类筛选 + 关键词搜索）、管理员 CRUD（增删改） |
| AI 助手 | DeepSeek 对话、RAG 知识增强、快捷问题、回复保存为笔记 |
| 笔记管理 | 笔记 CRUD、分类筛选、标签支持 |
| 错题本 | 错题 CRUD、难度/状态筛选、掌握标记 |
| 学习资源 | 文件上传/下载、分类浏览 |
| 成就系统 | 自动同步学习数据、8 项预设成就、稀有度分级 |
| 数据分析 | 月度趋势、周时长分布、任务分布、课程进度、学习热力图 |
| 个人中心 | 资料编辑、学习统计、退出登录 |

**范围外**：移动端 App、在线支付、第三方课程平台集成、OAuth2.0 第三方登录、实时消息推送

### 1.4 术语定义

| 术语 | 说明 |
|------|------|
| SPA | 单页应用（Single Page Application），前端路由驱动页面切换，无整页刷新 |
| JWT | JSON Web Token，无状态身份认证令牌，HS256 签名，24 小时过期 |
| RESTful | 基于 REST 风格的 API 设计，使用 HTTP 方法语义（GET/POST/PUT/DELETE） |
| CRUD | 增（Create）、删（Delete）、改（Update）、查（Read）四项基本数据操作 |
| RAG | 检索增强生成（Retrieval-Augmented Generation），将用户知识库检索结果注入 AI 对话上下文 |
| ORM | 对象关系映射，本项目使用 Spring Data JPA（底层 Hibernate 5.6） |
| Pinia | Vue 3 官方状态管理库，替代 Vuex |
| DTO | 数据传输对象（Data Transfer Object），用于接口层请求/响应封装 |

---

## 二、系统概述

### 2.1 系统描述

AI 学习成长助手平台是基于 B/S 架构的 Web 单页应用（SPA）。前端采用 Vue 3 Composition API + Vite 5 构建工具 + Element Plus 组件库，后端基于 Spring Boot 2.7.18 提供 RESTful API，数据层使用 MySQL 8.0 持久化存储（JPA `ddl-auto: update` 自动管理表结构），AI 模块集成 DeepSeek API + LangChain4j RAG 实现智能问答。

开发环境通过 Vite Dev Server（端口 5173）代理 `/api` 请求至 Spring Boot（端口 8080），MySQL 使用端口 3307。

### 2.2 用户角色

系统采用简化的双角色模型，角色信息存储在 `users` 表的 `role` 字段中，并在 JWT Token 的 Claims 中携带：

| 角色 | 标识 | 权限范围 |
|------|------|---------|
| 管理员 | `admin` | 全部功能 + 课程增删改（POST/PUT/DELETE `/api/course`） |
| 普通用户 | `user` | 个人学习管理（计划、笔记、错题、资源、成就）、查看课程、AI 对话、数据分析 |

**默认账号**：
- 管理员：`admin` / `admin123`（由 `DataInitializer` 启动时自动创建）
- 普通用户：`zhangsan` / `123456`（同上）
- 新注册用户默认角色为 `user`

### 2.3 功能模块总览

系统共 **12 个功能模块**，对应 12 条前端路由：

```
┌──────────────────────────────────────────────────────────────────┐
│                     AI 学习成长助手平台                           │
├──────────┬──────────┬──────────┬──────────┬─────────────────────┤
│ 1.登录   │ 2.Dash-  │ 3.学习   │ 4.番茄   │ 5.课程管理          │
│   认证   │  board   │   计划   │   专注   │  ★ 管理员 CRUD      │
├──────────┼──────────┼──────────┼──────────┼─────────────────────┤
│ 6.笔记   │ 7.错题   │ 8.学习   │ 9.成就   │ 10.AI 助手          │
│   管理   │   本     │   资源   │   系统   │  DeepSeek + RAG      │
├──────────┼──────────┼──────────┼──────────┼─────────────────────┤
│ 11.数据  │ 12.个人  │          │          │                     │
│   分析   │   中心   │          │          │                     │
└──────────┴──────────┴──────────┴──────────┴─────────────────────┘
```

---

## 三、功能需求

### 3.1 登录与认证模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-AUTH-001 | 用户登录 | 账号密码登录，验证成功返回 JWT Token + 用户信息 | 用户名 3-20 字符，密码 6-32 字符；登录页预填演示账号 `admin/123456` |
| REQ-AUTH-002 | 用户注册 | 新用户注册，成功后自动登录 | 用户名唯一性校验；密码与确认密码一致性校验；自动生成 DiceBear 头像 |
| REQ-AUTH-003 | 路由守卫 | 未登录用户访问受保护页面自动跳转登录页 | 保存原始路径（`redirect` 参数），登录成功后跳回 |
| REQ-AUTH-004 | 会话持久化 | 刷新浏览器后自动恢复登录状态 | Token + userInfo（含 role 字段）存储于 localStorage |
| REQ-AUTH-005 | 退出登录 | 清除本地 Token 和用户信息，Token 加入服务端黑名单 | 后端维护 `ConcurrentHashMap` 内存黑名单；`POST /api/user/logout` |
| REQ-AUTH-006 | 粒子背景 | 登录页展示 Canvas 粒子动画背景 | 60 个漂浮粒子，距离 < 120px 连线，`requestAnimationFrame` 驱动 |

### 3.2 首页仪表盘

| 编号 | 功能 | 描述 | 数据来源 |
|------|------|------|---------|
| REQ-DASH-001 | 欢迎区域 | 显示用户昵称 + 每日名言（随机切换） | 用户信息 + 前端内置名言库 |
| REQ-DASH-002 | AI 推荐卡片 | 展示一条 AI 学习建议 | 前端静态推荐内容 |
| REQ-DASH-003 | 快捷入口 | 4 个快捷操作入口（学习计划/课程/AI 助手/数据分析） | 前端常量配置 `QUICK_ENTRIES` |
| REQ-DASH-004 | 今日统计 | 今日学习时长（分钟）、任务完成数、完成率 | `GET /api/dashboard/stats` |
| REQ-DASH-005 | 数据面板 | 课程、成就、任务、错题 4 个统计面板 | 并行拉取多个 API |
| REQ-DASH-006 | 最近笔记 | 展示最近更新的笔记列表 | `GET /api/note/list` |

### 3.3 学习计划模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-PLAN-001 | 计划列表 | 分页展示用户学习计划，支持表格/卡片双视图切换 | 后端分页查询，默认 10 条/页 |
| REQ-PLAN-002 | 多条件筛选 | 按优先级（高/中/低）、状态（待开始/进行中/已完成）、关键词筛选 | 400ms 防抖搜索（lodash-es debounce） |
| REQ-PLAN-003 | 新增计划 | 创建学习计划 | 标题必填（≤120 字符），默认优先级=中，默认状态=待开始 |
| REQ-PLAN-004 | 编辑计划 | 修改计划信息 | 仅创建者可编辑（后端 userId 校验） |
| REQ-PLAN-005 | 删除计划 | 删除计划 | 删除前确认对话框 |
| REQ-PLAN-006 | 状态切换 | 循环切换计划状态 | 待开始 → 进行中 → 已完成 → 待开始 |

### 3.4 番茄专注模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-POMO-001 | 计时器 | 25 分钟专注倒计时，环形进度条展示 | 使用 `Date.now()` 精确计时，支持后台标签页恢复校准 |
| REQ-POMO-002 | 任务绑定 | 可绑定自由任务/课程/学习计划 | 下拉选择，从课程列表和学习计划列表加载 |
| REQ-POMO-003 | 阶段切换 | 专注（25 分钟）→ 短休（5 分钟）→ 长休（15 分钟） | 每 4 个专注周期后触发长休 |
| REQ-POMO-004 | 浏览器通知 | 计时结束时发送 Notification API 通知 | 浏览器不支持时回退 ElMessage 提示 |
| REQ-POMO-005 | 统计复盘 | 完成弹窗展示本次番茄钟统计 + 质量评分 | 支持沉淀内容到笔记和错题本 |
| REQ-POMO-006 | 数据图表 | 周趋势、高峰时段、课程分布 3 个 ECharts 图表 | 由 `usePomodoroStore` 提供计算属性数据 |

### 3.5 课程管理模块 ★

| 编号 | 功能 | 描述 | 权限 | 业务规则 |
|------|------|------|------|---------|
| REQ-COUR-001 | 课程浏览 | 卡片网格展示所有课程，含封面、标题、分类、讲师、课时、进度条 | 所有用户 | 图片懒加载（`LazyImage` 组件 + IntersectionObserver） |
| REQ-COUR-002 | 分类筛选 | 按课程分类筛选（前端/后端/AI/英语/数学） | 所有用户 | 与关键词搜索联动 |
| REQ-COUR-003 | 关键词搜索 | 按课程标题关键词搜索 | 所有用户 | 400ms 防抖 |
| REQ-COUR-004 | 课程详情 | 抽屉（el-drawer）展示：封面、描述、讲师、知识点标签、章节时间轴、进度条 | 所有用户 | `GET /api/course/{id}` |
| REQ-COUR-005 | 添加课程 | 表单：ID/标题/类别/封面 URL/描述/讲师/课时数 | **仅管理员** | `POST /api/course`；AuthContext.requireAdmin() |
| REQ-COUR-006 | 编辑课程 | 卡片悬停显示编辑按钮，表单回填课程数据 | **仅管理员** | `PUT /api/course/{id}` |
| REQ-COUR-007 | 删除课程 | 确认对话框 → 删除 | **仅管理员** | `DELETE /api/course/{id}` |

### 3.6 AI 助手模块

| 编号 | 功能 | 描述 | 技术实现 |
|------|------|------|---------|
| REQ-AI-001 | AI 对话 | 用户输入问题，后端调用 DeepSeek API 返回回答 | `POST /api/ai/chat`；模型 `deepseek-v4-pro`；60s 超时；消息气泡展示（用户右对齐/AI 左对齐） |
| REQ-AI-002 | RAG 知识增强 | 基于用户上传资源和笔记构建增强 Prompt | LangChain4j 0.36.2：文档解析 → 500 字符分块 → DeepSeek text-embedding-v3 向量化 → InMemoryEmbeddingStore → 语义检索（相似度阈值 0.7） |
| REQ-AI-003 | 快捷问题 | 预设 5 类快捷问题（考研/英语/Vue/时间管理/算法） | 优先从 `GET /api/ai/quick-questions` 获取，失败回退前端硬编码 |
| REQ-AI-004 | 保存为笔记 | AI 回复内容一键保存到笔记 | 调用 `POST /api/note/create` |
| REQ-AI-005 | Typing 动画 | AI 回复时显示打字动画效果 | 前端 AIChatBox 组件控制 |
| REQ-AI-006 | 错误处理 | API 调用失败时显示友好错误提示 | 后端 try-catch + 前端 silent 模式请求 |

### 3.7 笔记管理模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-NOTE-001 | 笔记列表 | 卡片网格展示所有笔记，含分类标签 | 按更新时间倒序排列 |
| REQ-NOTE-002 | 分类筛选 | 按分类筛选（学习/思考/计划） | 与关键词搜索联动；400ms 防抖 |
| REQ-NOTE-003 | 新增笔记 | 抽屉编辑器创建笔记 | 标题必填（≤150 字符），内容支持多行文本（TEXT 类型） |
| REQ-NOTE-004 | 编辑笔记 | 抽屉编辑器修改笔记 | 仅创建者可编辑 |
| REQ-NOTE-005 | 删除笔记 | 删除笔记 | 删除前确认 |
| REQ-NOTE-006 | 标签系统 | 支持多选标签 + 动态创建标签 | el-select 的 `multiple` + `allow-create` 属性 |

### 3.8 错题本模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-WRQ-001 | 错题列表 | 列表展示所有错题，左侧彩色边框表示状态 | 红色=新题目、黄色=复习中、绿色=已掌握 |
| REQ-WRQ-002 | 多条件筛选 | 按分类（数学/英语/计算机）、难度（简单/中等/困难）、状态筛选 | 后端分页查询 |
| REQ-WRQ-003 | 新增错题 | 添加错题，含标题、内容、答案、解析、分类、难度 | `POST /api/wrongQuestion/create` |
| REQ-WRQ-004 | 编辑错题 | 修改错题信息 | 仅创建者可编辑 |
| REQ-WRQ-005 | 删除错题 | 删除错题 | 删除前确认 |
| REQ-WRQ-006 | 掌握标记 | 将错题标记为"已掌握" | `POST /api/wrongQuestion/master/{id}`；状态变为 mastered |
| REQ-WRQ-007 | 查看详情 | 只读模式查看错题详情（含答案和解析） | `GET /api/wrongQuestion/detail/{id}` |

### 3.9 学习资源模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-RES-001 | 资源列表 | 网格布局展示所有资源，含类型图标 | 后端分页查询 |
| REQ-RES-002 | 分类/类型筛选 | 按分类 + 文件类型筛选 | 分类：课程资料/参考书籍/笔记/其他；类型：PDF/视频/图片/文档 |
| REQ-RES-003 | 文件上传 | 上传文件资源（支持拖拽） | `POST /api/resource/upload`（multipart/form-data）；UUID 重命名；自动注入 RAG 知识库 |
| REQ-RES-004 | 创建资源 | 创建外部链接资源记录 | `POST /api/resource/create`（无文件上传） |
| REQ-RES-005 | 下载资源 | 下载文件 | `GET /api/resource/download/{id}`；下载计数 +1；Blob URL 触发下载 |
| REQ-RES-006 | 删除资源 | 删除资源记录 | 删除前确认 |

### 3.10 成就系统模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-ACH-001 | 成就总览 | Hero 区域展示环形进度指示器（已解锁/总数） | `GET /api/achievement/stats` |
| REQ-ACH-002 | 统计卡片 | 积分、解锁数、连续天数、最高连击 4 张卡片 | 来自成就统计数据 |
| REQ-ACH-003 | 成就网格 | 按稀有度着色展示所有成就 | 稀有度：普通/稀有/珍贵/史诗/传说（对应不同颜色） |
| REQ-ACH-004 | 分类筛选 | 按分类（学习/习惯/任务/创作/成长）和状态（已解锁/未解锁）筛选 | 前端筛选 |
| REQ-ACH-005 | 数据同步 | 自动同步学习数据计算成就进度 | 后端评估函数：笔记数→创作成就、错题数→学习成就、学习天数→习惯成就、资源数→收藏成就 |
| REQ-ACH-006 | 传说成就 | "博学者"成就需其他 7 项成就全部解锁 | `AchievementService` 中特殊逻辑判断 |
| REQ-ACH-007 | 初始化 | 首次使用或重置时初始化成就列表 | `POST /api/achievement/init?reset=true` 可重置 |

**8 项预设成就**：

| 成就名称 | 类别 | 解锁条件 | 稀有度 |
|---------|------|---------|--------|
| 初次登录 | 成长 | 首次登录 | 普通 |
| 学习新手 | 学习 | 创建 1 个学习计划 | 普通 |
| 学习达人 | 学习 | 创建 5 个学习计划 | 稀有 |
| 笔记新手 | 创作 | 创建 1 篇笔记 | 普通 |
| 笔记达人 | 创作 | 创建 10 篇笔记 | 珍贵 |
| 错题克星 | 学习 | 掌握 5 道错题 | 稀有 |
| 资源收藏家 | 任务 | 上传 10 个资源 | 稀有 |
| 博学者 | 成长 | 其他 7 项全部解锁 | 传说 |

### 3.11 数据分析模块

| 编号 | 功能 | 描述 | 图表类型 | 数据来源 |
|------|------|------|---------|---------|
| REQ-ANA-001 | 月度学习趋势 | 近 6 个月学习时长趋势 | 折线图 | `GET /api/analytics/dashboard` |
| REQ-ANA-002 | 周时长分布 | 周一至周日学习时长对比 | 柱状图 | `GET /api/analytics/dashboard` |
| REQ-ANA-003 | 任务完成分布 | 待完成/进行中/已完成任务分布 | 环形图 | `GET /api/analytics/tasks` |
| REQ-ANA-004 | 课程进度对比 | 各课程学习进度横向对比 | 横向柱状图 | `GET /api/analytics/dashboard` |
| REQ-ANA-005 | 学习热力图 | 近 12 周每日学习时长热力图 | 日历热力图 | `GET /api/analytics/dashboard` |
| REQ-ANA-006 | 统计卡片 | 总课程数、总笔记数、总错题数、资源数 | 数字卡片 | `GET /api/analytics/overview` |
| REQ-ANA-007 | 洞察面板 | 最佳学习时段分析 + 学习趋势预测 | 文本分析 | 前端基于数据计算 |

### 3.12 个人中心模块

| 编号 | 功能 | 描述 | 业务规则 |
|------|------|------|---------|
| REQ-PROF-001 | 统计概览 | 展示学习天数、总时长、连续天数 3 张卡片 | `GET /api/user/profile` |
| REQ-PROF-002 | 资料编辑 | 修改昵称、个性签名、头像 URL | `PUT /api/user/profile`；昵称必填 |
| REQ-PROF-003 | 个人笔记 | 展示用户最近的笔记列表 | `GET /api/note/list` |
| REQ-PROF-004 | 退出登录 | 清除登录状态并跳转登录页 | 调用 `POST /api/user/logout` + 前端清除 localStorage |

---

## 四、非功能需求

### 4.1 性能需求

| 指标 | 目标值 | 实现措施 |
|------|--------|---------|
| 页面首次加载 | < 3 秒 | 路由懒加载（动态 import + chunk 命名）、ECharts 按需引入（仅 line/bar/pie/heatmap + 必要组件，约 350KB vs 全量 1MB） |
| API 响应时间 | < 500ms（不含 AI 请求） | JPA 参数化查询、合理索引（user_id、course_id 外键索引） |
| AI 请求超时 | 60 秒 | axios 请求级 timeout 配置、WebClient 超时设置 |
| 前端构建产物 | Gzip 后 < 2.5MB | Vite 代码分割（echarts、element-plus、vue-vendor 独立 chunk）、gzip + brotli 预压缩（vite-plugin-compression，阈值 1KB） |
| 搜索响应 | < 200ms | 客户端 400ms 防抖 + 后端 Stream API 流式过滤 |

### 4.2 安全需求

| 安全项 | 实现方案 | 说明 |
|--------|---------|------|
| 身份认证 | JWT Token，HMAC-SHA256 签名，24 小时过期 | `AuthInterceptor` 拦截 `/api/**`，排除 `/api/user/login` 和 `/api/user/register` |
| Token 黑名单 | `ConcurrentHashMap<String, Long>` 内存存储 | 退出登录时 Token 加入黑名单，过期自动清理 |
| 角色权限 | `User.role` 字段 + JWT role Claims + `AuthContext.requireAdmin()` | 管理员接口：课程 POST/PUT/DELETE；非管理员返回 403 |
| 数据隔离 | 所有查询通过 `AuthContext.getCurrentUserId()` 过滤 | 用户只能操作自己的计划、笔记、错题、资源 |
| 密码存储 | 明文存储（演示项目） | 生产环境建议替换为 BCrypt |
| XSS 防护 | Vue 默认模板转义输出 | 前端框架级保护 |
| SQL 注入 | Spring Data JPA 参数化查询 | `findByUsername(String)` 等方法使用 `?` 占位符 |
| CORS | 后端 `allowedOriginPatterns("*")` | 开发环境允许所有来源；生产环境应限制具体域名 |

### 4.3 可用性需求

- **响应式布局**：适配 1366×768 至 1920×1080 分辨率，1200px / 992px / 768px 三级断点
- **深色/浅色双主题**：一键切换，CSS 变量驱动 + Element Plus dark class + ECharts 图表联动
- **粒子动画开关**：用户可控的 Canvas 粒子背景效果
- **全局 Loading**：Axios 请求拦截器自动管理（引用计数），支持 `showLoading` 和 `silent` 模式
- **统一错误提示**：业务错误通过 `ElMessage` / `ElNotification` 展示，服务端错误返回统一 `ApiResponse` 格式
- **表单实时校验**：前端 el-form 校验规则 + 后端 `@Valid` 注解校验，中文错误提示
- **键盘操作**：AI 对话框 Enter 发送、Shift+Enter 换行；对话框 ESC 关闭
- **KeepAlive 缓存**：`<keep-alive>` 包裹页面组件，避免切换时重复渲染
- **图片懒加载**：`v-lazy` 自定义指令（IntersectionObserver，rootMargin 100px），加载后 0.4s 渐显动画
- **页面过渡动画**：路由切换 `page-fade` 过渡 + 子页面 `fade-slide` 过渡
- **骨架屏**：ChartCard 支持 loading 骨架状态

### 4.4 兼容性

| 浏览器 | 最低版本 |
|--------|---------|
| Chrome | ≥ 90 |
| Firefox | ≥ 90 |
| Edge | ≥ 90 |
| Safari | ≥ 15 |

---

## 五、数据需求

### 5.1 核心实体

系统涉及 **8 个核心 JPA 实体**（对应 9 张数据库表，`course_knowledge_point` 为 `@ElementCollection` 映射表）：

| 实体 | 表名 | 主键策略 | 说明 |
|------|------|---------|------|
| User | `users` | BIGINT AUTO_INCREMENT | 用户账户与学习统计 |
| Course | `course` | VARCHAR(40) 手动赋值 | 课程信息 |
| CourseChapter | `course_chapter` | VARCHAR(50) 手动赋值 | 课程章节（@ManyToOne → Course） |
| - | `course_knowledge_point` | BIGINT AUTO_INCREMENT | 课程知识点（@ElementCollection） |
| StudyPlan | `study_plan` | BIGINT AUTO_INCREMENT | 学习计划 |
| Note | `notes` | BIGINT AUTO_INCREMENT | 笔记 |
| Resource | `resources` | BIGINT AUTO_INCREMENT | 学习资源 |
| Achievement | `achievements` | BIGINT AUTO_INCREMENT | 成就记录 |
| WrongQuestion | `wrong_questions` | BIGINT AUTO_INCREMENT | 错题 |

### 5.2 用户表数据字典

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录用户名 |
| password | VARCHAR(100) | NOT NULL | 登录密码（明文） |
| nickname | VARCHAR(50) | NOT NULL | 显示昵称 |
| avatar | VARCHAR(255) | NULLABLE | 头像 URL（注册时自动生成 DiceBear URL） |
| signature | VARCHAR(255) | NULLABLE | 个性签名 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'user' | 角色：admin / user |
| study_days | INT | NOT NULL, DEFAULT 0 | 累计学习天数 |
| total_hours | INT | NOT NULL, DEFAULT 0 | 累计学习小时数 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 注册时间 |

### 5.3 课程表数据字典

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(40) | PK | 课程编号（手动指定，如 `vue3-advanced`） |
| title | VARCHAR(120) | NOT NULL | 课程标题 |
| category | VARCHAR(40) | NOT NULL | 分类（前端/后端/AI/英语/数学） |
| cover | VARCHAR(255) | NULLABLE | 封面图片 URL |
| description | VARCHAR(500) | NOT NULL | 课程描述 |
| progress | INT | NOT NULL, DEFAULT 0 | 学习进度（0-100） |
| teacher | VARCHAR(50) | NOT NULL | 讲师姓名 |
| lessons | INT | NOT NULL | 总课时数 |

### 5.4 其他核心表数据字典（摘要）

**学习计划（study_plan）**：id, user_id(FK), title, content, deadline, priority(高/中/低), status(待开始/进行中/已完成), created_at

**笔记（notes）**：id, user_id(FK), title, content(TEXT), category(学习/思考/计划), created_at, updated_at

**错题（wrong_questions）**：id, user_id(FK), title, content(TEXT), answer(TEXT), analysis(TEXT), category(数学/英语/计算机), difficulty(简单/中等/困难), mastered, wrong_count, tags, created_at, updated_at

**资源（resources）**：id, user_id(FK), title, type(PDF/视频/图片/文档), category(课程资料/参考书籍/笔记/其他), size, url, description, download_count, created_at

**成就（achievements）**：id, user_id(FK), title, description, icon, unlocked, unlocked_at, progress, target, category, rarity(普通/稀有/珍贵/史诗/传说), points

> 完整 DDL 脚本见 `backend/src/main/resources/schema.sql`（注意：`spring.sql.init.mode=never`，实际由 JPA `ddl-auto: update` 自动建表，初始数据由 `DataInitializer.java` 写入）。

---

## 六、接口需求

### 6.1 统一响应格式

所有 API 响应采用统一 `ApiResponse<T>` 封装：

```json
{
  "code": 200,
  "message": "ok",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 200 | 请求成功 |
| 400 | 参数校验失败 |
| 401 | 未登录 / Token 无效 |
| 403 | 无权限（非管理员访问管理接口） |
| 404 | 资源不存在 |
| 409 | 数据冲突（如用户名重复） |
| 500 | 服务器内部错误 |

### 6.2 认证方式

所有受保护接口需在请求头携带：

```
Authorization: Bearer <jwt_token>
```

公开接口（无需认证）：
- `POST /api/user/login`
- `POST /api/user/register`

### 6.3 API 端点清单

系统共 **10 个 Controller**，**45 个 API 端点**：

#### 用户认证（UserController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/user/login` | 否 | 用户登录 |
| POST | `/api/user/register` | 否 | 用户注册 |
| POST | `/api/user/logout` | 是 | 退出登录（Token 加入黑名单） |
| GET | `/api/user/profile` | 是 | 获取当前用户资料 |
| PUT | `/api/user/profile` | 是 | 更新当前用户资料 |

#### 课程管理（CourseController）★

| 方法 | 路径 | 认证 | 权限 | 说明 |
|------|------|------|------|------|
| GET | `/api/course/list` | 是 | 所有用户 | 课程列表（可选 category、keyword 筛选） |
| GET | `/api/course/{id}` | 是 | 所有用户 | 课程详情（含章节列表） |
| POST | `/api/course` | 是 | **仅管理员** | 创建课程 |
| PUT | `/api/course/{id}` | 是 | **仅管理员** | 更新课程 |
| DELETE | `/api/course/{id}` | 是 | **仅管理员** | 删除课程 |

#### 学习计划（StudyPlanController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/study-plan/list` | 是 | 分页+筛选列表（page, pageSize, priority, status, keyword） |
| POST | `/api/study-plan` | 是 | 创建计划 |
| PUT | `/api/study-plan/{id}` | 是 | 更新计划 |
| DELETE | `/api/study-plan/{id}` | 是 | 删除计划 |

#### 笔记（NoteController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/note/list` | 是 | 笔记列表（category, keyword 筛选） |
| GET | `/api/note/detail/{id}` | 是 | 笔记详情 |
| POST | `/api/note/create` | 是 | 创建笔记 |
| PUT | `/api/note/update/{id}` | 是 | 更新笔记 |
| DELETE | `/api/note/delete/{id}` | 是 | 删除笔记 |
| GET | `/api/note/categories` | 是 | 获取预设分类列表 |

#### 错题本（WrongQuestionController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/wrongQuestion/list` | 是 | 错题列表（category, keyword, difficulty, status, page, pageSize） |
| GET | `/api/wrongQuestion/detail/{id}` | 是 | 错题详情 |
| POST | `/api/wrongQuestion/create` | 是 | 创建错题 |
| PUT | `/api/wrongQuestion/update/{id}` | 是 | 更新错题 |
| DELETE | `/api/wrongQuestion/delete/{id}` | 是 | 删除错题 |
| POST | `/api/wrongQuestion/master/{id}` | 是 | 标记为已掌握 |
| GET | `/api/wrongQuestion/categories` | 是 | 获取预设分类列表 |

#### 学习资源（ResourceController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/resource/list` | 是 | 资源列表（category, keyword, type, page, pageSize） |
| GET | `/api/resource/detail/{id}` | 是 | 资源详情 |
| POST | `/api/resource/upload` | 是 | 上传文件（multipart/form-data） |
| POST | `/api/resource/create` | 是 | 创建资源记录（无文件） |
| DELETE | `/api/resource/delete/{id}` | 是 | 删除资源 |
| GET | `/api/resource/download/{id}` | 是 | 下载文件（Blob 流） |
| GET | `/api/resource/categories` | 是 | 获取预设分类列表 |

#### 成就系统（AchievementController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/achievement/list` | 是 | 获取用户全部成就 |
| GET | `/api/achievement/stats` | 是 | 成就统计（积分、解锁数等） |
| POST | `/api/achievement/unlock/{id}` | 是 | 手动解锁成就 |
| POST | `/api/achievement/init` | 是 | 初始化/重置成就（?reset=true） |

#### AI 助手（AiController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/ai/chat` | 是 | AI 对话（含 RAG 增强） |
| GET | `/api/ai/quick-questions` | 是 | 获取预设快捷问题列表 |

#### 数据分析（AnalyticsController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/analytics/overview` | 是 | 学习数据总览 |
| GET | `/api/analytics/dashboard` | 是 | 仪表盘面板数据（支持 range: week/month/quarter） |
| GET | `/api/analytics/tasks` | 是 | 任务完成分布统计 |
| GET | `/api/analytics/resources` | 是 | 资源使用统计 |

#### 仪表盘（DashboardController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/dashboard/stats` | 是 | 首页关键指标（今日时长、任务数、完成率等） |

---

## 七、约束与假设

### 7.1 开发约束

| 约束项 | 说明 |
|--------|------|
| 技术栈 | Spring Boot 2.7.18 + Vue 3.5 + Spring Data JPA + MySQL 8.0 + Element Plus 2.14 |
| Java 版本 | JDK 17 |
| 构建工具 | Maven 3.9（后端）+ Vite 5.4（前端） |
| 部署形式 | 本地开发环境演示（Vite :5173 + Spring Boot :8080 + MySQL :3307） |
| 代码管理 | Git + Gitee |
| 数据库 | MySQL 8.0，JPA `ddl-auto: update` 自动管理表结构 |

### 7.2 假设条件

1. 用户具备基本计算机操作和 Web 浏览器使用经验
2. 开发环境网络可达 MySQL 8.0（端口 3307）和 DeepSeek API
3. AI 对话集成 DeepSeek API，需有效 API Key（`application.yml` 中 `app.deepseek.api-key`）
4. 课程数据由管理员统一维护，普通用户仅可浏览
5. 单用户使用场景，无并发冲突处理
6. 数据分析中趋势和热力图部分使用模拟数据生成（非真实埋点数据）
7. 密码以明文存储和比对（演示项目简化设计，生产环境应使用 BCrypt 加密）
8. RAG 知识库使用内存存储（`InMemoryEmbeddingStore`），服务重启后数据丢失

---

> **文档编制说明**：本需求规格说明书基于实际项目代码（前端 `src/` 12 个视图 + 后端 10 个 Controller / 12 个 Service / 8 个 JPA 实体）编写，所有需求条目与前端路由组件、后端 API 端点一一对应。初始种子数据定义见 `DataInitializer.java`（2 用户、5 课程含 52 章节、13 笔记、10 学习计划、9 错题、10 资源、13 成就）。完整数据库 DDL 见 `backend/src/main/resources/schema.sql`，前端组件接口说明见 `docs/COMPONENTS.md`。
