import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../api'
import type { Estado, Historial, Nivel, TicketDetalle as Ticket, Usuario } from '../api'
import { useSesion } from '../session'
import {
  Aviso, EstadoBadge, ETIQUETA_ESTADO, ETIQUETA_NIVEL, fechaCorta, fechaLarga, NivelBadge, NIVELES,
  PrioridadBadge, Recorrido, Sla,
} from '../components/ui'

interface Transicion {
  estado: Estado
  texto: string
  motivoObligatorio?: boolean
}

/** Pasos que puede dar el personal de soporte desde cada estado (el cierre lo confirma el cliente). */
const TRANSICIONES: Partial<Record<Estado, Transicion[]>> = {
  ASIGNADO: [{ estado: 'EN_PROGRESO', texto: 'Empezar a trabajar' }],
  EN_PROGRESO: [
    { estado: 'ESPERANDO_CLIENTE', texto: 'Pedir información al cliente' },
    { estado: 'ESCALADO', texto: 'Escalar a un supervisor', motivoObligatorio: true },
    { estado: 'RESUELTO', texto: 'Marcar como resuelto' },
  ],
  ESPERANDO_CLIENTE: [{ estado: 'EN_PROGRESO', texto: 'Retomar' }],
  ESCALADO: [
    { estado: 'EN_PROGRESO', texto: 'Retomar' },
    { estado: 'RESUELTO', texto: 'Marcar como resuelto' },
  ],
  REABIERTO: [{ estado: 'EN_PROGRESO', texto: 'Retomar' }],
}

const ETIQUETA_CAMPO: Record<string, string> = {
  estado: 'Estado',
  agente_id: 'Agente',
  nivel_atencion: 'Nivel de atención',
}

export default function TicketDetalle() {
  const { id } = useParams<{ id: string }>()
  const { yo, esPersonal, puedeGestionar, esSupervisor, esCliente, moduloActivo } = useSesion()

  const [ticket, setTicket] = useState<Ticket | null>(null)
  const [historial, setHistorial] = useState<Historial[]>([])
  const [agentes, setAgentes] = useState<Usuario[]>([])
  const [cargando, setCargando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [ok, setOk] = useState<string | null>(null)

  const [motivo, setMotivo] = useState('')
  const [errorMotivo, setErrorMotivo] = useState<string | null>(null)
  const motivoRef = useRef<HTMLTextAreaElement>(null)
  const [comentario, setComentario] = useState('')
  const [agenteElegido, setAgenteElegido] = useState('')
  const [nivelElegido, setNivelElegido] = useState<Nivel>('NIVEL_1')

  const cargar = useCallback(async () => {
    if (!id) return
    try {
      const datos = await api.ticket(id)
      setTicket(datos)
      setNivelElegido(datos.nivelAtencion)
      if (esPersonal) setHistorial(await api.historial(id))
      setError(null)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo cargar el ticket.')
    } finally {
      setCargando(false)
    }
  }, [id, esPersonal])

  useEffect(() => { cargar() }, [cargar])

  useEffect(() => {
    if (esSupervisor) api.agentes().then(setAgentes).catch(() => setAgentes([]))
  }, [esSupervisor])

  async function ejecutar(accion: () => Promise<unknown>, mensajeOk: string) {
    setOcupado(true)
    setError(null)
    setOk(null)
    try {
      await accion()
      await cargar()
      setOk(mensajeOk)
      setMotivo('')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo completar la acción.')
    } finally {
      setOcupado(false)
    }
  }

  function cambiarEstado(t: Transicion) {
    if (!ticket) return
    if (t.motivoObligatorio && !motivo.trim()) {
      setErrorMotivo('Para escalar el ticket tenés que escribir el motivo en este cuadro.')
      motivoRef.current?.focus()
      return
    }
    setErrorMotivo(null)
    ejecutar(() => api.cambiarEstado(ticket.id, t.estado, motivo.trim() || undefined), `El ticket pasó a estado ${ETIQUETA_ESTADO[t.estado].toLowerCase()}.`)
  }

  function comentar(e: FormEvent) {
    e.preventDefault()
    if (!ticket || !comentario.trim()) return
    ejecutar(async () => {
      await api.comentar(ticket.id, comentario.trim())
      setComentario('')
    }, 'Comentario enviado.')
  }

  function subirFoto(archivo: File | undefined) {
    if (!ticket || !archivo) return
    ejecutar(() => api.subirAdjunto(ticket.id, archivo), 'Foto subida.')
  }

  if (cargando) return <p className="cargando">Cargando…</p>
  if (!ticket) return (
    <>
      {error && <Aviso tipo="error">{error}</Aviso>}
      <Link to="/" className="boton-texto">Volver a los tickets</Link>
    </>
  )

  const cerrado = ticket.estado === 'CERRADO'
  const esDueno = ticket.clienteId === yo?.id
  const transiciones = TRANSICIONES[ticket.estado] ?? []
  const puedeTomarlo = puedeGestionar && !cerrado && !esSupervisor && ticket.agenteId !== yo?.id

  return (
    <>
      <Link to="/" className="volver">Volver a los tickets</Link>

      <header className="cabecera cabecera-ticket">
        <div>
          <h1>{ticket.titulo}</h1>
          <div className="etiquetas">
            <EstadoBadge estado={ticket.estado} />
            <PrioridadBadge prioridad={ticket.prioridad} />
            {esPersonal && <NivelBadge nivel={ticket.nivelAtencion} />}
            {esPersonal && ticket.reincidente && <span className="reincidente">Reincidente</span>}
            {ticket.canalOrigen === 'VOZ' && <span className="badge nivel">Creado por voz</span>}
          </div>
        </div>
      </header>

      {error && <Aviso tipo="error">{error}</Aviso>}
      {ok && <Aviso tipo="ok">{ok}</Aviso>}

      <section className="panel">
        <Recorrido estado={ticket.estado} />
        {esPersonal && <Sla ticket={ticket} />}
      </section>

      <div className="dos-columnas">
        <div className="columna-principal">
          <section className="panel">
            <h2>Descripción</h2>
            <p className="texto-largo">{ticket.descripcion}</p>
            {ticket.audioUrl && (
              <div className="audio-original">
                <h3>Audio original</h3>
                <audio controls src={ticket.audioUrl} />
                {ticket.transcripcionOriginal && <p className="ayuda">Transcripción: {ticket.transcripcionOriginal}</p>}
              </div>
            )}
          </section>

          <section className="panel">
            <h2>Conversación</h2>
            {ticket.comentarios.length === 0 && <p className="ayuda">Todavía no hay comentarios.</p>}
            <ul className="comentarios">
              {ticket.comentarios.map(c => (
                <li key={c.id} className={c.usuarioRol === 'ROLE_USER' ? 'del-cliente' : 'del-equipo'}>
                  <div className="comentario-cabecera">
                    <strong>{c.usuarioNombre}</strong>
                    <span>{c.usuarioRol === 'ROLE_USER' ? 'Cliente' : 'Soporte'}</span>
                    <time dateTime={c.fecha}>{fechaCorta(c.fecha)}</time>
                  </div>
                  <p>{c.contenido}</p>
                </li>
              ))}
            </ul>

            {!cerrado ? (
              <form onSubmit={comentar} className="formulario">
                <label>
                  Agregar un comentario
                  <textarea value={comentario} onChange={e => setComentario(e.target.value)} rows={3} required />
                </label>
                <div className="acciones">
                  <button type="submit" className="boton" disabled={ocupado || !comentario.trim()}>Enviar comentario</button>
                </div>
              </form>
            ) : <p className="ayuda">El ticket está cerrado y no admite nuevos comentarios.</p>}
          </section>

          {(ticket.adjuntos.length > 0 || (esCliente && moduloActivo('adjuntos') && !cerrado)) && (
            <section className="panel">
              <h2>Fotos</h2>
              {ticket.adjuntos.length > 0 && (
                <ul className="galeria">
                  {ticket.adjuntos.map(a => (
                    <li key={a.id}>
                      <a href={a.url} target="_blank" rel="noreferrer">
                        <img src={a.url} alt={`Foto subida el ${fechaLarga(a.fechaSubida)}`} loading="lazy" />
                      </a>
                    </li>
                  ))}
                </ul>
              )}
              {esCliente && moduloActivo('adjuntos') && !cerrado && (
                <label className="subir">
                  Subir una foto o captura
                  <input type="file" accept="image/*" disabled={ocupado} onChange={e => { subirFoto(e.target.files?.[0]); e.target.value = '' }} />
                </label>
              )}
            </section>
          )}
        </div>

        <aside className="columna-lateral">
          <section className="panel">
            <h2>Datos</h2>
            <dl className="datos">
              <dt>Categoría</dt><dd>{ticket.categoria}</dd>
              <dt>Cliente</dt><dd>{ticket.clienteNombre}</dd>
              <dt>Agente</dt><dd>{ticket.agenteNombre ?? 'Sin asignar'}</dd>
              <dt>Creado</dt><dd>{fechaLarga(ticket.fechaCreacion)}</dd>
              <dt>Última actualización</dt><dd>{fechaLarga(ticket.fechaActualizacion)}</dd>
              {ticket.fechaResuelto && (<><dt>Resuelto</dt><dd>{fechaLarga(ticket.fechaResuelto)}</dd></>)}
            </dl>
          </section>

          {esDueno && ticket.estado === 'RESUELTO' && (
            <section className="panel destacado">
              <h2>¿Se solucionó tu problema?</h2>
              <div className="acciones vertical">
                <button type="button" className="boton" disabled={ocupado}
                  onClick={() => ejecutar(() => api.cambiarEstado(ticket.id, 'CERRADO'), 'Cerraste el ticket. ¡Gracias!')}>
                  Sí, cerrar el ticket
                </button>
                <button type="button" className="boton boton-secundario" disabled={ocupado}
                  onClick={() => ejecutar(() => api.cambiarEstado(ticket.id, 'REABIERTO'), 'Reabrimos el ticket.')}>
                  El problema continúa
                </button>
              </div>
            </section>
          )}

          {puedeGestionar && !cerrado && (
            <section className="panel">
              <h2>Gestión del ticket</h2>
              <div className="formulario">
                {ticket.estado === 'NUEVO' && !esSupervisor && (
                  <button type="button" className="boton" disabled={ocupado}
                    onClick={() => ejecutar(() => api.asignar(ticket.id, yo!.id), 'Tomaste el ticket.')}>
                    Tomar este ticket
                  </button>
                )}
                {puedeTomarlo && ticket.estado !== 'NUEVO' && (
                  <button type="button" className="boton boton-secundario" disabled={ocupado}
                    onClick={() => ejecutar(() => api.asignar(ticket.id, yo!.id), 'Tomaste el ticket.')}>
                    Tomarlo yo
                  </button>
                )}

                {esSupervisor && (
                  <div className="fila-accion">
                    <label>
                      Asignar a
                      <select value={agenteElegido} onChange={e => setAgenteElegido(e.target.value)}>
                        <option value="">Elegí a una persona</option>
                        {agentes.map(a => <option key={a.id} value={a.id}>{a.nombre}</option>)}
                      </select>
                    </label>
                    <button type="button" className="boton boton-secundario" disabled={ocupado || !agenteElegido}
                      onClick={() => ejecutar(() => api.asignar(ticket.id, agenteElegido, motivo.trim() || undefined), 'Ticket asignado.')}>
                      Asignar
                    </button>
                  </div>
                )}

                {transiciones.length > 0 && (
                  <>
                    <label>
                      Motivo o detalle (opcional)
                      <textarea ref={motivoRef} value={motivo} onChange={e => { setMotivo(e.target.value); setErrorMotivo(null) }} rows={2}
                        placeholder="Solo es obligatorio si vas a escalar el ticket." />
                    </label>
                    {errorMotivo && <Aviso tipo="error">{errorMotivo}</Aviso>}
                    <div className="acciones vertical">
                      {transiciones.map(t => (
                        <button key={t.estado} type="button" disabled={ocupado}
                          className={t.estado === 'RESUELTO' ? 'boton' : 'boton boton-secundario'}
                          onClick={() => cambiarEstado(t)}>
                          {t.texto}
                        </button>
                      ))}
                    </div>
                  </>
                )}

                <div className="fila-accion">
                  <label>
                    Nivel de atención
                    <select value={nivelElegido} onChange={e => setNivelElegido(e.target.value as Nivel)}>
                      {NIVELES.map(n => <option key={n} value={n}>{ETIQUETA_NIVEL[n]}</option>)}
                    </select>
                  </label>
                  <button type="button" className="boton boton-secundario" disabled={ocupado || nivelElegido === ticket.nivelAtencion}
                    onClick={() => ejecutar(() => api.cambiarNivel(ticket.id, nivelElegido, motivo.trim() || undefined), 'Nivel actualizado.')}>
                    Guardar
                  </button>
                </div>
              </div>
            </section>
          )}

          {esPersonal && (
            <section className="panel">
              <h2>Historial de cambios</h2>
              {historial.length === 0 && <p className="ayuda">Todavía no hay cambios registrados.</p>}
              <ol className="historial">
                {historial.map(h => (
                  <li key={h.id}>
                    <strong>{ETIQUETA_CAMPO[h.campoModificado] ?? h.campoModificado}</strong>
                    <span>{valorLegible(h.campoModificado, h.valorAnterior, yo?.id)} a {valorLegible(h.campoModificado, h.valorNuevo, yo?.id)}</span>
                    {h.motivo && <em>{h.motivo}</em>}
                    <small>{h.realizadoPor}, {fechaCorta(h.fecha)}</small>
                  </li>
                ))}
              </ol>
            </section>
          )}
        </aside>
      </div>
    </>
  )
}

/** Traduce los valores técnicos del historial a texto que se entienda. */
function valorLegible(campo: string, valor: string | null, miId?: string): string {
  if (!valor) return 'sin dato'
  if (campo === 'agente_id') {
    if (valor === 'sin_asignar') return 'sin asignar'
    return valor === miId ? 'vos' : 'otra persona'
  }
  return valor.toLowerCase().replace(/_/g, ' ')
}
