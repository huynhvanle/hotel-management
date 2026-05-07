/* global apiGet, clearClientToken, requireClientLoginOrRedirect */

function qs(sel) {
  return document.querySelector(sel);
}

function fmtId(v) {
  if (v === null || v === undefined) return "—";
  const s = String(v).trim();
  return s ? s : "—";
}

document.addEventListener("DOMContentLoaded", async () => {
  const alertBox = qs("#meAlert");
  const fullNameEl = qs("#meFullName");
  const phoneEl = qs("#mePhone");
  const idCardEl = qs("#meIdCard");
  const logoutBtn = qs("#logoutBtn");

  logoutBtn?.addEventListener("click", () => {
    clearClientToken();
    window.location.href = "../index.html?logout=1";
  });

  if (!requireClientLoginOrRedirect()) return;

  try {
    const me = await apiGet("/api/client/me");
    if (fullNameEl) fullNameEl.textContent = me.fullName || "—";
    if (phoneEl) phoneEl.textContent = me.phone || "—";
    if (idCardEl) idCardEl.textContent = fmtId(me.idCardNumber);
  } catch (e) {
    if (alertBox) {
      alertBox.textContent = e.message || String(e);
      alertBox.classList.remove("d-none");
    }
  }
});

