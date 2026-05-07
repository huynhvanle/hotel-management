const CTX = "/hotel-management";
const CLIENT_TOKEN_KEY = "hm_client_token";

function qs(sel) {
  return document.querySelector(sel);
}

function getParam(name) {
  const u = new URL(window.location.href);
  return u.searchParams.get(name);
}

function setToken(t) {
  localStorage.setItem(CLIENT_TOKEN_KEY, t);
}

/** Giống app.js: map ô nhập CCCD → Long (để trống → null; có ký tự lạ → NaN). */
function parseIdCardNumberInput(raw) {
  const s = String(raw ?? "").trim().replace(/\s/g, "");
  if (!s) return null;
  if (!/^\d+$/.test(s)) return NaN;
  const n = Number(s);
  return Number.isSafeInteger(n) ? n : NaN;
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

function redirectAfterAuth() {
  // Sau khi đăng nhập/đăng ký: luôn về Trang chủ để khách thấy trạng thái đã đăng nhập.
  // (Không quay về luồng đặt phòng cũ.)
  window.location.href = "../index.html?login=1";
}

async function initLogin() {
  const form = qs("#clientLoginForm");
  if (!form) return;
  const alertBox = qs("#loginAlert");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    alertBox.classList.add("d-none");
    const fd = new FormData(form);
    const payload = {
      phone: (fd.get("phone") || "").trim(),
      password: fd.get("password"),
    };
    try {
      const res = await apiPost("/auth/client/login", payload);
      setToken(res.accessToken);
      redirectAfterAuth();
    } catch (err) {
      alertBox.textContent = err.message || String(err);
      alertBox.classList.remove("d-none");
    }
  });
}

async function initRegister() {
  const form = qs("#clientRegisterForm");
  if (!form) return;
  const alertBox = qs("#registerAlert");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    alertBox.classList.add("d-none");
    const fd = new FormData(form);
    const idParsed = parseIdCardNumberInput(fd.get("idCardNumber"));
    if (Number.isNaN(idParsed)) {
      alertBox.textContent = "CCCD/Hộ chiếu chỉ gồm chữ số hoặc để trống.";
      alertBox.classList.remove("d-none");
      return;
    }
    const payload = {
      fullName: fd.get("fullName"),
      phone: (fd.get("phone") || "").trim(),
      password: fd.get("password"),
      idCardNumber: idParsed,
    };
    try {
      const res = await apiPost("/auth/client/register", payload);
      setToken(res.accessToken);
      redirectAfterAuth();
    } catch (err) {
      alertBox.textContent = err.message || String(err);
      alertBox.classList.remove("d-none");
    }
  });
}

document.addEventListener("DOMContentLoaded", async () => {
  await initLogin();
  await initRegister();
});

