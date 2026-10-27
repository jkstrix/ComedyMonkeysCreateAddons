// Checks the website's data before publishing. Run from the repo root:  node tools/website/check-data.mjs
// Errors (exit 1): unreadable files, duplicate or missing ids, links to entries that don't exist.
// Warnings: entries without a picture, mod items no entry covers, entries missing their teaching.

import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const DOCS = join(process.cwd(), 'docs');
const errors = [], warnings = [];
const read = f => { try { return JSON.parse(readFileSync(join(DOCS, 'data', f), 'utf8')); } catch (e) { errors.push(`${f}: ${e.message}`); return null; } };

const site = read('site.json');
if (!site) { console.error(errors.join('\n')); process.exit(1); }
const recipes = read('recipes.json') ?? [];
const entries = new Map();
for (const c of site.categories.filter(c => c.file)) {
  for (const e of read(c.file) ?? []) {
    if (!e.id || !e.name) { errors.push(`${c.file}: an entry has no id or name (${JSON.stringify(e).slice(0, 60)}…)`); continue; }
    if (entries.has(e.id)) errors.push(`${c.file}: duplicate id "${e.id}" (also in ${entries.get(e.id).file})`);
    entries.set(e.id, { ...e, file: c.file, cat: c.id });
  }
}

const has = id => entries.has(id);
const check = (where, id) => { if (id && !has(id)) errors.push(`${where}: no entry "${id}"`); };
const links = (where, v) => {
  if (typeof v === 'string') for (const m of v.matchAll(/\[\[([a-z0-9:_/.-]+)(?:\|[^\]]+)?\]\]/g)) if (!m[1].includes(':')) check(where, m[1]);
  else if (Array.isArray(v)) v.forEach(x => links(where, x));
  else if (v && typeof v === 'object') Object.entries(v).forEach(([k, x]) => (k === 'ref' ? check(where, x) : k === 'entries' && Array.isArray(x) ? x.forEach(i => check(where, i)) : links(where, x)));
};

for (const e of entries.values()) {
  const where = `${e.file} ${e.id}`;
  (e.requires ?? []).forEach(r => check(`${where} requires`, r));
  (e.related ?? []).forEach(r => check(`${where} related`, r));
  links(where, e);
  if (!e.summary) warnings.push(`${where}: no summary`);
  if (['blocks', 'machines', 'items', 'mobs'].includes(e.cat) && !e.teach) warnings.push(`${where}: no "teach" (the five questions)`);
  const pics = [e.image && `assets/${e.image}`, `assets/renders/${e.id}.png`, ...(e.registry ?? []).map(g => `assets/icons/${g.replace(':', '/')}.png`)].filter(Boolean);
  if (!pics.some(p => existsSync(join(DOCS, p)))) warnings.push(`${where}: no picture yet (add a scene to tools/website/renders.json, or "image")`);
}
const h = site.home ?? {};
for (const k of ['latest', 'featuredMachines', 'featuredBlocks', 'featuredMobs']) (h[k] ?? []).forEach(id => check(`site.json home.${k}`, id));
check('site.json home.hero', h.hero?.entry);
for (const s of site.progression ?? []) { (s.entries ?? []).forEach(id => check(`site.json progression ${s.id}`, id)); links(`site.json progression ${s.id}`, s.goals); }

// every item of the mod that a recipe makes should have an entry that covers it
const covered = new Set([...entries.values()].flatMap(e => [...(e.registry ?? []), ...(e.variants ?? []).flatMap(v => v.registry ?? [])]));
const unreleased = new Set(site.unreleased ?? []);
for (const r of recipes) for (const o of r.outputs) {
  if (o.item?.startsWith('comedymonkeysaddons:') && !covered.has(o.item) && !unreleased.has(o.item)) warnings.push(`recipes: ${o.item} has no entry (add it to an entry's "registry")`);
}

const unique = a => [...new Set(a)];
unique(warnings).forEach(w => console.log(`warning  ${w}`));
unique(errors).forEach(e => console.log(`ERROR    ${e}`));
console.log(`\n${entries.size} entries, ${recipes.length} recipes: ${unique(errors).length} errors, ${unique(warnings).length} warnings`);
process.exit(errors.length ? 1 : 0);
