// Trade Master: Trade Bot, upgraded (design/briefs/trade-master.md). Not a generator of its own: run
//   node trade-bot.gen.js --variant=trade-master.variant.js
// (or node trade-master.gen.js). Trade Bot's geometry, rig, clips and sound cues are reused untouched; this module
// swaps his frame and lid plates to netherite, adds the beard, monocle, ledger and Stock Link bulb, and gives the beard
// and monocle chain derived spring motion from each clip's own head motion. Nothing here edits a Trade Bot track.
const PI = Math.PI;

const palette = {
  // from vanilla netherite armour (netherite_layer_1.png, user 2026-11-04): plum mid-tones, warm brown-black in the
  // shadow slot, a dusty grey-lilac leading edge. Index-matched to Trade Bot's steel shades (dark, main, light)
  netherite: ['#322727', '#3f303b', '#51444e'],
  netheriteHi: ['#5d565d'],
  // muted violet eyes (user, 2026-11-04). Trade Bot's "amber" keys drive his irises and his station's glow, so both
  // turn violet together and his light still moves into the station when he docks
  amber: ['#9c80c9', '#9276bf'],
  amberHi: ['#b89ee0'],
  catch: ['#e2d6f0'],
  amberOff: ['#2e2638', '#362c42'],
  // the house walnut (visual-language doc): the generator's darker prop walnut reads black in game
  walnutHouse: ['#4e3626', '#634532', '#6f4d36'],
  // the Helper Bot's whisker grille colours, so moustache and beard read as one family
  whisk: ['#6e5a2a', '#7a6530'],
  whiskSlot: ['#4a3c1b'],
  // Create's Stock Link bulb (lit #ae0000, unlit #3b0000), muted (user, 2026-11-04)
  bulbOff: ['#3b1a17', '#43201c'],
  bulb: ['#9a2a20'],
  bulbHi: ['#b8473a'],
};

// ---------- geometry ----------
const find = (n, name) => {
  if (n.name === name) return n;
  for (const k of n.kids || []) { const f = find(k, name); if (f) return f; }
  return null;
};
const must = (root, name) => { const n = find(root, name); if (!n) throw new Error('trade master: no part ' + name); return n; };

// beard rows (spade, odd widths 7-5-3-1 so every column is centred on a whole x): [x0, x1, y0, y1]. Grille slots sit on
// the odd columns, so they line up down the beard and mirror left to right; the moustache's ends are slots, like the
// Helper Bot's slot-dark whisker tips
const BEARD_Z = [-4.75, -4.25];
const BEARD_ROWS = [
  ['beard_top', -3.5, 3.5, 16.5, 17.5],
  ['beard_mid', -2.5, 2.5, 15.75, 16.5],
  ['beard_low', -1.5, 1.5, 15, 15.75],
  ['beard_tip', -0.5, 0.5, 14.25, 15],
];
const isSlot = (c) => Math.abs(Math.round(c)) % 2 === 1; // c: a column's centre x
const MONO_Z = [-5.3, -5.05]; // just in front of the lid plate (z -5), which slides behind it
// monocle (smaller, user 2026-11-04): a round ring 4 px across, 0.5 px bars, framing the right iris (centre 4, 19.5),
// standing proud of the brass rim and the lid plate; its bottom sits on the moustache line
const MONO_C = [4, 19.5, -5.175];
const MONO_RING = [
  ['mono_top', [3, 21, 0], [5, 21.5, 0]],
  ['mono_bottom', [3, 17.5, 0], [5, 18, 0]],
  ['mono_inner', [2, 18.5, 0], [2.5, 20.5, 0]],
  ['mono_outer', [5.5, 18.5, 0], [6, 20.5, 0]],
  ['mono_c1', [2.5, 20.5, 0], [3, 21, 0]], ['mono_c2', [5, 20.5, 0], [5.5, 21, 0]],
  ['mono_c3', [2.5, 18, 0], [3, 18.5, 0]], ['mono_c4', [5, 18, 0], [5.5, 18.5, 0]],
];
// the chain hangs from the ring's bottom just outside the beard's edge (beard top reaches x 3.5), so a flaring beard
// never pushes into it, and inside the right arm (x 5)
const CHAIN_PIVOT = [4, 17.5, -5.175];
const CHAIN_LINKS = [
  ['chain_a', [3.75, 16.75], [4.25, 17.5]],
  ['chain_b', [4, 16], [4.5, 16.75]],
];
const FOB = [[3.6, 15.25, -5.425], [4.4, 16, -5.05]];
// the ledger lies flat on his back (covers face front and back): its bounding box, and its parts
const LEDGER = [[-4.5, 9, 4], [-2.5, 13, 5]];

function model({ bot, station, G, C, P, mot }) {
  // netherite frame plates and lid plates (brass kept); the station's plinth frame to match
  const MAP = {};
  P.steel.forEach((c, i) => { MAP[c] = palette.netherite[i]; });
  MAP[P.steelHi[0]] = palette.netheriteHi[0];
  const recolour = (root, names, map) => names.forEach((name) => {
    const c = must(root, name), orig = c.paint, mat = c.mat;
    c.paint = (f, i, j, w, h, x, y) => {
      let col = orig ? orig(f, i, j, w, h, x, y) : null;
      if (col === null || col === undefined) col = mot(mat, x, y);
      return map[col] || col;
    };
  });
  const netheritise = (root, names) => recolour(root, names, MAP);
  // upper arms and the steel cuff row of each forearm too (texture review, 2026-11-04): one plated body; the eye pods
  // and neck stay steel so the face stays Trade Bot's
  netheritise(bot, ['chassis', 'side_panel_right', 'side_panel_left', 'top_cap', 'drawer_box', 'lid_plate_right', 'lid_plate_left',
    'upper_arm_right', 'upper_arm_left', 'forearm_shell_right', 'forearm_shell_left']);
  netheritise(station, ['plinth_core', 'plinth_front', 'plinth_back', 'deck_left', 'deck_right', 'deck_front', 'deck_back']);
  // Trade Bot's unlit iris keeps a warm amber glint pixel; his is a dark violet one (texture review, 2026-11-04)
  recolour(bot, ['iris_dark_right', 'iris_dark_left'], { '#6b5038': '#4a3d5a' });

  // whisker-grille beard, hanging from the head under the eye pods
  const grille = (x0) => (f, i, j, w, h, x, y) => {
    if (f === 'north') return isSlot(x0 + i + 0.5) ? P.whiskSlot[0] : null;
    if (f === 'up') return P.brass[1];
    if (f === 'down') return P.whiskSlot[0];
    return null;
  };
  must(bot, 'head').kids.push(G('beard', [0, 17.5, -4.625], BEARD_ROWS.map(([n, x0, x1, y0, y1]) =>
    C(n, [x0, y0, BEARD_Z[0]], [x1, y1, BEARD_Z[1]], 'whisk', { paint: grille(x0) }))));

  // monocle on the right lens, with a short chain and fob hanging from its lower inner edge
  const ring = MONO_RING.map(([n, a, b]) => C(n, [a[0], a[1], MONO_Z[0]], [b[0], b[1], MONO_Z[1]], 'brass', {
    paint: (f, i, j, w, h, x, y) => (f === 'north' && n === 'mono_top' ? P.brass[2] : f === 'north' && n === 'mono_bottom' ? P.brassDark[0] : null),
  }));
  const chain = G('monocle_chain', CHAIN_PIVOT, [
    ...CHAIN_LINKS.map(([n, a, b]) => C(n, [a[0], a[1], MONO_Z[0]], [b[0], b[1], MONO_Z[1]], 'brass', { paint: (f) => (f === 'north' && n === 'chain_b' ? P.brassDark[0] : null) })),
    C('fob', FOB[0], FOB[1], 'brass', { paint: (f, i, j) => (f === 'north' && i === 0 && j === 0 ? P.brass[2] : null) }),
  ]);
  must(bot, 'eye_right').kids.push(G('monocle', MONO_C, [...ring, chain]));

  // the Trade Ledger strapped to his back, left of the key: walnut covers and spine (spine outward) round a paper page
  // block that shows on the three open edges, a raised emerald on the back cover, and a dark steel strap across it
  // with a brass buckle (painted, so nothing reaches into the turning key)
  const ruled = (f, i, j, w, h, x, y) => (f === 'up' || f === 'down' ? (i % 2 === 1 ? '#7a7262' : null) : j % 2 === 1 ? '#7a7262' : null);
  must(bot, 'body').kids.push(G('ledger', [-3.5, 11, 4.5], [
    C('ledger_cover_back', [-4.5, 9, 4.75], [-2.5, 13, 5], 'walnutHouse', {
      paint: (f, i, j, w, h, x, y) => (f === 'south' && j === 2 ? (i === w - 1 ? P.brass[1] : mot('steel', x, y)) : f === 'south' && (j === 0 || j === h - 1) ? P.walnutHouse[2] : f === 'east' && j === 2 ? P.brass[0] : null),
    }),
    C('ledger_cover_front', [-4.5, 9, 4], [-2.5, 13, 4.25], 'walnutHouse'),
    C('ledger_spine', [-4.5, 9, 4.25], [-4.25, 13, 4.75], 'walnutHouse', { paint: (f, i, j, w, h, x, y) => (f === 'west' && (j === 0 || j === h - 1) ? P.brass[0] : null) }),
    C('ledger_pages', [-4.25, 9.25, 4.25], [-2.6, 12.75, 4.75], 'paper', { paint: ruled }),
    C('ledger_gem', [-3.75, 11.75, 5], [-3.25, 12.25, 5.25], 'emerald', { paint: (f, i, j) => (f === 'south' && i === 0 && j === 0 ? P.emeraldHi[0] : null) }),
  ]));

  // Stock Link aerial: the bead becomes a dark glass bulb in a brass cup, with a lit cube for the blink
  const aerial = must(bot, 'aerial');
  aerial.kids = aerial.kids.filter((k) => k.name !== 'aerial_bead');
  must(aerial, 'aerial_rod').to[1] = 22.75; // ends inside the cup instead of sharing its top face
  aerial.kids.push(G('aerial_bulb', [-5.5, 23.5, 1], [
    C('aerial_cup', [-6, 22.75, 0.5], [-5, 23.25, 1.5], 'brass', { paint: (f) => (f === 'up' ? P.brassDark[0] : null) }),
    C('bulb_dark', [-5.875, 23.25, 0.625], [-5.125, 23.875, 1.375], 'bulbOff', { paint: (f, i, j) => (f === 'north' && i === 0 && j === 0 ? '#5a2a24' : null) }),
    G('aerial_bulb_glow', [-5.5, 23.6, 1], [
      C('bulb_lit', [-5.975, 23.25, 0.525], [-5.025, 24, 1.475], 'bulb', { paint: (f, i, j) => (f === 'north' && i === 0 && j === 0 ? P.bulbHi[0] : null) }),
    ]),
  ]));
}

// ---------- derived motion and checks ----------
function after(ctx) {
  const { ALL_CLIPS, animations, groupByName, chainMatrix, m4inv, M4, ARM_POINTS, BODY_BOXES, HEAD, uuid, r2, FPS, V, addV, smooth, lin, station } = ctx;
  const clamp = (v, m) => Math.max(-m, Math.min(m, v));
  const loopOf = Object.fromEntries(animations.map((a) => [a.name, a.loop]));

  // a hanging part: its target leans against the summed X/Z tilt of the bones carrying it (k = how freely it hangs),
  // reached through a damped spring, which gives the lag and the overshoot. Loops run to a steady state and use the
  // last period; one-shot clips start settled and blend onto the target over the last 0.3 s, so clips still chain.
  function hanging(T, L, loop, bones, k, hz, zeta, lim, kz = k) {
    const target = (t) => {
      let x = 0, z = 0;
      for (const b of bones) { const r = T[b] && T[b].rotation ? T[b].rotation(t) : null; if (r) { x += r[0]; z += r[2]; } }
      return [clamp(-k * x, lim), clamp(-kz * z, lim)];
    };
    const isLoop = loop === 'loop', w = 2 * PI * hz, dt = 1 / 600, span = isLoop ? 4 * L : L;
    const n = Math.ceil(span / dt) + 2, xs = new Float64Array(n), zs = new Float64Array(n);
    let [x, z] = target(0), vx = 0, vz = 0;
    for (let s = 0; s < n; s++) {
      xs[s] = x; zs[s] = z;
      const t = s * dt, tg = target(isLoop ? t % L : Math.min(t, L));
      vx += (w * w * (tg[0] - x) - 2 * zeta * w * vx) * dt; x += vx * dt;
      vz += (w * w * (tg[1] - z) - 2 * zeta * w * vz) * dt; z += vz * dt;
    }
    const at = (arr, t) => { const f = (isLoop ? 3 * L + t : t) / dt, s = Math.min(n - 2, Math.max(0, Math.floor(f))); return arr[s] + (arr[s + 1] - arr[s]) * (f - s); };
    return (t) => {
      let v = [at(xs, t), 0, at(zs, t)];
      if (!isLoop) { const b = smooth(lin(t, L - 0.3, L)), e = target(L); v = [v[0] + (e[0] - v[0]) * b, 0, v[2] + (e[1] - v[2]) * b]; }
      return v;
    };
  }
  const neckY = (T, t) => (T.neck && T.neck.position ? T.neck.position(t)[1] : 0);
  const beardSquash = (T) => (t) => V(1, 1 - 0.15 * Math.max(0, Math.min(1, -neckY(T, t) / 1.5)), 1);

  // keys in the same shape and sampling as Trade Bot's own tracks (loops: 30 fps linear; others: 20 fps Catmull-Rom)
  const addTrack = (anim, bone, channel, fn, L) => {
    const g = groupByName[bone];
    const fps = anim.loop === 'loop' ? 30 : FPS, interp = anim.loop === 'loop' ? 'linear' : 'catmullrom';
    const a = anim.animators[g.uuid] = anim.animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
    for (let s = 0, n = Math.round(L * fps); s <= n; s++) {
      const t = s / fps, v = fn(t);
      a.keyframes.push({ channel, data_points: [{ x: String(r2(v[0])), y: String(r2(v[1])), z: String(r2(v[2])) }], uuid: uuid(), time: r2(t), color: -1, interpolation: interp });
    }
  };
  const addStep = (anim, bone, channel, v) => {
    const g = groupByName[bone];
    const a = anim.animators[g.uuid] = anim.animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
    a.keyframes.push({ channel, data_points: [{ x: String(v[0]), y: String(v[1]), z: String(v[2]) }], uuid: uuid(), time: 0, color: -1, interpolation: 'step' });
  };

  const BEARD_CHAIN = [...HEAD, 'beard'], MONO_CHAIN = [...HEAD, 'eye_right', 'monocle'], CHAIN_CHAIN = [...MONO_CHAIN, 'monocle_chain'], BODY = ['trade_bot', 'body'];
  const beardBoxes = BEARD_ROWS.map(([n, x0, x1, y0, y1]) => ({ name: n, chain: BEARD_CHAIN, box: [[x0, y0, BEARD_Z[0]], [x1, y1, BEARD_Z[1]]] }));
  const chainBox = [[3.6, 15.25, -5.425], [4.5, 17.5, MONO_Z[1]]];
  const grid = (box, chain) => {
    const pts = [];
    for (const fx of [0.02, 0.5, 0.98]) for (const fy of [0.02, 0.5, 0.98]) for (const fz of [0.02, 0.5, 0.98])
      pts.push([chain, [0, 1, 2].map((a) => box[0][a] + (box[1][a] - box[0][a]) * [fx, fy, fz][a])]);
    return pts;
  };
  const inside = (box, q) => Math.min(q[0] - box[0][0], box[1][0] - q[0], q[1] - box[0][1], box[1][1] - q[1], q[2] - box[0][2], box[1][2] - q[2]);
  const local = (origin, rot, sc) => M4.mul(M4.T(origin), M4.mul(M4.Rz(rot[2]), M4.mul(M4.Ry(rot[1]), M4.mul(M4.Rx(rot[0]), M4.mul(M4.S(sc), M4.T(origin.map((x) => -x)))))));
  const lamp = { name: 'lamp', chain: BODY, box: [[2, 13, -4.6], [3, 14, -3.5]] };
  const chestBoxes = [...BODY_BOXES.filter((b) => ['chassis', 'drawer'].includes(b.name)), lamp];
  const beardLocalPts = beardBoxes.flatMap((b) => grid(b.box, BEARD_CHAIN)).map(([, p]) => p);
  const chainLocalPts = grid(chainBox, CHAIN_CHAIN).map(([, p]) => p);
  const BEARD_ORIGIN = groupByName.beard.origin, CHAIN_ORIGIN = groupByName.monocle_chain.origin;

  // contact: a hanging part rests against what's in its way instead of passing through it. Per frame, the cheapest
  // change that clears every obstacle: for the beard, concertina-ing up (a squash, like his telescoping neck) is
  // preferred over flaring forward, which is capped; then a light smoothing. The hang and the poses are periodic in
  // loops and match at clip seams, so the contact does too.
  function contact(T, L, loop, prefer, base, opts) {
    const fps = 30, n = Math.round(L * fps), out = [];
    for (let s = 0; s <= n; s++) {
      const t = s / fps, hang = prefer(t), sq0 = base.squash ? base.squash(t)[1] : 1;
      const parentM = chainMatrix(T, opts.parent, t);
      const obst = opts.obstacles(t);
      const pen = (rx, rz, sq) => {
        const M = M4.mul(parentM, local(opts.origin, [rx, 0, rz], [1, sq, 1]));
        let worst = 0;
        for (const p of opts.pts) { const w = M4.apply(M, p); for (const o of obst.boxes) worst = Math.max(worst, inside(o.box, M4.apply(o.inv, w))); }
        if (obst.points.length) {
          const inv = m4inv(M);
          for (const w of obst.points) { const q = M4.apply(inv, w); for (const b of opts.ownBoxes) worst = Math.max(worst, inside(b, q)); }
        }
        return worst;
      };
      let best = null, bestPen = null;
      const sqs = opts.squash ? [sq0, ...[0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3].filter((q) => q < sq0)] : [1];
      for (let k = 0; k <= opts.maxDeg / 2.5; k++) for (const sign of k ? opts.dirs : [1]) for (const sq of sqs) {
        const off = sign * k * 2.5, cost = Math.abs(off) / 10 + (sq0 - sq) / 0.3;
        if (best && cost >= best[0]) continue;
        const p = pen(hang[0] + off, hang[2], sq);
        if (p < 0.02) best = [cost, hang[0] + off, sq];
        else if (!bestPen || p < bestPen[0]) bestPen = [p, hang[0] + off, sq];
      }
      const pick = best || bestPen;
      out.push([pick[1], hang[2], pick[2]]);
    }
    // smoothing (wrapping in loops; seams keep their solved values)
    const sm = out.map((v, s) => {
      if (process.env.TM_NOSMOOTH === '1' || (loop !== 'loop' && (s < 2 || s > n - 2))) return v;
      const idx = (d) => (loop === 'loop' ? ((s + d) % n + n) % n : s + d);
      return [0, 1, 2].map((a) => [-2, -1, 0, 1, 2].reduce((acc, d) => acc + out[idx(d)][a], 0) / 5);
    });
    const at = (t, a) => { const f = Math.min(n, Math.max(0, t * fps)), s = Math.min(n - 1, Math.floor(f)); return sm[s][a] + (sm[s + 1][a] - sm[s][a]) * (f - s); };
    return { rotation: (t) => V(at(t, 0), 0, at(t, 1)), scale: (t) => V(1, at(t, 2), 1) };
  }
  // the monocle pop (user 2026-11-04): whenever his lid plates snap wide open (surprise), the monocle shoots out and up
  // with a stretch, flips over in the air (a coin flip: X scale through zero and back, so nothing has to unwind at
  // the clip's end), drops back into the eye with a squash and a wobble. 1.2 s, played up to 2x faster when the clip
  // ends sooner (the scared spot is only 1.1 s), so every clip still ends at rest and chains; at most once per 1.2 s.
  const POP_ARC = 0.55, POP_LEN = 1.2;
  function popTimes(T, L, name) {
    const lid = (t) => (T.lid_right && T.lid_right.position ? T.lid_right.position(t)[1] : 0);
    if (process.env.TM_LID === name) { const r = []; for (let t = 0; t <= L; t += 0.05) r.push(t.toFixed(2) + ":" + lid(t).toFixed(2)); console.error("lid " + name + " " + r.join(" ")); }
    const out = [];
    for (let t = 1 / 60; t <= L - 0.5 * POP_LEN; t += 1 / 60) {
      const a = lid(t - 1 / 60), b = lid(t);
      if (a < 0.8 && b >= 0.8 && (b - a) * 60 > 2 && (!out.length || t - out[out.length - 1][0] >= POP_LEN)) out.push([t, Math.min(1, (L - t) / POP_LEN)]);
    }
    return out;
  }
  function popTracks(times) {
    // phase in "full-speed" seconds since the pop began
    const phase = (t) => { for (const [t0, k] of times) if (t >= t0 && t < t0 + k * POP_LEN) return (t - t0) / k; return -1; };
    return {
      position: (t) => { const p = phase(t); if (p < 0 || p > POP_ARC) return V(); const a = Math.sin(PI * p / POP_ARC); return V(0.3 * a, 2.4 * a, -1.4 * a); },
      rotation: (t) => { const p = phase(t); if (p < 0) return V(); if (p <= POP_ARC) return V(0, 0, 15 * Math.sin(PI * p / POP_ARC)); const w = p - POP_ARC; return V(0, 0, 20 * Math.exp(-6 * w) * Math.sin(16 * w)); },
      scale: (t) => {
        const p = phase(t); if (p < 0) return V(1, 1, 1);
        if (p <= POP_ARC) { const u = p / POP_ARC; return V(Math.cos(2 * PI * u), 1 + 0.15 * Math.sin(PI * u), 1); }
        const w = p - POP_ARC, q = 0.2 * Math.exp(-8 * w) * Math.cos(18 * w); return V(1 + q, 1 - q, 1);
      },
      flail: (t) => { const p = phase(t); if (p < 0 || p > POP_ARC + 0.3) return V(); const u = Math.min(1, p / POP_ARC); return V(60 * Math.sin(PI * u), 0, 40 * Math.sin(2 * PI * u)); },
    };
  }
  const armWorld = (T, t) => ARM_POINTS.map(([chain, p]) => M4.apply(chainMatrix(T, chain, t), p));

  const NEW = new Set(['beard', 'monocle', 'monocle_chain', 'ledger', 'aerial_bulb', 'aerial_bulb_glow']);
  const derived = {}; // clip name -> extra tracks, for the checks
  let worstBeard = 0, worstChain = 0;
  const popLog = [];
  for (const [name, T, L] of ALL_CLIPS) {
    const anim = animations.find((a) => a.name === name);
    for (const id of Object.keys(anim.animators)) if (NEW.has(anim.animators[id].name)) throw new Error('trade master: ' + name + ' already animates ' + anim.animators[id].name);
    const loop = loopOf[name];
    const beardHang = hanging(T, L, loop, ['trade_bot', 'body', 'head'], 0.5, 2.2, 0.35, 25, 1.0); // hangs plumb against tilt, so its ends never dip
    const pops = popTimes(T, L, name), pop = popTracks(pops);
    const Tp = pops.length ? { ...T, monocle: { position: pop.position, rotation: pop.rotation, scale: pop.scale } } : T;
    const chainHang0 = hanging(T, L, loop, ['trade_bot', 'body', 'head', 'eye_right'], 0.85, 1.8, 0.25, 40);
    const chainHang = (t) => addV(chainHang0(t), pop.flail(t)); // flung about as the monocle flies
    const tuck = (t) => { const h = T.head && T.head.rotation ? T.head.rotation(t)[0] : 0; return Math.max(0, -h); };
    const beardPrefer = (t) => { const h = beardHang(t); return V(Math.max(-2, h[0], tuck(t)), 0, h[2]); };
    const beard = contact(T, L, loop, beardPrefer, { squash: beardSquash(T) }, {
      parent: HEAD, origin: BEARD_ORIGIN, pts: beardLocalPts, squash: true, dirs: [1, -1], maxDeg: 20,
      ownBoxes: beardBoxes.map((b) => b.box),
      obstacles: (t) => ({ boxes: chestBoxes.map((b) => ({ box: b.box, inv: m4inv(chainMatrix(T, b.chain, t)) })), points: armWorld(T, t) }),
    });
    const Tb = { ...Tp, beard };
    const chain = contact(Tb, L, loop, chainHang, {}, {
      parent: MONO_CHAIN, origin: CHAIN_ORIGIN, pts: chainLocalPts, squash: false, ownBoxes: [chainBox], dirs: [1, -1], maxDeg: 60,
      obstacles: (t) => ({ boxes: beardBoxes.map((b) => ({ box: b.box, inv: m4inv(chainMatrix(Tb, b.chain, t)) })), points: armWorld(T, t) }),
    });
    addTrack(anim, 'beard', 'rotation', beard.rotation, L);
    let minSq = 1;
    for (let t = 0; t <= L; t += 0.05) minSq = Math.min(minSq, beard.scale(t)[1]);
    if (minSq < 0.999) addTrack(anim, 'beard', 'scale', beard.scale, L);
    addTrack(anim, 'monocle_chain', 'rotation', chain.rotation, L);
    if (pops.length) { addTrack(anim, 'monocle', 'position', pop.position, L); addTrack(anim, 'monocle', 'rotation', pop.rotation, L); addTrack(anim, 'monocle', 'scale', pop.scale, L); popLog.push(name + ' ' + pops.map(([x, k]) => x.toFixed(2) + (k < 1 ? ' (x' + (1 / k).toFixed(2) + ')' : '')).join('/')); }
    addStep(anim, 'aerial_bulb_glow', 'scale', [0, 0, 0]); // the game blinks it; clips keep it off
    for (let t = 0; t <= L; t += 0.05) { worstBeard = Math.max(worstBeard, ...beard.rotation(t).map(Math.abs)); worstChain = Math.max(worstChain, ...chain.rotation(t).map(Math.abs)); }
    derived[name] = { beard, monocle_chain: chain, ...(pops.length ? { monocle: Tp.monocle } : {}) };
    if (process.env.TM_PROBE) for (const pr of process.env.TM_PROBE.split(',')) {
      const [pn, pt] = pr.split('@');
      if (pn !== name) continue;
      const t = +pt, g = (b, ch) => (T[b] && T[b][ch] ? T[b][ch](t).map((v) => v.toFixed(1)).join(' ') : '-');
      console.error('probe ' + pr + ': root rot ' + g('trade_bot', 'rotation') + ' | body rot ' + g('body', 'rotation') + ' pos ' + g('body', 'position') + ' | neck ' + g('neck', 'position') + ' | head ' + g('head', 'rotation') + ' | beard ' + beard.rotation(t).map((v) => v.toFixed(1)).join(' ') + ' sq ' + beard.scale(t)[1].toFixed(2) + ' (prefer ' + beardPrefer(t).map((v) => v.toFixed(1)).join(' ') + ')');
    }
  }
  console.error('trade master: monocle pops in ' + popLog.join(', '));
  console.error('trade master: beard swing up to ' + worstBeard.toFixed(1) + '°, monocle chain up to ' + worstChain.toFixed(1) + '°');

  // ---- checks: the new parts against his arms, his body, each other, and the station when docking ----
  const NEW_BOXES = [
    ...beardBoxes,
    ...MONO_RING.map(([n, a, b]) => ({ name: n, chain: MONO_CHAIN, box: [[a[0], a[1], MONO_Z[0]], [b[0], b[1], MONO_Z[1]]] })),
    { name: 'chain', chain: CHAIN_CHAIN, box: chainBox },
    { name: 'ledger', chain: BODY, box: LEDGER },
  ];
  const depth = (T, t, points, boxes) => {
    let worst = [0, ''];
    const inv = boxes.map((bx) => ({ ...bx, inv: m4inv(chainMatrix(T, bx.chain, t)) }));
    for (const [chain, pt] of points) {
      const p = M4.apply(chainMatrix(T, chain, t), pt);
      for (const bx of inv) {
        const q = M4.apply(bx.inv, p);
        const d = Math.min(q[0] - bx.box[0][0], bx.box[1][0] - q[0], q[1] - bx.box[0][1], bx.box[1][1] - q[1], q[2] - bx.box[0][2], bx.box[1][2] - q[2]);
        if (d > worst[0]) worst = [d, chain[chain.length - 1] + ' in ' + bx.name];
      }
    }
    return worst;
  };
  const bodyBoxes = chestBoxes;
  const beardPts = beardBoxes.flatMap((b) => grid(b.box, BEARD_CHAIN));
  const chainPts = grid(chainBox, CHAIN_CHAIN);
  const ledgerPts = grid(LEDGER, BODY);
  const stationCube = (n) => { const c = find(station, n); return [c.from, c.to]; };
  const STATION_BOXES = [
    { name: 'post', chain: ['station', 'station_base'], box: stationCube('post') },
    { name: 'post_cog', chain: ['station', 'post_cog'], box: stationCube('post_cog_a') },
    { name: 'winder_knuckle', chain: ['station', 'winder_arm'], box: stationCube('winder_knuckle') },
    { name: 'winder_bar', chain: ['station', 'winder_arm'], box: stationCube('winder_bar') },
    { name: 'winder_socket', chain: ['station', 'winder_arm', 'winder_head'], box: stationCube('winder_socket') },
  ];
  const DOCK = new Set(['dock_lights_out', 'action_comes_in', 'packed']);
  // Trade Bot's own eye pods dip into his chest when he looks down hard (approved clips); the beard hangs under them,
  // so it's held to "no deeper than his own head at that moment", not to zero
  const podPtsAll = [...grid([[1, 17, -4], [7, 22, 2]], [...HEAD, 'eye_right']), ...grid([[-7, 17, -4], [-1, 22, 2]], [...HEAD, 'eye_left'])];
  if (process.env.TM_DEBUG === '1') {
    // how far the head's own pose pushes a rigid beard (and Trade Bot's eye pods, for reference) into his chest
    const podPts = [...grid([[1, 17, -4], [7, 22, 2]], [...HEAD, 'eye_right']), ...grid([[-7, 17, -4], [-1, 22, 2]], [...HEAD, 'eye_left'])];
    for (const [name, T, L] of ALL_CLIPS) {
      let wb = [0, 0], wp = [0, 0], hr = [0, 0];
      for (let t = 0; t <= L; t += 1 / 30) {
        const b = depth(T, t, beardPts, chestBoxes)[0], p = depth(T, t, podPts, chestBoxes)[0];
        const h = T.head && T.head.rotation ? T.head.rotation(t)[0] : 0;
        if (b > wb[0]) wb = [b, t]; if (p > wp[0]) wp = [p, t]; if (h < hr[0]) hr = [h, t];
      }
      if (wb[0] > 0.05) console.error('rigid ' + name.padEnd(20) + ' beard ' + wb[0].toFixed(2) + ' at ' + wb[1].toFixed(2) + '  pods ' + wp[0].toFixed(2) + '  head pitch min ' + hr[0].toFixed(0) + '° at ' + hr[1].toFixed(2));
    }
  }
  const report = [];
  let anyClip = false;
  for (const [name, T0, L] of ALL_CLIPS) {
    const T = { ...T0, ...derived[name] };
    const w = { arms: [0, '', 0], beard: [0, '', 0], chain: [0, '', 0], ledger: [0, '', 0] };
    const keep = (k, r, t) => { if (r[0] > w[k][0]) w[k] = [r[0], r[1], t]; };
    for (let t = 0; t <= L + 1e-9; t += 1 / 30) {
      keep('arms', depth(T, t, ARM_POINTS, NEW_BOXES), t);
      { const b = depth(T, t, beardPts, bodyBoxes), p = depth(T, t, podPtsAll, bodyBoxes)[0]; keep('beard', [b[0] - p, b[1] + ' (beyond his pods)'], t); }
      keep('chain', depth(T, t, chainPts, beardBoxes), t);
      if (DOCK.has(name)) keep('ledger', depth(T, t, ledgerPts, STATION_BOXES), t);
    }
    const bad = Object.entries(w).filter(([, v]) => v[0] > 0.05);
    if (bad.length) anyClip = true;
    report.push(name.padEnd(22) + (bad.length ? bad.map(([k, v]) => k + ': ' + v[0].toFixed(2) + ' px ' + v[1] + ' at ' + v[2].toFixed(2) + 's').join('; ') : 'clear'));
  }
  if (anyClip || process.env.TM_REPORT === '1') console.error('trade master clearance (new parts):\n  ' + report.join('\n  '));
  else console.error('trade master clearance: every clip clear (arms, beard, chain, ledger vs station)');
}

module.exports = { slug: 'trade-master', id: 'trade_master', palette, model, after };
