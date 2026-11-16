import { useEffect, useState } from "react";
import { REPLY_ROOT_KEY } from "../constants.js";
import { fullDate, relativeTime } from "../utils/format.js";
import { Avatar } from "./Avatar.jsx";
import { Icon } from "./Icon.jsx";
import { ReplyComposer } from "./ReplyComposer.jsx";
import { ReplyNode, MAX_INLINE_DEPTH } from "./ReplyNode.jsx";
import { ReplyThread } from "./ReplyThread.jsx";

const EMPTY_THREAD = { byId: {}, childrenByParent: {}, pagination: {} };

export function MessageCard({
  message,
  currentUser,
  thread = EMPTY_THREAD,
  activeReply,
  onOpenReply,
  onCancelReply,
  onCreateReply,
  onDeleteMessage,
  onDeleteReply,
  onLoadReplies,
  onLoadMoreReplies,
  onRequireAuth,
}) {
  const [threadOpen, setThreadOpen] = useState(false);
  const [expandedIds, setExpandedIds] = useState(() => new Set());
  const [focusedReplyId, setFocusedReplyId] = useState(null);

  const composerOpen = activeReply?.messageId === message.id;
  const rootPage = thread.pagination[REPLY_ROOT_KEY];
  const isOwn = currentUser?.id === message.userId;

  function setExpanded(replyId, expanded) {
    setExpandedIds((current) => {
      const next = new Set(current);
      const targetParentId = thread.byId[replyId]?.parentReplyId ?? null;

      // 同一父节点下只保留一个展开分支，避免长列表同时铺开
      for (const id of [...next]) {
        if (id === replyId) continue;
        const node = thread.byId[id];
        if (node && (node.parentReplyId ?? null) === targetParentId) {
          next.delete(id);
        }
      }

      if (expanded) next.add(replyId);
      else next.delete(replyId);
      return next;
    });
  }

  useEffect(() => {
    if (!composerOpen) return;
    setThreadOpen(true);
    if (activeReply?.parentReplyId) {
      setExpanded(activeReply.parentReplyId, true);
    }
    if (!rootPage?.initialized && !rootPage?.loading && !rootPage?.error) {
      onLoadReplies(message.id, null);
    }
  }, [
    activeReply?.parentReplyId,
    composerOpen,
    message.id,
    onLoadReplies,
    rootPage?.error,
    rootPage?.initialized,
    rootPage?.loading,
  ]);

  useEffect(() => {
    if (
      threadOpen &&
      message.replyCount > 0 &&
      !rootPage?.initialized &&
      !rootPage?.loading &&
      !rootPage?.error
    ) {
      onLoadReplies(message.id);
    }
  }, [
    message.id,
    message.replyCount,
    onLoadReplies,
    rootPage?.error,
    rootPage?.initialized,
    rootPage?.loading,
    threadOpen,
  ]);

  useEffect(() => {
    if (focusedReplyId && !thread.byId[focusedReplyId]) {
      setFocusedReplyId(null);
    }
  }, [focusedReplyId, thread.byId]);

  function toggleThread() {
    const nextOpen = !threadOpen;
    setThreadOpen(nextOpen);
    if (nextOpen && !rootPage?.initialized && !rootPage?.loading) {
      onLoadReplies(message.id, null);
    }
  }

  function handleReplyToMessage() {
    onOpenReply(message, null);
  }

  function handleRequireAuth(reply) {
    if (onRequireAuth) onRequireAuth(message, reply);
    else onOpenReply(message, reply);
  }

  function handleFocusReply(reply) {
    setFocusedReplyId(reply.id);
    setThreadOpen(true);
    setExpanded(reply.id, true);

    const page = thread.pagination[reply.id];
    if (!page?.initialized && !page?.loading && !page?.error) {
      onLoadReplies(message.id, reply.id);
    }
  }

  function getReplyDepth(replyId) {
    let depth = 0;
    let current = thread.byId[replyId];
    const visited = new Set();

    while (current && !visited.has(current.id)) {
      visited.add(current.id);
      if (!current.parentReplyId) return depth;
      depth += 1;
      current = thread.byId[current.parentReplyId];
    }

    return depth;
  }

  async function handleReplySubmit(payload) {
    const parentReplyId = activeReply?.parentReplyId || null;
    const created = await onCreateReply(message.id, { ...payload, parentReplyId });
    setThreadOpen(true);

    if (parentReplyId) {
      if (getReplyDepth(parentReplyId) >= MAX_INLINE_DEPTH) {
        handleFocusReply(thread.byId[parentReplyId] || { id: parentReplyId });
      } else {
        setExpanded(parentReplyId, true);
      }
    }
    return created;
  }

  return (
    <article className="message-card" id={`message-${message.id}`}>
      <div className="message-head">
        <Avatar name={message.userName} />
        <div className="author-meta">
          <div className="author-line">
            <strong>{message.userName}</strong>
            {isOwn && <span className="own-badge">我</span>}
            <span className="author-dot" aria-hidden="true" />
          </div>
          <div className="time-meta">
            <Icon name="clock" />
            <time dateTime={message.createdAt} title={fullDate(message.createdAt)}>
              {relativeTime(message.createdAt)}
            </time>
            <span className="uid-tag" title="用户 ID">ID {message.userId}</span>
          </div>
        </div>
      </div>

      <p className="message-content">{message.content}</p>

      <div className="message-actions">
        <button
          className="action-button reply-action"
          type="button"
          aria-label={`回复 ${message.userName} 的留言`}
          onClick={handleReplyToMessage}
        >
          <Icon name="reply" />
          <span>回复</span>
        </button>

        {message.replyCount > 0 && (
          <button
            className="action-button thread-action"
            type="button"
            aria-expanded={threadOpen}
            onClick={toggleThread}
          >
            <Icon name="chat" />
            <span>{threadOpen ? "收起回复" : `${message.replyCount} 条回复`}</span>
          </button>
        )}

        {isOwn && (
          <button
            className="action-button danger-action delete-message-action"
            type="button"
            aria-label="删除这条留言"
            onClick={() => onDeleteMessage(message)}
          >
            <Icon name="trash" />
            <span>删除</span>
          </button>
        )}
      </div>

      {(threadOpen || composerOpen) && (
        <div className="replies-block">
          {composerOpen && !activeReply?.parentReplyId && (
            <ReplyComposer
              currentUser={currentUser}
              targetName={null}
              onSubmit={handleReplySubmit}
              onCancel={onCancelReply}
            />
          )}

          {threadOpen && (
            <ReplyThread
              messageId={message.id}
              thread={thread}
              currentUser={currentUser}
              activeReply={activeReply}
              expandedIds={expandedIds}
              focusedReplyId={focusedReplyId}
              onSetExpanded={setExpanded}
              onLoadReplies={onLoadReplies}
              onLoadMoreReplies={onLoadMoreReplies}
              onFocusReply={handleFocusReply}
              onExitFocus={() => setFocusedReplyId(null)}
              onRequestReply={(reply) => onOpenReply(message, reply)}
              onDeleteReply={(reply) => onDeleteReply(message, reply)}
              onRequireAuth={(reply) => handleRequireAuth(reply)}
              onSubmitReply={handleReplySubmit}
              onCancelReply={onCancelReply}
            />
          )}
        </div>
      )}
    </article>
  );
}
