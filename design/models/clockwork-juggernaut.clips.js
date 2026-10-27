// Clockwork Juggernaut preview clips (brief: design/briefs/clockwork-juggernaut.md, Motion Set and Revision 2). Loaded by
// clockwork-juggernaut.gen.js. Motion is Trade Bot's recipe: springy keyed poses (springStep), each bone a beat behind
// the one carrying it, plus small adjustments that tick like an escapement. Every clip is checked for self-clipping
// (clockwork-juggernaut.collide.js) and his feet are kept on the ground through crouches and kneels.
const J = require('./clockwork-juggernaut.gen.js');
const { groupByName, uuid } = J;
const CL = require('./clockwork-juggernaut.collide.js');
// approved sounds (design/sound/clockwork-juggernaut.md): each clip's cues become sound keyframes on its Effects track,
// pointing at the real OGG files; the build stops if a cued sound has no real file
const fs = require('fs');
const path = require('path');
const SOUND_DIR = path.resolve(__dirname, '../sound/clockwork-juggernaut');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'clockwork-juggernaut.cues.json'), 'utf8'));
function soundTrack(name) {
  const cues = CUES.clips[name];
  if (!cues) return null;
  const file = (id) => path.join(SOUND_DIR, 'real', id + '.ogg');
  const missing = cues.filter(([, id]) => !fs.existsSync(file(id)));
  if (missing.length) throw new Error(name + ': no real sound for ' + [...new Set(missing.map((c) => c[1]))].join(', '));
  return {
    name: 'Effects', type: 'effect',
    keyframes: cues.map(([t, id]) => ({ channel: 'sound', data_points: [{ effect: CUES.namespace + '.' + id, locator: '', file: file(id).split(path.sep).join('/') }], uuid: uuid(), time: r2(t), color: -1, interpolation: 'linear' })),
  };
}

// ---------- maths ----------
const FPS = 20;
const r2 = (v) => Math.round(v * 1000) / 1000;
const PI = Math.PI, sin = Math.sin, cos = Math.cos, W2 = 2 * PI;
const smooth = (t) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
const seg = (t, a, b) => smooth((t - a) / (b - a));
const lin = (t, a, b) => (t <= a ? 0 : t >= b ? 1 : (t - a) / (b - a));
const env = (t, a, b, c, d) => seg(t, a, b) * (1 - seg(t, c, d));
const springStep = (t, t0, freq = 6, damp = 4) => { if (t < t0) return 0; const u = t - t0; return 1 - Math.exp(-damp * u) * (Math.cos(2 * PI * freq * u / 4) + (damp / (2 * PI * freq / 4)) * Math.sin(2 * PI * freq * u / 4)); };
const V = (x = 0, y = 0, z = 0) => [x, y, z];
const addV = (...vs) => vs.reduce((a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]], [0, 0, 0]);
const ONE = V(1, 1, 1), NONE = V(0, 0, 0);
const tick = (v, step) => Math.round(v / step) * step; // an escapement: small turns go in steps
const decay = (t, t0, hz, k = 6) => (t < t0 ? 0 : Math.sin(W2 * hz * (t - t0)) * Math.exp(-k * (t - t0)));

// each bone a beat behind the one carrying it: heavy parts lag most, loose parts settle last
const DELAY = {
  hips: 0, waist: 0.03, neck: 0.08, head: 0.1, jaw: 0.13, brow_left: 0.06, brow_right: 0.07, helm: 0.12,
  arm_left: 0.1, arm_right: 0.1, forearm_left: 0.13, forearm_right: 0.13, claw_right: 0.16, fist_left: 0.16,
  finger_1: 0.17, finger_2: 0.18, finger_3: 0.19, thumb: 0.17, key: 0.12, pipe_left: 0.18, pipe_right: 0.19,
  shield_left: 0.15, shield_right: 0.15, chain_end: 0.24,
};
const LOOSE = new Set(['pipe_left', 'pipe_right', 'chain_end', 'shield_left', 'shield_right', 'jaw']);

// ---------- poses (deltas from the rest pose) ----------
// the rewind: bowed forward, presenting the key, the slit nearly out
const REWIND = {
  waist: { rotation: V(-9, 0, 0) }, neck: { rotation: V(-12, 0, 0) }, head: { rotation: V(-6, 0, 4) },
  arm_left: { rotation: V(-6, 0, 2) }, arm_right: { rotation: V(-6, 0, -2) }, forearm_left: { rotation: V(-6, 0, 0) }, forearm_right: { rotation: V(-8, 0, 0) },
  brow_left: { rotation: V(0, 0, 14) }, brow_right: { rotation: V(0, 0, -14) },
  slit_glow: { scale: V(1, 0.3, 1) },
};
// kneeling: right knee down, left foot planted, fists on the ground, head bowed
const KNEEL = {
  leg_right: { rotation: V(-20, 0, 0) }, shin_right: { rotation: V(-66, 0, 0) }, foot_right: { rotation: V(-6, 0, 0) },
  leg_left: { rotation: V(56, 0, 0) }, shin_left: { rotation: V(-50, 0, 0) }, foot_left: { rotation: V(-10, 0, 0) },
  waist: { rotation: V(-10, 0, 0) }, neck: { rotation: V(-10, 0, 0) }, head: { rotation: V(-6, -4, 5) },
  arm_right: { rotation: V(6, 0, 4) }, arm_left: { rotation: V(8, 0, -4) }, forearm_right: { rotation: V(-6, 0, 0) }, forearm_left: { rotation: V(-6, 0, 0) },
  slit_glow: { scale: V(1, 0, 1) }, brow_left: { rotation: V(0, 0, 10) }, brow_right: { rotation: V(0, 0, -10) },
};
// the deep crouch before the leap (and where the ritual wake starts)
const COIL = {
  leg_right: { rotation: V(40, 0, 0) }, shin_right: { rotation: V(-66, 0, 0) }, foot_right: { rotation: V(14, 0, 0) },
  leg_left: { rotation: V(40, 0, 0) }, shin_left: { rotation: V(-66, 0, 0) }, foot_left: { rotation: V(14, 0, 0) },
  waist: { rotation: V(-14, 0, 0) }, neck: { rotation: V(10, 0, 0) },
  arm_right: { rotation: V(-28, 0, 10) }, arm_left: { rotation: V(-28, 0, -10) }, forearm_right: { rotation: V(10, 0, 0) }, forearm_left: { rotation: V(10, 0, 0) },
};
// arms straight out to the sides (the gear spin)
const ARMS_OUT = {
  arm_right: { rotation: V(-22, 0, 84) }, arm_left: { rotation: V(-22, 0, -84) }, forearm_right: { rotation: V(-14, 0, 0) }, forearm_left: { rotation: V(-10, 0, 0) },
  waist: { rotation: V(8, 0, 0) }, brow_left: { rotation: V(0, 0, -6) }, brow_right: { rotation: V(0, 0, 6) },
};
// the salute: right claw to the brow, chest up
const SALUTE = {
  waist: { rotation: V(10, 0, 0) }, neck: { rotation: V(-6, 0, 0) }, head: { rotation: V(0, -5, 5) },
  arm_right: { rotation: V(66, -10, 4) }, forearm_right: { rotation: V(100, 0, 0) },
  finger_1: { rotation: V(11, 0, 0) }, finger_2: { rotation: V(12, 0, 0) }, finger_3: { rotation: V(9, 0, 0) },
  brow_left: { rotation: V(0, 0, 6) }, brow_right: { rotation: V(0, 0, -6) },
};
const FURY = { brow_left: { rotation: V(0, 0, -10) }, brow_right: { rotation: V(0, 0, 10) }, slit_glow: { scale: V(1.05, 0.75, 1) } };
const merge = (...ps) => { const o = {}; for (const p of ps) for (const b of Object.keys(p)) { o[b] = o[b] || {}; for (const ch of Object.keys(p[b])) o[b][ch] = ch === 'scale' && o[b][ch] ? o[b][ch].map((v, i) => v * p[b][ch][i]) : o[b][ch] ? addV(o[b][ch], p[b][ch]) : p[b][ch]; } return o; };

// ---------- tracks ----------
// keys: [[t, pose, freq?, damp?], ...]; every bone named in any pose gets springy transitions between them
function posed(keys, o = {}) {
  const f0 = o.freq || 4, d0 = o.damp || 6;
  const bones = new Set(keys.flatMap(([, p]) => Object.keys(p)));
  const T = {};
  for (const b of bones) {
    const chans = new Set(keys.flatMap(([, p]) => Object.keys(p[b] || {})));
    const dl = (o.delays === false ? 0 : DELAY[b] || 0), loose = LOOSE.has(b);
    T[b] = {};
    for (const ch of chans) {
      const id = ch === 'scale' ? ONE : V();
      const vals = keys.map(([, p]) => (p[b] && p[b][ch]) || id);
      T[b][ch] = (t) => [0, 1, 2].map((i) => {
        let x = vals[0][i];
        for (let k = 1; k < keys.length; k++) {
          const f = (keys[k][2] || f0) * (loose ? 0.8 : 1), dm = (keys[k][3] || d0) * (loose ? 0.6 : 1);
          x += (vals[k][i] - vals[k - 1][i]) * springStep(t, keys[k][0] + dl, f, dm);
        }
        return ch === 'scale' && Math.abs(x) < 0.02 ? 0 : x;
      });
    }
  }
  return T;
}
// add tracks together (rotations and positions add, scales multiply)
function mix(...Ts) {
  const o = {};
  for (const T of Ts) for (const b of Object.keys(T)) for (const ch of Object.keys(T[b])) {
    o[b] = o[b] || {};
    const prev = o[b][ch], f = T[b][ch];
    o[b][ch] = !prev ? f : ch === 'scale' ? (t) => prev(t).map((v, i) => v * f(t)[i]) : (t) => addV(prev(t), f(t));
  }
  return o;
}
const fn = (o) => { const T = {}; for (const b of Object.keys(o)) { T[b] = {}; for (const ch of Object.keys(o[b])) T[b][ch] = o[b][ch]; } return T; };
// ease every channel onto `target` (a pose) over the last `span` s, so the next clip starts where this one ends
function settleTo(T, L, target = {}, span = 0.3) {
  const o = { ...T };
  const bones = new Set([...Object.keys(T), ...Object.keys(target)]);
  for (const b of bones) for (const ch of new Set([...Object.keys(T[b] || {}), ...Object.keys(target[b] || {})])) {
    const id = ch === 'scale' ? ONE : V(), goal = (target[b] && target[b][ch]) || id, own = (T[b] && T[b][ch]) || (() => id);
    o[b] = { ...(o[b] || {}) };
    o[b][ch] = (t) => { const w = seg(t, L - span, L), a = own(t); return a.map((x, i) => x + (goal[i] - x) * w); };
  }
  return o;
}

// ---------- clips ----------
const animations = [], ALL = [];
// props and hidden parts: shown only where a clip scales them in
const HIDDEN = Object.values(groupByName).filter((g) => g.hidden).map((g) => g.name);
const FEET = (e) => /^(foot_|toe_|heel_)/.test(e.c.name);
const GROUNDERS = (e) => /^(foot_|toe_|heel_|knee_cap_|shin_|fist_|knuckle_|finger_|palm_|thumb_)/.test(e.c.name);
function anim(name, length, loop, T, o = {}) {
  T = { ...T };
  for (const s of ['right', 'left']) {
    const a = T['arm_' + s] && T['arm_' + s].rotation;
    if (!a) continue;
    const own = T['pauldron_' + s] && T['pauldron_' + s].rotation;
    T['pauldron_' + s] = { ...(T['pauldron_' + s] || {}), rotation: (t) => { const r = a(t); const past = (v, m) => (v > m ? v - m : v < -m ? v + m : 0); return addV(own ? own(t) : V(), V(0, 0, -0.9 * past(r[2], 30))); } };
  }
  for (const b of HIDDEN) if (!(T[b] && T[b].scale)) T[b] = { ...(T[b] || {}), scale: () => NONE };
  // feet (or knee and fists) on the ground: the whole body is lifted or lowered so the lowest of them sits at y = 0
  if (o.ground) {
    const w = typeof o.ground === 'function' ? o.ground : () => 1;
    const only = o.kneel ? GROUNDERS : FEET;
    const base = T.juggernaut && T.juggernaut.position;
    const cache = new Map();
    const lift = (t) => {
      const k = r2(t);
      if (!cache.has(k)) { const T0 = { ...T, juggernaut: { ...(T.juggernaut || {}), position: base || (() => V()) } }; cache.set(k, -CL.extent(T0, t, only).lo[0]); }
      return cache.get(k);
    };
    T.juggernaut = { ...(T.juggernaut || {}), position: (t) => addV(base ? base(t) : V(), V(0, lift(t) * w(t), 0)) };
  }
  const animators = {};
  const add = (bone, kf) => {
    const g = groupByName[bone]; if (!g) throw new Error(name + ': no bone ' + bone);
    animators[g.uuid] = animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
    animators[g.uuid].keyframes.push(kf);
  };
  const fps = loop === 'loop' ? 30 : FPS;
  for (const bone of Object.keys(T)) for (const ch of Object.keys(T[bone])) {
    const f = T[bone][ch], n = Math.round(length * fps);
    const steps = ch === 'scale' && o.stepScale !== false;
    const vals = [];
    for (let k = 0; k <= n; k++) vals.push([k / fps, f(k / fps)]);
    // skip channels that never move away from identity
    const id = ch === 'scale' ? 1 : 0;
    if (vals.every(([, v]) => v.every((x) => Math.abs(x - id) < 1e-4)) && !(ch === 'scale' && HIDDEN.includes(bone))) continue;
    for (const [t, v] of vals) add(bone, { channel: ch, data_points: [{ x: String(r2(v[0])), y: String(r2(v[1])), z: String(r2(v[2])) }], uuid: uuid(), time: r2(t), color: -1, interpolation: steps && v.some((x) => x === 0) ? 'step' : loop === 'loop' ? 'linear' : 'catmullrom' });
  }
  const effects = soundTrack(name);
  if (effects) animators.effects = effects;
  ALL.push({ name, T, length, skip: o.skip || [] });
  animations.push({ uuid: uuid(), name, loop, override: false, length, snapping: fps, selected: false, group_name: '', anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });
}

// the key turns steadily while he's wound: whole half-turns per loop, so loops are seamless (the bow is symmetric)
const keyTurn = (L, halfTurns = 1) => ({ key: { rotation: (t) => V(0, 0, (180 * halfTurns * t) / L) } });
// the idle background, periodic in P seconds (whole cycles)
function idleOf(P, k = 1) {
  const w = W2 / P;
  return {
    hips: { position: (t) => V(0, -0.35 * k * (1 - cos(2 * w * t)) / 2, 0) },
    waist: { rotation: (t) => V(1.2 * k * sin(w * t), 0, 1.2 * k * sin(w * t + 1)) },
    neck: { rotation: (t) => V(-0.8 * k * sin(w * t - 0.5), tick(5 * k * sin(w * t - 0.9), 2), 0) },
    arm_right: { rotation: (t) => V(1.6 * k * sin(w * t - 0.8), 0, 1 * k * sin(w * t)) },
    arm_left: { rotation: (t) => V(1.6 * k * sin(w * t - 1), 0, -1 * k * sin(w * t)) },
    forearm_right: { rotation: (t) => V(-2 * k * sin(w * t - 1.2), 0, 0) },
    forearm_left: { rotation: (t) => V(-2 * k * sin(w * t - 1.4), 0, 0) },
    pipe_left: { rotation: (t) => V(-2 * sin(w * t - 1.6), 0, 1.5 * sin(w * t - 1.2)) },
    pipe_right: { rotation: (t) => V(-2 * sin(w * t - 1.7), 0, -1.5 * sin(w * t - 1.3)) },
    chain_end: { rotation: (t) => V(4 * sin(w * t - 2), 0, 3 * sin(w * t - 1.5)) },
  };
}

// 1. idle (3 s loop): weight shifting, breathing, the head ticking as it scans, the key turning, one claw flex
anim('idle', 3, 'loop', mix(fn(idleOf(3)), fn(keyTurn(3)), fn({
  finger_1: { rotation: (t) => V(-8 * env(t, 1.4, 1.7, 2.0, 2.4), 0, 0) },
  finger_2: { rotation: (t) => V(-9 * env(t, 1.45, 1.75, 2.0, 2.4), 0, 0) },
  finger_3: { rotation: (t) => V(-7 * env(t, 1.5, 1.8, 2.0, 2.4), 0, 0) },
})), { ground: true });

// 2. walk (1.6 s loop, in place): a heavy, wide-legged march; arms counter-swing; the key turns; the chain swings
{
  const L = 1.6, w = W2 / L;
  const lift = (ph) => (t) => Math.max(0, sin(w * t + ph)); // a leg's swing phase
  anim('walk', L, 'loop', mix(fn(keyTurn(L)), fn({
    leg_right: { rotation: (t) => V(16 * sin(w * t), 0, 0) },
    leg_left: { rotation: (t) => V(-16 * sin(w * t), 0, 0) },
    shin_right: { rotation: (t) => V(-26 * lift(0.9)(t), 0, 0) },
    shin_left: { rotation: (t) => V(-26 * lift(0.9 + PI)(t), 0, 0) },
    foot_right: { rotation: (t) => V(Math.max(-13, Math.min(13, -16 * sin(w * t) + 26 * lift(0.9)(t))), 0, 0) },
    foot_left: { rotation: (t) => V(Math.max(-13, Math.min(13, 16 * sin(w * t) + 26 * lift(0.9 + PI)(t))), 0, 0) },
    hips: { position: (t) => V(1.2 * sin(w * t + 0.4), -1.6 * (1 - cos(2 * w * t)) / 2, 0), rotation: (t) => V(0, 4 * sin(w * t), -2.5 * sin(w * t + 0.4)) },
    waist: { rotation: (t) => V(1.5 * cos(2 * w * t), -5 * sin(w * t - 0.2), 2.5 * sin(w * t + 0.2)) },
    neck: { rotation: (t) => V(-1.5 * cos(2 * w * t - 0.5), 3 * sin(w * t - 0.4), 0) },
    arm_right: { rotation: (t) => V(-12 * sin(w * t - 0.25), 0, 2 * (1 - cos(2 * w * t)) / 2) },
    arm_left: { rotation: (t) => V(12 * sin(w * t - 0.25), 0, -2 * (1 - cos(2 * w * t)) / 2) },
    forearm_right: { rotation: (t) => V(6 + -6 * sin(w * t - 0.6), 0, 0) },
    forearm_left: { rotation: (t) => V(6 + 6 * sin(w * t - 0.6), 0, 0) },
    finger_1: { rotation: (t) => V(6 * sin(w * t - 0.8), 0, 0) }, finger_2: { rotation: (t) => V(6 * sin(w * t - 0.9), 0, 0) }, finger_3: { rotation: (t) => V(6 * sin(w * t - 1), 0, 0) },
    pipe_left: { rotation: (t) => V(-4 * cos(2 * w * t - 1), 0, 0) }, pipe_right: { rotation: (t) => V(-4 * cos(2 * w * t - 1.1), 0, 0) },
    chain_end: { rotation: (t) => V(-14 * sin(w * t - 1.4), 0, 6 * sin(w * t - 1)) },
    shield_right: { rotation: (t) => V(0, 0, 1.5 * cos(2 * w * t - 0.8)) }, shield_left: { rotation: (t) => V(0, 0, -1.5 * cos(2 * w * t - 0.8)) },
    jaw: { rotation: (t) => V(-1 * cos(2 * w * t - 1), 0, 0) },
  })), { ground: true });
}

// 3. scan (2.5 s): the head ticks left in steps, holds, ticks right, the brows narrow at nothing
anim('scan', 2.5, 'once', mix(fn(idleOf(2.5, 0.6)), fn(keyTurn(2.5, 1)), fn({
  head: { rotation: (t) => V(0, tick(28 * (seg(t, 0.2, 0.7) - seg(t, 1.1, 1.6) * 2 + seg(t, 1.9, 2.4)), 4), 0) },
  brow_left: { rotation: (t) => V(0, 0, -6 * env(t, 0.75, 0.9, 1.0, 1.15) - 6 * env(t, 1.65, 1.8, 1.9, 2.05)) },
  brow_right: { rotation: (t) => V(0, 0, 6 * env(t, 0.75, 0.9, 1.0, 1.15) + 6 * env(t, 1.65, 1.8, 1.9, 2.05)) },
})));

// 4. knuckles (2 s): he lifts the stone fist and rolls it, flexing the chain
anim('knuckles', 2, 'once', mix(fn(idleOf(2, 0.5)), fn(keyTurn(2)), posed([
  [0, {}],
  [0.25, { arm_left: { rotation: V(38, 0, -6) }, forearm_left: { rotation: V(52, 0, 0) }, neck: { rotation: V(0, -10, 0) }, head: { rotation: V(4, -6, 0) } }],
  [0.9, { arm_left: { rotation: V(38, 0, -6) }, forearm_left: { rotation: V(52, 0, 0) }, fist_left: { rotation: V(0, -60, 0) }, neck: { rotation: V(0, -10, 0) }, head: { rotation: V(4, -6, 0) } }],
  [1.3, { arm_left: { rotation: V(38, 0, -6) }, forearm_left: { rotation: V(52, 0, 0) }, fist_left: { rotation: V(0, 30, 0) }, neck: { rotation: V(0, -10, 0) }, head: { rotation: V(4, -6, 0) } }],
  [1.6, {}],
])));

// 5. sigh (2 s): shoulders rise and fall, both pipes puff, the beard drops a little
anim('sigh', 2, 'once', mix(fn(keyTurn(2)), posed([
  [0, {}],
  [0.2, { waist: { rotation: V(5, 0, 0) }, neck: { rotation: V(4, 0, 0) }, arm_right: { rotation: V(-4, 0, -3) }, arm_left: { rotation: V(-4, 0, 3) }, hips: { position: V(0, 0.8, 0) } }],
  [0.9, { waist: { rotation: V(-4, 0, 0) }, neck: { rotation: V(-7, 0, 0) }, arm_right: { rotation: V(3, 0, 2) }, arm_left: { rotation: V(3, 0, -2) }, jaw: { rotation: V(-6, 0, 0) }, hips: { position: V(0, -0.8, 0) }, brow_left: { rotation: V(0, 0, 8) }, brow_right: { rotation: V(0, 0, -8) } }],
  [1.6, {}],
]), fn({
  pipe_steam_left: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.85, 1.0, 1.3, 1.6)), position: (t) => V(0, 6 * lin(t, 0.85, 1.6), 0) },
  pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.9, 1.05, 1.35, 1.65)), position: (t) => V(0, 6 * lin(t, 0.9, 1.65), 0) },
})), { ground: true });

// 6. charge (2.2 s): head down, scrapes his left foot twice, lunges into a ram-run, skids and rocks back
{
  const L = 2.2, run = (t) => env(t, 0.55, 0.7, 1.55, 1.75), w = W2 / 0.5;
  const RAM = merge(FURY, { waist: { rotation: V(-14, 0, 0) }, neck: { rotation: V(8, 0, 0) }, arm_right: { rotation: V(28, 0, 6) }, arm_left: { rotation: V(28, 0, -6) }, forearm_right: { rotation: V(20, 0, 0) }, forearm_left: { rotation: V(20, 0, 0) } });
  anim('charge', L, 'once', mix(fn(keyTurn(L, 2)), posed([
    [0, {}],
    [0.05, merge(FURY, { neck: { rotation: V(-10, 0, 0) }, waist: { rotation: V(-6, 0, 0) } })],
    [0.55, RAM, 5, 6],
    [1.7, merge(FURY, { waist: { rotation: V(8, 0, 0) }, arm_right: { rotation: V(-10, 0, 8) }, arm_left: { rotation: V(-10, 0, -8) }, neck: { rotation: V(-6, 0, 0) } }), 5, 5],
    [1.95, {}, 4, 6],
  ]), fn({
    leg_left: { rotation: (t) => V(-16 * (env(t, 0.1, 0.17, 0.22, 0.3) + env(t, 0.3, 0.37, 0.42, 0.5)) + 20 * run(t) * sin(w * t), 0, 0) },
    leg_right: { rotation: (t) => V(-20 * run(t) * sin(w * t), 0, 0) },
    shin_left: { rotation: (t) => V(-10 * (env(t, 0.1, 0.17, 0.22, 0.3) + env(t, 0.3, 0.37, 0.42, 0.5)) - 30 * run(t) * Math.max(0, sin(w * t + PI + 0.9)), 0, 0) },
    shin_right: { rotation: (t) => V(-30 * run(t) * Math.max(0, sin(w * t + 0.9)), 0, 0) },
    foot_left: { rotation: (t) => V(Math.max(-13, Math.min(13, 20 * run(t) * sin(w * t) + 24 * run(t) * Math.max(0, sin(w * t + PI + 0.9)))), 0, 0) },
    foot_right: { rotation: (t) => V(Math.max(-13, Math.min(13, -20 * run(t) * sin(w * t) + 24 * run(t) * Math.max(0, sin(w * t + 0.9)))), 0, 0) },
    hips: { position: (t) => V(0, -2 * run(t) * (1 - cos(2 * w * t)) / 2, 0) },
    pipe_steam_left: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.45, 0.55, 0.8, 1.0)), position: (t) => V(0, 5 * lin(t, 0.45, 1), 3 * lin(t, 0.45, 1)) },
    pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.5, 0.6, 0.85, 1.05)), position: (t) => V(0, 5 * lin(t, 0.5, 1.05), 3 * lin(t, 0.5, 1.05)) },
    chain_end: { rotation: (t) => V(-30 * run(t) + 20 * decay(t, 1.7, 2), 0, 0) },
  })), { ground: true });
}

// 7. swing (1.4 s): twists back winding the stone fist, then a 120-degree sweep at chest height, overshoot and settle
anim('swing', 1.4, 'once', mix(fn(keyTurn(1.4)), posed([
  [0, {}],
  [0.02, merge(FURY, { waist: { rotation: V(0, 34, 0) }, arm_left: { rotation: V(-16, 0, -64) }, forearm_left: { rotation: V(18, 0, 0) }, neck: { rotation: V(0, -18, 0) }, hips: { rotation: V(0, 8, 0) } }), 5, 7],
  [0.42, merge(FURY, { waist: { rotation: V(0, -56, 0) }, arm_left: { rotation: V(22, 0, -70) }, forearm_left: { rotation: V(6, 0, 0) }, neck: { rotation: V(0, 14, 0) }, hips: { rotation: V(0, -10, 0) } }), 7, 5],
  [0.85, {}, 4, 6],
]), fn({ chain_end: { rotation: (t) => V(0, 0, -40 * env(t, 0.42, 0.55, 0.7, 1.1)) } })), { ground: true });

// 8. stomp (1.3 s) and 9. double stomp (2.0 s): a leg raised high, slammed down, the body squashes and the plates jump
function stompKeys(side, t0) {
  const s = side > 0 ? 'right' : 'left', o = side > 0 ? 'left' : 'right';
  const up = { [`leg_${s}`]: { rotation: V(45, 0, 0) }, [`shin_${s}`]: { rotation: V(-52, 0, 0) }, [`foot_${s}`]: { rotation: V(4, 0, 0) }, waist: { rotation: V(7, 0, 0) }, hips: { position: V(-3 * side, 0, 0), rotation: V(0, 0, 4 * side) }, arm_right: { rotation: V(-6, 0, 22) }, arm_left: { rotation: V(-6, 0, -22) }, [`leg_${o}`]: { rotation: V(0, 0, 0) } };
  const down = merge(FURY, { waist: { rotation: V(-7, 0, 0) }, hips: { position: V(0, -3, 0) }, arm_right: { rotation: V(6, 0, 6) }, arm_left: { rotation: V(6, 0, -6) }, neck: { rotation: V(-5, 0, 0) } });
  return [[t0 - 0.5, up, 4, 6], [t0, down, 9, 7], [t0 + 0.25, {}, 4, 5]];
}
const rattle = (t0, k = 1) => ({
  shield_right: { rotation: (t) => V(0, 0, 4 * k * decay(t, t0, 7, 5)) }, shield_left: { rotation: (t) => V(0, 0, -4 * k * decay(t, t0, 7, 5)) },
  pipe_left: { rotation: (t) => V(6 * k * decay(t, t0, 5, 4), 0, 0) }, pipe_right: { rotation: (t) => V(6 * k * decay(t, t0 + 0.02, 5, 4), 0, 0) },
  chain_end: { rotation: (t) => V(14 * k * decay(t, t0 + 0.03, 2.5, 3), 0, 0) }, jaw: { rotation: (t) => V(-3 * k * decay(t, t0, 6, 6), 0, 0) },
});
const ring = (t0) => ({ stomp_ring: { scale: (t) => { const u = lin(t, t0, t0 + 0.45); return t < t0 || u >= 1 ? NONE : V(0.4 + 1.6 * u, 1, 0.4 + 1.6 * u); } } });
anim('stomp', 1.3, 'once', mix(fn(keyTurn(1.3)), posed([[0, {}], ...stompKeys(1, 0.6)]), fn(rattle(0.6)), fn(ring(0.6))), { ground: true });
anim('stomp_double', 2.0, 'once', mix(fn(keyTurn(2.0)), posed([[0, {}], ...stompKeys(1, 0.6).slice(0, 2), ...stompKeys(-1, 1.15)]), fn(rattle(0.6)), fn(rattle(1.15)), fn(ring(0.6)), fn(ring(1.15))), { ground: true });

// 10. grab and throw (2.4 s): the gauntlet rises and opens (1 s), snaps shut, holds the catch up to his face, hurls it
{
  const OPEN = { finger_1: { rotation: V(34, 0, 0) }, finger_2: { rotation: V(36, 0, 0) }, finger_3: { rotation: V(32, 0, 0) }, thumb: { rotation: V(-28, 0, 0) } };
  const SHUT = { finger_1: { rotation: V(5, 0, 0) }, finger_2: { rotation: V(5, 0, 0) }, finger_3: { rotation: V(5, 0, 0) }, thumb: { rotation: V(-4, 0, 0) } };
  anim('grab_throw', 2.4, 'once', mix(fn(keyTurn(2.4)), posed([
    [0, {}],
    [0.05, merge(OPEN, { arm_right: { rotation: V(58, 0, 8) }, forearm_right: { rotation: V(14, 0, 0) }, head: { rotation: V(0, 10, 8) }, waist: { rotation: V(-4, 10, 0) } }), 3, 6],
    [1.0, merge(SHUT, FURY, { arm_right: { rotation: V(48, 0, 8) }, forearm_right: { rotation: V(16, 0, 0) }, waist: { rotation: V(-6, 6, 0) } }), 9, 7],
    [1.15, merge(SHUT, { arm_right: { rotation: V(84, -20, 0) }, forearm_right: { rotation: V(44, 0, 0) }, head: { rotation: V(4, 14, -6) }, waist: { rotation: V(4, 12, 0) } }), 4, 6],
    [1.5, merge(SHUT, { arm_right: { rotation: V(150, 0, 4) }, forearm_right: { rotation: V(30, 0, 0) }, waist: { rotation: V(10, 18, 0) } }), 6, 6],
    [1.72, merge(OPEN, { arm_right: { rotation: V(30, 0, 8) }, forearm_right: { rotation: V(6, 0, 0) }, waist: { rotation: V(-7, -12, 0) } }), 9, 6],
    [2.0, {}, 4, 6],
  ]), fn({ held_player: { scale: (t) => (t >= 1.02 && t < 1.78 ? ONE : NONE) } })), { ground: true });
  // 11. the grab blocked by a shield (1.2 s): the claw clangs shut on nothing; he recoils and shakes it
  anim('grab_blocked', 1.2, 'once', mix(fn(keyTurn(1.2)), posed([
    [0, {}],
    [0.02, merge(OPEN, { arm_right: { rotation: V(56, 0, 8) }, forearm_right: { rotation: V(14, 0, 0) } }), 6, 6],
    [0.4, merge(SHUT, { arm_right: { rotation: V(50, 0, 8) }, forearm_right: { rotation: V(16, 0, 0) } }), 10, 8],
    [0.5, merge(SHUT, { arm_right: { rotation: V(14, 0, 12) }, forearm_right: { rotation: V(30, 0, 0) }, waist: { rotation: V(5, -6, 0) }, brow_left: { rotation: V(0, 0, 8) }, brow_right: { rotation: V(0, 0, 4) } }), 5, 5],
    [0.95, {}, 4, 6],
  ]), fn({ claw_right: { rotation: (t) => V(0, 12 * decay(t, 0.55, 5, 4), 0) }, neck: { rotation: (t) => V(0, 8 * decay(t, 0.6, 4, 5), 0) } })), { ground: true });
}

// 12. spring leap (2.6 s): coils 1.5 s (the key spins up), launches stretched, arms raised together, lands fists-first
{
  const AIR = { waist: { rotation: V(6, 0, 0) }, arm_right: { rotation: V(150, 0, -16) }, arm_left: { rotation: V(150, 0, 16) }, forearm_right: { rotation: V(10, 0, 0) }, forearm_left: { rotation: V(10, 0, 0) }, leg_right: { rotation: V(-6, 0, 0) }, leg_left: { rotation: V(-6, 0, 0) }, shin_right: { rotation: V(-10, 0, 0) }, shin_left: { rotation: V(-10, 0, 0) } };
  const LAND = merge(COIL, FURY, { arm_right: { rotation: V(60, 0, 4) }, arm_left: { rotation: V(60, 0, -4) }, forearm_right: { rotation: V(10, 0, 0) }, forearm_left: { rotation: V(10, 0, 0) } });
  const air = (t) => (t > 1.55 && t < 2.1 ? Math.sin(PI * lin(t, 1.55, 2.1)) : 0);
  anim('leap', 2.6, 'once', mix(posed([
    [0, {}],
    [0.05, COIL, 2.2, 6],
    [1.5, AIR, 8, 6],
    [2.08, LAND, 7, 10],
    [2.25, {}, 2.4, 5],
  ]), fn({
    key: { rotation: (t) => V(0, 0, 180 * Math.pow(lin(t, 0, 1.5), 2) * 3 + (t > 1.5 ? 540 * Math.min(1, (t - 1.5) / 1.1) : 0)) },
    juggernaut: { position: (t) => V(0, 34 * air(t), -40 * smooth(lin(t, 1.55, 2.1))) },
    pipe_steam_left: { scale: (t) => V(1, 1, 1).map(() => env(t, 1.0, 1.2, 1.5, 1.8)) }, pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 1.05, 1.25, 1.55, 1.85)) },
  }), fn(rattle(2.1, 1.4)), fn({ stomp_ring: { scale: (t) => { const u = lin(t, 2.1, 2.6); return t < 2.1 || u >= 1 ? NONE : V(0.6 + 1.8 * u, 1, 0.6 + 1.8 * u); }, position: () => V(0, 0, -40) } })), { ground: (t) => (t < 1.55 || t > 2.08 ? 1 : 0) });
}

// 13. steam vent (3 s): plants his feet, shoulders back, the beard glows up for 2 s, then a blast through the beard
anim('steam_vent', 3, 'once', mix(fn(keyTurn(3)), posed([
  [0, {}],
  [0.05, { waist: { rotation: V(7, 0, 0) }, neck: { rotation: V(-4, 0, 0) }, arm_right: { rotation: V(-12, 0, 10) }, arm_left: { rotation: V(-12, 0, -10) }, hips: { position: V(0, -1, 0) }, jaw: { rotation: V(-4, 0, 0) } }, 2, 6],
  [2.0, merge(FURY, { waist: { rotation: V(-10, 0, 0) }, neck: { rotation: V(6, 0, 0) }, arm_right: { rotation: V(6, 0, 12) }, arm_left: { rotation: V(6, 0, -12) }, jaw: { rotation: V(-10, 0, 0) }, hips: { position: V(0, -1.5, 0) } }), 8, 6],
  [2.8, {}, 3, 6],
]), fn({
  beard_glow: { scale: (t) => (t < 0.2 || t > 2.75 ? NONE : V(1, Math.max(0.05, Math.min(1, lin(t, 0.2, 1.9) * (1 - lin(t, 2.5, 2.75)))), 1)) },
  vent_steam: { scale: (t) => { const u = env(t, 2.0, 2.15, 2.6, 2.9); return u < 0.03 ? NONE : V(u, u, u); }, position: (t) => V(0, 0, -6 * lin(t, 2.0, 2.9)) },
  slit_glow: { scale: (t) => V(1, 1 + 0.25 * env(t, 0.5, 1.9, 2.4, 2.7), 1) },

})), { ground: true });

// 14-16. gear spin: arms out and the torso winds back (start), spins like a governor at 2 turns a second (loop), and
// slows to a stagger (end)
{
  const spinArms = (k) => ({ arm_right: { rotation: (t) => V(0, 0, 8 * k(t)) }, arm_left: { rotation: (t) => V(0, 0, -8 * k(t)) } });
  anim('gear_spin_start', 2.0, 'once', mix(posed([[0, {}], [0.05, ARMS_OUT, 3, 6]]), fn({
    waist: { rotation: (t) => { const u = lin(t, 1.2, 2.0); return V(0, t < 1.2 ? -40 * seg(t, 0.2, 1.1) : -40 + 224 * u + 176 * u * u, 0); } },
    neck: { rotation: (t) => V(0, 18 * seg(t, 0.4, 1.2) * (1 - seg(t, 1.4, 2.0)), 0) },
    key: { rotation: (t) => V(0, 0, 180 * Math.pow(lin(t, 0, 2), 2) * 4) },
  }), fn(spinArms((t) => seg(t, 1.4, 2.0)))));
  anim('gear_spin', 1.0, 'loop', mix(fn({
    ...Object.fromEntries(Object.entries(ARMS_OUT).map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, () => v]))])),
  }), fn({
    waist: { rotation: (t) => V(8, 720 * t, 0) },
    key: { rotation: (t) => V(0, 0, 720 * t) },
    hips: { position: (t) => V(0, -0.5 * (1 - cos(W2 * 4 * t)) / 2, 0) },
    chain_end: { rotation: () => V(0, 0, -60) },
  }), fn(spinArms(() => 1))));
  anim('gear_spin_end', 1.5, 'once', mix(posed([[0, ARMS_OUT], [1.0, merge({ neck: { rotation: V(0, 0, 8) }, waist: { rotation: V(0, 0, 4) } }), 3, 5], [1.3, {}, 4, 6]]), fn({
    waist: { rotation: (t) => { const u = Math.min(1, t); return V(0, 720 * u - 360 * u * u, 0); } },
    key: { rotation: (t) => { const u = Math.min(1, t / 1.2); return V(0, 0, 720 * 1.2 * (u - u * u / 2)); } },
    slit_glow: { scale: (t) => V(1 + 0.15 * sin(W2 * 3 * t) * env(t, 0.9, 1.0, 1.2, 1.4), 1, 1) },
    hips: { rotation: (t) => V(0, 0, 3 * decay(t, 1.0, 1.5, 3)) },
  }), fn(spinArms((t) => 1 - seg(t, 0.3, 1.0)))), { ground: true });
}

// 17. cog throw (1.6 s): reaches back to the hip rack, takes a cog, throws it sidearm with a grunt of steam
anim('cog_throw', 1.6, 'once', mix(fn(keyTurn(1.6)), posed([
  [0, {}],
  [0.02, { arm_right: { rotation: V(-46, 0, 2) }, forearm_right: { rotation: V(40, 0, 0) }, waist: { rotation: V(0, -22, 0) }, neck: { rotation: V(0, 14, 0) } }, 4, 6],
  [0.55, { arm_right: { rotation: V(-30, 0, 30) }, forearm_right: { rotation: V(30, 0, 0) }, waist: { rotation: V(4, -30, 0) }, neck: { rotation: V(0, 20, 0) } }, 5, 6],
  [0.75, merge(FURY, { arm_right: { rotation: V(40, -8, 46) }, forearm_right: { rotation: V(10, 0, 0) }, waist: { rotation: V(-6, 34, 0) }, neck: { rotation: V(0, -14, 0) } }), 8, 6],
  [1.15, {}, 4, 6],
]), fn({
  cog_3: { scale: (t) => (t >= 0.45 && t < 1.5 ? NONE : ONE) },
  held_cog: { scale: (t) => (t >= 0.45 && t < 0.98 ? ONE : NONE) },
  pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.9, 1.0, 1.2, 1.45)) },
})), { ground: true });

// 18. running down (4 s, holds its last frame = the rewind pose): four ticks further and further apart, each one
// dropping him into a deeper slump and dimming the slit a step
{
  const TICKS = [0.4, 1.3, 2.3, 3.5];
  const steps = TICKS.map((t0, k) => [t0, merge(...Object.entries(REWIND).map(([b, p]) => ({ [b]: Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, ch === 'scale' ? v.map((x) => 1 + (x - 1) * (k + 1) / 4) : v.map((x) => (x * (k + 1)) / 4)])) }))), 7, 7]);
  anim('run_down', 4, 'once', mix(posed([[0, {}], ...steps]), fn({
    key: { rotation: (t) => V(0, 0, TICKS.reduce((a, t0, k) => a + (40 - 8 * k) * seg(t, t0, t0 + 0.12), 0) + 35 * lin(t, 0, 0.4)) },
  })), { ground: true });
}
const REWIND_KEY = 35 + 40 + 32 + 24 + 16; // where run_down leaves the key
// 19. rewinding (1 s loop): bowed and still, the key spinning hard, the slit pulsing faintly
anim('rewind', 1.0, 'loop', mix(fn(Object.fromEntries(Object.entries(REWIND).map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, () => v]))]))), fn({
  key: { rotation: (t) => V(0, 0, REWIND_KEY + 720 * t) },
  slit_glow: { scale: (t) => V(1, 1 + 0.6 * Math.max(0, sin(W2 * 2 * t)), 1) },
  waist: { rotation: (t) => V(0.4 * sin(W2 * 2 * t), 0, 0) },
})), { ground: true });
// 20. rewind end (1 s): straightens with a shudder, the slit back up, into the idle
anim('rewind_end', 1.0, 'once', mix(posed([[0, REWIND], [0.15, {}, 4, 5]]), fn({
  key: { rotation: (t) => V(0, 0, REWIND_KEY + 180 * (1 - Math.pow(1 - Math.min(1, t / 0.6), 2)) * 1) },
  waist: { rotation: (t) => V(2 * decay(t, 0.1, 8, 6), 0, 0) },
})), { ground: true });

// 21. feint (1.8 s): two ticks of a run-down with the slit still lit, then a sharp snap and a lunging swipe
anim('feint', 1.8, 'once', mix(posed([
  [0, {}],
  [0.4, merge(Object.fromEntries(Object.entries(REWIND).filter(([b]) => b !== 'slit_glow').map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, v.map((x) => x * 0.25)]))]))), 7, 7],
  [0.95, merge(Object.fromEntries(Object.entries(REWIND).filter(([b]) => b !== 'slit_glow').map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, v.map((x) => x * 0.5)]))]))), 7, 7],
  [1.2, merge(FURY, { waist: { rotation: V(-10, 8, 0) }, neck: { rotation: V(10, 0, 0) }, arm_right: { rotation: V(58, 0, 14) }, forearm_right: { rotation: V(4, 0, 0) }, slit_glow: { scale: V(1.1, 1.6, 1) } }), 10, 6],
  [1.45, {}, 4, 6],
]), fn({ key: { rotation: (t) => V(0, 0, 30 * seg(t, 0.4, 0.5) + 22 * seg(t, 0.95, 1.05) + 128 * seg(t, 1.2, 1.8)) } })), { ground: true });

// 22. key jam (1.2 s, from the rewind pose): the key catches and shudders, sparks at the pawl, he jerks twice
anim('key_jam', 1.2, 'once', mix(fn(Object.fromEntries(Object.entries(REWIND).map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, () => v]))]))), fn({
  key: { rotation: (t) => V(0, 0, REWIND_KEY + 8 * sin(W2 * 15 * t) * env(t, 0.05, 0.1, 0.9, 1.1)) },
  sparks: { scale: (t) => ((t > 0.1 && t < 0.3) || (t > 0.5 && t < 0.62) || (t > 0.8 && t < 0.88) ? V(1 + 0.5 * sin(t * 90), 1 + 0.5 * cos(t * 70), 1) : NONE) },
  waist: { rotation: (t) => V(-3 * (env(t, 0.15, 0.2, 0.25, 0.4) + env(t, 0.55, 0.6, 0.65, 0.8)), 0, 0) },
  head: { rotation: (t) => V(0, 0, 6 * env(t, 0.15, 0.22, 0.6, 0.9)) },
  brow_left: { rotation: (t) => V(0, 0, 10 * env(t, 0.15, 0.22, 0.6, 0.9)) }, brow_right: { rotation: (t) => V(0, 0, 8 * env(t, 0.15, 0.22, 0.6, 0.9)) },
  slit_glow: { scale: (t) => V(1, 1 + 1.5 * ((t * 13) % 1 < 0.5 ? 1 : 0) * env(t, 0.15, 0.2, 0.7, 0.9), 1) },
})), { ground: true });

// 23-24. shedding plates (1.6 s each): he reels back a step, the stage's plates spring off and tumble away, a roar of steam
function shed(name, plates) {
  const fling = (dir, k) => ({
    position: (t) => { const u = lin(t, 0.2, 1.3); return u <= 0 ? V() : V(dir[0] * 30 * u, 26 * u - 70 * u * u, dir[2] * 30 * u); },
    rotation: (t) => V(dir[2] * -260 * lin(t, 0.2, 1.3), 0, dir[0] * 200 * lin(t, 0.2, 1.3) + k * 40),
    scale: (t) => (t < 1.3 ? ONE : NONE),
  });
  const T = {};
  for (const [b, dir] of plates) T[b] = fling(dir, plates.length);
  anim(name, 1.6, 'once', mix(fn(keyTurn(1.6)), posed([
    [0, {}],
    [0.12, { waist: { rotation: V(9, 0, 0) }, hips: { position: V(0, 0, 3) }, arm_right: { rotation: V(-14, 0, 26) }, arm_left: { rotation: V(-14, 0, -26) }, neck: { rotation: V(-6, 0, 0) } }, 6, 5],
    [0.7, merge(FURY, { waist: { rotation: V(-4, 0, 0) }, jaw: { rotation: V(-14, 0, 0) }, neck: { rotation: V(10, 0, 0) }, arm_right: { rotation: V(10, 0, 18) }, arm_left: { rotation: V(10, 0, -18) } }), 6, 6],
    [1.3, FURY, 4, 6],
  ]), fn(T), fn({
    vent_steam: { scale: (t) => { const u = env(t, 0.75, 0.85, 1.1, 1.4); return u < 0.03 ? NONE : V(u, u, u); } },
    beard_glow: { scale: (t) => (t > 0.7 && t < 1.35 ? ONE : NONE) },
  })), { ground: true, skip: plates.map(([b]) => b) });
}
shed('shed_plates_2', [['chest_plate', [0, 0, -1]], ['shield_right', [1, 0, 0.2]], ['shield_left', [-1, 0, 0.2]]]);
shed('shed_plates_3', [['helm', [0.3, 0, -0.6]], ['forearm_guard_right', [1, 0, -0.5]], ['forearm_guard_left', [-1, 0, -0.5]], ['shin_guard_right', [0.7, 0, -1]], ['shin_guard_left', [-0.7, 0, -1]]]);

// 25. hit while wound (0.4 s): barely a shudder; his head ticks toward the blow, the brows narrow
anim('hit_wound', 0.4, 'once', mix(fn(keyTurn(0.4, 0.25)), fn({
  waist: { rotation: (t) => V(1.5 * decay(t, 0, 9, 9), 0, 0) },
  neck: { rotation: (t) => V(0, tick(8 * seg(t, 0.03, 0.12) * (1 - seg(t, 0.28, 0.4)), 4), 0) },
  brow_left: { rotation: (t) => V(0, 0, -6 * env(t, 0.03, 0.08, 0.25, 0.4)) }, brow_right: { rotation: (t) => V(0, 0, 6 * env(t, 0.03, 0.08, 0.25, 0.4)) },
  shield_right: { rotation: (t) => V(0, 0, 2 * decay(t, 0, 9, 7)) }, shield_left: { rotation: (t) => V(0, 0, -2 * decay(t, 0, 9, 7)) },
})), { ground: true });
// 26. hit while rewinding (0.6 s): a real flinch, the key wobbling on its axis, the slit flickering
anim('hit_open', 0.6, 'once', mix(fn(Object.fromEntries(Object.entries(REWIND).map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, () => v]))]))), fn({
  waist: { rotation: (t) => V(-6 * decay(t, 0, 3, 5), 0, 0) },
  neck: { rotation: (t) => V(-8 * decay(t, 0.03, 3, 5), 0, 0) },
  key: { rotation: (t) => V(6 * decay(t, 0, 6, 5), 0, REWIND_KEY + 432 * t) },
  slit_glow: { scale: (t) => V(1, t < 0.25 && (t * 30) % 2 < 1 ? 2.5 : 1, 1) },
})), { ground: true });
// 27. struck by lightning mid-rewind (1.2 s): arcs crawl over the key, he jolts upright and is straight back in the fight
anim('lightning', 1.2, 'once', mix(posed([[0, REWIND], [0.12, merge(FURY, { waist: { rotation: V(6, 0, 0) }, neck: { rotation: V(-6, 0, 0) }, slit_glow: { scale: V(1.2, 1.8, 1) } }), 9, 5], [0.7, {}, 4, 6]]), fn({
  arcs: { scale: (t) => (t < 0.5 && (t * 24) % 2 < 1.3 ? ONE : NONE) },
  key: { rotation: (t) => V(0, 0, REWIND_KEY + 540 * (1 - Math.pow(1 - Math.min(1, t / 0.6), 3))) },
})), { ground: true });

// 28. dormant (4 s loop): kneeling, fists down, head bowed; an ember of the slit flickers now and then; a wisp of steam
anim('dormant', 4, 'loop', mix(fn(Object.fromEntries(Object.entries(KNEEL).map(([b, p]) => [b, Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, () => v]))]))), fn({
  slit_glow: { scale: (t) => ((t > 1.3 && t < 1.42) || (t > 3.1 && t < 3.18) ? V(1, 0.3, 1) : NONE) },
  waist: { rotation: (t) => V(0.6 * sin((W2 * t) / 4), 0, 0) },
  pipe_steam_left: { scale: (t) => { const u = env(t, 2.0, 2.3, 2.6, 3.2); return u < 0.03 ? NONE : V(u * 0.6, u * 0.6, u * 0.6); }, position: (t) => V(0, 6 * lin(t, 2.0, 3.2), 0) },
})), { ground: true, kneel: true });

// 29. waking (3 s, from dormant): the slit lights, one heavy turn of the key, he pushes up joint by joint, stands, salutes
{
  const L = 3;
  const T = posed([
    [0, KNEEL],
    [0.8, merge(KNEEL, { waist: { rotation: V(6, 0, 0) }, neck: { rotation: V(8, 0, 0) }, slit_glow: { scale: V(1, 1, 1) } }), 6, 6],
    [1.2, merge({ leg_left: { rotation: V(30, 0, 0) }, shin_left: { rotation: V(-30, 0, 0) }, foot_left: { rotation: V(0, 0, 0) }, leg_right: { rotation: V(20, 0, 0) }, shin_right: { rotation: V(-46, 0, 0) }, foot_right: { rotation: V(14, 0, 0) } }, { waist: { rotation: V(-10, 0, 0) } }), 5, 6],
    [1.7, {}, 4, 5],
    [2.05, SALUTE, 6, 6],
    [2.6, {}, 4, 6],
  ]);
  // the push-up goes in clockwork steps
  for (const b of ['leg_left', 'leg_right', 'shin_left', 'shin_right', 'waist']) { const f = T[b].rotation; T[b].rotation = (t) => (t > 0.8 && t < 1.9 ? f(t).map((x) => tick(x, 6)) : f(t)); }
  T.slit_glow = { scale: (t) => (t < 0.2 ? NONE : t < 0.5 ? ((t * 20) % 2 < 1 ? V(1, 0.4, 1) : NONE) : ONE) };
  anim('wake', L, 'once', mix(T, fn({ key: { rotation: (t) => V(0, 0, 90 * seg(t, 0.55, 0.8) + 90 * seg(t, 1.9, 2.6)) } }), fn(rattle(1.75, 0.8))), { ground: true, kneel: true });
}
// 30. ritual wake (3 s): from a standing crouch where the T stood; he uncurls, spreads his arms, salutes
anim('wake_ritual', 3, 'once', mix(posed([
  [0, merge(COIL, { slit_glow: { scale: V(1, 0, 1) }, arm_right: { rotation: V(10, 0, -6) }, arm_left: { rotation: V(10, 0, 6) } })],
  [0.4, merge(COIL, { arm_right: { rotation: V(10, 0, -6) }, arm_left: { rotation: V(10, 0, 6) } }), 6, 6],
  [0.9, { waist: { rotation: V(10, 0, 0) }, neck: { rotation: V(-8, 0, 0) }, arm_right: { rotation: V(-10, 0, 58) }, arm_left: { rotation: V(-10, 0, -58) }, forearm_right: { rotation: V(-10, 0, 0) }, forearm_left: { rotation: V(-10, 0, 0) } }, 3, 5],
  [1.8, SALUTE, 6, 6],
  [2.5, {}, 4, 6],
]), fn({ key: { rotation: (t) => V(0, 0, 360 * seg(t, 0.2, 1.4)) }, pipe_steam_left: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.9, 1.0, 1.3, 1.6)) }, pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 0.95, 1.05, 1.35, 1.65)) } }), fn(rattle(1.1, 0.8))), { ground: true });

// 31. death (4 s): a last wind-down, the slit going out; one knee, then onto his fists; the key spins free backwards and
// the Mainspring bursts out of his chest, uncoiling and spinning; a last puff of steam
{
  const HALF = merge(...Object.entries(KNEEL).map(([b, p]) => ({ [b]: Object.fromEntries(Object.entries(p).map(([ch, v]) => [ch, ch === 'scale' ? V(1, 0, 1) : v.map((x) => x * 0.55)])) })));
  anim('death', 4, 'once', mix(posed([
    [0, {}],
    [0.3, merge(REWIND, { slit_glow: { scale: V(1, 0.6, 1) } }), 7, 7],
    [0.8, merge(REWIND, { slit_glow: { scale: V(1, 0.4, 1) } }), 7, 7],
    [1.4, merge(HALF, { slit_glow: { scale: V(1, 0, 1) } }), 4, 5],
    [2.2, merge(KNEEL, { waist: { rotation: V(-6, 0, 0) } }), 4, 5],
    [3.1, merge(KNEEL, { waist: { rotation: V(4, 0, 0) } }), 6, 5],
  ]), fn({
    key: { rotation: (t) => V(0, 0, 25 * seg(t, 0.3, 0.4) + 15 * seg(t, 0.8, 0.9) - 900 * Math.pow(lin(t, 2.6, 3.4), 0.6)) },
    mainspring: {
      scale: (t) => (t < 2.9 ? NONE : V(1 + 0.6 * lin(t, 2.9, 3.3), 1 + 0.6 * lin(t, 2.9, 3.3), 1)),
      position: (t) => { const u = lin(t, 2.9, 3.8); return V(4 * u, 22 * u - 40 * u * u, -26 * u); },
      rotation: (t) => V(0, 0, 720 * lin(t, 2.9, 3.8)),
    },
    pipe_steam_left: { scale: (t) => V(1, 1, 1).map(() => env(t, 3.35, 3.5, 3.7, 4.0)) }, pipe_steam_right: { scale: (t) => V(1, 1, 1).map(() => env(t, 3.4, 3.55, 3.75, 4.0)) },
  }), fn(rattle(2.2, 1.2))), { ground: true, kneel: true, skip: ['mainspring'] });
}

// 32. victory salute (2 s): stands straight and salutes over a fallen player, then back to scanning
anim('salute', 2, 'once', mix(fn(keyTurn(2)), posed([[0, {}], [0.2, SALUTE, 5, 6], [1.3, {}, 4, 6]])), { ground: true });

// pose sheet (one held pose a second, for review): rest, stage 2, stage 3, coil, gear spin, rewind, kneel
{
  const G2 = { chest_plate: { scale: NONE }, shield_right: { scale: NONE }, shield_left: { scale: NONE } };
  const G3 = merge(G2, { helm: { scale: NONE }, forearm_guard_right: { scale: NONE }, forearm_guard_left: { scale: NONE }, shin_guard_right: { scale: NONE }, shin_guard_left: { scale: NONE } });
  const SHEET = [{}, G2, G3, COIL, ARMS_OUT, REWIND, KNEEL, SALUTE];
  const bones = new Set(SHEET.flatMap((p) => Object.keys(p)));
  const T = {};
  for (const b of bones) {
    T[b] = {};
    for (const ch of new Set(SHEET.flatMap((p) => Object.keys(p[b] || {})))) {
      const id = ch === 'scale' ? ONE : V();
      T[b][ch] = (t) => (SHEET[Math.min(SHEET.length - 1, Math.floor(t + 1e-6))][b] || {})[ch] || id;
    }
  }
  anim('pose_sheet', SHEET.length - 0.05, 'once', T, { ground: (t) => 1, kneel: true });
}

module.exports = { animations, ALL };

// ---------- checks: self-clipping in every clip, and how high and low he goes ----------
if (process.env.CHECK !== '0') {
  for (const { name, T, length, skip } of ALL) {
    const hits = CL.clipCollisions(T, length, Number(process.env.CHECK_FPS || 8), 0.3, skip).filter((h) => h.depth > 0.3);
    let lo = [1e9, ''], hi = -1e9;
    for (let t = 0; t <= length + 1e-9; t += 0.25) { const e = CL.extent(T, t); if (e.lo[0] < lo[0]) lo = [e.lo[0], e.lo[1] + '@' + t]; hi = Math.max(hi, e.hi[0]); }
    console.error(name.padEnd(16), hits.length ? hits.slice(0, 4).map((h) => `${h.depth}px ${h.a}<>${h.b} @${h.t}`).join(' | ') : 'clean', `  y ${lo[0].toFixed(1)} (${lo[1]})..${hi.toFixed(1)}`);
  }
}
