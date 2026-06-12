# 开发日志 05：测试、性能优化与项目总结

> **日期**：2026-06-08
> **开发者**:何宇轩
> **阶段目标**：全链路功能验证、性能优化、文档完善、项目收尾

---

## 完成内容

### 全链路功能验证

逐模块走了一遍完整用户流程，记录并修复了以下问题：

1. **登录状态恢复**：在 `main.js` 里加了 `themeStore.loadFromStorage()` 确保刷新后主题设置不丢失。Pinia 的持久化用 `usePersist` 工具函数统一处理，但 themeStore 因为需要在 mount 前恢复（否则页面会闪一下白色），所以用 `watch` 手动同步。

2. **空状态处理**：多个列表页面在无数据时只显示空白。逐页加上 `el-empty` 组件，加上引导性提示文字（如"还没有学习计划，点击右上角创建第一个吧"）。

3. **AI 请求超时**：DeepSeek 的 `deepseek-v4-pro` 模型推理时间较长，复杂问题可能要 30-40 秒。把 axios timeout 设成 60000ms，前端加了 typing 动画和"AI 正在思考中..."的状态提示，避免用户以为卡住了。

4. **管理员权限前端联动**：CourseView 的添加/编辑/删除按钮根据 `useUserStore.isAdmin` 条件渲染。非管理员用户看不到这些按钮，但如果他们直接调 API，后端 `AuthContext.requireAdmin()` 会返回 403。双重保障。

### 性能优化

**前端构建优化**：
- `vite.config.js` 的 `build.rollupOptions.output.manualChunks` 手动拆了三个独立 chunk：echarts（~350KB）、element-plus（~500KB）、vue-vendor（vue + vue-router + pinia，~150KB）。配合浏览器缓存，第二次访问时这些 vendor chunk 直接走缓存。
- `vite-plugin-compression` 同时生成 `.gz` 和 `.br` 预压缩文件。阈值设 1KB（小于 1KB 的文件压缩反而更大）。Brotli 比 Gzip 压缩率高约 15-20%，但需要现代浏览器和服务器支持。
- ECharts 按需引入从 `echarts-init.js` 统一入口，仅注册 line/bar/pie/heatmap 四种图表 + Grid/Tooltip/Legend/Title/Calendar/Toolbox 组件。全量引入约 1MB，按需后 ~350KB。

**运行时优化**：
- 路由懒加载：12 个视图全部用 `() => import(/* webpackChunkName */ ...)` 动态导入。
- `<keep-alive>` 缓存 Dashboard、StudyPlan、Course 三个最常切换的页面。
- 搜索防抖统一使用 `lodash-es/debounce(fn, 400)`。
- 图片懒加载：`v-lazy` 自定义指令基于 `IntersectionObserver`，`rootMargin: 100px` 让图片在进入视口前 100px 就开始加载。

### 文档完善

本周花了不少时间整理了四份核心文档：

- **需求规格说明书 v2.1**：12 个功能模块逐项编号（REQ-xxx-xxx），补充了 45 个 API 端点的完整清单
- **系统设计说明书 v2.1**：完整的系统架构图、前端组件树（含 props/emit/slot 标注）、后端分层架构、数据库 DDL、AI/RAG 集成设计
- **README.md**：从零重构，加入 Mermaid 架构图、ER 图、功能详解、贡献说明
- **开发日志（5 篇）**：覆盖从环境搭建到项目收尾的完整开发过程

---

## 技术难点

**ECharts 主题联动**：深色/浅色切换时，所有图表需要重新渲染以适配新主题。最初的做法是 watch `isDark` 然后调 `chart.setOption()` 更新配置。但 `setOption` 只更新数据，不会改底色和轴线颜色。正确的做法是 `chart.dispose()` 销毁实例 → 用新主题 `init()` → 重新 `setOption()`。`ChartCard.vue` 通过 `inject('themeConfig')` 监听变化，在 `nextTick` 里执行销毁重建。

**并发 401 处理**：当 Token 过期时，页面上可能同时有几个 API 请求（比如 Dashboard 一次拉 6 个接口），每个请求的响应拦截器都会触发 `router.push('/login')`。导致路由重复跳转、控制台报 `NavigationDuplicated` 错误。解决是用 `isRedirecting` 标记：第一个 401 触发跳转 + 设置标记，后续 401 发现标记已设置则直接跳过。

---

## 个人反思

回头看了这四周的开发过程，几个关键体会：

**关于技术选型**：Spring Boot 2.7 + Vue 3 的组合确实很适合课程项目。Spring Boot 的自动配置和 starter 生态让后端开发效率很高，Vue 3 + Vite 的开发体验（秒级冷启动、HMR 热更新）比之前用 Vue 2 + Webpack 舒服太多了。唯一遗憾的是 `InMemoryEmbeddingStore` 的局限性——演示环境能用，但如果是真实产品，必须换持久化向量数据库。

**关于开发节奏**：第一周搭脚手架、第二三周并行开发模块、第四周联调修 bug、最后周优化写文档——这个节奏虽然紧但可行。最大的风险点在第三周：模块并行开发时各人（或者说各模块）之间的接口约定不够明确，联调时多花了时间对接。

**关于文档**：以前做项目总是先写代码最后补文档，这次尝试了"边写代码边记录关键设计决策"的方式。虽然多花了一些时间，但在写设计文档和 README 时省了很多回忆的功夫。而且写开发日志的过程中发现了几个之前没注意到的设计不一致——比如状态命名的中英文混杂（pending/in_progress/done vs 待开始/进行中/已完成）。**文档不只是给别人看的，更是整理自己思路的工具。**

---

## 项目总结

四周时间完成了 AI 学习成长助手平台的完整开发，包括：
- **前端**：12 个视图页面、11 个组件、8 个 Pinia Store、10 个 API 模块
- **后端**：10 个 Controller（45 个端点）、12 个 Service、7 个 Repository、8 个 JPA 实体
- **AI**：DeepSeek 对话 + LangChain4j RAG 管道（文档摄入→分块→嵌入→检索→增强）
- **工程化**：路由懒加载、ECharts 按需引入、Gzip/Brotli 预压缩、JWT 认证、角色权限
- **文档**：需求规格说明书、系统设计说明书、组件接口说明、5 篇开发日志

项目完整演示地址：`http://localhost:5173`（需同时启动 MySQL :3307 + Spring Boot :8080）
