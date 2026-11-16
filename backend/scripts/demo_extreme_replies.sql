-- 演示数据：两个回复性能极端示例，插入后排在留言列表最前面（最新）
-- 注意：demo_seed.sql 已内置同样的极端数据（含另外 4 组），新用户直接导入种子文件即可；
-- 本脚本仅用于在已有环境中单独重灌这两个示例（会先按内容删除旧示例再重插，留言 ID 会变化）。
--   1.「1000层的嵌套回复」：一条回复链嵌套 1000 层（parent_id 逐层指向上一条）
--   2.「1000回复的一层」：同一层（parent_id 均为 NULL）挂 1000 条直接回复
-- 可重复执行：先删除旧示例留言（回复经 message_id 外键级联删除），再重新插入。
-- 前置条件：Flyway 已建表（后端至少成功启动过一次）。
-- 若库中无用户，自动创建演示账号 user001（密码 123456）作为作者。
SET NAMES utf8mb4;
START TRANSACTION;

-- 清理旧示例，保证可重复执行
DELETE FROM messages WHERE content IN ('1000层的嵌套回复', '1000回复的一层');

-- 确保至少存在一个用户（无用户时创建演示账号，BCrypt 对应密码 123456）
INSERT INTO users (username, password_hash, display_name)
SELECT 'user001', '$2a$10$5NHdkXBkPuf/gN7LXxUD.eMGmhkKtClSkmdqqRzXgroVvYaKrMrOu', '金若溪'
WHERE NOT EXISTS (SELECT 1 FROM users);

SET @u1 = (SELECT MIN(id) FROM users);
SET @n1 = (SELECT display_name FROM users WHERE id = @u1);
SET @u2 = COALESCE((SELECT MIN(id) FROM users WHERE id > @u1), @u1);
SET @n2 = (SELECT display_name FROM users WHERE id = @u2);

-- 示例一：1000 层嵌套回复链（存储过程循环逐层插入，parent_id 指向上一条）
INSERT INTO messages (user_id, user_name, content)
VALUES (@u1, @n1, '1000层的嵌套回复');
SET @mA = LAST_INSERT_ID();

DELIMITER $$

CREATE PROCEDURE tmp_build_reply_chain()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE prev BIGINT;
  INSERT INTO replies (message_id, parent_id, user_id, user_name, content)
  VALUES (@mA, NULL, @u1, @n1, '第 1 层');
  SET prev = LAST_INSERT_ID();
  WHILE i < 1000 DO
    INSERT INTO replies (message_id, parent_id, user_id, user_name, content)
    VALUES (@mA, prev, @u1, @n1, CONCAT('第 ', i + 1, ' 层'));
    SET prev = LAST_INSERT_ID();
    SET i = i + 1;
  END WHILE;
END$$

DELIMITER ;

CALL tmp_build_reply_chain();
DROP PROCEDURE tmp_build_reply_chain;

-- 示例二：一层 1000 条直接回复（parent_id 全为 NULL）
INSERT INTO messages (user_id, user_name, content)
VALUES (@u2, @n2, '1000回复的一层');
SET @mB = LAST_INSERT_ID();

INSERT INTO replies (message_id, parent_id, user_id, user_name, content)
WITH RECURSIVE seq(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT @mB, NULL, @u2, @n2, CONCAT('回复 ', n) FROM seq;

COMMIT;

-- 结果验证：两条留言应排在最新列表前两位，各自挂 1000 条回复
SELECT id, user_name, content, created_at
FROM messages
ORDER BY created_at DESC, id DESC
LIMIT 3;

SELECT message_id, COUNT(*) AS reply_count
FROM replies
WHERE message_id IN (@mA, @mB)
GROUP BY message_id;
