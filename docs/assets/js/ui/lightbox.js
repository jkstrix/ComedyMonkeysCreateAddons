// The lightbox: any image with data-zoom opens full screen. Images sharing a data-zoom value form a group you can step
// through (arrows, keys, swipe). Wheel or pinch zooms, drag pans, double-click/tap toggles 2x.

import { icon } from './icons.js';
import { esc } from '../core/text.js';

let box, img, cap, count, group = [], at = 0;
let scale = 1, x = 0, y = 0;
const pointers = new Map();
let pinchFrom = 0, pinchScale = 1, dragFrom = null, swipeFrom = null, lastFocus = null;

function build() {
  document.body.insertAdjacentHTML('beforeend', `
    <div class="lightbox" hidden role="dialog" aria-modal="true" aria-label="Image viewer">
      <div class="lb-bar"><span class="lb-count"></span><span class="sp"></span>
        <button class="lb-zoom" title="Zoom (double-click)">${icon('zoom')}</button>
        <button class="lb-close" title="Close (Esc)">${icon('close')}</button></div>
      <div class="lb-stage"><img alt=""><button class="lb-nav prev" title="Previous">${icon('left')}</button><button class="lb-nav next" title="Next">${icon('right')}</button></div>
      <div class="lb-cap"></div>
    </div>`);
  box = document.querySelector('.lightbox');
  img = box.querySelector('img');
  cap = box.querySelector('.lb-cap');
  count = box.querySelector('.lb-count');
  const stage = box.querySelector('.lb-stage');
  box.querySelector('.lb-close').onclick = close;
  box.querySelector('.lb-zoom').onclick = () => setScale(scale > 1 ? 1 : 2.5);
  box.querySelector('.prev').onclick = () => step(-1);
  box.querySelector('.next').onclick = () => step(1);
  stage.addEventListener('click', e => { if (e.target === stage && scale === 1) close(); });
  stage.addEventListener('dblclick', () => setScale(scale > 1 ? 1 : 2.5));
  stage.addEventListener('wheel', e => { e.preventDefault(); setScale(scale * (e.deltaY < 0 ? 1.15 : 1 / 1.15)); }, { passive: false });
  stage.addEventListener('pointerdown', e => {
    stage.setPointerCapture(e.pointerId);
    pointers.set(e.pointerId, e);
    if (pointers.size === 2) { pinchFrom = distance(); pinchScale = scale; }
    else { dragFrom = { px: e.clientX, py: e.clientY, x, y }; swipeFrom = e.clientX; stage.classList.add('drag'); }
  });
  stage.addEventListener('pointermove', e => {
    if (!pointers.has(e.pointerId)) return;
    pointers.set(e.pointerId, e);
    if (pointers.size === 2 && pinchFrom) setScale(pinchScale * distance() / pinchFrom);
    else if (dragFrom && scale > 1) { x = dragFrom.x + e.clientX - dragFrom.px; y = dragFrom.y + e.clientY - dragFrom.py; apply(); }
  });
  const up = e => {
    pointers.delete(e.pointerId);
    stage.classList.remove('drag');
    if (swipeFrom != null && scale === 1 && pointers.size === 0) {
      const dx = e.clientX - swipeFrom;
      if (Math.abs(dx) > 60) step(dx < 0 ? 1 : -1);
    }
    if (pointers.size < 2) pinchFrom = 0;
    dragFrom = null;
    swipeFrom = null;
  };
  stage.addEventListener('pointerup', up);
  stage.addEventListener('pointercancel', up);
  document.addEventListener('keydown', e => {
    if (box.hidden) return;
    if (e.key === 'Escape') close();
    else if (e.key === 'ArrowLeft') step(-1);
    else if (e.key === 'ArrowRight') step(1);
    else if (e.key === '+' || e.key === '=') setScale(scale * 1.25);
    else if (e.key === '-') setScale(scale / 1.25);
  });
}

const distance = () => { const [a, b] = [...pointers.values()]; return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY); };
function apply() { img.style.transform = `translate(${x}px, ${y}px) scale(${scale})`; }
function setScale(s) { scale = Math.min(6, Math.max(1, s)); if (scale === 1) { x = 0; y = 0; } apply(); }

function show() {
  const item = group[at];
  img.src = item.src;
  img.alt = item.caption;
  cap.innerHTML = esc(item.caption);
  count.textContent = group.length > 1 ? `${at + 1} / ${group.length}` : '';
  box.querySelectorAll('.lb-nav').forEach(b => { b.hidden = group.length < 2; });
  setScale(1);
}

function step(d) { if (group.length > 1) { at = (at + d + group.length) % group.length; show(); } }

function open(target) {
  if (!box) build();
  const key = target.dataset.zoom;
  const all = [...document.querySelectorAll(`img[data-zoom="${CSS.escape(key)}"]`)].filter(i => !i.closest('.pending'));
  group = all.map(i => ({ src: i.currentSrc || i.src, caption: i.dataset.caption || i.alt || '' }))
    .filter((g, i, arr) => arr.findIndex(o => o.src === g.src) === i);
  at = Math.max(0, group.findIndex(g => g.src === (target.currentSrc || target.src)));
  lastFocus = document.activeElement;
  box.hidden = false;
  document.body.style.overflow = 'hidden';
  show();
  box.querySelector('.lb-close').focus();
}

function close() {
  box.hidden = true;
  document.body.style.overflow = '';
  lastFocus?.focus?.();
}

export function installLightbox() {
  document.addEventListener('click', e => {
    const target = e.target.closest('img[data-zoom], [data-zoom-for]');
    if (!target) return;
    const imgEl = target.matches('img') ? target : document.querySelector(target.dataset.zoomFor);
    if (!imgEl || !imgEl.dataset.zoom) return;
    e.preventDefault();
    open(imgEl);
  });
}
