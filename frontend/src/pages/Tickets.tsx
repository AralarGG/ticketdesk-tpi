import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api'
import type { Categoria, Estado, FiltrosTickets, Nivel, Prioridad, TicketResumen } from '../api'
import { useSesion } from '../session'
import {
  Aviso, EstadoBadge, ESTADOS, ETIQUETA_ESTADO, ETIQUETA_NIVEL, ETIQUETA_PRIORIDAD, fechaCorta,
  NivelBadge, NIVELES, PrioridadBadge, PRIORIDADES, Vacio,
} from '../components/ui'

export default function Tickets() {
  const { yo, esPersonal, puedeCrearTickets, moduloActivo } = useSesion()
  const navegar = useNavigate()

  const [tickets, setTickets] = useState<TicketResumen[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [estado, setEstado] = useState<Estado | ''>('')
  const [prioridad, setPrioridad] = useState<Prioridad | ''>('')
  const [nivel, setNivel] = useState<Nivel | ''>('')
  const [categoriaId, setCategoriaId] = useState('')
  const [soloMios, setSoloMios] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (esPersonal) api.categorias().then(setCategorias).catch(() => setCategorias([]))
  }, [esPersonal])

  useEffect(() => {
    const filtros: FiltrosTickets = esPersonal
      ? { estado, prioridad, nivel, categoriaId: categoriaId || undefined, agenteId: soloMios ? yo?.id : undefined }
      : {}
    setCargando(true)
    setError(null)
    api.tickets(filtros)
      .then(setTickets)
      .catch(e => setError(e instanceof ApiError ? e.message : 'No se pudieron cargar los tickets.'))
      .finally(() => setCargando(false))
  }, [esPersonal, estado, prioridad, nivel, categoriaId, soloMios, yo?.id])

  const hayFiltros = estado !== '' || prioridad !== '' || nivel !== '' || categoriaId !== '' || soloMios

  function limpiar() {
    setEstado('')
    setPrioridad('')
    setNivel('')
    setCategoriaId('')
    setSoloMios(false)
  }

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>{esPersonal ? 'Tickets' : 'Mis tickets'}</h1>
          <p className="subtitulo">
            {cargando ? 'Cargando…' : `${tickets.length} ${tickets.length === 1 ? 'ticket' : 'tickets'}${hayFiltros ? ' con estos filtros' : ''}`}
          </p>
        </div>
        {puedeCrearTickets && (
          <div className="acciones">
            {moduloActivo('voz') && <Link className="boton boton-secundario" to="/tickets/nuevo-voz">Crear por voz</Link>}
            <Link className="boton" to="/tickets/nuevo">Nuevo ticket</Link>
          </div>
        )}
      </header>

      {esPersonal && (
        <div className="filtros" role="search" aria-label="Filtros de tickets">
          <label>Estado
            <select value={estado} onChange={e => setEstado(e.target.value as Estado | '')}>
              <option value="">Todos</option>
              {ESTADOS.map(s => <option key={s} value={s}>{ETIQUETA_ESTADO[s]}</option>)}
            </select>
          </label>
          <label>Prioridad
            <select value={prioridad} onChange={e => setPrioridad(e.target.value as Prioridad | '')}>
              <option value="">Todas</option>
              {PRIORIDADES.map(p => <option key={p} value={p}>{ETIQUETA_PRIORIDAD[p]}</option>)}
            </select>
          </label>
          <label>Nivel de atención
            <select value={nivel} onChange={e => setNivel(e.target.value as Nivel | '')}>
              <option value="">Todos</option>
              {NIVELES.map(n => <option key={n} value={n}>{ETIQUETA_NIVEL[n]}</option>)}
            </select>
          </label>
          <label>Categoría
            <select value={categoriaId} onChange={e => setCategoriaId(e.target.value)}>
              <option value="">Todas</option>
              {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </label>
          <label className="casilla">
            <input type="checkbox" checked={soloMios} onChange={e => setSoloMios(e.target.checked)} />
            Solo los míos
          </label>
          {hayFiltros && <button type="button" className="boton-texto" onClick={limpiar}>Quitar filtros</button>}
        </div>
      )}

      {error && <Aviso tipo="error">{error}</Aviso>}

      {!cargando && !error && tickets.length === 0 && (
        hayFiltros
          ? <Vacio titulo="Ningún ticket coincide con los filtros">Probá quitar alguno para ver más resultados.</Vacio>
          : esPersonal
            ? <Vacio titulo="Todavía no hay tickets">Cuando un cliente cree uno, va a aparecer acá.</Vacio>
            : <Vacio titulo="Todavía no creaste ningún ticket">Contanos tu problema y lo vamos a atender.</Vacio>
      )}

      {tickets.length > 0 && (
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr>
                <th>Ticket</th>
                {esPersonal && <th>Cliente</th>}
                <th>Estado</th>
                <th>Prioridad</th>
                {esPersonal && <th>Nivel</th>}
                {esPersonal && <th>Agente</th>}
                <th>Creado</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map(t => (
                <tr key={t.id} className="fila-link" onClick={() => navegar(`/tickets/${t.id}`)}>
                  <td>
                    <Link to={`/tickets/${t.id}`} onClick={e => e.stopPropagation()}>{t.titulo}</Link>
                    <span className="secundario">{t.categoria}</span>
                  </td>
                  {esPersonal && <td>{t.clienteNombre}</td>}
                  <td><EstadoBadge estado={t.estado} /></td>
                  <td><PrioridadBadge prioridad={t.prioridad} /></td>
                  {esPersonal && (
                    <td>
                      <NivelBadge nivel={t.nivelAtencion} />
                      {t.reincidente && <span className="reincidente">Reincidente</span>}
                    </td>
                  )}
                  {esPersonal && <td>{t.agenteAsignado ?? <span className="secundario">Sin asignar</span>}</td>}
                  <td>{fechaCorta(t.fechaCreacion)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
