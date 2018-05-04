const CACHE_NAME = 'facto-v2';
const ROOT_URL = new Request("/").url;
const APP_PAGE_PATH = '/appwithoutcreds/';
const SCRIPT_PATHS_TO_CACHE = [
  '/assets/lib/bootstrap/css/bootstrap.min.css',
];

self.addEventListener('install', (event) => {
  console.log("  Installing service worker for cache", CACHE_NAME);
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        cache.add(APP_PAGE_PATH);
        cache.addAll(SCRIPT_PATHS_TO_CACHE);
      })
  )
});


self.addEventListener('fetch', (event) => {
  if(event.request.url.startsWith(ROOT_URL + 'app/')) {
    console.log("  Fetching app at", event.request.url)
    // Check whether we are still logged in
    // If we are offline, return the cached app page.
    event.respondWith(
      fetch(event.request).catch(() => caches.match(APP_PAGE_PATH))
    );
  } else {
    event.respondWith(
      caches.match(event.request)
        .then((response) => response ? response : fetch(event.request)
      )
    )
  }
});

self.addEventListener('activate', (event) => {
  console.log("  Activating service worker for cache", CACHE_NAME);
  event.waitUntil(
    caches.keys().then((cacheNames) =>
      Promise.all(
        cacheNames
          .filter((cacheName) => cacheName !== CACHE_NAME)
          .map((cacheName) => caches.delete(cacheName))
      )
    )
  )
});
