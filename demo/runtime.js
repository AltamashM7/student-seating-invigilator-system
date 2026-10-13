/* Simulated accounts and browser-local data for the team familiarization demo. */
(function () {
  'use strict';
  const root = new URL('./', document.currentScript.src);
  const key = 'exam-planner-team-demo-v1:' + root.pathname;
  const sessionKey = key + ':session';
  let initial, storageWorks = true, memory, activeSession;
  const read = name => {
    try { return localStorage.getItem(name); } catch (_) { storageWorks = false; return name === key ? memory : null; }
  };
  const write = value => {
    const raw = JSON.stringify(value);
    if (!storageWorks) { memory = raw; return; }
    try { localStorage.setItem(key, raw); }
    catch (cause) {
      const error = new Error('Browser storage is unavailable or full. No changes saved. Free storage or use another browser.');
      error.cause = cause; throw error;
    }
  };
  function session(value) {
    if (arguments.length) {
      activeSession = value;
      try { value ? sessionStorage.setItem(sessionKey, JSON.stringify(value)) : sessionStorage.removeItem(sessionKey); } catch (_) {}
    }
    let saved = activeSession;
    try { saved = JSON.parse(sessionStorage.getItem(sessionKey)) || saved; } catch (_) {}
    if (!saved || saved.expiresAt <= Date.now()) return null;
    return saved;
  }
  function fresh() {
    const engine = new DemoEngine(initial);
    engine.state.epoch = crypto.randomUUID();
    engine.initialize();
    return engine;
  }
  const ready = (async () => {
    const response = await fetch(new URL('assets/data/demo-seed.json', root));
    if (!response.ok) throw new Error('The demo sample data could not be loaded. Refresh the page.');
    initial = await response.json();
    let raw = read(key), saved;
    try { saved = raw && JSON.parse(raw); } catch (_) {}
    if (!saved || saved.schemaVersion !== initial.schemaVersion) {
      saved = fresh().exportState();
      write(saved);
    }
    return saved;
  })();
  ready.catch(error => {
    document.addEventListener('DOMContentLoaded', () => showStatus(error.message));
    if (document.readyState !== 'loading') showStatus(error.message);
  });
  function showStatus(message) {
    const node = document.getElementById('demoStorageStatus');
    if (node) node.textContent = message;
  }
  let queue = Promise.resolve();
  function serialized(action) {
    if (navigator.locks) return navigator.locks.request(key, action);
    const task = queue.then(action, action);
    queue = task.catch(() => {});
    return task;
  }
  apiRequest = async function (url, options = {}) {
    await ready;
    const target = new URL(url, location.href), match = target.pathname.match(/\/api\/(.+)$/);
    if (!match) throw new Error('Unknown team demo request');
    const path = match[1], method = (options.method || 'GET').toUpperCase(), params = Object.fromEntries(target.searchParams);
    if (options.body instanceof URLSearchParams) for (const key of new Set(options.body.keys())) {
      const values = options.body.getAll(key); params[key] = values.length > 1 ? values : values[0];
    }
    if (options.body instanceof FormData) {
      const file = options.body.get('file');
      if (!(file instanceof File) || !file.size) throw new Error('Choose a CSV file');
      if (file.size > 2 * 1024 * 1024) throw new Error('Choose a CSV under 2 MB');
      params.csv = await file.text();
    }
    return serialized(() => {
      const engine = new DemoEngine(JSON.parse(read(key) || memory));
      const current = session(), result = engine.handle(path, method, params, current?.id || 0);
      if (method !== 'GET' && !['auth/login', 'auth/logout'].includes(path)) write(engine.exportState());
      if (path === 'auth/login') session({id:result.userId, expiresAt:Date.now() + 30 * 60 * 1000});
      else if (path === 'auth/logout') session(null);
      else if (current) session({...current, expiresAt:Date.now() + 30 * 60 * 1000});
      if (!storageWorks) showStatus('Temporary browser session · Storage is disabled; changes may be lost on navigation.');
      return result;
    });
  };
  function init() {
    const bar = document.createElement('aside');
    bar.className = 'demo-bar';bar.setAttribute('aria-label', 'Team demo controls');
    bar.innerHTML = `<div><strong>Team demo</strong><span id="demoStorageStatus">Sample data saved in this browser · Simulated login</span></div><nav aria-label="Demo help"><a href="${new URL('guide.html', root).href}">Demo guide</a><button id="demoReset" type="button" class="secondary">Reset sample data</button></nav>`;
    document.body.prepend(bar);
    document.getElementById('demoReset').addEventListener('click', async event => {
      if (!confirm('Restore the original sample records and demo accounts in this browser? Your practice changes will be removed.')) return;
      event.target.disabled = true;
      try { await ready; await serialized(() => write(fresh().exportState())); session(null);location.href = root.href; }
      catch (error) { showStatus(error.message); event.target.disabled = false; }
    });
    if (location.pathname.endsWith('/login.html')) {
      const credentials = document.createElement('div');credentials.className = 'demo-credentials';
      credentials.innerHTML = '<h2>Demo accounts</h2><p><strong>Administrator:</strong> <code>admin</code> / <code>Admin@123</code><br><strong>Faculty:</strong> <code>cse.faculty1</code> / <code>Faculty@123</code></p><p class="helper-text">Use fictional records and demo passwords. Account changes apply only in this browser.</p>';
      document.getElementById('loginForm').before(credentials);
    }
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
