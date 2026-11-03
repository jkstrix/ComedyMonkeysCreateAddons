// Writes the approved Clockwork Jetpack and Jetpack Winding Station (design/models/clockwork-jetpack.gen.js, brief
// design/briefs/clockwork-jetpack.md) into the mod:
//  - each jetpack's parts, one model per moving bone, in the jetpack's own block space (plate against the wearer at z 4,
//    facing north); the worn renderer and the station's renderer pose them (pivots: JetpackParts.java, from the generator);
//  - the station's block model (its fixed parts, facing north) and its moving parts (drive, boom, reach, socket);
//  - the item models (the jetpacks drawn in 3D from their parts; the station drawn small), the station's blockstate, the
//    Curios slot icon, and only the textures all of these use.
// Usage: node design/models/clockwork-jetpack.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS } = require('./process-box.lib.js');
const { blockModel } = require('./block-model.lib.js');
// the generator rewrites its Blockbench preview as it loads; send that to a scratch file, not over the approved one
process.argv[2] = path.join(require('os').tmpdir(), 'clockwork-jetpack.export-scratch.bbmodel');
const G = require('./clockwork-jetpack.gen.js');

const NS = 'comedymonkeysaddons';
const VARIANTS = [['clockwork_jetpack', 'cj:', G.TP], ['netherite_clockwork_jetpack', 'cn:', G.TN]];
const used = { 'cj:': new Set(), 'cn:': new Set() };
const prefixOf = { 'cj:': 'clockwork_jetpack', 'cn:': 'netherite_clockwork_jetpack' };
const ref = (key) => {
  const p = Object.keys(used).find((k) => key.startsWith(k));
  if (!p) throw new Error('texture key outside the jetpack: ' + key);
  used[p].add(key.slice(p.length));
  return `${NS}:block/${prefixOf[p]}_${key.slice(p.length)}`;
};
const dir = (kind) => path.join(ASSETS, 'models', kind);
const write = (kind, name, cubes, particle, extra = {}) => {
  const model = { ...blockModel(ref, cubes, particle), ...extra };
  fs.writeFileSync(path.join(dir(kind), name + '.json'), JSON.stringify(model, null, 2) + '\n');
  return cubes.length;
};

let parts = 0;
for (const [v, K] of VARIANTS) {
  const p = K + 'steel';
  const part = (name, cubes) => { write('block', `${v}_${name}`, cubes, p); parts++; };
  part('frame', G.frame(K));
  part('drum', [...G.drum(K), ...G.glass(K)]);
  part('ducts', G.ducts(K));
  part('flange', G.flange(K));
  part('key', G.keyParts(K));
  part('pawl', G.pawlParts(K));
  [0, 1, 2, 3].forEach((k) => part('coil_' + k, G.coilPlane(K, k)));
  for (const s of ['L', 'R']) {
    const l = s.toLowerCase();
    part('gear_' + l, G.gear(K, s));
    part('rotor_' + l, G.rotor(K, s));
    part('hub_' + l, G.hubCap(K, s));
    for (let k = 1; k <= 5; k++) {
      const B = G.blade(K, s, k);
      part(`blade_${l}${k}`, B.inner);
      part(`elbow_${l}${k}`, B.outer);
    }
  }
  part('harness', G.harness(K));
  part('harness_shirt', G.harness(K, 'shirt'));
  // the item: the jetpack itself, drawn from these parts by JetpackItemRenderer, at a block's angles turned round so the
  // drum (its south face) faces you
  fs.writeFileSync(path.join(dir('item'), v + '.json'), JSON.stringify({
    parent: 'minecraft:builtin/entity', gui_light: 'side', textures: { particle: ref(K + 'steel') },
    display: {
      gui: { rotation: [30, 45, 0], translation: [0, -1, 0], scale: [0.625, 0.625, 0.625] },
      fixed: { rotation: [0, 180, 0], translation: [0, 0, 0], scale: [0.5, 0.5, 0.5] },
      ground: { rotation: [0, 0, 0], translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
      thirdperson_righthand: { rotation: [75, 225, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
      thirdperson_lefthand: { rotation: [75, 225, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
      firstperson_righthand: { rotation: [0, 225, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] },
      firstperson_lefthand: { rotation: [0, 45, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] },
    },
  }, null, 2) + '\n');
}

// the Winding Station: its fixed parts are the block model; the drive, boom, reach and socket are the renderer's
const S = 'cj:';
write('block', 'jetpack_winding_station', G.stationFixed(S), S + 'walnut');
for (const [name, cubes] of [['drive', G.stationDrive(S)], ['boom', G.stationBoom(S)], ['reach', G.stationReach(S)], ['socket', G.stationSocket(S)]]) {
  write('block', 'jetpack_winding_station_' + name, cubes, S + 'walnut');
  parts++;
}
// the item: the whole station (fixed parts and the parked boom's pieces in their rest place), shown small, as it's 1.6 blocks tall
const at = (cubes, dy) => cubes.map((c) => ({ ...c, from: [c.from[0], c.from[1] + dy, c.from[2]], to: [c.to[0], c.to[1] + dy, c.to[2]], rot: c.rot }));
write('item', 'jetpack_winding_station', at([...G.stationFixed(S), ...G.stationDrive(S), ...G.stationBoom(S)], -6), S + 'walnut', {
  display: {
    gui: { rotation: [30, 225, 0], translation: [0, 0, 0], scale: [0.5, 0.5, 0.5] },
    fixed: { rotation: [0, 0, 0], translation: [0, 0, 0], scale: [0.5, 0.5, 0.5] },
    ground: { rotation: [0, 0, 0], translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
    thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.35, 0.35, 0.35] },
    firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 0, 0], scale: [0.35, 0.35, 0.35] },
    firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 0, 0], scale: [0.35, 0.35, 0.35] },
  },
});
// facing: the model's front (the hung jetpack's key) is north
const variants = Object.fromEntries([['north', 0], ['east', 90], ['south', 180], ['west', 270]].map(([f, y]) =>
  [`facing=${f}`, y ? { model: `${NS}:block/jetpack_winding_station`, y } : { model: `${NS}:block/jetpack_winding_station` }]));
fs.writeFileSync(path.join(ASSETS, 'blockstates', 'jetpack_winding_station.json'), JSON.stringify({ variants }, null, 2) + '\n');

// textures: only those used, per variant; the Curios slot icon
let tex = 0;
for (const [v, K, T] of VARIANTS) {
  for (const key of used[K]) {
    if (!T.OWN[key]) throw new Error('no painted texture ' + K + key);
    fs.writeFileSync(path.join(ASSETS, 'textures', 'block', `${v}_${key}.png`), T.OWN[key]);
    tex++;
  }
}
fs.mkdirSync(path.join(ASSETS, 'textures', 'slot'), { recursive: true });
fs.writeFileSync(path.join(ASSETS, 'textures', 'slot', 'jetpack.png'), G.TP.OWN.slot_icon);
console.log(`wrote ${parts} part models, the station's block and item models and blockstate, 2 jetpack items (3D), ${tex} textures, the slot icon`);
