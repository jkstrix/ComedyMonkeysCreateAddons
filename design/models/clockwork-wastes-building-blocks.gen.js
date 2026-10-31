// Clockwork Wastes: the building block set -> design/models/clockwork-wastes-building-blocks.bbmodel (Generic Model preview).
// Preview only: nothing here is in the mod until the user approves the look.
// node clockwork-wastes-building-blocks.gen.js   (writes the preview and runs the checks; required, it exports the builders)
// To design/briefs/clockwork-wastes-buildings.md ("The Void-Alloy Building Block Set", the makers' masonry set, Key-Marked
// Stone) and D73:
//   void-alloy: Stairs, Slab, Wall, Slotted Grille, Band, Clamp Bracket (wall strap and saddle), Scarred Plate (chipped,
//   cracked, heat); the makers: Stained End Stone Bricks, Cracked, Stairs, Slab, Wall; the resistance: Key-Marked Stone.
// The user's first look ("the base texture on the darker blocks reads as stripey, also walls don't have any texturing, large
// areas with a single block"): the diagonal va_tile sheen is gone from every new block. Three options for the void-alloy base
// surface, each on its own wall and tower corner (row D): A, 2 x 2-block cast panels (a parity state picks the quadrant);
// B, one crisp plate per block with weighted random variants (plain, plain, a fitted patch, a small vent, riveted); C, an
// ashlar bond of 1 x 1 and 2 x 1 plates in offset courses (picked by position).
// Row A shows the void-alloy set alone and row B the masonry set and the key stone (both seen from the north), row C the
// existing family (on the D75 base since the swap),
// then five vignettes from the brief's sketches: a slotted tower corner on a makers' plinth, a Stained End brick drum
// flat with its buttress, windows and corbel table, a snapped tower with its scarred plates and fallen top, a siege-works
// segment with the key stone, and the alcove wall's slab switchback.
// Block model space (x 0 west -> 16 east, y up, z 0 north -> 16 south). Every block is built as the vanilla-style model it
// will export as; the scene places them in a little block world so connections (walls, bands) and the position-picked
// textures come from real neighbours and positions. Glows are shade:false parts in lit_ groups; clips 'dormant' and 'awake'.
const path = require('path');
const fs = require('fs');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const CP = require('./clockwork-wastes-city-props.gen.js');
const S = require('./clockwork-wastes-salvage.gen.js');
const { T, VA, STAR, GROUND, box, grain, framed } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-building-blocks.bbmodel');
const GLOW = { shade: false };
const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
// the base option the vignettes are built in (the option walls in row D show all three)
const DEFAULT_OPT = 'C';

// ---------- textures: the void-alloy base ----------
// Crisp cast metal with the house's gentle grain: three close shades with a faint violet tone, mostly the middle one, no
// stripes; a 1 px lit edge (#34304a) top and left, a dark edge (#16151c) bottom and right where a plate ends.
// (D75: the grain, the plate frame, option C's bond textures va_bond_s/l/r, the plain plate va_plain and the plain field
// va_field now live in clockwork-wastes-materials.gen.js, the whole family's base surface)
// option A: one cast panel over 2 x 2 blocks; the quadrant textures vA_<row><col> (row 0 the upper, col 0 the left)
for (const r of [0, 1]) for (const c of [0, 1]) {
  T.paint(`vA_${r}${c}`, (x, y) => {
    const X = c * 16 + x, Y = r * 16 + y;
    if (X === 31 || Y === 31) return VA.lo;
    if (X === 0 || Y === 0) return VA.hi;
    return grain(500)(X, Y);
  });
}
// option B: a crisp plate per block, and its weighted variants
T.paint('vB_plain', framed(grain(501)));
T.paint('vB_plain2', framed(grain(502)));
// a fitted patch: a small plate let into the face, its own edges lit and dark
T.paint('vB_patch', framed((x, y) => {
  if (x >= 9 && x <= 13 && y >= 9 && y <= 12) return x === 13 || y === 12 ? VA.lo : x === 9 || y === 9 ? VA.hi : '#2b2939';
  return grain(503)(x, y);
}));
// a small vent: two cut slots, dark inside, the lower lip catching the light
T.paint('vB_vent', framed((x, y) => {
  if (x >= 5 && x <= 10 && (y === 9 || y === 11)) return '#121118';
  if (x >= 5 && x <= 10 && (y === 10 || y === 12)) return VA.hi;
  return grain(504)(x, y);
}));
// riveted (as asked; the world brief says the uprising never rivets: choice B0)
const RIVET = (x, y) => { for (const [a, b] of [[2, 2], [12, 2], [2, 12], [12, 12]]) { if (x === a && y === b) return '#3e3a55'; if ((x === a + 1 && y === b) || (x === a && y === b + 1)) return VA.hi; if (x === a + 1 && y === b + 1) return VA.lo; } return null; };
T.paint('vB_rivet', framed((x, y) => RIVET(x, y) || grain(505)(x, y)));
// option C (chosen, D75): ashlar plates, 1 x 1 (va_bond_s) and 2 x 1 (va_bond_l then va_bond_r), painted in the materials sheet
// shared by all three: the plain field (set-back margins, pillars), the band, its steps, the wall post's cap, the scars
T.paint('va_band', (x, y) => (y === 8 ? VA.seam : grain(521)(x, y)));
T.paint('va_step_hi', () => VA.hi);
T.paint('va_step_lo', () => VA.lo);
T.paint('va_reveal', (x, y) => (y === 2 ? VA.hi : VA.lo));
T.paint('va_post_top', (x, y) => {
  if (x < 4 || x > 11 || y < 4 || y > 11) return grain(532)(x, y);
  if (x === 11 || y === 11) return VA.lo;
  if (x === 4 || y === 4) return VA.hi;
  return grain(532)(x, y);
});
const plainB = framed(grain(501));
const CHIP = { '12,13': VA.lo, '13,13': VA.lo, '12,14': VA.lo, '13,14': VA.lo, '11,13': VA.hi, '11,14': VA.hi, '12,12': VA.hi, '13,12': VA.hi };
// the chipped and cracked faces are the materials sheet's va_chipped and va_cracked (the same marks, now on the plain plate)
const CRACK = [[4, 2], [4, 3], [5, 4], [5, 5], [6, 6], [6, 7], [7, 8], [8, 9], [8, 10]];
const CRACK_SIDE = [[5, 3], [6, 5], [7, 7], [9, 10]];
const has = (list, x, y) => list.some(([a, b]) => a === x && b === y);
// heat tint (visual language: fire on void-alloy shows as heat tint): warm over the lower face, three streaks, gone by row 6
// an ordered (Bayer) fade, so the tint thins out smoothly instead of a ragged fringe; the streaks taper to single pixels
const BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]];
const HEAT_STREAK = { 4: 7, 10: 9 };
T.paint('va_heat', (x, y) => {
  if (y === 15 || x === 15) return VA.lo;
  if (x > 0 && HEAT_STREAK[x] !== undefined && y >= HEAT_STREAK[x] && y < 15) return y < HEAT_STREAK[x] + 2 ? '#3a2e24' : '#4a3a2e';
  const f = y >= 12 ? 1 : y >= 6 ? (y - 5) / 7 : 0;
  if (x > 0 && (BAYER[y % 4][x % 4] + 0.5) / 16 < f) return '#3a2e24';
  return plainB(x, y);
});
T.paint('crack_glow', (x, y) => (has(CRACK_SIDE, x, y) ? STAR.dim : null));
T.paint('grille_glow', () => STAR.dim);
T.paint('void_dark', (x, y) => (hash(x, y, 402) > 0.8 ? '#16151c' : '#121118'));

// ---------- textures: the makers' masonry (Stained End Stone Bricks), aged a step darker ----------
// The sheet's brick layout (8 x 4 bricks, every other course shifted), the brief's hue, but aged darker than the sheet's
// (the user: "near vanilla's brightness"): bricks #6c6750 / #746f57, mortar #57533f (choice M1). Three weighted variants
// shade single bricks a step up or down (age), so a wall isn't one tile repeated; dust only on up faces.
const SEBC = { mortar: '#57533f', a: '#6c6750', b: '#746f57', dark: '#635e49', light: '#7b765b' };
const brickAt = (x, y) => { const c = y >> 2, off = c % 2 ? 4 : 0; return { c, i: Math.floor((x + off) / 8) % 2, mortar: y % 4 === 3 || (x + off) % 8 === 7 }; };
const sebPaint = (shade) => (x, y) => {
  const k = brickAt(x, y);
  if (k.mortar) return SEBC.mortar;
  const s = shade[k.c * 2 + k.i];
  if (s === 'd') return hash(x, y, 601) > 0.8 ? SEBC.a : SEBC.dark;
  if (s === 'l') return hash(x, y, 602) > 0.75 ? SEBC.b : SEBC.light;
  // a brick's top row a touch lighter, its lowest a touch darker: the face reads as laid bricks, not a print
  if (y % 4 === 0 && hash(x, y, 603) > 0.5) return SEBC.b;
  return hash(x, y, 604) > 0.8 ? SEBC.b : SEBC.a;
};
T.paint('seb_0', sebPaint('........'));
T.paint('seb_1', sebPaint('.d...l.d'));
T.paint('seb_2', sebPaint('l...d..d'));
// dust on up faces, drifting to one edge (the city's dust rule)
const DUST = ['#7c7659', '#878063'];
T.paint('seb_top', (x, y) => {
  const r = hash(x, y, 56), drift = y === 0 ? 0.25 : y === 1 && (x < 3 || x > 12) ? 0.4 : y === 2 && (x < 1 || x > 14) ? 0.6 : 1;
  return r > drift ? DUST[hash(x, y, 57) > 0.5 ? 1 : 0] : sebPaint('........')(x, y);
});
// Cracked: a settlement crack that steps down through the mortar joints (the way brick really cracks), splitting one brick
// on its way, and one brick's corner spalled off (the deeper stone, its broken edge lit). Cracked only at a cause.
const CRK = new Set(['7,0', '7,1', '7,2', '7,3', '8,3', '9,3', '10,3', '11,3', '11,4', '11,5', '11,6', '11,7', '12,7', '13,7', '14,7', '15,7',
  '15,8', '14,8', '14,9', '13,9', '13,10', '13,11', '12,11', '11,11', '10,11', '10,12', '9,12', '9,13', '9,14', '9,15']);
const CRK_WIDE = new Set(['11,5', '14,8', '13,10']);
const SPALL = new Set(['1,9', '2,9', '3,9', '1,10', '2,10', '1,8', '2,8']);
const SPALL_LIP = new Set(['4,9', '3,10', '3,8', '2,11']);
T.paint('seb_cracked', (x, y) => {
  const k = x + ',' + y;
  if (CRK.has(k)) return '#3a3729';
  if (CRK_WIDE.has((x - 1) + ',' + y)) return '#46432f';
  if (SPALL.has(k)) return hash(x, y, 605) > 0.5 ? '#4f4b39' : '#57533f';
  if (SPALL_LIP.has(k)) return '#827d5f';
  return sebPaint('..d.....')(x, y);
});
// Key-Marked Stone: the resistance's winding key (the approved butterfly bow, a boss, the shaft, the square socket) scratched
// into a Scorched End Stone face: the groove dark, scratch strokes running down-right inside it, a pale burr where the tool
// lifted the crust on the upper-left edges
const KEY = ['................',
  '.###........###.',
  '.####......####.',
  '.##############.',
  '.##############.',
  '.####......####.',
  '.###...##...###.',
  '......####......',
  '.......##.......',
  '.......##.......',
  '.......##.......',
  '.....######.....',
  '.....#....#.....',
  '.....#....#.....',
  '.....######.....',
  '................'];
const inKey = (x, y) => (KEY[y] || '')[x] === '#';
const ground = (x, y) => { const r = hash(x, y, 31); return r < 0.1 ? GROUND[0] : r > 0.9 ? GROUND[2] : GROUND[1]; };
T.paint('key_stone', (x, y) => {
  if (inKey(x, y)) return (x - y + 32) % 3 === 0 ? '#5f5a46' : '#4c4736';
  if ((inKey(x + 1, y + 1) || inKey(x, y + 1)) && !inKey(x - 1, y) && hash(x, y, 404) > 0.35) return '#968f70';
  return ground(x, y);
});
// the makers' windows, as the brief's states say: dark, dusty, mostly broken. An aged iron frame (four panes a block), the
// glass grimed dark with dust settled in its lower rows; broken panes are gone but for shards at the frame (cutout)
const IRONW = { frame: '#2f3338', lit: '#3a3f45', dark: '#24272b' };
const windowPaint = (broken) => (x, y) => {
  const fx = x === 0 || x === 15 || x === 7 || x === 8, fy = y === 0 || y === 15 || y === 7 || y === 8;
  if (fx || fy) return (x === 0 || y === 0) ? IRONW.lit : (x === 15 || y === 15) ? IRONW.dark : IRONW.frame;
  const pane = (x > 7 ? 1 : 0) + (y > 7 ? 2 : 0), lx = x > 7 ? x - 9 : x - 1, ly = y > 7 ? y - 9 : y - 1;
  if (broken.includes(pane)) {
    // shards: a jagged rim of glass left in the frame
    const edge = Math.min(lx, ly, 5 - lx, 5 - ly);
    return edge === 0 && hash(x, y, 610 + pane) > 0.45 ? '#2a2e2e' : null;
  }
  if (ly >= 4) return hash(x, y, 611) > 0.4 ? '#3d3e36' : '#34362f';
  return hash(x, y, 612) > 0.85 ? '#33393a' : '#282c2d';
};
T.paint('win_a', windowPaint([0, 3]));
T.paint('win_b', windowPaint([1, 2, 3]));
T.paint('win_c', windowPaint([0, 1, 2]));
// the dome's copper rib: aged, gone to a muted patina (choice M3), not vanilla's mint weathered copper
T.paint('rib_patina', (x, y) => { const r = hash(x, y, 620); return r < 0.2 ? '#435a50' : r > 0.85 ? '#56705f' : '#4c6457'; });

// ---------- cube transforms ----------
const FACE_ROT_Y = { north: 'east', east: 'south', south: 'west', west: 'north', up: 'up', down: 'down' };
const FACE_FLIP = { north: 'south', south: 'north', up: 'down', down: 'up', east: 'east', west: 'west' };
function remap(obj, map) {
  if (!obj) return obj;
  const out = {};
  for (const [k, v] of Object.entries(obj)) out[k === 'all' ? 'all' : map[k]] = v;
  return out;
}
function xform(cubes, pt, map) {
  return cubes.map((c) => {
    const a = pt(c.from), b = pt(c.to);
    return { ...c, from: [0, 1, 2].map((i) => Math.min(a[i], b[i])), to: [0, 1, 2].map((i) => Math.max(a[i], b[i])), faces: remap(c.faces, map), uv: remap(c.uv, map) };
  });
}
// turn about the block's vertical axis by 90-degree steps, as a blockstate's y rotation does (east -> south)
function rotY(cubes, deg) {
  let out = cubes;
  for (let k = 0; k < (((deg / 90) % 4) + 4) % 4; k++) out = xform(out, ([x, y, z]) => [16 - z, y, x], FACE_ROT_Y);
  return out;
}
// upside down, as a blockstate's x 180 does
const flipX = (cubes) => xform(cubes, ([x, y, z]) => [x, 16 - y, 16 - z], FACE_FLIP);
const YAW = { east: 0, south: 90, west: 180, north: 270 };
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]) }));
const named = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));
const mapTex = (cubes, from, to) => cubes.map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([k, v]) => [k, v === from ? to : v])) }));

// ---------- the base options: which texture a face shows, from the block's position ----------
// 'VA' in a builder's faces is the void-alloy base; 'SEB' the Stained End Stone Brick variant. Both are resolved per block.
const mod = (n, m) => ((n % m) + m) % m;
// a face's left-to-right coordinate as the viewer facing it sees it (texture u runs the same way)
const across = (dir, x, z) => (dir === 'south' ? x : dir === 'north' ? -x : dir === 'west' ? z : -z);
const OPTS = {
  // A: 2 x 2 panels. In game: a `panel` state (four values per axis pair) set from the position's parity on placement; the
  // model per state names each face's quadrant
  A: (x, y, z, dir) => (dir === 'up' || dir === 'down' ? `vA_${mod(z, 2)}${mod(x, 2)}` : `vA_${1 - mod(y, 2)}${mod(across(dir, x, z), 2)}`),
  // B: one plate a block; vanilla weighted random models (no state): plain 60, plain2 20, patch 8, vent 7, rivet 5
  B: (x, y, z, dir) => {
    if (dir === 'up' || dir === 'down') return 'vB_plain';
    const r = hash(x * 31 + z * 17, y * 13 + z, 777);
    return r < 0.6 ? 'vB_plain' : r < 0.8 ? 'vB_plain2' : r < 0.88 ? 'vB_patch' : r < 0.95 ? 'vB_vent' : 'vB_rivet';
  },
  // C: ashlar courses of 2 x 1 and 1 x 1 plates (L R S L R, repeating), each course shifted; in game a `bond` state per
  // horizontal axis set from position on placement
  C: (x, y, z, dir) => {
    if (dir === 'up' || dir === 'down') return 'va_bond_s';
    const P = ['L', 'R', 'S', 'L', 'R'], SH = [0, 3, 1, 4, 2];
    return 'va_bond_' + P[mod(across(dir, x, z) + SH[mod(y, 5)], 5)].toLowerCase();
  },
};
const sebVariant = (x, y, z) => { const r = hash(x * 7 + z * 3, y, 888); return r < 0.5 ? 'seb_0' : r < 0.75 ? 'seb_1' : 'seb_2'; };
function resolve(cubes, x, y, z, opt) {
  return cubes.map((c) => {
    const faces = {};
    for (const d of DIRS) {
      let t = d in c.faces ? c.faces[d] : c.faces.all;
      if (t === 'VA') t = OPTS[opt](x, y, z, d);
      else if (t === 'SEB') t = sebVariant(x, y, z);
      faces[d] = t || null;
    }
    return { ...c, faces };
  });
}

// ---------- the shaped blocks (vanilla templates, any texture) ----------
// Stairs: vanilla's own elements (block/stairs, inner_stairs, outer_stairs, all built facing east), turned by the blockstate.
function stairs(tex, facing = 'east', shape = 'straight', half = 'bottom') {
  const f = typeof tex === 'string' ? { all: tex } : tex;
  const els = [box('stair_base', [0, 0, 0], [16, 8, 16], f)];
  if (shape === 'straight') els.push(box('stair_step', [8, 8, 0], [16, 16, 16], f));
  if (shape === 'inner') els.push(box('stair_step', [8, 8, 0], [16, 16, 16], f), box('stair_step_in', [0, 8, 8], [8, 16, 16], f));
  if (shape === 'outer') els.push(box('stair_step', [8, 8, 8], [16, 16, 16], f));
  const placed = half === 'top' ? flipX(els) : els;
  return rotY(placed, YAW[facing]);
}
// Slab: block/slab, slab_top; a double slab is the full block (the Plate's model for void-alloy)
function slab(tex, type = 'bottom') {
  const f = typeof tex === 'string' ? { all: tex } : tex;
  if (type === 'double') return [box('slab_double', [0, 0, 0], [16, 16, 16], f)];
  return [box('slab_' + type, [0, type === 'top' ? 8 : 0, 0], [16, type === 'top' ? 16 : 8, 16], f)];
}
// Wall: template_wall_post ([4,0,4]-[12,16,12]), template_wall_side ([5,0,0]-[11,14,8], no south face) and
// template_wall_side_tall ([5,0,0]-[11,16,8]), each side turned to its direction as the vanilla blockstate does (uvlock).
function wall(tex, postTop, sides = {}, post = true) {
  const out = [];
  if (post) out.push(box('wall_post', [4, 0, 4], [12, 16, 12], { all: tex, up: postTop || tex }));
  const turn = { north: 0, east: 90, south: 180, west: 270 };
  for (const [dir, h] of Object.entries(sides)) {
    if (!h) continue;
    out.push(...rotY([box('wall_side_' + dir, [5, 0, 0], [11, h === 'tall' ? 16 : 14, 8], { all: tex, south: null })], turn[dir]));
  }
  return out;
}

// ---------- the void-alloy set ----------
// The shaped blocks carry the crisp plate (vB_plain) in every option: with uvlock, a stair or slab shows the part of one
// plate's frame its faces cover, a cast part (choice B3)
const VA_SHAPED = 'va_plain';
const vaStairs = (facing, shape, half) => stairs(VA_SHAPED, facing, shape, half);
const vaSlab = (type) => slab(type === 'double' ? 'VA' : VA_SHAPED, type);
const vaWall = (sides, post) => wall(VA_SHAPED, 'va_post_top', sides, post);
const plate = () => [box('plate', [0, 0, 0], [16, 16, 16], 'VA')];
// the Void-Alloy Pillar's shape (two 1 px chamfer steps on each vertical edge, a dark seam strip at its north and south
// corners), here in the new plain field
const vaPillar = () => [
  box('p_x', [0, 0, 2], [16, 16, 14], 'va_field'), box('p_m', [1, 0, 1], [15, 16, 15], 'va_field'), box('p_z', [2, 0, 0], [14, 16, 16], 'va_field'),
  box('p_s_nw', [1, 0, 0.75], [2, 16, 0.75], { north: 'seam_strip_dark' }), box('p_s_ne', [14, 0, 0.75], [15, 16, 0.75], { north: 'seam_strip_dark' }),
  box('p_s_sw', [1, 0, 15.25], [2, 16, 15.25], { south: 'seam_strip_dark' }), box('p_s_se', [14, 0, 15.25], [15, 16, 15.25], { south: 'seam_strip_dark' })];

// Slotted Grille, axis z (its slots face north and south; axis x is the same model turned 90). Three tall slots, 2 px wide
// at x 3-4, 7-8, 11-12, cut right through: 12 px tall at the faces (y 2-14), stepping in 1 px top and bottom inside (the
// chamfered ends, y 3-13). Field the base; every reveal VA.lo, the top step's face the VA.hi lip.
const SLOTS = [3, 7, 11];
function grille(axis = 'z') {
  const field = { all: 'VA' }, rev = 'va_reveal';
  const out = [];
  const bars = [[0, 3], [5, 7], [9, 11], [13, 16]];
  bars.forEach(([x0, x1], i) => out.push(box('bar_' + i, [x0, 0, 0], [x1, 16, 16], { ...field, ...(i > 0 ? { west: rev } : {}), ...(i < 3 ? { east: rev } : {}) })));
  for (const sx of SLOTS) {
    const n = 'slot' + sx;
    // the reveals' sill and soffit dark; the top chamfer step's faces the lit lip (VA.hi), the bottom step's in shadow
    out.push(box(n + '_sill', [sx, 0, 0], [sx + 2, 2, 16], { all: 'VA', east: null, west: null, up: 'va_step_lo' }));
    out.push(box(n + '_head', [sx, 14, 0], [sx + 2, 16, 16], { all: 'VA', east: null, west: null, down: 'va_step_lo' }));
    out.push(box(n + '_step_lo', [sx, 2, 1], [sx + 2, 3, 15], { north: 'va_step_lo', south: 'va_step_lo', up: 'va_step_lo', all: null }));
    out.push(box(n + '_step_hi', [sx, 13, 1], [sx + 2, 14, 15], { north: 'va_step_hi', south: 'va_step_hi', down: 'va_step_lo', all: null }));
  }
  return axis === 'x' ? rotY(out, 90) : out;
}
// lit (dim): a thin pane across each slot at mid-depth, both ways (choice B1)
function grilleGlow(axis = 'z') {
  const out = SLOTS.map((sx) => box('glow_' + sx, [sx, 3, 7.5], [sx + 2, 13, 8.5], { north: 'grille_glow', south: 'grille_glow', all: null }, GLOW));
  return axis === 'x' ? rotY(out, 90) : out;
}

// Void-Alloy Band: on every side face a 10 px band (y 3-13) at the face, the top 3 and bottom 3 px set back 1 px, the band's
// upper edge VA.hi and lower edge VA.lo (the chamfer steps), one seam along its middle (y 7-8). conn: { north, east, south,
// west } true where a neighbour block touches that side: there the set-back margin is filled flush (choice B2).
function band(conn = {}) {
  const out = [
    box('band', [0, 3, 0], [16, 13, 16], { all: 'va_band', up: 'va_step_hi', down: 'va_step_lo' }),
    box('margin_lo', [1, 0, 1], [15, 3, 15], 'va_field'),
    box('margin_hi', [1, 13, 1], [15, 16, 15], 'va_field'),
  ];
  const turn = { north: 0, east: 90, south: 180, west: 270 };
  for (const dir of Object.keys(turn)) if (conn[dir]) {
    out.push(...rotY([box('fill_lo_' + dir, [1, 0, 0], [15, 3, 1], { all: 'va_field', north: null }), box('fill_hi_' + dir, [1, 13, 0], [15, 16, 1], { all: 'va_field', north: null })], turn[dir]));
  }
  return out;
}
function bandGlow() {
  return [box('seam_n', [0, 7, -0.25], [16, 8, -0.25], { north: 'seam_strip_lit' }, GLOW), box('seam_s', [0, 7, 16.25], [16, 8, 16.25], { south: 'seam_strip_lit' }, GLOW),
    box('seam_w', [-0.25, 7, 0], [-0.25, 8, 16], { west: 'seam_strip_lit' }, GLOW), box('seam_e', [16.25, 7, 0], [16.25, 8, 16], { east: 'seam_strip_lit' }, GLOW)];
}

// Clamp Bracket, wall mount, facing north (it hangs on the face of the block to its south). A strap 6 px wide x 16 tall x 2
// deep; two jaws (10 wide x 3 tall x 4 deep), their wings 1 px off the stone with a 1 px lip turned back to it (the bite); a
// boss (4 x 4 x 3) as the load socket; one dark seam line down the strap. All `va` (a small part: the full facet frame).
function clampWall(facing = 'north') {
  const V = 'va';
  const jaw = (n, y0, y1, lipY0, lipY1) => [
    box(n + '_mid', [5, y0, 12], [11, y1, 16], V),
    box(n + '_wing_w', [3, y0, 12], [5, y1, 15], V), box(n + '_wing_e', [11, y0, 12], [13, y1, 15], V),
    box(n + '_lip_w', [3, lipY0, 15], [5, lipY1, 16], { all: V, south: null }), box(n + '_lip_e', [11, lipY0, 15], [13, lipY1, 16], { all: V, south: null })];
  const out = [
    box('strap', [5, 3, 14], [11, 13, 16], { all: V, south: null, up: null, down: null }),
    ...jaw('jaw_top', 13, 16, 15, 16), ...jaw('jaw_bot', 0, 3, 0, 1),
    box('boss', [6, 6, 11], [10, 10, 14], { all: V, south: null }),
    box('seam_hi', [7.5, 10, 13.75], [8.5, 13, 13.75], { north: 'seam_strip_dark' }), box('seam_lo', [7.5, 3, 13.75], [8.5, 6, 13.75], { north: 'seam_strip_dark' }),
  ];
  return rotY(out, { north: 0, east: 90, south: 180, west: 270 }[facing]);
}
// Clamp Bracket, top mount (a saddle) over the top of the block below, its jaws on that block's north and south faces
// (axis z). A 4 px top plate (6 wide) across the block, split at the cell edges; two jaws (10 wide, 2 thick) reaching 4 px
// down, 1 px off the faces with a lip turned back at the bottom; the boss on top; the seam along the plate.
function clampSaddle(axis = 'z') {
  const V = 'va';
  const out = [
    box('plate', [5, 0, 0], [11, 4, 16], { all: V, north: null, south: null }),
    box('plate_n', [5, 0, -1], [11, 4, 0], { all: V, north: null, south: null }), box('plate_s', [5, 0, 16], [11, 4, 17], { all: V, north: null, south: null }),
    box('jaw_n', [3, -4, -3], [13, 4, -1], V), box('jaw_s', [3, -4, 17], [13, 4, 19], V),
    box('lip_n', [3, -4, -1], [13, -3, 0], { all: V, south: null }), box('lip_s', [3, -4, 16], [13, -3, 17], { all: V, north: null }),
    box('boss', [6, 4, 6], [10, 7, 10], { all: V, down: null }),
    box('seam_n', [7.5, 4.25, -1], [8.5, 4.25, 6], { up: 'seam_strip_dark' }), box('seam_s', [7.5, 4.25, 10], [8.5, 4.25, 17], { up: 'seam_strip_dark' }),
  ];
  return axis === 'x' ? rotY(out, 90) : out;
}

// Scarred Void-Alloy Plate: a full block, its `facing` face scarred (chipped | cracked | heat), the rest the base
const SCAR_TEX = { chipped: 'va_chipped', cracked: 'va_cracked', heat: 'va_heat' };
function scarred(scar, facing = 'north') {
  return [box('scarred_' + scar, [0, 0, 0], [16, 16, 16], { all: 'VA', [facing]: SCAR_TEX[scar] })];
}
function scarredGlow(scar, facing = 'north') {
  if (scar !== 'cracked') return [];
  return rotY([box('crack_glow', [0, 0, -0.25], [16, 16, -0.25], { north: 'crack_glow' }, GLOW)], { north: 0, east: 90, south: 180, west: 270 }[facing]);
}

// ---------- the makers' masonry set (Stained End Stone Bricks) and the key stone ----------
const SEB = { all: 'SEB', up: 'seb_top' };
const stainedBricks = () => [box('stained_bricks', [0, 0, 0], [16, 16, 16], SEB)];
const crackedStainedBricks = () => [box('cracked_stained_bricks', [0, 0, 0], [16, 16, 16], { all: 'seb_cracked', up: 'seb_top' })];
const stainedStairs = (facing, shape, half) => stairs(SEB, facing, shape, half);
const stainedSlab = (type) => slab(SEB, type);
const stainedWall = (sides, post) => wall('SEB', 'seb_top', sides, post);
const keyStone = (facing = 'north') => [box('key_stone', [0, 0, 0], [16, 16, 16], { all: 'scorched', [facing]: 'key_stone' })];

module.exports = { stairs, slab, wall, vaStairs, vaSlab, vaWall, plate, vaPillar, grille, grilleGlow, band, bandGlow, clampWall, clampSaddle, scarred, scarredGlow,
  stainedBricks, crackedStainedBricks, stainedStairs, stainedSlab, stainedWall, keyStone, rotY, flipX, resolve, OPTS, sebVariant, SLOTS };

// ---------- the scene ----------
if (require.main === module) {
const { cube, group, clip, write } = preview('clockwork_wastes_building_blocks', { 'w:': T });
const outliner = [], LIT = [], WORLD = [], REST = {};
const B = 16;
const at = (bx, by, bz) => [bx * B, by * B, bz * B];

// borrowed makers' blocks (references in game; embedded in the preview)
const MK = {
  deepslateBrick: 'create:block/palettes/stone_types/brick/deepslate_cut_brick',
  deepslatePolished: 'create:block/palettes/stone_types/polished/deepslate_cut_polished',
  crackedDeepslate: 'minecraft:block/cracked_deepslate_bricks',
  casing: 'create:block/andesite_casing',
  cobbledDeepslate: 'minecraft:block/cobbled_deepslate',
  darkOak: 'minecraft:block/dark_oak_planks',
  endStoneBricks: 'minecraft:block/end_stone_bricks',
};
const full = (tex) => () => [box('block', [0, 0, 0], [16, 16, 16], tex)];

// A tiny block world for one exhibit: set(x, y, z, block) where block = { make(nb) -> cubes, glow?(nb) -> cubes, solid }.
// emit() adds the exhibit at block position `origin`, its base textures resolved by `opt` and each block's position.
function world(name, origin, opt = DEFAULT_OPT) {
  const m = new Map();
  const key = (x, y, z) => `${x},${y},${z}`;
  const api = {
    set(x, y, z, b) { m.set(key(x, y, z), b); return api; },
    get: (x, y, z) => m.get(key(x, y, z)),
    emit(o = {}) {
      const kids = [], glow = [];
      for (const [k, b] of m) {
        const [x, y, z] = k.split(',').map(Number);
        const nb = (dx, dy, dz) => m.get(key(x + dx, y + dy, z + dz));
        const ax = origin[0] + x, ay = origin[1] + y, az = origin[2] + z;
        const off = at(ax, ay, az);
        const prefix = `${name}_${x}_${y}_${z}_`;
        const cubes = resolve(named(b.make(nb), prefix), ax, ay, az, opt);
        kids.push(...cubes.map((c) => cube(c, off)));
        if (!o.skipCheck) WORLD.push(...shift(cubes, [ax * B, ay * B, az * B]));
        if (b.glow && !o.unlit) {
          const g = resolve(named(b.glow(nb), prefix), ax, ay, az, opt);
          glow.push(...g.map((c) => cube(c, off)));
          if (!o.skipCheck) WORLD.push(...shift(g, [ax * B, ay * B, az * B]));
        }
      }
      if (glow.length) { LIT.push('lit_' + name); kids.push(group('lit_' + name, [8, 0, 8], at(...origin), glow)); }
      const g = group(name, o.pivot || [8, 0, 8], at(...origin), kids);
      outliner.push(g);
      return g;
    },
  };
  return api;
}
const solid = (make, glow) => ({ make: () => make(), glow: glow && (() => glow()), solid: true });
const shaped = (make) => ({ make: () => make(), solid: false });
const SOLID = (b) => !!(b && b.solid);
const K = {
  plate: () => solid(plate),
  pillar: () => solid(vaPillar),
  band: () => ({ make: (nb) => band({ north: SOLID(nb(0, 0, -1)), south: SOLID(nb(0, 0, 1)), west: SOLID(nb(-1, 0, 0)), east: SOLID(nb(1, 0, 0)) }), glow: () => bandGlow(), solid: true }),
  grille: (axis) => ({ make: () => grille(axis), glow: () => grilleGlow(axis), solid: true }),
  scarred: (scar, facing) => ({ make: () => scarred(scar, facing), glow: scar === 'cracked' ? () => scarredGlow(scar, facing) : undefined, solid: true }),
  stairs: (facing, shape, half) => shaped(() => vaStairs(facing, shape, half)),
  slab: (type) => (type === 'double' ? solid(() => vaSlab('double')) : shaped(() => vaSlab(type))),
  // a wall block: its sides from its neighbours, as vanilla does (walls, solid blocks); post unless it's a straight run
  wall: (tex = 'va') => ({ make: (nb) => {
    const isWall = (b) => b && b.wall;
    const up = nb(0, 1, 0);
    const conn = { north: nb(0, 0, -1), south: nb(0, 0, 1), west: nb(-1, 0, 0), east: nb(1, 0, 0) };
    const sides = {};
    for (const [d, b] of Object.entries(conn)) if (isWall(b) || SOLID(b)) sides[d] = up && (isWall(up) || SOLID(up)) ? 'tall' : 'low';
    const straight = (sides.north && sides.south && !sides.east && !sides.west) || (sides.east && sides.west && !sides.north && !sides.south);
    const post = !straight || !!(up && isWall(up));
    return tex === 'va' ? vaWall(sides, post) : stainedWall(sides, post);
  }, wall: true, solid: false }),
  clampWall: (facing) => shaped(() => clampWall(facing)),
  clampSaddle: (axis) => shaped(() => clampSaddle(axis)),
  mk: (tex) => solid(full(tex)),
  mkStairs: (tex, facing, shape, half) => shaped(() => stairs(tex, facing, shape, half)),
  mkSlab: (tex, type) => shaped(() => slab(tex, type)),
  seb: () => solid(stainedBricks),
  sebCracked: () => solid(crackedStainedBricks),
  sebStairs: (facing, shape, half) => shaped(() => stainedStairs(facing, shape, half)),
  sebSlab: (type) => shaped(() => stainedSlab(type)),
  key: (facing) => solid(() => keyStone(facing)),
  fence: () => shaped(() => [box('fence_post', [6, 0, 6], [10, 16, 10], MK.darkOak)]),
  // a makers' window (2 x 2 panes a block, mostly broken) in the middle of its cell, the dark of the hall behind it
  window: (v) => solid(() => [box('window', [0, 0, 7], [16, 16, 9], { north: v, south: v, all: null }), box('window_back', [0, 0, 0.5], [16, 16, 0.5], { south: 'void_dark' })]),
  // the Charging Alcove (D72) facing south, on the plain plate (D75: the base change reaches it too)
  alcove: () => ({ make: () => rotY([...CP.alcoveBase(), ...CP.shelf()], 180), glow: () => rotY(S.alcoveLit(), 180), solid: true }),
  occupied: () => ({ make: () => [], solid: true }),
};

// a makers' plinth under a footprint (x0..x1, z0..z1): a body course of cut deepslate bricks, a dressed cap course of
// polished cut deepslate, a ground step of the plinth stone's stairs 1 proud all round (right way up, step outward);
// bites: the cap stones under clamp jaws, cracked
function plinth(w, x0, x1, z0, z1, bites = []) {
  for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) {
    w.set(x, 0, z, K.mk(MK.deepslateBrick));
    w.set(x, 1, z, bites.some(([bx, bz]) => bx === x && bz === z) ? K.mk(MK.crackedDeepslate) : K.mk(MK.deepslatePolished));
  }
  const st = (x, z, f, s = 'straight') => w.set(x, 0, z, K.mkStairs(MK.deepslateBrick, f, s, 'bottom'));
  for (let x = x0; x <= x1; x++) { st(x, z1 + 1, 'north'); st(x, z0 - 1, 'south'); }
  for (let z = z0; z <= z1; z++) { st(x0 - 1, z, 'east'); st(x1 + 1, z, 'west'); }
  st(x0 - 1, z0 - 1, 'east', 'outer'); st(x1 + 1, z0 - 1, 'south', 'outer'); st(x1 + 1, z1 + 1, 'west', 'outer'); st(x0 - 1, z1 + 1, 'north', 'outer');
}

// the slotted tower corner (vignette 1, and each option's exhibit): a 3 x 3 slotted tower (Pillar corners, Plate faces, a
// Grille run 1 x 3 on the south face then on the east face, Band rings at y 5 and y 9 running round the corners, a chamfer
// course of stairs sloping in, a slab cap), stood on a 5 x 5 makers' plinth (body, cap, ground step), clamped at its foot by
// wall straps whose lower jaws bite the ledge (the cap stones under them cracked). Worn: one chip per outward face. On its
// east face a walkway deck at the lower band (a slab deck, a plate kerb on the front lane carried on corbels of upside-down
// stairs) with a Void-Alloy Wall parapet. Optionally, a stub of the old makers' wall with a saddle clamp over its top.
function towerCorner(name, origin, opt, stub) {
  const w = world(name, origin, opt);
  plinth(w, 0, 4, 0, 4, [[2, 4], [4, 2]]);
  for (let y = 2; y <= 9; y++) for (let x = 1; x <= 3; x++) for (let z = 1; z <= 3; z++) {
    const corner = (x === 1 || x === 3) && (z === 1 || z === 3);
    let b;
    if (y === 5 || y === 9) b = K.band();
    else if (corner) b = K.pillar();
    else if (x === 2 && z === 3 && y <= 4) b = K.grille('z');
    else if (x === 3 && z === 2 && y >= 6 && y <= 8) b = K.grille('x');
    else b = K.plate();
    w.set(x, y, z, b);
  }
  w.set(2, 7, 3, K.scarred('chipped', 'south')).set(3, 3, 2, K.scarred('chipped', 'east'));
  w.set(2, 10, 2, K.plate());
  w.set(2, 10, 1, K.stairs('south')).set(2, 10, 3, K.stairs('north')).set(1, 10, 2, K.stairs('east')).set(3, 10, 2, K.stairs('west'));
  // outer stairs: vanilla's outer step sits in the facing's quadrant (east: south-east); each faces the tower's middle
  w.set(1, 10, 1, K.stairs('east', 'outer')).set(3, 10, 1, K.stairs('south', 'outer')).set(3, 10, 3, K.stairs('west', 'outer')).set(1, 10, 3, K.stairs('north', 'outer'));
  w.set(2, 11, 2, K.slab('bottom'));
  w.set(2, 2, 4, K.clampWall('south')).set(4, 2, 2, K.clampWall('east'));
  for (let z = 0; z <= 4; z++) {
    w.set(4, 4, z, K.slab('top'));
    w.set(5, 4, z, K.plate());
    if (z % 2 === 0) w.set(5, 3, z, K.stairs('west', 'straight', 'top'));
    w.set(5, 5, z, K.wall());
  }
  if (stub) {
    for (const x of [-5, -4, -3]) w.set(x, 0, 2, K.mk(MK.deepslateBrick));
    w.set(-5, 1, 2, K.mk(MK.deepslateBrick)).set(-4, 1, 2, K.mk(MK.crackedDeepslate));
    w.set(-4, 2, 2, K.clampSaddle('z'));
  }
  w.emit();
}
// an option's wall, 8 wide x 6 tall over a makers' plinth: Pillar pilasters at the ends, a Band course at y 4 (rings round
// the pilasters), two Grille runs in the upper storey, a chamfer course of stairs sloping back, foot clamps biting the
// ledge. Its plate fields (6 x 2 below, 4 columns of 3 above) are where the base option shows.
function optionWall(name, origin, opt) {
  const w = world(name, origin, opt);
  plinth(w, 0, 7, 0, 1, [[2, 1], [5, 1]]);
  for (let y = 2; y <= 7; y++) for (let x = 0; x <= 7; x++) {
    let b;
    if (y === 4) b = K.band();
    else if (x === 0 || x === 7) b = K.pillar();
    else if ((x === 2 || x === 5) && y >= 5) b = K.grille('z');
    else b = K.plate();
    w.set(x, y, 0, b);
  }
  for (let x = 0; x <= 7; x++) w.set(x, 8, 0, K.stairs('north'));
  w.set(2, 2, 1, K.clampWall('south')).set(5, 2, 1, K.clampWall('south'));
  w.emit();
}

// ---------- floor ----------
{
  const floor = [];
  const tiles = new Set();
  const area = (x0, x1, z0, z1) => { for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) tiles.add(x + ',' + z); };
  area(-2, 56, -2, 12); area(-7, 72, 13, 43);
  for (const t of tiles) {
    const [x, z] = t.split(',').map(Number);
    floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 11 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
  }
  outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
}

// ---------- row A (z 0): the void-alloy set, each block alone ----------
{
  let x = 0;
  const one = (name, b) => { const w = world('a_' + name, [x, 0, 0]); w.set(0, 0, 0, b); w.emit(); x += 2; };
  one('stairs_straight', K.stairs('north', 'straight', 'bottom'));
  one('stairs_inner', K.stairs('north', 'inner', 'bottom'));
  one('stairs_outer', K.stairs('north', 'outer', 'bottom'));
  one('stairs_top', K.stairs('north', 'straight', 'top'));
  one('slab_bottom', K.slab('bottom'));
  one('slab_top', K.slab('top'));
  one('slab_double', K.slab('double'));
  one('wall_post', K.wall());
  { const w = world('a_wall_run', [x, 0, 0]); w.set(0, 0, 0, K.wall()).set(1, 0, 0, K.wall()).set(2, 0, 0, K.wall()).set(2, 0, 1, K.wall()); w.emit(); x += 4; }
  { const w = world('a_wall_tall', [x, 0, 0]); w.set(0, 0, 0, K.wall()).set(1, 0, 0, K.wall()).set(1, 1, 0, K.wall()).set(2, 0, 0, K.wall()); w.emit(); x += 4; }
  one('grille', K.grille('z'));
  one('band', K.band());
  { const w = world('a_band_run', [x, 0, 0]); w.set(0, 0, 0, K.band()).set(1, 0, 0, K.band()).set(2, 0, 0, K.band()); w.emit(); x += 4; }
  // clamps: always on cracked stone
  { const w = world('a_clamp_wall', [x, 0, 0]); w.set(0, 0, 1, K.mk(MK.crackedDeepslate)).set(0, 0, 0, K.clampWall('north')); w.emit(); x += 2; }
  { const w = world('a_clamp_saddle', [x, 0, 0]); w.set(0, 0, 0, K.mk(MK.crackedDeepslate)).set(0, 1, 0, K.clampSaddle('z')); w.emit(); x += 2; }
  one('scarred_chipped', K.scarred('chipped', 'north'));
  one('scarred_cracked', K.scarred('cracked', 'north'));
  one('scarred_heat', K.scarred('heat', 'north'));
}

// ---------- row B (z 5): the makers' masonry set and the resistance's key stone ----------
{
  let x = 0;
  const one = (name, b) => { const w = world('b_' + name, [x, 0, 5]); w.set(0, 0, 0, b); w.emit(); x += 2; };
  one('vanilla_end_stone_bricks', K.mk(MK.endStoneBricks));
  one('stained_bricks', K.seb());
  one('cracked_stained_bricks', K.sebCracked());
  one('stained_stairs_straight', K.sebStairs('north', 'straight', 'bottom'));
  one('stained_stairs_inner', K.sebStairs('north', 'inner', 'bottom'));
  one('stained_stairs_outer', K.sebStairs('north', 'outer', 'bottom'));
  one('stained_stairs_top', K.sebStairs('north', 'straight', 'top'));
  one('stained_slab_bottom', K.sebSlab('bottom'));
  one('stained_slab_top', K.sebSlab('top'));
  one('stained_wall_post', K.wall('seb'));
  { const w = world('b_stained_wall_run', [x, 0, 5]); w.set(0, 0, 0, K.wall('seb')).set(1, 0, 0, K.wall('seb')).set(2, 0, 0, K.wall('seb')).set(2, 0, 1, K.wall('seb')); w.emit(); x += 4; }
  // a stained brick panel: four wide, three high, the variants by position, one cracked under a clamp (its cause)
  { const w = world('b_stained_panel', [x, 0, 5]); for (let i = 0; i < 4; i++) for (let j = 0; j < 3; j++) w.set(i, j, 0, i === 2 && j === 2 ? K.sebCracked() : K.seb()); w.set(2, 2, -1, K.clampWall('north')); w.emit(); x += 5; }
  one('key_stone', K.key('north'));
  one('scorched_end_stone', K.mk('scorched'));
}

// ---------- row C (z 9): the existing family on the D75 base, and a corner of the new set ----------
{
  const t = (name, x, make, glow) => { const w = world('c_today_' + name, [x, 0, 9]); w.set(0, 0, 0, { make, glow, solid: true }); w.emit({ skipCheck: name === 'alcove' }); };
  t('plate', 0, () => [box('plate', [0, 0, 0], [16, 16, 16], 'VA')]);
  t('seamed_plate', 2, () => [box('seamed', [0, 0, 0], [16, 16, 16], 'va_seamed')], () => [M.overlay('seam_overlay', 'seam_lit', true)]);
  { const w = world('c_today_pillar', [4, 0, 9]); w.set(0, 0, 0, solid(() => M.pillar('p', 0, false))).set(0, 1, 0, solid(() => M.pillar('p', 0, false))); w.emit(); }
  t('alcove', 6, () => [...CP.alcoveBase(), ...CP.shelf()], () => S.alcoveLit());
  // a short wall corner of the new set: the convex corner at the south-east; arms run west and north
  const wc = world('c_wall_corner', [9, 0, 9]);
  for (let y = 0; y < 4; y++) {
    const kind = y === 2 ? K.band : null;
    wc.set(3, y, 2, kind ? kind() : K.pillar());
    for (let i = 0; i <= 2; i++) wc.set(i, y, 2, kind ? kind() : i === 1 && y < 2 ? K.grille('z') : K.plate());
    for (let j = 0; j <= 1; j++) wc.set(3, y, j, kind ? kind() : j === 1 && y < 2 ? K.grille('x') : K.plate());
  }
  for (let i = 0; i <= 2; i++) wc.set(i, 4, 2, K.stairs('north'));
  wc.set(3, 4, 2, K.stairs('west', 'outer'));
  for (let j = 0; j <= 1; j++) wc.set(3, 4, j, K.stairs('west'));
  wc.emit();
}

// ---------- vignette 1 (z 15): the slotted tower corner on its makers' plinth, with the old wall stub and its saddle ----------
towerCorner('v1_tower', [2, 0, 15], DEFAULT_OPT, true);

// ---------- vignette 2 (z 15): a Stained End brick drum flat and its buttress ----------
// One flat of the heart drum (2 thick) and its buttress (2 wide, 2 proud, stair set-offs at y 5 and y 10), the two-course
// plinth of cut deepslate bricks with a stair water table (the buttress's stepping out with it), a sill string course at
// y 3 and a lintel band at y 9 (cut deepslate bricks, the Quarters' dressings), two tall makers' windows (2 x 4, mostly
// broken glass) round-headed with an arch of upside-down stained stairs, each in a panel recessed 1; a corbel table at y 12
// (upside-down cut deepslate brick stairs on alternate blocks, slabs between) carrying the Stained End brick parapet. The
// rib that landed on this buttress snapped (an aged patina stub) and broke the parapet next to it: the gap, cracked bricks
// round it (the only cracked bricks: their cause), the fallen parapet's wall, slab and stair at the drum's foot. The brick
// variants (age) by position; dust on every up face; no soot (the heart is intact, brief type 9).
{
  const w = world('v2_drum', [12, 0, 15]);
  const XL = 2, XR = 12, WIN = [4, 9];
  const inPanel = (x, y) => WIN.some((wx) => x >= wx - 1 && x <= wx + 2) && y >= 4 && y <= 8;
  const isWin = (x, y) => WIN.some((wx) => x >= wx && x <= wx + 1) && y >= 4 && y <= 7;
  const CRACKED = ['4,11,1', '5,12,1', '4,12,1', '6,12,1'];
  for (let x = 0; x <= XR; x++) {
    for (let y = 0; y <= 1; y++) for (let z = 0; z <= 2; z++) w.set(x, y, z, K.mk(MK.deepslateBrick));
    if (x >= XL) w.set(x, 2, 2, K.mkStairs(MK.deepslateBrick, 'north', 'straight', 'bottom'));
    for (let y = 2; y <= 12; y++) for (let z = 0; z <= 1; z++) {
      if (x >= XL && z === 1 && (y === 3 || y === 9)) { w.set(x, y, z, K.mk(MK.deepslateBrick)); continue; }
      if (x >= XL && z === 1 && inPanel(x, y)) continue;
      if (z === 0 && isWin(x, y)) { w.set(x, y, z, K.window(['win_a', 'win_b', 'win_c'][(x + y * 3) % 3])); continue; }
      w.set(x, y, z, CRACKED.includes(`${x},${y},${z}`) ? K.sebCracked() : K.seb());
    }
    if (x >= XL) {
      const gap = x === 4 || x === 5;
      if (x !== 5) w.set(x, 12, 2, x % 2 === 0 ? K.mkStairs(MK.deepslateBrick, 'north', 'straight', 'top') : K.mkSlab(MK.deepslateBrick, 'top'));
      if (!gap) w.set(x, 13, 2, K.wall('seb'));
    }
  }
  // the windows' round heads: upside-down stairs, their low halves at the jambs
  for (const wx of WIN) w.set(wx, 8, 0, K.sebStairs('west', 'straight', 'top')).set(wx + 1, 8, 0, K.sebStairs('east', 'straight', 'top'));
  // the dark of the hall behind the window heads (inside the drum)
  for (const wx of WIN) for (const x of [wx, wx + 1]) w.set(x, 8, -1, shaped(() => [box('hall_dark', [0, 0, 15.5], [16, 16, 15.5], { south: 'void_dark' })]));
  // the buttress: x 0-1, 2 proud (z 2-3) to the set-off at y 5, 1 proud (z 2) to y 10, then flush; its own plinth and
  // water table stepping out with it
  for (let x = 0; x <= 1; x++) {
    for (let y = 0; y <= 1; y++) for (let z = 3; z <= 4; z++) w.set(x, y, z, K.mk(MK.deepslateBrick));
    w.set(x, 2, 4, K.mkStairs(MK.deepslateBrick, 'north', 'straight', 'bottom'));
    for (let y = 2; y <= 4; y++) { w.set(x, y, 2, K.seb()); w.set(x, y, 3, K.seb()); }
    w.set(x, 5, 2, K.seb()).set(x, 5, 3, K.sebStairs('north', 'straight', 'bottom'));
    for (let y = 6; y <= 9; y++) w.set(x, y, 2, K.seb());
    w.set(x, 10, 2, K.sebStairs('north', 'straight', 'bottom'));
    w.set(x, 13, 0, K.seb()).set(x, 13, 1, K.seb());
  }
  // the rib's foot on the buttress top: aged patina copper, 1 wide x 2 deep, snapped two blocks up
  w.set(0, 14, 0, K.mk('rib_patina')).set(0, 14, 1, K.mk('rib_patina')).set(0, 15, 1, K.mk('rib_patina'));
  // what fell: the parapet's own wall, a slab and a stair at the foot under the gap (about a third of what's gone)
  w.set(4, 0, 3, K.sebSlab('bottom')).set(5, 0, 3, K.sebStairs('west', 'straight', 'bottom')).set(4, 0, 4, K.wall('seb')).set(6, 0, 4, K.seb());
  w.emit();
}

// ---------- vignette 3 (z 15): a Scarred Plate run on a snapped tower, its top fallen toward the heart (west) ----------
// The tower on its plinth, snapped on a stepped diagonal (1 per block) falling from y 10 at the west to y 7 at the
// south-east; the top two blocks of each outward face's middle column along the break are cracked Scarred Plates (the run);
// heat tint on the plates above the outward grilles (the strikes that took it). The fallen top (its band ring, a grille, the
// chamfer and cap) lies to the west on its side, unlit (choice B5).
{
  const w = world('v3_snapped', [38, 0, 15]);
  plinth(w, 0, 4, 0, 4, [[2, 4]]);
  w.set(2, 2, 4, K.clampWall('south'));
  const top = (x, z) => 10 - (x - 1) - (z === 3 ? 1 : 0);
  for (let x = 1; x <= 3; x++) for (let z = 1; z <= 3; z++) {
    const h = top(x, z);
    for (let y = 2; y <= h; y++) {
      const corner = (x === 1 || x === 3) && (z === 1 || z === 3);
      const outS = z === 3, outE = x === 3, atBreak = y >= h - 1;
      let b;
      if (y === 5 || y === 9) b = K.band();
      else if (atBreak && (outS || outE) && !corner) b = K.scarred('cracked', outS ? 'south' : 'east');
      else if (corner) b = K.pillar();
      else if (x === 2 && z === 3 && y <= 4) b = K.grille('z');
      else if (x === 3 && z === 2 && y <= 4) b = K.grille('x');
      else if (x === 2 && z === 3 && y === 6) b = K.scarred('heat', 'south');
      else if (x === 3 && z === 2 && y === 6) b = K.scarred('heat', 'east');
      else b = K.plate();
      w.set(x, y, z, b);
    }
  }
  w.emit();
  const f = world('v3_fallen_top', [33, 0, 16]);
  for (let x = 0; x <= 2; x++) for (let z = 0; z <= 2; z++) {
    const corner = (x === 0 || x === 2) && (z === 0 || z === 2);
    f.set(x, 0, z, K.band());
    for (let y = 1; y <= 2; y++) f.set(x, y, z, corner ? K.pillar() : x === 1 && z === 2 ? K.grille('z') : K.plate());
  }
  f.set(1, 3, 1, K.plate());
  f.set(1, 3, 0, K.stairs('south')).set(1, 3, 2, K.stairs('north')).set(0, 3, 1, K.stairs('east')).set(2, 3, 1, K.stairs('west'));
  f.set(0, 3, 0, K.stairs('east', 'outer')).set(2, 3, 0, K.stairs('south', 'outer')).set(2, 3, 2, K.stairs('west', 'outer')).set(0, 3, 2, K.stairs('north', 'outer'));
  f.set(1, 4, 1, K.slab('bottom'));
  f.emit({ skipCheck: true, unlit: true, pivot: [0, 0, 24] });
  REST.v3_fallen_top = [0, 0, 90];
}

// ---------- vignette 4 (z 15): a siege-works segment with the Key-Marked Stone ----------
// The resistance's cover (makers' materials, never lit, no void-alloy): stacked andesite casing 2 high facing the city
// (north), walnut posts every 3 standing 1 above it, rubble of cobbled deepslate and scorched End Stone in the fill, a
// walnut slab firing step behind it (outside, south), the key stone in the outer face, one per segment; the segment turns
// by a stepped 45-degree joint at its east end.
{
  const w = world('v4_siege', [46, 0, 16]);
  for (let x = 0; x < 7; x++) for (let y = 0; y <= 1; y++) {
    const rubble = (x === 2 && y === 0) || (x === 5 && y === 1);
    w.set(x, y, 1, rubble ? K.mk(x === 2 ? MK.cobbledDeepslate : 'scorched') : K.mk(MK.casing));
  }
  w.set(4, 1, 1, K.key('south'));
  for (const x of [0, 3, 6]) for (let y = 0; y <= 2; y++) w.set(x, y, 2, K.fence());
  for (const x of [1, 2, 4, 5]) w.set(x, 0, 2, K.mkSlab(MK.darkOak, 'bottom'));
  w.set(7, 0, 0, K.mk(MK.casing)).set(7, 1, 0, K.mk(MK.casing)).set(8, 0, -1, K.mk(MK.casing)).set(8, 1, -1, K.mk(MK.cobbledDeepslate));
  w.set(7, 0, 1, K.mk(MK.casing));
  w.emit();
}

// ---------- vignette 5 (z 15): the alcove wall's slab switchback ----------
// The alcove wall's honeycomb (Charging Alcoves alternating with Pillars, each tier offset by one; a Band at each tier's
// head; a chamfer course on top) and the ramp they used instead of stairs: a 2-wide Void-Alloy Slab ramp, half a block a
// block, one flight a tier. Flight 1 runs east along the wall on a solid plate body (as the city's ramp is built) to a
// landing at the tier-2 floor; flight 2 runs back west on the outer lanes, carried on Void-Alloy Wall legs, to the tier-3
// walkway (a plate deck on corbels, its parapet). Flight 2 and the landing carry the parapet: Void-Alloy Wall on a kerb of
// top slabs, so it stands flush (choice B4).
{
  const w = world('v5_switchback', [58, 0, 15]);
  const XA = -2, XB = 9;
  for (let x = XA; x <= XB; x++) {
    for (const t of [0, 1]) {
      const y0 = t * 4;
      if (mod(x + t, 2) === 0) { w.set(x, y0, 0, K.alcove()); w.set(x, y0 + 1, 0, K.occupied()).set(x, y0 + 2, 0, K.occupied()); }
      else for (let y = y0; y <= y0 + 2; y++) w.set(x, y, 0, K.pillar());
      w.set(x, y0 + 3, 0, K.band());
    }
    w.set(x, 8, 0, K.stairs('north'));
  }
  for (let i = 0; i < 8; i++) {
    const halves = i + 1, fullN = Math.floor(halves / 2);
    for (let z = 1; z <= 2; z++) {
      for (let y = 0; y < fullN; y++) w.set(i, y, z, K.plate());
      if (halves % 2 === 1) w.set(i, fullN, z, K.slab('bottom'));
    }
  }
  for (let x = 8; x <= 9; x++) for (let z = 1; z <= 4; z++) for (let y = 0; y <= 3; y++) w.set(x, y, z, K.plate());
  for (let z = 1; z <= 4; z++) w.set(9, 4, z, K.wall());
  w.set(8, 3, 5, K.slab('top')).set(9, 3, 5, K.slab('top')).set(8, 4, 5, K.wall()).set(9, 4, 5, K.wall());
  for (let i = 0; i < 8; i++) {
    const x = 7 - i, halves = 9 + i, cell = Math.floor(halves / 2), top = halves % 2 === 0;
    for (let z = 3; z <= 4; z++) w.set(x, top ? cell - 1 : cell, z, K.slab(top ? 'top' : 'bottom'));
    if (!top && (x === 7 || x === 3)) for (let y = 0; y < cell; y++) w.set(x, y, 4, K.wall());
    const kc = top ? cell - 1 : cell;
    w.set(x, kc, 5, K.slab('top'));
    w.set(x, kc + 1, 5, K.wall());
  }
  for (let x = XA; x <= -1; x++) {
    for (let z = 1; z <= 4; z++) w.set(x, 7, z, K.plate());
    w.set(x, 6, 4, K.stairs('north', 'straight', 'top'));
    w.set(x, 8, 4, K.wall());
  }
  w.set(XA, 8, 3, K.wall()).set(XA, 8, 2, K.wall()).set(XA, 8, 1, K.wall());
  w.emit();
}

// ---------- row D (z 34): the three base options, each on its own wall and tower corner ----------
['A', 'B', 'C'].forEach((opt, k) => {
  const X = k * 24;
  optionWall('opt_' + opt + '_wall', [X, 0, 37], opt);
  towerCorner('opt_' + opt + '_tower', [X + 12, 0, 35], opt, false);
});

// ---------- clips ----------
const all = (v) => Object.fromEntries(LIT.map((g) => [g, { scale: [[0, v]] }]));
clip('dormant', 2, 'loop', {}, all([0, 0, 0]));
clip('awake', 2, 'loop', {}, all([1, 1, 1]));
write(OUT, outliner);
{
  const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}

// ---------- checks ----------
// z-fights: zfight-check.js's rule (same direction, closer than MIN, overlapping) over the whole scene at once, neighbouring
// blocks included, bucketed by plane; MIN 0.25 px, the house's spacing for overlays
{
  const MIN = 0.25;
  const AX = { north: [2, 'from'], south: [2, 'to'], west: [0, 'from'], east: [0, 'to'], down: [1, 'from'], up: [1, 'to'] };
  const buckets = new Map();
  for (const c of WORLD) {
    if (c.rot && c.rot[1]) continue;
    for (const [dir, [ax, end]] of Object.entries(AX)) {
      if (!c.faces[dir]) continue;
      const others = [0, 1, 2].filter((a) => a !== ax);
      const f = { name: c.name, dir, plane: c[end][ax], rect: others.map((a) => [c.from[a], c.to[a]]) };
      const k = dir + Math.floor(f.plane);
      (buckets.get(k) || buckets.set(k, []).get(k)).push(f);
    }
  }
  const ov = (a, b) => { const w = Math.min(a.rect[0][1], b.rect[0][1]) - Math.max(a.rect[0][0], b.rect[0][0]); const h = Math.min(a.rect[1][1], b.rect[1][1]) - Math.max(a.rect[1][0], b.rect[1][0]); return w > 0.05 && h > 0.05 ? w * h : 0; };
  const problems = [], template = [];
  const TPL = /(wall_post|wall_side_[a-z]+|_p_[xmz])$/;
  for (const [k, list] of buckets) {
    const dir = k.replace(/-?\d+$/, ''), base = Number(k.slice(dir.length));
    const near = [...list, ...(buckets.get(dir + (base + 1)) || [])];
    for (let i = 0; i < list.length; i++) for (let j = 0; j < near.length; j++) {
      const a = list[i], b = near[j];
      if (j < list.length && j <= i) continue;
      const d = Math.abs(a.plane - b.plane);
      if (d >= MIN) continue;
      const area = ov(a, b);
      if (area <= 0.02) continue;
      // vanilla's own templates (wall post and sides, the Pillar's crossed boxes) overlap their top and bottom faces on the
      // block's boundary planes (cullface up/down, hidden by any block above or below), and a wall's two low sides overlap
      // their tops inside its post
      const boundary = (a.dir === 'up' || a.dir === 'down') && d === 0 && a.plane % 16 === 0;
      const sameWall = /wall_side_[a-z]+$/.test(a.name) && /wall_side_[a-z]+$/.test(b.name) && a.name.replace(/_[a-z]+$/, '') === b.name.replace(/_[a-z]+$/, '');
      if ((boundary && TPL.test(a.name) && TPL.test(b.name)) || sameWall) { template.push(a.name); continue; }
      problems.push(`${a.name} ${a.dir} vs ${b.name} (${d.toFixed(2)} px apart, ${area.toFixed(1)} px²)`);
    }
  }
  for (const p of problems.slice(0, 40)) console.log('z-fight:', p);
  console.log(`(${template.length} template overlaps, vanilla's own: wall and pillar tops/bottoms on block boundaries, low wall sides inside the post)`);
  console.log(problems.length ? `${problems.length} z-fights` : `no z-fights: every same-facing face pair at least ${MIN} px apart (${WORLD.length} cubes)`);
}
// block-model export limits, per block model
{
  const issues = [];
  const models = {
    stairs_straight: vaStairs('east', 'straight'), stairs_inner: vaStairs('east', 'inner'), stairs_outer: vaStairs('east', 'outer'),
    slab_bottom: vaSlab('bottom'), slab_top: vaSlab('top'), wall_post: vaWall({}, true), wall_side: vaWall({ north: 'low' }, false), wall_side_tall: vaWall({ north: 'tall' }, false),
    plate: plate(), pillar: vaPillar(), grille: grille('z'), grille_glow: grilleGlow('z'), band: band({}), band_fill_east: band({ east: true }).slice(3), band_glow: bandGlow(),
    clamp_wall: clampWall('north'), clamp_saddle: clampSaddle('z'),
    scarred_chipped: scarred('chipped'), scarred_cracked: scarred('cracked'), scarred_glow: scarredGlow('cracked'), scarred_heat: scarred('heat'),
    stained_bricks: stainedBricks(), cracked_stained_bricks: crackedStainedBricks(), stained_stairs: stainedStairs('east'), stained_slab: stainedSlab('bottom'), stained_wall_post: stainedWall({}, true), key_stone: keyStone(),
  };
  const { fitUv } = require('./process-box.lib.js');
  for (const [name, els] of Object.entries(models)) for (const c of els) {
    const faces = DIRS.filter((d) => (d in c.faces ? c.faces[d] : c.faces.all));
    if (!faces.length) issues.push(`${name}:${c.name} has no faces`);
    for (const v of [...c.from, ...c.to]) if (v < -16 || v > 32) issues.push(`${name}:${c.name} out of -16..32`);
    if (c.rot && ![0, 22.5, -22.5, 45, -45].includes(c.rot[1])) issues.push(`${name}:${c.name} rotation ${c.rot[1]}`);
    for (const d of faces) {
      const uv = (c.uv && c.uv[d]) || fitUv(d, c.from, c.to);
      const w = Math.abs(uv[2] - uv[0]), h = Math.abs(uv[3] - uv[1]);
      const ax = { north: [0, 1], south: [0, 1], east: [2, 1], west: [2, 1], up: [0, 2], down: [0, 2] }[d];
      const fw = c.to[ax[0]] - c.from[ax[0]], fh = c.to[ax[1]] - c.from[ax[1]];
      if (fw > 0 && fh > 0 && (w === 0 || h === 0)) issues.push(`${name}:${c.name} ${d} zero-width UV`);
      if (fw > 0 && fh > 0 && (Math.abs(w - Math.min(fw, 16)) > 0.001 || Math.abs(h - Math.min(fh, 16)) > 0.001)) issues.push(`${name}:${c.name} ${d} uv ${w}x${h} for a ${fw}x${fh} face`);
    }
  }
  console.log(issues.length ? issues.join('\n') : 'export limits: ok (faces, bounds -16..32, rotations, UVs one texel per pixel)');
}
// luminance of the new textures (mean, 0-255)
{
  const zlib = require('zlib');
  const lum = (buf) => {
    let p = 8;
    const idat = [];
    while (p < buf.length) { const len = buf.readUInt32BE(p), type = buf.toString('ascii', p + 4, p + 8); if (type === 'IDAT') idat.push(buf.slice(p + 8, p + 8 + len)); p += 12 + len; }
    const raw = zlib.inflateSync(Buffer.concat(idat));
    let s = 0, n = 0;
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const o = y * 65 + 1 + x * 4;
      if (raw[o + 3] === 0) continue;
      s += 0.2126 * raw[o] + 0.7152 * raw[o + 1] + 0.0722 * raw[o + 2]; n++;
    }
    return Math.round(s / n);
  };
  const keys = ['va_plain', 'vA_00', 'vB_plain', 'vB_vent', 'va_bond_l', 'va_field', 'va_band', 'va_heat', 'seb_0', 'seb_1', 'seb_2', 'seb_cracked', 'seb_top', 'endbrick', 'key_stone', 'scorched', 'win_a', 'rib_patina'];
  console.log('luminance:', keys.map((k) => `${k} ${lum(T.OWN[k])}`).join(', '));
}
}
