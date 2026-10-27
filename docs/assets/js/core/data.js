// Loads the data files once and indexes them: entries by id, by category and by game id; recipes by what they make
// and what they use; item names. Every page reads from here.

import { setLinkResolver, titleCase } from './text.js';

const BASE = new URL('../../../', import.meta.url);   // docs/
const url = path => new URL(path, BASE).href;

export const db = {
  site: null,
  categories: [],          // site.json categories, each with .entries
  category: new Map(),     // id -> category
  entries: new Map(),      // entry id -> entry (with .cat)
  all: [],                 // every entry, in file order
  byGame: new Map(),       // game id -> { entry, variant }
  recipes: [],
  made: new Map(),         // game id -> recipes that make it
  used: new Map(),         // game id -> recipes that use it
  names: {},               // game id -> name
};

/** Fetches a JSON file, revalidating with the server (cheap 304s) so edits show up on reload. */
async function json(path, fallback) {
  try {
    const res = await fetch(url(path), { cache: 'no-cache' });
    if (!res.ok) throw new Error(`${res.status} ${path}`);
    return await res.json();
  } catch (e) {
    if (fallback !== undefined) return fallback;
    throw e;
  }
}

export async function load() {
  db.site = await json('data/site.json');
  const files = db.site.categories.filter(c => c.file);
  const [lists, recipes, names] = await Promise.all([
    Promise.all(files.map(c => json(`data/${c.file}`, []))),
    json('data/recipes.json', []),
    json('data/names.json', {}),
  ]);
  db.names = names;

  db.categories = db.site.categories.map(c => ({ ...c, entries: [] }));
  db.categories.forEach(c => db.category.set(c.id, c));
  files.forEach((c, i) => {
    const cat = db.category.get(c.id);
    for (const e of lists[i]) {
      if (!e?.id || db.entries.has(e.id)) {
        console.warn(`Skipped an entry in ${c.file}: ${e?.id ? `duplicate id "${e.id}"` : 'no id'}`);
        continue;
      }
      e.cat = cat;
      cat.entries.push(e);
      db.entries.set(e.id, e);
      db.all.push(e);
      for (const g of e.registry ?? []) if (!db.byGame.has(g)) db.byGame.set(g, { entry: e });
      for (const v of e.variants ?? []) for (const g of v.registry ?? []) db.byGame.set(g, { entry: e, variant: v });
    }
  });
  // the guide reads in chapter order
  db.category.get('guide')?.entries.sort((a, b) => (a.chapter ?? 99) - (b.chapter ?? 99));

  db.recipes = recipes;
  for (const r of recipes) {
    for (const o of r.outputs ?? []) push(db.made, o.item, r);
    for (const i of ingredients(r)) push(db.used, i, r);
  }

  setLinkResolver(id => {
    const e = db.entries.get(id);
    if (e) return { href: href(e), name: e.name };
    if (id.includes(':')) return { href: itemHref(id), name: itemName(id) };
    return null;
  });
  return db;
}

function push(map, key, value) {
  if (!key) return;
  const list = map.get(key);
  if (!list) map.set(key, [value]); else if (!list.includes(value)) list.push(value);
}

/** Every distinct game id a recipe takes in. */
export function ingredients(r) {
  const ids = new Set();
  const add = i => i?.item && ids.add(i.item);
  r.grid?.flat().forEach(add);
  r.inputs?.forEach(add);
  r.sequence?.forEach(s => s.inputs?.forEach(add));
  return [...ids];
}

// ---------------------------------------------------------------- addresses

export const href = (e, anchor) => `#/${e.cat.id}/${e.id}${anchor ? `?s=${anchor}` : ''}`;
export const catHref = c => `#/${c.id}`;

/** Where a game id goes: its entry if one covers it, otherwise its own item page. */
export function itemHref(id) {
  const hit = db.byGame.get(id);
  return hit ? href(hit.entry) : `#/item/${id.replace(':', '/')}`;
}

export function itemName(id) {
  if (!id) return '';
  const hit = db.byGame.get(id);
  if (hit?.variant?.name && hit.variant.registry?.length === 1) return hit.variant.name;
  return db.names[id] ?? (hit && hit.entry.registry?.[0] === id ? hit.entry.name : null) ?? titleCase(id.split(':')[1].split('/').pop());
}

// ---------------------------------------------------------------- pictures

/** The images to try for an entry, best first: its render, then its first item's icon. */
export function entryImages(e, variant) {
  const list = [];
  if (variant) {
    if (variant.image) list.push(url(`assets/${variant.image}`));
    list.push(url(`assets/renders/${e.id}--${variant.id}.png`));
    // the tier the main render already shows (same block) uses it; another block falls back to its own icon
    if (variant.registry?.[0] && variant.registry[0] !== e.registry?.[0]) {
      list.push(iconUrl(variant.registry[0]));
      return list;
    }
  }
  if (e.image) list.push(url(`assets/${e.image}`));
  list.push(url(`assets/renders/${e.id}.png`));
  const first = e.registry?.find(Boolean);
  if (first) list.push(iconUrl(first));
  return list;
}

/** The mod a game id comes from, as players know it (site.json "mods" names them). */
export const modName = id => {
  const ns = id.split(':')[0];
  return db.site.mods?.[ns] ?? titleCase(ns);
};

export const iconUrl = id => {
  const [ns, path] = id.split(':');
  return url(`assets/icons/${ns}/${path}.png`);
};

export const assetUrl = path => url(`assets/${path}`);

// ---------------------------------------------------------------- lookups

export const entry = id => db.entries.get(id);
export const entriesOf = ids => (ids ?? []).map(id => db.entries.get(id)).filter(Boolean);

/** Is this entry one of the site's latest additions? */
export const isNew = e => (db.site.home?.latest ?? []).includes(e.id);

/** The subcategory shown in lists: the first tag. */
export const subcat = e => e.tags?.[0] ?? '';

export const STAGES = ['early', 'mid', 'advanced', 'end'];
export const stageIndex = e => Math.max(0, STAGES.indexOf(e.stage));
export const stageName = s => db.site.progression?.find(p => p.id === s)?.title ?? titleCase(s ?? '');
