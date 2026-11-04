// The Trade Board -> design/models/trade-board.bbmodel (Generic Model preview), to the brief design/briefs/trade-board.md:
// a plain flat board hung on a container's face like an item frame, for Create's Factory Gauges (user, 2026-11-04: "the
// hatch looks silly and isn't required"; it replaces the Trade Hatch). A gauge block takes four panels on a face, 6 x 6 px
// each with a 1 px margin (Create's factory_gauge/panel.json), the same as on a Packager's side, so the board's 1 px rim
// and the seam between the panels fall exactly in those margins. Board space: the container's face at z 16, the board in
// front.
const path = require('path');
const { C, textureSet, hash, preview } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'trade-board.bbmodel');

// ---------- palette: the Trade Bot's own (trade-bot.gen.js), on its recipe's andesite ----------
const TB = { andesite: ['#6b6e6a', '#7c7f79', '#8a8c86'], steel: ['#2f343b', '#3c424a', '#4a5059'], brass: ['#8f7638', '#b08f45'] };
const BRASS_DARK = '#6e5a2a', STEEL_HI = '#5a616b', SEAM = '#5b5e5a';
const tbMot = (mat, x, y, s) => { const p = TB[mat], r = hash(x, y, s); return r < 0.18 ? p[0] : r < 0.84 ? p[1] : p[2]; };

// ---------- textures (trade_board_<key>), painted in face coordinates (u across, v down) ----------
const T = textureSet(); T.file = 'trade_board_';
// the board: one of his andesite panels, a pressed seam between the four gauge places (in their 2 px margin)
T.paint('board', (u, v) => ((u === 7 || v === 7) && u > 0 && v > 0 && u < 15 && v < 15 ? SEAM : tbMot('andesite', u, v, 2)));
T.paint('board_edge', (u, v) => tbMot('andesite', u, v, 3));
// the rim: his dark steel, lit along its top and left like his lid plates, a dark brass bolt in each corner
T.paint('rim', (u, v) => {
  if ((u === 0 || u === 15) && (v === 0 || v === 15)) return BRASS_DARK;
  if (v === 0 || u === 0) return STEEL_HI;
  return tbMot('steel', u, v, 4);
});
T.paint('rim_edge', (u, v) => (hash(u, v, 5) > 0.5 ? TB.steel[0] : TB.steel[1]));

// ---------- geometry ----------
const K = 'tb:';
const t = (k) => K + k;
const D = 16; // the container's face
const board = [C('board', [0, 0, D - 0.75], [16, 16, D], { north: t('board'), south: null, all: t('board_edge') })];
// the rim, half a pixel proud of the board, round its edge: four bars that meet, not a plate behind
const R = { north: t('rim'), south: null, all: t('rim_edge') };
const z0 = D - 1.25, z1 = D - 0.75;
const rim = [
  C('rim_top', [0, 15, z0], [16, 16, z1], R),
  C('rim_bottom', [0, 0, z0], [16, 1, z1], R),
  C('rim_left', [0, 1, z0], [1, 15, z1], R),
  C('rim_right', [15, 1, z0], [16, 15, z1], R),
];

// Create's Factory Gauges, for the preview: a panel is 6 x 6 x 2 px with a 1 px margin, in each quarter of the face
const PANEL = 'create:block/factory_panel';
const gauge = (n, x, y) => C('gauge_' + n, [x + 1, y + 1, D - 2], [x + 7, y + 7, D], {
  north: PANEL, south: null, all: PANEL,
}, { uv: { north: [1, 1, 7, 7], east: [0, 1, 2, 7], west: [6, 1, 8, 7], up: [1, 0, 7, 2], down: [1, 6, 7, 8] }, frot: { north: 180 } });
const gauges = [gauge(0, 0, 0), gauge(1, 8, 0), gauge(2, 0, 8), gauge(3, 8, 8)];
// the containers, for the preview: barrels, behind their boards
const barrel = (n) => [C('barrel_' + n, [0, 0, 16], [16, 16, 32], { north: 'minecraft:block/barrel_top', south: 'minecraft:block/barrel_bottom', all: 'minecraft:block/barrel_side' })];

// ---------- preview ----------
const { cube, group, write } = preview('trade-board', { [K]: T });
const at = (list, off) => list.map((c) => cube(c, off));
const IN = [0, 0, 0], OUT_ = [20, 0, 0], TWO = [40, 0, 0]; // bare, full (four gauges), and two gauges, side by side
const out = [
  group('board_bare', [8, 8, 15], IN, [...at(board, IN), ...at(rim, IN)]),
  group('barrel_bare', [8, 8, 24], IN, at(barrel(0), IN)),
  group('board_full', [8, 8, 15], OUT_, [...at(board, OUT_), ...at(rim, OUT_), ...at(gauges, OUT_)]),
  group('barrel_full', [8, 8, 24], OUT_, at(barrel(1), OUT_)),
  group('board_two', [8, 8, 15], TWO, [...at(board, TWO), ...at(rim, TWO), ...at([gauges[0], gauges[3]], TWO)]),
  group('barrel_two', [8, 8, 24], TWO, at(barrel(2), TWO)),
];
write(OUT, out);
module.exports = { T, board, rim };
