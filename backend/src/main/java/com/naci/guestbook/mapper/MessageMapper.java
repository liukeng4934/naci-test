package com.naci.guestbook.mapper;

import com.naci.guestbook.model.dto.MessageQuery;
import com.naci.guestbook.model.entity.Message;
import com.naci.guestbook.model.vo.MessageSummaryVO;
import com.naci.guestbook.model.vo.MessageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper {

    /**
     * 统计符合关键字查询条件的留言总数。
     *
     * @param query 留言查询条件
     * @return 留言总数
     */
    long countPage(@Param("query") MessageQuery query);

    /**
     * 分页查询留言列表，并按创建时间排序。
     *
     * @param query 留言查询条件，包含排序方式和搜索关键字
     * @param offset 结果起始偏移量
     * @param limit 单页最大记录数
     * @return 当前页留言列表
     */
    List<MessageVO> selectPage(@Param("query") MessageQuery query,
                               @Param("offset") long offset,
                               @Param("limit") int limit);

    /**
     * 按 ID 查询留言视图，包括回复数量。
     *
     * @param id 留言 ID
     * @return 留言视图；不存在时返回 null
     */
    MessageVO selectVOById(@Param("id") Long id);

    /**
     * 按 ID 查询留言并加行锁，用于删除等需要并发保护的场景。
     *
     * @param id 留言 ID
     * @return 留言实体；不存在时返回 null
     */
    Message selectByIdForUpdate(@Param("id") Long id);

    /**
     * 新增留言，并回填数据库生成的主键。
     *
     * @param message 待新增留言
     * @return 受影响行数
     */
    int insert(Message message);

    /**
     * 统计指定留言下的全部未删除回复数量。
     *
     * @param messageId 留言 ID
     * @return 该留言的未删除回复总数
     */
    long countRepliesByMessageId(@Param("messageId") Long messageId);

    /**
     * 按留言 ID 和作者 ID 软删除留言，避免删除他人内容。
     *
     * @param id 留言 ID
     * @param userId 留言作者 ID
     * @return 受影响行数，成功标记删除时为 1
     */
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询待物理清理的软删除留言 ID。
     *
     * @param limit 单次最多返回的留言数量
     * @return 按删除时间升序排列的留言 ID 列表
     */
    List<Long> selectSoftDeletedIds(@Param("limit") int limit);

    /**
     * 按留言 ID 分批物理删除回复，每批最多 limit 条。
     * 每次调用是一个独立小事务，避免一次性级联删除长时间持有大量行锁。
     *
     * @param messageId 留言 ID
     * @param limit 单批最多删除的回复数量
     * @return 本批实际删除的回复行数；返回 0 表示该留言下已无回复
     */
    int deleteRepliesByMessageId(@Param("messageId") Long messageId, @Param("limit") int limit);

    /**
     * 物理删除已软删除的留言。回复应由调用方先批删完毕，
     * 残留的 message_id 外键 ON DELETE CASCADE 仅作为兜底。
     *
     * @param id 留言 ID
     * @return 受影响行数，成功删除时为 1
     */
    int deleteSoftDeletedById(@Param("id") Long id);

    /**
     * 查询留言板汇总统计，包括留言数、回复数和今日留言数。
     *
     * @return 留言汇总统计；无数据时可能返回 null
     */
    MessageSummaryVO selectSummary();
}
