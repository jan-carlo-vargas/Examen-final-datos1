import { useEffect, useMemo, useState } from 'react'
import { dataMode, service, serviceName } from './services/index'
import { layoutGraph, visibleRoomIds } from './lib/graphLayout'
import { filterCorridors } from './lib/corridorFilter'
import { datasetLabel } from './lib/datasetLabel'
import {
  ServiceError,
  type AlarmReport,
  type Building,
  type Corridor,
  type EvacuationPlan,
  type ExitPoint,
  type Room,
  type ServiceState,
  type Wing,
} from './services/emergencyService'

type View = 'directorio' | 'alarma' | 'evacuacion'

const isDemo = dataMode === 'demo'

function describeError(error: unknown): string {
  if (error instanceof ServiceError) {
    return error.kind === 'connection' ? `${error.message} Verifica que el backend esté en ejecución y que la URL configurada sea correcta.` : error.message
  }
  return 'No fue posible recuperar la información. Inténtalo de nuevo.'
}

const roomLabel = (room: Room) => room.name === room.code ? room.code : `${room.code} — ${room.name}`

const Icon = ({ name, size = 20 }: { name: string; size?: number }) => {
  const paths: Record<string, React.ReactNode> = {
    directory: <><path d="M4 21V10l8-6 8 6v11" /><path d="M9 21v-6h6v6M3 21h18" /></>,
    alarm: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    route: <><circle cx="6" cy="18" r="2" /><circle cx="18" cy="6" r="2" /><path d="M8 18h3a3 3 0 0 0 3-3v-6a3 3 0 0 1 3-3" /></>,
    people: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    edit: <><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" /></>,
    building: <><path d="M3 21h18M6 21V4h12v17M9 8h2M13 8h2M9 12h2M13 12h2M9 16h2M13 16h2" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    warning: <><path d="M10.3 3.7 2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4M12 17h.01" /></>,
    info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

const navItems: { id: View; label: string; icon: string; description: string }[] = [
  { id: 'directorio', label: 'Directorio', icon: 'directory', description: 'Edificios y ocupación' },
  { id: 'alarma', label: 'Alarma y reporte', icon: 'alarm', description: 'Simulación de brigada' },
  { id: 'evacuacion', label: 'Evacuación', icon: 'route', description: 'Rutas y corredores' },
]

function StatusPanel({ state, emptyText, errorText }: { state: ServiceState; emptyText?: string; errorText?: string }) {
  if (state === 'loading') return <div className="state-panel" role="status"><span className="spinner" />Cargando información…</div>
  if (state === 'error') return <div className="state-panel error" role="alert"><Icon name="warning" />{errorText ?? 'No fue posible recuperar la información. Inténtalo de nuevo.'}</div>
  if (state === 'empty') return <div className="state-panel"><Icon name="info" />{emptyText ?? 'No hay información disponible.'}</div>
  return null
}

function DatasetPicker({ onLoaded }: { onLoaded: () => void }) {
  const [datasets, setDatasets] = useState<string[]>([])
  const [current, setCurrent] = useState<string | null>(null)
  const [choice, setChoice] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  useEffect(() => {
    service.getDatasets?.().then((r) => { setDatasets(r.datasets); setCurrent(r.current); setChoice(r.current ?? r.datasets[0] ?? '') })
      .catch((error) => setMessage({ type: 'error', text: describeError(error) }))
  }, [])

  if (!service.getDatasets || (datasets.length === 0 && !message)) return null

  const load = async () => {
    if (!service.loadDataset || !choice) return
    setBusy(true); setMessage(null)
    try {
      const result = await service.loadDataset(choice)
      setCurrent(result.current)
      setMessage({ type: 'success', text: `Cargado: ${datasetLabel(result.current)}.` })
      onLoaded()
    } catch (error) {
      setMessage({ type: 'error', text: describeError(error) })
    } finally { setBusy(false) }
  }

  return <div className="dataset-picker">
    <p>Datos</p>
    <label htmlFor="dataset-select">Conjunto de datos</label>
    <select id="dataset-select" value={choice} onChange={(e) => setChoice(e.target.value)} disabled={busy || datasets.length === 0}>
      {datasets.map((name) => <option key={name} value={name}>{datasetLabel(name)}</option>)}
    </select>
    <button className="button primary compact" onClick={load} disabled={busy || !choice || choice === current}>{busy ? 'Cargando…' : choice === current ? 'En uso' : 'Cargar datos'}</button>
    <small>Al cargar se reemplazan los datos y se descartan los cambios hechos en memoria.</small>
    {message && <div className={`dataset-message ${message.type}`} role={message.type === 'error' ? 'alert' : 'status'}>{message.text}</div>}
  </div>
}

function DirectoryView() {
  const [buildings, setBuildings] = useState<Building[]>([])
  const [state, setState] = useState<ServiceState>('loading')
  const [openBuilding, setOpenBuilding] = useState<string | null>(null)
  const [openWing, setOpenWing] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState<Room | null>(null)
  const [occupants, setOccupants] = useState('')
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [errorText, setErrorText] = useState<string | undefined>()
  const [loadedAt, setLoadedAt] = useState<string>('')

  const load = () => {
    setState('loading')
    service.getDirectory().then((data) => {
      setBuildings(data)
      setLoadedAt(new Date().toLocaleTimeString('es-CR'))
      setState(data.length ? 'ready' : 'empty')
      if (data.length) setOpenBuilding(data[0].id)
    }).catch((error) => { setErrorText(describeError(error)); setState('error') })
  }
  useEffect(load, [])

  // En modo conectado los totales los calcula el backend (árbol B+); el modo demo los suma aquí.
  const sumRooms = (rooms: Room[]) => rooms.reduce((sum, room) => sum + room.occupants, 0)
  const buildingTotal = (building: Building) => building.totalOccupants ?? building.wings.reduce((sum, wing) => sum + sumRooms(wing.rooms), 0)
  const wingTotal = (wing: Wing) => wing.totalOccupants ?? sumRooms(wing.rooms)
  const maxOccupants = service.maxOccupantsPerRoom

  const beginEdit = (room: Room) => {
    setEditing(room)
    setOccupants(String(room.occupants))
    setFeedback(null)
  }

  const updateOccupants = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!editing) return
    const count = Number(occupants)
    if (occupants.trim() === '' || !Number.isInteger(count) || count < 0 || count > maxOccupants) {
      setFeedback({ type: 'error', text: `Ingresa un número entero entre 0 y ${maxOccupants}.` })
      return
    }
    try {
      const updated = await service.updateRoomOccupants(editing.id, count)
      setBuildings((current) => current.map((building) => ({
        ...building,
        wings: building.wings.map((wing) => ({
          ...wing,
          rooms: wing.rooms.map((room) => room.id === updated.id ? updated : room),
        })),
      })))
      setFeedback({ type: 'success', text: `Ocupación de ${updated.code} actualizada a ${count}.` })
      setEditing(null)
    } catch (error) {
      setFeedback({ type: 'error', text: error instanceof ServiceError ? error.message : 'No se pudo guardar el cambio.' })
      return
    }
    // Recarga silenciosa para que los totales sean los que calcula el servicio.
    try {
      setBuildings(await service.getDirectory())
      setLoadedAt(new Date().toLocaleTimeString('es-CR'))
    } catch (error) {
      setFeedback({ type: 'error', text: `El cambio se guardó, pero no se pudieron actualizar los totales. ${describeError(error)}` })
    }
  }

  return <section aria-labelledby="directory-title">
    <header className="page-header">
      <div>
        <p className="eyebrow">Gestión de espacios</p>
        <h1 id="directory-title">Directorio institucional</h1>
        <p>Consulta la jerarquía de edificios, alas y salas, y actualiza su ocupación.</p>
      </div>
      <div className="demo-chip"><span /> {isDemo ? 'Datos de demostración' : 'Datos del backend'}</div>
    </header>

    {feedback && <div className={`feedback ${feedback.type}`} role={feedback.type === 'error' ? 'alert' : 'status'}>
      <Icon name={feedback.type === 'success' ? 'check' : 'warning'} /> <span>{feedback.text}</span>
      <button aria-label="Cerrar mensaje" onClick={() => setFeedback(null)}><Icon name="close" size={16} /></button>
    </div>}

    {state !== 'ready' ? <StatusPanel state={state} errorText={errorText} emptyText="El backend no tiene edificios registrados." /> : <div className="directory-layout">
      <div className="tree-card">
        <div className="card-heading">
          <div><h2>Campus central</h2><p>{buildings.length} edificios registrados</p></div>
          <label className="search"><span className="sr-only">Buscar sala</span><Icon name="search" size={18} /><input value={query} onChange={(e) => { setQuery(e.target.value); setPage(1) }} placeholder="Buscar sala o código" /></label>
        </div>
        <div className="tree-list">
          {buildings.map((building) => {
            const isBuildingOpen = openBuilding === building.id
            return <div className="tree-group" key={building.id}>
              <button className="tree-row building-row" aria-expanded={isBuildingOpen} onClick={() => setOpenBuilding(isBuildingOpen ? null : building.id)}>
                <span className={`chevron ${isBuildingOpen ? 'open' : ''}`}><Icon name="chevron" size={17} /></span>
                <span className="tree-icon"><Icon name="building" size={19} /></span>
                <span className="tree-name"><strong>{building.name}</strong><small>{building.wings.length} alas</small></span>
                <span className="occupancy"><strong>{buildingTotal(building)}</strong><small>personas</small></span>
              </button>
              {isBuildingOpen && <div className="wings">
                {building.wings.map((wing) => {
                  const isWingOpen = openWing === wing.id
                  const filtered = wing.rooms.filter((room) => `${room.name} ${room.code}`.toLocaleLowerCase('es').includes(query.toLocaleLowerCase('es')))
                  const perPage = 8
                  const pages = Math.max(1, Math.ceil(filtered.length / perPage))
                  const visible = filtered.slice((page - 1) * perPage, page * perPage)
                  return <div className="wing" key={wing.id}>
                    <button className="tree-row wing-row" aria-expanded={isWingOpen} onClick={() => { setOpenWing(isWingOpen ? null : wing.id); setPage(1) }}>
                      <span className={`chevron ${isWingOpen ? 'open' : ''}`}><Icon name="chevron" size={16} /></span>
                      <span className="tree-name"><strong>{wing.name}</strong><small>{wing.rooms.length} salas · orden alfabético español</small></span>
                      <span className="occupancy"><strong>{wingTotal(wing)}</strong><small>personas</small></span>
                    </button>
                    {isWingOpen && <div className="rooms-region" aria-label={`Salas de ${wing.name}`}>
                      <div className="rooms-header"><span>Sala</span><span>Ocupación</span><span>Acción</span></div>
                      {visible.length ? visible.map((room) => <div className="room-row" key={room.id}>
                        <span><strong>{room.name}</strong><small>{room.code}</small></span>
                        <span className="room-count"><Icon name="people" size={16} /> {room.occupants}</span>
                        <button className="text-button" onClick={() => beginEdit(room)}><Icon name="edit" size={15} /> Actualizar<span className="sr-only"> {room.code}</span></button>
                      </div>) : <div className="no-results">No se encontraron salas con ese criterio.</div>}
                      {pages > 1 && <div className="pagination">
                        <span>Página {Math.min(page, pages)} de {pages}</span>
                        <div><button disabled={page === 1} onClick={() => setPage((p) => p - 1)}>Anterior</button><button disabled={page >= pages} onClick={() => setPage((p) => p + 1)}>Siguiente</button></div>
                      </div>}
                    </div>}
                  </div>
                })}
              </div>}
            </div>
          })}
        </div>
      </div>

      <aside className="summary-card" aria-label="Resumen de ocupación">
        <p className="eyebrow">Resumen operativo</p>
        <div className="big-number">{buildings.reduce((sum, b) => sum + buildingTotal(b), 0)}</div>
        <p>personas registradas en el campus</p>
        <div className="summary-divider" />
        <dl>
          <div><dt>Edificios</dt><dd>{buildings.length}</dd></div>
          <div><dt>Salas</dt><dd>{buildings.flatMap((b) => b.wings.flatMap((w) => w.rooms)).length}</dd></div>
          <div><dt>Última actualización</dt><dd>{isDemo ? 'Demo' : loadedAt}</dd></div>
        </dl>
      </aside>
    </div>}

    {editing && <div className="modal-backdrop" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) setEditing(null) }}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="edit-title">
        <button className="icon-button modal-close" aria-label="Cerrar" onClick={() => setEditing(null)}><Icon name="close" /></button>
        <p className="eyebrow">Actualizar registro</p>
        <h2 id="edit-title">{editing.name}</h2>
        <p className="modal-subtitle">{editing.code} · Valor actual: {editing.occupants}</p>
        <form onSubmit={updateOccupants}>
          <label htmlFor="occupants">Número de ocupantes</label>
          <input id="occupants" autoFocus inputMode="numeric" value={occupants} onChange={(e) => setOccupants(e.target.value)} aria-describedby="occupants-help" />
          <small id="occupants-help">Ingresa un número entero entre 0 y {maxOccupants}.</small>
          {feedback?.type === 'error' && <p className="field-error" role="alert">{feedback.text}</p>}
          <div className="modal-actions"><button type="button" className="button secondary" onClick={() => setEditing(null)}>Cancelar</button><button className="button primary" type="submit">Guardar cambio</button></div>
        </form>
      </div>
    </div>}
  </section>
}

function AlarmView() {
  const [state, setState] = useState<ServiceState>('empty')
  const [report, setReport] = useState<AlarmReport | null>(null)
  const [page, setPage] = useState(1)
  const [errorText, setErrorText] = useState<string | undefined>()
  const simulate = async () => {
    setState('loading'); setReport(null); setPage(1)
    try {
      const result = await service.simulateAlarm()
      setReport(result); setState(result.entries.length ? 'ready' : 'empty')
    } catch (error) { setErrorText(describeError(error)); setState('error') }
  }
  const pageSize = 8
  const visible = report?.entries.slice((page - 1) * pageSize, page * pageSize) ?? []
  const pages = Math.ceil((report?.entries.length ?? 0) / pageSize)

  return <section aria-labelledby="alarm-title">
    <header className="page-header">
      <div><p className="eyebrow">Coordinación de brigada</p><h1 id="alarm-title">Alarma y reporte</h1><p>Genera un reporte de prioridad para apoyar la respuesta del equipo.</p></div>
      <div className="demo-chip"><span /> Modo simulación</div>
    </header>
    <div className="notice warning"><Icon name="warning" /><div><strong>Esto es una simulación digital</strong><p>No activa sirenas ni está conectado a dispositivos físicos de alarma.</p></div></div>
    <div className="alarm-hero">
      <div><span className="alarm-icon"><Icon name="alarm" size={26} /></span><div><h2>Simular señal de alarma</h2><p>Solicita al {serviceName} un reporte de salas priorizado.</p></div></div>
      <button className="button emergency" onClick={simulate} disabled={state === 'loading'}>{state === 'loading' ? 'Generando…' : 'Simular alarma'}</button>
    </div>

    <div className="report-card">
      <div className="report-heading"><div><p className="eyebrow">Reporte de brigada</p><h2>{report ? `${report.entries.length} salas por atender` : 'Aún no se ha generado un reporte'}</h2></div>{report && <span className="complete-badge"><Icon name="check" size={15} /> Completado</span>}</div>
      {state !== 'ready' ? <StatusPanel state={state} errorText={errorText} emptyText={report ? 'El backend no devolvió salas para el reporte.' : `Pulsa «Simular alarma» para solicitar el reporte${isDemo ? ' de demostración' : ''}.`} /> : <>
        <div className="order-note"><Icon name="info" size={18} /><span>Orden recibido del servicio: mayor a menor ocupación. Los empates conservan el orden estable del backend; esta interfaz no reordena los resultados.</span></div>
        <div className="table-wrap">
          <table><caption className="sr-only">Reporte de salas ordenado por ocupación</caption><thead><tr><th>Prioridad</th><th>Sala</th><th>Edificio</th><th>Ala</th><th>Ocupantes</th></tr></thead>
            <tbody>{visible.map((entry, index) => <tr key={`${entry.building}/${entry.wing}/${entry.roomCode}`}><td><span className={`priority ${page === 1 && index < 3 ? 'high' : ''}`}>{(page - 1) * pageSize + index + 1}</span></td><td><strong>{entry.roomCode}</strong></td><td>{entry.building}</td><td>{entry.wing}</td><td><strong>{entry.occupants}</strong> personas</td></tr>)}</tbody>
          </table>
        </div>
        {pages > 1 && <div className="pagination"><span>Mostrando {(page - 1) * pageSize + 1}–{Math.min(page * pageSize, report!.entries.length)} de {report!.entries.length}</span><div><button disabled={page === 1} onClick={() => setPage((p) => p - 1)}>Anterior</button><button disabled={page === pages} onClick={() => setPage((p) => p + 1)}>Siguiente</button></div></div>}
      </>}
    </div>
  </section>
}

function EvacuationView() {
  const [rooms, setRooms] = useState<Room[]>([])
  const [corridors, setCorridors] = useState<Corridor[]>([])
  const [exits, setExits] = useState<ExitPoint[]>([])
  const [selected, setSelected] = useState('')
  const [plan, setPlan] = useState<EvacuationPlan | null>(null)
  const [state, setState] = useState<ServiceState>('loading')
  const [errorText, setErrorText] = useState<string | undefined>()
  const [actionError, setActionError] = useState<string | null>(null)
  const [updating, setUpdating] = useState<string | null>(null)
  const [corridorQuery, setCorridorQuery] = useState('')
  const [onlyBlocked, setOnlyBlocked] = useState(false)

  const refresh = async (code = selected) => {
    setState('loading')
    try {
      const data = await service.getEvacuation(code)
      setRooms(data.rooms); setCorridors(data.corridors); setExits(data.exits ?? []); setPlan(data.plan)
      setSelected(data.plan.roomCode)
      setState(data.rooms.length ? 'ready' : 'empty')
    } catch (error) { setErrorText(describeError(error)); setState('error') }
  }
  useEffect(() => { refresh('') }, [])

  const chooseRoom = (code: string) => { setSelected(code); setActionError(null); refresh(code) }
  const toggle = async (corridor: Corridor) => {
    setUpdating(corridor.id); setActionError(null)
    try {
      await service.setCorridorStatus(corridor.id, corridor.open ? 'blocked' : 'open')
      await refresh(selected)
    } catch (error) {
      setActionError(describeError(error))
    } finally { setUpdating(null) }
  }
  const pathPairs = useMemo(() => new Set((plan?.path ?? []).slice(0, -1).map((n, i) => `${n}|${plan!.path[i + 1]}`)), [plan])

  // Posiciones solo para dibujar; las rutas siempre vienen del servicio.
  const roomIds = useMemo(() => rooms.map((r) => r.code), [rooms])
  const shownRooms = useMemo(() => visibleRoomIds(roomIds, selected, plan?.path ?? []), [roomIds, selected, plan])
  const reduced = shownRooms.length < roomIds.length
  const graph = useMemo(() => {
    if (!reduced) return layoutGraph(roomIds, exits.map((e) => e.id), corridors)
    // Campus grande: solo la sala elegida y los puntos de su ruta (el resto va en la lista de corredores).
    const keep = new Set([selected, ...(plan?.path ?? [])])
    return layoutGraph(shownRooms, exits.map((e) => e.id).filter((id) => keep.has(id)), corridors.filter((c) => keep.has(c.from) && keep.has(c.to)))
  }, [reduced, shownRooms, roomIds, exits, corridors, selected, plan])
  const listed = useMemo(() => filterCorridors(corridors, { query: corridorQuery, onlyBlocked, limit: 25 }), [corridors, corridorQuery, onlyBlocked])
  const positions = useMemo(() => new Map(graph.nodes.map((n) => [n.id, n])), [graph])
  const exitNames = useMemo(() => new Map(exits.map((e) => [e.id, e.name])), [exits])
  const pointLabel = (id: string) => (exitNames.get(id) ?? id).split(' — ')[0]

  return <section aria-labelledby="evacuation-title">
    <header className="page-header">
      <div><p className="eyebrow">Guía de salida</p><h1 id="evacuation-title">Evacuación</h1><p>Consulta rutas y administra la disponibilidad de los corredores.</p></div>
      <div className="demo-chip"><span /> {isDemo ? 'Rutas de demostración' : 'Rutas del backend'}</div>
    </header>
    {actionError && <div className="feedback error" role="alert"><Icon name="warning" /> <span>{actionError}</span><button aria-label="Cerrar mensaje" onClick={() => setActionError(null)}><Icon name="close" size={16} /></button></div>}
    <div className="evacuation-toolbar">
      <label htmlFor="room-select"><span>Sala de origen</span><select id="room-select" value={selected} onChange={(e) => chooseRoom(e.target.value)}>{rooms.map((room) => <option key={room.id} value={room.code}>{roomLabel(room)}</option>)}</select></label>
      <div className="legend" aria-label="Leyenda"><span><i className="dot room" /> Sala</span><span><i className="dot exit" /> Salida</span><span><i className="line open" /> Abierto</span><span><i className="line blocked" /> Bloqueado</span></div>
    </div>

    {state === 'error' || state === 'empty' ? <StatusPanel state={state} errorText={errorText} emptyText="El backend no tiene salas registradas en el grafo." /> : <div className="evacuation-grid">
      <div className="graph-card">
        <div className="card-heading"><div><h2>Esquema de circulación</h2><p>Selecciona un corredor para cambiar su estado</p></div><span className="live-status"><i /> {state === 'loading' ? 'Actualizando' : 'Guía lista'}</span></div>
        <div className="graph-scroll">
          <svg className="graph" viewBox={`0 0 ${graph.width} ${graph.height}`} role="img" aria-labelledby="graph-title graph-description">
            <title id="graph-title">Grafo de evacuación del campus</title>
            <desc id="graph-description">Salas conectadas con {exits.length} {exits.length === 1 ? 'salida' : 'salidas'} mediante corredores abiertos o bloqueados. La ruta recomendada se resalta.</desc>
            {corridors.map((edge) => {
              const a = positions.get(edge.from), b = positions.get(edge.to)
              if (!a || !b) return null
              const active = pathPairs.has(`${edge.from}|${edge.to}`) || pathPairs.has(`${edge.to}|${edge.from}`)
              return <g key={edge.id} className="edge-group" onClick={() => toggle(edge)} role="button" tabIndex={0} aria-label={`${edge.label}, ${edge.seconds} segundos, ${edge.open ? 'abierto' : 'bloqueado'}`} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') toggle(edge) }}>
                <line className={`edge-hit`} x1={a.x} y1={a.y} x2={b.x} y2={b.y} />
                <line className={`edge ${edge.open ? 'open' : 'blocked'} ${active ? 'active' : ''}`} x1={a.x} y1={a.y} x2={b.x} y2={b.y} />
                <g transform={`translate(${(a.x + b.x) / 2}, ${(a.y + b.y) / 2})`}><rect className="time-bg" x="-24" y="-12" width="48" height="24" rx="12" /><text className="time-label" textAnchor="middle" dy="4">{edge.seconds} s</text></g>
              </g>
            })}
            {graph.nodes.map((node) => {
              const isJunction = node.kind === 'junction', isSelected = node.id === selected
              return <g key={node.id} transform={`translate(${node.x},${node.y})`} className={`node ${node.kind} ${isSelected ? 'selected' : ''}`}>
                {isJunction ? <circle r="8" /> : <rect x={-node.width / 2} y="-22" width={node.width} height="44" rx="10" />}
                {isJunction ? <text textAnchor="middle" y="-14" fontSize="10">{node.label}</text> : <text textAnchor="middle" dy="4">{node.kind === 'exit' ? pointLabel(node.id) : node.label}</text>}
              </g>
            })}
          </svg>
        </div>
        <p className="graph-hint"><Icon name="info" size={16} /> Los tiempos indican segundos a pie entre nodos.{reduced && ` Vista reducida (campus de ${roomIds.length} salas): se dibuja solo la ruta de la sala elegida; el resto de corredores está en la lista de abajo.`}</p>
      </div>

      <aside className="route-panel">
        <div className="route-heading"><span className="route-icon"><Icon name="route" /></span><div><p className="eyebrow">Ruta más rápida</p><h2>Desde {selected}</h2></div></div>
        {state === 'loading' ? <StatusPanel state="loading" /> : plan?.reachable ? <>
          <div className="route-result"><div><small>Salida asignada</small><strong>{plan.exitName}</strong></div><div><small>Tiempo total</small><strong>{plan.totalSeconds} s</strong></div></div>
          <ol className="route-steps">{plan.path.map((step, index) => <li key={step}><span>{index + 1}</span><div><strong>{pointLabel(step)}</strong><small>{index === 0 ? 'Punto de origen' : index === plan.path.length - 1 ? 'Salida segura' : 'Continuar por corredor'}</small></div>{index < plan.path.length - 1 && <Icon name="arrow" size={16} />}</li>)}</ol>
          <div className="safe-note"><Icon name="check" /><span><strong>Ruta disponible</strong>Resultado recibido del {serviceName}.</span></div>
        </> : <div className="unreachable" role="alert"><Icon name="warning" size={28} /><h3>Sin salida alcanzable</h3><p>Los corredores disponibles no conectan esta sala con una salida. Revisa los bloqueos.</p></div>}
      </aside>
    </div>}

    <div className="corridors-card">
      <div className="card-heading"><div><h2>Estado de corredores</h2><p>Un corredor bloqueado se excluye de los cálculos del servicio.</p></div>
        {corridors.length > 25 && <div className="corridor-filters">
          <label className="search"><span className="sr-only">Buscar corredor</span><Icon name="search" size={18} /><input value={corridorQuery} onChange={(e) => setCorridorQuery(e.target.value)} placeholder="Buscar corredor o sala" /></label>
          <label className="only-blocked"><input type="checkbox" checked={onlyBlocked} onChange={(e) => setOnlyBlocked(e.target.checked)} /> Solo bloqueados</label>
        </div>}
      </div>
      {listed.total > listed.items.length && <p className="graph-hint"><Icon name="info" size={16} /> Mostrando {listed.items.length} de {listed.total} corredores. Usa la búsqueda para ver otros.</p>}
      {listed.total === 0 && <div className="no-results">No hay corredores con ese criterio.</div>}
      <div className="corridor-list">{listed.items.map((corridor) => <div className="corridor-item" key={corridor.id}>
        <span className={`status-icon ${corridor.open ? 'open' : 'blocked'}`}><Icon name={corridor.open ? 'check' : 'close'} size={16} /></span>
        <span><strong>{corridor.label}</strong><small>{corridor.from} ↔ {corridor.to} · {corridor.seconds} s</small></span>
        <span className={`status-text ${corridor.open ? 'open' : 'blocked'}`}>{corridor.open ? 'Abierto' : 'Bloqueado'}</span>
        <button className="button secondary compact" disabled={updating === corridor.id} onClick={() => toggle(corridor)}>{updating === corridor.id ? 'Actualizando…' : corridor.open ? 'Bloquear' : 'Desbloquear'}</button>
      </div>)}</div>
    </div>

    <details className="text-alternative"><summary>Alternativa textual accesible del grafo</summary><div><h3>Nodos</h3><ul>{rooms.map((r) => <li key={r.id}>{r.code}{r.name !== r.code ? `: ${r.name}` : ''}</li>)}{exits.map((e) => <li key={e.id}>{e.name}</li>)}</ul><h3>Conexiones</h3><ul>{corridors.map((c) => <li key={c.id}>{c.from} a {c.to}: {c.seconds} segundos, {c.open ? 'abierto' : 'bloqueado'}.</li>)}</ul></div></details>
  </section>
}

export default function App() {
  const [view, setView] = useState<View>('directorio')
  const [mobileNav, setMobileNav] = useState(false)
  const [dataVersion, setDataVersion] = useState(0)
  const active = navItems.find((item) => item.id === view)!
  return <div className="app-shell">
    <a className="skip-link" href="#main-content">Saltar al contenido principal</a>
    <aside className={`sidebar ${mobileNav ? 'mobile-open' : ''}`}>
      <div className="brand"><span className="brand-mark"><Icon name="route" size={23} /></span><span><strong>SalidaSegura</strong><small>Gestión de emergencias</small></span></div>
      <nav aria-label="Navegación principal">
        <p>Operaciones</p>
        {navItems.map((item) => <button key={item.id} className={view === item.id ? 'active' : ''} aria-current={view === item.id ? 'page' : undefined} onClick={() => { setView(item.id); setMobileNav(false) }}>
          <Icon name={item.icon} /><span><strong>{item.label}</strong><small>{item.description}</small></span>
        </button>)}
      </nav>
      <DatasetPicker onLoaded={() => setDataVersion((v) => v + 1)} />
      <div className="sidebar-footer"><span className="connection-dot" /><span><strong>{isDemo ? 'Entorno de demostración' : 'Modo conectado al backend'}</strong><small>Sin conexión a dispositivos</small></span></div>
    </aside>
    {mobileNav && <button className="nav-overlay" aria-label="Cerrar navegación" onClick={() => setMobileNav(false)} />}
    <div className="main-column">
      <header className="topbar">
        <button className="icon-button mobile-menu" aria-label="Abrir navegación" aria-expanded={mobileNav} onClick={() => setMobileNav(!mobileNav)}><Icon name="menu" /></button>
        <div className="mobile-title"><strong>{active.label}</strong><small>Campus central</small></div>
        <div className="topbar-status"><span className="connection-dot" /> {isDemo ? 'Sistema demo disponible' : 'Modo conectado al backend'}</div>
        <div className="operator"><span>MO</span><div><strong>Operador de turno</strong><small>Coordinación universitaria</small></div></div>
      </header>
      <main id="main-content" key={dataVersion}>{view === 'directorio' ? <DirectoryView /> : view === 'alarma' ? <AlarmView /> : <EvacuationView />}</main>
      <footer>SalidaSegura · {isDemo ? 'Interfaz de demostración' : 'Interfaz conectada al backend'} · No conectada a sistemas físicos de alarma</footer>
    </div>
  </div>
}
