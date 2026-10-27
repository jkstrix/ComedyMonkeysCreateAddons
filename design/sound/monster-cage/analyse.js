// Prints each recording's length, peak, and a loudness envelope (one character per `step` seconds), so trims can be
// chosen without listening. node analyse.js <file...> [--step 0.1]
const { execFileSync } = require('child_process');
const args = process.argv.slice(2);
const si = args.indexOf('--step');
const step = si >= 0 ? +args.splice(si, 2)[1] : 0.1;
const SR = 22050, CH = ' .:-=+*#%@';
for (const f of args) {
  const raw = execFileSync('ffmpeg', ['-loglevel', 'error', '-i', f, '-ac', '1', '-ar', String(SR), '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
  const s = new Float32Array(raw.buffer, raw.byteOffset, raw.length / 4);
  const n = Math.floor(step * SR), env = [];
  let peak = 0;
  for (let i = 0; i < s.length; i += n) {
    let sum = 0;
    for (let j = i; j < Math.min(s.length, i + n); j++) { sum += s[j] * s[j]; peak = Math.max(peak, Math.abs(s[j])); }
    env.push(Math.sqrt(sum / n));
  }
  const db = env.map((r) => 20 * Math.log10(r + 1e-9));
  const line = db.map((d) => CH[Math.max(0, Math.min(9, Math.round((d + 60) / 6)))]).join('');
  const name = f.replace(/^.*[\\/]/, '');
  console.log(`${name.padEnd(14)} ${(s.length / SR).toFixed(2).padStart(6)}s peak ${(20 * Math.log10(peak)).toFixed(1).padStart(5)} dB |${line.slice(0, 160)}|`);
}
