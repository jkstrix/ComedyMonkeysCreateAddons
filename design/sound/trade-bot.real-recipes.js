// Real-sound recipes for Trade Bot: CC0 Kenney recordings for material and mechanical layers, synth for voice and
// springs. Run `node trade-bot.real-recipes.js` to write them into trade-bot.cues.json (as each event's `real`).
const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'trade-bot.cues.json');
const cues = JSON.parse(fs.readFileSync(file, 'utf8'));

const S = (src, o = {}) => ({ src, ...o });                 // one recording
const P = (pick, o = {}) => ({ pick, ...o });               // rotate through recordings across variants
const Y = (type, o = {}) => ({ type, ...o });               // synth layer
const range = (base, n, from = 0) => Array.from({ length: n }, (_, i) => base + String(from + i).padStart(3, '0'));
// the approved placeholder melody, sung by the richer voice synth
const voiceOf = (id, extra = {}) => cues.events[id].layers.filter((l) => l.type === 'chirp').map((l) => ({ ...l, type: 'voice', ...extra }));
const R = (layers, o = {}) => ({ variants: 3, level: 0.6, ...o, layers });

const softHits = range('impactSoft_medium_', 5), heavySoft = range('impactSoft_heavy_', 5), plateLight = range('impactPlate_light_', 5);
const metalLight = range('impactMetal_light_', 5), woodLight = range('impactWood_light_', 5), woodMed = range('impactWood_medium_', 5);
const tinyClicks = ['switch12', 'switch13', 'switch14'], chipsCollide = ['chips-collide-1', 'chips-collide-2', 'chips-collide-3', 'chips-collide-4'];
const glass = ['glass_002', 'glass_003', 'glass_005', 'glass_006'];
const tink = (at, gain = 0.1) => P(metalLight, { at, gain, pitch: 2.3, trim: [0, 0.2], fadeOut: 0.08, highpass: 1500 });

const real = {
  // signature
  clock_tick: R([P(['tick_001', 'tick_002', 'tick_004'], { gain: 0.9, highpass: 700 }), S('click_002', { at: 0.004, gain: 0.3 })], { variants: 4, level: 0.35, vary: { pitch: 0.03 } }),
  focus_click: R([P(tinyClicks, { pitch: 1.3, gain: 0.8 }), Y('servo', { at: 0.01, f: 900, f2: 1300, dur: 0.08, gain: 0.25 }), S('tick_004', { at: 0.08, gain: 0.5, pitch: 1.2 })], { level: 0.45 }),
  brass_ping: R([S('impactBell_heavy_003', { pitch: 1.6, gain: 0.9, lowpass: 9000 }), S('impactMetal_light_002', { pitch: 1.8, gain: 0.3, at: 0.01 }), Y('ping', { f: 1320, dur: 0.8, gain: 0.3 })], { variants: 2, level: 0.7, vary: { pitch: 0.015 } }),
  // warm, soft winding: ticks pitched down and rolled off above ~3 kHz (the bright version was piercing when repeated)
  key_rattle: R([...[0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6].map((at, i) => S(i % 2 ? 'tick_002' : 'tick_004', { at, gain: 0.8, pitch: 0.6 + (i % 3) * 0.04, lowpass: 2200 })), S('metalLatch', { at: 0.7, gain: 0.3, pitch: 1.0, trim: [0, 0.2], fadeOut: 0.05, lowpass: 3000 })], { level: 0.28, post: { lowpass: 2400, highpass: 120 } }),
  merchant_trill: R([...voiceOf('merchant_trill', { gain: 0.8 }), S('pluck_001', { at: 0.22, pitch: 1.5, gain: 0.3 }), Y('ping', { at: 0.22, f: 2640, dur: 0.4, gain: 0.12 })], { level: 0.6, vary: { pitch: 0.01 } }),

  // movement
  // natural rubber on a surface: a soft rolling rumble whose tread thump speeds up as he sets off, a quiet motor under it
  roll_start: R([Y('roll', { f: 380, rate: 2, rate2: 6, dur: 0.75, fadeIn: 0.3, fadeOut: 0.25, crunch: 0.3, crunchLevel: 0.25, gain: 0.8 }), Y('servo', { f: 70, f2: 120, dur: 0.5, gain: 0.15 }), P(softHits, { gain: 0.15, pitch: 0.8, lowpass: 800 })], { level: 0.36, vary: { pitch: 0.05 } }),
  // braking: the rumble slows, the rubber scrubs (grip-slip) and he settles with a soft thump
  roll_stop: R([Y('roll', { f: 360, rate: 6, rate2: 1.5, dur: 0.45, fadeIn: 0.02, fadeOut: 0.3, crunch: 0.25, crunchLevel: 0.25, gain: 0.7 }), Y('scrub', { at: 0.03, f: 650, rate: 70, dur: 0.24, gain: 0.55 }), P(softHits, { at: 0.26, gain: 0.45, pitch: 0.9, lowpass: 1500 })], { level: 0.42 }),
  // the cruising rumble: a seamless 3 s loop (crossfaded seam), steady tread rate, quieter than the start so a long roll isn't tiring
  roll_loop: R([Y('roll', { f: 380, rate: 6, dur: 3.4, fadeIn: 0.01, fadeOut: 0.01, depth: 0.3, crunch: 0.3, crunchLevel: 0.25, gain: 0.8 })], { variants: 2, level: 0.28, loop: 3.0 }),
  servo_short: R([Y('servo', { f: 160, f2: 220, dur: 0.18, gain: 0.7 }), P(['click5', 'click4', 'click2'], { at: 0.16, gain: 0.35 })], { variants: 4, level: 0.35, vary: { pitch: 0.08 } }),
  servo_snap: R([Y('servo', { f: 220, f2: 340, dur: 0.08, gain: 0.7 }), P(tinyClicks, { at: 0.07, gain: 0.6 })], { level: 0.4, vary: { pitch: 0.06 } }),
  // heavier: a weighty push-off thud, a lower and slower spring
  hop_launch: R([P(heavySoft, { gain: 0.6, pitch: 0.85, trim: [0, 0.25], fadeOut: 0.1 }), Y('thud', { f: 60, dur: 0.18, gain: 0.5 }), Y('servo', { f: 240, f2: 120, dur: 0.12, gain: 0.3 }), Y('boing', { at: 0.12, f: 190, dur: 0.5, wobble: 0.35, gain: 0.55 }), ...voiceOf('hop_launch', { gain: 0.22 })], { level: 0.7, post: { lowpass: 7000 } }),
  hop_launch_big: R([P(heavySoft, { gain: 0.6, pitch: 0.75, trim: [0, 0.3], fadeOut: 0.12 }), Y('thud', { f: 52, dur: 0.22, gain: 0.6 }), Y('servo', { f: 260, f2: 100, dur: 0.22, gain: 0.35 }), ...voiceOf('hop_launch_big', { gain: 0.3 }), Y('boing', { at: 0.22, f: 150, dur: 0.65, wobble: 0.4, gain: 0.65 })], { level: 0.8, post: { lowpass: 6500 } }),
  hop_small: R([Y('boing', { f: 340, dur: 0.25, wobble: 0.3, gain: 0.5 }), P(softHits, { at: 0.2, gain: 0.5, pitch: 1.2 })], { level: 0.45 }),
  // heavier: lower rubber thump, a deep body thud, a metal body clank, a slow spring, then the faint tink
  land_soft: R([P(heavySoft, { gain: 0.9, pitch: 0.9, trim: [0, 0.35], fadeOut: 0.12 }), Y('thud', { f: 58, dur: 0.22, gain: 0.7 }), P(range('impactPlate_heavy_', 5), { at: 0.015, gain: 0.35, pitch: 0.9, trim: [0, 0.3], fadeOut: 0.1 }), Y('boing', { at: 0.03, f: 200, dur: 0.35, wobble: 0.3, gain: 0.2 }), tink(0.18, 0.07)], { variants: 4, level: 0.85, post: { lowpass: 7000 } }),
  land_heavy: R([P(range('impactPunch_heavy_', 5), { gain: 0.9, pitch: 0.8, lowpass: 2500 }), Y('thud', { f: 45, dur: 0.3, gain: 0.9 }), P(range('impactPlate_heavy_', 5), { at: 0.01, gain: 0.55, pitch: 0.8 }), P(range('impactMetal_heavy_', 5), { at: 0.02, gain: 0.3, pitch: 0.75 }), P(['dice-throw-2', 'die-throw-1', 'die-throw-4'], { at: 0.04, gain: 0.3, pitch: 0.85, trim: [0, 0.35], fadeOut: 0.12 }), Y('boing', { at: 0.05, f: 160, dur: 0.55, wobble: 0.4, gain: 0.25 }), tink(0.34, 0.06)], { level: 1.0, post: { lowpass: 6000 } }),
  drop_whoosh: R([Y('whoosh', { f: 250, f2: 900, dur: 0.28, gain: 0.55 }), ...voiceOf('drop_whoosh', { gain: 0.25, at: 0.02 })], { level: 0.45 }),
  aerial_boing: R([Y('boing', { f: 520, dur: 0.45, wobble: 0.5, gain: 0.5 }), Y('boing', { at: 0.28, f: 700, dur: 0.22, wobble: 0.3, gain: 0.2 }), tink(0.5, 0.08)], { level: 0.5 }),
  blink: R([P(['click_003', 'click_004', 'click_005'], { gain: 0.8 }), S('toggle_004', { at: 0.12, pitch: 1.5, gain: 0.4 })], { variants: 2, level: 0.25 }),

  // voice and reactions
  bonk: R([S('impactBell_heavy_002', { pitch: 0.95, gain: 0.8 }), P(['impactMetal_heavy_001', 'impactMetal_heavy_003'], { gain: 0.7 }), Y('boing', { at: 0.02, f: 180, dur: 0.6, wobble: 0.5, gain: 0.45 }), S('impactPunch_medium_000', { gain: 0.5 })], { variants: 2, level: 1.0 }),
  dizzy: R([...voiceOf('dizzy', { gain: 0.8 }), S('scroll_002', { at: 0.05, pitch: 1.6, gain: 0.35, trim: [0, 0.8], fadeOut: 0.2 })], { variants: 2, level: 0.55 }),
  head_shake: R([0, 0.14, 0.28].flatMap((at, i) => [Y('servo', { at, f: 250, f2: 330, dur: 0.07, gain: 0.5 - i * 0.1 }), S(tinyClicks[i], { at: at + 0.06, gain: 0.4 - i * 0.08 })]), { variants: 2, level: 0.45 }),
  grumble: R([...voiceOf('grumble', { gain: 0.9 }), P(softHits, { pitch: 0.7, gain: 0.2 })], { level: 0.5 }),
  double_take: R([S('switch14', { gain: 0.6 }), S('metalLatch', { at: 0.02, pitch: 1.6, gain: 0.3, trim: [0, 0.15] }), Y('boing', { at: 0.01, f: 400, dur: 0.35, wobble: 0.5, gain: 0.5 }), ...voiceOf('double_take', { gain: 0.6 })], { variants: 2, level: 0.7 }),
  wave_clink: R([P(metalLight, { pitch: 1.7, gain: 0.6, trim: [0, 0.15], fadeOut: 0.06, lowpass: 7000 }), Y('ping', { f: 2200, dur: 0.18, gain: 0.25 }), Y('servo', { f: 180, f2: 240, dur: 0.12, gain: 0.15 })], { variants: 4, level: 0.38, vary: { pitch: 0.08 } }),
  curious_chirp: R([...voiceOf('curious_chirp', { gain: 0.8 }), S('tick_004', { at: 0.2, gain: 0.4 })], { level: 0.5 }),
  sad_aww: R([...voiceOf('sad_aww', { gain: 0.8 }), S('creak3', { at: 0.05, pitch: 1.5, gain: 0.25, lowpass: 3000 })], { variants: 2, level: 0.5 }),
  sigh: R([Y('servo', { f: 220, f2: 110, dur: 0.9, gain: 0.3 }), S('cloth3', { pitch: 0.8, lowpass: 2500, gain: 0.45 })], { variants: 2, level: 0.45 }),
  snore: R([...voiceOf('snore', { gain: 0.7, tine: false }), S('cloth4', { pitch: 0.6, lowpass: 800, gain: 0.3 })], { level: 0.3 }),
  startled_chirp: R([...voiceOf('startled_chirp', { gain: 0.8 }), Y('boing', { at: 0.01, f: 450, dur: 0.25, wobble: 0.4, gain: 0.3 }), S('switch14', { gain: 0.4 })], { level: 0.6 }),
  puzzled_hmm: R([S('tick_004', { gain: 0.4 }), ...voiceOf('puzzled_hmm', { gain: 0.8 })], { level: 0.5 }),
  huh: R([...voiceOf('huh', { gain: 0.8 })], { level: 0.5 }),
  shrug: R([P(metalLight, { pitch: 1.5, gain: 0.5, trim: [0, 0.15], fadeOut: 0.06, lowpass: 7000 }), P(metalLight, { at: 0.05, pitch: 1.7, gain: 0.4, trim: [0, 0.12], fadeOut: 0.05, lowpass: 7000 }), ...voiceOf('shrug', { gain: 0.5 })], { variants: 2, level: 0.42 }),
  pleased_boop: R([...voiceOf('pleased_boop', { gain: 0.8 }), P(['pluck_001', 'pluck_002'], { pitch: 1.8, gain: 0.2 })], { level: 0.5 }),
  hi_boop: R([...voiceOf('hi_boop', { gain: 0.8 }), S('pluck_002', { pitch: 1.6, gain: 0.2 })], { level: 0.5 }),
  bye_boop: R([...voiceOf('bye_boop', { gain: 0.8 }), S('pluck_001', { pitch: 1.4, gain: 0.2 })], { level: 0.5 }),
  found_it: R([...voiceOf('found_it', { gain: 0.8 }), S('impactBell_heavy_004', { at: 0.03, pitch: 1.7, gain: 0.5 }), P(glass, { at: 0.2, pitch: 1.3, gain: 0.35 }), P(glass, { at: 0.27, pitch: 1.6, gain: 0.25 })], { variants: 2, level: 0.75 }),
  haggle_chatter: R([...voiceOf('haggle_chatter', { gain: 0.8 }), S('tick_004', { at: 0.2, gain: 0.3 })], { level: 0.55 }),
  plead_chirp: R([...voiceOf('plead_chirp', { gain: 0.8 })], { variants: 2, level: 0.5 }),

  // lights and station
  light_off: R([P(['switch2', 'switch3', 'switch8'], { gain: 0.8, trim: [0, 0.15], fadeOut: 0.04 }), ...voiceOf('light_off', { gain: 0.25, tine: false })], { variants: 2, level: 0.4 }),
  light_on: R([P(['switch5', 'switch6', 'switch9'], { gain: 0.8, trim: [0, 0.15], fadeOut: 0.04 }), ...voiceOf('light_on', { gain: 0.35, tine: false })], { variants: 2, level: 0.4 }),
  flicker: R([0, 0.05, 0.09].map((at, i) => S(tinyClicks[i], { at, pitch: 1.4, gain: 0.7 - i * 0.15 })), { variants: 2, level: 0.35 }),
  sleepy_mmm: R([...voiceOf('sleepy_mmm', { gain: 0.7, tine: false }), S('cloth4', { pitch: 0.7, gain: 0.2, lowpass: 1500 })], { level: 0.3 }),
  glow_on: R([Y('swell', { f: 180, f2: 260, dur: 0.8, gain: 0.45 }), Y('hum', { at: 0.5, f: 110, dur: 1.2, gain: 0.25 }), S('glass_004', { pitch: 0.5, lowpass: 2000, gain: 0.2, fadeIn: 0.1 })], { variants: 1, level: 0.55 }),
  glow_off: R([Y('swell', { f: 260, f2: 150, dur: 0.5, gain: 0.35 })], { variants: 1, level: 0.4 }),
  winder_engage: R([S('metalLatch', { gain: 0.8 }), S('metalClick', { at: 0.06, gain: 0.6, trim: [0, 0.3], fadeOut: 0.1 }), P(range('impactMetal_medium_', 5), { at: 0.12, pitch: 1.1, gain: 0.35 })], { variants: 2, level: 0.42, post: { lowpass: 3200 } }),
  winder_release: R([S('metalClick', { pitch: 1.2, gain: 0.7, trim: [0, 0.25], fadeOut: 0.08 }), P(range('impactTin_medium_', 5), { at: 0.04, gain: 0.3 }), Y('servo', { at: 0.04, f: 200, f2: 150, dur: 0.2, gain: 0.2 })], { variants: 2, level: 0.5 }),
  flag_ding: R([S('impactBell_heavy_003', { pitch: 1.5, gain: 0.9 }), Y('boing', { at: 0.02, f: 600, dur: 0.5, wobble: 0.4, gain: 0.25 })], { variants: 1, level: 0.8 }),

  // items and containers
  drawer_open: R([Y('servo', { f: 90, f2: 120, dur: 0.2, gain: 0.3 }), S('handleCoins2', { gain: 0.3, trim: [0, 0.2], fadeOut: 0.08, lowpass: 5000 }), P(woodLight, { at: 0.2, pitch: 1.3, gain: 0.45 })], { level: 0.5 }),
  drawer_close: R([P(woodMed, { pitch: 1.3, gain: 0.8 }), S('handleCoins2', { at: 0.02, gain: 0.25, trim: [0, 0.15], fadeOut: 0.06, lowpass: 5000 })], { level: 0.65 }),
  chest_open: R([S('doorOpen_1', { pitch: 1.2, gain: 0.8, trim: [0, 0.6], fadeOut: 0.15 }), P(woodLight, { at: 0.45, gain: 0.35 })], { variants: 2, level: 0.6 }),
  chest_close: R([P(['doorClose_1', 'doorClose_2', 'doorClose_3'], { gain: 0.6, trim: [0, 0.45], fadeOut: 0.1 }), P(range('impactWood_heavy_', 5), { gain: 0.7 })], { variants: 2, level: 0.8 }),
  barrel_open: R([P(woodLight, { pitch: 0.9, gain: 0.7 }), S('creak3', { at: 0.02, gain: 0.4 })], { variants: 2, level: 0.6 }),
  barrel_close: R([P(woodMed, { gain: 0.8 }), P(range('impactWood_heavy_', 5), { at: 0.02, gain: 0.4 })], { variants: 2, level: 0.7 }),
  rummage: R([S('cloth1', { gain: 0.6, lowpass: 3000 }), S('cloth2', { at: 0.6, gain: 0.5, lowpass: 3000 }), S('cloth3', { at: 1.1, gain: 0.5, lowpass: 3000 }), S('handleCoins', { gain: 0.25, lowpass: 5000 }), S('bookFlip1', { at: 0.9, gain: 0.3, lowpass: 4000 }), P(woodLight, { at: 1.3, gain: 0.35 })], { variants: 2, level: 0.45 }),
  toss_whoosh: R([Y('whoosh', { f: 300, f2: 1100, dur: 0.3, gain: 0.6 })], { level: 0.45 }),
  cobble_clack: R([P(range('impactMining_', 5), { gain: 0.8, trim: [0, 0.35], fadeOut: 0.1 }), P(['die-throw-2', 'dice-throw-2'], { at: 0.1, gain: 0.4 })], { variants: 2, level: 0.7 }),
  set_on_metal: R([P(['bookPlace1', 'bookPlace2', 'bookPlace3'], { pitch: 0.9, gain: 0.7, lowpass: 4000 }), P(plateLight, { pitch: 1.2, gain: 0.3, trim: [0, 0.25], fadeOut: 0.08, lowpass: 6000 })], { level: 0.55 }),
  set_soft: R([P(['bookPlace1', 'bookPlace2', 'bookPlace3'], { gain: 0.7, lowpass: 4000 }), P(softHits, { gain: 0.4 })], { level: 0.5 }),
  pat: R([0, 0.2, 0.4].map((at, i) => P(softHits, { at, gain: 0.7 - i * 0.15, pitch: 1.2 })), { level: 0.45 }),
  dust_off: R([S('cloth2', { lowpass: 2500, gain: 0.5 }), P(metalLight, { at: 0.2, pitch: 1.8, gain: 0.15, trim: [0, 0.1], fadeOut: 0.04 }), S('cloth3', { at: 0.4, lowpass: 2500, gain: 0.4 })], { variants: 2, level: 0.32 }),
  push_scrape: R([Y('scrape', { f: 500, dur: 0.5, gain: 0.5 }), P(woodLight, { pitch: 0.8, gain: 0.25, lowpass: 2500 })], { level: 0.35, post: { lowpass: 1800 } }),
  grip: R([P(range('impactTin_medium_', 5), { pitch: 1.3, gain: 0.6, trim: [0, 0.15], fadeOut: 0.05, lowpass: 6000 }), Y('ping', { f: 2200, dur: 0.18, gain: 0.25 })], { level: 0.42 }),
  claw_fidget: R([0, 0.15, 0.3].map((at, i) => S(tinyClicks[i], { at, gain: 0.7 - i * 0.15 })), { variants: 2, level: 0.35 }),
  poof_in: R([Y('pop', { f: 700, gain: 0.7 }), P(glass, { at: 0.03, pitch: 1.5, gain: 0.4 }), P(glass, { at: 0.08, pitch: 1.8, gain: 0.25 })], { level: 0.65 }),
  poof_out: R([Y('swell', { f: 400, f2: 900, dur: 0.12, gain: 0.3 }), Y('pop', { at: 0.12, f: 900, gain: 0.6 }), P(glass, { at: 0.14, pitch: 1.7, gain: 0.35 })], { level: 0.6 }),
  emerald_catch: R([S('handleCoins2', { gain: 0.45, trim: [0, 0.3], fadeOut: 0.1, lowpass: 6000 }), S('impactBell_heavy_004', { at: 0.02, pitch: 1.7, gain: 0.4 }), P(range('impactGlass_light_', 5), { at: 0.01, pitch: 1.3, gain: 0.4, lowpass: 7000 }), Y('ping', { at: 0.05, f: 2900, dur: 0.3, gain: 0.25 })], { level: 0.75 }),
  spin_whir: R([Y('servo', { f: 120, f2: 320, dur: 0.45, gain: 0.4 }), Y('whoosh', { at: 0.03, f: 280, f2: 1000, dur: 0.45, gain: 0.45 })], { variants: 2, level: 0.5 }),

  // villager stand-ins: in game these are vanilla villager sounds; kept as voice sketches for the preview
  villager_no: R([...voiceOf('villager_no', { gain: 0.8, tine: false }), Y('noise', { f: 500, dur: 0.32, gain: 0.08 })], { variants: 1, level: 0.5 }),
  villager_yes: R([...voiceOf('villager_yes', { gain: 0.8, tine: false }), Y('noise', { f: 500, dur: 0.2, gain: 0.08 })], { variants: 1, level: 0.5 }),
  villager_trade: R([...voiceOf('villager_trade', { gain: 0.8, tine: false }), Y('noise', { f: 500, dur: 0.26, gain: 0.08 })], { variants: 1, level: 0.5 }),

  // scared of hostile mobs
  scared_yelp: R([...voiceOf('scared_yelp', { gain: 0.8 }), Y('boing', { at: 0.02, f: 450, dur: 0.3, wobble: 0.5, gain: 0.3 }), S('switch14', { gain: 0.45 }), S('metalLatch', { at: 0.01, pitch: 1.6, gain: 0.2, trim: [0, 0.12], fadeOut: 0.04 })], { level: 0.6 }),
  panic_chatter: R([...voiceOf('panic_chatter', { gain: 0.8 }), S('tick_004', { at: 0.15, gain: 0.25 })], { variants: 4, level: 0.5 }),
  roll_fast_loop: R([Y('roll', { f: 520, rate: 12, dur: 1.9, fadeIn: 0.01, fadeOut: 0.01, depth: 0.3, crunch: 0.3, crunchLevel: 0.25, gain: 0.8 })], { variants: 2, level: 0.32, loop: 1.5 }),
  hard_brake: R([Y('roll', { f: 420, rate: 12, rate2: 2, dur: 0.5, fadeIn: 0.01, fadeOut: 0.3, crunch: 0.25, crunchLevel: 0.25, gain: 0.7 }), Y('scrub', { at: 0.02, f: 700, rate: 80, dur: 0.35, gain: 0.6 }), P(softHits, { at: 0.3, gain: 0.5, pitch: 0.9, lowpass: 1500 })], { level: 0.5 }),
  hide_tuck: R([Y('servo', { f: 220, f2: 110, dur: 0.25, gain: 0.45 }), P(metalLight, { at: 0.2, pitch: 1.8, gain: 0.35, trim: [0, 0.12], fadeOut: 0.05, lowpass: 7000 }), P(metalLight, { at: 0.24, pitch: 2.0, gain: 0.25, trim: [0, 0.1], fadeOut: 0.04, lowpass: 7000 }), P(softHits, { at: 0.3, gain: 0.45, pitch: 1.1 })], { variants: 2, level: 0.45 }),
  // rusty metal plates chattering together: ~11 short, damped knocks a second (plate / tin / metal, uneven timing and pitch,
  // cut short so they knock rather than ring), a gritty mid edge, and a faint corroded grind underneath; a seamless 2 s loop
  tremble_loop: R([
    ...(() => { let x = 12345; const r = () => ((x = (Math.imul(x, 1664525) + 1013904223) >>> 0) / 4294967296); const out = [];
      const pool = [...plateLight, ...range('impactTin_medium_', 5), ...metalLight];
      for (let k = 0; k < 26; k++) out.push(S(pool[Math.floor(r() * pool.length)], { at: k / 11 + (r() - 0.5) * 0.03, pitch: 0.8 + r() * 0.45, gain: 0.35 + r() * 0.45, trim: [0, 0.07], decay: 0.025, fadeOut: 0.02, lowpass: 4500, peak: { f: 1200, gain: 4, q: 1.2 } }));
      return out; })(),
    Y('scrub', { f: 900, rate: 45, dur: 2.4, gain: 0.18 }),
  ], { variants: 2, level: 0.22, loop: 2.0, vary: { pitch: 0.03, gain: 0.05, at: 0.002 }, post: { lowpass: 5000 } }),
  whimper: R([...voiceOf('whimper', { gain: 0.8, tine: false })], { variants: 3, level: 0.5 }),
  peek_click: R([S('tick_004', { gain: 0.45 }), ...voiceOf('peek_click', { gain: 0.35, tine: false })], { variants: 2, level: 0.3 }),
  relief_sigh: R([Y('servo', { f: 220, f2: 100, dur: 1.0, gain: 0.3 }), S('cloth3', { pitch: 0.8, lowpass: 2500, gain: 0.4 }), ...voiceOf('relief_sigh', { gain: 0.6, tine: false })], { variants: 2, level: 0.45 }),
  shake_off: R([P(['dice-shake-2', 'dice-shake-3'], { gain: 0.6, trim: [0, 0.4], fadeOut: 0.12, lowpass: 4000 }), P(metalLight, { at: 0.05, pitch: 1.6, gain: 0.3, trim: [0, 0.15], fadeOut: 0.05, lowpass: 7000 }), Y('boing', { at: 0.1, f: 500, dur: 0.35, wobble: 0.4, gain: 0.3 })], { variants: 3, level: 0.5 }),
};

// the voice is dense (high RMS), so voice-led events sit ~30% lower in peak than impacts, to sound small and close
for (const id of ['merchant_trill', 'curious_chirp', 'sad_aww', 'grumble', 'huh', 'pleased_boop', 'hi_boop', 'bye_boop', 'plead_chirp', 'haggle_chatter', 'dizzy', 'snore', 'sleepy_mmm', 'puzzled_hmm', 'villager_no', 'villager_yes', 'villager_trade', 'panic_chatter', 'whimper', 'relief_sigh']) real[id].level *= 0.7;

// softer, more elongated robot noises (user: "especially during expressions"; loudness unchanged, set by `level`):
// voice notes stretched (expression sounds most, quick reactions least), servo whirs rounder and a little longer
const STRETCH = {
  curious_chirp: 1.9, sad_aww: 1.9, grumble: 1.9, huh: 1.9, puzzled_hmm: 1.9, plead_chirp: 1.9, pleased_boop: 1.9, hi_boop: 1.9, bye_boop: 1.9, sleepy_mmm: 1.9, snore: 1.9,
  merchant_trill: 1.6, dizzy: 1.6, found_it: 1.5, haggle_chatter: 1.4, shrug: 1.6,
  startled_chirp: 1.3, double_take: 1.3, hop_launch: 1.2, hop_launch_big: 1.2, drop_whoosh: 1.2, light_on: 1.2, light_off: 1.2,
  villager_no: 1, villager_yes: 1, villager_trade: 1,
  scared_yelp: 1.0, panic_chatter: 1.0, whimper: 1.4, peek_click: 1.2, relief_sigh: 1.6,
};
for (const [id, r] of Object.entries(real)) {
  const k = STRETCH[id] || 1.5;
  for (const l of r.layers) {
    if (l.type === 'voice') l.stretch = k;
    if (l.type === 'servo') { l.soft = true; l.stretch = 1.3; }
  }
}

// gentle 9 kHz roll-off on every sound that doesn't set its own, to take pack hiss off the top
for (const r of Object.values(real)) r.post = { lowpass: 9000, ...(r.post || {}) };

const missing = Object.keys(cues.events).filter((id) => !real[id]);
if (missing.length) throw new Error('no real recipe for: ' + missing.join(', '));
for (const [id, r] of Object.entries(real)) { if (!cues.events[id]) throw new Error('unknown event ' + id); cues.events[id].real = r; }
const ev = Object.entries(cues.events).map(([k, v]) => '    ' + JSON.stringify(k) + ': ' + JSON.stringify(v)).join(',\n');
const cl = Object.entries(cues.clips).map(([k, v]) => '    ' + JSON.stringify(k) + ': ' + JSON.stringify(v)).join(',\n');
fs.writeFileSync(file, '{\n  "namespace": ' + JSON.stringify(cues.namespace) + ',\n  "events": {\n' + ev + '\n  },\n  "clips": {\n' + cl + '\n  }\n}\n');
console.log('wrote real recipes for ' + Object.keys(real).length + ' events');
