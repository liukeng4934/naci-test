import { EmptyState } from "./EmptyState.jsx";
import { ErrorState } from "./ErrorState.jsx";
import { Icon } from "./Icon.jsx";
import { MessageCard } from "./MessageCard.jsx";
import { MessagePagination } from "./MessagePagination.jsx";
import { MessageSkeleton } from "./MessageSkeleton.jsx";
import { WallAside } from "./WallAside.jsx";
import { MESSAGE_PAGE_SIZE } from "../constants.js";

export function WallSection({
  messages,
  page,
  totalPages,
  total,
  status,
  error,
  query,
  sort,
  currentUser,
  activeReply,
  replyThreads,
  onQueryChange,
  onSortChange,
  onRetry,
  onPageChange,
  onEmptyAction,
  onOpenReply,
  onCancelReply,
  onCreateReply,
  onDeleteMessage,
  onDeleteReply,
  onLoadReplies,
  onLoadMoreReplies,
  onRequireAuth,
}) {
  const loading = status === "loading";
  const failed = status === "error";
  const ready = status === "ready";

  return (
    <section className="wall-section" id="wall" aria-labelledby="wall-title">
      <div className="container wall-layout">
        <div className="wall-main">
          <div className="section-head">
            <div>
              <p className="eyebrow"><span />COMMUNITY VOICES</p>
              <h2 id="wall-title">
                留言墙
                <span className="count-pill">
                  {loading ? "加载中" : failed ? "连接异常" : `${total} 条`}
                </span>
              </h2>
            </div>

            <div className="sort-tabs" role="group" aria-label="留言排序">
              <button
                className={`sort-tab${sort === "newest" ? " is-active" : ""}`}
                type="button"
                aria-pressed={sort === "newest"}
                onClick={() => onSortChange("newest")}
              >
                最新发布
              </button>
              <button
                className={`sort-tab${sort === "oldest" ? " is-active" : ""}`}
                type="button"
                aria-pressed={sort === "oldest"}
                onClick={() => onSortChange("oldest")}
              >
                最早发布
              </button>
            </div>
          </div>

          <div className="wall-tools">
            <label className="search-box" htmlFor="search-input">
              <Icon name="search" />
              <input
                id="search-input"
                type="search"
                placeholder="搜索留言、用户或回复……"
                autoComplete="off"
                value={query}
                onChange={(event) => onQueryChange(event.target.value)}
                disabled={failed}
              />
              {query && (
                <button type="button" aria-label="清空搜索" onClick={() => onQueryChange("")}>
                  <Icon name="close" />
                </button>
              )}
            </label>
            {query && ready && (
              <p className="search-hint">“{query}” 找到 {total} 条相关留言</p>
            )}
          </div>

          {loading && <MessageSkeleton />}
          {failed && <ErrorState message={error} onRetry={() => onRetry()} />}
          {ready && messages.length === 0 && (
            <EmptyState hasQuery={Boolean(query)} onAction={onEmptyAction} />
          )}
          {ready && messages.length > 0 && (
            <>
              <div className="message-list" aria-live="polite">
                {messages.map((message) => (
                  <MessageCard
                    key={message.id}
                    message={message}
                    currentUser={currentUser}
                    thread={replyThreads[message.id]}
                    activeReply={activeReply}
                    onOpenReply={onOpenReply}
                    onCancelReply={onCancelReply}
                    onCreateReply={onCreateReply}
                    onDeleteMessage={onDeleteMessage}
                    onDeleteReply={onDeleteReply}
                    onLoadReplies={onLoadReplies}
                    onLoadMoreReplies={onLoadMoreReplies}
                    onRequireAuth={onRequireAuth}
                  />
                ))}
              </div>

              <MessagePagination
                page={page}
                totalPages={totalPages}
                total={total}
                pageSize={MESSAGE_PAGE_SIZE}
                onPageChange={onPageChange}
              />
            </>
          )}
        </div>

        <WallAside />
      </div>
    </section>
  );
}
