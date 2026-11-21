// Integración: el adaptador contra el backend Java REAL (proceso aparte, puerto efímero).
// Se omite si no se indica el classpath compilado del backend:
//   SALIDASEGURA_CLASSPATH=../java/target/classes pnpm test
import test from 'node:test'
import assert from 'node:assert/strict'
import { spawn, type ChildProcess } from 'node:child_process'
import { ServiceError } from './emergencyService.ts'
import { createHttpEmergencyService } from './httpEmergencyService.ts'

const classpath = process.env.SALIDASEGURA_CLASSPATH
const skip = classpath ? false : 'defina SALIDASEGURA_CLASSPATH para probar contra el backend real'

function startBackend(): Promise<{ proc: ChildProcess; port: number }> {
  return new Promise((resolve, reject) => {
    const proc = spawn('java', ['-cp', classpath!, 'cr.ac.tec.examen2.MainServidor', '../datos/version-b', '0', '*'],
      { cwd: new URL('../../../java/', import.meta.url).pathname })
    let out = ''
    const timer = setTimeout(() => { proc.kill(); reject(new Error('el backend no arrancó: ' + out)) }, 15000)
    proc.stdout!.on('data', (d) => {
      out += d
      const m = /puerto (\d+)/.exec(out)
      if (m) { clearTimeout(timer); resolve({ proc, port: Number(m[1]) }) }
    })
    proc.stderr!.on('data', (d) => { out += d })
    proc.on('exit', (code) => { clearTimeout(timer); reject(new Error(`el backend terminó (${code}): ${out}`)) })
  })
}

test('flujo completo del servicio contra el backend real', { skip }, async () => {
  const { proc, port } = await startBackend()
  proc.removeAllListeners('exit')
  try {
    const service = createHttpEmergencyService(`http://127.0.0.1:${port}`)

    // Directorio
    const directory = await service.getDirectory()
    assert.equal(directory.length, 1)
    assert.equal(directory[0].name, 'Principal')
    assert.equal(directory[0].totalOccupants, 180)
    assert.deepEqual(directory[0].wings.map((w) => [w.name, w.totalOccupants]), [['Oeste', 54], ['Central', 75], ['Este', 51]])
    const oeste = directory[0].wings[0]
    assert.deepEqual(oeste.rooms.map((r) => [r.code, r.occupants]), [['LAB-101', 24], ['LAB-102', 30]])

    // Actualizar ocupantes y volver a consultar totales
    const updated = await service.updateRoomOccupants(oeste.rooms[0].id, 80)
    assert.equal(updated.occupants, 80)
    const after = await service.getDirectory()
    assert.equal(after[0].wings[0].totalOccupants, 110)
    assert.equal(after[0].totalOccupants, 236)
    await assert.rejects(service.updateRoomOccupants(oeste.rooms[0].id, 500), (e: any) =>
      e instanceof ServiceError && e.kind === 'api' && e.status === 400)
    await assert.rejects(service.updateRoomOccupants(oeste.rooms[0].id, -1), (e: any) => e.status === 400)

    // Alarma: orden del backend (LAB-101 ahora tiene 80)
    const report = await service.simulateAlarm()
    assert.deepEqual(report.entries.map((e: any) => e.roomCode), ['LAB-101', 'RM-103', 'LIB-001', 'LAB-102', 'RM-104', 'OFF-105'])
    assert.deepEqual(report.entries.map((e: any) => e.occupants), [80, 45, 45, 30, 30, 6])
    assert.equal(report.entries[1].wing, 'Central')

    // Evacuación
    const first = await service.getEvacuation('')
    assert.equal(first.plan.roomCode, 'LAB-101')
    assert.equal(first.exits!.length, 3)
    assert.equal(first.rooms.length, 6)
    const rm104 = await service.getEvacuation('RM-104')
    assert.deepEqual(rm104.plan.path, ['RM-104', 'Pasillo C', 'Salida Sur'])
    assert.equal(rm104.plan.totalSeconds, 27)
    assert.equal(rm104.corridors.length, 15)
    assert.equal(rm104.corridors.filter((c) => !c.open).length, 1)

    // Bloquear: la ruta cambia y nunca usa el corredor bloqueado
    const sur = rm104.corridors.find((c) => c.from === 'Pasillo C' && c.to === 'Salida Sur')!
    await service.setCorridorStatus(sur.id, 'blocked')
    const blocked = await service.getEvacuation('RM-104')
    assert.equal(blocked.plan.exitName, 'Salida Este')
    assert.equal(blocked.plan.totalSeconds, 53)
    assert.equal(blocked.corridors.find((c) => c.id === sur.id)!.open, false)
    const pares = blocked.plan.path.slice(0, -1).map((n, i) => [n, blocked.plan.path[i + 1]].sort().join('|'))
    assert.ok(!pares.includes(['Pasillo C', 'Salida Sur'].sort().join('|')))

    // Desbloquear: vuelve la ruta original
    await service.setCorridorStatus(sur.id, 'open')
    const reopened = await service.getEvacuation('RM-104')
    assert.deepEqual(reopened.plan.path, ['RM-104', 'Pasillo C', 'Salida Sur'])

    // Sin salida alcanzable: nada fabricado
    await service.setCorridorStatus(sur.id, 'blocked')
    for (const c of reopened.corridors.filter((c) => (c.from === 'Salida Norte' && c.to === 'Pasillo A')
      || (c.from === 'LIB-001' && c.to === 'Salida Este'))) {
      await service.setCorridorStatus(c.id, 'blocked')
    }
    const none = await service.getEvacuation('LAB-101')
    assert.equal(none.plan.reachable, false)
    assert.deepEqual(none.plan.path, [])
    assert.equal(none.plan.totalSeconds, undefined)

    await assert.rejects(service.getEvacuation('NO-EXISTE'), (e: any) => e.kind === 'api' && e.status === 404)
  } finally {
    proc.kill()
  }
})

test('con el backend apagado el servicio informa conexión no disponible', { skip }, async () => {
  const service = createHttpEmergencyService('http://127.0.0.1:1')
  await assert.rejects(service.getDirectory(), (e: any) => e instanceof ServiceError && e.kind === 'connection')
  await assert.rejects(service.simulateAlarm(), (e: any) => e.kind === 'connection')
})

test('cambiar de conjunto de datos desde el servicio contra el backend real', { skip }, async () => {
  const { proc, port } = await startBackend()
  proc.removeAllListeners('exit')
  try {
    const service = createHttpEmergencyService(`http://127.0.0.1:${port}`)
    const before = await service.getDatasets!()
    assert.equal(before.current, 'version-b')
    assert.ok(before.datasets.includes('campus-400'))
    const loaded = await service.loadDataset!('campus-400')
    assert.deepEqual(loaded, { current: 'campus-400', rooms: 400 })
    const directory = await service.getDirectory()
    assert.equal(directory.length, 5)
    assert.equal((await service.getEvacuation('')).rooms.length, 400)
    await assert.rejects(service.loadDataset!('../version-b'), (e: any) => e.kind === 'api' && e.status === 404)
    assert.equal((await service.getDatasets!()).current, 'campus-400')
  } finally {
    proc.kill()
  }
})
