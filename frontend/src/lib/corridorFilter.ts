import type { Corridor } from '../services/emergencyService.ts'

const normalize = (text: string) => text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

/** Filtra la lista de corredores para mostrarla (no cambia su orden ni calcula nada del backend). */
export function filterCorridors(
  corridors: Corridor[],
  options: { query: string; onlyBlocked: boolean; limit: number },
): { items: Corridor[]; total: number } {
  const query = normalize(options.query.trim())
  const matches = corridors.filter((c) =>
    (!options.onlyBlocked || !c.open) && (query === '' || normalize(`${c.label} ${c.from} ${c.to}`).includes(query)))
  return { items: matches.slice(0, options.limit), total: matches.length }
}
