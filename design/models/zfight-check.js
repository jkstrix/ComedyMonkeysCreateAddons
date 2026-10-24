// Finds faces that will z-fight: two faces pointing the same way, closer than MIN px, overlapping by more than a sliver.
// Usage: node zfight-check.js            (checks the Geothermal Turbine's machine and every part drawn with it)
// Rotated elements are skipped (their faces aren't axis-aligned); keep overlays on them well apart by hand.
const MIN = 0.2;
const DIRS = { north: [2, 'from', -1], south: [2, 'to', 1], west: [0, 'from', -1], east: [0, 'to', 1], down: [1, 'from', -1], up: [1, 'to', 1] };

function faces(list, part) {
  const out = [];
  for (const c of list) {
    if (c.rot && c.rot[1]) continue;
    for (const [dir, [ax, end]] of Object.entries(DIRS)) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const others = [0, 1, 2].filter((a) => a !== ax);
      out.push({ part, name: c.name, dir, plane: c[end][ax], rect: others.map((a) => [Math.min(c.from[a], c.to[a]), Math.max(c.from[a], c.to[a])]) });
    }
  }
  return out;
}
function overlap(a, b) {
  const w = Math.min(a.rect[0][1], b.rect[0][1]) - Math.max(a.rect[0][0], b.rect[0][0]);
  const h = Math.min(a.rect[1][1], b.rect[1][1]) - Math.max(a.rect[1][0], b.rect[1][0]);
  return w > 0.05 && h > 0.05 ? w * h : 0;
}
function check(parts) {
  const all = Object.entries(parts).flatMap(([part, list]) => faces(list, part));
  const problems = [];
  for (let i = 0; i < all.length; i++) for (let j = i + 1; j < all.length; j++) {
    const a = all[i], b = all[j];
    if (a.dir !== b.dir || Math.abs(a.plane - b.plane) >= MIN) continue;
    const area = overlap(a, b);
    if (area > 0.02) problems.push(`${a.part}:${a.name} ${a.dir} vs ${b.part}:${b.name} (${Math.abs(a.plane - b.plane).toFixed(2)} px apart, ${area.toFixed(1)} px² overlap)`);
  }
  return problems;
}

if (require.main === module) {
  const G = require('./geothermal-turbine.gen.js');
  const problems = check({ machine: G.machine(), glow: G.glow(), lamp_output: G.lamp('output', 'lamp_cyan'), lamp_stall: G.lamp('stall', 'lamp_red'),
    needle_energy: G.needleEnergy(), needle_steam: G.needleSteam(), gauge_lava: G.gaugeFill('lava', 'lava'), gauge_water: G.gaugeFill('water', 'water') });
  for (const p of problems) console.log(p);
  console.log(problems.length ? `${problems.length} z-fights` : 'no z-fights');
  process.exitCode = problems.length ? 1 : 0;
}
module.exports = { check };
