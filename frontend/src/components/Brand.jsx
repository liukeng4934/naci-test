export function Brand({ footer = false, label = "留声留言墙首页" }) {
  return (
    <a className={`brand${footer ? " footer-brand" : ""}`} href="#" aria-label={label}>
      <span className="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 38 38" fill="none">
          <path d="M10 12h18M10 19h12M10 26h15" />
          <circle cx="28.5" cy="26" r="3.5" />
        </svg>
      </span>
      <span className="brand-copy">
        <strong>留声</strong>
        <small>ECHO GUESTBOOK</small>
      </span>
    </a>
  );
}
