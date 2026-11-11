# BPMN Prompt Backend

Backend del challenge técnico para generar procesos BPMN a partir de una descripción en lenguaje natural.

## Estado actual

La aplicación integra tres proveedores LLM:

- Google Gemini.
- Groq.
- OpenRouter.

Además incluye:

- API para generar diagramas y consultar proveedores disponibles.
- Editor BPMN manual disponible desde el inicio, sin necesidad de enviar un prompt.
- Exportación del estado actual como BPMN, SVG, PNG o JPEG, más una vista estática en otra ventana.
- Contrato común para adapters LLM.
- JSON Schema canónico del proceso.
- Parseo estricto de la respuesta del modelo.
- Clasificación semántica para rechazar textos que no describen un proceso sin inventar tareas.
- Validación semántica del grafo.
- Un intento de reparación si el modelo responde con una estructura inválida.
- Generación segura de BPMN XML sin coordenadas.
- Health check mediante Spring Boot Actuator.

Un proveedor solo aparece en `GET /api/v1/providers` cuando su API key está configurada.

## Arquitectura del backend

El backend usa una estructura por capas visible desde los paquetes:

```text
com.desafiotecnicoimr.bpmn
├── controller       endpoints REST y manejo de errores HTTP
│   └── dto          contratos de entrada y salida de la API
├── application      caso de uso y orquestación de la generación
├── domain           modelo y reglas de validación del proceso
├── llm              contrato, registro y adapters de proveedores LLM
├── bpmn             transformación del modelo validado a BPMN XML
└── config           configuración técnica de Spring
```

El flujo principal es `controller → application → domain/llm/bpmn`. La capa de aplicación representa el único caso de uso sin agregar una capa `service` innecesaria. Los controllers no conocen detalles HTTP de los proveedores y los adapters LLM no conocen la API web.

## Requisitos

- Java 21
- Maven 3.8 o superior, o Maven Wrapper incluido
- Node.js 20 o superior y npm

## Ejecutar tests

En Windows:

```powershell
.\mvnw.cmd test
```

Con Maven instalado:

```powershell
mvn test
```

## Ejecutar la aplicación

Copiar `.env.example` como `.env` y configurar al menos una API key:

```properties
GEMINI_API_KEY=tu-api-key
GROQ_API_KEY=tu-api-key
OPENROUTER_API_KEY=tu-api-key
```

No se deben usar comillas alrededor de las claves. El archivo `.env` está ignorado por Git y Spring lo carga tanto al iniciar desde la carpeta del backend como desde la raíz del proyecto en IntelliJ. No es necesario configurar las tres; el frontend mostrará únicamente las que estén disponibles.

Para construir el monolito (frontend y backend dentro de un único JAR):

```powershell
.\mvnw.cmd clean package
java -jar .\target\bpmn-prompt-backend-0.0.1-SNAPSHOT.jar
```

La aplicación completa queda disponible en `http://localhost:8080`. Maven ejecuta `npm ci`, construye React y empaqueta el resultado como recursos estáticos de Spring Boot.

Para desarrollo con recarga automática se pueden ejecutar Spring Boot y Vite por separado:

```powershell
.\mvnw.cmd spring-boot:run
```

```powershell
cd ..\frontend
npm run dev
```

En ese modo, Vite queda disponible en `http://localhost:5173` y redirige las llamadas `/api` al backend en el puerto `8080`.

## Endpoints

### Estado

```http
GET /actuator/health
```

### Proveedores configurados

```http
GET /api/v1/providers
```

### Generar un diagrama

```http
POST /api/v1/diagrams/generate
Content-Type: application/json
```

```json
{
  "prompt": "Modelar el proceso de aprobación de vacaciones",
  "provider": "provider-id"
}
```

La respuesta exitosa contiene el BPMN XML semántico. El frontend será responsable de aplicar auto-layout antes de importarlo en `bpmn-js`.

Si el texto no describe un flujo operativo, la API responde `422` con el código `NOT_A_PROCESS_DESCRIPTION`. Para considerarse un proceso debe incluir actividades o eventos orientados a un resultado; hechos, opiniones, preguntas y frases inconexas se rechazan.

Cada llamada al proveedor tiene 10 segundos de espera de conexión y 60 segundos de inactividad de lectura. Si se supera ese tiempo, la API devuelve `504` con `LLM_PROVIDER_TIMEOUT`; no reintenta automáticamente un timeout. La reparación se intenta solamente si llegó una respuesta inválida.

El frontend espera hasta 150 segundos por la generación completa (incluida la posible reparación), también durante la descarga del JSON. Esta espera es un límite total del navegador; el límite de lectura del backend mide inactividad, por lo que un proveedor que envía datos de forma intermitente puede tardar más. Los proveedores gratuitos pueden tener demoras variables.

## Alcance BPMN inicial

- Evento de inicio.
- Evento de fin.
- Tarea genérica.
- Gateway exclusivo.
- Gateway paralelo.
- Sequence flows con nombre opcional.

## Agregar un proveedor

Un adapter debe implementar `LlmProvider` y registrarse como bean de Spring. El registro lo detecta automáticamente; el resto del flujo no depende del SDK ni de la API concreta del proveedor.

## Configuración opcional

Los modelos predeterminados pueden reemplazarse mediante variables de entorno:

```text
GEMINI_MODEL=gemini-3.8-flash
GROQ_MODEL=openai/gpt-oss-20b
OPENROUTER_MODEL=openrouter/free
```

Las claves nunca se envían al frontend ni deben almacenarse en el repositorio.
