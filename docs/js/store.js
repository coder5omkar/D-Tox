// Local-only state. Nothing leaves this browser.

const KEY = 'dtox-web-v1';

export const PLANT_STAGES = [0, 1, 5, 15, 30, 60, 100, 200];
export const PLANT_NAMES = [
  'Seed', 'Sprout', 'Small stem', 'Leaves',
  'Bud', 'Flower', 'Full bloom', 'Ancient tree',
];

const DEFAULTS = {
  gems: 0,
  sessions: 0,
  practiceSeconds: 0,
  daysPracticed: 0,
  streak: 0,
  bestStreak: 0,
  lastDay: null,
  grace: '',
  counts: {},
  seconds: {},
  dayCounts: {},
  unlocks: [],
  theme: 'forest',
  zen: false,
  sound: 'off',
};

function load() {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) || '{}');
    return { ...structuredClone(DEFAULTS), ...raw };
  } catch {
    return structuredClone(DEFAULTS);
  }
}

const s = load();
const save = () => localStorage.setItem(KEY, JSON.stringify(s));

const DAY = 86400000;
const dateStr = (d = new Date()) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export const store = {
  get state() { return s; },

  save,

  plantStage(sessions = s.sessions) {
    let stage = 0;
    PLANT_STAGES.forEach((t, i) => { if (sessions >= t) stage = i; });
    return stage;
  },

  nextStageAt(sessions = s.sessions) {
    return PLANT_STAGES.find((t) => t > sessions) ?? null;
  },

  daysSinceLast() {
    if (!s.lastDay) return null;
    return Math.round((Date.parse(dateStr()) - Date.parse(s.lastDay)) / DAY);
  },

  isNewExercise(id) { return !(s.counts[id] > 0); },

  recordCompletion(exercise) {
    const today = dateStr();
    const firstToday = s.lastDay !== today;
    const isNew = this.isNewExercise(exercise.id);
    const streakBefore = s.streak;

    let newStreak;
    if (!s.lastDay) {
      newStreak = 1;
    } else {
      const diff = Math.round((Date.parse(today) - Date.parse(s.lastDay)) / DAY);
      if (diff <= 0) newStreak = streakBefore;
      else if (diff === 1) { s.grace = ''; newStreak = streakBefore + 1; }
      else if (diff === 2 && s.grace !== s.lastDay) { s.grace = s.lastDay; newStreak = streakBefore + 1; }
      else newStreak = 1;
    }

    const stageBefore = this.plantStage();

    s.sessions += 1;
    s.counts[exercise.id] = (s.counts[exercise.id] || 0) + 1;
    s.seconds[exercise.id] = (s.seconds[exercise.id] || 0) + exercise.durationSeconds;
    s.practiceSeconds += exercise.durationSeconds;
    s.lastDay = today;
    s.streak = newStreak;
    s.bestStreak = Math.max(s.bestStreak, newStreak);
    if (firstToday) s.daysPracticed += 1;
    s.dayCounts[today] = (s.dayCounts[today] || 0) + 1;

    const cutoff = Date.now() - 14 * DAY;
    Object.keys(s.dayCounts).forEach((k) => { if (Date.parse(k) < cutoff) delete s.dayCounts[k]; });
    save();

    const stageAfter = this.plantStage();
    if (s.zen) {
      return { gems: 0, breakdown: [], streak: newStreak, stageBefore, stageAfter };
    }

    const breakdown = [['Exercise', exercise.baseGems]];
    if (firstToday) breakdown.push(['First today', 2]);
    if (isNew) breakdown.push(['New exercise', 5]);
    if (newStreak !== streakBefore && newStreak % 7 === 0) breakdown.push([`Streak day ${newStreak}`, 10]);
    if (newStreak === 30) breakdown.push(['30-day streak', 50]);

    const gems = breakdown.reduce((n, [, v]) => n + v, 0);
    s.gems += gems;
    save();
    return { gems, breakdown, streak: newStreak, stageBefore, stageAfter };
  },

  awardMoodBonus() {
    if (s.zen) return 0;
    s.gems += 1;
    save();
    return 1;
  },

  recentDays(n = 7) {
    const out = [];
    for (let i = n - 1; i >= 0; i -= 1) {
      const d = new Date(Date.now() - i * DAY);
      const key = dateStr(d);
      out.push({ date: d, count: s.dayCounts[key] || 0 });
    }
    return out;
  },

  get unlocked() { return s.unlocks; },
  isUnlocked(id) { return s.unlocks.includes(id); },
  buy(item) {
    if (this.isUnlocked(item.id)) return true;
    if (s.gems < item.cost) return false;
    s.gems -= item.cost;
    s.unlocks.push(item.id);
    save();
    return true;
  },

  get theme() { return s.theme; },
  setTheme(id) { s.theme = id; save(); },

  get zen() { return s.zen; },
  setZen(v) { s.zen = !!v; save(); },

  get sound() { return s.sound; },
  setSound(id) { s.sound = id; save(); },

  reset() {
    Object.assign(s, structuredClone(DEFAULTS));
    save();
  },
};
