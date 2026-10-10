/**
 * Vis-network graph module for QuanLyNhanThan
 */
import { API, showToast } from './api.js';

let mainNetwork = null;
let graphRawData = null;

export async function initMainGraph() {
  const container = document.getElementById('graphNetwork');
  if (!container) return;

  setupGraphControls();
  await loadAndRenderGraph();
}

export async function loadAndRenderGraph() {
  const switchShowEnded = document.getElementById('switchShowEnded');
  const includeEnded = switchShowEnded ? switchShowEnded.checked : true;

  try {
    const data = await API.getGraph(includeEnded);
    graphRawData = data;
    renderVisNetwork('graphNetwork', data);
  } catch (err) {
    showToast('Lỗi khi tải sơ đồ gia phả: ' + err.message, true);
  }
}

function setupGraphControls() {
  const btnFit = document.getElementById('btnFit');
  if (btnFit) {
    btnFit.addEventListener('click', () => {
      if (mainNetwork) mainNetwork.fit({ animation: { duration: 500 } });
    });
  }

  const btnZoomIn = document.getElementById('btnZoomIn');
  if (btnZoomIn) {
    btnZoomIn.addEventListener('click', () => {
      if (mainNetwork) {
        const scale = mainNetwork.getScale();
        mainNetwork.moveTo({ scale: scale * 1.25, animation: { duration: 300 } });
      }
    });
  }

  const btnZoomOut = document.getElementById('btnZoomOut');
  if (btnZoomOut) {
    btnZoomOut.addEventListener('click', () => {
      if (mainNetwork) {
        const scale = mainNetwork.getScale();
        mainNetwork.moveTo({ scale: scale * 0.8, animation: { duration: 300 } });
      }
    });
  }

  const switchShowEnded = document.getElementById('switchShowEnded');
  if (switchShowEnded) {
    switchShowEnded.addEventListener('change', () => {
      loadAndRenderGraph();
    });
  }
}

function formatNodeLabel(node) {
  const birthYear = node.dateOfBirth ? node.dateOfBirth.split('-')[0] : '?';
  const deathYear = node.dateOfDeath ? ' – ✝' + node.dateOfDeath.split('-')[0] : '';
  const job = node.occupation ? `\n[${node.occupation}]` : '';
  return `${node.fullName}\n(${birthYear}${deathYear})${job}`;
}

function getNodeColor(node) {
  if (node.deceased) {
    return {
      background: '#475569',
      border: '#94a3b8',
      highlight: { background: '#64748b', border: '#cbd5e1' }
    };
  }
  if (node.gender === 'Nam') {
    return {
      background: '#1d4ed8',
      border: '#38bdf8',
      highlight: { background: '#2563eb', border: '#7dd3fc' }
    };
  } else {
    return {
      background: '#be185d',
      border: '#f472b6',
      highlight: { background: '#db2777', border: '#fbcfe8' }
    };
  }
}

export function renderVisNetwork(containerId, data, highlightPath = null) {
  const container = document.getElementById(containerId);
  if (!container || !window.vis) return;

  const highlightedNodes = new Set(highlightPath ? highlightPath.nodes : []);
  const highlightedEdges = new Set();
  if (highlightPath && highlightPath.edges) {
    highlightPath.edges.forEach(e => {
      highlightedEdges.add(`${e.from}->${e.to}`);
      highlightedEdges.add(`${e.to}->${e.from}`);
    });
  }

  const visNodes = data.nodes.map(n => {
    const isHighlighted = highlightedNodes.has(n.id || n.personId);
    const hasHighlight = highlightedNodes.size > 0;
    const baseColor = getNodeColor(n);

    let opacity = 1.0;
    if (hasHighlight && !isHighlighted) {
      opacity = 0.25;
    }

    return {
      id: n.id || n.personId,
      label: formatNodeLabel(n),
      level: n.generation !== undefined ? n.generation : 0,
      shape: 'box',
      margin: 10,
      shapeProperties: { borderRadius: 8 },
      color: {
        background: isHighlighted ? '#06b6d4' : baseColor.background,
        border: isHighlighted ? '#22d3ee' : baseColor.border,
        highlight: baseColor.highlight
      },
      font: {
        color: '#f8fafc',
        size: 13,
        face: 'Be Vietnam Pro, Inter, sans-serif'
      },
      opacity: opacity,
      shadow: isHighlighted ? { enabled: true, color: '#06b6d4', size: 15 } : { enabled: true, color: 'rgba(0,0,0,0.5)', size: 8 }
    };
  });

  const visEdges = data.edges.map(e => {
    const edgeKey = `${e.from}->${e.to}`;
    const isHighlighted = highlightedEdges.has(edgeKey);
    const hasHighlight = highlightedNodes.size > 0;

    let color = e.color || '#94a3b8';
    let width = 2;
    let opacity = 1.0;

    if (hasHighlight) {
      if (isHighlighted) {
        color = '#38bdf8';
        width = 4;
        opacity = 1.0;
      } else {
        opacity = 0.15;
      }
    } else {
      if (e.type === 'MARRIAGE') {
        width = 2.5;
      }
    }

    return {
      id: e.id || `e_${e.from}_${e.to}`,
      from: e.from,
      to: e.to,
      arrows: e.type === 'PARENT' ? 'to' : '',
      color: { color: color, opacity: opacity, highlight: '#38bdf8' },
      dashes: !!e.dashes,
      width: width,
      smooth: { type: 'cubicBezier', forceDirection: 'vertical', roundness: 0.4 }
    };
  });

  const visData = {
    nodes: new window.vis.DataSet(visNodes),
    edges: new window.vis.DataSet(visEdges)
  };

  const options = {
    layout: {
      hierarchical: {
        direction: 'UD',
        sortMethod: 'directed',
        levelSeparation: 120,
        nodeSpacing: 180,
        treeSpacing: 220
      }
    },
    physics: {
      hierarchicalRepulsion: {
        nodeDistance: 180
      }
    },
    interaction: {
      hover: true,
      tooltipDelay: 200,
      zoomView: true,
      dragView: true
    }
  };

  const network = new window.vis.Network(container, visData, options);

  if (containerId === 'graphNetwork') {
    mainNetwork = network;
    network.on('click', async (params) => {
      if (params.nodes && params.nodes.length > 0) {
        const personId = params.nodes[0];
        await showPersonDetail(personId);
      }
    });
  }

  return network;
}

export async function showPersonDetail(personId) {
  const panel = document.getElementById('personDetailPanel');
  if (!panel) return;

  panel.innerHTML = '<div class="text-center py-5"><div class="spinner-custom mx-auto"></div></div>';

  try {
    const p = await API.getPerson(personId);
    renderPersonDetailHtml(panel, p);
  } catch (err) {
    panel.innerHTML = `<div class="p-3 text-danger">Lỗi: ${err.message}</div>`;
  }
}

function renderPersonDetailHtml(panel, p) {
  const avatarClass = p.deceased ? 'avatar-deceased' : (p.gender === 'Nam' ? 'avatar-male' : 'avatar-female');
  const initial = p.fullName ? p.fullName.split(' ').pop().charAt(0) : '?';
  const statusBadge = p.deceased
    ? '<span class="detail-badge bg-secondary text-white"><i class="bi bi-cross"></i> Đã mất</span>'
    : '<span class="detail-badge bg-success text-white"><i class="bi bi-heart-pulse"></i> Còn sống</span>';

  const birthStr = p.dateOfBirth || 'Không rõ';
  const deathStr = p.dateOfDeath ? `<div class="small text-muted mt-1"><i class="bi bi-clock-history"></i> Mất: ${p.dateOfDeath}</div>` : '';

  // Cha mẹ
  let parentsHtml = '<span class="text-muted small">Chưa có thông tin</span>';
  if (p.parents && p.parents.length > 0) {
    parentsHtml = p.parents.map(par =>
      `<button class="relation-chip" data-id="${par.personId}">
        <i class="bi bi-person-fill ${par.gender === 'Nam' ? 'text-primary' : 'text-danger'}"></i>
        ${par.fullName} (${par.gender === 'Nam' ? 'Cha' : 'Mẹ'})
      </button>`
    ).join('');
  }

  // Vợ / Chồng
  let marriagesHtml = '<span class="text-muted small">Không có</span>';
  if (p.marriages && p.marriages.length > 0) {
    marriagesHtml = p.marriages.map(m => {
      let badge = '';
      if (m.status === 'ACTIVE') badge = '<span class="badge bg-success ms-1">Đang hôn nhân</span>';
      else if (m.status === 'DEATH') badge = '<span class="badge bg-secondary ms-1">Góa</span>';
      else badge = '<span class="badge bg-danger ms-1">Ly hôn</span>';

      return `<div class="mb-2">
        <button class="relation-chip" data-id="${m.personId}">
          <i class="bi bi-heart-fill text-danger"></i> ${m.fullName || m.personId}
        </button>
        ${badge}
        <div class="small text-muted ms-1">Từ ${m.startDate || '?'}${m.endDate ? ' đến ' + m.endDate : ''}</div>
      </div>`;
    }).join('');
  }

  // Con
  let childrenHtml = '<span class="text-muted small">Không có</span>';
  if (p.children && p.children.length > 0) {
    childrenHtml = p.children.map(ch =>
      `<button class="relation-chip" data-id="${ch.personId}">
        <i class="bi bi-person ${ch.gender === 'Nam' ? 'text-primary' : 'text-danger'}"></i>
        ${ch.fullName} (${ch.gender})
      </button>`
    ).join('');
  }

  // Anh chị em
  let siblingsHtml = '<span class="text-muted small">Không có</span>';
  if (p.siblings && p.siblings.length > 0) {
    siblingsHtml = p.siblings.map(sib =>
      `<button class="relation-chip" data-id="${sib.personId}">
        <i class="bi bi-people ${sib.gender === 'Nam' ? 'text-primary' : 'text-danger'}"></i>
        ${sib.fullName} (${sib.gender})
      </button>`
    ).join('');
  }

  panel.innerHTML = `
    <div class="p-3">
      <div class="d-flex align-items-center gap-3 person-detail-header">
        <div class="person-avatar ${avatarClass}">${initial}</div>
        <div>
          <h5 class="fw-bold mb-1">${p.fullName}</h5>
          <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="badge bg-secondary">${p.personId}</span>
            <span class="badge ${p.gender === 'Nam' ? 'bg-primary' : 'bg-danger'}">${p.gender}</span>
            ${statusBadge}
          </div>
        </div>
      </div>

      <div class="mb-3">
        <div class="small text-muted"><i class="bi bi-calendar-event"></i> Sinh ngày: <span class="text-dark fw-bold">${birthStr}</span></div>
        ${deathStr}
        <div class="small text-muted mt-1"><i class="bi bi-briefcase"></i> Nghề nghiệp: <span class="text-dark fw-bold">${p.occupation || 'Không rõ'}</span></div>
      </div>

      <div class="mb-3">
        <div class="fw-600 small text-accent mb-1"><i class="bi bi-arrow-up-circle"></i> Cha Mẹ:</div>
        <div>${parentsHtml}</div>
      </div>

      <div class="mb-3">
        <div class="fw-600 small text-accent mb-1"><i class="bi bi-heart"></i> Hôn nhân:</div>
        <div>${marriagesHtml}</div>
      </div>

      <div class="mb-3">
        <div class="fw-600 small text-accent mb-1"><i class="bi bi-arrow-down-circle"></i> Con cái:</div>
        <div>${childrenHtml}</div>
      </div>

      <div>
        <div class="fw-600 small text-accent mb-1"><i class="bi bi-people"></i> Anh/chị/em:</div>
        <div>${siblingsHtml}</div>
      </div>
    </div>
  `;

  // Gắn sự kiện click vào các chip người thân để chuyển nhanh sang người đó
  panel.querySelectorAll('.relation-chip').forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-id');
      if (targetId) {
        showPersonDetail(targetId);
        if (mainNetwork) {
          mainNetwork.selectNodes([targetId]);
          mainNetwork.focus(targetId, { animation: { duration: 500 }, scale: 1.1 });
        }
      }
    });
  });
}

export function highlightPathInMiniGraph(containerId, pathInfo) {
  if (!graphRawData) return;
  renderVisNetwork(containerId, graphRawData, pathInfo);
}
