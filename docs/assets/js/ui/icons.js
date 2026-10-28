// The icon set: one inline SVG sprite, used as icon('name'). Line icons on a 24 grid, drawn in currentColor.

const S = 'fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"';

const ICONS = {
  home: `<path ${S} d="M3 11l9-7 9 7v9h-6v-6H9v6H3z"/>`,
  book: `<path ${S} d="M4 4h6a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H4zM20 4h-5a3 3 0 0 0-3 3"/><path ${S} d="M20 4v14h-7"/>`,
  cube: `<path ${S} d="M12 3l8 4.5v9L12 21l-8-4.5v-9zM12 12l8-4.5M12 12v9M12 12L4 7.5"/>`,
  cog: `<circle ${S} cx="12" cy="12" r="3.2"/><circle ${S} cx="12" cy="12" r="7"/><path ${S} d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1"/>`,
  gem: `<path ${S} d="M6 3h12l4 6-10 12L2 9zM2 9h20M9 3l3 18 3-18"/>`,
  skull: `<path ${S} d="M5 11a7 7 0 0 1 14 0v4h-2v4H7v-4H5z"/><circle cx="9.5" cy="12" r="1.6" fill="currentColor"/><circle cx="14.5" cy="12" r="1.6" fill="currentColor"/>`,
  grid: `<path ${S} d="M4 4h16v16H4zM4 9.3h16M4 14.6h16M9.3 4v16M14.6 4v16"/>`,
  steps: `<path ${S} d="M3 20h5v-5h5v-5h5V4h3"/>`,
  net: `<circle ${S} cx="12" cy="5" r="2.2"/><circle ${S} cx="5" cy="18" r="2.2"/><circle ${S} cx="19" cy="18" r="2.2"/><path ${S} d="M11 7l-5 9M13 7l5 9M7.2 18h9.6"/>`,
  mountain: `<path ${S} d="M2 20l7-11 4 6 3-4 6 9zM14.5 6.5a2 2 0 1 0 0-.1"/>`,
  search: `<circle ${S} cx="10.5" cy="10.5" r="6.5"/><path ${S} d="M15.5 15.5L21 21"/>`,
  star: `<path ${S} d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>`,
  starFill: `<path fill="currentColor" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>`,
  print: `<path ${S} d="M7 9V3h10v6M7 17H4v-7h16v7h-3M7 14h10v7H7z"/>`,
  close: `<path ${S} d="M6 6l12 12M18 6L6 18"/>`,
  left: `<path ${S} d="M15 5l-7 7 7 7"/>`,
  right: `<path ${S} d="M9 5l7 7-7 7"/>`,
  zoom: `<path ${S} d="M14 4h6v6M10 20H4v-6M20 4l-7 7M4 20l7-7"/>`,
  info: `<circle cx="12" cy="12" r="10" fill="currentColor" opacity=".18"/><path ${S} stroke-width="2.2" d="M12 11v6M12 7.5v.5"/>`,
  warning: `<path d="M12 3l10 18H2z" fill="currentColor" opacity=".18"/><path ${S} stroke-width="2.2" d="M12 10v5M12 18v.5"/>`,
  fact: `<path ${S} d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"/>`,
  strategy: `<path ${S} d="M5 21V4M5 4h11l-2 4 2 4H5"/>`,
  bolt: `<path ${S} d="M13 2L4 14h7l-1 8 9-12h-7z"/>`,
  lamp: `<path ${S} d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.8 10.6c.6.5 1.3 1.4 1.3 2.4v.5h5V16c0-1 .7-1.9 1.3-2.4A6 6 0 0 0 12 3z"/>`,
  flame: `<path ${S} d="M12 22c4 0 7-3 7-7 0-4-3-6-4-10-2 2-3 4-3 6-1-1-2-2-2-4-2 2-5 5-5 8 0 4 3 7 7 7z"/>`,
  rotate: `<path ${S} d="M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5"/>`,
  drop: `<path ${S} d="M12 3s7 7.5 7 12a7 7 0 0 1-14 0c0-4.5 7-12 7-12z"/>`,
  bookmark: `<path ${S} d="M6 3h12v18l-6-4-6 4z"/>`,
  clock: `<circle ${S} cx="12" cy="12" r="9"/><path ${S} d="M12 7v5l3 2"/>`,
  more: `<circle cx="5" cy="12" r="1.8" fill="currentColor"/><circle cx="12" cy="12" r="1.8" fill="currentColor"/><circle cx="19" cy="12" r="1.8" fill="currentColor"/>`,
  arrow: `<path ${S} d="M4 12h16M14 6l6 6-6 6"/>`,
  check: `<path ${S} d="M4 12l5 5L20 6"/>`,
  layers: `<path ${S} d="M12 3l9 5-9 5-9-5zM3 13l9 5 9-5"/>`,
  link: `<path ${S} d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>`,
};

let injected = false;

/** Puts the sprite in the page once. */
export function injectIcons() {
  if (injected) return;
  injected = true;
  const symbols = Object.entries(ICONS).map(([k, v]) => `<symbol id="i-${k}" viewBox="0 0 24 24">${v}</symbol>`).join('');
  document.body.insertAdjacentHTML('afterbegin', `<svg width="0" height="0" style="position:absolute" aria-hidden="true">${symbols}</svg>`);
}

/** An icon, sized by CSS. Unknown names draw the cog. */
export const icon = (name, cls = '') =>
  `<svg class="${cls}" aria-hidden="true"><use href="#i-${ICONS[name] ? name : 'cog'}"/></svg>`;
