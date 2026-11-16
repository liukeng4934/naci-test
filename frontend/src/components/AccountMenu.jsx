import { useEffect, useRef, useState } from "react";
import { Avatar } from "./Avatar.jsx";
import { Icon } from "./Icon.jsx";

export function AccountMenu({ user, status, onOpenAuth, onLogout }) {
  const [open, setOpen] = useState(false);
  const areaRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    function handlePointerDown(event) {
      if (!areaRef.current?.contains(event.target)) setOpen(false);
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") setOpen(false);
    }

    window.addEventListener("pointerdown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  if (status === "loading") {
    return <span className="account-loading">确认登录中</span>;
  }

  if (!user) {
    return (
      <button className="account-login" type="button" onClick={() => onOpenAuth("login")}>
        <Icon name="user" />
        <span>登录 / 注册</span>
      </button>
    );
  }

  return (
    <div className="account-area" ref={areaRef}>
      <button
        className="account-trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <Avatar name={user.username} small />
        <span className="account-name">{user.username}</span>
        <Icon name="chevronDown" className="account-chevron" />
      </button>

      {open && (
        <div className="account-menu" role="menu">
          <div className="account-summary">
            <Avatar name={user.username} small />
            <div>
              <strong>{user.username}</strong>
              <span>用户 ID · {user.id}</span>
            </div>
          </div>
          <button
            className="account-logout"
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
          >
            <Icon name="logout" />
            <span>退出登录</span>
          </button>
        </div>
      )}
    </div>
  );
}
