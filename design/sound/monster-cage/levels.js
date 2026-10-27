// Sets each real event's `level` from the staging ladder: measures the rendered take 1 (loops as integrated LUFS,
// one-shots as max momentary LUFS), compares it with the recipe's `target`, and writes levels.json for make-cues.js.
// node levels.js <cues.json> <realDir> [<cues.json> <realDir> ...]
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const file = path.join(__dirname, 'levels.json');
let levels = {};
try { levels = JSON.parse(fs.readFileSync(file, 'utf8')); } catch (e) { /* new */ }
const args = process.argv.slice(2);
for (let a = 0; a < args.length; a += 2) {
  const cues = JSON.parse(fs.readFileSync(args[a], 'utf8')), dir = args[a + 1];
  for (const [id, ev] of Object.entries(cues.events)) {
    if (!ev.real || ev.real.target === undefined) continue;
    const f = path.join(dir, id + '.ogg'), loop = id.endsWith('_loop') || id === 'field_hum';
    const r = require('child_process').spawnSync('ffmpeg', ['-hide_banner', '-nostats', '-v', 'verbose', '-i', f, '-af', loop ? 'ebur128=framelog=verbose' : 'apad=pad_dur=0.5,ebur128=framelog=verbose', '-f', 'null', '-'], { encoding: 'utf8' });
    const log = r.stderr;
    const M = [...log.matchAll(/M:\s*(-?[\d.]+)/g)].map((m) => +m[1]).filter((v) => v > -70);
    const I = +((log.match(/I:\s*(-?[\d.]+) LUFS\s*\n\s*Threshold/) || [])[1] ?? NaN);
    const measured = loop ? I : Math.max(...M);
    const key = `${cues.namespace}.${id}`, old = ev.real.level ?? 0.5;   // the level this render was made at
    let next = old * Math.pow(10, (ev.real.target - measured) / 20);
    const capped = next > 1;
    next = Math.min(1, next);
    levels[key] = +next.toFixed(4);
    console.log(`${key.padEnd(32)} ${loop ? 'I' : 'M'} ${measured.toFixed(1).padStart(6)} -> target ${ev.real.target}  level ${old} -> ${levels[key]}${capped ? '  (capped: too quiet even at full level)' : ''}`);
  }
}
fs.writeFileSync(file, JSON.stringify(levels, null, 2));
