const CTX = "/hotel-management";

function qs(sel) {
  return document.querySelector(sel);
}

function qsa(sel) {
  return Array.from(document.querySelectorAll(sel));
}

function getToken() {
  return localStorage.getItem("hm_admin_token");
}

function setToken(token) {
  localStorage.setItem("hm_admin_token", token);
}

function clearToken() {
  localStorage.removeItem("hm_admin_token");
}

function isLoginPage() {
  return window.location.pathname.endsWith("/admin/login.html");
}

function requireAuth() {
  if (isLoginPage()) return;
  if (!getToken()) window.location.href = "./login.html";
}

function isAdminPage() {
  return window.location.pathname.includes("/admin/");
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
    const msg = payload?.message || payload || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return payload;
}

async function loadMe() {
  return await api("/me");
}

function normalizeRole(position) {
  return (position || "").toUpperCase() === "ADMIN" ? "ADMIN" : "USER";
}

async function loadMeSafe() {
  try {
    return await loadMe();
  } catch (e) {
    // token invalid/expired
    clearToken();
    window.location.href = "./login.html";
    return null;
  }
}

function isUsersPage() {
  return window.location.pathname.endsWith("/admin/users.html");
}

function isHotelsPage() {
  return window.location.pathname.endsWith("/admin/hotels.html");
}

async function enforceUsersAdminOnly(role) {
  if (isLoginPage()) return;
  if (!isAdminPage()) return;
  if (!isUsersPage()) return;
  if (role !== "ADMIN") {
    // USER should not manage users
    window.location.href = "./index.html";
    return false;
  }
  return true;
}

async function enforceHotelsAdminOnly(role) {
  if (isLoginPage()) return;
  if (!isAdminPage()) return;
  if (!isHotelsPage()) return;
  if (role !== "ADMIN") {
    // USER should not manage hotels
    window.location.href = "./index.html";
    return false;
  }
  return true;
}

function hideUsersLinksForUser(role) {
  if (role !== "USER") return;
  // Navbar link
  qsa('a[href="./users.html"], a[href="users.html"]').forEach((el) => {
    const li = el.closest("li");
    if (li) li.remove();
    else el.remove();
  });
  // Quick link button on dashboard
  qsa('a[href="./users.html"], a[href="users.html"]').forEach((el) => el.remove());
}

function hideHotelsLinksForUser(role) {
  if (role !== "USER") return;
  qsa('a[href="./hotels.html"], a[href="hotels.html"]').forEach((el) => {
    const li = el.closest("li");
    if (li) li.remove();
    else el.remove();
  });
}

async function enforceAdminPageAuthAndRole() {
  if (isLoginPage()) return null;
  // Only run in /admin/* pages
  if (!isAdminPage()) return null;
  try {
    const me = await loadMeSafe();
    if (!me) return null;
    const role = normalizeRole(me.position);
    hideUsersLinksForUser(role);
    hideHotelsLinksForUser(role);
    const ok = await enforceUsersAdminOnly(role);
    if (!ok) return null;
    const okHotels = await enforceHotelsAdminOnly(role);
    if (!okHotels) return null;
    return { me, role };
  } catch {
    return null;
  }
}

function wireLogout() {
  const btn = qs("#btnLogout");
  if (!btn) return;
  btn.addEventListener("click", () => {
    clearToken();
    window.location.href = "./login.html";
  });
}

async function initLogin() {
  const form = qs("#loginForm");
  if (!form) return;

  const alertBox = qs("#loginAlert");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    alertBox.classList.add("d-none");
    const fd = new FormData(form);
    const payload = {
      username: fd.get("username"),
      password: fd.get("password"),
    };
    try {
      const res = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setToken(res.accessToken);
      // Redirect based on role
      const me = await loadMeSafe();
      const role = normalizeRole(me?.position);
      window.location.href = role === "ADMIN" ? "./index.html" : "../staff/index.html";
    } catch (err) {
      alertBox.textContent = `Login failed: ${err.message}`;
      alertBox.classList.remove("d-none");
    }
  });
}

function renderUserRow(u) {
  const role = normalizeRole(u.position);
  const canDelete = role === "USER";
  return `
    <tr>
      <td>${u.id ?? ""}</td>
      <td>${u.username ?? ""}</td>
      <td>${u.fullName ?? ""}</td>
      <td>${role}</td>
      <td>${u.mail ?? ""}</td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-primary" data-edit-user="${u.id}">Edit</button>
        <button class="btn btn-sm btn-outline-danger ms-1" data-delete-user="${u.id}" ${canDelete ? "" : "disabled"}>Delete</button>
      </td>
    </tr>
  `;
}

async function initUsers() {
  const tbody = qs("#usersTbody");
  if (!tbody) return;

  const alertBox = qs("#usersAlert");
  const btnReload = qs("#btnReload");
  const btnAddUser = qs("#btnAddUser");

  const modalEl = qs("#editModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const editForm = qs("#editForm");
  const editAlert = qs("#editAlert");
  const btnSave = qs("#btnSave");

  const createModalEl = qs("#createModal");
  const createModal = createModalEl ? new bootstrap.Modal(createModalEl) : null;
  const createForm = qs("#createForm");
  const createAlert = qs("#createAlert");
  const btnCreate = qs("#btnCreate");

  async function load() {
    alertBox.classList.add("d-none");
    tbody.innerHTML = "";
    try {
      const res = await api("/user");
      const users = res.users || [];
      tbody.innerHTML = users.map(renderUserRow).join("");
    } catch (e) {
      alertBox.textContent = e.message;
      alertBox.classList.remove("d-none");
    }
  }

  btnReload?.addEventListener("click", load);

  btnAddUser?.addEventListener("click", () => {
    createAlert?.classList.add("d-none");
    createForm?.reset?.();
    if (createForm) {
      createForm.elements.namedItem("position").value = "USER";
    }
    createModal?.show();
  });

  tbody.addEventListener("click", async (e) => {
    const btn = e.target?.closest?.("[data-edit-user]");
    const del = e.target?.closest?.("[data-delete-user]");
    if (btn) {
      const id = btn.getAttribute("data-edit-user");
      editAlert.classList.add("d-none");
      try {
        const res = await api(`/user/${id}`);
        const u = res.user;
        editForm.elements.namedItem("id").value = u.id;
        editForm.elements.namedItem("username").value = u.username || "";
        editForm.elements.namedItem("fullName").value = u.fullName || "";
        editForm.elements.namedItem("position").value = normalizeRole(u.position);
        editForm.elements.namedItem("mail").value = u.mail || "";
        editForm.elements.namedItem("description").value = u.description || "";
        editForm.elements.namedItem("password").value = "";
        modal?.show();
      } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove("d-none");
      }
      return;
    }

    if (del) {
      const id = del.getAttribute("data-delete-user");
      if (!id) return;
      const row = del.closest("tr");
      const username = row?.children?.[1]?.textContent || "";
      const role = row?.children?.[3]?.textContent || "";
      if ((role || "").toUpperCase() !== "USER") {
        alertBox.textContent = "Chỉ được phép xoá user có vai trò USER.";
        alertBox.classList.remove("d-none");
        return;
      }
      if (!confirm(`Xoá user "${username}" (ID=${id})?`)) return;
      alertBox.classList.add("d-none");
      try {
        await api(`/user/${id}`, { method: "DELETE" });
        await load();
      } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove("d-none");
      }
    }
  });

  btnSave?.addEventListener("click", async () => {
    editAlert.classList.add("d-none");
    const fd = new FormData(editForm);
    const id = fd.get("id");
    const payload = {
      username: fd.get("username") || null,
      fullName: fd.get("fullName") || null,
      position: fd.get("position") || null,
      mail: fd.get("mail") || null,
      description: fd.get("description") || null,
      password: fd.get("password") || null,
    };
    try {
      await api(`/user/${id}`, { method: "PUT", body: JSON.stringify(payload) });
      modal?.hide();
      await load();
    } catch (err) {
      editAlert.textContent = err.message;
      editAlert.classList.remove("d-none");
    }
  });

  btnCreate?.addEventListener("click", async () => {
    if (!createForm) return;
    createAlert.classList.add("d-none");
    const fd = new FormData(createForm);
    const payload = {
      username: fd.get("username"),
      password: fd.get("password"),
      fullName: fd.get("fullName"),
      mail: fd.get("mail"),
      position: fd.get("position"),
      description: fd.get("description") || null,
    };
    try {
      await api("/user", { method: "POST", body: JSON.stringify(payload) });
      createModal?.hide();
      await load();
    } catch (err) {
      createAlert.textContent = err.message;
      createAlert.classList.remove("d-none");
    }
  });

  await load();
}

function initDashboardMe(me) {
  const roleText = qs("#roleText");
  const meBox = qs("#meBox");
  if (!roleText && !meBox) return;
  if (!me) return;
  const role = normalizeRole(me.position);
  if (roleText) roleText.textContent = `Bạn đã đăng nhập với vai trò: ${role}.`;
  if (meBox) meBox.textContent = `User: ${me.username || ""} · Mail: ${me.mail || ""}`;
}

document.addEventListener("DOMContentLoaded", async () => {
  requireAuth();
  wireLogout();
  await initLogin();
  const auth = await enforceAdminPageAuthAndRole();
  if (auth?.me) initDashboardMe(auth.me);
  await initUsers();
});

