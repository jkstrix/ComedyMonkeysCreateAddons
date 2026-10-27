// Writes helper-bot.cues.json: Helper Bot's sound events and every clip's cues (times from
// design/models/helper-bot.motion.js). Most events reuse Trade Bot's approved real sounds (`reuse: 'trade_bot'`,
// same id and files, Helper Bot subtitles); only the whistle and the lever are new.
const fs = require('fs');
const path = require('path');

const R = (subtitle, group) => ({ subtitle, group, reuse: 'trade_bot', layers: [{ type: "Trade Bot's approved sound" }] });
const events = {
  // ---- new ----
  whistle_toot: { subtitle: 'Helper Bot toots his whistle', group: 'Signature sounds (new)', layers: [
    { type: 'noise', at: 0, gain: 0.25, f: 3000, dur: 0.06 },
    { type: 'chirp', at: 0.02, gain: 0.55, notes: [[1180, 1240, 0.2]] },
    { type: 'chirp', at: 0.02, gain: 0.3, notes: [[1480, 1550, 0.2]] },
    { type: 'noise', at: 0.05, gain: 0.12, f: 6000, dur: 0.18 },
  ] },
  whistle_peep: { subtitle: 'Helper Bot peeps his whistle', group: 'Signature sounds (new)', layers: [
    { type: 'noise', at: 0, gain: 0.2, f: 3000, dur: 0.04 },
    { type: 'chirp', at: 0.01, gain: 0.5, notes: [[1240, 1300, 0.11]] },
    { type: 'noise', at: 0.03, gain: 0.1, f: 6000, dur: 0.09 },
  ] },
  lever_clunk: { subtitle: 'Helper Bot pulls a lever', group: 'Signature sounds (new)', layers: [
    { type: 'click', at: 0, gain: 0.6, dur: 0.03 },
    { type: 'clack', at: 0.015, gain: 0.7, f: 420, dur: 0.12 },
    { type: 'thud', at: 0.02, gain: 0.35, f: 110, dur: 0.15 },
  ] },
  // ---- reused from Trade Bot ----
  clock_tick: R('Helper Bot\'s watch ticks', 'Signature sounds (Trade Bot\'s)'),
  focus_click: R('Helper Bot focuses', 'Signature sounds (Trade Bot\'s)'),
  merchant_trill: R('Helper Bot trills', 'Signature sounds (Trade Bot\'s)'),
  brass_ping: R('Helper Bot dings', 'Signature sounds (Trade Bot\'s)'),
  servo_short: R('Helper Bot whirs', 'Movement'),
  servo_snap: R('Helper Bot whirs', 'Movement'),
  aerial_boing: R('Helper Bot wobbles on his spring', 'Movement'),
  hop_small: R('Helper Bot bounces', 'Movement'),
  land_soft: R('Helper Bot settles', 'Movement'),
  land_heavy: R('Helper Bot is jolted', 'Movement'),
  blink: R('Helper Bot blinks', 'Movement'),
  grip: R('Helper Bot grips', 'Hands, watch and cap'),
  claw_fidget: R('Helper Bot fidgets', 'Hands, watch and cap'),
  pat: R('Helper Bot taps', 'Hands, watch and cap'),
  set_soft: R('Helper Bot clips the schedule in', 'Hands, watch and cap'),
  dust_off: R('Helper Bot wipes', 'Hands, watch and cap'),
  drop_whoosh: R('Helper Bot blows', 'Hands, watch and cap'),
  light_off: R('Helper Bot\'s watch snaps shut', 'Hands, watch and cap'),
  light_on: R('Helper Bot\'s eyes come on', 'Hands, watch and cap'),
  flicker: R('Helper Bot\'s eyes flicker', 'Hands, watch and cap'),
  poof_in: R('Helper Bot\'s cap pops on', 'Hands, watch and cap'),
  poof_out: R('Helper Bot\'s cap pops off', 'Hands, watch and cap'),
  wave_clink: R('Helper Bot waves', 'Hands, watch and cap'),
  bonk: R('Helper Bot bonks', 'Voice and reactions'),
  dizzy: R('Helper Bot is dizzy', 'Voice and reactions'),
  head_shake: R('Helper Bot shakes his head', 'Voice and reactions'),
  grumble: R('Helper Bot huffs', 'Voice and reactions'),
  double_take: R('Helper Bot does a double-take', 'Voice and reactions'),
  curious_chirp: R('Helper Bot wonders', 'Voice and reactions'),
  puzzled_hmm: R('Helper Bot is puzzled', 'Voice and reactions'),
  huh: R('Helper Bot is baffled', 'Voice and reactions'),
  shrug: R('Helper Bot shrugs', 'Voice and reactions'),
  startled_chirp: R('Helper Bot startles', 'Voice and reactions'),
  sad_aww: R('Helper Bot droops', 'Voice and reactions'),
  sigh: R('Helper Bot sighs', 'Voice and reactions'),
  snore: R('Helper Bot snores', 'Voice and reactions'),
  sleepy_mmm: R('Helper Bot gets sleepy', 'Voice and reactions'),
  pleased_boop: R('Helper Bot boops', 'Voice and reactions'),
  hi_boop: R('Helper Bot says hi', 'Voice and reactions'),
  bye_boop: R('Helper Bot says bye', 'Voice and reactions'),
  scared_yelp: R('Helper Bot yelps', 'Scared'),
  hide_tuck: R('Helper Bot hides', 'Scared'),
  tremble_loop: R('Helper Bot trembles', 'Scared'),
  whimper: R('Helper Bot whimpers', 'Scared'),
  peek_click: R('Helper Bot peeks', 'Scared'),
  relief_sigh: R('Helper Bot sighs with relief', 'Scared'),
  shake_off: R('Helper Bot shakes it off', 'Scared'),
};

// cue times in seconds, from the keys in helper-bot.motion.js
const clips = {
  idle: [[2.2, 'blink']],
  drive_ready: [[0.75, 'focus_click'], [2.3, 'blink']],
  cruise: [[0.3, 'servo_short'], [1.2, 'blink'], [1.6, 'servo_short']],
  throttle_up: [[0.08, 'servo_snap'], [0.4, 'aerial_boing'], [0.6, 'lever_clunk'], [0.95, 'pleased_boop'], [1.4, 'grip']],
  brake: [[0.2, 'aerial_boing'], [0.35, 'grip'], [0.6, 'lever_clunk'], [0.65, 'grumble']],
  station_wait: [[0.35, 'grip'], [1.2, 'focus_click'], [1.4, 'clock_tick'], [1.9, 'clock_tick'], [2.05, 'pat'], [2.4, 'clock_tick'], [3.0, 'clock_tick'], [3.5, 'clock_tick'], [3.95, 'grumble'], [4.05, 'light_off'], [4.4, 'grip'], [4.95, 'pat'], [5.2, 'pat'], [5.45, 'pat']],
  depart: [[0.15, 'servo_snap'], [0.5, 'grip'], [0.62, 'whistle_toot'], [0.97, 'whistle_toot'], [1.45, 'wave_clink'], [1.55, 'bye_boop']],
  honk: [[0.3, 'grip'], [0.42, 'whistle_peep']],
  no_path: [[0.4, 'grip'], [0.8, 'focus_click'], [1.3, 'claw_fidget'], [1.45, 'curious_chirp'], [2.0, 'claw_fidget'], [2.2, 'huh'], [2.6, 'servo_short'], [2.95, 'servo_short'], [3.3, 'shrug'], [3.75, 'set_soft']],
  manual_folded: [[1.45, 'grumble']],
  paused_point: [[0.3, 'pat'], [0.6, 'pat'], [1.0, 'servo_short'], [1.8, 'focus_click']],
  crash: [[0.12, 'bonk'], [0.14, 'land_heavy'], [0.25, 'aerial_boing'], [0.5, 'dizzy'], [2.0, 'head_shake'], [2.55, 'sad_aww']],
  sulk: [[1.65, 'sigh']],
  no_schedule: [[0.2, 'servo_short'], [0.7, 'servo_short'], [1.25, 'curious_chirp'], [2.15, 'pat']],
  schedule_given: [[0.2, 'set_soft'], [0.22, 'servo_snap'], [0.3, 'aerial_boing'], [0.45, 'poof_in'], [0.85, 'wave_clink'], [0.9, 'merchant_trill'], [1.85, 'grip']],
  schedule_taken: [[0.2, 'grip'], [0.35, 'servo_short'], [0.55, 'sad_aww'], [1.3, 'poof_out']],
  passenger: [[1.3, 'blink'], [2.4, 'wave_clink']],
  shop_idle: [[1.7, 'blink']],
  shop_greet: [[0.2, 'focus_click'], [0.55, 'wave_clink'], [0.6, 'hi_boop'], [1.2, 'wave_clink']],
  shop_order_nod: [[0.15, 'servo_short'], [0.2, 'brass_ping'], [0.45, 'servo_short'], [0.95, 'pleased_boop']],
  check_watch: [[0.3, 'grip'], [0.85, 'focus_click'], [1.0, 'clock_tick'], [1.5, 'clock_tick'], [1.7, 'light_off'], [1.95, 'grip']],
  polish_lens: [[0.65, 'dust_off'], [1.25, 'dust_off'], [2.0, 'blink'], [2.25, 'blink']],
  toot_tune: [[0.5, 'grip'], [0.72, 'whistle_peep'], [1.17, 'whistle_peep'], [1.62, 'whistle_peep'], [2.2, 'pleased_boop']],
  adjust_cap: [[0.5, 'grip'], [0.75, 'claw_fidget'], [1.45, 'grip']],
  dust_controls: [[0.35, 'dust_off'], [0.95, 'dust_off'], [1.62, 'drop_whoosh'], [2.0, 'pleased_boop']],
  doze_off: [[0.4, 'sleepy_mmm'], [1.2, 'servo_short'], [2.0, 'servo_short'], [2.2, 'flicker'], [2.36, 'light_off'], [2.6, 'snore'], [3.6, 'snore'], [4.5, 'startled_chirp'], [4.5, 'aerial_boing'], [4.52, 'light_on'], [4.9, 'servo_snap'], [5.6, 'servo_snap'], [6.1, 'huh']],
  sees_player: [[0.2, 'servo_snap'], [0.45, 'servo_snap'], [0.75, 'double_take'], [1.0, 'servo_short'], [1.35, 'wave_clink'], [1.45, 'hi_boop']],
  wave: [[0.3, 'wave_clink'], [0.35, 'hi_boop']],
  hostile_shrink: [[0.1, 'scared_yelp'], [0.2, 'aerial_boing'], [0.45, 'hide_tuck']],
  hostile_cower: [[0, 'tremble_loop'], [0.85, 'peek_click'], [1.5, 'whimper']],
  hostile_recover: [[0.3, 'servo_short'], [0.6, 'servo_short'], [0.95, 'relief_sigh'], [1.4, 'shake_off']],
  rain_wipe: [[0.5, 'dust_off'], [1.2, 'dust_off'], [1.7, 'head_shake']],
  thunder_flinch: [[0.05, 'startled_chirp'], [0.08, 'hop_small'], [0.55, 'servo_short']],
  idle_ducked: [[1.1, 'servo_short'], [2.5, 'blink']],
  duck_down: [[0.05, 'servo_short'], [0.35, 'land_soft']],
  duck_up: [[0.1, 'servo_short'], [0.45, 'aerial_boing']],
  expr_happy: [[0.2, 'merchant_trill'], [0.4, 'hop_small'], [0.68, 'hop_small'], [1.0, 'aerial_boing']],
  expr_proud: [[0.25, 'servo_snap'], [0.35, 'merchant_trill'], [1.2, 'claw_fidget']],
  expr_focused: [[0.2, 'focus_click'], [2.3, 'focus_click']],
  expr_curious: [[0.25, 'curious_chirp'], [0.9, 'focus_click'], [1.5, 'focus_click'], [2.1, 'focus_click']],
  expr_startled: [[0.15, 'startled_chirp'], [0.2, 'aerial_boing']],
  expr_sad: [[0.3, 'sad_aww'], [1.1, 'sigh']],
  expr_cross: [[0.2, 'focus_click'], [1.0, 'grumble'], [1.1, 'head_shake']],
  expr_confused: [[0.3, 'puzzled_hmm'], [1.3, 'huh']],
  expr_dizzy: [[0.25, 'dizzy'], [2.8, 'head_shake']],
};
// ---- real recipes for the new events (stage 2), with candidates to pick from by ear ----
const HISS = { src: '0227', trim: [0.05, 0.3], highpass: 2500, lowpass: 9000, gain: 0.16, fadeIn: 0.01, fadeOut: 0.15 };
const WHISTLES = {
  a: { name: 'small whistle (BigSoundBank 3319, up 1.6x)', src: '3319', trim: [0.07, 0.52], pitch: 1.6 },
  b: { name: 'small whistle (BigSoundBank 3317, up 1.5x)', src: '3317', trim: [0.08, 0.53], pitch: 1.5 },
  c: { name: 'low toy whistle (BigSoundBank 3315, up 3x)', src: '3315', trim: [0.4, 1.3], pitch: 3.0 },
  d: { name: 'real locomotive (BigSoundBank 0225, up 2.2x)', src: '0225', trim: [0.04, 0.7], pitch: 2.2 },
};
const toot = (w) => ({ variants: 3, level: 0.5, vary: { pitch: 0.03, gain: 0.08, at: 0.004 }, post: { lowpass: 9000 }, layers: [
  { src: w.src, trim: w.trim, pitch: w.pitch, gain: 0.9, fadeIn: 0.005, fadeOut: 0.09 },
  { ...HISS, at: 0 },
] });
const peep = (w) => ({ variants: 4, level: 0.42, vary: { pitch: 0.06, gain: 0.08, at: 0.003 }, post: { lowpass: 9000 }, layers: [
  { src: w.src, trim: [w.trim[0], w.trim[0] + 0.14 * w.pitch], pitch: w.pitch, gain: 0.9, fadeIn: 0.004, fadeOut: 0.05 },
  { ...HISS, trim: [0.05, 0.16], gain: 0.12, fadeOut: 0.08 },
] });
const LEVERS = {
  a: { name: 'vanilla lever click (block.lever.click)', real: { variants: 1, level: 0.5, layers: [{ src: 'random_click', gain: 1 }] } },
  b: { name: 'vanilla copper trapdoor (block.copper_trapdoor.open)', real: { variants: 2, level: 0.5, vary: { pitch: 0.03 }, layers: [{ pick: ['block_copper_trapdoor_toggle1', 'block_copper_trapdoor_toggle2'], gain: 1, pitch: 1.1, fadeOut: 0.05 }] } },
  c: { name: 'latch + soft thud (Kenney metalLatch, impactSoft_heavy_000)', real: { variants: 3, level: 0.5, vary: { pitch: 0.04 }, post: { lowpass: 9000 }, layers: [{ src: 'metalLatch', gain: 0.9, trim: [0, 0.25], fadeOut: 0.06 }, { src: 'impactSoft_heavy_000', at: 0.01, gain: 0.45, pitch: 1.2, trim: [0, 0.2], fadeOut: 0.08 }] } },
};
const PICK = { whistle: 'd', lever: 'b' }; // the user's picks (2026-10-27): "whistle d, lever b"
const CANDIDATES = process.argv.includes('--candidates'); // put the candidates back on the board
if (CANDIDATES) for (const [k, w] of Object.entries(WHISTLES)) events['whistle_toot_' + k] = { subtitle: 'Candidate ' + k.toUpperCase() + ': ' + w.name, group: 'Pick one: whistle', layers: events.whistle_toot.layers, real: toot(w) };
if (CANDIDATES) for (const [k, l] of Object.entries(LEVERS)) events['lever_clunk_' + k] = { subtitle: 'Candidate ' + k.toUpperCase() + ': ' + l.name, group: 'Pick one: lever', layers: events.lever_clunk.layers, real: l.real };
events.whistle_toot.real = toot(WHISTLES[PICK.whistle]);
events.whistle_peep.real = peep(WHISTLES[PICK.whistle]);
events.lever_clunk.real = LEVERS[PICK.lever].real;

for (const [clip, list] of Object.entries(clips)) for (const [, id] of list) if (!events[id]) throw new Error(clip + ': no event ' + id);
fs.writeFileSync(path.join(__dirname, 'helper-bot.cues.json'), JSON.stringify({ namespace: 'helper_bot', events, clips }, null, 1));
// the new events only, for the placeholder synth
const fresh = Object.fromEntries(Object.entries(events).filter(([, e]) => !e.reuse));
fs.writeFileSync(path.join(__dirname, 'new-events.cues.json'), JSON.stringify({ namespace: 'helper_bot', events: fresh, clips: {} }, null, 1));
console.log(Object.keys(events).length + ' events (' + Object.keys(fresh).length + ' new), ' + Object.keys(clips).length + ' clips, ' + Object.values(clips).reduce((a, l) => a + l.length, 0) + ' cues');
