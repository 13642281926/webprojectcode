# 开发日志 03：错题本 + 笔记管理模块开发

> **日期**：2026-06-06
> **开发者**:何宇轩
> **阶段目标**：完成错题本和笔记管理模块的前后端全链路开发

---

## 完成内容

### 错题本模块

**后端设计**：`WrongQuestion` 实体有 14 个字段，核心字段包括 `mastered`（是否掌握）、`difficulty`（简单/中等/困难）、`wrongCount`（错误次数）、`tags`（逗号分隔标签字符串）。

`WrongQuestionController` 提供 7 个端点。`list` 接口的筛选参数最多——category、keyword、difficulty、status（mastered/not_mastered），加上分页 page 和 pageSize。Service 层用 Stream API 链式过滤，难度统一使用中文值（简单/中等/困难），和前端下拉选项保持一致。

掌握标记的接口设计为 `POST /api/wrongQuestion/master/{id}`，触发后 `mastered` 翻转为 true，`updatedAt` 自动更新。这里最初设计成 PUT 更新整个实体，后来觉得太重了——标记掌握只是一个布尔值翻转，没必要传整个 JSON body，就改成了更语义化的 POST + 路径参数。

**前端实现**：`WrongQuestionView.vue` 的列表项左侧有彩色边框——红色表示未掌握（`mastered=false`），黄色表示复习中（`wrongCount > 1`），绿色表示已掌握（`mastered=true`）。这个三色状态条是纯 CSS 实现：`border-left: 3px solid var(--状态颜色)` 绑定到模板里的动态 class。

顶部放了 3 张 StatCard：新题目数、复习中数、已掌握数。数值来自 `useWrongQuestionStore` 的计算属性，`newCount`、`reviewingCount`、`masteredCount` 分别对列表做 filter 计数。每次 CRUD 操作后自动刷新这些数字。

筛选区四个维度：分类（数学/英语/计算机）、难度（简单/中等/困难）、状态（未掌握/已掌握）、关键词搜索。筛选参数存储在 Pinia Store 的 query 对象中，watch query 的变化自动重新请求列表。

### 笔记管理模块

**后端设计**：`Note` 实体使用 MySQL `TEXT` 类型存内容，`category` 字段预设三个值（study/thought/plan）对应学习笔记/想法记录/计划备忘。`NoteService` 的 update 和 delete 操作前会校验所有权——比较 `note.userId` 和 `AuthContext.getCurrentUserId()`，不匹配就抛 403。

**前端实现**：`NoteView.vue` 用卡片网格展示笔记。每个卡片显示标题、分类标签、更新时间、内容前三行预览。内容预览用 CSS 实现：`display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;`，不需要截断文本存数据库。

新增和编辑使用同一个 `el-drawer` 抽屉组件，通过 `isEdit` ref 控制标题和提交逻辑。分类输入用了 `el-select` 的 `multiple` + `allow-create` 组合，既可以从预设选项里选，也可以动态输入新分类名。标签功能类似。

**AI 回复保存为笔记**：这是跨模块协作的关键功能。用户在 `AiAssistantView` 看到 AI 回复后，点击消息气泡下方的"保存为笔记"按钮 → `AIChatBox` 组件 `emit('saveAsNote', message)` → 父组件接收后调用 `useNoteStore.createNote({ title: message.content.slice(0, 50) + '...', content: message.content, category: 'study' })`。自动截取前 50 字符作标题是个偷懒但实用的做法——用户保存后再自己去改标题。

---

## 技术难点

**错题本状态流转**：最初设计是 `status` 枚举（new/reviewing/mastered），但后来在开发中意识到——"复习中"其实是从 `wrongCount > 1` 推导出来的，不需要单独存一个字段。最终表结构简化为 `mastered` 布尔 + `wrongCount` 整数，前端根据这两个字段计算出三种视觉状态。这个简化避免了状态不一致的 bug。

**笔记抽屉的内容回填**：编辑笔记时，el-drawer 里的表单需要回填已有数据。如果用 `v-model` 绑定 store 的 `currentNote`，关闭抽屉时修改不会自动撤销。解决方案是用 `reactive()` 创建一个本地副本 `formData`，打开编辑时 `Object.assign(formData, currentNote)`，提交成功后才更新 store。这样关闭抽屉（取消编辑）就是安全的——formData 是本地数据，不影响 store。

---

## 个人反思

这周最有意思的是跨模块协作的设计——AI 回复一键保存为笔记。表面上看只是一个按钮，但背后牵扯到 AIChatBox 的 emit、父组件的回调、NoteStore 的 createNote action、后端 API 调用、自动标题生成五个环节。之前没有在一开始就设计好这个交互，是做到 AI 助手模块时临时加的，导致传参格式前后不一致调了好几次。

教训：**跨模块交互应该在第一版需求评审时就画好数据流图**，而不是各做各的到联调时再对接。
