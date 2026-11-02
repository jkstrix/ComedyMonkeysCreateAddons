// Writes the winding tower's props into the mod (D80-D84, D87; the look approved as pass 4 in
// design/texture-reviews/clockwork-wastes-winding-tower/pass-4/, built by clockwork-wastes-mainspring.gen.js):
//   - the Sprung Mainspring: one block a piece the tower lays (the turn on the arbor in two halves, the straight, the burst,
//     the peel's and the spiral's 3 x 3-block sections), each a block model reaching into the blocks round its own
//   - the Fallen Rafter: the long (45 degree) and the short (22.5 degree) dark oak beam
//   - their blockstates (facing north as the tower lays them; east, south and west turned as the city turns the tower),
//     item models (the block model, fitted to the slot), the strip's textures (textures/block/wastes/props/: the main
//     exporter prunes the pngs in wastes/ itself), the loot tables (decor drops itself, D37)
//   - src/main/java/.../content/wastes/WindingTowerPieces.java: the pieces' ids, outlines, and where the tower lays them
// Every model is checked as clockwork-wastes.export.js checks its own (UVs on the texture, no zero-width UV, bounds
// -16..32, one rotation of 0/±22.5/±45 per element) and z-fight checked (faces of different textures closer than 0.2 px).
// The mining tags and the lang names are hand-edited (shared files); this prints the lines they need.
// Usage: node design/models/clockwork-wastes-props.export.js
// Run second, after clockwork-wastes.export.js (export-wastes-all.js runs every Wastes exporter in order).
const fs = require('fs');
const path = require('path');
const { ASSETS, REPO, fitGui } = require('./process-box.lib.js');
const { check } = require('./zfight-check.js');
const MS = require('./clockwork-wastes-mainspring.gen.js');

const NS = 'comedymonkeysaddons';
const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
const r3 = (n) => Math.round(n * 1000) / 1000;
const TEX_REF = {
  '#log': 'minecraft:block/dark_oak_log', '#log_top': 'minecraft:block/dark_oak_log_top',
  ...Object.fromEntries(Object.keys(MS.TEX).map((k) => ['#' + k, `${NS}:block/wastes/props/sprung_mainspring_${k}`])),
};
const problems = [];
const writeJson = (rel, obj) => { const f = path.join(ASSETS, rel); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n'); };
const writeData = (rel, obj) => { const f = path.join(REPO, 'src/main/resources/data', NS, rel); fs.mkdirSync(path.dirname(f), { recursive: true }); fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n'); };

// ---------- a piece's block model ----------
function blockModel(id, elements, particle) {
  const used = new Map();
  const v = (t) => { if (!used.has(t)) used.set(t, t.slice(1)); return '#' + used.get(t); };
  const out = [];
  for (const e of elements) {
    const faces = {};
    for (const d of DIRS) {
      const f = e.faces[d];
      if (!f) continue;
      if (f.uv.some((n) => n < -0.001 || n > 16.001)) problems.push(`${id}:${e.name} ${d}: UV ${f.uv} is off the texture`);
      if (Math.abs(f.uv[0] - f.uv[2]) < 1e-6 || Math.abs(f.uv[1] - f.uv[3]) < 1e-6) problems.push(`${id}:${e.name} ${d}: zero-width UV ${f.uv}`);
      faces[d] = { uv: f.uv.map(r3), texture: v(f.texture), ...(f.rotation ? { rotation: f.rotation } : {}) };
    }
    const el = { name: e.name, from: e.from.map(r3), to: e.to.map(r3) };
    for (const p of [...el.from, ...el.to]) if (p < -16 || p > 32) problems.push(`${id}:${e.name} is outside the model bounds`);
    if (e.rotation) {
      if (![-45, -22.5, 22.5, 45].includes(e.rotation.angle)) problems.push(`${id}:${e.name}: angle ${e.rotation.angle}`);
      el.rotation = { origin: e.rotation.origin.map(r3), axis: e.rotation.axis, angle: e.rotation.angle };
    }
    el.faces = faces;
    out.push(el);
  }
  const textures = { particle: TEX_REF[particle] };
  for (const [t, name] of used) textures[name] = TEX_REF[t];
  // the item: the whole piece fitted to the slot; held, framed and dropped at the same scale against block/block's views
  const gui = fitGui(out), s = gui.scale[0] / 0.625;
  const display = {
    gui,
    ground: { rotation: [0, 0, 0], translation: [0, 3, 0], scale: [0.25, 0.25, 0.25].map((n) => r3(n * s)) },
    fixed: { rotation: [0, 0, 0], translation: [0, 0, 0], scale: [0.5, 0.5, 0.5].map((n) => r3(n * s)) },
    thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375].map((n) => r3(n * s)) },
    firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4].map((n) => r3(n * s)) },
    firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4].map((n) => r3(n * s)) },
  };
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, display, elements: out };
}
// only fights between different textures show (the same texture meeting itself draws the same pixels)
function zcheck(id, elements) {
  const byTex = {};
  for (const e of elements) for (const d of DIRS) {
    const f = e.faces[d];
    if (!f) continue;
    (byTex[f.texture] = byTex[f.texture] || []).push({ name: e.name, from: e.from, to: e.to, rot: e.rotation ? [e.rotation.axis, e.rotation.angle, e.rotation.origin] : null, faces: { [d]: f.texture } });
  }
  const keys = Object.keys(byTex), parts = Object.fromEntries(keys.map((k, i) => ['t' + i, byTex[k]]));
  for (const p of check(parts)) {
    const m = p.match(/^t(\d+):(\S+) (\w+) vs t(\d+):(.*)$/);
    if (m && m[1] !== m[4]) problems.push(`z-fight ${id}: ${keys[m[1]]}:${m[2]} ${m[3]} vs ${keys[m[4]]}:${m[5]}`);
  }
}
// its outline in its own block (the geometry's turned box clipped to the block), in pixels; the whole block if none of
// the piece lies in its own block (a section whose strip passes round its middle)
function outline(elements) {
  const rot = (p, r) => {
    if (!r) return p;
    const a = (r.angle * Math.PI) / 180, o = r.origin, q = p.map((n, i) => n - o[i]);
    const [i, j] = r.axis === 'x' ? [1, 2] : r.axis === 'y' ? [2, 0] : [0, 1];
    const u = q[i] * Math.cos(a) - q[j] * Math.sin(a), w = q[i] * Math.sin(a) + q[j] * Math.cos(a);
    q[i] = u; q[j] = w;
    return q.map((n, k) => n + o[k]);
  };
  let lo = [Infinity, Infinity, Infinity], hi = [-Infinity, -Infinity, -Infinity];
  for (const e of elements) {
    const n = 4;
    for (let a = 0; a <= n; a++) for (let b = 0; b <= n; b++) for (let c = 0; c <= n; c++) {
      const p = rot([0, 1, 2].map((i, k) => e.from[i] + ((e.to[i] - e.from[i]) * [a, b, c][k]) / n), e.rotation);
      if (p.every((v) => v >= 0 && v <= 16)) { lo = lo.map((v, i) => Math.min(v, p[i])); hi = hi.map((v, i) => Math.max(v, p[i])); }
    }
  }
  if (!isFinite(lo[0])) return [0, 0, 0, 16, 16, 16];
  // at least 2 px each way, so it can be picked
  for (let i = 0; i < 3; i++) if (hi[i] - lo[i] < 2) { const m = (hi[i] + lo[i]) / 2; lo[i] = Math.max(0, m - 1); hi[i] = Math.min(16, lo[i] + 2); }
  return [...lo, ...hi].map((v) => Math.round(v * 4) / 4);
}

// ---------- the pieces the tower lays ----------
const laid = MS.lay(), fallen = MS.layFallen(), rafters = MS.RAFTERS;
const spring = [...new Set([...laid, ...fallen].map((p) => p.type))];
for (const p of [...laid, ...fallen]) if (p.rx || p.ry) throw new Error(`${p.type} is laid turned (${p.rx}, ${p.ry}): bake it into its model first`);
const idOf = (type) => (type.startsWith('rafter_') ? `fallen_${type}` : `sprung_mainspring_${type}`);
// a piece's reach: the cells its model runs through, from its own block, facing north (the same wherever it's laid)
function reachOf(type) {
  const pc = [...laid, ...fallen, ...rafters].find((l) => l.type === type);
  return MS.cellsOf([pc], () => false).map((c) => c.map((v, i) => v - pc.at[i]));
}
const pieces = [];
for (const type of [...spring, ...rafters.map((r) => r.type)]) {
  const id = idOf(type), els = MS.PIECES[type].elements, particle = type.startsWith('rafter_') ? '#log' : '#plain';
  writeJson(`models/block/wastes/${id}.json`, blockModel(id, els, particle));
  zcheck(id, els);
  writeJson(`blockstates/${id}.json`, { variants: Object.fromEntries([['north', 0], ['east', 90], ['south', 180], ['west', 270]]
    .map(([f, y]) => [`facing=${f}`, { model: `${NS}:block/wastes/${id}`, ...(y ? { y } : {}) }])) });
  writeJson(`models/item/${id}.json`, { parent: `${NS}:block/wastes/${id}` });
  writeData(`loot_table/blocks/${id}.json`, { type: 'minecraft:block', pools: [{ rolls: 1, entries: [{ type: 'minecraft:item', name: `${NS}:${id}` }],
    conditions: [{ condition: 'minecraft:survives_explosion' }] }] });
  pieces.push({ type, id, box: outline(els), reach: reachOf(type) });
}
for (const [k, png] of Object.entries(MS.TEX)) {
  const f = path.join(ASSETS, 'textures/block/wastes/props', `sprung_mainspring_${k}.png`);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, png);
}

// ---------- WindingTowerPieces.java: the pieces, where each is laid, and the cells each laying passes through ----------
const lays = [...laid, ...fallen, ...rafters].map((pc) => ({ piece: pieces.findIndex((p) => p.type === pc.type), at: pc.at, pc }));
const cellsWithLay = (from, to, skip) => {
  const seen = new Set(), out = [];
  for (let i = from; i < to; i++)
    for (const c of MS.cellsOf([lays[i].pc], skip)) { const k = c.join(','); if (!seen.has(k)) { seen.add(k); out.push([...c, i]); } }
  return out;
};
const springCells = cellsWithLay(0, laid.length + fallen.length, (c) => c[0] === 5 && c[1] === 10 && (c[2] === 3 || c[2] === 4));
const rafterCells = cellsWithLay(laid.length + fallen.length, lays.length, (c) => c[0] <= 1);
const rows = (nums, per) => { const r = []; for (let i = 0; i < nums.length; i += per) r.push('\t\t' + nums.slice(i, i + per).join(', ') + ','); return r.join('\n'); };
const java = `package com.comedymonkey.createaddons.content.wastes;

/**
 * The winding tower's props (D80-D84), as design/models/clockwork-wastes-props.export.js writes them: generated, don't edit
 * (change design/models/clockwork-wastes-mainspring.gen.js and run the exporter). Each piece is a block whose model reaches
 * into the blocks round its own (its reach: the city counts a prop standing in every cell its reach covers, D87); the tower
 * lays them facing north in its own frame.
 */
public final class WindingTowerPieces {
	private WindingTowerPieces() {}

	/**
	 * A piece: its block id, its outline in its own block facing north (pixels: x0, y0, z0, x1, y1, z1), and its reach (the
	 * cells its model runs through, from its own block, facing north: dx, dy, dz triples).
	 */
	public record Piece(String id, double[] box, int[] reach) {}

	/** A laying: a piece (its index in {@link #PIECES}) at its own block (local x, y, z). */
	public record Lay(int piece, int x, int y, int z) {}

	/** The Sprung Mainspring's pieces, then the Fallen Rafters. */
	public static final Piece[] PIECES = {
${pieces.map((p) => `\t\tnew Piece("${p.id}", new double[] { ${p.box.join(', ')} },\n\t\t\tnew int[] { ${p.reach.flat().join(', ')} }),`).join('\n')}
	};

	/** The Sprung Mainspring's pieces in {@link #PIECES} (the rest are the rafters). */
	public static final int SPRING_PIECES = ${spring.length};

	/** Where the tower lays them: the spring's layings, the ruined ring's fallen peel, then the rafters'. */
	public static final Lay[] LAYS = {
${lays.map((l) => `\t\tnew Lay(${l.piece}, ${l.at.join(', ')}),`).join('\n')}
	};

	/** The standing spring's layings in {@link #LAYS}, then the fallen peel's (laid in the ruined ring instead of the peel). */
	public static final int SPRING_LAYS = ${laid.length}, FALLEN_LAYS = ${fallen.length};

	/** The cells the spring's strip passes through (local x, y, z, and the laying's index in {@link #LAYS}), less the arbor's. */
	public static final int[] SPRING_CELLS = {
${rows(springCells.flat(), 16)}
	};

	/** The cells the rafters pass through (local x, y, z, laying index), less the wall's. */
	public static final int[] RAFTER_CELLS = {
${rows(rafterCells.flat(), 16)}
	};
}
`;
fs.writeFileSync(path.join(REPO, 'src/main/java/com/comedymonkey/createaddons/content/wastes/WindingTowerPieces.java'), java);

// ---------- what the shared files need ----------
const nice = (type) => {
  if (type.startsWith('fallen_')) return 'Sprung Mainspring (Torn Scrap)';
  if (type.startsWith('rafter_')) return `Fallen Rafter (${type.endsWith('long') ? 'Long' : 'Short'})`;
  const m = type.match(/^(peel|spiral|fallen)_(\d+)_(\d+)$/);
  if (m) return `Sprung Mainspring (${{ peel: 'Peel', spiral: 'Spiral', fallen: 'Fallen Peel' }[m[1]]} ${Number(m[2]) + 1}-${Number(m[3]) + 1})`;
  return 'Sprung Mainspring (' + { arbor_turn_upper: 'Arbor Turn, Upper', arbor_turn_lower: 'Arbor Turn, Lower', straight: 'Straight', burst: 'Burst' }[type] + ')';
};
console.log('lang:\n' + pieces.map((p) => `  "block.${NS}.${p.id}": "${nice(p.type)}",`).join('\n'));
console.log('pickaxe:\n' + pieces.filter((p) => !p.type.startsWith('rafter_')).map((p) => `    "${NS}:${p.id}",`).join('\n'));
console.log('axe:\n' + pieces.filter((p) => p.type.startsWith('rafter_')).map((p) => `    "${NS}:${p.id}",`).join('\n'));
const issues = [...MS.checks(), ...problems];
console.log(issues.length ? issues.join('\n') : `${pieces.length} pieces written; checks ok (UVs on the texture, no zero-width UV, bounds -16..32, rotations, no z-fights)`);
process.exitCode = issues.length ? 1 : 0;
