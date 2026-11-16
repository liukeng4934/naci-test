# Backend workspace

后端采用 Spring Boot 3.4.1 + SSM（Spring Framework、Spring MVC、MyBatis），使用 Java 21 和 Maven 构建。用户注册、登录、JWT 认证、当前用户和退出接口已完成；留言与回复模块的 Controller、Service、Mapper 也已实现。

- 项目结构：`docs/project-structure.md`
- 数据库设计：`docs/database-design.md`
- MySQL 8.0 初始化脚本：`src/main/resources/db/migration/V1__init_schema.sql`

## 模型层

- `model/entity`：`User`、`Message`、`Reply`
- `model/dto`：注册/登录、发布留言、发布回复、留言分页查询、回复分页查询
- `model/vo`：用户、认证结果、留言、回复、留言汇总、分页结果、删除结果
- 对外 VO 的 `Long` 类型 ID 使用 `ToStringSerializer` 输出为字符串，避免 JavaScript 大整数精度丢失。

## 用户认证

- `POST /api/auth/register`：注册，用户名唯一，密码使用 BCrypt 哈希后保存。
- `POST /api/auth/login`：登录并签发 JWT。
- `GET /api/auth/me`：根据 `Authorization: Bearer <token>` 返回当前用户。
- `POST /api/auth/logout`：退出登录；JWT 为无状态令牌，客户端需同时清除本地令牌。
- 注册、登录以及留言/回复的 GET 查询无需 JWT；发布留言、发布回复和删除接口要求有效 JWT。未认证返回 `401`，参数错误和业务冲突分别返回统一的 `{code,message}` 错误结构。

## 技术依赖

- `spring-boot-starter-web`：REST API
- `spring-boot-starter-validation`：请求参数校验
- `spring-boot-starter-security` + JJWT 0.12.6：JWT 认证
- `mybatis-spring-boot-starter` 3.0.4：MyBatis 与 Spring Boot 集成
- `mysql-connector-j`：MySQL 驱动
- `flyway-core`、`flyway-mysql`：数据库迁移
- Lombok、Spring Boot Test、Spring Security Test

## 启动

先创建数据库：

```sql
CREATE DATABASE naci_guestbook
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

默认连接 `127.0.0.1:3306/naci_guestbook`，用户名和密码为 `root/123456`。可以通过环境变量覆盖：

```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/naci_guestbook?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-password"
$env:JWT_SECRET = "replace-with-a-random-secret-at-least-32-bytes"
```

启动应用：

```powershell
cd backend
mvn spring-boot:run
```

构建并运行可执行 Jar：

```powershell
mvn -DskipTests package
java -jar target/guestbook-backend-0.0.1-SNAPSHOT.jar
```

启动时 Flyway 会自动执行 `classpath:db/migration` 下的迁移脚本。服务默认监听 `http://localhost:8080`。

代码采用经典 SSM 分层：`controller` 控制层、`service` 业务层、`mapper` 数据访问层、`model` 模型层，其中模型层包含 `entity`、`dto`、`vo`；JWT、统一响应、全局异常和通用配置放在 `common` 下。MyBatis XML 按数据对象放在 `src/main/resources/mapper`，启动类通过 `@MapperScan("com.naci.guestbook.mapper")` 扫描 Mapper 接口。

核心表为 `users`、`messages`、`replies`。回复树采用邻接表（Adjacency List）：仅用 `parent_id` 表达父子关系，不存储物化路径和冗余层级；按层查询，删除子树时由应用层逐层收集后代。

前端已按前后端分离方式预留 API 接口，接口约定见根目录 `README.md`。
