/* global bootstrap */

const CTX = "/hotel-management";

function qs(sel) {
  return document.querySelector(sel);
}

function qsa(sel) {
  return Array.from(document.querySelectorAll(sel));
}

function fmtMoney(v) {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "-";
  return new Intl.NumberFormat("vi-VN").format(Number(v)) + " ₫";
}

function clampInt(n, fallback = 0) {
  const x = Number(n);
  if (!Number.isFinite(x)) return fallback;
  return Math.trunc(x);
}

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

async function apiGet(path) {
  const res = await fetch(`${CTX}${path}`);
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(txt || `HTTP ${res.status}`);
  }
  return await res.json();
}

async function apiPost(path, body) {
  const res = await fetch(`${CTX}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const isJson = (res.headers.get("content-type") || "").includes("application/json");
  const payload = isJson ? await res.json() : await res.text();
  if (!res.ok) {
    throw new Error((payload && payload.message) || payload || `HTTP ${res.status}`);
  }
  return payload;
}

function setMinDateInputs() {
  const today = new Date();
  const iso = today.toISOString().slice(0, 10);
  qsa('input[type="date"]').forEach((i) => i.setAttribute("min", iso));
}

function getParam(name) {
  const u = new URL(window.location.href);
  return u.searchParams.get(name);
}

function toQuery(form) {
  const fd = new FormData(form);
  const q = new URLSearchParams();
  for (const [k, v] of fd.entries()) {
    if (v !== null && String(v).trim() !== "") q.set(k, String(v).trim());
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

async function initIndex() {
  const form = qs("#searchForm");
  if (!form) return;

  setMinDateInputs();

  const hint = qs("#searchHint");
  hint.textContent = "Mẹo: bấm Tìm phòng để xem danh sách phòng và đặt phòng.";

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const q = toQuery(form);
    window.location.href = `./room-list.html${q}`;
  });
}

function renderRoomCard(r) {
  const price = fmtMoney(r.price);
  return `
    <div class="col-12 col-md-6 col-lg-4">
      <div class="card room-card h-100">
        <div class="room-card-hero">
          <img class="w-100 h-100" style="object-fit: cover;" alt="Room image" src="${CTX}/api/rooms/${encodeURIComponent(r.id)}/image" onerror="this.style.display='none'" />
        </div>
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start gap-2">
            <div>
              <div class="small text-muted">${r.type || "Room"}</div>
              <div class="h5 mb-1 fw-semibold">${r.name || r.id}</div>
              <div class="text-muted small">Mã phòng: ${r.id}</div>
            </div>
            <div class="text-end">
              <div class="text-muted small">Giá</div>
              <div class="fw-semibold">${price}</div>
            </div>
          </div>
          <p class="text-muted mt-2 mb-3">${(r.description || "").slice(0, 90) || "—"}</p>
          <div class="d-grid">
            <a class="btn btn-primary" href="./room.html?id=${encodeURIComponent(r.id)}">Xem chi tiết</a>
          </div>
        </div>
      </div>
    </div>
  `;
}

async function initRooms() {
  const grid = qs("#roomsGrid");
  if (!grid) return;

  setMinDateInputs();

  const alertBox = qs("#roomsAlert");
  const form = qs("#filterForm");
  const hotelSelect = qs("#hotelSelect");

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
  for (const key of ["type", "minPrice", "maxPrice"]) {
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

  async function load() {
    alertBox.classList.add("d-none");
    grid.innerHTML = "";
    try {
      const query = u.search ? u.search : toQuery(form);
      const rooms = await apiGet(`/api/rooms${query}`);
      if (!rooms || rooms.length === 0) {
        alertBox.textContent = "Không tìm thấy phòng phù hợp.";
        alertBox.classList.remove("d-none");
        return;
      }
      grid.innerHTML = rooms.map(renderRoomCard).join("");
    } catch (e) {
      alertBox.textContent = `Lỗi tải danh sách phòng: ${e.message}`;
      alertBox.classList.remove("d-none");
    }
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const q = toQuery(form);
    window.location.href = `./room-list.html${q}`;
  });

  await load();
}

async function initRoomDetail() {
  const nameEl = qs("#roomName");
  if (!nameEl) return;

  setMinDateInputs();

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

  function showResultOk(msg) {
    result.textContent = msg;
    result.classList.add("alert", "alert-success");
    result.classList.remove("alert-danger", "d-none");
  }

  function showResultErr(msg) {
    result.textContent = msg;
    result.classList.add("alert", "alert-danger");
    result.classList.remove("alert-success", "d-none");
  }

  function getRoomIdsFromHiddenOrFallback(fallbackId) {
    if (hiddenRoomIds && hiddenRoomIds.value) {
      const ids = hiddenRoomIds.value.split(",").map((s) => s.trim()).filter(Boolean);
      if (ids.length) return ids;
    }
    return fallbackId ? [fallbackId] : [];
  }

  function wireBookingSubmit(getRoomIds) {
    if (!form) return;
    const submitBtn = form.querySelector('button[type="submit"]');

    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      result.classList.add("d-none");
      result.classList.remove("alert-success", "alert-danger");
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
      if (nights > 7) {
        showResultErr("Chỉ cho phép đặt tối đa 7 đêm.");
        if (submitBtn) submitBtn.disabled = false;
        return;
      }

      const roomIds = getRoomIds(fd);
      if (!roomIds || roomIds.length === 0) {
        showResultErr("Vui lòng chọn phòng trước khi đặt.");
        if (submitBtn) submitBtn.disabled = false;
        return;
      }

      const payload = {
        roomIds,
        checkin,
        checkout,
        note: fd.get("note") || "",
        client: {
          idCardNumber: Number(fd.get("idCardNumber")),
          fullName: fd.get("fullName"),
          email: fd.get("email"),
          phone: fd.get("phone") || "",
          address: fd.get("address") || "",
          description: "",
        },
      };

      try {
        const created = await apiPost("/api/bookings", payload);
        showResultOk("Đặt hàng thành công");
        form.reset();
      } catch (err) {
        showResultErr(`Đặt phòng thất bại: ${err.message}`);
      } finally {
        if (submitBtn) submitBtn.disabled = false;
      }
    });
  }

  if (!id) {
    // Step 1: pick room(s), then go to step 2
    step1.classList.remove("d-none");
    // hide step2 room detail until selected
    qs(".row.g-4")?.classList.add("d-none");

    const selectedRooms = new Map(); // id -> room

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
      if (n > 7) {
        estimateLine.textContent = "Chỉ cho phép tối đa 7 đêm.";
        return;
      }
      let sumPerNight = 0;
      for (const r of selectedRooms.values()) sumPerNight += Number(r.price || 0);
      const total = sumPerNight * n;
      estimateLine.textContent = `${n} đêm · ${selectedRooms.size} phòng · ${fmtMoney(sumPerNight)}/đêm tổng = ${fmtMoney(total)}`;
    }

    function renderSummary() {
      const count = selectedRooms.size;
      selectedCount.textContent = `${count}`;
      let total = 0;
      for (const r of selectedRooms.values()) total += Number(r.price || 0);
      selectedTotal.textContent = fmtMoney(total);
      btnNext.disabled = selectedRooms.size === 0;
      updateEstimateMulti();
    }

    function renderPickCard(r) {
      const price = fmtMoney(r.price);
      const active = selectedRooms.has(r.id);
      return `
        <div class="col-12">
          <div class="card border-0 shadow-sm ${active ? "border border-primary" : ""}">
            <div class="card-body">
              <div class="d-flex flex-wrap gap-2 justify-content-between align-items-start">
                <div>
                  <div class="text-muted small">${r.type || "Room"}</div>
                  <div class="h5 mb-1 fw-semibold">${r.name || r.id}</div>
                  <div class="text-muted small">Mã phòng: ${r.id} · Hotel: ${r.hotelName || "—"}</div>
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

    async function loadRoomsForPick() {
      pickAlert.classList.add("d-none");
      pickList.innerHTML = "";
      try {
        // If user already selected dates in step 2, only show available rooms.
        const checkin = form ? new FormData(form).get("checkin") : null;
        const checkout = form ? new FormData(form).get("checkout") : null;
        const rooms = (checkin && checkout)
          ? await apiGet(`/api/rooms/available?checkin=${encodeURIComponent(checkin)}&checkout=${encodeURIComponent(checkout)}`)
          : await apiGet("/api/rooms");
        if (!rooms || rooms.length === 0) {
          pickAlert.textContent = (checkin && checkout)
            ? "Không có phòng trống trong khoảng ngày bạn chọn."
            : "Không có phòng để đặt.";
          pickAlert.classList.remove("d-none");
          return;
        }
        pickList.innerHTML = rooms.map(renderPickCard).join("");
        pickList.addEventListener("click", (e) => {
          const btn = e.target?.closest?.("[data-pick-room]");
          if (!btn) return;
          const rid = decodeURIComponent(btn.getAttribute("data-pick-room"));
          const room = rooms.find((x) => x.id === rid) || null;
          if (!room) return;
          if (selectedRooms.has(room.id)) selectedRooms.delete(room.id);
          else selectedRooms.set(room.id, room);
          pickList.innerHTML = rooms.map(renderPickCard).join("");
          renderSummary();
        });
        renderSummary();
      } catch (e) {
        pickAlert.textContent = `Lỗi tải danh sách phòng: ${e.message}`;
        pickAlert.classList.remove("d-none");
      }
    }

    btnNext.addEventListener("click", async () => {
      if (selectedRooms.size === 0) return;
      // show step2 and preload selected room
      step1.classList.add("d-none");
      qs(".row.g-4")?.classList.remove("d-none");
      btnBack.classList.remove("d-none");
      const ids = Array.from(selectedRooms.keys());
      if (hiddenRoomIds) hiddenRoomIds.value = ids.join(",");

      const first = selectedRooms.values().next().value;
      nameEl.textContent = ids.length === 1 ? (first.name || first.id) : `Bạn đã chọn ${ids.length} phòng`;
      metaEl.textContent = ids.length === 1
        ? `${first.type || "Room"} · Mã: ${first.id} · Hotel: ${first.hotelName || "—"}`
        : `Danh sách phòng: ${ids.join(", ")}`;
      priceEl.textContent = selectedTotal.textContent;
      descEl.textContent = ids.length === 1 ? (first.description || "—") : "Bạn sẽ đặt cùng lúc các phòng đã chọn ở bước trước.";
      updateEstimateMulti();
    });

    btnBack.addEventListener("click", async () => {
      result.classList.add("d-none");
      btnBack.classList.add("d-none");
      qs(".row.g-4")?.classList.add("d-none");
      step1.classList.remove("d-none");
    });

    // IMPORTANT: wire submit for multi-room flow too
    wireBookingSubmit(() => getRoomIdsFromHiddenOrFallback(null));
    wireDateEstimate(updateEstimateMulti);

    await loadRoomsForPick();
    return;
  }

  let room;
  try {
    room = await apiGet(`/api/rooms/${encodeURIComponent(id)}`);
    nameEl.textContent = room.name || room.id;
    metaEl.textContent = `${room.type || "Room"} · Mã: ${room.id} · Hotel: ${room.hotelName || "—"}`;
    priceEl.textContent = fmtMoney(room.price);
    descEl.textContent = room.description || "—";
    if (hero) {
      hero.innerHTML = `<img class="w-100 h-100" style="object-fit: cover;" alt="Room image" src="${CTX}/api/rooms/${encodeURIComponent(room.id)}/image" onerror="this.remove()" />`;
    }
    if (hiddenRoomIds) hiddenRoomIds.value = room.id;
  } catch (e) {
    alertBox.textContent = `Không tải được phòng: ${e.message}`;
    alertBox.classList.remove("d-none");
    return;
  }

  wireBookingSubmit(() => getRoomIdsFromHiddenOrFallback(room.id));
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
    if (n > 7) {
      estimateLine.textContent = "Chỉ cho phép tối đa 7 đêm.";
      return;
    }
    const per = Number(room.price || 0);
    const total = per * n;
    estimateLine.textContent = `${n} đêm × ${fmtMoney(per)} = ${fmtMoney(total)}`;
  });
}

async function initRoomTypeDetail() {
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

  try {
    const rooms = await apiGet(`/api/rooms?type=${encodeURIComponent(type)}`);
    if (!rooms || rooms.length === 0) {
      roomsAlert.textContent = "Chưa có phòng thuộc hạng này.";
      roomsAlert.classList.remove("d-none");
      return;
    }
    grid.innerHTML = rooms.map(renderRoomCard).join("");
  } catch (e) {
    roomsAlert.textContent = `Lỗi tải phòng: ${e.message}`;
    roomsAlert.classList.remove("d-none");
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  await initIndex();
  await initRooms();
  await initRoomDetail();
  await initRoomTypeDetail();
});

