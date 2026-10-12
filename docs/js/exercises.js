// The exercise catalog — a direct port of the app's Practice module.

export const PATTERNS = {
  box: {
    label: 'Box 4·4·4·4',
    phases: [['in', 'Breathe in', 4], ['hold', 'Hold', 4], ['out', 'Breathe out', 4], ['hold', 'Hold', 4]],
  },
  calm: {
    label: 'Calm 4·7·8',
    phases: [['in', 'Breathe in', 4], ['hold', 'Hold', 7], ['out', 'Breathe out', 8]],
  },
  sigh: {
    label: 'Sigh 3·1·6',
    phases: [['in', 'Breathe in', 3], ['in', 'Second sip', 1], ['out', 'Long exhale', 6]],
  },
  coherent: {
    label: 'Coherent 6·6',
    phases: [['in', 'Breathe in', 6], ['out', 'Breathe out', 6]],
  },
  extended: {
    label: 'Extended exhale 4·8',
    phases: [['in', 'Breathe in', 4], ['out', 'Breathe out', 8]],
  },
};

export const cycleSeconds = (patternId) =>
  PATTERNS[patternId].phases.reduce((n, [, , sec]) => n + sec, 0);

export const durationOf = (exercise) => {
  if (exercise.kind.type === 'breathing') {
    return cycleSeconds(exercise.kind.pattern) * exercise.kind.rounds;
  }
  return exercise.kind.steps.reduce((n, st) => n + (st.seconds ?? 15), 0);
};

const breathe = (id, title, subtitle, emoji, pattern, rounds, baseGems) => ({
  id, title, subtitle, emoji, baseGems,
  kind: { type: 'breathing', pattern, rounds },
});

const steps = (id, title, subtitle, emoji, baseGems, list) => ({
  id, title, subtitle, emoji, baseGems,
  kind: { type: 'steps', steps: list },
});

export const BASE_EXERCISES = [
  breathe('three-sighs', 'Three Sighs', 'A 30-second reset', '🌬️', 'sigh', 3, 1),
  breathe('box', 'Box Breathing', 'Calm your nervous system', '🟩', 'box', 8, 2),
  breathe('calm', '4·7·8 Breath', 'Wind down, ease into rest', '🌙', 'calm', 4, 1),
  breathe('sighing', 'Cyclic Sighing', 'Strongest evidence for fast calm', '💨', 'sigh', 6, 2),
  steps('grounding', '5·4·3·2·1 Grounding', 'When your mind races', '🖐️', 2, [
    { title: '5 things you can see', detail: 'Look around slowly. Name them silently.', seconds: 25 },
    { title: '4 things you can hear', detail: 'Near sounds, far sounds — just notice.', seconds: 25 },
    { title: '3 things you can touch', detail: 'Texture, temperature, weight.', seconds: 25 },
    { title: '2 things you can smell', detail: 'Or two smells you like.', seconds: 25 },
    { title: '1 thing you can taste', detail: 'Or a taste you remember.', seconds: 25 },
  ]),
  steps('body-scan', 'Body Scan', 'Head to toe, three minutes', '🧘', 3, [
    { title: 'Settle', detail: 'Sit or lie down. Let your body be heavy.', seconds: 20 },
    { title: 'Feet', detail: 'Weight, temperature, the points of contact.', seconds: 20 },
    { title: 'Legs', detail: 'Move slowly up through calves and thighs.', seconds: 20 },
    { title: 'Hips & belly', detail: 'Feel the belly rise and fall.', seconds: 20 },
    { title: 'Chest', detail: 'Notice the chest expanding with each breath.', seconds: 20 },
    { title: 'Hands & arms', detail: 'Warmth, tingling — whatever is there.', seconds: 20 },
    { title: 'Shoulders & neck', detail: 'Let them soften. Release the holding.', seconds: 20 },
    { title: 'Face & jaw', detail: 'Unclench. Smooth the forehead.', seconds: 20 },
    { title: 'Whole body', detail: 'Feel everything at once, breathing.', seconds: 20 },
  ]),
  steps('feet', 'Feel Your Feet', 'Instant grounding, 30 seconds', '🦶', 1, [
    { title: 'Stand or sit', detail: 'Press both feet into the floor.', seconds: 15 },
    { title: 'Notice', detail: 'Weight, temperature, exact contact points.', seconds: 15 },
  ]),
  steps('good-thing', 'One Good Thing', 'A tiny gratitude pause', '🌱', 1, [
    { title: 'Think of one small thing', detail: 'Something that went okay today. Small counts.' },
    { title: 'Picture it', detail: 'Where were you? What happened?' },
    { title: 'Notice', detail: 'How does it feel in your body right now?' },
  ]),
];

// Shop extras: two themes and two breathing patterns that become real exercises.
export const SHOP_ITEMS = [
  {
    id: 'theme:midnight', type: 'theme', value: 'midnight', cost: 40,
    name: 'Midnight', description: 'Deep-blue night palette', emoji: '🌌',
  },
  {
    id: 'theme:paper', type: 'theme', value: 'paper', cost: 50,
    name: 'Paper', description: 'Warm light theme', emoji: '📜',
  },
  {
    id: 'pattern:coherent', type: 'pattern', value: 'coherent', cost: 80,
    name: 'Coherent 6·6', description: 'Six slow breaths a minute — added as an exercise', emoji: '🌀',
  },
  {
    id: 'pattern:extended', type: 'pattern', value: 'extended', cost: 80,
    name: 'Extended Exhale 4·8', description: 'A long, calming exhale — added as an exercise', emoji: '🍃',
  },
];

export const PATTERN_EXERCISES = {
  coherent: breathe('coherent', 'Coherent 6·6', 'Six slow breaths a minute', '🌀', 'coherent', 5, 2),
  extended: breathe('extended', 'Extended Exhale 4·8', 'A long, calming exhale', '🍃', 'extended', 6, 2),
};

export const allExercises = (isUnlocked) => [
  ...BASE_EXERCISES,
  ...Object.entries(PATTERN_EXERCISES)
    .filter(([key]) => isUnlocked(`pattern:${key}`))
    .map(([, ex]) => ex),
];

export const exerciseById = (id, isUnlocked) =>
  allExercises(isUnlocked).find((e) => e.id === id) ?? null;

export const formatDuration = (seconds) => {
  if (seconds < 60) return `~${seconds}s`;
  return `~${Math.round(seconds / 60)} min`;
};
