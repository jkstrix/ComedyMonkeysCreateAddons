// Writes the approved Monster Cage models into the mod: the five block models, the parts the renderer draws, the jars'
// item models and every texture. Usage: node design/models/monster-cage.export.js (regenerates the preview too).
// The renderer's parts (MonsterCageRenderer): the Flooded hatch, water and side water column; the Netherite reflector,
// lens (dark and lit), coil tips (dark and lit) and the shield's six faces (animated, translucent, full-bright).
const fs = require('fs');
const path = require('path');
const { ASSETS, C, fitUv, DIRS } = require('./process-box.lib.js');
const MC = require('./monster-cage.gen.js');

const NS = 'comedymonkeysaddons';
const MODELS = path.join(ASSETS, 'models');
const BLOCK_TEX = path.join(ASSETS, 'textures/block');
const ITEM_TEX = path.join(ASSETS, 'textures/item');

const PREFIX = { 'cage:': 'monster_cage_', 'brass:': 'brass_monster_cage_', 'neth:': 'netherite_monster_cage_' };
function ref(key) {
  for (const [p, file] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${file}${key.slice(p.length)}`;
  if (key.startsWith('jar:')) return `${NS}:item/jar_${key.slice(4)}`;
  if (key.startsWith('item:')) return `${NS}:item/${key.slice(5)}`;
  return key;
}

// A part list -> Java block model JSON (the family's: no AO; cutout unless said otherwise)
function blockModel(cubes, particle, renderType = 'minecraft:cutout', parent = 'block/block') {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      faces[dir] = { uv, texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const el = { name: c.name, from: c.from, to: c.to };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: c.rot[2], axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  const model = { parent, render_type: renderType, ambientocclusion: false, textures, elements };
  if (!parent) delete model.parent;
  return model;
}
const writeJson = (rel, obj) => { fs.writeFileSync(path.join(MODELS, rel), JSON.stringify(obj, null, 2) + '\n'); console.log('wrote models/' + rel); };
const writePng = (dir, name, buf) => { fs.writeFileSync(path.join(dir, name + '.png'), buf); };

// ---------- the five cages ----------
const CAGES = [
  ['monster_cage', 'cage', 'dry'], ['brass_monster_cage', 'brass', 'dry'],
  ['flooded_monster_cage', 'cage', 'flooded'], ['brass_flooded_monster_cage', 'brass', 'flooded'],
  ['netherite_monster_cage', 'neth', 'neth'],
];
for (const [name, tier, kind] of CAGES) {
  writeJson(`block/${name}.json`, blockModel(MC.cageModel(tier, kind), kind === 'neth' ? 'neth:side' : `${tier}:side`));
  writeJson(`item/${name}.json`, { parent: `${NS}:block/${name}` });
  fs.writeFileSync(path.join(ASSETS, 'blockstates', name + '.json'), JSON.stringify({ variants: Object.fromEntries(
    [['north', 0], ['east', 90], ['south', 180], ['west', 270]].map(([f, y]) => [`facing=${f}`, y ? { model: `${NS}:block/${name}`, y } : { model: `${NS}:block/${name}` }])) }, null, 2) + '\n');
}

// ---------- the renderer's parts ----------
writeJson('block/monster_cage_hatch.json', blockModel(MC.hatch(), 'cage:hatch'));
// water: a 1 px tall unit the renderer lifts to the floor and stretches to the level; the side column likewise
const G = MC.GAUGE;
writeJson('block/flooded_monster_cage_water.json', blockModel([
  C('water', [2.05, 0, MC.GLASS_Z + 0.25], [13.95, 1, 13.75], { north: 'cage:water', east: 'cage:water', west: 'cage:water', up: 'cage:water_top', all: null },
    { uv: { north: [0, 0, 16, 16], east: [0, 0, 16, 16], west: [0, 0, 16, 16], up: [0, 0, 16, 16] }, shade: false }),
], 'cage:water', 'minecraft:translucent'));
writeJson('block/flooded_monster_cage_water_column.json', blockModel([
  C('wcol_west', [0.25, 0, G.z0 + 0.05], [0.25, 1, G.z1 - 0.05], { west: 'cage:water_col', all: null }, { uv: { west: [G.z0, 16 - G.y1, G.z1, 16 - G.y0] }, shade: false }),
  C('wcol_east', [15.75, 0, G.z0 + 0.05], [15.75, 1, G.z1 - 0.05], { east: 'cage:water_col', all: null }, { uv: { east: [16 - G.z1, 16 - G.y1, 16 - G.z0, 16 - G.y0] }, shade: false }),
], 'cage:water_col'));
writeJson('block/netherite_monster_cage_reflector.json', blockModel(MC.reflector(), 'neth:reflector'));
writeJson('block/netherite_monster_cage_lens.json', blockModel(MC.beacon(), 'neth:lens'));
writeJson('block/netherite_monster_cage_lens_lit.json', blockModel(MC.beaconLit(), 'neth:lens_lit'));
writeJson('block/netherite_monster_cage_tips_lit.json', blockModel(MC.tips(true), 'neth:tip_lit'));
writeJson('block/netherite_monster_cage_tips_dark.json', blockModel(MC.tips(false), 'neth:tip_dark'));
// the shield: one model per face, so the renderer can leave out a face against a solid block
for (const dir of DIRS)
  writeJson(`block/netherite_monster_cage_shield_${dir}.json`, blockModel([
    C(`shield_${dir}`, [-1, -1, -1], [17, 17, 17], { [dir]: 'neth:shield', all: null }, { uv: { [dir]: [0, 0, 16, 16] }, shade: false }),
  ], 'neth:shield', 'minecraft:translucent'));

// ---------- the jars: their 3D models (from the Capture Bottle's) and builtin item models drawn by MonsterJarRenderer ----------
const bottleItem = JSON.parse(fs.readFileSync(path.join(MODELS, 'item/capture_bottle.json'), 'utf8'));
const bottled = JSON.parse(fs.readFileSync(path.join(MODELS, 'item/bottled_animal.json'), 'utf8'));
for (const [name, metal, rim] of [['iron_jar', 'iron', null], ['netherite_jar', 'netherite', 'jar:blue_rim']]) {
  const model = blockModel(MC.jar(metal, rim), 'comedymonkeysaddons:item/capture_bottle_glass', 'minecraft:translucent', 'block/block');
  model.gui_light = 'side';
  model.display = bottleItem.display;
  writeJson(`item/${name}_model.json`, model);
  writeJson(`item/${name}.json`, { parent: 'minecraft:builtin/entity', gui_light: 'side',
    textures: { particle: 'comedymonkeysaddons:item/capture_bottle_glass' }, display: bottled.display });
}
writeJson('item/monster_chow.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/monster_chow` } });

// ---------- textures ----------
const PREVIEW_ONLY = new Set(['zombie', 'drowned', 'cloth', 'creeper', 'wither', 'eyes', 'chow']);
for (const [set, prefix] of [[MC.CAGE, 'monster_cage_'], [MC.BRASS, 'brass_monster_cage_'], [MC.NETH, 'netherite_monster_cage_']])
  for (const [key, buf] of Object.entries(set.OWN)) if (!PREVIEW_ONLY.has(key)) writePng(BLOCK_TEX, prefix + key, buf);
// the shield is an animated strip (12 frames, 2 ticks each)
fs.writeFileSync(path.join(BLOCK_TEX, 'netherite_monster_cage_shield.png.mcmeta'), JSON.stringify({ animation: { frametime: 2 } }, null, 2) + '\n');
for (const [key, buf] of Object.entries(MC.JARS.OWN)) writePng(ITEM_TEX, 'jar_' + key, buf);
writePng(ITEM_TEX, 'monster_chow', MC.ITEMS.OWN.monster_chow);
console.log('textures written');
