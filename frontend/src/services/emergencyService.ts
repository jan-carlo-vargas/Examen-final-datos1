export type ServiceState = 'loading' | 'ready' | 'empty' | 'error'

export interface Room {
  id: string
  code: string
  name: string
  occupants: number
}

export interface Wing {
  id: string
  name: string
  rooms: Room[]
  /** Total que entrega el backend (modo conectado); el modo demo lo suma en la interfaz. */
  totalOccupants?: number
}

export interface Building {
  id: string
  name: string
  wings: Wing[]
  totalOccupants?: number
}

export interface AlarmEntry {
  roomCode: string
  building: string
  wing: string
  occupants: number
}

export interface AlarmReport {
  generatedAt: string
  entries: AlarmEntry[]
}

export interface Corridor {
  id: string
  label: string
  from: string
  to: string
  seconds: number
  open: boolean
}

export interface EvacuationPlan {
  roomCode: string
  reachable: boolean
  exitId?: string
  exitName?: string
  totalSeconds?: number
  path: string[]
}

export interface ExitPoint {
  id: string
  name: string
}

export interface EvacuationResponse {
  rooms: Room[]
  corridors: Corridor[]
  plan: EvacuationPlan
  /** Salidas reales del backend (N salidas). Si falta, la interfaz las deduce del demo. */
  exits?: ExitPoint[]
}

export type ServiceErrorKind = 'connection' | 'api' | 'response' | 'configuration'

/** Error tipado para que la interfaz distinga backend caído, rechazo de la API y respuestas inválidas. */
export class ServiceError extends Error {
  readonly kind: ServiceErrorKind
  readonly status?: number
  constructor(kind: ServiceErrorKind, message: string, status?: number) {
    super(message)
    this.name = 'ServiceError'
    this.kind = kind
    this.status = status
  }
}

/**
 * Boundary expected from the Java backend. The UI consumes ordering and route
 * results exactly as received and never runs Counting Sort or Dijkstra itself.
 */
export interface EmergencyService {
  /** Máximo de ocupantes por sala aceptado por el origen de datos (el backend sigue validando). */
  readonly maxOccupantsPerRoom: number
  getDirectory(): Promise<Building[]>
  updateRoomOccupants(roomId: string, occupants: number): Promise<Room>
  simulateAlarm(): Promise<AlarmReport>
  getEvacuation(roomCode: string): Promise<EvacuationResponse>
  setCorridorStatus(corridorId: string, status: 'open' | 'blocked'): Promise<void>
  /** Solo en modo conectado: conjuntos de datos que ofrece el backend. */
  getDatasets?(): Promise<DatasetList>
  /** Solo en modo conectado: el backend reemplaza sus datos (descarta los cambios en memoria). */
  loadDataset?(name: string): Promise<{ current: string; rooms: number }>
}

export interface DatasetList {
  current: string | null
  datasets: string[]
}

const rooms: Room[] = [
  { id: 'r1', code: 'A-101', name: 'Aula de Anatomía', occupants: 38 },
  { id: 'r2', code: 'A-102', name: 'Biblioteca Norte', occupants: 64 },
  { id: 'r3', code: 'A-103', name: 'Laboratorio de Química', occupants: 28 },
  { id: 'r4', code: 'A-104', name: 'Sala Ñandú', occupants: 22 },
  { id: 'r5', code: 'A-105', name: 'Taller de Diseño', occupants: 31 },
  { id: 'r6', code: 'A-106', name: 'Unidad de Posgrado', occupants: 17 },
  { id: 'r7', code: 'A-107', name: 'Videoconferencias', occupants: 12 },
  { id: 'r8', code: 'A-108', name: 'Zona de estudio', occupants: 41 },
  { id: 'r9', code: 'B-201', name: 'Auditorio Principal', occupants: 96 },
  { id: 'r10', code: 'B-202', name: 'Centro de Cómputo', occupants: 52 },
  { id: 'r11', code: 'B-203', name: 'Enfermería', occupants: 8 },
  { id: 'r12', code: 'C-301', name: 'Aula Magna', occupants: 64 },
]

let directory: Building[] = [
  { id: 'b1', name: 'Edificio de Ciencias', wings: [
    { id: 'w1', name: 'Ala Norte', rooms: rooms.slice(0, 8) },
    { id: 'w2', name: 'Ala Sur', rooms: rooms.slice(8, 10) },
  ] },
  { id: 'b2', name: 'Edificio de Servicios', wings: [
    { id: 'w3', name: 'Ala Central', rooms: rooms.slice(10, 12) },
  ] },
]

let corridors: Corridor[] = [
  { id: 'c1', label: 'Corredor A norte', from: 'A-101', to: 'COR-1', seconds: 18, open: true },
  { id: 'c2', label: 'Corredor A central', from: 'A-102', to: 'COR-1', seconds: 15, open: true },
  { id: 'c3', label: 'Corredor B oeste', from: 'B-201', to: 'COR-1', seconds: 22, open: true },
  { id: 'c4', label: 'Pasillo principal', from: 'COR-1', to: 'COR-2', seconds: 28, open: true },
  { id: 'c5', label: 'Corredor C norte', from: 'C-301', to: 'COR-2', seconds: 16, open: true },
  { id: 'c6', label: 'Acceso salida norte', from: 'COR-2', to: 'SAL-1', seconds: 12, open: true },
  { id: 'c7', label: 'Acceso salida central', from: 'COR-2', to: 'SAL-2', seconds: 10, open: false },
  { id: 'c8', label: 'Acceso salida sur', from: 'COR-2', to: 'SAL-3', seconds: 20, open: true },
]

const wait = (milliseconds = 450) => new Promise((resolve) => setTimeout(resolve, milliseconds))
const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value))

// Preset demo responses model backend output; they are not calculated in the UI.
const presetPlans: Record<string, { normal: EvacuationPlan; noMain: EvacuationPlan }> = {
  'A-101': {
    normal: { roomCode: 'A-101', reachable: true, exitId: 'SAL-1', exitName: 'Salida 1 — Norte', totalSeconds: 58, path: ['A-101', 'COR-1', 'COR-2', 'SAL-1'] },
    noMain: { roomCode: 'A-101', reachable: false, path: [] },
  },
  'A-102': {
    normal: { roomCode: 'A-102', reachable: true, exitId: 'SAL-1', exitName: 'Salida 1 — Norte', totalSeconds: 55, path: ['A-102', 'COR-1', 'COR-2', 'SAL-1'] },
    noMain: { roomCode: 'A-102', reachable: false, path: [] },
  },
  'B-201': {
    normal: { roomCode: 'B-201', reachable: true, exitId: 'SAL-1', exitName: 'Salida 1 — Norte', totalSeconds: 62, path: ['B-201', 'COR-1', 'COR-2', 'SAL-1'] },
    noMain: { roomCode: 'B-201', reachable: false, path: [] },
  },
  'C-301': {
    normal: { roomCode: 'C-301', reachable: true, exitId: 'SAL-1', exitName: 'Salida 1 — Norte', totalSeconds: 28, path: ['C-301', 'COR-2', 'SAL-1'] },
    noMain: { roomCode: 'C-301', reachable: true, exitId: 'SAL-1', exitName: 'Salida 1 — Norte', totalSeconds: 28, path: ['C-301', 'COR-2', 'SAL-1'] },
  },
}

const graphRooms = [rooms[0], rooms[1], rooms[8], rooms[11]]

export const demoEmergencyService: EmergencyService = {
  maxOccupantsPerRoom: 999,
  async getDirectory() {
    await wait()
    return clone(directory)
  },
  async updateRoomOccupants(roomId, occupants) {
    await wait(300)
    let updated: Room | undefined
    directory = directory.map((building) => ({ ...building, wings: building.wings.map((wing) => ({
      ...wing,
      rooms: wing.rooms.map((room) => {
        if (room.id !== roomId) return room
        updated = { ...room, occupants }
        return updated
      }),
    })) }))
    if (!updated) throw new Error('Room not found')
    return clone(updated)
  },
  async simulateAlarm() {
    await wait(800)
    // Stable, descending order is intentionally preset as if returned by the backend.
    const entries: AlarmEntry[] = [
      { roomCode: 'B-201', building: 'Edificio de Ciencias', wing: 'Ala Sur', occupants: 96 },
      { roomCode: 'A-102', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 64 },
      { roomCode: 'C-301', building: 'Edificio de Servicios', wing: 'Ala Central', occupants: 64 },
      { roomCode: 'B-202', building: 'Edificio de Ciencias', wing: 'Ala Sur', occupants: 52 },
      { roomCode: 'A-108', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 41 },
      { roomCode: 'A-101', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 38 },
      { roomCode: 'A-105', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 31 },
      { roomCode: 'A-103', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 28 },
      { roomCode: 'A-104', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 22 },
      { roomCode: 'A-106', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 17 },
      { roomCode: 'A-107', building: 'Edificio de Ciencias', wing: 'Ala Norte', occupants: 12 },
      { roomCode: 'B-203', building: 'Edificio de Servicios', wing: 'Ala Central', occupants: 8 },
    ]
    return { generatedAt: new Date().toISOString(), entries }
  },
  async getEvacuation(roomCode) {
    await wait(500)
    roomCode = roomCode || 'A-101'
    const selected = presetPlans[roomCode] ?? { normal: { roomCode, reachable: false, path: [] }, noMain: { roomCode, reachable: false, path: [] } }
    const mainOpen = corridors.find((corridor) => corridor.id === 'c4')?.open
    const exitOpen = corridors.some((corridor) => corridor.to.startsWith('SAL') && corridor.open)
    const plan = !exitOpen ? { roomCode, reachable: false, path: [] } : mainOpen ? selected.normal : selected.noMain
    const exits = [
      { id: 'SAL-1', name: 'Salida 1 — Norte' },
      { id: 'SAL-2', name: 'Salida 2 — Central' },
      { id: 'SAL-3', name: 'Salida 3 — Sur' },
    ]
    return { rooms: clone(graphRooms), corridors: clone(corridors), plan: clone(plan), exits }
  },
  async setCorridorStatus(corridorId, status) {
    await wait(350)
    corridors = corridors.map((corridor) => corridor.id === corridorId ? { ...corridor, open: status === 'open' } : corridor)
  },
}
