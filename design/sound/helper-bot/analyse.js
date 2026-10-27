// Describes recordings for picking cuts by numbers: the sounding segments (start, end, peak dB), each one's pitch
// (autocorrelation) and how tonal it is. node analyse.js <file.ogg ...>
const { execFileSync } = require('child_process');
const SR = 44100;
const pcm = (f) => { const b = execFileSync('ffmpeg', ['-v', 'error', '-i', f, '-ac', '1', '-ar', String(SR), '-f', 'f32le', '-'], { maxBuffer: 1 << 28 }); return new Float32Array(b.buffer, b.byteOffset, b.length / 4); };
const db = (x) => 20 * Math.log10(Math.max(1e-9, x));
function pitch(x, a, n) {
  // normalised autocorrelation over 80–3000 Hz; returns [Hz, clarity 0–1]
  let best = [0, 0];
  const e0 = x.slice(a, a + n).reduce((s, v) => s + v * v, 0) || 1e-9;
  for (let lag = Math.floor(SR / 3000); lag <= Math.floor(SR / 80); lag++) {
    let s = 0, e1 = 0;
    for (let i = 0; i < n; i++) { s += x[a + i] * x[a + i + lag]; e1 += x[a + i + lag] * x[a + i + lag]; }
    const r = s / Math.sqrt(e0 * (e1 || 1e-9));
    if (r > best[1]) best = [SR / lag, r];
  }
  return best;
}
for (const f of process.argv.slice(2)) {
  const x = pcm(f), hop = Math.round(SR * 0.01), env = [];
  for (let i = 0; i + hop <= x.length; i += hop) { let s = 0; for (let k = 0; k < hop; k++) s += x[i + k] * x[i + k]; env.push(Math.sqrt(s / hop)); }
  const peak = Math.max(...env), thr = peak * Math.pow(10, -24 / 20);
  const segs = [];
  let start = -1;
  env.forEach((v, k) => { if (v > thr && start < 0) start = k; if ((v <= thr || k === env.length - 1) && start >= 0) { if (k - start >= 4) segs.push([start, k]); start = -1; } });
  console.log(`${f.split(/[\\/]/).pop()}  ${(x.length / SR).toFixed(2)} s, peak ${db(peak).toFixed(1)} dB, ${segs.length} segments`);
  for (const [a, b] of segs.slice(0, 12)) {
    const mid = Math.floor((a + b) / 2) * hop, n = Math.min(2048, Math.floor((b - a) * hop * 0.5));
    const [hz, cl] = n > 512 ? pitch(x, Math.max(0, mid - n / 2), n) : [0, 0];
    const pk = Math.max(...env.slice(a, b));
    console.log(`   ${(a / 100).toFixed(2)}–${(b / 100).toFixed(2)} s  peak ${db(pk).toFixed(1)} dB  pitch ${hz.toFixed(0)} Hz  tonal ${cl.toFixed(2)}`);
  }
}
