export function MessageSkeleton() {
  return (
    <div className="message-list skeleton-list" aria-hidden="true">
      {[0, 1, 2].map((item) => (
        <div className="message-card skeleton-card" key={item}>
          <div className="skeleton-head">
            <span className="skeleton skeleton-avatar" />
            <div className="skeleton-author">
              <span className="skeleton skeleton-line skeleton-name" />
              <span className="skeleton skeleton-line skeleton-time" />
            </div>
          </div>
          <span className="skeleton skeleton-line skeleton-content-line" />
          <span className="skeleton skeleton-line skeleton-content-line skeleton-content-short" />
          <span className="skeleton skeleton-line skeleton-actions-line" />
        </div>
      ))}
    </div>
  );
}
