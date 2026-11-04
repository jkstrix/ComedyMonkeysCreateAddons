// The Trade Master's recipe items -> design/models/trade-master-items.bbmodel (Generic Model preview), to the brief
// design/briefs/trade-master-items.md: the Netherite Sheet, the Trade Ledger and the incomplete Trade Ledger (the
// sequenced assembly's in-between item). Each is a 3D item model in the game's model space (x 0 west -> 16 east, y up,
// z 0 north -> 16 south), side by side along x in the preview.
const path = require('path');
const { C, textureSet, hash, preview } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'trade-master-items.bbmodel');

// ---------- palette ----------
const WALNUT = ['#4e3626', '#634532', '#6f4d36']; // the house walnut
const BRASS = ['#8f7638', '#b08f45'], BRASS_DARK = '#6e5a2a', BRASS_HI = '#c9a75a'; // bot brass
const PAPER = '#8a8170', RULE = '#7a7262';
const EMERALD = '#4f9a68', EMERALD_HI = '#6fb184';
const TUBE = ['#a8656d', '#c2848b']; // a muted rose bulb, like Create's electron tube
const ramp = (arr, x, y, s) => { const r = hash(x, y, s); return r < 0.2 ? arr[0] : r < 0.85 ? arr[1] : arr[arr.length - 1]; };
const brass = (x, y, s) => (hash(x, y, s) < 0.6 ? BRASS[0] : BRASS[1]);

// ---------- textures (painted in block coordinates: a face's UV is where it sits) ----------
const T = textureSet(); T.file = '';
// the Netherite Sheet is a flat icon like Create's iron, brass and golden sheets (user, 2026-11-04): Create's brass sheet
// pixel for pixel, each of its nine shades (dark to light) swapped for the vanilla netherite ingot's, so the outline,
// the pressed sheen and the highlights sit where Create's do
const SHEET_PIXELS = [
  '................', '................', '................', '................',
  '........cc......', '.....cccffb.....', '..cccfgfgffb....', '.chhggggfgffb...',
  '.aeihhgggfgffb..', '..aeihhggggggga.', '...afihhgghhdca.', '....afihhdccaaa.',
  '.....afccaaa....', '......aaa.......', '................', '................',
];
const SHEET_SHADES = { a: '#241e1f', b: '#31292a', c: '#3c3232', d: '#4c4143', e: '#4d494d', f: '#5a575a', g: '#656163', h: '#737173', i: '#8c888a' };
T.paint('netherite_sheet', (x, y) => SHEET_SHADES[SHEET_PIXELS[y][x]] || null);
// the ledger's cover: walnut, brass corner caps; the finished one has the emblem, a brass cog round the emerald
const cornerCap = (x, y) => (x <= 4 || x >= 12) && (y <= 2 || y >= 13);
const coverField = (x, y) => (cornerCap(x, y) ? (x === 4 || x === 12 || y === 2 || y === 13 ? BRASS_DARK : brass(x, y, 3)) : ramp(WALNUT, x, y, 4));
T.paint('trade_ledger_cover_plain', coverField);
T.paint('trade_ledger_cover', (x, y) => {
  const d = Math.hypot(x + 0.5 - 8.25, y + 0.5 - 8), a = Math.atan2(y + 0.5 - 8, x + 0.5 - 8.25);
  if (d >= 2 && d < 3) return y + 0.5 < 8 ? BRASS[1] : BRASS[0];
  const tooth = Math.abs(Math.sin(4 * a)) < 0.45;
  if (d >= 3 && d < 3.7 && tooth) return BRASS_DARK;
  return coverField(x, y);
});
T.paint('trade_ledger_edge', (x, y) => (hash(x, y, 5) < 0.6 ? WALNUT[0] : WALNUT[1]));
// the spine: walnut with two brass bands
T.paint('trade_ledger_spine', (x, y) => (x === 4 || x === 11 ? brass(x, y, 6) : ramp(WALNUT, x, y, 7)));
// the page edges: paper with ruled lines a pixel apart
T.paint('trade_ledger_pages', (x, y) => (y % 2 === 1 ? RULE : PAPER));
T.paint('brass', (x, y) => brass(x, y, 8));
T.paint('brass_dark', (x, y) => (hash(x, y, 9) < 0.5 ? BRASS_DARK : BRASS[0]));
T.paint('emerald', (x, y) => ((x + y) % 7 === 0 ? EMERALD_HI : EMERALD));
T.paint('tube', (x, y) => ((x + y) % 5 === 0 ? TUBE[1] : TUBE[0]));

// ---------- geometry ----------
const K = 'tm:';
// Netherite Sheet: a flat icon (item/generated in game); in the preview, a sprite standing up like Create's would
const sheet = [C('sheet_icon', [0, 0, 8], [16, 16, 8], { north: K + 'netherite_sheet', south: K + 'netherite_sheet', all: null },
  { uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] }, shade: false })];

// Trade Ledger: a closed book lying flat, spine on the west, 11 x 13 px, 4 px thick, the pages recessed half a pixel
function book(cover) {
  const edge = K + 'trade_ledger_edge';
  return [
    C('cover_bottom', [3, 0, 1.5], [13.5, 0.5, 14.5], { up: null, down: K + 'trade_ledger_cover_plain', west: null, all: edge }),
    C('pages', [3, 0.5, 2], [13, 3.5, 14], { north: K + 'trade_ledger_pages', south: K + 'trade_ledger_pages', east: K + 'trade_ledger_pages', all: null }),
    C('cover_top', [3, 3.5, 1.5], [13.5, 4, 14.5], { up: K + cover, west: null, all: edge }),
    C('spine', [2, 0, 1.5], [3, 4, 14.5], { west: K + 'trade_ledger_spine', east: null, all: edge }),
  ];
}
const ledger = [
  ...book('trade_ledger_cover'),
  // the emerald set in the cog emblem, raised half a pixel
  C('gem', [7.5, 4, 7.25], [9, 4.5, 8.75], { all: K + 'emerald' }),
  // the clasp: a tab on the cover and a strap down over the open edge
  C('clasp_tab', [12, 4, 7.25], [14, 4.25, 8.75], { down: null, all: K + 'brass' }),
  C('clasp_strap', [13.5, 1.75, 7.25], [14, 4, 8.75], { west: null, all: K + 'brass' }),
  // the electron tube for a bookmark, poking out of the top edge between the pages
  C('tube_base', [9.5, 1.5, 0.75], [10.5, 2.5, 2], { south: null, all: K + 'brass_dark' }),
  C('tube_bulb', [9.6, 1.6, -0.5], [10.4, 2.4, 0.75], { south: null, all: K + 'tube' }),
];
// the incomplete ledger: the plain book, with the precision mechanism's brass cog laid on it (the assembly's first step)
const incomplete = [
  ...book('trade_ledger_cover_plain'),
  C('cog_a', [6.75, 4, 6.5], [9.75, 4.75, 9.5], { down: null, all: K + 'brass' }),
  C('cog_b', [6.76, 4.01, 6.51], [9.74, 4.74, 9.49], { down: null, all: K + 'brass_dark' }, { rot: ['y', 45, [8.25, 4.375, 8]] }),
];

// ---------- preview ----------
const { cube, group, write } = preview('trade-master-items', { [K]: T });
const at = (list, off) => list.map((c) => cube(c, off));
const SHEET = [0, 0, 0], LEDGER = [20, 0, 0], INCOMPLETE = [40, 0, 0];
write(OUT, [
  group('netherite_sheet', [8, 0, 8], SHEET, at(sheet, SHEET)),
  group('trade_ledger', [8, 0, 8], LEDGER, at(ledger, LEDGER)),
  group('incomplete_trade_ledger', [8, 0, 8], INCOMPLETE, at(incomplete, INCOMPLETE)),
]);

module.exports = { T, sheet, ledger, incomplete };
