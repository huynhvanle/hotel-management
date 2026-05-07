const CTX = "/hotel-management";
const STAFF_TOKEN_KEY = "hm_staff_token";

function qs(sel) {
  return document.querySelector(sel);
}

function getToken() {
  return localStorage.getItem(STAFF_TOKEN_KEY);
}

function setToken(t) {
  localStorage.setItem(STAFF_TOKEN_KEY, t);
}

function clearToken() {
  localStorage.removeItem(STAFF_TOKEN_KEY);
}

async function api(path, opts = {}) {
  const headers = { ...(opts.headers || {}) };
  const t = getToken();
  if (t) headers["Authorization"] = `Bearer ${t}`;
  if (opts.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const res = await fetch(`${CTX}${path}`, { ...opts, headers });
  const isJson = (res.headers.get("content-type") || "").includes("application/json");
  const payload = isJson ? await res.json() : await res.text();
  if (!res.ok) {
    const msg = (payload && payload.message) || payload || `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return payload;
}

function requireLoginOrRedirect() {
  if (getToken()) return true;
  if (!window.location.pathname.endsWith("/admin/login.html")) {
    window.location.href = "./login.html";
  }
  return false;
}

function bootLogout() {
  const btn = qs("#btnLogout");
  btn?.addEventListener("click", () => {
    clearToken();
    window.location.href = "./login.html";
  });
}

function bootBackHome() {
  const btn = qs("#btnBack");
  btn?.addEventListener("click", () => {
    window.location.href = "./index.html";
  });
}

async function initLogin() {
  const form = qs("#staffLoginForm");
  if (!form) return;
  const alertBox = qs("#loginAlert");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    alertBox.classList.add("d-none");
    const fd = new FormData(form);
    const payload = { username: fd.get("username"), password: fd.get("password") };
    try {
      const res = await api("/auth/login", { method: "POST", body: JSON.stringify(payload) });
      setToken(res.accessToken);
      window.location.href = "./index.html";
    } catch (err) {
      alertBox.textContent = err.message || String(err);
      alertBox.classList.remove("d-none");
    }
  });
}

async function initReceptionHome() {
  const roleText = qs("#roleText");
  const meBox = qs("#meBox");
  const receptionLinks = qs("#receptionLinks");
  const managerLinks = qs("#managerLinks");
  const adminLinks = qs("#adminLinks");
  if (!roleText && !meBox) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  try {
    const me = await api("/me");
    const role = me?.role || me?.position || "—";
    if (roleText) roleText.textContent = `Quyền hiện tại: ${role}`;
    if (meBox) {
      const name = me?.fullName || "—";
      const username = me?.username || "—";
      meBox.textContent = `Tài khoản: ${username} · ${name}`;
    }
    // Phân quyền hiển thị chức năng sau khi đăng nhập.
    const r = String(role || "").toUpperCase();
    const canReception = r === "RECEPTIONIST";
    const canManager = r === "BRANCH_MANAGER";
    const canAdmin = r === "ADMIN";
    if (receptionLinks) receptionLinks.classList.toggle("d-none", !canReception);
    if (managerLinks) managerLinks.classList.toggle("d-none", !canManager);
    if (adminLinks) adminLinks.classList.toggle("d-none", !canAdmin);
  } catch (e) {
    if (roleText) roleText.textContent = `Không tải được quyền hiện tại: ${e.message}`;
  }
}

function statusViRoom(st) {
  switch (String(st || "")) {
    case "AVAILABLE": return "Có sẵn";
    case "UNAVAILABLE": return "Không có sẵn";
    case "MAINTENANCE": return "Bảo trì";
    default: return st || "—";
  }
}

function fmtMoney(v) {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "—";
  return new Intl.NumberFormat("vi-VN").format(Number(v)) + " ₫";
}

let __revChart = null;

async function initBranchRooms() {
  const tbody = qs("#roomTbody");
  if (!tbody) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const branchHint = qs("#branchHint");
  const form = qs("#searchForm");
  const btnAdd = qs("#btnAddRoom");
  const btnEdit = qs("#btnEditRoom");
  const btnDel = qs("#btnDeleteRoom");

  const modalEl = qs("#roomModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = qs("#roomModalTitle");
  const modalAlert = qs("#roomModalAlert");
  const roomForm = qs("#roomForm");
  const btnSave = qs("#btnSaveRoom");
  const roomTypeSelect = roomForm?.elements?.namedItem("roomTypeId");

  const deleteModalEl = qs("#deleteModal");
  const deleteModal = deleteModalEl ? new bootstrap.Modal(deleteModalEl) : null;
  const deleteAlert = qs("#deleteAlert");
  const deleteStep1 = qs("#deleteStep1");
  const deleteRoomId1 = qs("#deleteRoomId1");
  const btnDeleteConfirm = qs("#btnDeleteConfirm");

  let me = null;
  let selectedId = null;
  let selectedStatus = null;
  let lastRows = [];
  let mode = "create"; // create | edit
  let roomTypes = [];
  let pendingDeleteId = null;

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalErr() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }

  function applySelectionState() {
    const canEditDelete = Boolean(selectedId) && String(selectedStatus) === "MAINTENANCE";
    if (btnEdit) btnEdit.disabled = !canEditDelete;
    if (btnDel) btnDel.disabled = !canEditDelete;
  }

  function renderRows(items) {
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-muted">Không có phòng.</td></tr>`;
      return;
    }
    tbody.innerHTML = items.map((r) => `
      <tr data-room-id="${String(r.id || "").replace(/"/g, "&quot;")}" role="button" style="cursor:pointer">
        <td><b>${r.id || "—"}</b></td>
        <td class="text-muted">${r.floor ?? "—"}</td>
        <td class="text-muted">${r.roomTypeName || "—"} <span class="small text-muted">(ID ${r.roomTypeId ?? "—"})</span></td>
        <td><span class="fw-semibold">${statusViRoom(r.status)}</span></td>
        <td class="text-end"><b>${fmtMoney(r.price)}</b></td>
      </tr>
    `).join("");
  }

  function renderRoomTypeOptions() {
    if (!roomTypeSelect) return;
    const current = String(roomTypeSelect.value || "");
    const active = roomTypes.filter((x) => String(x.status || "") === "ACTIVE");
    const options = active.length ? active : roomTypes;
    roomTypeSelect.innerHTML = [
      `<option value="">— Chọn loại phòng —</option>`,
      ...options.map((rt) => {
        const label = `${rt.name || "—"} · ${fmtMoney(rt.basePrice)}${rt.status && rt.status !== "ACTIVE" ? ` · ${rt.status}` : ""}`;
        return `<option value="${String(rt.id).replace(/\"/g, "&quot;")}">${label.replace(/</g, "&lt;").replace(/>/g, "&gt;")}</option>`;
      }),
    ].join("");
    if (current) roomTypeSelect.value = current;
  }

  async function loadRoomTypes() {
    roomTypes = await api("/api/room-types");
    if (!Array.isArray(roomTypes)) roomTypes = [];
    roomTypes.sort((a, b) => String(a?.name || "").localeCompare(String(b?.name || ""), "vi"));
    renderRoomTypeOptions();
  }

  async function loadMe() {
    me = await api("/me");
    const role = String(me?.role || "").toUpperCase();
    if (!["BRANCH_MANAGER", "ADMIN"].includes(role)) throw new Error("Tài khoản không có quyền quản lí phòng.");
    if (branchHint) {
      const bname = me?.branchName || "—";
      branchHint.textContent = `Chi nhánh: ${bname}`;
    }
  }

  async function loadRooms() {
    clearErr();
    const fd = form ? new FormData(form) : null;
    const roomNumber = fd ? (fd.get("roomNumber") || "").toString().trim() : "";
    const floor = fd ? (fd.get("floor") || "").toString().trim() : "";
    const type = fd ? (fd.get("type") || "").toString().trim() : "";
    const q = new URLSearchParams();
    if (roomNumber) q.set("roomNumber", roomNumber);
    if (floor) q.set("floor", floor);
    if (type) q.set("type", type);
    // hotelId: chỉ cần khi ADMIN. Branch manager tự suy ra từ token.
    if (String(me?.role || "").toUpperCase() === "ADMIN") {
      if (!me?.branchId) throw new Error("Admin cần branchId để quản lí phòng (tạm thời).");
      q.set("hotelId", String(me.branchId));
    }
    const items = await api(`/api/branch/rooms${q.toString() ? `?${q}` : ""}`);
    lastRows = Array.isArray(items) ? items : [];
    selectedId = null;
    selectedStatus = null;
    applySelectionState();
    renderRows(lastRows);
  }

  tbody.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-room-id]");
    if (!tr) return;
    const id = tr.getAttribute("data-room-id");
    const it = lastRows.find((x) => String(x.id) === String(id));
    if (!it) return;
    selectedId = String(it.id);
    selectedStatus = String(it.status || "");
    tbody.querySelectorAll("tr[data-room-id]").forEach((row) => row.classList.remove("table-active"));
    tr.classList.add("table-active");
    applySelectionState();
  });

  btnAdd?.addEventListener("click", () => {
    mode = "create";
    selectedId = null;
    selectedStatus = null;
    applySelectionState();
    clearModalErr();
    if (modalTitle) modalTitle.textContent = "Thêm phòng mới";
    roomForm?.reset();
    const idEl = roomForm?.elements?.namedItem("id");
    if (idEl) {
      idEl.disabled = false;
      idEl.value = "";
    }
    renderRoomTypeOptions();
    modal?.show();
  });

  btnEdit?.addEventListener("click", () => {
    if (!selectedId) return;
    const it = lastRows.find((x) => String(x.id) === String(selectedId));
    if (!it) return;
    if (String(it.status) !== "MAINTENANCE") return;
    mode = "edit";
    clearModalErr();
    if (modalTitle) modalTitle.textContent = `Sửa phòng ${selectedId}`;
    const idEl = roomForm?.elements?.namedItem("id");
    const floorEl = roomForm?.elements?.namedItem("floor");
    const rtEl = roomForm?.elements?.namedItem("roomTypeId");
    if (idEl) {
      idEl.value = String(it.id || "");
      idEl.disabled = true;
    }
    if (floorEl) floorEl.value = String(it.floor ?? "");
    if (rtEl) rtEl.value = String(it.roomTypeId ?? "");
    renderRoomTypeOptions();
    modal?.show();
  });

  btnDel?.addEventListener("click", async () => {
    if (!selectedId) return;
    const it = lastRows.find((x) => String(x.id) === String(selectedId));
    if (!it) return;
    if (String(it.status) !== "MAINTENANCE") return;
    pendingDeleteId = String(selectedId);
    if (deleteAlert) {
      deleteAlert.classList.add("d-none");
      deleteAlert.textContent = "";
    }
    if (deleteRoomId1) deleteRoomId1.textContent = pendingDeleteId;
    deleteStep1?.classList.remove("d-none");
    btnDeleteConfirm?.classList.remove("d-none");
    deleteModal?.show();
  });

  function showDeleteErr(msg) {
    if (!deleteAlert) return;
    deleteAlert.textContent = msg;
    deleteAlert.classList.remove("d-none");
  }

  btnDeleteConfirm?.addEventListener("click", async () => {
    const id = String(pendingDeleteId || "").trim();
    if (!id) return;
    if (deleteAlert) {
      deleteAlert.classList.add("d-none");
      deleteAlert.textContent = "";
    }
    btnDeleteConfirm.disabled = true;
    try {
      const q = new URLSearchParams();
      if (String(me?.role || "").toUpperCase() === "ADMIN") q.set("hotelId", String(me.branchId || ""));
      await api(`/api/branch/rooms/${encodeURIComponent(id)}${q.toString() ? `?${q}` : ""}`, { method: "DELETE" });
      deleteModal?.hide();
      pendingDeleteId = null;
      await loadRooms();
    } catch (e) {
      showDeleteErr(e.message || String(e));
    } finally {
      btnDeleteConfirm.disabled = false;
    }
  });

  deleteModalEl?.addEventListener("hidden.bs.modal", () => {
    pendingDeleteId = null;
  });

  btnSave?.addEventListener("click", async () => {
    clearModalErr();
    const fd = new FormData(roomForm);
    // Note: when editing, the room id input is disabled so it's not included in FormData.
    const formId = (fd.get("id") || "").toString().trim();
    const finalId = mode === "edit" ? String(selectedId || formId || "").trim() : formId;
    const payload = {
      id: finalId,
      floor: Number(fd.get("floor")),
      status: "AVAILABLE", // create ignored; update will force MAINTENANCE on backend
      roomTypeId: Number(fd.get("roomTypeId")),
      hotelId: me?.branchId, // branch manager: branchId từ /me
    };
    if (!payload.id || !/^\d+$/.test(payload.id)) {
      showModalErr("Số phòng chỉ gồm chữ số (vd: 301, 102).");
      return;
    }
    if (!Number.isFinite(payload.floor) || payload.floor < 0) {
      showModalErr("Tầng không hợp lệ.");
      return;
    }
    if (!Number.isFinite(payload.roomTypeId) || payload.roomTypeId <= 0) {
      showModalErr("Vui lòng chọn loại phòng.");
      return;
    }
    if (!payload.hotelId) {
      showModalErr("Thiếu chi nhánh của tài khoản.");
      return;
    }

    btnSave.disabled = true;
    try {
      if (mode === "create") {
        await api("/api/branch/rooms", { method: "POST", body: JSON.stringify(payload) });
      } else {
        const id = selectedId || payload.id;
        await api(`/api/branch/rooms/${encodeURIComponent(String(id))}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await loadRooms();
    } catch (e) {
      showModalErr(e.message || String(e));
    } finally {
      btnSave.disabled = false;
    }
  });

  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await loadRooms();
    } catch (err) {
      showErr(err.message || String(err));
    }
  });

  try {
    await loadMe();
  } catch (e) {
    showErr(e.message || String(e));
    return;
  }
  try {
    await loadRoomTypes();
  } catch (e) {
    showErr(e.message || String(e));
    return;
  }

  // Auto load rooms on first view (no need to submit search).
  try {
    await loadRooms();
  } catch (e) {
    showErr(e.message || String(e));
  }
}

async function initBranchStaff() {
  const tbody = qs("#staffTbody");
  if (!tbody) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const branchHint = qs("#branchHint");
  const searchForm = qs("#staffSearchForm");
  const btnAdd = qs("#btnAddStaff");
  const btnEdit = qs("#btnEditStaff");
  const btnDel = qs("#btnDeleteStaff");

  const modalEl = qs("#staffModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = qs("#staffModalTitle");
  const modalAlert = qs("#staffModalAlert");
  const form = qs("#staffForm");
  const btnSave = qs("#btnSaveStaff");
  const passHint = qs("#staffPassHint");

  const delModalEl = qs("#staffDeleteModal");
  const delModal = delModalEl ? new bootstrap.Modal(delModalEl) : null;
  const delAlert = qs("#staffDeleteAlert");
  const delName = qs("#staffDeleteName");
  const btnConfirmDel = qs("#btnConfirmDeleteStaff");

  let me = null;
  let rows = [];
  let selected = null; // item
  let mode = "create"; // create | edit

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalErr() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }
  function showDelErr(msg) {
    if (!delAlert) return;
    delAlert.textContent = msg;
    delAlert.classList.remove("d-none");
  }
  function clearDelErr() {
    delAlert?.classList.add("d-none");
    if (delAlert) delAlert.textContent = "";
  }

  function applySelectionState() {
    const can = Boolean(selected && selected.id);
    if (btnEdit) btnEdit.disabled = !can;
    if (btnDel) btnDel.disabled = !can;
  }

  function renderTable() {
    if (!rows.length) {
      tbody.innerHTML = `<tr><td colspan="4" class="text-muted">Không có lễ tân.</td></tr>`;
      return;
    }
    tbody.innerHTML = rows
      .map((x) => `
        <tr data-staff-id="${x.id}" role="button" style="cursor:pointer">
          <td><b>#${x.id}</b></td>
          <td class="text-muted">${String(x.username || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td>${String(x.fullName || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-muted">${x.phone || "—"}</td>
        </tr>
      `)
      .join("");
  }

  async function loadMe() {
    me = await api("/me");
    const role = String(me?.role || "").toUpperCase();
    if (!["BRANCH_MANAGER", "ADMIN"].includes(role)) throw new Error("Tài khoản không có quyền quản lí nhân sự.");
    if (branchHint) branchHint.textContent = `Chi nhánh: ${me?.branchName || "—"}`;
  }

  async function loadList() {
    clearErr();
    const fd = searchForm ? new FormData(searchForm) : null;
    const id = fd ? (fd.get("id") || "").toString().trim() : "";
    const phone = fd ? (fd.get("phone") || "").toString().trim() : "";
    const fullName = fd ? (fd.get("fullName") || "").toString().trim() : "";
    const q = new URLSearchParams();
    if (id) q.set("id", id);
    if (phone) q.set("phone", phone);
    if (fullName) q.set("fullName", fullName);
    rows = await api(`/api/branch/receptionists${q.toString() ? `?${q.toString()}` : ""}`);
    if (!Array.isArray(rows)) rows = [];
    selected = null;
    applySelectionState();
    renderTable();
  }

  tbody.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-staff-id]");
    if (!tr) return;
    const id = Number(tr.getAttribute("data-staff-id"));
    const it = rows.find((r) => Number(r.id) === id);
    if (!it) return;
    selected = it;
    tbody.querySelectorAll("tr[data-staff-id]").forEach((r) => r.classList.remove("table-active"));
    tr.classList.add("table-active");
    applySelectionState();
  });

  btnAdd?.addEventListener("click", () => {
    mode = "create";
    selected = null;
    applySelectionState();
    clearModalErr();
    if (modalTitle) modalTitle.textContent = "Thêm lễ tân";
    form?.reset();
    const uEl = form?.elements?.namedItem("username");
    const pEl = form?.elements?.namedItem("password");
    if (uEl) uEl.disabled = false;
    if (pEl) pEl.required = true;
    if (passHint) passHint.textContent = "Mật khẩu tối thiểu 8 ký tự.";
    modal?.show();
  });

  btnEdit?.addEventListener("click", () => {
    if (!selected?.id) return;
    mode = "edit";
    clearModalErr();
    if (modalTitle) modalTitle.textContent = `Sửa lễ tân #${selected.id}`;
    const uEl = form?.elements?.namedItem("username");
    const pEl = form?.elements?.namedItem("password");
    const nEl = form?.elements?.namedItem("fullName");
    const phEl = form?.elements?.namedItem("phone");
    if (uEl) {
      uEl.value = selected.username || "";
      uEl.disabled = true;
    }
    if (nEl) nEl.value = selected.fullName || "";
    if (phEl) phEl.value = selected.phone || "";
    if (pEl) {
      pEl.value = "";
      pEl.required = false;
    }
    if (passHint) passHint.textContent = "Để trống nếu không đổi mật khẩu.";
    modal?.show();
  });

  btnSave?.addEventListener("click", async () => {
    clearModalErr();
    const fd = new FormData(form);
    const payload = {
      username: (fd.get("username") || "").toString().trim(),
      password: (fd.get("password") || "").toString(),
      fullName: (fd.get("fullName") || "").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
    };
    if (!payload.username) return showModalErr("Vui lòng nhập tên đăng nhập.");
    if (!payload.fullName) return showModalErr("Vui lòng nhập họ tên.");
    if (!/^\d{10}$/.test(payload.phone)) return showModalErr("Số điện thoại phải đúng 10 chữ số.");
    if (mode === "create" && (!payload.password || payload.password.trim().length < 8)) {
      return showModalErr("Mật khẩu phải có ít nhất 8 ký tự.");
    }
    if (mode === "edit" && payload.password && payload.password.trim() && payload.password.trim().length < 8) {
      return showModalErr("Mật khẩu phải có ít nhất 8 ký tự.");
    }

    btnSave.disabled = true;
    try {
      if (mode === "create") {
        await api("/api/branch/receptionists", { method: "POST", body: JSON.stringify(payload) });
      } else {
        const id = selected?.id;
        if (!id) throw new Error("Chưa chọn lễ tân.");
        await api(`/api/branch/receptionists/${encodeURIComponent(String(id))}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await loadList();
    } catch (e) {
      showModalErr(e.message || String(e));
    } finally {
      btnSave.disabled = false;
    }
  });

  btnDel?.addEventListener("click", () => {
    if (!selected?.id) return;
    clearDelErr();
    if (delName) delName.textContent = selected.username || `#${selected.id}`;
    delModal?.show();
  });

  btnConfirmDel?.addEventListener("click", async () => {
    if (!selected?.id) return;
    clearDelErr();
    btnConfirmDel.disabled = true;
    try {
      await api(`/api/branch/receptionists/${encodeURIComponent(String(selected.id))}`, { method: "DELETE" });
      delModal?.hide();
      await loadList();
    } catch (e) {
      showDelErr(e.message || String(e));
    } finally {
      btnConfirmDel.disabled = false;
    }
  });

  searchForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    await loadList();
  });

  await loadMe();
  await loadList();
}

async function initBranchReports() {
  const form = qs("#reportForm");
  if (!form) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const branchHint = qs("#branchHint");
  const emptyBox = qs("#emptyBox");
  const totalRevenue = qs("#totalRevenue");
  const rangeText = qs("#rangeText");
  const tbody = qs("#revTbody");
  const canvas = qs("#revChart");

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }

  try {
    const me = await api("/me");
    const role = String(me?.role || "").toUpperCase();
    if (!["BRANCH_MANAGER", "ADMIN"].includes(role)) throw new Error("Tài khoản không có quyền xem báo cáo.");
    if (branchHint) branchHint.textContent = `Chi nhánh: ${me?.branchName || "—"}`;
  } catch (e) {
    showErr(e.message || String(e));
    return;
  }

  function renderTable(lines) {
    if (!tbody) return;
    if (!lines.length) {
      tbody.innerHTML = `<tr><td colspan="3" class="text-muted">Không có dữ liệu.</td></tr>`;
      return;
    }
    tbody.innerHTML = lines
      .map((x) => `
        <tr>
          <td>${String(x.label || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-end">${Number(x.invoiceCount || 0)}</td>
          <td class="text-end"><b>${fmtMoney(x.revenue)}</b></td>
        </tr>
      `)
      .join("");
  }

  function renderChart(lines) {
    if (!canvas || typeof window.Chart === "undefined") return;
    const labels = lines.map((x) => x.label);
    const data = lines.map((x) => Number(x.revenue || 0));
    if (__revChart) {
      __revChart.destroy();
      __revChart = null;
    }
    __revChart = new window.Chart(canvas, {
      type: "bar",
      data: {
        labels,
        datasets: [
          {
            label: "Doanh thu",
            data,
            backgroundColor: "#c9a227",
            borderColor: "#000000",
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${fmtMoney(ctx.parsed.y)}`,
            },
          },
        },
        scales: {
          x: { ticks: { color: "#000" } },
          y: { ticks: { color: "#000" } },
        },
      },
    });
  }

  function isoTomorrowFrom(isoDate) {
    const base = isoDate || new Date().toISOString().slice(0, 10);
    const d = new Date(`${base}T12:00:00`);
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 10);
  }

  // default: last 7 days
  const today = new Date().toISOString().slice(0, 10);
  const fromEl = form.elements.namedItem("from");
  const toEl = form.elements.namedItem("to");
  if (toEl && !toEl.value) toEl.value = today;
  if (fromEl && !fromEl.value) {
    const d = new Date(`${today}T12:00:00`);
    d.setDate(d.getDate() - 6);
    fromEl.value = d.toISOString().slice(0, 10);
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearErr();
    const fd = new FormData(form);
    const from = (fd.get("from") || "").toString().trim();
    const to = (fd.get("to") || "").toString().trim();
    const groupBy = (fd.get("groupBy") || "DAY").toString().trim();
    if (!from || !to) return showErr("Vui lòng chọn khoảng thời gian.");
    const q = new URLSearchParams({ from, to, groupBy });
    const urlParams = new URLSearchParams(window.location.search || "");
    const branchId = (urlParams.get("branchId") || "").trim();
    if (branchId) q.set("branchId", branchId);
    try {
      const res = await api(`/api/branch/reports/revenue?${q.toString()}`);
      const lines = Array.isArray(res?.lines) ? res.lines : [];
      const total = res?.totalRevenue;
      if (totalRevenue) totalRevenue.textContent = fmtMoney(total);
      if (rangeText) rangeText.textContent = `${from} → ${to} · Nhóm theo ${groupBy}`;
      if (branchHint && (res?.branchName || res?.branchId)) {
        branchHint.textContent = `Chi nhánh: ${res.branchName || "—"}${res.branchId ? ` (ID ${res.branchId})` : ""}`;
      }
      emptyBox?.classList.toggle("d-none", lines.length > 0);
      renderTable(lines);
      renderChart(lines);
    } catch (err) {
      showErr(err.message || String(err));
    }
  });
}

async function initAdminDashboard() {
  const form = qs("#dashForm");
  if (!form) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const totalEl = qs("#totalRevenue");
  const rankTbody = qs("#rankTbody");
  const compareSel = qs("#compareSelect");
  const canvas = qs("#growthChart");
  let growthChart = null;

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }

  try {
    const me = await api("/me");
    if (String(me?.role || "").toUpperCase() !== "ADMIN") throw new Error("Chỉ Admin mới được xem dashboard.");
  } catch (e) {
    showErr(e.message || String(e));
    return;
  }

  // Load branches for compare selection.
  try {
    const branches = await api("/api/admin/hotels");
    const items = Array.isArray(branches) ? branches : [];
    if (compareSel) {
      compareSel.innerHTML = items
        .map((b) => `<option value="${b.id}">${String(b.name || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</option>`)
        .join("");
    }
  } catch (e) {
    showErr(e.message || String(e));
  }

  // Default current month
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const first = `${yyyy}-${mm}-01`;
  const today = new Date().toISOString().slice(0, 10);
  const fromEl = form.elements.namedItem("from");
  const toEl = form.elements.namedItem("to");
  if (fromEl && !fromEl.value) fromEl.value = first;
  if (toEl && !toEl.value) toEl.value = today;

  function renderGrowth(lines) {
    const labels = lines.map((x) => x.label);
    const data = lines.map((x) => Number(x.revenue || 0));
    if (growthChart) {
      growthChart.destroy();
      growthChart = null;
    }
    if (!canvas || typeof window.Chart === "undefined") return;
    growthChart = new window.Chart(canvas, {
      type: "line",
      data: {
        labels,
        datasets: [
          {
            label: "Doanh thu",
            data,
            borderColor: "#c9a227",
            backgroundColor: "rgba(201, 162, 39, 0.2)",
            tension: 0.25,
          },
        ],
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (ctx) => ` ${fmtMoney(ctx.parsed.y)}` } },
        },
      },
    });
  }

  function renderRanking(items) {
    if (!rankTbody) return;
    if (!items.length) {
      rankTbody.innerHTML = `<tr><td colspan="3" class="text-muted">Không có dữ liệu.</td></tr>`;
      return;
    }
    rankTbody.innerHTML = items
      .map((x) => `
        <tr>
          <td><a href="./branch-reports.html?branchId=${encodeURIComponent(String(x.branchId))}" class="link-dark text-decoration-none fw-semibold">${String(x.branchName || ("Chi nhánh " + x.branchId)).replace(/</g, "&lt;").replace(/>/g, "&gt;")}</a></td>
          <td class="text-end">${Number(x.invoiceCount || 0)}</td>
          <td class="text-end"><b>${fmtMoney(x.revenue)}</b></td>
        </tr>
      `)
      .join("");
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearErr();
    const fd = new FormData(form);
    const from = (fd.get("from") || "").toString().trim();
    const to = (fd.get("to") || "").toString().trim();
    const groupBy = (fd.get("groupBy") || "MONTH").toString().trim();
    if (!from || !to) return showErr("Vui lòng chọn khoảng thời gian.");
    const q = new URLSearchParams({ from, to, groupBy });
    const selected = compareSel ? [...compareSel.selectedOptions].map((o) => o.value).filter(Boolean) : [];
    selected.forEach((id) => q.append("branchIds", id));
    try {
      const res = await api(`/api/admin/dashboard/revenue?${q.toString()}`);
      if (totalEl) totalEl.textContent = fmtMoney(res?.totalRevenue);
      renderGrowth(Array.isArray(res?.growth) ? res.growth : []);
      renderRanking(Array.isArray(res?.ranking) ? res.ranking : []);
    } catch (err) {
      showErr(err.message || String(err));
    }
  });

  // load default view
  form.dispatchEvent(new Event("submit", { cancelable: true }));
}

async function initAdminBranches() {
  const tbody = qs("#branchTbody");
  if (!tbody) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const btnAdd = qs("#btnAddBranch");

  const modalEl = qs("#branchModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = qs("#branchModalTitle");
  const modalAlert = qs("#branchModalAlert");
  const form = qs("#branchForm");
  const btnSave = qs("#btnSaveBranch");

  const chkAssign = qs("#chkAssignManager");
  const managerPickBox = qs("#managerPickBox");
  const managerQuery = qs("#managerQuery");
  const btnSearchManager = qs("#btnSearchManager");
  const managerTbody = qs("#managerTbody");
  const managerIdEl = qs("#managerId");

  const delModalEl = qs("#branchDeleteModal");
  const delModal = delModalEl ? new bootstrap.Modal(delModalEl) : null;
  const delAlert = qs("#branchDeleteAlert");
  const delName = qs("#branchDeleteName");
  const btnConfirmDel = qs("#btnConfirmDeleteBranch");

  let me = null;
  let rows = [];
  let editId = null;
  let pendingDelete = null;

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalErr() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }
  function showDelErr(msg) {
    if (!delAlert) return;
    delAlert.textContent = msg;
    delAlert.classList.remove("d-none");
  }
  function clearDelErr() {
    delAlert?.classList.add("d-none");
    if (delAlert) delAlert.textContent = "";
  }

  async function loadMe() {
    me = await api("/me");
    const role = String(me?.role || "").toUpperCase();
    if (role !== "ADMIN") throw new Error("Chỉ Admin mới được quản lí chi nhánh.");
  }

  function statusViBranch(st) {
    return String(st || "").toUpperCase() === "ACTIVE" ? "Hoạt động" : "Ngừng hoạt động";
  }

  function renderTable() {
    if (!rows.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-muted">Không có chi nhánh.</td></tr>`;
      return;
    }
    tbody.innerHTML = rows
      .map((h) => `
        <tr>
          <td><b>#${h.id}</b></td>
          <td>${String(h.name || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-muted">${String(h.address || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-muted">${h.phone || "—"}</td>
          <td><span class="fw-semibold">${statusViBranch(h.status)}</span></td>
          <td class="text-end">
            <button class="btn btn-sm btn-primary fw-semibold btn-edit-branch" data-id="${h.id}">Sửa</button>
            <button class="btn btn-sm btn-primary fw-semibold btn-del-branch" data-id="${h.id}">Xóa</button>
          </td>
        </tr>
      `)
      .join("");
  }

  async function loadList() {
    clearErr();
    rows = await api("/api/admin/hotels");
    if (!Array.isArray(rows)) rows = [];
    rows.sort((a, b) => Number(a.id) - Number(b.id));
    renderTable();
  }

  function setAssignBoxVisible(v) {
    if (!managerPickBox) return;
    managerPickBox.classList.toggle("d-none", !v);
  }

  async function searchManagers() {
    if (!managerTbody) return;
    const q = (managerQuery?.value || "").toString().trim();
    const res = await api(`/api/admin/managers${q ? `?q=${encodeURIComponent(q)}` : ""}`);
    const items = Array.isArray(res) ? res : [];
    if (!items.length) {
      managerTbody.innerHTML = `<tr><td colspan="5" class="text-muted">Không có kết quả.</td></tr>`;
      return;
    }
    managerTbody.innerHTML = items
      .map((m) => `
        <tr role="button" style="cursor:pointer" data-mid="${m.id}">
          <td>${m.id}</td>
          <td class="text-muted">${m.username || "—"}</td>
          <td>${m.fullName || "—"}</td>
          <td class="text-muted">${m.phone || "—"}</td>
          <td class="text-muted">${m.branchName || "—"}</td>
        </tr>
      `)
      .join("");
  }

  managerTbody?.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-mid]");
    if (!tr) return;
    const id = tr.getAttribute("data-mid");
    if (managerIdEl) managerIdEl.value = String(id || "");
  });

  chkAssign?.addEventListener("change", () => {
    setAssignBoxVisible(Boolean(chkAssign.checked));
  });
  btnSearchManager?.addEventListener("click", async () => {
    try {
      await searchManagers();
    } catch (e) {
      showModalErr(e.message || String(e));
    }
  });

  btnAdd?.addEventListener("click", () => {
    editId = null;
    clearModalErr();
    form?.reset();
    if (modalTitle) modalTitle.textContent = "Thêm chi nhánh mới";
    if (chkAssign) chkAssign.checked = false;
    setAssignBoxVisible(false);
    if (managerTbody) managerTbody.innerHTML = `<tr><td colspan="5" class="text-muted">—</td></tr>`;
    modal?.show();
  });

  tbody.addEventListener("click", async (e) => {
    const btnEdit = e.target.closest(".btn-edit-branch");
    const btnDel = e.target.closest(".btn-del-branch");
    const id = btnEdit?.getAttribute("data-id") || btnDel?.getAttribute("data-id");
    if (!id) return;
    const it = rows.find((x) => String(x.id) === String(id));
    if (!it) return;

    if (btnEdit) {
      editId = Number(it.id);
      clearModalErr();
      if (modalTitle) modalTitle.textContent = `Sửa chi nhánh #${it.id}`;
      form?.reset();
      const nEl = form?.elements?.namedItem("name");
      const aEl = form?.elements?.namedItem("address");
      const pEl = form?.elements?.namedItem("phone");
      const sEl = form?.elements?.namedItem("status");
      if (nEl) nEl.value = it.name || "";
      if (aEl) aEl.value = it.address || "";
      if (pEl) pEl.value = it.phone || "";
      if (sEl) sEl.value = String(it.status || "ACTIVE").toUpperCase();
      if (chkAssign) chkAssign.checked = false;
      setAssignBoxVisible(false);
      if (managerIdEl) managerIdEl.value = "";
      if (managerTbody) managerTbody.innerHTML = `<tr><td colspan="5" class="text-muted">—</td></tr>`;
      modal?.show();
    }

    if (btnDel) {
      pendingDelete = it;
      clearDelErr();
      if (delName) delName.textContent = it.name || `#${it.id}`;
      delModal?.show();
    }
  });

  btnSave?.addEventListener("click", async () => {
    clearModalErr();
    const fd = new FormData(form);
    const payload = {
      name: (fd.get("name") || "").toString().trim(),
      address: (fd.get("address") || "").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
      status: (fd.get("status") || "ACTIVE").toString().trim(),
      managerId: chkAssign?.checked ? Number(managerIdEl?.value || "") : null,
    };
    if (!payload.name) return showModalErr("Vui lòng nhập tên chi nhánh.");
    btnSave.disabled = true;
    try {
      if (!payload.managerId) delete payload.managerId;
      if (editId) {
        await api(`/api/admin/hotels/${encodeURIComponent(String(editId))}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        await api("/api/admin/hotels", { method: "POST", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await loadList();
    } catch (e) {
      showModalErr(e.message || String(e));
    } finally {
      btnSave.disabled = false;
    }
  });

  btnConfirmDel?.addEventListener("click", async () => {
    if (!pendingDelete?.id) return;
    clearDelErr();
    btnConfirmDel.disabled = true;
    try {
      await api(`/api/admin/hotels/${encodeURIComponent(String(pendingDelete.id))}`, { method: "DELETE" });
      delModal?.hide();
      pendingDelete = null;
      await loadList();
    } catch (e) {
      showDelErr(e.message || String(e));
    } finally {
      btnConfirmDel.disabled = false;
    }
  });

  await loadMe();
  await loadList();
}

async function initAdminManagers() {
  const tbody = qs("#mgrTbody");
  if (!tbody) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const searchForm = qs("#mgrSearchForm");
  const btnAdd = qs("#btnAddMgr");
  const btnEdit = qs("#btnEditMgr");
  const btnDel = qs("#btnDeleteMgr");

  const modalEl = qs("#mgrModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = qs("#mgrModalTitle");
  const modalAlert = qs("#mgrModalAlert");
  const form = qs("#mgrForm");
  const btnSave = qs("#btnSaveMgr");
  const passHint = qs("#mgrPassHint");
  const branchSel = form?.elements?.namedItem("branchId");

  const delModalEl = qs("#mgrDeleteModal");
  const delModal = delModalEl ? new bootstrap.Modal(delModalEl) : null;
  const delAlert = qs("#mgrDeleteAlert");
  const delName = qs("#mgrDeleteName");
  const btnConfirmDel = qs("#btnConfirmDeleteMgr");

  let rows = [];
  let selected = null;
  let mode = "create";
  let branches = [];

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalErr() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }
  function showDelErr(msg) {
    if (!delAlert) return;
    delAlert.textContent = msg;
    delAlert.classList.remove("d-none");
  }
  function clearDelErr() {
    delAlert?.classList.add("d-none");
    if (delAlert) delAlert.textContent = "";
  }

  async function requireAdmin() {
    const me = await api("/me");
    if (String(me?.role || "").toUpperCase() !== "ADMIN") throw new Error("Chỉ Admin mới được thao tác.");
  }

  function applySelectionState() {
    const can = Boolean(selected?.id);
    if (btnEdit) btnEdit.disabled = !can;
    if (btnDel) btnDel.disabled = !can;
  }

  function renderTable() {
    if (!rows.length) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-muted">Không có Manager.</td></tr>`;
      return;
    }
    tbody.innerHTML = rows
      .map(
        (m) => `
        <tr data-mid="${m.id}" role="button" style="cursor:pointer">
          <td><b>#${m.id}</b></td>
          <td class="text-muted">${String(m.username || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td>${String(m.fullName || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-muted">${m.phone || "—"}</td>
          <td class="text-muted">${m.branchName || "—"}</td>
        </tr>
      `
      )
      .join("");
  }

  function renderBranchOptions(selectedId = "") {
    if (!branchSel) return;
    const cur = String(selectedId || branchSel.value || "");
    branchSel.innerHTML = [
      `<option value="">— Chọn chi nhánh —</option>`,
      ...branches.map((b) => `<option value="${b.id}">${String(b.name || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</option>`),
    ].join("");
    if (cur) branchSel.value = cur;
  }

  async function loadBranches() {
    branches = await api("/api/admin/hotels");
    if (!Array.isArray(branches)) branches = [];
    branches.sort((a, b) => Number(a.id) - Number(b.id));
    renderBranchOptions();
  }

  async function loadList() {
    clearErr();
    const fd = searchForm ? new FormData(searchForm) : null;
    const q = new URLSearchParams();
    const id = fd ? (fd.get("id") || "").toString().trim() : "";
    const username = fd ? (fd.get("username") || "").toString().trim() : "";
    const phone = fd ? (fd.get("phone") || "").toString().trim() : "";
    const fullName = fd ? (fd.get("fullName") || "").toString().trim() : "";
    if (id) q.set("id", id);
    if (username) q.set("username", username);
    if (phone) q.set("phone", phone);
    if (fullName) q.set("fullName", fullName);
    const res = await api(`/api/admin/branch-managers${q.toString() ? `?${q.toString()}` : ""}`);
    rows = Array.isArray(res) ? res : [];
    selected = null;
    applySelectionState();
    renderTable();
  }

  tbody.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-mid]");
    if (!tr) return;
    const id = Number(tr.getAttribute("data-mid"));
    const it = rows.find((x) => Number(x.id) === id);
    if (!it) return;
    selected = it;
    tbody.querySelectorAll("tr[data-mid]").forEach((r) => r.classList.remove("table-active"));
    tr.classList.add("table-active");
    applySelectionState();
  });

  searchForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await loadList();
    } catch (err) {
      showErr(err.message || String(err));
    }
  });

  btnAdd?.addEventListener("click", () => {
    mode = "create";
    selected = null;
    applySelectionState();
    clearModalErr();
    form?.reset();
    if (modalTitle) modalTitle.textContent = "Thêm Manager";
    const uEl = form?.elements?.namedItem("username");
    const pEl = form?.elements?.namedItem("password");
    if (uEl) uEl.disabled = false;
    if (pEl) pEl.required = true;
    if (passHint) passHint.textContent = "Mật khẩu tối thiểu 8 ký tự.";
    renderBranchOptions();
    modal?.show();
  });

  btnEdit?.addEventListener("click", () => {
    if (!selected?.id) return;
    mode = "edit";
    clearModalErr();
    if (modalTitle) modalTitle.textContent = `Sửa Manager #${selected.id}`;
    const uEl = form?.elements?.namedItem("username");
    const pEl = form?.elements?.namedItem("password");
    const nEl = form?.elements?.namedItem("fullName");
    const phEl = form?.elements?.namedItem("phone");
    if (uEl) {
      uEl.value = selected.username || "";
      uEl.disabled = false; // admin can change username, will validate uniqueness
    }
    if (nEl) nEl.value = selected.fullName || "";
    if (phEl) phEl.value = selected.phone || "";
    if (pEl) {
      pEl.value = "";
      pEl.required = false;
    }
    if (passHint) passHint.textContent = "Để trống nếu không đổi mật khẩu.";
    renderBranchOptions(String(selected.branchId || ""));
    modal?.show();
  });

  btnSave?.addEventListener("click", async () => {
    clearModalErr();
    const fd = new FormData(form);
    const payload = {
      username: (fd.get("username") || "").toString().trim(),
      password: (fd.get("password") || "").toString(),
      fullName: (fd.get("fullName") || "").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
      branchId: Number(fd.get("branchId")),
    };
    if (!payload.username) return showModalErr("Vui lòng nhập tên đăng nhập.");
    if (!payload.fullName) return showModalErr("Vui lòng nhập họ tên.");
    if (!/^\d{10}$/.test(payload.phone)) return showModalErr("Số điện thoại phải đúng 10 chữ số.");
    if (!Number.isFinite(payload.branchId) || payload.branchId <= 0) return showModalErr("Vui lòng chọn chi nhánh.");
    if (mode === "create" && (!payload.password || payload.password.trim().length < 8)) return showModalErr("Mật khẩu phải có ít nhất 8 ký tự.");
    if (mode === "edit" && payload.password && payload.password.trim() && payload.password.trim().length < 8) return showModalErr("Mật khẩu phải có ít nhất 8 ký tự.");

    btnSave.disabled = true;
    try {
      if (mode === "create") {
        await api("/api/admin/branch-managers", { method: "POST", body: JSON.stringify(payload) });
      } else {
        const id = selected?.id;
        if (!id) throw new Error("Chưa chọn Manager.");
        await api(`/api/admin/branch-managers/${encodeURIComponent(String(id))}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await loadList();
    } catch (e) {
      showModalErr(e.message || String(e));
    } finally {
      btnSave.disabled = false;
    }
  });

  btnDel?.addEventListener("click", () => {
    if (!selected?.id) return;
    clearDelErr();
    if (delName) delName.textContent = selected.username || `#${selected.id}`;
    delModal?.show();
  });

  btnConfirmDel?.addEventListener("click", async () => {
    if (!selected?.id) return;
    clearDelErr();
    btnConfirmDel.disabled = true;
    try {
      await api(`/api/admin/branch-managers/${encodeURIComponent(String(selected.id))}`, { method: "DELETE" });
      delModal?.hide();
      await loadList();
    } catch (e) {
      showDelErr(e.message || String(e));
    } finally {
      btnConfirmDel.disabled = false;
    }
  });

  try {
    await requireAdmin();
    await loadBranches();
    await loadList();
  } catch (e) {
    showErr(e.message || String(e));
  }
}

async function initAdminRoomTypes() {
  const tbody = qs("#rtTbody");
  if (!tbody) return;
  if (!requireLoginOrRedirect()) return;
  bootLogout();
  bootBackHome();

  const alertBox = qs("#pageAlert");
  const btnAdd = qs("#btnAddRt");

  const modalEl = qs("#rtModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = qs("#rtModalTitle");
  const modalAlert = qs("#rtModalAlert");
  const form = qs("#rtForm");
  const btnSave = qs("#btnSaveRt");

  const delModalEl = qs("#rtDeleteModal");
  const delModal = delModalEl ? new bootstrap.Modal(delModalEl) : null;
  const delAlert = qs("#rtDeleteAlert");
  const delName = qs("#rtDeleteName");
  const btnConfirmDel = qs("#btnConfirmDeleteRt");

  let rows = [];
  let editId = null;
  let pendingDelete = null;

  function showErr(msg) {
    if (!alertBox) return;
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox?.classList.add("d-none");
    if (alertBox) alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalErr() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }
  function showDelErr(msg) {
    if (!delAlert) return;
    delAlert.textContent = msg;
    delAlert.classList.remove("d-none");
  }
  function clearDelErr() {
    delAlert?.classList.add("d-none");
    if (delAlert) delAlert.textContent = "";
  }
  async function requireAdmin() {
    const me = await api("/me");
    if (String(me?.role || "").toUpperCase() !== "ADMIN") throw new Error("Chỉ Admin mới được thao tác.");
  }
  function statusViRt(st) {
    return String(st || "").toUpperCase() === "ACTIVE" ? "Hoạt động" : "Ngừng";
  }
  function renderTable() {
    if (!rows.length) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-muted">Không có loại phòng.</td></tr>`;
      return;
    }
    tbody.innerHTML = rows
      .map((rt) => `
        <tr>
          <td><b>#${rt.id}</b></td>
          <td class="text-muted">${rt.code || "—"}</td>
          <td>${String(rt.name || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-muted">${String(rt.description || "—").replace(/</g, "&lt;").replace(/>/g, "&gt;")}</td>
          <td class="text-end"><b>${fmtMoney(rt.basePrice)}</b></td>
          <td><span class="fw-semibold">${statusViRt(rt.status)}</span></td>
          <td class="text-end">
            <button class="btn btn-sm btn-primary fw-semibold btn-edit-rt" data-id="${rt.id}">Sửa</button>
            <button class="btn btn-sm btn-primary fw-semibold btn-del-rt" data-id="${rt.id}">Xóa</button>
          </td>
        </tr>
      `)
      .join("");
  }
  async function loadList() {
    clearErr();
    const res = await api("/api/admin/room-types");
    rows = Array.isArray(res) ? res : [];
    rows.sort((a, b) => Number(a.id) - Number(b.id));
    renderTable();
  }

  btnAdd?.addEventListener("click", () => {
    editId = null;
    clearModalErr();
    form?.reset();
    if (modalTitle) modalTitle.textContent = "Thêm loại phòng mới";
    const stEl = form?.elements?.namedItem("status");
    if (stEl) stEl.value = "ACTIVE";
    modal?.show();
  });

  tbody.addEventListener("click", (e) => {
    const btnEdit = e.target.closest(".btn-edit-rt");
    const btnDel = e.target.closest(".btn-del-rt");
    const id = btnEdit?.getAttribute("data-id") || btnDel?.getAttribute("data-id");
    if (!id) return;
    const it = rows.find((x) => String(x.id) === String(id));
    if (!it) return;

    if (btnEdit) {
      editId = Number(it.id);
      clearModalErr();
      if (modalTitle) modalTitle.textContent = `Sửa loại phòng #${it.id}`;
      const codeEl = form?.elements?.namedItem("code");
      const nameEl = form?.elements?.namedItem("name");
      const descEl = form?.elements?.namedItem("description");
      const priceEl = form?.elements?.namedItem("basePrice");
      const stEl = form?.elements?.namedItem("status");
      if (codeEl) codeEl.value = it.code || "";
      if (nameEl) nameEl.value = it.name || "";
      if (descEl) descEl.value = it.description || "";
      if (priceEl) priceEl.value = String(it.basePrice ?? "");
      if (stEl) stEl.value = String(it.status || "ACTIVE").toUpperCase();
      modal?.show();
    }

    if (btnDel) {
      pendingDelete = it;
      clearDelErr();
      if (delName) delName.textContent = it.name || `#${it.id}`;
      delModal?.show();
    }
  });

  btnSave?.addEventListener("click", async () => {
    clearModalErr();
    const fd = new FormData(form);
    const code = (fd.get("code") || "").toString().trim();
    const name = (fd.get("name") || "").toString().trim();
    const description = (fd.get("description") || "").toString().trim();
    const rawPrice = (fd.get("basePrice") || "").toString().trim().replace(/,/g, "");
    const basePrice = Number(rawPrice);
    const status = (fd.get("status") || "ACTIVE").toString().trim().toUpperCase();
    if (!code) return showModalErr("Vui lòng nhập mã.");
    if (!name) return showModalErr("Vui lòng nhập tên.");
    if (!Number.isFinite(basePrice) || basePrice < 0) {
      return showModalErr("Mức giá phải là số dương lớn hơn 0 và đúng định dạng tiền tệ");
    }
    const payload = { code, name, description: description || null, basePrice, status };
    btnSave.disabled = true;
    try {
      if (editId) {
        await api(`/api/admin/room-types/${encodeURIComponent(String(editId))}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        await api("/api/admin/room-types", { method: "POST", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await loadList();
    } catch (e) {
      showModalErr(e.message || String(e));
    } finally {
      btnSave.disabled = false;
    }
  });

  btnConfirmDel?.addEventListener("click", async () => {
    if (!pendingDelete?.id) return;
    clearDelErr();
    btnConfirmDel.disabled = true;
    try {
      await api(`/api/admin/room-types/${encodeURIComponent(String(pendingDelete.id))}`, { method: "DELETE" });
      delModal?.hide();
      pendingDelete = null;
      await loadList();
    } catch (e) {
      showDelErr(e.message || String(e));
    } finally {
      btnConfirmDel.disabled = false;
    }
  });

  try {
    await requireAdmin();
    await loadList();
  } catch (e) {
    showErr(e.message || String(e));
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  bootLogout();
  await initLogin();
  await initReceptionHome();
  await initBranchRooms();
  await initBranchStaff();
  await initBranchReports();
  await initAdminBranches();
  await initAdminManagers();
  await initAdminRoomTypes();
  await initAdminDashboard();
});

