import { MessageComposer } from "./MessageComposer.jsx";

export function Hero({
  stats,
  onCreateMessage,
  composerDisabled,
  user,
  authStatus,
  onRequireAuth,
}) {
  return (
    <section className="hero container" aria-labelledby="hero-title">
      <div className="hero-copy">
        <p className="eyebrow"><span />OPEN GUESTBOOK · 欢迎来到留言墙</p>
        <h1 id="hero-title">
          让每一句话，<br />
          都有<span className="highlight">
            回响
            <svg viewBox="0 0 150 18" aria-hidden="true">
              <path d="M4 12C38 4 95 3 146 8" />
            </svg>
          </span>。
        </h1>
        <p className="hero-lede">
          这里没有匆忙的划过，只有认真写下的文字和真诚的回应。留下一段心情、一个问题，或一句想对陌生人说的话。
        </p>

        <div className="hero-meta" aria-label="留言墙数据">
          <div className="stat">
            <strong>{stats.messages ?? "--"}</strong>
            <span>条留言</span>
          </div>
          <div className="stat-divider" aria-hidden="true" />
          <div className="stat">
            <strong>{stats.replies ?? "--"}</strong>
            <span>条回响</span>
          </div>
          <div className="stat-divider" aria-hidden="true" />
          <div className="stat">
            <strong>{stats.today ?? "--"}</strong>
            <span>今日新增</span>
          </div>
        </div>

        <a className="hero-link" href="#wall">
          看看大家在说什么
          <svg viewBox="0 0 18 18" aria-hidden="true">
            <path d="M4 9h10M10 5l4 4-4 4" />
          </svg>
        </a>

        <div className="hero-note" aria-hidden="true">
          <span className="note-line" />
          <span>WORDS<br />FIND<br />THEIR WAY</span>
        </div>
      </div>

      <MessageComposer
        onSubmit={onCreateMessage}
        disabled={composerDisabled}
        user={user}
        authStatus={authStatus}
        onRequireAuth={onRequireAuth}
      />
    </section>
  );
}
