export function Toast({ message }) {
  return (
    <div className={`toast${message ? " is-visible" : ""}`} role="status" aria-live="polite">
      <span className="toast-icon" aria-hidden="true">✦</span>
      <span>{message}</span>
    </div>
  );
}
