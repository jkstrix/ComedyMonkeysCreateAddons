// Clockwork Wastes salvage, shells, decor and items preview -> design/models/clockwork-wastes-salvage.bbmodel (Generic Model).
// To design/briefs/clockwork-wastes-blocks.md §8-10: the broken Create machines (Press, Mixer, Deployer, Mechanical Arm,
// Crushing Wheel), Worker Shells and Resistance Shells (three poses each), the Charging Alcove (with a trinket), the Maker
// Statue (standing, toppled), the Scrap Heap, and the Unknown Device and Clockwork Scrap icons. The plates, pillars and
// Scorched End Stone are on the material sheet.
// node clockwork-wastes-salvage.gen.js
// Block model space (x 0 west -> 16 east, y up, z 0 north -> 16 south), fronts to the north. The shells are one rig each
// (torso, head, arms, legs as bones), posed by bone rotations, so they pose the way the bots will.
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, STEEL, IRON, BRASS, BRASS_AGED, brassAged, COPPER, PATINA, WALNUT, RUST, ANDESITE, GROUND, mott, edge, box, zdisc, endbrick, vaTile } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-salvage.bbmodel');
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';

// ---------- textures ----------
const DUST = ['#7c7659', '#878063'];
const dusty = (base, s) => (x, y) => { const r = hash(x, y, s), drift = y === 0 ? 0.25 : y === 1 && (x < 3 || x > 12) ? 0.4 : 1; return r > drift ? DUST[hash(x, y, s + 1) > 0.5 ? 1 : 0] : base(x, y); };
// the makers' andesite casing, aged a step: an andesite frame round a darker inset panel (Create's casing language, our pixels)
const casing = (x, y) => {
  const e = edge(x, y);
  if (e === 0) return x === 0 || y === 0 ? '#767873' : '#4f524e';
  if (e === 1) return mott(['#5e615d', '#666964', '#6b6e6a'], 71, 0.2, 0.9)(x, y);
  if (e === 2) return x === 2 || y === 2 ? '#4f524e' : '#6b6e6a';
  return mott(['#505350', '#575a56', '#5e615d'], 72, 0.2, 0.9)(x, y);
};
T.paint('casing_aged', casing);
T.paint('casing_aged_rust', (x, y) => (x === 13 && y >= 1 && y <= 9 ? (y < 6 ? RUST[1] : RUST[0]) : x === 12 && y >= 1 && y <= 3 ? RUST[0] : casing(x, y)));
T.paint('casing_aged_top', dusty(casing, 73));
// a crack across the casing: a dark line, the inset panel split
T.paint('casing_cracked', (x, y) => ([[3, 2], [4, 3], [5, 4], [5, 5], [6, 6], [7, 7], [7, 8], [8, 9], [9, 10], [10, 11]].some(([a, b]) => a === x && b === y) ? '#2a2c2a' : casing(x, y)));
T.paint('press_steel', (x, y) => (edge(x, y) === 0 ? '#2a2f35' : hash(x, y, 74) > 0.88 ? '#40464e' : STEEL[1]));
T.paint('steel_worn', (x, y) => (y < 2 && hash(x, y, 75) > 0.55 ? STEEL[2] : hash(x, y, 76) > 0.9 ? '#40464e' : STEEL[1]));
// every brass part here is the makers' or the resistance's old work: aged brass
T.paint('brass', brassAged(77));
T.paint('copper', mott(COPPER, 78, 0.15, 0.85));
T.paint('basin', (x, y) => (edge(x, y) === 0 ? '#4f524e' : mott(['#5e615d', '#666964', '#6b6e6a'], 79, 0.2, 0.9)(x, y)));
// the Crushing Wheel: dark stone, its grinding ridges running round the rim (concentric about the axle at the face's foot)
T.paint('wheel_stone', (x, y) => { const r = Math.hypot(x + 0.5 - 8, 16 - (y + 0.5)); return Math.floor(r) % 3 === 0 ? '#4a4d4a' : hash(x, y, 80) > 0.85 ? '#5e615d' : '#545753'; });
T.paint('steel_aged', (x, y) => (hash(x, y, 91) > 0.85 ? STEEL[1] : STEEL[0]));
T.paint('wheel_break', (x, y) => (hash(x, y, 81) > 0.6 ? '#3a3c3a' : '#454845'));
// the Worker: void-alloy, its dead lens dark glass with one dull catchlight; a brass citizen tag
T.paint('lens_dark', (x, y) => (x === 7 && y === 7 ? VA.glint : VA.glass));
// the ring the lens sits in: a lighter facet than the head, lit along its top-left, as the family face (Trade Bot's pods) asks
T.paint('lens_ring', (x, y) => (x + y < 15 ? '#4c475f' : '#3a3650'));
// the resistance: its casing torso with brass corner straps, its glass eyes, its spring strip
T.paint('res_casing', (x, y) => (x <= 1 || x >= 14 ? brassAged(82)(x, y) : (x === 2 || x === 13) ? '#4f524e' : (y === 4 || y === 11) && x % 3 === 0 ? '#767873' : mott(['#5e615d', '#666964', '#6b6e6a'], 83, 0.2, 0.9)(x, y)));
T.paint('res_patch', (x, y) => (edge(x, y) === 0 ? WALNUT[0] : y % 4 === 0 ? WALNUT[0] : WALNUT[1]));
T.paint('glass_eye', (x, y) => (x === 7 && y === 7 ? '#6b6e6a' : '#3a3430'));
T.paint('key_hole', () => '#1a1714');
// the Maker Statue: carved End Stone, smooth (not brick), weathered darker in the folds; aged brass trims
T.paint('carved', (x, y) => (hash(x, y, 84) > 0.88 ? '#77714f' : hash(x, y, 85) > 0.8 ? '#8d876a' : '#827d5f'));
// robe folds as cloth: irregular spacing (3, 5, 4, 6 px), each fold starting and stopping at its own height and fading upward
const FOLDS = [[1, 4], [4, 9], [9, 2], [13, 6], [15, 11]];
T.paint('carved_fold', (x, y) => { for (const [fx, top] of FOLDS) if (x === fx && y >= top) return y > 12 ? '#625d46' : '#6c6750'; return hash(x, y, 86) > 0.85 ? '#8d876a' : '#827d5f'; });
// the Charging Alcove's contact plate, dark and lit
T.paint('contact_dark', (x, y) => (y % 3 === 0 ? VA.lo : VA.seam));
T.paint('contact_lit', (x, y) => (y % 3 === 0 ? STAR.dim : STAR.edge));
T.paint('flower', (x, y) => (hash(x, y, 87) > 0.5 ? '#8a6a72' : '#7a5c64'));
T.paint('flower_stem', () => '#4f5236');
// the Scrap Heap: rusted iron, bent bars, chain and cog teeth; the heap's crust
// scrap in pieces: 3 x 2 chunks, each one material (rust-streaked iron plate, steel bar, aged brass tooth, soot in the hollows),
// with a dark gap round each piece, so it reads as things to pick through, not static
T.paint('scrap_mix', (x, y) => {
  const cx = Math.floor(x / 3), cy = Math.floor(y / 2), ox = cy % 2 ? 1 : 0;
  const kx = Math.floor((x + ox) / 3);
  if ((x + ox) % 3 === 2 || y % 2 === 1 && hash(kx, cy, 92) > 0.5) return '#1a1714';
  const r = hash(kx, cy, 88);
  if (r < 0.35) return x % 3 === 0 ? RUST[1] : IRON[1];
  if (r < 0.6) return STEEL[1];
  if (r < 0.72) return BRASS_AGED[1];
  if (r < 0.85) return RUST[0];
  return '#26211c';
});
T.paint('rust', mott(RUST, 89, 0.25, 0.85));

// ---------- item icons (16 x 16 sprites, a step lighter than block textures, as the house rule for icons) ----------
// The Unknown Device: a sealed void-alloy puck seen three-quarter, a broken-chain seam of starlight across its top, a small
// lens on its rim, one violet glint
T.paint('icon_device', (x, y) => {
  const tx = (x + 0.5 - 8) / 6.5, ty = (y + 0.5 - 6.5) / 3.6, top = tx * tx + ty * ty <= 1;
  const sx = (x + 0.5 - 8) / 6.5, sy = (y + 0.5 - 10) / 3.6, side = !top && Math.abs(sx) <= 1 && y >= 6 && (sx * sx + sy * sy <= 1 || y <= 10);
  const out = (dx, dy) => { const a = ((x + dx) + 0.5 - 8) / 6.5, b = ((y + dy) + 0.5 - 6.5) / 3.6, c = ((y + dy) + 0.5 - 10) / 3.6; return (a * a + b * b <= 1) || (Math.abs(a) <= 1 && (y + dy) >= 6 && (a * a + c * c <= 1 || (y + dy) <= 10)); };
  if (!top && !side) return [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => out(dx, dy)) ? '#121118' : null;
  if (top) {
    if (y === 6 && x >= 3 && x <= 12 && x !== 7 && x !== 8) return x === 9 ? STAR.core : STAR.edge;
    if (y === 5 && x === 8) return STAR.edge;
    if (x === 4 && y === 4) return '#6d6696';
    return ty < -0.3 ? '#46415e' : '#3a3650';
  }
  if (x === 12 && (y === 9 || y === 10)) return y === 9 ? STAR.edge : STAR.dim;
  return y >= 12 ? '#1d1c25' : '#2a2838';
});
// Clockwork Scrap: a bent brass cog with two teeth missing, a curl of patina'd copper wire, a sliver of void-alloy
T.paint('icon_scrap', (x, y) => {
  const dx = x + 0.5 - 7, dy = y + 0.5 - 8.5, r = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
  const s = Math.PI / 4, m = ((a % s) + s) % s, tooth = Math.min(m, s - m) < 0.28 && !(a > -0.5 && a < 0.5);
  const wire = [[11, 11], [12, 11], [13, 12], [13, 13], [12, 14], [11, 14], [10, 13], [11, 12]];
  if (wire.some(([p, q]) => p === x && q === y)) return x + y > 24 ? PATINA[0] : (x === 12 && y === 11 ? '#b26247' : PATINA[1]);
  if ([[11, 2], [12, 3], [13, 3], [12, 2]].some(([p, q]) => p === x && q === y)) return x === 11 ? '#4c475f' : '#25232f';
  if (r < 1.6) return '#4e3f1c';
  if (r < 4.4) return dx + dy < 0 ? '#c9a75a' : r > 3.6 ? '#7e6832' : '#a3873f';
  if (r < 5.8 && tooth) return dx + dy < 0 ? '#b08f45' : '#7e6832';
  if (r < 6.6 && (r < 4.6 || (r < 6.2 && tooth))) return '#3a2f14';
  return null;
});

// ---------- helpers ----------
const faces = (tex, o = {}) => (typeof tex === 'string' ? { all: tex, ...o } : tex);
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]), rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const named = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));
const card = (name, tex) => [box(name, [0, 0, 8], [16, 16, 8], { north: tex, south: tex }, { shade: false, uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] } })];

// ---------- the broken machines (the makers' Create kit, each broken one readable way) ----------
// Broken Press: the casing's north panel gone, showing the shaft snapped in two; the head dropped and jammed crooked on the
// ground, its pole bent
function brokenPress() {
  return [
    ...[[0, 0], [14, 0], [0, 14], [14, 14]].map(([x, z]) => box(`post_${x}_${z}`, [x, 6, z], [x + 2, 14, z + 2], 'casing_aged')),
    box('lid', [0, 14, 0], [16, 16, 16], faces('casing_aged', { up: 'casing_aged_top', down: null })),
    box('floor', [0, 6, 0], [16, 8, 16], faces('casing_aged', { up: null })),
    box('panel_e', [15, 8, 2], [16, 14, 14], faces('casing_aged_rust', { west: null })), box('panel_w', [0, 8, 2], [1, 14, 14], faces('casing_aged', { east: null })),
    box('panel_s', [2, 8, 15], [14, 14, 16], faces('casing_aged', { north: null })),
    // the shaft, snapped: the west half straight, the east half drooping at the break
    box('shaft_w', [1, 10, 6], [7, 14, 10], { north: AXIS, south: AXIS, up: AXIS, down: AXIS, west: AXIS_TOP, east: AXIS_TOP }, { uv: { north: [0, 6, 6, 10], south: [0, 6, 6, 10], up: [0, 6, 6, 10], down: [0, 6, 6, 10], west: [6, 6, 10, 10], east: [6, 6, 10, 10] }, frot: { north: 90, south: 90, up: 90, down: 90 } }),
    box('shaft_e', [9, 10, 6], [15, 14, 10], { north: AXIS, south: AXIS, up: AXIS, down: AXIS, west: AXIS_TOP, east: AXIS_TOP }, { rot: ['z', -22.5, [15, 12, 8]], uv: { north: [0, 6, 6, 10], south: [0, 6, 6, 10], up: [0, 6, 6, 10], down: [0, 6, 6, 10], west: [6, 6, 10, 10], east: [6, 6, 10, 10] }, frot: { north: 90, south: 90, up: 90, down: 90 } }),
    // the pole, bent, from the casing's floor down to the head
    box('pole_top', [7, 3.5, 7], [9, 6, 9], 'steel_worn'),
    box('pole_bent', [7, 1, 7], [9, 4.5, 9], 'steel_worn', { rot: ['z', 22.5, [8, 4, 8]] }),
    // the head, dropped and jammed crooked
    box('head', [3, 0, 3.5], [13, 3, 12.5], faces('press_steel'), { rot: ['z', -22.5, [8, 1.5, 8]] }),
    box('head_plate', [5, 3, 5.5], [11, 4, 10.5], 'press_steel', { rot: ['z', -22.5, [8, 1.5, 8]] }),
  ];
}
// Broken Mixer: the casing above; the whisk bent and wedged in a cracked basin (one wall split away, leaning out)
function brokenMixer() {
  const out = [box('casing', [0, 10, 0], [16, 16, 16], faces('casing_aged', { up: 'casing_aged_top', east: 'casing_aged_rust' })),
    box('basin_floor', [1, 0, 1], [15, 1, 15], 'basin'), box('basin_w', [1, 1, 1], [2, 6, 15], 'basin'), box('basin_e', [14, 1, 1], [15, 6, 15], 'basin'),
    box('basin_s', [2, 1, 14], [14, 6, 15], 'basin'),
    // the north wall cracked away, leaning out
    box('basin_n', [2, 0.5, 0.5], [14, 5.5, 1.5], 'basin', { rot: ['x', -22.5, [8, 0.5, 1.5]] })];
  // the whisk on its shaft, bent 22.5 degrees (a block model's nearest to the brief's 30), its loops wedged in the basin
  const whisk = [box('whisk_shaft', [7.5, 3, 7.5], [8.5, 10, 8.5], 'steel_worn'),
    box('loop_a_l', [4.5, 2, 7.5], [5.5, 7, 8.5], 'steel_worn'), box('loop_a_r', [10.5, 2, 7.5], [11.5, 7, 8.5], 'steel_worn'), box('loop_a_b', [5.5, 2, 7.5], [10.5, 3, 8.5], 'steel_worn'),
    box('loop_b_n', [7.5, 2, 4.5], [8.5, 7, 5.5], 'steel_worn'), box('loop_b_s', [7.5, 2, 10.5], [8.5, 7, 11.5], 'steel_worn'), box('loop_b_b', [7.5, 2, 5.5], [8.5, 3, 7.5], 'steel_worn'), box('loop_b_b2', [7.5, 2, 8.5], [8.5, 3, 10.5], 'steel_worn')];
  return [...out, ...whisk.map((c) => ({ ...c, rot: ['z', 22.5, [8, 10, 8]] }))];
}
// Broken Deployer: the casing, its arm out north and snapped at the elbow, the brass hand hanging from a cable
function brokenDeployer() {
  return [box('casing', [0, 0, 0], [16, 16, 16], faces('casing_aged', { up: 'casing_aged_top', west: 'casing_aged_rust' })),
    box('collar', [5, 5, -1], [11, 11, 0], 'press_steel'),
    box('arm', [7, 7, -8], [9, 9, -1], 'steel_worn'),
    box('elbow', [6.5, 6.5, -9], [9.5, 9.5, -7.5], 'press_steel'),
    // the forearm, snapped, hanging down
    box('forearm', [7, 1.5, -9], [9, 7, -7], 'steel_worn', { rot: ['x', 22.5, [8, 7, -8]] }),
    // the hand, dropped to the ground beyond it, hanging off a copper cable from the elbow
    box('cable', [7.75, 0.5, -12], [8.25, 1, -8.5], 'copper'), box('cable_drop', [7.75, 1, -8.5], [8.25, 7, -8], 'copper'),
    box('hand_palm', [6, 0, -15], [10, 1.5, -12], 'brass'),
    box('finger_a', [6, 0, -16], [7, 1, -15], 'brass'), box('finger_b', [7.5, 0, -16], [8.5, 1, -15], 'brass'), box('finger_c', [9, 0, -16], [10, 1, -15], 'brass')];
}
// Broken Mechanical Arm: the base cracked and leaning, the arm frozen mid-reach, its claw open over nothing
function brokenArm() {
  const base = [box('base', [2, 0, 2], [14, 3, 14], faces('casing_cracked', { up: 'casing_aged_top' })), box('ring', [4, 3, 4], [12, 4, 12], 'brass')];
  const arm = [box('turret', [5, 4, 5], [11, 7, 11], 'casing_aged'),
    box('lower', [7, 7, 7], [9, 17, 9], 'steel_worn'), box('elbow', [6.5, 16, 6.5], [9.5, 19, 9.5], 'brass'),
    box('upper', [7, 17, 0], [9, 18.5, 8], 'steel_worn'),
    box('wrist', [6.5, 16, -1.5], [9.5, 18.75, 0.5], 'brass'),
    box('claw_l', [6, 13, -2.5], [7, 17, -1.5], 'brass', { rot: ['z', -22.5, [6.5, 17, -2]] }), box('claw_r', [9, 13, -2.5], [10, 17, -1.5], 'brass', { rot: ['z', 22.5, [9.5, 17, -2]] })];
  return { base, arm };
}
// Broken Crushing Wheel: a toothed stone wheel split in two; one half stands on its broken edge, the other lies on its side
function halfWheel(name, r, w) {
  const out = [];
  for (let y = 0; y < r; y += 1) {
    const hw = Math.round(Math.sqrt(r * r - (y + 0.5) * (y + 0.5)) * 2) / 2;
    out.push(box(`${name}_${y}`, [8 - hw, y, 8 - w / 2], [8 + hw, y + 1, 8 + w / 2], faces('wheel_stone', { down: y === 0 ? 'wheel_break' : null })));
  }
  for (let i = 0; i < 5; i++) { const a = (i + 0.5) / 5 * Math.PI, x = 8 + Math.cos(a) * (r + 0.5), y = Math.sin(a) * (r + 0.5); out.push(box(`${name}_tooth_${i}`, [x - 1, y - 1, 8 - w / 2 - 0.5], [x + 1, y + 1, 8 + w / 2 + 0.5], 'steel_aged')); }
  return out;
}
function brokenWheel() {
  const up = shift(halfWheel('half_up', 7, 5), [0, 0, -3]);
  const down = halfWheel('half_down', 7, 5).map((c) => ({ ...c, rot: ['x', 90, [8, 0, 8]] }));
  return [...up, ...shift(down, [0, 2.5, 4])];
}

// ---------- the shells: one rig each, posed by bone rotations ----------
// A rig node: { n: name, p: pivot, c: cubes, k: kids, r: rest rotation }
function worker() {
  return { n: 'root', p: [8, 16, 8], k: [
    { n: 'torso', p: [8, 16, 8], c: [box('pelvis', [6, 16, 6.5], [10, 18, 9.5], 'va'), box('spine', [7.25, 18, 7.25], [8.75, 20, 8.75], 'va'),
      box('chest', [5, 20, 6], [11, 26, 10], faces('va', { north: 'va' })), box('chest_cap', [5.5, 26, 6.5], [10.5, 26.5, 9.5], 'va'),
      box('tag', [6.5, 23, 5.75], [8, 24.5, 6], { north: 'brass' })], k: [
      { n: 'head', p: [8, 26.5, 8], c: [box('neck', [7.25, 26.5, 7.25], [8.75, 27.5, 8.75], 'va'), box('skull', [5.5, 27.5, 5.5], [10.5, 33, 10.5], 'va'), box('crest', [6, 33, 6], [10, 33.5, 10], 'va'),
        ...zdisc('lens_ring', 8, 30.25, 5.25, 5.5, 5, { north: 'lens_ring', all: null }), ...zdisc('lens', 8, 30.25, 5, 5.25, 4, { north: 'lens_dark', all: null })] },
      { n: 'arm_w', p: [4.25, 25, 8], c: [box('shoulder_w', [3.5, 24, 7], [5, 26, 9], 'va'), box('upper_w', [3.5, 17, 7.25], [5, 24, 8.75], 'va')], k: [
        { n: 'fore_w', p: [4.25, 17, 8], c: [box('fore_w', [3.5, 9.5, 7.25], [5, 17, 8.75], 'va'), box('clamp_w_a', [3.5, 7.5, 7.25], [4.25, 9.5, 7.75], 'va'), box('clamp_w_b', [4.25, 7.5, 8.25], [5, 9.5, 8.75], 'va')] }] },
      { n: 'arm_e', p: [11.75, 25, 8], c: [box('shoulder_e', [11, 24, 7], [12.5, 26, 9], 'va'), box('upper_e', [11, 17, 7.25], [12.5, 24, 8.75], 'va')], k: [
        { n: 'fore_e', p: [11.75, 17, 8], c: [box('fore_e', [11, 9.5, 7.25], [12.5, 17, 8.75], 'va'), box('clamp_e_a', [11, 7.5, 7.25], [11.75, 9.5, 7.75], 'va'), box('clamp_e_b', [11.75, 7.5, 8.25], [12.5, 9.5, 8.75], 'va')] }] }] },
    { n: 'leg_w', p: [7, 16, 8], c: [box('thigh_w', [6, 8, 7], [8, 16, 9], 'va')], k: [
      { n: 'shin_w', p: [7, 8, 8], c: [box('shin_w', [6.25, 1, 7.25], [7.75, 8, 8.75], 'va'), box('foot_w', [6, 0, 5.5], [8, 1, 9], 'va')] }] },
    { n: 'leg_e', p: [9, 16, 8], c: [box('thigh_e', [8, 8, 7], [10, 16, 9], 'va')], k: [
      { n: 'shin_e', p: [9, 8, 8], c: [box('shin_e', [8.25, 1, 7.25], [9.75, 8, 8.75], 'va'), box('foot_e', [8, 0, 5.5], [10, 1, 9], 'va')] }] }] };
}
function resistance(split) {
  // the mainspring drum on its back (round: it's the engine), the butterfly key standing out of it; a split one with its spring sprung
  const drum = [...zdisc('drum', 8, 12, 11, 14, 6, 'brass'), box('key_shaft', [7.5, 11.5, 14], [8.5, 12.5, 15.5], 'brass'),
    box('key_bow', [6.5, 11, 15.5], [9.5, 13, 16.5], 'brass'), box('key_wing_w', [5, 10.5, 15.5], [6.5, 13.5, 16.5], 'brass'), box('key_wing_e', [9.5, 10.5, 15.5], [11, 13.5, 16.5], 'brass')];
  if (split) drum.push(box('spring_a', [3, 9, 13], [5, 9.5, 17], 'spring_top'), box('spring_b', [3, 9, 17], [10, 9.5, 18], 'spring_top'), box('spring_c', [9, 9, 14.5], [10, 9.5, 17], 'spring_top'));
  return { n: 'root', p: [8, 8, 8], k: [
    { n: 'torso', p: [8, 8, 8], c: [box('belly', [4.5, 8, 5.5], [11.5, 10, 10.5], 'res_casing'), box('chest', [4, 10, 5], [12, 17, 11], faces('res_casing', { up: 'casing_aged_top' })),
      box('patch', [10.5, 12, 4.75], [11.75, 15, 5], { north: 'res_patch' }), box('stamp', [4.25, 15, 4.75], [5.25, 16, 5], { north: 'brass' }), ...drum], k: [
      { n: 'head', p: [8, 17, 8], c: [box('neck', [7, 17, 7], [9, 17.5, 9], 'press_steel'), box('skull', [5.5, 17.5, 5.5], [10.5, 21.5, 10.5], faces('casing_aged', { up: 'casing_aged_top' })),
        box('brow', [5.5, 20.5, 5.25], [10.5, 21, 5.5], { north: 'brass' }),
        ...zdisc('rim_w', 6.75, 19, 5.25, 5.5, 3, { north: 'brass', all: null }), ...zdisc('eye_w', 6.75, 19, 5, 5.25, 2, { north: 'glass_eye', all: null }),
        ...zdisc('rim_e', 9.25, 19, 5.25, 5.5, 3, { north: 'brass', all: null }), ...zdisc('eye_e', 9.25, 19, 5, 5.25, 2, { north: 'glass_eye', all: null })] },
      { n: 'arm_w', p: [3.25, 16, 8], c: [box('shoulder_w', [2, 15, 6.5], [4, 17, 9.5], 'brass'), box('upper_w', [2.5, 11, 7], [4, 15, 9], 'press_steel')], k: [
        { n: 'fore_w', p: [3.25, 11, 8], c: [box('fore_w', [2.25, 7, 6.75], [4.25, 11, 9.25], 'res_casing'), box('hand_w', [2.25, 5, 7], [4.25, 7, 9], 'brass')] }] },
      { n: 'arm_e', p: [12.75, 16, 8], c: [box('shoulder_e', [12, 15, 6.5], [14, 17, 9.5], 'brass'), box('upper_e', [12, 11, 7], [13.5, 15, 9], 'press_steel')], k: [
        { n: 'fore_e', p: [12.75, 11, 8], c: [box('fore_e', [11.75, 7, 6.75], [13.75, 11, 9.25], 'res_casing'), box('hand_e', [11.75, 5, 7], [13.75, 7, 9], 'brass')] }] }] },
    { n: 'leg_w', p: [6, 8, 8], c: [box('thigh_w', [4.5, 4, 6.5], [7.5, 8, 9.5], 'press_steel')], k: [
      { n: 'shin_w', p: [6, 4, 8], c: [box('shin_w', [4.75, 1, 6.75], [7.25, 4, 9.25], 'res_casing'), box('foot_w', [4.5, 0, 5], [7.5, 1, 10], 'press_steel')] }] },
    { n: 'leg_e', p: [10, 8, 8], c: [box('thigh_e', [8.5, 4, 6.5], [11.5, 8, 9.5], 'press_steel')], k: [
      { n: 'shin_e', p: [10, 4, 8], c: [box('shin_e', [8.75, 1, 6.75], [11.25, 4, 9.25], 'res_casing'), box('foot_e', [8.5, 0, 5], [11.5, 1, 10], 'press_steel')] }] }] };
}
// poses: bone -> rest rotation [x, y, z] (x > 0 swings a hanging part's lower end forward, north), plus a lift for the whole figure
const POSES = {
  worker_slumped: { lift: [0, -14, 3], r: { torso: [16, 0, 0], head: [-38, 0, 8], leg_w: [150, 0, 0], shin_w: [-160, 0, 0], leg_e: [142, 0, -6], shin_e: [-150, 0, 0], arm_w: [60, 0, -12], fore_w: [-35, 0, 0], arm_e: [60, 0, -6], fore_e: [-45, 0, 0] } },
  worker_fallen: { lift: [0, -11.2, -6], r: { root: [-90, 0, 0], head: [-15, 25, 0], arm_e: [-170, 0, 0], fore_e: [-10, 0, 0], arm_w: [-10, 0, -10], leg_w: [8, 0, 0], leg_e: [-6, 0, 4], shin_e: [20, 0, 0] } },
  worker_frozen: { lift: [0, -0.9, 0], r: { torso: [-6, 0, 0], head: [0, 12, 0], leg_w: [-30, 0, 0], shin_w: [25, 0, 0], leg_e: [18, 0, 0], shin_e: [30, 0, 0], arm_w: [20, 0, 0], fore_w: [-25, 0, 0], arm_e: [-25, 0, 0], fore_e: [-10, 0, 0] } },
  res_slumped: { lift: [0, -3.15, 1], r: { torso: [-32, 0, 0], head: [-30, 0, 0], leg_w: [-80, 0, 0], shin_w: [85, 0, 0], leg_e: [-80, 0, 0], shin_e: [85, 0, 0], arm_w: [-40, 0, 0], fore_w: [-30, 0, 0], arm_e: [-40, 0, 0], fore_e: [-30, 0, 0] } },
  res_fallen: { lift: [0, -2, 0], r: { root: [0, 0, 90], arm_e: [0, 0, 150], fore_e: [-40, 0, 0], head: [0, 0, -15], leg_w: [-25, 0, 0], shin_w: [40, 0, 0] } },
  res_reaching: { lift: [0, 0, 0], r: { torso: [10, 180, 0], head: [-25, 0, 0], arm_e: [-50, 0, 0], fore_e: [-15, 0, 0], arm_w: [10, 0, 0], leg_w: [-20, 0, 0], shin_w: [20, 0, 0] } },
};

// ---------- decor ----------
// Charging Alcove (1 x 3): an upright void-alloy cradle round a standing Worker's outline, a contact plate at its back, a ledge
// with a trinket (a toy cart: the makers' child's toy)
function alcove() {
  return [box('back', [1, 0, 13], [15, 46, 16], faces(M.VA_BODY, { north: 'va' })),
    box('cheek_w', [1, 0, 4], [3, 44, 13], M.VA_BODY), box('cheek_e', [13, 0, 4], [15, 44, 13], M.VA_BODY),
    box('arch_w', [3, 40, 5], [5, 46, 13], 'va'), box('arch_e', [11, 40, 5], [13, 46, 13], 'va'), box('arch_top', [5, 44, 5], [11, 47, 13], 'va'),
    box('sill', [3, 0, 5], [13, 2, 13], faces('va', { up: M.VA_BODY })),
    box('shoulder_rest_w', [3, 26, 9], [4.5, 28, 13], 'va'), box('shoulder_rest_e', [11.5, 26, 9], [13, 28, 13], 'va'),
    box('contact', [6.5, 12, 12.75], [9.5, 36, 13], { north: 'contact_dark' }),
    box('ledge', [11.5, 18, 6], [13, 18.5, 11], 'va'),
    // the toy cart on the ledge: a walnut body, two brass wheels
    box('cart', [12, 18.5, 7], [12.5, 19.75, 10], 'walnut'), box('cart_wheel_a', [11.75, 18.25, 7.25], [12.75, 19, 7.75], 'brass'), box('cart_wheel_b', [11.75, 18.25, 9.25], [12.75, 19, 9.75], 'brass')];
}
const alcoveLit = () => [box('contact_lit', [7, 12.5, 12.5], [9, 35.5, 12.5], { north: 'contact_lit' }, { shade: false })];
// Maker Statue (1 x 3): a tall robed figure, hands folded, its face chiselled flat and a blank void-alloy plate clamped over it
function statue() {
  return [box('plinth', [1.5, 0, 1.5], [14.5, 3, 14.5], faces('carved', { up: 'carved' })), box('plinth_band', [2, 3, 2], [14, 4, 14], 'brass'),
    box('hem', [3, 4, 3.5], [13, 12, 12.5], faces('carved_fold')), box('robe_mid', [3.5, 12, 4], [12.5, 24, 12], faces('carved_fold')),
    box('robe_top', [4, 24, 4.5], [12, 32, 11.5], 'carved'), box('shoulders', [3, 30, 5], [13, 34, 11], 'carved'), box('collar', [5, 33.5, 5.5], [11, 34.5, 10.5], 'brass'),
    box('sleeve_w', [3, 22, 3.5], [6, 30, 7], 'carved_fold'), box('sleeve_e', [10, 22, 3.5], [13, 30, 7], 'carved_fold'),
    box('hands', [6, 23, 3], [10, 26, 5], 'carved'), box('belt', [3.25, 20, 3.75], [12.75, 21, 12.25], 'brass'),
    box('hood', [5, 34.5, 5], [11, 43, 11], 'carved'), box('hood_peak', [6, 43, 6], [10, 45, 10.5], 'carved'),
    box('face_cut', [6, 35.5, 4.75], [10, 41, 5], { north: 'casing_aged' }),
    // the uprising's plate, clamped over the face, not bolted
    box('face_plate', [5.75, 35.25, 4.25], [10.25, 41.25, 4.75], faces('va', { south: null })),
    box('clamp_t', [7, 41, 4], [9, 42, 6], 'va'), box('clamp_b', [7, 34.25, 4], [9, 35.5, 6], 'va')];
}
// Scrap Heap: a half-height mound of rusted iron, a bent bar, chain links and cog teeth
function scrapHeap() {
  return [box('mound_a', [0, 0, 1], [16, 3, 15], faces('scrap_mix', { down: null })), box('mound_b', [1.5, 3, 2.5], [14, 5.5, 13.5], faces('scrap_mix', { down: null })),
    box('mound_c', [3.5, 5.5, 4], [12, 7.5, 12], faces('scrap_mix', { down: null })), box('mound_d', [5.5, 7.5, 6], [10, 8, 10], faces('scrap_mix', { down: null })),
    box('bar', [2, 6, 7.5], [14, 7, 8.5], 'rust', { rot: ['z', 22.5, [8, 6.5, 8]] }),
    box('link_a', [10, 5.5, 2], [12, 6, 4], 'press_steel'), box('link_b', [11.5, 5, 0.5], [12.25, 6.5, 2.5], 'press_steel'),
    box('cog_tooth_a', [4, 7.5, 9], [5.5, 8.5, 10.5], 'brass'), box('cog_tooth_b', [8, 8, 6.5], [9, 9, 7.5], 'brass'),
    box('plate', [1, 3, 9], [6, 3.5, 14], 'casing_aged', { rot: ['x', -22.5, [3.5, 3, 11.5]] })];
}

// the builders, for the game export (clockwork-wastes.export.js)
module.exports = { T, brokenPress, brokenMixer, brokenDeployer, brokenArm, brokenWheel, halfWheel, worker, resistance, POSES, alcove, alcoveLit, statue, scrapHeap, shift };

// ---------- the scene ----------
if (require.main === module) {
const { cube, group, clip, write } = preview('clockwork_wastes_salvage', { 'w:': T });
const outliner = [];
const REST = {};
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const G = (name, off, list, pivot = [8, 0, 8]) => outliner.push(group(name, pivot, off, put(named(list, name + '_'), off)));
function rig(prefix, node, off, pose) {
  const name = `${prefix}_${node.n}`;
  if (pose.r[node.n]) REST[name] = pose.r[node.n];
  return group(name, node.p, off, [...put(named(node.c || [], prefix + '_'), off), ...(node.k || []).map((k) => rig(prefix, k, off, pose))]);
}

// floor
{
  const floor = [];
  for (let x = -2; x <= 18; x++) for (let z = -2; z <= 8; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
  outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
}
// row 1: the broken machines
G('broken_press', at(0, 0, 0), brokenPress());
G('broken_mixer', at(2, 0, 0), brokenMixer());
G('broken_deployer', at(4, 0, 1), brokenDeployer());
{
  const off = at(6, 0, 0), a = brokenArm();
  outliner.push(group('broken_arm', [8, 0, 8], off, [...put(named(a.base, 'broken_arm_'), off), group('broken_arm_lean', [8, 4, 8], off, put(named(a.arm, 'broken_arm_'), off))]));
  REST.broken_arm_lean = [0, 0, 22.5];
}
G('broken_wheel', at(8, 0, 0), brokenWheel());
// row 2: Worker Shells (slumped against an End Stone brick wall, fallen, frozen mid-step) and Resistance Shells beside them
const wall = (name, off) => outliner.push(group(name, [8, 0, 8], off, put([box(name, [0, 0, 16], [16, 16, 18], { all: 'endbrick', up: 'endbrick' })], off)));
[['worker_slumped', 0, 3], ['worker_fallen', 2, 4], ['worker_frozen', 4, 3]].forEach(([pose, bx, bz]) => {
  const p = POSES[pose], off = at(bx, 0, bz).map((v, i) => v + p.lift[i]);
  if (pose === 'worker_slumped') wall(pose + '_wall', at(bx, 0, bz));
  outliner.push(rig(pose, worker(), off, p));
});
[['res_slumped', 7, 3], ['res_fallen', 9, 3], ['res_reaching', 11, 3]].forEach(([pose, bx, bz]) => {
  const p = POSES[pose], off = at(bx, 0, bz).map((v, i) => v + p.lift[i]);
  outliner.push(rig(pose, resistance(pose === 'res_fallen'), off, p));
});
// row 3: decor
{
  const off = at(0, 0, 6);
  outliner.push(group('alcove', [8, 0, 8], off, [...put(named(alcove(), 'alcove_'), off), group('g_alcove_contact', [8, 24, 12], off, put(alcoveLit(), off))]));
}
G('statue', at(2, 0, 6), statue());
// the toppled statue: on its back along x, its head broken off and lying beside it
{
  const body = statue().filter((c) => !['hood', 'hood_peak', 'face_cut', 'face_plate', 'clamp_t', 'clamp_b'].includes(c.name));
  const head = statue().filter((c) => ['hood', 'hood_peak', 'face_cut', 'face_plate', 'clamp_t', 'clamp_b'].includes(c.name));
  const off = at(4, 0, 6);
  outliner.push(group('statue_toppled', [8, 8, 8], off, put(named(body, 'toppled_'), off)));
  REST.statue_toppled = [0, 0, -90];
  const hoff = at(7, 0, 6).map((v, i) => v + [2, -36.75, 0][i]);
  outliner.push(group('statue_head', [8, 39.75, 8], hoff, put(named(head, 'thead_'), hoff)));
  REST.statue_head = [0, 25, 90];
}
G('scrap_heap', at(10, 0, 6), scrapHeap());
// the item icons, standing as cards
G('icon_device', at(13, 0, 6), card('icon_device', 'icon_device'));
G('icon_scrap', at(14, 0, 6), card('icon_scrap', 'icon_scrap'));

// the alcove's contact plate lights with its district
clip('alcove_fed', 2, 'loop', {}, { g_alcove_contact: { scale: [[0, [0, 0, 0]], [1, [1, 1, 1]]] } });
write(OUT, outliner);
{
  const fs = require('fs'), bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
// the icons as PNGs at 16 and 32 for the review
{
  const fs = require('fs'), dir = path.resolve(__dirname, '../texture-reviews/clockwork-wastes-salvage');
  fs.mkdirSync(dir, { recursive: true });
  for (const k of ['icon_device', 'icon_scrap']) fs.writeFileSync(path.join(dir, k + '.png'), T.OWN[k]);
}
}
