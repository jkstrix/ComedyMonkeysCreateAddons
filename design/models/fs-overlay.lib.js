// A write overlay for export-wastes-all.js --check: preloaded into each exporter (node -r), it sends every write, mkdir and
// delete under the repo to an overlay folder (WASTES_OVERLAY) instead, and reads see the overlay first, so the exporters run
// in order as they would for real while the repo stays untouched. Deletes are tombstones in <overlay>/.deleted.json.
const fs = require('fs');
const path = require('path');

const REPO = path.resolve(__dirname, '../..');
const O = process.env.WASTES_OVERLAY;
if (O) {
  const real = { ...fs };
  const TOMB = path.join(O, '.deleted.json');
  const dead = new Set(real.existsSync(TOMB) ? JSON.parse(real.readFileSync(TOMB, 'utf8')) : []);
  const save = () => real.writeFileSync(TOMB, JSON.stringify([...dead].sort()));
  // the repo-relative path (forward slashes), or null outside the repo
  const rel = (p) => {
    if (typeof p !== 'string') return null;
    const r = path.relative(REPO, path.resolve(p));
    return r && !r.startsWith('..') && !path.isAbsolute(r) ? r.split(path.sep).join('/') : null;
  };
  const over = (r) => path.join(O, r);
  const isDead = (r) => dead.has(r) || [...dead].some((d) => r.startsWith(d + '/'));

  fs.writeFileSync = (p, data, opts) => {
    const r = rel(p);
    if (r === null) return real.writeFileSync(p, data, opts);
    real.mkdirSync(path.dirname(over(r)), { recursive: true });
    real.writeFileSync(over(r), data, opts);
    if (dead.delete(r)) save();
  };
  fs.mkdirSync = (p, opts) => {
    const r = rel(p);
    if (r === null) return real.mkdirSync(p, opts);
    real.mkdirSync(over(r), { recursive: true });
  };
  fs.existsSync = (p) => {
    const r = rel(p);
    if (r === null) return real.existsSync(p);
    if (real.existsSync(over(r))) return true;
    return !isDead(r) && real.existsSync(p);
  };
  fs.readFileSync = (p, opts) => {
    const r = rel(p);
    if (r === null) return real.readFileSync(p, opts);
    if (real.existsSync(over(r))) return real.readFileSync(over(r), opts);
    if (isDead(r)) { const e = new Error(`ENOENT (overlay): ${p}`); e.code = 'ENOENT'; throw e; }
    return real.readFileSync(p, opts);
  };
  fs.readdirSync = (p, opts) => {
    const r = rel(p);
    if (r === null || (opts && opts.withFileTypes)) return real.readdirSync(p, opts);
    const names = new Set(real.existsSync(p) ? real.readdirSync(p).filter((n) => !isDead(`${r}/${n}`)) : []);
    if (real.existsSync(over(r))) for (const n of real.readdirSync(over(r))) names.add(n);
    return [...names].sort();
  };
  fs.unlinkSync = (p) => {
    const r = rel(p);
    if (r === null) return real.unlinkSync(p);
    if (real.existsSync(over(r))) real.unlinkSync(over(r));
    dead.add(r);
    save();
  };
  fs.rmSync = (p, opts) => {
    const r = rel(p);
    if (r === null) return real.rmSync(p, opts);
    if (real.existsSync(over(r))) real.rmSync(over(r), { recursive: true, force: true });
    dead.add(r);
    save();
  };
}
module.exports = { REPO };
