// Motion that waits until it's seen: cards rising in, gauge needles swinging to their value.

const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;

const observer = 'IntersectionObserver' in window ? new IntersectionObserver(entries => {
  for (const e of entries) {
    if (!e.isIntersecting) continue;
    play(e.target);
    observer.unobserve(e.target);
  }
}, { rootMargin: '0px 0px -6% 0px', threshold: 0.05 }) : null;

function play(el) {
  if (el.classList.contains('gauge')) {
    const arc = el.querySelector('.arc');
    const needle = el.querySelector('.needle');
    if (arc) arc.style.strokeDashoffset = arc.dataset.offset;
    if (needle) needle.style.transform = `rotate(${needle.dataset.angle}deg)`;
  } else {
    el.classList.add('in');
  }
}

/** Arms everything new in `root`: reveals stagger slightly in reading order. */
export function animate(root = document) {
  const items = root.querySelectorAll('.reveal:not(.in), .gauge:not([data-armed])');
  let i = 0;
  const fold = innerHeight;
  for (const el of items) {
    if (el.classList.contains('gauge')) el.dataset.armed = '1';
    if (reduced || !observer) { play(el); continue; }
    if (el.classList.contains('reveal')) el.style.transitionDelay = `${Math.min(i++, 8) * 40}ms`;
    // already on screen: play now rather than wait for the observer's first round
    const top = el.getBoundingClientRect().top;
    if (top < fold) requestAnimationFrame(() => play(el));
    else observer.observe(el);
  }
}
