package com.naci.guestbook.service.impl;

import com.naci.guestbook.common.exception.BusinessException;
import com.naci.guestbook.common.exception.ErrorCode;
import com.naci.guestbook.mapper.ReplyMapper;
import com.naci.guestbook.model.dto.CreateReplyRequest;
import com.naci.guestbook.model.dto.ReplyQuery;
import com.naci.guestbook.model.entity.Reply;
import com.naci.guestbook.model.entity.ReplyWithChildCount;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.ReplyPageVO;
import com.naci.guestbook.model.vo.ReplyVO;
import com.naci.guestbook.service.ReplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReplyServiceImpl implements ReplyService {

    /** 回复内容最大长度（字符数），超出则拒绝发布。 */
    private static final int MAX_CONTENT_LENGTH = 300;

    /** 回复中展示的用户名最大长度（字符数），超出则拒绝发布。 */
    private static final int MAX_USER_NAME_LENGTH = 32;

    /** 单条 SQL 中 IN 条件的最大参数个数，删除子树时按此大小分批执行。 */
    private static final int SQL_IN_BATCH_SIZE = 500;

    private final ReplyMapper replyMapper;

    /**
     * 创建回复业务服务。
     *
     * @param replyMapper 回复数据访问对象
     */
    public ReplyServiceImpl(ReplyMapper replyMapper) {
        this.replyMapper = replyMapper;
    }

    /**
     * 游标分页查询指定留言或指定父回复下的直接子回复。
     *
     * @param messageId 留言 ID
     * @param query 父回复 ID、游标和每页条数；为 null 时查询一级回复第一页
     * @return 回复分页结果，每条回复附带直接子回复数量
     * @throws BusinessException 留言 ID、父回复归属、游标或每页条数不合法，或留言、父回复不存在时抛出
     */
    @Override
    @Transactional(readOnly = true)
    public ReplyPageVO listReplies(Long messageId, ReplyQuery query) {
        requireMessageId(messageId);
        ReplyQuery safeQuery = normalizeQuery(query);
        Long parentId = resolveParentId(safeQuery);
        CursorPosition cursor = resolveCursor(safeQuery.getCursor());

        if (replyMapper.selectMessageId(messageId) == null) {
            throw messageNotFound();
        }
        if (parentId != null) {
            requireParentInMessage(parentId, messageId, false);
        }

        long total = replyMapper.countDirectChildren(messageId, parentId);
        // 多取 1 条判断是否还有下一页，避免再执行一次 COUNT 或依赖总数推算。
        List<ReplyWithChildCount> rows = replyMapper.selectDirectChildren(
                messageId,
                parentId,
                cursor == null ? null : cursor.createdAt(),
                cursor == null ? null : cursor.id(),
                safeQuery.getPageSize() + 1
        );

        boolean hasMore = rows.size() > safeQuery.getPageSize();
        List<ReplyWithChildCount> pageRows = hasMore
                ? rows.subList(0, safeQuery.getPageSize())
                : rows;

        List<ReplyVO> items = pageRows.stream()
                .map(this::toReplyVO)
                .toList();
        ReplyPageVO page = new ReplyPageVO();
        page.setItems(items);
        page.setPageSize(safeQuery.getPageSize());
        page.setTotal(total);
        page.setHasMore(hasMore);
        page.setNextCursor(hasMore ? encodeCursor(pageRows.get(pageRows.size() - 1)) : null);
        return page;
    }

    /**
     * 校验留言、用户、父回复和内容后创建回复，从而实现任意层级嵌套。
     *
     * @param messageId 留言 ID
     * @param currentUserId 当前用户 ID
     * @param currentUserName 当前用户名
     * @param request 回复内容和父回复 ID；父回复 ID 为 null 时创建一级回复
     * @return 新创建的回复
     * @throws BusinessException 用户未登录、留言或父回复不存在、父回复不属于当前留言或内容不合法时抛出
     */
    @Override
    @Transactional
    public ReplyVO createReply(
            Long messageId,
            Long currentUserId,
            String currentUserName,
            CreateReplyRequest request
    ) {
        requireMessageId(messageId);
        requireCurrentUser(currentUserId);
        if (request == null) {
            throw validationError("回复内容不能为空");
        }
        String content = normalizeContent(request.getContent());
        String userName = normalizeUserName(currentUserName);

        requireMessageForUpdate(messageId);
        if (request.getParentReplyId() != null) {
            requireParentInMessage(request.getParentReplyId(), messageId, true);
        }

        Reply reply = new Reply();
        reply.setMessageId(messageId);
        reply.setParentId(request.getParentReplyId());
        reply.setUserId(currentUserId);
        reply.setUserName(userName);
        reply.setContent(content);
        replyMapper.insert(reply);

        Reply saved = replyMapper.selectById(reply.getId());
        if (saved == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "回复发布失败，请稍后重试");
        }
        return toReplyVO(saved, 0);
    }

    /**
     * 物理删除当前用户发布的回复及其全部后代回复。
     *
     * <p>在同一事务内按层收集完整子树并分批物理删除，
     * 避免只删除根节点造成残留数据和统计短暂不一致。</p>
     *
     * @param replyId 回复 ID
     * @param currentUserId 当前用户 ID
     * @return 被删除的回复 ID 及实际删除的子树节点数量
     * @throws BusinessException 参数不合法、回复不存在或当前用户不是回复作者时抛出
     */
    @Override
    @Transactional
    public DeleteResultVO deleteReply(Long replyId, Long currentUserId) {
        if (replyId == null || replyId <= 0) {
            throw validationError("回复 ID 不合法");
        }
        requireCurrentUser(currentUserId);

        Reply snapshot = replyMapper.selectById(replyId);
        if (snapshot == null) {
            throw replyNotFound();
        }

        requireMessageForUpdate(snapshot.getMessageId());
        Reply target = replyMapper.selectByIdForUpdate(replyId);
        if (target == null) {
            throw replyNotFound();
        }
        if (!currentUserId.equals(target.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己发布的回复");
        }

        List<Long> subtreeIds = collectSubtreeIds(target.getMessageId(), replyId);
        long deletedCount = deleteInBatches(target.getMessageId(), subtreeIds);
        return new DeleteResultVO(replyId, deletedCount);
    }

    /**
     * 校验留言 ID 为正数。
     *
     * @param messageId 留言 ID
     * @throws BusinessException 留言 ID 为空或非正数时抛出
     */
    private void requireMessageId(Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw validationError("留言 ID 不合法");
        }
    }

    /**
     * 校验当前用户已登录。
     *
     * @param currentUserId 当前用户 ID
     * @throws BusinessException 用户 ID 为空时抛出
     */
    private void requireCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录或登录已过期");
        }
    }

    /**
     * 确认留言存在并对留言行加锁，保证回复写入或删除期间不会与留言删除并发冲突。
     *
     * @param messageId 留言 ID
     * @throws BusinessException 留言不存在时抛出
     */
    private void requireMessageForUpdate(Long messageId) {
        if (replyMapper.selectMessageIdForUpdate(messageId) == null) {
            throw messageNotFound();
        }
    }

    /**
     * 校验父回复存在且属于指定留言。
     *
     * @param parentId 父回复 ID
     * @param messageId 留言 ID
     * @param forUpdate 是否在查询时加行锁
     * @throws BusinessException 父回复不存在或不属于指定留言时抛出
     */
    private void requireParentInMessage(Long parentId, Long messageId, boolean forUpdate) {
        Reply parent = forUpdate
                ? replyMapper.selectByIdForUpdate(parentId)
                : replyMapper.selectById(parentId);
        if (parent == null || !messageId.equals(parent.getMessageId())) {
            throw replyNotFound();
        }
    }

    /**
     * 将查询参数中的父回复 ID 字符串解析为长整数。
     *
     * @param query 回复查询条件
     * @return 父回复 ID；未指定时返回 null
     * @throws BusinessException 父回复 ID 不是合法数字时抛出
     */
    private Long resolveParentId(ReplyQuery query) {
        try {
            return query.resolveParentReplyId();
        } catch (NumberFormatException exception) {
            throw validationError("parentReplyId 必须是 null 或数字 ID");
        }
    }

    /**
     * 规范化并校验回复查询条件。
     *
     * @param query 原始查询条件
     * @return 可直接用于数据访问的查询条件
     * @throws BusinessException 游标或每页条数不合法时抛出
     */
    private ReplyQuery normalizeQuery(ReplyQuery query) {
        ReplyQuery normalized = query == null ? new ReplyQuery() : query;
        if (normalized.getPageSize() < 1 || normalized.getPageSize() > 50) {
            throw validationError("每页条数需为 1-50");
        }
        return normalized;
    }

    /**
     * 将游标字符串解析为查询边界位置。
     *
     * @param cursor 游标字符串，格式为「创建时间毫秒数_回复 ID」
     * @return 游标位置；游标为空时返回 null，表示查询第一页
     * @throws BusinessException 游标内容不是合法数字时抛出
     */
    private CursorPosition resolveCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        int separator = cursor.indexOf('_');
        try {
            long epochMilli = Long.parseLong(cursor.substring(0, separator));
            long id = Long.parseLong(cursor.substring(separator + 1));
            // 数据库存储 UTC 本地时间，毫秒精度与 DATETIME(3) 一致。
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMilli),
                    ZoneOffset.UTC
            );
            return new CursorPosition(createdAt, id);
        } catch (NumberFormatException | IndexOutOfBoundsException exception) {
            throw validationError("cursor 格式不合法");
        }
    }

    /**
     * 用当前页最后一条回复生成下一页游标。
     *
     * @param reply 当前页最后一条回复
     * @return 下一页游标字符串
     */
    private String encodeCursor(Reply reply) {
        long epochMilli = reply.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return epochMilli + "_" + reply.getId();
    }

    /**
     * 游标位置：查询只返回 (created_at, id) 严格小于该位置的记录。
     *
     * @param createdAt 游标回复的创建时间
     * @param id 游标回复的 ID
     */
    private record CursorPosition(LocalDateTime createdAt, Long id) {
    }

    /**
     * 去除回复内容首尾空白并校验非空及最大长度。
     *
     * @param rawContent 原始回复内容
     * @return 规范化后的回复内容
     * @throws BusinessException 内容为空或超过 300 个字符时抛出
     */
    private String normalizeContent(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw validationError("回复内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw validationError("回复内容不能超过 300 个字符");
        }
        return content;
    }

    /**
     * 去除用户名首尾空白并校验非空及最大长度。
     *
     * @param currentUserName 当前用户名
     * @return 规范化后的用户名
     * @throws BusinessException 用户名为空或超过 32 个字符时抛出
     */
    private String normalizeUserName(String currentUserName) {
        String userName = currentUserName == null ? "" : currentUserName.trim();
        if (userName.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户信息不完整，请重新登录");
        }
        if (userName.length() > MAX_USER_NAME_LENGTH) {
            throw validationError("用户名不能超过 32 个字符");
        }
        return userName;
    }

    /**
     * 按层收集指定回复及其全部后代的 ID，用于删除完整回复子树。
     *
     * @param messageId 留言 ID，用于限定查询范围
     * @param rootId 子树根回复 ID
     * @return 包含根回复在内、按收集顺序排列的子树节点 ID 列表
     */
    private List<Long> collectSubtreeIds(Long messageId, Long rootId) {
        Set<Long> collected = new LinkedHashSet<>();
        collected.add(rootId);
        List<Long> frontier = List.of(rootId);

        while (!frontier.isEmpty()) {
            Set<Long> next = new LinkedHashSet<>();
            for (List<Long> batch : partition(frontier)) {
                for (Long childId : replyMapper.selectChildIdsByParentIds(messageId, batch)) {
                    if (childId != null && collected.add(childId)) {
                        next.add(childId);
                    }
                }
            }
            frontier = new ArrayList<>(next);
        }
        return new ArrayList<>(collected);
    }

    /**
     * 分批物理删除回复子树，避免单条 SQL 的 IN 参数过多。
     *
     * @param messageId 留言 ID，用于限定删除范围
     * @param ids 待删除回复 ID 列表
     * @return 实际删除的行数
     */
    private long deleteInBatches(Long messageId, List<Long> ids) {
        long deletedCount = 0;
        for (List<Long> batch : partition(ids)) {
            deletedCount += replyMapper.deleteByMessageIdAndIds(messageId, batch);
        }
        return deletedCount;
    }

    /**
     * 按固定批大小切分 ID 集合。
     *
     * @param ids 待切分的 ID 集合
     * @return 每个子列表大小不超过批处理上限的分组列表
     */
    private List<List<Long>> partition(Collection<Long> ids) {
        List<List<Long>> batches = new ArrayList<>();
        List<Long> values = new ArrayList<>(ids);
        for (int start = 0; start < values.size(); start += SQL_IN_BATCH_SIZE) {
            int end = Math.min(start + SQL_IN_BATCH_SIZE, values.size());
            batches.add(values.subList(start, end));
        }
        return batches;
    }

    /**
     * 将带子回复数量的回复查询结果转换为视图对象。
     *
     * @param reply 回复查询结果
     * @return 回复视图对象；子回复数量为空时按 0 处理
     */
    private ReplyVO toReplyVO(ReplyWithChildCount reply) {
        long childReplyCount = reply.getChildReplyCount() == null
                ? 0
                : reply.getChildReplyCount();
        return toReplyVO(reply, childReplyCount);
    }

    /**
     * 将回复实体转换为视图对象。
     *
     * @param reply 回复实体
     * @param childReplyCount 直接子回复数量
     * @return 回复视图对象
     */
    private ReplyVO toReplyVO(Reply reply, long childReplyCount) {
        return new ReplyVO(
                reply.getId(),
                reply.getMessageId(),
                reply.getParentId(),
                reply.getUserId(),
                reply.getUserName(),
                reply.getContent(),
                toInstant(reply.getCreatedAt()),
                childReplyCount
        );
    }

    /**
     * 将数据库中的 UTC 本地时间转换为瞬时时间。
     *
     * @param value 数据库时间
     * @return 对应的瞬时时间；输入为 null 时返回 null
     */
    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    /**
     * 创建留言不存在的业务异常。
     *
     * @return 留言不存在异常
     */
    private BusinessException messageNotFound() {
        return new BusinessException(ErrorCode.MESSAGE_NOT_FOUND, "这条留言不存在或已被删除");
    }

    /**
     * 创建回复不存在的业务异常。
     *
     * @return 回复不存在异常
     */
    private BusinessException replyNotFound() {
        return new BusinessException(ErrorCode.REPLY_NOT_FOUND, "要回复的内容不存在或已被删除");
    }

    /**
     * 创建参数校验失败的业务异常。
     *
     * @param message 校验失败提示
     * @return 参数校验失败异常
     */
    private BusinessException validationError(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }
}
