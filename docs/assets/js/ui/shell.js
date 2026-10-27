// The frame: icon rail (desktop), top bar with instant search, tab bar and "More" sheet (phones).

import { db, href } from '../core/data.js';
import { esc, highlight } from '../core/text.js';
import { go, link } from '../core/router.js';
import { search, queryWords } from '../core/search.js';
import { recent } from '../core/storage.js';
import { picture, entryPic } from './components.js';
import { icon } from './icons.js';
import { gear } from './gear.js';

/** The nav, in site.json order: Home first, then categories and pages. */
const navItems = () => [
  { id: '', label: 'Home', icon: 'home' },
  ...db.categories.filter(c => !c.hidden).map(c => ({ id: c.id, label: c.short ?? c.label, icon: c.icon, soon: c.status === 'soon' })),
];

let scope = '';

export function renderShell(root) {
  const items = navItems();
  root.innerHTML = `
    <a class="sr-only" href="#view">Skip to content</a>
    <div class="app">
      <nav class="rail" aria-label="Sections">
        <a class="rail-logo" href="#/" title="${esc(db.site.modName)}">${gear(10)}</a>
        ${items.map(n => `<a class="nav" href="#/${n.id}" data-nav="${n.id}">${icon(n.icon)}${esc(n.label)}${n.soon ? '<i class="soon-dot" title="Coming soon"></i>' : ''}</a>`).join('')}
        <div class="spacer"></div>
        <a class="nav" href="#/favourites" data-nav="favourites">${icon('bookmark')}Saved</a>
      </nav>
      <main>
        <header class="topbar">
          <a class="mark" href="#/" aria-label="Home">${gear(10)}</a>
          <div class="search" role="search">
            <label class="search-box">${icon('search')}<span class="sr-only">Search the codex</span>
              <input id="q" type="search" autocomplete="off" spellcheck="false" placeholder="Search blocks, machines, items, recipes…"
                aria-controls="suggest" aria-expanded="false"><kbd>/</kbd></label>
            <div class="suggest" id="suggest" role="listbox" hidden></div>
          </div>
          <div class="chips" id="scope">
            <button class="chip on" data-scope="">All</button>
            ${db.categories.filter(c => c.file && c.entries.length).slice(0, 4).map(c => `<button class="chip" data-scope="${c.id}">${esc(c.label)}</button>`).join('')}
          </div>
          <span class="ver">v${esc(db.site.version)} · MC ${esc(db.site.minecraft)}</span>
        </header>
        <div id="view" tabindex="-1"></div>
        <footer class="foot">
          <span>${esc(db.site.modName)} ${esc(db.site.version)}</span>
          <span>Create ${esc(db.site.create)} · NeoForge ${esc(db.site.neoforge)} · Minecraft ${esc(db.site.minecraft)}</span>
          ${(db.site.links ?? []).map(l => `<a href="${esc(l.href)}" rel="noopener">${esc(l.label)}</a>`).join('')}
          <span>Pictures rendered by the game itself.</span>
        </footer>
      </main>
    </div>
    <nav class="tabbar" aria-label="Sections">
      <a href="#/" data-nav="">${icon('home')}Home</a>
      <a href="#/machines" data-nav="machines">${icon('cog')}Machines</a>
      <button id="tab-search">${icon('search')}Search</button>
      <a href="#/progression" data-nav="progression">${icon('steps')}Progress</a>
      <button id="tab-more">${icon('more')}More</button>
    </nav>
    <div class="sheet" id="sheet" hidden><div class="sheet-body"><div class="grip"></div><div class="sheet-grid">
      ${items.map(n => `<a href="#/${n.id}">${icon(n.icon)}${esc(n.label)}</a>`).join('')}
      <a href="#/favourites">${icon('bookmark')}Saved</a>
    </div></div></div>`;

  wireSearch();
  const sheet = document.getElementById('sheet');
  document.getElementById('tab-more').onclick = () => { sheet.hidden = false; };
  sheet.onclick = e => { if (e.target === sheet || e.target.closest('a')) sheet.hidden = true; };
  document.getElementById('tab-search').onclick = () => document.getElementById('q').focus();
}

/** Lights up the nav item for the current page. */
export function markNav(segments) {
  const first = segments[0] ?? '';
  const active = first === 'item' ? 'items' : first;
  document.querySelectorAll('[data-nav]').forEach(a => a.classList.toggle('on', a.dataset.nav === active));
  document.getElementById('sheet').hidden = true;
}

// ---------------------------------------------------------------- instant search

function wireSearch() {
  const input = document.getElementById('q');
  const box = document.getElementById('suggest');
  let hits = [];
  let at = -1;

  const close = () => { box.hidden = true; input.setAttribute('aria-expanded', 'false'); at = -1; };
  const open = () => { box.hidden = false; input.setAttribute('aria-expanded', 'true'); };

  function draw() {
    const q = input.value.trim();
    if (!q) {
      const rec = recent().map(id => db.entries.get(id)).filter(Boolean).slice(0, 6);
      if (!rec.length) { close(); return; }
      hits = rec.map(e => ({ href: href(e), entry: e, title: e.name, sub: e.summary }));
      box.innerHTML = `<div class="group label">Recently viewed</div>` + hits.map((h, i) => hitHtml(h, i, [])).join('');
      open();
      return;
    }
    hits = search(q, { cat: scope || undefined, limit: 9 });
    const words = queryWords(q);
    let html = '';
    let last = null;
    hits.forEach((h, i) => {
      const group = h.kind === 'item' ? 'Game items' : db.category.get(h.cat)?.label;
      if (group !== last) { html += `<div class="group label">${esc(group)}</div>`; last = group; }
      html += hitHtml(h, i, words);
    });
    html += hits.length
      ? `<a class="all" href="${link('search', { q, cat: scope })}">All results for “${esc(q)}” ↵</a>`
      : `<div class="none">Nothing matches “${esc(q)}”. Try fewer words, or a game id like <code>pressure_crucible</code>.</div>`;
    box.innerHTML = html;
    at = -1;
    open();
  }

  function hitHtml(h, i, words) {
    const pic = h.entry ? entryPic(h.entry) : picture(h.images ?? [], { text: h.title });
    return `<a class="hit" role="option" id="hit-${i}" href="${h.href}">${pic}<span><b>${highlight(h.title, words)}</b><small>${esc(stripMarks(h.sub))}</small></span></a>`;
  }

  function move(d) {
    const links = [...box.querySelectorAll('a.hit')];
    if (!links.length) return;
    at = (at + d + links.length) % links.length;
    links.forEach((l, i) => l.classList.toggle('on', i === at));
    // scroll the suggestion box only, never the page behind it
    const l = links[at];
    if (l.offsetTop < box.scrollTop) box.scrollTop = l.offsetTop;
    else if (l.offsetTop + l.offsetHeight > box.scrollTop + box.clientHeight) box.scrollTop = l.offsetTop + l.offsetHeight - box.clientHeight;
    input.setAttribute('aria-activedescendant', links[at].id);
  }

  let timer;
  input.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(draw, 60); });
  input.addEventListener('focus', draw);
  input.addEventListener('keydown', e => {
    if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
    else if (e.key === 'Escape') { close(); input.blur(); }
    else if (e.key === 'Enter') {
      e.preventDefault();
      const chosen = box.querySelectorAll('a.hit')[at];
      if (chosen) location.hash = chosen.getAttribute('href');
      else if (input.value.trim()) go('search', { q: input.value.trim(), cat: scope });
      close();
      input.blur();
    }
  });
  box.addEventListener('click', e => { if (e.target.closest('a')) { close(); input.blur(); } });
  document.addEventListener('pointerdown', e => { if (!e.target.closest('.search')) close(); });
  document.addEventListener('keydown', e => {
    if (e.key === '/' && !/input|textarea|select/i.test(document.activeElement.tagName)) { e.preventDefault(); input.focus(); input.select(); }
  });
  document.getElementById('scope').addEventListener('click', e => {
    const b = e.target.closest('[data-scope]');
    if (!b) return;
    scope = b.dataset.scope;
    document.querySelectorAll('#scope .chip').forEach(c => c.classList.toggle('on', c === b));
    if (input.value.trim()) { input.focus(); draw(); }
  });
}

const stripMarks = s => String(s ?? '').replace(/\[\[[^|\]]+\|([^\]]+)\]\]/g, '$1').replace(/\[\[([^\]]+)\]\]/g, (_, id) => db.entries.get(id)?.name ?? id).replace(/[*`]/g, '');
