import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError } from '../api'
import type { Categoria, Rol, Usuario } from '../api'
import { useSesion } from '../session'
import { Aviso, Vacio } from '../components/ui'

type Pestana = 'configuracion' | 'categorias' | 'usuarios'

const MODULOS: { clave: string; nombre: string; detalle: string; obligatorio?: boolean }[] = [
  { clave: 'tickets', nombre: 'Tickets', detalle: 'Alta, seguimiento y cierre de tickets.', obligatorio: true },
  { clave: 'comentarios', nombre: 'Comentarios', detalle: 'Conversación entre cliente y soporte dentro del ticket.', obligatorio: true },
  { clave: 'adjuntos', nombre: 'Fotos adjuntas', detalle: 'Los clientes pueden subir capturas de pantalla.' },
  { clave: 'voz', nombre: 'Tickets por voz', detalle: 'Los clientes pueden crear un ticket hablando.' },
  { clave: 'facturacion', nombre: 'Facturación', detalle: 'Módulo de facturación de la empresa.' },
  { clave: 'categorias_propias', nombre: 'Categorías propias', detalle: 'La empresa puede crear sus propias categorías.' },
]

const ROLES: { valor: Rol; nombre: string }[] = [
  { valor: 'ROLE_USER', nombre: 'Cliente' },
  { valor: 'ROLE_AGENT', nombre: 'Agente' },
  { valor: 'ROLE_SUPERVISOR', nombre: 'Supervisor' },
  { valor: 'ROLE_ADMIN', nombre: 'Administrador' },
]

const nombreRol = (rol: Rol) => ROLES.find(r => r.valor === rol)?.nombre ?? rol

export default function Admin() {
  const [pestana, setPestana] = useState<Pestana>('configuracion')
  const [error, setError] = useState<string | null>(null)
  const [ok, setOk] = useState<string | null>(null)

  function avisar(mensaje: string | null, fallo: string | null = null) {
    setOk(mensaje)
    setError(fallo)
  }

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Administración</h1>
          <p className="subtitulo">Configurá cómo funciona TicketDesk para tu empresa.</p>
        </div>
      </header>

      <div className="pestanas" role="tablist">
        {([['configuracion', 'Configuración'], ['categorias', 'Categorías'], ['usuarios', 'Usuarios']] as [Pestana, string][]).map(([clave, nombre]) => (
          <button key={clave} type="button" role="tab" aria-selected={pestana === clave}
            className={pestana === clave ? 'activa' : ''}
            onClick={() => { setPestana(clave); avisar(null) }}>
            {nombre}
          </button>
        ))}
      </div>

      {error && <Aviso tipo="error">{error}</Aviso>}
      {ok && <Aviso tipo="ok">{ok}</Aviso>}

      {pestana === 'configuracion' && <PanelConfiguracion avisar={avisar} />}
      {pestana === 'categorias' && <PanelCategorias avisar={avisar} />}
      {pestana === 'usuarios' && <PanelUsuarios avisar={avisar} />}
    </>
  )
}

type Avisar = (mensaje: string | null, fallo?: string | null) => void

function mensajeDe(e: unknown, defecto: string): string {
  return e instanceof ApiError ? e.message : defecto
}

function PanelConfiguracion({ avisar }: { avisar: Avisar }) {
  const { yo, configuracion, recargarConfiguracion } = useSesion()
  const [modulos, setModulos] = useState<string[]>(configuracion?.modulosHabilitados ?? [])
  const [inicio, setInicio] = useState(configuracion?.ventanaMantenimientoInicio?.slice(0, 5) ?? '')
  const [fin, setFin] = useState(configuracion?.ventanaMantenimientoFin?.slice(0, 5) ?? '')
  const [guardando, setGuardando] = useState(false)

  function alternar(clave: string) {
    setModulos(actuales => actuales.includes(clave) ? actuales.filter(m => m !== clave) : [...actuales, clave])
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (!yo) return
    setGuardando(true)
    avisar(null)
    try {
      await api.actualizarConfiguracion(yo.empresaId, {
        modulosHabilitados: modulos,
        ventanaMantenimientoInicio: inicio || null,
        ventanaMantenimientoFin: fin || null,
      })
      await recargarConfiguracion()
      avisar('Configuración guardada.')
    } catch (err) {
      avisar(null, mensajeDe(err, 'No se pudo guardar la configuración.'))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="panel formulario" onSubmit={guardar}>
      <fieldset>
        <legend>Módulos habilitados</legend>
        <ul className="modulos">
          {MODULOS.map(m => (
            <li key={m.clave}>
              <label className="casilla">
                <input type="checkbox" checked={modulos.includes(m.clave) || !!m.obligatorio}
                  disabled={m.obligatorio} onChange={() => alternar(m.clave)} />
                <span><strong>{m.nombre}</strong><small>{m.detalle}{m.obligatorio ? ' Siempre activo.' : ''}</small></span>
              </label>
            </li>
          ))}
        </ul>
      </fieldset>

      <fieldset>
        <legend>Ventana de mantenimiento</legend>
        <p className="ayuda">
          En este horario no se pueden crear tickets ni hacer cambios; los datos se pueden seguir consultando.
          Si la hora de fin es anterior a la de inicio, la ventana cruza la medianoche.
        </p>
        <div className="fila-horas">
          <label>Desde<input type="time" value={inicio} onChange={e => setInicio(e.target.value)} /></label>
          <label>Hasta<input type="time" value={fin} onChange={e => setFin(e.target.value)} /></label>
        </div>
      </fieldset>

      <div className="acciones">
        <button type="submit" className="boton" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
      </div>
    </form>
  )
}

function PanelCategorias({ avisar }: { avisar: Avisar }) {
  const { moduloActivo } = useSesion()
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')

  const cargar = useCallback(() => {
    api.categorias().then(setCategorias).catch(e => avisar(null, mensajeDe(e, 'No se pudieron cargar las categorías.')))
  }, [avisar])

  useEffect(() => { cargar() }, [cargar])

  async function crear(e: FormEvent) {
    e.preventDefault()
    avisar(null)
    try {
      await api.crearCategoria(nombre, descripcion)
      setNombre('')
      setDescripcion('')
      cargar()
      avisar('Categoría creada.')
    } catch (err) {
      avisar(null, mensajeDe(err, 'No se pudo crear la categoría.'))
    }
  }

  return (
    <>
      <section className="panel">
        <h2>Categorías disponibles</h2>
        <ul className="lista-simple">
          {categorias.map(c => (
            <li key={c.id}>
              <strong>{c.nombre}</strong>
              <span className="secundario">{c.esGenerica ? 'General, de todo el sistema' : 'Propia de tu empresa'}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel">
        <h2>Crear una categoría propia</h2>
        {moduloActivo('categorias_propias') ? (
          <form className="formulario" onSubmit={crear}>
            <label>Nombre<input value={nombre} onChange={e => setNombre(e.target.value)} maxLength={100} required /></label>
            <label>Descripción (opcional)<input value={descripcion} onChange={e => setDescripcion(e.target.value)} /></label>
            <div className="acciones"><button type="submit" className="boton">Crear categoría</button></div>
          </form>
        ) : (
          <Vacio titulo="Las categorías propias están desactivadas">
            Activá el módulo "Categorías propias" en la pestaña Configuración para crear las tuyas.
          </Vacio>
        )}
      </section>
    </>
  )
}

function PanelUsuarios({ avisar }: { avisar: Avisar }) {
  const { yo } = useSesion()
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rol, setRol] = useState<Rol>('ROLE_AGENT')
  const [rolExtra, setRolExtra] = useState<Record<string, Rol>>({})

  const cargar = useCallback(() => {
    api.usuarios().then(setUsuarios).catch(e => avisar(null, mensajeDe(e, 'No se pudieron cargar los usuarios.')))
  }, [avisar])

  useEffect(() => { cargar() }, [cargar])

  async function accion(fn: () => Promise<unknown>, mensajeOk: string) {
    avisar(null)
    try {
      await fn()
      cargar()
      avisar(mensajeOk)
    } catch (err) {
      avisar(null, mensajeDe(err, 'No se pudo completar la acción.'))
    }
  }

  async function crear(e: FormEvent) {
    e.preventDefault()
    await accion(async () => {
      await api.crearUsuario({ nombre, email, password, rol })
      setNombre('')
      setEmail('')
      setPassword('')
    }, 'Usuario creado.')
  }

  return (
    <>
      <section className="panel">
        <h2>Usuarios de la empresa</h2>
        <div className="tabla-contenedor">
          <table className="tabla">
            <thead>
              <tr><th>Nombre</th><th>Roles</th><th>Estado</th><th>Acciones</th></tr>
            </thead>
            <tbody>
              {usuarios.map(u => (
                <tr key={u.id}>
                  <td>{u.nombre}<span className="secundario">{u.email}</span></td>
                  <td>{u.roles.map(nombreRol).join(', ')}</td>
                  <td>{u.activo ? 'Activo' : <span className="secundario">Desactivado</span>}</td>
                  <td>
                    <div className="acciones-fila">
                      <select aria-label={`Rol a sumar para ${u.nombre}`} value={rolExtra[u.id] ?? ''}
                        onChange={e => setRolExtra({ ...rolExtra, [u.id]: e.target.value as Rol })}>
                        <option value="">Sumar un rol</option>
                        {ROLES.filter(r => !u.roles.includes(r.valor)).map(r => <option key={r.valor} value={r.valor}>{r.nombre}</option>)}
                      </select>
                      <button type="button" className="boton-texto" disabled={!rolExtra[u.id]}
                        onClick={() => accion(() => api.agregarRol(u.id, rolExtra[u.id]), 'Rol agregado.')}>
                        Sumar
                      </button>
                      {u.activo
                        ? <button type="button" className="boton-texto peligro" disabled={u.id === yo?.id}
                            onClick={() => accion(() => api.desactivarUsuario(u.id), 'Usuario desactivado.')}>Desactivar</button>
                        : <button type="button" className="boton-texto"
                            onClick={() => accion(() => api.activarUsuario(u.id), 'Usuario activado.')}>Activar</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="ayuda">Un agente con tickets abiertos no se puede desactivar hasta reasignarlos.</p>
      </section>

      <section className="panel">
        <h2>Agregar una persona</h2>
        <form className="formulario" onSubmit={crear}>
          <label>Nombre y apellido<input value={nombre} onChange={e => setNombre(e.target.value)} required /></label>
          <label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label>
          <label>Contraseña inicial
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} minLength={8} required />
            <small>Mínimo 8 caracteres.</small>
          </label>
          <label>Rol principal
            <select value={rol} onChange={e => setRol(e.target.value as Rol)}>
              {ROLES.map(r => <option key={r.valor} value={r.valor}>{r.nombre}</option>)}
            </select>
          </label>
          <div className="acciones"><button type="submit" className="boton">Agregar usuario</button></div>
        </form>
      </section>
    </>
  )
}
