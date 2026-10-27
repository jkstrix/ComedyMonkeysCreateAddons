// The progression chart: four stages left to right, each with its goals (tick them off; they're remembered) and its
// key entries. Lines join each entry to what it needs built first; hover one to light its path.

import { db, href, entriesOf } from '../core/data.js';
import { esc, md } from '../core/text.js';
import { isGoalDone, setGoal } from '../core/storage.js';
import { entryPic, crumbs } from '../ui/components.js';
import { gear } from '../ui/gear.js';

export function progressionPage(view) {
  document.title = `Progression · ${db.site.modName} Codex`;
  const stages = db.site.progression ?? [];
  const nextAt = stages.findIndex(s => (s.goals ?? []).some((_, i) => !isGoalDone(`${s.id}:${i}`)));

  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ['Progression']])}
      <h1>What to build next</h1>
      <p>${md(db.site.progressionIntro ?? 'Four stages, from your first andesite machines to the end game. Tick off goals as you go: the stage you are in is marked.')}</p>
    </section>
    <section class="sec"><div class="prog" id="prog">
      <svg class="prog-links" aria-hidden="true"></svg>
      ${stages.map((s, i) => column(s, i, i === nextAt)).join('')}
    </div></section>`;

  view.querySelector('#prog').addEventListener('change', e => {
    const box = e.target.closest('input[data-goal]');
    if (!box) return;
    setGoal(box.dataset.goal, box.checked);
    box.closest('.goal').classList.toggle('done', box.checked);
  });
  drawLinks(view);
  const redraw = () => drawLinks(view);
  window.addEventListener('resize', redraw, { passive: true });
  // pictures load late and move things: draw again then, and stop listening once the page changes
  view.querySelectorAll('img').forEach(img => img.addEventListener('load', redraw, { once: true }));
  window.addEventListener('hashchange', () => window.removeEventListener('resize', redraw), { once: true });
}

function column(s, i, isNext) {
  const listed = entriesOf(s.entries);
  const list = listed.length ? listed : db.all.filter(e => e.stage === s.id && ['machines', 'blocks'].includes(e.cat.id));
  return `<div class="prog-col">
    <div class="head ${isNext ? 'next' : ''}">
      ${isNext ? '<span class="badge new" style="float:right">you are here</span>' : ''}
      <div class="num">${String(i + 1).padStart(2, '0')}</div>
      <h2>${esc(s.title)}</h2><div class="era">${esc(s.era ?? '')}</div>
      ${s.summary ? `<p>${md(s.summary)}</p>` : ''}
      ${s.needs ? `<p><strong>You'll need:</strong> ${md(s.needs)}</p>` : ''}
      ${(s.goals ?? []).length ? `<div class="goals">${s.goals.map((g, gi) => {
        const key = `${s.id}:${gi}`;
        return `<label class="goal ${isGoalDone(key) ? 'done' : ''}"><input type="checkbox" data-goal="${key}" ${isGoalDone(key) ? 'checked' : ''}><span>${md(g)}</span></label>`;
      }).join('')}</div>` : ''}
    </div>
    ${list.map(e => `<a class="node reveal" href="${href(e)}" data-node="${esc(e.id)}">${entryPic(e)}<span><b>${esc(e.name)}</b><small>${esc(e.cat.single ?? e.cat.label)}</small></span></a>`).join('')}
  </div>`;
}

/** Lines from each entry to the ones it needs first, when both are on the chart. */
function drawLinks(view) {
  const wrap = view.querySelector('#prog');
  const svg = wrap?.querySelector('.prog-links');
  if (!svg || getComputedStyle(svg).display === 'none') return;
  const box = wrap.getBoundingClientRect();
  svg.setAttribute('viewBox', `0 0 ${box.width} ${box.height}`);
  const nodes = new Map([...wrap.querySelectorAll('[data-node]')].map(n => [n.dataset.node, n]));
  let paths = '';
  for (const [id, el] of nodes) {
    for (const req of db.entries.get(id)?.requires ?? []) {
      const from = nodes.get(req);
      if (!from) continue;
      const a = from.getBoundingClientRect(), b = el.getBoundingClientRect();
      if (Math.abs(a.left - b.left) < 10) continue; // same column: the list order says it
      const x1 = a.right - box.left, y1 = a.top + a.height / 2 - box.top;
      const x2 = b.left - box.left, y2 = b.top + b.height / 2 - box.top;
      const mx = (x1 + x2) / 2;
      paths += `<path data-from="${esc(req)}" data-to="${esc(id)}" d="M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2},${y2}"/>`;
    }
  }
  svg.innerHTML = paths;
  if (!wrap.dataset.wired) {
    wrap.dataset.wired = '1';
    wrap.addEventListener('pointerover', e => light(wrap, e.target.closest('[data-node]')?.dataset.node));
    wrap.addEventListener('pointerleave', () => light(wrap, null));
  }
}

/** Lights an entry, everything it needs (all the way back) and what it leads to. */
function light(wrap, id) {
  const lit = new Set();
  if (id) {
    const back = [id];
    while (back.length) {
      const x = back.pop();
      if (lit.has(x)) continue;
      lit.add(x);
      back.push(...(db.entries.get(x)?.requires ?? []));
    }
  }
  wrap.querySelectorAll('[data-node]').forEach(n => n.classList.toggle('lit', lit.has(n.dataset.node)));
  wrap.querySelectorAll('.prog-links path').forEach(p => p.classList.toggle('lit', lit.has(p.dataset.to) && lit.has(p.dataset.from)));
}

