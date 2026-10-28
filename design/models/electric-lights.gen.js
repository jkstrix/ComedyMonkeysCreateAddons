// Electric Lights preview generator -> design/models/electric-lights-<group>.bbmodel (Generic Model preview).
// To design/briefs/electric-lights-street.md (group 'street'), -indoor.md ('indoor') and -runs-and-specials.md ('runs').
// node electric-lights.gen.js <street|indoor|runs>
// Every piece is built in the game's model space of the block it belongs to (x 0 west -> 16 east, y up, z 0 north -> 16 south)
// and placed into the scene by block position. Lamp-head arms are built pointing north and turned to their side.
// Glow parts (lenses, panes, globes) are their own cubes on the 'glow' textures; in game the renderer draws them, tinted.
const path = require('path');
const { C, hash, textureSet, preview } = require('./process-box.lib.js');

// required as a library (by electric-lights.export.js): the geometry only, no scene written
const LIB = require.main !== module;
const GROUP = LIB ? 'lib' : process.argv[2] || 'street';
const OUT = path.resolve(__dirname, `electric-lights-${GROUP}.bbmodel`);

// ---------- palette ----------
const IRON = ['#26292d', '#2f3338', '#3a3f45'];
const GALV = ['#565c62', '#62686e', '#6e7479'];
const STEEL = ['#2f343b', '#3c424a', '#4a5059'];
const BRASS = ['#7e6832', '#8f7638', '#a3873f'];
const CERAMIC = ['#7d786f', '#8f897e', '#9c968b'];
const ALU = ['#6b6e6a', '#7c7f79', '#8a8c86'];
const GLOW = { core: '#d99a4a', edge: '#c47f3c' };
const SODIUM_LOW = { core: '#8a2f22', edge: '#76281d' };
// the dyes' lens colours (the spec page's set): muted, lifted for lit glass, never near-white
const DYE_GLOW = { white: '#d6d3c8', orange: '#d38a4c', magenta: '#b979ad', light_blue: '#7fa9bf', yellow: '#d2b964', lime: '#9cba66',
  pink: '#cf97a5', gray: '#8c8f91', light_gray: '#b0afa8', cyan: '#5fa6a6', purple: '#9270b4', blue: '#6e7cbd', brown: '#a7845f',
  green: '#86a058', red: '#c0665a', black: '#5c5c62' };
const darker = (hex, k) => '#' + [1, 3, 5].map((i) => Math.round(parseInt(hex.slice(i, i + 2), 16) * k).toString(16).padStart(2, '0')).join('');

// ---------- textures ----------
const T = textureSet(); T.file = 'light_';
const mott = (p, seed, lo = 0.15, hi = 0.85) => (x, y) => { const r = hash(x, y, seed); return r < lo ? p[0] : r > hi ? p[2] : p[1]; };
T.paint('iron', mott(IRON, 1, 0.12, 0.9));
// the cast-iron shaft: faint vertical flutes
T.paint('iron_shaft', (x, y) => (x % 2 === 0 ? '#2b2f33' : hash(x, y, 2) > 0.9 ? IRON[2] : '#33373c'));
// the fluted section of the foot: one groove every other pixel
T.paint('iron_flute', (x, y) => (x % 2 === 0 ? '#202326' : hash(x, y, 3) > 0.88 ? IRON[2] : IRON[1]));
// its south face carries the access hatch (block x 6-9, y 2-6 -> u 6-8, v 10-13): a dark outline, a brass hinge, a keyhole
T.paint('iron_hatch', (x, y) => {
  if (x >= 6 && x <= 8 && y >= 10 && y <= 13) {
    if (x === 6 && y === 11) return BRASS[1];
    if (x === 8 && y === 12) return '#141618';
    if (y === 10 || y === 13 || x === 6 || x === 8) return '#1d2023';
    return '#30343a';
  }
  return x % 2 === 0 ? '#202326' : IRON[1];
});
T.paint('iron_vent', (x, y) => (x % 2 === 0 ? '#16181a' : IRON[1]));
T.paint('galv', mott(GALV, 4, 0.15, 0.88));
// the steel foot plate: four bolt heads at its corners (plate x/z 4-12)
T.paint('galv_plate', (x, y) => ((x === 5 || x === 10) && (y === 5 || y === 10) ? STEEL[1] : mott(GALV, 5)(x, y)));
// the steel tube's south face at the foot: a flush access panel (u 7-8, v 9-13) with a screw
T.paint('galv_panel', (x, y) => {
  if (x >= 7 && x <= 8 && y >= 9 && y <= 13) return y === 9 || y === 13 ? '#4a5056' : x === 8 && y === 12 ? STEEL[1] : '#6a7076';
  return mott(GALV, 6)(x, y);
});
T.paint('steel', mott(STEEL, 7, 0.12, 0.9));
T.paint('steel_fins', mott(STEEL, 14, 0.3, 0.95));
T.paint('brass', mott(BRASS, 8, 0.2, 0.9));
T.paint('ceramic', (x, y) => (y % 3 === 0 ? CERAMIC[2] : hash(x, y, 9) > 0.85 ? CERAMIC[0] : CERAMIC[1]));
T.paint('copper', (x, y) => (hash(x, y, 10) > 0.88 ? '#a75a40' : '#b26247'));
T.paint('reflector', mott(ALU, 11, 0.2, 0.9));
T.paint('alu', mott(ALU, 12, 0.2, 0.9));
T.paint('wood', (x, y) => (x % 3 === 0 ? '#4e3626' : hash(x, y, 31) > 0.86 ? '#6f4d36' : '#634532'));
T.paint('wood_end', (x, y) => (Math.hypot(x - 7.5, y - 7.5) % 3 < 1 ? '#4e3626' : '#6f4d36'));
// unlit glass: smoky, a faint pane line and a couple of soft glints
T.paint('glass_off', (x, y) => ((x + y) % 7 === 0 ? '#575b5d' : (x === 3 && y === 4) || (x === 11 && y === 10) ? '#63686a' : '#474b4d'));
// lit glass: the core colour, a shade deeper toward the edges of the face
const glowTex = (g) => (x, y) => {
  const e = Math.min(x, y, 15 - x, 15 - y);
  return e < 2 ? g.edge : hash(x, y, 13) > 0.9 ? g.edge : g.core;
};
T.paint('glow', glowTex(GLOW));
T.paint('glow_sodium', glowTex(SODIUM_LOW));
// the brightness steps (Bright, Dim, Glow): the lit colour taken down toward dark glass
for (const [k, f] of [['b80', 0.82], ['b50', 0.58], ['b25', 0.4]]) T.paint('glow_' + k, glowTex({ core: darker(GLOW.core, f), edge: darker(GLOW.edge, f) }));
// the sodium warm-up between dark red and amber
const mix = (a, b, f) => '#' + [1, 3, 5].map((i) => Math.round(parseInt(a.slice(i, i + 2), 16) * (1 - f) + parseInt(b.slice(i, i + 2), 16) * f).toString(16).padStart(2, '0')).join('');
for (const [k, f] of [['w1', 0.4], ['w2', 0.75]]) T.paint('glow_' + k, glowTex({ core: mix(SODIUM_LOW.core, GLOW.core, f), edge: mix(SODIUM_LOW.edge, GLOW.edge, f) }));
for (const [k, v] of Object.entries(DYE_GLOW)) T.paint('glow_' + k, glowTex({ core: v, edge: darker(v, 0.86) }));

// ---------- geometry helpers ----------
// A round disc or rod along y: two crossed boxes (and a third for wider ones) so it reads round
function disc(name, cx, cz, y0, y1, w, tex, o = {}) {
  const h = w / 2, n = o.notch ?? (w >= 6 ? Math.round(w * 0.16 * 2) / 2 : w >= 4 ? 1 : w / 4);
  const faces = { all: tex, ...(o.faces || {}) };
  const out = [C(name + '_a', [cx - h, y0, cz - h + n], [cx + h, y1, cz + h - n], faces, o.cube), C(name + '_b', [cx - h + n, y0, cz - h], [cx + h - n, y1, cz + h], faces, o.cube)];
  if (w >= 6) { const m = h - n / 2; out.push(C(name + '_c', [cx - m, y0, cz - m], [cx + m, y1, cz + m], faces, o.cube)); }
  return out;
}
function zdisc(name, cx, cy, z0, z1, w, tex, o = {}) {
  const h = w / 2, n = o.notch ?? (w >= 6 ? Math.round(w * 0.16 * 2) / 2 : w >= 4 ? 1 : w / 4);
  const faces = { all: tex, ...(o.faces || {}) };
  const out = [C(name + '_a', [cx - h, cy - h + n, z0], [cx + h, cy + h - n, z1], faces, o.cube), C(name + '_b', [cx - h + n, cy - h, z0], [cx + h - n, cy + h, z1], faces, o.cube)];
  if (w >= 6) { const m = h - n / 2; out.push(C(name + '_c', [cx - m, cy - m, z0], [cx + m, cy + m, z1], faces, o.cube)); }
  return out;
}
const box = (name, f, t, tex, o = {}) => C(name, f, t, typeof tex === 'string' ? { all: tex } : tex, o);
const glowBox = (name, f, t, tex = 'glow', hide = []) => C(name, f, t, Object.fromEntries([['all', tex], ...hide.map((d) => [d, null])]), { shade: false });
const move = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]),
  rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
// swap every glow texture on a part list (dye, sodium warm-up, unlit)
const reglow = (cubes, tex) => cubes.map((c) => (c.shade === false && Object.values(c.faces).some((f) => f && f.startsWith('glow'))
  ? { ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([k, v]) => [k, v && v.startsWith('glow') ? tex : v])), shade: tex === 'glass_off' ? true : false } : c));
// Turn a part built pointing north (-z) to another side, about the block's centre column
const TURN = {
  north: { p: (x, z) => [x, z], f: {}, ax: { x: ['x', 1], z: ['z', 1] } },
  east: { p: (x, z) => [16 - z, x], f: { north: 'east', east: 'south', south: 'west', west: 'north' }, ax: { x: ['z', 1], z: ['x', -1] } },
  south: { p: (x, z) => [16 - x, 16 - z], f: { north: 'south', east: 'west', south: 'north', west: 'east' }, ax: { x: ['x', -1], z: ['z', -1] } },
  west: { p: (x, z) => [z, 16 - x], f: { north: 'west', east: 'north', south: 'east', west: 'south' }, ax: { x: ['z', -1], z: ['x', 1] } },
};
function turn(cubes, side) {
  const t = TURN[side];
  return cubes.map((c) => {
    const [ax, az] = t.p(c.from[0], c.from[2]), [bx, bz] = t.p(c.to[0], c.to[2]);
    const faces = {};
    for (const [k, v] of Object.entries(c.faces)) faces[t.f[k] || k] = v;
    let rot = c.rot;
    if (rot) {
      const [ox, oz] = t.p(rot[2][0], rot[2][2]);
      const [axis, sign] = rot[0] === 'y' ? ['y', 1] : t.ax[rot[0]];
      rot = [axis, rot[1] * sign, [ox, rot[2][1], oz]];
    }
    return { ...c, from: [Math.min(ax, bx), c.from[1], Math.min(az, bz)], to: [Math.max(ax, bx), c.to[1], Math.max(az, bz)], faces, rot, uv: undefined };
  });
}

// ---------- street: posts ----------
// Cast Iron: a stepped, fluted plinth with an access hatch on the south (the placer's side); a 4 px shaft; the head's collar
// and cap plate at the top of the block. Steel: a bolted base plate, a 3 px round tube, a clamp band for arms, a cap.
const IRON_COLLAR = [12.5, 14.5], STEEL_BAND = [12, 13];
function castIronPost(role) {
  const foot = role === 'single' || role === 'foot', head = role === 'single' || role === 'head';
  const out = [];
  let y = 0;
  if (foot) {
    out.push(box('plinth', [4, 0, 4], [12, 2, 12], { all: 'iron', down: null }));
    out.push(box('fluted', [5, 2, 5], [11, 6, 11], { all: 'iron_flute', south: 'iron_hatch', up: 'iron', down: null }));
    out.push(box('moulding', [5.5, 6, 5.5], [10.5, 7, 10.5], { all: 'iron', down: null }));
    y = 7;
  }
  const top = head ? IRON_COLLAR[0] : 16;
  out.push(box('shaft', [6, y, 6], [10, top, 10], { all: 'iron_shaft', up: null, down: null }));
  if (head) {
    out.push(box('collar', [5, IRON_COLLAR[0], 5], [11, IRON_COLLAR[1], 11], 'iron'));
    out.push(box('cap', [5.5, IRON_COLLAR[1], 5.5], [10.5, 16, 10.5], { all: 'iron', down: null }));
    out.push(...spanBracket(4, 6));
  }
  return out;
}
function steelPost(role) {
  const foot = role === 'single' || role === 'foot', head = role === 'single' || role === 'head';
  const out = [];
  let y = 0;
  if (foot) { out.push(box('plate', [4, 0, 4], [12, 1, 12], { all: 'galv', up: 'galv_plate', down: null })); y = 1; }
  const tube = (y0, y1, panel) => disc('tube', 8, 8, y0, y1, 3, 'galv', { faces: { up: null, down: null, ...(panel ? { south: 'galv_panel' } : {}) } });
  if (foot) { out.push(...tube(y, 7, true)); y = 7; }
  if (head) {
    out.push(...tube(y, STEEL_BAND[0]));
    out.push(box('band', [6, STEEL_BAND[0], 6], [10, STEEL_BAND[1], 10], 'steel'));
    out.push(...tube(STEEL_BAND[1], 15));
    out.push(box('cap', [6, 15, 6], [10, 16, 10], { all: 'steel', down: null }));
    out.push(...spanBracket(4, 6.5));
  } else out.push(...tube(y, 16));
  return out;
}
// The span insulator on a small iron bracket on the north side, low on the head block (clear of a north arm) (the Line Post's branch insulator, 0.8 scale)
function spanBracket(y, face) {
  return [box('bracket', [7.5, y - 1, 1.5], [8.5, y, face], 'iron'), ...insulatorStack(8, 2.5, y, 0.8, 'span_ins', 'copper')];
}
function insulatorStack(cx, cz, y0, k, name, capTex) {
  const h = 1.5 * k, out = [];
  let y = y0;
  for (const [nm, w] of [['d1', 5 * k], ['n1', 3 * k], ['d2', 5 * k]]) { out.push(...disc(name + '_' + nm, cx, cz, y, y + h, w, 'ceramic', { notch: w >= 4 ? 1 : 0.5 })); y += h; }
  out.push(box(name + '_cap', [cx - 1, y, cz - 1], [cx + 1, y + 1.5, cz + 1], { all: capTex, down: null }));
  return out;
}
const spanTie = (y = 4) => [8, y + 3.6 + 1.5, 2.5];

// ---------- street: arms (built pointing north) ----------
// tip anchors: the end of the arm, at the centre of its bar
const ANCHOR = { iron: [8, 13.5, -6.5], steel: [8, 14.3, -4.6] };
function arm(finish, tip) {
  const out = [];
  if (finish === 'iron') {
    // a 2 px bar out level from the collar, braced by a scroll of stepped 1 px cubes curling up from the shaft
    out.push(box('arm_bar', [7, 12.5, -6.5], [9, 14.5, 5], { all: 'iron', south: null }));
    const scroll = [[9.5, 11, 5, 6], [10.5, 11.5, 4, 5], [11, 12, 2.5, 4], [11.5, 12.5, 0, 2.5]];
    scroll.forEach(([y0, y1, z0, z1], i) => out.push(box('scroll_' + i, [7.5, y0, z0], [8.5, y1, z1], 'iron')));
    out.push(box('scroll_curl', [7.5, 8.5, 4.5], [8.5, 9.5, 5.5], 'iron'));
  } else {
    // a 2 px tube out level from the clamp band, then a gentle rise, then a short level spigot
    out.push(box('arm_root', [7, 11.25, 3], [9, 12.75, 6.5], { all: 'galv', south: null }));
    out.push(box('arm_rise', [7, 11.5, -3], [9, 13, 3], 'galv', { rot: ['x', 22.5, [8, 12.25, 3]] }));
    out.push(box('arm_spigot', [7, 13.55, -5.6], [9, 15.05, -2.6], 'galv'));
  }
  const [ax, ay, az] = ANCHOR[finish];
  if (tip === 'hook') {
    out.push(box('hook', [ax - 0.5, ay - 2.5, az + 1], [ax + 0.5, ay - 1, az + 2], 'iron'));
    out.push(box('hook_ring', [ax - 0.75, ay - 3, az + 0.75], [ax + 0.75, ay - 2.5, az + 2.25], 'iron'));
  } else if (tip === 'cup') {
    out.push(box('cup', [ax - 1.5, ay + 0.75, az + 0.25], [ax + 1.5, ay + 1.75, az + 3.25], 'iron'));
  }
  return out;
}

// ---------- street: heads, standing on top of a block (y 0 is the block top) ----------
function lantern(withBase = true) {
  const out = [];
  if (withBase) out.push(box('l_base', [6, 0, 6], [10, 1.5, 10], { all: 'iron', down: 'iron' }));
  out.push(box('l_sill', [5, 1.5, 5], [11, 2.5, 11], 'iron'));
  for (const [x, z] of [[5, 5], [10, 5], [5, 10], [10, 10]]) out.push(box(`l_post_${x}_${z}`, [x, 2.5, z], [x + 1, 7.5, z + 1], { all: 'iron', up: null, down: null }));
  out.push(glowBox('l_panes', [5.5, 2.5, 5.5], [10.5, 7.5, 10.5], 'glow', ['up', 'down']));
  out.push(box('l_band', [5, 7.5, 5], [11, 8.5, 11], 'iron'));
  out.push(box('l_vent', [5.5, 8.5, 5.5], [10.5, 9.5, 10.5], { all: 'iron_vent', up: null, down: null }));
  out.push(box('l_roof1', [4.5, 9.5, 4.5], [11.5, 10.5, 11.5], 'iron'));
  out.push(box('l_roof2', [5.5, 10.5, 5.5], [10.5, 11.5, 10.5], { all: 'iron', down: null }));
  out.push(box('l_roof3', [6.5, 11.5, 6.5], [9.5, 12.5, 9.5], { all: 'iron', down: null }));
  out.push(box('l_roof4', [7.5, 12.5, 7.5], [8.5, 13.5, 8.5], { all: 'iron', down: null }));
  out.push(box('l_finial', [7.25, 13.5, 7.25], [8.75, 15, 8.75], { all: 'brass', down: null }));
  return out;
}
// The Cobra Head's housing, level, its back at z0 and its nose 12 px north; its lens underneath
function cobraHousing(y, z0) {
  return [
    box('c_body', [5.5, y, z0 - 6], [10.5, y + 3, z0], { all: 'steel' }),
    box('c_nose', [5.5, y + 0.5, z0 - 12], [10.5, y + 2.5, z0 - 6], { all: 'steel', south: null }),
    box('c_dome', [6.5, y + 3, z0 - 10], [9.5, y + 3.5, z0 - 0.5], { all: 'steel', down: null }),
    box('c_ridge', [7.5, y + 3.5, z0 - 8], [8.5, y + 4, z0 - 1], { all: 'steel', down: null }),
    glowBox('c_lens', [6.5, y - 0.5, z0 - 11], [9.5, y + 0.5, z0 - 3], 'glow', ['up']),
  ];
}
function cobraTop() {
  // its own gooseneck: up 5 px from a footing, then out over the street
  return [
    box('c_footing', [6.5, 0, 6.5], [9.5, 1, 9.5], { all: 'steel', down: 'steel' }),
    box('c_neck', [7, 1, 7], [9, 6, 9], { all: 'galv', up: null, down: null }),
    box('c_bend', [7, 4.6, 4.2], [9, 6.6, 9], 'galv', { rot: ['x', 22.5, [8, 5.6, 8]] }),
    ...cobraHousing(6, 5.5),
  ];
}
// The Globe: a stepped ball on a brass collar ring and a 2 px neck; y0 is where its neck starts
function globe(y0 = 0) {
  const out = [box('g_neck', [7, y0, 7], [9, y0 + 2, 9], { all: 'iron', down: 'iron' }), box('g_collar', [6, y0 + 2, 6], [10, y0 + 3, 10], 'brass')];
  let y = y0 + 3;
  for (const [w, h] of [[4, 1], [6, 1], [8, 4], [6, 1], [4, 1]]) {
    out.push(...disc('g_' + w + '_' + y, 8, 8, y, y + h, w, 'glow', { cube: { shade: false } }));
    y += h;
  }
  return out;
}
// The Yard Light: a flat finned box tilted 22.5 degrees down to the north about its yoke pin, its lens underneath, a ballast
// box behind. pin: the yoke pin, in block space.
function yardBox(pin) {
  const [px, py, pz] = pin, R = { rot: ['x', -22.5, pin] };
  const b = (name, f, t, tex) => box(name, [f[0] + px, f[1] + py, f[2] + pz], [t[0] + px, t[1] + py, t[2] + pz], tex, R);
  return [
    b('y_box', [-5, -1.5, -8], [5, 1.5, -1], 'steel'),
    b('y_fin1', [-4, 1.5, -7], [-3, 2.5, -2], 'steel_fins'),
    b('y_fin2', [-0.5, 1.5, -7], [0.5, 2.5, -2], 'steel_fins'),
    b('y_fin3', [3, 1.5, -7], [4, 2.5, -2], 'steel_fins'),
    { ...b('y_lens', [-4, -2, -7.5], [4, -1.5, -2.5], 'glow'), faces: { all: 'glow', up: null }, shade: false },
    b('y_yoke_l', [-5.75, -1, -1.5], [-5, 1, 0.5], 'iron'),
    b('y_yoke_r', [5, -1, -1.5], [5.75, 1, 0.5], 'iron'),
    box('y_yoke_bar', [px - 5.75, py - 1, pz + 0.5], [px + 5.75, py, pz + 1.5], 'iron'),
    box('y_ballast', [px - 1.5, py - 1, pz + 1.5], [px + 1.5, py + 1.75, pz + 4], 'steel'),
  ];
}
function yardTop() {
  return [box('y_stub', [7, 0, 7], [9, 3, 9], { all: 'galv', down: 'galv' }), ...yardBox([8, 4, 8])];
}

// A head on an arm, by finish, built pointing north
function armHead(kind, finish) {
  const [ax, ay, az] = ANCHOR[finish];
  switch (kind) {
    case 'lantern': return move(lantern(false), [ax - 8, ay - 3 - 15 + 0.01, az + 1.5 - 8]);
    case 'globe': return move(globe(0), [ax - 8, ay + 1.75, az + 1.5 - 8]);
    case 'cobra': return cobraHousing(ay - 1.5, az + 2.5);
    case 'yard': return yardBox([ax, ay - 1.25, az - 1]);
  }
}
const ARM_TIP = { lantern: 'hook', globe: 'cup', cobra: null, yard: null };

// ---------- preview ----------
const { cube, group, clip, write } = preview('electric-lights-' + GROUP, { 'el:': T });
const outliner = [];
const placed = (list, bx, by, bz) => list.map((c) => cube(c, [bx * 16, by * 16, bz * 16]));
const blockOf = (name, list, bx, by, bz, extra = []) => group(name, [8, 0, 8], [bx * 16, by * 16, bz * 16], [...placed(list, bx, by, bz), ...extra]);
const stand = (name, tex, bx, by, bz) => blockOf(name, [C(name, [0, 0, 0], [16, 16, 16], { all: tex })], bx, by, bz);
const pfx = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));

// A pole: role blocks stacked up from (bx, 0, bz), arms on the head, a head lamp on top (or none)
function pole(name, finish, height, top, arms = [], glow = {}) {
  const post = finish === 'iron' ? castIronPost : steelPost;
  const kids = [];
  for (let i = 0; i < height; i++) {
    const role = height === 1 ? 'single' : i === 0 ? 'foot' : i === height - 1 ? 'head' : 'pole';
    let list = post(role);
    if (i === height - 1) for (const [side, kind] of arms) {
      const part = [...arm(finish, ARM_TIP[kind]), ...armHead(kind, finish)];
      list = list.concat(pfx(turn(glow[side] ? reglow(part, glow[side]) : part, side), side + '_'));
    }
    kids.push(blockOf(`${name}_${role}${height > 2 && role === 'pole' ? i : ''}`, list, POS[name][0], i + (POS[name][2] || 0), POS[name][1]));
  }
  if (top) { const t = turn(TOPS[top](), glow.topSide || 'north'); kids.push(blockOf(name + '_top', glow.top ? reglow(t, glow.top) : t, POS[name][0], height + (POS[name][2] || 0), POS[name][1])); }
  return group(name, [8, 0, 8], [POS[name][0] * 16, 0, POS[name][1] * 16], kids);
}
const TOPS = { lantern: () => lantern(true), cobra: cobraTop, globe: () => globe(0), yard: yardTop };
const POS = {};

if (GROUP === 'street') {
  // the scene: a street of poles along x, the dye row and the head pairs in front (south, toward the camera)
  const poles = [
    ['iron_lantern', 'iron', 4, 'lantern', []],
    ['iron_hanging', 'iron', 4, null, [['east', 'lantern'], ['west', 'lantern']]],
    ['steel_double_cobra', 'steel', 5, null, [['east', 'cobra'], ['west', 'cobra']]],
    ['steel_cobra_top', 'steel', 4, 'cobra', []],
    ['iron_plaza', 'iron', 3, 'globe', [['north', 'globe'], ['east', 'globe'], ['south', 'globe'], ['west', 'globe']]],
    ['steel_yard', 'steel', 5, null, [['east', 'yard'], ['west', 'yard']]],
    ['iron_single', 'iron', 1, null, []],
    ['steel_single', 'steel', 1, null, []],
  ];
  const xs = [0, 3, 6, 9, 12, 15, 18, 20];
  poles.forEach((p, i) => { POS[p[0]] = [xs[i], 0]; });
  for (const [name, finish, h, top, arms] of poles) outliner.push(pole(name, finish, h, top, arms, name === 'steel_yard' ? { west: 'glow_sodium' } : name === 'steel_cobra_top' ? { topSide: 'east' } : {}));

  // a wooden Line Post 4 blocks west of the first pole, and a copper span between the two insulators
  const lineX = -4;
  const wood = [box('lp_foot_a', [5, 0, 6], [11, 1.5, 10], { all: 'steel', down: null }), box('lp_foot_b', [6, 0, 5], [10, 1.5, 11], { all: 'steel', down: null })];
  const lineKids = [];
  for (let i = 0; i < 4; i++) {
    const list = [...(i === 0 ? wood : []), box('lp_pole', [6.5, i === 0 ? 1.5 : 0, 6.5], [9.5, i === 3 ? 10.5 : 16, 9.5], { all: 'wood', down: null, up: i === 3 ? 'wood_end' : null })];
    if (i === 3) list.push(...insulatorStack(8, 8, 10.5, 1, 'lp_ins', 'copper'));
    lineKids.push(blockOf('line_post_' + i, list, lineX, i, 0));
  }
  outliner.push(group('line_post', [8, 0, 8], [lineX * 16, 0, 0], lineKids));
  const a = [lineX * 16 + 8, 3 * 16 + 10.5 + 4.5 + 1.5, 8], tie = spanTie(4), b = [0 * 16 + tie[0], 3 * 16 + tie[1], tie[2]];
  const span = [], N = 40, sag = Math.max(8, Math.hypot(b[0] - a[0], b[2] - a[2]) / 24);
  for (let k = 0; k < N; k++) {
    const t = (k + 0.5) / N, x = a[0] + (b[0] - a[0]) * t, z = a[2] + (b[2] - a[2]) * t, y = a[1] + (b[1] - a[1]) * t - sag * 4 * t * (1 - t);
    const len = Math.hypot(b[0] - a[0], b[2] - a[2]) / N;
    span.push(C('span_' + k, [x - len / 2 - 0.05, y - 0.3, z - 0.3], [x + len / 2 + 0.05, y + 0.3, z + 0.3], { all: 'copper' }));
  }
  outliner.push(group('span', [0, 0, 0], [0, 0, 0], span.map((c) => cube(c, [0, 0, 0]))));

  // dye row: Globes on top of andesite blocks, five colours, and the plain one
  const dyes = [null, 'red', 'light_blue', 'lime', 'purple', 'yellow'];
  const dyeKids = [];
  dyes.forEach((d, i) => {
    dyeKids.push(stand('dye_block_' + i, 'minecraft:block/polished_andesite', i * 2, 0, 5));
    const g = globe(0);
    dyeKids.push(blockOf('dye_globe_' + (d || 'plain'), d ? reglow(g, 'glow_' + d) : g, i * 2, 1, 5));
  });
  outliner.push(group('dye_row', [8, 0, 8], [0, 0, 80], dyeKids));

  // head pairs, lit and unlit, on andesite blocks
  const pairKids = [];
  ['lantern', 'cobra', 'globe', 'yard'].forEach((k, i) => {
    for (const lit of [true, false]) {
      const x = i * 4 + (lit ? 0 : 1.5) + 0;
      pairKids.push(stand(`pair_block_${k}_${lit ? 'lit' : 'off'}`, 'minecraft:block/polished_andesite', x, 0, 9));
      const hd = turn(TOPS[k](), 'east'); pairKids.push(blockOf(`pair_${k}_${lit ? 'lit' : 'off'}`, lit ? hd : reglow(hd, 'glass_off'), x, 1, 9));
    }
  });
  outliner.push(group('head_pairs', [8, 0, 8], [0, 0, 144], pairKids));
}

// ---------- group 2 and 3 textures ----------
// the lit inside of a bell or shade, the glow's edge colour a little deeper
T.paint('bell_inner', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r < 4 ? (hash(x, y, 15) > 0.7 ? '#c98c45' : '#cf9449') : r < 6.5 ? '#bb8040' : '#a06a35'; });
T.paint('flex', (x, y) => (hash(x, y, 16) > 0.85 ? '#353638' : '#2b2c2e'));
// amber glass over the Warning Beacon's reflector: part-clear, so the rotor shows through
T.paint('amber_glass', (x, y) => ((x + y) % 6 === 0 ? '#d9a35cb0' : '#c47f3c90'));
// the Indicator's bezel front (block x/y 6-10 -> u 6-9, v 6-9): a muted brass ring on dark steel
T.paint('bezel', (x, y) => (x >= 6 && x <= 9 && y >= 6 && y <= 9 && (x === 6 || x === 9 || y === 6 || y === 9) ? BRASS[1] : STEEL[1]));
// the Light Switch plate (block x 5-11, y 4-12 -> u 5-10, v 4-11): andesite grey, two screws above and below the toggle
T.paint('switch_plate', (x, y) => ((x === 7 || x === 8) && (y === 5 || y === 10) ? (x === 7 ? '#6b6e6a' : '#5f625f') : x === 5 || x === 10 || y === 4 || y === 11 ? STEEL[0] : mott(STEEL, 17)(x, y)));
// ...and laid on the floor across a wire along x, as line fittings are (block x 4-12 along the wire, z 5-11 across it)
T.paint('switch_plate_floor', (x, y) => ((y === 7 || y === 8) && (x === 5 || x === 10) ? (y === 7 ? '#6b6e6a' : '#5f625f') : y === 5 || y === 10 || x === 4 || x === 11 ? STEEL[0] : mott(STEEL, 17)(y, x)));
T.paint('bolt_plate', (x, y) => ((x === 5 || x === 10) && (y === 5 || y === 10) ? STEEL[0] : mott(STEEL, 18)(x, y)));

// A square ring of four bars round an opening: outer o, inner i, centred on (cx, cz), from y0 to y1
function ringFrame(name, cx, cz, y0, y1, o, i, tex) {
  const a = o / 2, b = i / 2;
  return [
    box(name + '_n', [cx - a, y0, cz - a], [cx + a, y1, cz - b], tex), box(name + '_s', [cx - a, y0, cz + b], [cx + a, y1, cz + a], tex),
    box(name + '_w', [cx - a, y0, cz - b], [cx - b, y1, cz + b], tex), box(name + '_e', [cx + b, y0, cz - b], [cx + a, y1, cz + b], tex),
  ];
}
// Turn a part built for a floor (standing on y 0) onto the north wall (its top pointing south, its back on z 0)
const toWall = (cubes) => cubes.map((c) => {
  const p = ([x, y, z]) => [x, 16 - z, y];
  const a = p(c.from), b = p(c.to);
  const f = { up: 'south', down: 'north', north: 'up', south: 'down' };
  const faces = {};
  for (const [k, v] of Object.entries(c.faces)) faces[f[k] || k] = v;
  return { ...c, from: a.map((v, i) => Math.min(v, b[i])), to: a.map((v, i) => Math.max(v, b[i])), faces, rot: c.rot && c.rot[0] === 'y' ? ['z', c.rot[1], p(c.rot[2])] : c.rot };
});

// ---------- group 2: wall (built on the north wall, z 0, facing south) ----------
function bulkhead() {
  const out = [
    box('b_body_a', [4, 6, 0], [12, 10, 2.5], { all: 'steel', north: null }), box('b_body_b', [5, 5, 0], [11, 11, 2.5], { all: 'steel', north: null }),
    box('b_rim_t', [5, 10, 2.5], [11, 11, 3], 'steel'), box('b_rim_b', [5, 5, 2.5], [11, 6, 3], 'steel'),
    box('b_rim_w', [4, 6, 2.5], [5, 10, 3], 'steel'), box('b_rim_e', [11, 6, 2.5], [12, 10, 3], 'steel'),
    glowBox('b_lens', [5, 6, 2.5], [11, 10, 2.75], 'glow', ['north', 'east', 'west', 'up', 'down']),
    box('b_guard_v1', [6.5, 6, 3], [7.5, 10, 3.5], 'iron'), box('b_guard_v2', [8.5, 6, 3], [9.5, 10, 3.5], 'iron'),
    box('b_guard_h1', [5, 7.5, 3], [6.5, 8.5, 3.5], 'iron'), box('b_guard_h2', [7.5, 7.5, 3], [8.5, 8.5, 3.5], 'iron'), box('b_guard_h3', [9.5, 7.5, 3], [11, 8.5, 3.5], 'iron'),
  ];
  return out;
}
function sconce() {
  const out = [box('s_plate', [6.5, 4, 0], [9.5, 9, 0.5], { all: 'brass', north: null }),
    box('s_arm_out', [7.5, 5, 0.5], [8.5, 6, 4], 'brass'), box('s_arm_up', [7.5, 6, 3], [8.5, 7.5, 4], 'brass')];
  let y = 7.5;
  for (const [w, h] of [[3, 1], [4, 1.5], [5, 1.5]]) { out.push(...disc('s_shade_' + w, 8, 4.5, y, y + h, w, 'glow', { cube: { shade: false } })); y += h; }
  out.push(...disc('s_rim', 8, 4.5, 11.5, 12.25, 6, 'brass', { faces: { up: null, down: null } }));
  return out;
}
function gooseneck() {
  const t = 'steel';
  return [
    box('g_plate', [6, 10, 0], [10, 14, 1], { all: t, north: null }),
    box('g_out', [7.25, 11.25, 1], [8.75, 12.75, 4], t),
    box('g_rise', [7.25, 11.25, 4], [8.75, 12.75, 9], t, { rot: ['x', -45, [8, 12, 4]] }),
    box('g_top', [7.25, 14.8, 7.3], [8.75, 16, 9.8], t),
    box('g_fall', [7.25, 14.8, 9.8], [8.75, 16, 13.3], t, { rot: ['x', 45, [8, 15.4, 9.8]] }),
    box('g_drop', [7.25, 11, 11.5], [8.75, 13.3, 13], t),
    ...disc('g_shade3', 8, 12.25, 10, 11, 3, t),
    ...disc('g_shade5', 8, 12.25, 9, 10, 5, t),
    ...disc('g_shade7', 8, 12.25, 8, 9, 7, t, { faces: { down: 'reflector' } }),
    glowBox('g_lamp', [6.5, 7.5, 10.75], [9.5, 8, 13.75], 'glow', ['up']),
  ];
}
// ---------- group 2: ceiling (hung from the block above: y 16 is the ceiling) ----------
function highBay() {
  const out = [...disc('h_plate', 8, 8, 15.5, 16, 4, 'steel'), box('h_rod', [7, 12.5, 7], [9, 15.5, 9], { all: 'galv', up: null, down: null }),
    box('h_driver', [5.5, 9.5, 5.5], [10.5, 12.5, 10.5], { all: 'steel_fins', up: 'steel', down: 'steel' })];
  for (const [w, y0, y1] of [[6, 8.5, 9.5], [9, 7, 8.5], [11, 5.5, 7], [12, 3.5, 5.5]]) out.push(...disc('h_dome' + w, 8, 8, y0, y1, w, 'steel', { faces: { down: w === 12 ? 'bell_inner' : 'steel' } }));
  out.push(...disc('h_rim', 8, 8, 3.25, 4.5, 13, 'steel', { faces: { up: null, down: null } }));
  out.push(...disc('h_lamp', 8, 8, 3, 4.5, 5, 'glow', { cube: { shade: false } }));
  return out;
}
function cageLamp() {
  return [...disc('k_plate', 8, 8, 15, 16, 6, 'steel'),
    ...disc('k_glass', 8, 8, 10, 15, 4, 'glow', { cube: { shade: false }, faces: { up: null } }),
    box('k_bar_w', [5, 10, 7.5], [6, 15, 8.5], 'iron'), box('k_bar_e', [10, 10, 7.5], [11, 15, 8.5], 'iron'),
    box('k_bar_n', [7.5, 10, 5], [8.5, 15, 6], 'iron'), box('k_bar_s', [7.5, 10, 10], [8.5, 15, 11], 'iron'),
    ...disc('k_ring', 8, 8, 9, 10, 6, 'iron'), box('k_cap', [7.5, 8.5, 7.5], [8.5, 9, 8.5], 'iron')];
}
function flush() {
  return [...disc('f_body', 8, 8, 14.5, 16, 10, 'brass', { faces: { up: null } }), ...disc('f_glow', 8, 8, 14.25, 14.5, 8, 'glow', { cube: { shade: false }, faces: { up: null } })];
}
// ---------- group 2: hanging; drop 0 hangs in the block under the ceiling, each extra block of drop a cable segment ----------
const rose = (w, tex) => disc('rose', 8, 8, 15.5, 16, w, tex);
const flexSeg = (y0, y1) => box('flex', [7.5, y0, 7.5], [8.5, y1, 8.5], { all: 'flex', up: null, down: null });
function pendantShade() {
  const out = [];
  for (const [w, y0, y1] of [[2, 8, 9], [4, 7, 8], [6, 6, 7], [7, 5, 6]]) out.push(...disc('p_shade' + w, 8, 8, y0, y1, w, 'steel', { faces: { down: w === 7 ? 'reflector' : 'steel' } }));
  out.push(...disc('p_bulb', 8, 8, 4.5, 5.5, 3, 'glow', { cube: { shade: false } }));
  return out;
}
function chandelierBody() {
  const out = [box('ch_stem_lo', [7, 6, 7], [9, 8.5, 9], { all: 'brass', down: null }), ...disc('ch_knop', 8, 8, 8.5, 10, 3, 'brass'),
    box('ch_stem_hi', [7, 10, 7], [9, 12, 9], { all: 'brass', up: null, down: null }), ...disc('ch_hub', 8, 8, 5, 6, 4, 'brass'),
    box('ch_finial', [7.5, 3.5, 7.5], [8.5, 5, 8.5], 'brass')];
  const armN = [box('arm_out', [7.5, 5, 1.5], [8.5, 6, 6], 'brass'), box('arm_up', [7.5, 6, 1.5], [8.5, 8, 2.5], 'brass'),
    box('arm_cup', [7, 8, 1], [9, 9, 3], 'brass'), glowBox('arm_bulb', [7.5, 9, 1.5], [8.5, 11, 2.5])];
  for (const side of ['north', 'east', 'south', 'west']) out.push(...pfx(turn(armN, side), side + '_'));
  // the four diagonal arms: the same, turned 45 degrees about the stem
  for (const side of ['north', 'east', 'south', 'west']) out.push(...pfx(turn(armN, side), 'd' + side + '_').map((c) => ({ ...c, rot: ['y', 45, [8, 5, 8]] })));
  return out;
}
const segment = () => [flexSeg(0, 16)];
// a hanging lamp: list of block part-lists from the ceiling block down
function hanging(kind, drop) {
  const top = kind === 'pendant' ? rose(3, 'steel') : rose(3, 'brass');
  const lamp = kind === 'pendant' ? pendantShade() : chandelierBody();
  const lampTop = kind === 'pendant' ? 9 : 12;
  if (drop === 0) return [[...top, flexSeg(lampTop, 15.5), ...lamp]];
  const blocks = [[...top, flexSeg(0, 15.5)]];
  for (let i = 1; i < drop; i++) blocks.push(segment());
  blocks.push([flexSeg(lampTop, 16), ...lamp]);
  return blocks;
}

// ---------- group 3: runs ----------
// Light Strip on the floor along x, from a to b; ends: cap the run's open ends
function stripFloor(a, b, capA, capB) {
  const out = [box('st_chan', [a, 0, 7], [b, 1, 9], { all: 'alu', down: null }), glowBox('st_diff', [a, 1, 7.5], [b, 1.25, 8.5], 'glow', ['down'])];
  if (capA) out.push(box('st_capA', [a - 0.25, 0, 6.75], [a + 0.5, 1.5, 9.25], 'alu'));
  if (capB) out.push(box('st_capB', [b - 0.5, 0, 6.75], [b + 0.25, 1.5, 9.25], 'alu'));
  return out;
}
// The Light Strip in the game: parts a strip block is built from, as Power Line wires are (StripModel turns them onto
// each face and direction): lying on the floor and running north from the middle. Each is its aluminium channel and,
// separately, its diffuser (lit and tinted, or the unlit glass). A strip goes straight through a block to its edges; at
// a junction or a turn it's the middle and an arm to each strip it meets. Up the next face of the same block (inside
// corner) the arm stops a pixel short, and one of the two draws the corner; over an edge (outside corner) both draw the
// fold, as wires do. An open end gets a cap at the block's edge. Each arm's diffuser runs to the middle, so two arms
// meet there without a gap (the middle itself is only channel).
function stripParts() {
  const chan = (n, f, t) => box(n, f, t, { all: 'alu', down: null });
  return {
    centre: { body: [chan('st_centre', [7, 0, 7], [9, 1, 9])], diffuser: [] },
    arm: { body: [chan('st_arm', [7, 0, 0], [9, 1, 7])], diffuser: [glowBox('st_arm_d', [7.5, 1, 0], [8.5, 1.25, 8], 'glow', ['down'])] },
    arm_inner: { body: [chan('st_arm_in', [7, 0, 1], [9, 1, 7])], diffuser: [glowBox('st_arm_in_d', [7.5, 1, 1], [8.5, 1.25, 8], 'glow', ['down'])] },
    corner: { body: [box('st_corner', [7, 0, 0], [9, 1, 1], { all: 'alu', down: null, north: null })], diffuser: [] },
    fold: { body: [box('st_fold', [7, 0, -1], [9, 1, 0], { all: 'alu', down: null, south: null })],
      diffuser: [glowBox('st_fold_d', [7.5, 0, -1.25], [8.5, 1.25, 0], 'glow', ['down', 'south'])] },
    cap: { body: [box('st_cap', [6.75, 0, -0.25], [9.25, 1.5, 0.5], { all: 'alu', down: null })], diffuser: [] },
  };
}
// Edge Light: an aluminium angle along the north-bottom edge of the block (sitting on the face below, at its north edge),
// a lit bar along its outer edge; x from a to b
// (the back stands at the edge and the lit chamfer faces in across the face, away from the edge: trim along a wall's top
// edge shines down the wall, along its bottom edge up it; changed 2026-10-28, the first version faced out over the edge)
function edgeX(a, b) {
  return [box('e_back', [a, 0, 0], [b, 2, 1.5], 'alu'), box('e_lip', [a, 0, 1.5], [b, 0.75, 2.5], 'alu'), glowBox('e_glow', [a, 0.75, 1.5], [b, 2.25, 2.25], 'glow', ['north', 'down'])];
}
// The Edge Light in the game: parts EdgeModel builds an edge-light block from, lying on the floor at its north edge and
// running along x (turned onto each face and edge as wires' parts are). The profile as edgeX, split: the middle, and each
// end as it meets the next block's edge (or the edge it mitres with in the same block), or capped where nothing meets it.
// Each is its aluminium body and, separately, its lit chamfer (lit and tinted, or the unlit glass).
function edgeParts() {
  const body = (n, a, b) => [box(n + '_back', [a, 0, 0], [b, 2, 1.5], { all: 'alu', down: null }), box(n + '_lip', [a, 0, 1.5], [b, 0.75, 2.5], { all: 'alu', down: null })];
  const chamfer = (n, a, b) => [glowBox(n + '_glow', [a, 0.75, 1.5], [b, 2.25, 2.25], 'glow', ['north', 'down'])];
  const cap = (n, x0, x1) => [box(n, [x0, 0, 0], [x1, 2.5, 2.5], { all: 'alu', down: null })];
  return {
    mid: { body: body('ed_mid', 2.5, 13.5), diffuser: chamfer('ed_mid', 2.5, 13.5) },
    join_e: { body: body('ed_je', 13.5, 16), diffuser: chamfer('ed_je', 13.5, 16) },
    join_w: { body: body('ed_jw', 0, 2.5), diffuser: chamfer('ed_jw', 0, 2.5) },
    cap_e: { body: [...body('ed_ce', 13.5, 15.25), ...cap('ed_ce_cap', 15.25, 16)], diffuser: chamfer('ed_ce', 13.5, 15.25) },
    cap_w: { body: [...body('ed_cw', 0.75, 2.5), ...cap('ed_cw_cap', 0, 0.75)], diffuser: chamfer('ed_cw', 0.75, 2.5) },
  };
}
function edgeZ(a, b) {
  return [box('e_back', [0, 0, a], [1.5, 2, b], 'alu'), box('e_lip', [1.5, 0, a], [2.5, 0.75, b], 'alu'), glowBox('e_glow', [1.5, 0.75, a], [2.25, 2.25, b], 'glow', ['west', 'down'])];
}
// ---------- group 3: ground ----------
function bollard() {
  return [...disc('bo_flange', 8, 8, 0, 1, 7, 'bolt_plate', { faces: { down: 'steel' } }), ...disc('bo_post', 8, 8, 1, 6, 5, 'steel', { faces: { up: null, down: null } }),
    ...disc('bo_louvre1', 8, 8, 6, 7, 5, 'steel'), ...disc('bo_louvre2', 8, 8, 8, 9, 5, 'steel'), ...disc('bo_louvre3', 8, 8, 10, 11, 5, 'steel'),
    ...disc('bo_core', 8, 8, 7, 10, 4, 'glow', { cube: { shade: false }, faces: { up: null, down: null } }),
    ...disc('bo_cap', 8, 8, 11, 12, 6, 'steel'), ...disc('bo_dome', 8, 8, 12, 13, 4, 'steel')];
}
function floorLight() {
  return [box('fl_body', [0, 0, 0], [16, 15, 16], { all: 'create:block/andesite_casing', up: null }),
    box('fl_frame_n', [0, 15, 0], [16, 16, 3], { all: 'bolt_plate', down: null }), box('fl_frame_s', [0, 15, 13], [16, 16, 16], { all: 'bolt_plate', down: null }),
    box('fl_frame_w', [0, 15, 3], [3, 16, 13], { all: 'bolt_plate', down: null }), box('fl_frame_e', [13, 15, 3], [16, 16, 13], { all: 'bolt_plate', down: null }),
    glowBox('fl_panel', [3, 15, 3], [13, 15.75, 13], 'glow', ['down', 'north', 'south', 'east', 'west']),
    box('fl_cross_x1', [3, 15.75, 7.75], [7.75, 16, 8.25], { all: 'steel', down: null }), box('fl_cross_x2', [8.25, 15.75, 7.75], [13, 16, 8.25], { all: 'steel', down: null }),
    box('fl_cross_z', [7.75, 15.75, 3], [8.25, 16, 13], { all: 'steel', down: null })];
}
// ---------- group 3: specials ----------
// Floodlight on a floor; returns { base, yoke, head } so the yoke and head can be posed
function floodlight() {
  return {
    base: [box('fd_plate', [5, 0, 5], [11, 1, 11], { all: 'bolt_plate', down: 'steel' }), ...disc('fd_turntable', 8, 8, 1, 2, 4, 'steel')],
    yoke: [box('fd_yoke_bar', [2.5, 2, 7.5], [13.5, 3, 8.5], 'iron'), box('fd_yoke_w', [2.5, 3, 7.5], [3.5, 8.5, 8.5], 'iron'), box('fd_yoke_e', [12.5, 3, 7.5], [13.5, 8.5, 8.5], 'iron')],
    head: [...zdisc('fd_body', 8, 7.5, 5, 11, 8, 'steel'),
      ...zdisc('fd_bezel', 8, 7.5, 4.5, 5, 8, 'reflector', { faces: { south: null } }),
      ...zdisc('fd_lens', 8, 7.5, 4.25, 4.5, 6, 'glow', { cube: { shade: false }, faces: { south: null } }),
      box('fd_visor', [4.5, 11.5, 3], [11.5, 12, 7], 'steel'),
      ...zdisc('fd_fin1', 8, 7.5, 8, 8.5, 8.5, 'steel'), ...zdisc('fd_fin2', 8, 7.5, 9.25, 9.75, 8.5, 'steel'), ...zdisc('fd_fin3', 8, 7.5, 10.25, 10.75, 8.5, 'steel'),
      box('fd_back', [6, 5.5, 11], [10, 9.5, 12], 'steel'),
      box('fd_knob_w', [1.5, 6.75, 7.25], [2.5, 8.25, 8.75], 'brass'), box('fd_knob_e', [13.5, 6.75, 7.25], [14.5, 8.25, 8.75], 'brass')],
  };
}
function beacon() {
  return {
    base: [box('wb_base', [5, 0, 5], [11, 2, 11], { all: 'steel', down: 'steel' }),
      ...[['wb_dome5', 2, 4.5, 5], ['wb_dome4', 4.5, 6, 4], ['wb_dome3', 6, 7, 3]].flatMap(([n, y0, y1, w]) => disc(n, 8, 8, y0, y1, w, 'amber_glass', { cube: { shade: false } })),
      box('wb_guard_w', [4.5, 2, 7.5], [5, 7.5, 8.5], 'iron'), box('wb_guard_e', [11, 2, 7.5], [11.5, 7.5, 8.5], 'iron'),
      box('wb_guard_n', [7.5, 2, 4.5], [8.5, 7.5, 5], 'iron'), box('wb_guard_s', [7.5, 2, 11], [8.5, 7.5, 11.5], 'iron'),
      box('wb_guard_tx1', [5, 7.5, 7.5], [7.5, 8, 8.5], 'iron'), box('wb_guard_tx2', [8.5, 7.5, 7.5], [11, 8, 8.5], 'iron'),
      box('wb_guard_tz1', [7.5, 7.5, 5], [8.5, 8, 11], 'iron')],
    rotor: [...disc('wb_cog', 8, 8, 2.25, 2.75, 2.5, 'brass'), box('wb_spindle', [7.75, 2.75, 7.75], [8.25, 5.5, 8.25], 'steel'),
      box('wb_mirror', [8.5, 2.9, 6.75], [8.75, 4.2, 9.25], 'reflector'), glowBox('wb_bulb', [7.1, 3.1, 7.1], [8.4, 4.2, 8.4]),
      // the reflector's beam where it meets the dome, inside the glass: it sweeps round with the rotor (lit only)
      glowBox('wb_flare', [5.85, 2.5, 6.75], [6.1, 4.3, 9.25], 'glow', ['east'])],
  };
}
function indicator() {
  return [box('in_bezel', [6, 6, 0], [10, 10, 1], { all: 'steel', south: 'bezel', north: null }), glowBox('in_lens', [7, 7, 1], [9, 9, 2], 'glow', ['north'])];
}
function lightSwitch() {
  return {
    plate: [box('sw_plate', [5, 4, 0], [11, 12, 1], { all: 'steel', south: 'switch_plate', north: null }), box('sw_boss', [7, 7, 1], [9, 9, 1.5], 'alu'),
      box('sw_gland_b', [7, 3, 0], [9, 4, 1], 'steel'), box('sw_gland_t', [7, 12, 0], [9, 13, 1], 'steel')],
    toggle: [box('sw_toggle', [7.5, 7.5, 1.5], [8.5, 8.5, 4.5], 'steel'), box('sw_tip', [7.5, 7.5, 4.5], [8.5, 8.5, 5], 'brass')],
  };
}
// The Light Switch as a line fitting: the same plate, boss, glands and toggle laid on the floor over a wire running along
// x (line fittings are authored so, and turned onto the wire's face by PowerLineRenderer). The plate stands 1.5 px proud so
// it covers the 1 px wire; the glands are 1.25 px so their tops clear the wire's. The toggle pivots at (8, 2, 8) about z.
function lightSwitchFloor() {
  return {
    plate: [box('sw_plate', [4, 0, 5], [12, 1.5, 11], { all: 'steel', up: 'switch_plate_floor', down: null }), box('sw_boss', [7, 1.5, 7], [9, 2, 9], { all: 'alu', down: null }),
      box('sw_gland_w', [3, 0, 7], [4, 1.25, 9], { all: 'steel', down: null }), box('sw_gland_e', [12, 0, 7], [13, 1.25, 9], { all: 'steel', down: null })],
    toggle: [box('sw_toggle', [7.5, 2, 7.5], [8.5, 5, 8.5], { all: 'steel', down: null }), box('sw_tip', [7.5, 5, 7.5], [8.5, 5.5, 8.5], 'brass')],
  };
}
const wallWire = (y0, y1) => box('wire', [7.5, y0, 0], [8.5, y1, 0.5], { all: 'copper', north: null });

const ANDESITE = 'minecraft:block/polished_andesite';
const isGlow = (c) => c.shade === false && Object.values(c.faces).some((f) => f && f.startsWith('glow'));
// a copy sits a hair inside the first one, so in Edit mode the first (the resting state) shows
const shrink = (cubes, k) => cubes.map((c) => ({ ...c, from: c.from.map((v) => v + k), to: c.to.map((v) => v - k), rot: c.rot }));
// a light whose glow has several looks: states = [[groupSuffix, texture], ...]; the first is the resting look
function litStates(name, list, bx, by, bz, states, pivot = [8, 8, 8]) {
  const solid = list.filter((c) => !isGlow(c)), glow = list.filter(isGlow);
  const kids = states.map(([suffix, tex], i) => group(name + '_' + suffix, pivot, [bx * 16, by * 16, bz * 16], placed(shrink(tex === 'glow' ? glow : reglow(glow, tex), i * 0.01), bx, by, bz)));
  return blockOf(name, solid, bx, by, bz, kids);
}
// a moving part: its own group about a pivot inside the block's group, posed by a rotation
function mover(name, list, pivot, bx, by, bz, rotation) {
  const g = group(name, pivot, [bx * 16, by * 16, bz * 16], placed(list, bx, by, bz));
  if (rotation) g.rotation = rotation;
  return g;
}

if (GROUP === 'indoor') {
  // a wall along x at z 0 (blocks 0-12, 4 high); wall lights in front of it at z 1. A ceiling strip at y 5 over z 4, lamps under it.
  const wallKids = [];
  for (let x = 0; x <= 12; x++) for (let y = 0; y < 4; y++) wallKids.push(stand(`wall_${x}_${y}`, ANDESITE, x, y, 0));
  outliner.push(group('wall', [8, 0, 8], [0, 0, 0], wallKids));
  const wl = [];
  [0, 1, 2].forEach((x) => wl.push(blockOf('bulkhead_' + x, bulkhead(), x, 1, 1)));
  wl.push(blockOf('bulkhead_off', reglow(bulkhead(), 'glass_off'), 3, 1, 1));
  wl.push(blockOf('sconce', sconce(), 5, 1, 1), blockOf('sconce_off', reglow(sconce(), 'glass_off'), 6, 1, 1));
  wl.push(blockOf('gooseneck', gooseneck(), 8, 2, 1), blockOf('gooseneck_off', reglow(gooseneck(), 'glass_off'), 10, 2, 1));
  outliner.push(group('wall_lights', [8, 0, 8], [0, 0, 16], wl));

  const ceilKids = [];
  for (let x = 0; x <= 20; x++) ceilKids.push(stand(`ceil_${x}`, ANDESITE, x, 5, 4));
  outliner.push(group('ceiling', [8, 0, 8], [0, 80, 64], ceilKids));
  const cl = [];
  cl.push(blockOf('highbay_0', highBay(), 0, 4, 4), blockOf('highbay_1', highBay(), 2, 4, 4), litStates('highbay_warm', highBay(), 4, 4, 4, [['sodium', 'glow_sodium'], ['w1', 'glow_w1'], ['w2', 'glow_w2'], ['full', 'glow']]));
  cl.push(blockOf('cage', cageLamp(), 6, 4, 4), blockOf('cage_off', reglow(cageLamp(), 'glass_off'), 7, 4, 4));
  cl.push(blockOf('flush', flush(), 8, 4, 4), blockOf('flush_off', reglow(flush(), 'glass_off'), 9, 4, 4));
  [0, 1, 2].forEach((d) => hanging('pendant', d).forEach((b, i) => cl.push(blockOf(`pendant_${d}_${i}`, b, 11 + d, 4 - i, 4))));
  hanging('chandelier', 1).forEach((b, i) => cl.push(blockOf('chandelier_' + i, b, 15, 4 - i, 4)));
  ['red', 'light_blue', 'lime', 'purple', 'yellow'].forEach((d, i) => cl.push(blockOf('flush_' + d, reglow(flush(), 'glow_' + d), 16 + i, 4, 4)));
  outliner.push(group('ceiling_lights', [8, 0, 8], [0, 64, 64], cl));
}

if (GROUP === 'runs') {
  // a corridor corner: floor blocks z 1-3 along x 0-6, a wall at z 0, and a strip along the floor that turns up the wall
  const floorKids = [];
  for (let x = 0; x <= 14; x++) for (let z = 1; z <= 6; z++) if (!(z === 5 && x >= 8 && x <= 12)) floorKids.push(stand(`floor_${x}_${z}`, ANDESITE, x, 0, z));
  for (let x = 0; x <= 6; x++) for (let y = 1; y <= 3; y++) floorKids.push(stand(`wall_${x}_${y}`, ANDESITE, x, y, 0));
  outliner.push(group('stage', [8, 0, 8], [0, 0, 0], floorKids));
  const rk = [];
  // Light Strip: along the floor toward the wall (x 4, z 5 -> 1), turning up the wall face to the top
  const along = (a, b, ca, cb) => turn(stripFloor(a, b, ca, cb), 'east');
  for (let z = 2; z <= 5; z++) rk.push(blockOf('strip_floor_' + z, along(0, 16, false, z === 5), 4, 1, z));
  rk.push(blockOf('strip_corner', [...along(1, 16, false, false), ...toWall(along(0, 16, false, false))], 4, 1, 1));
  for (let y = 2; y <= 3; y++) rk.push(blockOf('strip_wall_' + y, toWall(along(0, 16, y === 3, false)), 4, y, 1));
  // a second run, dyed: along the floor by the wall's foot
  for (let x = 0; x <= 2; x++) rk.push(blockOf('strip_blue_' + x, reglow(move(stripFloor(0, 16, x === 0, x === 2), [0, 0, -5]), 'glow_light_blue'), x, 1, 1));
  // Edge Light: along the wall's top south edge, sitting on the top face of the wall blocks, turning along the west end
  const edgeS = (a, b) => [box('e_back', [a, 0, 14.5], [b, 2, 16], 'alu'), box('e_lip', [a, 0, 13.5], [b, 0.75, 14.5], 'alu'), glowBox('e_glow', [a, 0.75, 13.75], [b, 2.25, 14.5], 'glow', ['south', 'down'])];
  // on the wall's face (the cell in front of it, against its south face): along the top edge the chamfer faces down the
  // wall, along the bottom edge up it
  const faceTop = () => [box('ef_back', [0, 14.5, 0], [16, 16, 2], 'alu'), box('ef_lip', [0, 13.5, 0], [16, 14.5, 0.75], 'alu'), glowBox('ef_glow', [0, 13.75, 0.75], [16, 14.5, 2.25], 'glow', ['up', 'north'])];
  const faceBottom = () => [box('ef_back', [0, 0, 0], [16, 1.5, 2], 'alu'), box('ef_lip', [0, 1.5, 0], [16, 2.5, 0.75], 'alu'), glowBox('ef_glow', [0, 1.5, 0.75], [16, 2.25, 2.25], 'glow', ['down', 'north'])];
  for (let x = 5; x <= 6; x++) {
    rk.push(blockOf('edge_face_top_' + x, faceTop(), x, 3, 1));
    rk.push(blockOf('edge_face_bottom_' + x, faceBottom(), x, 1, 1));
  }
  for (let x = 0; x <= 6; x++) rk.push(blockOf('edge_' + x, x === 0 ? [...edgeS(2.5, 16), ...edgeZ(0, 16)] : edgeS(0, 16), x, 4, 0));
  outliner.push(group('runs', [8, 0, 8], [0, 0, 0], rk));

  // ground: a path of Bollards and a platform edge of Floor Lights (one dyed)
  const gk = [];
  [8, 10, 12].forEach((x, i) => gk.push(i === 2 ? blockOf('bollard_' + i, reglow(bollard(), 'glass_off'), x, 1, 2) : litStates('bollard_' + i, bollard(), x, 1, 2, [['lit', 'glow'], ['dark', 'glass_off']])));
  [8, 9, 10, 11, 12].forEach((x, i) => gk.push(i === 2 ? litStates('floor_light_' + i, floorLight(), x, 0, 5, [['full', 'glow'], ['b80', 'glow_b80'], ['b50', 'glow_b50'], ['b25', 'glow_b25']], [8, 15, 8]) : blockOf('floor_light_' + i, i === 3 ? reglow(floorLight(), 'glow_cyan') : floorLight(), x, 0, 5)));
  outliner.push(group('ground', [8, 0, 8], [0, 0, 0], gk));

  // specials: a Floodlight on a pillar aimed down and across, a Warning Beacon, a control board with a Switch and Indicators
  const sk = [];
  for (let y = 1; y <= 2; y++) sk.push(stand('pillar_' + y, ANDESITE, 14, y, 1));
  const fd = floodlight();
  const fdHead = mover('floodlight_head', fd.head, [8, 7.5, 8], 14, 3, 1, [-30, 0, 0]);
  const fdYoke = group('floodlight_yoke', [8, 0, 8], [14 * 16, 48, 16], [...placed(fd.yoke, 14, 3, 1), fdHead]);
  fdYoke.rotation = [0, 150, 0];
  sk.push(blockOf('floodlight', fd.base, 14, 3, 1, [fdYoke]));
  const wb = beacon();
  sk.push(blockOf('beacon', wb.base, 14, 1, 5, [mover('beacon_rotor', wb.rotor, [8, 2, 8], 14, 1, 5, [0, 40, 0])]));
  // the board: three wall blocks at x 0-2 on a second wall at z 8 facing north... kept simple: on the main wall, y 1, x 0-2
  const sw = lightSwitch();
  sk.push(blockOf('switch_on', [...sw.plate, wallWire(0, 3)], 1, 1, 1, [mover('switch_on_toggle', sw.toggle, [8, 8, 1.5], 1, 1, 1, [-30, 0, 0])]));
  sk.push(blockOf('switch_off', sw.plate, 2, 1, 1, [mover('switch_off_toggle', sw.toggle, [8, 8, 1.5], 2, 1, 1, [30, 0, 0])]));
  [null, 'red', 'lime', 'light_blue'].forEach((d, i) => sk.push(litStates('indicator_' + i, indicator(), 1 + (i % 2), 2 + (i >> 1), 1, [['lit', d ? 'glow_' + d : 'glow'], ['dark', 'glass_off']])));
  outliner.push(group('specials', [8, 0, 8], [0, 0, 0], sk));

  // Festoon: from a steel lamp post at x 13 z 6 across to a wall hook on the wall block at x 6, y 3
  POS.festoon_post = [13, 4, 1];
  outliner.push(pole('festoon_post', 'steel', 4, null, []));
  const hook = [box('hk_plate', [7, 11, 0], [9, 13, 0.5], { all: 'iron', north: null }), box('hk_eye', [7.5, 11.5, 0.5], [8.5, 12.5, 1.5], 'iron')];
  outliner.push(blockOf('festoon_hook', hook, 6, 3, 1));
  const tie = spanTie(4);
  const a = [6 * 16 + 8, 3 * 16 + 12, 16 + 1.5], b = [13 * 16 + tie[0], 4 * 16 + tie[1], 4 * 16 + tie[2]];
  const L = Math.hypot(b[0] - a[0], b[2] - a[2]), N = 160, sag = L / 12;
  const fs2 = [];
  const at = (t) => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t - sag * 4 * t * (1 - t), a[2] + (b[2] - a[2]) * t];
  const yaw = -Math.atan2(b[2] - a[2], b[0] - a[0]) * 180 / Math.PI, seg = Math.hypot(b[0] - a[0], b[2] - a[2]) / N;
  for (let k = 0; k < N; k++) { const [x, y, z] = at((k + 0.5) / N); fs2.push(C('fs_' + k, [x - seg / 2 - 0.2, y - 0.3, z - 0.3], [x + seg / 2 + 0.2, y + 0.3, z + 0.3], { all: 'flex' }, { rot: ['y', yaw, [x, y, z]] })); }
  const bulbs = Math.floor(L / 32);
  for (let k = 1; k <= bulbs; k++) {
    const [x, y, z] = at(k / (bulbs + 1));
    fs2.push(C('fb_sock_' + k, [x - 0.5, y - 1.5, z - 0.5], [x + 0.5, y - 0.3, z + 0.5], { all: 'steel' }));
    fs2.push(...disc('fb_bulb_' + k, 0, 0, 0, 2.5, 2.5, 'glow', { cube: { shade: false } }).map((c) => ({ ...c, from: [c.from[0] + x, y - 4, c.from[2] + z], to: [c.to[0] + x, y - 1.5, c.to[2] + z] })));
  }
  outliner.push(group('festoon', [0, 0, 0], [0, 0, 0], fs2.map((c) => cube(c, [0, 0, 0]))));
}

// ---------- preview clips, with the approved sounds (design/sound/electric-lights) ----------
const SOUND = path.resolve(__dirname, '../sound/electric-lights');
const CUES = JSON.parse(require('fs').readFileSync(path.join(SOUND, 'electric-lights.cues.json'), 'utf8'));
const cueSounds = (name) => (CUES.clips[name] || []).map(([t, ev]) => {
  const file = path.join(SOUND, 'real', ev + '.ogg');
  if (!require('fs').existsSync(file)) throw new Error('no real sound for ' + ev + ': ' + file);
  return [t, CUES.namespace + '.' + ev, file.split(String.fromCharCode(92)).join('/')];
});
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
// step keys showing exactly one state group at a time: plan = [[time, suffix], ...]
const swap = (name, suffixes, plan) => Object.fromEntries(suffixes.map((sfx) => [name + '_' + sfx, { scale: plan.map(([t, on]) => [t, on === sfx ? SHOW : HIDE]) }]));
const ease = (t, t0, t1) => (t <= t0 ? 0 : t >= t1 ? 1 : ((u) => u * u * (3 - 2 * u))((t - t0) / (t1 - t0)));
if (GROUP === 'indoor') {
  clip('warm_up', 4.5, 'loop', {}, swap('highbay_warm', ['sodium', 'w1', 'w2', 'full'], [[0, 'sodium'], [1.3, 'w1'], [2.6, 'w2'], [4.0, 'full']]), cueSounds('warm_up'));
}
if (GROUP === 'runs') {
  // the switch: off, flipped on at 0.3 s (the Indicators light), off again at 1.4 s; the toggle snaps over 0.08 s with a small overshoot
  const snap = (t, t0) => { if (t <= t0) return 0; const u = (t - t0) / 0.08; return u < 1 ? u * u * (3 - 2 * u) * 1.08 : 1 + 0.08 * Math.exp(-(u - 1) * 2.4) * Math.cos((u - 1) * 3); };
  const flip = (t) => (t < 1.4 ? -60 * snap(t, 0.3) : -60 + 60 * snap(t, 1.4));
  const ind = {};
  for (let i = 0; i < 4; i++) Object.assign(ind, swap('indicator_' + i, ['lit', 'dark'], [[0, 'dark'], [0.3, 'lit'], [1.4, 'dark']]));
  clip('switch', 2.0, 'loop', { switch_off_toggle: { rotation: (t) => [flip(t), 0, 0] } }, ind, cueSounds('switch'));
  clip('beacon', 2.0, 'loop', { beacon_rotor: { rotation: (t) => [0, -t * 360, 0] } }, {}, cueSounds('beacon'));
  clip('floodlight_aim', 1.0, 'loop', { floodlight_yoke: { rotation: (t) => [0, -50 * ease(t, 0.1, 0.6), 0] }, floodlight_head: { rotation: (t) => [15 * ease(t, 0.12, 0.6), 0, 0] } }, {}, cueSounds('floodlight_aim'));
  // the flicker: the renderer's 90 ms frames, then dark
  const fl = [[0, 'dark'], [0.09, 'lit'], [0.2, 'dark'], [0.27, 'lit'], [0.45, 'dark'], [0.62, 'lit'], [0.85, 'dark']];
  clip('flicker_out', 1.5, 'loop', {}, { ...swap('bollard_0', ['lit', 'dark'], fl), ...swap('bollard_1', ['lit', 'dark'], fl) }, cueSounds('flicker_out'));
  clip('brightness', 2.0, 'loop', {}, swap('floor_light_2', ['full', 'b80', 'b50', 'b25'], [[0, 'full'], [0.2, 'b80'], [0.7, 'b50'], [1.2, 'b25'], [1.7, 'full']]), cueSounds('brightness'));
}

if (!LIB) write(OUT, outliner);
module.exports = { T, DYE_GLOW, GLOW, SODIUM_LOW, C, box, disc, zdisc, glowBox, move, reglow, turn, toWall, insulatorStack, isGlow,
  castIronPost, steelPost, arm, armHead, ARM_TIP, lantern, cobraTop, globe, yardTop, bulkhead, sconce, gooseneck, highBay, cageLamp, flush,
  pendantShade, chandelierBody, rose, flexSeg, stripFloor, edgeZ, bollard, floorLight, floodlight, beacon, indicator, lightSwitch, lightSwitchFloor, stripParts, edgeParts, edgeX };
