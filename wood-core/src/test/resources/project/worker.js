var cacheName = 'ngo-store';
var filesToCache = [
    '/ngo/index.htm'
];

/* Start the service worker and cache all of the app's content */
self.addEventListener('install', error => error.waitUntil(caches.open(cacheName).then(cache => cache.addAll(filesToCache))));

/* Serve cached content when offline */
self.addEventListener('fetch', error => error.respondWith(caches.match(error.request).then(response => response || fetch(error.request))));
