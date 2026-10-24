// Trade Bot + station generator -> design/models/trade-bot.bbmodel (modded_entity, box UV)
// Front of the bot is -Z, bot's right is +X. Rotations right-handed (rx+ tips top to +Z, rz+ tips top to -X, ry+ turns front to -X).
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');
const path = require('path');

const OUT = process.argv[2] || path.resolve(__dirname, 'trade-bot.bbmodel');
// approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files
const SOUND_DIR = path.resolve(__dirname, '../sound');
const CUES = fs.existsSync(path.join(SOUND_DIR, 'trade-bot.cues.json')) ? JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'trade-bot.cues.json'), 'utf8')) : null;
const TEX = 256; // props included for the preview; the in-game entity texture is generated without them
const uuid = () => crypto.randomUUID();

// ---------- palette ----------
const P = {
  steel: ['#2f343b', '#3c424a', '#4a5059'],
  steelHi: ['#5a616b'],
  andesite: ['#6b6e6a', '#7c7f79', '#8a8c86'],
  brass: ['#8f7638', '#b08f45', '#c9a75a'],
  brassDark: ['#6e5a2a'],
  copper: ['#7a4a2e', '#9c5f3a', '#a96a42'],
  rubber: ['#26272a', '#323337'],
  glass: ['#14181c', '#1c2228'],
  glassHi: ['#2c3540'],
  amber: ['#d99a4a', '#d0903f'],
  amberHi: ['#e8b56a'],
  catch: ['#ecd7a6'],
  amberOff: ['#3e2e22', '#4a3524'],
  emerald: ['#4f9a68'],
  emeraldHi: ['#6fb184'],
  emeraldOff: ['#24382c'],
  gold: ['#b08f45'],
  paper: ['#8a8170'],
  walnut: ['#3b2a1f', '#4e3626', '#5a3f2c'],
  cardboard: ['#6e5638', '#7d6446', '#86694a'],
  tape: ['#a08a62'],
  cobble: ['#5e5f5c', '#6f706c', '#7f807b'],
  hollow: ['#16110d', '#1d1612'],
  villagerSkin: ['#7d5f4b', '#8a6a55', '#94735c'],
  villagerNose: ['#6e5241'],
  villagerRobe: ['#4e3b2d', '#5b4636', '#654e3c'],
  villagerEye: ['#3f6b48'],
  villagerBrow: ['#3a2a20'],
  cushion: ['#6e3530', '#7a3d36', '#84453d'],
};
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

// face painters
const framed = (inner = 'andesite', edge = 'steel') => (f, i, j, w, h, x, y) => {
  if (f === 'up' || f === 'down') return null;
  return i === 0 || j === 0 || i === w - 1 || j === h - 1 ? mot(edge, x, y) : mot(inner, x, y);
};
const topFramed = (inner = 'andesite', edge = 'steel') => (f, i, j, w, h, x, y) => {
  return i === 0 || j === 0 || i === w - 1 || j === h - 1 ? mot(edge, x, y) : mot(inner, x, y);
};

// ---------- Trade Bot ----------
function eyePod(side) {
  // side: +1 right (+X), -1 left (-X)
  const sx = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const [x0, x1] = sx(1, 7);
  const cx = (x0 + x1) / 2;
  const kids = [
    C(`pod_${side > 0 ? 'right' : 'left'}`, [x0, 17, -3], [x1, 22, 2], 'steel', {
      paint: (f, i, j, w, h, x, y) => {
        if (f === 'north') {
          if (i >= 1 && i <= 4 && j >= 1 && j <= 3) {
            if ((i === 1 && j === 1) || (i === 2 && j === 1)) return mot('glassHi', x, y);
            return mot('glass', x, y);
          }
          return mot('steel', x, y);
        }
        // a bolt on the outer side
        if ((f === 'east' && side > 0) || (f === 'west' && side < 0)) {
          if (i === 2 && j === 2) return P.brassDark[0];
          if (j === 0) return mot('steelHi', x, y);
        }
        if (f === 'up') return mot('steelHi', x, y) && (hash(x, y, 5) < 0.5 ? P.steel[2] : P.steel[1]);
        return null;
      },
    }),
    C(`rim_${side > 0 ? 'right' : 'left'}`, [x0, 17, -4], [x1, 22, -3], 'brass', {
      paint: (f, i, j, w, h, x, y) => {
        if (f === 'north' || f === 'south') {
          if (i >= 1 && i <= 4 && j >= 1 && j <= 3) return 'clear';
          if (j === h - 1) return mot('brassDark', x, y);
        }
        return null;
      },
    }),
    G(`iris_${side > 0 ? 'right' : 'left'}`, [cx, 19.5, -3], [
      C(`iris_dark_${side > 0 ? 'right' : 'left'}`, [cx - 1, 18.5, -3.5], [cx + 1, 20.5, -2.5], 'amberOff', {
        paint: (f, i, j) => (f === 'north' && i === 0 && j === 0 ? '#6b5038' : null),
      }),
      G(`iris_glow_${side > 0 ? 'right' : 'left'}`, [cx, 19.5, -3], [
        C(`iris_lit_${side > 0 ? 'right' : 'left'}`, [cx - 1, 18.5, -3.6], [cx + 1, 20.5, -2.6], 'amber', {
          paint: (f, i, j) => {
            if (f !== 'north') return null;
            if (i === 0 && j === 0) return P.catch[0];
            if (i === 1 && j === 1) return P.amberHi[0];
            return P.amber[0];
          },
        }),
      ]),
    ]),
    // big front lid plate (user's pick): slides on Y (+1 wide, 0 neutral, -2 shut) and tilts on Z for brow expressions
    G(`lid_${side > 0 ? 'right' : 'left'}`, [cx, 21.5, -4.5], [
      C(`lid_plate_${side > 0 ? 'right' : 'left'}`, [x0, 20, -5], [x1, 23, -4], 'steel', {
        paint: (f, i, j, w, h, x, y) => (f === 'north' && j === h - 1 ? mot('steelHi', x, y) : null),
      }),
    ]),
  ];
  if (side < 0) {
    kids.push(
      G('aerial', [-5.5, 22, 1], [
        C('aerial_rod', [-5.75, 22, 0.75], [-5.25, 23.25, 1.25], 'copper'),
        C('aerial_bead', [-6, 23, 0.5], [-5, 24, 1.5], 'brass'),
      ])
    );
  }
  return G(`eye_${side > 0 ? 'right' : 'left'}`, [side, 19.5, -0.5], kids);
}

// a little Create-style cardboard package, 3 px, bottom-centred on `at`, with a tape band
function packageCube(name, at) {
  return C(name, [at[0] - 1.5, at[1], at[2] - 1.5], [at[0] + 1.5, at[1] + 3, at[2] + 1.5], 'cardboard', {
    paint: (f, i, j, w, h, x, y) => (i === 1 && f !== 'east' && f !== 'west' ? P.tape[0] : null),
  });
}

function arm(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const ax = X(5, 7);
  const cogX = X(7, 7.5);
  const px = 6 * side;
  return G(`arm_${s}`, [px, 14, 0], [
    C(`upper_arm_${s}`, [ax[0], 10, -1], [ax[1], 15, 1], 'steel', {
      paint: (f, i, j, w, h, x, y) => (f === 'up' ? mot('steelHi', x, y) : null),
    }),
    C(`shoulder_cog_${s}`, [cogX[0], 12.5, -1.5], [cogX[1], 15.5, 1.5], 'brass', {
      paint: (f, i, j) => ((f === 'east' || f === 'west') && i === 1 && j === 1 ? P.brassDark[0] : null),
    }),
    C(`shoulder_cog_teeth_${s}`, [cogX[0] + (side > 0 ? -0.01 : 0.01), 12.5, -1.5], [cogX[1] + (side > 0 ? -0.01 : 0.01), 15.5, 1.5], 'brassDark', {
      rotation: [45, 0, 0], origin: [(cogX[0] + cogX[1]) / 2, 14, 0],
    }),
    G(`forearm_${s}`, [px, 10, 0], [
      C(`forearm_shell_${s}`, [ax[0], 6, -1], [ax[1], 10, 1], 'andesite', {
        paint: (f, i, j, w, h, x, y) => (j === 0 && f !== 'up' && f !== 'down' ? mot('steel', x, y) : null),
      }),
      G(`claw_${s}`, [px, 6, 0], [
        C(`palm_${s}`, [ax[0], 5, -1], [ax[1], 6, 1], 'brass'),
        C(`prong_front_${s}`, [px - 0.5, 3, -1], [px + 0.5, 5, -0.25], 'brass', {
          paint: (f, i, j, w, h, x, y) => (j === h - 1 ? mot('brassDark', x, y) : null),
        }),
        C(`prong_back_${s}`, [px - 0.5, 3, 0.25], [px + 0.5, 5, 1], 'brass', {
          paint: (f, i, j, w, h, x, y) => (j === h - 1 ? mot('brassDark', x, y) : null),
        }),
        // the item he's carrying (right claw only; hidden when empty)
        ...(side > 0 ? [G('held_item', [px, 1.5, 0], [packageCube('held_package', [px, 0, 0])])] : []),
      ]),
    ]),
  ]);
}

const hubPaint = (f, i, j, w, h) => {
  if (f !== 'east' && f !== 'west') return null;
  if (i === 1 && j === 1) return P.steel[0];
  if (i === 1 || j === 1) return P.brassDark[0];
  return null;
};
const treadPaint = (f, i, j, w, h, x, y) => {
  if (f === 'east' || f === 'west') return null;
  return j % 2 === 0 && (f === 'north' || f === 'south') ? P.rubber[1] : i % 2 === 0 && (f === 'up' || f === 'down') ? P.rubber[1] : P.rubber[0];
};

const bot = G('trade_bot', [0, 0, 0], [
  G('wheel', [0, 4, 0], [
    C('tyre_a', [-1.5, 0, -2], [1.5, 8, 2], 'rubber', { paint: treadPaint }),
    C('tyre_b', [-1.5, 2, -4], [1.5, 6, 4], 'rubber', { paint: treadPaint }),
    C('tyre_c', [-1.4, 1, -3], [1.4, 7, 3], 'rubber'),
    C('hub_right', [1.5, 2.5, -1.5], [2.5, 5.5, 1.5], 'brass', { paint: hubPaint }),
    C('hub_left', [-2.5, 2.5, -1.5], [-1.5, 5.5, 1.5], 'brass', { paint: hubPaint }),
  ]),
  G('body', [0, 4, 0], [
    C('fork_right', [2.5, 3, -1], [3.5, 7.5, 1], 'copper'),
    C('fork_left', [-3.5, 3, -1], [-2.5, 7.5, 1], 'copper'),
    C('axle_nut_right', [3.5, 3.5, -0.5], [4, 4.5, 0.5], 'brass'),
    C('axle_nut_left', [-4, 3.5, -0.5], [-3.5, 4.5, 0.5], 'brass'),
    C('chassis', [-4, 7, -4], [4, 16, 4], 'andesite', {
      paint: (f, i, j, w, h, x, y) => {
        if (f === 'up' || f === 'down') return mot('steel', x, y);
        return framed()(f, i, j, w, h, x, y);
      },
    }),
    C('side_panel_right', [4, 8, -3], [5, 15, 3], 'andesite', { paint: (f, i, j, w, h, x, y) => (f === 'east' ? framed()(f, i, j, w, h, x, y) : mot('steel', x, y)) }),
    C('side_panel_left', [-5, 8, -3], [-4, 15, 3], 'andesite', { paint: (f, i, j, w, h, x, y) => (f === 'west' ? framed()(f, i, j, w, h, x, y) : mot('steel', x, y)) }),
    C('waist_band', [-4.5, 11, -4.5], [4.5, 12, 4.5], 'brass', {
      paint: (f, i, j, w, h, x, y) => ((f === 'north' || f === 'south') && i % 2 === 1 ? mot('brassDark', x, y) : null),
    }),
    C('top_cap', [-3, 16, -3], [3, 16.5, 3], 'steel'),
    G('drawer', [0, 9.5, -4], [
      C('drawer_box', [-3, 8, -5], [3, 11, 1], 'steel', {
        paint: (f, i, j, w, h, x, y) => {
          if (f === 'north') return j === h - 1 ? mot('steel', x, y) : mot('andesite', x, y);
          if (f === 'up') {
            // wares, seen from above
            if (i === 0 || i === w - 1 || j === 0 || j === h - 1) return mot('steel', x, y);
            const wares = { '1,4': 'emerald', '2,4': 'emeraldHi', '2,3': 'emerald', '4,4': 'gold', '4,3': 'brass', '3,2': 'paper', '4,2': 'paper', '1,2': 'copper', '1,1': 'copper' };
            const k = wares[`${i},${j}`];
            return k ? P[k][0] : P.steel[0];
          }
          return null;
        },
      }),
      C('drawer_handle', [-1, 9, -6], [1, 10, -5], 'brass'),
    ]),
    G('lamp', [2.5, 13.5, -4], [
      C('lamp_dark', [2, 13, -4.5], [3, 14, -3.5], 'emeraldOff'),
      G('lamp_glow', [2.5, 13.5, -4.5], [C('lamp_lit', [2, 13, -4.6], [3, 14, -3.6], 'emerald', { paint: (f) => (f === 'north' ? P.emeraldHi[0] : null) })]),
    ]),
    G('key', [0, 12, 4.5], [
      C('key_shaft', [-0.5, 11.5, 4], [0.5, 12.5, 5], 'brass'),
      C('key_bow', [-2.5, 10.5, 5], [2.5, 13.5, 6], 'brass', {
        paint: (f, i, j, w, h, x, y) => {
          if ((f === 'north' || f === 'south') && j === 1 && (i === 1 || i === 3)) return 'clear';
          // shaded top and bottom edges, so the bow looks the same after a half turn (loops turn it 180°)
          if ((j === 0 || j === h - 1) && f !== 'up' && f !== 'down') return mot('brassDark', x, y);
          return null;
        },
      }),
    ]),
    arm(1),
    arm(-1),
    G('neck', [0, 16, 0], [
      C('neck_tube', [-1, 12.5, -1], [1, 18.5, 1], 'steel', { paint: (f, i, j, w, h, x, y) => (j === 0 && f !== 'up' && f !== 'down' ? mot('brass', x, y) : null) }),
      G('head', [0, 18, 0], [
        C('bridge', [-1, 18.5, -1.5], [1, 20.5, 0.5], 'steel'),
        eyePod(1),
        eyePod(-1),
      ]),
    ]),
  ]),
]);

// ---------- Station (station-local coords, shifted so the docked bot sits at the origin) ----------
const SH = [0, -2, 1.5];
const st = (v) => [v[0] + SH[0], v[1] + SH[1], v[2] + SH[2]];
const SC = (name, from, to, mat, o = {}) => C(name, st(from), st(to), mat, { ...o, origin: o.origin ? st(o.origin) : undefined });
const SG = (name, origin, kids, o) => G(name, st(origin), kids, o);

const plinthPaint = (f, i, j, w, h, x, y) => {
  if (f === 'down') return mot('steel', x, y);
  if (f === 'up') return topFramed()(f, i, j, w, h, x, y);
  return framed()(f, i, j, w, h, x, y);
};
const station = SG('station', [0, 0, 0], [
  SG('station_base', [0, 0, 0], [
    SC('plinth_core', [-7, 0, -6], [7, 2, 6], 'andesite', {
      paint: (f, i, j, w, h, x, y) => (f === 'up' ? mot('rubber', x, y) : plinthPaint(f, i, j, w, h, x, y)),
    }),
    SC('plinth_front', [-6, 0, -7], [6, 4, -6], 'andesite', {
      paint: (f, i, j, w, h, x, y) => {
        // ember window, seen from the front: x -2..2 -> i 4..7, y 1..3 -> rows 1..2
        if (f === 'north' && i >= 4 && i <= 7 && (j === 1 || j === 2)) return mot('amberOff', x, y);
        return plinthPaint(f, i, j, w, h, x, y);
      },
    }),
    SC('plinth_back', [-6, 0, 6], [6, 4, 7], 'andesite', { paint: plinthPaint }),
    SC('deck_left', [-7, 2, -6], [-2, 4, 6], 'andesite', { paint: plinthPaint }),
    SC('deck_right', [2, 2, -6], [7, 4, 6], 'andesite', { paint: plinthPaint }),
    SC('deck_front', [-2, 2, -6], [2, 4, -5], 'andesite', { paint: plinthPaint }),
    SC('deck_back', [-2, 2, 2], [2, 4, 6], 'andesite', { paint: plinthPaint }),
    SC('post', [-1.5, 4, 5], [1.5, 15, 7], 'steel', {
      paint: (f, i, j, w, h, x, y) => {
        if ((f === 'north' || f === 'south') && i === 1 && j > 0 && j < h - 1) return mot('andesite', x, y);
        if (j === 1 && f !== 'up' && f !== 'down') return mot('brass', x, y);
        return null;
      },
    }),
  ]),
  SG('post_cog', [-1.75, 11, 6], [
    SC('post_cog_a', [-2.25, 9.5, 4.5], [-1.5, 12.5, 7.5], 'brass', {
      paint: (f, i, j) => ((f === 'east' || f === 'west') && i === 1 && j === 1 ? P.brassDark[0] : null),
    }),
    SC('post_cog_b', [-2.24, 9.5, 4.5], [-1.51, 12.5, 7.5], 'brassDark', { rotation: [45, 0, 0], origin: [-1.75, 11, 6] }),
  ]),
  SG('winder_arm', [2, 14, 5], [
    SC('winder_knuckle', [1.5, 13, 4.5], [2.5, 15, 5.5], 'brass'),
    SC('winder_bar', [0.75, 13.5, 4.5], [1.6, 14.5, 5], 'brass'),
    SG('winder_head', [0, 14, 4.7], [
      SC('winder_socket', [-1, 13, 4.4], [1, 15, 5], 'brass', {
        paint: (f, i, j) => (f === 'north' && (i === 0 ? j === 1 : i === 1 && j === 0) ? P.brassDark[0] : null),
      }),
    ]),
  ], { rot: [0, 90, 0] }),
  SG('signal_flag', [0, 15, 6], [
    SC('flag_pole', [-0.25, 15, 5.75], [0.25, 18.5, 6.25], 'steel'),
    SC('flag_plate', [0.25, 16.75, 5.9], [2.25, 18.25, 6.1], 'brass', { paint: (f, i, j, w, h, x, y) => (j === h - 1 ? mot('brassDark', x, y) : null) }),
  ], { rot: [0, 0, -90] }),
  SG('station_glow', [0, 3, -1.5], [
    SC('glow_left', [-2.75, 4, -5.75], [-2, 4.25, 2.75], 'amber'),
    SC('glow_right', [2, 4, -5.75], [2.75, 4.25, 2.75], 'amber'),
    SC('glow_front', [-2, 4, -5.75], [2, 4.25, -5], 'amber'),
    SC('glow_back', [-2, 4, 2], [2, 4.25, 2.75], 'amber'),
    SC('glow_window', [-2, 1, -7.25], [2, 3, -7], 'amber', { paint: (f, i, j) => (f === 'north' && j === 0 ? P.amberHi[0] : null) }),
  ]),
]);

// ---------- stair prop for the stairs clips (rests off to the side at x -40; the clips move it in front of him) ----------
const STAIR_REST_X = -40;
const stairsProp = G('stairs_prop', [0, 0, 0], [
  C('stair_low', [STAIR_REST_X - 8, 0, -26], [STAIR_REST_X + 8, 8, -10], 'andesite', { paint: plinthPaint }),
  C('stair_high', [STAIR_REST_X - 8, 8, -26], [STAIR_REST_X + 8, 16, -18], 'andesite', { paint: plinthPaint }),
]);

// ---------- item-handling props: built where the clips use them, then parked 200 px below (the clips lift them back) ----------
const PROP_DROP = 200;
const pv = (v) => [v[0], v[1] - PROP_DROP, v[2]];
const PC = (name, from, to, mat, o = {}) => C(name, pv(from), pv(to), mat, { ...o, origin: o.origin ? pv(o.origin) : undefined });
const PG = (name, origin, kids, o) => G(name, pv(origin), kids, o);
const PPkg = (name, at) => { const c = packageCube(name, pv(at)); return c; };
const woodBands = (rows) => (f, i, j, w, h, x, y) => (f !== 'up' && f !== 'down' && rows.includes(j) ? mot('steel', x, y) : i % 4 === 0 && f !== 'up' && f !== 'down' ? P.walnut[0] : null);
const hollowTop = (f, i, j, w, h, x, y) => (f === 'up' ? (i === 0 || j === 0 || i === w - 1 || j === h - 1 ? mot('walnut', x, y) : mot('hollow', x, y)) : null);
// chest: front faces the bot (+Z side at z -7); lid hinged along the back top edge
const propChest = PG('prop_chest', [0, 0, 0], [
  PC('chest_base', [-7, 0, -21], [7, 10, -7], 'walnut', { paint: (f, ...r) => hollowTop(f, ...r) || woodBands([0, 9])(f, ...r) }),
  PG('chest_item', [5, 10, -13], [PPkg('chest_package', [5, 10, -13])]),
  PG('chest_lid', [0, 10, -21], [
    PC('chest_lid_box', [-7, 10, -21], [7, 14, -7], 'walnut', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? (i === 0 || i === w - 1 ? mot('steel', x, y) : null) : f === 'down' ? mot('hollow', x, y) : woodBands([3])(f, i, j, w, h, x, y)) }),
    PC('chest_latch', [-1, 9, -6.75], [1, 12, -6.25], 'brass', { paint: (f, i, j) => (f === 'south' && j === 1 ? P.brassDark[0] : null) }),
  ]),
  PG('junk', [3, 11, -14], [PC('junk_cobble', [2, 10, -15], [4, 12, -13], 'cobble')]),
]);
// barrel: a hollow rim around a floor, lid plate hinged at the back
const propBarrel = PG('prop_barrel', [0, 0, 0], [
  PC('barrel_floor', [-8, 0, -34], [8, 11, -18], 'walnut', { paint: (f, ...r) => hollowTop(f, ...r) || woodBands([1, 9])(f, ...r) }),
  PC('barrel_wall_front', [-8, 11, -19], [8, 15, -18], 'walnut', { paint: woodBands([2]) }),
  PC('barrel_wall_back', [-8, 11, -34], [8, 15, -33], 'walnut', { paint: woodBands([2]) }),
  PC('barrel_wall_left', [-8, 11, -33], [-7, 15, -19], 'walnut', { paint: woodBands([2]) }),
  PC('barrel_wall_right', [7, 11, -33], [8, 15, -19], 'walnut', { paint: woodBands([2]) }),
  PG('barrel_item', [4, 11, -22], [PPkg('barrel_package', [4, 11, -22])]),
  // closed top: swapped out (scale 0) when the barrel is open, revealing the dark hollow, like the vanilla open texture
  PG('barrel_top_closed', [0, 15.5, -26], [PC('barrel_top_plate', [-8, 15, -34], [8, 16, -18], 'walnut', { paint: (f, i, j, w, h, x, y) => (f === 'up' && (j % 4 === 0) ? P.walnut[0] : f === 'up' && (i === 0 || i === w - 1) ? mot('steel', x, y) : null) })]),
]);
// high chest: a chest sitting on an andesite block, too high for him to see into
const propChestHigh = PG('prop_chest_high', [0, 0, 0], [
  PC('chest_high_stand', [-8, 0, -32], [8, 16, -16], 'andesite', { paint: plinthPaint }),
  PC('chest_high_base', [-7, 16, -31], [7, 26, -17], 'walnut', { paint: (f, ...r) => hollowTop(f, ...r) || woodBands([0, 9])(f, ...r) }),
  PG('chest_high_lid', [0, 26, -31], [
    PC('chest_high_lid_box', [-7, 26, -31], [7, 30, -17], 'walnut', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? (i === 0 || i === w - 1 ? mot('steel', x, y) : null) : f === 'down' ? mot('hollow', x, y) : woodBands([3])(f, i, j, w, h, x, y)) }),
    PC('chest_high_latch', [-1, 25, -16.75], [1, 28, -16.25], 'brass', { paint: (f, i, j) => (f === 'south' && j === 1 ? P.brassDark[0] : null) }),
  ]),
]);
// villager trading scene: a workstation one block in front of him, and a villager sitting on a Create-style seat
// behind it, facing him. The thrown package and emerald are props too (their flights are animated per clip).
const villagerFace = (f, i, j, w, h, x, y) => {
  if (f !== 'south') return null; // the villager faces +Z (toward the bot)
  if (j === 3 && (i === 1 || i === 6)) return P.villagerEye[0];
  if (j === 3 && (i === 2 || i === 5)) return '#d8d2c4';
  if (j === 2 && i >= 1 && i <= 6) return P.villagerBrow[0];
  return null;
};
const propVillager = PG('prop_villager', [0, 0, 0], [
  PG('workstation', [0, 8, -22], [
    PC('workstation_block', [-8, 0, -30], [8, 16, -14], 'walnut', { paint: (f, i, j, w, h, x, y) => (f === 'up' ? (j % 4 === 0 ? P.walnut[0] : mot('villagerRobe', x, y)) : j === 0 || j === 1 ? mot('villagerRobe', x, y) : woodBands([6, 15])(f, i, j, w, h, x, y)) }),
  ]),
  PC('seat_base', [-8, 0, -48], [8, 6, -32], 'andesite', { paint: plinthPaint }),
  PC('seat_cushion', [-7, 6, -47], [7, 8, -33], 'cushion'),
  PC('villager_thigh_left', [-4, 8, -36], [0, 12, -30], 'villagerRobe'),
  PC('villager_thigh_right', [0, 8, -36], [4, 12, -30], 'villagerRobe'),
  PC('villager_shin_left', [-4, 0, -32], [0, 8, -30.01], 'villagerRobe'),
  PC('villager_shin_right', [0, 0, -32], [4, 8, -30.01], 'villagerRobe'),
  PC('villager_body', [-4, 8, -42], [4, 20, -36], 'villagerRobe'),
  PC('villager_arms', [-4, 14, -36], [4, 18, -32], 'villagerRobe', { paint: (f, i, j, w, h, x, y) => (f === 'south' && (i === 0 || i === w - 1) ? mot('villagerSkin', x, y) : null) }),
  PG('villager_head', [0, 20, -39], [
    PC('villager_skull', [-4, 20, -43], [4, 30, -35], 'villagerSkin', { paint: villagerFace }),
    PC('villager_nose', [-1, 21, -35], [1, 25, -33], 'villagerSkin', { paint: (f, i, j, w, h, x, y) => mot('villagerNose', x, y) }),
  ]),
  PG('trade_package', [0, 19.5, -34], [packageCube('trade_package_box', pv([0, 18, -34]))]),
  PG('trade_emerald', [0, 19, -34], [PC('trade_emerald_gem', [-1, 18, -34.5], [1, 20, -33.5], 'emerald', { paint: (f, i, j) => (i === 0 && j === 0 ? P.emeraldHi[0] : null) })]),
]);
// depot: andesite casing with a dark steel plate on top
const propDepot = PG('prop_depot', [0, 0, 0], [
  PC('depot_base', [-8, 0, -24], [8, 9, -8], 'andesite', { paint: plinthPaint }),
  PC('depot_plate', [-7, 9, -23], [7, 10, -9], 'steel', { paint: (f, i, j, w, h, x, y) => (f === 'up' && (i === 0 || j === 0 || i === w - 1 || j === h - 1) ? mot('brass', x, y) : null) }),
  PG('depot_item', [6, 10, -11], [PPkg('depot_package', [6, 10, -11])]),
]);
// belt: runs left-right in front of him, rubber on steel rails, casing at both ends
const propBelt = PG('prop_belt', [0, 0, 0], [
  PC('belt_rubber', [-24, 3, -20], [24, 6, -8], 'rubber', { paint: (f, i, j, w, h, x, y) => (f === 'up' && i % 4 === 0 ? P.rubber[1] : null) }),
  PC('belt_rail_front', [-24, 2, -8], [24, 5, -7.5], 'steel'),
  PC('belt_rail_back', [-24, 2, -20.5], [24, 5, -20], 'steel'),
  PC('belt_end_left', [-28, 0, -21], [-24, 7, -7], 'andesite', { paint: plinthPaint }),
  PC('belt_end_right', [24, 0, -21], [28, 7, -7], 'andesite', { paint: plinthPaint }),
  PG('belt_item', [6, 6, -12], [PPkg('belt_package', [6, 6, -12])]),
]);
const propSlab = PG('prop_slab', [0, 0, 0], [PC('slab_block', [-8, 0, -26], [8, 8, -10], 'andesite', { paint: plinthPaint })]);
const propBlock = PG('prop_block', [0, 0, 0], [PC('full_block', [-8, 0, -26], [8, 16, -10], 'andesite', { paint: plinthPaint })]);
const ITEM_PROPS = ['prop_chest', 'prop_barrel', 'prop_depot', 'prop_belt', 'prop_slab', 'prop_block', 'prop_chest_high', 'prop_villager'];
// which prop each clip shows (all others are hidden)
const CLIP_PROPS = { chest_take: 'prop_chest', chest_put: 'prop_chest', barrel_take: 'prop_barrel', barrel_put: 'prop_barrel', depot_place: 'prop_depot', depot_take: 'prop_depot', belt_place: 'prop_belt', hop_on_slab: 'prop_slab', hop_off_slab: 'prop_slab', hop_on_block: 'prop_block', hop_off_block: 'prop_block', chest_take_high: 'prop_chest_high', chest_put_high: 'prop_chest_high', trade_villager: 'prop_villager', trade_villager_open: 'prop_villager' };

// ---------- build: pack UVs, paint, emit ----------
const tex = new Uint8Array(TEX * TEX * 4);
const cubes = [];
for (const root of [bot, station, stairsProp, propChest, propBarrel, propDepot, propBelt, propSlab, propBlock, propChestHigh, propVillager])
  (function collect(n) {
    if (n.kind === 'c') cubes.push(n);
    else n.kids.forEach(collect);
  })(root);

for (const c of cubes) {
  c.size = [0, 1, 2].map((a) => +(c.to[a] - c.from[a]).toFixed(4));
  const [w, h, d] = c.size;
  c.netW = Math.ceil(2 * (w + d));
  c.netH = Math.ceil(d + h);
}
// shelf pack, tallest first
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
  return {
    east: [u, v + d, d, h],
    north: [u + d, v + d, w, h],
    west: [u + d + w, v + d, d, h],
    south: [u + 2 * d + w, v + d, w, h],
    up: [u + d, v, w, d],
    down: [u + d + w, v, w, d],
  };
}
for (const c of cubes) {
  const R = faceRects(c);
  for (const f of Object.keys(R)) {
    const [fx, fy, fw, fh] = R[f];
    const x0 = Math.floor(fx), y0 = Math.floor(fy);
    const W = Math.max(1, Math.ceil(fx + fw) - x0), H = Math.max(1, Math.ceil(fy + fh) - y0);
    for (let j = 0; j < H; j++)
      for (let i = 0; i < W; i++) {
        const x = x0 + i, y = y0 + j;
        let col = c.paint ? c.paint(f, i, j, W, H, x, y) : null;
        if (col === null || col === undefined) col = mot(c.mat, x, y);
        const o = (y * TEX + x) * 4;
        if (col === 'clear') { tex[o + 3] = 0; continue; }
        const [r, g, b, a] = rgb(col);
        tex[o] = r; tex[o + 1] = g; tex[o + 2] = b; tex[o + 3] = a;
      }
  }
}

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
fs.writeFileSync(path.resolve(path.dirname(OUT), 'trade-bot.png'), pngBuf);

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
  return { uuid: n.uuid, isOpen: n.name === 'trade_bot' || n.name === 'body', children: n.kids.map(emit) };
}
const outliner = [emit(bot), emit(station), emit(stairsProp), emit(propChest), emit(propBarrel), emit(propDepot), emit(propBelt), emit(propSlab), emit(propBlock), emit(propChestHigh), emit(propVillager)];

// ---------- animation helpers ----------
const FPS = 20;
const r2 = (v) => Math.round(v * 1000) / 1000;
const smooth = (t) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
const seg = (t, a, b) => smooth((t - a) / (b - a)); // 0..1 eased over [a,b]
const lin = (t, a, b) => (t <= a ? 0 : t >= b ? 1 : (t - a) / (b - a));
const sin = Math.sin, PI = Math.PI;
// damped spring response to a unit step at t0: overshoots and settles
const springStep = (t, t0, freq = 6, damp = 4) => { if (t < t0) return 0; const u = t - t0; return 1 - Math.exp(-damp * u) * (Math.cos(2 * PI * freq * u / 4) + (damp / (2 * PI * freq / 4)) * Math.sin(2 * PI * freq * u / 4)); };
// key between values with springy arrival
const ease = (t, keys, freq, damp) => { let v = keys[0][1]; for (let k = 1; k < keys.length; k++) v += (keys[k][1] - keys[k - 1][1]) * (freq ? springStep(t, keys[k][0], freq, damp) : seg(t, keys[k][0], keys[k][0] + (keys[k][2] || 0.25))); return v; };

// generic 2nd-order simulator: returns sampler(t)
function sim(len, accFn, dt = 1 / 600) {
  const n = Math.ceil(len / dt) + 2; const ys = new Float64Array(n); let y = 0, v = 0;
  for (let k = 0; k < n; k++) { ys[k] = y; const t = k * dt; const a = accFn(t, y, v); v += a * dt; y += v * dt; }
  return (t) => { const f = t / dt; const k = Math.min(n - 2, Math.max(0, Math.floor(f))); return ys[k] + (ys[k + 1] - ys[k]) * (f - k); };
}
const deriv = (f, h = 1 / 240) => (t) => (f(t + h) - f(t - h)) / (2 * h);
const CLEAR_CHECKS = []; // [name, tracks, length, boxes], checked once the clearance helper exists
const landKickAt = (t, tl, f = 2.2, d = 6) => (t < tl ? 0 : Math.exp(-d * (t - tl)) * Math.sin(2 * PI * f * (t - tl)));
const env = (t, a, b, c, d) => seg(t, a, b) * (1 - seg(t, c, d));
const hopArc = (t, a, b) => (t > a && t < b ? Math.sin(PI * lin(t, a, b)) : 0);

const animations = [];
const ALL_CLIPS = []; // [name, tracks, length] for checks that run once everything is defined
function anim(name, length, loop, tracks, steps = {}) {
  ALL_CLIPS.push([name, tracks, length]);
  const animators = {};
  const add = (bone, channel, kf) => {
    const g = groupByName[bone]; if (!g) throw new Error('no bone ' + bone);    animators[g.uuid] = animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
    animators[g.uuid].keyframes.push(kf);
  };
  for (const bone of Object.keys(tracks)) for (const ch of Object.keys(tracks[bone])) {
    const fn = tracks[bone][ch];
    // looping clips: linear keys at 30 fps, because Catmull-Rom has no neighbour across the loop point and eases
    // every motion to a near-stop there (a visible hitch each cycle)
    const fps = loop === 'loop' ? 30 : FPS, interp = loop === 'loop' ? 'linear' : 'catmullrom';
    const n = Math.round(length * fps);
    const samples = [];
    for (let k = 0; k <= n; k++) { const t = k / fps; samples.push([t, fn(t)]); }
    for (const [t, v] of samples) add(bone, ch, { channel: ch, data_points: [{ x: String(r2(v[0])), y: String(r2(v[1])), z: String(r2(v[2])) }], uuid: uuid(), time: r2(t), color: -1, interpolation: interp });
  }
  // clips that don't use the dock park the station beside him, sitting on his ground level
  if (!['dock_lights_out', 'action_comes_in'].includes(name)) steps = { ...steps, station: { position: [[0, [22, 2, 6]]] }, station_glow: { scale: [[0, [0, 0, 0]]] } };
  // the stair prop only shows in the stairs clips
  steps = { ...steps, stairs_prop: name.startsWith('stairs_') ? { position: [[0, [-STAIR_REST_X, 0, 0]]] } : { scale: [[0, [0, 0, 0]]] } };
  // item props only show in their clips; the claw is empty unless a clip fills it
  for (const p of ITEM_PROPS) steps = { ...steps, [p]: CLIP_PROPS[name] === p ? { position: [[0, [0, PROP_DROP, 0]]] } : { scale: [[0, [0, 0, 0]]] } };
  if (!steps.held_item && !(tracks.held_item)) steps = { ...steps, held_item: { scale: [[0, [0, 0, 0]]] } };
  // the "open for trade" lamp is off unless a clip lights it
  if (!steps.lamp_glow) steps = { ...steps, lamp_glow: { scale: [[0, [0, 0, 0]]] } };
  for (const bone of Object.keys(steps)) for (const ch of Object.keys(steps[bone])) for (const [t, v] of steps[bone][ch])
    add(bone, ch, { channel: ch, data_points: [{ x: String(v[0]), y: String(v[1]), z: String(v[2]) }], uuid: uuid(), time: r2(t), color: -1, interpolation: 'step' });
  const cues = CUES && CUES.clips[name];
  if (cues) {
    const missing = cues.filter(([, id]) => !fs.existsSync(path.join(SOUND_DIR, 'real', id + '.ogg')));
    // sounds only go into a clip once all its real sounds exist (approved); until then the clip stays silent
    if (missing.length) console.error('sounds not added to ' + name + ' yet (no real sound for ' + [...new Set(missing.map((c) => c[1]))].join(', ') + ')');
    else animators.effects = {
      name: 'Effects', type: 'effect',
      keyframes: cues.map(([t, id]) => ({ channel: 'sound', data_points: [{ effect: CUES.namespace + '.' + id, locator: '', file: path.join(SOUND_DIR, 'real', id + '.ogg').split(path.sep).join('/') }], uuid: uuid(), time: r2(t), color: -1, interpolation: 'linear' })),
    };
  }
  animations.push({ uuid: uuid(), name, loop, override: false, length, snapping: FPS, selected: false, group_name: '', anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });
}
const V = (x = 0, y = 0, z = 0) => [x, y, z];
const addV = (...vs) => vs.reduce((a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]], [0, 0, 0]);

// idle background (periodic, 3 s), reused by other clips
const W3 = (2 * PI) / 3;
const idle = {
  body: { rotation: (t) => V(1.5 * sin(W3 * t + PI / 2), 0, 2 * sin(W3 * t)), position: (t) => V(0, 0.25 * sin(2 * W3 * t), 0) },
  wheel: { rotation: (t) => V(-5 * sin(W3 * t + PI / 2), 0, 0) },
  head: { rotation: (t) => V(-1.0 * sin(W3 * t + PI / 2 - 0.6), 0, -1.2 * sin(W3 * t - 0.6)) },
  eye_left: { rotation: (t) => V(0, 0, 1.2 * sin(W3 * t - 1.1)) },
  eye_right: { rotation: (t) => V(0, 0, 1.2 * sin(W3 * t - 1.1)) },
  aerial: { rotation: (t) => V(-5 * sin(W3 * t + PI / 2 - 1.4), 0, -7 * sin(W3 * t - 1.4)) },
  arm_left: { rotation: (t) => V(2 * sin(W3 * t - 0.9), 0, -2.5 * sin(W3 * t - 0.9)) },
  arm_right: { rotation: (t) => V(2 * sin(W3 * t - 0.9), 0, -2.5 * sin(W3 * t - 0.9)) },
  forearm_left: { rotation: (t) => V(-3 * sin(W3 * t - 1.3), 0, 0) },
  forearm_right: { rotation: (t) => V(-3 * sin(W3 * t - 1.3), 0, 0) },
  key: { rotation: (t) => V(0, 0, -60 * t) },
};
const blink = (t, t0) => -2 * (seg(t, t0, t0 + 0.07) - seg(t, t0 + 0.09, t0 + 0.2));

// 1. idle loop
{
  const lid = (t) => V(0, blink(t, 2.2), 0);
  const tr = JSON.parse(JSON.stringify({}));
  const T = {};
  for (const b of Object.keys(idle)) T[b] = { ...idle[b] };
  T.key = { rotation: (t) => V(0, 0, -60 * t) }; // 180 deg over 3 s: the bow is symmetric so it loops
  T.lid_left = { position: lid };
  T.lid_right = { position: lid };
  anim('idle', 3, 'loop', T);
}

// 2. roll, bank into a turn, brake, wobble settle (the inertia chain)
{
  const L = 5;
  const speed = (t) => 10 * (seg(t, 0.2, 1.1) - seg(t, 3.0, 3.45)); // px/s
  const yawRate = (t) => 75 * (seg(t, 1.5, 1.9) - seg(t, 2.5, 2.9)); // deg/s, turning left
  const accRaw = deriv(speed);
  const accel = (t) => 15 * Math.tanh(accRaw(t) / 15); // saturate so hard braking doesn't throw him flat
  const integ = (f, dt = 1 / 600) => { const n = Math.ceil(L / dt) + 2; const a = new Float64Array(n); for (let k = 1; k < n; k++) a[k] = a[k - 1] + f(k * dt) * dt; return (t) => a[Math.min(n - 1, Math.max(0, Math.round(t / dt)))]; };
  const yaw = integ(yawRate);
  const xPos = integ((t) => -Math.sin((yaw(t) * PI) / 180) * speed(t));
  const zPos = integ((t) => -Math.cos((yaw(t) * PI) / 180) * speed(t));
  const dist = integ(speed);
  // body pitch/roll: spring toward lean targets
  const bodyP = sim(L, (t, y, v) => 60 * (-0.7 * accel(t) - y) - 7 * v);
  const bodyR = sim(L, (t, y, v) => 60 * (0.022 * speed(t) * yawRate(t) - y) - 7 * v);
  const bpA = deriv(deriv(bodyP)), brA = deriv(deriv(bodyR));
  const headP = sim(L, (t, y, v) => -60 * y - 6 * v - 1.4 * bpA(t) - 8 * accel(t));
  const headR = sim(L, (t, y, v) => -60 * y - 6 * v - 1.4 * brA(t));
  const hpA = deriv(deriv(headP)), hrA = deriv(deriv(headR));
  const eyeR = sim(L, (t, y, v) => -160 * y - 10 * v - 0.6 * (brA(t) + hrA(t)));
  const aerP = sim(L, (t, y, v) => -45 * y - 2.5 * v - 1.6 * (bpA(t) + hpA(t)) + 14 * accel(t));
  const aerR = sim(L, (t, y, v) => -45 * y - 2.5 * v - 1.6 * (brA(t) + hrA(t)));
  const armP = sim(L, (t, y, v) => -40 * y - 3.5 * v - 1.2 * bpA(t) - 14 * accel(t));
  const armR = sim(L, (t, y, v) => -40 * y - 3.5 * v - 1.2 * brA(t));
  const foreP = sim(L, (t, y, v) => -80 * y - 5 * v - 1.0 * deriv(deriv(armP))(t));
  const T = {
    trade_bot: { position: (t) => V(xPos(t), 0, zPos(t)), rotation: (t) => V(0, yaw(t), 0) },
    wheel: { rotation: (t) => V((-dist(t) / 4) * (180 / PI), 0, 0) },
    body: { rotation: (t) => addV(idle.body.rotation(t), V(bodyP(t), 0, bodyR(t))), position: idle.body.position },
    head: { rotation: (t) => addV(idle.head.rotation(t), V(headP(t) - 0.3 * bodyP(t), 0, headR(t) - 0.45 * bodyR(t))) },
    eye_left: { rotation: (t) => V(0, 0, eyeR(t)) },
    eye_right: { rotation: (t) => V(0, 0, eyeR(t)) },
    aerial: { rotation: (t) => V(aerP(t), 0, aerR(t)) },
    arm_left: { rotation: (t) => V(armP(t), 0, armR(t) - 0.15 * speed(t) * Math.abs(yawRate(t)) / 75) },
    arm_right: { rotation: (t) => V(armP(t), 0, armR(t) + 0.15 * speed(t) * Math.abs(yawRate(t)) / 75) },
    forearm_left: { rotation: (t) => V(foreP(t) + 6 * speed(t) / 10, 0, 0) },
    forearm_right: { rotation: (t) => V(foreP(t) + 6 * speed(t) / 10, 0, 0) },
    key: { rotation: (t) => V(0, 0, -60 * t - 20 * dist(t) / 10) },
    lid_left: { position: (t) => V(0, blink(t, 3.6), 0) },
    lid_right: { position: (t) => V(0, blink(t, 3.6), 0) },
  };
  anim('roll_turn_stop', L, 'once', T);
}

// 3. wall bump
{
  const L = 2.8, tHit = 0.5;
  const zPos = (t) => (t < tHit ? -10 * t : -10 * tHit + 1.5 * seg(t, tHit, tHit + 0.25)) + 4 * seg(t, 2.45, 2.8);
  const dist = (t) => (t < tHit ? 10 * t : 10 * tHit - 1.5 * seg(t, tHit, tHit + 0.25)) - 4 * seg(t, 2.45, 2.8);
  // impulse: body thrown back then springs forward past upright
  const kick = (t) => (t < tHit ? 0 : Math.exp(-5 * (t - tHit)) * Math.sin(2 * PI * 1.6 * (t - tHit)));
  const bodyP = (t) => (t < tHit ? -6 * seg(t, 0, 0.3) : -6 * (1 - seg(t, tHit, tHit + 0.05)) + 38 * kick(t));
  const bpA = deriv(deriv(bodyP));
  const headP = sim(L, (t, y, v) => -110 * y - 8 * v - 0.5 * bpA(t));
  const aer = sim(L, (t, y, v) => -70 * y - 3 * v - 1.0 * bpA(t));
  const dizzy = (t) => seg(t, 1.0, 1.15) * (1 - seg(t, 2.0, 2.15));
  const shake = (t) => (t > 2.05 && t < 2.5 ? 15 * Math.sin(2 * PI * 6.5 * (t - 2.05)) * (1 - seg(t, 2.35, 2.5)) : 0);
  const cross = (t) => seg(t, 2.35, 2.45) * (1 - seg(t, 2.7, 2.8));
  const armsUp = (t) => seg(t, tHit, tHit + 0.1) - seg(t, 0.85, 1.25);
  const T = {
    trade_bot: { position: (t) => V(0, 1.2 * Math.max(0, Math.sin(PI * lin(t, tHit, tHit + 0.3))), zPos(t)) },
    wheel: { rotation: (t) => V((-dist(t) / 4) * (180 / PI), 0, 0) },
    body: { rotation: (t) => addV(idle.body.rotation(t), V(bodyP(t), 0, 0)) },
    neck: { position: (t) => V(0, -1.2 * (seg(t, tHit, tHit + 0.06) - seg(t, tHit + 0.1, tHit + 0.45)), 0) },
    head: {
      rotation: (t) => V(headP(t) + 8 * cross(t), shake(t), 7 * dizzy(t) * Math.sin(2 * PI * 1.5 * t)),
      scale: (t) => { const s = 0.15 * (seg(t, tHit, tHit + 0.06) - seg(t, tHit + 0.1, tHit + 0.35)); return V(1 + s * 0.6, 1 - s, 1 + s * 0.6); },
    },
    eye_left: { rotation: (t) => V(0, 0, 10 * dizzy(t) * Math.sin(2 * PI * 2 * t) - 8 * cross(t)) },
    eye_right: { rotation: (t) => V(0, 0, -10 * dizzy(t) * Math.sin(2 * PI * 2 * t + 1.2) + 8 * cross(t)) },
    iris_left: { rotation: (t) => V(0, 0, 720 * lin(t, 1.0, 2.0)) },
    iris_right: { rotation: (t) => V(0, 0, -720 * lin(t, 1.0, 2.0)) },
    lid_left: { position: (t) => V(0, 1 * seg(t, tHit, tHit + 0.05) * (1 - seg(t, 0.9, 1.0)) - 1.2 * cross(t), 0), rotation: (t) => V(0, 0, -14 * cross(t)) },
    lid_right: { position: (t) => V(0, 1 * seg(t, tHit, tHit + 0.05) * (1 - seg(t, 0.9, 1.0)) - 1.2 * cross(t), 0), rotation: (t) => V(0, 0, 14 * cross(t)) },
    aerial: { rotation: (t) => V(aer(t) + 25 * dizzy(t) * Math.sin(2 * PI * 1.5 * t), 0, 20 * dizzy(t) * Math.cos(2 * PI * 1.5 * t)) },
    arm_left: { rotation: (t) => V(140 * armsUp(t) + 6 * Math.sin(2 * PI * 5 * t) * armsUp(t), 0, -25 * armsUp(t)) },
    arm_right: { rotation: (t) => V(140 * armsUp(t) + 6 * Math.sin(2 * PI * 5 * t + 1) * armsUp(t), 0, 25 * armsUp(t)) },
    forearm_left: { rotation: (t) => V(30 * armsUp(t), 0, 0) },
    forearm_right: { rotation: (t) => V(30 * armsUp(t), 0, 0) },
    claw_left: { rotation: (t) => V(0, 0, 0) },
    key: { rotation: (t) => V(0, 0, -60 * t) },
  };
  anim('wall_bump', L, 'once', T);
}

// 4. count the stock (mannerism)
{
  const L = 3;
  const out = (t) => ease(t, [[0, 0], [0.15, -3], [2.35, 0]], 5, 7);
  const slam = (t) => (t < 2.4 ? 0 : Math.exp(-6 * (t - 2.4)) * Math.sin(2 * PI * 3 * (t - 2.4)));
  const look = (t) => seg(t, 0.35, 0.7) * (1 - seg(t, 2.05, 2.4));
  const nod = (t) => (t > 1.0 && t < 1.8 ? 9 * Math.sin(2 * PI * 2.5 * (t - 1.0)) : 0);
  const T = {
    ...idle,
    drawer: { position: (t) => V(0, 0, out(t)) },
    body: { rotation: (t) => addV(idle.body.rotation(t), V(-4 * look(t) - 3 * slam(t), 0, 0)), position: idle.body.position },
    neck: { position: (t) => V(0, -0.5 * look(t), 0) },
    head: { rotation: (t) => addV(idle.head.rotation(t), V(-22 * look(t) - nod(t), 6 * look(t), 4 * slam(t))) },
    lid_left: { position: (t) => V(0, -0.6 * look(t) + blink(t, 1.9), 0) },
    lid_right: { position: (t) => V(0, -0.6 * look(t) + blink(t, 1.9), 0) },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(-18 * slam(t) + 8 * Math.sin(2 * PI * 2.5 * t) * (t > 1 && t < 1.9 ? 1 : 0), 0, 0)) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(35 * look(t), 0, 0)) },
    forearm_right: { rotation: (t) => addV(idle.forearm_right.rotation(t), V(45 * look(t), 0, 0)) },
  };
  anim('count_stock', L, 'once', T);
}

// 5. docking: reverse up to the station, glance back, reverse-hop up into the cradle, lights out, station glows, winder engages
{
  const L = 6;
  const tA = 1.78, tL = 2.2; // launch and landing of the backwards hop
  const back = (t) => -18 + 8 * seg(t, 0, 1.4); // reverse to z -10, in front of the plinth
  const hop = (t) => lin(t, tA, tL);
  const zPos = (t) => (t < tA ? back(t) : -10 + 10 * smooth(hop(t)));
  const yPos = (t) => {
    if (t < tA) return -2;
    if (t < tL) { const u = hop(t); return -2 + 2 * u + 6 * Math.sin(PI * u); }
    const u = t - tL; // heavy landing: a 1 px rebound, then a small one
    return u < 0.16 ? 1.0 * Math.sin((PI * u) / 0.16) : u < 0.27 ? 0.3 * Math.sin((PI * (u - 0.16)) / 0.11) : 0;
  };
  const dist = (t) => (t < tA ? 8 * seg(t, 0, 1.4) : 8 + 10 * smooth(hop(t)));
  const lookBack = (t) => seg(t, 0.05, 0.35) * (1 - seg(t, 1.25, 1.5)) + env(t, 1.45, 1.6, 2.05, 2.35); // over the shoulder, then again at the cradle before the hop
  const A = (t) => env(t, 1.5, tA, tA, tA + 0.06); // crouch
  const F = (t) => hopArc(t, tA, tL); // airborne
  const K = (t) => landKickAt(t, tL, 2.2, 5.5);
  const Kd = (t) => landKickAt(t, tL + 0.07, 2.0, 5);
  const relax = (t) => seg(t, 2.4, 2.9);
  const fold = (t) => seg(t, 2.5, 3.1);
  const lidsDown = (t) => seg(t, 2.7, 3.5);
  const brakeKick = (t) => landKickAt(t, 1.4, 1.8, 5);
  const idleFade = (t) => 1 - seg(t, 2.4, 3.1);
  const T = {
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)) },
    wheel: { rotation: (t) => V((dist(t) / 4) * (180 / PI) + 260 * lin(t, tA, tL), 0, 0) },
    body: {
      position: (t) => V(0, -1 * A(t) - 1.3 * Math.max(0, K(t)), 0),
      rotation: (t) => V(idle.body.rotation(t)[0] * idleFade(t) - 7 * brakeKick(t) - 6 * A(t) + 8 * F(t) + 7 * K(t) + 4 * relax(t), 0, idle.body.rotation(t)[2] * idleFade(t)),
    },
    neck: { position: (t) => V(0, -0.8 * A(t) + 0.6 * F(t) - 0.8 * Math.max(0, Kd(t)) - 1.5 * lidsDown(t), 0) },
    head: {
      rotation: (t) => V(5 * brakeKick(t) - 5 * A(t) - 9 * F(t) + 11 * Kd(t) - 6 * lidsDown(t), 55 * lookBack(t), 0),
      scale: (t) => { const s = 0.1 * Math.max(0, K(t)); return V(1 + 0.5 * s, 1 - s, 1 + 0.5 * s); },
    },
    eye_left: { rotation: (t) => V(0, 0, 5 * lookBack(t) + 4 * Kd(t) - 4 * lidsDown(t)) },
    eye_right: { rotation: (t) => V(0, 0, -2 * lookBack(t) - 4 * Kd(t) + 4 * lidsDown(t)) },
    lid_left: { position: (t) => V(0, 0.8 * F(t) - 1.4 * lidsDown(t), 0) },
    lid_right: { position: (t) => V(0, 0.8 * F(t) - 1.4 * lidsDown(t), 0) },
    iris_left: { scale: (t) => { const s = 1 + 0.12 * F(t); return V(s, s, 1); } },
    iris_right: { scale: (t) => { const s = 1 + 0.12 * F(t); return V(s, s, 1); } },
    aerial: { rotation: (t) => V(18 * brakeKick(t) - 22 * F(t) + 34 * Kd(t) + 20 * lidsDown(t), 0, 0) },
    arm_left: { rotation: (t) => V(25 * A(t) - 30 * F(t) - 10 * K(t) + 28 * fold(t), 0, -6 * F(t) + 8 * fold(t)) },
    arm_right: { rotation: (t) => V(25 * A(t) - 30 * F(t) - 10 * K(t) + 28 * fold(t), 0, 6 * F(t) - 8 * fold(t)) },
    forearm_left: { rotation: (t) => V(-15 * F(t) + 90 * fold(t), -50 * fold(t), 0) },
    forearm_right: { rotation: (t) => V(-15 * F(t) + 90 * fold(t), 50 * fold(t), 0) },
    key: { rotation: (t) => V(0, 0, t < 3.9 ? -60 * t : -234 - 140 * (t - 3.9)) },
    winder_arm: { rotation: (t) => V(0, -90 * ease(t, [[0, 0], [3.3, 1]], 3.2, 6), 0) },
    winder_head: { rotation: (t) => V(0, 0, t < 3.9 ? 0 : -140 * (t - 3.9)) },
    post_cog: { rotation: (t) => V(t < 3.9 ? 0 : 140 * (t - 3.9), 0, 0) },
  };
  const S = {
    lamp_glow: { scale: [[0, V(1, 1, 1)], [2.55, V(0, 0, 0)]] },
    iris_glow_left: { scale: [[0, V(1, 1, 1)], [2.75, V(0, 0, 0)], [2.85, V(1, 1, 1)], [2.95, V(0, 0, 0)]] },
    iris_glow_right: { scale: [[0, V(1, 1, 1)], [2.95, V(0, 0, 0)], [3.05, V(1, 1, 1)], [3.2, V(0, 0, 0)]] },
    station_glow: { scale: [[0, V(0, 0, 0)], [2.9, V(1, 1, 1)], [3.0, V(0, 0, 0)], [3.1, V(1, 1, 1)]] },
  };
  anim('dock_lights_out', L, 'hold', T, S);
}

// 6. an action comes in: flag springs up, light jumps back into him, hop out and roll off
{
  const L = 3.2;
  const startled = (t) => ease(t, [[0, 0], [0.45, 1], [1.2, 0.35]], 4, 6);
  const hop = (t) => lin(t, 1.15, 1.55);
  const yPos = (t) => {
    if (t < 0.95) return 0;
    if (t < 1.15) return -0.8 * seg(t, 0.95, 1.15);
    if (t < 1.55) { const u = hop(t); return -0.8 + (-2 + 0.8) * u + 5 * Math.sin(PI * u); }
    return -2 - 0.8 * Math.exp(-7 * (t - 1.55)) * Math.sin(2 * PI * 2.2 * (t - 1.55));
  };
  const zPos = (t) => (t < 1.15 ? 0 : t < 1.55 ? -11 * smooth(hop(t)) : -11 - 12 * (t - 1.55) * seg(t, 1.55, 1.9));
  const dist = (t) => (t < 1.55 ? 0 : 11 + 12 * (t - 1.55) * seg(t, 1.55, 1.9));
  const go = (t) => seg(t, 1.5, 1.8);
  const landKick = (t) => (t < 1.55 ? 0 : Math.exp(-5 * (t - 1.55)) * Math.sin(2 * PI * 1.8 * (t - 1.55)));
  const unfold = (t) => 1 - seg(t, 0.45, 0.9);
  const T = {
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)) },
    wheel: { rotation: (t) => V((-dist(t) / 4) * (180 / PI), 0, 0) },
    body: { rotation: (t) => V(4 * (1 - seg(t, 0.45, 0.7)) - 8 * go(t) - 9 * landKick(t) + (t > 1.1 && t < 1.55 ? -6 * Math.sin(PI * hop(t)) : 0), 0, 0) },
    neck: { position: (t) => V(0, -1.5 * (1 - seg(t, 0.4, 0.5)) + 2 * startled(t), 0) },
    head: { rotation: (t) => V(-6 * (1 - seg(t, 0.4, 0.55)) + 10 * landKick(t) - 5 * startled(t), 0, 0) },
    lid_left: { position: (t) => V(0, -1.4 * (1 - seg(t, 0.42, 0.5)) + 1 * startled(t), 0) },
    lid_right: { position: (t) => V(0, -1.4 * (1 - seg(t, 0.42, 0.5)) + 1 * startled(t), 0) },
    iris_left: { scale: (t) => { const s = 1 - 0.35 * seg(t, 0.45, 0.55) + 0.5 * seg(t, 0.75, 0.9) - 0.15 * seg(t, 1.0, 1.2); return V(s, s, 1); } },
    iris_right: { scale: (t) => { const s = 1 - 0.35 * seg(t, 0.45, 0.55) + 0.5 * seg(t, 0.75, 0.9) - 0.15 * seg(t, 1.0, 1.2); return V(s, s, 1); } },
    aerial: { rotation: (t) => V(20 * (1 - seg(t, 0.45, 0.55)) - 30 * Math.exp(-5 * Math.max(0, t - 0.45)) * Math.sin(2 * PI * 3 * Math.max(0, t - 0.45)) + 25 * landKick(t), 0, 0) },
    arm_left: { rotation: (t) => V(28 * unfold(t) - 20 * go(t) - 18 * landKick(t), 0, 8 * unfold(t)) },
    arm_right: { rotation: (t) => V(28 * unfold(t) - 20 * go(t) - 18 * landKick(t), 0, -8 * unfold(t)) },
    forearm_left: { rotation: (t) => V(90 * unfold(t), -50 * unfold(t), 0) },
    forearm_right: { rotation: (t) => V(90 * unfold(t), 50 * unfold(t), 0) },
    signal_flag: { rotation: (t) => V(0, 0, 90 * ease(t, [[0, 0], [0.05, 1]], 4, 4.5)) },
    winder_arm: { rotation: (t) => V(0, -90 + 90 * ease(t, [[0, 0], [0.6, 1]], 3.2, 6), 0) },
    key: { rotation: (t) => V(0, 0, -60 * t) },
  };
  const S = {
    station_glow: { scale: [[0, V(1, 1, 1)], [0.4, V(0, 0, 0)]] },
    iris_glow_left: { scale: [[0, V(0, 0, 0)], [0.42, V(1, 1, 1)]] },
    iris_glow_right: { scale: [[0, V(0, 0, 0)], [0.46, V(1, 1, 1)]] },
    lamp_glow: { scale: [[0, V(0, 0, 0)], [0.55, V(1, 1, 1)]] },
  };
  anim('action_comes_in', L, 'hold', T, S);
}

// 6b. sees a player (to his left): notice, double-take, perk up, wave, roll closer, open for trade
{
  const L = 3.6;
  const notice = (t) => ease(t, [[0, 0], [0.05, 32], [0.5, -6], [0.68, 34], [1.0, 34], [1.4, 0]], 5, 7); // head yaw: look, away, snap back, body catches up
  const turn = (t) => 34 * seg(t, 1.0, 1.5); // whole bot turns to face the player
  const startled = (t) => seg(t, 0.66, 0.74) * (1 - seg(t, 1.1, 1.4));
  const happy = (t) => seg(t, 1.1, 1.35);
  const wave = (t) => seg(t, 1.3, 1.5) * (1 - seg(t, 2.35, 2.6));
  const rollD = (t) => 5 * seg(t, 2.4, 3.2);
  const rollA = deriv(deriv(rollD));
  const bodyP = sim(L, (t, y, v) => 70 * (-0.5 * rollA(t) - y) - 8 * v);
  const aer = sim(L, (t, y, v) => -45 * y - 2.5 * v - 1.6 * deriv(deriv(bodyP))(t) - 120 * deriv(deriv(startled))(t) / 60);
  const yawRad = (34 * PI) / 180;
  const T = {
    ...idle,
    trade_bot: { position: (t) => V(-Math.sin(yawRad) * rollD(t), 0, -Math.cos(yawRad) * rollD(t)), rotation: (t) => V(0, turn(t), 0) },
    wheel: { rotation: (t) => V((-rollD(t) / 4) * (180 / PI) + 25 * (seg(t, 1.0, 1.5) - 0.5 * seg(t, 1.5, 1.7)), 0, 0) },
    body: { rotation: (t) => addV(idle.body.rotation(t), V(bodyP(t) + 4 * startled(t), 0, -6 * (seg(t, 1.0, 1.25) - seg(t, 1.3, 1.7)))), position: idle.body.position },
    neck: { position: (t) => V(0, 2 * startled(t) + 0.5 * happy(t), 0) },
    head: { rotation: (t) => addV(idle.head.rotation(t), V(-4 * happy(t), notice(t), -8 * happy(t) * (1 - seg(t, 2.6, 3.2)))) },
    eye_left: { rotation: (t) => V(0, 0, 10 * happy(t)) },
    eye_right: { rotation: (t) => V(0, 0, -10 * happy(t)) },
    lid_left: { position: (t) => V(0, 1 * startled(t) + 0.5 * happy(t) + blink(t, 1.05), 0) },
    lid_right: { position: (t) => V(0, 1 * startled(t) + 0.5 * happy(t) + blink(t, 1.05), 0) },
    iris_left: { scale: (t) => { const s = 1 - 0.3 * startled(t) + 0.25 * happy(t); return V(s, s, 1); } },
    iris_right: { scale: (t) => { const s = 1 - 0.3 * startled(t) + 0.25 * happy(t); return V(s, s, 1); } },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(aer(t) - 10 * happy(t), 0, 0)) },
    arm_left: { rotation: (t) => addV(idle.arm_left.rotation(t), V(-8 * startled(t), 0, -10 * startled(t))) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(-10 * wave(t), 0, 115 * wave(t) + 10 * startled(t))) },
    forearm_right: { rotation: (t) => V(0, 0, wave(t) * (45 + 22 * Math.sin(2 * PI * 3 * (t - 1.5)))) },
    claw_right: { rotation: (t) => V(0, 0, -15 * wave(t) * Math.sin(2 * PI * 3 * (t - 1.5) - 0.8)) },
    drawer: { position: (t) => V(0, 0, ease(t, [[0, 0], [3.0, -3]], 5, 7)) },
  };
  anim('sees_player', L, 'once', T, { lamp_glow: { scale: [[0, V(0, 0, 0)], [3.0, V(1, 1, 1)]] } });
}

// 6c. stairs: bounce up two 8 px steps, then bounce back down them
const bumpBefore = (t, ta, pre = 0.15) => seg(t, ta - pre, ta) * (1 - seg(t, ta, ta + 0.08));
const sumOver = (list, fn) => (t) => list.reduce((acc, x) => acc + fn(t, x), 0);
{
  const L = 2.4;
  // each hop: launch, land, from/to z and y; up first, forward once the wheel is above the step
  const hops = [
    { ta: 0.6, tl: 0.94, z0: -4.5, z1: -12.5, y0: 0, y1: 8 },
    { ta: 1.26, tl: 1.6, z0: -12.5, z1: -22, y0: 8, y1: 16 },
  ];
  const zPos = (t) => {
    if (t < hops[0].ta) return hops[0].z0 * seg(t, 0, 0.45);
    for (let k = 0; k < hops.length; k++) {
      const h = hops[k];
      if (t < h.ta) return h.z0;
      if (t <= h.tl) return h.z0 + (h.z1 - h.z0) * smooth(lin(lin(t, h.ta, h.tl), 0.3, 1));
    }
    return hops[hops.length - 1].z1;
  };
  const yPos = (t) => {
    for (const h of hops) if (t >= h.ta && t <= h.tl) { const u = lin(t, h.ta, h.tl); return u < 0.6 ? h.y0 + (h.y1 - h.y0 + 5) * Math.sin((PI / 2) * (u / 0.6)) : h.y1 + 5 * Math.cos((PI / 2) * ((u - 0.6) / 0.4)); }
    let y = 0; for (const h of hops) if (t > h.tl) y = h.y1; return y;
  };
  const A = sumOver(hops, (t, h) => bumpBefore(t, h.ta)); // anticipation crouch
  const F = sumOver(hops, (t, h) => (t > h.ta && t < h.tl ? Math.sin(PI * lin(t, h.ta, h.tl)) : 0)); // in the air
  const K = sumOver(hops, (t, h) => landKickAt(t, h.tl)); // landing spring
  const Kd = sumOver(hops, (t, h) => landKickAt(t, h.tl + 0.06, 2.0, 5)); // the head and aerial a beat later
  const spin = sumOver(hops, (t, h) => lin(t, h.ta, h.tl));
  const T = {
    ...idle,
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)) },
    wheel: { rotation: (t) => V((zPos(t) / 4) * (180 / PI) - 300 * spin(t), 0, 0) },
    body: { position: (t) => V(0, -1 * A(t) - 1.3 * Math.max(0, K(t)), 0), rotation: (t) => addV(idle.body.rotation(t), V(-5 * A(t) - 7 * F(t) - 6 * K(t), 0, 0)) },
    neck: { position: (t) => V(0, -0.8 * A(t) + 0.7 * F(t) - 0.8 * Math.max(0, Kd(t)), 0) },
    head: {
      rotation: (t) => addV(idle.head.rotation(t), V(-6 * A(t) + 11 * F(t) - 13 * Kd(t), 0, 0)),
      scale: (t) => { const s = 0.1 * Math.max(0, K(t)); return V(1 + 0.5 * s, 1 - s, 1 + 0.5 * s); },
    },
    eye_left: { rotation: (t) => V(0, 0, 5 * Kd(t)) },
    eye_right: { rotation: (t) => V(0, 0, -5 * sumOver(hops, (tt, h) => landKickAt(tt, h.tl + 0.1, 2.0, 5))(t)) },
    lid_left: { position: (t) => V(0, 0.7 * F(t) - 0.4 * A(t), 0) },
    lid_right: { position: (t) => V(0, 0.7 * F(t) - 0.4 * A(t), 0) },
    iris_left: { scale: (t) => { const s = 1 + 0.12 * F(t); return V(s, s, 1); } },
    iris_right: { scale: (t) => { const s = 1 + 0.12 * F(t); return V(s, s, 1); } },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(22 * F(t) - 38 * Kd(t), 0, 0)) },
    arm_left: { rotation: (t) => addV(idle.arm_left.rotation(t), V(-28 * A(t) + 32 * F(t) + 12 * K(t), 0, -6 * F(t))) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(-28 * A(t) + 32 * F(t) + 12 * K(t), 0, 6 * F(t))) },
    forearm_left: { rotation: (t) => V(18 * F(t) + 10 * Kd(t), 0, 0) },
    forearm_right: { rotation: (t) => V(18 * F(t) + 10 * Kd(t), 0, 0) },
  };
  CLEAR_CHECKS.push(['stairs_up', T, L, [[[-8, 0, -26], [8, 8, -10]], [[-8, 8, -26], [8, 16, -18]]]]);
  anim('stairs_up', L, 'once', T);
}
{
  const L = 2.4;
  // facing back down the stairs (+Z), rolling without stopping; drops when the wheel centre passes each edge
  const speed = (t) => 13 * seg(t, 0, 0.3) * (1 - seg(t, 1.9, 2.25));
  const dt = 1 / 600, n = Math.ceil(L / dt) + 2, zs = new Float64Array(n);
  zs[0] = -22; for (let k = 1; k < n; k++) zs[k] = zs[k - 1] + speed(k * dt) * dt;
  const zPos = (t) => zs[Math.min(n - 1, Math.max(0, Math.round(t / dt)))];
  const cross = (zc) => { for (let k = 0; k < n; k++) if (zs[k] >= zc) return k * dt; return L; };
  const DROP = 0.2;
  const drops = [{ td: cross(-17), h: 16 }, { td: cross(-9), h: 8 }].map((d) => ({ ...d, tl: d.td + DROP }));
  const bounce = (u) => (u < 0 ? 0 : u < 0.18 ? 1.5 * Math.sin((PI * u) / 0.18) : u < 0.3 ? 0.5 * Math.sin((PI * (u - 0.18)) / 0.12) : 0);
  const yPos = (t) => {
    let y = 16;
    for (const d of drops) {
      if (t < d.td) return y;
      if (t < d.tl) { const u = (t - d.td) / DROP; return d.h - 8 * u * u; }
      y = d.h - 8;
      if (t < d.tl + 0.3 && (drops.indexOf(d) === drops.length - 1 || t < drops[drops.indexOf(d) + 1].td)) return y + bounce(t - d.tl);
    }
    return y;
  };
  const D = sumOver(drops, (t, d) => (t > d.td - 0.04 && t < d.tl + 0.04 ? Math.sin(PI * lin(t, d.td - 0.04, d.tl + 0.04)) : 0));
  const K = sumOver(drops, (t, d) => landKickAt(t, d.tl, 2.4, 6));
  const Kd = sumOver(drops, (t, d) => landKickAt(t, d.tl + 0.07, 2.0, 5));
  const T = {
    ...idle,
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)), rotation: () => V(0, 180, 0) },
    wheel: { rotation: (t) => V((-(zPos(t) + 22) / 4) * (180 / PI), 0, 0) },
    body: { position: (t) => V(0, -1.1 * Math.max(0, K(t)), 0), rotation: (t) => addV(idle.body.rotation(t), V(-3 * speed(t) / 13 - 10 * D(t) + 9 * K(t), 0, 0)) },
    neck: { position: (t) => V(0, 0.5 * D(t) - 0.7 * Math.max(0, Kd(t)), 0) },
    head: { rotation: (t) => addV(idle.head.rotation(t), V(7 * D(t) - 12 * Kd(t), 0, 0)) },
    eye_left: { rotation: (t) => V(0, 0, 4 * Kd(t)) },
    eye_right: { rotation: (t) => V(0, 0, -4 * Kd(t)) },
    lid_left: { position: (t) => V(0, 0.8 * D(t), 0) },
    lid_right: { position: (t) => V(0, 0.8 * D(t), 0) },
    iris_left: { scale: (t) => { const s = 1 + 0.15 * D(t); return V(s, s, 1); } },
    iris_right: { scale: (t) => { const s = 1 + 0.15 * D(t); return V(s, s, 1); } },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(26 * D(t) - 36 * Kd(t), 0, 0)) },
    arm_left: { rotation: (t) => addV(idle.arm_left.rotation(t), V(10 * D(t) - 8 * K(t), 0, -32 * D(t))) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(10 * D(t) - 8 * K(t), 0, 32 * D(t))) },
    forearm_left: { rotation: (t) => V(12 * D(t), 0, -15 * D(t)) },
    forearm_right: { rotation: (t) => V(12 * D(t), 0, 15 * D(t)) },
  };
  CLEAR_CHECKS.push(['stairs_down', T, L, [[[-8, 0, -26], [8, 8, -10]], [[-8, 8, -26], [8, 16, -18]]]]);
  anim('stairs_down', L, 'once', T);
}

// 7. expressions: the review sheet (one held pose per second) plus a smooth clip for each
const POSES = {
  neutral: {},
  happy: { eye_left: V(0, 0, 10), eye_right: V(0, 0, -10), lidY: 0.6, iris: 1.2, aerial: V(-12, 0, 0), head: V(-4, 0, 0), neck: 0.5 },
  curious: { head: V(0, 0, 15), eye_left: V(0, 0, 6), eye_right: V(0, 0, -2), iris: 1.25, lidY: 0.4, aerial: V(0, 0, -15) },
  startled: { neck: 2, lidY: 1, iris: 0.7, aerial: V(25, 0, 0), arms: V(-20, 0, 20), body: V(5, 0, 0) },
  sad: { eye_left: V(0, 0, 12), eye_right: V(0, 0, -12), lidY: -1, head: V(12, 0, 0), aerial: V(45, 0, 0), neck: -1 },
  cross: { lidY: -1.2, lidRot: 14, head: V(10, 0, 0), eye_left: V(-6, 0, -6), eye_right: V(-6, 0, 6) },
  wave: { eye_left: V(0, 0, 8), eye_right: V(0, 0, -8), lidY: 0.5, waveArm: true, head: V(0, 0, -8) },
  dozing: { lidY: -2, neck: -1.5, head: V(14, 0, 0), aerial: V(40, 0, 0), fold: true, off: true },
};
function poseTargets(p) {
  const a = p.arms || V();
  const s = p.iris || 1;
  return {
    head: { rotation: p.head || V() },
    neck: { position: V(0, p.neck || 0, 0) },
    eye_left: { rotation: p.eye_left || V() },
    eye_right: { rotation: p.eye_right || V() },
    lid_left: { position: V(0, p.lidY || 0, 0), rotation: V(0, 0, -(p.lidRot || 0)) },
    lid_right: { position: V(0, p.lidY || 0, 0), rotation: V(0, 0, p.lidRot || 0) },
    iris_left: { scale: V(s, s, 1) },
    iris_right: { scale: V(s, s, 1) },
    aerial: { rotation: p.aerial || V() },
    body: { rotation: p.body || V() },
    arm_left: { rotation: p.fold ? V(28, 0, 8) : V(a[0], 0, -a[2]) },
    arm_right: { rotation: p.fold ? V(28, 0, -8) : p.waveArm ? V(-10, 0, 115) : V(a[0], 0, a[2]) },
    forearm_left: { rotation: p.fold ? V(90, -50, 0) : V() },
    forearm_right: { rotation: p.fold ? V(90, 50, 0) : p.waveArm ? V(0, 0, 45) : V() },
    claw_right: { rotation: p.waveArm ? V(0, 0, -15) : V() },
  };
}
{
  const S = {};
  const put = (bone, ch, t, v) => { S[bone] = S[bone] || {}; S[bone][ch] = S[bone][ch] || []; S[bone][ch].push([t, v]); };
  Object.values(POSES).forEach((p, t) => {
    const tg = poseTargets(p);
    for (const b of Object.keys(tg)) for (const ch of Object.keys(tg[b])) put(b, ch, t, tg[b][ch]);
    put('iris_glow_left', 'scale', t, p.off ? V(0, 0, 0) : V(1, 1, 1));
    put('iris_glow_right', 'scale', t, p.off ? V(0, 0, 0) : V(1, 1, 1));
    put('lamp_glow', 'scale', t, p.waveArm ? V(1, 1, 1) : V(0, 0, 0));
  });
  anim('expression_sheet', Object.keys(POSES).length, 'once', {}, S);
}

// each part springs into the pose a beat after the one carrying it, and springs back out the same way
const DELAY = { body: 0, neck: 0.03, head: 0.05, lid_left: 0.04, lid_right: 0.05, iris_left: 0.06, iris_right: 0.07, eye_left: 0.09, eye_right: 0.1, aerial: 0.14, arm_left: 0.06, arm_right: 0.05, forearm_left: 0.1, forearm_right: 0.09, claw_right: 0.13 };
function exprClip(name, L, pose, o) {
  const tg = poseTargets(pose);
  const extra = o.extra || {};
  const weight = (bone) => {
    const d = DELAY[bone] || 0;
    const loose = bone === 'aerial';
    const f = (o.freq || 5) * (loose ? 0.8 : 1), dm = (o.damp || 6) * (loose ? 0.55 : 1);
    const fo = (o.outFreq || o.freq || 5) * (loose ? 0.8 : 1);
    return (t) => springStep(t, o.tIn + d, f, dm) - springStep(t, o.tOut + d, fo, dm);
  };
  const T = {};
  const bones = new Set([...Object.keys(idle), ...Object.keys(tg), ...Object.keys(extra)]);
  for (const b of bones) {
    const chans = new Set([...Object.keys(idle[b] || {}), ...Object.keys(tg[b] || {}), ...Object.keys(extra[b] || {})]);
    const w = weight(b);
    T[b] = {};
    for (const ch of chans) {
      const base = (idle[b] || {})[ch], target = (tg[b] || {})[ch], ex = (extra[b] || {})[ch];
      const isScale = ch === 'scale';
      T[b][ch] = (t) => {
        let v = base ? base(t) : isScale ? V(1, 1, 1) : V();
        if (target) { const k = w(t); v = isScale ? v.map((x, i) => x + k * (target[i] - 1)) : addV(v, target.map((x) => x * k)); }
        return ex ? addV(v, ex(t)) : v;
      };
    }
  }
  anim(name, L, 'once', T, o.steps || {});
}

// happy: springs into a bright face, two little bounces on the wheel, a happy wiggle
{
  const hops = (t) => hopArc(t, 0.4, 0.62) + hopArc(t, 0.68, 0.88);
  const wig = (t) => 3 * Math.sin(2 * PI * 2 * (t - 0.95)) * env(t, 0.95, 1.1, 2.2, 2.4);
  exprClip('expr_happy', 3.3, POSES.happy, {
    tIn: 0.2, tOut: 2.5,
    extra: {
      trade_bot: { position: (t) => V(0, 1.0 * hops(t), 0) },
      body: { rotation: (t) => V(-3 * hops(t), 0, wig(t)) },
      head: { rotation: (t) => V(0, 0, -2 * wig(t)) },
      aerial: { rotation: (t) => V(-12 * hops(t), 0, 2.5 * wig(t)) },
    },
  });
}
// curious: slow tilt and lean in, a scanning look, pupils pulsing with interest
{
  const k = (t) => env(t, 0.6, 0.9, 2.8, 3.1);
  exprClip('expr_curious', 3.8, POSES.curious, {
    tIn: 0.25, tOut: 3.0, freq: 3.5, damp: 5,
    extra: {
      body: { rotation: (t) => V(-5 * k(t), 0, 0) },
      head: { rotation: (t) => V(0, 8 * Math.sin(2 * PI * 0.5 * (t - 0.9)) * k(t), 0) },
      iris_left: { scale: (t) => { const p = 0.08 * Math.sin(2 * PI * 1.6 * t) * k(t); return V(p, p, 0); } },
      iris_right: { scale: (t) => { const p = 0.08 * Math.sin(2 * PI * 1.6 * t + 0.6) * k(t); return V(p, p, 0); } },
      aerial: { rotation: (t) => V(0, 0, 6 * Math.sin(2 * PI * 1.2 * t) * k(t)) },
    },
  });
}
// startled: a fast snap, a jolt back on the wheel, the aerial trembling, then a slow wary relax
{
  const back = (t) => springStep(t, 0.15, 8, 5) - springStep(t, 1.7, 3.5, 5);
  exprClip('expr_startled', 3.0, POSES.startled, {
    tIn: 0.15, tOut: 2.1, freq: 9, damp: 6, outFreq: 3.5,
    extra: {
      trade_bot: { position: (t) => V(0, 0.9 * hopArc(t, 0.15, 0.4), 1.5 * back(t)) },
      wheel: { rotation: (t) => V(((1.5 * back(t)) / 4) * (180 / PI), 0, 0) },
      aerial: { rotation: (t) => V(6 * Math.sin(2 * PI * 7 * t) * env(t, 0.2, 0.3, 1.0, 1.5), 0, 0) },
    },
  });
}
// sad: a slow droop, then a sigh (rise, long sink), arms hanging limp
{
  const k = (t) => env(t, 0.4, 1.0, 3.4, 3.9);
  const sigh = (t) => (0.5 * seg(t, 1.1, 1.5) - 1.1 * seg(t, 1.5, 2.3)) * (1 - seg(t, 3.4, 3.9));
  const slump = (t) => seg(t, 1.5, 2.4) * (1 - seg(t, 3.4, 3.9));
  exprClip('expr_sad', 4.4, POSES.sad, {
    tIn: 0.3, tOut: 3.5, freq: 2.5, damp: 4.5,
    extra: {
      body: { position: (t) => V(0, sigh(t), 0), rotation: (t) => V(-3 * slump(t), 0, 0) },
      head: { rotation: (t) => V(5 * slump(t), 0, 0) },
      arm_left: { rotation: (t) => V(-3 * k(t), 0, 5 * k(t)) },
      arm_right: { rotation: (t) => V(-3 * k(t), 0, -5 * k(t)) },
    },
  });
}
// cross: lids flatten, a puff and a huffy head shake, elbows out like hands on hips
{
  const k = (t) => env(t, 0.25, 0.6, 2.8, 3.2);
  const huff = (t) => (t > 1.1 && t < 1.5 ? 6 * Math.sin(2 * PI * 5 * (t - 1.1)) * (1 - seg(t, 1.35, 1.5)) : 0);
  const puff = (t) => 0.4 * (seg(t, 1.0, 1.15) - seg(t, 1.2, 1.5));
  exprClip('expr_cross', 3.6, POSES.cross, {
    tIn: 0.2, tOut: 2.8,
    extra: {
      head: { rotation: (t) => V(0, huff(t), 0) },
      body: { rotation: (t) => V(-4 * k(t), 0, 0), position: (t) => V(0, puff(t), 0) },
      arm_left: { rotation: (t) => V(0, 0, -18 * k(t)) },
      arm_right: { rotation: (t) => V(0, 0, 18 * k(t)) },
      forearm_left: { rotation: (t) => V(0, 0, 35 * k(t)) },
      forearm_right: { rotation: (t) => V(0, 0, -35 * k(t)) },
    },
  });
}
// wave: the arm springs up, three waves with a loose claw flick, leaning in, lamp on
{
  const k = (t) => env(t, 0.3, 0.6, 2.3, 2.6);
  const osc = (t) => Math.sin(2 * PI * 2.6 * (t - 0.65)) * env(t, 0.55, 0.75, 2.0, 2.3);
  exprClip('expr_wave', 3.4, POSES.wave, {
    tIn: 0.2, tOut: 2.4,
    extra: {
      forearm_right: { rotation: (t) => V(0, 0, 22 * osc(t)) },
      claw_right: { rotation: (t) => V(0, 0, -12 * Math.sin(2 * PI * 2.6 * (t - 0.65) - 0.9) * env(t, 0.55, 0.75, 2.0, 2.3)) },
      body: { rotation: (t) => V(-3 * k(t), 0, -3 * k(t)) },
    },
    steps: { lamp_glow: { scale: [[0, V(0, 0, 0)], [0.4, V(1, 1, 1)], [2.6, V(0, 0, 0)]] } },
  });
}
// dozing off: lids sink, two nod-and-catch dips, eyes go dark, sleepy breathing, then a jolt awake and a look round
{
  const dip = (t, t0) => Math.sin(PI * lin(t, t0, t0 + 0.45));
  const breathe = (t) => 0.3 * Math.sin((2 * PI * t) / 2) * env(t, 2.4, 2.8, 4.3, 4.5);
  const wide = (t) => env(t, 4.5, 4.6, 5.4, 6.0);
  const look = (t) => -25 * (seg(t, 4.9, 5.1) - seg(t, 5.4, 5.6)) + 25 * (seg(t, 5.6, 5.8) - seg(t, 6.1, 6.4));
  exprClip('expr_doze_off', 6.8, POSES.dozing, {
    tIn: 0.4, tOut: 4.5, freq: 1.6, damp: 3, outFreq: 9,
    extra: {
      head: { rotation: (t) => V(7 * (dip(t, 1.2) + dip(t, 2.0)), look(t), 0) },
      body: { position: (t) => V(0, breathe(t), 0) },
      trade_bot: { position: (t) => V(0, 1.5 * hopArc(t, 4.5, 4.8), 0) },
      neck: { position: (t) => V(0, 1 * wide(t), 0) },
      lid_left: { position: (t) => V(0, 1 * wide(t), 0) },
      lid_right: { position: (t) => V(0, 1 * wide(t), 0) },
    },
    steps: {
      iris_glow_left: { scale: [[0, V(1, 1, 1)], [2.2, V(0, 0, 0)], [2.27, V(1, 1, 1)], [2.36, V(0, 0, 0)], [4.5, V(1, 1, 1)]] },
      iris_glow_right: { scale: [[0, V(1, 1, 1)], [2.3, V(0, 0, 0)], [4.52, V(1, 1, 1)]] },
    },
  });
}

// 8. item handling: chests, barrels, depots, belts

// forward kinematics, so world items appear exactly where the claw lets go (Blockbench order: X, then Y, then Z)
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
function fk(T, chain, t, point) {
  let M = M4.I();
  for (const b of chain) {
    const g = groupByName[b], tr = T[b] || {};
    const pos = tr.position ? tr.position(t) : V(), rot = addV(g.rot, tr.rotation ? tr.rotation(t) : V()), sc = tr.scale ? tr.scale(t) : V(1, 1, 1);
    const o = g.origin;
    M = M4.mul(M, M4.mul(M4.T(addV(o, pos)), M4.mul(M4.Rz(rot[2]), M4.mul(M4.Ry(rot[1]), M4.mul(M4.Rx(rot[0]), M4.mul(M4.S(sc), M4.T(o.map((x) => -x))))))));
  }
  return M4.apply(M, point);
}
const HELD_CHAIN = ['trade_bot', 'body', 'arm_right', 'forearm_right', 'claw_right', 'held_item'];
const heldCentre = (T, t) => fk(T, HELD_CHAIN, t, [6, 1.5, 0]);
const clawTip = (T, t, side) => fk(T, ['trade_bot', 'body', `arm_${side}`, `forearm_${side}`, `claw_${side}`], t, [side === 'right' ? 6 : -6, 3, 0]);

// keyed poses with springy transitions; a key is [time, V, freq?, damp?] and each bone's keys are offset by its chain delay
function keyedTracks(keys, o = {}) {
  const f0 = o.freq || 5, d0 = o.damp || 6;
  const T = {};
  const base0 = o.noIdle ? {} : idle; // noIdle: no idle sway underneath (clips that must start/end on exact poses)
  const bones = new Set([...Object.keys(base0), ...Object.keys(keys), ...Object.keys(o.extra || {})]);
  for (const b of bones) {
    const base = base0[b] || {}, kb = keys[b] || {}, ex = (o.extra || {})[b] || {};
    const d = DELAY[b] || 0, loose = b === 'aerial';
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
          v = ch === 'scale' ? kv : addV(v, kv);
        }
        return ex[ch] ? addV(v, ex[ch](t)) : v;
      };
    }
  }
  return T;
}
const Z = V();
const CARRY_ARM = V(40, 0, -12), CARRY_FORE = V(55, 0, 0);
const osc = (t, a, b, hz, ph = 0) => Math.sin(2 * PI * hz * t + ph) * env(t, a, a + 0.2, b - 0.2, b);
// a world item that appears at the claw on release and settles to its resting spot
const settleFrom = (p, rest, tr, dur = 0.18) => (t) => (t < tr ? V() : addV(p.map((x, i) => x - rest[i]).map((x) => x * (1 - smooth(lin(t, tr, tr + dur))))));
// a world item that slides into the claw just before it's picked up
const slideTo = (p, rest, tg, dur = 0.15) => (t) => p.map((x, i) => (x - rest[i]) * smooth(lin(t, tg - dur, tg)));
const DEBUG_FK = process.env.FK === '1';
const fkLog = (label, p) => DEBUG_FK && console.error(label, p.map((x) => x.toFixed(1)).join(', '));
// small IK: the upper-arm and forearm pitch that put the claw tip (or the held item's centre) on a target, for a given body lean
function solveArm(side, target, lean, o = {}) {
  const pt = o.held ? [6, 1.5, 0] : [side === 'right' ? 6 : -6, 3, 0];
  const chain = o.held ? HELD_CHAIN : ['trade_bot', 'body', 'arm_' + side, 'forearm_' + side, 'claw_' + side];
  let best = null;
  for (let a = -30; a <= 180; a += 1) for (let f = -150; f <= 90; f += 1) {
    const T = { body: { rotation: () => V(lean, 0, 0) }, ['arm_' + side]: { rotation: () => V(a, 0, o.rz || 0) }, ['forearm_' + side]: { rotation: () => V(f, 0, 0) } };
    const p = fk(T, chain, 0, pt);
    const e = Math.hypot(p[1] - target[1], p[2] - target[2]) + 0.006 * Math.abs(f - (o.preferF || 0)) + 0.01 * Math.abs(a - (o.preferA === undefined ? 90 : o.preferA));
    if (!best || e < best[0]) best = [e, a, f, p];
  }
  fkLog('solve ' + side + ' -> [' + target + '] arm ' + best[1] + ' fore ' + best[2] + ' at', best[3]);
  return [best[1], best[2]];
}

// take from a chest: heave the lid, peek, rummage, toss junk, found it, hold it up, shut the lid, carry
{
  const L = 6.6, tg = 3.45;
  const [srA, srF] = solveArm('right', [5, 11.8, -11.5], -14, { held: true, rz: -4, preferA: 95, preferF: -50 });
  const [slA, slF] = solveArm('left', [-5, 12.5, -11], -14, { rz: 4, preferA: 95, preferF: -50 });
  const [hkA, hkF] = solveArm('left', [-5, 12.5, -8.5], -6, { rz: 4, preferA: 55, preferF: 40 });
  const rummage = (t, ph) => osc(t, 1.55, 3.35, 2.2, ph);
  const T = keyedTracks({
    body: { rotation: [[0, Z], [0.3, V(-6, 0, 0)], [1.3, V(-14, 0, 0)], [3.55, V(-3, 0, 0), 7, 6], [4.5, V(-8, 0, 0)], [5.3, Z]] },
    neck: { position: [[0, Z], [1.3, V(0, 1.2, 0)], [3.5, V(0, 2, 0), 8, 6], [4.2, V(0, 0.5, 0)], [5.4, Z]] },
    head: { rotation: [[0, Z], [0.3, V(-12, 0, 0)], [1.3, V(-28, 0, 0)], [3.5, V(-2, 0, -10), 7, 6], [4.5, V(-15, 0, 0)], [5.4, V(-4, 0, 0)]] },
    eye_left: { rotation: [[0, Z], [3.55, V(0, 0, 10)], [4.5, Z]] },
    eye_right: { rotation: [[0, Z], [3.55, V(0, 0, -10)], [4.5, Z]] },
    lid_left: { position: [[0, Z], [1.2, V(0, 0.6, 0)], [1.7, Z], [3.45, V(0, 1, 0), 9, 6], [3.8, V(0, 0.6, 0)], [4.6, Z]] },
    lid_right: { position: [[0, Z], [1.2, V(0, 0.6, 0)], [1.7, Z], [3.45, V(0, 1, 0), 9, 6], [3.8, V(0, 0.6, 0)], [4.6, Z]] },
    iris_left: { scale: [[0, V(1, 1, 1)], [1.3, V(1.2, 1.2, 1)], [1.7, V(1, 1, 1)], [3.45, V(0.75, 0.75, 1), 9, 6], [3.7, V(1.25, 1.25, 1)], [4.6, V(1, 1, 1)]] },
    iris_right: { scale: [[0, V(1, 1, 1)], [1.3, V(1.2, 1.2, 1)], [1.7, V(1, 1, 1)], [3.45, V(0.75, 0.75, 1), 9, 6], [3.7, V(1.25, 1.25, 1)], [4.6, V(1, 1, 1)]] },
    arm_left: { rotation: [[0, Z], [0.35, V(hkA, 0, 4)], [0.85, V(150, 0, 6)], [1.25, V(slA, 0, 4)], [4.3, V(150, 0, 6)], [4.75, V(85, 0, 4)], [5.3, Z]] },
    forearm_left: { rotation: [[0, Z], [0.35, V(hkF, 0, 0)], [0.85, Z], [1.25, V(slF, 0, 0)], [4.3, Z], [5.3, Z]] },
    arm_right: { rotation: [[0, Z], [1.25, V(srA, 0, -4)], [tg, V(srA + 2, 0, -4)], [tg + 0.12, V(155, 0, 8), 7, 6], [4.25, V(150, 0, 8)], [4.7, CARRY_ARM]] },
    forearm_right: { rotation: [[0, Z], [1.25, V(srF, 0, 0)], [tg + 0.12, V(10, 0, 0), 7, 6], [4.7, CARRY_FORE]] },
    chest_lid: { rotation: [[0, Z], [0.55, V(-105, 0, 0), 4, 4], [4.45, Z, 6, 14]] },
  }, {
    extra: {
      arm_left: { rotation: (t) => V(14 * rummage(t, 0), 0, 0) },
      forearm_left: { rotation: (t) => V(18 * rummage(t, 1), 0, 0) },
      arm_right: { rotation: (t) => V(14 * rummage(t, PI), 0, 0) },
      forearm_right: { rotation: (t) => V(18 * rummage(t, PI + 1), 0, 0) },
      body: { rotation: (t) => V(0, 0, 4 * osc(t, 1.55, 3.35, 1.1)) },
      head: { rotation: (t) => V(5 * rummage(t, 0.5), 0, 0) },
      aerial: { rotation: (t) => V(10 * rummage(t, 2), 0, 6 * osc(t, 1.55, 3.35, 1.1, 1)) },
    },
  });
  const p = heldCentre(T, tg);
  fkLog('chest_take grab', p);
  fkLog('chest_take lid claw', clawTip(T, 0.45, 'left'));
  const restItem = [5, 11.5, -13];
  T.chest_item = { position: slideTo(p, restItem, tg) };
  const toss = (t) => { const u = lin(t, 2.4, 3.1); return t < 2.4 ? V() : t < 3.1 ? V(12 * u, 20 * Math.sin(PI * u) - 10 * u, 26 * u) : V(12, -10, 26 + 2 * smooth(lin(t, 3.1, 3.4))); };
  T.junk = { position: toss, rotation: (t) => V(540 * lin(t, 2.4, 3.1), 0, 300 * lin(t, 2.4, 3.1)) };
  anim('chest_take', L, 'hold', T, { chest_item: { scale: [[0, V(1, 1, 1)], [tg, V(0, 0, 0)]] }, held_item: { scale: [[0, V(0, 0, 0)], [tg, V(1, 1, 1)]] } });
}

// put into a chest: open, lean in, lower it, let go, pat, close, dust off the claws
{
  const L = 5.6, tr = 2.0;
  const [prA, prF] = solveArm('right', [5, 12, -12], -14, { held: true, rz: -4, preferA: 95, preferF: -50 });
  const [hkA, hkF] = solveArm('left', [-5, 12.5, -8.5], -6, { rz: 4, preferA: 55, preferF: 40 });
  const [rsA, rsF] = solveArm('left', [-5, 14.5, -7.5], -14, { rz: 6, preferA: 70, preferF: 20 });
  const T = keyedTracks({
    body: { rotation: [[0, Z], [0.3, V(-6, 0, 0)], [1.3, V(-14, 0, 0)], [2.6, V(-6, 0, 0)], [3.1, V(-8, 0, 0)], [3.9, Z]] },
    neck: { position: [[0, Z], [1.3, V(0, 1, 0)], [3.9, Z]] },
    head: { rotation: [[0, Z], [0.3, V(-12, 0, 0)], [1.3, V(-26, 0, 0)], [2.4, V(-14, 0, -10)], [3.9, V(-4, 0, 0)], [4.9, Z]] },
    eye_left: { rotation: [[0, Z], [2.3, V(0, 0, 9)], [3.6, Z]] },
    eye_right: { rotation: [[0, Z], [2.3, V(0, 0, -9)], [3.6, Z]] },
    lid_left: { position: [[0, Z], [2.3, V(0, 0.6, 0)], [3.6, Z]] },
    lid_right: { position: [[0, Z], [2.3, V(0, 0.6, 0)], [3.6, Z]] },
    arm_left: { rotation: [[0, Z], [0.35, V(hkA, 0, 4)], [0.85, V(150, 0, 6)], [1.3, V(rsA, 0, 6)], [3.0, V(150, 0, 6)], [3.45, V(85, 0, 4)], [3.95, V(50, 0, 18)], [5.0, Z]] },
    forearm_left: { rotation: [[0, Z], [0.35, V(hkF, 0, 0)], [0.85, Z], [1.3, V(rsF, 0, 0)], [3.0, Z], [3.95, V(40, 0, 0)], [5.0, Z]] },
    arm_right: { rotation: [[0, CARRY_ARM], [1.3, V(prA, 0, -4)], [tr + 0.2, V(prA - 6, 0, -4)], [2.8, V(40, 0, 0)], [3.95, V(50, 0, -18)], [5.0, Z]] },
    forearm_right: { rotation: [[0, CARRY_FORE], [1.3, V(prF, 0, 0)], [2.8, V(10, 0, 0)], [3.95, V(40, 0, 0)], [5.0, Z]] },
    chest_lid: { rotation: [[0, Z], [0.55, V(-105, 0, 0), 4, 4], [3.15, Z, 6, 14]] },
  }, {
    extra: {
      forearm_right: { rotation: (t) => V(10 * osc(t, 2.15, 2.75, 3.5), 0, 0) },
      arm_left: { rotation: (t) => V(0, 0, 9 * osc(t, 4.0, 4.9, 6)) },
      arm_right: { rotation: (t) => V(0, 0, -9 * osc(t, 4.0, 4.9, 6)) },
      body: { position: (t) => V(0, -0.6 * landKickAt(t, 3.2, 3, 7), 0) },
      aerial: { rotation: (t) => V(-25 * landKickAt(t, 3.2, 2, 5), 0, 0) },
    },
  });
  const p = heldCentre(T, tr);
  fkLog('chest_put release', p);
  T.chest_item = { position: settleFrom(p, [5, 11.5, -13], tr, 0.22) };
  anim('chest_put', L, 'hold', T, {
    held_item: { scale: [[0, V(1, 1, 1)], [tr, V(0, 0, 0)]] },
    chest_item: { scale: [[0, V(0, 0, 0)], [tr, V(1, 1, 1)]] },
    junk: { scale: [[0, V(0, 0, 0)]] },
  });
}

// "blind" hand-offs: containers he can't see into or reach (barrels, which just sit there, and chests up high or
// out of reach). He stands back half a block; the container opens by itself; the package simply pops into his claw
// (or vanishes from it). Baffled: claw, container, claw, shrug. Then it shuts by itself and he gives it a startled glance.
const BLIND = { open: 0.5, poof: 1.3, shrug: 2.85, close: 3.7 };
function blindHandoff(name, mode, c) {
  const B = BLIND, L = 4.6, tp = B.poof, look = c.lookPitch, sh = B.shrug;
  const T = keyedTracks({
    neck: { position: [[0, Z], [B.open, V(0, 1, 0), 7, 6], [tp, V(0, 2, 0), 9, 6], [tp + 0.6, V(0, 0.5, 0)], [sh + 0.15, V(0, -0.6, 0)], [sh + 0.4, V(0, 0.4, 0)], [B.close, V(0, 1.2, 0), 9, 6], [B.close + 0.5, Z]] },
    head: { rotation: [[0, Z], [B.open, V(look, 0, 0), 7, 6], [tp + 0.02, V(-25, -35, 12), 8, 6], [tp + 0.55, V(-10, -20, 14)], [2.3, V(look, 0, -10)], [2.65, V(-10, -20, 14)], [sh + 0.15, V(0, 0, 10)], [B.close, V(look, 0, 0), 9, 6], [B.close + 0.6, Z]] },
    eye_left: { rotation: [[0, Z], [tp + 0.02, V(0, 0, 8)], [sh + 0.4, Z]] },
    eye_right: { rotation: [[0, Z], [tp + 0.02, V(0, 0, 8)], [sh + 0.4, Z]] },
    lid_left: { position: [[0, Z], [B.open, V(0, 0.5, 0)], [tp + 0.02, V(0, -0.6, 0)], [sh + 0.4, Z], [B.close, V(0, 1, 0), 9, 6], [B.close + 0.5, Z]] },
    lid_right: { position: [[0, Z], [B.open, V(0, 0.5, 0)], [tp + 0.02, V(0, 0.6, 0)], [sh + 0.4, Z], [B.close, V(0, 1, 0), 9, 6], [B.close + 0.5, Z]] },
    iris_left: { scale: [[0, V(1, 1, 1)], [B.open, V(1.15, 1.15, 1)], [tp, V(0.75, 0.75, 1), 9, 6], [tp + 0.3, V(1.15, 1.15, 1)], [sh + 0.4, V(1, 1, 1)], [B.close, V(0.8, 0.8, 1), 9, 6], [B.close + 0.5, V(1, 1, 1)]] },
    iris_right: { scale: [[0, V(1, 1, 1)], [B.open, V(1.15, 1.15, 1)], [tp, V(0.75, 0.75, 1), 9, 6], [tp + 0.3, V(1.15, 1.15, 1)], [sh + 0.4, V(1, 1, 1)], [B.close, V(0.8, 0.8, 1), 9, 6], [B.close + 0.5, V(1, 1, 1)]] },
    aerial: { rotation: [[0, Z], [B.open, V(-8, 0, 0), 5, 4], [tp + 0.02, V(-10, 0, 25), 4, 3], [sh + 0.4, Z], [B.close, V(20, 0, 0), 8, 4], [B.close + 0.3, Z]] },
    body: { rotation: [[0, Z], [B.open, V(-3, 0, 0)], [tp, V(2, 0, 0), 9, 6], [tp + 0.4, Z], [B.close, V(4, 0, 0), 9, 6], [B.close + 0.4, Z]] },
    arm_left: { rotation: [[0, Z], [sh, V(40, 0, -18)], [sh + 0.5, Z]] },
    forearm_left: { rotation: [[0, Z], [sh, V(40, 0, 0)], [sh + 0.5, Z]] },
    ...(mode === 'take'
      ? {
          arm_right: { rotation: [[0, Z], [tp, V(10, 0, 0), 9, 5], [tp + 0.35, V(55, 0, -8)], [sh, V(40, 0, 18)], [sh + 0.5, CARRY_ARM]] },
          forearm_right: { rotation: [[0, Z], [tp + 0.35, V(60, 0, 0)], [sh, V(40, 0, 0)], [sh + 0.5, CARRY_FORE]] },
        }
      : {
          // holds the package out toward the container; it vanishes; he lifts the empty claw to look at it
          arm_right: { rotation: [[0, CARRY_ARM], [B.open + 0.2, V(c.offerArm, 0, -6)], [tp + 0.15, V(55, 0, -8)], [sh, V(40, 0, 18)], [sh + 0.5, Z]] },
          forearm_right: { rotation: [[0, CARRY_FORE], [B.open + 0.2, V(5, 0, 0)], [tp + 0.15, V(60, 0, 0)], [sh, V(40, 0, 0)], [sh + 0.5, Z]] },
        }),
    ...(c.keys || {}),
  }, {
    extra: {
      body: { position: (t) => V(0, 0.6 * env(t, sh, sh + 0.15, sh + 0.25, sh + 0.45), 0) },
      ...(mode === 'put' ? { claw_right: { rotation: (t) => V(0, 0, 12 * osc(t, tp + 0.2, tp + 0.9, 4)) } } : {}),
    },
  });
  // poof: pops in with a little overshoot, or swells and is gone
  T.held_item = mode === 'take'
    ? { scale: (t) => { const k = t < tp ? 0 : t < tp + 0.08 ? (1.25 * (t - tp)) / 0.08 : t < tp + 0.2 ? 1.25 - 0.25 * smooth((t - tp - 0.08) / 0.12) : 1; return V(k, k, k); } }
    : { scale: (t) => { const k = t < tp - 0.1 ? 1 : t < tp ? 1 + 0.25 * Math.sin((PI / 2) * ((t - tp + 0.1) / 0.1)) : t < tp + 0.08 ? 1.25 * (1 - (t - tp) / 0.08) : 0; return V(k, k, k); } };
  anim(name, L, 'hold', T, c.steps || {});
}

// barrels: the top switches open and closed by itself (like the vanilla textures)
const BARREL = {
  lookPitch: -10, offerArm: 80,
  steps: { barrel_top_closed: { scale: [[0, V(1, 1, 1)], [BLIND.open, V(0, 0, 0)], [BLIND.close, V(1, 1, 1)]] }, barrel_item: { scale: [[0, V(0, 0, 0)]] } },
};
blindHandoff('barrel_take', 'take', BARREL);
blindHandoff('barrel_put', 'put', BARREL);

// a chest up high or out of reach: the lid swings open by itself and he looks up at it
const HIGH_CHEST = {
  lookPitch: 24, offerArm: 125,
  keys: { chest_high_lid: { rotation: [[0, Z], [BLIND.open, V(-105, 0, 0), 4, 4], [BLIND.close, Z, 6, 14]] } },
};
blindHandoff('chest_take_high', 'take', HIGH_CHEST);
blindHandoff('chest_put_high', 'put', HIGH_CHEST);

// onto a depot: set it down in the same spot, a beat ("hmm, not quite"), push it into the middle, lean back and admire
{
  const L = 5.6, tr = 1.05, tp = 2.3, push = [-6, 0, -5]; // the depot's centre is 6 px left and 5 px further than the drop spot
  const [dA, dF] = solveArm('right', [6, 11.8, -11], -10, { held: true, rz: -2, preferA: 95, preferF: -20 });
  const [p0A, p0F] = solveArm('right', [6, 11.6, -8.8], -10, { rz: -2, preferA: 100, preferF: -40 });
  const [p1A, p1F] = solveArm('right', [6, 11.6, -13.8], -15, { rz: -2, preferA: 100, preferF: -20 });
  const T = keyedTracks({
    body: { rotation: [[0, Z], [0.3, V(-10, 0, 0)], [1.3, V(-6, 0, 0)], [2.0, V(-10, 0, 0)], [tp, V(-15, 0, 0), 4, 6], [3.05, V(-4, 0, 0)], [3.5, V(3, 0, 0)], [4.8, Z]] },
    head: { rotation: [[0, Z], [0.3, V(-18, 0, 0)], [1.35, V(-16, 0, 14)], [2.0, V(-20, 0, 0)], [3.5, V(-6, 0, -12)], [4.9, Z]] },
    eye_left: { rotation: [[0, Z], [1.35, V(0, 0, 6)], [2.0, Z], [3.5, V(0, 0, 10)], [4.9, Z]] },
    eye_right: { rotation: [[0, Z], [1.35, V(0, 0, -2)], [2.0, Z], [3.5, V(0, 0, -10)], [4.9, Z]] },
    lid_left: { position: [[0, Z], [1.35, V(0, 0.4, 0)], [2.0, Z], [3.5, V(0, 0.6, 0)], [4.9, Z]] },
    lid_right: { position: [[0, Z], [1.35, V(0, 0.4, 0)], [2.0, Z], [3.5, V(0, 0.6, 0)], [4.9, Z]] },
    iris_left: { scale: [[0, V(1, 1, 1)], [1.35, V(1.2, 1.2, 1)], [2.0, V(1, 1, 1)], [3.5, V(1.2, 1.2, 1)], [4.9, V(1, 1, 1)]] },
    iris_right: { scale: [[0, V(1, 1, 1)], [1.35, V(1.2, 1.2, 1)], [2.0, V(1, 1, 1)], [3.5, V(1.2, 1.2, 1)], [4.9, V(1, 1, 1)]] },
    aerial: { rotation: [[0, Z], [1.35, V(0, 0, -12)], [2.0, Z]] },
    arm_right: { rotation: [[0, CARRY_ARM], [0.45, V(dA, 0, -2)], [1.3, V(dA - 20, 0, 0)], [2.0, V(p0A, 0, -2)], [tp, V(p1A, 0, -20), 4, 7], [3.05, V(30, 0, 0)], [3.7, Z]] },
    forearm_right: { rotation: [[0, CARRY_FORE], [0.45, V(dF, 0, 0)], [1.3, V(dF + 10, 0, 0)], [2.0, V(p0F, 0, 0)], [tp, V(p1F, 0, 0), 4, 7], [3.05, V(20, 0, 0)], [3.7, Z]] },
  });
  const p = heldCentre(T, tr);
  fkLog('depot_place release', p);
  fkLog('depot_place push start claw', clawTip(T, tp, 'right'));
  const settle = settleFrom(p, [6, 11.5, -11], tr, 0.2);
  const slide = (t) => smooth(lin(t, tp + 0.12, tp + 0.6));
  T.depot_item = {
    position: (t) => addV(settle(t), push.map((x) => x * slide(t))),
    rotation: (t) => V(0, 10 * Math.sin(PI * slide(t)), 0),
  };
  anim('depot_place', L, 'hold', T, { held_item: { scale: [[0, V(1, 1, 1)], [tr, V(0, 0, 0)]] }, depot_item: { scale: [[0, V(0, 0, 0)], [tr, V(1, 1, 1)]] } });
}

// take from a depot: the package sits in the middle, out of reach. He rolls in 2 px, puts his claw on top of it,
// drags it toward himself to the near edge, lets go, grabs it properly, lifts it into the carry pose, rolls back.
// IK with the sideways angle solved too (yaw, Y: with X applied first, Y swings a forward-pointing arm sideways)
function solveArm3(side, target, lean, rootZ, o = {}) {
  const pt = o.held ? [6, 1.5, 0] : [side === 'right' ? 6 : -6, 3, 0];
  const chain = o.held ? HELD_CHAIN : ['trade_bot', 'body', 'arm_' + side, 'forearm_' + side, 'claw_' + side];
  let best = null;
  for (let z = -50; z <= 50; z += 2) for (let a = 20; a <= 170; a += 2) for (let f = -120; f <= 60; f += 2) {
    const T = { trade_bot: { position: () => V(0, 0, rootZ) }, body: { rotation: () => V(lean, 0, 0) }, ['arm_' + side]: { rotation: () => V(a, z, 0) }, ['forearm_' + side]: { rotation: () => V(f, 0, 0) } };
    const p = fk(T, chain, 0, pt);
    const e = Math.hypot(p[0] - target[0], p[1] - target[1], p[2] - target[2]) + 0.004 * Math.abs(a - (o.preferA === undefined ? 90 : o.preferA));
    if (!best || e < best[0]) best = [e, a, f, z, p];
  }
  fkLog('solve3 ' + side + ' -> [' + target + '] arm ' + best[1] + ' fore ' + best[2] + ' ry ' + best[3] + ' at', best[4]);
  return [best[1], best[2], best[3]];
}
{
  const L = 3.8, t0 = 0.9, t1 = 1.45, tg = 2.05, rootZ = -2;
  const centre = [0, 11.5, -16], restC = [6, 11.5, -11]; // the package centre at the depot's middle, and at its drop spot
  const [a0, f0, z0] = solveArm3('right', [0, 13.3, -15.2], -20, rootZ, { preferA: 110 }); // claw tip on top of the package, in the middle
  const [a1, f1, z1] = solveArm3('right', [6, 13, -11], -10, rootZ, { preferA: 100 }); // dragged to the near edge
  const [aG, fG, zG] = solveArm3('right', restC, -10, rootZ, { held: true, preferA: 100 }); // grab it properly
  const T = keyedTracks({
    trade_bot: { position: [[0, Z], [0.02, V(0, 0, rootZ), 4, 6], [2.35, Z, 4, 6]] },
    body: { rotation: [[0, Z], [0.2, V(-20, 0, 0)], [t0, V(-10, 0, 0), 4, 6], [1.6, V(-10, 0, 0)], [tg + 0.1, V(-4, 0, 0)], [2.6, Z]] },
    head: { rotation: [[0, Z], [0.3, V(-22, 0, 0)], [1.5, V(-18, 0, 0)], [2.3, V(-4, 0, -10)], [3.3, Z]] },
    eye_left: { rotation: [[0, Z], [2.3, V(0, 0, 8)], [3.3, Z]] },
    eye_right: { rotation: [[0, Z], [2.3, V(0, 0, -8)], [3.3, Z]] },
    lid_left: { position: [[0, Z], [2.3, V(0, 0.5, 0)], [3.3, Z]] },
    lid_right: { position: [[0, Z], [2.3, V(0, 0.5, 0)], [3.3, Z]] },
    arm_right: { rotation: [[0, Z], [0.3, V(a0, z0, 0), 5, 10], [t0, V(a1, z1, 0), 4, 7], [1.5, V(a1 - 18, z1, 0)], [1.65, V(aG, zG, 0)], [tg + 0.15, CARRY_ARM]] },
    forearm_right: { rotation: [[0, Z], [0.3, V(f0, 0, 0), 5, 10], [t0, V(f1, 0, 0), 4, 7], [1.5, V(f1 + 10, 0, 0)], [1.65, V(fG, 0, 0)], [tg + 0.15, CARRY_FORE]] },
  }, {
    extra: { body: { position: (t) => V(0, 0.4 * env(t, 2.3, 2.4, 2.45, 2.6), 0) } },
  });
  // the wheel rolls with the 2 px approach and back
  const wheelBase = T.wheel.rotation;
  T.wheel.rotation = (t) => addV(wheelBase(t), V((T.trade_bot.position(t)[2] / 4) * (180 / PI), 0, 0));
  // the package: in the middle, then dragged under his claw (relative motion, so nothing jumps), then into the claw
  const under = (t) => clawTip(T, t, 'right');
  const s0 = under(t0), s1 = under(t1);
  const centreOff = centre.map((x, i) => x - restC[i]);
  const dragged = (t) => { const u = under(Math.min(Math.max(t, t0), t1)); return centreOff.map((x, i) => x + u[i] - s0[i]); };
  const g = heldCentre(T, tg), endOff = dragged(t1);
  fkLog('depot_take drag start claw', s0);
  fkLog('depot_take drag end claw', s1);
  fkLog('depot_take grab', g);
  T.depot_item = { position: (t) => (t < tg - 0.15 ? dragged(t) : endOff.map((x, i) => x + (g[i] - restC[i] - x) * smooth(lin(t, tg - 0.15, tg)))) };
  anim('depot_take', L, 'hold', T, { depot_item: { scale: [[0, V(1, 1, 1)], [tg, V(0, 0, 0)]] }, held_item: { scale: [[0, V(0, 0, 0)], [tg, V(1, 1, 1)]] } });
}

// trading with a seated villager, a block away (with the workstation in between, or with nothing in between):
// lamp on, wave hi, drawer out, take the package, present it high, haggle, lob it over to the villager,
// the villager tosses an emerald back into his open drawer, drawer shut, spin-and-hop, lamp off, wave bye
function tradeClip(name, withStation) {
  const L = 7.2, tTake = 1.45, tr = 3.42, tLand = 3.97, te = 4.45, tCatch = 5.0, tShut = 5.15;
  const [dA, dF, dY] = solveArm3('right', [2, 12.5, -7], -8, 0, { held: true, preferA: 40 }); // into the drawer
  const T = keyedTracks({
    head: { rotation: [[0, Z], [0.05, V(-4, 0, 0), 8, 6], [1.15, V(-14, 0, 0)], [1.6, V(8, 0, 0)], [3.0, V(6, 0, 0)], [3.3, V(-4, 0, 0)], [4.45, V(2, 0, 0)], [tCatch - 0.1, V(-18, 0, 0), 8, 6], [5.3, Z]] },
    eye_left: { rotation: [[0, Z], [0.35, V(0, 0, 8)], [1.2, Z], [2.45, V(0, 0, 10)], [2.85, V(0, 0, 8)], [3.4, Z], [5.3, V(0, 0, 10)], [6.4, Z]] },
    eye_right: { rotation: [[0, Z], [0.35, V(0, 0, -8)], [1.2, Z], [2.45, V(0, 0, -10)], [2.85, V(0, 0, -8)], [3.4, Z], [5.3, V(0, 0, -10)], [6.4, Z]] },
    lid_left: { position: [[0, Z], [0.05, V(0, 0.6, 0)], [0.6, Z], [2.45, V(0, -0.6, 0)], [2.85, V(0, 0.5, 0)], [3.4, Z], [tCatch - 0.05, V(0, 1, 0), 9, 6], [5.6, Z]] },
    lid_right: { position: [[0, Z], [0.05, V(0, 0.6, 0)], [0.6, Z], [2.45, V(0, -0.6, 0)], [2.85, V(0, 0.5, 0)], [3.4, Z], [tCatch - 0.05, V(0, 1, 0), 9, 6], [5.6, Z]] },
    iris_left: { scale: [[0, V(1, 1, 1)], [0.05, V(1.2, 1.2, 1), 8, 6], [0.6, V(1, 1, 1)], [tCatch - 0.05, V(1.25, 1.25, 1)], [5.6, V(1, 1, 1)]] },
    iris_right: { scale: [[0, V(1, 1, 1)], [0.05, V(1.2, 1.2, 1), 8, 6], [0.6, V(1, 1, 1)], [tCatch - 0.05, V(1.25, 1.25, 1)], [5.6, V(1, 1, 1)]] },
    drawer: { position: [[0, Z], [0.95, V(0, 0, -3), 5, 7], [tShut, Z, 7, 8]] },
    body: { rotation: [[0, Z], [1.15, V(-8, 0, 0)], [1.6, V(4, 0, 0)], [3.0, V(6, 0, 0)], [3.22, V(-10, 0, 0), 9, 6], [3.7, Z], [4.45, V(-6, 0, 0)], [tShut, Z]] },
    arm_right: { rotation: [[0, Z], [0.35, V(-10, 0, 115)], [0.95, Z], [1.15, V(dA, dY, 0), 5, 9], [1.6, V(150, 0, 10)], [3.0, V(-35, 0, 8)], [3.22, V(135, 0, 4), 9, 6], [3.7, V(20, 0, 0)], [4.4, V(40, 0, 25)], [tShut, Z], [5.3, V(160, 0, 30)], [6.0, Z], [6.4, V(-10, 0, 115)], [7.0, Z]] },
    forearm_right: { rotation: [[0, Z], [0.35, V(0, 0, 45)], [0.95, Z], [1.15, V(dF, 0, 0), 5, 9], [1.6, V(10, 0, 0)], [3.0, V(20, 0, 0)], [3.22, V(-10, 0, 0), 9, 6], [3.7, Z], [5.3, V(10, 0, 0)], [6.0, Z], [6.4, V(0, 0, 45)], [7.0, Z]] },
    arm_left: { rotation: [[0, Z], [4.4, V(40, 0, -25)], [tShut, Z], [5.3, V(160, 0, -30)], [6.0, Z]] },
    trade_bot: { rotation: [[0, Z], [5.3, V(0, 360, 0), 2.5, 5]] },
  }, {
    extra: {
      forearm_right: { rotation: (t) => V(0, 0, 22 * osc(t, 0.4, 0.95, 2.6) + 22 * osc(t, 6.45, 7.0, 2.6)) },
      head: { rotation: (t) => V(6 * env(t, 2.45, 2.55, 2.7, 2.85), 0, 10 * osc(t, 1.9, 2.4, 2)) },
      body: { rotation: (t) => V(0, 0, 4 * osc(t, 1.9, 3.0, 1.6)) },
      trade_bot: { position: (t) => V(0, 3 * hopArc(t, 5.4, 5.8), 0) },
      villager_head: { rotation: (t) => V(10 * osc(t, 2.8, 3.25, 3.5) + 8 * osc(t, 3.95, 4.35, 3.5), 14 * osc(t, 2.2, 2.7, 4), 0) },
    },
  });
  // the lob: from the claw at release, in a high arc that clears a full block, into the villager's arms
  const p0 = heldCentre(T, tr), pkgRest = [0, 19.5, -34];
  const arc = (a, b, u, h) => a.map((x, i) => x + (b[i] - x) * u + (i === 1 ? h * 4 * u * (1 - u) : 0));
  T.trade_package = {
    position: (t) => { const u = lin(t, tr, tLand); return arc(p0, pkgRest, u, 14).map((x, i) => x - pkgRest[i]); },
    rotation: (t) => V(360 * lin(t, tr, tLand), 0, 0),
  };
  // the payment: an emerald from the villager's hands, arcing back into his open drawer (then riding the drawer until it shuts)
  const gemRest = [0, 19, -34];
  const inDrawer = (t) => fk(T, ['trade_bot', 'body', 'drawer'], t, [0, 12, -2]);
  const gemLand = inDrawer(tCatch);
  T.trade_emerald = {
    position: (t) => (t < tCatch ? arc(gemRest, gemLand, lin(t, te, tCatch), 10) : inDrawer(t)).map((x, i) => x - gemRest[i]),
    rotation: (t) => V(0, 540 * lin(t, te, tCatch), 0),
  };
  if (DEBUG_FK && withStation) {
    const box = [[-8, 0, -30], [8, 16, -14]];
    const pts = [[['trade_bot', 'body', 'drawer'], [0, 9.5, -6]], [['trade_bot', 'body', 'drawer'], [3, 8, -5]], [['trade_bot', 'body', 'drawer'], [-3, 8, -5]]];
    for (const sd of ['left', 'right']) { const x = sd === 'right' ? 6 : -6; pts.push([['trade_bot', 'body', 'arm_' + sd, 'forearm_' + sd, 'claw_' + sd], [x, 3, 0]], [['trade_bot', 'body', 'arm_' + sd, 'forearm_' + sd], [x, 10, 0]]); }
    for (const z of [-4, 4]) for (const y of [7, 16]) for (const x of [-5, 5]) pts.push([['trade_bot', 'body'], [x, y, z]]);
    let worst = [0, 0, ''];
    for (let t = 0; t <= L; t += 1 / 60) for (const [chain, pt] of pts) {
      const p = fk(T, chain, t, pt);
      const d = Math.min(p[0] - box[0][0], box[1][0] - p[0], p[1] - box[0][1], box[1][1] - p[1], p[2] - box[0][2], box[1][2] - p[2]);
      if (d > worst[0]) worst = [d, t, chain[chain.length - 1]];
    }
    console.error('clearance ' + name + ' vs workstation: deepest overlap ' + worst[0].toFixed(2) + ' px at t=' + worst[1].toFixed(2) + ' (' + worst[2] + ')');
  }
  fkLog(name + ' release', p0);
  fkLog(name + ' emerald lands in drawer', gemLand);
  anim(name, L, 'hold', T, {
    lamp_glow: { scale: [[0, V(0, 0, 0)], [0.3, V(1, 1, 1)], [6.4, V(0, 0, 0)]] },
    held_item: { scale: [[0, V(0, 0, 0)], [tTake, V(1, 1, 1)], [tr, V(0, 0, 0)]] },
    trade_package: { scale: [[0, V(0, 0, 0)], [tr, V(1, 1, 1)], [4.35, V(0, 0, 0)]] },
    trade_emerald: { scale: [[0, V(0, 0, 0)], [te, V(1, 1, 1)], [tShut, V(0, 0, 0)]] },
    ...(withStation ? {} : { workstation: { scale: [[0, V(0, 0, 0)]] } }),
  });
}
tradeClip('trade_villager', true);
tradeClip('trade_villager_open', false);

// onto a belt: lay it on, let go, watch it ride away, wave it goodbye
{
  const L = 5.2, tr = 1.0, rideFrom = 1.3, speed = 6;
  const [beA, beF] = solveArm('right', [6, 7.8, -12], -12, { held: true, rz: -2, preferA: 75, preferF: -10 });
  const ride = (t) => (t < rideFrom ? 0 : Math.min(22, speed * (t - rideFrom)));
  const look = (t) => (t < 1.2 ? 0 : -Math.atan2(6 + ride(t), 13) * (180 / PI) * seg(t, 1.2, 1.5));
  const T = keyedTracks({
    body: { rotation: [[0, Z], [0.3, V(-12, 0, 0)], [1.5, V(-3, 0, 0)], [4.6, Z]] },
    head: { rotation: [[0, Z], [0.3, V(-22, 0, 0)], [1.4, V(-8, 0, 0)], [3.2, V(-4, 0, -10)], [4.6, Z]] },
    eye_left: { rotation: [[0, Z], [3.1, V(0, 0, 9)], [4.6, Z]] },
    eye_right: { rotation: [[0, Z], [3.1, V(0, 0, -9)], [4.6, Z]] },
    trade_bot: { rotation: [[0, Z], [1.6, V(0, -12, 0), 3, 5], [4.6, Z, 3, 5]] },
    arm_right: { rotation: [[0, CARRY_ARM], [0.45, V(beA, 0, -2)], [1.7, Z], [3.1, V(-10, 0, 115)], [4.4, Z]] },
    forearm_right: { rotation: [[0, CARRY_FORE], [0.45, V(beF, 0, 0)], [1.7, Z], [3.1, V(0, 0, 45)], [4.4, Z]] },
  }, {
    extra: {
      head: { rotation: (t) => V(0, look(t) * (1 - seg(t, 4.3, 4.8)) + 12 * seg(t, 1.6, 1.9) * (1 - seg(t, 4.3, 4.8)), 0) },
      forearm_right: { rotation: (t) => V(0, 0, 22 * osc(t, 3.25, 4.25, 2.6)) },
      claw_right: { rotation: (t) => V(0, 0, -12 * osc(t, 3.25, 4.25, 2.6, -0.9)) },
    },
  });
  const p = heldCentre(T, tr);
  fkLog('belt_place release', p);
  const settle = settleFrom(p, [6, 7.5, -12], tr, 0.18);
  T.belt_item = { position: (t) => addV(settle(t), V(ride(t), 0, 0)) };
  anim('belt_place', L, 'hold', T, { held_item: { scale: [[0, V(1, 1, 1)], [tr, V(0, 0, 0)]] }, belt_item: { scale: [[0, V(0, 0, 0)], [tr, V(1, 1, 1)]] } });
}

// clearance: sample his wheel rim, chassis corners, drawer handle and key against a box and report the deepest overlap
function clearance(name, T, L, boxes) {
  if (typeof boxes[0][0] === 'number') boxes = [boxes];
  const pts = [];
  for (let k = 0; k < 12; k++) for (const x of [-1.5, 1.5]) pts.push([['trade_bot', 'wheel'], [x, 4 + 4 * Math.sin((k * PI) / 6), 4 * Math.cos((k * PI) / 6)]]);
  for (const x of [-5, 5]) for (const y of [7, 11.5, 16]) for (const z of [-4, 4]) pts.push([['trade_bot', 'body'], [x, y, z]]);
  pts.push([['trade_bot', 'body'], [0, 9.5, -6]], [['trade_bot', 'body'], [0, 12, 6]], [['trade_bot', 'body'], [0, 8, -5]]);
  let worst = [0, 0];
  for (let t = 0; t <= L; t += 1 / 60) for (const [chain, pt] of pts) {
    const p = fk(T, chain, t, pt);
    for (const box of boxes) {
      const d = Math.min(p[0] - box[0][0], box[1][0] - p[0], p[1] - box[0][1], box[1][1] - p[1], p[2] - box[0][2], box[1][2] - p[2]);
      if (d > worst[0]) worst = [d, t];
    }
  }
  if (DEBUG_FK) console.error('clearance ' + name + ': deepest overlap ' + worst[0].toFixed(2) + ' px at t=' + worst[1].toFixed(2));
}

for (const c of CLEAR_CHECKS) clearance(...c);

// 9. climbing on and off slabs (8 px) and full blocks (16 px): the stair hop, scaled by height
function climbOn(name, h) {
  const big = h > 8;
  const ta = big ? 0.78 : 0.62, tl = ta + (big ? 0.5 : 0.36), L = tl + 1.6;
  const z0 = big ? -3 : -4.5, z1 = -18, arc = big ? 6 : 5, e = big ? 1.5 : 1; // e: effort
  // up first (peak at 60% of the hop), forward only from 30% on, when the wheel is already above the top
  const zPos = (t) => (t < ta ? z0 * seg(t, 0, 0.5) : t <= tl ? z0 + (z1 - z0) * smooth(lin(lin(t, ta, tl), 0.3, 1)) : z1);
  const yPos = (t) => { if (t < ta) return 0; if (t > tl) return h; const u = lin(t, ta, tl); return u < 0.6 ? (h + arc) * Math.sin((PI / 2) * (u / 0.6)) : h + arc * Math.cos((PI / 2) * ((u - 0.6) / 0.4)); };
  const A = (t) => bumpBefore(t, ta, big ? 0.26 : 0.15);
  const F = (t) => hopArc(t, ta, tl);
  const K = (t) => landKickAt(t, tl, big ? 2.0 : 2.2, big ? 5 : 6);
  const Kd = (t) => landKickAt(t, tl + 0.06, 2.0, 5);
  const wig = (t) => 3 * osc(t, tl + 0.45, tl + 1.15, 2.2);
  const T = {
    ...idle,
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)) },
    wheel: { rotation: (t) => V((zPos(t) / 4) * (180 / PI) - (big ? 420 : 300) * lin(t, ta, tl), 0, 0) },
    body: { position: (t) => V(0, -1 * e * A(t) - 1.3 * e * Math.max(0, K(t)), 0), rotation: (t) => addV(idle.body.rotation(t), V(-5 * e * A(t) - 7 * F(t) - 9 * e * K(t), 0, wig(t))) },
    neck: { position: (t) => V(0, -0.8 * e * A(t) + 0.7 * F(t) - 0.8 * Math.max(0, Kd(t)), 0) },
    head: {
      rotation: (t) => addV(idle.head.rotation(t), V(-6 * e * A(t) + 11 * F(t) - 13 * e * Kd(t), 0, -2 * wig(t))),
      scale: (t) => { const q = 0.1 * e * Math.max(0, K(t)); return V(1 + 0.5 * q, 1 - q, 1 + 0.5 * q); },
    },
    eye_left: { rotation: (t) => V(0, 0, 5 * Kd(t) + 8 * env(t, tl + 0.35, tl + 0.5, tl + 1.1, tl + 1.3)) },
    eye_right: { rotation: (t) => V(0, 0, -5 * landKickAt(t, tl + 0.1, 2.0, 5) - 8 * env(t, tl + 0.35, tl + 0.5, tl + 1.1, tl + 1.3)) },
    lid_left: { position: (t) => V(0, 0.7 * F(t) - 0.4 * A(t) + 0.5 * env(t, tl + 0.35, tl + 0.5, tl + 1.1, tl + 1.3), 0) },
    lid_right: { position: (t) => V(0, 0.7 * F(t) - 0.4 * A(t) + 0.5 * env(t, tl + 0.35, tl + 0.5, tl + 1.1, tl + 1.3), 0) },
    iris_left: { scale: (t) => { const q = 1 + 0.12 * F(t); return V(q, q, 1); } },
    iris_right: { scale: (t) => { const q = 1 + 0.12 * F(t); return V(q, q, 1); } },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(22 * F(t) - 38 * e * Kd(t), 0, 2.5 * wig(t))) },
    arm_left: { rotation: (t) => addV(idle.arm_left.rotation(t), V(-28 * e * A(t) + 32 * e * F(t) + 12 * K(t), 0, -6 * F(t))) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(-28 * e * A(t) + 32 * e * F(t) + 12 * K(t), 0, 6 * F(t))) },
    forearm_left: { rotation: (t) => V(18 * F(t) + 10 * Kd(t), 0, 0) },
    forearm_right: { rotation: (t) => V(18 * F(t) + 10 * Kd(t), 0, 0) },
  };
  clearance(name, T, L, [[-8, 0, -26], [8, h, -10]]);
  anim(name, L, 'once', T);
}
function climbOff(name, h) {
  const big = h > 8;
  const ta = 0.55, drop = big ? 0.5 : 0.38, tl = ta + drop, L = tl + 1.7;
  const e = big ? 1.5 : 1;
  // facing back toward +Z: roll to the edge, a little hop out, fall, land
  const zPos = (t) => (t < ta ? -18 + 5 * seg(t, 0, ta) : t <= tl ? -13 + 10.5 * Math.sin((PI / 2) * lin(t, ta, tl)) : -2.5 + 9 * seg(t, tl + 0.5, L));
  const bounce = (u) => { const b1 = big ? 2.0 : 1.2, b2 = big ? 0.7 : 0.4; return u < 0 ? 0 : u < 0.2 ? b1 * Math.sin((PI * u) / 0.2) : u < 0.33 ? b2 * Math.sin((PI * (u - 0.2)) / 0.13) : 0; };
  // stays up while it clears the edge, then falls
  const yPos = (t) => { if (t < ta) return h; if (t <= tl) { const u = lin(t, ta, tl); const f = Math.max(0, (u - 0.45) / 0.55); return h + 3 * Math.sin(PI * u) - h * f * f; } return bounce(t - tl); };
  const D = (t) => (t > ta - 0.04 && t < tl + 0.04 ? Math.sin(PI * lin(t, ta - 0.04, tl + 0.04)) : 0);
  const K = (t) => landKickAt(t, tl, 2.4, big ? 5 : 6);
  const Kd = (t) => landKickAt(t, tl + 0.07, 2.0, 5);
  const T = {
    ...idle,
    trade_bot: { position: (t) => V(0, yPos(t), zPos(t)), rotation: () => V(0, 180, 0) },
    wheel: { rotation: (t) => V((-(zPos(t) + 18) / 4) * (180 / PI), 0, 0) },
    body: { position: (t) => V(0, -1.1 * e * Math.max(0, K(t)), 0), rotation: (t) => addV(idle.body.rotation(t), V(-10 * D(t) + 9 * e * K(t), 0, 0)) },
    neck: { position: (t) => V(0, 0.5 * D(t) - 0.7 * e * Math.max(0, Kd(t)), 0) },
    head: {
      rotation: (t) => addV(idle.head.rotation(t), V(7 * D(t) - 12 * e * Kd(t), 0, 0)),
      scale: (t) => { const q = (big ? 0.14 : 0.08) * Math.max(0, K(t)); return V(1 + 0.5 * q, 1 - q, 1 + 0.5 * q); },
    },
    eye_left: { rotation: (t) => V(0, 0, 4 * e * Kd(t)) },
    eye_right: { rotation: (t) => V(0, 0, -4 * e * Kd(t)) },
    lid_left: { position: (t) => V(0, 0.8 * D(t) + (big ? blink(t, tl + 0.3) + blink(t, tl + 0.55) : 0), 0) },
    lid_right: { position: (t) => V(0, 0.8 * D(t) + (big ? blink(t, tl + 0.3) + blink(t, tl + 0.55) : 0), 0) },
    iris_left: { scale: (t) => { const q = 1 + 0.15 * D(t); return V(q, q, 1); } },
    iris_right: { scale: (t) => { const q = 1 + 0.15 * D(t); return V(q, q, 1); } },
    aerial: { rotation: (t) => addV(idle.aerial.rotation(t), V(26 * D(t) - 36 * e * Kd(t), 0, 0)) },
    arm_left: { rotation: (t) => addV(idle.arm_left.rotation(t), V(10 * D(t) - 8 * K(t), 0, -32 * D(t))) },
    arm_right: { rotation: (t) => addV(idle.arm_right.rotation(t), V(10 * D(t) - 8 * K(t), 0, 32 * D(t))) },
    forearm_left: { rotation: (t) => V(12 * D(t), 0, -15 * D(t)) },
    forearm_right: { rotation: (t) => V(12 * D(t), 0, 15 * D(t)) },
  };
  // the props sit at z -26..-10; facing +Z he starts on top at z -18 and drops off the near edge
  clearance(name, T, L, [[-8, 0, -26], [8, h, -10]]);
  anim(name, L, 'once', T);
}
climbOn('hop_on_slab', 8);
climbOff('hop_off_slab', 8);
climbOn('hop_on_block', 16);
climbOff('hop_off_block', 16);

// 10. locomotion set for the game: start rolling → rolling (seamless loop) → stop rolling. In place (the game moves
// the entity). Each clip's last frame equals the next clip's first frame, so they chain without a pop.
const ROLL = { wheel: 120, lean: -6, head: 2, aerial: 8, arm: -6, fore: 10 }; // cruise pose; wheel deg/s = 360 per 3 s loop
// wheel and wind-up key carry on from clip to clip: start_rolling begins at rest (0) and turns the wheel exactly 90°
// (the wheel looks the same every quarter turn) and the key 60°/s; the loop and the stop pick up from there
const START_L = 1.3, START_WHEEL = 90, START_KEY = 60 * START_L;
const W2 = 2 * PI;
// the cruise loop (3 s): everything oscillates around the cruise pose; each term is 0 at t = 0 and periodic over 3 s
const lagSin = (t, hz, ph) => Math.sin(W2 * hz * t - ph) + Math.sin(ph);
const cruise = {
  wheel: { rotation: (t) => V(-(START_WHEEL + ROLL.wheel * t), 0, 0) },
  body: { rotation: (t) => V(ROLL.lean + 0.8 * Math.sin(W2 * 2 * t), 0, 1.5 * Math.sin((W2 * t) / 3)), position: (t) => V(0, 0.3 * Math.sin(W2 * 2 * t), 0) },
  head: { rotation: (t) => V(ROLL.head + 1.2 * lagSin(t, 2, 0.8), 0, -1.0 * lagSin(t, 1 / 3, 0.6)) },
  eye_left: { rotation: (t) => V(0, 0, 1.2 * lagSin(t, 1 / 3, 1.1)) },
  eye_right: { rotation: (t) => V(0, 0, 1.2 * lagSin(t, 1 / 3, 1.1)) },
  aerial: { rotation: (t) => V(ROLL.aerial + 6 * lagSin(t, 2, 1.4), 0, 4 * lagSin(t, 1 / 3, 1.0)) },
  arm_left: { rotation: (t) => V(ROLL.arm + 5 * Math.sin(W2 * t), 0, -2 * Math.sin((W2 * t) / 3)) },
  arm_right: { rotation: (t) => V(ROLL.arm - 5 * Math.sin(W2 * t), 0, -2 * Math.sin((W2 * t) / 3)) },
  forearm_left: { rotation: (t) => V(ROLL.fore + 4 * lagSin(t, 1, 0.5), 0, 0) },
  forearm_right: { rotation: (t) => V(ROLL.fore - 4 * lagSin(t, 1, 0.5) + 8 * Math.sin(0.5), 0, 0) },
  key: { rotation: (t) => V(0, 0, -(START_KEY + 60 * t)) },
  lid_left: { position: (t) => V(0, blink(t, 1.7), 0) },
  lid_right: { position: (t) => V(0, blink(t, 1.7), 0) },
};
anim('rolling', 3, 'loop', cruise);

// start rolling: a small wind-up, then a lunge into the acceleration as the wheel spins up; head, aerial and arms lag,
// then everything settles into the cruise pose (which the last frame matches exactly)
{
  const L = START_L, dt = 1 / 600, n = Math.ceil(L / dt) + 2, ang = new Float64Array(n);
  // spin-up from rest to cruise speed, timed (bisection on the ramp end) so the wheel turns exactly START_WHEEL degrees
  const integrate = (e) => { for (let k = 1; k < n; k++) ang[k] = ang[k - 1] + ROLL.wheel * seg(k * dt, 0.12, e) * dt; return ang[Math.round(L / dt)]; };
  let lo = 0.2, hi = L - 0.02; for (let it = 0; it < 40; it++) { const mid = (lo + hi) / 2; if (integrate(mid) > START_WHEEL) lo = mid; else hi = mid; }
  integrate((lo + hi) / 2);
  const wheelAngle = (t) => -ang[Math.min(n - 1, Math.round(t / dt))];
  const at = (track, ch, t) => cruise[track][ch](t);
  const T = keyedTracks({
    body: { rotation: [[0, Z], [0.05, V(3, 0, 0)], [0.18, V(-12, 0, 0), 4, 5], [0.75, V(ROLL.lean, 0, 0), 3, 6]], position: [[0, Z], [0.03, V(0, -0.4, 0)], [0.16, Z]] },
    head: { rotation: [[0, Z], [0.05, V(-2, 0, 0)], [0.2, V(6, 0, 0), 5, 5], [0.75, V(ROLL.head, 0, 0), 3, 6]] },
    aerial: { rotation: [[0, Z], [0.2, V(25, 0, 0), 4, 3], [0.8, V(ROLL.aerial, 0, 0), 3, 5]] },
    arm_left: { rotation: [[0, Z], [0.05, V(6, 0, 0)], [0.2, V(-20, 0, 0)], [0.8, V(ROLL.arm, 0, 0), 3, 6]] },
    arm_right: { rotation: [[0, Z], [0.05, V(6, 0, 0)], [0.2, V(-20, 0, 0)], [0.8, V(ROLL.arm, 0, 0), 3, 6]] },
    forearm_left: { rotation: [[0, Z], [0.25, V(14, 0, 0)], [0.8, V(ROLL.fore, 0, 0), 3, 6]] },
    forearm_right: { rotation: [[0, Z], [0.25, V(14, 0, 0)], [0.8, V(ROLL.fore, 0, 0), 3, 6]] },
    lid_left: { position: [[0, Z], [0.15, V(0, 0.4, 0)], [0.9, Z]] },
    lid_right: { position: [[0, Z], [0.15, V(0, 0.4, 0)], [0.9, Z]] },
  }, { noIdle: true });
  T.wheel = { rotation: (t) => V(wheelAngle(t), 0, 0) };
  T.key = { rotation: (t) => V(0, 0, -60 * t) };
  // blend the last 0.25 s exactly onto the loop's first frame, so the hand-over is seamless whatever the springs did
  for (const b of Object.keys(cruise)) {
    if (b === 'wheel' || b === 'key' || b.startsWith('lid_')) continue;
    for (const ch of Object.keys(cruise[b])) {
      const own = T[b] && T[b][ch] ? T[b][ch] : () => V();
      T[b] = T[b] || {};
      T[b][ch] = (t) => { const w = seg(t, L - 0.25, L); const a = own(t), c = at(b, ch, 0); return a.map((x, i) => x + (c[i] - x) * w); };
    }
  }
  anim('start_rolling', L, 'once', T);
}

// stop rolling: from the cruise pose, the wheel brakes to a stop, he rocks back, head and aerial whip forward,
// arms swing forward, and it all wobbles to rest
{
  const L = 1.6;
  const spin = (t) => ROLL.wheel * (1 - seg(t, 0, 0.5));
  const dt = 1 / 600, n = Math.ceil(L / dt) + 2, ang = new Float64Array(n);
  for (let k = 1; k < n; k++) ang[k] = ang[k - 1] + spin(k * dt) * dt;
  const T = keyedTracks({
    body: { rotation: [[0, V(ROLL.lean, 0, 0)], [0.05, V(12, 0, 0), 4, 4.5], [0.7, Z, 3, 5]], position: [[0, Z], [0.42, V(0, -0.4, 0)], [0.6, Z]] },
    head: { rotation: [[0, V(ROLL.head, 0, 0)], [0.08, V(-10, 0, 0), 5, 4], [0.75, Z, 3, 5]] },
    aerial: { rotation: [[0, V(ROLL.aerial, 0, 0)], [0.1, V(-35, 0, 0), 4, 2.5], [0.9, Z, 3, 3]] },
    arm_left: { rotation: [[0, V(ROLL.arm, 0, 0)], [0.08, V(25, 0, 0), 5, 4], [0.8, Z, 3, 5]] },
    arm_right: { rotation: [[0, V(ROLL.arm, 0, 0)], [0.08, V(25, 0, 0), 5, 4], [0.8, Z, 3, 5]] },
    forearm_left: { rotation: [[0, V(ROLL.fore, 0, 0)], [0.1, V(25, 0, 0)], [0.8, Z, 3, 5]] },
    forearm_right: { rotation: [[0, V(ROLL.fore + 8 * Math.sin(0.5), 0, 0)], [0.1, V(25, 0, 0)], [0.8, Z, 3, 5]] },
    lid_left: { position: [[0, Z], [0.05, V(0, 0.6, 0), 8, 6], [0.5, Z]] },
    lid_right: { position: [[0, Z], [0.05, V(0, 0.6, 0), 8, 6], [0.5, Z]] },
  }, { noIdle: true });
  T.wheel = { rotation: (t) => V(-(START_WHEEL + ang[Math.min(n - 1, Math.round(t / dt))]), 0, 0) };
  T.key = { rotation: (t) => V(0, 0, -(START_KEY + 60 * t)) };
  anim('stop_rolling', L, 'once', T);
}

// self-collision: points sampled over his arms, tested against his own head and body boxes (in each box's own
// moving frame). Used to report clipping per clip (ARMCLIP=1) and as a penalty when solving arm poses.
function m4inv(m) {
  const a = m, inv = new Array(16);
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
function chainMatrix(T, chain, t) {
  let M = M4.I();
  for (const b of chain) {
    const g = groupByName[b], tr = T[b] || {};
    const pos = tr.position ? tr.position(t) : V(), rot = addV(g.rot, tr.rotation ? tr.rotation(t) : V()), sc = tr.scale ? tr.scale(t) : V(1, 1, 1);
    M = M4.mul(M, M4.mul(M4.T(addV(g.origin, pos)), M4.mul(M4.Rz(rot[2]), M4.mul(M4.Ry(rot[1]), M4.mul(M4.Rx(rot[0]), M4.mul(M4.S(sc), M4.T(g.origin.map((x) => -x))))))));
  }
  return M;
}
const HEAD = ['trade_bot', 'body', 'neck', 'head'];
const BODY_BOXES = [
  { name: 'pod_right', chain: [...HEAD, 'eye_right'], box: [[1, 17, -4], [7, 22, 2]] },
  { name: 'pod_left', chain: [...HEAD, 'eye_left'], box: [[-7, 17, -4], [-1, 22, 2]] },
  { name: 'lid_right', chain: [...HEAD, 'eye_right', 'lid_right'], box: [[1, 20, -5], [7, 23, -4]] },
  { name: 'lid_left', chain: [...HEAD, 'eye_left', 'lid_left'], box: [[-7, 20, -5], [-1, 23, -4]] },
  { name: 'bridge', chain: HEAD, box: [[-1, 18.5, -1.5], [1, 20.5, 0.5]] },
  { name: 'chassis', chain: ['trade_bot', 'body'], box: [[-4, 7, -4], [4, 16, 4]] },
  { name: 'panel_right', chain: ['trade_bot', 'body'], box: [[4, 8, -3], [5, 15, 3]] },
  { name: 'panel_left', chain: ['trade_bot', 'body'], box: [[-5, 8, -3], [-4, 15, 3]] },
  { name: 'drawer', chain: ['trade_bot', 'body', 'drawer'], box: [[-3, 8, -5], [3, 11, 1]] },
];
// arm sample points (rest coordinates) with the chain each belongs to; the shoulder end of the upper arm is left
// out, since it sits against the side panel by design
function armPoints(side) {
  const s = side === 'right' ? 1 : -1, xs = [5 * s, 6 * s, 7 * s], pts = [];
  const base = ['trade_bot', 'body', 'arm_' + side];
  for (const x of xs) for (const z of [-1, 0, 1]) {
    for (const y of [10, 11.5, 13]) pts.push([base, [x, y, z]]);
    for (const y of [6, 8, 10]) pts.push([[...base, 'forearm_' + side], [x, y, z]]);
    for (const y of [3, 4.5, 5.5]) pts.push([[...base, 'forearm_' + side, 'claw_' + side], [x, y, z]]);
  }
  return pts;
}
const ARM_POINTS = [...armPoints('left'), ...armPoints('right')];
function selfClipAt(T, t, points = ARM_POINTS) {
  let worst = [0, ''];
  const inv = BODY_BOXES.map((bx) => ({ ...bx, inv: m4inv(chainMatrix(T, bx.chain, t)) }));
  for (const [chain, pt] of points) {
    const p = M4.apply(chainMatrix(T, chain, t), pt);
    for (const bx of inv) {
      const q = M4.apply(bx.inv, p);
      const d = Math.min(q[0] - bx.box[0][0], bx.box[1][0] - q[0], q[1] - bx.box[0][1], bx.box[1][1] - q[1], q[2] - bx.box[0][2], bx.box[1][2] - q[2]);
      if (d > worst[0]) worst = [d, chain[chain.length - 1] + ' in ' + bx.name];
    }
  }
  return worst;
}
const ARMCLIP = process.env.ARMCLIP === '1', ARMCLIP_ONLY = process.env.ARMCLIP_ONLY || '';
function armClipReport(name, T, L) {
  if (!ARMCLIP || !name.startsWith(ARMCLIP_ONLY)) return;
  let worst = [0, '', 0];
  for (let t = 0; t <= L + 1e-9; t += 1 / 30) { const w = selfClipAt(T, t); if (w[0] > worst[0]) worst = [w[0], w[1], t]; }
  console.error(('armclip ' + name).padEnd(34) + (worst[0] > 0.05 ? worst[0].toFixed(2) + ' px  ' + worst[1] + '  at ' + worst[2].toFixed(2) + 's' : 'clear'));
}

// arm IK with self-collision: claw tip (or held item) on a target, with the arm kept out of his head and body.
// Tbase supplies the rest of the pose (body lean, crouch, neck, head) at time t.
function solveArmClear(side, target, Tbase, t, o = {}) {
  const pt = [side === 'right' ? 6 : -6, 3, 0];
  const chain = ['trade_bot', 'body', 'arm_' + side, 'forearm_' + side, 'claw_' + side];
  const pts = ARM_POINTS.filter(([c]) => c[2] === 'arm_' + side);
  const rz = o.rz || 0;
  let best = null;
  const tryPose = (a, y, f) => {
    const T = { ...Tbase, ['arm_' + side]: { rotation: () => V(a, y, rz) }, ['forearm_' + side]: { rotation: () => V(f, 0, 0) } };
    const p = M4.apply(chainMatrix(T, chain, t), pt);
    const d = Math.hypot(p[0] - target[0], p[1] - target[1], p[2] - target[2]);
    if (best && d > best[0] + 0.5) return;
    const clip = selfClipAt(T, t, pts)[0];
    const cost = d + (o.clipWeight || 4) * clip + 0.002 * Math.abs(a - (o.preferA === undefined ? 90 : o.preferA));
    if (!best || cost < best[0]) best = [cost, a, y, f, p, clip];
  };
  for (let a = -20; a <= 175; a += 4) for (let y = -60; y <= 60; y += 5) for (let f = -140; f <= 140; f += 5) tryPose(a, y, f);
  const [, a0, y0, f0] = best;
  for (let a = a0 - 4; a <= a0 + 4; a += 1) for (let y = y0 - 5; y <= y0 + 5; y += 1) for (let f = f0 - 5; f <= f0 + 5; f += 1) tryPose(a, y, f);
  fkLog('solveClear ' + side + ' -> [' + target + '] arm ' + best[1] + ' yaw ' + best[2] + ' fore ' + best[3] + ' clip ' + best[5].toFixed(2) + ' at', best[4]);
  return [best[1], best[2], best[3]];
}

// 11. afraid of a hostile mob: spot → flee (loop) → hide → cower (loop) → recover. In place (the game finds the
// corner and moves him). Each clip ends exactly on the next one's first frame; wheel and key carry on between clips.
function blendEnd(T, L, target) {
  // ease every channel of T onto the target's first frame over the last 0.25 s (wheel and key are continuous already)
  for (const b of Object.keys(target)) {
    if (b === 'wheel' || b === 'key') continue;
    for (const ch of Object.keys(target[b])) {
      T[b] = T[b] || {};
      const own = T[b][ch] || (() => (ch === 'scale' ? V(1, 1, 1) : V()));
      const c = target[b][ch](0);
      T[b][ch] = (t) => { const w = seg(t, L - 0.25, L), a = own(t); return a.map((x, i) => x + (c[i] - x) * w); };
    }
  }
  return T;
}
const integrateSpin = (L, spin) => { const dt = 1 / 600, n = Math.ceil(L / dt) + 2, a = new Float64Array(n); for (let k = 1; k < n; k++) a[k] = a[k - 1] + spin(k * dt) * dt; return (t) => a[Math.min(n - 1, Math.max(0, Math.round(t / dt)))]; };
const FLEE_WHEEL = 240; // deg/s, double cruise: 360° per 1.5 s loop
const C3 = (x, y, z) => () => V(x, y, z);

// hiding pose: claws up in front of his eye pods (solved with the body crouched 1.2 px and leaning back 4°)
// (poses solved clear of his own head: the old ones reached up past the eye pods and clipped through them)
// hiding pose: claws come up from below and in front over his eye pods; body crouched 1.2 px, leaning back 4°,
// neck in 1.5 px, head tucked 15°
const TB_COWER = { body: { rotation: C3(4, 0, 0), position: C3(0, -1.2, 0) }, neck: { position: C3(0, -1.5, 0) }, head: { rotation: C3(-15, 0, 0) } };
const eyeFront = (side) => fk(TB_COWER, [...HEAD, 'eye_' + side], 0, [side === 'right' ? 4 : -4, 19.5, -7.5]);
const [cLA, cLY, cLF] = solveArmClear('left', eyeFront('left'), TB_COWER, 0, { preferA: 70, rz: -6, clipWeight: 12 });
const [cRA, cRY, cRF] = solveArmClear('right', eyeFront('right'), TB_COWER, 0, { preferA: 70, rz: 6, clipWeight: 12 });
const COVER = { arm_left: V(cLA, cLY, -6), forearm_left: V(cLF, 0, 0), arm_right: V(cRA, cRY, 6), forearm_right: V(cRF, 0, 0) };
// fleeing pose: arms flung up and outward in a panicked V beside his head
const TB_FLEE = { body: { rotation: C3(-14, 0, 0) }, neck: { position: C3(0, 0.5, 0) }, head: { rotation: C3(4, 0, 0) } };
const [fLA, fLY, fLF] = solveArmClear('left', [-11, 22, -1], TB_FLEE, 0, { preferA: 120, rz: -40 });
const [fRA, fRY, fRF] = solveArmClear('right', [11, 22, -1], TB_FLEE, 0, { preferA: 120, rz: 40 });
const FLEE_ARM = { arm_left: V(fLA, fLY, -40), forearm_left: V(fLF, 0, 0), arm_right: V(fRA, fRY, 40), forearm_right: V(fRF, 0, 0) };

// ---- stage 1: spot. Startle, double-take, hop back, crouch, spin the wheel up
const SPOT_L = 1.1;
const spotSpin = integrateSpin(SPOT_L, (t) => FLEE_WHEEL * seg(t, 0.7, 1.05));
const SPOT_WHEEL = spotSpin(SPOT_L), SPOT_KEY = 60 * SPOT_L;
// ---- the flee loop's pose at t = 0 (spot ends here, hide starts here)
const FLEE0 = {
  body: { rotation: C3(-14, 0, 0), position: C3(0, 0, 0) }, head: { rotation: C3(4, 0, 0) }, neck: { position: C3(0, 0.5, 0) },
  lid_left: { position: C3(0, 1, 0) }, lid_right: { position: C3(0, 1, 0) }, iris_left: { scale: C3(0.65, 0.65, 1) }, iris_right: { scale: C3(0.65, 0.65, 1) },
  aerial: { rotation: C3(35, 0, 0) }, arm_left: { rotation: () => FLEE_ARM.arm_left }, arm_right: { rotation: () => FLEE_ARM.arm_right },
  forearm_left: { rotation: () => FLEE_ARM.forearm_left }, forearm_right: { rotation: () => FLEE_ARM.forearm_right }, eye_left: { rotation: C3(0, 0, 0) }, eye_right: { rotation: C3(0, 0, 0) },
};
{
  const T = keyedTracks({
    head: { rotation: [[0, Z], [0.05, V(0, 25, 0), 9, 6], [0.2, V(0, -4, 0), 9, 6], [0.3, V(8, 28, 0), 10, 6], [0.75, V(4, 0, 0)]] },
    neck: { position: [[0, Z], [0.3, V(0, 2, 0), 9, 6], [0.75, V(0, 0.5, 0)]] },
    lid_left: { position: [[0, Z], [0.3, V(0, 1, 0), 10, 6]] },
    lid_right: { position: [[0, Z], [0.3, V(0, 1, 0), 10, 6]] },
    iris_left: { scale: [[0, V(1, 1, 1)], [0.3, V(0.55, 0.55, 1), 10, 6], [0.8, V(0.65, 0.65, 1)]] },
    iris_right: { scale: [[0, V(1, 1, 1)], [0.3, V(0.55, 0.55, 1), 10, 6], [0.8, V(0.65, 0.65, 1)]] },
    aerial: { rotation: [[0, Z], [0.3, V(-20, 0, 0), 6, 3], [0.8, V(35, 0, 0)]] },
    body: { rotation: [[0, Z], [0.3, V(8, 0, 0), 8, 5], [0.7, V(4, 0, 0)], [0.85, V(-14, 0, 0), 5, 6]], position: [[0, Z], [0.3, V(0, 0.3, 0)], [0.7, V(0, -0.8, 0)], [0.9, Z]] },
    arm_left: { rotation: [[0, Z], [0.3, V(40, 0, -30), 9, 5], [0.8, FLEE_ARM.arm_left]] },
    arm_right: { rotation: [[0, Z], [0.3, V(40, 0, 30), 9, 5], [0.8, FLEE_ARM.arm_right]] },
    forearm_left: { rotation: [[0, Z], [0.3, V(40, 0, 0)], [0.8, FLEE_ARM.forearm_left]] },
    forearm_right: { rotation: [[0, Z], [0.3, V(40, 0, 0)], [0.8, FLEE_ARM.forearm_right]] },
  }, { noIdle: true, extra: { trade_bot: { position: (t) => V(0, 1.2 * hopArc(t, 0.28, 0.5), 1.5 * seg(t, 0.28, 0.5) - 1.5 * seg(t, 0.75, 1.05)) } } });
  T.wheel = { rotation: (t) => V(-spotSpin(t), 0, 0) };
  T.key = { rotation: (t) => V(0, 0, -60 * t) };
  anim('scared_spot', SPOT_L, 'once', blendEnd(T, SPOT_L, FLEE0));
}

// ---- stage 2: flee (1.5 s loop). Panicked rolling: flailing arms, aerial streaming, a glance back each loop
const flee = {
  wheel: { rotation: (t) => V(-(SPOT_WHEEL + FLEE_WHEEL * t), 0, 0) },
  key: { rotation: (t) => V(0, 0, -(SPOT_KEY + 120 * t)) },
  body: { rotation: (t) => V(-14 + 1.2 * Math.sin(W2 * 4 * t), 0, 2 * Math.sin((W2 * t) / 1.5)), position: (t) => V(0, 0.4 * Math.sin(W2 * 4 * t), 0) },
  head: { rotation: (t) => V(4 + 1.5 * lagSin(t, 4, 0.8), 45 * env(t, 0.65, 0.8, 0.95, 1.1), 0) },
  neck: { position: C3(0, 0.5, 0) },
  lid_left: { position: C3(0, 1, 0) }, lid_right: { position: C3(0, 1, 0) },
  iris_left: { scale: C3(0.65, 0.65, 1) }, iris_right: { scale: C3(0.65, 0.65, 1) },
  eye_left: { rotation: (t) => V(0, 0, 3 * lagSin(t, 4, 1)) }, eye_right: { rotation: (t) => V(0, 0, -3 * lagSin(t, 4, 1)) },
  aerial: { rotation: (t) => V(35 + 10 * lagSin(t, 4, 1.2), 0, 6 * lagSin(t, 2 / 1.5, 1)) },
  // raised arms: +Z on the left arm (−Z on the right) swings the claw outward, away from the eye pods
  arm_left: { rotation: (t) => addV(FLEE_ARM.arm_left, V(10 * Math.sin(W2 * 2 * t), 0, 8 * (1 - Math.cos(W2 * 4 * t)) / 2 + 12 * env(t, 0.6, 0.75, 0.95, 1.15))) },
  arm_right: { rotation: (t) => addV(FLEE_ARM.arm_right, V(-10 * Math.sin(W2 * 2 * t), 0, -8 * (1 - Math.cos(W2 * 4 * t)) / 2 - 12 * env(t, 0.6, 0.75, 0.95, 1.15))) },
  forearm_left: { rotation: (t) => addV(FLEE_ARM.forearm_left, V(12 * lagSin(t, 2, 0.6), 0, 0)) },
  forearm_right: { rotation: (t) => addV(FLEE_ARM.forearm_right, V(-12 * lagSin(t, 2, 0.6), 0, 0)) },
};
anim('scared_flee', 1.5, 'loop', flee);

// ---- the cower loop's pose at t = 0 (hide ends here, recover starts here)
const COWER0 = {
  body: { rotation: C3(4, 0, 0), position: C3(0, -1.2, 0) }, neck: { position: C3(0, -1.5, 0) }, head: { rotation: C3(-15, 0, 0) },
  lid_left: { position: C3(0, -1, 0) }, lid_right: { position: C3(0, -1, 0) }, iris_left: { scale: C3(0.7, 0.7, 1) }, iris_right: { scale: C3(0.7, 0.7, 1) },
  eye_left: { rotation: C3(0, 0, 6) }, eye_right: { rotation: C3(0, 0, -6) }, aerial: { rotation: C3(70, 0, 0) },
  arm_left: { rotation: () => COVER.arm_left }, arm_right: { rotation: () => COVER.arm_right }, forearm_left: { rotation: () => COVER.forearm_left }, forearm_right: { rotation: () => COVER.forearm_right },
};

// ---- stage 3: hide. Brake hard in the corner, then shrink: neck in, crouch, claws over eyes, aerial flat
const HIDE_L = 1.3;
const hideSpin = integrateSpin(HIDE_L, (t) => FLEE_WHEEL * (1 - seg(t, 0, 0.35)));
const HIDE_WHEEL = SPOT_WHEEL + hideSpin(HIDE_L), HIDE_KEY = SPOT_KEY + 90 * HIDE_L;
{
  const T = keyedTracks({
    body: { rotation: [[0, V(-14, 0, 0)], [0.05, V(10, 0, 0), 5, 4], [0.45, V(4, 0, 0), 4, 6]], position: [[0, Z], [0.4, V(0, -1.2, 0), 5, 7]] },
    neck: { position: [[0, V(0, 0.5, 0)], [0.35, V(0, -1.5, 0), 6, 7]] },
    head: { rotation: [[0, V(4, 0, 0)], [0.08, V(-12, 0, 0)], [0.4, V(-15, 0, 0)]] },
    arm_left: { rotation: [[0, FLEE_ARM.arm_left], [0.3, COVER.arm_left, 5, 11]] },
    arm_right: { rotation: [[0, FLEE_ARM.arm_right], [0.3, COVER.arm_right, 5, 11]] },
    forearm_left: { rotation: [[0, FLEE_ARM.forearm_left], [0.3, COVER.forearm_left, 5, 11]] },
    forearm_right: { rotation: [[0, FLEE_ARM.forearm_right], [0.3, COVER.forearm_right, 5, 11]] },
    lid_left: { position: [[0, V(0, 1, 0)], [0.35, V(0, -1, 0)]] },
    lid_right: { position: [[0, V(0, 1, 0)], [0.35, V(0, -1, 0)]] },
    iris_left: { scale: [[0, V(0.65, 0.65, 1)], [0.35, V(0.7, 0.7, 1)]] },
    iris_right: { scale: [[0, V(0.65, 0.65, 1)], [0.35, V(0.7, 0.7, 1)]] },
    eye_left: { rotation: [[0, Z], [0.35, V(0, 0, 6)]] },
    eye_right: { rotation: [[0, Z], [0.35, V(0, 0, -6)]] },
    aerial: { rotation: [[0, V(35, 0, 0)], [0.1, V(-20, 0, 0), 5, 3], [0.5, V(70, 0, 0), 3, 4]] },
  }, { noIdle: true });
  T.wheel = { rotation: (t) => V(-(SPOT_WHEEL + hideSpin(t)), 0, 0) };
  T.key = { rotation: (t) => V(0, 0, -(SPOT_KEY + 90 * t)) };
  anim('scared_hide', HIDE_L, 'once', blendEnd(T, HIDE_L, COWER0));
}

// ---- stage 4: cower (2 s loop). Trembling all over; halfway, one eye peeks out between the claws and hides again
{
  const peek = (t) => env(t, 0.8, 1.0, 1.4, 1.6);
  const cower = {
    wheel: { rotation: C3(-HIDE_WHEEL, 0, 0) },
    key: { rotation: (t) => V(0, 0, -(HIDE_KEY + 90 * t)) },
    body: { rotation: (t) => V(4 + 1.0 * Math.sin(W2 * 11 * t), 0, 1.6 * Math.sin(W2 * 9 * t)), position: (t) => V(0, -1.2 + 0.15 * Math.sin(W2 * 13 * t), 0) },
    neck: { position: C3(0, -1.5, 0) },
    head: { rotation: (t) => V(-15 + 1.0 * lagSin(t, 11, 0.7) + 6 * peek(t), 12 * peek(t), 1.2 * lagSin(t, 9, 0.5)) },
    lid_left: { position: (t) => V(0, -1 + 1.3 * peek(t), 0) }, lid_right: { position: C3(0, -1, 0) },
    iris_left: { scale: (t) => { const s = 0.7 + 0.3 * peek(t); return V(s, s, 1); } }, iris_right: { scale: C3(0.7, 0.7, 1) },
    eye_left: { rotation: (t) => V(0, 0, 6 - 6 * peek(t) + 1.5 * lagSin(t, 11, 1)) }, eye_right: { rotation: (t) => V(0, 0, -6 - 1.5 * lagSin(t, 11, 1)) },
    aerial: { rotation: (t) => V(70 + 5 * lagSin(t, 13, 1), 0, 4 * lagSin(t, 9, 1.3)) },
    arm_left: { rotation: (t) => addV(COVER.arm_left, V(2 * Math.sin(W2 * 13 * t) - 18 * peek(t), 0, 0)) },
    arm_right: { rotation: (t) => addV(COVER.arm_right, V(2 * (Math.sin(W2 * 13 * t + 1) - Math.sin(1)), 0, 0)) },
    forearm_left: { rotation: () => COVER.forearm_left }, forearm_right: { rotation: () => COVER.forearm_right },
  };
  anim('scared_cower', 2, 'loop', cower);
}

// ---- stage 5: recover. Cautious peek, look left and right, head up, a relieved sigh, shake it off, back to normal
{
  const L = 2.6;
  const shake = (t) => env(t, 1.7, 1.75, 2.05, 2.15) * Math.sin(W2 * 7 * (t - 1.7));
  const T = keyedTracks({
    lid_left: { position: [[0, V(0, -1, 0)], [0.2, V(0, 0.4, 0), 4, 6], [1.0, Z]] },
    lid_right: { position: [[0, V(0, -1, 0)], [0.7, V(0, 0.3, 0)], [1.0, Z]] },
    head: { rotation: [[0, V(-15, 0, 0)], [0.45, V(-10, 20, 0)], [0.75, V(-8, -20, 0)], [1.05, V(-4, 0, 0), 3, 5], [1.3, V(4, 0, 0)], [1.6, Z]] },
    neck: { position: [[0, V(0, -1.5, 0)], [0.9, V(0, 0.5, 0), 3, 5], [1.6, Z]] },
    arm_left: { rotation: [[0, COVER.arm_left], [0.2, V(20, 0, -10), 4, 6], [1.4, Z]] },
    arm_right: { rotation: [[0, COVER.arm_right], [0.2, V(20, 0, 10), 4, 6], [1.4, Z]] },
    forearm_left: { rotation: [[0, COVER.forearm_left], [0.2, V(10, 0, 0), 4, 6], [1.4, Z]] },
    forearm_right: { rotation: [[0, COVER.forearm_right], [0.2, V(10, 0, 0), 4, 6], [1.4, Z]] },
    body: { rotation: [[0, V(4, 0, 0)], [0.9, Z]], position: [[0, V(0, -1.2, 0)], [0.9, Z, 3, 5], [1.2, V(0, 0.5, 0)], [1.5, V(0, -0.3, 0)], [1.8, Z]] },
    iris_left: { scale: [[0, V(0.7, 0.7, 1)], [0.3, V(1, 1, 1)], [1.25, V(1.15, 1.15, 1)], [2.0, V(1, 1, 1)]] },
    iris_right: { scale: [[0, V(0.7, 0.7, 1)], [0.7, V(1, 1, 1)], [1.25, V(1.15, 1.15, 1)], [2.0, V(1, 1, 1)]] },
    eye_left: { rotation: [[0, V(0, 0, 6)], [1.25, V(0, 0, 8)], [2.0, Z]] },
    eye_right: { rotation: [[0, V(0, 0, -6)], [1.25, V(0, 0, -8)], [2.0, Z]] },
    aerial: { rotation: [[0, V(70, 0, 0)], [0.95, V(10, 0, 0), 3, 4], [1.8, Z]] },
  }, { noIdle: true, extra: {
    body: { rotation: (t) => V(0, 0, 6 * shake(t)) },
    head: { rotation: (t) => V(0, 0, -4 * shake(t)) },
    aerial: { rotation: (t) => V(0, 0, 15 * shake(t)) },
  } });
  T.wheel = { rotation: C3(-HIDE_WHEEL, 0, 0) };
  T.key = { rotation: (t) => V(0, 0, -(HIDE_KEY + 60 * t)) };
  // land exactly on the neutral pose, so the game hands straight back to idle
  const NEUTRAL = Object.fromEntries(Object.entries(T).filter(([b]) => b !== 'wheel' && b !== 'key').map(([b, chs]) => [b, Object.fromEntries(Object.keys(chs).map((ch) => [ch, ch === 'scale' ? C3(1, 1, 1) : C3(0, 0, 0)]))]));
  anim('scared_recover', L, 'once', blendEnd(T, L, NEUTRAL));
}

for (const [n, T, L] of ALL_CLIPS) armClipReport(n, T, L);

const bb = {
  meta: { format_version: '5.0', model_format: 'modded_entity', box_uv: true },
  name: 'trade_bot', model_identifier: 'trade_bot', modded_entity_entity_class: '', modded_entity_version: '1.17', modded_entity_flip_y: true,
  visible_box: [1, 1.5, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
  resolution: { width: TEX, height: TEX },
  elements, groups, outliner,
  textures: [{ path: '', name: 'trade_bot.png', folder: '', namespace: '', id: '0', group: '', width: TEX, height: TEX, uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + pngBuf.toString('base64') }],
  animations,
};
fs.writeFileSync(OUT, JSON.stringify(bb));
console.error('wrote', OUT, elements.length, 'cubes', groups.length, 'groups', animations.length, 'animations');
