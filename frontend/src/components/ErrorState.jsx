import { Icon } from "./Icon.jsx";

export function ErrorState({ message, onRetry }) {
  return (
    <div className="error-state" role="alert">
      <span className="error-mark" aria-hidden="true"><Icon name="cloud" /></span>
      <h3>留言暂时没有加载出来</h3>
      <p>{message || "请检查网络连接后再试一次。"}</p>
      <button type="button" onClick={onRetry}>重新加载</button>
    </div>
  );
}
