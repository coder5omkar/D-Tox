// The practice plant — drawn on canvas, same shapes as the Android app,
// plus a gentle sway and a wiggle when you tap it.

let wiggle = 0;
let wiggleDir = 0;

export const pokePlant = () => {
  wiggle = 1;
  wiggleDir = (Math.random() - 0.5) * 2;
};

function leaf(ctx, x, y, len, wid, angle, color) {
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(angle);
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.bezierCurveTo(len * 0.25, -wid, len * 0.75, -wid, len, 0);
  ctx.bezierCurveTo(len * 0.75, wid, len * 0.25, wid, 0, 0);
  ctx.fillStyle = color;
  ctx.fill();
  ctx.restore();
}

function mix(hexA, hexB, t) {
  const a = hexA.match(/\w\w/g).map((h) => parseInt(h, 16));
  const b = hexB.match(/\w\w/g).map((h) => parseInt(h, 16));
  const c = a.map((v, i) => Math.round(v + (b[i] - v) * t));
  return `#${c.map((v) => v.toString(16).padStart(2, '0')).join('')}`;
}

/**
 * @param {HTMLCanvasElement} canvas
 * @param {number} stage 0..7
 * @param {boolean} wilting
 * @param {number} time  performance.now()
 * @param {{green:string, soil:string, petal:string, gold:string}} colors
 */
export function drawPlant(canvas, stage, wilting, time, colors) {
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.clientWidth;
  const h = canvas.clientHeight;
  if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
    canvas.width = w * dpr;
    canvas.height = h * dpr;
  }
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, h);

  const green = wilting ? mix(colors.green, '#9a8b6a', 0.5) : colors.green;
  const leafColor = wilting ? mix(colors.green, '#8e9b6f', 0.3) : mix(colors.green, '#ffffff', 0.16);
  const soil = colors.soil;

  const cx = w / 2;
  const baseY = h * 0.86;
  const unit = Math.min(w, h);
  const sway = Math.sin(time / 2300) * 0.022;
  const kick = wiggle * wiggleDir * 0.16 * Math.sin(time / 55);

  ctx.save();
  ctx.translate(cx, baseY);
  ctx.rotate(sway + kick);
  ctx.translate(-cx, -baseY);

  // soil mound (upper half-disc sitting on the baseline)
  ctx.beginPath();
  ctx.ellipse(cx, baseY, w * 0.21, h * 0.1, 0, Math.PI, Math.PI * 2);
  ctx.closePath();
  ctx.fillStyle = soil;
  ctx.fill();

  const stages = Math.max(0, Math.min(7, stage));

  if (stages === 0) {
    ctx.save();
    ctx.translate(cx, baseY - unit * 0.05);
    ctx.rotate(-0.42);
    ctx.beginPath();
    ctx.ellipse(0, 0, unit * 0.1, unit * 0.062, 0, 0, Math.PI * 2);
    ctx.fillStyle = green;
    ctx.fill();
    ctx.restore();
    ctx.restore();
    decayWiggle();
    return;
  }

  const stemHeight = h * (0.3 + 0.08 * Math.min(6, stages - 1));
  const stemTop = baseY - stemHeight;
  ctx.lineCap = 'round';
  ctx.strokeStyle = green;
  ctx.lineWidth = unit * (0.026 + 0.006 * stages);
  ctx.beginPath();
  ctx.moveTo(cx, baseY);
  ctx.lineTo(cx, stemTop);
  ctx.stroke();

  const leafCount = stages <= 2 ? 2 : stages === 3 ? 3 : stages === 4 ? 4 : stages === 5 ? 4 : stages === 6 ? 5 : 4;
  const len = unit * (0.34 + 0.04 * stages);
  const wid = len * 0.36;
  for (let i = 0; i < leafCount; i += 1) {
    const frac = leafCount === 1 ? 0.5 : i / (leafCount - 1);
    const y = baseY - stemHeight * (0.32 + 0.58 * frac);
    const angle = (i % 2 === 0 ? -30 : -150) * (Math.PI / 180);
    leaf(ctx, cx, y, len, wid, angle, leafColor);
  }

  if (stages === 4) {
    ctx.beginPath();
    ctx.arc(cx, stemTop, unit * 0.07, 0, Math.PI * 2);
    ctx.fillStyle = colors.petal;
    ctx.fill();
  } else if (stages === 5 || stages === 6) {
    const petalR = unit * (stages === 5 ? 0.055 : 0.07);
    const ringR = unit * (stages === 5 ? 0.11 : 0.14);
    ctx.fillStyle = colors.petal;
    for (let a = 0; a < 6; a += 1) {
      const ang = (a * 60 * Math.PI) / 180;
      ctx.beginPath();
      ctx.arc(cx + ringR * Math.cos(ang), stemTop + ringR * Math.sin(ang), petalR, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.beginPath();
    ctx.arc(cx, stemTop, unit * 0.05, 0, Math.PI * 2);
    ctx.fillStyle = colors.gold;
    ctx.fill();
  } else if (stages === 7) {
    const y = stemTop + unit * 0.1;
    ctx.fillStyle = leafColor;
    ctx.beginPath(); ctx.arc(cx, y, unit * 0.16, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx - unit * 0.22, y + unit * 0.06, unit * 0.12, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + unit * 0.22, y + unit * 0.06, unit * 0.12, 0, Math.PI * 2); ctx.fill();
  }

  ctx.restore();
  decayWiggle();
}

function decayWiggle() {
  wiggle *= 0.97;
  if (wiggle < 0.01) wiggle = 0;
}
