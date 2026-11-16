import { fullDate, relativeTime } from "../utils/format.js";
import { Avatar } from "./Avatar.jsx";
import { Icon } from "./Icon.jsx";
import { ReplyComposer } from "./ReplyComposer.jsx";

// 内联展示到第 4 层，更深的回复改为“聚焦该对话”查看
export const MAX_INLINE_DEPTH = 3;

export function ReplyNode({
  reply,
  messageId,
  thread,
  currentUser,
  depth = 0,
  focusRoot = false,
  expandedIds,
  onSetExpanded,
  onLoadReplies,
  onLoadMoreReplies,
  onFocusReply,
  activeReply,
  onRequestReply,
  onDeleteReply,
  onRequireAuth,
  onSubmitReply,
  onCancelReply,
}) {
  const children = thread.childrenByParent[reply.id] || [];
  const page = thread.pagination[reply.id];
  const expanded = expandedIds.has(reply.id);
  const childCount = reply.childReplyCount || 0;
  const canExpandInline = depth < MAX_INLINE_DEPTH;
  const showChildren = focusRoot || expanded;
  const parentName = reply.parentReplyId
    ? thread.byId[reply.parentReplyId]?.userName
    : "";
  const composerOpen =
    activeReply?.messageId === messageId && activeReply.parentReplyId === reply.id;
  const isOwn = currentUser?.id === reply.userId;

  function handleReply() {
    if (!currentUser) {
      onRequireAuth(reply);
      return;
    }
    onRequestReply(reply);
  }

  function toggleChildren() {
    if (expanded) {
      onSetExpanded(reply.id, false);
      return;
    }
    onSetExpanded(reply.id, true);
    if (!page?.initialized && !page?.loading) {
      onLoadReplies(messageId, reply.id);
    }
  }

  return (
    <article
      className={`reply-item reply-node${focusRoot ? " reply-node-focus" : ""}`}
      id={`reply-${reply.id}`}
    >
      <div className="reply-head">
        <Avatar name={reply.userName} small />
        <div className="reply-meta">
          <div className="reply-author-line">
            <strong>{reply.userName}</strong>
            {parentName && (
              <span className="reply-to-tag" title={`回复 ${parentName}`}>
                回复 @{parentName}
              </span>
            )}
            <span className="uid-tag" title="用户 ID">ID {reply.userId}</span>
            {isOwn && <span className="own-badge">我</span>}
            <time dateTime={reply.createdAt} title={fullDate(reply.createdAt)}>
              {relativeTime(reply.createdAt)}
            </time>
          </div>
        </div>
      </div>

      <p className="reply-content">{reply.content}</p>

      <div className="reply-actions">
        <button
          className="action-button"
          type="button"
          aria-label={`回复 ${reply.userName}`}
          onClick={handleReply}
        >
          <Icon name="reply" />
          <span>回复</span>
        </button>

        {!focusRoot && (childCount > 0 || page?.total > 0) && canExpandInline && (
          <button
            className="action-button child-toggle"
            type="button"
            aria-expanded={expanded}
            onClick={toggleChildren}
          >
            <span>
              {expanded ? "收起回复" : `${Math.max(childCount, page?.total || 0)} 条子回复`}
            </span>
          </button>
        )}

        {!focusRoot && (childCount > 0 || page?.total > 0) && !canExpandInline && (
          <button
            className="action-button child-toggle"
            type="button"
            onClick={() => onFocusReply(reply)}
          >
            <span>继续查看该对话（{Math.max(childCount, page?.total || 0)} 条）</span>
          </button>
        )}

        {isOwn && (
          <button
            className="action-button danger-action"
            type="button"
            aria-label="删除这条回复"
            onClick={() => onDeleteReply(reply)}
          >
            <Icon name="trash" />
            <span>删除</span>
          </button>
        )}
      </div>

      {composerOpen && (
        <ReplyComposer
          currentUser={currentUser}
          targetName={reply.userName}
          onSubmit={onSubmitReply}
          onCancel={onCancelReply}
        />
      )}

      {showChildren && (
        <div className="reply-children">
          {page?.loading && !page?.initialized && (
            <p className="thread-status" role="status">
              <span className="thread-spinner" aria-hidden="true" />
              正在加载回复
            </p>
          )}

          {page?.error && (
            <div className="thread-status thread-status-error" role="alert">
              <span>{page.error}</span>
              <button type="button" onClick={() => onLoadReplies(messageId, reply.id)}>
                重试
              </button>
            </div>
          )}

          {children.map((childId) => {
            const child = thread.byId[childId];
            if (!child) return null;
            return (
              <ReplyNode
                key={child.id}
                reply={child}
                messageId={messageId}
                thread={thread}
                currentUser={currentUser}
                depth={focusRoot ? 0 : depth + 1}
                expandedIds={expandedIds}
                onSetExpanded={onSetExpanded}
                onLoadReplies={onLoadReplies}
                onLoadMoreReplies={onLoadMoreReplies}
                onFocusReply={onFocusReply}
                activeReply={activeReply}
                onRequestReply={onRequestReply}
                onDeleteReply={onDeleteReply}
                onRequireAuth={onRequireAuth}
                onSubmitReply={onSubmitReply}
                onCancelReply={onCancelReply}
              />
            );
          })}

          {page?.initialized && children.length === 0 && (
            <p className="thread-empty">还没有子回复。</p>
          )}

          {page?.hasMore && (
            <button
              className="thread-load-more"
              type="button"
              onClick={() => onLoadMoreReplies(messageId, reply.id)}
              disabled={page.loading}
            >
              {page.loading ? "加载中…" : "加载更多回复"}
            </button>
          )}
        </div>
      )}
    </article>
  );
}
