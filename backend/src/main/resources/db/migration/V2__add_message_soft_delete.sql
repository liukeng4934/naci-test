-- 留言软删除支持：
-- DELETE /api/messages/{id} 只写入 deleted_at，立即对用户不可见；
-- 关联回复不在此同步删除，由后台任务物理删除留言时通过 message_id 外键级联清理。
ALTER TABLE `messages`
  ADD COLUMN `deleted_at` DATETIME(3) NULL DEFAULT NULL
    COMMENT '软删除时间（UTC）；NULL 表示未删除' AFTER `updated_at`,
  ADD KEY `idx_messages_deleted_at` (`deleted_at`);
