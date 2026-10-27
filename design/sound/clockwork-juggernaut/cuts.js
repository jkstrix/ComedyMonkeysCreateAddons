// Cuts the Clockwork Juggernaut's segments out of the BigSoundBank recordings (all CC0) into named files under
// design/sound/sources/bigsoundbank/cuts/ (prefix jug_), so a recipe's `pick` list can name each take. Onsets were found
// with an onset scan (see clockwork-juggernaut.md, Stage 2). Loop cuts fold their tail into their head so they repeat
// without a seam. SOURCES.md records the recordings themselves.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const SRC = path.resolve(__dirname, '../sources/bigsoundbank');
const OUT = path.join(SRC, 'cuts');
const SR = 44100;
const takes = (prefix, id, starts, len, o) => Object.fromEntries(starts.map((s, k) => [`${prefix}_${k + 1}`, [id, s, s + len, o]]));
// name: [recording id, start s, end s, options]  (loop: crossfade length in s)
const CUTS = {
  // the heartbeat: a grandfather clock's escapement, tick and tock alternating
  ...takes('jug_tick', '1567', [0.17, 2.32, 4.42, 6.68], 0.42),
  ...takes('jug_tock', '1567', [1.01, 3.16, 5.32, 7.51], 0.45),
  // stone: stones dropped one by one (footsteps, knees, fists)
  ...takes('jug_stone', '1022', [0.17, 1.69, 3.08, 4.62, 6.19, 8.26, 10.07], 0.45),
  // stone ground against stone: a tombstone slab sliding open
  jug_grind_stone_a: ['0580', 0.5, 2.0], jug_grind_stone_b: ['0580', 4.0, 5.5], jug_grind_stone_c: ['0580', 10.0, 11.5],
  // whooshes: a stick swung through the air
  ...takes('jug_whoosh', '0572', [0.0, 1.07, 2.24, 3.4, 6.46, 7.46], 0.8),
  // the shields: struck gongs, damped short
  jug_gong_a: ['1483', 0.1, 2.6], jug_gong_b: ['1485', 0.05, 2.4], jug_gong_c: ['1482', 0.05, 3.0],
  // bronze and iron struck: an anvil
  ...takes('jug_anvil', '3589', [0.1, 1.32, 2.39], 0.75),
  // a large spring (a door stop) flicked: the Mainspring, the leap
  jug_spring_a: ['0206', 0.05, 1.6], jug_spring_b: ['0206', 6.94, 9.0],
  // creaks: a barn door's low groans, a long creaking door, a metal door
  jug_creak_low_a: ['0539', 18.6, 19.5], jug_creak_low_b: ['0539', 26.35, 27.4], jug_creak_low_c: ['0539', 12.45, 12.95],
  jug_creak_long: ['0306', 0.3, 2.6], jug_creak_metal: ['0616', 0.1, 1.8],
  // metal grinding: a metal barrier dragged; an angle grinder's sparks
  jug_grind_metal: ['2359', 0.1, 2.0],
  jug_grinder_a: ['0857', 0.15, 0.5], jug_grinder_b: ['0857', 1.28, 1.62], jug_grinder_c: ['0857', 3.05, 3.4],
  // chains
  jug_chain_rattle: ['0361', 0.0, 1.6], jug_chain_clink_a: ['0359', 1.15, 1.55], jug_chain_clink_b: ['0359', 2.18, 2.6], jug_chain_clink_c: ['0359', 3.28, 3.75],
  // ratchets: a chain hoist (fast, continuous) and a fishing reel (a train of clicks)
  jug_ratchet_hoist_loop: ['1134', 7.4, 9.4, { loop: 0.2 }], jug_ratchet_hoist: ['1134', 6.3, 7.3],
  jug_reel_slow: ['1435', 0.3, 2.3], jug_reel: ['1435', 0.3, 4.6],
  // a hatch motor's whirr (the governor in the gear spin, the cog in flight)
  jug_whirr_loop: ['1804', 5.0, 9.0, { loop: 0.3 }],
  // steam: hisses from a steam locomotive
  jug_steam_long: ['3011', 0.1, 2.9], jug_steam_mid: ['3013', 0.1, 1.7], jug_steam_puff: ['3017', 0.0, 0.62], jug_steam_burst: ['3017', 0.78, 1.1],
  // a pendulum clock's chime
  jug_chime: ['3360', 2.9, 5.5],
  // a big key in a lock
  jug_key_lock_a: ['0378', 9.22, 9.65], jug_key_lock_b: ['0378', 11.4, 11.85],
  // welding sparks
  jug_weld: ['0950', 1.0, 2.5],
};

function decode(file) {
  const raw = execFileSync('ffmpeg', ['-loglevel', 'error', '-i', file, '-ac', '1', '-ar', String(SR), '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
  return new Float32Array(raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.length));
}
function writeWav(file, s) {
  const tmp = file + '.raw';
  fs.writeFileSync(tmp, Buffer.from(s.buffer, s.byteOffset, s.byteLength));
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'f32le', '-ar', String(SR), '-ac', '1', '-i', tmp, '-c:a', 'pcm_s16le', file]);
  fs.unlinkSync(tmp);
}
fs.mkdirSync(OUT, { recursive: true });
const cache = {};
for (const [name, [id, t0, t1, o = {}]] of Object.entries(CUTS)) {
  const all = (cache[id] = cache[id] || decode(path.join(SRC, id + '.ogg')));
  if (o.loop) {
    const X = Math.floor(o.loop * SR), a = Math.floor(t0 * SR), L = Math.floor((t1 - t0) * SR);
    const seg = all.slice(a, a + L + X), out = new Float32Array(L);
    for (let i = 0; i < L; i++) out[i] = seg[i];
    for (let i = 0; i < X; i++) { const f = i / X; out[i] = seg[i] * Math.sin(f * Math.PI / 2) + seg[L + i] * Math.cos(f * Math.PI / 2); }
    writeWav(path.join(OUT, name + '.wav'), out);
  } else {
    const s = all.slice(Math.floor(t0 * SR), Math.floor(t1 * SR));
    const fadeN = Math.min(Math.floor(0.03 * SR), s.length >> 2);
    for (let i = 0; i < fadeN; i++) s[s.length - 1 - i] *= i / fadeN;
    for (let i = 0; i < Math.min(64, s.length); i++) s[i] *= i / 64;
    writeWav(path.join(OUT, name + '.wav'), s);
  }
}
console.log('cut', Object.keys(CUTS).length, 'segments into', OUT);
module.exports = { CUTS };
