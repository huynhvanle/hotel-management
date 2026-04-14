const CTX = "/hotel-management";

function qs(sel) {
  return document.querySelector(sel);
}

function getToken() {
  return localStorage.getItem("hm_admin_token");
}

function clearToken() {
  localStorage.removeItem("hm_admin_token");
}

function normalizeRole(position) {
  return (position || "").toUpperCase() === "ADMIN" ? "ADMIN" : "USER";
}

async function api(path, opts = {}) {
  const headers = new Headers(opts.headers || {});
  headers.set("Content-Type", "application/json");
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(`${CTX}${path}`, { ...opts, headers });
  const isJson = (res.headers.get("content-type") || "").includes("application/json");
  const payload = isJson ? await res.json() : await res.text();
  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    if (typeof payload === "string") msg = payload || msg;
    else if (payload && typeof payload === "object") {
      msg = payload.message || payload.error || msg;
      if (payload.errors && typeof payload.errors === "object") {
        const details = Object.entries(payload.errors)
          .map(([k, v]) => `${k}: ${v}`)
          .join(", ");
        msg = details ? `${msg} (${details})` : msg;
      }
    }
    throw new Error(msg);
  }
  return payload;
}

async function loadMeSafe() {
  try {
    return await api("/me");
  } catch {
    clearToken();
    window.location.href = "../admin/login.html";
    return null;
  }
}

function wireLogout() {
  const btn = qs("#btnLogout");
  btn?.addEventListener("click", () => {
    clearToken();
    window.location.href = "../admin/login.html";
  });
}

function renderDashboardMe(me) {
  const roleText = qs("#roleText");
  const meBox = qs("#meBox");
  if (!roleText && !meBox) return;
  if (!me) return;
  const role = normalizeRole(me.position);
  if (roleText) roleText.textContent = `Bạn đã đăng nhập với vai trò: ${role}.`;
  if (meBox) meBox.textContent = `User: ${me.username || ""} · Mail: ${me.mail || ""}`;
}

document.addEventListener("DOMContentLoaded", async () => {
  wireLogout();
  if (!getToken()) {
    window.location.href = "../admin/login.html";
    return;
  }
  const me = await loadMeSafe();
  if (!me) return;
  // If ADMIN lands on staff area, send them to admin dashboard.
  if (normalizeRole(me.position) === "ADMIN") {
    window.location.href = "../admin/index.html";
    return;
  }
  renderDashboardMe(me);
});

