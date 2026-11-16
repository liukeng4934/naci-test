import { Brand } from "./Brand.jsx";
import { Icon } from "./Icon.jsx";

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="container footer-inner">
        <Brand footer label="返回顶部" />
        <p>
          认真写下，真诚回应。© {new Date().getFullYear()} ECHO
          <span className="api-mode api-mode-http">LIVE API</span>
        </p>
        <a className="back-top" href="#">
          回到顶部
          <Icon name="arrowRight" className="back-top-icon" />
        </a>
      </div>
    </footer>
  );
}
