import assert from 'node:assert/strict';
import { test } from 'node:test';
import { generateDiagram, getProviders } from '../src/api.js';

function setup(t) {
  const previousWindow = globalThis.window;
  globalThis.window = globalThis;
  t.after(() => { globalThis.window = previousWindow; });
  t.mock.timers.enable({ apis: ['setTimeout'] });
}

function aborted(signal) {
  return new Promise((_, reject) => {
    signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), { once: true });
  });
}

test('permite una generación de 90 segundos, incluyendo la reparación', async (t) => {
  setup(t);
  let requestSignal;
  t.mock.method(globalThis, 'fetch', (_url, { signal }) => {
    requestSignal = signal;
    return Promise.race([
      aborted(signal),
      new Promise((resolve) => setTimeout(() => resolve({
        ok: true, json: async () => ({ bpmnXml: '<definitions />' }),
      }), 90_000)),
    ]);
  });
  const result = generateDiagram('Proceso ficticio', 'openrouter');
  result.catch(() => {});
  t.mock.timers.tick(65_000);
  assert.equal(requestSignal.aborted, false);
  t.mock.timers.tick(25_000);
  assert.deepEqual(await result, { bpmnXml: '<definitions />' });
});

test('limita también la espera del cuerpo después de recibir los encabezados', async (t) => {
  setup(t);
  let requestSignal;
  let bodyStarted;
  const started = new Promise((resolve) => { bodyStarted = resolve; });
  t.mock.method(globalThis, 'fetch', async (_url, { signal }) => {
    requestSignal = signal;
    return { ok: true, json: () => { bodyStarted(); return aborted(signal); } };
  });
  const result = generateDiagram('Proceso ficticio', 'openrouter');
  result.catch(() => {});
  await started;
  await Promise.resolve();
  t.mock.timers.tick(150_000);
  assert.equal(requestSignal.aborted, true);
  await assert.rejects(result, /tardó demasiado/);
});

test('un timeout permite iniciar otra solicitud normalmente', async (t) => {
  setup(t);
  const fetchMock = t.mock.method(globalThis, 'fetch', (_url, { signal }) => aborted(signal));
  const result = generateDiagram('Proceso ficticio', 'openrouter');
  const rejected = assert.rejects(result, /tardó demasiado/);
  t.mock.timers.tick(150_000);
  await rejected;
  fetchMock.mock.mockImplementation(async () => ({ ok: true, json: async () => ({ bpmnXml: 'ok' }) }));
  assert.deepEqual(await generateDiagram('Otro proceso', 'openrouter'), { bpmnXml: 'ok' });
});

test('conserva el mensaje específico de timeout del backend', async (t) => {
  setup(t);
  t.mock.method(globalThis, 'fetch', async () => ({
    ok: false, status: 504,
    json: async () => ({ code: 'LLM_PROVIDER_TIMEOUT', detail: 'El proveedor no respondió a tiempo.' }),
  }));
  await assert.rejects(generateDiagram('Proceso ficticio', 'openrouter'), /El proveedor no respondió a tiempo/);
});

test('conserva el código cuando el texto no describe un proceso', async (t) => {
  setup(t);
  t.mock.method(globalThis, 'fetch', async () => ({
    ok: false, status: 422,
    json: async () => ({
      code: 'NOT_A_PROCESS_DESCRIPTION',
      detail: 'El texto no describe un proceso.',
    }),
  }));

  await assert.rejects(
    generateDiagram('Texto sin proceso', 'openrouter'),
    (error) => error.code === 'NOT_A_PROCESS_DESCRIPTION'
      && error.status === 422
      && error.message === 'El texto no describe un proceso.',
  );
});

test('la consulta de proveedores sigue limitada a 15 segundos', async (t) => {
  setup(t);
  t.mock.method(globalThis, 'fetch', (_url, { signal }) => aborted(signal));
  const rejected = assert.rejects(getProviders(), /tardó demasiado/);
  t.mock.timers.tick(15_000);
  await rejected;
});

test('un cuerpo de error bloqueado también termina por timeout', async (t) => {
  setup(t);
  let bodyStarted;
  const started = new Promise((resolve) => { bodyStarted = resolve; });
  t.mock.method(globalThis, 'fetch', async (_url, { signal }) => ({
    ok: false, status: 502,
    json: () => { bodyStarted(); return aborted(signal); },
  }));
  const result = generateDiagram('Proceso ficticio', 'openrouter');
  const rejected = assert.rejects(result, /tardó demasiado/);
  await started;
  t.mock.timers.tick(150_000);
  await rejected;
});
