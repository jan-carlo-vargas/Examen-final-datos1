import test from 'node:test'
import assert from 'node:assert/strict'
import { filterCorridors } from './corridorFilter.ts'

const mk = (id: string, from: string, to: string, open = true) => ({ id, label: `${from} – ${to}`, from, to, seconds: 5, open })
const corridors = [
  mk('1', 'Pasillo Ñandú', 'CIE-N-01'), mk('2', 'Pasillo B', 'ING-A-02', false),
  mk('3', 'Vestíbulo Ciencias', 'Salida Norte', false), mk('4', 'Pasillo C', 'HUM-S-03'),
]

test('sin filtro devuelve todo respetando el orden y el límite', () => {
  const r = filterCorridors(corridors, { query: '', onlyBlocked: false, limit: 3 })
  assert.deepEqual(r.items.map((c) => c.id), ['1', '2', '3'])
  assert.equal(r.total, 4)
})

test('busca por texto sin distinguir mayúsculas ni tildes', () => {
  assert.deepEqual(filterCorridors(corridors, { query: 'vestibulo', onlyBlocked: false, limit: 10 }).items.map((c) => c.id), ['3'])
  assert.deepEqual(filterCorridors(corridors, { query: 'ÑANDÚ', onlyBlocked: false, limit: 10 }).items.map((c) => c.id), ['1'])
  assert.deepEqual(filterCorridors(corridors, { query: 'ing-a', onlyBlocked: false, limit: 10 }).items.map((c) => c.id), ['2'])
})

test('solo bloqueados', () => {
  const r = filterCorridors(corridors, { query: '', onlyBlocked: true, limit: 10 })
  assert.deepEqual(r.items.map((c) => c.id), ['2', '3'])
  assert.equal(r.total, 2)
})

test('combina texto y bloqueados; sin coincidencias devuelve vacío', () => {
  assert.deepEqual(filterCorridors(corridors, { query: 'salida', onlyBlocked: true, limit: 10 }).items.map((c) => c.id), ['3'])
  assert.deepEqual(filterCorridors(corridors, { query: 'zzz', onlyBlocked: false, limit: 10 }), { items: [], total: 0 })
})
