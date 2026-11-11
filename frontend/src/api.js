const API_URL = (import.meta.env?.VITE_API_URL ?? '').replace(/\/$/, '');
// Dos llamadas LLM de hasta 120 s (generación + reparación), más margen para el backend.
const DEFAULT_TIMEOUT_MS = 270_000;

async function request(path, options = {}, timeoutMs = DEFAULT_TIMEOUT_MS) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_URL}${path}`, {
      ...options,
      signal: controller.signal,
    });

    if (!response.ok) {
      const problem = await response.json().catch((error) => {
        if (error.name === 'AbortError') throw error;
        return null;
      });
      const requestError = new Error(problem?.detail ?? 'No se pudo completar la solicitud.');
      requestError.code = problem?.code;
      requestError.status = response.status;
      throw requestError;
    }

    return await response.json();
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('La solicitud tardó demasiado en responder. Probá nuevamente o elegí otro proveedor.');
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

export function getProviders() {
  return request('/api/v1/providers', {}, 15_000);
}

export function generateDiagram(prompt, provider) {
  return request('/api/v1/diagrams/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ prompt, provider }),
  });
}
