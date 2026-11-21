# Registro de prompts

- Integrantes: Fran
- Herramienta(s) de IA utilizada(s): Sonnet 5.5

## Prompt 1
(pegue aquí el texto exacto del prompt)

## Prompt 2
...

## Prompt 3

- Herramienta: Sonnet 5.5

````text
Implement the university emergency system “SalidaSegura” in the existing repository. Use Java 17 or later. Treat every requirement below as fixed: do not propose alternatives, redesign, omit, weaken, or change requirements, even if another approach seems better. Do not add unrequested features.

Before coding, inspect the repository and the CSV files in `datos/`. Use the CSVs as the data examples and source of truth for their actual schema; do not assume or replace their format. Do not use any other exam context beyond these requirements and the supplied data. If a necessary detail is genuinely ambiguous, choose the smallest deterministic interpretation consistent with these requirements and document it without changing them.

## 1. B+ tree — institutional hierarchy

Implement the hierarchy as one B+ tree with exactly four logical levels:
- Level 0: a single root node linking the institution’s hierarchy.
- Level 1: guide nodes identifying buildings.
- Level 2: guide nodes identifying each building’s wings.
- Level 3: leaf nodes containing a fixed-capacity array of 27 room slots for each wing.

Implement the tree and its node, guide, and leaf structures yourself. Use arrays, custom classes, primitive types, and `String`; do not use Java collection classes. The 27 room slots represent the Spanish alphabet: A through Z, including Ñ in its Spanish alphabetical position. Each room stores its letter/code and integer occupant count. Support buildings and wings represented by the supplied data while preserving this four-level structure.

Provide these operations:
1. Return the room array associated with a given wing.
2. Update the occupants of a specified room.
3. Return the total occupants of a wing or building subtree.

Show the requested counts by wing and building. Handle missing buildings, wings, and rooms, invalid occupant values, and other invalid inputs explicitly; do not silently ignore errors.

## 2. Simulated alarm and brigade report

There is no physical alarm device. Provide a console command/input that simulates receiving the alarm signal. Each time it is received, gather all university rooms and display them in descending order of occupant count using a self-implemented Counting Sort.

The sort must be stable: rooms with equal occupant counts retain their original deterministic input order, preserving each wing array’s Spanish alphabetical order. Define and use a deterministic traversal order for buildings and wings, based on the supplied data. Do not add a different tie-break rule.

The exam data includes a brigade report of 400 rooms and a maximum of 80 occupants per room. The report must finish in less than one second in the worst case, not merely on average. Use the bounded occupant range to implement Counting Sort with linear-time behavior; do not substitute a comparison sort. Include a focused performance check for the specified workload and explain the worst-case time and space costs. Do not claim a hardware-independent timing guarantee that the implementation cannot demonstrate.

## 3. Evacuation graph and route guidance

Implement a self-contained, undirected, weighted, sparse graph for relevant points in a building, including rooms, corridors, and exits. Edge weights are walking time in seconds. Make the model expandable for the supplied CSV data and future buildings, points, and exits.

Implement the graph as an adjacency list and implement its priority queue yourself using arrays/custom classes. Do not use graph, shortest-path, or priority-queue libraries. Use Dijkstra’s algorithm to identify, for every room, the fastest route to any available exit: three exits in the supplied scenario, with support for N exits. Display the selected exit, route, and total walking time for each of the six sample rooms.

Support blocked corridors by removing the corresponding edge before running Dijkstra. Because the graph is undirected, remove the edge in both adjacency directions. A route must never include a blocked edge. If no exit is reachable from a room, report that explicitly as “Bloqueado” (or an equally clear Spanish message); do not return a fabricated route.

## Fixed implementation constraints

- Use Java 17+ only.
- Implement all required data structures and algorithms from scratch.
- Allowed for implementation: arrays, primitive types, custom classes, and `String`.
- Do not use Java collections, including `ArrayList`, `LinkedList`, `HashMap`, `HashSet`, `TreeMap`, `TreeSet`, `PriorityQueue`, `Stack`, or `ArrayDeque`.
- Do not use `Arrays.sort`, `Collections.sort`, `List.sort`, or stream `sorted()`.
- Do not use third-party graph, pathfinding, sorting, or collection libraries.
- Use only the core JDK as needed for console and CSV file I/O, plus JUnit for tests; add no other dependencies.
- Keep the tree and graph structures expandable/modifiable without changing their required logical behavior.
- Keep all user-facing output and README content in Spanish unless the repository’s existing conventions require otherwise.
- Do not suggest improvements or alter the design or requirements.

## Required workflow and deliverables

Use TDD: first write focused tests that fail for the missing behavior, then implement the smallest complete solution, and rerun the tests. Test every functional requirement, including exact expected results, edge cases, ties/stability, blocked and unreachable routes, invalid inputs, CSV loading, and the specified 400-room/80-occupant workload. Ensure any timing checks are reproducible and do not rely only on average-case performance.

Deliver working source code, unit tests, `README.md`, and `PROMPTS.md` at the repository root. The README must include:
- The exam-project name and authors.
- The complete original requirements verbatim, without edits.
- Installation, execution, and test instructions.
- The folder/module/class hierarchy.
- Which requirements were implemented or excluded and why.
- Mermaid diagrams for program flow, components, and the graph.

Append this entire prompt verbatim to `PROMPTS.md` in its chronological position and identify the tool used as “Sonnet 5.5”. Preserve existing prompt-log entries and do not rewrite earlier entries. Do not change any locked requirements while implementing.
````

## Prompt Figma

- Herramienta: Figma

````text
Create a responsive, accessible frontend for “SalidaSegura,” a university emergency-management system. The interface must be in Spanish. Build it in Figma Make (or the available Figma code-generation tool) as a usable frontend, not just static mockups.

First inspect any available project files and existing API contracts. Preserve the existing backend and its requirements. The frontend must not reimplement or replace the Java B+ tree, stable Counting Sort, graph, priority queue, or Dijkstra algorithm: request those operations from the backend. Do not invent API endpoints or claim data is live. If no backend/API is available, use clearly labeled demo data behind a replaceable service adapter and document the expected integration contract.

## Main experience

Create a clear emergency operations dashboard with navigation for:
1. Directorio
2. Alarma y reporte
3. Evacuación

Use a calm, high-contrast operational design: neutral surfaces, legible typography, and restrained color. Reserve red/orange for emergency states, green for open/safe states, and gray for blocked or unavailable items. Do not rely on color alone to convey status. Include keyboard-accessible controls, visible focus states, semantic labels, and responsive layouts for desktop, tablet, and mobile.

### Directorio
- Present the institution’s hierarchy as buildings → wings → rooms.
- Allow users to expand a building and wing, and view the rooms in that wing in Spanish alphabetical order, including Ñ.
- Show each room’s code and occupant count.
- Provide the total occupants for a wing and building.
- Provide a room-occupant update form with validation and clear success/error feedback.
- Make the room list usable when there are up to 400 rooms; avoid requiring the user to scroll through one enormous page.

### Alarma y reporte
- Provide an explicit Simular alarma control. Make clear this simulates a signal; it is not connected to a physical alarm device.
- When activated, show the brigade report ordered from highest to lowest occupants, preserving the backend’s stable tie order. Do not sort or reorder the report in the frontend.
- Display room code, building, wing, and occupants. Support browsing the full report without altering its order.
- Show loading, empty, error, and completed states.

### Evacuación
- Display a clear, interactive schematic graph of rooms, corridors, and exits. Identify corridor walking times and open/blocked status.
- Show the fastest route for each room, the selected exit, and total walking time returned by the backend.
- Support three exits and a variable number of exits.
- Provide a control to block or unblock a corridor. Communicate that blocking a corridor removes that connection from route calculations. Refresh route guidance using backend results.
- Clearly show when no exit is reachable; never display a fabricated route.
- Include a room selector and a concise route-details panel. Ensure the graph remains understandable on small screens, with an accessible text alternative for its nodes, edges, and statuses.

## Implementation and handoff
- Reuse the repository’s existing frontend stack and conventions if present. Keep the frontend separate from, and do not weaken or modify, the Java algorithm requirements.
- Put API calls behind a small service/data adapter so live backend integration and demo data are clearly separated.
- Include realistic loading, validation, empty, unavailable, and error states; do not silently fall back from live data to demo data.
- Use the supplied CSV data only if it is available. Label any sample or mocked information as demo data.
- Include concise run instructions and document any assumed API contract.
- If working in the project repository, append this prompt verbatim to `PROMPTS.md` in chronological order, identifying the tool as “Figma”; preserve all existing entries.

````

## Prompt 4

- Herramienta: Sonnet 5.5

````text
Conecta el backend existente de “SalidaSegura” con el frontend diseñado en Figma.

FIGMA_WORK_URL: [PEGA AQUÍ EL ENLACE AL TRABAJO DE FIGMA]
PROJECT_REPOSITORY_OR_PATH: [PEGA AQUÍ EL ENLACE O LA RUTA DEL REPOSITORIO]

Inspecciona primero el repositorio y el enlace de Figma. Conserva el diseño y los requisitos existentes; no rehagas la interfaz ni modifiques los requisitos del examen. Si el enlace de Figma no es accesible o solo permite ver un prototipo, no supongas que contiene código exportable: indícalo y trabaja con el frontend disponible en el repositorio, usando Figma como referencia visual.

## Integración

1. Identifica el frontend y el backend existentes, sus tecnologías, cómo se ejecutan y qué interfaces o endpoints están realmente implementados. Basa la integración en el código y los contratos existentes; no inventes rutas, formatos ni respuestas.
2. Conecta el frontend al backend real para:
   - consultar edificios, alas, salas y ocupantes;
   - actualizar ocupantes y consultar totales por ala y edificio;
   - activar la alarma simulada y mostrar el reporte ordenado que devuelve el backend;
   - consultar rutas de evacuación, salidas, tiempos y estado de pasillos;
   - bloquear o desbloquear pasillos y actualizar las rutas.
3. No vuelvas a implementar algoritmos en el frontend. El árbol B+, Counting Sort estable, el grafo, la cola de prioridad y Dijkstra deben seguir ejecutándose en el backend. El frontend debe mostrar los resultados que este devuelva, sin volver a ordenar el reporte ni fabricar rutas.
4. Si el backend todavía no expone una interfaz de comunicación, determina la adaptación mínima necesaria para conectarlo, manteniendo las estructuras, algoritmos y restricciones ya implementadas. No agregues dependencias ni cambies el diseño para evitar esa adaptación. Si no es posible hacerlo respetando las restricciones existentes, explica el bloqueo con precisión en lugar de simular una conexión.
5. Separa las llamadas al backend en un servicio o adaptador pequeño. Mantén la URL de servicio configurable; no incluyas secretos ni credenciales en el código del frontend. Si hacen falta cambios de CORS o configuración del servidor, limítalos a lo estrictamente necesario.
6. Conserva el manejo claro de estados de carga, error, datos vacíos y conexión no disponible. No cambies silenciosamente a datos de demostración si falla el backend. Si el proyecto ya dispone de datos de demostración, mantenlos claramente identificados como tales y separados del modo conectado.

## Restricciones no negociables

- Respeta los requisitos del PDF, las decisiones ya fijadas y las restricciones tecnológicas del proyecto.
- Usa Java 17+ para el backend existente. No uses colecciones Java prohibidas, métodos de ordenamiento prohibidos ni bibliotecas externas de grafos, caminos mínimos o colas de prioridad. No sustituyas algoritmos o estructuras desarrollados por el proyecto.
- Preserva el comportamiento existente y evita cambios ajenos a la integración.
- No sugieras cambios de diseño ni alternativas a los requisitos. Si encuentras una contradicción o un bloqueo real, no lo resuelvas alterando un requisito: explica el conflicto y detente en ese punto.

## Validación y entrega

- Sigue TDD para cualquier comportamiento nuevo de integración: añade pruebas que fallen antes del cambio y luego verifica que pasen.
- Prueba las llamadas reales disponibles y los estados de error; verifica que el reporte conserva el orden del backend y que las rutas excluyen pasillos bloqueados.
- Actualiza las instrucciones de ejecución si cambia la configuración o el modo de iniciar los servicios.
- Añade este prompt íntegro a `PROMPTS.md` en orden cronológico e identifica la herramienta como “Sonnet 5.5”, sin borrar entradas previas.
- Al finalizar, resume qué conectaste, cómo ejecutar frontend y backend, qué pruebas corriste y cualquier limitación comprobada. Incluye el enlace de Figma proporcionado como referencia; no afirmes que la interfaz está desplegada ni generes un enlace de despliegue si no lo está.
````

## Prompt 5

- Herramienta: Sonnet 5.5

````text
Este es el zip del código de Figma, puedes conectarlo con el proyecto que acabas de entregar?
````

(Adjunto: `Build_SalidaSegura_Emergency_Dashboard.zip`, el código exportado de Figma Make.)

## Prompt 6

- Herramienta: Sonnet 5.5

````text
puedes crear algún archivo ejecutable que corra todo el proyecto al abrirse, tanto frontend como backend
````

## Prompt 7

- Herramienta: Sonnet 5.5

````text
puedes desarrollar un modelo más amplio con 400 salas en total, divididas en diferentes alas y edifcios para probar bien la apliación manualmente
````

## Prompt 8

- Herramienta: Sonnet 5.5

````text
puedes implementar algún botoón o función directamente en la página que cargue e modo grande de forma más accesible
````
