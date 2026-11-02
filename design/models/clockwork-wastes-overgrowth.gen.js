// Clockwork Wastes: the overgrowth (D91, D92; brief design/briefs/clockwork-wastes-overgrowth.md) -> clockwork-wastes-overgrowth.bbmodel
// Preview only: nothing here goes into the mod. Every plant is written as a block model in the game's model space (MODELS:
// elements with from/to/rotation/faces, 0..16 in its own cell) so the checks below are the ones a real block model needs,
// and drawn into the scene from there. The city's blocks come from the game's own blockstates, models and textures through
// kit-building-preview.js (the terrace and the shed are the kit's dumps in build/kit-preview/).
//   node design/models/clockwork-wastes-overgrowth.gen.js          (writes the .bbmodel, the icon sheet, and prints the checks)
// The scene, block units, every front facing north (-z), left to right along x:
//   z -40: the swatch board (every species and state, the colour chips);
//   z 0:   the wall study (the terrace LIGHT, a void-alloy tower, the terrace HEAVY), the street corner (the shed LIGHT and HEAVY),
//          the grapple with the Crookwood, the Shieldcrown up through a burnt-out shed, both trees as black shapes, the lit check.
const fs = require('fs');
const path = require('path');
const { png, hash } = require('./process-box.lib.js');
const K = require('./kit-building-preview.js');

const REPO = path.resolve(__dirname, '../..');
const OUT = path.join(__dirname, 'clockwork-wastes-overgrowth.bbmodel');
const REVIEW = path.join(REPO, 'design/texture-reviews/clockwork-wastes-overgrowth');
const DUMP = path.join(REPO, 'build/kit-preview');

// ---------------------------------------------------------------- the palette (the brief's Theme table)
const C = {
  leaf: ['#141c2e', '#1c2a45', '#24365a', '#2e4670', '#3b5886'],
  stem: ['#1a1e33', '#252b48', '#323a5e', '#424c75'],
  bark: ['#1b1d3a', '#272c55', '#333a6b', '#424a86'],
  root: ['#171b2e', '#212845', '#2c3558', '#3a4672'],
  cleaf: ['#3e4e68', '#55688a', '#6b7fa0', '#7a8dab'],
  sbark: ['#1f232e', '#2b3140', '#384053', '#4a5469'],
  sleaf: ['#121a36', '#18234a', '#213060', '#2a3d78', '#3a5296'], sveins: '#2e4670',
  moss: ['#141e2b', '#1e2b3d', '#26364c', '#30445e'],
  lichen: ['#3a414c', '#4a5462', '#5d6878', '#6f7b8b', '#7e8898'],
  bell: ['#1a2244', '#2c3f7a', '#3a5296', '#4d66ad'],
  rushTip: '#5a6b85',
  gBlue: ['#2b4180', '#4f6bb5', '#8095d8'], gPlum: ['#3f2848', '#8a4fa0', '#b483c8'],
  cap: ['#1d2238', '#283050', '#36406a'],
};
// the approved colours the overgrowth must stay clear of (visual-language.md), and the city's own
const CLEAR = [
  ['starlight', ['#3a3552', '#7f76a8', '#b8b0d8']], ['cyan glow', ['#4f8f99', '#6fb7c4']], ['frost blue', ['#23404c', '#4a87a4', '#4f8fae', '#7ab8d0']],
  ['void-alloy', ['#16151c', '#1d1c25', '#232130', '#2a2838', '#34304a']], ['patina', ['#4f6b5e', '#5b7a6c']], ['dead vines', ['#36372a', '#4f5236']],
  ['stained End brick', ['#6c6750', '#827d5f', '#8d876a']], ['andesite ceiling', ['#8a8c86']],
];
const PLANT_RAMPS = [['leaf', C.leaf], ['rope stem', C.stem], ['Crookwood bark', C.bark], ['Breakroot', C.root], ['Crookwood leaf', C.cleaf],
  ['Shieldcrown bark', C.sbark], ['Shieldcrown leaf', [...C.sleaf, C.sveins]], ['moss', C.moss], ['lichen', C.lichen], ['Vigil Bell', [...C.bell, C.rushTip]],
  ['Shiftcap blue', C.gBlue], ['Shiftcap plum', C.gPlum], ['Shiftcap cap', C.cap]];

// ---------------------------------------------------------------- textures, painted here
// (the scene needs the game's jars, so it's made only when this runs; the exporter just takes the models and textures)
let scene = null;
const T = {}, PNGS = {}, OWN = {};
const rgba = (h) => (h ? [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255] : [0, 0, 0, 0]);
function grid() {
  const px = Array.from({ length: 16 }, () => Array(16).fill(null));
  return { px, set(x, y, c) { if (x >= 0 && x < 16 && y >= 0 && y < 16) px[y][x] = c; }, get: (x, y) => (x >= 0 && x < 16 && y >= 0 && y < 16 ? px[y][x] : null) };
}
function paint(name, fn) {
  const d = new Uint8Array(16 * 16 * 4);
  let alpha = false;
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const c = fn(x, y); if (!c) alpha = true; d.set(rgba(c), (y * 16 + x) * 4); }
  PNGS[name] = png(16, 16, d);
  T[name] = 'own:og_' + name;
  OWN[T[name]] = { data: PNGS[name], w: 16, h: 16, alpha };
  return T[name];
}
const fromGrid = (name, g) => paint(name, (x, y) => g.px[y][x]);
const pick = (arr, r) => arr[Math.min(arr.length - 1, Math.floor(r * arr.length))];

// the palette sheet: one pixel per colour, for chips and flat parts
const PAL = [];
for (const [, ramp] of [...PLANT_RAMPS, ...CLEAR]) for (const c of ramp) if (!PAL.includes(c)) PAL.push(c);
for (const c of ['#101114', '#0b0b0d', '#2a2b30']) PAL.push(c);
paint('palette', (x, y) => PAL[y * 16 + x] || '#000000');
const palUv = (c) => { const i = PAL.indexOf(c); if (i < 0) throw new Error('not on the palette: ' + c); const x = i % 16, y = Math.floor(i / 16); return [x + 0.25, y + 0.25, x + 0.75, y + 0.75]; };

// a 2 px rope stem: two strands twisting (lit and dark pixel pairs swap every 2 rows)
function rope(g, x, y, ramp = C.stem) {
  const ph = (y >> 1) & 1;
  g.set(x, y, ph ? ramp[3] : ramp[1]);
  g.set(x + 1, y, ph ? ramp[1] : ramp[2]);
}
function polyline(pts, fn) {
  for (let i = 0; i < pts.length - 1; i++) {
    const [ax, ay] = pts[i], [bx, by] = pts[i + 1], n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
    for (let s = 0; s <= n; s++) fn(Math.round(ax + ((bx - ax) * s) / (n || 1)), Math.round(ay + ((by - ay) * s) / (n || 1)));
  }
}
// a lobed leaf off a stem pixel, to the right (s 1) or the left (s -1)
const LEAF = [[1, -2, 4], [2, -2, 3], [0, -1, 3], [1, -1, 4], [2, -1, 3], [3, -1, 2], [0, 0, 2], [1, 0, 1], [2, 0, 3], [3, 0, 2], [4, 0, 1], [1, 1, 1], [2, 1, 2], [3, 1, 1], [2, 2, 0]];
function leaf(g, x, y, s, ramp = C.leaf) { for (const [dx, dy, k] of LEAF) g.set(s > 0 ? x + 2 + dx : x - 1 - dx, y + dy, ramp[k]); }
// a tendril curl ending a stem
function curl(g, x, y, s) { for (const [dx, dy] of [[1, 0], [2, -1], [3, -1], [4, 0], [4, 1], [3, 2], [2, 1]]) g.set(x + s * dx, y + dy, dx === 3 && dy === -1 ? C.stem[3] : C.stem[2]); }

// 1. Knotvine: stems enter and leave at x 4-5 and 11-12 on the top and bottom edges, so cells join into ropes
function knotvine(name, stems, leaves, curls) {
  const g = grid();
  for (const s of stems) polyline(s, (x, y) => rope(g, x, y));
  for (const [x, y, s] of leaves) leaf(g, x, y, s);
  for (const [x, y, s] of curls || []) curl(g, x, y, s);
  return fromGrid(name, g);
}
// (two meandering ropes a cell, never an X, so a wall of them reads as growth and not as wallpaper; a short knot loop on b)
knotvine('knotvine_a', [[[4, 0], [6, 4], [5, 9], [3, 12], [4, 15]], [[11, 0], [12, 3], [10, 8], [11, 12], [11, 15]]], [[6, 3, 1], [4, 8, -1], [4, 13, 1], [12, 2, 1], [10, 7, -1], [11, 12, 1]], [[7, 10, 1]]);
knotvine('knotvine_b', [[[4, 0], [3, 5], [6, 10], [4, 15]], [[11, 0], [13, 6], [12, 11], [11, 15]], [[6, 10], [9, 9], [12, 11]]], [[3, 3, -1], [6, 8, 1], [3, 13, -1], [13, 4, 1], [12, 10, -1], [10, 14, -1]]);
knotvine('knotvine_c', [[[4, 0], [5, 7], [4, 11], [4, 15]], [[11, 0], [9, 5], [11, 10], [11, 15]]], [[5, 5, 1], [3, 10, -1], [4, 14, 1], [9, 3, -1], [11, 8, 1], [12, 13, -1]], [[1, 6, -1]]);
// the floor and ceiling card: a creeping run across the face
knotvine('knotvine_floor', [[[4, 0], [5, 6], [9, 10], [11, 15]]], [[5, 4, 1], [9, 11, -1], [3, 13, -1]], [[10, 6, 1]]);

// 2. Scale Lichen: lobed rosettes, a mottled grey-blue body with dark spore dots, a paler lobed rim (no target rings)
function lichen(name, rs) {
  const g = grid();
  rs.forEach(([cx, cy, r], i) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const dx = x + 0.5 - cx, dy = y + 0.5 - cy, d = Math.hypot(dx, dy), re = r * (0.8 + 0.2 * Math.sin(5 * Math.atan2(dy, dx) + i * 1.7));
      if (d >= re) continue;
      if (d > re - 1.1) g.set(x, y, hash(x, y, 43) > 0.45 ? C.lichen[4] : C.lichen[3]);
      else if (hash(x, y, 44 + i) > 0.86) g.set(x, y, C.lichen[0]);
      else g.set(x, y, d < re * 0.45 ? C.lichen[1] : hash(x, y, 45) > 0.5 ? C.lichen[2] : C.lichen[1]);
    }
  });
  return fromGrid(name, g);
}
lichen('lichen_a', [[5, 5, 4.8], [11.5, 9.5, 4.2], [4, 13.5, 3]]);
lichen('lichen_b', [[10.5, 4.5, 4.6], [4.5, 10, 4.8], [12.5, 13.5, 2.8]]);

// 3. Dusk Moss: cushions (nearest tuft centre), dark gaps between them, a few pale spore stalks
function moss(name, shift, seed) {
  const pts = [];
  for (let j = 0; j < 5; j++) for (let i = 0; i < 5; i++) pts.push([i * 3.2 + 0.8 + hash(i, j, seed) * 1.8, j * 3.2 + 0.8 + hash(i, j, seed + 1) * 1.8]);
  const wrap = (v) => (v > 8 ? v - 16 : v < -8 ? v + 16 : v);
  return paint(name, (x, y) => {
    const ds = pts.map((p) => { const dx = wrap(x + 0.5 - p[0]), dy = wrap(y + 0.5 - p[1]); return { d: Math.hypot(dx, dy), dx, dy }; }).sort((a, b) => a.d - b.d);
    if (ds[1].d - ds[0].d < 0.2) return C.moss[0];
    if (ds[1].d - ds[0].d < 0.45) return C.moss[Math.max(0, 1 - shift)];
    if (hash(x, y, seed + 7) > 0.975) return C.lichen[2];
    // each tuft lit on its upper left, shaded on its lower right
    const s = ds[0].dx + ds[0].dy, k = s < -0.8 ? 3 : s < 0.6 ? 2 : 1;
    return C.moss[Math.max(0, k - shift)];
  });
}
moss('moss_top', 0, 5);
moss('moss_side', 1, 9);
// the drape: a ragged fringe 2-4 px down from the carpet's edge (5 rows: the carpet's 1 px and up to 4 below)
paint('moss_drape', (x, y) => {
  const len = 2 + Math.floor(hash(x, 0, 13) * 3);
  if (y > len) return null;
  return y === 0 ? C.moss[2] : y === len ? C.moss[0] : hash(x, y, 14) > 0.6 ? C.moss[2] : C.moss[1];
});

// 4/5/6/7. crack plants
const COIL = ['.XXX.', 'X...X', 'X.X.X', 'X.XX.', '.X...'];
function coil(g, ox, oy, mirror, ramp = C.leaf) {
  COIL.forEach((row, dy) => [...row].forEach((ch, dx) => { if (ch === 'X') g.set(mirror ? ox + 4 - dx : ox + dx, oy + dy, dx + dy < 4 ? ramp[4] : ramp[3]); }));
}
function frond(g, pts, leaflets) {
  polyline(pts, (x, y) => { g.set(x, y, C.leaf[3]); g.set(x + 1, y, C.leaf[2]); if (leaflets && y % 2 === 0 && y < 14) { g.set(x - 1, y, C.leaf[4]); g.set(x + 2, y, C.leaf[2]); } });
}
// a fiddlehead: a spiral 7 px across, lit on its outer turn, the gap between turns left open; returns where its stalk leaves
function spiral(g, cx, cy, s, ramp = C.leaf, turns = 3.3) {
  let end = null;
  for (let t = 0; t <= Math.PI * turns; t += 0.05) {
    const r = 0.4 + 0.33 * t, x = Math.round(cx + s * r * Math.cos(t)), y = Math.round(cy - r * Math.sin(t));
    g.set(x, y, t > Math.PI * 2 ? ramp[4] : ramp[3]);
    end = [x, y];
  }
  return end;
}
{
  const g = grid();
  frond(g, [[7, 15], [5, 12], [3, 10], [1, 8]], true);
  frond(g, [[8, 15], [10, 12], [12, 10], [14, 8]], true);
  const a = spiral(g, 5, 3.5, 1); polyline([[a[0], a[1]], [6, 9], [7, 15]], (x, y) => { g.set(x, y, C.leaf[3]); g.set(x + 1, y, C.leaf[2]); });
  const b = spiral(g, 10.5, 5, -1); polyline([[b[0], b[1]], [9, 10], [8, 15]], (x, y) => { g.set(x, y, C.leaf[3]); g.set(x + 1, y, C.leaf[2]); });
  fromGrid('springcurl_grown', g);
}
{
  const g = grid();
  const a = spiral(g, 4.5, 8.5, 1); polyline([[a[0], a[1]], [6, 12], [7, 15]], (x, y) => { g.set(x, y, C.leaf[3]); g.set(x + 1, y, C.leaf[2]); });
  const b = spiral(g, 10.5, 9.5, -1); polyline([[b[0], b[1]], [9, 12], [8, 15]], (x, y) => { g.set(x, y, C.leaf[3]); g.set(x + 1, y, C.leaf[2]); });
  fromGrid('springcurl_young', g);
}
{
  const g = grid();
  const stems = [[3, 6], [5, 4], [7, 2], [8, 5], [10, 3], [12, 7]];
  stems.forEach(([bx, top], i) => {
    for (let y = 15; y >= top; y--) {
      const lean = Math.round(((bx - 7.5) * 0.3 * (15 - y)) / 10), off = ((y >> 1) + i) & 1, x = bx + lean + off;
      const tip = y < top + 2;
      g.set(x, y, tip ? C.rushTip : y % 2 ? C.stem[2] : C.stem[1]);
      if (!tip && y % 2 === 0) g.set(x + (off ? -1 : 1), y, C.stem[3]);
    }
  });
  fromGrid('rush', g);
}
{
  const g = grid();
  for (let y = 7; y <= 15; y++) { g.set(7, y, C.stem[2]); g.set(8, y, y % 2 ? C.stem[1] : C.stem[3]); }
  for (const [x, y] of [[8, 6], [9, 5], [10, 5], [11, 5], [12, 6], [12, 7]]) g.set(x, y, C.stem[2]);
  // the big bell under the crook
  for (const [x, y, k] of [[11, 8, 2], [12, 8, 3], [13, 8, 1], [10, 9, 1], [11, 9, 2], [12, 9, 3], [13, 9, 2], [14, 9, 1], [10, 10, 3], [11, 10, 0], [12, 10, 0], [13, 10, 0], [14, 10, 3], [10, 11, 2], [14, 11, 2]]) g.set(x, y, C.bell[k]);
  // a bud on a short shoot (a bell to come)
  for (const [x, y] of [[6, 11], [5, 10]]) g.set(x, y, C.stem[2]);
  for (const [x, y, k] of [[4, 10, 2], [4, 11, 1], [5, 11, 3]]) g.set(x, y, C.bell[k]);
  // the leaf rosette at the foot
  for (const [x, y, k] of [[5, 14, 3], [6, 14, 4], [9, 14, 4], [10, 14, 3], [4, 15, 2], [5, 15, 3], [10, 15, 3], [11, 15, 2]]) g.set(x, y, C.leaf[k]);
  fromGrid('vigil_bell', g);
}

// 8. Bramblecoil: a small leaf core, and sprigs of thorny stems that end in coils
{
  // lobed leaves stamped over a dark tangle, a few gaps to the coils inside, pale thorn pixels
  const g = grid();
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if (hash(x, y, 51) > 0.09) g.set(x, y, hash(x, y, 53) > 0.8 ? C.leaf[1] : C.leaf[0]);
  for (let j = 0; j < 3; j++) for (let i = 0; i < 3; i++) leaf(g, i * 5 + (j % 2) * 2 + Math.floor(hash(i, j, 54) * 2) - 1, j * 5 + 2, hash(i, j, 56) > 0.5 ? 1 : -1);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if (hash(x, y, 52) > 0.965) g.set(x, y, C.lichen[3]);
  fromGrid('bramble_leaf', g);
}
function sprig(name, arms, leaves) {
  const g = grid(), SR = [null, null, null, C.stem[3], C.leaf[4]];
  for (const [pts, cx, cy, sd] of arms) {
    polyline(pts, (x, y) => { g.set(x, y, C.stem[2]); g.set(x + 1, y, C.stem[1]); if ((x * 3 + y) % 4 === 0) g.set(x - 1, y, C.lichen[3]); });
    spiral(g, cx, cy, sd, SR, 2.2);
  }
  for (const [x, y, sd] of leaves) leaf(g, x, y, sd);
  return fromGrid(name, g);
}
sprig('bramble_sprig', [[[[7, 15], [4, 11], [2, 7]], 2.5, 4, 1], [[[8, 15], [11, 10], [13, 6]], 12.5, 3.5, -1], [[[7, 15], [6, 9], [5, 4]], 5.5, 2, 1], [[[8, 15], [9, 8], [10, 3]], 10.5, 1.5, -1]],
  [[3, 12, -1], [11, 12, 1], [5, 8, -1], [9, 7, 1], [2, 10, 1], [12, 9, -1], [6, 13, 1]]);
sprig('bramble_sprig_b', [[[[8, 15], [5, 12], [3, 9]], 2.5, 6.5, 1], [[[7, 15], [10, 11], [12, 8]], 12.5, 6, -1], [[[8, 15], [7, 8], [8, 3]], 8.5, 2, -1]],
  [[4, 13, -1], [10, 13, 1], [6, 6, -1], [9, 10, 1], [11, 7, -1], [3, 11, 1]]);

// 9. Shiftcap: dark caps, the glow underneath (gills) in the blue and the plum ramps, never the violet between
paint('cap_top', (x, y) => (hash(x, y, 61) > 0.85 ? C.cap[2] : hash(x, y, 62) > 0.5 ? C.cap[1] : C.cap[0]));
// (growth lines that wander, never even stripes: straight lit bands read as the uprising's seams)
paint('cap_side', (x, y) => (y === 1 + (hash(x >> 2, 0, 66) > 0.5 ? 1 : 0) && hash(x, y, 67) > 0.25 ? C.cap[2] : C.cap[hash(x, y, 63) > 0.6 ? 1 : 0]));
paint('stalk', (x, y) => (hash(x >> 1, y, 64) > 0.55 ? C.stem[3] : C.stem[2]));
function gills(name, inner, outer, innerR) {
  return paint(name, (x, y) => {
    const dx = x + 0.5 - 8, dy = y + 0.5 - 8, d = Math.hypot(dx, dy), ramp = d < innerR ? inner : outer;
    if (d < 1.1) return ramp[2];
    return Math.floor(((Math.atan2(dy, dx) + Math.PI) * 6) / Math.PI) % 2 ? ramp[1] : ramp[0];
  });
}
gills('gill_young', C.gBlue, C.gBlue, 99);
gills('gill_mature', C.gBlue, C.gPlum, 2.2);
gills('gill_old', C.gPlum, C.gPlum, 99);

// 10. Breakroot: root bark with spiral grooves; a cut end in rings; root hairs
paint('root_bark', (x, y) => { const u = (x + y) % 6; return u === 0 ? C.root[0] : u === 1 ? C.root[3] : hash(x, y >> 1, 71) > 0.65 ? C.root[2] : C.root[1]; });
paint('root_end', (x, y) => { const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8); return d < 1.2 ? C.root[0] : Math.floor(d) % 2 ? C.root[2] : C.root[1]; });
{
  const g = grid();
  for (const [x0, len, s] of [[2, 5, 1], [7, 4, -1], [11, 5, 1]]) for (let i = 0; i < len; i++) g.set(x0 + Math.round((i * s) / 2), 15 - i, i === len - 1 ? C.root[3] : C.root[2]);
  fromGrid('root_hairs', g);
}

// 11. Crookwood: indigo bark with spiral grooves (period 8, so faces and stacked logs meet), rings with a pale heart ring
paint('crook_log', (x, y) => { const u = (x + y) % 8; return u === 0 ? C.bark[0] : u === 1 ? C.bark[3] : hash(x, y >> 2, 81) > 0.7 ? C.bark[2] : C.bark[1]; });
paint('crook_log_top', (x, y) => { const d = Math.hypot(x + 0.5 - 8, y + 0.5 - 8); return d > 6.5 ? C.bark[0] : d < 1 ? C.bark[0] : d < 2 ? C.bark[3] : Math.floor(d) % 2 ? C.bark[2] : C.bark[1]; });
const crookLeaf = (x, y) => {
  if (hash(x, y, 83) < 0.2) return null;
  const lx = (x + (Math.floor(y / 4) % 2) * 2) % 4, ly = y % 4, l = lx + ly;
  return l <= 1 ? C.cleaf[3] : l <= 3 ? C.cleaf[2] : l <= 5 ? C.cleaf[1] : C.cleaf[0];
};
paint('crook_leaves', crookLeaf);
// the willow's hanging state: strands of small pale leaves on crossed cards (see-through), a full-length strand texture for the
// middle of a curtain and one whose strands end at different lengths for its foot
const STRANDS = [1, 4, 7, 10, 13];
function hangStrands(name, end) {
  const g = grid();
  STRANDS.forEach((sx, i) => {
    const len = end ? 5 + Math.floor(hash(sx, i, 85) * 10) : 16;
    for (let y = 0; y < len; y++) {
      g.set(sx, y, C.cleaf[0]);
      if ((y + i) % 3 === 0) { g.set(sx - 1, y, C.cleaf[2]); g.set(sx + 1, y + 1, C.cleaf[1]); }
      if ((y + i) % 3 === 1) g.set(sx + 1, y, C.cleaf[3]);
    }
    if (end) { g.set(sx, len, C.cleaf[2]); g.set(sx, len + 1, C.cleaf[3]); }
  });
  return fromGrid(name, g);
}
hangStrands('crook_hang', false);
hangStrands('crook_hang_end', true);

// 12. Shieldcrown: a braid of three strands (diagonal bands that turn at the middle, so the crossings read), deep blue leaves
paint('shield_log', (x, y) => {
  const u = y < 8 ? x + y : x - y + 32, w = u % 5, r = y % 8, mid = r >= 2 && r <= 5;
  if (w === 4 || r === 0) return C.sbark[0];
  const k = w === 0 ? (mid ? 3 : 2) : w === 1 ? (mid ? 2 : 1) : r === 7 || r === 1 ? 0 : 1;
  return C.sbark[k];
});
paint('shield_log_top', (x, y) => {
  for (const [cx, cy] of [[5, 5.5], [11, 6], [8, 11]]) { const d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy); if (d < 3.4) return d < 1 ? C.sbark[1] : Math.floor(d) % 2 ? C.sbark[2] : C.sbark[1]; }
  return C.sbark[0];
});
const shieldLeaf = (x, y) => {
  if (hash(x, y, 91) < 0.12) return null;
  const lx = x % 3, ly = (y + (Math.floor(x / 3) % 2)) % 3;
  if (lx === 1 && ly === 1) return C.sveins;
  return lx + ly === 0 ? C.sleaf[3] : lx + ly <= 2 ? C.sleaf[2] : C.sleaf[1];
};
paint('shield_leaves', shieldLeaf);
paint('shield_leaves_rim', (x, y) => (y < 2 ? (hash(x, y, 92) < 0.08 ? null : hash(x, y, 93) > 0.35 ? C.sleaf[4] : C.sleaf[3]) : shieldLeaf(x, y)));
// the underside: the same small leaves, a step darker (no speckle: it read as a starry sky)
paint('shield_under', (x, y) => { const c = shieldLeaf(x, y); return c === null ? null : c === C.sveins ? C.sleaf[2] : c === C.sleaf[3] ? C.sleaf[2] : c === C.sleaf[2] ? C.sleaf[1] : C.sleaf[0]; });

// 2. Twinerope: the strands' rope (a lit strand and a dark one, so they read as twisted round each other), its leaf card, the hook
paint('rope', (x, y) => { const u = (x + y) % 4; return u === 0 ? C.stem[3] : u === 1 ? C.stem[2] : C.stem[1]; });
paint('rope_dark', (x, y) => { const u = (x + y) % 4; return u === 0 ? C.stem[2] : u === 1 ? C.stem[1] : C.stem[0]; });
{
  const g = grid();
  for (const [x, y, k] of [[1, 0, 4], [2, 0, 3], [0, 1, 3], [1, 1, 3], [2, 1, 2], [3, 1, 2], [1, 2, 1], [2, 2, 2], [3, 2, 1]]) g.set(x, y, C.leaf[k]);
  fromGrid('twine_leaf', g);
}
{
  const g = grid();
  coil(g, 0, 0, false, [null, null, null, C.stem[2], C.stem[3]]);
  fromGrid('twine_hook', g);
}

// item sprites: Twinerope (two strands twisting, leaves, the hook), Shiftcap (two caps, their lit lips), Shiftcap Shelf (brackets)
{
  const g = grid();
  for (let y = 0; y < 13; y++) {
    const w = Math.sin((y * Math.PI) / 4) * 1.5;
    g.set(Math.round(7.5 + w), y, C.stem[3]); g.set(Math.round(7.5 + w) + 1, y, C.stem[2]);
    g.set(Math.round(7.5 - w), y, C.stem[1]); g.set(Math.round(7.5 - w) + 1, y, C.stem[0]);
  }
  leaf(g, 8, 3, 1); leaf(g, 6, 8, -1);
  coil(g, 5, 11, false, [null, null, null, C.stem[2], C.stem[3]]);
  g.set(10, 12, C.lichen[3]); g.set(10, 13, C.lichen[2]); g.set(11, 13, C.lichen[2]); g.set(10, 14, C.lichen[1]);
  fromGrid('item_twinerope', g);
}
{
  const g = grid();
  const cap = (x0, x1, y0, rows, lip) => {
    for (let r = 0; r < rows; r++) for (let x = x0 + (rows - 1 - r); x <= x1 - (rows - 1 - r); x++) g.set(x, y0 + r, r === 0 ? C.cap[2] : (x + r) % 3 ? C.cap[1] : C.cap[0]);
    for (let x = x0; x <= x1; x++) g.set(x, y0 + rows, x === x0 + 1 ? lip[2] : lip[1]);
  };
  for (let y = 6; y < 16; y++) { g.set(y < 11 ? 8 : 7, y, C.stem[3]); g.set(y < 11 ? 9 : 8, y, C.stem[2]); }
  cap(3, 13, 2, 4, C.gBlue);
  for (let y = 11; y < 16; y++) g.set(3, y, C.stem[3]);
  cap(0, 6, 8, 2, C.gPlum);
  fromGrid('item_shiftcap', g);
  // the potted Shiftcap: the sprite less its lit lips, and the lips alone for its full-bright card (as the caps' lips glow)
  const lit = new Set([...C.gBlue, ...C.gPlum]);
  paint('potted_shiftcap', (x, y) => (lit.has(g.px[y][x]) ? null : g.px[y][x]));
  paint('potted_shiftcap_glow', (x, y) => (lit.has(g.px[y][x]) ? g.px[y][x] : null));
}
{
  const g = grid();
  const br = (x0, x1, y, lip) => {
    for (let x = x0 + 2; x <= x1 - 2; x++) g.set(x, y, C.cap[2]);
    for (let x = x0; x <= x1; x++) g.set(x, y + 1, (x + y) % 3 ? C.cap[1] : C.cap[0]);
    for (let x = x0 + 1; x <= x1 - 1; x++) g.set(x, y + 2, x % 2 ? lip[1] : C.cap[0]);
  };
  br(2, 13, 2, C.gPlum); br(5, 15, 7, C.gBlue); br(0, 9, 11, C.gPlum);
  fromGrid('item_shelf', g);
}

// the scorched End Stone chunks borrow the mod's own texture
T.scorched = 'comedymonkeysaddons:block/wastes/scorched';

// ---------------------------------------------------------------- the block models (game model space)
// faces: { dir: { tex, uv, rotation } } with tex a key of T, or { pal: '#hex' } for a flat palette colour
const DIRS = K.DIRS;
function autoUv(dir, f, t) {
  // the game's default UV for the face, wrapped into 0..16 (parts outside the cell sample their own cell)
  const U = { north: [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]], south: [f[0], 16 - t[1], t[0], 16 - f[1]], west: [f[2], 16 - t[1], t[2], 16 - f[1]],
    east: [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]], up: [f[0], f[2], t[0], t[2]], down: [f[0], 16 - t[2], t[0], 16 - f[2]] }[dir];
  const wrap = (a, b) => { const w = b - a; if (w > 16) return [0, 16]; let s = ((a % 16) + 16) % 16; if (s + w > 16) s = 16 - w; return [s, s + w]; };
  const [u0, u1] = wrap(U[0], U[2]), [v0, v1] = wrap(U[1], U[3]);
  return [u0, v0, u1, v1].map((v) => Math.round(v * 1000) / 1000);
}
function el(name, from, to, tex, o = {}) {
  const dirs = o.dirs || DIRS, faces = {};
  for (const d of dirs) {
    const tx = (o.tex && o.tex[d]) || tex;
    if (!tx) continue;
    const pal = typeof tx === 'object' && tx.pal;
    faces[d] = { texture: pal ? T.palette : T[tx], uv: pal ? palUv(tx.pal) : (o.uv && o.uv[d]) || autoUv(d, from, to), ...(o.rot && o.rot[d] ? { rotation: o.rot[d] } : {}) };
    if (!pal && !T[tx]) throw new Error('no texture ' + tx);
  }
  return { name, from, to, faces, rotation: o.r ? { origin: o.origin || [8, 8, 8], axis: o.r[0], angle: o.r[1] } : null, shade: o.shade !== false };
}
const pal = (c) => ({ pal: c });
// a cross of two cards, as vanilla's cross model (no rescale here)
function cross(tex, h = 16, inset = 0.8) {
  const uv = [0, 16 - h, 16, 16];
  return [el(tex + ' card a', [inset, 0, 8], [16 - inset, h, 8], tex, { dirs: ['north', 'south'], uv: { north: uv, south: uv }, r: ['y', 45] }),
    el(tex + ' card b', [8, 0, inset], [8, h, 16 - inset], tex, { dirs: ['east', 'west'], uv: { east: uv, west: uv }, r: ['y', 45] })];
}

const MODELS = {};
// multi-face covers, drawn on the face toward the block to the SOUTH (the turn puts them on the other walls)
MODELS.knotvine_a = [el('knotvine', [0, 0, 15.2], [16, 16, 15.2], 'knotvine_a', { dirs: ['north'], uv: { north: [0, 0, 16, 16] } })];
MODELS.knotvine_b = [el('knotvine', [0, 0, 15.2], [16, 16, 15.2], 'knotvine_b', { dirs: ['north'], uv: { north: [0, 0, 16, 16] } })];
MODELS.knotvine_c = [el('knotvine', [0, 0, 15.2], [16, 16, 15.2], 'knotvine_c', { dirs: ['north'], uv: { north: [0, 0, 16, 16] } })];
MODELS.knotvine_floor = [el('knotvine floor', [0, 0.8, 0], [16, 0.8, 16], 'knotvine_floor', { dirs: ['up'], uv: { up: [0, 0, 16, 16] } })];
MODELS.lichen_a = [el('lichen', [0, 0, 15.75], [16, 16, 15.75], 'lichen_a', { dirs: ['north'], uv: { north: [0, 0, 16, 16] } })];
MODELS.lichen_b = [el('lichen', [0, 0, 15.75], [16, 16, 15.75], 'lichen_b', { dirs: ['north'], uv: { north: [0, 0, 16, 16] } })];
MODELS.lichen_floor = [el('lichen floor', [0, 0.25, 0], [16, 0.25, 16], 'lichen_a', { dirs: ['up'], uv: { up: [0, 0, 16, 16] } })];

// Twinerope: two 2 x 2 strands winding round each other, one turn a block, leaf cards at two nodes
function strands(ks) {
  const out = [];
  for (const k of ks) {
    const a = (k * Math.PI) / 2, cx = Math.round(Math.cos(a) * 1.5 * 2) / 2, cz = Math.round(Math.sin(a) * 1.5 * 2) / 2;
    for (const [s, sx, sz] of [['a', cx, cz], ['b', -cx, -cz]]) {
      const f = [8 + sx - 1, 4 * k, 8 + sz - 1], t = [8 + sx + 1, 4 * k + 4, 8 + sz + 1];
      out.push(el(`strand ${s}${k}`, f, t, s === 'a' ? 'rope' : 'rope_dark', { dirs: ['north', 'south', 'east', 'west'], uv: Object.fromEntries(['north', 'south', 'east', 'west'].map((d, i) => [d, [((k * 4 + i * 2) % 14), (s === 'a' ? 0 : 6) + k, ((k * 4 + i * 2) % 14) + 2, (s === 'a' ? 0 : 6) + k + 4]])) }));
    }
  }
  return out;
}
const twineLeaf = (name, f, t, ang) => el(name, f, t, 'twine_leaf', { dirs: ['north', 'south'], uv: { north: [0, 0, 4, 3], south: [4, 0, 0, 3] }, r: ['y', ang] });
MODELS.twinerope = [...strands([0, 1, 2, 3]), twineLeaf('leaf', [10, 5, 8], [14, 8, 8], 45), twineLeaf('leaf', [2, 12, 8], [6, 15, 8], -45)];
MODELS.twinerope_tip = [...strands([2, 3]),
  el('hook card a', [4, 2.5, 8], [9, 7.5, 8], 'twine_hook', { dirs: ['north', 'south'], uv: { north: [0, 0, 5, 5], south: [5, 0, 0, 5] } }),
  el('hook card b', [6.5, 2.5, 5.5], [6.5, 7.5, 10.5], 'twine_hook', { dirs: ['east', 'west'], uv: { east: [0, 0, 5, 5], west: [5, 0, 0, 5] } }),
  el('seed pod', [8.5, 5, 7], [10.5, 8, 9], pal(C.lichen[2]), { tex: { up: pal(C.lichen[3]), down: pal(C.lichen[1]) } }),
  el('seed pod tip', [9, 4, 7.5], [10, 5, 8.5], pal(C.lichen[1]), { dirs: ['north', 'south', 'east', 'west', 'down'] }),
];

// Dusk Moss: the block, the carpet, the carpet's drape over an open side (hangs over the block below, 0.25 px off it)
MODELS.dusk_moss = [el('moss block', [0, 0, 0], [16, 16, 16], 'moss_side', { tex: { up: 'moss_top', down: 'moss_side' } })];
MODELS.dusk_moss_carpet = [el('carpet', [0, 0, 0], [16, 1, 16], 'moss_side', { dirs: ['north', 'south', 'east', 'west', 'up'], tex: { up: 'moss_top' },
  uv: { north: [0, 0, 16, 1], south: [0, 0, 16, 1], east: [0, 0, 16, 1], west: [0, 0, 16, 1] } })];
MODELS.dusk_moss_drape = [el('drape', [0, -4, -0.25], [16, 1, -0.25], 'moss_drape', { dirs: ['north'], uv: { north: [0, 0, 16, 5] } })];

// crack plants (cross models; the game adds the random XZ offset)
MODELS.springcurl_young = cross('springcurl_young', 10);
MODELS.springcurl_grown = cross('springcurl_grown', 14);
MODELS.corkscrew_rush = cross('rush', 14);
MODELS.vigil_bell = cross('vigil_bell', 12);

// Bramblecoil: crossed sprig cards (two diagonal, two straight) round a small leaf core, so the outline is ragged and the coils show
const SIDES = ['north', 'south', 'east', 'west'];
function bramble(h, core) {
  const uv = { north: [0, 0, 16, 16], south: [0, 0, 16, 16], east: [0, 0, 16, 16], west: [0, 0, 16, 16] }, w = h;
  return [
    el('sprig diagonal a', [8 - w / 2, 0, 8], [8 + w / 2, h, 8], 'bramble_sprig', { dirs: ['north', 'south'], uv, r: ['y', 45] }),
    el('sprig diagonal b', [8, 0, 8 - w / 2], [8, h, 8 + w / 2], 'bramble_sprig', { dirs: ['east', 'west'], uv, r: ['y', 45] }),
    el('sprig straight a', [8 - w / 2, 0, 5], [8 + w / 2, h * 0.85, 5], 'bramble_sprig_b', { dirs: ['north', 'south'], uv }),
    el('sprig straight b', [11, 0, 8 - w / 2], [11, h * 0.85, 8 + w / 2], 'bramble_sprig_b', { dirs: ['east', 'west'], uv }),
    el('leaf core', [8 - core / 2, 0, 8 - core / 2], [8 + core / 2, core * 0.7, 8 + core / 2], 'bramble_leaf', { dirs: [...SIDES, 'up'] }),
  ];
}
MODELS.bramblecoil_small = bramble(12, 8);
MODELS.bramblecoil_large = bramble(16, 11);

// Shiftcap: three mushrooms on crooked stalks; gills 0.25 px under each cap, unshaded (the renderer's glow part)
function mushroom(id, x, z, h, w, gill, spots) {
  const s = [], half = w / 2;
  s.push(el(`${id} stalk lower`, [x - 1, 0, z - 1], [x + 1, h / 2, z + 1], 'stalk', { dirs: ['north', 'south', 'east', 'west'] }));
  s.push(el(`${id} stalk upper`, [x, h / 2, z - 1], [x + 2, h, z + 1], 'stalk', { dirs: ['north', 'south', 'east', 'west'] }));
  const cx = x + 1, cap0 = [cx - half, h, z - half], cap1 = [cx + half, h + 2, z + half];
  s.push(el(`${id} cap`, cap0, cap1, 'cap_side', { tex: { up: 'cap_top', down: 'cap_top' }, uv: { up: [0, 0, w, w], down: [0, 0, w, w] } }));
  s.push(el(`${id} cap crown`, [cx - half + 1, h + 2, z - half + 1], [cx + half - 1, h + 3, z + half - 1], 'cap_side', { dirs: ['north', 'south', 'east', 'west', 'up'], tex: { up: 'cap_top' } }));
  const gi = w - 1, gu = [8 - gi / 2, 8 - gi / 2, 8 + gi / 2, 8 + gi / 2];
  s.push(el(`${id} gills glow`, [cx - gi / 2, h - 0.25, z - gi / 2], [cx + gi / 2, h - 0.25, z + gi / 2], gill, { dirs: ['down'], uv: { down: gu }, shade: false }));
  // the lit lip: the gills' glow showing at the cap's lower edge, 0.25 px proud of each side (so it reads from above too)
  const lip = spots.lip;
  for (const [d, f, t] of [['north', [cx - half, h, z - half - 0.25], [cx + half, h + 0.75, z - half - 0.25]], ['south', [cx - half, h, z + half + 0.25], [cx + half, h + 0.75, z + half + 0.25]],
    ['west', [cx - half - 0.25, h, z - half], [cx - half - 0.25, h + 0.75, z + half]], ['east', [cx + half + 0.25, h, z - half], [cx + half + 0.25, h + 0.75, z + half]]])
    s.push(el(`${id} lip ${d} glow`, f, t, pal(lip), { dirs: [d], shade: false }));
  // glowing spots on the cap's crown, 0.25 px proud (single core-tone pixels: one thing per cap brightest)
  (spots.top || []).forEach(([dx, dz, c], i) => s.push(el(`${id} spot ${i} glow`, [cx + dx, h + 3, z + dz], [cx + dx + 1, h + 3.25, z + dz + 1], pal(c), { dirs: ['up', 'north', 'south', 'east', 'west'], shade: false })));
  return s;
}
// the shift by age: young all blue; mature blue gills, a plum lip and plum spots; old plum gills and lip, blue only at the stalk top
const AGE = {
  young: { lip: C.gBlue[1], top: [[-2, -2, C.gBlue[2]]] },
  mature: { lip: C.gPlum[1], top: [[-2, -2, C.gPlum[2]], [1, 0, C.gBlue[1]]] },
  old: { lip: C.gPlum[1], top: [[-2, -2, C.gPlum[2]], [1, -1, C.gPlum[1]]] },
};
function shiftcap(age) {
  const gill = 'gill_' + age, a = AGE[age];
  const parts = [...mushroom('big', 6, 7, 8, 6, gill, a), ...mushroom('mid', 11, 11, 5, 4, gill, { lip: a.lip, top: a.top.slice(0, 1).map(([, , c]) => [-1, -1, c]) }), ...mushroom('small', 3, 12, 3, 3, gill, { lip: a.lip })];
  if (age === 'old') parts.push(el('big stalk top glow', [5.75, 7, 5.75], [8.25, 7.5, 8.25], pal(C.gBlue[1]), { dirs: ['north', 'south', 'east', 'west'], shade: false }));
  return parts;
}
for (const age of ['young', 'mature', 'old']) MODELS['shiftcap_' + age] = shiftcap(age);
// the shelf: bracket fungi on the face toward the block to the south
function shelf(age) {
  const gill = 'gill_' + age, out = [];
  // each bracket a stepped half-disc in plan (a deep back tier, a shallower front tier) with a lens profile from the front
  [[3, 10, 13, 6], [7, 5, 14, 5], [2, 1, 9, 5]].forEach(([x0, y0, x1, d], i) => {
    const w = x1 - x0, zf = 16 - d, zb = zf + 2;
    const T3 = { dirs: ['north', 'east', 'west', 'up', 'down'], tex: { up: 'cap_top', down: 'cap_top' } };
    out.push(el(`shelf ${i} back`, [x0, y0, zb], [x1, y0 + 1.5, 16], 'cap_side', T3));
    out.push(el(`shelf ${i} back top`, [x0 + 1.5, y0 + 1.5, zb + 1], [x1 - 1.5, y0 + 2.5, 16], 'cap_side', { dirs: ['north', 'east', 'west', 'up'], tex: { up: 'cap_top' } }));
    out.push(el(`shelf ${i} front`, [x0 + 1.5, y0, zf], [x1 - 1.5, y0 + 1, zb], 'cap_side', T3));
    const gw = w - 4, gl = d - 0.5;
    out.push(el(`shelf ${i} gills glow`, [x0 + 2, y0 - 0.25, zf + 0.5], [x1 - 2, y0 - 0.25, 16], gill, { dirs: ['down'], uv: { down: [8 - gw / 2, 8 - gl / 2, 8 + gw / 2, 8 + gl / 2] }, shade: false }));
    // the lip: lit dots along the front tier's edge, 0.25 px proud
    for (let x = x0 + 2; x + 1 <= x1 - 2; x += 2.5) out.push(el(`shelf ${i} lip dot glow`, [x, y0, zf - 0.25], [x + 1, y0 + 0.75, zf - 0.25], pal(AGE[age].lip), { dirs: ['north'], shade: false }));
  });
  return out;
}
for (const age of ['young', 'mature', 'old']) MODELS['shiftcap_shelf_' + age] = shelf(age);

// Breakroot: the knuckle (an arch out of the ground and back), the run (along the ground), the burst (three roots out of a crack)
const RT = { tex: { north: 'root_bark', south: 'root_bark', east: 'root_bark', west: 'root_bark', up: 'root_bark', down: 'root_bark' } };
// (a low arch: two legs leaning in out of the ground under a short crown, 4 px thick, 8 px high; never an upright slab)
MODELS.breakroot_knuckle = [
  el('leg west', [1, -2, 6], [5, 6, 10], 'root_bark', { dirs: ['north', 'south', 'east', 'west'], r: ['z', -22.5], origin: [3, 0, 8] }),
  el('crown', [4.5, 4, 6], [11.5, 8, 10], 'root_bark', { dirs: ['north', 'south', 'up', 'down', 'east', 'west'] }),
  el('leg east', [11, -2, 6], [15, 6, 10], 'root_bark', { dirs: ['north', 'south', 'east', 'west'], r: ['z', 22.5], origin: [13, 0, 8] }),
  el('hairs west', [0, 0, 8], [4, 3, 8], 'root_hairs', { dirs: ['north', 'south'], uv: { north: [0, 13, 4, 16], south: [4, 13, 0, 16] } }),
  el('hairs east', [12, 0, 8], [16, 3, 8], 'root_hairs', { dirs: ['north', 'south'], uv: { north: [10, 13, 14, 16], south: [14, 13, 10, 16] } }),
];
MODELS.breakroot_run = [
  el('run west', [0, 0, 6], [6, 4, 10], 'root_bark', { dirs: ['north', 'south', 'up', 'west'], tex: { west: 'root_end' } }),
  el('run kink', [6, 0, 5], [11, 4.5, 9.5], 'root_bark', { dirs: ['north', 'south', 'up', 'east', 'west'] }),
  el('run east', [11, 0, 6], [16, 4, 10], 'root_bark', { dirs: ['north', 'south', 'up', 'east'], tex: { east: 'root_end' } }),
];
MODELS.breakroot_burst = [
  el('root tall', [6, 0, 6], [10, 15, 10], 'root_bark', { dirs: ['north', 'south', 'east', 'west', 'up'], r: ['x', -22.5], origin: [8, 0, 8], tex: { up: 'root_end' } }),
  el('root tall tip', [6.5, 12, 0.5], [9.5, 16, 3.5], 'root_bark', { dirs: ['north', 'south', 'east', 'west', 'up', 'down'], r: ['x', 45], origin: [8, 14, 2] }),
  el('root west', [0, 0, 6.5], [8, 4, 9.5], 'root_bark', { dirs: ['north', 'south', 'west', 'up'], r: ['z', -22.5], origin: [8, 0, 8], tex: { west: 'root_end' } }),
  el('root east', [9, 0, 9], [16, 3.5, 12], 'root_bark', { dirs: ['north', 'south', 'east', 'up'], r: ['y', 22.5], origin: [9, 0, 10], tex: { east: 'root_end' } }),
  el('stone chunk a', [1, 0, 1], [4, 2, 4], 'scorched', { dirs: ['north', 'south', 'east', 'west', 'up'], r: ['y', 22.5], origin: [2.5, 0, 2.5] }),
  el('stone chunk b', [12, 0, 2], [15, 3, 5], 'scorched', { dirs: ['north', 'south', 'east', 'west', 'up'] }),
  el('stone chunk c', [2, 0, 12], [5, 2, 15], 'scorched', { dirs: ['north', 'south', 'east', 'west', 'up'], r: ['y', -22.5], origin: [3.5, 0, 13.5] }),
];
// the sideways burst (D94): roots out of a wall's foot (the wall to the south), one lifting out toward the street, one climbing
// the wall's face and curling over, one splaying along the wall's foot; the stone they threw out lying in front
MODELS.breakroot_burst_wall = [
  el('root out', [5.5, 0, 6], [9.5, 4, 16], 'root_bark', { dirs: ['north', 'east', 'west', 'up', 'down'], r: ['x', -22.5], origin: [7.5, 0, 16], tex: { north: 'root_end' } }),
  el('root climbing', [10.5, 1, 12], [14, 13, 15.5], 'root_bark', { dirs: ['north', 'east', 'west', 'up'], r: ['z', 22.5], origin: [12, 1, 14] }),
  el('root climbing tip', [6.5, 11, 11.5], [9.5, 14, 14.5], 'root_bark', { dirs: ['north', 'south', 'east', 'west', 'up', 'down'], r: ['z', -45], origin: [8, 12.5, 13] }),
  el('root along', [0, 0, 11.5], [6, 3, 15.5], 'root_bark', { dirs: ['north', 'west', 'up'], r: ['y', -22.5], origin: [6, 0, 15.5], tex: { west: 'root_end' } }),
  el('stone chunk a', [2, 0, 3], [5, 2, 6], 'scorched', { dirs: ['north', 'south', 'east', 'west', 'up'], r: ['y', 22.5], origin: [3.5, 0, 4.5] }),
  el('stone chunk b', [11, 0, 1.5], [14, 2.5, 4.5], 'scorched', { dirs: ['north', 'south', 'east', 'west', 'up'] }),
];

// tree blocks: logs (axis y as drawn; the turn lays them), leaves
MODELS.crookwood_log = [el('log', [0, 0, 0], [16, 16, 16], 'crook_log', { tex: { up: 'crook_log_top', down: 'crook_log_top' } })];
MODELS.crookwood_leaves = [el('leaves', [0, 0, 0], [16, 16, 16], 'crook_leaves')];
MODELS.crookwood_leaves_hanging = cross('crook_hang_end');
MODELS.crookwood_leaves_hanging_mid = cross('crook_hang');
MODELS.shieldcrown_log = [el('log', [0, 0, 0], [16, 16, 16], 'shield_log', { tex: { up: 'shield_log_top', down: 'shield_log_top' } })];
MODELS.shieldcrown_leaves = [el('leaves', [0, 0, 0], [16, 16, 16], 'shield_leaves', { tex: { down: 'shield_under' } })];
MODELS.shieldcrown_leaves_rim = [el('leaves', [0, 0, 0], [16, 16, 16], 'shield_leaves_rim', { tex: { up: 'shield_leaves', down: 'shield_under' } })];

// ---------------------------------------------------------------- checks a block model needs
function checks() {
  const Z = require('./zfight-check.js');
  const out = [];
  const okAngle = (a) => [0, 22.5, -22.5, 45, -45].includes(a);
  // what the covers sit against: the block they're on, for the z-fight check
  const wallSouth = { name: 'wall to the south', from: [0, 0, 16], to: [16, 16, 32], faces: { north: 1 } };
  const blockBelow = { name: 'block below', from: [0, -16, 0], to: [16, 0, 16], faces: { north: 1, up: 1 } };
  const context = (name) => (/knotvine_[abc]|lichen_[ab]|shelf|burst_wall/.test(name) ? [wallSouth] : /knotvine_floor|lichen_floor|carpet|drape/.test(name) ? [blockBelow] : []);
  for (const [name, els] of Object.entries(MODELS)) {
    for (const e of els) {
      const where = `${name}/${e.name}`;
      if (!Object.keys(e.faces).length) out.push(`${where}: no faces`);
      for (const v of [...e.from, ...e.to]) if (v < -16 || v > 32) out.push(`${where}: coordinate ${v} outside -16..32`);
      if (e.rotation && !okAngle(e.rotation.angle)) out.push(`${where}: rotation ${e.rotation.angle}`);
      for (const [d, f] of Object.entries(e.faces)) {
        if (!f.texture) out.push(`${where} ${d}: no texture`);
        if (f.uv[0] === f.uv[2] || f.uv[1] === f.uv[3]) out.push(`${where} ${d}: zero-width UV ${f.uv}`);
        if (f.uv.some((v) => v < 0 || v > 16)) out.push(`${where} ${d}: UV outside 0..16 ${f.uv}`);
      }
    }
    const list = [...els.map((e) => ({ name: e.name, from: e.from, to: e.to, faces: e.faces, rot: e.rotation ? [e.rotation.axis, e.rotation.angle] : null })), ...context(name)];
    for (const p of Z.check({ [name]: list })) out.push('z-fight ' + p);
  }
  // the multi-face covers on two faces of one cell (south and west), and the carpet with drapes on every side
  const turned = (els, ry) => els.map((e) => { const t = K.turnElement({ ...e, faces: Object.fromEntries(Object.entries(e.faces).map(([d, f]) => [d, { ...f, texture: 't' }])) }, 0, ry, false); return { name: e.name + ' y' + ry, from: t.from, to: t.to, faces: t.faces, rot: t.rotation ? [t.rotation.axis, t.rotation.angle] : null }; });
  for (const m of ['knotvine_a', 'lichen_a']) for (const p of Z.check({ corner: [...turned(MODELS[m], 0), ...turned(MODELS[m], 90), ...turned(MODELS.knotvine_floor, 0)] })) out.push('z-fight ' + p);
  for (const p of Z.check({ carpet: [...turned(MODELS.dusk_moss_carpet, 0), ...[0, 90, 180, 270].flatMap((r) => turned(MODELS.dusk_moss_drape, r)), blockBelow] })) out.push('z-fight ' + p);
  return out;
}

// ---------------------------------------------------------------- drawing models into the scene
// a model at a block cell, turned by rx/ry (90 degree steps, the blockstate turn), nudged by off (px; the random offset)
function draw(kids, model, cell, rx = 0, ry = 0, off = [0, 0, 0], label = model) {
  const els = MODELS[model];
  if (!els) throw new Error('no model ' + model);
  for (const e of els) {
    const t = K.turnElement(e, rx, ry, false), faces = {};
    for (const [d, f] of Object.entries(t.faces)) faces[d] = { ref: f.texture, uv: f.uv, rotation: f.rotation };
    const S = (v, i) => cell[i] * 16 + v + off[i];
    kids.push(scene.push(`${label} ${e.name}`, t.from.map(S), t.to.map(S), faces, { rotation: t.rotation, origin: t.rotation ? t.rotation.origin.map(S) : null, shade: t.shade }));
  }
}
// a model straight from the mod's assets (the lamps' glow parts, which the renderer draws)
function drawRef(kids, ref, cell, ry = 0, label = ref) {
  const m = scene.R.model(ref);
  for (const e0 of m.elements) {
    const t = K.turnElement(e0, 0, ry, false), faces = {};
    for (const [d, f] of Object.entries(t.faces)) faces[d] = { ref: scene.R.tex(m.textures, f.texture), uv: f.uv, rotation: f.rotation };
    const S = (v, i) => cell[i] * 16 + v;
    kids.push(scene.push(`${label} ${e0.name || ''}`, t.from.map(S), t.to.map(S), faces, { rotation: t.rotation, origin: t.rotation ? t.rotation.origin.map(S) : null, shade: t.shade }));
  }
}
// the turn that puts a model drawn against the south face onto the face toward d
const RY = { south: 0, west: 90, north: 180, east: 270 };
const jit = (x, y, z, s) => [Math.round((hash(x * 7 + z, y, s) - 0.5) * 6 * 4) / 4, 0, Math.round((hash(x, z * 7 + y, s + 1) - 0.5) * 6 * 4) / 4];
const key = (x, y, z) => `${x},${y},${z}`;
const B = (state, x, y, z) => ({ x, y, z, state, ...K.parseState(state) });

// value noise for the patches
function noise(x, y, s) {
  const xi = Math.floor(x), yi = Math.floor(y), fx = x - xi, fy = y - yi, sm = (t) => t * t * (3 - 2 * t);
  const h = (a, b) => hash(a, b, s);
  const a = h(xi, yi) + (h(xi + 1, yi) - h(xi, yi)) * sm(fx), b = h(xi, yi + 1) + (h(xi + 1, yi + 1) - h(xi, yi + 1)) * sm(fx);
  return a + (b - a) * sm(fy);
}

// ---------------------------------------------------------------- the overgrowth placer for a building and its street
// The brief's tiers: LIGHT (lichen freckles, a crack plant every few blocks, moss on some sills, one short rope at most) and
// HEAVY (Knotvine up whole wall sections, rope curtains, moss with drapes, roots, bramble in corners, Shiftcaps freely).
// Plants grip the makers' work and never void-alloy; one plant per cell; nothing in front of a lamp's lens.
const MAKERS_NO = /void_alloy|slotted_grille|scarred|clamp_bracket|derelict|brass_plaque|window|lamp|glass|bars/;
function overgrow(blocks, mode, seed, street) {
  const occ = new Map(blocks.map((b) => [key(b.x, b.y, b.z), b]));
  const taken = new Set(occ.keys());
  const plants = [], heavy = mode === 'heavy';
  const put = (model, x, y, z, rx = 0, ry = 0, off) => { const k = key(x, y, z); if (taken.has(k)) return false; taken.add(k); plants.push({ model, cell: [x, y, z], rx, ry, off }); return true; };
  const makers = (b) => b && scene.isFull(b) && !MAKERS_NO.test(b.id);
  const xs = blocks.map((b) => b.x), zs = blocks.map((b) => b.z), ys = blocks.map((b) => b.y);
  const [x0, x1, z0, z1, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...zs), Math.max(...zs), Math.max(...ys)];
  // open toward d all the way out (an outside face, not a room's)
  const open = (x, y, z, d) => { const v = K.VEC[d]; for (let i = 0; i < 40; i++) { x += v[0]; y += v[1]; z += v[2]; if (occ.has(key(x, y, z))) return false; if (x < x0 - 1 || x > x1 + 1 || z < z0 - 1 || z > z1 + 1 || y > y1 + 1) return true; } return true; };
  const hcol = (x) => Math.floor(1 + noise(x / 3, 0.5, seed) * 12);
  const n2 = (x, y) => noise(x / 4, y / 4, seed + 3);
  // ropes under eaves, corbels and sills: an empty cell under a block, open to the north (outside), with air below
  const eaves = [];
  for (const b of blocks) {
    const c = [b.x, b.y - 1, b.z];
    if (c[1] < 1 || occ.has(key(...c)) || !open(...c, 'north') || occ.has(key(c[0], c[1] - 1, c[2]))) continue;
    if (MAKERS_NO.test(b.id)) continue;
    eaves.push(c);
  }
  const ropes = heavy ? eaves.filter((c) => noise(c[0] / 2.5, c[1], seed + 9) > 0.42) : eaves.filter((c) => hash(c[0], c[1], seed + 11) > 0.93).slice(0, 1);
  for (const c of ropes) {
    let len = heavy ? 2 + Math.floor(hash(c[0], c[2], seed + 12) * 5) : 2, y = c[1];
    const cells = [];
    while (len-- > 0 && y >= 0 && !taken.has(key(c[0], y, c[2]))) { cells.push(y); y--; }
    cells.forEach((cy, i) => put(i === cells.length - 1 ? 'twinerope_tip' : 'twinerope', c[0], cy, c[2]));
  }
  // walls: north, east and west faces of makers' blocks open to the outside
  for (const b of blocks) {
    if (!makers(b)) continue;
    for (const d of ['north', 'east', 'west']) {
      const v = K.VEC[d], c = [b.x + v[0], b.y + v[1], b.z + v[2]];
      if (occ.has(key(...c)) || !open(...c, d)) continue;
      const toward = { north: 'south', east: 'west', west: 'east' }[d], ry = RY[toward];
      const r = hash(c[0] * 3 + c[2], c[1], seed + 5), lich = r < 0.5 ? 'lichen_a' : 'lichen_b', vine = ['knotvine_a', 'knotvine_b', 'knotvine_c'][Math.floor(r * 3)];
      if (heavy) {
        if (c[1] <= 2 && r > 0.9) put(`shiftcap_shelf_${['young', 'mature', 'old'][Math.floor(hash(c[0], c[1], 77) * 3)]}`, ...c, 0, ry);
        else if (c[1] <= hcol(c[0] * 1 + c[2]) || (c[1] === hcol(c[0] + c[2]) + 1 && r < 0.4)) put(vine, ...c, 0, ry);
        else if (n2(c[0] + c[2], c[1]) > 0.45 && r < 0.5) put(lich, ...c, 0, ry);
      } else {
        if (n2(c[0] + c[2], c[1]) > 0.55 && r < 0.45) put(lich, ...c, 0, ry);
        else if (c[1] <= 1 && noise(c[0] / 3, 9, seed) > 0.78 && r < 0.7) put(vine, ...c, 0, ry);
      }
    }
  }
  // moss on up-faces (sills, plinths, wall tops): carpets on full makers' blocks, with drapes on open sides
  for (const b of blocks) {
    if (!makers(b)) continue;
    const c = [b.x, b.y + 1, b.z];
    if (occ.has(key(...c)) || !open(...c, 'up')) continue;
    const r = hash(c[0], c[2] * 5 + c[1], seed + 21);
    if (heavy ? r < 0.7 : (n2(c[0], c[1]) > 0.55 && r < 0.35)) {
      if (!put('dusk_moss_carpet', ...c)) continue;
      if (heavy) for (const d of ['north', 'east', 'west']) { const v = K.VEC[d]; if (!occ.has(key(c[0] + v[0], c[1] - 1, c[2] + v[2])) && hash(c[0], c[1], seed + 22 + d.length) < 0.8) plants.push({ model: 'dusk_moss_drape', cell: c, rx: 0, ry: { north: 0, east: 90, west: 270 }[d] }); }
    }
  }
  // the street: cells at y 0 on the paving (rows z in street.z, x across the building)
  const wallFoot = new Set();
  for (const b of blocks) if (b.y === 0 && makers(b)) { for (const d of ['north', 'east', 'west']) { const v = K.VEC[d]; if (!occ.has(key(b.x + v[0], 0, b.z + v[2]))) wallFoot.add(key(b.x + v[0], 0, b.z + v[2])); } }
  const ground = [];
  for (let x = street.x[0]; x <= street.x[1]; x++) for (let z = street.z[0]; z <= street.z[1]; z++) if (!occ.has(key(x, 0, z))) ground.push([x, 0, z]);
  const avoid = new Set((street.avoid || []).map((c) => key(...c)));
  let bursts = 0;
  const near = (x, z) => [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([a, b]) => wallFoot.has(key(x + a, 0, z + b)) || occ.has(key(x + a, 0, z + b)));
  for (const [x, y, z] of ground) {
    if (avoid.has(key(x, y, z))) continue;
    const r = hash(x, z, seed + 31), pn = noise(x / 3, z / 3, seed + 33), foot = wallFoot.has(key(x, y, z));
    if (heavy) {
      if (foot && bursts < 2 && r > 0.93) { if (put('breakroot_burst', x, y, z, 0, 180)) { bursts++; const w = occ.get(key(x, 0, z + 1)); if (w && /stained_end_stone_bricks$/.test(w.id)) street.cracked.push(key(x, 0, z + 1)); } }
      else if (foot && r < 0.22) put(r < 0.1 ? 'bramblecoil_large' : 'bramblecoil_small', x, y, z);
      else if (foot && r < 0.4) put(`shiftcap_${['young', 'mature', 'old'][Math.floor(hash(x, z, 78) * 3)]}`, x, y, z, 0, Math.floor(r * 40) % 4 * 90);
      else if (pn > 0.62 && r < 0.12) put('breakroot_knuckle', x, y, z, 0, r < 0.06 ? 0 : 90);
      else if ((foot && r < 0.7) || (pn > 0.66 && r < 0.8) || (pn > 0.56 && near(x, z) && r < 0.6)) put('dusk_moss_carpet', x, y, z);
      else if (r < 0.16) put('corkscrew_rush', x, y, z, 0, 0, jit(x, y, z, 1));
      else if (r < 0.22) put(r < 0.19 ? 'springcurl_grown' : 'springcurl_young', x, y, z, 0, 0, jit(x, y, z, 2));
      else if (pn > 0.45 && r < 0.3) put(`shiftcap_${['young', 'mature', 'old'][Math.floor(hash(x, z, 79) * 3)]}`, x, y, z);
      else if (r < 0.34) put('knotvine_floor', x, y, z, 0, Math.floor(r * 100) % 4 * 90);
    } else {
      if (r < 0.1) put('corkscrew_rush', x, y, z, 0, 0, jit(x, y, z, 1));
      else if (r < 0.14) put(r < 0.12 ? 'springcurl_young' : 'springcurl_grown', x, y, z, 0, 0, jit(x, y, z, 2));
      else if (foot && pn > 0.6 && r < 0.3) put('dusk_moss_carpet', x, y, z);
    }
  }
  for (const c of street.bells || []) put('vigil_bell', ...c, 0, 0, jit(...c, 3));
  return plants;
}

// ---------------------------------------------------------------- the scene (built only when this runs, not when the exporter requires it)
function buildScene() {
const nodes = [];
const layout = (n) => K.readLayout(path.join(DUMP, n + '.json'));
const paving = (x, z) => { const h = Math.abs((x * 73856093) ^ (z * 19349663) ^ (11 * 83492791)) % 20; return h < 13 ? 'minecraft:polished_andesite' : h < 16 ? 'minecraft:stone_bricks' : h < 19 ? 'minecraft:cracked_stone_bricks' : 'minecraft:andesite'; };
const scorched = (x, z) => { const h = Math.abs((x * 73856093) ^ (z * 19349663) ^ (7 * 83492791)) % 10; return h < 2 ? 'comedymonkeysaddons:rusted_scorched_end_stone' : h < 3 ? 'comedymonkeysaddons:littered_scorched_end_stone' : 'comedymonkeysaddons:scorched_end_stone'; };
// a ground slab of blocks at y -1 (only their tops and outer sides drawn)
function groundSlab(name, x0, x1, z0, z1, pick, at) {
  const blocks = [];
  for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) blocks.push(B(pick(x, z), x, -1, z));
  const tops = new Map();
  scene.addBuilding(name, blocks, at, { custom: (b) => {
    const parts = scene.blockParts(b), faces = {};
    const top = parts[0] && parts[0].faces.up, side = parts[0] && (parts[0].faces.north || parts[0].faces.up);
    if (!top) return null;
    for (const d of ['up', 'north', 'south', 'east', 'west']) faces[d] = { ref: (d === 'up' ? top : side).ref, uv: [0, 0, 16, 16] };
    return faces;
  } });
  return tops;
}
function plantGroup(name, plants, at) {
  const kids = [];
  for (const p of plants) draw(kids, p.model, [p.cell[0] + at[0], p.cell[1] + at[1], p.cell[2] + at[2]], p.rx, p.ry, p.off || [0, 0, 0], `${p.model} ${p.cell.join(',')}`);
  const g = scene.group(name, kids, at.map((v) => v * 16));
  return g;
}

// ---------- 1. the wall study: the terrace LIGHT, a void-alloy tower (left bare, the grip rule), the terrace HEAVY
const terrace = layout('terrace_intact');
const towerBlocks = [];
for (let y = 0; y < 16; y++) for (let x = 0; x < 3; x++) for (let z = 0; z < 3; z++) {
  const corner = (x === 0 || x === 2) && (z === 0 || z === 2);
  let st = corner ? 'comedymonkeysaddons:void_alloy_pillar[axis=y]' : 'comedymonkeysaddons:void_alloy_plate';
  if (y === 5 || y === 11) st = 'comedymonkeysaddons:void_alloy_band[axis=y,north=false,south=false,east=false,west=false]';
  if (!corner && (y === 7 || y === 8 || y === 13) && (x === 1 || z === 1)) st = `comedymonkeysaddons:slotted_grille[axis=${x === 1 ? 'z' : 'x'}]`;
  towerBlocks.push(B(st, x, y + 1, z));
}
for (let x = -1; x < 4; x++) for (let z = -1; z < 4; z++) towerBlocks.push(B(x === -1 || x === 3 || z === -1 || z === 3 ? 'create:cut_deepslate_bricks' : 'comedymonkeysaddons:void_alloy_plate', x, 0, z));
towerBlocks.push(B('comedymonkeysaddons:void_alloy_slab[type=bottom,waterlogged=false]', 1, 17, 1));
const WALL = { light: [0, 0, 0], tower: [27, 0, 0], heavy: [34, 0, 0] };
function study(name, blocks, at, mode, seed, streetSpec) {
  const street = { cracked: [], ...streetSpec };
  const plants = mode ? overgrow(blocks, mode, seed, street) : [];
  const shown = blocks.map((b) => (street.cracked.includes(key(b.x, b.y, b.z)) ? { ...b, ...K.parseState('comedymonkeysaddons:cracked_stained_end_stone_bricks'), state: 'comedymonkeysaddons:cracked_stained_end_stone_bricks' } : b));
  const kids = [];
  const bnode = scene.addBuilding(name + ' (blocks)', shown, at, { parent: { children: kids } });
  if (plants.length) kids.push(plantGroup(name + ' (overgrowth)', plants, at));
  nodes.push(scene.group(name, kids, at.map((v) => v * 16)));
  return { plants, bnode };
}
const terraceStreet = { x: [-1, 21], z: [-7, -3], bells: [] };
const counts = {};
counts.terraceLight = study('WALL STUDY: terrace, LIGHT', terrace, WALL.light, 'light', 401, terraceStreet).plants.length;
study('WALL STUDY: void-alloy tower, bare (plants grip the makers\' work, never void-alloy)', towerBlocks, WALL.tower, null);
counts.terraceHeavy = study('WALL STUDY: terrace, HEAVY', terrace, WALL.heavy, 'heavy', 402, terraceStreet).plants.length;
groundSlab('wall study street', -2, 58, -8, 7, (x, z) => (z === -2 || z === -3 ? scorched(x, z) : z < -2 ? paving(x, z) : scorched(x, z)), [0, 0, 0]);

// ---------- 2. the street corner: the makers' shed LIGHT and HEAVY, a lamp at each corner (one leaning), cable, a wreck, a shell
const shed = layout('ridge_shed');
const SHED = { light: [66, 0, 0], heavy: [88, 0, 0] };
function cornerProps(lit) {
  const p = [];
  // the street lamp at the corner (foot, two poles, head facing the street), and a leaning one along the front
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=foot]', -3, 0, -4));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=pole]', -3, 1, -4));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=pole]', -3, 2, -4));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=head,snapped=false]', -3, 3, -4));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=true,part=foot]', 9, 0, -5));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=true,part=pole]', 9, 1, -5));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=true,part=pole]', 9, 2, -5));
  p.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=true,part=head,snapped=false]', 9, 3, -5));
  p.push(B('comedymonkeysaddons:broken_press[facing=north]', 3, 0, -4));
  p.push(B('comedymonkeysaddons:resistance_shell[facing=north,pose=slumped]', 6, 0, -3));
  return p;
}
for (const [mode, at, seed] of [['light', SHED.light, 501], ['heavy', SHED.heavy, 502]]) {
  const blocks = [...shed, ...cornerProps()];
  const avoid = [[-3, 0, -4], [-3, 0, -5], [-3, 0, -3], [9, 0, -5], [9, 0, -6], [9, 0, -4]];
  const res = study(`STREET CORNER: makers' shed, ${mode.toUpperCase()}`, blocks, at, mode, seed, { x: [-6, 14], z: [-8, -3], avoid, bells: [[-2, 0, -4], [-3, 0, -5], [8, 0, -5]] });
  counts['shed' + mode] = res.plants.length;
  // the corner lamp lit in the HEAVY study (the renderer's glow part)
  if (mode === 'heavy') { const kids = []; drawRef(kids, 'comedymonkeysaddons:block/wastes/derelict_street_lamp_glow', [-3 + at[0], 3, -4 + at[2]], 0, 'lamp glow (lit)'); nodes.push(scene.group('STREET CORNER: the corner lamp lit', kids)); }
}
groundSlab('street corner street', 60, 104, -9, 13, (x, z) => ((x - SHED.light[0] >= -2 && x - SHED.light[0] <= 14 && z <= -3) || (x - SHED.heavy[0] >= -2 && x - SHED.heavy[0] <= 14 && z <= -3) ? paving(x, z) : scorched(x, z)), [0, 0, 0]);

// ---------- 3. the grapple: the two war-frames as the city lays them (CityLayout.warFrames), the Crookwood up between the
//            resistance frame's hand and the uprising frame's lens, its roots round both, the siege works' heavy growth
const GRAPPLE = [112, 0, 0];
function warFrames() {
  const b = [], put = (s, x, y, z) => b.push(B(s, x, y, z));
  const fill = (s, xa, ya, za, xb, yb, zb) => { for (let x = xa; x <= xb; x++) for (let y = ya; y <= yb; y++) for (let z = za; z <= zb; z++) put(s, x, y, z); };
  const PIL = 'comedymonkeysaddons:void_alloy_pillar[axis=x]', VA = 'comedymonkeysaddons:void_alloy_plate', CAS = 'create:andesite_casing', BR = 'create:brass_block';
  for (let i = 0; i < 11; i++) put(PIL, i, 0, 0);
  put(PIL, 2, 0, -1); put(PIL, 3, 0, -1);
  fill(VA, 5, 1, 0, 8, 1, 0);
  fill(VA, 11, 0, -1, 12, 1, 1);
  b.splice(b.findIndex((q) => q.x === 12 && q.y === 1 && q.z === 0), 1); put('minecraft:tinted_glass', 12, 1, 0);
  fill(CAS, 9, 0, 2, 11, 2, 5); fill('create:brass_casing', 9, 3, 3, 11, 3, 4);
  put(BR, 12, 2, 1); b.splice(b.findIndex((q) => q.x === 12 && q.y === 1 && q.z === 1), 1); put(CAS, 12, 1, 1);
  fill(CAS, 9, 0, 6, 11, 2, 7);
  put('minecraft:brown_stained_glass', 9, 1, 8); put('minecraft:brown_stained_glass', 11, 1, 8); put(BR, 10, 1, 8);
  fill(BR, 7, 0, 3, 8, 2, 4);
  for (let s = 0; s < 10; s++) { const x = 6 - Math.floor(s / 2), z = 4 + (s % 3); if (!b.some((q) => q.x === x && q.y === 0 && q.z === z)) put('minecraft:iron_bars[east=false,north=false,south=false,west=false,waterlogged=false]', x, 0, z); }
  fill(BR, 7, 3, 3, 7, 5, 3); fill(BR, 7, 6, 2, 7, 6, 4);
  return b;
}
function crookwood(base) {
  // the corkscrew: up, a step west, up, a step south, up, a step east, up; branches at the top, drooping pale crowns
  const [bx, by, bz] = base, logs = [], leaves = new Map();
  const L = (x, y, z, axis) => logs.push({ x: bx + x, y: by + y, z: bz + z, axis });
  for (let y = 0; y <= 3; y++) L(0, y, 0, 'y');
  L(-1, 3, 0, 'x'); for (let y = 4; y <= 6; y++) L(-1, y, 0, 'y');
  L(-1, 6, 1, 'z'); for (let y = 7; y <= 9; y++) L(-1, y, 1, 'y');
  L(0, 9, 1, 'x'); for (let y = 10; y <= 13; y++) L(0, y, 1, 'y');
  L(-1, 11, 1, 'x'); L(-2, 11, 1, 'x'); L(-3, 11, 1, 'x'); L(-4, 11, 1, 'x');
  L(1, 12, 1, 'x'); L(2, 12, 1, 'x'); L(3, 12, 1, 'x');
  L(0, 12, 0, 'z'); L(0, 12, -1, 'z'); L(0, 12, -2, 'z');
  const crown = (cx, cy, cz, r, drop) => {
    for (let dy = 0; dy <= 1; dy++) for (let dx = -Math.ceil(r); dx <= Math.ceil(r); dx++) for (let dz = -Math.ceil(r); dz <= Math.ceil(r); dz++) {
      const rr = dy ? r - 1.4 : r;
      if (dx * dx + dz * dz > rr * rr + 0.3) continue;
      leaves.set(key(bx + cx + dx, by + cy + dy, bz + cz + dz), 'crookwood_leaves');
    }
    // the willow droop: columns hanging from the bottom ring, the last block fringed
    for (let dx = -Math.ceil(r); dx <= Math.ceil(r); dx++) for (let dz = -Math.ceil(r); dz <= Math.ceil(r); dz++) {
      const d2 = dx * dx + dz * dz;
      if (d2 > r * r + 0.3 || d2 < (r - 1.3) * (r - 1.3)) continue;
      if (hash(cx + dx, cz + dz, 602) < 0.25) continue;
      const len = 1 + Math.floor(hash(cx + dx, cz + dz, 601) * drop);
      for (let i = 1; i <= len; i++) leaves.set(key(bx + cx + dx, by + cy - i, bz + cz + dz), i === len ? 'crookwood_leaves_hanging' : 'crookwood_leaves_hanging_mid');
    }
  };
  crown(-5, 12, 1, 2.6, 4); crown(4, 13, 1, 2.3, 4); crown(0, 13, -3, 2.2, 3); crown(0, 14, 1, 1.8, 2);
  for (const l of logs) leaves.delete(key(l.x, l.y, l.z));
  return { logs, leaves };
}
function shieldcrown(base, h = 11) {
  // a straight braided stem, four branches stepping out from 70% up, a flat round crown 11 across and 2 thick
  const [bx, by, bz] = base, logs = [], leaves = new Map();
  for (let y = 0; y <= h; y++) logs.push({ x: bx, y: by + y, z: bz, axis: 'y' });
  for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) for (let i = 1; i <= 3; i++) logs.push({ x: bx + dx * i, y: by + h - 3 + i, z: bz + dz * i, axis: 'y' });
  for (const [dy, r] of [[h + 1, 5.3], [h + 2, 4.3]]) for (let dx = -6; dx <= 6; dx++) for (let dz = -6; dz <= 6; dz++) {
    const d = Math.hypot(dx, dz);
    if (d > r) continue;
    leaves.set(key(bx + dx, by + dy, bz + dz), d > r - 1.1 ? 'shieldcrown_leaves_rim' : 'shieldcrown_leaves');
  }
  return { logs, leaves };
}
// Twinerope strands under a crown: bottom leaves with air below, n of them picked by hash, 2-3 long
function ropesUnder(t, n, seed, at, taken) {
  const logs = new Set(t.logs.map((l) => key(l.x, l.y, l.z)));
  const cand = [...t.leaves.keys()].map((k) => k.split(',').map(Number)).filter(([x, y, z]) => !t.leaves.has(key(x, y - 1, z)) && !logs.has(key(x, y - 1, z)) && !taken.has(key(x - at[0], y - 1, z - at[2])));
  cand.sort((a, b) => hash(a[0], a[2], seed) - hash(b[0], b[2], seed));
  const out = [];
  for (const [x, y, z] of cand.slice(0, n)) {
    const len = 2 + Math.floor(hash(x, z, seed + 1) * 2);
    for (let i = 1; i <= len; i++) { taken.add(key(x - at[0], y - i, z - at[2])); out.push({ model: i === len ? 'twinerope_tip' : 'twinerope', cell: [x - at[0], y - i, z - at[2]], rx: 0, ry: 0 }); }
  }
  return out;
}
function treeGroup(name, t, flat) {
  const kids = [], logModel = t.logs.length && t.leaves.size && [...t.leaves.values()][0].startsWith('crook') ? 'crookwood_log' : 'shieldcrown_log';
  for (const l of t.logs) {
    const [rx, ry] = l.axis === 'y' ? [0, 0] : l.axis === 'z' ? [90, 0] : [90, 90];
    if (flat) kids.push(scene.push('log (shape)', [l.x * 16, l.y * 16, l.z * 16], [l.x * 16 + 16, l.y * 16 + 16, l.z * 16 + 16], Object.fromEntries(DIRS.map((d) => [d, { ref: T.palette, uv: palUv('#0b0b0d') }]))));
    else draw(kids, logModel, [l.x, l.y, l.z], rx, ry, [0, 0, 0], `${logModel} ${l.axis}`);
  }
  for (const [k, m] of t.leaves) {
    const [x, y, z] = k.split(',').map(Number);
    const inset = /hanging/.test(m) ? 5 : 0; // a curtain strand is see-through: drawn as a thin column
    if (flat) kids.push(scene.push('leaves (shape)', [x * 16 + inset, y * 16, z * 16 + inset], [x * 16 + 16 - inset, y * 16 + 16, z * 16 + 16 - inset], Object.fromEntries(DIRS.map((d) => [d, { ref: T.palette, uv: palUv('#0b0b0d') }]))));
    else draw(kids, m, [x, y, z], 0, 0, [0, 0, 0], m);
  }
  return scene.group(name, kids);
}
{
  const frames = warFrames(), at = GRAPPLE;
  const kids = [];
  scene.addBuilding('war-frames (CityLayout.warFrames)', frames, at, { parent: { children: kids } });
  const t = crookwood([13 + at[0], 0, 1 + at[2]]);
  kids.push(treeGroup('CROOKWOOD (logs and leaves)', t));
  // the growth round the frames: roots round both, Knotvine over the casing, moss, Shiftcaps on the wrecks, ropes from the crown
  const p = [];
  const occ = new Set(frames.map((b) => key(b.x, b.y, b.z)));
  for (const l of t.logs) occ.add(key(l.x - at[0], l.y, l.z - at[2]));
  for (const k of t.leaves.keys()) { const [x, y, z] = k.split(',').map(Number); occ.add(key(x - at[0], y, z - at[2])); }
  const add = (model, x, y, z, ry = 0, off) => { if (!occ.has(key(x, y, z))) { occ.add(key(x, y, z)); p.push({ model, cell: [x, y, z], rx: 0, ry, off }); } };
  add('breakroot_run', 14, 0, 1, 90); add('breakroot_run', 14, 0, 2, 90); add('breakroot_knuckle', 13, 0, 3, 90); add('breakroot_burst', 14, 0, 0);
  add('breakroot_run', 13, 0, -1, 0); add('breakroot_knuckle', 10, 0, 1, 0);
  // Knotvine on the resistance frame's casing (north faces), shelves on the uprising frame's head and the casing
  for (const b of frames) if (/andesite_casing/.test(b.id)) { const c = [b.x, b.y, b.z - 1]; if (!frames.some((q) => q.x === c[0] && q.y === c[1] && q.z === c[2]) && hash(b.x, b.y, 611) < 0.7) add(['knotvine_a', 'knotvine_b', 'knotvine_c'][Math.floor(hash(b.x, b.y, 612) * 3)], ...c, RY.south); }
  for (const b of frames) if (/andesite_casing/.test(b.id) && b.x === 11) { const c = [12, b.y, b.z]; if (hash(b.y, b.z, 613) < 0.6) add('lichen_a', ...c, RY.west); }
  add('shiftcap_shelf_old', 13, 1, -1, RY.west); add('shiftcap_shelf_mature', 8, 1, 2, RY.south);
  add('shiftcap_old', 9, 1, 1); add('shiftcap_mature', 4, 1, 0); add('shiftcap_young', 13, 0, 4);
  for (const [x, z] of [[8, 1], [7, 1], [6, 0], [5, 0], [7, 2], [6, 2], [4, 1], [12, 3], [12, 4], [8, 6], [8, 5], [10, -1], [9, -1]]) add('dusk_moss_carpet', x, 0, z);
  for (const [x, z] of [[1, 0], [3, 1], [10, 0], [4, 0]]) add('dusk_moss_carpet', x, 1, z);
  add('bramblecoil_large', 15, 0, 3); add('bramblecoil_small', 8, 0, 8); add('bramblecoil_small', 2, 0, 2);
  for (const [x, z, m] of [[6, -2, 'corkscrew_rush'], [3, -2, 'springcurl_grown'], [12, -3, 'corkscrew_rush'], [15, -1, 'springcurl_young'], [1, 3, 'corkscrew_rush'], [5, 7, 'springcurl_grown']]) add(m, x, 0, z, 0, jit(x, 0, z, 4));
  // ropes from the crowns' undersides
  p.push(...ropesUnder(t, 3, 621, at, occ));
  kids.push(plantGroup('grapple overgrowth', p, at));
  nodes.push(scene.group('THE GRAPPLE: the Crookwood between the hand and the lens', kids, at.map((v) => v * 16)));
  groundSlab('grapple ground', at[0] - 6, at[0] + 20, -7, 12, (x, z) => scorched(x, z), [0, 0, 0]);
}

// ---------- 4. the Shieldcrown up through a burnt-out shed: the roof's middle gone, the stem from the floor, the disc above the roof
const SHIELD = [136, 0, 0];
{
  const at = SHIELD, cx = 6, cz = 4;
  const hole = (b) => b.y >= 4 && Math.abs(b.x - cx) <= 2 && Math.abs(b.z - cz) <= 2 + (b.y >= 7 ? 1 : 0);
  const blocks = shed.filter((b) => !hole(b) && !(b.x === cx && b.z === cz));
  const kids = [];
  scene.addBuilding('burnt-out shed (the roof holed over its middle)', blocks, at, { parent: { children: kids } });
  const t = shieldcrown([cx + at[0], 0, cz + at[2]], 11);
  kids.push(treeGroup('SHIELDCROWN (logs and leaves)', t));
  const p = [], occ = new Set(blocks.map((b) => key(b.x, b.y, b.z)));
  for (const l of t.logs) occ.add(key(l.x - at[0], l.y, l.z - at[2]));
  for (const k of t.leaves.keys()) { const [x, y, z] = k.split(',').map(Number); occ.add(key(x - at[0], y, z - at[2])); }
  // roots on the shed floor round the stem (on whatever floor is there), moss and a fern in the burnt room, ropes off the rim outside
  for (const [x, z, m, ry] of [[cx + 1, cz, 'breakroot_knuckle', 0], [cx - 1, cz + 1, 'breakroot_knuckle', 90], [cx, cz - 1, 'breakroot_run', 90], [cx - 2, cz - 1, 'dusk_moss_carpet', 0], [cx + 2, cz + 2, 'springcurl_grown', 0], [cx + 2, cz - 2, 'shiftcap_mature', 0]]) {
    let y = 0; while (occ.has(key(x, y, z)) && y < 3) y++;
    if (!occ.has(key(x, y, z))) { occ.add(key(x, y, z)); p.push({ model: m, cell: [x, y, z], rx: 0, ry }); }
  }
  p.push(...ropesUnder(t, 4, 631, at, occ).filter((q) => q.cell[1] > 0));
  kids.push(plantGroup('shed overgrowth', p, at));
  nodes.push(scene.group('THE SHIELDCROWN: a shield over the hole', kids, at.map((v) => v * 16)));
  groundSlab('shieldcrown ground', at[0] - 4, at[0] + 16, -6, 14, (x, z) => (z <= -3 ? paving(x, z) : scorched(x, z)), [0, 0, 0]);
}

// ---------- 5. both trees as black shapes, side by side (the silhouette check)
const SIL = [160, 0, 0];
{
  const a = crookwood([SIL[0] + 4, 0, 2]), b = shieldcrown([SIL[0] + 18, 0, 2], 11);
  nodes.push(scene.group('SILHOUETTES: Crookwood (left) and Shieldcrown (right) as black shapes', [treeGroup('crookwood shape', a, true), treeGroup('shieldcrown shape', b, true)]));
  groundSlab('silhouette ground', SIL[0] - 2, SIL[0] + 26, -4, 8, (x, z) => scorched(x, z), [0, 0, 0]);
}

// ---------- 6. the lit check: a lit street lamp and wall lamp, lit seamed void-alloy, Shiftcaps of every age and shelves beside them
const LIT = [192, 0, 0];
{
  const at = LIT, b = [];
  for (let x = 0; x < 8; x++) for (let y = 0; y < 4; y++) b.push(B(x >= 5 && y >= 1 ? 'comedymonkeysaddons:seamed_void_alloy_plate[lit=true]' : 'comedymonkeysaddons:stained_end_stone_bricks', x, y, 1));
  b.push(B('comedymonkeysaddons:derelict_wall_lamp[facing=north]', 2, 3, 0));
  for (const [y, part] of [[0, 'foot'], [1, 'pole'], [2, 'pole']]) b.push(B(`comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=${part}]`, 6, y, -3));
  b.push(B('comedymonkeysaddons:derelict_street_lamp[facing=north,leaning=false,part=head,snapped=false]', 6, 3, -3));
  const kids = [];
  scene.addBuilding('lit check: wall, lamps, lit seamed plate', b, at, { parent: { children: kids } });
  const g = [];
  drawRef(g, 'comedymonkeysaddons:block/wastes/derelict_street_lamp_glow', [6 + at[0], 3, -3 + at[2]], 0, 'street lamp glow (lit)');
  drawRef(g, 'comedymonkeysaddons:block/wastes/derelict_wall_lamp_glow', [2 + at[0], 3, 0 + at[2]], 0, 'wall lamp glow (lit)');
  kids.push(scene.group('lamp glows (lit)', g));
  const p = [];
  [['shiftcap_young', 1, 0, 0], ['shiftcap_mature', 3, 0, -1], ['shiftcap_old', 5, 0, -2], ['shiftcap_young', 7, 0, -2], ['vigil_bell', 6, 0, -4], ['vigil_bell', 5, 0, -3]].forEach(([m, x, y, z]) => p.push({ model: m, cell: [x, y, z], rx: 0, ry: 0 }));
  [['shiftcap_shelf_young', 0, 1, 0], ['shiftcap_shelf_mature', 1, 2, 0], ['shiftcap_shelf_old', 3, 1, 0], ['shiftcap_shelf_mature', 4, 2, 0]].forEach(([m, x, y, z]) => p.push({ model: m, cell: [x, y, z], rx: 0, ry: RY.south }));
  kids.push(plantGroup('Shiftcaps and bells', p, at));
  nodes.push(scene.group('LIT CHECK: Shiftcap glow beside starlight', kids, at.map((v) => v * 16)));
  groundSlab('lit check ground', at[0] - 2, at[0] + 10, -6, 3, (x, z) => (z <= -2 ? paving(x, z) : scorched(x, z)), [0, 0, 0]);
}

// ---------- 7. the swatch board (z -40): every species and state, and the colour chips
const SW = [0, 0, -40];
{
  const at = SW, b = [], p = [];
  const P = (model, x, y, z, ry = 0, off) => p.push({ model, cell: [x, y, z], rx: 0, ry, off });
  // Knotvine on a 3 x 2 brick wall (the three variants, each row) and creeping on the ground before it
  for (let x = 0; x < 3; x++) for (let y = 0; y < 2; y++) { b.push(B('comedymonkeysaddons:stained_end_stone_bricks', x, y, 1)); P(['knotvine_a', 'knotvine_b', 'knotvine_c'][x], x, y, 0, RY.south); }
  for (let x = 0; x < 3; x++) P('knotvine_floor', x, 0, -1);
  // Scale Lichen on a 2 x 2 wall
  for (let x = 4; x < 6; x++) for (let y = 0; y < 2; y++) { b.push(B('comedymonkeysaddons:stained_end_stone_bricks', x, y, 1)); P((x + y) % 2 ? 'lichen_b' : 'lichen_a', x, y, 0, RY.south); }
  // Twinerope from a deepslate frame: lengths 4, 2 and 1
  for (let y = 0; y < 5; y++) { b.push(B('create:cut_deepslate_bricks', 7, y, 0)); b.push(B('create:cut_deepslate_bricks', 11, y, 0)); }
  for (let x = 8; x < 11; x++) b.push(B('create:cut_deepslate_bricks', x, 4, 0));
  [[8, 4], [9, 2], [10, 1]].forEach(([x, len]) => { for (let i = 0; i < len; i++) P(i === len - 1 ? 'twinerope_tip' : 'twinerope', x, 3 - i, 0); });
  // Dusk Moss: the block, a carpet on a plinth with its drapes, a carpet on the ground
  P('dusk_moss', 13, 0, 0);
  b.push(B('comedymonkeysaddons:stained_end_stone_bricks', 15, 0, 0)); P('dusk_moss_carpet', 15, 1, 0);
  for (const ry of [0, 90, 180, 270]) p.push({ model: 'dusk_moss_drape', cell: [15, 1, 0], rx: 0, ry });
  P('dusk_moss_carpet', 17, 0, 0);
  // the crack plants
  [['springcurl_young', 19], ['springcurl_grown', 21], ['corkscrew_rush', 23], ['vigil_bell', 25]].forEach(([m, x]) => P(m, x, 0, 0));
  // Bramblecoil small and large
  P('bramblecoil_small', 27, 0, 0); P('bramblecoil_large', 29, 0, 0);
  // Shiftcap clusters young, mature, old, and the shelves above them on a wall
  for (let x = 31; x < 36; x++) for (let y = 0; y < 3; y++) b.push(B('comedymonkeysaddons:stained_end_stone_bricks', x, y, 1));
  [['young', 31], ['mature', 33], ['old', 35]].forEach(([age, x]) => { P('shiftcap_' + age, x, 0, 0); P('shiftcap_shelf_' + age, x, 2, 0, RY.south); });
  // Breakroot: knuckle, run (two), burst
  P('breakroot_knuckle', 38, 0, 0); P('breakroot_run', 40, 0, 0); P('breakroot_run', 41, 0, 0); P('breakroot_burst', 43, 0, 0);
  b.push(B('comedymonkeysaddons:stained_end_stone_bricks', 44, 0, 1), B('comedymonkeysaddons:stained_end_stone_bricks', 44, 1, 1)); P('breakroot_burst_wall', 44, 0, 0, RY.south);
  // the tree blocks
  P('crookwood_log', 46, 0, 0); P('crookwood_log', 46, 1, 0); P('crookwood_log', 46, 2, 0); P('crookwood_leaves', 47, 2, 0); P('crookwood_leaves_hanging_mid', 47, 1, 0); P('crookwood_leaves_hanging', 47, 0, 0);
  P('shieldcrown_log', 49, 0, 0); P('shieldcrown_log', 49, 1, 0); P('shieldcrown_leaves', 50, 1, 0); P('shieldcrown_leaves_rim', 51, 1, 0);
  const kids = [];
  scene.addBuilding('swatch board walls and plinths', b, at, { parent: { children: kids } });
  kids.push(plantGroup('every species and state', p, at));
  // the chips: plant ramps (upper rows) and the approved colours to stay clear of (lower rows), on a near-black board
  const ch = [], cx0 = (at[0] + 54) * 16, cz = (at[2] + 1) * 16;
  const rows = [...PLANT_RAMPS.map(([n, r]) => ['plant: ' + n, r]), ...CLEAR.map(([n, r]) => ['stay clear: ' + n, r])];
  const H = rows.length * 7 + 4;
  ch.push(scene.push('chip board', [cx0 - 2, 0, cz], [cx0 + 6 * 7 + 2, H, cz + 1], { north: { ref: T.palette, uv: palUv('#101114') } }));
  rows.forEach(([n, r], i) => r.forEach((c, j) => ch.push(scene.push(`${n} ${c}`, [cx0 + j * 7, H - 4 - i * 7 - 5, cz - 0.5], [cx0 + j * 7 + 6, H - 4 - i * 7 + 1, cz], { north: { ref: T.palette, uv: palUv(c) } }))));
  kids.push(scene.group('colour chips (plant ramps above, colours to stay clear of below)', ch));
  nodes.push(scene.group('SWATCH BOARD: every species and state', kids, at.map((v) => v * 16)));
  groundSlab('swatch ground', -2, 62, -44, -38, (x, z) => scorched(x, z), [0, 0, 0]);
}

// ---------- the base under everything, one block down (the studies' ground slabs read as plinths)
{
  const tex = K.decodePng(fs.readFileSync(path.join(REPO, 'src/main/resources/assets/comedymonkeysaddons/textures/block/wastes/scorched.png')));
  const N = 8, W = 16 * N, d = new Uint8Array(W * W * 4);
  for (let by = 0; by < N; by++) for (let bx = 0; bx < N; bx++) for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const o = (y * 16 + x) * 4; d.set(tex.rgba.subarray(o, o + 4), ((by * 16 + y) * W + bx * 16 + x) * 4); }
  const g = scene.addTexture('base_scorched', png(W, W, d));
  scene.own[g].w = W; scene.own[g].h = W;
  const kids = [];
  for (let x = -16; x < 216; x += N) for (let z = -56; z < 24; z += N) kids.push(scene.push('base', [x * 16, -17, z * 16], [(x + N) * 16, -16, (z + N) * 16], { up: { ref: g, uv: [0, 0, 16, 16] } }));
  nodes.push(scene.group('base (Scorched End Stone, one block down)', kids));
}

scene.outliner.push(...nodes);
return counts;
}

// ---------------------------------------------------------------- the icon sheet: each species' sprite at 16, 32 and 64 px
function iconSheet(file) {
  const icons = ['knotvine_a', 'lichen_a', 'item_twinerope', 'item_shiftcap', 'item_shelf', 'moss_top', 'springcurl_young', 'springcurl_grown', 'rush', 'vigil_bell', 'bramble_sprig', 'gill_mature', 'root_bark', 'crook_log', 'crook_leaves', 'shield_log', 'shield_leaves_rim'];
  const S = [1, 2, 4], pad = 6, cell = 16 * 4 + pad, Wd = icons.length * cell + pad, Hd = pad + S.reduce((s, k) => s + 16 * k + pad, 0);
  const d = new Uint8Array(Wd * Hd * 4);
  for (let i = 0; i < Wd * Hd; i++) d.set([74, 58, 46, 255], i * 4); // the Wastes' fog colour behind
  icons.forEach((name, i) => {
    const t = K.decodePng(PNGS[name]);
    let y0 = pad;
    for (const k of S) {
      for (let y = 0; y < 16 * k; y++) for (let x = 0; x < 16 * k; x++) { const o = (Math.floor(y / k) * 16 + Math.floor(x / k)) * 4; if (t.rgba[o + 3]) d.set(t.rgba.subarray(o, o + 4), ((y0 + y) * Wd + pad + i * cell + x) * 4); }
      y0 += 16 * k + pad;
    }
  });
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, png(Wd, Hd, d));
}

if (require.main === module) {
  const problems = checks();
  console.log(problems.length ? problems.join('\n') : 'model checks: every plant model passes (bounds -16..32, one-axis 0/22.5/45 rotations, no zero-width or out-of-range UVs, faces textured, no z-fights against their own parts or the block they sit on)');
  scene = new K.Scene('clockwork-wastes-overgrowth');
  scene.own = { ...OWN };
  const counts = buildScene();
  console.log('models checked:', Object.keys(MODELS).length, 'counts', JSON.stringify(counts));
  scene.write(OUT);
  console.log(scene.report());
  iconSheet(path.join(REVIEW, 'icons.png'));
}
// the textures by name (their PNGs), and a model face's texture back to its name
const texName = (ref) => (ref === T.scorched ? null : Object.keys(T).find((k) => T[k] === ref));
module.exports = { MODELS, T, C, PNGS, PAL, texName, checks };
