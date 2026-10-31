// D76: the small parts' void-alloy texture 'va', repainted with the house grain, before and after. Preview only: the mod's
// textures/block/wastes/va.png is unchanged until the user approves; the new one is va.preview.png here.
// The old 'va' kept a diagonal three-band sheen (lighter top-left, darker bottom-right) inside its facet frame; D75 took the
// sheen off every other void-alloy surface. The new field is the base surface's grain (three close shades, faint violet,
// mid-grey at most), its own seed; the frame is kept exactly (1 px #34304a lit edge top and left, #16151c bottom and right).
// Two textures are painted on 'va' and change with it: the Substation's sigil (sub_sigil) and the lift call plate's
// (lift_sigil). Everything else is the mod's own models and textures as they are now (src/main/resources/...), drawn twice:
// before (left of each pair, seen from the front) and after (right).
// node design/texture-reviews/clockwork-wastes-va-parts/va-parts.gen.js
//   -> va.preview.png (the new va), after-textures/ (va, sub_sigil, lift_sigil as they'd ship), before-textures/ (the mod's),
//      va-texture-before-after.png (the three textures, 16x), va-parts.bbmodel (open in Blockbench; clips 'dormant' and
//      'awake'), shots.json (camera marks for shots.js: gameplay distance, eye height, and close-ups)
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const MODELS = path.resolve(__dirname, '../../models');
const { preview, textureSet } = require(path.join(MODELS, 'process-box.lib.js'));
const CP = require(path.join(MODELS, 'clockwork-wastes-city-props.gen.js'));
const CITY = require(path.join(MODELS, 'clockwork-wastes-city.gen.js'));
const { decode, encode } = require('../clockwork-wastes-icons/pngio.js');

const ASSET = path.resolve(__dirname, '../../../src/main/resources/assets/comedymonkeysaddons');
const TEX = path.join(ASSET, 'textures/block/wastes');
const OUT = path.join(__dirname, 'va-parts.bbmodel');
const CHANGED = ['va', 'sub_sigil', 'lift_sigil'];

// ---------- the textures: the generators' own painters, once as the mod has them and once with the grain ----------
function paint(grain) {
  const code = `const M = require(${JSON.stringify(path.join(MODELS, 'clockwork-wastes-materials.gen.js'))});
require(${JSON.stringify(path.join(MODELS, 'clockwork-wastes-city.gen.js'))}); require(${JSON.stringify(path.join(MODELS, 'clockwork-wastes-lift.gen.js'))});
process.stdout.write(JSON.stringify(Object.fromEntries(${JSON.stringify(CHANGED)}.map((k) => [k, M.T.OWN[k].toString('base64')]))));`;
  const r = spawnSync(process.execPath, ['-e', code], { env: { ...process.env, WASTES_VA_GRAIN: grain ? '1' : '' }, maxBuffer: 1 << 24 });
  if (r.status !== 0) throw new Error(String(r.stderr));
  return Object.fromEntries(Object.entries(JSON.parse(r.stdout)).map(([k, v]) => [k, Buffer.from(v, 'base64')]));
}
const sheen = paint(false), grain = paint(true);
const same = (a, b) => { const p = decode(a), q = decode(b); return p.w === q.w && p.h === q.h && p.data.every((v, i) => v === q.data[i]); };
// the painter with the flag off is exactly what the mod ships (so 'before' is the mod and only the field changes)
for (const k of CHANGED) if (!same(sheen[k], fs.readFileSync(path.join(TEX, k + '.png')))) throw new Error(k + ': the sheen painter differs from the mod');
fs.mkdirSync(path.join(__dirname, 'after-textures'), { recursive: true });
fs.mkdirSync(path.join(__dirname, 'before-textures'), { recursive: true });
for (const k of CHANGED) {
  fs.writeFileSync(path.join(__dirname, 'after-textures', k + '.png'), grain[k]);
  fs.copyFileSync(path.join(TEX, k + '.png'), path.join(__dirname, 'before-textures', k + '.png'));
}
fs.writeFileSync(path.join(__dirname, 'va.preview.png'), grain.va);
// the rules: the frame unchanged pixel for pixel, the field only the grain's three shades
{
  const b = decode(sheen.va), a = decode(grain.va), hex = (im, x, y) => '#' + [0, 1, 2].map((c) => im.data[(y * 16 + x) * 4 + c].toString(16).padStart(2, '0')).join('');
  const field = new Set();
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    if (x === 0 || y === 0 || x === 15 || y === 15) { if (hex(a, x, y) !== hex(b, x, y)) throw new Error('the frame changed at ' + x + ',' + y); }
    else field.add(hex(a, x, y));
  }
  console.log('va frame kept; field shades', [...field].sort().join(' '));
}

// the three textures at 16x, before over after, on mid grey (the old icon review's ground)
{
  const S = 16, T = 16 * S, GAP = 16, items = CHANGED.map((k) => [decode(sheen[k]), decode(grain[k])]);
  const W = items.length * (T + GAP) + GAP, H = 2 * (T + GAP) + GAP, o = new Uint8Array(W * H * 4);
  for (let i = 0; i < W * H; i++) { o[i * 4] = 139; o[i * 4 + 1] = 139; o[i * 4 + 2] = 139; o[i * 4 + 3] = 255; }
  items.forEach((pair, col) => pair.forEach((im, row) => {
    const ox = GAP + col * (T + GAP), oy = GAP + row * (T + GAP);
    for (let y = 0; y < T; y++) for (let x = 0; x < T; x++) {
      const s = (Math.floor(y / S) * im.w + Math.floor(x / S)) * 4, d = ((oy + y) * W + ox + x) * 4, al = im.data[s + 3] / 255;
      for (let c = 0; c < 3; c++) o[d + c] = Math.round(im.data[s + c] * al + 139 * (1 - al));
    }
  }));
  fs.writeFileSync(path.join(__dirname, 'va-texture-before-after.png'), encode(W, H, o));
}

// ---------- the scene ----------
const before = textureSet(), after = textureSet();
before.file = 'before_'; after.file = 'after_';
for (const f of fs.readdirSync(TEX)) if (f.endsWith('.png')) { const k = f.slice(0, -4), buf = fs.readFileSync(path.join(TEX, f)); before.OWN[k] = buf; after.OWN[k] = buf; }
Object.assign(after.OWN, grain);
const { cube, group, clip, write } = preview('clockwork_wastes_va_parts', { 'a:': after, 'b:': before });

// a mod block model's elements as preview cubes (texture keys prefixed by the set)
const model = (name) => JSON.parse(fs.readFileSync(path.join(ASSET, 'models/block/wastes', name + '.json'), 'utf8'));
function cubesOf(name, set) {
  const m = model(name);
  const tex = (v) => { const r = m.textures[v.slice(1)] || ''; return r.startsWith('comedymonkeysaddons:') ? set + r.replace(/^.*\//, '') : r; };
  return (m.elements || []).map((e, i) => {
    const faces = {}, uv = {}, frot = {};
    for (const [d, f] of Object.entries(e.faces)) { faces[d] = tex(f.texture); uv[d] = f.uv; if (f.rotation) frot[d] = f.rotation; }
    return { name: `${name}_${i}`, from: e.from, to: e.to, faces, uv, frot,
      ...(e.rotation ? { rot: [e.rotation.axis, e.rotation.angle, e.rotation.origin] } : {}), ...(e.shade === false ? { shade: false } : {}) };
  });
}
const B = 16, at = (x, y, z) => [x * B, y * B, z * B];
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]),
  ...(c.rot ? { rot: [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] } : {}) }));
const prefix = (cubes, set) => cubes.map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([d, t]) => [d, t && !t.includes(':') ? set + t : t])) }));
const plate = (set, x, y, z) => shift([{ name: `plate_${x}_${y}_${z}`, from: [0, 0, 0], to: [16, 16, 16], faces: { all: set + 'va_bond_s' } }], at(x, y, z));
const girder = (y) => shift([{ name: 'girder_' + y, from: [4, 0, 4], to: [12, 16, 12], faces: { all: 'create:block/girder_pole_side', up: null, down: null },
  uv: { north: [4, 0, 12, 16], south: [4, 0, 12, 16], east: [4, 0, 12, 16], west: [4, 0, 12, 16] } }], at(0, y, 1));
// turned sub-groups (block-state turns, the vault's blades): [name, pivot (block px), rotation (Blockbench degrees), cubes, glow]
const ROT = {};

// each exhibit: parts(set) -> { solid, glow, turned: [[name, pivot, rot, cubes]] }; w, h in blocks; close: close-up marks
// [label, target (block px in the exhibit), distance (px), height offset]
const EXHIBITS = [
  { id: 'assembly_clamp_5', w: 1, h: 2, close: [['jaws', [8, 22, 8], 20, 2]], parts: (set) => ({ solid: prefix(CP.assemblyStage(5), set), glow: [] }) },
  { id: 'assembly_clamp_2', w: 1, h: 2, close: [['cage', [8, 22, 8], 20, 2]], parts: (set) => ({ solid: prefix(CP.assemblyStage(2), set), glow: [] }) },
  { id: 'city_vault', w: 1, h: 1, close: [['bezel_blades', [8, 8, 2], 18, 1]], parts: (set) => ({
    solid: cubesOf('city_vault', set),
    glow: cubesOf('city_vault_pip', set),
    // the closed iris: the six blades at 60k degrees about the centre, as its renderer draws them
    turned: [0, 1, 2, 3, 4, 5].map((k) => ['blade_' + k, [8, 8, CITY.BLADE_Z(k)], [0, 0, 60 * k], prefix(CITY.blade(k), set)]) }) },
  { id: 'sliding_door', w: 2, h: 3, close: [['panels', [8, 20, 4], 24, 0]], parts(set) {
    const solid = [];
    for (let c = 0; c < 2; c++) for (let r = 0; r < 3; r++) solid.push(...shift(cubesOf(`sliding_door_${c}_${r}`, set), at(c, r, 0)));
    for (const n of ['sliding_door_leaf_l', 'sliding_door_leaf_r', 'sliding_door_chain']) solid.push(...shift(cubesOf(n, set), at(0, 1, 0)));
    return { solid, glow: [...shift(cubesOf('sliding_door_seam', set), at(0, 1, 0)), ...shift(cubesOf('sliding_door_jamb_glow', set), at(0, 1, 0))] };
  } },
  { id: 'clamp_bracket', w: 2, h: 2, close: [['straps', [16, 12, 10], 24, 4]], parts: (set) => ({
    // a wall strap on a plate wall (the wall south of it) and a saddle over the plate beside it
    solid: [...cubesOf('clamp_bracket_wall', set), ...plate(set, 0, 0, 1), ...plate(set, 0, 1, 1), ...plate(set, 1, 0, 1), ...plate(set, 1, 0, 0),
      ...shift(cubesOf('clamp_bracket_saddle', set), at(1, 1, 0))], glow: [] }) },
  { id: 'derelict_cable', w: 2, h: 3, close: [['knot_and_clamp', [16, 6, 10], 22, 6]], parts(set) {
    // a floor corner (the knot) and the riser foot with a run up its girder
    // (the south arm: the floor's arm turned as its blockstate turns it, y 90)
    const turned = [['arm_s', [8, 8, 8], [0, -90, 0], cubesOf('derelict_cable_arm', set), cubesOf('derelict_cable_arm_glow', set)]];
    return { solid: [...cubesOf('derelict_cable_arm', set), ...cubesOf('derelict_cable_knot', set), ...shift(cubesOf('derelict_cable_riser_foot', set), at(1, 0, 0)),
      ...shift(cubesOf('derelict_cable_riser_run', set), at(1, 1, 0)), ...shift([...girder(0), ...girder(1), ...girder(2)], at(1, 0, 0))],
      glow: [...cubesOf('derelict_cable_arm_glow', set), ...shift(cubesOf('derelict_cable_riser_foot_glow', set), at(1, 0, 0)),
        ...shift(cubesOf('derelict_cable_riser_run_glow', set), at(1, 1, 0))], turned };
  } },
  { id: 'charging_alcove', w: 1, h: 3, close: [['shelf', [10, 18, 8], 20, 0]], parts(set) {
    const p = ['bottom', 'middle', 'top'];
    return { solid: p.flatMap((n, y) => shift(cubesOf('charging_alcove_' + n, set), at(0, y, 0))), glow: p.flatMap((n, y) => shift(cubesOf('charging_alcove_contact_' + n, set), at(0, y, 0))) };
  } },
  { id: 'lift', w: 3, h: 2, close: [['post_and_clamps', [24, 8, 4], 26, 5]], parts: (set) => ({
    solid: [...cubesOf('lift_deck_w', set), ...cubesOf('lift_deck_e', set), ...cubesOf('lift_guides_w', set), ...cubesOf('lift_guides_e', set),
      ...shift(cubesOf('lift_call_plate', set), at(2, 0, -1)), ...plate(set, 2, 0, 0), ...plate(set, 2, 1, 0)],
    glow: [...cubesOf('lift_guide_seams_w', set), ...cubesOf('lift_guide_seams_e', set), ...cubesOf('lift_deck_glow_w', set), ...cubesOf('lift_deck_glow_e', set),
      ...shift(cubesOf('lift_call_plate_lens', set), at(2, 0, -1))] }) },
  { id: 'substation', w: 2, h: 2, close: [['sigil', [8, 27, 1], 18, 0]], parts: (set) => ({
    solid: [...cubesOf('substation_lower', set), ...shift(cubesOf('substation_upper', set), at(0, 1, 0))],
    glow: [...cubesOf('substation_seams', set), ...shift(cubesOf('substation_sigil', set), at(0, 1, 0))] }) },
  { id: 'conduit_pylon', w: 1, h: 3, close: [['crown', [8, 40, 8], 22, 1]], parts(set) {
    const p = ['bottom', 'middle', 'top'];
    const solid = p.flatMap((n, y) => shift(cubesOf('conduit_pylon_' + n, set), at(0, y, 0)));
    solid.push(...shift([...cubesOf('conduit_pylon_crown', set), ...cubesOf('conduit_pylon_fin', set)], at(0, 2, 0)));
    return { solid, glow: shift([...cubesOf('conduit_pylon_core', set), ...cubesOf('conduit_pylon_seams', set)], at(0, 2, 0)) };
  } },
  { id: 'derelict_star_reactor', w: 3, h: 2, close: [], parts: (set) => ({
    solid: shift([...cubesOf('derelict_star_reactor', set), ...cubesOf('derelict_star_reactor_arm', set)], at(1, 0, 1)),
    glow: shift(cubesOf('derelict_star_reactor_grooves_glow', set), at(1, 0, 1)) }) },
  { id: 'wall_vent', w: 1, h: 1, close: [['lip', [8, 8, 0], 18, 1]], parts: (set) => ({
    solid: [...cubesOf('wall_vent', set), ...cubesOf('wall_vent_hatch', set), ...plate(set, 0, 1, 0)],
    glow: cubesOf('wall_vent_seam', set) }) },
  { id: 'insulator_and_lamp', w: 2, h: 3, close: [], parts: (set) => ({
    solid: [...cubesOf('derelict_insulator_top', set), ...shift([...cubesOf('derelict_street_lamp_foot', set)], at(1, 0, 0)),
      ...shift(cubesOf('derelict_street_lamp_pole', set), at(1, 1, 0)), ...shift(cubesOf('derelict_street_lamp_head', set), at(1, 2, 0))],
    glow: [...cubesOf('derelict_insulator_top_ring', set), ...shift(cubesOf('derelict_street_lamp_glow', set), at(1, 2, 0))] }) },
];

const outliner = [], LIT = [], SHOTS = [], CLOSE = [], floor = [];
let x0 = 0;
for (const ex of EXHIBITS) {
  const looks = [['before', 'b:'], ['after', 'a:']], pair = [];
  for (const [i, [k, set]] of looks.entries()) {
    // seen from the north, x runs right to left: before at the far end, so it reads before, after from the left
    const ox = x0 + (looks.length - 1 - i) * (ex.w + 1);
    const { solid, glow, turned = [] } = ex.parts(set);
    const kids = solid.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0)));
    for (const [n, pivot, rot, cubes, tglow = []] of turned) {
      const gname = `${ex.id}_${k}_${n}`;
      ROT[gname] = rot;
      const tk = cubes.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0)));
      if (tglow.length) { LIT.push('lit_' + gname); tk.push(group('lit_' + gname, pivot, at(ox, 0, 0), tglow.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0))))); }
      kids.push(group(gname, pivot, at(ox, 0, 0), tk));
    }
    if (glow.length) { LIT.push(`lit_${ex.id}_${k}`); kids.push(group(`lit_${ex.id}_${k}`, [8, 0, 8], at(ox, 0, 0), glow.map((c) => cube({ ...c, name: `${k}_${c.name}` }, at(ox, 0, 0))))); }
    pair.push(group(`${ex.id}_${k}`, [8, 0, 8], at(ox, 0, 0), kids));
    for (const [label, t, dist, dy] of ex.close) {
      // close up: a block or two away, a touch above, from the front (north) and a little to the side
      const tx = ox * B + t[0] - 8, ty = t[1], tz = t[2] - 8;
      CLOSE.push({ id: `${ex.id}-${label}-${k}`, position: [tx + dist * 0.35, ty + dy, tz - dist], target: [tx, ty, tz] });
    }
  }
  outliner.push(group(ex.id, [8, 0, 8], at(x0, 0, 0), pair));
  const span = ex.w * 2 + 1;
  for (let x = x0 - 1; x < x0 + span + 1; x++) for (let z = -6; z < 4; z++)
    floor.push(cube({ name: `floor_${x}_${z}`, from: [0, -1, 0], to: [16, 0, 16], faces: { up: 'a:scorched' } }, at(x, 0, z)));
  // gameplay distance: eye height (1.6 blocks), far enough out that the pair and its height fit, at its middle
  const cx = (x0 + span / 2) * B - 8, dist = Math.max(5, span * 0.95 + 1, ex.h * 1.9 + 1);
  SHOTS.push({ id: ex.id, position: [cx, 26, -dist * B], target: [cx, ex.h * 7, 0] });
  x0 += span + 2;
}
outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
const all = (v) => Object.fromEntries(LIT.map((g) => [g, { scale: [[0, v]] }]));
clip('dormant', 2, 'loop', {}, all([0, 0, 0]));
clip('awake', 2, 'loop', {}, all([1, 1, 1]));
write(OUT, outliner);
// the turned groups' rest rotations (the preview library doesn't set them), in the outliner and the group list
{
  const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (ROT[n.name]) n.rotation = ROT[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (ROT[g.name]) g.rotation = ROT[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
fs.writeFileSync(path.join(__dirname, 'shots.json'), JSON.stringify({ gameplay: SHOTS, close: CLOSE }, null, 1));
console.log(`wrote ${OUT}: ${EXHIBITS.length} pairs; ${SHOTS.length} gameplay and ${CLOSE.length} close-up camera marks in shots.json`);
