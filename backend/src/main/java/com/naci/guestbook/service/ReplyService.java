package com.naci.guestbook.service;

import com.naci.guestbook.model.dto.CreateReplyRequest;
import com.naci.guestbook.model.dto.ReplyQuery;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.ReplyPageVO;
import com.naci.guestbook.model.vo.ReplyVO;

public interface ReplyService {

    /**
     * 分页查询指定留言或指定父回复下的直接子回复。
     *
     * @param messageId 留言 ID
     * @param query 父回复 ID 和分页条件
     * @return 回复分页结果
     */
    ReplyPageVO listReplies(Long messageId, ReplyQuery query);

    /**
     * 发布回复，支持通过父回复 ID 创建任意层级的嵌套回复。
     *
     * @param messageId 留言 ID
     * @param currentUserId 当前用户 ID
     * @param currentUserName 当前用户名
     * @param request 回复内容和父回复 ID
     * @return 新创建的回复
     */
    ReplyVO createReply(
            Long messageId,
            Long currentUserId,
            String currentUserName,
            CreateReplyRequest request
    );

    /**
     * 在请求事务内物理删除当前用户发布的回复及其完整子树。
     *
     * @param replyId 回复 ID
     * @param currentUserId 当前用户 ID
     * @return 被删除的回复 ID 及实际删除的子树节点数量
     */
    DeleteResultVO deleteReply(Long replyId, Long currentUserId);
}
