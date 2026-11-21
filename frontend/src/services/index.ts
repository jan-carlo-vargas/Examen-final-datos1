import { demoEmergencyService, type EmergencyService } from './emergencyService.ts'
import { createHttpEmergencyService } from './httpEmergencyService.ts'

/**
 * Origen de datos de la interfaz.
 * - Con VITE_SALIDASEGURA_API_URL definida: modo conectado al backend Java. Si falla, la interfaz
 *   muestra el error; nunca cambia a datos de demostración por su cuenta.
 * - Sin ella: modo demostración, identificado como tal en pantalla.
 */
const apiUrl = (import.meta.env.VITE_SALIDASEGURA_API_URL ?? '').trim()

export type DataMode = 'connected' | 'demo'
export const dataMode: DataMode = apiUrl ? 'connected' : 'demo'
export const service: EmergencyService = apiUrl ? createHttpEmergencyService(apiUrl) : demoEmergencyService
export const serviceName = dataMode === 'demo' ? 'servicio demo' : 'backend'
