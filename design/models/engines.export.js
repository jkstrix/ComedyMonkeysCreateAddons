// Writes the approved Magma Engine and Supercharged Magma Engine into the mod: both engines' state models, the
// renderer parts (flywheel, pistons, the supercharger's pulley), the item models (with the moving parts at rest and the
// shaft), and only the magma_engine_* and supercharged_magma_engine_* textures the models use.
// Usage: node design/models/engines.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const E = require('./engines.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = { 'e:': 'magma_engine_', 's:': 'supercharged_magma_engine_' };
const SETS = { 'e:': E.ENG, 's:': E.SUP };
const ref = (key) => {
  for (const [p, file] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${file}${key.slice(p.length)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without an engine: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^(supercharged_)?magma_engine_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // An element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (/^[es]:/.test(t)) used.add(t);
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
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
    // the firebox and the exhaust mouths are full-bright while it burns
    if (c.glow) el.neoforge_data = { block_light: 15, sky_light: 15 };
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', textures, elements };
}
const write = (kind, name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};

for (const sup of [false, true]) {
  const id = sup ? 'supercharged_magma_engine' : 'magma_engine', p = sup ? 's:' : 'e:';
  write('block', id, E.engine('off', sup), p + 'plate');
  write('block', id + '_lit', E.engine('lit', sup), p + 'plate');
  write('block', id + '_flywheel', E.flywheel(sup), p + 'tread');
  write('block', id + '_piston_left', E.piston('left', sup), p + 'rod');
  write('block', id + '_piston_right', E.piston('right', sup), p + 'rod');
  if (sup) write('block', id + '_blower_pulley', E.blowerPulley(), p + 'pulley');
  write('item', id, [...E.engine('off', sup), ...E.flywheel(sup), ...E.piston('left', sup), ...E.piston('right', sup),
    ...(sup ? E.blowerPulley() : []), ...E.shaftStub()], p + 'plate');
}

let n = 0;
for (const key of used) {
  const p = key.slice(0, 2), k = key.slice(2), buf = SETS[p].OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX[p] + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/{magma_engine,supercharged_magma_engine}_*.png`);
