// D-Tox Practice — the fun, local-only mindfulness studio.
// No accounts, no tracking: gems, streaks, plant and sounds all live here.

import { store, PLANT_NAMES, PLANT_STAGES } from './store.js';
import {
  allExercises, exerciseById, durationOf, formatDuration,
  SHOP_ITEMS, PATTERNS, cycleSeconds,
} from './exercises.js';
import * as sounds from './sounds.js';
import { drawPlant, pokePlant } from './plant.js';
import * as confetti from './confetti.js';

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => [...document.querySelectorAll(sel)];

const MOODS = ['😣', '😟', '😐', '🙂', '😌'];
const THEME_BG = { forest: '#0a1210', midnight: '#0a0e1a', paper: '#f7f4ee' };
const THEME_PREVIEW = {
  forest: ['#0a1210', '#7fd1ae', '#182722'],
  midnight: ['#0a0e1a', '#93b4ff', '#1b2434'],
  paper: ['#f7f4ee', '#39694f', '#efeae0'],
};

// PWA install prompt
let installPrompt = null;
window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  installPrompt = event;
});
window.addEventListener('appinstalled', () => {
  installPrompt = null;
  toast('Installed — find D-Tox on your home screen 🎉');
});

// ---------------------------------------------------------------- helpers

const el = (tag, cls, html) => {
  const node = document.createElement(tag);
  if (cls) node.className = cls;
  if (html != null) node.innerHTML = html;
  return node;
};

function fmtTime(seconds) {
  if (seconds <= 0) return '0m';
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) return `${Math.round(seconds / 60)}m`;
  return `${Math.floor(seconds / 3600)}h ${Math.round((seconds % 3600) / 60)}m`;
}

const dayLetter = (d) => ['S', 'M', 'T', 'W', 'T', 'F', 'S'][d.getDay()];

function toast(msg, ms = 2300) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.remove('hidden');
  clearTimeout(t.dataset.timer);
  t.dataset.timer = setTimeout(() => t.classList.add('hidden'), ms);
}

function pulse(node) {
  node.classList.remove('pulse');
  void node.offsetWidth;
  node.classList.add('pulse');
}

function themeColors() {
  const css = getComputedStyle(document.documentElement);
  const val = (name) => css.getPropertyValue(name).trim();
  return { green: val('--primary'), soil: val('--surface2'), petal: val('--petal'), gold: val('--gold') };
}

function applyTheme() {
  document.documentElement.dataset.theme = store.theme;
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_BG[store.theme] || THEME_BG.forest);
}

export function updateTopChips(withPulse = false) {
  const { gems, streak } = store.state;
  $('#gemsCount').textContent = gems;
  $('#streakCount').textContent = streak;
  if (withPulse) pulse($('#gemsChip'));
}

function confirmDialog(title, text, okLabel = 'Confirm') {
  return new Promise((resolve) => {
    const d = el('dialog', 'modal');
    d.innerHTML = `
      <h3>${title}</h3>
      <p class="muted">${text}</p>
      <div class="row-buttons">
        <button class="btn ghost" data-no>Cancel</button>
        <button class="btn" data-yes>${okLabel}</button>
      </div>`;
    document.body.append(d);
    let settled = false;
    const finish = (value) => {
      if (settled) return;
      settled = true;
      resolve(value);
      d.close();
      d.remove();
    };
    d.querySelector('[data-yes]').onclick = () => finish(true);
    d.querySelector('[data-no]').onclick = () => finish(false);
    d.oncancel = () => finish(false);
    d.showModal();
  });
}

// ---------------------------------------------------------------- routing

const SCREENS = ['home', 'progress', 'shop', 'science', 'settings'];
let currentScreen = 'home';

function showScreen(name) {
  currentScreen = name;
  $$('.screen').forEach((s) => s.classList.toggle('active', s.id === `screen-${name}`));
  $$('.tabbar a').forEach((a) => a.classList.toggle('active', a.dataset.tab === name));
  document.body.classList.toggle('zen', store.zen);
  if (name === 'home') renderHome();
  if (name === 'progress') renderProgress();
  if (name === 'shop') renderShop();
  if (name === 'science') renderScience();
  if (name === 'settings') renderSettings();
  window.scrollTo(0, 0);
}

function hidePlayer() {
  sounds.stop();
  if (breath) { cancelAnimationFrame(breath.raf); breath = null; }
  if (steps) { clearInterval(steps.timer); steps = null; }
  $('#player').classList.add('hidden');
  document.body.classList.remove('player-open');
  player.ex = null;
}

function route() {
  const hash = location.hash || '#/home';
  const [, name, arg] = hash.replace(/^#\//, '').split('/');
  if (name === 'play' && arg) {
    openPlayer(arg);
    return;
  }
  hidePlayer();
  showScreen(SCREENS.includes(name) ? name : 'home');
}

window.addEventListener('hashchange', route);

// ---------------------------------------------------------------- home

function renderHome() {
  const st = store.state;
  const stage = store.plantStage();
  const next = store.nextStageAt();

  $('#plantStage').textContent = PLANT_NAMES[stage];
  $('#plantSub').textContent = `${st.sessions} sessions${st.streak > 1 ? ` · Day ${st.streak} streak 🔥` : ''}`;
  $('#plantNext').textContent = next ? `Next growth at ${next} session${next > 1 ? 's' : ''}` : 'Fully grown ✨';
  $('#zenChip').classList.toggle('hidden', !st.zen);

  const prev = PLANT_STAGES[stage];
  const pct = next ? Math.min(100, ((st.sessions - prev) / (next - prev)) * 100) : 100;
  $('#plantMeterFill').style.width = `${pct}%`;

  const grid = $('#exerciseGrid');
  grid.innerHTML = '';
  allExercises((id) => store.isUnlocked(id)).forEach((ex) => {
    const isNew = store.isNewExercise(ex.id);
    const card = el('button', 'ex-card card');
    card.innerHTML = `
      <span class="ex-emoji">${ex.emoji}</span>
      <span class="ex-body">
        <b>${ex.title}${isNew ? '<span class="new-tag">new</span>' : ''}</b>
        <span>${ex.subtitle}</span>
      </span>
      <span class="ex-side">
        <span class="gems">💎 +${ex.baseGems}</span>
        <span class="dur">${formatDuration(durationOf(ex))}</span>
      </span>`;
    card.onclick = () => { location.hash = `#/play/${ex.id}`; };
    grid.append(card);
  });
}

// plant animation loop
function plantLoop(t) {
  if (currentScreen === 'home' && !document.hidden) {
    drawPlant($('#plantCanvas'), store.plantStage(), (store.daysSinceLast() ?? 0) >= 3, t, themeColors());
  }
  requestAnimationFrame(plantLoop);
}

$('#plantCanvas').addEventListener('click', () => {
  pokePlant();
  if (Math.random() < 0.4) {
    const lines = ['Still growing 🌱', 'Nice and steady ✨', 'Water it with practice 💧', 'Hello, plant 👋'];
    toast(lines[(Math.random() * lines.length) | 0], 1600);
  }
});

// ---------------------------------------------------------------- player

const player = { ex: null, moodBefore: null, completion: null, bonusGiven: false };

const showPaneOnly = (id) => {
  ['moodPane', 'breathPane', 'stepsPane', 'donePane'].forEach((p) => {
    $(`#${p}`).classList.toggle('hidden', p !== id);
  });
};

function buildMoodRow(container, onPick) {
  container.innerHTML = '';
  MOODS.forEach((face, i) => {
    const b = el('button', 'mood-face', face);
    b.onclick = () => {
      $$('.mood-face', container).forEach((x) => x.classList.remove('chosen'));
      b.classList.add('chosen');
      setTimeout(() => onPick(i + 1), 160);
    };
    container.append(b);
  });
}

function openPlayer(id) {
  const ex = exerciseById(id, (key) => store.isUnlocked(key));
  if (!ex) { location.hash = '#/home'; return; }
  player.ex = ex;
  player.moodBefore = null;
  player.completion = null;
  player.bonusGiven = false;
  $('#player').classList.remove('hidden');
  document.body.classList.add('player-open');
  const skipMood = store.zen || new URLSearchParams(location.search).has('skipmood');
  if (skipMood) {
    startActive();
  } else {
    $('#moodTitle').textContent = 'How do you feel right now?';
    buildMoodRow($('#moodRow'), (v) => { player.moodBefore = v; startActive(); });
    $('#moodSkip').onclick = () => { player.moodBefore = null; startActive(); };
    showPaneOnly('moodPane');
  }
}

function startActive() {
  if (!player.ex) return;
  if (player.ex.kind.type === 'breathing') startBreathing();
  else startSteps();
}

// ---- breathing

let breath = null;

function startBreathing() {
  const { pattern: pid, rounds } = player.ex.kind;
  const pattern = PATTERNS[pid];
  const duration = cycleSeconds(pid) * rounds * 1000;

  $('#breathTitle').textContent = `${player.ex.emoji}  ${player.ex.title}`;
  $('#breathRound').textContent = `round 1 of ${rounds}`;
  $('#breathCount').textContent = Math.ceil(duration / 1000);
  $('#breathSub').textContent = 'seconds';
  $('#phaseText').textContent = pattern.phases[0][1];
  const circle = $('#breathCircle');
  circle.style.transitionDuration = '0.4s';
  circle.style.transform = 'scale(0.6)';

  initSoundChips();
  showPaneOnly('breathPane');
  if (store.sound !== 'off') sounds.play(store.sound);

  breath = {
    pattern, rounds, duration, pid,
    startAt: performance.now(), pausedMs: 0, pauseStart: 0,
    lastPhase: -1, lastRound: 0, raf: 0,
  };
  breath.raf = requestAnimationFrame(breathLoop);
}

function elapsedNow(st) {
  const raw = performance.now() - st.startAt - st.pausedMs;
  return st.pauseStart ? raw - (performance.now() - st.pauseStart) : raw;
}

function breathLoop() {
  const st = breath;
  if (!st) return;
  const elapsed = Math.max(0, elapsedNow(st));

  if (elapsed >= st.duration) {
    finishActive();
    return;
  }

  const cycleMs = cycleSeconds(st.pid) * 1000;
  const pos = elapsed % cycleMs;
  let acc = 0;
  let phaseIdx = 0;
  for (let i = 0; i < st.pattern.phases.length; i += 1) {
    const secs = st.pattern.phases[i][2] * 1000;
    if (pos < acc + secs) { phaseIdx = i; break; }
    acc += secs;
  }

  if (phaseIdx !== st.lastPhase) {
    st.lastPhase = phaseIdx;
    const [kind, label, secs] = st.pattern.phases[phaseIdx];
    $('#phaseText').textContent = label;
    const circle = $('#breathCircle');
    if (kind === 'in') {
      circle.style.transitionDuration = `${secs}s`;
      circle.style.transform = 'scale(1)';
    } else if (kind === 'out') {
      circle.style.transitionDuration = `${secs}s`;
      circle.style.transform = 'scale(0.6)';
    }
  }

  const round = Math.min(st.rounds, Math.floor(elapsed / cycleMs) + 1);
  if (round !== st.lastRound) {
    st.lastRound = round;
    $('#breathRound').textContent = `round ${round} of ${st.rounds}`;
  }

  $('#breathCount').textContent = Math.ceil((st.duration - elapsed) / 1000);
  $('#ringFill').style.strokeDashoffset = String(829.4 * (1 - Math.min(1, elapsed / st.duration)));

  st.raf = requestAnimationFrame(breathLoop);
}

// ---- steps

let steps = null;

function startSteps() {
  const list = player.ex.kind.steps;
  steps = { list, index: 0, remain: null, timer: 0 };
  $('#stepsTitle').textContent = `${player.ex.emoji}  ${player.ex.title}`;
  initSoundChips();
  showPaneOnly('stepsPane');
  if (store.sound !== 'off') sounds.play(store.sound);
  renderStep();
}

function renderStep() {
  const st = steps;
  const step = st.list[st.index];
  $('#stepsProgress').textContent = `step ${st.index + 1} of ${st.list.length}`;
  $('#stepName').textContent = step.title;
  $('#stepDetail').textContent = step.detail;
  $('#stepTimer').textContent = step.seconds ? String(st.remain ?? step.seconds) : '';
  $('#stepsNext').textContent = st.index < st.list.length - 1 ? 'Next' : 'Finish';

  const dots = $('#stepDots');
  dots.innerHTML = '';
  st.list.forEach((_, i) => dots.append(el('span', `dot${i <= st.index ? ' on' : ''}`)));

  clearInterval(st.timer);
  if (step.seconds) {
    st.remain = step.seconds;
    $('#stepTimer').textContent = String(st.remain);
    st.timer = setInterval(() => {
      if (document.hidden) return;
      st.remain -= 1;
      $('#stepTimer').textContent = String(Math.max(0, st.remain));
      if (st.remain <= 0) advanceStep();
    }, 1000);
  }
}

function advanceStep() {
  const st = steps;
  if (!st) return;
  clearInterval(st.timer);
  if (st.index < st.list.length - 1) {
    st.index += 1;
    renderStep();
  } else {
    finishActive();
  }
}

// ---- sound chips

function initSoundChips() {
  ['#soundChips', '#soundChipsSteps'].forEach((sel) => {
    const wrap = $(sel);
    if (!wrap) return;
    wrap.innerHTML = '';
    sounds.SOUNDS.forEach((snd) => {
      const b = el('button', `sound-chip${store.sound === snd.id ? ' on' : ''}`, snd.label);
      b.onclick = () => {
        store.setSound(snd.id);
        $$('.sound-chip').forEach((x) => x.classList.remove('on'));
        $$(`.sound-chip`).forEach((x) => { if (x.textContent === b.textContent) x.classList.add('on'); });
        if (snd.id === 'off') sounds.stop();
        else sounds.play(snd.id);
      };
      wrap.append(b);
    });
  });
}

// ---- completion

function finishActive() {
  sounds.stop();
  if (breath) { cancelAnimationFrame(breath.raf); breath = null; }
  if (steps) { clearInterval(steps.timer); steps = null; }

  const result = store.recordCompletion(player.ex);
  player.completion = result;

  $('#doneEx').textContent = player.ex.title;
  updateTopChips(true);
  renderCompletion(result);
  showPaneOnly('donePane');
  confetti.celebrate();
  if (result.stageAfter > result.stageBefore) {
    setTimeout(() => confetti.burst(window.innerWidth / 2, window.innerHeight * 0.35, 120), 420);
  }
}

function renderCompletion(c) {
  const amountEl = $('#gemAmount');
  const zen = store.zen;

  $('#gemPop').classList.toggle('hidden', zen);
  $('#doneStreak').classList.toggle('hidden', zen || c.streak <= 1);
  $('#doneGrew').classList.toggle('hidden', zen || c.stageAfter <= c.stageBefore);
  $('#moodAfterWrap').classList.toggle('hidden', zen || player.moodBefore == null);
  $('#moodBonusNote').classList.add('hidden');

  if (!zen) {
    let shown = 0;
    const total = c.gems;
    const from = performance.now();
    const countUp = () => {
      const t = Math.min(1, (performance.now() - from) / 650);
      shown = Math.round(total * t);
      amountEl.textContent = shown;
      if (t < 1) requestAnimationFrame(countUp);
    };
    countUp();

    const list = $('#gemBreakdown');
    list.innerHTML = '';
    c.breakdown.forEach(([label, value]) => list.append(el('li', null, `${label} +${value}`)));
    $('#doneStreak').textContent = `Day ${c.streak} streak 🔥`;
    $('#doneGrew').textContent = `🌱 Your plant grew — now: ${PLANT_NAMES[c.stageAfter]}`;

    buildMoodRow($('#moodAfterRow'), (v) => {
      let bonus = 0;
      if (player.moodBefore != null && v > player.moodBefore && !player.bonusGiven) {
        player.bonusGiven = true;
        bonus = store.awardMoodBonus();
      }
      amountEl.textContent = String(c.gems + bonus);
      $('#moodAfterWrap').classList.add('hidden');
      const note = $('#moodBonusNote');
      note.classList.remove('hidden');
      note.textContent = bonus ? 'Mood lifted — +1 💎' : 'Noted 🌿';
      if (bonus) { pulse($('#gemsChip')); updateTopChips(); }
    });
  }
}

// ---- player buttons

$('#exitBreath').onclick = () => { location.hash = '#/home'; };
$('#stepsExit').onclick = () => { location.hash = '#/home'; };
$('#stepsNext').onclick = () => advanceStep();
$('#doneBtn').onclick = () => { location.hash = '#/home'; };
$('#againBtn').onclick = () => {
  player.completion = null;
  player.moodBefore = null;
  player.bonusGiven = false;
  if (store.zen) startActive();
  else {
    buildMoodRow($('#moodRow'), (v) => { player.moodBefore = v; startActive(); });
    showPaneOnly('moodPane');
  }
};

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    sounds.stop();
    if (breath && !breath.pauseStart) breath.pauseStart = performance.now();
  } else {
    if (breath && breath.pauseStart) {
      breath.pausedMs += performance.now() - breath.pauseStart;
      breath.pauseStart = 0;
    }
    if (player.ex && !$('#player').classList.contains('hidden') && store.sound !== 'off') {
      sounds.play(store.sound);
    }
  }
});

// ---------------------------------------------------------------- progress

function renderProgress() {
  const st = store.state;
  const body = $('#progressBody');

  if (st.sessions === 0) {
    body.innerHTML = `
      <div class="card pad center">
        <h3>Nothing here yet</h3>
        <p class="muted">Finish any exercise and this page starts filling in —
        sessions, time, streaks and a week view.</p>
      </div>`;
    return;
  }

  const week = store.recentDays(7);
  const maxWeek = Math.max(1, ...week.map((d) => d.count));
  const exStats = allExercises((id) => store.isUnlocked(id))
    .map((ex) => ({ ex, count: st.counts[ex.id] || 0, seconds: st.seconds[ex.id] || 0 }))
    .filter((row) => row.count > 0)
    .sort((a, b) => b.count - a.count);
  const maxCount = Math.max(1, ...exStats.map((r) => r.count));
  const stage = store.plantStage();

  body.innerHTML = `
    <div class="stat-grid">
      <div class="card stat-card"><b>${st.sessions}</b><span>Sessions</span></div>
      <div class="card stat-card"><b>${fmtTime(st.practiceSeconds)}</b><span>Time practiced</span></div>
      <div class="card stat-card"><b>${st.daysPracticed}</b><span>Days</span></div>
    </div>
    <div class="stat-grid" style="margin-top:10px">
      <div class="card stat-card"><b>${st.streak}</b><span>Current streak 🔥</span></div>
      <div class="card stat-card" style="grid-column: span 2"><b>${st.bestStreak}</b><span>Best streak</span></div>
    </div>

    <h3 class="section-title">Last 7 days</h3>
    <div class="card week-chart">
      ${week.map(({ date, count }) => `
        <div class="week-col">
          <div class="week-bar ${count === 0 ? 'zero' : ''}"
               style="height:${6 + Math.round(72 * (count / maxWeek))}px"></div>
          <span>${dayLetter(date)}</span>
        </div>`).join('')}
    </div>

    <h3 class="section-title">Plant growth</h3>
    <div class="growth-path">
      ${PLANT_NAMES.map((name, i) => {
        const icons = ['🫘', '🌱', '🌿', '🍃', '🌷', '🌸', '🌼', '🌳'];
        return `<div class="growth-step ${i <= stage ? 'reached' : ''}">
          <b>${icons[i]}</b>${name}<br>${PLANT_STAGES[i]}
        </div>`;
      }).join('')}
    </div>

    <h3 class="section-title">By exercise</h3>
    ${exStats.map(({ ex, count, seconds }) => `
      <div class="card ex-stat" style="margin-bottom:10px">
        <div class="row-between">
          <span>${ex.emoji}&nbsp; <b>${ex.title}</b></span>
          <span class="chip tiny">${count}× · ${fmtTime(seconds)}</span>
        </div>
        <div class="bar"><div style="width:${Math.round((count / maxCount) * 100)}%"></div></div>
      </div>`).join('')}

    <h3 class="section-title">Made by</h3>
    <div class="card credit-card">
      <img src="img/avatar.png" alt="coder5omkar">
      <div>
        <b>coder5omkar</b>
        <small class="muted">Creator of D-Tox · free &amp; open source</small>
      </div>
      <a class="btn ghost small" style="margin-left:auto" href="https://github.com/coder5omkar"
         target="_blank" rel="noopener">GitHub</a>
    </div>
    <p class="footnote">Counted in this browser only. Nothing is uploaded.</p>`;
}

// ---------------------------------------------------------------- shop

function renderShop() {
  const st = store.state;
  $('#shopGems').textContent = st.gems;

  const row = (item) => {
    const owned = store.isUnlocked(item.id);
    const isActive = item.type === 'theme' && store.theme === item.value;
    let action;
    if (isActive) action = '<span class="chip tiny">Active</span>';
    else if (owned && item.type === 'theme') action = `<button class="btn ghost small" data-apply="${item.value}">Apply</button>`;
    else if (owned) action = '<span class="chip tiny">Owned</span>';
    else action = `<button class="btn small" data-buy="${item.id}">💎 ${item.cost}</button>`;
    return `
      <div class="card shop-item" style="margin-bottom:10px">
        <span class="shop-swatch">${item.emoji}</span>
        <span style="flex:1">
          <b>${item.name}</b><br>
          <small class="muted">${item.description}</small>
        </span>
        ${action}
      </div>`;
  };

  $('#shopBody').innerHTML = `
    <h3 class="section-title">Themes</h3>
    ${SHOP_ITEMS.filter((i) => i.type === 'theme').map(row).join('')}
    <h3 class="section-title">Breathing extras</h3>
    <p class="muted tiny" style="margin:0 4px 10px">Unlocked patterns appear as new exercises on the home screen.</p>
    ${SHOP_ITEMS.filter((i) => i.type === 'pattern').map(row).join('')}
    <p class="footnote">Gems are earned by practicing. They are never sold, and nothing leaves your browser.</p>`;

  $$('#shopBody [data-buy]').forEach((btn) => {
    btn.onclick = async () => {
      const item = SHOP_ITEMS.find((i) => i.id === btn.dataset.buy);
      const ok = await confirmDialog(
        `Unlock ${item.name}?`,
        `Costs 💎 ${item.cost}. You have 💎 ${store.state.gems}.`,
        'Unlock',
      );
      if (!ok) return;
      if (store.buy(item)) {
        if (item.type === 'theme') {
          store.setTheme(item.value);
          applyTheme();
        }
        updateTopChips();
        renderShop();
        confetti.burst(window.innerWidth / 2, window.innerHeight * 0.3, 60);
        toast(item.type === 'theme' ? `${item.name} applied ✨` : `${item.name} added to exercises 🎉`);
      } else {
        toast('Not enough gems yet — keep practicing 🌱');
      }
    };
  });

  $$('#shopBody [data-apply]').forEach((btn) => {
    btn.onclick = () => {
      store.setTheme(btn.dataset.apply);
      applyTheme();
      renderShop();
      toast('Theme applied ✨');
    };
  });
}

// ---------------------------------------------------------------- science

function renderScience() {
  $('#scienceBody').innerHTML = `
    <h3 style="margin-top:0">Evidence-informed, not magic</h3>
    <p class="muted">Every exercise here maps to techniques studied in clinical research.
    The effects are real but modest — this is a tool, not a cure.</p>

    <h4>Slow breathing &amp; cyclic sighs</h4>
    <p class="muted">Slow breathing around six breaths a minute reliably shifts the nervous
    system toward calm and raises heart-rate variability (Zaccaro et al., 2018). In a 2023
    Stanford study (Balban et al.), one month of daily cyclic sighing improved mood and
    lowered anxiety more than mindfulness meditation — the long exhale does the work.</p>

    <h4>Body scan</h4>
    <p class="muted">Body-scan practice is a core part of mindfulness-based stress reduction.
    Studies show lower cortisol and better emotional regulation, and training attention on
    the body strengthens interoceptive awareness (Farb et al., 2013).</p>

    <h4>Grounding, 5·4·3·2·1</h4>
    <p class="muted">A standard clinical technique for acute anxiety and panic: it interrupts
    rumination by moving attention to the senses. Widely used in trauma-informed care; its
    evidence is strong clinical practice rather than large controlled trials.</p>

    <h4>Gratitude</h4>
    <p class="muted">Noticing one good thing improves mood and even sleep in controlled
    studies (Emmons &amp; McCullough, 2003). Effects are small but reliable, and it often
    works better a few times a week than every single day.</p>

    <h4>What the research overall shows</h4>
    <p class="muted">Meta-analyses of meditation programs (Goyal et al., 2014, JAMA Internal
    Medicine) find moderate evidence for reduced anxiety, depression and pain, and lower
    evidence for stress and quality of life. Structural brain changes have been observed
    after about eight weeks of consistent practice (Hölzel et al., 2011).</p>

    <h4>Honest caveats</h4>
    <p class="muted">Effect sizes are modest. The field has publication bias, and responses
    vary a lot between people. Nothing here replaces therapy for clinical anxiety or
    depression. If you're struggling, please talk to a professional.</p>

    <p class="tiny muted center" style="margin-top:18px">No tracking. What you practice here stays in this browser.</p>`;
}

// ---------------------------------------------------------------- settings

function renderSettings() {
  const st = store.state;
  const themeCard = (id, label, desc) => {
    const owned = !SHOP_ITEMS.some((i) => i.type === 'theme' && i.value === id) || store.isUnlocked(`theme:${id}`);
    const active = store.theme === id;
    const dots = THEME_PREVIEW[id].map((c) => `<i style="background:${c}"></i>`).join('');
    return `
      <button class="theme-swatch ${active ? 'active' : ''}" data-theme="${id}" ${owned ? '' : 'data-locked'}>
        <span class="dots3">${dots}</span>
        <b>${label}</b><br>
        <small>${owned ? desc : `💎 ${SHOP_ITEMS.find((i) => i.value === id).cost} in the shop`}</small>
      </button>`;
  };

  $('#settingsBody').innerHTML = `
    <div class="card setting-row hidden" id="installRow">
      <span><b>Install D-Tox</b><small id="installHint">Add it to your home screen — full screen, works offline</small></span>
      <button class="btn small" id="installBtn">Install</button>
    </div>
    <div class="card"${''}>
      <div class="setting-row">
        <span><b>Zen mode</b><small>Hide gems, streaks and the plant — just exercises</small></span>
        <label class="switch">
          <input type="checkbox" id="zenToggle" ${st.zen ? 'checked' : ''}>
          <span class="track"></span>
        </label>
      </div>
      <div class="setting-row">
        <span><b>Sounds</b><small>Pick a sound while doing an exercise — the choice is remembered</small></span>
        <span class="chip tiny">${sounds.SOUNDS.find((s) => s.id === st.sound)?.label || '🔇 Off'}</span>
      </div>
    </div>

    <h3 class="section-title">Theme</h3>
    <div class="theme-row" style="padding:0">
      ${themeCard('forest', 'Forest', 'Calm dark green')}
      ${themeCard('midnight', 'Midnight', 'Deep blue night')}
      ${themeCard('paper', 'Paper', 'Warm light')}
    </div>

    <h3 class="section-title">About</h3>
    <div class="card pad">
      <p class="muted" style="margin-top:0">D-Tox Practice — the web companion to the D-Tox
      Android app. No accounts, no internet needed, no tracking. Everything (gems, streaks,
      plant, sound choice) is stored in your browser's local storage.</p>
      <p class="muted tiny">Ambient sounds are real recordings: CC0, CC BY 3.0 and CC BY-SA 4.0 —
      full credits in LICENSES.md in the repository.</p>
    </div>

    <h3 class="section-title">Maker</h3>
    <div class="card credit-card">
      <img src="img/avatar.png" alt="coder5omkar">
      <div><b>coder5omkar</b><small class="muted">D-Tox is free &amp; open source</small></div>
      <a class="btn ghost small" style="margin-left:auto" href="https://github.com/coder5omkar"
         target="_blank" rel="noopener">GitHub</a>
    </div>

    <h3 class="section-title">Danger zone</h3>
    <div class="card pad">
      <button class="btn danger wide" id="resetBtn">Reset all data</button>
      <p class="muted tiny center" style="margin-bottom:0">Wipes gems, streaks and the plant from this browser.</p>
    </div>`;

  $('#zenToggle').onchange = (e) => {
    store.setZen(e.target.checked);
    document.body.classList.toggle('zen', store.zen);
    toast(store.zen ? 'Zen mode on 🧘' : 'Zen mode off ✨');
  };

  // install row: native prompt where available, iOS hint otherwise
  const installRow = $('#installRow');
  const installBtn = $('#installBtn');
  const standalone = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone;
  if (standalone) {
    installRow.classList.remove('hidden');
    installRow.querySelector('b').textContent = 'D-Tox is installed ✓';
    installBtn.classList.add('hidden');
  } else if (installPrompt) {
    installRow.classList.remove('hidden');
    installBtn.onclick = async () => {
      installPrompt.prompt();
      try { await installPrompt.userChoice; } catch { /* dismissed */ }
      installPrompt = null;
      renderSettings();
    };
  } else if (/iPhone|iPad|iPod/i.test(navigator.userAgent)) {
    installRow.classList.remove('hidden');
    installBtn.classList.add('hidden');
    $('#installHint').textContent = 'In Safari: tap Share → “Add to Home Screen”';
  }

  $$('#settingsBody [data-theme]').forEach((btn) => {
    btn.onclick = () => {
      if (btn.dataset.locked) { toast('Unlock it in the shop 🛍️'); return; }
      store.setTheme(btn.dataset.theme);
      applyTheme();
      renderSettings();
      toast('Theme applied ✨');
    };
  });

  $('#resetBtn').onclick = async () => {
    const ok = await confirmDialog(
      'Reset everything?',
      'Gems, sessions, streaks, the plant and all unlocks will be erased from this browser.',
      'Reset',
    );
    if (!ok) return;
    store.reset();
    applyTheme();
    document.body.classList.toggle('zen', false);
    updateTopChips();
    renderSettings();
    toast('Fresh start 🌱');
  };
}

// ---------------------------------------------------------------- boot

// ?seed demo data — handy for screenshots and trying the UI quickly.
if (new URLSearchParams(location.search).has('seed')) {
  const day = 86400000;
  const ds = (offset) => {
    const d = new Date(Date.now() - offset * day);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  Object.assign(store.state, {
    gems: 425, sessions: 7, practiceSeconds: 313, daysPracticed: 4,
    streak: 4, bestStreak: 5, lastDay: ds(0),
    counts: {
      'three-sighs': 3, box: 1, grounding: 1, 'body-scan': 1, feet: 1,
    },
    seconds: {
      'three-sighs': 90, box: 128, grounding: 125, 'body-scan': 180,
    },
    dayCounts: { [ds(6)]: 2, [ds(5)]: 1, [ds(3)]: 3, [ds(2)]: 2, [ds(1)]: 1, [ds(0)]: 4 },
    unlocks: ['theme:midnight', 'pattern:coherent'],
    theme: 'midnight',
    sound: 'forest',
  });
  store.save();
}

applyTheme();
document.body.classList.toggle('zen', store.zen);
updateTopChips();

// Deep links: #/routes work in a normal browser; ?screen= / ?play= also work
// (handy for direct links and for headless screenshots).
const bootParams = new URLSearchParams(location.search);
if (bootParams.has('play')) {
  openPlayer(bootParams.get('play'));
} else if (bootParams.has('screen') && SCREENS.includes(bootParams.get('screen'))) {
  showScreen(bootParams.get('screen'));
} else {
  route();
}

requestAnimationFrame(plantLoop);

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}
