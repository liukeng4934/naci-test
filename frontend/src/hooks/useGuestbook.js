import { useCallback, useEffect, useRef, useState } from "react";
import { MESSAGE_PAGE_SIZE, REPLY_PAGE_SIZE, REPLY_ROOT_KEY } from "../constants.js";
import { isToday } from "../utils/format.js";

const EMPTY_SUMMARY = { messageCount: 0, replyCount: 0, todayCount: 0 };

function createEmptyThread() {
  return { byId: {}, childrenByParent: {}, pagination: {} };
}

function normalizePageResult(result, page, pageSize) {
  if (Array.isArray(result)) {
    return {
      items: result,
      page,
      pageSize,
      total: result.length,
      hasMore: false,
    };
  }

  const items = Array.isArray(result?.items) ? result.items : [];
  const total = Number.isFinite(result?.total) ? result.total : items.length;
  const normalizedPage = Number.isFinite(result?.page) ? result.page : page;
  const normalizedPageSize = Number.isFinite(result?.pageSize)
    ? result.pageSize
    : pageSize;

  return {
    ...result,
    items,
    page: normalizedPage,
    pageSize: normalizedPageSize,
    total,
    hasMore:
      typeof result?.hasMore === "boolean"
        ? result.hasMore
        : normalizedPage * normalizedPageSize < total,
  };
}

function normalizeSummary(summary, items, total) {
  return {
    messageCount: Number.isFinite(summary?.messageCount)
      ? summary.messageCount
      : Number.isFinite(total)
        ? total
        : items.length,
    replyCount: Number.isFinite(summary?.replyCount)
      ? summary.replyCount
      : items.reduce((sum, message) => sum + (message.replyCount || 0), 0),
    todayCount: Number.isFinite(summary?.todayCount)
      ? summary.todayCount
      : items.filter((item) => isToday(item.createdAt)).length,
  };
}

function collectReplySubtree(thread, replyId) {
  const ids = new Set([replyId]);
  const queue = [replyId];

  while (queue.length > 0) {
    const parentId = queue.shift();
    for (const childId of thread.childrenByParent[parentId] || []) {
      if (ids.has(childId)) continue;
      ids.add(childId);
      queue.push(childId);
    }
  }

  return ids;
}

export function useGuestbook(api, { sort = "newest", query = "" } = {}) {
  const [messages, setMessages] = useState([]);
  const [summary, setSummary] = useState(EMPTY_SUMMARY);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("loading");
  const [error, setError] = useState(null);
  const [replyThreads, setReplyThreads] = useState({});

  const loadSequence = useRef(0);
  const replyThreadsRef = useRef(replyThreads);
  const pendingReplyRequests = useRef(new Set());

  const updateReplyThreads = useCallback((updater) => {
    setReplyThreads((current) => {
      const next = updater(current);
      replyThreadsRef.current = next;
      return next;
    });
  }, []);

  const loadMessages = useCallback(
    async ({ showLoading = true, targetPage = 1 } = {}) => {
      const sequence = ++loadSequence.current;
      if (showLoading) setStatus("loading");
      setError(null);

      try {
        const rawResult = await api.listMessages({
          sort,
          page: targetPage,
          pageSize: MESSAGE_PAGE_SIZE,
          q: query.trim(),
        });
        if (sequence !== loadSequence.current) return null;

        const result = normalizePageResult(rawResult, targetPage, MESSAGE_PAGE_SIZE);
        setMessages(result.items);
        setPage(result.page);
        setTotal(result.total);
        setSummary(normalizeSummary(result.summary, result.items, result.total));
        replyThreadsRef.current = {};
        setReplyThreads({});
        setStatus("ready");
        return result;
      } catch (requestError) {
        if (sequence !== loadSequence.current) return null;
        setError(requestError instanceof Error ? requestError.message : "留言加载失败");
        setStatus("error");
        return null;
      }
    },
    [api, query, sort],
  );

  useEffect(() => {
    const delay = query.trim() ? 280 : 0;
    const timer = window.setTimeout(() => {
      loadMessages({ showLoading: true, targetPage: 1 });
    }, delay);

    return () => window.clearTimeout(timer);
  }, [loadMessages, query]);

  const refresh = useCallback(
    () => loadMessages({ showLoading: true, targetPage: page }),
    [loadMessages, page],
  );

  const totalPages = Math.max(1, Math.ceil(total / MESSAGE_PAGE_SIZE));
  const goToPage = useCallback(
    (targetPage) => {
      const nextPage = Math.min(
        Math.max(1, Number(targetPage) || 1),
        totalPages,
      );
      if (nextPage === page && status === "ready") return Promise.resolve(null);
      return loadMessages({ showLoading: true, targetPage: nextPage });
    },
    [loadMessages, page, status, totalPages],
  );

  const loadReplies = useCallback(
    async (messageId, parentReplyId = null, { append = false } = {}) => {
      const targetParentId = parentReplyId || null;
      const parentKey = targetParentId || REPLY_ROOT_KEY;
      const requestKey = `${messageId}:${parentKey}:${append ? "more" : "reset"}`;
      if (pendingReplyRequests.current.has(requestKey)) return null;

      // 追加加载时沿用上次的游标；没有游标说明已经加载到最后一页
      const storedPage =
        replyThreadsRef.current[messageId]?.pagination?.[parentKey];
      const cursor = append ? storedPage?.nextCursor || null : null;
      if (append && storedPage?.initialized && !cursor) return null;

      pendingReplyRequests.current.add(requestKey);

      updateReplyThreads((current) => {
        const thread = current[messageId] || createEmptyThread();
        const previous = thread.pagination[parentKey] || {
          nextCursor: null,
          total: 0,
          hasMore: false,
          initialized: false,
        };
        return {
          ...current,
          [messageId]: {
            ...thread,
            pagination: {
              ...thread.pagination,
              [parentKey]: { ...previous, loading: true, error: null },
            },
          },
        };
      });

      try {
        const rawResult = await api.listReplies(messageId, {
          parentReplyId: targetParentId,
          cursor,
          pageSize: REPLY_PAGE_SIZE,
        });
        const result = normalizePageResult(rawResult, 1, REPLY_PAGE_SIZE);

        updateReplyThreads((current) => {
          const thread = current[messageId] || createEmptyThread();
          const byId = { ...thread.byId };
          for (const reply of result.items) byId[reply.id] = reply;

          const previousIds = append
            ? thread.childrenByParent[parentKey] || []
            : [];
          const seen = new Set(previousIds);
          const childIds = [...previousIds];
          for (const reply of result.items) {
            if (seen.has(reply.id)) continue;
            seen.add(reply.id);
            childIds.push(reply.id);
          }

          return {
            ...current,
            [messageId]: {
              byId,
              childrenByParent: {
                ...thread.childrenByParent,
                [parentKey]: childIds,
              },
              pagination: {
                ...thread.pagination,
                [parentKey]: {
                  nextCursor: result.nextCursor ?? null,
                  total: result.total,
                  hasMore: result.hasMore,
                  initialized: true,
                  loading: false,
                  error: null,
                },
              },
            },
          };
        });
        return result;
      } catch (requestError) {
        const message =
          requestError instanceof Error ? requestError.message : "回复加载失败";
        updateReplyThreads((current) => {
          const thread = current[messageId] || createEmptyThread();
          const previous = thread.pagination[parentKey] || {
            nextCursor: null,
            total: 0,
            hasMore: false,
            initialized: false,
          };
          return {
            ...current,
            [messageId]: {
              ...thread,
              pagination: {
                ...thread.pagination,
                [parentKey]: {
                  ...previous,
                  loading: false,
                  error: message,
                },
              },
            },
          };
        });
        return null;
      } finally {
        pendingReplyRequests.current.delete(requestKey);
      }
    },
    [api, updateReplyThreads],
  );

  const loadMoreReplies = useCallback(
    (messageId, parentReplyId = null) =>
      loadReplies(messageId, parentReplyId, { append: true }),
    [loadReplies],
  );

  const createMessage = useCallback(
    async (payload) => {
      const created = await api.createMessage(payload);
      const message = {
        ...created,
        replyCount: created.replyCount || 0,
        directReplyCount: created.directReplyCount || 0,
      };
      setMessages((current) =>
        [message, ...current.filter((item) => item.id !== message.id)].slice(
          0,
          MESSAGE_PAGE_SIZE,
        ),
      );
      setTotal((current) => current + 1);
      setSummary((current) => ({
        ...current,
        messageCount: current.messageCount + 1,
        todayCount: current.todayCount + 1,
      }));
      return message;
    },
    [api],
  );

  const createReply = useCallback(
    async (messageId, payload) => {
      const parentReplyId = payload.parentReplyId || null;
      const reply = await api.createReply(messageId, { ...payload, parentReplyId });

      setMessages((current) =>
        current.map((message) =>
          message.id === messageId
            ? {
                ...message,
                replyCount: message.replyCount + 1,
                directReplyCount:
                  message.directReplyCount + (parentReplyId ? 0 : 1),
              }
            : message,
        ),
      );
      setSummary((current) => ({
        ...current,
        replyCount: current.replyCount + 1,
        todayCount: current.todayCount + 1,
      }));

      updateReplyThreads((current) => {
        const thread = current[messageId] || createEmptyThread();
        const byId = { ...thread.byId, [reply.id]: reply };
        if (parentReplyId && byId[parentReplyId]) {
          byId[parentReplyId] = {
            ...byId[parentReplyId],
            childReplyCount: (byId[parentReplyId].childReplyCount || 0) + 1,
          };
        }
        return { ...current, [messageId]: { ...thread, byId } };
      });

      await loadReplies(messageId, parentReplyId, { append: false });
      return reply;
    },
    [api, loadReplies, updateReplyThreads],
  );

  const deleteMessage = useCallback(
    async (messageId) => {
      const target = messages.find((message) => message.id === messageId);
      const result = await api.deleteMessage(messageId);
      const deletedReplyCount = result?.deletedReplyCount || 0;

      setMessages((current) => current.filter((message) => message.id !== messageId));
      setTotal((current) => Math.max(0, current - 1));
      setSummary((current) => ({
        ...current,
        messageCount: Math.max(0, current.messageCount - 1),
        replyCount: Math.max(0, current.replyCount - deletedReplyCount),
        todayCount: Math.max(
          0,
          current.todayCount - (target && isToday(target.createdAt) ? 1 : 0),
        ),
      }));
      updateReplyThreads((current) => {
        if (!current[messageId]) return current;
        const next = { ...current };
        delete next[messageId];
        return next;
      });

      const remainingTotal = Math.max(0, total - 1);
      const lastPage = Math.max(1, Math.ceil(remainingTotal / MESSAGE_PAGE_SIZE));
      const targetPage = Math.min(page, lastPage);
      await loadMessages({ showLoading: false, targetPage });
      return { deletedReplyCount };
    },
    [api, loadMessages, messages, page, total, updateReplyThreads],
  );

  const deleteReply = useCallback(
    async (messageId, replyId) => {
      const result = await api.deleteReply(replyId);
      const deletedReplyCount = result?.deletedReplyCount || 0;
      const thread = replyThreadsRef.current[messageId];
      const target = thread?.byId?.[replyId];
      const parentKey = target?.parentReplyId || REPLY_ROOT_KEY;

      updateReplyThreads((current) => {
        const currentThread = current[messageId];
        if (!currentThread) return current;

        const removedIds = collectReplySubtree(currentThread, replyId);
        const byId = { ...currentThread.byId };
        for (const id of removedIds) delete byId[id];

        if (target?.parentReplyId && byId[target.parentReplyId]) {
          byId[target.parentReplyId] = {
            ...byId[target.parentReplyId],
            childReplyCount: Math.max(
              0,
              byId[target.parentReplyId].childReplyCount - 1,
            ),
          };
        }

        const childrenByParent = {};
        for (const [key, ids] of Object.entries(currentThread.childrenByParent)) {
          childrenByParent[key] = ids.filter((id) => !removedIds.has(id));
        }

        const pagination = { ...currentThread.pagination };
        if (pagination[parentKey]) {
          pagination[parentKey] = {
            ...pagination[parentKey],
            total: Math.max(0, pagination[parentKey].total - 1),
          };
        }

        return {
          ...current,
          [messageId]: { byId, childrenByParent, pagination },
        };
      });

      setMessages((current) =>
        current.map((message) =>
          message.id === messageId
            ? {
                ...message,
                replyCount: Math.max(0, message.replyCount - deletedReplyCount),
                directReplyCount: Math.max(
                  0,
                  message.directReplyCount - (target?.parentReplyId === null ? 1 : 0),
                ),
              }
            : message,
        ),
      );
      setSummary((current) => ({
        ...current,
        replyCount: Math.max(0, current.replyCount - deletedReplyCount),
      }));
      return { deletedReplyCount };
    },
    [api, updateReplyThreads],
  );

  return {
    messages,
    summary,
    status,
    error,
    page,
    totalPages,
    total,
    refresh,
    goToPage,
    replyThreads,
    loadReplies,
    loadMoreReplies,
    createMessage,
    createReply,
    deleteMessage,
    deleteReply,
  };
}
