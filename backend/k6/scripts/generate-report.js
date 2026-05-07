#!/usr/bin/env node
// ============================================================
// scripts/generate-report.js
// Reads one or more k6 JSON summary files and produces a
// single self-contained HTML dashboard.
//
// Usage:
//   node scripts/generate-report.js reports/smoke.json
//   node scripts/generate-report.js reports/*.json
//   node scripts/generate-report.js reports/smoke.json --out reports/custom.html
// ============================================================

const fs   = require('fs');
const path = require('path');

// ── CLI args ─────────────────────────────────────────────────
const args    = process.argv.slice(2);
const outIdx  = args.indexOf('--out');
const outFile = outIdx !== -1 ? args[outIdx + 1] : 'reports/wallet-test-report.html';
const inputs  = args.filter((a, i) => a !== '--out' && i !== outIdx + 1);

if (inputs.length === 0) {
  console.error('Usage: node scripts/generate-report.js <summary.json> [more.json] [--out report.html]');
  process.exit(1);
}

// ── Load JSON summaries ───────────────────────────────────────
const suites = inputs.map((f) => {
  try {
    const raw  = fs.readFileSync(f, 'utf8');
    const data = JSON.parse(raw);
    return { file: path.basename(f, '.json'), data };
  } catch (e) {
    console.warn(`Warning: could not read ${f}: ${e.message}`);
    return null;
  }
}).filter(Boolean);

if (suites.length === 0) {
  console.error('No valid JSON summary files found.');
  process.exit(1);
}

// ── Helper: format metric value ──────────────────────────────
function fmt(v) {
  if (v === undefined || v === null) return '–';
  if (typeof v === 'number') return v % 1 === 0 ? v.toFixed(0) : v.toFixed(3);
  return String(v);
}

function ms(v) {
  if (v === undefined || v === null) return '–';
  return `${(+v).toFixed(1)} ms`;
}

function pct(v) {
  if (v === undefined || v === null) return '–';
  return `${(+v * 100).toFixed(2)}%`;
}

// ── Build per-suite summary card ─────────────────────────────
function buildSuiteCard(suite) {
  const { file, data } = suite;
  const m = data.metrics || {};

  const checks       = m.checks        || {};
  const httpFailed   = m.http_req_failed || {};
  const httpDur      = m.http_req_duration || {};
  const httpReqs     = m.http_reqs     || {};
  const vusMax       = m.vus_max       || {};

  const totalChecks  = (checks.passes || 0) + (checks.fails || 0);
  const passRate     = totalChecks > 0 ? ((checks.passes || 0) / totalChecks * 100).toFixed(1) : 'N/A';
  const allPassed    = (checks.fails || 0) === 0;
  const statusBadge  = allPassed
    ? '<span class="badge pass">PASSED</span>'
    : '<span class="badge fail">FAILED</span>';

  // Threshold results
  const thresholdRows = Object.entries(data.root_group?.checks || {}).map(([k, v]) => {
    const ok = v.passes > 0 && v.fails === 0;
    return `<tr>
      <td>${k}</td>
      <td class="${ok ? 'pass-text' : 'fail-text'}">${ok ? '✓ Pass' : '✗ Fail'}</td>
      <td>${v.passes}</td>
      <td>${v.fails}</td>
    </tr>`;
  }).join('');

  // Build check rows from root_group recursively
  const checkRows = [];
  function extractChecks(group, prefix = '') {
    if (!group) return;
    // k6 summary: checks can be an object {name: {passes,fails}} or array
    const checksRaw = group.checks || {};
    const checksArr = Array.isArray(checksRaw)
      ? checksRaw
      : Object.entries(checksRaw).map(([name, v]) => ({ name, ...v }));
    checksArr.forEach((c) => {
      const ok = (c.fails || 0) === 0;
      checkRows.push(`<tr>
        <td>${prefix}${c.name}</td>
        <td class="${ok ? 'pass-text' : 'fail-text'}">${ok ? '✓' : '✗'}</td>
        <td>${c.passes || 0}</td>
        <td>${c.fails || 0}</td>
      </tr>`);
    });
    // group.groups is an object {name: groupObj} in k6 JSON summaries, not an array
    const groupsRaw = group.groups || {};
    const groupsArr = Array.isArray(groupsRaw)
      ? groupsRaw
      : Object.values(groupsRaw);
    groupsArr.forEach((g) => extractChecks(g, `${prefix}${g.name} › `));
  }
  extractChecks(data.root_group);

  return `
  <div class="suite-card ${allPassed ? 'suite-pass' : 'suite-fail'}">
    <div class="suite-header">
      <h2>${file} ${statusBadge}</h2>
      <span class="check-summary">${checks.passes || 0} / ${totalChecks} checks passed (${passRate}%)</span>
    </div>

    <div class="metrics-grid">
      <div class="metric-box">
        <div class="metric-label">Total Requests</div>
        <div class="metric-value">${fmt(httpReqs.count)}</div>
      </div>
      <div class="metric-box">
        <div class="metric-label">Failed Requests</div>
        <div class="metric-value ${(httpFailed.rate || 0) > 0.05 ? 'fail-text' : ''}">${pct(httpFailed.rate)}</div>
      </div>
      <div class="metric-box">
        <div class="metric-label">Avg Response</div>
        <div class="metric-value">${ms(httpDur.avg)}</div>
      </div>
      <div class="metric-box">
        <div class="metric-label">p95 Latency</div>
        <div class="metric-value ${(httpDur['p(95)'] || 0) > 2000 ? 'fail-text' : ''}">${ms(httpDur['p(95)'])}</div>
      </div>
      <div class="metric-box">
        <div class="metric-label">p99 Latency</div>
        <div class="metric-value">${ms(httpDur['p(99)'])}</div>
      </div>
      <div class="metric-box">
        <div class="metric-label">Max VUs</div>
        <div class="metric-value">${fmt(vusMax.max)}</div>
      </div>
    </div>

    <details open>
      <summary>Check Results (${checkRows.length} checks)</summary>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Check</th><th>Result</th><th>Passes</th><th>Fails</th></tr></thead>
          <tbody>${checkRows.length ? checkRows.join('') : '<tr><td colspan="4">No checks recorded</td></tr>'}</tbody>
        </table>
      </div>
    </details>
  </div>`;
}

// ── Aggregate totals ──────────────────────────────────────────
let totalChecks = 0, totalPasses = 0, totalFails = 0, totalRequests = 0;
suites.forEach(({ data }) => {
  const m = data.metrics || {};
  totalChecks   += (m.checks?.passes || 0) + (m.checks?.fails || 0);
  totalPasses   += m.checks?.passes || 0;
  totalFails    += m.checks?.fails  || 0;
  totalRequests += m.http_reqs?.count || 0;
});
const overallOk = totalFails === 0;

// ── HTML template ─────────────────────────────────────────────
const now = new Date().toLocaleString();
const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>k6 Wallet Test Report</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #0f1117; color: #e2e8f0; }
    a { color: #63b3ed; }

    header { background: linear-gradient(135deg, #1a365d 0%, #2d3748 100%); padding: 24px 32px; border-bottom: 2px solid #4a5568; }
    header h1 { font-size: 1.8rem; color: #90cdf4; }
    header .subtitle { color: #a0aec0; margin-top: 4px; font-size: 0.9rem; }
    .overall-badge { display: inline-block; margin-top: 10px; padding: 4px 16px; border-radius: 20px; font-weight: 700; font-size: 1rem; }
    .overall-pass { background: #276749; color: #9ae6b4; }
    .overall-fail { background: #742a2a; color: #feb2b2; }

    .container { max-width: 1200px; margin: 0 auto; padding: 24px 16px; }

    .summary-bar { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 28px; }
    .summary-item { flex: 1; min-width: 140px; background: #1a202c; border-radius: 10px; padding: 16px; border: 1px solid #2d3748; text-align: center; }
    .summary-item .s-label { color: #a0aec0; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1px; }
    .summary-item .s-value { font-size: 2rem; font-weight: 700; margin-top: 6px; }
    .s-pass { color: #68d391; }
    .s-fail { color: #fc8181; }
    .s-neutral { color: #63b3ed; }

    .suite-card { background: #1a202c; border-radius: 12px; margin-bottom: 24px; overflow: hidden; border: 1px solid #2d3748; }
    .suite-pass { border-left: 5px solid #48bb78; }
    .suite-fail { border-left: 5px solid #fc8181; }
    .suite-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; padding: 18px 20px; background: #171923; }
    .suite-header h2 { font-size: 1.1rem; color: #e2e8f0; display: flex; align-items: center; gap: 10px; }
    .check-summary { color: #a0aec0; font-size: 0.85rem; }

    .badge { padding: 2px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; }
    .pass { background: #276749; color: #9ae6b4; }
    .fail { background: #742a2a; color: #feb2b2; }

    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; padding: 16px 20px; }
    .metric-box { background: #171923; border-radius: 8px; padding: 12px 14px; border: 1px solid #2d3748; }
    .metric-label { color: #718096; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.5px; }
    .metric-value { font-size: 1.25rem; font-weight: 600; margin-top: 4px; color: #90cdf4; }

    details { border-top: 1px solid #2d3748; }
    summary { padding: 12px 20px; cursor: pointer; user-select: none; color: #90cdf4; font-weight: 600; font-size: 0.9rem; }
    summary:hover { background: #171923; }
    .table-wrap { overflow-x: auto; padding: 0 20px 16px; }
    table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
    th { background: #171923; color: #a0aec0; text-align: left; padding: 8px 12px; border-bottom: 2px solid #2d3748; }
    td { padding: 7px 12px; border-bottom: 1px solid #2d3748; }
    tr:hover td { background: #171923; }
    .pass-text { color: #68d391; font-weight: 600; }
    .fail-text { color: #fc8181; font-weight: 600; }

    footer { text-align: center; padding: 24px; color: #4a5568; font-size: 0.8rem; border-top: 1px solid #2d3748; margin-top: 16px; }
  </style>
</head>
<body>
  <header>
    <h1>🏦 Multi-Currency Wallet — k6 Test Report</h1>
    <div class="subtitle">Generated: ${now} · ${suites.length} test suite(s) · ${totalRequests} total HTTP requests</div>
    <div class="overall-badge ${overallOk ? 'overall-pass' : 'overall-fail'}">
      ${overallOk ? '✓ ALL CHECKS PASSED' : '✗ SOME CHECKS FAILED'}
    </div>
  </header>

  <div class="container">
    <div class="summary-bar">
      <div class="summary-item">
        <div class="s-label">Total Checks</div>
        <div class="s-value s-neutral">${totalChecks}</div>
      </div>
      <div class="summary-item">
        <div class="s-label">Passed</div>
        <div class="s-value s-pass">${totalPasses}</div>
      </div>
      <div class="summary-item">
        <div class="s-label">Failed</div>
        <div class="s-value ${totalFails > 0 ? 's-fail' : 's-pass'}">${totalFails}</div>
      </div>
      <div class="summary-item">
        <div class="s-label">Pass Rate</div>
        <div class="s-value ${totalFails === 0 ? 's-pass' : 's-fail'}">${totalChecks > 0 ? (totalPasses / totalChecks * 100).toFixed(1) : 0}%</div>
      </div>
      <div class="summary-item">
        <div class="s-label">HTTP Requests</div>
        <div class="s-value s-neutral">${totalRequests}</div>
      </div>
    </div>

    ${suites.map(buildSuiteCard).join('\n')}
  </div>

  <footer>
    Mini Wallet Banking System · k6 Performance &amp; Functional Tests
  </footer>
</body>
</html>`;

// ── Write output ──────────────────────────────────────────────
fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, html, 'utf8');
console.log(`✅  Report written to: ${outFile}`);