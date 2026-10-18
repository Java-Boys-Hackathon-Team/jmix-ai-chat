const labels = ['A', 'B', 'C'];
const fields = labels.map((label) => document.querySelector(`#phrase-${label.toLowerCase()}`));
const embedButton = document.querySelector('#embed-button');
const compareButton = document.querySelector('#compare-button');
const vectorSection = document.querySelector('#vectors');
const metricsSection = document.querySelector('#metrics');
const errorMessage = document.querySelector('#error-message');
const runtimeBadge = document.querySelector('#runtime-badge');
let analysis = null;

const format = (value, digits = 4) => Number(value).toFixed(digits).replace('.', ',');
const formatInteger = (value) => new Intl.NumberFormat('ru-RU').format(value);
const escapeHtml = (value) => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

function setStatus(text, state = '') {
  runtimeBadge.className = `runtime-badge ${state}`.trim();
  runtimeBadge.querySelector('span').textContent = text;
}

function activateStage(number) {
  document.querySelectorAll('[data-stage-link]').forEach((link) => {
    link.classList.toggle('active', Number(link.dataset.stageLink) === number);
    link.classList.toggle('locked', Number(link.dataset.stageLink) > number &&
      (Number(link.dataset.stageLink) === 2 ? !analysis : metricsSection.hidden));
  });
}

function resetResults() {
  analysis = null;
  vectorSection.hidden = true;
  metricsSection.hidden = true;
  errorMessage.hidden = true;
  embedButton.querySelector('span').textContent = 'Получить embeddings';
  setStatus('фразы изменены');
  activateStage(1);
}

fields.forEach((field) => field.addEventListener('input', resetResults));

embedButton.addEventListener('click', async () => {
  const texts = fields.map((field) => field.value.trim());
  if (texts.some((text) => !text)) {
    showError('Заполните все три фразы.');
    return;
  }

  embedButton.disabled = true;
  embedButton.querySelector('span').textContent = 'Модель строит векторы…';
  errorMessage.hidden = true;
  metricsSection.hidden = true;
  setStatus('запрос к embedding-модели');

  try {
    const response = await fetch('/public/api/embeddings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ texts }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.detail || body.message || `HTTP ${response.status}`);
    }
    analysis = await response.json();
    renderVectors();
    vectorSection.hidden = false;
    activateStage(2);
    setStatus(`${formatInteger(analysis.dimensions)} координат`, 'ready');
    requestAnimationFrame(() => vectorSection.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  } catch (error) {
    showError(`Не удалось получить embeddings: ${error.message}`);
    setStatus('ошибка запроса', 'error');
  } finally {
    embedButton.disabled = false;
    embedButton.querySelector('span').textContent = analysis ? 'Перестроить embeddings' : 'Получить embeddings';
  }
});

compareButton.addEventListener('click', () => {
  if (!analysis) return;
  renderMetrics();
  metricsSection.hidden = false;
  activateStage(3);
  setStatus('метрики посчитаны локально', 'ready');
  requestAnimationFrame(() => metricsSection.scrollIntoView({ behavior: 'smooth', block: 'start' }));
});

function showError(message) {
  errorMessage.textContent = message;
  errorMessage.hidden = false;
}

function renderVectors() {
  document.querySelector('#model-strip').innerHTML = [
    stat('Модель', analysis.model),
    stat('Размерность', formatInteger(analysis.dimensions)),
    stat('Фраз', analysis.vectors.length),
    stat('Входных токенов', analysis.promptTokens ?? '—'),
  ].join('');

  const sampledVectors = analysis.vectors.map((vector) => sample(vector.values, 256));
  const maxAbs = Math.max(...sampledVectors.flat().map(Math.abs), Number.EPSILON);
  const differences = [
    sampledVectors[0].map((value, index) => Math.abs(value - sampledVectors[1][index])),
    sampledVectors[0].map((value, index) => Math.abs(value - sampledVectors[2][index])),
  ];
  const maxDifference = Math.max(...differences.flat(), Number.EPSILON);

  const heatmapRows = analysis.vectors.map((vector, vectorIndex) =>
    heatmapRow(vector.label, vector.text, sampledVectors[vectorIndex], maxAbs, false)).join('');
  const differenceRows = [
    heatmapRow('Δ A−B', 'Близкие фразы', differences[0], maxDifference, true),
    heatmapRow('Δ A−C', 'Разные темы', differences[1], maxDifference, true),
  ].join('');

  document.querySelector('#vector-list').innerHTML = `
    <section class="aligned-heatmap" aria-label="Сравнительная heatmap embedding-векторов">
      <header class="aligned-heatmap__header">
        <div><strong>Одинаковая колонка — одинаковая координата</strong></div>
        <div class="heatmap-legend"><i class="negative"></i><span>−</span><i class="zero"></i><span>0</span><i class="positive"></i><span>+</span></div>
      </header>
      <div class="aligned-heatmap__rows">${heatmapRows}</div>
      <div class="aligned-heatmap__differences">
        <p>Яркость ниже показывает модуль разницы в каждой координате</p>
        ${differenceRows}
      </div>
    </section>`;
}

function heatmapRow(label, description, values, scale, difference) {
  const cells = values.map((value, index) => {
    const intensity = Math.min(1, Math.abs(value) / scale);
    const color = difference
      ? `rgba(255,128,110,${.04 + intensity * .96})`
      : value >= 0
        ? `rgba(200,255,77,${.06 + intensity * .94})`
        : `rgba(139,168,255,${.06 + intensity * .94})`;
    return `<i style="background:${color};animation-delay:${index}ms" title="${format(value, 6)}"></i>`;
  }).join('');
  return `<div class="aligned-heatmap__row ${difference ? 'difference' : ''}">
    <div class="aligned-heatmap__label"><b>${label}</b><span>${escapeHtml(description)}</span></div>
    <div class="aligned-heatmap__strip" style="--heatmap-cells:${values.length}">${cells}</div>
  </div>`;
}

function stat(label, value) {
  return `<div class="model-stat"><span>${label}</span><strong title="${escapeHtml(value)}">${escapeHtml(value)}</strong></div>`;
}

function sample(values, requested) {
  const count = Math.min(requested, values.length);
  return Array.from({ length: count }, (_, index) => {
    const source = count === 1 ? 0 : Math.round(index * (values.length - 1) / (count - 1));
    return values[source];
  });
}

function dot(a, b) {
  return a.reduce((sum, value, index) => sum + value * b[index], 0);
}

function norm(vector) {
  return Math.sqrt(dot(vector, vector));
}

function cosine(a, b) {
  const denominator = norm(a) * norm(b);
  return denominator === 0 ? 0 : dot(a, b) / denominator;
}

function euclidean(a, b) {
  return Math.sqrt(a.reduce((sum, value, index) => sum + (value - b[index]) ** 2, 0));
}

function manhattan(a, b) {
  return a.reduce((sum, value, index) => sum + Math.abs(value - b[index]), 0);
}

function comparisons() {
  return [[0, 1], [0, 2], [1, 2]].map(([left, right]) => {
    const a = analysis.vectors[left].values;
    const b = analysis.vectors[right].values;
    return {
      left: labels[left], right: labels[right],
      cosine: cosine(a, b), dot: dot(a, b),
      euclidean: euclidean(a, b), manhattan: manhattan(a, b),
    };
  });
}

function renderMetrics() {
  const pairs = comparisons();
  const closest = Math.max(...pairs.map((pair) => pair.cosine));
  document.querySelector('#metric-table').innerHTML = `
    <div class="metric-row header"><span>Пара</span><span>Cosine ↑</span><span>Dot ↑</span><span>Euclidean ↓</span><span>Manhattan ↓</span></div>
    ${pairs.map((pair) => `<div class="metric-row ${Math.abs(pair.cosine - closest) < 1e-10 ? 'closest' : ''}">
      <span class="metric-pair">${pair.left} ↔ ${pair.right}</span>
      <span class="metric-value">${format(pair.cosine)}<small>направление</small></span>
      <span class="metric-value">${format(pair.dot)}<small>угол + длина</small></span>
      <span class="metric-value">${format(pair.euclidean)}<small>L2</small></span>
      <span class="metric-value">${format(pair.manhattan)}<small>L1</small></span>
    </div>`).join('')}`;
  document.querySelector('#semantic-map').innerHTML = semanticMap(pairs);
}

function semanticMap(pairs) {
  const distance = (left, right) => {
    const similarity = pairs.find((pair) =>
      (pair.left === left && pair.right === right) || (pair.left === right && pair.right === left)).cosine;
    return Math.sqrt(Math.max(0, 2 - 2 * similarity));
  };
  const ab = Math.max(distance('A', 'B'), .0001);
  const ac = Math.max(distance('A', 'C'), .0001);
  const bc = Math.max(distance('B', 'C'), .0001);
  const ax = 70;
  const ay = 80;
  const scale = 240 / Math.max(ab, ac, bc);
  const bx = ax + ab * scale;
  const by = ay;
  const rawCx = (ac ** 2 + ab ** 2 - bc ** 2) / (2 * ab);
  const rawCy = Math.sqrt(Math.max(0, ac ** 2 - rawCx ** 2));
  const cx = ax + rawCx * scale;
  const cy = ay + Math.max(rawCy * scale, 70);
  const xs = [ax, bx, cx];
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const shift = 210 - (minX + maxX) / 2;
  const points = { A: [ax + shift, ay], B: [bx + shift, by], C: [cx + shift, cy] };
  const line = (left, right, value) => {
    const [x1, y1] = points[left];
    const [x2, y2] = points[right];
    const normalized = Math.max(0, Math.min(1, value));
    const opacity = .2 + normalized * .8;
    return `<g><line class="map-line" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" opacity="${opacity}" stroke-width="${1 + normalized * 5}"/>
      <text class="map-line-label" x="${(x1 + x2) / 2}" y="${(y1 + y2) / 2 - 8}" text-anchor="middle">cos ${format(value, 3)}</text></g>`;
  };
  const node = (label) => {
    const [x, y] = points[label];
    return `<g class="map-node"><circle cx="${x}" cy="${y}" r="29"/><text x="${x}" y="${y + 7}" text-anchor="middle">${label}</text></g>`;
  };
  return `<svg viewBox="0 0 420 330" role="img" aria-label="Двумерная карта близости фраз">
    ${pairs.map((pair) => line(pair.left, pair.right, pair.cosine)).join('')}
    ${labels.map(node).join('')}
  </svg>`;
}

activateStage(1);
