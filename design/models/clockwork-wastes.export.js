// Writes the Clockwork Wastes' phase-1 blocks and items into the mod (design/specs/clockwork-wastes.md, phase 1; looks
// approved in clockwork-wastes-materials.bbmodel and clockwork-wastes-salvage.bbmodel):
//   - the decor set (D37): Scorched End Stone and its rusted and littered variants, the Void-Alloy Plate, Pillar and
//     Seamed Plate (with the lit seam overlay for phase 2), the Scrap Heap, the Charging Alcove and Maker Statue (1 x 3,
//     a part model per block like a door) and the Toppled Maker Statue (3 x 1, lying)
//   - the salvage blocks (D26): five broken machines (block models) and the Worker and Resistance Shells in three poses
//     each (OBJ models: their bones are posed at angles a block model can't hold, so the posed cubes are baked to quads)
//   - the items: Clockwork Scrap and the Unknown Device (the generator's icons), flat icons for the tall blocks
//   - blockstates, item models, only the textures the models use (textures/block/wastes/), and the data that goes
//     with them: loot tables (blocks/ = what breaking gives, salvage/ = what the wrench gives), the pickaxe tag and the
//     Clockwork Scrap crushing recipe
// Every block model is checked as the other exporters check them (UVs on the texture, no zero-width UV, bounds
// -16..32, one rotation of 0/±22.5/±45 per element) and every model is z-fight checked (faces of different textures
// closer than 0.2 px).
// Usage: node design/models/clockwork-wastes.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, fitGui, hash } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const S = require('./clockwork-wastes-salvage.gen.js');
const { check } = require('./zfight-check.js');

const T = M.T; // the salvage generator paints into the same set
const NS = 'comedymonkeysaddons';
const r3 = (n) => Math.round(n * 1000) / 1000;
const used = new Set();
const ref = (key) => (key.includes(':') ? key : `${NS}:block/wastes/${key}`);
const faceTex = (c, dir) => (dir in c.faces ? c.faces[dir] : c.faces.all);
const AXIS_I = { x: 0, y: 1, z: 2 };

// ---------- geometry helpers ----------
const move = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]),
  rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const rad = (d) => (d * Math.PI) / 180;
// a point turned by an angle (degrees, right-handed, as Blockbench and block models turn) about an axis through o
function turnPoint(p, axis, deg, o) {
  const a = rad(deg), q = p.map((v, i) => v - o[i]);
  const [i, j] = axis === 'x' ? [1, 2] : axis === 'y' ? [2, 0] : [0, 1];
  const u = q[i] * Math.cos(a) - q[j] * Math.sin(a), w = q[i] * Math.sin(a) + q[j] * Math.cos(a);
  q[i] = u; q[j] = w;
  return q.map((v, k) => v + o[k]);
}
const VEC = { north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0], down: [0, -1, 0], up: [0, 1, 0] };
const dirOf = (v) => Object.keys(VEC).find((d) => VEC[d].every((n, i) => Math.abs(n - v[i]) < 1e-6));
// each face's texture axes on the block (where u and v grow), as process-box.lib's defaultUv lays them
const FRAME = { north: [[-1, 0, 0], [0, -1, 0]], south: [[1, 0, 0], [0, -1, 0]], east: [[0, 0, -1], [0, -1, 0]],
  west: [[0, 0, 1], [0, -1, 0]], up: [[1, 0, 0], [0, 0, 1]], down: [[1, 0, 0], [0, 0, -1]] };
const neg = (v) => v.map((n) => -n || 0);
const same = (a, b) => a.every((n, i) => Math.abs(n - b[i]) < 1e-6);
// where a face rotation (clockwise, as block models turn a face's texture) sends the texture's u and v
const faceTurn = (U, V, deg) => ({ 0: [U, V], 90: [V, neg(U)], 180: [neg(U), neg(V)], 270: [neg(V), U] }[deg]);

// A quarter turn (90, 180 or 270 degrees) of axis-aligned cubes, done exactly by swapping axes: each face keeps the
// texture and UV it had and gets the face rotation that keeps its picture the right way round. (A block model can only
// turn an element by up to 45 degrees; the preview turned some by 90.)
function quarter(cubes, axis, deg, o) {
  return cubes.map((c) => {
    if (c.rot) throw new Error(c.name + ': quarter() turns unrotated cubes only');
    const a = turnPoint(c.from, axis, deg, o), b = turnPoint(c.to, axis, deg, o);
    const from = a.map((v, i) => r3(Math.min(v, b[i]))), to = a.map((v, i) => r3(Math.max(v, b[i])));
    const faces = {}, uv = {}, frot = {};
    for (const d of DIRS) {
      const tex = faceTex(c, d);
      const nd = dirOf(turnPoint(VEC[d], axis, deg, [0, 0, 0]));
      faces[nd] = tex || null;
      if (!tex) continue;
      uv[nd] = (c.uv && c.uv[d]) || fitUv(d, c.from, c.to);
      // the texture's axes on the old face, turned with the cube, must be the new face's axes under its rotation
      const [tu, tv] = faceTurn(...FRAME[d], (c.frot && c.frot[d]) || 0).map((v) => turnPoint(v, axis, deg, [0, 0, 0]));
      const r = [0, 90, 180, 270].find((k) => { const [u, v] = faceTurn(...FRAME[nd], k); return same(u, tu) && same(v, tv); });
      if (r === undefined) throw new Error(c.name + ': no face rotation for ' + nd);
      if (r) frot[nd] = r;
    }
    return { ...c, from, to, faces, uv, frot };
  });
}

// Two rotations about parallel axes (a cube's own inside a turned group) make one: the angles add, about the point the
// cube's own origin is carried to (a turn of 0 is just that move)
function compose(c, axis, deg, o) {
  if (!c.rot) return { ...c, rot: [axis, deg, o] };
  if (c.rot[0] !== axis) throw new Error(c.name + ': rotations on two axes can\'t export');
  const q = turnPoint(c.rot[2], axis, deg, o), d = q.map((v, i) => v - c.rot[2][i]);
  const angle = c.rot[1] + deg, moved = move([{ ...c, rot: null }], d)[0];
  return angle === 0 ? moved : { ...moved, rot: [axis, angle, q] };
}

// A cube list cut into the block cells along an axis (0-16, 16-32, 32-48): each piece moved into its cell, the faces on
// the cuts dropped (they're inside the model). Rotated cubes must sit inside one cell.
const slice = (cubes, axis, lo) => cut(cubes, axis, lo, lo + 16, lo);
// The same for any range lo..hi along the axis (a cell that also takes what overhangs it), moved back by 'by'
function cut(cubes, axis, lo, hi, by) {
  const i = AXIS_I[axis], lowDir = { x: 'west', y: 'down', z: 'north' }[axis], highDir = { x: 'east', y: 'up', z: 'south' }[axis];
  const out = [];
  for (const c of cubes) {
    const a = Math.max(c.from[i], lo), b = Math.min(c.to[i], hi);
    const flat = c.from[i] === c.to[i];
    if (flat ? !(c.from[i] >= lo && c.from[i] < hi) : b <= a) continue;
    if (c.rot && (c.from[i] < lo || c.to[i] > hi)) throw new Error(c.name + ': a rotated cube can\'t be cut');
    const from = [...c.from], to = [...c.to];
    if (!flat) { from[i] = a; to[i] = b; }
    const faces = { ...c.faces };
    if (!flat && a > c.from[i]) faces[lowDir] = null;
    if (!flat && b < c.to[i]) faces[highDir] = null;
    // a hand-set UV (a quarter-turned cube keeps the preview's) is cut in step with the face: the share of it along the
    // texture axis that runs with the cut
    let uv = c.uv;
    if (uv && !flat && (a > c.from[i] || b < c.to[i])) {
      uv = {};
      const len = c.to[i] - c.from[i];
      for (const [dir, r] of Object.entries(c.uv)) {
        const [tu, tv] = faceTurn(...FRAME[dir], (c.frot && c.frot[dir]) || 0);
        const cut = (lo0, hi0, s) => (s > 0 ? [lo0 + (a - c.from[i]) / len * (hi0 - lo0), lo0 + (b - c.from[i]) / len * (hi0 - lo0)]
          : [lo0 + (c.to[i] - b) / len * (hi0 - lo0), lo0 + (c.to[i] - a) / len * (hi0 - lo0)]);
        const nu = tu[i] ? cut(r[0], r[2], tu[i]) : [r[0], r[2]], nv = tv[i] ? cut(r[1], r[3], tv[i]) : [r[1], r[3]];
        uv[dir] = [nu[0], nv[0], nu[1], nv[1]];
      }
    }
    const d = [0, 0, 0]; d[i] = -by;
    out.push(move([{ ...c, from, to, faces, uv }], d)[0]);
  }
  return out;
}

// ---------- block models ----------
// A face lying on the block's edge, pointing out of it, can be culled by a full block beside it
function cullface(c, dir) {
  if (c.rot) return null;
  const [ax, end, val] = { north: [2, 'from', 0], south: [2, 'to', 16], west: [0, 'from', 0], east: [0, 'to', 16], down: [1, 'from', 0], up: [1, 'to', 16] }[dir];
  return c[end][ax] === val ? dir : null;
}
const area = (c, dir) => {
  const s = [0, 1, 2].map((i) => Math.abs(c.to[i] - c.from[i]));
  return dir === 'north' || dir === 'south' ? s[0] * s[1] : dir === 'east' || dir === 'west' ? s[2] * s[1] : s[0] * s[2];
};
function blockModel(cubes, particle, { gui = false, renderType = 'minecraft:cutout', emissive = false, ao = false } = {}) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_');
      while ([...vars.values()].includes(n)) n += '_';
      vars.set(r, n);
    }
    return '#' + vars.get(r);
  };
  const elements = [];
  for (const c of cubes) {
    const faces = {};
    for (const dir of DIRS) {
      const t = faceTex(c, dir);
      if (!t || area(c, dir) < 1e-6) continue;
      if (!t.includes(':')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      if (Math.abs(raw[0] - raw[2]) < 1e-6 || Math.abs(raw[1] - raw[3]) < 1e-6) throw new Error(`${c.name} ${dir}: zero-width UV ${raw}`);
      faces[dir] = { uv: raw.map(r3), texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
      const cf = cullface(c, dir);
      if (cf) faces[dir].cullface = cf;
    }
    if (!Object.keys(faces).length) continue;
    const el = { name: c.name, from: c.from.map(r3), to: c.to.map(r3) };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: c.rot[2].map(r3), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    // a glow the chunk draws (not the renderer): lit full-bright in the block model itself
    if (c.shade === false && emissive) el.neoforge_data = { block_light: 15, sky_light: 15 };
    el.faces = faces;
    for (const p of [...el.from, ...el.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    elements.push(el);
  }
  if (!particle.includes(':')) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  // (ao: the building block set shades like the vanilla blocks it's built beside; the city's machines and props don't)
  const model = { parent: 'block/block', render_type: renderType, ...(ao ? {} : { ambientocclusion: false }), textures, elements };
  if (gui) model.display = { gui: fitGui(elements) };
  return model;
}
function writeJson(rel, obj) {
  const file = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
}
const zfights = [];
function zcheck(name, parts) {
  for (const p of check(parts)) zfights.push(`${name}: ${p}`);
}
// only fights between different textures show (the same texture meeting itself draws the same pixels)
function zcheckModel(name, cubes) {
  const byTex = {};
  for (const c of cubes) for (const d of DIRS) {
    const t = faceTex(c, d);
    if (t && area(c, d) > 1e-6) (byTex[t] = byTex[t] || []).push({ ...c, faces: { [d]: t } });
  }
  // parts named t0, t1... (texture keys can hold a colon); a fight counts only between two of them
  const keys = Object.keys(byTex), parts = Object.fromEntries(keys.map((k, i) => ['t' + i, byTex[k]]));
  for (const p of check(parts)) {
    const m = p.match(/^t(\d+):(\S+) (\w+) vs t(\d+):(.*)$/);
    if (m && m[1] !== m[4]) zfights.push(`${name}: ${keys[m[1]]}:${m[2]} ${m[3]} vs ${keys[m[4]]}:${m[5]}`);
  }
}

const model = (id) => `${NS}:block/wastes/${id}`;
const HORIZONTAL = { north: 0, east: 90, south: 180, west: 270 };
const yrot = (deg) => (deg ? { y: deg } : {});

// ---------- the decor set: whole blocks ----------
function cubeAll(id, tex) {
  used.add(tex);
  writeJson(`models/block/wastes/${id}.json`, { parent: 'minecraft:block/cube_all', textures: { all: ref(tex) } });
}
function cubeTop(id, side, top) {
  used.add(side); used.add(top);
  writeJson(`models/block/wastes/${id}.json`, { parent: 'minecraft:block/cube_bottom_top', textures: { top: ref(top), side: ref(side), bottom: ref(side) } });
}
const FULL = {
  scorched_end_stone: () => cubeAll('scorched_end_stone', 'scorched'),
  rusted_scorched_end_stone: () => cubeTop('rusted_scorched_end_stone', 'scorched', 'scorched_rust'),
  littered_scorched_end_stone: () => cubeTop('littered_scorched_end_stone', 'scorched', 'scorched_litter'),
  seamed_void_alloy_plate: () => cubeAll('seamed_void_alloy_plate', 'va_seamed'),
};
// the ground's top textures lie any way round, so a field of them doesn't repeat
const SPUN = new Set(['scorched_end_stone', 'rusted_scorched_end_stone', 'littered_scorched_end_stone']);
for (const [id, make] of Object.entries(FULL)) {
  make();
  const m = model(id);
  writeJson(`blockstates/${id}.json`, { variants: { '': SPUN.has(id) ? [0, 90, 180, 270].map((y) => ({ model: m, ...yrot(y) })) : { model: m } } });
  writeJson(`models/item/${id}.json`, { parent: m });
}
// the Seamed Plate's lit seams: an emissive overlay in the block model while its district is fed (lit=true), as the
// Derelict Cable's line is (no renderer)
{
  const seams = [M.overlay('seam_overlay', 'seam_lit', true)];
  zcheckModel('seamed_void_alloy_plate', [M.box('plate', [0, 0, 0], [16, 16, 16], 'va_seamed'), ...seams]);
  writeJson('models/block/wastes/seamed_void_alloy_plate_seams.json', blockModel(seams, 'va_seamed', { emissive: true }));
  writeJson('blockstates/seamed_void_alloy_plate.json', { multipart: [{ apply: { model: model('seamed_void_alloy_plate') } },
    { when: { lit: 'true' }, apply: { model: model('seamed_void_alloy_plate_seams') } }] });
}

// the Void-Alloy Pillar: chamfered, its seam strips dark (they don't light in v1), placed along an axis like a log
{
  const cubes = M.pillar('pillar', 0, false);
  zcheckModel('void_alloy_pillar', cubes);
  writeJson('models/block/wastes/void_alloy_pillar.json', blockModel(cubes, 'va_field'));
  const m = model('void_alloy_pillar');
  writeJson('blockstates/void_alloy_pillar.json', { variants: { 'axis=y': { model: m }, 'axis=z': { model: m, x: 90 }, 'axis=x': { model: m, x: 90, y: 90 } } });
  writeJson('models/item/void_alloy_pillar.json', { parent: m });
}

// the Scrap Heap: half a block, turned any way round
{
  const cubes = S.scrapHeap();
  zcheckModel('scrap_heap', cubes);
  writeJson('models/block/wastes/scrap_heap.json', blockModel(cubes, 'scrap_mix'));
  const m = model('scrap_heap');
  writeJson('blockstates/scrap_heap.json', { variants: { '': [0, 90, 180, 270].map((y) => ({ model: m, ...yrot(y) })) } });
  writeJson('models/item/scrap_heap.json', { parent: m });
}

// ---------- the decor set: blocks three long, a part model per block ----------
// parts: bottom, middle, top (standing: up the column; lying: from the foot along the facing's clockwise side, east for
// a north-facing one)
const PARTS = ['bottom', 'middle', 'top'];
// lit: a model name prefix whose part models (<lit>_<part>) are drawn over each block while it's lit (a multipart)
function threePart(id, cubes, axis, particle, item, lit) {
  zcheckModel(id, cubes);
  const variants = {}, multipart = [];
  PARTS.forEach((part, k) => {
    writeJson(`models/block/wastes/${id}_${part}.json`, blockModel(slice(cubes, axis, k * 16), particle));
    for (const [facing, y] of Object.entries(HORIZONTAL)) {
      variants[`facing=${facing},part=${part}`] = { model: model(`${id}_${part}`), ...yrot(y) };
      multipart.push({ when: { facing, part }, apply: { model: model(`${id}_${part}`), ...yrot(y) } });
      if (lit) multipart.push({ when: { facing, part, lit: 'true' }, apply: { model: model(`${lit}_${part}`), ...yrot(y) } });
    }
  });
  writeJson(`blockstates/${id}.json`, lit ? { multipart } : { variants });
  if (item) writeJson(`models/item/${id}.json`, item);
}
// the Charging Alcove (D72: its corner shelf, trinkets and dead vines) is written in "the city props" section at the end,
// after the generators whose textures the props share are loaded
threePart('maker_statue', S.statue(), 'y', 'carved', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/maker_statue` } });

// the Toppled Maker Statue: the standing one turned onto its side (a quarter turn, done exactly), resting on the ground
// (the preview's floated 1.5 px), its head broken off and lying beyond the shoulders, rolled a little (the preview's 25
// degrees as a block model's 22.5)
const HEAD = ['hood', 'hood_peak', 'face_cut', 'face_plate', 'clamp_t', 'clamp_b'];
function toppled() {
  const all = S.statue();
  const body = move(quarter(all.filter((c) => !HEAD.includes(c.name)), 'z', -90, [8, 8, 8]), [0, -1.5, 0]);
  // the head about its own middle (8, 39.75, 8): a quarter turn the other way, then rolled about the statue's length;
  // set down past the shoulders with its pivot at (41, 3, 8)
  const pivot = [41, 3, 8];
  const head = quarter(move(all.filter((c) => HEAD.includes(c.name)), [pivot[0] - 8, pivot[1] - 39.75, 0]), 'z', 90, pivot)
    .map((c) => ({ ...c, rot: ['x', -22.5, pivot] }));
  return [...body, ...head];
}
{
  const cubes = toppled();
  // the item: the whole statue, its middle in the block
  threePart('toppled_maker_statue', cubes, 'x', 'carved', blockModel(move(cubes, [-16, 0, 0]), 'carved', { gui: true }));
}

// ---------- the broken machines: one block model each, any of four facings ----------
function brokenArm() {
  const a = S.brokenArm();
  // the arm leans 22.5 degrees from its base; its claws' own tilt and the lean add up to one turn each
  return [...a.base, ...a.arm.map((c) => compose(c, 'z', 22.5, [8, 4, 8]))];
}
function brokenWheel() {
  const up = S.shift(S.halfWheel('half_up', 7, 5), [0, 0, -3]);
  // the fallen half lies on its side: the preview's 90-degree turn, done by swapping axes
  const down = move(quarter(S.halfWheel('half_down', 7, 5), 'x', 90, [8, 0, 8]), [0, 2.5, 4]);
  return [...up, ...down];
}
const MACHINES = {
  broken_press: S.brokenPress,
  broken_mixer: S.brokenMixer,
  broken_deployer: S.brokenDeployer,
  broken_mechanical_arm: brokenArm,
  broken_crushing_wheel: brokenWheel,
};
for (const [id, make] of Object.entries(MACHINES)) {
  const cubes = make();
  zcheckModel(id, cubes);
  writeJson(`models/block/wastes/${id}.json`, blockModel(cubes, 'casing_aged'));
  writeJson(`blockstates/${id}.json`, { variants: Object.fromEntries(Object.entries(HORIZONTAL).map(([f, y]) => [`facing=${f}`, { model: model(id), ...yrot(y) }])) });
  writeJson(`models/item/${id}.json`, blockModel(cubes, 'casing_aged', { gui: true }));
}

// ---------- the shells: posed rigs baked to OBJ quads ----------
// Blockbench turns a bone about its pivot by z, then y, then x of its rotation applied to the point in x, y, z order
// (Three.js 'ZYX': x first), each right-handed; parents after children.
function posePoint(p, chain) {
  let q = p;
  for (const { pivot, r } of chain) {
    q = turnPoint(q, 'x', r[0], pivot);
    q = turnPoint(q, 'y', r[1], pivot);
    q = turnPoint(q, 'z', r[2], pivot);
  }
  return q;
}
// a face's corners in the order block models give them, and which UV corner each takes (FaceInfo, BlockFaceUV)
const CORNERS = {
  down: [[0, 0, 1], [0, 0, 0], [1, 0, 0], [1, 0, 1]], up: [[0, 1, 0], [0, 1, 1], [1, 1, 1], [1, 1, 0]],
  north: [[1, 1, 0], [1, 0, 0], [0, 0, 0], [0, 1, 0]], south: [[0, 1, 1], [0, 0, 1], [1, 0, 1], [1, 1, 1]],
  west: [[0, 1, 0], [0, 0, 0], [0, 0, 1], [0, 1, 1]], east: [[1, 1, 1], [1, 0, 1], [1, 0, 0], [1, 1, 0]],
};
function posedQuads(rig, pose) {
  const quads = [];
  const walk = (node, chain) => {
    const here = pose.r[node.n] ? [{ pivot: node.p, r: pose.r[node.n] }, ...chain] : chain;
    for (const c of node.c || []) {
      if (c.rot) throw new Error(c.name + ': a shell cube can\'t have its own rotation');
      for (const dir of DIRS) {
        const t = faceTex(c, dir);
        if (!t || area(c, dir) < 1e-6) continue;
        const uv = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
        const shiftBy = ((c.frot && c.frot[dir]) || 0) / 90;
        const corners = CORNERS[dir].map((k) => k.map((s, i) => (s ? c.to[i] : c.from[i])));
        const pts = corners.map((p) => posePoint(p, here).map((v, i) => v + pose.lift[i]));
        const uvs = [0, 1, 2, 3].map((n) => { const i = (n + shiftBy) % 4; return [i === 0 || i === 1 ? uv[0] : uv[2], i === 0 || i === 3 ? uv[1] : uv[3]]; });
        const normal = posePoint(VEC[dir], here.map((h) => ({ ...h, pivot: [0, 0, 0] })));
        quads.push({ tex: t, pts, uvs, normal, name: c.name });
      }
    }
    for (const k of node.k || []) walk(k, here);
  };
  walk(rig, []);
  return quads;
}
const fmt = (n) => (Math.round(n * 1e5) / 1e5).toString();
// an inventory view fitted to the posed points, as fitGui fits elements
function fitGuiPoints(points) {
  const els = points.map((p) => ({ from: p, to: p }));
  return fitGui(els);
}
const SHELL_TEX = new Set();
function writeObj(id, quads, particle, from = 'clockwork-wastes-salvage.gen.js') {
  const lines = [`# ${id}: written by design/models/clockwork-wastes.export.js from ${from} (block units, v down)`, 'mtllib wastes_shells.mtl'];
  const vs = [], vts = [], vns = [], faces = {};
  for (const q of quads) {
    used.add(q.tex); SHELL_TEX.add(q.tex);
    const idx = q.pts.map((p, n) => {
      vs.push(`v ${fmt(p[0] / 16)} ${fmt(p[1] / 16)} ${fmt(p[2] / 16)}`);
      vts.push(`vt ${fmt(q.uvs[n][0] / 16)} ${fmt(q.uvs[n][1] / 16)}`);
      return vs.length;
    });
    vns.push(`vn ${fmt(q.normal[0])} ${fmt(q.normal[1])} ${fmt(q.normal[2])}`);
    (faces[q.tex] = faces[q.tex] || []).push('f ' + idx.map((i) => `${i}/${i}/${vns.length}`).join(' '));
  }
  lines.push(...vs, ...vts, ...vns);
  for (const [tex, fs_] of Object.entries(faces)) lines.push(`usemtl ${tex}`, ...fs_);
  fs.writeFileSync(path.join(ASSETS, `models/block/wastes/${id}.obj`), lines.join('\n') + '\n');
  const textures = { particle: ref(particle) };
  for (const tex of Object.keys(faces)) textures[tex] = ref(tex);
  used.add(particle);
  return { parent: 'minecraft:block/block', loader: 'neoforge:obj', model: `${NS}:models/block/wastes/${id}.obj`, flip_v: false,
    emissive_ambient: false, render_type: 'minecraft:cutout', ambientocclusion: false, textures };
}
const SHELLS = {
  worker_shell: { rig: () => S.worker(), poses: { slumped: 'worker_slumped', fallen: 'worker_fallen', frozen: 'worker_frozen' }, particle: 'va' },
  resistance_shell: { rig: (pose) => S.resistance(pose === 'fallen'), poses: { slumped: 'res_slumped', fallen: 'res_fallen', reaching: 'res_reaching' }, particle: 'res_casing' },
};
const SHELL_BOUNDS = {};
for (const [id, s] of Object.entries(SHELLS)) {
  const variants = {};
  for (const [pose, key] of Object.entries(s.poses)) {
    const rig = s.rig(pose);
    // the rest pose's cubes, for the z-fight check (posed bones only move whole cubes)
    const flat = []; const collect = (n) => { flat.push(...(n.c || [])); (n.k || []).forEach(collect); }; collect(rig);
    zcheckModel(`${id}_${pose}`, flat);
    const quads = posedQuads(rig, S.POSES[key]);
    const json = writeObj(`${id}_${pose}`, quads, s.particle);
    writeJson(`models/block/wastes/${id}_${pose}.json`, json);
    const pts = quads.flatMap((q) => q.pts);
    SHELL_BOUNDS[`${id}_${pose}`] = [0, 1, 2].map((i) => [r3(Math.min(...pts.map((p) => p[i]))), r3(Math.max(...pts.map((p) => p[i])))]);
    if (pose === 'slumped') writeJson(`models/item/${id}.json`, { ...json, display: { gui: fitGuiPoints(pts) } });
    for (const [facing, y] of Object.entries(HORIZONTAL)) variants[`facing=${facing},pose=${pose}`] = { model: model(`${id}_${pose}`), ...yrot(y) };
  }
  writeJson(`blockstates/${id}.json`, { variants });
}
// ================================================================ phase 2: the powered city blocks
// From clockwork-wastes-city.gen.js (Substation, City Vault, Sliding Door, Derelict lamps and cable) and
// clockwork-wastes-reactors.gen.js (Conduit Pylon, Derelict Star Reactor, Star Reactor). Each block's still parts are its
// block model; what moves or glows is a part model of its own (models/block/wastes/<name>.json, loaded standalone),
// posed and lit by the block's renderer. Glass is translucent, in its own models.
const PART_NAMES = [];
// the phase-2 blocks
const CITY_IDS = ['substation', 'city_vault', 'sliding_door', 'derelict_street_lamp', 'derelict_wall_lamp', 'derelict_cable', 'conduit_pylon',
  'derelict_star_reactor', 'star_reactor'];
const part = (name, cubes, particle, o) => { PART_NAMES.push(name); writeJson(`models/block/wastes/${name}.json`, blockModel(cubes, particle, o)); };
const blockJson = (name, cubes, particle, o) => writeJson(`models/block/wastes/${name}.json`, blockModel(cubes, particle, o));
const TRANSLUCENT = { renderType: 'minecraft:translucent' };
const moveY = (cubes, dy) => move(cubes, [0, dy, 0]);
const remap = (cubes, map) => cubes.map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([d, t]) => [d, (t && map[t]) || t])) }));
const horizontalVariants = (key, models) => Object.fromEntries(Object.entries(HORIZONTAL).flatMap(([f, y]) =>
  Object.entries(models).map(([k, m]) => [`facing=${f}${k ? ',' + k : ''}`, { model: model(m), ...yrot(y) }])));
// a quarter turn of a cube that has its own rotation: the box turns as quarter() turns it, and its rotation's axis with
// it (an axis turned onto its own negative turns the other way)
function quarterAny(cubes, axis, deg, o) {
  return cubes.map((c) => {
    if (!c.rot) return quarter([c], axis, deg, o)[0];
    const q = quarter([{ ...c, rot: null }], axis, deg, o)[0];
    const v = turnPoint(VEC[{ x: 'east', y: 'up', z: 'south' }[c.rot[0]]], axis, deg, [0, 0, 0]);
    const k = v.findIndex((n) => Math.abs(n) > 0.5);
    return { ...q, rot: ['xyz'[k], v[k] > 0 ? c.rot[1] : -c.rot[1], turnPoint(c.rot[2], axis, deg, o).map(r3)] };
  });
}

// textures both phase-2 generators paint under a name the salvage set already uses: theirs are kept under their own
// names (so phase 1's models keep phase 1's pictures)
const P2_TEX = {};
function takeTextures(load, rename) {
  const before = { ...T.OWN };
  const mod = load();
  const map = {};
  for (const [key, own] of Object.entries(rename)) {
    if (before[key] && !before[key].equals(T.OWN[key])) {
      T.OWN[own] = T.OWN[key];
      T.OWN[key] = before[key];
      map[key] = own;
    }
  }
  return { mod, map };
}
const CITY = takeTextures(() => require('./clockwork-wastes-city.gen.js'), { copper: 'terminal_copper', lens_ring: 'lamp_lens_ring' });
const REACT = takeTextures(() => require('./clockwork-wastes-reactors.gen.js'), { brass: 'reactor_brass', copper: 'reactor_copper' });
const CG = new Proxy({}, { get: (_, k) => (...a) => { const r = CITY.mod[k](...a); return Array.isArray(r) ? remap(r, CITY.map) : Object.fromEntries(Object.entries(r).map(([n, l]) => [n, remap(l, CITY.map)])); } });
const RG = new Proxy({}, { get: (_, k) => (...a) => { const r = REACT.mod[k](...a); return Array.isArray(r) ? remap(r, REACT.map) : Object.fromEntries(Object.entries(r).map(([n, l]) => [n, Array.isArray(l) ? remap(l, REACT.map) : remap([l], REACT.map)[0]])); } });
const GLOW = { shade: false }, box = M.box;
const P2_ICONS = {};

// ---------- the Substation (1 x 2): the static cabinet cut at y 16; lever and glows are parts ----------
{
  const s = CG.substation();
  // the frame's lit seams: the preview drew them on the dark ones (coplanar); here 0.25 px in front of them
  s.seams = move(s.seams, [0, 0, -0.25]);
  zcheckModel('substation', [...s.body, ...s.seams, ...s.sigil, ...s.flash, ...s.column, ...s.lever]);
  blockJson('substation_lower', slice(s.body, 'y', 0), 'steel');
  blockJson('substation_upper', slice(s.body, 'y', 16), 'steel');
  part('substation_lever', s.lever, 'steel');
  part('substation_seams', s.seams, 'steel');
  part('substation_sigil', s.sigil, 'steel');
  part('substation_flash', s.flash, 'steel');
  part('substation_column', s.column, 'steel');
  writeJson('blockstates/substation.json', { variants: horizontalVariants('substation', { 'half=lower': 'substation_lower', 'half=upper': 'substation_upper' }) });
  P2_ICONS.substation = true;
}

// ---------- the City Vault: body static; six blades (one model, posed per blade), their seams and the pip are parts ----------
{
  const body = CG.vault(), C5 = CITY.mod.BLADE_Z(5);
  zcheckModel('city_vault', [...body, ...CG.blade(5), ...CG.bladeSeam(5), box('pip', [7.5, 7.5, 1.5], [8.5, 8.5, 1.5], { north: 'flash' }, GLOW)]);
  blockJson('city_vault', body, M.VA_BODY);
  part('city_vault_blade', CG.blade(5), 'va');
  part('city_vault_blade_seam', CG.bladeSeam(5), 'va');
  part('city_vault_pip', [box('pip', [7.5, 7.5, 1.5], [8.5, 8.5, 1.5], { north: 'flash' }, GLOW)], 'va');
  writeJson('blockstates/city_vault.json', { variants: horizontalVariants('city_vault', { '': 'city_vault' }) });
  // the item: the closed vault exactly, its blades at 60k degrees (a block model can't turn them so): baked to an OBJ
  // as the shells are, from a rig of the body and one bone per blade
  const rig = { n: 'vault', p: [8, 8, 8], c: body, k: [0, 1, 2, 3, 4, 5].map((k) => ({ n: 'blade_' + k, p: [8, 8, CITY.mod.BLADE_Z(k)], c: CG.blade(k) })) };
  const pose = { r: Object.fromEntries([0, 1, 2, 3, 4, 5].map((k) => ['blade_' + k, [0, 0, 60 * k]])), lift: [0, 0, 0] };
  const quads = posedQuads(rig, pose);
  const json = writeObj('city_vault_item', quads, M.VA_BODY, 'clockwork-wastes-city.gen.js');
  writeJson('models/item/city_vault.json', { ...json, display: { gui: fitGuiPoints(quads.flatMap((q) => q.pts)) } });
  if (C5 !== 2.25) throw new Error('blade 5 moved: ' + C5);
}

// ---------- the Sliding Door (2 x 3): the jamb cut per cell; leaves and glows are parts, 16 px down ----------
{
  const jamb = CG.jamb();
  // the jamb's lit grooves: the preview put them 0.25 px inside the jamb's faces (hidden); here 0.25 px proud of them
  const leafL = CG.leaf('l'), leafR = CG.leaf('r'), seam = CG.doorSeam('seam_strip_lit', GLOW), chain = [...CG.chainLit('l'), ...CG.chainLit('r')], jglow = CG.jambGlow().map((c) => move([c], [c.faces.east ? 0.5 : -0.5, 0, 0])[0]);
  zcheckModel('sliding_door', [...jamb, ...leafL, ...leafR, ...seam, ...chain, ...jglow]);
  const variants = {};
  for (const col of [0, 1]) for (const row of [0, 1, 2]) {
    const xs = col ? [16, 48, 16] : [-16, 16, 0];
    const cubes = cut(cut(jamb, 'x', ...xs), 'y', row * 16, row === 2 ? 64 : row * 16 + 16, row * 16);
    blockJson(`sliding_door_${col}_${row}`, cubes, M.VA_BODY);
    for (const [f, y] of Object.entries(HORIZONTAL)) variants[`facing=${f},col=${col},row=${row}`] = { model: model(`sliding_door_${col}_${row}`), ...yrot(y) };
  }
  writeJson('blockstates/sliding_door.json', { variants });
  part('sliding_door_leaf_l', moveY(leafL, -16), M.VA_BODY);
  part('sliding_door_leaf_r', moveY(leafR, -16), M.VA_BODY);
  part('sliding_door_seam', moveY(seam, -16), M.VA_BODY);
  part('sliding_door_chain', moveY(chain, -16), M.VA_BODY);
  part('sliding_door_jamb_glow', moveY(jglow, -16), M.VA_BODY);
  P2_ICONS.sliding_door = true;
}

// ---------- the Derelict lamps ----------
{
  const foot = CG.postFoot(false), pole = CG.postPole(), head = CG.postHead(false), snapped = CG.postHead(true), glow = CG.lampGlow('lamp_glow');
  zcheckModel('derelict_street_lamp_head', [...head, ...glow]);
  zcheckModel('derelict_street_lamp_foot', foot);
  zcheckModel('derelict_street_lamp_head_snapped', snapped);
  blockJson('derelict_street_lamp_foot', foot, 'iron');
  blockJson('derelict_street_lamp_pole', pole, 'iron');
  blockJson('derelict_street_lamp_head', head, 'iron');
  blockJson('derelict_street_lamp_head_snapped', snapped, 'iron');
  part('derelict_street_lamp_glow', glow, 'iron');
  // the leaning post (D41; the preview's lamp_leaning: its group turned 22.5 degrees about x through its foot, (8, 0, 8),
  // so the head leans away from the crook): each block of the post has its own model with every part turned so about
  // the post's foot, k blocks below it, so the three slices line up into one leaning post. Its foot is the chipped one,
  // as the preview's. A snapped head's crook (bent -45 about x) and the lean make one turn of -22.5.
  const LEAN = 22.5, lean = (cubes, k) => cubes.map((c) => compose(c, 'x', LEAN, [8, -16 * k, 8]));
  const footL = lean(CG.postFoot(true), 0), poleL = lean(pole, 1), headL = lean(head, 2), snappedL = lean(snapped, 2), glowL = lean(glow, 2);
  // (turned as one, they fight no more than the upright post's parts, checked above)
  blockJson('derelict_street_lamp_foot_leaning', footL, 'iron');
  blockJson('derelict_street_lamp_pole_leaning', poleL, 'iron');
  blockJson('derelict_street_lamp_head_leaning', headL, 'iron');
  blockJson('derelict_street_lamp_head_snapped_leaning', snappedL, 'iron');
  part('derelict_street_lamp_glow_leaning', glowL, 'iron');
  writeJson('blockstates/derelict_street_lamp.json', { variants: horizontalVariants('derelict_street_lamp', Object.fromEntries([false, true].flatMap((l) => {
    const s = l ? '_leaning' : '';
    return [[`leaning=${l},part=foot`, 'derelict_street_lamp_foot' + s], [`leaning=${l},part=pole`, 'derelict_street_lamp_pole' + s],
      [`leaning=${l},part=head,snapped=false`, 'derelict_street_lamp_head' + s], [`leaning=${l},part=head,snapped=true`, 'derelict_street_lamp_head_snapped' + s]];
  }))) });
  P2_ICONS.derelict_street_lamp = true;

  const wall = CG.wallLamp(), wglow = CG.wallLampGlow();
  zcheckModel('derelict_wall_lamp', [...wall, ...wglow]);
  blockJson('derelict_wall_lamp', wall, 'iron');
  part('derelict_wall_lamp_glow', wglow, 'iron');
  writeJson('blockstates/derelict_wall_lamp.json', { variants: horizontalVariants('derelict_wall_lamp', { '': 'derelict_wall_lamp' }) });
  writeJson('models/item/derelict_wall_lamp.json', blockModel(wall, 'iron', { gui: true }));
}

// ---------- the Derelict Cable: an arm per side (the east half of the preview's run, turned), a knot at corners ----------
let CABLE_TURN;
// the Derelict Cable's corner knot (block px, lying across a run drawn east to west), shared by the floor cable and the riser foot
const cableKnot = () => [box('knot', [6.75, 0, 6.5], [9.25, 1.5, 9.5], { all: 'va', down: null })];
{
  const arm = cut(CG.floorCable('cable'), 'x', 8, 16, 0), armGlow = cut(CG.floorCableGlow('cable'), 'x', 8, 16, 0);
  // the knot: a clip across the middle, over where two arms' strands cross at a corner. D74/D76: 2.5 px wide (x 6.75-9.25,
  // previewed in clockwork-wastes-cable-knot.gen.js), so the strands of an arm along its long side no longer share its side
  // faces (at 2 px it was exactly as wide as the strand pair: a z-fight at every corner)
  const knot = cableKnot();
  const west = (cubes) => quarter(cubes, 'y', 180, [8, 0, 8]);
  zcheckModel('derelict_cable (straight)', [...arm, ...armGlow, ...west(arm), ...west(armGlow)]);
  // the knot against every arm it can meet (east as modelled, west, and north and south turned a quarter), one at a time and
  // all four at a cross, each with its lit line
  const ARM_TURN = { east: 0, south: 270, west: 180, north: 90 };
  const armTo = (cubes, d) => (ARM_TURN[d] ? quarter(cubes, 'y', ARM_TURN[d], [8, 0, 8]) : cubes);
  for (const d of Object.keys(ARM_TURN)) zcheckModel('derelict_cable (knot, ' + d + ' arm)', [...armTo(arm, d), ...armTo(armGlow, d), ...knot]);
  zcheckModel('derelict_cable (knot, cross)', [...Object.keys(ARM_TURN).flatMap((d) => [...armTo(arm, d), ...armTo(armGlow, d)]), ...knot]);
  blockJson('derelict_cable_arm', arm, 'cable');
  blockJson('derelict_cable_arm_glow', armGlow, 'cable', { emissive: true });
  blockJson('derelict_cable_knot', knot, 'cable');
  // on any surface (the floor's run turned onto a wall or the ceiling): 'surface' is the side of its block it lies on, and
  // an arm runs each way along that side it joins. The floor's models turned by the blockstate (x, then y, in quarter
  // turns, as vanilla turns a model: v -> Ry(-y) Rx(-x) v), the turn found for each surface and arm.
  const V = { down: [0, -1, 0], up: [0, 1, 0], north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0] };
  const bsTurn = (v, x, y) => turnPoint(turnPoint(v, 'x', -x, [0, 0, 0]), 'y', -y, [0, 0, 0]);
  const vdir = (v) => Object.keys(V).find((d) => same(V[d], v));
  // a blockstate turns x before y, so an arm along x never turns upright: the arm (and its line) are also exported
  // turned a quarter to run south, for the arms up and down a wall. D76: turned 270 (east -> south); it was turned 90, which
  // runs north, so on every wall the up and down arms were drawn on each other's half (WastesGameTests.cableWallArmsPointTheirWay)
  blockJson('derelict_cable_arm_south', quarter(arm, 'y', 270, [8, 0, 8]), 'cable');
  blockJson('derelict_cable_arm_glow_south', quarter(armGlow, 'y', 270, [8, 0, 8]), 'cable', { emissive: true });
  // a model and turn that lay the floor's 'down' on the surface and its arm along d (the south-running one if it must)
  const turnFor = (surface, d) => {
    for (const [suffix, arm0] of [['', V.east], ['_south', V.south]])
      for (const x of [0, 90, 180, 270]) for (const y of [0, 90, 180, 270])
        if (vdir(bsTurn(V.down, x, y)) === surface && vdir(bsTurn(arm0, x, y)) === d) return { suffix, rot: { ...(x ? { x } : {}), ...(y ? { y } : {}) } };
    throw new Error('no turn for ' + surface + ' ' + d);
  };
  const armAt = (name, s, d) => { const t = turnFor(s, d); return { model: model(name + t.suffix), ...t.rot }; };
  const along = (s) => Object.keys(V).filter((d) => V[d].every((n, i) => n * V[s][i] === 0));
  const multipart = [];
  for (const s of Object.keys(V)) {
    const arms = along(s);
    // (girder 'none': the run along its side; a mast riser, D72, draws its own models, added in the city props section)
    for (const d of arms) multipart.push({ when: { girder: 'none', surface: s, [d]: 'true' }, apply: armAt('derelict_cable_arm', s, d) });
    // a knot where two arms meet at a corner (two along the side, at right angles)
    const corners = [];
    for (let i = 0; i < arms.length; i++) for (let j = i + 1; j < arms.length; j++)
      if (V[arms[i]].every((n, k) => n * V[arms[j]][k] === 0)) corners.push({ girder: 'none', surface: s, [arms[i]]: 'true', [arms[j]]: 'true' });
    multipart.push({ when: { OR: corners }, apply: { model: model('derelict_cable_knot'), ...turnFor(s, arms[0]).rot } });
    for (const d of arms) multipart.push({ when: { girder: 'none', surface: s, [d]: 'true', lit: 'true' }, apply: armAt('derelict_cable_arm_glow', s, d) });
  }
  writeJson('blockstates/derelict_cable.json', { multipart });
  writeJson('models/item/derelict_cable.json', blockModel([...arm, ...west(arm)], 'cable', { gui: true }));
  // for the mast riser's floor arms (the city props section)
  CABLE_TURN = { turnFor, armAt, knotTurn: (s) => turnFor(s, along(s)[0]).rot, arm, armGlow, armTo };
}

// ---------- the Conduit Pylon (1 x 3): the shaft cut per block; crown, fins and core are parts in the top block ----------
{
  const shaft = RG.pylonShaft(false), groove = RG.pylonGroove();
  const stat = [...shaft.body, ...shaft.seams, ...groove];
  // lit seams: the preview swapped dark strips for lit ones; here the dark stay in the block model, so the lit ones
  // sit 0.25 px further out, over them
  const out = (c) => ({ ...c, from: [c.from[0], c.from[1], c.from[2] + (c.faces.north ? -0.25 : 0.25)], to: [c.to[0], c.to[1], c.to[2] + (c.faces.north ? -0.25 : 0.25)] });
  const seams = RG.pylonShaft(true).seams.map(out), grooveGlow = RG.pylonGrooveGlow();
  const intact = RG.crown('intact'), cracked = RG.crown('cracked'), broken = RG.crown('broken');
  const blaze = { ...intact.core, faces: Object.fromEntries(Object.entries(intact.core.faces).map(([d, t]) => [d, t && 'core_blaze'])) };
  zcheckModel('conduit_pylon', [...stat, ...seams, ...grooveGlow, ...intact.cage, intact.core]);
  ['bottom', 'middle', 'top'].forEach((p, k) => blockJson(`conduit_pylon_${p}`, slice(stat, 'y', k * 16), M.VA_BODY));
  const variants = {};
  for (const [f, y] of Object.entries(HORIZONTAL)) for (const p of ['bottom', 'middle', 'top']) variants[`facing=${f},part=${p}`] = { model: model(`conduit_pylon_${p}`), ...yrot(y) };
  writeJson('blockstates/conduit_pylon.json', { variants });
  part('conduit_pylon_crown', moveY(intact.cage, -32), 'va');
  part('conduit_pylon_fin', moveY(RG.fin(0), -32), 'va');
  part('conduit_pylon_fin_stub', moveY(RG.finStub(0), -32), 'va');
  part('conduit_pylon_core', moveY([intact.core], -32), 'va');
  part('conduit_pylon_core_cracked', moveY([cracked.core], -32), 'va');
  part('conduit_pylon_core_dark', moveY([broken.core], -32), 'va');
  part('conduit_pylon_core_blaze', moveY([blaze], -32), 'va');
  part('conduit_pylon_seams', moveY(seams, -16), 'va');
  part('conduit_pylon_groove_glow', grooveGlow, 'va');
  P2_ICONS.conduit_pylon = true;
}

// ---------- the Derelict Star Reactor (3 x 3 x 3): base in the controller, glass in the block above it ----------
{
  const turn45 = (cubes) => cubes.map((c) => ({ ...c, rot: ['y', 45, [8, 6, 8]] }));
  const base = [...RG.derelictBase(), ...turn45(RG.grooves())];
  const S_ = REACT.mod.SPH;
  const glass = RG.sphere('sphere', ...S_, 'smoke_glass'), cracked = RG.sphere('sphere', ...S_, 'smoke_glass_cracked');
  const core = RG.sphere('sphere_lit', 8, 30.5, 8, 4, 'sphere_lit').map((c) => ({ ...c, shade: false }));
  zcheckModel('derelict_star_reactor', [...base, ...glass, ...core]);
  blockJson('derelict_star_reactor', base, M.VA_BODY);
  blockJson('derelict_star_reactor_glass', moveY(glass, -16), M.VA_BODY, TRANSLUCENT);
  blockJson('derelict_star_reactor_glass_cracked', moveY(cracked, -16), M.VA_BODY, TRANSLUCENT);
  blockJson('derelict_star_reactor_empty', [], M.VA_BODY);
  part('derelict_star_reactor_arm', moveY(RG.arm(0), -16), 'va');
  part('derelict_star_reactor_arm_seam', moveY(RG.armSeam(0), -16), 'va');
  part('derelict_star_reactor_core', moveY(core, -16), 'va', TRANSLUCENT);
  part('derelict_star_reactor_grooves_glow', turn45(RG.grooveGlow()), 'va');
  const others = Array.from({ length: 27 }, (_, i) => i).filter((i) => i !== 4 && i !== 13);
  writeJson('blockstates/derelict_star_reactor.json', { multipart: [
    { when: { part: '4' }, apply: { model: model('derelict_star_reactor') } },
    { when: { part: '13', salvaged: 'false' }, apply: { model: model('derelict_star_reactor_glass') } },
    { when: { part: '13', salvaged: 'true' }, apply: { model: model('derelict_star_reactor_glass_cracked') } },
    { when: { part: others.join('|') }, apply: { model: model('derelict_star_reactor_empty') } },
  ] });
  // the item: base, the four arms (quarter turns of the first, their own tilts turned with them) and the dark glass,
  // dropped 8 px into the model's bounds and fitted to the slot
  const arms = [0, 1, 2, 3].flatMap((k) => (k ? quarterAny(RG.arm(k), 'y', 90 * k, [8, 0, 8]) : RG.arm(0)));
  writeJson('models/item/derelict_star_reactor.json', blockModel(moveY([...base, ...arms, ...glass], -8), M.VA_BODY, { gui: true }));
}

// ---------- the Star Reactor: casing static, glass translucent, dark pips by its upgrade; star, ring and lit pips parts ----------
{
  const casing = RG.casing();
  const glass = [...['north', 'east', 'west'].flatMap((d) => RG.portGlass(d, 'port_glass', false)), ...RG.sphere('rsphere', 8, 8, 8, 4, 'smoke_glass')];
  const pips = (n) => REACT.mod.PIPS(n).map(([x, z], i) => REACT.mod.pip(`pipd_${i}`, x, z, 'pip_dark', 16.25));
  const lit = remap([REACT.mod.pip('pip', 8, 8, 'pip_lit', 16.5, GLOW)], REACT.map);
  // the ring's segments are square across: one turned by an angle is one turned by that angle less a quarter turn, so
  // each is brought within the +-45 degrees a block model allows
  const within = (a) => { let r = ((a % 90) + 90) % 90; if (r > 45) r -= 90; return r; };
  const orbit = RG.orbit().map((c) => { const a = within(c.rot[1]); return a ? { ...c, rot: ['y', a, c.rot[2]] } : { ...c, rot: null }; });
  const portsLit = ['north', 'east', 'west'].flatMap((d) => RG.portGlass(d, 'port_lit', true));
  zcheckModel('star_reactor', [...casing, ...glass, ...pips(12), ...REACT.mod.PIPS(12).map(([x, z]) => move(lit, [x - 8, 0, z - 8])[0]), ...portsLit]);
  zcheckModel('star_reactor (8)', [...casing, ...pips(8), ...REACT.mod.PIPS(8).map(([x, z]) => move(lit, [x - 8, 0, z - 8])[0])]);
  blockJson('star_reactor', casing, 'reactor_brass');
  blockJson('star_reactor_glass', glass, 'reactor_brass', TRANSLUCENT);
  blockJson('star_reactor_pips8', pips(8), 'reactor_brass');
  blockJson('star_reactor_pips12', pips(12), 'reactor_brass');
  part('star_reactor_orbit', orbit, 'va');
  part('star_reactor_pip', lit, 'reactor_brass');
  part('star_reactor_ports_lit', portsLit, 'reactor_brass', TRANSLUCENT);
  const TURN6 = { south: {}, west: { y: 90 }, north: { y: 180 }, east: { y: 270 }, up: { x: 90 }, down: { x: 270 } };
  writeJson('blockstates/star_reactor.json', { multipart: Object.entries(TURN6).flatMap(([f, r]) => [
    { when: { facing: f }, apply: { model: model('star_reactor'), ...r } },
    { when: { facing: f }, apply: { model: model('star_reactor_glass'), ...r } },
    { when: { facing: f, upgraded: 'false' }, apply: { model: model('star_reactor_pips8'), ...r } },
    { when: { facing: f, upgraded: 'true' }, apply: { model: model('star_reactor_pips12'), ...r } },
  ]) });
  writeJson('models/item/star_reactor.json', { parent: model('star_reactor') });
}


// ---------- items ----------
// Flat icons for the tall blocks (the Cuckoo Clock lesson): hand-drawn 16 x 16, a step lighter than the block textures
const ICONS = {
  maker_statue: { rows: [
    '      MMMM      ',
    '     MLLLLM     ',
    '     LVVVVD     ',
    '     LVggVD     ',
    '     LVVVVD     ',
    '     LLbbMD     ',
    '   LLLLLLMMDD   ',
    '   LMLLLLLMMD   ',
    '   LMLSSSSMMD   ',
    '   LMbbbbbbbD   ',
    '    LMLLMLMD    ',
    '    LMLLMLMD    ',
    '   LLMLLMLMDD   ',
    '   LLMLLMLMDD   ',
    '  bbbbbbbbbbbb  ',
    '  MMMMMMMMMMDD  '],
    pal: { L: '#a59f80', M: '#8d876a', D: '#6c6750', S: '#b3ad8e', V: '#2a2838', g: '#4c475f', b: '#a3873f' } },
};
// Phase 2's flat icons for the tall city blocks, painted as rectangles (key, x0, y0, x1, y1, inclusive) onto a 16 x 16 grid,
// later ones over earlier ones; a step lighter than the block textures, as phase 1's
const rectRows = (rects) => {
  const g = Array.from({ length: 16 }, () => Array(16).fill(' '));
  for (const [k, x0, y0, x1, y1] of rects) for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) g[y][x] = k;
  return g.map((r) => r.join(''));
};
const VA_ICON = { H: '#5d5778', A: '#4c475f', B: '#3a3650', C: '#2a2838', L: '#7f76a8', S: '#b8b0d8' };
// the Charging Alcove (D74/D76, approved as previewed in design/texture-reviews/clockwork-wastes-icons/charging-alcove-icon.js,
// its plain version: no plate seams): from the front, the arch, the two cheeks (lit edge H left and top, dark edge C right and
// bottom), the dark niche, the contact plate up its back a pixel left of phase 1's, the empty corner shelf on the right at
// chest height (top h, front e, stepped bracket b; a player's alcove has no trinket), the sill. Its field 'A' has the plate's
// grain (three close shades), painted per pixel ('paint' below), as the script draws it.
ICONS.charging_alcove = { rows: [
  '     HHHHHH     ',
  '    HAAAAAAC    ',
  '   HAAkkkkAAC   ',
  '   HAkkkkkkAC   ',
  '   HAkkkkkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskhhAC   ',
  '   HAkkskeeAC   ',
  '   HAkkkkkbAC   ',
  '   HAkkkkkkAC   ',
  '   HABBBBBBAC   ',
  '   HAAAAAAAAC   ',
  '   CCCCCCCCCC   '],
  pal: { ...VA_ICON, k: '#1b1a22', s: '#6d6696', h: VA_ICON.H, e: VA_ICON.B, b: VA_ICON.B },
  paint: (x, y, k) => { if (k !== 'A') return null; const r = hash(x, y, 611); return r < 0.12 ? '#48435a' : r > 0.93 ? '#514b66' : null; } };
Object.assign(ICONS, {
  // the makers' steel cabinet, the uprising's charge column (lit a third of the way) and sigil, the knife switch shut
  substation: { rows: rectRows([
    ['I', 5, 0, 5, 0], ['I', 10, 0, 10, 0],
    ['s', 3, 1, 12, 14], ['h', 3, 1, 11, 1], ['h', 3, 2, 3, 14], ['d', 12, 2, 12, 14], ['d', 4, 14, 12, 14],
    ['A', 5, 2, 10, 4], ['H', 5, 2, 5, 2], ['H', 10, 2, 10, 2], ['H', 5, 4, 5, 4], ['H', 10, 4, 10, 4], ['L', 7, 3, 8, 3],
    ['A', 6, 5, 9, 13], ['C', 7, 6, 8, 12], ['B', 7, 9, 8, 12], ['L', 7, 10, 8, 12], ['S', 7, 10, 7, 10],
    ['b', 13, 4, 13, 4], ['i', 13, 5, 13, 11], ['k', 13, 12, 13, 13], ['b', 14, 9, 14, 10],
    ['e', 2, 15, 13, 15]]),
    pal: { s: '#535a63', h: '#6a717b', d: '#3a3f46', e: '#2a2e33', I: VA_ICON.H, A: VA_ICON.A, H: VA_ICON.H, B: VA_ICON.B, C: VA_ICON.C,
      L: VA_ICON.L, S: VA_ICON.S, b: '#a3873f', i: '#6a717b', k: '#6f4d36' } },
  // two leaves meeting at a stepped seam, three panels each, the broken chain across the join, in a jamb
  sliding_door: { rows: rectRows([
    ['J', 3, 0, 12, 15], ['A', 4, 1, 11, 15],
    ['B', 5, 2, 6, 4], ['B', 9, 2, 10, 4], ['B', 5, 6, 6, 9], ['B', 9, 6, 10, 9], ['B', 5, 11, 6, 14], ['B', 9, 11, 10, 14],
    ['s', 8, 1, 8, 4], ['s', 7, 5, 7, 8], ['s', 8, 9, 8, 12], ['s', 7, 13, 7, 15], ['s', 7, 4, 8, 4], ['s', 7, 8, 8, 8], ['s', 7, 12, 8, 12],
    ['c', 5, 7, 6, 7], ['c', 9, 7, 10, 7], ['K', 3, 15, 12, 15]]),
    pal: { J: VA_ICON.H, A: VA_ICON.A, B: VA_ICON.B, s: VA_ICON.C, c: VA_ICON.L, K: VA_ICON.C } },
  // the makers' cast-iron post, its crook reaching over the street, the uprising's hooded lens hanging from it
  derelict_street_lamp: { rows: rectRows([
    ['p', 11, 2, 11, 13], ['P', 12, 2, 12, 13], ['r', 11, 7, 11, 9],
    ['i', 10, 2, 13, 3], ['i', 11, 0, 12, 1], ['i', 4, 0, 10, 0], ['i', 9, 1, 9, 1], ['i', 8, 2, 8, 2],
    ['V', 3, 0, 5, 1], ['V', 2, 2, 6, 3], ['v', 3, 2, 5, 2], ['V', 2, 4, 6, 4], ['L', 3, 5, 5, 5], ['S', 4, 5, 4, 5],
    ['P', 10, 12, 13, 13], ['p', 10, 12, 10, 13], ['D', 9, 14, 14, 15], ['d', 9, 14, 14, 14]]),
    pal: { p: '#5b6068', P: '#454a51', r: '#7f4d30', i: '#5b6068', D: '#3a3f45', d: '#4a5059', V: VA_ICON.A, v: VA_ICON.H, L: VA_ICON.L, S: VA_ICON.S } },
  // the narrow obelisk, its crown of fins round the starlight core in a cage
  conduit_pylon: { rows: rectRows([
    ['F', 3, 0, 3, 2], ['F', 4, 2, 4, 4], ['F', 12, 0, 12, 2], ['F', 11, 2, 11, 4], ['F', 7, 0, 8, 0],
    ['a', 5, 1, 10, 1], ['a', 5, 2, 5, 4], ['a', 10, 2, 10, 4], ['L', 7, 2, 8, 4], ['S', 7, 3, 7, 3],
    ['P', 5, 5, 10, 5], ['A', 7, 6, 8, 9], ['h', 7, 6, 7, 9], ['A', 6, 10, 9, 12], ['h', 6, 10, 6, 12],
    ['P', 5, 13, 10, 14], ['H', 5, 13, 10, 13], ['D', 4, 15, 11, 15]]),
    pal: { F: VA_ICON.H, a: VA_ICON.A, L: VA_ICON.L, S: VA_ICON.S, P: VA_ICON.A, A: VA_ICON.B, h: VA_ICON.A, H: VA_ICON.H, D: VA_ICON.C } },
});
// the lifts' flat icons (D65, approved in design/texture-reviews/clockwork-wastes-icons/lift-icons.js)
// the call plate: the lens-in-cog (the Substation's sigil, eight teeth) on its square void-alloy mount, chamfered; lit
// (a call waiting) for its icon, dark as placed
const LIFT_COG = ['....##....', '.#..##..#.', '..######..', '..##oo##..', '###oooo###', '###oooo###', '..##oo##..', '..######..', '.#..##..#.', '....##....'];
function callPlateIcon(lit) {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let r = '';
    for (let x = 0; x < 16; x++) {
      const inPlate = x >= 2 && x <= 13 && y >= 2 && y <= 13 && !((x === 2 || x === 13) && (y === 2 || y === 13));
      if (!inPlate) { r += ' '; continue; }
      const k = (LIFT_COG[y - 3] || '')[x - 3];
      if (k === 'o') r += x === 7 && y === 7 ? (lit ? 'S' : 'g') : lit ? 'L' : 'C';
      else if (k === '#') r += 'B';
      else if (y === 2 || x === 2) r += 'H';
      else if (y === 13 || x === 13) r += 'C';
      else r += 'A';
    }
    rows.push(r);
  }
  return { rows, pal: { ...VA_ICON, g: '#4c475f' } };
}
Object.assign(ICONS, {
  // the Lift: the deck halfway up its shaft, from the front: two guide rails, the deck across them (its top, the lit
  // groove along its front edge, its front face), clamps biting the rails, the guard rail (behind) and the west post
  // above it, the control post with its lens, the cross beam under the deck; the empty shaft above and below
  lift: { rows: [
    '                ',
    ' HB          HB ',
    ' HB          HB ',
    ' HB          HB ',
    ' HBBBBBBBBBBBHB ',
    ' HBB      AA HB ',
    ' HBB      SA HB ',
    ' HBB      AA HB ',
    'HHHAAAAAAAAAAHHH',
    'HABLLLLLLLLLLBAH',
    'HBBCCCCCCCCCCBBH',
    ' HB   BBBB   HB ',
    ' HB          HB ',
    ' HB          HB ',
    ' HB          HB ',
    '                '],
    pal: { ...VA_ICON } },
  lift_call_plate: callPlateIcon(true),
});
// the Assembly Clamp's flat icon (D72; review: design/texture-reviews/clockwork-wastes-icons/assembly-clamp-icon.js): the jig
// from the front at stage 5 (the stage its item places, AssemblyClampBlock.ITEM_STAGE): the two uprights on the footplate,
// the whole Worker standing in them (its head with the lens ring and the dark lens, the chest with its brass tag, arms
// hanging to their clamps, legs), the jaws at its chest and hips, and the stowed lead (copper gone to patina) on the east
// upright
ICONS.assembly_clamp = { rows: rectRows([
  ['C', 1, 15, 14, 15],
  ['H', 1, 1, 2, 1], ['A', 13, 1, 14, 1],
  ['H', 1, 2, 1, 14], ['A', 2, 2, 2, 14], ['A', 13, 2, 13, 14], ['B', 14, 2, 14, 14],
  ['p', 13, 3, 13, 4], ['C', 13, 5, 13, 5],
  ['C', 3, 4, 3, 4], ['C', 12, 4, 12, 4], ['C', 3, 7, 3, 7], ['C', 12, 7, 12, 7],
  ['A', 6, 8, 6, 14], ['B', 7, 8, 7, 14], ['A', 8, 8, 8, 14], ['B', 9, 8, 9, 14],
  ['H', 6, 7, 9, 7], ['C', 7, 6, 8, 6],
  ['A', 5, 3, 10, 5], ['H', 5, 3, 9, 3], ['B', 10, 3, 10, 5], ['b', 7, 4, 7, 4],
  ['H', 4, 3, 4, 10], ['B', 11, 3, 11, 10], ['B', 4, 11, 4, 12], ['C', 11, 11, 11, 12],
  ['A', 5, 0, 10, 2], ['H', 6, 0, 9, 2], ['C', 7, 1, 8, 1], ['B', 10, 0, 10, 2]]),
  pal: { ...VA_ICON, p: '#5b7a6c', b: '#a3873f' } };

fs.mkdirSync(path.join(ASSETS, 'textures/item'), { recursive: true });
const review = path.resolve(__dirname, '../texture-reviews/clockwork-wastes-icons');
fs.mkdirSync(review, { recursive: true });
for (const [id, icon] of Object.entries(ICONS)) {
  if (icon.rows.length !== 16 || icon.rows.some((r) => r.length !== 16)) throw new Error(id + ': the icon must be 16 x 16');
  // a 1 px dark outline round the silhouette, as the Cuckoo Clock's icon (texture review, 2026-10-29)
  // (an icon's 'paint' may colour a pixel itself, over its palette key: the alcove's grain)
  const at = (x, y) => (x < 0 || y < 0 || x > 15 || y > 15 ? null : (icon.paint && icon.paint(x, y, icon.rows[y][x])) || icon.pal[icon.rows[y][x]] || null);
  T.paint('item_' + id, (x, y) => at(x, y) || ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => at(x + dx, y + dy)) ? '#121118' : null));
  fs.writeFileSync(path.join(ASSETS, `textures/item/${id}.png`), T.OWN['item_' + id]);
  fs.writeFileSync(path.join(review, id + '.png'), T.OWN['item_' + id]);
}
// phase 2's tall blocks show their icons in hand
for (const id of Object.keys(P2_ICONS)) writeJson(`models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${id}` } });
for (const [id, key] of [['clockwork_scrap', 'icon_scrap'], ['unknown_device', 'icon_device']]) {
  fs.writeFileSync(path.join(ASSETS, `textures/item/${id}.png`), T.OWN[key]);
  writeJson(`models/item/${id}.json`, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/${id}` } });
}

// ---------- data: loot, the pickaxe tag, the crushing recipe ----------
const DATA = path.resolve(ASSETS, '../../data');
const writeData = (rel, obj) => {
  const file = path.join(DATA, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
};
const item = (id, o = {}) => ({ type: 'minecraft:item', name: id, ...o });
const count = (min, max) => ({ function: 'minecraft:set_count', count: min === max ? min : { type: 'minecraft:uniform', min, max } });
const SCRAP = `${NS}:clockwork_scrap`;
// decor drops itself (D37); a three-long one from its bottom part only, like a door
// (the Void-Alloy Plate's models and state are written with the building block set, D75: its faces follow the bond)
const DECOR = [...Object.keys(FULL), 'void_alloy_plate', 'void_alloy_pillar', 'scrap_heap'];
for (const id of DECOR)
  writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(`${NS}:${id}`)], conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
for (const id of ['charging_alcove', 'maker_statue', 'toppled_maker_statue'])
  writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(`${NS}:${id}`)],
    conditions: [{ condition: 'minecraft:block_state_property', block: `${NS}:${id}`, properties: { part: 'bottom' } }, { condition: 'minecraft:survives_explosion' }] }] });
// salvage (D26, P16): broken any way but the wrench, 1 scrap (pickaxe, drill, saw, blast); taken apart with the wrench,
// the full salvage: 1-3 Create parts matching what it was, rarely a Precision Mechanism, and 1-2 scrap
const PARTS_OF = {
  broken_press: [['create:shaft', 3], ['create:andesite_casing', 2], ['create:iron_sheet', 2], ['create:cogwheel', 1]],
  broken_mixer: [['create:whisk', 2], ['create:cogwheel', 2], ['create:andesite_casing', 2], ['create:shaft', 1]],
  broken_deployer: [['create:brass_hand', 2], ['create:andesite_casing', 2], ['create:shaft', 2], ['create:belt_connector', 1]],
  broken_mechanical_arm: [['create:brass_sheet', 2], ['create:cogwheel', 2], ['create:andesite_alloy', 2], ['create:shaft', 1]],
  broken_crushing_wheel: [['create:andesite_alloy', 3], ['create:large_cogwheel', 1], ['create:shaft', 2], ['create:belt_connector', 1]],
};
const MECHANISM = { broken_press: 0.05, broken_mixer: 0.05, broken_deployer: 0.08, broken_mechanical_arm: 0.12, broken_crushing_wheel: 0.05 };
const SALVAGE_IDS = [...Object.keys(PARTS_OF), ...Object.keys(SHELLS)];
for (const id of SALVAGE_IDS)
  writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(SCRAP)] }] });
for (const [id, parts] of Object.entries(PARTS_OF))
  writeData(`${NS}/loot_table/salvage/${id}.json`, { type: 'minecraft:block', pools: [
    { rolls: { type: 'minecraft:uniform', min: 1, max: 3 }, entries: parts.map(([p, w]) => item(p, { weight: w })) },
    { rolls: 1, entries: [item('create:precision_mechanism')], conditions: [{ condition: 'minecraft:random_chance', chance: MECHANISM[id] }] },
    { rolls: 1, entries: [item(SCRAP, { functions: [count(1, 2)] })] }] });
// bot shells (P16): scrap and a chance of an Electron Tube
for (const id of Object.keys(SHELLS))
  writeData(`${NS}/loot_table/salvage/${id}.json`, { type: 'minecraft:block', pools: [
    { rolls: 1, entries: [item(SCRAP, { functions: [count(2, 3)] })] },
    { rolls: 1, entries: [item('create:electron_tube')], conditions: [{ condition: 'minecraft:random_chance', chance: 0.25 }] }] });
const ALL_BLOCKS = [...DECOR, 'charging_alcove', 'maker_statue', 'toppled_maker_statue', ...SALVAGE_IDS];
const pick = path.join(DATA, 'minecraft/tags/block/mineable/pickaxe.json');
const pickTag = JSON.parse(fs.readFileSync(pick, 'utf8'));
for (const id of ALL_BLOCKS) if (!pickTag.values.includes(`${NS}:${id}`)) pickTag.values.push(`${NS}:${id}`);
fs.writeFileSync(pick, JSON.stringify(pickTag, null, 2) + '\n');
// the salvage blocks, for the code and for the city's later phases
writeData(`${NS}/tags/block/salvage.json`, { values: SALVAGE_IDS.map((id) => `${NS}:${id}`) });
// Clockwork Scrap in a Crushing Wheel (P15): iron, copper and brass nuggets, a small chance of zinc
writeData(`${NS}/recipe/crushing/clockwork_scrap.json`, { type: 'create:crushing', ingredients: [{ item: SCRAP }], processing_time: 150,
  results: [{ id: 'minecraft:iron_nugget', count: 3 }, { id: 'create:copper_nugget', count: 2 }, { id: 'create:brass_nugget' },
    { id: 'create:brass_nugget', chance: 0.5 }, { id: 'create:zinc_nugget', chance: 0.1 }] });

// ---------- phase 2's data: what breaking the city blocks gives, the reactor's salvage, the pickaxe tag ----------
// City blocks break into scrap, never into Electric Lights or Power Lines (D16, P16, P25); a multi-block drops once,
// from the part that holds its block entity. The Conduit Pylon can't be mined (its loot is empty); the Star Reactor drops
// itself with its burn and its upgrade (P14, P44).
{
  const scrap = (min, max) => item(SCRAP, min === 1 && max === 1 ? {} : { functions: [count(min, max)] });
  const only = (id, props) => ({ condition: 'minecraft:block_state_property', block: `${NS}:${id}`, properties: props });
  const table = (id, entry, conditions = []) => writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block',
    pools: entry ? [{ rolls: 1, entries: [entry], ...(conditions.length ? { conditions } : {}) }] : [] });
  table('substation', scrap(2, 3), [only('substation', { half: 'lower' })]);
  table('city_vault', scrap(2, 2));
  table('sliding_door', scrap(2, 2), [only('sliding_door', { col: '0', row: '0' })]);
  table('derelict_street_lamp', scrap(1, 1), [only('derelict_street_lamp', { part: 'head' })]);
  table('derelict_wall_lamp', scrap(1, 1));
  table('derelict_cable', scrap(1, 1), [{ condition: 'minecraft:random_chance', chance: 0.25 }]);
  table('conduit_pylon', null);
  table('derelict_star_reactor', scrap(3, 3), [only('derelict_star_reactor', { part: '4' })]);
  table('star_reactor', item(`${NS}:star_reactor`, { functions: [{ function: 'minecraft:copy_components', source: 'block_entity',
    include: [`${NS}:star_burn`, `${NS}:reactor_overhauled`] }] }));
  // the heart's reactor taken apart with a wrench (P13): two Unknown Devices, and the heart has no reactor afterwards
  writeData(`${NS}/loot_table/salvage/derelict_star_reactor.json`, { type: 'minecraft:block', pools: [
    { rolls: 1, entries: [item(`${NS}:unknown_device`, { functions: [count(2, 2)] })] }] });
  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of CITY_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
}

// ================================================================ the lifts (P49, D40)
// From clockwork-wastes-lift.gen.js (approved preview). The Lift block stands at the head of its 2 x 2 shaft, in one
// corner cell (model x+ runs to its facing's clockwise side, z+ away from its front, as the preview's shaft from (0, 0)).
// Everything of it is drawn by its renderer as part models, posed in that corner's frame: the head (the frame on the
// rails and the drive drum), one level of the four guide rails per block down the shaft, and the deck that rides them
// (the moving platform, an entity in game). Cubes are cut per block cell (16 px a block, as everything in game; the
// preview stretched its deck texture over 2 blocks) and split at x 16 into a west and an east part, so each fits a block
// model's bounds (the clamps and rails reach 2-3 px past the shaft on its closed sides). The call plate is a wall block.
const LIFT_IDS = ['lift', 'lift_call_plate'];
{
  const L = require('./clockwork-wastes-lift.gen.js');
  // cut into block cells along x and z (coordinates kept)
  const cells = (cubes) => { let out = cubes; for (const ax of ['x', 'z']) { const next = []; for (let lo = -16; lo < 48; lo += 16) next.push(...cut(out, ax, lo, lo + 16, 0)); out = next; } return out; };
  // the west half (x < 16) as it is; the east half moved 16 px west (the renderer draws it a block east)
  const half = (cubes, east) => (east ? cut(cubes, 'x', 16, 64, 16) : cut(cubes, 'x', -16, 16, 0));
  const deck = cells(L.deck()), deckGlow = cells(L.deckGlow());
  // the head: the frame on the rails' tops and the drum, at the head block's level (the shaft's height is 0 here)
  const HEAD = /^(head_|drum)/;
  const head = cells(L.shaft(0).filter((c) => HEAD.test(c.name)));
  // one block's level of the guide rails and their lit seams (the preview ran them 4 px into the pit and stopped the
  // seams 1 px short of the head; in game each level of the shaft is the same, from its bottom to its head)
  const guides = cut(L.shaft(16).filter((c) => /^guide_/.test(c.name)), 'y', 0, 16, 0);
  const seams = cut(L.shaftGlow(17), 'y', 0, 16, 0);
  const west = (cubes) => cubes.filter((c) => c.from[0] < 16), east = (cubes) => move(cubes.filter((c) => c.from[0] >= 16), [-16, 0, 0]);
  zcheckModel('lift (deck on its rails)', [...deck, ...deckGlow, ...guides, ...seams]);
  zcheckModel('lift (head)', head);
  part('lift_deck_w', half(deck, false), 'va');
  part('lift_deck_e', half(deck, true), 'va');
  part('lift_deck_glow_w', half(deckGlow, false), 'va');
  part('lift_deck_glow_e', half(deckGlow, true), 'va');
  // the head reaches past the shaft on all four sides: four parts, the south ones moved 16 px north as the east ones west
  for (const [n, e] of [['nw', false], ['ne', true]]) {
    part('lift_head_' + n, cut(half(head, e), 'z', -16, 16, 0), 'va');
    part('lift_head_' + n.replace('n', 's'), cut(half(head, e), 'z', 16, 64, 16), 'va');
  }
  part('lift_guides_w', west(guides), M.VA_BODY);
  part('lift_guides_e', east(guides), M.VA_BODY);
  part('lift_guide_seams_w', west(seams), M.VA_BODY);
  part('lift_guide_seams_e', east(seams), M.VA_BODY);
  // the lift block itself draws nothing of its own (its renderer draws the head, rails and deck)
  blockJson('lift', [], 'va');
  writeJson('blockstates/lift.json', { variants: horizontalVariants('lift', { '': 'lift' }) });
  // its item: the flat icon (D65; ICONS: the deck halfway up an open shaft)
  writeJson('models/item/lift.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/lift` } });
  // the call plate: the preview's mount and lens-in-cog on the wall at a landing, 10 px up (the preview set it with its
  // plate toward the wall; here its back is on the wall, the plate facing out); its lens lights while the lift works
  const plate = move(L.callPlate('call'), [4, 10, 15]), lens = move(L.callGlow('call'), [4, 10, 15]);
  zcheckModel('lift_call_plate', [...plate, ...lens]);
  blockJson('lift_call_plate', plate, 'va');
  part('lift_call_plate_lens', lens, 'va');
  writeJson('blockstates/lift_call_plate.json', { variants: horizontalVariants('lift_call_plate', { '': 'lift_call_plate' }) });
  // its item: the flat icon, its lens lit (D65; ICONS)
  writeJson('models/item/lift_call_plate.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/lift_call_plate` } });
  // what breaking them gives (provisional, waiting for the user: scrap, as the other city blocks)
  const LIFT_SCRAP = { lift: [2, 3], lift_call_plate: [1, 1] };
  for (const [id, [min, max]] of Object.entries(LIFT_SCRAP))
    writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(SCRAP, min === max ? {} : { functions: [count(min, max)] })] }] });
  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of LIFT_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
}
// ================================================================ end of the lifts

// ================================================================ the wall vents (D57, D59, D60)
// From clockwork-wastes-vent.gen.js (approved preview): a wall block in the heart hall with a tall chamfered opening at
// the floor, closed by a louvred hatch hinged along its bottom front edge; one curled Scuttler behind it. The body (jambs,
// lintel, chamfer steps, the cavity's back, the cast lip and the dark charging contact) is the block model. The hatch is
// a part the renderer poses (shut; rattling and blown flat in the burst; lying flat in front, spent): its louvres are
// turned 35 degrees, which a block model can't hold, so it's baked to an OBJ as the shells are. The lit seam, the burst's
// flash, the lit contact and the lens glint between the louvres are parts too.
// Rebuild notes: the preview laid its lit contact on the dark one (the same plane, a z-fight while lit); here it's 0.25 px
// in front of it, as the other lit seams. Its hatch seams sat 0.05 px proud of the hatch (dark), 0.3 (lit) and 0.4 (the
// flash): here 0.25, 0.5 and 0.75, each a quarter pixel clear of the one behind it.
const VENT_IDS = ['wall_vent'];
{
  const V = require('./clockwork-wastes-vent.gen.js');
  const body = [...V.ventBody(), ...V.contact(false)];
  const at = (cubes, z, suffix) => cubes.map((c) => ({ ...c, name: c.name + suffix, from: [c.from[0], c.from[1], z], to: [c.to[0], c.to[1], z] }));
  const hatchCubes = [...V.hatch(), ...at(V.hatchSeam('seam_strip_dark'), -0.25, '')];
  const seam = at(V.hatchSeam('seam_strip_lit', GLOW), -0.5, '_g');
  const flash = at(V.hatchSeam('vent_flash', GLOW), -0.75, '_f');
  const contact = move(V.contact(true), [0, 0, -0.25]);
  const glint = V.glint();
  zcheckModel('wall_vent', [...body, ...hatchCubes.filter((c) => !c.rot), ...seam, ...flash, ...contact, ...glint]);
  zcheckModel('wall_vent (louvres)', hatchCubes.filter((c) => c.rot));
  blockJson('wall_vent', body, M.VA_BODY);
  // the hatch as a rig: each louvre a bone turned about its own axis (its cube's rotation), baked shut
  const hatchRig = { n: 'hatch', p: V.HINGE, c: hatchCubes.filter((c) => !c.rot),
    k: hatchCubes.filter((c) => c.rot).map((c) => ({ n: c.name, p: c.rot[2], c: [{ ...c, rot: null }] })) };
  const louvres = Object.fromEntries(hatchCubes.filter((c) => c.rot).map((c) => {
    if (c.rot[0] !== 'x') throw new Error(c.name + ': a louvre turns about x');
    return [c.name, [c.rot[1], 0, 0]];
  }));
  const hatchQuads = posedQuads(hatchRig, { r: louvres, lift: [0, 0, 0] });
  PART_NAMES.push('wall_vent_hatch');
  writeJson('models/block/wastes/wall_vent_hatch.json', writeObj('wall_vent_hatch', hatchQuads, 'va', 'clockwork-wastes-vent.gen.js'));
  part('wall_vent_seam', seam, 'va');
  part('wall_vent_flash', flash, 'va');
  part('wall_vent_contact', contact, 'va');
  part('wall_vent_glint', glint, 'va');
  const variants = {};
  for (const [f, y] of Object.entries(HORIZONTAL)) for (const st of ['sealed', 'live', 'open'])
    variants[`facing=${f},state=${st}`] = { model: model('wall_vent'), ...yrot(y) };
  writeJson('blockstates/wall_vent.json', { variants });
  // the item: the vent shut, its hatch in place (an OBJ of the body and the hatch, fitted to the slot)
  const itemQuads = posedQuads({ n: 'vent', p: [8, 8, 8], c: body, k: [hatchRig] }, { r: louvres, lift: [0, 0, 0] });
  const itemJson = writeObj('wall_vent_item', itemQuads, M.VA_BODY, 'clockwork-wastes-vent.gen.js');
  writeJson('models/item/wall_vent.json', { ...itemJson, display: { gui: fitGuiPoints(itemQuads.flatMap((q) => q.pts)) } });
  // what breaking one gives (provisional, waiting for the user: a scrap, as the other city blocks)
  writeData(`${NS}/loot_table/blocks/wall_vent.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(SCRAP)] }] });
  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of VENT_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
}
// ================================================================ end of the wall vents

// ================================================================ the Derelict Insulator (D61, D64)
// From clockwork-wastes-insulator.gen.js (approved preview): one block, three mounts by the face it's placed on (top: a
// collar whose claws bite the girder below; wall: a plate and braced arm, the wall behind it; hanging: a socket under the
// block above), each carrying the three-shed void-alloy stack. Each mount's body is a block model; its lit neck ring is an
// overlay the multipart draws while its district is fed (lit=true), emissive in its block model as the Derelict Cable's
// line is (no renderer). The spans are the renderer's: the preview's span textures, the bead's flash and the lit line's
// ring_lit are exported for it (no model uses the span ones). 'facing' points away from what it's mounted on: the
// preview's wall is east, so facing west is the model as drawn (y 0); every mount takes the same turn.
// Rebuild notes: the preview's lit neck ring stood 0.1 px proud of its 2.5 px neck (a z-fight once lit): here 0.25 px.
const INSULATOR_IDS = ['derelict_insulator'];
{
  const SPAN_TEX = ['span_side', 'span_side_worn', 'span_top', 'span_top_worn', 'span_break', 'flash', 'ring_lit'];
  // any of its textures painted under a name already taken by a different picture keeps the old picture there and
  // exports the insulator's as insulator_<name>
  const ins = takeTextures(() => require('./clockwork-wastes-insulator.gen.js'),
    Object.fromEntries([...SPAN_TEX, 'va_facet'].map((k) => [k, 'insulator_' + k])));
  const I = ins.mod;
  const FACE_Y = { west: 0, north: 90, east: 180, south: 270 };
  // the ring's four hairline faces, each moved 0.15 px further out along its own facing (0.1 -> 0.25 px off the neck)
  const pull = (cubes) => cubes.map((c) => {
    const dir = Object.keys(c.faces).find((d) => c.faces[d]);
    return move([c], VEC[dir].map((n) => n * 0.15))[0];
  });
  const mounts = { top: I.topMount(), wall: I.wallMount(), hanging: I.hangingMount() };
  const multipart = [];
  for (const [name, m] of Object.entries(mounts)) {
    const body = remap(m.body, ins.map), ring = remap(pull(m.ring), ins.map);
    zcheckModel('derelict_insulator_' + name, [...body, ...ring]);
    blockJson('derelict_insulator_' + name, body, 'va');
    blockJson(`derelict_insulator_${name}_ring`, ring, 'va', { emissive: true });
    for (const [facing, y] of Object.entries(FACE_Y)) {
      multipart.push({ when: { mount: name, facing }, apply: { model: model('derelict_insulator_' + name), ...yrot(y) } });
      multipart.push({ when: { mount: name, facing, lit: 'true' }, apply: { model: model(`derelict_insulator_${name}_ring`), ...yrot(y) } });
    }
    // where a span attaches (model px, facing west: the wall mount's wall east)
    console.log(`insulator tie (${name})`, JSON.stringify(m.tie));
  }
  writeJson('blockstates/derelict_insulator.json', { multipart });
  // the item: the top mount's body fitted to the slot (provisional, waiting for the user: the stack on its collar)
  writeJson('models/item/derelict_insulator.json', blockModel(remap(mounts.top.body, ins.map), 'va', { gui: true }));
  // the span renderer's textures (none of the block models uses the span ones)
  const spanTex = SPAN_TEX.map((k) => ins.map[k] || k);
  for (const k of spanTex) used.add(k);
  console.log('insulator span textures (textures/block/wastes/):', spanTex.join(' '));
  console.log('insulator snapped span (s, y px from its tie, s along its heading):', JSON.stringify(I.SNAP_PTS));
  // what breaking one gives (provisional, waiting for the user: a scrap, as the other city blocks)
  writeData(`${NS}/loot_table/blocks/derelict_insulator.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(SCRAP)] }] });
  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of INSULATOR_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
}
// ================================================================ end of the Derelict Insulator

// ================================================================ the city props (D72)
// From clockwork-wastes-city-props.gen.js (approved preview, D72; screenshots in design/texture-reviews/clockwork-wastes-
// city-props/). Loaded last: it loads the city, insulator and salvage generators, whose textures are all in by now.
//   - the Assembly Clamp: one block, stages 1-5, any of four facings; baked to OBJ as the shells are (stages 4-5 reach
//     33.5 px, past a block model's 32; stage 1's open chest jaws turn 22.5 about y, each baked as a bone); its item a flat
//     icon (ICONS). Cubes a block or more tall (the jig's uprights) are cut at the block lines so each block's part takes
//     its UVs from its own cell, tiling at 16 px a block (the preview stretched them over one texture); the rest keep the
//     preview's UVs.
//   - the Charging Alcove: the new corner shelf in place of the thin ledge (and the toy cart that stood on it); its
//     trinkets (cog, key standing, cart, framed flower) as parts in its middle block, drawn by its 'trinket' state; its
//     dead vines as parts in its middle and top blocks ('vines').
//   - the Brass Plaque: one thin wall block, square or hanging askew from its last bolt.
//   - the mast riser: Derelict Cable on a Create Metal Girder's side ('girder' foot / run / top). The cable block stands in
//     the cell beside the girder and its model reaches into the girder's cell: in the model the girder is south (surface
//     south, y 0), as the preview's riser lies on the girder's north face; turned by the girder's side.
const PROP_IDS = ['assembly_clamp', 'brass_plaque'];
{
  const CP = require('./clockwork-wastes-city-props.gen.js');
  // cubes a block or more tall cut at the block lines (each piece then fits its own cell's UVs); the rest as they are
  const tiled = (cubes) => cubes.flatMap((c) => (c.rot || c.to[1] - c.from[1] < 16 ? [c] : [-16, 0, 16, 32].flatMap((lo) => cut([c], 'y', lo, lo + 16, 0))));
  // ---------- the Assembly Clamp ----------
  const clampVariants = {};
  for (let n = 1; n <= 5; n++) {
    const cubes = tiled(CP.assemblyStage(n));
    zcheckModel('assembly_clamp_' + n, cubes);
    // a rig: the still cubes, and one bone for each cube with its own turn (stage 1's open jaws)
    const turned = cubes.filter((c) => c.rot);
    const rig = { n: 'clamp', p: [8, 0, 8], c: cubes.filter((c) => !c.rot), k: turned.map((c) => ({ n: c.name, p: c.rot[2], c: [{ ...c, rot: null }] })) };
    const r = Object.fromEntries(turned.map((c) => [c.name, ['x', 'y', 'z'].map((a) => (a === c.rot[0] ? c.rot[1] : 0))]));
    if (new Set(turned.map((c) => c.name)).size !== turned.length) throw new Error('assembly_clamp_' + n + ': two turned cubes share a name');
    const quads = posedQuads(rig, { r, lift: [0, 0, 0] });
    const json = writeObj('assembly_clamp_' + n, quads, 'va', 'clockwork-wastes-city-props.gen.js');
    writeJson(`models/block/wastes/assembly_clamp_${n}.json`, json);
    const pts = quads.flatMap((q) => q.pts);
    console.log('assembly clamp bounds', n, JSON.stringify([0, 1, 2].map((i) => [r3(Math.min(...pts.map((p) => p[i]))), r3(Math.max(...pts.map((p) => p[i])))])));
    for (const [facing, y] of Object.entries(HORIZONTAL)) clampVariants[`facing=${facing},stage=${n}`] = { model: model('assembly_clamp_' + n), ...yrot(y) };
  }
  writeJson('blockstates/assembly_clamp.json', { variants: clampVariants });
  writeJson('models/item/assembly_clamp.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/assembly_clamp` } });
  // salvage like the Worker Shell (P16): the wrench takes it apart for 2-3 scrap and a chance of an Electron Tube; anything
  // else breaks it into one scrap
  writeData(`${NS}/loot_table/blocks/assembly_clamp.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(SCRAP)] }] });
  writeData(`${NS}/loot_table/salvage/assembly_clamp.json`, { type: 'minecraft:block', pools: [
    { rolls: 1, entries: [item(SCRAP, { functions: [count(2, 3)] })] },
    { rolls: 1, entries: [item('create:electron_tube')], conditions: [{ condition: 'minecraft:random_chance', chance: 0.25 }] }] });
  writeData(`${NS}/tags/block/salvage.json`, { values: [...SALVAGE_IDS, 'assembly_clamp'].map((id) => `${NS}:${id}`) });

  // ---------- the Charging Alcove: shelf, trinkets, vines ----------
  const base = [...CP.alcoveBase(), ...CP.shelf()], lit = S.alcoveLit();
  threePart('charging_alcove', base, 'y', M.VA_BODY, { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/charging_alcove` } },
    'charging_alcove_contact');
  zcheckModel('charging_alcove (lit)', [...base, ...lit]);
  PARTS.forEach((part, k) => writeJson(`models/block/wastes/charging_alcove_contact_${part}.json`, blockModel(slice(lit, 'y', k * 16), M.VA_BODY, { emissive: true })));
  const TRINKETS = { cog: 'cog', key: 'key', cart: 'cart', flower: 'flower' };
  const vines = CP.alcoveVines();
  zcheckModel('charging_alcove (vines)', [...base, ...lit, ...vines]);
  const alcoveState = JSON.parse(fs.readFileSync(path.join(ASSETS, 'blockstates/charging_alcove.json'), 'utf8'));
  for (const [state, kind] of Object.entries(TRINKETS)) {
    const t = CP.trinket(kind);
    if (!t.length) throw new Error('no trinket ' + kind);
    zcheckModel('charging_alcove (' + kind + ')', [...base, ...lit, ...vines, ...t]);
    if (t.some((c) => c.from[1] < 16 || c.to[1] > 32)) throw new Error('the ' + kind + ' leaves the middle block');
    blockJson('charging_alcove_trinket_' + state, moveY(t, -16), M.VA_BODY);
    for (const [facing, y] of Object.entries(HORIZONTAL))
      alcoveState.multipart.push({ when: { facing, part: 'middle', trinket: state }, apply: { model: model('charging_alcove_trinket_' + state), ...yrot(y) } });
  }
  for (const [k, part] of [[1, 'middle'], [2, 'top']]) {
    blockJson('charging_alcove_vines_' + part, slice(vines, 'y', k * 16), M.VA_BODY);
    for (const [facing, y] of Object.entries(HORIZONTAL))
      alcoveState.multipart.push({ when: { facing, part, vines: 'true' }, apply: { model: model('charging_alcove_vines_' + part), ...yrot(y) } });
  }
  if (slice(vines, 'y', 0).length) throw new Error('the vines reach the bottom block');
  writeJson('blockstates/charging_alcove.json', alcoveState);

  // ---------- the Brass Plaque ----------
  const plaque = CP.plaque(), askew = CP.plaqueAskew();
  zcheckModel('brass_plaque', plaque);
  zcheckModel('brass_plaque (askew)', askew);
  blockJson('brass_plaque', plaque, 'cp_plaque_field');
  blockJson('brass_plaque_askew', askew, 'cp_plaque_field');
  writeJson('blockstates/brass_plaque.json', { variants: horizontalVariants('brass_plaque', { 'askew=false': 'brass_plaque', 'askew=true': 'brass_plaque_askew' }) });
  // its item: the square plaque fitted to the slot (provisional, for the user's word)
  writeJson('models/item/brass_plaque.json', blockModel(plaque, 'cp_plaque_field', { gui: true }));
  writeData(`${NS}/loot_table/blocks/brass_plaque.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(`${NS}:brass_plaque`)],
    conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
  // where each reaches (block px, facing north), for its shapes
  for (const [name, cubes] of [['square', plaque], ['askew', askew]]) {
    const pts = cubes.flatMap((c) => [0, 1, 2, 3, 4, 5, 6, 7].map((k) => {
      const p = [k & 1 ? c.to[0] : c.from[0], k & 2 ? c.to[1] : c.from[1], k & 4 ? c.to[2] : c.from[2]];
      return c.rot ? turnPoint(p, c.rot[0], c.rot[1], c.rot[2]) : p;
    }));
    console.log('brass plaque bounds', name, JSON.stringify([0, 1, 2].map((i) => [r3(Math.min(...pts.map((p) => p[i]))), r3(Math.max(...pts.map((p) => p[i])))])));
  }

  // ---------- the mast riser (Derelict Cable on a girder) ----------
  // the preview's riser up a mast H girders tall, moved from the girder's cell into the cable's (16 px north of it)
  const H = 5, R = CP.riser(H), top = H * 16;
  const body = move(R.body, [0, 0, 16]), line = move(R.line, [0, 0, 16]);
  // the foot: the riser guard (to its lip at 19.75, over the next block's foot) and the strands in its own block, and the
  // ground cable's run from the block's middle into the guard (the preview's last ground run, gc2)
  const footRun = CP.groundRun('foot_run', [8, 8], [8, 17.25]);
  const foot = [...tiled(body.filter((c) => c.name.startsWith('guard_'))), ...cut(body.filter((c) => /^riser_\d$/.test(c.name)), 'y', 0, 16, 0), ...footRun.body];
  const footGlow = [...cut(line, 'y', 0, 16, 0), ...footRun.line];
  // a run: one block of the strands and the clamp that bites the girder in it (the preview's clamp at 38, 6 px up a block)
  const run = cut(body, 'y', 32, 48, 32), runGlow = cut(line, 'y', 32, 48, 32);
  // the top: the block under the insulator, with the pothead and the jumper up to the insulator's tie knob (in the cells
  // above and in the girder's)
  const topCubes = cut(body, 'y', top - 16, top + 32, top - 16), topGlow = cut(line, 'y', top - 16, top + 32, top - 16);
  if (!run.some((c) => /clamp/.test(c.name))) throw new Error('the riser run has no clamp');
  zcheckModel('derelict_cable_riser', [...body, ...line, ...footRun.body, ...footRun.line]);
  const RISER = { foot: [foot, footGlow], run: [run, runGlow], top: [topCubes, topGlow] };
  for (const [part, [cubes, glow]] of Object.entries(RISER)) {
    blockJson('derelict_cable_riser_' + part, cubes, 'cable');
    blockJson(`derelict_cable_riser_${part}_glow`, glow, 'cable', { emissive: true });
  }
  // its surface is the girder's side of it: the model's girder is south (y 0)
  const GIRDER_Y = { south: 0, west: 90, north: 180, east: 270 };
  const opposite = { north: 'south', south: 'north', east: 'west', west: 'east' };
  const cableState = JSON.parse(fs.readFileSync(path.join(ASSETS, 'blockstates/derelict_cable.json'), 'utf8'));
  for (const [s, y] of Object.entries(GIRDER_Y)) {
    for (const part of Object.keys(RISER)) {
      cableState.multipart.push({ when: { girder: part, surface: s }, apply: { model: model('derelict_cable_riser_' + part), ...yrot(y) } });
      cableState.multipart.push({ when: { girder: part, surface: s, lit: 'true' }, apply: { model: model(`derelict_cable_riser_${part}_glow`), ...yrot(y) } });
    }
    // the foot also joins the floor cable beside it: a floor arm toward each (never toward the girder, where its own run
    // goes), and a knot where one meets its run at a corner
    const floorArms = Object.keys(opposite).filter((d) => d !== s);
    for (const d of floorArms) {
      cableState.multipart.push({ when: { girder: 'foot', surface: s, [d]: 'true' }, apply: CABLE_TURN.armAt('derelict_cable_arm', 'down', d) });
      cableState.multipart.push({ when: { girder: 'foot', surface: s, [d]: 'true', lit: 'true' }, apply: CABLE_TURN.armAt('derelict_cable_arm_glow', 'down', d) });
    }
    // the knot (2 x 3 px) lies across the foot's run, as the floor cable's knot lies across the run it was drawn on (east
    // to west in its model): turned a quarter when the run goes north-south
    const corners = floorArms.filter((d) => d !== opposite[s]).map((d) => ({ girder: 'foot', surface: s, [d]: 'true' }));
    cableState.multipart.push({ when: { OR: corners }, apply: { model: model('derelict_cable_knot'), ...(s === 'north' || s === 'south' ? { y: 90 } : {}) } });
  }
  writeJson('blockstates/derelict_cable.json', cableState);
  // the foot's run and the knot across it fight nothing, nor does the knot with any floor arm the foot can take (the girder
  // south in the model: arms north, east and west; the knot only with east or west, where an arm meets the run at a corner)
  const footKnot = quarter(cableKnot(), 'y', 90, [8, 0, 8]);
  zcheckModel('derelict_cable_riser (foot, knot)', [...foot, ...footGlow, ...footKnot]);
  for (const d of ['north', 'east', 'west'])
    zcheckModel('derelict_cable_riser (foot, ' + d + ' arm)', [...foot, ...footGlow, ...CABLE_TURN.armTo(CABLE_TURN.arm, d), ...CABLE_TURN.armTo(CABLE_TURN.armGlow, d), ...(d === 'north' ? [] : footKnot)]);

  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of PROP_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
  console.log(`city props: ${PROP_IDS.join(', ')}; the alcove's ${Object.keys(TRINKETS).length} trinkets and vines; the riser's ${Object.keys(RISER).join('/')}`);
}
// ================================================================ end of the city props

// ================================================================ the building block set (D75)
// From clockwork-wastes-building-blocks.gen.js (approved preview, D75; screenshots in design/texture-reviews/clockwork-
// wastes-building-blocks/). The void-alloy base surface is option C, the ashlar bond: a full-cube plate's side faces show a
// 1 x 1 plate (va_bond_s) or the left or right half of a 2 x 1 plate (va_bond_l, va_bond_r), picked from the block's
// position (the generator's OPTS.C) and kept in its 'bond_ns' and 'bond_ew' states (the north then south face's plate, the
// east then west face's: s, l or r each), which the block sets from its position when it's placed (VoidAlloyBond). Tops
// and bottoms are 1 x 1. Each bonded face is a one-face model of its own, chosen per face by a multipart, so every block
// with the bond shares the same twelve face models (the Plate, the Slab's double, the Scarred Plate's other faces) and the
// Grille's slotted faces have three each. Shaped blocks (stairs, slab, wall) carry the plain plate (va_plain) with uvlock,
// as the preview did. The Band and the Grille also lie on their sides (fallen tops, D75): their whole model turned, its
// textures with it, as the preview's fallen top was.
//   - Void-Alloy Stairs, Slab (double = the bonded Plate), Wall (the post's top va_post_top)
//   - Slotted Grille: 'axis' x, z (upright: the slots look along it), x_lying, z_lying (the slots look along it, running
//     across), up_x, up_z (the slots look up, running along x or z); its dim pane lit while fed
//   - Void-Alloy Band: 'axis' (its ring's axis: y upright, x or z lying), a flush fill where a block touches each side
//     (north..down: the sides round its axis), its seam lit (edge tone) while fed
//   - Clamp Bracket: 'mount' wall (a strap on the face of the block behind it) or saddle (over the top of the block
//     below, jaws across 'facing''s axis); never lit
//   - Scarred Void-Alloy Plate: 'facing' (its scarred face) x 'scar' chipped, cracked, heat; the cracked one's crack
//     glows dim while fed
//   - Stained End Stone Bricks, Cracked, Stairs, Slab, Wall: three weighted variants (50/25/25) of the brick face, the
//     dusty top on up faces
//   - Key-Marked Stone ('facing': the key's face), Broken Makers' Window ('axis' x or z: three weighted variants of the
//     pane), Patina Rib
const BB_IDS = ['void_alloy_stairs', 'void_alloy_slab', 'void_alloy_wall', 'slotted_grille', 'void_alloy_band', 'clamp_bracket',
  'scarred_void_alloy_plate', 'stained_end_stone_bricks', 'cracked_stained_end_stone_bricks', 'stained_end_stone_brick_stairs',
  'stained_end_stone_brick_slab', 'stained_end_stone_brick_wall', 'key_marked_stone', 'broken_makers_window', 'patina_rib'];
{
  // the preview paints its textures into the shared set: none may change a texture another block already uses
  const before = { ...T.OWN };
  const BB = require('./clockwork-wastes-building-blocks.gen.js');
  for (const [k, buf] of Object.entries(before)) if (!buf.equals(T.OWN[k])) throw new Error('the building blocks repaint ' + k);
  const LETTERS = ['s', 'l', 'r'];
  const PAIRS = LETTERS.flatMap((a) => LETTERS.map((b) => a + b));
  // the state and the place in its value of a world side's plate: north and east first, south and west second
  const BOND = { north: ['bond_ns', 0], south: ['bond_ns', 1], east: ['bond_ew', 0], west: ['bond_ew', 1] };
  const pairsWith = (side, letter) => PAIRS.filter((p) => p[BOND[side][1]] === letter).join('|');
  const SIDES = ['north', 'east', 'south', 'west'];
  const bsTurn = (v, x, y) => turnPoint(turnPoint(v, 'x', -x, [0, 0, 0]), 'y', -y, [0, 0, 0]);
  const worldSide = (side, rot) => dirOf(bsTurn(VEC[side], rot.x || 0, rot.y || 0).map((n) => Math.round(n)));
  const rotOf = (rot) => ({ ...(rot.x ? { x: rot.x } : {}), ...(rot.y ? { y: rot.y } : {}) });
  // the turn (x, then y, quarter turns) that takes the model's 'from' sides to 'to' sides, as the blockstate turns it
  const turnTo = (pairs) => {
    for (const x of [0, 90, 180, 270]) for (const y of [0, 90, 180, 270])
      if (pairs.every(([a, b]) => b.includes(worldSide(a, { x, y })))) return { x, y };
    throw new Error('no turn for ' + JSON.stringify(pairs));
  };
  const AO = { ao: true };
  const bbModel = (name, cubes, particle, o = {}) => writeJson(`models/block/wastes/${name}.json`, blockModel(cubes, particle, { ...AO, ...o }));

  // ---------- the bond: one model per model side and plate; the multipart parts that pick them ----------
  // cubes whose 'VA' faces are the bonded surface; written as <name>_<side>_<plate> (only the 'VA' faces on that side) and
  // <name> (everything else). Returns the model sides that have bonded faces.
  function bondModels(name, cubes, particle) {
    const sides = DIRS.filter((d) => cubes.some((c) => faceTex(c, d) === 'VA'));
    const rest = cubes.map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, faceTex(c, d) === 'VA' ? null : faceTex(c, d) || null])) }));
    if (rest.some((c) => DIRS.some((d) => c.faces[d]))) bbModel(name, rest, particle);
    for (const side of sides) for (const p of LETTERS) {
      const only = cubes.map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, d === side && faceTex(c, d) === 'VA' ? 'va_bond_' + p : null])) }));
      bbModel(`${name}_${side}_${p}`, only, particle);
    }
    return sides;
  }
  // the parts that draw a bonded model turned by rot, under 'when': each model side's plate from the world side it faces
  // (up and down always the 1 x 1); 'skip' leaves out a world side (the Scarred Plate's scarred face)
  function bondParts(name, sides, rot = {}, when = {}, skip = null) {
    const out = [];
    for (const side of sides) {
      const ws = worldSide(side, rot);
      const w = skip ? { ...when, facing: SIDES.filter((f) => f !== ws).join('|') } : when;
      if (!BOND[ws]) { out.push({ ...(Object.keys(w).length ? { when: w } : {}), apply: { model: model(`${name}_${side}_s`), ...rotOf(rot) } }); continue; }
      for (const p of LETTERS) out.push({ when: { ...w, [BOND[ws][0]]: pairsWith(ws, p) }, apply: { model: model(`${name}_${side}_${p}`), ...rotOf(rot) } });
    }
    return out;
  }
  const full = (n, tex) => [box(n, [0, 0, 0], [16, 16, 16], tex)];

  // ---------- the Void-Alloy Plate (the decor block, D37): bonded ----------
  const plateSides = bondModels('void_alloy_plate', BB.plate(), 'va_bond_s');
  writeJson('blockstates/void_alloy_plate.json', { multipart: bondParts('void_alloy_plate', plateSides) });
  // its item: a 1 x 1 plate on every face
  bbModel('void_alloy_plate_item', full('plate', 'va_bond_s'), 'va_bond_s');
  writeJson('models/item/void_alloy_plate.json', { parent: model('void_alloy_plate_item') });

  // ---------- vanilla shapes: stairs, slabs, walls (models in vanilla's templates, its states and turns) ----------
  // stairs: block/stairs, inner_stairs, outer_stairs, turned as vanilla's BlockModelGenerators.createStairs turns them
  const YAW = { east: 0, south: 90, west: 180, north: 270 };
  function stairModels(id, tex) {
    for (const [suffix, parent] of [['', 'stairs'], ['_inner', 'inner_stairs'], ['_outer', 'outer_stairs']]) {
      for (const t of Object.values(tex)) used.add(t);
      writeJson(`models/block/wastes/${id}${suffix}.json`, { parent: `minecraft:block/${parent}`, textures: { bottom: ref(tex.bottom), top: ref(tex.top), side: ref(tex.side) } });
    }
  }
  function stairState(variantsOf) {
    const variants = {};
    for (const [facing, f] of Object.entries(YAW)) for (const half of ['bottom', 'top']) for (const shape of ['straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right']) {
      const kind = shape === 'straight' ? '' : shape.startsWith('inner') ? '_inner' : '_outer';
      const left = shape.endsWith('_left'), right = shape.endsWith('_right');
      const y = (f + (half === 'bottom' ? (left ? 270 : 0) : (right ? 90 : 0))) % 360;
      const x = half === 'top' ? 180 : 0;
      const turn = { ...(x ? { x } : {}), ...(y ? { y } : {}), ...(x || y ? { uvlock: true } : {}) };
      variants[`facing=${facing},half=${half},shape=${shape}`] = variantsOf(kind).map((v) => ({ ...v, ...turn }));
    }
    return { variants };
  }
  const one = (v) => (v.length === 1 ? v[0] : v);
  const slabModels = (id, tex) => {
    for (const t of Object.values(tex)) used.add(t);
    writeJson(`models/block/wastes/${id}.json`, { parent: 'minecraft:block/slab', textures: { bottom: ref(tex.bottom), top: ref(tex.top), side: ref(tex.side) } });
    writeJson(`models/block/wastes/${id}_top.json`, { parent: 'minecraft:block/slab_top', textures: { bottom: ref(tex.bottom), top: ref(tex.top), side: ref(tex.side) } });
  };
  // walls: vanilla's side templates; the post is vanilla's template_wall_post with its own top texture
  const wallModels = (id, side, top) => {
    used.add(side); used.add(top);
    const f = (uv, cull) => ({ uv, texture: '#wall', ...(cull ? { cullface: cull } : {}) });
    writeJson(`models/block/wastes/${id}_post.json`, { parent: 'minecraft:block/block', textures: { particle: ref(side), wall: ref(side), top: ref(top) },
      elements: [{ from: [4, 0, 4], to: [12, 16, 12], faces: { down: f([4, 4, 12, 12], 'down'), up: { uv: [4, 4, 12, 12], texture: '#top', cullface: 'up' },
        north: f([4, 0, 12, 16]), south: f([4, 0, 12, 16]), west: f([4, 0, 12, 16]), east: f([4, 0, 12, 16]) } }] });
    writeJson(`models/block/wastes/${id}_side.json`, { parent: 'minecraft:block/template_wall_side', textures: { wall: ref(side) } });
    writeJson(`models/block/wastes/${id}_side_tall.json`, { parent: 'minecraft:block/template_wall_side_tall', textures: { wall: ref(side) } });
  };
  // vanilla's wall multipart (a side runs north in the template; uvlock); 'of(name)' the models (weighted) of a part
  const wallState = (of) => {
    const multipart = [{ when: { up: 'true' }, apply: one(of('post')) }];
    for (const [d, y] of Object.entries(HORIZONTAL)) {
      multipart.push({ when: { [d]: 'low' }, apply: one(of('side').map((m) => ({ ...m, ...(y ? { y } : {}), uvlock: true }))) });
      multipart.push({ when: { [d]: 'tall' }, apply: one(of('side_tall').map((m) => ({ ...m, ...(y ? { y } : {}), uvlock: true }))) });
    }
    return { multipart };
  };

  // the void-alloy shapes: the plain plate on every face (uvlock: each face shows the part of one plate's frame it covers)
  const VP = 'va_plain', vtex = { bottom: VP, top: VP, side: VP };
  stairModels('void_alloy_stairs', vtex);
  writeJson('blockstates/void_alloy_stairs.json', stairState((k) => [{ model: model('void_alloy_stairs' + k) }]));
  writeJson('models/item/void_alloy_stairs.json', { parent: model('void_alloy_stairs') });
  slabModels('void_alloy_slab', vtex);
  writeJson('blockstates/void_alloy_slab.json', { multipart: [
    { when: { type: 'bottom' }, apply: { model: model('void_alloy_slab') } }, { when: { type: 'top' }, apply: { model: model('void_alloy_slab_top') } },
    ...bondParts('void_alloy_plate', plateSides, {}, { type: 'double' })] });
  writeJson('models/item/void_alloy_slab.json', { parent: model('void_alloy_slab') });
  wallModels('void_alloy_wall', VP, 'va_post_top');
  writeJson('blockstates/void_alloy_wall.json', wallState((p) => [{ model: model('void_alloy_wall_' + p) }]));
  writeJson('models/item/void_alloy_wall.json', { parent: 'minecraft:block/wall_inventory', textures: { wall: ref(VP) } });
  // the preview's own shapes, z-checked as the exporter checks every model
  zcheckModel('void_alloy_stairs', BB.vaStairs('east', 'straight'));
  // (a tall side's top meets the post's top on the block's top plane, as vanilla's templates do: both are culled by the
  // block above, the only time a side is tall; so the check runs with low sides)
  zcheckModel('void_alloy_wall', BB.vaWall({ north: 'low', east: 'low' }, true));

  // ---------- the Slotted Grille ----------
  // upright as drawn (slots looking south, running up), and lying: the same grille given an exact quarter turn about z
  // (its slots then run east-west; a blockstate can only turn a model about x, then y)
  const grille = BB.grille('z'), grilleSides = bondModels('slotted_grille', grille, 'va_bond_s');
  const lying = quarter(grille, 'z', 90, [8, 8, 8]), lyingSides = bondModels('slotted_grille_lying', lying, 'va_bond_s');
  bbModel('slotted_grille_lying_glow', quarter(BB.grilleGlow('z'), 'z', 90, [8, 8, 8]), 'va_bond_s', { emissive: true });
  zcheckModel('slotted_grille (lit)', [...grille.map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, faceTex(c, d) === 'VA' ? 'va_bond_l' : faceTex(c, d) || null])) })), ...BB.grilleGlow('z')]);
  bbModel('slotted_grille_glow', BB.grilleGlow('z'), 'va_bond_s', { emissive: true });
  // each orientation: where the model's slots look (south) and run (up) go
  const GRILLE = { z: [['south', 'south|north'], ['up', 'up|down']], x: [['south', 'east|west'], ['up', 'up|down']],
    z_lying: [['south', 'south|north'], ['up', 'east|west']], x_lying: [['south', 'east|west'], ['up', 'north|south']],
    up_x: [['south', 'up|down'], ['up', 'east|west']], up_z: [['south', 'up|down'], ['up', 'north|south']] };
  {
    const multipart = [];
    for (const [axis, pairs] of Object.entries(GRILLE)) {
      // the upright model turned if it can be, else the lying one (its slots run east: model 'up' is 'east' there)
      let name = 'slotted_grille', sides = grilleSides, rot;
      try { rot = turnTo(pairs); } catch (e) {
        name = 'slotted_grille_lying'; sides = lyingSides;
        rot = turnTo(pairs.map(([a, b]) => [a === 'up' ? 'east' : a, b]));
      }
      multipart.push({ when: { axis }, apply: { model: model(name), ...rotOf(rot) } });
      multipart.push(...bondParts(name, sides, rot, { axis }));
      multipart.push({ when: { axis, lit: 'true' }, apply: { model: model(name + '_glow'), ...rotOf(rot) } });
    }
    writeJson('blockstates/slotted_grille.json', { multipart });
    // its item: the grille in a bond of 1 x 1 plates
    bbModel('slotted_grille_item', grille.map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, faceTex(c, d) === 'VA' ? 'va_bond_s' : faceTex(c, d) || null])) })), 'va_bond_s');
    writeJson('models/item/slotted_grille.json', { parent: model('slotted_grille_item') });
  }

  // ---------- the Void-Alloy Band ----------
  {
    const body = BB.band({}), fills = {};
    for (const side of SIDES) fills[side] = BB.band({ [side]: true }).slice(3);
    zcheckModel('void_alloy_band (lit, joined)', [...body, ...Object.values(fills).flat(), ...BB.bandGlow()]);
    bbModel('void_alloy_band', body, 'va_band');
    for (const side of SIDES) bbModel('void_alloy_band_fill_' + side, fills[side], 'va_band');
    bbModel('void_alloy_band_glow', BB.bandGlow(), 'va_band', { emissive: true });
    const AXES = { y: [['up', 'up|down']], x: [['up', 'east|west']], z: [['up', 'north|south']] };
    const multipart = [];
    for (const [axis, pairs] of Object.entries(AXES)) {
      const rot = turnTo(pairs);
      multipart.push({ when: { axis }, apply: { model: model('void_alloy_band'), ...rotOf(rot) } });
      for (const side of SIDES) multipart.push({ when: { axis, [worldSide(side, rot)]: 'true' }, apply: { model: model('void_alloy_band_fill_' + side), ...rotOf(rot) } });
      multipart.push({ when: { axis, lit: 'true' }, apply: { model: model('void_alloy_band_glow'), ...rotOf(rot) } });
    }
    writeJson('blockstates/void_alloy_band.json', { multipart });
    writeJson('models/item/void_alloy_band.json', { parent: model('void_alloy_band') });
  }

  // ---------- the Clamp Bracket (the 'va' facet frame, a small part's texture; never lit) ----------
  {
    const strap = BB.clampWall('north'), saddle = BB.clampSaddle('z');
    zcheckModel('clamp_bracket (wall)', strap);
    zcheckModel('clamp_bracket (saddle)', saddle);
    bbModel('clamp_bracket_wall', strap, 'va');
    bbModel('clamp_bracket_saddle', saddle, 'va');
    const variants = {};
    for (const [facing, y] of Object.entries(HORIZONTAL)) {
      variants[`facing=${facing},mount=wall`] = { model: model('clamp_bracket_wall'), ...yrot(y) };
      // the saddle's jaws hang on the two faces of the block below along its facing's axis (north: its north and south faces)
      variants[`facing=${facing},mount=saddle`] = { model: model('clamp_bracket_saddle'), ...yrot(y % 180) };
    }
    writeJson('blockstates/clamp_bracket.json', { variants });
    writeJson('models/item/clamp_bracket.json', { parent: model('clamp_bracket_wall') });
  }

  // ---------- the Scarred Void-Alloy Plate ----------
  {
    const multipart = [...bondParts('void_alloy_plate', plateSides, {}, {}, true)];
    for (const scar of ['chipped', 'cracked', 'heat']) {
      const cubes = BB.scarred(scar, 'north');
      zcheckModel('scarred_void_alloy_plate ' + scar, cubes.map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, faceTex(c, d) === 'VA' ? 'va_bond_l' : faceTex(c, d)])) })).concat(BB.scarredGlow(scar, 'north')));
      bbModel('scarred_void_alloy_plate_' + scar, [box('scar', [0, 0, 0], [16, 16, 16], { north: faceTex(cubes[0], 'north') })], faceTex(cubes[0], 'north'));
      for (const [facing, y] of Object.entries(HORIZONTAL)) multipart.push({ when: { facing, scar }, apply: { model: model('scarred_void_alloy_plate_' + scar), ...yrot(y) } });
    }
    bbModel('scarred_void_alloy_plate_glow', BB.scarredGlow('cracked', 'north'), 'va_cracked', { emissive: true });
    for (const [facing, y] of Object.entries(HORIZONTAL)) multipart.push({ when: { facing, scar: 'cracked', lit: 'true' }, apply: { model: model('scarred_void_alloy_plate_glow'), ...yrot(y) } });
    writeJson('blockstates/scarred_void_alloy_plate.json', { multipart });
    // its item: the cracked plate (the scar toward the viewer), the rest 1 x 1 plates
    bbModel('scarred_void_alloy_plate_item', [box('plate', [0, 0, 0], [16, 16, 16], { all: 'va_bond_s', north: 'va_cracked' })], 'va_bond_s');
    writeJson('models/item/scarred_void_alloy_plate.json', { parent: model('scarred_void_alloy_plate_item') });
  }

  // ---------- the makers' masonry: Stained End Stone Bricks (three weighted variants, the dusty top) ----------
  {
    const V = [['seb_0', 2], ['seb_1', 1], ['seb_2', 1]];
    const weighted = (name) => V.map(([t, w], i) => ({ model: model(`${name}_${i}`), ...(w > 1 ? { weight: w } : {}) }));
    V.forEach(([t], i) => {
      used.add(t); used.add('seb_top');
      writeJson(`models/block/wastes/stained_end_stone_bricks_${i}.json`, { parent: 'minecraft:block/cube_bottom_top', textures: { top: ref('seb_top'), side: ref(t), bottom: ref(t) } });
      stairModels(`stained_end_stone_brick_stairs_${i}`, { bottom: t, top: 'seb_top', side: t });
      slabModels(`stained_end_stone_brick_slab_${i}`, { bottom: t, top: 'seb_top', side: t });
      wallModels(`stained_end_stone_brick_wall_${i}`, t, 'seb_top');
    });
    writeJson('blockstates/stained_end_stone_bricks.json', { variants: { '': weighted('stained_end_stone_bricks') } });
    writeJson('models/item/stained_end_stone_bricks.json', { parent: model('stained_end_stone_bricks_0') });
    used.add('seb_cracked');
    writeJson('models/block/wastes/cracked_stained_end_stone_bricks.json', { parent: 'minecraft:block/cube_bottom_top', textures: { top: ref('seb_top'), side: ref('seb_cracked'), bottom: ref('seb_cracked') } });
    writeJson('blockstates/cracked_stained_end_stone_bricks.json', { variants: { '': { model: model('cracked_stained_end_stone_bricks') } } });
    writeJson('models/item/cracked_stained_end_stone_bricks.json', { parent: model('cracked_stained_end_stone_bricks') });
    writeJson('blockstates/stained_end_stone_brick_stairs.json', stairState((k) => V.map(([t, w], i) => ({ model: model(`stained_end_stone_brick_stairs_${i}${k}`), ...(w > 1 ? { weight: w } : {}) }))));
    writeJson('models/item/stained_end_stone_brick_stairs.json', { parent: model('stained_end_stone_brick_stairs_0') });
    const slabVariants = (k) => V.map(([t, w], i) => ({ model: model(`stained_end_stone_brick_slab_${i}${k}`), ...(w > 1 ? { weight: w } : {}) }));
    writeJson('blockstates/stained_end_stone_brick_slab.json', { variants: { 'type=bottom': slabVariants(''), 'type=top': slabVariants('_top'),
      'type=double': weighted('stained_end_stone_bricks') } });
    writeJson('models/item/stained_end_stone_brick_slab.json', { parent: model('stained_end_stone_brick_slab_0') });
    writeJson('blockstates/stained_end_stone_brick_wall.json', wallState((p) => V.map(([t, w], i) => ({ model: model(`stained_end_stone_brick_wall_${i}_${p}`), ...(w > 1 ? { weight: w } : {}) }))));
    writeJson('models/item/stained_end_stone_brick_wall.json', { parent: 'minecraft:block/wall_inventory', textures: { wall: ref('seb_0') } });
    zcheckModel('stained_end_stone_brick_wall', BB.stainedWall({ north: 'low', east: 'low' }, true).map((c) => ({ ...c, faces: Object.fromEntries(DIRS.map((d) => [d, faceTex(c, d) === 'SEB' ? 'seb_0' : faceTex(c, d)])) })));
  }

  // ---------- the Key-Marked Stone, the Broken Makers' Window, the Patina Rib ----------
  {
    bbModel('key_marked_stone', BB.keyStone('north'), 'scorched');
    writeJson('blockstates/key_marked_stone.json', { variants: Object.fromEntries(Object.entries(HORIZONTAL).map(([f, y]) => [`facing=${f}`, { model: model('key_marked_stone'), ...yrot(y) }])) });
    writeJson('models/item/key_marked_stone.json', { parent: model('key_marked_stone') });
    // the window: the preview's pane (2 px thick, in the middle of its cell, glass both ways), three panes broken
    // differently; 'axis' z as drawn (the pane across x, seen from north and south), x turned a quarter
    const WIN = ['win_a', 'win_b', 'win_c'];
    WIN.forEach((t) => bbModel('broken_makers_window_' + t.slice(4), [box('window', [0, 0, 7], [16, 16, 9], { north: t, south: t, all: null })], t));
    const winVariants = (y) => WIN.map((t) => ({ model: model('broken_makers_window_' + t.slice(4)), ...yrot(y) }));
    writeJson('blockstates/broken_makers_window.json', { variants: { 'axis=z': winVariants(0), 'axis=x': winVariants(90) } });
    writeJson('models/item/broken_makers_window.json', { parent: model('broken_makers_window_a') });
    cubeAll('patina_rib', 'rib_patina');
    writeJson('blockstates/patina_rib.json', { variants: { '': { model: model('patina_rib') } } });
    writeJson('models/item/patina_rib.json', { parent: model('patina_rib') });
  }

  // ---------- data: each drops itself (decor, D37; a double slab two), the pickaxe tag, vanilla's shape tags ----------
  for (const id of BB_IDS) {
    const slab = id.endsWith('_slab');
    writeData(`${NS}/loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [item(`${NS}:${id}`, slab ? { functions: [
      { function: 'minecraft:set_count', count: 2, conditions: [{ condition: 'minecraft:block_state_property', block: `${NS}:${id}`, properties: { type: 'double' } }] },
      { function: 'minecraft:explosion_decay' }] } : {})], ...(slab ? {} : { conditions: [{ condition: 'minecraft:survives_explosion' }] }) }] });
  }
  const tag = JSON.parse(fs.readFileSync(pick, 'utf8'));
  for (const id of BB_IDS) if (!tag.values.includes(`${NS}:${id}`)) tag.values.push(`${NS}:${id}`);
  fs.writeFileSync(pick, JSON.stringify(tag, null, 2) + '\n');
  // stairs, slabs and walls in vanilla's tags (a wall joins only walls in #walls)
  for (const [t, suffix] of [['stairs', '_stairs'], ['slabs', '_slab'], ['walls', '_wall']]) {
    const file = path.join(DATA, `minecraft/tags/block/${t}.json`);
    const vt = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : { values: [] };
    for (const id of BB_IDS.filter((i) => i.endsWith(suffix))) if (!vt.values.includes(`${NS}:${id}`)) vt.values.push(`${NS}:${id}`);
    writeData(`minecraft/tags/block/${t}.json`, vt);
    const ifile = path.join(DATA, `minecraft/tags/item/${t}.json`);
    const it = fs.existsSync(ifile) ? JSON.parse(fs.readFileSync(ifile, 'utf8')) : { values: [] };
    for (const id of BB_IDS.filter((i) => i.endsWith(suffix))) if (!it.values.includes(`${NS}:${id}`)) it.values.push(`${NS}:${id}`);
    writeData(`minecraft/tags/item/${t}.json`, it);
  }
  // ---------- recipes (D76), shown by JEI as any vanilla crafting, stonecutting or smelting recipe ----------
  // In vanilla's shapes and counts (provisional, listed for the user). Key-Marked Stone and the Scarred Plates have none
  // (city-only), nor do the Plate, Pillar and Seamed Plate themselves (D37: void-alloy stays finite per city; the shapes are
  // cut from salvaged plates).
  const R = (id) => `${NS}:${id}`, mc = (id) => `minecraft:${id}`;
  const cutting = (from, to, count = 1) => writeData(`${NS}/recipe/wastes/${to}_from_${from}_stonecutting.json`,
    { type: 'minecraft:stonecutting', ingredient: { item: R(from) }, result: { id: R(to), count } });
  const shaped = (file, pattern, key, id, count) => writeData(`${NS}/recipe/wastes/${file}.json`,
    { type: 'minecraft:crafting_shaped', category: 'building', pattern, key, result: { id: R(id), count } });
  // the Void-Alloy Plate in a Stonecutter: stairs, wall, band, grille and clamp bracket one a plate, slabs two
  for (const [to, n] of [['void_alloy_stairs', 1], ['void_alloy_slab', 2], ['void_alloy_wall', 1], ['void_alloy_band', 1], ['slotted_grille', 1], ['clamp_bracket', 1]])
    cutting('void_alloy_plate', to, n);
  // Stained End Stone Bricks: eight End Stone Bricks round a brown dye (as stained glass and terracotta); their stairs, slab and
  // wall by vanilla's shapes and in a Stonecutter; cracked by smelting, as vanilla cracks its bricks
  const SEB = 'stained_end_stone_bricks';
  shaped(SEB, ['###', '#D#', '###'], { '#': { item: mc('end_stone_bricks') }, D: { tag: 'c:dyes/brown' } }, SEB, 8);
  shaped('stained_end_stone_brick_stairs', ['#  ', '## ', '###'], { '#': { item: R(SEB) } }, 'stained_end_stone_brick_stairs', 4);
  shaped('stained_end_stone_brick_slab', ['###'], { '#': { item: R(SEB) } }, 'stained_end_stone_brick_slab', 6);
  shaped('stained_end_stone_brick_wall', ['###', '###'], { '#': { item: R(SEB) } }, 'stained_end_stone_brick_wall', 6);
  for (const [to, n] of [['stained_end_stone_brick_stairs', 1], ['stained_end_stone_brick_slab', 2], ['stained_end_stone_brick_wall', 1]]) cutting(SEB, to, n);
  writeData(`${NS}/recipe/wastes/cracked_${SEB}.json`, { type: 'minecraft:smelting', category: 'blocks', ingredient: { item: R(SEB) },
    result: { id: R('cracked_' + SEB) }, experience: 0.1, cookingtime: 200 });
  // the Patina Rib: two copper blocks stacked (as the Quartz Pillar; 2 x 2 is vanilla's cut copper), any weathering, waxed or not
  const COPPER_BLOCKS = ['copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper', 'waxed_copper_block', 'waxed_exposed_copper',
    'waxed_weathered_copper', 'waxed_oxidized_copper'].map((id) => ({ item: mc(id) }));
  shaped('patina_rib', ['#', '#'], { '#': COPPER_BLOCKS }, 'patina_rib', 2);
  // the Broken Makers' Window: iron bars framing glass
  shaped('broken_makers_window', ['BGB', 'BGB'], { B: { item: mc('iron_bars') }, G: { tag: 'c:glass_blocks/colorless' } }, 'broken_makers_window', 4);
  console.log(`building block set: ${BB_IDS.join(', ')}`);
}
// ================================================================ end of the building block set

// one material per texture the OBJ models use (the shells, the vault's item, the vent's hatch), each the model's texture of that name
fs.writeFileSync(path.join(ASSETS, 'models/block/wastes/wastes_shells.mtl'),
  [...SHELL_TEX].sort().map((t) => `newmtl ${t}\nKd 1 1 1\nmap_Kd #${t}\n`).join('\n'));

// ---------- textures: only those the models use ----------
const texDir = path.join(ASSETS, 'textures/block/wastes');
fs.mkdirSync(texDir, { recursive: true });
for (const f of fs.readdirSync(texDir)) if (f.endsWith('.png')) fs.unlinkSync(path.join(texDir, f));
for (const key of used) {
  if (!T.OWN[key]) throw new Error('no texture ' + key);
  fs.writeFileSync(path.join(texDir, key + '.png'), T.OWN[key]);
}
// where each posed shell reaches (block px), for its block's shapes
for (const [k, b] of Object.entries(SHELL_BOUNDS)) console.log('shell bounds', k, JSON.stringify(b));
for (const z of zfights) console.log('z-fight', z);
console.log(`phase 2: ${CITY_IDS.length} blocks, ${PART_NAMES.length} part models: ${PART_NAMES.join(' ')}`);
console.log(`wrote ${ALL_BLOCKS.length + CITY_IDS.length} blocks, ${used.size} textures: ${[...used].sort().join(' ')}`);
console.log(zfights.length ? `${zfights.length} z-fights` : 'no z-fights');
process.exitCode = zfights.length ? 1 : 0;
