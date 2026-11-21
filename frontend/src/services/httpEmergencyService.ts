import {
  ServiceError,
  type AlarmReport,
  type Building,
  type Corridor,
  type EmergencyService,
  type EvacuationResponse,
  type Room,
} from './emergencyService.ts'
import { createBackendClient, type BackendClientOptions } from './backendClient.ts'

/** Límite del contrato (ArbolJerarquia.MAX_OCUPANTES_SALA). El backend sigue validando. */
const MAX_OCCUPANTS_PER_ROOM = 80

// Ids opacos para la interfaz: el backend identifica por nombres, no por ids.
export const roomIdOf = (building: string, wing: string, letter: string) => JSON.stringify(['room', building, wing, letter])
export const wingIdOf = (building: string, wing: string) => JSON.stringify(['wing', building, wing])
export const buildingIdOf = (building: string) => JSON.stringify(['building', building])
export const corridorIdOf = (from: string, to: string) => JSON.stringify(['corridor', from, to])

function parseId(id: string, tag: string, size: number): string[] {
  try {
    const value: unknown = JSON.parse(id)
    if (Array.isArray(value) && value.length === size + 1 && value[0] === tag && value.every((v) => typeof v === 'string')) {
      return value.slice(1) as string[]
    }
  } catch { /* cae al error de abajo */ }
  throw new ServiceError('response', `Identificador inválido: ${id}`)
}
export function parseRoomId(id: string) {
  const [building, wing, letter] = parseId(id, 'room', 3)
  return { building, wing, letter }
}
export function parseCorridorId(id: string) {
  const [from, to] = parseId(id, 'corridor', 2)
  return { from, to }
}

interface DtoEdificios { edificios: { nombre: string; totalOcupantes: number; alas: { nombre: string; totalOcupantes: number }[] }[] }
interface DtoSala { letra: string; codigo: string; ocupantes: number }
interface DtoSalas { salas: DtoSala[] }
interface DtoOcupante { edificio: string; ala: string; letra: string; codigo: string; ocupantes: number }
interface DtoAlarma { salas: { edificio: string; ala: string; codigo: string; ocupantes: number }[] }
interface DtoRutas { salidas: string[]; rutas: { sala: string; alcanzable: boolean; salida: string | null; tiempoSegundos: number | null; ruta: string[] }[] }
interface DtoPasillos { pasillos: { desde: string; hacia: string; segundos: number; estado: string }[] }

const toRoom = (building: string, wing: string, s: DtoSala): Room => ({
  id: roomIdOf(building, wing, s.letra), code: s.codigo, name: `Sala ${s.letra}`, occupants: s.ocupantes,
})

/** Servicio conectado al backend Java. No ordena ni calcula rutas: muestra lo que devuelve la API. */
export function createHttpEmergencyService(baseUrl: string, options: BackendClientOptions = {}): EmergencyService {
  const api = createBackendClient(baseUrl, options)

  return {
    maxOccupantsPerRoom: MAX_OCCUPANTS_PER_ROOM,

    async getDirectory(): Promise<Building[]> {
      const { edificios } = await api.get<DtoEdificios>('/api/edificios')
      return Promise.all(edificios.map(async (e) => ({
        id: buildingIdOf(e.nombre),
        name: e.nombre,
        totalOccupants: e.totalOcupantes,
        wings: await Promise.all(e.alas.map(async (a) => {
          const { salas } = await api.get<DtoSalas>('/api/salas', { edificio: e.nombre, ala: a.nombre })
          return {
            id: wingIdOf(e.nombre, a.nombre),
            name: a.nombre,
            totalOccupants: a.totalOcupantes,
            rooms: salas.map((s) => toRoom(e.nombre, a.nombre, s)),
          }
        })),
      })))
    },

    async updateRoomOccupants(roomId, occupants): Promise<Room> {
      const { building, wing, letter } = parseRoomId(roomId)
      const r = await api.post<DtoOcupante>('/api/ocupantes', { edificio: building, ala: wing, letra: letter, ocupantes: occupants })
      return { id: roomIdOf(r.edificio, r.ala, r.letra), code: r.codigo, name: `Sala ${r.letra}`, occupants: r.ocupantes }
    },

    async simulateAlarm(): Promise<AlarmReport> {
      const r = await api.post<DtoAlarma>('/api/alarma')
      return {
        generatedAt: new Date().toISOString(),
        entries: r.salas.map((s) => ({ roomCode: s.codigo, building: s.edificio, wing: s.ala, occupants: s.ocupantes })),
      }
    },

    async getEvacuation(roomCode): Promise<EvacuationResponse> {
      const [rutas, pasillos] = await Promise.all([
        api.get<DtoRutas>('/api/rutas'),
        api.get<DtoPasillos>('/api/pasillos'),
      ])
      const rooms: Room[] = rutas.rutas.map((r) => ({ id: r.sala, code: r.sala, name: r.sala, occupants: 0 }))
      const corridors: Corridor[] = pasillos.pasillos.map((p) => ({
        id: corridorIdOf(p.desde, p.hacia), label: `${p.desde} – ${p.hacia}`,
        from: p.desde, to: p.hacia, seconds: p.segundos, open: p.estado === 'Abierto',
      }))
      const exits = rutas.salidas.map((s) => ({ id: s, name: s }))
      const code = roomCode || rutas.rutas[0]?.sala || ''
      if (!code) {
        return { rooms, corridors, exits, plan: { roomCode: '', reachable: false, path: [] } }
      }
      const ruta = rutas.rutas.find((r) => r.sala === code)
      if (!ruta) throw new ServiceError('api', `El backend no conoce la sala '${code}'.`, 404)
      const reachable = ruta.alcanzable
      return {
        rooms, corridors, exits,
        plan: {
          roomCode: code,
          reachable,
          exitId: reachable ? ruta.salida ?? undefined : undefined,
          exitName: reachable ? ruta.salida ?? undefined : undefined,
          totalSeconds: reachable ? ruta.tiempoSegundos ?? undefined : undefined,
          path: reachable ? ruta.ruta : [],
        },
      }
    },

    async getDatasets() {
      const r = await api.get<{ actual: string | null; conjuntos: string[] }>('/api/datos')
      return { current: r.actual, datasets: r.conjuntos }
    },

    async loadDataset(name) {
      const r = await api.post<{ actual: string; salas: number }>('/api/datos/cargar', { conjunto: name })
      return { current: r.actual, rooms: r.salas }
    },

    async setCorridorStatus(corridorId, status): Promise<void> {
      const { from, to } = parseCorridorId(corridorId)
      await api.post(status === 'blocked' ? '/api/pasillos/bloquear' : '/api/pasillos/desbloquear', { desde: from, hacia: to })
    },
  }
}
