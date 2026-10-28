// An entry's page: the three-pane codex. Left, the other entries of its category; middle, the render and the teaching;
// right, the readout (dials, specs, power, what to build first). Which sections appear depends on what the entry has.

import { db, href, catHref, entryImages, entry as getEntry, entriesOf, isNew, subcat, stageIndex, stageName } from '../core/data.js';
import { esc, md } from '../core/text.js';
import { visit, isFavourite } from '../core/storage.js';
import { picture, entryPic, starButton, crumbs, pips, kv, badge, callout, entryCard, listItem, teach, stats, power, flow, recipeCard, body, bullets } from '../ui/components.js';
import { icon } from '../ui/icons.js';

/** Category-specific list fields: [field, title, icon] in reading order. */
const FIELDS = {
  machines: [['inputs', 'Inputs', 'arrow'], ['outputs', 'Outputs', 'layers'], ['throughput', 'Throughput', 'clock'], ['rules', 'Operating rules', 'book'], ['efficiency', 'Efficiency', 'bolt']],
  blocks: [['uses', 'Uses', 'check'], ['mechanics', 'Mechanics', 'cog'], ['automation', 'Automation', 'rotate']],
  items: [['obtain', 'How to get it', 'gem'], ['uses', 'Uses', 'check']],
  mobs: [['behaviour', 'Behaviour', 'skull'], ['spawning', 'Spawning', 'layers'], ['ai', 'AI', 'cog'], ['attacks', 'Attacks', 'warning'], ['weaknesses', 'Weaknesses', 'check'], ['loot', 'Loot', 'gem'], ['farming', 'Farming', 'rotate'], ['strategies', 'Strategies', 'strategy']],
  biomes: [['climate', 'Climate', 'drop'], ['features', 'Features', 'mountain'], ['resources', 'Resources', 'gem'], ['strategies', 'Strategies', 'strategy']],
};

export function entryPage(view, e, query) {
  visit(e.id);
  document.title = `${e.name} · ${db.site.modName} Codex`;
  const cat = e.cat;
  const sections = buildSections(e);

  view.innerHTML = `<div class="codex">
    ${listPane(cat, e)}
    <article class="detail">
      ${hero(e)}
      <header class="d-head">
        <div>
          ${crumbs([['Codex', '#/'], [cat.label, catHref(cat)], ...(subcat(e) ? [[subcat(e), `${catHref(cat)}?tag=${encodeURIComponent(subcat(e))}`]] : []), [e.name]])}
          <h1>${esc(e.name)}</h1>
          <div class="chips">
            ${isNew(e) ? '<span class="chip on">New</span>' : ''}
            ${e.chapter ? `<span class="chip">Chapter ${esc(e.chapter)}</span>` : ''}
            ${e.stage ? `<a class="chip" href="#/progression">${esc(stageName(e.stage))}</a>` : ''}
            ${(e.tags ?? []).map(t => `<a class="chip" href="${catHref(cat)}?tag=${encodeURIComponent(t)}">${esc(t)}</a>`).join('')}
            ${(e.requiresMods ?? []).map(m => `<span class="chip">needs ${esc(m)}</span>`).join('')}
          </div>
        </div>
        <div class="actions">
          ${starButton(e.id, 'icon-btn')}
          <button class="icon-btn" onclick="print()" title="Print this page">${icon('print')}</button>
        </div>
      </header>
      ${e.summary ? `<p class="d-summary">${md(e.summary)}</p>` : ''}
      <nav class="toc" aria-label="On this page">${sections.map((s, i) => `<a href="#/${cat.id}/${e.id}?s=${s.id}" data-s="${s.id}" class="${i ? '' : 'on'}">${esc(s.title)}</a>`).join('')}</nav>
      <div class="d-body">${sections.map(s => `<section class="d-sec" id="s-${s.id}"><h2>${icon(s.icon)}${esc(s.title)}</h2>${s.html}</section>`).join('')}</div>
    </article>
    ${readout(e)}
  </div>`;

  wireHero(view);
  wireList(view);
  wireToc(view, query.s);
}

// ---------------------------------------------------------------- left: the category's list

function listPane(cat, current) {
  const groups = new Map();
  for (const x of cat.entries) {
    const g = cat.id === 'guide' ? '' : subcat(x);
    if (!groups.has(g)) groups.set(g, []);
    groups.get(g).push(x);
  }
  return `<aside class="list" aria-label="${esc(cat.label)}">
    <div class="lhead"><a href="${catHref(cat)}">${esc(cat.label)}</a><span class="count">${cat.entries.length}</span></div>
    <label class="lfilter">${icon('search')}<input type="search" placeholder="Filter ${esc(cat.label.toLowerCase())}" aria-label="Filter the list"></label>
    <div class="items">${[...groups].map(([g, list]) => `${g ? `<div class="gh">${esc(g)}</div>` : ''}${list.map(x => listItem(x, x === current)).join('')}`).join('')}</div>
  </aside>`;
}

function wireList(view) {
  const pane = view.querySelector('.list');
  if (!pane) return;
  // bring the current entry into the list's view without moving the page
  const items = pane.querySelector('.items');
  const on = items.querySelector('.li.on');
  if (on) items.scrollTop = on.offsetTop - items.offsetTop - items.clientHeight / 2 + on.offsetHeight / 2;
  pane.querySelector('input').addEventListener('input', e => {
    const q = e.target.value.trim().toLowerCase();
    pane.querySelectorAll('.li').forEach(li => { li.hidden = q && !li.textContent.toLowerCase().includes(q); });
    pane.querySelectorAll('.gh').forEach(gh => {
      let n = gh.nextElementSibling, any = false;
      while (n && !n.classList.contains('gh')) { if (!n.hidden) any = true; n = n.nextElementSibling; }
      gh.hidden = !any;
    });
  });
}

// ---------------------------------------------------------------- middle: render and gallery

function hero(e) {
  const shots = [{ images: entryImages(e), caption: e.name }];
  for (const v of e.variants ?? []) shots.push({ images: entryImages(e, v), caption: v.name, variant: v });
  for (const g of e.gallery ?? []) shots.push({ images: [new URL(`assets/${g.src}`, document.baseURI).href], caption: g.caption ?? e.name });
  return `<div class="d-hero ${e.cat.id === 'mobs' ? 'mob' : ''}">
    <div class="hero-pic">${picture(shots[0].images, { alt: e.name, zoom: 'entry', caption: e.name, eager: true })}</div>
    <button class="zoom" data-zoom-for=".d-hero .hero-pic img">${icon('zoom')}zoom</button>
  </div>
  ${shots.length > 1 ? `<div class="thumbs">${shots.map((s, i) => `<button class="${i ? '' : 'on'}" data-shot="${i}" title="${esc(s.caption)}">${picture(s.images, { alt: s.caption, zoom: 'entry', caption: s.caption })}</button>`).join('')}</div>` : ''}`;
}

function wireHero(view) {
  const heroBox = view.querySelector('.hero-pic');
  view.querySelectorAll('.thumbs button').forEach(b => b.addEventListener('click', ev => {
    ev.stopPropagation();
    ev.preventDefault();
    view.querySelectorAll('.thumbs button').forEach(x => x.classList.toggle('on', x === b));
    const thumb = b.querySelector('.thumb');
    heroBox.innerHTML = thumb.outerHTML;
  }, true));
}

// ---------------------------------------------------------------- middle: sections

function buildSections(e) {
  const out = [];
  const add = (id, title, ic, html) => { if (html && html.trim()) out.push({ id, title, icon: ic, html }); };

  add('overview', 'At a glance', 'info', teach(e.teach) || bullets([e.summary]));

  if (e.cat.id === 'machines' && e.power) add('power', 'Power', POWER_ICON[e.power.kind] ?? 'bolt', power(e.power));
  for (const [field, title, ic] of FIELDS[e.cat.id] ?? []) add(field, title, ic, bullets(e[field]));
  if (e.cat.id === 'biomes' && e.mobs?.length) add('mobs-here', 'Mobs', 'skull', `<div class="dossiers">${entriesOf(e.mobs).map(m => entryCard(m)).join('')}</div>`);

  if (e.setups?.length) add('setups', 'Example setups', 'layers', e.setups.map(s => `<h4 style="margin:6px 0 8px">${md(s.title)}</h4>${body(s.body)}`).join(''));
  if (e.flows?.length) add('chains', e.cat.id === 'machines' ? 'Automation chains' : 'Process flow', 'arrow', e.flows.map(flow).join(''));
  for (const s of e.sections ?? []) add(s.id ?? s.title.toLowerCase().replace(/\W+/g, '-'), s.title, s.icon ?? 'book', body(s.body));

  if (e.variants?.length) add('variants', 'Tiers and kinds', 'layers', `<div class="variants">${e.variants.map(v => `<div class="variant">
      <div class="vh">${entryPic(e, { variant: v, zoom: 'variants', caption: v.name })}<b>${esc(v.name)}</b></div>
      ${v.summary ? `<p>${md(v.summary)}</p>` : ''}${kv(v.specs)}</div>`).join('')}</div>`);

  add('recipes', 'Recipes', 'grid', recipes(e));

  const tips = [...(e.tips ?? []).map(t => callout('tip', t)), ...(e.facts ?? []).map(f => callout('fact', f))];
  add('tips', 'Tips', 'fact', tips.length ? `<div class="callouts">${tips.join('')}</div>` : '');
  const mistakes = e.teach?.mistakes ?? [];
  add('mistakes', 'Mistakes to avoid', 'warning', mistakes.length ? `<div class="callouts">${mistakes.map(m => callout('warning', m)).join('')}</div>` : '');

  const related = entriesOf(e.related);
  add('related', 'Related', 'link', related.length ? `<div class="cards">${related.map(entryCard).join('')}</div>` : '');
  return out;
}

const POWER_ICON = { rotation: 'rotate', fe: 'bolt', lava: 'flame', none: 'check' };

/** How to make it (recipes that output any of its game ids) and what it's used in. */
function recipes(e) {
  const ids = new Set([...(e.registry ?? []), ...(e.variants ?? []).flatMap(v => v.registry ?? [])]);
  const made = new Set(), used = new Set();
  for (const id of ids) {
    (db.made.get(id) ?? []).forEach(r => made.add(r));
    (db.used.get(id) ?? []).forEach(r => used.add(r));
  }
  for (const r of made) used.delete(r);
  const usedList = [...used];
  let html = '';
  if (made.size) html += `<h4 class="label" style="margin-bottom:10px">How to make it</h4><div class="recipes">${[...made].map(recipeCard).join('')}</div>`;
  if (usedList.length) {
    html += `<h4 class="label" style="margin:${made.size ? 22 : 0}px 0 10px">Used in</h4><div class="recipes">${usedList.slice(0, 6).map(recipeCard).join('')}</div>`;
    if (usedList.length > 6) html += `<p style="margin-top:10px"><a class="link" href="#/recipes?item=${encodeURIComponent([...ids][0])}&mode=used">All ${usedList.length} recipes that use it</a></p>`;
  }
  return html;
}

// ---------------------------------------------------------------- right: readout

function readout(e) {
  const parts = [];
  if (e.stats?.length) parts.push(`<div><h4>Readout</h4>${stats(e.stats)}</div>`);
  if (e.cat.id === 'mobs') parts.push(`<div><h4>Dossier</h4><div class="meters">
    <span>Threat</span>${pips(e.threat ?? 0, 5, 'threat')}<span>Rarity</span>${pips(e.rarity ?? 0, 5, 'rarity')}<span>Stage</span>${pips(stageIndex(e) + 1, 4, 'stage')}
    </div>${e.kind ? `<p class="muted" style="font-size:13px;margin-top:8px">${esc(e.kind)}</p>` : ''}</div>`);
  if (e.power) parts.push(`<div><h4>Power</h4>${power(e.power)}</div>`);
  if (e.specs?.length) parts.push(`<div><h4>Specs</h4>${kv(e.specs)}</div>`);

  const first = entriesOf(e.requires);
  if (first.length) parts.push(`<div><h4>Build first</h4><div class="mini">${first.map(mini).join('')}</div></div>`);
  const unlocks = db.all.filter(x => x.requires?.includes(e.id));
  if (unlocks.length) parts.push(`<div><h4>Leads to</h4><div class="mini">${unlocks.slice(0, 8).map(mini).join('')}</div></div>`);
  if (e.stage) parts.push(`<div><h4>Progression</h4><div class="meters"><span>${esc(stageName(e.stage))}</span>${pips(stageIndex(e) + 1, 4, 'stage')}</div></div>`);
  return `<aside class="readout" aria-label="Readout">${parts.join('')}</aside>`;
}

const mini = x => `<a href="${href(x)}">${entryPic(x)}<span>${esc(x.name)}</span></a>`;

// ---------------------------------------------------------------- the table of contents follows the scroll

function wireToc(view, start) {
  const links = [...view.querySelectorAll('.toc a')];
  const secs = links.map(a => view.querySelector(`#s-${a.dataset.s}`));
  links.forEach((a, i) => a.addEventListener('click', ev => {
    ev.preventDefault();
    history.replaceState(null, '', a.getAttribute('href'));
    secs[i].scrollIntoView({ behavior: 'smooth', block: 'start' });
  }));
  if (start) {
    const target = view.querySelector(`#s-${CSS.escape(start)}`);
    if (target) requestAnimationFrame(() => target.scrollIntoView({ block: 'start' }));
  }
  if (!('IntersectionObserver' in window) || !secs.length) return;
  const visible = new Set();
  const io = new IntersectionObserver(items => {
    items.forEach(i => (i.isIntersecting ? visible.add(i.target) : visible.delete(i.target)));
    const top = secs.find(s => visible.has(s));
    if (!top) return;
    links.forEach((a, i) => a.classList.toggle('on', secs[i] === top));
    // keep the lit tab in view by scrolling the bar itself: scrollIntoView would also move the page under the
    // reader's scroll and make it stick
    const toc = view.querySelector('.toc');
    const on = toc?.querySelector('a.on');
    if (on && (on.offsetLeft < toc.scrollLeft || on.offsetLeft + on.offsetWidth > toc.scrollLeft + toc.clientWidth))
      toc.scrollTo({ left: on.offsetLeft - 24, behavior: 'smooth' });
  }, { rootMargin: '-120px 0px -55% 0px' });
  secs.forEach(s => io.observe(s));
}
