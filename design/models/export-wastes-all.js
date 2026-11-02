// Runs every Clockwork Wastes exporter in the order they depend on each other:
//   1. clockwork-wastes.export.js            (the main set; prunes textures/block/wastes/*.png, writes charging_alcove's state)
//   2. clockwork-wastes-props.export.js      (the winding tower's props; textures in textures/block/wastes/props/)
//   3. clockwork-wastes-overgrowth.export.js (the plants; adds the alcove's living vines and the Shiftcaps' emitters)
//   4. electric-lights.export.js             (the lights; their emitters merged with the Shiftcaps')
//   5. clockwork-wastes-drum-diagonal.gen.js --export (the brick and deepslate diagonals; needs the main set's bricks)
//   6. clockwork-wastes-ridge.gen.js --export         (the Patina Ridge Roll; needs the main set's rib_patina)
// Usage: node design/models/export-wastes-all.js            (writes into the mod)
//        node design/models/export-wastes-all.js --check    (writes nothing: lists every file the run would change)
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const REPO = path.resolve(__dirname, '../..');
const STEPS = [
  ['clockwork-wastes.export.js'],
  ['clockwork-wastes-props.export.js'],
  ['clockwork-wastes-overgrowth.export.js'],
  ['electric-lights.export.js'],
  ['clockwork-wastes-drum-diagonal.gen.js', '--export'],
  ['clockwork-wastes-ridge.gen.js', '--export'],
];
const check = process.argv.includes('--check');
const overlay = check ? fs.mkdtempSync(path.join(os.tmpdir(), 'wastes-check-')) : null;
const env = { ...process.env, ...(check ? { WASTES_OVERLAY: overlay } : {}) };

let failed = 0;
for (const [file, ...args] of STEPS) {
  const pre = check ? ['-r', path.join(__dirname, 'fs-overlay.lib.js')] : [];
  try {
    const out = execFileSync(process.execPath, [...pre, path.join(__dirname, file), ...args], { cwd: REPO, env, encoding: 'utf8', maxBuffer: 64 << 20 });
    const last = out.trim().split('\n').pop();
    console.log(`ok   ${file} ${args.join(' ')}: ${last}`);
  } catch (e) {
    failed++;
    console.log(`FAIL ${file} ${args.join(' ')} (exit ${e.status})\n${(e.stdout || '').trim().split('\n').slice(-15).join('\n')}\n${(e.stderr || '').trim()}`);
  }
}

if (check) {
  // every file the run wrote or deleted, against the repo as it is (text the same but for Git's CRLF checkout is the same)
  const lf = (b) => b.toString('latin1').replace(/\r\n/g, '\n');
  const same = (a, b) => a.equals(b) || (!a.includes(0) && !b.includes(0) && lf(a) === lf(b));
  const changed = [];
  const walk = (dir) => {
    for (const n of fs.readdirSync(dir)) {
      const f = path.join(dir, n);
      if (fs.statSync(f).isDirectory()) walk(f);
      else if (f !== path.join(overlay, '.deleted.json')) {
        const r = path.relative(overlay, f).split(path.sep).join('/'), onDisk = path.join(REPO, r);
        if (!fs.existsSync(onDisk)) changed.push('new      ' + r);
        else if (!same(fs.readFileSync(onDisk), fs.readFileSync(f))) changed.push('changed  ' + r);
      }
    }
  };
  walk(overlay);
  const tomb = path.join(overlay, '.deleted.json');
  const dead = fs.existsSync(tomb) ? JSON.parse(fs.readFileSync(tomb, 'utf8')) : [];
  const gone = (r) => {
    const f = path.join(REPO, r);
    if (!fs.existsSync(f)) return;
    if (fs.statSync(f).isDirectory()) for (const n of fs.readdirSync(f)) gone(`${r}/${n}`);
    else if (!fs.existsSync(path.join(overlay, r))) changed.push('deleted  ' + r);
  };
  dead.forEach(gone);
  fs.rmSync(overlay, { recursive: true, force: true });
  changed.sort((a, b) => a.slice(9).localeCompare(b.slice(9)));
  console.log(changed.length ? `${changed.length} files would change:\n${changed.join('\n')}` : 'check: every file matches the exporters');
  process.exitCode = failed || changed.length ? 1 : 0;
} else process.exitCode = failed ? 1 : 0;
