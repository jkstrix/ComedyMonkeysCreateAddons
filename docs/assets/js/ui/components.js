// The building blocks every page is made of. Each returns an HTML string; data is always escaped (md() escapes too).

import { db, href, catHref, itemHref, itemName, iconUrl, entryImages, entry as getEntry, isNew, subcat, stageIndex, stageName, assetUrl } from '../core/data.js';
import { esc, md, titleCase } from '../core/text.js';
import { isFavourite } from '../core/storage.js';
import { icon } from './icons.js';
import { gear } from './gear.js';

// ---------------------------------------------------------------- pictures

const PENDING = `<div class="pending">${gear(10)}<small>render pending</small></div>`;

/**
 * A picture that tries its sources in order (render, then icon) and ends on a "render pending" plate, or for an item
 * on a lettered tile. Icons are drawn pixel-sharp. `zoom` puts it in a lightbox group.
 */
export function picture(sources, { alt = '', cls = '', zoom = null, caption = '', text = null, eager = false } = {}) {
  const list = sources.filter(Boolean);
  if (!list.length) return `<div class="thumb ${cls}">${text ? textTile(text) : PENDING}</div>`;
  const isIcon = /\/icons\//.test(list[0]);
  return `<div class="thumb ${isIcon ? 'icon' : ''} ${cls}"${text ? ` data-text="${esc(text)}"` : ''}>` +
    `<img src="${esc(list[0])}" alt="${esc(alt)}" ${eager ? '' : 'loading="lazy"'} decoding="async"` +
    `${list.length > 1 ? ` data-alt="${esc(list.slice(1).join('|'))}"` : ''}` +
    `${zoom ? ` data-zoom="${esc(zoom)}" data-caption="${esc(caption || alt)}"` : ''}></div>`;
}

/** Image errors: try the next source, then fall back. One listener for the whole page. */
export function installImageFallbacks() {
  document.addEventListener('error', e => {
    const img = e.target;
    if (!(img instanceof HTMLImageElement) || !img.closest('.thumb')) return;
    const box = img.closest('.thumb');
    const rest = (img.dataset.alt ?? '').split('|').filter(Boolean);
    if (rest.length) {
      img.dataset.alt = rest.slice(1).join('|');
      box.classList.toggle('icon', /\/icons\//.test(rest[0]));
      img.src = rest[0];
    } else {
      box.classList.remove('icon');
      box.innerHTML = box.dataset.text ? textTile(box.dataset.text) : PENDING;
    }
  }, true);
}

const MATERIALS = [
  [/netherite/, '#4a4042'], [/brass/, '#b8913f'], [/copper/, '#b8693e'], [/gold/, '#c9a43a'], [/iron|chain/, '#a9a9a9'],
  [/zinc/, '#93a69c'], [/andesite/, '#8d918b'], [/obsidian/, '#2a1e3a'], [/diamond/, '#4fbdb8'], [/emerald/, '#2f9e5c'],
  [/redstone/, '#a8322a'], [/glass/, '#7fb3bd'], [/plank|log|stick|wood|fence/, '#8a6a3e'], [/lava|magma|blaze/, '#c45a26'],
  [/water|kelp|ice/, '#3f76b3'], [/slime/, '#5aa04a'], [/bone/, '#cfc9b0'],
];

/** A lettered tile for an item with no icon: two letters on its material's colour. */
function textTile(name) {
  const lower = name.toLowerCase();
  const colour = MATERIALS.find(([re]) => re.test(lower))?.[1] ?? '#5d646d';
  const letters = name.split(/\s+/).filter(Boolean).map(w => w[0]).join('').slice(0, 2).toUpperCase();
  return `<div class="tile-text" style="background:${colour}">${esc(letters)}</div>`;
}

/** A game item's icon. */
export const itemPic = (id, cls = '') => picture([iconUrl(id)], { alt: itemName(id), cls, text: itemName(id) });

/** An entry's picture: render, then icon. */
export const entryPic = (e, opts = {}) => picture(entryImages(e, opts.variant), { alt: e.name, ...opts });

// ---------------------------------------------------------------- small parts

export const starButton = (id, cls = 'star-btn') =>
  `<button class="${cls} ${isFavourite(id) ? 'on' : ''}" data-fav="${esc(id)}" aria-pressed="${isFavourite(id)}" title="Favourite">${icon(isFavourite(id) ? 'starFill' : 'star')}<span class="sr-only">Favourite</span></button>`;

export function crumbs(parts) {
  return `<nav class="crumbs" aria-label="Breadcrumbs">${parts.map(([label, link], i) =>
    (i ? '<span class="sep">/</span>' : '') + (link ? `<a href="${link}">${esc(label)}</a>` : `<span>${esc(label)}</span>`)).join('')}</nav>`;
}

export const pips = (n, max, kind) =>
  `<span class="pips ${kind}" role="img" aria-label="${n} of ${max}">${Array.from({ length: max }, (_, i) => `<i class="${i < n ? 'on' : ''}"></i>`).join('')}</span>`;

export const kv = pairs => pairs?.length
  ? `<div class="kv">${pairs.map(([k, v]) => `<div><span>${md(k)}</span><span>${md(v)}</span></div>`).join('')}</div>` : '';

export const badge = (text, cls = '') => `<span class="badge ${cls}">${text}</span>`;

const CALLOUT = {
  tip: ['info', 'Pro tip'], warning: ['warning', 'Common mistake'], fact: ['fact', 'Did you know'], strategy: ['strategy', 'Strategy'],
};

/** A tip, warning, fact or strategy panel. */
export function callout(kind, text, title) {
  const [ic, label] = CALLOUT[kind] ?? CALLOUT.tip;
  return `<div class="callout ${kind}">${icon(ic)}<div><b class="t">${esc(title ?? label)}</b>${md(text)}</div></div>`;
}

// ---------------------------------------------------------------- cards

/** The card used in grids: render, name, summary, a couple of badges. */
export function entryCard(e) {
  const stat = e.stats?.[0];
  return `<article class="card reveal">
    ${starButton(e.id)}
    <a href="${href(e)}" class="pic">${entryPic(e)}</a>
    <a href="${href(e)}" class="body">
      <span class="cat">${esc(e.cat.single ?? e.cat.label)}${subcat(e) ? ` · ${esc(subcat(e))}` : ''}</span>
      <h3>${esc(e.name)}</h3>
      <p>${md(e.summary, false)}</p>
      <span class="meta">
        ${isNew(e) ? badge('new', 'new') : ''}
        ${e.stage ? badge(esc(stageName(e.stage))) : ''}
        ${stat ? badge(`<b>${esc(stat.value)}</b> ${esc(stat.unit ?? '')}`, 'stat') : ''}
      </span>
    </a>
  </article>`;
}

/** A mob's dossier: picture, threat, rarity and stage bars. */
export function dossier(e) {
  return `<a class="dossier reveal" href="${href(e)}" style="${e.glow ? `--glow:${esc(e.glow)}` : ''}">
    <div class="pic">${entryPic(e)}</div>
    <div class="info">
      <span class="role">${esc(e.kind ?? e.cat.single ?? '')}</span>
      <h3>${esc(e.name)}</h3>
      <div class="meters">
        <span>Threat</span>${pips(e.threat ?? 0, 5, 'threat')}
        <span>Rarity</span>${pips(e.rarity ?? 0, 5, 'rarity')}
        <span>Stage</span>${pips(stageIndex(e) + 1, 4, 'stage')}
      </div>
      <p>${md(e.summary, false)}</p>
    </div>
  </a>`;
}

/** A one-line row (search results, favourites, item lists). */
export function row(e, { sub, title } = {}) {
  return `<a class="row" href="${href(e)}">${entryPic(e)}<span><b>${title ?? esc(e.name)}</b><small>${sub ?? `${esc(e.cat.single ?? e.cat.label)} · ${md(e.summary, false)}`}</small></span></a>`;
}

/** The codex list item (left column). */
export function listItem(e, on) {
  const stat = e.stats?.[0];
  return `<a class="li ${on ? 'on' : ''}" href="${href(e)}" data-id="${esc(e.id)}">${entryPic(e)}<span><b>${esc(e.name)}</b>` +
    `<small>${esc(subcat(e))}${stat ? ` · ${esc(stat.value)} ${esc(stat.unit ?? '')}` : ''}</small></span>` +
    `${isFavourite(e.id) ? icon('starFill', 'star') : ''}</a>`;
}

export function categoryTile(c) {
  const n = c.entries.length;
  return `<a class="tile reveal" href="${catHref(c)}"><b>${esc(c.label)}</b><span>${c.status === 'soon' ? 'coming soon' : `${n} ${n === 1 ? 'entry' : 'entries'}`}</span>` +
    `${c.status === 'soon' ? '<i class="soon">SOON</i>' : ''}${icon(c.icon)}</a>`;
}

// ---------------------------------------------------------------- teaching

const QUESTIONS = [['what', 'What it does'], ['why', 'Why use it'], ['when', 'When to unlock it'], ['interacts', 'How it fits in']];

/** The five questions every entry answers (the mistakes go in their own panels). */
export function teach(t) {
  if (!t) return '';
  const cells = QUESTIONS.filter(([k]) => t[k]).map(([k, label]) => `<div><small>${label}</small><p>${md(t[k])}</p></div>`);
  return cells.length ? `<div class="qa">${cells.join('')}</div>` : '';
}

// ---------------------------------------------------------------- readouts

/**
 * A dial: `gauge` (0..1) sets how far round it is. The needle swings there when it scrolls into view.
 */
export function gaugeDial(s, i = 0) {
  const g = Math.max(0, Math.min(1, s.gauge));
  const len = 119.4;
  const colours = ['var(--copper)', 'var(--cyan)', 'var(--amber)', 'var(--violet)'];
  return `<div class="gauge">
    <svg viewBox="0 0 100 62"><path d="M12 56a38 38 0 0 1 76 0" fill="none" stroke="var(--panel-2)" stroke-width="8" stroke-linecap="round"/>
      <path class="arc" d="M12 56a38 38 0 0 1 76 0" fill="none" stroke="${colours[i % colours.length]}" stroke-width="8" stroke-linecap="round"
        stroke-dasharray="${len}" stroke-dashoffset="${len}" data-offset="${(len * (1 - g)).toFixed(1)}"/>
      <g class="needle" data-angle="${(-90 + 180 * g).toFixed(1)}" style="transform:rotate(-90deg)"><path d="M50 56L50 26" stroke="var(--text)" stroke-width="2.5" stroke-linecap="round"/></g>
      <circle cx="50" cy="56" r="4" fill="var(--text)"/></svg>
    <b>${esc(s.value)}</b><small>${esc(s.unit ?? '')}</small><div class="lbl">${esc(s.label)}</div>
  </div>`;
}

/** An entry's headline numbers: dials for the ones with a gauge, plates for the rest. */
export function stats(list) {
  if (!list?.length) return '';
  const dials = list.filter(s => typeof s.gauge === 'number');
  const plain = list.filter(s => typeof s.gauge !== 'number');
  return (dials.length ? `<div class="gauges">${dials.map(gaugeDial).join('')}</div>` : '') +
    (plain.length ? `<div class="bignums" style="${dials.length ? 'margin-top:10px' : ''}">${plain.map(s =>
      `<div class="bignum"><b>${esc(s.value)}</b><small>${esc(s.unit ?? '')}</small><div class="label" style="margin-top:4px">${esc(s.label)}</div></div>`).join('')}</div>` : '');
}

const POWER = { rotation: ['rotate', 'Rotation'], fe: ['bolt', 'Forge Energy'], lava: ['flame', 'Lava'], none: ['check', 'No power'] };

export function power(p) {
  if (!p) return '';
  const [ic, label] = POWER[p.kind] ?? POWER.none;
  const lines = [p.stress, p.speed, p.draws, p.makes].filter(Boolean);
  return `<div class="power">${icon(ic)}<span><b>${esc(label)}</b>${lines.map(l => `<small>${md(l)}</small>`).join('')}</span></div>`;
}

// ---------------------------------------------------------------- process flows

function flowNode(step) {
  const target = step.ref ? getEntry(step.ref) : null;
  const label = step.label ?? target?.name ?? (step.item ? itemName(step.item) : '');
  const pic = target ? entryPic(target) : step.item ? itemPic(step.item) : '';
  const link = target ? href(target) : step.item ? itemHref(step.item) : null;
  const inner = `${step.note ? `<small>${esc(step.note)}</small>` : ''}${pic}<b>${esc(label)}</b>`;
  const cls = `fn ${esc(step.kind ?? (target?.cat.id === 'machines' ? 'machine' : 'item'))}`;
  return link ? `<a class="${cls}" href="${link}">${inner}</a>` : `<div class="${cls}">${inner}</div>`;
}

export function flow(f) {
  const steps = f.steps ?? [];
  // each arrow travels with the step it points to, so a wrapped flow never ends a line on an arrow
  return `<div class="flow-wrap">${f.title ? `<div class="flow-title">${md(f.title)}</div>` : ''}<div class="flow">${steps.map((s, i) =>
    `<div class="fstep">${i ? `<div class="fa">${s.via ? `<span>${esc(s.via)}</span>` : ''}</div>` : ''}${flowNode(s)}</div>`).join('')}</div></div>`;
}

// ---------------------------------------------------------------- recipes

function slot(ing, { big = false } = {}) {
  if (!ing) return `<span class="slot${big ? ' big' : ''}"></span>`;
  if (ing.fluid) {
    const name = itemName(ing.fluid);
    return `<a class="slot${big ? ' big' : ''}" href="${itemHref(ing.fluid)}" title="${esc(`${ing.amount} mB ${name}`)}">${itemPic(ing.fluid)}<span class="n">${ing.amount >= 1000 ? `${ing.amount / 1000}B` : ing.amount}</span></a>`;
  }
  const name = ing.item ? itemName(ing.item) : (ing.label ?? '?');
  const tip = ing.tag ? `${name} (any #${ing.tag})` : name;
  const pic = ing.item ? itemPic(ing.item) : '';
  const count = ing.count > 1 ? `<span class="n">${ing.count}</span>` : '';
  const chance = ing.chance != null ? `<span class="p">${+(ing.chance * 100).toFixed(1)}%</span>` : '';
  return ing.item
    ? `<a class="slot${big ? ' big' : ''}" href="${itemHref(ing.item)}" title="${esc(tip)}">${pic}${count}${chance}</a>`
    : `<span class="slot${big ? ' big' : ''}" title="${esc(tip)}">${count}</span>`;
}

const slotsGrid = (items, cols) =>
  `<div class="mc-panel"><div class="grid-in" style="grid-template-columns:repeat(${cols},38px)">${items.map(i => slot(i)).join('')}</div></div>`;

/** A recipe in Minecraft's own style: grid or inputs, the machine, and what comes out. */
export function recipeCard(r) {
  const type = db.site.recipeTypes?.[r.type] ?? { label: titleCase(r.type) };
  const machineEntry = type.machine ? db.byGame.get(type.machine)?.entry : null;
  const machine = type.machine
    ? `<${machineEntry ? `a href="${href(machineEntry)}"` : 'span'} class="r-machine" title="${esc(itemName(type.machine))}">${itemPic(type.machine)}</${machineEntry ? 'a' : 'span'}>` : '';
  const heat = r.heat && r.heat !== 'none' ? `<span class="heat ${esc(r.heat)}">${esc(r.heat)}</span>` : '';
  const time = r.time && type.timeAt ? badge(`${+(r.time / 20).toFixed(2)} s at ${esc(type.timeAt)}`) : '';
  const mods = [...(r.requiresMods ?? []).map(m => badge(`needs ${esc(m)}`)), ...(r.unlessMods ?? []).map(m => badge(`without ${esc(m)}`))].join('');

  let input;
  if (r.grid) {
    const cols = Math.max(...r.grid.map(row => row.length));
    input = slotsGrid(r.grid.flatMap(row => [...row, ...Array(cols - row.length).fill(null)]), cols);
  } else if (r.type === 'sequenced_assembly') {
    const steps = r.sequence.map(s => {
      const what = s.inputs.map(i => i.fluid ? `${i.amount} mB ${itemName(i.fluid)}` : `${i.count > 1 ? `${i.count} ` : ''}${itemName(i.item)}`).join(', ');
      const verb = { deploying: 'Deploy', filling: 'Fill with', pressing: 'Press', cutting: 'Cut' }[s.type] ?? titleCase(s.type);
      const first = s.inputs[0];
      return `<li>${first ? itemPic(first.fluid ?? first.item) : ''}<span>${esc(verb)} ${esc(what)}</span></li>`;
    }).join('');
    input = `${slotsGrid(r.inputs, 1)}<div class="r-steps"><ol>${steps}</ol>${r.loops > 1 ? `<span class="badge">repeat ×${r.loops}</span>` : ''}</div>`;
  } else {
    const ins = r.inputs ?? [];
    input = slotsGrid(ins, Math.min(3, Math.max(1, ins.length)));
  }
  const outs = r.outputs.length === 1
    ? `<div class="mc-panel">${slot(r.outputs[0], { big: true })}</div>`
    : slotsGrid(r.outputs, Math.min(2, r.outputs.length));

  return `<article class="recipe reveal" id="r-${esc(r.id.replace(/[:/]/g, '-'))}">
    <div class="recipe-head"><span class="type">${esc(type.label)}</span>${heat}${time}<span class="req">${mods}</span></div>
    <div class="recipe-body">${input}<div class="r-machine">${machine}<div class="r-arrow"></div></div>${outs}</div>
    ${r.note ? `<p class="recipe-note">${esc(r.note)}</p>` : ''}
  </article>`;
}

// ---------------------------------------------------------------- long-form bodies

/** Renders a body: strings are paragraphs, objects are headings, lists, panels, tables, flows, cards and images. */
export function body(blocks) {
  if (!blocks) return '';
  if (typeof blocks === 'string') blocks = [blocks];
  return `<div class="prose">${blocks.map(block).join('')}</div>`;
}

function block(b) {
  if (typeof b === 'string') return `<p>${md(b)}</p>`;
  if (!b || typeof b !== 'object') return '';
  if (b.heading) return `<h4>${md(b.heading)}</h4>`;
  if (b.list) return `<${b.ordered ? 'ol' : 'ul'}>${b.list.map(i => `<li>${md(i)}</li>`).join('')}</${b.ordered ? 'ol' : 'ul'}>`;
  for (const k of ['tip', 'warning', 'fact']) if (b[k]) return callout(k, b[k]);
  if (b.strategy) return callout('strategy', typeof b.strategy === 'string' ? b.strategy : b.strategy.body, b.strategy.title);
  if (b.table) return `<div class="table-wrap"><table>${b.table.head ? `<thead><tr>${b.table.head.map(h => `<th>${md(h)}</th>`).join('')}</tr></thead>` : ''}<tbody>${b.table.rows.map(r => `<tr>${r.map(c => `<td>${md(c)}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
  if (b.flow) return flow(b.flow);
  if (b.entries) return `<div class="cards">${b.entries.map(getEntry).filter(Boolean).map(entryCard).join('')}</div>`;
  if (b.image) return `<figure class="shot">${picture([assetUrl(b.image.src)], { alt: b.image.caption ?? '', zoom: 'body', caption: b.image.caption })}${b.image.caption ? `<figcaption>${md(b.image.caption)}</figcaption>` : ''}</figure>`;
  if (b.recipe) { const r = db.recipes.find(x => x.id === b.recipe); return r ? `<div class="recipes">${recipeCard(r)}</div>` : ''; }
  return '';
}

/** A list of strings as a bulleted list (one string: just a paragraph). */
export const bullets = list => !list?.length ? '' : list.length === 1 ? body([list[0]]) : body([{ list }]);
