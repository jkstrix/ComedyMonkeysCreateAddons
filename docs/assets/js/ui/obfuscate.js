// Mysteries: a name written as a run of ▓ in the data (an entry with "mystery": true) shows as Minecraft's obfuscated
// text, characters churning in place, wherever it appears: titles, cards, links, search. The real name is never in the
// data, so there's nothing to give it away. Still for anyone who prefers reduced motion.

const MARK = /▓+/g;
const GLYPHS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#$%&?@';
const still = matchMedia('(prefers-reduced-motion: reduce)');
const spans = new Set();

const scramble = n => Array.from({ length: n }, () => GLYPHS[Math.floor(Math.random() * GLYPHS.length)]).join('');

/** Wraps every run of ▓ under {@code root} in a churning span (text in inputs, scripts and styles left alone). */
function wrap(root) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode: n => n.nodeValue.includes('▓') && !n.parentElement?.closest('.obf, script, style, textarea, input')
      ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT,
  });
  const found = [];
  while (walker.nextNode()) found.push(walker.currentNode);
  for (const node of found) {
    const parts = node.nodeValue.split(/(▓+)/);
    const frag = document.createDocumentFragment();
    for (const part of parts) {
      if (!part) continue;
      if (!part.startsWith('▓')) { frag.append(part); continue; }
      const span = document.createElement('span');
      span.className = 'obf';
      span.dataset.n = part.length;
      span.setAttribute('aria-label', 'unknown');
      span.textContent = scramble(part.length);
      spans.add(span);
      frag.append(span);
    }
    node.replaceWith(frag);
  }
  // attributes the reader sees (tooltips, alt text, the tab's title) get a still scramble
  for (const el of root.querySelectorAll?.('[title*="▓"], [alt*="▓"], [aria-label*="▓"]') ?? [])
    for (const attr of ['title', 'alt', 'aria-label'])
      if (el.getAttribute(attr)?.includes('▓')) el.setAttribute(attr, el.getAttribute(attr).replace(MARK, m => scramble(m.length)));
  if (document.title.includes('▓')) document.title = document.title.replace(MARK, m => scramble(m.length));
}

export function installObfuscation() {
  wrap(document.body);
  new MutationObserver(records => {
    for (const r of records)
      for (const n of r.addedNodes)
        if (n.nodeType === 1 && !n.classList?.contains('obf')) wrap(n);
        else if (n.nodeType === 3 && n.nodeValue.includes('▓') && n.parentElement) wrap(n.parentElement);
  }).observe(document.body, { childList: true, subtree: true });
  setInterval(() => {
    if (still.matches) return;
    for (const span of spans) {
      if (!span.isConnected) { spans.delete(span); continue; }
      span.textContent = scramble(+span.dataset.n);
    }
  }, 70);
}
