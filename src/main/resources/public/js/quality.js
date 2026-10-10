/**
 * Data Quality checks module for QuanLyNhanThan
 */
import { API, showToast } from './api.js';

export function initQualityTab() {
  const btnReload = document.getElementById('btnReloadQuality');
  if (btnReload) {
    btnReload.addEventListener('click', loadQualityChecks);
  }
}

export async function loadQualityChecks() {
  const summaryEl = document.getElementById('qualitySummary');
  const listEl = document.getElementById('qualityList');

  if (!listEl) return;
  listEl.innerHTML = '<div class="text-center py-5"><div class="spinner-custom mx-auto"></div><p class="mt-2 text-muted">Đang kiểm tra chất lượng dữ liệu...</p></div>';

  try {
    const issues = await API.getQuality();
    renderQualityResults(issues, summaryEl, listEl);
  } catch (err) {
    listEl.innerHTML = `<div class="alert alert-danger">Lỗi kiểm tra chất lượng: ${err.message}</div>`;
  }
}

function renderQualityResults(issues, summaryEl, listEl) {
  let errorCount = 0;
  let warnCount = 0;
  let infoCount = 0;
  let passCount = 0;

  issues.forEach(i => {
    if (i.severity === 'ERROR') errorCount++;
    else if (i.severity === 'WARNING') warnCount++;
    else if (i.severity === 'INFO') infoCount++;
    else passCount++;
  });

  if (summaryEl) {
    summaryEl.innerHTML = `
      <div class="d-flex align-items-center gap-3 flex-wrap">
        <span class="badge ${errorCount === 0 ? 'badge-pass' : 'badge-error'} fs-6 p-2">
          <i class="bi ${errorCount === 0 ? 'bi-check-circle' : 'bi-x-circle'}"></i> ${errorCount} Lỗi (ERROR)
        </span>
        <span class="badge ${warnCount === 0 ? 'badge-pass' : 'badge-warning'} fs-6 p-2">
          <i class="bi ${warnCount === 0 ? 'bi-check-circle' : 'bi-exclamation-triangle'}"></i> ${warnCount} Cảnh báo (WARNING)
        </span>
        <span class="badge badge-info fs-6 p-2">
          <i class="bi bi-info-circle"></i> ${infoCount} Ghi chú (INFO)
        </span>
        <span class="ms-auto text-muted small">Tổng kiểm tra: ${issues.length} mục</span>
      </div>
    `;
  }

  listEl.innerHTML = issues.map((item, idx) => {
    let badgeClass = 'badge-pass';
    let icon = 'bi-check-circle-fill text-success';
    let label = 'ĐẠT';

    if (item.severity === 'ERROR') {
      badgeClass = 'badge-error';
      icon = 'bi-x-circle-fill text-danger';
      label = 'LỖI';
    } else if (item.severity === 'WARNING') {
      badgeClass = 'badge-warning';
      icon = 'bi-exclamation-triangle-fill text-warning';
      label = 'CẢNH BÁO';
    } else if (item.severity === 'INFO') {
      badgeClass = 'badge-info';
      icon = 'bi-info-circle-fill text-info';
      label = 'THÔNG TIN';
    }

    const hasItems = item.items && item.items.length > 0;
    const collapseId = `quality-collapse-${idx}`;

    let itemsHtml = '';
    if (hasItems) {
      const itemsList = item.items.map(it => `<li><code>${typeof it === 'object' ? JSON.stringify(it) : it}</code></li>`).join('');
      itemsHtml = `
        <div class="collapse" id="${collapseId}">
          <div class="p-3 border-top border-secondary bg-dark bg-opacity-25 small">
            <div class="fw-bold mb-1 text-muted">Các đối tượng vi phạm / ghi nhận:</div>
            <ul class="mb-0 ps-3">${itemsList}</ul>
          </div>
        </div>
      `;
    }

    return `
      <div class="quality-item">
        <div class="quality-header ${hasItems ? '' : 'pe-none'}" ${hasItems ? `data-bs-toggle="collapse" data-bs-target="#${collapseId}" role="button"` : ''}>
          <div class="d-flex align-items-center gap-2">
            <i class="bi ${icon}"></i>
            <span class="badge bg-dark">${item.code}</span>
            <span class="fw-500">${item.message}</span>
          </div>
          <div class="d-flex align-items-center gap-2">
            <span class="badge ${badgeClass}">${label}</span>
            ${hasItems ? `<span class="badge bg-secondary">${item.items.length} mục</span> <i class="bi bi-chevron-down text-muted small"></i>` : ''}
          </div>
        </div>
        ${itemsHtml}
      </div>
    `;
  }).join('');
}
