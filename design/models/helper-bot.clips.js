// Helper Bot preview clips and checks (run through helper-bot.gen.js). Motion helpers are Trade Bot's
// (trade-bot.gen.js): springStep, keyedTracks, the per-bone chain delays, FK; plus a fast arm IK for the levers.
const fs = require('fs');
const { DYES, dyeShades, P, paintCube, png, bot, groupByName, elements, groups, outliner, pngBuf, uuid, TEX, OUT, PROPS, PROP_DROP, LEVERS, ARGS } = require('./helper-bot.gen.js');

// ---------- animation helpers ----------
const FPS = 20;
const r2 = (v) => Math.round(v * 1000) / 1000;
const smooth = (t) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
const seg = (t, a, b) => smooth((t - a) / (b - a));
const lin = (t, a, b) => (t <= a ? 0 : t >= b ? 1 : (t - a) / (b - a));
const PI = Math.PI, sin = Math.sin, W2 = 2 * PI;
const springStep = (t, t0, freq = 6, damp = 4) => { if (t < t0) return 0; const u = t - t0; return 1 - Math.exp(-damp * u) * (Math.cos(2 * PI * freq * u / 4) + (damp / (2 * PI * freq / 4)) * Math.sin(2 * PI * freq * u / 4)); };
const env = (t, a, b, c, d) => seg(t, a, b) * (1 - seg(t, c, d));
const hopArc = (t, a, b) => (t > a && t < b ? Math.sin(PI * lin(t, a, b)) : 0);
const osc = (t, a, b, hz, ph = 0) => Math.sin(W2 * hz * t + ph) * env(t, a, a + 0.2, b - 0.2, b);
const V = (x = 0, y = 0, z = 0) => [x, y, z];
const addV = (...vs) => vs.reduce((a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]], [0, 0, 0]);
const C3 = (x, y, z) => () => V(x, y, z);
const Z = V();
const ONE = V(1, 1, 1), NONE = V(0, 0, 0);

// clips: which preview props each one shows (all others are scaled to 0)
const CLIP_PROPS = {};
const DRIVING = ['drive_ready', 'throttle_up', 'cruise', 'brake', 'station_wait', 'depart', 'honk', 'no_path', 'manual_folded', 'paused_point', 'crash', 'sulk', 'no_schedule', 'schedule_given', 'schedule_taken', 'dust_controls', 'pose_sheet'];
for (const c of DRIVING.filter((c) => c !== 'pose_sheet')) CLIP_PROPS[c] = ['prop_controls'];
for (const c of ['duck_down', 'duck_up', 'idle_ducked']) CLIP_PROPS[c] = ['prop_ceiling'];
for (const c of ['shop_idle', 'shop_greet', 'shop_order_nod']) CLIP_PROPS[c] = ['prop_ticker'];
// parts that are hidden unless a clip shows them
const HIDDEN = ['cap_train', 'cap_logistics', 'schedule_card', 'watch_held', 'card_held', 'steam_puff'];

// approved sounds (design/sound/helper-bot.md): each clip's cues become sound keyframes on its Effects track,
// pointing at the real OGG files; the build stops if a cued sound has no real file
const SOUND_DIR = require('path').resolve(__dirname, '../sound/helper-bot');
const CUES = JSON.parse(fs.readFileSync(require('path').join(SOUND_DIR, 'helper-bot.cues.json'), 'utf8'));
const animations = [];
const ALL_CLIPS = [];
function anim(name, length, loop, tracks, steps = {}) {
  const animators = {};
  const add = (bone, channel, kf) => {
    const g = groupByName[bone]; if (!g) throw new Error('no bone ' + bone);
    animators[g.uuid] = animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
    animators[g.uuid].keyframes.push(kf);
  };
  for (const bone of Object.keys(tracks)) for (const ch of Object.keys(tracks[bone])) {
    const fn = tracks[bone][ch];
    const fps = loop === 'loop' ? 30 : FPS, interp = loop === 'loop' ? 'linear' : 'catmullrom';
    const n = Math.round(length * fps);
    for (let k = 0; k <= n; k++) { const t = k / fps, v = fn(t); add(bone, ch, { channel: ch, data_points: [{ x: String(r2(v[0])), y: String(r2(v[1])), z: String(r2(v[2])) }], uuid: uuid(), time: r2(t), color: -1, interpolation: interp }); }
  }
  if (tracks.lid_left || tracks.lid_right) for (const [cap, clear] of [['cap_train', 0.6], ['cap_logistics', 0.6]]) {
    const lidY = (t) => Math.max(...['lid_left', 'lid_right'].map((b) => (tracks[b] && tracks[b].position ? tracks[b].position(t)[1] : 0)));
    const own = tracks[cap] && tracks[cap].position;
    tracks = { ...tracks, [cap]: { ...(tracks[cap] || {}), position: (t) => addV(own ? own(t) : V(), V(0, Math.max(0, lidY(t) - clear), 0)) } };
  }
  for (const p of PROPS) steps = { ...steps, [p]: (CLIP_PROPS[name] || []).includes(p) ? { position: [[0, [0, PROP_DROP, 0]]] } : { scale: [[0, NONE]] } };
  for (const b of HIDDEN) if (!steps[b] && !(tracks[b] && tracks[b].scale)) steps = { ...steps, [b]: { scale: [[0, NONE]] } };
  for (const bone of Object.keys(steps)) for (const ch of Object.keys(steps[bone])) for (const [t, v] of steps[bone][ch])
    add(bone, ch, { channel: ch, data_points: [{ x: String(v[0]), y: String(v[1]), z: String(v[2]) }], uuid: uuid(), time: r2(t), color: -1, interpolation: 'step' });
  const cues = CUES.clips[name];
  if (cues) {
    const file = (id) => require('path').join(SOUND_DIR, 'real', id + '.ogg');
    const missing = cues.filter(([, id]) => !fs.existsSync(file(id)));
    if (missing.length) throw new Error(name + ': no real sound for ' + [...new Set(missing.map((c) => c[1]))].join(', '));
    animators.effects = {
      name: 'Effects', type: 'effect',
      keyframes: cues.map(([t, id]) => ({ channel: 'sound', data_points: [{ effect: CUES.namespace + '.' + id, locator: '', file: file(id).split(require('path').sep).join('/') }], uuid: uuid(), time: r2(t), color: -1, interpolation: 'linear' })),
    };
  }
  ALL_CLIPS.push([name, tracks, length, steps]);
  animations.push({ uuid: uuid(), name, loop, override: false, length, snapping: FPS, selected: false, group_name: '', anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });
}

// ---------- idle background (periodic, 3 s), reused by other clips ----------
const W3 = W2 / 3;
const idle = {
  body: { rotation: (t) => V(1.5 * sin(W3 * t + PI / 2), 0, 2 * sin(W3 * t)), position: (t) => V(0, 0.2 * sin(2 * W3 * t), 0) },
  spring: { scale: (t) => V(1, 1 + 0.2 * sin(2 * W3 * t), 1) },
  head: { rotation: (t) => V(-1.0 * sin(W3 * t + PI / 2 - 0.6), 0, -1.2 * sin(W3 * t - 0.6)) },
  eye_left: { rotation: (t) => V(0, 0, 1.2 * sin(W3 * t - 1.1)) },
  eye_right: { rotation: (t) => V(0, 0, 1.2 * sin(W3 * t - 1.1)) },
  whistle: { rotation: (t) => V(-4 * sin(W3 * t + PI / 2 - 1.4), 0, -5 * sin(W3 * t - 1.4)) },
  whistle_cord: { rotation: (t) => V(-5 * sin(W3 * t + PI / 2 - 1.8), 0, -6 * sin(W3 * t - 1.8)) },
  whisker_left: { rotation: (t) => V(0, 0, 1.5 * sin(W3 * t - 1.0)) },
  whisker_right: { rotation: (t) => V(0, 0, 1.5 * sin(W3 * t - 1.0)) },
  arm_left: { rotation: (t) => V(2 * sin(W3 * t - 0.9), 0, -3 - 2.5 * sin(W3 * t - 0.9)) },
  arm_right: { rotation: (t) => V(2 * sin(W3 * t - 0.9), 0, 3 - 2.5 * sin(W3 * t - 0.9)) },
  forearm_left: { rotation: (t) => V(-3 * sin(W3 * t - 1.3), 0, 0) },
  forearm_right: { rotation: (t) => V(-3 * sin(W3 * t - 1.3), 0, 0) },
};
const blink = (t, t0) => -2 * (seg(t, t0, t0 + 0.07) - seg(t, t0 + 0.09, t0 + 0.2));
const DELAY = { body: 0, neck: 0.03, head: 0.05, lid_left: 0.04, lid_right: 0.05, iris_left: 0.06, iris_right: 0.07, eye_left: 0.09, eye_right: 0.1, whisker_left: 0.1, whisker_right: 0.11, whistle: 0.14, whistle_cord: 0.18, arm_left: 0.06, arm_right: 0.05, forearm_left: 0.1, forearm_right: 0.09, claw_left: 0.13, claw_right: 0.13 };
const LOOSE = new Set(['whistle', 'whistle_cord']);

// keyed poses with springy transitions; a key is [time, V, freq?, damp?] and each bone's keys are offset by its chain delay
function keyedTracks(keys, o = {}) {
  const f0 = o.freq || 5, d0 = o.damp || 6;
  const T = {};
  const base0 = o.noIdle ? {} : o.base || idle;
  const bones = new Set([...Object.keys(base0), ...Object.keys(keys), ...Object.keys(o.extra || {})]);
  for (const b of bones) {
    const base = base0[b] || {}, kb = keys[b] || {}, ex = (o.extra || {})[b] || {};
    const d = DELAY[b] || 0, loose = LOOSE.has(b);
    const chans = new Set([...Object.keys(base), ...Object.keys(kb), ...Object.keys(ex)]);
    T[b] = {};
    for (const ch of chans) {
      const ks = kb[ch];
      T[b][ch] = (t) => {
        let v = base[ch] ? base[ch](t) : ch === 'scale' ? V(1, 1, 1) : V();
        if (ks) {
          const kv = [0, 1, 2].map((i) => {
            let x = ks[0][1][i];
            for (let k = 1; k < ks.length; k++) {
              const f = (ks[k][2] || f0) * (loose ? 0.8 : 1), dm = (ks[k][3] || d0) * (loose ? 0.55 : 1);
              x += (ks[k][1][i] - ks[k - 1][1][i]) * springStep(t, ks[k][0] + d, f, dm);
            }
            return x;
          });
          v = ch === 'scale' ? (base[ch] ? v.map((x, i) => x * kv[i]) : kv) : addV(v, kv);
        }
        return ex[ch] ? addV(v, ex[ch](t)) : v;
      };
    }
  }
  return T;
}
// ease every channel of T onto a target pose's first frame over the last 0.25 s, so the next clip starts seamlessly
function blendEnd(T, L, target, skip = []) {
  for (const b of Object.keys(target)) {
    if (skip.includes(b)) continue;
    for (const ch of Object.keys(target[b])) {
      T[b] = T[b] || {};
      const own = T[b][ch] || (() => (ch === 'scale' ? V(1, 1, 1) : V()));
      const c = target[b][ch](0);
      T[b][ch] = (t) => { const w = seg(t, L - 0.25, L), a = own(t); return a.map((x, i) => x + (c[i] - x) * w); };
    }
  }
  return T;
}

// ---------- FK ----------
const M4 = {
  I: () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  mul: (a, b) => { const o = new Array(16).fill(0); for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) for (let k = 0; k < 4; k++) o[r * 4 + c] += a[r * 4 + k] * b[k * 4 + c]; return o; },
  T: (v) => [1, 0, 0, v[0], 0, 1, 0, v[1], 0, 0, 1, v[2], 0, 0, 0, 1],
  S: (v) => [v[0], 0, 0, 0, 0, v[1], 0, 0, 0, 0, v[2], 0, 0, 0, 0, 1],
  Rx: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]; },
  Ry: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]; },
  Rz: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; },
  apply: (m, p) => [0, 1, 2].map((r) => m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]),
};
function chainMatrix(T, chain, t) {
  let M = M4.I();
  for (const b of chain) {
    const g = groupByName[b], tr = T[b] || {};
    const pos = tr.position ? tr.position(t) : V(), rot = addV(g.rot, tr.rotation ? tr.rotation(t) : V()), sc = tr.scale ? tr.scale(t) : V(1, 1, 1);
    M = M4.mul(M, M4.mul(M4.T(addV(g.origin, pos)), M4.mul(M4.Rz(rot[2]), M4.mul(M4.Ry(rot[1]), M4.mul(M4.Rx(rot[0]), M4.mul(M4.S(sc), M4.T(g.origin.map((x) => -x))))))));
  }
  return M;
}
// every bone's chain from the root, and every cube with its bone chain (for height and clearance reports)
const CHAIN = {}, CUBES = [];
(function walk(n, chain) {
  if (n.kind === 'c') { CUBES.push({ c: n, chain }); return; }
  const ch = [...chain, n.name]; CHAIN[n.name] = ch; n.kids.forEach((k) => walk(k, ch));
})(bot, []);
const ARM = (side) => [...CHAIN['arm_' + side]];
const CLAW = (side) => CHAIN['claw_' + side];
const GRIP = (side) => [side === 'right' ? 6 : -6, 4, 0]; // between the prongs

// the highest point of his model in a pose (hidden parts skipped), and what it is
function topOf(T, t) {
  let best = [-1e9, ''];
  for (const { c, chain } of CUBES) {
    if (chain.some((b) => T[b] && T[b].scale && T[b].scale(t).some((s) => s === 0))) continue;
    if (chain.some((b) => HIDDEN.includes(b)) && !chain.some((b) => HIDDEN.includes(b) && T[b] && T[b].scale)) continue;
    const M = chainMatrix(T, chain, t);
    for (const x of [c.from[0], c.to[0]]) for (const y of [c.from[1], c.to[1]]) for (const z of [c.from[2], c.to[2]]) {
      const p = M4.apply(M, [x, y, z]);
      if (p[1] > best[0]) best = [p[1], c.name];
    }
  }
  return best;
}

// ---------- self-collision: points sampled over each arm, tested against his own solid parts ----------
function m4inv(a) {
  const inv = new Array(16);
  inv[0] = a[5] * a[10] * a[15] - a[5] * a[11] * a[14] - a[9] * a[6] * a[15] + a[9] * a[7] * a[14] + a[13] * a[6] * a[11] - a[13] * a[7] * a[10];
  inv[4] = -a[4] * a[10] * a[15] + a[4] * a[11] * a[14] + a[8] * a[6] * a[15] - a[8] * a[7] * a[14] - a[12] * a[6] * a[11] + a[12] * a[7] * a[10];
  inv[8] = a[4] * a[9] * a[15] - a[4] * a[11] * a[13] - a[8] * a[5] * a[15] + a[8] * a[7] * a[13] + a[12] * a[5] * a[11] - a[12] * a[7] * a[9];
  inv[12] = -a[4] * a[9] * a[14] + a[4] * a[10] * a[13] + a[8] * a[5] * a[14] - a[8] * a[6] * a[13] - a[12] * a[5] * a[10] + a[12] * a[6] * a[9];
  inv[1] = -a[1] * a[10] * a[15] + a[1] * a[11] * a[14] + a[9] * a[2] * a[15] - a[9] * a[3] * a[14] - a[13] * a[2] * a[11] + a[13] * a[3] * a[10];
  inv[5] = a[0] * a[10] * a[15] - a[0] * a[11] * a[14] - a[8] * a[2] * a[15] + a[8] * a[3] * a[14] + a[12] * a[2] * a[11] - a[12] * a[3] * a[10];
  inv[9] = -a[0] * a[9] * a[15] + a[0] * a[11] * a[13] + a[8] * a[1] * a[15] - a[8] * a[3] * a[13] - a[12] * a[1] * a[11] + a[12] * a[3] * a[9];
  inv[13] = a[0] * a[9] * a[14] - a[0] * a[10] * a[13] - a[8] * a[1] * a[14] + a[8] * a[2] * a[13] + a[12] * a[1] * a[10] - a[12] * a[2] * a[9];
  inv[2] = a[1] * a[6] * a[15] - a[1] * a[7] * a[14] - a[5] * a[2] * a[15] + a[5] * a[3] * a[14] + a[13] * a[2] * a[7] - a[13] * a[3] * a[6];
  inv[6] = -a[0] * a[6] * a[15] + a[0] * a[7] * a[14] + a[4] * a[2] * a[15] - a[4] * a[3] * a[14] - a[12] * a[2] * a[7] + a[12] * a[3] * a[6];
  inv[10] = a[0] * a[5] * a[15] - a[0] * a[7] * a[13] - a[4] * a[1] * a[15] + a[4] * a[3] * a[13] + a[12] * a[1] * a[7] - a[12] * a[3] * a[5];
  inv[14] = -a[0] * a[5] * a[14] + a[0] * a[6] * a[13] + a[4] * a[1] * a[14] - a[4] * a[2] * a[13] - a[12] * a[1] * a[6] + a[12] * a[2] * a[5];
  inv[3] = -a[1] * a[6] * a[11] + a[1] * a[7] * a[10] + a[5] * a[2] * a[11] - a[5] * a[3] * a[10] - a[9] * a[2] * a[7] + a[9] * a[3] * a[6];
  inv[7] = a[0] * a[6] * a[11] - a[0] * a[7] * a[10] - a[4] * a[2] * a[11] + a[4] * a[3] * a[10] + a[8] * a[2] * a[7] - a[8] * a[3] * a[6];
  inv[11] = -a[0] * a[5] * a[11] + a[0] * a[7] * a[9] + a[4] * a[1] * a[11] - a[4] * a[3] * a[9] - a[8] * a[1] * a[7] + a[8] * a[3] * a[5];
  inv[15] = a[0] * a[5] * a[10] - a[0] * a[6] * a[9] - a[4] * a[1] * a[10] + a[4] * a[2] * a[9] + a[8] * a[1] * a[6] - a[8] * a[2] * a[5];
  const det = a[0] * inv[0] + a[1] * inv[4] + a[2] * inv[8] + a[3] * inv[12];
  return inv.map((x) => x / det);
}
// [name, bone, box in the bone's rest frame]
const SOLIDS = [
  ['chassis', 'body', [[-4, 7, -4], [4, 16, 4]]],
  ['side panel', 'body', [[4, 8, -3], [5, 15, 3]]],
  ['side panel', 'body', [[-5, 8, -3], [-4, 15, 3]]],
  ['waist band', 'body', [[-4.5, 11, -4.5], [4.5, 12, 4.5]]],
  ['schedule clip', 'body', [[1.75, 14.25, -4.6], [3.25, 14.75, -4.1]]],
  ['right pod', 'eye_right', [[1, 17, -4], [7, 22, 2]]],
  ['left pod', 'eye_left', [[-7, 17, -4], [-1, 22, 2]]],
  ['right lid', 'lid_right', [[1, 20, -5], [7, 23, -4]]],
  ['left lid', 'lid_left', [[-7, 20, -5], [-1, 23, -4]]],
  ['whiskers', 'head', [[-5, 16.25, -4.75], [5, 18, -4]]],
  ['post', 'post', [[-1.5, 3, -1.5], [1.5, 6, 1.5]]],
  ['foot', 'foot', [[-6, 0, -4], [6, 2, 4]]],
  ['foot', 'foot', [[-4, 0, -6], [4, 2, 6]]],
];
// arm sample points in rest coordinates, by bone; the shoulder end of the upper arm is left out (it sits against the
// side panel by design)
const ARM_PTS = {};
for (const side of ['left', 'right']) {
  const s = side === 'right' ? 1 : -1, pts = [];
  for (const x of [5, 7]) for (const z of [-1, 1]) {
    for (const y of [10, 11.5]) pts.push(['arm_' + side, [x * s, y, z]]);
    for (const y of [6, 7.5, 9]) pts.push(['forearm_' + side, [x * s, y, z]]);
    pts.push(['claw_' + side, [x * s, 5.5, z]]);
  }
  for (const z of [-0.6, 0.6]) for (const y of [3.2, 4.2]) pts.push(['claw_' + side, [6 * s, y, z]]);
  ARM_PTS[side] = pts;
}
// the box frames at time t (world → box), and the matrix carrying the body
function solidsAt(T, t) {
  return SOLIDS.map(([name, bone, box]) => {
    const M = chainMatrix(T, CHAIN[bone], t), c = box[0].map((v, i) => (v + box[1][i]) / 2);
    return { name, box, inv: m4inv(M), centre: M4.apply(M, c), radius: Math.hypot(...box[0].map((v, i) => (box[1][i] - v) / 2)) };
  });
}
const LOCAL = (T, b, t) => chainMatrix(T, [b], t);
// deepest penetration of one arm's points into his solids; claw points may touch solids near the claw's own target
function armClip(side, T, t, solids, Mbody, target) {
  const Ma = M4.mul(Mbody, LOCAL(T, 'arm_' + side, t)), Mf = M4.mul(Ma, LOCAL(T, 'forearm_' + side, t)), Mc = M4.mul(Mf, LOCAL(T, 'claw_' + side, t));
  const M = { ['arm_' + side]: Ma, ['forearm_' + side]: Mf, ['claw_' + side]: Mc };
  let worst = [0, ''];
  const nearTarget = target ? solids.map((s) => { const q0 = M4.apply(s.inv, target); return Math.min(q0[0] - s.box[0][0], s.box[1][0] - q0[0], q0[1] - s.box[0][1], s.box[1][1] - q0[1], q0[2] - s.box[0][2], s.box[1][2] - q0[2]) > -1.5; }) : null;
  for (const [bone, pt] of ARM_PTS[side]) {
    const p = M4.apply(M[bone], pt);
    for (let k = 0; k < solids.length; k++) {
      const s = solids[k];
      if (Math.hypot(p[0] - s.centre[0], p[1] - s.centre[1], p[2] - s.centre[2]) > s.radius) continue;
      const q = M4.apply(s.inv, p);
      let d = Math.min(q[0] - s.box[0][0], s.box[1][0] - q[0], q[1] - s.box[0][1], s.box[1][1] - q[1], q[2] - s.box[0][2], s.box[1][2] - q[2]);
      if (nearTarget && nearTarget[k] && bone.startsWith('claw')) d -= 0.5;
      if (d > worst[0]) worst = [d, bone.replace('_' + side, '') + ' in ' + s.name];
    }
  }
  return worst;
}
function clipReport(name, T, L) {
  let worst = [0, '', 0, ''];
  for (let t = 0; t <= L + 1e-9; t += 1 / 30) {
    const solids = solidsAt(T, t), Mb = chainMatrix(T, CHAIN.body, t);
    for (const side of ['left', 'right']) { const w = armClip(side, T, t, solids, Mb); if (w[0] > worst[0]) worst = [w[0], w[1], t, side]; }
  }
  return worst;
}

// arm IK for a claw grip point on a target: arm pitch, yaw and sideways raise, forearm pitch (q = [a, y, f, z]);
// warm-started coordinate descent, with a few fresh starts when the warm start gets stuck
const IK_STARTS = [[60, 0, 20, 0], [120, 0, 100, 0], [160, -30, 140, 0], [20, 0, 120, -60], [90, -40, 120, -40], [150, 30, 140, 40], [40, 0, 140, 60], [100, 20, 60, 30]];
function ikArm(side, target, Tbase, t, init = [60, 0, 20, 0], o = {}) {
  const sz = side === 'right' ? 1 : -1; // sideways raise, mirrored so + lifts the arm outward on both sides
  // his solids and the body's frame don't depend on this arm, so they're worked out once per frame
  const solids = solidsAt(Tbase, t), Mb = chainMatrix(Tbase, CHAIN.body, t);
  const cost = (q) => {
    const T = { ...Tbase, ['arm_' + side]: { rotation: () => V(q[0], q[1], sz * q[3]) }, ['forearm_' + side]: { rotation: () => V(q[2], 0, 0) } };
    const Mc = M4.mul(M4.mul(M4.mul(Mb, LOCAL(T, 'arm_' + side, t)), LOCAL(T, 'forearm_' + side, t)), LOCAL(T, 'claw_' + side, t));
    const p = M4.apply(Mc, GRIP(side));
    const clip = armClip(side, T, t, solids, Mb, target)[0];
    return Math.hypot(p[0] - target[0], p[1] - target[1], p[2] - target[2]) + 6 * Math.max(0, clip - 0.15) + 0.002 * Math.abs(q[2] - (o.preferF ?? 30)) + 0.002 * Math.abs(q[1]) + 0.003 * Math.abs(q[3]);
  };
  const descend = (q0) => {
    let q = [...q0, 0].slice(0, 4), c = cost(q);
    for (const step of [16, 8, 4, 2, 1, 0.5, 0.25]) {
      let improved = true;
      while (improved) {
        improved = false;
        for (let k = 0; k < 4; k++) for (const s of [-step, step]) {
          const q2 = [...q]; q2[k] += s;
          if (q2[0] < -40 || q2[0] > 178 || Math.abs(q2[1]) > 75 || q2[2] < -155 || q2[2] > 155 || q2[3] < -40 || q2[3] > 110) continue;
          const c2 = cost(q2);
          if (c2 < c) { c = c2; q = q2; improved = true; }
        }
      }
    }
    return [q, c];
  };
  let best = descend(init);
  if (best[1] > 0.6) for (const s of IK_STARTS) {
    const r = descend(s);
    const jump = 0.002 * r[0].reduce((a, x, i) => a + Math.abs(x - (init[i] || 0)), 0);
    if (r[1] + jump < best[1]) best = r;
  }
  return best;
}
// tracks that keep a claw on a moving target, solved at 60 fps up front (warm start) and interpolated
function armOnTarget(side, targetFn, Tbase, L, weightFn = () => 1, init) {
  const N = Math.ceil(L * 30) + 1, sol = [];
  let q = init || [60, 0, 20, 0], worst = 0;
  for (let k = 0; k < N; k++) {
    const t = k / 30, [q2, c] = ikArm(side, targetFn(t), Tbase, t, q);
    q = q2; sol.push(q2); if (weightFn(t) > 0.99) worst = Math.max(worst, c);
  }
  const at = (t) => { const f = Math.max(0, Math.min(N - 1.001, t * 30)), k = Math.floor(f), u = f - k; return sol[k].map((x, i) => x + (sol[k + 1][i] - x) * u); };
  return { arm: (t) => at(t), worst };
}

// ---------- poses ----------
// driving: leaning in, claws on the two lever handles (solved below for the lean)
const DRIVE_LEAN = -14;
// ducked: the body slides down over the post (post down 3.75, gimbal spring flat, body down 1 more), neck in 1,
// lids half down, whistle folded back flat, cap squashed against the ceiling
const DUCK = {
  post: { position: V(0, -3.75, 0) },
  spring: { scale: V(1, 0.02, 1) },
  body: { position: V(0, -1, 0) },
  neck: { position: V(0, -1, 0) },
  whistle: { rotation: V(100, 0, 0) },
  whistle_cord: { rotation: V(-100, 0, 0) },
  lid_left: { position: V(0, -1, 0) },
  lid_right: { position: V(0, -1, 0) },
  cap_train: { scale: V(1, 0.25, 1) },
};
const toTracks = (pose) => Object.fromEntries(Object.entries(pose).map(([b, chs]) => [b, Object.fromEntries(Object.entries(chs).map(([ch, v]) => [ch, () => v]))]));

// drive pose arms: solve once for the lean
const leanT = { body: { rotation: () => V(DRIVE_LEAN, 0, 0) } };
// the claws close on the near side of each knob
const GRIP_AT = (side) => addV(LEVERS[side].tip, V(0, -0.3, 0.8));
const [qR, eR] = ikArm('right', GRIP_AT('right'), leanT, 0, [70, 10, 30]);
const [qL, eL] = ikArm('left', GRIP_AT('left'), leanT, 0, [80, -10, 30]);
console.error('lever reach error: right ' + eR.toFixed(2) + ' px, left ' + eL.toFixed(2) + ' px', 'arms', qR.map(Math.round), qL.map(Math.round));
const DRIVE = {
  body: { rotation: V(DRIVE_LEAN, 0, 0) },
  head: { rotation: V(8, 0, 0) },
  arm_right: { rotation: V(qR[0], qR[1], qR[3]) }, forearm_right: { rotation: V(qR[2], 0, 0) },
  arm_left: { rotation: V(qL[0], qL[1], -qL[3]) }, forearm_left: { rotation: V(qL[2], 0, 0) },
};

// ---------- 1. idle ----------
{
  const T = {};
  for (const b of Object.keys(idle)) T[b] = { ...idle[b] };
  T.lid_left = { position: (t) => V(0, blink(t, 2.2), 0) };
  T.lid_right = { position: (t) => V(0, blink(t, 2.2), 0) };
  anim('idle', 3, 'loop', T);
}

// ---------- pose sheet (one held pose per second, for review) ----------
{
  const S = {};
  const put = (bone, ch, t, v) => { S[bone] = S[bone] || {}; S[bone][ch] = S[bone][ch] || []; S[bone][ch].push([t, v]); };
  const SHEET = [
    ['rest', {}],
    ['driving, train cap, schedule', { ...DRIVE, cap_train: { scale: ONE }, schedule_card: { scale: ONE } }],
    ['ducked, train cap', { ...DUCK, cap_train: { scale: V(1, 0.25, 1) }, arm_left: { rotation: V(40, 0, -8) }, arm_right: { rotation: V(40, 0, 8) }, forearm_left: { rotation: V(60, 0, 0) }, forearm_right: { rotation: V(60, 0, 0) } }],
    ['shopkeeper, logistics cap', { cap_logistics: { scale: ONE }, swivel: { rotation: V(0, 0, 0) } }],
    ['watch out, lid open', { watch_pocket: { scale: NONE }, watch_held: { scale: ONE }, watch_lid: { rotation: V(115, 0, 0) }, arm_left: { rotation: V(55, -20, 0) }, forearm_left: { rotation: V(70, 0, 0) }, head: { rotation: V(18, 0, 0) } }],
    ['reading the card, puzzled', { card_held: { scale: ONE }, arm_right: { rotation: V(55, 20, 0) }, forearm_right: { rotation: V(70, 0, 0) }, head: { rotation: V(15, 0, 12) }, cap_train: { scale: ONE } }],
    ['toot (cord pulled, puff)', { whistle_cord: { position: V(0, -1.5, 0) }, steam_puff: { scale: ONE, position: V(0, 1, 0) }, arm_left: { rotation: V(10, 0, -95) }, forearm_left: { rotation: V(0, 0, -60) } }],
    ['proud', { head: { rotation: V(-8, 0, 0) }, body: { scale: V(1.04, 1, 1.04) }, lid_left: { position: V(0, -0.8, 0) }, lid_right: { position: V(0, -0.8, 0) }, whisker_left: { rotation: V(0, 0, -14) }, whisker_right: { rotation: V(0, 0, 14) }, cap_train: { scale: ONE } }],
    ['swivelled to wave', { swivel: { rotation: V(0, 70, 0) }, arm_right: { rotation: V(-10, 0, 115) }, forearm_right: { rotation: V(0, 0, 45) } }],
  ];
  const bones = new Set(SHEET.flatMap(([, p]) => Object.keys(p)));
  SHEET.forEach(([label, pose], t) => {
    for (const b of bones) {
      const chs = new Set([...Object.keys(pose[b] || {}), ...SHEET.flatMap(([, p]) => Object.keys(p[b] || {}))]);
      for (const ch of chs) put(b, ch, t, (pose[b] || {})[ch] || (ch === 'scale' ? (HIDDEN.includes(b) ? NONE : ONE) : Z));
    }
    const top = topOf(toTracks(pose), 0);
    console.error(('pose ' + t + ' ' + label).padEnd(40), 'top y ' + top[0].toFixed(2) + ' (' + top[1] + ')');
  });
  anim('pose_sheet', SHEET.length, 'once', {}, S);
}

module.exports = { solidsAt, armClip, GRIP_AT, anim, idle, keyedTracks, blendEnd, springStep, seg, lin, env, hopArc, osc, V, addV, C3, Z, ONE, NONE, PI, sin, W2, DELAY, DRIVE, DUCK, chainMatrix, M4, CHAIN, ikArm, armOnTarget, topOf, toTracks, blink, ALL_CLIPS, animations, LEVERS };
if (fs.existsSync(require('path').join(__dirname, 'helper-bot.motion.js'))) require('./helper-bot.motion.js');
// arms against his own body, every clip (CLIPS=0 skips it)
if (process.env.CLIPS !== '0') for (const [name, T, L] of ALL_CLIPS) {
  if (name === 'pose_sheet' || name === 'expression_sheet') continue;
  const w = clipReport(name, T, L);
  if (w[0] > 0.3) console.error(('armclip ' + name).padEnd(28) + w[0].toFixed(2) + ' px  ' + w[3] + ' ' + w[1] + '  at ' + w[2].toFixed(2) + 's');
}

const bb = {
  meta: { format_version: '5.0', model_format: 'modded_entity', box_uv: true },
  name: 'helper_bot', model_identifier: 'helper_bot', modded_entity_entity_class: '', modded_entity_version: '1.17', modded_entity_flip_y: true,
  visible_box: [1, 1.5, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
  resolution: { width: TEX, height: TEX },
  elements, groups, outliner,
  textures: [{ path: '', name: 'helper_bot.png', folder: '', namespace: '', id: '0', group: '', width: TEX, height: TEX, uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + pngBuf.toString('base64') }],
  animations,
};
fs.writeFileSync(OUT, JSON.stringify(bb));
console.error('wrote', OUT, elements.length, 'cubes', groups.length, 'groups', animations.length, 'animations');
// --game: also write the in-game assets (helper-bot.game-export.js)
if (ARGS.includes('--game')) require('./helper-bot.game-export.js')({ bot, P, DYES, dyeShades, paintCube, png, animations, cues: CUES });
