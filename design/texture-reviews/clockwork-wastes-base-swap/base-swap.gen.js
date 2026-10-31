// D75's base swap, before and after: every existing void-alloy block whose surface changed, the old striped sheen
// ('va_tile' and the textures painted on it) on the left of each pair, the new surface on the right. Only the surface
// changed: the models are the mod's own (src/main/resources/.../models/block/wastes), drawn twice, the old textures kept
// in before-textures/ (the exporter's output before the swap); the shapes were checked identical, texture names aside.
// Each pair: before (left), after (middle), and for the shaped and complex models a third, the frameless plain field (right).
// node design/texture-reviews/clockwork-wastes-base-swap/base-swap.gen.js  -> base-swap.bbmodel (open in Blockbench;
// clips 'dormant' and 'awake' show the glows off and on; shots.js takes the screenshots)
const fs = require('fs');
const path = require('path');
const MODELS = path.resolve(__dirname, '../../models');
const { preview, textureSet } = require(path.join(MODELS, 'process-box.lib.js'));
const BB = require(path.join(MODELS, 'clockwork-wastes-building-blocks.gen.js'));
const CP = require(path.join(MODELS, 'clockwork-wastes-city-props.gen.js'));

const ASSET = path.resolve(__dirname, '../../../src/main/resources/assets/comedymonkeysaddons');
const NEW_TEX = path.join(ASSET, 'textures/block/wastes'), OLD_TEX = path.join(__dirname, 'before-textures');
const OUT = path.join(__dirname, 'base-swap.bbmodel');

// the two texture sets: after = the mod's textures now; before = the same, but the swapped ones as they were
const after = textureSet(), before = textureSet();
after.file = 'after_'; before.file = 'before_';
for (const f of fs.readdirSync(NEW_TEX)) if (f.endsWith('.png')) {
  const k = f.slice(0, -4), buf = fs.readFileSync(path.join(NEW_TEX, f));
  after.OWN[k] = buf; before.OWN[k] = buf;
}
// the old striped sheen under its new name, and the textures painted on it
const RENAMED = { va_plain: 'va_tile' };
for (const f of fs.readdirSync(OLD_TEX)) if (f.endsWith('.png')) {
  const k = f.slice(0, -4), buf = fs.readFileSync(path.join(OLD_TEX, f));
  const key = Object.keys(RENAMED).find((n) => RENAMED[n] === k) || k;
  before.OWN[key] = buf;
}
// (the Pillar's chamfered faces were va_tile, now va_field; its seam strips unchanged)
before.OWN.va_field = before.OWN.va_plain;
// the plate's bond textures stand in for the old plate's va_tile in 'before' (the plate is drawn from its old model there)
// a third look for the shaped and complex models: the frameless plain field (va_field, as the preview drew the Charging
// Alcove) in place of the framed plain plate the mod now uses (choice for the user)
const field = textureSet();
field.file = 'field_';
Object.assign(field.OWN, after.OWN, { va_plain: after.OWN.va_field });
const { cube, group, clip, write } = preview('clockwork_wastes_base_swap', { 'a:': after, 'b:': before, 'f:': field });

// a mod block model's elements as preview cubes (texture keys prefixed by the set)
const model = (name) => JSON.parse(fs.readFileSync(path.join(ASSET, 'models/block/wastes', name + '.json'), 'utf8'));
function cubesOf(name, set, o = {}) {
  const m = model(name);
  const tex = (v) => (m.textures[v.slice(1)] || '').replace(/^.*\//, '');
  return (m.elements || []).map((e, i) => {
    const faces = {}, uv = {}, frot = {};
    for (const [d, f] of Object.entries(e.faces)) { faces[d] = set + tex(f.texture); uv[d] = f.uv; if (f.rotation) frot[d] = f.rotation; }
    return { name: `${name}_${i}`, from: e.from, to: e.to, faces, uv, frot,
      ...(e.rotation ? { rot: [e.rotation.axis, e.rotation.angle, e.rotation.origin] } : {}), ...(e.shade === false ? { shade: false } : {}), ...o };
  });
}
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]),
  ...(c.rot ? { rot: [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] } : {}) }));
const B = 16, at = (x, y, z) => [x * B, y * B, z * B];
const prefix = (cubes, set, map = {}) => cubes.map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([d, t]) => [d, t ? set + (map[t] || t) : t])) }));

// each exhibit: parts(set) -> { solid: cubes, glow: cubes } in block px, its width in blocks
const EXHIBITS = [
  { id: 'void_alloy_plate', h: 3, w: 4, parts(set) {
    const solid = [];
    for (let x = 0; x < 4; x++) for (let y = 0; y < 3; y++) {
      // before: the old plate (cube_all va_tile); after: the bond, by position, as the blockstate draws it
      const faces = {};
      for (const d of ['north', 'east', 'south', 'west', 'up', 'down']) faces[d] = set === 'b:' ? 'b:va_plain' : 'a:' + BB.OPTS.C(x, y, 0, d);
      solid.push(...shift([{ name: `plate_${x}_${y}`, from: [0, 0, 0], to: [16, 16, 16], faces }], at(x, y, 0)));
    }
    return { solid, glow: [] };
  } },
  { id: 'seamed_void_alloy_plate', h: 2, w: 2, parts(set) {
    const solid = [], glow = [];
    for (let x = 0; x < 2; x++) for (let y = 0; y < 2; y++) {
      solid.push(...shift([{ name: `seamed_${x}_${y}`, from: [0, 0, 0], to: [16, 16, 16], faces: { all: set + 'va_seamed' } }], at(x, y, 0)));
      glow.push(...shift(cubesOf('seamed_void_alloy_plate_seams', set), at(x, y, 0)));
    }
    return { solid, glow };
  } },
  { id: 'void_alloy_pillar', h: 3, w: 1, parts(set) {
    return { solid: [0, 1, 2].flatMap((y) => shift(cubesOf('void_alloy_pillar', set), at(0, y, 0))), glow: [] };
  } },
  { id: 'charging_alcove', h: 3, w: 1, parts(set) {
    const p = ['bottom', 'middle', 'top'];
    return { solid: p.flatMap((n, y) => shift(cubesOf('charging_alcove_' + n, set), at(0, y, 0))),
      glow: p.flatMap((n, y) => shift(cubesOf('charging_alcove_contact_' + n, set), at(0, y, 0))) };
  } },
  { id: 'assembly_clamp', h: 2, w: 1, parts(set) {
    // its baked OBJ is the generator's stage 5, drawn here from the generator's cubes
    return { solid: prefix(CP.assemblyStage(5), set), glow: [] };
  } },
  { id: 'city_vault', h: 1, w: 1, parts(set) {
    return { solid: [...cubesOf('city_vault', set), ...cubesOf('city_vault_blade', set)], glow: cubesOf('city_vault_pip', set) };
  } },
  { id: 'sliding_door', h: 3, w: 2, parts(set) {
    const solid = [];
    for (let c = 0; c < 2; c++) for (let r = 0; r < 3; r++) solid.push(...shift(cubesOf(`sliding_door_${c}_${r}`, set), at(c, r, 0)));
    // the leaves, seam and chain are parts its renderer draws in the middle row (their models start 16 px down)
    for (const n of ['sliding_door_leaf_l', 'sliding_door_leaf_r', 'sliding_door_chain']) solid.push(...shift(cubesOf(n, set), at(0, 1, 0)));
    return { solid, glow: [...shift(cubesOf('sliding_door_seam', set), at(0, 1, 0)), ...shift(cubesOf('sliding_door_jamb_glow', set), at(0, 1, 0))] };
  } },
  { id: 'conduit_pylon', h: 3, w: 1, parts(set) {
    const p = ['bottom', 'middle', 'top'];
    const solid = p.flatMap((n, y) => shift(cubesOf('conduit_pylon_' + n, set), at(0, y, 0)));
    solid.push(...shift([...cubesOf('conduit_pylon_crown', set), ...cubesOf('conduit_pylon_fin', set)], at(0, 2, 0)));
    return { solid, glow: shift([...cubesOf('conduit_pylon_core', set), ...cubesOf('conduit_pylon_seams', set)], at(0, 2, 0)) };
  } },
  { id: 'derelict_star_reactor', h: 2, w: 3, parts(set) {
    const solid = shift([...cubesOf('derelict_star_reactor', set), ...cubesOf('derelict_star_reactor_arm', set)], at(1, 0, 1));
    return { solid, glow: shift(cubesOf('derelict_star_reactor_grooves_glow', set), at(1, 0, 1)) };
  } },
  { id: 'derelict_cable_riser_foot', h: 1, w: 1, parts(set) {
    return { solid: cubesOf('derelict_cable_riser_foot', set), glow: cubesOf('derelict_cable_riser_foot_glow', set) };
  } },
  { id: 'lift', h: 1, w: 2, parts(set) {
    const solid = [...cubesOf('lift_deck_w', set), ...cubesOf('lift_deck_e', set), ...cubesOf('lift_guides_w', set), ...cubesOf('lift_guides_e', set)];
    return { solid, glow: [...cubesOf('lift_guide_seams_w', set), ...cubesOf('lift_guide_seams_e', set), ...cubesOf('lift_deck_glow_w', set), ...cubesOf('lift_deck_glow_e', set)] };
  } },
  { id: 'wall_vent', h: 1, w: 1, parts(set) {
    return { solid: cubesOf('wall_vent', set), glow: cubesOf('wall_vent_seam', set) };
  } },
];

const outliner = [], LIT = [];
const floor = [];
let x0 = 0;
const SHOTS = [];
for (const ex of EXHIBITS) {
  const pair = [], third = !['void_alloy_plate', 'seamed_void_alloy_plate', 'void_alloy_pillar'].includes(ex.id);
  const looks = [['before', 'b:'], ['after', 'a:'], ...(third ? [['field', 'f:']] : [])];
  for (const [i, [k, set]] of looks.entries()) {
    // seen from the north, x runs right to left: before at the far end, so it reads before, after, field from the left
    const ox = x0 + (looks.length - 1 - i) * (ex.w + 1);
    const { solid, glow } = ex.parts(set);
    const kids = solid.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0)));
    if (glow.length) { LIT.push(`lit_${ex.id}_${k}`); kids.push(group(`lit_${ex.id}_${k}`, [8, 0, 8], at(ox, 0, 0), glow.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0))))); }
    pair.push(group(`${ex.id}_${k}`, [8, 0, 8], at(ox, 0, 0), kids));
  }
  outliner.push(group(ex.id, [8, 0, 8], at(x0, 0, 0), pair));
  const span = ex.w * looks.length + looks.length - 1;
  for (let x = x0 - 1; x < x0 + span + 1; x++) for (let z = -6; z < 4; z++)
    floor.push(cube({ name: `floor_${x}_${z}`, from: [0, -1, 0], to: [16, 0, 16], faces: { up: 'a:scorched' } }, at(x, 0, z)));
  // gameplay distance: eye height (1.6 blocks), far enough out that the pair and its height fit (5 to 10 blocks), at its middle
  const cx = (x0 + span / 2) * B, dist = Math.max(5, span * 0.95 + 1, ex.h * 1.9 + 1);
  // (from the north: the models' fronts face north, as drawn)
  SHOTS.push({ id: ex.id, looks: looks.map((l) => l[0]), position: [cx, 26, -dist * B], target: [cx, ex.h * 7, 8] });
  x0 += span + 2;
}
outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
const all = (v) => Object.fromEntries(LIT.map((g) => [g, { scale: [[0, v]] }]));
clip('dormant', 2, 'loop', {}, all([0, 0, 0]));
clip('awake', 2, 'loop', {}, all([1, 1, 1]));
write(OUT, outliner);
fs.writeFileSync(path.join(__dirname, 'shots.json'), JSON.stringify(SHOTS, null, 1));
console.log(`wrote ${OUT}: ${EXHIBITS.length} pairs; ${SHOTS.length} camera marks in shots.json`);
