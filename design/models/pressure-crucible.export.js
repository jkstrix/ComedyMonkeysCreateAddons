// Writes the approved Pressure Crucible into the mod: the press frame (block model), the renderer parts (the screw with
// its big gear, the pinion, the platen, the gauge needle, the shaft stub and the three heat glows), the item model (the
// moving parts at rest and the shaft) and only the pressure_crucible_* textures the models use.
// Usage: node design/models/pressure-crucible.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const X = require('./pressure-crucible.gen.js');

const NS = 'comedymonkeysaddons';
const ref = (key) => {
  if (key.startsWith('c:')) return `${NS}:block/pressure_crucible_${key.slice(2)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without a prefix: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^pressure_crucible_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // An element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.startsWith('c:')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      // clamping a UV that runs off the texture collapses the face to a sliver of its neighbours in the atlas
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      const uv = raw.map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
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
    // the heat glows are full-bright once the burner is lit
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
// the platen is modelled at y 0; the item shows it at the top of its travel
const lift = (cubes, dy) => cubes.map((c) => ({ ...c, from: [c.from[0], c.from[1] + dy, c.from[2]], to: [c.to[0], c.to[1] + dy, c.to[2]],
  rot: c.rot && [c.rot[0], c.rot[1], [c.rot[2][0], c.rot[2][1] + dy, c.rot[2][2]]] }));

write('block', 'pressure_crucible', X.press(), 'c:plate');
write('block', 'pressure_crucible_screw', X.screw(), 'c:steel');
write('block', 'pressure_crucible_pinion', X.pinion(), 'c:steel');
write('block', 'pressure_crucible_ram', X.ram(), 'c:plate');
write('block', 'pressure_crucible_needle', X.needle(), 'c:needle');
write('block', 'pressure_crucible_shaft', X.shaftStub(), 'create:block/axis');
for (const h of ['smouldering', 'kindled', 'seething']) write('block', 'pressure_crucible_heat_' + h, X.heat(h), 'c:heat_' + h);
// the needle can't be turned to its resting angle in a static model, so the item's dial goes without it
write('item', 'pressure_crucible', [...X.press(), ...X.screw(), ...X.pinion(), ...lift(X.ram(), X.PLATE_TOP), ...X.shaftStub()], 'c:plate');

let n = 0;
for (const key of used) {
  if (!key.startsWith('c:')) continue;
  const k = key.slice(2), buf = X.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'pressure_crucible_' + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/pressure_crucible_*.png`);
