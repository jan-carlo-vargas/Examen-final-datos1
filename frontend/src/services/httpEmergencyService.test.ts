// Pruebas del adaptador HTTP con un fetch simulado. Ejecutar: pnpm test (node --test, sin dependencias nuevas).
import test from 'node:test'
import assert from 'node:assert/strict'
import { ServiceError } from './emergencyService.ts'
import {
  corridorIdOf,
  createHttpEmergencyService,
  parseCorridorId,
  parseRoomId,
  roomIdOf,
} from './httpEmergencyService.ts'

type Handler = (path: string, init: RequestInit) => { status: number; body?: unknown; invalidJson?: boolean } | Error

function fakeFetch(handler: Handler) {
  const calls: { path: string; method: string }[] = []
  const impl = (async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const path = String(input).replace('http://b', '')
    calls.push({ path, method: init.method ?? 'GET' })
    const r = handler(path, init)
    if (r instanceof Error) throw r
    return {
      ok: r.status >= 200 && r.status < 300,
      status: r.status,
      json: async () => {
        if (r.invalidJson) throw new SyntaxError('no es JSON')
        return r.body
      },
    }
  }) as unknown as typeof fetch
  return { impl, calls }
}

const edificios = {
  edificios: [
    { nombre: 'Principal', totalOcupantes: 180, alas: [
      { nombre: 'Oeste', totalOcupantes: 54 },
      { nombre: 'Este', totalOcupantes: 51 },
    ] },
    { nombre: 'Anexo Ñ', totalOcupantes: 7, alas: [{ nombre: 'Oeste', totalOcupantes: 7 }] },
  ],
}
const salasOeste = { edificio: 'Principal', ala: 'Oeste', capacidad: 27, totalOcupantes: 54, salas: [
  { letra: 'A', codigo: 'LAB-101', ocupantes: 24 },
  { letra: 'N', codigo: 'LAB-N', ocupantes: 1 },
  { letra: 'Ñ', codigo: 'LAB-Ñ', ocupantes: 2 },
  { letra: 'B', codigo: 'LAB-102', ocupantes: 30 }, // orden deliberadamente raro: el adaptador no debe reordenar
] }

function directoryHandler(path: string) {
  if (path === '/api/edificios') return { status: 200, body: edificios }
  if (path.startsWith('/api/salas?edificio=Principal&ala=Oeste')) return { status: 200, body: salasOeste }
  if (path.startsWith('/api/salas?edificio=Principal&ala=Este')) {
    return { status: 200, body: { edificio: 'Principal', ala: 'Este', capacidad: 27, totalOcupantes: 51, salas: [] } }
  }
  if (path.startsWith('/api/salas?')) {
    return { status: 200, body: { edificio: 'Anexo Ñ', ala: 'Oeste', capacidad: 27, totalOcupantes: 7, salas: [
      { letra: 'A', codigo: 'AN-1', ocupantes: 7 },
    ] } }
  }
  return { status: 404, body: { error: 'Ruta no encontrada: ' + path } }
}

// ---------- ids ----------

test('los ids de sala y corredor se codifican y decodifican sin pérdida, incluso con "|", espacios y ñ', () => {
  const id = roomIdOf('Edif|icio Ñ', 'Ala y/o 1', 'Ñ')
  assert.deepEqual(parseRoomId(id), { building: 'Edif|icio Ñ', wing: 'Ala y/o 1', letter: 'Ñ' })
  const cid = corridorIdOf('Pasillo|A', 'Salida Ñandú')
  assert.deepEqual(parseCorridorId(cid), { from: 'Pasillo|A', to: 'Salida Ñandú' })
})

test('ids mal formados se rechazan con un ServiceError explícito', () => {
  assert.throws(() => parseRoomId('basura'), ServiceError)
  assert.throws(() => parseCorridorId('basura'), ServiceError)
})

// ---------- configuración ----------

test('la URL base es obligatoria y se normaliza sin barra final', async () => {
  assert.throws(() => createHttpEmergencyService(''), (e: any) => e instanceof ServiceError && e.kind === 'configuration')
  const { impl, calls } = fakeFetch(() => ({ status: 200, body: { edificios: [] } }))
  const service = createHttpEmergencyService('http://b///', { fetchImpl: impl })
  await service.getDirectory()
  assert.equal(calls[0].path, '/api/edificios')
})

test('el límite de ocupantes del contrato es 80 (el backend sigue siendo quien valida)', () => {
  assert.equal(createHttpEmergencyService('http://b').maxOccupantsPerRoom, 80)
})

// ---------- directorio ----------

test('getDirectory arma edificios → alas → salas con los totales y el orden que entrega el backend', async () => {
  const { impl, calls } = fakeFetch(directoryHandler)
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  const directory = await service.getDirectory()

  assert.equal(directory.length, 2)
  assert.equal(directory[0].name, 'Principal')
  assert.equal(directory[0].totalOccupants, 180)
  assert.deepEqual(directory[0].wings.map((w) => [w.name, w.totalOccupants]), [['Oeste', 54], ['Este', 51]])
  const rooms = directory[0].wings[0].rooms
  assert.deepEqual(rooms.map((r) => r.code), ['LAB-101', 'LAB-N', 'LAB-Ñ', 'LAB-102']) // sin reordenar
  assert.deepEqual(rooms.map((r) => r.occupants), [24, 1, 2, 30])
  assert.equal(rooms[0].name, 'Sala A')
  assert.equal(directory[0].wings[1].rooms.length, 0)
  assert.ok(calls.every((c) => c.method === 'GET'))
  assert.equal(calls.filter((c) => c.path.startsWith('/api/salas')).length, 3) // una por ala
})

test('los ids de edificio, ala y sala son únicos aunque dos edificios tengan alas con el mismo nombre', async () => {
  const { impl } = fakeFetch(directoryHandler)
  const directory = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getDirectory()
  const wingIds = directory.flatMap((b) => b.wings.map((w) => w.id))
  assert.equal(new Set(wingIds).size, wingIds.length)
  const buildingIds = directory.map((b) => b.id)
  assert.equal(new Set(buildingIds).size, buildingIds.length)
  const roomIds = directory.flatMap((b) => b.wings.flatMap((w) => w.rooms.map((r) => r.id)))
  assert.equal(new Set(roomIds).size, roomIds.length)
  assert.deepEqual(parseRoomId(directory[0].wings[0].rooms[2].id), { building: 'Principal', wing: 'Oeste', letter: 'Ñ' })
})

test('directorio sin edificios devuelve una lista vacía (la interfaz muestra su estado vacío)', async () => {
  const { impl } = fakeFetch(() => ({ status: 200, body: { edificios: [] } }))
  assert.deepEqual(await createHttpEmergencyService('http://b', { fetchImpl: impl }).getDirectory(), [])
})

// ---------- actualizar ocupantes ----------

test('updateRoomOccupants hace POST con edificio, ala, letra y ocupantes decodificados del id', async () => {
  const { impl, calls } = fakeFetch((path) => ({ status: 200, body: {
    edificio: 'Anexo Ñ', ala: 'Oeste', letra: 'Ñ', codigo: 'LAB-Ñ', ocupantes: 55, totalAla: 60, totalEdificio: 61,
  }, ...(path.length ? {} : {}) }))
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  const room = await service.updateRoomOccupants(roomIdOf('Anexo Ñ', 'Oeste', 'Ñ'), 55)
  assert.deepEqual(calls, [{
    path: '/api/ocupantes?edificio=Anexo%20%C3%91&ala=Oeste&letra=%C3%91&ocupantes=55', method: 'POST',
  }])
  assert.equal(room.code, 'LAB-Ñ')
  assert.equal(room.occupants, 55)
  assert.equal(room.id, roomIdOf('Anexo Ñ', 'Oeste', 'Ñ'))
})

test('un error 400 del backend llega como ServiceError "api" con su mensaje y estado', async () => {
  const { impl } = fakeFetch(() => ({ status: 400, body: { error: 'Cantidad de ocupantes inválida: 99. Debe estar entre 0 y 80.' } }))
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  await assert.rejects(service.updateRoomOccupants(roomIdOf('Principal', 'Oeste', 'A'), 99), (e: any) =>
    e instanceof ServiceError && e.kind === 'api' && e.status === 400 && /entre 0 y 80/.test(e.message))
})

// ---------- alarma ----------

test('simulateAlarm hace POST y conserva exactamente el orden del backend, incluidos los empates', async () => {
  const salas = [
    { posicion: 1, edificio: 'Principal', ala: 'Central', letra: 'A', codigo: 'Z-1', ocupantes: 3 },
    { posicion: 2, edificio: 'Principal', ala: 'Este', letra: 'B', codigo: 'A-1', ocupantes: 9 }, // orden "equivocado" a propósito
    { posicion: 3, edificio: 'Anexo', ala: 'Norte', letra: 'A', codigo: 'M-1', ocupantes: 3 },
  ]
  const { impl, calls } = fakeFetch(() => ({ status: 200, body: { totalSalas: 3, salas } }))
  const report = await createHttpEmergencyService('http://b', { fetchImpl: impl }).simulateAlarm()
  assert.deepEqual(calls, [{ path: '/api/alarma', method: 'POST' }])
  assert.deepEqual(report.entries.map((e: any) => e.roomCode), ['Z-1', 'A-1', 'M-1'])
  assert.deepEqual(report.entries[1], { roomCode: 'A-1', building: 'Principal', wing: 'Este', occupants: 9 })
  assert.ok(!Number.isNaN(Date.parse(report.generatedAt)))
})

test('alarma sin salas devuelve un reporte vacío', async () => {
  const { impl } = fakeFetch(() => ({ status: 200, body: { totalSalas: 0, salas: [] } }))
  const report = await createHttpEmergencyService('http://b', { fetchImpl: impl }).simulateAlarm()
  assert.deepEqual(report.entries, [])
})

// ---------- evacuación ----------

const rutas = {
  salidas: ['Salida Norte', 'Salida Este', 'Salida Sur'],
  rutas: [
    { sala: 'LAB-101', alcanzable: true, salida: 'Salida Norte', tiempoSegundos: 30, ruta: ['LAB-101', 'Pasillo A', 'Salida Norte'] },
    { sala: 'RM-103', alcanzable: false, salida: null, tiempoSegundos: null, ruta: [] },
  ],
}
const pasillos = { pasillos: [
  { desde: 'Salida Norte', hacia: 'Pasillo A', segundos: 20, estado: 'Abierto' },
  { desde: 'Pasillo B', hacia: 'Salida Este', segundos: 20, estado: 'Bloqueado' },
] }
function evacuationHandler(path: string) {
  if (path === '/api/rutas') return { status: 200, body: rutas }
  if (path === '/api/pasillos') return { status: 200, body: pasillos }
  return { status: 404, body: { error: 'no' } }
}

test('getEvacuation devuelve salas, salidas, corredores y el plan de la sala pedida, tal como los entrega el backend', async () => {
  const { impl, calls } = fakeFetch(evacuationHandler)
  const result = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getEvacuation('LAB-101')
  assert.ok(calls.every((c) => c.method === 'GET'))
  assert.deepEqual(result.rooms.map((r) => r.code), ['LAB-101', 'RM-103'])
  assert.deepEqual(result.exits, [
    { id: 'Salida Norte', name: 'Salida Norte' }, { id: 'Salida Este', name: 'Salida Este' }, { id: 'Salida Sur', name: 'Salida Sur' },
  ])
  assert.deepEqual(result.plan, {
    roomCode: 'LAB-101', reachable: true, exitId: 'Salida Norte', exitName: 'Salida Norte', totalSeconds: 30,
    path: ['LAB-101', 'Pasillo A', 'Salida Norte'],
  })
  assert.equal(result.corridors.length, 2)
  assert.deepEqual(result.corridors.map((c) => [c.from, c.to, c.seconds, c.open]), [
    ['Salida Norte', 'Pasillo A', 20, true], ['Pasillo B', 'Salida Este', 20, false],
  ])
  assert.deepEqual(parseCorridorId(result.corridors[1].id), { from: 'Pasillo B', to: 'Salida Este' })
})

test('sin salida alcanzable el plan es reachable=false y path vacío, sin fabricar salida ni tiempo', async () => {
  const { impl } = fakeFetch(evacuationHandler)
  const { plan } = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getEvacuation('RM-103')
  assert.deepEqual(plan, { roomCode: 'RM-103', reachable: false, exitId: undefined, exitName: undefined, totalSeconds: undefined, path: [] })
})

test('getEvacuation con código vacío usa la primera sala que entrega el backend', async () => {
  const { impl } = fakeFetch(evacuationHandler)
  const { plan } = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getEvacuation('')
  assert.equal(plan.roomCode, 'LAB-101')
})

test('una sala que el backend no conoce se reporta como error, sin inventar una ruta', async () => {
  const { impl } = fakeFetch(evacuationHandler)
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  await assert.rejects(service.getEvacuation('NOPE'), (e: any) => e instanceof ServiceError && e.kind === 'api' && e.status === 404)
})

test('sin salas en el grafo el plan queda vacío y no alcanzable', async () => {
  const { impl } = fakeFetch((path) => path === '/api/rutas'
    ? { status: 200, body: { salidas: [], rutas: [] } } : { status: 200, body: { pasillos: [] } })
  const result = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getEvacuation('')
  assert.deepEqual(result.rooms, [])
  assert.equal(result.plan.reachable, false)
  assert.deepEqual(result.plan.path, [])
})

test('setCorridorStatus bloquea o desbloquea con los extremos decodificados del id', async () => {
  const { impl, calls } = fakeFetch(() => ({ status: 200, body: { desde: 'x', hacia: 'y', segundos: 1, estado: 'Bloqueado' } }))
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  const id = corridorIdOf('Pasillo C', 'Salida Sur')
  await service.setCorridorStatus(id, 'blocked')
  await service.setCorridorStatus(id, 'open')
  assert.deepEqual(calls, [
    { path: '/api/pasillos/bloquear?desde=Pasillo%20C&hacia=Salida%20Sur', method: 'POST' },
    { path: '/api/pasillos/desbloquear?desde=Pasillo%20C&hacia=Salida%20Sur', method: 'POST' },
  ])
})

test('un 404 al bloquear (corredor inexistente) se propaga como ServiceError', async () => {
  const { impl } = fakeFetch(() => ({ status: 404, body: { error: "No existe un corredor entre 'A' y 'B'." } }))
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  await assert.rejects(service.setCorridorStatus(corridorIdOf('A', 'B'), 'blocked'), (e: any) =>
    e instanceof ServiceError && e.status === 404 && /No existe un corredor/.test(e.message))
})

// ---------- fallos de conexión: nunca datos de demostración ----------

test('con el backend caído toda operación falla con "connection" y no devuelve datos de demostración', async () => {
  const { impl } = fakeFetch(() => new TypeError('fetch failed'))
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl })
  const operaciones = [
    () => service.getDirectory(),
    () => service.updateRoomOccupants(roomIdOf('P', 'O', 'A'), 1),
    () => service.simulateAlarm(),
    () => service.getEvacuation('X'),
    () => service.setCorridorStatus(corridorIdOf('A', 'B'), 'blocked'),
  ]
  for (const operacion of operaciones) {
    await assert.rejects(operacion(), (e: any) => e instanceof ServiceError && e.kind === 'connection')
  }
})

test('respuesta 200 que no es JSON se reporta como "response"; 500 sin JSON como "api"', async () => {
  const a = fakeFetch(() => ({ status: 200, invalidJson: true }))
  await assert.rejects(createHttpEmergencyService('http://b', { fetchImpl: a.impl }).simulateAlarm(), (e: any) => e.kind === 'response')
  const b = fakeFetch(() => ({ status: 500, invalidJson: true }))
  await assert.rejects(createHttpEmergencyService('http://b', { fetchImpl: b.impl }).simulateAlarm(), (e: any) =>
    e.kind === 'api' && e.status === 500)
})

test('el tiempo de espera agotado se reporta como "connection" con mensaje de tiempo', async () => {
  const impl = ((_: unknown, init: RequestInit) => new Promise((_, reject) => {
    init.signal!.addEventListener('abort', () => reject(init.signal!.reason))
  })) as unknown as typeof fetch
  const service = createHttpEmergencyService('http://b', { fetchImpl: impl, timeoutMs: 20 })
  await assert.rejects(service.getDirectory(), (e: any) => e.kind === 'connection' && /tiempo/i.test(e.message))
})

// ---------- conjuntos de datos ----------

test('getDatasets lee el conjunto actual y los disponibles', async () => {
  const { impl, calls } = fakeFetch(() => ({ status: 200, body: { actual: 'version-b', conjuntos: ['campus-400', 'version-b'] } }))
  const r = await createHttpEmergencyService('http://b', { fetchImpl: impl }).getDatasets!()
  assert.deepEqual(calls, [{ path: '/api/datos', method: 'GET' }])
  assert.deepEqual(r, { current: 'version-b', datasets: ['campus-400', 'version-b'] })
})

test('loadDataset hace POST con el nombre codificado y devuelve actual y cantidad de salas', async () => {
  const { impl, calls } = fakeFetch(() => ({ status: 200, body: { actual: 'campus-400', salas: 400 } }))
  const r = await createHttpEmergencyService('http://b', { fetchImpl: impl }).loadDataset!('campus-400')
  assert.deepEqual(calls, [{ path: '/api/datos/cargar?conjunto=campus-400', method: 'POST' }])
  assert.deepEqual(r, { current: 'campus-400', rooms: 400 })
})

test('loadDataset propaga el error de la API', async () => {
  const { impl } = fakeFetch(() => ({ status: 404, body: { error: "No existe el conjunto de datos 'x'." } }))
  await assert.rejects(createHttpEmergencyService('http://b', { fetchImpl: impl }).loadDataset!('x'), (e: any) =>
    e instanceof ServiceError && e.status === 404 && /No existe/.test(e.message))
})
