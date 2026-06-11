# Spring Boot Backend

AI 学习成长助手平台后端服务，提供 REST API、JWT 认证、DeepSeek AI 集成和 RAG 知识增强。

## Stack

- Spring Boot 2.7.18
- Spring Web (Tomcat)
- Spring Data JPA (Hibernate 5.6)
- MySQL 8.0
- Java 17
- JJWT 0.12.6 (JWT 认证)
- LangChain4j 0.36.2 (AI + RAG)
- DeepSeek API (deepseek-v4-pro)

## 配置

| 配置项 | 默认值 |
|--------|--------|
| 端口 | `8080` |
| 数据库 | `jdbc:mysql://localhost:3307/ai_learning` |
| 用户名 | `root` |
| 密码 | `123456` |
| JWT 有效期 | 1440 分钟 (24h) |
| 文件上传目录 | `./uploads` |

配置文件：`src/main/resources/application.yml`

## 启动

```bash
cd backend

# 1. 确保 MySQL 8.0 已启动
# 2. 修改 application.yml 中的数据库密码（如需）
# 3. 启动
mvn spring-boot:run

# 或打包运行
mvn package -DskipTests
java -jar target/backend-1.0.0.jar
```

首次启动 Hibernate 会自动建表（`ddl-auto: update`），演示数据需手动导入 `schema.sql`。

## 数据库初始化

```bash
# 创建数据库
mysql -u root -p -P 3307 -e "CREATE DATABASE IF NOT EXISTS ai_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入演示数据
mysql -u root -p -P 3307 ai_learning < src/main/resources/schema.sql
```

详细说明见 [`DATABASE_SETUP.md`](DATABASE_SETUP.md)。

## API 接口

### 认证
| 方法 | 路径 | 说明 | 需登录 |
|------|------|------|--------|
| POST | `/api/user/login` | 登录 | 否 |
| POST | `/api/user/register` | 注册 | 否 |
| GET | `/api/user/profile` | 获取个人信息 | 是 |
| PUT | `/api/user/profile` | 更新个人信息 | 是 |

### 课程
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/course/list` | 课程列表 | 所有用户 |
| GET | `/api/course/{id}` | 课程详情 | 所有用户 |
| POST | `/api/course` | 创建课程 | **仅管理员** |
| PUT | `/api/course/{id}` | 更新课程 | **仅管理员** |
| DELETE | `/api/course/{id}` | 删除课程 | **仅管理员** |

### AI
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 发送对话 |
| GET | `/api/ai/quick-questions` | 快捷问题列表 |

完整接口列表（47 个端点）见各 Controller。

## 角色权限

| 角色 | 说明 |
|------|------|
| `admin` | 管理员，可增删改课程 |
| `user` | 普通用户，仅可查看 |

JWT Token 包含 `role` 声明，通过 `AuthContext.requireAdmin()` 进行鉴权。

## 前端集成

前端 Vite 开发服务器代理 `/api` 到 `http://localhost:8080`。启动后端后运行：

```bash
cd ../
npm run dev
```

## 演示账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `123456` | admin |
