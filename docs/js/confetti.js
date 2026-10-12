// Confetti bursts — small, cheap and purely a celebration.

const canvas = () => document.getElementById('confettiCanvas');

let particles = [];
let running = false;

const COLORS = ['#7fd1ae', '#e8c468', '#93b4ff', '#ff9f9f', '#c9a7ff', '#9fe8c8'];

function resize(c) {
  const dpr = window.devicePixelRatio || 1;
  c.width = window.innerWidth * dpr;
  c.height = window.innerHeight * dpr;
  c.getContext('2d').setTransform(dpr, 0, 0, dpr, 0, 0);
}

function tick() {
  const c = canvas();
  if (!c) { running = false; return; }
  const ctx = c.getContext('2d');
  ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);

  particles = particles.filter((p) => p.life > 0 && p.y < window.innerHeight + 40);
  particles.forEach((p) => {
    p.x += p.vx;
    p.y += p.vy;
    p.vy += 0.12;
    p.vx *= 0.995;
    p.rot += p.vr;
    p.life -= 1;
    ctx.save();
    ctx.translate(p.x, p.y);
    ctx.rotate(p.rot);
    ctx.globalAlpha = Math.min(1, p.life / 40);
    ctx.fillStyle = p.color;
    if (p.shape === 'circle') {
      ctx.beginPath();
      ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
      ctx.fill();
    } else {
      ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
    }
    ctx.restore();
  });

  if (particles.length > 0) {
    requestAnimationFrame(tick);
  } else {
    running = false;
    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
  }
}

export function burst(x = window.innerWidth / 2, y = window.innerHeight * 0.25, count = 90) {
  const c = canvas();
  if (!c) return;
  resize(c);
  for (let i = 0; i < count; i += 1) {
    const angle = Math.random() * Math.PI * 2;
    const speed = 2 + Math.random() * 7;
    particles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - 4,
      size: 5 + Math.random() * 7,
      rot: Math.random() * Math.PI,
      vr: (Math.random() - 0.5) * 0.3,
      life: 90 + Math.random() * 50,
      color: COLORS[(Math.random() * COLORS.length) | 0],
      shape: Math.random() > 0.45 ? 'rect' : 'circle',
    });
  }
  if (!running) {
    running = true;
    requestAnimationFrame(tick);
  }
}

/** A bigger, two-sided burst for milestones. */
export function celebrate() {
  burst(window.innerWidth * 0.2, window.innerHeight * 0.3, 70);
  setTimeout(() => burst(window.innerWidth * 0.8, window.innerHeight * 0.3, 70), 180);
}
