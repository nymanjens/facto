const CACHE_NAME = 'facto-v3';
const ROOT_URL = new Request('/').url.slice(0, -1);
const APP_PAGE_PATH = '/appwithoutcreds/';
const GET_INITIAL_DATA_PATH = '/scalajsapi/getInitialData';
const SCRIPT_PATHS_TO_CACHE = [
  %SCRIPT_PATHS_TO_CACHE%
];

self.addEventListener('install', (event) => {
  console.log('  Installing service worker for cache', CACHE_NAME);
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        cache.add(APP_PAGE_PATH);
        cache.add(
            new Request(GET_INITIAL_DATA_PATH, {credentials: 'same-origin'}));
        cache.addAll(SCRIPT_PATHS_TO_CACHE);
      })
  )
});


self.addEventListener('fetch', (event) => {
  if(event.request.url.startsWith(ROOT_URL + '/app/')) {
    // Check whether we are still logged in. If we are offline, return the
    // cached app page.
    console.log('  Fetch or cache:', event.request.url)
    event.respondWith(
      fetch(event.request)
          .catch(e => {
            console.log(
                `Caught exception while fetching ${event.request.url}`, e);
            return caches.match(APP_PAGE_PATH);
          })
    );
  } else if(event.request.url == ROOT_URL + GET_INITIAL_DATA_PATH) {
    // Initial data may change, e.g. when logging in. If we are oflfine, return
    // the cached values.
    console.log('  (Fetch and cache) or cache:', event.request.url)
    event.respondWith(
      fetch(event.request)
          .then(response =>
              caches.open(CACHE_NAME)
                .then((cache) =>
                    cache
                        .put(event.request, response.clone())
                        .then(() => response)))
          .catch(e => {
            console.log(
                `Caught exception while fetching ${event.request.url}`, e);
            return caches.match(event.request);
          })
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
  console.log('  Activating service worker for cache', CACHE_NAME);
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
