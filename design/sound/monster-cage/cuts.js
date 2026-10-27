// Cuts the useful segments out of the BigSoundBank recordings (all CC0) into named files under
// design/sound/sources/bigsoundbank/cuts/, so a cue's `pick` list can name them (each with its own trim). Loop cuts get
// their tail crossfaded into their head so they repeat without a seam. Every cut is listed in CUTS below with the
// recording it came from; SOURCES.md records the recordings themselves.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const SRC = path.resolve(__dirname, '../sources/bigsoundbank');
const OUT = path.join(SRC, 'cuts');
const SR = 44100;
// name: [recording id, start s, end s, options]  (loop: crossfade length in s)
const CUTS = {
  // the klaxon: three takes of a real "oogah" horn
  klaxon_oogah_a: ['2541', 0, 1.15], klaxon_oogah_b: ['2542', 0, 1.41], klaxon_oogah_c: ['2543', 0, 0.76],
  // iron bars: a metal gate grinding shut, a stick dragged across bars, a falling iron bar
  bars_gate: ['0303', 0, 1.2], bars_stick: ['0995', 3.1, 3.9], bars_iron_clang: ['0235', 0, 1.0],
  // kibble: raw rice poured into a metal saucepan, dry cereal into a bowl
  kibble_rice_pan: ['0201', 1.0, 1.9], kibble_cereal_a: ['2072', 0.4, 1.3], kibble_cereal_b: ['2071', 0.9, 1.8], flakes_cereal: ['2073', 0.2, 0.8],
  // crunching: bones cracking, a rusk, crisps
  crunch_bones: ['0596', 2.2, 2.8], crunch_rusk: ['0407', 0.1, 0.7], crunch_crisps: ['1432', 1.5, 1.9],
  // locks: a barn lock locking and unlocking, a big key in a lock, an electric strike
  lock_barn_lock: ['1540', 0.5, 1.0], lock_barn_unlock: ['1539', 0.1, 0.8], lock_big_key: ['0378', 1.0, 1.5], strike_clunk: ['3442', 0, 0.6],
  // a plastic pot lid (the tank's feeder hatch)
  lid_pot_a: ['0686', 0.2, 0.6], lid_pot_b: ['0686', 0.6, 1.0],
  // the trident hatch: a metal mailbox flap
  hatch_mailbox_open: ['1528', 0.1, 0.6], hatch_mailbox_shut: ['1527', 0.2, 0.8],
  // electricity: shocks and short welds (sparks), a flashing bulb (flicker), a switch
  spark_shock_a: ['0134', 0.8, 1.1], spark_shock_b: ['0134', 1.6, 1.85], spark_shock_c: ['0134', 2.2, 2.45], spark_weld: ['0952', 2.7, 3.1],
  flicker_bulb_a: ['2143', 0, 0.4], flicker_bulb_b: ['2144', 0, 0.8], switch_click: ['0026', 0.2, 0.45],
  // the breakout: sheet steel bars falling
  crash_steel_a: ['1776', 0, 1.5], crash_steel_b: ['1771', 0, 0.8],
  // breath: blowing (Chow Breath)
  puff_blow_a: ['0748', 0.1, 0.7], puff_blow_b: ['0747', 0.2, 0.8],
  // loops: transformer and neon hum (the field), an electric fence (option), a small air compressor and an electric pump
  // (the aquarium pump), a fermentation airlock bubbling (the air stone)
  loop_transformer: ['2437', 2, 6, { loop: 0.4 }], loop_neon: ['2118', 2, 6, { loop: 0.4 }], loop_fence: ['1252', 0.5, 6.5, { loop: 0.2 }],
  loop_air_compressor: ['0856', 5, 9, { loop: 0.4 }], loop_electric_pump: ['0709', 1, 5, { loop: 0.4 }],
  loop_airlock_a: ['3560', 10, 16, { loop: 0.3 }], loop_airlock_b: ['3561', 10, 16, { loop: 0.3 }],
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
for (const [name, [id, t0, t1, o = {}]] of Object.entries(CUTS)) {
  const all = decode(path.join(SRC, id + '.ogg'));
  if (o.loop) {
    // take L + X, then fold the last X seconds over the first X with equal-power fades
    const X = Math.floor(o.loop * SR), a = Math.floor(t0 * SR), L = Math.floor((t1 - t0) * SR);
    const seg = all.slice(a, a + L + X), out = new Float32Array(L);
    for (let i = 0; i < L; i++) out[i] = seg[i];
    for (let i = 0; i < X; i++) { const f = i / X; out[i] = seg[i] * Math.sin(f * Math.PI / 2) + seg[L + i] * Math.cos(f * Math.PI / 2); }
    writeWav(path.join(OUT, name + '.wav'), out);
  } else {
    const s = all.slice(Math.floor(t0 * SR), Math.floor(t1 * SR));
    const fadeN = Math.min(Math.floor(0.03 * SR), s.length >> 2);
    for (let i = 0; i < fadeN; i++) s[s.length - 1 - i] *= i / fadeN;      // a short fade-out so a cut never clicks
    for (let i = 0; i < Math.min(64, s.length); i++) s[i] *= i / 64;
    writeWav(path.join(OUT, name + '.wav'), s);
  }
}
console.log('cut', Object.keys(CUTS).length, 'segments into', OUT);
module.exports = { CUTS };
