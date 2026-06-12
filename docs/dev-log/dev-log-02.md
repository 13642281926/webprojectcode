# 开发日志 02：学习资源模块 + RAG 知识增强流程

> **日期**：2026-06-04
> **开发者**:何宇轩
> **阶段目标**：完成学习资源模块前后端开发 + RAG 知识库集成

---

## 完成内容

### 资源模块后端

`ResourceController` 设计了 7 个端点，最核心的是上传和下载。

上传流程：前端发 `multipart/form-data` → Controller 接收 `@RequestParam("file") MultipartFile` → Service 层做三件事：(1) 用 UUID 重命名文件，保留原始扩展名 → 存到 `./uploads/` 目录；(2) 根据扩展名自动判定类型（`.pdf`→PDF、`.zip`→ZIP、`.jpg/.png`→图片，等等），文件大小格式化为人类可读字符串（"2.5MB"、"15.8MB"）；(3) 写入 `resources` 表，download_count 初始为 0。

下载则反方向：Controller 返回 `ResponseEntity<Resource>`，设置 `Content-Disposition: attachment` 头，Service 用 `FileSystemResource` 读取文件流返回。每次下载，`download_count + 1`。

筛选列表用了 Stream API：先从 Repository 查出当前用户的所有 resource，然后用 `filter()` 链式过滤 category、keyword（LIKE 前缀匹配）、type，最后内存分页截取。没有用数据库分页，因为单用户数据量不大。

### 资源模块前端

`ResourceView.vue` 用 Element Plus 的 `el-upload` 组件实现文件上传，配置了 `drag` 属性支持拖拽。上传时自动带 `Authorization` header（axios 拦截器注入），不需要额外处理。

卡片式展示用 CSS Grid：`grid-template-columns: repeat(auto-fill, minmax(280px, 1fr))`，自适应列数。每个卡片根据 `type` 字段显示不同的 Element Plus 图标（`Document` 对 PDF、`FolderZip` 对 ZIP、`VideoCamera` 对视频）。

筛选区放了三个维度：分类下拉框 + 类型下拉框 + 关键词搜索框。搜索用了 `lodash-es/debounce(fn, 400)` 防抖，避免每次按键都发请求。

`ResourceStore` 管理资源列表状态，upload action 成功后直接 `unshift` 到数组头部，不用重新拉全量——这样上传完立刻就能在页面上看到。

### RAG 知识增强流程 ★

这是本周最核心的工作。整个 RAG 管道分五步：

**第一步：文档摄入**。用户上传文件后，`ResourceService.upload()` 在保存完文件记录后，调用 `ragService.ingestDocument(file, userId)`。RagService 用 Apache Tika（`langchain4j-document-parser-apache-tika`）解析文档内容——Tika 的好处是自动识别格式，PDF/Word/Text 都能处理。纯 PDF 的用 PDFBox 作补充。

**第二步：文本分块**。Tika 提取出来的长文本，用 `DocumentSplitter` 按 500 字符窗口 + 100 字符重叠切分成 `TextSegment` 列表。500 字符的窗口大小是一篇中等段落的大小，100 字符重叠确保上下文不会在分块边界处断裂。

**第三步：向量嵌入**。每个 TextSegment 通过 `EmbeddingModel` 转为向量。EmbeddingModel 的底层指向 DeepSeek 的 `text-embedding-v3` 模型——用的是 `OpenAiEmbeddingModel`（因为 DeepSeek API 兼容 OpenAI 格式），baseUrl 配成 `https://api.deepseek.com/v1`。

**第四步：向量存储**。嵌入结果存入 `InMemoryEmbeddingStore`，附带 `userId` 元数据。内存存储的优点是零配置、速度快；缺点是重启就没了——这个问题记在了后续优化清单里。

**第五步：语义检索**。用户提问时，RagService 先把问题也向量化，然后调用 `EmbeddingStore.search(questionEmbedding, maxResults, minScore)` 做相似度检索。阈值设了 0.7，太低会召回无关内容，太高可能漏掉相关段落。检索结果按 userId 过滤（保证只查到用户自己的文档），然后拼接成上下文文本，注入到 AI 的 system prompt 中。

---

## 技术难点

**Tika 解析中文 PDF**：部分中文字符在默认配置下会乱码。排查后发现需要在 Tika 的 `Parser` 配置里指定 `Content-Type` 和字符编码，或者在 PDFBox 的 `PDFParser` 里显式处理 CJK 字体。最终采用 PDFBox 直接解析 PDF（因为用户上传的多是中文技术文档），Tika 作为兜底处理其他格式。

**相似度阈值调参**：0.7 这个值试了好几次。0.5 会召回太多噪音（"Python"作为通用词在很多不相干的文档里出现），0.85 又太严格（"Vue3 组件通信"和"Vue 组件传参"明明语义相近却匹配不上）。最后折中选了 0.7。

---

## 个人反思

RAG 管道看起来简单——"上传→分块→嵌入→检索"，但每个环节都有坑。文档解析要考虑格式兼容性，分块要考虑上下文连贯性，嵌入要考虑模型选择，检索要考虑阈值调参。这周深刻体会到：**AI 工程的难点不在模型本身，而在数据管道的工程化**。

另外 `InMemoryEmbeddingStore` 虽然方便，但毕竟是玩具级别的方案。答辩时如果被问到"服务重启后 RAG 还能用吗"，得准备好回答持久化向量数据库的迁移方案。
