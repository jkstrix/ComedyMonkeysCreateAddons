// Helper Bot's preview clips (brief: design/briefs/helper-bot.md, Motion Set). Loaded by helper-bot.clips.js.
const H = require('./helper-bot.clips.js');
const { solidsAt, armClip, ikArm, anim, keyedTracks, blendEnd, springStep, seg, lin, env, hopArc, V, addV, Z, ONE, NONE, PI, W2, DRIVE, DUCK, chainMatrix, M4, CHAIN, armOnTarget, blink, GRIP_AT, LEVERS } = H;
const sin = Math.sin;

// ---------- helpers ----------
const idleOf = (P) => {
  const W = W2 / P, n = P / 3; // the 3 s idle, with whole cycles fitted to P
  const w = (W2 / 3) * (Math.round(n) / n) * 1;
  const Ww = W2 / (P / Math.max(1, Math.round(n)));
  return {
    body: { rotation: (t) => V(1.5 * sin(Ww * t + PI / 2), 0, 2 * sin(Ww * t)), position: (t) => V(0, 0.2 * sin(2 * Ww * t), 0) },
    spring: { scale: (t) => V(1, 1 + 0.2 * sin(2 * Ww * t), 1) },
    head: { rotation: (t) => V(-1.0 * sin(Ww * t + PI / 2 - 0.6), 0, -1.2 * sin(Ww * t - 0.6)) },
    eye_left: { rotation: (t) => V(0, 0, 1.2 * sin(Ww * t - 1.1)) },
    eye_right: { rotation: (t) => V(0, 0, 1.2 * sin(Ww * t - 1.1)) },
    whistle: { rotation: (t) => V(-4 * sin(Ww * t + PI / 2 - 1.4), 0, -5 * sin(Ww * t - 1.4)) },
    whistle_cord: { rotation: (t) => V(-5 * sin(Ww * t + PI / 2 - 1.8), 0, -6 * sin(Ww * t - 1.8)) },
    whisker_left: { rotation: (t) => V(0, 0, 1.5 * sin(Ww * t - 1.0)) },
    whisker_right: { rotation: (t) => V(0, 0, 1.5 * sin(Ww * t - 1.0)) },
    arm_left: { rotation: (t) => V(2 * sin(Ww * t - 0.9), 0, -3 - 2.5 * sin(Ww * t - 0.9)) },
    arm_right: { rotation: (t) => V(2 * sin(Ww * t - 0.9), 0, 3 - 2.5 * sin(Ww * t - 0.9)) },
    forearm_left: { rotation: (t) => V(-3 * sin(Ww * t - 1.3), 0, 0) },
    forearm_right: { rotation: (t) => V(-3 * sin(Ww * t - 1.3), 0, 0) },
  };
};
const IDLE3 = idleOf(3), IDLE4 = idleOf(4), IDLE6 = idleOf(6);
// one channel of springy keys: [time, V, freq?, damp?]
const keyedV = (keys, f = 5, d = 6, delay = 0) => (t) => [0, 1, 2].map((i) => {
  let x = keys[0][1][i];
  for (let k = 1; k < keys.length; k++) x += (keys[k][1][i] - keys[k - 1][1][i]) * springStep(t, keys[k][0] + delay, keys[k][2] || f, keys[k][3] || d);
  return x;
});
const mix = (a, b, w) => a.map((x, i) => x + (b[i] - x) * w);
const ARM_INIT = { right: [DRIVE.arm_right.rotation[0], DRIVE.arm_right.rotation[1], DRIVE.forearm_right.rotation[0], DRIVE.arm_right.rotation[2]], left: [DRIVE.arm_left.rotation[0], DRIVE.arm_left.rotation[1], DRIVE.forearm_left.rotation[0], -DRIVE.arm_left.rotation[2]] };
// claws on targets: spec = { right: { target(t), w(t) }, left: ... }; mixes the IK solution over the clip's own arm tracks
function withIK(name, T, L, spec) {
  for (const side of Object.keys(spec)) {
    const { target, w = () => 1, init = ARM_INIT[side] } = spec[side];
    const sol = armOnTarget(side, target, T, L, w, init);
    if (sol.worst > 0.45) console.error(`  ${name}: ${side} claw misses its target by ${sol.worst.toFixed(2)} px`);
    const own = (T['arm_' + side] || {}).rotation || (() => Z), ownF = (T['forearm_' + side] || {}).rotation || (() => Z);
    const sz = side === 'right' ? 1 : -1;
    T['arm_' + side] = { ...(T['arm_' + side] || {}), rotation: (t) => { const s = sol.arm(t); return mix(own(t), [s[0], s[1], sz * (s[3] || 0)], w(t)); } };
    T['forearm_' + side] = { ...(T['forearm_' + side] || {}), rotation: (t) => { const s = sol.arm(t); return mix(ownF(t), [s[2], 0, 0], w(t)); } };
  }
  return T;
}
const lever = (side, push = () => 0) => (t) => addV(GRIP_AT(side), V(0, 0.4 * push(t), (side === 'left' ? -0.35 : -0.8) * push(t)));
// a point on a bone, in model space, at time t (for claws that follow moving parts)
const pointOn = (T, bone, p) => (t) => M4.apply(chainMatrix(T, CHAIN[bone], t), p);
// train rattle: small, whole-hertz shakes so it loops on any whole-second clip
const rattle = (k = 1) => ({
  body: { rotation: (t) => V(k * (0.35 * sin(W2 * 9 * t) + 0.2 * sin(W2 * 13 * t + 1)), 0, k * 0.3 * sin(W2 * 7 * t + 0.5)), position: (t) => V(0, k * 0.06 * sin(W2 * 11 * t), 0) },
  whistle: { rotation: (t) => V(k * 2 * sin(W2 * 9 * t - 0.8), 0, k * 1.5 * sin(W2 * 7 * t)) },
  whistle_cord: { rotation: (t) => V(k * 3 * sin(W2 * 9 * t - 1.4), 0, k * 2 * sin(W2 * 7 * t - 0.6)) },
});
const mergeExtra = (...xs) => {
  const o = {};
  for (const x of xs) for (const b of Object.keys(x)) for (const ch of Object.keys(x[b])) {
    o[b] = o[b] || {};
    const prev = o[b][ch], f = x[b][ch];
    o[b][ch] = prev ? (t) => addV(prev(t), f(t)) : f;
  }
  return o;
};
// close a loop: the last 0.25 s eases onto its own first frame
function loopClose(T, L) {
  const snap = {};
  for (const b of Object.keys(T)) for (const ch of Object.keys(T[b])) { snap[b] = snap[b] || {}; const v = T[b][ch](0); snap[b][ch] = () => v; }
  return blendEnd(T, L, snap);
}
const firstFrame = (T) => { const o = {}; for (const b of Object.keys(T)) for (const ch of Object.keys(T[b])) { o[b] = o[b] || {}; const v = T[b][ch](0); o[b][ch] = () => v; } return o; };
const lidsBlink = (times) => (t) => V(0, times.reduce((a, t0) => a + -2 * (seg(t, t0, t0 + 0.07) - seg(t, t0 + 0.09, t0 + 0.2)), 0), 0);
const blinkAt = (...times) => ({ lid_left: { position: lidsBlink(times) }, lid_right: { position: lidsBlink(times) } });
const pulse = (t, a, b) => env(t, a, a + (b - a) * 0.3, a + (b - a) * 0.5, b);
// a steam puff: pops at t0, rises and fades over 0.6 s
const puffs = (...times) => ({
  steam_puff: {
    scale: (t) => { let s = 0; for (const t0 of times) s = Math.max(s, t < t0 ? 0 : t > t0 + 0.6 ? 0 : Math.sin(PI * Math.min(1, (t - t0) / 0.6)) * (0.6 + 0.6 * lin(t, t0, t0 + 0.6))); return V(s, s, s); },
    position: (t) => { let y = 0; for (const t0 of times) if (t >= t0 && t <= t0 + 0.6) y = 2.2 * lin(t, t0, t0 + 0.6); return V(0, y, 0); },
  },
});
const SHOW = (...bones) => Object.fromEntries(bones.map((b) => [b, { scale: [[0, ONE]] }]));
const DRIVING_STEPS = SHOW('cap_train', 'schedule_card');
// a grip point's world position for the left claw holding the cord bead
const BEAD = [-7.5, 18.25, 1];
const CARD_AT = [2.5, 13.4, -5.1]; // just in front of the card in its clip

// driving pose keys (body lean, head counter-tilt), on the idle underneath
const DRIVE_KEYS = { body: { rotation: [[0, DRIVE.body.rotation]] }, head: { rotation: [[0, DRIVE.head.rotation]] } };
const withDrive = (keys) => {
  const k = { ...keys };
  for (const b of Object.keys(DRIVE_KEYS)) for (const ch of Object.keys(DRIVE_KEYS[b])) {
    k[b] = { ...(k[b] || {}) };
    if (!k[b][ch]) k[b][ch] = DRIVE_KEYS[b][ch];
    else if (k[b][ch][0][0] === 0 && k[b][ch][0][1] === Z) k[b][ch] = [[0, DRIVE_KEYS[b][ch][0][1]], ...k[b][ch].slice(1)];
  }
  return k;
};
const LEFT_REST = { arm: V(6, 0, -4), fore: V(28, 0, 0) }; // relaxed at his side while cruising

// ========== DRIVING ==========
// drive_ready (3 s loop): claws on both levers, leaning in, a glance at the watch pocket
let DRIVE_READY0;
{
  const L = 3;
  const glance = (t) => env(t, 0.7, 0.95, 1.5, 1.8);
  const T = keyedTracks(withDrive({}), {
    base: IDLE3,
    extra: mergeExtra(rattle(0.3), blinkAt(2.3), { head: { rotation: (t) => V(14 * glance(t), -10 * glance(t), 0) }, eye_left: { rotation: (t) => V(-4 * glance(t), 0, 0) }, eye_right: { rotation: (t) => V(-4 * glance(t), 0, 0) } }),
  });
  withIK('drive_ready', T, L, { right: { target: lever('right') }, left: { target: lever('left') } });
  loopClose(T, L);
  DRIVE_READY0 = firstFrame(T);
  anim('drive_ready', L, 'loop', T, DRIVING_STEPS);
}

// cruise (3 s loop): right claw on the throttle, left arm easy at his side, looking out, chin up, rattling along
let CRUISE0;
const CRUISE_LEAN = V(-12, 0, 0);
{
  const L = 3;
  const look = (t) => env(t, 0.3, 0.8, 1.6, 2.1);
  const chin = (t) => env(t, 2.0, 2.2, 2.5, 2.8);
  const T = keyedTracks({
    body: { rotation: [[0, CRUISE_LEAN]] },
    head: { rotation: [[0, V(6, 0, 0)]] },
    arm_left: { rotation: [[0, LEFT_REST.arm]] }, forearm_left: { rotation: [[0, LEFT_REST.fore]] },
  }, {
    base: IDLE3,
    extra: mergeExtra(rattle(1), blinkAt(1.2), {
      head: { rotation: (t) => V(-6 * chin(t), 28 * look(t), -4 * look(t)) },
      eye_left: { rotation: (t) => V(0, 0, 3 * look(t)) }, eye_right: { rotation: (t) => V(0, 0, 3 * look(t)) },
      whisker_left: { rotation: (t) => V(0, 0, -8 * chin(t)) }, whisker_right: { rotation: (t) => V(0, 0, 8 * chin(t)) },
      lid_left: { position: (t) => V(0, -0.6 * chin(t), 0) }, lid_right: { position: (t) => V(0, -0.6 * chin(t), 0) },
    }),
  });
  withIK('cruise', T, L, { right: { target: lever('right') } });
  loopClose(T, L);
  CRUISE0 = firstFrame(T);
  anim('cruise', L, 'loop', T, DRIVING_STEPS);
}

// throttle_up (2.4 s): nod, the train's pull rocks him back, he shoves both levers and leans in, then lets go with
// his left and settles into the cruise
{
  const L = 2.4;
  const pull = (t) => springStep(t, 0.35, 4, 4) * (1 - seg(t, 1.5, 2.2)); // inertia: pushed back as the train pulls away
  const shove = (t) => env(t, 0.55, 0.75, 1.2, 1.5);
  const T = keyedTracks(withDrive({
    body: { rotation: [[0, DRIVE.body.rotation], [0.6, V(-18, 0, 0), 4, 5], [1.6, CRUISE_LEAN, 3, 5]] },
    head: { rotation: [[0, DRIVE.head.rotation], [0.1, V(16, 0, 0), 7, 6], [0.35, V(10, 0, 0), 5, 6], [1.6, V(6, 0, 0)]] },
    lid_left: { position: [[0, Z], [0.1, V(0, -0.9, 0)], [1.6, Z]] }, lid_right: { position: [[0, Z], [0.1, V(0, -0.9, 0)], [1.6, Z]] },
    iris_left: { scale: [[0, ONE], [0.1, V(0.85, 0.85, 1)], [1.6, ONE]] }, iris_right: { scale: [[0, ONE], [0.1, V(0.85, 0.85, 1)], [1.6, ONE]] },
    arm_left: { rotation: [[0, Z], [1.5, LEFT_REST.arm, 4, 5]] }, forearm_left: { rotation: [[0, Z], [1.5, LEFT_REST.fore, 4, 5]] },
  }), {
    base: IDLE3,
    extra: mergeExtra(rattle(0.6), {
      body: { rotation: (t) => V(7 * pull(t), 0, 0) },
      whistle: { rotation: (t) => V(25 * pull(t), 0, 0) },
      whistle_cord: { rotation: (t) => V(35 * pull(t), 0, 0) },
    }),
  });
  const leftW = (t) => 1 - seg(t, 1.35, 1.6);
  withIK('throttle_up', T, L, { right: { target: lever('right', shove) }, left: { target: lever('left', shove), w: leftW } });
  anim('throttle_up', L, 'once', blendEnd(T, L, CRUISE0), DRIVING_STEPS);
}

// brake (2.4 s): the train slows: he pitches forward, grabs the brake with his left, both haul back, elbows out,
// rocks back and settles (into the station wait)
let WAIT0;
{
  const L = 3;
  const lurch = (t) => springStep(t, 0.15, 4, 4) * (1 - seg(t, 1.3, 2.0));
  const haul = (t) => env(t, 0.5, 0.75, 1.3, 1.7);
  const T = keyedTracks({
    body: { rotation: [[0, CRUISE_LEAN], [1.6, DRIVE.body.rotation, 3, 5]] },
    head: { rotation: [[0, V(6, 0, 0)], [1.6, DRIVE.head.rotation]] },
    arm_left: { rotation: [[0, LEFT_REST.arm]] }, forearm_left: { rotation: [[0, LEFT_REST.fore]] },
    lid_left: { position: [[0, Z], [0.15, V(0, 0.8, 0)], [1.0, V(0, -0.8, 0)], [1.8, Z]] }, lid_right: { position: [[0, Z], [0.15, V(0, 0.8, 0)], [1.0, V(0, -0.8, 0)], [1.8, Z]] },
  }, {
    base: IDLE3,
    extra: mergeExtra(rattle(0.5), {
      body: { rotation: (t) => V(-10 * lurch(t) + 6 * haul(t), 0, 0) },
      head: { rotation: (t) => V(-10 * lurch(t), 0, 0) },
      whistle: { rotation: (t) => V(-35 * lurch(t), 0, 0) },
      whistle_cord: { rotation: (t) => V(-45 * lurch(t), 0, 0) },
      whisker_left: { rotation: (t) => V(0, 0, -10 * haul(t)) }, whisker_right: { rotation: (t) => V(0, 0, 10 * haul(t)) },
    }),
  });
  const grab = (t) => seg(t, 0.12, 0.4);
  withIK('brake', T, L, { right: { target: lever('right', (t) => -haul(t)) }, left: { target: lever('left', (t) => -haul(t)), w: grab } });
  WAIT0 = DRIVE_READY0;
  anim('brake', L, 'once', blendEnd(T, L, DRIVE_READY0), DRIVING_STEPS);
}

// station_wait (6 s loop): the pocket-watch routine, then drumming a claw on the lever
{
  const L = 6;
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.9, V(10, 24, 0)], [2.0, V(12, 26, 8), 3, 5], [2.9, V(6, 34, -6)], [3.9, V(10, 22, 0)], [4.6, DRIVE.head.rotation]] },
    lid_left: { position: [[0, Z], [1.0, V(0, -0.7, 0)], [2.9, V(0, 0.5, 0)], [3.9, V(0, -0.9, 0)], [4.6, Z]] },
    lid_right: { position: [[0, Z], [1.0, V(0, -0.7, 0)], [2.9, V(0, 0.5, 0)], [3.9, V(0, -0.9, 0)], [4.6, Z]] },
    eye_left: { rotation: [[0, Z], [3.9, V(-6, 0, -5)], [4.6, Z]] }, eye_right: { rotation: [[0, Z], [3.9, V(-6, 0, 5)], [4.6, Z]] },
    watch_lid: { rotation: [[0, Z], [1.2, V(118, 0, 0), 9, 7], [4.0, Z, 9, 8]] },
  }), {
    base: IDLE6,
    extra: mergeExtra(rattle(0), {
      claw_right: { rotation: (t) => V(14 * Math.max(0, sin(W2 * 4 * (t - 4.9))) * env(t, 4.9, 5.0, 5.6, 5.75), 0, 0) },
      watch_held: { rotation: (t) => V(0, 0, 10 * sin(W2 * 6 * (t - 2.0)) * env(t, 2.0, 2.05, 2.35, 2.45)) },
    }),
  });
  // left claw: to the pocket, out in front of his eyes, a shake, up to his ear (the side of his left pod), back, pocket
  const pocket = [-2.25, 13.75, -5.9];
  const eyes = [-7.2, 17.2, -6.2]; // held up beside his left pod (the levers are in front of his face)
  const ear = [-8.6, 18.5, -2.5];
  const tgt = keyedV([[0, GRIP_AT('left')], [0.3, pocket, 4, 6], [0.9, eyes, 3.5, 6], [2.9, ear, 3.5, 6], [3.6, eyes, 3.5, 6], [4.3, pocket, 4, 6], [4.75, GRIP_AT('left'), 4, 6]]);
  withIK('station_wait', T, L, { right: { target: lever('right') }, left: { target: tgt } });
  loopClose(T, L);
  anim('station_wait', L, 'loop', T, { ...DRIVING_STEPS, watch_pocket: { scale: [[0, ONE], [0.75, NONE], [4.4, ONE]] }, watch_held: { scale: [[0, NONE], [0.75, ONE], [4.4, NONE]] } });
}

// depart (2.8 s): a satisfied nod, two tugs on the whistle cord (toot-toot), a wave out of the side window, back to cruising
{
  const L = 2.8;
  const tug = (t) => pulse(t, 0.6, 0.85) + pulse(t, 0.95, 1.2);
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.15, V(18, 0, 0), 7, 6], [0.4, V(4, 0, 0)], [1.3, V(2, 26, -6)], [2.2, V(6, 0, 0)]] },
    body: { rotation: [[0, DRIVE.body.rotation], [2.2, CRUISE_LEAN]] },
    whisker_left: { rotation: [[0, Z], [0.15, V(0, 0, -12)], [2.2, Z]] }, whisker_right: { rotation: [[0, Z], [0.15, V(0, 0, 12)], [2.2, Z]] },
    arm_left: { rotation: [[0, Z], [1.35, V(-15, 25, -100), 5, 5], [2.15, LEFT_REST.arm, 4, 6]] },
    forearm_left: { rotation: [[0, Z], [1.35, V(0, 0, -40), 5, 5], [2.15, LEFT_REST.fore, 4, 6]] },
  }), {
    base: IDLE3,
    extra: mergeExtra(rattle(0.3), puffs(0.68, 1.03), {
      whistle_cord: { position: (t) => V(0, -1.4 * tug(t), 0) },
      whistle: { rotation: (t) => V(0, 0, 6 * sin(W2 * 14 * t) * env(t, 0.65, 0.7, 1.25, 1.35)) },
      forearm_left: { rotation: (t) => V(0, 0, -28 * sin(W2 * 2.6 * (t - 1.5)) * env(t, 1.45, 1.6, 1.95, 2.1)) },
    }),
  });
  // the left claw goes to the cord's bead and pulls it down with it
  const bead = pointOn(T, 'whistle_cord', BEAD);
  const onCord = (t) => env(t, 0.3, 0.55, 1.2, 1.35);
  const tgt = (t) => mix(GRIP_AT('left'), addV(bead(t), V(-0.9, -0.4, -0.5)), onCord(t));
  withIK('depart', T, L, { right: { target: lever('right') }, left: { target: tgt, w: (t) => 1 - seg(t, 1.25, 1.4) } });
  anim('depart', L, 'once', blendEnd(T, L, CRUISE0), DRIVING_STEPS);
}

// honk (1.2 s, layered over anything in game): a quick tug of the cord, one puff
{
  const L = 1.2;
  const tug = (t) => pulse(t, 0.4, 0.7);
  const T = keyedTracks(withDrive({}), {
    base: IDLE3,
    extra: mergeExtra(rattle(0.3), puffs(0.48), {
      whistle_cord: { position: (t) => V(0, -1.4 * tug(t), 0) },
      whistle: { rotation: (t) => V(0, 0, 6 * sin(W2 * 14 * t) * env(t, 0.45, 0.5, 0.75, 0.85)) },
      whisker_left: { rotation: (t) => V(0, 0, -6 * tug(t)) }, whisker_right: { rotation: (t) => V(0, 0, 6 * tug(t)) },
    }),
  });
  const bead = pointOn(T, 'whistle_cord', BEAD);
  const onCord = (t) => env(t, 0.1, 0.35, 0.8, 1.05);
  withIK('honk', T, L, { right: { target: lever('right') }, left: { target: (t) => mix(GRIP_AT('left'), addV(bead(t), V(-0.9, -0.4, -0.5)), onCord(t)) } });
  anim('honk', L, 'once', blendEnd(T, L, DRIVE_READY0), DRIVING_STEPS);
}

// no_path (4.2 s): takes the schedule card, reads it, turns it round, turns it upside down, looks at the controls
// and back, a big shrug, card back in its clip, hands back on the levers
{
  const L = 4.6;
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.7, V(16, 10, 0)], [1.4, V(14, 12, 10)], [2.1, V(16, 14, -14), 4, 5], [2.6, V(24, 0, 0)], [2.95, V(16, 12, -16)], [3.3, V(4, 0, 12), 4, 5], [4.1, DRIVE.head.rotation]] },
    card_held: { rotation: [[0, Z], [1.3, V(0, 180, 0), 3, 6], [2.0, V(0, 180, 180), 3, 6], [3.75, Z, 6, 7]] },
    eye_left: { rotation: [[0, Z], [2.0, V(0, 0, 10)], [3.3, V(0, 0, 14)], [4.0, Z]] }, eye_right: { rotation: [[0, Z], [2.0, V(0, 0, 4)], [3.3, V(0, 0, -6)], [4.0, Z]] },
    lid_left: { position: [[0, Z], [2.0, V(0, -1, 0)], [3.3, V(0, 0.6, 0)], [4.0, Z]] }, lid_right: { position: [[0, Z], [2.0, V(0, 0.6, 0)], [3.3, V(0, 0.6, 0)], [4.0, Z]] },
    iris_left: { scale: [[0, ONE], [2.0, V(0.8, 0.8, 1)], [2.6, V(1.2, 1.2, 1)], [4.0, ONE]] }, iris_right: { scale: [[0, ONE], [2.0, V(0.8, 0.8, 1)], [2.6, V(1.2, 1.2, 1)], [4.0, ONE]] },
    whistle: { rotation: [[0, Z], [3.3, V(40, 0, 10), 3, 4], [4.1, Z]] },
    whisker_left: { rotation: [[0, Z], [3.3, V(0, 0, 14)], [4.1, Z]] }, whisker_right: { rotation: [[0, Z], [3.3, V(0, 0, -14)], [4.1, Z]] },
    body: { rotation: [[0, DRIVE.body.rotation], [3.3, V(-6, 0, 0), 4, 5], [4.1, DRIVE.body.rotation]], position: [[0, Z], [3.3, V(0, 0.6, 0), 5, 5], [3.8, Z]] },
  }), { base: IDLE3, extra: rattle(0) });
  const read = [2.5, 16.5, -8.5];
  const shrugR = [8.5, 14.5, -4.5], shrugL = [-8.5, 14.5, -4.5];
  const tR = keyedV([[0, GRIP_AT('right')], [0.25, addV(CARD_AT, V(0, 0, -0.8)), 4, 6], [0.75, read, 3.5, 6], [3.2, shrugR, 4, 5], [3.7, addV(CARD_AT, V(0, 0, -0.8)), 4, 6], [4.05, GRIP_AT('right'), 4, 6]]);
  const tL = keyedV([[0, GRIP_AT('left')], [3.2, shrugL, 4, 5], [3.95, GRIP_AT('left'), 4, 6]]);
  withIK('no_path', T, L, { right: { target: tR }, left: { target: tL } });
  anim('no_path', L, 'once', blendEnd(T, L, DRIVE_READY0), { cap_train: { scale: [[0, ONE]] }, schedule_card: { scale: [[0, ONE], [0.45, NONE], [3.75, ONE]] }, card_held: { scale: [[0, NONE], [0.45, ONE], [3.75, NONE]] } });
}

// manual_folded (3 s loop): a player is driving: he's let go, swivelled 20° away, arms folded, cross, a huff
// arms folded: each claw tucked by the other elbow, the right forearm stacked above the left, both in front of his
// belly; searched over shoulder pitch, yaw and raise and forearm bend and twist, with clipping as a hard cost
function solveFold(side, target) {
  const sg = side === 'right' ? 1 : -1, solids = solidsAt({}, 0), Mb = chainMatrix({}, CHAIN.body, 0);
  const cost = (q) => {
    const T = { ['arm_' + side]: { rotation: () => V(q[0], q[1], sg * q[2]) }, ['forearm_' + side]: { rotation: () => V(q[3], q[4], 0) } };
    const p = M4.apply(chainMatrix(T, CHAIN['claw_' + side], 0), [6 * sg, 4, 0]);
    return Math.hypot(p[0] - target[0], p[1] - target[1], p[2] - target[2]) + 10 * Math.max(0, armClip(side, T, 0, solids, Mb)[0] - 0.02);
  };
  let best = [1e9, null];
  for (let a = 0; a <= 90; a += 10) for (let y = -40; y <= 40; y += 10) for (let z = 0; z <= 30; z += 10) for (let fx = 50; fx <= 130; fx += 10) for (let fy = -80; fy <= 80; fy += 10) {
    const q = [a, y, z, fx, fy], c = cost(q); if (c < best[0]) best = [c, q];
  }
  for (const step of [5, 2, 1, 0.5]) {
    let improved = true;
    while (improved) { improved = false; for (let k = 0; k < 5; k++) for (const d of [-step, step]) { const q = [...best[1]]; q[k] += d; const c = cost(q); if (c < best[0]) { best = [c, q]; improved = true; } } }
  }
  console.error('fold ' + side, best[1].map(Math.round), 'cost ' + best[0].toFixed(2));
  const q = best[1];
  return { arm: V(q[0], q[1], sg * q[2]), fore: V(q[3], q[4], 0) };
}
const FOLD = (() => {
  const r = solveFold('right', [-4.6, 11.4, -6.6]), l = solveFold('left', [4.6, 9.1, -6.7]);
  return { arm_right: r.arm, forearm_right: r.fore, arm_left: l.arm, forearm_left: l.fore };
})();
{
  const L = 3;
  const huff = (t) => env(t, 1.4, 1.55, 1.75, 2.1);
  const T = keyedTracks({
    swivel: { rotation: [[0, V(0, -20, 0)]] },
    head: { rotation: [[0, V(8, 12, 0)]] },
    lid_left: { position: [[0, V(0, -1.1, 0)]], rotation: [[0, V(0, 0, -12)]] }, lid_right: { position: [[0, V(0, -1.1, 0)]], rotation: [[0, V(0, 0, 12)]] },
    eye_left: { rotation: [[0, V(-6, 0, -5)]] }, eye_right: { rotation: [[0, V(-6, 0, 5)]] },
    arm_left: { rotation: [[0, FOLD.arm_left]] }, arm_right: { rotation: [[0, FOLD.arm_right]] }, forearm_left: { rotation: [[0, FOLD.forearm_left]] }, forearm_right: { rotation: [[0, FOLD.forearm_right]] },
  }, {
    base: IDLE3,
    extra: mergeExtra(rattle(0.4), {
      body: { position: (t) => V(0, 0.4 * huff(t), 0), rotation: (t) => V(-3 * huff(t), 0, 0) },
      whisker_left: { rotation: (t) => V(0, 0, -16 * huff(t)) }, whisker_right: { rotation: (t) => V(0, 0, 16 * huff(t)) },
      head: { rotation: (t) => V(0, 6 * sin(W2 * 5 * (t - 1.5)) * env(t, 1.5, 1.55, 1.8, 1.9), 0) },
    }),
  });
  anim('manual_folded', L, 'loop', loopClose(T, L), DRIVING_STEPS);
}

// paused_point (3 s loop): nobody's at the controls and the schedule is paused: taps on the controls, points at his
// schedule card, looks round at you expectantly
{
  const L = 3;
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.2, V(22, 0, 0)], [1.1, V(22, -10, 0)], [1.8, V(0, 34, -6), 4, 5], [2.6, DRIVE.head.rotation]] },
    lid_left: { position: [[0, Z], [1.8, V(0, 0.7, 0)], [2.6, Z]] }, lid_right: { position: [[0, Z], [1.8, V(0, 0.7, 0)], [2.6, Z]] },
    iris_left: { scale: [[0, ONE], [1.8, V(1.2, 1.2, 1)], [2.6, ONE]] }, iris_right: { scale: [[0, ONE], [1.8, V(1.2, 1.2, 1)], [2.6, ONE]] },
  }), { base: IDLE3 });
  const desk = addV(LEVERS.left.tip, V(0, 0.9, 0.9)), up = addV(LEVERS.left.tip, V(0, 2.4, 1.3));
  const tL = keyedV([[0, GRIP_AT('left')], [0.15, up, 6, 6], [0.3, desk, 9, 9], [0.45, up, 9, 9], [0.6, desk, 9, 9], [0.8, GRIP_AT('left'), 5, 6]]);
  const tR = keyedV([[0, GRIP_AT('right')], [1.0, addV(CARD_AT, V(0.5, 0.3, -2.2)), 4, 6], [2.3, GRIP_AT('right'), 4, 6]]);
  withIK('paused_point', T, L, { left: { target: tL }, right: { target: tR } });
  anim('paused_point', L, 'loop', loopClose(T, L), DRIVING_STEPS);
}

// crash (3.4 s): thrown forward into the levers, cap knocked askew, eyes spinning, a head shake, slumps into the sulk
let SULK0;
const SULK = {
  body: { rotation: V(-4, 0, 3), position: V(0, -0.5, 0) },
  head: { rotation: V(14, -10, 6) },
  eye_left: { rotation: V(0, 0, 12) }, eye_right: { rotation: V(0, 0, -12) },
  lid_left: { position: V(0, -1, 0) }, lid_right: { position: V(0, -1, 0) },
  whistle: { rotation: V(50, 0, 0) },
  whisker_left: { rotation: V(0, 0, 14) }, whisker_right: { rotation: V(0, 0, -14) },
  arm_left: { rotation: FOLD.arm_left }, arm_right: { rotation: FOLD.arm_right }, forearm_left: { rotation: FOLD.forearm_left }, forearm_right: { rotation: FOLD.forearm_right },
  cap_train: { rotation: V(-8, 0, 16), position: V(0.6, -0.3, 0) },
};
const keysFrom = (pose) => Object.fromEntries(Object.entries(pose).map(([b, chs]) => [b, Object.fromEntries(Object.entries(chs).map(([ch, v]) => [ch, [[0, v]]]))]));
{
  const L = 4;
  const T = keyedTracks(keysFrom(SULK), {
    base: IDLE4,
    extra: {
      body: { position: (t) => V(0, -0.5 * env(t, 1.6, 2.0, 2.4, 3.2), 0) },
      lid_left: { position: (t) => V(0, -0.4 * env(t, 1.6, 2.0, 2.4, 3.2), 0) }, lid_right: { position: (t) => V(0, -0.4 * env(t, 1.6, 2.0, 2.4, 3.2), 0) },
      whistle: { rotation: (t) => V(8 * env(t, 1.6, 2.0, 2.4, 3.2), 0, 0) },
    },
  });
  loopClose(T, L);
  SULK0 = firstFrame(T);
  anim('sulk', L, 'loop', T, DRIVING_STEPS);
}
{
  const L = 3.4;
  const hit = (t) => springStep(t, 0.12, 5, 5) * (1 - seg(t, 0.9, 1.4));
  const dizzy = (t) => env(t, 0.35, 0.5, 1.9, 2.3);
  const shake = (t) => sin(W2 * 6 * (t - 2.0)) * env(t, 2.0, 2.05, 2.35, 2.45);
  const T = keyedTracks(withDrive({
    cap_train: { rotation: [[0, Z], [0.2, SULK.cap_train.rotation, 8, 5]], position: [[0, Z], [0.2, SULK.cap_train.position, 8, 5]] },
    lid_left: { position: [[0, Z], [0.15, V(0, 1, 0)], [0.5, Z]] }, lid_right: { position: [[0, Z], [0.15, V(0, 1, 0)], [0.5, Z]] },
  }), {
    base: IDLE3,
    extra: {
      body: { rotation: (t) => V(-18 * hit(t), 0, 4 * hit(t)) },
      head: { rotation: (t) => V(-16 * hit(t), 8 * shake(t), 10 * Math.sin(W2 * 1.6 * t) * dizzy(t)) },
      whistle: { rotation: (t) => V(-40 * hit(t), 0, 20 * Math.sin(W2 * 3 * t) * dizzy(t)) },
      whistle_cord: { rotation: (t) => V(-50 * hit(t), 0, 25 * Math.sin(W2 * 3 * t - 1) * dizzy(t)) },
      iris_left: { rotation: (t) => V(0, 0, 720 * lin(t, 0.35, 2.3) * (1 - 0)) }, iris_right: { rotation: (t) => V(0, 0, -720 * lin(t, 0.35, 2.3)) },
      eye_left: { rotation: (t) => V(0, 0, 8 * Math.sin(W2 * 2.2 * t) * dizzy(t)) }, eye_right: { rotation: (t) => V(0, 0, 8 * Math.sin(W2 * 2.2 * t + PI) * dizzy(t)) },
    },
  });
  const onLevers = (t) => 1 - seg(t, 2.45, 2.75);
  withIK('crash', T, L, { right: { target: lever('right'), w: onLevers }, left: { target: lever('left'), w: onLevers } });
  anim('crash', L, 'once', blendEnd(T, L, SULK0), DRIVING_STEPS);
}

// no_schedule (3 s loop): in control but nothing to do: looks round, holds out an open claw toward you, taps the empty clip
let NOSCHED0;
{
  const L = 3;
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.2, V(6, 26, 0)], [0.7, V(6, -24, 0)], [1.2, V(2, 30, -8), 4, 5], [2.2, V(20, 0, 0)], [2.7, DRIVE.head.rotation]] },
    lid_left: { position: [[0, Z], [1.2, V(0, 0.6, 0)], [2.2, Z]] }, lid_right: { position: [[0, Z], [1.2, V(0, 0.6, 0)], [2.2, Z]] },
    claw_right: { rotation: [[0, Z], [1.25, V(0, 0, 70), 5, 6], [2.0, Z]] },
  }), { base: IDLE3, extra: blinkAt(0.95) });
  const tR = keyedV([[0, GRIP_AT('right')], [1.15, [10, 13, -8], 4, 5], [2.0, addV(CARD_AT, V(0.4, 0.6, -1.6)), 4, 6], [2.15, addV(CARD_AT, V(0.4, 0.6, -0.9)), 9, 9], [2.3, addV(CARD_AT, V(0.4, 0.6, -1.6)), 9, 9], [2.65, GRIP_AT('right'), 5, 6]]);
  withIK('no_schedule', T, L, { left: { target: lever('left') }, right: { target: tR } });
  loopClose(T, L);
  NOSCHED0 = firstFrame(T);
  anim('no_schedule', L, 'loop', T, {});
}

// schedule_given (2.6 s): the card appears in his clip, he snaps upright, the cap pops on, a salute, proud, back to work
{
  const L = 2.6;
  const capPop = (t) => (t < 0.45 ? 0 : springStep(t, 0.45, 7, 4.5));
  const T = keyedTracks(withDrive({
    body: { rotation: [[0, DRIVE.body.rotation], [0.25, V(2, 0, 0), 6, 5], [2.0, DRIVE.body.rotation]], scale: [[0, ONE], [0.9, V(1.04, 1, 1.04)], [2.0, ONE]] },
    neck: { position: [[0, Z], [0.25, V(0, 1.2, 0), 7, 5], [1.9, Z]] },
    head: { rotation: [[0, DRIVE.head.rotation], [0.25, V(-6, 0, 0)], [2.0, DRIVE.head.rotation]] },
    lid_left: { position: [[0, Z], [0.2, V(0, 1, 0)], [0.9, V(0, -0.8, 0)], [2.0, Z]] }, lid_right: { position: [[0, Z], [0.2, V(0, 1, 0)], [0.9, V(0, -0.8, 0)], [2.0, Z]] },
    iris_left: { scale: [[0, ONE], [0.2, V(1.25, 1.25, 1)], [2.0, ONE]] }, iris_right: { scale: [[0, ONE], [0.2, V(1.25, 1.25, 1)], [2.0, ONE]] },
    whisker_left: { rotation: [[0, Z], [0.9, V(0, 0, -14)], [2.0, Z]] }, whisker_right: { rotation: [[0, Z], [0.9, V(0, 0, 14)], [2.0, Z]] },
    whistle: { rotation: [[0, Z], [0.25, V(-20, 0, 0), 5, 3], [1.9, Z]] },
    claw_right: { rotation: [[0, Z], [0.75, V(0, -30, -20)], [1.75, Z]] },
  }), { base: IDLE3 });
  // the salute: right claw up to the cap peak's side
  const tR = keyedV([[0, GRIP_AT('right')], [0.75, [8.3, 21.5, -4.6], 6, 6], [1.75, GRIP_AT('right'), 4, 6]]);
  withIK('schedule_given', T, L, { left: { target: lever('left'), w: (t) => 1 - env(t, 0.1, 0.3, 1.6, 1.95) }, right: { target: tR } });
  T.cap_train = { scale: (t) => { const s = capPop(t); return V(s, s, s); }, position: (t) => V(0, 1.5 * (1 - seg(t, 0.45, 0.7)) * (t > 0.45 ? 1 : 0), 0) };
  anim('schedule_given', L, 'once', blendEnd(T, L, DRIVE_READY0), { schedule_card: { scale: [[0, NONE], [0.2, ONE]] } });
}

// schedule_taken (2.2 s): the card goes, a disappointed look at the empty clip, the cap lifts off and pops away, sad
{
  const L = 2.4;
  const lift = (t) => seg(t, 0.8, 1.3);
  const T = keyedTracks(withDrive({
    head: { rotation: [[0, DRIVE.head.rotation], [0.35, V(26, 6, 0)], [1.1, V(12, 0, 0)], [1.9, DRIVE.head.rotation]] },
    eye_left: { rotation: [[0, Z], [0.5, V(0, 0, 12)], [1.9, Z]] }, eye_right: { rotation: [[0, Z], [0.5, V(0, 0, -12)], [1.9, Z]] },
    lid_left: { position: [[0, Z], [0.5, V(0, -1, 0)], [1.9, Z]] }, lid_right: { position: [[0, Z], [0.5, V(0, -1, 0)], [1.9, Z]] },
    whisker_left: { rotation: [[0, Z], [0.5, V(0, 0, 14)], [1.9, Z]] }, whisker_right: { rotation: [[0, Z], [0.5, V(0, 0, -14)], [1.9, Z]] },
    whistle: { rotation: [[0, Z], [0.5, V(45, 0, 0), 3, 4], [1.9, Z]] },
    body: { rotation: [[0, DRIVE.body.rotation], [0.5, V(-8, 0, 0)], [1.9, DRIVE.body.rotation]], position: [[0, Z], [0.5, V(0, -0.5, 0)], [1.9, Z]] },
  }), { base: IDLE3 });
  const off = (t) => env(t, 0.4, 0.7, 1.6, 2.0);
  withIK('schedule_taken', T, L, { left: { target: (t) => addV(GRIP_AT('left'), V(0, -2 * off(t), 2.5 * off(t))) }, right: { target: (t) => addV(GRIP_AT('right'), V(0, -2 * off(t), 2.5 * off(t))) } });
  T.cap_train = { position: (t) => V(0, 3 * lift(t), 0), scale: (t) => { const s = t < 0.8 ? 1 : 1 - seg(t, 1.1, 1.35); return V(s, s, s); } };
  anim('schedule_taken', L, 'once', blendEnd(T, L, NOSCHED0, ['cap_train']), { schedule_card: { scale: [[0, ONE], [0.2, NONE]] } });
}

// passenger (4 s loop): not in control: swivelled toward a window, watching the world go by, an occasional wave
{
  const L = 4;
  const wave = (t) => env(t, 2.1, 2.35, 3.2, 3.5);
  const T = keyedTracks({ swivel: { rotation: [[0, V(0, 75, 0)]] } }, {
    base: IDLE4,
    extra: mergeExtra(rattle(1), blinkAt(1.3), {
      head: { rotation: (t) => V(0, 18 * sin(W2 * 0.25 * t), 0) },
      eye_left: { rotation: (t) => V(0, 0, 2 * sin(W2 * 0.25 * t)) }, eye_right: { rotation: (t) => V(0, 0, 2 * sin(W2 * 0.25 * t)) },
      arm_right: { rotation: (t) => V(-10 * wave(t), 0, 110 * wave(t)) },
      forearm_right: { rotation: (t) => V(0, 0, (45 + 22 * sin(W2 * 2.6 * (t - 2.4))) * wave(t)) },
      lid_left: { position: (t) => V(0, 0.5 * wave(t), 0) }, lid_right: { position: (t) => V(0, 0.5 * wave(t), 0) },
    }),
  });
  anim('passenger', L, 'loop', loopClose(T, L), {});
}

// ========== SHOP ==========
let SHOP0;
const CLASP_R = [1.6, 10, -8.5], CLASP_L = [-1.6, 10, -8.5];
{
  const L = 3;
  const T = keyedTracks({ head: { rotation: [[0, V(-2, 0, 0)]] } }, { base: IDLE3, extra: blinkAt(1.7) });
  withIK('shop_idle', T, L, { right: { target: () => CLASP_R, init: [40, 30, 70] }, left: { target: () => CLASP_L, init: [40, -30, 70] } });
  loopClose(T, L);
  SHOP0 = firstFrame(T);
  anim('shop_idle', L, 'loop', T, SHOW('cap_logistics'));
}
{
  const L = 2.2;
  const T = keyedTracks({
    head: { rotation: [[0, V(-2, 0, 0)], [0.2, V(-6, 20, 0)], [1.5, V(-2, 0, 0)]] },
    lid_left: { position: [[0, Z], [0.2, V(0, 0.7, 0)], [1.5, Z]] }, lid_right: { position: [[0, Z], [0.2, V(0, 0.7, 0)], [1.5, Z]] },
    cap_logistics: { rotation: [[0, Z], [0.55, V(-14, 0, 0), 6, 5], [1.0, Z, 6, 5]], position: [[0, Z], [0.55, V(0, 0.7, 0), 6, 5], [1.0, Z]] },
    claw_left: { rotation: [[0, Z], [1.2, V(0, 0, 30)], [1.8, Z]] },
  }, {
    base: IDLE3,
    extra: { forearm_left: { rotation: (t) => V(0, 0, -24 * sin(W2 * 2.6 * (t - 1.15)) * env(t, 1.1, 1.2, 1.55, 1.7)) } },
  });
  const tR = keyedV([[0, CLASP_R], [0.35, [5.0, 23.9, -1.2], 5, 6], [1.0, CLASP_R, 4, 6]]);
  const tL = keyedV([[0, CLASP_L], [0.95, [-10, 17, -6], 5, 6], [1.75, CLASP_L, 4, 6]]);
  withIK('shop_greet', T, L, { right: { target: tR, init: [40, 30, 70] }, left: { target: tL, init: [40, -30, 70] } });
  anim('shop_greet', L, 'once', blendEnd(T, L, SHOP0), SHOW('cap_logistics'));
}
{
  const L = 1.6;
  const nod = (t) => hopArc(t, 0.15, 0.4) + hopArc(t, 0.45, 0.7);
  const bow = (t) => env(t, 0.85, 1.0, 1.15, 1.4);
  const T = keyedTracks({ head: { rotation: [[0, V(-2, 0, 0)]] } }, {
    base: IDLE3,
    extra: { head: { rotation: (t) => V(14 * nod(t) + 6 * bow(t), 0, 0) }, body: { rotation: (t) => V(-12 * bow(t), 0, 0) }, whisker_left: { rotation: (t) => V(0, 0, -6 * nod(t)) }, whisker_right: { rotation: (t) => V(0, 0, 6 * nod(t)) } },
  });
  withIK('shop_order_nod', T, L, { right: { target: () => CLASP_R, init: [40, 30, 70] }, left: { target: () => CLASP_L, init: [40, -30, 70] } });
  anim('shop_order_nod', L, 'once', blendEnd(T, L, SHOP0), SHOW('cap_logistics'));
}

// ========== OFF DUTY: idle extras, mannerisms, reactions ==========
const IDLE0 = (() => { const T = keyedTracks({}, { base: IDLE3 }); return firstFrame(T); })();
// check_watch (3 s): watch out, lid open, a glance, snap shut, pocket it
{
  const L = 3;
  const T = keyedTracks({
    head: { rotation: [[0, Z], [0.7, V(20, -8, 0)], [2.0, Z]] },
    watch_lid: { rotation: [[0, Z], [0.85, V(118, 0, 0), 9, 7], [1.7, Z, 9, 8]] },
    lid_left: { position: [[0, Z], [0.8, V(0, -0.6, 0)], [1.7, Z]] }, lid_right: { position: [[0, Z], [0.8, V(0, -0.6, 0)], [1.7, Z]] },
  }, { base: IDLE3 });
  const tL = keyedV([[0, [-6.5, 4, 0]], [0.25, [-2.25, 13.75, -5.9], 4, 6], [0.6, [-3, 15, -9.5], 4, 6], [1.8, [-2.25, 13.75, -5.9], 4, 6], [2.3, [-6.5, 4, 0], 4, 6]]);
  const w = (t) => env(t, 0.0, 0.25, 2.3, 2.7);
  withIK('check_watch', T, L, { left: { target: tL, w, init: [10, 0, 10] } });
  anim('check_watch', L, 'once', blendEnd(T, L, IDLE0), { watch_pocket: { scale: [[0, ONE], [0.5, NONE], [1.95, ONE]] }, watch_held: { scale: [[0, NONE], [0.5, ONE], [1.95, NONE]] } });
}
// polish_lens (2.5 s): right claw rubs the right lens in two circles, a double blink
{
  const L = 2.5;
  const T = keyedTracks({ head: { rotation: [[0, Z], [0.3, V(-4, 0, -8)], [1.8, Z]] }, eye_right: { rotation: [[0, Z], [0.4, V(0, 0, -6)], [1.8, Z]] } }, { base: IDLE3, extra: blinkAt(2.0, 2.25) });
  const lens = (t) => addV([4, 19.2, -6.4], V(0.9 * Math.cos(W2 * 1.6 * (t - 0.6)), 0.9 * Math.sin(W2 * 1.6 * (t - 0.6)), 0));
  const on = (t) => env(t, 0.1, 0.5, 1.75, 2.05);
  withIK('polish_lens', T, L, { right: { target: lens, w: on, init: [100, 20, 90] } });
  anim('polish_lens', L, 'once', blendEnd(T, L, IDLE0), {});
}
// toot_tune (3 s): three little half-tugs on the cord, three wisps, bouncing in time
{
  const L = 3;
  const tugs = [0.7, 1.15, 1.6];
  const tug = (t) => tugs.reduce((a, t0) => a + pulse(t, t0, t0 + 0.25), 0);
  const T = keyedTracks({ lid_left: { position: [[0, Z], [0.4, V(0, -0.5, 0)], [2.3, Z]] }, lid_right: { position: [[0, Z], [0.4, V(0, -0.5, 0)], [2.3, Z]] } }, {
    base: IDLE3,
    extra: mergeExtra(puffs(...tugs.map((x) => x + 0.05)), {
      whistle_cord: { position: (t) => V(0, -0.9 * tug(t), 0) },
      body: { position: (t) => V(0, 0.5 * tugs.reduce((a, t0) => a + hopArc(t, t0, t0 + 0.3), 0), 0) },
      head: { rotation: (t) => V(0, 0, 6 * sin(W2 * 1.1 * (t - 0.6)) * env(t, 0.6, 0.7, 1.9, 2.1)) },
    }),
  });
  const bead = pointOn(T, 'whistle_cord', BEAD);
  const on = (t) => env(t, 0.1, 0.5, 1.95, 2.35);
  withIK('toot_tune', T, L, { left: { target: (t) => addV(bead(t), V(-0.9, -0.4, -0.5)), w: on, init: [10, 0, 120] } });
  anim('toot_tune', L, 'once', blendEnd(T, L, IDLE0), {});
}
// adjust_cap (2 s, driving with the cap on): left claw lets go, sets the cap straight with a tug on the peak, back
{
  const L = 2;
  const T = keyedTracks(withDrive({ cap_train: { rotation: [[0, V(0, 0, 4)], [0.75, V(-6, 0, 0), 7, 6], [1.0, Z, 6, 6]] }, head: { rotation: [[0, DRIVE.head.rotation], [0.6, V(2, 0, 0)], [1.4, DRIVE.head.rotation]] } }), { base: IDLE3 });
  const tL = keyedV([[0, GRIP_AT('left')], [0.45, [-2.6, 22.9, -7.0], 5, 6], [0.75, [-2.6, 23.2, -7.5], 8, 7], [1.0, [-2.6, 22.9, -7.0], 8, 7], [1.45, GRIP_AT('left'), 4, 6]]);
  withIK('adjust_cap', T, L, { right: { target: lever('right') }, left: { target: tL } });
  anim('adjust_cap', L, 'once', blendEnd(T, L, DRIVE_READY0, ['cap_train']), DRIVING_STEPS);
}
// dust_controls (2.5 s, at the controls): a quick wipe of both lever knobs, a blow on them
{
  const L = 2.5;
  const blow = (t) => env(t, 1.6, 1.7, 1.9, 2.1);
  const T = keyedTracks(withDrive({ head: { rotation: [[0, DRIVE.head.rotation], [0.3, V(22, 0, 0)], [2.1, DRIVE.head.rotation]] } }), {
    base: IDLE3,
    extra: { whisker_left: { rotation: (t) => V(0, 0, -14 * blow(t)) }, whisker_right: { rotation: (t) => V(0, 0, 14 * blow(t)) }, body: { position: (t) => V(0, 0.4 * blow(t), 0), rotation: (t) => V(-4 * blow(t), 0, 0) } },
  });
  const wipe = (side, t0) => (t) => addV(GRIP_AT(side), V(0.8 * Math.cos(W2 * 3 * (t - t0)), 0.6 + 0.6 * Math.sin(W2 * 3 * (t - t0)), -0.2).map((v) => v * env(t, t0, t0 + 0.1, t0 + 0.55, t0 + 0.7)));
  withIK('dust_controls', T, L, { right: { target: wipe('right', 0.3) }, left: { target: wipe('left', 0.9) } });
  anim('dust_controls', L, 'once', blendEnd(T, L, DRIVE_READY0), DRIVING_STEPS);
}
// doze_off (6.8 s, off duty): lids sink, two nods, eyes go dark, a jolt awake and a look round
{
  const L = 6.8;
  const dip = (t, t0) => Math.sin(PI * lin(t, t0, t0 + 0.45));
  const wide = (t) => env(t, 4.5, 4.6, 5.4, 6.0);
  const look = (t) => -25 * (seg(t, 4.9, 5.1) - seg(t, 5.4, 5.6)) + 25 * (seg(t, 5.6, 5.8) - seg(t, 6.1, 6.4));
  const T = keyedTracks({
    lid_left: { position: [[0, Z], [0.4, V(0, -2, 0), 1.6, 3], [4.5, Z, 9, 6]] }, lid_right: { position: [[0, Z], [0.4, V(0, -2, 0), 1.6, 3], [4.5, Z, 9, 6]] },
    head: { rotation: [[0, Z], [0.4, V(14, 0, 0), 1.6, 3], [4.5, Z, 9, 6]] },
    whistle: { rotation: [[0, Z], [0.4, V(40, 0, 0), 1.6, 3], [4.5, Z, 9, 6]] },
    whisker_left: { rotation: [[0, Z], [0.4, V(0, 0, 10), 1.6, 3], [4.5, Z, 9, 6]] }, whisker_right: { rotation: [[0, Z], [0.4, V(0, 0, -10), 1.6, 3], [4.5, Z, 9, 6]] },
  }, {
    base: IDLE3,
    extra: {
      head: { rotation: (t) => V(7 * (dip(t, 1.2) + dip(t, 2.0)), look(t), 0) },
      post: { position: (t) => V(0, 1.2 * hopArc(t, 4.5, 4.8), 0) },
      neck: { position: (t) => V(0, 1 * wide(t), 0) },
      lid_left: { position: (t) => V(0, 1 * wide(t), 0) }, lid_right: { position: (t) => V(0, 1 * wide(t), 0) },
    },
  });
  anim('doze_off', L, 'once', blendEnd(T, L, IDLE0), {
    iris_glow_left: { scale: [[0, ONE], [2.2, NONE], [2.27, ONE], [2.36, NONE], [4.5, ONE]] },
    iris_glow_right: { scale: [[0, ONE], [2.3, NONE], [4.52, ONE]] },
  });
}
// sees_player (2.8 s): a glance, a double-take with his neck shooting up, swivels to face them, a wave
{
  const L = 2.8;
  const T = keyedTracks({
    head: { rotation: [[0, Z], [0.2, V(0, 30, 0), 6, 6], [0.45, Z, 6, 6], [0.75, V(-6, 34, 0), 9, 5], [1.15, V(-4, 0, 0), 4, 6], [2.2, Z]] },
    swivel: { rotation: [[0, Z], [1.0, V(0, 40, 0), 3, 5]] },
    neck: { position: [[0, Z], [0.75, V(0, 1.8, 0), 9, 5], [1.5, Z, 4, 5]] },
    lid_left: { position: [[0, Z], [0.75, V(0, 1, 0)], [1.5, V(0, 0.5, 0)], [2.3, Z]] }, lid_right: { position: [[0, Z], [0.75, V(0, 1, 0)], [1.5, V(0, 0.5, 0)], [2.3, Z]] },
    iris_left: { scale: [[0, ONE], [0.75, V(0.75, 0.75, 1)], [1.2, V(1.2, 1.2, 1)], [2.3, ONE]] }, iris_right: { scale: [[0, ONE], [0.75, V(0.75, 0.75, 1)], [1.2, V(1.2, 1.2, 1)], [2.3, ONE]] },
    whistle: { rotation: [[0, Z], [0.75, V(25, 0, 0), 6, 3], [1.4, V(-10, 0, 0)], [2.3, Z]] },
    whisker_left: { rotation: [[0, Z], [1.2, V(0, 0, -10)], [2.3, Z]] }, whisker_right: { rotation: [[0, Z], [1.2, V(0, 0, 10)], [2.3, Z]] },
    arm_right: { rotation: [[0, Z], [1.3, V(-10, 0, 115), 5, 5], [2.3, Z, 4, 6]] }, forearm_right: { rotation: [[0, Z], [1.3, V(0, 0, 45), 5, 5], [2.3, Z, 4, 6]] },
  }, { base: IDLE3, extra: { forearm_right: { rotation: (t) => V(0, 0, 22 * sin(W2 * 2.6 * (t - 1.45)) * env(t, 1.4, 1.55, 2.05, 2.2)) } } });
  // he stays turned toward them (in game the swivel follows the player)
  const target = { ...IDLE0, swivel: { rotation: () => V(0, 40, 0) } };
  anim('sees_player', L, 'once', blendEnd(T, L, target), {});
}
// wave (2 s): right-clicked off duty: a big happy wave
{
  const L = 2;
  const T = keyedTracks({
    arm_right: { rotation: [[0, Z], [0.2, V(-10, 0, 124), 5, 5], [1.55, Z, 4, 6]] }, forearm_right: { rotation: [[0, Z], [0.2, V(0, 0, 36), 5, 5], [1.55, Z, 4, 6]] },
    eye_left: { rotation: [[0, Z], [0.2, V(0, 0, 9)], [1.6, Z]] }, eye_right: { rotation: [[0, Z], [0.2, V(0, 0, -9)], [1.6, Z]] },
    lid_left: { position: [[0, Z], [0.2, V(0, 0.6, 0)], [1.6, Z]] }, lid_right: { position: [[0, Z], [0.2, V(0, 0.6, 0)], [1.6, Z]] },
    whistle: { rotation: [[0, Z], [0.2, V(-14, 0, 0)], [1.6, Z]] },
    whisker_left: { rotation: [[0, Z], [0.2, V(0, 0, -12)], [1.6, Z]] }, whisker_right: { rotation: [[0, Z], [0.2, V(0, 0, 12)], [1.6, Z]] },
    body: { rotation: [[0, Z], [0.2, V(-3, 0, -3)], [1.6, Z]] },
  }, { base: IDLE3, extra: { forearm_right: { rotation: (t) => V(0, 0, 24 * sin(W2 * 2.6 * (t - 0.35)) * env(t, 0.3, 0.45, 1.3, 1.5)) } } });
  anim('wave', L, 'once', blendEnd(T, L, IDLE0), {});
}
// hostile: shrink (1.6 s) → cower (2 s loop) → recover (2.2 s); never leaves his spot
const COWER = {
  post: { position: V(0, -2, 0) },
  spring: { scale: V(1, 0.4, 1) },
  neck: { position: V(0, -0.5, 0) },
  head: { rotation: V(10, 0, 0) },
  lid_left: { position: V(0, -1, 0) }, lid_right: { position: V(0, -1, 0) },
  iris_left: { scale: V(0.65, 0.65, 1) }, iris_right: { scale: V(0.65, 0.65, 1) },
  whistle: { rotation: V(70, 0, 0) },
  whisker_left: { rotation: V(0, 0, 12) }, whisker_right: { rotation: V(0, 0, -12) },
};
const lensFront = (side) => [side === 'right' ? 4 : -4, 18.6, -8.2];
let COWER0;
{
  const L = 2;
  const peek = (t) => env(t, 0.8, 1.0, 1.4, 1.6);
  const T = keyedTracks(keysFrom(COWER), {
    noIdle: true,
    extra: {
      body: { rotation: (t) => V(1.0 * sin(W2 * 11 * t), 0, 1.6 * sin(W2 * 9 * t)), position: (t) => V(0, 0.15 * sin(W2 * 13 * t), 0) },
      head: { rotation: (t) => V(1.0 * sin(W2 * 11 * t + 0.7) - 8 * peek(t), 10 * peek(t), 1.2 * sin(W2 * 9 * t + 0.5)) },
      lid_left: { position: (t) => V(0, 1.3 * peek(t), 0) },
      whistle: { rotation: (t) => V(5 * sin(W2 * 13 * t + 1), 0, 4 * sin(W2 * 9 * t + 1.3)) },
    },
  });
  const coverT = (side) => (t) => addV(pointOn(T, 'head', [0, 0, 0])(t), V(0, 0, 0)).map(() => 0) && lensFront(side);
  withIK('hostile_cower', T, L, {
    right: { target: (t) => addV(lensFront('right'), V(0, -2.2, 0)), init: [120, 20, 90] },
    left: { target: (t) => addV(lensFront('left'), V(-1.5 * peek(t), -2.2 - 1.5 * peek(t), 0)), init: [120, -20, 90] },
  });
  loopClose(T, L);
  COWER0 = firstFrame(T);
  anim('hostile_cower', L, 'loop', T, {});
}
{
  const L = 1.6;
  const T = keyedTracks({
    ...Object.fromEntries(Object.entries(COWER).map(([b, chs]) => [b, Object.fromEntries(Object.entries(chs).map(([ch, v]) => [ch, [[0, ch === 'scale' ? ONE : Z], [0.45, v, 4, 6]]]))])),
    neck: { position: [[0, Z], [0.15, V(0, 1.8, 0), 9, 5], [0.45, COWER.neck.position, 4, 6]] },
    lid_left: { position: [[0, Z], [0.15, V(0, 1, 0), 9, 5], [0.45, COWER.lid_left.position]] }, lid_right: { position: [[0, Z], [0.15, V(0, 1, 0), 9, 5], [0.45, COWER.lid_right.position]] },
  }, { base: IDLE3 });
  const arms = (t) => seg(t, 0.35, 0.9);
  withIK('hostile_shrink', T, L, {
    right: { target: () => addV(lensFront('right'), V(0, -2.2, 0)), w: arms, init: [60, 10, 60] },
    left: { target: () => addV(lensFront('left'), V(0, -2.2, 0)), w: arms, init: [60, -10, 60] },
  });
  anim('hostile_shrink', L, 'once', blendEnd(T, L, COWER0), {});
}
{
  const L = 2.2;
  const shake = (t) => env(t, 1.4, 1.45, 1.75, 1.85) * sin(W2 * 7 * (t - 1.4));
  const T = keyedTracks({
    ...Object.fromEntries(Object.entries(COWER).map(([b, chs]) => [b, Object.fromEntries(Object.entries(chs).map(([ch, v]) => [ch, [[0, v], [0.9, ch === 'scale' ? ONE : Z, 3, 5]]]))])),
    head: { rotation: [[0, COWER.head.rotation], [0.3, V(4, 20, 0)], [0.6, V(4, -20, 0)], [0.9, Z, 3, 5]] },
  }, { base: IDLE3, extra: { body: { rotation: (t) => V(0, 0, 6 * shake(t)) }, head: { rotation: (t) => V(0, 0, -4 * shake(t)) }, whistle: { rotation: (t) => V(0, 0, 15 * shake(t)) } } });
  const arms = (t) => 1 - seg(t, 0.15, 0.7);
  withIK('hostile_recover', T, L, {
    right: { target: () => addV(lensFront('right'), V(0, -2.2, 0)), w: arms, init: [120, 20, 90] },
    left: { target: () => addV(lensFront('left'), V(0, -2.2, 0)), w: arms, init: [120, -20, 90] },
  });
  anim('hostile_recover', L, 'once', blendEnd(T, L, IDLE0), {});
}
// rain_wipe (2.2 s): a claw sweeps across both lenses like a windscreen wiper, then a little shake
{
  const L = 2.2;
  const on = (t) => env(t, 0.1, 0.45, 1.45, 1.8);
  const sweep = (t) => Math.cos(W2 * 1.0 * (t - 0.45));
  const T = keyedTracks({ lid_left: { position: [[0, Z], [0.4, V(0, -0.8, 0)], [1.5, Z]] }, lid_right: { position: [[0, Z], [0.4, V(0, -0.8, 0)], [1.5, Z]] } }, {
    base: IDLE3, extra: { head: { rotation: (t) => V(0, 0, 6 * sin(W2 * 7 * (t - 1.7)) * env(t, 1.7, 1.75, 1.95, 2.05)) } },
  });
  const wiper = (side, ph) => (t) => [(side === 'right' ? 1 : -1) * (3.8 + 1.8 * Math.cos(W2 * 1.4 * (t - 0.45) + ph)), 18.4, -5.6];
  withIK('rain_wipe', T, L, { right: { target: wiper('right', 0), w: on, init: [110, 20, 90] }, left: { target: wiper('left', PI), w: on, init: [110, -20, 90] } });
  anim('rain_wipe', L, 'once', blendEnd(T, L, IDLE0), {});
}
// thunder_flinch (1.4 s): a jolt down on the gimbal, lids shut, arms flinch, then a nervous glance up
{
  const L = 1.4;
  const T = keyedTracks({
    post: { position: [[0, Z], [0.05, V(0, -1.2, 0), 9, 6], [0.5, Z, 4, 5]] },
    lid_left: { position: [[0, Z], [0.05, V(0, -2, 0), 9, 6], [0.5, V(0, 0.6, 0)], [1.1, Z]] }, lid_right: { position: [[0, Z], [0.05, V(0, -2, 0), 9, 6], [0.5, V(0, 0.6, 0)], [1.1, Z]] },
    arm_left: { rotation: [[0, Z], [0.05, V(-25, 0, -25), 9, 6], [0.6, Z]] }, arm_right: { rotation: [[0, Z], [0.05, V(-25, 0, 25), 9, 6], [0.6, Z]] },
    head: { rotation: [[0, Z], [0.05, V(10, 0, 0), 9, 6], [0.55, V(-22, 6, 0), 4, 5], [1.1, Z]] },
    whistle: { rotation: [[0, Z], [0.05, V(30, 0, 0), 6, 3], [0.7, Z]] },
  }, { base: IDLE3 });
  anim('thunder_flinch', L, 'once', blendEnd(T, L, IDLE0), {});
}

// ========== DUCKING ==========
const DUCK_ARMS = { arm_left: { rotation: FOLD.arm_left }, arm_right: { rotation: FOLD.arm_right }, forearm_left: { rotation: FOLD.forearm_left }, forearm_right: { rotation: FOLD.forearm_right } };
const DUCK_ALL = { ...DUCK, ...DUCK_ARMS };
let DUCKED0;
{
  const L = 3;
  const glance = (t) => env(t, 1.1, 1.35, 1.8, 2.1);
  const T = keyedTracks(keysFrom(DUCK_ALL), {
    base: IDLE3,
    extra: mergeExtra(blinkAt(2.5), { head: { rotation: (t) => V(-12 * glance(t), 0, 6 * glance(t)) }, eye_left: { rotation: (t) => V(-6 * glance(t), 0, 0) }, eye_right: { rotation: (t) => V(-6 * glance(t), 0, 0) } }),
  });
  loopClose(T, L);
  DUCKED0 = firstFrame(T);
  anim('idle_ducked', L, 'loop', T, SHOW('cap_train'));
}
const duckKeys = (from, to, t0) => {
  const k = {};
  for (const b of new Set([...Object.keys(from), ...Object.keys(to)])) for (const ch of new Set([...Object.keys(from[b] || {}), ...Object.keys(to[b] || {})])) {
    const zero = ch === 'scale' ? ONE : Z;
    k[b] = k[b] || {};
    k[b][ch] = [[0, (from[b] || {})[ch] || zero], [t0, (to[b] || {})[ch] || zero, 5, 6]];
  }
  return k;
};
const FOLD_AT = { right: [-2.2, 11.8, -6.9], left: [2.2, 9.4, -6.9] }, HANG_AT = { right: [6.6, 4, 0], left: [-6.6, 4, 0] };
function foldArms(name, T, L, from, to, t0) {
  const s = (t) => springStep(t, t0, 5, 6);
  const tgt = (side) => (t) => {
    const k = s(t), a = from === 'fold' ? FOLD_AT[side] : HANG_AT[side], b = to === 'fold' ? FOLD_AT[side] : HANG_AT[side];
    const local = addV(a.map((v, i) => v + (b[i] - v) * k), V(0, 1.5 * Math.sin(PI * Math.min(1, k)), -3.5 * Math.sin(PI * Math.min(1, k))));
    return pointOn(T, 'body', local)(t);
  };
  return withIK(name, T, L, { right: { target: tgt('right'), init: [10, 0, 10, 0] }, left: { target: tgt('left'), init: [10, 0, 10, 0] } });
}
{
  const L = 1.0;
  const noArms = Object.fromEntries(Object.entries(DUCK_ALL).filter(([b]) => !b.startsWith('arm') && !b.startsWith('forearm')));
  const T = keyedTracks(duckKeys({}, noArms, 0.1), { base: IDLE3 });
  foldArms('duck_down', T, L, 'hang', 'fold', 0.05);
  anim('duck_down', L, 'once', blendEnd(T, L, DUCKED0), SHOW('cap_train'));
}
{
  const L = 1.0;
  const noArms = Object.fromEntries(Object.entries(DUCK_ALL).filter(([b]) => !b.startsWith('arm') && !b.startsWith('forearm')));
  const T = keyedTracks(duckKeys(noArms, {}, 0.1), { base: IDLE3 });
  foldArms('duck_up', T, L, 'fold', 'hang', 0.1);
  anim('duck_up', L, 'once', blendEnd(T, L, IDLE0), SHOW('cap_train'));
}

// ========== EXPRESSIONS ==========
const POSES = {
  neutral: {},
  happy: { eye_left: V(0, 0, 10), eye_right: V(0, 0, -10), lidY: 0.6, iris: 1.2, whistle: V(-12, 0, 0), head: V(-4, 0, 0), neck: 0.5, whisk: -10 },
  proud: { head: V(-8, 0, 0), lidY: -0.8, whistle: V(-10, 0, 0), neck: 0.5, whisk: -14, puff: 1.04 },
  focused: { lidY: -1, eye_left: V(-5, 0, 0), eye_right: V(-5, 0, 0), iris: 0.9, head: V(5, 0, 0) },
  curious: { head: V(0, 0, 15), eye_left: V(0, 0, 6), eye_right: V(0, 0, -2), iris: 1.25, lidY: 0.4, whistle: V(0, 0, -15) },
  startled: { neck: 2, lidY: 1, iris: 0.7, whistle: V(25, 0, 0), arms: V(-20, 0, 20), body: V(5, 0, 0), whisk: -8 },
  sad: { eye_left: V(0, 0, 12), eye_right: V(0, 0, -12), lidY: -1, head: V(12, 0, 0), whistle: V(45, 0, 0), whisk: 14 },
  cross: { lidY: -1.2, lidRot: 14, head: V(10, 0, 0), eye_left: V(-6, 0, -6), eye_right: V(-6, 0, 6), whisk: -16 },
  confused: { head: V(0, 0, -14), eye_left: V(0, 0, 10), eye_right: V(0, 0, 4), lidL: -1, lidR: 0.6, iris: 0.85, whistle: V(0, 0, 30), whisk: 6 },
  dizzy: { lidY: -0.4, whisk: 8, whistle: V(20, 0, 0) },
};
function poseKeys(p, tIn, tOut, o = {}) {
  const k = {};
  const put = (b, ch, v, zero = Z) => { k[b] = k[b] || {}; k[b][ch] = [[0, zero], [tIn, v, o.freq, o.damp], [tOut, zero, o.outFreq || o.freq, o.damp]]; };
  if (p.head) put('head', 'rotation', p.head);
  if (p.neck) put('neck', 'position', V(0, p.neck, 0));
  if (p.eye_left) put('eye_left', 'rotation', p.eye_left);
  if (p.eye_right) put('eye_right', 'rotation', p.eye_right);
  const lL = p.lidL ?? p.lidY, lR = p.lidR ?? p.lidY;
  if (lL) put('lid_left', 'position', V(0, lL, 0));
  if (lR) put('lid_right', 'position', V(0, lR, 0));
  if (p.lidRot) { put('lid_left', 'rotation', V(0, 0, -p.lidRot)); put('lid_right', 'rotation', V(0, 0, p.lidRot)); }
  if (p.iris) { put('iris_left', 'scale', V(p.iris, p.iris, 1), ONE); put('iris_right', 'scale', V(p.iris, p.iris, 1), ONE); }
  if (p.whistle) put('whistle', 'rotation', p.whistle);
  if (p.whisk) { put('whisker_left', 'rotation', V(0, 0, p.whisk)); put('whisker_right', 'rotation', V(0, 0, -p.whisk)); }
  if (p.body) put('body', 'rotation', p.body);
  if (p.puff) put('body', 'scale', V(p.puff, 1, p.puff), ONE);
  if (p.arms) { put('arm_left', 'rotation', V(p.arms[0], 0, -p.arms[2])); put('arm_right', 'rotation', V(p.arms[0], 0, p.arms[2])); }
  return k;
}
function exprClip(name, L, pose, o) {
  const T = keyedTracks(poseKeys(pose, o.tIn, o.tOut, o), { base: IDLE3, extra: o.extra || {}, freq: o.freq, damp: o.damp });
  anim(name, L, 'once', blendEnd(T, L, IDLE0), o.steps || {});
}
exprClip('expr_happy', 3.3, POSES.happy, { tIn: 0.2, tOut: 2.5, extra: { post: { position: (t) => V(0, 0.8 * (hopArc(t, 0.4, 0.62) + hopArc(t, 0.68, 0.88)), 0) }, body: { rotation: (t) => V(0, 0, 3 * sin(W2 * 2 * (t - 0.95)) * env(t, 0.95, 1.1, 2.2, 2.4)) } } });
exprClip('expr_proud', 3.4, POSES.proud, { tIn: 0.25, tOut: 2.6, freq: 4, extra: { whisker_left: { rotation: (t) => V(0, 0, -4 * sin(W2 * 3 * (t - 1.2)) * env(t, 1.2, 1.3, 1.6, 1.8)) }, whisker_right: { rotation: (t) => V(0, 0, 4 * sin(W2 * 3 * (t - 1.2)) * env(t, 1.2, 1.3, 1.6, 1.8)) } } });
exprClip('expr_focused', 3.0, POSES.focused, { tIn: 0.2, tOut: 2.3 });
exprClip('expr_curious', 3.8, POSES.curious, { tIn: 0.25, tOut: 3.0, freq: 3.5, damp: 5, extra: { head: { rotation: (t) => V(0, 8 * sin(W2 * 0.5 * (t - 0.9)) * env(t, 0.6, 0.9, 2.8, 3.1), 0) } } });
exprClip('expr_startled', 3.0, POSES.startled, { tIn: 0.15, tOut: 2.1, freq: 9, damp: 6, outFreq: 3.5, extra: { whistle: { rotation: (t) => V(6 * sin(W2 * 7 * t) * env(t, 0.2, 0.3, 1.0, 1.5), 0, 0) } } });
exprClip('expr_sad', 4.4, POSES.sad, { tIn: 0.3, tOut: 3.5, freq: 2.5, damp: 4.5, extra: { body: { position: (t) => V(0, (0.5 * seg(t, 1.1, 1.5) - 1.1 * seg(t, 1.5, 2.3)) * (1 - seg(t, 3.4, 3.9)), 0) } } });
exprClip('expr_cross', 3.6, POSES.cross, { tIn: 0.2, tOut: 2.8, extra: { head: { rotation: (t) => V(0, t > 1.1 && t < 1.5 ? 6 * sin(W2 * 5 * (t - 1.1)) * (1 - seg(t, 1.35, 1.5)) : 0, 0) }, body: { position: (t) => V(0, 0.4 * (seg(t, 1.0, 1.15) - seg(t, 1.2, 1.5)), 0) } } });
exprClip('expr_confused', 3.8, POSES.confused, { tIn: 0.25, tOut: 3.0, freq: 4, extra: { iris_left: { scale: (t) => { const s = 0.35 * env(t, 1.2, 1.4, 2.2, 2.6); return V(s, s, 0); } }, iris_right: { scale: (t) => { const s = 0.35 * env(t, 1.3, 1.5, 2.2, 2.6); return V(s, s, 0); } } } });
exprClip('expr_dizzy', 3.6, POSES.dizzy, {
  tIn: 0.2, tOut: 2.8,
  extra: {
    iris_left: { rotation: (t) => V(0, 0, 900 * lin(t, 0.2, 2.8)) }, iris_right: { rotation: (t) => V(0, 0, -900 * lin(t, 0.2, 2.8)) },
    eye_left: { rotation: (t) => V(0, 0, 9 * sin(W2 * 2 * t) * env(t, 0.2, 0.5, 2.5, 2.9)) }, eye_right: { rotation: (t) => V(0, 0, 9 * sin(W2 * 2 * t + PI) * env(t, 0.2, 0.5, 2.5, 2.9)) },
    head: { rotation: (t) => V(6 * sin(W2 * 0.8 * t) * env(t, 0.2, 0.5, 2.5, 2.9), 0, 8 * Math.cos(W2 * 0.8 * t) * env(t, 0.2, 0.5, 2.5, 2.9)) },
  },
});
// the review sheet: one held pose per second
{
  const S = {};
  const put = (b, ch, t, v) => { S[b] = S[b] || {}; S[b][ch] = S[b][ch] || []; S[b][ch].push([t, v]); };
  const names = Object.keys(POSES);
  const keysAll = names.map((n) => poseKeys(POSES[n], 0.0001, 99));
  const chans = new Set(keysAll.flatMap((k) => Object.entries(k).flatMap(([b, chs]) => Object.keys(chs).map((ch) => b + '|' + ch))));
  names.forEach((n, t) => {
    for (const bc of chans) { const [b, ch] = bc.split('|'); const k = keysAll[t][b] && keysAll[t][b][ch]; put(b, ch, t, k ? k[1][1] : ch === 'scale' ? ONE : Z); }
  });
  anim('expression_sheet', names.length, 'once', {}, S);
}
