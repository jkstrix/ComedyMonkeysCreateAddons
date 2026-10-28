// Builds docs/data/recipes.json from the mod's recipe files, for the website's recipe book.
// Run from the repo root:  node tools/website/import-recipes.mjs
// No dependencies. Re-run whenever recipes change.

import { readFileSync, writeFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = process.cwd();
const DATA = join(ROOT, 'src/main/resources/data');
const OUT = join(ROOT, 'docs/data/recipes.json');
const NAMES = join(ROOT, 'docs/data/names.json');
const MOD = 'comedymonkeysaddons';

// The site shows a tag as one real item (its icon and name), with the tag kept for the tooltip.
const TAG_ITEMS = {
  'c:chests/wooden': 'minecraft:chest',
  'c:gems/diamond': 'minecraft:diamond',
  'c:glass_blocks': 'minecraft:glass',
  'c:ingots/copper': 'minecraft:copper_ingot',
  'c:ingots/iron': 'minecraft:iron_ingot',
  'c:ingots/netherite': 'minecraft:netherite_ingot',
  'c:nuggets/copper': 'create:copper_nugget',
  'c:nuggets/gold': 'minecraft:gold_nugget',
  'c:nuggets/iron': 'minecraft:iron_nugget',
  'c:nuggets/zinc': 'create:zinc_nugget',
  'c:plates/brass': 'create:brass_sheet',
  'c:plates/copper': 'create:copper_sheet',
  'c:plates/gold': 'create:golden_sheet',
  'c:plates/iron': 'create:iron_sheet',
  'c:slime_balls': 'minecraft:slime_ball',
  'c:dyes': 'minecraft:red_dye',
  'minecraft:coals': 'minecraft:coal',
  'minecraft:logs': 'minecraft:oak_log',
  'minecraft:wooden_fences': 'minecraft:oak_fence',
};

// Mods named in neoforge:mod_loaded conditions, as players know them
const MOD_NAMES = { sable: 'Create Aeronautics', simulated: 'Create Aeronautics', exnihilosequentia: 'Ex Nihilo' };

const walk = dir => readdirSync(dir).flatMap(f => {
  const p = join(dir, f);
  return statSync(p).isDirectory() ? walk(p) : p.endsWith('.json') ? [p] : [];
});

// Every recipe folder: the mod's own, and the Create ones it adds or replaces
const files = [join(DATA, MOD, 'recipe'), join(DATA, 'create', 'recipe'), join(DATA, 'minecraft', 'recipe')]
  .filter(existsSync).flatMap(walk);

/** An ingredient as the site wants it: { item, tag?, count, amount?, fluid?, label? } */
function ingredient(j, count = 1) {
  if (!j) return null;
  if (Array.isArray(j)) return ingredient(j[0], count);
  if (j.fluid) return { fluid: j.fluid, amount: j.amount ?? 1000, count: 1 };
  if (j.tag) return { item: TAG_ITEMS[j.tag] ?? null, tag: j.tag, count };
  if (j.item) return { item: j.item, count };
  if (j.type === 'neoforge:components') return { item: j.items, count, dyed: true };
  if (j.type === 'neoforge:compound') return { ...ingredient(j.children[0], count), dyed: true };
  return { item: null, label: JSON.stringify(j), count };
}

function result(j) {
  if (!j) return null;
  const out = { item: j.id ?? j.item, count: j.count ?? 1 };
  if (j.chance != null) out.chance = j.chance;
  if (j.components?.[`${MOD}:line_dye`]) out.dyed = true;
  return out;
}

/** Merges repeated ingredients (a mixer recipe lists 20 gold nuggets as 20 entries) */
function merge(list) {
  const out = [];
  for (const i of list.filter(Boolean)) {
    const same = out.find(o => o.item === i.item && o.tag === i.tag && o.fluid === i.fluid && !!o.dyed === !!i.dyed);
    if (same) same.count += i.count; else out.push({ ...i });
  }
  return out;
}

function grid(pattern, key) {
  return pattern.map(row => [...row].map(ch => ch === ' ' ? null : ingredient(key[ch])));
}

/** { requiresMods, unlessMods }: mods a recipe needs, and mods whose presence switches it off */
function conditions(j) {
  const needs = new Set(), unless = new Set();
  const visit = (c, negated) => {
    if (!c) return;
    if (c.type === 'neoforge:mod_loaded' && MOD_NAMES[c.modid]) (negated ? unless : needs).add(MOD_NAMES[c.modid]);
    if (c.type === 'neoforge:not') visit(c.value, !negated);
    (c.values ?? []).forEach(v => visit(v, negated));
  };
  (j['neoforge:conditions'] ?? []).forEach(c => visit(c, false));
  return { ...(needs.size ? { requiresMods: [...needs] } : {}), ...(unless.size ? { unlessMods: [...unless] } : {}) };
}

/** A stonecutting recipe that takes any of a group (Electric Lights swapping style): left off the site. */
const SWAP = Symbol('swap');

function convert(j) {
  const t = j.type;
  const base = { type: t.split(':')[1], machine: t.split(':')[0] };
  switch (t) {
    case 'minecraft:crafting_shaped':
    case 'create:mechanical_crafting':
      return { ...base, grid: grid(j.pattern, j.key), outputs: [result(j.result)] };
    case 'minecraft:crafting_shapeless':
      return { ...base, inputs: merge(j.ingredients.map(i => ingredient(i))), outputs: [result(j.result)] };
    case 'minecraft:stonecutting':
      // a light swapped for another of its group takes any of them: the site shows the cut from the part, not every swap
      if (Array.isArray(j.ingredient) && j.ingredient.length > 1) return SWAP;
      return { ...base, inputs: [ingredient(j.ingredient)], outputs: [result(j.result)] };
    case 'minecraft:smithing_transform':
      return { ...base, inputs: [ingredient(j.template), ingredient(j.base), ingredient(j.addition)], outputs: [result(j.result)] };
    case 'create:sequenced_assembly':
      return {
        ...base, inputs: [ingredient(j.ingredient)], outputs: j.results.map(result), loops: j.loops ?? 1,
        transitional: j.transitional_item?.id,
        sequence: j.sequence.map(s => ({
          type: s.type.split(':')[1],
          // the first ingredient of each step is the item on the belt; the rest is what's added
          inputs: merge(s.ingredients.slice(1).map(i => ingredient(i))),
        })),
      };
    case 'create:mixing':
    case 'create:deploying':
    case 'create:filling':
    case 'create:splashing':
    case 'create:crushing':
    case 'create:milling':
      return {
        ...base, inputs: merge(j.ingredients.map(i => ingredient(i))), outputs: j.results.map(result),
        ...(j.heat_requirement ? { heat: j.heat_requirement } : {}),
        ...(j.processing_time ? { time: j.processing_time } : {}),
      };
    case `${MOD}:pressure_crucible`:
      return { ...base, inputs: [ingredient(j.ingredient, j.count ?? 1)], outputs: [result(j.result)],
        heat: j.heat_requirement ?? 'none', time: j.processing_time };
    case `${MOD}:infusing`:
    case `${MOD}:tangling`:
      return { ...base, inputs: [ingredient(j.ingredient, j.count ?? 1), ingredient(j.extra, j.extra_count ?? 1)],
        outputs: [result(j.result)], time: j.processing_time };
    default:
      return null;
  }
}

const recipes = [];
const skipped = [];
let swaps = 0;
for (const file of files) {
  const j = JSON.parse(readFileSync(file, 'utf8'));
  const rel = relative(DATA, file).split(sep);           // [namespace, 'recipe', ...path]
  const id = `${rel[0]}:${rel.slice(2).join('/').replace(/\.json$/, '')}`;
  const r = convert(j);
  if (r === SWAP) { swaps++; continue; }
  if (!r) { skipped.push(`${id} (${j.type})`); continue; }
  recipes.push({ id, ...r, ...conditions(j) });
}

// The 16 dye recipes for one thing (dyed/<colour>_<thing>) become one recipe: "any dye"
const COLOURS = 'white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black';
const DYED = new RegExp(`^${MOD}:dyed/(?:${COLOURS})_(.+)$`);
const isDye = c => /_dye$/.test(c?.item ?? '');
const anyDye = { item: TAG_ITEMS['c:dyes'], tag: 'c:dyes', count: 1 };
const dyed = new Set();
const final = [];
for (const r of recipes) {
  const m = r.id.match(DYED);
  if (!m) { final.push(r); continue; }
  if (dyed.has(m[1])) continue;
  dyed.add(m[1]);
  if (r.grid) r.grid = r.grid.map(row => row.map(c => isDye(c) ? anyDye : c));
  if (r.inputs) r.inputs = r.inputs.map(c => isDye(c) ? anyDye : c);
  r.id = `${MOD}:dyed/${m[1]}`;
  r.note = 'Any of the 16 dyes: it takes that colour.';
  final.push(r);
}
for (const r of final) if (r.type === 'splashing' && r.inputs.some(i => i.dyed)) r.note = 'Washes the dye out of a dyed line.';

// Things that aren't released yet (site.json "unreleased") stay off the site, with every recipe that makes or uses them
const site = JSON.parse(readFileSync(join(ROOT, 'docs/data/site.json'), 'utf8'));
const unreleased = new Set(site.unreleased ?? []);
const mentions = r => [...(r.grid?.flat() ?? []), ...(r.inputs ?? []), ...r.outputs, ...(r.sequence ?? []).flatMap(s => s.inputs)]
  .some(i => i && unreleased.has(i.item));
const hidden = final.filter(mentions).length;
for (let i = final.length - 1; i >= 0; i--) if (mentions(final[i])) final.splice(i, 1);
if (hidden) console.log(`${hidden} recipes left out: they make or use something in site.json "unreleased"`);

final.sort((a, b) => a.id.localeCompare(b.id));
writeFileSync(OUT, JSON.stringify(final, null, 1) + '\n');

// Names for every item a recipe mentions: the mod's from its lang file, the rest from names.json
// (written by the render run, which asks the game) or, failing that, made from the id.
const lang = JSON.parse(readFileSync(join(ROOT, `src/main/resources/assets/${MOD}/lang/en_us.json`), 'utf8'));
const names = existsSync(NAMES) ? JSON.parse(readFileSync(NAMES, 'utf8')) : {};
const items = new Set();
const note = i => i?.item && items.add(i.item);
for (const r of final) {
  r.grid?.flat().forEach(note); r.inputs?.forEach(note); r.outputs.forEach(note);
  r.sequence?.forEach(s => s.inputs.forEach(note));
}
let added = 0;
for (const id of items) {
  const [ns, path] = id.split(':');
  const fromLang = lang[`item.${ns}.${path}`] ?? lang[`block.${ns}.${path}`];
  if (fromLang && ns === MOD) { names[id] = fromLang; continue; }
  if (!names[id]) { names[id] = path.split('_').map(w => w[0].toUpperCase() + w.slice(1)).join(' '); added++; }
}
writeFileSync(NAMES, JSON.stringify(Object.fromEntries(Object.entries(names).sort()), null, 1) + '\n');

console.log(`${final.length} recipes (${recipes.length - final.length} dye recipes folded), ${items.size} items, ${added} names guessed from ids`);
if (swaps) console.log(`${swaps} stonecutter swaps between lights of one group left out`);
if (skipped.length) console.log('Skipped (unknown type):\n  ' + skipped.join('\n  '));
