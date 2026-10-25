// Fluid Lines and Power Lines showcase generator -> design/models/fluid-and-power-lines.bbmodel (Generic Model preview).
// To design/briefs/fluid-and-power-lines.md. Every piece is built in the game's model space of the block it belongs to
// (x 0 west -> 16 east, y up, z 0 north -> 16 south) and placed into scene rows by block position.
//
// Fluid Lines: a 4 px pipe made round from crossed boxes, a 1 px coupling ring at each joint, round hubs at bends,
// a recessed sight slot along the top with the fluid in it. Power Lines: thin strands lying on the face they're placed
// on, clipped down at every block, folding round corners. Poles are stacked Line Posts with branches on the head.
const path = require('path');
const fs = require('fs');
const { C, hash, png, textureSet, preview } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'fluid-and-power-lines.bbmodel');

// ---------- palette ----------
// Create's own copper (sampled from its fluid tank), clean; the generators' mid brass, no bright highlights
const CU = { edge: '#904931', shade: '#9a5038', mid: '#a75a40', field: '#b26247', lit: '#c26b4c', ring: '#9a5038', ringLo: '#904931' };
const BR = { lo: '#7e6832', field: '#8f7638', lit: '#a3873f', ring: '#7e6832', ringLo: '#76612e' };
const STEEL = ['#2f343b', '#3c424a', '#4a5059'];
const CERAMIC = ['#7d786f', '#8f897e', '#9c968b'];
const RED = '#7a2a22';
// The 16 dyes: vanilla's colours, about 40% less saturated and kept at mid value or below
const VANILLA_DYES = { white: '#f9fffe', orange: '#f9801d', magenta: '#c74ebd', light_blue: '#3ab3da', yellow: '#fed83d', lime: '#80c71f',
  pink: '#f38baa', gray: '#474f52', light_gray: '#9d9d97', cyan: '#169c9c', purple: '#8932b8', blue: '#3c44aa', brown: '#835432',
  green: '#5e7c16', red: '#b02e26', black: '#1d1d21' };
function muted(hex) {
  let [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b);
  let h = 0, s = 0, l = (mx + mn) / 2;
  if (mx !== mn) {
    const d = mx - mn;
    s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
    h = mx === r ? (g - b) / d + (g < b ? 6 : 0) : mx === g ? (b - r) / d + 2 : (r - g) / d + 4;
    h /= 6;
  }
  // keep the hue, take 60% of the colour's chroma (so near-whites stay grey), and bring the lightness to mid or below
  const chroma = mx - mn;
  l = Math.min(0.54, 0.16 + l * 0.42);
  s = Math.min(1, (0.6 * chroma) / (1 - Math.abs(2 * l - 1)));
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
  const f = (t) => { t = (t + 1) % 1; return t < 1 / 6 ? p + (q - p) * 6 * t : t < 1 / 2 ? q : t < 2 / 3 ? p + (q - p) * (2 / 3 - t) * 6 : p; };
  return '#' + [f(h + 1 / 3), f(h), f(h - 1 / 3)].map((v) => Math.round(v * 255).toString(16).padStart(2, '0')).join('');
}
const DYES = Object.fromEntries(Object.entries(VANILLA_DYES).map(([k, v]) => [k, muted(v)]));
const darker = (hex, k) => '#' + [1, 3, 5].map((i) => Math.round(parseInt(hex.slice(i, i + 2), 16) * k).toString(16).padStart(2, '0')).join('');

// ---------- textures ----------
const T = textureSet(); T.file = 'line_';
const flat = (a, b, seed, p = 0.12) => (x, y) => (hash(x, y, seed) > 1 - p ? b : a);
T.paint('copper', flat(CU.field, CU.mid, 1));
T.paint('copper_ring', flat(CU.ring, CU.ringLo, 2, 0.2));
T.paint('brass', flat(BR.field, BR.lo, 3));
T.paint('brass_ring', flat(BR.ring, BR.ringLo, 4, 0.2));
T.paint('steel', flat(STEEL[1], STEEL[2], 5, 0.1));
T.paint('steel_dark', flat(STEEL[0], STEEL[1], 6, 0.1));
T.paint('wood', (x, y) => (x % 3 === 0 ? '#4e3626' : hash(x, y, 31) > 0.86 ? '#6f4d36' : '#634532'));
T.paint('wood_h', (x, y) => (y % 3 === 0 ? '#4e3626' : hash(x, y, 32) > 0.86 ? '#6f4d36' : '#634532'));
T.paint('wood_end', (x, y) => (Math.hypot(x - 7.5, y - 7.5) % 3 < 1 ? '#4e3626' : '#6f4d36'));
T.paint('ceramic', (x, y) => (y % 3 === 0 ? CERAMIC[2] : hash(x, y, 7) > 0.85 ? CERAMIC[0] : CERAMIC[1]));
T.paint('red', flat(RED, darker(RED, 0.85), 8));
T.paint('slot_idle', () => '#1f2226');
// the brass twin flex: two strands with a twist mark every 4 px
T.paint('brass_twin', (x, y) => ((x + y) % 4 === 0 ? BR.lo : BR.field));
for (const [k, v] of Object.entries(DYES)) T.paint('dye_' + k, flat(v, darker(v, 0.88), 9 + k.length));
// The gauge: a 6 px dial at u 5-11, v 0-6, brass rim, mid-grey face, five dark ticks
T.paint('gauge', (x, y) => {
  const dx = x + 0.5 - 8, dy = y + 0.5 - 3, r = Math.hypot(dx, dy);
  if (y > 5 || x < 5 || x > 10) return BR.field;
  if (r > 2.6) return BR.field;
  if ((y === 1 && (x === 6 || x === 9)) || (y === 0 && (x === 7 || x === 8)) || (y === 2 && (x === 5 || x === 10))) return '#3a3f46';
  return '#8a8c86';
});
// the gauge lying flat on a wire (same dial, read from above: u = x, v = z)
T.paint('gauge_top', (x, y) => {
  const r = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
  if (r > 2.7) return BR.field;
  if ((y === 6 && (x === 6 || x === 9)) || (y === 5 && (x === 7 || x === 8))) return '#3a3f46';
  return '#8a8c86';
});
// The Splitter dial marks: Even (a fork), Nearest first (one arrow), Round robin (a curl)
const MARK = {
  even: ['....', '.x.x', 'xx..', '.x.x'], nearest: ['..x.', 'xxxx', '..x.', '....'], robin: ['xxx.', 'x.x.', 'x.xx', '..x.'],
};
for (const [k, m] of Object.entries(MARK)) T.paint('dial_' + k, (x, y) => (x >= 6 && x < 10 && y >= 6 && y < 10 && m[y - 6][x - 6] === 'x' ? STEEL[0] : '#5f656e'));
// Filter mesh windows, tinted with the filter's fluid
const FLUID = { lava: ['#c76a2a', '#d98a3e', '#a9501f'], water: ['#3f6b8f', '#5582a6', '#335a7a'] };
for (const [k, f] of Object.entries(FLUID)) T.paint('mesh_' + k, (x, y) => (x % 2 === 0 || y % 2 === 0 ? STEEL[0] : f[0]));

// The fluid in the sight slot: an animated strip (4 frames, each slid 4 px along the pipe), lit like the pipe
function flowStrip(f) {
  const frames = 4, d = new Uint8Array(16 * 16 * frames * 4);
  for (let fr = 0; fr < frames; fr++) for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const u = (x - fr * 4 + 64) % 16;
    const c = u % 8 < 2 ? f[1] : u % 8 > 5 ? f[2] : f[0];
    const i = ((fr * 16 + y) * 16 + x) * 4;
    d.set([1, 3, 5].map((k) => parseInt(c.slice(k, k + 2), 16)).concat(255), i);
  }
  return png(16, 16 * frames, d);
}
const ANIMATED = {};
for (const k of Object.keys(FLUID)) { T.OWN['flow_' + k] = flowStrip(FLUID[k]); ANIMATED['line_flow_' + k + '.png'] = 4; }

// ---------- geometry helpers ----------
// A box along an axis: a0-a1 on the axis, p0-p1 and q0-q1 on the other two (x: y, z / y: x, z / z: x, y)
function ab(axis, a0, a1, p0, p1, q0, q1) {
  if (axis === 'x') return [[a0, p0, q0], [a1, p1, q1]];
  if (axis === 'y') return [[p0, a0, q0], [p1, a1, q1]];
  return [[p0, q0, a0], [p1, q1, a1]];
}
const END = { x: ['west', 'east'], y: ['down', 'up'], z: ['north', 'south'] };
// A round rod along an axis: two crossed boxes of width w (w x w-2n and w-2n x w, n = notch), its end faces left out
// unless asked for. Every other face takes tex.
function rod(name, axis, a0, a1, w, tex, o = {}) {
  const n = o.notch ?? (w >= 4 ? 1 : w / 4), c = 8, h = w / 2;
  const [lo, hi] = END[axis];
  const faces = { all: tex, [lo]: o.capLo ? tex : null, [hi]: o.capHi ? tex : null, ...(o.faces || {}) };
  const out = [];
  const [f1, t1] = ab(axis, a0, a1, c - h, c + h, c - h + n, c + h - n);
  const [f2, t2] = ab(axis, a0, a1, c - h + n, c + h - n, c - h, c + h);
  out.push(C(name + '_a', f1, t1, faces), C(name + '_b', f2, t2, faces));
  if (w >= 4) { const m = h - n / 2; const [f3, t3] = ab(axis, a0, a1, c - m, c + m, c - m, c + m); out.push(C(name + '_c', f3, t3, faces)); }
  return out;
}
// a round hub: three crossed boxes
function hub(name, w, tex) {
  const h = w / 2, n = 1;
  return [
    C(name + '_x', [8 - h, 8 - h + n, 8 - h + n], [8 + h, 8 + h - n, 8 + h - n], { all: tex }),
    C(name + '_y', [8 - h + n, 8 - h, 8 - h + n], [8 + h - n, 8 + h, 8 + h - n], { all: tex }),
    C(name + '_z', [8 - h + n, 8 - h + n, 8 - h], [8 + h - n, 8 + h - n, 8 + h], { all: tex }),
  ];
}
const AX = { east: ['x', 1], west: ['x', -1], up: ['y', 1], down: ['y', -1], south: ['z', 1], north: ['z', -1] };
// One arm of a pipe, from the middle of the block (or the hub's edge) out to the face, with its half coupling ring
// at the face. A slot only on horizontal arms: a recessed groove along the top with the fluid strip in it.
function pipeArm(dir, tier, { from = 8, fluid = null, slot = true, ring = true } = {}) {
  const [axis, s] = AX[dir];
  const body = tier, ringT = tier + '_ring';
  const span = s > 0 ? [from, 16] : [0, 16 - from];
  const out = [];
  const name = `${tier}_${dir}`;
  const horiz = axis !== 'y' && slot;
  // the half ring at the joint (0.5 px each side makes one 1 px ring between two pieces)
  // a subtle coupling: half a pixel proud of the pipe, a quarter pixel each side of the joint
  const rA = s > 0 ? 15.75 : 0, rB = s > 0 ? 16 : 0.25;
  if (ring) out.push(...rod(name + '_ring', axis, rA, rB, 5, ringT, { capLo: s > 0, capHi: s < 0 }));
  const b0 = s > 0 ? span[0] : (ring ? 0.25 : 0), b1 = s > 0 ? (ring ? 15.75 : 16) : span[1];
  // box a: the wide-and-flat one (4 across, 2 tall); box b: the tall one (2 across, 4 tall), with the slot cut in its top
  const [fa, ta] = ab(axis, b0, b1, ...(axis === 'x' ? [7, 9, 6, 10] : axis === 'z' ? [6, 10, 7, 9] : [6, 10, 7, 9]));
  out.push(C(name + '_a', fa, ta, { all: body, [END[axis][0]]: null, [END[axis][1]]: null }));
  // the corner-filling square that makes the 4 px pipe read round
  { const [fc, tc] = ab(axis, b0, b1, 6.5, 9.5, 6.5, 9.5); out.push(C(name + '_c', fc, tc, { all: body, [END[axis][0]]: null, [END[axis][1]]: null })); }
  if (!horiz) {
    const [fb, tb] = ab(axis, b0, b1, ...(axis === 'x' ? [6, 10, 7, 9] : axis === 'z' ? [7, 9, 6, 10] : [7, 9, 6, 10]));
    out.push(C(name + '_b', fb, tb, { all: body, [END[axis][0]]: null, [END[axis][1]]: null }));
    return out;
  }
  // the slot: 1.5 px of full-height pipe at each end, a groove in between with the fluid strip 0.25 px below the rim
  const g0 = b0 + 1.5, g1 = b1 - 1.5;
  out.push(C(name + '_b0', ...reorder(axis, b0, g0, 6, 10, 7, 9), { all: body, [END[axis][0]]: null, [END[axis][1]]: null }));
  out.push(C(name + '_b1', ...reorder(axis, g1, b1, 6, 10, 7, 9), { all: body, [END[axis][0]]: null, [END[axis][1]]: null }));
  out.push(C(name + '_bm', ...reorder(axis, g0, g1, 6, 9.25, 7, 9), { all: body, up: null, [END[axis][0]]: null, [END[axis][1]]: null }));
  out.push(C(name + '_fluid', ...reorder(axis, g0, g1, 9.25, 9.75, 7.25, 8.75),
    { up: fluid ? 'flow_' + fluid : 'slot_idle', all: null }, { uv: { up: axis === 'x' ? [0, 7, 16, 8.5] : [7, 0, 8.5, 16] } }));
  return out;
}
// The tall box of a horizontal pipe: 2 across, from y lo to hi (the cross axis is z for x pipes, x for z pipes)
function reorder(axis, a0, a1, y0, y1, c0, c1) {
  return axis === 'x' ? [[a0, y0, c0], [a1, y1, c1]] : [[c0, y0, a0], [c1, y1, a1]];
}
// A straight pipe through the block along an axis ('x' or 'z' or 'y')
function straight(axis, tier, o = {}) {
  const [lo, hi] = axis === 'x' ? ['west', 'east'] : axis === 'z' ? ['north', 'south'] : ['down', 'up'];
  // one arm across the whole block: build it as the 'hi' arm from 0, with a ring at each end
  const parts = pipeArm(hi, tier, { ...o, from: 0.25 });
  const [ax] = AX[lo];
  parts.push(...rod(`${tier}_${lo}_ring`, ax, 0, 0.25, 5, tier + '_ring', { capHi: true }));
  return parts;
}
// A bend, T or end: a hub plus an arm to each connected side
function junction(dirs, tier, o = {}) {
  const out = hub(tier + '_hub', 6, tier + '_ring');
  // an arm starts at the hub's edge: 11 from its own far side either way
  for (const d of dirs) out.push(...pipeArm(d, tier, { ...o, from: 11 }));
  return out;
}
// The flange at a container: an 8 px plate 1 px thick on the face (dir: the side the container is on)
function flange(dir, tex) {
  const [axis, s] = AX[dir];
  return rod('flange_' + dir, axis, s > 0 ? 15 : 0, s > 0 ? 16 : 1, 8, tex, { capLo: s > 0, capHi: s < 0, notch: 1.5 });
}

// ---------- fittings (on a straight x pipe, centred) ----------
// The Intake, on the west side: a brass flange on the tank, a dark-steel collar with its filter window on top
function intake(tier, filterFluid, fluid) {
  const out = [...flange('west', 'brass')];
  out.push(...rod('intake_collar', 'x', 1, 5, 6, 'steel', { capHi: true }));
  out.push(C('intake_window', [1.75, 11.25, 7], [4.25, 11.25, 9], { up: 'mesh_' + filterFluid, all: null }, { uv: { up: [0, 0, 2.5, 2] } }));
  out.push(...pipeArm('east', tier, { from: 5, fluid }));
  return out;
}
// The Valve: a round steel body, a stem and an octagonal hand wheel (its own group, turning on the stem)
function valve(tier, fluid) {
  const out = [...pipeArmPair(tier, 5.5, 10.5, fluid), ...rod('valve_body', 'x', 5.5, 10.5, 6, 'steel', { capLo: true, capHi: true }),
    C('valve_stem', [7.5, 11, 7.5], [8.5, 14, 8.5], { all: 'steel_dark', down: null })];
  const wheel = [C('wheel_hub', [7.25, 14, 7.25], [8.75, 15, 8.75], { all: 'steel_dark' })];
  const R = 3.5, ap = R * Math.cos(Math.PI / 8), side = 2 * R * Math.sin(Math.PI / 8) + 0.1;
  // eight rim bars round an octagon: the ones on the axes lie along their tangent, the diagonal ones turn 45 degrees
  for (let k = 0; k < 8; k++) {
    const a = (k * Math.PI) / 4, cx = 8 + ap * Math.cos(a), cz = 8 + ap * Math.sin(a);
    const alongZ = k === 0 || k === 4;
    const from = alongZ ? [cx - 0.45, 14, cz - side / 2] : [cx - side / 2, 14, cz - 0.45], to = alongZ ? [cx + 0.45, 15, cz + side / 2] : [cx + side / 2, 15, cz + 0.45];
    const o = k % 2 ? { rot: ['y', k === 1 || k === 5 ? 45 : -45, [cx, 14.5, cz]] } : {};
    wheel.push(C('wheel_rim_' + k, from, to, { all: 'red' }, o));
  }
  // spokes: a cross
  wheel.push(C('wheel_spoke_x', [5, 14.2, 7.6], [11, 14.8, 8.4], { all: 'red' }), C('wheel_spoke_z', [7.6, 14.2, 5], [8.4, 14.8, 11], { all: 'red' }));
  return { parts: out, wheel };
}
// the pipe either side of a fitting body (a0-a1 on x)
function pipeArmPair(tier, a0, a1, fluid) {
  return [...pipeArm('west', tier, { from: 16 - a0, fluid }), ...pipeArm('east', tier, { from: a1, fluid })];
}
function filter(tier, filterFluid, fluid) {
  return [...pipeArmPair(tier, 5, 11, fluid), ...rod('filter_can', 'x', 5, 11, 6, 'steel', { capLo: true, capHi: true }),
    C('filter_band_top', [6, 11.25, 7], [10, 11.25, 9], { up: 'mesh_' + filterFluid, all: null }, { uv: { up: [0, 0, 4, 2] } }),
    C('filter_band_side', [6, 7, 11.25], [10, 9, 11.25], { south: 'mesh_' + filterFluid, all: null }, { uv: { south: [0, 0, 4, 2] } })];
}
// The Meter on a pipe: a ring housing, a stem and a round gauge facing south, its needle a group pivoting at the centre
function meterPipe(tier, fluid) {
  const parts = [...pipeArmPair(tier, 6.5, 9.5, fluid), ...rod('meter_ring', 'x', 6.5, 9.5, 5, 'steel', { capLo: true, capHi: true }),
    C('meter_stem', [7.25, 10.5, 7.25], [8.75, 11.5, 8.75], { all: 'steel_dark', down: null }),
    C('gauge_a', [5, 12, 7.25], [11, 16, 8.75], { all: 'brass', south: 'gauge' }, { uv: { south: [5, 1, 11, 5] } }),
    C('gauge_b', [6, 11, 7.25], [10, 17, 8.75], { all: 'brass', south: 'gauge' }, { uv: { south: [6, 0, 10, 6] } })];
  const needle = [C('needle', [7.75, 14, 9], [8.25, 16.2, 9], { south: 'red', all: null }, { uv: { south: [0, 0, 1, 2] } })];
  return { parts, needle, pivot: [8, 14, 9] };
}
// The Splitter on a pipe: a round hub with a mode dial on top (its own group)
function splitterPipe(tier, fluid, mode) {
  const parts = [...pipeArmPair(tier, 5, 11, fluid), ...hub('splitter_hub', 6, 'steel')];
  const dial = [C('dial_a', [6, 11, 7], [10, 11.75, 9], { all: 'steel', up: 'dial_' + mode, down: null }),
    C('dial_b', [7, 11, 6], [9, 11.75, 10], { all: 'steel', up: 'dial_' + mode, down: null })];
  return { parts, dial };
}

// ---------- wires (attached to a face) ----------
// tier: 'copper' | 'brass'; dye: null or a dye name (a covered cable: thicker, in the dye colour)
function wireSpec(tier, dye) {
  if (dye) return tier === 'brass' ? { w: 2.25, h: 1.75, tex: 'dye_' + dye } : { w: 1.75, h: 1.75, tex: 'dye_' + dye };
  return tier === 'brass' ? { w: 1.5, h: 0.75, tex: 'brass_twin' } : { w: 1, h: 1, tex: 'copper' };
}
// A floor wire (on the block's down face) along x or z between a0 and a1
function floorRun(axis, a0, a1, ws, name = 'wire') {
  const h = ws.w / 2;
  const from = axis === 'x' ? [a0, 0, 8 - h] : [8 - h, 0, a0], to = axis === 'x' ? [a1, ws.h, 8 + h] : [8 + h, ws.h, a1];
  return C(name, from, to, { all: ws.tex, down: null, [END[axis][0]]: a0 <= 0 ? null : ws.tex, [END[axis][1]]: a1 >= 16 ? null : ws.tex });
}
// A wall wire on a block's north face (z 0), along y or x
function wallRun(axis, a0, a1, ws, name = 'wire_wall', at = 8) {
  const h = ws.w / 2;
  const from = axis === 'y' ? [8 - h, a0, 0] : [a0, at - h, 0], to = axis === 'y' ? [8 + h, a1, ws.h] : [a1, at + h, ws.h];
  return C(name, from, to, { all: ws.tex, north: null, [END[axis][0]]: null, [END[axis][1]]: null });
}
// A wire on a block's east or west face (x 16 or x 0), along y
function sideRun(side, a0, a1, ws, name) {
  const h = ws.w / 2;
  const [x0, x1] = side === 'east' ? [16 - ws.h, 16] : [0, ws.h];
  return C(name, [x0, a0, 8 - h], [x1, a1, 8 + h], { all: ws.tex, [side]: null, down: null, up: null });
}
const CLIP = { copper: 'copper_ring', brass: 'brass_ring' };
// A clip across a floor wire at a (x or z)
function floorClip(axis, a, ws, tier, name = 'clip') {
  const h = ws.w / 2 + 0.5;
  return axis === 'x' ? C(name, [a - 0.5, 0, 8 - h], [a + 0.5, ws.h + 0.5, 8 + h], { all: CLIP[tier], down: null })
    : C(name, [8 - h, 0, a - 0.5], [8 + h, ws.h + 0.5, a + 0.5], { all: CLIP[tier], down: null });
}
function wallClip(axis, a, ws, tier, name = 'clip_wall', at = 8) {
  const h = ws.w / 2 + 0.5;
  return axis === 'y' ? C(name, [8 - h, a - 0.5, 0], [8 + h, a + 0.5, ws.h + 0.5], { all: CLIP[tier], north: null })
    : C(name, [a - 0.5, at - h, 0], [a + 0.5, at + h, ws.h + 0.5], { all: CLIP[tier], north: null });
}
// The splice dome at a junction
function splice(ws, tier) {
  const t = ws.h + 0.75;
  return [C('splice_a', [6.5, 0, 7], [9.5, t, 9], { all: CLIP[tier], down: null }), C('splice_b', [7, 0, 6.5], [9, t, 9.5], { all: CLIP[tier], down: null })];
}
// The Breaker on a floor wire along x: a round steel switch with a lever (its own group, rocking about z)
function breaker(ws) {
  const parts = [C('breaker_a', [5.5, 0, 6.5], [10.5, 3, 9.5], { all: 'steel', down: null }), C('breaker_b', [6.5, 0, 5.5], [9.5, 3, 10.5], { all: 'steel', down: null })];
  const lever = [C('lever', [7.5, 3, 7.5], [8.5, 6.5, 8.5], { all: 'steel_dark', down: null }), C('knob', [7.25, 6.5, 7.25], [8.75, 8, 8.75], { all: 'red' })];
  return { parts, lever, pivot: [8, 3, 8] };
}
// The Meter on a floor wire: a low round gauge, dial up, its needle a group turning about y
function meterWire() {
  const parts = [C('wmeter_a', [5, 0, 6], [11, 1.5, 10], { all: 'brass', up: 'gauge_top', down: null }), C('wmeter_b', [6, 0, 5], [10, 1.5, 11], { all: 'brass', up: 'gauge_top', down: null })];
  const needle = [C('wneedle', [7.75, 1.75, 6], [8.25, 1.75, 8.25], { up: 'red', all: null }, { uv: { up: [0, 0, 1, 2] } })];
  return { parts, needle, pivot: [8, 1.75, 8] };
}
function splitterWire(mode) {
  const parts = [C('wsplit_a', [5.5, 0, 6.5], [10.5, 1.75, 9.5], { all: 'steel', down: null }), C('wsplit_b', [6.5, 0, 5.5], [9.5, 1.75, 10.5], { all: 'steel', down: null })];
  const dial = [C('wdial', [6.5, 1.75, 6.5], [9.5, 2.25, 9.5], { all: 'steel', up: 'dial_' + mode, down: null })];
  return { parts, dial };
}

// ---------- Line Posts ----------
// Wooden poles with no tier of their own (the user's call); each insulator's terminal cap takes the tier of the cable tied
// to it. role: 'single' | 'foot' | 'pole' | 'head'; branches: list of 'north'|'south'|'east'|'west' (head or single only)
function postBlock(role, tier, branches = []) {
  const out = [];
  const hasFoot = role === 'single' || role === 'foot', hasHead = role === 'single' || role === 'head';
  // the foot: an iron bracket plate bolted to the face
  if (hasFoot) out.push(C('foot_a', [5, 0, 6], [11, 1.5, 10], { all: 'steel_dark', down: null }), C('foot_b', [6, 0, 5], [10, 1.5, 11], { all: 'steel_dark', down: null }));
  const p0 = hasFoot ? 1.5 : 0, p1 = hasHead ? 10.5 : 16;
  out.push(C('pole', [6.5, p0, 6.5], [9.5, p1, 9.5], { all: 'wood', down: null, up: hasHead ? 'wood_end' : null }));
  // plain wood the whole way up: no bands at the block joints (the user's call)
  if (hasHead) {
    out.push(...insulator(8, 8, 10.5, 1, 'head', tier));
    for (const b of branches) out.push(...branch(b, tier));
  }
  return out;
}
// A ceramic insulator stack centred at (cx, cz) from y0: disc, neck, disc, then the terminal cap in the cable's tier
function insulator(cx, cz, y0, k, name, tier) {
  const disc = 5 * k, neck = 3 * k, cap = 2;
  const ring = (nm, y, w, h, tex) => {
    const a = w / 2, n = w >= 4 ? 1 : 0.5;
    return [C(nm + '_a', [cx - a, y, cz - a + n], [cx + a, y + h, cz + a - n], { all: tex }), C(nm + '_b', [cx - a + n, y, cz - a], [cx + a - n, y + h, cz + a], { all: tex })];
  };
  const h = 1.5 * k;
  let y = y0;
  const out = [];
  out.push(...ring(name + '_d1', y, disc, h, 'ceramic')); y += h;
  out.push(...ring(name + '_n1', y, neck, h, 'ceramic')); y += h;
  out.push(...ring(name + '_d2', y, disc, h, 'ceramic')); y += h;
  out.push(C(name + '_cap', [cx - cap / 2, y, cz - cap / 2], [cx + cap / 2, y + 1.5, cz + cap / 2], { all: tier, down: null }));
  return out;
}
// tie point of an insulator (top of its cap), in block space
const headTie = () => [8, 10.5 + 4.5 + 1.5, 8];
const branchTie = (b) => { const [dx, dz] = { east: [5.5, 0], west: [-5.5, 0], south: [0, 5.5], north: [0, -5.5] }[b]; return [8 + dx, 10 + 3.6 + 1.5, 8 + dz]; };
// A wooden crossarm from the pole out to 7 px from the centre, an iron brace under it at 45 degrees, a smaller insulator on the tip
function branch(b, tier) {
  const s = b === 'east' || b === 'south' ? 1 : -1, axis = b === 'east' || b === 'west' ? 'x' : 'z';
  const a0 = s > 0 ? 9.5 : 1, a1 = s > 0 ? 15 : 6.5;
  const arm = axis === 'x' ? C('arm_' + b, [a0, 8, 7], [a1, 10, 9], { all: 'wood_h', east: 'wood_end', west: 'wood_end', [s > 0 ? 'west' : 'east']: null })
    : C('arm_' + b, [7, 8, a0], [9, 10, a1], { all: 'wood_h', up: 'wood', down: 'wood', north: 'wood_end', south: 'wood_end', [s > 0 ? 'north' : 'south']: null });
  const bc = 8 + s * 3.1, L = 4;
  const brace = axis === 'x' ? C('brace_' + b, [bc - L / 2, 6, 7.5], [bc + L / 2, 7, 8.5], { all: 'steel_dark' }, { rot: ['z', s * 45, [bc, 6.5, 8]] })
    : C('brace_' + b, [7.5, 6, bc - L / 2], [8.5, 7, bc + L / 2], { all: 'steel_dark' }, { rot: ['x', -s * 45, [8, 6.5, bc]] });
  const [tx, , tz] = branchTie(b);
  return [arm, brace, ...insulator(tx, tz, 10, 0.8, 'tip_' + b, tier)];
}

// ---------- preview ----------
const { cube, group, clip, write } = preview('fluid-and-power-lines', { 'line:': T });
const outliner = [];
const placed = (list, bx, by, bz, row) => list.map((c) => cube(c, [bx * 16 + row[0], by * 16 + row[1], bz * 16 + row[2]]));
const blockOf = (name, list, bx, by, bz, row, extra = []) => group(name, [8, 0, 8], [bx * 16 + row[0], by * 16 + row[1], bz * 16 + row[2]], [...placed(list, bx, by, bz, row), ...extra]);
// a moving part: its own group about a pivot, inside the block's group
const mover = (name, list, pivot, bx, by, bz, row) => group(name, pivot, [bx * 16 + row[0], by * 16 + row[1], bz * 16 + row[2]], placed(list, bx, by, bz, row));
const stand = (name, tex, bx, by, bz, row, top = tex) => blockOf(name, [C(name, [0, 0, 0], [16, 16, 16], { all: tex, up: top, down: top })], bx, by, bz, row);

const MOVERS = { wheels: [], needles: [], wneedles: [], levers: [], dials: [], wdials: [] };
// Fluid row: tank, Intake, straight, Meter, Valve, Filter, Splitter, T (north branch up a bend), straight, flange into a basin
function fluidRow(tier, fluid, row, label) {
  const kids = [];
  kids.push(stand(label + '_tank', 'create:block/fluid_tank', 0, 0, 0, row, 'create:block/fluid_tank_top'));
  kids.push(blockOf(label + '_intake', intake(tier, fluid, fluid), 1, 0, 0, row));
  kids.push(blockOf(label + '_straight', straight('x', tier, { fluid }), 2, 0, 0, row));
  const m = meterPipe(tier, fluid);
  const nd = mover(label + '_meter_needle', m.needle, m.pivot, 3, 0, 0, row); MOVERS.needles.push(label + '_meter_needle');
  kids.push(blockOf(label + '_meter', m.parts, 3, 0, 0, row, [nd]));
  const v = valve(tier, fluid);
  const wh = mover(label + '_valve_wheel', v.wheel, [8, 14.5, 8], 4, 0, 0, row); MOVERS.wheels.push(label + '_valve_wheel');
  kids.push(blockOf(label + '_valve', v.parts, 4, 0, 0, row, [wh]));
  kids.push(blockOf(label + '_filter', filter(tier, fluid, fluid), 5, 0, 0, row));
  const sp = splitterPipe(tier, fluid, 'even');
  const dl = mover(label + '_splitter_dial', sp.dial, [8, 11, 8], 6, 0, 0, row); MOVERS.dials.push(label + '_splitter_dial');
  kids.push(blockOf(label + '_splitter', sp.parts, 6, 0, 0, row, [dl]));
  kids.push(blockOf(label + '_tee', junction(['west', 'east', 'north'], tier, { fluid }), 7, 0, 0, row));
  // the branch: north, a bend up, a vertical piece and a capped end (this branch is idle)
  kids.push(blockOf(label + '_bend', junction(['south', 'up'], tier, {}), 7, 0, -1, row));
  kids.push(blockOf(label + '_riser', straight('y', tier, {}), 7, 1, -1, row));
  kids.push(blockOf(label + '_cap', [...pipeArm('down', tier, { from: 10.5 }), ...hub(tier + '_endcap', 5, tier + '_ring')], 7, 2, -1, row));
  kids.push(blockOf(label + '_straight2', straight('x', tier, { fluid }), 8, 0, 0, row));
  kids.push(blockOf(label + '_end', endPiece(tier, fluid), 9, 0, 0, row));
  kids.push(stand(label + '_basin', 'create:block/basin', 10, 0, 0, row));
  outliner.push(group(label, [8, 0, 8], row, kids));
}
// the end piece: arm from the west edge to the flange
function endPiece(tier, fluid) { return [...pipeArm('east', tier, { from: 0.25, fluid, ring: false }), ...rod(tier + '_west_ring', 'x', 0, 0.25, 5, tier + '_ring', { capHi: true }), ...flange('east', tier + '_ring')]; }

fluidRow('copper', 'lava', [0, 0, 0], 'copper_fluid');
fluidRow('brass', 'water', [0, 0, 64], 'brass_fluid');
// replace the end pieces with the proper one (an arm across to the flange)
// (built separately to keep the row readable)

// Dye row: two-block runs in eight dyes, and a wrenched join between two colours
function dyedPipe(tier, dye, bx, row, name) {
  // a dyed pipe is the dye colour all over; its couplings stay the tier's darker shade
  const swap = (list) => list.map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([k, v]) => [k, v === tier ? 'dye_' + dye : v])) }));
  return blockOf(name, swap(straight('x', tier, { fluid: 'water' })), bx, 0, 0, row);
}
const DROW = [0, 0, 128];
const dyeKids = [];
['red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'blue', 'purple', 'white', 'black'].forEach((d, i) => {
  dyeKids.push(dyedPipe(i % 2 ? 'brass' : 'copper', d, i, DROW, 'pipe_' + d));
});
// wrenched join: red copper into blue copper, the ring split down the middle
{
  const bx = 11;
  dyeKids.push(dyedPipe('copper', 'red', bx, DROW, 'join_red'));
  dyeKids.push(dyedPipe('copper', 'blue', bx + 1, DROW, 'join_blue'));
  // the split ring sits a quarter pixel proud of the two half rings it covers
  dyeKids.push(blockOf('join_ring', [C('split_ring_red_a', [15.4, 4.75, 5.75], [16, 11.25, 10.25], { all: 'dye_red', east: null }), C('split_ring_red_b', [15.4, 5.75, 4.75], [16, 10.25, 11.25], { all: 'dye_red', east: null }),
    C('split_ring_blue_a', [16, 4.75, 5.75], [16.6, 11.25, 10.25], { all: 'dye_blue', west: null }), C('split_ring_blue_b', [16, 5.75, 4.75], [16.6, 10.25, 11.25], { all: 'dye_blue', west: null })], bx, 0, 0, DROW));
}
// encased: a copper pipe into three stone-brick blocks and out the far side
dyeKids.push(blockOf('encase_in', straight('x', 'copper', { fluid: 'lava' }), 14, 0, 0, DROW));
for (let k = 0; k < 3; k++) dyeKids.push(stand('encased_' + k, 'minecraft:block/stone_bricks', 15 + k, 0, 0, DROW));
dyeKids.push(blockOf('encase_out', straight('x', 'copper', { fluid: 'lava' }), 18, 0, 0, DROW));
outliner.push(group('dyes_and_encasing', [8, 0, 8], DROW, dyeKids));

// Wire scene (copper, undyed): a machine, along the floor, up and along a wall, down, a junction, a Breaker, a 3-high pole
// with two branches; spans to a 2-high pole with four; down over a step (inside and outside corners) into a machine
function wireScene(tier, dye, row, label, long = true) {
  const ws = wireSpec(tier, dye), kids = [];
  // the floor: one slab per block, only its top shows
  for (let x = 0; x < (long ? 16 : 8); x++) for (let z = 0; z < 3; z++) kids.push(blockOf(`${label}_floor_${x}_${z}`, [C('floor', [0, -2, 0], [16, 0, 16], { all: 'minecraft:block/smooth_stone' })], x, 0, z, row));
  kids.push(stand(label + '_source', 'minecraft:block/polished_andesite', 0, 0, 1, row));
  const W = (name, list, bx, by, bz) => kids.push(blockOf(`${label}_${name}`, list, bx, by, bz, row));
  // wall behind the run at z 0 (blocks z -1), x 2-4, two high
  for (let x = 2; x <= 4; x++) for (let y = 0; y < 2; y++) kids.push(stand(`${label}_wall_${x}_${y}`, 'minecraft:block/stone_bricks', x, y, 0, row));
  const Z = 1; // the run sits in the row of blocks at z 1, the wall is z 0
  // from the machine's terminal along the floor; a Splitter on the first block
  const sw = splitterWire('nearest');
  W('w1', [C('lug', [0, 0, 6.5], [1, 3, 9.5], { all: CLIP[tier], west: null }), floorRun('x', 1, 16, ws), ...sw.parts], 1, 0, Z);
  kids.push(mover(`${label}_wsplitter_dial`, sw.dial, [8, 2, 8], 1, 0, Z, row)); MOVERS.wdials.push(`${label}_wsplitter_dial`);
  // inside corner: floor west -> centre, north to the wall, up the wall (the wall is the block to the north)
  W('w2', [floorRun('x', 0, 8 + ws.w / 2, ws, 'wire_in'), floorRun('z', ws.h, 8 - ws.w / 2, ws, 'wire_north'), wallRun('y', 0, 16, ws), floorClip('x', 4, ws, tier), wallClip('y', 10, ws, tier)], 2, 0, Z);
  W('w3', [wallRun('y', 0, 8 + ws.w / 2, ws), wallRun('x', 8 + ws.w / 2, 16, ws, 'wire_along')], 2, 1, Z);
  W('w4', [wallRun('x', 0, 16, ws, 'wire_along'), wallClip('x', 8, ws, tier)], 3, 1, Z);
  W('w5', [wallRun('x', 0, 8 - ws.w / 2, ws, 'wire_along'), wallRun('y', 0, 8 + ws.w / 2, ws)], 4, 1, Z);
  W('w6', [wallRun('y', 0, 16, ws), floorRun('z', ws.h, 8 + ws.w / 2, ws, 'wire_out'), floorRun('x', 8 + ws.w / 2, 16, ws, 'wire_east'), wallClip('y', 8, ws, tier)], 4, 0, Z);
  // a junction with a branch south to a Meter and a second machine
  W('w7', [floorRun('x', 0, 16, ws), floorRun('z', 8 + ws.w / 2, 16, ws, 'wire_south'), ...splice(ws, tier)], 5, 0, Z);
  const wm = meterWire();
  W('w7s', [floorRun('z', 0, 16, ws), ...wm.parts], 5, 0, Z + 1);
  kids.push(mover(`${label}_wmeter_needle`, wm.needle, wm.pivot, 5, 0, Z + 1, row)); MOVERS.wneedles.push(`${label}_wmeter_needle`);
  W('w7e', [floorRun('z', 0, 15, ws), C('lug2', [6.5, 0, 15], [9.5, 3, 16], { all: CLIP[tier], south: null })], 5, 0, Z + 2);
  kids.push(stand(label + '_machine2', 'minecraft:block/polished_andesite', 5, 0, Z + 3, row));
  // the Breaker
  const br = breaker(ws);
  W('w8', [floorRun('x', 0, 16, ws), ...br.parts], 6, 0, Z);
  kids.push(mover(`${label}_breaker_lever`, br.lever, br.pivot, 6, 0, Z, row)); MOVERS.levers.push(`${label}_breaker_lever`);
  // pole A: 3 high, branches north and south; the wire runs in under its foot
  W('poleA_foot', [floorRun('x', 0, 8, ws), ...postBlock('foot', tier)], 7, 0, Z);
  W('poleA_mid', postBlock('pole', tier), 7, 1, Z);
  W('poleA_head', postBlock('head', tier, ['north', 'south']), 7, 2, Z);
  const tie = (b, bx, by, p) => { const t = p === 'head' ? headTie() : branchTie(p); return [bx * 16 + t[0] - 8 + row[0], by * 16 + t[1] + row[1], Z * 16 + t[2] - 8 + row[2]]; };
  if (!long) {
    // a single one-block post, with a span from pole A's head
    W('post_single', postBlock('single', tier), 11, 0, Z);
    kids.push(group(`${label}_spans`, [0, 0, 0], [0, 0, 0], span(tie(0, 7, 2, 'head'), tie(0, 11, 0, 'head'), ws, 'span_top')));
    return { kids, ws };
  }
  // pole B: 2 high, four branches
  W('poleB_foot', [floorRun('x', 8, 16, ws), ...postBlock('foot', tier)], 11, 0, Z);
  W('poleB_head', postBlock('head', tier, ['north', 'south', 'east', 'west']), 11, 1, Z);
  // spans: north to north, south to south, head to head
  kids.push(group(`${label}_spans`, [0, 0, 0], [0, 0, 0], [
    ...span(tie(0, 7, 2, 'north'), tie(0, 11, 1, 'north'), ws, 'span_n'),
    ...span(tie(0, 7, 2, 'south'), tie(0, 11, 1, 'south'), ws, 'span_s'),
    ...span(tie(0, 7, 2, 'head'), tie(0, 11, 1, 'head'), ws, 'span_top')]));
  // down over a step: floor east to an inside corner up the step's west face, over its top, an outside corner down its east face
  W('w12', [floorRun('x', 0, 16 - ws.h, ws), sideRun('east', 0, 16, ws, 'wire_up'), floorClip('x', 8, ws, tier)], 12, 0, Z);
  kids.push(stand(label + '_step', 'minecraft:block/stone_bricks', 13, 0, Z, row));
  W('w13', [floorRun('x', 0, 16, ws, 'wire_top'), floorClip('x', 8, ws, tier),
    C('fold_w', [-ws.h, 0, 8 - ws.w / 2], [0, ws.h, 8 + ws.w / 2], { all: ws.tex, down: null }), C('fold_e', [16, 0, 8 - ws.w / 2], [16 + ws.h, ws.h, 8 + ws.w / 2], { all: ws.tex, down: null })], 13, 1, Z);
  W('w14', [sideRun('west', 0, 16, ws, 'wire_down'), floorRun('x', ws.h, 15, ws), C('lug3', [15, 0, 6.5], [16, 3, 9.5], { all: CLIP[tier], east: null })], 14, 0, Z);
  kids.push(stand(label + '_motor', 'minecraft:block/polished_andesite', 15, 0, Z, row));
  return { kids, ws };
}
// A span: the wire's own cable sagging between two tie points (same z), as short segments
function span(a, b, wire, name) {
  // hanging cable is thinner than placed wire: about 60% of it, never under half a pixel
  const ws = { tex: wire.tex, w: Math.max(0.5, wire.w * 0.6), h: Math.max(0.5, wire.h * 0.6) };
  const out = [], N = 14, L = Math.hypot(b[0] - a[0], b[1] - a[1]), sag = Math.max(8, L / 24);
  const pt = (t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t - 4 * sag * t * (1 - t), a[2]];
  for (let i = 0; i < N; i++) {
    const p = pt(i / N), q = pt((i + 1) / N), mx = (p[0] + q[0]) / 2, my = (p[1] + q[1]) / 2, len = Math.hypot(q[0] - p[0], q[1] - p[1]) + 0.3;
    const ang = (Math.atan2(q[1] - p[1], q[0] - p[0]) * 180) / Math.PI;
    out.push({ name: `${name}_${i}`, from: [mx - len / 2 + 8, my - ws.h / 2, a[2] - ws.w / 2 + 8], to: [mx + len / 2 + 8, my + ws.h / 2, a[2] + ws.w / 2 + 8],
      faces: { all: ws.tex }, rot: ['z', ang, [mx + 8, my, a[2] + 8]] });
  }
  return out.map((c) => cube(c, [0, 0, 0]));
}
const WROW = [0, 0, 176];
const ws1 = wireScene('copper', null, WROW, 'copper_wire');
outliner.push(group('copper_wires', [8, 0, 8], WROW, ws1.kids));
// Brass, and dyed covered cables, in a shorter scene each
const BROW = [0, 0, 288];
const ws2 = wireScene('brass', null, BROW, 'brass_wire', false);
outliner.push(group('brass_wires', [8, 0, 8], BROW, ws2.kids));
const DWROW = [160, 0, 288];
const ws3 = wireScene('copper', 'red', DWROW, 'red_cable', false);
outliner.push(group('red_cable', [8, 0, 8], DWROW, ws3.kids));
const DWROW2 = [0, 0, 368];
const ws4 = wireScene('brass', 'blue', DWROW2, 'blue_cable', false);
outliner.push(group('blue_cable', [8, 0, 8], DWROW2, ws4.kids));
// dye swatches on short covered cable lengths, all 16, on a floor
{
  const row = [160, 0, 368], kids = [];
  Object.keys(DYES).forEach((d, i) => {
    const ws = wireSpec(i % 2 ? 'brass' : 'copper', d);
    kids.push(blockOf('cable_' + d, [C('floor', [0, -2, 0], [16, 0, 16], { all: 'minecraft:block/smooth_stone' }), floorRun('z', 2, 14, ws), floorClip('z', 8, ws, i % 2 ? 'brass' : 'copper')], i % 8, 0, Math.floor(i / 8), row));
  });
  outliner.push(group('dye_swatches', [8, 0, 8], row, kids));
}

// Pole variants: 1 to 4 branches on poles of different heights (a branch can go on any side of the head)
{
  const row = [0, 0, 448], kids = [];
  const POLES = [[['east'], 2], [['north', 'south'], 3], [['north', 'south', 'east'], 2], [['north', 'south', 'east', 'west'], 4], [[], 1]];
  POLES.forEach(([branches, height], i) => {
    const bx = i * 3;
    for (let x = bx - 1; x <= bx + 1; x++) kids.push(blockOf(`pv_floor_` + x, [C('floor', [0, -2, 0], [16, 0, 16], { all: 'minecraft:block/smooth_stone' })], x, 0, 0, row));
    for (let y = 0; y < height; y++) {
      const role = height === 1 ? 'single' : y === 0 ? 'foot' : y === height - 1 ? 'head' : 'pole';
      kids.push(blockOf(`pv_` + i + '_' + y, postBlock(role, i % 2 ? 'brass' : 'copper', role === 'head' || role === 'single' ? branches : []), bx, y, 0, row));
    }
  });
  outliner.push(group('pole_variants', [8, 0, 8], row, kids));
}

// ---------- inventory icons ----------
// 3D item models, one per item, each in its own cell; the GUI view looks down 30 degrees from the south-east corner.
// A coil of cable for the Power Line items: three loose loops lying flat, a clip-coloured tie and a loose end
function coil(tier) {
  const tex = tier === 'brass' ? 'brass_twin' : 'copper', t = 1, out = [];
  // loose loops: a gap between them, each a slightly different size
  const loops = [[4.2, 3.9], [5.9, 4.9], [7.6, 4.2]];
  // each loop an octagon of bars, as the Valve wheel's rim: bars on the axes lie along their tangent, diagonal ones turn 45
  loops.forEach(([y, R], li) => {
    const ap = R * Math.cos(Math.PI / 8), side = 2 * R * Math.sin(Math.PI / 8) + 0.15;
    for (let k = 0; k < 8; k++) {
      const an = (k * Math.PI) / 4, cx = 8 + ap * Math.cos(an), cz = 8 + ap * Math.sin(an), alongZ = k === 0 || k === 4;
      const from = alongZ ? [cx - t / 2, y, cz - side / 2] : [cx - side / 2, y, cz - t / 2], to = alongZ ? [cx + t / 2, y + t, cz + side / 2] : [cx + side / 2, y + t, cz + t / 2];
      out.push(C(`coil_` + li + '_' + k, from, to, { all: tex }, k % 2 ? { rot: ['y', k === 1 || k === 5 ? 45 : -45, [cx, y + t / 2, cz]] } : {}));
    }
  });
  out.push(C('coil_tie', [11.4, 3.8, 7.25], [13.4, 9.0, 8.75], { all: CLIP[tier] }));
  out.push(C('coil_end', [12.2, 7.6, 9.4], [15.6, 7.6 + t, 9.4 + t], { all: tex }, { rot: ['y', -22.5, [12.2, 8.1, 9.9]] }));
  return out;
}
const ICONS = [
  ['copper_fluid_line', straight('x', 'copper', {})],
  ['brass_fluid_line', straight('x', 'brass', {})],
  ['copper_power_line', coil('copper')],
  ['brass_power_line', coil('brass')],
  ['line_post', postBlock('single', 'copper', ['east', 'west'])],
  ['intake', intake('copper', 'water', null)],
  ['valve', (() => { const v = valve('copper', null); return [...v.parts, ...v.wheel]; })()],
  ['breaker', (() => { const ws = { w: 1.5, h: 1.5, tex: 'copper' }; const b = breaker(ws); return [floorRun('x', 1, 15, ws), ...b.parts, ...b.lever]; })()],
  ['filter', filter('copper', 'water', null)],
  ['meter', (() => { const m = meterPipe('copper', null); return [...m.parts, ...m.needle]; })()],
  ['splitter', (() => { const p = splitterPipe('copper', null, 'even'); return [...p.parts, ...p.dial]; })()],
];
{
  const row = [0, 0, 560];
  outliner.push(group('icons', [8, 0, 8], row, ICONS.map(([name, parts], i) => blockOf('icon_' + name, parts, i * 2, 0, 0, row))));
}

// ---------- clip: the moving parts ----------
const spring = (t, t0, v0, v1, f = 2.2, d = 0.45) => {
  if (t < t0) return v0;
  const u = t - t0, w = 2 * Math.PI * f;
  return v1 + (v0 - v1) * Math.exp(-d * w * u) * Math.cos(w * Math.sqrt(1 - d * d) * u);
};
const tracks = {};
// the Valve wheel: shut over 0.4 s at 1 s (270 degrees), open again in 10% clicks from 2.5 s
for (const w of MOVERS.wheels) tracks[w] = { rotation: (t) => { let a = 0; if (t > 1 && t < 1.4) a = -270 * ((t - 1) / 0.4); else if (t >= 1.4 && t < 2.5) a = -270; else if (t >= 2.5) a = -270 + 27 * Math.min(10, Math.floor((t - 2.5) / 0.15)); return [0, a, 0]; } };
// Meter needles: settle on 60% of the sweep, drop to 0 while the valve is shut, back up after
const level = (t) => (t < 1 ? 0.6 : t < 2.5 ? 0 : Math.min(1, (t - 2.5) / 1.5) * 0.6);
for (const n of MOVERS.needles) tracks[n] = { rotation: (t) => [0, 0, 90 - 180 * spring(t, 0, 0, level(t), 1.8, 0.35)] };
for (const n of MOVERS.wneedles) tracks[n] = { rotation: (t) => [0, -(-90 + 180 * spring(t, 0, 0, 0.45, 1.8, 0.35)), 0] };
// the Breaker: on, snaps off at 1.5 s and back on at 3 s, with a small overshoot
for (const l of MOVERS.levers) tracks[l] = { rotation: (t) => [0, 0, t < 1.5 ? spring(t, 0, 35, 35) : t < 3 ? spring(t, 1.5, 35, -35, 6, 0.4) : spring(t, 3, -35, 35, 6, 0.4)] };
// Splitter dials: click round 120 degrees at 1, 2 and 3 s
for (const d of [...MOVERS.dials, ...MOVERS.wdials]) tracks[d] = { rotation: (t) => { const k = Math.floor(t); const base = -120 * Math.min(3, k), u = t - k; return [0, base - (u < 0.12 && k >= 1 && k <= 3 ? 0 : 0), 0]; } };
clip('run', 4.5, 'loop', tracks);

write(OUT, outliner);
// the flow strips are animated textures: 4 frames down the image
const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
for (const t of bb.textures) if (ANIMATED[t.name]) { t.height = 64; t.uv_height = 16; t.frame_time = 3; }
fs.writeFileSync(OUT, JSON.stringify(bb));
console.error('dyes:', JSON.stringify(DYES));
