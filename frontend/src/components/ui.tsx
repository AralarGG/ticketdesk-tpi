import type { ReactNode } from 'react'
import type { Estado, Nivel, Prioridad, TicketDetalle } from '../api'

export const ETIQUETA_ESTADO: Record<Estado, string> = {
  NUEVO: 'Nuevo',
  ASIGNADO: 'Asignado',
  EN_PROGRESO: 'En progreso',
  ESPERANDO_CLIENTE: 'Esperando al cliente',
  ESCALADO: 'Escalado',
  RESUELTO: 'Resuelto',
  REABIERTO: 'Reabierto',
  CERRADO: 'Cerrado',
}

export const ETIQUETA_NIVEL: Record<Nivel, string> = {
  NIVEL_1: 'Nivel 1',
  NIVEL_2: 'Nivel 2',
  NIVEL_3: 'Nivel 3',
  CRITICO: 'Crítico',
}

export const ETIQUETA_PRIORIDAD: Record<Prioridad, string> = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
}

export const ESTADOS: Estado[] = [
  'NUEVO', 'ASIGNADO', 'EN_PROGRESO', 'ESPERANDO_CLIENTE', 'ESCALADO', 'RESUELTO', 'REABIERTO', 'CERRADO',
]
export const NIVELES: Nivel[] = ['NIVEL_1', 'NIVEL_2', 'NIVEL_3', 'CRITICO']
export const PRIORIDADES: Prioridad[] = ['BAJA', 'MEDIA', 'ALTA']

export function EstadoBadge({ estado }: { estado: Estado }) {
  return <span className={`badge estado-${estado.toLowerCase()}`}>{ETIQUETA_ESTADO[estado]}</span>
}

export function NivelBadge({ nivel }: { nivel: Nivel }) {
  return <span className={`badge nivel ${nivel === 'CRITICO' ? 'nivel-critico' : ''}`}>{ETIQUETA_NIVEL[nivel]}</span>
}

export function PrioridadBadge({ prioridad }: { prioridad: Prioridad }) {
  return <span className={`prioridad prioridad-${prioridad.toLowerCase()}`}>{ETIQUETA_PRIORIDAD[prioridad]}</span>
}

export function Aviso({ tipo, children }: { tipo: 'error' | 'ok' | 'info'; children: ReactNode }) {
  return (
    <div className={`aviso aviso-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

export function Vacio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="vacio">
      <strong>{titulo}</strong>
      {children && <p>{children}</p>}
    </div>
  )
}

export function fechaCorta(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export function fechaLarga(iso: string | null): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('es-AR', {
    day: '2-digit', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

const PASOS: { estado: Estado; titulo: string }[] = [
  { estado: 'NUEVO', titulo: 'Nuevo' },
  { estado: 'ASIGNADO', titulo: 'Asignado' },
  { estado: 'EN_PROGRESO', titulo: 'En progreso' },
  { estado: 'RESUELTO', titulo: 'Resuelto' },
  { estado: 'CERRADO', titulo: 'Cerrado' },
]

/** Posición del estado dentro del recorrido de cinco pasos. */
function posicion(estado: Estado): number {
  switch (estado) {
    case 'NUEVO': return 0
    case 'ASIGNADO': return 1
    case 'EN_PROGRESO':
    case 'ESPERANDO_CLIENTE':
    case 'ESCALADO':
    case 'REABIERTO': return 2
    case 'RESUELTO': return 3
    case 'CERRADO': return 4
  }
}

const AVISO_ESTADO: Partial<Record<Estado, string>> = {
  ESPERANDO_CLIENTE: 'Esperando una respuesta del cliente para seguir.',
  ESCALADO: 'Derivado a un supervisor.',
  REABIERTO: 'El cliente indicó que el problema continúa.',
  RESUELTO: 'Si el cliente no responde dentro de las 72 horas, se cierra solo.',
}

/** Recorrido del ticket: muestra en qué paso del ciclo de vida está. */
export function Recorrido({ estado }: { estado: Estado }) {
  const actual = posicion(estado)
  return (
    <div className="recorrido">
      <ol aria-label="Recorrido del ticket">
        {PASOS.map((paso, i) => (
          <li key={paso.estado} className={i < actual ? 'hecho' : i === actual ? 'actual' : ''}>
            <span className="punto" aria-hidden="true" />
            <span className="paso-titulo">{paso.titulo}</span>
          </li>
        ))}
      </ol>
      {AVISO_ESTADO[estado] && <p className="recorrido-nota">{AVISO_ESTADO[estado]}</p>}
    </div>
  )
}

/** Tiempo objetivo de resolución (SLA) según el nivel de atención del ticket. */
export function Sla({ ticket }: { ticket: TicketDetalle }) {
  if (!ticket.vencimientoSla) return null
  const inicio = new Date(ticket.fechaCreacion).getTime()
  const fin = new Date(ticket.vencimientoSla).getTime()
  const terminado = ticket.estado === 'RESUELTO' || ticket.estado === 'CERRADO'
  const referencia = terminado && ticket.fechaResuelto ? new Date(ticket.fechaResuelto).getTime() : Date.now()
  const avance = Math.max(0, Math.min(100, ((referencia - inicio) / (fin - inicio)) * 100))
  const vencido = referencia > fin

  let texto: string
  if (terminado) {
    texto = vencido ? 'Se resolvió fuera del tiempo objetivo.' : 'Se resolvió dentro del tiempo objetivo.'
  } else if (vencido) {
    texto = `Tiempo objetivo vencido el ${fechaCorta(ticket.vencimientoSla).replace(/\.$/, '')}.`
  } else {
    texto = `Vence el ${fechaCorta(ticket.vencimientoSla).replace(/\.$/, '')}.`
  }

  return (
    <div className="sla">
      <div className="sla-cabecera">
        <span>Tiempo objetivo: {ticket.slaHoras} h</span>
        <span className={vencido && !terminado ? 'sla-vencido' : ''}>{texto}</span>
      </div>
      <div className="sla-barra" role="progressbar" aria-valuenow={Math.round(avance)} aria-valuemin={0} aria-valuemax={100}>
        <div className={`sla-relleno ${vencido ? 'vencido' : ''}`} style={{ width: `${avance}%` }} />
      </div>
    </div>
  )
}
