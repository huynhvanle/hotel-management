function qs(sel) {
  return document.querySelector(sel);
}

function money(v) {
  if (v == null || Number.isNaN(Number(v))) return "—";
  return new Intl.NumberFormat("vi-VN").format(Number(v)) + " ₫";
}

function initRevenueUi() {
  const alertBox = qs("#revenueAlert");
  const fromEl = qs("#revenueFrom");
  const toEl = qs("#revenueTo");
  const basisEl = qs("#revenueBasis");
  const totalEl = qs("#revenueTotal");
  const metaEl = qs("#revenueMeta");
  const tbody = qs("#revenueTbody");

  const today = new Date();
  const isoToday = today.toISOString().slice(0, 10);
  const first = new Date(today.getFullYear(), today.getMonth(), 1);
  const isoFirst = first.toISOString().slice(0, 10);
  if (fromEl && !fromEl.value) fromEl.value = isoFirst;
  if (toEl && !toEl.value) toEl.value = isoToday;

  async function load() {
    if (!fromEl?.value || !toEl?.value) {
      alertBox.textContent = "Chọn đủ ngày bắt đầu và kết thúc.";
      alertBox.classList.remove("d-none");
      return;
    }
    alertBox.classList.add("d-none");
    tbody.innerHTML = "";
    try {
      const data = await api(
        `/api/admin/revenue?from=${encodeURIComponent(fromEl.value)}&to=${encodeURIComponent(toEl.value)}`
      );
      if (basisEl) basisEl.textContent = data.basis || "";
      if (totalEl) totalEl.textContent = money(data.totalRevenue);
      if (metaEl) {
        metaEl.textContent = `Khoảng ${data.from} → ${data.to} · ${data.distinctBookingCount} booking · ${data.bookedRoomLineCount} dòng phòng`;
      }
      const lines = data.lines || [];
      tbody.innerHTML = lines
        .map(
          (r) => `
        <tr>
          <td>${r.bookingId ?? ""}</td>
          <td>${r.roomId ?? ""}</td>
          <td>${r.roomName ?? ""}</td>
          <td>${r.bookingDate ?? ""}</td>
          <td>${r.checkin ?? ""}</td>
          <td>${r.checkout ?? ""}</td>
          <td class="text-end">${r.nights ?? 0}</td>
          <td class="text-end">${money(r.amount)}</td>
        </tr>`
        )
        .join("");
      if (lines.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-muted text-center py-3">Không có dữ liệu trong khoảng này.</td></tr>`;
      }
    } catch (e) {
      alertBox.textContent = e.message || String(e);
      alertBox.classList.remove("d-none");
    }
  }

  qs("#btnRevenueLoad")?.addEventListener("click", load);
  return load;
}

/** Gọi sau khi admin.js đã xác thực (tránh race với redirect USER). */
window.addEventListener("load", () => {
  const load = initRevenueUi();
  if (typeof load === "function") load();
});
