// Game export for Trade Bot, called by trade-bot.gen.js with --game. Writes into the mod's resources:
//   textures/entity/trade_bot.png       bot + station cubes, plus the trade props (lobbed package, emerald), re-packed
//   textures/entity/trade_bot_glow.png  the same layout with only the glowing cubes, drawn full bright in game
//   trade_bot/geometry.json             bones (pivot, rotation) and cubes (from, to, uv) in Blockbench units
//   trade_bot/clips.json                every clip's sampled tracks, step tracks, root mode and sound cues
// Geometry and tracks stay in Blockbench's frame (Y up, front -Z, right +X); the game converts them in one place.
const fs = require('fs');
const path = require('path');

const RES = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons');

// how each clip's root (trade_bot bone) is used in game; see design/plans/trade-bot.md, milestone 0
const PHYSICAL = ['hop_on_slab', 'hop_off_slab', 'hop_on_block', 'hop_off_block', 'stairs_up', 'stairs_down', 'dock_lights_out', 'action_comes_in'];
const GAME = ['rolling', 'start_rolling', 'stop_rolling', 'turn_left', 'turn_right', 'turn_around', 'sees_player']; // sees_player: the game turns him to face the player
const rootMode = (name) => (PHYSICAL.includes(name) ? 'physical' : GAME.includes(name) ? 'game' : 'visual');
// glowing cubes live under these bones; they're painted into the glow texture too
const GLOW_BONES = ['iris_glow_left', 'iris_glow_right', 'lamp_glow', 'station_glow'];

module.exports = function exportGame({ bot, station, props = [], propDrop = 0, paintCube, png, animations, cues }) {
  // bones and cubes of the two in-game roots, parents first
  const bones = [], cubes = [];
  function walk(n, parent, glow) {
    const isGlow = glow || GLOW_BONES.includes(n.name);
    const bone = { name: n.name, parent, pivot: n.origin, rotation: n.rot, cubes: [] };
    bones.push(bone);
    for (const k of n.kids) {
      if (k.kind === 'g') { walk(k, n.name, isGlow); continue; }
      const rot = k.rotation && k.rotation.some((v) => v !== 0);
      const cube = { node: k, glow: isGlow, out: { name: k.name, from: k.from, to: k.to } };
      cubes.push(cube);
      // a rotated cube becomes its own small bone (ModelPart cubes can't rotate), pivoting on the cube's origin
      if (rot) bones.push({ name: k.name, parent: n.name, pivot: k.origin, rotation: k.rotation, cubes: [cube.out], synthetic: true });
      else bone.cubes.push(cube.out);
    }
  }
  walk(bot, null, false);
  walk(station, null, false); // the station root is kept in the geometry, so its sub-bones keep their pivots
  // the trade props (lobbed package, emerald), lifted back from where the preview parks props, under a root of their own
  const up = (v) => v && [v[0], v[1] + propDrop, v[2]];
  const lift = (n) => (n.kind === 'g' ? { ...n, origin: up(n.origin), kids: n.kids.map(lift) } : { ...n, from: up(n.from), to: up(n.to), origin: up(n.origin) });
  bones.push({ name: 'trade_props', parent: null, pivot: [0, 0, 0], rotation: [0, 0, 0], cubes: [] });
  for (const p of props) walk(lift(p), 'trade_props', false);

  // pack: same shelf packer as the preview, on the smallest square texture that fits
  const size = [64, 128, 256].find((S) => {
    let sx = 0, sy = 0, rowH = 0;
    for (const c of [...cubes].sort((a, b) => b.node.netH - a.node.netH || b.node.netW - a.node.netW)) {
      if (sx + c.node.netW > S) { sx = 0; sy += rowH; rowH = 0; }
      c.uv = [sx, sy]; sx += c.node.netW; rowH = Math.max(rowH, c.node.netH);
    }
    return sy + rowH <= S;
  });
  if (!size) throw new Error('game texture overflow');
  for (const c of cubes) c.out.uv = c.uv;

  const tex = new Uint8Array(size * size * 4), glow = new Uint8Array(size * size * 4);
  for (const c of cubes) {
    paintCube(c.node, c.uv, tex, size);
    if (c.glow) paintCube(c.node, c.uv, glow, size);
  }
  fs.mkdirSync(path.join(RES, 'textures/entity'), { recursive: true });
  fs.writeFileSync(path.join(RES, 'textures/entity/trade_bot.png'), png(size, size, tex));
  fs.writeFileSync(path.join(RES, 'textures/entity/trade_bot_glow.png'), png(size, size, glow));

  const keep = new Set(bones.map((b) => b.name));
  keep.delete('station'); // the station root never moves in game (it's a block); the preview parks it per clip
  fs.mkdirSync(path.join(RES, 'trade_bot'), { recursive: true });
  const geometry = { texture: [size, size], bones: bones.map(({ name, parent, pivot, rotation, cubes: cs }) => ({ name, parent, pivot, rotation, cubes: cs })) };
  fs.writeFileSync(path.join(RES, 'trade_bot/geometry.json'), JSON.stringify(geometry, null, 1) + '\n');

  // clips: sampled channels become flat arrays at the clip's frame rate; step keys stay as keys
  const num = (s) => +(+s).toFixed(3);
  const clips = animations.map((a) => {
    const tracks = {}, steps = {};
    for (const an of Object.values(a.animators)) {
      if (an.type !== 'bone' || !keep.has(an.name)) continue;
      for (const ch of ['rotation', 'position', 'scale']) {
        const ks = an.keyframes.filter((k) => k.channel === ch).sort((p, q) => p.time - q.time);
        const stepKs = ks.filter((k) => k.interpolation === 'step');
        const sampled = ks.filter((k) => k.interpolation !== 'step');
        if (stepKs.length) (steps[an.name] = steps[an.name] || {})[ch] = stepKs.map((k) => [num(k.time), ...['x', 'y', 'z'].map((i) => num(k.data_points[0][i]))]);
        if (!sampled.length) continue;
        const fps = sampled.length > 1 ? Math.round(1 / (sampled[1].time - sampled[0].time)) : 20;
        sampled.forEach((k, i) => { if (Math.abs(k.time - i / fps) > 0.002) throw new Error(`${a.name} ${an.name}.${ch}: keys not evenly spaced`); });
        (tracks[an.name] = tracks[an.name] || {})[ch] = {
          fps, interp: sampled[0].interpolation,
          values: sampled.flatMap((k) => ['x', 'y', 'z'].map((i) => num(k.data_points[0][i]))),
        };
      }
    }
    return { name: a.name, length: a.length, mode: a.loop, root: rootMode(a.name), tracks, steps, cues: (cues && cues.clips[a.name]) || [] };
  });
  fs.writeFileSync(path.join(RES, 'trade_bot/clips.json'), JSON.stringify({ clips }) + '\n');
  const kb = (f) => Math.round(fs.statSync(path.join(RES, f)).size / 1024) + ' KB';
  console.error(`game export: ${bones.length} bones, ${cubes.length} cubes, texture ${size}px, ${clips.length} clips (${kb('trade_bot/clips.json')})`);
};
