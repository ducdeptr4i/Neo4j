/**
 * API client module for QuanLyNhanThan
 */

export async function fetchJson(url, options = {}) {
  try {
    const res = await fetch(url, options);
    if (!res.ok) {
      let errBody;
      try {
        errBody = await res.json();
      } catch {
        errBody = { message: `Lỗi máy chủ (${res.status} ${res.statusText})` };
      }
      const error = new Error(errBody.message || 'Lỗi không xác định');
      error.status = res.status;
      error.data = errBody;
      throw error;
    }
    return await res.json();
  } catch (err) {
    console.error(`[API Error] ${url}:`, err);
    throw err;
  }
}

export function showToast(message, isError = false) {
  const toastEl = document.getElementById('appToast');
  const toastMsg = document.getElementById('toastMsg');
  if (!toastEl || !toastMsg) return;

  toastMsg.textContent = message;
  toastEl.classList.remove('bg-danger', 'bg-success', 'text-white');
  if (isError) {
    toastEl.classList.add('bg-danger', 'text-white');
  } else {
    toastEl.classList.add('bg-success', 'text-white');
  }

  // @ts-ignore
  if (window.bootstrap && window.bootstrap.Toast) {
    const toast = new window.bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
  }
}

export const API = {
  getHealth: () => fetchJson('/api/health'),
  getPersons: () => fetchJson('/api/persons'),
  getPerson: (id) => fetchJson(`/api/persons/${encodeURIComponent(id)}`),
  getGraph: (includeEnded = true) => fetchJson(`/api/graph?includeEnded=${includeEnded}`),
  getKinship: (a, b, includeDivorced = false, region = 'COMMON') =>
    fetchJson(`/api/kinship?a=${encodeURIComponent(a)}&b=${encodeURIComponent(b)}&includeDivorced=${includeDivorced}&region=${encodeURIComponent(region)}`),
  getMarriageCheck: (a, b) =>
    fetchJson(`/api/marriage-check?a=${encodeURIComponent(a)}&b=${encodeURIComponent(b)}`),
  getQuality: () => fetchJson('/api/quality')
};
