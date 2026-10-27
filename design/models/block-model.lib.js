// A part list -> Java block model JSON, for the exporters (the family's: no AO; cutout unless said otherwise). `ref` turns a
// generator texture key ('cage:bars') into the mod's texture name.
const { fitUv, DIRS } = require('./process-box.lib.js');

function blockModel(ref, cubes, particle, renderType = 'minecraft:cutout', parent = 'block/block') {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      faces[dir] = { uv, texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const el = { name: c.name, from: c.from, to: c.to };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: c.rot[2], axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  const model = { parent, render_type: renderType, ambientocclusion: false, textures, elements };
  if (!parent) delete model.parent;
  return model;
}
module.exports = { blockModel };
