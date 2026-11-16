package com.naci.guestbook.service.impl;

import com.naci.guestbook.common.exception.BusinessException;
import com.naci.guestbook.common.exception.ErrorCode;
import com.naci.guestbook.mapper.MessageMapper;
import com.naci.guestbook.model.dto.CreateMessageRequest;
import com.naci.guestbook.model.dto.MessageQuery;
import com.naci.guestbook.model.entity.Message;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.MessagePageVO;
import com.naci.guestbook.model.vo.MessageSummaryVO;
import com.naci.guestbook.model.vo.MessageVO;
import com.naci.guestbook.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final MessageMapper messageMapper;

    /**
     * 分页查询留言，并返回留言板汇总统计。
     *
     * @param query 分页、排序和搜索条件；为 null 时使用默认查询条件
     * @return 留言分页结果及留言板汇总统计
     * @throws BusinessException 排序方式、页码、每页条数或搜索关键词不合法时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public MessagePageVO listMessages(MessageQuery query) {
        MessageQuery normalizedQuery = normalizeQuery(query);
        long total = messageMapper.countPage(normalizedQuery);
        long offset = (long) (normalizedQuery.getPage() - 1) * normalizedQuery.getPageSize();
        List<MessageVO> items = total == 0
                ? List.of()
                : messageMapper.selectPage(normalizedQuery, offset, normalizedQuery.getPageSize());

        MessageSummaryVO summary = messageMapper.selectSummary();
        if (summary == null) {
            summary = new MessageSummaryVO(0, 0, 0);
        }

        MessagePageVO result = new MessagePageVO();
        result.setItems(items == null ? List.of() : items);
        result.setPage(normalizedQuery.getPage());
        result.setPageSize(normalizedQuery.getPageSize());
        result.setTotal(total);
        result.setHasMore(offset + result.getItems().size() < total);
        result.setSummary(summary);
        return result;
    }

    /**
     * 校验登录用户和留言内容后发布留言。
     *
     * @param userId 当前用户 ID
     * @param userName 当前用户名
     * @param request 留言请求，包含留言内容
     * @return 新创建的留言
     * @throws BusinessException 用户未登录、用户名无效、内容不合法或写入失败时抛出
     */
    @Override
    @Transactional
    public MessageVO createMessage(Long userId, String userName, CreateMessageRequest request) {
        String normalizedUserName = requireUserName(userId, userName);
        String content = normalizeContent(
                request == null ? null : request.getContent(),
                "留言内容",
                MAX_MESSAGE_LENGTH
        );

        Message message = new Message();
        message.setUserId(userId);
        message.setUserName(normalizedUserName);
        message.setContent(content);
        int inserted = messageMapper.insert(message);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, "留言发布失败，请稍后重试");
        }

        return messageMapper.selectVOById(message.getId());
    }

    /**
     * 校验作者身份后标记删除留言；留言立即对用户不可见，回复和留言本体由后台任务异步清理。
     *
     * @param messageId 留言 ID
     * @param userId 当前用户 ID
     * @return 被删除的留言 ID 及关联回复数量
     * @throws BusinessException 参数不合法、留言不存在、无删除权限或并发删除冲突时抛出
     */
    @Override
    @Transactional
    public DeleteResultVO deleteMessage(Long messageId, Long userId) {
        if (messageId == null || messageId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "留言 ID 不合法");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        Message message = messageMapper.selectByIdForUpdate(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND, "这条留言不存在或已被删除");
        }
        if (!Objects.equals(message.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己发布的留言");
        }

        long deletedReplyCount = messageMapper.countRepliesByMessageId(messageId);
        int updated = messageMapper.softDeleteByIdAndUserId(messageId, userId);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION, "留言已被删除，请刷新后重试");
        }
        return new DeleteResultVO(messageId, deletedReplyCount);
    }

    /**
     * 规范化并校验留言查询条件。
     *
     * @param query 原始查询条件
     * @return 可直接用于数据访问的查询条件
     * @throws BusinessException 排序方式、页码、每页条数或搜索关键词不合法时抛出
     */
    private MessageQuery normalizeQuery(MessageQuery query) {
        MessageQuery normalized = query == null ? new MessageQuery() : query;
        if (normalized.getSort() == null || normalized.getSort().isBlank()) {
            normalized.setSort(MessageQuery.SORT_NEWEST);
        } else if (!MessageQuery.SORT_NEWEST.equals(normalized.getSort())
                && !MessageQuery.SORT_OLDEST.equals(normalized.getSort())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "排序方式不合法");
        }
        if (normalized.getPage() < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "页码必须大于 0");
        }
        if (normalized.getPageSize() < 1 || normalized.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每页条数需为 1-50");
        }
        normalized.setQ(normalized.getQ() == null ? "" : normalized.getQ().trim());
        if (normalized.getQ().length() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "搜索关键词不能超过 100 个字符");
        }
        return normalized;
    }

    /**
     * 校验当前用户信息并规范化用户名。
     *
     * @param userId 当前用户 ID
     * @param userName 当前用户名
     * @return 去除首尾空白后的用户名
     * @throws BusinessException 用户未登录、用户名为空或超过 32 个字符时抛出
     */
    private String requireUserName(Long userId, String userName) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (userName == null || userName.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态无效，请重新登录");
        }
        String normalized = userName.trim();
        return normalized;
    }

    /**
     * 去除文本首尾空白并校验非空及最大长度。
     *
     * @param content 原始文本
     * @param label 用于错误提示的字段名称
     * @param maxLength 允许的最大字符数
     * @return 规范化后的文本
     * @throws BusinessException 文本为空或超过最大长度时抛出
     */
    private String normalizeContent(String content, String label, int maxLength) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, label + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    label + "不能超过 " + maxLength + " 个字符"
            );
        }
        return normalized;
    }

}
