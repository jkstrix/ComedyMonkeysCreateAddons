// Writes the Monster Cage and Fish Tank cue files (stage 1: placeholders).
// Events marked `vanilla` are Minecraft's own sounds: the game plays the vanilla event (referenced from sounds.json or
// played directly), so nothing is shipped for them. On the board they're auditioned from the local asset cache
// (vanilla-preview.js), never committed.
// Clip times come from design/models/monster-cage.gen.js and fish-tank.gen.js.
const fs = require('fs');
const path = require('path');

const cage = {
  namespace: 'monster_cage',
  events: {
    // ---- signature sounds ----
    monster_grumble: { group: 'Signature sounds', subtitle: 'Caged monster grumbles',
      vanilla: { file: 'mob/zombie/say1', event: "the stored monster's own ambient sound (a zombie here)", gain: 0.35 },
      note: "In game: the stored monster's own ambient event at volume 0.35 (user: the stored mob, but quieter so it's not annoying), pitch 0.95-1.05, at most once every 10-30 s per cage, and cages within 8 blocks share one cooldown so a wall of cages never talks over itself.",
      layers: [{ type: 'hum', at: 0, gain: 0.2, f: 110, dur: 0.8 }] },
    bars_rattle: { group: 'Signature sounds', subtitle: 'Cage bars rattle',
      layers: [{ type: 'clang', at: 0, gain: 0.32, f: 420, dur: 0.5 }, { type: 'ratchet', at: 0.02, gain: 0.14, rate: 16, dur: 0.32 }, { type: 'clink', at: 0.06, gain: 0.12, f: 900, dur: 0.3 }] },
    chow_pour: { group: 'Signature sounds', subtitle: 'Monster Chow rattles into the bowl',
      layers: [{ type: 'rustle', at: 0, gain: 0.22, density: 30, dur: 0.8 }, { type: 'ping', at: 0.04, gain: 0.08, f: 700, dur: 0.6 },
        { type: 'clink', at: 0.12, gain: 0.1, f: 1300, dur: 0.15 }, { type: 'clink', at: 0.3, gain: 0.09, f: 1500, dur: 0.15 }, { type: 'clink', at: 0.52, gain: 0.07, f: 1200, dur: 0.15 }] },
    arrival: { group: 'Signature sounds', subtitle: 'A new monster joins the horde',
      layers: [{ type: 'rustle', at: 0, gain: 0.2, density: 14, dur: 0.5 }, { type: 'thud', at: 0.3, gain: 0.25, f: 120, dur: 0.3 }, { type: 'clang', at: 0.34, gain: 0.18, f: 380, dur: 0.4 }] },
    // ---- using the cage ----
    jar_in: { group: 'Using the cage', subtitle: 'Monster jar locked into a cage',
      layers: [{ type: 'click', at: 0, gain: 0.25, dur: 0.05 }, { type: 'clang', at: 0.1, gain: 0.25, f: 520, dur: 0.35 }, { type: 'ping', at: 0.14, gain: 0.08, f: 1100, dur: 0.3 }] },
    jar_out: { group: 'Using the cage', subtitle: 'Monster jar unlocked',
      layers: [{ type: 'click', at: 0, gain: 0.25, dur: 0.05 }, { type: 'clink', at: 0.08, gain: 0.18, f: 800, dur: 0.25 }, { type: 'whoosh', at: 0.12, gain: 0.08, f: 500, f2: 900, dur: 0.25 }] },
    cull: { group: 'Using the cage', subtitle: 'Horde culled',
      vanilla: { file: 'entity/player/attack/sweep1', event: 'entity.player.attack.sweep (as the pens use)', gain: 0.8 },
      note: 'In game: the vanilla sweep, as the Animal Pens play it, plus bars_rattle.',
      layers: [{ type: 'whoosh', at: 0, gain: 0.2, f: 400, f2: 1600, dur: 0.25 }] },
    drop_tray: { group: 'Using the cage', subtitle: 'Drops land in the tray',
      layers: [{ type: 'clink', at: 0, gain: 0.18, f: 1500, dur: 0.18 }, { type: 'thud', at: 0.01, gain: 0.12, f: 150, dur: 0.12 }] },
    // ---- flooded cages ----
    bubbles_loop: { group: 'Flooded cages', subtitle: 'Water bubbles',
      layers: [{ type: 'rustle', at: 0, gain: 0.06, density: 10, dur: 3 }, ...[0.15, 0.6, 0.9, 1.4, 1.75, 2.3, 2.6].map((at, i) => ({ type: 'pop', at, gain: 0.08 + (i % 3) * 0.02, f: 500 + (i * 137) % 400, dur: 0.08 }))] },
    hatch_open: { group: 'Flooded cages', subtitle: 'Hatch flaps open',
      layers: [{ type: 'creak', at: 0, gain: 0.14, f: 320, dur: 0.18 }, { type: 'click', at: 0.02, gain: 0.15, dur: 0.04 }] },
    trident_throw: { group: 'Flooded cages', subtitle: 'Drowned throws a trident',
      vanilla: { file: 'item/trident/throw1', event: 'item.trident.throw (the projectile plays it)', gain: 0.9 },
      layers: [{ type: 'whoosh', at: 0, gain: 0.25, f: 300, f2: 1400, dur: 0.3 }] },
    hatch_shut: { group: 'Flooded cages', subtitle: 'Hatch clanks shut',
      layers: [{ type: 'clang', at: 0, gain: 0.28, f: 600, dur: 0.3 }, { type: 'boing', at: 0.02, gain: 0.07, f: 240, dur: 0.3 }] },
    drowning: { group: 'Flooded cages', subtitle: 'Zombie turns into a drowned',
      vanilla: { file: 'mob/drowned/convert1', event: 'entity.zombie.converted_to_drowned', gain: 0.8 },
      layers: [{ type: 'noise', at: 0, gain: 0.15, f: 800, dur: 1 }] },
    // ---- netherite containment ----
    field_hum: { group: 'Netherite containment', subtitle: 'Containment field hums',
      note: 'Running loop: the staging bed (-36 LUFS), 8 blocks.',
      layers: [{ type: 'hum', at: 0, gain: 0.22, f: 60, dur: 2 }, { type: 'hum', at: 0, gain: 0.08, f: 120, dur: 2 }, { type: 'servo', at: 0, gain: 0.025, f: 900, f2: 900, dur: 2, soft: true }] },
    field_up: { group: 'Netherite containment', subtitle: 'Containment field rises',
      layers: [{ type: 'swell', at: 0, gain: 0.25, f: 80, f2: 240, dur: 1.2 }, { type: 'click', at: 0, gain: 0.15, dur: 0.05 }, { type: 'sparkle', at: 0.9, gain: 0.05, f: 2400, dur: 0.3 }] },
    field_flicker: { group: 'Netherite containment', subtitle: 'Containment field sputters',
      layers: [{ type: 'noise', at: 0, gain: 0.18, f: 4200, dur: 0.07 }, { type: 'click', at: 0.01, gain: 0.12, dur: 0.04 }, { type: 'hum', at: 0.03, gain: 0.08, f: 55, dur: 0.15 }] },
    field_cutout: { group: 'Netherite containment', subtitle: 'Containment field cuts out',
      note: "In game: the field_hum loop's volume follows the shield's flicker (cut to 5% while it's out, a short surge as it returns); this file previews that, timed to the containment clip.",
      layers: [{ type: 'hum', at: 0.15, gain: 0.2, f: 60, dur: 0.15 }, { type: 'hum', at: 0.7, gain: 0.2, f: 60, dur: 0.1 }, { type: 'hum', at: 1.1, gain: 0.2, f: 60, dur: 0.25 }, { type: 'hum', at: 1.4, gain: 0.2, f: 60, dur: 0.5 }, { type: 'hum', at: 2.0, gain: 0.2, f: 60, dur: 0.25 }, { type: 'hum', at: 2.6, gain: 0.2, f: 60, dur: 0.1 }] },
    field_down: { group: 'Netherite containment', subtitle: 'Containment field fails',
      layers: [{ type: 'click', at: 0, gain: 0.2, dur: 0.05 }, { type: 'servo', at: 0.02, gain: 0.18, f: 420, f2: 50, dur: 1.4 }, { type: 'noise', at: 0, gain: 0.1, f: 3000, dur: 0.12 }] },
    klaxon: { group: 'Netherite containment', subtitle: 'Containment klaxon',
      note: 'Loud on purpose: an alarm, heard across a base (about B + 12, 32 blocks).',
      layers: [{ type: 'chirp', at: 0, gain: 0.35, notes: [[300, 520, 0.45], [520, 380, 0.35]] }, { type: 'noise', at: 0, gain: 0.04, f: 2500, dur: 0.8 }] },
    breakout: { group: 'Netherite containment', subtitle: 'Wither breaks out',
      vanilla: { file: 'mob/wither/spawn', event: 'entity.wither.spawn (plays when it breaks free)', gain: 0.9 },
      note: 'In game: the vanilla Wither spawn, plus a cage-burst clang of our own.',
      layers: [{ type: 'clang', at: 0, gain: 0.45, f: 180, dur: 0.8 }, { type: 'noise', at: 0, gain: 0.2, f: 3000, dur: 0.3 }] },
    // ---- items ----
    jar_catch: { group: 'Jars and Monster Chow', subtitle: 'Monster caught in a jar',
      layers: [{ type: 'whoosh', at: 0, gain: 0.15, f: 900, f2: 300, dur: 0.25 }, { type: 'click', at: 0.22, gain: 0.25, dur: 0.05 }, { type: 'clink', at: 0.26, gain: 0.15, f: 1400, dur: 0.2 }] },
    chow_eat: { group: 'Jars and Monster Chow', subtitle: 'Eating Monster Chow',
      vanilla: { file: 'random/eat1', event: 'entity.generic.eat (vanilla eating)', gain: 0.8 },
      layers: [{ type: 'rustle', at: 0, gain: 0.2, density: 20, dur: 0.3 }] },
    monster_belch: { group: 'Jars and Monster Chow', subtitle: 'Player belches like a monster',
      vanilla: { file: 'mob/creeper/say1', event: "a random monster's ambient sound, from the player (a creeper hiss here)", gain: 0.8 },
      layers: [{ type: 'hum', at: 0, gain: 0.2, f: 90, dur: 0.5 }] },
    chow_breath: { group: 'Jars and Monster Chow', subtitle: 'Chow Breath',
      layers: [{ type: 'whoosh', at: 0, gain: 0.16, f: 700, f2: 250, dur: 0.5 }, { type: 'pop', at: 0.42, gain: 0.08, f: 300, dur: 0.08 }] },
  },
  clips: {
    // life (8 s): grumbles, drops into the tray at 2/4/6, a monster reaching the bars at 4.8
    life: [[0, 'field_hum'], [0, 'bubbles_loop'], [1.2, 'monster_grumble'], [2, 'drop_tray'], [2, 'field_hum'], [3, 'bubbles_loop'], [4, 'drop_tray'], [4, 'field_hum'],
      [4.8, 'bars_rattle'], [5.6, 'monster_grumble'], [6, 'drop_tray'], [6, 'field_hum'], [6, 'bubbles_loop']],
    // trident_hatch (2 s): flap opens 0.3-0.5, trident out at 0.42, falls shut and bounces at 1.15
    trident_hatch: [[0.3, 'hatch_open'], [0.42, 'trident_throw'], [1.15, 'hatch_shut']],
    // containment (7 s): field up 0-2, short of FE 2-5 (flickers at each drop-out, klaxon every 1.6 s), down at 5
    containment: [[0, 'field_hum'], [2, 'field_cutout'], [2, 'klaxon'], [2, 'field_flicker'], [2.3, 'field_flicker'], [2.8, 'field_flicker'], [3.35, 'field_flicker'], [3.6, 'klaxon'],
      [3.9, 'field_flicker'], [4.25, 'field_flicker'], [4.7, 'field_flicker'], [5, 'field_down'], [5.2, 'klaxon']],
  },
};

const tank = {
  namespace: 'fish_tank',
  events: {
    pump_loop: { group: 'Signature sounds', subtitle: 'Aquarium pump hums',
      note: 'Running loop: the staging bed (-36 LUFS), 8 blocks.',
      layers: [{ type: 'hum', at: 0, gain: 0.14, f: 100, dur: 2 }, { type: 'servo', at: 0, gain: 0.05, f: 220, f2: 220, dur: 2, soft: true }, { type: 'thud', at: 0, gain: 0.05, f: 90, dur: 0.15 }, { type: 'thud', at: 1, gain: 0.05, f: 90, dur: 0.15 }] },
    bubbles_loop: { group: 'Signature sounds', subtitle: 'Bubbles rise',
      layers: [{ type: 'rustle', at: 0, gain: 0.05, density: 10, dur: 3 }, ...[0.1, 0.35, 0.7, 1.0, 1.3, 1.55, 1.9, 2.2, 2.5, 2.75].map((at, i) => ({ type: 'pop', at, gain: 0.07 + (i % 3) * 0.02, f: 600 + (i * 151) % 500, dur: 0.07 }))] },
    feeder_sprinkle: { group: 'Signature sounds', subtitle: 'Fish Food sprinkles in',
      layers: [{ type: 'click', at: 0, gain: 0.15, dur: 0.04 }, { type: 'rustle', at: 0.05, gain: 0.1, density: 28, dur: 0.5 }, { type: 'pop', at: 0.25, gain: 0.05, f: 900, dur: 0.05 }, { type: 'pop', at: 0.4, gain: 0.04, f: 1100, dur: 0.05 }] },
    fish_birth: { group: 'Life in the tank', subtitle: 'New fish',
      vanilla: { file: 'block/bubble_column/bubble1', event: 'block.bubble_column.bubble_pop', gain: 0.7 },
      layers: [{ type: 'pop', at: 0, gain: 0.15, f: 700, dur: 0.1 }] },
    net_drop: { group: 'Life in the tank', subtitle: 'Catch drops into the net',
      vanilla: { file: 'entity/fish/swim1', event: 'entity.fish.swim (a small splash)', gain: 0.5 },
      layers: [{ type: 'pop', at: 0, gain: 0.12, f: 380, dur: 0.12 }, { type: 'noise', at: 0, gain: 0.06, f: 1800, dur: 0.15 }] },
    scoop: { group: 'Life in the tank', subtitle: 'Fish scooped up',
      vanilla: { file: 'item/bucket/fill_fish1', event: 'item.bucket.fill_fish', gain: 0.8 },
      layers: [{ type: 'whoosh', at: 0, gain: 0.15, f: 400, f2: 900, dur: 0.3 }] },
    lid_feed: { group: 'Life in the tank', subtitle: 'Feeder hatch',
      layers: [{ type: 'clack', at: 0, gain: 0.15, f: 600, dur: 0.1 }, { type: 'click', at: 0.12, gain: 0.1, dur: 0.04 }] },
  },
  clips: {
    // life (8 s): pump strokes every 2 s, food sprinkles at 0 and 4, catches into the net at 2/4/6
    life: [[0, 'pump_loop'], [0, 'bubbles_loop'], [0, 'feeder_sprinkle'], [2, 'pump_loop'], [2, 'net_drop'], [3, 'bubbles_loop'], [4, 'pump_loop'], [4, 'feeder_sprinkle'],
      [4, 'net_drop'], [6, 'pump_loop'], [6, 'net_drop'], [6, 'bubbles_loop']],
    pump: [[0, 'pump_loop']],
  },
};

// ---------- stage 2: real recipes (the real-first ladder) ----------
// Recordings: BigSoundBank (CC0) segments cut by cuts.js (names below), and the project's Kenney packs (CC0).
// `target` is the in-game loudness from design/sound/staging.md (B = -36 LUFS): loops as integrated LUFS, one-shots as
// max momentary. levels.js measures the renders and writes levels.json, which sets each `level`.
const K = (base, n = 5) => [...Array(n).keys()].map((i) => `${base}_00${i}`);
cage.events.cage_burst = { group: 'Netherite containment', subtitle: 'Cage bursts open', layers: [{ type: 'clang', at: 0, gain: 0.45, f: 180, dur: 0.8 }, { type: 'noise', at: 0, gain: 0.2, f: 3000, dur: 0.3 }] };
const REAL_CAGE = {
  bars_rattle: { target: -33, variants: 3, vary: { pitch: 0.05 }, post: { lowpass: 9000 }, layers: [{ pick: ['bars_gate', 'bars_stick', 'bars_iron_clang'], gain: 1 }] },
  chow_pour: { target: -32, variants: 3, vary: { pitch: 0.04 }, post: { lowpass: 10000 }, layers: [
    { pick: ['kibble_rice_pan', 'kibble_cereal_a', 'kibble_cereal_b'], gain: 1 },
    { pick: K('impactMetal_light'), at: 0.02, gain: 0.22, pitch: 0.8, fadeOut: 0.3 }] },
  arrival: { target: -34, variants: 3, vary: { pitch: 0.05 }, post: { lowpass: 9000 }, layers: [
    { pick: ['crunch_bones', 'crunch_rusk', 'crunch_crisps'], gain: 0.9 },
    { pick: ['bars_iron_clang', 'bars_gate'], at: 0.3, gain: 0.45, trim: [0, 0.5], fadeOut: 0.2 }] },
  jar_in: { target: -28, variants: 2, layers: [{ pick: ['lock_barn_lock', 'lock_big_key'], gain: 1 }, { pick: ['glass_002', 'glass_003'], at: 0.14, gain: 0.3 }] },
  jar_out: { target: -28, variants: 2, layers: [{ pick: ['lock_barn_unlock', 'strike_clunk'], gain: 1 }, { pick: ['glass_005', 'glass_006'], at: 0.1, gain: 0.25 }] },
  drop_tray: { target: -36, variants: 4, vary: { pitch: 0.06 }, layers: [{ pick: K('impactTin_medium'), gain: 1, pitch: 1.1 }] },
  bubbles_loop: { target: -39, variants: 2, layers: [{ pick: ['loop_airlock_a', 'loop_airlock_b'], gain: 1 }] },
  hatch_open: { target: -30, variants: 1, layers: [{ src: 'hatch_mailbox_open', gain: 1 }] },
  hatch_shut: { target: -30, variants: 2, vary: { pitch: 0.05 }, layers: [{ src: 'hatch_mailbox_shut', gain: 1 }] },
  // the field's voice: the user's own Stable Audio 3 take (rung 4, chosen by the user over the transformer and neon recordings), raw and cleaned
  field_hum: { target: -36, variants: 2, layers: [{ pick: ['sa3_field_hum_loop_raw', 'sa3_field_hum_loop_clean'], gain: 1 }] },
  field_cutout: { target: -34, variants: 2, layers: [{ pick: ['sa3_field_hum_cutout_raw', 'sa3_field_hum_cutout_clean'], gain: 1 }] },
  field_up: { target: -26, variants: 1, layers: [{ src: 'switch_click', gain: 0.8 }, { src: 'sa3_field_hum_v2_clean', at: 0.05, gain: 1, trim: [0.5, 1.9], fadeIn: 1.0, fadeOut: 0.25 }, { src: 'spark_weld', at: 0.9, gain: 0.35 }] },
  field_flicker: { target: -32, variants: 4, vary: { pitch: 0.06 }, layers: [{ pick: ['spark_shock_a', 'spark_shock_b', 'spark_shock_c', 'flicker_bulb_a'], gain: 1 }] },
  field_down: { target: -26, variants: 1, layers: [{ src: 'strike_clunk', gain: 0.9 }, { src: 'spark_weld', at: 0.03, gain: 0.5 }, { src: 'sa3_field_hum_v2_clean', at: 0.05, gain: 0.9, pitch: 0.85, trim: [0.5, 2.0], fadeOut: 1.3 }] },
  klaxon: { target: -24, variants: 3, layers: [{ pick: ['klaxon_oogah_a', 'klaxon_oogah_b', 'klaxon_oogah_c'], gain: 1 }] },
  cage_burst: { target: -24, variants: 2, layers: [{ src: 'strike_clunk', gain: 0.8 }, { pick: ['crash_steel_a', 'crash_steel_b'], at: 0.04, gain: 1 }] },
  jar_catch: { target: -28, variants: 2, layers: [{ pick: ['puff_blow_a', 'puff_blow_b'], gain: 0.5, trim: [0, 0.25], fadeOut: 0.1, lowpass: 3000 }, { src: 'lock_barn_lock', at: 0.2, gain: 1 }, { pick: ['glass_002', 'glass_004'], at: 0.26, gain: 0.25 }] },
  chow_breath: { target: -30, variants: 2, layers: [{ pick: ['puff_blow_a', 'puff_blow_b'], gain: 1, lowpass: 3000, fadeOut: 0.2 }] },
};
const REAL_TANK = {
  // 10 dB under the running bed (user, 2026-10-27: "make the pump and buzzing noise lower, like by half at least")
  pump_loop: { target: -46, variants: 2, post: { lowpass: 5000 }, layers: [{ pick: ['loop_air_compressor', 'loop_electric_pump'], gain: 1, pitch: 1.15 }] },
  bubbles_loop: { target: -39, variants: 2, layers: [{ pick: ['loop_airlock_a', 'loop_airlock_b'], gain: 1 }] },
  feeder_sprinkle: { target: -32, variants: 2, vary: { pitch: 0.05 }, layers: [{ pick: ['lid_pot_a', 'lid_pot_b'], gain: 0.8 }, { src: 'flakes_cereal', at: 0.08, gain: 0.6, lowpass: 5000 }] },
  lid_feed: { target: -32, variants: 2, vary: { pitch: 0.05 }, layers: [{ pick: ['lid_pot_a', 'lid_pot_b'], gain: 1 }] },
};
let LEVELS = {};
try { LEVELS = JSON.parse(fs.readFileSync(path.join(__dirname, 'levels.json'), 'utf8')); } catch (e) { /* first build: levels.js writes it */ }
for (const [cues, real] of [[cage, REAL_CAGE], [tank, REAL_TANK]])
  for (const [id, r] of Object.entries(real)) cues.events[id].real = { level: LEVELS[`${cues.namespace}.${id}`] ?? 0.5, ...r };

const here = __dirname;
fs.writeFileSync(path.join(here, 'monster-cage.cues.json'), JSON.stringify(cage, null, 2));
const tankDir = path.resolve(here, '../fish-tank');
fs.mkdirSync(tankDir, { recursive: true });
fs.writeFileSync(path.join(tankDir, 'fish-tank.cues.json'), JSON.stringify(tank, null, 2));
console.log('cues written:', Object.keys(cage.events).length, '+', Object.keys(tank.events).length, 'events');
