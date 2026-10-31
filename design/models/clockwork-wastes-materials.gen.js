// Clockwork Wastes material sheet -> design/models/clockwork-wastes-materials.bbmodel (Generic Model preview).
// To design/briefs/clockwork-wastes-world.md (Palette, Motifs, The war's two sides, Builder Notes: "the look bible palette and
// motifs first, as a material sheet in Blockbench").
// node clockwork-wastes-materials.gen.js
// Row A: the uprising (void-alloy, seams, sigils, the lens check), each sigil dark then lit. Row B: the makers' materials.
// Row C: the ground and the resistance. Glow parts are their own shade:false cubes; the 'power' clip turns them on and off.
const path = require('path');
const { C, hash, textureSet, preview } = require('./process-box.lib.js');

const OUT = path.resolve(__dirname, 'clockwork-wastes-materials.bbmodel');

// ---------- palette (world brief) ----------
const VA = { hi: '#34304a', a: '#2a2838', b: '#232130', c: '#1d1c25', lo: '#16151c', seam: '#2e2a40', glass: '#2a2833', glint: '#4c475f' };
const STAR = { dim: '#3a3552', edge: '#7f76a8', core: '#b8b0d8' };
const AMBER = { core: '#d99a4a', edge: '#c47f3c' };
const ANDESITE = ['#5e615d', '#6b6e6a', '#767873'];
const STEEL = ['#2f343b', '#3c424a', '#4a5059'];
const IRON = ['#26292d', '#2f3338', '#3a3f45'];
const BRASS = ['#7e6832', '#8f7638', '#a3873f'];
// aged brass: the makers' and the resistance's old work (tarnished, no highlight, patina in the odd recess); mid brass is for new work
const BRASS_AGED = ['#6e5a36', '#7e6832', '#8f7638'];
const COPPER = ['#904931', '#9a5038', '#a75a40'];
const PATINA = ['#4f6b5e', '#5b7a6c'];
const WALNUT = ['#4e3626', '#634532', '#6f4d36'];
const RUST = ['#5a3523', '#6e4129', '#7f4d30'];
const SOOT = ['#1a1714', '#26211c', '#30291f'];
const GROUND = ['#6f6a50', '#7c7659', '#878063'];
// spring steel is the mod's Mainspring steel (Spring Engine coil, Mainspring item): edge, strip, highlight
const SPRING = ['#4a525a', '#6d7780', '#8a949c'];
const EYE = { glass: '#3a3430', glint: '#6b6e6a' };

const T = textureSet(); T.file = 'wastes_';
const mott = (p, seed, lo = 0.15, hi = 0.85) => (x, y) => { const r = hash(x, y, seed); return r < lo ? p[0] : r > hi ? p[2] : p[1]; };
const edge = (x, y) => Math.min(x, y, 15 - x, 15 - y);
const map = (rows) => (x, y) => (rows[y] || '')[x];

// void-alloy: crisp facets, a faint violet tone, no mottle.
// 'va' (small parts: brackets, hoods, frames, clamps, bezels, door panels, vault blades, the cable's knot) has a full facet
// frame: a 1 px lit edge (VA.hi) top and left, dark (VA.lo) bottom and right, the lit edge winning at the two mixed corners.
// (the old 'va_tile', a diagonal sheen with no frame, is replaced by the plain plate below: D75)
// Its field was three diagonal bands (a faint violet sheen top-left): vaSheen, the approved look in the mod today. D76 repaints
// it with the house grain (vaGrain: the base surface's three close shades, its own seed), so no diagonal sheen is left
// anywhere; its frame is kept. Previewed in design/texture-reviews/clockwork-wastes-va-parts/ and NOT in the mod until the user
// approves it: VA_GRAIN false paints the sheen (the mod's va.png and the sigils painted on it unchanged); true (or
// WASTES_VA_GRAIN=1 in the environment, for the review sheet) paints the grain everywhere 'va' is painted.
const VA_GRAIN = true || process.env.WASTES_VA_GRAIN === '1';
const vaFrame = (field) => (x, y) => (x === 0 || y === 0 ? VA.hi : x === 15 || y === 15 ? VA.lo : field(x, y));
const vaSheen = vaFrame((x, y) => (x + y < 11 ? VA.a : x + y < 20 ? VA.b : VA.c));
// D75: the base surface. The old diagonal sheen ('va_tile') read as stripes over a wall ("the base texture on the darker
// blocks reads as stripey"), so it's gone from every void-alloy block. Crisp cast metal with the house's gentle grain: three
// close shades with a faint violet tone, mostly the middle one, no stripes; a 1 px lit edge (VA.hi) top and left and a dark
// edge (VA.lo) bottom and right where a plate ends (clockwork-wastes-building-blocks.gen.js, approved).
const GRAIN = ['#232130', '#272534', '#2b2939'];
const grain = (seed) => (x, y) => { const r = hash(x, y, seed); return r < 0.12 ? GRAIN[0] : r > 0.93 ? GRAIN[2] : GRAIN[1]; };
// D76: the small parts' field on the house grain (seed 541: not the plate's or the field's pattern)
const vaGrain = vaFrame(grain(541));
const va = VA_GRAIN ? vaGrain : vaSheen;
const framed = (field, e = { t: 1, l: 1, b: 1, r: 1 }) => (x, y) => {
  if ((e.b && y === 15) || (e.r && x === 15)) return VA.lo;
  if ((e.t && y === 0) || (e.l && x === 0)) return VA.hi;
  return field(x, y);
};
// 'va_plain': one crisp plate a block, for shaped models (stairs, slabs, walls; D76: the machines and props take VA_BODY) and
// the textures painted on a plate (seamed, chipped, cracked, the chain, the cog)
const vaTile = framed(grain(501));
// the full-cube plates' surface (option C): an ashlar bond of 1 x 1 (s) and 2 x 1 (l then r) plates in offset courses, the
// face's plate picked by the block's position (the 'bond' states, set when the block is placed); tops and bottoms are 1 x 1
T.paint('va_bond_s', framed(grain(511)));
T.paint('va_bond_l', framed(grain(512), { t: 1, l: 1, b: 1, r: 0 }));
T.paint('va_bond_r', framed((x, y) => grain(512)(x + 16, y), { t: 1, l: 0, b: 1, r: 1 }));
// the plain field (no plate edges): the Pillar's chamfered faces, the Band's set-back margins
T.paint('va_field', grain(531));
// D76: the city's machines and props (alcove, door, vault, pylon, reactors, lift deck and guides, vent, clamps, the riser's
// guard...) are cast in one piece, so their bodies take the frameless field and no lit or dark line crosses one object at a
// block line. The framed plain plate (va_plain) stays on stairs, slabs and walls, where a block meets a block, and the bond
// on full plates. This one name switches every machine and prop (their generators and the exporter's particles use it).
const VA_BODY = 'va_field';
T.paint('va', va);
T.paint('va_plain', vaTile);
// 3: the war on void-alloy. It never rusts; it chips (a 2 px notch, dark inside, a bright lip) and cracks (a dark line with
// seam colour beside it). One mark per face, on outward faces only.
const CHIP = { '12,13': VA.lo, '13,13': VA.lo, '12,14': VA.lo, '13,14': VA.lo, '11,13': VA.hi, '11,14': VA.hi, '12,12': VA.hi, '13,12': VA.hi };
T.paint('va_chipped', (x, y) => CHIP[x + ',' + y] || vaTile(x, y));
const CRACK = [[4, 2], [4, 3], [5, 4], [5, 5], [6, 6], [6, 7], [7, 8], [8, 9], [8, 10]];
const CRACK_SIDE = [[5, 3], [6, 5], [7, 7], [9, 10]];
T.paint('va_cracked', (x, y) => (CRACK.some(([a, b]) => a === x && b === y) ? '#121118' : CRACK_SIDE.some(([a, b]) => a === x && b === y) ? VA.seam : vaTile(x, y)));

// the seam pattern of the Seamed Void-Alloy Plate: three tall panels under a top course; crossings are where the core shows
const SEAM = new Set(), CROSS = new Set();
for (let x = 1; x <= 14; x++) SEAM.add(x + ',2');
for (const sx of [5, 10]) for (let y = 2; y <= 15; y++) SEAM.add(sx + ',' + y);
CROSS.add('5,2'); CROSS.add('10,2');
T.paint('va_seamed', (x, y) => (SEAM.has(x + ',' + y) ? VA.seam : vaTile(x, y)));
T.paint('seam_lit', (x, y) => (CROSS.has(x + ',' + y) ? STAR.core : SEAM.has(x + ',' + y) ? STAR.edge : null));

// the broken chain: three links, the middle one sprung open at the top; '*' marks where links cross
const CHAIN = map(['', '', '', '', '',
  '.........##.....',
  '.####.##...####.',
  '#...#*....##...#',
  '#...##....*#...#',
  '.####.####.####.']);
const chainAt = (x, y) => { const k = CHAIN(x, y); return k === '#' || k === '*' ? k : null; };
T.paint('chain_dark', (x, y) => (chainAt(x, y) ? VA.seam : vaTile(x, y)));
T.paint('chain_lit', (x, y) => { const k = chainAt(x, y); return k === '*' ? STAR.core : k ? STAR.edge : null; });

// the lens-in-cog: an eight-tooth cog in facet tones with a lens where its axle should be
const polar = (x, y) => { const dx = x + 0.5 - 8, dy = y + 0.5 - 8; return [Math.hypot(dx, dy), Math.atan2(dy, dx)]; };
// eight teeth on the axes and diagonals; one tone per side (lit top-left, dark bottom-right) so the cog reads as a solid part
const tooth = (a) => { const s = Math.PI / 4, m = ((a % s) + s) % s; return Math.min(m, s - m) < 0.24; };
// a dark outline separates it from the plate, as a cast part set into it
const cogBody = (x, y) => { const [r, a] = polar(x, y); return (r >= 3.6 && r < 4.9) || (r >= 4.9 && r < 6.3 && tooth(a)); };
const cog = (x, y) => {
  const [r] = polar(x, y);
  if (r < 2.6) return x === 7 && y === 7 ? VA.glint : VA.glass;
  if (r < 3.6) return VA.lo;
  if (cogBody(x, y)) return x + y < 15 ? VA.hi : VA.seam;
  if ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => cogBody(x + dx, y + dy))) return VA.lo;
  return vaTile(x, y);
};
T.paint('cog', cog);
const lensLit = (g) => (x, y) => { const [r] = polar(x, y); if (r >= 2.6) return null; return x >= 7 && x <= 8 && y >= 7 && y <= 8 ? g.core : r > 1.5 ? g.edge : g.dim || g.edge; };
T.paint('cog_lens_lit', lensLit(STAR));
// the lamp lenses for the brightness check, each a 6 px disc on a 16 px face centred at (8, 8)
T.paint('lens_star', (x, y) => (x >= 7 && x <= 8 && y >= 7 && y <= 8 ? STAR.core : STAR.edge));
T.paint('lens_amber', (x, y) => (edge(x, y) < 2 || hash(x, y, 13) > 0.9 ? AMBER.edge : AMBER.core));
T.paint('seam_strip_lit', () => STAR.edge);
T.paint('seam_strip_dark', () => VA.seam);

// ---------- the makers' materials (Create, aged a step darker) ----------
T.paint('andesite', (x, y) => {
  if (edge(x, y) === 0) return ANDESITE[0];
  if ((x === 2 || x === 13) && (y === 2 || y === 13)) return ANDESITE[2];
  return mott(ANDESITE, 21, 0.12, 0.92)(x, y);
});
// dark steel: two bolt heads with rust running down from them, as runs, never whole faces
const RUNS = { 3: 8, 12: 11 };
T.paint('steel_rust', (x, y) => {
  if ((x === 3 || x === 12) && y === 3) return STEEL[2];
  if (RUNS[x] && y > 3 && y <= RUNS[x]) return y < RUNS[x] - 2 ? RUST[1] : RUST[0];
  if (RUNS[x] && y === 4) return RUST[2];
  return mott(STEEL, 22, 0.12, 0.9)(x, y);
});
// one cast iron for the whole mod: the Electric Lights' painters (light/iron.png, light/iron_shaft.png)
T.paint('iron', mott(IRON, 1, 0.12, 0.9));
T.paint('iron_shaft', (x, y) => (x % 2 === 0 ? '#2b2f33' : hash(x, y, 2) > 0.9 ? IRON[2] : '#33373c'));
T.paint('brass', mott(BRASS, 24, 0.2, 0.9));
const brassAged = (seed) => (x, y) => (hash(x, y, seed + 1) > 0.93 ? '#4f6b5e' : mott(BRASS_AGED, seed, 0.25, 0.9)(x, y));
T.paint('brass_aged', brassAged(90));
// Create's own copper on the whole part; patina creeps in from the joints, heaviest along the bottom where water sits
// and in the corners, thin along the top
T.paint('copper_patina', (x, y) => {
  const r = hash(x, y, 25), side = Math.min(x, 15 - x), corner = side + Math.min(y, 15 - y);
  const reach = y === 15 ? 0.3 : y >= 13 ? 0.65 : corner < 3 ? 0.4 : side === 0 && y > 5 ? 0.6 : y === 0 ? 0.85 : 1;
  if (r > reach) return r > 0.9 ? PATINA[1] : PATINA[0];
  return mott(COPPER, 26, 0.15, 0.85)(x, y);
});
T.paint('walnut', (x, y) => (y % 4 === 0 ? WALNUT[0] : (x + (y >> 2) * 5) % 9 === 0 && y % 4 === 2 ? WALNUT[0] : hash(x, y, 27) > 0.85 ? WALNUT[2] : WALNUT[1]));
// End Stone bricks, stained down: 8 x 4 bricks, every other course shifted; a step above the ground so walls don't out-shout
// what's set in them
const endbrick = (x, y) => {
  if (y % 4 === 3 || (x + ((y >> 2) % 2 ? 4 : 0)) % 8 === 7) return '#6c6750';
  return hash(x, y, 28) > 0.8 ? '#8d876a' : '#827d5f';
};
T.paint('endbrick', endbrick);
// soot: a scorch fan, darkest where it rose from
T.paint('soot', (x, y) => { const d = Math.hypot(x - 7.5, y - 17) / 18 + (hash(x, y, 29) - 0.5) * 0.18; return d < 0.55 ? SOOT[0] : d < 0.8 ? SOOT[1] : SOOT[2]; });
T.paint('rust', mott(RUST, 30, 0.25, 0.85));

// ---------- the ground ----------
const ground = (x, y) => { const r = hash(x, y, 31); return r < 0.1 ? GROUND[0] : r > 0.9 ? GROUND[2] : GROUND[1]; };
T.paint('scorched', ground);
T.paint('scorched_rust', (x, y) => { const d = Math.hypot(x - 6.5, y - 8.5) + (hash(x, y, 32) - 0.5) * 2; return d < 3.6 ? '#6e5a3e' : ground(x, y); });
const LITTER = map(['', '', '', '...........b....', '..........bb....', '', '', '..sss...........', '.....s..........', '......ss........', '', '', '.........b.b....']);
T.paint('scorched_litter', (x, y) => { const k = LITTER(x, y); return k === 's' ? STEEL[2] : k === 'b' ? BRASS[0] : ground(x, y); });

// ---------- the resistance ----------
// smooth strip steel: its flat top the strip tone with a few highlights, its thin edges darker. The dark between turns on the
// Mainspring item is the gap itself, so it's never painted on.
T.paint('spring_top', (x, y) => (hash(x, y, 33) > 0.8 ? SPRING[2] : SPRING[1]));
T.paint('spring_edge', () => SPRING[0]);
T.paint('glass_eye', (x, y) => (x === 6 && y === 6 ? EYE.glint : EYE.glass));
T.paint('key_hole', () => '#1a1714');

// ---------- geometry ----------
const box = (name, f, t, tex, o = {}) => C(name, f, t, typeof tex === 'string' ? { all: tex } : tex, o);
const SIDES = (tex) => ({ north: tex, south: tex, east: tex, west: tex, up: null, down: null });
// a face-only overlay 0.25 px proud of a full block on all four sides, for seams and sigils
const overlay = (name, tex, lit) => box(name, [-0.25, 0, -0.25], [16.25, 16, 16.25], SIDES(tex), lit ? { shade: false } : {});
function zdisc(name, cx, cy, z0, z1, w, tex, o = {}) {
  const h = w / 2, n = w >= 6 ? Math.round(w * 0.16 * 2) / 2 : 1;
  if (w <= 2) return [box(name, [cx - h, cy - h, z0], [cx + h, cy + h, z1], tex, o)];
  const out =[box(name + '_a', [cx - h, cy - h + n, z0], [cx + h, cy + h - n, z1], tex, o), box(name + '_b', [cx - h + n, cy - h, z0], [cx + h - n, cy + h, z1], tex, o)];
  if (w >= 6) { const m = h - n / 2; out.push(box(name + '_c', [cx - m, cy - m, z0], [cx + m, cy + m, z1], tex, o)); }
  return out;
}
// the stepped chamfer of a Void-Alloy Pillar (two 1 px steps on each vertical edge), one block tall, with a seam strip at each corner
function pillar(name, y0, lit) {
  const s = lit ? 'seam_strip_lit' : 'seam_strip_dark', o = lit ? { shade: false } : {};
  return [
    box(name + '_x', [0, y0, 2], [16, y0 + 16, 14], 'va_field'), box(name + '_m', [1, y0, 1], [15, y0 + 16, 15], 'va_field'), box(name + '_z', [2, y0, 0], [14, y0 + 16, 16], 'va_field'),
    box(name + '_s_nw', [1, y0, 0.75], [2, y0 + 16, 0.75], { north: s }, o), box(name + '_s_ne', [14, y0, 0.75], [15, y0 + 16, 0.75], { north: s }, o),
    box(name + '_s_sw', [1, y0, 15.25], [2, y0 + 16, 15.25], { south: s }, o), box(name + '_s_se', [14, y0, 15.25], [15, y0 + 16, 15.25], { south: s }, o),
  ];
}
// a void-alloy lamp hood over a 6 px lens, both facing north and south, on a plinth
function lensHood(name, lensTex) {
  return [
    box(name + '_plinth', [3, 0, 3], [13, 2, 13], 'va'),
    box(name + '_post', [7, 2, 7], [9, 4, 9], 'va'),
    box(name + '_hood', [3, 4, 4], [13, 14, 12], 'va'),
    box(name + '_brow', [3, 12, 3], [13, 14, 13], 'va'),
    ...zdisc(name + '_lens_n', 8, 8, 3.75, 4, 6, { north: lensTex, all: null }, { shade: false }),
    ...zdisc(name + '_lens_s', 8, 8, 12, 12.25, 6, { south: lensTex, all: null }, { shade: false }),
  ];
}
// the resistance's glass eye: a brass rim round warm dark glass on an andesite plate; never lit
function glassEye() {
  const rim = (z0, z1, side) => [box('rim_t_' + side, [4, 11, z0], [12, 12, z1], 'brass'), box('rim_b_' + side, [4, 4, z0], [12, 5, z1], 'brass'),
    box('rim_l_' + side, [4, 5, z0], [5, 11, z1], 'brass'), box('rim_r_' + side, [11, 5, z0], [12, 11, z1], 'brass')];
  return [box('eye_plate', [0, 0, 2], [16, 16, 14], 'andesite'),
    ...rim(1, 2, 'n'), ...zdisc('eye_glass_n', 8, 8, 1.5, 2, 6, { north: 'glass_eye', all: null }),
    ...rim(14, 15, 's'), ...zdisc('eye_glass_s', 8, 8, 14, 14.5, 6, { south: 'glass_eye', all: null })];
}
// the winding key, the resistance's emblem: a butterfly bow, a shaft, a square socket, standing on a steel foot
function windingKey() {
  return [box('key_foot', [4, 0, 5], [12, 1, 11], 'iron'),
    box('key_socket', [6, 1, 6], [10, 5, 10], { all: 'brass_aged', down: 'key_hole' }),
    box('key_shaft', [7, 5, 7], [9, 10, 9], 'brass_aged'),
    box('key_boss', [6.5, 10, 7], [9.5, 12, 9], 'brass_aged'),
    // the butterfly bow: one flat bow-tie plate, narrow at the boss and flaring to tall wing ends (a real clock key's grip)
    box('key_bow_mid', [6, 11, 7.5], [10, 14, 8.5], 'brass_aged'),
    box('key_wing_l', [3.5, 10, 7.5], [6, 15, 8.5], 'brass_aged'), box('key_wing_l_end', [2.5, 10.5, 7.5], [3.5, 14.5, 8.5], 'brass_aged'),
    box('key_wing_r', [10, 10, 7.5], [12.5, 15, 8.5], 'brass_aged'), box('key_wing_r_end', [12.5, 10.5, 7.5], [13.5, 14.5, 8.5], 'brass_aged')];
}
// a mainspring lying flat on the ground, half sprung: a square spiral of 1 px strip with 1 px gaps (the Mainspring item's
// drawing, in 3D), its outer end sprung loose and trailing off
function springCoil() {
  const out = [], faces = { all: 'spring_edge', up: 'spring_top', down: null };
  const dirs = [[1, 0], [0, 1], [-1, 0], [0, -1]];
  let x = 7, z = 7, len = 2, k = 0;
  for (let leg = 0; leg < 12; leg++) {
    const [dx, dz] = dirs[leg % 4], x1 = x + dx * len, z1 = z + dz * len;
    out.push(box('turn_' + leg, [Math.min(x, x1), 0, Math.min(z, z1)], [Math.max(x, x1) + 1, 1, Math.max(z, z1) + 1], faces));
    x = x1; z = z1; if (leg % 2 === 1) len += 2; k++;
  }
  // the loose end, lifted where it sprang free
  out.push(box('loose', [x, 0.5, z], [x + 1, 1.5, z + 4], faces, { rot: ['x', -22.5, [x, 1, z]] }));
  return out;
}
const block = (name, tex, extra = []) => [box(name, [0, 0, 0], [16, 16, 16], tex), ...extra];

// ---------- the scene ----------
const { cube, group, clip, write } = preview('clockwork_wastes_materials', { 'w:': T });
const outliner = [], lit = [];
const GAP = 22;
function place(name, cubes, col, row, litCubes = []) {
  const off = [col * GAP, 0, row * 64];
  const kids = cubes.map((c) => cube(c, off));
  if (litCubes.length) { lit.push('lit_' + name); kids.push(group('lit_' + name, [8, 0, 8], off, litCubes.map((c) => cube(c, off)))); }
  outliner.push(group(name, [8, 0, 8], off, kids));
}
// row A: the uprising
place('va_plate', block('va_plate', 'va_plain'), 0, 0);
place('va_seamed_dark', block('va_seamed_dark', 'va_seamed'), 1, 0);
place('va_seamed_lit', block('va_seamed_lit', 'va_seamed'), 2, 0, [overlay('seam_overlay', 'seam_lit', true)]);
place('pillar_dark', [...pillar('pd0', 0, false), ...pillar('pd1', 16, false)], 3, 0);
place('pillar_lit', [...pillar('pl0', 0, false).slice(0, 3), ...pillar('pl1', 16, false).slice(0, 3)], 4, 0, [...pillar('pl0', 0, true).slice(3), ...pillar('pl1', 16, true).slice(3)]);
place('chain_dark', block('chain_dark', 'chain_dark'), 5, 0);
place('chain_lit', block('chain_lit', 'chain_dark'), 6, 0, [overlay('chain_overlay', 'chain_lit', true)]);
place('cog_dark', block('cog_dark', 'cog'), 7, 0);
place('cog_lit', block('cog_lit', 'cog'), 8, 0, [overlay('cog_overlay', 'cog_lens_lit', true)]);
place('lens_starlight', lensHood('ls', 'lens_star'), 9, 0);
place('lens_amber', lensHood('la', 'lens_amber'), 10, 0);
// row B: the makers
['andesite', 'steel_rust', 'iron_shaft', 'brass', 'brass_aged', 'copper_patina', 'walnut', 'endbrick', 'rust', 'soot'].forEach((t, i) => place('m_' + t, block('m_' + t, t), i, 1));
// row C: the ground and the resistance
place('scorched', block('scorched', 'scorched'), 0, 2);
place('scorched_rust', block('scorched_rust', { all: 'scorched', up: 'scorched_rust' }), 1, 2);
place('scorched_litter', block('scorched_litter', { all: 'scorched', up: 'scorched_litter' }), 2, 2);
place('spring_steel', springCoil(), 4, 2);
place('glass_eye', glassEye(), 5, 2);
place('winding_key', windingKey(), 6, 2);

// 'power': the city dark, then fed (the lit overlays on), then dark again
const steps = Object.fromEntries(lit.map((g) => [g, { scale: [[0, [0, 0, 0]], [1, [1, 1, 1]], [3, [0, 0, 0]]] }]));
clip('power', 4, 'loop', {}, steps);
if (require.main === module) write(OUT, outliner);
// the palette, textures and helpers, for the other Wastes generators
module.exports = { VA_BODY, VA_GRAIN, vaSheen, vaGrain, GRAIN, grain, framed, pillar, BRASS_AGED, brassAged, vaTile, endbrick, T, VA, STAR, AMBER, ANDESITE, STEEL, IRON, BRASS, COPPER, PATINA, WALNUT, RUST, SOOT, GROUND, SPRING, EYE, mott, edge, map, va, box, zdisc, overlay, CHAIN, chainAt };
