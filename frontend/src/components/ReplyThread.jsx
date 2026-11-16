import { Fragment } from "react";
import { REPLY_ROOT_KEY } from "../constants.js";
import { ReplyNode } from "./ReplyNode.jsx";

function buildReplyPath(thread, replyId) {
  const path = [];
  const visited = new Set();
  let current = thread.byId[replyId];

  while (current && !visited.has(current.id)) {
    path.unshift(current);
    visited.add(current.id);
    current = current.parentReplyId
      ? thread.byId[current.parentReplyId]
      : null;
  }

  return path;
}

export function ReplyThread({
  messageId,
  thread,
  currentUser,
  activeReply,
  expandedIds,
  focusedReplyId,
  onSetExpanded,
  onLoadReplies,
  onLoadMoreReplies,
  onFocusReply,
  onExitFocus,
  onRequestReply,
  onDeleteReply,
  onRequireAuth,
  onSubmitReply,
  onCancelReply,
}) {
  const page = thread.pagination[REPLY_ROOT_KEY];
  const replyIds = thread.childrenByParent[REPLY_ROOT_KEY] || [];
  const focusedReply = focusedReplyId ? thread.byId[focusedReplyId] : null;

  if (page?.loading && !page?.initialized) {
    return (
      <p className="thread-status" role="status">
        <span className="thread-spinner" aria-hidden="true" />
        正在加载回复
      </p>
    );
  }

  if (page?.error && !page?.initialized) {
    return (
      <div className="thread-status thread-status-error" role="alert">
        <span>{page.error}</span>
        <button type="button" onClick={() => onLoadReplies(messageId, null)}>
          重试
        </button>
      </div>
    );
  }

  if (!page?.initialized) return null;

  const sharedProps = {
    messageId,
    thread,
    currentUser,
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
  };

  if (focusedReply) {
    const path = buildReplyPath(thread, focusedReply.id);

    return (
      <div className="reply-thread">
        <div className="focus-bar">
          <button className="focus-back" type="button" onClick={onExitFocus}>
            ← 全部回复
          </button>
          <span className="focus-path">
            {path.map((node, index) => (
              <Fragment key={node.id}>
                {index > 0 && <span className="focus-sep">/</span>}
                {index === path.length - 1 ? (
                  <span className="focus-current">{node.userName}</span>
                ) : (
                  <button
                    className="focus-crumb"
                    type="button"
                    onClick={() => onFocusReply(node)}
                  >
                    {node.userName}
                  </button>
                )}
              </Fragment>
            ))}
          </span>
        </div>

        <ReplyNode {...sharedProps} reply={focusedReply} depth={0} focusRoot />
      </div>
    );
  }

  return (
    <div className="reply-thread">
      {replyIds.length > 0 && (
        <p className="thread-caption">
          {page.total} 条回复 · 按父子关系展示
        </p>
      )}

      <div className="reply-list">
        {replyIds.map((replyId) => {
          const reply = thread.byId[replyId];
          if (!reply) return null;
          return (
            <ReplyNode key={reply.id} {...sharedProps} reply={reply} depth={0} />
          );
        })}
      </div>

      {page?.error && page?.initialized && (
        <div className="thread-status thread-status-error" role="alert">
          <span>{page.error}</span>
          <button type="button" onClick={() => onLoadReplies(messageId, null)}>
            重试
          </button>
        </div>
      )}

      {replyIds.length === 0 && !page.loading && (
        <p className="thread-empty">还没有回复，来写下第一条吧。</p>
      )}

      {page.hasMore && (
        <button
          className="thread-load-more"
          type="button"
          onClick={() => onLoadMoreReplies(messageId, null)}
          disabled={page.loading}
        >
          {page.loading ? "加载中…" : "加载更早的回复"}
        </button>
      )}
    </div>
  );
}
