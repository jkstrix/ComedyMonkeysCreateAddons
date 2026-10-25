// Cobble Generators redesign generator -> design/models/cobble-generator.bbmodel (Generic Model preview with clips).
// To design/briefs/cobble-generator.md: the process-box steel shell (andesite or dark aged-brass trim) round the kept
// lava-meets-water chamber, a drill that jabs down on each drill hit (a wider four-bit head over a 2 x 2 batch on
// brass), and a status lamp on the front frame. Built in the game's model space: window north, shaft south.
// Renderer geometry kept (CobbleGeneratorRenderer.java:81-108): floor y 2, pools, the forming block x 6.5-9.5 z 5-8,
// pours x 3.5-4.5 / 11.5-12.5 at z 6.5-7.5 from y 3 to y 10.
const path = require('path');
const { P, C, hash, mot, edge, FIELD, HI, LO, SEAM, textureSet, painters, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'cobble-generator.bbmodel');
const PRODUCTS = ['cobble', 'basalt', 'blackstone', 'tuff'];
const ICE = { basalt: true, blackstone: true };
// what the chamber is lined with, per product (vanilla blocks, borrowed by reference)
const CHAMBER = {
  cobble: { wall: 'cg:cobble_generator_chamber', floor: 'cg:cobble_generator_floor', ceiling: 'cg:cobble_generator_ceiling' },
  basalt: { wall: 'minecraft:block/smooth_basalt', floor: 'minecraft:block/soul_soil', ceiling: 'minecraft:block/smooth_basalt' },
  blackstone: { wall: 'minecraft:block/polished_blackstone_bricks', floor: 'minecraft:block/polished_blackstone', ceiling: 'minecraft:block/polished_blackstone' },
  tuff: { wall: 'minecraft:block/tuff_bricks', floor: 'minecraft:block/gravel', ceiling: 'minecraft:block/polished_tuff' },
};
const SPRITE = { cobble: 'minecraft:block/cobblestone', basalt: 'minecraft:block/basalt_side', blackstone: 'minecraft:block/blackstone', tuff: 'minecraft:block/tuff' };
// file name of a tier and product's textures (tuff has no tier prefix, as today)
const id = (tier, product) => (product === 'tuff' ? 'tuff_generator' : `${tier}_${product}_generator`);
const LAMP = { x0: 2.25, x1: 3.75, y0: 14.25, y1: 15.75 }; // on the front frame, top right as you face it

// ---------- textures (all exported under these exact names) ----------
const T = textureSet(); T.file = '';
// the brass tier: a warm mid brass round the house brass (the pens' darker aged brass read as "a little grim" here),
// still under the house highlight cap
P.brassTrim = ['#7e6832', '#8f7638', '#a3873f'];
const TRIM = { andesite: 'andesite', brass: 'brassTrim' };
// each product's trim line along the bottom of the front frame (kept small: the ponder's "ice-blue trim" and
// "black and gold")
const PRODUCT_LINE = { cobble: null, basalt: ['#4f7488', '#5f8aa0'], blackstone: ['#6e5a2a', '#8f7638'], tuff: ['#5a6450', '#6f7a62'] };
function front(tier, product) {
  // the outer ring is always the tier's metal; the product only adds its accent
  const base = painters.front(TRIM[tier]);
  const line = PRODUCT_LINE[product];
  return (x, y) => {
    // blackstone: a near-black inner ring with gold rivets inside the tier's frame ("all black and gold")
    if (product === 'blackstone' && edge(x, y) === 1) return (x === 1 || x === 14) && (y === 1 || y === 14) ? '#8f7638' : '#1f1c1d';
    if (line && y === 14 && x >= 2 && x <= 13) return line[(x + 1) % 3 === 0 ? 1 : 0];
    return base(x, y);
  };
}
// sides: a riveted inspection plate in the steel field (no glowing vents)
const plate = (x, y) => {
  if (x < 5 || x > 10 || y < 5 || y > 10) return null;
  if (x === 5 || y === 5) return HI;
  if (x === 10 || y === 10) return LO;
  if ((x === 6 || x === 9) && (y === 6 || y === 9)) return '#5f656e';
  return hash(x, y, 40) > 0.86 ? '#4a5059' : '#454b53';
};
for (const tier of ['andesite', 'brass']) for (const product of PRODUCTS) {
  if (product === 'tuff' && tier === 'brass') continue;
  const n = id(tier, product), trim = TRIM[tier];
  T.paint(`${n}_front`, front(tier, product));
  T.paint(`${n}_side`, painters.side(trim, plate));
  // top: andesite tier boards in an andesite frame like Create's casing; brass tier walnut in dark brass (as the pens)
  T.paint(`${n}_top`, (x, y) => {
    const e = edge(x, y);
    if (e === 0) return mot(trim, x, y, 41);
    if (e === 1) return '#2e2118';
    return y % 4 === 1 ? '#2e2118' : mot('walnut', x + (y >> 2) * 5, y, 42);
  });
}
for (const tier of ['andesite', 'brass']) {
  T.paint(`${tier}_cobble_generator_bottom`, painters.bottom(TRIM[tier]));
  T.paint(`${tier}_cobble_generator_back`, painters.back(TRIM[tier], tier === 'brass' ? '#9c8240' : undefined));
}
// the shared interior: a dark chamber with depth, a tread floor, a plain ceiling; steel nozzles; grey glass glints
T.paint('cobble_generator_chamber', painters.chamber());
T.paint('cobble_generator_floor', painters.floor());
T.paint('cobble_generator_ceiling', painters.ceiling());
T.paint('cobble_generator_pipe', (x, y) => (x % 4 === 0 ? '#2f343b' : mot('steelLight', x, y, 43)));
T.paint('cobble_generator_glass', painters.glass());
T.paint('cobble_generator_steel', painters.steel());
T.paint('cobble_generator_steel_light', painters.steelLight());
T.paint('cobble_generator_dark', painters.dark());
// the drill: steel flutes winding round the bit, a brighter cutting edge
T.paint('cobble_generator_drill', (x, y) => ((x + y) % 4 === 0 ? '#2a2f35' : (x + y) % 4 === 1 ? '#6b7178' : mot('steelLight', x, y, 44)));
T.paint('cobble_generator_drill_collar', (x, y) => mot('andesite', x, y, 45));
T.paint('cobble_generator_drill_collar_brass', (x, y) => (hash(x, y, 46) > 0.9 ? '#b08f45' : mot('brassTrim', x, y, 47)));
// the status lamp: a dark lens (the static model) and its three lit colours (drawn full-bright by the renderer)
const lens = (c) => (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r > 7.6) return null; if (r > 5.5) return c[0]; return x + y < 12 ? c[2] : c[1]; };
T.paint('cobble_generator_lamp', lens(['#1f2327', '#2a2f35', '#3c424a']));
T.paint('cobble_generator_lamp_running', lens(['#3a6f7a', '#6fb7c4', '#8fc8d1']));
T.paint('cobble_generator_lamp_full', lens(['#8a5a2a', '#d99a4a', '#e0ab62']));
T.paint('cobble_generator_lamp_off', lens(['#5a1f17', '#8a2f22', '#a2402f']));
// preview only: stand-ins for vanilla's animated lava and water (the game draws the real ones)
T.paint('preview_lava', (x, y) => (hash(x, y, 51) > 0.8 ? '#b85e26' : hash(x, y, 52) > 0.85 ? '#d99a4a' : '#c76a2a'));
T.paint('preview_water', (x, y) => (hash(x, y, 53) > 0.8 ? '#2f5f9e' : hash(x, y, 54) > 0.85 ? '#4f7fbe' : '#3f6fae'));

// ---------- the block model (per tier and product) ----------
const cg = (k) => 'cg:' + k;
function shell(tier, product, wireless = false) {
  const n = id(tier, product), ch = CHAMBER[product];
  const S = { north: cg(`${n}_front`), east: cg(`${n}_side`), west: cg(`${n}_side`), south: cg(`${tier}_cobble_generator_back`) };
  const out = [
    C('top', [0, 14, 0], [16, 16, 16], { ...S, up: cg(`${n}_top`), down: ch.ceiling }),
    C('bottom', [0, 0, 0], [16, 2, 16], { ...S, down: cg(`${tier}_cobble_generator_bottom`), up: ch.floor }),
    C('wall_west', [0, 2, 0], [2, 14, 16], { north: S.north, west: S.west, south: S.south, east: ch.wall, up: null, down: null }),
    C('wall_east', [14, 2, 0], [16, 14, 16], { north: S.north, east: S.east, south: S.south, west: ch.wall, up: null, down: null }),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: S.south, north: ch.wall, east: null, west: null, up: null, down: null }),
    C('glass', [2, 2, 1.2], [14, 14, 1.2], { north: cg('cobble_generator_glass'), south: cg('cobble_generator_glass'), all: null }, { shade: false }),
    // the status lamp's dark lens, a quarter pixel proud of the frame
    C('lamp', [LAMP.x0, LAMP.y0, -0.25], [LAMP.x1, LAMP.y1, 0], { north: cg('cobble_generator_lamp'), all: cg('cobble_generator_steel'), south: null }, { uv: { north: [0, 0, 16, 16] } }),
    // the drill's mount: a steel bracket under the ceiling with a sleeve the drill's rod runs in
    C('drill_mount', [6, 13.25, 4.5], [10, 14, 8.5], { all: cg('cobble_generator_steel'), up: null }),
    C('drill_sleeve', [7.25, 12.25, 5.75], [8.75, 13.25, 7.25], { all: cg('cobble_generator_steel_light'), up: null }),
  ];
  // nozzles over the pours: steel pipes down to y 10 with a flared tip; no water nozzle over blue ice
  for (const [name, x] of [['lava', 4], ...(ICE[product] ? [] : [['water', 12]])]) {
    out.push(C(`nozzle_${name}`, [x - 0.75, 10.75, 6.25], [x + 0.75, 14, 7.75], { all: cg('cobble_generator_pipe'), up: null }));
    out.push(C(`nozzle_${name}_tip`, [x - 1, 10, 6], [x + 1, 10.75, 8], { all: cg('cobble_generator_steel_light'), down: cg('cobble_generator_dark') }));
  }
  if (wireless) out.push(...wirelessTop(wireless === 'powered'));
  return out;
}
// wireless: Create's Redstone Link antenna at the back corner and the two frequency pads on the front half (TopFrequencySlot)
// (the same elements as before the redesign: Create's own crossed antenna planes, its plate, and the pads' bridge UVs)
function wirelessTop(powered = false) {
  const ant = powered ? 'create:block/redstone_antenna_powered' : 'create:block/redstone_antenna', br = 'create:block/redstone_bridge';
  const side = [11, 2, 13, 2.5];
  return [
    C('antenna_a', [11, 16, 12], [14, 26, 13], { north: ant, south: ant, down: ant, all: null }, { uv: { north: [0, 0, 3, 10], south: [0, 0, 3, 10], down: [0, 9, 3, 10] } }),
    C('antenna_b', [12, 16, 11], [13, 26, 14], { east: ant, west: ant, all: null }, { uv: { east: [0, 0, 3, 10], west: [0, 0, 3, 10] } }),
    C('antenna_tip', [12, 24, 12], [13, 25, 13], { up: ant, all: null }, { uv: { up: [1, 1, 2, 2] } }),
    C('antenna_plate', [10, 22, 10], [15, 22, 15], { up: ant, down: ant, all: null }, { uv: { up: [4, 0, 9, 5], down: [4, 0, 9, 5] } }),
    C('pad_red', [3.5, 16, 3.5], [7.5, 17, 7.5], { all: br, down: null }, { uv: { up: [11, 0, 13, 2], north: side, south: side, east: side, west: side } }),
    C('pad_blue', [8.5, 16, 3.5], [12.5, 17, 7.5], { all: br, down: null }, { uv: { up: [13, 0, 15, 2], north: side, south: side, east: side, west: side } }),
  ];
}

// ---------- renderer-drawn parts ----------
// the drill, pointing down with its tip at y 0, spinning about the vertical axis through x 8, z 6.5
const DRILL = { x: 8, z: 6.5 };
function bit(cx, cz, s = 1) {
  const d = (w, y0, y1, name, tex) => C(name, [cx - w / 2, y0, cz - w / 2], [cx + w / 2, y1, cz + w / 2], { all: tex });
  return [
    d(0.75 * s, 0, 0.75, 'tip', cg('cobble_generator_drill')),
    d(1.5 * s, 0.75, 1.75, 'mid', cg('cobble_generator_drill')),
    d(2.25 * s, 1.75, 2.75, 'top', cg('cobble_generator_drill')),
  ];
}
function drill(brass) {
  const collar = cg(brass ? 'cobble_generator_drill_collar_brass' : 'cobble_generator_drill_collar');
  if (!brass) return [...bit(DRILL.x, DRILL.z), C('collar', [DRILL.x - 1.5, 2.75, DRILL.z - 1.5], [DRILL.x + 1.5, 3.5, DRILL.z + 1.5], { all: collar })];
  // brass: a cross plate carrying four smaller bits over the 2 x 2 batch
  const out = [];
  for (const [dx, dz] of [[-1.25, -1.25], [1.25, -1.25], [-1.25, 1.25], [1.25, 1.25]]) out.push(...bit(DRILL.x + dx, DRILL.z + dz, 0.7).map((c) => ({ ...c, name: c.name + `_${dx}_${dz}` })));
  out.push(C('plate', [DRILL.x - 2.75, 2.75, DRILL.z - 2.75], [DRILL.x + 2.75, 3.25, DRILL.z + 2.75], { all: cg('cobble_generator_steel_light'), down: cg('cobble_generator_steel') }));
  out.push(C('collar', [DRILL.x - 1.25, 3.25, DRILL.z - 1.25], [DRILL.x + 1.25, 3.75, DRILL.z + 1.25], { all: collar }));
  return out;
}
// the drill's rod, one pixel tall: the renderer stretches it from the drill's collar up into the sleeve
const rod = () => [C('rod', [DRILL.x - 0.5, 0, DRILL.z - 0.5], [DRILL.x + 0.5, 1, DRILL.z + 0.5], { all: cg('cobble_generator_steel_light'), up: null, down: null })];
// the lamp's lit lens, just in front of the dark one
const lampLit = (state) => [C('lamp_lit', [LAMP.x0, LAMP.y0, -0.5], [LAMP.x1, LAMP.y1, -0.5], { north: cg(`cobble_generator_lamp_${state}`), all: null }, { uv: { north: [0, 0, 16, 16] } })];
// the renderer's own interior, for the preview (pools, ice, pours, the forming block)
function interior(product) {
  const lava = cg('preview_lava'), water = cg('preview_water');
  const out = [C('lava_pool', [2, 2, 2], [6, 3, 13], { up: lava, north: lava, east: lava, all: null })];
  if (ICE[product]) out.push(C('ice', [10, 2, 4], [14, 6, 9], { all: 'minecraft:block/blue_ice', down: null }));
  else out.push(C('water_pool', [10, 2, 2], [14, 3, 13], { up: water, north: water, west: water, all: null }));
  return out;
}
const pours = (product) => [C('lava_pour', [3.5, 3, 6.5], [4.5, 10, 7.5], { all: cg('preview_lava'), up: null, down: null }),
  ...(ICE[product] ? [] : [C('water_pour', [11.5, 3, 6.5], [12.5, 10, 7.5], { all: cg('preview_water'), up: null, down: null })])];
// the forming block(s), modelled full size and scaled up from the floor by the clips
const BATCH = [[5.5, 4.25], [8.25, 4.25], [5.5, 7], [8.25, 7]];
const product = (p, brass) => (brass
  ? BATCH.map(([x, z], i) => C(`block_${i}`, [x, 2, z], [x + 2.25, 4.25, z + 2.25], { all: SPRITE[p], down: null }))
  : [C('block', [6.5, 2, 5], [9.5, 5, 8], { all: SPRITE[p], down: null })]);

// ---------- preview ----------
const { cube, group, clip, write } = preview('cobble-generator', { 'cg:': T });
const cubes = (list, off) => list.map((k) => cube(k, off));
const outliner = [];
const scenes = [];
// a scene: one generator with its interior, drill and lamps; state picks what the clips do with it
function scene(name, tier, p, off, state, wireless = false) {
  const brass = tier === 'brass';
  const kids = [
    group(`${name}_shell`, [8, 0, 8], off, cubes([...shell(tier, p, wireless), ...interior(p)], off)),
    group(`${name}_pours`, [8, 0, 8], off, cubes(pours(p), off)),
    group(`${name}_block`, [8, 2, 6.5], off, cubes(product(p, brass), off)),
    group(`${name}_drill`, [DRILL.x, 0, DRILL.z], off, [group(`${name}_bit`, [DRILL.x, 0, DRILL.z], off, cubes(drill(brass), off)),
      group(`${name}_rod`, [DRILL.x, 3.5, DRILL.z], off, cubes(rod().map((r) => ({ ...r, from: [r.from[0], 3.5, r.from[2]], to: [r.to[0], 4.5, r.to[2]] })), off))]),
    ...['running', 'full', 'off'].map((s) => group(`${name}_lamp_${s}`, [3, 15, -0.5], off, cubes(lampLit(s), off))),
  ];
  outliner.push(group(name, [8, 0, 8], off, kids));
  scenes.push({ name, state, brass, p });
}
// row 1, all running: the four products and the brass batch; row 2 (in front), one generator per state
scene('cobble', 'andesite', 'cobble', [0, 0, 0], 'running');
scene('cobble_brass', 'brass', 'cobble', [24, 0, 0], 'running');
scene('basalt', 'andesite', 'basalt', [48, 0, 0], 'running');
scene('blackstone', 'brass', 'blackstone', [72, 0, 0], 'running');
scene('tuff', 'andesite', 'tuff', [96, 0, 0], 'running', true);
scene('full', 'andesite', 'cobble', [0, 0, -32], 'full');
scene('off', 'brass', 'basalt', [24, 0, -32], 'off');
scene('stopped', 'andesite', 'blackstone', [48, 0, -32], 'stopped');
scene('wireless', 'brass', 'cobble', [72, 0, -32], 'running', true);

// The drill pace at 64 RPM (CobbleGeneratorBlockEntity.java:114-130): hits every (wait + 1) ticks, 10 per block;
// cobble/blackstone/tuff wait 3 ticks (hardness 2 or 1.5), basalt 1. The block grows a tenth per hit, the drill's tip
// rides its top and jabs 0.75 px down on each hit; idle, the drill waits raised.
const WAIT = { cobble: 3, basalt: 1, blackstone: 2, tuff: 2 };
const LEN = 4, FPS = 20, RAISED = 8.75, JAB = 0.75, JAB_TICKS = 3;
const HEIGHT = (brass) => (brass ? 2.25 : 3);
function sample(sc, tick) {
  const step = WAIT[sc.p] + 1, cycle = 9 * step + 1;
  const k = tick % cycle, hits = Math.min(10, Math.floor(k / step) + 1), since = k - (hits - 1) * step;
  return { frac: hits / 10, jab: since < JAB_TICKS ? JAB * Math.sin(Math.PI * (since + 1) / (JAB_TICKS + 1)) : 0 };
}
const tracks = {}, steps = {};
const spin = (t) => [0, (t * 20 * 64 * 6 * 0.25) % 360, 0]; // the drill turns with the shaft (64 RPM), a quarter speed in the preview so it reads
for (const sc of scenes) {
  const h = HEIGHT(sc.brass), running = sc.state === 'running';
  for (const s of ['running', 'full', 'off']) steps[`${sc.name}_lamp_${s}`] = { scale: [[0, s === sc.state ? SHOW : HIDE]] };
  steps[`${sc.name}_pours`] = { scale: [[0, running ? SHOW : HIDE]] };
  const frozen = sc.state === 'full' ? 1 : sc.state === 'off' ? 0.6 : 0.4;
  tracks[`${sc.name}_block`] = { scale: (t) => [1, running ? sample(sc, Math.floor(t * FPS)).frac : frozen, 1] };
  // the drill's tip: on the block's top (plus a quarter pixel) while running, jabbing; raised otherwise
  const tipAt = (t) => (running ? (() => { const s = sample(sc, Math.floor(t * FPS)); return 2 + h * s.frac + 0.25 - s.jab; })() : RAISED);
  tracks[`${sc.name}_drill`] = { position: (t) => [0, tipAt(t), 0] };
  tracks[`${sc.name}_bit`] = { rotation: (t) => (running ? spin(t) : [0, 0, 0]) };
  // the rod: from the collar (tip + 3.5) up to the sleeve (y 12.25), as a stretched 1 px piece
  tracks[`${sc.name}_rod`] = { scale: (t) => [1, Math.max(0.01, 12.25 - (tipAt(t) + 3.5)), 1] };
}
clip('running', LEN, 'loop', tracks, steps);
write(OUT, outliner);

module.exports = { T, PRODUCTS, ICE, SPRITE, id, shell, drill, rod, lampLit, interior, DRILL, LAMP, BATCH };
