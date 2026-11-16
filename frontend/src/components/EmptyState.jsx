import { Icon } from "./Icon.jsx";

export function EmptyState({ hasQuery, onAction }) {
  return (
    <div className="empty-state">
      <span className="empty-mark" aria-hidden="true"><Icon name="chat" /></span>
      <h3>{hasQuery ? "还没有找到相关留言" : "留言墙还是空的"}</h3>
      <p>
        {hasQuery
          ? "换一个关键词试试，或者成为第一个留下文字的人。"
          : "写下第一条留言，让这里从你的声音开始。"}
      </p>
      <button type="button" onClick={onAction}>
        {hasQuery ? "清空搜索" : "写一条留言"}
      </button>
    </div>
  );
}
