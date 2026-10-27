// The smaller pages: search results, saved (favourites and recent), a game item's page, and not found.

import { db, href, itemName, iconUrl, modName, entry as getEntry } from '../core/data.js';
import { esc, highlight, plural } from '../core/text.js';
import { go } from '../core/router.js';
import { search, queryWords } from '../core/search.js';
import { favourites, recent } from '../core/storage.js';
import { picture, entryPic, entryCard, row, recipeCard, crumbs } from '../ui/components.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

export function searchPage(view, query) {
  const q = query.q ?? '';
  const cat = query.cat ?? '';
  document.title = `${q ? `“${q}”` : 'Search'} · ${db.site.modName} Codex`;
  const all = q ? search(q, { limit: 400 }) : [];
  const counts = new Map();
  all.forEach(r => counts.set(r.cat, (counts.get(r.cat) ?? 0) + 1));
  const hits = cat ? all.filter(r => r.cat === cat) : all;
  const words = queryWords(q);

  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ['Search']])}
      <h1>${q ? `“${esc(q)}”` : 'Search'}</h1>
      <p>${q ? plural(all.length, 'result') : 'Type in the search bar above, or press <kbd>/</kbd>.'}</p>
      ${all.length ? `<div class="chips">
        <button class="chip ${cat ? '' : 'on'}" data-cat="">All<span class="n">${all.length}</span></button>
        ${[...counts].map(([c, n]) => `<button class="chip ${c === cat ? 'on' : ''}" data-cat="${esc(c)}">${esc(c === 'items' && !db.category.get(c) ? 'Items' : db.category.get(c)?.label ?? c)}<span class="n">${n}</span></button>`).join('')}
      </div>` : ''}
    </section>
    <section class="sec">
      ${hits.length ? `<div class="rows results">${hits.map(h => `<a class="row reveal" href="${h.href}">
          ${h.entry ? entryPic(h.entry) : picture(h.images ?? [], { text: h.title })}
          <span><b>${highlight(h.title, words)}</b><small>${h.entry ? `${esc(h.entry.cat.single ?? h.entry.cat.label)} · ` : ''}${highlight(plainSub(h.sub), words)}</small></span>
          <span class="end">${icon('right')}</span></a>`).join('')}</div>`
        : q ? `<div class="empty">${gear(12)}<h2>No results</h2><p>Nothing matches “${esc(q)}”. Try fewer words, part of a word, or a game id.</p></div>` : ''}
    </section>`;

  view.querySelector('.banner .chips')?.addEventListener('click', e => {
    const b = e.target.closest('[data-cat]');
    if (b) go('search', { q, cat: b.dataset.cat }, true);
  });
  const input = document.getElementById('q');
  if (input && input.value !== q) input.value = q;
}

const plainSub = s => String(s ?? '').replace(/\[\[[^|\]]+\|([^\]]+)\]\]/g, '$1').replace(/\[\[([^\]]+)\]\]/g, (_, id) => getEntry(id)?.name ?? id).replace(/[*`]/g, '');

export function savedPage(view) {
  document.title = `Saved · ${db.site.modName} Codex`;
  const favs = favourites().map(getEntry).filter(Boolean);
  const rec = recent().map(getEntry).filter(Boolean);
  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}${crumbs([['Codex', '#/'], ['Saved']])}<h1>Saved</h1>
      <p>Star any entry to keep it here. Favourites and history stay in this browser only.</p></section>
    <section class="sec"><h2 class="sec-title">Favourites</h2>
      ${favs.length ? `<div class="cards">${favs.map(entryCard).join('')}</div>`
        : `<p class="muted">No favourites yet. Use the ${icon('star').replace('<svg', '<svg style="display:inline;width:14px;height:14px;vertical-align:-2px"')} on any card or entry.</p>`}</section>
    <section class="sec"><h2 class="sec-title">Recently viewed</h2>
      ${rec.length ? `<div class="rows">${rec.map(e => row(e)).join('')}</div>` : '<p class="muted">Nothing yet.</p>'}</section>`;
}

/** An item from vanilla or Create (or any id no entry covers): its icon, name, and recipes. */
export function itemPage(view, id) {
  const hit = db.byGame.get(id);
  if (hit) { location.replace(href(hit.entry)); return; }
  const name = itemName(id);
  document.title = `${name} · ${db.site.modName} Codex`;
  const made = db.made.get(id) ?? [];
  const used = db.used.get(id) ?? [];
  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ['Recipes', '#/recipes'], [name]])}
      <div class="item-head" style="margin-top:14px">${picture([iconUrl(id)], { alt: name, text: name, eager: true })}
        <div><div class="kicker">${esc(modName(id).toUpperCase())} · <code>${esc(id)}</code></div><h1 style="margin:6px 0 0">${esc(name)}</h1>
        <p>Not part of this mod, but it goes into its recipes${made.length ? ', and some of them make it' : ''}.</p></div></div>
    </section>
    ${made.length ? `<section class="sec"><h2 class="sec-title">Made by</h2><div class="recipes">${made.map(recipeCard).join('')}</div></section>` : ''}
    ${used.length ? `<section class="sec"><h2 class="sec-title">Used in ${plural(used.length, 'recipe')}</h2><div class="recipes">${used.map(recipeCard).join('')}</div></section>` : ''}
    ${!made.length && !used.length ? `<div class="empty">${gear(12)}<h2>Not used here</h2><p>No recipe in this mod uses ${esc(name)}.</p></div>` : ''}`;
}

export function notFound(view) {
  document.title = `Not found · ${db.site.modName} Codex`;
  view.innerHTML = `<div class="empty" style="min-height:60vh">${gear(12)}<h2>Page not found</h2>
    <p>That page isn't in the codex. It may have been renamed.</p>
    <div class="btns"><a class="btn primary" href="#/">${icon('home')}Codex home</a><a class="btn" href="#/search">${icon('search')}Search</a></div></div>`;
}
