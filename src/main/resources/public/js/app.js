/**
 * Main application coordinator for QuanLyNhanThan
 */
import { API, showToast } from './api.js';
import { initMainGraph } from './graph.js';
import { initKinshipTab } from './kinship.js';
import { initMarriageTab } from './marriage.js';
import { initPersonsTab } from './persons.js';
import { initQualityTab, loadQualityChecks } from './quality.js';

document.addEventListener('DOMContentLoaded', async () => {
  console.log('Khởi tạo ứng dụng Quản lý Nhân thân Neo4j...');

  // 1. Kiểm tra trạng thái hệ thống
  await checkSystemHealth();

  // 2. Khởi tạo các module
  await initSelectOptions();
  await initMainGraph();
  initKinshipTab();
  initMarriageTab();
  await initPersonsTab();
  initQualityTab();

  // 3. Tab change handlers
  setupTabListeners();
});

async function checkSystemHealth() {
  const statusBadge = document.getElementById('navStatus');
  if (!statusBadge) return;

  try {
    const health = await API.getHealth();
    if (health.neo4j === 'connected') {
      statusBadge.className = 'status-badge status-connected';
      statusBadge.innerHTML = '<span class="status-dot"></span> Neo4j Đã kết nối';
    } else {
      statusBadge.className = 'status-badge status-disconnected';
      statusBadge.innerHTML = '<span class="status-dot"></span> Neo4j Mất kết nối';
    }
  } catch {
    statusBadge.className = 'status-badge status-disconnected';
    statusBadge.innerHTML = '<span class="status-dot"></span> Không thể kết nối API';
  }
}

async function initSelectOptions() {
  const selects = ['kinA', 'kinB', 'marA', 'marB'].map(id => document.getElementById(id));
  if (selects.some(s => !s)) return;

  try {
    const persons = await API.getPersons();

    const optionsHtml = persons.map(p => {
      const year = p.dateOfBirth ? p.dateOfBirth.split('-')[0] : '?';
      const death = p.deceased ? ' ✝' : '';
      return `<option value="${p.personId}">${p.personId} – ${p.fullName} (${p.gender}, sinh ${year}${death})</option>`;
    }).join('');

    selects.forEach(sel => {
      if (sel) sel.innerHTML = optionsHtml;
    });

    // Defaults:
    // Kinship: A = P011 (Đức), B = P005 (Bình)
    const kinA = document.getElementById('kinA');
    const kinB = document.getElementById('kinB');
    if (kinA) kinA.value = 'P011';
    if (kinB) kinB.value = 'P005';

    // Marriage: A = P003 (Thành), B = P012 (Ngọc Anh)
    const marA = document.getElementById('marA');
    const marB = document.getElementById('marB');
    if (marA) marA.value = 'P003';
    if (marB) marB.value = 'P012';

  } catch (err) {
    showToast('Lỗi nạp danh sách chọn người: ' + err.message, true);
  }
}

function setupTabListeners() {
  const tabs = document.querySelectorAll('button[data-bs-toggle="tab"]');
  tabs.forEach(tab => {
    tab.addEventListener('shown.bs.tab', (event) => {
      const targetId = event.target.getAttribute('data-bs-target');
      if (targetId === '#pane-quality') {
        loadQualityChecks();
      } else if (targetId === '#pane-graph') {
        const btnFit = document.getElementById('btnFit');
        if (btnFit) setTimeout(() => btnFit.click(), 100);
      }
    });
  });
}
