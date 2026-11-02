// Clockwork Tubes generator -> design/models/clockwork-tubes.bbmodel (Generic Model preview with clips), to the brief
// design/briefs/clockwork-tubes.md. Four blocks in the bots' andesite tier: the Clockwork Receiver (two housings with a
// hollow Create-profile ring gear between them), the Clockwork Inlet and Brass Clockwork Inlet (a stepped hood with
// Trade Bot's lid plate as the gulping flap) and the Clockwork Tube (glass panes between steel corner rails, andesite
// collars at every joint, a hub for junctions). Model space as the game's: Receiver input at y 0, output at y 16, cog in
// the mid-plane; Inlet mouth at y 0, tube at y 16. The cog and the flap are renderer parts with their own pivots.
const path = require('path');
const { execFileSync } = require('child_process');
const { P, C, textureSet, mot, hash, HI, LO, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'clockwork-tubes.bbmodel');

// ---------- textures (clockwork_tubes_<key>) ----------
const TB = { andesite: ['#6b6e6a', '#7c7f79', '#8a8c86'], steel: ['#2f343b', '#3c424a', '#4a5059'], brass: ['#8f7638', '#b08f45', '#c9a75a'] };
const BRASS_DARK = '#6e5a2a', STEEL_HI = '#5a616b';
const tbMot = (mat, x, y, s) => { const p = TB[mat], r = hash(x, y, s); return r < 0.18 ? p[0] : r < 0.84 ? p[1] : p[2]; };
// big brass areas (the ring gear, lips, socket): the Trade Bot's base pair in his proportion (#b08f45 over #8f7638),
// without his #c9a75a highlight, which stays on the band and rims
const brassMid = (x, y, s) => (hash(x, y, s) > 0.7 ? '#8f7638' : '#b08f45');
P.botBrass = ['#8f7638', '#8f7638', '#b08f45'];
const T = textureSet(); T.file = 'clockwork_tubes_';
// the Trade Bot's andesite panels
const FIELD = (x, y, s) => tbMot('andesite', x, y, s);
const RIVET = BRASS_DARK; // the bot's bolts: dark brass dots
T.paint('field', (x, y) => FIELD(x, y, 101));
T.paint('steel', (x, y) => tbMot('steel', x, y, 102));
T.paint('dark', (x, y) => (hash(x, y, 103) > 0.85 ? '#24282d' : '#1f2327'));
T.paint('floor', (x, y) => (hash(x, y, 104) > 0.8 ? '#1a1d20' : '#16181b'));
T.paint('collar', (x, y) => tbMot('andesite', x, y, 105));
// glass, cutout: clear, a lit edge line down one side and a short diagonal glint every 8 texels. _v runs along v, _h along u
// glass, cutout: clear, a lit edge line down one side and a two-texel diagonal glint every 12 (outward faces only; the
// inward faces show just the edge, so looking through a tube never stacks four panes of glints). _v runs along v, _h along u
const glint = (a, b) => a >= 2 && a <= 3 && (((b - a) % 12) + 12) % 12 === 0;
T.paint('glass_v', (u, v) => (u === 0 ? '#6f7f87' : glint(u, v) ? '#a9bac2' : null));
T.paint('glass_h', (u, v) => (v === 0 ? '#6f7f87' : glint(v, u) ? '#a9bac2' : null));
T.paint('glass_bv', (u) => (u === 0 ? '#6f7f87' : null));
T.paint('glass_bh', (u, v) => (v === 0 ? '#6f7f87' : null));
// the tube hub's faces (4-12 on the block grid): an andesite panel in a steel frame, as the bots' panels
T.paint('hub', (x, y) => (x === 4 || x === 11 || y === 4 || y === 11 ? tbMot('steel', x, y, 106) : x >= 7 && x <= 8 && y >= 7 && y <= 8 ? BRASS_DARK : FIELD(x, y, 107)));
// an end cap's disc (5-11): steel with one centre bolt
T.paint('cap', (x, y) => (x >= 7 && x <= 8 && y >= 7 && y <= 8 ? (x === 7 && y === 7 ? '#6b717a' : '#5f656e') : mot('steel', x, y, 108)));
// the Inlet's waist band (y 7-8 = texel row 8): andesite with steel-grey rivets, or bot brass with brass rivets
T.paint('band', (x, y) => (x % 2 === 1 ? BRASS_DARK : tbMot('brass', x, y, 109)));
T.paint('lip_brass', (x, y) => brassMid(x, y, 111));
// the Brass Inlet's hood sides: Trade Bot brass, with the filter slot set into each: a dark steel recess the size of
// Create's filter box (4 x 4: x 6-10, y 2-6 = cols 6-9, rows 10-13) in a dark brass rim (user, 2026-10-31: "isn't
// brass enough", "rectangle needs to match the filter size")
T.paint('hood_brass', (u, v) => {
  if (u >= 5 && u <= 10 && v >= 9 && v <= 14) {
    if (u === 5 || u === 10 || v === 9 || v === 14) return BRASS_DARK;
    return u === 6 || v === 10 ? '#2f343b' : tbMot('steel', u, v, 112);
  }
  return brassMid(u, v, 113);
});
// the flap: Trade Bot's lid plate. Lit leading edge, dark trailing edge, a rivet line along the hinge (z 3.5 = row 12)
T.paint('flap', (u, v) => (v === 3 ? STEEL_HI : tbMot('steel', u, v, 114)));
// the Receiver's crown sides (y 9.5-15 = rows 1-6): andesite with a rivet line along its lower edge (row 5)
T.paint('crown', (x, y) => (y === 5 && x % 2 === 1 && x >= 3 && x <= 13 ? RIVET : FIELD(x, y, 115)));
// the crown's top: andesite, a steel lip round the 8 px mouth, a rivet in each corner
T.paint('crown_top', (u, v) => {
  if ((u === 2 || u === 13) && (v === 2 || v === 13)) return RIVET;
  if (u >= 3 && u <= 12 && v >= 3 && v <= 12) return u === 12 || v === 12 ? BRASS_DARK : tbMot('brass', u, v, 116);
  return FIELD(u, v, 117);
});
// the ring gear: andesite like Create's andesite parts, the teeth a shade darker so they read, steel round the hole
T.paint('cog', (x, y) => brassMid(x, y, 118));
T.paint('cog_tooth', (x, y) => (hash(x, y, 119) > 0.8 ? '#8f7638' : BRASS_DARK));
T.paint('cog_inner', (x, y) => mot('steel', x, y, 120));

const t = (k) => 'ct:' + k;
const S = t('steel'), F = t('field'), D = t('dark');

// ---------- shapes ----------
// A ring along Y with 1 px stepped outer corners: outer a..b, square hole h0..h1, y0..y1.
// f: { side, step, inner, up, down } (null hides). The pieces meet, never overlap.
function ring(name, a, b, h0, h1, y0, y1, f) {
  const out = [
    C(name + '_w', [a, y0, a + 1], [h0, y1, b - 1], { west: f.side, north: f.step, south: f.step, east: f.inner, up: f.up, down: f.down }),
    C(name + '_e', [h1, y0, a + 1], [b, y1, b - 1], { east: f.side, north: f.step, south: f.step, west: f.inner, up: f.up, down: f.down }),
    C(name + '_n', [a + 1, y0, a], [b - 1, y1, a + 1], { north: f.side, west: f.step, east: f.step, south: h0 > a + 1 ? null : f.inner, up: f.up, down: f.down }),
    C(name + '_s', [a + 1, y0, b - 1], [b - 1, y1, b], { south: f.side, west: f.step, east: f.step, north: h1 < b - 1 ? null : f.inner, up: f.up, down: f.down }),
  ];
  if (h0 > a + 1) out.push(C(name + '_ni', [h0, y0, a + 1], [h1, y1, h0], { south: f.inner, north: null, west: null, east: null, up: f.up, down: f.down }));
  if (h1 < b - 1) out.push(C(name + '_si', [h0, y0, h1], [h1, y1, b - 1], { north: f.inner, south: null, west: null, east: null, up: f.up, down: f.down }));
  return out;
}
// A solid Y-prism with 1 px stepped vertical edges
function prism(name, a, b, y0, y1, f) {
  return [
    C(name + '_m', [a, y0, a + 1], [b, y1, b - 1], { west: f.side, east: f.side, north: f.step, south: f.step, up: f.up, down: f.down }),
    C(name + '_n', [a + 1, y0, a], [b - 1, y1, a + 1], { north: f.side, west: f.step, east: f.step, south: null, up: f.up, down: f.down }),
    C(name + '_s', [a + 1, y0, b - 1], [b - 1, y1, b], { south: f.side, west: f.step, east: f.step, north: null, up: f.up, down: f.down }),
  ];
}

// ---------- Clockwork Receiver ----------
// Lower housing (the throat): a socket plate, four stepped corner pillars with a glass window between each pair, a top
// plate round the 6 px bore. Upper housing (the crown): a bored prism with its rivet line and a lip round the 8 px mouth.
function receiver() {
  const out = [];
  out.push(...ring('in', 2, 14, 4, 12, 0, 1.5, { side: S, step: S, inner: D, up: S, down: S }));
  for (const [n, x0, x1, xs0, xs1, z0, z1, zs0, zs1] of [
    ['nw', 2, 6, 3, 6, 3, 6, 2, 3], ['ne', 10, 14, 10, 13, 3, 6, 2, 3], ['sw', 2, 6, 3, 6, 10, 13, 13, 14], ['se', 10, 14, 10, 13, 10, 13, 13, 14]]) {
    const west = x0 === 2, north = z0 === 3;
    out.push(C(`pillar_${n}`, [x0, 1.5, z0], [x1, 5.5, z1], { west: west ? F : D, east: west ? D : F, north: north ? S : D, south: north ? D : S, up: null, down: null }));
    out.push(C(`pillar_${n}_step`, [xs0, 1.5, zs0], [xs1, 5.5, zs1], { north: north ? F : null, south: north ? null : F, west: west ? S : D, east: west ? D : S, up: null, down: null }));
  }
  // the throat windows: cutout glass a quarter pixel inside the face, one per side, so no face can form
  const pane = (n, from, to, dirs) => C(`window_${n}`, from, to, Object.fromEntries([...dirs.map((d) => [d, t('glass_v')]), ['all', null]]),
    { uv: Object.fromEntries(dirs.map((d) => [d, [0, 0, 4, 4]])) });
  out.push(pane('n', [6, 1.5, 2.25], [10, 5.5, 2.25], ['north', 'south']), pane('s', [6, 1.5, 13.75], [10, 5.5, 13.75], ['north', 'south']),
    pane('w', [2.25, 1.5, 6], [2.25, 5.5, 10], ['west', 'east']), pane('e', [13.75, 1.5, 6], [13.75, 5.5, 10], ['west', 'east']));
  out.push(...ring('throat_top', 2, 14, 5, 11, 5.5, 6.5, { side: S, step: S, inner: D, up: S, down: D }));
  out.push(...ring('crown', 2, 14, 5, 11, 9.5, 15, { side: t('crown'), step: S, inner: D, up: t('floor'), down: S }));
  out.push(...ring('lip', 2, 14, 4, 12, 15, 16, { side: S, step: S, inner: D, up: t('crown_top'), down: null }));
  return out;
}
// The ring gear: Create's small-cog profile (eight 3 x 3 teeth to -1..17, 3 px thick at y 6.5-9.5) on an octagonal
// ring with a 6 px hole. Straight segments full thickness; the diagonal ones a quarter pixel thinner each side, so where
// they cross the straight ones near the hole their faces never meet.
const COG_PIVOT = [8, 8, 8];
function cog() {
  const out = [], hw = 2.485, ct = t('cog'), tt = t('cog_tooth'), ci = t('cog_inner');
  const seg = (n, y0, y1, rot) => [
    C(`seg_${n}_px`, [11, y0, 8 - hw], [14, y1, 8 + hw], { all: ct, west: ci }, { rot }),
    C(`seg_${n}_nx`, [2, y0, 8 - hw], [5, y1, 8 + hw], { all: ct, east: ci }, { rot }),
    C(`seg_${n}_pz`, [8 - hw, y0, 11], [8 + hw, y1, 14], { all: ct, north: ci }, { rot }),
    C(`seg_${n}_nz`, [8 - hw, y0, 2], [8 + hw, y1, 5], { all: ct, south: ci }, { rot }),
  ];
  const teeth = (n, rot) => [
    C(`tooth_${n}_px`, [14, 6.5, 6.5], [17, 9.5, 9.5], { all: tt, west: null }, { rot }),
    C(`tooth_${n}_nx`, [-1, 6.5, 6.5], [2, 9.5, 9.5], { all: tt, east: null }, { rot }),
    C(`tooth_${n}_pz`, [6.5, 6.5, 14], [9.5, 9.5, 17], { all: tt, north: null }, { rot }),
    C(`tooth_${n}_nz`, [6.5, 6.5, -1], [9.5, 9.5, 2], { all: tt, south: null }, { rot }),
  ];
  const diag = ['y', 45, COG_PIVOT];
  out.push(...seg('s', 6.5, 9.5), ...seg('d', 6.75, 9.25, diag), ...teeth('s'), ...teeth('d', diag));
  return out;
}

// ---------- Clockwork Inlet ----------
// A stepped hood (14 px mouth, hollow to a ceiling at 10.5), a waist band, a 12 px shoulder, a 10 px neck and the tube
// socket on top. Brass: brass band, brass lip, a filter panel on each hood side.
function inlet(brass) {
  const side = brass ? t('hood_brass') : F, lip = brass ? t('lip_brass') : S, band = t('band'), sock = brass ? t('lip_brass') : S;
  return [
    ...ring('lip', 0.5, 15.5, 3, 13, 0, 1, { side: lip, step: lip, inner: D, up: lip, down: lip }),
    ...ring('hood', 1, 15, 3, 13, 1, 7, { side, step: S, inner: D, up: brass ? lip : F, down: null }),
    ...ring('band', 0.5, 15.5, 2, 14, 7, 8, { side: band, step: band, inner: band, up: band, down: band }),
    ...ring('shoulder', 2, 14, 3, 13, 7, 11, { side: F, step: S, inner: D, up: F, down: null }),
    C('ceiling', [3, 10.5, 3], [13, 11, 13], { down: t('floor'), up: S, all: null }),
    ...prism('neck', 3, 13, 11, 15, { side: F, step: S, up: t('floor'), down: null }),
    ...ring('socket', 3, 13, 4, 12, 15, 16, { side: sock, step: sock, inner: D, up: sock, down: null }),
  ];
}
// The flap: Trade Bot's lid plate across the mouth, hung from its north edge
const FLAP_PIVOT = [8, 1.25, 3.5];
const flap = () => [C('flap', [3.5, 1, 3.5], [12.5, 1.5, 12.5], { all: S, down: t('flap') })];

// ---------- Clockwork Tube ----------
// Axis indices 0 x, 1 y, 2 z; the faces at each axis's low and high end
const NEG = ['west', 'down', 'north'], POS = ['east', 'up', 'south'];
const UAX = { north: 0, south: 0, east: 2, west: 2, up: 0, down: 0 }; // which axis a face's u runs along
const cross = (ax) => [0, 1, 2].filter((i) => i !== ax);
function boxAlong(ax, s, a, b) {
  const [o0, o1] = cross(ax), from = [0, 0, 0], to = [0, 0, 0];
  from[ax] = s[0]; to[ax] = s[1]; from[o0] = a[0]; to[o0] = a[1]; from[o1] = b[0]; to[o1] = b[1];
  return [from, to];
}
// steel corner rails; ends shown only where they finish in the open (a capped end)
function rails(ax, s0, s1, ends = {}) {
  const out = [];
  for (const p of [[4, 5], [11, 12]]) for (const q of [[4, 5], [11, 12]]) {
    const [from, to] = boxAlong(ax, [s0, s1], p, q);
    out.push(C('rail', from, to, { all: S, [NEG[ax]]: ends.min || null, [POS[ax]]: ends.max || null }));
  }
  return out;
}
// glass panes between the rails, half a pixel inside them, shown from both sides so the glints read through the tube
function panes(ax, s0, s1) {
  const out = [], L = s1 - s0;
  for (const k of cross(ax)) for (const pos of [4.5, 11.5]) {
    const m = cross(ax).find((i) => i !== k), from = [0, 0, 0], to = [0, 0, 0];
    from[ax] = s0; to[ax] = s1; from[k] = pos; to[k] = pos; from[m] = 5; to[m] = 11;
    const faces = { all: null }, uv = {};
    const outward = pos < 8 ? NEG[k] : POS[k];
    for (const dir of [NEG[k], POS[k]]) {
      const alongU = UAX[dir] === ax;
      faces[dir] = t((dir === outward ? 'glass_' : 'glass_b') + (alongU ? 'h' : 'v'));
      uv[dir] = alongU ? [0, 0, L, 6] : [0, 0, 6, L];
    }
    out.push(C('pane', from, to, faces, { uv }));
  }
  return out;
}
// a half-collar: a 1 px andesite ring (10 x 10, stepped corners) round the 8 px tube. outer: the end at the block face
function collar(ax, s0, s1, outer) {
  const [o0, o1] = cross(ax), K = t('collar'), out = [];
  for (const [a, b, thin] of [[[3, 4], [4, 12], 'a-'], [[12, 13], [4, 12], 'a+'], [[4, 12], [3, 4], 'b-'], [[4, 12], [12, 13], 'b+']]) {
    const [from, to] = boxAlong(ax, [s0, s1], a, b);
    const k = thin[0] === 'a' ? o0 : o1, other = thin[0] === 'a' ? o1 : o0, low = thin[1] === '-';
    const faces = { [low ? NEG[k] : POS[k]]: K, [low ? POS[k] : NEG[k]]: S, [NEG[other]]: S, [POS[other]]: S,
      [NEG[ax]]: outer === 'min' ? null : K, [POS[ax]]: outer === 'max' ? null : K };
    out.push(C('collar', from, to, faces));
  }
  return out;
}
// the middle of a straight tube (4-12), between its two arms: the game draws a straight as two arms and this
const spine = (ax) => [...rails(ax, 4, 12), ...panes(ax, 4, 12)];
function straight(ax) {
  return [...rails(ax, 0, 16), ...panes(ax, 0, 16), ...collar(ax, 0, 1, 'min'), ...collar(ax, 15, 16, 'max')];
}
// the hub: 1 px andesite panels in steel frames round a dark-lined core, one on each side no tube joins; where a tube
// joins, the side is open, so the tube runs into the hub instead of ending at a wall. The notches are the stepped edges.
// open: joints like 'x+'
function hub(open = []) {
  const out = [];
  for (let ax = 0; ax < 3; ax++) for (const hi of [false, true]) {
    if (open.includes('xyz'[ax] + (hi ? '+' : '-'))) continue;
    const [from, to] = boxAlong(ax, hi ? [12, 13] : [3, 4], [4, 12], [4, 12]);
    out.push(C('hub', from, to, { all: S, [hi ? POS[ax] : NEG[ax]]: t('hub'), [hi ? NEG[ax] : POS[ax]]: open.length ? D : null }));
  }
  return out;
}
// an arm from the hub to one face; capped: a steel disc with a bolt closes the end between the rails
function arm(ax, hi, capped = false) {
  const s = hi ? [12, 16] : [0, 4], end = capped ? S : null; // from the hub's open side to the face
  const out = [...rails(ax, s[0], s[1], hi ? { max: end } : { min: end }), ...panes(ax, s[0], s[1]), ...collar(ax, hi ? 15 : 0, hi ? 16 : 1, hi ? 'max' : 'min')];
  if (capped) {
    const [from, to] = boxAlong(ax, hi ? [15, 15.75] : [0.25, 1], [5, 11], [5, 11]);
    out.push(C('cap', from, to, { all: S, [hi ? POS[ax] : NEG[ax]]: t('cap'), [hi ? NEG[ax] : POS[ax]]: null }));
  }
  return out;
}
// a junction: the hub plus an arm per joint, e.g. ['x+', 'y-']
const junction = (joints, caps = []) => [...hub([...joints, ...caps]), ...joints.map((j) => arm('xyz'.indexOf(j[0]), j[1] === '+')).flat(),
  ...caps.map((j) => arm('xyz'.indexOf(j[0]), j[1] === '+', true)).flat()];

// ---------- preview ----------
if (require.main === module) {
  const fs = require('fs');
  const D_ = require('./clockwork-dynamo.gen.js');
  const { cube, group, clip, write } = preview('clockwork_tubes', { 'ct:': T, 'dyn:': D_.T });
  const at = (gx, gy, gz = 0) => [gx * 16, gy * 16, gz * 16];
  const cubes = (list, off) => list.map((c) => cube(c, off));
  // a Create block model, borrowed as a stand-in (never exported)
  const CREATE = 'C:/Users/bones/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-281/484ecc6852a36a72fb50e905be79b3ce96181032/create-1.21.1-6.0.10-281-slim.jar';
  function createModel(p) {
    const m = JSON.parse(execFileSync('unzip', ['-p', CREATE, `assets/create/models/block/${p}.json`]).toString());
    const tex = (r) => { const v = m.textures[r.slice(1)]; return v.includes(':') ? v : 'minecraft:' + v; };
    return m.elements.map((e, i) => {
      const faces = { all: null }, uv = {}, frot = {};
      for (const [d, f] of Object.entries(e.faces)) { faces[d] = tex(f.texture); uv[d] = f.uv; if (f.rotation) frot[d] = f.rotation; }
      const rot = e.rotation && e.rotation.angle ? [e.rotation.axis, e.rotation.angle, e.rotation.origin] : undefined;
      return C(`${p.replace(/\W/g, '_')}_${i}`, e.from, e.to, faces, { uv, frot, rot });
    });
  }
  const barrel = () => [C('barrel', [0, 0, 0], [16, 16, 16], { all: 'minecraft:block/barrel_side', up: 'minecraft:block/barrel_top', down: 'minecraft:block/barrel_bottom' })];
  const floorBlock = () => [C('floor', [0, 0, 0], [16, 16, 16], { all: 'minecraft:block/polished_andesite' })];
  const FULL = [0, 0, 16, 16];
  // item stand-ins: an upright card for items in motion, a flat one for items lying about
  const itemCard = (tex) => [C('item', [5.5, 5.5, 8], [10.5, 10.5, 8], { north: tex, south: tex, all: null }, { uv: { north: FULL, south: FULL } })];
  const itemFlat = (tex, x, y, z) => [C('item', [x - 2.5, y, z - 2.5], [x + 2.5, y, z + 2.5], { up: tex, down: tex, all: null }, { uv: { up: FULL, down: FULL } })];

  const outliner = [];
  const blockGroup = (name, gx, gy, gz, parts, extra = [], pivot = [8, 8, 8]) => {
    const off = at(gx, gy, gz);
    return group(name, pivot, off, [...cubes(parts, off), ...extra]);
  };
  const inletGroup = (name, gx, gy, gz, brass) => {
    const off = at(gx, gy, gz);
    return group(name, [8, 8, 8], off, [...cubes(inlet(brass), off), group(`${name}_flap`, FLAP_PIVOT, off, cubes(flap(), off))]);
  };
  const receiverGroup = (name, gx, gy, gz) => {
    const off = at(gx, gy, gz);
    return group(name, [8, 8, 8], off, [...cubes(receiver(), off), group(`${name}_cog`, COG_PIVOT, off, cubes(cog(), off))]);
  };

  // --- the user's diagram: three Inlets (on a barrel, over a depot, over the floor), tubes rising and branching into one,
  // the Receiver above outputting into a barrel, a Create cogwheel meshed with its ring gear
  const diagram = [];
  for (let gx = -4; gx <= 4; gx++) diagram.push(blockGroup(`floor_${gx + 4}`, gx, -2, 0, floorBlock()));
  diagram.push(blockGroup('barrel_in', -3, -1, 0, barrel()));
  diagram.push(inletGroup('inlet_l', -3, 0, 0, false));
  diagram.push(blockGroup('hub_l', -3, 1, 0, junction(['y-', 'x+'])));
  diagram.push(blockGroup('tube_l1', -2, 1, 0, straight(0)), blockGroup('tube_l2', -1, 1, 0, straight(0)));
  diagram.push(blockGroup('hub_c', 0, 1, 0, junction(['x-', 'x+', 'y-', 'y+', 'z+'])));
  // the branch out of the plane: back along z to a fourth Inlet on its own barrel
  diagram.push(blockGroup('tube_b1', 0, 1, 1, straight(2)), blockGroup('tube_b2', 0, 1, 2, straight(2)));
  diagram.push(blockGroup('hub_b', 0, 1, 3, junction(['z-', 'y-'])));
  diagram.push(inletGroup('inlet_b', 0, 0, 3, false));
  diagram.push(blockGroup('barrel_back', 0, -1, 3, barrel()));
  for (let gz = 1; gz <= 4; gz++) diagram.push(blockGroup(`floor_b${gz}`, 0, -2, gz, floorBlock()));
  diagram.push(blockGroup('depot', 0, -1, 0, createModel('depot/block')));
  diagram.push(inletGroup('inlet_c', 0, 0, 0, false));
  diagram.push(blockGroup('tube_r1', 1, 1, 0, straight(0)), blockGroup('tube_r2', 2, 1, 0, straight(0)));
  diagram.push(blockGroup('hub_r', 3, 1, 0, junction(['x-', 'y-'])));
  diagram.push(inletGroup('inlet_r', 3, 0, 0, false));
  diagram.push(blockGroup('tube_up', 0, 2, 0, straight(1)));
  diagram.push(receiverGroup('receiver', 0, 3, 0));
  diagram.push(blockGroup('barrel_out', 0, 4, 0, barrel()));
  diagram.push(blockGroup('cogwheel', 1, 3, 0, createModel('cogwheel_shaftless')));
  // items waiting: one on the depot, one on the floor under the right Inlet
  diagram.push(group('item_depot', [8, 13.5, 8], at(0, -1), cubes(itemFlat('minecraft:item/iron_ingot', 8, 13.05, 8), at(0, -1))));
  diagram.push(group('item_floor_wait', [5, 0.1, 11], at(3, -1), cubes(itemFlat('minecraft:item/bone', 5, 0.05, 11), at(3, -1))));
  // items in motion (the clip moves them along their paths); W is a world point in px on the block grid
  const ITEMS = [
    { name: 'item_l', tex: 'minecraft:item/rotten_flesh', path: [[-40, 3, 8], [-40, 24, 8], [8, 24, 8], [8, 67, 8]] },
    { name: 'item_c', tex: 'minecraft:item/iron_ingot', path: [[8, -2.5, 8], [8, 67, 8]] },
    { name: 'item_r', tex: 'minecraft:item/bone', path: [[56, -15, 8], [56, 24, 8], [8, 24, 8], [8, 67, 8]] },
    { name: 'item_b', tex: 'minecraft:item/wheat', path: [[8, 3, 56], [8, 24, 56], [8, 24, 8], [8, 67, 8]] },
  ];
  for (const it of ITEMS) {
    const [x, y, z] = it.path[0], off = [x - 8, y - 8, z - 8];
    diagram.push(group(it.name, [8, 8, 8], off, cubes(itemCard(it.tex), off)));
  }
  outliner.push(group('diagram', [8, 8, 8], [0, 0, 0], diagram, { open: true }));

  // --- showcase: an Inlet in all six facings; plain beside Brass; the Receiver on its side and upside down
  const SX = 8;
  const show = [];
  const FACE = { down: [0, 0, 0], up: [180, 0, 0], north: [-90, 0, 0], south: [90, 0, 0], east: [0, 0, -90], west: [0, 0, 90] };
  Object.keys(FACE).forEach((f, i) => show.push(inletGroup(`facing_${f}`, SX + i * 2, 0, 0, false)));
  show.push(inletGroup('plain', SX, 3, 0, false), inletGroup('brass', SX + 2, 3, 0, true));
  show.push(receiverGroup('rcv_side', SX + 5, 3, 0), receiverGroup('rcv_down', SX + 8, 3, 0));
  show.push(group('item_pop', [8, 8, 8], at(SX + 5, 3), cubes(itemCard('minecraft:item/wheat'), at(SX + 5, 3))));
  // tube shapes: straight, elbow, T, a 3D cross, a capped end
  show.push(blockGroup('shape_straight', SX, 6, 0, straight(0)));
  show.push(blockGroup('shape_elbow', SX + 2, 6, 0, junction(['x-', 'y+'])));
  show.push(blockGroup('shape_t', SX + 4, 6, 0, junction(['x-', 'x+', 'y-'])));
  show.push(blockGroup('shape_cross', SX + 6, 6, 0, junction(['x-', 'x+', 'y-', 'y+', 'z-'])));
  show.push(blockGroup('shape_capped', SX + 8, 6, 0, junction(['x-'], ['x+'])));
  // the family: Receiver and Inlet beside the Clockwork Dynamo
  show.push(receiverGroup('fam_receiver', SX + 12, 0, 0), inletGroup('fam_inlet', SX + 13, 0, 0, true));
  show.push(blockGroup('fam_dynamo', SX + 14, 0, 0, D_.block(true), [group('fam_dynamo_aerial', D_.AERIAL_PIVOT, at(SX + 14, 0), cubes(D_.aerial(), at(SX + 14, 0)))]));
  outliner.push(group('showcase', [8, 8, 8], [0, 0, 0], show, { open: false }));

  // ---------- motion ----------
  // Create has no inertia: cogs turn steadily. Only the flap springs.
  const DEG = 400; // degrees a second (about 67 RPM): 3.6 s is four whole turns, so loops close
  const PXS = DEG / 6 / 16 * 16; // items travel RPM / 16 blocks a second = 66.7 px a second
  const INWARD = -1; // flap rotation about X that lifts it into the hood
  const smooth = (f) => f * f * (3 - 2 * f);
  // keyed curve with smoothstep between keys
  const curve = (keys) => (s) => {
    if (s <= keys[0][0]) return keys[0][1];
    for (let i = 1; i < keys.length; i++) if (s <= keys[i][0]) {
      const [t0, v0] = keys[i - 1], [t1, v1] = keys[i];
      return v0 + (v1 - v0) * smooth((s - t0) / (t1 - t0));
    }
    return keys[keys.length - 1][1];
  };
  // the gulp: puff out 5°, snap in 50°, swing back past closed by 8°, settle (0.3 s)
  const gulpCurve = curve([[0, 0], [0.05, -5], [0.13, 50], [0.23, -8], [0.3, 0]]);
  const gulp = (starts, L) => (s) => {
    let a = 0;
    for (const g of starts) { const d = (((s - g) % L) + L) % L; if (d < 0.3) a = gulpCurve(d); }
    return [INWARD * a, 0, 0];
  };
  const merge = (...xs) => { const o = {}; for (const x of xs) for (const [k, v] of Object.entries(x)) o[k] = { ...(o[k] || {}), ...v }; return o; };
  const RCVS = ['receiver', 'rcv_side', 'rcv_down', 'fam_receiver'];
  const spin = () => merge(
    Object.fromEntries(RCVS.map((r) => [`${r}_cog`, { rotation: (s) => [0, (s * DEG) % 360, 0] }])),
    { cogwheel: { rotation: (s) => [0, 22.5 - ((s * DEG) % 360), 0] } });
  // facings for the showcase, held in every clip
  const facings = {
    ...Object.fromEntries(Object.entries(FACE).map(([f, r]) => [`facing_${f}`, { rotation: [[0, r]] }])),
    rcv_side: { rotation: [[0, [0, 0, -90]]] }, rcv_down: { rotation: [[0, [180, 0, 0]]] },
  };
  const ALL_INLETS = ['inlet_l', 'inlet_c', 'inlet_r', 'inlet_b', ...Object.keys(FACE).map((f) => `facing_${f}`), 'plain', 'brass', 'fam_inlet'];
  const hidden = (names) => Object.fromEntries(names.map((n) => [n, { scale: [[0, HIDE]] }]));

  // a polyline in px -> position at distance d
  const dist = (a, b) => Math.hypot(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
  function along(pts, d) {
    for (let i = 1; i < pts.length; i++) {
      const a = pts[i - 1], b = pts[i], len = dist(a, b);
      if (d <= len) return a.map((v, k) => v + ((b[k] - v) * d) / len);
      d -= len;
    }
    return pts[pts.length - 1];
  }
  const lengthOf = (pts) => pts.slice(1).reduce((acc, p, i) => acc + dist(pts[i], p), 0);

  // flow: the round robin. Each Inlet gulps in turn and its item glides up the glass, through the hub, up the throat,
  // through the hollow cog and into the barrel. Loop 3.6 s.
  const FL = 3.6, TURN = [0.2, 1.1, 2.0, 2.9];
  const flowTracks = merge(spin(FL),
    { inlet_l_flap: { rotation: gulp([TURN[0]], FL) }, inlet_c_flap: { rotation: gulp([TURN[1]], FL) }, inlet_r_flap: { rotation: gulp([TURN[2]], FL) }, inlet_b_flap: { rotation: gulp([TURN[3]], FL) } },
    ...ITEMS.map((it, i) => {
      const L = lengthOf(it.path), dur = L / PXS, t0 = TURN[i];
      const local = (s) => (((s - t0) % FL) + FL) % FL;
      return { [it.name]: {
        position: (s) => { const d = local(s); const p = along(it.path, Math.min(d, dur) * PXS); return [p[0] - it.path[0][0], p[1] - it.path[0][1], p[2] - it.path[0][2]]; },
        rotation: (s) => [0, ((Math.min(local(s), dur) * PXS) / 16) * 90, 0],
        scale: (s) => (local(s) < dur ? SHOW : HIDE),
      } };
    }),
    // the depot's item is taken at the centre Inlet's turn and a new one arrives a beat after its trip
    { item_depot: { scale: (s) => { const d = (((s - TURN[1]) % FL) + FL) % FL; return d < 1.6 ? HIDE : SHOW; } } },
    // the showcase Inlets gulp too, one after another
    ...[...Object.keys(FACE).map((f) => `facing_${f}`), 'plain', 'brass', 'fam_inlet'].map((n, i) => ({ [`${n}_flap`]: { rotation: gulp([0.3 + i * 0.35], FL) } })));
  clip('flow', FL, 'loop', flowTracks, { ...facings, ...hidden(['item_pop']) });

  // gulp: every Inlet at once, slowed down to watch (1 s)
  clip('gulp', 1, 'loop', merge(...ALL_INLETS.map((n) => ({ [`${n}_flap`]: { rotation: (s) => [INWARD * gulpCurve(Math.max(0, s - 0.2)), 0, 0] } }))),
    { ...facings, ...hidden(['item_l', 'item_c', 'item_r', 'item_b', 'item_pop']) });

  // cog_turn: the ring gears and the meshed Create cogwheel, nothing else (0.9 s = one turn)
  clip('cog_turn', 0.9, 'loop', spin(0.9), { ...facings, ...hidden(['item_l', 'item_c', 'item_r', 'item_b', 'item_pop']) });

  // pop: the Receiver on its side has nothing in front: an item rises through the cog and hops out of the mouth (1.5 s).
  // On its side the output faces +x; the item comes up the bore (local +y = world +x) and falls under gravity once out.
  clip('pop', 1.5, 'loop', merge(spin(1.5), { item_pop: {
    position: (s) => {
      const x = Math.min(s, 0.12) * PXS, out = Math.max(0, s - 0.12);
      return [x + out * 40, out * 30 - 0.5 * 160 * out * out, 0];
    },
    rotation: (s) => [0, 0, -s * 220],
    scale: (s) => (s < 0.95 ? SHOW : HIDE),
  } }), { ...facings, ...hidden(['item_l', 'item_c', 'item_r', 'item_b']) });

  write(OUT, outliner);
}
module.exports = { T, receiver, cog, inlet, flap, straight, spine, hub, arm, junction, COG_PIVOT, FLAP_PIVOT };
