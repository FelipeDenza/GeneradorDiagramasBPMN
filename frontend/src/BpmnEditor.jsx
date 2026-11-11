import { useEffect, useRef, useState } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import { layoutProcess } from 'bpmn-auto-layout';

import { exportDiagram, openDiagramPreview } from './diagramExport.js';

import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';

export default function BpmnEditor({ diagramXml, generationVersion, onError, onNewDiagram }) {
  const panelRef = useRef(null);
  const containerRef = useRef(null);
  const modelerRef = useRef(null);
  const onErrorRef = useRef(onError);
  const suppressDirtyRef = useRef(false);
  const [isImporting, setIsImporting] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isDirty, setIsDirty] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [showNewConfirm, setShowNewConfirm] = useState(false);

  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(document.fullscreenElement === panelRef.current);
      window.requestAnimationFrame(() => modelerRef.current?.get('canvas').resized());
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  useEffect(() => {
    const modeler = new BpmnModeler({
      container: containerRef.current,
    });

    modelerRef.current = modeler;
    const eventBus = modeler.get('eventBus');
    const updateEditorState = () => {
      if (suppressDirtyRef.current) {
        return;
      }
      setIsDirty(true);
    };
    eventBus.on('commandStack.changed', updateEditorState);

    suppressDirtyRef.current = true;
    modeler.createDiagram()
      .then(() => {
        setIsDirty(false);
      })
      .catch(() => {
        onErrorRef.current('No se pudo iniciar el editor BPMN.');
      })
      .finally(() => {
        suppressDirtyRef.current = false;
      });

    return () => {
      eventBus.off('commandStack.changed', updateEditorState);
      modeler.destroy();
      modelerRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!diagramXml || !modelerRef.current) {
      return;
    }

    let cancelled = false;
    let scrollFrame;

    async function importDiagram() {
      setIsImporting(true);
      suppressDirtyRef.current = true;
      try {
        const laidOutXml = await layoutProcess(diagramXml);
        if (cancelled || !modelerRef.current) {
          return;
        }

        await modelerRef.current.importXML(laidOutXml);
        if (cancelled || !modelerRef.current) {
          return;
        }
        const canvas = modelerRef.current.get('canvas');
        canvas.zoom('fit-viewport', 'auto');
        setZoomLevel(Math.round(canvas.zoom() * 100));
        setIsDirty(false);
        scrollFrame = window.requestAnimationFrame(() => {
          panelRef.current?.scrollIntoView({
            behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
            block: 'center',
            inline: 'nearest',
          });
        });
      } catch {
        if (!cancelled) {
          onErrorRef.current('El diagrama generado no se pudo abrir.');
        }
      } finally {
        if (!cancelled) {
          setIsImporting(false);
        }
        suppressDirtyRef.current = false;
      }
    }

    importDiagram();

    return () => {
      cancelled = true;
      window.cancelAnimationFrame(scrollFrame);
    };
  }, [diagramXml, generationVersion]);

  async function handleExport(event) {
    const format = event.target.value;
    event.target.value = '';
    if (!format || !modelerRef.current) {
      return;
    }

    setIsExporting(true);
    try {
      await exportDiagram(modelerRef.current, format);
      if (format === 'bpmn') {
        setIsDirty(false);
      }
    } catch (error) {
      onErrorRef.current(error.message || 'No se pudo exportar el diagrama.');
    } finally {
      setIsExporting(false);
    }
  }

  async function handleOpenPreview() {
    if (!modelerRef.current) {
      return;
    }

    setIsExporting(true);
    try {
      await openDiagramPreview(modelerRef.current);
    } catch (error) {
      onErrorRef.current(error.message || 'No se pudo abrir la vista del diagrama.');
    } finally {
      setIsExporting(false);
    }
  }

  function handleNewDiagram() {
    if (isDirty) {
      setShowNewConfirm(true);
      return;
    }
    onNewDiagram();
  }

  function confirmNewDiagram() {
    setShowNewConfirm(false);
    onNewDiagram();
  }

  async function handleToggleFullscreen() {
    try {
      if (document.fullscreenElement === panelRef.current) {
        await document.exitFullscreen();
      } else {
        await panelRef.current?.requestFullscreen();
      }
    } catch {
      onErrorRef.current('No se pudo cambiar la vista del editor.');
    }
  }

  function handleZoom(delta) {
    if (!modelerRef.current) {
      return;
    }

    const canvas = modelerRef.current.get('canvas');
    const nextZoom = Math.min(2, Math.max(0.3, canvas.zoom() + delta));
    canvas.zoom(nextZoom);
    setZoomLevel(Math.round(nextZoom * 100));
  }

  return (
    <section ref={panelRef} className="editor-panel" aria-label="Editor de diagrama BPMN">
      <header className="editor-toolbar">
        <div className="editor-heading">
          <div className="editor-heading__title">
            <h2>Editor BPMN</h2>
          </div>
        </div>
        <div className="editor-actions">
          <button
            className="editor-icon-button"
            type="button"
            onClick={handleToggleFullscreen}
            disabled={isImporting || isExporting}
            aria-label={isFullscreen ? 'Salir de pantalla completa' : 'Ver en pantalla completa'}
            title={isFullscreen ? 'Salir de pantalla completa' : 'Ver en pantalla completa'}
          >
            <FullscreenIcon isActive={isFullscreen} />
          </button>
          <button
            className="editor-icon-button"
            type="button"
            onClick={handleOpenPreview}
            disabled={isImporting || isExporting}
            aria-label="Abrir en otra ventana"
            title="Abrir en otra ventana"
          >
            <ExternalLinkIcon />
          </button>
          <span className="editor-actions__divider" aria-hidden="true" />
          <button className="button button--tertiary editor-new-button" type="button" onClick={handleNewDiagram} disabled={isImporting || isExporting}>
            Nuevo
          </button>
          <select
            className="button button--secondary export-select"
            defaultValue=""
            onChange={handleExport}
            disabled={isImporting || isExporting}
            aria-label="Exportar diagrama"
          >
            <option value="" disabled>{isExporting ? 'Exportando' : 'Exportar'}</option>
            <option value="bpmn">BPMN editable (.bpmn)</option>
            <option value="svg">Imagen SVG (.svg)</option>
            <option value="png">Imagen PNG (.png)</option>
            <option value="jpeg">Imagen JPEG (.jpg)</option>
          </select>
        </div>
      </header>
      <div className="editor-workspace">
        <div ref={containerRef} className="bpmn-canvas" />
        <div className="zoom-controls" aria-label="Controles de zoom">
          <button type="button" onClick={() => handleZoom(-0.1)} aria-label="Alejar">−</button>
          <output aria-live="polite">{zoomLevel}%</output>
          <button type="button" onClick={() => handleZoom(0.1)} aria-label="Acercar">+</button>
        </div>
        {isImporting && (
          <div className="editor-loading" role="status" aria-live="polite">
            <span className="spinner spinner--dark" aria-hidden="true" />
            Preparando diagrama…
          </div>
        )}
      </div>

      {showNewConfirm && (
        <div className="confirm-overlay" onClick={() => setShowNewConfirm(false)}>
          <div className="confirm-dialog" onClick={e => e.stopPropagation()}>
            <p>¿Seguro que quieres borrar el diagrama actual?</p>
            <div className="confirm-dialog__actions">
              <button className="button button--tertiary" type="button" onClick={() => setShowNewConfirm(false)}>Cancelar</button>
              <button className="button button--primary" type="button" onClick={confirmNewDiagram}>Sí, borrar</button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function FullscreenIcon({ isActive }) {
  return isActive ? (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M8 3v5H3M16 3v5h5M8 21v-5H3M16 21v-5h5" />
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5" />
    </svg>
  );
}

function ExternalLinkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M14 4h6v6M20 4l-9 9M19 13v6a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h6" />
    </svg>
  );
}

