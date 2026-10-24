// Writes the approved Upgraded Chest and Upgraded Barrel into the mod: for every tier and chest type the open body
// (`upgraded_chest_<tier>_<type>`), the closed chest (`_closed`) and the swinging lid (`_lid`), each with its own
// elements now that the fittings differ per tier (the shared `upgraded_chest_<type>_{body,closed,lid}` parents go), and
// every upgraded_chest_* and upgraded_barrel_* texture. The barrel models don't change.
// Usage: node design/models/upgraded-chest.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS } = require('./process-box.lib.js');
const G = require('./upgraded-chest.gen.js');

const NS = 'comedymonkeysaddons';
const ref = (key) => {
  if (key.startsWith('ch:')) return `${NS}:block/upgraded_chest_${key.slice(3)}`;
  throw new Error('texture key without a prefix: ' + key);
};
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*\/upgraded_chest_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: raw.map((n) => Math.round(n * 1000) / 1000), texture: v(ref(t)) };
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return { name: c.name, from: r3(c.from), to: r3(c.to), faces };
  });
  used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', textures, elements };
}
const write = (name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models/block', name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
};

let n = 0;
for (const t of G.TIERS) for (const type of ['single', 'left', 'right']) {
  const id = `upgraded_chest_${t}_${type}`, side = `ch:${t}_side`;
  write(id, G.body(t, type), side);
  write(id + '_closed', [...G.body(t, type), ...G.lid(t, type)], side);
  write(id + '_lid', G.lid(t, type), side);
  n += 3;
}
console.log(`wrote ${n} chest models`);
// the shared parents the old per-tier models pointed at
for (const type of ['single', 'left', 'right']) for (const part of ['body', 'closed', 'lid']) {
  const f = path.join(ASSETS, 'models/block', `upgraded_chest_${type}_${part}.json`);
  if (fs.existsSync(f)) { fs.unlinkSync(f); console.log('removed', path.basename(f)); }
}

let tex = 0;
for (const key of used) {
  const k = key.slice(3), buf = G.CH.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'upgraded_chest_' + k + '.png'), buf);
  tex++;
}
for (const [k, buf] of Object.entries(G.BR.OWN)) { fs.writeFileSync(path.join(ASSETS, 'textures/block', 'upgraded_barrel_' + k + '.png'), buf); tex++; }
console.log(`wrote ${tex} textures`);
