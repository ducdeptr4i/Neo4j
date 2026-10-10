/**
 * Marriage check module for QuanLyNhanThan
 */
import { API, showToast } from './api.js';
import { highlightPathInMiniGraph } from './graph.js';

export const MARRIAGE_EXAMPLES = [
  { id: 'M1', a: 'P003', b: 'P012', desc: 'P003 (Thành) & P012 (Ngọc Anh): ❌ Trực hệ, cha–con', verdict: 'VIOLATION' },
  { id: 'M2', a: 'P001', b: 'P011', desc: 'P001 (Hòa) & P011 (Đức): ❌ Trực hệ, ông–cháu', verdict: 'VIOLATION' },
  { id: 'M3', a: 'P011', b: 'P012', desc: 'P011 (Đức) & P012 (Ngọc Anh): ❌ Anh chị em ruột (đời 2)', verdict: 'VIOLATION' },
  { id: 'M4', a: 'P005', b: 'P012', desc: 'P005 (Bình) & P012 (Ngọc Anh): ❌ Chú–cháu ruột (đời 3)', verdict: 'VIOLATION' },
  { id: 'M5', a: 'P011', b: 'P016', desc: 'P011 (Đức) & P016 (Vy): ❌ Anh chị em họ (đời 3)', verdict: 'VIOLATION' },
  { id: 'M6', a: 'P015', b: 'P012', desc: 'P015 (Khang) & P012 (Ngọc Anh): ❌ Anh chị em họ (đời 3)', verdict: 'VIOLATION' },
  { id: 'M7', a: 'P019', b: 'P020', desc: 'P019 (Bảo) & P020 (Châu): ❌ Anh chị em họ (đời 3)', verdict: 'VIOLATION' },
  { id: 'M8', a: 'P014', b: 'P019', desc: 'P014 (Hà) & P019 (Bảo): ✅ Đời 4 (Cảnh báo: chưa đủ tuổi kết hôn, lệch thế hệ)', verdict: 'ALLOWED' },
  { id: 'M9', a: 'P004', b: 'P018', desc: 'P004 (Hạnh) & P018 (Sơn): ✅ Không huyết thống (Cảnh báo: mẹ vợ–con rể)', verdict: 'ALLOWED' },
  { id: 'M10', a: 'P015', b: 'P017', desc: 'P015 (Khang) & P017 (Ngọc): ✅ Không huyết thống (Cảnh báo: đang có hôn nhân)', verdict: 'ALLOWED' },
  { id: 'M11', a: 'P002', b: 'P018', desc: 'P002 (Mai) & P018 (Sơn): ✅ Cùng họ Lê nhưng không có quan hệ trong DB', verdict: 'ALLOWED' }
];

export function initMarriageTab() {
  const btnSwap = document.getElementById('btnSwapMar');
  if (btnSwap) {
    btnSwap.addEventListener('click', () => {
      const a = document.getElementById('marA');
      const b = document.getElementById('marB');
      if (a && b) {
        const tmp = a.value;
        a.value = b.value;
        b.value = tmp;
      }
    });
  }

  const btnCheck = document.getElementById('btnCheckMar');
  if (btnCheck) {
    btnCheck.addEventListener('click', performMarriageCheck);
  }

  const btnExample = document.getElementById('btnMarExample');
  if (btnExample) {
    btnExample.addEventListener('click', openMarriageExampleModal);
  }
}

function openMarriageExampleModal() {
  const container = document.getElementById('marExampleList');
  if (!container) return;

  container.innerHTML = MARRIAGE_EXAMPLES.map(ex => {
    const badgeClass = ex.verdict === 'VIOLATION' ? 'bg-danger' : 'bg-success';
    return `
      <div class="example-card" data-a="${ex.a}" data-b="${ex.b}">
        <div class="d-flex justify-content-between align-items-center">
          <span class="badge ${badgeClass} me-2">${ex.id}</span>
          <span class="fw-500">${ex.desc}</span>
          <i class="bi bi-chevron-right text-muted ms-auto"></i>
        </div>
      </div>
    `;
  }).join('');

  container.querySelectorAll('.example-card').forEach(card => {
    card.addEventListener('click', () => {
      const a = card.getAttribute('data-a');
      const b = card.getAttribute('data-b');

      const selA = document.getElementById('marA');
      const selB = document.getElementById('marB');
      if (selA) selA.value = a;
      if (selB) selB.value = b;

      const modalEl = document.getElementById('marExampleModal');
      // @ts-ignore
      const modal = window.bootstrap.Modal.getInstance(modalEl);
      if (modal) modal.hide();

      performMarriageCheck();
    });
  });

  const modalEl = document.getElementById('marExampleModal');
  // @ts-ignore
  const modal = new window.bootstrap.Modal(modalEl);
  modal.show();
}

export async function performMarriageCheck() {
  const selA = document.getElementById('marA');
  const selB = document.getElementById('marB');

  if (!selA || !selB) return;
  const idA = selA.value;
  const idB = selB.value;

  if (!idA || !idB) {
    showToast('Vui lòng chọn cả người A và người B', true);
    return;
  }
  if (idA === idB) {
    showToast('Không thể kiểm tra hôn nhân với chính mình', true);
    return;
  }

  const resContainer = document.getElementById('marResult');
  const resCard = document.getElementById('marResultCard');
  if (resContainer) resContainer.style.display = 'block';
  if (resCard) resCard.innerHTML = '<div class="text-center py-5"><div class="spinner-custom mx-auto"></div><p class="mt-2 text-muted">Đang phân tích quan hệ huyết thống theo luật...</p></div>';

  try {
    const data = await API.getMarriageCheck(idA, idB);
    renderMarriageResult(data);
  } catch (err) {
    if (resCard) {
      resCard.innerHTML = `
        <div class="alert alert-danger">
          <i class="bi bi-exclamation-triangle-fill"></i> ${err.message}
        </div>
      `;
    }
  }
}

function renderMarriageResult(data) {
  const resCard = document.getElementById('marResultCard');
  if (!resCard) return;

  const isViolation = data.verdict === 'VIOLATION';
  const bannerClass = isViolation ? 'verdict-violation' : 'verdict-allowed';
  const iconHtml = isViolation
    ? '<i class="bi bi-x-circle-fill text-danger verdict-icon"></i>'
    : '<i class="bi bi-check-circle-fill text-success verdict-icon"></i>';
  const titleText = isViolation
    ? 'VI PHẠM QUY ĐỊNH KẾT HÔN'
    : 'KHÔNG VI PHẠM QUAN HỆ HUYẾT THỐNG';
  const subtitleText = isViolation
    ? 'Có quan hệ huyết thống trong diện cấm kết hôn theo pháp luật Việt Nam.'
    : 'Không phát hiện vi phạm điều kiện huyết thống trong phạm vi 3 đời.';

  const br = data.bloodRelation || {};

  // Thước đo thế hệ
  let gaugeHtml = '';
  if (br.type === 'DIRECT_LINE') {
    gaugeHtml = `<span class="badge bg-danger fs-6 p-2">Dòng máu trực hệ (Mọi đời)</span>`;
  } else if (br.doi !== undefined && br.doi !== null && br.doi > 0) {
    const badgeColor = br.doi <= 3 ? 'bg-danger' : 'bg-success';
    gaugeHtml = `<span class="badge ${badgeColor} fs-6 p-2">Thuộc đời thứ: ${br.doi} / Giới hạn cấm: 3 đời</span>`;
  } else {
    gaugeHtml = `<span class="badge bg-secondary fs-6 p-2">Không phát hiện tổ tiên chung trong DB</span>`;
  }

  // Explanation steps
  let stepsHtml = '';
  if (data.explanation && data.explanation.length > 0) {
    stepsHtml = data.explanation.map(step => `<div class="step-item">${step}</div>`).join('');
  }

  // Cảnh báo bổ sung
  let warningsHtml = '';
  if (data.additionalWarnings && data.additionalWarnings.length > 0) {
    warningsHtml = `
      <div class="mt-3">
        <div class="fw-600 small text-warning mb-2"><i class="bi bi-exclamation-triangle-fill"></i> Cảnh báo bổ sung (điều kiện khác ngoài huyết thống):</div>
        ${data.additionalWarnings.map(w => `
          <div class="warning-badge">
            <i class="bi bi-exclamation-circle-fill mt-1"></i>
            <div>
              <strong>[${w.code}]</strong>: ${w.message}
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  // Common ancestors
  let ancestorText = 'Không có';
  if (br.commonAncestors && br.commonAncestors.length > 0) {
    ancestorText = br.commonAncestors.join(', ');
  }

  resCard.innerHTML = `
    <div class="verdict-banner ${bannerClass}">
      ${iconHtml}
      <div>
        <h4 class="verdict-title">${titleText}</h4>
        <div class="text-muted mt-1">${subtitleText}</div>
        <div class="mt-2">${gaugeHtml}</div>
      </div>
    </div>

    <div class="card-glass p-3 mb-3">
      <div class="row g-3">
        <div class="col-md-6">
          <div class="small text-muted">Loại quan hệ huyết thống:</div>
          <div class="fw-bold text-dark fs-6 mt-1">${br.label || 'Không có'}</div>
        </div>
        <div class="col-md-6">
          <div class="small text-muted">Tổ tiên chung gần nhất:</div>
          <div class="fw-bold text-accent fs-6 mt-1">${ancestorText}</div>
        </div>
        <div class="col-12 border-top pt-2 mt-2">
          <div class="small text-muted">Căn cứ pháp lý:</div>
          <div class="small text-dark fst-italic mt-1">${data.legalBasis || 'Luật Hôn nhân và Gia đình 2014, Điều 5'}</div>
        </div>
      </div>
    </div>

    <div class="mb-3">
      <div class="fw-600 small text-accent mb-2"><i class="bi bi-list-ol"></i> Lời giải thích từng bước:</div>
      <div class="ps-1">${stepsHtml}</div>
    </div>

    ${warningsHtml}
  `;

  // Mini graph path highlight
  if (data.path && data.path.nodes && data.path.nodes.length > 0) {
    highlightPathInMiniGraph('marGraph', data.path);
  }
}
