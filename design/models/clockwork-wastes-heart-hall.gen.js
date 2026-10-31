// Clockwork Wastes heart hall preview -> design/models/clockwork-wastes-heart-hall.bbmodel (Generic Model preview).
// Spec D55 / bots brief, Primary Forms 4 (the Custodian's tether spine): four heavy cables from the sockets on its back to
// the coils of the four conduit pylons, makers' copper with void-alloy plugs (the Derelict Cable's look: patina'd copper,
// copper showing only where it's worn), sagging catenaries. Shielded, a crawling starlight pulse runs along each toward it;
// kneeling dormant, they lie slack on the floor; when it dies they drop. P32's hall: about 31 x 31 blocks, the Custodian on
// a dais in the middle, a pylon in each corner; D57's wall vents in the walls. Full scale (1 px = 1/16 block).
// Preview only (the game draws the cables with its renderer). Run clockwork-wastes-bots.gen.js first (the Custodian, its
// clips and its socket positions come from there).
// node clockwork-wastes-heart-hall.gen.js
// Clips: waking (slack on the floor, lifting off and tightening as it rises, the pulses starting once it's shielded),
// shielded (idle, the cables swaying, the pulses crawling), kneel (a reset: the cables slacken onto the floor), death (dark,
// then unplugged at 5 s: they drop).
const fs = require('fs');
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, COPPER, PATINA, box } = M;
const R = require('./clockwork-wastes-reactors.gen.js');
const VENT = require('./clockwork-wastes-vent.gen.js');
const BOTS = require('./clockwork-wastes-bots.gen.js');
const { embedBot } = require('./clockwork-wastes-embed.lib.js');

const OUT = path.resolve(__dirname, 'clockwork-wastes-heart-hall.bbmodel');
const GLOW = { shade: false };
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const HALF = 15;            // the floor runs -15..15 blocks (31 across); walls at +-16
const PY = 11;              // pylons at (+-11, +-11)
const DAIS_Y = 16;          // the dais is one block high, 3 x 3
const N = 22;               // segments per cable

// ---------- textures ----------
// the tether: patina'd copper; its underside (the face that drags on the floor) worn back to copper in patches
T.paint('tether', (x, y) => {
  if (y >= 12 && y <= 13) return hash(x, y, 81) > 0.78 ? (hash(x, y, 82) > 0.5 ? COPPER[1] : COPPER[0]) : PATINA[0];
  return hash(x, y, 83) > 0.6 ? PATINA[1] : PATINA[0];
});
T.paint('tether_pulse', () => STAR.edge);
const TETHER = { north: 'tether', south: 'tether', up: 'tether', east: 'tether', west: 'tether', down: 'tether' };
const TETHER_UV = (L) => { const u = Math.min(16, L); return { north: [0, 6, u, 8], south: [0, 6, u, 8], up: [0, 6, u, 8], down: [0, 12, u, 14], east: [0, 6, 2, 8], west: [0, 6, 2, 8] }; };

// ---------- vector helpers (Generic Model space: x, y up, z; the Custodian faces -z) ----------
const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]], sub3 = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]], mul = (a, s) => [a[0] * s, a[1] * s, a[2] * s];
const len = (a) => Math.hypot(a[0], a[1], a[2]), norm = (a) => mul(a, 1 / (len(a) || 1)), lerp3 = (a, b, f) => add(a, mul(sub3(b, a), f));
const flat = (a) => norm([a[0], 0, a[2]]);
const smooth = (u) => (u <= 0 ? 0 : u >= 1 ? 1 : u * u * (3 - 2 * u));
const spring = (t, t0, f = 1.3, d = 3) => { if (t < t0) return 0; const u = t - t0, w = 2 * Math.PI * f / 4; return 1 - Math.exp(-d * u) * (Math.cos(w * u) + (d / w) * Math.sin(w * u)); };
// resample a polyline to n + 1 points evenly by length
function resample(pts, n) {
  const seg = [], acc = [0];
  for (let i = 1; i < pts.length; i++) { seg.push(len(sub3(pts[i], pts[i - 1]))); acc.push(acc[i - 1] + seg[i - 1]); }
  const L = acc[acc.length - 1], out = [];
  for (let k = 0; k <= n; k++) { const s = (k / n) * L; let i = 1; while (i < acc.length - 1 && acc[i] < s) i++; const f = (s - acc[i - 1]) / (seg[i - 1] || 1); out.push(lerp3(pts[i - 1], pts[i], Math.max(0, Math.min(1, f)))); }
  return out;
}
const bezier = (p0, p1, p2, p3, n) => { const out = []; for (let k = 0; k <= n * 4; k++) { const u = k / (n * 4), a = (1 - u) ** 3, b = 3 * u * (1 - u) ** 2, c = 3 * u * u * (1 - u), d = u ** 3; out.push([0, 1, 2].map((i) => a * p0[i] + b * p1[i] + c * p2[i] + d * p3[i])); } return out; };

// ---------- the Custodian's sockets, per clip, from its rig ----------
const CU = BOTS.prepare('custodian');
const ON_DAIS = [0, DAIS_Y, 0];
const SOCKETS = [[-2.5, 30], [2.5, 30], [-2.5, 27], [2.5, 27]]; // socket_1..4: upper left, upper right, lower left, lower right
// where cable k leaves its socket (the plug's back face, 1 px behind) and the way the back faces, at time t of a clip
function socketAt(clipName, k, t) {
  const cl = CU.clips.find((c) => c.name === clipName), [x, y] = SOCKETS[k], name = `socket_${k + 1}_plug`;
  const a = CU.CK.pointAt(cl.T, t, name, [x, y, 4.5]), b = CU.CK.pointAt(cl.T, t, name, [x, y, 6.5]);
  return { p: add(add(a, mul(norm(sub3(b, a)), 1.5)), ON_DAIS), back: norm(sub3(b, a)) };
}
// the pylons: upper sockets to the back pylons, lower sockets to the front ones (front is -z)
const PYLONS = [[-PY, PY], [PY, PY], [-PY, -PY], [PY, -PY]].map(([bx, bz]) => [bx * 16, 0, bz * 16]);
// where each cable plugs into its pylon: the crown plate's corner facing the dais, just under the fins
const COIL = PYLONS.map((p) => { const d = flat(mul(p, -1)); return add([p[0], 40.75, p[2]], mul(d, 4.6)); });

// the cable's shapes, each n + 1 points from the socket end (0) to the pylon end (n)
// taut: a hanging curve (cubic Bezier) that leaves the socket backward and outward and sags between them
function taut(k, s, sway = 0) {
  const C = COIL[k], toS = flat(sub3(s.p, C)), side = [Math.sign(SOCKETS[k][0]), 0, 0];
  const p1 = add(add(s.p, mul(flat(add(s.back, mul(side, 0.45))), 34)), [0, -12 + sway, 0]);
  const p2 = add(add(C, mul(toS, 52)), [0, -24 - sway, 0]);
  return resample(bezier(s.p, p1, p2, C, N), N);
}
// slack (kneeling, or fallen): from the socket back over its robe plates (they flare out behind it when it kneels) and
// down onto the dais behind it, across the dais and over its edge, along the
// floor to the pylon's foot, and up the pylon's corner to its plug. fallen: the socket end lies on the dais
function slack(k, s, fallen = false) {
  const P = PYLONS[k], C = COIL[k];
  const q1 = [s.p[0] + s.back[0] * 22, DAIS_Y + 1, s.p[2] + s.back[2] * 22];
  const dir = flat(sub3(P, q1));
  // where the line from q1 toward the pylon leaves the dais (|x|, |z| <= 24)
  const tEdge = Math.min(...[0, 2].map((i) => (Math.abs(dir[i]) < 1e-6 ? 1e9 : ((dir[i] > 0 ? 24 : -24) - q1[i]) / dir[i])));
  const q2 = add(q1, mul(dir, tEdge)), q3 = add(q2, mul(dir, 2.5)), q4 = [q3[0] + dir[0] * 3, 1, q3[2] + dir[2] * 3];
  const toP = flat(sub3(P, q4)), foot = add([P[0], 1, P[2]], mul(toP, -7.5)), collar = add([P[0], 4.4, P[2]], mul(toP, -5.6)), up = add([P[0], 37, P[2]], mul(toP, -4.7));
  const start = fallen ? [q1[0] - s.back[0] * 9, DAIS_Y + 1, q1[2] - s.back[2] * 9] : s.p;
  const over = [add(s.p, add(mul(s.back, 7), [0, -1.5, 0])), add(s.p, add(mul(s.back, 13), [0, -4.5, 0])), [s.p[0] + s.back[0] * 18, Math.max(DAIS_Y + 3, s.p[1] - 12), s.p[2] + s.back[2] * 18]];
  const pts = fallen ? [start, q1, q2, q3, q4, foot, collar, up, C] : [s.p, ...over, q1, q2, q3, q4, foot, collar, up, C];
  return resample(pts, N);
}

// ---------- the scene ----------
const { cube, group, clip, write } = preview('clockwork_wastes_heart_hall', { 'w:': T });
const outliner = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const gsub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));
const REST = {};
// Generic Model space -> the preview's block-local space (block (0, 0, 0) spans -8..8)
const L = (p) => [p[0] + 8, p[1], p[2] + 8];

// the floor: void-alloy plate (top faces only); the dais in the middle, its centre seamed
{
  const kids = [];
  for (let x = -HALF; x <= HALF; x++) for (let z = -HALF; z <= HALF; z++) kids.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: 'va_plain', all: null }), at(x, 0, z)));
  outliner.push(group('floor', [8, 0, 8], [0, 0, 0], kids));
  const dais = [];
  for (let x = -1; x <= 1; x++) for (let z = -1; z <= 1; z++) dais.push(cube(box(`dais_${x}_${z}`, [0, 0, 0], [16, 16, 16], { all: x === 0 && z === 0 ? 'va_seamed' : 'va_plain', down: null }), at(x, 0, z)));
  outliner.push(group('dais', [8, 0, 8], [0, 0, 0], dais));
}
// the walls: three blocks high, a seamed course in the middle (as the test hall), a doorway in the front (-z) wall; the
// vents are left out of the wall where they go
const VENTS = [[-16, -PY + 2, -90], [-16, PY - 2, -90], [16, -PY + 2, 90], [16, PY - 2, 90], [-PY + 2, 16, 0], [PY - 2, 16, 0], [-PY + 2, -16, 180], [PY - 2, -16, 180]];
{
  const kids = [], isVent = (x, z) => VENTS.some(([vx, vz]) => vx === x && vz === z);
  for (let i = -16; i <= 16; i++) for (const [x, z, face] of [[i, -16, 'south'], [i, 16, 'north'], [-16, i, 'east'], [16, i, 'west']]) {
    if (Math.abs(x) === 16 && Math.abs(z) === 16 && face !== 'north' && face !== 'south') continue;
    for (let y = 0; y < 3; y++) {
      if (y === 0 && isVent(x, z)) continue;
      if (z === -16 && Math.abs(x) <= 1 && y < 3) continue; // the doorway
      kids.push(cube(box(`wall_${x}_${y}_${z}`, [0, 0, 0], [16, 16, 16], { [face]: y === 1 ? 'va_seamed' : 'va_plain', up: y === 2 ? 'va_plain' : null }), at(x, y, z)));
    }
  }
  outliner.push(group('walls', [8, 0, 8], [0, 0, 0], kids));
}
// the vents (live: the heart is awake), each turned to face into the hall
VENTS.forEach(([x, z, yaw], i) => {
  const off = at(x, 0, z), name = `vent_${i}`;
  const kids = [...put(VENT.ventBody(), off), ...put(VENT.contact(false), off), gsub(`${name}_contact_lit`, [8, 5, VENT.DEPTH], off, VENT.contact(true)),
    group(`${name}_hatch`, VENT.HINGE, off, [...put(VENT.hatch(), off), ...put(VENT.hatchSeam('seam_strip_dark'), off),
      gsub(`${name}_seam_lit`, [8, 6, 0], off, VENT.hatchSeam('seam_strip_lit', GLOW).map((c) => ({ ...c, name: c.name + '_g', from: [c.from[0], c.from[1], -0.3], to: [c.to[0], c.to[1], -0.3] })))])];
  outliner.push(group(name, [8, 8, 8], off, kids));
  REST[name] = [0, yaw, 0];
});
// the pylons, intact, each turned so its groove and plug face the dais
PYLONS.forEach((p, i) => {
  const off = [p[0], 0, p[2]], name = `pylon_${i}`, shaft = R.pylonShaft(true), cr = R.crown('intact');
  const fins = [0, 1, 2, 3, 4, 5].map((k) => { const g = gsub(`${name}_fin_${k}`, [8, 44, 8], off, R.fin(k)); REST[`${name}_fin_${k}`] = [0, 60 * k + 30, 0]; return g; });
  const crown = group(`${name}_crown`, [8, 40, 5], off, [...put(cr.cage, off), ...fins, gsub(`${name}_core`, [8, 44, 8], off, [cr.core])]);
  outliner.push(group(name, [8, 0, 8], off, [...put(shaft.body, off), gsub(`${name}_seams`, [8, 0, 8], off, shaft.seams), crown]));
  // the cable's plug on the crown plate's corner: a void-alloy sleeve, turned along the cable
  const C = COIL[i], d = flat(mul(p, -1)), yaw = Math.atan2(-d[2], d[0]) * 180 / Math.PI, c = L(C);
  outliner.push(group(`plug_pylon_${i}`, c, [0, 0, 0], put([box(`plug_p_${i}`, [c[0] - 2, c[1] - 1.75, c[2] - 1.75], [c[0] + 2, c[1] + 1.75, c[2] + 1.75], 'va'), box(`plug_p_${i}_collar`, [c[0] + 2, c[1] - 1.25, c[2] - 1.25], [c[0] + 3, c[1] + 1.25, c[2] + 1.25], 'va')], [0, 0, 0])));
  REST[`plug_pylon_${i}`] = [0, yaw, 0];
});

// ---------- the cables: N segment bones each, a plug at the socket end, two pulse beads ----------
// each segment is built at rest along +x from its start, in two bones: the outer turns it to its heading (y) and carries its
// start (position), the inner tips it up or down (z) and stretches it (x scale), so its worn underside always faces down.
// A clip keys them frame by frame from the cable's shape at that time
const DEG = 180 / Math.PI;
const angles = (d) => { const u = norm(d); return { yaw: Math.atan2(-u[2], u[0]) * DEG, pitch: Math.asin(Math.max(-1, Math.min(1, u[1]))) * DEG }; };
const REST_PTS = [0, 1, 2, 3].map((k) => slack(k, socketAt('waking', k, 0)));
const segRest = REST_PTS.map((pts) => pts.slice(0, N).map((p, j) => { const d = sub3(pts[j + 1], p); return { p, rot: angles(d), len: len(d) }; }));
for (let k = 0; k < 4; k++) {
  const kids = [];
  segRest[k].forEach((s, j) => {
    const o = L(s.p), Ls = s.len + 0.7, name = `cable_${k}_seg_${j}`;
    kids.push(group(name, o, [0, 0, 0], [group(name + '_p', o, [0, 0, 0], put([box(`c${k}_${j}`, [o[0] - 0.35, o[1] - 1, o[2] - 1], [o[0] - 0.35 + Ls, o[1] + 1, o[2] + 1], TETHER, { uv: TETHER_UV(Ls) })], [0, 0, 0]))]));
    REST[name] = [0, s.rot.yaw, 0]; REST[name + '_p'] = [0, 0, s.rot.pitch];
  });
  // the socket end's plug: a void-alloy sleeve over the cable's end
  const sp = L(REST_PTS[k][0]);
  kids.push(group(`cable_${k}_plug`, sp, [0, 0, 0], put([box(`plug_s_${k}`, [sp[0] - 1.75, sp[1] - 1.75, sp[2] - 1.75], [sp[0] + 1.75, sp[1] + 1.75, sp[2] + 1.75], 'va')], [0, 0, 0])));
  // the pulse beads: starlight, a little fatter than the cable, resting at the pylon end
  const cp = L(COIL[k]);
  for (const b of [0, 1]) kids.push(group(`cable_${k}_pulse_${b}`, cp, [0, 0, 0], put([box(`pulse_${k}_${b}`, [cp[0] - 1.6, cp[1] - 1.3, cp[2] - 1.3], [cp[0] + 1.6, cp[1] + 1.3, cp[2] + 1.3], 'tether_pulse', GLOW)], [0, 0, 0])));
  outliner.push(group(`cable_${k}`, [8, 0, 8], [0, 0, 0], kids));
}

// a clip of the cables: shape(k, t) gives cable k's points at time t; pulse(t) says whether the pulses run (they run from the
// pylon to the socket, two to a cable, 3 s each, staggered)
function cableClip(name, length, shape, pulse, steps = {}) {
  const tracks = {}, FPS = 20, n = Math.round(length * FPS);
  for (let k = 0; k < 4; k++) {
    const frames = []; for (let f = 0; f <= n; f++) frames.push(shape(k, f / FPS));
    for (let j = 0; j < N; j++) {
      const rest = segRest[k][j]; let prev = null;
      const rots = frames.map((pts) => { const a = angles(sub3(pts[j + 1], pts[j])), ref = prev || rest.rot; while (a.yaw - ref.yaw > 180) a.yaw -= 360; while (a.yaw - ref.yaw < -180) a.yaw += 360; prev = a; return a; });
      tracks[`cable_${k}_seg_${j}`] = {
        position: (t) => sub3(frames[Math.round(t * FPS)][j], rest.p),
        rotation: (t) => [0, rots[Math.round(t * FPS)].yaw - rest.rot.yaw, 0],
      };
      tracks[`cable_${k}_seg_${j}_p`] = {
        rotation: (t) => [0, 0, rots[Math.round(t * FPS)].pitch - rest.rot.pitch],
        scale: (t) => [(len(sub3(frames[Math.round(t * FPS)][j + 1], frames[Math.round(t * FPS)][j])) + 0.7) / (rest.len + 0.7), 1, 1],
      };
    }
    tracks[`cable_${k}_plug`] = { position: (t) => sub3(frames[Math.round(t * FPS)][0], REST_PTS[k][0]) };
    for (const b of [0, 1]) {
      tracks[`cable_${k}_pulse_${b}`] = { position: (t) => { const pts = frames[Math.round(t * FPS)], u = 1 - (((t + b * 1.5 + k * 0.4) / 3) % 1), x = u * N, i = Math.min(N - 1, Math.floor(x)); return sub3(lerp3(pts[i], pts[i + 1], x - i), COIL[k]); } };
      steps[`cable_${k}_pulse_${b}`] = { scale: pulse };
    }
  }
  clip(name, length, 'hold', tracks, steps);
}
// waking: slack until it rises (2 s), then the cables lift off the floor and tighten with a little bounce; shielded from
// 3.2 s, the pulses start
cableClip('waking', 6.5, (k, t) => { const f = spring(t, 1.9, 1.1, 3.2), s = socketAt('waking', k, Math.min(t, 4.5)); return slack(k, s).map((p, j) => lerp3(p, taut(k, s)[j], f)); },
  [[0, HIDE], [3.2, SHOW]]);
// shielded (in the fight): the cables sway a little, the pulses crawl in toward it
cableClip('shielded', 6, (k, t) => taut(k, socketAt('idle', k, t), 1.6 * Math.sin(t / 6 * 2 * Math.PI + k)), [[0, SHOW]]);
// a reset: back on the dais it kneels; the pulses stop (the pylons re-light behind it), the cables slacken onto the floor
cableClip('kneel', 3.5, (k, t) => { const f = 1 - smooth((t - 0.4) / 1.5), s = socketAt('kneel', k, Math.min(t, 3)); return slack(k, s).map((p, j) => lerp3(p, taut(k, s)[j], f)); },
  [[0, SHOW], [0.3, HIDE]]);
// death: unshielded (the pylons are down), the cables dark and hanging; at 5 s, as the plates fall, they unplug and drop
cableClip('death', 6.5, (k, t) => {
  const s = socketAt('death', k, Math.min(t, 5)), hang = taut(k, s), down = slack(k, s, true), u = Math.max(0, (t - 5) / 0.45);
  const f = u >= 1 ? 1 - 0.05 * Math.exp(-(t - 5.45) * 8) * Math.abs(Math.sin((t - 5.45) * 18)) : u * u;
  return hang.map((p, j) => lerp3(p, down[j], f));
}, [[0, HIDE]]);
write(OUT, outliner);

// rest rotations, then the Custodian on the dais with its four clips
const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
bb.outliner.forEach(walk);
for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
// the cables' keys thinned to 5 a second, and any key on the straight line between its neighbours dropped (Blockbench
// interpolates between them), so the file stays light enough to open
for (const a of bb.animations) for (const an of Object.values(a.animators)) {
  if (!/^cable_/.test(an.name)) continue;
  const keep = an.keyframes.filter((k) => k.interpolation === 'step' || Math.abs(k.time * 5 - Math.round(k.time * 5)) < 1e-6 || Math.abs(k.time - a.length) < 1e-6);
  const out = [];
  for (const ch of new Set(keep.map((k) => k.channel))) {
    const ks = keep.filter((k) => k.channel === ch).sort((p, q) => p.time - q.time), v = (k) => ['x', 'y', 'z'].map((i) => +k.data_points[0][i]);
    ks.forEach((k, i) => { if (k.interpolation === 'step' || i === 0 || i === ks.length - 1) return out.push(k); const p = v(ks[i - 1]), c = v(k), n = v(ks[i + 1]), f = (k.time - ks[i - 1].time) / (ks[i + 1].time - ks[i - 1].time); if (c.some((x, j) => Math.abs(x - (p[j] + (n[j] - p[j]) * f)) > 0.02)) out.push(k); });
  }
  an.keyframes = out;
}
embedBot(bb, path.resolve(__dirname, 'clockwork-wastes-custodian.bbmodel'), { name: 'custodian_place', offset: ON_DAIS },
  { waking: { from: 'waking' }, shielded: { from: 'idle' }, kneel: { from: 'kneel' }, death: { from: 'death' } });
fs.writeFileSync(OUT, JSON.stringify(bb));
console.error(`heart hall: ${bb.elements.length} cubes, ${bb.groups.length} bones, clips ${bb.animations.map((a) => a.name).join(', ')}`);
