// Wellspring and Incinerator redesign generator -> design/models/wellspring-incinerator.bbmodel (Generic Model preview).
// To design/briefs/wellspring.md and incinerator.md. Built in the game's model space.
//
// Wellspring: a muted copper tank, no facing: slim copper corner posts and riveted steel bands top and bottom round big
// glass windows on all four sides (the renderer's fluid box is x/z 2.05-13.95, y 3-13), a copper lid with six sockets
// (generation upgrades along the north row, from the blockstate; capacity upgrades along the south row, drawn by the
// renderer) clear of the middle, where a pipe attaches.
// Incinerator: a steel furnace, front north: a recessed firebox mouth behind a heavy grate (the flames spawn there),
// riveted sides, a hot plate top flush at y 16 that glows when lit, with the embers.
const path = require('path');
const { P, C, hash, mot, edge, FIELD, HI, LO, textureSet, painters, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'wellspring-incinerator.bbmodel');
const STEEL = ['#2f343b', '#3c424a', '#4a5059'], RIVET = '#6b7178';
// Create's own copper (its fluid tank and copper casing), worn as clean bevelled plates rather than mottle
const CU = { edge: '#904931', shade: '#9a5038', mid: '#a75a40', field: '#b26247', lit: '#c26b4c', hi: '#c87456', rivet: '#d67b5b' };
const IRON = ['#444140', '#4b4948', '#5e5b5a'];
// a copper plate: dark outer pixel, a lit top-left bevel and a shaded bottom-right one, a flat field
const plate = (x, y) => {
  const e = edge(x, y);
  if (e === 0) return CU.edge;
  if (e === 1) return x === 1 || y === 1 ? CU.hi : CU.shade;
  return CU.field;
};
const RIVETS = new Set([1, 5, 10, 14]);
// near-flat steel with a bevel and rivets: crisp, not speckled
const riveted = (x, y, s, rivet = RIVET) => {
  const e = edge(x, y);
  if (e === 0) return x === 0 || y === 0 ? HI : LO;
  // four rivets per edge, symmetric (1, 5, 10, 14)
  if (e === 1 && RIVETS.has(x === 1 || x === 14 ? y : x)) return rivet;
  return FIELD(x, y, s);
};

// ---------- Wellspring textures (wellspring_<key>) ----------
const WS = textureSet(); WS.file = 'wellspring_';
// the posts: two columns of copper (lit, shade) sampled with uv [7, 3, 9, 13], with the 25 / 50 / 75 % marks
// (y 5.5, 8, 10.5 = v 10, 8, 5) as small dark ticks on the lit column
WS.paint('post', (x, y) => (x === 7 ? (y === 10 || y === 8 || y === 5 ? CU.shade : CU.lit) : CU.mid));
// the bands' sides: copper strap plates with a row of bright rivets
WS.paint('band', (x, y) => {
  if (y === 13) return CU.hi;
  if (y === 15) return CU.edge;
  if (y === 14) return x % 4 === 1 ? CU.rivet : CU.field;
  if (y === 0) return CU.edge;
  if (y === 2) return CU.shade;
  if (y === 1) return x % 4 === 1 ? CU.rivet : CU.field;
  return CU.field;
});
// glass: one small glint high on each window (the house glints read as grey blobs against bright fluid)
WS.paint('glass', (x, y) => ((x === 4 && y === 4) || (x === 5 && y === 3) ? '#7d8d94' : null));
WS.paint('steel', (x, y) => FIELD(x, y, 118));
WS.paint('dark', painters.dark());
WS.paint('copper', plate);
// the lid: a copper plate with six sockets on the pixel grid: a 2x2 dark hole in a 4x4 iron collar with its corners
// left off so it reads round (north row z 4, south row z 12, x 3 / 8 / 13); a module covers the whole collar
const SOCKETS = { gen: [[3, 4], [8, 4], [13, 4]], cap: [[3, 12], [8, 12], [13, 12]] };
WS.paint('lid', (x, y) => {
  for (const [sx, sz] of [...SOCKETS.gen, ...SOCKETS.cap]) {
    const dx = x + 0.5 - sx, dz = y + 0.5 - sz;
    if (Math.abs(dx) < 1 && Math.abs(dz) < 1) return '#1f2327';
    if (Math.abs(dx) < 2 && Math.abs(dz) < 2 && !(Math.abs(dx) > 1 && Math.abs(dz) > 1)) return dx < 0 || dz < 0 ? IRON[0] : IRON[2];
  }
  return plate(x, y);
});
WS.paint('bottom', plate);
// the generation caps' lenses, one per fluid (muted, unlit)
const LENS = { water: ['#2f5a7e', '#3f6f96'], lava: ['#8a3a1e', '#c06c2c'], honey: ['#8f6a2a', '#b08a3a'], chocolate: ['#3b2418', '#5a3a26'] };
// a module's top, painted 4x4 (uv [0, 0, 4, 4]): an iron cap with a 2x2 lens
for (const [src, [d, l]] of Object.entries(LENS)) WS.paint(`cap_${src}`, (x, y) => (x >= 1 && x <= 2 && y >= 1 && y <= 2 ? (x === 1 && y === 1 ? l : d) : x === 0 || y === 0 ? IRON[2] : IRON[0]));
// the capacity canister: 2 px of copper side (v 14-15) on an iron foot, and a 4x4 copper top
// (uv [0, 0, 4, 4]) with a dark filler cap
WS.paint('canister', (x, y) => (y === 15 ? IRON[1] : CU.field));
WS.paint('canister_top', (x, y) => (x >= 1 && x <= 2 && y >= 1 && y <= 2 ? (x === 1 && y === 1 ? IRON[2] : IRON[0]) : x === 0 || y === 0 ? CU.hi : CU.shade));
// preview only: the fluids the renderer draws
for (const [src, c] of Object.entries({ water: '#3f6fae', lava: '#c76a2a', honey: '#c08a2a', chocolate: '#5a3a26' })) WS.paint(`fluid_${src}`, () => c);

// ---------- the Wellspring ----------
const w = (k) => 'ws:' + k;
function wellspring(source, gens) {
  const out = [
    C('band_bottom', [0, 0, 0], [16, 3, 16], { all: w('band'), up: w('dark'), down: w('bottom') }),
    C('band_top', [0, 13, 0], [16, 16, 16], { all: w('band'), up: w('lid'), down: w('dark') }, { uv: { up: [0, 0, 16, 16] } }),
  ];
  for (const [x, z] of [[0, 0], [14, 0], [0, 14], [14, 14]]) out.push(C(`post_${x}_${z}`, [x, 3, z], [x + 2, 13, z + 2], { all: w('post'), up: null, down: null }, { uv: { north: [7, 3, 9, 13], south: [7, 3, 9, 13], east: [7, 3, 9, 13], west: [7, 3, 9, 13] } }));
  // glass on all four sides, a quarter pixel in from the face
  out.push(C('glass_n', [2, 3, 0.25], [14, 13, 0.25], { north: w('glass'), south: w('glass'), all: null }, { shade: false }));
  out.push(C('glass_s', [2, 3, 15.75], [14, 13, 15.75], { north: w('glass'), south: w('glass'), all: null }, { shade: false }));
  out.push(C('glass_w', [0.25, 3, 2], [0.25, 13, 14], { east: w('glass'), west: w('glass'), all: null }, { shade: false }));
  out.push(C('glass_e', [15.75, 3, 2], [15.75, 13, 14], { east: w('glass'), west: w('glass'), all: null }, { shade: false }));
  // the fitted generation upgrades: a steel cap with the fluid's lens in each of the first `gens` north sockets
  for (let i = 0; i < gens; i++) {
    const [x, z] = SOCKETS.gen[i];
    out.push(C(`gen_${i}`, [x - 2, 16, z - 2], [x + 2, 17, z + 2], { all: w('steel'), up: w(`cap_${source}`), down: null }, { uv: { up: [0, 0, 4, 4] } }));
  }
  return out;
}
// one capacity canister, modelled in the first south socket; the renderer moves it 5 px along x to the others
const canister = () => [C('canister', [SOCKETS.cap[0][0] - 2, 16, SOCKETS.cap[0][1] - 2], [SOCKETS.cap[0][0] + 2, 18, SOCKETS.cap[0][1] + 2], { all: w('canister'), up: w('canister_top'), down: null }, { uv: { up: [0, 0, 4, 4] } })];
const fluid = (src, f) => [C('fluid', [2.05, 3, 2.05], [13.95, 3 + 10 * f, 13.95], { all: w(`fluid_${src}`), down: null })];

// ---------- Incinerator textures (incinerator_<key>) ----------
const IN = textureSet(); IN.file = 'incinerator_';
const EMBER = ['#5a2a18', '#a4521f', '#c06c2c', '#d08a3e'];
IN.paint('side', painters.side('andesite', (x, y) => ((x === 4 || x === 11) && (y === 4 || y === 11) ? RIVET : null)));
IN.paint('steel', (x, y) => riveted(x, y, 120));
IN.paint('trim', (x, y) => mot('andesite', x, y, 121));
IN.paint('grate', (x, y) => (y % 3 === 0 ? '#1f2327' : '#2a2f35'));
IN.paint('ash', (x, y) => (hash(x, y, 122) > 0.8 ? '#3a3533' : hash(x, y, 123) > 0.6 ? '#2a2624' : '#1f1c1b'));
// embers: hottest low on the bed, cooling upward, with a 2x2 ordered dither between bands (no random speckle)
const DITHER = [[0, 0.5], [0.75, 0.25]];
const band = (v, x, y) => { const b = Math.floor(v), t = v - b; return Math.min(3, b + (t > DITHER[y & 1][x & 1] ? 1 : 0)); };
IN.paint('ember', (x, y) => EMBER[band(Math.max(0, 3.2 - (15 - y) * 0.22 - Math.abs(x - 7.5) * 0.08), x, y)]);
IN.paint('ember_bed', (x, y) => EMBER[band(Math.max(0, 3.1 - Math.abs(x - 7.5) * 0.25 - y * 0.05), x, y)]);
// the hot plate's centre (x/z 2-14 of the top): a round cast hob in a dark seat. Cold: dark iron with a faint ring;
// lit: glowing from the middle out, brightest amber at the centre, a dull red at its edge
const hob = (lit) => (x, y) => {
  const r = Math.hypot(x - 5.5, y - 5.5);
  if (r > 5.9) return '#1f2327';
  if (r > 5.2) return lit ? EMBER[0] : '#2a2f35';
  if (!lit) return r > 3.6 && r < 4.3 ? '#2a2f35' : '#24282d';
  return EMBER[band(Math.max(0, 3.4 - r * 0.62), x, y)];
};
// the hot plate's rim strips: riveted steel
IN.paint('rim', (x, y) => riveted(x, y, 126));
IN.paint('top', hob(false));
IN.paint('top_lit', hob(true));
IN.paint('bottom', (x, y) => riveted(x, y, 127));

// ---------- the Incinerator ----------
const n = (k) => 'in:' + k;
// the mouth: x 4-12, y 3-10, 4 px deep; lit swaps the ash and the back wall for embers (full-bright)
function incinerator(lit) {
  const S = { east: n('side'), west: n('side'), south: n('side') };
  // the parts that glow when it's lit are their own thin elements, so only they go full-bright
  const hot = (o = {}) => (lit ? { glow: true, ...o } : o);
  return [
    C('back', [0, 0, 4], [16, 15.75, 16], { ...S, north: n('steel'), down: n('bottom'), up: null }),
    C('front_bottom', [0, 0, 0], [16, 3, 4], { north: n('steel'), east: n('side'), west: n('side'), up: n('ash'), down: n('bottom') }),
    C('front_top', [0, 10, 0], [16, 15.75, 4], { north: n('steel'), east: n('side'), west: n('side'), down: n('steel'), up: null }),
    C('front_left', [12, 3, 0], [16, 10, 4], { north: n('steel'), east: n('side'), west: n('steel'), up: null, down: null }),
    C('front_right', [0, 3, 0], [4, 10, 4], { north: n('steel'), west: n('side'), east: n('steel'), up: null, down: null }),
    // the hot plate: the top quarter pixel, flush at y 16; a steel rim round a round hob, and only the hob glows
    C('rim_n', [0, 15.75, 0], [16, 16, 2], { up: n('rim'), north: n('steel'), east: n('side'), west: n('side'), down: null, south: null }),
    C('rim_s', [0, 15.75, 14], [16, 16, 16], { up: n('rim'), south: n('side'), east: n('side'), west: n('side'), down: null, north: null }),
    C('rim_w', [0, 15.75, 2], [2, 16, 14], { up: n('rim'), west: n('side'), all: null }),
    C('rim_e', [14, 15.75, 2], [16, 16, 14], { up: n('rim'), east: n('side'), all: null }),
    C('hot_plate', [2, 15.75, 2], [14, 16, 14], { up: n(lit ? 'top_lit' : 'top'), all: null }, hot({ uv: { up: [0, 0, 12, 12] } })),
    // the fire in the mouth: its back wall and bed, ash when cold and embers when lit
    C('fire_back', [4, 3, 3.75], [12, 10, 3.75], { north: n(lit ? 'ember' : 'ash'), all: null }, hot()),
    C('fire_bed', [4, 3.25, 0.5], [12, 3.25, 3.75], { up: n(lit ? 'ember_bed' : 'ash'), all: null }, hot()),
    // the grate across the mouth: four heavy bars and a crossbar, just inside the face
    ...[5, 7, 9, 11].map((x) => C(`bar_${x}`, [x - 0.5, 3.25, 0.5], [x + 0.5, 10, 1.25], { all: n('grate') })),
    C('crossbar', [4, 6, 0.25], [12, 7, 1], { all: n('grate') }),
    // andesite trim round the mouth
    C('trim_top', [3.5, 10, -0.25], [12.5, 10.75, 0], { all: n('trim'), south: null }),
    C('trim_bottom', [3.5, 2.25, -0.25], [12.5, 3, 0], { all: n('trim'), south: null }),
  ];
}

// ---------- preview ----------
const { cube, group, clip, write } = preview('wellspring-incinerator', { 'ws:': WS, 'in:': IN });
const cubes = (list, off) => list.map((c) => cube(c, off));
const outliner = [];
// Wellsprings: source, generation upgrades, capacity upgrades, fill
const WELLS = [['none', 0, 0, 0], ['water', 1, 0, 0.5], ['lava', 2, 2, 1], ['honey', 3, 3, 0.25], ['chocolate', 1, 1, 0.75]];
WELLS.forEach(([src, gens, caps, f], i) => {
  const off = [i * 22, 0, 0];
  const kids = [...cubes(wellspring(src === 'none' ? 'water' : src, gens), off)];
  if (f > 0) kids.push(...cubes(fluid(src, f), off));
  for (let k = 0; k < caps; k++) kids.push(...cubes(canister().map((c) => ({ ...c, from: [c.from[0] + k * 5, c.from[1], c.from[2]], to: [c.to[0] + k * 5, c.to[1], c.to[2]] })), off));
  outliner.push(group(`wellspring_${src}`, [8, 0, 8], off, kids));
});
// Incinerators: cold, lit, and lit under a Create fluid tank (a stand-in block)
for (const [name, lit, off] of [['incinerator_cold', false, [0, 0, 34]], ['incinerator_lit', true, [22, 0, 34]], ['incinerator_boiler', true, [44, 0, 34]]]) {
  const kids = cubes(incinerator(lit), off);
  if (name === 'incinerator_boiler') kids.push(...cubes([C('tank', [0, 16, 0], [16, 32, 16], { all: 'create:block/fluid_tank', up: 'create:block/fluid_tank_top', down: 'create:block/fluid_tank_top' })], off));
  outliner.push(group(name, [8, 0, 8], off, kids));
}
write(OUT, outliner);

module.exports = { WS, IN, SOCKETS, wellspring, canister, incinerator };
