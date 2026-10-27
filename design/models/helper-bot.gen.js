// Helper Bot generator -> design/models/helper-bot.bbmodel (modded_entity, box UV). Forked from trade-bot.gen.js:
// the head, eye pods, lids, irises, arms, texture packing, spring maths, FK and arm IK are his; the wheel, drawer,
// lamp, wind-up key and station are replaced by a foot plate, swivel post, gimbal spring, pocket watch, schedule clip, whistle,
// whisker grille and caps. Brief: design/briefs/helper-bot.md.
// Front is -Z, his right is +X. Rotations right-handed (rx+ tips top to +Z, rz+ tips top to -X, ry+ turns front to -X).
// The origin is the centre of his foot plate, which sits 2 px forward of the block centre (toward the controls).
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');
const path = require('path');

const ARGS = process.argv.slice(2);
const OUT = ARGS.find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'helper-bot.bbmodel');
const TEX = 256; // props included for the preview
const uuid = () => crypto.randomUUID();
const BLOCK_Z = 2; // block centre, in model coords (the foot plate is 2 px forward of it)

// ---------- palette ----------
const P = {
  steel: ['#2f343b', '#3c424a', '#4a5059'],
  steelHi: ['#5a616b'],
  andesite: ['#6b6e6a', '#7c7f79', '#8a8c86'],
  panel: ['#6b6e6a', '#7c7f79', '#8a8c86'], // the dye zone; plain is andesite, dyed copies tint it (see dyeShades)
  brass: ['#8f7638', '#b08f45', '#c9a75a'],
  brassDark: ['#6e5a2a'],
  copper: ['#7a4a2e', '#9c5f3a', '#a96a42'],
  copperDark: ['#5e3823'],
  rubber: ['#26272a', '#323337'],
  glass: ['#14181c', '#1c2228'],
  glassHi: ['#2c3540'],
  cyan: ['#6fb7c4', '#67adba'],
  cyanHi: ['#8fcad4'],
  catch: ['#d3e8ea'],
  cyanOff: ['#1e3236', '#24393e'],
  navy: ['#2b3442', '#353f50'],
  navyDark: ['#222a36'],
  peak: ['#1f2125', '#26282d'],
  cardboard: ['#6e5236', '#7f6142'],
  cardboardDark: ['#5c442d'],
  tape: ['#9a8058'],
  paper: ['#a9a493', '#a29d8b'],
  ink: ['#4a4a44'],
  dial: ['#a9a493'],
  puff: ['#9a9c98', '#a4a6a2'],
  whisk: ['#6e5a2a', '#7a6530'],
  whiskSlot: ['#4a3c1b'],
  // preview props
  casing: ['#3a3c3f', '#44474b', '#4e5156'],
  railCasing: ['#2a2d31', '#33363b'],
  plank: ['#4e3626', '#5a3f2c'],
  stone: ['#5e5f5c', '#6f706c', '#7f807b'],
};
// muted dye colours, shared with the Fluid Lines (LineColors.DYES, by DyeColor id)
const DYES = { white: '#888C8B', orange: '#A55D21', magenta: '#873F81', light_blue: '#337B93', yellow: '#A58E31', lime: '#618C27', pink: '#985A6C', gray: '#464A4C', light_gray: '#6B6B68', cyan: '#267676', purple: '#663282', blue: '#383D7A', brown: '#674B37', green: '#546629', red: '#7F312C', black: '#353537' };
const shade = (h, k) => '#' + [1, 3, 5].map((i) => Math.max(0, Math.min(255, Math.round(parseInt(h.slice(i, i + 2), 16) * k))).toString(16).padStart(2, '0')).join('');
const dyeShades = (h) => [shade(h, 0.86), h, shade(h, 1.1)];
const rgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
function hash(x, y, s) {
  let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}
function mot(mat, x, y) {
  const p = P[mat];
  if (!p) throw new Error('no mat ' + mat);
  if (p.length === 1) return p[0];
  const r = hash(x, y, mat.length * 31 + mat.charCodeAt(0));
  if (p.length === 2) return r < 0.6 ? p[1] : p[0];
  return r < 0.18 ? p[0] : r < 0.84 ? p[1] : p[2];
}

// ---------- model DSL ----------
const G = (name, origin, kids, o = {}) => ({ kind: 'g', name, origin, kids, rot: o.rot || [0, 0, 0], hidden: !!o.hidden });
const C = (name, from, to, mat, o = {}) => ({ kind: 'c', name, from, to, mat, ...o });
const framed = (inner = 'panel', edge = 'steel') => (f, i, j, w, h, x, y) => {
  if (f === 'up' || f === 'down') return null;
  return i === 0 || j === 0 || i === w - 1 || j === h - 1 ? mot(edge, x, y) : mot(inner, x, y);
};

// ---------- head (Trade Bot's pods, with cyan irises; the whistle replaces his aerial) ----------
function eyePod(side) {
  const s = side > 0 ? 'right' : 'left';
  const sx = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const [x0, x1] = sx(1, 7);
  const cx = (x0 + x1) / 2;
  const kids = [
    C(`pod_${s}`, [x0, 17, -3], [x1, 22, 2], 'steel', {
      paint: (f, i, j, w, h, x, y) => {
        if (f === 'north') {
          if (i >= 1 && i <= 4 && j >= 1 && j <= 3) return (i === 1 || i === 2) && j === 1 ? mot('glassHi', x, y) : mot('glass', x, y);
          return mot('steel', x, y);
        }
        if ((f === 'east' && side > 0) || (f === 'west' && side < 0)) {
          if (i === 2 && j === 2) return P.brassDark[0];
          if (j === 0) return mot('steelHi', x, y);
        }
        if (f === 'up') return hash(x, y, 5) < 0.5 ? P.steel[2] : P.steel[1];
        return null;
      },
    }),
    C(`rim_${s}`, [x0, 17, -4], [x1, 22, -3], 'brass', {
      paint: (f, i, j, w, h, x, y) => {
        if (f === 'north' || f === 'south') {
          if (i >= 1 && i <= 4 && j >= 1 && j <= 3) return 'clear';
          if (j === h - 1) return mot('brassDark', x, y);
        }
        return null;
      },
    }),
    G(`iris_${s}`, [cx, 19.5, -3], [
      C(`iris_dark_${s}`, [cx - 1, 18.5, -3.5], [cx + 1, 20.5, -2.5], 'cyanOff', { paint: (f, i, j) => (f === 'north' && i === 0 && j === 0 ? '#3d5c62' : null) }),
      G(`iris_glow_${s}`, [cx, 19.5, -3], [
        C(`iris_lit_${s}`, [cx - 1, 18.5, -3.75], [cx + 1, 20.5, -2.75], 'cyan', {
          paint: (f, i, j) => (f !== 'north' ? null : i === 0 && j === 0 ? P.catch[0] : i === 1 && j === 1 ? P.cyanHi[0] : P.cyan[0]),
        }),
      ]),
    ]),
    G(`lid_${s}`, [cx, 21.5, -4.5], [
      C(`lid_plate_${s}`, [x0, 20, -5], [x1, 23, -4], 'steel', { paint: (f, i, j, w, h, x, y) => (f === 'north' && j === h - 1 ? mot('steelHi', x, y) : null) }),
    ]),
  ];
  if (side < 0) {
    // a little brass steam whistle on a copper pipe (where Trade Bot's aerial was), with a lever and a pull cord
    kids.push(
      G('whistle', [-5.5, 22, 1], [
        C('whistle_pipe', [-5.75, 22, 0.75], [-5.25, 23, 1.25], 'copper'),
        C('whistle_bell', [-6, 23, 0.5], [-5, 24, 1.5], 'brass', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? P.brassDark[0] : j === h - 1 ? mot('brassDark', x, y) : null) }),
        C('whistle_lever', [-7.75, 23.25, 0.75], [-6, 23.75, 1.25], 'brass'),
        G('whistle_cord', [-7.5, 23.25, 1], [
          C('cord', [-7.75, 18.75, 0.75], [-7.25, 23.25, 1.25], 'rubber'),
          C('cord_bead', [-8, 17.75, 0.5], [-7, 18.75, 1.5], 'brass'),
        ]),
        G('steam_puff', [-5.5, 24.5, 1], [C('puff', [-6.5, 24.25, 0], [-4.5, 25.75, 2], 'puff'), C('puff_wisp', [-5.25, 25.5, 0.5], [-3.75, 26.75, 1.5], 'puff')]),
      ])
    );
  }
  return G(`eye_${s}`, [side, 19.5, -0.5], kids);
}
// the whisker grille: his voice grille, shaped like an engine driver's walrus moustache
const grille = (f, i, j, w, h, x, y) => (f === 'north' && i % 2 === 1 ? P.whiskSlot[0] : f === 'up' ? P.brass[1] : null);
function whisker(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  return G(`whisker_${s}`, [0.75 * side, 17.6, -4.4], [
    C(`whisker_a_${s}`, [X(0.75, 2.5)[0], 17.25, -4.75], [X(0.75, 2.5)[1], 18, -4], 'whisk', { paint: grille }),
    C(`whisker_b_${s}`, [X(2.5, 4)[0], 16.75, -4.75], [X(2.5, 4)[1], 17.75, -4], 'whisk', { paint: grille }),
    C(`whisker_tip_${s}`, [X(4, 5)[0], 16.25, -4.75], [X(4, 5)[1], 17.25, -4], 'whisk', { paint: (f, i, j, w, h, x, y) => (f === 'north' ? P.whiskSlot[0] : grille(f, i, j, w, h, x, y)) }),
  ]);
}
// the caps sit across both pods; the peaks clear the lids at rest and the lids push them up when they open wide
const capTrain = G('cap_train', [0, 22, 2.5], [
  C('cap_crown', [-3.5, 22, -2.5], [3.5, 24.75, 1.5], 'navy', { paint: (f, i, j, w, h, x, y) => (f !== 'up' && f !== 'down' && j === h - 1 ? P.navyDark[0] : null) }),
  C('cap_top', [-4, 24.75, -3], [4, 25.5, 2], 'navy', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? (hash(x, y, 9) < 0.5 ? P.navy[1] : '#3a4456') : f === 'down' ? P.navyDark[0] : null) }),
  C('cap_peak', [-3.5, 23.6, -5.75], [3.5, 24, -2.5], 'peak'),
  C('cap_badge', [-0.5, 24.05, -2.75], [0.5, 24.7, -2.5], 'brass', { paint: (f, i, j) => (f === 'north' ? P.brass[2] : null) }),
]);
const capLogistics = G('cap_logistics', [0, 22, 2.5], [
  C('flatcap_crown', [-3.75, 22, -2.75], [3.75, 24.5, 1.75], 'cardboard', { paint: (f, i, j, w, h, x, y) => (f !== 'up' && f !== 'down' && j === h - 1 ? P.tape[0] : null) }),
  C('flatcap_top', [-4.25, 24.5, -3.5], [4.25, 25.25, 2], 'cardboard', { paint: (f, i, j, w, h, x, y) => (f === 'down' ? P.cardboardDark[0] : null) }),
  C('flatcap_button', [-0.5, 25.25, -1.25], [0.5, 25.5, -0.25], 'cardboardDark'),
  C('flatcap_brim', [-3.25, 23.6, -5.5], [3.25, 24, -2.75], 'cardboardDark'),
]);

// ---------- arms (Trade Bot's; the forearms are in the dye zone) ----------
function arm(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const ax = X(5, 7), cogX = X(7, 7.5), px = 6 * side;
  return G(`arm_${s}`, [px, 14, 0], [
    C(`upper_arm_${s}`, [ax[0], 10, -1], [ax[1], 15, 1], 'steel', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? mot('steelHi', x, y) : null) }),
    C(`shoulder_cog_${s}`, [cogX[0], 12.5, -1.5], [cogX[1], 15.5, 1.5], 'brass', { paint: (f, i, j) => ((f === 'east' || f === 'west') && i === 1 && j === 1 ? P.brassDark[0] : null) }),
    C(`shoulder_cog_teeth_${s}`, [cogX[0] + (side > 0 ? -0.01 : 0.01), 12.5, -1.5], [cogX[1] + (side > 0 ? -0.01 : 0.01), 15.5, 1.5], 'brassDark', { rotation: [45, 0, 0], origin: [(cogX[0] + cogX[1]) / 2, 14, 0] }),
    G(`forearm_${s}`, [px, 10, 0], [
      C(`forearm_shell_${s}`, [ax[0], 6, -1], [ax[1], 10, 1], 'panel', { dye: true, paint: (f, i, j, w, h, x, y) => (j === 0 && f !== 'up' && f !== 'down' ? mot('steel', x, y) : null) }),
      G(`claw_${s}`, [px, 6, 0], [
        C(`palm_${s}`, [ax[0], 5, -1], [ax[1], 6, 1], 'brass'),
        C(`prong_front_${s}`, [px - 0.5, 3, -1], [px + 0.5, 5, -0.25], 'brass', { paint: (f, i, j, w, h, x, y) => (j === h - 1 ? mot('brassDark', x, y) : null) }),
        C(`prong_back_${s}`, [px - 0.5, 3, 0.25], [px + 0.5, 5, 1], 'brass', { paint: (f, i, j, w, h, x, y) => (j === h - 1 ? mot('brassDark', x, y) : null) }),
        // the pocket watch in his left claw (lid hinged on its top front edge) and the schedule card in his right
        ...(side < 0
          ? [G('watch_held', [-6, 5, -1], [
              C('watch_body', [-7, 3.5, -2.25], [-5, 5.5, -1.75], 'brass', {
                paint: (f, i, j) => (f === 'north' ? ((i === 0 || i === 1) && (j === 0 || j === 1) ? (i === 1 && j === 0 ? P.ink[0] : P.dial[0]) : P.dial[0]) : null),
              }),
              G('watch_lid', [-6, 5.5, -2.25], [C('watch_lid_plate', [-7, 3.5, -2.5], [-5, 5.5, -2.25], 'brass', { paint: (f, i, j) => (f === 'north' && i === 1 && j === 1 ? P.brassDark[0] : null) })]),
            ])]
          : [G('card_held', [6, 5, -1], [
              C('card_held_paper', [5, 3.5, -2.25], [7, 6, -2], 'paper', { paint: (f, i, j) => (f === 'north' && j % 1 === 0 && j > 0 && j < 2 ? P.ink[0] : null) }),
            ])]),
      ]),
    ]),
  ]);
}

// ---------- the bot ----------
const footSide = (f, i, j, w, h, x, y) => (f === 'up' ? null : f === 'down' ? mot('steel', x, y) : j === 0 ? mot('steelHi', x, y) : mot('steel', x, y));
const bolt = (x, z) => C(`foot_bolt_${x > 0 ? 'r' : 'l'}${z > 0 ? 'b' : 'f'}`, [x - 0.5, 2, z - 0.5], [x + 0.5, 2.5, z + 0.5], 'brass', { paint: (f) => (f === 'up' ? P.brassDark[0] : null) });
const ringTeeth = (f, i, j, w, h, x, y) => (f === 'up' || f === 'down' ? null : i % 2 === 0 ? mot('brassDark', x, y) : null);
const bot = G('helper_bot', [0, 0, 0], [
  G('foot', [0, 0, 0], [
    C('foot_a', [-6, 0, -4], [6, 2, 4], 'andesite', { paint: footSide }),
    C('foot_b', [-5, 0, -5], [5, 2, -4], 'andesite', { paint: footSide }),
    C('foot_b2', [-5, 0, 4], [5, 2, 5], 'andesite', { paint: footSide }),
    C('foot_c', [-4, 0, -6], [4, 2, -5], 'andesite', { paint: footSide }),
    C('foot_c2', [-4, 0, 5], [4, 2, 6], 'andesite', { paint: footSide }),
    bolt(-4, -4), bolt(4, -4), bolt(-4, 4), bolt(4, 4),
    // a small Create cog on the front edge of the plate, geared to the swivel underneath
    G('collar_cog', [0, 2, -6.4], [
      C('collar_cog_disc', [-1.5, 0.5, -6.75], [1.5, 3.5, -6.05], 'brass', { paint: (f, i, j) => (f === 'north' && i === 1 && j === 1 ? P.brassDark[0] : null) }),
      C('collar_cog_teeth', [-1.5, 0.5, -6.7], [1.5, 3.5, -6.1], 'brassDark', { rotation: [0, 0, 45], origin: [0, 2, -6.4] }),
    ]),
  ]),
  G('swivel', [0, 2, 0], [
    C('collar', [-2.5, 2, -2.5], [2.5, 3, 2.5], 'brass', { paint: ringTeeth }),
    G('post', [0, 3, 0], [
      C('post_tube', [-1.5, 3, -1.5], [1.5, 6, 1.5], 'steel', { paint: (f, i, j, w, h, x, y) => (f !== 'up' && f !== 'down' && i === 0 ? mot('steelHi', x, y) : null) }),
      G('spring', [0, 6, 0], [C('spring_coil', [-1.25, 6, -1.25], [1.25, 7, 1.25], 'copper', { paint: (f, i, j, w, h, x, y) => (f !== 'up' && f !== 'down' && (i + j) % 2 === 0 ? P.copperDark[0] : null) })]),
      G('body', [0, 7, 0], [
        C('chassis', [-4, 7, -4], [4, 16, 4], 'panel', { dye: true, paint: (f, i, j, w, h, x, y) => (f === 'up' || f === 'down' ? mot('steel', x, y) : framed()(f, i, j, w, h, x, y)) }),
        C('side_panel_right', [4, 8, -3], [5, 15, 3], 'panel', { dye: true, paint: (f, i, j, w, h, x, y) => (f === 'east' ? framed()(f, i, j, w, h, x, y) : mot('steel', x, y)) }),
        C('side_panel_left', [-5, 8, -3], [-4, 15, 3], 'panel', { dye: true, paint: (f, i, j, w, h, x, y) => (f === 'west' ? framed()(f, i, j, w, h, x, y) : mot('steel', x, y)) }),
        C('waist_band', [-4.5, 11, -4.5], [4.5, 12, 4.5], 'brass', { paint: (f, i, j, w, h, x, y) => ((f === 'north' || f === 'south') && i % 2 === 1 ? mot('brassDark', x, y) : null) }),
        C('neck_collar', [-1.5, 16, -1.5], [1.5, 16.5, 1.5], 'brass'),
        // pocket watch on his left chest, on a short chain to the waist band
        G('watch_pocket', [-2.25, 13.75, -4.4], [
          C('watch_case', [-3.25, 12.75, -4.6], [-1.25, 14.75, -4.1], 'brass', { paint: (f, i, j) => (f === 'north' && i > 0 && j > 0 ? P.brass[2] : f === 'north' ? P.brass[1] : null) }),
          C('watch_crown', [-2.5, 14.75, -4.5], [-2, 15.25, -4.2], 'brass'),
          C('watch_chain', [-1.25, 12, -4.5], [-0.75, 13, -4.25], 'brassDark'),
        ]),
        // schedule clip on his right chest; the card only shows while the train has a schedule
        C('schedule_clip', [1.75, 14.25, -4.6], [3.25, 14.75, -4.1], 'brass'),
        G('schedule_card', [2.5, 13, -4.3], [
          C('card_paper', [1.5, 12.25, -4.4], [3.5, 14.5, -4.15], 'paper', { paint: (f, i, j) => (f === 'north' && (j === 1 || j === 3) && i < 3 ? P.ink[0] : null) }),
        ]),
        arm(1),
        arm(-1),
        G('neck', [0, 16, 0], [
          C('neck_tube', [-1, 12.5, -1], [1, 18.5, 1], 'steel', { paint: (f, i, j, w, h, x, y) => (j === 0 && f !== 'up' && f !== 'down' ? mot('brass', x, y) : null) }),
          G('head', [0, 18, 0], [
            C('bridge', [-1, 18.5, -1.5], [1, 20.5, 0.5], 'steel'),
            C('whisker_stem', [-0.75, 17.25, -4], [0.75, 18.5, -1.5], 'steel'),
            C('whisker_centre', [-0.75, 17.25, -4.75], [0.75, 18, -4], 'whisk', { paint: grille }),
            whisker(1),
            whisker(-1),
            eyePod(1),
            eyePod(-1),
            capTrain,
            capLogistics,
          ]),
        ]),
      ]),
    ]),
  ]),
]);

// ---------- preview props: built where the clips use them, parked 200 px below (the clips lift them back) ----------
const PROP_DROP = 200;
const pv = (v) => [v[0], v[1] - PROP_DROP, v[2]];
const PC = (name, from, to, mat, o = {}) => C(name, pv(from), pv(to), mat, { ...o, origin: o.origin ? pv(o.origin) : undefined });
const PG = (name, origin, kids) => G(name, pv(origin), kids);
// Create's Train Controls (a stand-in, not Create's model) in the block in front, facing him. Controls-local
// (lx, y, lz) with lz = 0 at the edge facing him maps to model (BLOCK_Z... ) x = 8 - lx, z = BLOCK_Z - 8 - lz.
const cx = (lx) => 8 - lx, cz = (lz) => BLOCK_Z - 8 - lz;
const ctlBox = (name, l0, l1, mat, o) => PC(name, [cx(l1[0]), l0[1], cz(l1[2])], [cx(l0[0]), l1[1], cz(l0[2])], mat, o);
// lever handle tips and the far ends of their rods, from Create 6.0.10's ControlsRenderer at rest (lever partial
// rotated, then the renderer's transform chain; see the scratch lever.js): first lever (his right), second (his left)
const LEVERS = {
  right: { tip: [3, 13.47, -9.82], deg: 20 },
  left: { tip: [-3, 16.79, -11.21], deg: 45 },
};
function leverRod(side) {
  const L = LEVERS[side], len = 10, a = (L.deg * Math.PI) / 180;
  // the rod runs away from him and down into the controls from the tip
  const end = [L.tip[0], L.tip[1] - len * Math.sin(a), L.tip[2] - len * Math.cos(a)];
  const mid = L.tip.map((v, i) => (v + end[i]) / 2);
  return [
    PC(`lever_rod_${side}`, [mid[0] - 0.5, mid[1] - 0.5, mid[2] - len / 2], [mid[0] + 0.5, mid[1] + 0.5, mid[2] + len / 2], 'steel', { rotation: [-L.deg, 0, 0], origin: mid }),
    PC(`lever_knob_${side}`, [L.tip[0] - 1, L.tip[1] - 1, L.tip[2] - 0.5], [L.tip[0] + 1, L.tip[1] + 1, L.tip[2] + 1.5], side === 'right' ? 'brass' : 'copper', { rotation: [-L.deg, 0, 0], origin: L.tip }),
  ];
}
const propControls = PG('prop_controls', [0, 0, 0], [
  ctlBox('controls_base', [0, 0, 4], [16, 2, 16], 'railCasing'),
  ctlBox('controls_lower', [2, 2, 8], [14, 7, 16], 'casing'),
  ctlBox('controls_desk', [2, 7, 7], [14, 15, 15], 'casing', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? mot('railCasing', x, y) : null) }),
  ctlBox('controls_side_r', [0, 2, 6], [2, 16, 16], 'railCasing'),
  ctlBox('controls_side_l', [14, 2, 6], [16, 16, 16], 'railCasing'),
  ctlBox('controls_back', [2, 7, 14], [14, 16, 16], 'railCasing'),
  ...leverRod('right'),
  ...leverRod('left'),
]);
// the solid block above him (for the duck clips), drawn as a plank ceiling
const propCeiling = PG('prop_ceiling', [0, 0, 0], [PC('ceiling_block', [-8, 16, BLOCK_Z - 8], [8, 32, BLOCK_Z + 8], 'plank', { paint: (f, i, j, w, h, x, y) => (i % 4 === 0 && f !== 'up' && f !== 'down' ? P.plank[0] : null) })]);
// a Stock Ticker stand-in, beside him on his right
const propTicker = PG('prop_ticker', [0, 0, 0], [
  PC('ticker_base', [9, 0, BLOCK_Z - 6], [21, 9, BLOCK_Z + 6], 'casing'),
  PC('ticker_top', [10, 9, BLOCK_Z - 5], [20, 10, BLOCK_Z + 5], 'brass'),
  PC('ticker_screen', [11, 3, BLOCK_Z - 6.25], [19, 8, BLOCK_Z - 6], 'glass', { paint: (f, i, j) => (f === 'north' && j === 2 && i > 0 && i < 7 ? P.cyanOff[1] : null) }),
]);
const PROPS = ['prop_controls', 'prop_ceiling', 'prop_ticker'];
const ROOTS = [bot, propControls, propCeiling, propTicker];

// ---------- build: pack UVs, paint, emit ----------
const tex = new Uint8Array(TEX * TEX * 4);
const cubes = [];
for (const root of ROOTS) (function collect(n) { if (n.kind === 'c') cubes.push(n); else n.kids.forEach(collect); })(root);
for (const c of cubes) {
  c.size = [0, 1, 2].map((a) => +(c.to[a] - c.from[a]).toFixed(4));
  const [w, h, d] = c.size;
  c.netW = Math.ceil(2 * (w + d));
  c.netH = Math.ceil(d + h);
}
const order = [...cubes].sort((a, b) => b.netH - a.netH || b.netW - a.netW);
let sx = 0, sy = 0, rowH = 0;
for (const c of order) {
  if (sx + c.netW > TEX) { sx = 0; sy += rowH; rowH = 0; }
  c.uv = [sx, sy];
  sx += c.netW;
  rowH = Math.max(rowH, c.netH);
}
if (sy + rowH > TEX) throw new Error('texture overflow: ' + (sy + rowH));
console.error('texture rows used:', sy + rowH, 'of', TEX);

function faceRects(c) {
  const [w, h, d] = c.size;
  const [u, v] = c.uv;
  return { east: [u, v + d, d, h], north: [u + d, v + d, w, h], west: [u + d + w, v + d, d, h], south: [u + 2 * d + w, v + d, w, h], up: [u + d, v, w, d], down: [u + d + w, v, w, d] };
}
function paintCube(c, uv, buf, size) {
  const R = faceRects({ size: c.size, uv });
  for (const f of Object.keys(R)) {
    const [fx, fy, fw, fh] = R[f];
    const x0 = Math.floor(fx), y0 = Math.floor(fy);
    const W = Math.max(1, Math.ceil(fx + fw) - x0), H = Math.max(1, Math.ceil(fy + fh) - y0);
    for (let j = 0; j < H; j++)
      for (let i = 0; i < W; i++) {
        const x = x0 + i, y = y0 + j;
        let col = c.paint ? c.paint(f, i, j, W, H, x, y) : null;
        if (col === null || col === undefined) col = mot(c.mat, x, y);
        const o = (y * size + x) * 4;
        if (col === 'clear') { buf[o + 3] = 0; continue; }
        const [r, g, b, a] = rgb(col);
        buf[o] = r; buf[o + 1] = g; buf[o + 2] = b; buf[o + 3] = a;
      }
  }
}
for (const c of cubes) paintCube(c, c.uv, tex, TEX);
function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; Buffer.from(data.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1); }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const pngBuf = png(TEX, TEX, tex);
fs.writeFileSync(path.resolve(path.dirname(OUT), 'helper-bot.png'), pngBuf);
// dyed copies of the texture (the dye zone repainted in each colour), for review and for the game's tint check
const DYE_DIR = path.resolve(path.dirname(OUT), 'helper-bot-dyes');
fs.mkdirSync(DYE_DIR, { recursive: true });
const dyeTextures = {};
for (const [name, hex] of Object.entries(DYES)) {
  const buf = Uint8Array.from(tex);
  P.panel = dyeShades(hex);
  for (const c of cubes) if (c.dye) paintCube(c, c.uv, buf, TEX);
  const b = png(TEX, TEX, buf);
  fs.writeFileSync(path.join(DYE_DIR, name + '.png'), b);
  dyeTextures[name] = b;
}
P.panel = P.andesite;

const elements = [], groups = [], groupByName = {};
function emit(n) {
  if (n.kind === 'c') {
    n.uuid = uuid();
    const R = faceRects(n);
    const faces = {};
    for (const f of Object.keys(R)) { const [x, y, w, h] = R[f]; faces[f] = { uv: [x, y, x + w, y + h], texture: 0 }; }
    elements.push({
      name: n.name, box_uv: true, render_order: 'default', locked: false, export: true, allow_mirror_modeling: true,
      from: n.from, to: n.to, autouv: 0, color: 0, rotation: n.rotation || [0, 0, 0],
      origin: n.origin || [(n.from[0] + n.to[0]) / 2, (n.from[1] + n.to[1]) / 2, (n.from[2] + n.to[2]) / 2],
      uv_offset: n.uv, faces, type: 'cube', uuid: n.uuid,
    });
    return n.uuid;
  }
  n.uuid = uuid();
  groupByName[n.name] = n;
  groups.push({ name: n.name, uuid: n.uuid, export: true, locked: false, visibility: !n.hidden, origin: n.origin, rotation: n.rot, color: 0, children: [], reset: false, shade: true, mirror_uv: false, autouv: 0, isOpen: false });
  return { uuid: n.uuid, isOpen: ['helper_bot', 'swivel', 'post', 'body'].includes(n.name), children: n.kids.map(emit) };
}
const outliner = ROOTS.map(emit);

module.exports = { DYES, dyeShades, P, G, C, bot, cubes, groupByName, elements, groups, outliner, pngBuf, dyeTextures, uuid, TEX, OUT, PROPS, PROP_DROP, LEVERS, BLOCK_Z, png, paintCube, ARGS };
if (require.main === module) require('./helper-bot.clips.js');
