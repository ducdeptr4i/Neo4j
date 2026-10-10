/**
 * Kinship resolution module for QuanLyNhanThan
 */
import { API, showToast } from './api.js';
import { highlightPathInMiniGraph } from './graph.js';

export const KINSHIP_EXAMPLES = [
  { id: 'T1', a: 'P011', b: 'P003', desc: 'P011 (Đức) → P003 (Thành): ba (cha) | con', divorced: false },
  { id: 'T2', a: 'P011', b: 'P002', desc: 'P011 (Đức) → P002 (Mai): bà nội | cháu (cháu nội)', divorced: false },
  { id: 'T3', a: 'P019', b: 'P001', desc: 'P019 (Bảo) → P001 (Hòa): cụ ông (cụ nội) | chắt (chắt nội)', divorced: false },
  { id: 'T4', a: 'P011', b: 'P012', desc: 'P011 (Đức) → P012 (Ngọc Anh): em (em gái ruột) | anh', divorced: false },
  { id: 'T5', a: 'P011', b: 'P005', desc: 'P011 (Đức) → P005 (Bình): chú | cháu', divorced: false },
  { id: 'T6', a: 'P011', b: 'P007', desc: 'P011 (Đức) → P007 (Lan): cô | cháu', divorced: false },
  { id: 'T7', a: 'P013', b: 'P011', desc: 'P013 (Nam) → P011 (Đức): anh họ (con bác) | em họ (con chú)', divorced: false },
  { id: 'T8', a: 'P013', b: 'P015', desc: 'P013 (Nam) → P015 (Khang): anh họ (con cô) | em họ (con cậu)', divorced: false },
  { id: 'T9', a: 'P012', b: 'P016', desc: 'P012 (Ngọc Anh) → P016 (Vy): em họ (con cô) | chị họ (con cậu)', divorced: false },
  { id: 'T10', a: 'P020', b: 'P019', desc: 'P020 (Châu) → P019 (Bảo): anh họ (con cậu) | em họ (con cô)', divorced: false },
  { id: 'T11', a: 'P019', b: 'P005', desc: 'P019 (Bảo) → P005 (Bình): ông chú | cháu', divorced: false },
  { id: 'T12', a: 'P019', b: 'P007', desc: 'P019 (Bảo) → P007 (Lan): bà cô | cháu', divorced: false },
  { id: 'T13', a: 'P019', b: 'P014', desc: 'P019 (Bảo) → P014 (Hà): cô họ | cháu họ', divorced: false },
  { id: 'T14', a: 'P004', b: 'P001', desc: 'P004 (Hạnh) → P001 (Hòa): bố chồng (gọi "ba") | con dâu', divorced: false },
  { id: 'T15', a: 'P018', b: 'P004', desc: 'P018 (Sơn) → P004 (Hạnh): mẹ vợ (gọi "mẹ") | con rể', divorced: false },
  { id: 'T16', a: 'P004', b: 'P007', desc: 'P004 (Hạnh) → P007 (Lan): em chồng (cô) | chị dâu', divorced: false },
  { id: 'T17', a: 'P017', b: 'P012', desc: 'P017 (Ngọc) → P012 (Ngọc Anh): em chồng | chị dâu', divorced: false },
  { id: 'T18', a: 'P003', b: 'P010', desc: 'P003 (Thành) → P010 (Phúc): em rể | anh vợ', divorced: false },
  { id: 'T19', a: 'P010', b: 'P008', desc: 'P010 (Phúc) → P008 (Quang): anh em đồng hao (cột chèo)', divorced: false },
  { id: 'T20', a: 'P005', b: 'P006', desc: 'P005 (Bình) → P006 (Thu): Đã ly hôn 2008-08-15', divorced: false },
  { id: 'T21', a: 'P006', b: 'P003', desc: 'P006 (Thu) → P003 (Thành): Ly hôn (bật ly hôn: anh chồng (cũ))', divorced: true },
  { id: 'T22', a: 'P004', b: 'P006', desc: 'P004 (Hạnh) → P006 (Thu): Ly hôn (bật ly hôn: chị em dâu (cũ))', divorced: true },
  { id: 'T23', a: 'P004', b: 'P002', desc: 'P004 (Hạnh) → P002 (Mai): mẹ chồng | con dâu', divorced: false }
];

export function initKinshipTab() {
  const btnSwap = document.getElementById('btnSwapKin');
  if (btnSwap) {
    btnSwap.addEventListener('click', () => {
      const a = document.getElementById('kinA');
      const b = document.getElementById('kinB');
      if (a && b) {
        const tmp = a.value;
        a.value = b.value;
        b.value = tmp;
      }
    });
  }

  const btnFind = document.getElementById('btnFindKin');
  if (btnFind) {
    btnFind.addEventListener('click', performKinshipLookup);
  }

  const btnExample = document.getElementById('btnKinExample');
  if (btnExample) {
    btnExample.addEventListener('click', openKinshipExampleModal);
  }
}

function openKinshipExampleModal() {
  const container = document.getElementById('kinExampleList');
  if (!container) return;

  container.innerHTML = KINSHIP_EXAMPLES.map(ex => `
    <div class="example-card" data-a="${ex.a}" data-b="${ex.b}" data-divorced="${ex.divorced}">
      <div class="d-flex justify-content-between align-items-center">
        <span class="badge bg-primary me-2">${ex.id}</span>
        <span class="fw-500">${ex.desc}</span>
        <i class="bi bi-chevron-right text-muted ms-auto"></i>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.example-card').forEach(card => {
    card.addEventListener('click', () => {
      const a = card.getAttribute('data-a');
      const b = card.getAttribute('data-b');
      const divorced = card.getAttribute('data-divorced') === 'true';

      const selA = document.getElementById('kinA');
      const selB = document.getElementById('kinB');
      const chkDivorced = document.getElementById('kinIncludeDivorced');

      if (selA) selA.value = a;
      if (selB) selB.value = b;
      if (chkDivorced) chkDivorced.checked = divorced;

      // Close modal
      const modalEl = document.getElementById('kinExampleModal');
      // @ts-ignore
      const modal = window.bootstrap.Modal.getInstance(modalEl);
      if (modal) modal.hide();

      performKinshipLookup();
    });
  });

  const modalEl = document.getElementById('kinExampleModal');
  // @ts-ignore
  const modal = new window.bootstrap.Modal(modalEl);
  modal.show();
}

export async function performKinshipLookup() {
  const selA = document.getElementById('kinA');
  const selB = document.getElementById('kinB');
  const chkDivorced = document.getElementById('kinIncludeDivorced');
  const selRegion = document.getElementById('kinRegion');

  if (!selA || !selB) return;
  const idA = selA.value;
  const idB = selB.value;

  if (!idA || !idB) {
    showToast('Vui lòng chọn cả người A và người B', true);
    return;
  }
  if (idA === idB) {
    showToast('A và B phải là hai người khác nhau', true);
    return;
  }

  const includeDivorced = chkDivorced ? chkDivorced.checked : false;
  const region = selRegion ? selRegion.value : 'COMMON';

  const resContainer = document.getElementById('kinResult');
  const resCard = document.getElementById('kinResultCard');
  if (resContainer) resContainer.style.display = 'block';
  if (resCard) resCard.innerHTML = '<div class="text-center py-5"><div class="spinner-custom mx-auto"></div><p class="mt-2 text-muted">Đang phân tích cách xưng hô...</p></div>';

  try {
    const data = await API.getKinship(idA, idB, includeDivorced, region);
    renderKinshipResult(data);
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

function renderKinshipResult(data) {
  const resCard = document.getElementById('kinResultCard');
  if (!resCard) return;

  const a = data.personA;
  const b = data.personB;
  const aToB = data.aToB || {};
  const bToA = data.bToA || {};

  // Headline
  let headlineHtml = '';
  if (data.category === 'NONE') {
    headlineHtml = `<div class="kin-headline text-warning"><i class="bi bi-info-circle"></i> Không tìm thấy quan hệ</div>`;
  } else if (data.category === 'SPOUSE') {
    headlineHtml = `<div class="kin-headline">${a.fullName} và ${b.fullName} là vợ chồng</div>`;
  } else {
    headlineHtml = `
      <div class="kin-headline">
        <span class="text-accent">${a.fullName}</span> gọi <span class="text-accent">${b.fullName}</span> là <strong class="text-warning">"${aToB.call || '?'}"</strong>
        <br />
        <span class="text-accent">${b.fullName}</span> gọi <span class="text-accent">${a.fullName}</span> là <strong class="text-warning">"${bToA.call || '?'}"</strong>
      </div>
    `;
  }

  // Explanation steps
  let stepsHtml = '';
  if (data.explanation && data.explanation.length > 0) {
    stepsHtml = data.explanation.map(step => `<div class="step-item">${step}</div>`).join('');
  }

  // Common ancestors
  let ancestorHtml = '';
  if (data.commonAncestors && data.commonAncestors.length > 0) {
    const rows = data.commonAncestors.map(ca =>
      `<tr><td><code>${ca.personId || ca.ancestorId || 'Không rõ'}</code></td><td>${ca.d1} bước (A lên tổ tiên)</td><td>${ca.d2} bước (B lên tổ tiên)</td></tr>`
    ).join('');
    ancestorHtml = `
      <div class="mt-3">
        <div class="fw-600 small text-accent mb-1"><i class="bi bi-diagram-3"></i> Tổ tiên chung:</div>
        <table class="table table-sm table-custom">
          <thead><tr><th>Tổ tiên ID</th><th>Khoảng cách A</th><th>Khoảng cách B</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    `;
  }

  // Notes
  let notesHtml = '';
  if (data.notes && data.notes.length > 0) {
    notesHtml = data.notes.map(n =>
      `<div class="small text-muted mt-2"><i class="bi bi-chat-left-dots"></i> <em>${n}</em></div>`
    ).join('');
  }

  resCard.innerHTML = `
    <div class="result-callout">
      ${headlineHtml}
      <div class="kin-subtext mt-2">${data.summary || ''}</div>
    </div>

    <div class="row g-3 mb-3">
      <div class="col-md-6">
        <div class="card-glass p-3 h-100">
          <div class="small text-muted text-uppercase fw-600 mb-1">Chiều A → B</div>
          <div class="fw-bold fs-5 text-dark mb-1">Gọi: "${aToB.call || '?'}"</div>
          <div class="small text-accent mb-2">Danh xưng: ${aToB.label || 'Không rõ'}</div>
          <div class="small text-muted mb-1">Tự xưng: "${aToB.selfRefer || '?'}"</div>
          <div class="small fst-italic text-dim">${aToB.sentence || ''}</div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="card-glass p-3 h-100">
          <div class="small text-muted text-uppercase fw-600 mb-1">Chiều B → A</div>
          <div class="fw-bold fs-5 text-dark mb-1">Gọi: "${bToA.call || '?'}"</div>
          <div class="small text-accent mb-2">Danh xưng: ${bToA.label || 'Không rõ'}</div>
          <div class="small text-muted mb-1">Tự xưng: "${bToA.selfRefer || '?'}"</div>
          <div class="small fst-italic text-dim">${bToA.sentence || ''}</div>
        </div>
      </div>
    </div>

    <div class="mb-3">
      <div class="fw-600 small text-accent mb-2"><i class="bi bi-list-ol"></i> Lời giải thích từng bước:</div>
      <div class="ps-1">${stepsHtml}</div>
    </div>

    ${ancestorHtml}
    ${notesHtml}
  `;

  // Mini graph path highlight
  if (data.path && data.path.nodes && data.path.nodes.length > 0) {
    highlightPathInMiniGraph('kinGraph', data.path);
  }
}
