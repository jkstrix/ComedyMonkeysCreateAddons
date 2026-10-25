// Writes the approved Cobble Generators into the mod: the 14 block models and the 7 wireless `_powered` ones, the 14
// item models (the shell with a still interior and the drill raised), the renderer parts (the drill, the brass drill,
// its rod, the lamp's three lit lenses) and every texture the models use.
// Usage: node design/models/cobble-generator.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, C } = require('./process-box.lib.js');
const G = require('./cobble-generator.gen.js');

const NS = 'comedymonkeysaddons';
const ref = (key) => (key.startsWith('cg:') ? `${NS}:block/${key.slice(3)}` : key);
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
      if (t.startsWith('cg:')) used.add(t);
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
  if (particle.startsWith('cg:')) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const write = (kind, name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
};
const lift = (cubes, dy) => cubes.map((c) => ({ ...c, from: [c.from[0], c.from[1] + dy, c.from[2]], to: [c.to[0], c.to[1] + dy, c.to[2]],
  rot: c.rot && [c.rot[0], c.rot[1], [c.rot[2][0], c.rot[2][1] + dy, c.rot[2][2]]] }));

// every registered id: tier, product, wireless (CobbleGeneratorBlock ids, ModBlocks.java:74-131)
const IDS = [];
for (const p of ['cobble', 'basalt', 'blackstone']) for (const tier of ['andesite', 'brass']) {
  IDS.push({ id: `${tier}_${p}_generator`, tier, p, wireless: false }, { id: `wireless_${tier}_${p}_generator`, tier, p, wireless: true });
}
IDS.push({ id: 'tuff_generator', tier: 'andesite', p: 'tuff', wireless: false }, { id: 'wireless_tuff_generator', tier: 'andesite', p: 'tuff', wireless: true });

// the item: a still interior (Minecraft's lava, the item water, blue ice), a 2 px block forming, the drill raised
function itemInterior(p, brass) {
  const out = [C('lava_pool', [2, 2, 2], [6, 3, 13], { up: 'minecraft:block/lava_still', north: 'minecraft:block/lava_still', east: 'minecraft:block/lava_still', all: null })];
  if (G.ICE[p]) out.push(C('ice', [10, 2, 4], [14, 6, 9], { all: 'minecraft:block/blue_ice', down: null }));
  else out.push(C('water_pool', [10, 2, 2], [14, 3, 13], { up: 'cg:cobble_generator_item_water', north: 'cg:cobble_generator_item_water', west: 'cg:cobble_generator_item_water', all: null }));
  if (brass) for (const [x, z] of G.BATCH) out.push(C(`block_${x}_${z}`, [x, 2, z], [x + 2.25, 3.5, z + 2.25], { all: G.SPRITE[p], down: null }));
  else out.push(C('block', [6.5, 2, 5], [9.5, 4, 8], { all: G.SPRITE[p], down: null }));
  out.push(...lift(G.drill(brass), 8.75));
  return out;
}
let n = 0;
for (const g of IDS) {
  const side = `cg:${G.id(g.tier, g.p)}_side`;
  write('block', g.id, G.shell(g.tier, g.p, g.wireless), side);
  if (g.wireless) write('block', g.id + '_powered', G.shell(g.tier, g.p, 'powered'), side);
  write('item', g.id, [...G.shell(g.tier, g.p, g.wireless), ...itemInterior(g.p, g.tier === 'brass')], side);
  n += g.wireless ? 3 : 2;
}
// the parts the renderer draws
write('block', 'cobble_generator_drill', G.drill(false), 'cg:cobble_generator_drill');
write('block', 'cobble_generator_drill_brass', G.drill(true), 'cg:cobble_generator_drill');
write('block', 'cobble_generator_drill_rod', G.rod(), 'cg:cobble_generator_steel_light');
for (const s of ['running', 'full', 'off']) write('block', `cobble_generator_lamp_${s}`, G.lampLit(s), `cg:cobble_generator_lamp_${s}`);
console.log(`wrote ${n} generator models and 6 part models`);

let tex = 0;
for (const key of used) {
  const k = key.slice(3);
  if (k === 'cobble_generator_item_water') continue; // kept as it is
  const buf = G.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', k + '.png'), buf);
  tex++;
}
console.log(`wrote ${tex} textures`);
module.exports = { IDS };
