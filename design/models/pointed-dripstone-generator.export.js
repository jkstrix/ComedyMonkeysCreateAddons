// Writes the approved Pointed Dripstone Generator into the mod: the block models (plain, Wireless, Wireless powered), the
// item models (the shell with still water, the four seed stalactites and the hammer at rest), the renderer parts (the
// hammer's spindle, and its arm with the head) and every texture of its own the models use. The lamp's lit lenses, glass
// and chamber are the Cobblestone Generators' (already in the mod).
// Usage: node design/models/pointed-dripstone-generator.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, C } = require('./process-box.lib.js');
const G = require('./pointed-dripstone-generator.gen.js');

const NS = 'comedymonkeysaddons';
const ref = (key) => (key.startsWith('pd:') ? `${NS}:block/${key.slice(3)}` : key);
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_');
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
      if (t === 'pd:preview_water') throw new Error(`${c.name}: the preview's water stand-in can't be exported`);
      if (t.startsWith('pd:')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      if (raw[0] === raw[2] || raw[1] === raw[3]) throw new Error(`${c.name} ${dir}: zero-width UV ${raw}`);
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
  if (particle.startsWith('pd:')) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const write = (kind, name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
};

// the item: still water on the ceiling (the Cobblestone Generators' item water), the four seeds, the hammer at rest
// beside the first stalactite (the arm turned 45 degrees, the nearest a model allows)
const ITEM_WATER = `${NS}:block/cobble_generator_item_water`;
function itemInterior() {
  const out = [C('water', [2, G.WATER.y0, 2], [14, G.WATER.y1, 14], { north: ITEM_WATER, up: ITEM_WATER, all: null })];
  for (const s of G.STALACTITES) out.push(...G.seed(s));
  out.push(...G.spindle());
  const pivot = [G.SPIN.x, 6, G.SPIN.z];
  out.push(...G.arm().map((c) => ({ ...c, rot: ['y', 45, pivot] })));
  return out;
}

const side = `pd:${G.N}_side`;
for (const [id, wireless] of [[G.N, false], [`wireless_${G.N}`, true]]) {
  write('block', id, G.shell(wireless), side);
  if (wireless) write('block', id + '_powered', G.shell('powered'), side);
  write('item', id, [...G.shell(wireless), ...itemInterior()], side);
}
// the parts the renderer draws (the hammer's spindle; its arm and head, which swing about the spindle)
write('block', `${G.N}_spindle`, G.spindle(), `pd:${G.N}_spindle`);
write('block', `${G.N}_arm`, G.arm(), `pd:${G.N}_hammer`);
console.log('wrote 5 block models, 2 item models and 2 part models');

let tex = 0;
for (const key of used) {
  const k = key.slice(3);
  const buf = G.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', k + '.png'), buf);
  tex++;
}
console.log(`wrote ${tex} textures: ${[...used].map((k) => k.slice(3)).join(', ')}`);
