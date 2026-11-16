package com.naci.guestbook.task;

import com.naci.guestbook.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 后台清理任务：把已软删除的留言及其回复从数据库中物理删除。
 *
 * <p>删除接口只负责标记 deleted_at，因此立即返回；物理清理放在本任务中执行，
 * 不再阻塞用户请求。清理单条留言时先按批删除其回复（每批一个独立小事务，
 * 避免大量回复的删除长时间持有行锁、撑大 undo log），回复清空后再删留言本体。
 * 任务按批查询、失败时记录日志并在下一轮自动重试，
 * 因此应用重启后未清理的数据仍会被继续处理。</p>
 */
@Slf4j
@Component
public class MessageCleanupTask {

    /** 每轮查询待清理留言的批大小。 */
    private static final int PURGE_BATCH_SIZE = 100;

    /** 单个事务内最多物理删除的回复条数，控制行锁持有时间和 undo log 大小。 */
    private static final int REPLY_PURGE_BATCH_SIZE = 500;

    /** 单轮任务最多清理的留言数量上限，避免积压过多时长时间占用调度线程。 */
    private static final int MAX_PURGE_PER_RUN = 1000;

    private final MessageMapper messageMapper;

    /** 编程式事务模板，用于把每批删除包成独立小事务，提交后立即释放行锁。 */
    private final TransactionTemplate transactionTemplate;

    public MessageCleanupTask(MessageMapper messageMapper, PlatformTransactionManager transactionManager) {
        this.messageMapper = messageMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 定期清理软删除留言，默认每 5 秒执行一次。
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void purgeSoftDeletedMessages() {
        int purged = 0;
        while (purged < MAX_PURGE_PER_RUN) {
            List<Long> messageIds = messageMapper.selectSoftDeletedIds(PURGE_BATCH_SIZE);
            if (messageIds.isEmpty()) {
                return;
            }

            int purgedInBatch = 0;
            for (Long messageId : messageIds) {
                try {
                    purgedInBatch += purgeMessage(messageId);
                } catch (RuntimeException exception) {
                    log.warn("物理删除留言失败，将在下次任务重试: messageId={}", messageId, exception);
                }
            }
            purged += purgedInBatch;

            // 整批都没删成功说明遇到持续失败，先结束本轮，避免死循环占满调度线程。
            if (purgedInBatch == 0) {
                return;
            }
        }
    }

    /**
     * 清理单条留言：回复按每批 {@link #REPLY_PURGE_BATCH_SIZE} 条删除，
     * 每次 transactionTemplate.execute 是一个独立事务，提交后立即释放该批行锁；
     * 回复删完后再删除留言本体。任何一步失败都会在下一轮任务中重试，操作幂等。
     */
    private int purgeMessage(Long messageId) {
        while (true) {
            Integer deleted = transactionTemplate.execute(
                    status -> messageMapper.deleteRepliesByMessageId(messageId, REPLY_PURGE_BATCH_SIZE)
            );
            if (deleted == null || deleted < REPLY_PURGE_BATCH_SIZE) {
                break;
            }
        }
        Integer purged = transactionTemplate.execute(status -> messageMapper.deleteSoftDeletedById(messageId));
        return purged == null ? 0 : purged;
    }
}
