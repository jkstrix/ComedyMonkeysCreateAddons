// Hash routing: #/path/parts?query. Works on any static host (GitHub Pages included) with no rewrite rules.

const routes = [];
let fallback = null;
let current = { path: null };
const after = new Set();

/** route('recipes', fn) or route('item/:ns/:path', fn); fn(params, query) renders the page. */
export function route(pattern, fn) {
  const parts = pattern.split('/').filter(Boolean);
  routes.push({ parts, fn });
}

/** Handles anything no route matched: fn(segments, query) returns false for "not found". */
export function otherwise(fn) { fallback = fn; }

/** Runs after every page render (shell highlighting, reveal animations...). */
export function onRoute(fn) { after.add(fn); }

export function parse(hash = location.hash) {
  const raw = hash.replace(/^#\/?/, '');
  const [path, qs = ''] = raw.split('?');
  const segments = path.split('/').filter(Boolean).map(decodeURIComponent);
  return { path: segments.join('/'), segments, query: Object.fromEntries(new URLSearchParams(qs)) };
}

export function link(path, query) {
  const qs = query ? new URLSearchParams(Object.entries(query).filter(([, v]) => v != null && v !== '')).toString() : '';
  return `#/${path}${qs ? `?${qs}` : ''}`;
}

/** Changes the address; `replace` keeps it out of the history (filters, typing). */
export function go(path, query, replace = false) {
  const target = link(path, query);
  if (replace) {
    history.replaceState(null, '', target);
    resolve();
  } else {
    location.hash = target;
  }
}

async function resolve() {
  const r = parse();
  const samePage = r.path === current.path;
  current = r;
  let handled = false;
  for (const { parts, fn } of routes) {
    if (parts.length !== r.segments.length) continue;
    const params = {};
    if (parts.every((p, i) => p.startsWith(':') ? (params[p.slice(1)] = r.segments[i], true) : p === r.segments[i])) {
      await fn(params, r.query, samePage);
      handled = true;
      break;
    }
  }
  if (!handled && fallback) await fallback(r.segments, r.query, samePage);
  if (!samePage && !r.query.s) window.scrollTo({ top: 0, behavior: 'instant' });
  after.forEach(fn => fn(r, samePage));
}

export function start() {
  window.addEventListener('hashchange', resolve);
  return resolve();
}

export const currentRoute = () => current;
