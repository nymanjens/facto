var CACHE_NAME = 'facto-v1';
var URLS_TO_CACHE = [
  '/app/',
  '/assets/lib/bootstrap/css/bootstrap.min.css',
];

self.addEventListener('install', (event) =>
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(URLS_TO_CACHE))
  )
);


self.addEventListener('fetch', (event) =>
  event.respondWith(
    caches.match(event.request)
      .then((response) =>
        response ?
            response :
            fetch(event.request, {credentials: 'include'})
    )
  )
);

self.addEventListener('activate', (event) =>
  event.waitUntil(
    caches.keys().then((cacheNames) =>
      Promise.all(
        cacheNames
          .filter((cacheName) => cacheName !== CACHE_NAME)
          .map((cacheName) => caches.delete(cacheName))
      )
    )
  )
);
