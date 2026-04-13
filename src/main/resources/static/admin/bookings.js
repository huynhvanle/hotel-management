async function initBookingsPage(apiFn, tokenKey) {
  const tbody = document.querySelector("#bookingsTbody");
  if (!tbody) return;

  const alertBox = document.querySelector("#bookingsAlert");
  const btnReload = document.querySelector("#btnReloadBookings");

  const modalEl = document.querySelector("#bookingModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const titleEl = document.querySelector("#bookingTitle");
  const modalAlert = document.querySelector("#bookingModalAlert");
  const bkClient = document.querySelector("#bkClient");
  const bkClientMeta = document.querySelector("#bkClientMeta");
  const bkEmployee = document.querySelector("#bkEmployee");
  const bkDate = document.querySelector("#bkDate");
  const bkNote = document.querySelector("#bkNote");
  const brTbody = document.querySelector("#bookedRoomsTbody");

  function safe(v, fallback = "—") {
    return v === null || v === undefined || v === "" ? fallback : String(v);
  }

  function renderBookingRow(b) {
    return `
      <tr>
        <td>${b.id}</td>
        <td>${safe(b.bookingDate)}</td>
        <td>
          <div class="fw-semibold">${safe(b.clientName)}</div>
          <div class="small text-muted">${safe(b.clientEmail)}</div>
        </td>
        <td>${safe(b.employeeUsername)}</td>
        <td class="text-end">${safe(b.roomsCount, "0")}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-primary" data-view-booking="${b.id}">View</button>
        </td>
      </tr>
    `;
  }

  function renderBookedRoomRow(br) {
    const checked = br.isCheckedIn === 1;
    return `
      <tr>
        <td>${br.id}</td>
        <td>
          <div class="fw-semibold">${safe(br.roomName, br.roomId)}</div>
          <div class="small text-muted">${safe(br.roomType)} · ${safe(br.roomId)}</div>
        </td>
        <td>${safe(br.checkin)}</td>
        <td>${safe(br.checkout)}</td>
        <td>${checked ? "Yes" : "No"}</td>
        <td class="text-end">
          <button class="btn btn-sm ${checked ? "btn-outline-secondary" : "btn-outline-success"}" data-checkin-br="${br.id}" data-next="${checked ? 0 : 1}">
            ${checked ? "Undo" : "Check-in"}
          </button>
        </td>
      </tr>
    `;
  }

  async function load() {
    alertBox.classList.add("d-none");
    tbody.innerHTML = "";
    try {
      const bookings = await apiFn("/api/staff/bookings");
      tbody.innerHTML = (bookings || []).map(renderBookingRow).join("");
    } catch (e) {
      alertBox.textContent = e.message;
      alertBox.classList.remove("d-none");
    }
  }

  btnReload?.addEventListener("click", load);

  tbody.addEventListener("click", async (e) => {
    const btn = e.target?.closest?.("[data-view-booking]");
    if (!btn) return;
    const id = btn.getAttribute("data-view-booking");
    modalAlert.classList.add("d-none");
    brTbody.innerHTML = "";
    try {
      const bk = await apiFn(`/api/staff/bookings/${id}`);
      titleEl.textContent = `Booking #${bk.id}`;
      bkClient.textContent = safe(bk.clientName);
      bkClientMeta.textContent = `${safe(bk.clientEmail)} · ${safe(bk.clientPhone, "")}`;
      bkEmployee.textContent = safe(bk.employeeUsername);
      bkDate.textContent = `Booking date: ${safe(bk.bookingDate)}`;
      bkNote.textContent = safe(bk.note, "");
      brTbody.innerHTML = (bk.bookedRooms || []).map(renderBookedRoomRow).join("");
      modal?.show();
    } catch (err) {
      modalAlert.textContent = err.message;
      modalAlert.classList.remove("d-none");
    }
  });

  brTbody.addEventListener("click", async (e) => {
    const btn = e.target?.closest?.("[data-checkin-br]");
    if (!btn) return;
    const id = btn.getAttribute("data-checkin-br");
    const next = Number(btn.getAttribute("data-next"));
    try {
      await apiFn(`/api/staff/bookings/booked-rooms/${id}/checkin`, {
        method: "PUT",
        body: JSON.stringify({ isCheckedIn: next }),
      });
      // refresh modal by triggering reload of current booking
      const bkId = (titleEl.textContent || "").match(/#(\d+)/)?.[1];
      if (bkId) {
        const bk = await apiFn(`/api/staff/bookings/${bkId}`);
        brTbody.innerHTML = (bk.bookedRooms || []).map(renderBookedRoomRow).join("");
      }
      await load();
    } catch (err) {
      modalAlert.textContent = err.message;
      modalAlert.classList.remove("d-none");
    }
  });

  await load();
}

document.addEventListener("DOMContentLoaded", async () => {
  if (typeof api !== "function") return;
  await initBookingsPage(api, "hm_admin_token");
});

