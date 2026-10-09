import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api'
import type { EmpresaPublica } from '../api'
import { useSesion } from '../session'
import { Aviso } from '../components/ui'

export default function Login() {
  const { yo, iniciarSesion, registrarse } = useSesion()
  const navegar = useNavigate()

  const [empresas, setEmpresas] = useState<EmpresaPublica[]>([])
  const [empresaId, setEmpresaId] = useState('')
  const [modoRegistro, setModoRegistro] = useState(false)
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    api.empresas()
      .then(lista => {
        setEmpresas(lista)
        if (lista.length === 1) setEmpresaId(lista[0].id)
      })
      .catch(e => setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las empresas.'))
  }, [])

  if (yo) return <Navigate to="/" replace />

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      if (modoRegistro) await registrarse(nombre, email, password, empresaId)
      else await iniciarSesion(email, password, empresaId)
      navegar('/')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Ocurrió un error inesperado.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login">
      <section className="login-presentacion">
        <h1>TicketDesk</h1>
        <p>
          Cada consulta de tus clientes, en un solo lugar: quién la atiende, en qué estado está
          y qué se hizo hasta ahora.
        </p>
      </section>

      <section className="login-formulario">
        <form onSubmit={enviar}>
          <h2>{modoRegistro ? 'Crear cuenta de cliente' : 'Ingresar'}</h2>

          {error && <Aviso tipo="error">{error}</Aviso>}

          <label>
            Empresa
            <select value={empresaId} onChange={e => setEmpresaId(e.target.value)} required>
              <option value="">Elegí tu empresa</option>
              {empresas.map(emp => <option key={emp.id} value={emp.id}>{emp.nombre}</option>)}
            </select>
          </label>

          {modoRegistro && (
            <label>
              Nombre y apellido
              <input value={nombre} onChange={e => setNombre(e.target.value)} required autoComplete="name" />
            </label>
          )}

          <label>
            Email
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
          </label>

          <label>
            Contraseña
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              minLength={modoRegistro ? 8 : undefined}
              autoComplete={modoRegistro ? 'new-password' : 'current-password'}
            />
            {modoRegistro && <small>Mínimo 8 caracteres.</small>}
          </label>

          <button type="submit" className="boton" disabled={enviando}>
            {enviando ? 'Un momento…' : modoRegistro ? 'Crear cuenta' : 'Ingresar'}
          </button>

          <button
            type="button"
            className="boton-texto"
            onClick={() => { setModoRegistro(!modoRegistro); setError(null) }}
          >
            {modoRegistro ? 'Ya tengo cuenta' : '¿No tenés cuenta? Registrate'}
          </button>

          {import.meta.env.DEV && (
            <p className="ayuda-demo">
              Datos de prueba: cliente@demo.com, agente@demo.com, supervisor@demo.com o admin@demo.com,
              todos con la contraseña Demo1234!
            </p>
          )}
        </form>
      </section>
    </div>
  )
}
