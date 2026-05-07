/* global bootstrap */

/**
 * File JS cho khu vực KHÁCH (public).
 *
 * Các hàm khởi tạo chính (tự chạy khi DOMContentLoaded):
 * - initIndex(): trang chủ — chọn chi nhánh, bấm "Tới điểm đến" — hiện mô tả chi nhánh + link đặt phòng (room-list)
 * - initRooms(): room-list — catalogue hạng phòng + số lượng & sidebar đặt ngay (POST /api/bookings)
 * - initRoomDetail(): room.html: luồng đặt phòng (1 phòng hoặc nhiều phòng)
 * - initRoomTypeDetail(): room-type-detail: đọc ?type=... và load phòng theo hạng
 *
 * Tất cả API đều đi qua base: CTX + /api/...
 */

const CTX = "/hotel-management";
const CLIENT_TOKEN_KEY = "hm_client_token";

function getClientToken() {
  return localStorage.getItem(CLIENT_TOKEN_KEY);
}

function setClientToken(t) {
  localStorage.setItem(CLIENT_TOKEN_KEY, t);
}

function clearClientToken() {
  localStorage.removeItem(CLIENT_TOKEN_KEY);
}

function initClientNavbarAuth() {
  const loginLink = qs("#clientLoginLink");
  const ordersLink = qs("#clientOrdersLink");
  const menuBtn = qs("#clientUserMenuBtn");
  const profileLink = qs("#clientProfileMenuLink");
  const logoutBtn = qs("#clientLogoutMenuBtn");
  if (!loginLink && !ordersLink && !menuBtn) return;

  const token = getClientToken();
  if (token) {
    if (loginLink) loginLink.classList.add("d-none");
    if (ordersLink) {
      ordersLink.classList.remove("d-none");
      ordersLink.href = ordersLink.getAttribute("data-orders-href") || "./client/profile.html";
    }
    if (menuBtn) menuBtn.classList.remove("d-none");
    if (profileLink) profileLink.href = profileLink.getAttribute("data-profile-href") || "./client/me.html";
    if (logoutBtn) {
      logoutBtn.onclick = () => {
        clearClientToken();
        window.location.href = "./index.html?logout=1";
      };
    }
  } else {
    if (loginLink) {
      loginLink.textContent = "Đăng nhập";
      loginLink.href = `${loginLink.getAttribute("data-login-href") || "./client/login.html"}`;
      loginLink.classList.remove("d-none");
    }
    ordersLink?.classList.add("d-none");
    menuBtn?.classList.add("d-none");
  }
}

/** CCCD (số) → giá trị gửi API (Long). Để trống → null; ký tự không phải số → NaN. */
function parseIdCardNumberInput(raw) {
  const s = String(raw ?? "").trim().replace(/\s/g, "");
  if (!s) return null;
  if (!/^\d+$/.test(s)) return NaN;
  const n = Number(s);
  return Number.isSafeInteger(n) ? n : NaN;
}

/** Lấy 1 phần tử theo selector. */
function qs(sel) {
  return document.querySelector(sel);
}

/** Lấy nhiều phần tử theo selector (trả về mảng). */
function qsa(sel) {
  return Array.from(document.querySelectorAll(sel));
}

/** Format tiền VND để hiển thị UI. */
function fmtMoney(v) {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "-";
  return new Intl.NumberFormat("vi-VN").format(Number(v)) + " ₫";
}

function escapeHtml(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function parseMoneyNumber(txt) {
  const raw = String(txt ?? "");
  const digits = raw.replace(/[^\d]/g, "");
  if (!digits) return 0;
  const n = Number(digits);
  return Number.isFinite(n) ? n : 0;
}

/** Ép về số nguyên (thường dùng cho query params). */
function clampInt(n, fallback = 0) {
  const x = Number(n);
  if (!Number.isFinite(x)) return fallback;
  return Math.trunc(x);
}

/** Tính số ngày giữa 2 yyyy-mm-dd (checkout không tính là 1 đêm). */
function daysBetweenInclusiveStart(checkin, checkout) {
  // expects yyyy-mm-dd strings
  if (!checkin || !checkout) return 0;
  const a = new Date(checkin);
  const b = new Date(checkout);
  const ms = b.getTime() - a.getTime();
  if (!Number.isFinite(ms) || ms <= 0) return 0;
  return Math.ceil(ms / (1000 * 60 * 60 * 24));
}

/** Số đêm lưu trú: checkin → checkout (checkout exclusive), cùng quy ước booking */
function stayNightsFromDates(checkin, checkout) {
  if (!checkin || !checkout || checkout <= checkin) return 0;
  return daysBetweenInclusiveStart(checkin, checkout);
}

/** yyyy-mm-dd → dd/mm/yyyy */
function fmtViDate(iso) {
  if (!iso || typeof iso !== "string") return "—";
  const p = iso.trim().slice(0, 10).split("-");
  if (p.length !== 3) return iso;
  return `${p[2]}/${p[1]}/${p[0]}`;
}

/** Khối HTML thanh toán đặt cọc (QR / CK) sau khi đặt thành công. */
function renderPaymentInfoBlock(info, bookingId, phone) {
  if (!info) return "";
  const tpl = String(info.transferContentTemplate || "");
  let transferContent = tpl;
  if (transferContent.includes("{bookingId}")) {
    transferContent = transferContent.replaceAll("{bookingId}", String(bookingId ?? ""));
  }
  if (transferContent.includes("{phone}")) {
    transferContent = transferContent.replaceAll("{phone}", String(phone ?? ""));
  }
  const qrUrl = info.qrImageUrl ? `${CTX}${info.qrImageUrl}` : "";
  const percent = Number(info.suggestedDepositPercent);
  const percentLine = Number.isFinite(percent)
    ? `<div class="small text-muted">Gợi ý đặt cọc: <b>${escapeHtml(percent)}</b>% tổng tiền.</div>`
    : "";
  return `
      <div class="mt-2">
        <div class="fw-semibold mb-1">Thanh toán đặt cọc (QR/Chuyển khoản)</div>
        <div class="small text-muted mb-2">
          Vui lòng chuyển khoản theo thông tin dưới đây. Nội dung chuyển khoản giúp lễ tân đối chiếu nhanh.
        </div>
        <div class="row g-3 align-items-start">
          <div class="col-12">
            <div class="border rounded-3 p-3 bg-light">
              <div><span class="text-muted">Ngân hàng:</span> <b>${escapeHtml(info.bankName)}</b></div>
              <div><span class="text-muted">Số TK:</span> <b>${escapeHtml(info.accountNumber)}</b></div>
              <div><span class="text-muted">Chủ TK:</span> <b>${escapeHtml(info.accountName)}</b></div>
              <div class="mt-2"><span class="text-muted">Nội dung:</span> <code>${escapeHtml(transferContent || "")}</code></div>
              ${percentLine}
            </div>
          </div>
          <div class="col-12">
            ${qrUrl ? `
              <div class="border rounded-3 p-3 bg-white text-center">
                <div class="small text-muted mb-2">Quét QR để chuyển khoản</div>
                <img src="${escapeHtml(qrUrl)}" alt="QR chuyển khoản" style="max-width: 100%; height: auto;" loading="lazy" />
              </div>` : ""}
          </div>
        </div>
      </div>
    `;
}

function ensureDepositModal() {
  let el = document.getElementById("hmDepositModal");
  if (el) return el;
  const wrap = document.createElement("div");
  wrap.innerHTML = `
    <div class="modal fade" id="hmDepositModal" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Thanh toán đặt cọc</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
          </div>
          <div class="modal-body">
            <div id="hmDepositBody">—</div>
            <div id="hmDepositResult" class="alert d-none mt-3" role="alert"></div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Đóng</button>
            <button type="button" class="btn btn-primary" id="hmDepositConfirmBtn">Xác nhận đặt phòng</button>
          </div>
        </div>
      </div>
    </div>
  `.trim();
  document.body.appendChild(wrap.firstElementChild);
  el = document.getElementById("hmDepositModal");
  return el;
}

async function openDepositModal({ summaryLine, depositAmount, totalAmount, phone, onConfirm }) {
  const modalEl = ensureDepositModal();
  const body = document.getElementById("hmDepositBody");
  const result = document.getElementById("hmDepositResult");
  const confirmBtn = document.getElementById("hmDepositConfirmBtn");

  if (result) {
    result.classList.add("d-none");
    result.textContent = "";
    result.className = "alert d-none mt-3";
  }

  let info = null;
  try {
    info = await apiGet("/api/public/payment-info");
  } catch {
    info = null;
  }

  if (body) {
    body.innerHTML = `
      <div class="small text-muted mb-2">${escapeHtml(summaryLine || "")}</div>
      <div class="border rounded-3 p-3 bg-light">
        <div class="d-flex justify-content-between"><span class="text-muted">Tổng tiền</span><b>${fmtMoney(totalAmount || 0)}</b></div>
        <div class="d-flex justify-content-between"><span class="text-muted">Cọc 50%</span><b>${fmtMoney(depositAmount || 0)}</b></div>
      </div>
      ${info ? `
        <div class="mt-3">
          <div class="fw-semibold mb-1">Thông tin chuyển khoản / QR</div>
          <div class="border rounded-3 p-3 bg-white">
            <div><span class="text-muted">Ngân hàng:</span> <b>${escapeHtml(info.bankName || "")}</b></div>
            <div><span class="text-muted">Số TK:</span> <b>${escapeHtml(info.accountNumber || "")}</b></div>
            <div><span class="text-muted">Chủ TK:</span> <b>${escapeHtml(info.accountName || "")}</b></div>
            <div class="mt-2 small text-muted">Nội dung chuyển khoản sẽ có sau khi tạo đơn.</div>
          </div>
          ${info.qrImageUrl ? `
            <div class="border rounded-3 p-3 bg-white text-center mt-3">
              <div class="small text-muted mb-2">Quét QR để chuyển khoản</div>
              <img src="${escapeHtml(`${CTX}${info.qrImageUrl}`)}" alt="QR chuyển khoản" style="max-width: 100%; height: auto;" loading="lazy" />
            </div>` : ""}
        </div>
      ` : ""}
    `;
  }

  if (confirmBtn) {
    confirmBtn.disabled = false;
    confirmBtn.textContent = "Xác nhận đặt phòng";
    confirmBtn.onclick = async () => {
      if (!onConfirm) return;
      confirmBtn.disabled = true;
      try {
        const created = await onConfirm();
        const bookingId = created?.bookingId ?? created?.bookingID ?? created?.id ?? null;
        if (body) {
          body.innerHTML = `
            <div class="fw-semibold">Đã tạo đơn (chờ xác nhận)</div>
            ${bookingId ? `<div class="small text-muted">Mã booking: <b>#${escapeHtml(bookingId)}</b></div>` : ""}
            <div class="mt-2 border rounded-3 p-3 bg-light">
              <div class="d-flex justify-content-between"><span class="text-muted">Cọc 50%</span><b>${fmtMoney(depositAmount || 0)}</b></div>
            </div>
            ${renderPaymentInfoBlock(info, bookingId, phone)}
          `;
        }
        confirmBtn.textContent = "Xong";
      } catch (e) {
        if (result) {
          result.textContent = e.message || String(e);
          result.className = "alert alert-danger mt-3";
          result.classList.remove("d-none");
        }
        confirmBtn.disabled = false;
      }
    };
  }

  if (modalEl && typeof bootstrap !== "undefined" && bootstrap.Modal) {
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
  }
}

async function openDepositInfoModal({ bookingId, depositAmount, totalAmount, phone }) {
  const modalEl = ensureDepositModal();
  const body = document.getElementById("hmDepositBody");
  const result = document.getElementById("hmDepositResult");
  const confirmBtn = document.getElementById("hmDepositConfirmBtn");

  if (result) {
    result.classList.add("d-none");
    result.textContent = "";
    result.className = "alert d-none mt-3";
  }

  let info = null;
  try {
    info = await apiGet("/api/public/payment-info");
  } catch {
    info = null;
  }

  if (confirmBtn) {
    confirmBtn.classList.add("d-none");
  }

  if (body) {
    body.innerHTML = `
      <div class="fw-semibold">Thanh toán đặt cọc</div>
      ${bookingId ? `<div class="small text-muted">Mã booking: <b>#${escapeHtml(bookingId)}</b></div>` : ""}
      <div class="mt-2 border rounded-3 p-3 bg-light">
        <div class="d-flex justify-content-between"><span class="text-muted">Tổng tiền</span><b>${fmtMoney(totalAmount || 0)}</b></div>
        <div class="d-flex justify-content-between"><span class="text-muted">Cọc 50%</span><b>${fmtMoney(depositAmount || 0)}</b></div>
      </div>
      ${renderPaymentInfoBlock(info, bookingId, phone)}
    `;
  }

  if (modalEl && typeof bootstrap !== "undefined" && bootstrap.Modal) {
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
  }
}

/** Gọi API GET (trả về JSON). Nếu lỗi thì throw Error với message dễ đọc. */
async function apiGet(path) {
  const headers = {};
  const token = getClientToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${CTX}${path}`, { headers });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(txt || `HTTP ${res.status}`);
  }
  return await res.json();
}

/** Gọi API POST (gửi JSON). Nếu lỗi thì throw Error với message dễ đọc. */
async function apiPost(path, body) {
  const headers = { "Content-Type": "application/json" };
  const token = getClientToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${CTX}${path}`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  const isJson = (res.headers.get("content-type") || "").includes("application/json");
  const payload = isJson ? await res.json() : await res.text();
  if (!res.ok) {
    throw new Error((payload && payload.message) || payload || `HTTP ${res.status}`);
  }
  return payload;
}

function requireClientLoginOrRedirect() {
  if (getClientToken()) return true;
  const returnUrl = window.location.pathname + window.location.search;
  window.location.href = `./client/login.html?return=${encodeURIComponent(returnUrl)}`;
  return false;
}

/** Set min=today cho tất cả input[type=date] (không cho chọn ngày quá khứ). */
function setMinDateInputs() {
  const today = new Date();
  const iso = today.toISOString().slice(0, 10);
  qsa('input[type="date"]').forEach((i) => i.setAttribute("min", iso));
}

/** Lấy query param từ URL hiện tại (vd: getParam("id") trong room.html?id=R-101). */
function getParam(name) {
  const u = new URL(window.location.href);
  return u.searchParams.get(name);
}

/** Chuyển form -> query string, bỏ qua field rỗng (vd: ?type=Deluxe&maxPrice=1000000). */
function toQuery(form) {
  const fd = new FormData(form);
  const q = new URLSearchParams();
  for (const [k, v] of fd.entries()) {
    if (v !== null && String(v).trim() !== "") q.set(k, String(v).trim());
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

/** Tìm kiếm tại trang chủ. */
async function initIndex() {
  const form = qs("#searchForm");
  if (!form) return;

  // Auth UI (Trang chủ)
  initClientNavbarAuth();

  // Show one-time notice after login/logout
  try {
    const u = new URL(window.location.href);
    const showLogin = u.searchParams.get("login") === "1";
    const showLogout = u.searchParams.get("logout") === "1";
    if (showLogin || showLogout) {
      const host = qs(".hero-content") || qs("main") || document.body;
      const box = document.createElement("div");
      box.className = showLogin ? "alert alert-success mt-3 mb-0" : "alert alert-light mt-3 mb-0";
      box.textContent = showLogin ? "Đăng nhập thành công." : "Đã đăng xuất.";
      host?.appendChild(box);
      // remove param so refresh doesn't show again
      u.searchParams.delete("login");
      u.searchParams.delete("logout");
      window.history.replaceState({}, "", u.toString());
    }
  } catch {
    // ignore URL issues
  }

  const hint = qs("#searchHint");
  const branchSel = qs("#branchSelect");
  const spotlight = qs("#branchSpotlight");
  const heroLead = qs("#heroLead");

  /** @type {Array<{id:number,name?:string,address?:string,phone?:string,description?:string}>} */
  let hotelsCache = [];

  if (heroLead && heroLead.dataset.defaultText === undefined) {
    heroLead.dataset.defaultText = heroLead.textContent.trim();
  }

  hint.textContent = "Đang tải danh sách chi nhánh...";

  try {
    const hotels = await apiGet("/api/hotels");
    hotelsCache = hotels || [];
    if (branchSel) {
      branchSel.innerHTML = `<option value="">Chọn chi nhánh...</option>`;
      hotelsCache.forEach((h) => {
        const opt = document.createElement("option");
        opt.value = String(h.id);
        const addr = h.address ? ` (${h.address})` : "";
        opt.textContent = `${h.name || "Chi nhánh"}${addr}`;
        branchSel.appendChild(opt);
      });
    }
    hint.textContent =
      "Chọn chi nhánh làm điểm đến, rồi bấm \"Tới điểm đến\" để xem giới thiệu chi nhánh và tiếp tục đặt phòng.";
    hint.classList.remove("text-danger");
  } catch (err) {
    hint.textContent = `Không tải được chi nhánh: ${err.message}`;
    if (branchSel) branchSel.disabled = true;
    return;
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const hotelId = fd.get("hotelId");
    if (!hotelId || String(hotelId).trim() === "") {
      hint.textContent = "Vui lòng chọn chi nhánh.";
      hint.classList.add("text-danger");
      return;
    }
    hint.classList.remove("text-danger");

    const h = hotelsCache.find((x) => String(x.id) === String(hotelId));
    if (!h) {
      hint.textContent = "Không tìm thấy thông tin chi nhánh đã chọn.";
      hint.classList.add("text-danger");
      return;
    }

    const nameEl = qs("#branchSpotlightName");
    const metaEl = qs("#branchSpotlightMeta");
    const descEl = qs("#branchSpotlightDesc");
    const roomsBtn = qs("#branchSpotlightRoomsBtn");

    if (nameEl) nameEl.textContent = h.name || "Chi nhánh";

    const metaBits = [];
    if (h.address) metaBits.push(`Địa chỉ: ${h.address}`);
    if (h.phone) metaBits.push(`Hotline: ${h.phone}`);
    if (metaEl) metaEl.textContent = metaBits.join(" · ");

    const desc = (h.description && String(h.description).trim()) || "";
    if (descEl) {
      descEl.textContent =
        desc || "Chưa có mô tả chi tiết cho chi nhánh này. Vui lòng liên hệ hotline hoặc bấm Đặt phòng để chọn phòng.";
    }

    if (heroLead && heroLead.dataset.defaultText !== undefined) {
      const firstLine = desc.split(/\r?\n/).find((line) => line.trim()) || "";
      if (firstLine) {
        heroLead.textContent =
          firstLine.length > 240 ? `${firstLine.slice(0, 240)}…` : firstLine;
      } else {
        heroLead.textContent = heroLead.dataset.defaultText;
      }
    }

    if (roomsBtn) {
      roomsBtn.textContent = "Đặt phòng";
      roomsBtn.href = `./room-list.html?hotelId=${encodeURIComponent(String(hotelId))}`;
    }

    spotlight?.classList.remove("d-none");
    spotlight?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  });
}

/** Gộp các căn trả về API thành nhóm theo roomTypeId (catalog khách xem). */
function groupRoomsByType(rooms) {
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

/** Một hàng catalogue: ảnh + hạng phòng + giá + ô tăng/giảm số phòng (mặc định 0). */
function renderRoomTypeOfferCard(g, { groupIdx, checkin, checkout } = {}) {
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
  const defaultQty = 0;
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
                    <button type="button" class="btn btn-outline-secondary catalog-qty-minus" data-catalog-group="${gi}" aria-label="Giảm">−</button>
                    <input type="number" class="form-control text-center catalog-qty-input" inputmode="numeric" min="0" max="${maxN}" value="${defaultQty}" data-catalog-group="${gi}" aria-label="Số phòng" />
                    <button type="button" class="btn btn-outline-secondary catalog-qty-plus" data-catalog-group="${gi}" aria-label="Tăng">+</button>
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

/** Render 1 card phòng (internal). Khách không đặt theo số phòng cụ thể. */
function renderRoomCard(r) {
  const typeTitle = escapeHtml(r.roomTypeName || "Loại phòng");
  const price = fmtMoney(r.price);
  const type = r.roomTypeName || "";
  return `
    <div class="col-12 col-md-6 col-lg-4">
      <div class="card room-card h-100">
        <div class="room-card-hero">
          <img class="w-100 h-100" style="object-fit: cover;" alt="${typeTitle}" src="${CTX}/api/rooms/${encodeURIComponent(r.id)}/image" onerror="this.style.display='none'" />
        </div>
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start gap-2">
            <div>
              <div class="small text-muted">Loại phòng</div>
              <div class="h5 mb-1 fw-semibold">${typeTitle}</div>
            </div>
            <div class="text-end">
              <div class="text-muted small">Giá / đêm</div>
              <div class="fw-semibold">${price}</div>
            </div>
          </div>
          <p class="text-muted mt-2 mb-3">Chi nhánh: ${escapeHtml(r.hotelName || "—")}</p>
          <div class="d-grid">
            <a class="btn btn-primary" href="./room-list.html?type=${encodeURIComponent(type)}">Đặt phòng</a>
          </div>
        </div>
      </div>
    </div>
  `;
}
/** Trang room-list: catalogue hạng phòng + sidebar tổng tiền & Đặt phòng → modal khi đã đăng nhập. */
async function initRooms() {
  const grid = qs("#roomsGrid");
  if (!grid) return;

  setMinDateInputs();

  const catalogSection = qs("#catalogBookSection");
  const alertBox = qs("#roomsAlert");
  const form = qs("#filterForm");
  const hotelSelect = qs("#hotelSelect");
  const catalogHotelLine = qs("#catalogHotelLine");
  const catalogDateLine = qs("#catalogDateLine");
  const catalogLines = qs("#catalogLines");
  const catalogTotal = qs("#catalogTotal");
  const catalogBookBtn = qs("#catalogBookBtn");
  // Modal elements (legacy UI) — not used for booking anymore.
  const catalogCheckoutModalEl = qs("#catalogCheckoutModal");
  const catalogBookForm = qs("#catalogBookForm");
  const catalogModalSubmitBtn = qs("#catalogModalSubmitBtn");
  const catalogBookResult = qs("#catalogBookResult");

  /** @type {ReturnType<typeof groupRoomsByType>} */
  let catalogGroupsRef = [];

  function hideCatalogSection() {
    catalogSection?.classList.add("d-none");
    catalogGroupsRef = [];
  }

  try {
    const hotels = await apiGet("/api/hotels");
    (hotels || []).forEach((h) => {
      const opt = document.createElement("option");
      opt.value = String(h.id);
      opt.textContent = `${h.name} (${h.address || "—"})`;
      hotelSelect.appendChild(opt);
    });
  } catch (e) {
    // ignore, rooms can still load
  }

  const u = new URL(window.location.href);
  if (u.searchParams.get("hotelId")) hotelSelect.value = u.searchParams.get("hotelId");
  for (const key of ["type", "minPrice", "maxPrice", "checkin", "checkout"]) {
    const el = form.elements.namedItem(key);
    const v = u.searchParams.get(key);
    if (el && v) el.value = v;
  }
  const typeEl = form.elements.namedItem("type");
  if (typeEl && typeEl.tagName === "SELECT" && u.searchParams.get("type")) {
    const raw = u.searchParams.get("type").trim();
    const canon = ["Standard", "Deluxe", "Suite"].find((t) => t.toLowerCase() === raw.toLowerCase());
    typeEl.value = canon || "";
  }

  const checkinEl = form.elements.namedItem("checkin");
  const checkoutEl = form.elements.namedItem("checkout");

  // Default dates for room-list search: today → tomorrow (if empty).
  if (checkinEl && checkoutEl) {
    const todayIso = new Date().toISOString().slice(0, 10);
    const tmr = new Date(`${todayIso}T12:00:00`);
    tmr.setDate(tmr.getDate() + 1);
    const tmrIso = tmr.toISOString().slice(0, 10);

    if (!checkinEl.value && !checkoutEl.value) {
      checkinEl.value = todayIso;
      checkoutEl.value = tmrIso;
    } else if (checkinEl.value && !checkoutEl.value) {
      const next = new Date(`${checkinEl.value}T12:00:00`);
      next.setDate(next.getDate() + 1);
      checkoutEl.value = next.toISOString().slice(0, 10);
    }
  }

  /** Giữ ngày đi sau ngày đến (ít nhất 1 đêm). */
  function syncCheckoutMinFromCheckin() {
    if (!checkinEl || !checkoutEl) return;
    if (checkinEl.value) {
      const next = new Date(`${checkinEl.value}T12:00:00`);
      next.setDate(next.getDate() + 1);
      checkoutEl.min = next.toISOString().slice(0, 10);
      if (checkoutEl.value && checkoutEl.value <= checkinEl.value) checkoutEl.value = checkoutEl.min;
    } else {
      checkoutEl.removeAttribute("min");
      const today = new Date().toISOString().slice(0, 10);
      checkoutEl.setAttribute("min", today);
    }
  }
  checkinEl?.addEventListener("change", syncCheckoutMinFromCheckin);
  checkoutEl?.addEventListener("change", syncCheckoutMinFromCheckin);
  syncCheckoutMinFromCheckin();

  function clampQtyInput(input) {
    if (!input) return 0;
    const max = Math.max(0, clampInt(input.getAttribute("max"), 0));
    let v = clampInt(input.value, 0);
    if (v < 0) v = 0;
    if (v > max) v = max;
    input.value = String(v);
    return v;
  }

  function readQty(input) {
    if (!input) return 0;
    const max = Math.max(0, clampInt(input.getAttribute("max"), 0));
    let v = clampInt(input.value, 0);
    return Math.min(max, Math.max(0, v));
  }

  function collectCatalogRoomIds() {
    const ids = [];
    catalogGroupsRef.forEach((g, i) => {
      const inp = grid.querySelector(`input.catalog-qty-input[data-catalog-group="${i}"]`);
      const q = readQty(inp);
      ids.push(...g.rooms.slice(0, q).map((r) => r.id));
    });
    return ids;
  }

  function updateCatalogSummary() {
    if (!catalogTotal || !catalogBookBtn) return;

    const fd = new FormData(form);
    const checkin = (fd.get("checkin") || "").trim();
    const checkout = (fd.get("checkout") || "").trim();
    const nights = stayNightsFromDates(checkin, checkout);

    const hid = hotelSelect?.value;
    if (catalogHotelLine) {
      if (hid && hotelSelect.selectedOptions?.[0]) {
        catalogHotelLine.textContent = hotelSelect.selectedOptions[0].textContent.trim();
      } else if (catalogGroupsRef.length) {
        const names = [...new Set(catalogGroupsRef.map((g) => g.hotelName).filter(Boolean))];
        catalogHotelLine.textContent =
          names.length === 1 ? names[0] : names.length ? names.join(", ") : "—";
      } else {
        catalogHotelLine.textContent = "—";
      }
    }

    if (catalogDateLine) {
      if (checkin && checkout) {
        catalogDateLine.innerHTML = `${fmtViDate(checkin)} → ${fmtViDate(checkout)} <span class="hm-muted">(${nights} đêm)</span>`;
      } else {
        catalogDateLine.textContent = "—";
      }
    }

    let sumPerNight = 0;
    const parts = [];
    catalogGroupsRef.forEach((g, i) => {
      const inp = grid.querySelector(`input.catalog-qty-input[data-catalog-group="${i}"]`);
      const q = readQty(inp);
      if (q <= 0) return;
      const sub = q * Number(g.price || 0);
      sumPerNight += sub;
      parts.push(`<div class="d-flex justify-content-between gap-2"><span>${escapeHtml(g.roomTypeName || "Loại phòng")}</span><span>× ${q}</span></div>
        <div class="small hm-muted mb-2">${fmtMoney(g.price)}/đêm · ${fmtMoney(sub)} / đêm (loại này)</div>`);
    });

    if (catalogLines) {
      catalogLines.innerHTML = parts.length ? parts.join("") : '<span class="hm-muted">—</span>';
    }

    const total = nights > 0 ? sumPerNight * nights : 0;
    if (nights > 0 && sumPerNight > 0) {
      catalogTotal.textContent = fmtMoney(total);
    } else if (sumPerNight > 0) {
      catalogTotal.textContent = `${fmtMoney(sumPerNight)} / đêm`;
    } else {
      catalogTotal.textContent = fmtMoney(0);
    }

    catalogBookBtn.disabled = false;
    catalogBookBtn.classList.remove("btn-secondary");
    catalogBookBtn.classList.add("btn-primary");
  }

  grid.addEventListener("click", (e) => {
    const minus = e.target.closest(".catalog-qty-minus");
    const plus = e.target.closest(".catalog-qty-plus");
    const gi = minus?.getAttribute("data-catalog-group") ?? plus?.getAttribute("data-catalog-group");
    if (gi === undefined || gi === null) return;
    const input = grid.querySelector(`input.catalog-qty-input[data-catalog-group="${gi}"]`);
    if (!input) return;
    let v = readQty(input);
    const max = Math.max(0, clampInt(input.getAttribute("max"), 0));
    if (minus) v = Math.max(0, v - 1);
    if (plus) v = Math.min(max, v + 1);
    input.value = String(v);
    updateCatalogSummary();
  });

  grid.addEventListener("change", (e) => {
    const t = e.target;
    if (!t.classList?.contains("catalog-qty-input")) return;
    clampQtyInput(t);
    updateCatalogSummary();
  });

  checkinEl?.addEventListener("input", updateCatalogSummary);
  checkoutEl?.addEventListener("input", updateCatalogSummary);
  hotelSelect?.addEventListener("change", updateCatalogSummary);

  async function load() {
    hideCatalogSection();
    alertBox.classList.add("d-none");
    grid.innerHTML = "";
    try {
      const fd = new FormData(form);
      const checkin = (fd.get("checkin") || "").trim();
      const checkout = (fd.get("checkout") || "").trim();

      const q = new URLSearchParams();
      for (const [k, v] of fd.entries()) {
        if (k === "checkin" || k === "checkout") continue;
        if (v !== null && String(v).trim() !== "") q.set(k, String(v).trim());
      }

      // Bắt buộc phải chọn đủ ngày đến + ngày đi để chỉ hiển thị phòng trống.
      if (!checkin || !checkout) {
        alertBox.textContent = "Vui lòng chọn ngày đến và ngày đi.";
        alertBox.classList.remove("d-none");
        return;
      }
      if (checkout <= checkin) {
        alertBox.textContent = "Ngày đi phải sau ngày đến.";
        alertBox.classList.remove("d-none");
        return;
      }
      q.set("checkin", checkin);
      q.set("checkout", checkout);
      const endpoint = "/api/rooms/available";

      const qsStr = q.toString();
      const rooms = await apiGet(`${endpoint}${qsStr ? `?${qsStr}` : ""}`);
      if (!rooms || rooms.length === 0) {
        alertBox.textContent =
          "Không có phòng trống trong khoảng ngày đã chọn (hoặc không khớp bộ lọc).";
        alertBox.classList.remove("d-none");
        return;
      }
      const groups = groupRoomsByType(rooms);
      catalogGroupsRef = groups;
      catalogSection?.classList.remove("d-none");

      grid.innerHTML = groups
        .map((g, i) => renderRoomTypeOfferCard(g, { groupIdx: i, checkin, checkout }))
        .join("");
      updateCatalogSummary();
    } catch (e) {
      hideCatalogSection();
      alertBox.textContent = `Lỗi tải danh sách phòng: ${e.message}`;
      alertBox.classList.remove("d-none");
    }
  }

  catalogBookBtn?.addEventListener("click", async () => {
    if (!catalogBookBtn) return;
    if (!getClientToken()) {
      requireClientLoginOrRedirect();
      return;
    }
    const fd = new FormData(form);
    const checkin = (fd.get("checkin") || "").trim();
    const checkout = (fd.get("checkout") || "").trim();
    const nights = stayNightsFromDates(checkin, checkout);
    const roomIds = collectCatalogRoomIds();

    const canBook =
      Boolean(checkin && checkout) && nights > 0 && roomIds.length > 0 && catalogGroupsRef.length > 0;

    if (!canBook) {
      if (catalogBookResult) {
        let msg = "Chọn ngày đến / đi, Tìm kiếm, và chọn ít nhất một phòng.";
        if (checkin && checkout && nights <= 0) msg = "Ngày đi phải sau ngày đến.";
        else if (checkin && checkout && roomIds.length === 0) msg = "Chọn ít nhất một phòng.";
        catalogBookResult.textContent = msg;
        catalogBookResult.className = "alert alert-warning mt-3";
        catalogBookResult.classList.remove("d-none");
      }
      return;
    }

    if (catalogBookResult) {
      catalogBookResult.classList.add("d-none");
      catalogBookResult.textContent = "";
    }

    let sumPerNight = 0;
    catalogGroupsRef.forEach((g, i) => {
      const inp = grid.querySelector(`input.catalog-qty-input[data-catalog-group="${i}"]`);
      const q = readQty(inp);
      if (q <= 0) return;
      sumPerNight += q * Number(g.price || 0);
    });
    const total = sumPerNight * nights;
    const deposit = total * 0.5;
    const summaryLine = `${fmtViDate(checkin)} → ${fmtViDate(checkout)} · ${nights} đêm · ${roomIds.length} phòng`;

    let phone = "";
    try {
      const me = await apiGet("/api/client/me");
      phone = me?.phone || "";
    } catch {
      phone = "";
    }

    openDepositModal({
      summaryLine,
      totalAmount: total,
      depositAmount: deposit,
      phone,
      onConfirm: async () => {
        const payload = { roomIds, checkin, checkout, note: "" };
        const created = await apiPost("/api/bookings", payload);
        await load();
        return created;
      },
    });
  });

  // Modal flow disabled: booking happens directly on button click.

  /** Submit filter form: đổi filter -> chuyển URL sang room-list.html?.... để reload theo filter. */
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const q = toQuery(form);
    window.location.href = `./room-list.html${q}`;
  });

  await load();
}

/**
 * Trang room.html:
 * - Nếu KHÔNG có ?id=... => luồng chọn NHIỀU phòng (Step 1 chọn phòng -> Step 2 nhập thông tin -> submit 1 booking)
 * - Nếu CÓ ?id=... => luồng đặt 1 phòng cụ thể (load room detail -> submit booking)
 */
async function initRoomDetail() {
  const nameEl = qs("#roomName");
  if (!nameEl) return;

  // Khách chỉ đặt theo LOẠI phòng, không đặt theo SỐ phòng cụ thể.
  // Chặn toàn bộ luồng room.html (kể cả ?id=...) để tránh hiển thị/đặt theo số phòng.
  window.location.href = "./room-list.html";
  return;

  // setMinDateInputs();

  const id = getParam("id");
  const alertBox = qs("#roomAlert");
  const step1 = qs("#bookingStep1");
  const pickList = qs("#pickRoomsList");
  const pickAlert = qs("#pickRoomsAlert");
  const btnNext = qs("#btnNextToInfo");
  const selectedCount = qs("#selectedCount");
  const selectedTotal = qs("#selectedTotal");
  const btnBack = qs("#btnBackToPick");
  const metaEl = qs("#roomMeta");
  const priceEl = qs("#roomPrice");
  const descEl = qs("#roomDesc");
  const hero = qs(".room-hero");
  const form = qs("#bookingForm");
  const result = qs("#bookingResult");
  const hiddenRoomIds = form ? form.elements.namedItem("roomIds") : null;
  /** Khối ước tính (trình duyệt có thể cache room.html cũ không có #bookingEstimateLine) */
  let estimateLine = qs("#bookingEstimateLine");
  if (!estimateLine && form) {
    const checkoutCol = form.querySelector('input[name="checkout"]')?.closest(".col-6");
    if (checkoutCol) {
      const row = document.createElement("div");
      row.className = "col-12";
      row.innerHTML = `
        <div class="rounded-3 border border-primary border-2 p-3 bg-light small shadow-sm" id="bookingEstimateWrap">
          <div class="text-primary fw-semibold mb-1">Ước tính thanh toán</div>
          <div class="fw-semibold" id="bookingEstimateLine">Chọn ngày nhận / trả để xem tạm tính.</div>
        </div>`;
      checkoutCol.insertAdjacentElement("afterend", row);
      estimateLine = qs("#bookingEstimateLine");
    }
  }

  /** Gắn listener thay đổi ngày (checkin/checkout) để cập nhật dòng ước tính thanh toán. */
  function wireDateEstimate(updateFn) {
    if (!form || !estimateLine) return;
    const ci = form.elements.namedItem("checkin");
    const co = form.elements.namedItem("checkout");
    if (!ci || !co) return;
    const run = () => updateFn();
    ci.addEventListener("change", run);
    co.addEventListener("change", run);
    ci.addEventListener("input", run);
    co.addEventListener("input", run);
    run();
  }

  /** Hiển thị thông báo thành công (alert-success) dưới form đặt phòng. */
  function showResultOkHtml(html) {
    result.innerHTML = html;
    result.classList.add("alert", "alert-success");
    result.classList.remove("alert-danger", "d-none");
  }

  /** Hiển thị thông báo lỗi (alert-danger) dưới form đặt phòng. */
  function showResultErr(msg) {
    result.textContent = msg;
    result.classList.add("alert", "alert-danger");
    result.classList.remove("alert-success", "d-none");
  }

  /**
   * Lấy danh sách roomIds để gửi lên API booking.
   * - Luồng multi-room: đọc từ hidden input name="roomIds" (CSV).
   * - Luồng 1 room: fallbackId = room.id.
   */
  function getRoomIdsFromHiddenOrFallback(fallbackId) {
    if (hiddenRoomIds && hiddenRoomIds.value) {
      const ids = hiddenRoomIds.value.split(",").map((s) => s.trim()).filter(Boolean);
      if (ids.length) return ids;
    }
    return fallbackId ? [fallbackId] : [];
  }

  /** Gắn handler submit cho form booking (mở popup đặt cọc 50% → confirm mới tạo booking). */
  function wireBookingSubmit(getRoomIds) {
    if (!form) return;
    const submitBtn = form.querySelector('button[type="submit"]');

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      result.classList.add("d-none");
      result.classList.remove("alert-success", "alert-danger");

      if (!requireClientLoginOrRedirect()) return;

      if (submitBtn) submitBtn.disabled = true;

      const fd = new FormData(form);
      const checkin = fd.get("checkin");
      const checkout = fd.get("checkout");
      if (!checkin || !checkout) {
        showResultErr("Vui lòng chọn ngày nhận/trả phòng.");
        if (submitBtn) submitBtn.disabled = false;
        return;
      }
      if (checkout <= checkin) {
        showResultErr("Ngày trả phòng phải sau ngày nhận phòng.");
        if (submitBtn) submitBtn.disabled = false;
        return;
      }
      const nights = daysBetweenInclusiveStart(checkin, checkout);

      const roomIds = getRoomIds(fd);
      if (!roomIds || roomIds.length === 0) {
        showResultErr("Vui lòng chọn phòng trước khi đặt.");
        if (submitBtn) submitBtn.disabled = false;
        return;
      }

      try {
        const totalPerNightText = qs("#selectedTotal")?.textContent || "";
        const sumPerNight = parseMoneyNumber(totalPerNightText);
        const total = sumPerNight * nights;
        const deposit = total * 0.5;
        const summaryLine = `${fmtViDate(checkin)} → ${fmtViDate(checkout)} · ${nights} đêm · ${roomIds.length} phòng`;

        let phone = "";
        try {
          const me = await apiGet("/api/client/me");
          phone = me?.phone || "";
        } catch {
          phone = "";
        }

        openDepositModal({
          summaryLine,
          totalAmount: total,
          depositAmount: deposit,
          phone,
          onConfirm: async () => {
            const payload = { roomIds, checkin, checkout, note: fd.get("note") || "" };
            const created = await apiPost("/api/bookings", payload);
            return created;
          },
        });
      } catch (err) {
        showResultErr(`Đặt phòng thất bại: ${err.message}`);
      } finally {
        if (submitBtn) submitBtn.disabled = false;
      }
    });
  }

  if (!id) {
    // ===== Luồng chọn NHIỀU phòng (không có ?id=...) =====
    // Luồng multi-room không cần hero (khối gradient lớn). Ẩn đi để UI gọn hơn.
    if (hero) hero.remove();
    step1.classList.remove("d-none");
    // Ẩn Step 2 (form nhập thông tin) cho tới khi bấm "Kế tiếp"
    qs(".row.g-4")?.classList.add("d-none");

    const urlHotelId = getParam("hotelId");
    const urlRoomTypeId = getParam("roomTypeId");
    const urlCheckin = getParam("checkin");
    const urlCheckout = getParam("checkout");
    if (form) {
      const ciIn = form.elements.namedItem("checkin");
      const coIn = form.elements.namedItem("checkout");
      if (urlCheckin && ciIn) ciIn.value = urlCheckin;
      if (urlCheckout && coIn) coIn.value = urlCheckout;
      if (ciIn?.value && coIn) {
        const next = new Date(`${ciIn.value}T12:00:00`);
        next.setDate(next.getDate() + 1);
        coIn.min = next.toISOString().slice(0, 10);
      }
    }
    const step1H1 = step1.querySelector("h1");
    if (step1H1 && urlRoomTypeId) {
      const hint = document.createElement("p");
      hint.className = "text-muted small mb-0";
      hint.textContent =
        "Chọn căn phòng cụ thể trong hạng phòng bạn đã chọn (đúng chi nhánh và khoảng ngày nếu đã chọn lúc tìm kiếm).";
      step1H1.after(hint);
    }

    /** Snapshot phòng Step 1 — delegated click (không gắn listener lặp mỗi lần load). */
    let pickRoomsCache = [];

    /** Danh sách phòng đã chọn: Map<roomId, roomObject>. */
    const selectedRooms = new Map(); // id -> room

    /** Cập nhật dòng ước tính cho multi-room: số đêm × (tổng giá/đêm của các phòng đã chọn). */
    function updateEstimateMulti() {
      if (!estimateLine) return;
      const ci = form.elements.namedItem("checkin")?.value;
      const co = form.elements.namedItem("checkout")?.value;
      const n = stayNightsFromDates(ci, co);
      if (selectedRooms.size === 0) {
        estimateLine.textContent = "Chọn phòng ở bước trước.";
        return;
      }
      if (!ci || !co) {
        estimateLine.textContent = "Chọn ngày nhận / trả để xem tạm tính.";
        return;
      }
      if (n <= 0) {
        estimateLine.textContent = "Ngày trả phải sau ngày nhận.";
        return;
      }
      let sumPerNight = 0;
      for (const r of selectedRooms.values()) sumPerNight += Number(r.price || 0);
      const total = sumPerNight * n;
      estimateLine.textContent = `${n} đêm · ${selectedRooms.size} phòng · ${fmtMoney(sumPerNight)}/đêm tổng = ${fmtMoney(total)}`;
    }

    /** Render panel tóm tắt bên phải (số phòng đã chọn + tổng giá/đêm). */
    function renderSummary() {
      const count = selectedRooms.size;
      selectedCount.textContent = `${count}`;
      let total = 0;
      for (const r of selectedRooms.values()) total += Number(r.price || 0);
      selectedTotal.textContent = fmtMoney(total);
      btnNext.disabled = selectedRooms.size === 0;
      updateEstimateMulti();
    }

    /** Render 1 card trong Step 1 (nút Chọn/Đã chọn). */
    function renderPickCard(r) {
      const price = fmtMoney(r.price);
      const active = selectedRooms.has(r.id);
      return `
        <div class="col-12">
          <div class="card border-0 shadow-sm ${active ? "border border-primary" : ""}">
            <div class="card-body">
              <div class="d-flex flex-wrap gap-2 justify-content-between align-items-start">
                <div>
                  <div class="small text-muted">Loại phòng</div>
                  <div class="h5 mb-1 fw-semibold">${escapeHtml(r.roomTypeName || "Loại phòng")}</div>
                  <div class="text-muted small">Mã ${escapeHtml(r.id)} · Tầng ${r.floor ?? "—"} · ${escapeHtml(r.hotelName || "—")}</div>
                </div>
                <div class="text-end">
                  <div class="text-muted small">Giá / đêm</div>
                  <div class="h5 mb-0 fw-semibold">${price}</div>
                </div>
              </div>
              <div class="d-flex flex-wrap gap-2 justify-content-end mt-3">
                <a class="btn btn-outline-secondary btn-sm" href="./room.html?id=${encodeURIComponent(r.id)}">Xem</a>
                <button class="btn btn-primary btn-sm" type="button" data-pick-room="${encodeURIComponent(r.id)}">
                  ${active ? "Đã chọn" : "Chọn"}
                </button>
              </div>
            </div>
          </div>
        </div>
      `;
    }

    /**
     * Load phòng để chọn (Step 1): hotelId / roomTypeId từ URL; có ngày trong form => /available.
     */
    async function loadRoomsForPick() {
      pickAlert.classList.add("d-none");
      pickList.innerHTML = "";
      try {
        const fd = form ? new FormData(form) : null;
        const checkin = fd?.get("checkin") ? String(fd.get("checkin")).trim() : "";
        const checkout = fd?.get("checkout") ? String(fd.get("checkout")).trim() : "";

        const params = new URLSearchParams();
        if (urlHotelId) params.set("hotelId", urlHotelId);
        if (urlRoomTypeId) params.set("roomTypeId", urlRoomTypeId);

        let path = "/api/rooms";
        if (checkin && checkout) {
          params.set("checkin", checkin);
          params.set("checkout", checkout);
          path = "/api/rooms/available";
        }

        const qsPart = params.toString();
        const rooms = await apiGet(`${path}${qsPart ? `?${qsPart}` : ""}`);
        if (!rooms || rooms.length === 0) {
          pickAlert.textContent =
            checkin && checkout
              ? "Không có phòng trống trong khoảng ngày đã chọn (hoặc không còn căn cho hạng phòng này)."
              : "Không có phòng phù hợp. Thử chọn đủ ngày nhận/trả ở bước sau rồi quay lại, hoặc kiểm tra chi nhánh.";
          pickAlert.classList.remove("d-none");
          return;
        }
        pickRoomsCache = rooms;
        pickList.innerHTML = pickRoomsCache.map(renderPickCard).join("");
        renderSummary();
      } catch (e) {
        pickAlert.textContent = `Lỗi tải danh sách phòng: ${e.message}`;
        pickAlert.classList.remove("d-none");
      }
    }

    pickList.addEventListener("click", (e) => {
      const btn = e.target?.closest?.("[data-pick-room]");
      if (!btn) return;
      const rid = decodeURIComponent(btn.getAttribute("data-pick-room"));
      const room = pickRoomsCache.find((x) => x.id === rid) || null;
      if (!room) return;
      if (selectedRooms.has(room.id)) selectedRooms.delete(room.id);
      else selectedRooms.set(room.id, room);
      pickList.innerHTML = pickRoomsCache.map(renderPickCard).join("");
      renderSummary();
    });

    /** Bấm "Kế tiếp": chuyển Step 1 -> Step 2 và ghi roomIds vào hidden input. */
    btnNext.addEventListener("click", async () => {
      if (selectedRooms.size === 0) return;
      // show step2 and preload selected room
      step1.classList.add("d-none");
      qs(".row.g-4")?.classList.remove("d-none");
      btnBack.classList.remove("d-none");
      const ids = Array.from(selectedRooms.keys());
      if (hiddenRoomIds) hiddenRoomIds.value = ids.join(",");

      const first = selectedRooms.values().next().value;
      nameEl.textContent =
        ids.length === 1 ? first.roomTypeName || "Loại phòng" : `Bạn đã chọn ${ids.length} phòng`;
      metaEl.textContent =
        ids.length === 1
          ? `Mã phòng: ${first.id} · Tầng: ${first.floor ?? "—"} · Chi nhánh: ${first.hotelName || "—"}`
          : `Danh sách phòng: ${ids.join(", ")}`;
      priceEl.textContent = selectedTotal.textContent;
      const oneDesc =
        ids.length === 1 && first.roomTypeDescription && String(first.roomTypeDescription).trim();
      descEl.textContent =
        ids.length === 1
          ? oneDesc || `Trạng thái: ${first.status || "—"}`
          : "Bạn sẽ đặt cùng lúc các phòng đã chọn ở bước trước.";
      updateEstimateMulti();
    });

    /** Bấm "Quay lại": quay lại Step 1 để chọn lại phòng. */
    btnBack.addEventListener("click", async () => {
      result.classList.add("d-none");
      btnBack.classList.add("d-none");
      qs(".row.g-4")?.classList.add("d-none");
      step1.classList.remove("d-none");
    });

    // Gắn submit cho luồng multi-room
    wireBookingSubmit(() => getRoomIdsFromHiddenOrFallback(null));
    // Gắn ước tính tiền theo ngày cho luồng multi-room
    wireDateEstimate(updateEstimateMulti);

    await loadRoomsForPick();
    return;
  }

  // ===== Luồng đặt 1 phòng cụ thể (có ?id=...) =====
  let room;
  try {
    room = await apiGet(`/api/rooms/${encodeURIComponent(id)}`);
    nameEl.textContent = room.roomTypeName || "Loại phòng";
    metaEl.textContent = `Mã phòng: ${room.id} · Tầng: ${room.floor ?? "—"} · Chi nhánh: ${room.hotelName || "—"}`;
    priceEl.textContent = fmtMoney(room.price);
    const td = room.roomTypeDescription && String(room.roomTypeDescription).trim();
    descEl.textContent = td || `Trạng thái phòng: ${room.status || "—"}`;
    if (hero) {
      hero.innerHTML = `<img class="w-100 h-100" style="object-fit: cover;" alt="Room image" src="${CTX}/api/rooms/${encodeURIComponent(room.id)}/image" onerror="this.remove()" />`;
    }
    if (hiddenRoomIds) hiddenRoomIds.value = room.id;
  } catch (e) {
    alertBox.textContent = `Không tải được phòng: ${e.message}`;
    alertBox.classList.remove("d-none");
    return;
  }

  // Gắn submit cho luồng 1 phòng
  wireBookingSubmit(() => getRoomIdsFromHiddenOrFallback(room.id));
  /** Ước tính tiền cho 1 phòng: số đêm × giá/đêm. */
  wireDateEstimate(() => {
    if (!estimateLine) return;
    const ci = form.elements.namedItem("checkin")?.value;
    const co = form.elements.namedItem("checkout")?.value;
    const n = stayNightsFromDates(ci, co);
    if (!ci || !co) {
      estimateLine.textContent = "Chọn ngày nhận / trả để xem tạm tính.";
      return;
    }
    if (n <= 0) {
      estimateLine.textContent = "Ngày trả phải sau ngày nhận.";
      return;
    }
    const per = Number(room.price || 0);
    const total = per * n;
    estimateLine.textContent = `${n} đêm × ${fmtMoney(per)} = ${fmtMoney(total)}`;
  });
}

async function initRoomTypeDetail() {
  // Trang room-type-detail.html: đọc ?type=..., set text mô tả, và load phòng theo hạng.
  const title = qs("#typeTitle");
  if (!title) return;

  const type = getParam("type");
  const alertBox = qs("#typeAlert");
  const subtitle = qs("#typeSubtitle");
  const fit = qs("#typeFit");
  const listLink = qs("#typeListLink");
  const grid = qs("#typeRoomsGrid");
  const roomsAlert = qs("#typeRoomsAlert");

  if (!type) {
    alertBox.textContent = "Thiếu tham số type.";
    alertBox.classList.remove("d-none");
    return;
  }

  const presets = {
    Standard: {
      subtitle: "Phòng tiêu chuẩn, đầy đủ tiện nghi cơ bản, phù hợp 1-2 người.",
      fit: "Đi công tác, cặp đôi, nhu cầu cơ bản.",
    },
    Deluxe: {
      subtitle: "Không gian rộng hơn, tiện nghi nâng cấp, trải nghiệm thoải mái.",
      fit: "Nghỉ dưỡng, cần không gian thoáng và tiện nghi hơn.",
    },
    Suite: {
      subtitle: "Hạng cao cấp, không gian lớn, phù hợp gia đình/đặc biệt.",
      fit: "Gia đình, kỳ nghỉ dài, trải nghiệm cao cấp.",
    },
  };

  const p = presets[type] || {
    subtitle: `Giới thiệu hạng phòng ${type}.`,
    fit: "Tuỳ nhu cầu của khách.",
  };

  title.textContent = `Hạng phòng: ${type}`;
  subtitle.textContent = p.subtitle;
  fit.textContent = p.fit;
  listLink.href = `./room-list.html?type=${encodeURIComponent(type)}`;

  // Không hiển thị danh sách phòng theo số phòng ở khu vực khách.
  if (grid) grid.innerHTML = "";
  if (roomsAlert) {
    roomsAlert.textContent = "Bấm “Đặt phòng” để xem loại phòng còn trống theo ngày bạn chọn.";
    roomsAlert.classList.remove("d-none");
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  initClientNavbarAuth();
  // Mỗi init* sẽ tự kiểm tra DOM để chỉ chạy ở đúng trang.
  await initIndex();
  await initRooms();
  await initRoomDetail();
  await initRoomTypeDetail();
});

