import { useCallback, useEffect, useRef, useState } from 'react';

import BpmnEditor from './BpmnEditor.jsx';
import { generateDiagram, getProviders } from './api.js';

const MAX_PROMPT_LENGTH = 4000;
const PROMPT_WARNING_THRESHOLD = 3600;

const PROMPT_EXAMPLES = [
  {
    label: 'Proceso lineal',
    prompt: 'Modelá el alta de un cliente: el asesor recibe los datos, valida la documentación, registra al cliente en el sistema y envía la confirmación.',
  },
  {
    label: 'Con decisión',
    prompt: 'Modelá la aprobación de una compra. El solicitante carga el pedido y su responsable lo revisa. Si lo aprueba, Compras emite la orden; si lo rechaza, se notifica el motivo. En ambos casos el proceso termina.',
  },
  {
    label: 'Con paralelismo',
    prompt: 'Modelá el ingreso de una persona a la empresa. Después de firmar el contrato, en paralelo Sistemas crea sus accesos y Administración prepara el legajo. Cuando ambas tareas terminan, RR. HH. confirma el alta.',
  },
];

export default function App() {
  const formRef = useRef(null);
  const promptRef = useRef(null);
  const [prompt, setPrompt] = useState('');
  const [providers, setProviders] = useState([]);
  const [selectedProvider, setSelectedProvider] = useState('');
  const [diagramXml, setDiagramXml] = useState('');
  const [generationVersion, setGenerationVersion] = useState(0);
  const [editorKey, setEditorKey] = useState(0);
  const [providerReloadKey, setProviderReloadKey] = useState(0);
  const [isLoadingProviders, setIsLoadingProviders] = useState(true);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState('');
  const [errorAction, setErrorAction] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setIsLoadingProviders(true);

    getProviders()
      .then((availableProviders) => {
        if (cancelled) {
          return;
        }
        setProviders(availableProviders);
        setSelectedProvider((currentProvider) => (
          availableProviders.some((provider) => provider.id === currentProvider)
            ? currentProvider
            : (availableProviders[0]?.id ?? '')
        ));
        setError('');
        setErrorAction(null);
      })
      .catch(() => {
        if (!cancelled) {
          setProviders([]);
          setSelectedProvider('');
          setError('No pudimos conectar con el backend. Verificá que la aplicación esté iniciada.');
          setErrorAction('providers');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingProviders(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [providerReloadKey]);

  const showEditorError = useCallback((message) => {
    setError(message);
    setErrorAction(null);
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    const cleanPrompt = prompt.trim();

    if (!cleanPrompt) {
      setError('Escribí una descripción del proceso o elegí uno de los ejemplos.');
      setErrorAction(null);
      promptRef.current?.focus();
      return;
    }
    if (prompt.length > MAX_PROMPT_LENGTH) {
      setError(`El prompt supera el límite por ${prompt.length - MAX_PROMPT_LENGTH} caracteres.`);
      setErrorAction(null);
      promptRef.current?.focus();
      return;
    }
    if (!selectedProvider) {
      setError('No hay un modelo disponible. Revisá la configuración de proveedores.');
      setErrorAction('providers');
      return;
    }

    setError('');
    setErrorAction(null);
    setIsGenerating(true);
    try {
      const result = await generateDiagram(cleanPrompt, selectedProvider);
      setDiagramXml(result.bpmnXml);
      setGenerationVersion((version) => version + 1);
    } catch (requestError) {
      setError(requestError.message);
      setErrorAction(requestError.code === 'NOT_A_PROCESS_DESCRIPTION' ? null : 'generate');
    } finally {
      setIsGenerating(false);
    }
  }

  function handlePromptKeyDown(event) {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      event.currentTarget.form?.requestSubmit();
    }
  }

  function handleExampleClick(examplePrompt) {
    setPrompt(examplePrompt);
    setError('');
    setErrorAction(null);
    requestAnimationFrame(() => promptRef.current?.focus());
  }

  function handleRetry() {
    if (errorAction === 'providers') {
      setProviderReloadKey((currentKey) => currentKey + 1);
      return;
    }
    if (errorAction === 'generate') {
      formRef.current?.requestSubmit();
    }
  }

  function handleNewDiagram() {
    setDiagramXml('');
    setError('');
    setErrorAction(null);
    setEditorKey((currentKey) => currentKey + 1);
  }

  const hasProviders = providers.length > 0;
  const promptIsOverLimit = prompt.length > MAX_PROMPT_LENGTH;
  const promptIsNearLimit = prompt.length >= PROMPT_WARNING_THRESHOLD && !promptIsOverLimit;
  const counterClassName = [
    'character-count',
    promptIsNearLimit ? 'character-count--warning' : '',
    promptIsOverLimit ? 'character-count--error' : '',
  ].filter(Boolean).join(' ');
  const canGenerate = Boolean(prompt.trim())
    && !promptIsOverLimit
    && hasProviders
    && !isLoadingProviders
    && !isGenerating;

  return (
    <div className="app-page">
      <header className="site-header">
        <div className="site-header__content">
          <div className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32">
              <circle cx="6" cy="16" r="3" />
              <rect x="13" y="11" width="8" height="10" rx="2" />
              <circle cx="27" cy="16" r="3" />
              <path d="M9 16h4M21 16h3" />
            </svg>
          </div>
          <div>
            <h1>Generador BPMN</h1>
          </div>
        </div>
      </header>

      <main className="app-shell">
        <section className="prompt-card card" aria-labelledby="prompt-title">
          <div className="section-heading">
            <div>
              <h2 id="prompt-title">Describí tu proceso</h2>
            </div>
          </div>

          <form ref={formRef} onSubmit={handleSubmit} noValidate>
            <label htmlFor="process-prompt" className="sr-only">Descripción del proceso</label>
            <textarea
              ref={promptRef}
              id="process-prompt"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              onKeyDown={handlePromptKeyDown}
              placeholder="Ej.: El proceso comienza cuando un cliente envía un pedido. Ventas valida los datos y, si están completos, confirma la compra…"
              disabled={isGenerating}
              aria-describedby="prompt-counter"
              aria-invalid={promptIsOverLimit}
            />

            <div className="prompt-examples" aria-label="Ejemplos de prompts">
              <span>Probá un ejemplo:</span>
              <div className="chip-list">
                {PROMPT_EXAMPLES.map((example) => (
                  <button
                    key={example.label}
                    type="button"
                    className="prompt-chip"
                    onClick={() => handleExampleClick(example.prompt)}
                    disabled={isGenerating}
                  >
                    {example.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="prompt-controls">
              <div className="prompt-controls__left">
                <div className="field-group model-field">
                  <label htmlFor="provider-select" className="field-label">Modelo</label>
                  <select
                    id="provider-select"
                    value={selectedProvider}
                    onChange={(event) => setSelectedProvider(event.target.value)}
                    disabled={isLoadingProviders || !hasProviders || isGenerating}
                  >
                    {!hasProviders && (
                      <option value="">
                        {isLoadingProviders ? 'Buscando modelos…' : 'Sin modelos disponibles'}
                      </option>
                    )}
                    {providers.map((provider) => (
                      <option key={provider.id} value={provider.id}>{provider.displayName}</option>
                    ))}
                  </select>
                </div>

                <div id="prompt-counter" className={counterClassName} aria-live="polite">
                  <strong>{prompt.length.toLocaleString('es-AR')}</strong>
                  <span> / {MAX_PROMPT_LENGTH.toLocaleString('es-AR')}</span>
                  {promptIsOverLimit && <small>Reducí {prompt.length - MAX_PROMPT_LENGTH} caracteres</small>}
                </div>
              </div>

              <button className="button button--primary generate-button" type="submit" disabled={!canGenerate}>
                {isGenerating && <span className="spinner" aria-hidden="true" />}
                {isGenerating ? 'Generando…' : 'Generar diagrama'}
              </button>
            </div>

            <span className="sr-only" aria-live="polite">
              {isGenerating ? 'Generando el diagrama, esperá un momento.' : ''}
            </span>
          </form>
        </section>

        {error && (
          <div className="error-banner" role="alert" aria-live="assertive">
            <span className="error-banner__icon" aria-hidden="true">!</span>
            <div>
              <strong>No pudimos completar la acción</strong>
              <p>{error}</p>
            </div>
            {errorAction && (
              <button type="button" onClick={handleRetry} disabled={isGenerating || isLoadingProviders}>
                Reintentar
              </button>
            )}
          </div>
        )}

        <BpmnEditor
          key={editorKey}
          diagramXml={diagramXml}
          generationVersion={generationVersion}
          onError={showEditorError}
          onNewDiagram={handleNewDiagram}
        />
      </main>

      <footer className="site-footer">
        <span>Challenge técnico · Felipe Denza</span>
        <span className="site-footer__separator" aria-hidden="true">·</span>
        <span className="repository-placeholder">Repositorio · próximamente</span>
      </footer>
    </div>
  );
}
