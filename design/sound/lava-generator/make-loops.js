// Makes seamless game loops from the lava generator's approved loop renders: the steady middle of each, with its tail
// crossfaded into its head so it repeats without a gap or a pulse. Writes real/<id>_loop.ogg (mono 44.1 kHz).
// Usage: node design/sound/lava-generator/make-loops.js   (needs ffmpeg on the PATH)
const { execFileSync } = require('child_process');
const path = require('path');

const RATE = 44100;
const LOOPS = { whir: { from: 0.25, len: 1.0, fade: 0.25 }, burn: { from: 0.25, len: 1.0, fade: 0.25 } };
const real = path.join(__dirname, 'real');

for (const [id, o] of Object.entries(LOOPS)) {
  const raw = execFileSync('ffmpeg', ['-v', 'error', '-i', path.join(real, id + '.ogg'), '-f', 'f32le', '-ac', '1', '-ar', String(RATE), '-'], { maxBuffer: 1 << 26 });
  const src = new Float32Array(raw.buffer, raw.byteOffset, raw.length / 4);
  const a = Math.round(o.from * RATE), n = Math.round(o.len * RATE), f = Math.round(o.fade * RATE);
  // the loop is the segment minus its last f samples; its first f samples blend in the tail with an equal-power crossfade
  const out = new Float32Array(n - f);
  for (let i = 0; i < out.length; i++) {
    const head = src[a + i];
    if (i < f) {
      const t = i / f;
      out[i] = head * Math.sin(t * Math.PI / 2) + src[a + n - f + i] * Math.cos(t * Math.PI / 2);
    } else out[i] = head;
  }
  execFileSync('ffmpeg', ['-v', 'error', '-y', '-f', 'f32le', '-ar', String(RATE), '-ac', '1', '-i', '-', '-c:a', 'libvorbis', '-q:a', '5', path.join(real, id + '_loop.ogg')],
    { input: Buffer.from(out.buffer) });
  console.log(`${id}_loop.ogg: ${(out.length / RATE).toFixed(2)} s`);
}
