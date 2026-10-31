// Clockwork Wastes pylons and reactors preview -> design/models/clockwork-wastes-reactors.bbmodel (Generic Model preview).
// To design/briefs/clockwork-wastes-blocks.md §5-7: the Conduit Pylon (five states), the Derelict Star Reactor (dormant,
// fuelled, salvaged) and the Star Reactor (fuelled, unfuelled, upgraded). Palette and textures from the material sheet.
// node clockwork-wastes-reactors.gen.js
// Built in block model space (x 0 west -> 16 east, y up, z 0 north -> 16 south), front to the north; multiblocks span
// several blocks from their controller. Glows are their own shade:false cubes in groups, so clips can light them.
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, STEEL, BRASS, COPPER, box, zdisc, vaTile, mott, edge } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-reactors.bbmodel');
const GLOW = { shade: false };
const STAR_ITEM = 'minecraft:item/nether_star';

// ---------- textures ----------
// the pylon's 3 x 3 x 3 core, uv 6-9: lit (edge, a core-tone centre), dim (failing), dark (dead), blaze (the overload's flash)
const coreTex = (ring, mid) => (x, y) => (x === 7 && y === 7 ? mid : ring);
T.paint('core_lit', coreTex(STAR.edge, STAR.core));
T.paint('core_dim', coreTex(STAR.dim, STAR.edge));
T.paint('core_dark', coreTex(VA.glass, VA.glint));
T.paint('core_cracked', (x, y) => ((x === 6 && y === 6) || (x === 7 && y === 7) || (x === 8 && y === 7) ? '#121118' : x === 7 && y === 8 ? STAR.edge : STAR.dim));
T.paint('core_blaze', coreTex(STAR.core, STAR.core));
// smoky glass (translucent); cracked; the sphere lit from inside (a translucent violet, the edge tone at the rim)
// smoky glass: a light tint, so what's inside (the pylon's core, the reactors' stars) shows through
T.paint('smoke_glass', (x, y) => (hash(x, y, 61) > 0.93 ? '#3a355270' : '#2a283850'));
T.paint('smoke_glass_cracked', (x, y) => ([[3, 3], [4, 4], [5, 4], [6, 5], [7, 6], [7, 7], [8, 8], [9, 8]].some(([a, b]) => a === x && b === y) ? '#4c475f' : hash(x, y, 61) > 0.93 ? '#3a355270' : '#2a283850'));
T.paint('sphere_lit', (x, y) => (edge(x, y) === 0 ? '#7f76a8aa' : hash(x, y, 62) > 0.85 ? '#7f76a888' : '#3a355288'));
T.paint('core_glow', () => STAR.core);
T.paint('spark', () => STAR.core);
// the Star Reactor: a Create brass casing in house brass and dark steel; its side panels; the top plate (brass rim, steel
// field where the pips and the hatch sit); the hatch; pips dark and lit; the ports' glass dark and lit
// the crafted casing is new: mid brass, the highlight kept rare so it reads as brass, not yellow
T.paint('brass', mott(BRASS, 63, 0.2, 0.97));
T.paint('casing_panel', (x, y) => (edge(x, y) === 0 ? '#2a2f35' : hash(x, y, 64) > 0.88 ? '#40464e' : STEEL[1]));
T.paint('casing_top', (x, y) => (edge(x, y) < 2 ? mott(BRASS, 65, 0.2, 0.97)(x, y) : edge(x, y) === 2 ? '#2a2f35' : hash(x, y, 66) > 0.9 ? '#363b42' : STEEL[0]));
T.paint('hatch', (x, y) => (edge(x, y) === 0 ? BRASS[0] : (x === 7 || x === 8) && y === 7 ? '#2a2f35' : mott(BRASS, 67, 0.2, 0.97)(x, y)));
T.paint('pip_dark', () => VA.seam);
T.paint('pip_lit', () => STAR.edge);
// the ports' smoked glass: light enough that the star, its cage and the orbit ring show through; lit, a violet tint and a
// brighter rim where the light catches the glass's edge
T.paint('port_glass', (x, y) => ((x + y) % 7 === 0 ? '#34304a70' : '#2a283358'));
T.paint('port_lit', (x, y) => (Math.hypot(x - 7.5, y - 7.5) > 3.1 ? '#7f76a890' : null));
T.paint('copper', mott(COPPER, 68, 0.15, 0.85));
T.paint('groove', () => VA.lo);

// ---------- helpers ----------
const shift = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]), rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const faces = (tex, o = {}) => (typeof tex === 'string' ? { all: tex, ...o } : tex);
// a stepped round slab along y: one box per 2 px band across z, its half-width from the circle
function roundSlab(name, cx, cz, y0, y1, r, tex, step = 2) {
  const out = [];
  for (let z = -r; z < r; z += step) {
    const zm = Math.min(Math.abs(z), Math.abs(z + step)), hw = Math.round(Math.sqrt(Math.max(0, r * r - zm * zm)) * 2) / 2;
    if (hw > 0) out.push(box(`${name}_${z}`, [cx - hw, y0, cz + z], [cx + hw, y1, cz + z + step], faces(tex)));
  }
  return out;
}
// a stepped sphere: a round slab per 2 px slice of height, with only its outward faces (sides; the end rows' north and
// south; tops on the upper half, bottoms on the lower), so translucent glass is a thin shell, not layers stacked on layers
function sphere(name, cx, cy, cz, r, tex, o = {}) {
  const out = [];
  for (let y = -r; y < r; y += 2) {
    const ym = Math.min(Math.abs(y), Math.abs(y + 2)), rr = Math.sqrt(Math.max(0, r * r - ym * ym));
    const rows = roundSlab(`${name}_${y}`, cx, cz, cy + y, cy + y + 2, Math.max(1, Math.round(rr)), tex, Math.max(1, Math.min(2, Math.round(rr / 3))));
    rows.forEach((c, i) => out.push({ ...c, ...o, faces: { east: tex, west: tex, north: i === 0 ? tex : null, south: i === rows.length - 1 ? tex : null, up: y >= 0 ? tex : null, down: y < 0 ? tex : null } }));
  }
  return out;
}
// the Nether Star, as items render: two crossed planes of its sprite
const starItem = (name, cx, cy, cz, size) => [box(name + '_a', [cx - size / 2, cy - size / 2, cz], [cx + size / 2, cy + size / 2, cz], { north: STAR_ITEM, south: STAR_ITEM }, { ...GLOW, uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
  box(name + '_b', [cx, cy - size / 2, cz - size / 2], [cx, cy + size / 2, cz + size / 2], { east: STAR_ITEM, west: STAR_ITEM }, { ...GLOW, uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] } })];

// ---------- 5. Conduit Pylon (1 x 3): a narrow void-alloy obelisk, a crown of six fins round a starlight core ----------
// The shaft steps 6 -> 5 -> 4 px; a seam up each corner. The crown (top 8 px) pivots at the shaft top's front edge.
const SECTIONS = [[3, 14, 3], [14, 27, 2.5], [27, 40, 2]];
function pylonShaft(lit) {
  const out = [box('collar', [3, 0, 3], [13, 3, 13], faces('va', { down: null })), box('collar_lip', [4, 3, 4], [12, 3.5, 12], faces('va', { down: null }))];
  const strips = [];
  for (const [y0, y1, h] of SECTIONS) {
    out.push(box(`shaft_${y0}`, [8 - h, y0, 8 - h], [8 + h, y1, 8 + h], faces(M.VA_BODY, { up: 'va', down: null })));
    // a seam up each corner: on the north and south faces, half a pixel in from each edge
    for (const [x, z, dir] of [[8 - h + 0.5, 8 - h - 0.25, 'north'], [8 + h - 1, 8 - h - 0.25, 'north'], [8 - h + 0.5, 8 + h + 0.25, 'south'], [8 + h - 1, 8 + h + 0.25, 'south']])
      strips.push(box(`seam_${y0}_${x}_${dir}`, [x, y0 + 0.5, z], [x + 0.5, y1 - 0.5, z], { [dir]: lit ? 'seam_strip_lit' : 'seam_strip_dark' }, lit ? GLOW : {}));
  }
  return { body: out, seams: strips };
}
// a fin: 2 wide, 8 tall, 1 thick, on the north side of the core, leaning out 22.5 degrees; its group turns it to 60k + 30
// degrees, so a gap (not a fin) faces each side and the core shows through
const fin = (k) => [box('fin_' + k, [7, 41, 3.5], [9, 49, 4.5], 'va', { rot: ['x', -22.5, [8, 41, 4]] }), box('fin_root_' + k, [7, 40.5, 4], [9, 42, 5.5], 'va')];
const finStub = (k) => [box('fin_stub_' + k, [7, 41, 3.5], [9, 43, 4.5], 'va', { rot: ['x', -22.5, [8, 41, 4]] }), box('fin_root_' + k, [7, 40.5, 4], [9, 42, 5.5], 'va')];
function crown(state) {
  const cage = [box('crown_plate', [5, 40, 5], [11, 41.5, 11], 'va'), box('cage_top', [5.5, 46.5, 5.5], [10.5, 47, 10.5], 'va'),
    ...[[5.5, 5.5], [10, 5.5], [5.5, 10], [10, 10]].map(([x, z], i) => box('cage_bar_' + i, [x, 41.5, z], [x + 0.5, 46.5, z + 0.5], 'va'))];
  // an open cage of bars, so the core reads from any side; the crack the brief puts in the cage's glass goes across the core
const coreTexFor = { intact: 'core_lit', cracked: 'core_cracked', failing: 'core_cracked', broken: 'core_dark', relit: 'core_lit' }[state];
  const uv = { north: [6, 6, 9, 9], south: [6, 6, 9, 9], east: [6, 6, 9, 9], west: [6, 6, 9, 9], up: [6, 6, 9, 9], down: [6, 6, 9, 9] };
  const core = box('core', [6.5, 42.5, 6.5], [9.5, 45.5, 9.5], coreTexFor, { ...(coreTexFor === 'core_dark' ? {} : GLOW), uv });
  return { cage, core };
}
// fins left standing per state: one snaps per hit, six hits break it
const FINS_LEFT = { intact: 6, cracked: 4, failing: 2, broken: 0, relit: 0 };

// ---------- 6. Derelict Star Reactor (3 x 3 x 3, the heart's): a ring base, four cradle arms, a star held in smoky glass ----------
// Built about its centre column (8, *, 8) of the middle block, from -16 to 32. The sphere's centre is at y 30, radius 8.
const SPH = [8, 30, 8, 8];
function derelictBase() {
  const out = roundSlab('ring', 8, 8, 0, 4, 24, faces(M.VA_BODY, { up: M.VA_BODY }));
  out.push(...roundSlab('ring_top', 8, 8, 4, 6, 20, faces(M.VA_BODY)));
  return out;
}
// conduit grooves out from under the sphere across the raised ring (r 8.5 to 19), on the diagonals between the arms (the
// group turns the four 45 degrees): dark channels, with lit strips in them
const GROOVES = [[7, -11, 9, -0.5], [7, 16.5, 9, 27], [-11, 7, -0.5, 9], [16.5, 7, 27, 9]];
const grooves = () => GROOVES.map(([x0, z0, x1, z1], i) => box('groove_' + i, [x0, 6, z0], [x1, 6.25, z1], { up: 'groove' }));
const grooveGlow = () => GROOVES.map(([x0, z0, x1, z1], i) => box('groove_g_' + i, x0 === 7 ? [7.5, 6.5, z0] : [x0, 6.5, 7.5], x0 === 7 ? [8.5, 6.5, z1] : [x1, 6.5, 8.5], { up: 'seam_strip_lit' }, GLOW));
// an arm on the north side (z -12 to -8 at its foot), rising and curling in over the sphere; its group turns it to 90k degrees
function arm(k) {
  return [box(`arm_foot_${k}`, [5, 6, -13], [11, 9, -6], 'va'),
    box(`arm_shaft_${k}`, [6, 9, -12], [10, 26, -8], faces('va', { south: M.VA_BODY })),
    box(`arm_bend_${k}`, [6, 26, -12], [10, 32, -8], 'va', { rot: ['x', 22.5, [8, 26, -10]] }),
    box(`arm_knuckle_${k}`, [6, 31.54, -9.7], [10, 36.54, -5.7], 'va', { rot: ['x', 45, [8, 31.54, -7.7]] }),
    box(`arm_tip_${k}`, [6.5, 34, -4.6], [9.5, 36, 0.5], 'va')];
}
const armSeam = (k) => [box(`arm_seam_${k}`, [7.5, 10, -7.75], [8.5, 25.5, -7.75], { south: 'seam_strip_lit' }, GLOW)];

// ---------- 7. Star Reactor (1 x 1, crafted): a brass casing, three round ports, a star in a void-alloy cage ----------
// The ports are 8 px stepped circles centred on each face (rows of half-width 2, 3, 4, 4, 4, 4, 3, 2 from y 12 down to y 4).
const PORT = [[11, 2], [10, 3], [9, 4], [8, 4], [7, 4], [6, 4], [5, 3], [4, 2]];
function sidePanel(dir, port) {
  // built as the north panel (z 1-2, between the corner posts x 2-14, y 2-14), then turned to its side
  const out = [];
  const pf = { all: 'brass', north: 'casing_panel', south: null };
  if (!port) out.push(box('panel', [2, 2, 1], [14, 14, 2], { all: 'brass', north: 'casing_panel', south: null }));
  else {
    out.push(box('panel_t', [2, 12, 1], [14, 14, 2], pf), box('panel_b', [2, 2, 1], [14, 4, 2], pf));
    for (const [y, hw] of PORT) { out.push(box(`panel_w${y}`, [2, y, 1], [8 - hw, y + 1, 2], pf), box(`panel_e${y}`, [8 + hw, y, 1], [14, y + 1, 2], pf)); }
  }
  return turnTo(out, dir);
}
const TURN = {
  north: (x, z) => [x, z], east: (x, z) => [16 - z, x], south: (x, z) => [16 - x, 16 - z], west: (x, z) => [z, 16 - x],
};
const FACE_TURN = { north: {}, east: { north: 'east', east: 'south', south: 'west', west: 'north' }, south: { north: 'south', east: 'west', south: 'north', west: 'east' }, west: { north: 'west', east: 'north', south: 'east', west: 'south' } };
function turnTo(cubes, dir) {
  const t = TURN[dir], f = FACE_TURN[dir];
  return cubes.map((c) => {
    const [ax, az] = t(c.from[0], c.from[2]), [bx, bz] = t(c.to[0], c.to[2]);
    const fc = {}; for (const [k, v] of Object.entries(c.faces)) fc[f[k] || k] = v;
    return { ...c, name: c.name + '_' + dir, from: [Math.min(ax, bx), c.from[1], Math.min(az, bz)], to: [Math.max(ax, bx), c.to[1], Math.max(az, bz)], faces: fc };
  });
}
// the port's glass: one flat stepped disc, a box per row (no overlaps, so translucent glass is a single layer)
const portGlass = (dir, tex, lit) => turnTo(PORT.map(([y, hw]) => box(`port_${y}`, [8 - hw, y, lit ? 1.25 : 1.5], [8 + hw, y + 1, lit ? 1.25 : 1.5], { north: tex }, { ...(lit ? GLOW : {}), uv: { north: [8 - hw, 15 - y, 8 + hw, 16 - y] } })), dir);
function casing() {
  const out = [];
  for (const [x, z] of [[0, 0], [14, 0], [0, 14], [14, 14]]) out.push(box(`post_${x}_${z}`, [x, 2, z], [x + 2, 14, z + 2], 'brass'));
  out.push(box('bottom', [0, 0, 0], [16, 2, 16], faces('brass', { up: null })), box('top', [0, 14, 0], [16, 16, 16], faces('brass', { up: 'casing_top', down: null })));
  for (const dir of ['north', 'east', 'west']) out.push(...sidePanel(dir, true));
  out.push(...sidePanel('south', false));
  // FE out at the back face centre: a copper terminal on the solid back panel
  out.push(box('terminal', [6, 6, 15], [10, 10, 16.5], faces('copper', { north: null })));
  // the funnel hatch on top, at the centre of the pip ring
  out.push(box('hatch', [6, 16, 6], [10, 16.5, 10], faces('hatch', { down: null })), box('hatch_grip', [7, 16.5, 7.5], [9, 17, 8.5], 'brass'));
  // the void-alloy cage inside: four bars at the diagonals and two rings
  for (const [x, z] of [[4, 4], [11.5, 4], [4, 11.5], [11.5, 11.5]]) out.push(box(`cage_${x}_${z}`, [x, 3, z], [x + 0.5, 13, z + 0.5], 'va'));
  out.push(box('cage_floor', [4, 2, 4], [12, 3, 12], faces('va', { down: null })), box('cage_roof', [4, 13, 4], [12, 14, 12], faces('va', { up: null })));
  return out;
}
const PIPS = (n) => Array.from({ length: n }, (_, i) => { const a = (i / n) * 2 * Math.PI - Math.PI / 2, r = n === 8 ? 4.5 : 5; return [Math.round((8 + Math.cos(a) * r) * 2) / 2, Math.round((8 + Math.sin(a) * r) * 2) / 2]; });
const pip = (name, x, z, tex, y, o = {}) => box(name, [x - 0.5, y, z - 0.5], [x + 0.5, y, z + 0.5], { up: tex }, o);

// the pylon's floor groove from the collar toward the dais (north), and its glow while live
const pylonGroove = () => [box('groove', [7, 0, -14], [9, 0.25, 3], { up: 'groove' })];
const pylonGrooveGlow = () => [box('groove_g', [7.5, 0.5, -13], [8.5, 0.5, 3], { up: 'seam_strip_lit' }, GLOW)];
// the failing crown's two sparks
const sparks = () => [box('spark_a', [10.75, 45, 7.5], [11.75, 45, 8.5], { up: 'spark', down: 'spark' }, GLOW), box('spark_b', [4.5, 43.5, 9], [5.25, 43.5, 9.75], { up: 'spark', down: 'spark' }, GLOW)];
// the upgrade's orbit ring: 16 void-alloy segments round the core (the group tilts it 25 degrees)
function orbit() {
  const seg = [];
  for (let i = 0; i < 16; i++) { const a = (i / 16) * 2 * Math.PI, x = 8 + Math.cos(a) * 5.5, z = 8 + Math.sin(a) * 5.5; seg.push(box('orbit_' + i, [x - 0.6, 7.5, z - 0.6], [x + 0.6, 8.5, z + 0.6], 'va', { rot: ['y', -i * 22.5, [x, 8, z]] })); }
  return seg;
}

// ---------- the scene (only when run directly; the exporter takes the builders) ----------
if (require.main === module) {
const { cube, group, clip, write } = preview('clockwork_wastes_reactors', { 'w:': T });
const outliner = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const sub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));
const REST = {};

// a floor of Scorched End Stone, top faces only
{
  const floor = [];
  for (let x = -2; x <= 22; x++) for (let z = -3; z <= 3; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(x, 0, z)));
  outliner.push(group('floor', [8, 0, 8], [0, 0, 0], floor));
}

// the five pylon states, 2 blocks apart
const STATES = ['intact', 'cracked', 'failing', 'broken', 'relit'];
STATES.forEach((state, i) => {
  const off = at(i * 2, 0, 0), name = 'pylon_' + state, live = state !== 'broken';
  const shaft = pylonShaft(live), cr = crown(state), left = FINS_LEFT[state];
  const fins = [0, 1, 2, 3, 4, 5].map((k) => {
    const g = sub(`${name}_fin_${k}`, [8, 44, 8], off, k < left ? fin(k) : finStub(k));
    REST[`${name}_fin_${k}`] = [0, 60 * k + 30, 0];
    return g;
  });
  const crownKids = [...put(cr.cage, off), ...fins, sub(`${name}_core`, [8, 44, 8], off, [cr.core])];
  if (state === 'failing') crownKids.push(sub(`${name}_sparks`, [8, 44, 8], off, sparks()));
  const crownG = group(`${name}_crown`, [8, 40, 5], off, crownKids);
  if (state === 'broken') REST[`${name}_crown`] = [-20, 0, 0];
  // the floor groove from the collar toward the dais (north), lit while live
  const groove = pylonGroove();
  const kids = [...put([...shaft.body, ...groove], off), sub(`${name}_seams`, [8, 0, 8], off, shaft.seams), crownG];
  if (live) kids.push(sub(`${name}_groove_glow`, [8, 0, 8], off, pylonGrooveGlow()));
  outliner.push(group(name, [8, 0, 8], off, kids));
});

// the Derelict Star Reactor, centred on block (12, 0, 0), shown fuelled; its clips show it dormant and salvaged
{
  const off = at(12, 0, 0), kids = put(derelictBase(), off);
  kids.push(group('dsr_groove_turn', [8, 6, 8], off, [...put(grooves(), off), sub('dsr_grooves', [8, 6, 8], off, grooveGlow())]));
  REST.dsr_groove_turn = [0, 45, 0];
  for (let k = 0; k < 4; k++) {
    const g = group(`dsr_arm_${k}`, [8, 6, 8], off, [sub(`dsr_arm_fall_${k}`, [8, 6, -10], off, arm(k)), sub(`dsr_arm_seam_${k}`, [8, 6, 8], off, armSeam(k))]);
    REST[`dsr_arm_${k}`] = [0, 90 * k, 0];
    kids.push(g);
  }
  kids.push(sub('dsr_sphere', [8, 30, 8], off, sphere('sphere', ...SPH, 'smoke_glass')));
  // fuelled, a smaller sphere of light glows inside the glass (no faces shared with it)
  kids.push(sub('dsr_sphere_lit', [8, 30, 8], off, sphere('sphere_lit', 8, 30.5, 8, 4, 'sphere_lit').map((c) => ({ ...c, shade: false }))));
  kids.push(sub('dsr_star', [8, 30, 8], off, starItem('star', 8, 30, 8, 10)));
  outliner.push(group('derelict_star_reactor', [8, 0, 8], off, kids));
}

// three Star Reactors at blocks 16, 18, 20: fuelled, unfuelled, upgraded
[['star_reactor', 16, 'fuelled', 8], ['star_reactor_cold', 18, 'cold', 8], ['star_reactor_upgraded', 20, 'fuelled', 12]].forEach(([name, bx, mode, n]) => {
  const off = at(bx, 0, 0), lit = mode === 'fuelled';
  const kids = put(casing(), off);
  for (const dir of ['north', 'east', 'west']) kids.push(...put(portGlass(dir, 'port_glass', false), off));
  if (lit) kids.push(sub(`${name}_ports_lit`, [8, 8, 8], off, ['north', 'east', 'west'].flatMap((dir) => portGlass(dir, 'port_lit', true))));
  // the star sphere in its cage, and the star turning inside it
  kids.push(sub(`${name}_sphere`, [8, 8, 8], off, sphere('rsphere', 8, 8, 8, 4, 'smoke_glass')));
  if (lit) kids.push(sub(`${name}_star`, [8, 8, 8], off, starItem('rstar', 8, 8, 8, 6)));
  // the pip ring: dark pips always, lit ones over them
  const ring = PIPS(n);
  kids.push(...put(ring.map(([x, z], i) => pip(`pipd_${i}`, x, z, 'pip_dark', 16.25)), off));
  if (lit) ring.forEach(([x, z], i) => kids.push(sub(`${name}_pip_${i}`, [x, 16.5, z], off, [pip(`pip_${i}`, x, z, 'pip_lit', 16.5, GLOW)])));
  // the upgrade: a void-alloy orbit ring round the core, tilted 25 degrees, turning
  if (n === 12) {
    const seg = orbit();
    kids.push(group(`${name}_orbit_tilt`, [8, 8, 8], off, [sub(`${name}_orbit`, [8, 8, 8], off, seg)]));
    REST[`${name}_orbit_tilt`] = [25, 0, 0];
  }
  outliner.push(group(name, [8, 0, 8], off, kids));
});

// ---------- clips ----------
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const ease = (t, t0, t1) => (t <= t0 ? 0 : t >= t1 ? 1 : ((u) => u * u * (3 - 2 * u))((t - t0) / (t1 - t0)));
// a damped jolt: from 0 to 1 by t0 + d, overshooting and settling
const jolt = (t, t0, d = 0.15) => { if (t <= t0) return 0; const u = (t - t0) / d; return u < 1 ? u * u : 1 + 0.18 * Math.exp(-(u - 1) * 1.4) * Math.sin((u - 1) * 2.6); };
// six hits on the intact pylon, one fin per hit: each snaps, drops and tumbles off. Then the crown slumps; then at the
// Custodian's overload it jolts back upright and its core blazes before settling at the failing look.
{
  const HITS = [0.5, 1.1, 1.7, 2.3, 2.9, 3.5], tracks = {}, steps = {};
  HITS.forEach((h, k) => {
    tracks[`pylon_intact_fin_${k}`] = {
      position: (t) => { const u = Math.max(0, t - h); return t < h ? [0, 0, 0] : [0, -Math.min(40, 30 * u * u), -3 * Math.min(u, 0.6)]; },
      rotation: (t) => [t < h ? 0 : -Math.min(150, 260 * (t - h)), 0, 0],
    };
    steps[`pylon_intact_fin_${k}`] = { scale: [[0, SHOW], [h + 0.55, HIDE], [6.9, SHOW]] };
  });
  tracks.pylon_intact_crown = { rotation: (t) => [-20 * jolt(t, 3.6, 0.35) * (1 - jolt(t, 5.0, 0.12)), 0, 0] };
  steps.pylon_intact_seams = { scale: [[0, SHOW], [3.6, HIDE], [5.0, SHOW]] };
  steps.pylon_intact_groove_glow = { scale: [[0, SHOW], [3.6, HIDE], [5.0, SHOW]] };
  // the core dims as fins go, dies at the sixth, blazes at the overload (scaled up briefly), then holds at a flicker
  tracks.pylon_intact_core = { scale: (t) => { if (t < 3.5) return [1 - 0.08 * Math.floor((t + 0.1) / 0.6), 1 - 0.08 * Math.floor((t + 0.1) / 0.6), 1 - 0.08 * Math.floor((t + 0.1) / 0.6)]; if (t < 5.0) return [0.35, 0.35, 0.35]; const b = 1 + 0.6 * Math.exp(-(t - 5.0) * 5); const f = t > 5.6 && Math.floor(t * 9) % 4 === 0 ? 0.6 : 1; return [b * f, b * f, b * f]; } };
  clip('six_hits', 7, 'loop', tracks, steps);
}
// the Derelict Star Reactor: dormant (dark glass, no star), fuelled (the default), salvaged (arms fallen open, glass cracked)
{
  const dsr = (litOn, salvaged) => ({
    dsr_sphere_lit: { scale: [[0, litOn ? SHOW : HIDE]] }, dsr_star: { scale: [[0, litOn ? SHOW : HIDE]] }, dsr_grooves: { scale: [[0, litOn ? SHOW : HIDE]] },
    ...Object.fromEntries([0, 1, 2, 3].map((k) => [`dsr_arm_seam_${k}`, { scale: [[0, litOn ? SHOW : HIDE]] }])),
  });
  clip('derelict_dormant', 1, 'loop', {}, dsr(false, false));
  // fuelled: the star turns slowly, the glass pulses on a 4 s beat
  clip('derelict_fuelled', 8, 'loop', { dsr_star: { rotation: (t) => [0, -t * 45, 0] }, dsr_sphere_lit: { scale: (t) => { const p = 1 + 0.015 * Math.sin(t * Math.PI / 2); return [p, p, p]; } } },
    Object.fromEntries(Object.entries(dsr(true, false)).filter(([k]) => k !== 'dsr_sphere_lit')));
  const fall = Object.fromEntries([0, 1, 2, 3].map((k) => [`dsr_arm_fall_${k}`, { rotation: (t) => [-35 * jolt(t, 0.3, 0.5), 0, 0] }]));
  clip('derelict_salvaged', 2, 'hold', fall, dsr(false, true));
}
// the Star Reactor: the star turns once per 8 s; the burn readout counts down (one pip per hour; here one per second);
// the upgraded one's orbit ring turns once per 6 s
{
  const tracks = { star_reactor_star: { rotation: (t) => [0, -t * 45, 0] }, star_reactor_upgraded_star: { rotation: (t) => [0, -t * 45, 0] }, star_reactor_upgraded_orbit: { rotation: (t) => [0, -t * 60, 0] } };
  const steps = {};
  for (let i = 0; i < 8; i++) steps[`star_reactor_pip_${i}`] = { scale: [[0, SHOW], [i + 1, HIDE]] };
  for (let i = 0; i < 12; i++) steps[`star_reactor_upgraded_pip_${i}`] = { scale: [[0, SHOW], [(i + 1) * 0.75, HIDE]] };
  clip('reactor_burn', 9, 'loop', tracks, steps);
}
write(OUT, outliner);
// rest rotations the preview library doesn't set (fins round the core, the broken crown's slump, the arms round the
// reactor, the orbit ring's tilt): written into both the outliner and the group list
{
  const fs = require('fs'), bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
  bb.outliner.forEach(walk);
  for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
}
module.exports = { pylonShaft, crown, fin, finStub, FINS_LEFT, SECTIONS, pylonGroove, pylonGrooveGlow, sparks, derelictBase, grooves, grooveGlow, arm, armSeam,
  sphere, roundSlab, SPH, casing, sidePanel, portGlass, turnTo, PIPS, pip, orbit, starItem, shift, GLOW };
