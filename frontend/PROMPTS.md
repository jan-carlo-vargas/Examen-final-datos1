# Registro de prompts

## Figma

Create a responsive, accessible frontend for “SalidaSegura,” a university emergency-management system. The interface must be in Spanish. Build it in Figma Make (or the available Figma code-generation tool) as a usable frontend, not just static mockups.

First inspect any available project files and existing API contracts. Preserve the existing backend and its requirements. The frontend must not reimplement or replace the Java B+ tree, stable Counting Sort, graph, priority queue, or Dijkstra algorithm: request those operations from the backend. Do not invent API endpoints or claim data is live. If no backend/API is available, use clearly labeled demo data behind a replaceable service adapter and document the expected integration contract.

## Main experience

Create a clear emergency operations dashboard with navigation for:
1. \*\*Directorio\*\*
2. \*\*Alarma y reporte\*\*
3. \*\*Evacuación\*\*

Use a calm, high-contrast operational design: neutral surfaces, legible typography, and restrained color. Reserve red/orange for emergency states, green for open/safe states, and gray for blocked or unavailable items. Do not rely on color alone to convey status. Include keyboard-accessible controls, visible focus states, semantic labels, and responsive layouts for desktop, tablet, and mobile.

### Directorio
- Present the institution’s hierarchy as buildings → wings → rooms.
- Allow users to expand a building and wing, and view the rooms in that wing in Spanish alphabetical order, including Ñ.
- Show each room’s code and occupant count.
- Provide the total occupants for a wing and building.
- Provide a room-occupant update form with validation and clear success/error feedback.
- Make the room list usable when there are up to 400 rooms; avoid requiring the user to scroll through one enormous page.

### Alarma y reporte
- Provide an explicit \*\*Simular alarma\*\* control. Make clear this simulates a signal; it is not connected to a physical alarm device.
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
- If working in the project repository, append this prompt verbatim to \`PROMPTS.md\` in chronological order, identifying the tool as “Figma”; preserve all existing entries.
