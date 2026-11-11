const FILE_BASENAME = 'diagrama-bpmn';
const MAX_RASTER_DIMENSION = 8192;

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function getSvg(modeler) {
  const { svg } = await modeler.saveSVG();
  if (!svg) {
    throw new Error('No se pudo obtener una imagen del diagrama.');
  }
  return svg;
}

export async function exportDiagram(modeler, format) {
  if (format === 'bpmn') {
    const { xml } = await modeler.saveXML({ format: true });
    downloadBlob(new Blob([xml], { type: 'application/xml;charset=utf-8' }), `${FILE_BASENAME}.bpmn`);
    return;
  }

  const svg = await getSvg(modeler);
  if (format === 'svg') {
    downloadBlob(new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }), `${FILE_BASENAME}.svg`);
    return;
  }

  if (format === 'png' || format === 'jpeg') {
    const blob = await svgToRaster(svg, format);
    const extension = format === 'jpeg' ? 'jpg' : 'png';
    downloadBlob(blob, `${FILE_BASENAME}.${extension}`);
    return;
  }

  throw new Error('El formato de exportación no es válido.');
}

export async function openDiagramPreview(modeler) {
  const previewWindow = window.open('', '_blank');
  if (!previewWindow) {
    throw new Error('El navegador bloqueó la nueva ventana. Habilitá las ventanas emergentes para continuar.');
  }

  previewWindow.opener = null;
  previewWindow.document.title = 'Vista del diagrama BPMN';
  const backgroundColor = getThemeValue('--bg');
  const surfaceColor = getThemeValue('--surface');
  const shadowColor = getThemeValue('--shadow-color');
  previewWindow.document.body.style.cssText = [
    'margin: 0',
    'min-height: 100vh',
    'display: grid',
    'place-items: center',
    'overflow: auto',
  ].join(';');
  previewWindow.document.body.style.background = backgroundColor;

  try {
    const svg = await getSvg(modeler);
    const url = URL.createObjectURL(new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }));
    const image = previewWindow.document.createElement('img');
    image.alt = 'Diagrama BPMN';
    image.style.cssText = 'display:block;max-width:none;margin:24px';
    image.style.background = surfaceColor;
    image.style.boxShadow = `0 8px 24px ${shadowColor}`;
    image.addEventListener('load', () => URL.revokeObjectURL(url), { once: true });
    image.src = url;
    previewWindow.document.body.appendChild(image);
  } catch (error) {
    previewWindow.close();
    throw error;
  }
}

async function svgToRaster(svg, format) {
  const dimensions = readSvgDimensions(svg);
  const scale = Math.min(
    2,
    MAX_RASTER_DIMENSION / dimensions.width,
    MAX_RASTER_DIMENSION / dimensions.height,
  );
  const width = Math.max(1, Math.round(dimensions.width * scale));
  const height = Math.max(1, Math.round(dimensions.height * scale));
  const svgUrl = URL.createObjectURL(new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }));

  try {
    const image = await loadImage(svgUrl);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('El navegador no permite preparar la imagen.');
    }

    if (format === 'jpeg') {
      context.fillStyle = getThemeValue('--surface');
      context.fillRect(0, 0, width, height);
    }
    context.drawImage(image, 0, 0, width, height);

    const mimeType = format === 'jpeg' ? 'image/jpeg' : 'image/png';
    return await canvasToBlob(canvas, mimeType, format === 'jpeg' ? 0.92 : undefined);
  } finally {
    URL.revokeObjectURL(svgUrl);
  }
}

function getThemeValue(propertyName) {
  return getComputedStyle(document.documentElement).getPropertyValue(propertyName).trim();
}

function readSvgDimensions(svg) {
  const documentNode = new DOMParser().parseFromString(svg, 'image/svg+xml');
  const svgNode = documentNode.documentElement;
  if (svgNode.nodeName === 'parsererror') {
    throw new Error('No se pudo interpretar la imagen del diagrama.');
  }

  const viewBox = (svgNode.getAttribute('viewBox') ?? '')
    .trim()
    .split(/[\s,]+/)
    .map(Number);
  const viewBoxWidth = viewBox.length === 4 ? viewBox[2] : 0;
  const viewBoxHeight = viewBox.length === 4 ? viewBox[3] : 0;
  const width = Number.parseFloat(svgNode.getAttribute('width')) || viewBoxWidth;
  const height = Number.parseFloat(svgNode.getAttribute('height')) || viewBoxHeight;

  if (!(width > 0) || !(height > 0)) {
    throw new Error('El diagrama no tiene un tamaño válido para exportar.');
  }
  return { width, height };
}

function loadImage(url) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.addEventListener('load', () => resolve(image), { once: true });
    image.addEventListener('error', () => reject(new Error('No se pudo preparar la imagen del diagrama.')), { once: true });
    image.src = url;
  });
}

function canvasToBlob(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) {
        resolve(blob);
      } else {
        reject(new Error('No se pudo generar el archivo de imagen.'));
      }
    }, type, quality);
  });
}
