function createPageItems(currentPage, totalPages) {
  if (totalPages <= 1) return [1];

  const pages = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1]);
  if (currentPage <= 4) {
    [2, 3, 4, 5].forEach((page) => pages.add(page));
  }
  if (currentPage >= totalPages - 3) {
    [totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1].forEach((page) =>
      pages.add(page),
    );
  }

  const sortedPages = [...pages]
    .filter((page) => page >= 1 && page <= totalPages)
    .sort((a, b) => a - b);
  const items = [];

  sortedPages.forEach((page, index) => {
    const previousPage = sortedPages[index - 1];
    if (previousPage && page - previousPage > 1) {
      items.push({ type: "ellipsis", key: `ellipsis-${previousPage}-${page}` });
    }
    items.push({ type: "page", key: page, page });
  });

  return items;
}

export function MessagePagination({
  page,
  totalPages,
  total,
  pageSize,
  disabled = false,
  onPageChange,
}) {
  const safeTotalPages = Math.max(1, Number(totalPages) || 1);
  const safePage = Math.min(Math.max(1, Number(page) || 1), safeTotalPages);
  const pageItems = createPageItems(safePage, safeTotalPages);
  const rangeStart = total === 0 ? 0 : (safePage - 1) * pageSize + 1;
  const rangeEnd = Math.min(total, safePage * pageSize);
  const controlsDisabled = disabled || safeTotalPages <= 1;

  return (
    <nav className="message-pagination" aria-label="留言分页">
      <p className="pagination-summary" aria-live="polite">
        第 <strong>{safePage}</strong> / {safeTotalPages} 页
        <span>
          当前显示 {rangeStart}-{rangeEnd} 条，共 {total} 条
        </span>
      </p>

      <div className="pagination-controls">
        <button
          className="pagination-button"
          type="button"
          disabled={controlsDisabled || safePage === 1}
          onClick={() => onPageChange(safePage - 1)}
        >
          上一页
        </button>

        <div className="pagination-pages" role="group" aria-label="选择页码">
          {pageItems.map((item) =>
            item.type === "ellipsis" ? (
              <span className="pagination-ellipsis" key={item.key} aria-hidden="true">
                …
              </span>
            ) : (
              <button
                className={`pagination-page${item.page === safePage ? " is-active" : ""}`}
                key={item.key}
                type="button"
                aria-label={`第 ${item.page} 页`}
                aria-current={item.page === safePage ? "page" : undefined}
                disabled={disabled || item.page === safePage}
                onClick={() => onPageChange(item.page)}
              >
                {item.page}
              </button>
            ),
          )}
        </div>

        <button
          className="pagination-button"
          type="button"
          disabled={controlsDisabled || safePage === safeTotalPages}
          onClick={() => onPageChange(safePage + 1)}
        >
          下一页
        </button>

        <label className="pagination-jump">
          <span>跳至</span>
          <select
            value={safePage}
            disabled={controlsDisabled}
            aria-label="选择留言页码"
            onChange={(event) => onPageChange(Number(event.target.value))}
          >
            {Array.from({ length: safeTotalPages }, (_, index) => index + 1).map((pageNumber) => (
              <option key={pageNumber} value={pageNumber}>
                {pageNumber}
              </option>
            ))}
          </select>
          <span>页</span>
        </label>
      </div>
    </nav>
  );
}
