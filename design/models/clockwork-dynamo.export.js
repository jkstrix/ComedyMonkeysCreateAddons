// Writes the approved Clockwork Dynamo into the mod: its two state models (lamp dark / lit), the renderer's parts (the
// aerial and the commutator), the item model (with the aerial up and the shaft, which the renderer draws in the world),
// and only the clockwork_dynamo_* textures the models use. Usage: node design/models/clockwork-dynamo.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const D = require('./clockwork-dynamo.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = 'clockwork_dynamo_';
const ref = (key) => {
  if (key.startsWith('dyn:')) return `${NS}:block/${PREFIX}${key.slice(4)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without a block: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^clockwork_dynamo_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // an element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.startsWith('dyn:')) used.add(t);
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      if (uv[0] === uv[2] || uv[1] === uv[3]) throw new Error(`${c.name} ${dir}: zero-width UV`);
      faces[dir] = { uv, texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    // the lamp is full-bright
    if (c.glow) el.neoforge_data = { block_light: 15, sky_light: 15 };
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

write('block', 'clockwork_dynamo', D.block(false), 'dyn:steel');
write('block', 'clockwork_dynamo_working', D.block(true), 'dyn:steel');
write('block', 'clockwork_dynamo_aerial', D.aerial(), 'dyn:copper');
write('block', 'clockwork_dynamo_commutator', D.commutator(), 'dyn:copper');
write('item', 'clockwork_dynamo', [...D.block(false), ...D.aerial(), ...D.commutator(), ...D.shaftStub()], 'dyn:steel');

// the blockstates: one model per state, turned for all six facings as the Electric Motor's are
const variants = {};
const turn = { north: {}, east: { y: 90 }, south: { y: 180 }, west: { y: 270 }, up: { x: 270 }, down: { x: 90 } };
for (const [facing, rot] of Object.entries(turn))
  for (const working of [false, true])
    variants[`facing=${facing},working=${working}`] = { model: `${NS}:block/clockwork_dynamo${working ? '_working' : ''}`, ...rot };
fs.writeFileSync(path.join(ASSETS, 'blockstates', 'clockwork_dynamo.json'), JSON.stringify({ variants }, null, 2) + '\n');
console.log('wrote blockstates/clockwork_dynamo.json');

let n = 0;
for (const key of used) {
  const k = key.slice(4), buf = D.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/clockwork_dynamo_*.png`);
