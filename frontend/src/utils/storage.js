import { AUTH_TOKEN_STORAGE_KEY } from "../constants.js";

export function getAuthToken() {
  try {
    return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) || "";
  } catch (error) {
    return "";
  }
}

export function setAuthToken(token) {
  try {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
  } catch (error) {
    // The in-memory session still works when browser storage is unavailable.
  }
}

export function clearAuthToken() {
  try {
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  } catch (error) {
    // Ignore storage failures during logout.
  }
}
