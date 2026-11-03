// The Clockwork Jetpack -> design/models/clockwork-jetpack.bbmodel (Generic Model preview with clips), to the brief
// design/briefs/clockwork-jetpack.md. One geometry in the jetpack's own block space (plate against the wearer's back at
// z 4, facing north; drum, key and wings out behind it), used for the worn model, the placed block and the Netherite
// version. Every moving part is its own bone; blades are authored pointing straight out and turned by their bone.
// Scenes, side by side: worn on a Steve with a chestplate (centre), worn with a backpack, built into it (pack mode,
// left), the placed block on a shaft (right), the Netherite pair (far right), and the item and slot icons (far left).
// Steve, the chestplate, the backpack and the shaft are stand-ins for the preview: never exported.
const fs = require('fs');
const path = require('path');
const { C, textureSet, hash, edge, png, preview, SHOW, HIDE, smooth } = require('./process-box.lib.js');
const { check } = require('./zfight-check.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'clockwork-jetpack.bbmodel');
const pick = (arr, x, y, s) => arr[Math.floor(hash(x, y, s) * arr.length * 0.999)];

// ---------- palettes: the Aeronautics set (aviator steel, mid brass, walnut) and its Netherite twin ----------
const PAL = {
  plain: {
    field: '#3c424a', field2: '#40464e', hi: '#4a5059', lo: '#2f343b', seam: '#2a2f35',
    canvas: ['#6a604e', '#6f6552', '#756a55'], canvasDark: '#5a5242', wood: ['#4e3626', '#634532', '#6f4d36'], ribDark: '#3b2a1f',
    metal: ['#7e6832', '#8f7638', '#a3873f'], metalDark: '#6e5a2a', metalDeep: '#5e4c24', // the Aeronautics brass
    ductField: ['#6e5a2a', '#7e6832'], ductRivet: '#8f7638',
  },
  netherite: {
    field: '#3b3536', field2: '#3f383a', hi: '#4a4344', lo: '#2e2a2b', seam: '#272324',
    canvas: ['#46272b', '#4a2a2e', '#502e33'], canvasDark: '#381e22', wood: ['#2e2523', '#3a2e2a', '#443631'], ribDark: '#231c1a',
    metal: ['#433d40', '#4d4649', '#5a5255'], metalDark: '#2a2526', metalDeep: '#1f1b1c', // netherite in place of the brass, darker rims (user, 2026-11-03)
    ductField: ['#2a2526', '#3a3436'], ductRivet: '#4d4649',
  },
};
const LEATHER = ['#3b2a1f', '#46321f'], CARBON = ['#26272a', '#323337'];
const STEEL_LIGHT = ['#4a5059', '#5a616b'];
const winR = (x, y) => Math.hypot(x - 7.5, y - 7.5);

function textures(variant, file) {
  const P = PAL[variant];
  // the trim metal: brass on the plain jetpack, netherite on the Netherite one
  const BRASS = P.metal, BRASS_DARK = P.metalDark, DEEP = P.metalDeep;
  const brass = (x, y, s) => (hash(x, y, s) < 0.65 ? BRASS[0] : BRASS[1]);
  // the key, the hero, is the brightest metal on the back
  const keyMetal = (x, y, s) => { const r = hash(x, y, s); return r < 0.15 ? BRASS[0] : r < 0.7 ? BRASS[1] : BRASS[2]; };
  // a 4-texel grid for faces under 2 px, so one step is about half a pixel instead of speckle
  const cell = (x, y) => [x >> 2, y >> 2], corner = (cx, cy) => (cx === 0 || cx === 3) && (cy === 0 || cy === 3), rim = (cx, cy) => cx === 0 || cx === 3 || cy === 0 || cy === 3;
  const T = textureSet(); T.file = file;
  const field = (x, y, s) => (hash(x, y, s) > 0.86 ? P.field2 : P.field);
  // panels: a near-flat field with a crisp bevel (light top-left, dark bottom-right)
  T.paint('steel', (x, y) => (x === 0 || y === 0 ? P.hi : x === 15 || y === 15 ? P.lo : field(x, y, 1)));
  T.paint('steel_plain', (x, y) => field(x, y, 2));
  // the back pad against the wearer: leather in quilted rows, brass rivets at the corners
  T.paint('pad', (x, y) => {
    if ((x === 1 || x === 14) && (y === 1 || y === 14)) return BRASS[2];
    if (edge(x, y) === 0) return P.lo;
    return y % 4 === 0 ? '#2e2118' : pick(LEATHER, x, y, 3);
  });
  // the drum's sides: u runs along the drum's depth (z 7.5..10 -> u 0..10); two brass bands round it
  T.paint('drum_side', (x, y) => {
    if ((x >= 2 && x < 4) || (x >= 6 && x < 8)) return x === 2 || x === 6 ? BRASS_DARK : brass(x, y, 4);
    if (x === 0) return P.lo;
    if (x === 9) return P.hi;
    return field(x, y, 5);
  });
  // the drum's face: dark steel round a brass-ringed porthole (the coil frames lie over the window)
  T.paint('drum_face', (x, y) => {
    const r = winR(x, y);
    if (r < 6.2) return '#16181b';
    if (r < 7.2) return r < 6.7 ? BRASS_DARK : brass(x, y, 6);
    return field(x, y, 7);
  });
  // the Mainspring through the porthole (the Spring Engine's frames): 0 run down, a loose ring at the wall .. 3 full,
  // packed tight round the arbor
  const coil = (k) => (x, y) => {
    const r = winR(x, y);
    if (r > 6.2) return null;
    if (r < 2.7) return r < 1.6 ? '#14161a' : brass(x, y, 8);
    const rIn = [5.3, 4.4, 3.5, 2.7][k], rOut = [6.2, 6.0, 5.6, 5.0][k];
    if (r >= rIn && r <= rOut) return Math.round(r * 1.6) % 2 ? (y < 8 ? '#8a949c' : '#6d7780') : '#4a525a';
    return '#16181b';
  };
  [0, 1, 2, 3].forEach((k) => T.paint('coil_' + k, coil(k)));
  T.paint('glass', (x, y) => ((x === 5 && y === 4) || (x === 4 && y === 5) || (x === 6 && y === 3) ? '#a9bac2' : null));
  T.paint('brass', (x, y) => brass(x, y, 9));
  T.paint('key_metal', (x, y) => keyMetal(x, y, 27));
  T.paint('key_hub', (x, y) => { const [cx, cy] = cell(x, y); return rim(cx, cy) ? (cx === 0 || cy === 0 ? BRASS[2] : BRASS[1]) : '#1c1e21'; });
  // a toothed gear face on the 4-texel grid: eight teeth round a brass hub (a dark arbor here read as a pair of eyes)
  T.paint('gear', (x, y) => { const [cx, cy] = cell(x, y); if (corner(cx, cy)) return null; if (rim(cx, cy)) return (cx + cy) % 2 ? BRASS[1] : BRASS_DARK; return cx === 1 && cy === 1 ? BRASS[1] : BRASS[0]; });
  // the precision-mechanism cap on the 4-texel grid: a ring of cog teeth, a dark centre with one bright rivet
  T.paint('hub_cap', (x, y) => { const [cx, cy] = cell(x, y); if (corner(cx, cy)) return null; if (rim(cx, cy)) return (cx + cy) % 2 ? BRASS[2] : BRASS_DARK; return cx === 1 && cy === 1 ? BRASS[2] : '#1c1e21'; });
  // oiled canvas: a faint weave, two close shades, a darker selvedge at the edges
  T.paint('canvas', (x, y) => (y === 0 || y === 15 ? P.canvasDark : y % 2 && hash(x, y, 13) < 0.35 ? P.canvas[0] : hash(x, y, 14) < 0.15 ? P.canvas[2] : P.canvas[1]));
  T.paint('rib', (x, y) => (x % 4 === 1 ? (y % 2 ? '#7d725c' : '#5f5747') : y === 15 ? P.ribDark : pick(P.wood, x, y, 15)));
  T.paint('canvas_plain', (x, y) => (y === 1 && x % 3 === 0 ? P.canvasDark : y % 2 && hash(x, y, 13) < 0.35 ? P.canvas[0] : hash(x, y, 14) < 0.15 ? P.canvas[2] : P.canvas[1]));
  T.paint('membrane', (x, y) => {
    if (x > 15 - 3.5 * Math.sin((Math.PI * (y + 0.5)) / 16)) return null; // the scallop where the canvas sags between rib tips
    if (y === 1 && x % 3 === 0) return P.canvasDark; // stitching along the rib
    return y % 2 && hash(x, y, 13) < 0.35 ? P.canvas[0] : hash(x, y, 14) < 0.15 ? P.canvas[2] : P.canvas[1];
  });
  // the fan ducts: a step darker than the trim, shaded round, a riveted band at the top and a riveted seam (3x3-texel
  // rivets, about half a pixel), a dark lip at the mouth
  const DF = P.ductField, DR = P.ductRivet;
  const rivetRow = (x) => x % 6 >= 1 && x % 6 <= 3;
  T.paint('duct', (x, y) => {
    if (y <= 2) return rivetRow(x) ? DR : DEEP;
    if (y >= 13) return DEEP;
    if (y >= 7 && y <= 9) return rivetRow(x) ? DR : DEEP;
    if (x <= 1 || x >= 14) return DEEP;
    if (x <= 4 || x >= 11) return DF[0];
    return hash(x, y, 25) < 0.3 ? DF[0] : DF[1];
  });
  T.paint('collar', (x, y) => (x % 5 === 2 ? DR : hash(x, y, 26) < 0.5 ? DEEP : DF[0]));
  // the station: a walnut post in vertical planks; the winder's square socket on the 4-texel grid
  T.paint('walnut', (x, y) => (x % 4 === 0 ? P.ribDark : pick(P.wood, x, y, 28)));
  T.paint('socket_face', (x, y) => { const [cx, cy] = cell(x, y); return rim(cx, cy) ? (cx === 0 || cy === 0 ? BRASS[1] : BRASS_DARK) : '#14161a'; });
  T.paint('throat', (x, y) => (winR(x, y) > 6 ? '#1c1e21' : '#121315'));
  T.paint('rotor', (x, y) => pick(STEEL_LIGHT, x, y, 16));
  T.paint('rotor_hub', (x, y) => pick(CARBON, x, y, 17));
  T.paint('axle', (x, y) => (hash(x, y, 18) < 0.5 ? P.lo : P.seam));
  T.paint('strap', (x, y) => (x === 0 || x === 15 ? '#2e2118' : pick(LEATHER, x, y, 19)));
  T.paint('buckle', (x, y) => (edge(x, y) <= 2 ? brass(x, y, 20) : '#1c1e21'));
  // the winding stand: a steel top with a vent under each duct, a brass socket ring round the shaft hole underneath
  T.paint('base_top', (x, y) => (x === 0 || y === 0 ? P.hi : x === 15 || y === 15 ? P.lo : field(x, y, 21)));
  T.paint('socket', (x, y) => { const r = winR(x, y); if (r < 2.9) return '#14161a'; if (r < 4.6) return brass(x, y, 22); return field(x, y, 23); });
  return T;
}
const TP = textures('plain', 'clockwork_jetpack_');
const TN = textures('netherite', 'netherite_clockwork_jetpack_');

// ---------- stand-ins (preview only) ----------
const SI = textureSet(); SI.file = 'standin_';
const flat = (a, b) => (x, y) => (hash(x, y, 40) < 0.3 ? b : a);
SI.paint('skin', flat('#9c7a5e', '#94735a'));
SI.paint('hair', flat('#3a2a1e', '#33251a'));
SI.paint('shirt', flat('#3f6670', '#3a5f68'));
SI.paint('pants', flat('#3b3f66', '#373a5f'));
SI.paint('iron', (x, y) => (edge(x, y) === 0 ? '#5f6366' : flat('#7c8085', '#767a7f')(x, y)));
SI.paint('pack', (x, y) => (edge(x, y) === 0 ? '#4a3626' : flat('#6b5038', '#664c35')(x, y)));
SI.paint('pack_trim', flat('#4a3626', '#45321f'));

// ---------- the jetpack (block space) ----------
// Each builder takes the texture prefix, so the plain and Netherite versions share the geometry.
const DRUM = { x: 8, y: 12 }, HUB = { L: [4.5, 15], R: [11.5, 15] };
// ten 0.3 px layers between the plate and the drum: each rib's inner half, then its outer half; rib 1 (the leading
// spar) nearest the drum
const LAYER = (k, outer) => 5.1 + (2 * (5 - k) + (outer ? 0 : 1)) * 0.3, LT = 0.3;
const FOLDED = 90, OPEN = [5, -4, -13, -22, -31]; // degrees from straight out: folded points straight down, open sweeps back
const DIHEDRAL = 20; // open, each wing tilts this far out of the back's plane, so in flight the wings rise in a shallow V
// jointed ribs (Leonardo's articulated wing, a bat's fingers): inner half to a brass knuckle, then the outer half, which
// folds back up beside the inner one to stow; 48 px tip to tip open
const INNER = [10.5, 10, 10, 10, 10], OUTER = [10, 9.5, 8.5, 7, 5.5];
const DELTA = OPEN.map((a) => a + FOLDED); // how far each blade turns from folded to open
// The outer canvas above rib k fills the gap up to rib k-1, which widens towards the tip: each panel is tapered in its
// texture to that gap (row 0 against its own rib), and scalloped at the tip where it sags between the rib ends.
const OUT_W = 3.1;
for (const [T, v] of [[TP, 'plain'], [TN, 'netherite']]) for (let k = 2; k <= 5; k++) {
  const P = PAL[v], a = INNER[k - 1], b = OUTER[k - 1], h = 0.5, r0 = a - 0.45, len = b + 0.45 - 1;
  const gap = Math.sin(((OPEN[k - 2] - OPEN[k - 1]) * Math.PI) / 180);
  T.paint('membrane_out_' + k, (x, y) => {
    const r = r0 + ((x + 0.5) / 16) * len, above = ((y + 0.5) / 16) * OUT_W;
    if (above > r * gap - h) return null;
    if (x > 15 - 3.5 * Math.sin((Math.PI * (y + 0.5)) / 16)) return null;
    if (y === 1 && x % 3 === 0) return P.canvasDark;
    return y % 2 && hash(x, y, 13) < 0.35 ? P.canvas[0] : hash(x, y, 14) < 0.15 ? P.canvas[2] : P.canvas[1];
  });
}

// the drum's stepped round, five boxes that never overlap; u along z on the sides; the face carries the porthole
const fuv = (x0, y0, x1, y1) => [(x0 - 4) * 2, (16 - y1) * 2, (x1 - 4) * 2, (16 - y0) * 2];
const sideUv = { east: [0, 0, 10, 16], west: [0, 0, 10, 16], up: [0, 0, 10, 16], down: [0, 0, 10, 16] };
const sideRot = { up: 90, down: 90 };
function drum(K) {
  const z0 = 8.5, z1 = 11;
  const box = (name, x0, y0, x1, y1, hide) => C(name, [x0, y0, z0], [x1, y1, z1],
    { north: K + 'steel_plain', south: K + 'drum_face', ...Object.fromEntries(hide.map((d) => [d, null])), all: K + 'drum_side' },
    { uv: { south: fuv(x0, y0, x1, y1), ...sideUv }, frot: sideRot });
  return [
    box('drum_mid', 4, 10, 12, 14, []),
    box('drum_up1', 5, 14, 11, 15, ['down']), box('drum_up2', 6, 15, 10, 16, ['down']),
    box('drum_dn1', 5, 9, 11, 10, ['up']), box('drum_dn2', 6, 8, 10, 9, ['up']),
  ];
}
// the plate and the boss the drum stands on, clear of the stowed blades; the drum is its own part, as it tucks in
// against the plate (TUCK px) once the wings are out of the way, and back out before they fold
const TUCK = 3.5;
function frame(K) {
  return [
    C('plate', [4, 4, 4], [12, 16, 5], { north: K + 'pad', all: K + 'steel' }),
    C('boss', [7.7, 10.5, 5], [8.3, 13.5, 8.5], { north: null, south: null, all: K + 'steel_plain' }),
  ];
}
// fan ducts under the drum's lower corners, a brass collar at each mouth, a dark throat inside; their own part, as on a
// backpack they drop below it
function ducts(K) {
  return [
    ...['L', 'R'].flatMap((s) => {
      const x0 = s === 'L' ? 4 : 9;
      return [
        C('duct_' + s, [x0, 4, 8.5], [x0 + 3, 8, 11.5], { down: null, north: null, up: K + 'steel_plain', all: K + 'duct' }, { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
        C('collar_' + s, [x0 - 0.25, 4, 8.25], [x0 + 3.25, 4.75, 11.75], { down: null, all: K + 'collar' }),
        C('throat_' + s, [x0 + 0.05, 4.9, 8.55], [x0 + 2.95, 4.9, 11.45], { down: K + 'throat', all: null }, { uv: { down: [0, 0, 16, 16] } }),
      ];
    }),
  ];
}
const keyParts = (K) => [
  C('key_stem', [7.25, 11.25, 11], [8.75, 12.75, 13], { north: null, all: K + 'key_metal' }),
  C('key_hub', [7, 11, 13], [9, 13, 14], { south: K + 'key_hub', all: K + 'key_metal' }, { uv: { south: [0, 0, 16, 16] } }),
  // two loops, either side of the hub (the Juggernaut's key in miniature)
  ...[1, -1].flatMap((s) => {
    const x = (a) => (s > 0 ? a : 16 - a), lo = (a, b) => [Math.min(x(a), x(b)), Math.max(x(a), x(b))];
    const [a0, a1] = lo(9, 11.5), [b0, b1] = lo(10.75, 11.5), n = s > 0 ? 'r' : 'l';
    return [
      C('loop_' + n + '_top', [a0, 12.75, 13.25], [a1, 13.5, 13.75], { all: K + 'key_metal' }),
      C('loop_' + n + '_bottom', [a0, 10.5, 13.25], [a1, 11.25, 13.75], { all: K + 'key_metal' }),
      C('loop_' + n + '_outer', [b0, 11.25, 13.25], [b1, 12.75, 13.75], { all: K + 'key_metal' }),
    ];
  }),
];
const coilPlane = (K, k) => [C('coil_' + k, [4, 8, 11.25 + 0.05 * k], [12, 16, 11.25 + 0.05 * k], { south: K + 'coil_' + k, all: null }, { uv: { south: [0, 0, 16, 16] }, shade: false })];
const glass = (K) => [C('porthole_glass', [4, 8, 11.65], [12, 16, 11.65], { south: K + 'glass', all: null }, { uv: { south: [0, 0, 16, 16] } })];
// the click pawl on the rim at two o'clock
const pawlParts = (K) => [
  C('pawl_pin', [11.25, 13.6, 11], [12, 14.35, 12.15], { all: K + 'brass' }),
  C('pawl_arm', [9.75, 13.75, 11.1], [11.6, 14.25, 12.1], { all: K + 'brass' }, { rot: ['z', -22.5, [11.6, 14, 11.6]] }),
];
const gear = (K, s) => { const cx = s === 'L' ? 5.5 : 10.5; return [C('gear_' + s, [cx - 1, 7.25, 11.9], [cx + 1, 9.25, 12.4], { south: K + 'gear', north: null, all: K + 'brass' }, { uv: { south: [0, 0, 16, 16] } })]; };
const rotor = (K, s) => {
  const cx = s === 'L' ? 5.5 : 10.5, cz = 10, y0 = 4.25, y1 = 4.4;
  return [
    C('rotor_hub_' + s, [cx - 0.4, 4.2, cz - 0.4], [cx + 0.4, 4.45, cz + 0.4], { all: K + 'rotor_hub' }),
    C('rotor_a_' + s, [cx + 0.4, y0, cz - 0.2], [cx + 1.2, y1, cz + 0.2], { all: K + 'rotor' }),
    C('rotor_b_' + s, [cx - 1.2, y0, cz - 0.2], [cx - 0.4, y1, cz + 0.2], { all: K + 'rotor' }),
    C('rotor_c_' + s, [cx - 0.2, y0, cz + 0.4], [cx + 0.2, y1, cz + 1.2], { all: K + 'rotor' }),
    C('rotor_d_' + s, [cx - 0.2, y0, cz - 1.2], [cx + 0.2, y1, cz - 0.4], { all: K + 'rotor' }),
  ];
};
const hubCap = (K, s) => { const [hx, hy] = HUB[s]; return [C('hub_cap_' + s, [hx - 0.75, hy - 0.75, 8.1], [hx + 0.75, hy + 0.75, 8.5], { south: K + 'hub_cap', north: null, all: K + 'brass' }, { uv: { south: [0, 0, 16, 16] } })]; };
// one finger of the wing, pointing straight out from its joint. Inner half: a walnut rib (the leading spar heavier), the
// canvas stretched below it, a brass knuckle at the elbow. Outer half: the rib on to a brass ferrule, its canvas above it
// (so folded back 180 degrees it lies on the same side as the inner canvas), scalloped where it sags between rib tips.
function blade(K, s, k) {
  const [hx, hy] = HUB[s], dir = s === 'R' ? 1 : -1, a = INNER[k - 1], b = OUTER[k - 1], h = k === 1 ? 0.75 : 0.5;
  const zi = LAYER(k, false), zo = LAYER(k, true);
  const xs = (u, v) => [Math.min(hx + dir * u, hx + dir * v), Math.max(hx + dir * u, hx + dir * v)];
  // the inner canvas starts where, open, it clears the drum's side (half a pixel in from the hub) with the drum tucked in:
  // past the fan's tilt, and the V's lift, which swings a layer in front of the wing's pivot (z 6.6) in towards the drum;
  // its texture is cut to match, so it doesn't squash
  const tilt = (-OPEN[k - 1] * Math.PI) / 180, lean = (DIHEDRAL * Math.PI) / 180;
  const ahead = Math.max(0, zi + 0.225 - 6.6), clear = (0.5 + ahead * Math.sin(lean)) / Math.cos(lean);
  const m = Math.max(0.5, (clear + 3 * Math.sin(tilt)) / Math.cos(tilt) + 0.1);
  const cut = (16 * (m - 0.5)) / (a - 0.05);
  const flip = s === 'R' ? { south: [cut, 0, 16, 16], north: [16, 0, cut, 16] } : { south: [16, 0, cut, 16], north: [cut, 0, 16, 16] };
  const flipUp = s === 'R' ? { south: [0, 16, 16, 0], north: [16, 16, 0, 0] } : { south: [16, 16, 0, 0], north: [0, 16, 16, 0] };
  const [r0, r1] = xs(0, a - 0.45), [m0, m1] = xs(m, a + 0.45), [k0, k1] = xs(a - 0.45, a + 0.45);
  const [o0, o1] = xs(a + 0.45, a + b - 0.75), [t0, t1] = xs(a + b - 0.75, a + b), [n0, n1] = xs(a - 0.45, a + b - 1);
  const canvasTex = K + (k === 5 ? 'membrane' : 'canvas_plain');
  const inner = [
    C(`rib_${s}${k}`, [r0, hy, zi], [r1, hy + h, zi + LT], { all: K + 'rib' }),
    C(`canvas_${s}${k}`, [m0, hy - 3, zi + 0.075], [m1, hy, zi + 0.225], { south: canvasTex, north: canvasTex, all: null }, { uv: flip }),
    C(`knuckle_${s}${k}`, [k0, hy, zo], [k1, hy + h, zi + LT], { all: K + 'brass' }),
  ];
  const outer = [
    C(`rib_out_${s}${k}`, [o0, hy, zo], [o1, hy + h, zo + LT], { all: K + 'rib' }),
    C(`ferrule_${s}${k}`, [t0, hy - 0.1, zo], [t1, hy + h + 0.1, zo + LT], { all: K + 'brass' }),
    ...(k === 1 ? [] : [C(`canvas_out_${s}${k}`, [n0, hy + h, zo + 0.075], [n1, hy + h + OUT_W, zo + 0.225], { south: K + 'membrane_out_' + k, north: K + 'membrane_out_' + k, all: null }, { uv: flipUp })]),
  ];
  return { inner, outer, elbow: [hx + dir * a, hy + h / 2, zo + LT] };
}
// worn only: shoulder straps and a belt, hugging what's under them. What they wrap, in jetpack space (the worn renderer
// sets the plate a quarter pixel off it): a chestplate's shell, or the bare shirt, a pixel nearer the plate all round.
const SHELL = { plate: { x0: 3, x1: 13, top: 17, front: -2.25, back: 3.75 }, shirt: { x0: 4, x1: 12, top: 16, front: -0.25, back: 3.75 } };
const harness = (K, fit = 'plate') => {
  const S = SHELL[fit], f = S.front - 0.5, t = S.top;
  return [
    ...[[5, 6.5], [9.5, 11]].flatMap(([x0, x1], i) => [
      C('strap_top_' + i, [x0, t, f], [x1, t + 0.5, S.back + 0.5], { all: K + 'strap' }),
      C('strap_front_' + i, [x0, 9, f], [x1, t, S.front], { south: null, all: K + 'strap' }),
      // on the shirt, level with the plate's top: it stops at the plate rather than share its top face
      C('strap_back_' + i, [x0, t - 1, S.back], [x1, t, t > 16 ? S.back + 0.5 : 4], { north: null, all: K + 'strap' }),
      C('strap_buckle_' + i, [x0 - 0.25, 12, f - 0.25], [x1 + 0.25, 13.5, f], { south: null, all: K + 'buckle' }, { uv: { north: [0, 0, 16, 16] } }),
    ]),
    C('belt_front', [S.x0, 5, f], [S.x1, 6.5, S.front], { south: null, all: K + 'strap' }),
    C('belt_side_0', [S.x0 - 0.5, 5, f], [S.x0, 6.5, S.back + 0.25], { all: K + 'strap' }),
    C('belt_side_1', [S.x1, 5, f], [S.x1 + 0.5, 6.5, S.back + 0.25], { all: K + 'strap' }),
    // round the back to the plate's edges (the shirt's sides are the plate's: nothing to cross)
    ...(S.x0 < 4 ? [
      C('belt_back_0', [S.x0, 5, S.back], [4, 6.5, S.back + 0.5], { north: null, all: K + 'strap' }),
      C('belt_back_1', [12, 5, S.back], [S.x1, 6.5, S.back + 0.5], { north: null, all: K + 'strap' }),
    ] : []),
    C('belt_buckle', [7, 4.75, f - 0.25], [9, 6.75, f], { south: null, all: K + 'buckle' }, { uv: { north: [0, 0, 16, 16] } }),
  ];
};
// ---------- on a backpack (pack mode) ----------
// Worn with a backpack, the jetpack is built into it: everything moves PACK_OUT px out so the drum stands through the
// pack's face, seated in a stepped steel flange (one pixel round the drum's own steps, four brass rivets) that hides the cut;
// the ducts drop PACK_DROP px, their mouths out under the pack; the folded wings stow inside it and unfold out of its sides.
// A Sophisticated Backpack's face sits at z 9.35 (over a chestplate) to 9.55 (shirt) in jetpack space, its front pouch
// out to 11.35..11.55 over y 4..10, its bottom at y 4 (its BackpackModel, worn).
const PACK_OUT = 1, PACK_DROP = 2.5;
const FLANGE_Z = [9.2, 10];
const flangeSteps = [[3, 10, 14], [4, 9, 10], [4, 14, 15], [5, 8, 9], [5, 15, 16]]; // [x0 (left side), y0, y1], 1 px wide
const flange = (K) => [
  ...flangeSteps.flatMap(([x0, y0, y1], i) => [
    C('flange_l' + i, [x0, y0, FLANGE_Z[0]], [x0 + 1, y1, FLANGE_Z[1]], { north: null, all: K + 'steel_plain' }),
    C('flange_r' + i, [15 - x0, y0, FLANGE_Z[0]], [16 - x0, y1, FLANGE_Z[1]], { north: null, all: K + 'steel_plain' }),
  ]),
  C('flange_top', [6, 16, FLANGE_Z[0]], [10, 17, FLANGE_Z[1]], { north: null, all: K + 'steel_plain' }),
  C('flange_bottom', [6, 7, FLANGE_Z[0]], [10, 8, FLANGE_Z[1]], { north: null, all: K + 'steel_plain' }),
  ...[[3.25, 11.75], [12.25, 11.75], [7.75, 16.25], [7.75, 7.25]].map(([x, y], i) =>
    C('rivet_' + i, [x, y, FLANGE_Z[1]], [x + 0.5, y + 0.5, FLANGE_Z[1] + 0.25], { north: null, all: K + 'brass' })),
];

// ---------- the Jetpack Winding Station (its own block space; front north, z 0) ----------
// A coat stand for a flying machine: a steel plinth taking the shaft from below, a walnut post with the drive turning
// in a slot down its front, a brass yoke the jetpack hangs from (key out front, key centre at y 20, the same height as
// a standing wearer's key), and a brass boom from the post top that lowers, telescopes forward and drops a square
// socket onto the key. The reach parts are built retracted by REACH_BAKE; the clips slide them out.
const REACH_BAKE = 5.5, REACH_HUNG = -5.5, REACH_WORN = -1.5, BOOM_PARK = 60;
const stationFixed = (K) => [
  C('plinth', [1, 0, 1], [15, 4, 15], { up: K + 'base_top', down: K + 'socket', all: K + 'steel' }, { uv: { down: [0, 0, 16, 16] } }),
  C('gear_housing', [5.5, 4, 10.5], [10.5, 7, 15], { all: K + 'collar' }),
  C('post_l', [6.5, 8, 12.5], [7.5, 24.4, 15.5], { up: null, down: null, all: K + 'walnut' }),
  C('post_r', [8.5, 8, 12.5], [9.5, 24.4, 15.5], { up: null, down: null, all: K + 'walnut' }),
  C('post_back', [7.5, 8, 13.5], [8.5, 24.4, 15.5], { up: null, down: null, all: K + 'walnut' }),
  C('collar_low', [6.25, 7, 12.25], [9.75, 8, 15.75], { all: K + 'brass' }),
  C('collar_top', [6.25, 24.4, 12.25], [9.75, 25.4, 15.75], { all: K + 'brass' }),
  // the yoke the jetpack hangs from, its hooks down over the plate's top edge
  C('yoke', [5, 24, 11.5], [11, 24.75, 12.25], { all: K + 'brass' }),
  C('hook_l', [5.5, 23.5, 11], [6.5, 24, 11.5], { all: K + 'brass' }),
  C('hook_r', [9.5, 23.5, 11], [10.5, 24, 11.5], { all: K + 'brass' }),
];
const stationDrive = (K) => [C('drive', [7.7, 8, 12.7], [8.3, 24.4, 13.3], { up: null, down: null, all: K + 'axle' })];
const stationBoom = (K) => [
  C('boom_hub', [7, 25.4, 13], [9, 27, 15], { all: K + 'brass' }),
  C('boom_pawl', [9, 26.6, 13.6], [9.8, 27.2, 14.4], { all: K + 'brass' }),
  C('boom', [7.4, 25.4, 7.5], [8.6, 26.6, 13], { all: K + 'brass' }),
];
const stationReach = (K) => [
  C('boom_inner', [7.65, 25.65, 1.6 + REACH_BAKE], [8.35, 26.35, 7.25 + REACH_BAKE], { all: K + 'axle' }),
  C('drop_link', [7.65, 21.25, 1.475 + REACH_BAKE], [8.35, 25.65, 2.275 + REACH_BAKE], { up: null, all: K + 'brass' }),
];
const stationSocket = (K) => [C('socket', [6.75, 18.75, 1.25 + REACH_BAKE], [9.25, 21.25, 2.5 + REACH_BAKE], { north: K + 'socket_face', south: K + 'socket_face', all: K + 'brass' }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } })];
const shaftV = C('shaft', [6, -10, 6], [10, 0, 10], { up: 'create:block/axis_top', down: 'create:block/axis_top', all: 'create:block/axis' },
  { uv: { up: [6, 6, 10, 10], down: [6, 6, 10, 10], north: [6, 0, 10, 10], south: [6, 0, 10, 10], east: [6, 0, 10, 10], west: [6, 0, 10, 10] } });

// ---------- preview scenes ----------
const { cube, group, clip, write } = preview('clockwork-jetpack', { 'cj:': TP, 'cn:': TN, 'si:': SI });
const at = (list, off) => list.map((c) => cube(c, off));
const ROT = {}, HIDDEN = new Set();
// one jetpack's bones; pre: bone-name prefix per scene; worn adds the harness; pack: built into a backpack
function jetpack(pre, K, off, o = {}) {
  const sOff = [off[0], off[1], off[2] + (o.pack ? PACK_OUT : 0)];
  const kids = [];
  const wing = (s) => [
    group(pre + 'hub_' + s, [...HUB[s], 8.3], sOff, at(hubCap(K, s), sOff)),
    group(pre + 'wing_' + s, [...HUB[s], 6.6], sOff, [1, 2, 3, 4, 5].map((k) => {
      const B = blade(K, s, k);
      const elbow = group(`${pre}elbow_${s}${k}`, B.elbow, sOff, at(B.outer, sOff));
      ROT[`${pre}elbow_${s}${k}`] = [0, 0, s === 'R' ? -180 : 180];
      const g = group(`${pre}blade_${s}${k}`, [...HUB[s], LAYER(k, true) + LT], sOff, [...at(B.inner, sOff), elbow]);
      ROT[`${pre}blade_${s}${k}`] = [0, 0, s === 'R' ? -FOLDED : FOLDED];
      return g;
    })),
  ];
  const dOff = o.pack ? [sOff[0], sOff[1] - PACK_DROP, sOff[2]] : sOff;
  kids.push(group(pre + 'frame', [8, 10, 4], sOff, [
    ...at(frame(K), sOff),
    ...(o.pack ? [group(pre + 'flange', [8, 12, 9.5], off, at(flange(K), off))] : []),
    // the drum and all it carries: slides TUCK in (-z) while the wings are out
    group(pre + 'drum', [8, 12, 8.5], sOff, [
      ...at([...drum(K), ...glass(K)], sOff),
      group(pre + 'ducts', [8, 6, 10], dOff, at(ducts(K), dOff)),
      group(pre + 'key', [DRUM.x, DRUM.y, 12], sOff, at(keyParts(K), sOff)),
      group(pre + 'pawl', [11.6, 14, 11.6], sOff, at(pawlParts(K), sOff)),
      ...[0, 1, 2, 3].map((k) => group(pre + 'coil_' + k, [DRUM.x, DRUM.y, 11.25], sOff, at(coilPlane(K, k), sOff))),
      group(pre + 'gear_L', [5.5, 8.25, 12.15], sOff, at(gear(K, 'L'), sOff)), group(pre + 'gear_R', [10.5, 8.25, 12.15], sOff, at(gear(K, 'R'), sOff)),
      group(pre + 'rotor_L', [5.5, 4.45, 10], dOff, at(rotor(K, 'L'), dOff)), group(pre + 'rotor_R', [10.5, 4.45, 10], dOff, at(rotor(K, 'R'), dOff)),
    ]),
    ...wing('L'), ...wing('R'),
  ]));
  const body = group(pre + 'body', [8, 10, 4], off, kids);
  const root = [body];
  if (o.worn) {
    root.push(group(pre + 'harness', [8, 10, 4], off, at(harness(K), off)));
  }
  return group(pre + 'root', [8, 10, 4], off, root);
}
// one Winding Station's bones (pre: prefix per scene), with the shaft under it
function station(pre, K, off) {
  ROT[pre + 'boom'] = [BOOM_PARK, 0, 0];
  return group(pre + 'station', [8, 8, 8], off, [
    ...at(stationFixed(K), off),
    group(pre + 'drive', [8, 16, 13], off, at(stationDrive(K), off)),
    group(pre + 'boom', [8, 26, 14], off, [
      ...at(stationBoom(K), off),
      group(pre + 'reach', [8, 26, 8], off, [...at(stationReach(K), off), group(pre + 'socket', [8, 20, 1.875 + REACH_BAKE], off, at(stationSocket(K), off))]),
    ]),
    group(pre + 'shaft', [8, -5, 8], off, at([shaftV], off)),
  ]);
}
// Steve (in Generic Model space: feet on y 0, facing north, back at z +2), a chestplate shell, optionally a backpack
function steve(pre, sx, pack, sz = 0) {
  const o = [8 + sx, 0, 8 + sz], S = (n, f, t, tex) => cube(C(n, f, t, { all: 'si:' + tex }), o);
  const kids = [
    S('leg_l', [-4, 0, -2], [0, 12, 2], 'pants'), S('leg_r', [0, 0, -2], [4, 12, 2], 'pants'),
    S('body', [-4, 12, -2], [4, 24, 2], 'shirt'), S('head', [-4, 24, -4], [4, 32, 4], 'skin'), S('hair', [-4.25, 29, -4.25], [4.25, 32.25, 4.25], 'hair'),
    S('arm_l', [-8, 12, -2], [-4, 24, 2], 'skin'), S('arm_r', [4, 12, -2], [8, 24, 2], 'skin'),
    S('chest_shell', [-5, 11, -3], [5, 25, 3], 'iron'),
    S('arm_shell_l', [-9.25, 15, -3.25], [-3.75, 25.25, 3.25], 'iron'), S('arm_shell_r', [3.75, 15, -3.25], [9.25, 25.25, 3.25], 'iron'),
  ];
  // Sophisticated Backpacks' worn model (BackpackModel boxes, over a chestplate: 5.6 px out, its top a pixel over the
  // shoulders): body, lid, side pouches, front pouch; trims in the border colour
  if (pack === 'pack') kids.push(
    S('pack_body', [-5, 12, 2.6], [5, 25, 8.6], 'pack'), S('pack_lid', [-3.5, 21.25, 2.85], [3.5, 25.25, 8.85], 'pack_trim'),
    S('pack_side_l', [-7, 12, 3.1], [-5, 23, 8.1], 'pack'), S('pack_side_r', [5, 12, 3.1], [7, 23, 8.1], 'pack'),
    S('pack_front', [-4, 12, 8.6], [4, 18, 10.6], 'pack'), S('pack_front_trim', [-4, 17, 8.6], [4, 18, 10.65], 'pack_trim'),
  );
  return kids;
}
const outliner = [];
// 1. worn, centre: the player bone tips him into the fall-flying pose in the flight clip
const WORN = [0, 8, 7.25];
outliner.push(group('player', [0, 12, 0], [8, 0, 8], [...steve('', 0, false), jetpack('', 'cj:', WORN, { worn: true })]));
// 2. worn with a backpack, left: built into the pack (pack mode)
const PACK_X = -40, PACK = [PACK_X, 8, 7.25];
outliner.push(group('p_player', [PACK_X, 12, 0], [8, 0, 8], [...steve('p_', PACK_X, 'pack'), jetpack('p_', 'cj:', PACK, { worn: true, pack: true })]));
// 3. the Winding Station with a jetpack hung on it, right: turned to hang key-out (its plate against the post)
const ST1 = [40, 0, 0];
ROT.h_hang = [0, 180, 0];
outliner.push(station('st_', 'cj:', ST1));
outliner.push(group('h_hang', [8, 20, 8.25], ST1, [jetpack('h_', 'cj:', [ST1[0], 8, ST1[2]])]));
// 4. a Steve backing into a second station, further right: he stands on the block in front, his back to the post
const ST2 = [80, 0, 0];
outliner.push(station('ws_', 'cj:', ST2));
outliner.push(group('w_player', [ST2[0], 12, -16], [8, 0, 8], [...steve('w_', ST2[0], false, -16), jetpack('w_', 'cj:', [ST2[0], 8, 7.25 - 16], { worn: true })]));
// 5. the Netherite jetpack, worn, far right
const N_X = 120, NW = [N_X, 8, 7.25];
outliner.push(group('n_player', [N_X, 12, 0], [8, 0, 8], [...steve('n_', N_X, false), jetpack('n_', 'cn:', NW, { worn: true })]));

// ---------- icons: hand-drawn 16x16 (lighter than the model textures, like the Mainspring's) ----------
const ICON_MAP = [
  '................',
  '.dd..kkkkkk..dd.',
  '.dbdkSSSSSSkdbd.',
  '..ckSSwwwwSSkc..',
  '..ckSwoooowSkc..',
  '.rckbbbBBbbbkcr.',
  '.rckbwbBBbwbkcr.',
  '.rckbbbBBbbbkcr.',
  '..ckSwoooowSkc..',
  '..cckSSSSSSkcc..',
  '...cckkkkkkcc...',
  '....kbk..kbk....',
  '....kBk..kBk....',
  '....kdk..kdk....',
  '....kkk..kkk....',
  '................',
];
const ICON_COL = {
  plain: { k: '#1d2024', S: '#6d757f', s: '#5a616b', w: '#2f343b', o: '#9aa4ac', b: '#a3873f', B: '#b08f45', d: '#8f7638', c: '#9a8e74', r: '#7a5a40' },
  netherite: { k: '#161314', S: '#5d5456', s: '#4a4344', w: '#2a2526', o: '#9aa4ac', b: '#6a6265', B: '#7a7175', d: '#3a3436', c: '#7a4049', r: '#4e3c34' },
};
function icon(cols) {
  const d = new Uint8Array(16 * 16 * 4);
  ICON_MAP.forEach((row, y) => [...row].forEach((ch, x) => {
    const h = cols[ch];
    if (!h) return;
    d.set([parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255], (y * 16 + x) * 4);
  }));
  return png(16, 16, d);
}
// the Curios slot icon: the same silhouette as a flat light-grey shape, the key's holes and the window a shade darker
const SLOT = (() => {
  const d = new Uint8Array(16 * 16 * 4);
  ICON_MAP.forEach((row, y) => [...row].forEach((ch, x) => {
    if (ch === '.') return;
    const v = ch === 'w' || ch === 'k' ? 0x7b : 0xa8;
    d.set([v, v, v, 255], (y * 16 + x) * 4);
  }));
  return png(16, 16, d);
})();
TP.OWN.icon = icon(ICON_COL.plain); TN.OWN.icon = icon(ICON_COL.netherite); TP.OWN.slot_icon = SLOT;
const ICONS = [-80, 8, 0];
const sprite = (n, tex, x) => C(n, [x, 0, 8], [x + 16, 16, 8], { north: tex, south: tex, all: null }, { uv: { north: [16, 0, 0, 16], south: [0, 0, 16, 16] }, shade: false });
outliner.push(group('icons', [8, 8, 8], ICONS, at([sprite('item_icon', 'cj:icon', -18), sprite('netherite_icon', 'cn:icon', 0), sprite('slot_icon', 'cj:slot_icon', 18)], ICONS)));

// ---------- clips ----------
// Machine motion: the key and gears move in ticks; rotors spin smoothly; the wings snap open like a fan, with
// anticipation, overshoot and settle, and fold in reverse.
const frac = (v) => v - Math.floor(v);
const spring = (u, d = 12, w = 16) => (u <= 0 ? 0 : 1 - Math.exp(-d * u) * (Math.cos(w * u) + (d / w) * Math.sin(w * u)));
const settle = (u, a = 30) => (u <= 0 ? 0 : 1 - (1 + a * u) * Math.exp(-a * u));
// a ticking turn: n ticks of 30 degrees per second, each a quick flick then a rest
const tick = (t, perSec, sign = -1) => { const v = t * perSec; return sign * 30 * (Math.floor(v) + smooth(Math.min(1, frac(v) * 3))); };
const coils = (pre, f) => Object.fromEntries([0, 1, 2, 3].map((k) => [pre + 'coil_' + k, { scale: (t) => (f(t) === k ? SHOW : HIDE) }]));
const wings = (pre, f) => Object.fromEntries(['L', 'R'].flatMap((s) => [1, 2, 3, 4, 5].map((k) => [`${pre}blade_${s}${k}`, { rotation: (t) => [0, 0, (s === 'R' ? 1 : -1) * f(t, k)] }])));
const elbows = (pre, f) => Object.fromEntries(['L', 'R'].flatMap((s) => [1, 2, 3, 4, 5].map((k) => [`${pre}elbow_${s}${k}`, { rotation: (t) => [0, 0, (s === 'R' ? 1 : -1) * f(t, k)] }])));
const lift = (pre, f) => ({ [pre + 'wing_L']: { rotation: (t) => [0, f(t), 0] }, [pre + 'wing_R']: { rotation: (t) => [0, -f(t), 0] } });
const hubs = (pre, f) => ({ [pre + 'hub_L']: { rotation: (t) => [0, 0, -f(t)] }, [pre + 'hub_R']: { rotation: (t) => [0, 0, f(t)] } });
const spin = (pre, revPerSec) => ({
  [pre + 'rotor_L']: { rotation: (t) => [0, 360 * revPerSec * t, 0] }, [pre + 'rotor_R']: { rotation: (t) => [0, -360 * revPerSec * t, 0] },
  [pre + 'gear_L']: { rotation: (t) => [0, 0, 120 * revPerSec * t] }, [pre + 'gear_R']: { rotation: (t) => [0, 0, -120 * revPerSec * t] },
});
const full = () => 3;
// approved sounds (design/sound/clockwork-jetpack.md): each clip's cues become sound keyframes on its Effects track;
// the build stops if a cued sound has no real file
const SOUND_DIR = path.resolve(__dirname, '../sound/clockwork-jetpack');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'clockwork-jetpack.cues.json'), 'utf8'));
const sounds = (name) => (CUES.clips[name] || []).map(([t, id]) => {
  const file = path.join(SOUND_DIR, 'real', id + '.ogg');
  if (!fs.existsSync(file)) throw new Error(name + ': no real sound for ' + id);
  return [t, CUES.namespace + '.' + id, file.split(path.sep).join('/')];
});

clip('idle', 2, 'loop', { ...coils('', full) });
clip('thrust', 1, 'loop', { ...coils('', full), key: { rotation: (t) => [0, 0, tick(t, 12)] }, ...spin('', 4) });
clip('hover', 2, 'loop', { ...coils('', full), key: { rotation: (t) => [0, 0, tick(t, 6)] }, ...spin('', 2) });
// two stages: a 0.05 s anticipation (the ribs tuck 5 degrees), the fan spreads from the leading rib 0.05 s apart, then the
// elbows straighten, leading rib first, and the wings lift into their V
// the drum tucks in once the wings are out (from 0.9 s), and out again before they fold (the first 0.2 s)
const tuck = (pre, f) => ({ [pre + 'drum']: { position: (t) => [0, 0, -TUCK * f(t)] } });
clip('wings_unfold', 1.3, 'hold', {
  ...tuck('', (t) => settle(t - 0.9, 25)), ...tuck('n_', (t) => settle(t - 0.9, 25)),
  ...coils('', full), key: { rotation: (t) => [0, 0, tick(t, 6)] }, ...spin('', 2),
  ...wings('', (t, k) => -5 * smooth(Math.min(1, t / 0.05)) + (DELTA[k - 1] + 5) * spring(t - (0.05 + 0.05 * (k - 1)))),
  ...elbows('', (t, k) => 180 * spring(t - (0.3 + 0.04 * (k - 1)), 14, 14)),
  ...hubs('', (t) => 90 * spring(t - 0.05)), ...lift('', (t) => DIHEDRAL * spring(t - 0.55)),
  ...wings('n_', (t, k) => -5 * smooth(Math.min(1, t / 0.05)) + (DELTA[k - 1] + 5) * spring(t - (0.05 + 0.05 * (k - 1)))),
  ...elbows('n_', (t, k) => 180 * spring(t - (0.3 + 0.04 * (k - 1)), 14, 14)),
  ...hubs('n_', (t) => 90 * spring(t - 0.05)), ...lift('n_', (t) => DIHEDRAL * spring(t - 0.55)),
  ...wings('p_', (t, k) => -5 * smooth(Math.min(1, t / 0.05)) + (DELTA[k - 1] + 5) * spring(t - (0.05 + 0.05 * (k - 1)))),
  ...elbows('p_', (t, k) => 180 * spring(t - (0.3 + 0.04 * (k - 1)), 14, 14)),
  ...hubs('p_', (t) => 90 * spring(t - 0.05)), ...lift('p_', (t) => DIHEDRAL * spring(t - 0.55)),
}, {}, sounds('wings_unfold'));
// open, in the fall-flying pose: the trailing ribs tremble like air on a sail
clip('flight', 1.5, 'loop', {
  ...coils('', full), player: { rotation: () => [-90, 0, 0] }, ...tuck('', () => 1), ...tuck('n_', () => 1),
  key: { rotation: (t) => [0, 0, tick(t, 8)] }, ...spin('', 2),
  ...wings('', (t, k) => DELTA[k - 1] + (k >= 3 ? 1.5 * Math.sin(2 * Math.PI * (t / 0.5) + k) : 0)),
  ...elbows('', (t, k) => 180 + (k >= 3 ? 1.5 * Math.sin(2 * Math.PI * (t / 0.5) + k + 1) : 0)),
  ...hubs('', () => 90), ...lift('', () => DIHEDRAL),
  ...wings('n_', (t, k) => DELTA[k - 1]), ...elbows('n_', () => 180), ...hubs('n_', () => 90), ...lift('n_', () => DIHEDRAL),
  ...wings('p_', (t, k) => DELTA[k - 1]), ...elbows('p_', () => 180), ...hubs('p_', () => 90), ...lift('p_', () => DIHEDRAL),
  p_player: { rotation: () => [-90, 0, 0] },
});
// the reverse: the wings drop out of their V and the elbows fold, trailing rib first; then the fan closes, no overshoot,
// and the leading rib lands with a 2 degree bounce
const FOLD_AFTER = 0.2;
clip('wings_fold', 0.7 + FOLD_AFTER, 'hold', {
  ...coils('', full), ...tuck('', (t) => 1 - settle(t, 30)),
  ...elbows('', (t, k) => 180 * (1 - settle(t - FOLD_AFTER - 0.03 * (5 - k), 30))),
  ...wings('', (t, k) => {
    const u = t - FOLD_AFTER - 0.2 - 0.05 * (5 - k);
    const b = k === 1 && u > 0.15 ? 2 * Math.exp(-25 * (u - 0.15)) * Math.sin(40 * (u - 0.15)) : 0;
    return DELTA[k - 1] * (1 - settle(u)) + b;
  }),
  ...hubs('', (t) => 90 * (1 - settle(t - FOLD_AFTER - 0.2))), ...lift('', (t) => DIHEDRAL * (1 - settle(t - FOLD_AFTER, 40))),
}, {}, sounds('wings_fold').map(([t, ...rest]) => [t + FOLD_AFTER, ...rest]));
// the spring runs out: the key and rotors slow to a stop, a last half-tick shiver, the pawl drops with a click
const runAngle = (t) => { let a = 0; const dt = 0.005; for (let s = 0; s < Math.min(t, 1.5); s += dt) a += 360 * (1 - smooth(s / 1.5)) * dt; return a; };
clip('run_down', 2.5, 'hold', {
  ...coils('', () => 0),
  key: { rotation: (t) => [0, 0, -(Math.floor(runAngle(t) / 30) * 30 + 30 * smooth(Math.min(1, frac(runAngle(t) / 30) * 3))) + (t > 1.75 ? 5 * Math.exp(-12 * (t - 1.75)) * Math.sin(50 * (t - 1.75)) : 0)] },
  rotor_L: { rotation: (t) => [0, 2 * runAngle(t), 0] }, rotor_R: { rotation: (t) => [0, -2 * runAngle(t), 0] },
  gear_L: { rotation: (t) => [0, 0, runAngle(t) / 3] }, gear_R: { rotation: (t) => [0, 0, -runAngle(t) / 3] },
  pawl: { rotation: (t) => [0, 0, t > 1.6 ? -10 * settle(t - 1.6, 60) : 0] },
}, {}, sounds('run_down'));
// the wind gauge: full to empty, a quarter a second
clip('coil_states', 4, 'loop', { ...coils('', (t) => 3 - Math.min(3, Math.floor(t))), key: { rotation: (t) => [0, 0, tick(t, 6)] } }, {}, sounds('coil_states'));
// the Winding Station: the boom lowers, telescopes out and its socket meets the key; it turns the key (the jetpack's
// pawl clicking) while the coil tightens; full, the boom draws back and lifts to park. jp: the jetpack's bone prefix;
// reach: how far the socket slides out; keySign: the hung jetpack is turned round, so its key turns the other way locally
function windingClip(name, st, jp, reach, keySign) {
  // out first while parked high, then down in front of the jetpack; back up first, then in (never through the drum)
  const ext = (t) => settle(t - 0.1, 25) * (1 - settle(t - 5.45, 25));
  const eng = (t) => settle(t - 0.4, 20) * (1 - settle(t - 5.0, 20));
  const w = (t) => Math.max(0, Math.min(t, 4.9) - 0.8);
  const turn = (t) => (w(t) > 0 ? tick(w(t), 6, 1) : 0);
  clip(name, 6, 'loop', {
    ...coils(jp, (t) => Math.max(0, Math.min(3, Math.floor(t - 1)))),
    [st + 'boom']: { rotation: (t) => [-BOOM_PARK * eng(t), 0, 0] },
    [st + 'reach']: { position: (t) => [0, 0, reach * ext(t)] },
    [st + 'socket']: { rotation: (t) => [0, 0, turn(t)] },
    [jp + 'key']: { rotation: (t) => [0, 0, keySign * turn(t)] },
    [jp + 'pawl']: { rotation: (t) => [0, 0, w(t) > 0 && t < 4.9 ? 10 * Math.max(0, 1 - frac(w(t) * 6) * 4) : 0] },
    [st + 'drive']: { rotation: (t) => [0, 180 * t, 0] }, [st + 'shaft']: { rotation: (t) => [0, 180 * t, 0] },
  }, {}, sounds(name));
}
windingClip('station_hang', 'st_', 'h_', REACH_HUNG, -1);
windingClip('station_back_in', 'ws_', 'w_', REACH_WORN, 1);
clip('station_idle', 2, 'loop', { st_drive: { rotation: (t) => [0, 180 * t, 0] }, st_shaft: { rotation: (t) => [0, 180 * t, 0] } });
write(OUT, outliner);

// rest pose: blades folded (a bone's own rotation), only the full coil frame shown
const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (ROT[n.name]) n.rotation = ROT[n.name]; n.children.forEach(walk); } };
bb.outliner.forEach(walk);
for (const g of bb.groups) if (ROT[g.name]) g.rotation = ROT[g.name];
fs.writeFileSync(OUT, JSON.stringify(bb));

// ---------- checks ----------
const shift = (list, dy, dz) => list.map((c) => ({ ...c, from: [c.from[0], c.from[1] + dy, c.from[2] + dz], to: [c.to[0], c.to[1] + dy, c.to[2] + dz] }));
const K = 'cj:';
const problems = check({
  frame: [...frame(K), ...drum(K)], ducts: ducts(K), key: keyParts(K), pawl: pawlParts(K), glass: glass(K), harness: harness(K),
  coil_3: coilPlane(K, 3), gear_L: gear(K, 'L'), gear_R: gear(K, 'R'), rotor_L: rotor(K, 'L'), rotor_R: rotor(K, 'R'), hub_L: hubCap(K, 'L'), hub_R: hubCap(K, 'R'),
  ...Object.fromEntries(['L', 'R'].flatMap((s) => [1, 2, 3, 4, 5].map((k) => [`blade_${s}${k}`, [...blade(K, s, k).inner, ...blade(K, s, k).outer]]))),
});
problems.push(...check({ frame: frame(K), harness_shirt: harness(K, 'shirt') }));
problems.push(...check({ frame: frame(K), drum_tucked: shift([...drum(K), ...glass(K), ...ducts(K)], 0, -TUCK) }));
problems.push(...check({ frame_pack: shift([...frame(K), ...drum(K)], 0, PACK_OUT), ducts_pack: shift(ducts(K), -PACK_DROP, PACK_OUT), flange: flange(K), harness: harness(K) }));
problems.push(...check({ station: stationFixed(K), drive: stationDrive(K), boom: stationBoom(K), reach: stationReach(K), socket: stationSocket(K) }));
for (const p of problems) console.log(p);
console.log(problems.length ? `${problems.length} z-fights` : 'no z-fights');
for (const [n, buf] of [['clockwork-jetpack-icon.png', TP.OWN.icon], ['netherite-clockwork-jetpack-icon.png', TN.OWN.icon], ['jetpack-slot-icon.png', SLOT]])
  fs.writeFileSync(path.resolve(__dirname, n), buf);
module.exports = { TP, TN, frame, ducts, TUCK, flange, PACK_OUT, PACK_DROP, drum, keyParts, coilPlane, glass, pawlParts, gear, rotor, hubCap, blade, harness, stationFixed, stationDrive, stationBoom, stationReach, stationSocket, HUB, DRUM, FOLDED, OPEN, DELTA, LAYER, REACH_BAKE, REACH_HUNG, REACH_WORN, BOOM_PARK };
