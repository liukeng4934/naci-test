import { createHttpGuestbookApi } from "./httpGuestbookApi.js";

const baseUrl = import.meta.env.VITE_API_BASE_URL || "/api";

export const guestbookApi = createHttpGuestbookApi(baseUrl);
