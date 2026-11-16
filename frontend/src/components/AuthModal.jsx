import { useEffect, useRef, useState } from "react";
import {
  MAX_PASSWORD_LENGTH,
  MAX_USERNAME_LENGTH,
  MIN_PASSWORD_LENGTH,
  MIN_USERNAME_LENGTH,
} from "../constants.js";
import { Icon } from "./Icon.jsx";

export function AuthModal({
  open,
  initialMode = "login",
  onClose,
  onLogin,
  onRegister,
  onSuccess,
}) {
  const [mode, setMode] = useState(initialMode);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldError, setFieldError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const usernameRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    setMode(initialMode);
    setPassword("");
    setConfirmPassword("");
    setFieldError("");
    setSubmitError("");
    setSubmitting(false);

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const frame = window.requestAnimationFrame(() => usernameRef.current?.focus());

    function handleKeyDown(event) {
      if (event.key === "Escape") onClose();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [initialMode, onClose, open]);

  if (!open) return null;

  const isRegister = mode === "register";

  function switchMode(nextMode) {
    setMode(nextMode);
    setFieldError("");
    setSubmitError("");
    setPassword("");
    setConfirmPassword("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    const cleanUsername = username.trim();
    if (
      cleanUsername.length < MIN_USERNAME_LENGTH ||
      cleanUsername.length > MAX_USERNAME_LENGTH
    ) {
      setFieldError(`用户名需为 ${MIN_USERNAME_LENGTH}-${MAX_USERNAME_LENGTH} 个字符`);
      return;
    }
    if (
      password.length < MIN_PASSWORD_LENGTH ||
      password.length > MAX_PASSWORD_LENGTH
    ) {
      setFieldError(`密码需为 ${MIN_PASSWORD_LENGTH}-${MAX_PASSWORD_LENGTH} 个字符`);
      return;
    }
    if (isRegister && password !== confirmPassword) {
      setFieldError("两次输入的密码不一致");
      return;
    }

    setFieldError("");
    setSubmitError("");
    setSubmitting(true);
    try {
      const action = isRegister ? onRegister : onLogin;
      const user = await action({ username: cleanUsername, password });
      onSuccess?.(user, mode);
    } catch (error) {
      setSubmitError(
        error instanceof Error
          ? error.message
          : `${isRegister ? "注册" : "登录"}失败，请稍后重试`,
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) onClose();
      }}
    >
      <section
        className="modal-card auth-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        <button
          className="modal-close"
          type="button"
          aria-label="关闭"
          onClick={onClose}
          disabled={submitting}
        >
          <Icon name="close" />
        </button>

        <div className="auth-modal-head">
          <p className="modal-kicker">YOUR ECHO ID</p>
          <h2 id="auth-title">{isRegister ? "创建你的账号" : "欢迎回来"}</h2>
          <p>
            {isRegister
              ? "注册后即可发布留言、参与回复，并管理自己的内容。"
              : "登录后继续留下文字，也继续回应那些认真写下的话。"}
          </p>
        </div>

        <div className="auth-tabs" role="tablist" aria-label="登录或注册">
          <button
            className={!isRegister ? "is-active" : ""}
            type="button"
            role="tab"
            aria-selected={!isRegister}
            onClick={() => switchMode("login")}
          >
            登录
          </button>
          <button
            className={isRegister ? "is-active" : ""}
            type="button"
            role="tab"
            aria-selected={isRegister}
            onClick={() => switchMode("register")}
          >
            注册
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label className="modal-field" htmlFor="auth-username">
            <span>用户名</span>
            <input
              ref={usernameRef}
              id="auth-username"
              name="username"
              type="text"
              autoComplete="username"
              minLength={MIN_USERNAME_LENGTH}
              maxLength={MAX_USERNAME_LENGTH}
              value={username}
              placeholder="2-20 个字符"
              onChange={(event) => {
                setUsername(event.target.value);
                setFieldError("");
                setSubmitError("");
              }}
              disabled={submitting}
            />
          </label>

          <label className="modal-field" htmlFor="auth-password">
            <span>密码</span>
            <input
              id="auth-password"
              name="password"
              type="password"
              autoComplete={isRegister ? "new-password" : "current-password"}
              minLength={MIN_PASSWORD_LENGTH}
              maxLength={MAX_PASSWORD_LENGTH}
              value={password}
              placeholder="至少 6 个字符"
              onChange={(event) => {
                setPassword(event.target.value);
                setFieldError("");
                setSubmitError("");
              }}
              disabled={submitting}
            />
          </label>

          {isRegister && (
            <label className="modal-field" htmlFor="auth-confirm-password">
              <span>确认密码</span>
              <input
                id="auth-confirm-password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                maxLength={MAX_PASSWORD_LENGTH}
                value={confirmPassword}
                placeholder="再输入一次密码"
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                  setFieldError("");
                  setSubmitError("");
                }}
                disabled={submitting}
              />
            </label>
          )}

          {(fieldError || submitError) && (
            <p className="auth-error" role="alert">
              {fieldError || submitError}
            </p>
          )}

          <button className="auth-submit" type="submit" disabled={submitting}>
            {submitting && <span className="button-spinner" aria-hidden="true" />}
            {submitting
              ? isRegister
                ? "正在注册"
                : "正在登录"
              : isRegister
                ? "创建账号"
                : "登录"}
          </button>
        </form>
      </section>
    </div>
  );
}
