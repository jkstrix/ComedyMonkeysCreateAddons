// Client-side search over every entry, every item named in the recipes, and the systems and guide text.
// The index is built once, on first use. Ranking: name matches beat game ids, tags and summaries, which beat body text;
// a word that matches the start of a name word counts most, and every query word must match somewhere.

import { db, itemName, itemHref, href, entryImages, iconUrl, modName } from './data.js';
import { fold, plain } from './text.js';

let index = null;

const FIELDS = { name: 12, ids: 6, tags: 5, summary: 3, body: 1 };

/** All the text in an entry, minus its name and summary: lists, sections, teaching, variants... */
function bodyText(e) {
  const out = [];
  const walk = v => {
    if (v == null) return;
    if (typeof v === 'string') out.push(plain(v));
    else if (Array.isArray(v)) v.forEach(walk);
    else if (typeof v === 'object') Object.entries(v).forEach(([k, x]) => k !== 'id' && k !== 'ref' && walk(x));
  };
  const { id, name, summary, registry, tags, cat, ...rest } = e;
  walk(rest);
  return out.join(' ');
}

const words = s => fold(s).split(/[^a-z0-9]+/).filter(w => w.length > 1 || /\d/.test(w));

function build() {
  index = [];
  for (const e of db.all) {
    index.push({
      kind: 'entry', entry: e, cat: e.cat.id, title: e.name, sub: e.summary, href: href(e), images: entryImages(e),
      f: {
        name: words(e.name),
        ids: words([...(e.registry ?? []), ...(e.variants ?? []).flatMap(v => [v.name, ...(v.registry ?? [])])].join(' ')),
        tags: words([...(e.tags ?? []), e.stage, e.kind].join(' ')),
        summary: words(plain(e.summary)),
        body: words(bodyText(e)),
      },
    });
  }
  // game items no entry covers: Create's and vanilla ingredients, so "brass sheet" finds its recipes
  const items = new Set();
  db.recipes.forEach(r => [...(r.grid?.flat() ?? []), ...(r.inputs ?? []), ...(r.outputs ?? [])].forEach(i => i?.item && items.add(i.item)));
  for (const id of items) {
    if (db.byGame.has(id)) continue;
    const name = itemName(id);
    index.push({
      kind: 'item', cat: 'items', title: name, sub: `${modName(id)} item · recipes`,
      href: itemHref(id), images: [iconUrl(id)], icon: true,
      f: { name: words(name), ids: words(id), tags: [], summary: [], body: [] },
    });
  }
  index.forEach(d => { d.sets = Object.fromEntries(Object.entries(d.f).map(([k, v]) => [k, new Set(v)])); });
}

/**
 * Finds the best matches for a query. Returns [{ ...doc, score }], best first.
 * @param {string} query
 * @param {{ cat?: string, limit?: number }} opts
 */
export function search(query, { cat, limit = 50 } = {}) {
  if (!index) build();
  const terms = words(query);
  if (!terms.length) return [];
  const results = [];
  for (const d of index) {
    if (cat && d.cat !== cat) continue;
    let score = 0;
    let all = true;
    for (const t of terms) {
      let best = 0;
      for (const [field, weight] of Object.entries(FIELDS)) {
        const list = d.f[field];
        if (!list.length) continue;
        if (d.sets[field].has(t)) best = Math.max(best, weight * 1.5);
        else if (list.some(w => w.startsWith(t))) best = Math.max(best, weight);
        else if (t.length > 3 && list.some(w => w.includes(t))) best = Math.max(best, weight * 0.4);
      }
      if (!best) { all = false; break; }
      score += best;
    }
    if (!all) continue;
    const name = fold(d.title);
    const q = fold(query.trim());
    if (name === q) score += 40;
    else if (name.startsWith(q)) score += 18;
    if (d.kind === 'entry') score += 2;
    results.push({ ...d, score });
  }
  results.sort((a, b) => b.score - a.score || a.title.localeCompare(b.title));
  return results.slice(0, limit);
}

/** The query's words, for highlighting. */
export const queryWords = q => words(q);
