package com.naci.guestbook.mapper;

import com.naci.guestbook.model.entity.Reply;
import com.naci.guestbook.model.entity.ReplyWithChildCount;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReplyMapper {

    /**
     * 查询留言 ID，用于确认留言是否存在。
     *
     * @param messageId 留言 ID
     * @return 留言 ID；留言不存在时返回 null
     */
    Long selectMessageId(@Param("messageId") Long messageId);

    /**
     * 查询留言 ID 并对留言行加锁，用于回复写入和删除时的并发控制。
     *
     * @param messageId 留言 ID
     * @return 留言 ID；留言不存在时返回 null
     */
    Long selectMessageIdForUpdate(@Param("messageId") Long messageId);

    /**
     * 按 ID 查询回复。
     *
     * @param replyId 回复 ID
     * @return 回复实体；不存在时返回 null
     */
    Reply selectById(@Param("replyId") Long replyId);

    /**
     * 按 ID 查询回复并加行锁。
     *
     * @param replyId 回复 ID
     * @return 回复实体；不存在时返回 null
     */
    Reply selectByIdForUpdate(@Param("replyId") Long replyId);

    /**
     * 统计指定父节点下的直接子回复数量。
     *
     * @param messageId 留言 ID
     * @param parentId 父回复 ID；为 null 时统计留言的一级回复
     * @return 直接子回复数量
     */
    long countDirectChildren(
            @Param("messageId") Long messageId,
            @Param("parentId") Long parentId
    );

    /**
     * 游标分页查询指定父节点下的直接子回复，并附带各自的直接子回复数量。
     *
     * @param messageId 留言 ID
     * @param parentId 父回复 ID；为 null 时查询一级回复
     * @param cursorCreatedAt 游标创建时间；与 cursorId 同时为 null 时查询第一页
     * @param cursorId 游标回复 ID；有值时只返回 (created_at, id) 严格小于游标的记录
     * @param limit 单页最大记录数，调用方通常多取 1 条用于判断是否还有下一页
     * @return 当前页直接子回复列表，按创建时间和主键倒序
     */
    List<ReplyWithChildCount> selectDirectChildren(
            @Param("messageId") Long messageId,
            @Param("parentId") Long parentId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    /**
     * 新增回复，并回填数据库生成的主键。
     *
     * @param reply 待新增回复
     * @return 受影响行数
     */
    int insert(Reply reply);

    /**
     * 批量查询一组父回复下的直接子回复 ID，供逐层收集待删除子树。
     *
     * @param messageId 留言 ID，用于限定查询范围
     * @param parentIds 父回复 ID 集合
     * @return 直接子回复 ID 列表
     */
    List<Long> selectChildIdsByParentIds(
            @Param("messageId") Long messageId,
            @Param("parentIds") Collection<Long> parentIds
    );

    /**
     * 批量物理删除指定留言下的回复。
     *
     * @param messageId 留言 ID，用于限定删除范围
     * @param ids 待清理回复 ID 集合
     * @return 实际删除行数
     */
    int deleteByMessageIdAndIds(
            @Param("messageId") Long messageId,
            @Param("ids") Collection<Long> ids
    );
}
