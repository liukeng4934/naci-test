import { useCallback, useEffect, useRef, useState } from "react";
import { guestbookApi } from "./api/guestbookApi.js";
import { AuthProvider, useAuth } from "./auth/AuthContext.jsx";
import { AuthModal } from "./components/AuthModal.jsx";
import { ConfirmDialog } from "./components/ConfirmDialog.jsx";
import { Hero } from "./components/Hero.jsx";
import { SiteFooter } from "./components/SiteFooter.jsx";
import { SiteHeader } from "./components/SiteHeader.jsx";
import { Toast } from "./components/Toast.jsx";
import { WallSection } from "./components/WallSection.jsx";
import { useGuestbook } from "./hooks/useGuestbook.js";

const EMPTY_STATS = { messages: null, replies: null, today: null };

function GuestbookPage() {
  const { user, status: authStatus, login, register, logout } = useAuth();
  const [sort, setSort] = useState("newest");
  const [query, setQuery] = useState("");
  const {
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
  } = useGuestbook(guestbookApi, { sort, query });

  const [activeReply, setActiveReply] = useState(null);
  const [pendingReply, setPendingReply] = useState(null);
  const [authModal, setAuthModal] = useState({ open: false, mode: "login" });
  const [confirmState, setConfirmState] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [toast, setToast] = useState("");
  const toastTimer = useRef(null);

  const notify = useCallback((message) => {
    if (!message) return;
    setToast(message);
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(""), 3_000);
  }, []);

  useEffect(
    () => () => {
      window.clearTimeout(toastTimer.current);
    },
    [],
  );

  useEffect(() => {
    if (authStatus === "ready" && !user) {
      setActiveReply(null);
      setPendingReply(null);
    }
  }, [authStatus, user]);

  const openAuth = useCallback((mode = "login") => {
    setAuthModal({ open: true, mode });
  }, []);

  const closeAuth = useCallback(() => {
    setAuthModal({ open: false, mode: "login" });
    setPendingReply(null);
  }, []);

  const handleAuthenticated = useCallback(
    (authenticatedUser, mode) => {
      setAuthModal({ open: false, mode: "login" });
      notify(
        mode === "register"
          ? `欢迎加入，${authenticatedUser.username} ✦`
          : `欢迎回来，${authenticatedUser.username} ✦`,
      );

      if (pendingReply) {
        const nextReply = pendingReply;
        setPendingReply(null);
        setActiveReply(nextReply);
        window.requestAnimationFrame(() => {
          document
            .querySelector(`#message-${nextReply.messageId}`)
            ?.scrollIntoView({ behavior: "smooth", block: "center" });
        });
      }
    },
    [notify, pendingReply],
  );

  const handleCreateMessage = useCallback(
    async (payload) => {
      const created = await createMessage(payload);
      setSort("newest");
      setQuery("");
      await goToPage(1);
      notify("留言已发布，愿你的声音被听见 ✦");
      return created;
    },
    [createMessage, goToPage, notify],
  );

  const handleCreateReply = useCallback(
    async (messageId, payload) => {
      const targetName = activeReply?.replyToName;
      const reply = await createReply(messageId, payload);
      setActiveReply(null);
      notify(
        targetName
          ? `已回复 @${targetName}，谢谢你的回应 ✦`
          : "回复已送出，谢谢你的回应 ✦",
      );
      return reply;
    },
    [activeReply?.replyToName, createReply, notify],
  );

  const handleOpenReply = useCallback(
    (message, reply) => {
      if (authStatus === "loading") {
        notify("正在确认登录状态，请稍候");
        return;
      }
      if (!user) {
        setPendingReply({
          messageId: message.id,
          parentReplyId: reply?.id || null,
          replyToName: reply?.userName || null,
        });
        openAuth("login");
        notify(reply ? "登录后即可回复这条消息" : "登录后即可参与回复");
        return;
      }

      setActiveReply({
        messageId: message.id,
        parentReplyId: reply?.id || null,
        replyToName: reply?.userName || null,
      });
    },
    [authStatus, notify, openAuth, user],
  );

  const handleDeleteMessage = useCallback((message) => {
    setDeleteError("");
    setConfirmState({
      kind: "message",
      message,
      title: "删除这条留言？",
      description:
        message.replyCount > 0
          ? `这条留言及其下 ${message.replyCount} 条回复将被一并删除，且无法恢复。`
          : "这条留言将被永久删除，且无法恢复。",
      confirmLabel: "删除留言",
    });
  }, []);

  const handleDeleteReply = useCallback((message, reply) => {
    setDeleteError("");
    setConfirmState({
      kind: "reply",
      message,
      reply,
      title: "删除这条回复？",
      description:
        reply.childReplyCount > 0
          ? "这条回复及其下的所有子回复将被一并删除，且无法恢复。"
          : "这条回复将被永久删除，且无法恢复。",
      confirmLabel: "删除回复",
    });
  }, []);

  const handleCloseConfirm = useCallback(() => {
    if (deleting) return;
    setConfirmState(null);
    setDeleteError("");
  }, [deleting]);

  const handleConfirmDelete = useCallback(async () => {
    if (!confirmState || deleting) return;
    setDeleting(true);
    setDeleteError("");

    try {
      if (confirmState.kind === "message") {
        const result = await deleteMessage(confirmState.message.id);
        if (activeReply?.messageId === confirmState.message.id) setActiveReply(null);
        if (pendingReply?.messageId === confirmState.message.id) setPendingReply(null);
        notify(
          result.deletedReplyCount > 0
            ? `留言已删除，同时移除了 ${result.deletedReplyCount} 条回复`
            : "留言已删除",
        );
      } else {
        const result = await deleteReply(
          confirmState.message.id,
          confirmState.reply.id,
        );
        if (activeReply?.parentReplyId === confirmState.reply.id) {
          setActiveReply(null);
        }
        notify(
          result.deletedReplyCount > 1
            ? `回复已删除，同时移除了 ${result.deletedReplyCount - 1} 条子回复`
            : "回复已删除",
        );
      }
      setConfirmState(null);
    } catch (deleteRequestError) {
      setDeleteError(
        deleteRequestError instanceof Error
          ? deleteRequestError.message
          : "删除失败，请稍后重试",
      );
    } finally {
      setDeleting(false);
    }
  }, [
    activeReply?.messageId,
    activeReply?.parentReplyId,
    confirmState,
    deleteMessage,
    deleteReply,
    deleting,
    notify,
    pendingReply?.messageId,
  ]);

  const handlePageChange = useCallback(
    async (nextPage) => {
      setActiveReply(null);
      await goToPage(nextPage);
      window.requestAnimationFrame(() => {
        document
          .querySelector("#wall")
          ?.scrollIntoView({ behavior: "smooth", block: "start" });
      });
    },
    [goToPage],
  );

  const handleLogout = useCallback(async () => {
    await logout();
    setActiveReply(null);
    setPendingReply(null);
    notify("已退出登录");
  }, [logout, notify]);

  const handleEmptyAction = useCallback(() => {
    if (query) {
      setQuery("");
      return;
    }
    document.querySelector("#composer")?.scrollIntoView({ behavior: "smooth", block: "center" });
    window.setTimeout(() => {
      const target =
        document.querySelector("#content-input") ||
        document.querySelector(".composer-auth-button");
      target?.focus({ preventScroll: true });
    }, 380);
  }, [query]);

  const composerDisabled = status === "loading" || status === "error";
  const stats =
    status === "error"
      ? EMPTY_STATS
      : {
          messages: summary.messageCount,
          replies: summary.replyCount,
          today: summary.todayCount,
        };

  return (
    <>
      <div className="page-texture" aria-hidden="true" />
      <a className="skip-link" href="#wall">跳到留言墙</a>
      <SiteHeader
        user={user}
        authStatus={authStatus}
        onOpenAuth={openAuth}
        onLogout={handleLogout}
      />
      <main>
        <Hero
          stats={stats}
          onCreateMessage={handleCreateMessage}
          composerDisabled={composerDisabled}
          user={user}
          authStatus={authStatus}
          onRequireAuth={openAuth}
        />
        <WallSection
          messages={messages}
          page={page}
          totalPages={totalPages}
          total={status === "ready" ? total : "—"}
          status={status}
          error={error}
          query={query}
          sort={sort}
          currentUser={user}
          activeReply={activeReply}
          replyThreads={replyThreads}
          onQueryChange={setQuery}
          onSortChange={setSort}
          onRetry={refresh}
          onPageChange={handlePageChange}
          onEmptyAction={handleEmptyAction}
          onOpenReply={handleOpenReply}
          onCancelReply={() => setActiveReply(null)}
          onCreateReply={handleCreateReply}
          onDeleteMessage={handleDeleteMessage}
          onDeleteReply={handleDeleteReply}
          onLoadReplies={loadReplies}
          onLoadMoreReplies={loadMoreReplies}
          onRequireAuth={handleOpenReply}
        />
      </main>
      <SiteFooter />

      <AuthModal
        open={authModal.open}
        initialMode={authModal.mode}
        onClose={closeAuth}
        onLogin={login}
        onRegister={register}
        onSuccess={handleAuthenticated}
      />

      <ConfirmDialog
        open={Boolean(confirmState)}
        title={confirmState?.title}
        description={confirmState?.description}
        confirmLabel={confirmState?.confirmLabel}
        busy={deleting}
        error={deleteError}
        onConfirm={handleConfirmDelete}
        onCancel={handleCloseConfirm}
      />

      <Toast message={toast} />
    </>
  );
}

export default function App() {
  return (
    <AuthProvider api={guestbookApi}>
      <GuestbookPage />
    </AuthProvider>
  );
}
