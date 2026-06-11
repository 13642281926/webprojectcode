# 组件化说明（考核点：props / emit / slot）

---

## 组件树

```
App.vue
└── <router-view>
    ├── LoginView                          # /login
    │   └── ParticleBackground
    │
    └── AppLayout                          # 系统主壳（provide themeConfig）
        ├── ParticleBackground
        ├── AppSidebar                     # 侧栏导航（component :is 动态图标）
        │   └── <router-link> × 12
        ├── AppHeader                      # 顶栏 + 主题/粒子切换
        │
        └── <router-view> → <transition> → <keep-alive> → <component :is>
            ├── DashboardView              # /dashboard
            │   ├── StatCard × 3
            │   ├── ChartCard × 2
            │   └── QuickEntry
            │
            ├── CourseView                 # /course（管理员可增删改）
            │   ├── CourseCard × N         # emit: click
            │   │   ├── LazyImage
            │   │   └── 管理按钮（编辑/删除）*admin only*
            │   ├── el-drawer（详情）
            │   │   └── el-timeline + el-progress
            │   └── el-dialog（添加/编辑表单）*admin only*
            │
            ├── AiAssistantView            # /ai-assistant
            │   └── AIChatBox              # emit: send/quick/saveAsNote
            │
            ├── NoteView                   # /note
            ├── WrongQuestionView          # /wrong-question
            ├── ResourceView               # /resource
            ├── AchievementView            # /achievement
            ├── PomodoroView               # /pomodoro
            ├── AnalyticsView              # /analytics
            │   ├── StatCard × 3
            │   └── ChartCard × 4
            │
            └── ProfileView                # /profile
                ├── StatCard × 3
                └── el-form（资料编辑）
```

---

## 考核技术点分布

| 考核项 | 实现位置 | 说明 |
|--------|---------|------|
| KeepAlive | `AppLayout.vue` | `<keep-alive>` 包裹 `<component :is>` |
| 自定义指令 v-lazy | `src/directives/lazy.js` | IntersectionObserver 懒加载 + 渐显 |
| provide/inject | `AppLayout` → `ChartCard` | provide `themeConfig` |
| 动态组件 | `AppSidebar.vue`、`QuickEntry.vue` | `<component :is>` 动态图标 |
| 角色权限 | `stores/user.js` + `AuthInterceptor.java` | isAdmin + AuthContext.requireAdmin() |
| 课程 CRUD | `CourseView.vue` + `CourseController.java` | 管理员增删改，普通用户 403 |
| 插槽（Slot） | `StatCard`（default/footer）、`ChartCard`（extra/default） | 具名插槽 + 默认插槽 |
| Lodash | `src/utils/debounce.js` | lodash-es debounce |
| 路由懒加载 | `router/routes.js` | 动态 import() + chunk 命名 |
| 导航守卫 | `router/index.js` | beforeEach 鉴权 |
| Pinia 持久化 | `stores/user.js`、`stores/theme.js` | localStorage 读写 |

---

## 通用组件 `components/common/`

### StatCard

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `label` | 指标名称（必填） |
| props | `value` / `unit` | 数值与单位 |
| props | `icon` / `color` | 图标与主题色 |
| props | `clickable` | 是否可点击 |
| emit | `click` | 卡片点击 |
| slot | `default` | 自定义数值区域 |
| slot | `footer` | 底部扩展 |

### ChartCard

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `title` / `height` / `option` | 标题、高度、ECharts 配置 |
| props | `loading` | 加载骨架 |
| emit | `ready` | 图表实例就绪 |
| slot | `extra` | 标题栏右侧 |
| slot | `default` | 图表下方扩展 |

### CourseCard

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `course` | 课程对象（必填） |
| emit | `click` | 点击卡片，参数为 course 对象 |

### AIChatBox

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `messages` | 消息列表 |
| props | `loading` | 加载状态 |
| emit | `send` | 发送问题 |
| emit | `quick` | 快捷问题点击 |

### LazyImage

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `src` | 图片 URL |
| props | `alt` | 替代文本（无障碍） |
| props | `fetchpriority` | 加载优先级（`low` / `high`） |

### ParticleBackground

| 类型 | 名称 | 说明 |
|------|------|------|
| props | `count` | 粒子数量（默认 60） |
| props | `color` | 粒子颜色（RGB 字符串） |
| props | `linkLines` | 是否启用连线 |
| props | `opacity` | 透明度（0-1） |

---

## 布局组件 `components/layout/`

| 组件 | 说明 | 通信方式 |
|------|------|----------|
| `AppLayout` | 系统主壳：粒子背景 + 侧栏 + 顶栏 + `<router-view>` | 读取 `themeStore` |
| `AppHeader` | 顶栏标题、主题/粒子开关、面包屑、用户头像 | 读取 stores + route |
| `AppSidebar` | 侧边菜单（`getMenuItems()` 配置驱动） | 无 props，配置驱动 |
