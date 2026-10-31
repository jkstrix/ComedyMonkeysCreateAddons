// Clockwork Wastes lift preview -> design/models/clockwork-wastes-lift.bbmodel (Generic Model preview).
// The city's lifts (spec P49, D10, D24; world brief: "stairs are rare (they used ramps and lifts)", "a lift halfway up its
// shaft" in the heart's frozen shift). Uprising work: a 2 x 2 void-alloy deck (the automatons' doorways are 2 wide) that
// rides four guide rails between landings, a waist-high guard on its two closed sides (open front and back for the doors),
// a control post with a lens-in-cog plate, clamps that bite the rails, a call plate at each landing, and a drive drum at
// the head of the shaft (round: it's the lift's motor). Dark until its district wakes; then its seams light, and it moves
// only when someone standing on it (or at a call plate) calls it, stopping if anything is in the way.
// node clockwork-wastes-lift.gen.js
const path = require('path');
const { hash, preview } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const { T, VA, STAR, box, zdisc } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-lift.bbmodel');
const GLOW = { shade: false };
const FLOOR = 80; // the upper landing, 5 blocks up

// the call plate: a lens-in-cog, 6 x 6 at uv 0-6 (the Substation's sigil), dark and lit
const SIGIL = ['#.##.#', '.####.', '##oo##', '##oo##', '.####.', '#.##.#'];
T.paint('lift_sigil', (x, y) => { const k = (SIGIL[y] || '')[x]; return k === '#' ? (x + y < 5 ? VA.hi : VA.seam) : k === 'o' ? VA.glass : k === '.' ? VA.lo : M.va(x, y); });
T.paint('lift_sigil_lit', (x, y) => ((SIGIL[y] || '')[x] === 'o' ? (x === 2 && y === 2 ? STAR.core : STAR.edge) : null));
T.paint('flash', () => STAR.core);

// ---------- the lift ----------
// the deck, 32 x 32 px (2 x 2 blocks), its lit groove inset 1 px round the edge, cross beams under it
function deck() {
  return [box('deck', [0, 0, 0], [32, 2, 32], { all: 'va', up: M.VA_BODY }),
    box('beam_x', [2, -2, 14], [30, 0, 18], 'va'), box('beam_z', [14, -2, 2], [18, 0, 30], 'va'),
    // the guard on the closed sides (west and east): posts at the corners and a top rail at a standing automaton's waist
    ...[[0, 0], [0, 30], [30, 0], [30, 30]].map(([x, z], i) => box('post_' + i, [x, 2, z], [x + 2, 18, z + 2], 'va')),
    box('rail_w', [0, 18, 0], [2, 20, 32], 'va'), box('rail_e', [30, 18, 0], [32, 20, 32], 'va'),
    box('mid_w', [0.5, 10, 2], [1.5, 11, 30], 'va'), box('mid_e', [30.5, 10, 2], [31.5, 11, 30], 'va'),
    // the control post: a chamfered pedestal at the north-east corner, its lens-in-cog plate facing in (west)
    box('ctrl', [25, 2, 3], [29, 14, 7], 'va'), box('ctrl_cap', [25.5, 14, 3.5], [28.5, 15, 6.5], 'va'),
    box('ctrl_plate', [24.75, 7, 2], [24.75, 13, 8], { west: 'lift_sigil' }, { uv: { west: [0, 0, 6, 6] } }),
    // four clamps that bite the guide rails, at the deck's sides
    ...[[-3, 4], [-3, 24], [31, 4], [31, 24]].map(([x, z], i) => box('clamp_' + i, [x, -1, z], [x + 4, 3, z + 4], 'va'))];
}
const deckGlow = () => [
  box('groove_n', [1, 2.25, 1], [31, 2.25, 1.75], { up: 'seam_strip_lit' }, GLOW), box('groove_s', [1, 2.25, 30.25], [31, 2.25, 31], { up: 'seam_strip_lit' }, GLOW),
  box('groove_w', [1, 2.25, 1.75], [1.75, 2.25, 30.25], { up: 'seam_strip_lit' }, GLOW), box('groove_e', [30.25, 2.25, 1.75], [31, 2.25, 30.25], { up: 'seam_strip_lit' }, GLOW),
  box('ctrl_lens', [24.5, 7, 2], [24.5, 13, 8], { west: 'lift_sigil_lit' }, { ...GLOW, uv: { west: [0, 0, 6, 6] } }),
  ...[[-3, 4], [-3, 24], [31, 4], [31, 24]].map(([x, z], i) => box('clamp_pip_' + i, [x + 1.5, 0.5, z - 0.25], [x + 2.5, 1.5, z - 0.25], { north: 'flash' }, GLOW)),
];
// the shaft: four guide rails (2 x 2, a seam up each facing the deck), a head frame with the drive drum, a pit collar
function shaft(height) {
  const out = [];
  [[-2, 5], [-2, 25], [32, 5], [32, 25]].forEach(([x, z], i) => {
    out.push(box('guide_' + i, [x, -4, z], [x + 2, height, z + 2], M.VA_BODY));
  });
  out.push(box('head_w', [-3, height, 0], [-1, height + 4, 32], 'va'), box('head_e', [33, height, 0], [35, height + 4, 32], 'va'),
    box('head_n', [-3, height, -2], [35, height + 3, 0], 'va'), box('head_s', [-3, height, 32], [35, height + 3, 34], 'va'),
    ...zdisc('drum', 16, height + 6, -1, 3, 8, 'va'), box('drum_axle', [14, height + 4, -3], [18, height + 8, -1], 'va'),
    box('pit_n', [-3, -4, -2], [35, -3, 0], 'va'), box('pit_s', [-3, -4, 32], [35, -3, 34], 'va'));
  return out;
}
const shaftGlow = (height) => [[-2, 5], [-2, 25], [32, 5], [32, 25]].map(([x, z], i) => {
  const inward = x < 0 ? 'east' : 'west', fx = x < 0 ? x + 2.25 : x - 0.25;
  return box('guide_seam_' + i, [fx, -3, z + 0.75], [fx, height - 1, z + 1.25], { [inward]: 'seam_strip_lit' }, GLOW);
});
// a landing's call plate on a wall beside the shaft door: the lens-in-cog on a small void-alloy mount
const callPlate = (name) => [box(name + '_mount', [0, 0, 0], [8, 8, 1], 'va'), box(name + '_plate', [1, 1, -0.25], [7, 7, -0.25], { north: 'lift_sigil' }, { uv: { north: [0, 0, 6, 6] } })];
const callGlow = (name) => [box(name + '_lens', [1, 1, -0.5], [7, 7, -0.5], { north: 'lift_sigil_lit' }, { ...GLOW, uv: { north: [0, 0, 6, 6] } })];

module.exports = { deck, deckGlow, shaft, shaftGlow, callPlate, callGlow, FLOOR };

// the preview scene, only when run directly (the mod's exporter takes the builders above)
if (require.main === module) {
// ---------- the scene: a 2 x 2 shaft between the ground and a landing 5 blocks up, in End Stone brick walls ----------
const { cube, group, clip, write } = preview('clockwork_wastes_lift', { 'w:': T });
const outliner = [], put = (list, off) => list.map((c) => cube(c, off));
const sub = (name, pivot, off, list) => group(name, pivot, off, put(list, off));
{
  const floor = [];
  for (let x = -3; x <= 4; x++) for (let z = -3; z <= 4; z++) floor.push(cube(box(`floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (x * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), [x * 16, -4, z * 16]));
  outliner.push(group('floor', [0, 0, 0], [0, 0, 0], floor));
}
// the upper landing: a brick walkway on the shaft's north side, the wall behind it with the call plate
{
  const land = [box('landing', [-24, FLOOR - 4, -40], [56, FLOOR, -2], { all: 'endbrick', up: 'endbrick' }),
    box('landing_wall', [-24, FLOOR, -44], [56, FLOOR + 40, -40], 'endbrick'),
    box('ground_wall', [-24, -4, -44], [56, FLOOR - 4, -40], 'endbrick')];
  outliner.push(group('landing', [0, 0, 0], [0, 0, 0], put(land, [0, 0, 0])));
  outliner.push(group('call_up', [0, 0, 0], [0, 0, 0], [...put(callPlate('call_up'), [36, FLOOR + 10, -39.75]), sub('g_call_up', [0, 0, 0], [36, FLOOR + 10, -39.75], callGlow('call_up'))]));
  outliner.push(group('call_down', [0, 0, 0], [0, 0, 0], [...put(callPlate('call_down'), [36, 10, -39.75]), sub('g_call_down', [0, 0, 0], [36, 10, -39.75], callGlow('call_down'))]));
}
outliner.push(group('shaft', [16, 0, 16], [0, 0, 0], [...put(shaft(FLOOR + 40), [0, 0, 0]), sub('g_shaft', [16, 0, 16], [0, 0, 0], shaftGlow(FLOOR + 40))]));
// the lift, halfway up its shaft in the rest pose (the heart's frozen shift)
outliner.push(group('lift', [16, 0, 16], [0, FLOOR / 2, 0], [...put(deck(), [0, FLOOR / 2, 0]), sub('g_lift', [16, 0, 16], [0, FLOOR / 2, 0], deckGlow())]));

// ---------- clips ----------
const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const ease = (t, a, b) => (t <= a ? 0 : t >= b ? 1 : ((u) => u * u * (3 - 2 * u))((t - a) / (b - a)));
const GLOWS = ['g_lift', 'g_shaft', 'g_call_up', 'g_call_down'];
// dormant: dark, frozen halfway; awake: lit
clip('dormant', 1, 'loop', {}, Object.fromEntries(GLOWS.map((g) => [g, { scale: [[0, HIDE]] }])));
clip('awake', 1, 'loop', {}, Object.fromEntries(GLOWS.map((g) => [g, { scale: [[0, SHOW]] }])));
// ride: called from the upper landing it comes up (a small settle as the clamps catch), waits, then rides down; about
// 1 block a second each way. Its position runs from halfway (rest) down to the ground, up to the landing and back.
{
  const y = (t) => {
    if (t < 1.2) return -FLOOR / 2 * ease(t, 0, 1.2);
    if (t < 2.0) return -FLOOR / 2;
    if (t < 7.0) { const u = ease(t, 2.0, 7.0); return -FLOOR / 2 + FLOOR * u + (t > 6.6 ? -0.6 * Math.sin((t - 6.6) / 0.4 * Math.PI) : 0); }
    if (t < 8.0) return FLOOR / 2;
    return FLOOR / 2 - FLOOR / 2 * ease(t, 8.0, 10.5);
  };
  const steps = Object.fromEntries(GLOWS.map((g) => [g, { scale: [[0, SHOW]] }]));
  steps.g_call_up = { scale: [[0, HIDE], [1.5, SHOW], [7.4, HIDE]] };
  clip('ride', 11, 'loop', { lift: { position: (t) => [0, y(t), 0] } }, steps);
}
write(OUT, outliner);
}
