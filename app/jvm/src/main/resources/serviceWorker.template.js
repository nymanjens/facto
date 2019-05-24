const CACHE_NAME = 'app-$CACHE_NAME_SUFFIX$';
const ROOT_URL = new Request('/').url.slice(0, -1);
const APP_PAGE_PATH = '/appwithoutcreds/';
const GET_INITIAL_DATA_PATH = '/scalajsapi/getInitialData';
const PERSIST_ENTITY_MODIFICATIONS_PATH =
    '/scalajsapi/persistEntityModifications';
const SCRIPT_PATHS_TO_CACHE = [
  $SCRIPT_PATHS_TO_CACHE$
];

class PersistedMap {
  static get databaseName_() { return "ServiceWorkerDb"; }
  static get tableName_() { return "ServiceWorkerDb"; }

  constructor() {
    this.databasePromise_ = new Promise((promiseResolve, promiseReject) => {
      const openRequest = indexedDB.open(this.databaseName_, /* version = */ 3);

      openRequest.onupgradeneeded = (event) => {
        const database = event.target.result;
        const store =
            database.createObjectStore(this.tableName_, {keyPath: "key"});
      };

      openRequest.onerror = (e) => {
        promiseReject("Error: Opening IndexedDB failed", e);
      };

      openRequest.onsuccess = (event) => {
        const database = event.target.result;
        database.onerror = (event) => {
          console.log("[SW] Database Error " + event.target.errorCode);
        };
        promiseResolve(database);
      };
    });
  }

  /**
   * @param key string
   * @param value any
   * @return Promise<Void>
   */
  put(key, value) {
    return new Promise((promiseResolve, promiseReject) => {
      this.databasePromise_.then(database => {
        var tx = database.transaction(this.tableName_, "readwrite");
        var store = tx.objectStore(this.tableName_);
        store.put({key, value});
        tx.oncomplete = promiseResolve;
      });
    });
  }

  /**
   * @param key string
   * @return Promise<any>
   */
  get(key) {
    return new Promise((promiseResolve, promiseReject) => {
      this.databasePromise_.then(database => {
        var tx = database.transaction(this.tableName_, "readwrite");
        var store = tx.objectStore(this.tableName_);
        var query = store.get(key);
        query.onsuccess = () => {
          promiseResolve(query.result);
        };
      });
    });
  }

  /**
   * @param key string
   * @return Promise<Void>
   */
  delete(key) {
    return new Promise((promiseResolve, promiseReject) => {
      this.databasePromise_.then(database => {
        var tx = database.transaction(this.tableName_, "readwrite");
        var store = tx.objectStore(this.tableName_);
        store.delete(key);
        tx.oncomplete = promiseResolve;
      });
    });
  }
}

const persistedMap = new PersistedMap();

const generateRandomString = () => Math.random().toString(36).substring(2);

const waitingPromise = (ms) => {
  return new Promise((resolve, reject) => {
    setTimeout(resolve, ms);
  });
};
const firstSuccessfulPromise = (promise1, promise2) => {
  return Promise.race([promise1, promise2])
      .catch(e => promise1)
      .catch(e => promise2);
};
const undefinedToError = (value) => {
  if(value === undefined) {
    throw new Error("No cached value");
  }
  return value;
};


self.addEventListener('install', (event) => {
  console.log('  [SW] Installing service worker for cache', CACHE_NAME);
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        cache.add(APP_PAGE_PATH);
        cache.add(
            new Request(GET_INITIAL_DATA_PATH, {credentials: 'same-origin'}));
        cache.addAll(SCRIPT_PATHS_TO_CACHE);
      })
  );
});


self.addEventListener('fetch', (event) => {
  if(event.request.url == ROOT_URL + '/' ||
      event.request.url.startsWith(ROOT_URL + '/app/')) {
    // Check whether we are still logged in. If we are offline, return the
    // cached app page.
    console.log('  [SW] Fetch or delayed cache:', event.request.url);
    event.respondWith(
      firstSuccessfulPromise(
        fetch(event.request),
        waitingPromise(/* ms */ 800)
            .then(() => caches.match(APP_PAGE_PATH))
            .then(undefinedToError)
      )
    );
  } else if(event.request.url == ROOT_URL + GET_INITIAL_DATA_PATH) {
    // Initial data may change, e.g. when logging in. If we are oflfine, return
    // the cached values.
    console.log('  [SW] (Fetch and cache) or cache:', event.request.url);
    event.respondWith(
      firstSuccessfulPromise(
        fetch(event.request)
            .then(response =>
                caches.open(CACHE_NAME)
                  .then((cache) =>
                      cache
                          .put(event.request, response.clone())
                          .then(() => response))),
        waitingPromise(/* ms */ 1500)
            .then(() => caches.match(event.request))
            .then(undefinedToError)
      )
    );
  } else if(event.request.url == ROOT_URL + PERSIST_ENTITY_MODIFICATIONS_PATH) {
    if('sync' in self.registration) {
      const requestClone = event.request.clone();
      event.respondWith(
        fetch(event.request)
          .catch(e => {
            console.log(
                `[SW] Caught exception while persisting entity modification`,
                e);
            return new Promise((promiseResolve, promiseReject) => {
              const key = generateRandomString();
              console.log(`  [SW] Sending sync(${key})`);
              requestClone.arrayBuffer()
                  .then((arrayBuffer)  => persistedMap.put(key, arrayBuffer))
                  .then(() => self.registration.sync.register(key))
                  .then(() => {
                    // Wait for persisted Map entry to become empty, indicating
                    // completion
                    let timeout = 2;
                    const tryAgain = () => {
                      persistedMap.get(key).then(result => {
                        if(result) {
                          setTimeout(tryAgain, ++timeout);
                        } else {
                          promiseResolve(new Response());
                        }
                      });
                    };
                    tryAgain();
                  });
            });
          })
      );
    } else {
      event.respondWith(fetch(event.request));
    }
  } else {
    event.respondWith(
      caches.match(event.request)
        .then((response) => response ? response : fetch(event.request)
      )
    );
  }
});

self.addEventListener('activate', (event) => {
  console.log('  [SW] Activating service worker for cache', CACHE_NAME);
  event.waitUntil(
    caches.keys().then((cacheNames) =>
      Promise.all(
        cacheNames
          .filter((cacheName) => cacheName !== CACHE_NAME)
          .map((cacheName) => caches.delete(cacheName))
      )
    )
  );
});

self.addEventListener('sync', (event) => {
  // All syncs are requests for sending an entity modification. The tags are
  // keys in `persistedMap`, where the values in that map are request bodies.
  // This sync hanlder must remove the `persistedMap` entries when done.

  const key = event.tag;
  console.log(`  [SW] sync(${key}): Starting processing...`);
  event.waitUntil(
      persistedMap.get(key)
          .then(({value}) =>
            fetch(
                new Request(
                    ROOT_URL + PERSIST_ENTITY_MODIFICATIONS_PATH,
                    {
                      method: 'POST',
                      body: value,
                      credentials: 'same-origin',
                    })))
          .then(() => persistedMap.delete(key))
          .then(() => console.log(`  [SW] sync(${key}): Done`))
  );
});
