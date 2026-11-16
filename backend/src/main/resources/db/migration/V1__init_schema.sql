-- MySQL 8.0+
-- 留言墙核心表：用户、顶层留言、无限层级回复

SET NAMES utf8mb4;

CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` VARCHAR(32) NOT NULL COMMENT '登录名，唯一',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  `display_name` VARCHAR(32) NOT NULL COMMENT '展示名称',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '注册时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  CONSTRAINT `chk_users_username_not_blank`
    CHECK (CHAR_LENGTH(TRIM(`username`)) > 0),
  CONSTRAINT `chk_users_display_name_not_blank`
    CHECK (CHAR_LENGTH(TRIM(`display_name`)) > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户';

CREATE TABLE `messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '留言 ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发布者 ID',
  `user_name` VARCHAR(32) NOT NULL COMMENT '发布时的展示名称快照',
  `content` VARCHAR(500) NOT NULL COMMENT '留言内容',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数缓存',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发布时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_messages_created_at` (`created_at`, `id`),
  KEY `idx_messages_user_created_at` (`user_id`, `created_at`),
  KEY `idx_messages_like_created_at` (`like_count`, `created_at`),
  CONSTRAINT `fk_messages_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_messages_content_not_blank`
    CHECK (CHAR_LENGTH(TRIM(`content`)) BETWEEN 1 AND 500)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='顶层留言';

CREATE TABLE `replies` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '回复 ID',
  `message_id` BIGINT UNSIGNED NOT NULL COMMENT '所属顶层留言 ID',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '父回复 ID；NULL 表示直接回复留言',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '回复者 ID',
  `user_name` VARCHAR(32) NOT NULL COMMENT '回复时的展示名称快照',
  `content` VARCHAR(300) NOT NULL COMMENT '回复内容',
  `like_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数缓存',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '回复时间（UTC）',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_replies_message_created_at` (`message_id`, `created_at`, `id`),
  KEY `idx_replies_message_parent_created_at` (`message_id`, `parent_id`, `created_at`, `id`),
  KEY `idx_replies_user_created_at` (`user_id`, `created_at`),
  CONSTRAINT `fk_replies_message`
    FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_replies_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_replies_content_not_blank`
    CHECK (CHAR_LENGTH(TRIM(`content`)) BETWEEN 1 AND 300)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='留言回复；parent_id 邻接表，支持任意层级';

-- 注意：
-- 1. 回复树采用邻接表：仅用 parent_id 表达父子关系，不存储物化路径和层级。
-- 2. replies.parent_id 故意不建自引用外键，避免 MySQL 自引用级联最多 15 层的限制。
-- 3. 父回复的存在性和同 message_id 归属由写入事务校验并锁定；
--    回复写入与子树删除都应先锁定对应的 messages 行，避免并发删除产生孤儿回复。
-- 4. 删除回复子树时，在事务内按 parent_id 逐层收集后代（临时表或应用层 BFS），
--    不依赖固定长度的 path，也不受自引用外键级联 15 层限制。
-- 5. 删除顶层留言依赖 message_id 外键 ON DELETE CASCADE；只级联一层，不受上述限制。
