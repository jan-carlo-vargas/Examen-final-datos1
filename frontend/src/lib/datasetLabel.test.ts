import test from 'node:test'
import assert from 'node:assert/strict'
import { datasetLabel } from './datasetLabel.ts'

test('nombres conocidos tienen una etiqueta legible', () => {
  assert.equal(datasetLabel('campus-400'), 'Campus grande (400 salas)')
  assert.equal(datasetLabel('version-b'), 'Datos del enunciado (6 salas)')
})

test('un nombre desconocido se muestra tal cual', () => {
  assert.equal(datasetLabel('mi-campus'), 'mi-campus')
})
