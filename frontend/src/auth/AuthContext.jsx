import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { guestbookApi } from "../api/guestbookApi.js";
import { AUTH_EXPIRED_EVENT } from "../constants.js";
import { clearAuthToken, getAuthToken, setAuthToken } from "../utils/storage.js";

const AuthContext = createContext(null);

function normalizeAuthPayload(payload) {
  return {
    token: payload?.token || "",
    user: payload?.user || payload,
  };
}

export function AuthProvider({ api = guestbookApi, children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    let active = true;

    if (!getAuthToken()) {
      setStatus("ready");
      return () => {
        active = false;
      };
    }

    api
      .getMe()
      .then((payload) => {
        if (!active) return;
        setUser(normalizeAuthPayload(payload).user);
        setStatus("ready");
      })
      .catch(() => {
        if (!active) return;
        clearAuthToken();
        setUser(null);
        setStatus("ready");
      });

    return () => {
      active = false;
    };
  }, [api]);

  useEffect(() => {
    function handleExpired() {
      setUser(null);
      setStatus("ready");
    }

    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired);
  }, []);

  const applySession = useCallback((payload) => {
    const session = normalizeAuthPayload(payload);
    if (session.token) setAuthToken(session.token);
    setUser(session.user);
    setStatus("ready");
    return session.user;
  }, []);

  const login = useCallback(
    async (credentials) => applySession(await api.login(credentials)),
    [api, applySession],
  );

  const register = useCallback(
    async (credentials) => applySession(await api.register(credentials)),
    [api, applySession],
  );

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch (error) {
      // Clearing the local session must still succeed when the server is offline.
    } finally {
      clearAuthToken();
      setUser(null);
      setStatus("ready");
    }
  }, [api]);

  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
