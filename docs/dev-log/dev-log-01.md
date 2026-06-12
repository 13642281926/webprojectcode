# 开发日志 01：项目初始化与环境搭建

> **日期**：2026-06-02
> **开发者**:何宇轩
> **阶段目标**：完成前后端工程初始化、数据库设计、基础架构搭建

---

## 完成内容

### 前端工程初始化

使用 `npm create vite@latest` 创建 Vue 3 项目，选择 JavaScript 模板。初始目录结构比较简单，只有基础的 `App.vue`、`main.js` 和 Vite 配置。

首先安装了核心依赖：
- Vue 3.5.34 + Vue Router 5.0.7 + Pinia 3.0.4
- Element Plus 2.14.0 + @element-plus/icons-vue 2.3.2
- ECharts 6.1.0 + Axios 1.16.1
- Sass 1.100.0 + lodash-es 4.18.1

配置 Vite 插件花费了不少时间。`unplugin-auto-import` 和 `unplugin-vue-components` 配合 Element Plus Resolver 实现了按需自动导入，不需要手动 `import { ElButton } from 'element-plus'`，直接在 template 里用 `<el-button>` 即可。配置 SCSS 的 `additionalData` 时踩了个坑——`@use` 语句必须在所有其他语句之前，所以不能和 `@import` 混用。

### 后端工程初始化

使用 Spring Initializr 创建 Spring Boot 2.7.18 项目，选了 web、validation、data-jpa、mysql-connector-j 四个 starter。Java 版本选 17，因为 Spring Boot 不再支持 Java 8。

在 `application.yml` 里配置了数据库连接。MySQL 用的 3307 端口（本机 3306 被另一个项目占用了），URL 里加了 `createDatabaseIfNotExist=true`，这样首次启动可以自动建库。JPA 用 `ddl-auto: update`，让 Hibernate 根据实体类自动管理表结构。

### 数据库设计

照着需求文档里的实体关系，建了 8 个 JPA 实体类（User、Course、CourseChapter、Note、Resource、StudyPlan、Achievement、WrongQuestion），对应 9 张表（Course 的 knowledgePoints 用 `@ElementCollection` 映射到单独的 `course_knowledge_point` 表）。

写 `schema.sql` 花了不少功夫。所有外键都加了 `ON DELETE CASCADE`，6 个 user_id 的外键列都建了索引。还写了一些初始 INSERT 语句来放演示数据。不过后来发现 `application.yml` 里设了 `spring.sql.init.mode: never`，所以 schema.sql 实际上不会自动执行——改由 DataInitializer 这个 CommandLineRunner 在代码里初始化数据。

### 基础布局搭建

前端搭了 `AppLayout.vue` 三栏布局（侧栏 + 顶栏 + 内容区），`AppSidebar.vue` 用 `getMenuItems()` 函数从路由配置自动生成菜单项。`AppHeader.vue` 放了主题切换按钮和用户头像。路由配置设了 `beforeEach` 守卫，没登录的用户自动跳到登录页。

---

## 技术难点

**SCSS 全局注入配置**：Vite 的 `css.preprocessorOptions.scss.additionalData` 会在每个 `.vue` 文件的 `<style>` 块前面自动注入指定的 SCSS 代码。我用 `@use` 引入了 `variables.scss` 和 `mixins.scss`，但 `@use` 有模块作用域限制——在一个文件里 `@use` 的变量，在另一个文件里要重新 `@use` 才能用。Vite 的注入机制恰好解决了这个痛点：每个组件都自动注入了 `@use`，所以所有组件都能直接用 `$color-accent`、`@include glass-card` 这些变量和混入。

---

## 个人反思

这周最大的收获是**从零开始搭全栈项目的脚手架真的比想象中费时间**。光是 Maven 依赖版本兼容性就查了半天——Spring Boot 2.7.x 用的是 `javax.persistence`（不是 `jakarta.persistence`），LangChain4j 0.36.2 需要 Java 17，JJWT 0.12.x 的 API 和旧版本 0.9.x 完全不同。建议以后开新项目先用 Spring Initializr 选好版本，不要手动拼依赖。

另外`DataInitializer` 的设计从一开始就考虑了幂等性（先 deleteAll 再 saveAll），后来每次重启应用都能保证数据一致性，这个决策很值得。
