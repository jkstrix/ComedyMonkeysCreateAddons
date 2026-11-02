// Clockwork Wastes: the ridge roll at slab height (D83) -> design/models/clockwork-wastes-ridge.bbmodel
// Preview only: nothing here goes into the mod. The buildings are the kit's own, dumped from the game by a game test into
// build/kit-preview/ridge_*.json (a one-off game test, since removed, wrote each kit Model's blocks as { x, y, z, state }:
// a Foundry shed 13 x 10 intact, a Workshops shed with a walnut door, a worn Quarters terrace of four) and turned into models by kit-building-preview.js (the real blockstates, models and
// textures); only the ridge's Patina Rib cells are drawn here, in the Patina Rib's own texture (rib_patina), three ways:
//   A  the current full Patina Rib block (as built);
//   B  a plain bottom slab of it, half the height (a "Patina Rib Slab": vanilla slab shapes, no facing);
//   C  a slab-height rounded roll: each half of the 2-wide ridge a stepped quarter-round (a base 4 px high the full
//      block wide, a 4 px step set back 4 px from the eave side, a 2 px step at the ridge line; 10 px at the ridge), so the
//      two halves meet as one round cap; it needs a facing (toward the ridge line).
// The scene (block units, fronts facing north, -z): a row per option, each with a Foundry shed (13 x 10) and a worn Quarters
// terrace of four (its hatches and curbs) side by side; in front, each option's two blocks alone on the ground.
//   node design/models/clockwork-wastes-ridge.gen.js
const path = require('path');
const K = require('./kit-building-preview.js');

const REPO = path.resolve(__dirname, '../..');
const DUMP = path.join(REPO, 'build/kit-preview');
const OUT = path.join(__dirname, 'clockwork-wastes-ridge.bbmodel');
const RIB = 'comedymonkeysaddons:patina_rib';
const TEX = 'comedymonkeysaddons:block/wastes/rib_patina';
const load = (n) => K.readLayout(path.join(DUMP, n + '.json'));

// ---------------------------------------------------------------- the export (D85: option C into the mod)
//   node design/models/clockwork-wastes-ridge.gen.js --export
// (run last of the Wastes exporters, after clockwork-wastes.export.js writes the textures it checks: export-wastes-all.js)
// The Patina Ridge Roll (comedymonkeysaddons:patina_roll, PatinaRollBlock): option C's three boxes, drawn facing south (its
// high edge at +z, the ridge line), turned by the blockstate for the others; the Patina Rib's own texture (rib_patina, already
// in the mod: nothing new to paint). Checked as the Wastes exporter checks its models: bounds -16..32, UVs on the texture
// with no zero width, no rotations, no z-fights between faces.
if (process.argv.includes('--export')) {
  const fs = require('fs');
  const { check } = require('./zfight-check.js');
  const ASSETS = path.join(REPO, 'src/main/resources/assets/comedymonkeysaddons');
  const ID = 'patina_roll';
  const boxes = [[[0, 0, 0], [16, 4, 16]], [[0, 4, 4], [16, 8, 16]], [[0, 8, 10], [16, 10, 16]]];
  const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
  const elements = boxes.map(([from, to], i) => {
    const f = boxFaces(from, to), faces = {};
    for (const d of DIRS) {
      // a face on the block's boundary culls against a neighbour (the ridge's other half, the next roll along, the roof)
      const onEdge = { north: from[2] === 0, south: to[2] === 16, west: from[0] === 0, east: to[0] === 16, down: from[1] === 0, up: to[1] === 16 }[d];
      // the faces a box above or behind covers aren't drawn
      if (d === 'up' && i < boxes.length - 1) {
        const n = boxes[i + 1];
        if (n[0][2] <= from[2] && n[1][2] >= to[2]) continue;
      }
      // the step's and the cap's undersides sit on the box below: never seen
      if (d === 'down' && i > 0) continue;
      faces[d] = { uv: f[d].uv, texture: '#all', ...(onEdge ? { cullface: d } : {}) };
    }
    return { name: ['base', 'step', 'cap'][i], from, to, faces };
  });
  // the checks
  for (const e of elements) {
    if ([...e.from, ...e.to].some((v) => v < -16 || v > 32)) throw new Error(e.name + ': out of bounds');
    for (const [d, f] of Object.entries(e.faces)) {
      if (f.uv.some((v) => v < 0 || v > 16)) throw new Error(`${e.name} ${d}: UV off the texture`);
      if (f.uv[0] === f.uv[2] || f.uv[1] === f.uv[3]) throw new Error(`${e.name} ${d}: zero-width UV`);
    }
  }
  const problems = check({ t0: elements.map((e) => ({ name: e.name, from: e.from, to: e.to, faces: Object.fromEntries(Object.keys(e.faces).map((d) => [d, 'rib'])) })) });
  // one texture, but its UVs follow each box, so two faces sharing a plane would still fight: none may
  if (problems.length) throw new Error('z-fights:\n' + problems.join('\n'));
  const write = (rel, obj) => {
    const file = path.join(ASSETS, rel);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
    console.log('wrote', rel);
  };
  const model = `comedymonkeysaddons:block/wastes/${ID}`;
  write(`models/block/wastes/${ID}.json`, { parent: 'minecraft:block/block', textures: { all: 'comedymonkeysaddons:block/wastes/rib_patina', particle: 'comedymonkeysaddons:block/wastes/rib_patina' }, elements });
  write(`blockstates/${ID}.json`, { variants: { 'facing=south': { model }, 'facing=west': { model, y: 90 }, 'facing=north': { model, y: 180 }, 'facing=east': { model, y: 270 } } });
  write(`models/item/${ID}.json`, { parent: model });
  if (!fs.existsSync(path.join(ASSETS, 'textures/block/wastes/rib_patina.png'))) throw new Error('the Patina Rib texture is missing');
  process.exit(0);
}

const shed = load('ridge_shed'), terrace = load('ridge_terrace_worn');
const scene = new K.Scene('clockwork-wastes-ridge');

// A box's faces in the rib texture, UVs from its own extent (world-aligned, as a cube_all block's would be)
function boxFaces(from, to) {
  const [x0, y0, z0] = from, [x1, y1, z1] = to, f = {};
  f.north = { ref: TEX, uv: [16 - x1, 16 - y1, 16 - x0, 16 - y0] };
  f.south = { ref: TEX, uv: [x0, 16 - y1, x1, 16 - y0] };
  f.west = { ref: TEX, uv: [z0, 16 - y1, z1, 16 - y0] };
  f.east = { ref: TEX, uv: [16 - z1, 16 - y1, 16 - z0, 16 - y0] };
  f.up = { ref: TEX, uv: [x0, z0, x1, z1] };
  f.down = { ref: TEX, uv: [x0, 16 - z1, x1, 16 - z0] };
  return f;
}
// One option's boxes for a ridge cell, in block px; `toRidge` is +1 when the ridge line is on the cell's +z side, -1 on its -z side
function shapes(opt, toRidge) {
  if (opt === 'A') return [[[0, 0, 0], [16, 16, 16]]];
  if (opt === 'B') return [[[0, 0, 0], [16, 8, 16]]];
  // C: a quarter-round in three steps, highest at the ridge line
  const z = (a, b) => (toRidge > 0 ? [a, b] : [16 - b, 16 - a]);
  const [a0, a1] = z(0, 16), [b0, b1] = z(4, 16), [c0, c1] = z(10, 16);
  return [[[0, 0, a0], [16, 4, a1]], [[0, 4, b0], [16, 8, b1]], [[0, 8, c0], [16, 10, c1]]];
}
function drawRibs(blocks, off, opt, kids, label) {
  const ribs = blocks.filter((b) => b.id === RIB);
  const at = new Set(ribs.map((b) => `${b.x},${b.y},${b.z}`));
  for (const b of ribs) {
    const toRidge = at.has(`${b.x},${b.y},${b.z + 1}`) ? 1 : -1;
    for (const [from, to] of shapes(opt, toRidge)) {
      const S = (v, i) => (b[['x', 'y', 'z'][i]] + off[i]) * 16 + v;
      kids.push(scene.push(`${label} ridge ${b.x},${b.z}`, from.map((v, i) => S(v, i)), to.map((v, i) => S(v, i)), boxFaces(from, to)));
    }
  }
}
const OPTIONS = [
  ['A', 'A: full Patina Rib (as built)'],
  ['B', 'B: Patina Rib slab (plain, half height)'],
  ['C', 'C: slab-height rounded roll (two halves, faced to the ridge)'],
];
const shedW = Math.max(...shed.map((b) => b.x)) - Math.min(...shed.map((b) => b.x)) + 1;
OPTIONS.forEach(([opt, name], row) => {
  const z = row * 26;
  const node = scene.group(name, []);
  scene.outliner.push(node);
  const shedOff = [-Math.min(...shed.map((b) => b.x)), 0, z];
  const terOff = [shedW + 6 - Math.min(...terrace.map((b) => b.x)), 0, z];
  for (const [blocks, off, n] of [[shed, shedOff, 'Foundry shed'], [terrace, terOff, 'Quarters terrace, worn']]) {
    const b = scene.addBuilding(`${opt} ${n}`, blocks.filter((x) => x.id !== RIB), off, { parent: node });
    const kids = [];
    drawRibs(blocks, off, opt, kids, opt);
    b.children.push(scene.group(`${opt} ridge roll`, kids));
  }
  // the option's two ridge blocks alone on the ground, in front of the shed
  const kids = [];
  drawRibs([{ id: RIB, x: 0, y: 0, z: 0 }, { id: RIB, x: 0, y: 0, z: 1 }], [-4, 0, z - 6], opt, kids, `${opt} alone`);
  node.children.push(scene.group(`${opt} two blocks alone`, kids));
});
// a fourth row (the same pass, D83): the Workshops shed with its new dressings (polished cut dripstone sills, corbels and
// lintel band on the tuff, tuff pillar piers), its ridge as built (A)
{
  const ws = load('ridge_shed_workshops'), z = OPTIONS.length * 26;
  const node = scene.group('D83: Workshops shed, dripstone dressings (ridge as built)', []);
  scene.outliner.push(node);
  scene.addBuilding('Workshops shed', ws, [-Math.min(...ws.map((b) => b.x)), 0, z], { parent: node });
}
// the ground: Scorched End Stone under each row
const ground = [];
for (let row = 0; row <= OPTIONS.length; row++)
  for (let x = -8; x < shedW + 34; x += 4)
    for (let z = row * 26 - 9; z < row * 26 + 14; z += 4)
      ground.push(scene.push('ground', [x * 16, -16, z * 16], [(x + 4) * 16, 0, (z + 4) * 16], {
        up: { ref: 'comedymonkeysaddons:block/wastes/scorched', uv: [0, 0, 16, 16] } }));
scene.outliner.push(scene.group('ground', ground));
scene.write(OUT);
console.log(scene.report());
