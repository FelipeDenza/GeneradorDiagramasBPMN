# BPMN Prompt Frontend

Interfaz React para describir un proceso, solicitar su generación al backend y editar el resultado con `bpmn-js`.

## Requisitos

- Node.js 20
- Backend ejecutándose en `http://localhost:8080`
- Al menos un proveedor LLM configurado en el backend

## Ejecutar localmente

```powershell
npm install
npm run dev
```

La aplicación queda disponible en `http://localhost:5173`. Vite redirige las llamadas `/api` al backend local.

## Compilar

```powershell
npm run build
```

## Verificar el auto-layout

```powershell
npm run test:layout
```

Esta comprobación toma BPMN XML semántico sin coordenadas, equivalente al generado por el backend, y verifica que `bpmn-auto-layout` agregue shapes y conexiones gráficas.

## Backend remoto

Para conectarse a una API desplegada, crear `.env.local`:

```text
VITE_API_URL=https://api.ejemplo.com
```

En ese caso, el backend debe permitir por CORS el origen donde se publique el frontend.
