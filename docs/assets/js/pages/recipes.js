// The recipe book: every recipe, filtered by machine and by words, or focused on one item (what makes it, what uses it).

import { db, itemName, itemHref, ingredients } from '../core/data.js';
import { esc, fold, plural } from '../core/text.js';
import { go } from '../core/router.js';
import { recipeCard, itemPic, crumbs } from '../ui/components.js';
import { animate } from '../ui/motion.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

const BATCH = 30;

export function recipesPage(view, query, samePage) {
  document.title = `Recipes · ${db.site.modName} Codex`;
  const types = db.site.recipeTypes ?? {};
  const type = query.type ?? '';
  const item = query.item ?? '';
  const mode = query.mode === 'used' ? 'used' : 'made';
  const q = query.q ?? '';

  // grouped by machine in the order site.json lists them, then by what they make
  const order = Object.keys(types);
  const rank = r => { const i = order.indexOf(r.type); return i < 0 ? order.length : i; };
  let list = [...(item ? (mode === 'used' ? db.used : db.made).get(item) ?? [] : db.recipes)]
    .sort((a, b) => rank(a) - rank(b) || itemName(a.outputs[0]?.item).localeCompare(itemName(b.outputs[0]?.item)));
  const typeCounts = new Map();
  list.forEach(r => typeCounts.set(r.type, (typeCounts.get(r.type) ?? 0) + 1));
  if (type) list = list.filter(r => r.type === type);
  if (q) {
    const words = fold(q).split(/\s+/).filter(Boolean);
    list = list.filter(r => {
      const text = fold([...r.outputs.map(o => itemName(o.item)), ...ingredients(r).map(itemName), types[r.type]?.label ?? r.type].join(' '));
      return words.every(w => text.includes(w));
    });
  }

  const typeOrder = Object.keys(types).filter(t => typeCounts.has(t)).concat([...typeCounts.keys()].filter(t => !types[t]));
  const total = [...typeCounts.values()].reduce((a, b) => a + b, 0);

  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ['Recipes']])}
      <h1>Recipe book</h1>
      <p>Every recipe the mod adds, straight from its data files: crafting, Create's machines and the mod's own. Click any ingredient to see what it's for.</p>
    </section>
    <div class="rb-layout">
      <nav class="rb-side" aria-label="Recipe types">
        <button class="${type ? '' : 'on'}" data-type="">All recipes<span>${total}</span></button>
        ${typeOrder.map(t => `<button class="${t === type ? 'on' : ''}" data-type="${esc(t)}">${esc(types[t]?.label ?? t)}<span>${typeCounts.get(t)}</span></button>`).join('')}
      </nav>
      <div>
        <div class="rb-filter">
          <label class="search-box">${icon('search')}<input id="rq" type="search" value="${esc(q)}" placeholder="Filter by item or machine…" aria-label="Filter recipes"></label>
          ${item ? `<div class="rb-focus">${itemPic(item)}<span>${mode === 'used' ? 'Recipes that use' : 'Recipes that make'} <a class="link" href="${itemHref(item)}">${esc(itemName(item))}</a></span>
            <button class="chip" data-mode="${mode === 'used' ? 'made' : 'used'}">${mode === 'used' ? 'how to make it' : 'what it’s used in'}</button>
            <button class="chip" data-clear title="Show every recipe">${icon('close')}</button></div>` : ''}
          <span class="count mono muted" style="font-size:12px">${plural(list.length, 'recipe')}</span>
        </div>
        <div class="recipes" id="rgrid"></div><div class="sentinel"></div>
        ${list.length ? '' : `<div class="empty">${gear(12)}<h2>No recipes</h2><p>Nothing matches. Try another word, or all types.</p></div>`}
      </div>
    </div>`;

  const set = patch => go('recipes', { type, item, mode: item ? mode : '', q, ...patch }, true);
  view.querySelector('.rb-side').addEventListener('click', e => { const b = e.target.closest('[data-type]'); if (b) set({ type: b.dataset.type }); });
  view.querySelector('[data-mode]')?.addEventListener('click', e => set({ mode: e.currentTarget.dataset.mode, type: '' }));
  view.querySelector('[data-clear]')?.addEventListener('click', () => set({ item: '', mode: '' }));
  const input = view.querySelector('#rq');
  let t;
  input.addEventListener('input', () => { clearTimeout(t); t = setTimeout(() => set({ q: input.value.trim() }), 200); });
  if (samePage && q) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); }

  // batches, as on the category pages
  const grid = view.querySelector('#rgrid');
  let at = 0;
  const next = () => {
    grid.insertAdjacentHTML('beforeend', list.slice(at, at + BATCH).map(recipeCard).join(''));
    at += BATCH;
    animate(grid);
    if (at >= list.length) io?.disconnect();
  };
  const io = 'IntersectionObserver' in window ? new IntersectionObserver(i => i.some(x => x.isIntersecting) && next(), { rootMargin: '900px 0px' }) : null;
  next();
  if (io && at < list.length) io.observe(view.querySelector('.sentinel'));
  else while (at < list.length) next();
}
