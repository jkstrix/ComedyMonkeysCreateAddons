// Game export for Helper Bot, called by helper-bot.clips.js when the generator runs with --game. Writes into the
// mod's resources, in the same formats as Trade Bot's (see trade-bot.game-export.js):
//   textures/entity/helper_bot/plain.png      his cubes re-packed, the dye zone in andesite
//   textures/entity/helper_bot/<dye>.png      the same, the dye zone in each of the 16 muted dye colours
//   textures/entity/helper_bot/glow.png       the same layout with only the glowing cubes (his irises)
//   helper_bot/geometry.json                  bones (pivot, rotation) and cubes (from, to, uv) in Blockbench units
//   helper_bot/clips.json                     every clip's sampled tracks, step tracks and sound cues
// Geometry and tracks stay in Blockbench's frame (Y up, front -Z, right +X); the game converts them in one place.
// The preview props (the controls, ceiling and ticker stand-ins) aren't exported.
const fs = require('fs');
const path = require('path');

const RES = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons');
const GLOW_BONES = ['iris_glow_left', 'iris_glow_right'];

module.exports = function exportGame({ bot, P, DYES, dyeShades, paintCube, png, animations, cues }) {
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

  const texDir = path.join(RES, 'textures/entity/helper_bot');
  fs.rmSync(texDir, { recursive: true, force: true });
  fs.mkdirSync(texDir, { recursive: true });
  const plain = new Uint8Array(size * size * 4), glow = new Uint8Array(size * size * 4);
  const panel = P.panel;
  P.panel = P.andesite;
  for (const c of cubes) {
    paintCube(c.node, c.uv, plain, size);
    if (c.glow) paintCube(c.node, c.uv, glow, size);
  }
  fs.writeFileSync(path.join(texDir, 'plain.png'), png(size, size, plain));
  fs.writeFileSync(path.join(texDir, 'glow.png'), png(size, size, glow));
  for (const [name, hex] of Object.entries(DYES)) {
    const tex = Uint8Array.from(plain);
    P.panel = dyeShades(hex);
    for (const c of cubes) if (c.node.dye) paintCube(c.node, c.uv, tex, size);
    fs.writeFileSync(path.join(texDir, name + '.png'), png(size, size, tex));
  }
  P.panel = panel;

  const keep = new Set(bones.map((b) => b.name));
  fs.mkdirSync(path.join(RES, 'helper_bot'), { recursive: true });
  const geometry = { texture: [size, size], bones: bones.map(({ name, parent, pivot, rotation, cubes: cs }) => ({ name, parent, pivot, rotation, cubes: cs })) };
  fs.writeFileSync(path.join(RES, 'helper_bot/geometry.json'), JSON.stringify(geometry, null, 1) + '\n');

  const num = (s) => +(+s).toFixed(3);
  const clips = animations.filter((a) => a.name !== 'pose_sheet').map((a) => {
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
        (tracks[an.name] = tracks[an.name] || {})[ch] = { fps, interp: sampled[0].interpolation, values: sampled.flatMap((k) => ['x', 'y', 'z'].map((i) => num(k.data_points[0][i]))) };
      }
    }
    return { name: a.name, length: a.length, mode: a.loop, root: 'visual', tracks, steps, cues: (cues && cues.clips[a.name]) || [] };
  });
  fs.writeFileSync(path.join(RES, 'helper_bot/clips.json'), JSON.stringify({ clips }) + '\n');
  const kb = (f) => Math.round(fs.statSync(path.join(RES, f)).size / 1024) + ' KB';
  console.error(`game export: ${bones.length} bones, ${cubes.length} cubes, texture ${size}px x ${2 + Object.keys(DYES).length}, ${clips.length} clips (${kb('helper_bot/clips.json')})`);
};
