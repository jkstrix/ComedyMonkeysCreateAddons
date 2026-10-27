// The containment field's voice, from the user's own Stable Audio 3 take (made in the audio-studio web UI, 2026-10-27):
//   raw and cleaned copies, seamless 2 s loops of each (the field hum), and the hum "cutting out": the same hum gated
//   off and on in the shield's flicker pattern (design/models/monster-cage.gen.js, containment clip, 2.0-5.0 s).
// Writes into design/sound/sources/generated/; recorded in its SOURCES.md.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const TAKE = 'C:/audio-studio-mcp/outputs/20261027-031402_a_steady_electrical_containment_field_hum_low_60_501191783_v2.wav';
const GEN = path.resolve(__dirname, '../sources/generated');
const SR = 44100;
const CLEAN = 'highpass=f=40,afftdn=nr=18:nf=-42:tn=1,lowpass=f=8000';

const decode = (file, af) => {
  const raw = execFileSync('ffmpeg', ['-loglevel', 'error', '-i', file, ...(af ? ['-af', af] : []), '-ac', '1', '-ar', String(SR), '-f', 'f32le', '-'], { maxBuffer: 1 << 28 });
  return new Float32Array(raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.length));
};
const write = (name, s) => {
  const tmp = path.join(GEN, name + '.raw');
  fs.writeFileSync(tmp, Buffer.from(s.buffer, s.byteOffset, s.byteLength));
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'f32le', '-ar', String(SR), '-ac', '1', '-i', tmp, '-c:a', 'pcm_s16le', path.join(GEN, name + '.wav')]);
  fs.unlinkSync(tmp);
};
// a seamless loop: take L + X from t0, fold the last X over the first X with equal-power fades
function loop(s, t0, L, X) {
  const a = Math.floor(t0 * SR), n = Math.floor(L * SR), x = Math.floor(X * SR), out = new Float32Array(n);
  for (let i = 0; i < n; i++) out[i] = s[a + i];
  for (let i = 0; i < x; i++) { const f = i / x; out[i] = s[a + i] * Math.sin(f * Math.PI / 2) + s[a + n + i] * Math.cos(f * Math.PI / 2); }
  return out;
}
// The shield's flicker, relative to the countdown's start (containment clip times minus 2.0): it drops out at the first
// time, comes back at the next, and so on; after the last drop-out it stays down until the field fails at 3.0.
const TOGGLES = [0, 0.15, 0.3, 0.7, 0.8, 1.1, 1.35, 1.4, 1.9, 2.0, 2.25, 2.6, 2.7];
function cutout(hum) {
  const n = Math.floor(3.0 * SR), out = new Float32Array(n);
  const on = (t) => { let state = true; for (const k of TOGGLES) if (t >= k) state = !state; return state; };
  let g = 1;
  for (let i = 0; i < n; i++) {
    const t = i / SR, target = on(t) ? 1 : 0.05;              // a faint residual buzz while it's out, not dead silence
    g += (target - g) * (target > g ? 0.02 : 0.012);           // about 1-2 ms edges: a hard electrical cut, no click
    // the power surging back: a short swell above full each time it returns
    let surge = 0;
    for (const k of TOGGLES.filter((_, j) => j % 2 === 1)) if (t >= k && t < k + 0.09) surge = 0.3 * (1 - (t - k) / 0.09);
    out[i] = hum[i % hum.length] * g * (1 + surge);
  }
  return out;
}

const raw = decode(TAKE), clean = decode(TAKE, CLEAN);
write('sa3_field_hum_v2_raw', raw);
write('sa3_field_hum_v2_clean', clean);
const loopRaw = loop(raw, 0.5, 2.0, 0.35), loopClean = loop(clean, 0.5, 2.0, 0.35);
write('sa3_field_hum_loop_raw', loopRaw);
write('sa3_field_hum_loop_clean', loopClean);
write('sa3_field_hum_cutout_raw', cutout(loopRaw));
write('sa3_field_hum_cutout_clean', cutout(loopClean));
console.log('field hum: 2 copies, 2 loops, 2 cut-outs in', GEN);
