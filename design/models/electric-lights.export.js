// Writes the approved Electric Lights into the mod (design/briefs/electric-lights-*.md, approved 2026-10-28): for each
// light, its body (baked into the world), its unlit glass (baked in only while it's dark, light=0) and its glow (drawn
// by LightRenderer, full-bright, tinted by dye, brightness, flicker and warm-up), the multipart blockstate that turns
// them to face their way, and an item model with the glass lit. Only the light textures the models use are written.
// Usage: node design/models/electric-lights.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, fitGui, hash } = require('./process-box.lib.js');
const G = require('./electric-lights.gen.js');

const NS = 'comedymonkeysaddons';
const r3 = (n) => Math.round(n * 1000) / 1000;
const used = new Set();
const ref = (key) => (key.includes(':') ? key : `${NS}:block/light/${key}`);

// the glow the renderer tints: the lit lens's pattern in greys (white core, a shade deeper toward the face's edges)
G.T.paint('glow_white', (x, y) => {
  const e = Math.min(x, y, 15 - x, 15 - y);
  return e < 2 || hash(x, y, 13) > 0.9 ? '#e3e0dc' : '#ffffff';
});

// Every light that is one plain block (LightKind), with its parts as built: wall and face lights on the north wall
// facing south, ceiling lights hanging from the top of the block, the rest standing on its floor; leaning heads point north
const KINDS = {
  bulkhead_light: { mount: 'wall', parts: G.bulkhead, particle: 'steel' },
  wall_sconce: { mount: 'wall', parts: G.sconce, particle: 'brass' },
  gooseneck_lamp: { mount: 'wall', parts: G.gooseneck, particle: 'steel' },
  high_bay_lamp: { mount: 'fixed', parts: G.highBay, particle: 'steel' },
  cage_lamp: { mount: 'fixed', parts: G.cageLamp, particle: 'steel' },
  flush_ceiling_light: { mount: 'fixed', parts: G.flush, particle: 'brass' },
  bollard_light: { mount: 'fixed', parts: G.bollard, particle: 'steel' },
  floor_light: { mount: 'fixed', parts: G.floorLight, particle: 'steel' },
  indicator_lamp: { mount: 'any', parts: G.indicator, particle: 'steel' },
  lantern_head: { mount: 'fixed', parts: () => G.lantern(true), particle: 'iron' },
  cobra_head: { mount: 'leaning', parts: G.cobraTop, particle: 'steel' },
  globe_head: { mount: 'fixed', parts: () => G.globe(0), particle: 'brass' },
  yard_light_head: { mount: 'leaning', parts: G.yardTop, particle: 'steel' },
};

// The blockstate rotation for each facing, as LightBlock.turn and LightRenderer turn shapes and parts
const ROTATIONS = {
  wall: { south: {}, west: { y: 90 }, north: { y: 180 }, east: { y: 270 }, up: {}, down: {} },
  any: { south: {}, west: { y: 90 }, north: { y: 180 }, east: { y: 270 }, up: { x: 90 }, down: { x: 270 } },
  leaning: { north: {}, east: { y: 90 }, south: { y: 180 }, west: { y: 270 }, up: {}, down: {} },
};

function blockModel(cubes, particle, { glow = false, gui = false } = {}) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (!t.includes(':')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: raw.map(r3), texture: v(ref(t)) };
      // the glow is tinted by the renderer: a tinted face takes its colour when drawn without Flywheel (Flywheel's
      // instances multiply every face anyway)
      if (glow === 'tint' && c.shade === false) faces[dir].tintindex = 0;
    }
    const el = { name: c.name, from: c.from.map(r3), to: c.to.map(r3) };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: c.rot[2].map(r3), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    if (glow && c.shade === false) el.neoforge_data = { block_light: 15, sky_light: 15 };
    el.faces = faces;
    for (const p of [...el.from, ...el.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  const model = { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
  if (gui) model.display = { gui: fitGui(elements) };
  return model;
}
function writeJson(rel, obj) {
  const file = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
}
// a light's glow parts with another texture (glow_white for the renderer to tint, glass_off for unlit glass)
const lens = (cubes, tex) => G.reglow(cubes, tex).map((c) => (tex === 'glass_off' ? c : { ...c, shade: false }));

for (const [id, k] of Object.entries(KINDS)) {
  const all = k.parts();
  const body = all.filter((c) => !G.isGlow(c)), glass = all.filter(G.isGlow);
  if (!glass.length) throw new Error(id + ' has no glow parts');
  writeJson(`models/block/light/${id}.json`, blockModel(body, k.particle));
  writeJson(`models/block/light/${id}_glass.json`, blockModel(lens(glass, 'glass_off'), k.particle));
  writeJson(`models/block/light/${id}_glow.json`, blockModel(lens(glass, 'glow_white'), 'glow_white', { glow: 'tint' }));
  // the item: lit, in the house's warm glow
  writeJson(`models/item/${id}.json`, blockModel([...body, ...glass], k.particle, { glow: true, gui: true }));
  // multipart: the body always, the unlit glass only while it's dark; both turned to face its way
  const model = (part) => `${NS}:block/light/${id}${part}`;
  const multipart = [];
  if (k.mount === 'fixed') {
    multipart.push({ apply: { model: model('') } }, { when: { light: '0' }, apply: { model: model('_glass') } });
  } else {
    for (const [facing, rot] of Object.entries(ROTATIONS[k.mount])) {
      multipart.push({ when: { facing }, apply: { model: model(''), ...rot } });
      multipart.push({ when: { facing, light: '0' }, apply: { model: model('_glass'), ...rot } });
    }
  }
  writeJson(`blockstates/${id}.json`, { multipart });
}

// ---------- the Light Switch: a fitting on a Power Line (drawn by PowerLineRenderer), and its item, upright on a wall ----------
{
  const flat = G.lightSwitchFloor(), wall = G.lightSwitch();
  writeJson('models/block/line_fitting/light_switch.json', blockModel(flat.plate, 'steel'));
  writeJson('models/block/line_fitting/light_switch_toggle.json', blockModel(flat.toggle, 'steel'));
  writeJson('models/item/light_switch.json', blockModel([...wall.plate, ...wall.toggle], 'steel', { gui: true }));
}

// ---------- the Light Strip: the parts StripModel builds each strip block from, its item, its blockstate ----------
{
  const parts = G.stripParts();
  for (const [name, p] of Object.entries(parts)) {
    writeJson(`models/block/light_strip/${name}.json`, blockModel(p.body, 'alu'));
    if (!p.diffuser.length) continue;
    writeJson(`models/block/light_strip/${name}_glow.json`, blockModel(lens(p.diffuser, 'glow_white'), 'glow_white', { glow: 'tint' }));
    writeJson(`models/block/light_strip/${name}_glass.json`, blockModel(lens(p.diffuser, 'glass_off'), 'alu'));
  }
  // the block's own model is only its particle: StripModel draws the strips from the parts
  writeJson('models/block/light_strip/base.json', { parent: 'block/block', render_type: 'minecraft:cutout', textures: { particle: ref('alu') }, elements: [] });
  writeJson('blockstates/light_strip.json', { variants: { '': { model: `${NS}:block/light_strip/base` } } });
  writeJson('models/item/light_strip.json', blockModel(G.stripFloor(0, 16, true, true), 'alu', { glow: true, gui: true }));
}

// ---------- the Edge Light: the parts EdgeModel builds each edge-light block from, its item, its blockstate ----------
{
  for (const [name, p] of Object.entries(G.edgeParts())) {
    writeJson(`models/block/edge_light/${name}.json`, blockModel(p.body, 'alu'));
    writeJson(`models/block/edge_light/${name}_glow.json`, blockModel(lens(p.diffuser, 'glow_white'), 'glow_white', { glow: 'tint' }));
    writeJson(`models/block/edge_light/${name}_glass.json`, blockModel(lens(p.diffuser, 'glass_off'), 'alu'));
  }
  writeJson('models/block/edge_light/base.json', { parent: 'block/block', render_type: 'minecraft:cutout', textures: { particle: ref('alu') }, elements: [] });
  writeJson('blockstates/edge_light.json', { variants: { '': { model: `${NS}:block/edge_light/base` } } });
  const e = G.edgeParts();
  writeJson('models/item/edge_light.json', blockModel([...e.cap_w.body, ...e.cap_w.diffuser, ...e.mid.body, ...e.mid.diffuser, ...e.cap_e.body, ...e.cap_e.diffuser]
    .map((c) => ({ ...c, from: [c.from[0], c.from[1], c.from[2] + 6.75], to: [c.to[0], c.to[1], c.to[2] + 6.75] })), 'alu', { glow: true, gui: true }));
}

// ---------- the colour each light casts, for Colorful Lighting (it reads emitters by block state) ----------
// As LightColours.castHex (checked by LightColoursTest): the lens colour at full; a dyed one strengthened so it reads over
// vanilla's white light, black as a deep ultraviolet, green from a cooler green than its olive lens, undyed the warm lens as it is. Dark lights emit nothing anyway.
const WARM = 0xd99a4a, BLACK_LIGHT = 0x5b3aa8, GREEN_LIGHT = 0x5e9e6a, SATURATE = 4.0;
const DYES = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
  'blue', 'brown', 'green', 'red', 'black'];
const LENS = [0xd6d3c8, 0xd38a4c, 0xb979ad, 0x7fa9bf, 0xd2b964, 0x9cba66, 0xcf97a5, 0x8c8f91, 0xb0afa8, 0x5fa6a6, 0x9270b4,
  0x6e7cbd, 0xa7845f, 0x86a058, 0xc0665a, 0x5c5c62];
function castHex(dye) {
  const f = Math.fround;
  const c = dye === 'black' ? BLACK_LIGHT : dye === 'green' ? GREEN_LIGHT : dye ? LENS[DYES.indexOf(dye)] : WARM;
  const saturate = dye ? SATURATE : 1;
  let rgb = [c >> 16 & 255, c >> 8 & 255, c & 255].map((v) => f(v / 255));
  const l = f(f(f(f(0.2126) * rgb[0]) + f(f(0.7152) * rgb[1])) + f(f(0.0722) * rgb[2]));
  rgb = rgb.map((v) => Math.max(0, f(l + f(f(v - l) * f(saturate)))));
  const max = Math.max(...rgb);
  rgb = rgb.map((v) => f(v / max));
  return '#' + rgb.map((v) => Math.floor(f(v * 255) + 0.5).toString(16).toUpperCase().padStart(2, '0')).join('');
}
const emitters = {};
for (const id of [...Object.keys(KINDS), 'light_strip', 'edge_light']) {
  emitters[`${NS}:${id}`] = castHex(null);
  for (const dye of DYES) emitters[`${NS}:${id}[color=${dye}]`] = castHex(dye);
}
writeJson('light/emitters.json', emitters);

// ---------- the Light Assembly's item: a bulb in a brass cap on a copper plate (hand-drawn) ----------
const ASSEMBLY = [
  '................',
  '......gggg......',
  '....ggGGgggg....',
  '...gGGgffgggg...',
  '...gGgfwwffgg...',
  '..gggfwffwffgg..',
  '..ggffwffwffgg..',
  '..ggfffwwfffgg..',
  '...ggffrrffgg...',
  '....ggfrrfgg....',
  '.....bbbbbb.....',
  '.....bkbbkb.....',
  '...CccccccccC...',
  '..CccccccccccC..',
  '..CCCCCCCCCCCC..',
  '................',
];
const AP = { g: '#8fa4abd0', G: '#c3d0d4e0', f: '#a8bcc260', w: '#d99a4a', r: '#8a2f22', b: '#a3873f', k: '#3c424a', c: '#b26247', C: '#904931' };
G.T.paint('light_assembly', (x, y) => AP[ASSEMBLY[y][x]] || null);
fs.mkdirSync(path.join(ASSETS, 'textures/item'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures/item/light_assembly.png'), G.T.OWN.light_assembly);
writeJson('models/item/light_assembly.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/light_assembly` } });

// ---------- data: loot (always plain: D5), pickaxe, recipes ----------
const DATA = path.resolve(ASSETS, '../../data');
const writeData = (rel, obj) => {
  const file = path.join(DATA, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
};
const ids = Object.keys(KINDS);
for (const id of ids)
  writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1,
    entries: [{ type: 'minecraft:item', name: `${NS}:${id}` }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
const pick = path.join(DATA, 'minecraft/tags/block/mineable/pickaxe.json');
const pickTag = JSON.parse(fs.readFileSync(pick, 'utf8'));
for (const id of ids) if (!pickTag.values.includes(`${NS}:${id}`)) pickTag.values.push(`${NS}:${id}`);
fs.writeFileSync(pick, JSON.stringify(pickTag, null, 2) + '\n');
// a block of Light Strip drops a strip for each face it has one on (as Power Lines drop a wire a face)
writeData(`${NS}/loot_table/blocks/light_strip.json`, { type: 'minecraft:block',
  pools: ['down', 'up', 'north', 'south', 'west', 'east'].map((face) => ({ rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:light_strip` }],
    conditions: [{ condition: 'minecraft:block_state_property', block: `${NS}:light_strip`, properties: { [face]: 'true' } },
      { condition: 'minecraft:survives_explosion' }] })) });
// an edge-light block drops one for each edge it has (EdgeLightBlock.getDrops counts them; the table gives the one)
writeData(`${NS}/loot_table/blocks/edge_light.json`, { type: 'minecraft:block', pools: [{ rolls: 1,
  entries: [{ type: 'minecraft:item', name: `${NS}:edge_light` }], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });

// The Light Assembly (P21), then each light from it in a stonecutter (P22): one of a full light, eight of a small one.
// A light goes back in to become another of its yield group, 1:1 (P23). The bigger lights are crafted instead (D26).
const GROUPS = {
  full: ['bulkhead_light', 'wall_sconce', 'gooseneck_lamp', 'cage_lamp', 'flush_ceiling_light', 'bollard_light', 'floor_light',
    'lantern_head', 'cobra_head', 'globe_head', 'yard_light_head'],
  small: ['indicator_lamp', 'light_strip', 'edge_light'],
};
const YIELD = { full: 1, small: 8 };
const CRAFTED = { high_bay_lamp: [{ tag: 'c:plates/iron' }] };
fs.rmSync(path.join(DATA, NS, 'recipe/light'), { recursive: true, force: true });
writeData(`${NS}/recipe/light_assembly.json`, { type: 'minecraft:crafting_shapeless', category: 'misc',
  ingredients: [{ item: 'minecraft:glass' }, { tag: 'c:plates/copper' }, { item: 'minecraft:redstone' }],
  result: { id: `${NS}:light_assembly`, count: 2 } });
for (const [group, members] of Object.entries(GROUPS)) {
  writeData(`${NS}/tags/item/lights/${group}.json`, { values: members.map((m) => `${NS}:${m}`) });
  for (const m of members) {
    writeData(`${NS}/recipe/light/${m}.json`, { type: 'minecraft:stonecutting',
      ingredient: { item: `${NS}:light_assembly` }, result: { id: `${NS}:${m}`, count: YIELD[group] } });
    // a swap: from any other light of the group (not itself, or the stonecutter would offer it back)
    if (members.length > 1)
      writeData(`${NS}/recipe/light/${m}_from_${group}_light.json`, { type: 'minecraft:stonecutting',
        ingredient: members.filter((o) => o !== m).map((o) => ({ item: `${NS}:${o}` })), result: { id: `${NS}:${m}`, count: 1 } });
  }
}
for (const [m, parts] of Object.entries(CRAFTED))
  writeData(`${NS}/recipe/light/${m}.json`, { type: 'minecraft:crafting_shapeless', category: 'misc',
    ingredients: [{ item: `${NS}:light_assembly` }, ...parts], result: { id: `${NS}:${m}`, count: 1 } });
// the Light Switch is a control, not a light: lever + copper sheet -> 2 (P25)
writeData(`${NS}/recipe/light_switch.json`, { type: 'minecraft:crafting_shapeless', category: 'redstone',
  ingredients: [{ item: 'minecraft:lever' }, { tag: 'c:plates/copper' }], result: { id: `${NS}:light_switch`, count: 2 } });
for (const id of ids) if (!GROUPS.full.includes(id) && !GROUPS.small.includes(id) && !CRAFTED[id]) throw new Error(id + ' has no recipe');

// textures: only those the models use
const texDir = path.join(ASSETS, 'textures/block/light');
fs.mkdirSync(texDir, { recursive: true });
for (const f of fs.readdirSync(texDir)) if (f.endsWith('.png')) fs.unlinkSync(path.join(texDir, f));
for (const key of used) {
  if (!G.T.OWN[key]) throw new Error('no texture ' + key);
  fs.writeFileSync(path.join(texDir, key + '.png'), G.T.OWN[key]);
}
console.log('wrote', Object.keys(KINDS).length, 'lights,', used.size, 'textures:', [...used].sort().join(' '));
