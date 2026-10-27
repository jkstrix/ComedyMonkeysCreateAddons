// Copies the Monster Cage's and the Fish Tank's approved real sounds into the mod, and writes their sounds.json entries
// and subtitles. Usage: node design/sound/monster-cage/export-game.js
// Events marked "vanilla" aren't exported: the code plays Minecraft's own sound (the monster's own ambient, the sweep,
// the trident, the Wither, the bucket...). field_cutout is a preview only: in game the field_hum loop's volume follows
// the shield. Running sounds and frequent pulses carry 8 blocks (they run all day); the rest the default 16.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const assets = path.resolve(__dirname, '../../../src/main/resources/assets', MOD);
const SETS = [
  { dir: path.join(__dirname), cues: 'monster-cage.cues.json', skip: ['field_cutout'],
    quiet: ['field_hum', 'bubbles_loop', 'drop_tray', 'arrival', 'chow_pour', 'bars_rattle', 'field_flicker'] },
  { dir: path.resolve(__dirname, '../fish-tank'), cues: 'fish-tank.cues.json', skip: [],
    quiet: ['pump_loop', 'bubbles_loop', 'feeder_sprinkle', 'lid_feed'] },
];

const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
let files = 0;
for (const set of SETS) {
  const cues = JSON.parse(fs.readFileSync(path.join(set.dir, set.cues), 'utf8'));
  const ns = cues.namespace;
  const outDir = path.join(assets, 'sounds', ns);
  fs.rmSync(outDir, { recursive: true, force: true });
  fs.mkdirSync(outDir, { recursive: true });
  for (const k of Object.keys(sounds)) if (k.startsWith(ns + '.')) delete sounds[k];
  for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.${ns}.`)) delete lang[k];
  for (const [id, ev] of Object.entries(cues.events)) {
    if (ev.vanilla || !ev.real || set.skip.includes(id))
      continue;
    const n = ev.real.variants || 1;
    const takes = n > 1 ? Array.from({ length: n }, (_, i) => `${id}_${i + 1}`) : [id];
    const entries = [];
    for (const t of takes) {
      const src = path.join(set.dir, 'real', t + '.ogg');
      if (!fs.existsSync(src)) throw new Error('missing real sound ' + src);
      fs.copyFileSync(src, path.join(outDir, t + '.ogg'));
      files++;
      const name = `${MOD}:${ns}/${t}`;
      entries.push(set.quiet.includes(id) ? { name, attenuation_distance: 8 } : name);
    }
    const subtitle = `subtitles.${MOD}.${ns}.${id}`;
    sounds[`${ns}.${id}`] = { subtitle, sounds: entries };
    lang[subtitle] = ev.subtitle;
  }
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log('exported', files, 'files');
