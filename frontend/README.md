# SalidaSegura

Frontend responsivo y accesible para la gestión universitaria de emergencias. Puede correr en **modo conectado** al backend Java (con `VITE_SALIDASEGURA_API_URL`) o en **modo demostración** (sin ella). La simulación de alarma no se conecta a alarmas físicas.

## Ejecutar

```bash
pnpm install
pnpm dev                                   # modo demostración (sin backend)
```

Modo conectado (el backend debe permitir este origen por CORS; ver el README de la raíz, sección 7):

```bash
cp .env.example .env.local                 # ajusta VITE_SALIDASEGURA_API_URL si hace falta
pnpm dev --host localhost --port 8443
```

Pruebas: `pnpm typecheck` y `pnpm test` (Node 22+, sin dependencias nuevas). Con `SALIDASEGURA_CLASSPATH=../java/target/classes pnpm test` se prueba también contra el backend Java real.

## Integración con el backend Java

- Frontera: `EmergencyService` (`src/services/emergencyService.ts`). Implementaciones: `demoEmergencyService` y `createHttpEmergencyService` (`httpEmergencyService.ts`).
- `src/services/index.ts` elige el modo según `VITE_SALIDASEGURA_API_URL`. Si el backend falla, la interfaz muestra el error; no cambia a datos demo por su cuenta.
- El frontend no implementa el árbol B+, Counting Sort, el grafo, la cola de prioridad ni Dijkstra: muestra totales, orden del reporte y rutas tal como los devuelve el backend. `src/lib/graphLayout.ts` solo calcula posiciones para dibujar.
- En modo conectado, la barra lateral muestra el selector «Conjunto de datos» (`getDatasets` / `loadDataset`): pide al backend cambiar de conjunto y recarga las vistas.
- Los errores se tipan con `ServiceError` (`connection`, `api`, `response`, `configuration`).

## Estados y accesibilidad

Las vistas incluyen estados de carga, vacío, error y completado; navegación por teclado, foco visible, etiquetas semánticas y una alternativa textual para el grafo. El directorio pagina las salas para admitir alas de gran tamaño sin crear una página interminable.
