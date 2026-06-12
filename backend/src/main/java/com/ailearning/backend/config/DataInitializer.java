package com.ailearning.backend.config;

import com.ailearning.backend.entity.*;
import com.ailearning.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据初始化 —— admin(管理端) + zhangsan(用户端) 两大角色全覆盖。
 * <p>
 * 所有初始化操作为幂等：先清空旧数据再重建。
 * 难度统一使用中文值（简单/中等/困难）与前端筛选保持一致。
 * </p>
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDemoData(
            UserRepository userRepo,
            CourseRepository courseRepo,
            NoteRepository noteRepo,
            StudyPlanRepository planRepo,
            WrongQuestionRepository wrongRepo,
            ResourceRepository resourceRepo,
            AchievementRepository achievementRepo) {

        return args -> {
            System.out.println("[DataInit] 清理旧数据...");
            achievementRepo.deleteAll();
            wrongRepo.deleteAll();
            resourceRepo.deleteAll();
            planRepo.deleteAll();
            noteRepo.deleteAll();
            courseRepo.deleteAll();
            userRepo.deleteAll();

            System.out.println("[DataInit] 开始填充演示数据...");
            LocalDateTime now = LocalDateTime.now();

            // ================================================================
            // 1. 用户：admin(管理端) + zhangsan(用户端)
            // ================================================================
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setNickname("系统管理员");
            admin.setAvatar("https://api.dicebear.com/9.x/avataaars/svg?seed=admin");
            admin.setRole("admin");
            admin.setSignature("管理平台，服务每一位学习者");
            admin.setStudyDays(365);
            admin.setTotalHours(1800);
            admin = userRepo.save(admin);

            User zhangsan = new User();
            zhangsan.setUsername("zhangsan");
            zhangsan.setPassword("123456");
            zhangsan.setNickname("张三");
            zhangsan.setAvatar("https://api.dicebear.com/9.x/avataaars/svg?seed=zhangsan");
            zhangsan.setRole("user");
            zhangsan.setSignature("前端开发工程师，热爱 Vue 生态");
            zhangsan.setStudyDays(120);
            zhangsan.setTotalHours(580);
            zhangsan = userRepo.save(zhangsan);

            System.out.println("[DataInit] 已创建 2 个用户 (admin + zhangsan)");

            // ================================================================
            // 2. 课程（平台级，5门，52章节）
            // ================================================================
            List<Course> courses = new ArrayList<>();

            Course vue3 = new Course();
            vue3.setId("vue3-advanced");
            vue3.setTitle("Vue3 高级开发实战");
            vue3.setCategory("前端开发");
            vue3.setCover("https://picsum.photos/seed/vue3/400/240");
            vue3.setDescription("从 Composition API 到 Pinia 状态管理，深入组件设计与性能优化，全面掌握 Vue3 企业级开发技能");
            vue3.setProgress(17);   // 2/12 章节已完成
            vue3.setTeacher("张三");
            vue3.setLessons(12);
            vue3.setKnowledgePoints(new ArrayList<>(List.of(
                    "Composition API", "响应式原理", "Pinia 状态管理",
                    "Vue Router 4", "组件设计模式", "Vite 构建优化",
                    "TypeScript 集成", "单元测试")));
            List<CourseChapter> vue3Chs = new ArrayList<>();
            vue3Chs.add(new CourseChapter("vue3-ch01", "Vue3 响应式系统原理", "45:30", true, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch02", "Composition API 深入", "52:10", true, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch03", "setup 语法糖与 TypeScript", "38:20", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch04", "Pinia 状态管理实战", "55:00", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch05", "Vue Router 4 路由设计", "42:15", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch06", "组件设计模式与插槽", "48:40", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch07", "自定义指令与组合函数", "36:25", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch08", "Vite 构建与性能优化", "50:30", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch09", "SSR 服务端渲染入门", "58:10", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch10", "组件库封装与发布", "44:55", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch11", "E2E 测试与 Cypress", "40:20", false, vue3));
            vue3Chs.add(new CourseChapter("vue3-ch12", "企业级项目：后台管理系统", "90:00", false, vue3));
            vue3.setChapters(vue3Chs);
            courses.add(vue3);

            Course pythonMl = new Course();
            pythonMl.setId("python-ml");
            pythonMl.setTitle("Python 机器学习入门");
            pythonMl.setCategory("AI/机器学习");
            pythonMl.setCover("https://picsum.photos/seed/python/400/240");
            pythonMl.setDescription("从 NumPy/Pandas 数据处理到 Scikit-learn 模型训练，系统入门机器学习理论与实践");
            pythonMl.setProgress(20);  // 2/10 章节已完成
            pythonMl.setTeacher("吴恩达");
            pythonMl.setLessons(10);
            pythonMl.setKnowledgePoints(new ArrayList<>(List.of(
                    "NumPy 科学计算", "Pandas 数据分析", "Matplotlib 可视化",
                    "Scikit-learn", "线性回归", "决策树与随机森林",
                    "K-Means 聚类", "模型评估与调参")));
            List<CourseChapter> mlChs = new ArrayList<>();
            mlChs.add(new CourseChapter("ml-ch01", "机器学习概述与环境搭建", "30:20", true, pythonMl));
            mlChs.add(new CourseChapter("ml-ch02", "NumPy 科学计算基础", "48:10", true, pythonMl));
            mlChs.add(new CourseChapter("ml-ch03", "Pandas 数据分析实战", "55:30", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch04", "Matplotlib 数据可视化", "42:00", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch05", "线性回归：从理论到代码", "52:40", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch06", "决策树与随机森林", "50:15", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch07", "K-Means 聚类分析", "38:45", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch08", "特征工程与数据预处理", "44:20", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch09", "模型评估与超参数调优", "48:50", false, pythonMl));
            mlChs.add(new CourseChapter("ml-ch10", "综合项目：房价预测竞赛", "65:00", false, pythonMl));
            pythonMl.setChapters(mlChs);
            courses.add(pythonMl);

            Course javaSpring = new Course();
            javaSpring.setId("java-spring");
            javaSpring.setTitle("Java Spring Boot 微服务实战");
            javaSpring.setCategory("后端开发");
            javaSpring.setCover("https://picsum.photos/seed/spring/400/240");
            javaSpring.setDescription("从 Spring Boot 基础到微服务架构，涵盖 JPA、Security、Docker 容器化等核心技术");
            javaSpring.setProgress(20); // 2/10 章节已完成
            javaSpring.setTeacher("王五");
            javaSpring.setLessons(10);
            javaSpring.setKnowledgePoints(new ArrayList<>(List.of(
                    "Spring Boot 自动配置", "JPA 数据持久化", "RESTful API 设计",
                    "Spring Security", "微服务架构", "Docker 容器化",
                    "API 网关", "服务注册与发现")));
            List<CourseChapter> javaChs = new ArrayList<>();
            javaChs.add(new CourseChapter("java-ch01", "Spring Boot 快速上手", "40:00", true, javaSpring));
            javaChs.add(new CourseChapter("java-ch02", "JPA 与 MySQL 数据持久化", "52:30", true, javaSpring));
            javaChs.add(new CourseChapter("java-ch03", "RESTful API 设计规范", "45:20", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch04", "全局异常处理与校验", "35:50", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch05", "Spring Security 认证授权", "58:10", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch06", "JWT 令牌与拦截器", "42:40", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch07", "文件上传与静态资源", "30:15", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch08", "微服务拆分与通信", "55:00", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch09", "Docker 容器化部署", "48:30", false, javaSpring));
            javaChs.add(new CourseChapter("java-ch10", "综合实战：AI 学习平台后端", "70:00", false, javaSpring));
            javaSpring.setChapters(javaChs);
            courses.add(javaSpring);

            Course dsAlgo = new Course();
            dsAlgo.setId("ds-algo");
            dsAlgo.setTitle("数据结构与算法精讲");
            dsAlgo.setCategory("计算机基础");
            dsAlgo.setCover("https://picsum.photos/seed/algorithm/400/240");
            dsAlgo.setDescription("从数组链表到动态规划，结合 LeetCode 高频真题，系统提升算法思维与编码能力");
            dsAlgo.setProgress(25);    // 3/12 章节已完成
            dsAlgo.setTeacher("赵六");
            dsAlgo.setLessons(12);
            dsAlgo.setKnowledgePoints(new ArrayList<>(List.of(
                    "数组与链表", "栈与队列", "树与二叉树",
                    "哈希表", "DFS/BFS", "动态规划",
                    "贪心算法", "排序算法", "图论基础")));
            List<CourseChapter> algoChs = new ArrayList<>();
            algoChs.add(new CourseChapter("algo-ch01", "复杂度分析与编码规范", "32:10", true, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch02", "数组与链表高频题", "55:20", true, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch03", "栈与队列应用", "48:40", true, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch04", "二叉树遍历与构造", "52:30", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch05", "二叉搜索树与平衡树", "50:15", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch06", "哈希表与字符串", "45:00", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch07", "DFS 深度优先搜索", "48:25", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch08", "BFS 广度优先搜索", "42:50", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch09", "动态规划入门", "58:30", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch10", "动态规划进阶", "55:40", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch11", "贪心与排序算法", "46:10", false, dsAlgo));
            algoChs.add(new CourseChapter("algo-ch12", "综合面试题精讲", "62:00", false, dsAlgo));
            dsAlgo.setChapters(algoChs);
            courses.add(dsAlgo);

            Course engTech = new Course();
            engTech.setId("english-tech");
            engTech.setTitle("技术英语阅读与写作");
            engTech.setCategory("语言能力");
            engTech.setCover("https://picsum.photos/seed/english/400/240");
            engTech.setDescription("提升英文技术文档阅读能力，掌握 Stack Overflow 提问技巧和 GitHub 协作英语表达");
            engTech.setProgress(12);   // 1/8 章节已完成
            engTech.setTeacher("张三");
            engTech.setLessons(8);
            engTech.setKnowledgePoints(new ArrayList<>(List.of(
                    "技术词汇积累", "英文文档阅读", "API 文档理解",
                    "Stack Overflow 提问", "GitHub Issue 协作",
                    "技术博客写作", "英语口语表达")));
            List<CourseChapter> engChs = new ArrayList<>();
            engChs.add(new CourseChapter("eng-ch01", "高频技术词汇 200 词", "35:00", true, engTech));
            engChs.add(new CourseChapter("eng-ch02", "阅读英文官方文档技巧", "42:20", false, engTech));
            engChs.add(new CourseChapter("eng-ch03", "理解英文 API Reference", "38:50", false, engTech));
            engChs.add(new CourseChapter("eng-ch04", "Stack Overflow 高效提问", "30:15", false, engTech));
            engChs.add(new CourseChapter("eng-ch05", "GitHub Issue 协作英语", "28:40", false, engTech));
            engChs.add(new CourseChapter("eng-ch06", "技术博客英文写作", "45:30", false, engTech));
            engChs.add(new CourseChapter("eng-ch07", "技术演讲与英语表达", "40:00", false, engTech));
            engChs.add(new CourseChapter("eng-ch08", "英文面试模拟实战", "50:20", false, engTech));
            engTech.setChapters(engChs);
            courses.add(engTech);

            courseRepo.saveAll(courses);
            System.out.println("[DataInit] 已创建 5 门课程 + 52 个章节");

            // ================================================================
            // 3. 笔记 —— admin 5条 + zhangsan 8条 = 13条
            // ================================================================
            List<Note> notes = new ArrayList<>();

            // --- admin 的笔记 (5条) ---
            notes.add(buildNote(admin.getId(), "平台管理规范与操作手册",
                    "### 用户管理\n- 普通用户注册后默认 role=user\n- 管理员可查看全部用户数据\n\n### 课程管理\n- 管理员可创建/编辑/删除课程\n- 课程需包含完整的章节信息\n\n### 数据统计\n- 仪表盘实时展示平台核心指标\n- 分析页提供多维度数据图表",
                    "平台管理", now.minusDays(2), now.minusDays(2)));
            notes.add(buildNote(admin.getId(), "AI 学习平台架构设计笔记",
                    "### 技术栈\n- 后端：Spring Boot 2.7 + JPA + MySQL\n- 前端：Vue 3 + Vite + Element Plus\n- AI：DeepSeek API + RAG 知识库\n\n### 核心模块\n1. 用户认证 JWT\n2. 课程管理 CRUD\n3. 笔记/计划/错题/资源\n4. 番茄专注 + 成就系统\n5. AI 助手 + RAG 检索",
                    "平台管理", now.minusDays(1), now.minusDays(1)));
            notes.add(buildNote(admin.getId(), "数据库优化记录",
                    "### 慢查询优化\n- 为 study_plan.user_id 添加索引\n- notes 表全文搜索改用 LIKE 前缀匹配\n\n### 连接池配置\n- HikariCP maximumPoolSize: 20\n- connectionTimeout: 30000ms\n\n### 备份策略\n- 每日凌晨 2 点全量备份\n- 保留最近 7 天备份文件",
                    "平台管理", now.minusDays(5), now.minusDays(4)));
            notes.add(buildNote(admin.getId(), "Vue3 前端性能优化要点",
                    "### 组件懒加载\n- 路由级：defineAsyncComponent\n- 图表库按需引入（ECharts 仅加载 line/bar/pie）\n\n### 打包优化\n- manualChunks 拆分 vendor\n- gzip + brotli 双压缩\n- Tree Shaking 减少包体积\n\n### 运行时优化\n- v-memo 缓存静态列表\n- shallowRef 减少深层响应",
                    "前端开发", now.minusDays(3), now.minusDays(3)));
            notes.add(buildNote(admin.getId(), "安全加固检查清单",
                    "### 认证安全\n- JWT secret 使用环境变量，不硬编码\n- Token 过期时间 24h，支持 refresh\n\n### 接口安全\n- 所有 /api/** 通过 AuthInterceptor 校验\n- CORS 限制生产域名\n- SQL 注入：JPA 参数化查询\n\n### 数据安全\n- 用户密码 bcrypt 加密\n- 敏感日志脱敏\n- 定期安全审计",
                    "平台管理", now.minusDays(0), now.minusDays(0)));

            // --- zhangsan 的笔记 (8条) ---
            notes.add(buildNote(zhangsan.getId(), "Vue3 Composition API 学习笔记",
                    "### setup 函数\nsetup 是 Composition API 的入口，在 beforeCreate 之前执行。\n\n### ref vs reactive\n- ref：包装基本类型，需要 .value 访问\n- reactive：包装对象，直接访问属性\n\n### watch vs watchEffect\n- watch：明确指定依赖，惰性执行\n- watchEffect：自动追踪依赖，立即执行\n\n### computed\n返回一个只读的响应式引用，自动缓存计算结果。",
                    "前端开发", now.minusDays(5), now.minusDays(5)));
            notes.add(buildNote(zhangsan.getId(), "Pinia 状态管理踩坑记录",
                    "### 定义 Store\n使用 defineStore 创建，支持 Options API 和 Setup 两种风格。\n\n### 持久化插件\npinia-plugin-persistedstate 可将状态自动同步到 localStorage。\n注意事项：敏感数据不要持久化！\n\n### 模块化拆分\n按业务领域拆分为 user、course、note 等独立 store。",
                    "前端开发", now.minusDays(3), now.minusDays(3)));
            notes.add(buildNote(zhangsan.getId(), "TypeScript 泛型进阶",
                    "### 泛型约束\n使用 extends 关键字约束类型参数：T extends Lengthwise\n\n### 条件类型\nT extends U ? X : Y 实现类型级别的条件判断\n\n### 映射类型\nPartial<T>、Required<T>、Pick<T,K>、Omit<T,K> 等工具类型的实现原理。\n\n### 模板字面量类型\n结合 infer 实现字符串解析类型。",
                    "编程基础", now.minusDays(1), now.minusDays(1)));
            notes.add(buildNote(zhangsan.getId(), "CSS Grid 布局完全指南",
                    "### 容器属性\n- grid-template-columns/rows：定义轨道\n- gap：间距\n- grid-template-areas：命名区域\n\n### 子项属性\n- grid-column/row：跨轨放置\n- place-self：单个对齐\n\n### 实战技巧\n- auto-fill + minmax 实现自适应卡片网格\n- Grid + Flexbox 混合布局",
                    "前端开发", now.minusDays(8), now.minusDays(7)));
            notes.add(buildNote(zhangsan.getId(), "Vite 构建优化实践",
                    "### 开发体验\n- 秒级冷启动（ESBuild 预构建）\n- HMR 热更新（模块粒度）\n\n### 生产构建\n- Rollup 打包，支持 code splitting\n- manualChunks 拆分大型依赖\n- CSS code splitting\n\n### 插件生态\n- unplugin-auto-import 自动导入\n- unplugin-vue-components 按需加载",
                    "前端开发", now.minusDays(10), now.minusDays(9)));
            notes.add(buildNote(zhangsan.getId(), "JavaScript 异步编程总结",
                    "### Promise\n- 三种状态：pending / fulfilled / rejected\n- .then/.catch/.finally 链式调用\n- Promise.all / allSettled / race / any\n\n### async/await\n- async 函数返回 Promise\n- await 暂停执行直到 Promise 解决\n- try/catch 捕获错误\n- 注意：await 会阻塞后续代码",
                    "编程基础", now.minusDays(12), now.minusDays(11)));
            notes.add(buildNote(zhangsan.getId(), "Element Plus 组件定制技巧",
                    "### 主题定制\n- CSS 变量覆盖：--el-color-primary\n- SCSS 变量 + 按需引入\n\n### 常用组件\n- el-table：虚拟滚动、自定义列\n- el-form：动态校验规则\n- el-dialog：嵌套使用与拖拽\n\n### 封装经验\n- 二次封装统一 loading/empty/error 状态\n- slot 透传保持灵活性",
                    "前端开发", now.minusDays(15), now.minusDays(14)));
            notes.add(buildNote(zhangsan.getId(), "前端工程化体系梳理",
                    "### 代码规范\n- ESLint + Prettier 统一风格\n- Husky + lint-staged 提交检查\n\n### 测试体系\n- 单元测试：Vitest\n- 组件测试：Vue Test Utils\n- E2E：Cypress / Playwright\n\n### CI/CD\n- GitHub Actions 自动构建部署\n- 分支保护 + PR Review",
                    "前端开发", now.minusDays(20), now.minusDays(18)));

            noteRepo.saveAll(notes);
            System.out.println("[DataInit] 已创建 " + notes.size() + " 条笔记 (admin:5, zhangsan:8)");

            // ================================================================
            // 4. 学习计划 —— admin 4条 + zhangsan 6条 = 10条
            // ================================================================
            List<StudyPlan> plans = new ArrayList<>();

            plans.add(buildPlan(admin.getId(), "审核新增课程内容", "检查近期上传的课程章节质量，确保内容准确完整", LocalDate.now().plusDays(3), "high", "in_progress", LocalDate.now().minusDays(1)));
            plans.add(buildPlan(admin.getId(), "优化平台响应速度", "排查慢查询，优化首页加载性能，目标 < 1.5s", LocalDate.now().plusDays(14), "high", "pending", LocalDate.now().minusDays(2)));
            plans.add(buildPlan(admin.getId(), "编写开发者文档", "整理 API 接口文档和部署指南，方便后续维护", LocalDate.now().plusDays(30), "medium", "pending", LocalDate.now().minusDays(5)));
            plans.add(buildPlan(admin.getId(), "完成安全审计", "全面检查 JWT、CORS、SQL 注入等安全项", LocalDate.now().minusDays(1), "high", "done", LocalDate.now().minusDays(15)));

            plans.add(buildPlan(zhangsan.getId(), "完成 Vue3 课程剩余章节", "学完第3-12章，并完成每章课后练习项目", LocalDate.now().plusDays(14), "high", "in_progress", LocalDate.now().minusDays(7)));
            plans.add(buildPlan(zhangsan.getId(), "重构个人博客为 Vue3 + TS", "将旧项目从 Vue2 迁移到 Vue3 + TypeScript，替换 Vuex 为 Pinia", LocalDate.now().plusDays(30), "medium", "in_progress", LocalDate.now().minusDays(14)));
            plans.add(buildPlan(zhangsan.getId(), "读完《JavaScript 高级程序设计》第4版", "重点阅读第6-10章和第20-24章，做读书笔记", LocalDate.now().minusDays(3), "low", "done", LocalDate.now().minusDays(60)));
            plans.add(buildPlan(zhangsan.getId(), "搭建前端组件库项目", "基于 Vue3 + TS + Vite 封装 20+ 业务组件，发布到 npm", LocalDate.now().plusDays(45), "high", "pending", LocalDate.now().minusDays(3)));
            plans.add(buildPlan(zhangsan.getId(), "学习 Nuxt3 SSR 框架", "完成官方教程 + 实战一个博客项目，理解 SSR/SSG 原理", LocalDate.now().plusDays(60), "medium", "pending", LocalDate.now().minusDays(1)));
            plans.add(buildPlan(zhangsan.getId(), "刷完 LeetCode 前端专题 50 题", "数组/字符串/树/DP 各10题，总结 JS/TS 最优解法", LocalDate.now().plusDays(21), "high", "in_progress", LocalDate.now().minusDays(5)));

            planRepo.saveAll(plans);
            System.out.println("[DataInit] 已创建 " + plans.size() + " 条学习计划 (admin:4, zhangsan:6)");

            // ================================================================
            // 5. 错题本 —— admin 3条 + zhangsan 6条 = 9条  [难度: 简单/中等/困难]
            // ================================================================
            List<WrongQuestion> wrongs = new ArrayList<>();

            wrongs.add(buildWrong(admin.getId(), "JWT Token 过期后前端未自动跳转登录", "用户长时间不操作后 Token 过期，但页面仍停留在当前路由，API 返回 401 但前端未处理跳转", "1. axios 响应拦截器统一捕获 401 → 清除 token → router.push('/login')\n2. 路由守卫 beforeEach 检查 token 有效性\n3. 可选的 token 刷新机制（refresh token）", "前端 axios 拦截器的 handleUnauthorized 函数需在 401 时立即执行，确保 router 已初始化。", "平台管理", false, now.minusDays(5), now.minusDays(3), "中等", 2, "JWT,认证,前端"));
            wrongs.add(buildWrong(admin.getId(), "Spring Boot CORS 配置不生效", "跨域请求仍然被浏览器拦截，即使已配置 WebMvcConfigurer.addCorsMappings", "1. 检查是否同时使用了 @CrossOrigin 注解和全局 CORS 配置导致冲突\n2. 确认 allowedOriginPatterns 使用 \"*\" 而非 allowedOrigins(\"*\")\n3. Spring Security 需额外配置 .cors()", "allowedOrigins(\"*\") 与 allowCredentials(true) 不能同时使用，需改用 allowedOriginPatterns(\"*\")。", "平台管理", true, now.minusDays(15), now.minusDays(10), "简单", 3, "CORS,Spring,跨域"));
            wrongs.add(buildWrong(admin.getId(), "MySQL 8.x 认证插件兼容问题", "MySQL 5.6 客户端连接 8.x 服务端报错 caching_sha2_password", "1. ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';\n2. 或在连接串添加 defaultAuth=mysql_native_password\n3. 升级客户端到 8.x", "MySQL 8 默认使用 caching_sha2_password，旧客户端不支持。", "平台管理", false, now.minusDays(2), now.minusDays(2), "困难", 1, "MySQL,认证,兼容性"));

            wrongs.add(buildWrong(zhangsan.getId(), "Vue3 watch 监听 ref 对象不触发", "watch(() => state.count, ...) 无法监听到变化，但直接 watch(state, ...) 可以", "ref 包装的对象需要用 .value 访问内部值。watch 的第一个参数应返回具体值：watch(() => state.value.count, callback)。或使用 watchEffect 自动追踪依赖。", "watch 需要明确返回监听的 getter 函数，或直接传入 reactive 对象。ref 对象的 .value 在 template 中自动解包，但在 JS 中需手动访问。", "前端开发", false, now.minusDays(4), now.minusDays(4), "中等", 3, "Vue3,watch,响应式"));
            wrongs.add(buildWrong(zhangsan.getId(), "CSS Grid 子元素高度不一致", "grid 容器中两列高度不同，导致视觉效果差，底部不对齐", "设置 align-items: stretch（默认值）使子元素等高。若已设置固定高度，改用 grid-auto-rows: 1fr 统一行高。检查子元素是否设置了固定 height 覆盖了 stretch 行为。", "Grid 默认 stretch 会拉伸所有子元素到同一行高。但如果子元素内部有固定高度内容，需要额外处理。", "前端开发", true, now.minusDays(10), now.minusDays(8), "简单", 2, "CSS,Grid,布局"));
            wrongs.add(buildWrong(zhangsan.getId(), "Pinia store 在 setup 外使用报错", "在 axios 拦截器中直接 import 使用 useUserStore() 报错：getActivePinia was called with no active Pinia", "Pinia store 必须在 setup 函数或 pinia 实例激活后才能使用。解决方案：1. 将 store 实例作为参数传入拦截器 2. 在拦截器内动态 import router 一样的方式 import store", "Pinia 依赖 Vue 应用的 pinia 插件实例。在模块顶层调用 useXxxStore() 时 pinia 尚未安装，需延迟调用。", "前端开发", false, now.minusDays(6), now.minusDays(6), "困难", 4, "Pinia,Vue3,状态管理"));
            wrongs.add(buildWrong(zhangsan.getId(), "TypeScript 类型推断失败：ref 类型变为 Ref<UnwrapRef<T>>", "定义一个 ref<number>(0)，但在后续赋值时 ts 报类型不匹配", "ref 的类型会自动展开：const count = ref<number>(0) 即可。如果从接口获取数据且结构复杂，建议定义 interface 并传入 ref<IMyData>(initialValue)。", "ref 的类型推断使用 UnwrapRef 展开嵌套 ref，通常不需要特殊处理。复杂类型用 as 断言或显式泛型参数。", "编程基础", true, now.minusDays(12), now.minusDays(11), "中等", 2, "TypeScript,ref,类型推断"));
            wrongs.add(buildWrong(zhangsan.getId(), "Vite proxy 代理 WebSocket 连接失败", "Vite 配置了 proxy 但 HMR WebSocket 连接失败，控制台报 WebSocket connection error", "在 vite.config.js 的 server.proxy 中配置 ws: true。若后端也使用 WebSocket，需单独配置 proxy 路径转发。", "Vite 的 HMR WebSocket 默认连接 dev server 地址。使用 proxy 时需确保 ws 选项为 true。", "前端开发", false, now.minusDays(3), now.minusDays(3), "中等", 1, "Vite,WebSocket,代理"));
            wrongs.add(buildWrong(zhangsan.getId(), "el-table 虚拟滚动下固定列错位", "Element Plus el-table 开启虚拟滚动后，fixed 固定列与滚动内容不对齐", "1. 升级 Element Plus 到最新版本（2.4+ 修复了该问题）\n2. 临时方案：固定列不使用 fixed 属性，改用 CSS sticky 定位\n3. 减少表格列数或用分页替代虚拟滚动", "虚拟滚动导致 DOM 复用，固定列的 top 值计算可能不准确。优先尝试版本升级。", "前端开发", false, now.minusDays(1), now.minusDays(1), "简单", 1, "ElementPlus,el-table,虚拟滚动"));

            wrongRepo.saveAll(wrongs);
            System.out.println("[DataInit] 已创建 " + wrongs.size() + " 条错题 (admin:3, zhangsan:6)");

            // ================================================================
            // 6. 学习资源 —— admin 4条 + zhangsan 6条 = 10条
            // ================================================================
            List<Resource> resources = new ArrayList<>();

            resources.add(buildResource(admin.getId(), "Spring Boot 官方文档中文版", "PDF", "后端开发", "12MB", "https://spring-docs.example.com", "Spring Boot 2.7 官方参考文档中文翻译，含完整配置项说明", 256, now.minusDays(30)));
            resources.add(buildResource(admin.getId(), "MySQL 性能优化手册", "PDF", "后端开发", "3.5MB", "https://mysql-opt.example.com", "索引优化、SQL 调优、配置参数、慢查询分析等全面指南", 182, now.minusDays(15)));
            resources.add(buildResource(admin.getId(), "Vue3 服务端渲染最佳实践", "文章", "前端开发", "0.8MB", "https://vue-ssr.example.com", "Nuxt3 + Vue3 SSR 实战指南，涵盖 SEO 优化和性能调优", 98, now.minusDays(7)));
            resources.add(buildResource(admin.getId(), "Docker + K8s 部署手册", "PDF", "后端开发", "15MB", "https://docker-k8s.example.com", "从 Dockerfile 到 Kubernetes 集群管理的完整实践指南", 310, now.minusDays(3)));

            resources.add(buildResource(zhangsan.getId(), "Vue3 官方文档中文版 PDF", "PDF", "前端开发", "8.5MB", "https://vue3-docs.example.com", "Vue3 官方文档的中文翻译版本，包含完整的 API 参考和教程", 428, now.minusDays(30)));
            resources.add(buildResource(zhangsan.getId(), "Element Plus 组件库使用指南", "PDF", "前端开发", "4.2MB", "https://element-plus.example.com", "Element Plus 全部组件的 Demo 和主题定制说明，含暗黑模式配置", 286, now.minusDays(15)));
            resources.add(buildResource(zhangsan.getId(), "TypeScript 高级类型编程", "文章", "编程基础", "1.5MB", "https://ts-advanced.example.com", "深入条件类型、映射类型、模板字面量类型等 TS 高级特性", 156, now.minusDays(10)));
            resources.add(buildResource(zhangsan.getId(), "前端性能优化完全指南", "PDF", "前端开发", "6.8MB", "https://perf-guide.example.com", "涵盖 Core Web Vitals、打包优化、渲染性能、网络优化等全链路", 342, now.minusDays(5)));
            resources.add(buildResource(zhangsan.getId(), "CSS 动画与交互设计", "视频", "前端开发", "220MB", "https://css-anim.example.com", "CSS transition/animation + GSAP + Lottie 动画实战教程", 178, now.minusDays(20)));
            resources.add(buildResource(zhangsan.getId(), "LeetCode 前端高频 100 题精解", "PDF", "算法刷题", "3.2MB", "https://leetcode-fe.example.com", "按专题分类的 JS/TS 题解，含复杂度分析和多种解法对比", 512, now.minusDays(1)));

            resourceRepo.saveAll(resources);
            System.out.println("[DataInit] 已创建 " + resources.size() + " 条资源 (admin:4, zhangsan:6)");

            // ================================================================
            // 7. 成就 —— admin 5条 + zhangsan 8条 = 13条
            // ================================================================
            List<Achievement> achievements = new ArrayList<>();

            achievements.add(buildAchievement(admin.getId(), "创始管理员", "平台第一位管理员，见证平台从零到一", "Trophy", true, now.minusDays(365), 1, 1, "成长", "legendary", 500));
            achievements.add(buildAchievement(admin.getId(), "全能管理者", "掌握平台所有模块的管理能力", "Medal", true, now.minusDays(60), 5, 5, "成长", "epic", 300));
            achievements.add(buildAchievement(admin.getId(), "数据洞察者", "累计分析 100 条用户学习数据", "CircleCheck", false, null, 45, 100, "任务", "rare", 150));
            achievements.add(buildAchievement(admin.getId(), "平台守护者", "连续管理平台 365 天", "Trophy", true, now.minusDays(1), 365, 365, "成长", "legendary", 500));
            achievements.add(buildAchievement(admin.getId(), "内容贡献者", "创建 5 门优质课程", "Document", false, null, 5, 10, "创作", "epic", 250));

            achievements.add(buildAchievement(zhangsan.getId(), "初出茅庐", "注册并开始学习", "Timer", true, now.minusDays(120), 1, 1, "成长", "common", 10));
            achievements.add(buildAchievement(zhangsan.getId(), "笔记达人", "创建 10 条学习笔记", "Document", false, null, 8, 10, "学习", "uncommon", 30));
            achievements.add(buildAchievement(zhangsan.getId(), "连续学习 30 天", "连续登录学习 30 天", "Trophy", true, now.minusDays(30), 30, 30, "成长", "rare", 80));
            achievements.add(buildAchievement(zhangsan.getId(), "前端专家", "完成前端方向 5 门课程", "Medal", false, null, 2, 5, "学习", "epic", 200));
            achievements.add(buildAchievement(zhangsan.getId(), "算法新星", "刷完 50 道 LeetCode 题", "CircleCheck", false, null, 28, 50, "学习", "rare", 100));
            achievements.add(buildAchievement(zhangsan.getId(), "错题终结者", "掌握 10 道错题", "Warning", false, null, 3, 10, "学习", "rare", 80));
            achievements.add(buildAchievement(zhangsan.getId(), "资源收藏家", "上传 5 份学习资源", "Folder", true, now.minusDays(5), 6, 5, "创作", "uncommon", 40));
            achievements.add(buildAchievement(zhangsan.getId(), "学习达人", "累计学习 500 小时", "Medal", false, null, 120, 500, "成长", "legendary", 500));

            achievementRepo.saveAll(achievements);
            System.out.println("[DataInit] 已创建 " + achievements.size() + " 条成就 (admin:5, zhangsan:8)");

            // ================================================================
            // 完成
            // ================================================================
            System.out.println("[DataInit] ========== 演示数据初始化完成 ==========");
            System.out.println("[DataInit] 用户: admin/admin123 | zhangsan/123456");
            System.out.println("[DataInit] 课程:5 | 笔记:" + notes.size() + " | 计划:" + plans.size());
            System.out.println("[DataInit] 错题:" + wrongs.size() + " | 资源:" + resources.size() + " | 成就:" + achievements.size());
        };
    }

    // ==================== 辅助工厂方法 ====================

    private Note buildNote(Long userId, String title, String content, String category,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        Note n = new Note();
        n.setUserId(userId); n.setTitle(title); n.setContent(content);
        n.setCategory(category); n.setCreatedAt(createdAt); n.setUpdatedAt(updatedAt);
        return n;
    }

    private StudyPlan buildPlan(Long userId, String title, String content, LocalDate deadline,
                                String priority, String status, LocalDate createdAt) {
        StudyPlan p = new StudyPlan();
        p.setUserId(userId); p.setTitle(title); p.setContent(content);
        p.setDeadline(deadline); p.setPriority(priority);
        p.setStatus(status); p.setCreatedAt(createdAt);
        return p;
    }

    private WrongQuestion buildWrong(Long userId, String title, String content, String answer,
                                     String analysis, String category, boolean mastered,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     String difficulty, int wrongCount, String tags) {
        WrongQuestion w = new WrongQuestion();
        w.setUserId(userId); w.setTitle(title); w.setContent(content);
        w.setAnswer(answer); w.setAnalysis(analysis); w.setCategory(category);
        w.setMastered(mastered); w.setCreatedAt(createdAt); w.setUpdatedAt(updatedAt);
        w.setDifficulty(difficulty); w.setWrongCount(wrongCount); w.setTags(tags);
        return w;
    }

    private Resource buildResource(Long userId, String title, String type, String category,
                                   String size, String url, String description,
                                   int downloadCount, LocalDateTime createdAt) {
        Resource r = new Resource();
        r.setUserId(userId); r.setTitle(title); r.setType(type); r.setCategory(category);
        r.setSize(size); r.setUrl(url); r.setDescription(description);
        r.setDownloadCount(downloadCount); r.setCreatedAt(createdAt);
        return r;
    }

    private Achievement buildAchievement(Long userId, String title, String description,
                                         String icon, boolean unlocked, LocalDateTime unlockedAt,
                                         int progress, int target, String category,
                                         String rarity, int points) {
        Achievement a = new Achievement();
        a.setUserId(userId); a.setTitle(title); a.setDescription(description);
        a.setIcon(icon); a.setUnlocked(unlocked); a.setUnlockedAt(unlockedAt);
        a.setProgress(progress); a.setTarget(target); a.setCategory(category);
        a.setRarity(rarity); a.setPoints(points);
        return a;
    }
}
