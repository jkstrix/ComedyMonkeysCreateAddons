// Writes the Fluid Lines into the mod: the line's block-model parts (a hub, arms and risers in two lengths, the flange
// at a tank, the Intake), the multipart blockstates that assemble them from the six sides, the item models and the
// textures. Every part is authored facing north (or up, for the risers) and turned into place by the blockstate.
// Usage: node design/models/fluid-and-power-lines.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, fitGui } = require('./process-box.lib.js');
const G = require('./fluid-and-power-lines.gen.js');

const NS = 'comedymonkeysaddons';
const MODELS = path.join(ASSETS, 'models');
const TEXTURES = path.join(ASSETS, 'textures/block/fluid_line');
const TIERS = ['copper', 'brass'];
const ref = (key) => (key.includes(':') ? key : `${NS}:block/fluid_line/${key}`);
const used = new Set();

// A part list -> Java block model JSON (cutout, no AO). Faces of an element marked tint get tintindex 0; with tintBody
// the line's own body (its copper or brass) becomes the near-white body texture, tinted in game by tier or dye.
// A model with glass in it (an item, or a glass part) is translucent; the block parts keep their glass apart.
// With tintRing the metal's ring shade on a hub or flange becomes the near-white joint body, tinted (tintindex 2) with the
// ring shade, or on a dyed line the dye a shade darker; the thin coupling rings and collars keep the metal.
function blockModel(cubes, particle, { tintBody = false, tintRing = false } = {}) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_');
      while ([...vars.values()].includes(n)) n += '_';
      vars.set(r, n);
    }
    return '#' + vars.get(r);
  };
  const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      let t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const body = tintBody && (t === 'copper' || t === 'brass' || t === 'brass_twin');
      const joint = tintRing && (t === 'copper_ring' || t === 'brass_ring');
      if (body) t = t === 'brass_twin' ? 'body_twin' : 'body';
      if (joint) t = 'ring_body';
      if (!t.includes(':')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: r3(raw), texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
      if (c.tint || body) faces[dir].tintindex = 0;
      if (joint) faces[dir].tintindex = 2;
    }
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  if (!particle.includes(':')) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  const translucent = cubes.some((c) => c.glass);
  return { parent: 'block/block', render_type: translucent ? 'minecraft:translucent' : 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
// An item model: the block model, with an inventory view that fits the part in the slot (lines and fittings are far
// smaller than a block, so block/block's view left them a speck in the corner of the slot).
function itemModel(cubes, particle, opts) {
  const m = blockModel(cubes, particle, opts);
  m.display = { gui: fitGui(m.elements) };
  return m;
}
const solid = (cubes) => cubes.filter((c) => !c.glass), glassOf = (cubes) => cubes.filter((c) => c.glass);
function writeJson(rel, obj) {
  const file = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
  console.log('wrote ' + rel);
}

// ---------- block-model parts ----------
const part = (name) => `${NS}:block/fluid_line/${name}`;
for (const tier of TIERS) {
  writeJson(`models/block/fluid_line/${tier}_hub.json`, blockModel(G.hub(tier + '_hub', 6, tier + '_ring'), tier, { tintBody: true, tintRing: true }));
  // arms: from the middle (no hub: the slot runs open into the middle) or from the hub's edge, to the north face
  writeJson(`models/block/fluid_line/${tier}_arm.json`, blockModel(solid(G.pipeArm('north', tier, { from: 8, openInner: true })), tier, { tintBody: true }));
  // (from the hub: a thin collar of the metal where it leaves it)
  writeJson(`models/block/fluid_line/${tier}_arm_short.json`, blockModel(solid(G.pipeArm('north', tier, { from: 11, collar: true })), tier, { tintBody: true }));
  // risers: the same up the block, their sight slot on the north and south sides
  writeJson(`models/block/fluid_line/${tier}_riser.json`, blockModel(solid(G.pipeArm('up', tier, { from: 8, openInner: true })), tier, { tintBody: true }));
  writeJson(`models/block/fluid_line/${tier}_riser_short.json`, blockModel(solid(G.pipeArm('up', tier, { from: 11, collar: true })), tier, { tintBody: true }));
  writeJson(`models/block/fluid_line/${tier}_flange.json`, blockModel(G.flange('north', tier + '_ring'), tier, { tintBody: true, tintRing: true }));
}
// The glass over the sight slot, the same for both tiers: the long arm's and the short (hub) arm's
writeJson('models/block/fluid_line/glass.json', blockModel(glassOf(G.pipeArm('north', 'copper', { from: 8, openInner: true })), 'copper'));
writeJson('models/block/fluid_line/glass_short.json', blockModel(glassOf(G.pipeArm('north', 'copper', { from: 11 })), 'copper'));
writeJson('models/block/fluid_line/glass_riser.json', blockModel(glassOf(G.pipeArm('up', 'copper', { from: 8, openInner: true })), 'copper'));
writeJson('models/block/fluid_line/glass_riser_short.json', blockModel(glassOf(G.pipeArm('up', 'copper', { from: 11 })), 'copper'));
// The Intake on the north side: a brass flange on the tank, a dark-steel collar, and its window on top, a glass cover
// over the fluid the renderer draws (at y 11.25, over the collar's top at 11)
const intakePart = [
  ...G.flange('north', 'brass'),
  ...G.rod('intake_collar', 'z', 1, 5, 6, 'steel', { capHi: true }),
];
writeJson('models/block/fluid_line/intake.json', blockModel(intakePart, 'steel'));
writeJson('models/block/fluid_line/intake_glass.json', blockModel([G.glass(G.C('intake_window', [7, 11, 1.75], [9, 11.5, 4.25], { all: 'glass', down: null }))], 'steel'));

// ---------- fitting parts (drawn by the line's renderer round a straight piece along x) ----------
const fit = (name, cubes, particle = 'steel') => writeJson(`models/block/line_fitting/${name}.json`, blockModel(cubes, particle));
fit('valve_body', [...G.rod('valve_body', 'x', 5.5, 10.5, 6, 'steel', { capLo: true, capHi: true }),
  G.C('valve_stem', [7.5, 11, 7.5], [8.5, 14, 8.5], { all: 'steel_dark', down: null })]);
fit('valve_wheel', G.valve('copper', null).wheel, 'red');
// the Valve's and Filter's signal flag, hanging (the renderer swings it up about z at 8, 9)
fit('flag', G.flag(false), 'red');
// the Filter: a canister with glass windows on top and on the south side, over the fluid the renderer draws beneath
{
  const f = G.filter('copper', null, null).filter((c) => /^filter_/.test(c.name));
  fit('filter', solid(f));
  fit('filter_glass', glassOf(f));
}
{
  const m = G.meterPipe('copper', null);
  fit('meter', m.parts.filter((c) => /^(meter_|gauge_)/.test(c.name)));
  fit('meter_needle', m.needle, 'red');
}
fit('splitter', G.hub('splitter_hub', 7, 'steel'));
for (const mode of ['even', 'nearest', 'robin']) fit('splitter_dial_' + mode, G.splitterPipe('copper', null, mode).dial);

// ---------- blockstates ----------
// Parts authored facing north are turned with y for the four sides, x for up and down; risers are authored up
const SIDES = ['north', 'east', 'south', 'west', 'up', 'down'];
const HORIZONTAL_Y = { north: 0, east: 90, south: 180, west: 270 };
const turn = (side) => (side in HORIZONTAL_Y ? (HORIZONTAL_Y[side] ? { y: HORIZONTAL_Y[side] } : {}) : side === 'up' ? { x: 270 } : { x: 90 });
const riserTurn = (side) => (side === 'up' ? {} : { x: 180 });
for (const tier of TIERS) {
  const multipart = [{ when: { hub: 'true' }, apply: { model: part(tier + '_hub') } }];
  for (const side of SIDES) {
    const vertical = side === 'up' || side === 'down';
    const t = vertical ? riserTurn(side) : turn(side);
    const long = vertical ? tier + '_riser' : tier + '_arm', short = long + '_short';
    multipart.push({ when: { [side]: 'line|tank|intake', hub: 'false' }, apply: { model: part(long), ...t } });
    multipart.push({ when: { [side]: 'line|tank|intake', hub: 'true' }, apply: { model: part(short), ...t } });
    const glassPart = vertical ? 'glass_riser' : 'glass';
    multipart.push({ when: { [side]: 'line|tank|intake', hub: 'false' }, apply: { model: part(glassPart), ...t } });
    multipart.push({ when: { [side]: 'line|tank|intake', hub: 'true' }, apply: { model: part(glassPart + '_short'), ...t } });
    multipart.push({ when: { [side]: 'tank' }, apply: { model: part(tier + '_flange'), ...turn(side) } });
    multipart.push({ when: { [side]: 'intake' }, apply: { model: part('intake'), ...turn(side) } });
    multipart.push({ when: { [side]: 'intake' }, apply: { model: part('intake_glass'), ...turn(side) } });
  }
  writeJson(`blockstates/${tier}_fluid_line.json`, { multipart });
}

// ---------- item models ----------
const icon = (name) => G.ICONS.find(([n]) => n === name)[1];
for (const tier of TIERS) writeJson(`models/item/${tier}_fluid_line.json`, itemModel(icon(tier + '_fluid_line'), tier, { tintBody: true }));
for (const f of ['valve', 'filter', 'meter', 'splitter', 'breaker']) writeJson(`models/item/${f}.json`, itemModel(icon(f), 'steel'));
writeJson('models/item/intake.json', itemModel(icon('intake'), 'steel'));


// ---------- Power Lines: wire parts, authored lying on the floor and running north from the middle ----------
// The game's wire model turns these to any face and any direction across it. Styles: bare copper (1 px), bare brass twin
// flex (1.5 x 0.75 px) and the covered cable a dyed wire becomes (1.75 px; 2.25 x 1.75 over brass).
const STYLES = { copper: [1, 1, 'copper'], brass: [1.5, 0.75, 'brass_twin'], copper_covered: [1.75, 1.75, 'copper'], brass_covered: [2.25, 1.75, 'copper'] };
for (const [style, [w, h, tex]] of Object.entries(STYLES)) {
  const tier = style.startsWith('brass') ? 'brass' : 'copper', clip = tier + '_ring', a = 8 - w / 2, b = 8 + w / 2;
  const wire = (name, cubes) => writeJson(`models/block/power_line/${style}_${name}.json`, blockModel(cubes, tier, { tintBody: true }));
  wire('centre', [G.C('centre', [a, 0, a], [b, h, b], { all: tex, down: null })]);
  // to the north edge, and (inner) only as far as a wire up the north face of the same block
  wire('arm', [G.C('arm', [a, 0, 0], [b, h, a], { all: tex, down: null, north: null, south: null })]);
  // on into a Line Post's block, to the middle under its foot (as the showcase's wire under pole A)
  wire('post_reach', [G.C('reach', [a, 0, -8], [b, h, 0], { all: tex, down: null, south: null })]);
  wire('arm_inner', [G.C('arm', [a, 0, h], [b, h, a], { all: tex, down: null, south: null })]);
  // folding over the edge, down the far side of the block it lies on (an outside corner)
  wire('fold', [G.C('fold', [a, 0, -h], [b, h, 0], { all: tex, down: null, south: null })]);
  // a clip across the middle, a splice at a junction, a terminal lug at the north edge (against a machine)
  const cz = w <= 1 ? 0.75 : 0.5;
  wire('clip', [G.C('clip', [a - 0.5, 0, 8 - cz], [b + 0.5, h + 0.5, 8 + cz], { all: clip, down: null })]);
  const t = h + 0.75;
  // sized from the cable, so its sides never lie within a hair of the arms'
  const sl = w / 2 + 1, sw = w / 2 + 0.5;
  wire('splice', [G.C('splice_a', [8 - sl, 0, 8 - sw], [8 + sl, t, 8 + sw], { all: clip, down: null }), G.C('splice_b', [8 - sw, 0, 8 - sl], [8 + sw, t, 8 + sl], { all: clip, down: null })]);
  // into a machine at the north edge: up its face (z 0) to the middle, and a socket there; or straight up through the
  // block into a machine above, a socket in the middle of its face
  const so = 1.5;
  wire('rise', [G.C('rise', [a, 0, 0], [b, 8 - so, h], { all: tex, north: null, down: null })]);
  wire('socket', [G.C('socket', [8 - so, 8 - so, 0], [8 + so, 8 + so, h + 0.75], { all: clip, north: null })]);
  wire('riser', [G.C('riser', [a, h, a], [b, 16 - h - 0.75, b], { all: tex, up: null, down: null }),
    G.C('riser_socket', [8 - so, 16 - h - 0.75, 8 - so], [8 + so, 16, 8 + so], { all: clip, up: null })]);
  // through a drilled wall: the surround on the cable's free sides (never against the floor or where it runs off), and
  // the dark hole inside it. i: the hole's half-width, o: the surround's, s: how far the surround stands off the wall
  const i = w / 2 + 0.25, o = w / 2 + 1, s = h + 0.5, bar = { all: 'steel_dark', down: null }, hole = (f) => ({ [f]: 'slot_idle', all: null });
  // a floor wire running north into the wall at z 0: either side and over the top
  wire('grommet', [G.C('grommet_w', [8 - o, 0, 0], [8 - i, h + 1, 0.75], { ...bar, north: null }), G.C('grommet_e', [8 + i, 0, 0], [8 + o, h + 1, 0.75], { ...bar, north: null }),
    G.C('grommet_t', [8 - i, h + 0.25, 0], [8 + i, h + 1, 0.75], { all: 'steel_dark', north: null }), G.C('grommet_hole', [8 - i, 0, 0], [8 + i, h + 0.25, 0.25], hole('south'))]);
  // a wire lying on the wall, running north to a hole at the wall's edge (it turns into the wall there): either side only
  wire('hole_edge', [G.C('hole_edge_w', [8 - o, 0, 0], [8 - i, s, w + 0.25], { ...bar, north: null }), G.C('hole_edge_e', [8 + i, 0, 0], [8 + o, s, w + 0.25], { ...bar, north: null }),
    G.C('hole_edge_hole', [8 - i, 0, 0], [8 + i, 0.25, w + 0.25], hole('up'))]);
  // a hole in the middle of the wall the wire lies on, and the surround's bar on its north side (long: across the whole
  // surround; short: between the long ones), left out where the wire runs off
  wire('hole', [G.C('hole', [8 - i, 0, 8 - i], [8 + i, 0.25, 8 + i], hole('up'))]);
  wire('hole_bar_long', [G.C('hole_bar', [8 - o, 0, 8 - o], [8 + o, s, 8 - i], bar)]);
  wire('hole_bar_short', [G.C('hole_bar', [8 - i, 0, 8 - o], [8 + i, s, 8 - i], bar)]);
}
// the wire fittings, lying on the floor across a wire running along x
{
  const br = G.breaker({ w: 1, h: 1, tex: 'copper' });
  fit('breaker', br.parts);
  fit('breaker_lever', br.lever, 'steel_dark');
  const m = G.meterWire();
  fit('wire_meter', m.parts);
  fit('wire_meter_needle', m.needle, 'red');
  fit('wire_splitter', G.splitterWire('even').parts);
  for (const mode of ['even', 'nearest', 'robin']) fit('wire_splitter_dial_' + mode, G.splitterWire(mode).dial);
  fit('wire_splitter_dial_first', G.splitterFirstDial());
}

// ---------- Line Posts: wooden, no tier; authored standing on the floor ----------
const post = (name, cubes) => writeJson(`models/block/line_post/${name}.json`, blockModel(cubes, 'wood'));
post('single', G.postBlock('single', 'steel_dark'));
post('foot', G.postBlock('foot', 'steel_dark'));
post('pole', G.postBlock('pole', 'steel_dark'));
post('head', G.postBlock('head', 'steel_dark'));
{
  const FACING = { up: {}, down: { x: 180 }, north: { x: 90 }, south: { x: 270 }, east: { x: 90, y: 90 }, west: { x: 90, y: 270 } };
  const BRANCH_Y = { north: 0, east: 90, south: 180, west: 270 };
  const multipart = [];
  for (const [facing, rot] of Object.entries(FACING)) {
    for (const role of ['single', 'foot', 'pole', 'head'])
      multipart.push({ when: { facing, role }, apply: { model: `${NS}:block/line_post/${role}`, ...rot } });
    // each side's branch is its own model, authored on an upright post; only a head or a lone post shows them
    for (const side of Object.keys(BRANCH_Y)) {
      multipart.push({ when: { facing, [side]: 'true', role: 'head|single' }, apply: { model: `${NS}:block/line_post/branch_${side}`, ...rot } });
    }
  }
  writeJson('blockstates/line_post.json', { multipart });
  // a branch model per side, authored on an upright post (the facing's rotation stands it on its face)
  for (const side of Object.keys(BRANCH_Y)) post('branch_' + side, G.branch(side, 'steel_dark'));
}

// the wire's blockstates: one base model (its particle); the game's wire model builds every wire from the parts
for (const tier of TIERS) writeJson(`blockstates/${tier}_power_line.json`, { variants: { '': { model: `${NS}:block/power_line/${tier}_centre` } } });

// ---------- Power Line and Line Post items ----------
for (const tier of TIERS) writeJson(`models/item/${tier}_power_line.json`, itemModel(icon(tier + '_power_line'), tier, { tintBody: true }));
writeJson('models/item/line_post.json', itemModel(icon('line_post'), 'wood'));

// ---------- textures ----------
fs.mkdirSync(TEXTURES, { recursive: true });
for (const key of used) {
  if (!G.T.OWN[key]) throw new Error('no texture ' + key);
  fs.writeFileSync(path.join(TEXTURES, key + '.png'), G.T.OWN[key]);
  console.log('wrote textures/block/fluid_line/' + key + '.png');
}
