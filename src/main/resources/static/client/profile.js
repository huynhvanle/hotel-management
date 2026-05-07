/* global apiGet, clearClientToken, requireClientLoginOrRedirect */

function qs(sel) {
  return document.querySelector(sel);
}

function fmtId(v) {
  if (v === null || v === undefined) return "—";
  const s = String(v).trim();
  return s ? s : "—";
}

function fmtMoney(v) {
  const n = Number(v);
  if (!Number.isFinite(n)) return "—";
  return new Intl.NumberFormat("vi-VN").format(n) + " ₫";
}

function fmtDate(iso) {
  if (!iso) return "—";
  const s = String(iso).slice(0, 10);
  const p = s.split("-");
  if (p.length !== 3) return s;
  return `${p[2]}/${p[1]}/${p[0]}`;
}

function safeStatus(s) {
  const x = String(s || "").trim();
  return x || "—";
}

function escapeAttr(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

document.addEventListener("DOMContentLoaded", async () => {
  const alertBox = qs("#profileAlert");
  const logoutBtn = qs("#logoutBtn");
  const emptyEl = qs("#bookingsEmpty");
  const tableEl = qs("#bookingsTable");
  const tbodyEl = qs("#bookingsTbody");

  logoutBtn?.addEventListener("click", () => {
    clearClientToken();
    window.location.href = "../index.html?logout=1";
  });

  if (!requireClientLoginOrRedirect()) return;

  try {
    const bookings = await apiGet("/api/client/bookings");
    const list = Array.isArray(bookings) ? bookings : [];
    if (!list.length) {
      emptyEl?.classList.remove("d-none");
      tableEl?.classList.add("d-none");
    } else {
      emptyEl?.classList.add("d-none");
      tableEl?.classList.remove("d-none");
      const me = await apiGet("/api/client/me");
      const phone = (me && me.phone) || "";
      if (tbodyEl) {
        tbodyEl.innerHTML = list
          .map((b) => {
            const roomTypeLines = Array.isArray(b.roomTypes) && b.roomTypes.length
              ? b.roomTypes.map((x) => `${x.roomTypeName || "—"} × ${Number(x.quantity || 0)}`).join(", ")
              : "—";
            const roomLine = b.checkedIn ? (Array.isArray(b.roomIds) ? b.roomIds.join(", ") : "—") : "";
            const dateLine = `${fmtDate(b.checkin)} → ${fmtDate(b.checkout)}`;
            const st = String(b.status || "");
            const canPay = st === "PENDING" && Number(b.depositAmount || 0) > 0;
            const canCancel = st === "PENDING" || st === "CONFIRMED";
            const payBtn = canPay
              ? `<button class="btn btn-primary btn-sm" type="button" data-pay-deposit="1"
                    data-booking-id="${b.bookingId ?? ""}"
                    data-deposit="${Number(b.depositAmount || 0)}"
                    data-total="${Number(b.totalAmount || 0)}"
                    data-phone="${phone}">Thanh toán cọc</button>`
              : "";
            const cancelBtn = canCancel
              ? `<button class="btn btn-outline-secondary btn-sm ms-2" type="button" data-cancel-booking="1"
                    data-booking-id="${b.bookingId ?? ""}"
                    data-status="${escapeAttr(st)}"
                    data-checkin="${escapeAttr(b.checkin || "")}">Huỷ</button>`
              : "";
            return `
              <tr>
                <td><b>#${b.bookingId ?? ""}</b></td>
                <td class="text-muted">${dateLine}</td>
                <td class="text-muted">
                  <div>${escapeAttr(roomTypeLines)}</div>
                  ${roomLine ? `<div class="small hm-muted">Phòng: ${escapeAttr(roomLine)}</div>` : `<div class="small hm-muted">Phòng: Chưa gán</div>`}
                </td>
                <td><span class="fw-semibold">${safeStatus(b.status)}</span></td>
                <td class="text-end"><b>${fmtMoney(b.depositAmount)}</b></td>
                <td class="text-end">${fmtMoney(b.totalAmount)}</td>
                <td class="text-end">${payBtn}${cancelBtn}</td>
              </tr>
            `;
          })
          .join("");

        tbodyEl.addEventListener("click", async (e) => {
          const btn = e.target.closest('button[data-pay-deposit="1"]');
          if (!btn) return;
          const bookingId = btn.getAttribute("data-booking-id");
          const depositAmount = Number(btn.getAttribute("data-deposit") || 0);
          const totalAmount = Number(btn.getAttribute("data-total") || 0);
          const phone = btn.getAttribute("data-phone") || "";
          if (typeof openDepositInfoModal === "function") {
            await openDepositInfoModal({ bookingId, depositAmount, totalAmount, phone });
          }
        });

        tbodyEl.addEventListener("click", async (e) => {
          const btn = e.target.closest('button[data-cancel-booking="1"]');
          if (!btn) return;
          const bookingId = btn.getAttribute("data-booking-id");
          const status = btn.getAttribute("data-status") || "";
          const checkin = btn.getAttribute("data-checkin") || "";

          const today = new Date();
          const ci = checkin ? new Date(`${checkin}T00:00:00`) : null;
          const hoursToCheckin = ci ? Math.floor((ci.getTime() - today.getTime()) / (1000 * 60 * 60)) : null;

          let policy = "Bạn có chắc muốn huỷ đơn này?";
          let allow = true;
          if (status === "PENDING") {
            policy = "Đơn đang chờ xác nhận. Bạn có thể huỷ ngay.";
          } else if (status === "CONFIRMED") {
            if (hoursToCheckin !== null && hoursToCheckin < 48) {
              policy = "Không thể huỷ trong vòng 48h trước ngày nhận phòng. Bạn sẽ mất toàn bộ tiền cọc.";
              allow = false;
            } else {
              policy = "Đơn đã xác nhận. Nếu huỷ trước 2 ngày so với ngày nhận phòng, vui lòng liên hệ khách sạn để được hoàn tiền.";
            }
          }

          if (!allow) {
            alert(policy);
            return;
          }

          if (!confirm(`${policy}\n\nXác nhận huỷ?`)) return;
          try {
            await fetch(`/hotel-management/api/client/bookings/${encodeURIComponent(String(bookingId))}/cancel`, {
              method: "PUT",
              headers: { Authorization: `Bearer ${localStorage.getItem("hm_client_token") || ""}` },
            }).then(async (r) => {
              if (!r.ok) throw new Error((await r.text()) || `HTTP ${r.status}`);
              return r.json();
            });
            window.location.reload();
          } catch (err) {
            alert(err.message || String(err));
          }
        });
      }
    }
  } catch (e) {
    if (alertBox) {
      alertBox.textContent = e.message || String(e);
      alertBox.classList.remove("d-none");
    }
  }
});

