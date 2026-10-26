// Battery and Industrial Battery generator -> design/models/battery.bbmodel (Generic Model preview with clips), to the brief
// design/briefs/battery.md. Built on the family kit in the game's model space (front north, z 0). The Industrial Battery is
// made of 16 px tiles by position (corner, edge, front middle; bottom, middle and top layer), so every piece exports as a
// block model; the fills, pulses, lamps and arcs are the renderer's parts.
const fs = require('fs');
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, SEAM, preview, SHOW, HIDE, smooth, lerp } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'battery.bbmodel');
const LAYER = 0.25; // overlays sit this far in front of what's behind them (zfight-check.js)

// ---------- textures (battery_<key>) ----------
const T = textureSet(); T.file = 'battery_';
const COPPER = ['#904931', '#9a5038', '#a75a40', '#b26247', '#c26b4c']; // Create's own copper
const CERAMIC = ['#7d786f', '#8f897e', '#9c968b'];
const CYAN = '#6fb7c4', CYAN_EDGE = '#5a9fac', CYAN_DIM = '#3f7d84', AMBER = '#d99a4a';
T.paint('plinth', (x, y) => (y === 14 ? '#8c8f88' : y === 15 ? '#5a5d58' : mot('andesite', x, y, 70)));
T.paint('plate', (x, y) => (edge(x, y) === 0 ? (x === 0 || y === 0 ? HI : LO) : FIELD(x, y, 40)));
T.paint('panel', painters.side('andesite', () => null));
T.paint('dark', painters.dark());
T.paint('andesite', painters.andesite());
const BRASS = ['#6e5a2b', '#7e6832', '#8f7638'];
T.paint('brass', (x, y) => BRASS[Math.floor(hash(x, y, 13) * 2.4)]); // the generators' mid brass, kept dark: it outlines the tubes
T.paint('copper', (x, y) => COPPER[Math.min(4, Math.floor(hash(x, y, 20) * 3) + (y < 2 ? 2 : 1))]);
T.paint('insulator', (x, y) => (hash(x, y, 62) > 0.85 ? '#2a2420' : '#241f1c'));
T.paint('backing', (x, y) => (hash(x, y, 63) > 0.85 ? '#24313a' : '#1f2a2e'));
T.paint('fill', (x) => (x === 0 || x === 15 ? CYAN_EDGE : CYAN));
T.paint('fill_dim', (x) => (x === 0 || x === 15 ? '#356a70' : CYAN_DIM));
T.paint('pulse', (x) => (x === 0 || x === 15 ? '#7cc2cd' : '#93d0da'));
T.paint('ember', (x, y) => (hash(x, y, 64) > 0.7 ? '#6a2219' : '#8a2f22'));
// a glass tube: clear, with one soft vertical highlight and a darker far edge, never white
T.paint('tubeglass', (x) => (x === 3 || x === 4 ? '#9fb3ba55' : x === 13 ? '#2a333855' : null));
T.paint('glass', painters.glass());
// the flow lamps: an up arrow (charging, cyan) and a down arrow (giving, amber), dark when off
const arrow = (up, on, col) => (x, y) => {
  const yy = up ? y : 15 - y, half = (yy - 2) * 0.55;
  if (yy >= 2 && yy <= 13 && Math.abs(x - 7.5) <= half) return on ? (Math.abs(x - 7.5) > half - 1.2 || yy === 13 ? col[1] : col[0]) : '#2c3236';
  return '#1a1d20';
};
T.paint('lamp_in_on', arrow(true, true, [CYAN, '#4f8f99']));
T.paint('lamp_in_off', arrow(true, false));
T.paint('lamp_out_on', arrow(false, true, [AMBER, '#b07a35']));
T.paint('lamp_out_off', arrow(false, false));
// the cap: a steel plate with an andesite rim; its band round the sides
T.paint('captop', (x, y) => (edge(x, y) === 0 ? mot('andesite', x, y, 80) : edge(x, y) === 1 ? (x === 1 || y === 1 ? LO : HI) : FIELD(x, y, 81)));
T.paint('capside', (x, y) => (y === 0 ? '#8c8f88' : y === 15 ? '#5a5d58' : mot('andesite', x, y, 82)));
// the tower: one near-flat field, a dark seam at the bottom of each layer, no per-block bevels (no Rubik's cube)
T.paint('tower', (x, y) => (y === 15 ? SEAM : FIELD(x, y, 83)));
T.paint('steel_light', (x, y) => mot('steelLight', x, y, 88));
T.paint('steel', (x, y) => mot('steel', x, y, 89));
T.paint('fin', (x, y) => (x === 0 ? '#5f656e' : x === 15 ? '#3c424a' : mot('steelLight', x, y, 84)));
T.paint('crowntop', (x, y) => FIELD(x, y, 86));
T.paint('crown', (x, y) => (y === 0 ? '#8c8f88' : y === 15 ? '#5a5d58' : mot('andesite', x, y, 85)));
T.paint('ceramic', (x, y) => (y % 3 === 0 ? CERAMIC[2] : hash(x, y, 7) > 0.85 ? CERAMIC[0] : CERAMIC[1]));
T.paint('cap_copper', (x, y) => COPPER[1 + Math.floor(hash(x, y, 90) * 3)]);
T.paint('cap_brass', (x, y) => mot('brass', x, y, 91));
T.paint('strike', (x, y) => (edge(x, y) === 0 ? COPPER[0] : edge(x, y) === 1 ? COPPER[1] : COPPER[2 + Math.floor(hash(x, y, 95) * 2)]));
// warning colours, muted: safety yellow, black, earth green
const SAFETY = ['#8f7428', '#a1832e', '#b39234'];
T.paint('hazard', (x, y) => (((x + y) >> 2) % 2 ? SAFETY[1 + (hash(x, y, 97) > 0.8 ? 1 : 0)] : hash(x, y, 98) > 0.85 ? '#2a2d31' : '#25282c'));
T.paint('safety', (x, y) => SAFETY[Math.floor(hash(x, y, 99) * 2.6)]);
T.paint('earth', (x, y) => ((y >> 1) % 2 ? '#4f7a3c' : SAFETY[2]));
// a lightning warning sign: a yellow triangle with a black edge and bolt, on a dark plate
T.paint('sign', (x, y) => {
  const yy = y - 1.5, half = yy * 0.52;
  const inTri = yy >= 0 && yy <= 13 && Math.abs(x - 7.5) <= half;
  if (!inTri) return edge(x, y) === 0 ? '#2a2f35' : '#1f2327';
  if (yy > 11.6 || Math.abs(x - 7.5) > half - 1.3) return '#1a1d20';
  const bolt = (yy > 3 && yy <= 7.5 && x - 7.5 > -(yy - 3) * 0.55 - 0.3 && x - 7.5 < -(yy - 3) * 0.55 + 1.4)
    || (yy > 7 && yy <= 11 && x - 7.5 > -1.2 + (yy - 7) * 0.5 - 1 && x - 7.5 < (yy - 7) * 0.5 + 0.6);
  return bolt ? '#1a1d20' : SAFETY[2];
});
// grating: a steel grid, open between the bars
T.paint('grating', (x, y) => (x % 4 === 0 || y % 4 === 0 ? (x % 4 === 0 && y % 4 === 0 ? '#5f656e' : '#4a5059') : null));
T.paint('tankside', (x, y) => (y === 0 ? '#555b63' : y === 15 ? '#2a2f35' : FIELD(x, y, 87)));
T.paint('arc', (x, y) => (hash(x, y, 92) > 0.6 ? '#a8dde5' : '#7fc6d1'));
// the Line Post (preview only): dark stained wood
T.paint('wood', (x, y) => (x % 4 === 0 ? '#3b2a1f' : mot('walnut', x, y, 93)));
T.paint('wood_end', (x, y) => (Math.hypot(x - 7.5, y - 7.5) % 3 < 1 ? '#3b2a1f' : '#5a3f2c'));
T.paint('steel_dark', (x, y) => mot('dark', x, y, 94));
T.paint('rod', (x, y) => COPPER[(x + y) % 3 === 0 ? 3 : 1]);

const t = (k) => 'bat:' + k;

// ---------- the Battery ----------
// joined: the faces ('west', 'east', 'south') where another Battery touches, which show a bus bar instead of terminal posts.
// The front connector, between the tubes, never joins (a Battery facing it would face its front)
function battery({ joined = [] } = {}) {
  const out = [];
  out.push(
    C('plinth', [0, 0, 0], [16, 2, 16], { all: t('plinth') }),
    C('body', [1, 2, 2], [15, 14, 15], { all: t('panel'), north: null, up: null, down: null }),
    C('cap', [0.5, 14, 0.5], [15.5, 16, 15.5], { all: t('capside'), up: t('captop'), down: t('capside') }),
    C('lug', [5.5, 16, 5.5], [10.5, 16.5, 10.5], { all: t('copper'), up: t('strike'), down: null }),
  );
  // the front: plates round two tube openings (x 3-5.5 and 10.5-13, y 3.5-12.5), their walls dark
  const plate = (n, f, to, faces) => out.push(C('front_' + n, f, to, { north: t('plate'), up: null, down: null, south: null, west: null, east: null, ...faces }));
  plate('l', [1, 2, 1], [3, 14, 2], { east: t('dark'), west: t('plate') });
  plate('m', [5.5, 2, 1], [10.5, 14, 2], { west: t('dark'), east: t('dark') });
  plate('r', [13, 2, 1], [15, 14, 2], { west: t('dark'), east: t('plate') });
  for (const [s, x0, x1] of [['l', 3, 5.5], ['r', 10.5, 13]]) {
    plate('top_' + s, [x0, 12.5, 1], [x1, 14, 2], { down: t('dark') });
    plate('bot_' + s, [x0, 2, 1], [x1, 3.5, 2], { up: t('dark') });
    out.push(C('tube_back_' + s, [x0, 3.5, 2], [x1, 12.5, 2], { north: t('backing'), all: null }));
    out.push(C('tube_glass_' + s, [x0, 3.5, 1.25], [x1, 12.5, 1.25], { north: t('tubeglass'), all: null }, { uv: { north: [0, 0, 16, 16] }, shade: false }));
    // the brass bezel goes round the opening, standing 0.25 px proud of the plate
    for (const [n, f, to] of [['t', [x0 - 0.5, 12.5, 0.75], [x1 + 0.5, 13, 1]], ['b', [x0 - 0.5, 3, 0.75], [x1 + 0.5, 3.5, 1]],
      ['l', [x0 - 0.5, 3.5, 0.75], [x0, 12.5, 1]], ['r', [x1, 3.5, 0.75], [x1 + 0.5, 12.5, 1]]])
      out.push(C(`bezel_${s}_${n}`, f, to, { all: t('brass'), south: null }));
  }
  // the front connector at (8, 8) between the tubes, and the two flow lamps above it
  out.push(C('terminal_front', [6, 5.5, 0.5], [10, 10.5, 1], { all: t('insulator'), south: null }));
  for (const [i, x] of [[0, 6.5], [1, 8.5]]) {
    out.push(C(`post_front_${i}`, [x, 7, 0], [x + 1, 9, 0.5], { all: t('copper'), south: null }));
    out.push(C(`collar_front_${i}`, [x - 0.4, 6.6, 0.25], [x + 1.4, 9.4, 0.5], { all: t('andesite'), south: null }));
  }
  // connectors at (8, 8) on the two sides and the back: the Lava Generator's terminal plate, or a bus bar where joined
  const sides = { west: (a, b) => [a, b], east: (a, b) => [16 - b, 16 - a] };
  for (const side of ['west', 'east']) {
    const x = sides[side], inner = side === 'west' ? 'east' : 'west';
    if (joined.includes(side)) {
      const [b0, b1] = x(0, 1);
      out.push(C(`bus_${side}`, [b0, 5, 2.5], [b1, 11, 13.5], { all: t('copper'), [inner]: null, [side]: null }));
      continue;
    }
    const [p0, p1] = x(0.5, 1), [q0, q1] = x(0, 0.5), [c0, c1] = x(0.25, 0.5);
    out.push(C(`terminal_${side}`, [p0, 5.5, 5.5], [p1, 10.5, 10.5], { all: t('insulator'), [inner]: null }));
    for (const [i, z] of [[0, 6], [1, 8.5]]) {
      out.push(C(`post_${side}_${i}`, [q0, 7, z], [q1, 9, z + 1.5], { all: t('copper'), [inner]: null }));
      out.push(C(`collar_${side}_${i}`, [c0, 6.6, z - 0.4], [c1, 9.4, z + 1.9], { all: t('andesite'), [inner]: null }));
    }
  }
  if (joined.includes('south')) out.push(C('bus_south', [2.5, 5, 15], [13.5, 11, 16], { all: t('copper'), north: null, south: null }));
  else {
    out.push(C('terminal_back', [5.5, 5.5, 15], [10.5, 10.5, 15.5], { all: t('insulator'), north: null }));
    for (const [i, xx] of [[0, 6], [1, 8.5]]) {
      out.push(C(`post_back_${i}`, [xx, 7, 15.5], [xx + 1.5, 9, 16], { all: t('copper'), north: null }));
      out.push(C(`collar_back_${i}`, [xx - 0.4, 6.6, 15.5], [xx + 1.9, 9.4, 15.75], { all: t('andesite'), north: null }));
    }
  }
  return out;
}
const TUBE = { y0: 3.5, y1: 12.5 };
// the renderer's parts: each tube's fill (up to f of the way) or its ember when empty, at 0.25 px steps in front of the backing
function tubeFill(f) {
  const out = [], top = TUBE.y0 + (TUBE.y1 - TUBE.y0) * f;
  for (const [s, x0, x1] of [['l', 3, 5.5], ['r', 10.5, 13]]) {
    if (f <= 0) out.push(C('ember_' + s, [x0, TUBE.y0, 1.75], [x1, TUBE.y0 + 1, 1.75], { north: t('ember'), all: null }, { glow: true }));
    else out.push(C('fill_' + s, [x0, TUBE.y0, 1.75], [x1, top, 1.75], { north: t('fill'), all: null }, { glow: true }));
  }
  return out;
}
const pulses = () => [C('pulse_l', [3, 0, 1.5], [5.5, 1, 1.5], { north: t('pulse'), all: null }, { glow: true }), C('pulse_r', [10.5, 0, 1.5], [13, 1, 1.5], { north: t('pulse'), all: null }, { glow: true })];
const lamps = (inOn, outOn) => [
  C('lamp_in', [6, 11, 0.75], [7.8, 12.75, 1], { north: t(inOn ? 'lamp_in_on' : 'lamp_in_off'), south: null, all: t('andesite') }, { uv: { north: [0, 0, 16, 16] }, glow: inOn }),
  C('lamp_out', [8.2, 11, 0.75], [10, 12.75, 1], { north: t(outOn ? 'lamp_out_on' : 'lamp_out_off'), south: null, all: t('andesite') }, { uv: { north: [0, 0, 16, 16] }, glow: outOn }),
];

// ---------- the Industrial Battery: tiles by position ----------
// ix, iz: 0-2 across and back (0 the front row, north); layer 0 the bottom, H the height. Faces inside the tower are left out.
const BAND = 0.5; // the plinth and crown stand this far out of the tower's faces
// Each layer is a module with its own job, at a fixed place, so a taller tower gains new kinds rather than repeating one:
// the base at the bottom, the control layer second (from 4 high), the junction in the middle (from 6 high), the crown on
// top, and plain cells everywhere else
function layerModule(layer, H) {
  if (layer === 0) return 'base';
  if (layer === H - 1) return 'crown';
  if (layer === 1 && H >= 4) return 'control';
  if (H >= 6 && layer === Math.ceil(H / 2)) return 'junction';
  return 'cells';
}
function tile(ix, iz, layer, H) {
  const out = [], bottom = layer === 0, top = layer === H - 1;
  const y0 = bottom ? 3 : 0, y1 = top ? 13 : 16;
  const W = ix === 0, E = ix === 2, N = iz === 0, S = iz === 2, frontMiddle = N && ix === 1;
  const outer = { west: W, east: E, north: N, south: S };
  const face = (dir, tex) => (outer[dir] ? tex : null);
  const kind = layerModule(layer, H);
  // a recessed cell bay on the front side tiles of the cell layers (and the control layer's east tile, beside the switch)
  const bayed = N && (W || E) && (kind === 'cells' || (kind === 'control' && E));
  if (W || E || N || S)
    out.push(C('body', [0, y0, 0], [16, y1, 16], { west: face('west', t('tower')), east: face('east', t('tower')), north: frontMiddle || bayed ? null : face('north', t('tower')),
      south: face('south', t('tower')), up: null, down: null }));
  if (bayed) {
    const [b0, b1] = W ? [5, 13.5] : [2.5, 11], c0 = 2.5, c1 = 13.5, D = 1.5;
    for (const [n, f, to] of [['l', [0, 0, 0], [b0, 16, 0]], ['r', [b1, 0, 0], [16, 16, 0]], ['t', [b0, c1, 0], [b1, 16, 0]], ['b', [b0, 0, 0], [b1, c0, 0]]])
      out.push(C('bay_skin_' + n, f, to, { north: t('tower'), all: null }));
    out.push(C('bay_back', [b0, c0, D], [b1, c1, D], { north: t('panel'), all: null }));
    out.push(C('bay_wall_l', [b0, c0, 0], [b0, c1, D], { east: t('dark'), all: null }));
    out.push(C('bay_wall_r', [b1, c0, 0], [b1, c1, D], { west: t('dark'), all: null }));
    out.push(C('bay_wall_t', [b0, c1, 0], [b1, c1, D], { down: t('dark'), all: null }));
    out.push(C('bay_wall_b', [b0, c0, 0], [b1, c0, D], { up: t('dark'), all: null }));
    // the cell module's pull handle, standing out of the bay's back
    const hc = (b0 + b1) / 2;
    out.push(C('bay_handle', [hc - 2, 7.5, 0.25], [hc + 2, 8.25, D], { all: t('andesite'), south: null }));
  }
  // the plinth and crown bands, a step out of the tower on its outer sides
  const ex0 = W ? -BAND : 0, ex1 = E ? 16 + BAND : 16, ez0 = N ? -BAND : 0, ez1 = S ? 16 + BAND : 16;
  const band = (name, b0, b1, tex, up) => out.push(C(name, [ex0, b0, ez0], [ex1, b1, ez1], { west: face('west', tex), east: face('east', tex), north: face('north', tex),
    south: face('south', tex), up, down: null }));
  if (bottom && (W || E || N || S)) band('plinth', 0, 3, t('hazard'), t('plinth'));
  if (top) band('crown', 13, 16, t('crown'), t('crowntop'));
  // corner pilasters, the full height between plinth and crown
  if ((W || E) && (N || S)) {
    const px = W ? [-BAND, 1.5] : [14.5, 16 + BAND], pz = N ? [-BAND, 1.5] : [14.5, 16 + BAND];
    out.push(C('pilaster', [px[0], y0, pz[0]], [px[1], y1, pz[1]], { all: t('andesite'), up: null, down: null,
      [W ? 'east' : 'west']: null, [N ? 'south' : 'north']: null }));
  }
  // base: a bolted access hatch on each front side tile, with a grab bar
  if (kind === 'base' && N && (W || E)) {
    const [h0, h1] = W ? [5, 12] : [4, 11];
    out.push(C('hatch', [h0, 5, -0.5], [h1, 11.5, 0], { all: t('plate'), south: null }));
    for (const [n, x, y] of [[0, h0 + 0.5, 5.5], [1, h1 - 1.5, 5.5], [2, h0 + 0.5, 10], [3, h1 - 1.5, 10]])
      out.push(C('hatch_bolt_' + n, [x, y, -0.75], [x + 1, y + 1, -0.5], { all: t('andesite'), south: null }));
    out.push(C('hatch_bar', [(h0 + h1) / 2 - 2, 7.75, -1.25], [(h0 + h1) / 2 + 2, 8.5, -0.5], { all: t('andesite'), south: null }));
  }
  if (kind === 'control' && N && W) {
    out.push(C('switch_base', [5, 3.5, -0.5], [11, 12.5, 0], { all: t('ceramic'), south: null }));
    out.push(C('switch_hinge', [7, 4.5, -1.25], [9, 6, -0.5], { all: t('copper'), south: null }));
    out.push(C('switch_jaw_l', [6.75, 10.5, -1.25], [7.25, 12, -0.5], { all: t('copper'), south: null }));
    out.push(C('switch_jaw_r', [8.75, 10.5, -1.25], [9.25, 12, -0.5], { all: t('copper'), south: null }));
    out.push(C('switch_blade', [7.25, 6, -1], [8.75, 11.5, -0.75], { all: t('copper'), south: null }));
    out.push(C('switch_handle', [7.5, 9, -2.5], [8.5, 10.25, -1], { all: t('wood'), south: null }));
    // a warning sign over it
    out.push(C('switch_sign', [6.25, 12.75, -0.75], [9.75, 15.75, -0.5], { north: t('sign'), south: null, all: t('dark') }, { uv: { north: [0, 0, 16, 16] } }));
  }
  // junction: a bolted copper bus band across the front, joining the corner strips, with a gap for the level column
  if (kind === 'junction' && N) {
    const pieces = W ? [[1.5, 16]] : E ? [[0, 14.5]] : [[0, 3.5], [12.5, 16]];
    pieces.forEach(([b0, b1], k) => {
      out.push(C(`junction_${k}`, [b0, 6.5, -0.75], [b1, 9.5, 0], { all: t('copper'), south: null }));
      for (const bx of [b0 + (b1 - b0) * 0.3, b0 + (b1 - b0) * 0.7])
        out.push(C(`junction_bolt_${k}_${Math.round(bx)}`, [bx - 0.5, 7.5, -1], [bx + 0.5, 8.5, -0.75], { all: t('andesite'), south: null }));
    });
  }
  // cooling fins down the back: 1 px ribs, 2 px apart, from the plinth to the crown (the sides carry the radiator banks)
  const finAt = [2.5, 5.5, 8.5, 11.5];
  for (const [dir, on] of [['south', S]]) {
    if (!on) continue;
    for (const u of finAt) {
      // keep clear of the pilasters at the tower's corners
      const lo = u, hi = u + 1;
      if (dir === 'south' && ((W && lo < 2) || (E && hi > 14))) continue;
      if ((dir === 'west' || dir === 'east') && ((N && lo < 2) || (S && hi > 14))) continue;
      const box = dir === 'west' ? [[-1, y0, lo], [0, y1, hi]] : dir === 'east' ? [[16, y0, lo], [17, y1, hi]] : [[lo, y0, 16], [hi, y1, 17]];
      out.push(C(`fin_${dir}_${u}`, box[0], box[1], { all: t('fin'), up: top ? t('fin') : null, down: bottom ? t('fin') : null,
        [dir === 'west' ? 'east' : dir === 'east' ? 'west' : 'north']: null }));
    }
  }
  // the level channel up the front middle column: skin either side, andesite rails, a dark backing 1 px in
  if (frontMiddle) {
    out.push(C('skin_l', [0, y0, 0], [4, y1, 0], { north: t('tower'), all: null }));
    out.push(C('skin_r', [12, y0, 0], [16, y1, 0], { north: t('tower'), all: null }));
    out.push(C('rail_l', [4, y0, -0.5], [5, y1, 1], { north: t('andesite'), west: t('andesite'), east: t('dark'), all: null }));
    out.push(C('rail_r', [11, y0, -0.5], [12, y1, 1], { north: t('andesite'), east: t('andesite'), west: t('dark'), all: null }));
    out.push(C('channel_back', [5, y0, 1], [11, y1, 1], { north: t('backing'), all: null }));
    out.push(C('channel_glass', [5, y0, 0.25], [11, y1, 0.25], { north: t('glass'), all: null }, { shade: false }));
    // brass ticks at every block's height (the bottom of each layer above the first)
    if (!bottom)
      for (const [n, x0, x1] of [['l', 3.5, 5], ['r', 11, 12.5]]) out.push(C(`tick_${n}`, [x0, 0, -0.75], [x1, 0.75, -0.5], { all: t('brass'), south: null }));
  }
  // copper bus strips up the front corners, to the crown, and a lead over the crown's lip to the bushing
  if (N && (W || E)) {
    const [s0, s1] = W ? [2, 3.5] : [12.5, 14];
    out.push(C('bus_strip', [s0, y0, -0.5], [s1, y1, 0], { north: t('copper'), west: t('copper'), east: t('copper'), all: null }));
    if (top) out.push(C('bus_lead', [s0, 17, -BAND], [s1, 17.5, 1.25], { all: t('copper'), down: null }));
  }
  if (top) {
    // a low lip round the crown's top edge
    if (N) out.push(C('lip_n', [ex0, 16, -BAND], [ex1, 17, 0.5], { all: t('andesite'), down: null, east: E ? t('andesite') : null, west: W ? t('andesite') : null }));
    if (S) out.push(C('lip_s', [ex0, 16, 15.5], [ex1, 17, 16 + BAND], { all: t('andesite'), down: null, east: E ? t('andesite') : null, west: W ? t('andesite') : null }));
    if (W) out.push(C('lip_w', [-BAND, 16, N ? 0.5 : 0], [0.5, 17, S ? 15.5 : 16], { all: t('andesite'), down: null, north: N ? null : null, south: null }));
    if (E) out.push(C('lip_e', [15.5, 16, N ? 0.5 : 0], [16 + BAND, 17, S ? 15.5 : 16], { all: t('andesite'), down: null, north: null, south: null }));
    // the bushings on the corners, and the lightning lug in the middle
    if ((W || E) && (N || S)) out.push(...bushing(W ? BUSHING_AT : 16 - BUSHING_AT, N ? BUSHING_AT : 16 - BUSHING_AT, 16, (ix + iz) % 2 ? 'brass' : 'copper', 'bushing', BUSHING_K));
    if (ix === 1 && iz === 1) out.push(C('lug', [4.5, 16, 4.5], [11.5, 16.5, 11.5], { all: t('copper'), up: t('strike'), down: null }));
  }
  return out;
}
// A ribbed ceramic bushing on an andesite collar: four discs narrowing upward with necks between, then the cable's tier cap
function bushing(cx, cz, y0, tier, name, k = 1) {
  const ring = (nm, y, w, h, tex) => {
    const a = w / 2, n = w >= 4 ? 1 : 0.5;
    // a plus-shaped ring: one bar across, and the two ends of the other bar meeting it (no overlap, so no shared tops)
    return [C(nm + '_a', [cx - a, y, cz - a + n], [cx + a, y + h, cz + a - n], { all: tex }),
      C(nm + '_b', [cx - a + n, y, cz - a], [cx + a - n, y + h, cz - a + n], { all: tex, south: null }),
      C(nm + '_c', [cx - a + n, y, cz + a - n], [cx + a - n, y + h, cz + a], { all: tex, north: null })];
  };
  const out = [...ring(name + '_collar', y0, 6 * k, 1, t('andesite'))];
  let y = y0 + 1;
  BUSHING.forEach(([w, h], i) => { out.push(...ring(`${name}_${i}`, y, w * k, h * k, t('ceramic'))); y += h * k; });
  out.push(C(name + '_cap', [cx - k, y, cz - k], [cx + k, y + 1.5, cz + k], { all: t('cap_' + tier), down: null }));
  return out;
}
const BUSHING = [[5, 1.5], [3, 1], [4.5, 1.5], [3, 1], [4, 1.5], [2.5, 1], [3.5, 1.5]];
const BUSHING_K = 1.45; // the tower's bushings, scaled up from a pole's
const stackHeight = (k) => 1 + BUSHING.reduce((a, [, h]) => a + h * k, 0) + 1.5;
const BUSHING_AT = 5.5; // in from the tower's corner, clear of the crown's lip
const bushingTop = (ix, iz) => [(ix === 0 ? BUSHING_AT : 16 - BUSHING_AT) + ix * 16, 16 + stackHeight(BUSHING_K), (iz === 0 ? BUSHING_AT : 16 - BUSHING_AT) + iz * 16];
const CH = { y0: 3, y1: (H) => (H - 1) * 16 + 13 };
// the level fill and its pulse, up the channel from the plinth to the crown (renderer parts; one piece per layer in game)
const levelFill = (H, f) => {
  const top = CH.y0 + (CH.y1(H) - CH.y0) * f;
  return f <= 0 ? [C('level_ember', [21, CH.y0, 0.75], [27, CH.y0 + 1.5, 0.75], { north: t('ember'), all: null }, { glow: true })]
    : [C('level', [21, CH.y0, 0.75], [27, top, 0.75], { north: t('fill'), all: null }, { glow: true })];
};

// ---------- the substation hardware ----------
// mirror a part from the west side to the east (x -> 48 - x)
const mirrorX = (c) => ({ ...c, name: c.name + '_e', from: [48 - c.to[0], c.from[1], c.from[2]], to: [48 - c.from[0], c.to[1], c.to[2]],
  faces: Object.fromEntries(Object.entries(c.faces).map(([d, v]) => [d === 'west' ? 'east' : d === 'east' ? 'west' : d, v])) });
function substation(H) {
  const out = [], top = (H - 1) * 16;
  // radiator banks: one per run of cell layers, standing off both sides on brackets, fins between header pipes
  const runs = [];
  for (let l = 0; l < H; l++) if (layerModule(l, H) === 'cells') { if (runs.length && runs[runs.length - 1][1] === l - 1) runs[runs.length - 1][1] = l; else runs.push([l, l]); }
  const west = [];
  runs.forEach(([a, b], r) => {
    const yb = a * 16 + 2, yt = b * 16 + 14;
    west.push(C(`rad_${r}_header_b`, [-5.5, yb, 3.5], [-3.5, yb + 2, 43.5], { all: t('steel_light') }));
    west.push(C(`rad_${r}_header_t`, [-5.5, yt - 2, 3.5], [-3.5, yt, 43.5], { all: t('steel_light') }));
    for (let z = 4.5, k = 0; z < 42.5; z += 2.5, k++)
      west.push(C(`rad_${r}_fin_${k}`, [-6.5, yb + 2, z], [-2.5, yt - 2, z + 0.5], { all: t('fin'), up: null, down: null }));
    for (let l = a; l <= b; l++) for (const z of [5.5, 40.5])
      west.push(C(`rad_${r}_bracket_${l}_${z}`, [-3.5, l * 16 + 7.5, z], [0, l * 16 + 8.5, z + 1.5], { all: t('andesite'), east: null }));
  });
  // conduits up the back corners: from a gland box on the plinth to a junction box under the crown, clamped every block
  west.push(C('gland_box', [-3.5, 3, 43.5], [-0.5, 7.5, 48], { all: t('plate'), east: null }));
  west.push(C('top_box', [-3.5, top + 7.5, 43.5], [-0.5, top + 12.5, 48], { all: t('plate'), east: null }));
  west.push(C('top_box_sign', [-3.75, top + 8.5, 44.5], [-3.5, top + 11.5, 47], { west: t('sign'), all: null }, { uv: { west: [0, 0, 16, 16] } }));
  west.push(C('conduit_a', [-2.5, 7.5, 45.5], [-0.5, top + 7.5, 46.5], { all: t('steel_light'), up: null, down: null }));
  west.push(C('conduit_b', [-2, 7.5, 45], [-1, top + 7.5, 45.5], { all: t('steel_light'), up: null, down: null, south: null }));
  west.push(C('conduit_c', [-2, 7.5, 46.5], [-1, top + 7.5, 47], { all: t('steel_light'), up: null, down: null, north: null }));
  for (let l = 1; l < H; l++) if (l * 16 + 2 < top + 7.5) west.push(C(`clamp_${l}`, [-3, l * 16 + 2, 44.5], [-0.5, l * 16 + 3, 47.5], { all: t('andesite'), east: null }));
  // a warning sign on the base's side
  west.push(C('base_sign', [-0.75, 5.5, 20.5], [0, 11.5, 27.5], { west: t('sign'), all: t('dark'), east: null }, { uv: { west: [0, 0, 16, 16] } }));
  // the junction layer: a bolted busbar chamber standing out of each side, with its own sign
  for (let l = 0; l < H; l++) if (layerModule(l, H) === 'junction') {
    const y = l * 16;
    west.push(C('chamber', [-4, y + 3, 14], [0, y + 13, 34], { all: t('plate'), east: null }));
    for (const [n, f, to] of [['t', [-4.5, y + 12.5, 13.5], [-4, y + 13, 34.5]], ['b', [-4.5, y + 3, 13.5], [-4, y + 3.5, 34.5]],
      ['l', [-4.5, y + 3.5, 13.5], [-4, y + 12.5, 14]], ['r', [-4.5, y + 3.5, 34], [-4, y + 12.5, 34.5]]])
      west.push(C('chamber_flange_' + n, f, to, { all: t('andesite'), east: null }));
    west.push(C('chamber_sign', [-4.25, y + 5.5, 21], [-4, y + 10.5, 27], { west: t('sign'), all: null }, { uv: { west: [0, 0, 16, 16] } }));
  }
  out.push(...west, ...west.map(mirrorX));
  // a green-and-yellow earth strap down the east front corner, to a copper ground clamp on the plinth
  out.push(C('earth_strap', [48.5, 4.5, 0], [49, top + 13, 1], { east: t('earth'), north: t('earth'), south: t('earth'), all: null }));
  out.push(C('earth_clamp', [48.5, 3, -0.25], [49.75, 4.5, 1.25], { all: t('copper'), west: null }));
  // the back: a caged ladder from the ground to a railed catwalk under the crown
  for (const x of [19, 28]) {
    out.push(C('ladder_rail_' + x, [x, 0, 50.5], [x + 1, top + 21.5, 51.5], { all: t('steel_light'), down: null }));
    for (let l = 0; l < H; l++) out.push(C(`ladder_bracket_${x}_${l}`, [x, l * 16 + 8, 48], [x + 1, l * 16 + 8.75, 50.5], { all: t('andesite'), north: null }));
  }
  for (let y = 3, k = 0; y < top + 10.5; y += 3, k++) out.push(C('rung_' + k, [20, y, 50.75], [28, y + 0.6, 51.25], { all: t('steel_light') }));
  for (let h = 32, k = 0; h < top + 10.5; h += 6, k++) {
    out.push(C('hoop_back_' + k, [17.5, h, 55.25], [30.5, h + 0.75, 56], { all: t('safety') }));
    out.push(C('hoop_l_' + k, [17.5, h, 51.5], [18.25, h + 0.75, 55.25], { all: t('safety'), south: null }));
    out.push(C('hoop_r_' + k, [29.75, h, 51.5], [30.5, h + 0.75, 55.25], { all: t('safety'), south: null }));
  }
  if (top + 10.5 > 32) for (const x of [21, 23.6, 26.25]) out.push(C('cage_strap_' + x, [x, 32, 54.5], [x + 0.75, top + 11, 55.25], { all: t('safety'), south: null }));
  const py = top + 11;
  for (const [n, x0, x1, z0] of [['l', -0.5, 17.5, 48.5], ['r', 30.5, 48.5, 48.5], ['m', 17.5, 30.5, 48.5]])
    out.push(C('catwalk_' + n, [x0, py, z0], [x1, py + 1, n === 'm' ? 50.5 : 56.5], { up: t('grating'), down: t('grating'), all: t('steel_light'), north: null }));
  out.push(C('toe_board', [-0.5, py + 1, 56], [48.5, py + 2.5, 56.5], { south: t('hazard'), up: t('safety'), all: t('safety'), down: null }));
  for (const [n, x] of [['w', -0.5], ['e', 48]])
    out.push(C('toe_board_' + n, [x, py + 1, 48.5], [x + 0.5, py + 2.5, 56], { all: t('safety'), [n === 'w' ? 'west' : 'east']: t('hazard'), down: null, south: null }));
  for (const x of [0, 12, 17, 30.25, 35.5, 47.25]) out.push(C('post_' + x, [x, py + 2.5, 55.25], [x + 0.75, py + 9.75, 56], { all: t('safety'), down: null, up: null }));
  out.push(C('handrail_top', [0, py + 9.75, 55.5], [48, py + 10.5, 56.25], { all: t('safety') }));
  out.push(C('handrail_mid', [0.25, py + 6, 55.5], [47.75, py + 6.75, 56.25], { all: t('safety') }));
  for (const [n, x] of [['w', 0], ['e', 47.25]]) out.push(C('handrail_side_' + n, [x, py + 9.75, 48.5], [x + 0.75, py + 10.5, 55.5], { all: t('safety'), south: null }));
  out.push(C('ladder_sign', [8, 5, 49], [14, 11, 49.5], { south: t('sign'), all: t('dark'), north: null }, { uv: { south: [0, 0, 16, 16] } }));
  // on the crown: a conservator tank on two legs, across the back behind the bushings
  const ty = top + 16;
  out.push(C('tank', [12, ty + 4.5, 37], [36, ty + 9.5, 43], { all: t('tankside'), east: t('steel'), west: t('steel') }));
  out.push(C('tank_top', [12, ty + 9.5, 38], [36, ty + 10.5, 42], { all: t('tankside'), down: null }));
  out.push(C('tank_bottom', [12, ty + 3.5, 38], [36, ty + 4.5, 42], { all: t('tankside'), up: null }));
  for (const [n, x0, x1] of [['w', 11, 12], ['e', 36, 37]]) out.push(C('tank_end_' + n, [x0, ty + 5.5, 38], [x1, ty + 8.5, 42], { all: t('andesite') }));
  for (const x of [14.5, 32]) {
    out.push(C('tank_leg_' + x, [x, ty, 39.25], [x + 1.5, ty + 3.5, 40.75], { all: t('andesite'), down: null, up: null }));
    out.push(C('tank_saddle_' + x, [x - 0.5, ty + 3, 38.5], [x + 2, ty + 3.5, 41.5], { all: t('andesite'), up: null }));
  }
  return out;
}

// ---------- props: a Line Post, a Lightning Rod, a span, arcs ----------
function linePost(height) {
  const out = [];
  out.push(C('foot_a', [5, 0, 6], [11, 1.5, 10], { all: t('steel_dark'), down: null }), C('foot_b', [6, 0, 5], [10, 1.5, 11], { all: t('steel_dark'), down: null }));
  out.push(C('pole', [6.5, 1.5, 6.5], [9.5, (height - 1) * 16 + 10.5, 9.5], { all: t('wood'), down: null, up: t('wood_end') }));
  out.push(...bushing(8, 8, (height - 1) * 16 + 10.5 - 1, 'brass', 'post_insulator').slice(2));
  return out;
}
const postTie = (height) => [8, (height - 1) * 16 + 10.5 - 1 + stackHeight(1), 8];
const lightningRod = (y0) => [C('rod', [7.5, y0, 7.5], [8.5, y0 + 12, 8.5], { all: t('rod') }), C('rod_tip', [7, y0 + 12, 7], [9, y0 + 15, 9], { all: t('rod') })];
// the turn that points a segment's long (x) side from p to q, and its length (Blockbench turns X, then Y, then Z)
function aim(p, q, extra) {
  const dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2], L = Math.hypot(dx, dy, dz);
  return [(-Math.asin(dz / L) * 180) / Math.PI, (Math.atan2(dy, dx) * 180) / Math.PI, L + extra];
}
// a sagging cable between two points (preview: free rotation, as the renderer draws it)
function cable(a, b, name, tex, w = 0.6) {
  const out = [], n = 16, L = Math.hypot(b[0] - a[0], b[2] - a[2], b[1] - a[1]), sag = Math.max(4, L / 10);
  const pt = (s) => [a[0] + (b[0] - a[0]) * s, a[1] + (b[1] - a[1]) * s - 4 * sag * s * (1 - s), a[2] + (b[2] - a[2]) * s];
  for (let i = 0; i < n; i++) {
    const p = pt(i / n), q = pt((i + 1) / n), m = p.map((v, k) => (v + q[k]) / 2);
    const [yaw, pitch, len] = aim(p, q, 0.2);
    out.push(C(`${name}_${i}`, [m[0] - len / 2, m[1] - w / 2, m[2] - w / 2], [m[0] + len / 2, m[1] + w / 2, m[2] + w / 2], { all: tex },
      { rot: [['y', yaw, m], ['z', pitch, m]] }));
  }
  return out;
}
// a jagged arc from a to b: short straight pieces kinked at random either side of the line
function arc(a, b, name, seed) {
  const pts = [a];
  for (let i = 1; i < 6; i++) {
    const s = i / 6, j = (k) => (hash(i, k, seed) - 0.5) * 3;
    pts.push([a[0] + (b[0] - a[0]) * s + j(1), a[1] + (b[1] - a[1]) * s + j(2), a[2] + (b[2] - a[2]) * s + j(3)]);
  }
  pts.push(b);
  const out = [];
  for (let i = 0; i < pts.length - 1; i++) {
    const p = pts[i], q = pts[i + 1], m = p.map((v, k) => (v + q[k]) / 2);
    const [yaw, pitch, len] = aim(p, q, 0.3);
    out.push(C(`${name}_${i}`, [m[0] - len / 2, m[1] - 0.25, m[2] - 0.25], [m[0] + len / 2, m[1] + 0.25, m[2] + 0.25], { all: t('arc') }, { rot: [['y', yaw, m], ['z', pitch, m]], glow: true }));
  }
  return out;
}

// ---------- preview ----------
const { cube, group, clip, write } = preview('battery', { 'bat:': T });
const cubes = (list, off) => list.map((c) => cube(c, off));
const shifted = (list, dx, dy, dz) => list.map((c) => ({ ...c, from: [c.from[0] + dx, c.from[1] + dy, c.from[2] + dz], to: [c.to[0] + dx, c.to[1] + dy, c.to[2] + dz],
  rot: c.rot ? (Array.isArray(c.rot[0]) ? c.rot.map((r) => [r[0], r[1], [r[2][0] + dx, r[2][1] + dy, r[2][2] + dz]]) : [c.rot[0], c.rot[1], [c.rot[2][0] + dx, c.rot[2][1] + dy, c.rot[2][2] + dz]]) : undefined }));
const outliner = [];

// 1. three Batteries: empty, half, full (lamps off)
[['battery_empty', 0, 0], ['battery_half', 24, 0.5], ['battery_full', 48, 1]].forEach(([n, x, f]) => {
  const o = [x, 0, 0];
  outliner.push(group(n, [8, 0, 8], o, [...cubes(battery(), o), ...cubes(tubeFill(f), o), ...cubes(lamps(false, false), o)]));
});

// 2. a bank of three touching Batteries (bus bars between them), animated by the clips
const BANK = [[80, ['east']], [96, ['west', 'east']], [112, ['west']]];
const bankKids = [];
BANK.forEach(([x, joined], i) => {
  const o = [x, 0, 0];
  bankKids.push(group(`bank_${i}`, [8, 0, 8], o, [
    ...cubes(battery({ joined }), o),
    group(`bank_${i}_ember`, [8, TUBE.y0, 1.75], o, cubes(tubeFill(0), o)),
    group(`bank_${i}_fill`, [8, TUBE.y0, 1.75], o, cubes(tubeFill(1), o)),
    group(`bank_${i}_pulse`, [8, 0, 1.5], o, cubes(pulses(), o)),
    group(`bank_${i}_lamps_off`, [8, 12, 1], o, cubes(lamps(false, false), o)),
    group(`bank_${i}_lamp_in`, [8, 12, 1], o, cubes(lamps(true, false), o)),
    group(`bank_${i}_lamp_out`, [8, 12, 1], o, cubes(lamps(false, true), o)),
  ]));
});
outliner.push(group('bank', [8, 0, 8], [96, 0, 0], bankKids, { open: true }));

// 3. Industrial Batteries: 3 high (half full, still) and 9 high (animated), a Line Post between them with spans to both
function tower(name, H, o, f, animated) {
  const kids = [];
  for (let layer = 0; layer < H; layer++) for (let iz = 0; iz < 3; iz++) for (let ix = 0; ix < 3; ix++) {
    const list = shifted(tile(ix, iz, layer, H), ix * 16, layer * 16, iz * 16);
    if (list.length) kids.push(group(`${name}_tile_${layer}_${ix}${iz}`, [8 + ix * 16, layer * 16, 8 + iz * 16], o, cubes(list, o)));
  }
  kids.push(group(`${name}_hardware`, [24, 0, 24], o, cubes(substation(H), o)));
  if (!animated) kids.push(group(`${name}_level`, [24, CH.y0, 0.75], o, cubes(levelFill(H, f), o)));
  else {
    kids.push(group(`${name}_ember`, [24, CH.y0, 0.75], o, cubes(levelFill(H, 0), o)));
    kids.push(group(`${name}_level`, [24, CH.y0, 0.75], o, cubes(levelFill(H, 1), o)));
    kids.push(group(`${name}_pulse`, [24, 0, 0.5], o, cubes([C('pulse', [21, 0, 0.5], [27, 2, 0.5], { north: t('pulse'), all: null }, { glow: true })], o)));
    kids.push(group(`${name}_ripple`, [24, 0, 0.5], o, cubes([C('ripple', [21, 0, 0.45], [27, 8, 0.45], { north: t('pulse'), all: null }, { glow: true })], o)));
    // the rod on the lug, and an arc from its tip to each bushing (shown only by the lightning clip)
    const rodBase = (H - 1) * 16 + 16.5;
    kids.push(group(`${name}_rod`, [24, rodBase, 24], o, cubes(shifted(lightningRod(rodBase), 16, 0, 16), o)));
    const tip = [24, rodBase + 15, 24];
    const arcs = [[0, 0], [2, 0], [0, 2], [2, 2]].flatMap(([ix, iz], i) => { const b = bushingTop(ix, iz); return arc(tip, [b[0], b[1] + (H - 1) * 16, b[2]], `arc_${i}`, 7 + i); });
    kids.push(group(`${name}_arcs`, tip, o, cubes(arcs, o)));
  }
  return group(name, [24, 0, 24], o, kids, { open: animated });
}
const TX = (H) => 150 + (H - 3) * 72;
const T3 = [TX(8), 0, 0], T9 = [TX(9), 0, 0], POST = [TX(8) + 52, 0, -3.5], POST_H = 8;
for (const H of [3, 4, 5, 6, 7, 8]) outliner.push(tower('tower_' + H, H, [TX(H), 0, 0], [0, 0, 0, 0.3, 0.5, 0.65, 0.8, 0.45][H - 1] || 0.5, false));
outliner.push(tower('tower_9', 9, T9, 1, true));
const tie = postTie(POST_H), postAt = [POST[0] + tie[0], tie[1], POST[2] + tie[2]];
const bt3 = bushingTop(2, 0), bt9 = bushingTop(0, 0);
const toWorld = (o, p, H) => [o[0] + p[0], p[1] + (H - 1) * 16, o[2] + p[2]];
const spans = [...cable(toWorld(T3, bt3, 8), postAt, 'span_8', t('cap_copper')), ...cable(postAt, toWorld(T9, bt9, 9), 'span_9', t('cap_copper'))];
outliner.push(group('line_post', [8, 0, 8], POST, [...cubes(linePost(POST_H), POST), group('spans', [0, 0, 0], [0, 0, 0], cubes(spans, [0, 0, 0]))]));

// ---------- clips ----------
// approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files (a cue
// whose real file is missing stops the build, so a sound can't go missing quietly)
const SOUND = path.resolve(__dirname, '../sound/battery');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND, 'battery.cues.json'), 'utf8'));
function sounds(clipName) {
  const list = CUES.clips[clipName] || [];
  const missing = [...new Set(list.map(([, id]) => id).filter((id) => !fs.existsSync(path.join(SOUND, 'real', id + '.ogg'))))];
  if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
  return list.map(([t, id]) => [t, CUES.namespace + '.' + id, path.join(SOUND, 'real', id + '.ogg').split(path.sep).join('/')]);
}
const bankGroups = (s) => [0, 1, 2].map((i) => `bank_${i}_${s}`);
const each = (names, track) => Object.fromEntries(names.map((n) => [n, track]));
// the tubes fill in eight steps; the tower's column smoothly
const step8 = (f) => Math.round(f * 8) / 8;
const tubeScale = (f) => [1, Math.max(0.0001, step8(f)), 1];
const H9 = CH.y1(9) - CH.y0;
const towerScale = (f) => [1, Math.max(0.0001, f), 1];
// a pulse that travels along a fill from bottom to top (up) or top to bottom, inside the filled part
const pulseY = (s, period, f, up, y0, span) => { const k = (s % period) / period, u = up ? k : 1 - k; return y0 + u * Math.max(0, span * f - 1); };
function flowClip(name, up) {
  const L = 4, f0 = up ? 0.25 : 0.9, f1 = up ? 0.9 : 0.25;
  const fAt = (s) => lerp(f0, f1, smooth(Math.min(1, s / L)));
  clip(name, L, 'loop', {
    ...each(bankGroups('fill'), { scale: (s) => tubeScale(fAt(s)) }),
    ...each(bankGroups('pulse'), { position: (s) => [0, pulseY(s, 0.8, step8(fAt(s)), up, TUBE.y0, TUBE.y1 - TUBE.y0), 0] }),
    tower_9_level: { scale: (s) => towerScale(fAt(s)) },
    tower_9_pulse: { position: (s) => [0, pulseY(s, 1.4, fAt(s), up, CH.y0, H9), 0] },
  }, {
    ...each(bankGroups('ember'), { scale: [[0, HIDE]] }),
    ...each(bankGroups('lamps_off'), { scale: [[0, HIDE]] }),
    ...each(bankGroups('lamp_in'), { scale: [[0, up ? SHOW : HIDE]] }),
    ...each(bankGroups('lamp_out'), { scale: [[0, up ? HIDE : SHOW]] }),
    tower_9_ember: { scale: [[0, HIDE]] }, tower_9_ripple: { scale: [[0, HIDE]] }, tower_9_arcs: { scale: [[0, HIDE]] },
  }, sounds(name));
}
flowClip('charging', true);
flowClip('giving', false);
const formF = (s) => (s < 1.4 ? 0.0001 : lerp(0, 0.45, smooth(Math.min(1, (s - 1.4) / 0.8))));
const strikeF = (s) => (s < 0.3 ? 0.5 : 0.5 + 0.08 * smooth(Math.min(1, (s - 0.3) / 0.4)) + (s > 0.3 && s < 0.6 ? 0.03 * Math.sin((s - 0.3) * 40) : 0));
// forming: the last Battery goes in, a ripple of cyan runs up the column from the plinth, and the fill settles at the summed charge
clip('forming', 3, 'loop', {
  tower_9_ripple: { position: (s) => [0, s < 0.4 ? 0 : Math.min(H9, (s - 0.4) / 1.1 * (H9 + 8)) + CH.y0 - 8, 0], scale: (s) => (s >= 0.4 && s < 1.6 ? SHOW : HIDE) },
  tower_9_level: { scale: (s) => towerScale(formF(s)) },
}, {
  tower_9_pulse: { scale: [[0, HIDE]] }, tower_9_ember: { scale: [[0, HIDE]] }, tower_9_arcs: { scale: [[0, HIDE]] },
  ...each(bankGroups('lamps_off'), { scale: [[0, SHOW]] }), ...each(bankGroups('lamp_in'), { scale: [[0, HIDE]] }), ...each(bankGroups('lamp_out'), { scale: [[0, HIDE]] }),
}, sounds('forming'));
// lightning: a strike on the rod throws arcs to the four bushings, the column flashes and jumps by a quarter-Battery's worth
clip('lightning', 2.4, 'loop', {
  tower_9_level: { scale: (s) => towerScale(strikeF(s)) },
  tower_9_ripple: { position: () => [0, 0, 0], scale: (s) => (s > 0.3 && s < 0.55 ? [1, (H9 + 3) / 8, 1] : HIDE) },
}, {
  tower_9_arcs: { scale: [[0, HIDE], [0.3, SHOW], [0.38, HIDE], [0.42, SHOW], [0.55, HIDE], [0.6, SHOW], [0.66, HIDE]] },
  tower_9_pulse: { scale: [[0, HIDE]] }, tower_9_ember: { scale: [[0, HIDE]] },
}, sounds('lightning'));

write(OUT, outliner);
module.exports = { T, battery, tubeFill, pulses, lamps, tile, bushing, levelFill, substation, layerModule, LAYER, TUBE, CH };
