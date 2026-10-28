// Writes the Clockwork Juggernaut's approved sounds into the mod: node design/sound/clockwork-juggernaut/export-game.js
// Every cue-file event becomes comedymonkeysaddons:clockwork_juggernaut.<id> with its subtitle, its takes copied to
// sounds/clockwork_juggernaut/, and the range the sound doc's loudness table gives it (attenuation_distance), so his
// big hits carry across an arena at volume 1. Nothing here is Minecraft's own audio.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons', NS = 'clockwork_juggernaut';
const here = __dirname;
const assets = path.resolve(here, '../../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, 'clockwork-juggernaut.cues.json'), 'utf8'));

// ranges in blocks, from the "Loudness and range" table in design/sound/clockwork-juggernaut.md (16 when not listed)
const RANGE = {
  48: ['stomp_impact', 'leap_land', 'mainspring_burst', 'ritual_unfold', 'ritual_ready'],
  32: ['tick', 'tock', 'run_down_tick', 'rewind_loop', 'rewind_end', 'charge_tell', 'vent_tell', 'vent_blast', 'leap_coil', 'leap_launch',
    'spin_windup', 'spin_loop', 'spin_down', 'stage_roar', 'plates_burst', 'grab_open', 'grab_snap', 'grab_clang', 'throw_whoosh',
    'swing_windup', 'swing_whoosh', 'stomp_raise', 'feint_snap', 'key_jam', 'glow_on', 'salute_snap', 'kneel_impact', 'death_steam',
    'ritual_collapse', 'step_run', 'key_ratchet', 'key_turn', 'glow_out', 'key_unwind', 'cog_throw'],
  24: ['hit_wound', 'hit_open', 'lightning', 'plate_ring', 'step', 'skid', 'foot_scrape', 'plates_settle', 'plates_land', 'joint_step',
    'cog_fly', 'cog_hit', 'cog_draw', 'steam_puff', 'fist_thud', 'grab_release'],
  8: ['engine_ratchet', 'engine_tick', 'engine_tock', 'engine_rune_up', 'engine_rune_down'],
};
const range = (id) => +(Object.entries(RANGE).find(([, ids]) => ids.includes(id)) || [16])[0];

const outDir = path.join(assets, 'sounds', NS);
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });
const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
for (const k of Object.keys(sounds)) if (k.startsWith(NS + '.')) delete sounds[k];
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.${NS}.`)) delete lang[k];

const takes = (id) => {
  const dir = path.join(here, 'real');
  const numbered = fs.readdirSync(dir).filter((f) => new RegExp('^' + id + '_\\d+\\.ogg$').test(f)).map((f) => f.slice(0, -4)).sort((a, b) => +a.split('_').pop() - +b.split('_').pop());
  return numbered.length ? numbered : fs.existsSync(path.join(dir, id + '.ogg')) ? [id] : [];
};
let copied = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  const t = takes(id);
  if (!t.length) throw new Error('no real sound for ' + id);
  for (const n of t) { fs.copyFileSync(path.join(here, 'real', n + '.ogg'), path.join(outDir, n + '.ogg')); copied++; }
  const subtitle = `subtitles.${MOD}.${NS}.${id}`;
  const r = range(id);
  sounds[NS + '.' + id] = { subtitle, sounds: t.map((n) => (r === 16 ? `${MOD}:${NS}/${n}` : { name: `${MOD}:${NS}/${n}`, attenuation_distance: r })) };
  lang[subtitle] = ev.subtitle;
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`${NS}: ${Object.keys(cues.events).length} sound events, ${copied} files copied`);
