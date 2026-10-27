// For events marked `vanilla` in a cue file: replace the placeholder sketch with Minecraft's own sound, taken from the
// local asset cache, so the board plays what the game will play. Preview only: the files land in the board's
// placeholder folder and design/sound/sources/vanilla-preview/, both git-ignored, and are never shipped.
// Usage: node vanilla-preview.js <cues.json> <outDir> [wav|ogg]
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const [cuesPath, outDir, ext = 'wav'] = process.argv.slice(2);
const cues = JSON.parse(fs.readFileSync(cuesPath, 'utf8'));
const ASSETS = path.join(os.homedir(), '.gradle/caches/neoformruntime/assets');
const index = JSON.parse(fs.readFileSync(path.join(ASSETS, 'indexes/17.json'), 'utf8')).objects;
const cache = path.resolve(__dirname, '../sources/vanilla-preview');
fs.mkdirSync(cache, { recursive: true });
for (const [id, ev] of Object.entries(cues.events)) {
  if (!ev.vanilla) continue;
  const key = `minecraft/sounds/${ev.vanilla.file}.ogg`;
  const obj = index[key];
  if (!obj) throw new Error('no vanilla sound ' + key);
  const src = path.join(ASSETS, 'objects', obj.hash.slice(0, 2), obj.hash);
  const local = path.join(cache, ev.vanilla.file.replace(/\//g, '_') + '.ogg');
  fs.copyFileSync(src, local);
  const v = ev.vanilla, af = [];
  if (v.pitch) af.push(`asetrate=44100*${v.pitch}`, 'aresample=44100');
  if (v.lowpass) af.push(`lowpass=f=${v.lowpass}`);
  af.push(`volume=${v.gain || 1}`);
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-i', local, '-ac', '1', '-ar', '44100', '-af', af.join(','), path.join(outDir, id + '.' + ext)]);
  console.log('vanilla', id, '<-', v.file);
}
