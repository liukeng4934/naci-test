import { Icon } from "./Icon.jsx";

export function WallAside() {
  return (
    <aside className="wall-aside">
      <section className="rules-card" id="guidelines" aria-labelledby="rules-title">
        <p className="aside-kicker">HOUSE RULES</p>
        <h3 id="rules-title">留一点温柔，<br />也留一点真诚。</h3>
        <ol className="rules-list">
          <li><span>01</span><p>尊重每一种不同的声音，不攻击、不冒犯。</p></li>
          <li><span>02</span><p>分享真实感受，让文字成为人与人之间的桥。</p></li>
          <li><span>03</span><p>回复之前多读一遍，善意值得被认真表达。</p></li>
        </ol>
        <p className="rules-footnote">— 留声留言墙</p>
      </section>

      <blockquote className="quote-card">
        <span className="quote-mark" aria-hidden="true">“</span>
        <p>我们随口说出的一句话，也许正是另一个人等了很久的回应。</p>
        <footer>给每一个认真表达的人</footer>
      </blockquote>

      <section className="local-card">
        <span className="local-icon" aria-hidden="true"><Icon name="cloud" /></span>
        <div>
          <h4>服务端数据源</h4>
          <p>留言与回复正在通过后端 API 加载与保存。</p>
        </div>
      </section>
    </aside>
  );
}
