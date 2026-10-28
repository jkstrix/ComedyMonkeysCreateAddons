// Clockwork Wastes city blocks preview -> design/models/clockwork-wastes-city.bbmodel (Generic Model preview).
// To design/briefs/clockwork-wastes-blocks.md §1-4: Substation, City Vault, Sliding Door, Derelict Street Lamp (upright, leaning,
// snapped), Derelict Wall Lamp, Derelict Cable (surface and span). Palette and textures from clockwork-wastes-materials.gen.js.
// node clockwork-wastes-city.gen.js
// Every piece is built in its block's model space (x 0 west -> 16 east, y up, z 0 north -> 16 south), front to the north.
// Glows are their own shade:false cubes in their own groups, so the clips can light them. The rest pose is awake;
// the clips are 'dormant' (as found), 'wake' (the district waking, about 6 s) and 'awake'.
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, STEEL, IRON, BRASS, COPPER, PATINA, RUST, GROUND, mott, edge, va, box, CHAIN } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-city.bbmodel');
const GLOW = { shade: false };

// ---------- textures ----------
const BEVEL_HI = '#4a5059', BEVEL_LO = '#2a2f35', DUST = ['#7c7659', '#878063'];
const steelField = (x, y, s) => (hash(x, y, s) > 0.88 ? '#40464e' : STEEL[1]);
// the makers' cabinet: near-flat dark steel panels with crisp bevels (the process-box look); rust runs from the top corners
// one panel over both blocks: the lower cell has no top bevel, the upper none at its bottom
const cabFront = (rust) => (x, y) => {
  if (rust && (x === 1 || x === 14) && y >= 1 && y <= (x === 1 ? 5 : 3)) return y < 3 ? RUST[1] : RUST[0];
  if (x === 0 || (rust && y === 0)) return BEVEL_HI;
  if (x === 15 || (!rust && y === 15)) return BEVEL_LO;
  return steelField(x, y, 41);
};
T.paint('cab_front_lo', cabFront(false));
T.paint('cab_front_hi', cabFront(true));
// the sides: a steel frame round one tall andesite inset panel over both blocks (no frame where the two cells meet)
const cabSide = (upper) => (x, y) => {
  const e = Math.min(x, 15 - x, upper ? y : 99, upper ? 99 : 15 - y);
  if (e === 0) return x === 0 || y === 0 ? BEVEL_HI : BEVEL_LO;
  if (e === 1) return STEEL[1];
  if (e === 2) return x === 2 || y === 2 ? BEVEL_LO : '#767873';
  return mott(['#5e615d', '#6b6e6a', '#767873'], 42, 0.15, 0.9)(x, y);
};
T.paint('cab_side_lo', cabSide(false));
T.paint('cab_side_hi', cabSide(true));
// settled End dust: clusters along the top edges only, never speckle
const dusty = (base, s) => (x, y) => (edge(x, y) === 0 && hash(x, y, s) > 0.45 ? DUST[hash(x, y, s + 1) > 0.5 ? 1 : 0] : base(x, y));
T.paint('cab_top', dusty((x, y) => steelField(x, y, 43), 44));
T.paint('cab_back', (x, y) => steelField(x, y, 45));
T.paint('glass_tube', (x, y) => (x === 7 ? '#34304a' : '#2a2833'));
T.paint('column_lit', (x, y) => (y === 0 ? STAR.core : STAR.edge));
T.paint('flash', () => STAR.core);
T.paint('steel', mott(STEEL, 46, 0.12, 0.9));
T.paint('copper', mott(COPPER, 47, 0.15, 0.85));
// the Substation's sigil plate, 6 x 6 at uv 0-6: a small lens-in-cog; the lit lens at the same pixels
const SIGIL = ['#.##.#', '.####.', '##oo##', '##oo##', '.####.', '#.##.#'];
T.paint('sub_sigil', (x, y) => { const k = (SIGIL[y] || '')[x]; return k === '#' ? (x + y < 5 ? VA.hi : VA.seam) : k === 'o' ? VA.glass : k === '.' ? VA.lo : va(x, y); });
T.paint('sub_sigil_lit', (x, y) => ((SIGIL[y] || '')[x] === 'o' ? (x === 2 && y === 2 ? STAR.core : STAR.edge) : null));
T.paint('vault_inner', (x, y) => (hash(x, y, 48) > 0.9 ? '#16151c' : '#121118'));
T.paint('loot_gold', () => '#a3873f');
T.paint('loot_gem', () => '#5f9a9a');
// the cast-iron lamp posts: our Cast Iron Lamp Post's family, older (rust streaks, a broken flute)
T.paint('iron', mott(IRON, 51, 0.12, 0.9));
T.paint('iron_shaft', (x, y) => (x % 2 === 0 ? '#2b2f33' : hash(x, y, 52) > 0.9 ? IRON[2] : '#33373c'));
T.paint('iron_shaft_rust', (x, y) => (x === 5 && y > 2 && y < 11 ? (y < 8 ? RUST[1] : RUST[0]) : x % 2 === 0 ? '#2b2f33' : '#33373c'));
T.paint('iron_flute', (x, y) => (x % 2 === 0 ? '#202326' : IRON[1]));
T.paint('iron_flute_broken', (x, y) => (x === 9 && y > 5 ? '#151719' : x === 9 ? IRON[0] : x % 2 === 0 ? '#202326' : IRON[1]));
T.paint('iron_chipped', (x, y) => ((x > 11 && y < 3) || (x === 11 && y === 0) ? '#5a5d58' : mott(IRON, 53, 0.12, 0.9)(x, y)));
// lamp lenses, 4 px at uv 6-10: dark glass with one dull catchlight; lit: edge ring and a 2 x 2 core; cracked never lights
T.paint('lens_dark', (x, y) => (x === 7 && y === 7 ? VA.glint : VA.glass));
T.paint('lens_lit', (x, y) => (x >= 7 && x <= 8 && y >= 7 && y <= 8 ? STAR.core : STAR.edge));
T.paint('lens_cracked', (x, y) => (x - 6 === y - 6 || (x === 9 && y === 7) ? '#4c475f' : VA.glass));
// copper drawn from the makers' pipes, mostly gone to patina, copper showing in places
T.paint('cable', (x, y) => (hash(x, y, 54) > 0.8 ? COPPER[1] : hash(x, y, 55) > 0.5 ? PATINA[0] : PATINA[1]));
T.paint('endbrick_dusty', dusty((x, y) => (y % 4 === 3 || (x + ((y >> 2) % 2 ? 4 : 0)) % 8 === 7 ? '#6f6a50' : hash(x, y, 28) > 0.8 ? '#9c9672' : '#8f8a68'), 56));

// ---------- geometry helpers ----------
// a round part along y (lenses pointing down, posts): crossed boxes; up/down faces mapped round the texture's centre (8, 8)
function ydisc(name, cx, cz, y0, y1, w, tex, o = {}) {
  const h = w / 2, n = w >= 6 ? Math.round(w * 0.16 * 2) / 2 : 1;
  const rects = w <= 2 ? [[h, h]] : [[h, h - n], [h - n, h], ...(w >= 6 ? [[h - n / 2, h - n / 2]] : [])];
  return rects.map(([hx, hz], i) => {
    const uv = [8 - hx, 8 - hz, 8 + hx, 8 + hz];
    return box(`${name}_${i}`, [cx - hx, y0, cz - hz], [cx + hx, y1, cz + hz], typeof tex === 'string' ? { all: tex } : tex, { ...o, uv: { up: uv, down: uv } });
  });
}
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]), rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const rotAll = (cubes, axis, angle, origin) => cubes.map((c) => ({ ...c, rot: [axis, angle, origin] }));
const named = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));
// a small void-alloy insulator: a chamfered stack, 2 px across, 3 tall
const insulator = (name, cx, cz, y) => [box(name + '_a', [cx - 1, y, cz - 1], [cx + 1, y + 1, cz + 1], 'va'), box(name + '_b', [cx - 0.75, y + 1, cz - 0.75], [cx + 0.75, y + 2, cz + 0.75], 'va'), box(name + '_c', [cx - 1, y + 2, cz - 1], [cx + 1, y + 3, cz + 1], 'va')];

// ---------- 1. Substation (1 x 2): makers' cabinet, the uprising's charge column clamped into it ----------
// the column frame and sigil sit 1 px proud of the cabinet front (z 2); the knife-switch lever on the east side, pivot (15, 20, 8)
const COL = { x0: 6.5, x1: 9.5, y0: 3.5, y1: 21.5 };
function substation() {
  const body = [
    box('plinth', [0.5, 0, 1.5], [15.5, 1, 14.5], { all: 'steel', down: null }),
    box('cab_lo', [1, 1, 2], [15, 16, 14], { north: 'cab_front_lo', south: 'cab_back', east: 'cab_side_lo', west: 'cab_side_lo', up: null, down: null }),
    box('cab_hi', [1, 16, 2], [15, 31, 14], { north: 'cab_front_hi', south: 'cab_back', east: 'cab_side_hi', west: 'cab_side_hi', up: 'cab_top', down: null }),
    // the frame round the tube: four void-alloy bars (frames go round, not behind)
    box('frame_l', [5, 2, 1], [COL.x0, 23, 2], { all: 'va', south: null }), box('frame_r', [COL.x1, 2, 1], [11, 23, 2], { all: 'va', south: null }),
    box('frame_b', [COL.x0, 2, 1], [COL.x1, COL.y0, 2], { all: 'va', south: null }), box('frame_t', [COL.x0, COL.y1, 1], [COL.x1, 23, 2], { all: 'va', south: null }),
    // the smoked tube, set back half a pixel in the frame
    box('tube', [COL.x0, COL.y0, 1.5], [COL.x1, COL.y1, 2], { north: 'glass_tube' }),
    // four void-alloy brackets that bite into the steel: the join between the two layers
    ...[[3.5, 3], [3.5, 20.5], [11, 3], [11, 20.5]].map(([x, y], i) => box('bracket_' + i, [x, y, 1.25], [x + 1.5, y + 1.5, 2], { all: 'va', south: null })),
    // the sigil plate above the column, 6 x 6, uv 0-6 of its texture
    box('sigil', [5, 24, 1], [11, 30, 2], { north: 'sub_sigil', east: 'va', west: 'va', up: 'va', down: 'va', south: null }, { uv: { north: [0, 0, 6, 6] } }),
    // the lever's brass pivot boss and its two contact jaws below it
    box('boss', [15, 19, 7], [16.5, 21, 9], { all: 'brass', west: null }),
    box('jaw_n', [15, 10, 6.5], [17, 12, 7.25], { all: 'brass', west: null }), box('jaw_s', [15, 10, 8.75], [17, 12, 9.5], { all: 'brass', west: null }),
    // FE in at the back face centre: a copper terminal
    box('terminal', [6, 6, 14], [10, 10, 15], { all: 'copper', north: null }),
    // the cable tray on top: two insulators where the district's cable leaves for the sky-web
    ...insulator('ins_w', 5, 8, 31), ...insulator('ins_e', 11, 8, 31),
  ];
  // seams on the frame bars (dark always there, lit on top of them), the sigil's lens, the contact flash, the column fill
  const seam = (tex, o) => [box('seam_l', [5.5, 2.5, 0.75], [6, 22.5, 0.75], { north: tex }, o), box('seam_r', [10, 2.5, 0.75], [10.5, 22.5, 0.75], { north: tex }, o)];
  return {
    body: [...body, ...seam('seam_strip_dark')],
    lever: [box('blade', [16.5, 11, 7.5], [17.5, 20, 8.5], 'steel'), box('grip', [16.25, 10, 7.25], [17.75, 11, 8.75], 'walnut'), box('pin', [16.25, 19.5, 7.75], [17.75, 20.5, 8.25], 'brass')],
    seams: seam('seam_strip_lit', GLOW),
    sigil: [box('sigil_lens', [5, 24, 0.75], [11, 30, 0.75], { north: 'sub_sigil_lit' }, { ...GLOW, uv: { north: [0, 0, 6, 6] } })],
    flash: [box('flash_n', [17.25, 10.5, 6.75], [17.25, 11.75, 7], { east: 'flash' }, GLOW), box('flash_s', [17.25, 10.5, 9], [17.25, 11.75, 9.25], { east: 'flash' }, GLOW)],
    column: [box('column', [COL.x0 + 0.25, COL.y0, 1.25], [COL.x1 - 0.25, COL.y1, 1.25], { north: 'column_lit' }, { ...GLOW, uv: { north: [6.75, 0, 9.25, 16] } })],
  };
}

// ---------- 2. City Vault: a deep void-alloy block, an iris of six blades in a round recess ----------
// the bezel (z 1-2) is a front plate with a 12 px stepped round opening; the walls round the recess; blades stacked 0.5 apart
const OPEN = [[13, 6, 10], [12, 4, 12], [11, 3, 13], [10, 3, 13], [9, 2, 14], [8, 2, 14], [7, 2, 14], [6, 2, 14], [5, 3, 13], [4, 3, 13], [3, 4, 12], [2, 6, 10]];
function vault() {
  const out = [box('core', [0, 0, 7], [16, 16, 16], { all: 'va', north: 'vault_inner' }),
    box('wall_w', [0, 0, 2], [2, 16, 7], 'va'), box('wall_e', [14, 0, 2], [16, 16, 7], 'va'),
    box('wall_b', [2, 0, 2], [14, 2, 7], 'va'), box('wall_t', [2, 14, 2], [14, 16, 7], 'va'),
    box('bezel_t', [1, 14, 1], [15, 16, 2], { all: 'va', south: null }), box('bezel_b', [1, 0, 1], [15, 2, 2], { all: 'va', south: null })];
  for (const [y, x0, x1] of OPEN) {
    if (x0 > 1) out.push(box(`bezel_w${y}`, [1, y, 1], [x0, y + 1, 2], { all: 'va', south: null }));
    if (x1 < 15) out.push(box(`bezel_e${y}`, [x1, y, 1], [15, y + 1, 2], { all: 'va', south: null }));
  }
  // the shelf inside and a few loot glints, seen once the iris opens
  out.push(box('shelf', [4, 5, 3.5], [12, 6, 7], 'va'), box('loot_a', [5, 6, 4], [6.5, 7, 5], 'loot_gold'), box('loot_b', [9.5, 6, 4.5], [10.5, 7.5, 5.5], 'loot_gem'));
  return out;
}
// blade k: a rectangle whose inner edge runs through the iris centre at 60k degrees; the bezel hides what lies past the rim.
// Built as a flat 7 x 5 plate in the blade's own frame, its hinge at the rim end of its inner edge (14, 8). The outer group
// turns it to 60k degrees about the centre (rest pose); the inner 'swing' group turns it about the hinge to open.
// each blade sits in front of the one before it, so blade k+1 hides blade k's far side and the closed iris shows one edge per blade
// blades 0.75 apart: room for each blade's dark seam (0.25 in front of it) and lit seam (0.5) before the next blade
const BLADE_Z = (k) => 2.25 + (5 - k) * 0.75;
const blade = (k) => [box('blade_' + k, [8 - 1, 8, BLADE_Z(k)], [8 + 6, 13, BLADE_Z(k) + 0.5], { north: 'va', east: 'va', west: 'va', up: 'va', down: 'va', south: null }),
  box('blade_seam_d_' + k, [8 - 1, 8, BLADE_Z(k) - 0.25], [8 + 6, 8.5, BLADE_Z(k) - 0.25], { north: 'seam_strip_dark' })];
const bladeSeam = (k) => [box('blade_seam_' + k, [8 - 1, 8, BLADE_Z(k) - 0.5], [8 + 6, 8.5, BLADE_Z(k) - 0.5], { north: 'seam_strip_lit' }, GLOW)];

// ---------- 3. Sliding Door (2 wide x 3 tall): two void-alloy leaves meeting at a stepped seam ----------
// teeth alternate every 12 px so the leaves interlock; three stepped panels per leaf; the broken chain across the seam
const TEETH = [[0, 12, 'l'], [12, 24, 'r'], [24, 36, 'l'], [36, 48, 'r']];
function leaf(side) {
  const L = side === 'l', out = [];
  out.push(box('leaf_' + side, L ? [0, 0, 6] : [17, 0, 6], L ? [15, 48, 10] : [32, 48, 10], 'va'));
  for (const [y0, y1, s] of TEETH) if (s === side) out.push(box(`tooth_${side}_${y0}`, [15, y0, 6], [17, y1, 10], 'va'));
  const px = L ? [1.5, 13.5] : [18.5, 30.5];
  [[2, 15], [17, 31], [33, 46]].forEach(([y0, y1], i) => {
    out.push(box(`panel_${side}_${i}`, [px[0], y0, 5], [px[1], y1 - 1, 6], { all: 'va', south: null }));
    out.push(box(`panel_cap_${side}_${i}`, [px[0] + 1, y1 - 1, 5], [px[1] - 1, y1, 6], { all: 'va', south: null }));
  });
  // the chain plate's half on this leaf: 8 x 5 at chest height for a bot; viewed from the north, the east half shows art u 0-8
  out.push(box('chain_' + side, L ? [8, 32, 4.75] : [16, 32, 4.75], L ? [16, 37, 5] : [24, 37, 5], { north: 'chain_dark', all: null }, { uv: { north: L ? [8, 5, 16, 10] : [0, 5, 8, 10] } }));
  return out;
}
const chainLit = (side) => [box('chain_lit_' + side, side === 'l' ? [8, 32, 4.5] : [16, 32, 4.5], side === 'l' ? [16, 37, 4.5] : [24, 37, 4.5], { north: 'chain_lit' }, { ...GLOW, uv: { north: side === 'l' ? [8, 5, 16, 10] : [0, 5, 8, 10] } })];
// the stepped seam's glow, on the leaves' front between the panels (0.25 proud of z 6)
function doorSeam(tex, o = {}) {
  const out = [];
  for (const [y0, y1, s] of TEETH) out.push(box(`dseam_v${y0}`, [s === 'l' ? 16.75 : 14.75, y0, 5.75], [s === 'l' ? 17.25 : 15.25, y1, 5.75], { north: tex }, o));
  for (const y of [12, 24, 36]) out.push(box(`dseam_h${y}`, [14.75, y - 0.25, 5.75], [17.25, y + 0.25, 5.75], { north: tex }, o));
  return out;
}
const jamb = () => [box('jamb_w', [-2, 0, 4], [0, 50, 12], 'va'), box('jamb_e', [32, 0, 4], [34, 50, 12], 'va'), box('jamb_t', [0, 48, 4], [32, 50, 12], 'va')];
const jambGlow = () => [box('jglow_w', [-0.25, 1, 7.5], [-0.25, 47, 8.5], { east: 'seam_strip_lit' }, GLOW), box('jglow_e', [32.25, 1, 7.5], [32.25, 47, 8.5], { west: 'seam_strip_lit' }, GLOW)];

// ---------- 4. Derelict lamps: makers' cast-iron oil-lamp posts, the uprising's hooded lens heads; and the cable ----------
function postFoot(chipped) {
  return [box('plinth', [4, 0, 4], [12, 2, 12], { all: chipped ? 'iron_chipped' : 'iron', down: null }),
    box('fluted', [5, 2, 5], [11, 6, 11], { all: 'iron_flute', north: 'iron_flute_broken', up: 'iron', down: null }),
    box('moulding', [5.5, 6, 5.5], [10.5, 7, 10.5], { all: 'iron', down: null }),
    box('shaft', [6, 7, 6], [10, 16, 10], { all: 'iron_shaft', north: 'iron_shaft_rust', up: null, down: null })];
}
const postPole = () => [box('shaft', [6, 0, 6], [10, 16, 10], { all: 'iron_shaft', up: null, down: null })];
// the head block: the shaft, a collar, the crook arm curling north with its scrollwork, a void-alloy clamp biting the arm's tip,
// and the hood hanging from it: 6 x 4 x 6, chamfered, a 4 px lens looking down at the street
function postHead(snapped) {
  const out = [box('shaft', [6, 0, 6], [10, 11, 10], { all: 'iron_shaft', up: null, down: null }),
    box('collar', [5.5, 11, 5.5], [10.5, 12.5, 10.5], 'iron'), box('riser', [7, 12.5, 7], [9, 15, 9], 'iron')];
  if (!snapped) {
    out.push(box('arm', [7, 14, 1.5], [9, 15.5, 7], { all: 'iron', south: null }));
    [[11.5, 12.5, 5, 6.5], [12.5, 13.5, 4, 5], [13, 14, 2.5, 4]].forEach(([y0, y1, z0, z1], i) => out.push(box('scroll_' + i, [7.5, y0, z0], [8.5, y1, z1], 'iron')));
    out.push(box('scroll_curl', [7.5, 10.5, 4.5], [8.5, 11.5, 5.5], 'iron'));
    out.push(box('clamp', [6.5, 13.5, -0.5], [9.5, 16, 1.5], 'va'), box('neck', [7.5, 12.5, 0], [8.5, 13.5, 1], 'va'));
    out.push(...hood(0, 0));
  } else {
    // the crook bent down 45 degrees at the riser, the hood hanging off its end, lens cracked
    const bent = [box('arm', [7, 14, 1.5], [9, 15.5, 7], { all: 'iron', south: null }), box('clamp', [6.5, 13.5, -0.5], [9.5, 16, 1.5], 'va'),
      box('neck', [7.5, 12.5, 0], [8.5, 13.5, 1], 'va'), ...hood(0, 0, 'lens_cracked')];
    out.push(...rotAll(bent, 'x', -45, [8, 14.75, 7]));
  }
  return out;
}
function hood(dy, dz, lens = 'lens_dark') {
  // a stepped crown over the body (6 x 2.5 x 6, then 5 x 1 x 5, then 3 x 0.5 x 3), so it reads as a hood, not a box
  return shift([box('hood', [5, 8.5, -3], [11, 11, 3], { all: 'va', down: null }), box('hood_shoulder', [5.5, 11, -2.5], [10.5, 12, 2.5], { all: 'va', down: null }),
    box('hood_crown', [6.5, 12, -1.5], [9.5, 12.5, 1.5], { all: 'va', down: null }),
    box('hood_lip_n', [5, 8, -3], [11, 8.5, -2], 'va'), box('hood_lip_s', [5, 8, 2], [11, 8.5, 3], 'va'),
    box('hood_lip_w', [5, 8, -2], [6, 8.5, 2], 'va'), box('hood_lip_e', [10, 8, -2], [11, 8.5, 2], 'va'),
    ...ydisc('lens', 8, 0, 8.25, 8.5, 4, { down: lens, all: null })], [0, dy, dz]);
}
const lampGlow = (name) => ydisc(name, 8, 0, 8, 8, 4, { down: 'lens_lit', all: null }, GLOW);
// the wall lamp: a scrolled cast-iron bracket on the wall (south, z 16), the same hooded lens, tipped 22.5 degrees down and out
function wallLamp() {
  const head = [box('clamp', [6.5, 11.5, 3.5], [9.5, 14, 5.5], 'va'), box('neck', [7.5, 10.5, 4], [8.5, 11.5, 5], 'va'), ...shift(hood(0, 0), [0, -2, 4])];
  return [box('wall_plate', [6, 6, 15], [10, 14, 16], { all: 'iron', south: null }),
    box('w_arm', [7, 12, 5.5], [9, 13.5, 15], { all: 'iron', south: null }),
    ...[[10, 11, 13, 14.5], [11, 12, 12, 13]].map(([y0, y1, z0, z1], i) => box('w_scroll_' + i, [7.5, y0, z0], [8.5, y1, z1], 'iron')),
    box('w_curl', [7.5, 8, 13.5], [8.5, 9, 14.5], 'iron'),
    ...rotAll(head, 'x', -22.5, [8, 12.75, 5.5])];
}
const wallLampGlow = () => rotAll(shift(lampGlow('wl_glow'), [0, -2, 4]), 'x', -22.5, [8, 12.75, 5.5]);
// surface cable on the floor along x: two strands with a hairline gap, void-alloy clips every 8 px
function floorCable(name) {
  const out = [box(name + '_n', [0, 0, 7], [16, 1, 7.75], { all: 'cable', down: null }), box(name + '_s', [0, 0, 8.25], [16, 1, 9], { all: 'cable', down: null })];
  for (const x of [3, 11]) out.push(box(`${name}_clip_${x}`, [x, 0, 6.5], [x + 2, 1.5, 9.5], { all: 'va', down: null }));
  return out;
}
const floorCableGlow = (name) => [box(name + '_g', [0, 0, 7.75], [16, 0.75, 8.25], { up: 'seam_strip_lit' }, GLOW)];

// ---------- the scene ----------
const { cube, group, clip, write } = preview('clockwork_wastes_city', { 'w:': T });
const outliner = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const G = (name, pivot, off, kids) => { outliner.push(group(name, pivot, off, kids)); };
const sub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));

// a floor of Scorched End Stone under it all (top faces only)
const floor = [];
for (let x = -2; x <= 13; x++) for (let z = -1; z <= 4; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
G('floor', [8, 0, 8], [0, 0, 0], floor);

// Substation at block (0, 0, 0)
{
  const s = substation(), off = at(0, 0, 0);
  G('substation', [8, 0, 8], off, [...put(s.body, off), sub('lever', [16, 20, 8], off, s.lever), sub('g_sub_seams', [8, 0, 1], off, s.seams),
    sub('g_sub_sigil', [8, 27, 1], off, s.sigil), sub('g_sub_flash', [17, 11, 8], off, s.flash), sub('g_sub_column', [8, COL.y0, 1.25], off, s.column)]);
}
// a void-alloy tower stand-in for the sky-web: a 3-tall pillar with an insulator on top, 4 blocks east
{
  const off = at(4, 0, 0), list = [];
  for (let i = 0; i < 3; i++) list.push(...shift([box('p_x' + i, [0, 0, 2], [16, 16, 14], 'va'), box('p_m' + i, [1, 0, 1], [15, 16, 15], 'va'), box('p_z' + i, [2, 0, 0], [14, 16, 16], 'va')], [0, i * 16, 0]));
  list.push(...insulator('tower_ins', 8, 8, 48));
  G('tower', [8, 0, 8], off, put(list, off));
}
// the span: from the Substation's east insulator to the tower's, sagging; and a snapped span hanging off the tower's far side
const SPAN_A = [11, 34, 8], SPAN_B = [4 * 16 + 8, 51, 8];
const spanPoint = (t) => { const sag = 10; return [SPAN_A[0] + (SPAN_B[0] - SPAN_A[0]) * t, SPAN_A[1] + (SPAN_B[1] - SPAN_A[1]) * t - sag * 4 * t * (1 - t), 8]; };
{
  const N = 48, seg = (SPAN_B[0] - SPAN_A[0]) / N, dark = [], lit = [];
  for (let k = 0; k < N; k++) {
    const t = (k + 0.5) / N, [x, y] = spanPoint(t), [x1, y1] = spanPoint(t + 0.5 / N), [x0, y0] = spanPoint(t - 0.5 / N);
    const ang = Math.atan2(y1 - y0, x1 - x0) * 180 / Math.PI;
    dark.push(box('span_' + k, [x - seg / 2 - 0.1, y - 0.4, 7.6], [x + seg / 2 + 0.1, y + 0.4, 8.4], 'cable', { rot: ['z', ang, [x, y, 8]] }));
    lit.push(box('span_g_' + k, [x - seg / 2 - 0.1, y + 0.65, 7.85], [x + seg / 2 + 0.1, y + 0.65, 8.15], { up: 'seam_strip_lit' }, { ...GLOW, rot: ['z', ang, [x, y, 8]] }));
  }
  // the snapped span: hangs straight down the tower's east side, its end curled
  const hang = [box('hang', [4 * 16 + 16.5, 30, 7.6], [4 * 16 + 17.3, 51, 8.4], 'cable'), box('hang_tip', [4 * 16 + 17.3, 30, 7.6], [4 * 16 + 19, 30.8, 8.4], 'cable')];
  G('span', [0, 0, 0], [0, 0, 0], [...put(dark, [0, 0, 0]), ...put(hang, [0, 0, 0]), sub('g_span', [0, 0, 0], [0, 0, 0], lit)]);
  G('pulse', [0, 0, 0], [0, 0, 0], put([box('bead', [-1.5, -0.6, 7.4], [1.5, 0.6, 8.6], 'flash', GLOW)], SPAN_A));
}
// City Vault at block (7, 0, 0)
{
  const off = at(7, 0, 0), kids = put(vault(), off);
  for (let k = 0; k < 6; k++) {
    const swing = group('swing_' + k, [14, 8, BLADE_Z(k)], off, [...put(blade(k), off), sub('g_blade_seam_' + k, [14, 8, BLADE_Z(k)], off, bladeSeam(k))]);
    kids.push(group('blade_' + k, [8, 8, BLADE_Z(k)], off, [swing]));
  }
  kids.push(sub('g_vault_pip', [8, 8, 2], off, [box('pip', [7.5, 7.5, 1.5], [8.5, 8.5, 1.5], { north: 'flash' }, GLOW)]));
  G('vault', [8, 0, 8], off, kids);
}
// Sliding Door at blocks (9-10, 0, 0) with End Stone brick wall either side
{
  const off = at(9, 0, 0);
  const wall = [];
  for (const bx of [-1, 2]) for (let y = 0; y < 3; y++) wall.push(box(`wall_${bx}_${y}`, [bx * 16 + (bx < 0 ? 0 : 2), y * 16, 4], [bx * 16 + (bx < 0 ? 14 : 16), y * 16 + 16, 12], 'endbrick_dusty'));
  wall.push(box('wall_top', [-16, 50, 4], [48, 52, 12], 'endbrick_dusty'));
  G('door', [16, 0, 8], off, [...put([...jamb(), ...wall], off),
    sub('leaf_l', [8, 0, 8], off, leaf('l')), sub('leaf_r', [24, 0, 8], off, leaf('r')),
    sub('g_door_seam', [16, 24, 6], off, doorSeam('seam_strip_lit', GLOW)), sub('g_door_chain', [16, 34, 5], off, [...chainLit('l'), ...chainLit('r')]),
    sub('g_door_jamb', [16, 24, 8], off, jambGlow())]);
}
// the street: lamps on the south side, 2 blocks out, their crooks reaching north over the floor cable
const LAMPS = [['lamp_upright', 0, 0], ['lamp_leaning', 2, 12], ['lamp_snapped', 4, 0]];
LAMPS.forEach(([name, bx, lean]) => {
  const off = at(bx, 0, 3), snapped = name === 'lamp_snapped';
  const list = [...postFoot(bx === 2), ...shift(postPole(), [0, 16, 0]), ...shift(postHead(snapped), [0, 32, 0])];
  const kids = put(list, off);
  if (!snapped) kids.push(sub('g_' + name, [8, 40, 0], off, shift(lampGlow(name + '_glow'), [0, 32, 0])));
  outliner.push(group(name, [8, 0, 8], off, kids));
});
// the wall lamp on a brick wall block at (6, 0, 3)
{
  const off = at(6, 0, 3);
  G('wall_lamp', [8, 0, 8], off, [...put([box('wl_wall', [0, 0, 16], [16, 32, 20], 'endbrick_dusty'), ...shift(wallLamp(), [0, 16, 0])], off),
    sub('g_wall_lamp', [8, 26, 8], off, shift(wallLampGlow(), [0, 16, 0]))]);
}
// surface cable along the street, z 2, from the Substation's foot to the tower
{
  const kids = [], glow = [];
  for (let bx = 0; bx < 5; bx++) { kids.push(...put(floorCable('fc' + bx), at(bx, 0, 2))); glow.push(...put(floorCableGlow('fc' + bx), at(bx, 0, 2))); }
  G('floor_cable', [0, 0, 0], [0, 0, 0], [...kids, group('g_floor_cable', [0, 0, 0], [0, 0, 0], glow)]);
}


// ---------- clips ----------
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const GLOWS = ['g_sub_seams', 'g_sub_sigil', 'g_sub_column', 'g_span', 'g_floor_cable', 'g_door_seam', 'g_door_chain', 'g_door_jamb', 'g_vault_pip',
  'g_lamp_upright', 'g_lamp_leaning', 'g_wall_lamp', ...[0, 1, 2, 3, 4, 5].map((k) => 'g_blade_seam_' + k)];
const LEVER_OPEN = -130;
const ease = (t, t0, t1) => (t <= t0 ? 0 : t >= t1 ? 1 : ((u) => u * u * (3 - 2 * u))((t - t0) / (t1 - t0)));
// the slam: 0.12 s down, a hard bounce off the jaw and a short settle
const slam = (t, t0) => { if (t <= t0) return 0; const u = (t - t0) / 0.12; return u < 1 ? u * u : 1 - 0.12 * Math.exp(-(u - 1) * 1.6) * Math.abs(Math.sin((u - 1) * 2.2)); };
// blades: each sits at 60k degrees about the iris centre; opening, each swings out about its hinge on the rim and folds in
// toward it (a 16 px block has only a 2 px ring outside the 12 px iris, so a blade can't swing out whole without leaving the block)
const BLADE_SWING = -40, BLADE_FOLD = [0.35, 0.6, 1];
const bladeTrack = (t0, t1) => ({ rotation: (t) => [0, 0, BLADE_SWING * ease(t, t0, t1)], scale: (t) => BLADE_FOLD.map((f) => 1 + (f - 1) * ease(t, t0, t1)) });
const steadyBlades = (open) => Object.fromEntries([0, 1, 2, 3, 4, 5].map((k) => ['swing_' + k, bladeTrack(open ? -1 : 1, open ? 0 : 2)]));
const doors = (o) => ({ leaf_l: { position: () => [-16 * o, 0, 0] }, leaf_r: { position: () => [16 * o, 0, 0] } });

// dormant: everything dark, the lever thrown open, the iris shut, the doors shut
clip('dormant', 1, 'loop', { lever: { rotation: () => [LEVER_OPEN, 0, 0] }, ...steadyBlades(false), ...doors(0) },
  Object.fromEntries([...GLOWS, 'g_sub_flash', 'pulse'].map((g) => [g, { scale: [[0, HIDE]] }])));
// awake: lit, the lever home, the iris and doors open (the door's seam and chain went with its leaves)
clip('awake', 1, 'loop', { lever: { rotation: () => [0, 0, 0] }, ...steadyBlades(true), ...doors(1) },
  Object.fromEntries([...GLOWS, 'g_sub_flash', 'pulse'].map((g) => [g, { scale: [[0, ['g_door_seam', 'g_door_chain', 'g_sub_flash', 'pulse', 'g_vault_pip'].includes(g) ? HIDE : SHOW]] }])));
// wake: the column fills (2 s, 10 steps), the lever slams (2.0 s), the pulse runs out along the span, the lamps ripple on,
// the vault's seams light in a spiral, then its blades turn open; the door's seam lights, then its leaves slide into the walls
{
  const steps = {};
  steps.g_sub_column = { scale: [[0, [1, 0, 1]], ...[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((k) => [k * 0.2, [1, k / 10, 1]])] };
  steps.g_sub_flash = { scale: [[0, HIDE], [2.08, SHOW], [2.3, HIDE]] };
  for (const g of ['g_sub_seams', 'g_sub_sigil']) steps[g] = { scale: [[0, HIDE], [2.1, SHOW]] };
  steps.g_span = { scale: [[0, HIDE], [3.2, SHOW]] };
  steps.g_floor_cable = { scale: [[0, HIDE], [2.4, SHOW]] };
  steps.pulse = { scale: [[0, HIDE], [2.2, SHOW], [3.2, HIDE]] };
  [['g_lamp_upright', 3.3], ['g_lamp_leaning', 3.45], ['g_wall_lamp', 3.6]].forEach(([g, t]) => { steps[g] = { scale: [[0, HIDE], [t, SHOW]] }; });
  for (let k = 0; k < 6; k++) steps['g_blade_seam_' + k] = { scale: [[0, HIDE], [3.6 + k * 0.08, SHOW]] };
  steps.g_vault_pip = { scale: [[0, HIDE], [3.55, SHOW], [4.1, HIDE]] };
  steps.g_door_seam = { scale: [[0, HIDE], [3.7, SHOW], [4.2, HIDE]] };
  steps.g_door_chain = { scale: [[0, HIDE], [3.7, SHOW], [4.2, HIDE]] };
  steps.g_door_jamb = { scale: [[0, HIDE], [5.2, SHOW]] };
  const tracks = {
    lever: { rotation: (t) => [LEVER_OPEN * (1 - slam(t, 2.0)), 0, 0] },
    pulse: { position: (t) => { const u = ease(t, 2.2, 3.2); const p = spanPoint(u); return [p[0] - SPAN_A[0], p[1] - SPAN_A[1], 0]; } },
    ...doors(0),
  };
  for (let k = 0; k < 6; k++) {
    tracks['swing_' + k] = bladeTrack(4.1, 5.1);
  }
  tracks.leaf_l = { position: (t) => [-16 * ease(t, 4.2, 5.2), 0, 0] };
  tracks.leaf_r = { position: (t) => [16 * ease(t, 4.2, 5.2), 0, 0] };
  clip('wake', 6.5, 'hold', tracks, steps);
}
write(OUT, outliner);
// rest rotations the preview library doesn't set: each blade at 60k degrees round the iris, the leaning post's 12 degrees
// (a block model can only do 22.5; see the notes). Written into both the outliner and the group list.
{
  const REST = { lamp_leaning: [12, 0, 0] };
  for (let k = 0; k < 6; k++) REST['blade_' + k] = [0, 0, 60 * k];
  const bb = JSON.parse(require('fs').readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  require('fs').writeFileSync(OUT, JSON.stringify(bb));
}
module.exports = { substation, vault, blade, leaf, postFoot, postPole, postHead, wallLamp, floorCable, ydisc, insulator };
