async function initRoomsPage(apiFn) {
  const tbody = document.querySelector("#roomsTbody");
  if (!tbody) return;

  const alertBox = document.querySelector("#roomsAlert");
  const btnReload = document.querySelector("#btnReloadRooms");
  const btnAdd = document.querySelector("#btnAddRoom");

  const modalEl = document.querySelector("#roomModal");
  const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  const modalTitle = document.querySelector("#roomModalTitle");
  const form = document.querySelector("#roomForm");
  const formAlert = document.querySelector("#roomAlert");
  const btnSave = document.querySelector("#btnSaveRoom");
  const imgPreview = document.querySelector("#roomImgPreview");
  const imgFile = document.querySelector("#roomImgFile");
  const btnUploadImg = document.querySelector("#btnUploadRoomImg");

  let mode = "create";
  let editId = null;
  let currentRoomIdForImage = null;

  const IMG_BASE = "/hotel-management/uploads/rooms/";
  const IMG_EXTS = [".jpg", ".jpeg", ".png", ".webp"];

  async function setPreviewByRoomId(roomId) {
    currentRoomIdForImage = roomId;
    if (!imgPreview) return;
    if (!roomId) {
      imgPreview.src = "";
      imgPreview.style.display = "none";
      return;
    }
    imgPreview.style.display = "block";
    const ts = Date.now();
    for (const ext of IMG_EXTS) {
      const url = `${IMG_BASE}${encodeURIComponent(roomId)}${ext}?t=${ts}`;
      const ok = await fetch(url, { method: "HEAD" }).then((r) => r.ok).catch(() => false);
      if (ok) {
        imgPreview.src = url;
        return;
      }
    }
    imgPreview.src = "";
    imgPreview.style.display = "none";
  }

  function money(v) {
    if (v == null) return "";
    try {
      return new Intl.NumberFormat("vi-VN").format(Number(v));
    } catch {
      return String(v);
    }
  }

  async function loadHotelsIntoSelect(selectedId) {
    const sel = form.elements.namedItem("hotelId");
    sel.innerHTML = "";
    const hotels = await fetch(`/hotel-management/api/hotels`).then((r) => r.json());
    hotels.forEach((h) => {
      const opt = document.createElement("option");
      opt.value = h.id;
      opt.textContent = `${h.name} (ID=${h.id})`;
      sel.appendChild(opt);
    });
    if (selectedId != null) sel.value = String(selectedId);
  }

  function renderRow(r) {
    return `
      <tr>
        <td>${r.id ?? ""}</td>
        <td>${r.name ?? ""}</td>
        <td>${r.type ?? ""}</td>
        <td class="text-end">${money(r.price)}</td>
        <td>${r.hotelName ?? ""} (ID=${r.hotelId ?? ""})</td>
        <td class="text-end">
          <button class="btn btn-sm btn-outline-primary" data-edit-room="${r.id}">Edit</button>
          <button class="btn btn-sm btn-outline-danger ms-1" data-delete-room="${r.id}">Delete</button>
        </td>
      </tr>
    `;
  }

  async function load() {
    alertBox.classList.add("d-none");
    tbody.innerHTML = "";
    try {
      const rooms = await apiFn("/api/staff/rooms");
      tbody.innerHTML = (rooms || []).map(renderRow).join("");
    } catch (e) {
      alertBox.textContent = e.message;
      alertBox.classList.remove("d-none");
    }
  }

  btnReload?.addEventListener("click", load);

  btnAdd?.addEventListener("click", async () => {
    mode = "create";
    editId = null;
    modalTitle.textContent = "Create room";
    formAlert.classList.add("d-none");
    form.reset();
    form.elements.namedItem("id").disabled = false;
    if (imgFile) imgFile.value = "";
    await setPreviewByRoomId(null);
    await loadHotelsIntoSelect(null);
    modal?.show();
  });

  tbody.addEventListener("click", async (e) => {
    const editBtn = e.target?.closest?.("[data-edit-room]");
    const delBtn = e.target?.closest?.("[data-delete-room]");

    if (editBtn) {
      mode = "edit";
      editId = editBtn.getAttribute("data-edit-room");
      modalTitle.textContent = `Edit room: ${editId}`;
      formAlert.classList.add("d-none");
      try {
        const r = await apiFn(`/api/staff/rooms/${editId}`);
        form.elements.namedItem("id").value = r.id || "";
        form.elements.namedItem("id").disabled = true;
        form.elements.namedItem("name").value = r.name || "";
        form.elements.namedItem("type").value = r.type || "";
        form.elements.namedItem("price").value = r.price ?? "";
        form.elements.namedItem("description").value = r.description || "";
        await loadHotelsIntoSelect(r.hotelId);
        if (imgFile) imgFile.value = "";
        await setPreviewByRoomId(editId);
        modal?.show();
      } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove("d-none");
      }
      return;
    }

    if (delBtn) {
      const id = delBtn.getAttribute("data-delete-room");
      if (!confirm(`Xoá room "${id}"?`)) return;
      alertBox.classList.add("d-none");
      try {
        await apiFn(`/api/staff/rooms/${id}`, { method: "DELETE" });
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
      id: fd.get("id"),
      name: fd.get("name"),
      type: fd.get("type"),
      price: fd.get("price") !== "" ? Number(fd.get("price")) : null,
      hotelId: fd.get("hotelId") !== "" ? Number(fd.get("hotelId")) : null,
      description: fd.get("description") || null,
    };

    try {
      if (mode === "create") {
        await apiFn("/api/staff/rooms", { method: "POST", body: JSON.stringify(payload) });
      } else {
        await apiFn(`/api/staff/rooms/${editId}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      modal?.hide();
      await load();
    } catch (err) {
      formAlert.textContent = err.message;
      formAlert.classList.remove("d-none");
    }
  });

  btnUploadImg?.addEventListener("click", async () => {
    formAlert.classList.add("d-none");
    const roomId = currentRoomIdForImage || form.elements.namedItem("id").value;
    if (!roomId) {
      formAlert.textContent = "Bạn cần có Room ID trước khi upload ảnh.";
      formAlert.classList.remove("d-none");
      return;
    }
    if (!imgFile?.files?.length) {
      formAlert.textContent = "Hãy chọn 1 file ảnh.";
      formAlert.classList.remove("d-none");
      return;
    }
    try {
      const fd = new FormData();
      fd.append("file", imgFile.files[0]);
      const res = await fetch(`/hotel-management/api/staff/rooms/${encodeURIComponent(roomId)}/image`, {
        method: "POST",
        headers: { Authorization: `Bearer ${localStorage.getItem("hm_admin_token")}` },
        body: fd,
      });
      if (!res.ok) {
        const txt = await res.text();
        throw new Error(txt || `HTTP ${res.status}`);
      }
      await setPreviewByRoomId(roomId);
    } catch (e) {
      formAlert.textContent = e.message;
      formAlert.classList.remove("d-none");
    }
  });

  await load();
}

document.addEventListener("DOMContentLoaded", async () => {
  // reuse api() from staff-app.js (global)
  if (typeof api !== "function") return;
  await initRoomsPage(api);
});

