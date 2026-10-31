// Writes the approved Clockwork Tubes into the mod: the Receiver's body and its ring gear (a renderer part), the two
// Inlets and their shared flap (a renderer part), the Tube's pieces for its multipart blockstate (an arm per open side,
// a capped arm per shut joint, a hub panel per free side, the straight middle), the blockstates, the item models, and
// only the clockwork_tubes_* textures the models use. Usage: node design/models/clockwork-tubes.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const G = require('./clockwork-tubes.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = 'clockwork_tubes_';
const ref = (key) => {
  if (key.startsWith('ct:')) return `${NS}:block/${PREFIX}${key.slice(3)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without a block: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^clockwork_tubes_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // an element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.startsWith('ct:')) used.add(t);
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      if (uv[0] === uv[2] || uv[1] === uv[3]) throw new Error(`${c.name} ${dir}: zero-width UV`);
      faces[dir] = { uv, texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const write = (kind, name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};
const blockstate = (name, json) => {
  fs.writeFileSync(path.join(ASSETS, 'blockstates', name + '.json'), JSON.stringify(json, null, 2) + '\n');
  console.log(`wrote blockstates/${name}.json`);
};

// ---------- Receiver: built with its output up; the cog is the renderer's ----------
write('block', 'clockwork_receiver', G.receiver(), 'ct:field');
write('block', 'clockwork_receiver_cog', G.cog(), 'ct:cog');
write('item', 'clockwork_receiver', [...G.receiver(), ...G.cog()], 'ct:field');
// model output up -> facing (the output side)
const OUT_UP = { up: {}, down: { x: 180 }, north: { x: 90 }, south: { x: 270 }, east: { x: 90, y: 90 }, west: { x: 90, y: 270 } };
blockstate('clockwork_receiver', { variants: Object.fromEntries(Object.entries(OUT_UP).map(([f, r]) => [`facing=${f}`, { model: `${NS}:block/clockwork_receiver`, ...r }])) });

// ---------- Inlets: built with the mouth down; the flap is the renderer's ----------
write('block', 'clockwork_inlet', G.inlet(false), 'ct:field');
write('block', 'brass_clockwork_inlet', G.inlet(true), 'ct:field');
write('block', 'clockwork_inlet_flap', G.flap(), 'ct:steel');
write('item', 'clockwork_inlet', [...G.inlet(false), ...G.flap()], 'ct:field');
write('item', 'brass_clockwork_inlet', [...G.inlet(true), ...G.flap()], 'ct:field');
// model mouth down -> facing (the mouth's side)
const MOUTH_DOWN = { down: {}, up: { x: 180 }, north: { x: 270 }, south: { x: 90 }, east: { x: 270, y: 90 }, west: { x: 270, y: 270 } };
for (const name of ['clockwork_inlet', 'brass_clockwork_inlet']) {
  const variants = {};
  for (const [f, r] of Object.entries(MOUTH_DOWN)) for (const powered of [false, true])
    variants[`facing=${f},powered=${powered}`] = { model: `${NS}:block/${name}`, ...r };
  blockstate(name, { variants });
}

// ---------- Tube: pieces built for the north side (or along Y), turned per side in a multipart blockstate ----------
write('block', 'clockwork_tube_arm', G.arm(2, false), 'ct:collar');
write('block', 'clockwork_tube_arm_capped', G.arm(2, false, true), 'ct:collar');
write('block', 'clockwork_tube_plate', G.hub(['x-', 'x+', 'y-', 'y+', 'z+']), 'ct:hub');
write('block', 'clockwork_tube_spine', G.spine(1), 'ct:collar');
// the item: a straight tube lying east-west
write('item', 'clockwork_tube', [...G.arm(0, false), ...G.arm(0, true), ...G.spine(0)], 'ct:collar');

const SIDES = ['north', 'east', 'south', 'west', 'up', 'down'];
const FROM_NORTH = { north: {}, east: { y: 90 }, south: { y: 180 }, west: { y: 270 }, up: { x: 270 }, down: { x: 90 } };
const AXES = { x: ['east', 'west'], y: ['up', 'down'], z: ['north', 'south'] };
// straight along an axis: both its ends open and every other side empty (a capped side makes it a hub)
const straight = (ax) => Object.fromEntries(SIDES.map((s) => [s, AXES[ax].includes(s) ? 'open' : 'none']));
// not straight along an axis: one of its ends isn't open, or another side isn't empty
const notStraight = (ax) => ({ OR: SIDES.map((s) => ({ [s]: AXES[ax].includes(s) ? 'none|capped' : 'open|capped' })) });
const multipart = [];
for (const [ax, rot] of Object.entries({ x: { x: 90, y: 90 }, y: {}, z: { x: 90 } }))
  multipart.push({ when: straight(ax), apply: { model: `${NS}:block/clockwork_tube_spine`, ...rot } });
for (const side of SIDES) {
  const r = FROM_NORTH[side];
  multipart.push({ when: { [side]: 'open' }, apply: { model: `${NS}:block/clockwork_tube_arm`, ...r } });
  multipart.push({ when: { [side]: 'capped' }, apply: { model: `${NS}:block/clockwork_tube_arm_capped`, ...r } });
  // a hub panel on an empty side, unless the tube runs straight through along one of the other two axes
  const others = Object.keys(AXES).filter((ax) => !AXES[ax].includes(side));
  multipart.push({ when: { AND: [{ [side]: 'none' }, ...others.map(notStraight)] }, apply: { model: `${NS}:block/clockwork_tube_plate`, ...r } });
}
blockstate('clockwork_tube', { multipart });

// ---------- textures ----------
let n = 0;
for (const key of used) {
  const k = key.slice(3), buf = G.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/clockwork_tubes_*.png`);
