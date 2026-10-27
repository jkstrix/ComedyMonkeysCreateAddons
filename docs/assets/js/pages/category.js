// A category's page: banner, filters (subcategory, stage), sorting, and the entries as cards (mobs as dossiers,
// guide chapters in order), drawn in batches as you scroll so hundreds of entries stay quick.

import { db, catHref, subcat, stageIndex, stageName, STAGES } from '../core/data.js';
import { esc, md, plural } from '../core/text.js';
import { go } from '../core/router.js';
import { entryCard, dossier, row } from '../ui/components.js';
import { animate } from '../ui/motion.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

const BATCH = 48;

export function categoryPage(view, cat, query) {
  document.title = `${cat.label} · ${db.site.modName} Codex`;
  if (cat.status === 'soon' && !cat.entries.length) return soonPage(view, cat);

  const tags = count(cat.entries.map(subcat).filter(Boolean));
  const stages = count(cat.entries.map(e => e.stage).filter(Boolean));
  const tag = query.tag ?? '';
  const stage = query.stage ?? '';
  // headings per kind only pay off once there are enough entries to fill them
  const sort = query.sort ?? (cat.id === 'guide' ? 'chapter' : cat.entries.length >= 10 ? 'group' : 'stage');

  let list = cat.entries.filter(e => (!tag || e.tags?.includes(tag)) && (!stage || e.stage === stage));
  const byName = (a, b) => a.name.localeCompare(b.name);
  if (sort === 'name') list = [...list].sort(byName);
  else if (sort === 'stage') list = [...list].sort((a, b) => stageIndex(a) - stageIndex(b) || byName(a, b));
  else if (sort === 'group') list = [...list].sort((a, b) => tagOrder(tags, subcat(a)) - tagOrder(tags, subcat(b)) || stageIndex(a) - stageIndex(b) || byName(a, b));

  view.innerHTML = `
    <section class="banner">
      ${gear(24, 'gear-bg')}
      <div class="kicker">// CODEX · ${esc(cat.label.toUpperCase())}</div>
      <h1>${esc(cat.title ?? cat.label)}</h1>
      ${cat.description ? `<p>${md(cat.description)}</p>` : ''}
    </section>
    <div class="toolbar">
      <div class="chips" role="group" aria-label="Filter by kind">
        <button class="chip ${tag ? '' : 'on'}" data-tag="">All<span class="n">${cat.entries.length}</span></button>
        ${tags.map(([t, n]) => `<button class="chip ${t === tag ? 'on' : ''}" data-tag="${esc(t)}">${esc(t)}<span class="n">${n}</span></button>`).join('')}
      </div>
      ${stages.length > 1 ? `<select aria-label="Stage" data-k="stage"><option value="">Any stage</option>${STAGES.filter(s => stages.some(([x]) => x === s)).map(s => `<option value="${s}" ${s === stage ? 'selected' : ''}>${esc(stageName(s))}</option>`).join('')}</select>` : ''}
      <select aria-label="Sort" data-k="sort">
        ${[cat.id === 'guide' ? ['chapter', 'In order'] : ['group', 'By kind'], ['name', 'A to Z'], ['stage', 'By stage']].map(([v, l]) => `<option value="${v}" ${v === sort ? 'selected' : ''}>${l}</option>`).join('')}
      </select>
      <span class="count">${plural(list.length, 'entry', 'entries')}</span>
    </div>
    <section class="sec"><div id="grid"></div><div class="sentinel"></div></section>`;

  view.querySelector('.toolbar').addEventListener('click', e => {
    const b = e.target.closest('[data-tag]');
    if (b) go(cat.id, { ...query, tag: b.dataset.tag }, true);
  });
  view.querySelectorAll('.toolbar select').forEach(s => s.addEventListener('change', () => go(cat.id, { ...query, [s.dataset.k]: s.value }, true)));

  if (!list.length) {
    view.querySelector('#grid').innerHTML = `<div class="empty">${gear(12)}<h2>Nothing here</h2><p>No ${esc(cat.label.toLowerCase())} match those filters.</p><a class="btn" href="${catHref(cat)}">Show all</a></div>`;
    return;
  }
  renderBatches(view, cat, list, sort === 'group' && !tag);
}

function count(values) {
  const m = new Map();
  values.forEach(v => m.set(v, (m.get(v) ?? 0) + 1));
  return [...m];
}

const tagOrder = (tags, t) => { const i = tags.findIndex(([x]) => x === t); return i < 0 ? 999 : i; };

/** Cards in batches: the next batch is drawn when the end of the list scrolls near. */
function renderBatches(view, cat, list, grouped) {
  const grid = view.querySelector('#grid');
  const sentinel = view.querySelector('.sentinel');
  const card = cat.id === 'mobs' ? dossier : cat.id === 'guide' ? chapterRow : entryCard;
  const wrapCls = cat.id === 'mobs' ? 'dossiers' : cat.id === 'guide' ? 'rows' : 'cards';
  let at = 0, lastGroup = null, box = null;

  function next() {
    const end = Math.min(list.length, at + BATCH);
    for (; at < end; at++) {
      const e = list[at];
      const g = grouped ? subcat(e) : '';
      if (!box || g !== lastGroup) {
        if (grouped && g) grid.insertAdjacentHTML('beforeend', `<div class="group-head"><h2>${esc(g)}</h2><span>${list.filter(x => subcat(x) === g).length}</span></div>`);
        grid.insertAdjacentHTML('beforeend', `<div class="${wrapCls}"></div>`);
        box = grid.lastElementChild;
        lastGroup = g;
      }
      box.insertAdjacentHTML('beforeend', card(e));
    }
    animate(grid);
    if (at >= list.length) io?.disconnect();
  }

  const io = 'IntersectionObserver' in window
    ? new IntersectionObserver(items => items.some(i => i.isIntersecting) && next(), { rootMargin: '800px 0px' }) : null;
  next();
  if (io && at < list.length) io.observe(sentinel);
  else while (at < list.length) next();
}

const chapterRow = e => row(e, { title: `<span class="mono" style="color:var(--copper);margin-right:8px">${String(e.chapter ?? '').padStart(2, '0')}</span>${esc(e.name)}`, sub: md(e.summary, false) });

/** A category that's announced but has nothing in it yet (biomes). */
function soonPage(view, cat) {
  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}<div class="kicker">// CODEX · ${esc(cat.label.toUpperCase())}</div><h1>${esc(cat.title ?? cat.label)}</h1></section>
    <div class="empty">${icon(cat.icon)}
      <h2>Coming soon</h2>
      <p style="max-width:34em">${md(cat.teaser ?? 'This part of the codex is still being written.')}</p>
      <div class="btns"><a class="btn" href="#/progression">${icon('steps')}See the progression</a><a class="btn" href="#/">${icon('home')}Back to the codex</a></div>
    </div>`;
}
