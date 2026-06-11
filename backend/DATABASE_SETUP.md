# Database Setup Guide

## Prerequisites

- MySQL 8.0 已安装并运行
- root 用户密码已知

## 配置

`application.yml` 默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/ai_learning?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: 123456
```

如果密码或端口不同，修改 `application.yml` 中的对应字段。

## 自动初始化

项目使用 Hibernate `ddl-auto: update`，首次启动后端时**自动建表**。无需手动执行 DDL。

## 导入演示数据

启动后端后，手动导入 `schema.sql` 中的演示数据：

```bash
mysql -u root -p -P 3307 ai_learning < src/main/resources/schema.sql
```

演示数据包括：
- 1 个管理员用户（admin / 123456）
- 6 门示例课程 + 章节 + 知识点
- 3 条学习计划
- 3 条笔记
- 3 个学习资源
- 8 个成就
- 4 道错题

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `users` | 用户（含 role 角色字段） |
| `course` | 课程 |
| `course_chapter` | 课程章节 |
| `course_knowledge_point` | 课程知识点 |
| `study_plan` | 学习计划 |
| `resources` | 学习资源 |
| `notes` | 笔记 |
| `achievements` | 成就 |
| `wrong_questions` | 错题本 |

## 连接方式

**命令行：**
```bash
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p123456 -P 3307 ai_learning
```

**可视化工具（Navicat / MySQL Workbench）：**
- Host: `127.0.0.1`
- Port: `3307`
- User: `root`
- Password: `123456`
- Database: `ai_learning`

## 常见问题

| 问题 | 解决 |
|------|------|
| 连接被拒绝 | 确认 MySQL 8.0 服务已启动 |
| 找不到数据库 | 手动创建或确认 `createDatabaseIfNotExist=true` |
| 表验证失败 | 将 `ddl-auto: validate` 改为 `update` 或 `none` |
| 中文乱码 | 确认数据库字符集为 `utf8mb4` |
