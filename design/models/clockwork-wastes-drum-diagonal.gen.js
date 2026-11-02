// Clockwork Wastes: the heart drum's diagonal flats as one flat 45-degree face (D86, choice 30)
//   -> design/models/clockwork-wastes-drum-diagonal.bbmodel
// The brick diagonal (D88) and the cut deepslate course forms (D89) are approved and exported into the mod by --export.
//
// The drum is an octagon, and on its four diagonals every block, stair or slab can only make a staircase of 1-block steps,
// so a recessed panel cut into them couldn't read (pass 3). The proposed block, a "Stained End Stone Brick Diagonal", is a
// half cell cut on the 45-degree line through two of its corners: a triangular prism made as one block-model box turned
// 45 degrees about y (a legal block-model rotation), 22.6 px wide (the cell's diagonal) and 11.3 px deep (to the cell's
// far corner), so its face runs exactly from one corner of the cell to the other and the rest of the box lies inside the
// solid wall behind it. Its 'corner' (south_east, south_west, north_west, north_east: which way its face looks) turns it
// by the blockstate in quarter turns; its 'form' is:
//   full      the plain half cell (y 0-16);
//   arch_neg  the blind arch's springer on the face's -u side: the top half full, the bottom half only on that side;
//   arch_pos  the same on the +u side.
// Laid on a diagonal between the drum's courses (y 4-8) it makes:
//   - the flush face, in the four notch cells outside the steps (|x| + |z| = 31): one plane through the steps' outer tips
//     (|x| + |z| = 32), from the buttress beside one flat to the buttress beside the other;
//   - the recessed panel, by leaving the two middle notches open and cutting the three steps behind them (|x| + |z| = 30)
//     on the plane through their middles (|x| + |z| = 31): a flat back 11.3 px behind the face, the two end notches
//     standing as its jambs (their box ends are the panel's reveals);
//   - the stair-headed blind arch: the two middle notches at the top course (y 8) as arch_neg / arch_pos, their full sides
//     toward the jambs, as the drum's window heads.
// Its faces take the drum's own brick (seb_0: the course rhythm runs on, the 22.6 px face stretches the brick 1.41x
// along it), its tops the dusty top (seb_top).
//
// The scene (block units; the drum's local frame, the catalogue's quarter, north up): A, the quarter as the kit builds it
// now (pass 3: the stepped diagonal with its cut-back panel); B beside it to the east, its north-east diagonal built from
// the new block. The NW diagonal is cut by the quarter's edge and left as built in both.
//   node design/models/clockwork-wastes-drum-diagonal.gen.js
// (the drum quarter is dumped from the game into build/kit-preview/drum_quarter.json by a one-off game test, since removed)
const path = require('path');
const K = require('./kit-building-preview.js');

const REPO = path.resolve(__dirname, '../..');
const OUT = path.join(__dirname, 'clockwork-wastes-drum-diagonal.bbmodel');
const NS = 'comedymonkeysaddons';
const BRICK = `${NS}:block/wastes/seb_0`, TOP = `${NS}:block/wastes/seb_top`;
const W = 16 * Math.SQRT2, D = 8 * Math.SQRT2; // the cell's diagonal, the distance from it to the far corner
const r3 = (n) => Math.round(n * 1000) / 1000;

// ---------------------------------------------------------------- the block's model (drawn facing south-east)
// Boxes before the 45-degree turn about (8, 8, 8): u along x (centred on 8), the face at z 8 (its south face), the body
// behind it to z 8 - D. Turned +45 (x' = (x + z) / sqrt 2 from the origin), the face lies on x + z = 16 through the cell's
// north-east and south-west corners, looking south-east.
function box(name, u0, u1, y0, y1) {
  const from = [r3(8 + u0), y0, r3(8 - D)], to = [r3(8 + u1), y1, 8];
  // UVs: the face's brick across its own share of the face (0-16 over the full 22.6 px), the reveals the depth's share,
  // the tops and bottoms the footprint squeezed into the texture's height (W : D is 2 : 1)
  const fu0 = r3(((u0 + W / 2) / W) * 16), fu1 = r3(((u1 + W / 2) / W) * 16);
  return {
    name, from, to, rotation: { angle: 45, axis: 'y', origin: [8, 8, 8] },
    faces: {
      south: { uv: [fu0, 16 - y1, fu1, 16 - y0], texture: BRICK },
      east: { uv: [0, 16 - y1, r3((D / 16) * 16), 16 - y0], texture: BRICK },
      west: { uv: [r3(16 - D), 16 - y1, 16, 16 - y0], texture: BRICK },
      up: { uv: [fu0, 0, fu1, 8], texture: TOP },
      down: { uv: [fu0, 8, fu1, 16], texture: BRICK },
    },
  };
}
const FORMS = {
  full: [box('half cell', -W / 2, W / 2, 0, 16)],
  arch_neg: [box('head', -W / 2, W / 2, 8, 16), box('springer', -W / 2, 0, 0, 8)],
  arch_pos: [box('head', -W / 2, W / 2, 8, 16), box('springer', 0, W / 2, 0, 8)],
};
// the turn of the whole block for each corner (the blockstate's y, clockwise from above), and the element's own angle it
// becomes in the preview (Blockbench turns right-handed: +45 looks south-east)
const CORNERS = { south_east: { y: 0, angle: 45 }, south_west: { y: 90, angle: -45 }, north_west: { y: 180, angle: -135 }, north_east: { y: 270, angle: 135 } };

// the model checks the Wastes exporter makes: bounds -16..32, UVs on the texture and not zero-width, one rotation of
// 0/22.5/45 on one axis
for (const [form, els] of Object.entries(FORMS))
  for (const e of els) {
    if ([...e.from, ...e.to].some((v) => v < -16 || v > 32)) throw new Error(`${form} ${e.name}: out of bounds`);
    if (![0, 22.5, 45, -22.5, -45].includes(e.rotation.angle)) throw new Error(`${form} ${e.name}: rotation`);
    for (const [d, f] of Object.entries(e.faces)) {
      if (f.uv.some((v) => v < 0 || v > 16)) throw new Error(`${form} ${e.name} ${d}: UV off the texture`);
      if (f.uv[0] === f.uv[2] || f.uv[1] === f.uv[3]) throw new Error(`${form} ${e.name} ${d}: zero-width UV`);
    }
  }

// ---------------------------------------------------------------- the courses' 45-degree forms (D88, choice 33): preview only
// The drum's courses in cut deepslate, the same half cell so they run flat with the brick face: 'course' (the sill at y 3,
// the lintel at y 9: the plain half cell) and 'water_table' (y 2: its lower half 4 px proud of the face, the upper half
// flush, the stair's slope set on the diagonal). Approved (D89): the Cut Deepslate Brick Diagonal.
const DEEPSLATE = 'create:block/palettes/stone_types/brick/deepslate_cut_brick';
const P = 4; // the water table's projection, px
function courseBox(name, y0, y1, out) {
  const b = box(name, -W / 2, W / 2, y0, y1);
  b.to[2] = r3(8 + out);
  for (const fc of Object.values(b.faces)) fc.texture = DEEPSLATE;
  if (out) { b.faces.east.uv = [0, 16 - y1, r3(((D + out) / 16) * 16 > 16 ? 16 : D + out), 16 - y0]; b.faces.west.uv = [r3(16 - Math.min(16, D + out)), 16 - y1, 16, 16 - y0]; }
  return b;
}
const COURSE_FORMS = {
  course: [courseBox('course', 0, 16, 0)],
  water_table: [courseBox('table', 0, 8, P), courseBox('wall', 8, 16, 0)],
};
for (const [form, els] of Object.entries(COURSE_FORMS))
  for (const e of els) {
    if ([...e.from, ...e.to].some((v) => v < -16 || v > 32)) throw new Error(`${form} ${e.name}: out of bounds`);
    for (const [d, fc] of Object.entries(e.faces)) if (fc.uv.some((v) => v < 0 || v > 16) || fc.uv[0] === fc.uv[2] || fc.uv[1] === fc.uv[3]) throw new Error(`${form} ${e.name} ${d}: UV`);
  }

// ---------------------------------------------------------------- the export (D88, D89: approved, into the mod)
//   node design/models/clockwork-wastes-drum-diagonal.gen.js --export
// (run fifth of the Wastes exporters, after clockwork-wastes.export.js writes the textures it checks: export-wastes-all.js)
// comedymonkeysaddons:stained_end_stone_brick_diagonal (StainedBrickDiagonalBlock): a model a form, in the Stained End
// Stone Bricks' three weighted brick variants (seb_0 x2, seb_1, seb_2; D88, choice 34: the variants' models take the first's
// elements and change only the brick), turned by the blockstate a quarter a corner; the item is the full form. The textures
// are the bricks' own, already in the mod: nothing new painted.
if (process.argv.includes('--export')) {
  const fs = require('fs');
  const ASSETS = path.join(REPO, 'src/main/resources/assets', NS);
  const ID = 'stained_end_stone_brick_diagonal';
  const write = (rel, obj) => {
    const file = path.join(ASSETS, rel);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
    console.log('wrote', rel);
  };
  const model = (n) => `${NS}:block/wastes/${n}`;
  const VARIANTS = [['seb_0', 2], ['seb_1', 1], ['seb_2', 1]];
  for (const [form, els] of Object.entries(FORMS)) {
    const elements = els.map((e) => ({
      name: e.name, from: e.from, to: e.to, rotation: { ...e.rotation, rescale: false },
      faces: Object.fromEntries(Object.entries(e.faces).map(([d, f]) => [d, { uv: f.uv, texture: f.texture === TOP ? '#top' : '#brick' }])),
    }));
    write(`models/block/wastes/${ID}_${form}.json`, { parent: 'minecraft:block/block',
      textures: { brick: `${NS}:block/wastes/seb_0`, top: `${NS}:block/wastes/seb_top`, particle: `${NS}:block/wastes/seb_0` }, elements });
    for (const [tex] of VARIANTS.slice(1))
      write(`models/block/wastes/${ID}_${form}_${tex.slice(4)}.json`, { parent: model(`${ID}_${form}`),
        textures: { brick: `${NS}:block/wastes/${tex}`, particle: `${NS}:block/wastes/${tex}` } });
  }
  const variants = {};
  for (const [corner, { y }] of Object.entries(CORNERS))
    for (const form of Object.keys(FORMS))
      variants[`corner=${corner},form=${form}`] = VARIANTS.map(([tex, w], i) => ({ model: model(i ? `${ID}_${form}_${tex.slice(4)}` : `${ID}_${form}`),
        ...(y ? { y } : {}), ...(w > 1 ? { weight: w } : {}) }));
  write(`blockstates/${ID}.json`, { variants });
  write(`models/item/${ID}.json`, { parent: model(`${ID}_full`) });
  // the courses (D89, choice 33 approved): comedymonkeysaddons:cut_deepslate_brick_diagonal (CutDeepslateDiagonalBlock), a
  // model a course in Create's cut deepslate brick, its 'corner' turned as the brick's; the item is the plain course
  const CID = 'cut_deepslate_brick_diagonal';
  for (const [form, els] of Object.entries(COURSE_FORMS)) {
    const elements = els.map((e) => ({
      name: e.name, from: e.from, to: e.to, rotation: { ...e.rotation, rescale: false },
      faces: Object.fromEntries(Object.entries(e.faces).map(([d, fc]) => [d, { uv: fc.uv, texture: '#stone' }])),
    }));
    write(`models/block/wastes/${CID}_${form}.json`, { parent: 'minecraft:block/block', textures: { stone: DEEPSLATE, particle: DEEPSLATE }, elements });
  }
  const cvariants = {};
  for (const [corner, { y }] of Object.entries(CORNERS))
    for (const form of Object.keys(COURSE_FORMS)) cvariants[`corner=${corner},course=${form}`] = { model: model(`${CID}_${form}`), ...(y ? { y } : {}) };
  write(`blockstates/${CID}.json`, { variants: cvariants });
  write(`models/item/${CID}.json`, { parent: model(`${CID}_course`) });
  for (const t of ['seb_0', 'seb_1', 'seb_2', 'seb_top'])
    if (!fs.existsSync(path.join(ASSETS, `textures/block/wastes/${t}.png`))) throw new Error('missing texture ' + t);
  process.exit(0);
}

// ---------------------------------------------------------------- the scene
const scene = new K.Scene('clockwork-wastes-drum-diagonal');
const quarter = K.readLayout(path.join(REPO, 'build/kit-preview/drum_quarter.json'));
const minX = Math.min(...quarter.map((b) => b.x)), maxX = Math.max(...quarter.map((b) => b.x));
const STEP = maxX - minX + 8;
const ALL = { ...FORMS, ...COURSE_FORMS };
const SEB = ['seb_0', 'seb_0', 'seb_1', 'seb_2'].map((t) => `${NS}:block/wastes/${t}`); // the bricks' weights 2 : 1 : 1

// One form in a cell, turned for its corner (the box as drawn, its element angle per corner); the brick variant by position
function drawBlock(kids, form, corner, cell, off, label) {
  const { angle } = CORNERS[corner];
  const brick = SEB[Math.abs((cell[0] * 73856093) ^ (cell[1] * 19349663) ^ (cell[2] * 83492791)) % 4];
  for (const e of ALL[form]) {
    const S = (v, i) => (cell[i] + off[i]) * 16 + v;
    const faces = {};
    for (const [d, fc] of Object.entries(e.faces)) faces[d] = { ref: fc.texture === BRICK ? brick : fc.texture, uv: fc.uv };
    kids.push(scene.push(`${label} ${form} ${e.name}`, e.from.map(S), e.to.map(S), faces,
      { rotation: { axis: 'y', angle }, origin: [8, 8, 8].map(S) }));
  }
}

// A: as it stood (pass 3, the stepped diagonal with its cut-back panel)
scene.addBuilding('A: pass 3, the stepped diagonal', quarter, [0, 0, 0]);

// B and C: the north-east diagonal (x > 0, z < 0: its face looks north-east) from the brick diagonal between the courses
// (y 4-8; B is what Drum.build now builds); C adds the cut deepslate courses' forms (y 2, 3 and 9) and the brick between
// the lintel and the corbel table (y 10-11)
const sx = 1, sz = -1, corner = 'north_east';
const teeth = [14, 15, 16].map((x) => [sx * x, sz * (30 - x)]);
const notches = [14, 15, 16, 17].map((x) => [sx * x, sz * (31 - x)]);
const jambs = [notches[0], notches[3]], middle = [notches[1], notches[2]];
const key = (x, y, z) => `${x},${y},${z}`;
const a = (CORNERS[corner].angle * Math.PI) / 180, U = [Math.cos(a), -Math.sin(a)];
function diagonal(name, off, courses) {
  const taken = new Set();
  for (let y = 4; y <= 8; y++) for (const [x, z] of [...teeth, ...notches]) taken.add(key(x, y, z));
  if (courses) for (const y of [2, 3, 9, 10, 11]) for (const [x, z] of notches) taken.add(key(x, y, z));
  const node = scene.addBuilding(name, quarter.filter((b) => !taken.has(key(b.x, b.y, b.z))), off);
  const kids = [];
  for (let y = 4; y <= 8; y++) {
    for (const [x, z] of teeth) drawBlock(kids, 'full', corner, [x, y, z], off, `step ${x},${z} (the panel's back)`);
    for (const [x, z] of jambs) drawBlock(kids, 'full', corner, [x, y, z], off, `jamb ${x},${z}`);
  }
  for (const [x, z] of middle) {
    // the springer's full side toward its nearer jamb
    const j = jambs.reduce((p, q) => (Math.hypot(q[0] - x, q[1] - z) < Math.hypot(p[0] - x, p[1] - z) ? q : p));
    drawBlock(kids, (j[0] - x) * U[0] + (j[1] - z) * U[1] < 0 ? 'arch_neg' : 'arch_pos', corner, [x, 8, z], off, `arch ${x},${z}`);
  }
  if (courses)
    for (const [x, z] of notches) {
      drawBlock(kids, 'water_table', corner, [x, 2, z], off, `water table ${x},${z}`);
      drawBlock(kids, 'course', corner, [x, 3, z], off, `sill course ${x},${z}`);
      drawBlock(kids, 'course', corner, [x, 9, z], off, `lintel course ${x},${z}`);
      for (const y of [10, 11]) drawBlock(kids, 'full', corner, [x, y, z], off, `brick ${x},${z}`);
    }
  node.children.push(scene.group('the 45-degree blocks', kids, off.map((v) => v * 16)));
}
const B_OFF = [STEP, 0, 0], C_OFF = [2 * STEP, 0, 0];
diagonal('B: the brick diagonal as approved and built (D88)', B_OFF, false);
diagonal('C: B with the cut deepslate courses (water table, sill, lintel) and the brick above the lintel, 45 degrees (choice 33)', C_OFF, true);

// the forms alone, in front of C, on the ground (facing south-east as drawn)
const alone = [];
Object.keys(ALL).forEach((form, i) => drawBlock(alone, form, 'south_east', [i * 3, 0, 0], [C_OFF[0] - 2, 0, -26], 'alone'));
scene.outliner.push(scene.group('the forms alone (south-east): ' + Object.keys(ALL).join(', '), alone));

// the ground: Scorched End Stone under all three
const ground = [];
for (let x = minX - 6; x < C_OFF[0] + maxX + 8; x += 4)
  for (let z = -32; z < -4; z += 4)
    ground.push(scene.push('ground', [x * 16, -16, z * 16], [(x + 4) * 16, 0, (z + 4) * 16], { up: { ref: `${NS}:block/wastes/scorched`, uv: [0, 0, 16, 16] } }));
scene.outliner.push(scene.group('ground', ground));
scene.write(OUT);
console.log(scene.report(), '| B at x', STEP, '| C at x', 2 * STEP);
module.exports = { FORMS, COURSE_FORMS, CORNERS, W, D };
