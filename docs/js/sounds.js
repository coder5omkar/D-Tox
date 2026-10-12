// Ambient sound engine — the same recordings the Android app ships,
// decoded and looped gapless with the Web Audio API.

const TRACKS = {
  rain: 'sounds/rain.ogg',
  ocean: 'sounds/ocean.ogg',
  stream: 'sounds/stream.ogg',
  forest: 'sounds/forest.ogg',
};

export const SOUNDS = [
  { id: 'off', label: '🔇 Off' },
  { id: 'rain', label: '🌧️ Rain' },
  { id: 'ocean', label: '🌊 Ocean' },
  { id: 'stream', label: '🏞️ Stream' },
  { id: 'forest', label: '🌲 Forest' },
];

let ctx = null;
let buffers = {};
let current = null; // { src, gain }

const ensureCtx = async () => {
  if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
  if (ctx.state === 'suspended') await ctx.resume();
  return ctx;
};

async function loadBuffer(name) {
  if (buffers[name]) return buffers[name];
  const data = await fetch(TRACKS[name]).then((r) => {
    if (!r.ok) throw new Error(`sound ${name}: ${r.status}`);
    return r.arrayBuffer();
  });
  const buffer = await ctx.decodeAudioData(data);
  buffers[name] = buffer;
  return buffer;
}

export function stop() {
  if (!current || !ctx) return;
  const { src, gain } = current;
  current = null;
  const t = ctx.currentTime;
  gain.gain.cancelScheduledValues(t);
  gain.gain.setValueAtTime(gain.gain.value, t);
  gain.gain.linearRampToValueAtTime(0.0001, t + 0.5);
  setTimeout(() => { try { src.stop(); } catch { /* already stopped */ } }, 700);
}

export async function play(name) {
  stop();
  if (!name || name === 'off' || !TRACKS[name]) return;
  try {
    await ensureCtx();
    const buffer = await loadBuffer(name);
    const src = ctx.createBufferSource();
    src.buffer = buffer;
    src.loop = true;
    const gain = ctx.createGain();
    gain.gain.value = 0;
    src.connect(gain).connect(ctx.destination);
    src.start();
    gain.gain.linearRampToValueAtTime(1, ctx.currentTime + 1.2);
    current = { src, gain };
  } catch {
    // Sound is a nicety — never break the exercise over it.
  }
}

export const isPlaying = () => !!current;
