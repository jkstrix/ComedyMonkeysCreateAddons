// Pointed Dripstone Generator generator -> design/models/pointed-dripstone-generator.bbmodel (Generic Model preview with clips).
// To design/briefs/pointed-dripstone-generator.md: the Cobble Generators' process-box shell (andesite trim) round a little
// dripstone cave: water standing on a dripstone ceiling, four seed stalactites in a 2 x 2 square that take turns growing a
// tip, and a trip hammer on a spindle that ticks a quarter turn to each in turn and cracks the grown tip off into the front
// tray. Built in the game's model space: window north, shaft south. Shell, window, lamp and wireless top as
// cobble-generator.gen.js (copied, not required: requiring it rewrites the cobble generator's preview).
const path = require('path');
const { P, C, hash, mot, edge, FIELD, HI, LO, textureSet, painters, preview, SHOW, HIDE, smooth, lerp, trayItem, shaftStub } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'pointed-dripstone-generator.bbmodel');
const N = 'pointed_dripstone_generator';
const LAMP = { x0: 2.25, x1: 3.75, y0: 14.25, y1: 15.75 }; // the family lamp, front frame, top right as you face it

// ---------- the cave's layout ----------
const SLAB = { y0: 10.5, y1: 12 }; // the dripstone ceiling
const WATER = { y0: 12, y1: 13.75 }; // standing on it; the renderer sets its height from the tank
const SEED = { y0: 7.3, y1: 10.5 }; // each seed stalactite (vanilla's down frustum, scaled 0.2)
const TIP = { y0: 4.1, y1: 7.3 }; // the tip it grows (vanilla's down tip, scaled 0.2)
const SPIN = { x: 8, z: 7.5 }; // the hammer's spindle
// the four stalactites, in the order the hammer visits them, with the spindle angle (Blockbench y rotation) that points
// the arm (built along +x) at each
const STALACTITES = [
  { x: 11, z: 4.5, yaw: 45 },
  { x: 11, z: 10.5, yaw: -45 },
  { x: 5, z: 10.5, yaw: -135 },
  { x: 5, z: 4.5, yaw: -225 },
];
const REACH = Math.hypot(3, 3); // spindle to stalactite
const HEAD = { y0: 5.2, y1: 6.7, w: 1.5 }; // the hammer head, at the end of the arm
const ARM_Y = [5.65, 6.25];

// ---------- textures ----------
const T = textureSet(); T.file = '';
T.paint(`${N}_front`, painters.front('andesite'));
// sides: the water port, a round collar in the middle (8, 8) on the grid: a 2 x 2 hole in a 4 x 4 collar, corners off
const port = (x, y) => {
  if (x < 6 || x > 9 || y < 6 || y > 9) return null;
  if ((x === 6 || x === 9) && (y === 6 || y === 9)) return null;
  if (x >= 7 && x <= 8 && y >= 7 && y <= 8) return '#1f2327';
  return x + y < 15 ? '#7c7f79' : '#6b6e6a';
};
T.paint(`${N}_side`, painters.side('andesite', port));
T.paint(`${N}_top`, (x, y) => {
  const e = edge(x, y);
  if (e === 0) return mot('andesite', x, y, 41);
  if (e === 1) return '#2e2118';
  return y % 4 === 1 ? '#2e2118' : mot('walnut', x + (y >> 2) * 5, y, 42);
});
T.paint(`${N}_back`, painters.back('andesite'));
T.paint(`${N}_bottom`, painters.bottom('andesite'));
T.paint(`${N}_collar`, painters.andesite());
// the trip hammer: a dark steel spindle; the head and arm in the Cobble Generators' drill steel (user, 2026-11-10:
// "lets change the hammer rather than the drill, we are in create after all", after trying vanilla iron):
// cobble_generator_drill's steelLight #4a5059 #555b63 #5f656e with its bright edge #6b7178, without the drill's spiral
// flutes; the striking faces in the bright edge shade, worn by the knocks; the arm in the darker shades
T.paint(`${N}_spindle`, (x, y) => (edge(x, y) === 0 ? '#2f343b' : mot('steel', x, y, 61)));
T.paint(`${N}_hammer`, (x, y) => (hash(x, y, 65) > 0.88 ? '#6b7178' : mot('steelLight', x, y, 66)));
T.paint(`${N}_hammer_face`, (x, y) => (hash(x, y, 67) > 0.7 ? '#5f656e' : '#6b7178'));
T.paint(`${N}_arm`, (x, y) => (hash(x, y, 68) > 0.75 ? '#4a5059' : '#555b63'));
// preview only: a stand-in for vanilla's animated water (the game draws the real one)
T.paint('preview_water', (x, y) => (hash(x, y, 53) > 0.8 ? '#2f5f9e' : hash(x, y, 54) > 0.85 ? '#4f7fbe' : '#3f6fae'));

const pd = (k) => 'pd:' + k;
const own = (k) => 'comedymonkeysaddons:block/' + k; // the family's shared textures, already in the mod
const DRIP = 'minecraft:block/dripstone_block';

// ---------- the block model ----------
function shell(wireless = false) {
  const S = { north: pd(`${N}_front`), east: pd(`${N}_side`), west: pd(`${N}_side`), south: pd(`${N}_back`) };
  const wall = own('cobble_generator_chamber');
  const out = [
    C('top', [0, 14, 0], [16, 16, 16], { ...S, up: pd(`${N}_top`), down: own('cobble_generator_ceiling') }),
    C('bottom', [0, 0, 0], [16, 2, 16], { ...S, down: pd(`${N}_bottom`), up: own('cobble_generator_floor') }),
    C('wall_west', [0, 2, 0], [2, 14, 16], { north: S.north, west: S.west, south: S.south, east: wall, up: null, down: null }),
    C('wall_east', [14, 2, 0], [16, 14, 16], { north: S.north, east: S.east, south: S.south, west: wall, up: null, down: null }),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: S.south, north: wall, east: null, west: null, up: null, down: null }),
    C('glass', [2, 2, 1.2], [14, 14, 1.2], { north: own('cobble_generator_glass'), south: own('cobble_generator_glass'), all: null }, { shade: false }),
    C('lamp', [LAMP.x0, LAMP.y0, -0.25], [LAMP.x1, LAMP.y1, 0], { north: own('cobble_generator_lamp'), all: own('cobble_generator_steel'), south: null }, { uv: { north: [0, 0, 16, 16] } }),
    // the dripstone ceiling the water stands on
    C('dripstone_ceiling', [2, SLAB.y0, 2], [14, SLAB.y1, 14], { north: DRIP, down: DRIP, up: DRIP, all: null }),
  ];
  // the water ports' raised collars, a half pixel proud of each side: four bars round the 2 x 2 hole, corners off
  for (const [side, x0, x1] of [['west', -0.5, 0], ['east', 16, 16.5]]) {
    const f = { [side]: pd(`${N}_collar`), north: pd(`${N}_collar`), south: pd(`${N}_collar`), up: pd(`${N}_collar`), down: pd(`${N}_collar`), all: null };
    out.push(C(`port_${side}_top`, [x0, 9, 7], [x1, 10, 9], f));
    out.push(C(`port_${side}_bottom`, [x0, 6, 7], [x1, 7, 9], f));
    out.push(C(`port_${side}_front`, [x0, 7, 6], [x1, 9, 7], f));
    out.push(C(`port_${side}_back`, [x0, 7, 9], [x1, 9, 10], f));
  }
  if (wireless) out.push(...wirelessTop(wireless === 'powered'));
  return out;
}
// wireless: Create's Redstone Link antenna and the two frequency pads, exactly as the Cobble Generators
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
// a vanilla pointed dripstone piece as the game's cross: two planes at 45 degrees, scaled to the box
function cross(name, sx, sz, y0, y1, tex) {
  const w = (y1 - y0) / 2; // vanilla's sprite is drawn on a full-width plane; the spike itself is the middle of it
  const o = [sx, (y0 + y1) / 2, sz];
  return [
    C(`${name}_a`, [sx - w, y0, sz], [sx + w, y1, sz], { north: tex, south: tex, all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, rot: ['y', 45, o], shade: false }),
    C(`${name}_b`, [sx, y0, sz - w], [sx, y1, sz + w], { east: tex, west: tex, all: null }, { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] }, rot: ['y', 45, o], shade: false }),
  ];
}
const seed = (s) => cross('seed', s.x, s.z, SEED.y0, SEED.y1, 'minecraft:block/pointed_dripstone_down_frustum');
const tip = (s) => cross('tip', s.x, s.z, TIP.y0, TIP.y1, 'minecraft:block/pointed_dripstone_down_tip');
// the spindle, a collar where it stands, a cap; the arm and head are built pointing along +x
function spindle() {
  const h = pd(`${N}_spindle`), col = pd(`${N}_collar`);
  return [
    C('spindle_collar', [SPIN.x - 1, 2, SPIN.z - 1], [SPIN.x + 1, 2.75, SPIN.z + 1], { all: col, down: null }),
    C('spindle', [SPIN.x - 0.75, 2.75, SPIN.z - 0.75], [SPIN.x + 0.75, 6.4, SPIN.z + 0.75], { all: h, down: null }),
    C('spindle_cap', [SPIN.x - 0.5, 6.4, SPIN.z - 0.5], [SPIN.x + 0.5, 6.9, SPIN.z + 0.5], { all: col, down: null }),
  ];
}
function arm() {
  const hx = SPIN.x + REACH;
  return [
    C('arm', [SPIN.x + 0.75, ARM_Y[0], SPIN.z - 0.3], [hx - HEAD.w / 2, ARM_Y[1], SPIN.z + 0.3], { all: pd(`${N}_arm`), west: null }),
    // the head, its two striking faces (across the arm's swing) a shade brighter, worn by the knocks
    C('head', [hx - HEAD.w / 2, HEAD.y0, SPIN.z - HEAD.w / 2], [hx + HEAD.w / 2, HEAD.y1, SPIN.z + HEAD.w / 2],
      { all: pd(`${N}_hammer`), north: pd(`${N}_hammer_face`), south: pd(`${N}_hammer_face`) }),
  ];
}
const lampLit = (state) => [C('lamp_lit', [LAMP.x0, LAMP.y0, -0.5], [LAMP.x1, LAMP.y1, -0.5], { north: own(`cobble_generator_lamp_${state}`), all: null }, { uv: { north: [0, 0, 16, 16] } })];
const water = () => [C('water', [2, WATER.y0, 2], [14, WATER.y1, 14], { north: pd('preview_water'), up: pd('preview_water'), all: null })];
const TRAY_X = [5.5, 8, 10.5];
const tray = (i) => trayItem(i, TRAY_X[i], 'minecraft:item/pointed_dripstone');

// ---------- preview ----------
const { cube, group, clip, write } = preview('pointed-dripstone-generator', { 'pd:': T });
const cubes = (list, off) => list.map((k) => cube(k, off));
const outliner = [];
const scenes = [];
function scene(name, off, state, wireless = false) {
  const kids = [
    group(`${name}_shell`, [8, 0, 8], off, cubes([...shell(wireless), ...shaftStub()], off)),
    group(`${name}_water`, [8, WATER.y0, 8], off, cubes(water(), off)),
    ...STALACTITES.map((s, i) => group(`${name}_seed${i}`, [s.x, SEED.y1, s.z], off, cubes(seed(s), off))),
    ...STALACTITES.map((s, i) => group(`${name}_tip${i}`, [s.x, TIP.y1, s.z], off,
      [group(`${name}_fall${i}`, [s.x, (TIP.y0 + TIP.y1) / 2, s.z], off, cubes(tip(s), off))])),
    group(`${name}_hammer`, [SPIN.x, 2, SPIN.z], off, [...cubes(spindle(), off),
      group(`${name}_arm`, [SPIN.x, (ARM_Y[0] + ARM_Y[1]) / 2, SPIN.z], off, cubes(arm(), off))]),
    ...[0, 1, 2].map((i) => group(`${name}_tray${i}`, [TRAY_X[i], 2.5, 2.2], off, cubes(tray(i), off))),
    ...['running', 'full', 'off'].map((s) => group(`${name}_lamp_${s}`, [3, 15, -0.5], off, cubes(lampLit(s), off))),
  ];
  outliner.push(group(name, [8, 0, 8], off, kids));
  scenes.push({ name, state });
}
// row 1: running, and the Wireless one; row 2 (in front): one per stopped state
scene('running', [0, 0, 0], 'running');
scene('wireless', [24, 0, 0], 'running', true);
scene('full', [0, 0, -32], 'full');
scene('off', [24, 0, -32], 'off');
scene('dry', [48, 0, -32], 'dry');
scene('three_seeds', [72, 0, -32], 'three');
scene('stopped', [96, 0, -32], 'stopped');

// ---------- the cycle ----------
// One cycle makes four (user, 2026-11-10: "lets have them all grow at the same time then the hammer moves to each one
// with a view of doubling the output"), L ticks (400 x 256 / RPM, so one every 5 s at 256 RPM): all four tips grow
// together, then the hammer goes round them: for each, the spindle ticks a quarter turn to it (10 ticks, a small
// overshoot) and knocks it off (30 ticks): wind-up 10, hold 3, whack 2, the hit (the piece counts here), the tip
// shudders 3 and drops 6 with a bounce, the hammer recoils 6 and settles 6. The round of knocks (160 ticks) is the same
// at any speed; only the growing slows.
const KNOCK = 30, INDEX = 10, ROUND = 4 * (INDEX + KNOCK);
const REST = 30, WOUND = 60, CONTACT = 18, RECOIL = 36; // hammer yaw offsets from the stalactite (degrees)
const easeOut = (u) => 1 - (1 - u) * (1 - u);
const backOut = (u) => { const c1 = 1.2, c3 = c1 + 1; return 1 + c3 * Math.pow(u - 1, 3) + c1 * Math.pow(u - 1, 2); };
const clamp01 = (u) => Math.max(0, Math.min(1, u));
function knockOffset(k) {
  if (k < 10) return lerp(REST, WOUND, easeOut(k / 10));
  if (k < 13) return WOUND;
  if (k < 15) return lerp(WOUND, CONTACT, (k - 13) / 2);
  if (k < 21) return lerp(CONTACT, RECOIL, easeOut((k - 15) / 6));
  if (k < 27) return lerp(RECOIL, REST, smooth((k - 21) / 6));
  return REST;
}
function knockTilt(k) {
  if (k < 10) return lerp(0, 12, easeOut(k / 10));
  if (k < 13) return 12;
  if (k < 15) return lerp(12, -4, (k - 13) / 2);
  if (k < 21) return lerp(-4, 0, smooth((k - 15) / 6));
  return 0;
}
// a knocked tip, kk ticks into its knock: whole until the hit, then it shudders, drops tumbling towards the window with
// a bounce, slides forward into the tray and joins it (gone)
const TIP_REST = { scale: 1, pos: [0, 0, 0], rot: [0, 0, 0] }, TIP_GONE = { scale: 0, pos: [0, 0, 0], rot: [0, 0, 0] };
function knocked(s, kk) {
  if (kk < 15) return TIP_REST;
  if (kk >= 29) return TIP_GONE;
  const shake = kk < 18 ? (Math.floor(kk) % 2 ? 0.35 : -0.35) : 0;
  const f = clamp01((kk - 18) / 6), drop = -3.1 * f * f;
  const bounce = kk > 24 && kk < 27 ? 0.4 * Math.sin(Math.PI * (kk - 24) / 3) : 0;
  const g = smooth(clamp01((kk - 24) / 5)), slot = TRAY_X.reduce((a, b) => (Math.abs(b - s.x) < Math.abs(a - s.x) ? b : a));
  return { scale: 1, pos: [shake + (slot - s.x) * g, drop + bounce, (3.6 - s.z) * g], rot: [-80 * smooth(f), 0, 15 * f] };
}
// the whole machine at a tick (fractional), for cycles of L ticks (L > ROUND)
function at(tick, L) {
  const c = Math.floor(tick / L), k = tick - c * L, G = L - ROUND;
  // the spindle: unwrapped angles, so it always turns the same way; it waits by the last stalactite while they grow
  const base = (n) => STALACTITES[0].yaw - 90 * n;
  // straight after the last knock it ticks round to the first stalactite while the new tips are still tiny (review: a
  // later sweep past the fourth cut through its regrown tip), then waits there
  let yaw = lerp(base(4 * c - 1), base(4 * c), backOut(clamp01(k / INDEX))) + REST, tilt = 0;
  // which knock of the round (0-3), and how far into its quarter turn and knock
  const r = k - G, j = r < 0 ? -1 : Math.floor(r / (INDEX + KNOCK)), q = r - j * (INDEX + KNOCK);
  if (j >= 0) {
    const n = 4 * c + j;
    if (q < INDEX) yaw = (j === 0 ? base(n) : lerp(base(n - 1), base(n), backOut(clamp01(q / INDEX)))) + REST;
    else {
      yaw = base(n) + knockOffset(q - INDEX);
      tilt = knockTilt(q - INDEX);
    }
  }
  const tips = STALACTITES.map((s, i) => {
    if (j < 0) return { scale: clamp01(k / G), pos: [0, 0, 0], rot: [0, 0, 0] };
    if (i > j) return TIP_REST;
    if (i < j) return TIP_GONE;
    return q < INDEX ? TIP_REST : knocked(s, q - INDEX);
  });
  // pieces in the tray: one more as each knocked tip slides in (the piece itself counts at the hit), up to three
  const done = 4 * c + (j < 0 ? 0 : j + (q - INDEX >= 29 ? 1 : 0));
  return { yaw, tilt, tips, tray: Math.min(3, done) };
}
// a frozen pose for the stopped scenes: all four tips half grown, the hammer at rest
const frozen = { yaw: STALACTITES[3].yaw + REST, tilt: 0, tips: STALACTITES.map(() => ({ scale: 0.5, pos: [0, 0, 0], rot: [0, 0, 0] })), tray: 2 };
const full = { ...frozen, tips: STALACTITES.map(() => ({ scale: 1, pos: [0, 0, 0], rot: [0, 0, 0] })), tray: 3 };

function makeClip(cname, length, timeToTick, L) {
  const tracks = {}, steps = {};
  for (const sc of scenes) {
    const running = sc.state === 'running';
    const pose = (t) => (running ? at(timeToTick(t), L) : sc.state === 'full' ? full : frozen);
    const lamp = { running: 'running', full: 'full', off: 'off', dry: 'off', three: 'off', stopped: null }[sc.state];
    for (const s of ['running', 'full', 'off']) steps[`${sc.name}_lamp_${s}`] = { scale: [[0, s === lamp ? SHOW : HIDE]] };
    // an empty tank draws no water at all (a sliver on the ceiling would z-fight with it)
    steps[`${sc.name}_water`] = { scale: [[0, sc.state === 'dry' ? HIDE : SHOW]] };
    steps[`${sc.name}_seed3`] = { scale: [[0, sc.state === 'three' ? HIDE : SHOW]] };
    tracks[`${sc.name}_hammer`] = { rotation: (t) => [0, pose(t).yaw, 0] };
    tracks[`${sc.name}_arm`] = { rotation: (t) => [0, 0, pose(t).tilt] };
    STALACTITES.forEach((_, j) => {
      tracks[`${sc.name}_tip${j}`] = { scale: (t) => { const v = Math.max(0.001, pose(t).tips[j].scale); return [v, v, v]; } };
      tracks[`${sc.name}_fall${j}`] = { position: (t) => pose(t).tips[j].pos, rotation: (t) => pose(t).tips[j].rot };
    });
    [0, 1, 2].forEach((n) => { tracks[`${sc.name}_tray${n}`] = { scale: (t) => (pose(t).tray > n ? SHOW : HIDE) }; });
  }
  clip(cname, length, 'loop', tracks, steps);
}
// at 256 RPM, real time: 20 s a cycle, four pieces (12 s growing, 8 s knocking)
makeClip('running_256rpm', 20, (t) => t * 20, 400);
// a quick look: 2 s of growing, then the round of knocks
makeClip('demo_fast', 10, (t) => t * 20, 200);
// the first knock alone at quarter speed (from just before its quarter turn)
makeClip('knock_slowmo', 8, (t) => 38 + t * 5, 200);
write(OUT, outliner);

module.exports = { T, N, shell, seed, tip, spindle, arm, lampLit, water, tray, STALACTITES, SPIN, REACH, HEAD, SLAB, WATER, SEED, TIP, LAMP, at };
