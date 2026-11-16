import { AccountMenu } from "./AccountMenu.jsx";
import { Brand } from "./Brand.jsx";
import { Icon } from "./Icon.jsx";

export function SiteHeader({ user, authStatus, onOpenAuth, onLogout }) {
  return (
    <header className="site-header">
      <div className="container header-inner">
        <Brand />
        <nav className="main-nav" aria-label="主导航">
          <a className="nav-link is-active" href="#wall">留言墙</a>
          <a className="nav-link" href="#guidelines">关于这里</a>
        </nav>
        <div className="header-actions">
          <AccountMenu
            user={user}
            status={authStatus}
            onOpenAuth={onOpenAuth}
            onLogout={onLogout}
          />
          <a className="header-cta" href="#composer">
            <span>写一条留言</span>
            <Icon name="arrowRight" />
          </a>
        </div>
      </div>
    </header>
  );
}
