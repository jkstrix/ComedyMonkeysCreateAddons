// Clockwork Wastes: the wrecked winding tower (D79, D80, kit/WindingTower.java) -> design/models/clockwork-wastes-winding-tower.bbmodel
// Preview only: nothing here goes into the mod. The blocks are the kit's own, dumped from the game by a game test into
// build/kit-preview/*.json and turned into models by kit-building-preview.js (the real blockstates, models and textures);
// the Sprung Mainspring and the Fallen Rafters are the tower's own prop blocks (clockwork-wastes-props.export.js).
//   node design/models/clockwork-wastes-winding-tower.gen.js
// (D84: the approved pass 4; the props drawn from the exported block models, as the game draws them.)
// The scene (block units; the towers' fronts face north, -z): the DAMAGED tower (what the city builds) at x 0; the INTACT
// tower beside it to the east; a 4-house INTACT Quarters terrace beside that, for scale; in front (north), the prop's pieces
// laid out alone, each as modelled (no blockstate turn), a group each; the ground is the Wastes' Scorched End Stone.
// (Pass 1, the three block-material options for the old block coil, is in design/texture-reviews/clockwork-wastes-winding-tower/.)
const path = require('path');
const fs = require('fs');
const { png, hash } = require('./process-box.lib.js');
const K = require('./kit-building-preview.js');
const MS = require('./clockwork-wastes-mainspring.gen.js');

const REPO = path.resolve(__dirname, '../..');
const DUMP = path.join(REPO, 'build/kit-preview');
const OUT = path.join(__dirname, 'clockwork-wastes-winding-tower.bbmodel');
const load = (n) => K.readLayout(path.join(DUMP, n + '.json'));

const damaged = load('winding_tower_damaged'), intact = load('winding_tower_intact'), ruined = load('winding_tower_ruined'), terrace = load('terrace_intact');
const scene = new K.Scene('clockwork-wastes-winding-tower');
const TEX = { '#log': 'minecraft:block/dark_oak_log', '#log_top': 'minecraft:block/dark_oak_log_top' };
for (const [k, png] of Object.entries(MS.TEX)) TEX['#' + k] = scene.addTexture('spring_steel_' + k, png);

// One prop piece, turned as its blockstate would turn it, at a block (anchor) plus a block offset
function drawPiece(kids, pc, off, label) {
  for (const el of MS.PIECES[pc.type].elements) {
    const e = K.turnElement(el, pc.rx, pc.ry, false), faces = {};
    for (const [d, f] of Object.entries(e.faces)) faces[d] = { ref: TEX[f.texture], uv: f.uv, rotation: f.rotation };
    const S = (v, i) => (pc.at[i] + off[i]) * 16 + v;
    kids.push(scene.push(`${label} ${el.name}`, e.from.map(S), e.to.map(S), faces, { rotation: e.rotation, origin: e.rotation ? e.rotation.origin.map(S) : null }));
  }
}
// (the props are real blocks now, D84: the dump holds each piece at its own block, and the converter draws its exported
// model from the mod's assets like any other block)
function addTower(name, blocks, off, want = MS.lay().length + MS.RAFTERS.length) {
  scene.addBuilding(name, blocks, off);
  const got = blocks.filter((b) => /^comedymonkeysaddons:(sprung_mainspring|fallen_rafter)_/.test(b.id)).length;
  if (got !== want) console.warn(`${name}: ${got} prop pieces in the dump, ${want} laid (a piece's own block is taken, or the dump is old)`);
}

// ---------- the buildings, side by side along x (8 blocks between them)
const span = (bs) => [Math.min(...bs.map((b) => b.x)), Math.max(...bs.map((b) => b.x))];
const GAP = 8;
const offD = [0, 0, 0];
const offI = [span(damaged)[1] + GAP - span(intact)[0] + 1, 0, 0];
const offT = [offI[0] + span(intact)[1] + GAP - span(terrace)[0] + 1, 0, 0];
// the RUINED tower (D87) to the west of the damaged one, its spring side (west) free
const offR = [span(damaged)[0] - GAP - span(ruined)[1] - 1, 0, 0];
addTower('DAMAGED winding tower (what the city builds)', damaged, offD);
addTower('INTACT winding tower', intact, offI);
addTower('RUINED winding tower (the spiral, and the peel fallen across the stones)', ruined, offR,
  MS.lay().filter((p) => p.type.startsWith('spiral_')).length + MS.layFallen().length);
scene.addBuilding('Terrace, 4 houses, INTACT (for scale)', terrace, offT);

// ---------- the prop's pieces laid out alone, in a row in front of the towers (each in its own group, as modelled)
const PIECE_Z = -22, pieceNodes = [];
let px = -10;
for (const p of Object.values(MS.PIECES).filter((q) => !q.section)) {
  const xs = p.elements.flatMap((e) => [e.from[0], e.to[0]]), w = Math.ceil((Math.max(...xs) - Math.min(...xs)) / 16) + 1;
  const at = [px - Math.floor(Math.min(...xs) / 16), 0, PIECE_Z], kids = [];
  drawPiece(kids, { type: p.name, at, rx: 0, ry: 0 }, [0, 0, 0], p.name);
  pieceNodes.push(scene.group(`${p.name}: ${p.desc}`, kids, at.map((v) => v * 16)));
  px += w + 2;
}
// the peel's and the spiral's sections together, as the tower lays them, alone at the row's end (x from the peel's top)
{
  const kids = [], sections = MS.lay().filter((pc) => MS.PIECES[pc.type].section), dx = px + 12, dz = PIECE_Z - 10;
  for (const pc of sections) drawPiece(kids, { ...pc, at: [pc.at[0] + dx, pc.at[1], pc.at[2] + dz] }, [0, 0, 0], pc.type);
  pieceNodes.push(scene.group('the peel and the spiral, their sections together', kids, [dx * 16, 0, dz * 16]));
  px += 16;
}
scene.outliner.push(scene.group('Sprung Mainspring pieces (laid out alone)', pieceNodes));
const piecesX = [-10, px];

// ---------- the ground: Scorched End Stone, 8 x 8-block tiles of its real texture (each block turned as the game turns it)
{
  const tex = K.decodePng(fs.readFileSync(path.join(REPO, 'src/main/resources/assets/comedymonkeysaddons/textures/block/wastes/scorched.png')));
  const N = 8, W = 16 * N, d = new Uint8Array(W * W * 4);
  for (let by = 0; by < N; by++) for (let bx = 0; bx < N; bx++) {
    const rot = Math.floor(hash(bx, by, 71) * 4);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const [sx, sy] = [[x, y], [y, 15 - x], [15 - x, 15 - y], [15 - y, x]][rot], o = (sy * 16 + sx) * 4;
      d.set(tex.rgba.subarray(o, o + 4), ((by * 16 + y) * W + bx * 16 + x) * 4);
    }
  }
  const ground = scene.addTexture('ground_scorched_end_stone', png(W, W, d));
  scene.own[ground].w = W; scene.own[ground].h = W;
  const all = [...ruined.map((b) => [b.x + offR[0], b.z]), ...damaged.map((b) => [b.x + offD[0], b.z]), ...intact.map((b) => [b.x + offI[0], b.z]), ...terrace.map((b) => [b.x + offT[0], b.z]),
    [piecesX[0], PIECE_Z - 3], [piecesX[1], PIECE_Z + 3]];
  const x0 = Math.floor((Math.min(...all.map((p) => p[0])) - 6) / N) * N, x1 = Math.ceil((Math.max(...all.map((p) => p[0])) + 7) / N) * N;
  const z0 = Math.floor((Math.min(...all.map((p) => p[1])) - 8) / N) * N, z1 = Math.ceil((Math.max(...all.map((p) => p[1])) + 7) / N) * N;
  const kids = [];
  for (let x = x0; x < x1; x += N) for (let z = z0; z < z1; z += N)
    kids.push(scene.push('ground', [x * 16, -1, z * 16], [(x + N) * 16, 0, (z + N) * 16], { up: { ref: ground, uv: [0, 0, 16, 16] } }));
  scene.outliner.push(scene.group('Ground (Scorched End Stone)', kids));
}

scene.write(OUT);
console.log(scene.report());
console.log('offsets: ruined', offR, 'damaged', offD, 'intact', offI, 'terrace', offT, 'pieces row z', PIECE_Z, 'x', piecesX);
