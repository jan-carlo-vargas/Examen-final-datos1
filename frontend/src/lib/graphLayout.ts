// Trazado visual del grafo. Solo posiciona puntos para dibujarlos: no calcula rutas
// (las rutas siempre las entrega el backend).

export type NodeKind = 'room' | 'exit' | 'junction'

export interface LayoutNode {
  id: string
  label: string
  kind: NodeKind
  x: number
  y: number
  width: number
}

export interface GraphLayout {
  nodes: LayoutNode[]
  width: number
  height: number
}

const ROW_GAP = 60
const COLUMN_GAP = 170
const MARGIN_X = 70
const WIDTHS: Record<NodeKind, number> = { room: 96, exit: 110, junction: 64 }

/** Columnas por distancia (en tramos) a la salida más cercana: salidas a la derecha, salas a la izquierda. */
export function layoutGraph(
  roomIds: string[],
  exitIds: string[],
  corridors: { from: string; to: string }[],
): GraphLayout {
  const kinds = new Map<string, NodeKind>()
  for (const id of roomIds) kinds.set(id, 'room')
  for (const id of exitIds) kinds.set(id, 'exit')
  for (const c of corridors) {
    if (!kinds.has(c.from)) kinds.set(c.from, 'junction')
    if (!kinds.has(c.to)) kinds.set(c.to, 'junction')
  }
  const ids = [...kinds.keys()]

  const neighbours = new Map<string, string[]>(ids.map((id) => [id, []]))
  for (const c of corridors) {
    neighbours.get(c.from)!.push(c.to)
    neighbours.get(c.to)!.push(c.from)
  }

  // Recorrido por niveles desde todas las salidas.
  const depth = new Map<string, number>(exitIds.map((id) => [id, 0]))
  const queue = [...exitIds]
  for (let head = 0; head < queue.length; head++) {
    const current = queue[head]
    for (const next of neighbours.get(current)!) {
      if (!depth.has(next)) {
        depth.set(next, depth.get(current)! + 1)
        queue.push(next)
      }
    }
  }
  const reached = [...depth.values()]
  const farthest = reached.length ? Math.max(...reached) : 0
  // Sin conexión a una salida: columna más a la izquierda.
  const columnOf = (id: string) => (depth.has(id) ? farthest - depth.get(id)! : 0)

  const columns = new Map<number, string[]>()
  for (const id of ids) {
    const c = columnOf(id)
    if (!columns.has(c)) columns.set(c, [])
    columns.get(c)!.push(id)
  }
  const columnIndexes = [...columns.keys()].sort((a, b) => a - b)
  const tallest = Math.max(1, ...[...columns.values()].map((v) => v.length))
  const height = tallest * ROW_GAP + 40
  const lastColumn = columnIndexes.length ? columnIndexes[columnIndexes.length - 1] : 0
  const width = MARGIN_X * 2 + lastColumn * COLUMN_GAP

  const nodes: LayoutNode[] = []
  for (const c of columnIndexes) {
    const members = columns.get(c)!
    const top = (height - (members.length - 1) * ROW_GAP) / 2
    members.forEach((id, row) => {
      const kind = kinds.get(id)!
      nodes.push({ id, label: id, kind, x: MARGIN_X + c * COLUMN_GAP, y: top + row * ROW_GAP, width: WIDTHS[kind] })
    })
  }
  return { nodes, width, height }
}

/**
 * Salas que se dibujan en el esquema. Con campus grandes (más de `limit` salas) dibujarlas todas
 * lo vuelve ilegible, así que solo se dibujan la elegida y las que aparecen en la ruta recibida.
 */
export function visibleRoomIds(roomIds: string[], selected: string, path: string[], limit = 40): string[] {
  if (roomIds.length <= limit) return roomIds
  const keep = new Set([selected, ...path])
  return roomIds.filter((id) => keep.has(id))
}
