// Writes the approved Wellspring and Incinerator into the mod: the Wellspring's 13 block models (wellspring_0 with no
// generation upgrades, wellspring_<source>_<n> with n fitted, the names the blockstate already uses), the capacity
// canister part the renderer draws in each fitted capacity socket, and the Incinerator's cold and lit models (the hob,
// the embers and their bed full-bright when lit). Writes only the wellspring_* and incinerator_* textures they use and
// removes the old ones nothing uses any more. The item models stay parented to wellspring_0 and incinerator.
// Usage: node design/models/wellspring-incinerator.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const G = require('./wellspring-incinerator.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = { 'ws:': 'wellspring_', 'in:': 'incinerator_' };
const SETS = { 'ws:': G.WS, 'in:': G.IN };
const ref = (key) => { for (const [p, f] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${f}${key.slice(3)}`; return key; };
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/^(wellspring|incinerator)_/, '').replace(/[^a-z0-9_]/g, '_');
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
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    // the hob and the fire are full-bright while it's lit
    if (c.glow) el.neoforge_data = { block_light: 15, sky_light: 15 };
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  if (particle.slice(0, 3) in PREFIX) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const write = (name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models/block', name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
  console.log(`wrote models/block/${name}.json (${cubes.length} elements)`);
};

// the Wellspring: the bare tank, and one model per fluid and generation upgrade count
write('wellspring_0', G.wellspring('water', 0), 'ws:band');
for (const src of ['water', 'lava', 'honey', 'chocolate'])
  for (let n = 1; n <= 3; n++) write(`wellspring_${src}_${n}`, G.wellspring(src, n), 'ws:band');
write('wellspring_canister', G.canister(), 'ws:canister');

// the Incinerator
write('incinerator', G.incinerator(false), 'in:side');
write('incinerator_lit', G.incinerator(true), 'in:side');

// textures: what the models use; then the old wellspring_* / incinerator_* textures nothing references now
const dir = path.join(ASSETS, 'textures/block');
const written = new Set();
for (const key of used) {
  const p = key.slice(0, 3), k = key.slice(3), buf = SETS[p].OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  const file = PREFIX[p] + k + '.png';
  fs.writeFileSync(path.join(dir, file), buf);
  written.add(file);
}
console.log(`wrote ${written.size} textures`);
for (const file of fs.readdirSync(dir))
  if (/^(wellspring|incinerator)_.*\.png$/.test(file) && !written.has(file)) { fs.unlinkSync(path.join(dir, file)); console.log('removed textures/block/' + file); }
const base = path.join(ASSETS, 'models/block/wellspring_base.json');
if (fs.existsSync(base)) { fs.unlinkSync(base); console.log('removed models/block/wellspring_base.json'); }
