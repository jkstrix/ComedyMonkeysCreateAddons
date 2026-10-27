// The Clockwork Juggernaut's props -> design/models/juggernaut-props.bbmodel (Generic Model preview with clips), to the brief
// design/briefs/clockwork-juggernaut.md (Winding Key, Mainspring, Spring Engine; Revision 2's Norse palette). Built on the
// family kit in the game's model space (front north, z 0), so every part exports as a block model; the turning key, the
// coil frames, the ratchet, escapement and the lit runes are the renderer's parts.
const fs = require('fs');
const path = require('path');
const { C, textureSet, mot, hash, edge, png, preview, shaftStub, SHOW, HIDE, smooth } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'juggernaut-props.bbmodel');

// ---------- the Juggernaut's palette ----------
const IRON = ['#343a42', '#3d444d', '#454d56'], IRON_HI = '#56606b', CHAR = ['#1c1e21', '#232629', '#2a2d31'];
const BRONZE = ['#5c4a2e', '#6e5a36', '#7d6a44'], BRONZE_DARK = '#463822', PATINA = ['#4f6b5e', '#5b7a6c'];
const GOLD = ['#a8873e', '#93763a'], STONE = ['#4e504d', '#5a5c58', '#55574f'], CRACK = '#141516';
const FROST = ['#6f7c86', '#7a8791'], RUNE = ['#4f8fae', '#4a87a4'], RUNE_HI = '#7ab8d0', RUNE_DIM = '#26343c';
const pick = (arr, x, y, s) => arr[Math.floor(hash(x, y, s) * arr.length * 0.999)];
// Elder Futhark, 3x5
const RUNES = [
  ['.#.', '###', '#.#', '.#.', '.#.'], ['#.#', '##.', '#.#', '##.', '#..'], ['##.', '#.#', '#.#', '#.#', '#.#'], ['#..', '##.', '#.#', '##.', '#..'],
  ['##.', '#.#', '##.', '#.#', '#.#'], ['#.#', '#.#', '.#.', '.#.', '.#.'], ['.#.', '#.#', '.#.', '#.#', '#.#'], ['.#.', '#.#', '.#.', '#.#', '.#.'],
];
const runeAt = (x, y, x0, y0, seed) => {
  if (x < x0) return false;
  const k = Math.floor((x - x0) / 4), dx = (x - x0) % 4, dy = y - y0;
  if (dx === 3 || dy < 0 || dy > 4) return false;
  return RUNES[(k * 5 + seed) % RUNES.length][dy][dx] === '#';
};

// ---------- textures (jug_<key>) ----------
const T = textureSet(); T.file = 'jug_';
const iron = (x, y, s = 1) => (y === 0 && hash(x, y, s) < 0.5 ? IRON_HI : y === 15 && hash(x, y, s + 1) < 0.4 ? '#3e3530' : pick(IRON, x, y, s));
T.paint('iron', (x, y) => iron(x, y, 3));
T.paint('char', (x, y) => pick(CHAR, x, y, 4));
T.paint('bronze', (x, y) => (hash(x, y, 5) < 0.12 ? pick(PATINA, x, y, 6) : pick(BRONZE, x, y, 7)));
T.paint('bronze_teeth', (x, y) => ((x + y) % 3 === 0 ? BRONZE_DARK : hash(x, y, 8) < 0.1 ? pick(PATINA, x, y, 9) : pick(BRONZE, x, y, 10)));
T.paint('bronze_dark', (x, y) => (hash(x, y, 11) < 0.5 ? BRONZE_DARK : BRONZE[0]));
// the key's bars: bronze with a gold inlay line down the middle
T.paint('key_bar', (x, y) => (y === 7 || y === 8 ? (x % 3 === 1 ? GOLD[1] : GOLD[0]) : y === 15 ? BRONZE_DARK : hash(x, y, 12) < 0.1 ? pick(PATINA, x, y, 13) : pick(BRONZE, x, y, 14)));
// the hub's front: bronze round a dark square socket for the shaft
T.paint('key_hub', (x, y) => (x >= 4 && x <= 11 && y >= 4 && y <= 11 ? (x === 4 || y === 4 ? '#14161a' : '#1c1e21') : edge(x, y) === 0 ? BRONZE_DARK : pick(BRONZE, x, y, 15)));
// the socket plate: a bronze rim, a knotwork border, a ring of runes (dark until wound), a dark socket in the middle
const knot = (x, y) => ((x + y) % 4 === 0) !== ((x - y + 64) % 4 === 0);
const ringRune = (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r < 4.6 || r > 6.4) return false; const a = Math.round(((Math.atan2(y - 7.5, x - 7.5) + Math.PI) / (2 * Math.PI)) * 24); return a % 3 !== 2 && hash(a, Math.round(r), 61) < 0.75; };
T.paint('plate_front', (x, y) => {
  if (edge(x, y) === 0) return pick(BRONZE, x, y, 16);
  if (edge(x, y) === 1) return knot(x, y) ? BRONZE_DARK : pick(IRON, x, y, 17);
  if (ringRune(x, y)) return RUNE_DIM;
  if (Math.hypot(x - 7.5, y - 7.5) < 3.4) return '#1c1e21';
  return iron(x, y, 18);
});
T.paint('runes_lit', (x, y) => (ringRune(x, y) ? ((x + y) % 2 ? RUNE[0] : RUNE[1]) : null));
// stone, cracked and stapled
const crackX = (y, seed) => Math.round(4 + hash(seed, 1, 5) * 8 + 1.4 * Math.sin(y * 0.7 + seed));
const stone = (seed) => (x, y) => {
  const cx = crackX(y, seed);
  if (y > 0 && y < 15 && x === cx) return CRACK;
  if (y % 5 === 2 && Math.abs(x - cx) <= 2 && Math.abs(x - cx) >= 1) return pick(IRON, x, y, seed);
  if (y === 0 && hash(x, y, seed) < 0.3) return pick(FROST, x, y, seed + 1);
  return hash(x, y, seed + 2) < 0.05 ? '#666863' : pick(STONE, x, y, seed + 3);
};
T.paint('stone', stone(21));
T.paint('stone_top', (x, y) => (edge(x, y) <= 1 && hash(x >> 1, y >> 1, 22) < 0.35 ? pick(FROST, x, y, 23) : stone(24)(x, y)));
// the drum: iron staves (a seam every 4 px) with a rune band (dark until wound) down its top
T.paint('drum', (x, y) => (x % 4 === 0 ? CHAR[2] : iron(x, y, 25)));
T.paint('drum_top', (x, y) => (x % 4 === 0 ? CHAR[2] : (x >= 6 && x <= 9) ? (runeAt(y, x - 6, 0, 0, 3) ? RUNE_DIM : CHAR[1]) : y % 5 === 0 && hash(x, y, 26) < 0.6 ? pick(FROST, x, y, 27) : iron(x, y, 28)));
// rune band, lit to a level: k of 4 quarters (the renderer picks the frame by how wound it is)
for (let k = 1; k <= 4; k++) T.paint('runes_band_' + k, (x, y) => (x >= 6 && x <= 9 && y < k * 4 && runeAt(y, x - 6, 0, 0, 3) ? (y === k * 4 - 1 ? RUNE_HI : RUNE[(x + y) % 2]) : null));
// the end plate: an iron ring with a round window (clear), a bronze rim, and the shaft hole in the middle
const winR = (x, y) => Math.hypot(x - 7.5, y - 7.5);
T.paint('end_plate', (x, y) => { const r = winR(x, y); if (r < 2.6) return '#14161a'; if (r < 6.1) return null; if (r > 7.2) return pick(BRONZE, x, y, 29); return iron(x, y, 30); });
T.paint('glass', (x, y) => { const r = winR(x, y); if (r < 2.6 || r > 6.4) return null; return (x === 5 && y === 4) || (x === 4 && y === 5) ? '#83939a99' : null; });
// the coil seen through the window: wound, the spring packs tight round the arbor; run down, it lies out against the
// barrel wall (frames 0 empty .. 3 full): the more wound, the more steel shows round the shaft; the dark barrel behind
const coil = (k) => (x, y) => {
  const r = winR(x, y);
  if (r < 1.6) return null;
  if (r < 2.7) return pick(BRONZE, x, y, 31);
  if (r > 6.3) return null;
  if ((x === 5 && y === 4) || (x === 4 && y === 5)) return '#83939a'; // a glint on the glass
  const rIn = [5.3, 4.4, 3.5, 2.7][k], rOut = [6.3, 6.0, 5.6, 5.0][k];
  if (r >= rIn && r <= rOut) return Math.round(r * 1.6) % 2 ? (y < 8 ? '#8a949c' : '#6d7780') : '#4a525a';
  return '#16181b';
};
const COILS = [0, 1, 2, 3];
COILS.forEach((k) => T.paint('coil_' + k, coil(k)));
// a ratchet or escapement wheel face: a toothed bronze disc with a dark hub
T.paint('wheel', (x, y) => { const r = winR(x, y); if (r < 1.6) return '#14161a'; const a = Math.atan2(y - 7.5, x - 7.5); const tooth = Math.floor(((a + Math.PI) / (2 * Math.PI)) * 12) % 2; return r > 6.5 ? (tooth ? BRONZE_DARK : pick(BRONZE, x, y, 32)) : pick(BRONZE, x, y, 33); });
T.paint('leather', (x, y) => (y === 0 || y === 15 ? CHAR[0] : hash(x, y, 34) < 0.4 ? '#46321f' : '#3b2a1f'));
// brass blocks for the effigy stand-in
T.paint('brass_block', (x, y) => (edge(x, y) === 0 ? '#6e5a2b' : edge(x, y) === 1 ? '#c9a75a' : hash(x, y, 35) < 0.2 ? '#9c8240' : '#b08f45'));

// ---------- the Mainspring (the item, a flat 16x16 icon, lighter than the model textures) ----------
const ICON = (() => {
  const d = new Uint8Array(16 * 16 * 4);
  const put = (x, y, h) => { const n = (y * 16 + x) * 4; d[n] = parseInt(h.slice(1, 3), 16); d[n + 1] = parseInt(h.slice(3, 5), 16); d[n + 2] = parseInt(h.slice(5, 7), 16); d[n + 3] = 255; };
  // a square-cut spiral of steel strip (1 px strip, 1 px gap) wound out from a bronze arbor, lit from the top left;
  // a hooked tail at the outer end; a dark outline round the outside only
  const G = [...Array(16)].map(() => Array(16).fill(null));
  const dirs = [[1, 0], [0, 1], [-1, 0], [0, -1]];
  let x = 8, y = 8, dir = 0, len = 1;
  G[y][x] = 'S';
  outer: for (let k = 0; k < 40; k++) {
    for (let r = 0; r < 2; r++) {
      for (let i = 0; i < len * 2; i++) {
        x += dirs[dir][0]; y += dirs[dir][1];
        if (x < 1 || y < 2 || x > 14 || y > 14) break outer;
        G[y][x] = dir === 0 || dir === 3 ? 'S' : 's'; // top runs and left runs catch the light
      }
      dir = (dir + 1) % 4;
    }
    len++;
  }
  // the arbor: bronze round a square hole
  for (const [ax, ay] of [[7, 7], [8, 7], [7, 8]]) G[ay][ax] = 'B';
  G[8][8] = 'o';
  // the tail: out of the last turn and down to a hook
  G[14][14] = 's'; G[15][14] = 's'; G[15][13] = 's';
  // round off the outer turn's corners
  for (const [cx, cy] of [[2, 2], [14, 2], [2, 14]]) if (G[cy][cx]) G[cy][cx] = null;
  // outline: transparent pixels outside the coil that touch it
  const out = [...Array(16)].map(() => Array(16).fill(false)), q = [];
  for (let i = 0; i < 16; i++) for (const [px, py] of [[i, 0], [i, 15], [0, i], [15, i]]) if (!G[py][px] && !out[py][px]) { out[py][px] = true; q.push([px, py]); }
  while (q.length) { const [px, py] = q.pop(); for (const [dx, dy] of dirs) { const nx = px + dx, ny = py + dy; if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && !G[ny][nx] && !out[ny][nx]) { out[ny][nx] = true; q.push([nx, ny]); } } }
  const COL = { S: '#8d979f', s: '#646e77', B: '#b08f45', o: '#24282d', k: '#1d2024' };
  for (let py = 0; py < 16; py++) for (let px = 0; px < 16; px++) {
    let c = G[py][px];
    if (!c && out[py][px] && dirs.some(([dx, dy]) => { const nx = px + dx, ny = py + dy; return nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && G[ny][nx]; })) c = 'k';
    if (c) put(px, py, COL[c]);
  }
  return png(16, 16, d);
})();
T.OWN.mainspring = ICON;

// ---------- the Winding Key (1 block; its back against the effigy's brass, the key and shaft out of its front) ----------
const K = 'jp:';
const keyBase = [
  C('plate', [1, 1, 14], [15, 15, 16], { north: K + 'plate_front', south: K + 'iron', all: K + 'bronze' }),
  C('collar', [5, 5, 11.5], [11, 11, 14], { all: K + 'bronze_teeth' }),
  C('collar_teeth', [5.4, 5.4, 11.75], [10.6, 10.6, 13.75], { all: K + 'bronze_dark' }, { rot: ['z', 45, [8, 8, 12.75]] }),
  C('pawl_bolt', [12, 11, 12], [14, 13, 14], { all: K + 'bronze' }),
  C('pawl_arm', [9.5, 11.5, 12.5], [13, 12.5, 13.5], { all: K + 'iron' }, { rot: ['z', -22.5, [13, 12, 13]] }),
];
const keyRunes = [C('runes', [1.5, 1.5, 13.75], [14.5, 14.5, 13.75], { north: K + 'runes_lit', south: K + 'runes_lit', all: null }, { uv: { north: [0.5, 0.5, 13.5, 13.5], south: [0.5, 0.5, 13.5, 13.5] }, shade: false })];
const bar = (name, from, to) => C(name, from, to, { all: K + 'key_bar' });
const keyTurning = [
  C('key_stem', [6.75, 6.75, 7], [9.25, 9.25, 11.5], { all: K + 'bronze_teeth' }),
  C('key_hub', [5.5, 5.5, 4], [10.5, 10.5, 7], { north: K + 'key_hub', all: K + 'bronze' }),
  // two loops either side of the hub, 1.5 px bars
  bar('loop_r_top', [10.5, 10, 4.5], [15, 11.5, 6.5]), bar('loop_r_bottom', [10.5, 4.5, 4.5], [15, 6, 6.5]), bar('loop_r_outer', [13.5, 6, 4.5], [15, 10, 6.5]),
  bar('loop_l_top', [1, 10, 4.5], [5.5, 11.5, 6.5]), bar('loop_l_bottom', [1, 4.5, 4.5], [5.5, 6, 6.5]), bar('loop_l_outer', [1, 6, 4.5], [2.5, 10, 6.5]),
];

// ---------- the Spring Engine (2 blocks along its shafts: the output end in front, the winding end behind) ----------
// one block's drum section (z0..z1 in that block's own model space), with bands at both ends
function drum(z0, z1) {
  return [
    C('drum_core', [1, 6, z0], [15, 12, z1], { up: null, down: null, all: K + 'drum' }),
    C('drum_mid_top', [2, 12, z0], [14, 14.5, z1], { down: null, up: null, all: K + 'drum' }),
    C('drum_top', [4, 14.5, z0], [12, 16, z1], { down: null, up: K + 'drum_top', all: K + 'drum' }),
    C('drum_mid_bottom', [2, 3.5, z0], [14, 6, z1], { up: null, down: null, all: K + 'drum' }),
  ];
}
// a band round the drum (wider than the staves, so their edges never meet)
const band = (name, z0, z1) => [
  C(name + '_core', [0.5, 5.5, z0], [15.5, 12.5, z1], { all: K + 'bronze_teeth' }),
  C(name + '_top', [1.5, 12.5, z0], [14.5, 15, z1], { all: K + 'bronze_teeth', down: null }),
  C(name + '_crown', [3.5, 15, z0], [12.5, 16.5, z1], { all: K + 'bronze_teeth', down: null }),
];
const cradle = (z0, z1) => [
  C('cradle', [1, 0, z0], [15, 3.5, z1], { up: K + 'stone_top', all: K + 'stone' }),
  C('cradle_strap', [0.5, 3.5, (z0 + z1) / 2 - 1.5], [15.5, 6, (z0 + z1) / 2 + 1.5], { all: K + 'leather' }),
];
// the end plate with its window, the glass, and the coil behind it (the renderer swaps the coil's frame by wind level)
function endWindow(z, dir) {
  const s = dir; // +1: the plate faces north at z, -1: faces south at z
  const zz = (a) => z + s * a;
  const lo = (a, b) => [Math.min(zz(a), zz(b)), Math.max(zz(a), zz(b))];
  const [p0, p1] = lo(0, 2), [g] = lo(0.75, 0.75), [c] = lo(1.5, 1.5);
  const face = s > 0 ? 'north' : 'south';
  return {
    plate: [C('end_plate', [1, 2, p0], [15, 16, p1], { [face]: K + 'end_plate', [s > 0 ? 'south' : 'north']: null, all: K + 'iron' }, { uv: { [face]: [0, 0, 16, 16] } })],
    glass: [C('end_glass', [1, 2, g], [15, 16, g], { north: K + 'glass', south: K + 'glass', all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } })],
    coil: COILS.map((_, k) => [C('coil_' + k, [1, 2, c - s * 0.05 * [0, 1, 3, 2][k]], [15, 16, c - s * 0.05 * [0, 1, 3, 2][k]], { north: K + 'coil_' + k, south: K + 'coil_' + k, all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } })]),
  };
}
// a wheel on the drum's east side (the ratchet on the winding end, the escape wheel on the output end), turning about x
const wheel = (name, cz) => [C(name, [15, 5, cz - 4], [16.5, 13, cz + 4], { east: K + 'wheel', west: K + 'wheel', all: K + 'bronze_teeth' }, { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] } })];
const frontWin = endWindow(0, 1), backWin = endWindow(16, -1);
const engineFront = [...cradle(1, 15), ...drum(2, 14), ...band('band_seam', 14, 16), ...frontWin.plate];
const engineBack = [...cradle(1, 15), ...drum(2, 14), ...band('band_seam', 0, 2), ...backWin.plate];
const escapeWheel = wheel('escape_wheel', 8);
const anchor = [
  C('anchor_arm', [16.5, 12, 4], [17.5, 13, 12], { all: K + 'iron' }),
  C('anchor_pallet_a', [16.5, 9.5, 4], [17.5, 12, 5.5], { all: K + 'iron' }),
  C('anchor_pallet_b', [16.5, 9.5, 10.5], [17.5, 12, 12], { all: K + 'iron' }),
  C('anchor_pivot', [16.5, 13, 7], [18, 15, 9], { all: K + 'bronze' }),
];
const ratchet = wheel('ratchet', 8);
const pawl = [C('pawl_bar', [16.5, 13, 2], [17.5, 14, 8], { all: K + 'iron' }), C('pawl_pin', [16.5, 13, 1.5], [18, 15, 3], { all: K + 'bronze' })];
const runeBand = (z0, z1) => [1, 2, 3, 4].map((k) => [C('runes_' + k, [4, 16.25 + 0.03 * k, z0], [12, 16.25 + 0.03 * k, z1], { up: K + 'runes_band_' + k, all: null }, { shade: false })]);

// ---------- preview: the scenes side by side ----------
const { cube, group, clip, write } = preview('juggernaut-props', { 'jp:': T });
const at = (list, off, shift) => list.map((c) => cube(c, off, shift));
// 1. the effigy: four Brass Blocks in a T, with the Winding Key on the centre block (turned to face the camera) and a shaft into it
const EFF = [0, 0, 0];
const brass = (n, x, y) => C('brass_' + n, [x, y, 16], [x + 16, y + 16, 32], { all: K + 'brass_block' });
const effigy = [brass(0, -16, 16), brass(1, 0, 16), brass(2, 16, 16), brass(3, 0, 0)];
const shaft = shaftStub().map((c) => ({ ...c, from: [6, 6, -10], to: [10, 10, 5], name: 'key_shaft' }));
const outliner = [];
outliner.push(group('effigy', [8, 16, 24], EFF, at(effigy, EFF)));
outliner.push(group('winding_key', [8, 24, 8], EFF, [
  ...at(keyBase, EFF, [0, 16, 0]),
  group('key_runes', [8, 24, 13.75], EFF, at(keyRunes, EFF, [0, 16, 0])),
  group('key', [8, 24, 8], EFF, [...at(keyTurning, EFF, [0, 16, 0]), ...at(shaft, EFF, [0, 16, 0])]),
]));
// 2. the Spring Engine, a little to the right; the output end at the front, the winding end behind, shafts at both ends
const ENG = [56, 0, 0];
const shaftF = shaftStub().map((c) => ({ ...c, from: [6, 7, -10], to: [10, 11, 2], name: 'out_shaft' }));
const shaftB = shaftStub().map((c) => ({ ...c, from: [6, 7, 30], to: [10, 11, 42], name: 'in_shaft' }));
outliner.push(group('spring_engine', [8, 8, 16], ENG, [
  ...at(engineFront, ENG), ...at(engineBack, ENG, [0, 0, 16]),
  group('out_shaft', [8, 9, 0], ENG, at(shaftF, ENG)), group('in_shaft', [8, 9, 32], ENG, at(shaftB, ENG)),
  ...COILS.map((_, k) => group('front_coil_' + k, [8, 9, 1.5], ENG, at(frontWin.coil[k], ENG))),
  ...COILS.map((_, k) => group('back_coil_' + k, [8, 9, 30.5], ENG, at(backWin.coil[k], ENG, [0, 0, 16]))),
  group('escape_wheel', [15.75, 9, 8], ENG, at(escapeWheel, ENG)),
  group('anchor', [17.25, 14, 8], ENG, at(anchor, ENG)),
  group('ratchet', [15.75, 9, 24], ENG, at(ratchet, ENG, [0, 0, 16])),
  group('pawl', [17.25, 14, 18.25], ENG, at(pawl, ENG, [0, 0, 16])),
  ...runeBand(2, 14).map((r, i) => group('runes_front_' + (i + 1), [8, 16.25, 8], ENG, at(r, ENG))),
  ...runeBand(2, 14).map((r, i) => group('runes_back_' + (i + 1), [8, 16.25, 24], ENG, at(r, ENG, [0, 0, 16]))),
]));
// 3. the Mainspring item, standing up as a flat sprite
const MS = [-40, 0, 0];
outliner.push(group('mainspring_item', [8, 8, 8], MS, at([C('mainspring', [0, 0, 8], [16, 16, 8], { north: K + 'mainspring', south: K + 'mainspring', all: null }, { uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] }, shade: false })], MS)));

// ---------- clips ----------
// approved sounds (design/sound/clockwork-juggernaut.md): each clip's cues become sound keyframes; the build stops if a
// cued sound has no real file
const SOUND_DIR = path.resolve(__dirname, '../sound/clockwork-juggernaut');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'clockwork-juggernaut.cues.json'), 'utf8'));
const sounds = (name) => (CUES.clips[name] || []).map(([t, id]) => {
  const file = path.join(SOUND_DIR, 'real', id + '.ogg');
  if (!fs.existsSync(file)) throw new Error(name + ': no real sound for ' + id);
  return [t, CUES.namespace + '.' + id, file.split(path.sep).join('/')];
});
const level = (f) => Math.min(3, Math.floor(f * 4)); // coil frame 0..3 for a wind level 0..1
const shown = (on) => (on ? SHOW : HIDE);
const coilTracks = (fAt) => Object.fromEntries(['front', 'back'].flatMap((e) => COILS.map((_, k) => [`${e}_coil_${k}`, { scale: (s) => shown(level(fAt(s)) === k) }])));
const runeTracks = (fAt) => Object.fromEntries(['front', 'back'].flatMap((e) => [1, 2, 3, 4].map((k) => [`runes_${e}_${k}`, { scale: (s) => shown(Math.round(fAt(s) * 4) === k) }])));
// the ritual: the shaft winds the key for 6 s (30 s in game), faster as it goes; the rune ring lights; the pawl clicks
clip('ritual_winding', 6, 'loop', {
  key: { rotation: (s) => [0, 0, -(90 * s + 40 * s * s)] },
  key_runes: { scale: (s) => (s > 0.6 && (s < 5.2 || (s * 6) % 1 < 0.5) ? SHOW : HIDE) },
}, {}, sounds('ritual_winding'));
// the Spring Engine winding up (the ratchet turns and its pawl clicks; the coil tightens; the runes rise) and running down
const L = 8;
clip('engine_winding', L, 'loop', {
  ...coilTracks((s) => s / L), ...runeTracks((s) => s / L),
  in_shaft: { rotation: (s) => [0, 0, 180 * s] },
  ratchet: { rotation: (s) => [-(Math.floor(s * 3) * 30 + 30 * smooth(Math.min(1, (s * 3 % 1) * 4))), 0, 0] },
  pawl: { rotation: (s) => [8 * Math.max(0, 1 - (s * 3 % 1) * 5), 0, 0] },
}, {}, sounds('engine_winding'));
clip('engine_unwinding', L, 'loop', {
  ...coilTracks((s) => 1 - s / L), ...runeTracks((s) => 1 - s / L),
  out_shaft: { rotation: (s) => [0, 0, 360 * s] },
  escape_wheel: { rotation: (s) => [Math.floor(s * 4) * 30, 0, 0] },
  anchor: { rotation: (s) => [(Math.floor(s * 4) % 2 ? 12 : -12), 0, 0] },
}, {}, sounds('engine_unwinding'));
write(OUT, outliner);
fs.writeFileSync(path.resolve(__dirname, 'mainspring-icon.png'), ICON);
module.exports = { T, keyBase, keyRunes, keyTurning, engineFront, engineBack, escapeWheel, anchor, ratchet, pawl, frontWin, backWin, runeBand, COILS, ICON };
