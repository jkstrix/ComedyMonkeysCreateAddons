// Writes the approved Lava Generator and Electric Motor into the mod: both blocks' state models, the generator's rotor,
// the item models (with the wheel and the shaft, which the renderers draw in the world), and only the
// lava_generator_* and electric_motor_* textures the models use. Usage: node design/models/electric.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const E = require('./electric.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = { 'gen:': 'lava_generator_', 'mot:': 'electric_motor_' };
const SETS = { 'gen:': E.GEN, 'mot:': E.MOT };
const ref = (key) => {
  for (const [p, file] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${file}${key.slice(p.length)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without a block: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^(lava_generator|electric_motor)_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // An element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.includes(':') && /^(gen|mot):/.test(t)) used.add(t);
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      faces[dir] = { uv, texture: v(ref(t)) };
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    // lamps and the burning lava are full-bright
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

write('block', 'lava_generator', E.generator('empty'), 'gen:plate');
write('block', 'lava_generator_full', E.generator('full'), 'gen:plate');
write('block', 'lava_generator_lit', E.generator('lit'), 'gen:plate');
write('block', 'lava_generator_rotor', E.rotor(), 'gen:blade');
write('item', 'lava_generator', [...E.generator('full'), ...E.rotor()], 'gen:plate');
write('block', 'electric_motor', E.motor(false), 'mot:plate');
write('block', 'electric_motor_powered', E.motor(true), 'mot:plate');
write('item', 'electric_motor', [...E.motor(false), ...E.shaftStub()], 'mot:plate');

let n = 0;
for (const key of used) {
  const p = key.slice(0, 4), k = key.slice(4), buf = SETS[p].OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX[p] + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/{lava_generator,electric_motor}_*.png`);
