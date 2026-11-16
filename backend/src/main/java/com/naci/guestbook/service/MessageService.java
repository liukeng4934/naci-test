package com.naci.guestbook.service;

import com.naci.guestbook.model.dto.CreateMessageRequest;
import com.naci.guestbook.model.dto.MessageQuery;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.MessagePageVO;
import com.naci.guestbook.model.vo.MessageVO;

public interface MessageService {

    /**
     * 分页查询留言，并返回留言板汇总统计。
     *
     * @param query 分页、排序和搜索条件
     * @return 留言分页结果及汇总统计
     */
    MessagePageVO listMessages(MessageQuery query);

    /**
     * 发布留言。
     *
     * @param userId 当前用户 ID
     * @param userName 当前用户名
     * @param request 留言内容
     * @return 新创建的留言
     */
    MessageVO createMessage(Long userId, String userName, CreateMessageRequest request);

    /**
     * 删除当前用户发布的留言：先标记删除，留言及其全部回复由后台任务异步清理。
     *
     * @param messageId 留言 ID
     * @param userId 当前用户 ID
     * @return 被删除的留言 ID 及回复数量
     */
    DeleteResultVO deleteMessage(Long messageId, Long userId);
}
