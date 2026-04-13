function qs(sel) {
  return document.querySelector(sel);
}

document.addEventListener("DOMContentLoaded", async () => {
  const alertBox = qs("#meAlert");
  const saved = qs("#meSaved");
  const form = qs("#meForm");

  // staff-app.js already validated token + role, but we still need data load here
  try {
    const me = await api("/me");
    qs("#meUsername").value = me.username || "";
    qs("#mePosition").value = normalizeRole(me.position);
    form.elements.namedItem("fullName").value = me.fullName || "";
    form.elements.namedItem("mail").value = me.mail || "";
    form.elements.namedItem("description").value = me.description || "";
  } catch (e) {
    alertBox.textContent = e.message;
    alertBox.classList.remove("d-none");
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    alertBox.classList.add("d-none");
    saved.classList.add("d-none");
    const fd = new FormData(form);
    const payload = {
      fullName: fd.get("fullName") || null,
      mail: fd.get("mail") || null,
      description: fd.get("description") || null,
      password: fd.get("password") || null,
    };
    try {
      await api("/me", { method: "PUT", body: JSON.stringify(payload) });
      form.elements.namedItem("password").value = "";
      saved.classList.remove("d-none");
    } catch (err) {
      alertBox.textContent = err.message;
      alertBox.classList.remove("d-none");
    }
  });
});

