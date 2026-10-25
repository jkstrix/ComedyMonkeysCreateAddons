// Ship hardware redesign generator (the Aeronautics set) -> design/models/ship-hardware.bbmodel (Generic Model preview).
// To design/briefs/compact-gyroscope.md (and torque-rotor.md, flight-instruments.md): aviator brass and steel, dark
// riveted steel plates, brass rings and bezels, round portholes. Built in the game's model space.
//
// Compact Gyroscope: a riveted cage (four corner posts, top and bottom plates with brass shaft collars, a porthole on
// each side with a brass bar and bearing boss) round a brass gimbal: the outer ring lies flat and pivots on pins at the
// north and south bosses (about Z), the inner ring stands upright on pins in the outer ring at east and west (about X),
// and carries the flywheel on a vertical axle. Brake calipers on the inner ring close on the flywheel's rim.
const path = require('path');
const { P, C, hash, mot, edge, HI, LO, textureSet, painters, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'ship-hardware.bbmodel');
const CEN = 8;

// ---------- textures (compact_gyroscope_<key>) ----------
const GY = textureSet(); GY.file = 'compact_gyroscope_';
const STEEL = ['#2f343b', '#3c424a', '#4a5059'], RIVET = '#6b7178';
// the set's brass: the warm mid brass of the brass cobble generators, no bright highlights (the house brass read too bright here)
P.shipBrass = ['#7e6832', '#8f7638', '#a3873f'];
P.shipBrassDark = ['#5e4c24', '#6e5a2a', '#7e6832'];
// the set's wood accents: the lighter walnut the chests landed on (the house walnut reads too dark in game)
P.shipWood = ['#4e3626', '#634532', '#6f4d36'];
const WOOD_SEAM = '#3b2a1f';
// grain along x (a 'run' of planks every 4 px across y), or along y when vertical
const grain = (x, y, s, vertical = false) => { const [a, b] = vertical ? [y, x] : [x, y]; return b % 4 === 3 ? WOOD_SEAM : mot('shipWood', (a >> 1) + (b >> 2) * 7, b, s); };
// a riveted steel plate: bevelled edge, a row of rivets just inside it every third pixel
const riveted = (x, y, s) => {
  const e = edge(x, y);
  if (e === 0) return x === 0 || y === 0 ? HI : LO;
  if (e === 1 && (x + y) % 3 === 0) return RIVET;
  return hash(x, y, s) > 0.88 ? STEEL[2] : STEEL[1];
};
GY.paint('plate', (x, y) => riveted(x, y, 80));
// the side plate round its porthole: a brass bezel round the round-cornered opening (x/y 3-12, two-step corners)
const inHole = (x, y) => x >= 3 && x <= 12 && y >= 3 && y <= 12 && Math.min(x - 3, 12 - x) + Math.min(y - 3, 12 - y) >= 2;
GY.paint('porthole', (x, y) => {
  if (inHole(x, y)) return '#1f2327';
  const near = [[1, 0], [-1, 0], [0, 1], [0, -1], [1, 1], [-1, -1], [1, -1], [-1, 1]].some(([dx, dy]) => inHole(x + dx, y + dy));
  if (near) return (x + y) % 4 === 0 ? '#a3873f' : mot('shipBrass', x, y, 81);
  return riveted(x, y, 82);
});
// the posts: a steel column with rivets down the middle
// the posts: walnut, its grain running up them, in riveted steel bands at the top and bottom (v 2-3 and 12-13)
GY.paint('post', (x, y) => {
  if (y <= 3 || y >= 12) return (y === 2 || y === 13) && x % 4 === 1 ? RIVET : y === 3 || y === 12 ? LO : STEEL[1];
  return x % 8 === 7 ? WOOD_SEAM : mot('shipWood', x >> 1, y >> 1, 83);
});
// top and bottom: riveted plate with a brass collar round the shaft opening (x/z 6-10 open)
GY.paint('top', (x, y) => {
  const r = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
  if (r < 2) return '#1f2327';
  if (r < 3.5) return r < 2.6 ? '#8f7638' : mot('shipBrass', x, y, 84);
  return riveted(x, y, 85);
});
GY.paint('brass', (x, y) => mot('shipBrass', x, y, 89));
GY.paint('brass_dark', (x, y) => mot('shipBrassDark', x, y, 86));
GY.paint('steel', painters.steel());
GY.paint('steel_light', painters.steelLight());
GY.paint('dark', painters.dark());
GY.paint('glass', painters.glass());
// the flywheel's face: a dark steel disc with brass spokes and hub, cut to a circle
GY.paint('flywheel', (x, y) => {
  const cx = x - 7.5, cy = y - 7.5, r = Math.hypot(cx, cy), a = Math.atan2(cy, cx);
  if (r > 7.6) return null;
  if (r > 6.3) return mot('shipBrass', x, y, 87);
  if (r < 1.8) return '#a3873f';
  if (Math.abs(Math.sin(a * 3)) < 0.22) return '#8f7638';
  return hash(x, y, 88) > 0.85 ? STEEL[2] : STEEL[1];
});

// ---------- round things ----------
// a ring of 16 staves round an axis through (CEN, CEN, CEN): 'y' lies flat (staves turned about Y), 'z' stands
// upright in the XY plane (staves turned about Z). a0..a1 is its width along the axis, r its outer radius.
function ring(name, axis, r, a0, a1, thick, tex) {
  const out = [], w = 2 * r * Math.tan(Math.PI / 16) + 0.1;
  for (let s = 0; s < 16; s++) {
    const phi = s * 22.5;
    const base = [0, 90, 180, 270].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    const delta = ((phi - base + 540) % 360) - 180;
    // in the ring's own plane: u along the base direction's radius, v across it
    let f, t;
    const lo = CEN - w / 2, hi = CEN + w / 2;
    if (axis === 'y') {
      if (base === 0) { f = [lo, a0, CEN - r]; t = [hi, a1, CEN - r + thick]; }
      else if (base === 180) { f = [lo, a0, CEN + r - thick]; t = [hi, a1, CEN + r]; }
      else if (base === 90) { f = [CEN + r - thick, a0, lo]; t = [CEN + r, a1, hi]; }
      else { f = [CEN - r, a0, lo]; t = [CEN - r + thick, a1, hi]; }
    } else {
      if (base === 0) { f = [lo, CEN + r - thick, a0]; t = [hi, CEN + r, a1]; }
      else if (base === 180) { f = [lo, CEN - r, a0]; t = [hi, CEN - r + thick, a1]; }
      else if (base === 90) { f = [CEN + r - thick, lo, a0]; t = [CEN + r, hi, a1]; }
      else { f = [CEN - r, lo, a0]; t = [CEN - r + thick, hi, a1]; }
    }
    out.push(C(`${name}_${s}`, f, t, { all: tex }, delta ? { rot: [axis, delta, [CEN, CEN, CEN]] } : {}));
  }
  return out;
}

// ---------- the Compact Gyroscope ----------
const R_OUT = 6, R_IN = 4.75, R_WHEEL = 3, BAND = 0.75;
const g = (k) => 'gy:' + k;
// one side's porthole plate, built on the north face (z 0-1) and turned to the others
function sidePlate(face) {
  // a 1 px band round a 10 px opening, its corners stepped round
  const pieces = [
    [[2, 13], [14, 14]], [[2, 2], [14, 3]], [[2, 3], [3, 13]], [[13, 3], [14, 13]],
    [[3, 3], [5, 4]], [[3, 4], [4, 5]], [[11, 3], [13, 4]], [[12, 4], [13, 5]],
    [[3, 12], [5, 13]], [[3, 11], [4, 12]], [[11, 12], [13, 13]], [[12, 11], [13, 12]],
  ];
  const out = pieces.map(([[x0, y0], [x1, y1]], i) => ({ n: `plate_${i}`, f: [x0, y0, 0], t: [x1, y1, 1], tex: g('porthole') }));
  out.push({ n: 'glass', f: [3, 3, 0.5], t: [13, 13, 0.5], tex: g('glass'), glass: true });
  // a thin steel brace down the porthole, behind the glass, with the bearing boss on it
  out.push({ n: 'bar', f: [7.75, 3, 1], t: [8.25, 13, 1.2], tex: g('steel') });
  out.push({ n: 'boss', f: [7.25, 7.25, 0.75], t: [8.75, 8.75, 1.25], tex: g('steel_light') });
  // a pin from the boss to the outer ring (north and south only: the ring pivots about Z)
  if (face === 'north' || face === 'south') out.push({ n: 'pin', f: [7.75, 7.75, 1.25], t: [8.25, 8.25, CEN - R_OUT + 0.25], tex: g('steel_light') });
  // turn the north-face pieces to this face
  const turn = (p) => ({ north: [p[0], p[1], p[2]], south: [16 - p[0], p[1], 16 - p[2]], west: [p[2], p[1], 16 - p[0]], east: [16 - p[2], p[1], p[0]] })[face];
  return out.map((o) => {
    const a = turn(o.f), b = turn(o.t);
    const f = [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])], t = [Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])];
    if (o.glass) {
      const flat = face === 'north' || face === 'south' ? { north: o.tex, south: o.tex, all: null } : { east: o.tex, west: o.tex, all: null };
      return C(`${face}_${o.n}`, f, t, flat, { shade: false });
    }
    return C(`${face}_${o.n}`, f, t, { all: o.tex });
  });
}
function gyroCage() {
  const out = [];
  // top and bottom plates round the 4 x 4 shaft opening (x/z 6-10)
  for (const [n, y0, y1] of [['bottom', 0, 2], ['top', 14, 16]]) {
    const face = { all: g('plate'), up: g('top'), down: g('top') };
    out.push(C(`${n}_a`, [0, y0, 0], [16, y1, 6], face), C(`${n}_b`, [0, y0, 10], [16, y1, 16], face),
      C(`${n}_c`, [0, y0, 6], [6, y1, 10], face), C(`${n}_d`, [10, y0, 6], [16, y1, 10], face));
  }
  for (const [x, z] of [[0, 0], [14, 0], [0, 14], [14, 14]]) out.push(C(`post_${x}_${z}`, [x, 2, z], [x + 2, 14, z + 2], { all: g('post'), up: null, down: null }));
  for (const face of ['north', 'south', 'east', 'west']) out.push(...sidePlate(face));
  return out;
}
// the outer ring (flat, pivoting about Z) with the inner ring's pins in it
const outerRing = () => [...ring('outer', 'y', R_OUT, CEN - 0.5, CEN + 0.5, BAND, g('brass')),
  C('pin_w', [CEN - R_OUT + BAND, CEN - 0.25, CEN - 0.25], [CEN - R_IN, CEN + 0.25, CEN + 0.25], { all: g('steel_light') }),
  C('pin_e', [CEN + R_IN, CEN - 0.25, CEN - 0.25], [CEN + R_OUT - BAND, CEN + 0.25, CEN + 0.25], { all: g('steel_light') })];
// the inner ring (upright in the XY plane, pivoting about X) with the caliper bodies on it
const innerRing = () => [...ring('inner', 'z', R_IN, CEN - 0.5, CEN + 0.5, BAND, g('brass_dark')),
  // the axle's bearings top and bottom
  C('bearing_top', [CEN - 0.75, CEN + R_IN - BAND - 0.5, CEN - 0.75], [CEN + 0.75, CEN + R_IN - BAND, CEN + 0.75], { all: g('steel') }),
  C('bearing_bottom', [CEN - 0.75, CEN - R_IN + BAND, CEN - 0.75], [CEN + 0.75, CEN - R_IN + BAND + 0.5, CEN + 0.75], { all: g('steel') }),
  // the calipers' bodies: steel blocks on the ring's sides, reaching in to the flywheel's rim
  ...[-1, 1].map((s) => C(`caliper_body_${s}`, s < 0 ? [CEN - R_IN + BAND, CEN - 1.25, CEN - 0.5] : [CEN + R_WHEEL + 0.25, CEN - 1.25, CEN - 0.5],
    s < 0 ? [CEN - R_WHEEL - 0.25, CEN + 1.25, CEN + 0.5] : [CEN + R_IN - BAND, CEN + 1.25, CEN + 0.5], { all: g('steel') }))];
// one caliper jaw (over the rim; the renderer slides it toward the wheel's face), side -1 west / +1 east, above or below
const jaw = (s, up) => {
  const x0 = s < 0 ? CEN - R_WHEEL - 0.25 : CEN + R_WHEEL - 1.25, x1 = s < 0 ? CEN - R_WHEEL + 1.25 : CEN + R_WHEEL + 0.25;
  return [C(`jaw_${s}_${up ? 'up' : 'down'}`, [x0, up ? CEN + 0.75 : CEN - 1.25, CEN - 0.5], [x1, up ? CEN + 1.25 : CEN - 0.75, CEN + 0.5], { all: g('dark') })];
};
// the flywheel: an octagonal disc with its face painted round, a brass rim, the axle
const flywheel = () => {
  // small enough that the turned square's corners stay inside the rim, thin enough to clear its faces
  const d = (name, rot) => C(name, [CEN - 2.1, CEN - 0.3, CEN - 2.1], [CEN + 2.1, CEN + 0.3, CEN + 2.1], { all: g('steel'), up: g('flywheel'), down: g('flywheel') }, { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] }, ...(rot ? { rot: ['y', 45, [CEN, CEN, CEN]] } : {}) });
  return [d('disc', false), d('disc_45', true), ...ring('rim', 'y', R_WHEEL, CEN - 0.5, CEN + 0.5, 0.5, g('brass')),
    C('axle', [CEN - 0.4, CEN - R_IN + BAND + 0.5, CEN - 0.4], [CEN + 0.4, CEN + R_IN - BAND - 0.5, CEN + 0.4], { all: g('steel_light'), up: null, down: null })];
};
// the shaft's ends in the collars (preview only: the renderer draws Create's shaft there)
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const stub = (name, y0, y1) => [C(name, [6, y0, 6], [10, y1, 10], { all: AXIS, up: AXIS_TOP, down: AXIS_TOP }, { uv: { north: [6, 0, 10, y1 - y0], south: [6, 0, 10, y1 - y0], east: [6, 0, 10, y1 - y0], west: [6, 0, 10, y1 - y0], up: [6, 6, 10, 10], down: [6, 6, 10, 10] } })];

// ---------- the Torque Rotor (torque_rotor_<key>), built facing up, base on the bottom ----------
const TR = textureSet(); TR.file = 'torque_rotor_';
TR.paint('plate', (x, y) => riveted(x, y, 90));
// the flange's face: riveted steel with brass bolt heads round the rim and the dark socket in the middle
// eight brass bolt heads at exact points on a 6 px circle
const BOLTS = new Set([0, 1, 2, 3, 4, 5, 6, 7].map((i) => { const a = (i * Math.PI) / 4 + Math.PI / 8; return Math.round(7.5 + 5.8 * Math.cos(a)) + ',' + Math.round(7.5 + 5.8 * Math.sin(a)); }));
TR.paint('flange', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r < 2.2) return '#1f2327';
  if (BOLTS.has(x + ',' + y)) return '#a3873f';
  return hash(x, y, 91) > 0.88 ? STEEL[2] : STEEL[1];
});
// the nacelle: steel with two rivet bands
TR.paint('nacelle', (x, y) => ((y === 4 || y === 11) ? (x % 2 === 0 ? RIVET : STEEL[0]) : hash(x, y, 92) > 0.88 ? STEEL[2] : STEEL[1]));
// a calmer brass for the hub and spinner: two shades in bands, the lighter only on the top of each band
TR.paint('brass', (x, y) => (y % 4 === 0 ? '#8f7638' : '#7e6832'));
TR.paint('steel', painters.steel());
TR.paint('dark', painters.dark());
// the blade: dark steel with a rib down its length
// the blades: laminated walnut, thin layers along their length (a darker glue line between each)
TR.paint('blade', (x, y) => (y % 3 === 2 ? WOOD_SEAM : mot('shipWood', x >> 2, y, 93)));
const t = (k) => 'tr:' + k;
// stepped-round: a square and a square turned 45 about Y, both centred on the axis
const octY = (name, r, y0, y1, faces, o = {}) => [
  C(name, [CEN - r, y0, CEN - r], [CEN + r, y1, CEN + r], faces, o),
  C(name + '_45', [CEN - r + 0.02, y0 + 0.02, CEN - r + 0.02], [CEN + r - 0.02, y1 - 0.02, CEN + r - 0.02], faces, { ...o, rot: ['y', 45, [CEN, y0, CEN]] }),
];
// the static model: the bolted flange (open in the middle for the shaft) and the nacelle
function rotorBase() {
  return [
    // the flange in four pieces round the 4 x 4 socket (x/z 6-10), so the shaft's end shows from underneath
    C('flange_n', [1, 0, 1], [15, 2, 6], { all: t('plate'), up: t('flange'), down: t('flange') }, { uv: { up: [1, 1, 15, 6], down: [1, 10, 15, 15] } }),
    C('flange_s', [1, 0, 10], [15, 2, 15], { all: t('plate'), up: t('flange'), down: t('flange') }, { uv: { up: [1, 10, 15, 15], down: [1, 1, 15, 6] } }),
    C('flange_w', [1, 0, 6], [6, 2, 10], { all: t('plate'), up: t('flange'), down: t('flange'), north: null, south: null }, { uv: { up: [1, 6, 6, 10], down: [1, 6, 6, 10] } }),
    C('flange_e', [10, 0, 6], [15, 2, 10], { all: t('plate'), up: t('flange'), down: t('flange'), north: null, south: null }, { uv: { up: [10, 6, 15, 10], down: [10, 6, 15, 10] } }),
    // the nacelle: a stepped-round steel drum on the flange, hollow for the shaft at its foot
    // short, so the blades sit close to the surface (it read too long at 6 px)
    ...octY('nacelle', 3.75, 2, 5.5, { all: t('nacelle'), down: null }),
    ...octY('nacelle_lip', 4.25, 5, 5.75, { all: t('steel'), up: t('steel') }),
  ];
}
// the spinning part: the brass hub and spinner (the blades are drawn three times round it)
function rotorHub() {
  return [
    ...octY('hub', 2.75, 5.75, 7.5, { all: t('brass') }),
    ...octY('spinner', 1.75, 7.5, 8.5, { all: t('brass') }),
    C('spinner_tip', [CEN - 0.75, 8.5, CEN - 0.75], [CEN + 0.75, 9.25, CEN + 0.75], { all: t('brass') }),
  ];
}
// one blade, reaching out along +X to 12 px from the axis, its paddle pitched 22.5 degrees about its own length
function rotorBlade() {
  const pitch = { rot: ['x', 22.5, [CEN, 6.5, CEN]] };
  return [
    C('blade_root', [CEN + 2, 6, CEN - 1.25], [CEN + 4, 7.25, CEN + 1.25], { all: t('brass') }),
    // a broad paddle, widening to the tip, with a brass leading edge
    C('blade_inner', [CEN + 4, 6.125, CEN - 1.75], [CEN + 8, 6.875, CEN + 1.5], { all: t('blade') }, pitch),
    C('blade_outer', [CEN + 8, 6.125, CEN - 2.5], [CEN + 12, 6.875, CEN + 1.5], { all: t('blade') }, pitch),
    C('blade_edge', [CEN + 4, 6.175, CEN + 1.5], [CEN + 12, 6.825, CEN + 2.25], { all: t('brass') }, pitch),
  ];
}

// ---------- the Flight Instruments (flight_instruments_<key>), panel facing north ----------
const FI = textureSet(); FI.file = 'flight_instruments_';
FI.paint('body', (x, y) => riveted(x, y, 95));
// the desk's sides: walnut cheeks in a riveted steel border
FI.paint('side', (x, y) => (edge(x, y) <= 1 ? riveted(x, y, 96) : grain(x, y, 99)));
// the front lip's top: walnut decking
FI.paint('deck', (x, y) => grain(x, y, 100));
FI.paint('wood', (x, y) => grain(x, y, 101));
FI.paint('brass', (x, y) => mot('shipBrass', x, y, 97));
FI.paint('dark', painters.dark());
// The 128 px panel: dark riveted steel with the four dials exactly where the renderer draws (centres at panel
// (4, 4), (12, 4), (4, 12), (12, 12) sixteenths = 32 / 96 px, radius 2.75 = 22 px): altimeter, airspeed, attitude (a
// plain disc: the ball is drawn over it), heading. Each in a brass bezel ring.
const FONT = { // 3 x 5 letters and digits
  // N and W five wide, so they can't be mistaken for each other
  N: ['10001', '11001', '10101', '10011', '10001'], E: ['111', '100', '110', '100', '111'], S: ['111', '100', '111', '001', '111'], W: ['10001', '10001', '10101', '10101', '01010'],
  0: ['111', '101', '101', '101', '111'], 1: ['010', '110', '010', '010', '111'], 2: ['111', '001', '111', '100', '111'], 3: ['111', '001', '111', '001', '111'],
  4: ['101', '101', '111', '001', '001'], 5: ['111', '100', '111', '001', '111'], 6: ['111', '100', '111', '101', '111'], 7: ['111', '001', '001', '001', '001'],
  8: ['111', '101', '111', '101', '111'], 9: ['111', '101', '111', '001', '111'],
};
function panel128() {
  const W = 128, d = new Uint8Array(W * W * 4);
  const set = (x, y, c) => { if (x < 0 || y < 0 || x >= W || y >= W) return; const i = (y * W + x) * 4, v = parseInt(c.slice(1), 16); d[i] = v >> 16; d[i + 1] = (v >> 8) & 255; d[i + 2] = v & 255; d[i + 3] = 255; };
  // the steel field, a bevel round the edge and rivets
  for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    const e = Math.min(x, y, W - 1 - x, W - 1 - y);
    set(x, y, e < 2 ? (x < 2 || y < 2 ? HI : LO) : hash(x >> 1, y >> 1, 98) > 0.9 ? STEEL[2] : STEEL[1]);
    if (e === 5 && (x + y) % 12 === 0) set(x, y, RIVET);
  }
  // a walnut inlay between the dials: a cross 8 px wide through the middle (x and y 60-67), its grain along each arm,
  // a dark line where it meets the steel; it stops short of the bevel and stays clear of the dials' brass rings
  for (let y = 4; y < W - 4; y++) for (let x = 4; x < W - 4; x++) {
    const inV = x >= 60 && x <= 67, inH = y >= 60 && y <= 67;
    if (!inV && !inH) continue;
    const edgeV = inV && !inH && (x === 60 || x === 67), edgeH = inH && !inV && (y === 60 || y === 67);
    const ends = (inV && !inH && (y === 4 || y === W - 5)) || (inH && !inV && (x === 4 || x === W - 5));
    if (edgeV || edgeH || ends) { set(x, y, WOOD_SEAM); continue; }
    // the vertical arm's grain runs up it, the horizontal's across; where they cross, the horizontal runs through
    set(x, y, inH ? grain(x >> 1, (y - 60) >> 0, 102) : grain((x - 60) >> 0, y >> 1, 103, true));
  }
  const DIALS = [[32, 32], [96, 32], [32, 96], [96, 96]], R = 22;
  const glyph = (ch, cx, cy, c) => { const g = FONT[ch], w = g[0].length; for (let j = 0; j < 5; j++) for (let i = 0; i < w; i++) if (g[j][i] === '1') set(cx - (w >> 1) + i, cy - 2 + j, c); };
  DIALS.forEach(([cx, cy], n) => {
    for (let y = cy - R - 4; y <= cy + R + 4; y++) for (let x = cx - R - 4; x <= cx + R + 4; x++) {
      const r = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
      if (r <= R) set(x, y, '#1f2327');
      else if (r <= R + 3) set(x, y, r > R + 2 ? '#5e4c24' : (x + y) % 5 === 0 ? '#a3873f' : '#8f7638');
    }
    const tick = (deg, len, c, w = 1) => { const a = (deg * Math.PI) / 180; for (let k = 0; k < len; k++) for (let s = 0; s < w; s++) set(Math.round(cx + Math.sin(a) * (R - 2 - k) + s * Math.cos(a)), Math.round(cy - Math.cos(a) * (R - 2 - k) + s * Math.sin(a)), c); };
    if (n === 0) { // altimeter: ten numbered marks, 0 at the top
      for (let i = 0; i < 50; i++) tick(i * 7.2, i % 5 === 0 ? 4 : 2, '#8a8c86');
      for (let i = 0; i < 10; i++) { const a = (i * 36 * Math.PI) / 180; glyph(String(i), Math.round(cx + Math.sin(a) * (R - 9)), Math.round(cy - Math.cos(a) * (R - 9)), '#a39d8b'); }
    } else if (n === 1) { // airspeed: 0-60 m/s over -150..+150 degrees, a muted green / amber / dark red arc
      for (let v = 0; v <= 60; v += 2) tick(-150 + v * 5, v % 10 === 0 ? 4 : 2, '#8a8c86');
      for (let v = 0; v <= 60; v += 0.5) { const c = v < 35 ? '#4e7a44' : v < 50 ? '#a3873f' : '#8a2f22'; tick(-150 + v * 5, 1, c, 2); }
      for (const [v, s] of [[0, '0'], [20, '2'], [40, '4'], [60, '6']]) { const a = ((-150 + v * 5) * Math.PI) / 180; glyph(s, Math.round(cx + Math.sin(a) * (R - 10)), Math.round(cy - Math.cos(a) * (R - 10)), '#a39d8b'); }
    } else if (n === 3) { // heading: N at the top, E, S, W clockwise
      for (let i = 0; i < 36; i++) tick(i * 10, i % 9 === 0 ? 4 : 2, '#8a8c86');
      for (const [deg, ch] of [[0, 'N'], [90, 'E'], [180, 'S'], [270, 'W']]) { const a = (deg * Math.PI) / 180; glyph(ch, Math.round(cx + Math.sin(a) * (R - 9)), Math.round(cy - Math.cos(a) * (R - 9)), deg === 0 ? '#a3873f' : '#a39d8b'); }
    }
  });
  return require('./process-box.lib.js').png(W, W, d);
}
FI.OWN.panel = panel128();
const fi = (k) => 'fi:' + k;
// the panel plane (MUST stay where the renderer draws): x 0-16, from y 2 at z 8 leant back 22.5 degrees about X
const LEAN = ['x', 22.5, [8, 2, 8]], PANEL_LEN = 14 / Math.cos((22.5 * Math.PI) / 180);
// a point on the panel in the renderer's units (u right as you look at it, v down from the top, sixteenths), in the
// panel's unturned frame
const onPanel = (u, v) => [16 - u, 2 + PANEL_LEN * (1 - v / 16)];
function instruments() {
  const out = [];
  // the desk: the lip and the steps up the back, as today (they make the outline the code expects)
  out.push(C('lip', [0, 0, 5], [16, 2, 16], { north: fi('body'), south: fi('body'), east: fi('side'), west: fi('side'), up: fi('deck'), down: fi('body') }));
  for (let i = 0; i < 14; i++) {
    // each step just behind the panel's back face (the old model's depths)
    out.push(C(`step_${i}`, [0, 2 + i, Math.round((9.014 + i * 0.41421) * 1000) / 1000], [16, 3 + i, 16], { east: fi('side'), west: fi('side'), south: fi('body'), all: null, ...(i === 13 ? { up: fi('body') } : {}) }));
  }
  out.push(C('panel', [0, 2, 8], [16, 2 + PANEL_LEN, 9], { north: fi('panel'), east: fi('body'), west: fi('body'), up: fi('body'), all: null }, { rot: LEAN, uv: { north: [0, 0, 16, 16] } }));
  // the walnut cross between the dials, raised off the panel: the upright strip a quarter pixel proud, the crossbar
  // half a pixel, so where they cross their faces never sit level
  {
    const [x0, yTop] = onPanel(8.5, 0.5), [x1, yBot] = onPanel(7.5, 15.5);
    out.push(C('cross_upright', [x0, yBot, 7.75], [x1, yTop, 8], { all: fi('wood'), south: null }, { rot: LEAN }));
    const [h0, hTop] = onPanel(15.5, 7.5), [h1, hBot] = onPanel(0.5, 8.5);
    out.push(C('cross_bar', [h0, hBot, 7.5], [h1, hTop, 8], { all: fi('wood'), south: null }, { rot: LEAN }));
  }
  // the glare hood over the panel's top edge, overhanging it toward the pilot
  out.push(C('hood', [0, 2 + PANEL_LEN, 6.25], [16, 2 + PANEL_LEN + 0.75, 9], { all: fi('body'), down: fi('dark') }, { rot: LEAN }));
  out.push(C('hood_lip', [0, 2 + PANEL_LEN - 0.5, 6.25], [16, 2 + PANEL_LEN, 6.75], { all: fi('brass') }, { rot: LEAN }));
  // the brass hand rail along the front lip, where the buttons were, and brass caps on the desk's back corners
  out.push(C('rail', [1, 3, 5.4], [15, 3.6, 6.1], { all: fi('wood') }));
  for (const x of [1, 14]) out.push(C(`rail_post_${x}`, [x, 2, 5.5], [x + 1, 3, 6], { all: fi('brass'), up: null, down: null }));
  for (const x of [0, 15]) out.push(C(`cap_${x}`, [x - (x ? 0 : 0.25), 0, 15], [x + 1 + (x ? 0.25 : 0), 2.25, 16.25], { all: fi('brass'), down: null }));
  return out;
}

// ---------- preview ----------
const { cube, group, clip, write } = preview('ship-hardware', { 'gy:': GY, 'tr:': TR, 'fi:': FI });
const cubes = (list, off) => list.map((c) => cube(c, off));
const outliner = [];
// three gyroscopes: still and level, on a ship that pitches and rolls, and braking
for (const [name, off] of [['level', [0, 0, 0]], ['ship', [24, 0, 0]], ['brake', [48, 0, 0]]]) {
  outliner.push(group(`${name}_body`, [CEN, CEN, CEN], off, [
    group(`${name}_cage`, [CEN, CEN, CEN], off, cubes([...gyroCage(), ...stub('stub_bottom', 0, 2.5), ...stub('stub_top', 13.5, 16)], off)),
    group(`${name}_outer`, [CEN, CEN, CEN], off, [...cubes(outerRing(), off),
      group(`${name}_inner`, [CEN, CEN, CEN], off, [...cubes(innerRing(), off),
        group(`${name}_jaws_up`, [CEN, CEN, CEN], off, cubes([...jaw(-1, true), ...jaw(1, true)], off)),
        group(`${name}_jaws_down`, [CEN, CEN, CEN], off, cubes([...jaw(-1, false), ...jaw(1, false)], off)),
        group(`${name}_wheel`, [CEN, CEN, CEN], off, cubes(flywheel(), off))])]),
  ]));
}
// the instruments, facing north
outliner.push(group('instruments', [8, 0, 8], [64, 0, 32], cubes(instruments(), [64, 0, 32])));
// two rotors: one standing up, one on a wall (facing north, the blockstate's x90 turn)
for (const [name, off, rot] of [['rotor_up', [0, 0, 32], [0, 0, 0]], ['rotor_wall', [30, 0, 32], [-90, 0, 0]]]) {
  const blades = [0, 1, 2].map((i) => group(`${name}_blade_${i}`, [CEN, 0, CEN], off, cubes(rotorBlade(), off)));
  outliner.push(group(`${name}_mount`, [CEN, CEN, CEN], off, [
    group(`${name}_base`, [CEN, 0, CEN], off, cubes(rotorBase(), off)),
    group(`${name}_spin`, [CEN, 0, CEN], off, [...cubes(rotorHub(), off), ...blades]),
  ]));
}
const rotorSteps = {};
for (const name of ['rotor_up', 'rotor_wall']) {
  for (const i of [0, 1, 2]) rotorSteps[`${name}_blade_${i}`] = { rotation: [[0, [0, 120 * i, 0]]] };
}
rotorSteps.rotor_wall_mount = { rotation: [[0, [-90, 0, 0]]] };
// working: every flywheel spins (the brake's slows to a stop as its calipers close); the ship's gyroscope rocks with
// the ship while its rings counter-tilt so the wheel stays level
const LEN = 6, spin = (t, f = 1) => [0, (t * 20 * 64 * 6 * 0.25 * f) % 360, 0];
const pitch = (t) => 14 * Math.sin((2 * Math.PI * t) / LEN), roll = (t) => 10 * Math.sin((4 * Math.PI * t) / LEN + 0.7);
const brake = (t) => Math.min(1, t / 3); // 0 open .. 1 clamped
let braked = 0; // the braked wheel's angle, integrated so it slows smoothly
const brakeAngle = [];
for (let k = 0; k <= LEN * 20; k++) { brakeAngle.push(braked); braked += 64 * 6 * 0.25 * (1 - brake(k / 20)); }
clip('working', LEN, 'loop', {
  level_wheel: { rotation: (t) => spin(t) },
  ship_wheel: { rotation: (t) => spin(t) },
  ship_body: { rotation: (t) => [pitch(t), 0, roll(t)] },
  ship_outer: { rotation: (t) => [0, 0, -roll(t)] },
  ship_inner: { rotation: (t) => [-pitch(t), 0, 0] },
  brake_wheel: { rotation: (t) => [0, brakeAngle[Math.round(t * 20)] % 360, 0] },
  brake_jaws_up: { position: (t) => [0, -0.25 * brake(t), 0] },
  brake_jaws_down: { position: (t) => [0, 0.25 * brake(t), 0] },
  // the rotors at 64 RPM, a quarter speed so the blades read
  rotor_up_spin: { rotation: (t) => spin(t) },
  rotor_wall_spin: { rotation: (t) => spin(t, -1) },
}, rotorSteps);
write(OUT, outliner);

module.exports = { GY, TR, FI, instruments, onPanel, PANEL_LEN, ring, gyroCage, outerRing, innerRing, jaw, flywheel, rotorBase, rotorHub, rotorBlade, R_OUT, R_IN, R_WHEEL };
