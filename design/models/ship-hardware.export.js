// Writes the approved ship hardware set into the mod: the Compact Gyroscope's cage and its renderer parts (the two
// gimbal rings, the flywheel, the caliper jaws, the shaft ends), the Torque Rotor's flange and nacelle, its hub and
// one blade (the renderer draws it three times), the Flight Instruments' desk, the three item models and every texture
// they use. Removes the instruments' button texture, which nothing uses any more.
// Usage: node design/models/ship-hardware.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, C } = require('./process-box.lib.js');
const H = require('./ship-hardware.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = { 'gy:': 'compact_gyroscope_', 'tr:': 'torque_rotor_', 'fi:': 'flight_instruments_' };
const SETS = { 'gy:': H.GY, 'tr:': H.TR, 'fi:': H.FI };
const ref = (key) => { for (const [p, f] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${f}${key.slice(3)}`; return key; };
const used = new Set();

function blockModel(cubes, particle, extra = {}) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/^(compact_gyroscope|torque_rotor|flight_instruments)_/, '').replace(/[^a-z0-9_]/g, '_');
      while ([...vars.values()].includes(n)) n += '_';
      vars.set(r, n);
    }
    return '#' + vars.get(r);
  };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.slice(0, 3) in PREFIX) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: raw.map((n) => Math.round(n * 1000) / 1000), texture: v(ref(t)) };
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  if (particle.slice(0, 3) in PREFIX) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements, ...extra };
}
const write = (kind, name, cubes, particle, extra) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle, extra), null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};

// the Compact Gyroscope
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const stub = (name, y0, y1) => C(name, [6, y0, 6], [10, y1, 10], { all: AXIS, up: AXIS_TOP, down: AXIS_TOP },
  { uv: { north: [6, 0, 10, y1 - y0], south: [6, 0, 10, y1 - y0], east: [6, 0, 10, y1 - y0], west: [6, 0, 10, y1 - y0], up: [6, 6, 10, 10], down: [6, 6, 10, 10] } });
const shaftEnds = () => [stub('shaft_bottom', 0, 2.5), stub('shaft_top', 13.5, 16)];
const jaws = (up) => [...H.jaw(-1, up), ...H.jaw(1, up)];
write('block', 'compact_gyroscope', H.gyroCage(), 'gy:plate');
write('block', 'compact_gyroscope_outer_ring', H.outerRing(), 'gy:brass');
write('block', 'compact_gyroscope_inner_ring', H.innerRing(), 'gy:brass_dark');
write('block', 'compact_gyroscope_flywheel', H.flywheel(), 'gy:steel');
write('block', 'compact_gyroscope_jaws_up', jaws(true), 'gy:dark');
write('block', 'compact_gyroscope_jaws_down', jaws(false), 'gy:dark');
write('block', 'compact_gyroscope_shaft', shaftEnds(), AXIS);
write('item', 'compact_gyroscope', [...H.gyroCage(), ...H.outerRing(), ...H.innerRing(), ...H.flywheel(), ...jaws(true), ...jaws(false), ...shaftEnds()], 'gy:plate',
  { display: { gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] } } });

// the Torque Rotor
write('block', 'torque_rotor', H.rotorBase(), 'tr:plate');
write('block', 'torque_rotor_blades', H.rotorHub(), 'tr:brass');
write('block', 'torque_rotor_blade', H.rotorBlade(), 'tr:blade');
// the item: two of the blades, opposite each other (a static model can't turn one by 120 degrees)
const opposite = H.rotorBlade().map((c) => ({ ...c, name: c.name + '_b', from: [16 - c.to[0], c.from[1], 16 - c.to[2]], to: [16 - c.from[0], c.to[1], 16 - c.from[2]],
  rot: c.rot && [c.rot[0], -c.rot[1], [16 - c.rot[2][0], c.rot[2][1], 16 - c.rot[2][2]]] }));
write('item', 'torque_rotor', [...H.rotorBase(), ...H.rotorHub(), ...H.rotorBlade(), ...opposite], 'tr:plate', { display: {
  gui: { rotation: [30, 225, 0], translation: [0, -1, 0], scale: [0.45, 0.45, 0.45] }, ground: { translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
  fixed: { scale: [0.5, 0.5, 0.5] }, thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
  firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 2, 0], scale: [0.4, 0.4, 0.4] }, firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 2, 0], scale: [0.4, 0.4, 0.4] } } });

// the Flight Instruments (the item model is still just its parent, the block)
write('block', 'flight_instruments', H.instruments(), 'fi:body');

let n = 0;
for (const key of used) {
  const p = key.slice(0, 3), k = key.slice(3), buf = SETS[p].OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX[p] + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures`);
const buttons = path.join(ASSETS, 'textures/block/flight_instruments_buttons.png');
if (fs.existsSync(buttons)) { fs.unlinkSync(buttons); console.log('removed flight_instruments_buttons.png'); }
