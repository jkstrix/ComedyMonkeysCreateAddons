// Gears, drawn as SVG paths: the logo, the slow background cogs and the "render pending" plate.

const cache = new Map();

/** A gear's outline with `teeth` teeth (and a hub hole), in a -50..50 box. */
function path(teeth) {
  if (cache.has(teeth)) return cache.get(teeth);
  const R = 48, r = R - (teeth > 16 ? 6 : 10), hub = teeth > 16 ? 30 : 14, step = 2 * Math.PI / teeth;
  const pts = [];
  for (let i = 0; i < teeth; i++) {
    const a = i * step;
    for (const [t, d] of [[a, r], [a + step * .18, R], [a + step * .48, R], [a + step * .66, r]])
      pts.push(`${(d * Math.cos(t)).toFixed(2)},${(d * Math.sin(t)).toFixed(2)}`);
  }
  const d = `M${pts.join('L')}Z M${hub},0 A${hub},${hub} 0 1 0 ${-hub},0 A${hub},${hub} 0 1 0 ${hub},0Z`;
  cache.set(teeth, d);
  return d;
}

/** <svg> of a gear; the class sets its size, colour and spin. */
export const gear = (teeth = 12, cls = '') =>
  `<svg class="${cls}" viewBox="-50 -50 100 100" aria-hidden="true"><path fill="currentColor" fill-rule="evenodd" d="${path(teeth)}"/></svg>`;
