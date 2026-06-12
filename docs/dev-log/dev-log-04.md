# 开发日志 04：学习计划 + 状态管理 + 前后端联调

> **日期**：2026-06-07
> **开发者**:何宇轩
> **阶段目标**：完成学习计划模块、Pinia 状态管理优化、前后端全面联调

---

## 完成内容

### 学习计划模块

**后端**：`StudyPlan` 实体字段比较标准——title、content、deadline、priority（高/中/低）、status（pending/in_progress/done）。`StudyPlanController` 提供 4 个标准 CRUD 端点，list 接口支持分页（page/pageSize）和三维筛选（priority/status/keyword）。

筛选逻辑在 `StudyPlanService.list()` 中用 Stream API：查出用户所有计划 → 按 priority 过滤 → 按 status 过滤 → 按 keyword 过滤（title 或 content 包含关键词）→ 按 id 倒序排列 → 手动分页（skip + limit）。没有用数据库分页是因为 JPA 的 Specification 动态查询写起来太啰嗦，而单用户数据量最多几十条，内存过滤完全够用。

**前端**：`StudyPlanView.vue` 实现了表格/卡片双视图切换——用 `el-radio-group` 切换 `viewMode` ref（`'table' | 'card'`），表格模式用 `el-table` + `el-pagination`，卡片模式用 `el-card` 网格布局。

状态流转是最核心的交互：点击状态标签 → `useStudyPlanStore.toggleStatus(plan)` → 循环切换 `pending → in_progress → done → pending` → `PUT /api/study-plan/{id}` → 成功后更新本地 state。用 `el-tag` 渲染状态，三个状态分别用不同颜色（灰色/蓝色/绿色）。

表格模式下每行右侧有编辑和删除操作按钮。编辑打开 `el-dialog` 表单回填，标题和内容用 `el-input`，优先级和状态用 `el-select`，截止日期用 `el-date-picker`。删除前弹出 `ElMessageBox.confirm` 确认。

### Pinia 状态管理优化

这一周花了不少时间整理 8 个 Store 的代码规范：

1. **统一持久化方案**：userStore 和 themeStore 用 `watch` + `localStorage.setItem` 手动持久化（因为需要在恢复时做额外的初始化逻辑），其他 Store 用 `usePersist` 工具函数（封装了 `store.$subscribe` 自动写入）。

2. **计算属性规范化**：studyPlanStore 的 `pendingCount`/`doneCount`，wrongQuestionStore 的 `masteredCount`/`reviewingCount`/`newCount`，pomodoroStore 的 `todayMinutes`/`weeklyTrend` 等计算属性，统一命名为 `xxxCount` 或 `xxxStats`，保持风格一致。

3. **action 的 API 调用模式**：所有 CRUD action 采用统一的三段式——(1) 调 API → (2) 成功则更新本地 state → (3) 失败则 ElMessage 提示。有段时间一些 action 在 API 失败后也乐观更新了 state，导致数据和服务器不一致，后来全改了。

### 前后端联调

这周是联调最密集的阶段。主要修了几个问题：

- **JWT 过期后前端未跳转**：axios 响应拦截器里 `case 401` 的处理逻辑在初始版本有 bug——`router.push('/login')` 在多个请求并发 401 时被调了多次，导致路由重复跳转。修法是用一个 `isRefreshing` 标记防止重复处理。

- **跨域预检请求被拦截器拒绝**：浏览器在发 POST 之前会发 OPTIONS 预检请求，但这个请求不带 `Authorization` header，被 AuthInterceptor 拦截返回 401。解决是在 `AuthInterceptor.preHandle()` 中加判断：如果请求方法是 OPTIONS，直接 `return true` 放行。

- **日期字段前后端格式不一致**：后端 `LocalDate` 序列化成 `[2026,6,15]` 数组，前端 `el-date-picker` 期望 `"2026-06-15"` 字符串。在 `application.yml` 里加了 `spring.jackson.date-format: yyyy-MM-dd HH:mm:ss` 和 `time-zone: Asia/Shanghai` 解决。

---

## 技术难点

**双视图切换时的状态保持**：从表格视图切到卡片视图再切回来，筛选条件和分页位置要保留。解决方案是把 `query`（page/pageSize/priority/status/keyword）存在 Pinia Store 中，两个视图共享同一份 query 状态，切换视图时只改 `viewMode`，不重置 query。

**乐观更新 vs 悲观更新**：学习计划的状态切换（pending→in_progress→done）用了乐观更新——先改本地 state，再发 API 请求。API 失败时回滚。这样做体验好（切换瞬间完成，没有等待），但需要仔细处理回滚逻辑。最终决定：状态切换用乐观更新（高频操作、失败概率低），创建/删除用悲观更新（低频操作、失败后果严重）。

---

## 个人反思

联调阶段是 bug 集中爆发期。最大的教训是：**不要等所有模块都写完了再联调**。应该在每个 Controller 写完后就立刻和前端对接，用 Postman 验证接口正确性，用浏览器 DevTools 检查网络请求。到联调周才发现的问题，很多其实在第一周就能发现。

另外 Pinia 的命名规范应该在一开始就定好。8 个 Store 的 action 有的叫 `fetch`、有的叫 `load`、有的叫 `getList`，后来虽然整理了一轮，但还是在一些地方残留了不一致的命名。团队开发时应该先在 README 或 Wiki 里写一份 Store 编码规范。
