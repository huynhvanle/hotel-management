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

function requireStaffLogin() {
  if (getToken()) return true;
  if (!window.location.pathname.endsWith("/reception/login.html")) {
    window.location.href = "./login.html";
  }
  return false;
}

async function ensureReceptionRoleOrRedirect() {
  // Only allow RECEPTIONIST to use /reception pages.
  const p = window.location.pathname || "";
  if (p.endsWith("/reception/login.html")) return true;
  if (!getToken()) return false;
  try {
    const me = await api("/me");
    const role = String(me?.role || "").toUpperCase();
    if (role === "RECEPTIONIST") return true;
  } catch {
    // ignore
  }
  window.location.href = "../admin/index.html";
  return false;
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

function statusVi(st) {
  switch (String(st || "")) {
    case "PENDING": return "Chờ xác nhận";
    case "CONFIRMED": return "Đã xác nhận";
    case "CANCELLED": return "Đã hủy";
    default: return st || "—";
  }
}

function fmtMoney(v) {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "—";
  return new Intl.NumberFormat("vi-VN").format(Number(v)) + " ₫";
}

function fmtDate(iso) {
  if (!iso) return "—";
  const s = String(iso).slice(0, 10);
  const p = s.split("-");
  if (p.length !== 3) return s;
  return `${p[2]}/${p[1]}/${p[0]}`;
}

function escapeHtml(s) {
  return String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function clampIntWalkIn(n, fallback = 0) {
  const x = Number(n);
  if (!Number.isFinite(x)) return fallback;
  return Math.trunc(x);
}

function stayNightsWalkIn(checkin, checkout) {
  if (!checkin || !checkout || checkout <= checkin) return 0;
  const a = new Date(checkin);
  const b = new Date(checkout);
  const ms = b.getTime() - a.getTime();
  if (!Number.isFinite(ms) || ms <= 0) return 0;
  return Math.ceil(ms / (1000 * 60 * 60 * 24));
}

function groupWalkInRoomsByType(rooms) {
  const map = new Map();
  for (const r of rooms || []) {
    const hid = r.hotelId !== undefined && r.hotelId !== null ? String(r.hotelId) : "na";
    const key =
      r.roomTypeId !== undefined && r.roomTypeId !== null
        ? `${hid}:id:${r.roomTypeId}`
        : `${hid}:name:${r.roomTypeName || ""}:${r.price}`;
    if (!map.has(key)) {
      map.set(key, {
        roomTypeId: r.roomTypeId,
        roomTypeName: r.roomTypeName,
        roomTypeDescription: r.roomTypeDescription,
        price: r.price,
        hotelName: r.hotelName,
        hotelId: r.hotelId,
        rooms: [],
      });
    }
    const g = map.get(key);
    g.rooms.push(r);
    if (!g.roomTypeDescription && r.roomTypeDescription) g.roomTypeDescription = r.roomTypeDescription;
  }
  return Array.from(map.values()).sort((a, b) =>
    String(a.roomTypeName || "").localeCompare(String(b.roomTypeName || ""), "vi")
  );
}

function renderWalkInOfferCard(g, groupIdx, checkin, checkout) {
  const typeTitle = escapeHtml(g.roomTypeName || "Loại phòng");
  const price = fmtMoney(g.price);
  const n = g.rooms.length;
  const hasDates = Boolean(checkin && checkout);
  const countHtml = hasDates
    ? `Còn <strong>${n}</strong> phòng trống`
    : `<strong>${n}</strong> căn trong kho`;
  const coverId = g.rooms[0]?.id;
  const descRaw = g.roomTypeDescription && String(g.roomTypeDescription).trim();
  const descBlock = descRaw
    ? `<p class="text-muted small mb-2 mb-md-3">${escapeHtml(descRaw)}</p>`
    : "";
  const maxN = Math.max(0, n);
  const gi = Number.isFinite(Number(groupIdx)) ? Number(groupIdx) : 0;

  return `
    <div class="col-12">
      <div class="card border-0 shadow-sm overflow-hidden room-type-offer-card">
        <div class="row g-0 align-items-stretch">
          <div class="col-md-4 col-lg-3">
            <div class="ratio ratio-4x3 bg-secondary bg-opacity-10">
              <img alt="${typeTitle}" src="${CTX}/api/rooms/${encodeURIComponent(coverId)}/image" onerror="this.style.visibility='hidden'" />
            </div>
          </div>
          <div class="col-md-8 col-lg-9">
            <div class="card-body d-flex flex-column flex-lg-row justify-content-between gap-3">
              <div class="flex-grow-1">
                <div class="text-muted text-uppercase small mb-1">Hạng phòng</div>
                <h3 class="h5 fw-semibold mb-2">${typeTitle}</h3>
                ${descBlock}
                <div class="small text-muted">${countHtml} · ${escapeHtml(g.hotelName || "—")}</div>
              </div>
              <div class="d-flex flex-column justify-content-between align-items-stretch align-items-lg-end gap-3 flex-shrink-0">
                <div class="text-lg-end">
                  <div class="text-muted small">Giá chỉ từ</div>
                  <div class="h5 fw-semibold hm-text-gold mb-0">${price}<span class="fs-6 fw-normal hm-muted"> / đêm</span></div>
                </div>
                <div class="w-100" style="max-width: 11rem">
                  <label class="form-label small text-muted mb-1">Số phòng đặt</label>
                  <div class="input-group">
                    <button type="button" class="btn btn-outline-secondary walkin-qty-minus" data-walkin-group="${gi}" aria-label="Giảm">−</button>
                    <input type="number" class="form-control text-center walkin-qty-input" inputmode="numeric" min="0" max="${maxN}" value="0" data-walkin-group="${gi}" aria-label="Số phòng" />
                    <button type="button" class="btn btn-outline-secondary walkin-qty-plus" data-walkin-group="${gi}" aria-label="Tăng">+</button>
                  </div>
                  <div class="small text-muted mt-1">Tối đa ${maxN}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;
}

function mapReceptionItemToCatalogRoom(it) {
  return {
    id: it.rawId,
    hotelId: it.hotelId,
    hotelName: it.hotelName,
    roomTypeName: it.roomTypeName,
    roomTypeId: null,
    roomTypeDescription: null,
    price: it.price,
  };
}

function bootLogout() {
  const btn = qs("#btnLogout");
  btn?.addEventListener("click", () => {
    clearToken();
    window.location.href = "./login.html";
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
      // After login, always go to the admin portal; it will route by role.
      window.location.href = "../admin/index.html";
    } catch (err) {
      alertBox.textContent = err.message || String(err);
      alertBox.classList.remove("d-none");
    }
  });
}

async function initSearchBookings() {
  const form = qs("#searchForm");
  if (!form) return;
  if (!requireStaffLogin()) return;
  if (!(await ensureReceptionRoleOrRedirect())) return;
  bootLogout();

  const alertBox = qs("#searchAlert");
  const tbody = qs("#resultTbody");
  const detailModalEl = qs("#detailModal");
  const detailModal = detailModalEl && window.bootstrap?.Modal ? new bootstrap.Modal(detailModalEl) : null;
  const detailBox = qs("#detailBox");
  const detailAlert = qs("#detailAlert");
  const detailTitle = qs("#detailTitle");
  const btnConfirm = qs("#btnConfirmBooking");
  const btnUnconfirm = qs("#btnUnconfirmBooking");
  const btnInvoice = qs("#btnShowInvoice");
  const btnCheckout = qs("#btnCheckout");
  const btnCancel = qs("#btnCancelBooking");
  const invoiceModalEl = qs("#invoiceModal");
  const invoiceModal = invoiceModalEl && window.bootstrap?.Modal ? new bootstrap.Modal(invoiceModalEl) : null;
  const invoiceTitle = qs("#invoiceTitle");
  const invoiceBox = qs("#invoiceBox");
  const invoiceAlert = qs("#invoiceAlert");
  const btnInvoicePaid = qs("#btnInvoicePaid");
  let currentDetail = null;
  let currentInvoiceBookingId = null;

  function showErr(msg) {
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }

  function clearErr() {
    alertBox.classList.add("d-none");
    alertBox.textContent = "";
  }

  function renderRows(items) {
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-muted">Không có kết quả.</td></tr>`;
      return;
    }
    tbody.innerHTML = items
      .map((it) => {
        const checkedIn = String(it.checkedIn) === "true" || it.checkedIn === true;
        const checkedOut = String(it.checkedOut) === "true" || it.checkedOut === true;
        return `
          <tr data-booking-id="${it.bookingId}">
            <td><b>#${it.bookingId}</b></td>
            <td class="text-muted">${it.clientPhone || "—"}</td>
            <td class="text-muted">${fmtDate(it.checkin)}</td>
            <td class="text-muted">${fmtDate(it.checkout)}</td>
            <td><span class="fw-semibold">${checkedIn ? "Đã" : "Chưa"}</span></td>
            <td><span class="fw-semibold">${checkedOut ? "Đã" : "Chưa"}</span></td>
            <td><span class="fw-semibold">${statusVi(it.status)}</span></td>
            <td class="text-end"><b>${fmtMoney(it.depositAmount)}</b></td>
          </tr>
        `;
      })
      .join("");
  }

  tbody.addEventListener("click", async (e) => {
    const tr = e.target.closest("tr[data-booking-id]");
    if (!tr) return;
    const id = tr.getAttribute("data-booking-id");
    if (!id) return;
    if (detailAlert) {
      detailAlert.classList.add("d-none");
      detailAlert.textContent = "";
    }
    if (btnConfirm) btnConfirm.classList.add("d-none");
    if (btnUnconfirm) btnUnconfirm.classList.add("d-none");
    try {
      const d = await api(`/api/reception/bookings/${encodeURIComponent(String(id))}`);
      currentDetail = d;
      if (detailTitle) detailTitle.textContent = `Chi tiết đơn #${d.bookingId}`;
      if (detailBox) detailBox.innerHTML = "";
      if (btnConfirm && String(d.status) === "PENDING") {
        btnConfirm.classList.remove("d-none");
      }
      if (btnUnconfirm && String(d.status) === "CONFIRMED") {
        btnUnconfirm.classList.remove("d-none");
      }
      await reloadDetail(d.bookingId);
      detailModal?.show();
    } catch (err) {
      if (detailAlert) {
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    }
  });

  async function reloadDetail(bid) {
    const d = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}`);
    currentDetail = d;
    if (detailBox) {
      const roomTypeLines = Array.isArray(d.roomTypes) && d.roomTypes.length
        ? d.roomTypes
            .map((x) => `${x.roomTypeName || "—"} × ${Number(x.quantity || 0)}`)
            .join(", ")
        : "—";
      const isCheckedIn = String(d.checkedIn) === "true" || d.checkedIn === true;
      const roomLine = isCheckedIn
        ? ((d.roomIds || []).join(", ") || "—")
        : "Chưa gán";
      const isInvoiceSaved = String(d.invoiceSaved) === "true" || d.invoiceSaved === true;
      const isCheckedOut = String(d.checkedOut) === "true" || d.checkedOut === true;
      detailBox.innerHTML = `
        <div><span class="text-muted">Khách:</span> <b>${d.clientFullName || "—"}</b></div>
        <div><span class="text-muted">SĐT:</span> <b>${d.clientPhone || "—"}</b></div>
        <div><span class="text-muted">Ngày:</span> <b>${fmtDate(d.checkin)} → ${fmtDate(d.checkout)}</b></div>
        <div><span class="text-muted">Trạng thái:</span> <b>${statusVi(d.status)}</b></div>
        <div><span class="text-muted">Check-in:</span> <b>${isCheckedIn ? "Đã check-in" : "Chưa check-in"}</b></div>
        <div><span class="text-muted">Hoá đơn:</span> <b>${isInvoiceSaved ? "Đã lưu" : "Chưa lưu"}</b></div>
        <div><span class="text-muted">Check-out:</span> <b>${isCheckedOut ? "Đã check-out" : "Chưa check-out"}</b></div>
        <div><span class="text-muted">Cọc:</span> <b>${fmtMoney(d.depositAmount)}</b></div>
        <div class="mt-2"><span class="text-muted">Hạng phòng:</span> <b>${roomTypeLines}</b></div>
        <div class="mt-1"><span class="text-muted">Phòng:</span> <b>${roomLine}</b></div>
        <div id="checkinBox" class="mt-3"></div>
      `;
    }
    if (btnConfirm) btnConfirm.classList.toggle("d-none", String(d.status) !== "PENDING");
    const isCheckedIn = String(d.checkedIn) === "true" || d.checkedIn === true;
    if (btnUnconfirm) btnUnconfirm.classList.toggle("d-none", String(d.status) !== "CONFIRMED" || isCheckedIn);
    if (btnInvoice) btnInvoice.classList.toggle("d-none", !(String(d.checkedIn) === "true" || d.checkedIn === true));
    const isInvoiceSaved = String(d.invoiceSaved) === "true" || d.invoiceSaved === true;
    const isCheckedOut = String(d.checkedOut) === "true" || d.checkedOut === true;
    if (btnCheckout) btnCheckout.classList.toggle("d-none", !(isCheckedIn && isInvoiceSaved && !isCheckedOut));
    if (btnCancel) btnCancel.classList.toggle("d-none", String(d.status) === "CANCELLED" || isCheckedIn || isCheckedOut);

    const checkinBox = qs("#checkinBox");
    if (!checkinBox) return;
    checkinBox.innerHTML = "";
    if (String(d.status) !== "CONFIRMED") return;

    // Check-in: lễ tân chọn phòng vật lí AVAILABLE để gán cho đơn.
    try {
      const opt = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/checkin-options`);
      const rts = Array.isArray(opt?.roomTypes) ? opt.roomTypes : [];
      if (!rts.length) return;

      const blocks = [];
      for (const rt of rts) {
        const qty = Number(rt.quantity || 0);
        const choices = Array.isArray(rt.availableRoomIds) ? rt.availableRoomIds : [];
        if (qty <= 0) continue;
        const selects = [];
        for (let i = 0; i < qty; i++) {
          const sid = `checkin_rt_${rt.roomTypeId}_${i}`;
          selects.push(`
            <div class="col-12 col-md-6">
              <label class="form-label small mb-1">${escapeHtml(rt.roomTypeName || "Loại phòng")} · Phòng ${i + 1}</label>
              <select class="form-select form-select-sm checkin-room-select" id="${sid}">
                <option value="">— Chọn phòng —</option>
                ${choices.map((rid) => `<option value="${String(rid).replace(/\"/g, "&quot;")}">${escapeHtml(rid)}</option>`).join("")}
              </select>
            </div>
          `);
        }
        blocks.push(`<div class="border rounded-3 p-3 mb-2 bg-light">
          <div class="fw-semibold mb-2">Check-in</div>
          <div class="row g-2">${selects.join("")}</div>
        </div>`);
      }
      checkinBox.innerHTML = `
        ${blocks.join("")}
        <div class="d-flex flex-wrap gap-2 justify-content-end mt-2">
          <button type="button" class="btn btn-primary fw-semibold" id="btnDoCheckin">Check-in</button>
        </div>
      `;

      function recomputeDisableOptions() {
        const selects = [...document.querySelectorAll(".checkin-room-select")];
        const chosen = new Set(selects.map((s) => s.value).filter(Boolean));
        selects.forEach((s) => {
          [...s.options].forEach((o) => {
            if (!o.value) return;
            o.disabled = chosen.has(o.value) && s.value !== o.value;
          });
        });
      }
      document.querySelectorAll(".checkin-room-select").forEach((s) => {
        s.addEventListener("change", recomputeDisableOptions);
      });
      recomputeDisableOptions();

      const btnDo = qs("#btnDoCheckin");
      btnDo?.addEventListener("click", async () => {
        if (detailAlert) {
          detailAlert.classList.add("d-none");
          detailAlert.textContent = "";
        }
        const selects = [...document.querySelectorAll(".checkin-room-select")];
        const roomIds = selects.map((s) => s.value).filter(Boolean);
        if (roomIds.length !== selects.length) {
          if (detailAlert) {
            detailAlert.className = "alert alert-danger";
            detailAlert.textContent = "Vui lòng chọn đủ phòng để check-in.";
            detailAlert.classList.remove("d-none");
          }
          return;
        }
        btnDo.disabled = true;
        try {
          const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/checkin`, {
            method: "PUT",
            body: JSON.stringify({ roomIds }),
          });
          if (detailAlert) {
            detailAlert.className = "alert alert-success";
            detailAlert.textContent = res?.message || "Check-in thành công.";
            detailAlert.classList.remove("d-none");
          }
          // refresh list and detail
          if (typeof form.requestSubmit === "function") form.requestSubmit();
          else form.dispatchEvent(new Event("submit", { cancelable: true }));
          await reloadDetail(bid);
        } catch (err) {
          if (detailAlert) {
            detailAlert.className = "alert alert-danger";
            detailAlert.textContent = err.message || String(err);
            detailAlert.classList.remove("d-none");
          }
        } finally {
          btnDo.disabled = false;
        }
      });
    } catch {
      // if already checked-in or not eligible, do not show check-in UI
      checkinBox.innerHTML = "";
    }
  }

  btnConfirm?.addEventListener("click", async () => {
    const bid = currentDetail?.bookingId;
    if (!bid) return;
    if (detailAlert) {
      detailAlert.classList.add("d-none");
      detailAlert.textContent = "";
    }
    btnConfirm.disabled = true;
    try {
      const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/confirm`, { method: "PUT" });
      if (detailAlert) {
        detailAlert.className = "alert alert-success";
        detailAlert.textContent = res?.message || "Xác nhận đơn đặt thành công.";
        detailAlert.classList.remove("d-none");
      }
      // refresh current search result list
      if (typeof form.requestSubmit === "function") form.requestSubmit();
      else form.dispatchEvent(new Event("submit", { cancelable: true }));

      await reloadDetail(bid);
    } catch (err) {
      if (detailAlert) {
        detailAlert.className = "alert alert-danger";
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    } finally {
      btnConfirm.disabled = false;
    }
  });

  btnUnconfirm?.addEventListener("click", async () => {
    const bid = currentDetail?.bookingId;
    if (!bid) return;
    if (detailAlert) {
      detailAlert.classList.add("d-none");
      detailAlert.textContent = "";
    }
    btnUnconfirm.disabled = true;
    try {
      const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/unconfirm`, { method: "PUT" });
      if (detailAlert) {
        detailAlert.className = "alert alert-success";
        detailAlert.textContent = res?.message || "Đã huỷ xác nhận.";
        detailAlert.classList.remove("d-none");
      }
      if (typeof form.requestSubmit === "function") form.requestSubmit();
      else form.dispatchEvent(new Event("submit", { cancelable: true }));
      await reloadDetail(bid);
    } catch (err) {
      if (detailAlert) {
        detailAlert.className = "alert alert-danger";
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    } finally {
      btnUnconfirm.disabled = false;
    }
  });

  btnInvoice?.addEventListener("click", async () => {
    const bid = currentDetail?.bookingId;
    if (!bid) return;
    currentInvoiceBookingId = bid;
    if (invoiceAlert) {
      invoiceAlert.classList.add("d-none");
      invoiceAlert.textContent = "";
    }
    if (btnInvoicePaid) btnInvoicePaid.classList.add("d-none");
    try {
      const inv = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/invoice-preview`);
      if (invoiceTitle) invoiceTitle.textContent = `Hoá đơn · Đơn #${inv.bookingId}`;
      if (invoiceBox) {
        const typeLine = Array.isArray(inv.roomTypes) && inv.roomTypes.length
          ? inv.roomTypes.map((x) => `${x.roomTypeName || "—"} × ${Number(x.quantity || 0)}`).join(", ")
          : "—";
        invoiceBox.innerHTML = `
          <div><span class="text-muted">Khách:</span> <b>${inv.clientFullName || "—"}</b></div>
          <div><span class="text-muted">SĐT:</span> <b>${inv.clientPhone || "—"}</b></div>
          <div><span class="text-muted">Ngày:</span> <b>${fmtDate(inv.checkin)} → ${fmtDate(inv.checkout)}</b></div>
          <div class="mt-2"><span class="text-muted">Hạng phòng:</span> <b>${typeLine}</b></div>
          <div class="mt-1"><span class="text-muted">Phòng:</span> <b>${(inv.roomIds || []).join(", ") || "—"}</b></div>
          <hr />
          <div class="d-flex justify-content-between"><span class="text-muted">Tổng tiền</span><b>${fmtMoney(inv.totalAmount)}</b></div>
          <div class="d-flex justify-content-between"><span class="text-muted">Tiền cọc (50%)</span><b>${fmtMoney(inv.depositAmount)}</b></div>
          <div class="d-flex justify-content-between"><span class="text-muted">Còn lại</span><b>${fmtMoney(inv.remainingAmount)}</b></div>
        `;
      }
      const isInvoiceSaved = String(currentDetail?.invoiceSaved) === "true" || currentDetail?.invoiceSaved === true;
      if (btnInvoicePaid) btnInvoicePaid.classList.toggle("d-none", isInvoiceSaved);
      if (invoiceAlert && isInvoiceSaved) {
        invoiceAlert.className = "alert alert-success";
        invoiceAlert.textContent = "Hoá đơn đã được lưu (đã thanh toán).";
        invoiceAlert.classList.remove("d-none");
      }
      invoiceModal?.show();
    } catch (err) {
      if (invoiceAlert) {
        invoiceAlert.textContent = err.message || String(err);
        invoiceAlert.classList.remove("d-none");
      }
    }
  });

  btnInvoicePaid?.addEventListener("click", async () => {
    const bid = currentInvoiceBookingId;
    if (!bid) return;
    if (invoiceAlert) {
      invoiceAlert.classList.add("d-none");
      invoiceAlert.textContent = "";
    }
    btnInvoicePaid.disabled = true;
    try {
      const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/invoice/pay`, { method: "PUT" });
      if (invoiceAlert) {
        invoiceAlert.className = "alert alert-success";
        invoiceAlert.textContent = res?.message || "Đã ghi nhận thanh toán.";
        invoiceAlert.classList.remove("d-none");
      }
      btnInvoicePaid.classList.add("d-none");
      await reloadDetail(bid);
    } catch (err) {
      if (invoiceAlert) {
        invoiceAlert.className = "alert alert-danger";
        invoiceAlert.textContent = err.message || String(err);
        invoiceAlert.classList.remove("d-none");
      }
    } finally {
      btnInvoicePaid.disabled = false;
    }
  });

  btnCheckout?.addEventListener("click", async () => {
    const bid = currentDetail?.bookingId;
    if (!bid) return;
    if (detailAlert) {
      detailAlert.classList.add("d-none");
      detailAlert.textContent = "";
    }
    btnCheckout.disabled = true;
    try {
      const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/checkout`, { method: "PUT" });
      if (detailAlert) {
        detailAlert.className = "alert alert-success";
        detailAlert.textContent = res?.message || "Check-out thành công.";
        detailAlert.classList.remove("d-none");
      }
      if (typeof form.requestSubmit === "function") form.requestSubmit();
      else form.dispatchEvent(new Event("submit", { cancelable: true }));
      await reloadDetail(bid);
    } catch (err) {
      if (detailAlert) {
        detailAlert.className = "alert alert-danger";
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    } finally {
      btnCheckout.disabled = false;
    }
  });

  btnCancel?.addEventListener("click", async () => {
    const bid = currentDetail?.bookingId;
    if (!bid) return;
    const ok = window.confirm("Bạn chắc chắn muốn huỷ đơn này?");
    if (!ok) return;
    if (detailAlert) {
      detailAlert.classList.add("d-none");
      detailAlert.textContent = "";
    }
    btnCancel.disabled = true;
    try {
      const res = await api(`/api/reception/bookings/${encodeURIComponent(String(bid))}/cancel`, { method: "PUT" });
      if (detailAlert) {
        detailAlert.className = "alert alert-success";
        detailAlert.textContent = res?.message || "Huỷ đơn thành công.";
        detailAlert.classList.remove("d-none");
      }
      if (typeof form.requestSubmit === "function") form.requestSubmit();
      else form.dispatchEvent(new Event("submit", { cancelable: true }));
      await reloadDetail(bid);
    } catch (err) {
      if (detailAlert) {
        detailAlert.className = "alert alert-danger";
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    } finally {
      btnCancel.disabled = false;
    }
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearErr();
    const fd = new FormData(form);
    const bookingId = (fd.get("bookingId") || "").toString().trim();
    const phone = (fd.get("phone") || "").toString().trim();
    if (!bookingId && !phone) {
      showErr("Vui lòng nhập Mã đơn đặt hoặc SĐT để tìm.");
      return;
    }
    const q = new URLSearchParams();
    if (bookingId) q.set("bookingId", bookingId);
    if (phone) q.set("phone", phone);
    try {
      const items = await api(`/api/reception/bookings/search?${q.toString()}`);
      renderRows(Array.isArray(items) ? items : []);
    } catch (err) {
      showErr(err.message || String(err));
    }
  });
}

async function initSearchClients() {
  const form = qs("#searchClientForm");
  if (!form) return;
  if (!requireStaffLogin()) return;
  if (!(await ensureReceptionRoleOrRedirect())) return;
  bootLogout();

  const alertBox = qs("#clientAlert");
  const tbody = qs("#clientTbody");
  const modalEl = qs("#clientDetailModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const titleEl = qs("#clientDetailTitle");
  const detailAlert = qs("#clientDetailAlert");
  const detailBox = qs("#clientDetailBox");

  const btnAddClient = qs("#btnAddClient");
  const addModalEl = qs("#addClientModal");
  const addModal = addModalEl ? new bootstrap.Modal(addModalEl) : null;
  const addForm = qs("#addClientForm");
  const addAlert = qs("#addClientAlert");
  const btnCreate = qs("#btnCreateClient");

  function showErr(msg) {
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox.classList.add("d-none");
    alertBox.textContent = "";
  }

  function renderRows(items) {
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="4" class="text-muted">Không có kết quả.</td></tr>`;
      return;
    }
    tbody.innerHTML = items
      .map((c) => {
        return `
          <tr data-client-id="${c.id}">
            <td><b>#${c.id}</b></td>
            <td class="text-muted">${c.fullName || "—"}</td>
            <td class="text-muted">${c.phone || "—"}</td>
            <td class="text-muted">${c.idCardNumber || "—"}</td>
          </tr>
        `;
      })
      .join("");
  }

  tbody.addEventListener("click", async (e) => {
    const tr = e.target.closest("tr[data-client-id]");
    if (!tr) return;
    const id = tr.getAttribute("data-client-id");
    if (!id) return;
    detailAlert?.classList.add("d-none");
    detailAlert.textContent = "";
    try {
      const d = await api(`/api/reception/clients/${encodeURIComponent(String(id))}`);
      if (titleEl) titleEl.textContent = `Khách hàng #${d.id}`;
      if (detailBox) {
        detailBox.innerHTML = `
          <div><span class="text-muted">Họ tên:</span> <b>${d.fullName || "—"}</b></div>
          <div><span class="text-muted">SĐT:</span> <b>${d.phone || "—"}</b></div>
          <div><span class="text-muted">CCCD/Passport:</span> <b>${d.idCardNumber || "—"}</b></div>
        `;
      }
      modal?.show();
    } catch (err) {
      if (detailAlert) {
        detailAlert.textContent = err.message || String(err);
        detailAlert.classList.remove("d-none");
      }
    }
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearErr();
    const fd = new FormData(form);
    const idCardNumber = (fd.get("idCardNumber") || "").toString().trim();
    const phone = (fd.get("phone") || "").toString().trim();
    if (!idCardNumber && !phone) {
      showErr("Vui lòng nhập CCCD/Passport hoặc SĐT để tìm.");
      return;
    }
    const q = new URLSearchParams();
    if (idCardNumber) q.set("idCardNumber", idCardNumber);
    if (phone) q.set("phone", phone);
    try {
      const items = await api(`/api/reception/clients/search?${q.toString()}`);
      renderRows(Array.isArray(items) ? items : []);
    } catch (err) {
      showErr(err.message || String(err));
    }
  });

  btnAddClient?.addEventListener("click", () => {
    addAlert?.classList.add("d-none");
    if (addAlert) addAlert.textContent = "";
    addForm?.reset();
    addModal?.show();
  });

  addForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    addAlert?.classList.add("d-none");
    if (addAlert) addAlert.textContent = "";
    if (btnCreate) btnCreate.disabled = true;
    const fd = new FormData(addForm);
    const payload = {
      fullName: (fd.get("fullName") || "").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
      idCardNumber: (fd.get("idCardNumber") || "").toString().trim(),
    };
    try {
      await api("/api/reception/clients", { method: "POST", body: JSON.stringify(payload) });
      addModal?.hide();
      // Refresh list using current filters (if any)
      form.dispatchEvent(new Event("submit", { cancelable: true }));
    } catch (err) {
      if (addAlert) {
        addAlert.textContent = err.message || String(err);
        addAlert.classList.remove("d-none");
      }
    } finally {
      if (btnCreate) btnCreate.disabled = false;
    }
  });
}

function isoTomorrowFrom(isoDate) {
  const base = isoDate || new Date().toISOString().slice(0, 10);
  const d = new Date(`${base}T12:00:00`);
  d.setDate(d.getDate() + 1);
  return d.toISOString().slice(0, 10);
}

async function initWalkInBooking() {
  const roomForm = qs("#walkInRoomForm");
  if (!roomForm) return;
  if (!requireStaffLogin()) return;
  if (!(await ensureReceptionRoleOrRedirect())) return;
  bootLogout();

  const grid = qs("#walkInRoomsGrid");
  const catalogSection = qs("#walkInCatalogSection");
  const roomAlert = qs("#walkInRoomsAlert");
  const catalogHotelLine = qs("#walkInCatalogHotelLine");
  const catalogDateLine = qs("#walkInCatalogDateLine");
  const catalogLines = qs("#walkInCatalogLines");
  const catalogTotal = qs("#walkInCatalogTotal");
  const topAlert = qs("#walkInTopAlert");
  const selectedBox = qs("#walkInSelectedClient");
  const btnSubmit = qs("#walkInBtnSubmit");
  const btnOpenClient = qs("#walkInBtnOpenClientSearch");
  const clientModalEl = qs("#walkInClientSearchModal");
  const clientModal = clientModalEl && window.bootstrap?.Modal ? new bootstrap.Modal(clientModalEl) : null;
  const clientSearchForm = qs("#walkInClientSearchForm");
  const clientTbody = qs("#walkInClientTbody");
  const clientAlert = qs("#walkInClientAlert");
  const btnAddClient = qs("#walkInBtnAddClient");
  const addModalEl = qs("#walkInAddClientModal");
  const addModal = addModalEl && window.bootstrap?.Modal ? new bootstrap.Modal(addModalEl) : null;
  const addForm = qs("#walkInAddClientForm");
  const addAlert = qs("#walkInAddClientAlert");
  const btnCreate = qs("#walkInBtnCreateClient");

  let catalogGroupsRef = [];
  let selectedClientId = null;

  function hideCatalog() {
    catalogSection?.classList.add("d-none");
    catalogGroupsRef = [];
    if (grid) grid.innerHTML = "";
    updateWalkInSummary();
  }

  function readWalkInQty(input) {
    if (!input) return 0;
    const max = Math.max(0, clampIntWalkIn(input.getAttribute("max"), 0));
    let v = clampIntWalkIn(input.value, 0);
    return Math.min(max, Math.max(0, v));
  }

  function clampWalkInQtyInput(input) {
    if (!input) return 0;
    const max = Math.max(0, clampIntWalkIn(input.getAttribute("max"), 0));
    let v = clampIntWalkIn(input.value, 0);
    if (v < 0) v = 0;
    if (v > max) v = max;
    input.value = String(v);
    return v;
  }

  function collectWalkInRoomIds() {
    const ids = [];
    if (!grid) return ids;
    catalogGroupsRef.forEach((g, i) => {
      const inp = grid.querySelector(`input.walkin-qty-input[data-walkin-group="${i}"]`);
      const qn = readWalkInQty(inp);
      ids.push(...g.rooms.slice(0, qn).map((r) => r.id));
    });
    return ids;
  }

  function updateWalkInSummary() {
    const fd = new FormData(roomForm);
    const checkin = (fd.get("checkin") || "").toString().trim();
    const checkout = (fd.get("checkout") || "").toString().trim();
    const nights = stayNightsWalkIn(checkin, checkout);

    if (catalogHotelLine) {
      const names = [...new Set(catalogGroupsRef.map((g) => g.hotelName).filter(Boolean))];
      catalogHotelLine.textContent =
        names.length === 1 ? names[0] : names.length ? names.join(", ") : "—";
    }
    if (catalogDateLine) {
      if (checkin && checkout) {
        catalogDateLine.innerHTML = `${fmtDate(checkin)} → ${fmtDate(checkout)} <span class="hm-muted">(${nights} đêm)</span>`;
      } else {
        catalogDateLine.textContent = "—";
      }
    }

    let sumPerNight = 0;
    const parts = [];
    catalogGroupsRef.forEach((g, i) => {
      const inp = grid?.querySelector(`input.walkin-qty-input[data-walkin-group="${i}"]`);
      const qn = readWalkInQty(inp);
      if (qn <= 0) return;
      const sub = qn * Number(g.price || 0);
      sumPerNight += sub;
      parts.push(`<div class="d-flex justify-content-between gap-2"><span>${escapeHtml(g.roomTypeName || "Loại phòng")}</span><span>× ${qn}</span></div>
        <div class="small hm-muted mb-2">${fmtMoney(g.price)}/đêm · ${fmtMoney(sub)} / đêm (loại này)</div>`);
    });

    if (catalogLines) {
      catalogLines.innerHTML = parts.length ? parts.join("") : '<span class="hm-muted">—</span>';
    }

    const total = nights > 0 ? sumPerNight * nights : 0;
    if (catalogTotal) {
      if (nights > 0 && sumPerNight > 0) catalogTotal.textContent = fmtMoney(total);
      else if (sumPerNight > 0) catalogTotal.textContent = `${fmtMoney(sumPerNight)} / đêm`;
      else catalogTotal.textContent = fmtMoney(0);
    }
  }

  async function runWalkInRoomSearch() {
    hideCatalog();
    roomAlert?.classList.add("d-none");
    if (roomAlert) roomAlert.textContent = "";

    const fd = new FormData(roomForm);
    const checkin = (fd.get("checkin") || "").toString().trim();
    const checkout = (fd.get("checkout") || "").toString().trim();
    const floor = (fd.get("floor") || "").toString().trim();
    const roomNumber = (fd.get("roomNumber") || "").toString().trim();
    const type = (fd.get("type") || "").toString().trim();
    const minPrice = (fd.get("minPrice") || "").toString().trim();
    const maxPrice = (fd.get("maxPrice") || "").toString().trim();

    if (!checkin || !checkout) {
      if (roomAlert) {
        roomAlert.textContent = "Vui lòng chọn ngày đến và ngày đi.";
        roomAlert.classList.remove("d-none");
      }
      return;
    }
    if (checkout <= checkin) {
      if (roomAlert) {
        roomAlert.textContent = "Ngày đi phải sau ngày đến.";
        roomAlert.classList.remove("d-none");
      }
      return;
    }

    const q = new URLSearchParams({ checkin, checkout });
    if (floor) q.set("floor", floor);
    if (roomNumber) q.set("roomNumber", roomNumber);
    if (type) q.set("type", type);
    if (minPrice) q.set("minPrice", minPrice);
    if (maxPrice) q.set("maxPrice", maxPrice);

    try {
      const items = await api(`/api/reception/walk-in/available-rooms?${q.toString()}`);
      const rows = Array.isArray(items) ? items.map(mapReceptionItemToCatalogRoom) : [];
      if (!rows.length) {
        if (roomAlert) {
          roomAlert.textContent =
            "Không có phòng trống trong khoảng ngày đã chọn (hoặc không khớp bộ lọc).";
          roomAlert.classList.remove("d-none");
        }
        return;
      }
      catalogGroupsRef = groupWalkInRoomsByType(rows);
      catalogSection?.classList.remove("d-none");
      if (grid) {
        grid.innerHTML = catalogGroupsRef
          .map((g, i) => renderWalkInOfferCard(g, i, checkin, checkout))
          .join("");
      }
      updateWalkInSummary();
    } catch (err) {
      if (roomAlert) {
        roomAlert.textContent = err.message || String(err);
        roomAlert.classList.remove("d-none");
      }
    }
  }

  function showClientErr(msg) {
    if (!clientAlert) return;
    clientAlert.textContent = msg;
    clientAlert.classList.remove("d-none");
  }
  function clearClientErr() {
    clientAlert?.classList.add("d-none");
    if (clientAlert) clientAlert.textContent = "";
  }
  function showTop(kind, msg) {
    if (!topAlert) return;
    topAlert.textContent = msg;
    topAlert.classList.remove("d-none", "alert-success", "alert-danger");
    topAlert.classList.add(kind === "ok" ? "alert-success" : "alert-danger");
  }
  function clearTop() {
    topAlert?.classList.add("d-none");
    if (topAlert) topAlert.textContent = "";
  }

  function paintSelectedClientRow() {
    if (!clientTbody) return;
    clientTbody.querySelectorAll("tr[data-client-id]").forEach((tr) => {
      const id = Number(tr.dataset.clientId);
      tr.classList.toggle("table-active", selectedClientId !== null && id === selectedClientId);
    });
  }

  function renderSelectedClientLabel(label) {
    if (!selectedBox) return;
    if (!selectedClientId) {
      selectedBox.innerHTML = `<span class="text-muted">Chưa chọn khách.</span>`;
      return;
    }
    selectedBox.innerHTML = `<span class="text-muted">Khách:</span> <b>${escapeHtml(label)}</b> <span class="text-muted">· ID ${selectedClientId}</span>`;
  }

  function renderClientRows(items) {
    if (!clientTbody) return;
    if (!items.length) {
      clientTbody.innerHTML = `<tr><td colspan="4" class="text-muted">Không có kết quả.</td></tr>`;
      paintSelectedClientRow();
      return;
    }
    clientTbody.innerHTML = items
      .map(
        (c) => `
          <tr data-client-id="${c.id}" role="button" style="cursor:pointer">
            <td>${c.id}</td>
            <td><b>${escapeHtml(c.fullName || "")}</b></td>
            <td>${escapeHtml(String(c.phone || "—"))}</td>
            <td>${c.idCardNumber ?? "—"}</td>
          </tr>
        `
      )
      .join("");
    paintSelectedClientRow();
  }

  const checkinEl = roomForm.elements.namedItem("checkin");
  const checkoutEl = roomForm.elements.namedItem("checkout");
  const todayIso = new Date().toISOString().slice(0, 10);
  if (checkinEl) checkinEl.min = todayIso;
  if (checkinEl && !checkinEl.value) checkinEl.value = todayIso;
  if (checkoutEl && !checkoutEl.value) checkoutEl.value = isoTomorrowFrom(checkinEl?.value || todayIso);

  function syncCheckoutMinFromCheckin() {
    if (!checkinEl || !checkoutEl) return;
    if (checkinEl.value) {
      const next = new Date(`${checkinEl.value}T12:00:00`);
      next.setDate(next.getDate() + 1);
      checkoutEl.min = next.toISOString().slice(0, 10);
      if (checkoutEl.value && checkoutEl.value <= checkinEl.value) checkoutEl.value = checkoutEl.min;
    }
  }
  checkinEl?.addEventListener("change", syncCheckoutMinFromCheckin);
  checkoutEl?.addEventListener("change", syncCheckoutMinFromCheckin);
  checkinEl?.addEventListener("input", () => {
    syncCheckoutMinFromCheckin();
    updateWalkInSummary();
  });
  checkoutEl?.addEventListener("input", updateWalkInSummary);
  syncCheckoutMinFromCheckin();

  roomForm.addEventListener("submit", (e) => {
    e.preventDefault();
    clearTop();
    runWalkInRoomSearch();
  });

  grid?.addEventListener("click", (e) => {
    const minus = e.target.closest(".walkin-qty-minus");
    const plus = e.target.closest(".walkin-qty-plus");
    const gi = minus?.getAttribute("data-walkin-group") ?? plus?.getAttribute("data-walkin-group");
    if (gi === undefined || gi === null) return;
    const input = grid.querySelector(`input.walkin-qty-input[data-walkin-group="${gi}"]`);
    if (!input) return;
    let v = readWalkInQty(input);
    const max = Math.max(0, clampIntWalkIn(input.getAttribute("max"), 0));
    if (minus) v = Math.max(0, v - 1);
    if (plus) v = Math.min(max, v + 1);
    input.value = String(v);
    updateWalkInSummary();
  });

  grid?.addEventListener("change", (e) => {
    const t = e.target;
    if (!t.classList?.contains("walkin-qty-input")) return;
    clampWalkInQtyInput(t);
    updateWalkInSummary();
  });

  btnOpenClient?.addEventListener("click", () => {
    clearClientErr();
    clearTop();
    clientModal?.show();
  });

  clientModalEl?.addEventListener("hidden.bs.modal", () => {
    clearClientErr();
  });

  clientSearchForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearClientErr();
    clearTop();
    const fd = new FormData(clientSearchForm);
    const idCardNumber = (fd.get("idCardNumber") || "").toString().trim();
    const phone = (fd.get("phone") || "").toString().trim();
    if (!idCardNumber && !phone) {
      showClientErr("Nhập CCCD/Passport hoặc SĐT để tìm.");
      return;
    }
    const q = new URLSearchParams();
    if (idCardNumber) q.set("idCardNumber", idCardNumber);
    if (phone) q.set("phone", phone);
    try {
      const items = await api(`/api/reception/clients/search?${q.toString()}`);
      renderClientRows(Array.isArray(items) ? items : []);
    } catch (err) {
      showClientErr(err.message || String(err));
    }
  });

  clientTbody?.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-client-id]");
    if (!tr) return;
    selectedClientId = Number(tr.dataset.clientId);
    const cells = tr.querySelectorAll("td");
    const name = cells[1]?.textContent?.trim() || "";
    paintSelectedClientRow();
    renderSelectedClientLabel(name);
    clientModal?.hide();
  });

  btnAddClient?.addEventListener("click", () => {
    addAlert?.classList.add("d-none");
    if (addAlert) addAlert.textContent = "";
    addForm?.reset();
    addModal?.show();
  });

  addForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    addAlert?.classList.add("d-none");
    if (addAlert) addAlert.textContent = "";
    if (btnCreate) btnCreate.disabled = true;
    const fd = new FormData(addForm);
    const payload = {
      fullName: (fd.get("fullName") || "").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
      idCardNumber: (fd.get("idCardNumber") || "").toString().trim(),
    };
    try {
      const created = await api("/api/reception/clients", { method: "POST", body: JSON.stringify(payload) });
      addModal?.hide();
      selectedClientId = created.id;
      renderClientRows([created]);
      paintSelectedClientRow();
      renderSelectedClientLabel(created.fullName || "");
      clientModal?.hide();
      showTop("ok", "Đã thêm khách và chọn cho đơn đặt.");
    } catch (err) {
      if (addAlert) {
        addAlert.textContent = err.message || String(err);
        addAlert.classList.remove("d-none");
      }
    } finally {
      if (btnCreate) btnCreate.disabled = false;
    }
  });

  btnSubmit?.addEventListener("click", async () => {
    clearTop();
    const fd = new FormData(roomForm);
    const checkin = (fd.get("checkin") || "").toString().trim();
    const checkout = (fd.get("checkout") || "").toString().trim();
    const roomIds = collectWalkInRoomIds();
    if (!checkin || !checkout) {
      showTop("err", "Chọn ngày đến và ngày đi.");
      return;
    }
    if (!roomIds.length) {
      showTop("err", "Chọn ít nhất một phòng.");
      return;
    }
    if (!selectedClientId) {
      showTop("err", "Chọn hoặc thêm khách hàng.");
      return;
    }
    try {
      const res = await api("/api/reception/walk-in/bookings", {
        method: "POST",
        body: JSON.stringify({
          clientId: selectedClientId,
          checkin,
          checkout,
          roomIds,
        }),
      });
      const bid = res.bookingId ?? "";
      const msg = res.message || `Đặt phòng thành công. Mã đơn: ${bid}.`;
      showTop("ok", msg);
      await runWalkInRoomSearch();
    } catch (err) {
      showTop("err", err.message || String(err));
    }
  });
}

async function initReceptionRooms() {
  const form = qs("#roomSearchForm");
  if (!form) return;
  if (!requireStaffLogin()) return;
  if (!(await ensureReceptionRoleOrRedirect())) return;
  bootLogout();

  const alertBox = qs("#roomAlert");
  const tbody = qs("#roomTbody");
  const modalEl = qs("#roomStatusModal");
  const modal = modalEl && window.bootstrap?.Modal ? new bootstrap.Modal(modalEl) : null;
  const modalAlert = qs("#roomStatusAlert");
  const roomNoEl = qs("#roomStatusRoomNo");
  const typeEl = qs("#roomStatusType");
  const curEl = qs("#roomStatusCurrent");
  const selEl = qs("#roomStatusSelect");
  const btnSave = qs("#btnSaveRoomStatus");

  let lastItems = [];
  let selectedRoomId = null;

  function showErr(msg) {
    alertBox.textContent = msg;
    alertBox.classList.remove("d-none");
  }
  function clearErr() {
    alertBox.classList.add("d-none");
    alertBox.textContent = "";
  }
  function showModalErr(msg) {
    if (!modalAlert) return;
    modalAlert.className = "alert alert-danger";
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function showModalOk(msg) {
    if (!modalAlert) return;
    modalAlert.className = "alert alert-success";
    modalAlert.textContent = msg;
    modalAlert.classList.remove("d-none");
  }
  function clearModalAlert() {
    modalAlert?.classList.add("d-none");
    if (modalAlert) modalAlert.textContent = "";
  }

  function statusViRoom(st) {
    switch (String(st || "")) {
      case "AVAILABLE": return "Sẵn sàng";
      case "UNAVAILABLE": return "Không có sẵn";
      case "MAINTENANCE": return "Bảo trì";
      default: return st || "—";
    }
  }

  function renderRows(items) {
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-muted">Không có kết quả.</td></tr>`;
      return;
    }
    tbody.innerHTML = items
      .map((r) => {
        return `
          <tr data-room-id="${escapeHtml(r.rawId || "")}" role="button" style="cursor:pointer">
            <td><b>${r.roomNumber || "—"}</b></td>
            <td class="text-muted">${r.floor ?? "—"}</td>
            <td class="text-muted">${r.roomTypeName || "—"}</td>
            <td><span class="fw-semibold">${statusViRoom(r.status)}</span></td>
            <td class="text-end"><b>${fmtMoney(r.price)}</b></td>
          </tr>
        `;
      })
      .join("");
  }

  async function runSearch() {
    clearErr();
    const fd = new FormData(form);
    const roomNumber = (fd.get("roomNumber") || "").toString().trim();
    const floor = (fd.get("floor") || "").toString().trim();
    const type = (fd.get("type") || "").toString().trim();
    const q = new URLSearchParams();
    if (roomNumber) q.set("roomNumber", roomNumber);
    if (floor) q.set("floor", floor);
    if (type) q.set("type", type);
    try {
      const items = await api(`/api/reception/rooms${q.toString() ? `?${q}` : ""}`);
      lastItems = Array.isArray(items) ? items : [];
      renderRows(lastItems);
    } catch (err) {
      showErr(err.message || String(err));
    }
  }

  tbody?.addEventListener("click", (e) => {
    const tr = e.target.closest("tr[data-room-id]");
    if (!tr) return;
    const rid = tr.getAttribute("data-room-id");
    if (!rid) return;
    const it = lastItems.find((x) => String(x.rawId) === String(rid));
    if (!it) return;

    selectedRoomId = rid;
    clearModalAlert();
    if (roomNoEl) roomNoEl.textContent = it.roomNumber || "—";
    if (typeEl) typeEl.textContent = it.roomTypeName || "—";
    if (curEl) curEl.textContent = statusViRoom(it.status);
    if (selEl) selEl.value = String(it.status || "AVAILABLE");
    modal?.show();
  });

  btnSave?.addEventListener("click", async () => {
    if (!selectedRoomId) return;
    clearModalAlert();
    const next = selEl ? String(selEl.value || "").trim() : "";
    if (!next) {
      showModalErr("Vui lòng chọn trạng thái.");
      return;
    }
    btnSave.disabled = true;
    try {
      await api(`/api/reception/rooms/${encodeURIComponent(String(selectedRoomId))}/status`, {
        method: "PUT",
        body: JSON.stringify({ status: next }),
      });
      showModalOk("Cập nhật trạng thái thành công.");
      await runSearch();
      setTimeout(() => modal?.hide(), 600);
    } catch (err) {
      showModalErr(err.message || String(err));
    } finally {
      btnSave.disabled = false;
    }
  });

  modalEl?.addEventListener("hidden.bs.modal", () => {
    selectedRoomId = null;
    clearModalAlert();
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    runSearch();
  });

  // load initial list
  runSearch();
}

document.addEventListener("DOMContentLoaded", async () => {
  bootLogout();
  await initLogin();
  await initSearchBookings();
  await initSearchClients();
  await initReceptionRooms();
  await initWalkInBooking();
});

