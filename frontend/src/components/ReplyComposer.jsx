import { useEffect, useId, useRef, useState } from "react";
import { MAX_REPLY_LENGTH } from "../constants.js";
import { Avatar } from "./Avatar.jsx";
import { Icon } from "./Icon.jsx";

export function ReplyComposer({ currentUser, targetName, onSubmit, onCancel }) {
  const [content, setContent] = useState("");
  const [fieldError, setFieldError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const contentId = useId();
  const contentRef = useRef(null);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => contentRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, []);

  if (!currentUser) return null;

  const displayName = currentUser.displayName || currentUser.username;

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    const cleanContent = content.trim();
    if (!cleanContent) {
      setFieldError("写下回复内容再送出");
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
        error instanceof Error ? error.message : "回复失败，请稍后重试",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="reply-composer">
      <div className="reply-composer-head">
        <div>
          <strong>写下回复</strong>
          <span className="reply-target-badge">
            {targetName ? `回复 @${targetName}` : "回复这条留言"}
          </span>
        </div>
        <span className="reply-composer-identity">
          <Avatar name={displayName} small />
          @{displayName}
        </span>
      </div>

      <form className="reply-form reply-form-single" onSubmit={handleSubmit} noValidate>
        <div className={`reply-field${fieldError ? " has-error" : ""}`}>
          <label htmlFor={contentId}>回复内容</label>
          <textarea
            ref={contentRef}
            className="reply-textarea"
            id={contentId}
            maxLength={MAX_REPLY_LENGTH}
            value={content}
            placeholder={targetName ? `回复 @${targetName}……` : "写下你的回应……"}
            onChange={(event) => {
              setContent(event.target.value);
              setFieldError("");
              setSubmitError("");
            }}
            disabled={submitting}
          />
          <p className="reply-error">{fieldError}</p>
        </div>

        {submitError && <p className="reply-submit-error" role="alert">{submitError}</p>}

        <div className="reply-form-foot">
          <span className="reply-counter">{content.length} / {MAX_REPLY_LENGTH}</span>
          <div className="reply-form-buttons">
            <button className="reply-cancel" type="button" onClick={onCancel} disabled={submitting}>
              取消
            </button>
            <button className="reply-submit" type="submit" disabled={submitting}>
              {submitting && <span className="button-spinner" aria-hidden="true" />}
              {submitting ? "发送中" : "送出回复"}
              {!submitting && <Icon name="arrowRight" />}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
