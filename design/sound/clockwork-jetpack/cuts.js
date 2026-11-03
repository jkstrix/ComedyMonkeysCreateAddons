// Cuts the Clockwork Jetpack's segments out of the BigSoundBank recordings (all CC0) into named files under
// design/sound/sources/bigsoundbank/cuts/ (prefix cj_), so a recipe's `pick` list can name each take. Segments were
// found from each recording's loudness envelope (see clockwork-jetpack.md, Stage 2). SOURCES.md records the recordings.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const SRC = path.resolve(__dirname, '../sources/bigsoundbank');
const OUT = path.join(SRC, 'cuts');
const SR = 44100;
// name: [recording id, start s, end s]
const CUTS = {
  // the fans spinning up and down: a small quadrotor drone starting (#1) and landing (#4), and an electric fan (#1)
  cj_fan_up_drone: ['3545', 0.3, 2.6], cj_fan_down_drone: ['3548', 10.6, 11.8],
  cj_fan_up_fan: ['0078', 0.25, 2.5], cj_fan_down_fan: ['0078', 3.0, 4.5],
  // the canvas: shaken nylon flags, each a single sharp snap; a longer shake for the canvas going slack
  cj_canvas_snap_a: ['1788', 0.1, 0.6], cj_canvas_snap_b: ['1787', 0.65, 1.05], cj_canvas_snap_c: ['1790', 0.35, 0.85],
  cj_canvas_slack: ['1789', 0.4, 1.0],
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
for (const [name, [id, t0, t1]] of Object.entries(CUTS)) {
  const all = (cache[id] = cache[id] || decode(path.join(SRC, id + '.ogg')));
  const s = all.slice(Math.floor(t0 * SR), Math.floor(t1 * SR));
  const fadeN = Math.min(Math.floor(0.03 * SR), s.length >> 2);
  for (let i = 0; i < fadeN; i++) s[s.length - 1 - i] *= i / fadeN;
  for (let i = 0; i < Math.min(64, s.length); i++) s[i] *= i / 64;
  writeWav(path.join(OUT, name + '.wav'), s);
}
console.log('cut', Object.keys(CUTS).length, 'segments into', OUT);
module.exports = { CUTS };
