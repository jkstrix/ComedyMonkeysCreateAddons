// Copies the Geothermal Turbine's approved real sounds into the mod and writes their sounds.json entries and subtitles.
// Usage: node design/sound/geothermal-turbine/export-game.js   (run make-loops.js first)
// Every cue-file event becomes comedymonkeysaddons:geothermal_turbine.<id> with all its variants; the two loops use
// their seamless _loop files. Only geothermal_turbine.* entries are touched in the shared sounds.json and lang file.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const here = __dirname;
const assets = path.resolve(here, '../../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, 'geothermal-turbine.cues.json'), 'utf8'));
const LOOPS = new Set(['turbine_whine', 'firebox_roar']);

const outDir = path.join(assets, 'sounds/geothermal_turbine');
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });

const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
for (const k of Object.keys(sounds)) if (k.startsWith('geothermal_turbine.')) delete sounds[k];
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.geothermal_turbine.`)) delete lang[k];

let files = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  const n = (ev.real && ev.real.variants) || 1;
  const takes = LOOPS.has(id) ? [id + '_loop'] : n > 1 ? Array.from({ length: n }, (_, i) => `${id}_${i + 1}`) : [id];
  for (const t of takes) {
    const src = path.join(here, 'real', t + '.ogg');
    if (!fs.existsSync(src)) throw new Error('missing real sound ' + src);
    fs.copyFileSync(src, path.join(outDir, t + '.ogg'));
    files++;
  }
  const subtitle = `subtitles.${MOD}.geothermal_turbine.${id}`;
  sounds['geothermal_turbine.' + id] = { subtitle, sounds: takes.map((t) => `${MOD}:geothermal_turbine/${t}`) };
  lang[subtitle] = ev.subtitle;
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`exported ${Object.keys(cues.events).length} sound events, ${files} files`);
