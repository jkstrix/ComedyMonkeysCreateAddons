// Clockwork Dynamo generator -> design/models/clockwork-dynamo.bbmodel (Generic Model preview with clips).
// Built on the family kit in the game's model space (shaft out of the north face, z 0; base at z 16), to the brief
// design/briefs/clockwork-dynamo.md. Bot materials: andesite fields in a dark-steel stepped frame, a brass waist band
// with rivets, copper. The two block states come from one part list (only the lamp differs); the aerial and the
// commutator are renderer parts with their own pivots.
const path = require('path');
const { P, C, textureSet, mot, hash, edge, HI, LO, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'clockwork-dynamo.bbmodel');
const LAYER = 0.25; // overlays sit this far in front of what's behind them (zfight-check.js)

// ---------- textures (clockwork_dynamo_<key>) ----------
// the generators' mid brass: house brass reads too bright on blocks
P.midBrass = ['#7e6832', '#8f7638', '#a3873f'];
const T = textureSet(); T.file = 'clockwork_dynamo_';
// andesite field in the bots' panel shade, near flat so big panels stay crisp
T.paint('field', (x, y) => { const r = hash(x, y, 81); return r > 0.93 ? '#8a8c86' : r < 0.12 ? '#73766f' : '#7c7f79'; });
T.paint('steel', (x, y) => mot('steel', x, y, 82));
T.paint('dark', (x, y) => (hash(x, y, 83) > 0.85 ? '#24282d' : '#1f2327'));
T.paint('floor', (x, y) => (hash(x, y, 84) > 0.8 ? '#1a1d20' : '#16181b'));
// the Trade Bot's brass (as the Clockwork Tubes): his base pair, #b08f45 over #8f7638, no highlight on trims this size
T.paint('brass', (x, y) => (hash(x, y, 85) > 0.7 ? '#8f7638' : '#b08f45'));
T.paint('brass_edge', () => '#6e5a2a');
// the waist band: the Trade Bot's own (and the Clockwork Tubes'), brass with dark brass every other pixel along its
// length. Faces mapped across it (top, bottom) run along u in rows 0-1; faces mapped down it (sides) run along v
const TB_BRASS = ['#8f7638', '#b08f45', '#c9a75a'];
T.paint('band', (x, y) => {
  if ((y < 2 ? x : y) % 2 === 1) return '#6e5a2a';
  const r = hash(x, y, 93);
  return r < 0.18 ? TB_BRASS[0] : r < 0.84 ? TB_BRASS[1] : TB_BRASS[2];
});
T.paint('rim', (x, y) => mot('steelLight', x, y, 92));
T.paint('copper', (x, y) => mot('copper', x, y, 86));
T.paint('carbon', (x, y) => (hash(x, y, 87) > 0.7 ? '#323337' : '#26272a'));
// commutator bars, one texel a pixel: each bar one flat copper shade, alternate bars darker so the segments read, a
// highlight row across the lighter ones
T.paint('bar_a', (x, y) => (y === 5 ? '#b87a4e' : '#9c5f3a'));
T.paint('bar_b', (x, y) => (y === 5 ? '#8a5434' : '#7a4a2e'));
// the aerial's bead, in the Trade Bot's own brass, lit on top
T.paint('bead', () => '#b08f45');
T.paint('bead_top', () => '#c9a75a');
// the bearing boss on the front (8 x 8 px, mapped at 2 texels a pixel so the ring is round): steel, a brass bearing ring
// round a 6 px dark opening, a rivet in each corner
T.paint('boss', (u, v) => {
  const r = Math.hypot(u + 0.5 - 8, v + 0.5 - 8);
  if (r < 6.1) return r < 4.5 ? '#121417' : '#1a1d20';
  if (r < 7.7) return u + v < 15 ? '#a3873f' : '#7e6832';
  if ((u === 1 || u === 14) && (v === 1 || v === 14)) return '#5f656e';
  if (u === 0 || v === 0) return HI;
  if (u === 15 || v === 15) return LO;
  return mot('steel', u, v, 88);
});
// the base: a steel plate (x 1-15, y 2-14) with a bevelled edge, four bolts, and the power out in the middle of the face:
// an insulator pad with two copper contacts, the motor's sign for "power connects here". South face: u = x, v = 16 - y
T.paint('base', (u, v) => {
  if (u === 1 || v === 2) return HI;
  if (u === 14 || v === 13) return LO;
  if ((u === 2 || u === 13) && (v === 3 || v === 12)) return '#6b6e6a';
  // an upright pad with the two contacts stacked, like a plug socket: side by side they read as a pair of eyes
  if (u >= 6 && u <= 9 && v >= 3 && v <= 12) {
    if ((u === 7 || u === 8) && (v === 5 || v === 6 || v === 9 || v === 10)) return v === 5 || v === 9 ? '#b87a4e' : '#9c5f3a';
    if (u === 6 || v === 3) return '#1a1614';
    return hash(u, v, 89) > 0.85 ? '#2a2420' : '#241f1c';
  }
  return mot('steel', u, v, 90);
});
// vents: the Helper Bot's whisker grille, muted brass with three dark slots (mapped explicitly, 4 x 7); three, so it
// reads as a grille and never as a pair of eyes when the block stands up
T.paint('vent', (u, v) => (u === 0 || u === 3 || v === 0 || v === 6 ? '#b08f45' : v % 2 === 1 ? '#2a2f35' : '#6e5a2a'));
T.paint('lamp_off', (x, y) => (hash(x, y, 91) > 0.8 ? '#24383c' : '#1e3236'));
// the electric family's running lamp, painted as the motor's and the generators' are
T.paint('lamp_cyan', (x, y) => (edge(x % 4, y % 4) === 0 ? '#4f8f99' : '#6fb7c4'));

const t = (k) => 'dyn:' + k;

// ---------- the block ----------
const CAV = { x0: 4.5, x1: 11.5, y0: 9, z0: 3.75, z1: 6.75 }; // the window cavity, behind the shaft stub's end (z 3.5)
const TOP = 13.5;
// The chassis as non-overlapping boxes: flat faces are andesite fields, the 1 px steps round every edge are dark steel
function chassis() {
  const F = t('field'), S = t('steel'), D = t('dark');
  return [
    // the middle column, with the window cavity cut out of its top
    C('mid_lo', [2.5, 2.5, 2], [13.5, CAV.y0, 13], { all: S, down: F, up: t('floor') }),
    C('mid_w', [2.5, CAV.y0, 2], [CAV.x0, TOP, 13], { all: S, up: F, east: D, down: null }),
    C('mid_e', [CAV.x1, CAV.y0, 2], [13.5, TOP, 13], { all: S, up: F, west: D, down: null }),
    C('mid_f', [CAV.x0, CAV.y0, 2], [CAV.x1, TOP, CAV.z0], { all: S, up: F, south: D, down: null, east: null, west: null }),
    C('mid_b', [CAV.x0, CAV.y0, CAV.z1], [CAV.x1, TOP, 13], { all: S, up: F, north: D, down: null, east: null, west: null }),
    // the side strips and the front and back steps
    C('side_w', [1.5, 3.5, 2], [2.5, 12.5, 13], { all: S, west: F, east: null }),
    C('side_e', [13.5, 3.5, 2], [14.5, 12.5, 13], { all: S, east: F, west: null }),
    C('front', [2.5, 3.5, 1], [13.5, 12.5, 2], { all: S, north: F, south: null }),
    C('back', [2.5, 3.5, 13], [13.5, 12.5, 14], { all: S, north: null, south: null }),
  ];
}
// The waist band round the chassis (across the shaft), stepped round the corners, with its rivet line
function band() {
  const z0 = 7.75, z1 = 9.25, B = t('band'), E = t('brass_edge');
  const ends = { north: E, south: E };
  return [
    C('band_top', [2.5, TOP, z0], [13.5, TOP + 0.5, z1], { ...ends, up: B, down: null }, { uv: { up: [2.5, 0, 13.5, 1.5] } }),
    C('band_bottom', [2.5, 2, z0], [13.5, 2.5, z1], { ...ends, down: B, up: null }, { uv: { down: [2.5, 0, 13.5, 1.5] } }),
    C('band_w', [1, 3.5, z0], [1.5, 12.5, z1], { ...ends, west: B, east: null }, { uv: { west: [0, 3.5, 1.5, 12.5] } }),
    C('band_e', [14.5, 3.5, z0], [15, 12.5, z1], { ...ends, east: B, west: null }, { uv: { east: [0, 3.5, 1.5, 12.5] } }),
    // corners fill the steps: square brass blocks where the band turns
    C('band_ne', [13.5, 12.5, z0], [15, 14, z1], { ...ends, up: t('brass'), east: t('brass'), west: null, down: null }),
    C('band_nw', [1, 12.5, z0], [2.5, 14, z1], { ...ends, up: t('brass'), west: t('brass'), east: null, down: null }),
    C('band_se', [13.5, 2, z0], [15, 3.5, z1], { ...ends, down: t('brass'), east: t('brass'), west: null, up: null }),
    C('band_sw', [1, 2, z0], [2.5, 3.5, z1], { ...ends, down: t('brass'), west: t('brass'), east: null, up: null }),
  ];
}
function block(working) {
  const out = [...chassis(), ...band()];
  // the bearing boss on the front: the only circle there
  out.push(C('boss', [4, 4, 1 - 0.5], [12, 12, 1], { all: t('steel'), north: t('boss'), south: null }, { uv: { north: [0, 0, 16, 16] } }));
  // the base plate, its back the power out
  out.push(C('base', [1, 2, 14], [15, 14, 16], { all: t('steel'), south: t('base') }));
  // the commutator window: a steel rim round the slot (brass there crowded the top), and the brushes on their holders inside
  const R = 0.5, y0 = TOP, y1 = TOP + 0.5, br = t('brass'), rim = t('rim');
  out.push(
    C('rim_f', [CAV.x0 - R, y0, CAV.z0 - R], [CAV.x1 + R, y1, CAV.z0], { all: rim, down: null }),
    C('rim_b', [CAV.x0 - R, y0, CAV.z1], [CAV.x1 + R, y1, CAV.z1 + R], { all: rim, down: null }),
    C('rim_w', [CAV.x0 - R, y0, CAV.z0], [CAV.x0, y1, CAV.z1], { all: rim, down: null, north: null, south: null }),
    C('rim_e', [CAV.x1, y0, CAV.z0], [CAV.x1 + R, y1, CAV.z1], { all: rim, down: null, north: null, south: null }),
  );
  for (const [s, hx0, hx1, bx0, bx1] of [['w', 4.5, 5, 5, 5.75], ['e', 11, 11.5, 10.25, 11]]) {
    out.push(C(`holder_${s}`, [hx0, 10.25, 4.25], [hx1, 12.25, 6.25], { all: t('copper'), [s === 'w' ? 'west' : 'east']: null }));
    out.push(C(`brush_${s}`, [bx0, 10.75, 4.5], [bx1, 11.75, 6], { all: t('carbon'), [s === 'w' ? 'west' : 'east']: null }));
    out.push(C(`spring_${s}`, [bx0, 11.75, 5], [bx1, 12.25, 5.5], { all: br, [s === 'w' ? 'west' : 'east']: null }));
  }
  // the status lamp, right beside the aerial so the two read as one signal unit (spaced apart they read as eyes over the
  // window's mouth): a lens in a brass rim (frames go round, not behind)
  const L = { x0: 8.5, x1: 10.5, z0: 10.5, z1: 12.5 };
  out.push(
    C('lamp_rim_f', [L.x0 - R, y0, L.z0 - R], [L.x1 + R, y1, L.z0], { all: br, down: null }),
    C('lamp_rim_b', [L.x0 - R, y0, L.z1], [L.x1 + R, y1, L.z1 + R], { all: br, down: null }),
    C('lamp_rim_w', [L.x0 - R, y0, L.z0], [L.x0, y1, L.z1], { all: br, down: null, north: null, south: null }),
    C('lamp_rim_e', [L.x1, y0, L.z0], [L.x1 + R, y1, L.z1], { all: br, down: null, north: null, south: null }),
    C('lamp', [L.x0, y0, L.z0], [L.x1, y0 + LAYER, L.z1], { up: working ? t('lamp_cyan') : t('lamp_off'), all: null }, { glow: working }),
  );
  // the aerial's collar (the aerial itself is a renderer part)
  out.push(C('aerial_collar', [11, y0, 10.75], [12.5, y1, 12.25], { all: br, down: null }));
  // vents on both sides, clear of the band
  out.push(C('vent_e', [14.5, 4.5, 9.75], [14.5 + LAYER, 11.5, 13], { east: t('vent'), all: t('brass_edge'), west: null }, { uv: { east: [0, 0, 4, 7] } }));
  out.push(C('vent_w', [1.5 - LAYER, 4.5, 9.75], [1.5, 11.5, 13], { west: t('vent'), all: t('brass_edge'), east: null }, { uv: { west: [0, 0, 4, 7] } }));
  return out;
}

// ---------- renderer parts ----------
const AERIAL_PIVOT = [11.75, TOP + 0.5, 11.5];
function aerial() {
  const [x, y, z] = AERIAL_PIVOT;
  return [
    C('aerial_rod', [x - 0.25, y, z - 0.25], [x + 0.25, y + 1, z + 0.25], { all: t('copper'), down: null }),
    C('aerial_bead', [x - 0.5, y + 1, z - 0.5], [x + 0.5, y + 2, z + 0.5], { all: t('bead'), up: t('bead_top') }),
  ];
}
// The commutator: a drum of 16 copper bars on its own axis, above the shaft's, spun at the shaft's speed
const COMM = { x: 8, y: 11.25, z0: 4, z1: 6.5, r: 2 };
function commutator() {
  const { x: ax, y: ay, z0, z1, r } = COMM, thick = 0.9, w = 2 * r * Math.tan(Math.PI / 16) + 0.15, out = [];
  for (let s = 0; s < 16; s++) {
    const phi = s * 22.5;
    const base = [0, 90, 180, 270].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    const delta = ((phi - base + 540) % 360) - 180;
    let f, to;
    if (base === 0) { f = [ax - w / 2, ay + r - thick, z0]; to = [ax + w / 2, ay + r, z1]; }
    else if (base === 180) { f = [ax - w / 2, ay - r, z0]; to = [ax + w / 2, ay - r + thick, z1]; }
    else if (base === 90) { f = [ax + r - thick, ay - w / 2, z0]; to = [ax + r, ay + w / 2, z1]; }
    else { f = [ax - r, ay - w / 2, z0]; to = [ax - r + thick, ay + w / 2, z1]; }
    const tex = t(s % 2 ? 'bar_b' : 'bar_a');
    out.push(C(`bar_${s}`, f, to, { all: tex, north: t('copper'), south: t('copper') }, { rot: delta ? ['z', -delta, [ax, ay, z0]] : undefined }));
  }
  // the hub, so the drum never looks hollow through the gaps
  out.push(C('hub', [ax - 1.2, ay - 1.2, z0 + LAYER], [ax + 1.2, ay + 1.2, z1 - LAYER], { all: t('dark') }));
  return out;
}
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const shaftStub = () => [C('shaft_stub', [6, 6, -0.02], [10, 10, 3.5], { north: AXIS_TOP, south: AXIS_TOP, all: AXIS },
  { uv: { north: [6, 6, 10, 10], south: [6, 6, 10, 10], east: [6, 0, 10, 4], west: [6, 0, 10, 4], up: [6, 0, 10, 4], down: [6, 0, 10, 4] }, frot: { east: 90, west: 90 } })];

// ---------- preview ----------
if (require.main === module) {
  // the Electric Motor beside it for the family read (electric.gen.js writes its own preview on load: send it to the scratchpad)
  const argv = process.argv;
  process.argv = [argv[0], argv[1], path.join(require('os').tmpdir(), 'electric-for-dynamo.bbmodel')];
  const E = require('./electric.gen.js');
  process.argv = argv;
  const { cube, group, clip, write } = preview('clockwork_dynamo', { 'dyn:': T, 'mot:': E.MOT });
  const cubes = (list, off) => list.map((c) => cube(c, off));
  // one dynamo standing as placed facing north, then facing up and facing down for the six-facing read
  const SCENES = [['dyn', [0, 0, 0]], ['up', [24, 0, 0]], ['down', [48, 0, 0]]];
  const MO = [-24, 0, 0];
  const motorScene = group('electric_motor', [8, 8, 8], MO, [...cubes(E.motor(true), MO), ...cubes(E.shaftStub(), MO)]);
  const outliner = [motorScene, ...SCENES.map(([n, o]) => group(n, [8, 8, 8], o, [
    group(`${n}_off`, [8, 8, 8], o, cubes(block(false), o)),
    group(`${n}_on`, [8, 8, 8], o, cubes(block(true), o)),
    group(`${n}_aerial`, AERIAL_PIVOT, o, cubes(aerial(), o)),
    group(`${n}_comm`, [COMM.x, COMM.y, COMM.z0], o, cubes(commutator(), o)),
    group(`${n}_shaft`, [8, 8, 8], o, cubes(shaftStub(), o)),
  ], { open: n === 'dyn' }))];

  // Motion. Create has no inertia: the shaft is at speed or stopped, so the shaft and commutator snap; only the aerial springs.
  const RPM = 120, DEG = RPM * 6; // degrees a second (2 s = 4 whole turns, so loops close)
  const FOLD = 70; // the aerial folded back along the top, towards the base
  // a damped spring from `from` to `to`, released at t0
  const spring = (t, t0, from, to, f, z) => {
    if (t <= t0) return from;
    const w = 2 * Math.PI * f, wd = w * Math.sqrt(1 - z * z), s = t - t0;
    return to + (from - to) * Math.exp(-z * w * s) * (Math.cos(wd * s) + (z / Math.sqrt(1 - z * z)) * Math.sin(wd * s));
  };
  // a kick that rings down: amplitude a, frequency f, decay time tau
  const ring = (t, t0, a, f, tau) => (t <= t0 ? 0 : a * Math.exp(-(t - t0) / tau) * Math.sin(2 * Math.PI * f * (t - t0)));
  const all = (fn) => Object.fromEntries(SCENES.flatMap(([n]) => Object.entries(fn(n))));
  const FACINGS = { up: { rotation: [[0, [-90, 0, 0]]] }, down: { rotation: [[0, [90, 0, 0]]] } };
  const lamp = (on) => all((n) => ({ [`${n}_off`]: { scale: [[0, on ? HIDE : SHOW]] }, [`${n}_on`]: { scale: [[0, on ? SHOW : HIDE]] } }));
  const lampAt = (tOn) => all((n) => ({ [`${n}_off`]: { scale: [[0, SHOW], [tOn, HIDE]] }, [`${n}_on`]: { scale: [[0, HIDE], [tOn, SHOW]] } }));
  const lampOffAt = (tOff) => all((n) => ({ [`${n}_off`]: { scale: [[0, HIDE], [tOff, SHOW]] }, [`${n}_on`]: { scale: [[0, SHOW], [tOff, HIDE]] } }));
  const spin = (angle) => all((n) => ({ [`${n}_shaft`]: { rotation: (s) => [0, 0, -angle(s)] }, [`${n}_comm`]: { rotation: (s) => [0, 0, -angle(s)] } }));
  const aer = (fn) => all((n) => ({ [`${n}_aerial`]: { rotation: fn } }));
  const merge = (...xs) => { const o = {}; for (const x of xs) for (const [k, v] of Object.entries(x)) o[k] = { ...(o[k] || {}), ...v }; return o; };
  // approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files
  const SOUND_DIR = path.resolve(__dirname, '../sound/clockwork-dynamo');
  const CUES = JSON.parse(require('fs').readFileSync(path.join(SOUND_DIR, 'clockwork-dynamo.cues.json'), 'utf8'));
  function sounds(clipName) {
    const list = CUES.clips[clipName] || [];
    const missing = [...new Set(list.map(([, id]) => id).filter((id) => !require('fs').existsSync(path.join(SOUND_DIR, 'real', id + '.ogg'))))];
    if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
    return list.map(([t, id]) => [t, CUES.namespace + '.' + id, path.join(SOUND_DIR, 'real', id + '.ogg').split(path.sep).join('/')]);
  }

  // start: idle 0.5 s, then the shaft turns: lamp on, the aerial springs up past upright and wobbles to rest (0.8 s)
  const ON = 0.5;
  clip('start', 2, 'once', merge(
    spin((s) => Math.max(0, s - ON) * DEG),
    aer((s) => [spring(s, ON + 0.05, FOLD, 0, 2.8, 0.42), 0, 0]),
  ), { ...lampAt(ON), ...FACINGS }, sounds('start'));
  // working: steady, the aerial upright and still
  clip('working', 2, 'loop', merge(spin((s) => s * DEG), aer(() => [0, 0, 0])), { ...lamp(true), ...FACINGS }, sounds('working'));
  // boing: the setting changes at 0.4 s: a quick wobble about the hinge with a slight sideways lean (0.7 s)
  clip('boing', 2, 'loop', merge(spin((s) => s * DEG),
    aer((s) => [ring(s, 0.4, -12, 4.3, 0.22), 0, ring(s, 0.45, 5, 2.6, 0.25)])), { ...lamp(true), ...FACINGS }, sounds('boing'));
  // stop: the shaft stops dead at 0.5 s, the lamp goes dark, and the aerial sags back to folded with one small bounce (1.0 s)
  const OFF = 0.5;
  clip('stop', 2, 'once', merge(
    spin((s) => Math.min(s, OFF) * DEG),
    aer((s) => [spring(s, OFF + 0.1, 0, FOLD, 1.4, 0.55), 0, 0]),
  ), { ...lampOffAt(OFF), ...FACINGS }, sounds('stop'));
  // idle: stopped, folded, dark
  clip('idle', 1, 'hold', aer(() => [FOLD, 0, 0]), { ...lamp(false), ...FACINGS }, sounds('idle'));

  write(OUT, outliner);
}
module.exports = { block, aerial, commutator, shaftStub, T, LAYER, AERIAL_PIVOT, COMM };
