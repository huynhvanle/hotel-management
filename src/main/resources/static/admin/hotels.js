async function initHotelsPage(apiFn) {
  const tbody = document.querySelector("#hotelsTbody");
  if (!tbody) return;

  const alertBox = document.querySelector("#hotelsAlert");
  const btnReload = document.querySelector("#btnReloadHotels");
  const btnAdd = document.querySelector("#btnAddHotel");

  const modalEl = document.querySelector("#hotelModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = document.querySelector("#hotelModalTitle");
  const form = document.querySelector("#hotelForm");
  const formAlert = document.querySelector("#hotelFormAlert");
  const btnSave = document.querySelector("#btnSaveHotel");

  let mode = "create";
  let editId = null;

  function safe(v, fallback = "") {
    return v === null || v === undefined ? fallback : String(v);
  }

  function renderRow(h) {
    return `
      <tr>
        <td>${h.id ?? ""}</td>
        <td>
          <div class="fw-semibold">${safe(h.name, "—")}</div>
          <div class="small text-muted">${safe(h.description, "").slice(0, 60)}</div>
        </td>
        <td>${safe(h.starLevel, "")}</td>
        <td>${safe(h.address, "")}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-primary" data-edit-hotel="${h.id}">Edit</button>
          <button class="btn btn-sm btn-outline-danger ms-1" data-delete-hotel="${h.id}">Delete</button>
        </td>
      </tr>
    `;
  }

  async function load() {
    alertBox.classList.add("d-none");
    tbody.innerHTML = "";
    try {
      const hotels = await apiFn("/api/admin/hotels");
      tbody.innerHTML = (hotels || []).map(renderRow).join("");
    } catch (e) {
      alertBox.textContent = e.message;
      alertBox.classList.remove("d-none");
    }
  }

  btnReload?.addEventListener("click", load);

  btnAdd?.addEventListener("click", () => {
    mode = "create";
    editId = null;
    modalTitle.textContent = "Create hotel";
    formAlert.classList.add("d-none");
    form.reset();
    form.elements.namedItem("id").value = "";
    modal?.show();
  });

  tbody.addEventListener("click", async (e) => {
    const editBtn = e.target?.closest?.("[data-edit-hotel]");
    const delBtn = e.target?.closest?.("[data-delete-hotel]");

    if (editBtn) {
      mode = "edit";
      editId = editBtn.getAttribute("data-edit-hotel");
      modalTitle.textContent = `Edit hotel: ${editId}`;
      formAlert.classList.add("d-none");
      try {
        const h = await apiFn(`/api/admin/hotels/${editId}`);
        form.elements.namedItem("id").value = h.id || "";
        form.elements.namedItem("name").value = h.name || "";
        form.elements.namedItem("starLevel").value = h.starLevel ?? "";
        form.elements.namedItem("address").value = h.address || "";
        form.elements.namedItem("description").value = h.description || "";
        modal?.show();
      } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove("d-none");
      }
      return;
    }

    if (delBtn) {
      const id = delBtn.getAttribute("data-delete-hotel");
      if (!confirm(`Xoá hotel ID=${id}? (Có thể xoá cả rooms thuộc hotel nếu không bị ràng buộc)`)) return;
      alertBox.classList.add("d-none");
      try {
        await apiFn(`/api/admin/hotels/${id}`, { method: "DELETE" });
        await load();
      } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove("d-none");
      }
    }
  });

  btnSave?.addEventListener("click", async () => {
    formAlert.classList.add("d-none");
    const fd = new FormData(form);
    const payload = {
      name: fd.get("name"),
      starLevel: fd.get("starLevel") !== "" ? Number(fd.get("starLevel")) : null,
      address: fd.get("address") || null,
      description: fd.get("description") || null,
    };
    try {
      if (mode === "create") {
        await apiFn("/api/admin/hotels", { method: "POST", body: JSON.stringify(payload) });
      } else {
        await apiFn(`/api/admin/hotels/${editId}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await load();
    } catch (err) {
      formAlert.textContent = err.message;
      formAlert.classList.remove("d-none");
    }
  });

  await load();
}

document.addEventListener("DOMContentLoaded", async () => {
  if (typeof api !== "function") return;
  await initHotelsPage(api);
});

