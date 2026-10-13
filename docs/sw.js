// Offline-first: cache everything once, then serve from cache.
const CACHE = 'dtox-web-v3';
const CORE = [
  './',
  './index.html',
  './styles.css',
  './manifest.webmanifest',
  './img/icon-192.png',
  './img/icon-512.png',
  './img/avatar.png',
  './js/app.js',
  './js/store.js',
  './js/exercises.js',
  './js/sounds.js',
  './js/plant.js',
  './js/confetti.js',
  './sounds/rain.ogg',
  './sounds/ocean.ogg',
  './sounds/stream.ogg',
  './sounds/forest.ogg',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(CORE)).then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      return fetch(event.request).then((response) => {
        const copy = response.clone();
        caches.open(CACHE).then((cache) => cache.put(event.request, copy));
        return response;
      });
    }),
  );
});
