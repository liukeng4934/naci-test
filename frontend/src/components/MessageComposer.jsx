import { useRef, useState } from "react";
import { MAX_MESSAGE_LENGTH } from "../constants.js";
import { Avatar } from "./Avatar.jsx";
import { Icon } from "./Icon.jsx";

export function MessageComposer({
  onSubmit,
  disabled = false,
  user,
  authStatus,
  onRequireAuth,
}) {
  const [content, setContent] = useState("");
  const [fieldError, setFieldError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const contentRef = useRef(null);

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting || disabled || !user) return;

    const cleanContent = content.trim();
    if (!cleanContent) {
      setFieldError("写下一句话再发布吧");
      contentRef.current?.focus();
      return;
    }

    setFieldError("");
    setSubmitError("");
    setSubmitting(true);
    try {
      await onSubmit({ content: cleanContent });
      setContent("");
    } catch (error) {
      setSubmitError(
        error instanceof Error ? error.message : "留言发布失败，请稍后重试",
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (!user) {
    const checking = authStatus === "loading";
    return (
      <aside className="composer-card composer-auth-card" id="composer" aria-labelledby="composer-title">
        <div className="composer-heading">
          <span className="composer-kicker">NEW NOTE</span>
          <h2 id="composer-title">写下此刻</h2>
          <p>不必字斟句酌，真实本身就足够动人。</p>
        </div>

        <div className="composer-auth-panel">
          <span className="composer-auth-icon" aria-hidden="true">
            <Icon name="lock" />
          </span>
          <strong>{checking ? "正在确认登录状态" : "登录后，留下你的声音"}</strong>
          <p>发布留言、回复和删除自己的内容，都需要先登录。</p>
          <button
            className="composer-auth-button"
            type="button"
            onClick={() => onRequireAuth?.("login")}
            disabled={checking}
          >
            {checking ? "请稍候" : "登录 / 注册"}
            {!checking && <Icon name="arrowRight" />}
          </button>
        </div>

        <p className="privacy-note">
          <Icon name="lock" />
          账号仅用于确认留言与回复的发布者身份
        </p>
      </aside>
    );
  }

  const displayName = user.displayName || user.username;

  return (
    <aside className="composer-card" id="composer" aria-labelledby="composer-title">
      <div className="composer-heading">
        <span className="composer-kicker">NEW NOTE</span>
        <h2 id="composer-title">写下此刻</h2>
        <p>不必字斟句酌，真实本身就足够动人。</p>
      </div>

      <div className="composer-identity">
        <Avatar name={displayName} small />
        <div>
          <span>当前身份</span>
          <strong>{displayName}</strong>
          <small>用户 ID · {user.id}</small>
        </div>
        <span className="identity-status">已登录</span>
      </div>

      <form id="message-form" onSubmit={handleSubmit} noValidate>
        <div className={`field${fieldError ? " has-error" : ""}`}>
          <div className="label-row">
            <label htmlFor="content-input">想说的话</label>
            <span>{content.length} / {MAX_MESSAGE_LENGTH}</span>
          </div>
          <textarea
            ref={contentRef}
            id="content-input"
            name="content"
            rows="5"
            maxLength={MAX_MESSAGE_LENGTH}
            placeholder="写一点什么吧，开心的、困惑的、想说给未来听的……"
            value={content}
            onChange={(event) => {
              setContent(event.target.value);
              setFieldError("");
              setSubmitError("");
            }}
            disabled={submitting}
          />
          <p className="field-error">{fieldError}</p>
        </div>

        {submitError && <p className="composer-submit-error" role="alert">{submitError}</p>}

        <div className="composer-footer">
          <span className="composer-counter">将以 @{displayName} 的身份发布</span>
          <button className="publish-button" type="submit" disabled={submitting || disabled}>
            {submitting && <span className="button-spinner" aria-hidden="true" />}
            <span>{submitting ? "发布中" : "发布留言"}</span>
            {!submitting && <Icon name="arrowRight" />}
          </button>
        </div>
      </form>

      <p className="privacy-note">
        <Icon name="lock" />
        {disabled ? "服务连接中，暂时无法发布" : "内容将通过 API 保存到留言服务"}
      </p>
    </aside>
  );
}
