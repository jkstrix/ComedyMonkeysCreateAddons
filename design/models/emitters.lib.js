// light/emitters.json (Colorful Lighting's coloured light by block state) is shared: each exporter writes only its own
// ids, which emitters.owners.json records, so an id it stops writing goes. Keys are written sorted, so the file never churns.
const fs = require('fs');
const path = require('path');
const { ASSETS } = require('./process-box.lib.js');

const FILE = path.join(ASSETS, 'light/emitters.json');
const OWNERS = path.join(__dirname, 'emitters.owners.json');
const read = (f) => (fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, 'utf8')) : {});
const sorted = (o) => Object.fromEntries(Object.keys(o).sort().map((k) => [k, o[k]]));

// owner: the exporter's file name; entries: { 'ns:id' or 'ns:id[prop=value]': '#RRGGBB' }
function writeEmitters(owner, entries) {
  const all = read(FILE), owners = read(OWNERS);
  for (const k of owners[owner] || []) delete all[k];
  for (const [o, keys] of Object.entries(owners))
    if (o !== owner) for (const k of keys) if (k in entries) throw new Error(`${owner}: emitter ${k} is ${o}'s`);
  Object.assign(all, entries);
  owners[owner] = Object.keys(entries).sort();
  for (const [f, obj] of [[FILE, sorted(all)], [OWNERS, sorted(owners)]]) {
    fs.mkdirSync(path.dirname(f), { recursive: true });
    fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
  }
}
module.exports = { writeEmitters };
