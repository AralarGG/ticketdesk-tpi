/**
 * Cliente de la API REST del backend. Los tipos reflejan los DTOs de Spring Boot
 * (ver docs/endpoints-api-ticketdesk.md).
 */

const BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export type Rol = 'ROLE_USER' | 'ROLE_AGENT' | 'ROLE_SUPERVISOR' | 'ROLE_ADMIN'
export type Estado =
  | 'NUEVO' | 'ASIGNADO' | 'EN_PROGRESO' | 'ESPERANDO_CLIENTE'
  | 'ESCALADO' | 'RESUELTO' | 'REABIERTO' | 'CERRADO'
export type Prioridad = 'BAJA' | 'MEDIA' | 'ALTA'
export type Nivel = 'NIVEL_1' | 'NIVEL_2' | 'NIVEL_3' | 'CRITICO'

export interface EmpresaPublica { id: string; nombre: string }
export interface AuthResponse { token: string; rol: Rol; usuarioId: string; empresaId: string }

export interface Usuario {
  id: string
  nombre: string
  email: string
  rol: Rol
  roles: Rol[]
  activo: boolean
  empresaId: string
  empresaNombre: string
}

export interface Configuracion {
  modulosHabilitados: string[]
  ventanaMantenimientoInicio: string | null
  ventanaMantenimientoFin: string | null
}

export interface Categoria { id: string; nombre: string; descripcion: string | null; esGenerica: boolean }

export interface TicketResumen {
  id: string
  titulo: string
  estado: Estado
  prioridad: Prioridad
  nivelAtencion: Nivel
  reincidente: boolean
  categoria: string
  clienteNombre: string
  agenteAsignado: string | null
  fechaCreacion: string
}

export interface Comentario {
  id: string
  contenido: string
  usuarioId: string
  usuarioNombre: string
  usuarioRol: Rol
  fecha: string
}

export interface Adjunto {
  id: string
  url: string
  tipo: string
  comentarioId: string | null
  fechaSubida: string
}

export interface TicketDetalle {
  id: string
  titulo: string
  descripcion: string
  estado: Estado
  prioridad: Prioridad
  nivelAtencion: Nivel
  slaHoras: number
  vencimientoSla: string | null
  reincidente: boolean
  esReclamoFormal: boolean
  responsableExterno: string | null
  canalOrigen: 'FORMULARIO' | 'VOZ'
  audioUrl: string | null
  transcripcionOriginal: string | null
  categoriaId: string
  categoria: string
  clienteId: string
  clienteNombre: string
  agenteId: string | null
  agenteNombre: string | null
  fechaCreacion: string
  fechaActualizacion: string
  fechaResuelto: string | null
  comentarios: Comentario[]
  adjuntos: Adjunto[]
}

export interface Historial {
  id: string
  campoModificado: string
  valorAnterior: string | null
  valorNuevo: string | null
  realizadoPor: string
  motivo: string | null
  fecha: string
}

export interface SugerenciaVoz {
  categoriaId: string | null
  categoriaNombre: string | null
  tituloSugerido: string
}

export interface FiltrosTickets {
  estado?: Estado | ''
  prioridad?: Prioridad | ''
  nivel?: Nivel | ''
  categoriaId?: string
  agenteId?: string
}

export class ApiError extends Error {
  status: number
  constructor(status: number, mensaje: string) {
    super(mensaje)
    this.status = status
  }
}

let token: string | null = localStorage.getItem('td_token')

export function guardarToken(nuevo: string | null) {
  token = nuevo
  if (nuevo) localStorage.setItem('td_token', nuevo)
  else localStorage.removeItem('td_token')
}

export function hayToken(): boolean {
  return token !== null
}

interface Opciones {
  metodo?: string
  json?: unknown
  form?: FormData
}

async function pedir<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  let body: BodyInit | undefined
  if (opciones.json !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(opciones.json)
  } else if (opciones.form) {
    body = opciones.form // el navegador agrega el boundary del multipart
  }

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE}${ruta}`, { method: opciones.metodo ?? 'GET', headers, body })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verificá que el backend esté corriendo.')
  }

  if (respuesta.status === 401 && token) {
    guardarToken(null)
    window.dispatchEvent(new Event('td-sesion-vencida'))
  }

  if (!respuesta.ok) {
    let mensaje = `Error ${respuesta.status}`
    try {
      const datos = await respuesta.json()
      if (datos && typeof datos.mensaje === 'string') mensaje = datos.mensaje
    } catch {
      /* la respuesta no traía JSON */
    }
    throw new ApiError(respuesta.status, mensaje)
  }

  if (respuesta.status === 204) return undefined as T
  return (await respuesta.json()) as T
}

function consulta(filtros: FiltrosTickets): string {
  const p = new URLSearchParams()
  if (filtros.estado) p.set('estado', filtros.estado)
  if (filtros.prioridad) p.set('prioridad', filtros.prioridad)
  if (filtros.nivel) p.set('nivel', filtros.nivel)
  if (filtros.categoriaId) p.set('categoriaId', filtros.categoriaId)
  if (filtros.agenteId) p.set('agenteId', filtros.agenteId)
  const texto = p.toString()
  return texto ? `?${texto}` : ''
}

export const api = {
  // Autenticación
  empresas: () => pedir<EmpresaPublica[]>('/auth/empresas'),
  login: (email: string, password: string, empresaId: string) =>
    pedir<AuthResponse>('/auth/login', { metodo: 'POST', json: { email, password, empresaId } }),
  registro: (nombre: string, email: string, password: string, empresaId: string) =>
    pedir<AuthResponse>('/auth/register', { metodo: 'POST', json: { nombre, email, password, empresaId } }),

  // Usuarios
  yo: () => pedir<Usuario>('/usuarios/me'),
  agentes: () => pedir<Usuario[]>('/usuarios/agentes'),
  usuarios: () => pedir<Usuario[]>('/usuarios'),
  crearUsuario: (datos: { nombre: string; email: string; password: string; rol: Rol }) =>
    pedir<Usuario>('/usuarios', { metodo: 'POST', json: datos }),
  agregarRol: (id: string, rol: Rol) =>
    pedir<Usuario>(`/usuarios/${id}/roles`, { metodo: 'POST', json: { rol } }),
  desactivarUsuario: (id: string) => pedir<Usuario>(`/usuarios/${id}/desactivar`, { metodo: 'PATCH' }),
  activarUsuario: (id: string) => pedir<Usuario>(`/usuarios/${id}/activar`, { metodo: 'PATCH' }),

  // Configuración de la empresa
  configuracion: (empresaId: string) => pedir<Configuracion>(`/empresas/${empresaId}/configuracion`),
  actualizarConfiguracion: (empresaId: string, datos: Partial<Configuracion>) =>
    pedir<Configuracion>(`/empresas/${empresaId}/configuracion`, { metodo: 'PATCH', json: datos }),

  // Categorías
  categorias: () => pedir<Categoria[]>('/categorias'),
  crearCategoria: (nombre: string, descripcion: string) =>
    pedir<Categoria>('/categorias', { metodo: 'POST', json: { nombre, descripcion } }),

  // Tickets
  tickets: (filtros: FiltrosTickets = {}) => pedir<TicketResumen[]>(`/tickets${consulta(filtros)}`),
  ticket: (id: string) => pedir<TicketDetalle>(`/tickets/${id}`),
  crearTicket: (datos: { titulo: string; descripcion: string; categoriaId: string; prioridad: Prioridad }) =>
    pedir<TicketDetalle>('/tickets', { metodo: 'POST', json: datos }),
  historial: (id: string) => pedir<Historial[]>(`/tickets/${id}/historial`),
  cambiarEstado: (id: string, estadoNuevo: Estado, motivo?: string) =>
    pedir<TicketDetalle>(`/tickets/${id}/estado`, { metodo: 'PATCH', json: { estadoNuevo, motivo } }),
  asignar: (id: string, agenteId: string, motivo?: string) =>
    pedir<TicketDetalle>(`/tickets/${id}/asignar`, { metodo: 'PATCH', json: { agenteId, motivo } }),
  cambiarNivel: (id: string, nivel: Nivel, motivo?: string) =>
    pedir<TicketDetalle>(`/tickets/${id}/nivel`, { metodo: 'PATCH', json: { nivel, motivo } }),

  // Comentarios y adjuntos
  comentar: (id: string, contenido: string) =>
    pedir<Comentario>(`/tickets/${id}/comentarios`, { metodo: 'POST', json: { contenido } }),
  subirAdjunto: (id: string, archivo: File) => {
    const form = new FormData()
    form.append('archivo', archivo)
    return pedir<Adjunto>(`/tickets/${id}/adjuntos`, { metodo: 'POST', form })
  },

  // Voz
  sugerirVoz: (transcripcion: string) =>
    pedir<SugerenciaVoz>('/tickets/voz/sugerencia', { metodo: 'POST', json: { transcripcion } }),
  crearTicketVoz: (datos: { audio: Blob; transcripcion: string; categoriaId: string; prioridad: Prioridad }) => {
    const form = new FormData()
    form.append('audio', datos.audio, 'voz.webm')
    form.append('transcripcion', datos.transcripcion)
    form.append('categoriaId', datos.categoriaId)
    form.append('prioridad', datos.prioridad)
    return pedir<TicketDetalle>('/tickets/voz', { metodo: 'POST', form })
  },
}
