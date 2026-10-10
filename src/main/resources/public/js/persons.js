/**
 * Persons list and management module for QuanLyNhanThan
 */
import { API, showToast } from './api.js';
import { showPersonDetail } from './graph.js';

let personsCache = [];
let sortCol = 'personId';
let sortAsc = true;

export async function initPersonsTab() {
  setupFilterControls();
  await loadPersonsTable();
}

export async function loadPersonsTable() {
  const tbody = document.getElementById('personsTableBody');
  if (!tbody) return;

  try {
    const list = await API.getPersons();
    personsCache = list;
    renderTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">Lỗi: ${err.message}</td></tr>`;
  }
}

function setupFilterControls() {
  const searchInput = document.getElementById('personSearch');
  if (searchInput) {
    searchInput.addEventListener('input', renderTable);
  }

  const genderFilter = document.getElementById('genderFilter');
  if (genderFilter) {
    genderFilter.addEventListener('change', renderTable);
  }

  const aliveFilter = document.getElementById('aliveFilter');
  if (aliveFilter) {
    aliveFilter.addEventListener('change', renderTable);
  }

  // Column sorting
  const headers = document.querySelectorAll('#personsTable th.sortable');
  headers.forEach(th => {
    th.addEventListener('click', () => {
      const col = th.getAttribute('data-col');
      if (sortCol === col) {
        sortAsc = !sortAsc;
      } else {
        sortCol = col;
        sortAsc = true;
      }
      renderTable();
    });
  });
}

function renderTable() {
  const tbody = document.getElementById('personsTableBody');
  if (!tbody) return;

  const searchInput = document.getElementById('personSearch');
  const genderFilter = document.getElementById('genderFilter');
  const aliveFilter = document.getElementById('aliveFilter');

  const query = (searchInput ? searchInput.value : '').toLowerCase().trim();
  const gender = genderFilter ? genderFilter.value : '';
  const alive = aliveFilter ? aliveFilter.value : '';

  let filtered = personsCache.filter(p => {
    if (query) {
      const matchName = (p.fullName || '').toLowerCase().includes(query);
      const matchId = (p.personId || '').toLowerCase().includes(query);
      const matchJob = (p.occupation || '').toLowerCase().includes(query);
      if (!matchName && !matchId && !matchJob) return false;
    }
    if (gender && p.gender !== gender) return false;
    if (alive === 'alive' && p.deceased) return false;
    if (alive === 'deceased' && !p.deceased) return false;
    return true;
  });

  // Sort
  filtered.sort((a, b) => {
    let valA = a[sortCol] || '';
    let valB = b[sortCol] || '';
    if (valA < valB) return sortAsc ? -1 : 1;
    if (valA > valB) return sortAsc ? 1 : -1;
    return 0;
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Không tìm thấy người phù hợp</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(p => {
    const isMale = p.gender === 'Nam';
    const genderBadge = isMale
      ? '<span class="badge bg-primary">Nam</span>'
      : '<span class="badge bg-danger">Nữ</span>';
    const statusBadge = p.deceased
      ? '<span class="badge bg-secondary">✝ Đã mất</span>'
      : '<span class="badge bg-success">Còn sống</span>';

    return `
      <tr style="cursor:pointer;" data-id="${p.personId}">
        <td><code>${p.personId}</code></td>
        <td class="fw-600 text-dark">${p.fullName}</td>
        <td>${genderBadge}</td>
        <td>${p.dateOfBirth || '-'}</td>
        <td>${p.age !== undefined ? p.age + ' tuổi' : '-'}</td>
        <td>${p.occupation || '-'}</td>
        <td>${statusBadge}</td>
      </tr>
    `;
  }).join('');

  // Row click -> View in graph detail
  tbody.querySelectorAll('tr').forEach(tr => {
    tr.addEventListener('click', () => {
      const id = tr.getAttribute('data-id');
      if (id) {
        // Switch to graph tab
        const tabGraph = document.getElementById('tab-graph');
        if (tabGraph) tabGraph.click();
        showPersonDetail(id);
      }
    });
  });
}
