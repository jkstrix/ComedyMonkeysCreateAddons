// Electric Motor and Lava Generator redesign generator -> design/models/electric.bbmodel (Generic Model preview with clips).
// Built on the family kit in the game's model space (front north, z 0), to the briefs design/briefs/lava-generator.md and
// design/briefs/electric-motor.md. Each block's two states come from one part list, so their geometry is identical;
// parts flagged glow get NeoForge's full-bright light in game.
const fs = require('fs');
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, SEAM, preview, SHOW, HIDE, smooth, lerp } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'electric.bbmodel');
const LAYER = 0.25; // overlays sit this far in front of what's behind them (zfight-check.js)

// ---------- textures (shared painters, one set per block: lava_generator_<key>, electric_motor_<key>) ----------
P.drum = ['#2f343b', '#33383f', '#363b42'];
P.amber = ['#c76a2a', '#d0803a', '#d99a4a'];
P.coil = ['#7a4a2e', '#8a5434', '#9c5f3a'];
function paintFamily(T) {
  T.paint('panel', painters.side('andesite', () => null));
  T.paint('plinth', (x, y) => (y === 14 ? '#8c8f88' : y === 15 ? '#5a5d58' : mot('andesite', x, y, 70)));
  T.paint('plate', (x, y) => (edge(x, y) === 0 ? (x === 0 || y === 0 ? HI : LO) : FIELD(x, y, 40)));
  T.paint('steel', painters.steel());
  T.paint('dark', painters.dark());
  T.paint('andesite', painters.andesite());
  T.paint('brass', painters.brass());
  T.paint('copper', painters.copper());
  T.paint('drum', (x, y) => (x % 8 === 0 ? (y % 4 === 1 ? '#4a5059' : '#2a2f35') : (hash(x, y, 41) > 0.88 ? '#363b42' : '#33383f')));
  T.paint('cap', (x, y) => {
    const r = Math.hypot(x - 7.5, y - 7.5), a = Math.atan2(y - 7.5, x - 7.5);
    if (r > 7.6) return null;
    if (r > 6.0) return '#5a5f5c';
    const k = Math.round(a / (Math.PI / 4)), rx = 7.5 + 4.6 * Math.cos(k * Math.PI / 4), ry = 7.5 + 4.6 * Math.sin(k * Math.PI / 4);
    if (Math.abs(x - rx) < 0.6 && Math.abs(y - ry) < 0.6) return '#4a5059';
    if (r < 1.5) return P.andesite[1];
    if (r < 2.5) return '#2a2f35';
    return '#33383f';
  });
  T.paint('bezel', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r > 7.6 || r < 6.2 ? null : (x + y < 15 ? '#b08f45' : '#8f7638'); });
  T.paint('backing', (x, y) => (Math.hypot(x - 7.5, y - 7.5) > 7.6 ? null : hash(x, y, 60) > 0.85 ? '#24282d' : '#1f2327'));
  T.paint('glass', painters.glass());
  T.paint('rim', () => '#3c424a');
  T.paint('blade', (x, y) => (y === 0 || x === 0 ? '#6b7078' : mot('steelLight', x, y, 45)));
  T.paint('lamp_off', (x, y) => (hash(x, y, 47) > 0.8 ? '#2c3236' : '#22272b'));
  T.paint('lamp_cyan', (x, y) => (edge(x % 4, y % 4) === 0 ? '#4f8f99' : '#6fb7c4'));
  T.paint('lamp_red', (x, y) => (edge(x % 4, y % 4) === 0 ? '#6a2219' : '#8a2f22'));
}
const GEN = textureSet(); GEN.file = 'lava_generator_';
const MOT = textureSet(); MOT.file = 'electric_motor_';
paintFamily(GEN);
paintFamily(MOT);
// the firebox window: blotchy embers (no stripes), dark ash when cold
const ember = (x, y) => hash(x >> 1, y >> 1, 46) * 0.6 + hash(x, y, 47) * 0.4;
GEN.paint('grate', (x, y) => (ember(x, y) < 0.25 ? '#2a1f1a' : hash(x, y, 48) > 0.8 ? '#1f2327' : '#1a1d20'));
// mapped at 1.6 texels a pixel across and 2 down (uv 16 x 4), so the glints are thin diagonal streaks; the rest is clear
const GLINTS = { '4,0': '#6f7f86', '3,1': '#6f7f86', '2,2': '#5f6d73', '6,0': '#5f6d73', '5,1': '#5f6d73', '13,1': '#6f7f86', '12,2': '#6f7f86', '11,3': '#5f6d73' };
GEN.paint('fireglass', (x, y) => GLINTS[x + ',' + y] || null);
// lava sitting in the tank, not burning: calmer and darker, with a little crust; it still glows (it's lava)
GEN.paint('still', (x, y) => { const n = ember(x, y); return n > 0.72 ? '#b0602a' : n > 0.45 ? '#94481f' : n > 0.2 ? '#7a3519' : '#3a1d14'; });
GEN.paint('glow', (x, y) => { const n = ember(x, y); return n > 0.72 ? '#d08a3e' : n > 0.45 ? '#c06c2c' : n > 0.2 ? '#a4521f' : '#5a2a18'; });
// the lava inlet: a copper flange round a dark bore, on steel
GEN.paint('inlet', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r < 3) return '#1f2327';
  if (r < 3.8) return '#2a2f35';
  if (r < 6.6) return x + y < 15 ? '#9c5f3a' : '#7a4a2e';
  if (edge(x, y) === 0) return x === 0 || y === 0 ? HI : LO;
  return FIELD(x, y, 61);
});
GEN.paint('insulator', (x, y) => (hash(x, y, 62) > 0.85 ? '#2a2420' : '#241f1c'));
// the motor's windings, seen through the slotted band: copper turns between steel slats
MOT.paint('winding', (x, y) => (x % 4 === 0 ? '#2a2f35' : y % 2 === 0 ? (x % 4 === 1 ? '#b87a4e' : '#9c5f3a') : mot('coil', x, y, 63)));
// the end bell: a ring round the shaft opening (hole radius about 3 px at the bell's 6 px)
MOT.paint('bell', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r > 7.6 || r < 4.6) return null;
  if (r > 6.8) return '#5a5f5c';
  return r < 5.5 ? '#2a2f35' : hash(x, y, 64) > 0.88 ? '#363b42' : '#33383f';
});
MOT.paint('back', (x, y) => { const e = edge(x, y); if (e === 0) return mot('andesite', x, y, 71); if (e === 1) return x === 1 || y === 1 ? LO : HI;
  if ((x === 3 || x === 12) && (y === 3 || y === 12)) return P.andesite[2]; // the power input: an insulator pad with two copper contacts
  if (x >= 4 && x <= 11 && y >= 4 && y <= 11) { if ((x === 5 || x === 6 || x === 9 || x === 10) && y >= 6 && y <= 9) return y === 6 ? '#b87a4e' : (x === 6 || x === 10) ? '#7a4a2e' : '#9c5f3a';
    return x === 4 || y === 4 ? '#1a1614' : hash(x, y, 73) > 0.85 ? '#2a2420' : '#241f1c'; }
  return FIELD(x, y, 72); });
MOT.paint('ring', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r > 7.6 || r < 6.4 ? null : mot('andesite', x, y, 50); });

// ---------- helpers ----------
const S = (pfx) => (k) => (k && !k.includes(':') ? pfx + k : k); // texture keys by block
// A round drum along Z from 16 staves; each is a box at top/right/bottom/left, turned up to 45 degrees about the axis.
// ends: textures for the staves' north/south faces where the drum's rim shows.
function drumZ(name, ax, r, z0, z1, thick, tex, skip = [], ends = {}) {
  const w = 2 * r * Math.tan(Math.PI / 16) + 0.15, out = [];
  for (let s = 0; s < 16; s++) {
    if (skip.includes(s)) continue;
    const phi = s * 22.5; // from +Y round to +X
    const base = [0, 90, 180, 270].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    const delta = ((phi - base + 540) % 360) - 180;
    let f, t;
    if (base === 0) { f = [ax.x - w / 2, ax.y + r - thick, z0]; t = [ax.x + w / 2, ax.y + r, z1]; }
    else if (base === 180) { f = [ax.x - w / 2, ax.y - r, z0]; t = [ax.x + w / 2, ax.y - r + thick, z1]; }
    else if (base === 90) { f = [ax.x + r - thick, ax.y - w / 2, z0]; t = [ax.x + r, ax.y + w / 2, z1]; }
    else { f = [ax.x - r, ax.y - w / 2, z0]; t = [ax.x - r + thick, ax.y + w / 2, z1]; }
    out.push(C(`${name}_${s}`, f, t, { all: tex, north: null, south: null, ...ends }, { rot: delta ? ['z', -delta, [ax.x, ax.y, z0]] : undefined }));
  }
  return out;
}
// A disc facing -Z or +Z: one plate, its texture cut out to a circle
const disc = (name, ax, r, z0, z1, tex, side = 'north') => [C(name, [ax.x - r, ax.y - r, z0], [ax.x + r, ax.y + r, z1], { [side]: tex, all: null }, { uv: { [side]: [0, 0, 16, 16] } })];

// ---------- Lava Generator ----------
const GAX = { x: 8, y: 11.5 }; // rotor axis (ElectricRenderers.Generator), and the housing's
// state: 'empty' (no lava: ash behind the glass), 'full' (lava sitting in the window) or 'lit' (burning, lamp on)
function generator(state = 'empty') {
  const t = S('gen:'), out = [], lit = state === 'lit', full = state !== 'empty';
  out.push(
    C('plinth', [0, 0, 0], [16, 2, 16], { all: t('plinth') }),
    C('firebox', [1, 2, 2], [15, 8, 15], { all: t('plate'), down: null, north: null }),
    // the front plate round a low window, set 0.75 px back, with the lava behind glass whenever there's lava in the tank
    C('front_l', [1, 2, 1], [3, 8, 2], { all: t('plate'), east: t('dark'), south: null, down: null }),
    C('front_r', [13, 2, 1], [15, 8, 2], { all: t('plate'), west: t('dark'), south: null, down: null }),
    C('front_t', [3, 6, 1], [13, 8, 2], { all: t('plate'), down: t('dark'), south: null, east: null, west: null }),
    C('front_b', [3, 2, 1], [13, 4, 2], { all: t('plate'), up: t('dark'), south: null, east: null, west: null, down: null }),
    C('lava', [3, 4, 1.75], [13, 6, 2], { north: t(lit ? 'glow' : full ? 'still' : 'grate'), all: null }, { glow: full }),
    C('window_glass_f', [3, 4, 1.25], [13, 6, 1.25], { north: t('fireglass'), all: null }, { uv: { north: [0, 0, 16, 4] }, shade: false }),
    C('bezel_t', [2.5, 6, 0.75], [13.5, 6.5, 1], { all: t('andesite'), south: null }),
    C('bezel_b', [2.5, 3.5, 0.75], [13.5, 4, 1], { all: t('andesite'), south: null }),
    C('bezel_l', [2.5, 4, 0.75], [3, 6, 1], { all: t('andesite'), south: null, up: null, down: null }),
    C('bezel_r', [13, 4, 0.75], [13.5, 6, 1], { all: t('andesite'), south: null, up: null, down: null }),
  );
  // turbine housing on top, with its back cap and the window in front
  out.push(...drumZ('housing', GAX, 4.5, 1.5, 14, 1.2, t('drum'), [8], { north: t('rim'), south: t('rim') })); // the bottom stave sits in the firebox
  out.push(...disc('housing_back', GAX, 3.5, 13.5, 13.75, t('cap'), 'south'));
  out.push(...disc('window_backing', GAX, 3.9, 6, 6.05, t('backing')));
  out.push(C('window_glass', [GAX.x - 3.6, GAX.y - 3.6, 1.8], [GAX.x + 3.6, GAX.y + 3.6, 1.8], { north: t('glass'), all: null }, { uv: { north: [0, 0, 16, 16] }, shade: false }));
  out.push(...disc('window_bezel', GAX, 4.3, 1.5 - LAYER, 1.55 - LAYER, t('bezel')));
  // lamp on the firebox's top front corner, cyan while running
  out.push(C('lamp_housing', [1.75, 8, 1.75], [4.25, 8.8, 4.25], { all: t('andesite'), down: null }));
  out.push(C('lamp', [2, 8.8, 2], [4, 9.6, 4], { all: lit ? t('lamp_cyan') : t('lamp_off'), down: null }, { glow: lit }));
  // The ports sit at the middle of their faces, where pipes and Pipez cables attach (8, 8).
  // Power: on each flank a steel terminal block stands on the firebox beside the drum, and the insulator plate with
  // its two copper posts is centred on the face, across the firebox and the block
  for (const side of ['west', 'east']) {
    const W = side === 'west', inner = W ? 'east' : 'west', x = (a0, a1) => (W ? [a0, a1] : [16 - a1, 16 - a0]);
    const [tx0, tx1] = x(1, 3.75), [ix0, ix1] = x(0.5, 1), [px0, px1] = x(0, 0.5), [cx0, cx1] = x(0.25, 0.5);
    // the block is stepped at both ends (full height in the middle, a lower step front and back) under an andesite cap, so it reads as a finished junction box
    out.push(C(`terminal_block_${side}`, [tx0, 8, 6], [tx1, 12, 10], { all: t('plate'), down: null, up: null }));
    for (const [n, z0, z1] of [['f', 4.5, 6], ['b', 10, 11.5]])
      out.push(C(`terminal_step_${side}_${n}`, [tx0, 8, z0], [tx1, 10, z1], { all: t('plate'), down: null, [n === 'f' ? 'south' : 'north']: null }));
    // an andesite cap finishes the top
    const [kx0, kx1] = x(0.75, 3.5); // up to the drum's side, not into it
    out.push(C(`terminal_cap_${side}`, [kx0, 12, 5.75], [kx1, 12.5, 10.25], { all: t('andesite') }));
    out.push(C(`terminal_${side}`, [ix0, 5.5, 5.5], [ix1, 10.5, 10.5], { all: t('insulator') })); // its back shows above the steps
    for (const [i, z] of [[0, 6], [1, 8.5]]) {
      out.push(C(`post_${side}_${i}`, [px0, 7, z], [px1, 9, z + 1.5], { all: t('copper'), [inner]: null }));
      out.push(C(`collar_${side}_${i}`, [cx0, 6.6, z - 0.4], [cx1, 9.4, z + 1.9], { all: t('andesite'), [inner]: null }));
    }
  }
  // Lava: a copper flange round a dark bore, centred on the back, on a short manifold bridging the firebox and the drum
  out.push(C('inlet_manifold', [5, 8, 14], [11, 11, 15], { all: t('plate'), south: null, down: null }));
  out.push(C('inlet', [4.5, 4.5, 15], [11.5, 11.5, 16], { all: t('copper'), south: t('inlet') }, { uv: { south: [0, 0, 16, 16] } }));
  return out;
}
function rotor() {
  const R = 3.4, z0 = 3, z1 = 4.2, out = [];
  for (const a of [0, 45, -45]) out.push(C(`blades_v_${a}`, [GAX.x - 0.45, GAX.y - R, z0], [GAX.x + 0.45, GAX.y + R, z1], { all: 'gen:blade' }, { rot: a ? ['z', a, [GAX.x, GAX.y, z0]] : undefined }));
  out.push(C('blades_h', [GAX.x - R, GAX.y - 0.45, z0 + LAYER], [GAX.x + R, GAX.y + 0.45, z1 - LAYER], { all: 'gen:blade' }));
  out.push(...disc('hub', GAX, 1.3, z0 - 0.3, z0 - 0.25, 'gen:cap'));
  out.push(C('arbor', [GAX.x - 0.5, GAX.y - 0.5, z1], [GAX.x + 0.5, GAX.y + 0.5, 6], { all: 'gen:steel' }));
  return out;
}

// ---------- Electric Motor ----------
const MAX = { x: 8, y: 8 };
function motor(powered) {
  const t = S('mot:'), out = [];
  // the square mounting flange at the back, with three bolts and the lamp in the fourth corner
  out.push(C('flange', [0.5, 0.5, 13], [15.5, 15.5, 16], { all: t('plate'), north: t('panel'), south: t('back') }));
  for (const [i, x, y] of [[0, 1.6, 1.6], [1, 12.9, 1.6], [2, 1.6, 12.9]]) out.push(C(`bolt_${i}`, [x, y, 13 - 0.5], [x + 1.5, y + 1.5, 13], { all: t('andesite'), south: null }));
  out.push(C('lamp', [12.5, 12.5, 13 - LAYER], [14.5, 14.5, 13], { north: powered ? t('lamp_cyan') : t('lamp_red'), all: null }, { glow: true }));
  for (const [n, f, to] of [['l', [12, 12, 12.4], [12.5, 15, 13]], ['r', [14.5, 12, 12.4], [15, 15, 13]], ['b', [12.5, 12, 12.4], [14.5, 12.5, 13]], ['t', [12.5, 14.5, 12.4], [14.5, 15, 13]]])
    out.push(C(`lamp_bezel_${n}`, f, to, { all: t('brass'), south: null }));
  // the can: steel staves, with the winding band in the middle
  out.push(...drumZ('can_front', MAX, 6, 3, 6.5, 1.2, t('drum'), [], { north: t('rim'), south: t('rim') }));
  out.push(...drumZ('can_band', MAX, 5.3, 6.5, 9.5, 1.2, t('winding'), [], { north: t('rim'), south: t('rim') }));
  out.push(...drumZ('can_back', MAX, 6, 9.5, 13, 1.2, t('drum'), [], { north: t('rim') }));
  // cooling ribs along the can, either side of the band
  for (const [i, a] of [[0, 0], [1, 90], [2, 180], [3, 270]]) for (const [zn, z0, z1] of [['f', 3.5, 6], ['b', 10, 12.5]]) {
    const r0 = 6, r1 = 6.7, wdt = 0.6;
    const f = a === 0 ? [MAX.x - wdt / 2, MAX.y + r0, z0] : a === 180 ? [MAX.x - wdt / 2, MAX.y - r1, z0] : a === 90 ? [MAX.x + r0, MAX.y - wdt / 2, z0] : [MAX.x - r1, MAX.y - wdt / 2, z0];
    const to = a === 0 ? [MAX.x + wdt / 2, MAX.y + r1, z1] : a === 180 ? [MAX.x + wdt / 2, MAX.y - r0, z1] : a === 90 ? [MAX.x + r1, MAX.y + wdt / 2, z1] : [MAX.x - r0, MAX.y + wdt / 2, z1];
    out.push(C(`rib_${i}_${zn}`, f, to, { all: t('steel') }));
    out.push(C(`rib45_${i}_${zn}`, f, to, { all: t('steel') }, { rot: ['z', 45, [MAX.x, MAX.y, z0]] }));
  }
  // the end bell round the shaft (opening about 6 px across for the turning stub), its bearing ring and dark inside
  out.push(...disc('bell', MAX, 5.2, 3 - LAYER, 3.05 - LAYER, t('bell')));
  out.push(...disc('bearing', MAX, 3.6, 3 - 2 * LAYER, 3.05 - 2 * LAYER, t('ring')));
  out.push(...disc('inside', MAX, 4, 4.2, 4.25, t('backing')));
  // a small brass nameplate on the can, between two ribs
  out.push(C('nameplate', [6.5, 14, 10], [9.5, 14.3, 12], { all: t('brass'), down: null }, { rot: ['z', -22.5, [MAX.x, MAX.y, 10]] }));
  return out;
}
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const shaftStub = () => [C('shaft_stub', [6, 6, -0.02], [10, 10, 3.5], { north: AXIS_TOP, south: AXIS_TOP, all: AXIS },
  { uv: { north: [6, 6, 10, 10], south: [6, 6, 10, 10], east: [6, 0, 10, 4], west: [6, 0, 10, 4], up: [6, 0, 10, 4], down: [6, 0, 10, 4] }, frot: { east: 90, west: 90 } })];

// ---------- preview ----------
const { cube, group, clip, write } = preview('electric', { 'gen:': GEN, 'mot:': MOT });
const cubes = (list, off) => list.map((c) => cube(c, off));
const G0 = [0, 0, 0], M0 = [24, 0, 0], M1 = [48, 0, 0], M2 = [72, 0, 0];
const outliner = [
  group('generator', [8, 0, 8], G0, [
    group('gen_off', [8, 0, 8], G0, cubes(generator('empty'), G0)),
    group('gen_full', [8, 0, 8], G0, cubes(generator('full'), G0)),
    group('gen_lit', [8, 0, 8], G0, cubes(generator('lit'), G0)),
    group('gen_rotor', [GAX.x, GAX.y, 4], G0, cubes(rotor(), G0)),
  ], { open: true }),
  group('motor', [8, 8, 8], M0, [
    group('mot_off', [8, 8, 8], M0, cubes(motor(false), M0)),
    group('mot_on', [8, 8, 8], M0, cubes(motor(true), M0)),
    group('mot_shaft', [8, 8, 8], M0, cubes(shaftStub(), M0)),
  ], { open: true }),
  // the same motor facing down and facing up, to judge the six-facing look
  ...[['down', M1], ['up', M2]].map(([n, o]) => group(`motor_${n}`, [8, 8, 8], o, [
    group(`${n}_off`, [8, 8, 8], o, cubes(motor(false), o)),
    group(`${n}_on`, [8, 8, 8], o, cubes(motor(true), o)),
    group(`${n}_shaft`, [8, 8, 8], o, cubes(shaftStub(), o)),
  ])),
];
// run: the generator idles, fires up and its wheel follows a load that rises, holds and eases; the power stops at 7.5 s and
// the fire goes out 2 s later. The motor's lamp is red until it gets power at 2 s, then cyan with its shaft spinning, until
// it runs short at 8 s and stops (6 whole turns, so the shaft comes to rest where it started)
const L = 10.5, ON = 1, OFF = 7.5;
const loadAt = (s) => (s < ON || s >= OFF ? 0 : s < 3 ? lerp(0.25, 1, smooth((s - ON) / 2)) : s < 5 ? 1 : lerp(1, 0.4, smooth((s - 5) / 1.5)));
const speed = (() => { const c = []; let v = 0; for (let k = 0; k <= L * 100; k++) { const s = k / 100; const target = s >= ON && s < OFF ? 0.25 + 0.75 * loadAt(s) : 0; v += (target - v) * (target > v ? 0.012 : 0.008); c.push(v); } return c; })();
const angle = (() => { const c = []; let a = 0; for (let k = 0; k <= L * 100; k++) { c.push(a); a += speed[k] * 24 * 20 / 100; } return (s) => c[Math.min(c.length - 1, Math.round(s * 100))]; })();
const litAt = (s) => s >= ON && s < OFF + 2; // stays lit 2 s after the last power made
const motorOn = (s) => s >= 2 && s < 8;
const shaftAt = (s) => (Math.min(Math.max(s, 2), 8) - 2) * 360; // held after it stops, so it never swings back
// approved sounds: each block's cues become sound keyframes on the clip's Effects track, pointing at the real OGG files
// (the lava pop is Minecraft's own; its preview file is made locally from the game assets and git-ignored)
const SOUNDS = ['lava-generator', 'electric-motor'].map((slug) => {
  const dir = path.resolve(__dirname, '../sound', slug);
  return { dir, cues: JSON.parse(fs.readFileSync(path.join(dir, slug + '.cues.json'), 'utf8')) };
});
function sounds(clipName) {
  return SOUNDS.flatMap(({ dir, cues }) => {
    const list = cues.clips[clipName] || [];
    const missing = [...new Set(list.map(([, id]) => id).filter((id) => !fs.existsSync(path.join(dir, 'real', id + '.ogg'))))];
    if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
    return list.map(([t, id]) => [t, cues.namespace + '.' + id, path.join(dir, 'real', id + '.ogg').split(path.sep).join('/')]);
  }).sort((a, b) => a[0] - b[0]);
}
const FACINGS = { motor_down: { rotation: [[0, [-90, 0, 0]]] }, motor_up: { rotation: [[0, [90, 0, 0]]] } };
clip('run', L, 'loop', {
  gen_rotor: { rotation: (s) => [0, 0, -angle(s) % 360] },
  gen_off: { scale: () => HIDE },
  gen_full: { scale: (s) => (litAt(s) ? HIDE : SHOW) },
  gen_lit: { scale: (s) => (litAt(s) ? SHOW : HIDE) },
  mot_off: { scale: (s) => (motorOn(s) ? HIDE : SHOW) },
  mot_on: { scale: (s) => (motorOn(s) ? SHOW : HIDE) },
  mot_shaft: { rotation: (s) => [0, 0, -shaftAt(s)] },
  ...Object.fromEntries(['down', 'up'].flatMap((n) => [[`${n}_off`, { scale: (s) => (motorOn(s) ? HIDE : SHOW) }], [`${n}_on`, { scale: (s) => (motorOn(s) ? SHOW : HIDE) }],
    [`${n}_shaft`, { rotation: (s) => [0, 0, -shaftAt(s)] }]])),
}, FACINGS, sounds('run'));
clip('idle', 1, 'hold', {}, { gen_off: { scale: [[0, SHOW]] }, gen_full: { scale: [[0, HIDE]] }, gen_lit: { scale: [[0, HIDE]] }, mot_off: { scale: [[0, SHOW]] }, mot_on: { scale: [[0, HIDE]] },
  down_off: { scale: [[0, SHOW]] }, down_on: { scale: [[0, HIDE]] }, up_off: { scale: [[0, SHOW]] }, up_on: { scale: [[0, HIDE]] }, ...FACINGS }, sounds('idle'));

write(OUT, outliner);
module.exports = { generator, rotor, motor, shaftStub, GEN, MOT, GAX, LAYER };
