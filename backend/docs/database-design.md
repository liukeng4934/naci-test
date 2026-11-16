# 留言墙后端数据库设计

## 1. 设计前提

- 数据库：MySQL 8.0+，存储引擎 InnoDB，字符集 `utf8mb4`。
- 主键：`BIGINT UNSIGNED` 自增；接口层统一序列化为字符串，避免 JavaScript 大整数精度丢失。
- 时间：数据库保存 UTC，字段使用 `DATETIME(3)`；接口返回 ISO 8601 字符串。
- 认证：简单注册/登录，密码使用 BCrypt 哈希；登录后使用 JWT，暂不引入服务端 Session 表。
- 迁移：使用 Flyway，脚本位于 `src/main/resources/db/migration/`（V1 建表，V2 留言软删除）。
- 回复树：采用邻接表（Adjacency List），只存 `parent_id`，不存物化路径和冗余层级。

## 2. 表关系

```text
users 1 ───── N messages
  │                 │
  │                 │ 1
  │                 ▼
  └────────────── N replies
                    │
                    └── parent_id 自关联，构成任意层级回复树
```

本次需求的核心表只有三张：

| 表 | 用途 |
| --- | --- |
| `users` | 用户账号、登录凭证、展示名称 |
| `messages` | 顶层留言，支持按创建时间排序 |
| `replies` | 留言回复，使用 `parent_id` 邻接表组成任意层级树 |

## 3. 表结构

### 3.1 `users`

| 字段 | 类型 | 约束/说明 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 主键，自增 |
| `username` | `VARCHAR(32)` | 登录名，唯一，非空 |
| `password_hash` | `VARCHAR(100)` | BCrypt 哈希，非空 |
| `display_name` | `VARCHAR(32)` | 展示名称，非空 |
| `created_at` | `DATETIME(3)` | 注册时间 |
| `updated_at` | `DATETIME(3)` | 更新时间 |

索引与约束：

- `UNIQUE KEY uk_users_username (username)`：登录查询、注册重名校验。
- 不保存明文密码；注册时校验用户名，登录时按 `username` 查询后校验 BCrypt。

### 3.2 `messages`

| 字段 | 类型 | 约束/说明 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 主键，自增 |
| `user_id` | `BIGINT UNSIGNED` | 发布者，外键到 `users.id` |
| `user_name` | `VARCHAR(32)` | 发布时的用户展示名快照 |
| `content` | `VARCHAR(500)` | 留言内容，非空 |
| `like_count` | `INT UNSIGNED` | 点赞数缓存，默认 0 |
| `created_at` | `DATETIME(3)` | 发布时间 |
| `updated_at` | `DATETIME(3)` | 更新时间 |
| `deleted_at` | `DATETIME(3) NULL` | 软删除时间；非空表示已删除，所有查询需过滤 |

索引与约束：

- `KEY idx_messages_created_at (created_at, id)`：支持 `ORDER BY created_at DESC, id DESC` 的时间排序和游标分页。
- `KEY idx_messages_user_created_at (user_id, created_at)`：查询某个用户的留言。
- `KEY idx_messages_like_created_at (like_count, created_at)`：兼容前端“最多回响”排序；如需求只保留时间排序可删除。
- `KEY idx_messages_deleted_at (deleted_at)`：支持后台任务扫描待物理清理的软删除留言。
- `user_id` 外键使用 `ON DELETE RESTRICT`，避免删除用户时误删历史留言。
- `user_name` 是发布时快照，避免列表查询额外关联用户表；若产品要求改名后历史留言同步显示新名字，则改为关联 `users.display_name`。

### 3.3 `replies`

| 字段 | 类型 | 约束/说明 |
| --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | 主键，自增 |
| `message_id` | `BIGINT UNSIGNED` | 所属顶层留言，非空 |
| `parent_id` | `BIGINT UNSIGNED NULL` | 父回复；`NULL` 表示直接回复留言，构成邻接表 |
| `user_id` | `BIGINT UNSIGNED` | 回复者，非空 |
| `user_name` | `VARCHAR(32)` | 回复时的用户展示名快照 |
| `content` | `VARCHAR(300)` | 回复内容，非空 |
| `like_count` | `INT UNSIGNED` | 点赞数缓存，默认 0 |
| `created_at` | `DATETIME(3)` | 回复时间 |
| `updated_at` | `DATETIME(3)` | 更新时间 |

索引与约束：

- `KEY idx_replies_message_created_at (message_id, created_at, id)`：按留言统计或导出全部回复，并保持稳定时间顺序。
- `KEY idx_replies_message_parent_created_at (message_id, parent_id, created_at, id)`：查询某条留言下某个父节点的直接子回复，并支持稳定时间顺序；根回复使用 `parent_id IS NULL`。
- `KEY idx_replies_user_created_at (user_id, created_at)`：查询某个用户的回复。
- `message_id` 外键使用 `ON DELETE CASCADE`：后台任务物理删除顶层留言时，数据库一次删除其全部回复。
- `parent_id` 不建立自引用外键。原因是 MySQL 的嵌套级联最多支持 15 层，而需求要求不设固定层级；父回复合法性由写入事务校验，子树删除由应用层按 `parent_id` 逐层收集后代完成。
- 回复树采用邻接表，不存储 `path` 和 `depth`；展示层级由前端按父子关系计算。

## 4. 回复树的实现

### 4.1 查询：按层加载，应用层组树

不要一次性递归查询整棵回复树，也不要为每个回复逐条查询子节点，否则会产生 N+1 查询。前端展开某个回复分支时，只查询该节点的直接子回复，并采用游标分页（keyset），避免 OFFSET 深翻页退化：

```sql
-- 查询某父节点下的直接子回复；根回复使用 parent_id IS NULL
SELECT r.id, r.message_id, r.parent_id, r.user_id, r.user_name,
       r.content, r.like_count, r.created_at,
       (SELECT COUNT(*)
          FROM replies child
         WHERE child.message_id = r.message_id
           AND child.parent_id = r.id) AS child_reply_count
FROM replies r
WHERE r.message_id = ?
  AND r.parent_id = ?            -- 根回复使用 parent_id IS NULL
  -- 翻页时追加游标条件，首页省略：
  AND (r.created_at, r.id) < (?, ?)
ORDER BY r.created_at DESC, r.id DESC
LIMIT ?;
```

- 游标格式为「`created_at` 毫秒数_回复 ID」，每次多取 1 条判断 `hasMore`，避免再执行一次 COUNT 或依赖总数推算。
- 行值比较 `(created_at, id) < (?, ?)` 直接沿 `idx_replies_message_parent_created_at` 索引定位，翻页代价与页深无关。
- 留言列表（顶层留言）不涉及层级，仍使用页码 OFFSET 分页，支持跳页。

响应保持扁平数组。前端先建立 `id -> 节点` 的 Map，再按 `parent_id` 建立子节点索引，整体复杂度为 O(n)。深层节点按需展开，不会一次性加载整棵树。

如果统计或导出场景确实需要一次取回某条留言的全部回复，才使用 `WHERE message_id = ?` 和 `idx_replies_message_created_at`；正常浏览不走这条路径。

### 4.2 创建回复

在一个事务中完成：

1. 校验 `message_id` 存在，并可锁定留言行，防止回复期间留言被删除。
2. 若 `parent_id` 非空，按主键查询父回复并锁定，确认父回复属于同一个 `message_id`。
3. 插入回复，`parent_id` 直接写入；直接回复留言时为 `NULL`。不需要回填路径或层级字段。
4. 提交事务。

只允许新增回复，不允许修改 `parent_id`，因此不会形成环。若以后增加“移动回复”功能，必须在事务中额外校验目标父节点不是自己的后代。

并发删除父节点时，删除事务也会锁定留言行；写入方等待后若发现父节点已不存在，应返回“父回复已删除”，不能继续插入。

### 4.3 删除

**删除顶层留言**

删除请求只做软删除，不等待回复删除：

```sql
-- 事务内先锁定留言并校验作者
SELECT id FROM messages WHERE id = ? AND deleted_at IS NULL FOR UPDATE;

-- 标记删除，列表、回复接口和统计立即排除该留言
UPDATE messages
SET deleted_at = UTC_TIMESTAMP(3)
WHERE id = ?
  AND user_id = ?
  AND deleted_at IS NULL;
```

后台任务（`MessageCleanupTask`）按批扫描 `deleted_at IS NOT NULL` 的留言，先分批删除该留言的全部回复，再删除留言本体：

```sql
-- 每批最多 500 条，每批一个独立事务，提交后立即释放行锁；
-- 返回删除行数不足 500 说明该留言的回复已清空
DELETE FROM replies
WHERE message_id = ?
  AND id IN (...最多 500 个...);

-- 回复清空后再删除留言本体
DELETE FROM messages WHERE id = ? AND deleted_at IS NOT NULL;
```

先删回复、后删留言本体，避免单个大事务长时间持有行锁、撑大 undo log；`replies.message_id` 的 `ON DELETE CASCADE` 仅作残留数据兜底，不作为主要清理路径。任务按批查询、失败记录日志并在下一轮重试，应用重启后未清理的数据仍会被继续处理。

**删除某条回复及其子树**

邻接表不能像物化路径那样用一条前缀 `DELETE` 删除整棵子树。实现在 `ReplyService` 的请求事务内完成：先按 `parent_id` 应用层 BFS 逐层收集后代 ID，再分批物理删除。

```sql
-- 1. 锁住留言，避免删除期间有新回复挂到被删子树下
SELECT id FROM messages WHERE id = ? AND deleted_at IS NULL FOR UPDATE;

-- 2. 锁定目标回复并校验作者后，按层收集后代（应用层循环，直到查不到新节点）
SELECT id FROM replies
WHERE message_id = ?
  AND parent_id IN (...上一层节点 ID，每批最多 500 个...);

-- 3. 分批物理删除整棵子树，避免单条 SQL 的 IN 参数过多
DELETE FROM replies
WHERE message_id = ?
  AND id IN (...每批最多 500 个...);
```

- BFS 用有序集合记录已收集节点，天然去重，不会因异常环死循环；正常情况下不会出现环。
- 收集与删除都按每批 500 条切分 IN 参数，子树很大也不会撑爆单条 SQL 或长时间持锁。
- `deletedReplyCount` 即收集到的子树节点数，在删除前即可确定并返回。

备选方案：MySQL 8 的递归 CTE 可以一次收集整棵子树，但 `cte_max_recursion_depth` 默认是 1000，需要显式调大；临时表 BFS 把循环下推到数据库，代价是临时表过大与事务持锁时间更长。应用层 BFS 不依赖这两个限制，且每批 SQL 结构恒定。

### 4.4 为什么采用邻接表

- 读取按层：每次只查直接子节点，命中 `idx_replies_message_parent_created_at`，不会为了展示一条留言而递归扫描整棵树。
- 写入 O(1)：新增回复只写 `parent_id`，不需要计算和维护整条路径。
- 没有固定深度上限：不受旧方案 `path VARCHAR(4096)` 或 `depth` 字段的限制；层级由前端递归渲染时计算。
- 删除子树 O(子树大小)：通过应用层 BFS 逐层收集后代；留言板场景下删除不是高频操作，这个代价可以接受。
- 避免自引用级联 15 层限制：`replies.parent_id` 不建自引用外键；`replies.message_id` 的级联只涉及一层，可以安全使用。
- 代价：子树查询/删除需要遍历，不能像物化路径那样用前缀索引一次定位。如果后续删除子树成为明显瓶颈，可以增加闭包表；不应重新把固定长度 `path` 作为唯一真相。

## 5. 关键 SQL 与索引验证

时间排序：

```sql
EXPLAIN SELECT id, user_id, user_name, content, created_at
FROM messages
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

应命中 `idx_messages_created_at`。

按留言取全部回复（统计、导出）：

```sql
EXPLAIN SELECT id, message_id, parent_id, user_id, user_name,
       content, like_count, created_at
FROM replies
WHERE message_id IN (1, 2, 3)
ORDER BY message_id, created_at, id;
```

应命中 `idx_replies_message_created_at`。

查询某个父节点的直接子回复：

```sql
EXPLAIN SELECT id, message_id, parent_id, user_id, user_name,
       content, like_count, created_at
FROM replies
WHERE message_id = 1
  AND parent_id = 2
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

应命中 `idx_replies_message_parent_created_at`。根回复查询只把 `parent_id = 2` 改为 `parent_id IS NULL`。

## 6. 可选扩展表

以下表不属于本次最低要求，但现有前端已有点赞入口，接入时可追加：

- `message_likes(message_id, user_id, created_at)`，主键 `(message_id, user_id)`。
- `reply_likes(reply_id, user_id, created_at)`，主键 `(reply_id, user_id)`。

如果登录态改为服务端 Session 而不是 JWT，再增加 `user_sessions(id, user_id, token_hash, expires_at, created_at)`；当前简单登录场景不需要。

## 7. 后端目录结构

```text
backend/
├─ docs/
│  ├─ database-design.md
│  └─ project-structure.md
├─ scripts/                  # 演示数据：demo_seed.sql 与生成脚本 generate_seed_data.py
├─ src/main/java/com/naci/guestbook/
│  ├─ GuestbookApplication.java
│  ├─ controller/           # 控制层：认证、留言、回复接口
│  ├─ service/              # 业务层接口
│  │  └─ impl/              # 业务层实现：事务、权限、回复树处理
│  ├─ mapper/               # 数据访问层：User/Message/Reply Mapper
│  ├─ model/                # 模型层
│  │  ├─ entity/            # 数据库实体
│  │  ├─ dto/               # 请求对象
│  │  └─ vo/                # 响应对象
│  ├─ task/                 # 后台任务：MessageCleanupTask 物理清理软删除留言
│  └─ common/               # 异常、统一响应、JWT、安全配置
└─ src/main/resources/
   ├─ application.yml
   ├─ mapper/               # MyBatis XML（user / message / reply 分包）
   └─ db/migration/
      ├─ V1__init_schema.sql
      └─ V2__add_message_soft_delete.sql
```
