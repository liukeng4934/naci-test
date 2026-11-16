import { useEffect, useRef } from "react";
import { Icon } from "./Icon.jsx";

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "确认删除",
  busy = false,
  error = "",
  onConfirm,
  onCancel,
}) {
  const cancelRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const frame = window.requestAnimationFrame(() => cancelRef.current?.focus());

    function handleKeyDown(event) {
      if (event.key === "Escape" && !busy) onCancel();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [busy, onCancel, open]);

  if (!open) return null;

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) onCancel();
      }}
    >
      <section
        className="modal-card confirm-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-description"
      >
        <span className="confirm-icon" aria-hidden="true">
          <Icon name="trash" />
        </span>
        <h2 id="confirm-title">{title}</h2>
        <p id="confirm-description">{description}</p>
        {error && <p className="confirm-error" role="alert">{error}</p>}
        <div className="confirm-actions">
          <button
            ref={cancelRef}
            className="confirm-cancel"
            type="button"
            onClick={onCancel}
            disabled={busy}
          >
            取消
          </button>
          <button
            className="confirm-delete"
            type="button"
            onClick={onConfirm}
            disabled={busy}
          >
            {busy && <span className="button-spinner" aria-hidden="true" />}
            {busy ? "删除中" : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
