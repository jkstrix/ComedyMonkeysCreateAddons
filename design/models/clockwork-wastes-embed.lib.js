// Puts a generated bot (design/models/clockwork-wastes-<bot>.bbmodel: box UV, its own atlas, its clips) into a Generic Model
// scene written by process-box.lib's preview(), for previews that show a bot with blocks (the heart hall's tethers, the wall
// vent's burst). Preview only: nothing here goes into the mod.
// The bot's cubes get per-face UVs from Blockbench's box-UV layout (up flipped both ways, down flipped across), on its own
// texture at its own UV size; its bones keep their names; chosen clips' tracks are copied into the scene's clips, shifted
// in time (holding their first frame until they start).
const fs = require('fs');
const crypto = require('crypto');
const uuid = () => crypto.randomUUID();

// box-UV face rectangles as Blockbench lays them out ([u0, v0, u1, v1], flips included)
function boxFaces(size, [u, v]) {
  const [w, h, d] = size;
  return {
    east: [u, v + d, u + d, v + d + h], west: [u + d + w, v + d, u + 2 * d + w, v + d + h],
    up: [u + d + w, v + d, u + d, v], down: [u + d + 2 * w, v, u + d + w, v + d],
    south: [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], north: [u + d, v + d, u + d + w, v + d + h],
  };
}

// scene: the parsed scene .bbmodel (edited in place); botFile: the bot's .bbmodel; place: { name, offset: [x, y, z], rot: [x, y, z] }
// clips: { sceneClipName: { from: botClipName, shift: seconds } }
function embedBot(scene, botFile, place, clips = {}) {
  const bot = JSON.parse(fs.readFileSync(botFile, 'utf8'));
  const tex = bot.textures[0], ti = scene.textures.length;
  scene.textures.push({ ...tex, uuid: uuid(), id: String(ti), uv_width: tex.width, uv_height: tex.height });
  const map = new Map(), off = place.offset;
  const sh = (p) => [p[0] + off[0], p[1] + off[1], p[2] + off[2]];
  for (const e of bot.elements) {
    const id = uuid(); map.set(e.uuid, id);
    const size = [0, 1, 2].map((a) => +(e.to[a] - e.from[a]).toFixed(4)), R = boxFaces(size, e.uv_offset), faces = {};
    for (const f of Object.keys(R)) faces[f] = { uv: R[f], texture: ti };
    scene.elements.push({ name: e.name, type: 'cube', uuid: id, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: sh(e.from), to: sh(e.to), origin: sh(e.origin), rotation: e.rotation || [0, 0, 0], autouv: 0, color: 0, inflate: 0, shade: !/(_lit|_glow)$|^wrist_seam_|^pectoral|^studs_|^spring_seam$|^petal_\d_seam$/.test(e.name), faces });
  }
  for (const g of bot.groups) { const id = uuid(); map.set(g.uuid, id); scene.groups.push({ ...g, uuid: id, origin: sh(g.origin), children: [] }); }
  const remap = (n) => (typeof n === 'string' ? map.get(n) : { ...n, uuid: map.get(n.uuid), isOpen: false, children: n.children.map(remap) });
  const placeId = uuid();
  scene.groups.push({ name: place.name, uuid: placeId, origin: off, rotation: place.rot || [0, 0, 0], export: true, locked: false, visibility: true, color: 0, children: [], autouv: 0, isOpen: false });
  scene.outliner.push({ name: place.name, uuid: placeId, origin: off, rotation: place.rot || [0, 0, 0], export: true, isOpen: false, locked: false, visibility: true, autouv: 0, color: 0, children: bot.outliner.map(remap) });
  for (const [sceneClip, { from, shift = 0 }] of Object.entries(clips)) {
    const src = bot.animations.find((a) => a.name === from); if (!src) throw new Error('no bot clip ' + from);
    let dst = scene.animations.find((a) => a.name === sceneClip);
    if (!dst) { dst = { uuid: uuid(), name: sceneClip, loop: src.loop, override: false, length: src.length + shift, snapping: 20, selected: false, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators: {} }; scene.animations.push(dst); }
    dst.length = Math.max(dst.length, src.length + shift);
    for (const [gid, an] of Object.entries(src.animators)) {
      const kfs = an.keyframes.map((k) => ({ ...k, uuid: uuid(), time: +(k.time + shift).toFixed(3) }));
      if (shift > 0) for (const ch of new Set(an.keyframes.map((k) => k.channel))) { const first = an.keyframes.filter((k) => k.channel === ch).sort((a, b) => a.time - b.time)[0]; kfs.push({ ...first, uuid: uuid(), time: 0, interpolation: 'step' }); }
      dst.animators[map.get(gid)] = { ...an, keyframes: kfs };
    }
  }
  return map;
}
module.exports = { embedBot, boxFaces };
