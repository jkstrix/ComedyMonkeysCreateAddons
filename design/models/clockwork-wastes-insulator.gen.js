// Clockwork Wastes: Derelict Insulator and sky-cable spans preview -> design/models/clockwork-wastes-insulator.bbmodel
// (Generic Model preview, D61). To design/briefs/clockwork-wastes-blocks.md §4 (the cable's void-alloy insulators, spans
// between masts and towers, sagging, some snapped and hanging; the wake pulse bead) and the world brief (sky-cables from every
// district's Substation up lattice masts to the spire's crown, each crown arm ending in an insulator). Preview only.
// node clockwork-wastes-insulator.gen.js  (writes the preview; required, it exports the builders for the mod's exporter)
// The block, in three mounts (its model space: x 0 west -> 16 east, y up, z 0 north -> 16 south):
//   top     on top of a post or mast: a collar whose claws bite the makers' girder below, a pedestal, the stack
//   wall    on a wall (here the wall is east, x 16): a tall plate biting the masonry, an arm with a brace, the stack on its end
//   hanging under an arm or a gantry (the spire's crown arm ends): a socket, the stack upside down, the tie at its foot
// The stack (the hero): three chamfered void-alloy sheds on narrow necks, a tie knob on top; the upper neck is a hairline
// starlight ring that lights with its district. Spans: two patina strands with a hairline gap (the lit line runs in it),
// copper only where they rub on a tie; one span snapped and hanging. Clips: dormant, wake (the bead), awake.
const path = require('path');
const fs = require('fs');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const CITY = require('./clockwork-wastes-city.gen.js');
const { T, VA, STAR, COPPER, PATINA, box } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-insulator.bbmodel');
const GLOW = { shade: false };

// ---------- textures ----------
const patina = (x, y) => (hash(x, y, 55) > 0.55 ? PATINA[1] : PATINA[0]);
// worn: copper shows through the patina where the span rubs on its tie
const worn = (x, y) => (hash(x, y, 61) > 0.35 ? COPPER[hash(x, y, 62) > 0.5 ? 1 : 2] : patina(x, y));
T.paint('span_side', patina);
T.paint('span_side_worn', worn);
// the top face, mapped 3 texel rows across the 1.2 px strand pair: strand, the hairline gap, strand
T.paint('span_top', (x, y) => (y % 3 === 1 ? VA.lo : patina(x, y)));
T.paint('span_top_worn', (x, y) => (y % 3 === 1 ? VA.lo : worn(x, y)));
// the break: bright copper core where the strands parted
T.paint('span_break', (x, y) => (hash(x, y, 63) > 0.5 ? COPPER[2] : '#b26247'));
T.paint('flash', () => STAR.core);
T.paint('ring_lit', () => STAR.edge);
// the facet tone on each shed's drip lip and the tie knob's top, so the stepped chamfers catch the light
T.paint('va_facet', (x, y) => (hash(x, y, 64) > 0.8 ? VA.a : VA.hi));

// ---------- the stack ----------
// a chamfered square of width w (two crossed boxes, 0.5 clipped at each corner)
const cham = (name, cx, cz, y0, y1, w, tex = 'va', o = {}) => {
  const h = w / 2, c = w >= 3 ? 0.5 : 0.25;
  return [box(name + '_x', [cx - h, y0, cz - h + c], [cx + h, y1, cz + h - c], tex, o), box(name + '_z', [cx - h + c, y0, cz - h], [cx + h - c, y1, cz + h], tex, o)];
};
// a shed: a 5 px drip lip, a 4 px shoulder over it (so each shed reads with a chamfered top)
const shed = (name, cx, cz, y) => [...cham(name + '_lip', cx, cz, y, y + 0.75, 5, { all: 'va', up: 'va_facet' }), ...cham(name + '_sh', cx, cz, y + 0.75, y + 1.5, 4)];
// the hanging shed: the drip lip still at the bottom (a suspension disc's skirt faces down)
const SHED = 1.5, NECK = 1;
// upright stack from y0: shed, neck, shed, neck (the lit one), shed, tie neck, tie knob. Returns { body, ring, tie }
function stack(name, cx, cz, y0) {
  let y = y0;
  const body = [], ring = [];
  body.push(...shed(name + '_s1', cx, cz, y)); y += SHED;
  body.push(...cham(name + '_n1', cx, cz, y, y + NECK, 2.5)); y += NECK;
  body.push(...shed(name + '_s2', cx, cz, y)); y += SHED;
  body.push(...cham(name + '_n2', cx, cz, y, y + NECK, 2.5));
  ring.push(...neckRing(name + '_ring', cx, cz, y + 0.25, y + 0.75)); y += NECK;
  body.push(...shed(name + '_s3', cx, cz, y)); y += SHED;
  body.push(...cham(name + '_tn', cx, cz, y, y + 0.5, 2)); y += 0.5;
  body.push(...cham(name + '_tie', cx, cz, y, y + 1.5, 3, { all: 'va', up: 'va_facet' })); y += 1.5;
  return { body, ring, tie: [cx, y, cz] };
}
// hanging stack from yTop down: the same parts, the tie knob at the foot
function hangingStack(name, cx, cz, yTop) {
  let y = yTop;
  const body = [], ring = [];
  y -= SHED; body.push(...shed(name + '_s1', cx, cz, y));
  y -= NECK; body.push(...cham(name + '_n1', cx, cz, y, y + NECK, 2.5));
  ring.push(...neckRing(name + '_ring', cx, cz, y + 0.25, y + 0.75));
  y -= SHED; body.push(...shed(name + '_s2', cx, cz, y));
  y -= NECK; body.push(...cham(name + '_n2', cx, cz, y, y + NECK, 2.5));
  y -= SHED; body.push(...shed(name + '_s3', cx, cz, y));
  y -= 0.5; body.push(...cham(name + '_tn', cx, cz, y, y + 0.5, 2));
  y -= 1.5; body.push(...cham(name + '_tie', cx, cz, y, y + 1.5, 3));
  // the clevis: two prongs under the knob the span hangs from
  body.push(box(name + '_prong_w', [cx - 1.25, y - 1, cz - 0.5], [cx - 0.5, y, cz + 0.5], 'va'), box(name + '_prong_e', [cx + 0.5, y - 1, cz - 0.5], [cx + 1.25, y, cz + 0.5], 'va'));
  return { body, ring, tie: [cx, y - 0.6, cz] };
}
// the lit neck: a hairline ring 0.1 proud of the 2.5 px neck, its own shade:false faces
const neckRing = (name, cx, cz, y0, y1) => {
  const h = 1.35;
  return [box(name + '_n', [cx - 0.75, y0, cz - h], [cx + 0.75, y1, cz - h], { north: 'ring_lit' }, GLOW), box(name + '_s', [cx - 0.75, y0, cz + h], [cx + 0.75, y1, cz + h], { south: 'ring_lit' }, GLOW),
    box(name + '_w', [cx - h, y0, cz - 0.75], [cx - h, y1, cz + 0.75], { west: 'ring_lit' }, GLOW), box(name + '_e', [cx + h, y0, cz - 0.75], [cx + h, y1, cz + 0.75], { east: 'ring_lit' }, GLOW)];
};

// ---------- the three mounts ----------
// top: a 9 x 9 collar on the girder's top (an 8 x 8 Create Metal Girder, x 4-12), four claws reaching 3 px down its faces,
// each with a lip that bites into the lattice; a chamfered pedestal; the stack
function topMount() {
  const body = [...cham('collar', 8, 8, 0, 1.5, 9), ...cham('collar_top', 8, 8, 1.5, 2, 7)];
  for (const [dir, x0, z0, x1, z1, lx0, lz0, lx1, lz1] of [
    ['n', 7, 3.25, 9, 4, 7, 4, 9, 4.5], ['s', 7, 12, 9, 12.75, 7, 11.5, 9, 12],
    ['w', 3.25, 7, 4, 9, 4, 7, 4.5, 9], ['e', 12, 7, 12.75, 9, 11.5, 7, 12, 9]]) {
    body.push(box('claw_' + dir, [x0, -3, z0], [x1, 0, z1], 'va'), box('claw_lip_' + dir, [lx0, -3, lz0], [lx1, -2.25, lz1], 'va'));
  }
  body.push(...cham('pedestal', 8, 8, 2, 3.5, 4));
  const s = stack('stk', 8, 8, 3.5);
  return { body: [...body, ...s.body], ring: s.ring, tie: s.tie };
}
// wall (on the east wall): a 4 x 12 plate, chamfered ends, biting the masonry's joints top and bottom; an arm out to the
// west with a 45-degree brace under it; the stack standing on the arm's end
function wallMount() {
  const body = [box('plate', [15, 1, 6], [16, 13, 10], 'va'), box('plate_top', [15, 13, 6.5], [16, 13.5, 9.5], 'va'), box('plate_bot', [15, 0.5, 6.5], [16, 1, 9.5], 'va'),
    box('bite_top', [15.5, 12, 7], [16.75, 12.5, 9], 'va'), box('bite_bot', [15.5, 2, 7], [16.75, 2.5, 9], 'va'),
    box('arm', [5.5, 3.5, 7], [15, 5, 9], 'va'), box('arm_tip', [5, 3.75, 7.25], [5.5, 4.75, 8.75], 'va'),
    box('brace', [12, -0.75, 7.5], [13, 5.25, 8.5], 'va', { rot: ['z', 45, [12.5, 2.25, 8]] })];
  const s = stack('stk', 8, 8, 5);
  return { body: [...body, ...s.body], ring: s.ring, tie: s.tie };
}
// hanging (under the block above): a socket plate cast into the arm's underside, a stem, the stack hanging, the clevis at its foot
function hangingMount() {
  const body = [...cham('socket', 8, 8, 15, 16, 6), ...cham('stem', 8, 8, 13.5, 15, 2.5)];
  const s = hangingStack('stk', 8, 8, 13.5);
  return { body: [...body, ...s.body], ring: s.ring, tie: s.tie };
}

// the snapped span: a polyline of (s, y) points in px from its tie (s along its heading, level; y up), in the plane of
// its heading. The preview hangs it from the mast's top mount, heading south, down the mast.
const SNAP_PTS = [[0, 0], [1.5, -0.5], [3, -1.8], [4.3, -4], [5.2, -7], [5.5, -10], [5.6, -20], [5.6, -30], [5.5, -40], [5.6, -46], [6.4, -49], [7.6, -50], [8.6, -49.2]];

module.exports = { stack, hangingStack, neckRing, cham, shed, topMount, wallMount, hangingMount, SNAP_PTS, SHED, NECK };

// the preview scene, only when run directly (the mod's exporter takes the builders above)
if (require.main === module) {
// ---------- the scene ----------
const { cube, group, clip, write } = preview('clockwork_wastes_insulator', { 'w:': T });
const outliner = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const sub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));
const add = (g) => { outliner.push(g); return g; };
const plus = (a, b) => a.map((v, i) => v + b[i]);
const REST = {};

// the floor
{
  const floor = [];
  for (let x = -2; x <= 12; x++) for (let z = -7; z <= 6; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
  add(group('floor', [8, 0, 8], [0, 0, 0], floor));
}
// a Create Metal Girder column (its real textures; the city builds masts from them)
const GIRDER_UV = { north: [4, 0, 12, 16], south: [4, 0, 12, 16], east: [4, 0, 12, 16], west: [4, 0, 12, 16], up: [8, 0, 16, 8], down: [8, 0, 16, 8] };
const girder = (name, top) => [box(name, [4, 0, 4], [12, 16, 12], { all: 'create:block/girder_pole_side', up: top ? 'create:block/girder_pole' : null, down: null }, { uv: GIRDER_UV })];
// a void-alloy tower block (chamfered column, as the city preview's stand-in)
const towerBlock = (name, face = 'va_plain', top = false) => [box(name + '_x', [0, 0, 2], [16, 16, 14], { all: 'va_plain', up: top ? 'va_plain' : null, down: null }), box(name + '_m', [1, 0, 1], [15, 16, 15], { all: 'va_plain', up: top ? 'va_plain' : null, down: null }),
  box(name + '_z', [2, 0, 0], [14, 16, 16], { all: 'va_plain', west: face, up: top ? 'va_plain' : null, down: null })];

// 1. the Substation (the city's own), lever on its east side; the district end of the chain is its tray's east insulator
const SUB_TIE = [11, 34, 8];
{
  const s = CITY.substation(), off = at(0, 0, 0);
  add(group('substation', [8, 0, 8], off, [...put(s.body, off), sub('lever', [16, 20, 8], off, s.lever), sub('g_sub_seams', [8, 0, 1], off, s.seams),
    sub('g_sub_sigil', [8, 27, 1], off, s.sigil), sub('g_sub_flash', [17, 11, 8], off, s.flash), sub('g_sub_column', [8, CITY.COL.y0, 1.25], off, s.column)]));
}
// 2. the lattice mast: four girders, the top-mounted insulator on it
const MAST = [5, 0, 1], MAST_H = 4;
let MAST_TIE;
{
  const kids = [];
  for (let i = 0; i < MAST_H; i++) kids.push(...put(girder('girder_' + i, false), at(MAST[0], i, MAST[2])));
  const m = topMount(), off = at(MAST[0], MAST_H, MAST[2]);
  kids.push(group('ins_mast', [8, 0, 8], off, [...put(m.body, off), sub('g_ins_mast', [8, 8, 8], off, m.ring)]));
  MAST_TIE = plus(m.tie, off);
  add(group('mast', [8, 0, 8], at(...MAST), kids));
}
// 3. the tower: a 7-tall void-alloy column, the wall-mounted insulator on its west face 5 up (the mount is drawn with its
// wall to the east, so it sits in the cell west of the tower)
const TOWER = [10, 0, 0], TOWER_H = 7;
let WALL_TIE;
{
  const kids = [];
  for (let i = 0; i < TOWER_H; i++) kids.push(...put(towerBlock('tower_' + i, i === 2 ? 'va_cracked' : i === 4 ? 'va_chipped' : 'va_plain', i === TOWER_H - 1), at(TOWER[0], i, TOWER[2])));
  // a chamfered cap
  kids.push(...put([box('tower_cap', [3, 0, 3], [13, 1.5, 13], 'va'), box('tower_cap2', [5, 1.5, 5], [11, 2.5, 11], 'va')], at(TOWER[0], TOWER_H, TOWER[2])));
  const w = wallMount(), off = at(TOWER[0] - 1, 5, TOWER[2]);
  kids.push(group('ins_wall', [8, 0, 8], off, [...put(w.body, off), sub('g_ins_wall', [8, 8, 8], off, w.ring)]));
  WALL_TIE = plus(w.tie, off);
  add(group('tower', [8, 0, 8], at(...TOWER), kids));
}
// 4. a stub of one of the spire's crown arms (the spire stands off to the east), its end holding the hanging insulator
const CROWN = [8, 11, -6];
let CROWN_TIE;
{
  const off = at(CROWN[0], CROWN[1] + 1, CROWN[2]);
  // the arm: 6 px square, chamfered, reaching east 5 blocks out of the scene, a dark seam along its side, a chamfered tip
  const arm = [box('arm', [4, 0, 5], [80, 6, 11], 'va'), box('arm_ch_t', [4, 6, 6], [80, 6.5, 10], 'va'), box('arm_ch_b', [4, -0.5, 6], [80, 0, 10], 'va'),
    box('arm_tip', [2.5, 0.5, 6], [4, 5.5, 10], 'va'), box('arm_seam', [4, 3, 4.75], [80, 3.5, 4.75], { north: 'seam_strip_dark' })];
  const h = hangingMount(), hoff = at(CROWN[0], CROWN[1], CROWN[2]);
  add(group('crown_arm', [8, 0, 8], off, [...put(arm, off),
    group('ins_crown', [8, 0, 8], hoff, [...put(h.body, hoff), sub('g_ins_crown', [8, 8, 8], hoff, h.ring)])]));
  CROWN_TIE = plus(h.tie, hoff);
}

// ---------- spans ----------
// built along +x from A in a vertical plane, the group turned about y at A to B's heading; a sagging parabola (the Line Posts'
// shape). Each segment: one strand-pair cube (patina, the gap down its top; worn copper on the two segments at each tie) and a
// lit-line cube 0.3 wide in the gap. The lit line is cut into chunks that light behind the bead.
const CHUNKS = 10;
function span(name, A, B, sag, N = 36) {
  const dx = B[0] - A[0], dz = B[2] - A[2], L = Math.hypot(dx, dz), dy = B[1] - A[1];
  const P = (t) => [A[0] + L * t, A[1] + dy * t - sag * 4 * t * (1 - t), A[2]];
  const dark = [], chunks = Array.from({ length: CHUNKS }, () => []);
  for (let k = 0; k < N; k++) {
    const t0 = k / N, t1 = (k + 1) / N, p0 = P(t0), p1 = P(t1), c = P((t0 + t1) / 2);
    const len = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]), ang = Math.atan2(p1[1] - p0[1], p1[0] - p0[0]) * 180 / Math.PI;
    const w = k < 2 || k >= N - 2;
    const rot = { rot: ['z', ang, c] };
    dark.push(box(`${name}_${k}`, [c[0] - len / 2 - 0.05, c[1] - 0.4, A[2] - 0.6], [c[0] + len / 2 + 0.05, c[1] + 0.4, A[2] + 0.6],
      { all: w ? 'span_side_worn' : 'span_side', up: w ? 'span_top_worn' : 'span_top' }, { ...rot, uv: { up: [0, 0, 16, 3] } }));
    chunks[Math.floor(k * CHUNKS / N)].push(box(`${name}_g${k}`, [c[0] - len / 2 - 0.05, c[1] + 0.42, A[2] - 0.15], [c[0] + len / 2 + 0.05, c[1] + 0.42, A[2] + 0.15], { up: 'ring_lit' }, { ...GLOW, rot: ['z', ang, c] }));
  }
  const kids = [...dark.map((c) => cube(c, [0, 0, 0]))];
  chunks.forEach((list, i) => kids.push(group(`g_${name}_${i}`, P((i + 0.5) / CHUNKS), [0, 0, 0], list.map((c) => cube(c, [0, 0, 0])))));
  // the bead: a 3 px starlight bead, starting at A, moved along the span by the wake clip
  kids.push(group('bead_' + name, A, [0, 0, 0], [cube(box('bead_' + name, [A[0] - 1.5, A[1] - 0.25, A[2] - 0.7], [A[0] + 1.5, A[1] + 1.05, A[2] + 0.7], 'flash', GLOW), [0, 0, 0])]));
  add(group('span_' + name, A, [0, 0, 0], kids));
  REST['span_' + name] = [0, -Math.atan2(dz, dx) * 180 / Math.PI, 0];
  return { P, L, dy, A };
}
// the preview's group pivots are in Generic Model space; the span builder works in scene px, so its pivots go through toBB as
// every other group's do (offset [0,0,0])
const SPANS = {
  a: span('a', SUB_TIE, MAST_TIE, 7),
  b: span('b', MAST_TIE, WALL_TIE, 6),
  c: span('c', WALL_TIE, CROWN_TIE, 9, 44),
};
// the snapped span: from the mast's tie it goes over the shed's edge to the south and hangs down the mast, its end curled,
// bare copper where it broke. A polyline in the plane of its heading (south); an inner group swings it slightly in the wind.
{
  const A = MAST_TIE, pts = SNAP_PTS;
  const list = [];
  for (let k = 0; k < pts.length - 1; k++) {
    const [s0, y0] = pts[k], [s1, y1] = pts[k + 1], cx = A[0] + (s0 + s1) / 2, cy = A[1] + (y0 + y1) / 2, len = Math.hypot(s1 - s0, y1 - y0);
    const ang = Math.atan2(y1 - y0, s1 - s0) * 180 / Math.PI, last = k === pts.length - 2, w = k < 2;
    list.push(box('snap_' + k, [cx - len / 2 - 0.05, cy - 0.4, A[2] - 0.6], [cx + len / 2 + 0.05, cy + 0.4, A[2] + 0.6],
      { all: last ? 'span_break' : w ? 'span_side_worn' : 'span_side', up: last ? 'span_break' : w ? 'span_top_worn' : 'span_top' }, { rot: ['z', ang, [cx, cy, A[2]]], uv: { up: [0, 0, 16, 3] } }));
  }
  // the frayed end: the two strands parted, copper bright
  const e = pts[pts.length - 1];
  list.push(box('snap_fray_a', [A[0] + e[0], A[1] + e[1] - 0.2, A[2] - 0.6], [A[0] + e[0] + 1.2, A[1] + e[1] + 0.2, A[2] - 0.2], 'span_break', { rot: ['z', 30, [A[0] + e[0], A[1] + e[1], A[2]]] }),
    box('snap_fray_b', [A[0] + e[0], A[1] + e[1] - 0.2, A[2] + 0.2], [A[0] + e[0] + 1.0, A[1] + e[1] + 0.2, A[2] + 0.6], 'span_break', { rot: ['z', 5, [A[0] + e[0], A[1] + e[1], A[2]]] }));
  add(group('span_snapped', A, [0, 0, 0], [group('snap_swing', A, [0, 0, 0], list.map((c) => cube(c, [0, 0, 0])))]));
  REST.span_snapped = [0, -90, 0];
}

// ---------- the three mounts on their own, for a close look (front row, z 4) ----------
{
  const row = [];
  // top, on a single girder
  row.push(...put(girder('solo_girder', false), at(0, 0, 4)));
  const t = topMount(); row.push(group('solo_top', [8, 0, 8], at(0, 1, 4), [...put(t.body, at(0, 1, 4)), sub('g_solo_top', [8, 8, 8], at(0, 1, 4), t.ring)]));
  // wall, on an End Stone brick wall
  row.push(...put([box('solo_wall', [0, 0, 0], [16, 32, 16], 'endbrick')], at(3, 0, 4)));
  const w = wallMount(); row.push(group('solo_wall_ins', [8, 0, 8], at(2, 1, 4), [...put(w.body, at(2, 1, 4)), sub('g_solo_wall', [8, 8, 8], at(2, 1, 4), w.ring)]));
  // hanging, under a void-alloy beam on two posts
  row.push(...put([box('solo_beam', [-12, 0, 5], [28, 6, 11], 'va'), box('solo_post_w', [-12, -32, 5], [-6, 0, 11], 'va'), box('solo_post_e', [22, -32, 5], [28, 0, 11], 'va')], at(6, 2, 4)));
  const h = hangingMount(); row.push(group('solo_hang', [8, 0, 8], at(6, 1, 4), [...put(h.body, at(6, 1, 4)), sub('g_solo_hang', [8, 8, 8], at(6, 1, 4), h.ring)]));
  add(group('mounts', [8, 0, 8], at(0, 0, 4), row));
}

// ---------- clips ----------
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const SUB_GLOWS = ['g_sub_seams', 'g_sub_sigil', 'g_sub_column'];
const RINGS = ['g_ins_mast', 'g_ins_wall', 'g_ins_crown', 'g_solo_top', 'g_solo_wall', 'g_solo_hang'];
const LINES = ['a', 'b', 'c'].flatMap((n) => Array.from({ length: CHUNKS }, (_, i) => `g_${n}_${i}`));
const BEADS = ['bead_a', 'bead_b', 'bead_c'];
const LEVER_OPEN = -130;
const ease = (t, t0, t1) => (t <= t0 ? 0 : t >= t1 ? 1 : ((u) => u * u * (3 - 2 * u))((t - t0) / (t1 - t0)));
const lin = (t, t0, t1) => (t <= t0 ? 0 : t >= t1 ? 1 : (t - t0) / (t1 - t0));
const slam = (t, t0) => { if (t <= t0) return 0; const u = (t - t0) / 0.12; return u < 1 ? u * u : 1 - 0.12 * Math.exp(-(u - 1) * 1.6) * Math.abs(Math.sin((u - 1) * 2.2)); };
// the snapped end sways a little in the wind (a slow 4 s swing about the tie)
const sway = (t) => [2.5 * Math.sin(t * Math.PI / 2), 0, 0];
const all = (list, v) => Object.fromEntries(list.map((g) => [g, { scale: [[0, v]] }]));
clip('dormant', 4, 'loop', { lever: { rotation: () => [LEVER_OPEN, 0, 0] }, snap_swing: { rotation: sway } },
  { ...all([...SUB_GLOWS, 'g_sub_flash', ...RINGS, ...LINES, ...BEADS], HIDE) });
clip('awake', 4, 'loop', { lever: { rotation: () => [0, 0, 0] }, snap_swing: { rotation: sway } },
  { ...all([...SUB_GLOWS, ...RINGS, ...LINES], SHOW), ...all(['g_sub_flash', ...BEADS], HIDE) });
// wake: the column fills (0-2 s), the lever slams at 2.0 s; the bead leaves the tray at the slam and runs each span in 2 s
// (a: 2-4, b: 4-6, c: 6-8), the lit line filling in behind it a chunk at a time; each insulator's neck ring lights as the
// bead reaches its tie. Steady speed along a span (a bead doesn't ease), so it reads as current, not a thrown ball.
{
  const T0 = { a: 2.0, b: 4.0, c: 6.0 }, DUR = 2.0;
  const steps = {};
  steps.g_sub_column = { scale: [[0, [1, 0, 1]], ...[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((k) => [k * 0.2, [1, k / 10, 1]])] };
  steps.g_sub_flash = { scale: [[0, HIDE], [2.08, SHOW], [2.3, HIDE]] };
  for (const g of ['g_sub_seams', 'g_sub_sigil']) steps[g] = { scale: [[0, HIDE], [2.1, SHOW]] };
  for (const n of ['a', 'b', 'c']) {
    for (let i = 0; i < CHUNKS; i++) steps[`g_${n}_${i}`] = { scale: [[0, HIDE], [T0[n] + DUR * (i + 0.5) / CHUNKS, SHOW]] };
    steps['bead_' + n] = { scale: [[0, HIDE], [T0[n], SHOW], [T0[n] + DUR, HIDE]] };
  }
  steps.g_ins_mast = { scale: [[0, HIDE], [T0.b, SHOW]] };
  steps.g_ins_wall = { scale: [[0, HIDE], [T0.c, SHOW]] };
  steps.g_ins_crown = { scale: [[0, HIDE], [T0.c + DUR, SHOW]] };
  for (const g of ['g_solo_top', 'g_solo_wall', 'g_solo_hang']) steps[g] = { scale: [[0, HIDE], [T0.a + 0.4, SHOW]] };
  const tracks = { lever: { rotation: (t) => [LEVER_OPEN * (1 - slam(t, 2.0)), 0, 0] }, snap_swing: { rotation: sway } };
  for (const n of ['a', 'b', 'c']) {
    const S = SPANS[n];
    // the bead's place and slope in its span's own frame (the span group carries the heading)
    tracks['bead_' + n] = {
      position: (t) => { const u = lin(t, T0[n], T0[n] + DUR), p = S.P(u); return [p[0] - S.A[0], p[1] - S.A[1], 0]; },
      rotation: (t) => { const u = Math.min(0.999, Math.max(0.001, lin(t, T0[n], T0[n] + DUR))), p0 = S.P(u - 0.001), p1 = S.P(u + 0.001); return [0, 0, Math.atan2(p1[1] - p0[1], p1[0] - p0[0]) * 180 / Math.PI]; },
    };
  }
  clip('wake', 9, 'hold', tracks, steps);
}
write(OUT, outliner);
// rest rotations the preview library doesn't set: each span's heading
{
  const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
console.log('ties', { SUB_TIE, MAST_TIE, WALL_TIE, CROWN_TIE });
}
