import * as mock from "./data/mockData";

// If VITE_API_URL is empty, every call returns sample data so the app
// runs with no backend. Set it in .env to talk to the Java API instead.
const BASE = import.meta.env.VITE_API_URL;

async function request(path, options, fallback) {
  if (!BASE) return fallback;
  const token = localStorage.getItem("token");
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || res.statusText);
  return res.status === 204 ? null : res.json();
}

export const api = {
  stats: () => request("/api/stats", {}, mock.stats),
  usage: () => request("/api/usage", {}, mock.usage),
  documents: () => request("/api/documents", {}, mock.documents),
  chats: () => request("/api/chats", {}, mock.chats),
  messages: (id) => request(`/api/chats/${id}/messages`, {}, mock.messages),
  send: (id, text) =>
    request(`/api/chats/${id}/messages`, { method: "POST", body: JSON.stringify({ text }) }, {
      id: crypto.randomUUID(), role: "assistant", kind: "synthesis",
      body: "Placeholder answer. Connect the backend (VITE_API_URL) to get answers from your documents.",
    }),
  flashcards: () => request("/api/flashcards", {}, mock.flashcards),
};
