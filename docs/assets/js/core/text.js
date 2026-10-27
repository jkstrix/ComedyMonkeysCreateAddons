// Text helpers: escaping, the little markup the data files use, and highlighting search matches.

const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

/** Escapes text for HTML (content and attributes). */
export const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ESC[c]);

/** Escapes a value for a CSS custom property or url() */
export const cssUrl = s => `url("${String(s).replace(/["\\\n]/g, '\\$&')}")`;

let resolveLink = () => null;
/** The data layer tells the markup how to turn [[id]] into a link: fn(id) => { href, name } | null */
export function setLinkResolver(fn) { resolveLink = fn; }

/**
 * The data files' markup, to HTML: **bold**, *italic*, `code`, [[entry-id]] and [[entry-id|words]].
 * Everything else is escaped. Pass `links = false` for text inside something that's already a link (a card):
 * a link inside a link isn't allowed, and the browser breaks the card apart to "fix" it.
 */
export function md(text, links = true) {
  if (text == null) return '';
  let s = esc(text);
  s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
  s = s.replace(/\[\[([a-z0-9:_/.-]+)(?:\|([^\]]+))?\]\]/g, (_, id, words) => {
    const target = resolveLink(id);
    const label = words ?? target?.name ?? id;
    return target && links ? `<a class="link" href="${target.href}">${label}</a>` : label;
  });
  s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/(^|[^*\w])\*([^*\s][^*]*?)\*(?!\w)/g, '$1<em>$2</em>');
  return s;
}

/** The markup reduced to plain text (for search and summaries). */
export function plain(text) {
  if (text == null) return '';
  return String(text)
    .replace(/\[\[([a-z0-9:_/.-]+)\|([^\]]+)\]\]/g, '$2')
    .replace(/\[\[([a-z0-9:_/.-]+)\]\]/g, (_, id) => resolveLink(id)?.name ?? id)
    .replace(/[*`]/g, '');
}

/** Wraps the parts of `text` that start with any of the query's words in <mark>. */
export function highlight(text, words) {
  const safe = esc(text);
  if (!words?.length) return safe;
  const pattern = words.filter(Boolean).map(w => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|');
  if (!pattern) return safe;
  return safe.replace(new RegExp(`(^|[^a-z0-9])(${pattern})`, 'gi'), '$1<mark>$2</mark>');
}

/** "andesite_alloy" -> "Andesite Alloy" */
export const titleCase = s => String(s).replace(/[_-]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase());

/** Folds case and accents, for matching. */
export const fold = s => String(s ?? '').normalize('NFKD').replace(/[̀-ͯ]/g, '').toLowerCase();

export const plural = (n, one, many = one + 's') => `${n.toLocaleString('en-GB')} ${n === 1 ? one : many}`;
