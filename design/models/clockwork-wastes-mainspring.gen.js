// Clockwork Wastes: the Sprung Mainspring prop (D80, D81, design/briefs/clockwork-wastes-winding-tower.md) -> its pieces,
// planned as block models, the winding tower's sprung spring laid from them, and the tower's Fallen Rafter prop (D81).
// Preview only: no block is registered and nothing here is in the mod until the user approves the look.
//   node design/models/clockwork-wastes-mainspring.gen.js          the export checks, the laid pieces, a sketch from the west
//   node design/models/clockwork-wastes-mainspring.gen.js --java   the placeholder cell tables (WindingTower.SPRUNG_CELLS
//                                                                   and RAFTER_CELLS)
// required by clockwork-wastes-winding-tower.gen.js, which draws the pieces in the scene.
//
// Model space as a block model's: x 0 west -> 16 east, y up, z 0 north -> 16 south; every coordinate in -16..32 (a piece
// may reach into the blocks round its own); one-axis element rotations of 0, +-22.5 or +-45 only; facing and turns through
// the blockstate (x and y in 90 degree steps). The strip is real thin strip: 2 px thick, 32 px (2 blocks) wide, curved in
// chords of 22.5 degree steps (tangent polygons, so every chord's angle is a legal rotation). Its flat faces carry the
// strip with its rust runs (always running in from the strip's own edge: every face is mapped so v 0 is that edge), its thin
// edges and cut ends the edge tone; the dark between turns is the real gap between pieces.
const { png, hash, fitUv } = require('./process-box.lib.js');
const { turn, corners, texAt } = require('./kit-building-preview.js');

const T = 2; // strip thickness (px)
const W = 32; // strip width (px): two blocks

// ---------------------------------------------------------------- the palette (D81: aged)
// The approved spring steel (edge #4a525a, strip #6d7780, highlight #8a949c) taken a step down so the strip sits at the
// tower's value (Stained End Stone Brick #6c6750 / #746f57, mean luminance about 102): strip luminance about 94, never
// lighter than the brick. The old highlight is kept only for the scuffs where it scraped the wall. Rust is the house's
// (clockwork-wastes-materials.gen.js RUST).
const SS = { edge: '#3b4248', strip: '#565f67', hi: '#6b747c', scuff: '#8a949c' };
const RUST = ['#5a3523', '#6e4129', '#7f4d30'];

// ---------------------------------------------------------------- textures (u along the strip, v 0 at the strip's edge)
const rgba = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
const paint = (fn) => { const d = new Uint8Array(16 * 16 * 4); for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) d.set(rgba(fn(x, y)), (y * 16 + x) * 4); return png(16, 16, d); };
// Rust runs from the edge in: [u, length, width at the root]; the root the brightest rust, the body, then a darker tail.
// A few short highlights run along the strip ([u, v, n]). Scuffs: bright scratches along it ([u, v, n]).
function stripPaint(runs, dashes, scuffs = []) {
  return (u, v) => {
    for (const [u0, len, w] of runs) {
      const wide = w > 1 && v < len / 2;
      if (v < len && (u === u0 || (wide && u === u0 + 1))) return v === 0 ? RUST[2] : v < len - 2 ? RUST[1] : RUST[0];
    }
    if (scuffs.some(([u0, v0, n]) => v === v0 && u >= u0 && u < u0 + n)) return SS.scuff;
    if (dashes.some(([u0, v0, n]) => v === v0 && u >= u0 && u < u0 + n)) return SS.hi;
    return SS.strip;
  };
}
const TEX = {
  // most of the strip: plain, one short highlight
  plain: paint(stripPaint([], [[6, 10, 3]])),
  // where water sat (the tops of the turns, the lowest of the run down the wall): one or two runs in from the edge
  rust_a: paint(stripPaint([[3, 6, 2], [11, 3, 1]], [])),
  rust_b: paint(stripPaint([[7, 8, 2]], [])),
  rust_c: paint(stripPaint([[2, 4, 1], [10, 6, 1]], [])),
  // where it scraped the hole's lip and the wall: scratched bright along its length, the rust scraped off but for one run
  scuff: paint(stripPaint([[11, 3, 1]], [], [[1, 5, 6], [4, 8, 4], [8, 11, 5], [2, 13, 3]])),
  // the thin edge and the cut ends: the edge tone (pass 3's first try put rust dots on it: they read as stitching)
  edge: paint(() => SS.edge),
};
const STRIP_TEX = ['#rust_a', '#rust_b', '#rust_c'];

// ---------------------------------------------------------------- geometry
const AX = { x: 0, y: 1, z: 2 };
const r4 = (v) => Math.round(v * 1e4) / 1e4;
// right-hand rotation of a vector about an axis (degrees), as a block model element's rotation turns it
function rot(v, axis, deg) {
  const a = (deg * Math.PI) / 180, c = Math.cos(a), s = Math.sin(a), [x, y, z] = v;
  if (axis === 'x') return [x, y * c - z * s, y * s + z * c];
  if (axis === 'y') return [x * c + z * s, y, -x * s + z * c];
  return [x * c - y * s, x * s + y * c, z];
}
// One element of strip: a box `len` long along `along`, `T` thick along `thick`, over `w` along the third axis, centred at m,
// turned `deg` about the third axis; split into 16 px of width a piece, each half knowing which of its sides is the edge.
function slab(name, m, along, thick, wAxis, w, len, deg, ext = [0, 0], dir = null) {
  const out = [];
  for (let w0 = w[0]; w0 < w[1] - 1e-6; w0 += 16) {
    const w1 = Math.min(w[1], w0 + 16), from = [0, 0, 0], to = [0, 0, 0];
    from[AX[along]] = m[AX[along]] - len / 2 - ext[0]; to[AX[along]] = m[AX[along]] + len / 2 + ext[1];
    from[AX[thick]] = m[AX[thick]] - T / 2; to[AX[thick]] = m[AX[thick]] + T / 2;
    from[AX[wAxis]] = w0; to[AX[wAxis]] = w1;
    const origin = [...m]; origin[AX[wAxis]] = (w[0] + w[1]) / 2;
    out.push({ name, from: from.map(r4), to: to.map(r4), rotation: deg ? { origin: origin.map(r4), axis: wAxis, angle: deg } : null, along, thick, wAxis, edgeW: w0 === w[0] ? w0 : w1, dir });
  }
  return out;
}
// A chord of the strip's centre line from p to q (3D, differing in two axes; the third, wAxis, is across the strip)
function chord(name, p, q, wAxis, w, ext) {
  const d = [q[0] - p[0], q[1] - p[1], q[2] - p[2]], len = Math.hypot(...d), u = d.map((v) => v / len), m = p.map((v, i) => (v + q[i]) / 2);
  const plane = ['x', 'y', 'z'].filter((a) => a !== wAxis);
  for (const along of plane) for (const deg of [0, 22.5, -22.5, 45, -45]) {
    const base = [0, 0, 0]; base[AX[along]] = 1;
    const r = rot(base, wAxis, deg), dot = r[0] * u[0] + r[1] * u[1] + r[2] * u[2];
    if (Math.abs(Math.abs(dot) - 1) < 1e-6) return slab(name, m, along, plane.find((a) => a !== along), wAxis, w, len, deg, dot > 0 ? ext : [ext[1], ext[0]], u);
  }
  throw new Error(`${name}: chord ${JSON.stringify(d)} isn't at a legal angle`);
}
// A polyline of chords. joints[i] says whether point i is a kink to close (overlapped 0.3 px); by default every inner point
function run(name, pts, wAxis, w, joints) {
  const out = [], j = (i) => (joints ? joints[i] : i > 0 && i < pts.length - 1);
  for (let i = 1; i < pts.length; i++) {
    if (Math.hypot(...pts[i].map((v, k) => v - pts[i - 1][k])) < 0.05) continue;
    out.push(...chord(`${name}_${i}`, pts[i - 1], pts[i], wAxis, w, [j(i - 1) ? 0.3 : 0, j(i) ? 0.3 : 0]));
  }
  return out;
}
// The tangent polygon of an arc from angle a0 to a1 (degrees, in the plane's own (u, v) frame), radius r(t) for t 0..1:
// tangent lines every 22.5 degrees, so every chord is at a legal angle. P(u, v) places a point.
function arc(P, a0, a1, r, steps = Math.round(Math.abs(a1 - a0) / 22.5)) {
  const k = (i) => a0 + ((a1 - a0) * i) / steps, R = (i) => (typeof r === 'number' ? r : r(i / steps));
  const pt = (a, rr) => [rr * Math.cos((a * Math.PI) / 180), rr * Math.sin((a * Math.PI) / 180)];
  const tan = (a) => { const s = Math.sign(a1 - a0); return [-Math.sin((a * Math.PI) / 180) * s, Math.cos((a * Math.PI) / 180) * s]; };
  const out = [P(...pt(k(0), R(0)))];
  for (let i = 0; i < steps; i++) {
    const p1 = pt(k(i), R(i)), d1 = tan(k(i)), p2 = pt(k(i + 1), R(i + 1)), d2 = tan(k(i + 1));
    const det = d1[0] * -d2[1] - d1[1] * -d2[0], t = ((p2[0] - p1[0]) * -d2[1] - (p2[1] - p1[1]) * -d2[0]) / det;
    out.push(P(p1[0] + d1[0] * t, p1[1] + d1[1] * t));
  }
  out.push(P(...pt(k(steps), R(steps))));
  return out;
}

// ---------------------------------------------------------------- the pieces
// ports: { at: [x, y, z] (the centre line's end, at mid-width), out: unit vector out of the piece, w: the across axis }
const PIECES = {};
const piece = (name, desc, elements, ports, extra = {}) => { PIECES[name] = { name, desc, elements, ports, ...extra }; };
// Standing pieces: the strip in the drum's plane (x, y), across z 0..32 (two blocks), its centre line at a block's middle.
const standRun = (name, pts) => run(name, pts.map(([x, y]) => [x, y, 16]), 'z', [0, W]);
const standPort = (x, y, out) => ({ at: [x, y, 16], out, w: 'z' });
piece('straight', 'A straight run of strip, one block', standRun('straight', [[0, 8], [16, 8]]), [standPort(0, 8, [-1, 0, 0]), standPort(16, 8, [1, 0, 0])]);
piece('burst', 'Through the wall: three blocks of strip buckled up 4 px in the hole where it tore through (scuffed)',
  standRun('burst', [[32, 8], [14, 8], [10, 12], [6, 12], [2, 8], [-16, 8]]), [standPort(32, 8, [1, 0, 0]), standPort(-16, 8, [-1, 0, 0])], { scuffed: true });
// the turn left on the arbor (arbor centre 8 px over the lower piece's block top, 8 px under the upper's bottom): one
// clockwise turn opening from radius 10 to 16 px, out at the bottom heading west to the lower block's west edge
{
  const r = (a) => 10 + 6 * ((270 - a) / 360); // a from 270 (bottom, the inner end) down to -90 (bottom, the outer)
  const turnPts = (cx, cy, a0, a1) => arc((u, v) => [cx + u, cy + v], a0, a1, (t) => r(a0 + (a1 - a0) * t));
  piece('arbor_turn_upper', 'The turn on the arbor, its upper half (the block over the arbor)', standRun('arbor_upper', turnPts(8, -8, 180, 0)), []);
  piece('arbor_turn_lower', 'The turn on the arbor, its lower half and the strip leaving it (the block under the arbor)',
    [...standRun('arbor_lower_a', turnPts(8, 24, 270, 180)), ...standRun('arbor_lower_b', [...turnPts(8, 24, 0, -90), [0, 8]])], [standPort(0, 8, [-1, 0, 0])]);
}

// The sprung spiral (D81, D82): standing on its edge in the west wall's plane (z, y), its two blocks of width out from the
// wall (x), properly sprung: three tight turns at its centre, then the outer turns sprung well out and apart, uneven, the
// last sagging onto the ground and lying along it (north, -z) to the stones where the run comes down. Drawn by a turtle that
// turns 22.5 degrees a chord (clockwise seen from the west) with each chord as long as its radius of curvature asks
// (2 r tan 11.25), so every chord is at a legal angle; two of the loose chords hold their heading (kinks).
const SPIRAL = {
  x: -8, // its width: blocks x -8..-7, sprung clear of the wall
  tight: { turns: 3, r: [3.5, 16] }, // the centre's radius of curvature, from the inner tip out
  loose: { turns: 2.25, r: [26, 64], jitter: 0.35 }, // the sprung turns
  kinks: [7, 22], // loose chords that hold their heading
  sag: 96, // px of strip lying along the ground from the last turn's foot
  end: [56, 1], // (z, y) px: the sag's end, under the stones where the run comes down
};
function spiralPoints() {
  const { tight, loose, kinks, sag, end } = SPIRAL, k = 2 * Math.tan(Math.PI / 16), seq = [];
  const nT = tight.turns * 16, nL = Math.round(loose.turns * 16);
  for (let i = 0; i < nT; i++) seq.push({ len: k * (tight.r[0] + ((tight.r[1] - tight.r[0]) * i) / nT), turn: 1 });
  for (let i = 0; i < nL; i++) {
    const t = i / nL, rr = (loose.r[0] + (loose.r[1] - loose.r[0]) * Math.pow(t, 0.85)) * (1 + loose.jitter * Math.sin(i * 1.7 + 0.5) * (0.4 + t));
    seq.push(kinks.includes(i) ? { len: 7, turn: 0 } : { len: k * rr, turn: 1 });
  }
  // headings in (z, y), degrees; clockwise turns lower it; the last chord heads -z (180) along the ground
  const turns = seq.reduce((n, c) => n + c.turn, 0);
  let h = 180 + 22.5 * turns, p = [0, 0];
  const pts = [p];
  for (const c of seq) { h -= 22.5 * c.turn; const a = (h * Math.PI) / 180; p = [p[0] + c.len * Math.cos(a), p[1] + c.len * Math.sin(a)]; pts.push(p); }
  pts.push([p[0] - sag, p[1]]);
  // shifted so the sag lies on the ground (its centre line at y 1) and ends at `end`
  const last = pts[pts.length - 1], dz = end[0] - last[0], dy = end[1] - last[1];
  const out = pts.map(([z, y]) => [z + dz, y + dy]);
  const low = Math.min(...out.slice(0, -2).map((q) => q[1]));
  if (low < end[1] - 1e-6) throw new Error('the spiral dips under the ground at ' + low.toFixed(2));
  return out;
}
// The run peeling off the wall (D82): from the burst's west end (x -16, y 152) in the drum's plane (x, y), across z 3..4,
// arcing out and down away from the bricks in chords of 0, 22.5, 45 and 67.5 degrees, to the stones at the spiral's sag
const PEEL = [[-16, 152], [-24, 152], [-24 - 20 * Math.cos(Math.PI / 8), 152 - 20 * Math.sin(Math.PI / 8)]];
{
  let [x, y] = PEEL[2];
  x -= 32 * Math.SQRT1_2; y -= 32 * Math.SQRT1_2; PEEL.push([x, y]);
  const l = (y - 1) / Math.sin((3 * Math.PI) / 8);
  PEEL.push([x - l * Math.cos((3 * Math.PI) / 8), 1]);
}
// A polyline laid into 3 x 3-block sections (each one piece; every length keeps its chord's angle). plane 'zy'
// (the spiral: u z, v y, width x) or 'xy' (the peel: u x, v y, width z); grid lines every 48 px from u0 (rising or falling).
function sectionize(prefix, pts, plane, u0, uSign, wAnchor, scuffUntil = null, v0 = 0) {
  // Each chord in short lengths (12 px at most), each length wholly in the section its middle lies in; a length whose turned
  // box would reach past its section's -16..32 is halved until it doesn't. Lengths of one chord abut end to end (the same
  // angle, no gap, no overlap); the chord's ends at real kinks overlap their neighbours a little to close the kink.
  const secOf = (m) => [Math.floor((uSign * (m[0] - u0)) / 48), Math.floor((m[1] - v0) / 48)];
  const origin = (gi, gj) => [uSign > 0 ? u0 / 16 + 3 * gi + 1 : u0 / 16 - 3 * gi - 2, v0 / 16 + 3 * gj + 1];
  const local = (gi, gj) => { const [au, av] = origin(gi, gj); return plane === 'zy' ? (pt) => [16, pt[1] - av * 16, pt[0] - au * 16] : (pt) => [pt[0] - au * 16, pt[1] - av * 16, 16]; };
  const wAxis = plane === 'zy' ? 'x' : 'z';
  const fits = (els) => els.every((e) => [...e.from, ...e.to].every((v) => v >= -16 - 1e-6 && v <= 32 + 1e-6));
  const groups = new Map();
  let n = 0;
  const place = (p, q, kinkP, kinkQ, depth = 0) => {
    const m = [(p[0] + q[0]) / 2, (p[1] + q[1]) / 2], [gi, gj] = secOf(m), L = local(gi, gj);
    const els = run(`${prefix}_${n}`, [L(p), L(q)], wAxis, [0, W], [kinkP, kinkQ]);
    if (!fits(els) && depth < 8) { place(p, m, kinkP, false, depth + 1); place(m, q, false, kinkQ, depth + 1); return; }
    if (!fits(els)) throw new Error(`${prefix}: a length won't fit its section`);
    const key = gi + ',' + gj;
    if (!groups.has(key)) groups.set(key, { gi, gj, els: [] });
    groups.get(key).els.push(...els.map((e) => ({ ...e, world: m })));
    n++;
  };
  // whether the line turns at point i (a joint that doesn't turn is closed already: overlapping it would z-fight)
  const turns = (i) => { const d = (p, q) => Math.atan2(q[1] - p[1], q[0] - p[0]); return Math.abs(Math.sin(d(pts[i], pts[i + 1]) - d(pts[i - 1], pts[i]))) > 1e-6 || Math.cos(d(pts[i], pts[i + 1]) - d(pts[i - 1], pts[i])) < 0; };
  for (let i = 1; i < pts.length; i++) {
    const [a0, b0] = pts[i - 1], [a1, b1] = pts[i], k = Math.max(1, Math.ceil(Math.hypot(a1 - a0, b1 - b0) / 12));
    for (let j = 0; j < k; j++) {
      const P = (t) => [a0 + (a1 - a0) * t, b0 + (b1 - b0) * t];
      place(P(j / k), P((j + 1) / k), j === 0 && i > 1 && turns(i - 1), j === k - 1 && i < pts.length - 1 && turns(i));
    }
  }
  const out = [];
  for (const { gi, gj, els } of groups.values()) {
    if (!els.length) continue;
    // the section's middle block: u0 / 16 + (3 gi + 1) along a rising axis (u0 / 16 - 3 gi - 2 along a falling one), y 3 gj + 1
    const [au, av] = origin(gi, gj), at = plane === 'zy' ? [wAnchor, av, au] : [au, av, wAnchor];
    const name = `${prefix}_${gi}_${gj}`;
    piece(name, `${{ spiral: 'The sprung spiral', peel: 'The run peeling off the wall', fallen: 'A torn-off scrap of the peel, fallen on the stones' }[prefix]}, a 3 x 3-block section of it (${gi}, ${gj})`, els, [], { section: { plane, at }, scuffUntil });
    out.push({ name, at });
  }
  return out;
}
// the section grids (u, v of the first grid lines, px): chosen so every section's middle block, where its block stands, lies
// in or beside the strip's own blocks (the kit's checks want each block joined to the rest); the peel's last section
// (peel_2_0) lies under the stones and isn't laid
const GRID = { peel: [-16, 0], spiral: [0, 0] };
// The ruined tower's fallen peel (D87, option c): a torn-off scrap of the peel, come down with the wall, lying on the
// stone pile's top (y 16, the pile's two blocks x -8..-7) in the drum's plane across z 3..4, clear of the spiral's sag
// (which comes out from under the pile to the south): flat across the pile, its west end keeping a little of the strip's
// curl (22.5 then 45 degrees up). x px, y px over the ground. The pile has no top slab in the ruined ring.
const FALLEN = [[-98, 17], [-116, 17], [-116 - 8 * Math.cos(Math.PI / 8), 17 + 8 * Math.sin(Math.PI / 8)]];
FALLEN.push([FALLEN[2][0] - 8 * Math.SQRT1_2, FALLEN[2][1] + 8 * Math.SQRT1_2]);
const SECTIONS = [...sectionize('peel', PEEL, 'xy', GRID.peel[0], -1, 3, -48, GRID.peel[1]), ...sectionize('spiral', spiralPoints(), 'zy', GRID.spiral[0], 1, SPIRAL.x, null, GRID.spiral[1])];
const FALLEN_SECTIONS = sectionize('fallen', FALLEN, 'xy', -96, -1, 3);

// The Fallen Rafter (D81: real angled beams, not little wooden stairs): a walnut beam 8 px square, fallen in from the west
// rim, its upper end caught in the wall top, turned 45 or 22.5 degrees down into the drum; in 16 px lengths so the log's
// grain keeps one texel a pixel. Dark oak's own log textures.
function beam(name, c, len, deg) {
  const els = [], x0 = c[0] - len / 2;
  for (let s = 0; s < len - 1e-6; s += 16) {
    const l = Math.min(16, len - s);
    els.push({ name: `${name}_${s / 16}`, from: [x0 + s, c[1] - 4, c[2] - 4].map(r4), to: [x0 + s + l, c[1] + 4, c[2] + 4].map(r4),
      rotation: { origin: c.map(r4), axis: 'z', angle: deg }, beam: true, segStart: x0 + s });
  }
  return els;
}
piece('rafter_long', 'A fallen rafter: a walnut beam 46 px long, 8 px square, at 45 degrees', beam('rafter_long', [9, 11, 8], 46, -45), [], { rafter: true });
piece('rafter_short', 'A fallen rafter: a walnut beam 40 px long, 8 px square, at 22.5 degrees', beam('rafter_short', [10.5, 2.3, 8], 40, -22.5), [], { rafter: true });

// ---------------------------------------------------------------- faces
const FACE_AXES = { north: ['x', 'y'], south: ['x', 'y'], east: ['z', 'y'], west: ['z', 'y'], up: ['x', 'z'], down: ['x', 'z'] };
const NORMAL = { north: 'z', south: 'z', east: 'x', west: 'x', up: 'y', down: 'y' };
const same = (a, b) => Math.abs(a[0] - b[0]) < 1e-3 && Math.abs(a[1] - b[1]) < 1e-3;
// The UV rectangle and turn that put texture point tex(p) at each corner p of a face (the game's corner order)
function solveUv(dir, f, t, tex) {
  const C = corners(dir, f, t);
  if (!C) return { uv: fitUv(dir, f, t), rotation: 0 };
  const want = C.map(tex);
  for (let r = 0; r < 4; r++) {
    const idx = (s) => [0, 1, 2, 3].find((k) => (k + r) % 4 === s), a = want[idx(0)], b = want[idx(2)], cand = [a[0], a[1], b[0], b[1]].map(r4);
    if ([0, 1, 2, 3].every((k) => same(texAt(cand, r * 90, k), want[k]))) return { uv: cand, rotation: r * 90 };
  }
  throw new Error(`no UV for ${dir} ${f} ${t}`);
}
// Rust where water sat: on the sections, the chords lying within 22.5 degrees of level (their flat faces up: the tops and
// the insides of the turns' feet, the sag on the ground) and the peel's foot by the stones, every other one or so (by a
// hash). The peel's start, over the hole's lip, is scuffed (it scraped the wall). The other pieces are plain: one model each.
function rusty(p, e, i) {
  if (!p.section || !e.dir) return false;
  const level = Math.abs(e.dir[1]) < 0.39, low = e.world && e.world[1] < 40;
  return (level || (low && p.name.startsWith('peel'))) && hash(i, p.name.length, 13) > 0.45;
}
const scuffed = (p, e) => p.scuffed || (p.scuffUntil != null && e.world && e.world[0] > p.scuffUntil);
for (const p of Object.values(PIECES)) p.elements.forEach((e, i) => {
  e.faces = {};
  if (e.beam) {
    // the log's grain runs along the beam (v), across it 8 px of the side texture (u 4..12); its ends the log's top
    for (const d of Object.keys(FACE_AXES)) {
      if (NORMAL[d] === 'x') { e.faces[d] = { uv: [4, 4, 12, 12], texture: '#log_top' }; continue; }
      const across = NORMAL[d] === 'y' ? 'z' : 'y';
      e.faces[d] = { ...solveUv(d, e.from, e.to, (q) => [4 + q[AX[across]] - e.from[AX[across]], q[0] - e.from[0]]), texture: '#log' };
    }
    return;
  }
  const L = AX[e.along], len = e.to[L] - e.from[L], scale = len > 16 ? 16 / len : 1, off = len < 16 ? Math.floor(hash(i, p.name.length, 7) * (16 - len)) : 0;
  const tex = scuffed(p, e) ? '#scuff' : rusty(p, e, i) ? STRIP_TEX[Math.floor(hash(i, p.name.charCodeAt(0), 9) * 3)] : '#plain';
  for (const d of Object.keys(FACE_AXES)) {
    const n = NORMAL[d];
    if (n === e.thick) e.faces[d] = { ...solveUv(d, e.from, e.to, (q) => [off + (q[L] - e.from[L]) * scale, Math.abs(q[AX[e.wAxis]] - e.edgeW)]), texture: tex };
    else if (n === e.wAxis) e.faces[d] = { ...solveUv(d, e.from, e.to, (q) => [off + (q[L] - e.from[L]) * scale, q[AX[e.thick]] - e.from[AX[e.thick]]]), texture: '#edge' };
    else e.faces[d] = { uv: fitUv(d, e.from, e.to), texture: '#edge' };
  }
});

// ---------------------------------------------------------------- export checks (as block models)
function checks() {
  const issues = [];
  for (const p of Object.values(PIECES)) for (const e of p.elements) {
    for (const v of [...e.from, ...e.to]) if (v < -16 || v > 32) issues.push(`${p.name}:${e.name} out of -16..32 (${v})`);
    if (e.rotation && ![22.5, -22.5, 45, -45].includes(e.rotation.angle)) issues.push(`${p.name}:${e.name} rotation ${e.rotation.angle}`);
    for (const [d, f] of Object.entries(e.faces)) {
      const [a, b] = FACE_AXES[d], fw = e.to[AX[a]] - e.from[AX[a]], fh = e.to[AX[b]] - e.from[AX[b]];
      if (fw > 0 && fh > 0 && (Math.abs(f.uv[2] - f.uv[0]) < 1e-3 || Math.abs(f.uv[3] - f.uv[1]) < 1e-3)) issues.push(`${p.name}:${e.name} ${d} zero-width UV`);
      if (f.uv.some((v) => v < -1e-3 || v > 16 + 1e-3)) issues.push(`${p.name}:${e.name} ${d} UV outside 0..16`);
    }
  }
  return issues;
}

// ---------------------------------------------------------------- laying the spring (the winding tower's, in its local frame)
const V = (v) => v.map((c) => Math.round(c * 1e3) / 1e3);
const eq = (a, b) => a.every((v, i) => Math.abs(v - b[i]) < 1e-3);
function put(type, port, want) {
  const p = PIECES[type], fits = [];
  for (const rx of [0, 90, 180, 270]) for (const ry of [0, 90, 180, 270]) for (const [i, e] of p.ports.entries()) {
    if (!eq(V(turn(e.out, rx, ry, 0)), port.out.map((v) => -v))) continue;
    const wv = [0, 0, 0]; wv[AX[e.w]] = 1;
    const pw = [0, 0, 0]; pw[AX[port.w]] = 1;
    if (!eq(V(turn(wv, rx, ry, 0)).map(Math.abs), pw)) continue;
    const at = turn(e.at, rx, ry), anchor = port.at.map((v, k) => (v - at[k]) / 16);
    if (!anchor.every((v) => Math.abs(v - Math.round(v)) < 1e-6)) continue;
    const a = anchor.map(Math.round), o = p.ports[1 - i];
    let next = null;
    if (o) { const ow = [0, 0, 0]; ow[AX[o.w]] = 1; next = { at: V(turn(o.at, rx, ry).map((v, k) => v + a[k] * 16)), out: V(turn(o.out, rx, ry, 0)), w: 'xyz'[V(turn(ow, rx, ry, 0)).findIndex((c) => Math.abs(c) > 0.5)] }; }
    fits.push({ type, at: a, rx, ry, next });
  }
  const f = fits.find((c) => !want || (c.next && eq(c.next.out, want)));
  if (!f) throw new Error(`${type} doesn't fit at ${JSON.stringify(port)}${want ? ' going ' + want : ''}`);
  return f;
}
// The lay (brief, "How it lies"): the turn on the arbor, three straights west, the burst through the hole; the run peeling off
// the wall in an arc down to the stones; the sprung spiral's sections standing off the wall, its sag along the ground.
function lay() {
  const out = [];
  const add = (f) => { out.push(f); return f.next; };
  out.push({ type: 'arbor_turn_upper', at: [5, 11, 3], rx: 0, ry: 0 });
  out.push({ type: 'arbor_turn_lower', at: [5, 9, 3], rx: 0, ry: 0 });
  let port = { at: [80, 152, 64], out: [-1, 0, 0], w: 'z' };
  for (let i = 0; i < 3; i++) port = add(put('straight', port, [-1, 0, 0]));
  port = add(put('burst', port, [-1, 0, 0]));
  if (!eq(port.at, [PEEL[0][0], PEEL[0][1], 64])) throw new Error('the peel does not start where the burst ends: ' + port.at);
  // (the peel's last scrap, peel_2_0, lies wholly under the stones where it comes down: not laid, so not a block)
  for (const s of SECTIONS) if (s.name !== 'peel_2_0') out.push({ type: s.name, at: s.at, rx: 0, ry: 0 });
  return out;
}
// The rafters: their anchors in the tower's frame (the long one at z 3, the short at z 7)
// the ruined ring's fallen peel, laid by the tower in place of the standing peel
const layFallen = () => FALLEN_SECTIONS.map((s) => ({ type: s.name, at: s.at, rx: 0, ry: 0 }));
const RAFTERS = [{ type: 'rafter_long', at: [2, 18, 3], rx: 0, ry: 0 }, { type: 'rafter_short', at: [2, 19, 7], rx: 0, ry: 0 }];

// The cells a laid piece passes through (sampled through its turned boxes), less the arbor's blocks and the wall's
const ARBOR = new Set(['5,10,3', '5,10,4']);
function cellsOf(placed, skip = (c) => ARBOR.has(c.join(','))) {
  const cells = new Set();
  for (const pc of placed) for (const e of PIECES[pc.type].elements) {
    const n = [0, 1, 2].map((i) => Math.max(2, Math.ceil((e.to[i] - e.from[i]) / 2)));
    const thick = e.thick ? AX[e.thick] : -1;
    for (let i = 0; i <= n[0]; i++) for (let j = 0; j <= n[1]; j++) for (let k = 0; k <= n[2]; k++) {
      let p = [e.from[0] + ((e.to[0] - e.from[0]) * i) / n[0], e.from[1] + ((e.to[1] - e.from[1]) * j) / n[1], e.from[2] + ((e.to[2] - e.from[2]) * k) / n[2]];
      // a hair inside the box, so a face lying on a block's edge doesn't claim the next block
      p = p.map((v, a) => v + (a === thick ? 0 : v === e.from[a] ? 0.05 : v === e.to[a] ? -0.05 : 0));
      if (e.rotation) { const o = e.rotation.origin; p = rot([p[0] - o[0], p[1] - o[1], p[2] - o[2]], e.rotation.axis, e.rotation.angle).map((v, a) => v + o[a]); }
      const q = turn(p, pc.rx, pc.ry).map((v, a) => Math.floor((v + pc.at[a] * 16) / 16));
      q[1] = Math.max(0, q[1]);
      if (!skip(q)) cells.add(q.join(','));
    }
  }
  return [...cells].map((s) => s.split(',').map(Number)).sort((a, b) => a[1] - b[1] || a[0] - b[0] || a[2] - b[2]);
}
const rafterCells = () => cellsOf(RAFTERS, (c) => c[0] <= 1);

module.exports = { PIECES, TEX, SS, RUST, SPIRAL, W, T, checks, lay, layFallen, cellsOf, put, RAFTERS, rafterCells };

if (require.main === module) {
  const placed = lay(), cells = cellsOf(placed);
  const table = (cs) => { const rows = []; for (let i = 0; i < cs.length; i += 12) rows.push('\t\t' + cs.slice(i, i + 12).map((c) => c.join(', ')).join(', ') + ','); return rows.join('\n'); };
  if (process.argv.includes('--java')) {
    console.log('// SPRUNG_CELLS\n' + table(cells) + '\n// RAFTER_CELLS\n' + table(rafterCells()));
    process.exit(0);
  }
  const issues = checks();
  console.log(issues.length ? issues.join('\n') : 'export limits: ok (bounds -16..32, one-axis turns of 22.5 or 45, no zero-width UVs, UVs in 0..16)');
  console.log(Object.values(PIECES).map((p) => `${p.name}: ${p.elements.length}`).join(', '));
  console.log(placed.length, 'pieces laid,', cells.length, 'cells; rafters', rafterCells().length, 'cells');
  // the cells seen from the west (z across, y up): # spiral (x -4..-3), | the drop and the rest
  const g = new Map();
  for (const c of cells) { const k = c[2] + ',' + c[1]; if (c[0] <= -3) g.set(k, '#'); else if (!g.has(k)) g.set(k, '|'); }
  for (let y = 12; y >= 0; y--) { let s = ''; for (let z = -1; z <= 8; z++) s += g.get(z + ',' + y) || '.'; console.log(String(y).padStart(3), s); }
}
