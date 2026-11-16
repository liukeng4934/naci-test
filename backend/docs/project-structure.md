# 后端项目结构

## 1. 技术方案

- 开发框架：Spring Boot 3.4.1
- SSM：Spring Framework + Spring MVC + MyBatis
- Java：21
- 数据库：MySQL 8.0+
- 数据迁移：Flyway
- 认证方式：Spring Security + JWT
- 构建方式：Maven

项目采用经典的三层架构：Controller 控制层、Service 业务层、Mapper 数据访问层；Entity、DTO、VO 统一放在 Model 模型层。用户认证、留言与回复模块均已实现 Controller、Service、Mapper 和 XML。

## 2. 总体目录

```text
backend/
├─ docs/
│  ├─ database-design.md              # 数据库设计
│  └─ project-structure.md            # 本文件
├─ scripts/                           # 演示数据：demo_seed.sql 与生成脚本 generate_seed_data.py
├─ src/
│  ├─ main/
│  │  ├─ java/com/naci/guestbook/
│  │  │  ├─ GuestbookApplication.java # Spring Boot 启动类
│  │  │  ├─ controller/               # 控制层
│  │  │  ├─ service/                  # 业务层接口
│  │  │  │  └─ impl/                  # 业务层实现
│  │  │  ├─ mapper/                   # MyBatis 数据访问接口
│  │  │  ├─ model/                    # 模型层
│  │  │  │  ├─ entity/                # 数据库实体
│  │  │  │  ├─ dto/                   # 请求参数
│  │  │  │  └─ vo/                    # 响应对象
│  │  │  ├─ task/                     # 后台任务（MessageCleanupTask 物理清理）
│  │  │  └─ common/                   # 公共基础设施
│  │  │     ├─ exception/             # 业务异常与全局异常处理
│  │  │     ├─ response/              # 统一响应与分页响应
│  │  │     └─ security/              # JWT 与安全配置
│  │  └─ resources/
│  │     ├─ mapper/                   # MyBatis XML
│  │     ├─ db/migration/             # Flyway 迁移脚本（V1~V2）
│  │     └─ application.yml           # 数据源、MyBatis、Flyway、JWT 配置
│  └─ test/
│     ├─ java/com/naci/guestbook/
│     │  ├─ controller/               # 控制层测试
│     │  ├─ service/                  # 业务层测试
│     │  ├─ mapper/                   # 数据访问测试
│     │  └─ common/                   # 公共组件测试
│     └─ resources/                   # 测试配置与测试数据
├─ README.md
└─ pom.xml
```

## 3. 各层职责

| 目录 | 职责 |
| --- | --- |
| `controller` | 接收 HTTP 请求、参数校验、读取登录用户、调用 Service、返回统一响应 |
| `service` | 定义业务能力，控制事务边界和业务流程 |
| `service/impl` | 实现业务规则、权限校验、查询编排和事务 |
| `mapper` | 定义 MyBatis 数据访问方法，不包含业务规则 |
| `model/entity` | 与数据库表对应的持久化对象 |
| `model/dto` | Controller 接收的请求参数对象 |
| `model/vo` | Controller 返回给前端的响应对象 |
| `common/exception` | `BusinessException`、`GlobalExceptionHandler` |
| `common/response` | `ApiResponse<T>`、`PageResponse<T>` |
| `common/security` | JWT 生成与解析、认证过滤器、Security 配置 |
| `resources/mapper` | MyBatis XML，与 `mapper` 接口一一对应 |

## 4. 依赖方向

```text
Controller
    -> Service
        -> Mapper
            -> MySQL

Controller -> DTO
Service    -> Entity / VO
Controller <- VO
```

- Controller 不允许直接调用 Mapper。
- Service 负责事务和业务规则，Mapper 只负责 SQL 数据访问。
- DTO 只用于接收请求，VO 只用于返回响应，Entity 不直接暴露给前端。
- 所有跨表事务、父回复归属校验和回复子树删除逻辑都放在 Service 层。

## 5. 请求链路

```text
HTTP Request
  -> Controller（DTO + 参数校验）
  -> Service（业务规则 + 事务）
  -> Mapper（MyBatis）
  -> MySQL
  -> Mapper
  -> Service（组装 VO）
  -> Controller
  -> ApiResponse / PageResponse
```

## 6. 业务类规划

| 层 | 认证 | 留言 | 回复 |
| --- | --- | --- | --- |
| Controller | `AuthController` | `MessageController` | `ReplyController` |
| Service | `AuthService` / `AuthServiceImpl` | `MessageService` / `MessageServiceImpl` | `ReplyService` / `ReplyServiceImpl` |
| Mapper | `UserMapper` | `MessageMapper` | `ReplyMapper` |
| Entity | `User` | `Message` | `Reply` |
| DTO | 注册、登录请求 | 发布留言、留言查询 | 发布回复、回复查询 |
| VO | 登录结果、当前用户 | 留言列表项、留言分页 | 回复节点、回复分页 |

## 7. 核心业务落点

- 注册、登录、退出和当前用户由 `AuthController`、`AuthService`、`UserMapper` 协作完成。
- 发布、查询、删除留言由 `MessageController`、`MessageService`、`MessageMapper` 完成。
- 发布回复、按父节点查询直接子回复、删除回复子树由 `ReplyController`、`ReplyService`、`ReplyMapper` 完成。
- 回复使用 `parent_id` 邻接表表达任意层级；查询时每次只取一层直接子节点，前端按 `parentReplyId` 组装树。
- 删除留言先写 `messages.deleted_at` 完成软删除并由后台 `MessageCleanupTask` 物理清理：先按 `message_id` 每 500 条一批、每批独立事务删除该留言的全部回复，再删除留言本体，`replies.message_id` 的 `ON DELETE CASCADE` 仅作残留数据兜底；删除回复时在 `ReplyService` 事务内按 `parent_id` 逐层收集后代后统一删除。
- 发布回复时，Service 必须校验父回复存在且与目标留言属于同一条留言。

## 8. 资源与测试

```text
src/main/resources/
├─ application.yml
├─ mapper/
│  ├─ user/UserMapper.xml
│  ├─ message/MessageMapper.xml
│  └─ reply/ReplyMapper.xml
└─ db/migration/
   ├─ V1__init_schema.sql
   └─ V2__add_message_soft_delete.sql
```

`GuestbookApplication` 使用 `@MapperScan("com.naci.guestbook.mapper")` 扫描 Mapper 接口。测试目录按 Controller、Service、Mapper 和公共组件分层，而不是按业务模块建立重复包。
