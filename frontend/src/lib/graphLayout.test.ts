import test from 'node:test'
import assert from 'node:assert/strict'
import { layoutGraph } from './graphLayout.ts'

// Grafo de los CSV suministrados (solo para probar el trazado; los datos reales los entrega el backend).
const rooms = ['LAB-101', 'LAB-102', 'RM-103', 'RM-104', 'OFF-105', 'LIB-001']
const exits = ['Salida Norte', 'Salida Este', 'Salida Sur']
const edges: [string, string][] = [
  ['Salida Norte', 'Pasillo A'], ['Pasillo A', 'LAB-101'], ['Pasillo A', 'LAB-102'], ['LAB-101', 'LAB-102'],
  ['LAB-102', 'Pasillo B'], ['Pasillo B', 'RM-103'], ['Pasillo B', 'Salida Este'], ['Pasillo B', 'Pasillo C'],
  ['Pasillo B', 'LIB-001'], ['RM-103', 'RM-104'], ['RM-104', 'Pasillo C'], ['Pasillo C', 'Salida Sur'],
  ['Pasillo C', 'OFF-105'], ['OFF-105', 'LIB-001'], ['LIB-001', 'Salida Este'],
]
const corridors = edges.map(([from, to]) => ({ from, to }))

test('cada punto aparece exactamente una vez, con su tipo', () => {
  const g = layoutGraph(rooms, exits, corridors)
  const ids = g.nodes.map((n) => n.id)
  assert.equal(new Set(ids).size, ids.length)
  assert.equal(ids.length, 12)
  for (const r of rooms) assert.equal(g.nodes.find((n) => n.id === r)!.kind, 'room')
  for (const e of exits) assert.equal(g.nodes.find((n) => n.id === e)!.kind, 'exit')
  for (const j of ['Pasillo A', 'Pasillo B', 'Pasillo C']) assert.equal(g.nodes.find((n) => n.id === j)!.kind, 'junction')
})

test('las salidas quedan a la derecha de sus vecinos y las salas más a la izquierda', () => {
  const g = layoutGraph(rooms, exits, corridors)
  const x = (id: string) => g.nodes.find((n) => n.id === id)!.x
  assert.ok(x('Salida Norte') > x('Pasillo A'))
  assert.ok(x('Salida Sur') > x('Pasillo C'))
  assert.ok(x('Pasillo A') > x('LAB-101'))
  assert.equal(x('Salida Norte'), x('Salida Este'))
  assert.equal(x('Salida Este'), x('Salida Sur'))
})

test('ningún par de nodos comparte posición y todos caben en el lienzo', () => {
  const g = layoutGraph(rooms, exits, corridors)
  const posiciones = g.nodes.map((n) => `${n.x},${n.y}`)
  assert.equal(new Set(posiciones).size, posiciones.length)
  for (const n of g.nodes) {
    assert.ok(n.x - n.width / 2 >= 0 && n.x + n.width / 2 <= g.width, `${n.id} fuera en x`)
    assert.ok(n.y >= 0 && n.y <= g.height, `${n.id} fuera en y`)
  }
  assert.ok(g.width > 0 && g.height > 0)
})

test('el trazado es determinista', () => {
  assert.deepEqual(layoutGraph(rooms, exits, corridors), layoutGraph(rooms, exits, corridors))
})

test('una sala sin corredores se ubica igualmente y no rompe el trazado', () => {
  const g = layoutGraph([...rooms, 'ISLA'], exits, corridors)
  assert.ok(g.nodes.some((n) => n.id === 'ISLA'))
  assert.equal(new Set(g.nodes.map((n) => `${n.x},${n.y}`)).size, g.nodes.length)
})

test('sin salidas y sin corredores todas las salas se muestran en una columna', () => {
  const g = layoutGraph(['A', 'B', 'C'], [], [])
  assert.equal(g.nodes.length, 3)
  assert.equal(new Set(g.nodes.map((n) => n.x)).size, 1)
})

test('N salidas: crece en alto sin superponer nodos', () => {
  const muchas = Array.from({ length: 12 }, (_, i) => `S${i}`)
  const g = layoutGraph(['R'], muchas, muchas.map((s) => ({ from: 'R', to: s })))
  assert.equal(g.nodes.length, 13)
  assert.equal(new Set(g.nodes.map((n) => `${n.x},${n.y}`)).size, 13)
  assert.ok(g.height >= 12 * 60)
})

// ---------- vista reducida para campus grandes ----------
import { visibleRoomIds } from './graphLayout.ts'

test('con pocas salas se dibujan todas', () => {
  assert.deepEqual(visibleRoomIds(['A', 'B', 'C'], 'A', ['A', 'X'], 40), ['A', 'B', 'C'])
})

test('con muchas salas solo se dibujan la elegida y las que están en la ruta, en el orden recibido', () => {
  const todas = Array.from({ length: 100 }, (_, i) => `R${i}`)
  assert.deepEqual(visibleRoomIds(todas, 'R7', ['R7', 'Pasillo', 'R3', 'Salida'], 40), ['R3', 'R7'])
})

test('la sala elegida se dibuja aunque no haya ruta', () => {
  const todas = Array.from({ length: 100 }, (_, i) => `R${i}`)
  assert.deepEqual(visibleRoomIds(todas, 'R50', [], 40), ['R50'])
})
