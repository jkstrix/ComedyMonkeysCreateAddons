// Copies a block's approved real sounds into the mod and writes their sounds.json entries and subtitles.
// Usage: node design/sound/export-block.js <lava-generator|electric-motor|magma-engine|pressure-crucible>   (run that folder's make-loops.js first)
// Every cue-file event becomes comedymonkeysaddons:<namespace>.<id> with all its variants; loops use their seamless
// _loop files; the running sounds carry only 8 blocks (they run all day); an event marked "vanilla" points at
// Minecraft's own sound event instead of a file. Only <namespace>.* entries are touched in sounds.json and the lang file.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const BLOCKS = {
  'lava-generator': { loops: ['whir', 'burn'], quiet: ['whir', 'burn', 'cool_tick'] },
  'electric-motor': { loops: ['hum'], quiet: ['hum', 'spark'] },
  'magma-engine': { loops: ['firebox', 'blower'], quiet: ['chug', 'firebox', 'blower', 'cool_tick'] },
  // every press creaks and crunches, all day, so those carry only 8 blocks too
  'pressure-crucible': { loops: ['whirr', 'fire', 'fire_soul'], quiet: ['whirr', 'creak', 'crunch', 'fire', 'fire_soul'] },
};
const slug = process.argv[2];
const conf = BLOCKS[slug];
if (!conf) throw new Error('usage: node export-block.js <' + Object.keys(BLOCKS).join('|') + '>');
const here = path.join(__dirname, slug);
const assets = path.resolve(__dirname, '../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, slug + '.cues.json'), 'utf8'));
const ns = cues.namespace;

const outDir = path.join(assets, 'sounds', ns);
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });

const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
for (const k of Object.keys(sounds)) if (k.startsWith(ns + '.')) delete sounds[k];
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.${ns}.`)) delete lang[k];

let files = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  const subtitle = `subtitles.${MOD}.${ns}.${id}`;
  lang[subtitle] = ev.subtitle;
  if (ev.vanilla) {
    sounds[ns + '.' + id] = { subtitle, sounds: [{ name: ev.vanilla.event, type: 'event' }] };
    continue;
  }
  const n = (ev.real && ev.real.variants) || 1;
  const takes = conf.loops.includes(id) ? [id + '_loop'] : n > 1 ? Array.from({ length: n }, (_, i) => `${id}_${i + 1}`) : [id];
  for (const t of takes) {
    const src = path.join(here, 'real', t + '.ogg');
    if (!fs.existsSync(src)) throw new Error('missing real sound ' + src);
    fs.copyFileSync(src, path.join(outDir, t + '.ogg'));
    files++;
  }
  const name = (t) => `${MOD}:${ns}/${t}`;
  sounds[ns + '.' + id] = { subtitle, sounds: takes.map((t) => (conf.quiet.includes(id) ? { name: name(t), attenuation_distance: 8 } : name(t))) };
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`${slug}: exported ${Object.keys(cues.events).length} sound events, ${files} files`);
