// What a visitor keeps between visits: favourites, recently viewed, entries seen, progression goals ticked.
// All in localStorage, which can be missing or full (private windows): then it just isn't remembered.

const KEY = 'codex.v1';
const listeners = new Set();

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) ?? {};
  } catch {
    return {};
  }
}

const state = Object.assign({ favourites: [], recent: [], seen: [], goals: [] }, load());

function save() {
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch { /* not remembered this time */ }
  listeners.forEach(fn => fn(state));
}

/** Called with the whole state whenever it changes. Returns an unsubscribe function. */
export function onChange(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

export const isFavourite = id => state.favourites.includes(id);
export const favourites = () => [...state.favourites];

export function toggleFavourite(id) {
  const i = state.favourites.indexOf(id);
  if (i >= 0) state.favourites.splice(i, 1); else state.favourites.unshift(id);
  save();
  return i < 0;
}

/** Most recent first, at most twelve. */
export const recent = () => [...state.recent];

export function visit(id) {
  state.recent = [id, ...state.recent.filter(r => r !== id)].slice(0, 12);
  if (!state.seen.includes(id)) state.seen.push(id);
  save();
}

export const seenCount = ids => ids.filter(id => state.seen.includes(id)).length;

export const isGoalDone = key => state.goals.includes(key);
export function setGoal(key, done) {
  state.goals = state.goals.filter(g => g !== key);
  if (done) state.goals.push(key);
  save();
}
