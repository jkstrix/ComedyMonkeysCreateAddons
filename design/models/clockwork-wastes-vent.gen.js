// Clockwork Wastes wall vent preview -> design/models/clockwork-wastes-vent.bbmodel (Generic Model preview).
// Spec D57: called Scuttlers burst out of wall vents the city places in the heart hall's walls (P35). A new block, in the
// uprising's language (world brief: void-alloy, tall chamfered rectangles, light in hairline seams, cast and fitted, never
// fastened). Preview only, for the user's approval.
// node clockwork-wastes-vent.gen.js   (run clockwork-wastes-bots.gen.js scuttler first: the burst scene embeds its vent_burst clip)
// Block model space: x 0 west -> 16 east, y up, z 0 north (the face, into the hall) -> 16 south (into the wall).
// The block: a wall block with a tall chamfered opening at the floor (12 wide, 15 tall, 15 deep) closed by a louvred hatch hinged
// along its bottom edge. A starlight seam traces the hatch. States: sealed (the heart asleep: seam dark), live (the heart
// awake: seam lit dim, and a dull lens catchlight behind the louvres), open (spent: the hatch lies flat on the floor in
// front, the cavity dark, the seam out).
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, box } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-vent.bbmodel');
const GLOW = { shade: false };
// the cavity: darker than the outside, a faint sheen so it reads as metal in shadow, not a hole
T.paint('vent_cavity', (x, y) => ((x + y) % 16 < 3 ? '#1a1920' : hash(x, y, 71) > 0.9 ? '#16151c' : '#121118'));
T.paint('vent_seam_dim', () => STAR.dim);
T.paint('vent_flash', () => STAR.core);
// a dull catchlight: the curled Scuttler's lens glimpsed between the louvres
T.paint('vent_glint', (x, y) => (x === 7 && y === 7 ? VA.glint : VA.glass));

// the opening, sized to a curled Scuttler (12 x 12 x 13 px): 12 wide, 15 tall, chamfered top corners (two 1 px steps):
// rows y 0-13 x 2-14, y 13-14 x 3-13, y 14-15 x 4-12; the cavity runs 15 px into the wall (a 1 px back)
const ROWS = [[0, 13, 2, 14], [13, 14, 3, 13], [14, 15, 4, 12]];
const DEPTH = 15; // the cavity's depth into the wall
function ventBody() {
  const tile = { all: M.VA_BODY }, inner = (side) => ({ all: M.VA_BODY, [side]: 'vent_cavity' });
  const out = [
    box('jamb_w', [0, 0, 0], [2, 16, 16], inner('east')), box('jamb_e', [14, 0, 0], [16, 16, 16], inner('west')),
    box('lintel', [2, 15, 0], [14, 16, 16], { ...tile, down: 'vent_cavity' }),
    // the chamfer steps in the top corners
    box('chamfer_w1', [2, 13, 0], [3, 15, DEPTH], { all: M.VA_BODY, east: 'vent_cavity', down: 'vent_cavity' }), box('chamfer_w2', [3, 14, 0], [4, 15, DEPTH], { all: M.VA_BODY, east: 'vent_cavity', down: 'vent_cavity' }),
    box('chamfer_e1', [13, 13, 0], [14, 15, DEPTH], { all: M.VA_BODY, west: 'vent_cavity', down: 'vent_cavity' }), box('chamfer_e2', [12, 14, 0], [13, 15, DEPTH], { all: M.VA_BODY, west: 'vent_cavity', down: 'vent_cavity' }),
    box('back', [2, 0, DEPTH], [14, 15, 16], { ...tile, north: 'vent_cavity' }),
    // a frame lip 1 px proud round the opening, following the chamfer (the city's cast edge, no fasteners)
    box('lip_w', [1, 0, -1], [2, 13, 0], 'va'), box('lip_e', [14, 0, -1], [15, 13, 0], 'va'),
    box('lip_w1', [1, 13, -1], [3, 14, 0], 'va'), box('lip_e1', [13, 13, -1], [15, 14, 0], 'va'),
    box('lip_w2', [2, 14, -1], [4, 15, 0], 'va'), box('lip_e2', [12, 14, -1], [14, 15, 0], 'va'),
    box('lip_top', [3, 15, -1], [13, 16, 0], 'va'),
  ];
  return out;
}
// the charging contact at the back of the cavity (what the Scuttlers nest against), dark or lit dim
const contact = (lit) => [box('contact', [6, 4, DEPTH - 0.25], [10, 8, DEPTH - 0.25], { north: lit ? 'vent_seam_dim' : 'seam_strip_dark' }, lit ? GLOW : {})];
// the hatch: a frame, a mullion and five louvres angled down and out, built closed in the opening (z 0-1.5), hinged at its
// bottom front edge (8, 0, 0) so it falls forward and lies flat on the floor when blown
const HINGE = [8, 0, 0];
function hatch() {
  const out = [
    box('h_w', [2, 0, 0], [3, 13, 1.5], 'va'), box('h_e', [13, 0, 0], [14, 13, 1.5], 'va'), box('h_bottom', [3, 0, 0], [13, 1, 1.5], 'va'),
    box('h_w1', [3, 13, 0], [4, 14, 1.5], 'va'), box('h_e1', [12, 13, 0], [13, 14, 1.5], 'va'), box('h_top', [4, 14, 0], [12, 15, 1.5], 'va'),
    box('h_mullion', [7.5, 1, 0.75], [8.5, 14, 1.5], 'va'),
  ];
  // louvres: slats 10 wide (the top one 8), 2 tall, tilted 35 degrees down and out (between them, the cavity's dark)
  [2.5, 5, 7.5, 10, 12.5].forEach((y, k) => out.push(box('louvre_' + k, [k === 4 ? 4 : 3, y - 1, 0.25], [k === 4 ? 12 : 13, y + 1, 1.0], 'va', { rot: ['x', 35, [8, y, 0.6]] })));
  return out;
}
// the seam round the hatch's face, inset half a pixel from its edge (lit, the edge tone, while the heart is awake; the core
// tone flares through it at the burst)
function hatchSeam(tex, o = {}) {
  return [box('s_w', [2.25, 0.5, -0.05], [2.75, 12.75, -0.05], { north: tex }, o), box('s_e', [13.25, 0.5, -0.05], [13.75, 12.75, -0.05], { north: tex }, o),
    box('s_w1', [2.75, 12.75, -0.05], [3.75, 13.25, -0.05], { north: tex }, o), box('s_e1', [12.25, 12.75, -0.05], [13.25, 13.25, -0.05], { north: tex }, o),
    box('s_w2', [3.75, 13.25, -0.05], [4.25, 14.25, -0.05], { north: tex }, o), box('s_e2', [11.75, 13.25, -0.05], [12.25, 14.25, -0.05], { north: tex }, o),
    box('s_top', [4.25, 14.25, -0.05], [11.75, 14.75, -0.05], { north: tex }, o), box('s_bottom', [2.75, 0.25, -0.05], [13.25, 0.75, -0.05], { north: tex }, o)];
}
// the glint: a 2 x 2 dull lens low in the cavity, seen between the louvres
const glint = () => [box('glint', [7, 3, 9], [9, 5, 9], { north: 'vent_glint' }, { uv: { north: [6, 6, 8, 8] } })];

module.exports = { ventBody, contact, hatch, hatchSeam, glint, HINGE, ROWS, DEPTH };

if (require.main === module) {
const fs = require('fs');
const { embedBot } = require('./clockwork-wastes-embed.lib.js');
const { cube, group, clip, write } = preview('clockwork_wastes_vent', { 'w:': T });
const outliner = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const put = (list, off) => list.map((c) => cube(c, off));
const sub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));
const REST = {};
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];

// a strip of wall and floor for each vent: Void-Alloy Plate blocks either side and above, the hall's floor in front
function wallAround(name, bx) {
  const kids = [];
  for (const dx of [-1, 1]) kids.push(...put([box(`${name}_wall_${dx}`, [0, 0, 0], [16, 16, 16], { all: 'va_plain' })], at(bx + dx, 0, 0)));
  for (const dx of [-1, 0, 1]) kids.push(...put([box(`${name}_wall_up_${dx}`, [0, 0, 0], [16, 16, 16], { all: 'va_plain' })], at(bx + dx, 1, 0)));
  for (const dx of [-1, 0, 1]) for (const dz of [-1, -2]) kids.push(...put([box(`${name}_floor_${dx}_${dz}`, [0, -1, 0], [16, 0, 16], { up: 'va_plain', all: null })], at(bx + dx, 0, dz)));
  return group(name + '_setting', [8, 0, 8], at(bx, 0, 0), kids);
}
// four vents, 4 blocks apart: sealed, live, open (spent), and one that bursts (its clip, with a Scuttler)
const VENTS = [['sealed', 0], ['live', 4], ['open', 8], ['burst', 12]];
for (const [state, bx] of VENTS) {
  const off = at(bx, 0, 0), name = 'vent_' + state, lit = state === 'live' || state === 'burst';
  outliner.push(wallAround(name, bx));
  const kids = [...put(ventBody(), off), ...put(contact(false), off)];
  if (lit) kids.push(sub(`${name}_contact_lit`, [8, 5, DEPTH], off, contact(true)));
  if (lit) kids.push(sub(`${name}_glint`, [8, 4, 9], off, glint()));
  const hKids = [...put(hatch(), off), ...put(hatchSeam('seam_strip_dark'), off)];
  if (lit) hKids.push(sub(`${name}_seam_lit`, [8, 6, 0], off, hatchSeam('seam_strip_lit', GLOW).map((c) => ({ ...c, name: c.name + '_g', from: [c.from[0], c.from[1], -0.3], to: [c.to[0], c.to[1], -0.3] }))));
  if (state === 'burst') hKids.push(sub(`${name}_flash`, [8, 6, 0], off, hatchSeam('vent_flash', GLOW).map((c) => ({ ...c, name: c.name + '_f', from: [c.from[0], c.from[1], -0.4], to: [c.to[0], c.to[1], -0.4] }))));
  kids.push(group(`${name}_hatch`, HINGE, off, hKids));
  if (state === 'open') REST[`${name}_hatch`] = [-90, 0, 0];
  outliner.push(group(name, [8, 0, 8], off, kids));
}

// ---------- the burst ----------
// 0.1 s: the seam flares to the core tone; 0.15-0.42: the hatch rattles (something kicking inside), the glint flickers;
// 0.45: it blows: the hatch slams down flat onto the floor (0.1 s) and bounces twice; the seam goes out (spent); the
// Scuttler (its vent_burst clip, shifted 0.3 s) tumbles out over it and lands a block out, past the hatch
{
  const ease = (u) => (u <= 0 ? 0 : u >= 1 ? 1 : u * u);
  const ang = (t) => {
    if (t < 0.15) return 0;
    if (t < 0.45) return 2.5 * Math.sin((t - 0.15) * 70) * (t > 0.3 ? 1.6 : 1);
    const u = (t - 0.45) / 0.1; if (u < 1) return -90 * ease(u);
    const b = t - 0.55; return -90 + 14 * Math.exp(-7 * b) * Math.abs(Math.sin(b * 16));
  };
  const tracks = { vent_burst_hatch: { rotation: (t) => [ang(t), 0, 0] } };
  const steps = {
    vent_burst_seam_lit: { scale: [[0, SHOW], [0.1, HIDE], [0.3, SHOW], [0.45, HIDE]] },
    vent_burst_flash: { scale: [[0, HIDE], [0.1, SHOW], [0.3, HIDE]] },
    vent_burst_glint: { scale: [[0, SHOW], [0.2, HIDE], [0.26, SHOW], [0.36, HIDE]] },
    vent_burst_contact_lit: { scale: [[0, SHOW], [0.45, HIDE]] },
  };
  clip('burst', 2.6, 'hold', tracks, steps);
  // the live vent's idle: the glint shifts now and then (it's alive in there), the seam steady
  clip('live', 4, 'loop', {}, { vent_live_glint: { scale: [[0, SHOW], [1.6, HIDE], [1.75, SHOW], [3.1, HIDE], [3.2, SHOW]] } });
}
write(OUT, outliner);
const bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
const walk = (n) => { if (n && typeof n === 'object' && n.children) { if (REST[n.name]) n.rotation = REST[n.name]; n.children.forEach(walk); } };
bb.outliner.forEach(walk);
for (const g of bb.groups) if (REST[g.name]) g.rotation = REST[g.name];
// the Scuttler: its vent_burst clip starts curled in the cavity, 29.3 px behind where it lands (1.25 blocks out, past the
// fallen hatch),
// shifted so it bursts as the hatch blows. In Generic Model space the burst vent's block centre is (12 x 16, 8, 0).
embedBot(bb, path.resolve(__dirname, 'clockwork-wastes-scuttler.bbmodel'), { name: 'scuttler_place', offset: [12 * 16, 0, -28] }, { burst: { from: 'vent_burst', shift: 0.3 } });
fs.writeFileSync(OUT, JSON.stringify(bb));
console.error('vent scene: 4 vents (sealed, live, open, burst) and a Scuttler');
}
