import { AUTH_EXPIRED_EVENT } from "../constants.js";
import { clearAuthToken, getAuthToken } from "../utils/storage.js";
import { ApiError } from "./apiError.js";

async function readJson(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new ApiError("服务端返回了无法解析的数据", response.status, "INVALID_RESPONSE");
  }
}

function normalizeListPayload(payload, page, pageSize) {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      page,
      pageSize,
      total: payload.length,
      hasMore: false,
    };
  }

  const items = Array.isArray(payload?.items) ? payload.items : [];
  const total = Number.isFinite(payload?.total) ? payload.total : items.length;
  const normalizedPage = Number.isFinite(payload?.page) ? payload.page : page;
  const normalizedPageSize = Number.isFinite(payload?.pageSize)
    ? payload.pageSize
    : pageSize;

  return {
    ...payload,
    items,
    page: normalizedPage,
    pageSize: normalizedPageSize,
    total,
    hasMore:
      typeof payload?.hasMore === "boolean"
        ? payload.hasMore
        : normalizedPage * normalizedPageSize < total,
  };
}

export function createHttpGuestbookApi(baseUrl = "/api") {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");

  async function request(path, { method = "GET", body, params } = {}) {
    const headers = { Accept: "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";

    const token = getAuthToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    const query = params
      ? new URLSearchParams(
          Object.entries(params).filter(([, value]) => value !== undefined),
        ).toString()
      : "";
    const url = `${normalizedBaseUrl}${path}${query ? `?${query}` : ""}`;

    let response;
    try {
      response = await fetch(url, {
        method,
        headers,
        credentials: "include",
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      throw new ApiError("无法连接服务器，请稍后重试", 0, "NETWORK_ERROR");
    }

    const payload = await readJson(response);
    if (!response.ok) {
      if (response.status === 401) {
        clearAuthToken();
        if (path !== "/auth/login" && path !== "/auth/register") {
          window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
        }
      }
      throw new ApiError(
        payload?.message || "请求失败，请稍后重试",
        response.status,
        payload?.code || "REQUEST_FAILED",
      );
    }
    // 后端统一返回 { code, message, data }，调用方只关心 data
    return payload?.data ?? null;
  }

  return {
    register(payload) {
      return request("/auth/register", { method: "POST", body: payload });
    },

    login(payload) {
      return request("/auth/login", { method: "POST", body: payload });
    },

    getMe() {
      return request("/auth/me");
    },

    logout() {
      return request("/auth/logout", { method: "POST" });
    },

    async listMessages({ sort = "newest", page = 1, pageSize = 8, q = "" } = {}) {
      const payload = await request("/messages", {
        params: {
          sort: sort === "oldest" ? "createdAt_asc" : "createdAt_desc",
          page,
          pageSize,
          q: q.trim() || undefined,
        },
      });
      return normalizeListPayload(payload, page, pageSize);
    },

    createMessage(payload) {
      return request("/messages", { method: "POST", body: payload });
    },

    deleteMessage(messageId) {
      return request(`/messages/${encodeURIComponent(messageId)}`, {
        method: "DELETE",
      });
    },

    async listReplies(
      messageId,
      { parentReplyId = null, cursor = null, pageSize = 8 } = {},
    ) {
      const payload = await request(
        `/messages/${encodeURIComponent(messageId)}/replies`,
        {
          params: {
            parentReplyId: parentReplyId ?? "null",
            cursor: cursor || undefined,
            pageSize,
          },
        },
      );
      return {
        ...normalizeListPayload(payload, 1, pageSize),
        nextCursor: payload?.nextCursor ?? null,
      };
    },

    createReply(messageId, payload) {
      return request(`/messages/${encodeURIComponent(messageId)}/replies`, {
        method: "POST",
        body: payload,
      });
    },

    deleteReply(replyId) {
      return request(`/replies/${encodeURIComponent(replyId)}`, {
        method: "DELETE",
      });
    },
  };
}
