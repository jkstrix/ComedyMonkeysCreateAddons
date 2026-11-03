// Checks the Winding Station's boom against the jetpack hung on it, through the station_hang motion: the reach
// (inner tube, drop link) and the socket, slid out and swung about the boom pivot, against the hung jetpack's frame,
// its stowed wing stack and the sweep of its turning key. Boxes are compared as the bounds of their turned corners,
// which errs on the safe side. Usage: node clockwork-jetpack.collide.js
const G = require('./clockwork-jetpack.gen.js');
const K = 'cj:';
const settle = (u, a = 30) => (u <= 0 ? 0 : 1 - (1 + a * u) * Math.exp(-a * u));
// the clip's timing (windingClip)
const ext = (t) => settle(t - 0.1, 25) * (1 - settle(t - 5.45, 25));
const eng = (t) => settle(t - 0.4, 20) * (1 - settle(t - 5.0, 20));
const PIVOT = [8, 26, 14];

// the hung jetpack in station space: turned 180 about y round (8, 8.25), lifted 8
const hang = (c) => {
  const xs = [16 - c.from[0], 16 - c.to[0]], zs = [16.5 - c.from[2], 16.5 - c.to[2]];
  return { name: c.name, min: [Math.min(...xs), c.from[1] + 8, Math.min(...zs)], max: [Math.max(...xs), c.to[1] + 8, Math.max(...zs)] };
};
const fixed = [
  ...G.frame(K).map(hang),
  { name: 'wing_stack', min: [4, 13, 11.4 - 3], max: [12, 23.5, 11.4] },
  { name: 'key_sweep', min: [4.2, 16.2, 2.5], max: [11.8, 23.8, 4] },
];
const moving = [...G.stationReach(K), ...G.stationSocket(K)];
function posed(c, t, reach) {
  const dz = reach * ext(t), a = ((G.BOOM_PARK * (1 - eng(t))) * Math.PI) / 180;
  const pts = [];
  for (const x of [c.from[0], c.to[0]]) for (const y of [c.from[1], c.to[1]]) for (const z of [c.from[2] + dz, c.to[2] + dz]) {
    const py = y - PIVOT[1], pz = z - PIVOT[2];
    pts.push([x, PIVOT[1] + py * Math.cos(a) - pz * Math.sin(a), PIVOT[2] + py * Math.sin(a) + pz * Math.cos(a)]);
  }
  return { min: [0, 1, 2].map((i) => Math.min(...pts.map((p) => p[i]))), max: [0, 1, 2].map((i) => Math.max(...pts.map((p) => p[i]))) };
}
const EPS = 0.01;
const hits = new Map();
for (let t = 0; t <= 6; t += 0.01) for (const c of moving) {
  const b = posed(c, t, G.REACH_HUNG);
  for (const f of fixed) {
    if ([0, 1, 2].every((i) => b.min[i] < f.max[i] - EPS && b.max[i] > f.min[i] + EPS)) {
      const k = c.name + ' x ' + f.name;
      if (!hits.has(k)) hits.set(k, [t, t]); else hits.get(k)[1] = t;
    }
  }
}
for (const [k, [a, b]] of hits) console.log(`${k}: ${a.toFixed(2)}-${b.toFixed(2)} s`);
console.log(hits.size ? `${hits.size} clips` : 'no clipping');
process.exitCode = hits.size ? 1 : 0;
