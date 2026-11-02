// Writes the Clockwork Wastes' overgrowth into the mod (D91, D92, D94; design/briefs/clockwork-wastes-overgrowth.md; the
// look approved in clockwork-wastes-overgrowth.bbmodel, painted and modelled by clockwork-wastes-overgrowth.gen.js):
//   - block models (models/block/wastes/overgrowth/), blockstates, item models, the textures the models use
//     (textures/block/wastes/overgrowth/, the item sprites in textures/item/), the potted plants
//   - loot tables (thin plants: shears or Silk Touch; the rest drop themselves, D92), the Vigil Bell's blue dye
//   - the Charging Alcove's living vines (D92: its vines none / dead / living): their models and the blockstate's parts
//     (run after clockwork-wastes.export.js, which writes the dead ones and keeps these)
//   - the Shiftcaps' light for Colorful Lighting by age (light/emitters.json, shared through emitters.lib.js): blue while
//     young, plum where the plum glow leads (mature and old caps, old shelves); the potted one's lit lips full-bright
// Every block model is checked as the other exporters check theirs (UVs on the texture, no zero-width UV, bounds -16..32,
// one rotation of 0/±22.5/±45 per element, every face textured) and z-fight checked (different textures closer than 0.2 px).
// The shared files (lang, mining and other tags, Carry On's list) are hand-edited; this prints what they need.
// Usage: node design/models/clockwork-wastes-overgrowth.export.js
// Run third, after clockwork-wastes.export.js and the props (export-wastes-all.js runs every Wastes exporter in order).
const fs = require('fs');
const path = require('path');
const { ASSETS, REPO, fitGui } = require('./process-box.lib.js');
const { check } = require('./zfight-check.js');
const G = require('./clockwork-wastes-overgrowth.gen.js');
const { writeEmitters } = require('./emitters.lib.js');

const NS = 'comedymonkeysaddons';
const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
const r3 = (n) => Math.round(n * 1000) / 1000;
const problems = [];
const usedTex = new Set();
const writeJson = (rel, obj) => { const f = path.join(ASSETS, rel); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n'); };
const writeData = (rel, obj) => { const f = path.join(REPO, 'src/main/resources/data', rel); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n'); };
const texRef = (name) => (name === null ? `${NS}:block/wastes/scorched` : `${NS}:block/wastes/overgrowth/${name}`);
const model = (name) => `${NS}:block/wastes/overgrowth/${name}`;

// ---------- a block model from the generator's elements ----------
function blockModel(id, elements, particle, { renderType = 'minecraft:cutout', gui = false } = {}) {
  const vars = new Map();
  const v = (ref) => {
    const name = G.texName(ref);
    if (name === undefined) problems.push(`${id}: a face's texture isn't one of the generator's (${ref})`);
    if (name) usedTex.add(name);
    const key = name === null ? 'scorched' : name;
    vars.set(key, texRef(name));
    return '#' + key;
  };
  const out = [];
  for (const e of elements) {
    const faces = {};
    for (const d of DIRS) {
      const f = e.faces[d];
      if (!f) continue;
      if (f.uv.some((n) => n < -0.001 || n > 16.001)) problems.push(`${id}:${e.name} ${d}: UV ${f.uv} is off the texture`);
      if (Math.abs(f.uv[0] - f.uv[2]) < 1e-6 || Math.abs(f.uv[1] - f.uv[3]) < 1e-6) problems.push(`${id}:${e.name} ${d}: zero-width UV ${f.uv}`);
      faces[d] = { uv: f.uv.map(r3), texture: v(f.texture), ...(f.rotation ? { rotation: f.rotation } : {}) };
    }
    if (!Object.keys(faces).length) problems.push(`${id}:${e.name} has no faces`);
    const el = { name: e.name, from: e.from.map(r3), to: e.to.map(r3) };
    for (const p of [...el.from, ...el.to]) if (p < -16 || p > 32) problems.push(`${id}:${e.name} is outside the model bounds`);
    if (e.rotation) {
      if (![-45, -22.5, 22.5, 45].includes(e.rotation.angle)) problems.push(`${id}:${e.name}: angle ${e.rotation.angle}`);
      // a card turned 45 degrees is scaled back to the cell's width, as vanilla's cross is
      const card = e.rotation.axis === 'y' && Math.abs(e.rotation.angle) === 45 && (e.from[0] === e.to[0] || e.from[2] === e.to[2]);
      el.rotation = { origin: e.rotation.origin.map(r3), axis: e.rotation.axis, angle: e.rotation.angle, ...(card ? { rescale: true } : {}) };
    }
    if (!e.shade) { el.shade = false; el.neoforge_data = { block_light: 15, sky_light: 15 }; }
    el.faces = faces;
    out.push(el);
  }
  const textures = { particle: texRef(particle) };
  usedTex.add(particle);
  for (const [k, ref] of vars) textures[k] = ref;
  const json = { parent: 'block/block', render_type: renderType, ambientocclusion: false, textures, elements: out };
  if (gui) json.display = { gui: fitGui(out) };
  return json;
}
// only fights between different textures show
function zcheck(id, elements) {
  const byTex = {};
  for (const e of elements) for (const d of DIRS) {
    const f = e.faces[d];
    if (!f) continue;
    (byTex[f.texture] = byTex[f.texture] || []).push({ name: e.name, from: e.from, to: e.to, rot: e.rotation ? [e.rotation.axis, e.rotation.angle] : null, faces: { [d]: 1 } });
  }
  const keys = Object.keys(byTex), parts = Object.fromEntries(keys.map((k, i) => ['t' + i, byTex[k]]));
  for (const p of check(parts)) {
    const m = p.match(/^t(\d+):(\S+) (\w+) vs t(\d+):(.*)$/);
    if (m && m[1] !== m[4]) problems.push(`z-fight ${id}: ${p}`);
  }
}
function writeModel(name, particle, o) {
  const els = G.MODELS[name];
  if (!els) throw new Error('no model ' + name);
  zcheck(name, els);
  writeJson(`models/block/wastes/overgrowth/${name}.json`, blockModel(name, els, particle, o));
}

// ---------- the blocks ----------
const LEAVES = { renderType: 'minecraft:cutout_mipped' };
const SOLID = { renderType: 'minecraft:solid' };
for (const [name, particle, o] of [
  ['knotvine_a', 'knotvine_a'], ['knotvine_b', 'knotvine_b'], ['knotvine_c', 'knotvine_c'], ['knotvine_floor', 'knotvine_floor'],
  ['lichen_a', 'lichen_a'], ['lichen_b', 'lichen_b'], ['lichen_floor', 'lichen_a'],
  ['twinerope', 'rope'], ['twinerope_tip', 'rope'],
  ['dusk_moss', 'moss_top', SOLID], ['dusk_moss_carpet', 'moss_top'], ['dusk_moss_drape', 'moss_top'],
  ['springcurl_young', 'springcurl_young'], ['springcurl_grown', 'springcurl_grown'], ['corkscrew_rush', 'rush'], ['vigil_bell', 'vigil_bell'],
  ['bramblecoil_small', 'bramble_leaf'], ['bramblecoil_large', 'bramble_leaf'],
  ['shiftcap_young', 'cap_top'], ['shiftcap_mature', 'cap_top'], ['shiftcap_old', 'cap_top'],
  ['shiftcap_shelf_young', 'cap_top'], ['shiftcap_shelf_mature', 'cap_top'], ['shiftcap_shelf_old', 'cap_top'],
  ['breakroot_knuckle', 'root_bark', { gui: true }], ['breakroot_run', 'root_bark', { gui: true }], ['breakroot_burst', 'root_bark', { gui: true }],
  ['breakroot_burst_wall', 'root_bark'],
  ['crookwood_log', 'crook_log', SOLID], ['crookwood_leaves', 'crook_leaves', LEAVES], ['crookwood_leaves_hanging', 'crook_hang_end', LEAVES],
  ['crookwood_leaves_hanging_mid', 'crook_hang', LEAVES],
  ['shieldcrown_log', 'shield_log', SOLID], ['shieldcrown_leaves', 'shield_leaves', LEAVES], ['shieldcrown_leaves_rim', 'shield_leaves_rim', LEAVES],
]) writeModel(name, particle, o);

const HORIZONTAL = [['north', 0], ['east', 90], ['south', 180], ['west', 270]];
// a model drawn against the wall to the SOUTH, onto the face toward d
const ATTACHED = { south: 0, west: 90, north: 180, east: 270 };
const rot = (x, y) => ({ ...(x ? { x } : {}), ...(y ? { y } : {}) });
const blockstate = (id, json) => writeJson(`blockstates/${id}.json`, json);
const one = (id, name) => blockstate(id, { variants: { '': { model: model(name) } } });

// the multi-face covers: a face's model by where it's attached, the wall faces picked by weight from their variants
function cover(id, walls, floor) {
  const multipart = [];
  for (const [d, y] of Object.entries(ATTACHED)) multipart.push({ when: { [d]: 'true' }, apply: walls.map((w) => ({ model: model(w), ...rot(0, y) })) });
  multipart.push({ when: { down: 'true' }, apply: { model: model(floor) } });
  multipart.push({ when: { up: 'true' }, apply: { model: model(floor), x: 180 } });
  blockstate(id, { multipart });
}
cover('knotvine', ['knotvine_a', 'knotvine_b', 'knotvine_c'], 'knotvine_floor');
cover('scale_lichen', ['lichen_a', 'lichen_b'], 'lichen_floor');
one('twinerope', 'twinerope_tip');
one('twinerope_plant', 'twinerope');
one('dusk_moss', 'dusk_moss');
blockstate('dusk_moss_carpet', { multipart: [{ apply: { model: model('dusk_moss_carpet') } },
  ...HORIZONTAL.map(([d, y]) => ({ when: { [d]: 'true' }, apply: { model: model('dusk_moss_drape'), ...rot(0, y) } }))] });
blockstate('springcurl', { variants: { 'grown=false': { model: model('springcurl_young') }, 'grown=true': { model: model('springcurl_grown') } } });
one('corkscrew_rush', 'corkscrew_rush');
one('vigil_bell', 'vigil_bell');
blockstate('bramblecoil', { variants: { 'large=false': { model: model('bramblecoil_small') }, 'large=true': { model: model('bramblecoil_large') } } });
blockstate('shiftcap', { variants: Object.fromEntries(['young', 'mature', 'old'].map((a) => [`age=${a}`, { model: model('shiftcap_' + a) }])) });
blockstate('shiftcap_shelf', { variants: Object.fromEntries(HORIZONTAL.flatMap(([f, y]) => ['young', 'mature', 'old'].map((a) => [`age=${a},facing=${f}`, { model: model('shiftcap_shelf_' + a), ...rot(0, y) }]))) });
for (const id of ['breakroot_knuckle', 'breakroot_run'])
  blockstate(id, { variants: { 'axis=x': { model: model(id) }, 'axis=z': { model: model(id), y: 90 } } });
blockstate('breakroot_burst', { variants: { 'facing=up': { model: model('breakroot_burst') },
  ...Object.fromEntries(HORIZONTAL.map(([f, y]) => [`facing=${f}`, { model: model('breakroot_burst_wall'), ...rot(0, y) }])) } });
for (const id of ['crookwood_log', 'shieldcrown_log'])
  blockstate(id, { variants: { 'axis=y': { model: model(id) }, 'axis=z': { model: model(id), x: 90 }, 'axis=x': { model: model(id), x: 90, y: 90 } } });
blockstate('crookwood_leaves', { variants: { 'hanging=none': { model: model('crookwood_leaves') }, 'hanging=mid': { model: model('crookwood_leaves_hanging_mid') },
  'hanging=end': { model: model('crookwood_leaves_hanging') } } });
blockstate('shieldcrown_leaves', { variants: { 'rim=false': { model: model('shieldcrown_leaves') }, 'rim=true': { model: model('shieldcrown_leaves_rim') } } });

// ---------- the potted plants (vanilla's flower pot with a cross of the plant's picture) ----------
const POTTED = { springcurl: 'springcurl_grown', corkscrew_rush: 'rush', vigil_bell: 'vigil_bell', shiftcap: 'potted_shiftcap' };
for (const [plant, tex] of Object.entries(POTTED)) {
  usedTex.add(tex);
  const name = 'potted_' + plant;
  writeJson(`models/block/wastes/overgrowth/${name}.json`, { parent: 'minecraft:block/flower_pot_cross', render_type: 'minecraft:cutout', textures: { plant: texRef(tex) } });
  if (plant !== 'shiftcap') one(name, name);
}
// the potted Shiftcap's lit lips: flower_pot_cross's cross again in the lips alone, unshaded and full-bright (the body's
// texture has them cut out, so the two never overlap)
{
  const tex = 'potted_shiftcap_glow', card = (from, to, dirs) => ({ name: 'lip glow', from, to, shade: false, neoforge_data: { block_light: 15, sky_light: 15 },
    rotation: { origin: [8, 8, 8], axis: 'y', angle: 45, rescale: true }, faces: Object.fromEntries(dirs.map((d) => [d, { uv: [0, 0, 16, 16], texture: '#glow' }])) });
  usedTex.add(tex);
  writeJson('models/block/wastes/overgrowth/potted_shiftcap_glow.json', { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false,
    textures: { particle: texRef(tex), glow: texRef(tex) }, elements: [card([2.6, 4, 8], [13.4, 16, 8], ['north', 'south']), card([8, 4, 2.6], [8, 16, 13.4], ['west', 'east'])] });
  blockstate('potted_shiftcap', { multipart: [{ apply: { model: model('potted_shiftcap') } }, { apply: { model: model('potted_shiftcap_glow') } }] });
}

// ---------- item models ----------
const flat = (id, tex) => { usedTex.add(tex); writeJson(`models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: texRef(tex) } }); };
const asBlock = (id, name) => writeJson(`models/item/${id}.json`, { parent: model(name) });
flat('knotvine', 'knotvine_a'); flat('scale_lichen', 'lichen_a'); flat('twinerope', 'item_twinerope');
asBlock('dusk_moss', 'dusk_moss'); asBlock('dusk_moss_carpet', 'dusk_moss_carpet');
flat('springcurl', 'springcurl_grown'); flat('corkscrew_rush', 'rush'); flat('vigil_bell', 'vigil_bell'); flat('bramblecoil', 'bramble_sprig');
flat('shiftcap', 'item_shiftcap'); flat('shiftcap_shelf', 'item_shelf');
for (const id of ['breakroot_knuckle', 'breakroot_run', 'breakroot_burst', 'crookwood_log', 'shieldcrown_log']) asBlock(id, id);
asBlock('crookwood_leaves', 'crookwood_leaves'); asBlock('shieldcrown_leaves', 'shieldcrown_leaves');

// ---------- textures (only the ones the models and items use) ----------
for (const name of usedTex) {
  if (!G.PNGS[name]) { problems.push('no texture painted for ' + name); continue; }
  const f = path.join(ASSETS, 'textures/block/wastes/overgrowth', name + '.png');
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, G.PNGS[name]);
}

// ---------- loot (D92: thin plants with shears or Silk Touch; the rest drop themselves) ----------
const SHEARS_OR_SILK = { condition: 'minecraft:any_of', terms: [{ condition: 'neoforge:can_item_perform_ability', ability: 'shears_dig' },
  { condition: 'minecraft:match_tool', predicate: { predicates: { 'minecraft:enchantments': [{ enchantments: 'minecraft:silk_touch', levels: { min: 1 } }] } } }] };
const lootFile = (id, pools) => writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools, random_sequence: `${NS}:blocks/${id}` });
const self = (id, item = id, extra = {}) => lootFile(id, [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:${item}`, ...extra }], conditions: [{ condition: 'minecraft:survives_explosion' }] }]);
const thin = (id, item = id) => lootFile(id, [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:${item}`, conditions: [SHEARS_OR_SILK] }] }]);
// a multi-face cover drops one a face, as glow lichen does
const coverLoot = (id) => lootFile(id, [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:${id}`, conditions: [SHEARS_OR_SILK], functions: [
  ...DIRS.map((d) => ({ function: 'minecraft:set_count', add: true, count: 1, conditions: [{ condition: 'minecraft:block_state_property', block: `${NS}:${id}`, properties: { [d]: 'true' } }] })),
  { function: 'minecraft:set_count', add: true, count: -1 }, { function: 'minecraft:explosion_decay' }] }] }]);
coverLoot('knotvine'); coverLoot('scale_lichen');
thin('twinerope'); thin('twinerope_plant', 'twinerope'); thin('dusk_moss_carpet'); thin('springcurl'); thin('corkscrew_rush');
thin('crookwood_leaves'); thin('shieldcrown_leaves');
for (const id of ['dusk_moss', 'vigil_bell', 'bramblecoil', 'shiftcap', 'shiftcap_shelf', 'breakroot_knuckle', 'breakroot_run', 'breakroot_burst', 'crookwood_log', 'shieldcrown_log']) self(id);
for (const plant of Object.keys(POTTED))
  lootFile('potted_' + plant, [{ rolls: 1, entries: [{ type: 'minecraft:item', name: 'minecraft:flower_pot' }], conditions: [{ condition: 'minecraft:survives_explosion' }] },
    { rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:${plant}` }], conditions: [{ condition: 'minecraft:survives_explosion' }] }]);
// the Vigil Bell's blue dye (G8)
writeData(`${NS}/recipe/blue_dye_from_vigil_bell.json`, { type: 'minecraft:crafting_shapeless', category: 'misc', group: 'blue_dye',
  ingredients: [{ item: `${NS}:vigil_bell` }], result: { id: 'minecraft:blue_dye', count: 1 } });

// ---------- the Charging Alcove's living vines (D92): its dead vines' cards in Knotvine ----------
{
  const CP = require('./clockwork-wastes-city-props.gen.js');
  const TEX = { cp_vine_upper: 'knotvine_a', cp_vine_lower: 'knotvine_c', cp_vine_hang: 'item_twinerope' };
  const cards = CP.alcoveVines();
  for (const [k, part] of [[1, 'middle'], [2, 'top']]) {
    const els = [];
    for (const c of cards) {
      if (c.to[1] <= k * 16 || c.from[1] >= (k + 1) * 16) continue;
      const faces = {};
      for (const [d, f] of Object.entries(c.faces)) {
        const t = typeof f === 'string' ? f : f.texture || f;
        const name = TEX[t];
        if (!name) { problems.push('alcove vines: no living texture for ' + t); continue; }
        usedTex.add(name);
        const lo = Math.max(c.from[1], k * 16) - k * 16, hi = Math.min(c.to[1], (k + 1) * 16) - k * 16;
        faces[d] = { uv: (c.uv && c.uv[d]) ? c.uv[d] : [0, 16 - hi, Math.max(1, Math.abs(c.to[0] - c.from[0]) || Math.abs(c.to[2] - c.from[2])), 16 - lo], texture: '#' + name };
      }
      els.push({ name: c.name, from: [c.from[0], Math.max(c.from[1], k * 16) - k * 16, c.from[2]], to: [c.to[0], Math.min(c.to[1], (k + 1) * 16) - k * 16, c.to[2]], faces });
    }
    if (!els.length) problems.push('alcove vines: nothing in the ' + part);
    const textures = { particle: texRef('knotvine_a') };
    for (const e of els) for (const f of Object.values(e.faces)) textures[f.texture.slice(1)] = texRef(f.texture.slice(1));
    writeJson(`models/block/wastes/charging_alcove_vines_living_${part}.json`, { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements: els });
  }
  for (const name of usedTex) if (G.PNGS[name]) fs.writeFileSync(path.join(ASSETS, 'textures/block/wastes/overgrowth', name + '.png'), G.PNGS[name]);
  const file = path.join(ASSETS, 'blockstates/charging_alcove.json'), state = JSON.parse(fs.readFileSync(file, 'utf8'));
  state.multipart = state.multipart.filter((p) => !(p.when && p.when.vines === 'living'));
  for (const p of state.multipart) if (p.when && p.when.vines === 'true') p.when.vines = 'dead';
  for (const part of ['middle', 'top'])
    for (const [facing, y] of HORIZONTAL)
      state.multipart.push({ when: { facing, part, vines: 'living' }, apply: { model: `${NS}:block/wastes/charging_alcove_vines_living_${part}`, ...rot(0, y) } });
  writeJson('blockstates/charging_alcove.json', state);
}

// ---------- the Shiftcaps' light, for Colorful Lighting (merged with the other exporters' emitters) ----------
// The glow's edge tones (the brief's ramps): a cap's lit lip and spots lead its colour, a shelf's gills (its lip is a few dots),
// so a mature cap reads plum and a mature shelf blue; the pot's sprite is mostly blue. The bare id is the young colour.
{
  const BLUE = G.C.gBlue[1].toUpperCase(), PLUM = G.C.gPlum[1].toUpperCase();
  const AGES = { shiftcap: { young: BLUE, mature: PLUM, old: PLUM }, shiftcap_shelf: { young: BLUE, mature: BLUE, old: PLUM } };
  const em = { [`${NS}:potted_shiftcap`]: BLUE };
  for (const [id, ages] of Object.entries(AGES)) {
    em[`${NS}:${id}`] = ages.young;
    for (const [age, c] of Object.entries(ages)) em[`${NS}:${id}[age=${age}]`] = c;
  }
  writeEmitters('clockwork-wastes-overgrowth.export.js', em);
}

// ---------- what the shared files need ----------
const NAMES = { knotvine: 'Knotvine', scale_lichen: 'Scale Lichen', twinerope: 'Twinerope', twinerope_plant: 'Twinerope', dusk_moss: 'Dusk Moss',
  dusk_moss_carpet: 'Dusk Moss Carpet', springcurl: 'Springcurl', corkscrew_rush: 'Corkscrew Rush', vigil_bell: 'Vigil Bell', bramblecoil: 'Bramblecoil',
  shiftcap: 'Shiftcap', shiftcap_shelf: 'Shiftcap Shelf', breakroot_knuckle: 'Breakroot Knuckle', breakroot_run: 'Breakroot Run', breakroot_burst: 'Breakroot Burst',
  crookwood_log: 'Crookwood Log', crookwood_leaves: 'Crookwood Leaves', shieldcrown_log: 'Shieldcrown Log', shieldcrown_leaves: 'Shieldcrown Leaves',
  potted_springcurl: 'Potted Springcurl', potted_corkscrew_rush: 'Potted Corkscrew Rush', potted_vigil_bell: 'Potted Vigil Bell', potted_shiftcap: 'Potted Shiftcap' };
if (process.argv.includes('--lang')) console.log(Object.entries(NAMES).map(([k, v]) => `  "block.${NS}.${k}": "${v}",`).join('\n'));
const issues = [...G.checks(), ...problems];
console.log(issues.length ? issues.join('\n') : `overgrowth written: ${Object.keys(NAMES).length} blocks, ${usedTex.size} textures; checks ok (UVs on the texture, no zero-width UV, bounds -16..32, rotations, no z-fights)`);
process.exitCode = issues.length ? 1 : 0;
module.exports = { NAMES };
