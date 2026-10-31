// Clockwork Wastes: the city's last stand-ins, modelled -> design/models/clockwork-wastes-city-props.bbmodel (Generic Model
// preview, D69). Preview only: nothing here is in the mod until the user approves the look.
// node clockwork-wastes-city-props.gen.js   (writes the preview; required, it exports the builders)
// To the briefs (world: Workshops' assembly line, Quarters' alcove walls and trinkets, brass makers' plaques, dead vines in
// the Quarters' alcoves, cables rising up lattice masts; blocks: Worker, Charging Alcove, Derelict Cable, brackets that bite):
//   1. Assembly Clamp, stages 1-5: a void-alloy jig (a footplate with foot sockets, two uprights, hip and chest jaws) holding a
//      Worker at five stages of being built: the armature; + the chest cage; + the core and upper arms; plated, forearms, the
//      head with an empty lens socket; whole, its lens in. A power lead hangs from the east upright: waiting (2), plugged into
//      the core (3), stowed (1, 4, 5). The body is the Worker Shell's own rig, standing straight.
//   2. Alcove trinkets: the Charging Alcove with a small cast corner shelf (in place of its thin side ledge) holding a brass
//      cog, a winding key, a toy cart, a pressed flower in a little brass frame (or loose, the option).
//   3. Brass makers' plaque: aged brass, a raised rim, three worn raised bars (no letters), three steel bolts (one lost);
//      an askew option hanging from one bolt.
//   4. Dead vines: dry olive strands and shrivelled leaves, drawn as cutout cards: in the alcove (down its back and cheek, over
//      its front, one strand hanging in the opening) and as a vine block on a makers' wall (the option).
//   5. Cable riser up a lattice mast: the yard's ground cable (the Derelict Cable's strand pair) rising up a Create Metal
//      Girder's face in a void-alloy riser guard, held off the girder by clamps that bite its edges, ending in a pothead
//      (cable termination) under the top insulator, with a jumper up to the insulator's tie.
// Block model space (x 0 west -> 16 east, y up, z 0 north -> 16 south), fronts to the north. Glows are shade:false parts in
// their own groups; clips 'dormant' and 'awake'.
const path = require('path');
const fs = require('fs');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const CITY = require('./clockwork-wastes-city.gen.js');
const INS = require('./clockwork-wastes-insulator.gen.js');
// the salvage set last: its aged 'brass' and the Worker's 'lens_ring' are the ones the shells use
const S = require('./clockwork-wastes-salvage.gen.js');
const { T, VA, STAR, STEEL, BRASS_AGED, brassAged, PATINA, RUST, box } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-city-props.bbmodel');
const GLOW = { shade: false };

// ---------- textures (keys prefixed cp_) ----------
// an empty lens socket: the dark of the head's inside, no glass
T.paint('cp_socket', (x, y) => (hash(x, y, 301) > 0.85 ? '#16151c' : '#121118'));
// the Worker's core: a void-alloy cell banded by dark seams (never lit: it has never run)
T.paint('cp_core', (x, y) => (y % 3 === 0 ? VA.lo : (x + y) % 6 < 3 ? '#4c475f' : '#3a3650'));
// the jig's power lead: copper drawn from the makers' pipes, gone to patina (the Derelict Cable's)
T.paint('cp_lead', (x, y) => (hash(x, y, 302) > 0.55 ? PATINA[1] : PATINA[0]));
T.paint('cp_brass', brassAged(310));
// the plaque: a recessed field of old brass with patina in it; the rim and the bars rubbed smooth
T.paint('cp_plaque_field', (x, y) => { const r = hash(x, y, 311); return r > 0.965 ? PATINA[0] : r > 0.85 ? '#5f4d2e' : BRASS_AGED[0]; });
T.paint('cp_plaque_rim', (x, y) => (hash(x, y, 312) > 0.985 ? PATINA[0] : hash(x, y, 313) > 0.7 ? BRASS_AGED[2] : BRASS_AGED[1]));
T.paint('cp_plaque_bar', (x, y) => (hash(x, y, 314) > 0.75 ? BRASS_AGED[1] : BRASS_AGED[2]));
T.paint('cp_bolt', (x, y) => (hash(x, y, 315) > 0.6 ? RUST[0] : STEEL[2]));
// the pressed flower, 3 x 4 texels at uv 0-3 / 0-4: faded mauve petals, a dark heart, an olive stem and leaf, on old paper
const FLOWER = ['pMp', 'MmM', 'pSL', 'pSp'];
T.paint('cp_flower_card', (x, y) => ({ p: '#5a5540', M: '#8a6a72', m: '#5e4850', S: '#4f5236', L: '#5b5d40' })[(FLOWER[y] || '')[x]] || '#5a5540');
T.paint('cp_flower', (x, y) => (hash(x, y, 316) > 0.5 ? '#8a6a72' : '#7a5c64'));
T.paint('cp_stem', () => '#4f5236');
// dead vines: dry woody strands every 4 px with shrivelled leaves, some browned. 'upper' leaves the top full; 'lower' carries
// the same strands on and ends them at different heights with a curl; 'hang' is two strands hanging free
const VINE = { stem: '#36372a', stemLit: '#45462f', leaf: '#4f5236', leafLit: '#5b5d40', brown: '#53472f', tip: '#3f3a2a' };
const WANDER = [0, 0, 1, 1, 1, 0, 0, -1, -1, -1, 0, 0, 1, 1, 0, 0];
function vinePainter(strands) {
  return (x, y) => {
    for (const s of strands) {
      if (y > s.end) continue;
      const sx = s.x + WANDER[(y + s.ph) % 16];
      if (x === sx) return y === s.end ? VINE.tip : hash(x, y, s.ph) > 0.7 ? VINE.stemLit : VINE.stem;
      // the curl at a strand's end: a hook back up
      if (y === s.end && x === sx + s.side) return VINE.tip;
      if (y === s.end - 1 && x === sx + s.side * 2 && s.end < 15) return VINE.stem;
      // leaves every 5 rows, alternating sides, two pixels; one in three gone brown
      const k = (y + s.ph * 3) % 5;
      const side = ((y + s.ph) >> 2) % 2 ? 1 : -1;
      if (y < s.end - 1 && k === 0 && (x === sx + side || (x === sx + side * 2 && hash(sx, y, s.ph) > 0.4))) return hash(sx, y, s.ph + 9) > 0.66 ? VINE.brown : x === sx + side ? VINE.leaf : VINE.leafLit;
      if (y < s.end - 1 && k === 1 && x === sx + side) return VINE.leaf;
    }
    return null;
  };
}
T.paint('cp_vine_upper', vinePainter([{ x: 1, ph: 0, end: 99, side: 1 }, { x: 5, ph: 5, end: 99, side: 1 }, { x: 9, ph: 11, end: 99, side: -1 }, { x: 13, ph: 3, end: 99, side: -1 }]));
T.paint('cp_vine_lower', vinePainter([{ x: 1, ph: 0, end: 13, side: 1 }, { x: 5, ph: 5, end: 7, side: 1 }, { x: 9, ph: 11, end: 15, side: -1 }, { x: 13, ph: 3, end: 10, side: -1 }]));
T.paint('cp_vine_hang', vinePainter([{ x: 4, ph: 7, end: 12, side: 1 }, { x: 10, ph: 2, end: 15, side: -1 }]));

// ---------- helpers ----------
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]), rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const named = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));
const rotAll = (cubes, axis, angle, origin) => cubes.map((c) => ({ ...c, rot: [axis, angle, origin] }));
// a chamfered upright: two crossed boxes, the corners clipped by c
const cham = (name, x0, z0, x1, z1, y0, y1, tex, c = 0.5) => [box(name + '_x', [x0, y0, z0 + c], [x1, y1, z1 - c], tex), box(name + '_z', [x0 + c, y0, z0], [x1 - c, y1, z1], tex)];
// the Worker as built: its rig flattened, every bone at rest (the Worker Shell's own parts and textures)
function workerParts() {
  const out = [];
  const walk = (n) => { out.push(...(n.c || [])); (n.k || []).forEach(walk); };
  walk(S.worker());
  return out;
}

// ---------- 1. Assembly Clamp ----------
// The jig (the uprising's: cast void-alloy, no bolts): a footplate round two foot sockets, two chamfered uprights behind the
// arms' line, hip jaws on the pelvis, chest jaws on the chest (open while there's no chest), both passing behind the arms.
function jig(chestOpen) {
  const plate = { all: 'va', down: null };
  const out = [
    // the footplate, in four pieces round the Worker's feet (x 6-10, z 5.5-9), its outer corners stepped in
    box('plate_front', [1.5, 0, 4], [14.5, 1, 5.5], plate), box('plate_back', [1.5, 0, 9], [14.5, 1, 12], plate),
    box('plate_w', [1, 0, 5.5], [6, 1, 9], plate), box('plate_e', [10, 0, 5.5], [15, 1, 9], plate),
    // two uprights and their caps
    ...cham('upright_w', 1, 8.5, 3, 11.5, 1, 29, { all: M.VA_BODY, down: null, up: 'va' }),
    ...cham('upright_e', 13, 8.5, 15, 11.5, 1, 29, { all: M.VA_BODY, down: null, up: 'va' }),
    box('cap_w', [1.25, 29, 9.25], [2.75, 30, 10.75], 'va'), box('cap_e', [13.25, 29, 9.25], [14.75, 30, 10.75], 'va'),
    // hip jaws: an arm from each upright to a pad on the pelvis's side
    box('hip_arm_w', [3, 16.25, 9.25], [5, 17.75, 10.5], 'va'), box('hip_pad_w', [5, 15.75, 9], [6, 18.25, 10.75], 'va'),
    box('hip_arm_e', [11, 16.25, 9.25], [13, 17.75, 10.5], 'va'), box('hip_pad_e', [10, 15.75, 9], [11, 18.25, 10.75], 'va'),
  ];
  // chest jaws: on the chest's back corners; open (turned 22.5 out about the upright) while there's no chest to hold
  const jaw = (side) => {
    const w = side === 'w';
    const parts = [box('chest_arm_' + side, w ? [3, 22.25, 9.5] : [12, 22.25, 9.5], w ? [4, 23.75, 10.75] : [13, 23.75, 10.75], 'va'),
      box('chest_pad_' + side, w ? [4, 21.75, 9] : [11, 21.75, 9], w ? [5, 24.25, 10.75] : [12, 24.25, 10.75], 'va')];
    return chestOpen ? rotAll(parts, 'y', w ? 22.5 : -22.5, [w ? 3 : 13, 23, 10.75]) : parts;
  };
  out.push(...jaw('w'), ...jaw('e'));
  return out;
}
// the power lead from the east upright: 'stowed' (a short loop parked on the upright), 'waiting' (hanging by the empty cage, its
// plug free), 'plugged' (in the core's front)
function lead(state) {
  const L = 'cp_lead', P = 'va';
  const out = [box('lead_socket', [13.5, 26.5, 7.75], [14.5, 28, 8.5], 'va')];
  if (state === 'stowed') {
    out.push(box('lead_a', [13.625, 23.5, 7.25], [14.375, 27, 7.75], L), box('lead_plug', [13.375, 22, 7], [14.625, 23.5, 8], P));
    return out;
  }
  // out of the socket, forward to z 5.5, across west above the shoulder
  out.push(box('lead_a', [13.625, 27, 5.25], [14.375, 27.75, 7.75], L));
  if (state === 'waiting') {
    out.push(box('lead_b', [12, 27, 5.25], [13.625, 27.75, 6], L), box('lead_c', [12, 22.5, 5.25], [12.75, 27, 6], L),
      box('lead_plug', [11.75, 21, 5.25], [13, 22.5, 6.5], P));
  } else {
    out.push(box('lead_b', [8.75, 27, 5.25], [13.625, 27.75, 6], L), box('lead_c', [8, 24, 5.25], [8.75, 27.75, 6], L),
      box('lead_plug', [7.5, 22.5, 5], [9.5, 24, 7], P));
  }
  return out;
}
// the chest cage (stage 2-3): two rings of bars and four corner posts round the spine, open to every side
function cage() {
  const t = 0.75;
  const ring = (n, y0, y1) => [box(n + '_n', [5, y0, 6], [11, y1, 6 + t], 'va'), box(n + '_s', [5, y0, 10 - t], [11, y1, 10], 'va'),
    box(n + '_w', [5, y0, 6 + t], [5 + t, y1, 10 - t], 'va'), box(n + '_e', [11 - t, y0, 6 + t], [11, y1, 10 - t], 'va')];
  return [...ring('cage_lo', 20, 21), ...ring('cage_hi', 25, 26),
    box('cage_nw', [5, 21, 6], [5 + t, 25, 6 + t], 'va'), box('cage_ne', [11 - t, 21, 6], [11, 25, 6 + t], 'va'),
    box('cage_sw', [5, 21, 10 - t], [5 + t, 25, 10], 'va'), box('cage_se', [11 - t, 21, 10 - t], [11, 25, 10], 'va'),
    box('cage_rib_w', [5, 22.75, 6 + t], [5 + t, 23.25, 10 - t], 'va'), box('cage_rib_e', [11 - t, 22.75, 6 + t], [11, 23.25, 10 - t], 'va')];
}
// stage 1-5: the jig, the body at that stage, the lead
const W = Object.fromEntries(workerParts().map((c) => [c.name, c]));
const pick = (...names) => names.map((n) => { if (!W[n]) throw new Error('no worker part ' + n); return W[n]; });
const LEGS = ['thigh_w', 'shin_w', 'foot_w', 'thigh_e', 'shin_e', 'foot_e'];
const UPPER_ARMS = ['shoulder_w', 'upper_w', 'shoulder_e', 'upper_e'];
const FOREARMS = ['fore_w', 'clamp_w_a', 'clamp_w_b', 'fore_e', 'clamp_e_a', 'clamp_e_b'];
const LENS = Object.keys(W).filter((n) => n.startsWith('lens_ring') || n.startsWith('lens_'));
function assemblyStage(n) {
  const body = [...pick('pelvis', ...LEGS)];
  if (n === 1) body.push(box('spine_long', [7.25, 18, 7.25], [8.75, 26.5, 8.75], 'va'), box('yoke', [4.5, 24.5, 7], [11.5, 26, 9], 'va'));
  if (n >= 2 && n <= 3) body.push(box('spine_long', [7.25, 18, 7.25], [8.75, 26.5, 8.75], 'va'), ...cage(), ...pick('neck'));
  // the wiring harness up the spine's front (the makers' copper, gone to patina): in before the plates, round the core in 3
  if (n <= 3) body.push(...(n === 3 ? [[18, 21.5], [24.5, 26.5]] : [[18, 26.5]]).map(([a, b], k) => box('harness_' + k, [7.5, a, 6.75], [8.5, b, 7.25], { all: 'cp_lead', up: null, down: null })));
  if (n === 3) body.push(box('core', [6.5, 21.5, 7], [9.5, 24.5, 9], 'cp_core'), ...pick(...UPPER_ARMS));
  if (n >= 4) {
    body.push(...pick('spine', 'chest', 'chest_cap', 'neck', 'skull', ...UPPER_ARMS, ...FOREARMS));
    if (n === 4) body.push(...M.zdisc('socket', 8, 30.25, 5.25, 5.5, 5, { north: 'cp_socket', all: null }));
    else body.push(...pick('crest', 'tag', ...LENS));
  }
  return [...jig(n === 1), ...body, ...lead(n === 2 ? 'waiting' : n === 3 ? 'plugged' : 'stowed')];
}

// ---------- 2. Alcove trinkets ----------
// the Charging Alcove as approved, its thin side ledge and toy cart taken off (they move to the shelf and its trinket state)
const LEDGE = ['ledge', 'cart', 'cart_wheel_a', 'cart_wheel_b'];
const alcoveBase = () => S.alcove().filter((c) => !LEDGE.includes(c.name));
// a small cast corner shelf in the back-east corner at chest height, on a stepped bracket
const shelf = () => [box('shelf', [9, 17, 9], [13, 18, 12.75], { all: 'va', up: M.VA_BODY }),
  box('shelf_bracket_a', [11, 16, 10], [13, 17, 12.75], { all: 'va', up: null }), box('shelf_bracket_b', [12, 15, 11], [13, 16, 12.75], { all: 'va', up: null })];
// the trinkets, on the shelf top (y 18, x 9-13, z 9-12.75)
function trinket(kind) {
  if (kind === 'cog') {
    // a brass cog standing on its edge against the back wall: a round hub, eight teeth (bars through it, 0.25 in from its faces)
    const cx = 11, cy = 20, z0 = 11.75, z1 = 12.5, tz0 = 12, tz1 = 12.25;
    const bar = (n, o = {}) => box('cog_' + n, [cx - 0.375, cy - 2, tz0], [cx + 0.375, cy + 2, tz1], 'cp_brass', o);
    return [box('cog_hub_a', [cx - 1.5, cy - 1, z0], [cx + 1.5, cy + 1, z1], 'cp_brass'), box('cog_hub_b', [cx - 1, cy - 1.5, z0], [cx + 1, cy + 1.5, z1], 'cp_brass'),
      bar('t_v'), box('cog_t_h', [cx - 2, cy - 0.375, tz0], [cx + 2, cy + 0.375, tz1], 'cp_brass'),
      { ...bar('t_d1'), from: [cx - 0.375, cy - 1.75, tz0], to: [cx + 0.375, cy + 1.75, tz1], rot: ['z', 45, [cx, cy, 12.125]] },
      { ...bar('t_d2'), from: [cx - 0.375, cy - 1.75, tz0], to: [cx + 0.375, cy + 1.75, tz1], rot: ['z', -45, [cx, cy, 12.125]] },
      box('cog_boss', [cx - 0.5, cy - 0.5, 11.5], [cx + 0.5, cy + 0.5, 11.75], { north: 'key_hole', all: 'cp_brass', south: null })];
  }
  if (kind === 'key') {
    // the resistance's butterfly clock key, small, stood on its socket and leaning back against the wall, its bow up
    const k = [box('key_socket', [10.5, 18, 11], [11.5, 19.25, 12], { all: 'cp_brass', down: 'key_hole' }), box('key_shaft', [10.75, 19.25, 11.25], [11.25, 20.75, 11.75], 'cp_brass'),
      box('key_boss', [10.5, 20.75, 11.25], [11.5, 21.25, 11.75], 'cp_brass'), box('key_bow', [10.25, 21.25, 11.25], [11.75, 22.5, 11.75], 'cp_brass'),
      box('key_wing_w', [9.25, 21, 11.25], [10.25, 23.25, 11.75], 'cp_brass'), box('key_wing_e', [11.75, 21, 11.25], [12.75, 23.25, 11.75], 'cp_brass')];
    return rotAll(k, 'x', 22.5, [11, 18, 11.5]);
  }
  if (kind === 'cart') {
    // the makers' child's toy: an open walnut box on four brass wheels, a pull handle toward the street
    return [box('cart_floor', [10, 18.75, 9.5], [12, 19.25, 12.5], 'walnut'),
      box('cart_wall_w', [10, 19.25, 9.5], [10.5, 20.25, 12.5], 'walnut'), box('cart_wall_e', [11.5, 19.25, 9.5], [12, 20.25, 12.5], 'walnut'),
      box('cart_wall_n', [10.5, 19.25, 9.5], [11.5, 20, 10], 'walnut'), box('cart_wall_s', [10.5, 19.25, 12], [11.5, 20, 12.5], 'walnut'),
      ...[['nw', 9.5, 9.75], ['ne', 12, 9.75], ['sw', 9.5, 11.25], ['se', 12, 11.25]].map(([n, x, z]) => box('cart_wheel_' + n, [x, 18, z], [x + 0.5, 19.25, z + 1.25], 'cp_brass')),
      box('cart_tongue', [10.75, 18.75, 8.25], [11.25, 19.25, 9.5], 'walnut'), box('cart_handle', [10.25, 18.75, 7.75], [11.75, 19.25, 8.25], 'walnut')];
  }
  if (kind === 'flower') {
    // a pressed flower in a little brass frame, leaning back against the wall: the only colour in the city, faded
    const f = [box('frame_t', [9.5, 22, 10], [12.5, 22.5, 10.75], 'cp_brass'), box('frame_b', [9.5, 18, 10], [12.5, 18.5, 10.75], 'cp_brass'),
      box('frame_w', [9, 18, 10], [9.5, 22.5, 10.75], 'cp_brass'), box('frame_e', [12.5, 18, 10], [13, 22.5, 10.75], 'cp_brass'),
      box('frame_card', [9.5, 18.5, 10.25], [12.5, 22, 10.75], { north: 'cp_flower_card', south: 'walnut', all: null }, { uv: { north: [0, 0, 3, 4], south: [0, 0, 3, 4] } })];
    return rotAll(f, 'x', 22.5, [11, 18, 10.75]);
  }
  if (kind === 'flower_loose') {
    // the option: the flower itself, dried, lying on the shelf
    const f = [box('stem', [10.75, 18, 10.25], [11.25, 18.5, 12.25], 'cp_stem'), box('leaf', [11.25, 18, 11.25], [12.25, 18.25, 11.75], 'cp_stem'),
      box('head', [10.25, 18, 9.25], [11.75, 19, 10.25], 'cp_flower')];
    return rotAll(f, 'y', 22.5, [11, 18, 10.75]);
  }
  return [];
}
// dead vines in an alcove: down the back's west side, down the west cheek, over the west cheek's front, and one strand hanging
// in the opening from the arch. Cutout cards, each 0.25 in front of what it lies on; the upper block's cells use 'upper'
function alcoveVines() {
  return [
    box('vine_back_hi', [3, 32, 12.75], [6, 44, 12.75], { north: 'cp_vine_upper' }), box('vine_back_lo', [3, 20, 12.75], [6, 32, 12.75], { north: 'cp_vine_lower' }),
    box('vine_cheek_hi', [3.25, 32, 6], [3.25, 40, 12.75], { east: 'cp_vine_upper' }), box('vine_cheek_lo', [3.25, 25, 6], [3.25, 32, 12.75], { east: 'cp_vine_lower' }),
    box('vine_front_hi', [1, 32, 3.75], [3, 44, 3.75], { north: 'cp_vine_upper' }), box('vine_front_lo', [1, 26, 3.75], [3, 32, 3.75], { north: 'cp_vine_lower' }),
    box('vine_hang', [5, 33, 6], [9, 44, 6], { north: 'cp_vine_hang', south: 'cp_vine_hang' }, { uv: { north: [8, 0, 12, 11], south: [12, 0, 8, 11] } }),
  ];
}

// ---------- 3. Brass makers' plaque (on a wall to its south) ----------
function plaque() {
  const out = [box('plate', [2, 3, 15.25], [14, 12, 16], { all: 'cp_plaque_field', south: null }),
    box('rim_t', [2, 11, 14.75], [14, 12, 15.25], { all: 'cp_plaque_rim', south: null }), box('rim_b', [2, 3, 14.75], [14, 4, 15.25], { all: 'cp_plaque_rim', south: null }),
    box('rim_w', [2, 4, 14.75], [3, 11, 15.25], { all: 'cp_plaque_rim', south: null }), box('rim_e', [13, 4, 14.75], [14, 11, 15.25], { all: 'cp_plaque_rim', south: null })];
  // three lines of worn raised bars, no letters
  for (const [n, x0, x1, y] of [['l1', 4, 12, 9], ['l2', 4, 10, 7], ['l3a', 4, 7, 5], ['l3b', 8, 11, 5]])
    out.push(box('bar_' + n, [x0, y, 15], [x1, y + 1, 15.25], { all: 'cp_plaque_bar', south: null }));
  // bolt heads on three corners; the fourth lost
  for (const [n, x, y] of [['nw', 2.25, 11.25], ['ne', 13.25, 11.25], ['sw', 2.25, 3.25]])
    out.push(box('bolt_' + n, [x, y, 14.25], [x + 0.5, y + 0.5, 14.75], { all: 'cp_bolt', south: null }));
  return out;
}
// the option: hanging askew from its top-west bolt, the others gone
const plaqueAskew = () => rotAll(plaque().filter((c) => !['bolt_ne', 'bolt_sw'].includes(c.name)), 'z', -22.5, [2.5, 11.5, 15]);

// ---------- 4. a Dead Vine block (the option): cards 0.8 px off the wall to its south, as vanilla vines ----------
const vineBlock = (lower) => [box('vine', [0, 0, 15.2], [16, 16, 15.2], { north: lower ? 'cp_vine_lower' : 'cp_vine_upper' })];

// ---------- 5. Cable riser up a lattice mast ----------
// On the north face of a Create Metal Girder column (x 4-12, z 4-12) H blocks tall, an insulator on top (block H). The strand
// pair is the Derelict Cable's (two 0.75 strands, a 0.5 gap with the lit line in it), 0.75 off the girder's face.
const SX = [[7, 7.75], [8.25, 9]], CZ = [2.25, 3.25];
const strandsY = (n, y0, y1) => SX.map(([a, b], i) => box(`${n}_${i}`, [a, y0, CZ[0]], [b, y1, CZ[1]], { all: 'cable', up: null, down: null }));
const lineY = (n, y0, y1) => [box(n, [7.75, y0, 2.5], [8.25, y1, 2.5], { north: 'seam_strip_lit' }, GLOW)];
// a clamp: a strap across the cable's front, a standoff behind it, legs that wrap the girder's edges and bite its sides
const clamp = (n, y) => [box(n + '_strap', [6, y, 1.75], [10, y + 1.5, 2.25], { all: 'va', south: null }),
  box(n + '_stand', [7, y, 3.25], [9, y + 1.5, 4], 'va'), box(n + '_bar', [3.5, y, 3.25], [7, y + 1.5, 4], { all: 'va', south: null }), box(n + '_bar_e', [9, y, 3.25], [12.5, y + 1.5, 4], { all: 'va', south: null }),
  box(n + '_leg_w', [3.5, y, 4], [4, y + 1.5, 5.5], 'va'), box(n + '_leg_e', [12, y, 4], [12.5, y + 1.5, 5.5], 'va'),
  box(n + '_side_w', [6, y, 2.25], [7, y + 1.5, 3.25], 'va'), box(n + '_side_e', [9, y, 2.25], [10, y + 1.5, 3.25], 'va')];
function riser(H) {
  const top = H * 16, P0 = top - 11, GUARD = 19;
  const body = [], line = [];
  // the riser guard: a U channel over the cable's foot, open to the girder, a stepped lip at its top
  body.push(box('guard_front', [6, 0, 1.25], [10, GUARD, 1.75], { all: M.VA_BODY, south: null, down: null }),
    box('guard_w', [6, 0, 1.75], [6.75, GUARD, 4], { all: M.VA_BODY, down: null }), box('guard_e', [9.25, 0, 1.75], [10, GUARD, 4], { all: M.VA_BODY, down: null }),
    box('guard_lip', [6, GUARD, 1.25], [10, GUARD + 0.75, 2.25], { all: 'va', down: null }));
  // the cable from the floor (inside the guard) to the pothead, and its lit line (hidden behind the guard where it's covered)
  body.push(...strandsY('riser', 0, P0));
  line.push(...lineY('riser_line', GUARD + 0.75, P0));
  // a clamp each block above the guard, and one at the guard's head biting the girder
  for (let y = 22; y < P0 - 2; y += 16) body.push(...clamp('clamp_' + y, y));
  body.push(box('guard_strap', [3.5, 15, 0.75], [12.5, 16.5, 1.25], { all: 'va', south: null }),
    box('guard_leg_w', [3.5, 15, 1.25], [4, 16.5, 5.5], 'va'), box('guard_leg_e', [12, 15, 1.25], [12.5, 16.5, 5.5], 'va'));
  // the pothead: a cast termination with two small sheds, the strands going in at its foot, the jumper out at its cap
  body.push(...clamp('clamp_pot', P0 - 1.5),
    ...cham('pot_body', 6.5, 1.25, 9.5, 3.75, P0, P0 + 4.5, 'va'),
    ...cham('pot_shed_a', 5.5, 0.75, 10.5, 4, P0 + 1, P0 + 1.75, { all: 'va', up: 'va_facet' }, 0.75),
    ...cham('pot_shed_b', 5.5, 0.75, 10.5, 4, P0 + 2.75, P0 + 3.5, { all: 'va', up: 'va_facet' }, 0.75),
    box('pot_cap', [7, P0 + 4.5, 1.5], [9, P0 + 5, 3], 'va'));
  // the jumper: the pair joins into one conductor at the pothead; it goes up under the collar, out to the girder's east
  // edge, up beside the insulator (never in front of its stack), back at 22.5 degrees, and in to the tie knob's east face
  const J0 = P0 + 5, JB = top - 2.5, JC = top + 8.5, run = 6, rise = run * Math.tan(Math.PI / 8);
  const jc = { all: 'cable', up: null, down: null };
  body.push(box('jumper_a', [7.5, J0, 1.5], [8.5, JB, 2.5], jc),
    box('jumper_b', [7.5, JB, 1.5], [13.5, JB + 1, 2.5], { all: 'cable', west: null }),
    box('jumper_c', [12.5, JB + 1, 1.5], [13.5, JC, 2.5], jc));
  const len = run / Math.cos(Math.PI / 8), mz = 2 + run / 2, my = JC + rise / 2;
  body.push(box('jumper_d', [12.5, my - 0.5, mz - len / 2], [13.5, my + 0.5, mz + len / 2], { all: 'cable', north: null, south: null }, { rot: ['x', -22.5, [13, my, mz]] }));
  const ye = JC + rise;
  body.push(box('jumper_e', [9.5, ye - 0.5, 7.5], [13.5, ye + 0.5, 8.5], { all: 'cable', east: null, west: null }),
    // a void-alloy wrap where it ties on
    box('jumper_wrap', [9.5, ye - 0.75, 7.25], [10.25, ye + 0.75, 8.75], 'va'));
  return { body, line };
}
// the ground cable: an axis-aligned run of the strand pair on the floor, its lit line in the gap; cleats at every corner
function groundRun(n, a, b) {
  const alongX = a[1] === b[1];
  const [p0, p1] = alongX ? [Math.min(a[0], b[0]), Math.max(a[0], b[0])] : [Math.min(a[1], b[1]), Math.max(a[1], b[1])];
  const c = alongX ? a[1] : a[0];
  const span = (o0, o1) => (alongX ? [[p0, 0, c + o0], [p1, 1, c + o1]] : [[c + o0, 0, p0], [c + o1, 1, p1]]);
  const strand = (k, o0, o1) => { const [f, t] = span(o0, o1); return box(`${n}_${k}`, f, t, { all: 'cable', up: 'cable_top', down: null }); };
  const [gf, gt] = span(-0.25, 0.25);
  return { body: [strand('a', -1, -0.25), strand('b', 0.25, 1)], line: [box(n + '_line', [gf[0], 0.75, gf[2]], [gt[0], 0.75, gt[2]], { up: 'seam_strip_lit' }, GLOW)] };
}
const cleat = (n, x, z) => [box(n, [x - 1.5, 0, z - 1.5], [x + 1.5, 1.5, z + 1.5], { all: 'va', down: null })];

module.exports = { assemblyStage, jig, lead, cage, workerParts, alcoveBase, shelf, trinket, alcoveVines, plaque, plaqueAskew, vineBlock, riser, clamp, groundRun, cleat };

// ---------- the scene ----------
if (require.main === module) {
const { cube, group, clip, write } = preview('clockwork_wastes_city_props', { 'w:': T });
const outliner = [];
const REST = {};
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const G = (name, off, list, pivot = [8, 0, 8]) => { const g = group(name, pivot, off, put(named(list, name + '_'), off)); outliner.push(g); return g; };
const sub = (name, pivot, off, list) => group(name, pivot, off, put(named(list, name + '_'), off));
const LIT = [];
function rig(prefix, node, off, pose) {
  const name = `${prefix}_${node.n}`;
  if (pose.r[node.n]) REST[name] = pose.r[node.n];
  return group(name, node.p, off, [...put(named(node.c || [], prefix + '_'), off), ...(node.k || []).map((k) => rig(prefix, k, off, pose))]);
}
const girder = (name, top) => [box(name, [4, 0, 4], [12, 16, 12], { all: 'create:block/girder_pole_side', up: top ? 'create:block/girder_pole' : null, down: null },
  { uv: { north: [4, 0, 12, 16], south: [4, 0, 12, 16], east: [4, 0, 12, 16], west: [4, 0, 12, 16], up: [8, 0, 16, 8], down: [8, 0, 16, 8] } })];
const girderX = (name) => [box(name, [0, 4, 4], [16, 12, 12], { all: 'create:block/girder_pole_side', east: null, west: null }, { uv: { north: [4, 0, 12, 16], south: [4, 0, 12, 16], up: [4, 0, 12, 16], down: [4, 0, 12, 16] }, frot: { north: 90, south: 90, up: 90, down: 90 } })];

// floor
{
  const floor = [];
  for (let x = -3; x <= 20; x++) for (let z = -4; z <= 19; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
  outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
}

// row A (z 0): the assembly line under a girder gantry: five clamps, three blocks apart; a Worker Shell `frozen` beside it
{
  const kids = [];
  for (let y = 0; y < 7; y++) kids.push(...put(named(girder('g', false), `gantry_w${y}_`), at(-1, y, 0)), ...put(named(girder('g', false), `gantry_e${y}_`), at(15, y, 0)));
  for (let x = -1; x <= 15; x++) kids.push(...put(named(girderX('beam'), `gantry_b${x}_`), at(x, 7, 0)));
  outliner.push(group('gantry', [8, 0, 8], at(-1, 0, 0), kids));
  for (let i = 1; i <= 5; i++) G('assembly_stage_' + i, at(3 * (i - 1) + 1, 0, 0), assemblyStage(i));
  const p = S.POSES.worker_frozen, off = at(18, 0, 0).map((v, i) => v + p.lift[i]);
  outliner.push(rig('family_worker_frozen', S.worker(), off, p));
}

// row B (z 5): an alcove wall: the alcove as built (side ledge, toy cart), then the proposal: empty shelf, cog, key, toy cart,
// pressed flower (framed), pressed flower (loose, the option); vines on two; a slumped Worker before one (the vigil)
{
  const R = 5;
  const al = (name, bx, extra, vines) => {
    const off = at(bx, 0, R);
    const kids = put(named([...(extra === 'as_built' ? S.alcove() : [...alcoveBase(), ...shelf(), ...trinket(extra)]), ...(vines ? alcoveVines() : [])], name + '_'), off);
    kids.push(sub('lit_' + name, [8, 24, 12], off, S.alcoveLit()));
    LIT.push('lit_' + name);
    outliner.push(group(name, [8, 0, 8], off, kids));
  };
  al('alcove_as_built', 0, 'as_built');
  al('alcove_empty', 1, 'none', true);
  al('alcove_cog', 2, 'cog');
  al('alcove_key', 3, 'key');
  al('alcove_cart', 4, 'cart', true);
  al('alcove_flower', 5, 'flower');
  al('alcove_flower_loose', 6, 'flower_loose');
  // the wall's seamed course over the tier, as the city builds it
  (G('alcove_course', at(0, 3, R), Array.from({ length: 7 }, (_, i) => box('course_' + i, [i * 16, 0, 0], [i * 16 + 16, 16, 16], { all: 'va_plain', north: 'va_seamed' }))));
  const p = S.POSES.worker_slumped, off = at(1, 0, R - 1).map((v, i) => v + p.lift[i]);
  outliner.push(rig('family_worker_slumped', S.worker(), off, p));
}

// row C (z 10): the makers' walls: stone brick with a bricked-up doorway and a plaque beside it; Create andesite casing with
// a plaque and the askew option; dead vine blocks (the option) on the brick; the Maker Statue for its aged brass
{
  const R = 10;
  const wall = [];
  for (let x = 0; x < 4; x++) for (let y = 0; y < 3; y++) {
    const door = x === 1 && y < 2;
    wall.push(box(`brick_${x}_${y}`, [x * 16, y * 16, 16], [x * 16 + 16, y * 16 + 16, 32], door ? (y === 0 ? 'minecraft:block/cracked_stone_bricks' : 'minecraft:block/mossy_stone_bricks') : 'minecraft:block/stone_bricks'));
  }
  // the bricked-up doorway's lintel and jambs stand a little proud, so the infill reads as a doorway filled in
  wall.push(box('lintel', [15, 32, 15], [33, 34, 16], 'minecraft:block/smooth_stone'), box('jamb_w', [15, 0, 15], [16, 32, 16], 'minecraft:block/smooth_stone'), box('jamb_e', [32, 0, 15], [33, 32, 16], 'minecraft:block/smooth_stone'));
  G('makers_wall_brick', at(0, 0, R), wall);
  G('plaque_on_brick', at(2, 1, R), plaque());
  // dead vine blocks over the wall's west end, two blocks down from the top
  G('vine_block_hi', at(0, 2, R), vineBlock(false));
  G('vine_block_lo', at(0, 1, R), vineBlock(true));
  const casing = [];
  for (let x = 0; x < 3; x++) for (let y = 0; y < 2; y++) casing.push(box(`casing_${x}_${y}`, [x * 16, y * 16, 16], [x * 16 + 16, y * 16 + 16, 32], 'create:block/andesite_casing'));
  G('makers_wall_casing', at(5, 0, R), casing);
  G('plaque_on_casing', at(5, 1, R), plaque());
  G('plaque_askew', at(7, 1, R), plaqueAskew());
  G('family_statue', at(9, 0, R), S.statue());
}

// row D (z 15): the Substation yard's mast: the Substation, its tray span to the mast's top insulator, the yard's ground
// cable from the Substation's front to the mast's foot, rising up the mast to the top insulator's tie
{
  const R = 15, MX = 4, H = 5;
  const s = CITY.substation(), soff = at(0, 0, R);
  outliner.push(group('substation', [8, 0, 8], soff, [...put(s.body, soff), group('lever', [16, 20, 8], soff, put(s.lever, soff)),
    sub('lit_sub_seams', [8, 0, 1], soff, s.seams), sub('lit_sub_sigil', [8, 27, 1], soff, s.sigil), sub('lit_sub_column', [8, 3.5, 1.25], soff, s.column)]));
  LIT.push('lit_sub_seams', 'lit_sub_sigil', 'lit_sub_column');
  // the mast: H girders, the top insulator
  const mkids = [];
  for (let i = 0; i < H; i++) mkids.push(...put(named(girder('g', false), `girder_${i}_`), at(MX, i, R)));
  const t = INS.topMount(), toff = at(MX, H, R);
  mkids.push(group('ins_mast', [8, 0, 8], toff, [...put(named(t.body, 'ins_'), toff), sub('lit_ins_ring', [8, 8, 8], toff, t.ring)]));
  LIT.push('lit_ins_ring');
  // the riser
  const r = riser(H), roff = at(MX, 0, R);
  mkids.push(group('riser', [8, 0, 8], roff, [...put(r.body, roff), sub('lit_riser', [8, 0, 2], roff, r.line)]));
  LIT.push('lit_riser');
  outliner.push(group('mast', [8, 0, 8], at(MX, 0, R), mkids));
  // the ground cable: out of the Substation's front, north 6 px, east to the mast's line, south to the riser guard
  const z0 = R * 16, gx = MX * 16 + 8, gz = z0 - 6;
  const runs = [groundRun('gc0', [12, z0 + 2], [12, gz + 1.5]), groundRun('gc1', [13.5, gz], [gx - 1.5, gz]), groundRun('gc2', [gx, gz + 1.5], [gx, z0 + 1.25])];
  const gbody = [...runs.flatMap((q) => q.body), ...cleat('cleat_a', 12, gz), ...cleat('cleat_b', gx, gz),
    ...[28, 44].map((x) => cleat('clip_' + x, x, gz)).flat()];
  outliner.push(group('ground_cable', [0, 0, 0], [0, 0, 0], [...put(gbody, [0, 0, 0]), group('lit_ground', [0, 0, 0], [0, 0, 0], put(runs.flatMap((q) => q.line), [0, 0, 0]))]));
  LIT.push('lit_ground');
  // the tray span (approved, D64): from the tray's east insulator to the mast's tie, a sagging strand pair along +x
  const A = [11, 34, z0 + 8], B = [MX * 16 + t.tie[0], H * 16 + t.tie[1], z0 + t.tie[2]], N = 30, sag = 7;
  const Pt = (u) => [A[0] + (B[0] - A[0]) * u, A[1] + (B[1] - A[1]) * u - sag * 4 * u * (1 - u), A[2]];
  const dark = [], glow = [];
  for (let k = 0; k < N; k++) {
    const p0 = Pt(k / N), p1 = Pt((k + 1) / N), c = Pt((k + 0.5) / N), len = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]), ang = Math.atan2(p1[1] - p0[1], p1[0] - p0[0]) * 180 / Math.PI;
    const w = k < 2 || k >= N - 2;
    dark.push(box(`span_${k}`, [c[0] - len / 2 - 0.05, c[1] - 0.4, A[2] - 0.6], [c[0] + len / 2 + 0.05, c[1] + 0.4, A[2] + 0.6], { all: w ? 'span_side_worn' : 'span_side', up: w ? 'span_top_worn' : 'span_top' }, { rot: ['z', ang, c], uv: { up: [0, 0, 16, 3] } }));
    glow.push(box(`span_g${k}`, [c[0] - len / 2 - 0.05, c[1] + 0.42, A[2] - 0.15], [c[0] + len / 2 + 0.05, c[1] + 0.42, A[2] + 0.15], { up: 'ring_lit' }, { ...GLOW, rot: ['z', ang, c] }));
  }
  outliner.push(group('tray_span', A, [0, 0, 0], [...put(dark, [0, 0, 0]), group('lit_span', A, [0, 0, 0], put(glow, [0, 0, 0]))]));
  LIT.push('lit_span');
}

// ---------- clips ----------
const all = (v) => Object.fromEntries(LIT.map((g) => [g, { scale: [[0, v]] }]));
clip('dormant', 2, 'loop', { lever: { rotation: () => [-130, 0, 0] } }, all([0, 0, 0]));
clip('awake', 2, 'loop', { lever: { rotation: () => [0, 0, 0] } }, all([1, 1, 1]));
write(OUT, outliner);
{
  const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
}
